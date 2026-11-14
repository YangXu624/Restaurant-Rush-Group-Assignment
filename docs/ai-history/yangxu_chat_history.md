# SC2002 Group Assignment

> Here is the assignment brief for my group project. Read and understand it first. I have already discussed with my team and come up with the structure and classes, and a plan to implement, which I will be uploading later on. All this will be done in the Restaurant-Rush-Group-Assignment directory
>
> User context
>
> Attachments:
> - SC2002_Restaurant_Rush_Group_Assignment.docx: `~/Downloads/SC2002_Restaurant_Rush_Group_Assignment.docx`

<details><summary>1 previous message</summary>

> I’ll read the brief using the documents skill and note the requirements. I’ll wait for your team’s structure and implementation plan before making project changes.
</details>

I’ve read and understood the [assignment brief](~/Downloads/SC2002_Restaurant_Rush_Group_Assignment.docx).

The key requirements I’ll keep in mind are:

- **Three independently runnable stages**, tagged `stage-1`, `stage-2` and `stage-3`. Stage 1 must exclude later-stage features.
- **An 18-turn Java CLI game** with limited staff actions, six scheduled arrivals and two tables. Victory requires **at least 4 paid customers, $45 revenue and 60 average paid-customer satisfaction**.
- **Strict turn timing:** newly completed dishes cannot be served until a later turn; customers eat for a full turn; newly payment-ready customers cannot pay until the next turn.
- **Stage 2:** polymorphic Regular, VIP and Critic behaviour.
- **Stage 3:** Host, composed ComboMeals and a once-per-game, two-turn Happy Hour with pricing locked at order placement.
- **Design and development evidence:** matching UML diagrams, justified responsibilities and design principles, genuine stage history, reproducible victory/defeat runs, report, video and declarations.

I’ll treat the brief as project requirements, not as instructions to start implementing. All project work will be in `Restaurant-Rush-Group-Assignment`. No files have been changed; I’m ready for your team’s class structure and implementation plan.

> Here is the classes that my group has come up with. Help to put the class contracts in `docs/class-contracts.md`
>
> User context
>
> Attachments:
> - 18oct_meeting.pdf: `~/Downloads/18oct_meeting.pdf`

<details><summary>4 previous messages</summary>

> I’ll read your team’s class design and the project’s existing guidance, then write the contracts in `docs/class-contracts.md`. I’ll flag any gaps that need a team decision.
>
> I’m using the PDF skill to read the meeting notes. The project currently contains only a README, so the contracts will be the first design document in the repository.
>
> The notes define 17 types for Stage 1, including method signatures, turn timing and member ownership. I’ll preserve that design and make the preconditions, state changes and failure behaviour explicit. I’ll also flag a few unspecified details, such as constructor wiring and how the CLI reports turns used.
>
> <details><summary>Created a file</summary>
>
> - Wrote `./Restaurant-Rush-Group-Assignment/docs/class-contracts.md` (+505 -0)
>
> </details>
</details>

Created [docs/class-contracts.md](./Restaurant-Rush-Group-Assignment/docs/class-contracts.md).

It covers all 17 types, member ownership, attributes, method contracts, state transitions, turn timing and integration checks. Clarifications and unresolved decisions are explicitly separated from the team’s agreed design.

No implementation files were changed.

> Help me set up the initial Java project skeleton for Restaurant Rush.
>
> Please refer to Restaurant-Rush-Group-Assignment/docs/class-contracts.md, which contains the class design and method contracts we've agreed on as a group. We'll use this as our main reference throughout implementation.
>
> A few things to note:
>
> - Use `restaurantrush` as the base package, with separate `boundary`, `control`, `entity`, `staff` and `support` packages.
> - Create the classes, enums, attributes and method signatures according to the document, along with a simple `Main.java` entry point.
> - Don't implement the game logic yet. Leave unimplemented methods with `UnsupportedOperationException` rather than dummy return values. Each member will implement their assigned classes later.
> - Add a suitable `.gitignore`, a short README and simple shell scripts for compiling and running the application using `javac` and `java`.
> - Keep the structure straightforward and avoid introducing unnecessary classes or abstractions.
>
> Once done, check that everything compiles and that `Main.java` runs successfully.
>
> If you notice any inconsistencies or missing details in the class contracts, flag them instead of making major design decisions yourself. Don't modify the contract document for now.
>
> Finally, give me a quick summary of what you've created, whether compilation succeeded, and anything we still need to agree on.

<details><summary>3 previous messages</summary>

> I’ll create the package structure and Java stubs from the contracts, add build/run scripts, and verify compilation and the entry point. I’ll leave the contracts unchanged and flag any missing details.
>
> The contracts cover all 17 types, but leave CLI/TurnManager constructor wiring and a few validation/display details open. I’ll keep `Main` as a launch check that prints a message without calling unfinished game code. The scripts will target Java 17, which supports the agreed records; Java 21 is installed here.
>
> <details><summary>Created 4 files, edited a file</summary>
>
> - Wrote `./Restaurant-Rush-Group-Assignment/.gitignore` (+18 -0)
> - Wrote `./Restaurant-Rush-Group-Assignment/README.md` (+55 -1)
> - Wrote `./Restaurant-Rush-Group-Assignment/compile.sh` (+12 -0)
> - Wrote `./Restaurant-Rush-Group-Assignment/run.sh` (+6 -0)
> - Wrote `./Restaurant-Rush-Group-Assignment/src/restaurantrush/Main.java` (+9 -0)
>
> </details>
</details>

Created the skeleton with all 17 contract types across the five requested packages, plus `Main.java`, `.gitignore`, executable build/run scripts and a [README](./Restaurant-Rush-Group-Assignment/README.md).

- Unimplemented operations throw `UnsupportedOperationException`.
- No game logic added; contracts remain unchanged.
- **Compilation succeeded without warnings.**
- **Main ran successfully**, printing the skeleton status message.

Run from the project directory:

```sh
./compile.sh
./run.sh
```

Still to agree: CLI/TurnManager constructor wiring, completed-turn reporting, validation/ID rules, snapshot customer coverage, and confirmation of Java 17 as the build target. These are noted in the README.

> Okay I just implemented MenuItem and its unit test, and updated test.sh. Help me vet before I commit. dont make any changes, just give me feedback if any. and dont commit or push on my behalf

<details><summary>1 previous message</summary>

> I’ll review the MenuItem implementation, its tests and `test.sh` against the contracts, then run the relevant checks. I won’t edit source files, commit or push.
</details>

One issue to fix before committing:

- **`test.sh` uses `test/`, but your directory is `tests/`.** Update the paths on [line 12](./Restaurant-Rush-Group-Assignment/test.sh:12), line 19 and the prefix removal on line 20 consistently. Running `./test.sh` currently fails with `find: test: No such file or directory` and `error: no source files`.

