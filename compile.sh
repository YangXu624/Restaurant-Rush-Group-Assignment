#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$PROJECT_DIR"

# This directory contains generated build output only.
rm -rf build/classes
mkdir -p build/classes
find src -name '*.java' -type f | sort > build/sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -d build/classes @build/sources.txt
printf '%s\n' 'Compilation succeeded.'
