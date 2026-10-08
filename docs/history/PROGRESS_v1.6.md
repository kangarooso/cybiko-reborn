# Cybiko Reborn v1.6 — genuine emulator binary smoke test

The `build-real-emulator.yml` workflow now does more than compile source: it executes the **actual upstream Java emulator JAR** with `--help`, verifies its executable manifest, exit code and recognizable output, and fails CI if no built JAR passes. This does not need copyrighted ROM files.

Run it by uploading this repository to GitHub and executing **Build real Cybiko emulator and Android prototype** in Actions. The upstream job will compile, run the smoke test, and upload the upstream sources and binaries. The Android job independently builds a prototype APK.

**Not yet accomplished:** No upstream source has been fetched into this local environment; no CI workflow has been run here; no CyOS ROM has booted; no Android CPU port is included. Passing the JAR smoke test establishes only that the genuine desktop emulator executable launches, not that firmware boots or that Android emulation works.
