package restaurantrush.support;

public class ActionResultTest {

    public static void main(String[] args) {
        testOk();
        testFail();

        System.out.println("All ActionResult tests passed successfully!");
    }

    private static void testOk() {
        ActionResult result = ActionResult.ok("Seated customer 1 at table 1.");
        assertTrue(result.isSuccess(), "ok() should be a success");
        assertEqual("Seated customer 1 at table 1.", result.getMessage(), "ok() message mismatch");
    }

    private static void testFail() {
        ActionResult result = ActionResult.fail("Table 3 does not exist.");
        assertFalse(result.isSuccess(), "fail() should not be a success");
        assertEqual("Table 3 does not exist.", result.getMessage(), "fail() message mismatch");
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
