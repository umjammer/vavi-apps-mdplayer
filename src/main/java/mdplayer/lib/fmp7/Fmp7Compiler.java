/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.lib.fmp7;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jdos.api.JDosBox;
import jdos.api.StdioSink;

import static java.lang.System.getLogger;


/**
 * Compiles FMP7 MML - a ".mwi" - into the ".owi" the player reads, by running FMP7's own
 * compiler on an emulated PC.
 * <p>
 * <b>Not FMC7.exe.</b> The compiler that comes with FMP7 is a gui program and it ignores its
 * command line entirely: its startup takes the arguments and nothing ever reads them again, and
 * it compiles only from its own file dialog, from a file dropped on its window, or from one it
 * has been told to watch. None of those can be driven from outside. What can is
 * {@code fmc.dll}, the compile core underneath it, whose {@code Compile()} is published for
 * exactly this purpose - see <a href="http://fmpdoc.fmp.jp/fmc-api/">the api page</a>. So what
 * runs on the emulated PC is {@code fmc7c.exe}, forty lines of C that load that dll and call it;
 * it is built from {@code /mdplayer/lib/fmp7/fmc7c.c} beside the copy this carries, and the
 * command that builds it is at the top of that file.
 * <p>
 * <b>What it needs.</b> {@code fmc.dll} and {@code common_resrc.dll}, out of the same FMP7
 * directory {@link Fmp7Player} plays from - {@code -Dmdplayer.fmp7.path=<dir>}.
 * <p>
 * <b>What comes back.</b> The object file, or an {@link IOException} carrying what the compiler
 * said. FMC7 has moved on since most of these songs were written and is stricter than the
 * version they were compiled with - envelopes on a pcm part, for one, are an error now and were
 * not then - so a song that will not compile is not necessarily a song with anything wrong with
 * it, and saying which line it stopped at is the whole of what can be done about that.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-08-27 nsano initial version <br>
 * @see <a href="http://fmpdoc.fmp.jp/fmc-api/">FMC.dll 利用手引き</a>
 */
public class Fmp7Compiler {

    private static final Logger logger = getLogger(Fmp7Compiler.class.getName());

    /** how much memory the emulated PC gets, in megabytes; the compiler is not FMP7 and needs little */
    private static final String MEMORY = System.getProperty("mdplayer.fmp7.compiler.memory", "32");

    /** how long a compile is given before the machine is taken down under it [ms] */
    private static final long TIMEOUT_MILLIS =
            Long.getLong("mdplayer.fmp7.compiler.timeout", 120_000);

    /** the front end for fmc.dll that runs on the emulated PC, carried in the jar beside its source */
    private static final String FMC7C = "/mdplayer/lib/fmp7/fmc7c.exe";

    /** what the compiler wrote, one line to an entry, errors and warnings alike */
    private final List<String> log = new CopyOnWriteArrayList<>();

    /** is there an fmc.dll to compile with? */
    public static boolean isAvailable() {
        File directory = Fmp7Player.playerDirectory();
        return new File(directory, "fmc.dll").exists()
                && new File(directory, "common_resrc.dll").exists();
    }

    /** what the compiler had to say about the last song, errors and warnings in the order they came */
    public List<String> getLog() {
        return List.copyOf(log);
    }

    /** the error and warning lines only, which is what a song that would not compile has to show for it */
    public List<String> getProblems() {
        return log.stream().filter(l -> l.startsWith("fmc7c: error") || l.startsWith("fmc7c: warning")).toList();
    }

    /**
     * Compiles one song.
     *
     * @param mml           the ".mwi" itself
     * @param songDirectory where it came from, whose other files may be part of it - a song that
     *                      includes another file names it relative to its own directory; null
     *                      when the song did not come from one
     * @return the ".owi"
     * @throws IOException when there is no fmc.dll to compile with, when the emulated PC will not
     *                     run, or when the compiler rejected the song - in which case the message
     *                     carries the first few things it said, and {@link #getProblems} the rest
     */
    public byte[] compile(byte[] mml, Path songDirectory) throws IOException {
        if (!isAvailable()) {
            throw new IOException("no fmc.dll under " + Fmp7Player.playerDirectory()
                    + "; set -D" + Fmp7Player.FMP7_PATH_KEY + "=<dir>");
        }
        log.clear();

        Path work = Files.createTempDirectory("mdplayer-fmc7");
        try {
            // the win32 layer only ever knows one drive, so the compiler and the song have to sit
            // on it together, and the song's own directory comes along with it: a song made of
            // more than one file names the others relative to where it is
            copyCompiler(work);
            copySongData(work, songDirectory);
            Files.write(work.resolve("song.mwi"), mml);
            Files.deleteIfExists(work.resolve("song.owi"));

            run(work);

            Path object = work.resolve("song.owi");
            if (!Files.exists(object)) {
                throw new IOException(whatWentWrong());
            }
            byte[] result = Files.readAllBytes(object);
logger.log(Level.DEBUG, "fmc7: compiled %d bytes of mml into %d, %d warnings"
        .formatted(mml.length, result.length, getProblems().size()));
            return result;
        } finally {
            delete(work);
        }
    }

