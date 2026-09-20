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
import vavi.sound.midi.openDoja.FuetrekSynthesizer;
import vavi.sound.midi.openDoja.Ma3Synthesizer;
import vavi.sound.midi.openDoja.OpenDojaSynthesizer;


/**
 * A sound source of openDoja (vavi-sound-sandbox), which takes the file's own voices and waves.
 * vavi-sound-sandbox is not a dependency of the application, without it on the class path this
 * is not {@link #isAvailable() available}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-20 nsano initial version <br>
 */
public abstract class OpenDojaMldSynth extends StreamMldSynth {

    /** the synthesizer class, by name, for {@link #isAvailable()} */
    protected abstract String getClassName();

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName(getClassName(), false, OpenDojaMldSynth.class.getClassLoader());
            Class.forName("opendoja.audio.mld.SamplerProvider", false, OpenDojaMldSynth.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Override
    public String getRequirement() {
        return "vavi-sound-sandbox on the class path";
    }

    @Override
    protected AudioInputStream openStream(Synthesizer synthesizer) throws MidiUnavailableException {
        return ((OpenDojaSynthesizer) synthesizer).openStream();
    }

    /** the MA-3 of openDoja */
    public static class Ma3 extends OpenDojaMldSynth {

        @Override
        public String getName() {
            return "opendoja.ma3";
        }

        /** below nuked: it doesn't sound the nec machine dependent songs nuked does (02 TRANSPARENT.mld) */
        @Override
        public int getPriority() {
            return -10;
        }

        @Override
        public String getDescription() {
            return "openDoja MA-3";
        }

        @Override
        public Set<MfiChip> getChips() {
            return EnumSet.of(MfiChip.YAMAHA);
        }

        @Override
        protected String getClassName() {
            return "vavi.sound.midi.openDoja.Ma3Synthesizer";
        }

        @Override
        protected Synthesizer createSynthesizer() {
            return new Ma3Synthesizer();
        }
    }

    /** the fuetrek sound source of openDoja */
    public static class Fuetrek extends OpenDojaMldSynth {

        @Override
        public String getName() {
            return "opendoja.fuetrek";
        }

        @Override
        public String getDescription() {
            return "openDoja FueTrek";
        }

        @Override
        public Set<MfiChip> getChips() {
            return EnumSet.of(MfiChip.FUETREK);
        }

        @Override
        protected String getClassName() {
            return "vavi.sound.midi.openDoja.FuetrekSynthesizer";
        }

        @Override
        protected Synthesizer createSynthesizer() {
            return new FuetrekSynthesizer();
        }
    }
}
