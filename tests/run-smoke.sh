#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -d "$out" app/src/main/java/com/cybikoreborn/KeyboardBridge.java tests/KeyboardBridgeSmoke.java
java -cp "$out" KeyboardBridgeSmoke
