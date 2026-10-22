package restaurantrush.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.MenuItem;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.staff.Cashier;

/** Standalone coordination tests; no real Cashier/payment integration is claimed. */
public class TurnManagerTest {
    public static void main(String[] args) {
        testLifecycleGuardsAndNulls();
        testArrivalFailurePreservesLifecycle();
        testAutomaticPhaseOrderAndEatingTiming();
        testAllTurnsAndCompletion();
        testCollaboratorFailureDoesNotResolve();
        System.out.println("All TurnManager tests passed successfully!");
    }

    private static void testLifecycleGuardsAndNulls() {
        expect(IllegalArgumentException.class, () -> new TurnManager(null));
        RecordingCashier cashier = new RecordingCashier();
        Restaurant restaurant = new Restaurant();
        TurnManager manager = new TurnManager(cashier);
        equal(new State(1, false, false, false), State.capture(manager), "Initial state");
        rejected(manager, IllegalArgumentException.class, () -> manager.beginTurn(null));
        rejected(manager, IllegalArgumentException.class, () -> manager.resolveTurn(null));
        rejected(manager, IllegalStateException.class, manager::recordWaiterTask);
        rejected(manager, IllegalStateException.class, manager::recordChefTask);
        rejected(manager, IllegalStateException.class, manager::advanceTurn);
        rejected(manager, IllegalStateException.class, () -> manager.resolveTurn(restaurant));
        equal(0, restaurant.getCustomers().size(), "No arrival before start");

        manager.beginTurn(restaurant);
        equal(new State(1, false, true, false), State.capture(manager), "Waiter starts first");
        // Repeated reads must not consume either choice.
        equal(State.capture(manager), State.capture(manager), "Availability checks are pure");
        rejected(manager, IllegalStateException.class, manager::recordChefTask);
        rejected(manager, IllegalStateException.class, () -> manager.beginTurn(restaurant));
        rejected(manager, IllegalArgumentException.class, () -> manager.beginTurn(null));
        rejected(manager, IllegalStateException.class, () -> manager.resolveTurn(restaurant));
        rejected(manager, IllegalStateException.class, manager::advanceTurn);
        equal(1, restaurant.getCustomers().size(), "Duplicate start adds no arrival");
        equal(100, restaurant.findCustomer(1).getSatisfaction(), "Rejected resolution has no penalty");

        manager.recordWaiterTask();
        equal(new State(1, false, false, true), State.capture(manager), "Chef follows Waiter");
        rejected(manager, IllegalStateException.class, manager::recordWaiterTask);
        rejected(manager, IllegalStateException.class, () -> manager.resolveTurn(restaurant));
        rejected(manager, IllegalArgumentException.class, () -> manager.resolveTurn(null));
        manager.recordChefTask();
        rejected(manager, IllegalStateException.class, manager::recordChefTask);
        rejected(manager, IllegalStateException.class, manager::recordWaiterTask);
        rejected(manager, IllegalStateException.class, manager::advanceTurn);
        rejected(manager, IllegalArgumentException.class, () -> manager.resolveTurn(null));
        equal(0, cashier.calls, "Invalid calls never invoke Cashier");

        manager.resolveTurn(restaurant);
        equal(1, cashier.calls, "One Cashier call");
        equal(92, restaurant.findCustomer(1).getSatisfaction(), "One penalty");
        rejected(manager, IllegalStateException.class, () -> manager.resolveTurn(restaurant));
        rejected(manager, IllegalStateException.class, () -> manager.beginTurn(restaurant));
        rejected(manager, IllegalStateException.class, manager::recordWaiterTask);
        rejected(manager, IllegalStateException.class, manager::recordChefTask);
        rejected(manager, IllegalArgumentException.class, () -> manager.resolveTurn(null));
        equal(1, cashier.calls, "Duplicate resolution does not call Cashier");
        equal(92, restaurant.findCustomer(1).getSatisfaction(), "Duplicate resolution has no penalty");
        manager.advanceTurn();
        equal(new State(2, false, false, false), State.capture(manager), "Advance closes turn");
        rejected(manager, IllegalStateException.class, manager::advanceTurn);
        rejected(manager, IllegalStateException.class, () -> manager.resolveTurn(restaurant));
        manager.beginTurn(restaurant);
        equal(new State(2, false, true, false), State.capture(manager), "Next turn resets choices");
    }

