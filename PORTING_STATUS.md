# Porting status — v0.2.1 (2026-10-08)

**Goal:** the true original Cybiko Classic (V1) experience on Android: real CyOS from the user's own ROMs,
with all original features (apps, games, CyberMessaging/chat, file system, sound, clock, the real OS
behavior), not a look-alike. Purple and blue Classic shells.

## What exists and is verified

- **Debug APK compiles** (`./gradlew :app:assembleDebug`, AGP 8.7.3, Gradle 8.9 wrapper, SDK 35, minSdk 26).
  The earlier GitHub Actions runs (4, all on 2026-10-08) failed in `android-actions/setup-android@v3`
  (it tries to install the removed `tools` SDK package); the workflow now uses the runner's preinstalled
  SDK, checks out the submodule, runs the smoke tests, then builds and uploads the APK.
- **Upstream core compiled for Android.** Portability audit (`tools/analyze_upstream_java.py`, output in
  `docs/upstream-port-inventory.json`): 24 upstream emulator files; 2 Swing (CybikoEmulator, SwingRenderer),
  2 Android-incompatible (SpeakerOutput uses javax.sound, PtySerialPort uses ProcessBuilder/socat).
  15 core files compile unmodified at Java 17 and are dexed into the APK; SpeakerOutput was adapted.
- **`ClassicV1Machine`** wires the V1 hardware exactly like upstream and runs 1/60 s frames.
  `tests/ClassicV1MachineSmoke.java` (no firmware needed) proves: reset vector, CPU execution of a
  synthetic ROM, 160×100 frame delivery, cycle budget, speaker PCM, keyboard matrix reads with the
  3-frame minimum hold, fresh CFS NVRAM, runner pause/resume.
- **Android wiring:** START creates `ClassicV1Session` → background runner → `LcdFrameView` (4 gray levels);
  on-screen Classic keyboard with real press/release, hardware keyboard mapping, AudioTrack speaker,
  NVRAM save on pause/stop/exit, live speed/PC status line, purple/blue skins.

## v0.2.1: Classic look

- `ClassicDeviceView` draws the whole handheld (purple or blue): F1–F7 shortcut bar, bezel with the 8:5
  crisp LCD, Esc/arrow pad/Del/Ins/?/Tab/Select/Enter deck and the 5-row Classic keyboard, from
  `ClassicV1Layout` (based on the Classic manual diagram and photos of a purple Classic).
- Added the Classic **BkSp** key (matrix column 6 bit 0x04, per MAME `cybiko.cpp` A.6): 69 keys total.
  Hardware-keyboard Backspace still sends Del, as upstream does.
- Multi-touch press/release per finger; light haptic tick per press.
- Installed and used by Michael on his phone at v0.2.0 (UI only; CyOS boot still unverified).
- `tests/ClassicV1LayoutSmoke.java`: every on-screen key is a real V1 matrix key, no duplicates or gaps, LCD is 8:5.
- **Freeze fix.** In v0.2.0 the UI thread took the emulation lock (status line every second, NVRAM
  snapshot in onPause, STOP/close joining the emulation thread and saving 512 KB with fsync). Java
  monitors are not fair, and when the phone runs below real time the emulation thread re-takes the lock
  between frames with no gap, so the UI thread could wait for a long time and the app froze. Now: status getters are
  lock-free (published once per frame); pause/resume/close/session creation and all NVRAM disk I/O run in
  order on one background `cybiko-io` thread; the runner parks on a pause handshake before NVRAM is copied;
  resume is queued behind a pending save so it can't be re-paused; audio writes never throw into the
  emulation loop; the emulation thread runs one notch below normal priority. Covered by new checks in
  `ClassicV1MachineSmoke` (park handshake, lock-free status). Not confirmed on Michael's phone yet.

## Not verified / known gaps (in order)

1. **Real CyOS boot on a phone.** Run START with real ROMs on the Galaxy Fold, or `HeadlessBootCheck` on a
   desktop. Not done: no ROMs are available to the developer environment, by design.
2. **Speed.** The Classic CPU loop executes 184,320 steps per frame (11.0592 MHz / 60). Upstream runs
   real-time on desktop JVMs; ART performance is unmeasured. The status line shows "speed %".
3. **Radio / CyberMessaging.** Upstream's LAN UDP transport works on desktop; needs an Android
   transport (Wi-Fi multicast lock, or Nearby/Bluetooth) plus the V1 RF-service stub from upstream.
4. **Apps:** file picker to add `.app` files into NVRAM (core `addFileToNvram` exists), and import/export of NVRAM.
5. **Shell art:** the shell is vector-drawn and styled, not photographic; landscape currently just scales the
   portrait handheld to fit.
6. Device testing of lifecycle (rotate, fold/unfold, background) and audio latency.

## Legacy

`docs/history/` holds the ChatGPT-era planning notes (v0.1–v1.6). Their CI workflows lived in a
top-level `workflows/` folder that GitHub never ran; it was removed in v0.2.0.
