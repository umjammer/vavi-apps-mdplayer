package mdplayer;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;
import vavi.sound.visualizer.fmdsp.LevelDataSource;
import vavi.sound.visualizer.fmdsp.TrackId;
import vavi.sound.visualizer.fmdsp.TrackStatus;
import vavi.util.archive.Archives;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


/**
 * scratch: what does the fmdsp source put on its rows for a given song? Every row a reader claimed,
 * with the key, volume, pan and meter the display would show, printed whenever any of it changes.
 */
class FmDspRowProbe {

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ai")
    void probe() throws Exception {
        LocalProperties.bind();
        String file = System.getProperty("probe.file");

        FileFormat format = FileFormat.getFileFormat(file);
        format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(Path.of(file)))), null);
        @SuppressWarnings("unchecked")
        var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", file));
        Integer songNo = Integer.getInteger("probe.songNo");
        if (songNo != null && plugin instanceof BasePlugin.HasSongNo hasSongNo) hasSongNo.setSongNo(songNo);
        plugin.prepare();
        plugin.stopped = false;
        plugin.paused = false;
        plugin.fadeout = false;

        ChipFmDspSource source = new ChipFmDspSource();
        source.bind(plugin);

        short[] buffer = new short[1024];
        BaseDriver d = plugin.getDriver();
        int seconds = Integer.getInteger("probe.seconds", 10);
        TrackStatus status = new TrackStatus();
        String last = "";
        for (int i = 0; i < 44100 * seconds / buffer.length; i++) {
            d.render(buffer, 0, buffer.length);
            if (i % 10 != 0) continue;
            source.snapshot();
            StringBuilder sb = new StringBuilder();
            for (TrackId row : TrackId.values()) {
                source.readStatus(row, status);
                if (!status.playing) continue;
                int meter = meterOf(source, row);
                sb.append(row).append('/').append(meter < 0 ? "-" : source.label(meter))
                        .append("[k=").append(Integer.toHexString(status.key))
                        .append(",v=").append(status.volume)
                        .append(',').append(meter < 0 ? "-" : source.pan(meter))
                        .append(",l=").append(meter < 0 ? -1 : source.level(meter))
                        .append("] ");
            }
            String now = sb.toString();
            if (!now.equals(last)) {
                System.err.printf("%.2fs %s%n", (double) i * buffer.length / 44100, now);
                last = now;
            }
        }
    }

    /** the level meter column this row feeds, -1 when it has none */
    private static int meterOf(ChipFmDspSource source, TrackId row) {
        for (int c = 0; c < LevelDataSource.COUNT; c++) {
            if (source.track(c) == row) return c;
        }
        return -1;
    }
}
