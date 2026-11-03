# Stage 2 fixtures

Each clean full-game fixture has 36 accepted commands, one per staff choice per turn. `invalid-input.txt` additionally retries malformed and domain-invalid requests, and must reach the identical final state as victory. `abandonment.txt` places an unfinished order that is cancelled when customer 1 leaves. EOF fixtures stop during the first turn.

Victory deliberately postpones customer 4's service to turn 15 to seat customer 5 on turn 13, order/cook on turn 14, cook again on turn 15, and serve on turn 16. Customer 4 pays on turn 17; its table is available to seat customer 6 on turn 18. See [test notes](../../../docs/test-notes.md) for exact totals.
