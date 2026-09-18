package mdplayer;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import vavi.sound.visualizer.fmdsp.FftDataSource;
import vavi.sound.visualizer.fmdsp.FmDspVisualizer;
import vavi.sound.visualizer.fmdsp.RightMode;
import vavi.sound.visualizer.fmdsp.TrackId;
import vavi.sound.visualizer.fmdsp.TrackStatus;
import vavi.util.event.GenericEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


/**
 * scratch: an mfi through the fmdsp source, the rows and the analyzer bars as the gui would get them.
 */
class MldFmDspProbe {

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ai")
    void probe() throws Exception {
        String file = System.getProperty("probe.file", "../../vavi/vavi-sound/tmp/PoN_jar5/mld_1.mld");
        Setting.getInstance().getOutputDevice().setDeviceType(Common.DEV_Null);

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

        short[] buffer = new short[1024];
        BaseDriver d = plugin.getDriver();
        TrackStatus status = new TrackStatus();
        int[] bars = new int[FftDataSource.LENGTH];
        for (int i = 0; i < 44100 * 2 * 8 / buffer.length; i++) {
            d.render(buffer, 0, buffer.length);
            source.update(new GenericEvent(d, "master", buffer, 0));
            if (i % 40 != 0) continue;
            StringBuilder sb = new StringBuilder();
            for (TrackId row : TrackId.values()) {
                source.readStatus(row, status);
                if (!status.playing) continue;
                sb.append(row).append("[k=").append(Integer.toHexString(status.key)).append(",v=").append(status.volume).append("] ");
            }
            source.readFft(bars);
            int sum = 0, max = 0;
            for (int b : bars) { sum += b; max = Math.max(max, b); }
            StringBuilder lv = new StringBuilder();
            for (int c = 0; c < vavi.sound.visualizer.fmdsp.LevelDataSource.COUNT; c++) {
                if (source.track(c) != null) lv.append(source.label(c)).append('=').append(source.level(c)).append(' ');
            }
            System.err.printf("%.2fs levels: %s%n", (double) i * buffer.length / 2 / 44100, lv);
            System.err.printf("%.2fs fft sum=%d max=%d rows: %s%n", (double) i * buffer.length / 2 / 44100, sum, max, sb);
        }
        FmDspVisualizer vis = new FmDspVisualizer(60);
        vis.setSize(640, 400);
        vis.setDataSource(source);
        vis.setRightMode(RightMode.valueOf(System.getProperty("probe.right", "DEFAULT")));
        BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        // the palette fades in over its first frames, so one paint would come out black
        for (int i = 0; i < 130; i++) vis.paint(g);
        g.dispose();
        Path png = Path.of(System.getProperty("probe.out", "tmp/mld-frame.png"));
        Files.createDirectories(png.getParent());
        ImageIO.write(image, "png", png.toFile());
System.err.println("wrote " + png);

        plugin.stop();
    }
}
