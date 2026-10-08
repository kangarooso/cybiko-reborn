package com.cybikoreborn.core;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Desktop/CI diagnostic: runs the Android-side Classic core headlessly with
 * user-supplied firmware and prints what the LCD shows. No firmware is bundled.
 *
 *   java -cp cybiko-core.jar com.cybikoreborn.core.HeadlessBootCheck cyrom112.bin flash_v1246.bin [seconds]
 *
 * A non-blank LCD with recognizable CyOS screens is evidence of a boot; a blank LCD
 * or a halted CPU is not. Inspect the ASCII render yourself before claiming success.
 */
public final class HeadlessBootCheck {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: HeadlessBootCheck <cyrom112.bin> <flash_v1246.bin> [seconds]");
            System.exit(2);
        }
        int seconds = args.length > 2 ? Integer.parseInt(args[2]) : 20;
        ClassicV1Machine m = new ClassicV1Machine();
        m.loadBootRom(Files.readAllBytes(Paths.get(args[0])));
        m.loadSpiFlash(Files.readAllBytes(Paths.get(args[1])));
        m.loadNvram(new byte[0]);
        m.reset();
        long start = System.nanoTime();
        for (int f = 0; f < seconds * ClassicV1Machine.FRAMES_PER_SECOND; f++) {
            m.runFrame();
            if (m.halted()) break;
        }
        double wall = (System.nanoTime() - start) / 1e9;
        int[] px = m.currentFrame();
        int ink = 0;
        for (int v : px) if (v < 128) ink++;
        System.out.printf("frames=%d steps=%d pc=0x%06X halted=%b displayOn=%b inkPixels=%d wall=%.1fs (%.0f%% of real time)%n",
                m.frames(), m.totalSteps(), m.pc(), m.halted(), m.displayOn(), ink, wall,
                m.frames() / (double) ClassicV1Machine.FRAMES_PER_SECOND / wall * 100);
        String serial = m.serialOutput();
        if (!serial.isEmpty()) System.out.println("serial: " + serial.replaceAll("[^\\x20-\\x7e\\n]", "."));
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < ClassicV1Machine.HEIGHT; y += 2) {
            for (int x = 0; x < ClassicV1Machine.WIDTH; x++) {
                int v = Math.min(px[y * ClassicV1Machine.WIDTH + x], px[Math.min(y + 1, 99) * ClassicV1Machine.WIDTH + x]);
                sb.append(v < 64 ? '#' : v < 128 ? '+' : v < 192 ? '.' : ' ');
            }
            sb.append('\n');
        }
        System.out.print(sb);
    }
}
