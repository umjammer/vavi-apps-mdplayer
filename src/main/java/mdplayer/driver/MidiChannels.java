/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver;

import java.util.Arrays;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;


/**
 * The sixteen midi channels as the messages a driver sends to its synthesizer have left them, for
 * the visualizer: none of those synthesizers reports what it is sounding, and every message of the
 * song goes past here on its way to one, so what is kept here is exact.
 * <p>
 * Kept by the drivers that play a converted sequence on a sound source of their own - the mfi one
 * ({@link mdplayer.driver.mfi.MldDriver}) and the smaf one
 * ({@link mdplayer.driver.smaf.SmafDriver2}) - and read by
 * {@link mdplayer.fmdsp.MidiChannelsReader}.
 * <p>
 * Written on the render thread, read on the drawing one; a torn read is a frame of a stale value.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 *          0.01 2026-09-21 nsano moved out of the mfi driver, the smaf one keeps one too <br>
 */
public class MidiChannels {

    public static final int CHANNELS = 16;

    private final boolean[][] keys = new boolean[CHANNELS][128];
    /** keys held down, per channel */
    private final int[] held = new int[CHANNELS];
    /** the key struck last and its velocity */
    private final int[] notes = new int[CHANNELS];
    private final int[] velocities = new int[CHANNELS];
    /** counts the note ons, a key struck again on the same note is seen by this */
    private final int[] strikes = new int[CHANNELS];
    private final int[] programs = new int[CHANNELS];
    private final int[] banks = new int[CHANNELS];
    private final int[] volumes = new int[CHANNELS];
    private final int[] expressions = new int[CHANNELS];
    private final int[] pans = new int[CHANNELS];
    private final int[] modulations = new int[CHANNELS];
    private final int[] bends = new int[CHANNELS];

    public MidiChannels() {
        reset();
    }

    /** as a synthesizer starts */
    public synchronized void reset() {
        for (int ch = 0; ch < CHANNELS; ch++) {
            Arrays.fill(keys[ch], false);
        }
        Arrays.fill(held, 0);
        Arrays.fill(notes, 0);
        Arrays.fill(velocities, 0);
        Arrays.fill(strikes, 0);
        Arrays.fill(programs, 0);
        Arrays.fill(banks, 0);
        Arrays.fill(volumes, 100);
        Arrays.fill(expressions, 127);
        Arrays.fill(pans, 64);
        Arrays.fill(modulations, 0);
        Arrays.fill(bends, 0x2000);
    }

    /** takes a message on its way to the synthesizer */
    public void send(MidiMessage message) {
        if (!(message instanceof ShortMessage sm)) return;
        int ch = sm.getChannel();
        int d1 = sm.getData1();
        int d2 = sm.getData2();
        switch (sm.getCommand()) {
            case ShortMessage.NOTE_ON -> {
                if (d2 == 0) {
                    noteOff(ch, d1);
                } else {
                    if (!keys[ch][d1]) held[ch]++;
                    keys[ch][d1] = true;
                    notes[ch] = d1;
                    velocities[ch] = d2;
                    strikes[ch]++;
                }
            }
            case ShortMessage.NOTE_OFF -> noteOff(ch, d1);
            case ShortMessage.PROGRAM_CHANGE -> programs[ch] = d1;
            case ShortMessage.PITCH_BEND -> bends[ch] = d1 | (d2 << 7);
            case ShortMessage.CONTROL_CHANGE -> {
                switch (d1) {
                    case 0 -> banks[ch] = d2;
                    case 1 -> modulations[ch] = d2;
                    case 7 -> volumes[ch] = d2;
                    case 10 -> pans[ch] = d2;
                    case 11 -> expressions[ch] = d2;
                    case 120, 123 -> allOff(ch);
                    default -> {}
                }
            }
            default -> {}
        }
    }

    private void noteOff(int ch, int note) {
        if (keys[ch][note]) {
            keys[ch][note] = false;
            if (--held[ch] <= 0) {
                held[ch] = 0;
            } else if (notes[ch] == note) {
                // the display shows one key a channel: the highest one still down
                for (int k = 127; k >= 0; k--) {
                    if (keys[ch][k]) {
                        notes[ch] = k;
                        break;
                    }
                }
            }
        }
    }

    private void allOff(int ch) {
        Arrays.fill(keys[ch], false);
        held[ch] = 0;
    }

    /** a key of the channel is down */
    public boolean sounding(int ch) {
        return held[ch] > 0;
    }

    public boolean key(int ch, int note) {
        return keys[ch][note];
    }

    public int note(int ch) {
        return notes[ch];
    }

    public int velocity(int ch) {
        return velocities[ch];
    }

    public int strikes(int ch) {
        return strikes[ch];
    }

    public int program(int ch) {
        return programs[ch];
    }

    public int bank(int ch) {
        return banks[ch];
    }

    public int volume(int ch) {
        return volumes[ch];
    }

    public int expression(int ch) {
        return expressions[ch];
    }

    public int pan(int ch) {
        return pans[ch];
    }

    public int modulation(int ch) {
        return modulations[ch];
    }

    /** 0 ~ 0x3fff, 0x2000 is the centre */
    public int bend(int ch) {
        return bends[ch];
    }
}
