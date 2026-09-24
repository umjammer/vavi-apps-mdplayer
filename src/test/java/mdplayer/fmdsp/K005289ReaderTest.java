/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * K005289ReaderTest.
 */
class K005289ReaderTest {

    static final int CLOCK = 3579545;

    @Test
    void testFrequencyOf() {
        assertEquals(CLOCK / 32.0, K005289Reader.frequencyOf(0, CLOCK), 1e-9);
        assertEquals(CLOCK / 64.0, K005289Reader.frequencyOf(1, CLOCK), 1e-9);
    }

    @Test
    void testNote() {
        // 3579545 / 32 / 440 = 254.2, so a period of 254 clocks a step is A4
        assertEquals(57, Notes.noteOf(K005289Reader.frequencyOf(253, CLOCK)));
    }
}
