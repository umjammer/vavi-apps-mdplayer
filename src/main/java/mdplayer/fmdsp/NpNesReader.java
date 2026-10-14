/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.NpNesChip;


/**
 * The NES APU as an NSF drives it, inside the machine the driver runs rather than through writes
 * the player makes. The registers are the same emulator's, so only where they are fetched from
 * differs from {@link NesReader}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 * @see NesApuReader for what the registers mean
 */
public class NpNesReader extends NesApuReader {

    private NpNesChip chip() {
        return chipRegister.chip(NpNesChip.class);
    }

    @Override
    public String chipName() {
        return "NES";
    }

    @Override
    public boolean ready() {
        return chipRegister != null && chip() != null;
    }

    @Override
    protected int[] apuRegisters() {
        return (int[]) chip().getInfo(0).getOrDefault("register", new int[0x20]);
    }

    @Override
    protected int[] dmcRegisters() {
        return (int[]) chip().getInfo(0).getOrDefault("dmcRegister", new int[0x20]);
    }

    @Override
    protected int[] levels() {
        return (int[]) chip().getInfo(0).get("levels");
    }

    @Override
    protected BaseChip.Channel mute(int ch) {
        // the pulses are the APU's own; the rest are counted from zero by the delta PCM chip
        return ch < 2 ? chip().channel(0, ch) : chipRegister.chip(NpNesChip.DmcChip.class).channel(0, ch - 2);
    }
}