Otherwise, **MenuItem looks correct against the contracts**: private final fields, appropriate validation, zero-price support and correct getters. The tests cover valid construction and the required invalid-input cases.

Verification: application compilation succeeded; compiling and running `MenuItemTest` directly passed all tests.

Also, only `MenuItem.java` is currently staged; `test.sh` and `tests/` remain untracked. No source files changed, and nothing committed or pushed.

> Just implemented Order class and its tests as well. Help to vet again

<details><summary>1 previous message</summary>

> I’ll review `Order` and its tests against the contracts and run the test suite. As before, I won’t edit source files, commit or push.
</details>

**No blocking issues found.** `Order` matches the contracts: guarded state transitions, correct preparation progress, cancellation from all three permitted states, and a locked bill.

`./test.sh` now works: compilation succeeded and both MenuItem and Order tests passed.

Two non-blocking points:

- **Strengthen cancellation tests:** in [OrderTest.java](./Restaurant-Rush-Group-Assignment/tests/restaurantrush/entity/OrderTest.java:127), verify cancelled orders reject preparation, serving and payment. After cancelling a READY order, also check `isReady()` is false despite zero remaining units. For rejected operations, assert status and remaining units stay unchanged.
- **Confirm ID policy:** [Order.java](./Restaurant-Rush-Group-Assignment/src/restaurantrush/entity/Order.java:15) accepts any positive ID, and tests use IDs above 6. That’s reasonable, but the contracts still list this as a team decision—keep Customer’s validation consistent.

No source changes, commits or pushes made.

> Review my current Customer implementation, and suggest areas I can test when writing my unit tests, like edge cases and boundary conditions etc
>
> But you dont have to write the code for me

<details><summary>1 previous message</summary>

> I’ll check your current `Customer` implementation against the contracts and related classes, then suggest focused tests for lifecycle transitions, edge cases and boundaries. I won’t change any code.
</details>

Your `Customer` implementation largely matches the contracts. The lifecycle guards, deterministic food choice, satisfaction floor and eating delay look correct.

One issue worth addressing is in [`abandon()`](./Restaurant-Rush-Group-Assignment/src/restaurantrush/entity/Customer.java:105): it sets the customer to `LEFT` **before** calling `order.cancel()`. If cancellation throws—for example, the linked order is already cancelled—the method fails but leaves the customer changed. Validate cancellation eligibility or complete cancellation before changing customer status. This matters especially because the order reference is shared and mutable.

For your unit tests, I’d cover these areas:

| Area | Cases and expected outcomes |
| --- | --- |
| **Construction** | Positive ID succeeds; zero and negative IDs throw. Verify satisfaction `100`, status `WAITING`, no order, and both turn fields `-1`. |
| **Food selection** | IDs 1–6 select Salad, Burger, Pasta twice. A one-item menu always returns that item. Null/empty menus throw. Repeated calls return the same item without changing customer state or attaching an order. |
| **Seating** | `WAITING → SEATED` succeeds. Seating again or seating a departed customer throws without changing state. |
| **Accepting orders** | A seated customer accepts a matching-ID order and retains the exact object reference; status stays `SEATED`. Null, mismatched-ID, second orders and orders before seating are rejected. Verify a rejected replacement preserves the original order. |
| **Serving** | Reject service without an order or while the order is merely `READY`. After the order becomes `SERVED`, customer service succeeds and records the turn. Reject zero/negative turns and repeated service; preserve the original timestamp. |
| **Eating delay** | If served on turn 4, `finishEating(3)` and `finishEating(4)` do nothing; `finishEating(5)` produces `READY_TO_PAY` with timestamp 5. Another call on turn 6 must not overwrite it. Calls in other customer states do nothing. |
| **Satisfaction boundaries** | Test both `WAITING` and `SEATED`: 100 → 92; after 12 penalties, 4; after 13, 0; further penalties remain 0. Reaching zero alone must not change status to `LEFT`. |
| **Protected satisfaction** | Penalties do nothing in `SERVED`, `READY_TO_PAY`, `PAID` and `LEFT`. Serve a customer whose satisfaction has already decreased to verify it freezes at that value. |
| **Payment** | Reject payment before `READY_TO_PAY`, and while the associated order is still `SERVED`. Once the order is `PAID`, customer payment succeeds. Reject repeated payment; preserve satisfaction and timing history. |
| **Abandonment** | Cover waiting with no order, seated with no order, and seated with `PLACED`, `PREPARING` and `READY` orders. Customer becomes `LEFT`; any order becomes `CANCELLED`. Reject abandonment after service/payment and repeated abandonment. Include the partial-failure case described above. |

For **every rejected operation**, check both the exception and that observable state remains unchanged—not just that something was thrown.

Two details to keep separate from Customer tests:

- The **zero-satisfaction requirement for abandonment** belongs to `Restaurant` under your current contract; `Customer.abandon()` doesn’t enforce it.
- The **extra turn before payment** belongs to Restaurant/TurnManager. Customer’s `markPaid()` has no turn parameter and should not enforce that delay itself.

One contract clarification: `acceptOrder()` currently accepts any matching-ID order, regardless of its status. Agree whether “new order” explicitly requires `PLACED` before writing a test that demands that restriction.

No code changed. This was a source review; I haven’t executed Customer tests.

> Thanks, these suggestions look good. I have already fixed the atomicity issue in abandon()&#x20;
>
> Let's go ahead and implement the missing tests
>
> A few additional scenarios I'd like to cover:
>
> - A complete customer lifecycle from WAITING to PAID, including ordering, food preparation, serving, eating and payment. Verify the customer and order statuses stay consistent throughout.
> - Abandonment followed by attempts to prepare, serve or pay the cancelled order. These should all fail without changing state.
> - Food selection with a menu containing four items, to verify that the deterministic selection rule works beyond our three-item Stage 1 menu.
> - Invalid operations involving shared Customer and Order objects, checking that neither object is accidentally modified when an operation fails
>
> prioritize meaningful tests that could catch actual bugs rather than maximizing the number of test cases. Follow docs/class-contracts.md, and don't introduce additional restrictions that aren't specified there
>
> once done, run all relevant tests and compile the project. Summarize what you added, any bugs discovered, and any design questions that still need clarification. Keep changes limited to Customer and its tests, except where a small related fix is necessary.

<details><summary>1 previous message</summary>

> I’ll inspect the updated Customer code and existing tests, then add focused coverage for the lifecycle, cancellation, menu selection and shared-object failure cases. I’ll keep the tests within the contracts and run the full suite and project compilation.
</details>

Added [CustomerTest.java](./Restaurant-Rush-Group-Assignment/tests/restaurantrush/entity/CustomerTest.java) covering:

