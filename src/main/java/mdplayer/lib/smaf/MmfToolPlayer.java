/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.lib.smaf;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import jdos.api.AudioSink;
import jdos.api.JDosBox;
import mdplayer.lib.PcmQueue;

import static java.lang.System.getLogger;


/**
 * Plays a SMAF file by running mmftoolc.exe - a real MA-2/3/5 player, built for Windows - on an
 * emulated PC, and taking what it writes to its {@code waveOut} device.
 * <p>
 * <b>What paces it.</b> The emulated player keeps time the way it would on real hardware: it
 * hands the sound card a buffer, and cannot hand it the next one until the card has played the
 * last. So the emulated card has to take its samples at the rate they are meant to be heard at,
 * no faster - which is exactly what happens when {@link #write} blocks until {@link #read} has
 * made room. Let it take them as fast as the emulator can make them and the player's own sense
 * of time goes with it: the song comes out at the wrong speed and ends in the wrong place.
 * <p>
 * The queue between the two is what makes that safe to rely on. The emulated player fills it
 * during the seconds it spends loading its instrument tables and then stays that far ahead, so a
 * moment where the emulation cannot keep up is taken out of the cushion rather than heard.
 * <p>
 * Those loading seconds are digital silence, dropped rather than queued - jdosbox drops them at
 * the emulated device now, and this drops whatever still gets through - so they cost nothing to
 * sit through. What is queued is real audio, which is the point: the cushion has to be built out
 * of the song, not out of the silence in front of it.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-08-10 nsano initial version <br>
 */
public class MmfToolPlayer {

    private static final Logger logger = getLogger(MmfToolPlayer.class.getName());

    /** where mmftoolc.exe and its M5_Emu*.dll live */
    public static final String MMFTOOL_PATH_KEY = "mdplayer.smaf.mmftool";

    /** how much audio the queue can hold, which only has to be more than the cushion wants */
    private static final double QUEUE_SECONDS = 20;

    /** how long the emulated player is given to produce a sound before we give up on it */
    private static final long SILENCE_LIMIT_SECONDS = 120;

    /**
     * How much audio has to be in hand before the first sample is handed over.
     * <p>
     * A song is not evenly hard to synthesize - a heavy passage half way through will drain a
     * cushion that the opening bars said was plenty - so the player starts a few seconds behind
     * the emulator rather than level with it. This costs nothing in total, because the emulator
     * is what everything is waiting for either way; it only decides whether the waiting is
     * silence before the music or stuttering during it.
     * <p>
     * Three seconds is enough for every song measured here, at the rates below. It is not enough
     * for a heavy song asked for at a higher rate - see {@link #DEEP_CUSHION_SECONDS}.
     */
    private static final double CUSHION_SECONDS =
            Double.parseDouble(System.getProperty("mdplayer.smaf.prime", "3"));

    /**
     * What a heavy song needs instead when it is synthesized above {@link #COMFORTABLE_RATE},
     * where the emulated PC runs it at about 0.95 of real time and it is therefore behind for
     * its whole length: the entire shortfall has to be in hand before it starts. Measured on the
     * longest one here - seven seconds was enough on a quiet host and not on a loaded one.
     * <p>
     * This is not a setting because it is not a choice: it is the price of {@code rate.ma5}, and
     * it follows it automatically.
     */
    private static final double DEEP_CUSHION_SECONDS = 10;

    /** the fastest a heavy song can be synthesized and still be comfortably ahead of real time */
    private static final int COMFORTABLE_RATE = 32000;

    /**
     * How fast the emulated PC is. The emulated player has to synthesize its audio faster than
     * it is heard or the cushion never fills, and "max" is what DOSBox calls giving it as much
     * of the host as it will take.
     */
    private static final String CYCLES = "max";

    /**
     * The emulated player's own master volume, 0 to 127, which it reads from its environment.
     * Its top setting clips on loud songs, so this leaves a little room.
     */
    private static final String VOLUME = System.getProperty("mdplayer.smaf.volume", "100");

