package com.cybikoreborn;

import java.io.File;

/** Explicitly fails closed until a verified Android-compatible upstream core is ported. */
public final class EmulatorFactory {
  private EmulatorFactory() {}
  public static EmulatorSession createClassicV1(File bootRom, File flashRom, File nvram) {
    if (!bootRom.isFile() || !flashRom.isFile())
      throw new IllegalArgumentException("Import both Classic V1 ROM files first.");
    throw new UnsupportedOperationException("Original H8S emulator core not yet integrated. No CyOS boot attempted.");
  }
}
