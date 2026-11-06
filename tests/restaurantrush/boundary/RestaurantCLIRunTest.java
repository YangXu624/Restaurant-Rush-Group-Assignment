package restaurantrush.boundary;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;
import restaurantrush.control.GameController;
import restaurantrush.control.TurnManager;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.OrderStatus;
import restaurantrush.entity.Restaurant;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Host;
import restaurantrush.staff.Waiter;

/** Runs the CLI with real game collaborators; no launcher wiring is required. */
public class RestaurantCLIRunTest {
    public static void main(String[] args) {
        testConstructorAndEarlyOutcomeGuards();
        testEndOfInputInEitherPhase();
        testRejectedActionsRetrySameChoice();
        testOrderingCookingServingAndPayment();
        testResolutionFailurePropagates();
        testHostSeatingAndRetry();
        testHappyHourChoiceAndDuration();
        System.out.println("All RestaurantCLI turn-loop tests passed successfully!");
    }

    private static void testConstructorAndEarlyOutcomeGuards() {
        Fixture f = new Fixture("");
        expect(NullPointerException.class, () -> new RestaurantCLI(null, f.input, f.output));
        expect(NullPointerException.class, () -> new RestaurantCLI(f.controller, null, f.output));
        expect(NullPointerException.class, () -> new RestaurantCLI(f.controller, f.input, null));
        expect(IllegalStateException.class, () -> f.cli.displayOutcome(true));
        equal("", f.text(), "Early outcome produces no output");
    }

    private static void testEndOfInputInEitherPhase() {
        Fixture managerEof = new Fixture("");
        managerEof.cli.run();
        equal(1, managerEof.manager.getCurrentTurn(), "EOF does not advance");
        equal(false, managerEof.restaurant.getHappyHour().isUsed(), "EOF does not activate Happy Hour");
        equal(true, managerEof.manager.canAssignHost(), "EOF does not invent a Host wait");
        assertIncomplete(managerEof);

        Fixture hostEof = new Fixture("wait\n");
        hostEof.cli.run();
        equal(true, hostEof.manager.canAssignHost(), "EOF does not invent a Host wait");
        assertIncomplete(hostEof);

        Fixture waiterEof = new Fixture("wait\nwait\n");
        waiterEof.cli.run();
        equal(1, waiterEof.manager.getCurrentTurn(), "EOF does not advance");
        equal(100, waiterEof.restaurant.findCustomer(1).getSatisfaction(), "EOF applies no penalty");
        equal(true, waiterEof.manager.canAssignWaiter(), "EOF does not invent Waiter wait");
        assertIncomplete(waiterEof);

        Fixture chefEof = new Fixture("wait\nwait\nseat 1 1\n");
        chefEof.cli.run();
        equal(CustomerStatus.SEATED, chefEof.restaurant.findCustomer(1).getStatus(), "Accepted action remains applied");
        equal(100, chefEof.restaurant.findCustomer(1).getSatisfaction(), "Partial turn has no penalty");
        equal(true, chefEof.manager.canAssignChef(), "EOF does not invent Chef wait");
        assertIncomplete(chefEof);
    }

    private static void testRejectedActionsRetrySameChoice() {
        Fixture f = new Fixture("wait\nwait\nseat x 1\nseat 99 1\nseat 1 1\ncook 1\nwait\n");
        f.cli.run();
        equal(2, f.manager.getCurrentTurn(), "Exactly one turn completed despite retries");
        equal(CustomerStatus.SEATED, f.restaurant.findCustomer(1).getStatus(), "Valid seat accepted after failures");
        equal(92, f.restaurant.findCustomer(1).getSatisfaction(), "Penalty runs once");
        contains(f.text(), "IDs must be whole numbers");
        contains(f.text(), "Customer 99 has not arrived.");
        contains(f.text(), "Customer 1 has no order.");
        contains(f.text(), "End of turn 1\nRevenue: $0.00");
        contains(f.text(), "Turns used: 1");
        absent(f.text(), "Turns used: 2");
        noOutcome(f);
    }

    private static void testOrderingCookingServingAndPayment() {
        Fixture f = new Fixture(turns("seat 1 1", "wait", "order 1", "cook 1", "serve 1", "wait", "wait", "wait", "wait", "wait"));
        f.cli.run();
        equal(6, f.manager.getCurrentTurn(), "Five complete turns");
        equal(CustomerStatus.PAID, f.restaurant.findCustomer(1).getStatus(), "Real payment through automatic phases");
        equal(OrderStatus.PAID, f.restaurant.findOrder(1).getStatus(), "Order paid");
        equal(3, f.restaurant.findCustomer(1).getServedTurn(), "Serve after prior turn cooking");
        equal(4, f.restaurant.findCustomer(1).getReadyToPayTurn(), "Eating ends next turn");
        equal(900, f.restaurant.getRevenueCents(), "Paid bill only");
        equal(84.0, f.restaurant.averagePaidSatisfaction(), "No waiting penalty after service");
        equal(true, f.restaurant.getTables().get(0).isAvailable(), "Payment releases table");
        String output = f.text();
        contains(output, "order: Salad | READY | 0 unit(s) remaining");
        contains(output, "End of turn 4\nRevenue: $0.00");
        contains(output, "End of turn 5\nRevenue: $9.00");
        noOutcome(f);
    }

