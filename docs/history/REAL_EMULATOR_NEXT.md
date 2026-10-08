# Real emulator acquisition and boot path

The upstream Java emulator is an actual Cybiko Classic V1 emulator; the Android prototype is not.

1. Upload this project's contents to a GitHub repository.
2. In GitHub Actions, run **Build real Cybiko emulator and Android prototype**.
3. Download `real-cybiko-java-emulator-and-source` to obtain compiled upstream JARs, source and license.
4. To test authentic CyOS on a desktop with Java 21, supply your own legitimately obtained `cyrom112.bin` and `flash_v1246.bin` and run the upstream fat JAR with `--machine v1`.
5. The Android job independently produces an APK of the *prototype only*. It does not boot CyOS.

## Android porting gap

A working Android CyOS emulator still requires porting upstream H8S CPU, memory and peripherals, replacing Swing/AWT LCD and keyboard wiring with Android equivalents, and validating the Classic V1 boot with original firmware. Neither the workflow nor the prototype implements those steps.

The GitHub workflow is written but cannot be executed from this environment without access to a GitHub repository and its Actions service. The upstream Java build may also fail if the upstream project changes; successful execution has not been verified.
