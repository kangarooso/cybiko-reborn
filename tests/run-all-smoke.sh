#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
javac -d "$tmp" app/src/main/java/com/cybikoreborn/KeyboardBridge.java app/src/main/java/com/cybikoreborn/MonochromeFrame.java tests/KeyboardBridgeSmoke.java tests/MonochromeFrameSmoke.java
java -cp "$tmp" KeyboardBridgeSmoke
java -cp "$tmp" MonochromeFrameSmoke

javac -d "$tmp" app/src/main/java/com/cybikoreborn/NvramStore.java tests/NvramStoreSmoke.java
java -cp "$tmp" NvramStoreSmoke
