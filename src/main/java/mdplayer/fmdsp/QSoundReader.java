/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.QSoundChip;


/**
 * Capcom Q-Sound: sixteen sampled channels on the PCM rows.
 * <p>
 * The DSP keeps everything in its register file. The rate is a 12 bit phase increment, so
 * {@code 0x1000} plays a sample at the chip's own rate and the key is the note that ratio comes
 * nearest, ratio 1.0 being o4 c. The pan is an index into the chip's own mixing tables.
 * <p>
 * Two emulators answer for this chip and a song picks one of them; both report the same fields.
 * <p>
 * Sixteen channels do not fit nine rows, so they are taken in the order they first sound.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class QSoundReader extends PcmSlotReader {

    private static final int CHANNELS = 16;

    /** the increment that plays a sample at the chip's own rate; the phase is 12 bit */
    private static final double unitRate = 0x1000;

    /** the volume register is fourteen bits */
    private static final double levelMax = 0x3fff;

    @Override
    public String chipName() {
        return "QSND";
    }

    @Override
    public int priority() {
        return 51;
    }

    @Override
    protected int channelCount() {
        return CHANNELS;
    }

    @Override
    protected BaseChip chip() {
        return chipRegister.chip(QSoundChip.class);
    }

    @Override
    protected boolean sounding(int ch) {
        return intOf(ch, "rate", 0) != 0 && intOf(ch, "volume", 0) != 0;
    }

    @Override
    protected int rateOf(int ch) {
        return intOf(ch, "rate", 0);
    }

    @Override
    protected void readChannel(int ch, FmDspChannel out) {
        int rate = rateOf(ch);
        int volume = intOf(ch, "volume", 0);

        out.volume = volume >> 6; // the register is fourteen bits, the display wants a byte
        out.amplitude = Math.min(1, volume / levelMax);
        out.pan = panOf(panIndex(ch), 32 - panIndex(ch));
        out.toneNum = intOf(ch, "bank", 0) & 0xff;
        out.note = rate > 0 ? Notes.noteOfRatio(rate / unitRate) : -1;
    }

    /**
     * The pan register counts from {@code 0x110}, and the chip looks its mixing levels up by that
     * distance - 0 to 32 across the field, with a second range from {@code 0x30} that pans the
     * same way but linearly.
     */
    private int panIndex(int ch) {
        int index = Math.clamp(intOf(ch, "pan", 0x110 + 16) - 0x110, 0, 97);
        return Math.min(index >= 0x30 ? index - 0x30 : index, 32);
    }
}
