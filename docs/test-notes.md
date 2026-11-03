# Stage 2 verification — 3 November 2026

Integration based on `c125c3ef024d54de6dad863d0744591ccf237181`, with local uncommitted changes. No new commit or tag was created. Original assignment and separate Stage 2 handoff guide were unavailable; verification uses the supplied integration plan and real implementation.

## Exact victory fixture

`tests/fixtures/stage2/victory.txt` is replayed through the real CLI and Main, without doubles. The first five customers pay:

| ID/type | Arrival | Seat | Order | Cook turns | Serve | Ready | Pay | Paid satisfaction | Bill cents |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 Regular | 1 | 1 | 2 | 2 | 3 | 4 | 5 | 84 | 900 |
| 2 VIP | 4 | 4 | 5 | 5 | 6 | 7 | 8 | 90 | 1350 |
| 3 Regular | 7 | 7 | 8 | 8, 9 | 10 | 11 | 12 | 76 | 1800 |
| 4 Regular | 10 | 11 | 12 | 12 | 15 | 16 | 17 | 60 | 900 |
| 5 Critic | 13 | 13 | 14 | 14, 15 | 16 | 17 | 18 | 64 | 1800 |

Penalties apply from arrival through the turn before service. Satisfaction total is 374; 374/5 = 74.80. Bills sum to 6750 cents. This independently derived schedule matches the candidate result: **Victory, five paid, $67.50, 74.80 average, five served, zero departures, 18 turns**. Customer 6 arrives on turn 16 and seats on turn 18 with satisfaction 76 and no order/bill. Both tables were occupied at the start of turn 17; payment releases customer 4's table only at its end.

## Other fixtures

- `defeat.txt`: all wait. Customer 1 reaches zero on turn 13. VIP customer 2 has 25 satisfaction at completion; Critic customer 5 has 28. Result: **Defeat, zero paid/served, $0.00, N/A average, one departure, 18 turns**.
- `invalid-input.txt`: same final state and totals as victory despite retries.
- `abandonment.txt`: customer 1's unfinished order is cancelled, table freed, one departure, zero revenue.
- `eof-waiter.txt` / `eof-chef.txt`: unfinished turn 1; no automatic events, completion summary or final outcome.

## Commands and coverage

```sh
./compile.sh
./test.sh restaurantrush.integration.Stage2IntegrationTest
./test.sh
for f in victory defeat abandonment invalid-input eof-waiter eof-chef; do
  ./run.sh < tests/fixtures/stage2/$f.txt
done
git diff --check
```

The focused Stage 2 test checks scheduled type labels and penalties across all 18 turns, zero clamping for queued/seated customers of all types, cancellation/table release, exact service/eating/payment status timing, locked bills before and after payment, detached snapshots and duplicate-payment rejection. Existing suites retain lifecycle, availability, invalid-action, EOF, snapshot immutability and threshold checks. MainTest launches production Main; M5IntegrationTest wires real collaborators in-process. Focused M4 coordination tests still use doubles where explicitly documented.

Local execution: Temurin JDK 25.0.4.1, compilation with `--release 17`. Java 17 runtime was unavailable; no remote CI success is claimed. Final build, all 19 suites and all six production fixture runs passed. The CI definition retains Temurin 17 and existing checks, switching fixture smoke runs to Stage 2. Stage 1 evidence is preserved separately in [stage1-test-notes.md](stage1-test-notes.md).

## Diagram scope

The new stage2_integration.drawio and SVG share the same layout and describe Stage 2 relationships and extension points. The older Stage 1 draw.io/PNG pair is preserved as historical, not relabelled as current. A native draw.io PNG exporter was not installed, so no Stage 2 PNG was generated.
