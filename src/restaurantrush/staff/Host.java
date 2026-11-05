package restaurantrush.staff;

import restaurantrush.entity.Restaurant;
import restaurantrush.support.ActionResult;

/** Seats customers. TurnManager owns the one-choice-per-turn limit. */
public class Host {
    public ActionResult seat(Restaurant restaurant, int customerId, int tableId) {
        return restaurant.seat(customerId, tableId);
    }
}
