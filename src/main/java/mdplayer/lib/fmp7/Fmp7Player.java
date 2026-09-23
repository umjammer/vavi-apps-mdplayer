/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.lib.fmp7;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import jdos.api.AudioSink;
import jdos.api.JDosBox;
import jdos.win.api.SharedMemory;
import mdplayer.lib.PcmQueue;

import static java.lang.System.getLogger;


/**
 * Plays an FMP7 song by running FMP7.exe - the real thing, built for Windows - on an emulated PC,
 * and taking what it writes to its DirectSound buffer.
 * <p>
 * <b>What paces it.</b> The emulated player keeps time the way it would on real hardware: it
 * writes into a sound buffer ahead of the play cursor and cannot get further ahead than the
 * buffer is long. So the emulated card has to take its samples at the rate they are meant to be
 * heard at, no faster - which is what happens when {@link PcmQueue#write} blocks until
 * {@link #read} has made room. The cushion between the two is therefore not ours to choose: it
 * is FMP7's own sound buffer, which on this machine is 400ms. There is room for it - FMP7
 * synthesizes about four times faster than it plays here - and the queue below only has to be
 * deep enough not to be the thing in the way.
 * <p>
 * <b>What it says it is playing.</b> FMP7 is closed source, but its api is published and it
 * keeps a shared memory - {@link Fmp7Work} - up to date as it plays. On a real PC another
 * process reads that; here the other process is this jvm, which is what
 * {@link SharedMemory} is. A reading is taken every time samples are handed over and kept with
 * the position in the audio they were handed over at, so that what the visualizer shows is what
 * was playing when the sound it is drawn against was made - see {@link #workAt}.
 * <p>
 * <b>Room.</b> FMP7 needs a machine with 64MB in it. On the default 16 it starts, plays, and
 * quietly never publishes its work: the allocation in front of that fails and it carries on
 * without it.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-08-17 nsano initial version <br>
 * @see <a href="http://fmpdoc.fmp.jp/fmp7-api/">FMP7 公開API仕様</a>
 */
public class Fmp7Player {

    private static final Logger logger = getLogger(Fmp7Player.class.getName());

    /** where FMP7.exe and its dlls live */
    public static final String FMP7_PATH_KEY = "mdplayer.fmp7.path";

    /**
     * Where else to look for a song's wave bank, as a list of directories separated the way
     * {@code mdplayer.fmp.pvi} separates its own.
     */
    public static final String PWI_PATH_KEY = "mdplayer.fmp7.pwi";

    /** how much memory the emulated PC gets, in megabytes - see the class comment */
    private static final String MEMORY = System.getProperty("mdplayer.fmp7.memory", "64");

    /**
     * How much audio the queue can hold.
     * <p>
     * Deep, and deliberately so: the queue filling up is not something FMP7 survives. A full
     * queue means the emulated sound card has stopped taking samples, and a card that stops
     * taking samples is a card that has failed - FMP7 watches for exactly that and shuts itself
     * down cleanly when it sees it, at whatever point in the song it happens, which is a song
     * that stops in the middle for no reason anyone can see. Held to real time by the play
     * cursor instead (see jdosbox's {@code IDirectSoundBuffer.moveCursor}), the emulator does not
     * run far enough ahead to fill this, and thirty seconds of headroom is six megabytes.
     */
    private static final double QUEUE_SECONDS =
            Double.parseDouble(System.getProperty("mdplayer.fmp7.queue", "6"));

    /**
     * How much audio has to be in hand before the first sample is handed over [s].
     * <p>
     * A song is not evenly hard to synthesize - a heavy passage half way through will drain a
     * cushion that the opening bars said was plenty - so the player starts a few seconds behind
     * the emulator rather than level with it. It costs less than it sounds: nothing is being
     * taken while it builds, so the emulator has its whole margin to build with, and three
     * seconds of cushion take about two of waiting.
     */
    private static final double PRIME_SECONDS =
            Double.parseDouble(System.getProperty("mdplayer.fmp7.prime", "3"));

    /** how long FMP7 is given to produce a sound before we give up on it */
    private static final long SILENCE_LIMIT_SECONDS = 60;

