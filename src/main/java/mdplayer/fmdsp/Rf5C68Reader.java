/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.Rf5C68Chip;


/**
 * Ricoh RF5C68: eight sampled channels on the PCM rows.
 * <p>
 * A channel keeps an address in 11.11 fixed point and adds its step to it every output sample, so
 * the step is the playback ratio in units of {@code 1 / 2048} - a step of {@code 0x800} plays the
 * sample at the chip's own rate. As with the other sampled chips the key is the note that ratio
 * comes nearest, ratio 1.0 being o4 c.
 * <p>
 * The envelope is the channel's volume and the pan register holds a level a side, four bits each.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class Rf5C68Reader extends PcmSlotReader {

    private static final int CHANNELS = 8;

    /** the step that plays a sample at the chip's own rate */
    private static final double unitStep = 0x800;

    /** the envelope register is eight bits */
    private static final double envelopeMax = 255;

    @Override
    public String chipName() {
        return "RF5C";
    }

    @Override
    public int priority() {
        return 54;
    }

    @Override
    protected int channelCount() {
        return CHANNELS;
    }

    @Override
    protected BaseChip chip() {
        return chipRegister.chip(Rf5C68Chip.class);
    }

    /** this chip names its keys {@code channel.<n>.<field>}, singular, rather than the usual way */
    private Object value(int ch, String field) {
        return info.get("channel." + ch + "." + field);
    }

    @Override
    protected boolean sounding(int ch) {
        if (info.isEmpty()) return false;
        return value(ch, "enable") instanceof Boolean enabled && enabled
                && value(ch, "key") instanceof Boolean key && key;
    }

    @Override
    protected int rateOf(int ch) {
        return value(ch, "step") instanceof Integer step ? step : 0;
    }

    @Override
    protected void readChannel(int ch, FmDspChannel out) {
        int step = rateOf(ch);
        int env = value(ch, "env") instanceof Integer e ? e : 0;
        int pan = value(ch, "pan") instanceof Integer p ? p : 0xff;

        out.volume = env;
        out.amplitude = env / envelopeMax;
        out.pan = panOf((pan >> 4) & 0x0f, pan & 0x0f);
        out.note = step > 0 ? Notes.noteOfRatio(step / unitStep) : -1;
    }
}
