package restaurantrush.entity;

import java.util.List;
import java.util.Objects;

/** Contract tests using real shared Customer and Order objects, without a test framework. */
public class CustomerTest {
    public static void main(String[] args) {
        testInitialState();
        testFoodSelection();
        testOrderAcceptanceAndSeatingFailures();
        testCompleteLifecycleAndTiming();
        testServingPreconditions();
        testWaitingPenaltyBoundaries();
        testAbandonmentWithoutOrder();
        testAbandonmentCancelsSharedOrder();
        testFailedCancellationIsAtomic();
        System.out.println("All Customer tests passed successfully!");
    }

    private static List<MenuItem> baseMenu() {
        return List.of(new MenuItem("Salad", 900, 1),
                new MenuItem("Burger", 1500, 1), new MenuItem("Pasta", 1800, 2));
    }

    private static void testInitialState() {
        Customer customer = new Customer(1);
        assertEqual(1, customer.getId(), "Customer ID");
        assertEqual(100, customer.getSatisfaction(), "Initial satisfaction");
        assertEqual(CustomerStatus.WAITING, customer.getStatus(), "Initial status");
        assertEqual(null, customer.getOrder(), "No initial order");
        assertEqual(-1, customer.getServedTurn(), "No service timestamp");
        assertEqual(-1, customer.getReadyToPayTurn(), "No payment-ready timestamp");
        // These follow the existing positive-ID policy, consistent with Order.
        assertThrows(IllegalArgumentException.class, () -> new Customer(0), "Zero ID");
        assertThrows(IllegalArgumentException.class, () -> new Customer(-1), "Negative ID");
        assertNoChange(customer, () -> customer.finishEating(1), "Waiting customer cannot eat");
    }

    private static void testFoodSelection() {
        List<MenuItem> menu = baseMenu();
        assertSelections(menu, new String[] {"Salad", "Burger", "Pasta", "Salad", "Burger", "Pasta"});
        List<MenuItem> fourItems = List.of(menu.get(0), menu.get(1), menu.get(2),
                new MenuItem("Soup", 700, 1));
        // Includes the fourth position and wraparound, without relying on IDs above six.
        assertSelections(fourItems, new String[] {"Salad", "Burger", "Pasta", "Soup", "Salad", "Burger"});

        Customer customer = new Customer(6);
        List<MenuItem> singleItem = List.of(menu.get(2));
        assertNoChange(customer, () -> assertSame(menu.get(2), customer.chooseItem(singleItem),
                "Single-item menu"), "Food selection must not mutate customer");
        assertRejectedUnchanged(customer, IllegalArgumentException.class,
                () -> customer.chooseItem(null), "Null menu");
        assertRejectedUnchanged(customer, IllegalArgumentException.class,
                () -> customer.chooseItem(List.of()), "Empty menu");
    }

    private static void assertSelections(List<MenuItem> menu, String[] expectedNames) {
        for (int i = 0; i < expectedNames.length; i++) {
            Customer customer = new Customer(i + 1);
            String expectedName = expectedNames[i];
            assertNoChange(customer, () -> {
                MenuItem choice = customer.chooseItem(menu);
                assertEqual(expectedName, choice.getName(), "Deterministic food choice");
                assertSame(choice, customer.chooseItem(menu), "Repeated choice returns same menu object");
            }, "Choosing food does not place an order or alter customer state");
        }
    }

    private static void testOrderAcceptanceAndSeatingFailures() {
        Customer customer = new Customer(1);
        Order order = new Order(1, baseMenu().get(0));
        assertRejectedUnchanged(customer, IllegalStateException.class,
                () -> customer.acceptOrder(order), "Cannot order before seating", order);
        customer.seat();
        assertEqual(CustomerStatus.SEATED, customer.getStatus(), "Seating transition");
        assertRejectedUnchanged(customer, IllegalStateException.class, customer::seat, "Duplicate seating");
        assertRejectedUnchanged(customer, IllegalArgumentException.class,
                () -> customer.acceptOrder(null), "Null order");
        Order otherCustomerOrder = new Order(2, baseMenu().get(1));
        assertRejectedUnchanged(customer, IllegalArgumentException.class,
                () -> customer.acceptOrder(otherCustomerOrder), "Mismatched order ID", otherCustomerOrder);

        customer.acceptOrder(order);
        assertSame(order, customer.getOrder(), "Keep the shared order reference");
        assertStatuses(customer, CustomerStatus.SEATED, OrderStatus.PLACED);
        assertRejectedUnchanged(customer, IllegalStateException.class,
                () -> customer.acceptOrder(order), "Cannot accept same order twice", order);
        Order replacement = new Order(1, baseMenu().get(2));
        assertRejectedUnchanged(customer, IllegalStateException.class,
                () -> customer.acceptOrder(replacement), "Cannot replace an existing order", replacement);
    }

