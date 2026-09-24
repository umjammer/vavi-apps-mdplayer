/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import vavi.util.archive.Archives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * GigatronReaderTest.
 */
class GigatronReaderTest {

    static final int CLOCK = 31250;

    /** mml2vgm's sample, copied from mml2vgm/samples/sample/Gigatron */
    static final Path SAMPLE = Path.of("tmp/zgm/gigatronTest.zgm");

    static boolean sampleExists() {
        return Files.exists(SAMPLE);
    }

    @Test
    void testFrequencyOf() {
        // o5 a in mml2vgm's FNUM_Gigatron.txt, from ROMv1's notesTable
        assertEquals(880, GigatronReader.frequencyOf(0x0e6b, 1, CLOCK), 0.5);
        // a channel the mask serves twice a tick runs twice as fast
        assertEquals(1760, GigatronReader.frequencyOf(0x0e6b, 2, CLOCK), 1);
        assertEquals(0, GigatronReader.frequencyOf(0, 1, CLOCK));
    }

    @Test
    void testNote() {
        assertEquals(69, Notes.noteOf(GigatronReader.frequencyOf(0x0e6b, 1, CLOCK))); // A5
    }

    /** plays the sample's first second and reads what the fmdsp would show */
    @Test
    @EnabledIf("sampleExists")
    @SuppressWarnings("unchecked")
    void testSample() throws Exception {
        Setting setting = Setting.getInstance();
        setting.getOutputDevice().setDeviceType(Common.DEV_Null);

        String file = SAMPLE.toString();
        FileFormat format = FileFormat.getFileFormat(file);
        format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(SAMPLE))), file);
        var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", file, "midiMode", 0, "songNo", 0));
        plugin.prepare();
        try {
            GigatronReader reader = new GigatronReader();
            reader.bind(plugin.chipRegister);
            reader.reset();

            int rate = setting.getOutputDevice().getSampleRate();
            short[] buf = new short[2 * rate / 50]; // 20ms
            List<Integer> notes = new ArrayList<>();
            boolean sawActive = false;
            for (int t = 0; t < 100; t++) { // 2 s, the first 0.5 s is the start latency
                plugin.getDriver().render(buf, 0, buf.length);
                reader.poll();
                if (reader.active(FmDspChipReader.Group.FM)) sawActive = true;
                FmDspChannel c = new FmDspChannel();
                reader.read(FmDspChipReader.Group.FM, 0, c);
                if (c.sounding && (notes.isEmpty() || notes.get(notes.size() - 1) != c.note)) notes.add(c.note);
                FmDspChannel idle = new FmDspChannel();
                reader.read(FmDspChipReader.Group.FM, 2, idle);
                assertFalse(idle.sounding, "channel 3 is never written");
            }
            assertTrue(sawActive);
            // "e.d8cd" on o4, which mml2vgm's table puts an octave down: E3 D3 C3 D3
            assertEquals(List.of(40, 38, 36, 38), notes.subList(0, 4), notes.toString());
        } finally {
            try { plugin.stop(); } catch (Exception ignore) {}
            try { plugin.close(); } catch (Exception ignore) {}
            try { plugin.chipRegister.close(); } catch (Exception ignore) {}
        }
    }
}
