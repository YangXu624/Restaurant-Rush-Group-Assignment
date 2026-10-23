# Stage 1 reproducible gameplay tests

Verified on 23 October 2026 at commit `cbf498e9f93c0fc7164c960989beb26b901d0b76` for assignment Section 4.1. Existing tests and fixtures were reused without changes.

## Reproduce the gameplay runs

Prerequisites: JDK 17 or newer with `javac` and `java` on PATH, and a POSIX shell (macOS, Linux or WSL). Run from the repository root. Verification used macOS and Temurin JDK 21.0.12; compilation targets Java 17 using `--release 17`. Execution on a Java 17 runtime was not separately verified.

```sh
./run.sh < tests/fixtures/m5/victory.txt
./run.sh < tests/fixtures/m5/defeat.txt
```

`run.sh` compiles production sources and launches `restaurantrush.Main`, which wires the real CLI, controller, turn manager, staff and entities. No test doubles or test classes are used in these two runs. Each fixture contains 36 commands: one Waiter choice followed by one Chef choice for each of 18 turns. Food choices and arrivals are deterministic; no seed or network access is required.

- **Victory:** `tests/fixtures/m5/victory.txt` schedules seating, ordering, cooking and serving. The first five customers pay; customer 6 remains seated with an unfinished Pasta order.
- **Defeat:** `tests/fixtures/m5/defeat.txt` chooses `wait` for both staff every turn. Nobody is served or pays; two customers abandon.

## Expected and observed results

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

## Existing automated checks

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

## Clean-checkout and tag verification

The working tree was clean before this documentation was added. A fresh temporary export of the verified commit was created with `git archive`; all five commands above passed there without pre-existing build output, IDE settings or untracked files. Scripts are tracked as executable and both input fixtures are tracked. This verifies reproducibility from the committed files; the export did not contain `.git` metadata.

No local `stage-1` tag existed when checked with `git tag --list stage-1`, so tagged-version verification was not possible. No remote tags were fetched and no tag was created. Repeat these commands against the eventual `stage-1` checkout before submission.

## Coverage limits

The two gameplay fixtures demonstrate one victory and one defeat, but do not separately demonstrate a CLI defeat caused solely by each individual target. Those independent threshold checks are direct controller tests using supplied statistics, as described above. No mismatch was observed in the executed scenarios.
