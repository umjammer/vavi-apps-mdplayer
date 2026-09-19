/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.sid;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import mdplayer.lib.sid.libsidplayfp.sidplayfp.SidTune;
import mdplayer.lib.sid.libsidplayfp.sidplayfp.SidTuneInfo;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-01-25 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property
    String sid;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        System.setProperty("mdsound.volume", String.valueOf(volume));
Debug.print("volume: " + volume);
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {
//        System.setProperty("javax.sound.sampled.SourceDataLine", "#WaveOut Mixer");
//        Path wav = Path.of("tmp", "waveout_sid.wav");
//        System.setProperty("vavi.sound.sampled.misc.waveout", wav.toString());

Debug.println(sid);
        SidTestProgram.main(new String[]{sid});

//        if ("#WaveOut Mixer".equals(System.getProperty("javax.sound.sampled.SourceDataLine")))
//            assertTrue(Files.exists(wav));
    }

    @Test
    void test2() throws Exception {
        byte[] buf = Files.readAllBytes(Paths.get(sid));
        SidTune tune = new SidTune(buf, buf.length);
        SidTuneInfo info = tune.getInfo();
        System.out.println("Songs: " + info.songs());
        System.out.println("Clock Speed: " + info.clockSpeed());
        System.out.println("Song Speed: " + info.songSpeed());
    }
}