    private static void testResolutionFailurePropagates() {
        IllegalStateException failure = new IllegalStateException("Controlled Cashier defect");
        int[] calls = {0};
        Cashier cashier = new Cashier() {
            @Override
            public boolean collectOne(Restaurant restaurant, int turn) {
                calls[0]++;
                throw failure;
            }
        };
        Fixture f = new Fixture("wait\nwait\nwait\nwait\n", cashier);
        try {
            f.cli.run();
            throw new AssertionError("Expected resolution failure");
        } catch (IllegalStateException actual) {
            if (actual != failure) {
                throw new AssertionError("Must propagate original failure", actual);
            }
        }
        equal(1, calls[0], "No automatic resolution retry");
        equal(1, f.manager.getCurrentTurn(), "Failed resolution does not advance");
        absent(f.text(), "End of turn");
        noOutcome(f);
    }

    private static void testHostSeatingAndRetry() {
        // Failed seating attempts keep the Host's action for this turn.
        Fixture f = new Fixture("wait\nseat 99 1\nseat 1 3\nseat 1 1\norder 1\ncook 1\n");
        f.cli.run();
        equal(CustomerStatus.SEATED, f.restaurant.findCustomer(1).getStatus(), "Host seated after retries");
        equal(OrderStatus.READY, f.restaurant.findOrder(1).getStatus(), "Ordered and cooked on the same turn");
        equal(2, f.manager.getCurrentTurn(), "One turn completed");
        contains(f.text(), "Host: available");
        contains(f.text(), "Waiter: available after the Host chooses");
        contains(f.text(), "Customer 99 has not arrived.");
        contains(f.text(), "Customer 1 ordered Salad.");
    }

    private static void testHappyHourChoiceAndDuration() {
        Fixture f = new Fixture("dance\nhappy\nseat 1 1\norder 1\ncook 1\n"
                + "wait\nserve 1\nwait\nwait\nwait\nwait\n");
        f.cli.run();
        equal(4, f.manager.getCurrentTurn(), "Three turns completed");
        equal(720, f.restaurant.findOrder(1).getBillCents(), "Salad with 20% off");
        String output = f.text();
        contains(output, "Invalid command. Use happy or wait.");
        contains(output, "Happy Hour is active for 2 turns.");
        contains(output, "Type: Regular | Locked bill: $7.20");
        contains(output, "Happy Hour: available");
        contains(output, "Happy Hour: active, 2 turn(s) remaining");
        contains(output, "Happy Hour: active, 1 turn(s) remaining");
        contains(output, "Happy Hour: unavailable");
        equal(2, occurrences(output, "Manager ("), "One rejected and one accepted prompt, then never offered again");
        equal(2, occurrences(output, "Turn 1\n"), "State is shown again after activation");

        Fixture declined = new Fixture("wait\nwait\nwait\nwait\nwait\n");
        declined.cli.run();
        contains(declined.text(), "Happy Hour stays off.");
        equal(false, declined.restaurant.getHappyHour().isUsed(), "Declining does not use the promotion");
        equal(2, occurrences(declined.text(), "Manager ("), "Offered again on the next turn");
    }

    /** Adds the Manager and Host waits before each pair of Waiter and Chef commands. */
    private static String turns(String... waiterAndChef) {
        StringBuilder commands = new StringBuilder();
        for (int i = 0; i < waiterAndChef.length; i += 2) {
            commands.append("wait\nwait\n").append(waiterAndChef[i]).append('\n')
                    .append(waiterAndChef[i + 1]).append('\n');
        }
        return commands.toString();
    }

    private static int occurrences(String text, String part) {
        return (text.length() - text.replace(part, "").length()) / part.length();
    }

    private static void assertIncomplete(Fixture f) {
        equal(false, f.controller.isFinished(), "EOF is not game completion");
        contains(f.text(), "Input ended. The current turn was not completed.");
        absent(f.text(), "End of turn");
        absent(f.text(), "Turns used:");
        noOutcome(f);
    }

    private static void noOutcome(Fixture f) {
        absent(f.text(), "Victory");
        absent(f.text(), "Defeat");
    }

    private static class Fixture {
        final Restaurant restaurant = new Restaurant();
        final TurnManager manager;
        final GameController controller;
        final Scanner input;
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        final RestaurantCLI cli;

        Fixture(String commands) {
            this(commands, new Cashier());
        }

        Fixture(String commands, Cashier cashier) {
            manager = new TurnManager(cashier);
            controller = new GameController(restaurant, manager, new Host(), new Waiter(), new Chef());
            input = new Scanner(commands);
            cli = new RestaurantCLI(controller, input, output);
        }

        String text() {
            return bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private static void contains(String text, String expected) {
        if (!text.contains(expected)) {
            throw new AssertionError("Missing output: " + expected + "\nActual:\n" + text);
        }
    }

    private static void absent(String text, String unexpected) {
        if (text.contains(unexpected)) {
            throw new AssertionError("Unexpected output: " + unexpected);
        }
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) {
                return;
            }
            throw new AssertionError("Unexpected exception", failure);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
