/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.fmp7;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import mdplayer.Audio;
import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;
import mdplayer.lib.fmp7.Fmp7File;
import mdplayer.lib.fmp7.Fmp7Player;
import mdplayer.lib.fmp7.Fmp7Work;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * Plays an .owi the way the player does - through the format, the plugin and the driver - which
 * means booting an emulated PC and running FMP7.exe on it, so this is not a quick test.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-08-17 nsano initial version <br>
 */
class Fmp7DriverTest {

    static final Path owi = Path.of(System.getProperty("mdplayer.fmp7.test.owi", "tmp/fmp7/829.owi"));

    @Test
    void readsWhatTheFileSaysAboutItself() throws Exception {
        assumeTrue(Files.exists(owi), owi + " is missing, set -Dmdplayer.fmp7.test.owi");

        Fmp7File file = Fmp7File.decode(Files.readAllBytes(owi));
        assertNotNull(file.getTitle());
        assertNotNull(file.getComposer());

        MetaData md = new Fmp7Driver().retrieveMetaData(Files.readAllBytes(owi));
        assertNotNull(md);
        assertEquals(file.getTitle(), md.getFirst(Tag.Title));
        assertEquals(file.getComposer(), md.getFirst(Tag.Composer));
        assertEquals("FMP7", md.getFirst(Tag.Chip));
System.err.printf("%s: \"%s\" by %s (data: %s)%n", owi, file.getTitle(), file.getComposer(), file.getCreator());
    }

    @Test
    void theFormatIsFoundByItsHeader() throws Exception {
        assumeTrue(Files.exists(owi), owi + " is missing");

        try (BufferedInputStream is = new BufferedInputStream(Files.newInputStream(owi))) {
            assertInstanceOf(Fmp7FileFormat.class, FileFormat.getFileFormat(is));
        }
        assertInstanceOf(Fmp7FileFormat.class, FileFormat.getFileFormat("song.owi"));
    }

    /**
     * Plays for a while and asks two things of it: that sound came out, and that FMP7 said what
     * it was playing while it did. The second is what the visualizer is drawn from, and it is
     * the half that fails quietly - FMP7 plays perfectly well without ever publishing its work.
     */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void playsThroughTheDriver() throws Exception {
        assumeTrue(Fmp7Player.isAvailable(),
                "no FMP7.exe, set -D" + Fmp7Player.FMP7_PATH_KEY + "=<dir>");
        assumeTrue(Files.exists(owi), owi + " is missing");

        Setting setting = Setting.getInstance();
        setting.getOutputDevice().setDeviceType(Common.DEV_Null);
        int sampleRate = setting.getOutputDevice().getSampleRate();

        FileFormat format = FileFormat.getFileFormat(owi.toString());
        format.load(new BufferedInputStream(Files.newInputStream(owi)), null);

        @SuppressWarnings("unchecked")
        BasePlugin<? extends BaseDriver> plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", owi.toString()));
        long opened = System.currentTimeMillis();
        plugin.prepare();

        BaseDriver driver = plugin.getDriver();
        assertInstanceOf(Fmp7Driver.class, driver);
        assertNotNull(driver.metaData);
        Fmp7Driver fmp7 = (Fmp7Driver) driver;

        // it loops for ever, so this is a length rather than a cap
        int seconds = Integer.getInteger("mdplayer.fmp7.test.seconds", 20);
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        short[] buffer = new short[2048];
        int peak = 0;
        int rendered = 0;
        long songStart = 0;
        long songFrames = 0;
        int notes = 0;
        int sounded = 0;
        StringBuilder parts = new StringBuilder();
        StringBuilder cushion = new StringBuilder();
        int cushionSamples = 0;
        while (rendered < sampleRate * seconds && !driver.stopped) {
            // the render loop has to run at the speed a sound card would take it at: FMP7 keeps
            // its own time against the rate its samples are taken
            long due = songStart == 0 ? 0 : songStart + songFrames * 1000L / sampleRate;
            long wait = due - System.currentTimeMillis();
            if (wait > 0) {
                Thread.sleep(wait);
            }
            driver.render(buffer, 0, buffer.length);
            if (songStart != 0) {
                songFrames += buffer.length / 2;
            } else {
                for (short v : buffer) {
                    if (v != 0) {
                        songStart = System.currentTimeMillis();
                        break;
                    }
                }
            }

            Fmp7Work work = fmp7.getWork();
            if (work != null && work.playing()) {
                sounded++;
                for (int p = 0; p < Fmp7Work.MAX_PART; p++) {
                    if (work.mode(p) != Fmp7Work.MODE_NONE && work.note(p) != Fmp7Work.REST) {
                        notes++;
                    }
                }
                if (sounded % 100 == 1) {
                    parts.setLength(0);
                    for (int p = 0; p < 16; p++) {
                        if (work.mode(p) == Fmp7Work.MODE_NONE) continue;
                        parts.append(" %s%d:%s".formatted(
                                work.mode(p) == Fmp7Work.MODE_FM ? "FM"
                                        : work.mode(p) == Fmp7Work.MODE_SSG ? "SG" : "PC",
                                work.partNo(p),
                                work.note(p) == Fmp7Work.REST ? "--" : String.valueOf(work.note(p))));
                    }
System.err.printf("%6.1fs %s%n", work.playMillis() / 1000, parts);
                }
            }

            ByteBuffer bytes = ByteBuffer.allocate(buffer.length * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (short s : buffer) {
                peak = Math.max(peak, Math.abs(s));
                bytes.putShort(s);
            }
            pcm.write(bytes.array());
            rendered += buffer.length / 2;

            // the cushion draining as the song plays is what "it gets slow and then chops" looks
            // like from here: the emulator is not keeping up, and this is by how much
            if (songStart != 0 && songFrames / (sampleRate / 2) > cushionSamples) {
                cushionSamples++;
                Fmp7Player p = fmp7.getPlayer();
                cushion.append(" %.1f".formatted(p == null ? 0 : p.getCushionSeconds()));
            }
        }

        Fmp7Player player = fmp7.getPlayer();
        if (player != null) {
System.err.println("player: " + player.getStatistics());
        }
        plugin.stop();
        plugin.close();

        byte[] b = pcm.toByteArray();
        Path out = Path.of("tmp/fmp7-driver.wav");
        Files.createDirectories(out.getParent());
        AudioFormat af = new AudioFormat(sampleRate, 16, 2, true, false);
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(b), af, b.length / 4),
                AudioFileFormat.Type.WAVE, out.toFile());
System.err.printf("%s: %.1fs of audio, peak %d -> %s%n", owi, rendered / (double) sampleRate, peak, out);
System.err.printf("silence before the music: %.1fs (booting the machine and loading FMP7)%n",
        (songStart - opened) / 1000.0);
System.err.println("cushion per half second:" + cushion);

