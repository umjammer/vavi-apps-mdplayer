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

import mdplayer.chips.BaseChip;
import mdplayer.chips.OkiM6258Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;


/**
 * OKI MSM6258: one ADPCM stream on a PCM row.
 * <p>
 * The chip is a decoder rather than a voice bank - the driver feeds it bytes and it plays them, so
 * there is nothing here that could be called a note beyond the rate it is running at, which is its
 * clock over a divider of 512, 768 or 1024. That rate is what the key shows, measured against
 * {@value #referenceRate} Hz.
 * <p>
 * It is sounding while the driver is still feeding it, which the chip reports as its status.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class OkiM6258Reader extends ChipReader {

    /** the rate ratio 1.0 stands for */
    private static final double referenceRate = 8000;

    private Map<String, Object> info;

    private boolean prevSounding;
    private int prevRate;
    private boolean active;

    @Override
    protected OkiM6258Chip chip() {
        return chipRegister.chip(OkiM6258Chip.class);
    }

    @Override
    public String chipName() {
        return "OKI";
    }

    @Override
    public void reset() {
        prevSounding = false;
        prevRate = 0;
        active = false;
        info = Collections.emptyMap();
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.PCM);
    }

    @Override
    public int priority() {
        return 59;
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

    private boolean sounding() {
        return intOf("status") != 0;
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
        // the playback frequency the view reports, in kHz
        int rate = intOf("pbFreq");
        out.sounding = sounding;
        out.keyOn = sounding && (!prevSounding || rate != prevRate);
        prevSounding = sounding;
        prevRate = rate;

        // the byte it is decoding, which is as near as this chip comes to a level
        int data = intOf("dataIn") & 0xff;
        out.volume = data;
        out.amplitude = sounding ? data / 255.0 : 0;
        out.pan = panOf(intOf("pan"));
        out.note = sounding && rate > 0 ? Notes.noteOfRatio(rate * 1000.0 / referenceRate) : -1;
    }

    /** the chip's pan register: bit 0 is the left channel and bit 1 the right */
    private static Pan panOf(int pan) {
        boolean left = (pan & 0x01) != 0;
        boolean right = (pan & 0x02) != 0;
        if (left && right) return Pan.CENTER;
        if (left) return Pan.LEFT;
        if (right) return Pan.RIGHT;
        return Pan.CENTER; // nothing set at all is the chip's default, which is both
    }

    /** the chip is the one channel, whichever row shows it */
    @Override
    public BaseChip.Channel muteChannel(Group group, int ch) {
        return super.muteChannel(group, 0);
    }
}
