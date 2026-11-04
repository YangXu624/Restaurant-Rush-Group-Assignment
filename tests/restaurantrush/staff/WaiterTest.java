package restaurantrush.staff;

import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.CriticCustomer;
import restaurantrush.entity.MenuItem;
import restaurantrush.entity.Order;
import restaurantrush.entity.OrderStatus;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.VIPCustomer;
import restaurantrush.support.ActionResult;

public class WaiterTest {

    public static void main(String[] args) {
        testSeatDelegatesToRestaurant();
        testSeatFailureChangesNothing();

        testTakeOrderCreatesSharedOrder();
        testTakeOrderUsesCustomerMenuChoice();
        testVipOrderAndPaymentUseOneDiscount();
        testCriticOrderUsesPreferenceAndFullPrice();
        testPaymentDoesNotRecalculateCustomerPrice();
        testInvalidPricingDoesNotLinkOrRegisterOrder();
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
        assertEqual(900, order.getBillCents(), "Regular order uses the full menu price");
        assertTrue(r.findOrder(1) == order, "Registry and customer should share one order");
        assertEqual(1, r.getOrders().size(), "Exactly one order should be registered");
    }

    private static void testTakeOrderUsesCustomerMenuChoice() {
        Restaurant r = new Restaurant();
        arrive(r, 3);
        r.seat(3, 1);

        new Waiter().takeOrder(r, 3);

        arrive(r, 4);
        r.seat(4, 2);
        new Waiter().takeOrder(r, 4);
        assertEqual("Salad + Pasta", r.findOrder(4).getItem().getName(), "Customer 4 should order the combo");
        assertEqual("Pasta", r.findOrder(3).getItem().getName(), "Customer 3 should order Pasta");
    }

    private static void testVipOrderAndPaymentUseOneDiscount() {
        Restaurant r = new Restaurant();
        Customer vip = new VIPCustomer(2);
        r.addArrival(vip);
        Waiter waiter = new Waiter();
        assertTrue(waiter.seat(r, 2, 1).isSuccess(), "VIP can be seated normally");
        assertTrue(waiter.takeOrder(r, 2).isSuccess(), "VIP can place an order normally");
        Order order = vip.getOrder();
        assertTrue(r.findOrder(2) == order, "VIP and restaurant share the same order");
        assertEqual(2, order.getId(), "VIP order ID matches its customer");
        assertEqual("Burger", order.getItem().getName(), "VIP retains deterministic menu choice");
        assertEqual(1500, order.getItem().getPriceCents(), "VIP does not change the menu price");
        assertEqual(1350, order.getBillCents(), "VIP bill is discounted at order placement");
        assertEqual(0, r.getRevenueCents(), "Placing an order does not collect payment");
        assertFalse(waiter.takeOrder(r, 2).isSuccess(), "VIP cannot order twice");
        assertTrue(vip.getOrder() == order && r.findOrder(2) == order, "Rejected repeat keeps shared order");
        assertEqual(1, r.getOrders().size(), "Rejected repeat does not register another order");
        assertEqual(1350, order.getBillCents(), "Rejected repeat does not discount again");

        assertTrue(new Chef().prepareOneUnit(order).isSuccess(), "VIP Burger needs one preparation unit");
        assertTrue(waiter.serve(r, 2, 2).isSuccess(), "VIP order can be served normally");
        vip.finishEating(3);
        Cashier cashier = new Cashier();
        assertFalse(cashier.collectOne(r, 3), "VIP cannot pay on the turn it becomes ready");
        assertEqual(0, r.getRevenueCents(), "Revenue remains zero before eligibility");
        assertTrue(cashier.collectOne(r, 4), "Cashier collects the VIP bill on a later turn");
        assertEqual(1350, r.getRevenueCents(), "Payment uses the locked bill without a second discount");
        assertEqual(1350, order.getBillCents(), "Payment preserves the locked bill");
        assertEqual(CustomerStatus.PAID, vip.getStatus(), "VIP is marked paid");
        assertEqual(OrderStatus.PAID, order.getStatus(), "VIP order is marked paid");
        assertEqual(1, r.getPaidCount(), "VIP payment counted once");
        assertTrue(r.getTables().get(0).isAvailable(), "VIP payment releases its table");
        assertFalse(cashier.collectOne(r, 4), "Cannot collect twice in the same turn");
        assertFalse(cashier.collectOne(r, 5), "Paid VIP cannot be charged on a later turn");
        assertEqual(1350, r.getRevenueCents(), "Rejected collections do not add revenue");
    }

