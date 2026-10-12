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
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;


/**
 * The OPL family's shared shape: nine two operator channels.
 * <p>
 * The chips are read through their own channel view rather than a copy of the register writes,
 * because the family is emulated several ways and they do not agree on what they keep - some hold
 * the register file, others decode it into operators and throw it away. Every one of them can say
 * what its channels are doing, so that is what this asks for.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public abstract class OplReader extends ChipReader {

    /** the OPL tick [Hz], clock / 72 - every family member's standard clock lands here */
    private static final double tick = 3579545.0 / 72;

    private final boolean[] prevOns = new boolean[9];
    private final int[] prevFnums = new int[9];
    private boolean active;

    /** the chip this reads */
    @Override
    protected abstract BaseChip chip();

    /** the chip's channel state, read back once a frame */
    Map<String, Object> info;

    Pan pan(int ch) {
        return Pan.CENTER;
    }

    @Override
    public String chipName() {
        return "OPL";
    }

    @Override
    public void reset() {
        Arrays.fill(prevOns, false);
        Arrays.fill(prevFnums, 0);
        active = false;
        tones.reset();
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public void poll() {
        try {
            info = chip().getInfo(chipId);
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
            info = null;
        }
    }

    private int intOf(int ch, String field) {
        Object value = info.get("channels." + ch + "." + field);
        return value instanceof Integer i ? i : 0;
    }

    boolean boolOf(int ch, String field) {
        Object value = info == null ? null : info.get("channels." + ch + "." + field);
        return value instanceof Boolean b && b;
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int ch = 0; ch < 9 && !active; ch++) active = boolOf(ch, "keyOn");
        }
        return active;
    }

    @Override
    public int channels(Group group) {
        return 9;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        out.name = "FM";
        out.num = ch + 1;
        if (info == null) return;

        boolean on = boolOf(ch, "keyOn");
        int fnum = intOf(ch, "fnum");
        int block = intOf(ch, "block");
        out.sounding = on;
        out.keyOn = on && (!prevOns[ch] || fnum != prevFnums[ch]);
        prevOns[ch] = on;
        prevFnums[ch] = fnum;

        double freq = fnum > 0 ? fnum * Math.pow(2, block - 1) * tick / (1 << 19) : 0;
        out.note = fnum > 0 ? Notes.noteOf(freq) : -1;
        out.detune = Notes.centsOf(freq);
        // the OPL's LFO runs at a fixed rate and an operator only chooses to hear it or not
        out.lfoPitch = boolOf(ch, "lfoPitch");
        out.lfoVolume = boolOf(ch, "lfoVolume");
        out.toneNum = tone(ch);
        // the carrier's total level, 0.75 dB per step over 0..63
        int tl = intOf(ch, "totalLevel");
        out.volume = 63 - tl;
        out.amplitude = Math.pow(10, -tl * 0.75 / 20);
        out.pan = pan(ch);
    }

    /**
     * The voice the channel is playing, numbered by {@link ToneNumbers} out of the registers that
     * make one up: the two operators of the channel, at the slot offsets the OPL scatters them
     * over, and the channel's own feedback and connection. The total level at 0x40 is left out -
     * it is the channel's volume as much as its voice, see {@code OpnFmReader#fmTone}.
     */
    private int tone(int ch) {
        if (!(info.get("register") instanceof int[] regs)) return 0;
        long fingerprint = ToneNumbers.fold(ToneNumbers.seed(), regs[0xc0 + ch] & 0x0f);
        int written = regs[0xc0 + ch] & 0x0f;
        for (int slot : new int[] {SLOT[ch], SLOT[ch] + 3}) {
            for (int reg : new int[] {0x20, 0x60, 0x80, 0xe0}) {
                fingerprint = ToneNumbers.fold(fingerprint, regs[reg + slot]);
                written |= regs[reg + slot];
            }
        }
        return written == 0 ? 0 : tones.numberOf(fingerprint);
    }

    /** the modulator's register offset of each channel; its carrier sits three further on */
    private static final int[] SLOT = {0, 1, 2, 8, 9, 10, 16, 17, 18};

    private final ToneNumbers tones = new ToneNumbers();
}
