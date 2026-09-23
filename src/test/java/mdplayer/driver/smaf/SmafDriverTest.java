/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.smaf;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;
import mdplayer.lib.smaf.MmfToolPlayer;
import mdplayer.lib.smaf.SmafFile;
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
 * Plays a .mmf the way the player does - through the format, the plugin and the driver - which
 * means booting an emulated PC and running mmftoolc.exe on it, so this is not a quick test.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-08-10 nsano initial version <br>
 */
class SmafDriverTest {

    /** the mmftool distribution carries one, so there is always something to play */
    static final Path mmf = Path.of(MmfToolPlayer.toolDirectory().getPath(), "test.mmf");

    @Test
    void readsWhatTheFileSaysAboutItself() throws Exception {
        assumeTrue(Files.exists(mmf), mmf + " is missing, set -D" + MmfToolPlayer.MMFTOOL_PATH_KEY);

        SmafFile file = SmafFile.decode(Files.readAllBytes(mmf));
        assertEquals(SmafFile.Format.MA2, file.getFormat());
        assertEquals("Love Motion", file.getTitle());

        MetaData md = new SmafDriver().retrieveMetaData(Files.readAllBytes(mmf));
        assertNotNull(md);
        assertEquals("Love Motion", md.getFirst(Tag.Title));
        assertEquals("MA-2", md.getFirst(Tag.Chip));
    }

    @Test
    void theFormatIsFoundByItsHeader() throws Exception {
        assumeTrue(Files.exists(mmf), mmf + " is missing");

        try (BufferedInputStream is = new BufferedInputStream(Files.newInputStream(mmf))) {
            assertInstanceOf(SmafFileFormat.class, FileFormat.getFileFormat(is));
        }
        assertInstanceOf(SmafFileFormat.class, FileFormat.getFileFormat("song.mmf"));
    }