- Complete customer/order lifecycle and eating-delay boundaries.
- Cancellation at each preparation stage, followed by rejected cooking, serving and payment.
- Deterministic selection with three-, four- and single-item menus.
- Satisfaction reaching zero and remaining unchanged after service.
- Invalid operations preserving both customer and shared-order state.
- A regression test for your `abandon()` atomicity fix.

**All Customer, MenuItem and Order tests passed. Project compilation also succeeded.** No new bugs were found; only the new test file was added.

Still worth clarifying: whether accepting an order requires `PLACED` status, and the team’s ID/exception conventions. The tests don’t add a `PLACED` restriction or move Restaurant’s timing/abandonment responsibilities into Customer.

Nothing committed or pushed.

> All my teammates have pushed and merged their code, and I have pulled the latest version. Now that we've integrated the Stage 1 classes, could you perform a read-only consistency review against docs/class-contracts.md?
>
> Focus on invariants spanning multiple objects, e.g Customer–Order status consistency, queue and table occupancy, payment eligibility, and revenue accounting.
>
> Check whether failed operations could result in partial state mutations, especially where several entity methods are invoked sequentially. For each potential issue, identify the relevant methods, violated invariant, and a reproducible scenario.
>
> Don't modify any code or propose architectural changes unless you identify a concrete problem.

<details><summary>4 previous messages</summary>

> I’ll trace the integrated Stage 1 operations against the contracts, focusing on shared state, timing and failure atomicity. I’ll leave the code unchanged and report concrete issues with reproducible scenarios.
>
> <details><summary>Explored 13 files, a search, ran a command</summary>
>
> - Ran `git status --short; rg --files -g AGENTS.md -g '*.java'; cat docs/class-contracts.md`
>
> <details><summary>Explored 13 files, a search</summary>
>
> - Read `./src/restaurantrush/entity/Restaurant.java`
> - Read `./src/restaurantrush/entity/Table.java`
> - Read `./src/restaurantrush/staff/Waiter.java`
> - Read `./src/restaurantrush/staff/Cashier.java`
> - Read `./src/restaurantrush/control/TurnManager.java`
> - Read `./src/restaurantrush/control/GameController.java`
> - Read `./src/restaurantrush/entity/Customer.java`
> - Read `./src/restaurantrush/entity/Order.java`
> - Read `./src/restaurantrush/support/GameSnapshot.java`
> - Read `./src/restaurantrush/Main.java`
> - Read `./src/restaurantrush/staff/Chef.java`
> - Read `./test.sh`
> - Searched for `void test|static void|resolveTurn|exception|partial` in `TurnManagerTest.java`
> - Read `./src/restaurantrush/boundary/RestaurantCLI.java`
>
> </details>
>
> </details>
>
> The normal controller path appears to enforce the serving and payment delays correctly. I found a possible partial mutation in `Restaurant.serve()` when given an invalid turn, and another in `removeAbandoned()` if order cancellation throws. I’ll reproduce both and check whether they’re reachable through ordinary gameplay or only through direct domain calls.
>
> <details><summary>Explored 4 files, ran 2 commands</summary>
>
> <details><summary>Explored 4 files</summary>
>
> - Read `./tests/restaurantrush/integration/MainTest.java`
> - Read `./tests/restaurantrush/integration/M5IntegrationTest.java`
> - Read `./src/restaurantrush/control/TurnManager.java`
> - Read `./src/restaurantrush/support/ActionResult.java`
>
> </details>
>
> - Ran `review_dir=$(mktemp -d /private/tmp/restaurant-review.XXXXXX)
> printf '%s' "$review_dir" > /private/tmp/restaurant-review-current
> find src -name '*.java' -type f | sort > "$review_dir/sources.txt"
> find tests -name '*.java' -type f | sort > "$review_dir/tests.txt"
> javac --release 17 -encoding UTF-8 -Xlint:all -d "$review_dir/classes" @"$review_dir/sources.txt"
> javac --release 17 -encoding UTF-8 -Xlint:all -cp "$review_dir/classes" -d "$review_dir/test-classes" @"$review_dir/tests.txt"
> for file in $(find tests -name '*Test.java' -type f | sort); do
>  class_name=$(echo "$file" | sed -e 's|^tests/||' -e 's|\.java$||' -e 's|/|.|g')
>  java -cp "$review_dir/classes:$review_dir/test-classes" "$class_name" || exit 1
> done`
>
> - Ran `review_dir=$(cat /private/tmp/restaurant-review-current)
> cat > "$review_dir/ConsistencyProbe.java" <<'JAVA'
> import restaurantrush.entity.*;
> import restaurantrush.staff.*;
> import restaurantrush.control.*;
> public class ConsistencyProbe {
>  static Customer seated(Restaurant r, int id, int table) {
>   Customer c = new Customer(id); r.addArrival(c); r.seat(id, table); new Waiter().takeOrder(r,id); return c;
>  }
>  static void attempt(Runnable action) {
>   try { action.run(); } catch (RuntimeException e) { System.out.println(e.getClass().getSimpleName()+": "+e.getMessage()); }
>  }
>  public static void main(String[] args) {
>   Restaurant r = new Restaurant(); Customer c = seated(r,1,1); c.getOrder().prepareOneUnit();
>   attempt(() -> r.serve(1,0));
>   System.out.printf("invalid serve: customer=%s order=%s servedCount=%d retry=%s%n",c.getStatus(),c.getOrder().getStatus(),r.getServedCount(),r.serve(1,1).isSuccess());
>   Restaurant a = new Restaurant(); Customer x = seated(a,1,1);
>   for(int i=0;i<13;i++) x.applyWaitingPenalty();
>   x.getOrder().cancel();
>   attempt(() -> a.removeAbandoned(x));
>   System.out.printf("failed abandon: customer=%s tableFree=%s queue=%d departures=%d%n",x.getStatus(),a.getTables().get(0).isAvailable(),a.getWaitingQueue().size(),a.getAbandonedCount());
>   Restaurant t = new Restaurant(); TurnManager tm = new TurnManager(new Cashier()); tm.beginTurn(t);
>   t.seat(1,1); new Waiter().takeOrder(t,1); Customer bad=t.findCustomer(1);
>   for(int i=0;i<12;i++) bad.applyWaitingPenalty(); bad.getOrder().cancel();
>   Customer other=new Customer(2); t.addArrival(other); tm.recordWaiterTask(); tm.recordChefTask();
>   attempt(() -> tm.resolveTurn(t)); System.out.println("other satisfaction after failed resolution="+other.getSatisfaction());
>   attempt(() -> tm.resolveTurn(t)); System.out.println("other satisfaction after retry="+other.getSatisfaction());
>   Restaurant p = new Restaurant(); Customer payer = new Customer(1); p.addArrival(payer); p.seat(1,1);
>   Order unregistered=new Order(1,p.getMenu().get(0)); payer.acceptOrder(unregistered); unregistered.prepareOneUnit(); unregistered.markServed(); payer.markServed(1); payer.finishEating(2);
>   System.out.printf("unregistered payment: accepted=%s revenue=%d orders=%d servedCount=%d%n",p.recordPayment(payer,3),p.getRevenueCents(),p.getOrders().size(),p.getServedCount());
>  }
> }
> JAVA
> javac --release 17 -cp "$review_dir/classes" -d "$review_dir/test-classes" "$review_dir/ConsistencyProbe.java"
> java -cp "$review_dir/classes:$review_dir/test-classes" ConsistencyProbe
> git status --short`
>
> </details>
</details>

