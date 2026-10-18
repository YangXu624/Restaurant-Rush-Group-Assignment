package restaurantrush.control;

import restaurantrush.entity.Restaurant;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Waiter;
import restaurantrush.support.ActionResult;
import restaurantrush.support.GameSnapshot;

/**
 * Stage 1 skeleton. Owner: M4: Nickolas.
 * See docs/class-contracts.md for the implementation contract.
 */
public class GameController {
    private Restaurant restaurant;
    private TurnManager turnManager;
    private Waiter waiter;
    private Chef chef;

    public GameController(Restaurant restaurant, TurnManager turnManager, Waiter waiter, Chef chef) {
        throw new UnsupportedOperationException("GameController.GameController is not implemented yet");
    }

    public void startTurn() {
        throw new UnsupportedOperationException("GameController.startTurn is not implemented yet");
    }

    public ActionResult seatCustomer(int customerId, int tableId) {
        throw new UnsupportedOperationException("GameController.seatCustomer is not implemented yet");
    }

    public ActionResult takeOrder(int customerId) {
        throw new UnsupportedOperationException("GameController.takeOrder is not implemented yet");
    }

    public ActionResult serveOrder(int customerId) {
        throw new UnsupportedOperationException("GameController.serveOrder is not implemented yet");
    }

    public ActionResult prepareOrder(int customerId) {
        throw new UnsupportedOperationException("GameController.prepareOrder is not implemented yet");
    }

    public ActionResult waitWaiter() {
        throw new UnsupportedOperationException("GameController.waitWaiter is not implemented yet");
    }

    public ActionResult waitChef() {
        throw new UnsupportedOperationException("GameController.waitChef is not implemented yet");
    }

    public void finishTurn() {
        throw new UnsupportedOperationException("GameController.finishTurn is not implemented yet");
    }

    public GameSnapshot getSnapshot() {
        throw new UnsupportedOperationException("GameController.getSnapshot is not implemented yet");
    }

    public boolean isVictory() {
        throw new UnsupportedOperationException("GameController.isVictory is not implemented yet");
    }

    public boolean isFinished() {
        throw new UnsupportedOperationException("GameController.isFinished is not implemented yet");
    }
}
