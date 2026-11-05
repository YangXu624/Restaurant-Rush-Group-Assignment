package restaurantrush.entity;

import java.util.Objects;
import restaurantrush.control.GameController;
import restaurantrush.control.TurnManager;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Host;
import restaurantrush.staff.Waiter;
import restaurantrush.support.ActionResult;
import restaurantrush.support.GameSnapshot;

public class ComboMealTest {
    public static void main(String[] args) {
        MenuItem salad = new MenuItem("Salad", 900, 1);
        MenuItem pasta = new MenuItem("Pasta", 1800, 2);
        ComboMeal combo = new ComboMeal(salad, pasta);

        if (combo.getFirstItem() != salad || combo.getSecondItem() != pasta) {
            throw new AssertionError("Combo should contain the original menu items");
        }
        if (combo.getPriceCents() != 2430 || combo.getPreparationUnits() != 3) {
            throw new AssertionError("Salad + Pasta should cost $24.30 and take 3 units");
        }

        MenuItem small = new MenuItem("Small", 3, 1);
        MenuItem side = new MenuItem("Side", 2, 1);
        if (new ComboMeal(small, side).getPriceCents() != 5) {
            throw new AssertionError("90% of 5 cents is 4.5 cents and should round up to 5");
        }
        testRoundingDown();
        testNullComponents();
        testServingRequiresThreeChefActions();
        System.out.println("All ComboMeal tests passed successfully!");
    }

    private static void testRoundingDown() {
        MenuItem first = new MenuItem("First", 3, 1);
        MenuItem second = new MenuItem("Second", 3, 1);
        equal(5, new ComboMeal(first, second).getPriceCents(),
                "90% of 6 cents is 5.4 cents and should round down to 5");
    }

    private static void testNullComponents() {
        MenuItem item = new MenuItem("Salad", 900, 1);
        expectIllegalArgument(() -> new ComboMeal(null, item));
        expectIllegalArgument(() -> new ComboMeal(item, null));
    }

    /** Uses real collaborators so failed service must preserve the Waiter's choice. */
    private static void testServingRequiresThreeChefActions() {
        Restaurant restaurant = new Restaurant();
        TurnManager manager = new TurnManager(new Cashier());
        GameController game = new GameController(restaurant, manager, new Host(), new Waiter(), new Chef());
        // Customer 4 arrives on turn 10 and selects the fourth menu item (the combo).
        for (int turn = 1; turn < 10; turn++) {
            game.startTurn();
            succeed(game.waitHost());
            succeed(game.waitWaiter());
            succeed(game.waitChef());
            game.finishTurn();
        }
        game.startTurn();
        succeed(game.waitHost());
        succeed(game.seatCustomer(4, 1));
        succeed(game.waitChef());
        game.finishTurn();

        game.startTurn(); // Turn 11: take the order and complete preparation unit 1.
        succeed(game.waitHost());
        succeed(game.takeOrder(4));
        Customer customer = restaurant.findCustomer(4);
        Order order = customer.getOrder();
        equal(true, order.getItem() instanceof ComboMeal, "Customer 4 selects the combo");
        equal(3, order.getRemainingUnits(), "Combo initially needs three Chef actions");
        succeed(game.prepareOrder(4));
        equal(2, order.getRemainingUnits(), "First Chef action completes one unit");
        game.finishTurn();

        game.startTurn(); // Turn 12: preparation unit 2.
        succeed(game.waitHost());
        succeed(game.waitWaiter());
        succeed(game.prepareOrder(4));
        game.finishTurn();

        game.startTurn(); // Turn 13: serving is too early; one unit is still needed.
        succeed(game.waitHost());
        equal(OrderStatus.PREPARING, order.getStatus(), "Two actions do not finish the combo");
        equal(1, order.getRemainingUnits(), "One preparation unit remains");
        GameSnapshot before = game.getSnapshot();
        equal(false, game.serveOrder(4).isSuccess(), "Reject serving partially prepared combo");
        equal(before, game.getSnapshot(), "Rejected service changes neither state nor staff availability");
        equal(true, manager.canAssignWaiter(), "Failed service does not consume Waiter choice");
        equal(CustomerStatus.SEATED, customer.getStatus(), "Customer remains unserved");
        equal(0, restaurant.getServedCount(), "No service recorded on failure");

        succeed(game.waitWaiter()); // The same Waiter choice is still usable.
        succeed(game.prepareOrder(4)); // Preparation unit 3.
        equal(0, order.getRemainingUnits(), "Third Chef action completes preparation");
        equal(OrderStatus.READY, order.getStatus(), "Combo becomes READY");
        before = game.getSnapshot();
        equal(false, game.serveOrder(4).isSuccess(), "Cannot serve after Chef acts in the same turn");
        equal(before, game.getSnapshot(), "Out-of-phase serving changes nothing");
        game.finishTurn();

        game.startTurn(); // Turn 14: the READY combo can now be served.
        succeed(game.waitHost());
        equal(14, manager.getCurrentTurn(), "Serve on the next turn");
        succeed(game.serveOrder(4));
        equal(OrderStatus.SERVED, order.getStatus(), "Order is served");
        equal(CustomerStatus.SERVED, customer.getStatus(), "Customer starts eating");
        equal(14, customer.getServedTurn(), "Service timestamp is the next turn");
        equal(1, restaurant.getServedCount(), "Record exactly one service");
        equal(false, manager.canAssignWaiter(), "Successful service consumes Waiter choice");
        succeed(game.waitChef());
        game.finishTurn();
    }

    private static void succeed(ActionResult result) {
        if (!result.isSuccess()) {
            throw new AssertionError("Expected accepted action: " + result.getMessage());
        }
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException for null combo component");
    }
}
