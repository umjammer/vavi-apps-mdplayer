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

import mdplayer.chips.Msm5205Chip;


/**
 * OKI MSM5205: one ADPCM stream on a PCM row.
 * <p>
 * Like the MSM6258 it is a decoder, not a voice bank: the host feeds it nibbles at the rate its
 * prescaler sets, and the key is that rate against {@value #referenceRate} Hz.
 * <p>
 * The chip has no status of its own. It is taken as sounding while it is out of reset and still
 * being fed, which is while it decoded a nibble within the last {@value #idleMillis} ms; a starved
 * chip only holds its last level.
 * <p>
 * A board like Darius feeds it without a break, silence and all, so it can sound for a whole
 * song: it is a {@linkplain FmDspChannel#sampled sampled} voice and a stream that long shows on
 * the level meter alone.
 * <p>
 * The level is the decoder's 12 bit signal, and the ADPCM step, 0 to 48, stands in for the volume
 * digits since it is what follows the loudness of the sample.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-24 nsano initial version <br>
 */
public class Msm5205Reader extends ChipReader {

    /** the rate ratio 1.0 stands for */
    private static final double referenceRate = 8000;

    /** how long without a nibble the chip counts as no longer fed */
    private static final int idleMillis = 50;

    private Map<String, Object> info;

    private boolean prevSounding;
    private boolean active;

    @Override
    protected Msm5205Chip chip() {
        return chipRegister.chip(Msm5205Chip.class);
    }

    @Override
    public String chipName() {
        return "MSM";
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
        return 61;
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

    private boolean boolOf(String field) {
        return info.get(field) instanceof Boolean b && b;
    }

    /** whether a chip at {@code rate} Hz that has gone {@code idleSamples} without a nibble is fed */
    static boolean fed(boolean reset, int idleSamples, int rate) {
        return !reset && rate > 0 && (long) idleSamples * 1000 < (long) rate * idleMillis;
    }

    private boolean sounding() {
        return fed(boolOf("reset"), intOf("idleSamples"), intOf("rate"));
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
        int rate = intOf("rate");
        out.sampled = true;
        out.sounding = sounding;
        out.keyOn = sounding && !prevSounding;
        prevSounding = sounding;

        out.volume = intOf("step");
        out.amplitude = sounding ? Math.min(1, Math.abs(intOf("signal")) / 2048.0) : 0;
        out.toneNum = intOf("data");
        out.note = sounding ? Notes.noteOfRatio(rate / referenceRate) : -1;
    }

    @Override
    public boolean masked(Group group, int ch) {
        return chip().getMask(chipId, 0);
    }
}
