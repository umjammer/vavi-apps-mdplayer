/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.visualizer.fmdsp;


/**
 * Top-level work state. Mirrors the subset of {@code struct fmdriver_work}
 * the C visualizer reads.
 */
public interface WorkStateSource {

    /** Sample rate the elapsed-time counter is expressed in. C uses 55467. */
    default int sampleRate() {
        return 55467;
    }

    /** Frames generated since playback started. */
    long generatedFrames();

    /** Driver internal tick counter (TimerB ticks). */
    long timerBCount();

    /** TimerB period (0..255). */
    int timerB();

    /** Loop count. */
    int loopCount();

    /** TimerB ticks measured for one loop, 0 if unknown. */
    long loopTimerBCount();

    /** TimerB ticks since the most recent loop start. */
    long timerBCountLoop();

    /** Total estimated TimerB ticks for the song, 0 if unknown. */
    default long totalTimerBCount() {
        return 0;
    }

    /**
     * The parts the {@code VOLUME DOWN} counter shows one of at a time, the four of the original:
     * FM, SSG, the rhythm part and PCM.
     */
    enum VolumePart {
        FM, SSG, RHY, PCM
    }

    /**
     * How far the player is turning {@code part} down, in whatever unit it counts corrections in,
     * negative being quieter - what the {@code VOLUME DOWN} counter shows. The counter follows
     * whichever part moved last, so a source that has one number for the whole song may report it
     * for every part.
     * <p>
     * 0, the default, is no correction, and a source that has no such number at all leaves the
     * counter at zero.
     */
    default int volumeDown(VolumePart part) {
        return 0;
    }

    /**
     * The number of the song being played among the file's songs, 1 origin - what the
     * {@code PGM NUMBER} counter shows. 0, the default, is a file that has no song numbers, and
     * leaves the counter blank.
     */
    default int songNo() {
        return 0;
    }

    /** True if a song is loaded and currently advancing. */
    boolean playing();

    /** True if playback is paused. */
    boolean paused();

    /** Name of the sound driver shown next to {@code DRIVER}, e.g. {@code "PMD"}, may be null. */
    default String driverName() {
        return null;
    }

    /** Using chips shown under {@code DRIVER}, e.g. {@code "YM2608"}, may be null. */
    default String chips() {
        return null;
    }

    /** Current title / filename, may be null. */
    default String filename() {
        return null;
    }

    /** Comment line for {@code line in 0..2}, may be null. */
    default String comment(int line) {
        return null;
    }

    /** Type label of the external PCM file for index (e.g. "PPC", "PPZ1", "PVI", etc.), may be null. */
    default String pcmType(int index) {
        return switch (index) {
            case 0 -> "PCM1";
            case 1 -> "PCM2";
            default -> null;
        };
    }

    /** Name of the external PCM file for index (0: PPC/PVI, 1: PPZ1/PPZ, 2: PPZ2, 3: PPS), may be null. */
    default String pcmFilename(int index) {
        return null;
    }

    /** True if there was an error loading the PCM file for index. */
    default boolean pcmError(int index) {
        return false;
    }
}
