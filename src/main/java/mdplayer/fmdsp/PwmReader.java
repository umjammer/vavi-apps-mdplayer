/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import mdplayer.chips.BaseChip;
import mdplayer.chips.PwmChip;


/**
 * The Mega Drive 32X's PWM: a stereo DAC the driver feeds through a FIFO.
 * <p>
 * There is no voice here at all - no pitch, no envelope, only the level each side of the DAC
 * currently sits at - so the row shows a meter that follows the output and no key. It takes one
 * PCM row.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class PwmReader extends ChipReader {

    /** the DAC is twelve bits */
    private static final double levelMax = 0x800;

    private Map<String, Object> info;

    private boolean prevSounding;
    private boolean active;

    @Override
    protected PwmChip chip() {
        return chipRegister.chip(PwmChip.class);
    }

    @Override
    public String chipName() {
        return "PWM";
    }

    @Override
    public void reset() {
        prevSounding = false;
        active = false;
        info = Collections.emptyMap();
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.PCM);
    }

    @Override
    public int priority() {
        return 48;
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

    private int intOf(String field) {
        Object value = info.get(field);
        return value instanceof Integer i ? i : 0;
    }

    private boolean sounding() {
        return intOf("outputL") != 0 || intOf("outputR") != 0;
    }

    @Override
    public boolean active(Group group) {
        if (!active) active = sounding();
        return active;
    }

    @Override
    public int channels(Group group) {
        return 1;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        out.name = "PCM";
        out.num = 1;
        out.pcmCh = 1;
        if (info.isEmpty()) return;

        boolean sounding = sounding();
        int left = Math.abs(intOf("outputL"));
        int right = Math.abs(intOf("outputR"));

        out.sounding = sounding;
        out.keyOn = sounding && !prevSounding;
        prevSounding = sounding;

        out.volume = Math.max(left, right);
        out.amplitude = Math.min(1, Math.max(left, right) / levelMax);
        out.pan = PcmSlotReader.panOf(left, right);
        out.note = -1; // a DAC has no pitch to report
    }

    /** the chip cannot mute a channel */
    @Override
    public BaseChip.Channel muteChannel(Group group, int ch) {
        return null;
    }
}
