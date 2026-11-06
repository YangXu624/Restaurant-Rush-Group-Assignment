package restaurantrush.control;

import restaurantrush.entity.CriticCustomer;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.VIPCustomer;
import restaurantrush.staff.Cashier;
import restaurantrush.support.ActionResult;

/** Owns Stage 2 turn sequencing and automatic events. Owner: M4: Nickolas. */
public class TurnManager {
    private static final int LAST_TURN = 18;
    private static final int ARRIVAL_INTERVAL = 3;

    private int currentTurn = 1;
    private boolean hostTaskUsed = false;
    private boolean waiterTaskUsed = false;
    private boolean chefTaskUsed = false;
    private boolean turnStarted = false;
    private boolean turnResolved = false;
    private boolean finished = false;
    private final Cashier cashier;

    /** @throws IllegalArgumentException if the shared cashier is null */
    public TurnManager(Cashier cashier) {
        if (cashier == null) {
            throw new IllegalArgumentException("Cashier cannot be null");
        }
        this.cashier = cashier;
    }

    /** Rejects null restaurants and duplicate or post-completion starts. */
    public void beginTurn(Restaurant r) {
        requireRestaurant(r);
        if (finished || turnStarted) {
            throw new IllegalStateException("Cannot start an active or finished turn");
        }
        // Add the arrival before changing lifecycle flags, in case validation fails.
        if ((currentTurn - 1) % ARRIVAL_INTERVAL == 0) {
            int id = (currentTurn - 1) / ARRIVAL_INTERVAL + 1;
            Customer arrival = switch (id) {
                case 2 -> new VIPCustomer(id);
                case 5 -> new CriticCustomer(id);
                default -> new Customer(id);
            };
            r.addArrival(arrival);
        }
        hostTaskUsed = false;
        waiterTaskUsed = false;
        chefTaskUsed = false;
        turnResolved = false;
        turnStarted = true;
        // beginTurn runs once per turn, so redisplays and rejected commands cannot repeat this.
        if (r.getHappyHour().isActive()) {
            recoverWaitingCustomers(r);
        }
    }

    /** The manager decides after the arrival and before the Host acts. */
    public boolean canActivateHappyHour(Restaurant r) {
        requireRestaurant(r);
        return turnStarted && !turnResolved && !finished && !hostTaskUsed && !r.getHappyHour().isUsed();
    }

    /** The activation turn is an active turn, so its recovery is applied here. */
    public ActionResult activateHappyHour(Restaurant r) {
        if (!canActivateHappyHour(r)) {
            return ActionResult.fail("Happy Hour is unavailable.");
        }
        ActionResult result = r.getHappyHour().activate();
        if (result.isSuccess()) {
            recoverWaitingCustomers(r);
        }
        return result;
    }

    public boolean canAssignHost() {
        return turnStarted && !turnResolved && !finished && !hostTaskUsed;
    }

    public void recordHostTask() {
        if (!canAssignHost()) {
            throw new IllegalStateException("Host choice is unavailable");
        }
        hostTaskUsed = true;
    }

    public boolean canAssignWaiter() {
        return turnStarted && !turnResolved && !finished && hostTaskUsed && !waiterTaskUsed;
    }

    public boolean canAssignChef() {
        return turnStarted && !turnResolved && !finished && hostTaskUsed && waiterTaskUsed && !chefTaskUsed;
    }

    public void recordWaiterTask() {
        if (!canAssignWaiter()) {
            throw new IllegalStateException("Waiter choice is unavailable");
        }
        waiterTaskUsed = true;
    }

    public void recordChefTask() {
        if (!canAssignChef()) {
            throw new IllegalStateException("Chef choice is unavailable");
        }
        chefTaskUsed = true;
    }

    /**
     * Runs whole phases in order, then marks resolution complete.
     * Collaborator exceptions propagate; partial processing is not rolled back
     * and retrying a failed resolution is not guaranteed safe.
     *
     * @throws IllegalArgumentException if the restaurant is null
     * @throws IllegalStateException unless all three choices in an active turn are used
     */
    public void resolveTurn(Restaurant r) {
        requireRestaurant(r);
        if (!turnStarted || turnResolved || finished || !hostTaskUsed || !waiterTaskUsed || !chefTaskUsed) {
            throw new IllegalStateException("Turn resolution requires all three staff choices in an active turn");
        }
        for (Customer customer : r.getActiveCustomers()) {
            customer.finishEating(currentTurn);
        }
        cashier.collectOne(r, currentTurn);
        for (Customer customer : r.getActiveCustomers()) {
            customer.applyWaitingPenalty();
        }
        // Iterate a read-only copy; Restaurant owns removal and all related changes.
        for (Customer customer : r.getActiveCustomers()) {
            CustomerStatus status = customer.getStatus();
            if ((status == CustomerStatus.WAITING || status == CustomerStatus.SEATED)
                    && customer.getSatisfaction() == 0) {
                r.removeAbandoned(customer);
            }
        }
        r.getHappyHour().endTurn();
        turnResolved = true;
    }

    public void advanceTurn() {
        if (!turnStarted || !turnResolved || finished) {
            throw new IllegalStateException("Only a successfully resolved turn can advance");
        }
        turnStarted = false;
        if (currentTurn < LAST_TURN) {
            currentTurn++;
        } else {
            finished = true;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    private static void recoverWaitingCustomers(Restaurant r) {
        for (Customer customer : r.getActiveCustomers()) {
            customer.recoverSatisfaction();
        }
    }

    private static void requireRestaurant(Restaurant r) {
        if (r == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
    }
}
