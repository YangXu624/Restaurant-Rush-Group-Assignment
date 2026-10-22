package restaurantrush.support;

import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.OrderStatus;

/**
 * Immutable display data for one arrived customer, without a mutable Customer or
 * Order reference. All order fields are null before ordering; tableId is null
 * when the customer does not occupy a table. Historical order data may remain.
 *
 * <p>Construction checks value shape only, not customer or order lifecycle rules.
 * All components are primitives or immutable values, so no defensive copies are needed.
 *
 * <p>Owner: M5: Toshiki. See docs/class-contracts.md for the contract.
 */
public record CustomerView(
        int id,
        int satisfaction,
        CustomerStatus status,
        OrderStatus orderStatus,
        String itemName,
        Integer remainingUnits,
        Integer tableId) {

    /** @throws IllegalArgumentException if a value or optional order group is invalid */
    public CustomerView {
        if (id <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive");
        }
        if (satisfaction < 0 || satisfaction > 100) {
            throw new IllegalArgumentException("Satisfaction must be between 0 and 100");
        }
        if (status == null) {
            throw new IllegalArgumentException("Customer status cannot be null");
        }
        boolean noOrder = orderStatus == null && itemName == null && remainingUnits == null;
        boolean completeOrder = orderStatus != null && itemName != null && remainingUnits != null;
        if (!noOrder && !completeOrder) {
            throw new IllegalArgumentException("Order fields must be all absent or all present");
        }
        if (remainingUnits != null && remainingUnits < 0) {
            throw new IllegalArgumentException("Remaining units cannot be negative");
        }
        if (tableId != null && tableId <= 0) {
            throw new IllegalArgumentException("Table ID must be positive when present");
        }
    }
}
