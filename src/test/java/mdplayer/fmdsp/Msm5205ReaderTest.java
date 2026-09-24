/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Msm5205ReaderTest.
 */
class Msm5205ReaderTest {

    @Test
    void testFed() {
        assertTrue(Msm5205Reader.fed(false, 0, 8000));
        assertTrue(Msm5205Reader.fed(false, 399, 8000)); // just under 50 ms
        assertFalse(Msm5205Reader.fed(false, 400, 8000));
        assertFalse(Msm5205Reader.fed(false, Integer.MAX_VALUE, 8000)); // never fed
        assertFalse(Msm5205Reader.fed(true, 0, 8000)); // held reset
        assertFalse(Msm5205Reader.fed(false, 0, 0));
    }
}
