# Marcus Lau: Chat History (GAI Assistance Records)

This file combines two records for the Restaurant Rush project (SC2002 group assignment, Java CLI game):

- **Part 1:** Record of GAI Assistance: Determinism Check
- **Part 2:** M3 staff actions: chat export

---

# Part 1: Record of GAI Assistance: Determinism Check

**Tool:** Claude Code (Claude Haiku 5.5), desktop app, Code tab
**Date:** 2026-11-14
**Project:** SC2002 Restaurant Rush Group Assignment (Java CLI game)
**Scope:** One question about the assignment rubric and whether the game's outputs are deterministic. No code was changed.

## Conversation summary

### 1. Rubric and determinism question

**User prompt:** Asked whether the game must be deterministic according to the rubric, so that its output matches the expected results. Stated the expected results: the victory run should end with $66.00 revenue, 5 paid, and 79.20 average satisfaction, and the defeat run should show 0 paid and 2 departures.

**What the assistant did:**
- Converted the rubric `.docx` to plain text (macOS `textutil`) and read it.
- Found that the rubric requires the food choice to use "a documented deterministic rule or seeded randomness", and that the test notes must include "one reproducible victory run and one reproducible defeat run".
- Searched the repository for randomness, fixtures and expected values. Found:
  - Dish assignment `menu.get((customerId - 1) % 3)`, documented in `README.md`.
  - Fixed command scripts `tests/fixtures/m5/victory.txt` and `defeat.txt`.
  - Expected results recorded in `docs/test-notes.md`.
- Attempted to run `./run.sh`. It failed because the default `javac` is JDK 11 and the code needs Java 17 or later.
- Located a JDK 25 `javac` inside the VS Code Java extension and compiled the sources into the session scratch directory (not the repository `build/` folder).
- Ran both fixtures and compared outputs.

**Results:**
- Victory fixture: Revenue $66.00, Paid 5, Served 5, Average paid satisfaction 79.20, Unhappy departures 0, outcome Victory.
- Defeat fixture: Revenue $0.00, Paid 0, Served 0, Unhappy departures 2, outcome Defeat.
- Two runs of the victory fixture produced identical output.

**Issues noted (not fixed):**
- `./run.sh` requires JDK 17 or later; the default JDK on the machine is 11.
- No `stage-1`, `stage-2` or `stage-3` git tags exist yet. The rubric requires them.

### 2. Request for this record

**User prompt:** Create a Markdown file of this chat history for the AI declaration submission.

**What the assistant did:** Wrote this file, summarizing the conversation above. It is a summary of the exchange, not a verbatim transcript.

## Declaration note

The assistant's work in this conversation was limited to reading the rubric and repository, compiling and running the existing fixtures, and summarizing results. The student's own work, including the design, implementation and report, is not described here.

## Later session: CI and fixture check (2026-11-14)

**Tool:** Claude Code (Claude Haiku 5.5), desktop app, Code tab
**Scope:** GitHub Actions advice, a new fixture-check script, and a commit and push on a feature branch. Changes were made in this session (see below).

### 3. GitHub Actions discussion

**User prompts:** Asked whether GitHub Actions would suit the project and how to set it up step by step. Asked what it adds, what GitHub already checks on pull requests, what other Actions could be integrated, and which would benefit the project most.

**What the assistant did:**
- Read `compile.sh`, `test.sh` and `run.sh`, and checked how test failures are reported (`AssertionError` plus `set -eu`, so a failing test gives a non-zero exit).
- Explained that a custom workflow is simpler than the starter templates, which target Maven or Gradle, and suggested a workflow that runs compile, tests and fixture runs.
- Gave the workflow YAML and the steps to create it in the GitHub editor, plus suggested commit message wording.
- Explained the limits of branch protection and required status checks, and listed other possible Actions (CodeQL, static analysis, artifacts, release on tag, Dependabot, coverage). Recommended a project-specific fixture check first, then CodeQL.

No repository files were changed in this part. The assistant's advice was given in chat.

### 4. Fixture check

**User prompts:** Asked how to add a fixture check that compares outputs with expected results. Later said JDK 17 was installed, asked to go ahead, asked for the change to be committed, and asked for the branch to be pushed.

**What the assistant did:**
- Found that the default JDK (11) could not compile the project, and asked the user to install Temurin 17. The user ran the Homebrew install.
- Ran the victory and defeat fixtures with JDK 17. Both exited 0 and matched the values in `docs/test-notes.md`.
- Created `check-fixtures.sh` in the repository root. It runs each fixture and checks that every expected line appears exactly, and exits 1 if one is missing. Made it executable.
- Ran the script (passed). Tested a failure by running a temporary copy with an incorrect expected revenue (failed with exit 1), then deleted the copy.
- Created branch `ci/fixture-check` and committed only `check-fixtures.sh` as `3380bd3`, with a commit message written by the assistant. Pushed the branch to `origin` at the user's request.

