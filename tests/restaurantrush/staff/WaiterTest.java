package restaurantrush.staff;

import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.Order;
import restaurantrush.entity.OrderStatus;
import restaurantrush.entity.Restaurant;
import restaurantrush.support.ActionResult;

public class WaiterTest {

    public static void main(String[] args) {
        testSeatDelegatesToRestaurant();
        testSeatFailureChangesNothing();

        testTakeOrderCreatesSharedOrder();
        testTakeOrderUsesCustomerMenuChoice();
        testTakeOrderUnknownCustomerFails();
        testTakeOrderWaitingCustomerFails();
        testTakeOrderTwiceFails();

        testServeReadyOrder();
        testServeNotReadyFails();

        System.out.println("All Waiter tests passed successfully!");
    }

    private static void testSeatDelegatesToRestaurant() {
        Restaurant r = new Restaurant();
        Customer c = arrive(r, 1);

        ActionResult result = new Waiter().seat(r, 1, 1);

        assertTrue(result.isSuccess(), "Seating a waiting customer at a free table should succeed");
        assertEqual(CustomerStatus.SEATED, c.getStatus(), "Customer should be SEATED");
        assertFalse(r.getWaitingQueue().contains(c), "Customer should leave the queue");
        assertEqual(c, r.getTables().get(0).getOccupant(), "Table 1 should hold the customer");
    }

    private static void testSeatFailureChangesNothing() {
        Restaurant r = new Restaurant();
        Customer c = arrive(r, 1);

        ActionResult result = new Waiter().seat(r, 1, 3);

        assertFalse(result.isSuccess(), "Seating at a missing table should fail");
        assertEqual(CustomerStatus.WAITING, c.getStatus(), "Customer should still be WAITING");
        assertTrue(r.getWaitingQueue().contains(c), "Customer should still be queued");
    }

    private static void testTakeOrderCreatesSharedOrder() {
        Restaurant r = new Restaurant();
        Customer c = seated(r, 1);

        ActionResult result = new Waiter().takeOrder(r, 1);

        assertTrue(result.isSuccess(), "Taking a seated customer's order should succeed");
        assertTrue(result.getMessage().contains("Salad"), "Message should name the dish");
        Order order = c.getOrder();
        assertTrue(order != null, "Customer should have an order");
        assertEqual(1, order.getId(), "Order ID should match customer ID");
        assertEqual(OrderStatus.PLACED, order.getStatus(), "New order should be PLACED");
        assertTrue(r.findOrder(1) == order, "Registry and customer should share one order");
        assertEqual(1, r.getOrders().size(), "Exactly one order should be registered");
    }

    private static void testTakeOrderUsesCustomerMenuChoice() {
        Restaurant r = new Restaurant();
        arrive(r, 3);
        r.seat(3, 1);

        new Waiter().takeOrder(r, 3);

        assertEqual("Pasta", r.findOrder(3).getItem().getName(), "Customer 3 should order Pasta");
    }

    private static void testTakeOrderUnknownCustomerFails() {
        Restaurant r = new Restaurant();

        ActionResult result = new Waiter().takeOrder(r, 9);

        assertFalse(result.isSuccess(), "Unknown customer should fail");
        assertEqual(0, r.getOrders().size(), "No order should be registered");
    }

    private static void testTakeOrderWaitingCustomerFails() {
        Restaurant r = new Restaurant();
        Customer c = arrive(r, 1);

        ActionResult result = new Waiter().takeOrder(r, 1);

        assertFalse(result.isSuccess(), "Waiting customer cannot order");
        assertTrue(c.getOrder() == null, "Customer should have no order");
        assertEqual(0, r.getOrders().size(), "No order should be registered");
    }

    private static void testTakeOrderTwiceFails() {
        Restaurant r = new Restaurant();
        Customer c = seated(r, 1);
        Waiter waiter = new Waiter();
        waiter.takeOrder(r, 1);
        Order first = c.getOrder();

        ActionResult result = waiter.takeOrder(r, 1);

        assertFalse(result.isSuccess(), "Second order should fail");
        assertTrue(c.getOrder() == first, "Original order should be kept");
        assertEqual(1, r.getOrders().size(), "Still exactly one order registered");
    }

    private static void testServeReadyOrder() {
        Restaurant r = new Restaurant();
        Customer c = seated(r, 1);
        Waiter waiter = new Waiter();
        waiter.takeOrder(r, 1);
        new Chef().prepareOneUnit(c.getOrder());

        ActionResult result = waiter.serve(r, 1, 2);

        assertTrue(result.isSuccess(), "Serving a READY order should succeed");
        assertEqual(CustomerStatus.SERVED, c.getStatus(), "Customer should be SERVED");
        assertEqual(OrderStatus.SERVED, c.getOrder().getStatus(), "Order should be SERVED");
        assertEqual(1, r.getServedCount(), "Served count should be 1");
    }

    private static void testServeNotReadyFails() {
        Restaurant r = new Restaurant();
        Customer c = seated(r, 1);
        Waiter waiter = new Waiter();
        waiter.takeOrder(r, 1);

        ActionResult result = waiter.serve(r, 1, 2);

        assertFalse(result.isSuccess(), "Serving a PLACED order should fail");
        assertEqual(CustomerStatus.SEATED, c.getStatus(), "Customer should stay SEATED");
        assertEqual(0, r.getServedCount(), "Served count should stay 0");
    }

    private static Customer arrive(Restaurant r, int id) {
        Customer c = new Customer(id);
        r.addArrival(c);
        return c;
    }

    private static Customer seated(Restaurant r, int id) {
        Customer c = arrive(r, id);
        r.seat(id, 1);
        return c;
    }

    private static void assertEqual(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message + " - Expected true, got false");
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message + " - Expected false, got true");
        }
    }
}
