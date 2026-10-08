import com.cybikoreborn.MonochromeFrame;
import java.util.Arrays;
public class MonochromeFrameSmoke {
  public static void main(String[] args) {
    byte[] source = new byte[MonochromeFrame.SIZE];
    source[159] = 5;
    MonochromeFrame frame = new MonochromeFrame(source);
    source[159] = 0;
    if (!frame.isDark(159, 0) || frame.isDark(0, 0)) throw new AssertionError("Pixels incorrect");
    byte[] copy = frame.copyPixels(); copy[159] = 0;
    if (!frame.isDark(159, 0)) throw new AssertionError("Frame not immutable");
    int[] argb = frame.toArgb(0xffabcdef, 0xff123456);
    if (argb[159] != 0xff123456 || argb[0] != 0xffabcdef) throw new AssertionError("ARGB mapping incorrect");
    try { new MonochromeFrame(new byte[42]); throw new AssertionError("Accepted invalid frame"); }
    catch (IllegalArgumentException expected) { }
    try { frame.isDark(160, 0); throw new AssertionError("Accepted invalid coordinates"); }
    catch (IndexOutOfBoundsException expected) { }
    System.out.println("PASS: immutable framebuffer, normalization, dimensions, pixel mapping, bounds");
  }
}
