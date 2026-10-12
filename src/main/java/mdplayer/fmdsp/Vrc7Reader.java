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


/**
 * Konami VRC7, an NSF expansion: an OPLL with six of its nine channels wired up and no rhythm
 * mode, so the register map is the YM2413's.
 * <ul>
 * <li>{@code 0x10+ch} the low eight bits of the F-number</li>
 * <li>{@code 0x20+ch} its ninth bit, the block in bits 1 to 3 and the key on in bit 4</li>
 * <li>{@code 0x30+ch} the instrument in the high nibble and a four bit attenuator in the low</li>
 * </ul>
 * The chip is mono.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class Vrc7Reader extends ChipReader {

    private static final int CHANNELS = 6;

    /** the NES CPU clock the chip is wired to, which its F-numbers are measured against */
    private static final double clock = 1789773;

    private int[] regs;

    private final boolean[] prevOns = new boolean[CHANNELS];
    private final int[] prevFnums = new int[CHANNELS];
    private boolean active;

    @Override
    protected NpNesChip.Vrc7Chip chip() {
        return chipRegister.chip(NpNesChip.Vrc7Chip.class);
    }

    @Override
    public String chipName() {
        return "VRC7";
    }

    @Override
    public void reset() {
        Arrays.fill(prevOns, false);
        Arrays.fill(prevFnums, 0);
        active = false;
        regs = null;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 81;
    }

    @Override
    public void poll() {
        regs = null;
        try {
            Map<String, Object> info = chip().getInfo(chipId);
            if (info != null && info.get("register") instanceof int[] r) regs = r;
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
        }
    }

    private boolean on(int ch) {
        return regs != null && 0x20 + ch < regs.length && (regs[0x20 + ch] & 0x10) != 0;
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int ch = 0; ch < CHANNELS && !active; ch++) active = on(ch);
        }
        return active;
    }

    @Override
    public int channels(Group group) {
        return CHANNELS;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        out.name = "FM";
        out.num = ch + 1;
        out.pan = Pan.CENTER; // the chip is mono
        if (regs == null || 0x30 + ch >= regs.length) return;

        int reg2 = regs[0x20 + ch];
        boolean on = (reg2 & 0x10) != 0;
        int fnum = (regs[0x10 + ch] & 0xff) | ((reg2 & 0x01) << 8);
        int block = (reg2 >> 1) & 0x07;

        out.sounding = on;
        out.keyOn = on && (!prevOns[ch] || fnum != prevFnums[ch]);
        prevOns[ch] = on;
        prevFnums[ch] = fnum;

        out.pitch(on && fnum > 0
                ? fnum * Math.pow(2, block - 1) * clock / (1 << 18) : 0);
        // three dB a step of the sixteen level attenuator
        int vol = regs[0x30 + ch] & 0x0f;
        out.volume = 15 - vol;
        out.amplitude = on ? Math.pow(10, -vol * 3.0 / 20) : 0;
        out.toneNum = (regs[0x30 + ch] >> 4) & 0x0f;
    }
}
