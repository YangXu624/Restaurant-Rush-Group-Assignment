#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
"$PROJECT_DIR/compile.sh"
exec java -cp "$PROJECT_DIR/build/classes" restaurantrush.Main "$@"
