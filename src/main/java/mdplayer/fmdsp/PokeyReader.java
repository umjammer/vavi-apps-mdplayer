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

import mdplayer.chips.PokeyChip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * Atari POKEY: four channels counting down a divider, so {@code clock / (2 * (divisor + 1))} Hz.
 * <p>
 * A channel can be told to output its volume alone rather than a tone, which the emulator folds
 * into its audible flag, so that is what says whether one is sounding. The chip is mono.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class PokeyReader extends ChipReader {

    private static final int CHANNELS = 4;

    /** the NTSC clock the chip divides */
    private static final double clock = 1789790;

    /** the level register is four bits */
    private static final double volumeMax = 15;

    private Map<String, Object> info;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevDivisors = new int[CHANNELS];
    private boolean active;

    @Override
    protected PokeyChip chip() {
        return chipRegister.chip(PokeyChip.class);
    }

    @Override
    public String chipName() {
        return "POKE";
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevDivisors, 0);
        active = false;
        info = Collections.emptyMap();
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 80;
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
        Object audible = info.get("channels." + ch + ".audible");
        return audible instanceof Boolean b && b && intOf(ch, "volume") > 0;
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
        int divisor = intOf(ch, "divisor");
        int volume = intOf(ch, "volume");

        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || divisor != prevDivisors[ch]);
        prevSoundings[ch] = sounding;
        prevDivisors[ch] = divisor;

        out.volume = volume;
        out.amplitude = sounding ? volume / volumeMax : 0;
        out.ssgTone = true;
        out.note = sounding && divisor > 0 ? Notes.noteOf(clock / (2.0 * (divisor + 1))) : -1;
    }
}
