/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import mdplayer.chips.OkiM6295Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;


/**
 * OKI MSM6295: four ADPCM voices on the PCM rows.
 * <p>
 * A voice carries a volume, taken from the four bit attenuation its key on command names, and
 * nothing else of its own - the chip has one rate for all four, divided from its clock by the pin 7
 * state. So the key here is the same on every row and moves only when the clock or that pin does,
 * which is the truth about this chip rather than a shortcoming of the reading.
 * <p>
 * Whether a voice is sounding is the emulator's own {@code playing}, which clears itself when the
 * sample runs out. The chip does publish key on edges, but reading them takes them, and the panel
 * reads them too - so the edges here are found by watching {@code playing} instead.
 * <p>
 * The chip is mono.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class OkiM6295Reader extends ChipReader {

    private static final int CHANNELS = 4;

    /** the chip's volume table tops out here */
    private static final double maxVolume = 32;

    /**
     * The rate ratio 1.0 stands for, which the key is measured against. The 6295 usually runs
     * somewhere near 8 kHz, so this puts a normal song around o4.
     */
    private static final double referenceRate = 8000;

    private Map<String, Object> info;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private boolean active;

    @Override
    protected OkiM6295Chip chip() {
        return chipRegister.chip(OkiM6295Chip.class);
    }

    @Override
    public String chipName() {
        return "OKI";
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        active = false;
        info = Collections.emptyMap();
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.PCM);
    }

    @Override
    public int priority() {
        return 60;
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

    private boolean sounding(int ch) {
        return info.containsKey("channels." + ch + ".playing") && (boolean) info.get("channels." + ch + ".playing");
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int ch = 0; ch < CHANNELS && !active; ch++) active = sounding(ch);
        }
        return active;
    }

    @Override
    public int channels(Group group) {
        return CHANNELS;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        out.name = "PCM";
        out.num = ch + 1;
        out.pcmCh = ch + 1;
        out.pan = Pan.CENTER; // the chip is mono
        if (info.isEmpty()) return;

        boolean sounding = sounding(ch);
        out.sounding = sounding;
        out.keyOn = sounding && !prevSoundings[ch];
        prevSoundings[ch] = sounding;

        int volume = (int) info.get("channels." + ch + ".volume");
        out.volume = volume;
        out.amplitude = sounding ? volume / maxVolume : 0;
        // the sample it is playing, as near as this chip comes to a tone number
        out.toneNum = (int) info.get("channels." + ch + ".sadr");
        int rate = (int) info.get("sampleRate");
        out.note = sounding && rate > 0 ? Notes.noteOfRatio(rate / referenceRate) : -1;
    }
}
