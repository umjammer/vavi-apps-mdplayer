/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import mdplayer.chips.Saa1099Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * SAA1099, the square wave chip of the Creative Music System and the Game Blaster: six tone
 * channels, each with its own stereo amplitude, a frequency divider and an octave.
 * <p>
 * The emulator decodes the registers as they arrive and keeps only the state they mean, so this
 * reads that state back rather than the registers: amplitudes as their nibbles, the frequency and
 * octave as written, and the tone, noise and chip enables as flags.
 * <p>
 * Six channels do not fit the three SSG rows, so they take the wider FM block and are labelled
 * for what they are.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class Saa1099Reader extends ChipReader {

    private static final int CHANNELS = 6;

    /**
     * The chip's usual 8 MHz clock. Its tone divider is a 256th of it, so a channel plays at
     * {@code clock / 256 * 2^octave / (511 - frequency)}.
     */
    private static final double clock = 8000000;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevPitches = new int[CHANNELS];
    private boolean active;

    /** the chip's own channel state, read back once a frame */
    private Map<String, Object> info;

    @Override
    protected Saa1099Chip chip() {
        return chipRegister.chip(Saa1099Chip.class);
    }

    @Override
    public String chipName() {
        return "SAA";
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevPitches, 0);
        active = false;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 95;
    }

    @Override
    public void poll() {
        try {
            info = chip().getInfo(chipId);
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
            info = Collections.emptyMap();
        }
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int ch = 0; ch < CHANNELS && !active; ch++) active = sounding(ch);
        }
        return active;
    }

    private int volume(int ch) {
        return Math.max((int) info.get("channels." + ch + ".volumeL"),
                (int) info.get("channels." + ch + ".volumeR"));
    }

    private boolean sounding(int ch) {
        if (info.isEmpty() || !(boolean) info.get("enabled")) return false;
        boolean tone = (boolean) info.get("channels." + ch + ".tone");
        boolean noise = (boolean) info.get("channels." + ch + ".noise");
        return (tone || noise) && volume(ch) != 0;
    }

    @Override
    public int channels(Group group) {
        return CHANNELS;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        out.name = "SSG";
        out.num = ch + 1;
        out.info = TrackInfo.SSG;
        if (info.isEmpty()) return;

        int left = (int) info.get("channels." + ch + ".volumeL");
        int right = (int) info.get("channels." + ch + ".volumeR");
        int fnum = (int) info.get("channels." + ch + ".frequency");
        int octave = (int) info.get("channels." + ch + ".octave");
        boolean tone = (boolean) info.get("channels." + ch + ".tone");
        boolean noise = (boolean) info.get("channels." + ch + ".noise");
        boolean sounding = sounding(ch);

        out.sounding = sounding;
        int pitch = fnum | (octave << 8);
        out.keyOn = sounding && (!prevSoundings[ch] || pitch != prevPitches[ch]);
        prevSoundings[ch] = sounding;
        prevPitches[ch] = pitch;

        out.note = tone ? Notes.noteOf(clock / 256 * Math.pow(2, octave) / (511 - fnum)) : -1;
        out.volume = Math.max(left, right);
        out.ssgTone = tone;
        out.ssgNoise = noise;
        // the amplitude is linear over its four bits
        out.amplitude = sounding ? Math.max(left, right) / 15.0 : 0;
        out.pan = panOf(left, right);
    }

    private static Pan panOf(int l, int r) {
        if (l == 0 && r == 0) return Pan.NONE;
        if (r == 0) return Pan.LEFT;
        if (l == 0) return Pan.RIGHT;
        double balance = r / (double) (l + r);
        if (balance < 0.25) return Pan.LEFT;
        if (balance < 0.45) return Pan.MID_LEFT;
        if (balance <= 0.55) return Pan.CENTER;
        if (balance <= 0.75) return Pan.MID_RIGHT;
        return Pan.RIGHT;
    }
}
