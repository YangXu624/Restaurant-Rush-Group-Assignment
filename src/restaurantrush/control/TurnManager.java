package restaurantrush.control;

import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.Restaurant;
import restaurantrush.staff.Cashier;

/** Owns Stage 1 turn sequencing and automatic events. Owner: M4: Nickolas. */
public class TurnManager {
    private static final int LAST_TURN = 18;
    private static final int ARRIVAL_INTERVAL = 3;

    private int currentTurn = 1;
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
            r.addArrival(new Customer((currentTurn - 1) / ARRIVAL_INTERVAL + 1));
        }
        waiterTaskUsed = false;
        chefTaskUsed = false;
        turnResolved = false;
        turnStarted = true;
    }

    public boolean canAssignWaiter() {
        return turnStarted && !turnResolved && !finished && !waiterTaskUsed;
    }

    public boolean canAssignChef() {
        return turnStarted && !turnResolved && !finished && waiterTaskUsed && !chefTaskUsed;
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
     * @throws IllegalStateException unless both choices in an active turn are used
     */
    public void resolveTurn(Restaurant r) {
        requireRestaurant(r);
        if (!turnStarted || turnResolved || finished || !waiterTaskUsed || !chefTaskUsed) {
            throw new IllegalStateException("Turn resolution requires both staff choices in an active turn");
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

    private static void requireRestaurant(Restaurant r) {
        if (r == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
    }
}
