package restaurantrush.control;

import java.util.Objects;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.Restaurant;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Host;
import restaurantrush.staff.Waiter;

/** Activation timing and once-per-turn recovery, using real collaborators. */
public class HappyHourTurnTest {
    public static void main(String[] args) {
        testRecoveryOncePerActiveTurn();
        testActivationOnlyAtTurnStart();
        testRecoverySkipsServedAndDepartedCustomers();
        System.out.println("All Happy Hour turn tests passed successfully!");
    }

    private static void testRecoveryOncePerActiveTurn() {
        Restaurant r = new Restaurant();
        GameController game = game(r);
        for (int turn = 1; turn <= 3; turn++) {
            playWaitingTurn(game);
        }
        Customer customer = r.findCustomer(1);
        equal(76, customer.getSatisfaction(), "Three waiting penalties");

        game.startTurn();
        equal(76, customer.getSatisfaction(), "No recovery before activation");
        equal(true, game.activateHappyHour().isSuccess(), "Activation on turn 4");
        equal(91, customer.getSatisfaction(), "Activation turn recovers 15");
        equal(false, game.activateHappyHour().isSuccess(), "A repeated command fails");
        game.getSnapshot();
        equal(false, game.hostSeatCustomer(99, 1).isSuccess(), "A rejected command changes nothing");
        equal(91, customer.getSatisfaction(), "Retries and redisplays award nothing");
        finishWaitingTurn(game);
        equal(83, customer.getSatisfaction(), "Waiting penalty still applies at turn end");
        equal(1, r.getHappyHour().getTurnsRemaining(), "One active turn remains");

        game.startTurn();
        equal(98, customer.getSatisfaction(), "Second active turn recovers at turn start");
        equal(false, game.canActivateHappyHour(), "Used promotion cannot be offered again");
        finishWaitingTurn(game);
        equal(90, customer.getSatisfaction(), "Penalty on the last active turn");
        equal(false, r.getHappyHour().isActive(), "Expired after two turns");

        game.startTurn();
        equal(90, customer.getSatisfaction(), "No recovery after expiry");
        finishWaitingTurn(game);
        equal(82, customer.getSatisfaction(), "Normal penalty after expiry");
    }

    private static void testActivationOnlyAtTurnStart() {
        Restaurant r = new Restaurant();
        GameController game = game(r);
        equal(false, game.canActivateHappyHour(), "Not offered before the turn starts");
        equal(false, game.activateHappyHour().isSuccess(), "Rejected before the turn starts");
        equal(false, r.getHappyHour().isUsed(), "Rejection does not use the promotion");

        game.startTurn();
        equal(true, game.canActivateHappyHour(), "Offered at the start of a turn");
        equal(false, game.hostSeatCustomer(99, 1).isSuccess(), "Failed Host command");
        equal(true, game.canActivateHappyHour(), "A failed Host command keeps the decision open");
        equal(true, game.waitHost().isSuccess(), "Host acts");
        equal(false, game.canActivateHappyHour(), "Not offered once the Host has acted");
        equal(false, game.activateHappyHour().isSuccess(), "Rejected once the Host has acted");
        equal(false, r.getHappyHour().isUsed(), "Rejection does not use the promotion");
    }

    private static void testRecoverySkipsServedAndDepartedCustomers() {
        Restaurant r = new Restaurant();
        Customer departed = new Customer(20);
        r.addArrival(departed);
        for (int i = 0; i < 12; i++) {
            departed.applyWaitingPenalty();
        }
        GameController game = game(r);

        // Turn 1: seat, order and cook customer 1. Customer 20 falls to zero and leaves.
        game.startTurn();
        game.hostSeatCustomer(1, 1);
        game.takeOrder(1);
        game.prepareOrder(1);
        game.finishTurn();
        equal(CustomerStatus.LEFT, departed.getStatus(), "Customer 20 left");

        // Turn 2: serve customer 1, whose satisfaction then freezes.
        game.startTurn();
        game.waitHost();
        game.serveOrder(1);
        game.waitChef();
        game.finishTurn();
        Customer served = r.findCustomer(1);
        equal(CustomerStatus.SERVED, served.getStatus(), "Customer 1 is eating");
        equal(92, served.getSatisfaction(), "Satisfaction frozen at service");

        game.startTurn();
        equal(true, game.activateHappyHour().isSuccess(), "Activation on turn 3");
        equal(92, served.getSatisfaction(), "Served customer does not recover");
        equal(0, departed.getSatisfaction(), "Departed customer does not recover");
    }

    private static void playWaitingTurn(GameController game) {
        game.startTurn();
        finishWaitingTurn(game);
    }

    private static void finishWaitingTurn(GameController game) {
        game.waitHost();
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
    }

    private static GameController game(Restaurant r) {
        return new GameController(r, new TurnManager(new Cashier()), new Host(), new Waiter(), new Chef());
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
