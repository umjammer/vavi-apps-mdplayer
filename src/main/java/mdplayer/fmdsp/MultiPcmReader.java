/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import mdplayer.chips.BaseChip;
import mdplayer.chips.MultiPcmChip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;


/**
 * The Sega MultiPCM (315-5560): twenty eight sampled slots on the PCM rows.
 * <p>
 * This one knows its own pitch - the slots carry an octave and a pitch within it - so the
 * emulator hands back a real note rather than the rate ratio the other sampled chips here have to
 * settle for. Volume is the total level as it stands, which ramps towards what was written, and
 * the tone number is the sample the slot was pointed at.
 * <p>
 * Whether a slot is sounding is the emulator's {@code playing} and not the key on bit the driver
 * wrote: the envelope clears it when it has run out, so a slot keyed on long ago may well be
 * silent by now.
 * <p>
 * Twenty eight slots do not fit nine rows, so they are taken in the order they first sound.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class MultiPcmReader extends ChipReader {

    private static final int CHANNELS = 28;

    /** the total level runs 0 loudest to here */
    private static final int levelMax = 0x7f;

    private Map<String, Object> info;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevNotes = new int[CHANNELS];
    private boolean active;

    /** chip channel shown on each row slot, -1 = none yet */
    private final int[] slotChannels = new int[CHANNELS];

    private int mappedChannels;

    @Override
    protected MultiPcmChip chip() {
        return chipRegister.chip(MultiPcmChip.class);
    }

    @Override
    public String chipName() {
        return "MPCM";
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevNotes, 0);
        Arrays.fill(slotChannels, -1);
        mappedChannels = 0;
        active = false;
        info = Collections.emptyMap();
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.PCM);
    }

    @Override
    public int priority() {
        return 62;
    }

    @Override
    public void poll() {
        try {
            info = chip().getInfo(chipId);
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
            info = Collections.emptyMap();
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

    private boolean sounding(int ch) {
        if (info.isEmpty()) return false;
        Object playing = info.get("channels." + ch + ".playing");
        return playing != null && (boolean) playing;
    }

    private int intOf(int ch, String field, int fallback) {
        Object value = info.get("channels." + ch + "." + field);
        return value instanceof Integer i ? i : fallback;
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
        if (ch < 0 || info.isEmpty()) return;

        boolean sounding = sounding(ch);
        int note = intOf(ch, "note", -1);

        out.num = ch + 1; // the chip channel, wherever the slot map put it
        out.pcmCh = ch + 1;
        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || note != prevNotes[ch]);
        prevSoundings[ch] = sounding;
        prevNotes[ch] = note;

        out.note = sounding ? note : -1;
        // the sample the slot was pointed at, the chip's nearest thing to an instrument
        out.toneNum = intOf(ch, "inst.0", 0);
        int level = intOf(ch, "totalLevel", levelMax);
        out.volume = levelMax - level;
        out.amplitude = sounding ? (levelMax - level) / (double) levelMax : 0;
        out.pan = panOf(intOf(ch, "panpot", 0));
    }

    /**
     * The panpot as the chip reads it: 0 is centre and 8 is silence, 1 to 7 hold the left channel
     * up and wind the right one down, and 9 to 15 do the same the other way about.
     */
    private static Pan panOf(int panpot) {
        if (panpot == 0) return Pan.CENTER;
        if (panpot == 8) return Pan.NONE;
        if (panpot < 8) return panpot >= 5 ? Pan.LEFT : Pan.MID_LEFT;
        return 0x10 - panpot >= 5 ? Pan.RIGHT : Pan.MID_RIGHT;
    }

    @Override
    public BaseChip.Channel muteChannel(Group group, int slot) {
        return super.muteChannel(group, slotChannels[slot]);
    }
}
