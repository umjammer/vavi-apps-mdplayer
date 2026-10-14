/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.YmZ280BChip;


/**
 * Yamaha YMZ280B: eight sampled channels on the PCM rows.
 * <p>
 * The chip works out its own playback frequency - the register is a step of a 256th of a 384th of
 * the clock - so the emulator hands over real Hz. That is still a rate and not a note, so the key
 * is the note it comes nearest, measured against {@value #referenceRate} Hz.
 * <p>
 * Pan runs 0 to 15 across the field with 8 in the middle.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class YmZ280BReader extends PcmSlotReader {

    private static final int CHANNELS = 8;

    /** the rate ratio 1.0 stands for */
    private static final double referenceRate = 8000;

    /** the level register is eight bits */
    private static final double levelMax = 255;

    @Override
    public String chipName() {
        return "YMZ";
    }

    @Override
    public int priority() {
        return 52;
    }

    @Override
    protected int channelCount() {
        return CHANNELS;
    }

    @Override
    protected BaseChip chip() {
        return chipRegister.chip(YmZ280BChip.class);
    }

    @Override
    protected boolean sounding(int ch) {
        return boolOf(ch, "playing");
    }

    @Override
    protected int rateOf(int ch) {
        return (int) frequencyOf(ch);
    }

    private double frequencyOf(int ch) {
        Object value = info.get("channels." + ch + ".frequency");
        return value instanceof Double d ? d : 0;
    }

    @Override
    protected void readChannel(int ch, FmDspChannel out) {
        int level = intOf(ch, "level", 0);
        int pan = intOf(ch, "pan", 8);
        double frequency = frequencyOf(ch);

        // the display counts volume the way every other row does, 0 to 127, not the register's 255
        out.volume = (int) (level / levelMax * 127);
        // what is actually being heard, which already has the level register in it - the register
        // alone is a setting a streamed part writes once and then holds for the whole song
        double output = outputLevel();
        out.amplitude = output < 0 ? level / levelMax : output;
        out.measured = output >= 0;
        out.pan = panOf(0x10 - pan, pan);
        out.note = frequency > 0 ? Notes.noteOfRatio(frequency / referenceRate) : -1;
    }
}