        assertTrue(peak > 1000, owi + " rendered near silence, peak " + peak);
        assertTrue(driver.counter > 0, "the driver clock should have advanced");
        assertTrue(sounded > 0, "FMP7 never published a playing work");
        assertTrue(notes > 0, "FMP7 published a work but no part ever held a note");
    }

    /**
     * A song played to its own end, and then another - which is what a play list does when
     * nobody touches it.
     * <p>
     * It is a different case from being stopped part way through: a song that ends leaves FMP7
     * sitting there with its threads parked, and the machine has to be taken down from that
     * state before the next one can have one. `tritone.owi` runs for three and a half minutes,
     * so this is a slow test, but it is the case a play list spends its time in.
     */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void aSongThatEndsDoesNotSpoilTheNext() throws Exception {
        assumeTrue(Fmp7Player.isAvailable(),
                "no FMP7.exe, set -D" + Fmp7Player.FMP7_PATH_KEY + "=<dir>");
        Path ends = Path.of(System.getProperty("mdplayer.fmp7.test.ending", "tmp/fmp7/tritone.owi"));
        assumeTrue(Files.exists(ends), ends + " is missing");

        Setting.getInstance().getOutputDevice().setDeviceType(Common.DEV_Null);

        for (int song = 0; song < 2; song++) {
            Path file = song == 0 ? ends : owi;
            FileFormat format = FileFormat.getFileFormat(file.toString());
            format.load(new BufferedInputStream(Files.newInputStream(file)), null);

            @SuppressWarnings("unchecked")
            BasePlugin<? extends BaseDriver> plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
            plugin.setParams(format, Map.of("fileName", file.toString()));
            plugin.prepare();

            BaseDriver driver = plugin.getDriver();
            short[] buffer = new short[2048];
            int peak = 0;
            // the first song is left to end on its own; the second only has to be heard
            long deadline = System.currentTimeMillis() + (song == 0 ? 400_000 : 120_000);
            while (System.currentTimeMillis() < deadline && !driver.stopped
                    && (song == 0 || peak <= 1000)) {
                driver.render(buffer, 0, buffer.length);
                for (short s : buffer) {
                    peak = Math.max(peak, Math.abs(s));
                }
            }
System.err.printf("song %d (%s): peak %d, driver stopped=%b%n", song, file.getFileName(), peak, driver.stopped);
            assertTrue(peak > 1000, "song " + song + " (" + file + ") never made a sound");
            if (song == 0) {
                assertTrue(driver.stopped, "the song never ended by itself");
            }

            plugin.stop();
            plugin.close();
        }
    }

    /**
     * And again, which is what a play list does every time somebody presses next.
     * <p>
     * The emulated machine is a JVM-wide singleton and the win32 layer around it is a whole
     * system in statics, so the second song can only start once the first one's machine is
     * really gone - and it is stopped here part way through, which is the harder of the two
     * ways a song ends.
     */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void aSecondSongPlaysAfterTheFirst() throws Exception {
        assumeTrue(Fmp7Player.isAvailable(),
                "no FMP7.exe, set -D" + Fmp7Player.FMP7_PATH_KEY + "=<dir>");
        assumeTrue(Files.exists(owi), owi + " is missing");

        Setting.getInstance().getOutputDevice().setDeviceType(Common.DEV_Null);

        for (int song = 0; song < 2; song++) {
            FileFormat format = FileFormat.getFileFormat(owi.toString());
            format.load(new BufferedInputStream(Files.newInputStream(owi)), null);

            @SuppressWarnings("unchecked")
            BasePlugin<? extends BaseDriver> plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
            plugin.setParams(format, Map.of("fileName", owi.toString()));
            plugin.prepare();

            BaseDriver driver = plugin.getDriver();
            short[] buffer = new short[2048];
            int peak = 0;
            Fmp7Player p = ((Fmp7Driver) driver).getPlayer();
            long deadline = System.currentTimeMillis() + 90_000;
            while (peak <= 1000 && System.currentTimeMillis() < deadline && !driver.stopped) {
                driver.render(buffer, 0, buffer.length);
                for (short s : buffer) {
                    peak = Math.max(peak, Math.abs(s));
                }
            }
System.err.printf("song %d: peak %d, %s%n", song, peak, p == null ? "no player" : p.getStatistics());
            if (p != null && p.getFailure() != null) {
                p.getFailure().printStackTrace();
            }
            assertTrue(peak > 1000, "song " + song + " never made a sound");

            // stopped where it stands, mid song
            plugin.stop();
            plugin.close();
        }
    }

    /**
     * Plays it the way the player itself does - through {@link Audio#play()}, which is the loop
     * the application runs, fade-out and all - and asks only that it comes back. A song that
     * "freezes at its end" is that call never returning: the driver can stop the song perfectly
     * and the player still sit there, because ending it is the loop's job, not the driver's.
     */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void endsThroughThePlayersOwnLoop() throws Exception {
        assumeTrue(Fmp7Player.isAvailable(),
                "no FMP7.exe, set -D" + Fmp7Player.FMP7_PATH_KEY + "=<dir>");
        // it has to be a song that ends: one that loops would run until the loop count is up,
        // which is a different test and a much longer one
        Path ending = Path.of(System.getProperty("mdplayer.fmp7.test.ending", "tmp/fmp7/tritone.owi"));
        assumeTrue(Files.exists(ending), ending + " is missing, set -Dmdplayer.fmp7.test.ending");

        Setting.getInstance().getOutputDevice().setDeviceType(Common.DEV_Null);

        FileFormat format = FileFormat.getFileFormat(ending.toString());
        format.load(new BufferedInputStream(Files.newInputStream(ending)), null);
        @SuppressWarnings("unchecked")
        BasePlugin<? extends BaseDriver> plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", ending.toString()));

        Audio audio = Audio.getInstance();
        audio.init(plugin);

        CountDownLatch done = new CountDownLatch(1);
        ExecutorService es = Executors.newSingleThreadExecutor();
        long start = System.currentTimeMillis();
        es.submit(() -> {
            try {
                audio.play();
            } finally {
                done.countDown();
            }
        });

        // long enough for the song plus its fade; it is the not-returning that is the failure
        int seconds = Integer.getInteger("mdplayer.fmp7.test.ending.timeout", 420);
        boolean returned = done.await(seconds, TimeUnit.SECONDS);
        double took = (System.currentTimeMillis() - start) / 1000d;
System.err.printf("play() returned=%b after %.1fs, plugin stopped=%b%n", returned, took, plugin.isStopped());
        es.shutdownNow();
        audio.stop();
        audio.close();

        assertTrue(returned, "play() never came back: the song froze at its end");
    }
}
