#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$PROJECT_DIR"

"$PROJECT_DIR/compile.sh"

rm -rf build/test-classes
mkdir -p build/test-classes

find tests -name '*.java' -type f | sort > build/test-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -cp build/classes -d build/test-classes @build/test-sources.txt

if [ $# -gt 0 ]; then
  exec java -cp "build/classes:build/test-classes" "$1"
else
  # Discover and run each test class
  for file in $(find tests -name '*Test.java' -type f | sort); do
    class_name=$(echo "$file" | sed -e 's|^tests/||' -e 's|\.java$||' -e 's|/|.|g')
    printf "Running %s...\n" "$class_name"
    java -cp "build/classes:build/test-classes" "$class_name"
  done
fi
