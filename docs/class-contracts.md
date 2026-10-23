# Class contracts

This document records the team's **Stage 1** design from `18oct_meeting.pdf` (18 October meeting, pages 1–18), checked against `SC2002_Restaurant_Rush_Group_Assignment.docx`. It defines the responsibilities and shared interfaces that members can implement against. It includes the decisions confirmed during subsequent group meetings and remains the main implementation reference.

The meeting's class names and ownership are preserved; agreed wiring and display details are included below. Java signatures below use normal constructor syntax (no return type). Details inferred to make the contracts consistent are identified under [Clarifications and open decisions](#9-clarifications-and-open-decisions). Stage 2 and Stage 3 classes are outside this meeting design and must not be added to the Stage 1 runnable version.

## 1. Ownership and responsibilities

| Owner | Area | Types | Responsibility |
| --- | --- | --- | --- |
| M1: Yang Xu | Customer and orders | `Customer`, `Order`, `MenuItem`, `CustomerStatus`, `OrderStatus` | Customer lifecycle, satisfaction, food selection, preparation states and billing |
| M2: Kai Xuan | Restaurant operations | `Restaurant`, `Table` | Queue, seating, occupancy, abandonment, shared state and statistics |
| M3: Marcus | Staff operations | `Waiter`, `Chef`, `Cashier`, `ActionResult` | Staff actions, action validation, preparation and payment |
| M4: Nickolas | Game logic | `GameController`, `TurnManager` | Coordination, turn sequence, arrivals, timing and victory conditions |
| M5: Toshiki | Interface and integration | `RestaurantCLI`, `GameSnapshot`, `CustomerView`, `TableView` | Commands, displays, integration testing and reproducible runs |

## 2. Shared conventions and invariants

- Target Java 17. `Main.java` initializes and connects the game components using shared instances, then launches the CLI.
- All entity and control attributes are private. Public operations are listed below. Record components expose their standard Java accessors, such as `snapshot.turn()`.
- Money is stored as integer cents and displayed as dollars with two decimal places. Stage 1 bills are locked to the selected menu item's price when the order is created.
- Player-requested actions return `ActionResult`. Failure explains the problem, leaves game state unchanged and does not consume the staff choice. An explicit `wait` succeeds and consumes that choice.
- Use `IllegalArgumentException` for invalid parameters, `IllegalStateException` for invalid lifecycle operations, and `ActionResult.fail(...)` for invalid player actions. Existing boolean-returning operations retain their documented failure results. Current implementation discrepancies are noted in Section 9.
- No domain, staff or control object reads input or prints output. `RestaurantCLI` handles interaction; `ActionResult` carries feedback and `GameSnapshot` carries display state.
- A customer is either queued or seated, never both. A table holds at most one customer, and a customer occupies at most one table. A table remains occupied through eating and waiting for payment.
- Each customer has at most one order. Its ID equals the customer's ID. The customer's order reference and the restaurant's order registry refer to the same object.
- Paid and departed customers and their orders remain in the historical lists. `PAID` and `LEFT` customers are excluded from active customers and the waiting queue, but included in immutable display views. Creating views must not modify the underlying entities.
- Satisfaction stays in `[0, 100]`. Only `WAITING` and `SEATED` customers lose satisfaction, at 8 points per turn in Stage 1, clamped to zero.
- Revenue includes only successful payments, once each. Served count, abandoned count, paid count and paid satisfaction total are updated once per corresponding event.
- Collection getters return read-only copies. These copies still contain domain entities; the CLI must receive immutable view records instead of those mutable references.
- Staff availability and action order belong to `TurnManager`. Order preparation validity belongs to `Order`. Cross-object seating, serving, payment and abandonment belong to `Restaurant`.

### Base configuration

| Setting | Contract |
| --- | --- |
| Turns | Exactly 18; no early victory or defeat |
| Tables | Two, IDs `1` and `2` |
| Arrivals | One customer at the start of turns `1, 4, 7, 10, 13, 16`; IDs `1` through `6` in arrival order |
| Staff | One Waiter choice, then one Chef choice per turn; at most one automatic Cashier payment |
| Menu order | `Salad` (900 cents, 1 unit), `Burger` (1500 cents, 1 unit), `Pasta` (1800 cents, 2 units) |
| Food selection | `menu.get((id - 1) % menu.size())`, when ordering; the manager never chooses the dish |
| Victory | Game finished AND paid count ≥ 4 AND revenue ≥ 4500 cents AND average paid satisfaction ≥ 60 |

## 3. Boundary contract

### RestaurantCLI

