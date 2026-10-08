/*
 * Headless Cybiko Classic (V1) machine for Cybiko Reborn.
 *
 * Adapted from CybikoEmulator.java in daberkow/cybiko-java-emulator
 * (https://github.com/daberkow/cybiko-java-emulator, commit 58bfbfc),
 * Copyright (c) 2026 Dan Berkowitz, MIT License (see THIRD_PARTY_NOTICES.md).
 * Upstream's emulation derives from MAME's cybiko driver by Tim Schuerewegen.
 *
 * What was kept: the V1 device wiring (boot ROM, 512 KB external RAM, on-chip RAM,
 * HD66421 LCD, H8S CPU, two 8-bit timers, three 16-bit timers, AT45DB041 SPI flash,
 * radio co-processor object, speaker on TIOCB1) and the per-frame execution order
 * (timers tick, cpu.step, DTC/SCI/serial ticks, then render, audio, RTC, radio).
 * What was removed: Swing UI, CLI parsing, V2/XT-specific paths, PTY serial, LAN/SDR
 * radio transports, status logging and real-time pacing (see ClassicV1Runner).
 * Added: thread-safe key queue with the upstream 3-frame minimum hold, byte-array
 * ROM/NVRAM loading, and validation so Android never silently runs garbage.
 */
package com.cybikoreborn.core;