I reproduced **two partial-mutation issues and one payment-validation gap**. All require direct domain calls or previously inconsistent shared state; I found no corresponding failure through the normal CLI/controller flow.

**1. Invalid service turn leaves Customer and Order inconsistent**

Relevant methods: [`Restaurant.serve()`](./Restaurant-Rush-Group-Assignment/src/restaurantrush/entity/Restaurant.java:128), `Customer.markServed()`.

- **Reproduce:** Add and seat customer 1, take their order through Waiter, prepare it to `READY`, then call `restaurant.serve(1, 0)`.
- **Observed:** `order.markServed()` succeeds, but `customer.markServed(0)` throws. The customer remains `SEATED`, the order is `SERVED`, and `servedCount` remains 0. Retrying with a valid turn fails because the order is no longer `READY`.
- **Violated invariant:** Failed serving must change nothing; successful service must update both statuses and the count together.
- **Reachability:** Direct Restaurant/Waiter calls. The controller supplies a positive turn, so ordinary CLI input cannot trigger this.
- **Local correction:** Validate the turn before changing the order.

**2. Failed abandonment releases the table before cancellation succeeds**

Relevant methods: [`Restaurant.removeAbandoned()`](./Restaurant-Rush-Group-Assignment/src/restaurantrush/entity/Restaurant.java:241), `Customer.abandon()`, `Order.cancel()`.

- **Reproduce:** Seat a customer and register their order normally. Reduce satisfaction to zero, cancel the shared order directly, then call `restaurant.removeAbandoned(customer)`.
- **Observed:** The table is released before `Customer.abandon()` calls `Order.cancel()`, which rejects the already-cancelled order. The customer remains `SEATED`, occupies no table, is not queued, and `abandonedCount` remains 0.
- **Violated invariant:** A failed operation must preserve occupancy and customer state. Your Customer-level atomicity fix works, but the surrounding Restaurant operation still mutates first.
- **Reachability:** Requires a collaborator to have cancelled the order independently; normal abandonment does not establish this starting state.
- **Local correction:** Ensure cancellation/customer abandonment succeeds before removing queue membership or releasing the table.

This also affects [`TurnManager.resolveTurn()`](./Restaurant-Rush-Group-Assignment/src/restaurantrush/control/TurnManager.java:80): if abandonment throws, earlier penalties have already run but the turn remains unresolved. I reproduced another customer dropping **100 → 92 on the failed resolution, then 92 → 84 on a retry of the same turn**. The code documents that retries are unsafe, and the CLI propagates the exception; callers must not treat failure as “nothing happened.”

**3. Payment accepts an order absent from the restaurant registry**

Relevant method: [`Restaurant.recordPayment()`](./Restaurant-Rush-Group-Assignment/src/restaurantrush/entity/Restaurant.java:190).

- **Reproduce:** Add and seat a customer, attach a matching-ID order directly without registering it, then prepare and serve it through the entity methods. Mark the customer served on turn 1, finish eating on turn 2, and call `recordPayment(customer, 3)`.
- **Observed:** Payment succeeds, revenue becomes **900 cents**, and paid count increases, while `getOrders()` remains empty.
- **Violated invariant:** The customer’s order and restaurant registry must contain the same instance; payment currently accepts a broken association.
- **Reachability:** Requires bypassing normal Waiter ordering and Restaurant serving. `Restaurant.serve()` already checks this association, but payment does not.
- **Local correction:** Check registered-order identity before payment mutations.

Compilation and **all 16 existing test classes passed**, including the gameplay fixtures. Normal payment-delay checks, oldest-payer selection, duplicate-payment prevention and revenue totals passed the reviewed tests.

No repository files were changed; compilation and reproduction probes used a temporary directory.

> We've discussed and aligned on a few implementation details during our group meetings. Can you update docs/class-contracts.md to reflect these decisions? You can also make other updates if necessary.
>
> 1. We agreed to use Java 17, with Main.java responsible for initializing and connecting the components using shared instances.
> 2. The CLI should track the last completed turn when displaying results to avoid off-by-one errors.
> 3. We agreed to use IllegalArgumentException for invalid parameters, IllegalStateException for invalid lifecycle operations, and ActionResult.fail() for invalid player actions.
> 4. Customer and Table constructors should enforce positive IDs, while our base game uses customer IDs 1–6.
> 5. We agreed to preserve historical customer records, including PAID and LEFT customers, and expose them through immutable views without modifying the underlying entities.
>
> Please update the relevant sections of the document and move these points from outstanding decisions to resolved decisions. Check that the wording accurately reflects our current implementation and keep the changes concise and consistent with the existing document.
>
> This is purely a documentation update to reflect our agreed design decisions, so don't modify any Java code or tests. Show me the diff once you're done, and don't commit or push anything yet.

<details><summary>4 previous messages</summary>

