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

import mdplayer.chips.YmF271Chip;


/**
 * Yamaha YMF271, the OPX: twelve channels of four slots each, which the chip reports slot by slot.
 * <p>
 * Each channel is read from the first of its four slots - the one that carries the pitch for every
 * algorithm - and the levels of that slot's two outputs give the stereo. The chip works the
 * frequency out itself from the F-number, the block and the multiple, so these are real notes.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class YmF271Reader extends ChipReader {

    private static final int CHANNELS = 12;

    /** the level registers are four bits each */
    private static final double levelMax = 15;

    private Map<String, Object> info;

    private final boolean[] prevActives = new boolean[CHANNELS];
    private final int[] prevFns = new int[CHANNELS];
    private boolean active;

    @Override
    protected YmF271Chip chip() {
        return chipRegister.chip(YmF271Chip.class);
    }

    @Override
    public String chipName() {
        return "OPX";
    }

    @Override
    public void reset() {
        Arrays.fill(prevActives, false);
        Arrays.fill(prevFns, 0);
        active = false;
        info = Collections.emptyMap();
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 40;
    }

    @Override
    public void poll() {
        try {
            info = chip().getInfo(chipId);
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
            info = Collections.emptyMap();
        }
    }

    /** the chip numbers its slots by channel: channel n's first slot is slot n */
    private int intOf(int ch, String field) {
        Object value = info.get("slots." + ch + "." + field);
        return value instanceof Integer i ? i : 0;
    }

    private boolean sounding(int ch) {
        if (info.isEmpty()) return false;
        Object on = info.get("slots." + ch + ".active");
        return on instanceof Boolean b && b;
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
    public void read(Group group, int ch, FmDspChannel out) {
        out.name = "FM";
        out.num = ch + 1;
        if (info.isEmpty()) return;

        boolean sounding = sounding(ch);
        // the F-number the view files under its instrument fields, used only to spot a re-strike
        int fns = intOf(ch, "inst.14");
        int left = intOf(ch, "ch0Level");
        int right = intOf(ch, "ch1Level");

        out.sounding = sounding;
        out.keyOn = sounding && (!prevActives[ch] || fns != prevFns[ch]);
        prevActives[ch] = sounding;
        prevFns[ch] = fns;

        out.volume = Math.max(left, right);
        out.amplitude = sounding ? Math.max(left, right) / levelMax : 0;
        out.pan = PcmSlotReader.panOf(left, right);
        out.toneNum = intOf(ch, "inst.9"); // the wave form
        // the chip works the frequency out itself, from the F-number, block and multiple
        Object hz = info.get("slots." + ch + ".frequency");
        double frequency = hz instanceof Double d ? d : 0;
        out.note = sounding && frequency > 0 ? Notes.noteOf(frequency) : -1;
    }
}