    /**
     * The rate the emulated player synthesizes at, which it also reads from its environment.
     * <p>
     * This is the one thing that changes how much work the emulated PC has to do, because the
     * cost is proportional: an MA-5 song that is synthesized at 0.95 of real time at 48000 is
     * synthesized at 1.43 of it at 32000. Only 22050, 32000, 44100 and 48000 do anything -
     * mmftool refuses the rest and says so rather than playing silence - and the driver
     * resamples whatever comes out to the output device's rate anyway.
     * <p>
     * MA-1/2/3 are left at full rate because they are comfortable there and there is nothing to
     * buy. MA-5 and MA-7 are not, and 32000 is what makes them comfortable; it costs them
     * everything above 16kHz. Set {@code mdplayer.smaf.rate.ma5} to 48000 to have that back and
     * pay for it in cushion (see {@link #DEEP_CUSHION_SECONDS}).
     */
    private static final String RATE = "48000";

    /** the same for MA-5 and MA-7, the ones the emulated PC has to work hardest at */
    private static final String RATE_HEAVY = System.getProperty("mdplayer.smaf.rate.ma5", "32000");

    private final byte[] mmf;

    /** which chip the song is for, worked out once when the cushion is first sized */
    private SmafFile.Format format;

    private Path work;

    private JDosBox dosbox;

    private PcmQueue queue;

    private volatile int sampleRate = 44100;
    private volatile int channels = 2;
    private volatile int sampleSizeInBits = 16;

    /** set once the emulated player has produced a sample that is not silence */
    private volatile boolean sounding;

    /** set when the emulated player was given long enough to make a sound and never did */
    private volatile boolean gaveUp;

    /** set once the cushion has been built and the song may start */
    private volatile boolean primed;

    /** every frame the emulated player has produced, silence and all */
    private volatile long producedFrames;

    /** how much digital silence has been dropped waiting for {@link #sounding} */
    private long droppedBytes;

    /** what the emulated player says it is playing, and where it has got to */
    private final SmafTelemetry telemetry = new SmafTelemetry();

    /** frames handed to the caller, which is the clock the display is shown against */
    private volatile long consumedFrames;

    public MmfToolPlayer(byte[] mmf) {
        this.mmf = mmf;
    }

    /** where mmftoolc.exe is, as the system property says */
    public static File toolDirectory() {
        return new File(System.getProperty(MMFTOOL_PATH_KEY, "/usr/local/src/mmftool"));
    }

