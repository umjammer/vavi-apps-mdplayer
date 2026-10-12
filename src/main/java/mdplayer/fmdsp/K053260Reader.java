/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.K053260Chip;


/**
 * Konami K053260: four sampled channels on the PCM rows.
 * <p>
 * The rate register divides the clock the other way about from most - a channel plays at
 * {@code clock / (0x1000 - rate)} - so a bigger register means a higher note. That is a rate and
 * not a note, so the key is the note it comes nearest, measured against {@value #referenceRate} Hz.
 * <p>
 * Pan runs 0 to 7 across the stereo field, and the chip mixes a channel at {@code volume * pan}
 * on the left and {@code volume * (8 - pan)} on the right.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class K053260Reader extends PcmSlotReader {

    private static final int CHANNELS = 4;

    private static final double referenceRate = 8000;

    /** the volume register is seven bits */
    private static final double volumeMax = 127;

    @Override
    public String chipName() {
        return "K053";
    }

    @Override
    public int priority() {
        return 56;
    }

    @Override
    protected int channelCount() {
        return CHANNELS;
    }

    @Override
    protected BaseChip chip() {
        return chipRegister.chip(K053260Chip.class);
    }

    @Override
    protected boolean sounding(int ch) {
        return intOf(ch, "play", 0) != 0;
    }

    @Override
    protected int rateOf(int ch) {
        return intOf(ch, "freq", 0);
    }

    @Override
    protected void readChannel(int ch, FmDspChannel out) {
        int rateReg = intOf(ch, "freq", 0);
        int volume = intOf(ch, "volume", 0);
        int pan = intOf(ch, "pan", 4);
        int clock = info.get("clock") instanceof Integer c ? c : 0;

        out.volume = volume;
        out.amplitude = volume / volumeMax;
        out.toneNum = intOf(ch, "bank", 0);
        out.pan = panOf(volume * pan, volume * (8 - pan));
        double rate = rateReg < 0x1000 ? clock / (double) (0x1000 - rateReg) : 0;
        out.note = rate > 0 ? Notes.noteOfRatio(rate / referenceRate) : -1;
    }
}
