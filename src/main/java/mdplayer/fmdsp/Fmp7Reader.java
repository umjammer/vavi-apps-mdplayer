/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

import mdplayer.ChipRegister;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.fmp7.Fmp7Driver;
import mdplayer.lib.fmp7.Fmp7Work;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackDetail;


/**
 * FMP7, which plays on an emulated PC and emulates no chip here.
 * <p>
 * Nothing goes past on its way out either - FMP7 synthesizes its own OPNA and hands the result
 * to a sound card - so there are no registers to read back. What there is instead is better:
 * FMP7 publishes what its driver is doing, part by part, as it does it, and that is the note the
 * MML asked for rather than a frequency worked back out of a divider. See {@link Fmp7Work}.
 * <p>
 * A part says which of the three kinds it is - FM, SSG, PCM - and those are the three row groups,
 * so the rows line up with the driver's own parts. Which sound source each one went to (an OPNA,
 * an OPM, the rhythm generator) is in the work too, and is what names the chip over the meters.
 * The analyzer bars are not this reader's business: FMP7's audio goes through mdplayer's mixer,
 * so they are measured off the sound itself.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-08-17 nsano initial version <br>
 */
public class Fmp7Reader implements FmDspChipReader {

    /** rows the visualizer has for each group */
    private static final int FM_ROWS = 9;
    private static final int SSG_ROWS = 3;
    private static final int PCM_ROWS = 9;

    /** FMP7's part volumes run 0 to here */
    private static final int VOLUME_MAX = 127;

    private Supplier<BaseDriver> driver;

    /** FMP7 part shown on each row of each group, -1 = none */
    private final int[] fmParts = new int[FM_ROWS];
    private final int[] ssgParts = new int[SSG_ROWS];
    private final int[] pcmParts = new int[PCM_ROWS];

    /** the key-on counter each row was last seen at, for the edge the display flashes on */
    private final int[] fmKeyOn = new int[FM_ROWS];
    private final int[] ssgKeyOn = new int[SSG_ROWS];
    private final int[] pcmKeyOn = new int[PCM_ROWS];

    private final boolean[] struck = new boolean[FM_ROWS + SSG_ROWS + PCM_ROWS];

    private boolean fmActive, ssgActive, pcmActive, rhythmActive;

    /** the key-on counter each rhythm part was last seen at; they share one meter, not rows */
    private final int[] rhythmKeyOn = new int[Fmp7Work.MAX_PART];

    /** a rhythm part was struck since the last snapshot, and how loud the loudest one is */
    private boolean rhythmStruck;
    private double rhythmVolume;

    @Override
    public String chipName() {
        Fmp7Work work = work();
        if (work == null) {
            return "FMP7";
        }
        // what the song is actually being played on, which FMP7 says part by part
        for (int p = 0; p < Fmp7Work.MAX_PART; p++) {
            if (work.mode(p) != Fmp7Work.MODE_NONE && work.device(p) == Fmp7Work.DEVICE_OPM) {
                return "OPM";
            }
        }
        return "OPNA";
    }

    @Override
    public void bind(ChipRegister chipRegister) {
    }

    @Override
    public void bind(Supplier<BaseDriver> driver) {
        this.driver = driver;
    }

    @Override
    public void reset() {
        Arrays.fill(fmParts, -1);
        Arrays.fill(ssgParts, -1);
        Arrays.fill(pcmParts, -1);
        Arrays.fill(fmKeyOn, -1);
        Arrays.fill(ssgKeyOn, -1);
        Arrays.fill(pcmKeyOn, -1);
        Arrays.fill(struck, false);
        Arrays.fill(rhythmKeyOn, -1);
        fmActive = ssgActive = pcmActive = rhythmActive = false;
        rhythmStruck = false;
        rhythmVolume = 0;
    }

