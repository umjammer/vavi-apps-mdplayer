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

import mdplayer.chips.BaseChip;
import mdplayer.chips.Ym2413Chip;


/**
 * OPLL: nine channels with the note from the F-number and the instrument as the tone number. In
 * rhythm mode channels 7-9 turn into the drums, which hit the drum meter instead.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class Ym2413Reader extends ChipReader {

    /** the OPLL tick [Hz], clock / 72 */
    private static final double tick = 3579545.0 / 72;

    private final boolean[] prevOns = new boolean[9];
    private final int[] prevFnums = new int[9];
    private int prevRhythmBits;
    private boolean fmActive;

    /** the chip's register file, read back once a frame */
    private int[] regs;
    private boolean rhythmActive;

    @Override
    protected Ym2413Chip chip() {
        return chipRegister.chip(Ym2413Chip.class);
    }

    @Override
    public String chipName() {
        return "OPLL";
    }

    @Override
    public void reset() {
        Arrays.fill(prevOns, false);
        Arrays.fill(prevFnums, 0);
        prevRhythmBits = 0;
        fmActive = false;
        rhythmActive = false;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM, Group.RHYTHM);
    }

    @Override
    public int priority() {
        return 80;
    }

    private boolean rhythmMode() {
        return regs != null && (regs[0x0e] & 0x20) != 0;
    }

    @Override
    public void poll() {
        regs = null;
        try {
            Map<String, Object> info = chip().getInfo(chipId);
            if (!info.isEmpty() && info.get("register") instanceof int[] r) regs = r;
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
        }
    }

    @Override
    public boolean active(Group group) {
        int[] regs = this.regs;
        if (regs == null) return false;
        if (group == Group.RHYTHM) {
            if (!rhythmActive) rhythmActive = rhythmMode() && (regs[0x0e] & 0x1f) != 0;
            return rhythmActive;
        }
        if (!fmActive) {
            for (int ch = 0; ch < 9 && !fmActive; ch++) fmActive = (regs[0x20 + ch] & 0x10) != 0;
        }
        return fmActive;
    }

    @Override
    public int channels(Group group) {
        return group == Group.FM ? 9 : 1;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        if (regs == null) return;
        if (group == Group.RHYTHM) {
            readRhythm(out);
            return;
        }
        // in rhythm mode channels 7-9 are the drums, their melodic rows go dark
        if (rhythmMode() && ch >= 6) {
            prevOns[ch] = false;
            return;
        }
        out.name = "FM";
        out.num = ch + 1;
        int[] regs = this.regs;
        int reg2 = regs[0x20 + ch];
        boolean on = (reg2 & 0x10) != 0;
        int fnum = (regs[0x10 + ch] & 0xff) | ((reg2 & 0x01) << 8);
        int block = (reg2 >> 1) & 0x07;

        out.sounding = on;
        out.keyOn = on && (!prevOns[ch] || fnum != prevFnums[ch]);
        prevOns[ch] = on;
        prevFnums[ch] = fnum;

        out.pitch(fnum > 0 ? fnum * Math.pow(2, block - 1) * tick / (1 << 18) : 0);
        // 3 dB per step of the 16 level attenuator
        int vol = regs[0x30 + ch] & 0x0f;
        out.volume = 15 - vol;
        out.amplitude = Math.pow(10, -vol * 3.0 / 20);
        out.toneNum = (regs[0x30 + ch] >> 4) & 0x0f;
    }

    private void readRhythm(FmDspChannel out) {
        int bits = rhythmMode() ? regs[0x0e] & 0x1f : 0;
        // a drum keys on by its bit rising
        out.keyOn = (bits & ~prevRhythmBits) != 0;
        prevRhythmBits = bits;
        out.sounding = false;
    }

    @Override
    public BaseChip.Channel muteChannel(Group group, int ch) {
        // the drums share the one meter, and have a channel each
        return group == Group.FM ? super.muteChannel(group, ch) : null;
    }
}
