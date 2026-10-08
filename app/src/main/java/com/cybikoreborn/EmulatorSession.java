package com.cybikoreborn;

/** Boundary between the Android UI and an emulation core.
 * The Classic V1 implementation (ClassicV1Session) runs the upstream H8S core;
 * no firmware or CyOS code is shipped with the app.
 */
public interface EmulatorSession extends AutoCloseable {
  /** Receives 160x100 row-major LCD frames: 0 = darkest ink, 255 = blank LCD (4 gray levels on real hardware). */
  interface VideoSink { void onFrame(int[] gray160x100); }
  void start(VideoSink sink) throws Exception;
  /** Physical Cybiko key by name (see com.cybikoreborn.core.ClassicV1Keys), pressed or released. */
  void key(String name, boolean down);
  void pause();
  void resume();
  /** One-line human-readable status (speed, errors). */
  default String statusLine() { return ""; }
  @Override void close();
}
