package restaurantrush.integration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import restaurantrush.boundary.RestaurantCLI;
import restaurantrush.control.GameController;
import restaurantrush.control.TurnManager;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.OrderStatus;
import restaurantrush.entity.Restaurant;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Waiter;
import restaurantrush.support.CustomerView;
import restaurantrush.support.GameSnapshot;
import restaurantrush.support.TableView;

/** File-backed CLI integration evidence. Does not claim to test the production Main. */
public class M5IntegrationTest {
    private static final Path FIXTURES = Path.of("tests", "fixtures", "m5");

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && args[0].equals("--replay")) {
            // Test-only replay while production launch wiring is owned by M4.
            new Game(new Scanner(System.in), System.out).cli.run();
            return;
        }
        if (args.length != 0) {
            throw new IllegalArgumentException("Use no arguments for tests, or --replay for stdin commands");
        }
        Result victory = runFixture("victory.txt");
        testVictory(victory);
        testDefeat(runFixture("defeat.txt"));
        testInvalidInput(runFixture("invalid-input.txt"), victory);
        testAbandonment(runFixture("abandonment.txt"));
        testEof(runFixture("eof-waiter.txt"), false);
        testEof(runFixture("eof-chef.txt"), true);
        System.out.println("All M5 fixture integration tests passed successfully!");
    }

    private static Result runFixture(String name) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Scanner input = new Scanner(FIXTURES.resolve(name), StandardCharsets.UTF_8);
                PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            Game game = new Game(input, output);
            game.cli.run();
            equal(false, input.hasNextLine(), name + ": all fixture input consumed");
            return new Result(game.controller.getSnapshot(), game.controller.isFinished(),
                    game.controller.isVictory(), bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n"));
        }
    }

    private static void testVictory(Result result) {
        complete(result, true);
        GameSnapshot state = result.state();
        equal(5, state.paidCount(), "Victory paid count");
        equal(5, state.servedCount(), "Victory served count");
        equal(0, state.abandonedCount(), "Victory departures");
        equal(6600, state.revenueCents(), "Five paid menu bills");
        equal(79.2, state.averagePaidSatisfaction(), "Unrounded recorded average");
        equal(List.of(), state.waitingCustomerIds(), "All customers seated");
        equal(List.of(new TableView(1, null), new TableView(2, 6)), state.tables(), "Final customer retains table");
        equal(List.of(1, 2, 3, 4, 5, 6), state.customers().stream().map(CustomerView::id).toList(), "Six arrivals in order");
        equal(new CustomerView(6, 76, CustomerStatus.SEATED, OrderStatus.PREPARING, "Pasta", 1, 2),
                state.customers().get(5), "Turn-16 arrival cannot complete payment");
        for (int i = 0; i < 5; i++) {
            equal(CustomerStatus.PAID, state.customers().get(i).status(), "Earlier customer paid");
            equal(null, state.customers().get(i).tableId(), "Paid customer releases table");
        }
        String text = result.text();
        contains(turnDisplay(text, 9), "Customer 3 | satisfaction 84 | SEATED | table 1 | order: Pasta | PREPARING | 1 unit(s) remaining");
        contains(turnDisplay(text, 10), "Customer 3 | satisfaction 76 | SEATED | table 1 | order: Pasta | READY | 0 unit(s) remaining");
        contains(turnDisplay(text, 11), "Customer 3 | satisfaction 76 | SERVED");
        contains(turnDisplay(text, 12), "Customer 3 | satisfaction 76 | READY_TO_PAY");
        contains(text, "End of turn 11\nRevenue: $24.00");
        contains(text, "End of turn 12\nRevenue: $42.00");
        contains(text, "Victory\nRevenue: $66.00\nAverage paid satisfaction: 79.20");
    }

    private static void testDefeat(Result result) {
        complete(result, false);
        equal(0, result.state().paidCount(), "No payments while waiting");
        equal(0, result.state().servedCount(), "No service while waiting");
        equal(0, result.state().revenueCents(), "No unpaid revenue");
        equal(null, result.state().averagePaidSatisfaction(), "No paid average");
        equal(2, result.state().abandonedCount(), "First two customers reach zero by turn 18");
        equal(List.of(3, 4, 5, 6), result.state().waitingCustomerIds(), "Only active arrivals remain queued");
        equal(List.of(new TableView(1, null), new TableView(2, null)), result.state().tables(), "Unused tables free");
        equal(CustomerStatus.LEFT, result.state().customers().get(0).status(), "Departed customer retained");
        contains(result.text(), "Defeat\nRevenue: $0.00\nAverage paid satisfaction: N/A");
    }

    private static void testInvalidInput(Result invalid, Result clean) {
        complete(invalid, true);
        equal(clean.state(), invalid.state(), "Invalid attempts change no state, timing or final result");
        contains(invalid.text(), "Invalid command.");
        equal(2, occurrences(invalid.text(), "IDs must be whole numbers"), "Malformed ID and overflow both rejected");
        contains(invalid.text(), "Customer 99 has not arrived.");
        contains(invalid.text(), "Customer 1 has no order.");
        contains(invalid.text(), "Customer 1 has already ordered.");
        contains(invalid.text(), "cannot be cooked (SERVED)");
    }

    private static void testAbandonment(Result result) {
        complete(result, false);
        equal(0, result.state().revenueCents(), "Abandoned order creates no revenue");
        equal(2, result.state().abandonedCount(), "Departures counted once");
        equal(new CustomerView(1, 0, CustomerStatus.LEFT, OrderStatus.CANCELLED, "Salad", 1, null),
                result.state().customers().get(0), "Abandonment cancels unfinished order and clears table");
        equal(List.of(new TableView(1, null), new TableView(2, null)), result.state().tables(), "Abandonment frees occupied table");
        contains(turnDisplay(result.text(), 14), "Table 1: free");
        contains(turnDisplay(result.text(), 14), "Customer 1 | satisfaction 0 | LEFT");
    }

    private static void testEof(Result result, boolean waiterActed) {
        equal(false, result.finished(), "EOF does not finish game");
        equal(false, result.won(), "EOF is not victory");
        equal(1, result.state().turn(), "Incomplete turn does not advance");
        equal(100, result.state().customers().get(0).satisfaction(), "No automatic penalty for incomplete turn");
        equal(!waiterActed, result.state().waiterAvailable(), "EOF preserves Waiter choice");
        equal(waiterActed, result.state().chefAvailable(), "EOF preserves Chef choice");
        equal(waiterActed ? CustomerStatus.SEATED : CustomerStatus.WAITING,
                result.state().customers().get(0).status(), "Accepted action remains applied");
        contains(result.text(), "Input ended. The current turn was not completed.");
        for (String forbidden : List.of("End of turn", "Turns used:", "Victory", "Defeat")) {
            equal(0, occurrences(result.text(), forbidden), "No fabricated completion: " + forbidden);
        }
    }

    private static void complete(Result result, boolean won) {
        equal(true, result.finished(), "Fixture completes game");
        equal(won, result.won(), "Controller decides outcome");
        equal(18, result.state().turn(), "All 18 turns played");
        equal(false, result.state().waiterAvailable(), "No Waiter after completion");
        equal(false, result.state().chefAvailable(), "No Chef after completion");
        equal(18, occurrences(result.text(), "End of turn "), "Exactly 18 turn completions");
        equal(won ? 1 : 0, occurrences(result.text(), "Victory"), "Victory printed only when appropriate");
        equal(won ? 0 : 1, occurrences(result.text(), "Defeat"), "Defeat printed only when appropriate");
        for (int turn = 1; turn <= 18; turn++) {
            String marker = "End of turn " + turn + "\n";
            int from = result.text().indexOf(marker);
            if (from < 0) {
                throw new AssertionError("Missing completion summary for turn " + turn);
            }
            List<String> summary = result.text().substring(from + marker.length()).lines().limit(4).toList();
            equal("Turns used: " + turn, summary.get(3), "Completed-turn count for turn " + turn);
        }
        equal(0, occurrences(result.text(), "Turn 19"), "No extra turn");
    }

    private static String turnDisplay(String text, int turn) {
        String start = "Turn " + turn + "\n";
        int from = text.indexOf(start);
        int to = text.indexOf("End of turn " + turn + "\n", from);
        if (from < 0 || to < 0) {
            throw new AssertionError("Missing turn output: " + turn);
        }
        return text.substring(from, to);
    }

    private record Result(GameSnapshot state, boolean finished, boolean won, String text) { }

    private static class Game {
        final RestaurantCLI cli;
        final GameController controller;

        Game(Scanner input, PrintStream output) {
            Restaurant restaurant = new Restaurant();
            TurnManager manager = new TurnManager(new Cashier());
            controller = new GameController(restaurant, manager, new Waiter(), new Chef());
            cli = new RestaurantCLI(controller, input, output);
        }
    }

    private static int occurrences(String text, String part) {
        return (text.length() - text.replace(part, "").length()) / part.length();
    }

    private static void contains(String text, String expected) {
        if (!text.contains(expected)) {
            throw new AssertionError("Missing output: " + expected);
        }
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
