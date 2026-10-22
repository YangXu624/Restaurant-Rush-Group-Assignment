package restaurantrush.boundary;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import restaurantrush.control.GameController;
import restaurantrush.support.CustomerView;
import restaurantrush.support.GameSnapshot;
import restaurantrush.support.TableView;

/**
 * Console input and snapshot presentation. Game actions belong to the controller.
 *
 * <p>Owner: M5: Toshiki. See docs/class-contracts.md for the contract.
 */
public class RestaurantCLI {
    private GameController controller;
    private final Scanner input;
    private final PrintStream output;

    /**
     * Creates the input/display boundary independently of controller integration.
     * The caller owns these streams; the CLI does not close them.
     */
    public RestaurantCLI(Scanner input, PrintStream output) {
        this.input = Objects.requireNonNull(input, "Input scanner cannot be null");
        this.output = Objects.requireNonNull(output, "Output stream cannot be null");
    }

    // TODO B2: Inject the shared controller and implement the turn loop/outcome.

    public void run() {
        throw new UnsupportedOperationException("RestaurantCLI.run is not implemented yet");
    }

    public void displayState(GameSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "Snapshot cannot be null");
        output.println("Turn " + snapshot.turn());
        output.printf(Locale.ROOT, "Revenue: $%d.%02d%n",
                snapshot.revenueCents() / 100, snapshot.revenueCents() % 100);
        output.println("Average paid satisfaction: " + (snapshot.averagePaidSatisfaction() == null
                ? "N/A" : String.format(Locale.ROOT, "%.2f", snapshot.averagePaidSatisfaction())));
        output.printf("Paid: %d | Served: %d | Unhappy departures: %d%n",
                snapshot.paidCount(), snapshot.servedCount(), snapshot.abandonedCount());
        output.println("Waiting queue: " + (snapshot.waitingCustomerIds().isEmpty()
                ? "empty" : snapshot.waitingCustomerIds()));
        output.println("Tables:");
        for (TableView table : snapshot.tables()) {
            output.println("  Table " + table.id() + ": " + (table.occupantId() == null
                    ? "free" : "customer " + table.occupantId()));
        }
        output.println("Customers:");
        if (snapshot.customers().isEmpty()) {
            output.println("  none");
        }
        for (CustomerView customer : snapshot.customers()) {
            String order = customer.orderStatus() == null ? "none"
                    : customer.itemName() + " | " + customer.orderStatus()
                            + " | " + customer.remainingUnits() + " unit(s) remaining";
            output.printf("  Customer %d | satisfaction %d | %s | table %s | order: %s%n",
                    customer.id(), customer.satisfaction(), customer.status(),
                    customer.tableId() == null ? "none" : customer.tableId().toString(), order);
        }
        output.println("Waiter: " + (snapshot.waiterAvailable() ? "available" : "unavailable"));
        String chefAvailability = snapshot.chefAvailable() ? "available"
                : snapshot.waiterAvailable() ? "available after the Waiter chooses" : "unavailable";
        output.println("Chef: " + chefAvailability);
        output.flush();
    }

    /** Returns a normalized valid command, or null at end of input. */
    public String readWaiterChoice() {
        return readChoice(true);
    }

    /** Returns a normalized valid command, or null at end of input. */
    public String readChefChoice() {
        return readChoice(false);
    }

    public void showFeedback(String message) {
        output.println(Objects.requireNonNull(message, "Feedback cannot be null"));
        output.flush();
    }

    private String readChoice(boolean waiter) {
        String usage = waiter
                ? "seat <customerId> <tableId>, order <customerId>, serve <customerId>, or wait"
                : "cook <customerId> or wait";
        while (true) {
            output.print((waiter ? "Waiter" : "Chef") + " (" + usage + ")> ");
            output.flush();
            if (!input.hasNextLine()) {
                return null;
            }
            String line = input.nextLine().strip();
            if (line.isEmpty()) {
                showFeedback("Enter a command: " + usage + ".");
                continue;
            }
            String[] parts = line.split("\\s+");
            parts[0] = parts[0].toLowerCase(Locale.ROOT);
            int expectedParts = switch (parts[0]) {
                case "wait" -> 1;
                case "seat" -> waiter ? 3 : -1;
                case "order", "serve" -> waiter ? 2 : -1;
                case "cook" -> waiter ? -1 : 2;
                default -> -1;
            };
            if (parts.length != expectedParts) {
                showFeedback("Invalid command. Use " + usage + ".");
                continue;
            }
            try {
                for (int i = 1; i < parts.length; i++) {
                    parts[i] = Integer.toString(Integer.parseInt(parts[i]));
                }
            } catch (NumberFormatException invalidId) {
                showFeedback("IDs must be whole numbers within the supported range.");
                continue;
            }
            // Existence, occupancy and action eligibility are checked by the controller/domain.
            return String.join(" ", parts);
        }
    }

    public void displayOutcome(boolean won) {
        throw new UnsupportedOperationException("RestaurantCLI.displayOutcome is not implemented yet");
    }
}