**Owner:** M5 · **Kind:** Boundary

Reads commands, invokes `GameController`, and displays returned state and feedback. It does not implement game rules or mutate domain objects.

| Private attribute | Purpose |
| --- | --- |
| `GameController controller` | The controller for player actions and state access |
| `Scanner input` | Command input |
| `PrintStream output` | Destination for CLI output |

| Public operation | Contract |
| --- | --- |
| `RestaurantCLI(GameController controller, Scanner input, PrintStream output)` | Store the shared controller and supplied streams; the caller owns the streams. |
| `void run()` | Start each turn through the controller, then display its snapshot. Read and retry Waiter commands until one succeeds, then read and retry Chef commands until one succeeds. After `finishTurn()` succeeds, display results using the saved turn number as the last completed turn. Repeat until the game finishes; stop cleanly if input ends. |
| `void displayState(GameSnapshot snapshot)` | Show turn, revenue, average paid satisfaction, waiting queue, tables, customer satisfaction and status, order state and progress, and staff availability. Show `N/A` when average paid satisfaction is `null`. |
| `String readWaiterChoice()` | Read `seat <customerId> <tableId>`, `order <customerId>`, `serve <customerId>`, or `wait`. |
| `String readChefChoice()` | Read `cook <customerId>` or `wait`. |
| `void showFeedback(String message)` | Print an action's success or failure message. |
| `void displayOutcome(boolean won)` | After turn 18 completes, display Victory or Defeat. Obtain the final snapshot from the controller and show served count, unhappy departures, paid count, revenue, average paid satisfaction and turns used. |

Malformed commands and non-integer IDs must be explained and retried without consuming a choice. End of input must not fabricate completed turns or a final outcome. The current CLI retains the start-of-turn snapshot in a local variable and uses `start.turn()` after successful completion; it does not need a separate counter field. Do not use the advanced snapshot turn as the completed-turn count. At final completion, the snapshot remains at turn 18.

## 4. Control contracts

### Main

**Kind:** Application entry point

`public static void main(String[] args)` creates one `Restaurant`, `Cashier`, `TurnManager`, `Waiter`, `Chef` and `GameController`. Pass the same Cashier to TurnManager, and the shared Restaurant, TurnManager, Waiter and Chef to GameController. Construct RestaurantCLI with that controller, a Scanner over standard input and `System.out`, then call `run()`. Main performs wiring, not game rules.

### GameController

**Owner:** M4 · **Kind:** Control

Bridges the CLI and game objects. Checks assignment availability, delegates actions and records successful choices. It does not duplicate turn-resolution rules or flags.

| Private attribute | Purpose |
| --- | --- |
| `Restaurant restaurant` | Shared game state |
| `TurnManager turnManager` | Turn lifecycle and staff availability |
| `Waiter waiter` | Seating, ordering and serving |
| `Chef chef` | Preparation |

| Public operation | Preconditions and effects |
| --- | --- |
| `GameController(Restaurant restaurant, TurnManager turnManager, Waiter waiter, Chef chef)` | Store the supplied collaborators; use these same instances throughout the game. |
| `void startTurn()` | Delegate to `turnManager.beginTurn(restaurant)` before the CLI displays the turn. |
| `ActionResult seatCustomer(int customerId, int tableId)` | Require `canAssignWaiter()`, then delegate to `waiter.seat(restaurant, customerId, tableId)`. Record the Waiter choice only on success. |
| `ActionResult takeOrder(int customerId)` | Require `canAssignWaiter()`, then delegate to `waiter.takeOrder(restaurant, customerId)`. Record the Waiter choice only on success. |
| `ActionResult serveOrder(int customerId)` | Require `canAssignWaiter()`, then delegate to `waiter.serve(restaurant, customerId, currentTurn)`. Record the Waiter choice only on success. |
| `ActionResult prepareOrder(int customerId)` | Require `canAssignChef()`. Find the customer's order; fail if it does not exist. Delegate to `chef.prepareOneUnit(order)` and record the Chef choice only on success. |
| `ActionResult waitWaiter()` | If the Waiter choice is available, record it and return success without performing a task; otherwise return failure. |
| `ActionResult waitChef()` | If the Chef choice is available, record it and return success without performing a task; otherwise return failure. |
| `void finishTurn()` | After both choices, call `turnManager.resolveTurn(restaurant)` followed by `turnManager.advanceTurn()`. Do not maintain another set of resolution flags. |
| `GameSnapshot getSnapshot()` | Return `GameSnapshot.from(restaurant, turnManager)`. |
| `boolean isVictory()` | Return true only after `turnManager.isFinished()` and all three base-setting victory targets are met. Return false during play. |
| `boolean isFinished()` | Return `turnManager.isFinished()`. |

