/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.smaf;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.SysexMessage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import mdplayer.Common.EnmModel;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.MidiChannels;
import mdplayer.driver.MidiSchedule;
import mdplayer.driver.MidiSchedule.Event;
import mdplayer.lib.smaf.SmafFile;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;
import vavi.sound.midi.smaf.SmafMa7Synthesizer;
import vavi.sound.midi.smaf.SmafMidiFileReader;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.AudioEngineMixer;

import static java.lang.System.getLogger;


/**
 * SMAF (".mmf") driver, played in pure java on the yamaha MA-7.
 * <p>
 * The song is read by vavi-sound into a midi sequence ({@link SmafMidiFileReader}) and played here,
 * against the samples rendered, on {@link SmafMa7Synthesizer} of vavi-apps-mfiplayer - the MA-7
 * emulator of yamaha's {@code libM7_EmuSmw7.so} with the smaf reading of a sequence's exclusives.
 * That is one sound source for every SMAF generation: an MA-1/2/3/5 song is played by the MA-7,
 * which is what a later phone did with it too. Like the mfi and sid drivers this overrides
 * {@link #render} instead of feeding a chip.
 * <p>
 * The stream waves of a song are mixed into the song by the synthesizer itself, on its own bus and
 * before it cuts anything to 16 bit, so this does not mix them and only asks for the room the sum
 * needs, see {@link #headroom}.
 * <p>
 * This is what {@link SmafPlugin} builds. {@link SmafDriver}, which plays the real thing -
 * mmftoolc.exe on an emulated PC - is still here and still works; see the readme for how to go
 * back to it.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-21 nsano initial version <br>
 */
public class SmafDriver2 extends BaseDriver {

    private static final Logger logger = getLogger(SmafDriver2.class.getName());

    /** how long a song rings on after its last event [s] */
    private static final double TAIL_SECONDS = 2;

    /** the frames the synthesizer renders at a time, a message waits at most this long */
    private static final int BLOCK = 32;

    /** what the messages sent have left the channels at, for the visualizer */
    private final MidiChannels channels = new MidiChannels();

    private SmafMa7Synthesizer synthesizer;
    private AudioInputStream stream;
    private Receiver receiver;

    private List<Event> events = List.of();
    private int next;
    /** the output frame about to be rendered */
    private long position;
    /** the output frame the song is over at */
    private long end;

    // the synthesizer's rate to the output rate
    private double step = 1;
    private double frac;
    private final int[] left = new int[BLOCK], right = new int[BLOCK];
    private int blockPos = BLOCK;
    /** what {@link #stream} gave and {@link #nextSourceFrame} has not turned into frames yet */
    private final byte[] pcm = new byte[BLOCK * 4];

    /** what the song is scaled by so that the sound source and its streams fit in 16 bit */
    private static final double HEADROOM = 0.5;

    private int outputRate;
    private int curL, curR, prevL, prevR;

    public SmafDriver2(BasePlugin<? extends BaseDriver> plugin) {
        super(plugin);
    }

    public SmafDriver2() {
        this(null); // gross
    }

    @Override
    public MetaData retrieveMetaData(byte[] buf, Object... args) {
        SmafFile smaf;
        try {
            smaf = SmafFile.decode(buf);
        } catch (IOException e) {
logger.log(Level.DEBUG, "not a smaf: " + e.getMessage());
            return null;
        }

        MetaData md = new MetaData();
        String title = smaf.getTitle() != null ? smaf.getTitle() : "";
        md.set(Tag.Title, title);
        md.set(Tag.TitleJ, title);
        set(md, Tag.Composer, smaf.getComposer());
        set(md, Tag.ComposerJ, smaf.getComposer());
        set(md, Tag.Maker, smaf.getCopyright());
        set(md, Tag.Note, smaf.getComment());
        md.set(Tag.NumberOfSongs, "1");
        // the generation the file was made for, which is not what sounds it any more
        md.set(Tag.GameSystem, "SMAF " + smaf.getFormat().label);
        // whatever generation that is, the MA-7 is what plays it here; the driver registers no
        // chip, so this is what names the sound source on the fmdsp header
        md.set(Tag.Chip, SmafFile.Format.MA7.label);

        this.metaData = md;
        return md;
    }

    private static void set(MetaData md, Tag tag, String value) {
        if (value != null && !value.isEmpty()) {
            md.set(tag, value);
        }
    }

    /** the channels as the song has left them so far */
    public MidiChannels getChannels() {
        return channels;
    }

    /** the synthesizer playing the song, nullable */
    public SmafMa7Synthesizer getSynthesizer() {
        return synthesizer;
    }

