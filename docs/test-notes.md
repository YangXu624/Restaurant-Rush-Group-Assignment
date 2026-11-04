# Reproducible gameplay tests

## Stage 2

Verified on 4 November 2026 at the Stage 2 gameplay merge (`f56f8f5`). From the repository root, with JDK 17 or newer and a POSIX shell, run:

```sh
./test.sh
./run.sh < tests/fixtures/m5/victory.txt
./run.sh < tests/fixtures/m5/defeat.txt
```

All 19 test suites passed. Both CLI runs completed 18 turns with these results:

| Fixture | Outcome | Paid | Served | Revenue | Average paid satisfaction | Unhappy departures |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| `victory.txt` | Victory | 5 | 5 | $67.50 | 74.80 | 0 |
| `defeat.txt` | Defeat | 0 | 0 | $0.00 | N/A | 1 |

In the victory run, look for customer 2 (VIP) on turn 4. Its Burger order locks at $13.50 on turn 5. Customer 5 (Critic) arrives on turn 13 and orders Pasta on turn 14. The Chef cooks it on turns 14 and 15, so the Waiter can serve it on turn 16. The defeat fixture uses `wait` throughout. Only customer 1 reaches zero satisfaction before the game ends.

## Stage 1 (historical)

Use the files at the `stage-1` tag to reproduce the results below. The victory fixture at that tag contains a different sequence of commands.

Verified on 24 October 2026 at commit `ead385c` on `main` (incorporating PR #20) for assignment Section 4.1. Existing tests and fixtures were reused without changes.

### Reproduce the gameplay runs

Prerequisites: JDK 17 or newer with `javac` and `java` on PATH, and a POSIX shell (macOS, Linux or WSL). Run from the repository root. Verification used macOS and Temurin JDK 21.0.12; compilation targets Java 17 using `--release 17`. Execution on a Java 17 runtime was not separately verified.

```sh
./run.sh < tests/fixtures/m5/victory.txt
./run.sh < tests/fixtures/m5/defeat.txt
```

`run.sh` compiles production sources and launches `restaurantrush.Main`, which wires the real CLI, controller, turn manager, staff and entities. No test doubles or test classes are used in these two runs. Each fixture contains 36 commands: one Waiter choice followed by one Chef choice for each of 18 turns. Food choices and arrivals are deterministic; no seed or network access is required.

- **Victory:** `tests/fixtures/m5/victory.txt` schedules seating, ordering, cooking and serving. The first five customers pay; customer 6 remains seated with an unfinished Pasta order.
- **Defeat:** `tests/fixtures/m5/defeat.txt` chooses `wait` for both staff every turn. Nobody is served or pays; two customers abandon.

### Expected and observed results

Expected values come from the existing `MainTest` and `M5IntegrationTest` assertions. Actual values below were read from fresh production CLI runs and matched those assertions.

| Metric | Victory expected | Victory actual | Defeat expected | Defeat actual |
| --- | --- | --- | --- | --- |
| Outcome | Victory | Victory | Defeat | Defeat |
| Completed turns | 18 | 18 | 18 | 18 |
| Paid customers | 5 | 5 | 0 | 0 |
| Revenue | $66.00 (6600 cents) | $66.00 (6600 cents) | $0.00 | $0.00 |
| Average paid satisfaction | 79.20 | 79.20 | N/A | N/A |
| Served customers | 5 | 5 | 0 | 0 |
| Unhappy departures | 0 | 0 | 2 | 2 |

Both processes exited successfully. Their transcripts contained exactly the sequential completion summaries `End of turn 1` through `End of turn 18`, one final outcome and `Turns used: 18`, with no turn 19 or premature end-of-input message.

Victory requires **paid count ≥ 4 AND revenue ≥ 4500 cents AND average paid satisfaction ≥ 60**, after turn 18. The victory run satisfies all three. The defeat run misses the count and revenue targets and has no paid-customer average; `N/A` is not a qualifying average. `M5IntegrationTest` additionally verified the victory snapshot's unrounded average as `79.2`.

### Existing automated checks

All three commands below were executed successfully during this verification. Each recompiles sources and tests before running the selected test class.

```sh
./test.sh restaurantrush.integration.MainTest
./test.sh restaurantrush.integration.M5IntegrationTest
./test.sh restaurantrush.control.GameControllerTest
```

| Test | Integration level and evidence |
| --- | --- |
| `MainTest` | Launches production Main in separate JVMs with fixture input. Checks final CLI metrics, successful exit, 18 completion summaries and no early EOF/turn 19. Includes victory, defeat and other existing fixtures. |
| `M5IntegrationTest` | Runs the real CLI and game collaborators in-process, wired by the test rather than Main. Checks fixture consumption, snapshots, customer/order/table states, final metrics and each completed-turn display. |
| `GameControllerTest` | Calls game classes directly, without CLI input. Its victory-boundary checks use supplied statistics via `StatisticsRestaurant` and a recording Cashier, not real payment gameplay. Confirms exact thresholds pass and independently lowering paid count to 3, revenue to 4499 cents or average to 59.99 fails. |

### Clean-checkout and tag verification

The working tree was clean before this documentation was added. A fresh temporary export of the verified commit was created with `git archive`; all five commands above passed there without pre-existing build output, IDE settings or untracked files. Scripts are tracked as executable and both input fixtures are tracked. This verifies reproducibility from the committed files; the export did not contain `.git` metadata.

No local `stage-1` tag existed when checked with `git tag --list stage-1`, so tagged-version verification was not possible. No remote tags were fetched and no tag was created. Repeat these commands against the eventual `stage-1` checkout before submission.

### Coverage limits

The two gameplay fixtures demonstrate one victory and one defeat, but do not separately demonstrate a CLI defeat caused solely by each individual target. Those independent threshold checks are direct controller tests using supplied statistics, as described above. No mismatch was observed in the executed scenarios.
