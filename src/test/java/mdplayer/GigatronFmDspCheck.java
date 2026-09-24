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
import vavi.util.archive.Archives;
import vavi.util.event.GenericEvent;


/**
 * Paints the fmdsp of mml2vgm's Gigatron sample off-screen to tmp/zgm/fmdsp.png.
 */
class GigatronFmDspCheck {

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    @SuppressWarnings("unchecked")
    void paint() throws Exception {
        Path sample = Path.of("tmp/zgm/gigatronTest.zgm");
        Setting setting = Setting.getInstance();
        setting.getOutputDevice().setDeviceType(Common.DEV_Null);

        FileFormat format = FileFormat.getFileFormat(sample.toString());
        format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(sample))), sample.toString());
        var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", sample.toString(), "midiMode", 0, "songNo", 0));
        plugin.prepare();
        try {
            ChipFmDspSource source = new ChipFmDspSource();
            source.bind(plugin);
            FmDspVisualizer vis = new FmDspVisualizer(60);
            vis.setDataSource(source);
            vis.setSize(640, 400);

            BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            short[] buf = new short[2 * 441];
            double seconds = Double.parseDouble(System.getProperty("at", "1.6"));
            for (int t = 0; t < seconds * 100; t++) {
                plugin.getDriver().render(buf, 0, buf.length);
                source.update(new GenericEvent(this, "master", buf, 0));
                vis.paint(g); // the palette fades in a step per paint
            }
            ImageIO.write(image, "png", Path.of("tmp/zgm/fmdsp.png").toFile());
        } finally {
            try { plugin.stop(); } catch (Exception ignore) {}
            try { plugin.close(); } catch (Exception ignore) {}
            try { plugin.chipRegister.close(); } catch (Exception ignore) {}
        }
    }
}