    private static void testArrivalFailurePreservesLifecycle() {
        Restaurant restaurant = new Restaurant();
        restaurant.addArrival(new Customer(1));
        TurnManager manager = new TurnManager(new RecordingCashier());
        rejected(manager, IllegalArgumentException.class, () -> manager.beginTurn(restaurant));
        equal(1, restaurant.getCustomers().size(), "Rejected arrival preserves history");
        // A separate valid fixture proves the failed start did not open the turn.
        manager.beginTurn(new Restaurant());
        equal(true, manager.canAssignWaiter(), "Start remains available after arrival rejection");
    }

    private static void testAutomaticPhaseOrderAndEatingTiming() {
        RecordingCashier cashier = new RecordingCashier();
        ActiveCustomerRestaurant restaurant = new ActiveCustomerRestaurant();
        TurnManager manager = new TurnManager(cashier);
        manager.beginTurn(restaurant);
        Customer waiting = restaurant.findCustomer(1);
        Customer otherWaiting = new Customer(50);
        restaurant.addArrival(otherWaiting);
        // Both queued customers reach zero in resolution, exercising removal from copies.
        for (int i = 0; i < 12; i++) {
            waiting.applyWaitingPenalty();
            otherWaiting.applyWaitingPenalty();
        }
        Customer first = servedCustomer(100, 1);
        Customer second = servedCustomer(101, 1);
        restaurant.extraActive = List.of(first, second);
        cashier.onCollect = (r, turn) -> {
            same(restaurant, r, "Pass supplied restaurant to Cashier");
            equal(1, turn, "Pass current turn");
            equal(CustomerStatus.SERVED, first.getStatus(), "Same-turn eating is blocked");
            equal(CustomerStatus.SERVED, second.getStatus(), "Same-turn eating is blocked for all");
            equal(4, waiting.getSatisfaction(), "Penalty has not run before Cashier");
            equal(4, otherWaiting.getSatisfaction(), "No interleaved penalties");
            equal(2, r.getWaitingQueue().size(), "Abandonment has not run before Cashier");
        };
        chooseBoth(manager);
        manager.resolveTurn(restaurant);
        equal(1, cashier.calls, "Cashier invoked once");
        equal(CustomerStatus.LEFT, waiting.getStatus(), "Abandonment follows zero-producing penalty");
        equal(CustomerStatus.LEFT, otherWaiting.getStatus(), "All eligible customers removed");
        equal(0, waiting.getSatisfaction(), "Penalty reaches zero");
        equal(0, restaurant.getWaitingQueue().size(), "Real Restaurant removes queued customers");
        equal(2, restaurant.getAbandonedCount(), "Real departure count");
        equal(2, restaurant.getCustomers().size(), "History retained");
        equal(100, first.getSatisfaction(), "Served customer receives no waiting penalty");
        manager.advanceTurn();
        manager.beginTurn(restaurant);
        cashier.onCollect = (r, turn) -> {
            equal(2, turn, "Second turn");
            for (Customer customer : List.of(first, second)) {
                equal(CustomerStatus.READY_TO_PAY, customer.getStatus(), "All eating finishes before Cashier");
                equal(2, customer.getReadyToPayTurn(), "Readiness timestamp is current turn");
            }
        };
        chooseBoth(manager);
        manager.resolveTurn(restaurant);
        equal(2, cashier.calls, "One call per resolution");
        equal(2, restaurant.getAbandonedCount(), "Departed customers not removed twice");
        manager.advanceTurn();
        manager.beginTurn(restaurant);
        cashier.onCollect = (r, turn) -> equal(2, first.getReadyToPayTurn(), "Readiness timestamp retained");
        chooseBoth(manager);
        manager.resolveTurn(restaurant);
    }

