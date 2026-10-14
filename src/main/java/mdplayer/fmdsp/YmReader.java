/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

import mdplayer.ChipRegister;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.ym.YmDriver;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * The Atari ST's YM2149, as the {@code .ym} driver plays it.
 * <p>
 * This one registers no chip: the driver renders the song itself and keeps the PSG inside, so the
 * registers are read from the driver rather than from {@link ChipRegister}. They are an AY8910's -
 * a twelve bit period over two registers a channel, the mixer at {@code 0x07} whose enables are
 * active low, and a level each.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class YmReader implements FmDspChipReader {

    private static final int CHANNELS = 3;

    /** the Atari's YM2149 clock */
    private static final double clock = 2000000;

    private Supplier<BaseDriver> driver;

    private int[] regs;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevPeriods = new int[CHANNELS];
    private boolean active;

    @Override
    public String chipName() {
        return "YM";
    }

    @Override
    public void bind(ChipRegister chipRegister) {
    }

    @Override
    public void bind(Supplier<BaseDriver> driver) {
        this.driver = driver;
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
        return EnumSet.of(Group.SSG);
    }

    @Override
    public int priority() {
        return 30;
    }

    @Override
    public boolean ready() {
        return driver != null && driver.get() instanceof YmDriver;
    }

    @Override
    public void poll() {
        regs = driver.get() instanceof YmDriver ym ? ym.getPsgRegisters() : null;
    }

    /** the mixer's enables are active low, so a clear bit is a channel that is on */
    private boolean tone(int s) {
        return regs != null && (regs[0x07] & (1 << s)) == 0;
    }

    private boolean noise(int s) {
        return regs != null && (regs[0x07] & (8 << s)) == 0;
    }

    private int level(int s) {
        return regs == null ? 0 : regs[0x08 + s] & 0x1f;
    }

    private boolean sounding(int s) {
        return regs != null && (tone(s) || noise(s)) && level(s) > 0;
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int s = 0; s < CHANNELS && !active; s++) active = sounding(s);
        }
        return active;
    }

    @Override
    public int channels(Group group) {
        return CHANNELS;
    }

    @Override
    public void read(Group group, int s, FmDspChannel out) {
        out.name = "SSG";
        out.num = s + 1;
        out.pan = Pan.CENTER; // the ST mixes its three channels to one output
        if (regs == null) return;
        out.info = (regs[0x08 + s] & 0x10) != 0 ? TrackInfo.SSGEFF : TrackInfo.SSG;

        int period = (regs[s * 2] & 0xff) | ((regs[s * 2 + 1] & 0x0f) << 8);
        int level = level(s);
        // a channel on the hardware envelope has no readable level, meter it at full scale
        if ((level & 0x10) != 0) level = 15;
        boolean sounding = sounding(s);

        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[s] || period != prevPeriods[s]);
        prevSoundings[s] = sounding;
        prevPeriods[s] = period;

        out.volume = level;
        out.ssgTone = tone(s);
        out.ssgNoise = noise(s);
        out.ssgNoiseFreq = regs[0x06] & 0x1f;
        out.note = sounding && period > 0 ? Notes.noteOf(clock / (16.0 * period)) : -1;
        // one step of the PSG's sixteen level table is 3 dB
        out.amplitude = sounding ? Math.pow(10, (Math.min(level, 15) - 15) * 3.0 / 20) : 0;
    }
}
