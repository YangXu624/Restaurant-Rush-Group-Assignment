package restaurantrush.control;

import java.util.Objects;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.OrderStatus;
import restaurantrush.entity.Restaurant;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Host;
import restaurantrush.staff.Waiter;
import restaurantrush.support.ActionResult;
import restaurantrush.support.GameSnapshot;

/** Real seating and phase integration, plus a narrow Host delegation double. */
public class HostIntegrationTest {
    public static void main(String[] args) {
        testDelegationAndSharedHost();
        testRealSeatingAndRetry();
        System.out.println("All Host integration tests passed!");
    }

    private static void testDelegationAndSharedHost() {
        ActionResult result = ActionResult.fail("Controlled rejection");
        Restaurant restaurant = new Restaurant() {
            @Override
            public ActionResult seat(int customerId, int tableId) {
                equal(37, customerId, "Forward customer ID");
                equal(2, tableId, "Forward table ID");
                return result;
            }
        };
        if (new Host().seat(restaurant, 37, 2) != result) {
            throw new AssertionError("Host must return Restaurant's exact result");
        }
        int[] calls = {0};
        Host host = new Host() {
            @Override
            public ActionResult seat(Restaurant actual, int customerId, int tableId) {
                if (actual != restaurant) throw new AssertionError("Shared Restaurant required");
                calls[0]++;
                return super.seat(actual, customerId, tableId);
            }
        };
        TurnManager manager = new TurnManager(new Cashier());
        GameController game = new GameController(restaurant, manager, host, new Waiter(), new Chef());
        try {
            new GameController(restaurant, manager, null, new Waiter(), new Chef());
            throw new AssertionError("Null Host accepted");
        } catch (IllegalArgumentException expected) { }
        equal(false, game.hostSeatCustomer(37, 2).isSuccess(), "Before start rejected");
        equal(false, game.waitHost().isSuccess(), "Before start skip rejected");
        equal(0, calls[0], "Unavailable Host never invoked");
        game.startTurn();
        if (game.hostSeatCustomer(37, 2) != result) throw new AssertionError("Exact staff result required");
        equal(1, calls[0], "Supplied Host invoked once");
        equal(true, manager.canAssignHost(), "Failure preserves choice");
        equal(true, game.waitHost().isSuccess(), "Explicit skip accepted");
        equal(false, game.waitHost().isSuccess(), "Duplicate skip rejected");
        equal(false, game.hostSeatCustomer(37, 2).isSuccess(), "Seat after skip rejected");
        equal(1, calls[0], "Consumed Host never invoked");
    }

    private static void testRealSeatingAndRetry() {
        Restaurant restaurant = new Restaurant();
        TurnManager manager = new TurnManager(new Cashier());
        GameController game = new GameController(restaurant, manager, new Host(), new Waiter(), new Chef());
        game.startTurn();
        GameSnapshot before = game.getSnapshot();
        equal(false, game.seatCustomer(1, 1).isSuccess(), "Waiter cannot seat before Host");
        equal(false, game.takeOrder(1).isSuccess(), "Waiter cannot order before Host");
        equal(false, game.serveOrder(1).isSuccess(), "Waiter cannot serve before Host");
        equal(false, game.waitWaiter().isSuccess(), "Waiter cannot skip before Host");
        equal(false, game.prepareOrder(1).isSuccess(), "Chef blocked before Host");
        equal(false, game.waitChef().isSuccess(), "Chef skip blocked before Host");
        equal(false, game.hostSeatCustomer(99, 1).isSuccess(), "Invalid customer rejected");
        equal(before, game.getSnapshot(), "Rejected actions preserve game state");
        equal(true, manager.canAssignHost(), "Invalid customer preserves Host");
        equal(true, game.hostSeatCustomer(1, 1).isSuccess(), "Valid retry seats customer");
        equal(CustomerStatus.SEATED, restaurant.findCustomer(1).getStatus(), "Real customer seated");
        equal(0, restaurant.getWaitingQueue().size(), "Removed from queue");
        equal(restaurant.findCustomer(1), restaurant.getTables().get(0).getOccupant(), "Table assigned");
        equal(true, manager.canAssignWaiter(), "Host seating leaves Waiter choice");
        equal(false, game.waitHost().isSuccess(), "Seat consumes Host choice");
        equal(false, game.hostSeatCustomer(1, 2).isSuccess(), "Duplicate seat rejected");
        equal(false, game.prepareOrder(1).isSuccess(), "Chef still waits for Waiter");
        equal(true, game.takeOrder(1).isSuccess(), "Waiter orders same turn");
        equal(true, game.prepareOrder(1).isSuccess(), "Chef cooks same turn");
        equal(OrderStatus.READY, restaurant.findOrder(1).getStatus(), "Salad ready in first turn");
        game.finishTurn();
        game.startTurn();
        equal(true, manager.canAssignHost(), "Host resets next turn");
        restaurant.addArrival(new Customer(20));
        before = game.getSnapshot();
        equal(false, game.hostSeatCustomer(20, 1).isSuccess(), "Occupied table rejected");
        equal(before, game.getSnapshot(), "Occupied-table rejection changes nothing");
        equal(true, manager.canAssignHost(), "Occupied table preserves Host choice");
        equal(true, game.waitHost().isSuccess(), "Can skip after failed seat");
        equal(true, game.seatCustomer(20, 2).isSuccess(), "Waiter retains seating after Host skips");
        equal(true, game.waitChef().isSuccess(), "Chef can skip");
        game.finishTurn();
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
