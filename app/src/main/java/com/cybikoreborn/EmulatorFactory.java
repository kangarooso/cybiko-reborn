package com.cybikoreborn;

import java.io.File;

/** Creates emulator sessions from user-imported firmware. */
public final class EmulatorFactory {
  private EmulatorFactory() {}
  public static EmulatorSession createClassicV1(File bootRom, File flashRom, File nvram) throws Exception {
    if (!bootRom.isFile() || !flashRom.isFile())
      throw new IllegalArgumentException("Import both Classic V1 ROM files first.");
    return new ClassicV1Session(bootRom, flashRom, nvram);
  }
}