    /**
     * How often the work is read, in frames of what FMP7 has produced. Twenty milliseconds is
     * finer than the display is redrawn at and coarser than the pieces the emulated sound card
     * is handed, which is the right way round: a reading per piece would cost twice as much and
     * be kept twice as long for nothing.
     */
    private static final int SNAPSHOT_FRAMES = 48000 / 50;

    /**
     * How many readings of the work are kept.
     * <p>
     * They have to reach back at least as far as the queue does: a reading is chosen by where
     * the listener has got to, and the listener is a whole queue behind what the emulator has
     * made. Too few and the oldest one still held is newer than the one wanted, which is a
     * display running ahead of its own music by however much is missing.
     */
    private static final int SNAPSHOTS =
            (int) (Math.min(QUEUE_SECONDS + 1.5, 10) * 48000 / SNAPSHOT_FRAMES);

    /**
     * How far ahead of the sound FMP7's work runs, at most [ms].
     * <p>
     * It is however much of its sound buffer FMP7 has filled but the card has not played, and it
     * is measured rather than assumed - see {@link #leadMillis}. The cap is what keeps a song
     * that opens with a silent bar from being measured as a lead of that whole bar.
     */
    private static final double MAX_LEAD_MILLIS = 500;

    /**
     * What FMP7 is told about itself before it starts, in the registry file jdosbox reads from
     * beside the program.
     * <p>
     * Every line of it buys back emulated time at the price of some quality: without it FMP7
     * needs nearly all of the machine and the sound breaks up, with it about three quarters of
     * what it has. A jdosbox.reg sitting in the user's own FMP7 directory is used instead, so
     * this is only the default.
     */
    private static final String REGISTRY = """
            Windows Registry Editor Version 5.00

            [HKEY_CURRENT_USER\\SOFTWARE\\Guu\\FMP7]
            ; the interpolation used when a pcm sample is resampled - 0 is the cheapest
            "ReSamplePower"=dword:00000000
            ; the rate it is resampled to: 0=44.1kHz 1=48kHz 2=88.2kHz 3=96kHz 4=176.4kHz 5=192kHz
            "ReSampleRate"=dword:00000001

            ; the emulation of each chip, oversampled or not. Off is where most of the time comes back.
            [HKEY_CURRENT_USER\\SOFTWARE\\Guu\\FMP7\\AddOn\\FMP7]
            "OverSampling"=dword:00000000

            [HKEY_CURRENT_USER\\SOFTWARE\\Guu\\FMP7\\AddOn\\FMP7\\PSG]
            "OverSampling"=dword:00000000

            [HKEY_CURRENT_USER\\SOFTWARE\\Guu\\FMP7\\AddOn\\FMP7\\PCM]
            "OverSampling"=dword:00000000
            """;

    private final byte[] song;

    /** where the song came from, whose other files may be part of it; null when it came from a jar */
    private final Path songDirectory;

    private Path work;

    private JDosBox dosbox;

    private PcmQueue queue;

    private volatile int sampleRate = 48000;
    private volatile int channels = 2;
    private volatile int sampleSizeInBits = 16;

    /** set once FMP7 has produced a sample that is not silence */
    private volatile boolean sounding;

    /** set when FMP7 was given long enough to make a sound and never did */
    private volatile boolean gaveUp;

    /** frames handed to the queue, which is the numbering the readings are filed under */
    private volatile long producedFrames;

    /** how much digital silence has been dropped waiting for {@link #sounding} */
    private long droppedBytes;

    /** frames handed to the caller, which is the clock the display is shown against */
    private volatile long consumedFrames;

    /** readings of the work, oldest first once it has wrapped */
    private final Fmp7Work[] snapshots = new Fmp7Work[SNAPSHOTS];

    /** how many readings have been taken; the newest is at {@code (taken - 1) % SNAPSHOTS} */
    private volatile long taken;

    /** how much had been produced when the last reading was taken */
    private long snapshotAt;

    /** the longest the emulated card was kept waiting for room, and how often it waited long */
    private volatile long longestBlockMillis;
    private volatile int blocksOver100;

    /** how far ahead of the sound the work was when the song started to sound [ms]; -1 until then */
    private volatile double leadMillis = -1;

    /** set once the cushion has been built and the song may start */
    private volatile boolean primed;

    /** how many machines have been booted for this song, the retries included */
    private int attempts;

    /** when the machine now running was started, for the first-sound timeout */
    private volatile long bootedAt;

