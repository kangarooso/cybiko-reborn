# Upstream integration checkpoint (v0.9)

This release does **not** contain the upstream H8S CPU implementation or boot CyOS.

## Automated source retrieval

1. Push this project to a GitHub repository with Actions enabled.
2. Run **Retrieve and validate upstream emulator** under Actions.
3. Download the `upstream-emulator-source-and-license` artifact.
4. Run `python3 tools/verify_upstream_archive.py upstream-emulator-source.tar.gz`.
5. Unpack and examine `upstream/emulator/`, retaining upstream LICENSE and commit revision.

The workflow clones the actual MIT-licensed emulator on GitHub-hosted runners, records its exact revision, checks for Java sources and the MIT license, and packages the source plus license as an artifact. It has not been executed here.

## Android port order

1. Inspect upstream package dependencies and isolate Swing/desktop-specific classes.
2. Create a JVM-only emulator core module; preserve original upstream attribution.
3. Implement Android video, keyboard, audio, and storage adapters.
4. Add headless boot tests with user-supplied Classic V1 ROMs.
5. Connect the verified core to EmulatorSession and build a debug APK.

Do not bundle third-party ROMs unless distribution rights are established.