import com.github.daberkow.AT45DB041Flash;
import com.github.daberkow.AddressBus;
import com.github.daberkow.CfsImage;
import com.github.daberkow.H8SCpu;
import com.github.daberkow.H8STimer16;
import com.github.daberkow.H8STimer8;
import com.github.daberkow.HD66421Lcd;
import com.github.daberkow.MachineConfig;
import com.github.daberkow.Memory;
import com.github.daberkow.RadioCoProcessor;
import com.github.daberkow.SpeakerOutput;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ClassicV1Machine {
    /** Receives the LCD after every emulated frame. gray: 0 = darkest ink, 255 = blank LCD. */
    public interface FrameListener { void onFrame(int[] gray, int width, int height); }

    public static final int WIDTH = HD66421Lcd.WIDTH, HEIGHT = HD66421Lcd.HEIGHT;
    public static final int FRAMES_PER_SECOND = 60;
    /** Upstream releases a tapped key only after it has been visible for this many frames. */
    public static final int MIN_HOLD_FRAMES = 3;

    private final MachineConfig config = MachineConfig.forType(MachineConfig.MachineType.V1);
    private final Memory bootRom = new Memory(config.bootRomSize, false);
    private final Memory externalRam = new Memory(config.externalRamSize, true);
    private final Memory onChipRam = new Memory(config.onChipRamSize, true);
    private final HD66421Lcd lcd = new HD66421Lcd();
    private final AddressBus bus = new AddressBus(config);
    private final H8SCpu cpu;
    private final H8STimer8 timer8_0, timer8_1;
    private final H8STimer16[] timer16 = new H8STimer16[3];
    private final RadioCoProcessor radio = new RadioCoProcessor();
    private SpeakerOutput speaker;
    private volatile FrameListener frameListener;

    private boolean bootRomLoaded, flashLoaded;
    // Published once per frame so UI/status readers never wait on the emulation lock.
    private volatile long totalSteps, frames;
    private volatile int lastPc;
    private volatile boolean lastHalted, lastDisplayOn;
    private final StringBuilder serialLog = new StringBuilder();

    private static final class KeyEvent { final int col, bit; final boolean down;
        KeyEvent(int c, int b, boolean d) { col = c; bit = b; down = d; } }
    private final ConcurrentLinkedQueue<KeyEvent> keyQueue = new ConcurrentLinkedQueue<>();
    private final int[][] holdLeft = new int[9][8];
    private final boolean[][] releasePending = new boolean[9][8];

    public ClassicV1Machine() {
        bus.setBootRom(bootRom);
        bus.setExternalRam(externalRam);
        bus.setOnChipRam(onChipRam);
        bus.setLcd(lcd);
        cpu = new H8SCpu(bus);
        bus.setCpu(cpu);
        bus.setRadio(radio);
        timer8_0 = new H8STimer8(0, cpu);
        timer8_1 = new H8STimer8(1, cpu);
        bus.setTimer8_0(timer8_0);
        bus.setTimer8_1(timer8_1);
        timer16[0] = new H8STimer16(0, 4, 32, cpu);
        timer16[1] = new H8STimer16(1, 2, 40, cpu);
        timer16[2] = new H8STimer16(2, 2, 44, cpu);
        for (int i = 0; i < 3; i++) bus.setTimer16(i, timer16[i]);
    }

    /** Loads the 32 KB Classic boot ROM (cyrom112.bin). */
    public synchronized void loadBootRom(byte[] data) {
        if (data == null || data.length == 0 || data.length > config.bootRomSize)
            throw new IllegalArgumentException("Boot ROM must be 1.." + config.bootRomSize + " bytes, got "
                    + (data == null ? 0 : data.length));
        bootRom.load(data, 0);
        bootRomLoaded = true;
    }

    /** Loads the AT45DB041 serial flash image (flash_v1246.bin). */
    public synchronized void loadSpiFlash(byte[] data) {
        if (data == null || data.length == 0 || data.length > config.spiFlashSize)
            throw new IllegalArgumentException("Flash image must be 1.." + config.spiFlashSize + " bytes, got "
                    + (data == null ? 0 : data.length));
        bus.setSpiFlash(new AT45DB041Flash(data));
        flashLoaded = true;
    }

    /**
     * Loads persistent RAM (CyOS file system). Empty or non-CFS data starts a fresh,
     * empty CFS image exactly like upstream. Returns true if existing data was kept.
     */
    public synchronized boolean loadNvram(byte[] data) {
        boolean valid = data != null && data.length > 0 && CfsImage.isCfsImage(data);
        CfsImage cfs = valid ? new CfsImage(data) : new CfsImage();
        externalRam.load(cfs.getImageData(), 0);
        return valid;
    }

    /** Adds a Cybiko .app/.help/data file to RAM before boot (upstream --app behaviour). */
    public synchronized void addFileToNvram(String name, byte[] data) {
        byte[] ram = externalRam.getRawData();
        CfsImage cfs = CfsImage.isCfsImage(ram) ? new CfsImage(ram.clone()) : new CfsImage();
        cfs.addFile(name, data);
        externalRam.load(cfs.getImageData(), 0);
    }

    /** Copies RAM. Takes the emulation lock: call with the runner paused (see ClassicV1Runner.pauseAndWait), never on a UI thread. */
    public synchronized byte[] snapshotNvram() { return externalRam.getRawData().clone(); }
    public static int nvramSize() { return MachineConfig.forType(MachineConfig.MachineType.V1).externalRamSize; }

    public synchronized void setSpeaker(SpeakerOutput.PcmSink sink) {
        speaker = sink == null ? null : new SpeakerOutput(config.clockHz, sink);
        bus.setSpeakerOutput(speaker);
        final SpeakerOutput s = speaker;
        timer16[1].setOutputBCallback(level -> { if (s != null) s.setLevel(level); });
    }

    public void setFrameListener(FrameListener listener) { frameListener = listener; }

    public synchronized void reset() {
        if (!bootRomLoaded) throw new IllegalStateException("Load the Classic boot ROM first");
        cpu.reset();
        totalSteps = 0;
        frames = 0;
        lastPc = cpu.getPC();
    }

    /** Queue a physical key change. Safe to call from any thread (e.g. the Android UI thread). */
    public void key(int column, int bit, boolean down) {
        if (column < 0 || column >= 9 || Integer.bitCount(bit) != 1 || bit > 0x80)
            throw new IllegalArgumentException("Invalid Classic key position");
        keyQueue.add(new KeyEvent(column, bit, down));
    }

    /** Queue a key by its Cybiko name (see ClassicV1Keys). Returns false for unknown names. */
    public boolean key(String name, boolean down) {
        ClassicV1Keys.Position p = ClassicV1Keys.find(name);
        if (p == null) return false;
        key(p.column, p.bit, down);
        return true;
    }

    private void applyKeys() {
        for (int c = 0; c < 9; c++) for (int b = 0; b < 8; b++) {
            if (holdLeft[c][b] > 0) holdLeft[c][b]--;
            if (holdLeft[c][b] == 0 && releasePending[c][b]) {
                releasePending[c][b] = false;
                bus.setKeyState(c, 1 << b, false);
            }
        }
        KeyEvent e;
        while ((e = keyQueue.poll()) != null) {
            int b = Integer.numberOfTrailingZeros(e.bit);
            if (e.down) {
                bus.setKeyState(e.col, e.bit, true);
                holdLeft[e.col][b] = MIN_HOLD_FRAMES;
                releasePending[e.col][b] = false;
            } else if (holdLeft[e.col][b] > 0) {
                releasePending[e.col][b] = true;
            } else {
                bus.setKeyState(e.col, e.bit, false);
            }
        }
    }

    /** Runs one 1/60 s frame of emulated time and delivers the LCD to the listener. */
    public void runFrame() {
        int[] frame;
        synchronized (this) {
            if (!bootRomLoaded) throw new IllegalStateException("Load the Classic boot ROM first");
            applyKeys();
            boolean t8a = timer8_0.isRunning(), t8b = timer8_1.isRunning();
            boolean t0 = timer16[0].isRunning(), t1 = timer16[1].isRunning(), t2 = timer16[2].isRunning();
            if (speaker != null) speaker.beginFrame();
            int budget = config.cyclesPerFrame;
            for (int i = 0; i < budget; i++) {
                if (speaker != null) speaker.setFrameCycle(i);
                if (t8a) timer8_0.tick();
                if (t8b) timer8_1.tick();
                if (t0) timer16[0].tick();
                if (t1) timer16[1].tick();
                if (t2) timer16[2].tick();
                cpu.step();
                bus.tickDtcCompletion();
                bus.tickSci0();
                bus.tickSci2();
                bus.tickSerial();
            }
            totalSteps += budget;
            frame = lcd.getFrameBuffer().clone();
            if (speaker != null) speaker.generateSamples(budget);
            bus.tickRtc();
            radio.tick();
            for (int sci = 0; sci < 3; sci++) {
                String s = bus.drainSerialOutput(sci);
                if (!s.isEmpty() && serialLog.length() < 64 * 1024) serialLog.append(s);
            }
            lastPc = cpu.getPC();
            lastHalted = cpu.isHalted();
            lastDisplayOn = lcd.isDisplayOn();
            frames++;
        }
        FrameListener l = frameListener;
        if (l != null) l.onFrame(frame, WIDTH, HEIGHT);
    }

    public synchronized void close() { if (speaker != null) speaker.close(); }

    public synchronized boolean isReady() { return bootRomLoaded && flashLoaded; }
    // Lock-free status getters (values as of the last completed frame). Safe on a UI thread.
    public long frames() { return frames; }
    public long totalSteps() { return totalSteps; }
    public int pc() { return lastPc; }
    public boolean halted() { return lastHalted; }
    public boolean displayOn() { return lastDisplayOn; }
    public synchronized String serialOutput() { return serialLog.toString(); }
    public synchronized int[] currentFrame() { return lcd.getFrameBuffer().clone(); }
    /** Direct bus read, for tests and diagnostics only. */
    public synchronized int debugRead16(int address) { return bus.read16(address); }
}