An unavailable assignment returns failure without invoking staff. The controller always obtains the turn from `TurnManager`, not from user input. All manager actions pass through this controller so the Waiter-before-Chef rule cannot be bypassed by the CLI.

### TurnManager

**Owner:** M4 · **Kind:** Control

Owns the turn lifecycle, scheduled arrivals, staff choice limits and automatic event timing.

| Private attribute | Initial value and meaning |
| --- | --- |
| `int currentTurn` | `1`; remains in the range `1..18` |
| `boolean waiterTaskUsed` | `false`; successful action or explicit Waiter wait recorded |
| `boolean chefTaskUsed` | `false`; successful action or explicit Chef wait recorded |
| `boolean turnStarted` | `false`; prevents duplicate starts |
| `boolean turnResolved` | `false`; prevents repeated automatic events |
| `boolean finished` | `false`; true only after the final turn is resolved and closed |
| `Cashier cashier` | Automatic payment collaborator |

| Public operation | Preconditions and effects |
| --- | --- |
| `TurnManager(Cashier cashier)` | Store the shared Cashier; reject null with `IllegalArgumentException`. |
| `void beginTurn(Restaurant r)` | Require a not-yet-started turn and an unfinished game. Mark the turn started, reset staff and resolution flags, and add its scheduled arrival if any. Reject a duplicate start or a start after completion. |
| `boolean canAssignWaiter()` | True iff the turn is started, unresolved and unfinished, and the Waiter choice is unused. |
| `boolean canAssignChef()` | True iff the turn is started, unresolved and unfinished, the Waiter choice is used, and the Chef choice is unused. |
| `void recordWaiterTask()` | Require `canAssignWaiter()`; mark the Waiter choice used. Reject repeated or out-of-phase calls. |
| `void recordChefTask()` | Require `canAssignChef()`; mark the Chef choice used. Reject repeated or out-of-phase calls. |
| `void resolveTurn(Restaurant r)` | Require an active unresolved turn with both choices complete; otherwise throw `IllegalStateException`. Process the automatic events below once, then set `turnResolved = true`. |
| `void advanceTurn()` | Require a started, resolved, unfinished turn. Close it and clear `turnStarted`. If below 18, increment the turn; otherwise set `finished = true` and keep the turn at 18. Reject repeated or invalid calls. |
| `boolean isFinished()` | Return `finished`; false while turn 18 is still being played. |
| `int getCurrentTurn()` | Return the current turn. |

Automatic events run in this order:

1. Call `finishEating(currentTurn)` for eligible customers. Only customers served on an earlier turn become `READY_TO_PAY`; record the current turn as their ready-to-pay turn.
2. Call `cashier.collectOne(r, currentTurn)`. Only a customer whose ready-to-pay turn is earlier than the current turn can pay.
3. Apply the waiting penalty to active customers. Only `WAITING` and `SEATED` customers lose satisfaction.
4. Ask `Restaurant` to remove unserved customers whose satisfaction reached zero.

The Waiter acts before the Chef. Therefore an order taken this turn can be prepared this turn, but an order completed by the Chef cannot be served until a later turn. No extra preparation or serving phase repeats the already-executed staff action.

## 5. Entity contracts

### Restaurant

**Owner:** M2 · **Kind:** Entity

Owns the restaurant's collections and totals. Coordinates operations that change multiple objects so validation finishes before mutation begins.

| Private attribute | Contract |
| --- | --- |
| `List<Customer> waitingQueue` | Unseated customers in arrival order |
| `List<Customer> customers` | Every arrived customer, including paid and departed customers |
| `List<Table> tables` | Two tables, IDs 1 and 2 |
| `List<MenuItem> menu` | Salad, Burger, Pasta in that order; selection depends on this ordering |
| `List<Order> orders` | All placed orders, including paid and cancelled orders |
| `int revenueCents` | Sum of paid bills only, initially 0 |
| `int servedCount` | Successful service count, initially 0 |
| `int abandonedCount` | Departures at zero satisfaction, initially 0 |
| `int paidSatisfactionTotal` | Sum of satisfaction recorded at each payment, initially 0 |
| `int paidCount` | Successful payment count, initially 0 |