    @Override
    public void init(EnmModel model, int latency, int waitTime, Object... args) {
        this.model = model;
        this.latency = latency;
        this.waitTime = waitTime;

        if (model == EnmModel.RealModel) {
            stopped = true;
            curLoop = 9999;
            return;
        }

        counter = 0;
        totalCounter = 0;
        loopCounter = 0;
        curLoop = 0;
        stopped = false;
        frameCounter = 0;
        speed = 1;
        speedCounter = 0;

        metaData = retrieveMetaData(dataBuf);

        int outputRate = setting.getOutputDevice().getSampleRate();
        Sequence sequence;
        // the reader wants to look at the first four bytes and put them back
        try (BufferedInputStream is = new BufferedInputStream(new ByteArrayInputStream(dataBuf))) {
            sequence = new SmafMidiFileReader().getSequence(is);
        } catch (InvalidMidiDataException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        events = MidiSchedule.of(sequence, outputRate);
        next = 0;
        position = 0;
        channels.reset();
        long last = events.isEmpty() ? 0 : events.getLast().frame();
        end = last + (long) (TAIL_SECONDS * outputRate);
        totalCounter = last;

        stopSynth();
        this.outputRate = outputRate;
        // none of the stream waves the song before stored is this song's
        AudioEngine.resetAll();
        openSynth();
        frac = 0;
        blockPos = BLOCK;
        curL = curR = prevL = prevR = 0;
    }

    /** the sound source, opened without a line of its own so that this renders it */
    private void openSynth() {
        SmafMa7Synthesizer synthesizer = new SmafMa7Synthesizer();
        AudioInputStream stream;
        Receiver receiver;
        try {
            // the streams mixed in by the synthesizer, not by this: unmixed they play to lines of
            // their own in wall clock time, which this, rendering ahead, is not in step with
            stream = synthesizer.openStream(true);
            receiver = synthesizer.getReceiver();
        } catch (MidiUnavailableException | RuntimeException e) {
            synthesizer.close();
            throw new IllegalStateException("cannot open the MA-7: " + e.getMessage(), e);
        }
        AudioFormat format = stream.getFormat();
        this.synthesizer = synthesizer;
        this.stream = stream;
        this.receiver = receiver;
        this.step = (double) format.getSampleRate() / outputRate;
        headroom(receiver);
logger.log(Level.INFO, "smaf: " + synthesizer.getDeviceInfo().getName() + ", " + format);
    }

    /**
     * The room the song needs to fit in 16 bit.
     * <p>
     * The sound source alone fills 16 bit - that is what it is for, it was the whole output of a
     * phone - and the stream waves of a song are mixed in level with it, so the sum wants scaling
     * before it is cut. The universal master volume is that scaling and the synthesizer applies it
     * to the sum, before it cuts anything, so this is real room and not a quieter distortion.
     * <p>
     * Half is what a song whose streams peak together with the sound source needs ("GuitarMan.mmf"
     * peaks at 32641 of 32767 with it); a song whose peaks fall apart has room to spare.
     */
    private void headroom(Receiver receiver) {
        int v = (int) (HEADROOM * 16383);
        try {
            SysexMessage volume = new SysexMessage();
            volume.setMessage(0xf0, new byte[] {
                    0x7f, 0x7f, 0x04, 0x01, (byte) (v & 0x7f), (byte) ((v >> 7) & 0x7f), (byte) 0xf7}, 7);
            receiver.send(volume, -1);
        } catch (InvalidMidiDataException e) {
logger.log(Level.DEBUG, "headroom: " + e);
        }
    }

    /** closes the synthesizer of the song */
    public void stopSynth() {
        if (synthesizer != null) {
            close(receiver::close);
            close(synthesizer::close);
            close(stream::close);
            synthesizer = null;
            stream = null;
            receiver = null;
        }
    }

    /** one of the three the song leaves behind, whatever the one before made of it */
    private static void close(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
logger.log(Level.DEBUG, "close: " + e);
        }
    }

    @Override
    public void processOneFrame() {
        if (model == EnmModel.RealModel) return;

        speedCounter += speed;
        while (speedCounter >= 1.0 && !stopped) {
            speedCounter -= 1.0;
            if (frameCounter > -1) {
                counter++;
            } else {
                frameCounter++;
            }
        }
    }

    /** sends what is due by the output frame about to be rendered */
    private void dispatch() {
        while (next < events.size() && events.get(next).frame() <= position) {
            MidiMessage m = events.get(next++).message();
            if (m instanceof MetaMessage) continue; // end of track
            channels.send(m);
            try {
                receiver.send(m, -1);
            } catch (RuntimeException e) {
logger.log(Level.DEBUG, "send: " + e);
            }
        }
    }

    /** the synthesizer's next frame */
    private void nextSourceFrame() {
        if (blockPos >= BLOCK) {
            int n = 0;
            try {
                while (n < pcm.length) {
                    int r = stream.read(pcm, n, pcm.length - n);
                    if (r <= 0) break;
                    n += r;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            // the stream has no end, but a closed one gives nothing: silence is the right answer
            for (int i = n; i < pcm.length; i++) {
                pcm[i] = 0;
            }
            for (int i = 0; i < BLOCK; i++) {
                left[i] = (short) ((pcm[i * 4] & 0xff) | (pcm[i * 4 + 1] << 8));
                right[i] = (short) ((pcm[i * 4 + 2] & 0xff) | (pcm[i * 4 + 3] << 8));
            }
            blockPos = 0;
        }
        curL = left[blockPos];
        curR = right[blockPos];
        blockPos++;
    }

    @Override
    public int render(short[] b, int offset, int length) {
        if (synthesizer == null) {
            return length;
        }

        for (int i = 0; i < length - 1; i += 2) {
            if (!stopped) {
                dispatch();
            }
            int l, r;
            if (step == 1.0) {
                nextSourceFrame();
                l = curL;
                r = curR;
            } else {
                frac += step;
                while (frac >= 1.0) {
                    frac -= 1.0;
                    prevL = curL;
                    prevR = curR;
                    nextSourceFrame();
                }
                l = (int) (prevL + (curL - prevL) * frac);
                r = (int) (prevR + (curR - prevR) * frac);
            }

            b[offset + i] = (short) Math.clamp(l, Short.MIN_VALUE, Short.MAX_VALUE);
            b[offset + i + 1] = (short) Math.clamp(r, Short.MIN_VALUE, Short.MAX_VALUE);

            position++;
            if (position >= end && !AudioEngineMixer.isPlaying()) {
                stopped = true;
            }

            processOneFrame();
            if (isWatched()) {
                fireEventHappened(this, "wave.buffer", (short) l, (short) r);
            }
        }
        return length;
    }

    @Override
    public String getName() {
        return "SMAF";
    }
}