    private static void testAllTurnsAndCompletion() {
        RecordingCashier cashier = new RecordingCashier();
        Restaurant restaurant = new Restaurant();
        TurnManager manager = new TurnManager(cashier);
        List<Integer> arrivalTurns = new ArrayList<>();
        for (int turn = 1; turn <= 18; turn++) {
            equal(turn, manager.getCurrentTurn(), "Current turn");
            equal(false, manager.isFinished(), "Game unfinished before start");
            int before = restaurant.getCustomers().size();
            manager.beginTurn(restaurant);
            if (restaurant.getCustomers().size() != before) {
                arrivalTurns.add(turn);
            }
            equal((turn - 1) / 3 + 1, restaurant.getCustomers().size(), "Arrival count");
            rejected(manager, IllegalStateException.class, () -> manager.beginTurn(restaurant));
            chooseBoth(manager);
            manager.resolveTurn(restaurant);
            equal(turn, cashier.calls, "Cashier runs even on final turn");
            equal(turn, cashier.turns.get(turn - 1), "Cashier receives every turn in order");
            equal(false, manager.isFinished(), "Resolution alone never finishes game");
            if (turn == 18) {
                equal(76, restaurant.findCustomer(6).getSatisfaction(), "Final-turn penalty runs");
                rejected(manager, IllegalStateException.class, () -> manager.resolveTurn(restaurant));
                equal(18, cashier.calls, "Final resolution cannot repeat");
            }
            manager.advanceTurn();
        }
        equal(List.of(1, 4, 7, 10, 13, 16), arrivalTurns, "Exact arrival turns");
        equal(List.of(1, 2, 3, 4, 5, 6),
                restaurant.getCustomers().stream().map(Customer::getId).toList(), "Arrival IDs");
        equal(new State(18, true, false, false), State.capture(manager), "Final state");
        rejected(manager, IllegalStateException.class, () -> manager.beginTurn(restaurant));
        rejected(manager, IllegalStateException.class, () -> manager.resolveTurn(restaurant));
        rejected(manager, IllegalStateException.class, manager::advanceTurn);
        rejected(manager, IllegalStateException.class, manager::recordWaiterTask);
        rejected(manager, IllegalStateException.class, manager::recordChefTask);
        rejected(manager, IllegalArgumentException.class, () -> manager.beginTurn(null));
        rejected(manager, IllegalArgumentException.class, () -> manager.resolveTurn(null));
        equal(6, restaurant.getCustomers().size(), "No arrivals after completion");
        equal(18, cashier.calls, "No automatic events after completion");
    }

    private static void testCollaboratorFailureDoesNotResolve() {
        RecordingCashier cashier = new RecordingCashier();
        Restaurant restaurant = new Restaurant();
        TurnManager manager = new TurnManager(cashier);
        IllegalStateException failure = new IllegalStateException("Controlled Cashier failure");
        cashier.onCollect = (r, turn) -> { throw failure; };
        manager.beginTurn(restaurant);
        chooseBoth(manager);
        try {
            manager.resolveTurn(restaurant);
            throw new AssertionError("Expected collaborator failure");
        } catch (IllegalStateException actual) {
            same(failure, actual, "Original exception propagates");
        }
        equal(1, cashier.calls, "No automatic retry");
        equal(100, restaurant.findCustomer(1).getSatisfaction(), "Later phases do not run after failure");
        rejected(manager, IllegalStateException.class, manager::advanceTurn);
        equal(1, manager.getCurrentTurn(), "Failed resolution does not advance");
        equal(false, manager.isFinished(), "Failed resolution does not finish");
        // Deliberately do not retry: earlier phases may already have mutated collaborators.
    }

    private static Customer servedCustomer(int id, int turn) {
        Customer customer = new Customer(id);
        customer.seat();
        Order order = new Order(id, new MenuItem("Fixture dish", 900, 1));
        customer.acceptOrder(order);
        order.prepareOneUnit();
        order.markServed();
        customer.markServed(turn);
        return customer;
    }

    private static void chooseBoth(TurnManager manager) {
        manager.recordWaiterTask();
        manager.recordChefTask();
    }

    /** Observes sequencing only; does not select payers or implement payments. */
    private static class RecordingCashier extends Cashier {
        private int calls;
        private final List<Integer> turns = new ArrayList<>();
        private BiConsumer<Restaurant, Integer> onCollect = (r, turn) -> { };

        @Override
        public boolean collectOne(Restaurant r, int turn) {
            calls++;
            turns.add(turn);
            onCollect.accept(r, turn);
            return false;
        }
    }

    /**
     * Supplies standalone real served customers for eating-phase tests only.
     * This bypasses unavailable seating/serving result factories, and does not
     * claim to verify Restaurant occupancy, service statistics, or payment.
     * Queued arrivals and their abandonment still use the real Restaurant APIs.
     */
    private static class ActiveCustomerRestaurant extends Restaurant {
        private List<Customer> extraActive = List.of();

        @Override
        public List<Customer> getActiveCustomers() {
            List<Customer> customers = new ArrayList<>(super.getActiveCustomers());
            customers.addAll(extraActive);
            return List.copyOf(customers);
        }
    }

    private record State(int turn, boolean finished, boolean waiter, boolean chef) {
        static State capture(TurnManager manager) {
            return new State(manager.getCurrentTurn(), manager.isFinished(),
                    manager.canAssignWaiter(), manager.canAssignChef());
        }
    }

    private static void rejected(TurnManager manager, Class<? extends Throwable> type, Runnable action) {
        State before = State.capture(manager);
        expect(type, action);
        equal(before, State.capture(manager), "Rejected operation preserves public lifecycle state");
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

    private static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void same(Object expected, Object actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected identical references");
        }
    }
}
