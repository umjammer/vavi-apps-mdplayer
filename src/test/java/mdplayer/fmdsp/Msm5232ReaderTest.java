/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Msm5232ReaderTest.
 */
class Msm5232ReaderTest {

    static final int CLOCK = 2119040;

    @Test
    void testFrequencyOf() {
        assertEquals(440, Msm5232Reader.frequencyOf(0x21, CLOCK), 1e-9);
        assertEquals(880, Msm5232Reader.frequencyOf(0x21 + 12, CLOCK), 1e-9);
        assertEquals(880, Msm5232Reader.frequencyOf(0x21, CLOCK * 2), 1e-9); // the clock transposes
        assertEquals(-1, Msm5232Reader.frequencyOf(0x55, CLOCK)); // off the scale
        assertEquals(-1, Msm5232Reader.frequencyOf(-1, CLOCK)); // never keyed
    }

    @Test
    void testNote() {
        assertEquals(57, Notes.noteOf(Msm5232Reader.frequencyOf(0x21, CLOCK))); // A4, semitones above C0
        assertEquals(48, Notes.noteOf(Msm5232Reader.frequencyOf(0x21 - 9, CLOCK))); // C4
    }
}
