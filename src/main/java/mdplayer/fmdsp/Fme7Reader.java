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
 * Sunsoft 5B, an NSF expansion: an AY compatible with three square channels, and the same register
 * map - a twelve bit period over two registers a channel, the mixer at {@code 0x07} with tone and
 * noise enables that are active low, and a four bit level each.
 * <p>
 * A channel counts {@code clock / (16 * period)} Hz, so these are real pitches. The chip is mono.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class Fme7Reader extends ChipReader {

    private static final int CHANNELS = 3;

    /** the NES CPU clock the chip is wired to */
    private static final double clock = 1789773;

    private byte[] regs;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevPeriods = new int[CHANNELS];
    private boolean active;

    @Override
    protected NpNesChip.Fme7Chip chip() {
        return chipRegister.chip(NpNesChip.Fme7Chip.class);
    }

    @Override
    public String chipName() {
        return "5B";
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevPeriods, 0);
        active = false;
        regs = null;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 83;
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

    private int periodOf(int ch) {
        if (regs == null) return 0;
        return (regs[ch * 2] & 0xff) | ((regs[ch * 2 + 1] & 0x0f) << 8);
    }

    private int volumeOf(int ch) {
        return regs == null ? 0 : regs[0x08 + ch] & 0x0f;
    }

    /** the mixer's enables are active low, so a clear bit is a channel that is on */
    private boolean tone(int ch) {
        return regs != null && (regs[0x07] & (1 << ch)) == 0;
    }

    private boolean noise(int ch) {
        return regs != null && (regs[0x07] & (1 << (ch + 3))) == 0;
    }

    private boolean sounding(int ch) {
        return regs != null && volumeOf(ch) > 0 && (tone(ch) || noise(ch));
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
        out.pan = Pan.CENTER; // the chip is mono
        if (regs == null) return;
        out.info = (regs[0x08 + ch] & 0x10) != 0 ? TrackInfo.SSGEFF : TrackInfo.SSG;

        boolean sounding = sounding(ch);
        int period = periodOf(ch);
        int volume = volumeOf(ch);

        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || period != prevPeriods[ch]);
        prevSoundings[ch] = sounding;
        prevPeriods[ch] = period;

        out.volume = volume;
        out.amplitude = sounding ? volume / 15.0 : 0;
        out.ssgTone = tone(ch);
        out.ssgNoise = noise(ch);
        out.ssgNoiseFreq = regs[0x06] & 0x1f;
        out.note = sounding && tone(ch) && period > 0
                ? Notes.noteOf(clock / (16.0 * period)) : -1;
    }
}