| Public operation | Preconditions and effects |
| --- | --- |
| `Restaurant()` | Create two free tables, the base menu, empty customer/queue/order lists and zero totals. |
| `void addArrival(Customer c)` | Add a new arrival to both the customer history and the back of the queue. Reject a duplicate ID. |
| `ActionResult seat(int cId, int tableId)` | Require an existing queued customer and an existing free table. Remove the customer from the queue, assign the table and transition the customer to `SEATED`. Invalid requests change nothing. |
| `ActionResult serve(int customerId, int turn)` | Require an existing seated customer occupying a table, their registered `READY` order, and no prior service. Validate first, then call `Order.markServed()`, `Customer.markServed(turn)`, and increment `servedCount` once. Return feedback; failure changes nothing. The full signature completes the shorthand used on meeting page 8. |
| `Customer findCustomer(int id)` | Return the arrived customer with that ID, or `null`. |
| `Order findOrder(int id)` | Return the order with that ID, or `null` if none was placed. |
| `void registerOrder(Order order)` | Add the order to the registry; reject a duplicate ID. Coordinate with `Waiter.takeOrder()` so customer and registry share one order instance. |
| `boolean recordPayment(Customer c, int turn)` | Require a restaurant customer occupying a table, status `READY_TO_PAY`, `readyToPayTurn < turn`, and their order in `SERVED`. Validate first; mark the order `PAID`, then the customer `PAID`, release the table, add the locked bill and recorded satisfaction, and increment paid count. Return true on success; otherwise false with no changes. |
| `void removeAbandoned(Customer c)` | For an active unserved customer at zero satisfaction: remove them from the queue or release their table, call `c.abandon()`, and increment abandoned count once. Preserve the historical customer and order records. |
| `Customer oldestEligiblePayer(int turn)` | Among `READY_TO_PAY` customers with `readyToPayTurn < turn`, return the earliest ready-to-pay turn, breaking ties by lowest customer ID. Return `null` if none qualify. |
| `double averagePaidSatisfaction()` | Return floating-point `paidSatisfactionTotal / paidCount`, or `0.0` if paid count is zero. The snapshot represents the latter case as `null`. |
| `List<Customer> getActiveCustomers()` | Return customers in `WAITING`, `SEATED`, `SERVED` or `READY_TO_PAY`; exclude `PAID` and `LEFT`. |
| `List<Customer> getWaitingQueue()` | Return a read-only copy in arrival order. |
| `List<Customer> getCustomers()` | Return a read-only copy of all arrived customers. |
| `List<Table> getTables()` | Return a read-only copy in table ID order. |
| `List<MenuItem> getMenu()` | Return a read-only copy in the fixed menu order. |
| `List<Order> getOrders()` | Return a read-only copy of all orders. |
| `int getRevenueCents()` | Return paid revenue in cents. |
| `int getPaidCount()` | Return successful payment count. |
| `int getServedCount()` | Return successful service count. |
| `int getAbandonedCount()` | Return unhappy departure count. |
| `int getPaidSatisfactionTotal()` | Return the sum of satisfaction recorded at payment. |

`recordPayment()` validates and performs one payment; `Cashier` enforces the per-turn payment limit. `serve()` coordinates service; `TurnManager` and `GameController` enforce when the Waiter can act. These responsibilities must not be duplicated in the CLI.

### Customer

**Owner:** M1 · **Kind:** Entity

Owns one customer's identity, satisfaction, lifecycle and optional order. Stage 1 customers are Regular customers represented by this class.

| Private attribute | Initial value and invariant |
| --- | --- |
| `int id` | Fixed positive ID; base arrivals use 1 through 6 |
| `int satisfaction` | 100, always within 0 through 100 |
| `CustomerStatus status` | `WAITING` |
| `int servedTurn` | `-1` until served; retains the service turn afterwards |
| `int readyToPayTurn` | `-1` until eating finishes; retains that turn afterwards |
| `Order order` | `null` until ordering; at most one order |

