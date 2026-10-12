/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import mdplayer.chips.NpNesChip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * Nintendo MMC5, an NSF expansion: two pulses laid out like the NES APU's own, and a PCM register
 * that is written to directly rather than played from a sample.
 * <p>
 * A pulse counts {@code 1789773 / (16 * (timer + 1))} Hz, so these are real pitches. The chip is
 * mono, and the two pulses take the wider block rather than the SSG rows so they sit beside the
 * APU's four.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class Mmc5Reader extends ChipReader {

    private static final int CHANNELS = 2;

    /** the NTSC CPU clock the pulses divide */
    private static final double cpuClock = 1789773;

    private byte[] regs;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevTimers = new int[CHANNELS];
    private boolean active;

    @Override
    protected NpNesChip.Mmc5Chip chip() {
        return chipRegister.chip(NpNesChip.Mmc5Chip.class);
    }

    @Override
    public String chipName() {
        return "MMC5";
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevTimers, 0);
        active = false;
        regs = null;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 84;
    }

    @Override
    public void poll() {
        regs = null;
        try {
            Map<String, Object> info = chip().getInfo(chipId);
            if (info != null && info.get("register") instanceof byte[] r) regs = r;
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
        }
    }

    private int timerOf(int ch) {
        if (regs == null) return 0;
        int base = ch * 4;
        return (regs[base + 2] & 0xff) | ((regs[base + 3] & 0x07) << 8);
    }

    private int volumeOf(int ch) {
        return regs == null ? 0 : regs[ch * 4] & 0x0f;
    }

    private boolean sounding(int ch) {
        return regs != null && volumeOf(ch) > 0 && timerOf(ch) > 0;
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
        if (regs == null) return;

        boolean sounding = sounding(ch);
        int timer = timerOf(ch);
        int volume = volumeOf(ch);

        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || timer != prevTimers[ch]);
        prevSoundings[ch] = sounding;
        prevTimers[ch] = timer;

        out.volume = volume;
        out.amplitude = sounding ? volume / 15.0 : 0;
        out.ssgTone = true;
        out.toneNum = (regs[ch * 4] >> 6) & 0x03; // the duty, as an instrument would be
        out.note = sounding ? Notes.noteOf(cpuClock / (16.0 * (timer + 1))) : -1;
    }
}