    private static void testCompleteLifecycleAndTiming() {
        Customer customer = new Customer(3); // Deterministically chooses Pasta.
        customer.seat(); // Turn 1.
        customer.applyWaitingPenalty();
        assertEqual(92, customer.getSatisfaction(), "Seated waiting penalty");
        Order order = new Order(customer.getId(), customer.chooseItem(baseMenu()));
        customer.acceptOrder(order); // Turn 2.
        assertStatuses(customer, CustomerStatus.SEATED, OrderStatus.PLACED);
        order.prepareOneUnit();
        assertStatuses(customer, CustomerStatus.SEATED, OrderStatus.PREPARING);
        assertEqual(1, order.getRemainingUnits(), "One pasta unit remains");
        customer.applyWaitingPenalty();
        assertEqual(84, customer.getSatisfaction(), "Penalty while preparing");
        order.prepareOneUnit(); // Turn 3.
        assertStatuses(customer, CustomerStatus.SEATED, OrderStatus.READY);
        assertEqual(0, order.getRemainingUnits(), "Preparation complete");
        customer.applyWaitingPenalty();
        assertEqual(76, customer.getSatisfaction(), "READY food still incurs waiting penalty");
        assertNoChange(customer, () -> customer.finishEating(3), "Unserved customer cannot eat");

        // Restaurant coordinates order first, then customer: enums do not synchronize themselves.
        order.markServed(); // Turn 4.
        assertStatuses(customer, CustomerStatus.SEATED, OrderStatus.SERVED);
        customer.markServed(4);
        assertStatuses(customer, CustomerStatus.SERVED, OrderStatus.SERVED);
        assertEqual(4, customer.getServedTurn(), "Service timestamp");
        assertEqual(-1, customer.getReadyToPayTurn(), "Still eating");
        assertNoChange(customer, customer::applyWaitingPenalty, "No penalty while eating");
        assertNoChange(customer, () -> customer.finishEating(3), "Earlier turn cannot finish eating");
        assertNoChange(customer, () -> customer.finishEating(4), "Same turn cannot finish eating");
        assertRejectedUnchanged(customer, IllegalStateException.class,
                () -> customer.markServed(5), "Duplicate service must preserve timestamp");
        assertRejectedUnchanged(customer, IllegalStateException.class, customer::markPaid, "Cannot pay while eating");
        assertRejectedUnchanged(customer, IllegalStateException.class, customer::abandon, "Cannot abandon after service");

        customer.finishEating(5);
        assertStatuses(customer, CustomerStatus.READY_TO_PAY, OrderStatus.SERVED);
        assertEqual(5, customer.getReadyToPayTurn(), "Eating completion timestamp");
        assertNoChange(customer, () -> customer.finishEating(6), "Do not overwrite readiness timestamp");
        assertNoChange(customer, customer::applyWaitingPenalty, "No penalty while waiting for payment");
        assertRejectedUnchanged(customer, IllegalStateException.class, customer::markPaid,
                "Customer cannot be paid before shared order");
        assertRejectedUnchanged(customer, IllegalStateException.class, customer::abandon, "Cannot abandon when ready to pay");

        // Turn 6: payment eligibility belongs to Restaurant, not Customer.markPaid().
        order.markPaid();
        assertStatuses(customer, CustomerStatus.READY_TO_PAY, OrderStatus.PAID);
        customer.markPaid();
        assertStatuses(customer, CustomerStatus.PAID, OrderStatus.PAID);
        assertEqual(76, customer.getSatisfaction(), "Satisfaction retained for payment statistics");
        assertEqual(4, customer.getServedTurn(), "Service history retained");
        assertEqual(5, customer.getReadyToPayTurn(), "Eating history retained");
        assertEqual(1800, order.getBillCents(), "Bill remains locked");
        assertNoChange(customer, customer::applyWaitingPenalty, "Paid customer has no penalty");
        assertNoChange(customer, () -> customer.finishEating(7), "Paid customer cannot restart eating");
        assertRejectedUnchanged(customer, IllegalStateException.class, customer::markPaid, "Duplicate payment");
        assertRejectedUnchanged(customer, IllegalStateException.class, customer::seat, "Paid customer cannot be reseated");
        assertRejectedUnchanged(customer, IllegalStateException.class, customer::abandon, "Paid customer cannot abandon");
    }

