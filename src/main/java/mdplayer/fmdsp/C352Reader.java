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
import mdplayer.chips.C352Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;


/**
 * C352: thirty two PCM channels on the PCM rows, eight registers each.
 * <ul>
 * <li>{@code +0} front volume, left in the high byte and right in the low</li>
 * <li>{@code +1} rear volume, the same way</li>
 * <li>{@code +2} the pitch increment: the channel steps one sample when its 16 bit accumulator
 * overflows, so {@code 0x10000} is the chip's own rate</li>
 * <li>{@code +3} the flags: the driver asks for a key on with {@code 0x4000}, and the chip answers
 * by raising the busy bit {@code 0x8000} and dropping it again when the sample runs out</li>
 * <li>{@code +4} the wave bank, shown as the tone number</li>
 * </ul>
 * The registers are read back from the chip rather than shadowed on the way in, so the flags here
 * are the chip's own: whether a channel is sounding is the busy bit, a key on being only a request
 * and one that stays set long after the sound has gone.
 * <p>
 * The chip knows a playback rate and not a note, so as with the other PCM chips here the key is
 * the note that rate ratio comes closest to, ratio 1.0 being o4 c.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class C352Reader extends ChipReader {

    /** the C352's own flags, as the emulator reports them back */
    private static final int FLAG_BUSY = 0x8000;

    private static final int CHANNELS = 32;

    private int[] regs;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevFreqs = new int[CHANNELS];
    private boolean active;

    /**
     * chip channel shown on each row slot, -1 = none yet. Thirty two channels compete for nine
     * visible rows, so they are taken in the order they first sound.
     */
    private final int[] slotChannels = new int[CHANNELS];

    private int mappedChannels;

    @Override
    protected C352Chip chip() {
        return chipRegister.chip(C352Chip.class);
    }

    @Override
    public String chipName() {
        return "C352";
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevFreqs, 0);
        Arrays.fill(slotChannels, -1);
        mappedChannels = 0;
        active = false;
        regs = null;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.PCM);
    }

    @Override
    public int priority() {
        return 65;
    }

    @Override
    public void poll() {
        regs = null;
        try {
            Map<String, Object> info = chip().getInfo(chipId);
            if (info.containsKey("register")) {
                regs = (int[]) info.get("register");
            }
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
        }
        for (int ch = 0; ch < CHANNELS && mappedChannels < slotChannels.length; ch++) {
            if (sounding(ch) && slotOf(ch) < 0) {
                slotChannels[mappedChannels++] = ch;
            }
        }
    }

    private int slotOf(int ch) {
        for (int s = 0; s < mappedChannels; s++) {
            if (slotChannels[s] == ch) return s;
        }
        return -1;
    }

    /**
     * The busy bit, which the chip itself owns: it raises it when the driver executes a key on and
     * drops it when the sample runs out. The key on bit is no good on its own - it stays set long
     * after the sound has gone.
     */
    private boolean sounding(int ch) {
        if (regs == null || ch * 8 + 3 >= regs.length) return false;
        return (regs[ch * 8 + 3] & FLAG_BUSY) != 0;
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
    public void read(Group group, int slot, FmDspChannel out) {
        out.name = "PCM";
        int ch = slotChannels[slot];
        if (ch < 0 || regs == null) return;

        int front = regs[ch * 8];
        int left = (front >> 8) & 0xff;
        int right = front & 0xff;
        int freq = regs[ch * 8 + 2] & 0xffff;
        boolean sounding = sounding(ch);

        out.num = ch + 1; // the chip channel, wherever the slot map put it
        out.pcmCh = ch + 1;
        out.toneNum = regs[ch * 8 + 4] & 0xff; // the wave bank, the chip's nearest thing to a tone
        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || freq != prevFreqs[ch]);
        prevSoundings[ch] = sounding;
        prevFreqs[ch] = freq;

        out.note = sounding && freq > 0 ? Notes.noteOfRatio(freq / 65536.0) : -1;
        out.volume = Math.max(left, right);
        out.amplitude = sounding ? Math.max(left, right) / 255.0 : 0;
        out.pan = panOf(left, right);
    }

    private static Pan panOf(int l, int r) {
        if (l == 0 && r == 0) return Pan.NONE;
        if (r == 0) return Pan.LEFT;
        if (l == 0) return Pan.RIGHT;
        double balance = r / (double) (l + r);
        if (balance < 0.25) return Pan.LEFT;
        if (balance < 0.45) return Pan.MID_LEFT;
        if (balance <= 0.55) return Pan.CENTER;
        if (balance <= 0.75) return Pan.MID_RIGHT;
        return Pan.RIGHT;
    }

    @Override
    public BaseChip.Channel muteChannel(Group group, int slot) {
        return super.muteChannel(group, slotChannels[slot]);
    }
}
