/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mfi.synth;

import java.util.EnumSet;
import java.util.Set;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioInputStream;

import vavi.sound.mfi.MfiChip;
import vavi.sound.mfi.ma7.Ma7Rom;
import vavi.sound.midi.ma7.Ma7Synthesizer;


/**
 * The yamaha MA-7 ({@link Ma7Synthesizer}), the rom out of {@code libM7_EmuSmw7.so}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-20 nsano initial version <br>
 */
public class Ma7MldSynth extends StreamMldSynth {

    @Override
    public String getName() {
        return "ma7";
    }

    @Override
    public String getDescription() {
        return "Yamaha MA-7";
    }

    @Override
    public Set<MfiChip> getChips() {
        return EnumSet.of(MfiChip.YAMAHA);
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean isAvailable() {
        return Ma7Rom.isAvailable();
    }

    @Override
    public String getRequirement() {
        return "no " + Ma7Rom.path() + ", set -D" + Ma7Rom.PATH_KEY + "=<" + Ma7Rom.SO + ">";
    }

    @Override
    protected Synthesizer createSynthesizer() {
        return new Ma7Synthesizer();
    }

    @Override
    protected AudioInputStream openStream(Synthesizer synthesizer) throws MidiUnavailableException {
        return ((Ma7Synthesizer) synthesizer).openStream();
    }

    /** it handles the vavi-sound exclusives (the mfi values, adpcm) itself */
    @Override
    protected boolean needsMfiReceiver() {
        return false;
    }
}