    private static void testServingPreconditions() {
        Customer customer = new Customer(1);
        assertRejectedUnchanged(customer, IllegalStateException.class,
                () -> customer.markServed(1), "Waiting customer cannot be served");
        customer.seat();
        assertRejectedUnchanged(customer, IllegalStateException.class,
                () -> customer.markServed(1), "Cannot serve without an order");
        assertRejectedUnchanged(customer, IllegalStateException.class, customer::markPaid, "Seated customer cannot pay");
        Order order = new Order(1, baseMenu().get(2));
        customer.acceptOrder(order);
        assertRejectedUnchanged(customer, IllegalStateException.class,
                () -> customer.markServed(1), "PLACED order is not served");
        order.prepareOneUnit();
        assertRejectedUnchanged(customer, IllegalStateException.class,
                () -> customer.markServed(2), "PREPARING order is not served");
        order.prepareOneUnit();
        assertRejectedUnchanged(customer, IllegalStateException.class,
                () -> customer.markServed(3), "READY order must be marked SERVED first");
        order.markServed();
        assertRejectedUnchanged(customer, IllegalArgumentException.class,
                () -> customer.markServed(0), "Zero service turn");
        assertRejectedUnchanged(customer, IllegalArgumentException.class,
                () -> customer.markServed(-1), "Negative service turn");
        customer.markServed(4); // Failed calls did not consume service or alter its timestamp.
        assertStatuses(customer, CustomerStatus.SERVED, OrderStatus.SERVED);
        assertEqual(4, customer.getServedTurn(), "Successful service after rejected calls");
    }

    private static void testWaitingPenaltyBoundaries() {
        for (boolean seated : new boolean[] {false, true}) {
            Customer customer = new Customer(1);
            if (seated) {
                customer.seat();
            }
            customer.applyWaitingPenalty();
            assertEqual(92, customer.getSatisfaction(), "One waiting penalty");
            for (int i = 1; i < 12; i++) {
                customer.applyWaitingPenalty();
            }
            assertEqual(4, customer.getSatisfaction(), "Twelve waiting penalties");
            customer.applyWaitingPenalty();
            assertEqual(0, customer.getSatisfaction(), "Thirteenth penalty clamps at zero");
            assertEqual(seated ? CustomerStatus.SEATED : CustomerStatus.WAITING, customer.getStatus(),
                    "Penalty alone must not perform abandonment");
            assertNoChange(customer, customer::applyWaitingPenalty, "Penalty at zero cannot go negative");
        }
    }

    private static void testAbandonmentWithoutOrder() {
        for (boolean seated : new boolean[] {false, true}) {
            Customer customer = new Customer(1);
            if (seated) {
                customer.seat();
            }
            reduceSatisfactionToZero(customer);
            customer.abandon();
            assertEqual(CustomerStatus.LEFT, customer.getStatus(), "Abandon without order");
            assertEqual(null, customer.getOrder(), "Abandonment must not create an order");
            assertNoChange(customer, customer::applyWaitingPenalty, "Departed customer has no penalty");
            assertNoChange(customer, () -> customer.finishEating(18), "Departed customer cannot eat");
            assertRejectedUnchanged(customer, IllegalStateException.class, customer::abandon, "Duplicate abandonment");
            assertRejectedUnchanged(customer, IllegalStateException.class, customer::seat, "Cannot reseat departed customer");
        }
    }

