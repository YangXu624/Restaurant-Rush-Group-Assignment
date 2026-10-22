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
                null, null, null, null);
        if (waiting.orderStatus() != null || waiting.itemName() != null
                || waiting.remainingUnits() != null || waiting.tableId() != null) {
            throw new AssertionError("Absent order and table must remain null, not sentinel values");
        }
        // Seating does not imply that an order already exists.
        new CustomerView(1, 0, CustomerStatus.SEATED, null, null, null, 1);
    }

    private static void testOrderProgressAndHistory() {
        new CustomerView(3, 84, CustomerStatus.SEATED, OrderStatus.PREPARING, "Pasta", 1, 2);
        new CustomerView(3, 76, CustomerStatus.SEATED, OrderStatus.READY, "Pasta", 0, 2);
        // Paid and departed customers can retain order details without occupying a table.
        new CustomerView(3, 76, CustomerStatus.PAID, OrderStatus.PAID, "Pasta", 0, null);
        new CustomerView(3, 0, CustomerStatus.LEFT, OrderStatus.CANCELLED, "Pasta", 1, null);
        // Zero remaining units does not imply READY: cancelled history can also have zero.
        new CustomerView(3, 0, CustomerStatus.LEFT, OrderStatus.CANCELLED, "Pasta", 0, null);
    }

    private static void testInvalidValues() {
        for (int id : new int[] {0, -1}) {
            reject(() -> new CustomerView(id, 100, CustomerStatus.WAITING, null, null, null, null));
        }
        for (int satisfaction : new int[] {-1, 101}) {
            reject(() -> new CustomerView(1, satisfaction, CustomerStatus.WAITING,
                    null, null, null, null));
        }
        reject(() -> new CustomerView(1, 100, null, null, null, null, null));
        reject(() -> new CustomerView(1, 100, CustomerStatus.SEATED,
                OrderStatus.PLACED, "Salad", -1, 1));
        for (int tableId : new int[] {0, -1}) {
            reject(() -> new CustomerView(1, 100, CustomerStatus.SEATED,
                    null, null, null, tableId));
        }
    }

    private static void testPartialOrderFields() {
        // All six partially populated shapes are invalid; all-null and all-present are tested above.
        for (int mask = 1; mask < 7; mask++) {
            OrderStatus orderStatus = (mask & 1) != 0 ? OrderStatus.PLACED : null;
            String itemName = (mask & 2) != 0 ? "Salad" : null;
            Integer remainingUnits = (mask & 4) != 0 ? 1 : null;
            reject(() -> new CustomerView(1, 100, CustomerStatus.SEATED,
                    orderStatus, itemName, remainingUnits, 1));
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
