/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mfi;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiChip;
import vavi.sound.mfi.vavi.VaviMfiSynthesizer;
import vavi.sound.mfi.vavi.sequencer.AudioDataSequencer;
import vavi.sound.mobile.MobileExclusive;

import static java.lang.System.getLogger;
import static vavi.sound.midi.VaviMidiDeviceProvider.MANUFACTURER_ID;


/**
 * A synthesizer an mfi is played on, rendering when it is asked to rather than into a line of
 * its own, so the song is mixed, recorded and paused like any other.
 * <p>
 * This is a service provider interface: an implementation is listed in
 * {@code META-INF/services/mdplayer.driver.mfi.MldSynth}, and {@link #forChip} picks one for the
 * chip of a song. A system property can pick one by its {@link #getName() name} for one chip,
 * {@code mdplayer.mfi.synth.yamaha=ma7}, or for every chip, {@code mdplayer.mfi.synth=gervill}.
 * Without one, the {@link #isAvailable() available} synthesizer of the highest
 * {@link #getPriority() priority} that {@link #getChips() sounds as the chip} is taken.
 * <p>
 * An instance the {@link ServiceLoader} makes is light, it only gets its sound source when it is
 * {@link #open() opened}, once, for a song. Every message is sent, and every frame rendered, on
 * the thread of the player's render loop.
 * <p>
 * The exclusives vavi-sound converts the machine dependent messages into (the adpcm, the UCS
 * waves) go through {@link VaviMfiSynthesizer#processSpecial}, see {@link MfiReceiver}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 *          0.01 2026-09-20 nsano service provider interface <br>
 */
public interface MldSynth extends AutoCloseable {

    /**
     * system property: a synthesizer by its {@link #getName() name} for every file, or, with
     * {@code .yamaha}, {@code .fuetrek} or {@code .rohm} after it, for the files of that chip
     */
    String SYNTH_KEY = "mdplayer.mfi.synth";

    // ---- the provider

    /** what {@link #SYNTH_KEY} picks it by, lower case */
    String getName();

    /** for the play list and the log */
    String getDescription();

    /** the chips it is the sound of or stands in for */
    Set<MfiChip> getChips();

    /** of the synthesizers for a chip, the highest one available plays it */
    int getPriority();

    /** whether what it needs is here, the library or the rom */
    default boolean isAvailable() {
        return true;
    }

    /** what it needs when it is not {@link #isAvailable()}, for the log */
    default String getRequirement() {
        return "";
    }

    // ---- the synthesizer

    /** gets the sound source, before anything else */
    void open() throws IOException, MidiUnavailableException;

    /** where the messages go */
    Receiver getReceiver();

    /** the rate {@link #render} renders at */
    int getSampleRate();

    /**
     * @param left overwritten
     * @param right overwritten
     */
    void render(int[] left, int[] right, int frames);

    @Override
    void close();

    // ----

    /** every synthesizer listed, fresh instances, highest priority first */
    static List<MldSynth> providers() {
        List<MldSynth> synths = new ArrayList<>();
        var i = ServiceLoader.load(MldSynth.class).iterator();
        while (true) {
            try {
                if (!i.hasNext()) break;
                synths.add(i.next());
            } catch (ServiceConfigurationError | LinkageError e) {
                // a library it is made of is not on the class path
                Holder.logger.log(Level.DEBUG, e.toString());
            }
        }
        synths.sort(Comparator.comparingInt(MldSynth::getPriority).reversed());
        return synths;
    }