    private static void testCriticOrderUsesPreferenceAndFullPrice() {
        Restaurant r = new Restaurant();
        Customer critic = new CriticCustomer(5);
        r.addArrival(critic);
        Waiter waiter = new Waiter();
        assertTrue(waiter.seat(r, 5, 1).isSuccess(), "Critic can be seated normally");
        assertTrue(waiter.takeOrder(r, 5).isSuccess(), "Critic can order normally");
        Order order = critic.getOrder();
        assertTrue(r.findOrder(5) == order, "Critic and registry share one order");
        assertEqual(5, order.getId(), "Critic order ID matches its customer");
        assertEqual("Salad + Pasta", order.getItem().getName(), "Critic ID 5 chooses the most expensive item");
        assertEqual(2430, order.getBillCents(), "Critic pays full price");
        assertEqual(3, order.getRemainingUnits(), "Critic combo requires three preparation units");
        assertEqual(0, r.getRevenueCents(), "Unpaid Critic order contributes no revenue");

        Restaurant regularRestaurant = new Restaurant();
        Customer regular = seated(regularRestaurant, 5);
        assertTrue(waiter.takeOrder(regularRestaurant, 5).isSuccess(), "Regular ID 5 can order");
        assertEqual("Salad", regular.getOrder().getItem().getName(), "Regular ID 5 chooses Salad instead");
        assertEqual(900, regular.getOrder().getBillCents(), "Regular order remains full price");
    }

    private static void testPaymentDoesNotRecalculateCustomerPrice() {
        Restaurant r = new Restaurant();
        int[] currentQuote = {700};
        Customer customer = new Customer(1) {
            @Override
            public int calculateBillCents(MenuItem item) {
                return currentQuote[0];
            }
        };
        r.addArrival(customer);
        Waiter waiter = new Waiter();
        assertTrue(waiter.seat(r, 1, 1).isSuccess(), "Custom-priced customer is seated");
        assertTrue(waiter.takeOrder(r, 1).isSuccess(), "Waiter uses the customer's price method");
        Order order = customer.getOrder();
        assertEqual(700, order.getBillCents(), "Order captures the price at placement");
        currentQuote[0] = 200;
        assertTrue(new Chef().prepareOneUnit(order).isSuccess(), "Custom-priced order can be prepared");
        assertTrue(waiter.serve(r, 1, 2).isSuccess(), "Custom-priced order can be served");
        customer.finishEating(3);
        assertTrue(new Cashier().collectOne(r, 4), "Custom-priced order can be paid");
        assertEqual(700, order.getBillCents(), "Later pricing changes do not affect the order");
        assertEqual(700, r.getRevenueCents(), "Cashier collects the original price rather than requesting a new one");
    }

    private static void testInvalidPricingDoesNotLinkOrRegisterOrder() {
        for (boolean throwsDuringPricing : new boolean[] {false, true}) {
            Restaurant r = new Restaurant();
            Customer customer = new Customer(1) {
                @Override
                public int calculateBillCents(MenuItem item) {
                    if (throwsDuringPricing) {
                        throw new IllegalArgumentException("Invalid price quote");
                    }
                    return -1;
                }
            };
            r.addArrival(customer);
            Waiter waiter = new Waiter();
            assertTrue(waiter.seat(r, 1, 1).isSuccess(), "Invalid-pricing fixture is seated");
            assertThrows(IllegalArgumentException.class, () -> waiter.takeOrder(r, 1),
                    "Invalid price or pricing failure must be rejected before linking");
            assertTrue(customer.getOrder() == null, "Pricing failure does not link an order");
            assertTrue(r.findOrder(1) == null, "Pricing failure does not register an order");
            assertEqual(0, r.getOrders().size(), "Registry stays empty after pricing failure");
            assertEqual(CustomerStatus.SEATED, customer.getStatus(), "Pricing failure preserves customer status");
            assertEqual(100, customer.getSatisfaction(), "Pricing failure preserves satisfaction");
            assertTrue(r.getTables().get(0).getOccupant() == customer, "Pricing failure preserves occupancy");
            assertFalse(r.getWaitingQueue().contains(customer), "Pricing failure does not requeue the customer");
            assertEqual(0, r.getRevenueCents(), "Pricing failure creates no revenue");
            assertEqual(0, r.getPaidCount(), "Pricing failure creates no payment");
            assertEqual(0, r.getServedCount(), "Pricing failure creates no service");
        }
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

    private static void assertThrows(Class<? extends Throwable> expected, Runnable operation, String message) {
        try {
            operation.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) {
                return;
            }
            throw new AssertionError(message + ": unexpected exception", failure);
        }
        throw new AssertionError(message + ": expected " + expected.getSimpleName());
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
