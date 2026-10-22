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
import restaurantrush.staff.Waiter;

/** Runs the CLI with real game collaborators; no launcher wiring is required. */
public class RestaurantCLIRunTest {
    public static void main(String[] args) {
        testConstructorAndEarlyOutcomeGuards();
        testEndOfInputInEitherPhase();
        testRejectedActionsRetrySameChoice();
        testOrderingCookingServingAndPayment();
        testFullDefeat();
        testFullVictory();
        testResolutionFailurePropagates();
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
        Fixture waiterEof = new Fixture("");
        waiterEof.cli.run();
        equal(1, waiterEof.manager.getCurrentTurn(), "EOF does not advance");
        equal(100, waiterEof.restaurant.findCustomer(1).getSatisfaction(), "EOF applies no penalty");
        equal(true, waiterEof.manager.canAssignWaiter(), "EOF does not invent Waiter wait");
        assertIncomplete(waiterEof);

        Fixture chefEof = new Fixture("seat 1 1\n");
        chefEof.cli.run();
        equal(CustomerStatus.SEATED, chefEof.restaurant.findCustomer(1).getStatus(), "Accepted action remains applied");
        equal(100, chefEof.restaurant.findCustomer(1).getSatisfaction(), "Partial turn has no penalty");
        equal(true, chefEof.manager.canAssignChef(), "EOF does not invent Chef wait");
        assertIncomplete(chefEof);
    }

    private static void testRejectedActionsRetrySameChoice() {
        Fixture f = new Fixture("seat x 1\nseat 99 1\nseat 1 1\ncook 1\nwait\n");
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
        Fixture f = new Fixture("seat 1 1\nwait\norder 1\ncook 1\nserve 1\nwait\nwait\nwait\nwait\nwait\n");
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

    private static void testFullDefeat() {
        Fixture f = new Fixture("wait\nwait\n".repeat(18));
        f.cli.run();
        equal(true, f.controller.isFinished(), "All turns completed");
        equal(false, f.controller.isVictory(), "Waiting loses");
        equal(18, occurrences(f.text(), "End of turn "), "Each turn resolves once");
        equal(1, occurrences(f.text(), "Defeat"), "One final outcome");
        equal(0, f.restaurant.getPaidCount(), "No invented payments");
        contains(f.text(), "Defeat\nRevenue: $0.00\nAverage paid satisfaction: N/A");
        contains(f.text(), "Turns used: 18");
        absent(f.text(), "Turn 19");
    }

    private static void testFullVictory() {
        // One Waiter and one Chef command per row, exercising all dispatch paths.
        String[][] turns = {
            {"seat 1 1", "wait"}, {"order 1", "cook 1"}, {"serve 1", "wait"},
            {"seat 2 2", "wait"}, {"order 2", "cook 2"}, {"serve 2", "wait"},
            {"seat 3 1", "wait"}, {"order 3", "cook 3"}, {"wait", "cook 3"},
            {"serve 3", "wait"}, {"seat 4 2", "wait"}, {"order 4", "cook 4"},
            {"serve 4", "wait"}, {"seat 5 1", "wait"}, {"order 5", "cook 5"},
            {"serve 5", "wait"}, {"seat 6 2", "wait"}, {"order 6", "cook 6"}
        };
        StringBuilder commands = new StringBuilder();
        for (String[] turn : turns) {
            commands.append(turn[0]).append('\n').append(turn[1]).append('\n');
        }
        Fixture f = new Fixture(commands.toString());
        f.cli.run();
        equal(true, f.controller.isFinished(), "Victory still completes all 18 turns");
        equal(true, f.controller.isVictory(), "Real controller decides victory");
        equal(5, f.restaurant.getPaidCount(), "Five paid customers");
        equal(6600, f.restaurant.getRevenueCents(), "Revenue from five bills");
        equal(79.2, f.restaurant.averagePaidSatisfaction(), "Recorded paid average");
        equal(18, occurrences(f.text(), "End of turn "), "No early victory");
        equal(1, occurrences(f.text(), "Victory"), "One final victory");
        contains(f.text(), "Victory\nRevenue: $66.00\nAverage paid satisfaction: 79.20");
        contains(f.text(), "Paid: 5 | Served: 5 | Unhappy departures: 0\nTurns used: 18");
        equal(false, f.input.hasNextLine(), "Consumes precisely the supplied choices");
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
        Fixture f = new Fixture("wait\nwait\n", cashier);
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
            controller = new GameController(restaurant, manager, new Waiter(), new Chef());
            input = new Scanner(commands);
            cli = new RestaurantCLI(controller, input, output);
        }

        String text() {
            return bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private static int occurrences(String text, String part) {
        return (text.length() - text.replace(part, "").length()) / part.length();
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