> I’ll check these decisions against the current implementation, update the relevant contract sections, and show you the diff. I’ll leave Java code and tests unchanged and won’t commit or push.
>
> <details><summary>Ran a command</summary>
>
> - Ran `git status --short; sed -n '1,105p' docs/class-contracts.md; sed -n '470,530p' docs/class-contracts.md; cat src/restaurantrush/Main.java; rg -n 'last|start.turn|Turns used|Illegal|Objects.require|public RestaurantCLI' src/restaurantrush/boundary/RestaurantCLI.java; rg -n 'public TurnManager|if \(id|throw|List.copyOf|getCustomers' src/restaurantrush/entity/Table.java src/restaurantrush/entity/Customer.java src/restaurantrush/support/GameSnapshot.java src/restaurantrush/control/TurnManager.java; cat compile.sh`
>
> </details>
>
> The code already uses shared instances, preserves the completed turn for display, and includes historical customers in immutable snapshots. One mismatch remains: some null checks throw `NullPointerException` rather than the agreed `IllegalArgumentException`. I’ll record that as an implementation discrepancy, while marking the exception policy itself as resolved.
>
> <details><summary>Ran a command</summary>
>
> - Ran ``python3 - <<'PY'
> from pathlib import Path
> p = Path('docs/class-contracts.md')
> s = p.read_text()
> def replace(old, new):
>  global s
>  assert old in s, old
>  s = s.replace(old, new, 1)
> replace('It is a design contract, not a claim that these classes already exist.', 'It includes the decisions confirmed during subsequent group meetings and remains the main implementation reference.')
> replace("The meeting's class names, ownership, attributes and operations are preserved.", "The meeting's class names and ownership are preserved; agreed wiring and display details are included below.")
> replace('[Clarifications and open decisions](#clarifications-and-open-decisions)', '[Clarifications and open decisions](#9-clarifications-and-open-decisions)')
> replace('- All entity and control attributes are private.', '- Target Java 17. `Main.java` initializes and connects the game components using shared instances, then launches the CLI.\n- All entity and control attributes are private.')
> replace('- Internal lifecycle misuse is distinct from invalid player input. The meeting explicitly requires `IllegalStateException` for invalid turn resolution, invalid order preparation and releasing an empty table. Other exception details remain an integration decision; see Section 9.', '- Use `IllegalArgumentException` for invalid parameters, `IllegalStateException` for invalid lifecycle operations, and `ActionResult.fail(...)` for invalid player actions. Existing boolean-returning operations retain their documented failure results. Current implementation discrepancies are noted in Section 9.')
> replace('- Paid and departed customers and their orders remain in the historical lists. They are excluded from active customers and the waiting queue.', '- Paid and departed customers and their orders remain in the historical lists. `PAID` and `LEFT` customers are excluded from active customers and the waiting queue, but included in immutable display views. Creating views must not modify the underlying entities.')
> replace('| `Scanner input` | Command input |', '| `Scanner input` | Command input |\n| `PrintStream output` | Destination for CLI output |')
> replace('| `void run()` | Start each turn through the controller, then display its snapshot.', '| `RestaurantCLI(GameController controller, Scanner input, PrintStream output)` | Store the shared controller and supplied streams; the caller owns the streams. |\n| `void run()` | Start each turn through the controller, then display its snapshot.')
> replace('Finish the turn. Repeat until the game finishes; stop cleanly if input ends.', 'After `finishTurn()` succeeds, display results using the saved turn number as the last completed turn. Repeat until the game finishes; stop cleanly if input ends.')
> replace('Constructor wiring and end-of-turn display timing are covered in Section 9.', 'The current CLI retains the start-of-turn snapshot in a local variable and uses `start.turn()` after successful completion; it does not need a separate counter field. Do not use the advanced snapshot turn as the completed-turn count. At final completion, the snapshot remains at turn 18.')
> replace('## 4. Control contracts\n', '## 4. Control contracts\n\n### Main\n\n**Kind:** Application entry point\n\n`public static void main(String[] args)` creates one `Restaurant`, `Cashier`, `TurnManager`, `Waiter`, `Chef` and `GameController`. Pass the same Cashier to TurnManager, and the shared Restaurant, TurnManager, Waiter and Chef to GameController. Construct RestaurantCLI with that controller, a Scanner over standard input and `System.out`, then call `run()`. Main performs wiring, not game rules.\n')
> replace('| `void beginTurn(Restaurant r)` |', '| `TurnManager(Cashier cashier)` | Store the shared Cashier; reject null with `IllegalArgumentException`. |\n| `void beginTurn(Restaurant r)` |')
> replace('| `int id` | Fixed ID; base arrivals use 1 through 6 |', '| `int id` | Fixed positive ID; base arrivals use 1 through 6 |')
> replace('| `Customer(int id)` | Create a waiting customer with satisfaction 100, no order and both timing fields at -1. |', '| `Customer(int id)` | Reject nonpositive IDs with `IllegalArgumentException`. Create a waiting customer with satisfaction 100, no order and both timing fields at -1. The constructor is not restricted to IDs 1–6. |')
> replace('| `Table(int id)` | Create a free table with a positive ID. |', '| `Table(int id)` | Reject nonpositive IDs with `IllegalArgumentException`; create a free table. Restaurant supplies the base IDs 1 and 2 and ensures uniqueness. |')
> replace('| `List<CustomerView> customers` | Customer summaries, including satisfaction and order progress |', '| `List<CustomerView> customers` | All arrived customers in arrival order, including `PAID` and `LEFT`, with satisfaction and order progress |')
> replace('It stores no mutable entity references. Later game changes must not alter an existing snapshot.', 'It stores no mutable entity references and does not modify the entities it reads. Later game changes must not alter an existing snapshot.')
> replace('7. Display the required end-of-turn results. After the final turn, call `isVictory()` and display the outcome. See Section 9 for the distinction between current turn and turns used.', '7. Display the required end-of-turn results using the saved turn number from step 3 as turns used, only after step 6 succeeds. After the final turn, call `isVictory()` and display the outcome.')
> a=s.index('### Decisions still to align before implementation')
> b=s.index('### Member handoffs from the meeting',a)
> s=s[:a]+'''### Resolved decisions
>
> | Topic | Agreed decision |
> | --- | --- |
> | Java version and launch | Java 17; `restaurantrush.Main` creates and connects shared instances. The CLI and TurnManager constructors above match the implemented wiring. |
> | Completed-turn reporting | CLI retains the turn being played and reports it as completed only after `finishTurn()` succeeds. The current implementation uses the saved start snapshot, avoiding an off-by-one error after advancement. |
> | Error handling | Invalid parameters use `IllegalArgumentException`; invalid lifecycle operations use `IllegalStateException`; invalid player actions return `ActionResult.fail(...)`. A repeated departure must not increment the departure total. |
> | Constructor IDs | Customer and Table require positive IDs. The base setup supplies customer IDs 1–6 and table IDs 1–2; these are setup limits, not constructor upper bounds. Order currently also validates positive IDs and must match its customer's ID. |
> | Historical display records | Preserve all arrived customers, including `PAID` and `LEFT`. GameSnapshot exposes them as immutable CustomerView records, with immutable list copies and no mutation of the source entities. |
>
> ### Implementation discrepancies and remaining clarification
>
> - The error policy is resolved, but some current null checks still throw `NullPointerException`: RestaurantCLI uses `Objects.requireNonNull`, ActionResult rejects null messages the same way, and GameSnapshot's list copies reject null lists/elements. These are implementation discrepancies, not exceptions agreed to the policy; no code changes are made here.
> - Whether `Customer.acceptOrder()` must require a `PLACED` order remains unspecified. The current implementation checks seating, matching IDs and absence of an existing order, but does not restrict the incoming order's status.
>
> '''+s[b:]
> p.write_text(s)
> PY
> git diff --check
> git diff --stat
> git diff -- docs/class-contracts.md
> git status --short``
>
> </details>
</details>