| Public operation | Preconditions and effects |
| --- | --- |
| `Customer(int id)` | Reject nonpositive IDs with `IllegalArgumentException`. Create a waiting customer with satisfaction 100, no order and both timing fields at -1. The constructor is not restricted to IDs 1–6. |
| `MenuItem chooseItem(List<MenuItem> menu)` | Given a nonempty menu, return `menu.get((id - 1) % menu.size())`. Called when taking the order; does not let the manager select food. |
| `void seat()` | Require `WAITING`; transition to `SEATED`. Called as part of `Restaurant.seat()`. |
| `void acceptOrder(Order order)` | Require `SEATED` and no existing order. Link the customer's new order; remain `SEATED`. The order ID must match the customer ID. |
| `void markServed(int turn)` | Require `SEATED`, no previous service and an associated order already marked `SERVED`. Transition to `SERVED` and record `servedTurn`. Do not update totals or release a table. |
| `void finishEating(int turn)` | If status is `SERVED` and `servedTurn < turn`, transition to `READY_TO_PAY` and set `readyToPayTurn = turn`. Otherwise do nothing; do not reset an existing ready-to-pay timestamp. |
| `void applyWaitingPenalty()` | If `WAITING` or `SEATED`, subtract 8 with a floor of zero. Other states are unchanged. |
| `void markPaid()` | Require `READY_TO_PAY` and an associated order already `PAID`; transition to `PAID`. Restaurant coordinates table release and statistics. |
| `void abandon()` | Transition `WAITING` or `SEATED` to `LEFT` and cancel any unserved order. Restaurant invokes this for a customer at zero satisfaction and handles queue/table cleanup. |
| `int getId()` | Return the fixed ID. |
| `int getSatisfaction()` | Return current satisfaction. |
| `CustomerStatus getStatus()` | Return current lifecycle state. |
| `Order getOrder()` | Return the order, or `null` before ordering. |
| `int getServedTurn()` | Return the service turn, or -1 before service. |
| `int getReadyToPayTurn()` | Return the eating-completion turn, or -1 before eating completes. |

### Table

**Owner:** M2 · **Kind:** Entity

Owns a fixed table ID and at most one occupant. Restaurant coordinates changes to occupancy with customer and queue state.

| Private attribute | Contract |
| --- | --- |
| `int id` | Positive, fixed table ID; Restaurant ensures uniqueness |
| `Customer occupant` | `null` when free |

| Public operation | Preconditions and effects |
| --- | --- |
| `Table(int id)` | Reject nonpositive IDs with `IllegalArgumentException`; create a free table. Restaurant supplies the base IDs 1 and 2 and ensures uniqueness. |
| `boolean isAvailable()` | Return whether occupant is `null`. |
| `void assign(Customer c)` | Require a free table and a customer. Set the occupant; reject an occupied table. Restaurant coordinates the customer's `seat()` transition and queue removal. |
| `void release()` | Clear an occupied table. Only Restaurant calls this during valid payment or abandonment. Throw `IllegalStateException` if already empty. |
| `int getId()` | Return the fixed ID. |
| `Customer getOccupant()` | Return the occupant, or `null`. |

### Order

**Owner:** M1 · **Kind:** Entity

Owns the selected item, preparation progress, locked bill and food lifecycle. Preparation-state rules live here, not in Chef or the controller.

| Private attribute | Contract |
| --- | --- |
| `int id` | Fixed; equals the owning customer's ID |
| `MenuItem item` | Chosen dish, fixed on creation |
| `OrderStatus status` | Initially `PLACED` |
| `int remainingUnits` | Initially the item's preparation units; never negative |
| `int billCents` | Initially the item's price; locked for the lifetime of the order |

| Public operation | Preconditions and effects |
| --- | --- |
| `Order(int id, MenuItem item)` | Create a `PLACED` order with the item's units and price. Reject invalid IDs, null items and nonpositive preparation units. |
| `boolean canPrepare()` | True only for `PLACED` or `PREPARING` with positive remaining units. No mutation. |
| `void prepareOneUnit()` | Call `canPrepare()` and throw `IllegalStateException` if false. Otherwise subtract exactly one unit; become `READY` at zero, else `PREPARING`. |
| `void markServed()` | Require `READY`; transition to `SERVED`. |
| `void markPaid()` | Require `SERVED`; transition to `PAID`. Customer payment timing is validated by Restaurant. |
| `void cancel()` | Require `PLACED`, `PREPARING` or `READY`; transition to `CANCELLED`. |
| `boolean isReady()` | Return whether status is exactly `READY`. |
| `int getBillCents()` | Return the locked bill. |
| `int getId()` | Return the ID. |
| `OrderStatus getStatus()` | Return the status. |
| `MenuItem getItem()` | Return the selected immutable item. |
| `int getRemainingUnits()` | Return remaining preparation units. |

Zero remaining units means preparation is complete, not necessarily that status is still `READY`: a served, paid or cancelled order can also have zero units. Use `isReady()` for service eligibility. Changing order status alone does not update customer status.

### MenuItem

**Owner:** M1 · **Kind:** Immutable entity

| Private attribute | Contract |
| --- | --- |
| `String name` | Nonblank, immutable display name |
| `int priceCents` | Nonnegative, immutable price in cents |
| `int preparationUnits` | Positive, immutable number of Chef actions required |

