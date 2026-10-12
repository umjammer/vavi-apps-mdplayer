/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.Ym3526Chip;


/**
 * OPL.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class Ym3526Reader extends OplReader {

    @Override
    protected Ym3526Chip chip() {
        return chipRegister.chip(Ym3526Chip.class);
    }

    @Override public String chipName() { return "OPL"; }

    @Override public int priority() { return 75; }
}
