package restaurantrush.entity;

public class MenuItemTest {

    public static void main(String[] args) {
        testValidCreation();
        testZeroPriceAllowed();
        testNullNameThrows();
        testEmptyNameThrows();
        testBlankNameThrows();
        testNegativePriceThrows();
        testZeroPreparationUnitsThrows();
        testNegativePreparationUnitsThrows();
        System.out.println("All MenuItem tests passed successfully!");
    }

    private static void testValidCreation() {
        MenuItem salad = new MenuItem("Salad", 900, 1);
        assertEqual("Salad", salad.getName(), "Name mismatch");
        assertEqual(900, salad.getPriceCents(), "Price mismatch");
        assertEqual(1, salad.getPreparationUnits(), "Preparation units mismatch");

        MenuItem pasta = new MenuItem("Pasta", 1800, 2);
        assertEqual("Pasta", pasta.getName(), "Name mismatch");
        assertEqual(1800, pasta.getPriceCents(), "Price mismatch");
        assertEqual(2, pasta.getPreparationUnits(), "Preparation units mismatch");
    }

    private static void testZeroPriceAllowed() {
        MenuItem freeItem = new MenuItem("Complimentary Water", 0, 1);
        assertEqual(0, freeItem.getPriceCents(), "Zero price should be permitted");
    }

    private static void testNullNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem(null, 500, 1),
                "Expected IllegalArgumentException for null name");
    }

    private static void testEmptyNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("", 500, 1),
                "Expected IllegalArgumentException for empty name");
    }

    private static void testBlankNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("   ", 500, 1),
                "Expected IllegalArgumentException for blank name");
    }

    private static void testNegativePriceThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("Burger", -1, 1),
                "Expected IllegalArgumentException for negative price");
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("Burger", -100, 1),
                "Expected IllegalArgumentException for negative price");
    }

    private static void testZeroPreparationUnitsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("Salad", 500, 0),
                "Expected IllegalArgumentException for 0 preparation units");
    }

    private static void testNegativePreparationUnitsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("Salad", 500, -2),
                "Expected IllegalArgumentException for negative preparation units");
    }

    private static void assertEqual(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertThrows(Class<? extends Throwable> expectedType, Runnable executable, String message) {
        try {
            executable.run();
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return; // Passed
            }
            throw new AssertionError(message + " - Threw unexpected exception type: " + t.getClass().getName(), t);
        }
        throw new AssertionError(message + " - Did not throw any exception");
    }
}
