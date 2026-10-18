package restaurantrush.staff;

import restaurantrush.entity.Restaurant;
import restaurantrush.support.ActionResult;

/**
 * Stage 1 skeleton. Owner: M3: Marcus.
 * See docs/class-contracts.md for the implementation contract.
 */
public class Waiter {

    public ActionResult seat(Restaurant r, int cId, int tableId) {
        throw new UnsupportedOperationException("Waiter.seat is not implemented yet");
    }

    public ActionResult takeOrder(Restaurant r, int customerId) {
        throw new UnsupportedOperationException("Waiter.takeOrder is not implemented yet");
    }

    public ActionResult serve(Restaurant r, int customerId, int turn) {
        throw new UnsupportedOperationException("Waiter.serve is not implemented yet");
    }
}
