# Restaurant Rush — Stage 2

Deterministic Java CLI restaurant-management game for SC2002. **Stage 2 is now the default.** The existing `stage-1` tag and historical Stage 1 fixtures remain unchanged; there is no stage selector.

## Play and verify

Use JDK 17 or newer and a POSIX shell. No external libraries are required.

```sh
./compile.sh
./run.sh
./test.sh
./run.sh < tests/fixtures/stage2/victory.txt
./run.sh < tests/fixtures/stage2/defeat.txt
```

Waiter commands: `seat <customerId> <tableId>`, `order <customerId>`, `serve <customerId>`, `wait`.
Chef commands: `cook <customerId>`, `wait`. Invalid requests retry the same choice. EOF stops without inventing a completed turn or outcome.

## Rules

Exactly 18 turns, two tables, one Waiter choice followed by one Chef choice per turn. Arrivals occur on turns 1, 4, 7, 10, 13 and 16, with IDs 1–6.

| Type | Arrivals | Waiting penalty | Dish | Bill |
| --- | --- | --- | --- | --- |
| Regular | 1, 3, 4, 6 | 8 | Cyclic by ID | Full price |
| VIP | 2 on turn 4 | 5 | Cyclic by ID | 10% discount, half-up to cents |
| Critic | 5 on turn 13 | 12 | Highest price; first maximum wins ties | Full price |

The fixed menu is Salad (900 cents, 1 unit), Burger (1500 cents, 1 unit), Pasta (1800 cents, 2 units). Only WAITING/SEATED customers lose satisfaction, clamped at zero. Zero-satisfaction unserved customers abandon and free their table.

Automatic phases remain eating → at most one Cashier payment → penalties → abandonment. Served on turn t means ready no earlier than t+1 and paid no earlier than t+2. Bills are locked at order placement and never recalculated for payment or display.

Victory after completion requires at least four payments, 4500 cents revenue and average paid satisfaction at least 60. The final fixtures produce **5 paid, $67.50, 74.80 average** for victory and **0 paid, $0.00, one departure** for all-wait defeat. See [derivation and evidence](docs/test-notes.md).

## Architecture and ownership

Main wires shared collaborators. CLI parses commands and formats immutable snapshots. GameController (M4) checks availability, delegates actions and evaluates victory. TurnManager (M4) owns arrivals and turn sequencing. Restaurant (M2) coordinates collections, tables, service, payments and abandonment. Customer/subclasses and Order (M1) own behavior and lifecycle. Staff/ActionResult (M3) execute operations and return feedback. Snapshot/view records, CLI and integration (M5) expose historical customers, their type and locked bills without mutable domain references.

See [current contracts](docs/class-contracts.md) and [Stage 2 integration diagram](docs/stage2_integration.svg) ([editable draw.io](docs/stage2_integration.drawio)). The `stage1_class_diagram.*`, [Stage 1 contracts](docs/stage1-class-contracts.md), [Stage 1 evidence](docs/stage1-test-notes.md) and `tests/fixtures/m5/` are historical.

The suite has 19 standalone test classes, including real Stage 2 arrival/resolution and fixture tests. CI targets Temurin 17, runs the full suite and Stage 2 fixture smoke runs. Local verification and limitations are recorded in test notes. This work follows the supplied integration plan; the original assignment and separate Stage 2 guide were unavailable. Host, ComboMeal and Happy Hour are not implemented.
