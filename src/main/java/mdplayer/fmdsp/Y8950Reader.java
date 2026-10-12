/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.Y8950Chip;


/**
 * Y8950 (MSX-Audio), the OPL with an ADPCM section - only the FM part is read.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class Y8950Reader extends OplReader {

    @Override
    protected Y8950Chip chip() {
        return chipRegister.chip(Y8950Chip.class);
    }

    @Override public String chipName() { return "Y895"; }

    @Override public int priority() { return 76; }
}
