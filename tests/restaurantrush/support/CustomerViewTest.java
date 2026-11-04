package restaurantrush.support;

import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.OrderStatus;

/** Focused display-value contract tests; no domain lifecycle rules are implemented here. */
public class CustomerViewTest {
    public static void main(String[] args) {
        testAbsentOrderAndTable();
        testOrderProgressAndHistory();
        testInvalidValues();
        testPartialOrderFields();
        System.out.println("All CustomerView tests passed successfully!");
    }

    private static void testAbsentOrderAndTable() {
        CustomerView waiting = new CustomerView(1, 100, CustomerStatus.WAITING,
                null, null, null, null, "Regular", null);
        if (waiting.orderStatus() != null || waiting.itemName() != null
                || waiting.billCents() != null || waiting.remainingUnits() != null || waiting.tableId() != null) {
            throw new AssertionError("Absent order and table must remain null, not sentinel values");
        }
        // Seating does not imply that an order already exists.
        new CustomerView(1, 0, CustomerStatus.SEATED, null, null, null, 1, "Regular", null);
    }

    private static void testOrderProgressAndHistory() {
        new CustomerView(3, 84, CustomerStatus.SEATED, OrderStatus.PREPARING, "Pasta", 1, 2, "Regular", 1800);
        new CustomerView(3, 76, CustomerStatus.SEATED, OrderStatus.READY, "Pasta", 0, 2, "Regular", 1800);
        // Paid and departed customers can retain order details without occupying a table.
        new CustomerView(3, 76, CustomerStatus.PAID, OrderStatus.PAID, "Pasta", 0, null, "Regular", 1800);
        new CustomerView(3, 0, CustomerStatus.LEFT, OrderStatus.CANCELLED, "Pasta", 1, null, "Regular", 1800);
        // Zero remaining units does not imply READY: cancelled history can also have zero.
        new CustomerView(3, 0, CustomerStatus.LEFT, OrderStatus.CANCELLED, "Pasta", 0, null, "Regular", 1800);
    }

    private static void testInvalidValues() {
        reject(() -> new CustomerView(1, 100, CustomerStatus.WAITING, null, null, null, null, null, null));
        reject(() -> new CustomerView(1, 100, CustomerStatus.WAITING, null, null, null, null, " ", null));
        reject(() -> new CustomerView(1, 100, CustomerStatus.SEATED, OrderStatus.PLACED, "Salad", 1, 1, "Regular", -1));
        new CustomerView(1, 100, CustomerStatus.SEATED, OrderStatus.PLACED, "Free dish", 1, 1, "Regular", 0);
        for (int id : new int[] {0, -1}) {
            reject(() -> new CustomerView(id, 100, CustomerStatus.WAITING, null, null, null, null, "Regular", null));
        }
        for (int satisfaction : new int[] {-1, 101}) {
            reject(() -> new CustomerView(1, satisfaction, CustomerStatus.WAITING,
                    null, null, null, null, "Regular", null));
        }
        reject(() -> new CustomerView(1, 100, null, null, null, null, null, "Regular", null));
        reject(() -> new CustomerView(1, 100, CustomerStatus.SEATED,
                OrderStatus.PLACED, "Salad", -1, 1, "Regular", 900));
        for (int tableId : new int[] {0, -1}) {
            reject(() -> new CustomerView(1, 100, CustomerStatus.SEATED,
                    null, null, null, tableId, "Regular", null));
        }
    }

    private static void testPartialOrderFields() {
        // All fourteen partially populated shapes are invalid; all-null and all-present are tested above.
        for (int mask = 1; mask < 15; mask++) {
            OrderStatus orderStatus = (mask & 1) != 0 ? OrderStatus.PLACED : null;
            String itemName = (mask & 2) != 0 ? "Salad" : null;
            Integer remainingUnits = (mask & 4) != 0 ? 1 : null;
            Integer bill = (mask & 8) != 0 ? 900 : null;
            reject(() -> new CustomerView(1, 100, CustomerStatus.SEATED,
                    orderStatus, itemName, remainingUnits, 1, "Regular", bill));
        }
    }

    private static void reject(Runnable construction) {
        try {
            construction.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected invalid display data to be rejected");
    }
}
