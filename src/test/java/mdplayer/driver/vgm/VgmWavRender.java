/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.vgm;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;
import vavi.util.archive.Archives;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


/**
 * Headless diagnostic: renders a vgm to a wav through the driver, without audio, to compare with
 * a reference player's render (e.g. libvgm's vgm2wav). Not a regression test.
 * <p>
 * Run with {@code -Dvavi.test=wav -Dvgm.file=<in> -Dvgm.out=<out.wav>}.
 */
class VgmWavRender {

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "wav")
    @SuppressWarnings("unchecked")
    void render() throws Exception {
        String file = System.getProperty("vgm.file");
        Path outPath = Path.of(System.getProperty("vgm.out"));

        Setting setting = Setting.getInstance();
        setting.getOutputDevice().setDeviceType(Common.DEV_Null);

        FileFormat format = FileFormat.getFileFormat(file);
        format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(Path.of(file)))), file);
        var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", file, "midiMode", 0, "songNo", 0));
        plugin.prepare();
        try {
            BaseDriver driver = plugin.getDriver();
            int rate = setting.getOutputDevice().getSampleRate();
            long max = (long) rate * 60 * 5; // 5 min max
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();
            short[] buf = new short[4096];
            ByteBuffer bb = ByteBuffer.allocate(buf.length * 2).order(ByteOrder.LITTLE_ENDIAN);
            long frames = 0;
            while (frames < max) {
                int r = driver.render(buf, 0, buf.length);
                if (r <= 0) break;
                bb.clear();
                for (int i = 0; i < r; i++) bb.putShort(buf[i]);
                pcm.write(bb.array(), 0, r * 2);
                frames += r / 2;
                if (plugin.driverVirtual != null && plugin.driverVirtual.stopped) break;
            }
            byte[] data = pcm.toByteArray();
            AudioFormat af = new AudioFormat(rate, 16, 2, true, false);
            try (AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(data), af, data.length / 4)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(outPath.toString()));
            }
            System.err.printf("wrote %s: %.2f sec%n", outPath, frames / (double) rate);
        } finally {
            try { plugin.stop(); } catch (Exception ignore) {}
            try { plugin.close(); } catch (Exception ignore) {}
            try { plugin.chipRegister.close(); } catch (Exception ignore) {}
        }
    }
}
