/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.ahx;

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

import mdplayer.lib.ahx.AHX;
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
    String ahx;

    static final boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static final long time = onIde ? 1000 * 1000 : 10 * 1000;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

if (!System.getProperty("vavi.test", "").equals("ide") && volume > 0.01) volume = 0.01; // adhoc fix for large volume
    }

    @Test
    void test1() throws Exception {
        Path path = Path.of(ahx);
Debug.print(ahx);

        SourceDataLine line = AudioSystem.getSourceDataLine(new AudioFormat(44100, 16, 1, true, false));
        line.open();
        volume(line, volume);
        line.start();

        AHX.AHXPlayer player = new AHX.AHXPlayer();
        player.init();

        player.loadSong(path.toString());

        player.initSubsong(0);

        AHX.AHXOutput output = new AHX.AHXOutput();
        output.player = player;
        output.init(44100, 16, 1, 256.0f, 50);

        byte[] byteBuf = new byte[882 * 2];
        long totalSquared = 0;
        int totalSamples = 0;
        int peak = 0;

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < time) {
            output.mixBuffer();

            for (int i = 0; i < 882; i++) {
                int sample = output.mixingBuffer[i];
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

    /** via spi (AhxFormat/AhxPlugin/AhxDriver) */
    @Test
    void test2() throws Exception {
        Path path = Path.of(ahx);
Debug.print(ahx);

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
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ai")
    void testExquisiet() throws Exception {
        Path path = Path.of("/Users/nsano/Public/np2/AHX/Silok/exquisiet.ahx");
        if (!Files.exists(path)) return;

        AHX.AHXPlayer player = new AHX.AHXPlayer();
        player.init();
        player.loadSong(path.toString());
        player.initSubsong(0);

        AHX.AHXOutput output = new AHX.AHXOutput();
        output.player = player;
        output.init(44100, 16, 1, 256.0f, 50);

        for (int frame = 0; frame < 20000; frame++) {
            output.mixBuffer();
        }
    }
}
