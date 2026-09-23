package mdplayer.driver.mxdrv;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.FileFormat;
import mdplayer.driver.BasePlugin;
import mdsound.MDSound.Chip;
import vavi.util.archive.Archives;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


/** temporary probe */
class MxProbe {

    static {
        System.setProperty("mdplayer.variant.pcm8", "-1");
        System.setProperty("mdplayer.variant.ym2151", "1");
        System.setProperty("mdplayer.variant.mpcm", "0");
        System.setProperty("mdplayer.variant.ay8910", "1");
        System.setProperty("mdplayer.variant.ym2413", "0");
        System.setProperty("mdplayer.variant.ymf262", "2");
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ai")
    void probes() throws Exception {
        for (String file : new String[] {"tmp/mdx/MHAWK/MHAWKN1.MDX", "tmp/mdx/sorc/SE313S.mdx"}) probe(file);
    }

    private void probe(String file) throws Exception {

        FileFormat format = FileFormat.getFileFormat(file);
        format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(Path.of(file)))), null);
        var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", file, "midiMode", 0, "songNo", 0));
        plugin.prepare();

        MxDriver driver = (MxDriver) plugin.getDriver();
        int sampleRate = Setting.getInstance().getOutputDevice().getSampleRate();

        java.lang.reflect.Field f = mdsound.MDSound.class.getDeclaredField("chips");
        f.setAccessible(true);
        List<Chip> cs = (List<mdsound.MDSound.Chip>) f.get(plugin.mds);
        for (mdsound.MDSound.Chip c : cs) {
            System.err.printf("XXX chip: %s id=%d rate=%d resampler=%d vol=%d%n", c.instrument.getClass().getSimpleName(), c.id, c.samplingRate, c.resampler, c.volume);
            if (c.instrument instanceof mdsound.instrument.X68kYm2151Inst x) {
                java.lang.reflect.Field cf = x.getClass().getDeclaredField("clocks");
                cf.setAccessible(true);
                Object[] clocks = (Object[]) cf.get(x);
                System.err.println("XXX   clocks: " + Arrays.toString(clocks));
            }
        }
        System.err.printf("XXX totalCounter: %d (%.1f sec)%n", driver.totalCounter, driver.totalCounter / (double) sampleRate);

        short[] buf = new short[4];
        long samples = 0;
        long next = 0;
        while (samples < (long) sampleRate * 60 * 12) {
            int r = driver.render(buf, 0, buf.length);
            if (r < 0) { System.err.println("XXX render < 0 at " + samples / (double) sampleRate); break; }
            samples += r / 2;
            if (samples >= next) {
                System.err.printf("XXX %.1f sec: stopped=%s curLoop=%d counter=%d%n", samples / (double) sampleRate, driver.stopped, driver.curLoop, driver.counter);
                next += sampleRate * 10L;
            }
            if (driver.stopped) { System.err.printf("XXX STOPPED (end of song) at %.1f sec%n", samples / (double) sampleRate); break; }
            if (Setting.getInstance().getOther().getUseLoopTimes()
                    && driver.curLoop > Setting.getInstance().getOther().getLoopTimes() - 1) {
                System.err.printf("XXX LOOP LIMIT (%d) reached at %.1f sec%n", Setting.getInstance().getOther().getLoopTimes(), samples / (double) sampleRate);
                break;
            }
        }
        System.err.println("XXX [" + file + "] end: %.1f sec stopped=%s curLoop=%d".formatted(samples / (double) sampleRate, driver.stopped, driver.curLoop));
    }
}
