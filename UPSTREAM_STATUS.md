# Cybiko Reborn v0.5 — upstream integration findings

Verified upstream: https://github.com/daberkow/cybiko-java-emulator

- Upstream states Java 21+ and a desktop Swing UI; this cannot be added to Android as an ordinary Java dependency without separating platform-independent components.
- Upstream Classic V1 firmware filenames: `cyrom112.bin` and `flash_v1246.bin` (not included).
- Upstream CLI: `--machine v1 cyrom112.bin flash_v1246.bin --nvram localmemory.bin`.
- Upstream has MIT license, but the repository also says its emulation derives from MAME. Inspect provenance of each reused component and dependencies before redistributing a port.
- No source downloaded or integrated in this version: environment cannot resolve github.com. Do not represent the demo screen as CyOS.
- `tools/import_upstream.py` validates and stages a real upstream ZIP for a future port, without fetching firmware or pretending an emulator is present.
- Next engineering work: isolate CPU/peripherals from Swing, adapt Java 21 features to Android-compatible toolchain, implement frame/key callbacks and test real boot using user-provided ROMs.
