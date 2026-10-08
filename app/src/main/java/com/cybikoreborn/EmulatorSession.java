package com.cybikoreborn;

/** Boundary for integrating the upstream H8S CPU, LCD, keyboard and NVRAM.
 * No original CyOS implementation is included in this source tree.
 */
public interface EmulatorSession extends AutoCloseable {
  interface VideoSink { void onFrame(byte[] monochrome160x100); }
  void start(VideoSink sink) throws Exception;
  void key(String name, boolean down);
  void pause();
  void resume();
  @Override void close();
}
