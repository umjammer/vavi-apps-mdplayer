/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mfi.synth;

import java.util.EnumSet;
import java.util.Set;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import com.sun.media.sound.AudioSynthesizer;
import vavi.sound.mfi.MfiChip;


/**
 * Gervill, the general midi pcm synthesizer of the jdk, the last stand in for every chip, the
 * rohm sound source's when its dll is not there.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
public class GervillMldSynth extends StreamMldSynth {

    private static final int RATE = 44100;

    @Override
    public String getName() {
        return "gervill";
    }

    @Override
    public String getDescription() {
        return "Gervill";
    }

    @Override
    public Set<MfiChip> getChips() {
        return EnumSet.allOf(MfiChip.class);
    }

    @Override
    public int getPriority() {
        return -100;
    }

    @Override
    protected Synthesizer createSynthesizer() throws MidiUnavailableException {
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            if (info.getName().equals("Gervill") && MidiSystem.getMidiDevice(info) instanceof AudioSynthesizer s) {
                return s;
            }
        }
        throw new MidiUnavailableException("no Gervill");
    }

    @Override
    protected AudioInputStream openStream(Synthesizer synthesizer) throws MidiUnavailableException {
        return ((AudioSynthesizer) synthesizer).openStream(new AudioFormat(RATE, 16, 2, true, false), null);
    }
}
