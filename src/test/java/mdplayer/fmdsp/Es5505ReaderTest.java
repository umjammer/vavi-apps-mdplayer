/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Es5505ReaderTest.
 */
class Es5505ReaderTest {

    /** the same values the emulator's volume table has for an ES5505 */
    @Test
    void testLevel() {
        assertEquals(0, Es5505Reader.level(0x00));
        assertEquals(0, Es5505Reader.level(0x0f)); // below 0x10 is silent
        assertEquals(1, Es5505Reader.level(0x10));
        assertEquals(16384, Es5505Reader.level(0xf0));
        assertEquals(31744, Es5505Reader.level(0xff));
        for (int v = 1; v < 0x100; v++)
            assertTrue(Es5505Reader.level(v) >= Es5505Reader.level(v - 1), "not monotonic at " + v);
    }
}
