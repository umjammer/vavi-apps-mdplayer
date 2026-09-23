package mdplayer;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import mdplayer.Common.EnmModel;
import mdplayer.chips.MidiPlugin;
import mdplayer.chips.RealChipPlugin;
import mdplayer.chips.VstPlugin;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.sampled.SampledPlugin;
import vavi.util.event.GenericEvent;
import vavi.util.event.GenericListener;

import static java.lang.System.getLogger;
import static vavi.sound.SoundUtil.volume;


/**
 * virtual device player
 * <p>
 * system property
 * <li>{@code mdplayer.volume} ... player volume, default {@code 0.2}; applied to the output
 * line and, through {@code Balance.midiVolume}, to the MIDI path that never reaches that line</li>
 */
public final class Audio {

    private static final Logger logger = getLogger(Audio.class.getName());

    private final Setting setting = Setting.getInstance();

    private static final Audio instance = new Audio();

    public BasePlugin<? extends BaseDriver> plugin;

    private SourceDataLine line;

    /** while true the render loop of {@link #play()} keeps running */
    private volatile boolean rendering = false;

    /** true when the render loop of {@link #play()} has exited */
    private volatile boolean renderStopped = true;

    private Audio() {
        Thread thread = new Thread(this::processRequest, "mdplayer-request-processor");
        thread.setDaemon(true);
        thread.start();
    }

    /** singleton */
    public static Audio getInstance() {
        return instance;
    }

