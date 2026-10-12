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
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;


/**
 * The shape the Amiga trackers share: a handful of voices, each with a note taken straight from
 * the pattern, a volume the envelope has worked on, and on HVL a pan.
 * <p>
 * These drivers render their own audio and register no chip, so there are no registers to read -
 * the replayer's voices are the state. The note is a tracker note number rather than a frequency,
 * which is what a tracker means by a key anyway.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public abstract class AmigaTrackerReader implements FmDspChipReader {

    /** the most voices any of these has */
    private static final int MAX_VOICES = 16;

    /** a tracker note of 1 is the replayer's lowest, which these tune to o1 c */
    private static final int lowestNote = 12;

    Supplier<BaseDriver> driver;

    private final boolean[] prevSoundings = new boolean[MAX_VOICES];
    private final int[] prevNotes = new int[MAX_VOICES];
    private boolean active;

    /** how many voices the song has, or 0 when nothing is playing */
    protected abstract int voices();

    /**
     * The tracker note the voice is on, 0 for none.
     * <p>
     * Not the note the pattern step named - that is only written as the step is read, and sits
     * still afterwards. What the voice is actually sounding is its Amiga period, which
     * {@link #noteOfPeriod} turns back into a note.
     */
    protected abstract int note(int voice);

    /** the voice's volume against {@link #volumeMax} */
    protected abstract int volume(int voice);

    /** the loudest a voice's volume goes */
    protected abstract int volumeMax();

    Pan pan(int voice) {
        return Pan.CENTER;
    }

    /**
     * The note whose period is nearest the one given, which is how a tracker names its pitches.
     * The table runs from note 1 upwards, a period per semitone.
     */
    static int noteOfPeriod(int[] periods, int period) {
        if (period <= 0) return 0;
        int best = 0;
        int bestDiff = Integer.MAX_VALUE;
        for (int note = 1; note < periods.length; note++) {
            int diff = Math.abs(periods[note] - period);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = note;
            }
        }
        return best;
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
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevNotes, 0);
        active = false;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.PCM);
    }

    @Override
    public int priority() {
        return 45;
    }

    private boolean sounding(int voice) {
        return voice < voices() && note(voice) > 0 && volume(voice) > 0;
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int v = 0; v < voices() && !active; v++) active = sounding(v);
        }
        return active;
    }

    @Override
    public int channels(Group group) {
        return Math.max(voices(), 1);
    }

    @Override
    public void read(Group group, int voice, FmDspChannel out) {
        out.name = "PCM";
        out.num = voice + 1;
        out.pcmCh = voice + 1;
        out.pan = pan(voice);
        if (voice >= voices()) return;

        int note = note(voice);
        boolean sounding = sounding(voice);
        int volume = volume(voice);

        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[voice] || note != prevNotes[voice]);
        prevSoundings[voice] = sounding;
        prevNotes[voice] = note;

        out.volume = volume;
        out.amplitude = sounding ? Math.min(1, volume / (double) volumeMax()) : 0;
        // a tracker names a note rather than a frequency, so show it as it is written
        out.note = sounding ? lowestNote + note - 1 : -1;
    }
}
