package restaurantrush.control;

import restaurantrush.entity.Restaurant;
import restaurantrush.staff.Cashier;

/**
 * Stage 1 skeleton. Owner: M4: Nickolas.
 * See docs/class-contracts.md for the implementation contract.
 */
public class TurnManager {
    private int currentTurn;
    private boolean waiterTaskUsed;
    private boolean chefTaskUsed;
    private boolean turnStarted;
    private boolean turnResolved;
    private boolean finished;
    private Cashier cashier;

    // TODO: Agree constructor signatures and collaborator wiring with the team.

    public void beginTurn(Restaurant r) {
        throw new UnsupportedOperationException("TurnManager.beginTurn is not implemented yet");
    }

    public boolean canAssignWaiter() {
        throw new UnsupportedOperationException("TurnManager.canAssignWaiter is not implemented yet");
    }

    public boolean canAssignChef() {
        throw new UnsupportedOperationException("TurnManager.canAssignChef is not implemented yet");
    }

    public void recordWaiterTask() {
        throw new UnsupportedOperationException("TurnManager.recordWaiterTask is not implemented yet");
    }

    public void recordChefTask() {
        throw new UnsupportedOperationException("TurnManager.recordChefTask is not implemented yet");
    }

    public void resolveTurn(Restaurant r) {
        throw new UnsupportedOperationException("TurnManager.resolveTurn is not implemented yet");
    }

    public void advanceTurn() {
        throw new UnsupportedOperationException("TurnManager.advanceTurn is not implemented yet");
    }

    public boolean isFinished() {
        throw new UnsupportedOperationException("TurnManager.isFinished is not implemented yet");
    }

    public int getCurrentTurn() {
        throw new UnsupportedOperationException("TurnManager.getCurrentTurn is not implemented yet");
    }
}
