package restaurantrush.support;

import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.OrderStatus;

/**
 * Stage 1 skeleton. Owner: M5: Toshiki.
 * See docs/class-contracts.md for the implementation contract.
 */
public record CustomerView(
        int id,
        int satisfaction,
        CustomerStatus status,
        OrderStatus orderStatus,
        String itemName,
        Integer remainingUnits,
        Integer tableId) {

    // TODO: Implement construction according to the record contract.
    public CustomerView {
        throw new UnsupportedOperationException("CustomerView construction is not implemented yet");
    }
}