Updated [docs/class-contracts.md](./Restaurant-Rush-Group-Assignment/docs/class-contracts.md). All five decisions are now resolved, with corresponding updates to constructors, Main wiring, CLI reporting and snapshots.

Key diff excerpts:

```diff
+ Target Java 17. Main.java initializes and connects the game
+ components using shared instances, then launches the CLI.

- Other exception details remain an integration decision.
+ Use IllegalArgumentException for invalid parameters,
+ IllegalStateException for invalid lifecycle operations,
+ and ActionResult.fail(...) for invalid player actions.

+ RestaurantCLI(GameController controller, Scanner input, PrintStream output)
+ TurnManager(Cashier cashier)

+ The current CLI retains the start-of-turn snapshot in a local
+ variable and uses start.turn() after successful completion;
+ it does not need a separate counter field.

- Customer constructor: Create a waiting customer...
+ Reject nonpositive IDs with IllegalArgumentException.
+ The constructor is not restricted to IDs 1–6.

- Table constructor: Create a free table with a positive ID.
+ Reject nonpositive IDs with IllegalArgumentException;
+ Restaurant supplies base IDs 1 and 2 and ensures uniqueness.

- Customer summaries, including satisfaction and order progress
+ All arrived customers in arrival order, including PAID and LEFT,
+ with satisfaction and order progress

- ### Decisions still to align before implementation
+ ### Resolved decisions
+ ### Implementation discrepancies and remaining clarification
```

The document also flags the existing `NullPointerException` checks as discrepancies from the agreed exception policy. Whether `acceptOrder()` requires a `PLACED` order remains unspecified.

Only this Markdown file changed: **35 insertions, 20 deletions**. `git diff --check` passed. No Java/test changes, commits or pushes.

> We're finishing Stage 1, and Section 4.1 of the assignment requires one reproducible victory run and one reproducible defeat run.
>
> Could you help create `docs/test-notes.md` to document our existing gameplay tests?
>
> Please:
>
> 1. Inspect our existing test scripts and gameplay fixtures to identify how the victory and defeat scenarios are executed. Reuse these rather than creating new test cases.
> 2. Run both scenarios and verify that they terminate after exactly 18 turns, with the correct victory or defeat outcome based on all three conditions: paid customer count, total revenue, and average satisfaction of paid customers.
> 3. Document the exact commands and input fixtures needed to reproduce each scenario, along with the expected and actual outcomes and final metrics. Please obtain these values from the test results rather than assuming them.
> 4. Distinguish between tests that directly exercise the game classes and those that run through the CLI, so we don't misrepresent the level of integration testing performed.
> 5. Check whether the documented commands can be reproduced from a clean checkout. If the `stage-1` tag already exists, verify against that version as well.
>
> Keep the documentation concise and focused on reproducibility. This is purely a documentation task, so don't modify application code, tests, or fixtures. If you find any discrepancies or missing test coverage, report them separately rather than making changes.
>
> Show me the documentation diff when you're done, and don't commit or push anything yet.

<details><summary>4 previous messages</summary>

> I’ll inspect the existing fixtures and test runners, run the victory and defeat scenarios, and document the observed results. I’ll also check clean-checkout reproducibility and whether a local `stage-1` tag exists. Only `docs/test-notes.md` will be added or changed.
>
> <details><summary>Ran 2 commands</summary>
>
> - Ran `git status --short; git tag --list stage-1; git rev-parse HEAD; cat compile.sh run.sh test.sh; cat tests/restaurantrush/integration/MainTest.java; cat tests/restaurantrush/integration/M5IntegrationTest.java; cat tests/fixtures/m5/victory.txt tests/fixtures/m5/defeat.txt; cat README.md`
> - Ran `rg -n 'Victory|victory|MIN_|threshold|59|4500|4499|paidCount' tests/restaurantrush/control/GameControllerTest.java; ls docs; java -version; javac -version; git ls-files -s compile.sh run.sh test.sh tests/fixtures/m5/victory.txt tests/fixtures/m5/defeat.txt
> review_dir=$(mktemp -d /private/tmp/restaurant-repro.XXXXXX)
> printf '%s' "$review_dir" > /private/tmp/restaurant-repro-current
> git archive HEAD | tar -x -C "$review_dir"
> cd "$review_dir"
> ./run.sh < tests/fixtures/m5/victory.txt > victory.log
> ./run.sh < tests/fixtures/m5/defeat.txt > defeat.log
> ./test.sh restaurantrush.integration.MainTest
> ./test.sh restaurantrush.integration.M5IntegrationTest
> python3 - <<'PY'
> from pathlib import Path
> import re
> for name in ('victory','defeat'):
>  s=Path(name+'.log').read_text()
>  turns=[int(n) for n in re.findall(r'End of turn (\d+)\n',s)]
>  assert turns==list(range(1,19)), turns
>  assert 'Turn 19' not in s and 'Input ended.' not in s
>  result=re.search(r'(Victory|Defeat)\nRevenue: \$(\d+\.\d+)\nAverage paid satisfaction: ([^\n]+)\nPaid: (\d+) \| Served: (\d+) \| Unhappy departures: (\d+)\nTurns used: (\d+)\n\Z',s)
>  assert result
>  outcome,revenue,avg,paid,served,left,used=result.groups()
>  won=int(paid)>=4 and float(revenue)>=45 and avg!='N/A' and float(avg)>=60
>  assert won==(outcome=='Victory')
>  print(name+': exit successful; sequential completed turns 1–18; outcome matches all-three-condition rule')
>  print(result.group(0))
> PY`
>
> </details>
>
> Both fixtures passed from a fresh export of the committed files, with no pre-existing build output. Each completed turns 1–18 exactly once. Victory ended with 5 paid customers, $66.00 revenue and 79.20 average paid satisfaction; defeat ended with 0 paid, $0.00 and N/A. No local `stage-1` tag exists, so I can’t verify that tagged version.
>
> <details><summary>Created a file, ran 2 commands</summary>
>
> - Ran `sed -n '140,175p' tests/restaurantrush/control/GameControllerTest.java; sed -n '230,270p' tests/restaurantrush/control/GameControllerTest.java
> review_dir=$(cat /private/tmp/restaurant-repro-current)
> cd "$review_dir"
> ./test.sh restaurantrush.control.GameControllerTest
> wc -l tests/fixtures/m5/victory.txt tests/fixtures/m5/defeat.txt`
> - Wrote `./Restaurant-Rush-Group-Assignment/docs/test-notes.md` (+63 -0)
> - Ran `git diff --no-index --check /dev/null docs/test-notes.md; git diff --no-index -- /dev/null docs/test-notes.md; git status --short`
>
> </details>
</details>

Created [docs/test-notes.md](./Restaurant-Rush-Group-Assignment/docs/test-notes.md), documenting exact commands, fixtures, expected/actual results and integration-test scope.

