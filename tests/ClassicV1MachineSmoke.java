import com.cybikoreborn.core.ClassicV1Keys;
import com.cybikoreborn.core.ClassicV1Machine;
import com.cybikoreborn.core.ClassicV1Runner;
import com.github.daberkow.CfsImage;
import java.io.ByteArrayOutputStream;

/**
 * Exercises the real upstream H8S core through the Android-facing machine wrapper,
 * WITHOUT copyrighted firmware: a synthetic 32 KB boot ROM whose reset vector points
 * at an H8S "BRA ." loop. Proves wiring, frame delivery, key matrix and NVRAM plumbing.
 * It does NOT prove CyOS boots.
 */
public class ClassicV1MachineSmoke {
    static void check(boolean ok, String what) { if (!ok) throw new AssertionError(what); }

    public static void main(String[] args) throws Exception {
        byte[] rom = new byte[0x8000];
        rom[3] = 0x00; rom[2] = 0x01;            // reset vector = 0x00000100 (big-endian)
        rom[0x100] = 0x40; rom[0x101] = (byte) 0xFE; // BRA -2 : spin forever
        ClassicV1Machine m = new ClassicV1Machine();
        m.loadBootRom(rom);
        m.loadSpiFlash(new byte[1024]);
        check(!m.loadNvram(new byte[0]), "empty NVRAM should start fresh");
        check(CfsImage.isCfsImage(m.snapshotNvram()), "fresh NVRAM should be a CFS image");
        check(m.snapshotNvram().length == ClassicV1Machine.nvramSize(), "NVRAM size");

        int[] delivered = {0};
        m.setFrameListener((gray, w, h) -> {
            check(w == 160 && h == 100 && gray.length == 16000, "frame geometry");
            delivered[0]++;
        });
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        m.setSpeaker((data, off, len) -> pcm.write(data, off, len));
        m.reset();
        check(m.pc() == 0x100, "reset vector not honored: pc=" + Integer.toHexString(m.pc()));
        for (int i = 0; i < 10; i++) m.runFrame();
        check(delivered[0] == 10 && m.frames() == 10, "10 frames delivered");
        check(m.pc() == 0x100 && !m.halted(), "CPU should be spinning at 0x100");
        check(m.totalSteps() == 10L * (11_059_200 / 60), "cycle budget per frame");
        check(pcm.size() >= 6000 && pcm.size() <= 8000, "~8000 PCM samples (2048-byte flushes) for 10 frames, got " + pcm.size());

        // Keyboard: Q is column 0, bit 0x10. Column 0 is selected when address bit 1 (word bit 0) is 0.
        int col0 = 0xE00000 + ((0x1FE) << 1);
        check((m.debugRead16(col0) & 0x10) != 0, "Q idle");
        check(m.key("q", true), "Q known");
        m.runFrame();
        check((m.debugRead16(col0) & 0x10) == 0, "Q pressed in matrix");
        m.key("Q", false);
        m.runFrame();
        check((m.debugRead16(col0) & 0x10) == 0, "Q held for minimum frames");
        for (int i = 0; i < ClassicV1Machine.MIN_HOLD_FRAMES; i++) m.runFrame();
        check((m.debugRead16(col0) & 0x10) != 0, "Q released after minimum hold");
        check(!m.key("NOT_A_KEY", true), "unknown key rejected");
        check(ClassicV1Keys.all().size() == 69, "69 Classic keys mapped, got " + ClassicV1Keys.all().size());

        // Runner: real-time thread, pause/resume, clean close.
        ClassicV1Runner r = new ClassicV1Runner(m);
        r.start();
        Thread.sleep(300);
        r.pause();
        Thread.sleep(100);
        long a = m.frames();
        Thread.sleep(200);
        check(m.frames() == a, "paused runner should not advance");
        r.resume();
        Thread.sleep(300);
        check(m.frames() > a, "resumed runner advances");
        check(r.failure() == null, "runner failure: " + r.failure());
        // Freeze regression (v0.2.0): pauseAndWait parks the thread so NVRAM can be copied at once.
        check(r.pauseAndWait(2000), "runner parks on pauseAndWait");
        long parked = m.frames();
        long t0 = System.nanoTime();
        byte[] nv = m.snapshotNvram();
        check(nv.length == ClassicV1Machine.nvramSize() && System.nanoTime() - t0 < 200_000_000L, "snapshot while parked is immediate");
        Thread.sleep(150);
        check(m.frames() == parked, "parked runner stays parked");
        r.resume();
        // Status getters must never wait on the emulation lock (they run on the Android UI thread).
        long[] seen = {-1};
        Thread reader = new Thread(() -> { seen[0] = m.frames(); m.pc(); m.halted(); m.displayOn(); });
        synchronized (m) { reader.start(); reader.join(1000); }
        check(seen[0] >= 0 && !reader.isAlive(), "status getters are lock-free");
        r.close();
        check(r.pauseAndWait(100), "pauseAndWait after close returns at once");
        System.out.println("PASS: upstream H8S core runs synthetic ROM, frames, audio PCM, key matrix + hold, NVRAM CFS, runner pause/resume/park, lock-free status");
    }
}
