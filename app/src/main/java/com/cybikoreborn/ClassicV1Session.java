package com.cybikoreborn;

import com.cybikoreborn.core.ClassicV1Machine;
import com.cybikoreborn.core.ClassicV1Runner;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Runs the upstream Cybiko Classic H8S core (via :cybiko-core) on a background thread.
 * Frames go to the VideoSink, keys into the V1 keyboard matrix, NVRAM is saved on pause/close.
 */
public final class ClassicV1Session implements EmulatorSession {
  private final ClassicV1Machine machine = new ClassicV1Machine();
  private final ClassicV1Runner runner = new ClassicV1Runner(machine);
  private final NvramStore nvram;
  private final AndroidSpeakerSink speaker = new AndroidSpeakerSink();
  private final boolean nvramRestored;
  private volatile String lastSaveError;

  ClassicV1Session(File bootRom, File flashRom, File nvramFile) throws IOException {
    machine.loadBootRom(Files.readAllBytes(bootRom.toPath()));
    machine.loadSpiFlash(Files.readAllBytes(flashRom.toPath()));
    nvram = new NvramStore(nvramFile, ClassicV1Machine.nvramSize());
    nvramRestored = machine.loadNvram(nvram.load());
    machine.setSpeaker(speaker);
  }

  @Override public void start(VideoSink sink) {
    machine.setFrameListener((gray, w, h) -> sink.onFrame(gray));
    runner.start();
  }

  @Override public void key(String name, boolean down) { machine.key(name, down); }

  @Override public void pause() { runner.pause(); save(); }
  @Override public void resume() { runner.resume(); }

  private void save() {
    try { nvram.save(machine.snapshotNvram()); lastSaveError = null; }
    catch (Exception e) { lastSaveError = e.getMessage(); }
  }

  @Override public String statusLine() {
    Throwable failure = runner.failure();
    if (failure != null) return "Emulator stopped: " + failure;
    String s = String.format(Locale.US, "Upstream H8S core • speed %.0f%% • frame %d • PC %06X%s",
        runner.speedPercent(), machine.frames(), machine.pc(), nvramRestored ? " • NVRAM restored" : " • fresh NVRAM");
    return lastSaveError == null ? s : s + " • NVRAM save failed: " + lastSaveError;
  }

  @Override public void close() { runner.close(); save(); speaker.close(); }
}