    @Test
    void playsThroughTheDriver() throws Exception {
        assumeTrue(SmafPlugin.DRIVER == SmafDriver.class, "SmafPlugin builds " + SmafPlugin.DRIVER.getSimpleName());
        assumeTrue(MmfToolPlayer.isAvailable(),
                "no mmftoolc.exe, set -D" + MmfToolPlayer.MMFTOOL_PATH_KEY + "=<dir>");
        assumeTrue(Files.exists(mmf), mmf + " is missing");

        String filename = System.getProperty("mdplayer.smaf.test.mmf", mmf.toString());

        Setting setting = Setting.getInstance();
        setting.getOutputDevice().setDeviceType(Common.DEV_Null);
        int sampleRate = setting.getOutputDevice().getSampleRate();

        FileFormat format = FileFormat.getFileFormat(filename);
        format.load(new BufferedInputStream(Files.newInputStream(Path.of(filename))), null);

        @SuppressWarnings("unchecked")
        BasePlugin<? extends BaseDriver> plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", filename));
        // the listener's clock starts here: prepare boots the emulated machine
        long opened = System.currentTimeMillis();
        plugin.prepare();

        BaseDriver driver = plugin.getDriver();
        assertInstanceOf(SmafDriver.class, driver);
        assertNotNull(driver.metaData);

        System.setProperty("java.util.logging.ConsoleHandler.level", "ALL");
        // the sample song is 18 seconds, so this is a cap rather than a length
        int seconds = Integer.getInteger("mdplayer.smaf.test.seconds", 60);
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        short[] buffer = new short[2048];
        int peak = 0;
        int rendered = 0;
        long start = System.currentTimeMillis();
        // what a sound card would have gone without: every time the samples for a moment were
        // not ready until after the moment had passed, the device would have been playing
        // nothing. The rendered samples are continuous either way, so this - not a gap in the
        // wav - is what a chopped start looks like from here.
        int late = 0;
        long lateMillis = 0;
        long worstLate = 0;
        double worstLateAt = 0;
        // the clock starts when the song does: the emulated player spends its first seconds
        // loading, and nothing is due to be heard until it has finished
        long songStart = 0;
        long songFrames = 0;
        int cushionSamples = 0;
        StringBuilder cushion = new StringBuilder();
        StringBuilder busy = new StringBuilder();
        long lastCpu = 0, lastWall = 0;
        while (rendered < sampleRate * seconds && !driver.stopped) {
            // the render loop has to run at the speed a sound card would take it at: the
            // emulated player keeps its own time against the rate its samples are taken
            long due = songStart == 0 ? 0 : songStart + songFrames * 1000L / sampleRate;
            long wait = due - System.currentTimeMillis();
            if (wait > 0) {
                Thread.sleep(wait);
            }
            driver.render(buffer, 0, buffer.length);
            if (songStart != 0) {
                songFrames += buffer.length / 2;
                long behind = System.currentTimeMillis() - due;
                if (behind > 20) {
                    late++;
                    lateMillis += behind;
                    if (behind > worstLate) {
                        worstLate = behind;
                        worstLateAt = songFrames / (double) sampleRate;
                    }
                }
            } else {
                for (short v : buffer) {
                    if (v != 0) {
                        songStart = System.currentTimeMillis();
                        break;
                    }
                }
            }
            // the cushion draining as the song plays is what "it gets slow and then chops"
            // looks like from here: the emulator is not keeping up, and this is by how much
            if (songStart != 0 && songFrames / (sampleRate / 2) > cushionSamples) {
                cushionSamples++;
                MmfToolPlayer p = ((SmafDriver) driver).getPlayer();
                cushion.append(" %.1f".formatted(p == null ? 0 : p.getCushionSeconds()));
                // and what the emulator thread is getting out of the host while that happens.
                // A thread that is short of cushion but not using a whole core is being held up
                // by something, not out of cpu - which is a different problem with a different fix
                long nowCpu = emulatorCpuNanos();
                long nowWall = System.nanoTime();
                if (lastCpu != 0) {
                    busy.append(" %.2f".formatted((nowCpu - lastCpu) / (double) (nowWall - lastWall)));
                }
                lastCpu = nowCpu;
                lastWall = nowWall;
            }
            ByteBuffer bytes = ByteBuffer.allocate(buffer.length * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (short s : buffer) {
                peak = Math.max(peak, Math.abs(s));
                bytes.putShort(s);
            }
            pcm.write(bytes.array());
            rendered += buffer.length / 2;
        }
        long elapsed = System.currentTimeMillis() - start;

        MmfToolPlayer player = ((SmafDriver) driver).getPlayer();
        if (player != null) {
System.err.println("player: " + player.getStatistics());
        }

        plugin.stop();
        plugin.close();

        byte[] b = pcm.toByteArray();
        Path out = Path.of("tmp/smaf-driver.wav");
        Files.createDirectories(out.getParent());
        AudioFormat af = new AudioFormat(sampleRate, 16, 2, true, false);
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(b), af, b.length / 4),
                AudioFileFormat.Type.WAVE, out.toFile());
System.err.printf("%s: %.1fs of audio in %.1fs, peak %d -> %s%n",
        filename, rendered / (double) sampleRate, elapsed / 1000.0, peak, out);
System.err.printf("silence before the music: %.1fs (booting the machine and building the cushion)%n",
        (songStart - opened) / 1000.0);
System.err.printf("late deliveries: %d, %.2fs behind in total, worst %dms at %.1fs into the song%n",
        late, lateMillis / 1000.0, worstLate, worstLateAt);
System.err.println("cushion per half second:" + cushion);
System.err.println("emulator thread, cores used:" + busy);

