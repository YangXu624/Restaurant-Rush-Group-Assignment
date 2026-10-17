package restaurantrush.boundary;

import java.util.Scanner;
import restaurantrush.control.GameController;
import restaurantrush.support.GameSnapshot;

/**
 * Stage 1 skeleton. Owner: M5: Toshiki.
 * See docs/class-contracts.md for the implementation contract.
 */
public class RestaurantCLI {
    private GameController controller;
    private Scanner input;

    // TODO: Agree constructor signatures and collaborator wiring with the team.

    public void run() {
        throw new UnsupportedOperationException("RestaurantCLI.run is not implemented yet");
    }

    public void displayState(GameSnapshot snapshot) {
        throw new UnsupportedOperationException("RestaurantCLI.displayState is not implemented yet");
    }

    public String readWaiterChoice() {
        throw new UnsupportedOperationException("RestaurantCLI.readWaiterChoice is not implemented yet");
    }

    public String readChefChoice() {
        throw new UnsupportedOperationException("RestaurantCLI.readChefChoice is not implemented yet");
    }

    public void showFeedback(String message) {
        throw new UnsupportedOperationException("RestaurantCLI.showFeedback is not implemented yet");
    }

    public void displayOutcome(boolean won) {
        throw new UnsupportedOperationException("RestaurantCLI.displayOutcome is not implemented yet");
    }
}
