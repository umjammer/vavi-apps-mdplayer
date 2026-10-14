/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.Rf5C164Chip;


/**
 * Ricoh RF5C164, the Mega CD's PCM chip: eight sampled channels on the PCM rows.
 * <p>
 * The same family as the {@link Rf5C68Reader RF5C68} and stepped the same way - the address is
 * 11.11 fixed point and the step is the playback ratio in units of {@code 1 / 2048} - but a
 * different emulator behind it, which reports a level a side rather than an envelope and a packed
 * pan. There is no separate key flag either: a channel is sounding when it is enabled and stepping.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class Rf5C164Reader extends PcmSlotReader {

    private static final int CHANNELS = 8;

    /** the step that plays a sample at the chip's own rate */
    private static final double unitStep = 0x800;

    /** the level registers are eight bits each */
    private static final double levelMax = 255;

    @Override
    public String chipName() {
        return "RF5C";
    }

    @Override
    public int priority() {
        return 53;
    }

    @Override
    protected int channelCount() {
        return CHANNELS;
    }

    @Override
    protected BaseChip chip() {
        return chipRegister.chip(Rf5C164Chip.class);
    }

    @Override
    protected boolean sounding(int ch) {
        return boolOf(ch, "enable") && intOf(ch, "stepB", 0) > 0;
    }

    @Override
    protected int rateOf(int ch) {
        return intOf(ch, "stepB", 0);
    }

    @Override
    protected void readChannel(int ch, FmDspChannel out) {
        int step = rateOf(ch);
        int left = intOf(ch, "mulL", 0);
        int right = intOf(ch, "mulR", 0);

        out.volume = Math.max(left, right);
        out.amplitude = Math.max(left, right) / levelMax;
        out.pan = panOf(left, right);
        out.note = step > 0 ? Notes.noteOfRatio(step / unitStep) : -1;
    }
}
