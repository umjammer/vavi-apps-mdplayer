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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.sound.visualizer.fmdsp.FmDspVisualizer;
import vavi.sound.visualizer.fmdsp.RightMode;
import vavi.sound.visualizer.fmdsp.TrackId;
import vavi.sound.visualizer.fmdsp.TrackStatus;


/**
 * scratch: plays a .mmf on {@link mdplayer.driver.smaf.SmafDriver2} and renders one frame of the
 * fmdsp off screen, to look at what {@link mdplayer.fmdsp.Smaf2Reader} puts on the rows.
 * <p>
 * Run with {@code -Dvavi.test=ai}, optionally {@code -Dprobe.file=...mmf},
 * {@code -Dprobe.seconds=10} and {@code -Dprobe.out=...png}.
 */
class Smaf2FmDspProbe {

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ai")
    void render() throws Exception {
        String file = System.getProperty("probe.file",
                System.getProperty("user.home") + "/Public/np2/mmf/ma3/sor51ma3.mmf");
        int seconds = Integer.getInteger("probe.seconds", 10);
        String out = System.getProperty("probe.out", "tmp/smaf2-frame.png");

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
        plugin.fadeout = false;

        ChipFmDspSource source = new ChipFmDspSource();
        source.bind(plugin);
        source.setFilename(Path.of(file).getFileName().toString());

        BaseDriver driver = plugin.getDriver();
        short[] buffer = new short[1024];
        int rendered = 0;
        while (rendered < sampleRate * seconds && !driver.stopped) {
            driver.render(buffer, 0, buffer.length);
            rendered += buffer.length / 2;
            source.snapshot();
        }

        TrackStatus status = new TrackStatus();
        for (TrackId row : TrackId.values()) {
            source.readStatus(row, status);
            if (!status.playing) continue;
System.err.println(row + " type=" + source.trackTypeName(row) + " num=" + source.trackNumber(row)
        + " key=" + status.key);
        }
System.err.println("chips: " + source.chips());

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
}
