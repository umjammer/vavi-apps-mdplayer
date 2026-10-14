/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.Ym3812Chip;


/**
 * OPL2.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class Ym3812Reader extends OplReader {

    @Override
    protected Ym3812Chip chip() {
        return chipRegister.chip(Ym3812Chip.class);
    }

    @Override public String chipName() { return "OPL2"; }

    @Override public int priority() { return 70; }
}
