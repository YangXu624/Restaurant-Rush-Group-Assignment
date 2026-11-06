# Restaurant Rush

Turn-based Java CLI restaurant management simulation developed for the **SC2002 Object-Oriented Design & Programming** group assignment.

Repository URL: [https://github.com/YangXu624/Restaurant-Rush-Group-Assignment](https://github.com/YangXu624/Restaurant-Rush-Group-Assignment)

---

## 1. Project Overview

Stage 3 adds a Host, a combo meal and a Happy Hour promotion to the playable Stage 2 game. The earlier games are available at the `stage-1` and `stage-2` tags.

The simulation runs for a fixed sequence of **18 turns**. The player manages restaurant floor operations through a command-line interface (CLI), deciding on Happy Hour and directing a Host, Waiter and Chef each turn while an automated Cashier handles billing. Customer arrivals, menu selection, and order preparation follow a completely deterministic schedule.

### Stage 1 Team Member Responsibilities & Module Ownership

| Member | Module Area | Types & Packages | Core Responsibilities |
| --- | --- | --- | --- |
| **M1: Yang Xu** | Customer & Orders | `Customer`, `Order`, `MenuItem`, `CustomerStatus`, `OrderStatus` (`entity`) | Customer lifecycle, satisfaction decay, food selection, order progression states, and billing calculation. |
| **M2: Kai Xuan** | Restaurant Operations | `Restaurant`, `Table` (`entity`) | Waiting queue, table assignment/release, occupancy, customer abandonment, and restaurant statistics. |
| **M3: Marcus** | Staff Operations | `Waiter`, `Chef`, `Cashier`, `ActionResult` (`staff`, `support`) | Staff action execution and validation, cooking preparation steps, automatic payment processing, and operation feedback. |
| **M4: Nickolas** | Game Logic & Control | `GameController`, `TurnManager` (`control`) | Coordination of turn lifecycle, scheduled customer arrivals, staff phase transitions, and victory/defeat evaluation. |
| **M5: Toshiki** | Boundary & Integration | `RestaurantCLI`, `GameSnapshot`, `CustomerView`, `TableView`, `Main` (`boundary`, `support`) | CLI presentation, user command parsing/validation, immutable view snapshots, integration tests, and fixture-based runs. |

---

## 2. Game Rules & Mechanics (Stage 3)

### Base Configuration
- **Total Turns:** Exactly 18 turns. The game does not terminate early.
- **Tables:** 2 dining tables (IDs `1` and `2`), each seating at most 1 customer.
- **Customer Arrivals:** 1 customer arrives at the start of turns **1, 4, 7, 10, 13, and 16** (Customer IDs `1` through `6`).
- **Menu & Food Selection:**
  - `Salad`: \$9.00 (900¢), requires **1 cooking unit**.
  - `Burger`: \$15.00 (1500¢), requires **1 cooking unit**.
  - `Pasta`: \$18.00 (1800¢), requires **2 cooking units**.
  - `Salad + Pasta` combo: \$24.30 (2430¢, 90% of the combined price), requires **3 cooking units**.
- **Customer Lifecycle & Satisfaction:**
  - Customers begin with **100** satisfaction.
  - While `WAITING` (in queue) or `SEATED` (waiting for food), satisfaction decreases at the rate for that customer type (minimum 0).
  - If satisfaction drops to 0 at the end of a turn, the customer abandons the restaurant (`LEFT`), freeing any table and cancelling their order.
  - Once `SERVED`, satisfaction freezes. The customer eats for 1 full turn (`servedTurn < currentTurn`), then transitions to `READY_TO_PAY`.
- **Cashier Billing:**
  - The Cashier collects payment automatically at the end of each turn from at most **one** eligible `READY_TO_PAY` customer.
  - Customers can only pay from the turn *after* they become ready (`readyToPayTurn < currentTurn`). Even though eating completes before Cashier collection, customers who just became ready that turn are not eligible yet (e.g., served on turn 3 $\to$ finishes eating on turn 4 $\to$ eligible to pay from turn 5).
  - Priority: earliest ready-to-pay turn; ties are broken by lower customer ID.
  - Upon payment, revenue is collected, the table is freed, and the customer transitions to `PAID`.

### Customer types

| Customer | Arrival turns (IDs) | Satisfaction lost per waiting turn | Dish choice | Bill |
| --- | --- | --- | --- | --- |
| Regular | 1, 7, 10, 16 (IDs 1, 3, 4, 6) | 8 | Cycle through the four-item menu using `(id - 1) % menu.size()`, so customer 4 chooses the combo | Full price |
| VIP | 4 (ID 2) | 5 | Same rule as Regular | 10% off, rounded to the nearest cent (half up) |
| Critic | 13 (ID 5) | 12 | Highest-priced item, first in menu order on a tie | Full price |

The Waiter asks each customer to choose a dish and calculate its bill. Java calls the method for that customer's class. The order stores the price at that point, and the Cashier collects it at payment. Without Happy Hour the VIP orders a $13.50 Burger and the Critic orders a $24.30 combo that takes three Chef actions.

### Host

The Host seats at most one queued customer per turn, in addition to the Waiter's task, and may wait instead. The Waiter can still seat customers. A failed Host command keeps the Host's action, so the player can retry.

### Happy Hour

The manager may activate Happy Hour once per game, at the start of a turn. It lasts for that turn and the next, and the CLI displays the remaining duration.

- Orders taken while it is active get 20% off, rounded to the nearest cent (half up). The VIP discount applies first: a VIP Burger costs $15.00 × 0.9 × 0.8 = $10.80.
- The price is locked when the order is taken. Orders already placed keep their price, even if they are paid during Happy Hour.
- During each active turn, customers who are `WAITING` or `SEATED` recover 15 satisfaction, up to 100. Served and departed customers do not recover. The usual waiting penalty still applies at the end of the turn.

### Turn order

Scheduled arrival → manager's Happy Hour decision → recovery if active → Host → Waiter → Chef → eating, payment, waiting penalties and abandonment.

### End-of-Game Evaluation (Turn 18)
Victory requires satisfying **all three** targets simultaneously at the conclusion of turn 18:
1. **Paid Customers:** $\ge 4$ customers paid.
2. **Total Revenue:** $\ge \$45.00$ (4,500 cents).
3. **Average Paid Satisfaction:** $\ge 60.00$ (average satisfaction across paid customers only).

---

## 3. CLI Commands

During each turn, the CLI prompts for these decisions in order:

### Manager Decision
Offered at the start of each turn until Happy Hour has been activated:
- `happy`: Activate Happy Hour for this turn and the next.
- `wait`: Leave Happy Hour off this turn.

### Host Phase
- `seat <customerId> <tableId>`: Seat a customer from the waiting queue at an empty table.
- `wait`: The Host takes no action this turn.

### Waiter Phase
The Waiter acts after the Host. Choose one of the following:
- `seat <customerId> <tableId>`: Seat a customer from the waiting queue at an empty table.
- `order <customerId>`: Take the order of a seated customer (assigns their deterministic dish).
- `serve <customerId>`: Deliver food to a customer whose order status is `READY`.
- `wait`: The Waiter takes no action this turn.

### Chef Phase
The Chef acts after the Waiter's choice has been processed:
- `cook <customerId>`: Spend 1 cooking unit preparing the customer's pending order.
- `wait`: The Chef takes no action this turn.

*Note: Entering an invalid command or attempting an illegal domain action (e.g. seating at an occupied table or cooking without an order) does not consume staff availability. The CLI displays feedback and reprompts until a valid action or `wait` succeeds.*

---

## 4. Build, Run, and Test

### Prerequisites
- Java Development Kit (JDK) 17 or higher (`javac` and `java` on `PATH`).
- POSIX-compatible shell (macOS, Linux, or Windows WSL/Git Bash).

### Compilation & Interactive Execution
```sh
# Compile all source files
./compile.sh

# Run the interactive game
./run.sh
```

### Automated Unit & Integration Tests
The repository includes a comprehensive standalone test suite (23 suites covering all units, contracts, and fixtures) without external build tool dependencies:
```sh
./test.sh
```

### Reproducible Fixture Runs
Automated end-to-end runs using standard input fixtures:
```sh
# Reproducible Victory Run (Turns 1-18 -> Victory: $59.94 revenue, 4 paid, 88.75 avg satisfaction; uses the Host, a combo and Happy Hour)
./run.sh < tests/fixtures/m5/victory.txt

# Reproducible Defeat Run (Turns 1-18 -> Defeat: 0 paid, 1 departure)
./run.sh < tests/fixtures/m5/defeat.txt

# Abandonment handling demonstration
./run.sh < tests/fixtures/m5/abandonment.txt

# Invalid input / command validation demonstration
./run.sh < tests/fixtures/m5/invalid-input.txt
```

Stage 3 results and historical Stage 1 and 2 runs are documented in [docs/test-notes.md](docs/test-notes.md).

---

## 5. Source Layout

```text
src/restaurantrush/
├── Main.java                 # Application entry point & dependency wiring
├── boundary/
│   └── RestaurantCLI.java    # Text interface, prompt loop, snapshot rendering
├── control/
│   ├── GameController.java   # Top-level gameplay facade, action routing, victory evaluator
│   └── TurnManager.java      # Turn lifecycle, arrival clock, staff phase tracking
├── entity/
│   ├── ComboMeal.java        # Menu item built from two other menu items
│   ├── Customer.java         # Lifecycle state, satisfaction tracking
│   ├── VIPCustomer.java      # Lower waiting penalty and discounted bill
│   ├── CriticCustomer.java   # Higher waiting penalty and highest-price dish choice
│   ├── CustomerStatus.java   # WAITING, SEATED, SERVED, READY_TO_PAY, PAID, LEFT
│   ├── HappyHour.java        # One activation, two-turn duration, 20% discount
│   ├── MenuItem.java         # Name, price (cents), cooking time (units)
│   ├── Order.java            # Dish association, preparation progress, status
│   ├── OrderStatus.java      # PLACED, PREPARING, READY, SERVED, PAID, CANCELLED
│   ├── Restaurant.java       # Central aggregate: tables, queue, orders, revenue
│   └── Table.java            # Table ID and seated customer tracking
├── staff/
│   ├── Host.java             # Seating, separate from the Waiter's task
│   ├── Waiter.java           # Seating, taking orders, serving meals
│   ├── Chef.java             # Order cooking and progress advancement
│   └── Cashier.java          # Automatic payment collection and bill settlement
└── support/
    ├── ActionResult.java     # Success/failure outcome carrier with message
    ├── CustomerView.java     # Immutable snapshot view of a customer
    ├── GameSnapshot.java     # Immutable snapshot of complete restaurant state
    └── TableView.java        # Immutable snapshot view of a dining table
```

---

## 6. Stage 1 Design Decisions & Contract Alignments

The team successfully aligned and closed all Stage 1 open items from Section 9 of [docs/class-contracts.md](docs/class-contracts.md):

1. **System Construction & Injection:**
   `Main.java` instantiates a single shared `Restaurant`, `TurnManager`, staff members (`Waiter`, `Chef`, `Cashier`), and connects them through `GameController` and `RestaurantCLI`.
2. **Error Handling & Invariants:**
   Invalid programmatic parameters throw `IllegalArgumentException`; illegal lifecycle state calls throw `IllegalStateException`. Player interaction mistakes return clean `ActionResult.fail(reason)` responses without mutating domain state or consuming turns. (Note: as documented under Section 9 discrepancies in [docs/class-contracts.md](docs/class-contracts.md), certain boundary/support classes like `RestaurantCLI`, `ActionResult`, and `GameSnapshot` use `Objects.requireNonNull` which throws `NullPointerException` for specific null inputs).
3. **View Snapshot Immutability:**
   Domain entities are never leaked directly to the boundary. `GameSnapshot` uses Java 17 records (`CustomerView`, `TableView`) and unmodifiable collections. All arrived customers (active, paid, and departed) are retained in historical snapshots for complete visibility.
4. **Turn Accounting:**
   `GameSnapshot.turn()` reports the active turn number. The CLI cleanly separates start-of-turn prompt headers from end-of-turn summaries to prevent off-by-one discrepancies.

## 7. Changes from Stage 1 to Stage 2

[Compare the Stage 1 tag with the merged Stage 2 gameplay](https://github.com/YangXu624/Restaurant-Rush-Group-Assignment/compare/stage-1...f56f8f5fdb2bb8b2faea68cbb9f7be0027036841). File names below are grouped by their role. No files were deleted.

| New files | Reason |
| --- | --- |
| `VIPCustomer.java`, `CriticCustomer.java` | Give each type its own waiting penalty and override the pricing or dish choice it changes. |
| `CustomerTypesTest.java`, `Stage2IntegrationTest.java` | Check the three customer types individually and through a complete game, including abandonment and payment. |
| `.github/workflows/main.yml` | Run build and test checks on GitHub. This workflow was added between stages, before the gameplay PR. |

| Existing files changed | Reason |
| --- | --- |
| `Customer.java` | Provide regular defaults for the waiting penalty, price and type label. Keep the customer lifecycle shared. |
| `TurnManager.java` | Create a VIP and a Critic in two of the six arrival slots. |
| `Waiter.java`, `Order.java` | Ask the customer for the bill and store it when the order is placed. |
| `CustomerView.java`, `GameSnapshot.java`, `RestaurantCLI.java` | Show the customer type and the locked bill once an order exists. |
| `CustomerTest.java`, `OrderTest.java`, `WaiterTest.java` | Check regular defaults, stored prices and customer-specific order placement. |
| `CustomerViewTest.java`, `GameSnapshotTest.java`, `RestaurantCLITest.java` | Check the additional view fields and their CLI display. |
| `tests/fixtures/m5/victory.txt`, `tests/fixtures/m5/invalid-input.txt` | Allow a second cooking action for the Critic's Pasta. |
| `MainTest.java`, `M5IntegrationTest.java` | Verify the revised fixture outcomes through the launcher and CLI. |
| `README.md`, `docs/test-notes.md` | Document the new rules, file changes and reproducible outcomes for both stages. |

## 8. Changes from Stage 2 to Stage 3

[Compare the Stage 2 and Stage 3 tags](https://github.com/YangXu624/Restaurant-Rush-Group-Assignment/compare/stage-2...stage-3). File names below are grouped by their role. No files were deleted.

| New files | Reason |
| --- | --- |
| `ComboMeal.java`, `ComboMealTest.java` | Add a menu item made of two existing items, with its derived price and preparation units. |
| `Host.java`, `HostIntegrationTest.java` | Add the Host's separate seating action and check it alongside the Waiter and Chef. |
| `HappyHour.java`, `HappyHourTest.java`, `HappyHourTurnTest.java` | Keep the promotion's duration, single activation and discount in one class, then check its timing and pricing during real turns. |
| `tests/fixtures/stage2/baseline-commands.txt` | Keep the command schedule that `Stage2IntegrationTest` checks, now that the main victory fixture uses Stage 3 features. |
| `docs/stage2_class_diagram.drawio` | Record the Stage 2 design. |

| Existing files changed | Reason |
| --- | --- |
| `Restaurant.java` | Register the combo as the fourth menu item and own the game's single `HappyHour`. |
| `Customer.java` | Recover satisfaction while waiting during Happy Hour. |
| `Waiter.java` | Apply Happy Hour after the customer calculates their bill, before the order locks the price. |
| `TurnManager.java` | Track the Host's action, activate Happy Hour before the Host acts, apply recovery once per active turn and end its duration with the turn. |
| `GameController.java`, `Main.java` | Share the Host and expose the Host and Happy Hour operations to the CLI. |
| `GameSnapshot.java`, `RestaurantCLI.java` | Add Manager and Host prompts, and display Host availability and the remaining Happy Hour duration. |
| `RestaurantCLITest.java`, `RestaurantCLIRunTest.java`, `GameSnapshotTest.java`, `CustomerTest.java`, `TurnManagerTest.java`, `GameControllerTest.java`, `WaiterTest.java` | Check the new behaviour and account for the Host's turn and the fourth menu item. |
| `tests/fixtures/m5/*.txt` | Answer the new prompts. The victory run now uses the Host, a combo and Happy Hour, so its schedule changed. |
| `MainTest.java`, `M5IntegrationTest.java`, `Stage2IntegrationTest.java` | Verify the revised fixture outcomes through the launcher and CLI. |
| `README.md`, `docs/class-contracts.md`, `docs/test-notes.md` | Document the new rules, contracts, file changes and reproducible outcomes. |