| Public operation | Contract |
| --- | --- |
| `MenuItem(String name, int priceCents, int preparationUnits)` | Reject blank names, negative prices and nonpositive preparation units. |
| `int getPriceCents()` | Return price in cents. |
| `int getPreparationUnits()` | Return required preparation units. |
| `String getName()` | Return the display name used in order feedback and state displays. |

There are no setters. Restaurant creates the three base menu items in the required order.

### Waiter

**Owner:** M3 · **Kind:** Stateless entity

Performs seating, ordering or serving. It has no persistent attributes or independent staff-availability flags.

| Public operation | Preconditions and effects |
| --- | --- |
| `ActionResult seat(Restaurant r, int cId, int tableId)` | Delegate to `r.seat(cId, tableId)` and return its result. |
| `ActionResult takeOrder(Restaurant r, int customerId)` | Require an existing `SEATED` customer with no order. Ask `customer.chooseItem(r.getMenu())`, create the order using the customer's ID, link it through `acceptOrder()` and register it with Restaurant. Return success naming the dish. Invalid requests change nothing. |
| `ActionResult serve(Restaurant r, int customerId, int turn)` | Delegate to `r.serve(customerId, turn)` and return its result. Do not duplicate its checks, mutate Customer or Order directly, or update served count. |

Order creation must preserve the shared-order invariant. Check the customer and registry before linking/registering so a rejected request cannot leave a partially registered order. The meeting assigns this orchestration to Waiter; a future redesign must be agreed rather than silently moved to another class.

### Chef

**Owner:** M3 · **Kind:** Stateless entity

| Public operation | Preconditions and effects |
| --- | --- |
| `ActionResult prepareOneUnit(Order o)` | Fail without mutation if `o` is null or `o.canPrepare()` is false. Otherwise call `o.prepareOneUnit()` exactly once and return success, mentioning if the order became `READY`. |

Chef does not duplicate checks for particular `OrderStatus` values. `TurnManager` and `GameController` enforce the one-choice-per-turn limit.

### Cashier

**Owner:** M3 · **Kind:** Entity

| Private attribute | Contract |
| --- | --- |
| `int lastPaidTurn` | Initially -1; updated only after successful collection |

| Public operation | Preconditions and effects |
| --- | --- |
| `boolean collectOne(Restaurant r, int turn)` | If already paid someone this turn, return false. Otherwise get `r.oldestEligiblePayer(turn)`. If none exists, return false. Call `r.recordPayment(customer, turn)`; on success record `lastPaidTurn = turn` and return true. If payment fails, return false without recording a payment turn. |

Cashier is automatic, invoked by TurnManager. Customers becoming `READY_TO_PAY` during the current resolution are not eligible until a later turn.

## 6. Enumerations

### CustomerStatus

**Owner:** M1

| Value | Meaning |
| --- | --- |
| `WAITING` | Queued for a table; loses satisfaction |
| `SEATED` | At a table but not served; may have no order or a `PLACED`, `PREPARING` or `READY` order; loses satisfaction |
| `SERVED` | Eating; no satisfaction loss |
| `READY_TO_PAY` | Finished eating, still occupying the table; no satisfaction loss |
| `PAID` | Payment recorded and table released |
| `LEFT` | Left at zero satisfaction before service; any order cancelled and queue/table membership cleared |

Normal flow: `WAITING → SEATED → SERVED → READY_TO_PAY → PAID`.

Abandonment flow: `WAITING → LEFT` or `SEATED → LEFT`. `PAID` and `LEFT` are terminal states.

### OrderStatus

**Owner:** M1

| Value | Meaning |
| --- | --- |
| `PLACED` | Taken but not yet prepared |
| `PREPARING` | At least one preparation unit completed; some remain |
| `READY` | All preparation complete, awaiting Waiter service |
| `SERVED` | Served; the associated customer is eating or waiting to pay |
| `PAID` | Payment recorded after the eating and payment delays |
| `CANCELLED` | Customer left before service; cannot be cooked, served or charged |

Normal flow: `PLACED → PREPARING → READY → SERVED → PAID`. A one-unit item goes directly from `PLACED` to `READY`.

Cancellation is permitted from `PLACED`, `PREPARING` or `READY`. `PAID` and `CANCELLED` are terminal states. Restaurant or the relevant staff operation coordinates customer and order transitions; the enums do not synchronize objects themselves.

## 7. Result and display contracts

### ActionResult

**Owner:** M3 · **Kind:** Helper value

