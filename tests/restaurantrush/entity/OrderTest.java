package restaurantrush.entity;

public class OrderTest {

    public static void main(String[] args) {
        testConstructorValid();
        testConstructorInvalidIdThrows();
        testConstructorNullItemThrows();

        testOneUnitPreparationLifecycle();
        testMultiUnitPreparationLifecycle();

        testServingLifecycle();
        testPaymentLifecycle();

        testCancellationFromPlaced();
        testCancellationFromPreparing();
        testCancellationFromReady();
        testCancellationInvalidStatesThrows();

        testPrepareAfterReadyOrServedThrows();

        System.out.println("All Order tests passed successfully!");
    }

    private static void testConstructorValid() {
        MenuItem salad = new MenuItem("Salad", 900, 1);
        Order order = new Order(1, salad);

        assertEqual(1, order.getId(), "Order ID mismatch");
        assertEqual(salad, order.getItem(), "Item mismatch");
        assertEqual(OrderStatus.PLACED, order.getStatus(), "Initial status should be PLACED");
        assertEqual(1, order.getRemainingUnits(), "Initial remaining units mismatch");
        assertEqual(900, order.getBillCents(), "Bill cents mismatch");
        assertTrue(order.canPrepare(), "Order should be preparable initially");
        assertFalse(order.isReady(), "Order should not be ready initially");
    }

    private static void testConstructorInvalidIdThrows() {
        MenuItem item = new MenuItem("Burger", 1500, 1);
        assertThrows(IllegalArgumentException.class, () -> new Order(0, item),
                "Expected IllegalArgumentException for ID 0");
        assertThrows(IllegalArgumentException.class, () -> new Order(-1, item),
                "Expected IllegalArgumentException for negative ID");
    }

