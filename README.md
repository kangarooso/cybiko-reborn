# Cybiko Reborn v1.3

**Android prototype, not a working CyOS emulator.**

New in v1.3: `.github/workflows/build-real-emulator.yml` builds the *real upstream Java emulator* and separately builds the Android prototype. See `REAL_EMULATOR_NEXT.md`. Neither workflow has been run.

# Cybiko Reborn — Android prototype v0.1

Android-first Java project designed for a Galaxy Fold, starting with a responsive purple Cybiko-inspired interface, a working on-screen keyboard test, and Android document-picker firmware import into app-private storage.

**Important:** This is a runnable Android *UI prototype*, not yet a running Cybiko emulator. Original CyOS cannot boot until the H8S CPU, LCD, keyboard and flash devices are integrated from the upstream emulator. No ROMs or upstream emulator code are bundled.

## Build
1. Install Android Studio and Android SDK 35.
2. Open this directory as a Gradle project. Allow Android Studio to sync/install Gradle 8.9 and Android Gradle Plugin 8.7.3 dependencies.
3. Connect Android phone with USB debugging, select it, and Run `app`.
4. Or run `gradle assembleDebug` with a suitable Gradle installation; APK appears under `app/build/outputs/apk/debug/`.

## Upstream emulator
https://github.com/daberkow/cybiko-java-emulator
License: MIT, copyright (c) 2026 Dan Berkowitz. Preserve upstream LICENSE when incorporating upstream code. Original project is Java 21 and uses Swing for desktop; Android needs a separate UI and adaptation of the emulation core.

Classic V1 firmware file names in upstream docs: `cyrom112.bin`, `flash_v1246.bin`. The current importer accepts a single file at a time; does not validate it as firmware or boot it.

## Next implementation milestones
- Extract non-Swing emulator CPU/peripheral logic to Android-compatible library, respecting MIT attribution and any third-party notices.
- Create framebuffer-to-Android Canvas adapter and keyboard matrix input adapter.
- Implement two-ROM selection/validation, NVRAM persistence, lifecycle and audio.
- Validate real Classic V1 boot on Galaxy Fold hardware.
- Add local multiplayer networking, then secure opt-in remote connectivity.

## Limitations
This project was authored without an Android SDK, Gradle runtime or external Git access in the build container, so an APK was not compiled or device-tested. Use Android Studio to build and validate.

## v0.2 firmware manager update
- Separate import actions for the Classic V1 `cyrom112.bin` boot ROM and `flash_v1246.bin` flash ROM.
- Persistent private storage under `files/classic-v1/`, SHA-256 digest display, empty/oversized file rejection, and incomplete-import cleanup.
- The import slots identify the expected file roles but **do not authenticate firmware contents**; successful import is not proof of a valid ROM.
- No original emulator source is bundled: upstream GitHub was inaccessible from the build environment. The project therefore **still does not boot CyOS**.
- This source update has not been compiled with Android SDK or device-tested. The next milestone is separating the upstream emulator's non-Swing core and connecting its LCD/keyboard interfaces.

## v0.3 display and input integration boundaries
- `LcdFrameView` provides a 160×100 monochrome framebuffer adapter to Android Canvas, with nearest-neighbor scaling and a deterministic diagnostic pattern. It is **not connected** to the upstream LCD peripheral.
- `KeyboardBridge` provides key press/release events for eventual CPU keyboard matrix integration. It currently powers only the existing keyboard demo, not original CyOS.
- Tap **LCD TEST** to see the pixel renderer. Tap **HOME** to return to the firmware status display.
- Upstream code could not be fetched due to DNS/network restrictions. No upstream emulation code or firmware is included; firmware boot remains unsupported.
- Android build was not run because Android SDK/Gradle are unavailable in this environment.

## v0.5 update

Added `UPSTREAM_STATUS.md` with verified Classic V1 boot requirements and `tools/import_upstream.py` for safely staging an upstream source archive. No original emulator engine is included yet; TRY BOOT continues to report this truthfully.

## v0.6 — Reproducible Android build pipeline

Added `.github/workflows/android-debug.yml`. Upload this directory to a GitHub repository and run **Actions → Build Android debug APK → Run workflow**; a successful build will provide an APK under workflow Artifacts. This is a *build path*, not evidence that the APK has been built or the original CyOS boots. The workflow is untested here because this environment has no Android SDK or GitHub network access.

Run `bash tests/run-smoke.sh` locally to verify the transport-neutral keyboard press/release contract with a Java compiler. This test does not exercise Android or emulation.

**Important:** The project still contains no original H8S CPU emulator or original ROMs. The upstream repository is MIT licensed, but code must be retrieved and adapted before CyOS boot is possible. Do not confuse successful UI compilation with original Cybiko emulation.

## v0.7 portable framebuffer work (October 2026)
- Added `MonochromeFrame`, an immutable, Android-independent 160x100 pixel frame representation.
- Wired Android `LcdFrameView` to use the same frame conversion path that is covered by JVM tests.
- Added a validation suite for frame dimensions, pixel values, immutability, ARGB conversion, and coordinate bounds.
- Run `bash tests/run-all-smoke.sh` using a local JDK; this does not require Android SDK.
- **Not yet emulation:** upstream CPU and device code has not been imported. Original firmware cannot boot.

## v0.8 engineering changes

- Added a platform-neutral firmware inspector with streaming SHA-256 and strict slot/size validation, plus a smoke test.
- Added a GitHub Actions **upstream source audit** that clones the actual upstream repository on GitHub's runners, records its commit and Java file inventory, and locates license files. This workflow does **not** integrate or compile the emulator; it is an actionable path around the local network restriction.
- Original CyOS is **not bootable in this version**. No original firmware or third-party source code is redistributed.

## v0.9 update

See `UPSTREAM_NEXT_STEPS.md`. The upstream retrieval workflow now exports a source archive, license, exact commit revision, and manifest. `tools/verify_upstream_archive.py` validates the artifact without extracting unsafe paths. This workflow has not been executed and the original CyOS core remains unintegrated.

## v1.1: upstream porting-kit import
Run the existing `upstream-compatibility.yml` workflow in a GitHub repository, download its `upstream-porting-kit.tar.gz` artifact, and execute `python3 tools/import_porting_kit.py /path/to/upstream-porting-kit.tar.gz`. The import validates license, Java source presence, file types, sizes and paths, then stages reference source under `upstream-reference/`. `verify-project.yml` adds Java smoke tests and importer safety tests in CI. These workflows have **not** run on GitHub here. No original CyOS boot or APK is claimed.

## v1.2 — Persistent NVRAM storage foundation
Added `NvramStore`, a platform-neutral, size-bounded, crash-resistant local NVRAM persistence component with atomic replacement when supported. A JVM smoke test verifies round trips and rejection of oversized writes without corrupting prior saves. This is **not yet connected to the original H8S emulator**; no ROMs or emulator source are bundled.

## v1.4 real-emulator test harness

Added `tools/verify_real_boot.py` and `REAL_BOOT_TEST.md`. This is a diagnostic launcher for the genuine upstream emulator, **not** an Android port. The original CPU implementation is not present in this package.

## v1.5 — Actual upstream source audit in CI
The `build-real-emulator.yml` workflow now compiles the real upstream emulator and runs a source-level Android portability audit on the cloned Java files. It exports `upstream-port-inventory.json` alongside the original emulator JAR and MIT license. `tools/analyze_upstream_java.py` distinguishes Java sources referencing Swing/AWT/JavaFX from likely portable candidates, without claiming these candidates are Android compatible. This is **not** a CyOS boot or an Android emulator port. The GitHub workflow has not been run in this environment.