| Private attribute | Meaning |
| --- | --- |
| `boolean success` | Whether the action or explicit wait was accepted |
| `String message` | Short explanatory feedback |

| Public operation | Contract |
| --- | --- |
| `static ActionResult ok(String message)` | Create a successful result. |
| `static ActionResult fail(String message)` | Create a failed result. |
| `boolean isSuccess()` | Return the success flag. |
| `String getMessage()` | Return feedback for the CLI. |

The result carries no printing logic and exposes no mutators.

### GameSnapshot

**Owner:** M5 · **Kind:** Immutable record

| Component | Meaning |
| --- | --- |
| `int turn` | Displayed turn, 1 through 18 |
| `int revenueCents` | Paid revenue |
| `Double averagePaidSatisfaction` | Recorded paid-customer average, or `null` if nobody paid |
| `List<Integer> waitingCustomerIds` | Waiting customer IDs in queue order |
| `List<CustomerView> customers` | All arrived customers in arrival order, including `PAID` and `LEFT`, with satisfaction and order progress |
| `List<TableView> tables` | Both tables and their occupants |
| `boolean waiterAvailable` | Whether a Waiter choice can be accepted now |
| `boolean chefAvailable` | Whether a Chef choice can be accepted now |
| `int paidCount` | Successful payments |
| `int servedCount` | Successfully served dishes |
| `int abandonedCount` | Unhappy departures |

**Factory:** `static GameSnapshot from(Restaurant r, TurnManager t)` reads state through getters, creates `CustomerView` and `TableView` records, and stores immutable copies of all lists. It stores no mutable entity references and does not modify the entities it reads. Later game changes must not alter an existing snapshot. Record construction must also preserve list immutability.

Availability is phase-specific: at the beginning of a turn the Chef is not yet assignable because the Waiter must choose first. The CLI should explain that sequence rather than describing the Chef's choice as already used.

### CustomerView

**Owner:** M5 · **Kind:** Immutable record

| Component | Meaning |
| --- | --- |
| `int id` | Customer ID |
| `int satisfaction` | Current satisfaction, 0 through 100 |
| `CustomerStatus status` | Customer lifecycle state |
| `OrderStatus orderStatus` | Order state, or `null` before ordering |
| `String itemName` | Chosen dish, or `null` before ordering |
| `Integer remainingUnits` | Units left, or `null` before ordering |
| `Integer tableId` | Current table ID, or `null` when not occupying a table |

This is display data, not a second Customer entity. Nullable order fields are absent together before ordering. Paid and departed customers have no table ID; historical order information can remain present.

### TableView

**Owner:** M5 · **Kind:** Immutable record

| Component | Meaning |
| --- | --- |
| `int id` | Table ID, 1 or 2 |
| `Integer occupantId` | Customer ID, or `null` when free |

## 8. Integration sequence and acceptance examples

### One turn through the agreed interfaces

1. `RestaurantCLI.run()` calls `GameController.startTurn()`.
2. `TurnManager.beginTurn(restaurant)` resets the phase and adds any scheduled arrival.
3. The CLI displays `GameController.getSnapshot()`.
4. The controller accepts one successful Waiter action or explicit wait, then records the Waiter choice. Failures retry without consuming it.
5. The controller accepts one successful Chef action or explicit wait, then records the Chef choice. The Chef may prepare an order taken in step 4.
6. `GameController.finishTurn()` resolves eating, payment, waiting penalties and abandonment, then advances or closes the game.
7. Display the required end-of-turn results using the saved turn number from step 3 as turns used, only after step 6 succeeds. After the final turn, call `isVictory()` and display the outcome.

### Timing example

For a Pasta customer receiving full staff attention, with arrival turn denoted by `t`:

| Turn | Event | Satisfaction at turn end |
| --- | --- | --- |
| `t` | Arrive and seat | 92 |
| `t + 1` | Take order; Chef completes unit 1 | 84 |
| `t + 2` | Chef completes unit 2; order becomes READY | 76 |
| `t + 3` | Waiter serves | 76 |
| `t + 4` | Finish eating; become READY_TO_PAY | 76 |
| `t + 5` | Cashier collects 1800 cents and frees table | Record 76 in paid satisfaction total |

This is a timing example, not a replacement for deterministic food selection: in the meeting's base menu cycle, customer 3 is the first Pasta customer.

### Integration checks

