# Restaurant Rush

Turn-based Java CLI restaurant management simulation developed for the **SC2002 Object-Oriented Design & Programming** group assignment.

Repository URL: [https://github.com/YangXu624/Restaurant-Rush-Group-Assignment](https://github.com/YangXu624/Restaurant-Rush-Group-Assignment)

---

## 1. Project Overview & Stage 1 Status

**Stage 1 is fully implemented, verified, and playable.**

The simulation runs for a fixed sequence of **18 turns**. The player manages restaurant floor operations through a command-line interface (CLI), directing a Waiter and Chef each turn while an automated Cashier handles billing. Customer arrivals, menu selection, and order preparation follow a completely deterministic schedule.

### Team Member Responsibilities & Module Ownership

| Member | Module Area | Types & Packages | Core Responsibilities |
| --- | --- | --- | --- |
| **M1: Yang Xu** | Customer & Orders | `Customer`, `Order`, `MenuItem`, `CustomerStatus`, `OrderStatus` (`entity`) | Customer lifecycle, satisfaction decay, food selection, order progression states, and billing calculation. |
| **M2: Kai Xuan** | Restaurant Operations | `Restaurant`, `Table` (`entity`) | Waiting queue, table assignment/release, occupancy, customer abandonment, and restaurant statistics. |
| **M3: Marcus** | Staff Operations | `Waiter`, `Chef`, `Cashier`, `ActionResult` (`staff`, `support`) | Staff action execution and validation, cooking preparation steps, automatic payment processing, and operation feedback. |
| **M4: Nickolas** | Game Logic & Control | `GameController`, `TurnManager` (`control`) | Coordination of turn lifecycle, scheduled customer arrivals, staff phase transitions, and victory/defeat evaluation. |
| **M5: Toshiki** | Boundary & Integration | `RestaurantCLI`, `GameSnapshot`, `CustomerView`, `TableView`, `Main` (`boundary`, `support`) | CLI presentation, user command parsing/validation, immutable view snapshots, integration tests, and fixture-based runs. |

---

## 2. Game Rules & Mechanics (Stage 1)

### Base Configuration
- **Total Turns:** Exactly 18 turns. The game does not terminate early.
- **Tables:** 2 dining tables (IDs `1` and `2`), each seating at most 1 customer.
- **Customer Arrivals:** 1 customer arrives at the start of turns **1, 4, 7, 10, 13, and 16** (Customer IDs `1` through `6`).
- **Menu & Food Selection:**
  - `Salad`: \$9.00 (900¢), requires **1 cooking unit**.
  - `Burger`: \$15.00 (1500¢), requires **1 cooking unit**.
  - `Pasta`: \$18.00 (1800¢), requires **2 cooking units**.
  - Dish assigned deterministically upon ordering: `menu.get((customerId - 1) % 3)`.
- **Customer Lifecycle & Satisfaction:**
  - Customers begin with **100** satisfaction.
  - While `WAITING` (in queue) or `SEATED` (waiting for food), satisfaction decreases by **8 points per turn** (clamped at 0).
  - If satisfaction drops to 0 at the end of a turn, the customer abandons the restaurant (`LEFT`), freeing any table and cancelling their order.
  - Once `SERVED`, satisfaction freezes. The customer eats for 1 full turn, then transitions to `READY_TO_PAY`.
- **Cashier Billing:**
  - The Cashier collects payment automatically at the end of each turn from at most **one** `READY_TO_PAY` customer.
  - Priority: earliest ready-to-pay turn; ties are broken by lower customer ID.
  - Upon payment, revenue is collected, the table is freed, and the customer transitions to `PAID`.

### End-of-Game Evaluation (Turn 18)
Victory requires satisfying **all three** targets simultaneously at the conclusion of turn 18:
1. **Paid Customers:** $\ge 4$ customers paid.
2. **Total Revenue:** $\ge \$45.00$ (4,500 cents).
3. **Average Paid Satisfaction:** $\ge 60.00$ (average satisfaction across paid customers only).

---

## 3. CLI Commands

During each turn, the CLI prompts for two distinct staff decisions:

### Waiter Phase
The Waiter acts first. Choose one of the following:
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
The repository includes a comprehensive standalone test suite (17 suites covering all units, contracts, and fixtures) without external build tool dependencies:
```sh
./test.sh
```

### Reproducible Fixture Runs
Automated end-to-end runs using standard input fixtures:
```sh
# Reproducible Victory Run (Turns 1-18 -> Victory: $66.00 revenue, 5 paid, 79.20 avg satisfaction)
./run.sh < tests/fixtures/m5/victory.txt

# Reproducible Defeat Run (Turns 1-18 -> Defeat: 0 paid, 2 departures)
./run.sh < tests/fixtures/m5/defeat.txt

# Abandonment handling demonstration
./run.sh < tests/fixtures/m5/abandonment.txt

# Invalid input / command validation demonstration
./run.sh < tests/fixtures/m5/invalid-input.txt
```

Reproducible Stage 1 victory and defeat scenarios, including commands, test fixtures, expected results, and verified outcomes, are documented in [docs/test-notes.md](docs/test-notes.md).

---

## 5. Source Layout

```text
src/restaurantrush/
├── Main.java                 # Application entry point & dependency wiring
├── boundary/
│   └── RestaurantCLI.java    # Text interface, prompt loop, snapshot rendering
├── control/
│   ├── GameController.java   # Top-level gameplay facade & action routing
│   └── TurnManager.java      # Turn lifecycle, arrival clock, victory evaluator
├── entity/
│   ├── Customer.java         # Lifecycle state, satisfaction tracking
│   ├── CustomerStatus.java   # WAITING, SEATED, SERVED, READY_TO_PAY, PAID, LEFT
│   ├── MenuItem.java         # Name, price (cents), cooking time (units)
│   ├── Order.java            # Dish association, preparation progress, status
│   ├── OrderStatus.java      # ORDERED, PREPARING, READY, SERVED, PAID, CANCELLED
│   ├── Restaurant.java       # Central aggregate: tables, queue, orders, revenue
│   └── Table.java            # Table ID and seated customer tracking
├── staff/
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
   Invalid programmatic parameters throw `IllegalArgumentException`; illegal lifecycle state calls throw `IllegalStateException`. Player interaction mistakes return clean `ActionResult.fail(reason)` responses without mutating domain state or consuming turns.
3. **View Snapshot Immutability:**
   Domain entities are never leaked directly to the boundary. `GameSnapshot` uses Java 17 records (`CustomerView`, `TableView`) and unmodifiable collections. All arrived customers (active, paid, and departed) are retained in historical snapshots for complete visibility.
4. **Turn Accounting:**
   `GameSnapshot.turn()` reports the active turn number. The CLI cleanly separates start-of-turn prompt headers from end-of-turn summaries to prevent off-by-one discrepancies.
