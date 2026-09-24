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

import mdplayer.chips.Msm5232Chip;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * OKI MSM5232, the organ chip of Taito's and Irem's early boards: eight square wave voices in two
 * groups of four.
 * <p>
 * A voice is keyed on with a pitch code, one semitone a step, which the chip turns into a counter
 * it takes four footages off, 16' to 2', an octave apart. The note shown is the 8' one, which is
 * where the datasheet tunes code {@code 0x21} to A4 at a 2119040 Hz clock; another clock
 * transposes every code by the same ratio. Codes {@code 0x55} to {@code 0x57} are not in the scale
 * (the chip repeats the top note and then drops far below it), and {@code 0xd8} and above play the
 * noise generator instead, which has no note.
 * <p>
 * The envelope is an external capacitor, charged for the attack and discharged for the decay or
 * the release, and a voice sounds while it holds any charge - after the key is up too - and while
 * its group has at least one footage output enabled. The charge is the level.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-24 nsano initial version <br>
 */
public class Msm5232Reader extends ChipReader {

    private static final int CHANNELS = Msm5232Chip.CHANNELS;

    /** the clock the datasheet's tuning is given at */
    private static final double referenceClock = 2119040;

    /** the pitch code of A4 on the 8' output at {@link #referenceClock} */
    private static final int a4 = 0x21;

    /** the highest pitch code in the scale */
    private static final int topCode = 0x54;

    /** the envelope's full charge */
    private static final double egMax = 2048;

    private final boolean[] prevKeys = new boolean[CHANNELS];
    private final int[] prevPitches = new int[CHANNELS];
    private boolean active;

    private Map<String, Object> info = Collections.emptyMap();

    @Override
    protected Msm5232Chip chip() {
        return chipRegister.chip(Msm5232Chip.class);
    }

    @Override
    public String chipName() {
        return "MSM";
    }

    @Override
    public void reset() {
        Arrays.fill(prevKeys, false);
        Arrays.fill(prevPitches, -1);
        active = false;
        info = Collections.emptyMap();
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 94;
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
     * The 8' output's frequency of a pitch code, -1 for one outside the scale.
     *
     * @param clock the chip's clock
     */
    static double frequencyOf(int code, int clock) {
        if (code < 0 || code > topCode) return -1;
        return 440 * Math.pow(2, (code - a4) / 12.0) * clock / referenceClock;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> voice(int ch) {
        Object voices = info.get("voices");
        return voices instanceof List<?> list && ch < list.size() ? (Map<String, Object>) list.get(ch) : Collections.emptyMap();
    }

    private static int intOf(Map<String, Object> map, String key, int defaultValue) {
        return map.get(key) instanceof Integer i ? i : defaultValue;
    }

    private static boolean boolOf(Map<String, Object> map, String key) {
        return map.get(key) instanceof Boolean b && b;
    }

    /** whether the voice's group has any of its four footage outputs enabled */
    private boolean groupEnabled(int ch) {
        return (intOf(info, ch < 4 ? "control1" : "control2", 0) & 0x0f) != 0;
    }

    private boolean sounding(int ch) {
        Map<String, Object> v = voice(ch);
        return intOf(v, "egSection", -1) >= 0 && intOf(v, "egVolume", 0) > 0 && groupEnabled(ch);
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

        Map<String, Object> v = voice(ch);
        boolean key = boolOf(v, "keyOn");
        boolean noise = boolOf(v, "noise");
        int pitch = intOf(v, "pitch", -1);
        int eg = intOf(v, "egVolume", 0);
        boolean sounding = sounding(ch);

        out.sounding = sounding;
        out.keyOn = key && (!prevKeys[ch] || pitch != prevPitches[ch]);
        prevKeys[ch] = key;
        prevPitches[ch] = pitch;

        out.ssgTone = !noise;
        out.ssgNoise = noise;
        out.note = sounding && !noise ? Notes.noteOf(frequencyOf(pitch, intOf(info, "clock", (int) referenceClock))) : -1;
        out.volume = eg >> 7; // 0..16
        out.amplitude = sounding ? eg / egMax : 0;
    }

    @Override
    public boolean masked(Group group, int ch) {
        return chip().getMask(chipId, ch);
    }
}
