package com.cybikoreborn.core;

/**
 * Runs a ClassicV1Machine on its own thread at real-time speed (60 emulated frames
 * per second), with pause/resume and a measured speed figure. Platform-neutral:
 * Android drives it from EmulatorSession; JVM tests can drive it directly.
 */
public final class ClassicV1Runner implements AutoCloseable {
    private static final long NANOS_PER_FRAME = 1_000_000_000L / ClassicV1Machine.FRAMES_PER_SECOND;
    private final ClassicV1Machine machine;
    private final Object lock = new Object();
    private Thread thread;
    private volatile boolean running, paused;
    private volatile double speedPercent;
    private volatile Throwable failure;

    public ClassicV1Runner(ClassicV1Machine machine) { this.machine = machine; }

    public void start() {
        synchronized (lock) {
            if (thread != null) return;
            machine.reset();
            running = true;
            thread = new Thread(this::loop, "cybiko-classic-v1");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void loop() {
        long deadline = System.nanoTime() + NANOS_PER_FRAME;
        long windowStart = System.nanoTime();
        int windowFrames = 0;
        try {
            while (running) {
                synchronized (lock) {
                    while (paused && running) lock.wait();
                }
                if (!running) break;
                machine.runFrame();
                windowFrames++;
                long now = System.nanoTime();
                if (now - windowStart >= 1_000_000_000L) {
                    speedPercent = windowFrames * 100.0 * 1_000_000_000L / (now - windowStart)
                            / ClassicV1Machine.FRAMES_PER_SECOND;
                    windowStart = now;
                    windowFrames = 0;
                }
                long sleep = deadline - now;
                if (sleep > 1_000_000) Thread.sleep(sleep / 1_000_000, (int) (sleep % 1_000_000));
                deadline += NANOS_PER_FRAME;
                if (deadline < now - 2 * NANOS_PER_FRAME) deadline = now + NANOS_PER_FRAME;
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            failure = t;
            running = false;
        }
    }

    public void pause() { paused = true; }
    public void resume() { synchronized (lock) { paused = false; lock.notifyAll(); } }
    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
    /** Emulated speed over the last second: 100 = real Cybiko speed. */
    public double speedPercent() { return speedPercent; }
    /** Non-null if the emulation thread crashed. */
    public Throwable failure() { return failure; }

    @Override public void close() {
        Thread t;
        synchronized (lock) { running = false; paused = false; lock.notifyAll(); t = thread; thread = null; }
        if (t != null) {
            t.interrupt();
            try { t.join(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        machine.close();
    }
}
