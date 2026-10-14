/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.NpNesChip;


/**
 * Konami VRC6, an NSF expansion: two pulses and a saw.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 * @see NsfTrackInfoReader for what the fields mean
 */
public class Vrc6Reader extends NsfTrackInfoReader {

    @Override
    public String chipName() {
        return "VRC6";
    }

    @Override
    public int priority() {
        return 86;
    }

    @Override
    protected int channelCount() {
        return 3;
    }

    @Override
    protected BaseChip chip() {
        return chipRegister.chip(NpNesChip.Vrc6Chip.class);
    }
}
