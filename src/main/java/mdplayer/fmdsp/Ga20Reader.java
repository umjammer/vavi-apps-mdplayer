/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.Ga20Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;


/**
 * Irem GA20: four sampled channels on the PCM rows.
 * <p>
 * The chip runs at a quarter of its clock and steps one sample every {@code 256 - freq} of those,
 * so a channel plays its sample at {@code clock / 4 / (256 - freq)} Hz. That is a rate and not a
 * note, so as with the other sampled chips the key is the note that rate comes nearest, measured
 * against {@value #referenceRate} Hz.
 * <p>
 * The chip is mono.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class Ga20Reader extends PcmSlotReader {

    private static final int CHANNELS = 4;

    /** the rate ratio 1.0 stands for */
    private static final double referenceRate = 8000;

    /** the volume register is eight bits */
    private static final double volumeMax = 255;

    @Override
    public String chipName() {
        return "GA20";
    }

    @Override
    public int priority() {
        return 58;
    }

    @Override
    protected int channelCount() {
        return CHANNELS;
    }

    @Override
    protected BaseChip chip() {
        return chipRegister.chip(Ga20Chip.class);
    }

    @Override
    protected boolean sounding(int ch) {
        return boolOf(ch, "play");
    }

    @Override
    protected int rateOf(int ch) {
        return intOf(ch, "freq", 0);
    }

    @Override
    protected void readChannel(int ch, FmDspChannel out) {
        int freq = intOf(ch, "freq", 0);
        int volume = intOf(ch, "volume", 0);
        int clock = info.get("clock") instanceof Integer c ? c : 0;

        out.pan = Pan.CENTER; // the chip is mono
        out.volume = volume;
        out.amplitude = volume / volumeMax;
        out.toneNum = intOf(ch, "sadr", 0); // the sample it was pointed at
        double rate = freq < 256 ? clock / 4.0 / (256 - freq) : 0;
        out.note = rate > 0 ? Notes.noteOfRatio(rate / referenceRate) : -1;
    }
}