**Not done:** The workflow file `.github/workflows/ci.yml` was not created or changed. The pull request was not opened. Nothing was merged. The user would need to add a step calling `sh check-fixtures.sh` to the workflow.

**Declaration note for this part:** The assistant wrote `check-fixtures.sh` and the commit message. The user chose to add the fixture check and approved the commit and push. The assistant gave the GitHub Actions advice in section 3.

---

# Part 2: M3 staff actions: chat export

Session on branch `feat/7-staff-actions` (Restaurant Rush, M3 Staff Operations). Tool output is condensed to what mattered. Nothing has been committed or pushed.

---

## User: task brief

Context: M3 (Staff Operations) on a 4-person Java team project. Source of truth is `docs/class-contracts.md`. Yang Xu owns Order, MenuItem and Customer. Kai Xuan owns Restaurant. Nickolas (M4) owns GameController and TurnManager. Goal: implement ActionResult, Waiter, Chef and Cashier so they satisfy the contract and plug into teammates' code without changing it.

Workflow, stopping to check after each phase:
1. Setup: GitHub issue with a checklist of the four classes, sync main, create branch `feat/<issue#>-staff-actions`.
2. Read before writing: contract plus public methods of Restaurant, Order and Customer. Say which rules teammates already enforce, before writing any code.
3. Implement in dependency order, ActionResult first.
4. Test: write tests, compile and run them.
5. Review: simplify without breaking any contract rule.
6. Ship: commit, push, draft PR text.

Design principles: DRY (delegate, don't re-check), single responsibility (staff classes do not enforce one-task-per-turn; that is M4's), validate then mutate in `takeOrder`, encapsulation and immutability for ActionResult, expected failures are return values and exceptions are for programmer errors, state changes only on success (Cashier `lastPaidTurn`), YAGNI.

Testing: plain `main()` plus assert helpers in the style of OrderTest. Cover success paths, failure paths with state unchanged, and edge cases (Pasta needs 2 cook actions, one payment per turn, no payment on the turn a customer becomes ready, oldest ready customer pays first, ties go to the lower ID with arrival order unable to pass the test by accident).

Constraints: don't modify teammates' files. Say which tests could and couldn't run (user believed the machine had Java 11). One commit per class plus one for tests, conventional messages, authored by the user only (no AI co-author line). Don't push until told.