    /**
     * The synthesizer for a chip, the one {@link #SYNTH_KEY} names or the highest one available,
     * opened.
     *
     * @throws IllegalArgumentException the name {@link #SYNTH_KEY} gives is not a synthesizer's
     * @throws IllegalStateException the synthesizer named can not be opened, or no synthesizer can
     */
    static MldSynth forChip(MfiChip chip) {
        List<MldSynth> synths = providers();

        String name = System.getProperty(SYNTH_KEY + "." + chip.name().toLowerCase(Locale.ROOT));
        if (name == null || name.isBlank()) {
            name = System.getProperty(SYNTH_KEY);
        }
        if (name != null && !name.isBlank()) {
            String key = name.strip().toLowerCase(Locale.ROOT);
            MldSynth synth = synths.stream().filter(s -> s.getName().equals(key)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(SYNTH_KEY + ": " + key + ", one of "
                            + synths.stream().map(MldSynth::getName).collect(Collectors.joining(", "))));
            try {
                synth.open();
                return synth;
            } catch (IOException | MidiUnavailableException | RuntimeException e) {
                throw new IllegalStateException(key + ": " + e.getMessage(), e);
            }
        }

        for (MldSynth synth : synths) {
            if (!synth.getChips().contains(chip)) continue;
            if (!synth.isAvailable()) {
                Holder.logger.log(Level.WARNING, "no " + synth.getName() + " for " + chip + ": " + synth.getRequirement());
                continue;
            }
            try {
                synth.open();
                return synth;
            } catch (IOException | MidiUnavailableException | RuntimeException e) {
                Holder.logger.log(Level.WARNING, "no " + synth.getName() + " for " + chip + ": " + e, e);
            }
        }
        throw new IllegalStateException("no synthesizer for " + chip);
    }

    /** for the logger of an interface */
    final class Holder {
        static final Logger logger = getLogger(MldSynth.class.getName());
    }

    /**
     * Hands the exclusives of vavi-sound's mfi (adpcm, machine dependent) to vavi-sound, and
     * everything to the synthesizer, as {@link VaviMfiSynthesizer.VaviMfiReceiver} does. For a
     * synthesizer that doesn't take them itself.
     */
    class MfiReceiver implements MidiDeviceReceiver {
        private final Receiver receiver;

        /** whether the audio data of an MFi 4 song goes to vavi-sound too, see the constructor */
        private final boolean audioDataToo;

        /** everything goes to vavi-sound */
        public MfiReceiver(Receiver receiver) {
            this(receiver, true);
        }

        /**
         * @param audioDataToo false: the audio data (adpcm) of an MFi 4 song is the
         *        synthesizer's, which sounds it in the song, so it is not handed to
         *        vavi-sound's {@link vavi.sound.mobile.AudioEngine} as well, which would play
         *        it a second time beside the song
         */
        public MfiReceiver(Receiver receiver, boolean audioDataToo) {
            this.receiver = receiver;
            this.audioDataToo = audioDataToo;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (message instanceof SysexMessage sysex && (audioDataToo || !isAudioData(sysex))) {
                try {
                    VaviMfiSynthesizer.processSpecial(sysex, this);
                } catch (InvalidMfiDataException | RuntimeException e) {
                    Holder.logger.log(Level.DEBUG, e.getMessage(), e);
                }
            }
            receiver.send(message, timeStamp);
        }

        /**
         * Whether it is one of the exclusives an MFi 4 song's audio data travels as, which
         * vavi-sound hands to an {@link vavi.sound.mobile.AudioEngine}, packed
         * {@code 45 7f <encode87(45 02 ...)> f7}.
         */
        private static boolean isAudioData(SysexMessage sysex) {
            byte[] data = sysex.getData();
            if (data.length < 2 || (data[0] & 0xff) != MANUFACTURER_ID
                    || (data[1] & 0xff) != MobileExclusive.MIDI_SYSEX_FUNCTION_ID_PACKED) {
                return false;
            }
            try {
                byte[] unpacked = MobileExclusive.unpack(data);
                return unpacked.length >= 2 && (unpacked[0] & 0xff) == MANUFACTURER_ID
                        && (unpacked[1] & 0xff) == AudioDataSequencer.MFi_SYSEX_FUNCTION_ID_MFi4;
            } catch (RuntimeException e) {
                Holder.logger.log(Level.DEBUG, "unpack: " + e);
                return false;
            }
        }

        @Override
        public void close() {
            receiver.close();
        }

        @Override
        public MidiDevice getMidiDevice() {
            return null;
        }
    }
}
