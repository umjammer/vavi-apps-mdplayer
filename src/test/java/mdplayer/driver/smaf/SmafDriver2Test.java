/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.smaf;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import mdplayer.ChipFmDspSource;
import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;
import mdplayer.lib.smaf.SmafFile;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;
import vavi.sound.ma7.Ma7Rom;
import vavi.sound.visualizer.fmdsp.TrackId;
import vavi.sound.visualizer.fmdsp.TrackStatus;
import vavi.util.event.GenericEvent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * SMAF (".mmf") on the yamaha MA-7 in pure java: the song through the format, the plugin and
 * {@link SmafDriver2}, and the keys it puts on the fmdsp rows.
 * <p>
 * The rom is read out of the installed {@code libM7_EmuSmw7.so}, which is nobody's to ship, so
 * every test here is skipped without it - {@code -Dvavi.sound.ma7.path=<the .so or the apk>}.
 * <p>
 * system properties
 * <ul>
 *  <li>{@code mdplayer.smaf.test.mmf} ... the song to render</li>
 *  <li>{@code mdplayer.smaf.test.seconds} ... how much of it, default 15</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-21 nsano initial version <br>
 */
class SmafDriver2Test {

    /** the mmftool distribution carries one, so there is usually something to play */
    static final Path mmf = Path.of(System.getProperty("mdplayer.smaf.test.mmf",
            System.getProperty("user.home") + "/Public/np2/mmf/ma3/sor51ma3.mmf"));

    /** songs of every SMAF generation, for {@link #everyGenerationPlays} */
    static final Path corpus = Path.of(System.getProperty("mdplayer.smaf.test.corpus",
            System.getProperty("user.home") + "/Public/np2/mmf"));

    @Test
    void readsWhatTheFileSaysAboutItself() throws Exception {
        assumeTrue(Files.exists(mmf), mmf + " is missing, set -Dmdplayer.smaf.test.mmf");

        byte[] b = Files.readAllBytes(mmf);
        MetaData md = new SmafDriver2().retrieveMetaData(b);
        assertNotNull(md);
        assertEquals(SmafFile.decode(b).getTitle(), md.getFirst(Tag.Title));
        // whatever the file was made for, the MA-7 is what sounds it here
        assertEquals("MA-7", md.getFirst(Tag.Chip));
    }

    /** the plugin builds the new driver; {@link SmafDriver} is only a comment away */
    @Test
    void theFormatBuildsTheMa7Driver() throws Exception {
        assumeTrue(Ma7Rom.isAvailable(), "no " + Ma7Rom.path() + ", set -D" + Ma7Rom.PATH_KEY);
        assumeTrue(Files.exists(mmf), mmf + " is missing");

        BasePlugin<? extends BaseDriver> plugin = open(mmf);
        try {
            assertInstanceOf(SmafDriver2.class, plugin.getDriver());
        } finally {
            plugin.stop();
            plugin.close();
        }
    }

    @Test
    void playsThroughTheDriver() throws Exception {
        assumeTrue(Ma7Rom.isAvailable(), "no " + Ma7Rom.path() + ", set -D" + Ma7Rom.PATH_KEY);
        assumeTrue(Files.exists(mmf), mmf + " is missing");

        int sampleRate = Setting.getInstance().getOutputDevice().getSampleRate();
        // the stream waves are mixed into the song here, this is for the ones that are not
        System.setProperty("vavi.sound.mobile.AudioEngine.volume", "0.02");

        BasePlugin<? extends BaseDriver> plugin = open(mmf);
        BaseDriver driver = plugin.getDriver();
        assertInstanceOf(SmafDriver2.class, driver);
        assertNotNull(driver.metaData);

        int seconds = Integer.getInteger("mdplayer.smaf.test.seconds", 15);
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        short[] buffer = new short[2048];
        int peak = 0;
        int rendered = 0;
        long start = System.currentTimeMillis();
        while (rendered < sampleRate * seconds && !driver.stopped) {
            driver.render(buffer, 0, buffer.length);
            for (short s : buffer) {
                peak = Math.max(peak, Math.abs(s));
                pcm.write(s & 0xff);
                pcm.write((s >> 8) & 0xff);
            }
            rendered += buffer.length / 2;
        }
        long elapsed = System.currentTimeMillis() - start;

        plugin.stop();
        plugin.close();

        byte[] b = pcm.toByteArray();
        Path out = Path.of("tmp/smaf-driver2.wav");
        Files.createDirectories(out.getParent());
        AudioFormat af = new AudioFormat(sampleRate, 16, 2, true, false);
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(b), af, b.length / 4),
                AudioFileFormat.Type.WAVE, out.toFile());
