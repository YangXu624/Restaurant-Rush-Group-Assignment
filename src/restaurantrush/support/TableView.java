package restaurantrush.support;

/**
 * Immutable table display data. A null occupantId means the table is free.
 * Stores IDs rather than mutable Table or Customer references; the restaurant
 * owns occupancy changes and the configured set of tables.
 *
 * <p>Owner: M5: Toshiki. See docs/class-contracts.md for the contract.
 */
public record TableView(
        int id,
        Integer occupantId) {

    /** @throws IllegalArgumentException if either supplied ID is not positive */
    public TableView {
        if (id <= 0) {
            throw new IllegalArgumentException("Table ID must be positive");
        }
        if (occupantId != null && occupantId <= 0) {
            throw new IllegalArgumentException("Occupant ID must be positive when present");
        }
    }
}
