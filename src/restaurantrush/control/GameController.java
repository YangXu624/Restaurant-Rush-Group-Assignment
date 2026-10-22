package restaurantrush.control;

import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Order;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Waiter;
import restaurantrush.support.ActionResult;
import restaurantrush.support.GameSnapshot;

/**
 * Coordinates Stage 1 actions using the supplied shared collaborators. Owner: M4: Nickolas.
 * See docs/class-contracts.md for the implementation contract.
 */
public class GameController {
    private static final int MIN_PAID_COUNT = 4;
    private static final int MIN_REVENUE_CENTS = 4500;
    private static final double MIN_AVERAGE_PAID_SATISFACTION = 60;

    private final Restaurant restaurant;
    private final TurnManager turnManager;
    private final Waiter waiter;
    private final Chef chef;

    public GameController(Restaurant restaurant, TurnManager turnManager, Waiter waiter, Chef chef) {
        if (restaurant == null || turnManager == null || waiter == null || chef == null) {
            throw new IllegalArgumentException("Game collaborators cannot be null");
        }
        this.restaurant = restaurant;
        this.turnManager = turnManager;
        this.waiter = waiter;
        this.chef = chef;
    }

    public void startTurn() {
        turnManager.beginTurn(restaurant);
    }

    public ActionResult seatCustomer(int customerId, int tableId) {
        if (!turnManager.canAssignWaiter()) {
            return ActionResult.fail("Waiter choice is unavailable.");
        }
        ActionResult result = waiter.seat(restaurant, customerId, tableId);
        if (result.isSuccess()) {
            turnManager.recordWaiterTask();
        }
        return result;
    }

    public ActionResult takeOrder(int customerId) {
        if (!turnManager.canAssignWaiter()) {
            return ActionResult.fail("Waiter choice is unavailable.");
        }
        ActionResult result = waiter.takeOrder(restaurant, customerId);
        if (result.isSuccess()) {
            turnManager.recordWaiterTask();
        }
        return result;
    }

    public ActionResult serveOrder(int customerId) {
        if (!turnManager.canAssignWaiter()) {
            return ActionResult.fail("Waiter choice is unavailable.");
        }
        ActionResult result = waiter.serve(restaurant, customerId, turnManager.getCurrentTurn());
        if (result.isSuccess()) {
            turnManager.recordWaiterTask();
        }
        return result;
    }

    public ActionResult prepareOrder(int customerId) {
        if (!turnManager.canAssignChef()) {
            return ActionResult.fail("Chef choice is unavailable.");
        }
        Order order = restaurant.findOrder(customerId);
        if (order == null) {
            return ActionResult.fail("Customer " + customerId + " has no order.");
        }
        ActionResult result = chef.prepareOneUnit(order);
        if (result.isSuccess()) {
            turnManager.recordChefTask();
        }
        return result;
    }

    public ActionResult waitWaiter() {
        if (!turnManager.canAssignWaiter()) {
            return ActionResult.fail("Waiter choice is unavailable.");
        }
        turnManager.recordWaiterTask();
        return ActionResult.ok("Waiter waits.");
    }

    public ActionResult waitChef() {
        if (!turnManager.canAssignChef()) {
            return ActionResult.fail("Chef choice is unavailable.");
        }
        turnManager.recordChefTask();
        return ActionResult.ok("Chef waits.");
    }

    public void finishTurn() {
        turnManager.resolveTurn(restaurant);
        turnManager.advanceTurn();
    }

    public GameSnapshot getSnapshot() {
        return GameSnapshot.from(restaurant, turnManager);
    }

    public boolean isVictory() {
        return turnManager.isFinished()
                && restaurant.getPaidCount() >= MIN_PAID_COUNT
                && restaurant.getRevenueCents() >= MIN_REVENUE_CENTS
                && restaurant.averagePaidSatisfaction() >= MIN_AVERAGE_PAID_SATISFACTION;
    }

    public boolean isFinished() {
        return turnManager.isFinished();
    }
}
