/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mfi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Locale;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import com.sun.media.sound.AudioSynthesizer;
import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.faith.FaithType4Player;
import vavi.sound.mfi.ucs.FuetrekRom;
import vavi.sound.mfi.ucs.UcsAudioEngine;
import vavi.sound.mfi.ucs.UcsMfiSynthesizer.UcsMfiReceiver;
import vavi.sound.mfi.vavi.VaviMfiSynthesizer;
import vavi.sound.midi.ymf262.NukedSynthesizer;

import static java.lang.System.getLogger;


/**
 * A synthesizer an mfi is played on, rendering when it is asked to rather than into a line of
 * its own, so the song is mixed, recorded and paused like any other.
 * <p>
 * Every message is sent, and every frame rendered, on the thread of the player's render loop.
 * <p>
 * The exclusives vavi-sound converts the machine dependent messages into (the adpcm, the UCS
 * waves) go through {@link VaviMfiSynthesizer#processSpecial}; the adpcm engines of vavi-sound
 * play into a line of their own, that part of a song is not mixed here.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
public interface MldSynth extends AutoCloseable {

    /** system property: a synthesizer for every file, {@code nuked}, {@code ucs} or {@code gervill} */
    String SYNTH_KEY = "mdplayer.mfi.synth";

    /** where the messages go */
    Receiver getReceiver();

    /** the rate {@link #render} renders at */
    int getSampleRate();

    /**
     * @param left overwritten
     * @param right overwritten
     */
    void render(int[] left, int[] right, int frames);

    /** for the play list and the log */
    String getName();

    @Override
    void close();

    /**
     * The synthesizer for a chip, or the one standing in for it.
     * <ul>
     *  <li>{@link MldChip#YAMAHA}: Nuked OPL3 with the voices the file sends</li>
     *  <li>{@link MldChip#FUETREK}: the fuetrek sound source, needs {@code rt_synth_4.dll}
     *      ({@code -Dvavi.sound.mfi.faith.path}), Nuked OPL3 without it</li>
     *  <li>{@link MldChip#ROHM}: no emulator of it, Gervill's general midi pcm stands in</li>
     * </ul>
     */
    static MldSynth forChip(MldChip chip) {
        String forced = System.getProperty(SYNTH_KEY);
        if (forced != null && !forced.isBlank()) {
            return byName(forced.strip().toLowerCase(Locale.ROOT));
        }
        return switch (chip) {
            case YAMAHA -> new Nuked();
            case FUETREK -> {
                if (FaithType4Player.isAvailable()) {
                    yield new Ucs();
                }
                Holder.logger.log(Level.WARNING, "no rt_synth_4.dll under " + FaithType4Player.toolsDirectory()
                        + ", the fuetrek song is played by the OPL3; set -Dvavi.sound.mfi.faith.path=<dir>");
                yield new Nuked();
            }
            case ROHM -> new Gervill();
        };
    }

    /** @throws IllegalArgumentException unknown name */
    static MldSynth byName(String name) {
        return switch (name) {
            case "nuked" -> new Nuked();
            case "ucs" -> new Ucs();
            case "gervill" -> new Gervill();
            default -> throw new IllegalArgumentException(SYNTH_KEY + ": " + name);
        };
    }

    /** for the logger of an interface */
    final class Holder {
        static final Logger logger = getLogger(MldSynth.class.getName());
    }

    /**
     * Hands the exclusives of vavi-sound's mfi (adpcm, machine dependent) to vavi-sound, and
     * everything to the synthesizer, as {@link VaviMfiSynthesizer.VaviMfiReceiver} does.
     */
    class MfiReceiver implements MidiDeviceReceiver {
        private final Receiver receiver;

        MfiReceiver(Receiver receiver) {
            this.receiver = receiver;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (message instanceof SysexMessage sysex) {
                try {
                    VaviMfiSynthesizer.processSpecial(sysex, this);
                } catch (InvalidMfiDataException | RuntimeException e) {
                    Holder.logger.log(Level.DEBUG, e.getMessage(), e);
                }
            }
            receiver.send(message, timeStamp);
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

    /** the Yamaha FM stand in: Nuked OPL3, which takes the FM voices an mfi sends */
    final class Nuked implements MldSynth {
        private final NukedSynthesizer synthesizer = new NukedSynthesizer();
        private final Receiver receiver;
        private int[][] buffer = new int[2][0];

        Nuked() {
            synthesizer.openRenderer();
            try {
                receiver = new MfiReceiver(synthesizer.getReceiver());
            } catch (MidiUnavailableException e) {
                throw new IllegalStateException(e);
            }
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
        public String getName() {
            return "Nuked OPL3";
        }

        @Override
        public void close() {
            synthesizer.close();
        }
    }

    /** the fuetrek sound source, the preset tones out of the authoring tool's dll */
    final class Ucs implements MldSynth {
        private final UcsAudioEngine engine;
        private final Receiver receiver;
        private byte[] pcm = new byte[0];

        Ucs() {
            try {
                engine = new UcsAudioEngine(FuetrekRom.getInstance(), false);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            // it handles the vavi-sound exclusives (UCS waves, adpcm) itself
            receiver = new UcsMfiReceiver(engine);
        }

        @Override
        public Receiver getReceiver() {
            return receiver;
        }

        @Override
        public int getSampleRate() {
            return UcsAudioEngine.SAMPLE_RATE;
        }

        @Override
        public void render(int[] left, int[] right, int frames) {
            if (pcm.length < frames * 4) pcm = new byte[frames * 4];
            engine.render(pcm, frames);
            for (int i = 0; i < frames; i++) {
                left[i] = (short) ((pcm[i * 4] & 0xff) | (pcm[i * 4 + 1] << 8));
                right[i] = (short) ((pcm[i * 4 + 2] & 0xff) | (pcm[i * 4 + 3] << 8));
            }
        }

        @Override
        public String getName() {
            return "FueTrek UCS";
        }

        @Override
        public void close() {
            receiver.close();
            engine.close();
        }
    }

    /**
     * Gervill, the general midi pcm synthesizer of the jdk, standing in for a pcm sound source
     * there is no emulator of (rohm).
     */
    final class Gervill implements MldSynth {
        private static final int RATE = 44100;
        private final AudioSynthesizer synthesizer;
        private final AudioInputStream stream;
        private final Receiver receiver;
        private byte[] pcm = new byte[0];

        Gervill() {
            try {
                AudioSynthesizer found = null;
                for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
                    if (info.getName().equals("Gervill") && MidiSystem.getMidiDevice(info) instanceof AudioSynthesizer s) {
                        found = s;
                        break;
                    }
                }
                if (found == null) throw new IllegalStateException("no Gervill");
                synthesizer = found;
                stream = synthesizer.openStream(new AudioFormat(RATE, 16, 2, true, false), null);
                receiver = new MfiReceiver(synthesizer.getReceiver());
            } catch (MidiUnavailableException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Receiver getReceiver() {
            return receiver;
        }

        @Override
        public int getSampleRate() {
            return RATE;
        }

        @Override
        public void render(int[] left, int[] right, int frames) {
            if (pcm.length < frames * 4) pcm = new byte[frames * 4];
            int n = 0;
            try {
                while (n < frames * 4) {
                    int r = stream.read(pcm, n, frames * 4 - n);
                    if (r < 0) break;
                    n += r;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            for (int i = 0; i < frames; i++) {
                if (i * 4 + 3 < n) {
                    left[i] = (short) ((pcm[i * 4] & 0xff) | (pcm[i * 4 + 1] << 8));
                    right[i] = (short) ((pcm[i * 4 + 2] & 0xff) | (pcm[i * 4 + 3] << 8));
                } else {
                    left[i] = right[i] = 0;
                }
            }
        }

        @Override
        public String getName() {
            return "Gervill (for Rohm)";
        }

        @Override
        public void close() {
            receiver.close();
            synthesizer.close();
        }
    }
}