    private Fmp7Work work() {
        return driver != null && driver.get() instanceof Fmp7Driver d ? d.getWork() : null;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM, Group.SSG, Group.PCM, Group.RHYTHM);
    }

    @Override
    public int priority() {
        return 44;
    }

    @Override
    public boolean ready() {
        return work() != null;
    }

    /**
     * Puts the parts on the rows and works out which of them were struck since the last snapshot.
     * <p>
     * A part keeps the row it is first seen on: FMP7 numbers its parts within each kind and does
     * not move them, so the map is made once and then stands for the song. The key-on edge has
     * to be found here rather than in {@link #read}, which the source may call more than once.
     */
    @Override
    public void poll() {
        Fmp7Work work = work();
        if (work == null) {
            return;
        }
        Arrays.fill(struck, false);
        rhythmStruck = false;
        rhythmVolume = 0;

        for (int p = 0; p < Fmp7Work.MAX_PART; p++) {
            if (work.mode(p) == Fmp7Work.MODE_NONE) {
                continue;
            }
            if (work.device(p) == Fmp7Work.DEVICE_RHYTHM) {
                // the drums get the meter the visualizer keeps for them, not a row, the same way
                // an OPNA's rhythm generator does
                rhythmActive = true;
                int now = work.keyOn(p);
                if (rhythmKeyOn[p] >= 0 && now != rhythmKeyOn[p]) {
                    rhythmStruck = true;
                }
                rhythmKeyOn[p] = now;
                if (sounding(work, p)) {
                    rhythmVolume = Math.max(rhythmVolume, work.volume(p) / (double) VOLUME_MAX);
                }
                continue;
            }
            switch (work.mode(p)) {
                case Fmp7Work.MODE_FM -> place(work, p, fmParts, fmKeyOn, 0);
                case Fmp7Work.MODE_SSG -> place(work, p, ssgParts, ssgKeyOn, FM_ROWS);
                case Fmp7Work.MODE_PCM -> place(work, p, pcmParts, pcmKeyOn, FM_ROWS + SSG_ROWS);
                default -> { }
            }
        }
    }

    /**
     * Gives the part a row if it has not got one, and notes a key-on on the row it has.
     * <p>
     * A row is only given to a part that is actually sounding, and they are given out in the
     * order the parts first sound. A song may declare more parts of a kind than the visualizer
     * has rows - one here uses sixteen FM parts - and several of them may be there for a section
     * that has not arrived yet; handed out by part number instead, the rows would go to those
     * and the parts carrying the tune would have none.
     */
    private void place(Fmp7Work work, int part, int[] rows, int[] keyOn, int strikeBase) {
        int slot = -1;
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] == part) {
                slot = i;
                break;
            }
            if (rows[i] < 0) {
                if (!sounding(work, part)) {
                    return; // nothing to show yet; the row may be wanted by a part that has
                }
                rows[i] = part;
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            return; // more parts of this kind than the visualizer has rows for
        }
        int now = work.keyOn(part);
        if (keyOn[slot] >= 0 && now != keyOn[slot]) {
            struck[strikeBase + slot] = true;
        }
        keyOn[slot] = now;
        if (sounding(work, part)) {
            switch (work.mode(part)) {
                case Fmp7Work.MODE_FM -> fmActive = true;
                case Fmp7Work.MODE_SSG -> ssgActive = true;
                case Fmp7Work.MODE_PCM -> pcmActive = true;
                default -> { }
            }
        }
    }

    private static boolean sounding(Fmp7Work work, int part) {
        return work.note(part) != Fmp7Work.REST && (work.state(part) & Fmp7Work.PART_PLAY) != 0;
    }

    @Override
    public boolean active(Group group) {
        return switch (group) {
            case FM -> fmActive;
            case SSG -> ssgActive;
            case PCM -> pcmActive;
            case RHYTHM -> rhythmActive;
        };
    }

    @Override
    public int channels(Group group) {
        return switch (group) {
            case FM -> FM_ROWS;
            case SSG -> SSG_ROWS;
            case PCM -> PCM_ROWS;
            case RHYTHM -> 1; // one drum meter, whatever the song hits
        };
    }

    private int[] rowsOf(Group group) {
        return switch (group) {
            case FM -> fmParts;
            case SSG -> ssgParts;
            case PCM -> pcmParts;
            case RHYTHM -> null;
        };
    }

    private int strikeBase(Group group) {
        return switch (group) {
            case FM -> 0;
            case SSG -> FM_ROWS;
            case PCM -> FM_ROWS + SSG_ROWS;
            case RHYTHM -> 0;
        };
    }

    @Override
    public void read(Group group, int slot, FmDspChannel out) {
        Fmp7Work work = work();
        if (group == Group.RHYTHM) {
            out.name = "RHY";
            out.keyOn = rhythmStruck;
            out.sounding = rhythmVolume > 0;
            out.amplitude = rhythmVolume;
            return;
        }
        int[] rows = rowsOf(group);
        if (work == null || rows == null || slot >= rows.length || rows[slot] < 0) {
            return;
        }
        int part = rows[slot];

        out.num = work.partNo(part); // the part number FMP7 itself uses
        out.name = nameOf(work.device(part));
        out.sounding = sounding(work, part);
        out.keyOn = struck[strikeBase(group) + slot];
        out.toneNum = work.tone(part);
        out.volume = work.volume(part);
        out.pan = panOf(work.pan(part));
        out.lfoPitch = work.vibrato(part);
        out.lfoVolume = work.tremolo(part);

        if (group == Group.SSG) {
            out.ssgTone = (work.state(part) & Fmp7Work.PART_TONE) != 0;
            out.ssgNoise = (work.state(part) & Fmp7Work.PART_NOISE) != 0;
            out.ssgNoiseFreq = work.noise(part);
        }

        if (!out.sounding) {
            out.note = -1;
            out.amplitude = 0;
            return;
        }
        out.note = work.note(part);
        // the pitch is counted in sixty-fourths of a semitone and reads one semitone high; what
        // it sits away from the note it was asked to play is the bend and the vibrato in it
        double cents = ((double) work.freq(part) / Fmp7Work.SEMITONE - 1 - out.note) * 100;
        out.detune = (int) Math.round(Math.clamp(cents, -50, 50));
        out.amplitude = Math.min(1, work.volume(part) / (double) VOLUME_MAX);
    }

    @Override
    public boolean masked(Group group, int slot) {
        Fmp7Work work = work();
        int[] rows = rowsOf(group);
        return work != null && rows != null && slot < rows.length && rows[slot] >= 0 && work.masked(rows[slot]);
    }

    /** the sound source the part goes to, short enough for the column it is shown in */
    private static String nameOf(int device) {
        return switch (device) {
            case Fmp7Work.DEVICE_OPNA -> "OPNA";
            case Fmp7Work.DEVICE_OPM -> "OPM";
            case Fmp7Work.DEVICE_SSG -> "SSG";
            case Fmp7Work.DEVICE_PCM -> "PCM";
            case Fmp7Work.DEVICE_ADPCM -> "ADPCM";
            case Fmp7Work.DEVICE_RHYTHM -> "RHY";
            default -> null;
        };
    }

    /** FMP7 pans the other way round from MIDI: 1 is hard right, 128 centre, 255 hard left */
    private static Pan panOf(int pan) {
        if (pan < 32) return Pan.RIGHT;
        if (pan < 112) return Pan.MID_RIGHT;
        if (pan <= 144) return Pan.CENTER;
        if (pan <= 224) return Pan.MID_LEFT;
        return Pan.LEFT;
    }

    /**
     * The right half, which for a part that is only ever described - never read back - is what
     * FMP7 last said about it.
     */
    @Override
    public boolean readDetail(Group group, int slot, TrackDetail out) {
        Fmp7Work work = work();
        int[] rows = rowsOf(group);
        if (work == null || rows == null || slot >= rows.length || rows[slot] < 0) {
            return false;
        }
        int part = rows[slot];
        int state = work.state(part);

        out.header = " PT TONE  VOL  PAN  KT   DT  ENV  LFO  GATE";
        out.text = " %2d  %3d  %3d  %3d %+3d %+4d  %3d %s %5d".formatted(
                work.partNo(part), work.tone(part), work.volume(part), work.pan(part),
                work.keyTranspose(part), work.detune(part), work.envNo(part),
                "%s%s%s%s".formatted(
                        (state & Fmp7Work.PART_LFO0) != 0 ? "1" : "-",
                        (state & Fmp7Work.PART_LFO0 << 1) != 0 ? "2" : "-",
                        (state & Fmp7Work.PART_LFO0 << 2) != 0 ? "3" : "-",
                        (state & Fmp7Work.PART_LFO0 << 3) != 0 ? "4" : "-"),
                work.noteCount(part));
        return true;
    }
}
