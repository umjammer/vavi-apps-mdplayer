/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.X1_010Chip;


/**
 * Seta X1-010: sixteen channels on the PCM rows, each either a sample or a wave form.
 * <p>
 * The two sorts count their pitch differently - a sampled channel steps at
 * {@code clock / 8192 * freq} and a wave form one at {@code clock / 128 / (1024 - freq)} - and
 * only the wave form has a pitch that means anything on its own, so a sampled channel's key is
 * the note its rate comes nearest against {@value #referenceRate} Hz.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class X1_010Reader extends PcmSlotReader {

    private static final int CHANNELS = 16;

    private static final double referenceRate = 8000;

    /** the level registers are four bits each */
    private static final double volumeMax = 15;

    @Override
    public String chipName() {
        return "X1";
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    protected int channelCount() {
        return CHANNELS;
    }

    @Override
    protected BaseChip chip() {
        return chipRegister.chip(X1_010Chip.class);
    }

    @Override
    protected boolean sounding(int ch) {
        return boolOf(ch, "enable")
                && Math.max(intOf(ch, "volumeL", 0), intOf(ch, "volumeR", 0)) > 0;
    }

    @Override
    protected int rateOf(int ch) {
        return intOf(ch, "frequency", 0);
    }

    @Override
    protected void readChannel(int ch, FmDspChannel out) {
        int freq = rateOf(ch);
        int left = intOf(ch, "volumeL", 0);
        int right = intOf(ch, "volumeR", 0);
        int clock = info.get("clock") instanceof Integer c ? c : 0;

        out.volume = Math.max(left, right);
        out.amplitude = Math.max(left, right) / volumeMax;
        out.pan = panOf(left, right);
        if (boolOf(ch, "waveform")) {
            // a wave form has a pitch of its own, so this is a real note
            out.note = freq > 0 && freq < 1024 ? Notes.noteOf(clock / 128.0 / (1024 - freq)) : -1;
        } else {
            double rate = clock / 8192.0 * (freq == 0 ? 4 : freq);
            out.note = rate > 0 ? Notes.noteOfRatio(rate / referenceRate) : -1;
        }
    }
}
