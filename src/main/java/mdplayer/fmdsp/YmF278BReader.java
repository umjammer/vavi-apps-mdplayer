/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.EnumSet;
import java.util.Set;

import mdplayer.chips.BaseChip;
import mdplayer.chips.YmF278BChip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;


/**
 * OPL4: the FM part is an OPL3 and is read as one - the first of its two banks, the nine channels
 * the rows have room for - plus the wave table's twenty four PCM channels.
 * <p>
 * The wave registers live in the chip's third port as groups of twenty four, the first group
 * starting at 8 - so a channel's register in group {@code n} is at {@code 8 + n * 24 + ch}:
 * <ul>
 * <li>{@code 0x20+ch} the low seven bits of the F-number, in bits 1-7</li>
 * <li>{@code 0x38+ch} its high three bits, and a signed octave in the high nibble</li>
 * <li>{@code 0x50+ch} the total level, in bits 1-7</li>
 * <li>{@code 0x68+ch} key on in bit 7, the four bit pan in bits 0-3, and in bit 4 the DO1 pin,
 * which a MoonSound has nothing wired to and so silences the channel</li>
 * </ul>
 * As with the other PCM chips the key is the note the playback rate comes closest to, a rate of
 * {@code F-number 0, octave 0} being o4 c.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class YmF278BReader extends OplReader {

    private static final int PCM_CHANNELS = 24;

    /** the wave registers sit in the third port */
    private static final int WAVE_PORT = 2;

    private final boolean[] prevKeyOns = new boolean[PCM_CHANNELS];
    private final int[] prevPitches = new int[PCM_CHANNELS];
    private boolean pcmActive;

    /** chip channel shown on each row slot, -1 = none yet, as the PCM rows are fewer */
    private final int[] slotChannels = new int[PCM_CHANNELS];

    private int mappedChannels;

    @Override
    protected YmF278BChip chip() {
        return chipRegister.chip(YmF278BChip.class);
    }

    /** the carrier operator's offset from 0x40, per channel */
    private static final int[] carrier = {3, 4, 5, 11, 12, 13, 19, 20, 21};

    private static final int[][] empty = new int[][] {new int[0x100], new int[0x100], new int[0x100]};

    private int[][] banks;

    /**
     * The OPL4's FM part answers as registers rather than as channel state, so this decodes them
     * into the shape {@link OplReader} reads. The wave part is read straight from its own bank.
     */
    @Override
    public void poll() {
        banks = empty;
        info = null;
        try {
            Object value = chip().getInfo(chipId).get("register");
            if (value instanceof int[][] r && r.length > WAVE_PORT) banks = r;
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
            banks = empty;
        }
        if (banks == null) return;
        int[] fm = banks[0];
        Map<String, Object> decoded = new HashMap<>();
        for (int ch = 0; ch < 9; ch++) {
            int regB = fm[0xb0 + ch] & 0xff;
            decoded.put("channels." + ch + ".keyOn", (regB & 0x20) != 0);
            decoded.put("channels." + ch + ".fnum", ((regB & 0x03) << 8) | (fm[0xa0 + ch] & 0xff));
            decoded.put("channels." + ch + ".block", (regB >> 2) & 0x07);
            decoded.put("channels." + ch + ".totalLevel", fm[0x40 + carrier[ch]] & 0x3f);
            decoded.put("channels." + ch + ".panL", (fm[0xc0 + ch] & 0x10) != 0);
            decoded.put("channels." + ch + ".panR", (fm[0xc0 + ch] & 0x20) != 0);
        }
        info = decoded;

        for (int ch = 0; ch < PCM_CHANNELS && mappedChannels < slotChannels.length; ch++) {
            if (keyOn(ch) && slotOf(ch) < 0) {
                slotChannels[mappedChannels++] = ch;
            }
        }
    }

    private int[] waveRegs() {
        return banks[WAVE_PORT];
    }

    /** the OPL3's register 0xC0 output bits, which the OPL4's FM part keeps */
    @Override
    protected Pan pan(int ch) {
        boolean left = boolOf(ch, "panL");
        boolean right = boolOf(ch, "panR");
        if (left && right) return Pan.CENTER;
        if (left) return Pan.LEFT;
        if (right) return Pan.RIGHT;
        return Pan.NONE;
    }

    @Override public String chipName() { return "OPL4"; }

    @Override public int priority() { return 65; }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM, Group.PCM);
    }

    @Override
    public void reset() {
        super.reset();
        Arrays.fill(prevKeyOns, false);
        Arrays.fill(prevPitches, 0);
        Arrays.fill(slotChannels, -1);
        mappedChannels = 0;
        pcmActive = false;
    }

    private int slotOf(int ch) {
        for (int s = 0; s < mappedChannels; s++) {
            if (slotChannels[s] == ch) return s;
        }
        return -1;
    }

    private boolean keyOn(int ch) {
        return (waveRegs()[0x68 + ch] & 0x80) != 0;
    }

    @Override
    public boolean active(Group group) {
        if (group != Group.PCM) return super.active(group);
        if (!pcmActive) {
            for (int ch = 0; ch < PCM_CHANNELS && !pcmActive; ch++) pcmActive = keyOn(ch);
        }
        return pcmActive;
    }

    @Override
    public int channels(Group group) {
        return group == Group.PCM ? PCM_CHANNELS : super.channels(group);
    }

    @Override
    public void read(Group group, int slot, FmDspChannel out) {
        if (group != Group.PCM) {
            super.read(group, slot, out);
            return;
        }
        out.name = "PCM";
        int ch = slotChannels[slot];
        if (ch < 0) return;

        int[] regs = waveRegs();
        int control = regs[0x68 + ch];
        boolean on = (control & 0x80) != 0;
        // the F-number is ten bits, its low seven in one register and its high three in the next
        int fnum = ((regs[0x20 + ch] & 0xff) >> 1) | ((regs[0x38 + ch] & 0x07) << 7);
        // the octave is a signed nibble, -8..7
        int octave = (regs[0x38 + ch] >> 4) & 0x0f;
        if (octave >= 8) octave -= 16;
        int tl = (regs[0x50 + ch] & 0xff) >> 1;

        out.num = ch + 1;
        out.pcmCh = ch + 1;
        // the wave number is the nearest thing the chip has to an instrument, nine bits of it
        out.toneNum = (regs[0x08 + ch] & 0xff) | ((regs[0x20 + ch] & 0x01) << 8);
        out.sounding = on;
        int pitch = fnum | (octave << 12);
        out.keyOn = on && (!prevKeyOns[ch] || pitch != prevPitches[ch]);
        prevKeyOns[ch] = on;
        prevPitches[ch] = pitch;

        out.note = on ? Notes.noteOfRatio((1024 + fnum) / 1024.0 * Math.pow(2, octave)) : -1;
        out.volume = 127 - tl;
        // the wave table attenuates 0.75 dB a step, as the FM part does
        out.amplitude = on ? Math.pow(10, -tl * 0.75 / 20) : 0;
        out.pan = (control & 0x10) != 0 ? Pan.NONE : panOf(control & 0x0f);
    }

    /** the OPL4's four bit pan: 0 and 15 centre, 1-7 turning right, 8-14 turning left */
    private static Pan panOf(int pan) {
        return switch (pan) {
            case 0, 15 -> Pan.CENTER;
            case 1, 2, 3 -> Pan.MID_RIGHT;
            case 4, 5, 6, 7 -> Pan.RIGHT;
            case 8, 9, 10, 11 -> Pan.LEFT;
            default -> Pan.MID_LEFT;
        };
    }

    @Override
    public BaseChip.Channel muteChannel(Group group, int slot) {
        if (group != Group.PCM) return super.muteChannel(group, slot);
        int ch = slotChannels[slot];
        // the chip counts its wave channels after the 18 FM ones and the 5 drums
        return ch >= 0 ? chip().channel(chipId, 23 + ch) : null;
    }
}
