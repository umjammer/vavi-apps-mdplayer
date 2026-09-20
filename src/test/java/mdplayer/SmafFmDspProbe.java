/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.imageio.ImageIO;

import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;
import mdplayer.driver.smaf.SmafDriver;
import mdplayer.driver.smaf.SmafPlugin;
import mdplayer.lib.smaf.MmfToolPlayer;
import mdplayer.lib.smaf.SmafScore;
import mdplayer.lib.smaf.SmafTelemetry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.sound.visualizer.fmdsp.FmDspVisualizer;
import vavi.sound.visualizer.fmdsp.RightMode;
import vavi.sound.visualizer.fmdsp.TrackId;
import vavi.sound.visualizer.fmdsp.TrackStatus;

import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * scratch: plays a .mmf and watches the fmdsp rows fill from what the emulated player says it is
 * playing, then renders one frame of it off screen.
 * <p>
 * The thing worth looking at here is not the picture but the line before it: where the display
 * thinks the song is when the first sound comes out of the mixer. That is the whole of the
 * synchronisation - a score in one hand and an audio clock in the other - and it is measured
 * against the score's own first note, which is the one moment the two can be compared without
 * anything to trust in between.
 * <p>
 * Run with {@code -Dvavi.test=ai}, optionally {@code -Dprobe.file=...mmf},
 * {@code -Dprobe.seconds=20} and {@code -Dprobe.out=...png}.
 */
class SmafFmDspProbe {

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ai")
    void render() throws Exception {
        String file = System.getProperty("probe.file",
                Path.of(MmfToolPlayer.toolDirectory().getPath(), "test.mmf").toString());
        int seconds = Integer.getInteger("probe.seconds", 20);
        String out = System.getProperty("probe.out", "tmp/smaf-frame.png");

        // SmafPlugin builds SmafDriver2 now; this probe is the emulated player's, see the readme
        assumeTrue(SmafPlugin.DRIVER == SmafDriver.class, "SmafPlugin builds " + SmafPlugin.DRIVER.getSimpleName());

        Setting setting = Setting.getInstance();
        setting.getOutputDevice().setDeviceType(Common.DEV_Null);
        int sampleRate = setting.getOutputDevice().getSampleRate();

        FileFormat format = FileFormat.getFileFormat(file);
        format.load(new BufferedInputStream(Files.newInputStream(Path.of(file))), null);
        @SuppressWarnings("unchecked")
        var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", file));
        plugin.prepare();
        plugin.stopped = false;
        plugin.paused = false;

        ChipFmDspSource source = new ChipFmDspSource();
        source.bind(plugin);
        source.setFilename(Path.of(file).getFileName().toString());

        SmafDriver driver = (SmafDriver) plugin.getDriver();
        SmafTelemetry telemetry = driver.getPlayer().getTelemetry();
        SmafScore score = telemetry.getScore();

        short[] buffer = new short[1024];
        // Where the display had the song when the mixer first made a sound, which is what says
        // whether the two are in step. Twice: a sample that is merely not zero is not music -
        // the dll puts a level on its output as soon as it opens the device - and the note the
        // score starts with is the first thing that can be heard.
        double firstSampleMillis = Double.NaN;
        double firstLoudMillis = Double.NaN;
        int rendered = 0;
        int rows = 0;
        // rendering flat out is the file rendering case, where the emulator is what holds the
        // loop up; paced is what a sound card does, and the emulator then runs ahead of it into
        // its cushion. The two put the beacons in very different places, so both are worth a run
        boolean paced = Boolean.getBoolean("probe.paced");
        long started = System.currentTimeMillis();
        while (rendered < sampleRate * seconds && !driver.stopped) {
            if (paced) {
                long wait = started + rendered * 1000L / sampleRate - System.currentTimeMillis();
                if (wait > 0) Thread.sleep(wait);
            }
            double millis = driver.getSongMillis();
            driver.render(buffer, 0, buffer.length);
            rendered += buffer.length / 2;
            source.snapshot();
            if (!Double.isNaN(millis)) {
                for (short v : buffer) {
                    if (v != 0 && Double.isNaN(firstSampleMillis)) firstSampleMillis = millis;
                    if (Math.abs(v) > LOUD && Double.isNaN(firstLoudMillis)) firstLoudMillis = millis;
                }
            }
            rows = Math.max(rows, sounding(source));
        }

        int firstNote = score.firstNoteMillis();

System.err.printf("score: %d notes, %d controls, %.1fs long, format %d%n",
        score.noteCount(), score.controlCount(), score.lengthMillis() / 1000.0, score.getFormat());
System.err.printf("sync: song starts at output frame %.0f (%.2fs in)%n",
        telemetry.getAnchorFrames(), telemetry.getAnchorFrames() / 48000.0);
System.err.printf("sync: player said it started with %d frames produced, %d frames dropped as"
        + " silence%n", telemetry.getStartedAt(), telemetry.getDroppedFrames());
System.err.printf("sync: mixer's first sample at song %.0fms, first one over %d at song %.0fms,"
        + " score's first note at %dms -> %.0fms out%n",
        firstSampleMillis, LOUD, firstLoudMillis, firstNote, firstLoudMillis - firstNote);
System.err.printf("audio: %.2fs rendered, driver stopped=%b (the score is %.2fs long, and %.2fs"
        + " of it after its first note)%n", rendered / (double) sampleRate, driver.stopped,
        score.lengthMillis() / 1000.0, (score.lengthMillis() - firstNote) / 1000.0);
System.err.printf("rows that sounded: %d%n", rows);

        TrackStatus status = new TrackStatus();
        for (TrackId row : TrackId.values()) {
            source.readStatus(row, status);
            if (!status.playing) continue;
System.err.println(row + " type=" + source.trackTypeName(row) + " num=" + source.trackNumber(row));
        }

        FmDspVisualizer vis = new FmDspVisualizer(60);
        vis.setSize(640, 400);
        vis.setDataSource(source);
        vis.setRightMode(RightMode.valueOf(System.getProperty("probe.right", "TRACK_INFO")));

        BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        // the palette fades in over its first frames, so one paint would come out black
        for (int i = 0; i < 130; i++) vis.paint(g);
        g.dispose();

        Path png = Path.of(out);
        if (png.getParent() != null) Files.createDirectories(png.getParent());
        ImageIO.write(image, "png", png.toFile());
System.err.println("wrote " + png);

        plugin.stop();
        plugin.close();
    }

    /** over this a sample is music rather than the level the dll sits at */
    private static final int LOUD = 1000;

    private static int sounding(ChipFmDspSource source) {
        TrackStatus status = new TrackStatus();
        int n = 0;
        for (TrackId row : TrackId.values()) {
            source.readStatus(row, status);
            if (status.playing) n++;
        }
        return n;
    }
}
