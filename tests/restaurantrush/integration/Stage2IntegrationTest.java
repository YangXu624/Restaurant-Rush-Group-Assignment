package restaurantrush.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import restaurantrush.control.GameController;
import restaurantrush.control.TurnManager;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.CriticCustomer;
import restaurantrush.entity.OrderStatus;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.VIPCustomer;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Waiter;
import restaurantrush.support.ActionResult;
import restaurantrush.support.GameSnapshot;

/** Stage 2 scheduling and resolution checks using real collaborators. */
public class Stage2IntegrationTest {
    public static void main(String[] args) throws Exception {
        testScheduledPenalties();
        testClampingDuringResolution();
        testVictoryBillsAndTiming();
        System.out.println("All Stage 2 integration tests passed successfully!");
    }

    private static void testScheduledPenalties() {
        Restaurant r = new Restaurant();
        GameController game = game(r);
        String[] types = {"Regular", "VIP", "Regular", "Regular", "Critic", "Regular"};
        int[] penalties = {8, 5, 8, 8, 12, 8};
        for (int turn = 1; turn <= 18; turn++) {
            game.startTurn();
            equal((turn - 1) / 3 + 1, r.getCustomers().size(), "Exact arrival schedule");
            for (Customer c : r.getCustomers()) {
                equal(types[c.getId() - 1], c.getCustomerType(), "Arrival type");
            }
            game.waitWaiter();
            game.waitChef();
            game.finishTurn();
            for (Customer c : r.getCustomers()) {
                int elapsed = turn - (1 + 3 * (c.getId() - 1)) + 1;
                int expected = Math.max(0, 100 - elapsed * penalties[c.getId() - 1]);
                equal(expected, c.getSatisfaction(), "Resolved subtype penalty");
                equal(expected == 0 ? CustomerStatus.LEFT : CustomerStatus.WAITING,
                        c.getStatus(), "Abandonment and eligibility");
            }
        }
        equal(true, game.isFinished(), "Finishes after 18 turns");
        equal(1, r.getAbandonedCount(), "Only first regular abandons");
        equal(0, r.getRevenueCents(), "Waiting creates no revenue");
    }

    private static void testClampingDuringResolution() {
        for (boolean seated : new boolean[] {false, true}) {
            List<Customer> customers = List.of(new Customer(20), new VIPCustomer(20), new CriticCustomer(20));
            int[] boundary = {4, 5, 4};
            int[] penaltyCalls = {12, 19, 8};
            for (int index = 0; index < customers.size(); index++) {
                Customer c = customers.get(index);
                Restaurant r = new Restaurant();
                r.addArrival(c);
                if (seated) {
                    new Waiter().seat(r, 20, 1);
                    new Waiter().takeOrder(r, 20);
                }
                // Prepare a positive boundary value, then let real turn resolution clamp it.
                for (int call = 0; call < penaltyCalls[index]; call++) {
                    c.applyWaitingPenalty();
                }
                equal(boundary[index], c.getSatisfaction(),
                        "Pre-resolution satisfaction for " + c.getCustomerType());
                GameController game = game(r);
                game.startTurn();
                game.waitWaiter();
                game.waitChef();
                game.finishTurn();
                equal(0, c.getSatisfaction(), "Clamp at zero");
                equal(CustomerStatus.LEFT, c.getStatus(), "Eligible customer abandons");
                equal(1, r.getAbandonedCount(), "Count departure once");
                if (seated) {
                    equal(OrderStatus.CANCELLED, c.getOrder().getStatus(), "Cancel unserved order");
                    equal(true, r.getTables().get(0).isAvailable(), "Release table");
                }
                game.startTurn(); game.waitWaiter(); game.waitChef(); game.finishTurn();
                equal(1, r.getAbandonedCount(), "No repeated abandonment");
            }
        }
    }

    private static void testVictoryBillsAndTiming() throws Exception {
        Restaurant r = new Restaurant();
        GameController game = game(r);
        List<String> commands = Files.readAllLines(Path.of("tests/fixtures/stage2/victory.txt"));
        equal(36, commands.size(), "One choice per staff per turn");
        GameSnapshot locked = null;
        int[] served = {3, 6, 10, 15, 16};
        int[] satisfaction = {84, 90, 76, 60, 64};
        int[] bills = {900, 1350, 1800, 900, 1800};
        for (int turn = 1; turn <= 18; turn++) {
            game.startTurn();
            for (int choice = 0; choice < 2; choice++) {
                String[] p = commands.get((turn - 1) * 2 + choice).split(" ");
                ActionResult result = switch (p[0]) {
                    case "seat" -> game.seatCustomer(Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                    case "order" -> game.takeOrder(Integer.parseInt(p[1]));
                    case "serve" -> game.serveOrder(Integer.parseInt(p[1]));
                    case "cook" -> game.prepareOrder(Integer.parseInt(p[1]));
                    case "wait" -> choice == 0 ? game.waitWaiter() : game.waitChef();
                    default -> throw new AssertionError("Unknown fixture command: " + p[0]);
                };
                equal(true, result.isSuccess(), "Fixture action: " + commands.get((turn - 1) * 2 + choice));
            }
            if (turn == 5) {
                locked = game.getSnapshot();
                equal(1350, locked.customers().get(1).billCents(), "VIP bill before payment");
                equal("Burger", locked.customers().get(1).itemName(), "VIP dish");
            }
            if (turn == 14) {
                equal("Pasta", r.findOrder(5).getItem().getName(), "Critic preference");
                equal(1, r.findOrder(5).getRemainingUnits(), "Pasta needs a second Chef choice");
            }
            game.finishTurn();
            for (int i = 0; i < served.length; i++) {
                Customer c = r.findCustomer(i + 1);
                if (c != null && turn >= served[i]) {
                    equal(satisfaction[i], c.getSatisfaction(), "No penalties after service");
                    equal(turn == served[i] ? CustomerStatus.SERVED
                            : turn == served[i] + 1 ? CustomerStatus.READY_TO_PAY : CustomerStatus.PAID,
                            c.getStatus(), "Eating/payment delays");
                }
            }
        }
        GameSnapshot end = game.getSnapshot();
        equal(List.of("Regular", "VIP", "Regular", "Regular", "Critic", "Regular"),
                end.customers().stream().map(c -> c.customerType()).toList(), "Snapshot type labels");
        for (int i = 0; i < bills.length; i++) {
            equal(bills[i], end.customers().get(i).billCents(), "Historical locked bill");
        }
        equal(1350, locked.customers().get(1).billCents(), "Old snapshot bill retained");
        equal(CustomerStatus.SEATED, locked.customers().get(1).status(), "Old snapshot detached");
        equal(null, end.customers().get(5).billCents(), "No order means absent bill");
        equal(6750, end.revenueCents(), "Sum locked bills");
        equal(74.8, end.averagePaidSatisfaction(), "374 / 5 paid satisfaction");
        equal(true, game.isVictory(), "Victory after completion");
        equal(false, new Cashier().collectOne(r, 19), "Paid customers cannot pay twice");
        equal(6750, r.getRevenueCents(), "No duplicate revenue");
    }

    private static GameController game(Restaurant r) {
        return new GameController(r, new TurnManager(new Cashier()), new Waiter(), new Chef());
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
