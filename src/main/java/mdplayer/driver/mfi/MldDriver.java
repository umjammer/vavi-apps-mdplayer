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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;

import mdplayer.Common.EnmModel;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.MasterVolumeSub;
import mdplayer.driver.MidiChannels;
import mdplayer.driver.MidiSchedule;
import mdplayer.driver.MidiSchedule.Event;
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
public class MldDriver extends BaseDriver implements MasterVolumeSub {

    /**
     * How loud the stream waves are against the song. They come at the level they were stored at
     * ({@link AudioEngineMixer}), so the level is this driver's to choose, and what it chooses is
     * what the volume of a line of their own used to make of them - the same property and the same
     * default - so that nothing sounds different here and a setting of it still works. It is
     * scaled by the synthesizer's {@link #getMasterVolumeSubType() sub master volume} too, so a song keeps its
     * balance of waves to notes on every synthesizer.
     */
    private static final double ADPCM_GAIN =
            Double.parseDouble(System.getProperty("vavi.sound.mobile.AudioEngine.volume", "0.2"));

    private static final Logger logger = getLogger(MldDriver.class.getName());

    /** how long a song rings on after its last event [s] */
    private static final double TAIL_SECONDS = 2;

    /** the frames the synthesizer renders at a time, a message waits at most this long */
    private static final int BLOCK = 32;

    private MfiChip.Detection detection;

    /** what the messages sent have left the channels at, for the visualizer */
    private final MidiChannels channels = new MidiChannels();
    private MldSynth synth;
    private Receiver receiver;
    /** the preset's {@code <MasterVolumeSub>} of the synthesizer playing, see {@link #getMasterVolumeSubType()} */
    private double gain = 1.0;

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
    public MidiChannels getChannels() {
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
        events = MidiSchedule.of(sequence, outputRate);
        next = 0;
        position = 0;
        channels.reset();
        long last = events.isEmpty() ? 0 : events.getLast().frame();
        end = last + (long) (TAIL_SECONDS * outputRate);
        totalCounter = last;

        stopSynth();
        this.outputRate = outputRate;
        // the adpcm (and the UCS waves) the song starts, mixed into what is rendered here
        mixing = AudioEngineMixer.attach();
        synth = MldSynth.forChip(detection.chip());
        receiver = synth.getReceiver();
        // the synthesizers are ~17 dB apart for the same songs: more than the driver's one
        // MasterVolume can level, and applied after the clamp below it could only make it worse
        gain = Math.pow(10.0, setting.getBalance().getMasterVolumeSub(getMasterVolumeSubType()) / 40.0);
        step = (double) synth.getSampleRate() / outputRate;
        frac = 0;
        blockPos = blockLen = BLOCK;
        curL = curR = prevL = prevR = 0;
logger.log(Level.INFO, "mfi: " + detection + " → " + synth.getDescription());
    }

    /** {@code "yamaha:ma7"}: the chip, and the {@link MldSynth#getName() synthesizer} playing it */
    @Override
    public String getMasterVolumeSubType() {
        if (detection == null || synth == null) return null;
        return group(detection.chip()) + ":" + synth.getName();
    }

    /** the synthesizers available that sound as the chip of the song */
    @Override
    public List<String> getMasterVolumeSubTypes() {
        if (detection == null) return List.of();
        String current = getMasterVolumeSubType();
        List<String> types = new ArrayList<>();
        if (current != null) types.add(current);
        for (MldSynth s : MldSynth.providers()) {
            if (!s.getChips().contains(detection.chip()) || !s.isAvailable()) continue;
            String type = group(detection.chip()) + ":" + s.getName();
            if (!types.contains(type)) types.add(type);
        }
        return types;
    }

    /** {@code mdplayer.mfi.synth.<chip>=<synthesizer>} */
    @Override
    public Map<String, String> masterVolumeSubProperties(String type) {
        String[] gv = type.split(":", 2);
        return Map.of(MldSynth.SYNTH_KEY + "." + gv[0], gv[1]);
    }

    private static String group(MfiChip chip) {
        return chip.name().toLowerCase(Locale.ROOT);
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
                if (mixing && next < events.size() && events.get(next).frame() <= position) {
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

            // leveled before the clamp: the loud synthesizers reach full scale as they are
            l = (int) (l * gain);
            r = (int) (r * gain);
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
            AudioEngineMixer.render(b, offset + mixedFrames * 2, frames - mixedFrames, outputRate, ADPCM_GAIN * gain);
            mixedFrames = frames;
        }
    }
}
