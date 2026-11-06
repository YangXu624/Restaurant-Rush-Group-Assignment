package restaurantrush.boundary;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import restaurantrush.control.GameController;
import restaurantrush.support.ActionResult;
import restaurantrush.support.CustomerView;
import restaurantrush.support.GameSnapshot;
import restaurantrush.support.TableView;

/**
 * Console input and snapshot presentation. Game actions belong to the controller.
 *
 * <p>Owner: M5: Toshiki. See docs/class-contracts.md for the contract.
 */
public class RestaurantCLI {
    /** Each player prompt, in turn order, with the commands it accepts. */
    private enum Role {
        MANAGER("Manager", "happy or wait"),
        HOST("Host", "seat <customerId> <tableId> or wait"),
        WAITER("Waiter", "seat <customerId> <tableId>, order <customerId>, serve <customerId>, or wait"),
        CHEF("Chef", "cook <customerId> or wait");

        final String label;
        final String usage;

        Role(String label, String usage) {
            this.label = label;
            this.usage = usage;
        }
    }

    private final GameController controller;
    private final Scanner input;
    private final PrintStream output;

    /**
     * Uses the supplied shared controller for every game action and state read.
     * The caller owns these streams; the CLI does not close them.
     */
    public RestaurantCLI(GameController controller, Scanner input, PrintStream output) {
        this.controller = Objects.requireNonNull(controller, "Controller cannot be null");
        this.input = Objects.requireNonNull(input, "Input scanner cannot be null");
        this.output = Objects.requireNonNull(output, "Output stream cannot be null");
    }

    public void run() {
        while (!controller.isFinished()) {
            controller.startTurn();
            GameSnapshot start = controller.getSnapshot();
            displayState(start);
            if (!chooseHappyHour() || !chooseTask(Role.HOST) || !chooseTask(Role.WAITER)
                    || !chooseTask(Role.CHEF)) {
                showFeedback("Input ended. The current turn was not completed.");
                return;
            }
            // Resolution failures propagate: automatic events may already have run.
            controller.finishTurn();
            output.println("End of turn " + start.turn());
            displayStatistics(controller.getSnapshot());
            output.println("Turns used: " + start.turn());
            output.flush();
        }
        displayOutcome(controller.isVictory());
    }

    public void displayState(GameSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "Snapshot cannot be null");
        output.println("Turn " + snapshot.turn());
        displayStatistics(snapshot);
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
            output.println("    Type: " + customer.customerType() + " | Locked bill: "
                    + (customer.billCents() == null ? "none"
                            : String.format(Locale.ROOT, "$%d.%02d",
                                    customer.billCents() / 100, customer.billCents() % 100)));
        }
        output.println("Happy Hour: " + (snapshot.happyHourTurnsRemaining() > 0
                ? "active, " + snapshot.happyHourTurnsRemaining() + " turn(s) remaining"
                : snapshot.happyHourAvailable() ? "available" : "unavailable"));
        output.println("Host: " + (snapshot.hostAvailable() ? "available" : "unavailable"));
        String waiterAvailability = snapshot.waiterAvailable() ? "available"
                : snapshot.hostAvailable() ? "available after the Host chooses" : "unavailable";
        output.println("Waiter: " + waiterAvailability);
        String chefAvailability = snapshot.chefAvailable() ? "available"
                : snapshot.hostAvailable() || snapshot.waiterAvailable()
                        ? "available after the Waiter chooses" : "unavailable";
        output.println("Chef: " + chefAvailability);
        output.flush();
    }

    /** Returns a normalized valid command, or null at end of input. */
    public String readManagerChoice() {
        return readChoice(Role.MANAGER);
    }

    /** Returns a normalized valid command, or null at end of input. */
    public String readHostChoice() {
        return readChoice(Role.HOST);
    }

    /** Returns a normalized valid command, or null at end of input. */
    public String readWaiterChoice() {
        return readChoice(Role.WAITER);
    }

    /** Returns a normalized valid command, or null at end of input. */
    public String readChefChoice() {
        return readChoice(Role.CHEF);
    }

    public void showFeedback(String message) {
        output.println(Objects.requireNonNull(message, "Feedback cannot be null"));
        output.flush();
    }

    private String readChoice(Role role) {
        String usage = role.usage;
        while (true) {
            output.print(role.label + " (" + usage + ")> ");
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
            int expectedParts = expectedParts(role, parts[0]);
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
        if (!controller.isFinished()) {
            throw new IllegalStateException("Cannot display an outcome before the game finishes");
        }
        GameSnapshot finalState = controller.getSnapshot();
        output.println(won ? "Victory" : "Defeat");
        displayStatistics(finalState);
        output.println("Turns used: " + finalState.turn());
        output.flush();
    }

    /** Returns how many words the role's command takes, or -1 if the role cannot use it. */
    private static int expectedParts(Role role, String command) {
        return switch (command) {
            case "wait" -> 1;
            case "happy" -> role == Role.MANAGER ? 1 : -1;
            case "seat" -> role == Role.HOST || role == Role.WAITER ? 3 : -1;
            case "order", "serve" -> role == Role.WAITER ? 2 : -1;
            case "cook" -> role == Role.CHEF ? 2 : -1;
            default -> -1;
        };
    }

    /** Offers the promotion only while it can be activated; returns false at end of input. */
    private boolean chooseHappyHour() {
        if (!controller.canActivateHappyHour()) {
            return true;
        }
        String command = readManagerChoice();
        if (command == null) {
            return false;
        }
        if (command.equals("happy")) {
            showFeedback(controller.activateHappyHour().getMessage());
            // Recovery has just changed satisfaction, so show the updated state.
            displayState(controller.getSnapshot());
        } else {
            showFeedback("Happy Hour stays off.");
        }
        return true;
    }

    private boolean chooseTask(Role role) {
        while (true) {
            String command = readChoice(role);
            if (command == null) {
                return false;
            }
            ActionResult result = perform(role, command.split(" "));
            showFeedback(result.getMessage());
            if (result.isSuccess()) {
                return true;
            }
        }
    }

    private ActionResult perform(Role role, String[] parts) {
        return switch (role) {
            case HOST -> parts[0].equals("wait") ? controller.waitHost()
                    : controller.hostSeatCustomer(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            case WAITER -> switch (parts[0]) {
                case "seat" -> controller.seatCustomer(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                case "order" -> controller.takeOrder(Integer.parseInt(parts[1]));
                case "serve" -> controller.serveOrder(Integer.parseInt(parts[1]));
                case "wait" -> controller.waitWaiter();
                default -> throw new IllegalStateException("Unexpected parsed Waiter command");
            };
            case CHEF -> parts[0].equals("wait") ? controller.waitChef()
                    : controller.prepareOrder(Integer.parseInt(parts[1]));
            case MANAGER -> throw new IllegalStateException("The manager's choice is not a staff task");
        };
    }

    private void displayStatistics(GameSnapshot snapshot) {
        output.printf(Locale.ROOT, "Revenue: $%d.%02d%n",
                snapshot.revenueCents() / 100, snapshot.revenueCents() % 100);
        output.println("Average paid satisfaction: " + (snapshot.averagePaidSatisfaction() == null
                ? "N/A" : String.format(Locale.ROOT, "%.2f", snapshot.averagePaidSatisfaction())));
        output.printf("Paid: %d | Served: %d | Unhappy departures: %d%n",
                snapshot.paidCount(), snapshot.servedCount(), snapshot.abandonedCount());
    }
}
