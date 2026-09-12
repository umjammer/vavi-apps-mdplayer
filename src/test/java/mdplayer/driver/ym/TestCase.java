/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.ym;

import java.io.BufferedInputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;
import mdplayer.lib.ym.Ym2149Ex;
import mdplayer.lib.ym.YmMusic;
import mdplayer.lib.ym.YmMusic.YmMusicInfo;
import vavi.sound.sampled.md.MdAudioFileReader;
import vavi.sound.sampled.md.MdFormatConversionProvider;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static mdplayer.driver.ym.Ym2Wav.writeLE16;
import static mdplayer.driver.ym.Ym2Wav.writeWavHeader;
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
    String ym;

    static final boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static final long time = onIde ? 1000 * 1000 : 10 * 1000;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    static final int NBSAMPLEPERBUFFER = 1024;

    @Test
    void test1() throws Exception {
Debug.print(ym);

        short[] convertBuffer = new short[NBSAMPLEPERBUFFER];

        YmMusic music = new YmMusic();
        music.ymChip = new YmMusic.Ym2149Ex() {
            final Ym2149Ex chip = new Ym2149Ex();
            @Override
            public void setClock(int clock) {
                chip.setClock(clock);
            }

            @Override
            public void reset() {
                chip.reset();
            }

            @Override
            public void writeRegister(int reg, int value) {
                chip.writeRegister(reg, value);
            }

            @Override
            public int readRegister(int reg) {
                return chip.readRegister(reg);
            }

            @Override
            public void update(short[] buffer, int length) {
                chip.update(buffer, length);
            }

            @Override
            public void sidStart(int voice, int freq, int volume) {
                chip.sidStart(voice, freq, volume);
            }

            @Override
            public void sidSinStart(int voice, int freq, int volume) {
                chip.sidSinStart(voice, freq, volume);
            }

            @Override
            public void sidStop(int voice) {
                chip.sidStop(voice);
            }

            @Override
            public void drumStart(int voice, byte[] data, int size, int freq) {
                chip.drumStart(voice, data, size, freq);
            }

            @Override
            public void syncBuzzerStart(int freq, int volume) {
                chip.syncBuzzerStart(freq, volume);
            }

            @Override
            public void syncBuzzerStop() {
                chip.syncBuzzerStop();
            }
        };

        music.load(ym);

        YmMusicInfo info = new YmMusicInfo();
        music.getMusicInfo(info);

        int totalNbSample;

        try (RandomAccessFile out = new RandomAccessFile("tmp/ym_out.wav", "rw")) {

            out.write(new byte[44]);

            music.setLoopMode(false);

            totalNbSample = 0;

            music.stop();
            music.play();

            while (music.update(convertBuffer, NBSAMPLEPERBUFFER)) {

                for (short s : convertBuffer) {
                    writeLE16(out, s);
                }

                totalNbSample += NBSAMPLEPERBUFFER;
            }

            out.seek(0);
            writeWavHeader(out, totalNbSample);
        }

        music.unLoad();
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test2() throws Exception {
Debug.print(ym);

        short[] convertBuffer = new short[NBSAMPLEPERBUFFER];

        YmMusic music = new YmMusic();
        music.ymChip = new YmMusic.Ym2149Ex() {
            final Ym2149Ex chip = new Ym2149Ex();
            @Override
            public void setClock(int clock) {
                chip.setClock(clock);
            }

            @Override
            public void reset() {
                chip.reset();
            }

            @Override
            public void writeRegister(int reg, int value) {
                chip.writeRegister(reg, value);
            }

            @Override
            public int readRegister(int reg) {
                return chip.readRegister(reg);
            }

            @Override
            public void update(short[] buffer, int length) {
                chip.update(buffer, length);
            }

            @Override
            public void sidStart(int voice, int freq, int volume) {
                chip.sidStart(voice, freq, volume);
            }

            @Override
            public void sidSinStart(int voice, int freq, int volume) {
                chip.sidSinStart(voice, freq, volume);
            }

            @Override
            public void sidStop(int voice) {
                chip.sidStop(voice);
            }

            @Override
            public void drumStart(int voice, byte[] data, int size, int freq) {
                chip.drumStart(voice, data, size, freq);
            }

            @Override
            public void syncBuzzerStart(int freq, int volume) {
                chip.syncBuzzerStart(freq, volume);
            }

            @Override
            public void syncBuzzerStop() {
                chip.syncBuzzerStop();
            }
        };

        music.load(ym);

        YmMusicInfo info = new YmMusicInfo();
        music.getMusicInfo(info);

        SourceDataLine line = AudioSystem.getSourceDataLine(new AudioFormat(44100, 16, 1, true, false));
        line.open();
        volume(line, volume);
        line.start();

        music.setLoopMode(false);

        music.stop();
        music.play();

        while (music.update(convertBuffer, NBSAMPLEPERBUFFER)) {

            byte[] bb = new byte[NBSAMPLEPERBUFFER * Short.BYTES];
            int c = 0;
            for (short s : convertBuffer) {
                bb[c * 2 + 0] = (byte) (s & 0xff);
                bb[c * 2 + 1] = (byte) ((s >>> 8) & 0xff);
                c++;
            }
            line.write(bb, 0, bb.length);
        }

        music.unLoad();
    }

    /** via spi (YmFormat/YmPlugin/YmDriver) */
    @Test
    void test3() throws Exception {
        Path path = Path.of(ym);
Debug.print(ym);

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
    void testUnionDemo() throws Exception {
        Path path = Path.of("tmp/ym/Union_Demo-Alloy_Run.ym");
        if (!Files.exists(path)) return;

        YmMusic music = new YmMusic();
        music.ymChip = new YmMusic.Ym2149Ex() {
            final Ym2149Ex chip = new Ym2149Ex();
            @Override public void setClock(int clock) { chip.setClock(clock); }
            @Override public void reset() { chip.reset(); }
            @Override public void writeRegister(int reg, int value) { chip.writeRegister(reg, value); }
            @Override public int readRegister(int reg) { return chip.readRegister(reg); }
            @Override public void update(short[] buffer, int length) { chip.update(buffer, length); }
            @Override public void sidStart(int voice, int freq, int volume) { chip.sidStart(voice, freq, volume); }
            @Override public void sidSinStart(int voice, int freq, int volume) { chip.sidSinStart(voice, freq, volume); }
            @Override public void sidStop(int voice) { chip.sidStop(voice); }
            @Override public void drumStart(int voice, byte[] data, int size, int freq) { chip.drumStart(voice, data, size, freq); }
            @Override public void syncBuzzerStart(int freq, int volume) { chip.syncBuzzerStart(freq, volume); }
            @Override public void syncBuzzerStop() { chip.syncBuzzerStop(); }
        };

        music.loadMemory(Files.readAllBytes(path), (int) Files.size(path));
        YmMusicInfo info = new YmMusicInfo();
        music.getMusicInfo(info);

        System.out.printf("Title: %s, Author: %s, Type: %s, TimeSec: %d, Attrib: 0x%x%n",
                info.pSongName, info.pSongAuthor, info.pSongType, info.musicTimeInSec, music.getAttrib());

        FileFormat format = FileFormat.getFileFormat(path.toString());
        format.load(Files.newInputStream(path), null);
        var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", path.toString()));
        plugin.prepare();
        BaseDriver driver = plugin.getDriver();

        short[] buf = new short[1024];
        int iterations = 0;
        while (!driver.stopped && iterations < 40000) {
            driver.render(buf, 0, buf.length);
            iterations++;
        }
        System.out.println("Driver stopped at iteration " + iterations + ", curLoop: " + driver.curLoop + ", stopped: " + driver.stopped);
        assertTrue(driver.stopped, "Driver should stop at song end for non-looping YM");
    }
}
