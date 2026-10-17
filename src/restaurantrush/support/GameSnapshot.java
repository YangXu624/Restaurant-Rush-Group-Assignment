package restaurantrush.support;

import java.util.List;
import restaurantrush.control.TurnManager;
import restaurantrush.entity.Restaurant;

/**
 * Stage 1 skeleton. Owner: M5: Toshiki.
 * See docs/class-contracts.md for the implementation contract.
 */
public record GameSnapshot(
        int turn,
        int revenueCents,
        Double averagePaidSatisfaction,
        List<Integer> waitingCustomerIds,
        List<CustomerView> customers,
        List<TableView> tables,
        boolean waiterAvailable,
        boolean chefAvailable,
        int paidCount,
        int servedCount,
        int abandonedCount) {

    // TODO: Implement construction according to the record contract.
    public GameSnapshot {
        throw new UnsupportedOperationException("GameSnapshot construction is not implemented yet");
    }

    public static GameSnapshot from(Restaurant r, TurnManager t) {
        throw new UnsupportedOperationException("GameSnapshot.from is not implemented yet");
    }
}
