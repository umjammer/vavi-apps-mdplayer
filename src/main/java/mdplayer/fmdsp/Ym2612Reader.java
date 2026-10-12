/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Map;

import mdplayer.chips.BaseChip;
import mdplayer.chips.Ym2612Chip;


/**
 * OPN2: FM 1-6 with the ch3 slots. The DAC channel has no readable state, it stays dark.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class Ym2612Reader extends OpnFmReader {

    @Override
    protected Ym2612Chip chip() {
        return chipRegister.chip(Ym2612Chip.class);
    }

    /** the chip's own registers and key states, read back once a frame */
    private int[][] regs;
    private int[] keys;

    private static final int[][] noRegs = {new int[0x100], new int[0x100]};
    private static final int[] noKeys = new int[6];

    @Override protected int[][] ports() { return regs != null ? regs : noRegs; }

    @Override protected int[] keyOns() { return keys != null ? keys : noKeys; }

    @Override
    public void poll() {
        regs = null;
        keys = null;
        try {
            Map<String, Object> info = chip().getInfo(chipId);
            if (!info.isEmpty()) {
                if (info.get("register") instanceof int[][] r) regs = r;
                if (info.get("keyOn") instanceof int[] k) keys = playerKeys(k);
            }
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
        }
        super.poll();
    }

    /**
     * The chip reports which slots are down, as bits 4 to 7. The rows want a channel that is
     * sounding to have bit 0 as well - and channel 3 in its extended mode keeps reading the slot
     * bits, so both have to survive.
     */
    private static int[] playerKeys(int[] slots) {
        int[] keys = new int[slots.length];
        for (int ch = 0; ch < slots.length; ch++) {
            keys[ch] = slots[ch] | (slots[ch] != 0 ? 1 : 0);
        }
        return keys;
    }

    /** the OPN2's usual 7.67 MHz NTSC clock */
    @Override protected double fnumK() { return 7670454.0 / 144; }

    @Override public String chipName() { return "OPN2"; }

    @Override public int priority() { return 20; }

    @Override
    protected BaseChip.Channel fmMute(int ch) {
        // the chip counts the ch3 slots as 6-8, as this does
        return chip().channel(chipId, ch);
    }
}
