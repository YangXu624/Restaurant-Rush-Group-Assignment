package restaurantrush.support;

/** Tests optional occupancy and ID validation, not generated record methods. */
public class TableViewTest {
    public static void main(String[] args) {
        testOptionalOccupant();
        testInvalidIds();
        System.out.println("All TableView tests passed successfully!");
    }

    private static void testOptionalOccupant() {
        TableView free = new TableView(1, null);
        if (free.occupantId() != null) {
            throw new AssertionError("A free table must use null, not a sentinel customer ID");
        }
        new TableView(2, 6);
        // The display value does not own the base game's two-table configuration.
        new TableView(3, 7);
    }

    private static void testInvalidIds() {
        for (int id : new int[] {0, -1}) {
            reject(() -> new TableView(id, null));
            reject(() -> new TableView(id, 1));
            reject(() -> new TableView(1, id));
        }
    }

    private static void reject(Runnable construction) {
        try {
            construction.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected nonpositive ID to be rejected");
    }
}
