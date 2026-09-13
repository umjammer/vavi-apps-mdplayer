package mdplayer.driver.gbs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import mdplayer.ChipFmDspSource;
import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.driver.FileFormat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import vavi.util.event.GenericEvent;

import static org.junit.jupiter.api.Assertions.*;

class GbsDriverTest {

    @BeforeAll
    static void setupAll() {
        System.setProperty("mdplayer.variant.ymf262", "0");
        System.setProperty("javax.sound.sampled.SourceDataLine", "#WaveOut Mixer");
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    public void testGbsTimeAndLoop() throws Exception {
        String filename = "../vavi-sound-emu/tmp/gbs/CGB-B2XE-USA.gbs";
        assertTrue(Files.exists(Path.of(filename)), "File must exist: " + filename);

        Setting setting = Setting.getInstance();
        setting.getOutputDevice().setDeviceType(Common.DEV_Null);

        FileFormat format = FileFormat.getFileFormat(filename);
        format.load(Files.newInputStream(Path.of(filename)), null);

        GbsPlugin plugin = (GbsPlugin) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", filename, "songNo", 1));

        plugin.prepare();

        GbsDriver driver = (GbsDriver) plugin.getDriver();
        assertNotNull(driver);

        ChipFmDspSource fmdspSource = new ChipFmDspSource();
        fmdspSource.bind(plugin);

        int sampleRate = setting.getOutputDevice().getSampleRate();

        short[] buffer = new short[1024];

        // Process 180 seconds of audio, logging status every 10s
        for (int sec = 10; sec <= 180; sec += 10) {
            int targetSamples = sampleRate * sec;
            while (driver.counter < targetSamples) {
                int toRender = Math.min(buffer.length, targetSamples - (int) driver.counter);
                driver.render(buffer, 0, toRender);
                fmdspSource.update(new GenericEvent(this, "master", buffer, 0));
            }

            System.err.printf("At %3ds: counter=%d, fmdspFrames=%d, curLoop=%d, loopCounter=%d, totalCounter=%d, stopped=%b\n",
                    sec, driver.counter, fmdspSource.generatedFrames(),
                    driver.curLoop, driver.loopCounter, driver.totalCounter, driver.stopped);
        }

        assertTrue(driver.counter >= sampleRate * 180, "Counter should reach 180s");
        assertTrue(fmdspSource.generatedFrames() > 0, "Generated frames should be positive");

        plugin.stop();
        plugin.close();
    }

    @AfterAll
    static void tearDownAll() {
        System.clearProperty("javax.sound.sampled.SourceDataLine");
    }
}
