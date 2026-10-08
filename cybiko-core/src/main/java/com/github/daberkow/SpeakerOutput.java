/*
 * Adapted from SpeakerOutput.java in daberkow/cybiko-java-emulator
 * (https://github.com/daberkow/cybiko-java-emulator, commit 58bfbfc).
 * Copyright (c) 2026 Dan Berkowitz. MIT License, see
 * third_party/cybiko-java-emulator/LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * Change for Cybiko Reborn: javax.sound.sampled is not available on Android, so
 * instead of opening a SourceDataLine this class hands the identical 8-bit unsigned
 * 48 kHz mono PCM stream to a pluggable PcmSink (Android uses AudioTrack). The
 * waveform reconstruction logic is unchanged. The public API used by AddressBus
 * and the machine loop (setLevel, beginFrame, setFrameCycle, generateSamples,
 * close) is kept identical so upstream AddressBus compiles unmodified.
 */
package com.github.daberkow;

public class SpeakerOutput {
    /** Receives unsigned 8-bit mono PCM at {@link #SAMPLE_RATE} Hz. */
    public interface PcmSink {
        void write(byte[] data, int offset, int length);
        default void close() {}
    }

    public static final int SAMPLE_RATE = 48000;
    private static final int BUFFER_SIZE = 2048;
    private static final int MAX_TRANSITIONS = 4096;

    private final PcmSink sink;
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private int bufferPos = 0;
    private int currentLevel = 0;
    private boolean open;

    private final double cyclesPerSample;
    private double cycleFraction = 0;

    private int frameStartLevel = 0;
    private int frameCycle = 0;
    private final int[] transitionCycles = new int[MAX_TRANSITIONS];
    private final int[] transitionLevels = new int[MAX_TRANSITIONS];
    private int transitionCount = 0;

    public SpeakerOutput(long clockHz, PcmSink sink) {
        this.cyclesPerSample = (double) clockHz / SAMPLE_RATE;
        this.sink = sink;
        this.open = sink != null;
    }

    public void beginFrame() {
        frameStartLevel = currentLevel;
        frameCycle = 0;
        transitionCount = 0;
    }

    public void setFrameCycle(int cycle) { frameCycle = cycle; }

    public void setLevel(int level) {
        if (level != currentLevel) {
            currentLevel = level;
            if (transitionCount < MAX_TRANSITIONS) {
                transitionCycles[transitionCount] = frameCycle;
                transitionLevels[transitionCount] = level;
                transitionCount++;
            }
        }
    }

    public void generateSamples(int cpuCycles) {
        if (!open) return;
        cycleFraction += cpuCycles;
        int samplesToWrite = (int) (cycleFraction / cyclesPerSample);
        cycleFraction -= samplesToWrite * cyclesPerSample;
        if (transitionCount == 0) {
            for (int i = 0; i < samplesToWrite; i++) {
                buffer[bufferPos++] = (byte) 128;
                if (bufferPos >= buffer.length) flush();
            }
        } else {
            int transIdx = 0;
            int level = frameStartLevel;
            for (int i = 0; i < samplesToWrite; i++) {
                double sampleCycle = (i + 0.5) * cyclesPerSample;
                while (transIdx < transitionCount && transitionCycles[transIdx] <= (int) sampleCycle) {
                    level = transitionLevels[transIdx];
                    transIdx++;
                }
                buffer[bufferPos++] = (level == 0) ? (byte) 96 : (byte) 160;
                if (bufferPos >= buffer.length) flush();
            }
        }
    }

    private void flush() {
        if (!open || bufferPos == 0) return;
        sink.write(buffer, 0, bufferPos);
        bufferPos = 0;
    }

    public void close() {
        if (!open) return;
        flush();
        sink.close();
        open = false;
    }
}
