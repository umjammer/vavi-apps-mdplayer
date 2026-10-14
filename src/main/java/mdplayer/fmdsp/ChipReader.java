/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.ChipRegister;
import mdplayer.chips.BaseChip;


/**
 * What every reader of a real chip shares: where the chip is, and which of it this reader shows.
 * <p>
 * A VGM may declare two of most chips - a Sega System 18 board is two YM3438s, and both of them
 * play - so {@code ChipFmDspSource} builds a second reader of every kind and points it at chip 1
 * with {@link #chipId(int)}. Everything a subclass reads out of {@link ChipRegister} is then read
 * at {@link #chipId} rather than at 0, and the second reader stays {@linkplain #ready not ready},
 * costing nothing, for the songs that declare one chip.
 * <p>
 * Readers whose state lives on a driver rather than on a chip - the trackers, the MIDI one - keep
 * implementing {@link FmDspChipReader} directly; there is nothing of theirs to double.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-08-04 nsano initial version <br>
 */
public abstract class ChipReader implements FmDspChipReader {

    protected ChipRegister chipRegister;

    /** which of the chips the song declared this reader shows, 0 unless it is a second reader */
    protected int chipId;

    /** the chip this reader shows, null when the register holds none of it */
    protected abstract BaseChip chip();

    @Override
    public Class<? extends mdplayer.Chip> chipClass() {
        BaseChip chip = chipRegister != null ? chip() : null;
        return chip != null ? chip.getClass() : null;
    }

    @Override
    public void bind(ChipRegister chipRegister) {
        this.chipRegister = chipRegister;
    }

    @Override
    public int chipId() {
        return chipId;
    }

    @Override
    public boolean chipId(int chipId) {
        this.chipId = chipId;
        return true;
    }

    /** channel {@code ch} of the chip, the rows of a group being its channels in order */
    @Override
    public BaseChip.Channel muteChannel(Group group, int ch) {
        BaseChip chip = chipRegister != null ? chip() : null;
        return chip != null && ch >= 0 ? chip.channel(chipId, ch) : null;
    }

    /**
     * The chip is there and the song declared as many of it as this reader's {@link #chipId} - the
     * caches are sized for two whatever the song does, so a second reader has to ask.
     */
    @Override
    public boolean ready() {
        if (chipRegister == null) return false;
        BaseChip chip = chip();
        return chip != null && (chipId == 0 || chip.instances() > chipId);
    }
}
