# Real Classic V1 boot test (not a simulation)

The upstream Java emulator can be tested on a desktop with Java 21.

1. Retrieve and build upstream using `.github/workflows/build-real-emulator.yml` on GitHub.
2. Supply legally obtained Classic V1 `cyrom112.bin` and `flash_v1246.bin`.
3. Run `python3 tools/verify_real_boot.py --jar PATH/TO/Cybiko-Emulator.jar --boot cyrom112.bin --flash flash_v1246.bin --seconds 30`.
4. Examine `cybiko-boot.log`. A timeout is **not** proof of a successful boot. Confirm recognizable CyOS activity and an interactive UI separately.

**Android remains blocked** until the upstream CPU, memory, peripheral and display implementations are ported and tested.