System.err.printf("%s: %.1fs of audio in %.1fs, peak %d -> %s%n",
        mmf, rendered / (double) sampleRate, elapsed / 1000.0, peak, out);

        assertTrue(peak > 1000, mmf + " rendered near silence, peak " + peak);
        assertTrue(driver.counter > 0, "the driver clock should have advanced");
    }

    /** the keys the song sends light the fm rows, see {@link mdplayer.fmdsp.Smaf2Reader} */
    @Test
    void theVisualizerShowsTheKeys() throws Exception {
        assumeTrue(Ma7Rom.isAvailable(), "no " + Ma7Rom.path() + ", set -D" + Ma7Rom.PATH_KEY);
        assumeTrue(Files.exists(mmf), mmf + " is missing");

        BasePlugin<? extends BaseDriver> plugin = open(mmf);
        plugin.stopped = false;
        plugin.paused = false;
        plugin.fadeout = false;
        try {
            ChipFmDspSource source = new ChipFmDspSource();
            source.bind(plugin);
            BaseDriver driver = plugin.getDriver();
            short[] buffer = new short[1024];
            TrackStatus status = new TrackStatus();
            Set<TrackId> lit = EnumSet.noneOf(TrackId.class);
            int sampleRate = Setting.getInstance().getOutputDevice().getSampleRate();
            for (int i = 0; i < sampleRate * 2 * 10 / buffer.length && !driver.stopped; i++) {
                driver.render(buffer, 0, buffer.length);
                source.update(new GenericEvent(driver, "master", buffer, 0));
                for (TrackId row : TrackId.values()) {
                    source.readStatus(row, status);
                    if (status.playing && status.key != 0xff && row.name().startsWith("FM")) {
                        lit.add(row);
                    }
                }
            }
System.err.println(mmf.getFileName() + ": lit " + lit);
            assertTrue(!lit.isEmpty(), "no fm row showed a key");
            assertTrue(Stream.of(TrackId.values()).anyMatch(r -> r.name().startsWith("FM")));
        } finally {
            plugin.stop();
            plugin.close();
        }
    }

    /**
     * Every generation the corpus has - MA-1, MA-2, MA-3, MA-5 and Uta - read, converted and
     * rendered on the one sound source, which is what says the driver is not an MA-3 driver with
     * a sample song. A few seconds of each is enough: what goes wrong goes wrong at the start.
     */
    @Test
    void everyGenerationPlays() throws Exception {
        assumeTrue(Ma7Rom.isAvailable(), "no " + Ma7Rom.path() + ", set -D" + Ma7Rom.PATH_KEY);
        assumeTrue(Files.isDirectory(corpus), corpus + " is missing, set -Dmdplayer.smaf.test.corpus");

        Map<SmafFile.Format, int[]> played = new EnumMap<>(SmafFile.Format.class);
        List<Path> songs;
        try (Stream<Path> walk = Files.walk(corpus)) {
            songs = walk.filter(p -> p.toString().toLowerCase().endsWith(".mmf")).sorted().toList();
        }
        // one of each generation is the point, not the whole corpus
        Map<SmafFile.Format, Integer> seen = new EnumMap<>(SmafFile.Format.class);
        for (Path song : songs) {
            SmafFile.Format format = SmafFile.decode(Files.readAllBytes(song)).getFormat();
            if (seen.merge(format, 1, Integer::sum) > PER_FORMAT) continue;

            int[] count = played.computeIfAbsent(format, f -> new int[2]);
            count[0]++;
            try {
                if (peakOf(song, 3) > 100) count[1]++;
            } catch (RuntimeException e) {
System.err.println(song + ": " + e);
            }
        }
System.err.println(corpus + ": sounded / played " + played.entrySet().stream()
        .map(e -> e.getKey() + " " + e.getValue()[1] + "/" + e.getValue()[0]).toList());

        assertTrue(played.size() > 1, "only " + played.keySet() + " in the corpus");
        played.forEach((format, count) ->
                assertTrue(count[1] > 0, format + ": none of its " + count[0] + " songs made a sound"));
    }

    /** how many songs of a generation the sweep plays */
    private static final int PER_FORMAT = 3;

    /** @return the loudest sample of the song's first {@code seconds} */
    private static int peakOf(Path song, int seconds) throws Exception {
        int sampleRate = Setting.getInstance().getOutputDevice().getSampleRate();
        BasePlugin<? extends BaseDriver> plugin = open(song);
        try {
            BaseDriver driver = plugin.getDriver();
            short[] buffer = new short[2048];
            int peak = 0;
            for (int rendered = 0; rendered < sampleRate * seconds && !driver.stopped; rendered += buffer.length / 2) {
                driver.render(buffer, 0, buffer.length);
                for (short s : buffer) {
                    peak = Math.max(peak, Math.abs(s));
                }
            }
            return peak;
        } finally {
            plugin.stop();
            plugin.close();
        }
    }

    /** the song through the format and the plugin, rendered by hand rather than to a device */
    private static BasePlugin<? extends BaseDriver> open(Path song) throws Exception {
        Setting.getInstance().getOutputDevice().setDeviceType(Common.DEV_Null);

        FileFormat format = FileFormat.getFileFormat(song.toString());
        format.load(new BufferedInputStream(Files.newInputStream(song)), null);
        @SuppressWarnings("unchecked")
        BasePlugin<? extends BaseDriver> plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", song.toString()));
        plugin.prepare();
        return plugin;
    }
}
