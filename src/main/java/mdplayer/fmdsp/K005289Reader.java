/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mdplayer.chips.K005289Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * Konami 005289, the two wavetable channels of the Bubble System and Nemesis (Gradius) boards.
 * <p>
 * A channel steps through the 32 samples of its wave once every {@code freq + 1} clocks, so it
 * plays {@code clock / (32 * (freq + 1))} Hz, a true note. The chip has no key: a channel sounds
 * while the board's AY-3-8910 gives it a volume, which is four bits. It is mono.
 * <p>
 * The board's two AY-3-8910s take the SSG rows, so these take the FM block.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-24 nsano initial version <br>
 */
public class K005289Reader extends ChipReader {

    private static final int CHANNELS = K005289Chip.CHANNELS;

    /** the Nemesis board's clock */
    private static final int defaultClock = 3579545;

    /** the volume is four bits */
    private static final double volumeMax = 15;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevFreqs = new int[CHANNELS];
    private boolean active;

    private Map<String, Object> info = Collections.emptyMap();

    @Override
    protected K005289Chip chip() {
        return chipRegister.chip(K005289Chip.class);
    }

    @Override
    public String chipName() {
        return "K005";
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevFreqs, -1);
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

    /** the frequency a channel plays at a {@code freq} */
    static double frequencyOf(int freq, int clock) {
        return clock / (32.0 * (freq + 1));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> voice(int ch) {
        Object voices = info.get("voices");
        return voices instanceof List<?> list && ch < list.size() ? (Map<String, Object>) list.get(ch) : Collections.emptyMap();
    }

    private static int intOf(Map<String, Object> map, String key, int defaultValue) {
        return map.get(key) instanceof Integer i ? i : defaultValue;
    }

    /** a period of no clock at all is the chip's idea of silence, the board's is volume 0 */
    private boolean sounding(int ch) {
        Map<String, Object> v = voice(ch);
        return intOf(v, "volume", 0) > 0 && intOf(v, "freq", 0) > 0;
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

        Map<String, Object> v = voice(ch);
        boolean sounding = sounding(ch);
        int freq = intOf(v, "freq", 0);
        int volume = intOf(v, "volume", 0);

        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || freq != prevFreqs[ch]);
        prevSoundings[ch] = sounding;
        prevFreqs[ch] = freq;

        out.volume = volume;
        out.amplitude = sounding ? volume / volumeMax : 0;
        out.ssgTone = true;
        out.note = sounding ? Notes.noteOf(frequencyOf(freq, intOf(info, "clock", defaultClock))) : -1;
    }

    @Override
    public boolean masked(Group group, int ch) {
        return chip().getMask(chipId, ch);
    }
}
