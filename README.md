# Cybiko Reborn

An Android app (built for the Galaxy Fold) that aims to bring back the **original Cybiko Classic**
(the first-generation, V1 handheld) running its **real CyOS**: the same apps, games, CyberMessaging
and OS behavior, on a 160×100 LCD with the Classic keyboard, in a purple or blue Classic shell.

## Status (v0.2.0, October 2026)

| Piece | State |
|---|---|
| Debug APK | **Builds** (`./gradlew :app:assembleDebug`); also built by GitHub Actions on every push |
| Emulation core | **Integrated:** the real H8S/2241 CPU, address bus, HD66421 LCD, AT45DB041 flash, timers and RTC from the MIT-licensed [daberkow/cybiko-java-emulator](https://github.com/daberkow/cybiko-java-emulator), compiled for Android without Swing |
| START button | Runs that core on a background thread using **your own** `cyrom112.bin` + `flash_v1246.bin` |
| LCD / keys / sound / saves | 4-level gray LCD at 60 fps, all 68 Classic keys (touch and hardware keyboard, press and hold), 1-bit speaker via AudioTrack, NVRAM (installed apps, settings) saved on pause/stop |
| Shells | Purple and blue Classic skins (the SKIN button) |
| **CyOS boot on Android** | **Not yet verified.** Nobody has run it with real ROMs on a phone yet. Upstream boots Classic V1 CyOS on desktop Java, but Android speed and behavior are unknown |
| Radio / CyberMessaging between devices | Not wired yet (upstream has LAN radio; Android transport still to do) |
| Loading `.app` files | Core supports it; no Android file picker for apps yet |

No ROMs, firmware or CyOS code are included or will ever be committed. You must supply your own legally
obtained Classic V1 files: `cyrom112.bin` (32 KB boot ROM) and `flash_v1246.bin` (serial flash).

## Build

```bash
git clone --recursive https://github.com/kangarooso/cybiko-reborn.git
cd cybiko-reborn
bash tests/run-all-smoke.sh          # JVM tests, needs JDK 17+
./gradlew :app:assembleDebug          # needs Android SDK 35; APK in app/build/outputs/apk/debug/
```

If you cloned without `--recursive`, run `git submodule update --init --recursive` first (the upstream
emulator lives in `third_party/cybiko-java-emulator`, pinned to commit `58bfbfc`).

To download a ready-made APK: open the repository on github.com, click **Actions**, open the newest green
**Build Android APK** run, and download **cybiko-reborn-debug-apk** from the Artifacts section at the bottom.

## Try a boot on a desktop (no phone needed)

```bash
./gradlew :cybiko-core:jar
java -cp cybiko-core/build/libs/cybiko-core.jar com.cybikoreborn.core.HeadlessBootCheck cyrom112.bin flash_v1246.bin 20
```

It runs exactly the code the Android app runs for 20 emulated seconds and prints an ASCII picture of the LCD.

## Layout

- `app/` Android UI: `MainActivity` (shell, keyboard, skins), `ClassicV1Session` (runs the core), `LcdFrameView`, `AndroidSpeakerSink`, `RomStore`, `NvramStore`
- `cybiko-core/` pure-Java core: `ClassicV1Machine` (headless V1 wiring adapted from upstream), `ClassicV1Runner` (60 fps thread), `ClassicV1Keys` (V1 key matrix), Android-safe `SpeakerOutput`; upstream sources are compiled from the submodule per `upstream-core-files.txt`
- `tests/` JVM smoke tests; `tools/` upstream audit/import scripts; `docs/history/` earlier planning notes

See `PORTING_STATUS.md` for what is done and what is next, and `THIRD_PARTY_NOTICES.md` for licenses.