    private static void testConstructorNullItemThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Order(1, null),
                "Expected IllegalArgumentException for null item");
    }

    private static void testOneUnitPreparationLifecycle() {
        MenuItem salad = new MenuItem("Salad", 900, 1);
        Order order = new Order(1, salad);

        order.prepareOneUnit();
        assertEqual(OrderStatus.READY, order.getStatus(), "1-unit item should become READY directly");
        assertEqual(0, order.getRemainingUnits(), "Remaining units should be 0");
        assertTrue(order.isReady(), "isReady should be true");
        assertFalse(order.canPrepare(), "canPrepare should be false when READY");

        assertThrows(IllegalStateException.class, order::prepareOneUnit,
                "Expected IllegalStateException preparing already READY order");
    }

    private static void testMultiUnitPreparationLifecycle() {
        MenuItem pasta = new MenuItem("Pasta", 1800, 2);
        Order order = new Order(2, pasta);

        // Step 1: PLACED -> PREPARING
        order.prepareOneUnit();
        assertEqual(OrderStatus.PREPARING, order.getStatus(), "Order should be PREPARING after 1 unit");
        assertEqual(1, order.getRemainingUnits(), "1 unit should remain");
        assertTrue(order.canPrepare(), "canPrepare should still be true");
        assertFalse(order.isReady(), "isReady should still be false");

        // Step 2: PREPARING -> READY
        order.prepareOneUnit();
        assertEqual(OrderStatus.READY, order.getStatus(), "Order should become READY after 2nd unit");
        assertEqual(0, order.getRemainingUnits(), "0 units should remain");
        assertTrue(order.isReady(), "isReady should be true");
        assertFalse(order.canPrepare(), "canPrepare should be false");

        assertThrows(IllegalStateException.class, order::prepareOneUnit,
                "Expected IllegalStateException preparing beyond remaining units");
    }

    private static void testServingLifecycle() {
        MenuItem item = new MenuItem("Burger", 1500, 1);
        Order order = new Order(3, item);

        // Cannot serve while PLACED
        assertThrows(IllegalStateException.class, order::markServed,
                "Cannot serve PLACED order");
        assertEqual(OrderStatus.PLACED, order.getStatus(), "Status must remain PLACED");
        assertEqual(1, order.getRemainingUnits(), "Remaining units must remain unchanged");

        order.prepareOneUnit(); // now READY
        assertTrue(order.isReady(), "Should be READY");

        order.markServed();
        assertEqual(OrderStatus.SERVED, order.getStatus(), "Should be SERVED");
        assertFalse(order.isReady(), "isReady must be false once SERVED");

        // Cannot serve again
        assertThrows(IllegalStateException.class, order::markServed,
                "Cannot serve already SERVED order");
        assertEqual(OrderStatus.SERVED, order.getStatus(), "Status must remain SERVED");
    }

    private static void testPaymentLifecycle() {
        MenuItem item = new MenuItem("Salad", 900, 1);
        Order order = new Order(4, item);

        assertThrows(IllegalStateException.class, order::markPaid,
                "Cannot pay for PLACED order");
        assertEqual(OrderStatus.PLACED, order.getStatus(), "Status must remain PLACED");

        order.prepareOneUnit();
        assertThrows(IllegalStateException.class, order::markPaid,
                "Cannot pay for READY order");
        assertEqual(OrderStatus.READY, order.getStatus(), "Status must remain READY");

        order.markServed();
        order.markPaid();
        assertEqual(OrderStatus.PAID, order.getStatus(), "Order should be PAID");

        assertThrows(IllegalStateException.class, order::markPaid,
                "Cannot pay for already PAID order");
        assertEqual(OrderStatus.PAID, order.getStatus(), "Status must remain PAID");
    }

    private static void testCancellationFromPlaced() {
        Order order = new Order(5, new MenuItem("Salad", 900, 1));
        order.cancel();
        assertEqual(OrderStatus.CANCELLED, order.getStatus(), "Should be CANCELLED");
        assertEqual(1, order.getRemainingUnits(), "Remaining units must remain unchanged");
        assertFalse(order.canPrepare(), "Cancelled order cannot be prepared");
        assertFalse(order.isReady(), "Cancelled order is not ready");

        assertThrows(IllegalStateException.class, order::prepareOneUnit, "Cancelled order cannot be prepared");
        assertEqual(OrderStatus.CANCELLED, order.getStatus(), "Status must remain CANCELLED");
        assertEqual(1, order.getRemainingUnits(), "Remaining units must remain unchanged");

        assertThrows(IllegalStateException.class, order::markServed, "Cancelled order cannot be served");
        assertEqual(OrderStatus.CANCELLED, order.getStatus(), "Status must remain CANCELLED");

        assertThrows(IllegalStateException.class, order::markPaid, "Cancelled order cannot be paid");
        assertEqual(OrderStatus.CANCELLED, order.getStatus(), "Status must remain CANCELLED");
    }

    private static void testCancellationFromPreparing() {
        Order order = new Order(6, new MenuItem("Pasta", 1800, 2));
        order.prepareOneUnit(); // PREPARING
        assertEqual(OrderStatus.PREPARING, order.getStatus(), "Should be PREPARING");
        assertEqual(1, order.getRemainingUnits(), "1 unit remaining");

        order.cancel();
        assertEqual(OrderStatus.CANCELLED, order.getStatus(), "Should be CANCELLED");
        assertEqual(1, order.getRemainingUnits(), "Remaining units must remain unchanged");
        assertFalse(order.canPrepare(), "Cancelled order cannot be prepared");
        assertFalse(order.isReady(), "Cancelled order is not ready");

        assertThrows(IllegalStateException.class, order::prepareOneUnit, "Cancelled order cannot be prepared");
        assertEqual(OrderStatus.CANCELLED, order.getStatus(), "Status must remain CANCELLED");
        assertEqual(1, order.getRemainingUnits(), "Remaining units must remain unchanged");

        assertThrows(IllegalStateException.class, order::markServed, "Cancelled order cannot be served");
        assertThrows(IllegalStateException.class, order::markPaid, "Cancelled order cannot be paid");
        assertEqual(OrderStatus.CANCELLED, order.getStatus(), "Status must remain CANCELLED");
    }

    private static void testCancellationFromReady() {
        Order order = new Order(7, new MenuItem("Salad", 900, 1));
        order.prepareOneUnit(); // READY
        assertTrue(order.isReady(), "Order should be READY");
        assertEqual(0, order.getRemainingUnits(), "Remaining units should be 0");

        order.cancel();
        assertEqual(OrderStatus.CANCELLED, order.getStatus(), "Should be CANCELLED");
        assertEqual(0, order.getRemainingUnits(), "Remaining units stay 0");
        assertFalse(order.isReady(), "Cancelled order must NOT be ready despite 0 remaining units");
        assertFalse(order.canPrepare(), "Cancelled order cannot be prepared");

        assertThrows(IllegalStateException.class, order::prepareOneUnit, "Cancelled order cannot be prepared");
        assertThrows(IllegalStateException.class, order::markServed, "Cancelled order cannot be served");
        assertThrows(IllegalStateException.class, order::markPaid, "Cancelled order cannot be paid");
        assertEqual(OrderStatus.CANCELLED, order.getStatus(), "Status must remain CANCELLED");
        assertEqual(0, order.getRemainingUnits(), "Remaining units must stay 0");
    }

    private static void testCancellationInvalidStatesThrows() {
        Order order = new Order(8, new MenuItem("Salad", 900, 1));
        order.prepareOneUnit();
        order.markServed();

        assertThrows(IllegalStateException.class, order::cancel,
                "Cannot cancel SERVED order");
        assertEqual(OrderStatus.SERVED, order.getStatus(), "Status must remain SERVED");

        order.markPaid();
        assertThrows(IllegalStateException.class, order::cancel,
                "Cannot cancel PAID order");
        assertEqual(OrderStatus.PAID, order.getStatus(), "Status must remain PAID");

        Order cancelledOrder = new Order(9, new MenuItem("Salad", 900, 1));
        cancelledOrder.cancel();
        assertThrows(IllegalStateException.class, cancelledOrder::cancel,
                "Cannot cancel already CANCELLED order");
        assertEqual(OrderStatus.CANCELLED, cancelledOrder.getStatus(), "Status must remain CANCELLED");
    }

    private static void testPrepareAfterReadyOrServedThrows() {
        Order order = new Order(10, new MenuItem("Salad", 900, 1));
        order.prepareOneUnit(); // READY
        assertThrows(IllegalStateException.class, order::prepareOneUnit, "Cannot prepare READY order");

        order.markServed();
        assertThrows(IllegalStateException.class, order::prepareOneUnit, "Cannot prepare SERVED order");

        order.markPaid();
        assertThrows(IllegalStateException.class, order::prepareOneUnit, "Cannot prepare PAID order");
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

    private static void assertThrows(Class<? extends Throwable> expectedType, Runnable executable, String message) {
        try {
            executable.run();
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return;
            }
            throw new AssertionError(message + " - Threw unexpected exception type: " + t.getClass().getName(), t);
        }
        throw new AssertionError(message + " - Did not throw any exception");
    }
}
