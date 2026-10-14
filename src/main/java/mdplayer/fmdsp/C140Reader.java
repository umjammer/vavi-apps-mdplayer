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
import mdplayer.chips.C140Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;


/**
 * C140: Namco's twenty four channel PCM chip, sixteen registers a channel.
 * <ul>
 * <li>{@code +0} and {@code +1} the right and left volume - that order, the right one first</li>
 * <li>{@code +2} and {@code +3} the sixteen bit pitch, high byte first</li>
 * <li>{@code +4} the wave bank, shown as the tone number</li>
 * <li>{@code +5} the mode, key on being bit 7</li>
 * </ul>
 * The chip knows a playback rate and not a note. The emulator steps a channel by
 * {@code frequency * 2 * baseRate} in 16.16, so it plays at
 * {@code frequency * 2 * baseRate / 65536} Hz, {@code baseRate} being the chip's clock over 576 -
 * 44100 Hz on the Namco boards. What it cannot tell us is the rate the sample was recorded at,
 * without which there is no absolute pitch, so the key is what that playback rate sounds like for
 * a sample recorded at {@link #referenceRate}. The intervals are therefore exact and the octave
 * is a convention.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class C140Reader extends ChipReader {

    private static final int CHANNELS = 24;

    /** registers a channel */
    private static final int STRIDE = 16;

    /** the chip's clock over 576, which is 44100 Hz on the Namco boards */
    private static final double baseRate = 44100;

    /** the sample rate the key is read against, a common one for a chip of this vintage */
    private static final double referenceRate = 8000;

    private final boolean[] prevKeyOns = new boolean[CHANNELS];
    private final int[] prevFreqs = new int[CHANNELS];
    private boolean active;

    /** the chip's register file, read back once a frame */
    private byte[] regs;

    /** chip channel shown on each row slot, -1 = none yet, as the PCM rows are fewer */
    private final int[] slotChannels = new int[CHANNELS];

    private int mappedChannels;

    @Override
    protected C140Chip chip() {
        return chipRegister.chip(C140Chip.class);
    }

    @Override
    public String chipName() {
        return "C140";
    }

    @Override
    public void reset() {
        regs = null;
        Arrays.fill(prevKeyOns, false);
        Arrays.fill(prevFreqs, 0);
        Arrays.fill(slotChannels, -1);
        mappedChannels = 0;
        active = false;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.PCM);
    }

    @Override
    public int priority() {
        return 66;
    }

    @Override
    public void poll() {
        regs = null;
        try {
            Map<String, Object> info = chip().getInfo(chipId);
            if (info.get("register") instanceof byte[] r) regs = r;
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
        }
        for (int ch = 0; ch < CHANNELS && mappedChannels < slotChannels.length; ch++) {
            if (keyOn(ch) && slotOf(ch) < 0) {
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

    /** the mode register's key on bit, read back out of the chip */
    private boolean keyOn(int ch) {
        if (regs == null) return false;
        int i = ch * STRIDE + 5;
        return i < regs.length && (regs[i] & 0x80) != 0;
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int ch = 0; ch < CHANNELS && !active; ch++) active = keyOn(ch);
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

        int right = regs[ch * STRIDE] & 0xff;
        int left = regs[ch * STRIDE + 1] & 0xff;
        int freq = ((regs[ch * STRIDE + 2] & 0xff) << 8) | (regs[ch * STRIDE + 3] & 0xff);
        boolean on = keyOn(ch);

        out.num = ch + 1; // the chip channel, wherever the slot map put it
        out.pcmCh = ch + 1;
        out.toneNum = regs[ch * STRIDE + 4] & 0xff; // the wave bank
        out.sounding = on;
        out.keyOn = on && (!prevKeyOns[ch] || freq != prevFreqs[ch]);
        prevKeyOns[ch] = on;
        prevFreqs[ch] = freq;

        out.note = on && freq > 0
                ? Notes.noteOfRatio(freq * 2 * baseRate / 65536 / referenceRate) : -1;
        out.volume = Math.max(left, right);
        out.amplitude = on ? Math.max(left, right) / 255.0 : 0;
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
