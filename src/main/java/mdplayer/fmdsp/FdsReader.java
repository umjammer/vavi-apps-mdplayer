/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import mdplayer.chips.BaseChip;
import mdplayer.chips.NpNesChip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * The Famicom Disk System's one wavetable channel.
 * <p>
 * The emulator reports the frequency register and the envelope's volume rather than a pitch, and
 * the channel steps a sixty four sample table, so it counts
 * {@code freq * 1789773 / 65536 / 64} Hz.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class FdsReader extends ChipReader {

    /** the NES CPU clock the channel divides */
    private static final double cpuClock = 1789773;

    /** the accumulator is sixteen bits and the wave table sixty four samples long */
    private static final double step = 65536.0 * 64;

    /** the envelope's volume register is six bits, though only the low five are heard */
    private static final double volumeMax = 32;

    private Map<String, Object> info;

    private boolean prevSounding;
    private int prevFreq;
    private boolean active;

    @Override
    protected NpNesChip.FdsChip chip() {
        return chipRegister.chip(NpNesChip.FdsChip.class);
    }

    @Override
    public String chipName() {
        return "FDS";
    }

    @Override
    public void reset() {
        prevSounding = false;
        prevFreq = 0;
        active = false;
        info = null;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 82;
    }

    @Override
    public void poll() {
        try {
            info = chip().getInfo(chipId);
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
            info = null;
        }
    }

    private int intOf(String field) {
        Object value = info == null ? null : info.get(field);
        return value instanceof Integer i ? i : 0;
    }

    private boolean sounding() {
        return info != null && !info.isEmpty() && intOf("freq") > 0 && intOf("vol") > 0
                && Boolean.TRUE.equals(info.get("WE")) == false;
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
        out.name = "SSG";
        out.num = 1;
        out.info = TrackInfo.SSG;
        out.pan = Pan.CENTER; // the chip is mono
        if (info == null || info.isEmpty()) return;

        boolean sounding = sounding();
        int freq = intOf("freq");
        int volume = intOf("vol");

        out.sounding = sounding;
        out.keyOn = sounding && (!prevSounding || freq != prevFreq);
        prevSounding = sounding;
        prevFreq = freq;

        out.volume = volume;
        out.amplitude = sounding ? Math.min(1, volume / volumeMax) : 0;
        out.ssgTone = true;
        out.note = sounding ? Notes.noteOf(freq * cpuClock / step) : -1;
    }

    /** the chip is the one channel, whichever row shows it */
    @Override
    public BaseChip.Channel muteChannel(Group group, int ch) {
        return super.muteChannel(group, 0);
    }
}
