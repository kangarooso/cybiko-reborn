# Real emulator integration plan (not yet complete)

Upstream: https://github.com/daberkow/cybiko-java-emulator
License: MIT (retain upstream LICENSE and copyright notices when porting).
The upstream README says its Java 21 desktop emulator supports Classic V1, with H8S CPU emulation and a Swing frontend. Android cannot run the desktop Swing UI as-is.

1. Obtain the upstream repository on a network-enabled workstation and inspect `emulator/` for the headless CPU, memory, peripheral, keyboard and LCD interfaces. Review dependencies and third-party licenses, including its stated MAME lineage.
2. Extract a Java-only core with no Swing, AWT or desktop audio dependencies. If Java 21 features are used, adapt for the Android toolchain or use supported desugaring.
3. Implement `EmulatorSession` and replace `EmulatorFactory`'s deliberate unsupported exception. Start the CPU off the main UI thread, deliver 160x100 framebuffer copies to the main thread, and translate key down/up to the Classic V1 keyboard matrix.
4. Load user-provided Classic V1 ROMs `cyrom112.bin` and `flash_v1246.bin`. Persist NVRAM privately. Do not redistribute firmware without permission.
5. Verify boot, key handling, sleep/resume, app loading, sound, and radio separately on a Galaxy Fold. No APK/device verification has occurred here.

Known limitation: firmware import and LCD tests do NOT imply an emulated boot.
