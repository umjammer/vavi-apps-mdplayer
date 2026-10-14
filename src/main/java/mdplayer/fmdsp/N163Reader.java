/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.NpNesChip;


/**
 * Namco 163, an NSF expansion: up to eight wavetable channels, which share one time slot each so
 * the more a song enables the slower they all run.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 * @see NsfTrackInfoReader for what the fields mean
 */
public class N163Reader extends NsfTrackInfoReader {

    @Override
    public String chipName() {
        return "N163";
    }

    @Override
    public int priority() {
        return 85;
    }

    @Override
    protected int channelCount() {
        return 8;
    }

    @Override
    protected BaseChip chip() {
        return chipRegister.chip(NpNesChip.N163Chip.class);
    }
}
