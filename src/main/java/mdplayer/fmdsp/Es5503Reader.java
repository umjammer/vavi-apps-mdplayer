/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.Es5503Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource;


/**
 * Ensoniq ES5503, the Apple IIGS's DOC: thirty two oscillators on the PCM rows.
 * <p>
 * An oscillator steps a wave table by its frequency register, so that register is the playback
 * ratio and the key is the note it comes nearest. How big a step it is, though, is not fixed: the
 * oscillator's {@linkplain #step resolution and table size} scale it, and taking it against one
 * rate for every oscillator - which is what this did - put every note far below the bottom of the
 * scale, where the key column has nothing to show and stayed blank for the whole song.
 * <p>
 * The control register halts an oscillator with its bit 0 and picks an output channel in its high
 * nibble - the display has no room for eight outputs, so that becomes a left or right lean.
 * <p>
 * Thirty two oscillators do not fit nine rows, so they are taken in the order they first sound.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class Es5503Reader extends PcmSlotReader {

    private static final int OSCILLATORS = 32;

    /** the level register is eight bits */
    private static final double volumeMax = 255;

    @Override
    public String chipName() {
        return "ES55";
    }

    @Override
    public int priority() {
        return 49;
    }

    @Override
    protected int channelCount() {
        return OSCILLATORS;
    }

    @Override
    protected BaseChip chip() {
        return chipRegister.chip(Es5503Chip.class);
    }

    /**
     * The frequency register that plays this oscillator's wave table one entry per output sample,
     * which is the playback ratio's 1.0.
     * <p>
     * The chip adds the frequency register to a 24 bit accumulator every sample and takes the
     * table index from the top of it, {@code resolution} many bits down - so a step is
     * {@code 2^(9+resolution)} - and a bigger table is indexed from further down still, which is
     * the {@code waveTblSize} back off. Both live in register {@code 0xc0+osc}, the low three bits
     * and the next three, and neither is anywhere in what the emulator reports.
     */
    private double step(int osc) {
        int reg = chipRegister.chip(Es5503Chip.class).register[chipId][0xc0 + osc];
        return 1 << (9 + (reg & 7) - ((reg >> 3) & 7));
    }

    @Override
    protected boolean sounding(int osc) {
        return boolOf(osc, "enable") && intOf(osc, "volume", 0) > 0;
    }

    @Override
    protected int rateOf(int osc) {
        return intOf(osc, "frequency", 0);
    }

    @Override
    protected void readChannel(int osc, FmDspChannel out) {
        int freq = rateOf(osc);
        int volume = intOf(osc, "volume", 0);

        out.volume = volume;
        out.amplitude = volume / volumeMax;
        out.toneNum = intOf(osc, "output", 0);
        // even outputs go one way and odd ones the other, which is how a IIGS gets its stereo
        out.pan = (intOf(osc, "output", 0) & 1) == 0
                ? LevelDataSource.Pan.LEFT
                : LevelDataSource.Pan.RIGHT;
        out.pitchOfRatio(freq > 0 ? freq / step(osc) : 0);
    }
}
