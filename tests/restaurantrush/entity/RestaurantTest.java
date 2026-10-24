package restaurantrush.entity;

import java.util.Objects;

/** Regression tests that failed Restaurant operations leave no partial state changes. */
public class RestaurantTest {
    public static void main(String[] args) {
        testInvalidServeTurnChangesNothing();
        testFailedAbandonmentKeepsTable();
        testPaymentRequiresRegisteredOrder();
        System.out.println("All Restaurant tests passed successfully!");
    }

    private static void testInvalidServeTurnChangesNothing() {
        Restaurant restaurant = new Restaurant();
        Customer customer = seatedCustomer(restaurant);
        Order order = registeredOrder(restaurant, customer);
        order.prepareOneUnit(); // Salad needs one unit, so it is now READY.

        assertThrows(IllegalArgumentException.class, () -> restaurant.serve(1, 0), "Turn 0 is rejected");
        assertEqual(OrderStatus.READY, order.getStatus(), "Order stays READY after rejected serve");
        assertEqual(CustomerStatus.SEATED, customer.getStatus(), "Customer stays SEATED after rejected serve");
        assertEqual(-1, customer.getServedTurn(), "No service timestamp after rejected serve");
        assertEqual(0, restaurant.getServedCount(), "Served count unchanged after rejected serve");

        assertEqual(true, restaurant.serve(1, 1).isSuccess(), "Retry with a valid turn succeeds");
        assertEqual(CustomerStatus.SERVED, customer.getStatus(), "Customer served on retry");
        assertEqual(1, restaurant.getServedCount(), "Served count updated on retry");
    }

    private static void testFailedAbandonmentKeepsTable() {
        Restaurant restaurant = new Restaurant();
        Customer customer = seatedCustomer(restaurant);
        Order order = registeredOrder(restaurant, customer);
        drainSatisfaction(customer);
        order.cancel(); // Abandonment can no longer cancel the order.

        assertThrows(IllegalStateException.class, () -> restaurant.removeAbandoned(customer),
                "Abandonment with an uncancellable order is rejected");
        assertEqual(CustomerStatus.SEATED, customer.getStatus(), "Customer stays SEATED after failed abandonment");
        assertSame(customer, restaurant.getTables().get(0).getOccupant(), "Table stays occupied");
        assertEqual(0, restaurant.getAbandonedCount(), "Abandoned count unchanged");

        // A valid abandonment still frees the table and cancels the order.
        Restaurant valid = new Restaurant();
        Customer leaving = seatedCustomer(valid);
        Order leavingOrder = registeredOrder(valid, leaving);
        drainSatisfaction(leaving);
        valid.removeAbandoned(leaving);
        assertEqual(CustomerStatus.LEFT, leaving.getStatus(), "Customer leaves");
        assertEqual(OrderStatus.CANCELLED, leavingOrder.getStatus(), "Order cancelled on departure");
        assertEqual(true, valid.getTables().get(0).isAvailable(), "Table freed on departure");
        assertEqual(1, valid.getAbandonedCount(), "Departure counted once");
    }

    private static void testPaymentRequiresRegisteredOrder() {
        Restaurant restaurant = new Restaurant();
        Customer customer = seatedCustomer(restaurant);
        Order unregistered = new Order(1, restaurant.getMenu().get(0));
        customer.acceptOrder(unregistered); // Linked to the customer but never registered.
        unregistered.prepareOneUnit();
        unregistered.markServed();
        customer.markServed(1);
        customer.finishEating(2);

        assertEqual(false, restaurant.recordPayment(customer, 3), "Unregistered order cannot be paid");
        assertEqual(OrderStatus.SERVED, unregistered.getStatus(), "Order stays SERVED");
        assertEqual(CustomerStatus.READY_TO_PAY, customer.getStatus(), "Customer stays READY_TO_PAY");
        assertSame(customer, restaurant.getTables().get(0).getOccupant(), "Table stays occupied");
        assertEqual(0, restaurant.getRevenueCents(), "No revenue from unregistered order");
        assertEqual(0, restaurant.getPaidCount(), "Paid count unchanged");

        // A registered order on the same path is still paid.
        Restaurant valid = new Restaurant();
        Customer payer = seatedCustomer(valid);
        Order order = registeredOrder(valid, payer);
        order.prepareOneUnit();
        assertEqual(true, valid.serve(1, 1).isSuccess(), "Registered order is served");
        payer.finishEating(2);
        assertEqual(true, valid.recordPayment(payer, 3), "Registered order is paid");
        assertEqual(900, valid.getRevenueCents(), "Salad bill added to revenue");
        assertEqual(1, valid.getPaidCount(), "Payment counted once");
    }

    /** Customer 1 arrives and sits at table 1. */
    private static Customer seatedCustomer(Restaurant restaurant) {
        Customer customer = new Customer(1);
        restaurant.addArrival(customer);
        assertEqual(true, restaurant.seat(1, 1).isSuccess(), "Customer 1 is seated");
        return customer;
    }

    /** Links and registers the customer's order, as the Waiter does. */
    private static Order registeredOrder(Restaurant restaurant, Customer customer) {
        Order order = new Order(customer.getId(), customer.chooseItem(restaurant.getMenu()));
        customer.acceptOrder(order);
        restaurant.registerOrder(order);
        return order;
    }

    private static void drainSatisfaction(Customer customer) {
        while (customer.getSatisfaction() > 0) {
            customer.applyWaitingPenalty();
        }
    }

    private static void assertThrows(Class<? extends Throwable> expected, Runnable action, String message) {
        try {
            action.run();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown)) {
                return;
            }
            throw new AssertionError(message + ": expected " + expected.getSimpleName()
                    + " but got " + thrown, thrown);
        }
        throw new AssertionError(message + ": expected " + expected.getSimpleName());
    }

    private static void assertEqual(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertSame(Object expected, Object actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected the same object");
        }
    }
}
