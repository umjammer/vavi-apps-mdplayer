/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.smaf;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;

import mdplayer.lib.smaf.MmfToolPlayer;
import vavi.util.Debug;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavi.sound.midi.MidiUtil.volume;


/**
 * SmafMa5SynthesizerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-21 nsano initial version <br>
 */
class SmafMa5SynthesizerTest {

    /** the mmftool distribution carries one */
    static final Path mmf = Path.of(System.getProperty("mdplayer.smaf.test.mmf",
            Path.of(MmfToolPlayer.toolDirectory().getPath(), "test.mmf").toString()));

    static boolean mmftoolExists() {
        return MmfToolPlayer.isAvailable() && Files.exists(mmf);
    }

    static final boolean onIde = System.getProperty("vavi.test", "").equals("ide");

    @Test
    void provided() throws Exception {
        MidiDevice.Info info = null;
        for (MidiDevice.Info i : MidiSystem.getMidiDeviceInfo()) {
            if (i.getName().equals(SmafMa5Synthesizer.info.getName())) info = i;
        }
        assertNotNull(info);
        assertInstanceOf(SmafMa5Synthesizer.class, MidiSystem.getMidiDevice(info));
    }

    @Test
    @EnabledIf("mmftoolExists")
    void play() throws Exception {
        SmafMa5Synthesizer synthesizer = new SmafMa5Synthesizer();
        synthesizer.open();
        try {
            Receiver receiver = synthesizer.getReceiver();
            volume(receiver, Float.parseFloat(System.getProperty("vavi.test.volume.midi", "0.1")));
            byte[] data = Files.readAllBytes(mmf);
            receiver.send(new MetaMessage(SmafMa5Synthesizer.META_SMAF, data, data.length), -1);

            long until = System.currentTimeMillis() + (onIde ? 1000 * 1000 : 15 * 1000);
            while (!synthesizer.isFinished() && System.currentTimeMillis() < until) {
                Thread.sleep(100);
            }
Debug.println("player: " + synthesizer.getPlayer().getStatistics());
            assertTrue(synthesizer.getPlayer().isSounding(), "silent");
        } finally {
            synthesizer.close();
        }
    }
}
