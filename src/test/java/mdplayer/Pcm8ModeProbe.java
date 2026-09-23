/*
 * scratch probe: which PCM8 frequency/format codes an MDX sends
 */

package mdplayer;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;
import mdplayer.lib.mxdrv.MXDRV;
import vavi.util.archive.Archives;

import org.junit.jupiter.api.Test;


class Pcm8ModeProbe {

    @Test
    void probe() throws Exception {
        LocalProperties.bind();
        System.setProperty("mdplayer.variant.pcm8", System.getProperty("pcm8", "-1"));
        for (String f : System.getProperty("mdx.files",
                "/Users/nsano/Public/np2/x68k/x68000mdx/Arrange/RUNUPC.MDX").split(",")) {
            probe(Path.of(f));
        }
    }

    void probe(Path path) throws Exception {
        FileFormat format = FileFormat.getFileFormat(path.toString());
        format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(path))), null);
        var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", path.toString()));
        plugin.prepare();
        plugin.stopped = false;
        plugin.paused = false;
        plugin.fadeout = false;

        MXDRV mxdrv = ((mdplayer.driver.mxdrv.MxDriver) plugin.getDriver()).getMxdrv();
        Map<String, Integer> modes = new TreeMap<>();
        MXDRV.Pcm8Interface pp = mxdrv.pcm8pp;
        mxdrv.pcm8pp = new MXDRV.Pcm8Interface() {
            @Override public void writePcm(byte[] pcm, int offset, int length) { pp.writePcm(pcm, offset, length); }
            @Override public void keyOn(int ch, int d1, int d2, int d3) {
                modes.merge("pcm8 f=%02x".formatted((d2 >> 8) & 0xff), 1, Integer::sum);
                pp.keyOn(ch, d1, d2, d3);
            }
            @Override public void keyOff(int ch) { pp.keyOff(ch); }
            @Override public void abort() { pp.abort(); }
        };
        MXDRV.MdxPcmInterface m = mxdrv.mdxPCM;
        mxdrv.mdxPCM = (MXDRV.MdxPcmInterface) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {MXDRV.MdxPcmInterface.class}, (proxy, method, args) -> {
                    if (method.getName().equals("keyOnAdpcm"))
                        { modes.merge("adpcm f=%02x".formatted(((int) args[1] >> 8) & 0xff), 1, Integer::sum); }
                    return method.invoke(m, args);
                });

        short[] buffer = new short[1024];
        double sum = 0; long n = 0;
        for (int i = 0; i < 44100 * 60 / 512; i++) {
            plugin.getDriver().render(buffer, 0, buffer.length);
            for (short s : buffer) { sum += s * (double) s; n++; }
        }
        System.err.printf("%s pcm8Mode=%b rms=%.1f %s scan=%s type=%d%n", path.getFileName(), mxdrv.isPcm8Mode(), Math.sqrt(sum / n), modes, mdplayer.driver.mxdrv.Pcm8Detector.frequencyCodes(Files.readAllBytes(path)), Setting.getInstance().pcm8Type(plugin));
        plugin.stop();
        plugin.close();
    }
}
