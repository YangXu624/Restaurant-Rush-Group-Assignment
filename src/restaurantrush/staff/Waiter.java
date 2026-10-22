package restaurantrush.staff;

import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.MenuItem;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.support.ActionResult;

/**
 * Seats customers, takes orders and serves dishes. Stateless: the one-task-per-turn
 * limit is enforced by GameController and TurnManager.
 *
 * <p>Owner: M3: Marcus. See docs/class-contracts.md for the implementation contract.
 */
public class Waiter {

    /** Delegates to Restaurant, which validates before changing anything. */
    public ActionResult seat(Restaurant r, int cId, int tableId) {
        return r.seat(cId, tableId);
    }

    /**
     * Creates the customer's order, links it to the customer and registers it.
     * Changes nothing on failure.
     */
    public ActionResult takeOrder(Restaurant r, int customerId) {
        // Brief 3.4: an invalid choice explains why it failed and changes nothing.
        // Every check runs before the order is linked or registered, so a rejected
        // request cannot leave a half-registered order behind.
        Customer c = r.findCustomer(customerId);
        if (c == null) {
            return ActionResult.fail("Customer " + customerId + " has not arrived.");
        }
        if (c.getStatus() != CustomerStatus.SEATED) {
            return ActionResult.fail("Customer " + customerId + " is not seated ("
                    + c.getStatus() + ").");
        }
        if (c.getOrder() != null || r.findOrder(customerId) != null) {
            return ActionResult.fail("Customer " + customerId + " has already ordered.");
        }

        MenuItem item = c.chooseItem(r.getMenu());
        // The order shares its customer's ID so Restaurant.findOrder() can locate it.
        Order order = new Order(customerId, item);
        c.acceptOrder(order);
        r.registerOrder(order);
        return ActionResult.ok("Customer " + customerId + " ordered " + item.getName() + ".");
    }

    /** Delegates to Restaurant, which updates the order, customer and served count. */
    public ActionResult serve(Restaurant r, int customerId, int turn) {
        return r.serve(customerId, turn);
    }
}