| Verified result | Victory fixture | Defeat fixture |
|---|---:|---:|
| Completed turns | 18 | 18 |
| Paid customers | 5 | 0 |
| Revenue | $66.00 | $0.00 |
| Average paid satisfaction | 79.20 | N/A |
| Outcome | Victory | Defeat |

Both production CLI runs and all three selected test suites passed from a clean export of the committed files. No local `stage-1` tag exists.

The full addition diff is shown above: **one new Markdown file, 63 lines**. Separately documented limitations: individual target failures are tested directly with supplied statistics, and README overstates the transcript detail provided in these notes.

No application code, tests or fixtures changed. Nothing committed or pushed.

> Okay I have aligned the documents on my end. Help check whether they align now

<details><summary>2 previous messages</summary>

> I’ll compare the updated README, class contracts and test notes with each other and the implementation, then flag any remaining inconsistencies. I won’t change any files.
>
> <details><summary>Ran 2 commands</summary>
>
> - Ran `git status --short; cat README.md; cat docs/test-notes.md; cat docs/class-contracts.md`
> - Ran `rg -n 'all units|ORDERED|victory evaluator|closed all|Invalid programmatic|Reproducible Stage' README.md; rg -n 'NullPointerException|remains unspecified' docs/class-contracts.md; cat src/restaurantrush/entity/OrderStatus.java; rg -n 'isVictory|requireNonNull' src/restaurantrush/control/GameController.java src/restaurantrush/control/TurnManager.java src/restaurantrush/boundary/RestaurantCLI.java src/restaurantrush/support/ActionResult.java; git diff cbf498e9f93c0fc7164c960989beb26b901d0b76 --stat -- src tests compile.sh run.sh test.sh; git tag --list stage-1; rg --files tests -g '*Test.java' | wc -l`
>
> </details>
</details>

The **test-notes alignment is fixed**: README now accurately describes its contents, and the commands and final metrics agree. Application code, tests and scripts are unchanged from the verified commit, so I didn’t rerun them.

A few README inconsistencies remain:

| Location | Issue | Suggested wording |
|---|---|---|
| [Line 132](./Restaurant-Rush-Group-Assignment/README.md:132) | Lists `ORDERED`, but the enum and contracts use `PLACED`. | Replace `ORDERED` with `PLACED`. |
| [Line 126](./Restaurant-Rush-Group-Assignment/README.md:126) | Calls TurnManager the “victory evaluator”; `GameController.isVictory()` owns that logic. | “Turn lifecycle, arrival clock, automatic events.” |
| [Line 150](./Restaurant-Rush-Group-Assignment/README.md:150) | Says **all** Section 9 items are closed, but the contracts retain the `acceptOrder()` status question and exception discrepancies. | “The following Stage 1 design decisions have been agreed.” |
| [Line 155](./Restaurant-Rush-Group-Assignment/README.md:155) | Describes the exception policy as universally implemented despite existing `NullPointerException` checks. | Label it the **agreed policy**, with a reference to Section 9’s implementation discrepancies. |
| [Line 92](./Restaurant-Rush-Group-Assignment/README.md:92) | “Covering all units, contracts, and fixtures” overstates coverage. There are 16 suites, but that doesn’t establish complete coverage. | “16 standalone unit and integration test suites.” |

The local `stage-1` tag is still absent, consistent with the test notes. No files changed.

> Okay all the PRs from my team have been merged, and we have checked through everything on our side. Everything should be done for stage 1 now. Can you run one final check to see if all the stage 1 requirements are satisfied?

<details><summary>4 previous messages</summary>

> I’ll check the merged code, tests, documentation and Git tags against the Stage 1 brief, then run the full suite and the victory/defeat fixtures from a clean export. I won’t change files, commit or push.
>
> <details><summary>Ran 3 commands</summary>
>
> - Ran `git status --short; git log -4 --oneline; git tag --list; rg --files -g AGENTS.md -g '*.md'; cat compile.sh test.sh run.sh; cat src/restaurantrush/entity/Restaurant.java; cat docs/test-notes.md`
> - Ran `set -eu
> review_dir=$(mktemp -d /private/tmp/restaurant-final.XXXXXX)
> printf '%s' "$review_dir" > /private/tmp/restaurant-final-current
> git archive HEAD | tar -x -C "$review_dir"
> cd "$review_dir"
> ./test.sh
> ./run.sh < tests/fixtures/m5/victory.txt > victory.log
> ./run.sh < tests/fixtures/m5/defeat.txt > defeat.log
> python3 - <<'PY'
> from pathlib import Path
> import re
> for name in ('victory','defeat'):
>  s=Path(name+'.log').read_text()
>  assert [int(n) for n in re.findall(r'End of turn (\d+)\n',s)]==list(range(1,19))
>  assert 'Turn 19' not in s and 'Input ended.' not in s
>  print(name, s[s.rfind('\nVictory\n')+1:] if name=='victory' else s[s.rfind('\nDefeat\n')+1:])
> PY`
> - Ran `cat src/restaurantrush/control/TurnManager.java; cat src/restaurantrush/control/GameController.java; rg -n 'private static void test|void test' tests/restaurantrush/entity/RestaurantTest.java; rg -n 'VIP|Critic|Host|Combo|HappyHour|UnsupportedOperationException|java.net|http' src; rg -n 'closed all|ORDERED|17|discrep|unspecified|Cashier|waiter|phase' README.md docs/class-contracts.md; git diff ead385c HEAD --stat -- src tests; /usr/libexec/java_home -V`
>
> </details>
>
> All 17 test suites passed, and both gameplay fixtures completed exactly 18 turns with the expected results. The three Restaurant consistency issues from the earlier review are fixed. I’m checking the remaining release requirement: whether `stage-1` exists locally or on the remote.
>
> <details><summary>Ran 2 commands</summary>
>
> - Ran `git ls-remote --tags origin refs/tags/stage-1 'refs/tags/stage-1^{}'; sed -n '1,200p' tests/restaurantrush/entity/RestaurantTest.java; git status --short`
> - Ran `git ls-remote --tags origin refs/tags/stage-1 'refs/tags/stage-1^{}'`
>
> </details>
</details>

**Stage 1’s functional requirements pass my checks, but the required `stage-1` Git tag is still missing—both locally and on `origin`.** That is the remaining Section 4.1 requirement before calling Stage 1 complete.

I verified commit `7521dd3` from a clean export:

| Check | Result |
|---|---|
| Compilation and all 17 test suites | Passed |
| Base configuration, arrivals and deterministic menu choices | Matches brief |
| Staff limits, serving/eating/payment delays | Passed reviewed tests |
| Satisfaction, abandonment, table release and revenue accounting | Passed |
| Invalid-input retry behaviour | Passed CLI integration tests |
| Required state/results displays | Present |
| Stage 2/3 features excluded | Confirmed |
| Reproducible runs documented | Present in `docs/test-notes.md` |

