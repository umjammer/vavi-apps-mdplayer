/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.hvl;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import mdplayer.lib.hvl.HVL;
import vavi.sound.sampled.md.MdAudioFileReader;
import vavi.sound.sampled.md.MdFormatConversionProvider;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavi.sound.SoundUtil.volume;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-02 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
@EnabledIf("localPropertiesExists")
class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property
    String hvl;

    static final boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static final long time = onIde ? 1000 * 1000 : 5 * 1000;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    void test1() throws Exception {
        Path path = Path.of(hvl);
Debug.print(hvl);

        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        volume(line, volume);
        line.start();

        HVL.hvl_InitReplayer();
        HVL.Tune ht = HVL.hvl_LoadTune(path.toString(), 44100, 0, (int) Files.size(path));

        int samples = ht.ht_Frequency / 50 / ht.ht_SpeedMultiplier;
        int totalFrameSamples = samples * ht.ht_SpeedMultiplier;
        short[] bufL = new short[totalFrameSamples];
        short[] bufR = new short[totalFrameSamples];
        byte[] byteBuf = new byte[totalFrameSamples * 2];

        long totalSquared = 0;
        int totalSamples = 0;
        int peak = 0;

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < time) {
            ht.hvl_DecodeFrame(bufL, 0, bufR, 0, 2);

            for (int i = 0; i < totalFrameSamples; i++) {
                int sample = (bufL[i] + bufR[i]) / 2;
                if (sample > 32767) {
                    sample = 32767;
                } else if (sample < -32768) {
                    sample = -32768;
                }

                totalSquared += (long) sample * sample;
                int absVal = Math.abs(sample);
                if (absVal > peak) peak = absVal;
                totalSamples++;

                byteBuf[i * 2 + 0] = (byte) (sample & 0xff);
                byteBuf[i * 2 + 1] = (byte) ((sample >>> 8) & 0xff);
            }

            line.write(byteBuf, 0, byteBuf.length);
        }

        line.drain();
        line.close();

        double rms = Math.sqrt((double) totalSquared / totalSamples);
        System.out.printf("Stats: RMS=%.2f Peak=%d Samples=%d%n", rms, peak, totalSamples);

        assertTrue(rms > 50.0, "RMS is too low: " + rms);
        assertTrue(peak > 2000, "Peak is too low: " + peak);
    }

    /** via spi (HvlFormat/HvlPlugin/HvlDriver) */
    @Test
    void test2() throws Exception {
        Path path = Path.of(hvl);
Debug.print(hvl);

        AudioInputStream sourceAis = new MdAudioFileReader().getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));

        AudioFormat inAudioFormat = sourceAis.getFormat();
Debug.println("IN: " + inAudioFormat);
        AudioFormat outAudioFormat = new AudioFormat(
                Encoding.PCM_SIGNED,
                44100,
                16,
                2,
                4,
                44100,
                false,
                Map.of("track", 1));
Debug.println("OUT: " + outAudioFormat);

        assertTrue(new MdFormatConversionProvider().isConversionSupported(outAudioFormat, inAudioFormat));

        AudioInputStream secondAis = new MdFormatConversionProvider().getAudioInputStream(outAudioFormat, sourceAis);
        SourceDataLine line = AudioSystem.getSourceDataLine(secondAis.getFormat());
        line.open(secondAis.getFormat());
        volume(line, volume);
        line.start();

        long totalSquared = 0;
        int totalSamples = 0;
        int peak = 0;

        byte[] buf = new byte[1024];
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < time) {
            int r = secondAis.read(buf, 0, buf.length);
            if (r < 0) break;

            for (int i = 0; i < r / 2; i++) {
                int sample = (short) ((buf[i * 2] & 0xff) | (buf[i * 2 + 1] << 8));
                totalSquared += (long) sample * sample;
                int absVal = Math.abs(sample);
                if (absVal > peak) peak = absVal;
                totalSamples++;
            }

            line.write(buf, 0, r);
        }

        line.drain();
        line.close();

        double rms = Math.sqrt((double) totalSquared / totalSamples);
        System.out.printf("Stats: RMS=%.2f Peak=%d Samples=%d%n", rms, peak, totalSamples);

        assertTrue(rms > 50.0, "RMS is too low: " + rms);
        assertTrue(peak > 2000, "Peak is too low: " + peak);
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void testAllHvlInTmp() throws Exception {
        Path tmpDir = Path.of("tmp/hvl");
        if (!Files.exists(tmpDir)) return;

        try (var stream = Files.walk(tmpDir)) {
            for (Path path : stream.filter(p -> p.toString().toLowerCase().endsWith(".hvl")).toList()) {
                System.out.println("Testing HVL: " + path);
                mdplayer.driver.FileFormat format = mdplayer.driver.FileFormat.getFileFormat(path.toString());
                format.load(Files.newInputStream(path), null);
                @SuppressWarnings("unchecked")
                var plugin = (mdplayer.driver.BasePlugin<? extends mdplayer.driver.BaseDriver>) format.getPlugin();
                plugin.setParams(format, Map.of("fileName", path.toString()));
                plugin.prepare();
                mdplayer.driver.BaseDriver driver = plugin.getDriver();

                short[] buf = new short[1024];
                int iterations = 0;
                while (!driver.stopped && driver.curLoop == 0 && iterations < 50000) {
                    driver.render(buf, 0, buf.length);
                    iterations++;
                }
                System.out.printf("  -> Finished in %d iterations, curLoop=%d, stopped=%s%n",
                        iterations, driver.curLoop, driver.stopped);
            }
        }
    }
}