    /** how often the work may be read to ask whether the song is over [ms] */
    private static final long ASK_INTERVAL_MILLIS = 100;

    /** how long FMP7 may claim to be playing while making no sound and going nowhere [ms] */
    private static final long STOPPED_MILLIS = 2000;

    /** what {@link #isSongOver} reads the work into, and what it last made of it */
    private final Fmp7Work asked = new Fmp7Work(Fmp7Work.GLOBAL_SIZE, Fmp7Work.PART_SIZE);
    private long askedAt;
    private boolean over;
    private double lastPlayMillis = -1;
    private long wentNowhereSince;

    /** FMP7 has said it was playing at least once, and whether the newest reading still says so */
    private volatile boolean everPlayed;
    private volatile boolean playingNow;

    public Fmp7Player(byte[] song, Path songDirectory) {
        this.song = song;
        this.songDirectory = songDirectory;
    }

    /** where FMP7 is, as the system property says */
    public static File playerDirectory() {
        return new File(System.getProperty(FMP7_PATH_KEY, "/usr/local/src/FMP7"));
    }

    /** is there an FMP7 to run? */
    public static boolean isAvailable() {
        return new File(playerDirectory(), "FMP7.exe").exists();
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    /** has FMP7 made a sound yet? */
    public boolean isSounding() {
        return sounding;
    }

    /**
     * How many times a song is given to FMP7 before giving up on it.
     * <p>
     * The second and later machines in one jvm sometimes fail while loading - FMP7's addon driver
     * answers "an unsupported error occurred" and it exits, having made no sound at all - and
     * whatever is left over from the machine before is not yet known. It is not the song's fault
     * and the next machine usually works, so the song gets another go rather than being dropped
     * from the play list. See the driver's readme.
     */
    private static final int ATTEMPTS = Integer.getInteger("mdplayer.fmp7.attempts", 3);

    /**
     * How long a machine is given to make its first sound before it is taken to have failed [ms].
     * <p>
     * The other way the second and later machines fail is by not going at all: FMP7 is up, its
     * work is never published and nothing is ever written to the sound card. Ten seconds is
     * several times what a machine needs to boot and load a song here.
     */
    private static final long START_MILLIS =
            Long.getLong("mdplayer.fmp7.start.timeout", 10_000);

    /**
     * Boots another machine for a song that never made a sound on the last one.
     * <p>
     * Only ever for a machine that has already gone: FMP7 fails during loading or not at all, so
     * once a song has been heard this cannot happen to it.
     */
    private void restart() {
        attempts++;
logger.log(Level.WARNING, "fmp7: the machine went without making a sound; going round again ("
        + attempts + " of " + ATTEMPTS + ")");
        if (dosbox != null) {
            dosbox.stop();
            dosbox = null;
        }
        if (queue != null) {
            queue.close();
        }
        producedFrames = 0;
        consumedFrames = 0;
        snapshotAt = 0;
        taken = 0;
        leadMillis = -1;
        droppedBytes = 0;
        everPlayed = false;
        playingNow = false;
        over = false;
        askedAt = 0;
        lastPlayMillis = -1;
        wentNowhereSince = 0;
        try {
            boot();
        } catch (IOException e) {
logger.log(Level.WARNING, "fmp7: and the next machine would not start either: " + e.getMessage());
        }
    }

    /** Boots the machine and returns; the audio turns up at {@link #read} in its own time. */
    public void start() throws IOException {
        if (!isAvailable()) {
            throw new IOException("no FMP7.exe under " + playerDirectory()
                    + "; set -D" + FMP7_PATH_KEY + "=<dir>");
        }

        // the win32 layer only ever knows one drive (see Win#run, which mounts C and nothing
        // else), so FMP7 and the song have to sit on it together; its own directory is copied
        // rather than mounted so that nothing this writes lands in it
        // FMP7 is wanted for its sound, and drawing its window is about half of everything the
        // machine does - measured on this song, the difference between a cushion that holds and
        // one that drains to nothing part way through. Nobody is looking at it: mdplayer draws
        // its own display from the work FMP7 publishes.
        if (System.getProperty("jdos.novideo") == null) {
            System.setProperty("jdos.novideo", "true");
        }

        work = Files.createTempDirectory("mdplayer-fmp7");
        Files.createDirectories(work.resolve("addon"));
        copy(playerDirectory().toPath(), work);
        Path addon = playerDirectory().toPath().resolve("addon");
        if (Files.isDirectory(addon)) {
            copy(addon, work.resolve("addon"));
        }
        if (!Files.exists(work.resolve("jdosbox.reg"))) {
            Files.writeString(work.resolve("jdosbox.reg"), REGISTRY, StandardCharsets.UTF_8);
        }
        Files.write(work.resolve("song.owi"), song);
        copySongData();
        copyWaveBanks();

        attempts = 1;
        boot();
    }

    /** the machine itself, which a song may need more than one of - see {@link #restart} */
    private void boot() throws IOException {
        primed = false;
        sounding = false;
        gaveUp = false;
        bootedAt = System.currentTimeMillis();
        queue = new PcmQueue((int) (48000 * 2 * 2 * QUEUE_SECONDS));
        for (int i = 0; i < snapshots.length; i++) {
            snapshots[i] = new Fmp7Work(Fmp7Work.GLOBAL_SIZE, Fmp7Work.PART_SIZE);
        }

        dosbox = new JDosBox()
                .arg("-m", MEMORY)
                .mount('c', work.toFile())
                .command("c:")
                .command("FMP7.exe song.owi")
                // nothing on the machine's own mixer is wanted, and not opening a line for it
                // keeps it from fighting the player for the host's audio device
                .set("mixer", "nosound", "true")
                // FMP7 has a whole OPNA to synthesize in real time, so it is not going to spend
                // any of the machine emulating sound cards nothing will use
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
                .set("cpu", "cycles", "max")
                // dosbox's "max" holds the emulated cpu to about 90% of what the host will give
                // it and sleeps a millisecond whenever it finds itself ahead - the right thing
                // for a game, a 10% brake on a machine whose only job is to synthesize audio
                // faster than it is heard. Turbo takes that off; the queue below still paces it.
                .turbo(true)
                // there is no screen to draw, so this is emulation nobody is going to look at
                .set("render", "frameskip", "10")
                .exitWhenProgramFinishes(true)
                .directSoundSink(new Sink());
        dosbox.start();
    }

    /**
     * The files that came with the song, which for some songs are part of it.
     * <p>
     * A song that uses PCM names its sample bank - a ".pwi" - inside itself, and FMP7 opens it by
     * that name out of the directory it is playing from. Copied on its own, such a song plays
     * with its PCM parts silent and nothing anywhere says why. So everything beside it that is
     * not itself a song comes along under its own name, and the emulated drive looks like the
     * directory the song was written in.
     */
    private void copySongData() throws IOException {
        if (songDirectory == null || !Files.isDirectory(songDirectory)) {
            return;
        }
        for (Path f : Files.newDirectoryStream(songDirectory)) {
            String name = f.getFileName().toString();
            String lower = name.toLowerCase();
            // wave banks are not copied here: they are named inside the song and sometimes have
            // to be renamed on the way over, which is copyWaveBanks' business
            if (!Files.isRegularFile(f) || lower.endsWith(".owi") || lower.endsWith(".mwi")
                    || lower.endsWith(".pwi") || name.startsWith(".") || Files.exists(work.resolve(name))) {
                continue;
            }
            try {
                Files.copy(f, work.resolve(name));
            } catch (IOException e) {
logger.log(Level.DEBUG, "fmp7: leaving " + name + " behind: " + e.getMessage());
            }
        }
    }

    /**
     * Finds the wave banks the song names and puts them beside it.
     * <p>
     * A song that uses PCM names its bank inside itself, without the ".pwi" it is kept in, and
     * FMP7 opens it out of the directory it is playing from. A song whose bank is not there does
     * not play at all - FMP7 gives up part way through loading and exits without a word, which
     * from outside looks like a song that is simply silent - so it is worth looking further than
     * the one directory: the song's own, then wherever {@link #PWI_PATH_KEY} says, then the
     * directories beside the song's own, which is how a collection of these is usually laid out.
     */
    private void copyWaveBanks() {
        List<Fmp7File.WaveBank> banks;
        try {
            banks = Fmp7File.decode(song).getWaveBanks();
        } catch (IOException e) {
            return;
        }
        for (Fmp7File.WaveBank bank : banks) {
            String name = bank.name() + ".pwi";
            if (Files.exists(work.resolve(name))) {
                continue;
            }
            Path found = findWaveBank(name);
            if (found == null) {
logger.log(Level.WARNING, "fmp7: " + name + " is nowhere to be found, and the song will not play"
        + " without it; -D" + PWI_PATH_KEY + "=<dir> says where to look");
                continue;
            }
            if (!isAscii(bank.name())) {
                // it will go over under its own name and FMP7 will ask for it by that name, and
                // the dos filesystem under the emulated drive does not carry those. Renaming it
                // would mean writing over the name inside the song, which carries a checksum
                // per chunk that there is no published way to recompute.
logger.log(Level.WARNING, "fmp7: " + name + " is not named in ascii, and the emulated drive"
        + " cannot carry that name; the song will not play");
            }
            try {
                Files.copy(found, work.resolve(name));
logger.log(Level.DEBUG, "fmp7: " + name + " came from " + found.getParent());
            } catch (IOException e) {
logger.log(Level.WARNING, "fmp7: cannot take " + found + ": " + e.getMessage());
            }
        }
    }

    /** a name the dos filesystem under the emulated drive can carry */
    private static boolean isAscii(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) > 0x7e || name.charAt(i) < 0x20) {
                return false;
            }
        }
        return true;
    }

    /** the song's own directory, then the search path, then the directories beside the song's */
    private Path findWaveBank(String name) {
        List<Path> places = new ArrayList<>();
        if (songDirectory != null) {
            places.add(songDirectory);
        }
        String path = System.getProperty(PWI_PATH_KEY, "");
        for (String each : path.split(File.pathSeparator + "|;")) {
            if (!each.isBlank()) {
                places.add(Path.of(each.trim()));
            }
        }
        // the directories under the song's own and under the one above it, which is how a
        // collection of these is laid out: one folder to a song, and the odd loose file at the top
        for (Path dir : new Path[] {songDirectory, songDirectory == null ? null : songDirectory.getParent()}) {
            if (dir == null) {
                continue;
            }
            try (Stream<Path> under = Files.list(dir)) {
                under.filter(Files::isDirectory).forEach(places::add);
            } catch (IOException e) {
logger.log(Level.DEBUG, "fmp7: cannot look under " + dir + ": " + e.getMessage());
            }
        }
        for (Path place : places) {
            Path file = place.resolve(name);
            if (Files.isRegularFile(file)) {
                return file;
            }
        }
        return null;
    }

    /** FMP7's own files: the program, its dlls, its addon drivers and its settings */
    private static void copy(Path from, Path to) throws IOException {
        for (Path f : Files.newDirectoryStream(from)) {
            String name = f.getFileName().toString().toLowerCase();
            if (Files.isRegularFile(f)
                    && (name.endsWith(".exe") || name.endsWith(".dll") || name.equals("jdosbox.reg"))) {
                Files.copy(f, to.resolve(f.getFileName().toString()));
            }
        }
    }

    /** what FMP7 writes to its DirectSound buffer */
    private class Sink implements AudioSink {

        @Override
        public void open(int sampleRate, int sampleSizeInBits, int channels) {
            Fmp7Player.this.sampleRate = sampleRate;
            Fmp7Player.this.sampleSizeInBits = sampleSizeInBits;
            Fmp7Player.this.channels = channels;
logger.log(Level.DEBUG, "fmp7: dsound " + sampleRate + "Hz " + sampleSizeInBits + "bit " + channels + "ch");
        }

        @Override
        public void write(byte[] data, int offset, int length) {
            int frame = channels * (sampleSizeInBits / 8);

            if (!sounding) {
                int start = firstNonZero(data, offset, length);
                if (start < 0) {
                    // dropped, not queued: FMP7 spends its first seconds loading, and what it
                    // writes while it does is digital silence nobody wants to sit through
                    droppedBytes += length;
                    if (droppedBytes > (long) sampleRate * frame * SILENCE_LIMIT_SECONDS) {
logger.log(Level.WARNING, "fmp7: nothing but silence after " + SILENCE_LIMIT_SECONDS + "s, giving up");
                        gaveUp = true;
                        queue.finish();
                    }
                    return;
                }
                // align to a frame so the channels do not swap
                start -= (start - offset) % frame;
                length -= start - offset;
                offset = start;
                sounding = true;
logger.log(Level.DEBUG, "fmp7: sound starts, " + droppedBytes + " bytes of silence dropped");
            }

            // the reading is taken before the samples go in, so that it is the state of the
            // player at the moment it had made this much sound and no more. It is counted in
            // the frames that are queued and not the ones that were made: the silence dropped
            // in front of the song never reaches the caller, so counting it here would file
            // every reading that much too late.
            producedFrames += length / frame;
            if (producedFrames - snapshotAt >= SNAPSHOT_FRAMES) {
                snapshotAt = producedFrames;
                snapshot();
            }

            // blocking here is the point: it is the emulated sound card taking its samples at
            // the rate they are heard, which is what holds FMP7 to the song's own tempo
            long before = System.nanoTime();
            queue.write(data, offset, length);
            long blocked = (System.nanoTime() - before) / 1000000L;
            if (blocked > longestBlockMillis) {
                longestBlockMillis = blocked;
            }
            if (blocked > 100) {
                blocksOver100++;
            }
        }

        @Override
        public void close() {
logger.log(Level.DEBUG, "fmp7: dsound closed");
        }
    }

    /** Reads the work as it is now and files it under how much sound has been made so far. */
    private void snapshot() {
        Fmp7Work next = snapshots[(int) (taken % SNAPSHOTS)];
        if (SharedMemory.read(Fmp7Work.KEY_MAP, 0, next.buffer(), 0, next.size()) == 0) {
            return; // FMP7 has not published its work yet
        }
        next.atFrame = producedFrames;
        taken++;

        // whether FMP7 is playing *now*, which is the only reading that can say a song is over.
        // The one the driver is shown is chosen by where the listener has got to and may have
        // fallen off the back of the ring, in which case it is the oldest one still held - from
        // before the song started, saying "not playing" about a song that has not begun.
        if (next.playing()) {
            everPlayed = true;
            playingNow = true;
        } else {
            playingNow = false;
        }

        if (leadMillis < 0 && next.playing()) {
            // whatever FMP7 had already made and the card had not played is how far ahead of the
            // sound its work runs, and this is the moment that lead can be seen: the first sound
            // of the song is being handed over, and the player is that much further on
            leadMillis = Math.min(next.playMillis(), MAX_LEAD_MILLIS);
logger.log(Level.DEBUG, "fmp7: the work runs %.0fms ahead of the sound".formatted(leadMillis));
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
     * Takes the next samples FMP7 has made.
     *
     * @param timeoutMillis how long to wait for them before giving up and returning short
     * @return bytes read; 0 means either the song is over ({@link #isFinished}) or the emulator
     *         did not keep up
     */
    public int read(byte[] b, int offset, int length, long timeoutMillis) {
        if (queue == null) {
            return 0;
        }
        // a machine that is never going to make a sound, either because it has gone or because
        // it is sitting there having failed to load - see ATTEMPTS
        if (!sounding && attempts < ATTEMPTS && dosbox != null
                && (!dosbox.isRunning() || System.currentTimeMillis() - bootedAt > START_MILLIS)) {
            restart();
            return 0;
        }
        if (!primed) {
            // nothing is handed over until the cushion is built, or until it becomes clear that
            // there will never be that much - a song shorter than the cushion, or a player that
            // has finished or failed
            if (!queue.awaitAtLeast(primeBytes(), timeoutMillis)
                    && !queue.isDrained() && (dosbox == null || dosbox.isRunning() || !sounding)) {
                return 0;
            }
            primed = true;
logger.log(Level.DEBUG, "fmp7: primed with %.1fs".formatted(getCushionSeconds()));
        }
        int n = queue.read(b, offset, length, timeoutMillis);
        consumedFrames += n / (channels * (sampleSizeInBits / 8));
        if (n == 0 && dosbox != null && !dosbox.isRunning()) {
            queue.finish();
        }
        return n;
    }

    /** frames handed over so far, which is what the listener has heard bar the output buffer */
    public long getConsumedFrames() {
        return consumedFrames;
    }

    /**
     * Copies into {@code out} what FMP7 was playing when the sound {@code framesBack} behind the
     * last frame handed over was made.
     * <p>
     * Two things sit between the two: the queue, which is however far the emulator has run ahead
     * of the listener, and FMP7's own sound buffer, which is how far its player has run ahead of
     * its sound card. The first is exact - every reading is filed under the frame it was taken
     * at - and the second is {@link #leadMillis}.
     *
     * @return false while FMP7 has published nothing to read
     */
    public boolean workAt(long framesBack, Fmp7Work out) {
        long taken = this.taken;
        if (taken == 0) {
            return false;
        }
        double lead = leadMillis > 0 ? leadMillis : 0;
        long want = consumedFrames - framesBack - (long) (lead * sampleRate / 1000);

        // the readings are in order, so the one wanted is the last one taken at or before it
        long first = Math.max(0, taken - SNAPSHOTS);
        Fmp7Work found = snapshots[(int) ((taken - 1) % SNAPSHOTS)];
        for (long i = taken - 1; i >= first; i--) {
            Fmp7Work each = snapshots[(int) (i % SNAPSHOTS)];
            found = each;
            if (each.atFrame <= want) {
                break;
            }
        }
        out.copyFrom(found);
        return true;
    }

    /** how much has to be in the queue before the song starts */
    private int primeBytes() {
        double seconds = Math.min(PRIME_SECONDS, QUEUE_SECONDS * 0.9);
        return (int) (seconds * sampleRate * channels * (sampleSizeInBits / 8));
    }

    /** what FMP7 has been up to, for a log line or a test */
    public String getStatistics() {
        int frameSize = channels * (sampleSizeInBits / 8);
        return "%.1fs produced, %.1fs of it the silence it starts with, %d readings, kept waiting %dms at worst (%d times over 100ms)".formatted(
                producedFrames / (double) sampleRate,
                droppedBytes / (double) (sampleRate * frameSize), taken,
                longestBlockMillis, blocksOver100);
    }

    /** how much audio is queued ahead of the listener - the cushion, in seconds */
    public double getCushionSeconds() {
        if (queue == null) {
            return 0;
        }
        return queue.available() / (double) (sampleRate * channels * (sampleSizeInBits / 8));
    }

    /** what the machine died of, when it did not end on its own terms */
    public Throwable getFailure() {
        return dosbox == null ? null : dosbox.getFailure();
    }

    /**
     * Is the song over on its own terms - FMP7 has stopped playing and everything it made has
     * been handed over? A song that does not loop ends this way and no other: the machine stays
     * up with the program sitting there, and a driver watching only for the machine to go would
     * render silence for ever and never let the play list move on.
     */
    public boolean isSongOver() {
        if (!everPlayed || (queue != null && queue.available() > 0)) {
            return false;
        }
        // The readings are taken as the samples are handed over, so when the song stops there
        // are no more of them and the last one says "playing" for ever. Asked that, a driver
        // waits at the end of every song that does not loop, which is a player that hangs on
        // the last bar. So the work itself is read, here, now - not often, because this is
        // called for every frame of silence a starved render puts out.
        long now = System.currentTimeMillis();
        if (now - askedAt < ASK_INTERVAL_MILLIS) {
            return over;
        }
        askedAt = now;
        if (SharedMemory.read(Fmp7Work.KEY_MAP, 0, asked.buffer(), 0, asked.size()) == 0) {
            return over;
        }
        if (!asked.playing()) {
            // it says so itself, which is how a song with an end ends
            over = true;
            return true;
        }
        // and if it still claims to be playing while making no sound and going nowhere, it has
        // stopped in a way it does not have a word for
        double at = asked.playMillis();
        if (at != lastPlayMillis) {
            lastPlayMillis = at;
            wentNowhereSince = now;
        } else if (wentNowhereSince != 0 && now - wentNowhereSince > STOPPED_MILLIS) {
            over = true;
        }
        return over;
    }

    /** is the song over - the machine gone and the queue dry, or never a sound out of it? */
    public boolean isFinished() {
        if (dosbox == null || gaveUp) {
            return true;
        }
        if (!sounding && attempts < ATTEMPTS) {
            // not finished, not started: a song that has made no sound yet still has another
            // machine owed to it, and saying "finished" here is what used to drop it from the
            // play list on the driver's very first starved read
            return false;
        }
        return !dosbox.isRunning() && (queue == null || queue.available() == 0);
    }

    /** Shuts the machine down and takes the song's scratch directory with it. */
    public void stop() {
logger.log(Level.DEBUG, "fmp7: " + getStatistics());
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
