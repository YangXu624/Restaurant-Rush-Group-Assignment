package restaurantrush.staff;

import restaurantrush.entity.MenuItem;
import restaurantrush.entity.Order;
import restaurantrush.entity.OrderStatus;
import restaurantrush.support.ActionResult;

public class ChefTest {

    public static void main(String[] args) {
        testNullOrderFails();
        testOneUnitDishBecomesReady();
        testTwoUnitDishTakesTwoTasks();
        testReadyOrderCannotBeCooked();
        testCancelledOrderCannotBeCooked();

        System.out.println("All Chef tests passed successfully!");
    }

    private static void testNullOrderFails() {
        ActionResult result = new Chef().prepareOneUnit(null);
        assertFalse(result.isSuccess(), "Cooking a null order should fail");
    }

    private static void testOneUnitDishBecomesReady() {
        Order order = new Order(1, new MenuItem("Salad", 900, 1));

        ActionResult result = new Chef().prepareOneUnit(order);

        assertTrue(result.isSuccess(), "Cooking a PLACED order should succeed");
        assertEqual(OrderStatus.READY, order.getStatus(), "1-unit dish should be READY");
        assertTrue(result.getMessage().contains("READY"), "Message should mention READY");
    }

    private static void testTwoUnitDishTakesTwoTasks() {
        Order order = new Order(3, new MenuItem("Pasta", 1800, 2));
        Chef chef = new Chef();

        ActionResult first = chef.prepareOneUnit(order);
        assertTrue(first.isSuccess(), "First unit should succeed");
        assertEqual(OrderStatus.PREPARING, order.getStatus(), "Should be PREPARING after one unit");
        assertEqual(1, order.getRemainingUnits(), "One unit should remain");
        assertFalse(first.getMessage().contains("READY"), "Message should not claim READY yet");

        ActionResult second = chef.prepareOneUnit(order);
        assertTrue(second.isSuccess(), "Second unit should succeed");
        assertEqual(OrderStatus.READY, order.getStatus(), "Should be READY after two units");
    }

    private static void testReadyOrderCannotBeCooked() {
        Order order = new Order(1, new MenuItem("Salad", 900, 1));
        Chef chef = new Chef();
        chef.prepareOneUnit(order);

        ActionResult result = chef.prepareOneUnit(order);

        assertFalse(result.isSuccess(), "Cooking a READY order should fail");
        assertEqual(OrderStatus.READY, order.getStatus(), "Order should stay READY");
        assertEqual(0, order.getRemainingUnits(), "Remaining units should stay 0");
    }

    private static void testCancelledOrderCannotBeCooked() {
        Order order = new Order(3, new MenuItem("Pasta", 1800, 2));
        order.cancel();

        ActionResult result = new Chef().prepareOneUnit(order);

        assertFalse(result.isSuccess(), "Cooking a CANCELLED order should fail");
        assertEqual(2, order.getRemainingUnits(), "Remaining units should be unchanged");
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
