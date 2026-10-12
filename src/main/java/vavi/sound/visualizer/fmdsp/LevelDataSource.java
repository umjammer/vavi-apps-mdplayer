/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.visualizer.fmdsp;


/**
 * Provides per-channel sample-peak values for the level meter strip.
 * <p>
 * Channel layout matches the C visualizer:
 * <pre>
 *   0..5  : FM 1..6
 *   6..8  : SSG 1..3
 *   9     : Drum (max of 6 drum voices)
 *   10    : ADPCM
 *   11..18: PPZ8 1..8
 * </pre>
 */
public interface LevelDataSource {

    /** Number of metered channels. Mirrors {@code FMDSP_LEVEL_COUNT}. */
    int COUNT = 19;

    /** Pan position. */
    enum Pan {
        /** Hard left. */
        LEFT,
        /** Mid-left. */
        MID_LEFT,
        /** Center. */
        CENTER,
        /** Mid-right. */
        MID_RIGHT,
        /** Hard right. */
        RIGHT,
        /** No pan / muted. */
        NONE,
    }

    /**
     * Sample-peak amplitude for the given channel, in {@code 0..32767}.
     * The renderer converts this to dBFS via {@code 20*log10(x/32768)} and
     * paints a 0..32 bar (1 step per 1.5 dB, 32 steps = 0 dBFS).
     */
    int level(int channel);

    /** Pan position to display. */
    Pan pan(int channel);

    /**
     * Label drawn above the channel's meter, up to 3 characters (e.g. {@code "OPM"}). As soon as
     * any channel provides one, the built-in PC-98 label row is replaced by the provided labels.
     */
    default String label(int channel) {
        return null;
    }

    /**
     * Whether the channel the meter column shows is muted, which greys its pan out. A column with
     * a {@linkplain #track row} asks {@link TrackStatusSource#masked} instead; this is for the
     * ones without, the drum meter.
     */
    default boolean masked(int channel) {
        return false;
    }

    /**
     * The track row whose key, pan and tone number the meter column displays underneath itself,
     * or null when the column has none (the drum meter). Defaults to the fixed PC-98 assignment;
     * a source that hands its meter columns out dynamically overrides this so the readout follows
     * the same chip as the meter.
     */
    default TrackId track(int channel) {
        return switch (channel) {
            case 0 -> TrackId.FM_1;
            case 1 -> TrackId.FM_2;
            case 2 -> TrackId.FM_3;
            case 3 -> TrackId.FM_4;
            case 4 -> TrackId.FM_5;
            case 5 -> TrackId.FM_6;
            case 6 -> TrackId.SSG_1;
            case 7 -> TrackId.SSG_2;
            case 8 -> TrackId.SSG_3;
            case 9 -> null; // rhythm
            case 10 -> TrackId.ADPCM;
            case 11, 12, 13, 14, 15, 16, 17, 18 -> TrackId.values()[TrackId.PPZ8_1.ordinal() + channel - 11];
            default -> null;
        };
    }
}