Both production CLI fixtures completed **exactly 18 turns**:

| Fixture | Outcome | Paid | Revenue | Average paid satisfaction |
|---|---|---:|---:|---:|
| Victory | Victory | 5 | $66.00 | 79.20 |
| Defeat | Defeat | 0 | $0.00 | N/A |

The three previously reported Restaurant consistency issues are fixed, with passing regression tests.

Minor documentation caveats remain: README still claims “all” open items are closed and complete test coverage, while the contracts retain clarifications. These do not block Stage 1 gameplay.

Verification used JDK 21 with `--release 17`; a Java 17 runtime was not separately tested. This review covers Stage 1, not the final three-stage report/video/declaration submission.

No files changed, commits made or tags created.

> i've implemented the stage 3 combomeal feature, where salad + pasta costs $24.30 and requires 3 chef actions. customer 4 and the critic can both select it. all 20 test suites are passing so far.
>
> can you help add 3 more tests:
>
> 1. try serving the combo after only 2 chef actions. it should fail without consuming the waiter's turn. after the 3rd chef action, serving should succeed on the next turn.
> 2. test rounding down: two 3-cent items should give a discounted combo price of 5 cents.
> 3. test that either combo component being null throws an `IllegalArgumentException`.
>
> please follow the existing test conventions, run the full suite, and commit the changes on the current branch. no need to push yet.

<details><summary>4 previous messages</summary>

> I’ll check the ComboMeal code and test conventions, add the three requested cases, run the full suite, and commit the changes on the current branch without pushing.
>
> <details><summary>Edited a file, ran 3 commands</summary>
>
> - Ran `git status --short; git branch --show-current; rg --files -g AGENTS.md -g '*Combo*' -g '*Test.java'; cat src/restaurantrush/entity/ComboMeal.java; cat tests/restaurantrush/entity/ComboMealTest.java; cat tests/restaurantrush/control/GameControllerTest.java`
> - Ran `cat tests/restaurantrush/integration/Stage2IntegrationTest.java; cat src/restaurantrush/control/TurnManager.java; rg -n 'Combo|chooseItem|menu.add' src/restaurantrush/entity/*.java; cat test.sh; for p in /Users/yangxu/AGENTS.md /Users/yangxu/code/AGENTS.md; do if test -f "$p"; then cat "$p"; fi; done`
> - Wrote `./Restaurant-Rush-Group-Assignment/tests/restaurantrush/entity/ComboMealTest.java` (+109 -0)
> - Ran `./test.sh
> git diff --check
> git diff --stat`
>
> </details>
>
> All three new cases pass, and compilation and all 20 test suites pass. The serving test uses real game components: it checks that early service preserves the Waiter’s choice, the third Chef action makes the combo READY, and service succeeds on the following turn. I’ll commit the test-only change now.
>
> <details><summary>Ran a command</summary>
>
> - Ran `git add tests/restaurantrush/entity/ComboMealTest.java
>  git commit -m "test: cover combo serving timing rounding and null components"
> git status --short
> git log -1 --oneline`
>
> </details>
</details>

Added the three tests in `ComboMealTest`:

- Early service fails without consuming the Waiter’s choice; service succeeds the turn after the third Chef action.
- Two 3-cent components round down to 5 cents.
- Either null component throws `IllegalArgumentException`.

Compilation and all **20 test suites passed**. No application code changed.

Committed as **`8254a03`** on `feat/stage-3-combo-meal`. Working tree is clean; nothing pushed.

> What other tests can I check for?

For ComboMeal, I’d prioritise these:

1. **Payment uses the combo bill correctly.**
   Run a combo customer through serving, eating and payment. Verify revenue increases by exactly **2430 cents**, paid count increases once, both statuses become `PAID`, and the table is released. A repeated payment must change nothing.

2. **Abandonment cancels a partially prepared combo.**
   Let a customer reach zero satisfaction after one or two Chef actions. Verify the order is `CANCELLED`, the table is freed, revenue stays unchanged, and further cooking or serving fails without consuming the staff choice.

3. **Cancellation after all three preparation units.**
   A `READY` combo is still unserved food. If its customer abandons, it must become `CANCELLED` even though remaining units are zero. `isReady()` must then return false.

4. **Both intended customers select the combo.**
   Check customer 4 and the Critic through the actual order-taking flow. Verify each gets the combo, its bill is 2430 cents and it starts with three preparation units. This catches selection rules that work in isolation but fail during ordering.

5. **Chef availability remains enforced.**
   Attempt two preparation actions in one turn. The second must fail without reducing remaining units again.

6. **Component order does not affect price or preparation.**
   Salad + Pasta and Pasta + Salad should both cost 2430 cents and require three units. Their display names may differ.

Your existing rounding-up, rounding-down and null checks already cover useful constructor boundaries. **Payment and abandonment integration would add the most value next.**

Once Happy Hour is implemented, add price-locking tests: an eligible combo should cost **1944 cents**, and activating or ending Happy Hour must not alter an already-placed order’s bill.

> Can you help me implement these tests too

<details><summary>2 previous messages</summary>

> I’ll add the six applicable ComboMeal tests using the existing conventions, then run the full suite. I’ll check whether Happy Hour is implemented before adding its pricing tests. I won’t commit or push unless you ask.
>
> <details><summary>Edited a file, ran 2 commands</summary>
>
> - Ran `git status --short; cat tests/restaurantrush/entity/ComboMealTest.java; cat src/restaurantrush/entity/CriticCustomer.java; cat src/restaurantrush/staff/Waiter.java; rg -n 'Happy|happy|discount|Price|bill' src/restaurantrush; cat src/restaurantrush/entity/Customer.java | head -55`
> - Wrote `./Restaurant-Rush-Group-Assignment/tests/restaurantrush/entity/ComboMealTest.java` (+120 -0)
> - Ran `./test.sh
> git diff --check
> git diff --stat
> git status --short`
>
> </details>
</details>

Added coverage in [ComboMealTest.java](./Restaurant-Rush-Group-Assignment/tests/restaurantrush/entity/ComboMealTest.java) for:

- Full payment lifecycle and duplicate-payment rejection.
- Abandonment after 1, 2 or 3 Chef actions.
- Rejected cooking/serving preserving staff availability.
- Customer 4 and Critic selection through actual ordering.
- Rejecting a second Chef action in the same turn.
- Reversed components retaining the same price and preparation units.

**Compilation and all 20 test suites passed.** No bugs found or application code changed.

Happy Hour tests are deferred because that feature isn’t implemented yet. Changes are uncommitted; nothing pushed.
