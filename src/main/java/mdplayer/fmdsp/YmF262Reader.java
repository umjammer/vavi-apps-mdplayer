/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.YmF262Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;


/**
 * OPL3: the first bank's nine channels - the fmdsp view has no rows for the other nine.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class YmF262Reader extends OplReader {

    @Override
    protected YmF262Chip chip() {
        return chipRegister.chip(YmF262Chip.class);
    }

    /** the OPL3's two output enables, which are all the panning it has */
    @Override
    protected Pan pan(int ch) {
        boolean left = boolOf(ch, "panL");
        boolean right = boolOf(ch, "panR");
        if (left && right) return Pan.CENTER;
        if (left) return Pan.LEFT;
        if (right) return Pan.RIGHT;
        return Pan.NONE;
    }

    @Override public String chipName() { return "OPL3"; }

    @Override public int priority() { return 60; }
}
