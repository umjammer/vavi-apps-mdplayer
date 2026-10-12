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
import mdplayer.driver.sid.SidMdDriver;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * The C64's SID, as the {@code .sid} driver plays it.
 * <p>
 * The driver renders its own audio and registers no chip, so the registers come from it. Seven a
 * voice:
 * <ul>
 * <li>{@code +0} and {@code +1} the sixteen bit frequency, which counts
 * {@code freq * clock / 16777216} Hz</li>
 * <li>{@code +4} the control: the waveform in the high nibble and the gate in bit 0</li>
 * <li>{@code +6} the sustain level in its high nibble, which is the nearest thing to a volume</li>
 * </ul>
 * with the chip's master volume in the low nibble of {@code 0x18}. The SID is mono.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class SidReader implements FmDspChipReader {

    private static final int VOICES = 3;

    /** seven registers a voice */
    private static final int STRIDE = 7;

    /** the PAL C64's clock; the phase accumulator is 24 bit */
    private static final double clock = 985248;

    private Supplier<BaseDriver> driver;

    private Integer[][] regs;

    private final boolean[] prevGates = new boolean[VOICES];
    private final int[] prevFreqs = new int[VOICES];
    private boolean active;

    @Override
    public String chipName() {
        return "SID";
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
        Arrays.fill(prevGates, false);
        Arrays.fill(prevFreqs, 0);
        active = false;
        regs = null;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.SSG);
    }

    @Override
    public int priority() {
        return 29;
    }

    @Override
    public boolean ready() {
        return driver != null && driver.get() instanceof SidMdDriver;
    }

    @Override
    public void poll() {
        regs = driver.get() instanceof SidMdDriver sid ? sid.getRegisterFromSid() : null;
    }

    private int reg(int i) {
        return regs != null && regs.length > 0 && regs[0] != null && i < regs[0].length
                && regs[0][i] != null ? regs[0][i] & 0xff : 0;
    }

    /** the gate bit, which is what a SID voice calls a key */
    private boolean gate(int v) {
        return (reg(v * STRIDE + 4) & 0x01) != 0;
    }

    private int freq(int v) {
        return reg(v * STRIDE) | (reg(v * STRIDE + 1) << 8);
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int v = 0; v < VOICES && !active; v++) active = gate(v);
        }
        return active;
    }

    @Override
    public int channels(Group group) {
        return VOICES;
    }

    @Override
    public void read(Group group, int v, FmDspChannel out) {
        out.name = "SSG";
        out.num = v + 1;
        out.info = TrackInfo.SSG;
        out.pan = Pan.CENTER; // the SID mixes its three voices to one output
        if (regs == null) return;

        boolean gate = gate(v);
        int freq = freq(v);
        int control = reg(v * STRIDE + 4);

        out.sounding = gate;
        out.keyOn = gate && (!prevGates[v] || freq != prevFreqs[v]);
        prevGates[v] = gate;
        prevFreqs[v] = freq;

        // the sustain level is the closest the register file has to a part volume
        int sustain = (reg(v * STRIDE + 6) >> 4) & 0x0f;
        int master = reg(0x18) & 0x0f;
        out.volume = sustain;
        out.amplitude = gate ? sustain * master / (15.0 * 15) : 0;
        // the waveform stands in for the tone number, the way an FM part shows its instrument
        out.toneNum = (control >> 4) & 0x0f;
        // the noise waveform has no pitch worth showing
        out.ssgNoise = (control & 0x80) != 0;
        out.ssgTone = (control & 0x70) != 0;
        out.note = gate && freq > 0 && out.ssgTone
                ? Notes.noteOf(freq * clock / (1 << 24)) : -1;
    }
}