    /** what the emulated player needs on its drive: itself, its dlls and its voice table */
    private static boolean isToolFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return Files.isRegularFile(path)
                && (name.equals("mmftoolc.exe") || name.endsWith(".dll") || name.endsWith(".inf") || name.endsWith(".vm3"));
    }

    /** is there an mmftool to run? */
    public static boolean isAvailable() {
        return new File(toolDirectory(), "mmftoolc.exe").exists();
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    /** has the emulated player made a sound yet? */
    public boolean isSounding() {
        return sounding;
    }


    /**
     * Whether this song is for one of the chips the emulated PC struggles to keep up with. A
     * file that will not decode counts as one, since being careful about a light song costs
     * only a little and being careless about a heavy one is what stutters.
     */
    private boolean isHeavy() {
        if (format == null) {
            try {
                format = SmafFile.decode(mmf).getFormat();
            } catch (Exception e) {
                logger.log(Level.DEBUG, "smaf: cannot tell which chip this is, assuming MA-5", e);
                format = SmafFile.Format.UNKNOWN;
            }
        }
        return switch (format) {
            case MA1, MA2, MA3, UTA2, UTA3 -> false;
            case MA5, MA7, UNKNOWN -> true;
        };
    }

    /**
     * How much audio has to be in hand before the song starts.
     * <p>
     * This used to be worked out from the rate the emulator was seen to produce at while the
     * player loaded. It does not work and the measurement is kept nowhere: taken before anything
     * is consuming, so the emulator has the host to itself, it reported 1.52 for a song that
     * then ran at 1.05 and 1.28 for one that then ran at 0.95 - it did not even rank them.
     */
    private int primeBytes() {
        double seconds = Math.min(cushionSeconds(), QUEUE_SECONDS * 0.9);
        return (int) (seconds * sampleRate * channels * (sampleSizeInBits / 8));
    }

    /** the cushion this song needs, which is a question about its chip and its rate */
    private double cushionSeconds() {
        int rate;
        try {
            rate = Integer.parseInt((isHeavy() ? RATE_HEAVY : RATE).trim());
        } catch (NumberFormatException e) {
            // mmftool will refuse it and use its own default, which is the full rate
            rate = 48000;
        }
        return isHeavy() && rate > COMFORTABLE_RATE ? DEEP_CUSHION_SECONDS : CUSHION_SECONDS;
    }

    /** how much audio is queued ahead of the listener - the cushion, in seconds */
    public double getCushionSeconds() {
        if (queue == null) {
            return 0;
        }
        return queue.available() / (double) (sampleRate * channels * (sampleSizeInBits / 8));
    }

    /** what the emulated player has been up to, for a log line or a test */
    public String getStatistics() {
        int frameSize = channels * (sampleSizeInBits / 8);
        return "%.1fs produced, %.1fs of it the silence it starts with".formatted(
                producedFrames / (double) sampleRate,
                droppedBytes / (double) (sampleRate * frameSize));
    }

    /** Boots the machine and returns; the audio turns up at {@link #read} in its own time. */
    public void start() throws IOException {
        if (!isAvailable()) {
            throw new IOException("no mmftoolc.exe under " + toolDirectory()
                    + "; set -D" + MMFTOOL_PATH_KEY + "=<dir>");
        }

        // the win32 layer only ever knows one drive (see Win#run, which mounts C and nothing
        // else), so the tool and the song have to sit on it together; the tool's own directory
        // is copied rather than mounted so that nothing this writes lands in it
        work = Files.createTempDirectory("mdplayer-smaf");
        for (Path f : Files.newDirectoryStream(toolDirectory().toPath(), MmfToolPlayer::isToolFile)) {
            Files.copy(f, work.resolve(f.getFileName().toString()));
        }
        Files.write(work.resolve("song.mmf"), mmf);

        queue = new PcmQueue((int) (48000 * 2 * 2 * QUEUE_SECONDS));

        dosbox = new JDosBox()
                .mount('c', work.toFile())
                .command("c:")
                .command("set MMFTOOL_MASTER_VOLUME=" + VOLUME)
                .command("set MMFTOOL_SAMPLE_RATE=" + (isHeavy() ? RATE_HEAVY : RATE))
                // have it say what it is playing, for the visualizer - see SmafTelemetry
                .command("set MMFTOOL_TELEMETRY=1")
                .command("mmftoolc.exe song.mmf")
                // nothing on the machine's own mixer is wanted, and not opening a line for it
                // keeps it from fighting the player for the host's audio device
                .set("mixer", "nosound", "true")
                // the emulated PC is barely fast enough to synthesize MA-5 in real time, so it
                // is not going to spend any of itself emulating sound cards nothing will use
                .set("sblaster", "sbtype", "none")
                .set("gus", "gus", "false")
                .set("speaker", "pcspeaker", "false")
                .set("speaker", "tandy", "off")
                .set("speaker", "disney", "false")
                // no midi out either: "default" opens MidiSystem.getSynthesizer() for every machine, which
                // is whatever synthesizer SPI is on the class path (SiON refuses a second one, the MFi
                // ones boot a machine of their own)
                .set("midi", "mpu401", "none")
                .set("midi", "mididevice", "none")
                .set("joystick", "joysticktype", "none")
                .set("cpu", "cycles", CYCLES)
                // dosbox's "max" holds the emulated cpu to about 90% of what the host will give
                // it, and sleeps a millisecond whenever it finds itself ahead - the right thing
                // for a game, a 10% brake on a machine whose only job is to synthesize audio
                // faster than it is heard. Turbo takes that off. It does not make the machine
                // run away: `PcmQueue.write` blocking is what paces it, and that still holds.
                .turbo(true)
                // there is no screen to draw, so this is emulation nobody is going to look at
                .set("render", "frameskip", "10")
                .exitWhenProgramFinishes(true)
                .waveOutSink(new Sink())
                .stdioSink(telemetry);
        dosbox.start();
    }

    /** what the emulated player writes to its waveOut device */
    private class Sink implements AudioSink {

        @Override
        public void open(int sampleRate, int sampleSizeInBits, int channels) {
            MmfToolPlayer.this.sampleRate = sampleRate;
            MmfToolPlayer.this.sampleSizeInBits = sampleSizeInBits;
            MmfToolPlayer.this.channels = channels;
            telemetry.setSampleRate(sampleRate);
logger.log(Level.DEBUG, "smaf: waveOut " + sampleRate + "Hz " + sampleSizeInBits + "bit " + channels + "ch");
        }

        @Override
        public void write(byte[] data, int offset, int length) {
            producedFrames += length / (channels * (sampleSizeInBits / 8));

            if (!sounding) {
                int start = firstNonZero(data, offset, length);
                if (start < 0) {
                    // dropped, not queued: silence would otherwise fill the cushion that is
                    // meant to carry the start of the song. jdosbox drops it first these days,
                    // so little reaches here - but a sink is not promised trimmed audio, and
                    // this is also what notices a player that never sounds at all
                    droppedBytes += length;
                    // the guest stamps what it prints with what it has produced, which is a
                    // count these never entered; see SmafTelemetry#setDroppedFrames
                    telemetry.setDroppedFrames(droppedBytes / (channels * (sampleSizeInBits / 8)));
                    if (droppedBytes > (long) sampleRate * channels * (sampleSizeInBits / 8) * SILENCE_LIMIT_SECONDS) {
logger.log(Level.WARNING, "smaf: nothing but silence after " + SILENCE_LIMIT_SECONDS + "s, giving up");
                        gaveUp = true;
                        queue.finish();
                    }
                    return;
                }
                // align to a frame so the channels do not swap
                int frame = channels * (sampleSizeInBits / 8);
                start -= (start - offset) % frame;
                sounding = true;
logger.log(Level.DEBUG, "smaf: sound starts, " + droppedBytes + " bytes of silence dropped");
                length -= start - offset;
                offset = start;
            }

            // blocking here is the point: it is the emulated sound card taking its samples at
            // the rate they are heard, which is what holds the player to the song's own tempo
            queue.write(data, offset, length);
        }

        @Override
        public void close() {
            // just this waveOut device closing - the player opens one to warm up and another to
            // play, so this is not the end of the song. What ends it is the machine finishing.
logger.log(Level.DEBUG, "smaf: waveOut closed");
        }
    }

    private static int firstNonZero(byte[] b, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            if (b[i] != 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Takes the next samples the emulated player has made.
     *
     * @param timeoutMillis how long to wait for them before giving up and returning short
     * @return bytes read; 0 means either the song is over ({@link #isFinished}) or the emulator
     *         did not keep up
     */
    public int read(byte[] b, int offset, int length, long timeoutMillis) {
        if (queue == null) {
            return 0;
        }
        if (!primed) {
            // nothing is handed over until the cushion is built, or until it becomes clear that
            // there will never be that much - a song shorter than the cushion, or a player that
            // has finished or failed
            if (!queue.awaitAtLeast(primeBytes(), timeoutMillis)
                    && !queue.isDrained() && (dosbox == null || dosbox.isRunning())) {
                return 0;
            }
            primed = true;
logger.log(Level.DEBUG, "smaf: primed with %.1fs".formatted(getCushionSeconds()));
        }
        int n = queue.read(b, offset, length, timeoutMillis);
        consumedFrames += n / (channels * (sampleSizeInBits / 8));
        if (n == 0 && dosbox != null && !dosbox.isRunning()) {
            queue.finish();
        }
        return n;
    }

    /** what the emulated player has said about the song, for the visualizer */
    public SmafTelemetry getTelemetry() {
        return telemetry;
    }

    /** frames handed over so far, which is what the listener has heard bar the output buffer */
    public long getConsumedFrames() {
        return consumedFrames;
    }

    /**
     * Where in the song the frame {@code framesBack} behind the last one handed over falls [ms],
     * or {@code NaN} while the player has not said enough to tell. The step back is for a caller
     * that has taken more than it has used.
     */
    public double getSongMillis(long framesBack) {
        return telemetry.songMillis(consumedFrames - framesBack);
    }

    /** is the song over - the machine gone and the queue dry, or never a sound out of it? */
    public boolean isFinished() {
        return dosbox == null || gaveUp || (!dosbox.isRunning() && (queue == null || queue.available() == 0));
    }

    /** Shuts the machine down and takes the song's scratch directory with it. */
    public void stop() {
logger.log(Level.DEBUG, "smaf: " + getStatistics());
        if (queue != null) {
            queue.close();
        }
        if (dosbox != null) {
            dosbox.stop();
            dosbox = null;
        }
        if (work != null) {
            try (Stream<Path> walk = Files.walk(work)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
logger.log(Level.DEBUG, "leaving " + p + ": " + e.getMessage());
                    }
                });
            } catch (IOException e) {
logger.log(Level.DEBUG, "leaving " + work + ": " + e.getMessage());
            }
            work = null;
        }
    }
}
