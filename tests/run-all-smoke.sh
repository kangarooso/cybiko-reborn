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

javac -d "$tmp" app/src/main/java/com/cybikoreborn/FirmwareInspector.java tests/FirmwareInspectorSmoke.java
java -cp "$tmp" FirmwareInspectorSmoke

# Emulation core: upstream sources from the pinned submodule (same whitelist Gradle uses).
upstream=third_party/cybiko-java-emulator/emulator/src/main/java/com/github/daberkow
if [ ! -d "$upstream" ]; then
  echo "Upstream emulator missing: run 'git submodule update --init --recursive'" >&2
  exit 1
fi
core_src=()
while read -r name; do
  case "$name" in ''|'#'*) continue;; esac
  core_src+=("$upstream/$name.java")
done < cybiko-core/upstream-core-files.txt
mapfile -t own < <(find cybiko-core/src/main/java -name '*.java')
javac --release 17 -nowarn -d "$tmp/core" "${core_src[@]}" "${own[@]}" tests/ClassicV1MachineSmoke.java
java -cp "$tmp/core" ClassicV1MachineSmoke