    /** binds a plugin */
    public void init(BasePlugin<? extends BaseDriver> plugin) {
        // a song change comes here without close(), a line left open keeps its device output
        // running for good (with the Rococoa line an AVAudioEngine that is never released)
        closeLine();

        this.plugin = plugin;

        try {
            int sampleRate = setting.getOutputDevice().getSampleRate();
            AudioFormat format = new AudioFormat(sampleRate, 16, 2, true, false);
            line = AudioSystem.getSourceDataLine(format);
logger.log(Level.DEBUG, format);
            line.addLineListener(Audio::lineListener);
            line.open(format);
            double gain = Double.parseDouble(System.getProperty("mdplayer.volume", "0.2"));
            volume(line, gain);
            attenuateMidi(gain);
            line.start();
        } catch (LineUnavailableException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Puts {@code mdplayer.volume} into the MIDI side of the mixer balance as well.
     * <p>
     * That property is a gain on this player's own {@code SourceDataLine}, and a song played
     * through {@link MidiPlugin} never passes through it: the synthesizer at the other end has its
     * own output, so a MIDI-driven song (a MIDI-only ZMS, RCP, RCS, MID) stays at full level
     * however far down the line is turned. {@code Balance.midiVolume} is the one slot that does
     * reach it -- {@code MidiPlugin} scales controller 7 by it.
     * <p>
     * Runs here rather than in the plugin because {@code setParams} replaces the balance wholesale
     * with the driver's calibrated preset, and every caller sets the params before {@link #init}.
     *
     * @param gain linear, as the property is written; {@code 1} and above change nothing
     */
    private void attenuateMidi(double gain) {
        if (gain <= 0 || gain >= 1) return;
        Setting.Balance balance = setting.getBalance();
        // setParams normally hands each song a fresh preset, but not when the auto balance is off
        // or the driver has no preset: without this the attenuation compounds song after song
        if (balance == attenuatedBalance) return;
        attenuatedBalance = balance;
        // the balance unit is 2 x dB, so a linear gain g is 40 * log10(g)
        int attenuated = Math.clamp(balance.getMidiVolume() + (int) Math.round(40 * Math.log10(gain)), -192, 20);
logger.log(Level.DEBUG, "midi volume: " + balance.getMidiVolume() + " -> " + attenuated);
        balance.setMidiVolume(attenuated);
    }

    /** the balance {@link #attenuateMidi} has already been applied to */
    private Setting.Balance attenuatedBalance;

    /** line listener */
    private static void lineListener(LineEvent e) {
logger.log(Level.DEBUG, "line: " + e.getType());
    }

    /**
     * Is {@link #play()} inside its render loop? By then it has prepared the plugin, so this is
     * what tells the view that the song has really begun — not the stopped flag, which a plugin
     * that has never played starts out with anyway.
     */
    public boolean isRendering() {
        return rendering && !renderStopped;
    }

    /**
     * Starts blocking rendering, and returns when the song has ended or was stopped.
     *
     * @return false when the song could not be set up and never played
     */
    public boolean play() {
        try {
            plugin.prepare();
        } catch (Exception e) {
            // A song that cannot be set up - an unsupported chip, a missing PCM file, a header
            // this driver rejects - must not take the caller down with it. Thrown from here the
            // exception leaves the caller waiting on a song that never sounds (the play list
            // never advances, the test's latch is never counted down): silence with no end. A
            // false return is what "this one did not play" already means to every caller.
            logger.log(Level.ERROR, "prepare: " + plugin.playingFileName, e);
            plugin.stopped = true;
            return false;
        }
//logger.log(Level.TRACE, "play: " + audio.stopped + ", " + audio.hashCode());
        listeners.forEach(l -> plugin.getDriver().addViewListener(l)); // TODO consider more

        stop();

        try { Thread.sleep(500); } catch (InterruptedException ignore) {}

        plugin.paused = false;
        plugin.stopped = false;

        // prepare() cleared the fade-out, but stop() has run since, and it fades the previous song
        // out when the line is idle. Leaving that armed would fade this song out within a few
        // samples and mark it stopped before a note of it was heard.
        plugin.fadeout = false;
        plugin.fadeoutCounter = 1.0;
        plugin.fadeoutCounterV = 0.00001;

        plugin.oneTimeReset = false;

//logger.log(Level.TRACE, "stopped: " + audio.stopped + ", " + audio.hashCode());

        if (plugin instanceof SampledPlugin sampledPlugin) {
            if (sampledPlugin.fileReader != null) {
                sampledPlugin.stopAudio();
            }
        }

        if (plugin instanceof SampledPlugin sampledPlugin) {
            sampledPlugin.fileName = plugin.playingFileName;
        }

        logger.log(Level.DEBUG, "driver: " + plugin.driverVirtual.getClass().getSimpleName());

        // after stop(): stop() closes the real chip thread and waits for it, so a thread
        // started before it would be shut down right away (it only survived the very first
        // song because driverReal was still null then and the thread exited by itself).
        plugin.chipRegister.plugin(RealChipPlugin.class).startThread();

        rendering = true;
        renderStopped = false;
        boolean started = false;
        int playRenders = 0;
        try {
            while (rendering) {
//logger.log(Level.TRACE, "loop HERE");
                short[] buffer = new short[4];

                int r;
                if (plugin instanceof SampledPlugin sampledPlugin) {
                    r = sampledPlugin.read(buffer, 0, buffer.length);
                } else {
                    r = render(buffer, 0, buffer.length);
                    if (setting.getMidiKbd().getUseMIDIKeyboard()) {
                        plugin.chipRegister.plugin(MidiPlugin.class).keyboard(buffer, 0, buffer.length);
                    }

                    if (!plugin.getStopped()) {
                        started = true;
                    }
                    if (started) {
                        playRenders++;
                    }

                    // detect the end of an emulated-chip song: once it has looped
                    // enough times or the driver reached the end of its sequence,
                    // start fading out. (The GUI drives this from its screen loop;
                    // headless callers such as tests have no such loop, so play()
                    // would otherwise render silence forever and never return.)
                    if (started && playRenders > 22050 && ((setting.getOther().getUseLoopTimes() && plugin.getCurLoopCounter() > setting.getOther().getLoopTimes() - 1)
                            || plugin.getStopped())) {
                        plugin.fadeout = true;
                    }
                }
                if (r == -1) break;

                if (!rendering) break;
                this.write(buffer, 0, buffer.length);

                // the fade-out has finished (render() marks the plugin stopped):
                // leave the loop so play() returns.
                if (plugin.stopped) break;

                Thread.yield();
            }
        } finally {
            renderStopped = true;
        }

        // it played, to the end or until it was stopped; only a song that could not start is false
        return true;
    }

    private int write(short[] buffer, int offset, int count) {
        int bytesToWrite = count * Short.BYTES;
        while (rendering) {
            if (line == null) return 0;
            int available = line.available();
            if (available >= bytesToWrite) {
                ByteBuffer bb = ByteBuffer.allocate(bytesToWrite).order(ByteOrder.LITTLE_ENDIAN);
                ShortBuffer sb = bb.asShortBuffer();
                sb.put(buffer, offset, count);
                sb.rewind();

                // Calculate peak left and right channel levels
                int maxL = 0;
                int maxR = 0;
                for (int i = 0; i < count; i += 2) {
                    int l = Math.abs(buffer[offset + i]);
                    int r = (i + 1 < count) ? Math.abs(buffer[offset + i + 1]) : l;
                    if (l > maxL) maxL = l;
                    if (r > maxR) maxR = r;
                }

                if (plugin != null) {
                    if (plugin.getDriver() != null) {
                        plugin.getDriver().fireEventHappened(this, "wave.buffer", (short) maxL, (short) maxR);
                    } else {
                        GenericEvent ev = new GenericEvent(this, "wave.buffer", (short) maxL, (short) maxR);
                        listeners.forEach(l -> l.eventHappened(ev));
                    }
                }

                return line.write(bb.array(), 0, bytesToWrite);
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }
        return 0;
    }

    /** */
    public void stop() {
        logger.log(Level.TRACE, "stop enter");
        if (plugin == null) return;
        try {
            if (plugin.paused) pause();

            if (plugin.stopped) {
                plugin.chipRegister.plugin(RealChipPlugin.class).closeThread();

                if (plugin instanceof SampledPlugin sampledPlugin) {
                    if (sampledPlugin.fileReader != null) {
                        sampledPlugin.stopAudio();
                    }
                }

                return;
            }

            if (!plugin.paused) {
                if (line == null || !line.isRunning()) {
                    plugin.fadeoutCounterV = 0.1;
                    plugin.fadeout = true;
                    int cnt = 0;
                    while (!plugin.stopped && cnt < 100) {
                        try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                        cnt++;
                    }
                }
            }
            plugin.stop();
            plugin.chipRegister.plugin(RealChipPlugin.class).setThreadClosed(true);

            if (plugin instanceof SampledPlugin sampledPlugin) {
                if (sampledPlugin.fileReader != null) {
                    return;
                }
            }

            try {
                plugin.chipRegister.softReset(EnmModel.VirtualModel);
                plugin.chipRegister.softReset(EnmModel.RealModel);
            } catch (Exception e) {
                logger.log(Level.ERROR, e.toString()); // usually chip 1 is null
            }

            int timeout = 5000;
            while (!plugin.chipRegister.plugin(RealChipPlugin.class).isThreadStopped()) {
                try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                timeout--;
                if (timeout < 1) break;
            }
            while (!plugin.stopped) {
                try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                timeout--;
                if (timeout < 1) break;
            }
            plugin.stopped = true;
logger.log(Level.INFO, "stop: " + plugin.stopped + ", " + hashCode());

            try {
                plugin.chipRegister.softReset(EnmModel.VirtualModel);
                plugin.chipRegister.softReset(EnmModel.RealModel);
            } catch (Exception e) {
                logger.log(Level.ERROR, e.toString()); // usually chip 1 is null
            }
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * Stops the render loop of play(), waits for it to exit, and closes the line, so the
     * previous track's thread can never render into the line reopened by the next init()
     * (shared singleton line/plugin fields).
     */
    private void closeLine() {
        rendering = false;
        int timeout = 1000;
        while (!renderStopped && timeout-- > 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignore) {
            }
        }
        if (line != null) {
            try {
                line.close();
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
            line = null;
        }
    }

    /** */
    public void close() {
        logger.log(Level.INFO, "close enter");

        closeLine();
        if (plugin != null) {
            plugin.close();
            try {
                plugin.chipRegister.plugin(MidiPlugin.class).midiClose();
                plugin.chipRegister.plugin(RealChipPlugin.class).close();
            } catch (Exception ex) {
                logger.log(Level.ERROR, ex.getMessage(), ex);
            }
        }
    }

    /** */
    public void stepPlay(int step) {
        plugin.stepCounter = step;
    }

    /** */
    private int render(short[] buffer, int offset, int sampleCount) {
        if (buffer == null || buffer.length < 1 || sampleCount == 0) return 0;
        if (plugin.driverVirtual == null) return sampleCount;

        try {
            //stwh.reset(); stwh.Start();

//logger.log(Level.TRACE, "stop: " + stopped + ", " + hashCode());
            if (plugin.stopped || plugin.paused) {
                if (plugin.driverVirtual.isNotRenderingOnPause()) {
                    for (int d = offset; d < offset + sampleCount; d++) buffer[d] = 0;
                    return sampleCount;
                } else {
                    int ret = plugin.mds.update(buffer, offset, sampleCount, null);
                    return ret;
                }
            }

            int cnt = plugin.driverVirtual.render(buffer, offset, sampleCount);
//logger.log(Level.TRACE, "sampleCount: " + sampleCount);

            // VST
            plugin.chipRegister.plugin(VstPlugin.class).update(buffer, offset, sampleCount);

            for (int i = 0; i < sampleCount; i++) {
                int mul = (int) (16384.0 * Math.pow(10.0, plugin.masterVolume / 40.0));
                buffer[offset + i] = (short) Math.clamp((buffer[offset + i] * mul) >> 13, -0x8000, 0x7fff);

                if (!plugin.fadeout) continue;

                // Fade-out Processing
                buffer[offset + i] = (short) (buffer[offset + i] * plugin.fadeoutCounter);

                plugin.fadeoutCounter -= plugin.fadeoutCounterV;
                if (plugin.fadeoutCounterV >= 0.004 && plugin.fadeoutCounterV != 0.1) {
                    plugin.fadeoutCounterV = 0.004;
                }

                if (plugin.fadeoutCounter < 0.0) {
                    plugin.fadeoutCounter = 0.0;
                }

                // After the fade out is complete, the music stops playing completely.
                if (plugin.fadeoutCounter == 0.0) {
                    plugin.chipRegister.softReset(EnmModel.VirtualModel);
                    plugin.chipRegister.softReset(EnmModel.RealModel);

                    plugin.chipRegister.close();

                    plugin.stopped = true;
logger.log(Level.DEBUG, "stop: " + plugin.stopped);

                    return i + 1;
                }
            }

            if (setting.getOutputDevice().getDeviceType() != Common.DEV_Null) {
                updateVisualVolume(buffer, offset);
            }

            return cnt;

        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            plugin.stopped = true;
        }

        return -1;
    }

    /** */
    private void updateVisualVolume(short[] buffer, int offset) {
        if (plugin.getDriver() != null) {
            plugin.getDriver().fireEventHappened(this, "master", buffer, offset);
        }

        for (var i : plugin.mds.getFirstInstruments()) {
            var vs = i.getView(0, "volume");
        }
    }

    /** */
    public void fadeout() {
        if (isPaused()) {
            pause();
        }

        plugin.fadeout = true;
    }

    /** */
    public void pause() {
        plugin.paused = !plugin.paused;
    }

    /** nothing loaded yet means nothing is paused */
    public boolean isPaused() {
        return plugin != null && plugin.paused;
    }

    public void seek(double n) {
        if (plugin instanceof SampledPlugin sampledPlugin) {
            try {
                if (sampledPlugin.fileReader != null) {
                    long totalBytes = sampledPlugin.fileReader.getFrameLength() * sampledPlugin.fileReader.getFormat().getFrameSize();
                    long targetPos = (long) (totalBytes * n);
                    sampledPlugin.fileReader.close();
                    sampledPlugin.fileReader = AudioSystem.getAudioInputStream(Path.of(sampledPlugin.fileName).toFile());
                    long skipped = 0;
                    while (skipped < targetPos) {
                        long s = sampledPlugin.fileReader.skip(targetPos - skipped);
                        if (s <= 0) break;
                        skipped += s;
                    }
                }
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }
    }

    // for gui
    public void processRequest() {
        while (true) {
            Request req = OpeManager.getRequestToAudio();
            if (req == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
                continue;
            }

            switch (req.request) {
                case Die: // Please kill yourself
                    close();
                    req.setEnd(true);
                    break;
                case Stop:
                    stop();
                    req.setEnd(true);
                    OpeManager.completeRequestToAudio(req);
                    break;
            }
        }
    }

    private boolean emuOnly;

    public boolean isEmuOnly() {
        return emuOnly;
    }

    /**
     * add to plugin before start playing
     * TODO consider more
     */
    private final List<GenericListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * view listeners (such as visualizer)
     * TODO consider more
     */
    public void addGenericListener(GenericListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
            if (plugin != null && plugin.getDriver() != null) {
                plugin.getDriver().addViewListener(l);
            }
        }
    }

    public void removeGenericListener(GenericListener l) {
        listeners.remove(l);
        if (plugin != null && plugin.getDriver() != null) {
            plugin.getDriver().removeViewListener(l);
        }
    }
}
