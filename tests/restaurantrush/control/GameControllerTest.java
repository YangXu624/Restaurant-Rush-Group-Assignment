package restaurantrush.control;

import java.util.Objects;
import restaurantrush.entity.Customer;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Waiter;
import restaurantrush.support.ActionResult;

/**
 * Standalone M4 coordination tests using real ActionResult factories and staff doubles.
 * These tests require the M3 ActionResult implementation. Controller-generated
 * failures, explicit waits, snapshots and real staff integration are not covered.
 */
public class GameControllerTest {
    public static void main(String[] args) {
        testConstructorNulls();
        testStartAndFinishWithSharedState();
        testWaiterForwardingAndChoices();
        testChefForwardingAndChoices();
        testResolutionFailure();
        testStaffExceptionsPropagate();
        testCompletionAndVictory();
        System.out.println("All GameController coordination tests passed; "
                + "real ActionResult factories exercised; controller-generated failures, explicit waits, "
                + "snapshots and real staff integration are not covered.");
    }

    private static void testConstructorNulls() {
        Fixture f = new Fixture();
        expectIllegalArgument(() -> new GameController(null, f.manager, f.waiter, f.chef));
        expectIllegalArgument(() -> new GameController(f.restaurant, null, f.waiter, f.chef));
        expectIllegalArgument(() -> new GameController(f.restaurant, f.manager, null, f.chef));
        expectIllegalArgument(() -> new GameController(f.restaurant, f.manager, f.waiter, null));
    }

    private static void testStartAndFinishWithSharedState() {
        Fixture f = new Fixture();
        equal(false, f.controller.isFinished(), "Initially unfinished");
        f.controller.startTurn();
        equal(1, f.restaurant.getCustomers().size(), "Start adds arrival to supplied restaurant");
        equal(true, f.manager.canAssignWaiter(), "Supplied manager starts");
        f.manager.recordWaiterTask();
        f.manager.recordChefTask();
        f.controller.finishTurn();
        same(f.restaurant, f.cashier.restaurant, "Resolution uses supplied restaurant");
        equal(1, f.cashier.turn, "Resolution occurs on original turn");
        equal(1, f.cashier.calls, "Resolve once");
        equal(92, f.restaurant.findCustomer(1).getSatisfaction(), "Real resolution applies penalty");
        equal(2, f.manager.getCurrentTurn(), "Advance after resolution");
        equal(false, f.manager.canAssignWaiter(), "Advanced turn is closed");
        f.controller.startTurn();
        equal(true, f.manager.canAssignWaiter(), "Same manager begins next turn");
        equal(1, f.restaurant.getCustomers().size(), "No extra arrival on turn two");
    }

    private static void testWaiterForwardingAndChoices() {
        for (String operation : new String[] {"seat", "order", "serve"}) {
            for (boolean success : new boolean[] {false, true}) {
                Fixture f = new Fixture();
                // Exercise a noninitial service turn using the real manager.
                f.controller.startTurn();
                f.manager.recordWaiterTask();
                f.manager.recordChefTask();
                f.controller.finishTurn();
                f.controller.startTurn();
                f.waiter.result = success ? ActionResult.ok("Staff test result") : ActionResult.fail("Staff test result");
                ActionResult actual = switch (operation) {
                    case "seat" -> f.controller.seatCustomer(37, 2);
                    case "order" -> f.controller.takeOrder(37);
                    default -> f.controller.serveOrder(37);
                };
                same(f.waiter.result, actual, "Return exact Waiter result");
                equal(operation, f.waiter.operation, "Correct Waiter method");
                same(f.restaurant, f.waiter.restaurant, "Forward shared restaurant");
                equal(37, f.waiter.customerId, "Forward customer ID without controller validation");
                equal(1, f.waiter.calls, "Invoke staff once");
                if (operation.equals("seat")) {
                    equal(2, f.waiter.tableId, "Forward table ID");
                }
                if (operation.equals("serve")) {
                    equal(2, f.waiter.turn, "Read service turn from manager");
                }
                equal(!success, f.manager.canAssignWaiter(), "Only success consumes Waiter");
                equal(success, f.manager.canAssignChef(), "Success unlocks Chef without consuming it");
                equal(0, f.chef.calls, "Waiter request never invokes Chef");
                equal(0, f.restaurant.getServedCount(), "Controller performs no serving mutation");
            }
        }
    }

    private static void testChefForwardingAndChoices() {
        for (boolean success : new boolean[] {false, true}) {
            Fixture f = new Fixture();
            f.controller.startTurn();
            Customer customer = f.restaurant.findCustomer(1);
            // Set up the registered order directly to isolate controller forwarding from seating.
            customer.seat();
            Order order = new Order(1, f.restaurant.getMenu().get(2));
            customer.acceptOrder(order);
            f.restaurant.registerOrder(order);
            f.manager.recordWaiterTask();
            f.chef.result = success ? ActionResult.ok("Staff test result") : ActionResult.fail("Staff test result");
            same(f.chef.result, f.controller.prepareOrder(1), "Return exact Chef result");
            same(order, f.chef.order, "Find and forward registered order instance");
            equal(1, f.chef.calls, "Invoke Chef once");
            equal(!success, f.manager.canAssignChef(), "Only success consumes Chef");
            equal(false, f.manager.canAssignWaiter(), "Waiter stays consumed");
            equal(2, order.getRemainingUnits(), "Controller does not prepare the order itself");
            equal(0, f.waiter.calls, "Cooking does not invoke Waiter");
        }
    }

