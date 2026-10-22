package restaurantrush.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import restaurantrush.control.TurnManager;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.OrderStatus;
import restaurantrush.entity.Restaurant;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Waiter;

/** Projection and immutability tests using real collaborators, without GameController. */
public class GameSnapshotTest {
    public static void main(String[] args) {
        testDirectConstructionCopiesLists();
        testNullInputs();
        testInitialStateAndTurnPhases();
        testProjectionAndDetachment();
        testPaidStatistics();
        System.out.println("All GameSnapshot tests passed successfully!");
    }

    private static void testDirectConstructionCopiesLists() {
        List<Integer> waiting = new ArrayList<>(List.of(1));
        List<CustomerView> customers = new ArrayList<>(List.of(
                new CustomerView(1, 100, CustomerStatus.WAITING, null, null, null, null)));
        List<TableView> tables = new ArrayList<>(List.of(new TableView(1, null)));
        GameSnapshot snapshot = snapshotWith(waiting, customers, tables);
        waiting.clear();
        customers.clear();
        tables.clear();
        equal(List.of(1), snapshot.waitingCustomerIds(), "Copy input queue IDs");
        equal(1, snapshot.customers().size(), "Copy input customer views");
        equal(1, snapshot.tables().size(), "Copy input table views");
        assertUnmodifiable(snapshot);
    }

    private static void testNullInputs() {
        expect(NullPointerException.class, () -> snapshotWith(null, List.of(), List.of()));
        expect(NullPointerException.class, () -> snapshotWith(List.of(), null, List.of()));
        expect(NullPointerException.class, () -> snapshotWith(List.of(), List.of(), null));
        expect(NullPointerException.class, () -> snapshotWith(Arrays.asList((Integer) null), List.of(), List.of()));
        expect(NullPointerException.class, () -> snapshotWith(List.of(), Arrays.asList((CustomerView) null), List.of()));
        expect(NullPointerException.class, () -> snapshotWith(List.of(), List.of(), Arrays.asList((TableView) null)));
        expect(IllegalArgumentException.class, () -> GameSnapshot.from(null, new TurnManager(new Cashier())));
        expect(IllegalArgumentException.class, () -> GameSnapshot.from(new Restaurant(), null));
    }

    private static void testInitialStateAndTurnPhases() {
        Restaurant restaurant = new Restaurant();
        TurnManager manager = new TurnManager(new Cashier());
        GameSnapshot initial = GameSnapshot.from(restaurant, manager);
        equal(1, initial.turn(), "Initial turn");
        equal(List.of(), initial.customers(), "No customers before arrivals");
        equal(List.of(), initial.waitingCustomerIds(), "Initially empty queue");
        equal(List.of(new TableView(1, null), new TableView(2, null)), initial.tables(), "Two free tables in order");
        equal(null, initial.averagePaidSatisfaction(), "No payments means N/A");
        equal(0, initial.revenueCents(), "Initial revenue");
        equal(0, initial.paidCount(), "Initial payments");
        equal(0, initial.servedCount(), "Initial service count");
        equal(0, initial.abandonedCount(), "Initial departures");
        availability(initial, false, false);

        manager.beginTurn(restaurant);
        GameSnapshot start = GameSnapshot.from(restaurant, manager);
        availability(start, true, false);
        equal(List.of(1), start.waitingCustomerIds(), "Arrival is visible");
        equal(new CustomerView(1, 100, CustomerStatus.WAITING, null, null, null, null),
                start.customers().get(0), "Unordered customer has absent order and table fields");
        equal(start, GameSnapshot.from(restaurant, manager), "Repeated capture does not change game state");
        manager.recordWaiterTask();
        availability(GameSnapshot.from(restaurant, manager), false, true);
        manager.recordChefTask();
        availability(GameSnapshot.from(restaurant, manager), false, false);
        manager.resolveTurn(restaurant);
        availability(GameSnapshot.from(restaurant, manager), false, false);
        manager.advanceTurn();
        GameSnapshot advanced = GameSnapshot.from(restaurant, manager);
        equal(2, advanced.turn(), "Projection uses current turn after advance");
        availability(advanced, false, false);
        equal(100, start.customers().get(0).satisfaction(), "Earlier snapshot survives waiting penalty");
        equal(92, advanced.customers().get(0).satisfaction(), "New snapshot sees waiting penalty");

        for (int turn = 2; turn <= 18; turn++) {
            manager.beginTurn(restaurant);
            manager.recordWaiterTask();
            manager.recordChefTask();
            manager.resolveTurn(restaurant);
            manager.advanceTurn();
        }
        GameSnapshot finished = GameSnapshot.from(restaurant, manager);
        equal(true, manager.isFinished(), "Fixture completes game");
        equal(18, finished.turn(), "Finished game stays on turn 18");
        availability(finished, false, false);
    }

