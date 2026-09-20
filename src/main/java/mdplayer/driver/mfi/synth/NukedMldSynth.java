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
import vavi.sound.midi.ymf262.NukedSynthesizer;


/**
 * The Yamaha FM stand in: Nuked OPL3, which takes the FM voices an mfi sends.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
public class NukedMldSynth implements MldSynth {

    private NukedSynthesizer synthesizer;
    private Receiver receiver;
    private int[][] buffer = new int[2][0];

    @Override
    public String getName() {
        return "nuked";
    }

    @Override
    public String getDescription() {
        return "Nuked OPL3";
    }

    @Override
    public Set<MfiChip> getChips() {
        return EnumSet.of(MfiChip.YAMAHA);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void open() throws MidiUnavailableException {
        synthesizer = new NukedSynthesizer();
        synthesizer.openRenderer();
        receiver = new MfiReceiver(synthesizer.getReceiver());
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
        if (synthesizer != null) synthesizer.close();
    }
}
