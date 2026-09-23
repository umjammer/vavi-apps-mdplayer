/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.Es5505Chip;


/**
 * Ensoniq ES5505 "OTIS", Taito F3's sound chip: thirty two voices on the PCM rows.
 * <p>
 * A voice steps its sample by the frequency count register, which counts in 1024ths of a word per
 * output sample, so {@code 0x400} plays the sample at the chip's own rate - the playback ratio's
 * 1.0 - and the key is the note that ratio comes nearest.
 * <p>
 * The left and right volumes are eight bits each, but not linear: the high nibble is an exponent
 * and the low one a mantissa, so they are {@linkplain #level expanded} before they are compared
 * with each other for the pan or with full scale for the meter. A voice below {@code 0x10} is
 * silent however it is set.
 * <p>
 * Only the voices the ACT register enables are played at all, and the emulator already answers
 * {@code enable} with that in mind. The output channel a voice is routed to stands in for the tone
 * number.
 * <p>
 * Thirty two voices do not fit nine rows, so they are taken in the order they first sound.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-24 nsano initial version <br>
 */
public class Es5505Reader extends PcmSlotReader {

    private static final int VOICES = 32;

    /** the frequency count register that plays a word per output sample */
    private static final double unity = 0x400;

    /** the loudest a volume register {@linkplain #level expands} to, {@code 0xff} */
    private static final double levelMax = level(0xff);

    @Override
    public String chipName() {
        return "OTIS";
    }

    @Override
    public int priority() {
        return 57;
    }

    @Override
    protected int channelCount() {
        return VOICES;
    }

    @Override
    protected BaseChip chip() {
        return chipRegister.chip(Es5505Chip.class);
    }

    /**
     * An eight bit volume register as the chip multiplies by it: {@code (16 + mantissa)} shifted
     * up by the exponent, the same table the emulator builds.
     */
    static int level(int volume) {
        int exponent = (volume >> 4) & 0x0f;
        int mantissa = (volume & 0x0f) | 0x10;
        return (mantissa << 11) >> (16 - exponent);
    }

    @Override
    protected boolean sounding(int v) {
        return boolOf(v, "enable")
                && (level(intOf(v, "volumeL", 0)) > 0 || level(intOf(v, "volumeR", 0)) > 0);
    }

    @Override
    protected int rateOf(int v) {
        return intOf(v, "frequency", 0);
    }

    @Override
    protected void readChannel(int v, FmDspChannel out) {
        int freq = rateOf(v);
        int left = level(intOf(v, "volumeL", 0));
        int right = level(intOf(v, "volumeR", 0));

        out.volume = Math.max(intOf(v, "volumeL", 0), intOf(v, "volumeR", 0));
        out.amplitude = Math.max(left, right) / levelMax;
        out.toneNum = intOf(v, "output", 0);
        out.pan = panOf(left, right);
        out.pitchOfRatio(freq > 0 ? freq / unity : 0);
    }

    @Override
    protected boolean muted(int v) {
        return boolOf(v, "mute");
    }
}
