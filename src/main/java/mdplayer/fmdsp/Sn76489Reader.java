/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import mdplayer.chips.Sn76489Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * DCSG (SN76489): the three squares on the SSG rows, the noise channel as the third row's noise
 * badge. The Game Gear stereo latch feeds the pans.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class Sn76489Reader extends ChipReader {

    /** the DCSG's usual NTSC clock [Hz] */
    private static final double clock = 3579545;

    private final boolean[] prevSoundings = new boolean[3];
    private final int[] prevPeriods = new int[3];
    private boolean active;

    /** the chip's eight registers, read back once a frame */
    private int[] regs = new int[8];

    @Override
    protected Sn76489Chip chip() {
        return chipRegister.chip(Sn76489Chip.class);
    }

    @Override
    public String chipName() {
        return "DCSG";
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
        return 90;
    }

    @Override
    public void poll() {
        try {
            Object value = chip().getInfo(chipId).get("register");
            regs = value instanceof int[] r ? r : new int[8];
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
            regs = new int[8];
        }
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int ch = 0; ch < 3 && !active; ch++) active = sounding(ch);
        }
        return active;
    }

    private boolean sounding(int ch) {
        int[] regs = this.regs;
        return regs[ch * 2] > 0 && (regs[ch * 2 + 1] & 0x0f) < 15;
    }

    @Override
    public int channels(Group group) {
        return 3;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        int[] regs = this.regs;
        int period = regs[ch * 2];
        int vol = regs[ch * 2 + 1] & 0x0f;
        boolean sounding = period > 0 && vol < 15;

        out.name = "SSG";
        out.num = ch + 1;
        out.info = TrackInfo.SSG;
        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || period != prevPeriods[ch]);
        prevSoundings[ch] = sounding;
        prevPeriods[ch] = period;

        out.pitch(period > 0 ? clock / (32.0 * period) : 0);
        out.volume = 15 - vol;
        out.ssgTone = sounding;
        // the noise channel has no row of its own, it badges the third one
        out.ssgNoise = ch == 2 && (regs[7] & 0x0f) < 15;
        // one attenuator step is 2 dB
        out.amplitude = Math.pow(10, -vol * 2.0 / 20);
        out.pan = panOf(ch);
    }

    /** the Game Gear stereo latch, a left enable in the high nibble and a right one in the low */
    private Pan panOf(int ch) {
        int pan = chip().pan[0];
        boolean left = (pan & (1 << (ch + 4))) != 0;
        boolean right = (pan & (1 << ch)) != 0;
        if (left && right) return Pan.CENTER;
        if (left) return Pan.LEFT;
        if (right) return Pan.RIGHT;
        return Pan.NONE;
    }
}
