/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mfi.synth;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import javax.sound.midi.Receiver;

import mdplayer.driver.mfi.MldSynth;
import vavi.sound.faith.FaithRom;
import vavi.sound.mfi.MfiChip;
import vavi.sound.mfi.rohm.RohmMfiSynthesizer.RohmMfiReceiver;
import vavi.sound.rohm.RohmAudioEngine;
import vavi.sound.rohm.RohmRom;


/**
 * The rohm sound source, the rom out of the authoring tool's {@code rt_synth_2.dll}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
public class RohmMldSynth implements MldSynth {

    private RohmAudioEngine engine;
    private Receiver receiver;
    private byte[] pcm = new byte[0];

    @Override
    public String getName() {
        return "rohm";
    }

    @Override
    public String getDescription() {
        return "Rohm";
    }

    @Override
    public Set<MfiChip> getChips() {
        return EnumSet.of(MfiChip.ROHM);
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean isAvailable() {
        return RohmRom.isAvailable();
    }

    @Override
    public String getRequirement() {
        return "rt_synth_2.dll under " + FaithRom.toolsDirectory() + ", set -Dvavi.sound.faith.path=<dir>";
    }

    @Override
    public void open() throws IOException {
        engine = new RohmAudioEngine(RohmRom.getInstance(), false);
        // it handles the vavi-sound exclusives (the mfi values, adpcm) itself
        receiver = new RohmMfiReceiver(engine);
    }

    @Override
    public Receiver getReceiver() {
        return receiver;
    }

    @Override
    public int getSampleRate() {
        return RohmAudioEngine.SAMPLE_RATE;
    }

    @Override
    public void render(int[] left, int[] right, int frames) {
        if (pcm.length < frames * 4) pcm = new byte[frames * 4];
        engine.render(pcm, frames);
        for (int i = 0; i < frames; i++) {
            left[i] = (short) ((pcm[i * 4] & 0xff) | (pcm[i * 4 + 1] << 8));
            right[i] = (short) ((pcm[i * 4 + 2] & 0xff) | (pcm[i * 4 + 3] << 8));
        }
    }

    @Override
    public void close() {
        if (receiver != null) receiver.close();
        if (engine != null) engine.close();
    }
}