    private static void testProjectionAndDetachment() {
        Restaurant restaurant = new Restaurant();
        TurnManager manager = new TurnManager(new Cashier());
        for (int id : new int[] {3, 2, 1}) {
            restaurant.addArrival(new Customer(id));
        }
        Waiter waiter = new Waiter();
        Chef chef = new Chef();
        accepted(waiter.seat(restaurant, 3, 2));
        accepted(waiter.takeOrder(restaurant, 3));
        accepted(chef.prepareOneUnit(restaurant.findOrder(3)));
        GameSnapshot preparing = GameSnapshot.from(restaurant, manager);
        equal(List.of(2, 1), preparing.waitingCustomerIds(), "Preserve queue order rather than sorting IDs");
        equal(List.of(3, 2, 1), preparing.customers().stream().map(CustomerView::id).toList(), "Preserve arrival order");
        equal(new CustomerView(3, 100, CustomerStatus.SEATED, OrderStatus.PREPARING, "Pasta", 1, 2),
                preparing.customers().get(0), "Map dish, progress and actual table");
        equal(List.of(new TableView(1, null), new TableView(2, 3)), preparing.tables(), "Map occupied and free tables");
        assertUnmodifiable(preparing);

        accepted(chef.prepareOneUnit(restaurant.findOrder(3)));
        equal(OrderStatus.READY, GameSnapshot.from(restaurant, manager).customers().get(0).orderStatus(), "New capture sees READY");
        accepted(waiter.serve(restaurant, 3, 1));
        restaurant.findCustomer(3).finishEating(2);
        equal(true, new Cashier().collectOne(restaurant, 3), "Real payment succeeds");
        // Give customer 2 an order before abandonment to retain cancelled order history.
        accepted(waiter.seat(restaurant, 2, 1));
        accepted(waiter.takeOrder(restaurant, 2));
        for (int i = 0; i < 13; i++) {
            restaurant.findCustomer(2).applyWaitingPenalty();
        }
        restaurant.removeAbandoned(restaurant.findCustomer(2));
        GameSnapshot historical = GameSnapshot.from(restaurant, manager);
        equal(new CustomerView(3, 100, CustomerStatus.PAID, OrderStatus.PAID, "Pasta", 0, null),
                historical.customers().get(0), "Retain paid order but clear table");
        equal(new CustomerView(2, 0, CustomerStatus.LEFT, OrderStatus.CANCELLED, "Burger", 1, null),
                historical.customers().get(1), "Retain cancelled order but clear table");
        equal(List.of(1), historical.waitingCustomerIds(), "Only queued customer remains");
        equal(List.of(new TableView(1, null), new TableView(2, null)), historical.tables(), "Payment and departure free tables");
        equal(1, historical.paidCount(), "Map payments");
        equal(1, historical.servedCount(), "Map service count");
        equal(1, historical.abandonedCount(), "Map departure count");
        equal(1800, historical.revenueCents(), "Only paid bill enters revenue");
        equal(100.0, historical.averagePaidSatisfaction(), "Map paid satisfaction");
        equal(new CustomerView(3, 100, CustomerStatus.SEATED, OrderStatus.PREPARING, "Pasta", 1, 2),
                preparing.customers().get(0), "Old order and customer data remain detached");
        equal(List.of(2, 1), preparing.waitingCustomerIds(), "Old queue remains detached");
        equal(new TableView(2, 3), preparing.tables().get(1), "Old occupancy remains detached");
        equal(0, preparing.revenueCents(), "Old revenue remains detached");
        equal(null, preparing.averagePaidSatisfaction(), "Old N/A average remains detached");
    }

    private static void testPaidStatistics() {
        Restaurant restaurant = new Restaurant();
        Waiter waiter = new Waiter();
        Chef chef = new Chef();
        // Set up real payment statistics directly; this is a projection test, not a full-game replay.
        for (int id = 1; id <= 3; id++) {
            Customer customer = new Customer(id);
            restaurant.addArrival(customer);
            accepted(waiter.seat(restaurant, id, 1));
            accepted(waiter.takeOrder(restaurant, id));
            while (customer.getOrder().canPrepare()) {
                accepted(chef.prepareOneUnit(customer.getOrder()));
            }
            for (int i = 0; i < (id == 1 ? 1 : 2); i++) {
                customer.applyWaitingPenalty();
            }
            accepted(waiter.serve(restaurant, id, 1));
            customer.finishEating(2);
            equal(true, restaurant.recordPayment(customer, 3), "Fixture payment");
        }
        GameSnapshot snapshot = GameSnapshot.from(restaurant, new TurnManager(new Cashier()));
        equal(3, snapshot.paidCount(), "All three payments projected");
        equal(4200, snapshot.revenueCents(), "Sum actual menu bills in cents");
        equal(260.0 / 3, snapshot.averagePaidSatisfaction(), "Preserve fractional paid average without rounding");
    }

    private static GameSnapshot snapshotWith(List<Integer> waiting, List<CustomerView> customers, List<TableView> tables) {
        return new GameSnapshot(1, 0, null, waiting, customers, tables, false, false, 0, 0, 0);
    }

    private static void assertUnmodifiable(GameSnapshot snapshot) {
        expect(UnsupportedOperationException.class, () -> snapshot.waitingCustomerIds().add(99));
        expect(UnsupportedOperationException.class, () -> snapshot.customers().clear());
        expect(UnsupportedOperationException.class, () -> snapshot.tables().set(0, new TableView(1, 99)));
    }

    private static void availability(GameSnapshot snapshot, boolean waiter, boolean chef) {
        equal(waiter, snapshot.waiterAvailable(), "Waiter phase availability");
        equal(chef, snapshot.chefAvailable(), "Chef phase availability");
    }

    private static void accepted(ActionResult result) {
        equal(true, result.isSuccess(), result.getMessage());
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
            throw new AssertionError("Expected " + type.getSimpleName(), failure);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