        assertTrue(peak > 1000, filename + " rendered near silence, peak " + peak);
        assertTrue(driver.counter > 0, "the driver clock should have advanced");
        assertTrue(driver.stopped, "the song should have ended by itself");
        // the sample song is 18 seconds under wine; the driver should be in the same country
        assertTrue(rendered > sampleRate * 10, "only " + rendered / sampleRate + "s came out");
    }

    /**
     * The emulated machine is a JVM-wide singleton, so the second song of a play list can only
     * start once the first one's machine is really gone.
     * <p>
     * The first song is left to end on its own here, because that is the case that works: a
     * machine stopped part way through a song leaves the win32 layer - a whole Win32 system in
     * statics, which was never written to be torn down and set up again - in a state where the
     * next machine's program runs but never gets its sound out. See driver/readme.md.
     */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void aSecondSongCanBePlayedAfterTheFirst() throws Exception {
        assumeTrue(SmafPlugin.DRIVER == SmafDriver.class, "SmafPlugin builds " + SmafPlugin.DRIVER.getSimpleName());
        assumeTrue(MmfToolPlayer.isAvailable(),
                "no mmftoolc.exe, set -D" + MmfToolPlayer.MMFTOOL_PATH_KEY + "=<dir>");
        assumeTrue(Files.exists(mmf), mmf + " is missing");

        Setting.getInstance().getOutputDevice().setDeviceType(Common.DEV_Null);

        for (int song = 0; song < 2; song++) {
            FileFormat format = FileFormat.getFileFormat(mmf.toString());
            format.load(new BufferedInputStream(Files.newInputStream(mmf)), null);

            @SuppressWarnings("unchecked")
            BasePlugin<? extends BaseDriver> plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
            plugin.setParams(format, Map.of("fileName", mmf.toString()));
            plugin.prepare();

            BaseDriver driver = plugin.getDriver();
            short[] buffer = new short[2048];
            int peak = 0;
            // the whole of the first song, so its machine ends on its own terms; enough of the
            // second to hear that it started
            long deadline = System.currentTimeMillis() + 120_000;
            while (System.currentTimeMillis() < deadline && !driver.stopped
                    && (song == 0 || peak <= 1000)) {
                driver.render(buffer, 0, buffer.length);
                for (short s : buffer) {
                    peak = Math.max(peak, Math.abs(s));
                }
            }
            assertTrue(peak > 1000, "song " + song + " never made a sound");

            plugin.stop();
            plugin.close();
        }
    }

    /**
     * And the same when the first song is cut off part way through, which is what a play list
     * does every time somebody presses next.
     */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void aSongStoppedPartWayThroughDoesNotSpoilTheNext() throws Exception {
        assumeTrue(SmafPlugin.DRIVER == SmafDriver.class, "SmafPlugin builds " + SmafPlugin.DRIVER.getSimpleName());
        assumeTrue(MmfToolPlayer.isAvailable(),
                "no mmftoolc.exe, set -D" + MmfToolPlayer.MMFTOOL_PATH_KEY + "=<dir>");
        assumeTrue(Files.exists(mmf), mmf + " is missing");

        Setting.getInstance().getOutputDevice().setDeviceType(Common.DEV_Null);

        for (int song = 0; song < 2; song++) {
            FileFormat format = FileFormat.getFileFormat(mmf.toString());
            format.load(new BufferedInputStream(Files.newInputStream(mmf)), null);

            @SuppressWarnings("unchecked")
            BasePlugin<? extends BaseDriver> plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
            plugin.setParams(format, Map.of("fileName", mmf.toString()));
            plugin.prepare();

            BaseDriver driver = plugin.getDriver();
            short[] buffer = new short[2048];
            int peak = 0;
            long deadline = System.currentTimeMillis() + 90_000;
            while (peak <= 1000 && System.currentTimeMillis() < deadline && !driver.stopped) {
                driver.render(buffer, 0, buffer.length);
                for (short s : buffer) {
                    peak = Math.max(peak, Math.abs(s));
                }
            }
            assertTrue(peak > 1000, "song " + song + " never made a sound");

            // stopped where it stands, mid song
            plugin.stop();
            plugin.close();
        }
    }

    /** cpu time burned by the thread jdosbox runs its machine on, or 0 if it is not there */
    private static long emulatorCpuNanos() {
        java.lang.management.ThreadMXBean mx = java.lang.management.ManagementFactory.getThreadMXBean();
        for (long id : mx.getAllThreadIds()) {
            java.lang.management.ThreadInfo info = mx.getThreadInfo(id);
            if (info != null && "jdosbox".equals(info.getThreadName())) {
                return mx.getThreadCpuTime(id);
            }
        }
        return 0;
    }
}