Output wanted: after each class a walkthrough linking code to contract clauses and the Java concept involved. After review, before/after for each change plus what was left alone and why. At the end, PR title and description (Closes #N, per-class summary, test coverage, notes for M4 on calling `cashier.collectOne` each turn). Short, friendly reviewer replies naming the fix and commit hash, not posted.

---

## Assistant: Phase 1 (setup)

Checked the repo: `main` was 3 commits behind origin, `gh` was not logged in, and the four classes already contained code (no `TODO`s, no `UnsupportedOperationException`). Fast-forwarded main. Because `gh` was unavailable (and you prefer doing GitHub steps yourself), I drafted the issue instead.

**Title:** `M3: Staff actions (ActionResult, Waiter, Chef, Cashier)`

```markdown
Owner: Marcus (M3, Staff Operations). Source of truth: docs/class-contracts.md

Make the staff classes satisfy the contract and plug into Restaurant, Order and Customer without changing teammates' files.

- [ ] ActionResult: immutable, private constructor, static ok()/fail(), rejects a null message
- [ ] Waiter: seat, takeOrder (validate first, then create, link and register), serve
- [ ] Chef: prepareOneUnit (Pasta takes 2 cook actions)
- [ ] Cashier: collectOne, at most one payment per turn, oldest ready customer first, ties go to the lower ID, no payment on the turn a customer becomes ready
- [ ] Tests in the OrderTest style: success paths, failure paths that leave state unchanged, edge cases

Notes for M4: staff classes don't enforce one task per turn; GameController/TurnManager do. Call cashier.collectOne(restaurant, turn) once each turn.
```

## User

7

## Assistant

Created branch `feat/7-staff-actions` from the synced main. Untracked docs files untouched, nothing committed.

## User

Start Phase 2

## Assistant: Phase 2 (read before writing)

Read the whole contract and the sources of Restaurant, Order, Customer and the four M3 classes. Ran `./test.sh`: **OpenJDK 17.0.21** (not 11), so the record-based teammate files compile too, and all 23 test classes passed.

The four classes were already implemented in Stage 1 (commits `d96ffb2`, `950f823`, `d7b1d38`, `ea2edf3`).

### Rules teammates already enforce

| Owner method | Rules it already enforces | M3 code |
|---|---|---|
| `Restaurant.seat` | Customer arrived, queued and `WAITING`; table exists and is free; validates before mutating | `Waiter.seat` only delegates |
| `Restaurant.serve` | Positive turn; customer `SEATED`, not yet served, at a table, has a registered `READY` order; marks order and customer served; increments `servedCount` | `Waiter.serve` only delegates |
| `Order.canPrepare` / `prepareOneUnit` | All preparation-status rules; Pasta's two steps come from `remainingUnits` | Chef asks `canPrepare()` |
| `Order(...)` constructor | Positive ID, non-null item, positive units, non-negative bill | Not re-checked |
| `Customer.chooseItem` | Non-empty menu; `(id-1) % size` | Not re-checked |
| `Restaurant.oldestEligiblePayer` | `READY_TO_PAY`, ready turn earlier than `turn`; earliest ready turn, then lowest ID | Cashier does no sorting |
| `Restaurant.recordPayment` | Every payment check; false with no change on failure | Cashier uses its boolean |

Two checks that look like duplication but are required:
- The start of `Waiter.takeOrder` (customer `SEATED`, no order). `Customer.acceptOrder` and `Restaurant.registerOrder` throw instead of returning `fail`, and `registerOrder` only runs after the customer is linked. Contract line 308 asks for the early checks.
- `Cashier`'s `lastPaidTurn == turn`. That is the per-turn payment limit, which belongs to Cashier (line 191). It is different from M4's one-task-per-turn limit.

### Gaps found

1. `ActionResult` null message throws `NullPointerException`, while the contract's error policy says `IllegalArgumentException` (§9 lists this as a known, unfixed mismatch).
2. No test proves `lastPaidTurn` only changes after a successful payment.
3. Some failure tests probably assert only the result, not full state before and after.
4. `r.findOrder(customerId) != null` in `takeOrder` is probably unreachable through public methods, but the contract line 308 asks for it. Raise in Phase 5 rather than delete.

## User

Rewrite from scratch

## Assistant: Phase 3 (implement)

Rewrote `ActionResult`, `Waiter`, `Chef` and `Cashier` in dependency order. Same public methods and the same feedback messages, because M5's tests check the exact text (e.g. `"Customer 1 ordered Salad."`, `"cannot be cooked (SERVED)"`). Kept `NullPointerException` for a null message: the rest of the codebase uses it, and the existing `ActionResultTest` expects it. `./test.sh` compiled and all 23 test classes passed. Diff: 4 files, +28 / -25. Nothing committed.

### ActionResult
| Code | Contract clause | Java concept |
|---|---|---|
| `private ActionResult(...)` | Results come only from `ok`/`fail` | Private constructor |
| `static ok()` / `static fail()` | §7 "Create a successful/failed result" | Static factory methods |
| `private final` fields, getters only | "exposes no mutators" | Immutability |
| `final class` | Same clause | Blocks mutable subclasses |
| `Objects.requireNonNull(message, ...)` | Programmer error rejected early | Fail-fast |

### Waiter
| Code | Contract clause | Java concept |
|---|---|---|
| `seat` / `serve` are one-line delegations | §5 Waiter: delegate, do not duplicate checks | Delegation (rule lives in Restaurant) |
| Step 1: customer arrived, `SEATED`, no order or registered order | Check customer and registry before linking | Guard clauses; short-circuit `\|\|` |
| Step 2: `chooseItem`, `calculateBillCents`, Happy Hour, `new Order(customerId, ...)` | Choose item, create order with customer's ID; Stage 3 Happy Hour | Polymorphism (VIP/Critic override `calculateBillCents`); nothing linked yet if something throws |
| Step 3: `acceptOrder` then `registerOrder` | Link, then register, one shared instance | Validate first, mutate last; order matters because `registerOrder` needs the customer to hold the order |

### Chef
| Code | Contract clause | Java concept |
|---|---|---|
| `o == null \|\| !o.canPrepare()` | Fail without mutation if null or not preparable | Short-circuit `\|\|` avoids a null dereference |
| No `OrderStatus` checks | Chef does not duplicate status checks | Ask the object, don't read its fields |
| `o.prepareOneUnit()` once; message from `isReady()` | Exactly one call; mention `READY` | Conditional `?:` |

### Cashier
| Code | Contract clause | Java concept |
|---|---|---|
| `private int lastPaidTurn = -1` | Initially -1 | Encapsulated state |
| `lastPaidTurn == turn` returns false | Already paid this turn | Guard clause |
| `r.oldestEligiblePayer(turn)` | Restaurant picks payer | Delegation |
| `payer == null \|\| !r.recordPayment(...)` | No one eligible, or payment fails, returns false | Short-circuit `\|\|` |
| `lastPaidTurn = turn` only after success | Updated only after successful collection | State changes only on success |

---

## Next

Phase 4 (tests) has not started. Planned: add a test that a failed `collectOne` leaves the turn's payment slot open, tighten failure tests to compare full state before and after, and confirm the tie-break test cannot pass by arrival order. Then Phase 5 (review) and Phase 6 (commits, push on your say-so, PR text). Commits will follow your rule: authored by you only, no AI co-author line, regardless of any default attribution reminder.
