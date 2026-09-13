/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.md;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.ServiceLoader;
import javax.sound.SoundClip;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import javax.sound.sampled.spi.FormatConversionProvider;
import com.sun.media.sound.JDK13Services;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavi.sound.SoundUtil.volume;
import static vavix.util.DelayedWorker.later;


/**
 * SpiTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-03-30 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class SpiTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static final boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static final long time = onIde ? 1000 * 1000 : 10 * 1000;

    @Property(name = "vgm")
    String inFile = "src/test/resources/test.vgm";

    /** 1 origin */
    @Property
    int track;

    @Property(name = "mdplayer.fmp.dir")
    String fmpDir;
    @Property(name = "mdplayer.fmp.pvi")
    String fmpPvi;
    @Property(name = "mdplayer.zms.dir")
    String zmsDir;
    @Property(name = "mdplayer.mgs.dir")
    String mgsDir;
    @Property(name = "mdplayer.ndp.dir")
    String ndpDir;
    @Property(name = "mdplayer.musica.dir")
    String musicaDir;
    @Property(name = "muap.dir.dta")
    String muapDirDta;
    @Property(name = "muap.dir.pcm")
    String muapDirPcm;

    @Property(name = "mdplayer.variant.pcm8")
    int variantPcm8;
    @Property(name = "mdplayer.variant.mpcm")
    int variantMpcm;
    @Property(name = "mdplayer.variant.ym2151")
    int variantYm2151;
    @Property(name = "mdplayer.variant.ym2413")
    int variantYm2413;
    @Property(name = "mdplayer.variant.ymf262")
    int variantYmf262;
    @Property(name = "mdplayer.variant.ay8910")
    int variantAy8910;

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);

            // fmp
            System.setProperty("mdplayer.fmp.dir", fmpDir);
            System.setProperty("mdplayer.fmp.pvi", fmpPvi);
            // zms
            System.setProperty("mdplayer.zms.dir", zmsDir);
            // mgsdrv
            System.setProperty("mdplayer.mgs.dir", mgsDir);
            // ndp
            System.setProperty("mdplayer.ndp.dir", ndpDir);
            // musica
            System.setProperty("mdplayer.musica.dir", musicaDir);
            // muap
            System.setProperty("muap.dir.dta", muapDirDta);
            System.setProperty("muap.dir.pcm", muapDirPcm);
//            System.setProperty("muap.dir.udp", muapDirUdp);
//            System.setProperty("muap.dir.sud", muapDirSud);
        }

        // disable other conflicted reader spi
        System.setProperty("vavi.sound.sampled.spi.emu.vgm", "false");
        System.setProperty("vavi.sound.sampled.spi.emu.gbs", "false");
        System.setProperty("vavi.sound.sampled.spi.emu.nsf", "false");
        System.setProperty("vavi.sound.sampled.spi.mod.sid", "false");
        System.setProperty("vavi.sound.sampled.spi.ymfm", "false");

        // chip variant settings
        System.setProperty("mdplayer.variant.pcm8", String.valueOf(variantPcm8));
        System.setProperty("mdplayer.variant.mpcm", String.valueOf(variantMpcm));
        System.setProperty("mdplayer.variant.ym2151", String.valueOf(variantYm2151));
        System.setProperty("mdplayer.variant.ym2413", String.valueOf(variantYm2413));
        System.setProperty("mdplayer.variant.ay8910", String.valueOf(variantAy8910));
        System.setProperty("mdplayer.variant.ymf262", String.valueOf(variantYmf262));

Debug.println("volume: " + volume + ", player.volume: " + System.getProperty("mdplayer.volume") + ", cwd: " + System.getProperty("user.dir") + ", time: " + time);
Debug.println("settings\n" +
        "mdplayer.fmp.dir: " + System.getProperty("mdplayer.fmp.dir") + "\n" +
        "mdplayer.fmp.pvi: " + System.getProperty("mdplayer.fmp.pvi") + "\n" +
        "mdplayer.zms.dir: " + System.getProperty("mdplayer.zms.dir") + "\n" +
        "mdplayer.mgs.dir: " + System.getProperty("mdplayer.mgs.dir") + "\n" +
        "mdplayer.ndp.dir: " + System.getProperty("mdplayer.ndp.dir") + "\n" +
        "mdplayer.musica.dir: " + System.getProperty("mdplayer.musica.dir") + "\n" +
        "muap.dir.dta: " + System.getProperty("muap.dir.dta") + "\n" +
        "muap.dir.pcm: " + System.getProperty("muap.dir.pcm") + "\n" +
        "mdplayer.variant.pcm8: " + System.getProperty("mdplayer.variant.pcm8") + "\n" +
        "mdplayer.variant.mpcm: " + System.getProperty("mdplayer.variant.mpcm") + "\n" +
        "mdplayer.variant.ay8910: " + System.getProperty("mdplayer.variant.ay8910") + "\n" +
        "mdplayer.variant.ym2413: " + System.getProperty("mdplayer.variant.ym2413") + "\n" +
        "mdplayer.variant.ymf262: " + System.getProperty("mdplayer.variant.ymf262") + "\n" +
        "mdplayer.variant.ym2151: " + System.getProperty("mdplayer.variant.ym2151"));
    }

    @Test
    @DisplayName("via spi directly")
    public void test0() throws Exception {
Debug.println(inFile);
        Path path = Paths.get(inFile);
        AudioInputStream sourceAis = new MdAudioFileReader().getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));

        AudioFormat inAudioFormat = sourceAis.getFormat();
