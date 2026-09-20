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
import vavi.sound.mfi.faith.FaithType4Player;
import vavi.sound.midi.ucs.UcsSynthesizer;


/**
 * The fuetrek sound source ({@link UcsSynthesizer}), the preset tones out of the authoring
 * tool's {@code rt_synth_4.dll}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
public class UcsMldSynth extends StreamMldSynth {

    @Override
    public String getName() {
        return "ucs";
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
        return FaithType4Player.isAvailable();
    }

    @Override
    public String getRequirement() {
        return "rt_synth_4.dll under " + FaithType4Player.toolsDirectory() + ", set -Dvavi.sound.mfi.faith.path=<dir>";
    }

    @Override
    protected Synthesizer createSynthesizer() {
        return new UcsSynthesizer();
    }

    @Override
    protected AudioInputStream openStream(Synthesizer synthesizer) throws MidiUnavailableException {
        return ((UcsSynthesizer) synthesizer).openStream();
    }

    /** it handles the vavi-sound exclusives (UCS waves, adpcm) itself */
    @Override
    protected boolean needsMfiReceiver() {
        return false;
    }
}
