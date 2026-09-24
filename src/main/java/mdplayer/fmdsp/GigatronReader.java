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

import mdplayer.chips.GigatronChip;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * Gigatron, the TTL microcomputer whose rom makes four wave table channels in its video loop.
 * <p>
 * A channel adds its 14 bit key to a 15 bit phase each time the rom serves it, once per 4
 * scanlines when the channel mask is 3, and the top 6 bits of the phase index a 64 step wave:
 * noise, triangle, pulse or sawtooth by the low 2 bits of wavX. So a note is
 * {@code key * servings * (clock / 4) / 32768} Hz. There is no key on and no volume register, a
 * song keys off by zeroing the key (the phase then stands still), so a channel sounds while its
 * output moves, and the level shown is how far it moves.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-24 nsano initial version <br>
 */
public class GigatronReader extends ChipReader {

    private static final int CHANNELS = GigatronChip.CHANNELS;

    /** 521 scanlines * 59.98 Hz */
    private static final int defaultClock = 31250;

    /** the phase's period */
    private static final double phaseMax = 1 << 15;

    /** the wave's peak to peak */
    private static final double levelMax = 63;

    private final int[] prevKeys = new int[CHANNELS];
    private boolean active;

    private Map<String, Object> info = Collections.emptyMap();

    @Override
    protected GigatronChip chip() {
        return chipRegister.chip(GigatronChip.class);
    }

    @Override
    public String chipName() {
        return "GTR";
    }

    @Override
    public void reset() {
        Arrays.fill(prevKeys, 0);
        active = false;
        info = Collections.emptyMap();
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 89;
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

    /**
     * @param clock scanlines per second
     * @param servings how many of a tick's 4 slots serve the channel
     */
    static double frequencyOf(int key, int servings, int clock) {
        return key * servings * (clock / 4.0) / phaseMax;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> channel(int ch) {
        Object channels = info.get("channels");
        return channels instanceof List<?> list && ch < list.size() ? (Map<String, Object>) list.get(ch) : Collections.emptyMap();
    }

    private static int intOf(Map<String, Object> map, String key, int defaultValue) {
        return map.get(key) instanceof Integer i ? i : defaultValue;
    }

    private boolean sounding(int ch) {
        Map<String, Object> c = channel(ch);
        return intOf(c, "key", 0) != 0 && intOf(c, "servings", 0) > 0 && intOf(c, "level", 0) > 0;
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

        Map<String, Object> c = channel(ch);
        int key = intOf(c, "key", 0);
        boolean noise = (intOf(c, "wavX", 0) & 3) == 0;
        int level = intOf(c, "level", 0);
        boolean sounding = sounding(ch);

        out.sounding = sounding;
        out.keyOn = key != 0 && key != prevKeys[ch];
        prevKeys[ch] = key;

        out.ssgTone = !noise;
        out.ssgNoise = noise;
        out.note = sounding && !noise
                ? Notes.noteOf(frequencyOf(key, intOf(c, "servings", 1), intOf(info, "clock", defaultClock))) : -1;
        out.volume = level >> 2; // 0..15
        out.amplitude = sounding ? level / levelMax : 0;
    }

    @Override
    public boolean masked(Group group, int ch) {
        return chip().getMask(chipId, ch);
    }
}
