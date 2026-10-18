package restaurantrush.entity;

/**
 * One restaurant table and the customer currently sitting at it.
 *
 * <p>Table only knows about its own occupancy. Queue removal and the customer's
 * status change are coordinated by {@link Restaurant}.
 *
 * <p>Owner: M2: Kai Xuan. See docs/class-contracts.md for the implementation contract.
 */
public class Table {
    private final int id;
    private Customer occupant;

    /**
     * Creates a free table. Uniqueness of IDs is checked by Restaurant.
     *
     * @throws IllegalArgumentException if the ID is not positive
     */
    public Table(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Table ID must be positive: " + id);
        }
        this.id = id;
    }

    /** Returns true when nobody is sitting at this table. */
    public boolean isAvailable() {
        return occupant == null;
    }

    /**
     * Seats a customer at this table.
     *
     * @throws IllegalArgumentException if the customer is null
     * @throws IllegalStateException if the table is already occupied
     */
    public void assign(Customer c) {
        if (c == null) {
            throw new IllegalArgumentException("Cannot assign a null customer to table " + id);
        }
        // Brief 3.4: a table stays occupied until its customer pays or leaves.
        if (!isAvailable()) {
            throw new IllegalStateException(
                    "Table " + id + " is already occupied by customer " + occupant.getId());
        }
        occupant = c;
    }

    /**
     * Frees this table after a payment or abandonment. Only Restaurant should call this.
     *
     * @throws IllegalStateException if the table is already empty, since that indicates
     *         a bug in the game logic rather than an invalid player command
     */
    public void release() {
        if (occupant == null) {
            throw new IllegalStateException("Table " + id + " is already empty");
        }
        occupant = null;
    }

    public int getId() {
        return id;
    }

    /** Returns the current customer, or null if the table is free. */
    public Customer getOccupant() {
        return occupant;
    }
}
