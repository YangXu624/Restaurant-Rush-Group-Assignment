package restaurantrush.staff;

import restaurantrush.entity.Order;
import restaurantrush.support.ActionResult;

/**
 * Prepares one unit of an order per task. Stateless: the one-task-per-turn limit is
 * enforced by GameController and TurnManager.
 *
 * <p>Owner: M3: Marcus. See docs/class-contracts.md for the implementation contract.
 */
public class Chef {

    /** Prepares one unit of the order. Changes nothing on failure. */
    public ActionResult prepareOneUnit(Order o) {
        if (o == null) {
            return ActionResult.fail("There is no order to cook.");
        }
        // Order.canPrepare() owns the status rules, so they are not repeated here.
        if (!o.canPrepare()) {
            return ActionResult.fail("Order " + o.getId() + " cannot be cooked ("
                    + o.getStatus() + ").");
        }

        o.prepareOneUnit();
        String dish = o.getItem().getName();
        if (o.isReady()) {
            return ActionResult.ok("Cooked " + dish + " for customer " + o.getId()
                    + "; it is now READY.");
        }
        return ActionResult.ok("Cooked one unit of " + dish + " for customer " + o.getId()
                + "; " + o.getRemainingUnits() + " unit(s) left.");
    }
}
