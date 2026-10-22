package restaurantrush.boundary;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.OrderStatus;
import restaurantrush.support.CustomerView;
import restaurantrush.support.GameSnapshot;
import restaurantrush.support.TableView;

/** Input and rendering tests only; controller actions and the turn loop are separate work. */
public class RestaurantCLITest {
    public static void main(String[] args) {
        testWaiterCommands();
        testChefCommands();
        testInvalidInputRetries();
        testEndOfInputAndStreamOwnership();
        testEmptyDisplay();
        testDetailedDisplayAndLocale();
        testAvailabilityAndFeedback();
        System.out.println("All RestaurantCLI input/display tests passed successfully!");
    }

    private static void testWaiterCommands() {
        Fixture f = new Fixture("  SeAt  01\t+2  \norder 3\nserve 4\nWAIT\n");
        equal("seat 1 2", f.cli.readWaiterChoice(), "Normalize command, whitespace and IDs");
        equal("order 3", f.cli.readWaiterChoice(), "Order grammar");
        equal("serve 4", f.cli.readWaiterChoice(), "Serve grammar");
        equal("wait", f.cli.readWaiterChoice(), "Explicit wait");
        // Syntactically valid IDs reach domain validation later; no base ID limits in the parser.
        Fixture domainInput = new Fixture("seat -1 99\n");
        equal("seat -1 99", domainInput.cli.readWaiterChoice(), "Do not implement domain validation");
    }

    private static void testChefCommands() {
        Fixture f = new Fixture(" COOK\t3 \nwait\n");
        equal("cook 3", f.cli.readChefChoice(), "Chef cooking grammar");
        equal("wait", f.cli.readChefChoice(), "Chef wait grammar");
    }

    private static void testInvalidInputRetries() {
        String[] badWaiter = {"", "dance", "cook 1", "seat 1", "seat 1 2 extra",
                "order", "order 1 extra", "serve", "serve 1 extra", "wait extra"};
        for (String invalid : badWaiter) {
            Fixture f = new Fixture(invalid + "\nseat 1 2\n");
            equal("seat 1 2", f.cli.readWaiterChoice(), "Retry invalid Waiter input: " + invalid);
            contains(f.text(), invalid.isEmpty() ? "Enter a command" : "Invalid command");
        }
        for (String invalid : new String[] {"seat one 2", "seat 1 two", "order 1.5", "serve 2147483648"}) {
            Fixture f = new Fixture(invalid + "\nwait\n");
            equal("wait", f.cli.readWaiterChoice(), "Retry invalid Waiter ID: " + invalid);
            contains(f.text(), "IDs must be whole numbers");
        }
        for (String invalid : new String[] {"seat 1 2", "order 1", "serve 1", "cook", "cook 1 extra"}) {
            Fixture f = new Fixture(invalid + "\nwait\n");
            equal("wait", f.cli.readChefChoice(), "Retry invalid Chef input: " + invalid);
            contains(f.text(), "Invalid command");
        }
        for (String invalid : new String[] {"cook x", "cook -2147483649"}) {
            Fixture f = new Fixture(invalid + "\nwait\n");
            equal("wait", f.cli.readChefChoice(), "Retry invalid Chef ID: " + invalid);
            contains(f.text(), "IDs must be whole numbers");
        }
    }

    private static void testEndOfInputAndStreamOwnership() {
        Fixture empty = new Fixture("");
        equal(null, empty.cli.readWaiterChoice(), "EOF at Waiter prompt");
        equal(null, empty.cli.readChefChoice(), "EOF at Chef prompt");
        Fixture invalid = new Fixture("cook no");
        equal(null, invalid.cli.readChefChoice(), "Invalid final line then EOF does not fabricate wait");
        Fixture lastLine = new Fixture("wait");
        equal("wait", lastLine.cli.readWaiterChoice(), "Accept final line without newline");
        equal(null, lastLine.cli.readWaiterChoice(), "Then report EOF");
        // Reading must leave caller-owned streams usable.
        Fixture shared = new Fixture("wait\nremaining\n");
        shared.cli.readChefChoice();
        equal("remaining", shared.input.nextLine(), "Scanner stays open");
        shared.cli.showFeedback("Still open");
        contains(shared.text(), "Still open");
        if (empty.text().contains("Victory") || empty.text().contains("Defeat")) {
            throw new AssertionError("EOF must not announce an outcome");
        }
    }

    private static void testEmptyDisplay() {
        Fixture f = new Fixture("");
        f.cli.displayState(new GameSnapshot(1, 0, null, List.of(), List.of(),
                List.of(new TableView(1, null), new TableView(2, null)), true, false, 0, 0, 0));
        for (String expected : new String[] {"Turn 1", "Revenue: $0.00", "Average paid satisfaction: N/A",
                "Waiting queue: empty", "Table 1: free", "Table 2: free", "Customers:\n  none",
                "Waiter: available", "Chef: available after the Waiter chooses"}) {
            contains(f.text(), expected);
        }
    }

    private static void testDetailedDisplayAndLocale() {
        Fixture f = new Fixture("");
        GameSnapshot snapshot = new GameSnapshot(7, 4205, 260.0 / 3, List.of(4, 2),
                List.of(new CustomerView(3, 76, CustomerStatus.SEATED, OrderStatus.PREPARING, "Pasta", 1, 2),
                        new CustomerView(1, 84, CustomerStatus.PAID, OrderStatus.PAID, "Salad", 0, null)),
                List.of(new TableView(1, null), new TableView(2, 3)), false, true, 1, 2, 1);
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            f.cli.displayState(snapshot);
        } finally {
            Locale.setDefault(original);
        }
        for (String expected : new String[] {"Turn 7", "Revenue: $42.05", "Average paid satisfaction: 86.67",
                "Paid: 1 | Served: 2 | Unhappy departures: 1", "Waiting queue: [4, 2]",
                "Table 2: customer 3", "Customer 3 | satisfaction 76 | SEATED | table 2 | order: Pasta | PREPARING | 1 unit(s) remaining",
                "Customer 1 | satisfaction 84 | PAID | table none | order: Salad | PAID | 0 unit(s) remaining",
                "Waiter: unavailable", "Chef: available"}) {
            contains(f.text(), expected);
        }
    }

    private static void testAvailabilityAndFeedback() {
        Fixture f = new Fixture("");
        f.cli.displayState(new GameSnapshot(18, 0, null, List.of(1),
                List.of(new CustomerView(1, 100, CustomerStatus.WAITING, null, null, null, null)),
                List.of(new TableView(1, null)), false, false, 0, 0, 0));
        contains(f.text(), "WAITING | table none | order: none");
        contains(f.text(), "Waiter: unavailable");
        contains(f.text(), "Chef: unavailable");
        f.cli.showFeedback("Customer has not arrived.");
        contains(f.text(), "Customer has not arrived.");
    }

    private static class Fixture {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final Scanner input;
        final RestaurantCLI cli;

        Fixture(String commands) {
            input = new Scanner(commands);
            cli = new RestaurantCLI(input, new PrintStream(bytes, true, StandardCharsets.UTF_8));
        }

        String text() {
            return bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void contains(String actual, String expected) {
        if (!actual.contains(expected)) {
            throw new AssertionError("Missing output: " + expected + "\nActual output:\n" + actual);
        }
    }
}