    /**
     * What to say about a song that produced no object.
     * <p>
     * There are two ways that happens and they read very differently. If the compiler got as far
     * as answering, its own errors are the answer and they name the line. If it did not, the song
     * has something in it that fmc.dll reports by throwing - which is how it reports every error
     * it finds, one throw each, caught a frame or two up - and the emulated PC has no c++
     * exception handling to carry that throw anywhere, so the program dies where it stands and
     * takes the reason with it. A song that compiles has nothing to throw and does not care; a
     * song that does not compile is a song that would not have compiled on Windows either, but
     * here it cannot say why. Warnings are not thrown and cost nothing.
     */
    private String whatWentWrong() {
        List<String> problems = getProblems();
        if (!problems.isEmpty()) {
            return String.join("; ", problems.subList(0, Math.min(3, problems.size())))
                    + (problems.size() > 3 ? " (and " + (problems.size() - 3) + " more)" : "");
        }
        if (log.stream().noneMatch(l -> l.startsWith("fmc7c: status"))) {
            return "the compiler stopped at the first thing it did not like."
                    + " Which line that was cannot be told from here: fmc.dll reports an error by"
                    + " throwing it, and the emulated PC cannot unwind a c++ throw";
        }
        return "the compiler produced nothing and said nothing";
    }

    /** boots the machine, runs the compiler on it and waits for it to finish */
    private void run(Path work) throws IOException {
        // the compiler draws nothing and there is nobody to look at it if it did
        if (System.getProperty("jdos.novideo") == null) {
            System.setProperty("jdos.novideo", "true");
        }

        JDosBox dosbox = new JDosBox()
                .arg("-m", MEMORY)
                .mount('c', work.toFile())
                .command("c:")
                // absolute paths: the win32 layer's idea of where it is need not be the drive's
                .command("fmc7c.exe c:\\song.mwi c:\\song.owi")
                .set("mixer", "nosound", "true")
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
                // nothing here is paced by anything: there is no audio to keep up with, and the
                // sooner it is over the sooner the song can start
                .turbo(true)
                .set("render", "frameskip", "10")
                .exitWhenProgramFinishes(true)
                .stdioSink(new Sink());
        dosbox.start();
        boolean finished = dosbox.await(TIMEOUT_MILLIS);
        dosbox.stop();
        if (!finished) {
            throw new IOException("the compiler did not finish within " + TIMEOUT_MILLIS + "ms");
        }
        if (dosbox.getFailure() != null) {
            throw new IOException("the emulated PC went while compiling", new Exception(dosbox.getFailure()));
        }
    }

    /** takes the lines fmc7c prints, which are utf-8 whatever the compiler said them in */
    private class Sink implements StdioSink {

        private final StringBuilder pending = new StringBuilder();

        @Override
        public void write(byte[] data, int offset, int length, long frames) {
            pending.append(new String(data, offset, length, StandardCharsets.UTF_8));
            int at;
            while ((at = pending.indexOf("\n")) >= 0) {
                String line = pending.substring(0, at).trim();
                pending.delete(0, at + 1);
                if (!line.isEmpty()) {
                    log.add(line);
logger.log(Level.DEBUG, line);
                }
            }
        }
    }

    /** fmc7c.exe out of the jar, and the two dlls it needs out of the FMP7 directory */
    private static void copyCompiler(Path work) throws IOException {
        try (InputStream in = Fmp7Compiler.class.getResourceAsStream(FMC7C)) {
            if (in == null) {
                throw new IOException("no " + FMC7C + " in the jar");
            }
            Files.write(work.resolve("fmc7c.exe"), in.readAllBytes());
        }
        Path from = Fmp7Player.playerDirectory().toPath();
        for (String name : new String[] {"fmc.dll", "common_resrc.dll"}) {
            Files.copy(from.resolve(name), work.resolve(name));
        }
    }

    /**
     * The files that came with the song. A song split across several files names the others
     * relative to its own directory, so the emulated drive is made to look like that directory -
     * everything on it except another song's object file, which would only be in the way.
     */
    private static void copySongData(Path work, Path songDirectory) {
        if (songDirectory == null || !Files.isDirectory(songDirectory)) {
            return;
        }
        try {
            for (Path f : Files.newDirectoryStream(songDirectory)) {
                String name = f.getFileName().toString();
                if (!Files.isRegularFile(f) || name.startsWith(".")
                        || name.toLowerCase().endsWith(".owi") || Files.exists(work.resolve(name))) {
                    continue;
                }
                try {
                    Files.copy(f, work.resolve(name));
                } catch (IOException e) {
logger.log(Level.DEBUG, "fmc7: leaving " + name + " behind: " + e.getMessage());
                }
            }
        } catch (IOException e) {
logger.log(Level.DEBUG, "fmc7: cannot look in " + songDirectory + ": " + e.getMessage());
        }
    }

    private static void delete(Path directory) {
        try {
            List<Path> paths = new ArrayList<>();
            try (var walk = Files.walk(directory)) {
                walk.forEach(paths::add);
            }
            for (int i = paths.size() - 1; i >= 0; i--) {
                Files.deleteIfExists(paths.get(i));
            }
        } catch (IOException | UncheckedIOException e) {
logger.log(Level.DEBUG, "fmc7: leaving " + directory + " behind: " + e.getMessage());
        }
    }
}
