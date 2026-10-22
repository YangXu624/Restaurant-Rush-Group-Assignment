package restaurantrush;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import restaurantrush.boundary.RestaurantCLI;
import restaurantrush.control.GameController;
import restaurantrush.control.TurnManager;
import restaurantrush.entity.Restaurant;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Waiter;

/** Creates the shared Stage 1 collaborators and launches the console game. */
public class Main {
    public static void main(String[] args) {
        Restaurant restaurant = new Restaurant();
        Cashier cashier = new Cashier();
        TurnManager turnManager = new TurnManager(cashier);
        Waiter waiter = new Waiter();
        Chef chef = new Chef();
        GameController controller = new GameController(restaurant, turnManager, waiter, chef);
        Scanner input = new Scanner(System.in, StandardCharsets.UTF_8);
        new RestaurantCLI(controller, input, System.out).run();
    }
}
