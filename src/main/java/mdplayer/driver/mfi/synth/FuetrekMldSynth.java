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

import vavi.sound.faith.FaithRom;
import vavi.sound.mfi.MfiChip;
import vavi.sound.midi.fuetrek.FuetrekSynthesizer;


/**
 * The fuetrek sound source ({@link FuetrekSynthesizer}), the preset tones out of the authoring
 * tool's {@code rt_synth_4.dll}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
public class FuetrekMldSynth extends StreamMldSynth {

    @Override
    public String getName() {
        return "fuetrek";
    }

    @Override
    public String getDescription() {
        return "FueTrek UCS";
    }

    @Override
    public Set<MfiChip> getChips() {
        return EnumSet.of(MfiChip.FUETREK);
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean isAvailable() {
        return FaithRom.isAvailable();
    }

    @Override
    public String getRequirement() {
        return "rt_synth_4.dll under " + FaithRom.toolsDirectory() + ", set -Dvavi.sound.faith.path=<dir>";
    }

    @Override
    protected Synthesizer createSynthesizer() {
        return new FuetrekSynthesizer();
    }

    @Override
    protected AudioInputStream openStream(Synthesizer synthesizer) throws MidiUnavailableException {
        return ((FuetrekSynthesizer) synthesizer).openStream();
    }

    /** it handles the vavi-sound exclusives (UCS waves, adpcm) itself */
    @Override
    protected boolean needsMfiReceiver() {
        return false;
    }
}
