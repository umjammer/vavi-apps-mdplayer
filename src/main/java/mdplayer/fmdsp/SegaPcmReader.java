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
import mdplayer.chips.SegaPcmChip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;


/**
 * SegaPCM: sixteen channels on the PCM rows (the view has nine, the rest stay dark). The decode
 * follows the chip panel: volumes at {@code ch*8+2/3}, the pitch delta at {@code ch*8+7}, the stop
 * bit in {@code ch*8+0x86}.
 * <p>
 * Key-on edges come from the chip's one-shot {@code keyOn} flags, which are consumed on read -
 * the same protocol the SegaPCM keyboard panel uses, so only one of the two views sees a given
 * edge when both are open.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class SegaPcmReader extends ChipReader {

    private boolean active;

    /** the chip's register file, read back once a frame */
    private byte[] regs;

    /** what each channel was doing last frame, so a channel that starts is a key on */
    private final boolean[] prevPlayings = new boolean[16];

    /**
     * chip channel shown on each row slot, -1 = none yet. Sixteen channels compete for nine
     * visible rows, so the rows go to the channels in the order they first sound - a song using
     * only the upper half still shows.
     */
    private final int[] slotChannels = new int[16];

    private int mappedChannels;

    @Override
    protected SegaPcmChip chip() {
        return chipRegister.chip(SegaPcmChip.class);
    }

    @Override
    public String chipName() {
        return "PCM";
    }

    @Override
    public void reset() {
        regs = null;
        Arrays.fill(prevPlayings, false);
        active = false;
        Arrays.fill(slotChannels, -1);
        mappedChannels = 0;
    }

    @Override
    public void poll() {
        regs = null;
        try {
            Map<String, Object> info = chip().getInfo(chipId);
            if (!info.isEmpty() && info.get("register") instanceof byte[] r) regs = r;
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
        }
        for (int ch = 0; ch < 16 && mappedChannels < slotChannels.length; ch++) {
            if (playing(ch) && slotOf(ch) < 0) {
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

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.PCM);
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int ch = 0; ch < 16 && !active; ch++) active = playing(ch);
        }
        return active;
    }

    private boolean playing(int ch) {
        if (regs == null) return false;
        int ctrl = regs[ch * 8 + 0x86] & 0xff;
        int dt = regs[ch * 8 + 7] & 0xff;
        int l = regs[ch * 8 + 2] & 0x7f;
        int r = regs[ch * 8 + 3] & 0x7f;
        return (ctrl & 0x01) == 0 && dt > 0 && (l | r) != 0;
    }

    @Override
    public int channels(Group group) {
        return 16;
    }

    @Override
    public void read(Group group, int slot, FmDspChannel out) {
        out.name = "PCM";
        int ch = slotChannels[slot];
        if (ch < 0 || regs == null) return;
        int l = regs[ch * 8 + 2] & 0x7f;
        int r = regs[ch * 8 + 3] & 0x7f;
        int dt = regs[ch * 8 + 7] & 0xff;
        boolean playing = playing(ch);

        out.sounding = playing;
        // the chip has no key on of its own to read, so a channel that starts playing is one
        out.keyOn = playing && !prevPlayings[ch];
        prevPlayings[ch] = playing;

        out.name = "PCM";
        out.num = ch + 1; // the chip channel, wherever the slot map put it
        out.pcmCh = ch + 1;
        out.pitchOfRatio(playing ? dt / 256.0 : 0);
        out.volume = Math.max(l, r);
        out.amplitude = playing ? Math.max(l, r) / 127.0 : 0;
        out.pan = panOf(l, r);
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
