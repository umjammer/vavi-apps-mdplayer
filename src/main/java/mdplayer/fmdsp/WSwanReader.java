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

import mdplayer.chips.WSwanChip;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * The WonderSwan's sound: four wavetable channels.
 * <p>
 * Each steps a thirty two sample table at {@code clock / 128 / (2048 - divider)} Hz, so unlike the
 * sampled chips these are real pitches. Levels are four bits a side, which is also the pan.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class WSwanReader extends ChipReader {

    private static final int CHANNELS = 4;

    /** the level registers are four bits each */
    private static final double volumeMax = 15;

    private Map<String, Object> info;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevDividers = new int[CHANNELS];
    private boolean active;

    @Override
    protected WSwanChip chip() {
        return chipRegister.chip(WSwanChip.class);
    }

    @Override
    public String chipName() {
        return "WSW";
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevDividers, 0);
        active = false;
        info = Collections.emptyMap();
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 87;
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
        if (info.isEmpty()) return false;
        Object enable = info.get("channels." + ch + ".enable");
        return enable instanceof Boolean b && b
                && Math.max(intOf(ch, "volumeL"), intOf(ch, "volumeR")) > 0;
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
        int divider = intOf(ch, "divider");
        int left = intOf(ch, "volumeL");
        int right = intOf(ch, "volumeR");
        int clock = info.get("clock") instanceof Integer c ? c : 0;

        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || divider != prevDividers[ch]);
        prevSoundings[ch] = sounding;
        prevDividers[ch] = divider;

        out.volume = Math.max(left, right);
        out.amplitude = sounding ? Math.max(left, right) / volumeMax : 0;
        out.ssgTone = true;
        out.pan = PcmSlotReader.panOf(left, right);
        out.note = sounding && divider > 0 && divider < 2048
                ? Notes.noteOf(clock / 128.0 / (2048 - divider)) : -1;
    }
}
