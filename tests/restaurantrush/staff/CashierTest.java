package restaurantrush.staff;

import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.Restaurant;

public class CashierTest {

    public static void main(String[] args) {
        testNobodyToPay();
        testPaysEligibleCustomer();
        testNotEligibleOnSameTurnAsReady();
        testOnlyOnePaymentPerTurn();
        testPaysOldestFirst();

        System.out.println("All Cashier tests passed successfully!");
    }

    private static void testNobodyToPay() {
        Restaurant r = new Restaurant();
        assertFalse(new Cashier().collectOne(r, 1), "Nobody to pay should return false");
        assertEqual(0, r.getPaidCount(), "Paid count should stay 0");
    }

    private static void testPaysEligibleCustomer() {
        Restaurant r = new Restaurant();
        Customer c = readyToPay(r, 1, 1); // served turn 1, ready turn 2

        assertTrue(new Cashier().collectOne(r, 3), "Ready customer should pay");
        assertEqual(CustomerStatus.PAID, c.getStatus(), "Customer should be PAID");
        assertEqual(900, r.getRevenueCents(), "Revenue should include the Salad bill");
        assertEqual(1, r.getPaidCount(), "Paid count should be 1");
        assertTrue(r.getTables().get(0).isAvailable(), "Table should be freed");
    }

    private static void testNotEligibleOnSameTurnAsReady() {
        Restaurant r = new Restaurant();
        Customer c = readyToPay(r, 1, 1); // ready turn 2

        assertFalse(new Cashier().collectOne(r, 2), "Cannot pay on the turn they became ready");
        assertEqual(CustomerStatus.READY_TO_PAY, c.getStatus(), "Customer should still be waiting to pay");
    }

    private static void testOnlyOnePaymentPerTurn() {
        Restaurant r = new Restaurant();
        Customer first = readyToPay(r, 1, 1);
        Customer second = readyToPay(r, 2, 2);
        Cashier cashier = new Cashier();

        assertTrue(cashier.collectOne(r, 3), "First payment this turn should succeed");
        assertFalse(cashier.collectOne(r, 3), "Second payment in the same turn should fail");
        assertEqual(1, r.getPaidCount(), "Only one customer should have paid");
        assertEqual(CustomerStatus.PAID, first.getStatus(), "First customer should be PAID");
        assertEqual(CustomerStatus.READY_TO_PAY, second.getStatus(), "Second should still wait");

        assertTrue(cashier.collectOne(r, 4), "Next turn should allow another payment");
        assertEqual(CustomerStatus.PAID, second.getStatus(), "Second customer should now be PAID");
    }

    private static void testPaysOldestFirst() {
        Restaurant r = new Restaurant();
        Customer lowerId = served(r, 1, 1);
        Customer higherId = served(r, 2, 2);
        // Customer 2 finishes eating first, so is older despite the higher ID.
        higherId.finishEating(2);
        lowerId.finishEating(3);

        assertTrue(new Cashier().collectOne(r, 4), "Payment should succeed");
        assertEqual(CustomerStatus.PAID, higherId.getStatus(), "Earliest-ready customer pays first");
        assertEqual(CustomerStatus.READY_TO_PAY, lowerId.getStatus(), "Later customer should still wait");
    }

    /** Arrives, seats, orders a 1-unit dish, serves it on turn 1 and finishes eating on turn 2. */
    private static Customer readyToPay(Restaurant r, int id, int tableId) {
        Customer c = served(r, id, tableId);
        c.finishEating(2);
        return c;
    }

    private static Customer served(Restaurant r, int id, int tableId) {
        Customer c = new Customer(id);
        r.addArrival(c);
        Waiter waiter = new Waiter();
        waiter.seat(r, id, tableId);
        waiter.takeOrder(r, id);
        new Chef().prepareOneUnit(c.getOrder());
        waiter.serve(r, id, 1);
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
