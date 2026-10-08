package com.cybikoreborn;

import java.util.Arrays;

/** Immutable, platform-independent 160x100 1-bit frame boundary.
 * Pixel data is unpacked, row-major: zero=LCD background, nonzero=ink.
 * This is an adapter, not a Cybiko LCD controller implementation.
 */
public final class MonochromeFrame {
  public static final int WIDTH = 160, HEIGHT = 100, SIZE = WIDTH * HEIGHT;
  private final byte[] pixels;
  public MonochromeFrame(byte[] source) {
    if (source == null || source.length != SIZE)
      throw new IllegalArgumentException("Expected exactly " + SIZE + " pixels");
    pixels = source.clone();
    for (int i = 0; i < pixels.length; i++) pixels[i] = (byte)(pixels[i] == 0 ? 0 : 1);
  }
  public boolean isDark(int x, int y) {
    if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT)
      throw new IndexOutOfBoundsException("Pixel outside LCD");
    return pixels[y * WIDTH + x] != 0;
  }
  public byte[] copyPixels() { return pixels.clone(); }
  public int[] toArgb(int background, int foreground) {
    int[] result = new int[SIZE];
    for (int i = 0; i < SIZE; i++) result[i] = pixels[i] == 0 ? background : foreground;
    return result;
  }
  public static MonochromeFrame blank() { return new MonochromeFrame(new byte[SIZE]); }
}
