package com.cybikoreborn;

import java.io.*;
import java.nio.file.*;

/** Crash-safe local storage for emulator-owned NVRAM bytes. No CyOS format assumptions. */
public final class NvramStore {
  private final File target;
  private final int maxBytes;
  public NvramStore(File target, int maxBytes) {
    if (target == null || maxBytes <= 0) throw new IllegalArgumentException("Invalid NVRAM configuration");
    this.target = target;
    this.maxBytes = maxBytes;
  }
  public byte[] load() throws IOException {
    if (!target.exists()) return new byte[0];
    if (!target.isFile() || target.length() > maxBytes) throw new IOException("Invalid NVRAM file");
    byte[] bytes = Files.readAllBytes(target.toPath());
    if (bytes.length > maxBytes) throw new IOException("NVRAM too large");
    return bytes;
  }
  public void save(byte[] bytes) throws IOException {
    if (bytes == null || bytes.length > maxBytes) throw new IllegalArgumentException("Invalid NVRAM bytes");
    File parent = target.getAbsoluteFile().getParentFile();
    if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create NVRAM directory");
    Path temp = Files.createTempFile(parent.toPath(), "nvram-", ".tmp");
    try {
      try (FileOutputStream out = new FileOutputStream(temp.toFile())) {
        out.write(bytes);
        out.getFD().sync();
      }
      try { Files.move(temp, target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
      catch (AtomicMoveNotSupportedException ex) { Files.move(temp, target.toPath(), StandardCopyOption.REPLACE_EXISTING); }
    } finally { Files.deleteIfExists(temp); }
  }
}