- An invalid seat, order, serve or cook request changes no entity, statistic or staff-availability flag.
- A successful action or explicit wait consumes exactly one choice; the Chef cannot act before the Waiter completes their choice.
- Duplicate turn starts, resolution and advancement cannot add extra arrivals, penalties or payments.
- A READY order cannot be served in the same turn it finishes preparation through the normal controller flow.
- A newly served customer cannot finish eating that turn; a newly payment-ready customer cannot pay that turn.
- If several customers are eligible to pay, the earliest ready-to-pay turn wins, then the lowest ID. At most one payment is collected.
- Abandonment cancels even an unserved READY order, frees any table and adds no revenue. Repeated processing cannot count another departure.
- Paid satisfaction averages use floating-point division and values recorded at payment. Before any payment, the CLI shows `N/A`.
- The game always completes 18 turns; the turn-16 arrival cannot pay by the end. Victory requires all three targets simultaneously.
- Snapshots and their lists cannot be mutated, and remain unchanged when later turns run.
- Stage 1 must have reproducible victory and defeat runs recorded separately in test notes. These contracts specify expected behaviour; they do not constitute executed tests.

## 9. Clarifications and open decisions

### Clarifications made in this document

These make implicit interactions explicit without adding classes or later-stage features:

- `Restaurant.serve(customerId, turn)` on meeting page 8 is written as `ActionResult serve(int customerId, int turn)`, matching its description and Waiter delegation.
- `addArrival()` updates both the historical customer list and waiting queue; seating updates queue, table and customer state together.
- The Cashier's `lastPaidTurn` changes only after `recordPayment()` succeeds. Failed collection does not consume a payment opportunity.
- Ready-to-pay timestamps remain available after payment. The meeting getter's wording “-1 if the customer is not ready to pay” is interpreted as “-1 before eating completes”; payment does not erase history.
- `remainingUnits == 0` indicates completed preparation, while `isReady()` tests the current state. Zero units alone does not permit serving a cancelled or already-served order.
- Order linking uses matching IDs and the same object in Customer and Restaurant. Validation precedes mutations to satisfy the brief's failure-without-state-change requirement.
- `Table.assign()` owns occupancy, while Restaurant coordinates the customer's `seat()` transition. This prevents the transition being applied twice.
- Base arrival IDs follow arrival order, giving the deterministic Salad, Burger, Pasta cycle twice.

### Resolved decisions

| Topic | Agreed decision |
| --- | --- |
| Java version and launch | Java 17; `restaurantrush.Main` creates and connects shared instances. The CLI and TurnManager constructors above match the implemented wiring. |
| Completed-turn reporting | CLI retains the turn being played and reports it as completed only after `finishTurn()` succeeds. The current implementation uses the saved start snapshot, avoiding an off-by-one error after advancement. |
| Error handling | Invalid parameters use `IllegalArgumentException`; invalid lifecycle operations use `IllegalStateException`; invalid player actions return `ActionResult.fail(...)`. A repeated departure must not increment the departure total. |
| Constructor IDs | Customer and Table require positive IDs. The base setup supplies customer IDs 1–6 and table IDs 1–2; these are setup limits, not constructor upper bounds. Order currently also validates positive IDs and must match its customer's ID. |
| Historical display records | Preserve all arrived customers, including `PAID` and `LEFT`. GameSnapshot exposes them as immutable CustomerView records, with immutable list copies and no mutation of the source entities. |

### Implementation discrepancies and remaining clarification

- The error policy is resolved, but some current null checks still throw `NullPointerException`: RestaurantCLI uses `Objects.requireNonNull`, ActionResult rejects null messages the same way, and GameSnapshot's list copies reject null lists/elements. These are implementation discrepancies, not exceptions agreed to the policy; no code changes are made here.
- Whether `Customer.acceptOrder()` must require a `PLACED` order remains unspecified. The current implementation checks seating, matching IDs and absence of an existing order, but does not restrict the incoming order's status.

### Member handoffs from the meeting

| Members | Shared contract to align |
| --- | --- |
| M1 and M2 | Customer changes during seating, serving, payment and abandonment; no duplicate transitions |
| M1 and M3 | Waiter order creation, shared order identity and Chef preparation through Order's API |
| M2 and M3 | Seating delegation, atomic order registration, Cashier payment and table release |
| M3 and M4 | Staff limits, Waiter-before-Chef sequencing and automatic Cashier timing |
| M4 and M5 | Command/result exchange, snapshots, lifecycle calls and end-of-turn reporting |

Future Stage 2 and Stage 3 work must extend this design deliberately and document changed contracts. In particular, the brief requires customer-dispatched behaviour, VIP/Critic types, Host seating, ComboMeal composition and Happy Hour pricing/timing, but this meeting does not yet define their class APIs.
