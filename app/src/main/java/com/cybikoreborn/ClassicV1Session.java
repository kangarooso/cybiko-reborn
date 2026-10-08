package com.cybikoreborn;

import com.cybikoreborn.core.ClassicV1Machine;
import com.cybikoreborn.core.ClassicV1Runner;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs the upstream Cybiko Classic H8S core (via :cybiko-core) on a background thread.
 * Frames go to the VideoSink, keys into the V1 keyboard matrix, NVRAM is saved on pause/close.
 *
 * Threading rule (fixes v0.2.0 freezes): nothing here blocks the caller. Every call that has
 * to wait for the emulation thread or touch the disk (pause+save, resume, close, creating a
 * session) runs in order on one background "cybiko-io" thread. key() and statusLine() are
 * lock-free.
 */
public final class ClassicV1Session implements EmulatorSession {
  /** Single ordered thread for session creation, NVRAM I/O and shutdown. */
  static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "cybiko-io"); t.setDaemon(true); return t;
  });

  private final ClassicV1Machine machine = new ClassicV1Machine();
  private final ClassicV1Runner runner = new ClassicV1Runner(machine);
  private final NvramStore nvram;
  private final AndroidSpeakerSink speaker = new AndroidSpeakerSink();
  private final boolean nvramRestored;
  private volatile String lastSaveError;
  private volatile boolean closed;

  /** Reads ROMs and NVRAM from disk: construct on IO, not the UI thread. */
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

  @Override public void pause() {
    runner.pause(); // stop promptly; the wait and the save happen off the UI thread
    IO.execute(() -> { if (!closed) { awaitParked(); save(); } });
  }

  @Override public void resume() {
    // Queued behind any pending pause/save so a late save can never re-pause a resumed session.
    IO.execute(() -> { if (!closed) runner.resume(); });
  }

  private void awaitParked() {
    try { runner.pauseAndWait(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
  }

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

  /** Returns immediately; the thread stop, final NVRAM save and audio release happen on IO. */
  @Override public void close() {
    if (closed) return;
    closed = true;
    machine.setFrameListener(null);
    runner.pause();
    IO.execute(() -> { runner.close(); save(); speaker.close(); });
  }
}
