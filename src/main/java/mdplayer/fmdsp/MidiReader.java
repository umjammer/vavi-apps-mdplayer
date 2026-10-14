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
import mdplayer.chips.MidiPlugin;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.mid.MidiDriver;
import mdplayer.driver.rcp.RcpDriver;
import mdplayer.driver.rcp.RcsDriver;
import mdplayer.driver.zms.ZmsDriver;
import vavi.sound.midi.visualizer.fmdsp.NoteSpectrum;
import vavi.sound.visualizer.fmdsp.FftDataSource;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;


/**
 * The MIDI drivers - RCP, RCS, MID and ZMS - which emulate no chip at all.
 * <p>
 * There is nothing to read back: the notes go out to a synthesizer that reports nothing, so what
 * is shown is what went past {@link MidiPlugin} on its way there. A MIDI note is already a note,
 * so these keys are exact; the volume is the note's velocity scaled by the channel's controller 7
 * and its expression, and the pan is controller 10.
 * <p>
 * Sixteen channels do not fit nine rows, so they are taken in the order they first sound. The
 * spectrum is not limited that way - see {@link #readFft}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class MidiReader implements FmDspChipReader, FftDataSource {

    private static final int CHANNELS = 16;

    /** GM puts the drums on channel 10 */
    private static final int DRUM_CHANNEL = 9;

    private ChipRegister chipRegister;
    private Supplier<BaseDriver> driver;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevNotes = new int[CHANNELS];
    private boolean active;

    /** the analyzer bars, which are the notes here rather than the sound - see {@link #readFft} */
    private final NoteSpectrum spectrum = new NoteSpectrum();

    /** keys the previous frame saw held, which a strike is spotted against - see {@link #spectral} */
    private final boolean[][] struck = new boolean[CHANNELS][128];

    /** what each key was struck at and when [ns] */
    private final double[][] strikeLevels = new double[CHANNELS][128];
    private final long[][] strikeNanos = new long[CHANNELS][128];

    /** MIDI channel shown on each row, -1 = none yet */
    private final int[] slotChannels = new int[CHANNELS];

    private int mappedChannels;

    @Override
    public String chipName() {
        return "MIDI";
    }

    @Override
    public void bind(ChipRegister chipRegister) {
        this.chipRegister = chipRegister;
    }

    @Override
    public void bind(Supplier<BaseDriver> driver) {
        this.driver = driver;
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevNotes, 0);
        for (int ch = 0; ch < CHANNELS; ch++) {
            Arrays.fill(struck[ch], false);
            Arrays.fill(strikeLevels[ch], 0);
            Arrays.fill(strikeNanos[ch], 0);
        }
        Arrays.fill(slotChannels, -1);
        mappedChannels = 0;
        active = false;
        if (midi() != null) midi().clearChannels();
    }

    private MidiPlugin midi() {
        return chipRegister == null ? null : chipRegister.plugin(MidiPlugin.class);
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.PCM);
    }

    @Override
    public int priority() {
        return 43;
    }

    @Override
    public boolean ready() {
        if (driver == null || midi() == null) return false;
        BaseDriver d = driver.get();
        return d instanceof RcpDriver || d instanceof RcsDriver
                || d instanceof MidiDriver || d instanceof ZmsDriver;
    }

    @Override
    public void poll() {
        for (int ch = 0; ch < CHANNELS && mappedChannels < slotChannels.length; ch++) {
            if (sounding(ch) && slotOf(ch) < 0) {
                slotChannels[mappedChannels++] = ch;
            }
        }
    }

    private int slotOf(int ch) {
        for (int s = 0; s < mappedChannels; s++) {
            if (slotChannels[s] == ch) return s;
        }
        return -1;
    }

    private boolean sounding(int ch) {
        MidiPlugin midi = midi();
        return midi != null && midi.velocity(ch) > 0;
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
    public void read(Group group, int slot, FmDspChannel out) {
        out.name = "MIDI";
        int ch = slotChannels[slot];
        MidiPlugin midi = midi();
        if (ch < 0 || midi == null) return;

        boolean sounding = sounding(ch);
        int note = midi.note(ch);

        out.num = ch + 1; // the MIDI channel, wherever the slot map put it
        out.pcmCh = ch + 1;
        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || note != prevNotes[ch]);
        prevSoundings[ch] = sounding;
        prevNotes[ch] = note;

        // a MIDI note is a note already, and the display counts from the same C
        out.note = sounding ? note : -1;
        out.toneNum = midi.program(ch);
        out.volume = midi.velocity(ch);
        out.amplitude = sounding ? amplitude(midi, ch) : 0;
        out.pan = panOf(midi.pan(ch));
    }

    /**
     * How loud the channel is playing, {@code 0..1}: the velocity it was struck at, through the
     * volume the part sits at and the expression it is being swelled or faded with. Nothing
     * reports back what the synthesizer made of it, so this is as close as the display gets.
     */
    private static double amplitude(MidiPlugin midi, int ch) {
        return midi.velocity(ch) * midi.volume(ch) * midi.expression(ch) / (127.0 * 127 * 127);
    }

    /**
     * The spectrum, which is drawn from the notes: a MIDI song is heard from a synthesizer that
     * mixes its own sound, so mdplayer renders nothing for an analyzer to measure and the bars
     * stood empty for the whole song. Every key of every one of the sixteen channels goes in - the
     * rows are limited to the nine the display has, the music is not - with the drum channel
     * taken as a kit rather than as notes, each at the level {@link #spectral} has it at. See
     * {@link NoteSpectrum} for what it makes of them.
     * <p>
     * Called once a drawn frame, which is also what times the decay and the display's ballistics.
     */
    @Override
    public void readFft(int[] out) {
        MidiPlugin midi = midi();
        spectrum.clear();
        if (midi != null) {
            long now = System.nanoTime();
            for (int ch = 0; ch < CHANNELS; ch++) {
                for (int note = 0; note < 128; note++) {
                    double amplitude = spectral(midi, ch, note, now);
                    if (amplitude < QUIET) continue;
                    if (ch == DRUM_CHANNEL) spectrum.addDrum(note, amplitude);
                    else spectrum.add(note, amplitude);
                }
            }
        }
        spectrum.readFft(out);
    }

    /**
     * What one key puts into the spectrum, which is not what its channel puts into its
     * {@linkplain #amplitude meter}.
     * <p>
     * A meter holds a sounding part at the level it was struck at, because the part is still
     * playing and a meter that fell would say it had stopped. A spectrum is the sound itself:
     * almost everything a synthesizer plays gets quieter the longer it is held, and a bar left
     * standing where the note struck it never comes back down for the next note to rise above. So
     * each key decays from its own strike and has no floor under it, and the bars bounce with the
     * music - the same fall {@code MidiFmDspSource#spectral} gives a stream it watches directly.
     * <p>
     * Per key rather than per channel because a MIDI stream cannot be relied on to let go: the
     * drum channel of a real ZMS collects ten hits it never sends a note off for, and a channel
     * envelope kept every one of them sounding at full level for the rest of the song - ten drum
     * bands stacked across the axis, which is the display filled to the ceiling and holding still.
     * A key that decays on its own falls out of the picture whether the song releases it or not,
     * and a drum {@linkplain #DRUM_TAU dies away} faster than a note does, the way a hit does.
     * <p>
     * A key struck again while it is still down - a note on with no note off in between - is
     * missed, the way a polled register file misses a channel retriggered on the same note.
     *
     * @param now {@link System#nanoTime}, one reading for the whole frame
     * @return the level to draw the key at, 0 for one that is not sounding
     */
    private double spectral(MidiPlugin midi, int ch, int note, long now) {
        boolean key = midi.key(ch, note);
        if (key && !struck[ch][note]) {
            // the channel's velocity is the one that struck it: the notes of a chord arrive
            // together and share it, which is as much as the stream says
            strikeLevels[ch][note] = midi.velocity(ch) / 127.0;
            strikeNanos[ch][note] = now;
        }
        struck[ch][note] = key;
        if (!key) return 0; // released: gone from the picture, and the bars fall on their own

        double dt = Math.max(0, (now - strikeNanos[ch][note]) / 1e9);
        return strikeLevels[ch][note] * Math.exp(-dt / (ch == DRUM_CHANNEL ? DRUM_TAU : NOTE_TAU))
                * midi.volume(ch) * midi.expression(ch) / (127.0 * 127);
    }

    /** how fast a held note's spectrum decays [s], and how fast a drum hit's does */
    private static final double NOTE_TAU = 0.7, DRUM_TAU = 0.25;

    /** below this a key is past drawing: about 20 dB under the bottom of the axis */
    private static final double QUIET = 1e-3;

    /** the notes stand in for the sound, which never passes through mdplayer's mixer */
    @Override
    public FftDataSource spectrum() {
        return this;
    }

    /** controller 10: 0 hard left, 64 centre, 127 hard right */
    private static Pan panOf(int pan) {
        if (pan < 16) return Pan.LEFT;
        if (pan < 56) return Pan.MID_LEFT;
        if (pan <= 72) return Pan.CENTER;
        if (pan <= 112) return Pan.MID_RIGHT;
        return Pan.RIGHT;
    }
}
