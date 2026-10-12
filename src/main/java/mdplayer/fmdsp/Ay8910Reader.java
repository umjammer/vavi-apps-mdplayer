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

import mdplayer.chips.Ay8910Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * PSG (AY-3-8910 / YM2149): the same register file as the OPN family's SSG section, on the SSG
 * rows.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class Ay8910Reader extends ChipReader {

    /** the PSG's usual MSX clock [Hz] */
    private static final double clock = 1789772.5;

    private final boolean[] prevSoundings = new boolean[3];
    private final int[] prevPeriods = new int[3];
    private boolean active;

    /** the chip's register file, read back once a frame */
    private int[] regs;

    @Override
    protected Ay8910Chip chip() {
        return chipRegister.chip(Ay8910Chip.class);
    }

    @Override
    public String chipName() {
        return "PSG";
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevPeriods, 0);
        active = false;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.SSG);
    }

    @Override
    public int priority() {
        return 85;
    }

    @Override
    public void poll() {
        regs = null;
        try {
            Map<String, Object> info = chip().getInfo(chipId);
            if (info.get("register") instanceof int[] r) regs = r;
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
        }
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int s = 0; s < 3 && !active; s++) active = sounding(s);
        }
        return active;
    }

    private boolean sounding(int s) {
        int[] regs = this.regs;
        if (regs == null) return false;
        int level = regs[0x08 + s] & 0x1f;
        boolean tone = (regs[0x07] & (1 << s)) == 0;
        boolean noise = (regs[0x07] & (8 << s)) == 0;
        return (tone || noise) && level > 0;
    }

    @Override
    public int channels(Group group) {
        return 3;
    }

    @Override
    public void read(Group group, int s, FmDspChannel out) {
        int[] regs = this.regs;
        if (regs == null) return;
        int period = (regs[s * 2] & 0xff) | ((regs[s * 2 + 1] & 0x0f) << 8);
        int level = regs[0x08 + s] & 0x1f;
        // a part on the hardware envelope has no readable level, meter it at full scale
        if ((level & 0x10) != 0) level = 15;
        boolean tone = (regs[0x07] & (1 << s)) == 0;
        boolean noise = (regs[0x07] & (8 << s)) == 0;
        boolean sounding = (tone || noise) && level > 0;

        out.name = "SSG";
        out.num = s + 1;
        out.info = (regs[0x08 + s] & 0x10) != 0 ? TrackInfo.SSGEFF : TrackInfo.SSG;
        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[s] || period != prevPeriods[s]);
        prevSoundings[s] = sounding;
        prevPeriods[s] = period;

        out.pitch(period > 0 ? clock / (16.0 * period) : 0);
        out.volume = level;
        out.ssgTone = tone;
        out.ssgNoise = noise;
        out.ssgNoiseFreq = regs[0x06] & 0x1f;
        // one step of the 16 level table is 3 dB
        out.amplitude = Math.pow(10, (Math.min(level, 15) - 15) * 3.0 / 20);
        out.pan = Pan.CENTER;
    }
}
