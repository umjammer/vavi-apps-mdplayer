/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mfi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import mdplayer.Common.EnmModel;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;
import vavi.sound.mfi.MfiChip;
import vavi.sound.mfi.MfiChip.Condition;
import vavi.sound.midi.mfi.MfiMidiFileReader;
import vavi.sound.mobile.AudioEngineMixer;

import static java.lang.System.getLogger;


/**
 * MFi (".mld", i-mode ringtone) driver.
 * <p>
 * The song is read by vavi-sound into a midi sequence and played here, against the samples
 * rendered, on a synthesizer of vavi-apps-mfiplayer picked for the sound chip of the phone the
 * file was made for ({@link MfiChip}, {@link MldSynth}). Nothing goes through
 * {@link mdplayer.chips.MidiPlugin}: the midi out of the other midi drivers is the listener's,
 * and an mfi only sounds right on its own sound source. Like the smaf and sid drivers this
 * overrides {@link #render} instead of feeding a chip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
public class MldDriver extends BaseDriver {

    private static final Logger logger = getLogger(MldDriver.class.getName());

    /** how long a song rings on after its last event [s] */
    private static final double TAIL_SECONDS = 2;

    /** the frames the synthesizer renders at a time, a message waits at most this long */
    private static final int BLOCK = 32;

    /** a message and the output frame it is due at */
    record Event(long frame, MidiMessage message) {}

    private MfiChip.Detection detection;

    /** what the messages sent have left the channels at, for the visualizer */
    private final MldChannels channels = new MldChannels();
    private MldSynth synth;
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
    private int[] left = new int[BLOCK], right = new int[BLOCK];
    private int blockPos = BLOCK, blockLen = BLOCK;

    /**
     * whether the adpcm of vavi-sound's engines is mixed in here, rather than played to a line
     * of their own beside the song, see {@link AudioEngineMixer#attach()}
     */
    private boolean mixing;
    private int outputRate;
    /** the frames of the buffer being rendered the adpcm has been mixed into */
    private int mixedFrames;
    private int curL, curR, prevL, prevR;

    public MldDriver(BasePlugin<? extends BaseDriver> plugin) {
        super(plugin);
    }

    public MldDriver() {
        this(null); // gross
    }

    @Override
    public MetaData retrieveMetaData(byte[] buf, Object... args) {
        if (!MldFile.isMfi(buf)) return null;
        MldFile file;
        try {
            file = MldFile.decode(buf);
        } catch (RuntimeException e) {
logger.log(Level.DEBUG, "not an mfi: " + e);
            return null;
        }
        MfiChip.Detection d = MfiChip.detect(condition(file));

        MetaData md = new MetaData();
        String title = file.getTitle() != null ? file.getTitle() : "";
        md.set(Tag.Title, title);
        md.set(Tag.TitleJ, title);
        set(md, Tag.Maker, file.getCopyright());
        set(md, Tag.Converter, file.getSupport());
        set(md, Tag.Note, file.getProtector());
        set(md, Tag.ReleaseDate, file.getDate());
        md.set(Tag.NumberOfSongs, "1");
        // no mdsound chip is registered, this is what names it on the fmdsp header
        md.set(Tag.Chip, d.name());

        this.metaData = md;
        return md;
    }

    static Condition condition(MldFile file) {
        return new Condition(file.getAudioFormats(), file.getSupport(), file.getVendorCarriers(), file.getVersion(), file.getMajorVersion());
    }

    private static void set(MetaData md, Tag tag, String value) {
        if (value != null && !value.isEmpty()) {
            md.set(tag, value);
        }
    }

    /** the chip found out for the song, nullable before {@link #init} */
    public MfiChip.Detection getDetection() {
        return detection;
    }

    /** the channels as the song has left them so far */
    public MldChannels getChannels() {
        return channels;
    }

    /** the synthesizer playing the song, nullable */
    public MldSynth getSynth() {
        return synth;
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

        MldFile file = MldFile.decode(dataBuf);
        detection = MfiChip.detect(condition(file));

        int outputRate = setting.getOutputDevice().getSampleRate();
        Sequence sequence;
        try {
            sequence = new MfiMidiFileReader().getSequence(new ByteArrayInputStream(dataBuf));
        } catch (InvalidMidiDataException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        events = schedule(sequence, outputRate);
        next = 0;
        position = 0;
        channels.reset();
        long last = events.isEmpty() ? 0 : events.getLast().frame;
        end = last + (long) (TAIL_SECONDS * outputRate);
        totalCounter = last;

        stopSynth();
        this.outputRate = outputRate;
        // the adpcm (and the UCS waves) the song starts, mixed into what is rendered here
        mixing = AudioEngineMixer.attach();
        synth = MldSynth.forChip(detection.chip());
        receiver = synth.getReceiver();
        step = (double) synth.getSampleRate() / outputRate;
        frac = 0;
        blockPos = blockLen = BLOCK;
        curL = curR = prevL = prevR = 0;
logger.log(Level.INFO, "mfi: " + detection + " → " + synth.getDescription());
    }

    /**
     * The messages of the sequence at the output frames they are due, the tempo changes
     * followed.
     */
    static List<Event> schedule(Sequence sequence, int outputRate) {
        if (sequence.getDivisionType() != Sequence.PPQ) {
            throw new IllegalArgumentException("division: " + sequence.getDivisionType());
        }
        List<MidiEvent> all = new ArrayList<>();
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                all.add(track.get(i));
            }
        }
        // stable: the order within a tick is the order in the file
        all.sort(Comparator.comparingLong(MidiEvent::getTick));

        int resolution = sequence.getResolution();
        List<Event> events = new ArrayList<>(all.size());
        long tempoTick = 0;
        double tempoMicros = 0;
        double microsPerTick = 500_000d / resolution;
        for (MidiEvent e : all) {
            double micros = tempoMicros + (e.getTick() - tempoTick) * microsPerTick;
            long frame = Math.round(micros * outputRate / 1_000_000d);
            MidiMessage m = e.getMessage();
            if (m instanceof MetaMessage meta) {
                if (meta.getType() == 0x51 && meta.getData().length >= 3) {
                    byte[] d = meta.getData();
                    int mpq = ((d[0] & 0xff) << 16) | ((d[1] & 0xff) << 8) | (d[2] & 0xff);
                    tempoTick = e.getTick();
                    tempoMicros = micros;
                    microsPerTick = (double) mpq / resolution;
                }
                if (meta.getType() == 0x2f) {
                    events.add(new Event(frame, m));
                }
                continue; // a synthesizer has nothing to do with the others
            }
            events.add(new Event(frame, m));
        }
        return events;
    }

    /** closes the synthesizer of the song */
    public void stopSynth() {
        if (mixing) {
            mixing = false;
            AudioEngineMixer.detach();
        }
        if (synth != null) {
            try {
                synth.close();
            } catch (RuntimeException e) {
logger.log(Level.DEBUG, "close: " + e);
            }
            synth = null;
            receiver = null;
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
        while (next < events.size() && events.get(next).frame <= position) {
            MidiMessage m = events.get(next++).message;
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
        if (blockPos >= blockLen) {
            synth.render(left, right, BLOCK);
            blockPos = 0;
            blockLen = BLOCK;
        }
        curL = left[blockPos];
        curR = right[blockPos];
        blockPos++;
    }

    @Override
    public int render(short[] b, int offset, int length) {
        if (synth == null) {
            return length;
        }

        mixedFrames = 0;
        for (int i = 0; i < length - 1; i += 2) {
            if (!stopped) {
                if (mixing && next < events.size() && events.get(next).frame <= position) {
                    // what sounds before a message is mixed before the message is sent: an
                    // adpcm it starts starts on this frame
                    mixAdpcm(b, offset, i / 2);
                }
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
            if (position >= end && !(mixing && AudioEngineMixer.isPlaying())) {
                stopped = true;
            }

            processOneFrame();
            if (isWatched()) {
                fireEventHappened(this, "wave.buffer", (short) l, (short) r);
            }
        }
        if (mixing) {
            mixAdpcm(b, offset, length / 2);
        }

        return length;
    }

    /** mixes the adpcm into the frames rendered since it was last, up to {@code frames} */
    private void mixAdpcm(short[] b, int offset, int frames) {
        if (frames > mixedFrames) {
            AudioEngineMixer.render(b, offset + mixedFrames * 2, frames - mixedFrames, outputRate);
            mixedFrames = frames;
        }
    }
}
