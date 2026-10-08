# Porting status — v1.5

- Upstream source: not present locally.
- CI workflow: retrieves and compiles upstream, then produces source classification inventory. Not executed here.
- Android app: prototype, no H8S CPU wired in.
- Real Classic V1 boot: not demonstrated.
- APK: not compiled or device tested.

Run `.github/workflows/build-real-emulator.yml` on GitHub to obtain the upstream JAR, source tree, exact commit, license, and inventory artifact. The inventory is a first-pass static heuristic, not a proof of Android compatibility.
