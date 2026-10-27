package mdplayer;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import mdplayer.driver.BaseDriver;
import mdplayer.driver.FileFormat;
import mdplayer.driver.BasePlugin;
import vavi.util.archive.Archives;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


/** scratch: does a muap song that fails to load its pcm ever stop? */
class MuapProbe {

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

        if (System.getProperty("probe.obj") != null) Files.write(Path.of(System.getProperty("probe.obj")), plugin.getData());
        var mdrv = (mdplayer.driver.muap.MuapDriver) plugin.getDriver();
        System.err.println("toneBuff=" + (mdrv.toneBuff == null ? null : mdrv.toneBuff.length) + " labelAdr=" + java.util.Arrays.toString(mdrv.labelAdr));
        var cc = new muap.compiler.Compiler();
        var ret = cc.compile(Files.newInputStream(Path.of(file)), x -> null);
        System.err.println("args=" + (ret == null ? "null ret" : ret[0].args == null ? null : ret[0].args.stream().map(a -> a == null ? "null" : a.getClass().getSimpleName()).toList()));
        if (System.getProperty("probe.stopAfterCompile") != null) return;
        short[] buffer = new short[1024];
        BaseDriver d = plugin.getDriver();
        int seconds = Integer.getInteger("probe.seconds", 20);
        var out = new java.io.DataOutputStream(new java.io.BufferedOutputStream(Files.newOutputStream(Path.of(System.getProperty("probe.raw", "/dev/null")))));
        for (int i = 0; i < 44100 * seconds / (buffer.length / 2); i++) {
            d.render(buffer, 0, buffer.length);
            for (short s : buffer) out.writeShort(s);
            if (i % 43 != 0) continue;
            double sum = 0;
            for (short s : buffer) sum += (double) s * s;
            var chip = plugin.chipRegister.chip(mdplayer.chips.Ym2608Chip.class);
            var f = mdplayer.chips.Ym2608Chip.class.getDeclaredField("register");
            f.setAccessible(true);
            int[][][] reg = (int[][][]) f.get(chip);
            StringBuilder sb = new StringBuilder();
            for (int p = 0; p < 2; p++) for (int c = 0; c < 3; c++) sb.append("%x%02x ".formatted(reg[0][p][0xa4 + c] & 0x3f, reg[0][p][0xa0 + c]));
            sb.append("ssg ");
            for (int c = 0; c < 3; c++) sb.append("%x%02x/%x ".formatted(reg[0][0][c * 2 + 1], reg[0][0][c * 2], reg[0][0][8 + c]));
            sb.append("mix %02x adpcm %02x".formatted(reg[0][0][7], reg[0][1][0]));
            var md = mdplayer.driver.muap.MuapDriver.class.getDeclaredField("muapDriver");
            md.setAccessible(true);
            var work = ((muap.driver.Driver) md.get(d)).work;
            var t = work.timerOPNA1;
            sb.append(" | cur=%d tc=%d".formatted(work.currentTimer, work.timeCounter));
            for (String n : new String[] {"timerA", "timerB", "timerReg", "statReg"}) {
                var ff = t.getClass().getSuperclass().getDeclaredField(n);
                ff.setAccessible(true);
                sb.append(" %s=%s".formatted(n, ff.get(t)));
            }
            sb.append(" 8253=").append(work._8253timer.getCh0Stat());
            var nf = muap.driver.Driver.class.getDeclaredField("nax");
            nf.setAccessible(true);
            Object nax = nf.get(md.get(d));
            var pf = nax.getClass().getDeclaredField("play4");
            pf.setAccessible(true);
            var play4 = (muap.driver.Play4) pf.get(nax);
            var lp = play4.getClass().getDeclaredField("labelPtr");
            lp.setAccessible(true);
            int[] labelPtr = (int[]) lp.get(play4);
            sb.append(" labels=");
            for (int k = 0; k < 17 * 40; k++) {
                if (play4.labelPassCnt[k] >= 0 || (labelPtr != null && labelPtr[k] != 0 && i == 0))
                    sb.append("%d.%d:%04x/%d ".formatted(k / 40, k % 40, labelPtr == null ? -1 : labelPtr[k], play4.labelPassCnt[k]));
            }
            if (labelPtr == null) sb.append("null");
            System.err.printf("%.1fs rms=%.0f driverStopped=%s pluginStopped=%s loop=%d %s%n",
                    (double) i * buffer.length / 2 / 44100, Math.sqrt(sum / buffer.length), d.stopped, plugin.stopped, d.curLoop, sb);
        }
        out.close();
    }
}
