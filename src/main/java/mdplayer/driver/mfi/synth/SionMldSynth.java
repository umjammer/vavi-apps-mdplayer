/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mfi.synth;

import java.util.EnumSet;
import java.util.Set;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;

import mdplayer.driver.mfi.MldSynth;
import vavi.sound.mfi.MfiChip;
import vavi.sound.midi.sion.SionSynthesizer;


/**
 * SiON, the software synthesizer whose FM module has the MA-3 algorithms and wave shapes, so it
 * takes the voices of an mfi as they are, and its PCM channel the wave table voices and the
 * streams.
 * <p>
 * Better than {@link NukedMldSynth}, which stands in with an OPL3, a chip an mfi was never
 * written for, and not the real thing {@link Ma7MldSynth} is, so it sits between them.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-20 nsano initial version <br>
 */
public class SionMldSynth implements MldSynth {

    private SionSynthesizer synthesizer;
    private Receiver receiver;
    private int[][] buffer = new int[2][0];

    @Override
    public String getName() {
        return "sion";
    }

    @Override
    public String getDescription() {
        return "SiON MA-3";
    }

    @Override
    public Set<MfiChip> getChips() {
        return EnumSet.of(MfiChip.YAMAHA);
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public void open() throws MidiUnavailableException {
        // SiONDriver keeps the one driver ever made in a static field it never lets go of, so
        // the song after the first is refused unless plural drivers are allowed
        System.setProperty("org.si.sion.allowPluralDrivers", "true");
        synthesizer = new SionSynthesizer();
        synthesizer.openRenderer();
        // the machine dependent messages have to be converted for it, the voices of a song are
        // in them, but the audio data is its own: it sounds the stream in the song
        receiver = new MfiReceiver(synthesizer.getReceiver(), false);
    }

    @Override
    public Receiver getReceiver() {
        return receiver;
    }

    @Override
    public int getSampleRate() {
        return (int) synthesizer.getSampleRate();
    }

    @Override
    public void render(int[] left, int[] right, int frames) {
        if (buffer[0].length < frames) buffer = new int[2][frames];
        synthesizer.render(buffer, frames);
        System.arraycopy(buffer[0], 0, left, 0, frames);
        System.arraycopy(buffer[1], 0, right, 0, frames);
    }

    @Override
    public void close() {
        if (receiver != null) receiver.close();
        if (synthesizer != null) synthesizer.close();
    }
}
