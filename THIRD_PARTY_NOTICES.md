# Third-party notices

## cybiko-java-emulator (MIT)

Source: https://github.com/daberkow/cybiko-java-emulator, included as the git submodule
`third_party/cybiko-java-emulator`, pinned to commit 58bfbfcd8285dbca50e5dcd1eb1d1e2251d1676e.
The submodule keeps the upstream `LICENSE` file. Upstream states that its emulation is derived from
MAME's Cybiko driver by Tim Schuerewegen.

Used by Cybiko Reborn:
- Compiled unmodified from the submodule (see `cybiko-core/upstream-core-files.txt`): AT45DB041Flash,
  AddressBus, CfsImage, FrameBufferRenderer, H8SCpu, H8STimer16, H8STimer8, HD66421Lcd, Log,
  MachineConfig, Memory, PCF8593Rtc, RadioCoProcessor, RadioTransport, SerialPort.
- Adapted (with attribution headers): `cybiko-core/.../com/github/daberkow/SpeakerOutput.java`
  (javax.sound replaced by a PCM sink), `ClassicV1Machine.java` (V1 wiring and frame loop from
  CybikoEmulator.java), `ClassicV1Keys.java` (V1 key matrix table from SwingRenderer.java).

```
MIT License

Copyright (c) 2026 Dan Berkowitz

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Firmware

Cybiko ROM/flash images are copyrighted and are not distributed with this project.