Debug.println("IN: " + inAudioFormat);
        Map<String, Object> map = Map.of("track", track);
        AudioFormat outAudioFormat = new AudioFormat(
                Encoding.PCM_SIGNED,
                44100,
                16,
                2,
                4,
                44100,
                false,
                map);
Debug.println("OUT: " + outAudioFormat);

        assertTrue(new MdFormatConversionProvider().isConversionSupported(outAudioFormat, inAudioFormat));

        AudioInputStream secondAis = new MdFormatConversionProvider().getAudioInputStream(outAudioFormat, sourceAis);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, secondAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(secondAis.getFormat());
        line.addLineListener(ev -> Debug.println(ev.getType()));
        line.start();

        volume(line, volume);

        byte[] buf = new byte[1024];
        while (!later(time).come()) {
            int r = secondAis.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
    }

    @Test
    @DisplayName("via spi")
    public void test1() throws Exception {
Debug.println(inFile);
        Path path = Paths.get(inFile);
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));

        AudioFormat inAudioFormat = sourceAis.getFormat();
Debug.println("IN: " + inAudioFormat + ", " + inAudioFormat.getEncoding().getClass().getName());
Debug.println("\n" +
 "artist:   " + inAudioFormat.getProperty("md.artist") + "\n" +
 "album:    " + inAudioFormat.getProperty("md.album") + "\n" +
 "title:    " + inAudioFormat.getProperty("md.title") + "\n" +
 "composer: " + inAudioFormat.getProperty("md.composer"));

        assertInstanceOf(MdEncoding.class, inAudioFormat.getEncoding());

        Map<String, Object> map = Map.of("track", track);
        AudioFormat outAudioFormat = new AudioFormat(
                Encoding.PCM_SIGNED,
                44100,
                16,
                2,
                4,
                44100,
                false,
                map);
Debug.println("OUT: " + outAudioFormat);

for(var codec : ServiceLoader.load(FormatConversionProvider.class)) {
 if (codec.isConversionSupported(outAudioFormat, inAudioFormat)) {
Debug.println("converter: " + codec.getClass().getName());
  break;
 }
}
        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        AudioInputStream secondAis = AudioSystem.getAudioInputStream(outAudioFormat, sourceAis);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, secondAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(secondAis.getFormat());
        line.addLineListener(ev -> Debug.println(ev.getType()));
        line.start();

        volume(line, volume);

        byte[] buf = new byte[1024];
        while (!later(time).come()) {
            int r = secondAis.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
Debug.print("done");
    }

    @Test
    public void test4() throws Exception {
        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            System.err.println(type);
        }
        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(new File(inFile).toURI().toURL());
        AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
Debug.println(originalAudioFormat);
    }

    @Test
    @DisplayName("another input type 2")
    void test2() throws Exception {
        URL url = Paths.get(inFile).toUri().toURL();
        AudioInputStream ais = AudioSystem.getAudioInputStream(url);
        assertInstanceOf(MdEncoding.class, ais.getFormat().getEncoding());
    }

    @Test
    @DisplayName("another input type 3")
    void test3() throws Exception {
        File file = Paths.get(inFile).toFile();
        AudioInputStream ais = AudioSystem.getAudioInputStream(file);
        assertInstanceOf(MdEncoding.class, ais.getFormat().getEncoding());
    }

    @Test
    @DisplayName("when unsupported file coming")
    void test5() throws Exception {
        InputStream is = SpiTest.class.getResourceAsStream("/test.caf");
        int available = is.available();
        UnsupportedAudioFileException e = assertThrows(UnsupportedAudioFileException.class, () -> {
            AudioInputStream ais = AudioSystem.getAudioInputStream(is);
Debug.println("encoding: " + ais.getFormat().getEncoding());
        });
Debug.println(e.getMessage());
        assertEquals(available, is.available()); // spi must not consume input stream even one byte
    }

    @Test
    @Disabled("loading takes too long time")
    void test6() throws Exception {
        var clip = SoundClip.createSoundClip(Path.of(inFile).toFile());
        clip.play();
    }

    @Test
    @DisplayName("simulate inside spi")
    void test7() throws IOException {
Debug.println(inFile);
        Path path = Paths.get(inFile);
        InputStream is = new BufferedInputStream(Files.newInputStream(path));

        for(var o : JDK13Services.getProviders(AudioFileReader.class)) {
            AudioFileReader reader = (AudioFileReader) o;
            try {
Debug.println("TRY reader: " + reader.getClass().getName() + ", " + is.available());
                reader.getAudioFileFormat(is);
Debug.println("OK reader: " + reader.getClass().getName());
                break;
            } catch (UnsupportedAudioFileException e) {
Debug.println("FAILED reader: " + reader.getClass().getName() + ", " + is.available());
            }
        }
    }
}