    private static void testAbandonmentCancelsSharedOrder() {
        // Exercise cancellation with 2, 1 and 0 preparation units remaining.
        for (int completedUnits = 0; completedUnits <= 2; completedUnits++) {
            Customer customer = new Customer(3);
            customer.seat();
            Order order = new Order(3, baseMenu().get(2));
            customer.acceptOrder(order);
            for (int i = 0; i < completedUnits; i++) {
                order.prepareOneUnit();
            }
            reduceSatisfactionToZero(customer);
            customer.abandon();
            assertStatuses(customer, CustomerStatus.LEFT, OrderStatus.CANCELLED);
            assertSame(order, customer.getOrder(), "Retain shared order for history");
            assertEqual(2 - completedUnits, order.getRemainingUnits(), "Cancellation preserves progress");
            assertEqual(false, order.canPrepare(), "Cancelled order is not preparable");
            assertEqual(false, order.isReady(), "Cancelled order is not READY even with zero units");
            assertRejectedUnchanged(customer, IllegalStateException.class, order::prepareOneUnit, "Cook cancelled order");
            assertRejectedUnchanged(customer, IllegalStateException.class, order::markServed, "Serve cancelled order");
            assertRejectedUnchanged(customer, IllegalStateException.class, order::markPaid, "Pay cancelled order");
            assertRejectedUnchanged(customer, IllegalStateException.class,
                    () -> customer.markServed(14), "Serve departed customer");
            assertRejectedUnchanged(customer, IllegalStateException.class, customer::markPaid, "Pay departed customer");
            assertRejectedUnchanged(customer, IllegalStateException.class, customer::abandon, "Abandon twice");
        }
    }

    private static void testFailedCancellationIsAtomic() {
        Customer customer = new Customer(1);
        customer.seat();
        Order sharedOrder = new Order(1, baseMenu().get(0));
        customer.acceptOrder(sharedOrder);
        reduceSatisfactionToZero(customer);
        // Simulate a collaborator cancelling the shared order before abandonment.
        sharedOrder.cancel();
        assertRejectedUnchanged(customer, IllegalStateException.class, customer::abandon,
                "Failed cancellation must not partially change customer to LEFT");
    }

    private static void reduceSatisfactionToZero(Customer customer) {
        for (int i = 0; i < 13; i++) {
            customer.applyWaitingPenalty();
        }
        assertEqual(0, customer.getSatisfaction(), "Abandonment fixture reaches zero");
    }

    private static void assertStatuses(Customer customer, CustomerStatus expectedCustomer, OrderStatus expectedOrder) {
        assertEqual(expectedCustomer, customer.getStatus(), "Customer lifecycle status");
        assertEqual(expectedOrder, customer.getOrder().getStatus(), "Shared order lifecycle status");
    }

    // Test-only value snapshots capture every exposed field, including the linked order.
    private record OrderState(int id, MenuItem item, OrderStatus status, int remainingUnits, int billCents) {
        static OrderState capture(Order order) {
            return order == null ? null : new OrderState(order.getId(), order.getItem(), order.getStatus(),
                    order.getRemainingUnits(), order.getBillCents());
        }
    }

    private record CustomerState(int id, int satisfaction, CustomerStatus status, int servedTurn,
            int readyToPayTurn, Order orderReference, OrderState orderState) {
        static CustomerState capture(Customer customer) {
            return new CustomerState(customer.getId(), customer.getSatisfaction(), customer.getStatus(),
                    customer.getServedTurn(), customer.getReadyToPayTurn(), customer.getOrder(),
                    OrderState.capture(customer.getOrder()));
        }
    }

    private static void assertNoChange(Customer customer, Runnable operation, String message) {
        CustomerState before = CustomerState.capture(customer);
        operation.run();
        assertEqual(before, CustomerState.capture(customer), message);
    }

    private static void assertRejectedUnchanged(Customer customer, Class<? extends Throwable> expected,
            Runnable operation, String message, Order... otherOrders) {
        CustomerState before = CustomerState.capture(customer);
        OrderState[] otherBefore = new OrderState[otherOrders.length];
        for (int i = 0; i < otherOrders.length; i++) {
            otherBefore[i] = OrderState.capture(otherOrders[i]);
        }
        assertThrows(expected, operation, message);
        assertEqual(before, CustomerState.capture(customer), message + ": customer and linked order unchanged");
        for (int i = 0; i < otherOrders.length; i++) {
            assertEqual(otherBefore[i], OrderState.capture(otherOrders[i]), message + ": supplied order unchanged");
        }
    }

    private static void assertSame(Object expected, Object actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected the same object reference");
        }
    }

    private static void assertEqual(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
        }
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
}
