package mdplayer.driver.mxdrv;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import mdplayer.Setting;
import mdplayer.lib.mxdrv.MXDRV;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;
import vavi.util.archive.Archives;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


/** temporary probe */
class ZanacProbe {

    static {
        System.setProperty("mdplayer.variant.pcm8", "-1");
        System.setProperty("mdplayer.variant.ym2151", "1");
    }

    static final String file = System.getProperty("zanac",
            "/Users/nsano/Public/np2/MXDRV/Game/Irem/ガーディック外伝/Waiendee/ALG04.MDX");

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ai")
    void measure() throws Exception {
        for (String f : System.getProperty("files", "").split(",")) {
            if (f.isEmpty()) continue;
            try {
                FileFormat format = FileFormat.getFileFormat(f);
                format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(Path.of(f)))), null);
                var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
                plugin.setParams(format, Map.of("fileName", f, "midiMode", 0, "songNo", 0));
                plugin.prepare();
                BaseDriver d = plugin.getDriver();
                int rate = Setting.getInstance().getOutputDevice().getSampleRate();
                System.err.printf("XXX %7.1f sec  %s%n", d.totalCounter / (double) rate, Path.of(f).getFileName());
            } catch (Exception e) {
                System.err.printf("XXX  ERROR      %s: %s%n", Path.of(f).getFileName(), e);
            }
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ai")
    void probe() throws Exception {
        FileFormat format = FileFormat.getFileFormat(file);
        format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(Path.of(file)))), null);
        var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", file, "midiMode", 0, "songNo", 0));
        plugin.prepare();

        MxDriver driver = (MxDriver) plugin.getDriver();
        int sampleRate = Setting.getInstance().getOutputDevice().getSampleRate();
        System.err.printf("XXX totalCounter: %d (%.1f sec) loopTimes=%d useLoopTimes=%s%n",
                driver.totalCounter, driver.totalCounter / (double) sampleRate,
                Setting.getInstance().getOther().getLoopTimes(), Setting.getInstance().getOther().getUseLoopTimes());

        MXDRV mx = driver.getMxdrv();
        var mm = mx.getMemory();
        int G = (int) mx.MXDRV_GetWork(MXDRV.MXDRV_WORK.GLOBAL);

        short[] buf = new short[512];
        long samples = 0;
        long next = 0;
        int seconds = Integer.getInteger("seconds", 600);
        while (samples < (long) sampleRate * seconds) {
            int r = driver.render(buf, 0, buf.length);
            if (r < 0) { System.err.println("XXX render < 0 at " + samples / (double) sampleRate); break; }
            samples += r / 2;
            if (samples >= next) {
                System.err.printf("XXX %6.1f sec: curLoop=%d stopped=%s terminate=%s | L002246=%04x L001e1a=%04x L001e06=%04x L001e13=%02x L001e18=%02x playtime=%d%n",
                        samples / (double) sampleRate, driver.curLoop, driver.stopped, mx.terminatePlay,
                        mm.readShort(G + MXDRV.MXWORK_GLOBAL.L002246) & 0xffff,
                        mm.readShort(G + MXDRV.MXWORK_GLOBAL.L001e1a) & 0xffff,
                        mm.readShort(G + MXDRV.MXWORK_GLOBAL.L001e06) & 0xffff,
                        mm.readByte(G + MXDRV.MXWORK_GLOBAL.L001e13) & 0xff,
                        mm.readByte(G + MXDRV.MXWORK_GLOBAL.L001e18) & 0xff,
                        mm.readInt(G + MXDRV.MXWORK_GLOBAL.PLAYTIME));
                next += sampleRate * 5L;
            }
            if (driver.stopped) { System.err.printf("XXX STOPPED at %.1f sec%n", samples / (double) sampleRate); break; }
        }
        System.err.printf("XXX end: %.1f sec curLoop=%d stopped=%s%n", samples / (double) sampleRate, driver.curLoop, driver.stopped);
    }
}