    private static void testResolutionFailure() {
        Fixture f = new Fixture();
        f.controller.startTurn();
        f.manager.recordWaiterTask();
        f.manager.recordChefTask();
        IllegalStateException failure = new IllegalStateException("Cashier failed");
        f.cashier.failure = failure;
        expectSameFailure(failure, f.controller::finishTurn);
        same(f.restaurant, f.cashier.restaurant, "Failed resolution uses shared restaurant");
        equal(1, f.cashier.calls, "No resolution retries");
        equal(1, f.manager.getCurrentTurn(), "Failure does not advance");
        equal(false, f.controller.isFinished(), "Failure does not finish");
        equal(100, f.restaurant.findCustomer(1).getSatisfaction(), "No later penalty phase");
    }

    private static void testStaffExceptionsPropagate() {
        Fixture f = new Fixture();
        f.controller.startTurn();
        IllegalStateException failure = new IllegalStateException("Staff programming defect");
        f.waiter.failure = failure;
        expectSameFailure(failure, () -> f.controller.seatCustomer(1, 1));
        expectSameFailure(failure, () -> f.controller.takeOrder(1));
        expectSameFailure(failure, () -> f.controller.serveOrder(1));
        equal(true, f.manager.canAssignWaiter(), "Exceptions do not consume choice");
    }

    private static void testCompletionAndVictory() {
        StatisticsRestaurant restaurant = new StatisticsRestaurant();
        TurnManager manager = new TurnManager(new RecordingCashier());
        GameController controller = new GameController(restaurant, manager, new Waiter(), new Chef());
        for (int turn = 1; turn <= 18; turn++) {
            controller.startTurn();
            equal(false, controller.isFinished(), "Game remains active through turn 18");
            equal(false, controller.isVictory(), "Meeting thresholds during play is not victory");
            manager.recordWaiterTask();
            manager.recordChefTask();
            controller.finishTurn();
            equal(turn == 18, controller.isFinished(), "Completion follows real manager");
        }
        equal(true, controller.isVictory(), "Exact thresholds succeed");
        restaurant.paid = 3;
        equal(false, controller.isVictory(), "Paid count independently required");
        restaurant.paid = 4;
        restaurant.revenue = 4499;
        equal(false, controller.isVictory(), "Revenue independently required");
        restaurant.revenue = 4500;
        restaurant.average = 59.99;
        equal(false, controller.isVictory(), "Average independently required without rounding");
        restaurant.average = 60;
        equal(true, controller.isVictory(), "Restored exact thresholds succeed");
        restaurant.paid = 5;
        restaurant.revenue = 6000;
        restaurant.average = 80;
        equal(true, controller.isVictory(), "Above thresholds succeeds");
    }

    private static final class Fixture {
        final Restaurant restaurant = new Restaurant();
        final RecordingCashier cashier = new RecordingCashier();
        final TurnManager manager = new TurnManager(cashier);
        final RecordingWaiter waiter = new RecordingWaiter();
        final RecordingChef chef = new RecordingChef();
        final GameController controller = new GameController(restaurant, manager, waiter, chef);
    }

    private static final class RecordingWaiter extends Waiter {
        ActionResult result;
        Restaurant restaurant;
        String operation;
        int customerId;
        int tableId;
        int turn;
        int calls;
        RuntimeException failure;
        private ActionResult record(String name, Restaurant r, int id) {
            operation = name;
            restaurant = r;
            customerId = id;
            calls++;
            if (failure != null) { throw failure; }
            return result;
        }
        @Override public ActionResult seat(Restaurant r, int id, int table) {
            tableId = table;
            return record("seat", r, id);
        }
        @Override public ActionResult takeOrder(Restaurant r, int id) {
            return record("order", r, id);
        }
        @Override public ActionResult serve(Restaurant r, int id, int currentTurn) {
            turn = currentTurn;
            return record("serve", r, id);
        }
    }

    private static final class RecordingChef extends Chef {
        ActionResult result;
        Order order;
        int calls;
        @Override public ActionResult prepareOneUnit(Order o) {
            order = o;
            calls++;
            return result;
        }
    }

    private static final class RecordingCashier extends Cashier {
        Restaurant restaurant;
        int turn;
        int calls;
        RuntimeException failure;
        @Override public boolean collectOne(Restaurant r, int currentTurn) {
            restaurant = r;
            turn = currentTurn;
            calls++;
            if (failure != null) { throw failure; }
            return false;
        }
    }

    /** Supplies boundary statistics without pretending to exercise real payment integration. */
    private static final class StatisticsRestaurant extends Restaurant {
        int paid = 4;
        int revenue = 4500;
        double average = 60;
        @Override public int getPaidCount() { return paid; }
        @Override public int getRevenueCents() { return revenue; }
        @Override public double averagePaidSatisfaction() { return average; }
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void same(Object expected, Object actual, String message) {
        if (expected != actual) { throw new AssertionError(message); }
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void expectSameFailure(RuntimeException expected, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException actual) {
            same(expected, actual, "Original exception propagates");
            return;
        }
        throw new AssertionError("Expected original exception");
    }
}
