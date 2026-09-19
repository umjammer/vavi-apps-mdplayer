package mdplayer;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import mdplayer.chips.YmZ280BChip;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.FileFormat;
import mdplayer.driver.BasePlugin;
import vavi.util.archive.Archives;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


/** scratch: what are a YMZ280B song's channels actually doing over time? */
class Probe {

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
        plugin.prepare();
        plugin.stopped = false;
        plugin.paused = false;
        plugin.fadeout = false;

        short[] buffer = new short[1024];
        BaseDriver d = plugin.getDriver();
        int seconds = Integer.getInteger("probe.seconds", 20);
        String last = "";
        for (int i = 0; i < 44100 * seconds / buffer.length; i++) {
            d.render(buffer, 0, buffer.length);
            if (i % 10 != 0) continue;
            var info = plugin.chipRegister.chip(YmZ280BChip.class).getInfo(0);
            StringBuilder sb = new StringBuilder();
            for (int ch = 0; ch < 8; ch++) {
                Object playing = info.get("channels." + ch + ".playing");
                if (!(playing instanceof Boolean b) || !b) continue;
                sb.append(ch).append(":f=").append(info.get("channels." + ch + ".frequency"))
                        .append(",l=").append(info.get("channels." + ch + ".level")).append("  ");
            }
            String now = sb.toString();
            if (!now.equals(last)) {
                System.err.printf("%.2fs %s%n", (double) i * buffer.length / 44100, now);
                last = now;
            }
        }
    }
}
