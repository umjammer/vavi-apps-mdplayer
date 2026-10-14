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

import mdplayer.chips.K051649Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * Konami K051649, the SCC: five wavetable channels.
 * <p>
 * Unlike the sampled chips these have a real pitch - a channel counts
 * {@code clock / (16 * (frequency + 1))} Hz - so the keys here are true notes. The volume is four
 * bits and the chip is mono.
 * <p>
 * Five channels do not fit the three SSG rows, so they take the wider FM block, labelled for what
 * they are.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class K051649Reader extends ChipReader {

    private static final int CHANNELS = 5;

    /** the chip's usual clock, an MSX bus clock */
    private static final double clock = 3579545;

    /** the volume register is four bits */
    private static final double volumeMax = 15;

    private Map<String, Object> info;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevFreqs = new int[CHANNELS];
    private boolean active;

    @Override
    protected K051649Chip chip() {
        return chipRegister.chip(K051649Chip.class);
    }

    @Override
    public String chipName() {
        return "SCC";
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevFreqs, 0);
        active = false;
        info = Collections.emptyMap();
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 90;
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
        // a frequency of eight or less is the chip's own idea of silence
        return intOf(ch, "key") != 0 && intOf(ch, "volumeL") > 0
                && intOf(ch, "frequency") > 8;
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
        out.pan = Pan.CENTER; // the chip is mono
        if (info.isEmpty()) return;

        boolean sounding = sounding(ch);
        int freq = intOf(ch, "frequency");
        int volume = intOf(ch, "volumeL");

        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || freq != prevFreqs[ch]);
        prevSoundings[ch] = sounding;
        prevFreqs[ch] = freq;

        out.volume = volume;
        out.amplitude = sounding ? volume / volumeMax : 0;
        out.ssgTone = true;
        out.note = sounding ? Notes.noteOf(clock / (16.0 * (freq + 1))) : -1;
    }
}
