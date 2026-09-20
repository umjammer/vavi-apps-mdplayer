/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mfi.synth;

import java.io.IOException;
import java.io.UncheckedIOException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import mdplayer.driver.mfi.MldSynth;


/**
 * A {@link Synthesizer} of the midi api opened without a line of its own, rendered by reading
 * the stream it gives, 16 bit, stereo, little endian.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-20 nsano initial version <br>
 */
public abstract class StreamMldSynth implements MldSynth {

    private Synthesizer synthesizer;
    private AudioInputStream stream;
    private Receiver receiver;
    private int sampleRate;
    private byte[] pcm = new byte[0];

    /** a new one, not open */
    protected abstract Synthesizer createSynthesizer() throws IOException, MidiUnavailableException;

    /** opens the synthesizer without a line */
    protected abstract AudioInputStream openStream(Synthesizer synthesizer) throws MidiUnavailableException;

    /**
     * @return true: the synthesizer doesn't take the exclusives of vavi-sound's mfi (adpcm, machine
     *         dependent) itself, they go through {@link MfiReceiver}
     */
    protected boolean needsMfiReceiver() {
        return true;
    }

    @Override
    public void open() throws IOException, MidiUnavailableException {
        synthesizer = createSynthesizer();
        stream = openStream(synthesizer);
        AudioFormat format = stream.getFormat();
        if (!AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding()) || format.getSampleSizeInBits() != 16
                || format.getChannels() != 2 || format.isBigEndian()) {
            synthesizer.close();
            throw new IllegalStateException(getName() + ": " + format);
        }
        sampleRate = (int) format.getSampleRate();
        Receiver receiver = synthesizer.getReceiver();
        this.receiver = needsMfiReceiver() ? new MfiReceiver(receiver) : receiver;
    }

    @Override
    public Receiver getReceiver() {
        return receiver;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public void render(int[] left, int[] right, int frames) {
        if (pcm.length < frames * 4) pcm = new byte[frames * 4];
        int n = 0;
        try {
            while (n < frames * 4) {
                int r = stream.read(pcm, n, frames * 4 - n);
                if (r <= 0) break;
                n += r;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (int i = 0; i < frames; i++) {
            if (i * 4 + 3 < n) {
                left[i] = (short) ((pcm[i * 4] & 0xff) | (pcm[i * 4 + 1] << 8));
                right[i] = (short) ((pcm[i * 4 + 2] & 0xff) | (pcm[i * 4 + 3] << 8));
            } else {
                left[i] = right[i] = 0;
            }
        }
    }

    @Override
    public void close() {
        if (receiver != null) receiver.close();
        if (synthesizer != null) synthesizer.close();
    }
}
