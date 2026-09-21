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

import mdplayer.chips.DmgChip;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * The Game Boy's sound: two square channels, a wavetable and a noise channel.
 * <p>
 * The three pitched channels share an eleven bit divider, and count
 * {@code 131072 / (2048 - freq)} Hz - the wavetable half that, since it steps through thirty two
 * samples where a square has two. The noise runs off a shift register and has no pitch at all.
 * <p>
 * Whether a channel is sounding is the emulator's own flag, which the length counter and the
 * envelope both clear. Pan is a bit a side, so a channel is hard left, hard right or centre and
 * nothing in between.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class DmgReader extends ChipReader {

    private static final int CHANNELS = 4;

    /** the square channels' base divider */
    private static final double squareBase = 131072;

    /** the display's volume range, which the chip's view already scales to */
    private static final double volumeMax = 19;

    private Map<String, Object> info;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevFreqs = new int[CHANNELS];
    private boolean active;

    @Override
    protected DmgChip chip() {
        return chipRegister.chip(DmgChip.class);
    }

    @Override
    public String chipName() {
        return "DMG";
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevFreqs, 0);
        active = false;
        info = null;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 88;
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

    private int intOf(int ch, String field) {
        Object value = info.get("channels." + ch + "." + field);
        return value instanceof Integer i ? i : 0;
    }

    private boolean sounding(int ch) {
        if (info == null || info.isEmpty()) return false;
        Object playing = info.get("channels." + ch + ".playing");
        return playing instanceof Boolean b && b;
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int ch = 0; ch < CHANNELS && !active; ch++) active = sounding(ch);
        }
        return active;
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

        boolean sounding = sounding(ch);
        int freq = intOf(ch, "freq");
        int left = intOf(ch, "volumeL");
        int right = intOf(ch, "volumeR");

        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || freq != prevFreqs[ch]);
        prevSoundings[ch] = sounding;
        prevFreqs[ch] = freq;

        out.volume = Math.max(left, right);
        out.amplitude = sounding ? Math.max(left, right) / volumeMax : 0;
        out.ssgTone = ch != 3;
        out.ssgNoise = ch == 3;
        out.pan = PcmSlotReader.panOf(left, right);
        // the noise has a shift register rather than a pitch, and the wave channel steps through
        // thirty two samples where a square has two
        out.note = ch == 3 || !sounding || freq >= 2048 ? -1
                : Notes.noteOf(squareBase / (2048 - freq) / (ch == 2 ? 2 : 1));
    }

    @Override
    public boolean masked(Group group, int ch) {
        return chip().getMask(chipId, ch);
    }
}
