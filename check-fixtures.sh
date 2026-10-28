#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$PROJECT_DIR"

failed=0

# Each fixture must produce every expected line exactly as printed.
expect_lines() {
  fixture=$1
  shift
  output=$(sh run.sh < "tests/fixtures/m5/$fixture.txt")
  for line in "$@"; do
    if ! printf '%s\n' "$output" | grep -Fxq -- "$line"; then
      printf 'FAIL %s: missing line: %s\n' "$fixture" "$line"
      failed=1
    fi
  done
  printf 'Checked %s\n' "$fixture"
}

expect_lines victory \
  'Victory' \
  'Revenue: $66.00' \
  'Average paid satisfaction: 79.20' \
  'Paid: 5 | Served: 5 | Unhappy departures: 0' \
  'Turns used: 18'

expect_lines defeat \
  'Defeat' \
  'Revenue: $0.00' \
  'Average paid satisfaction: N/A' \
  'Paid: 0 | Served: 0 | Unhappy departures: 2' \
  'Turns used: 18'

if [ "$failed" -ne 0 ]; then
  exit 1
fi
printf '%s\n' 'All fixture checks passed.'
