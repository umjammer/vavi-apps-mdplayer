/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer;

import mdplayer.Common.EnmModel;
import mdplayer.chips.Ay8910Chip;
import mdplayer.chips.BaseChip;
import mdplayer.chips.Saa1099Chip;
import mdplayer.chips.Sn76489Chip;
import mdplayer.chips.Ym2413Chip;
import mdsound.Instrument;
import mdsound.instrument.Ay8910Inst;
import mdsound.instrument.Saa1099Inst;
import mdsound.instrument.Sn76489Inst;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * A muted channel goes quiet on the emulator, whichever of the two ways its chip mutes: the
 * instrument told the mask, or the levels kept out of the register writes.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-12 nsano initial version <br>
 */
class ChipMuteTest {

    static final int N = 4410;

    /** the chips are singletons, and their mutes outlive a test: channel 0 is unmuted after each */
    BaseChip used;

    @AfterEach
    void unmute() {
        if (used != null) used.setMuted(0, 0, false);
    }

    /**
     * Peak to peak of the left output over {@link #N} samples, the first tenth left out. A sample
     * at a time, the way {@code MDSound} has them: some render no more than one a call.
     */
    static int swing(Instrument inst) {
        int[][] out = new int[2][1];
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < N; i++) {
            inst.update(0, out, 1);
            if (i < N / 10) continue;
            min = Math.min(min, out[0][0]);
            max = Math.max(max, out[0][0]);
        }
        return max - min;
    }

    /** channel 0 at full level, a tone in the middle of the range */
    static void saaTone(Saa1099Chip chip) {
        chip.write(0, 0x00, 0xff, EnmModel.VirtualModel); // amplitude
        chip.write(0, 0x08, 0x80, EnmModel.VirtualModel); // frequency
        chip.write(0, 0x10, 0x04, EnmModel.VirtualModel); // octave
        chip.write(0, 0x14, 0x01, EnmModel.VirtualModel); // tone on
        chip.write(0, 0x1c, 0x01, EnmModel.VirtualModel); // sound on
    }

    @Test
    @DisplayName("the instrument mutes: saa1099")
    void testInstrument() {
        Saa1099Inst inst = new Saa1099Inst();
        EmulatedPlugin plugin = EmulatedPlugin.of(inst);
        Saa1099Chip chip = plugin.chipRegister.chip(Saa1099Chip.class);
        used = chip;
        saaTone(chip);
        assertTrue(swing(inst) > 0x100, "silent before the mute");

        chip.setMuted(0, 0, true);
        assertEquals(0, swing(inst));

        chip.setMuted(0, 0, false);
        assertTrue(swing(inst) > 0x100, "silent after the mute");
    }

    @Test
    @DisplayName("a new song's instrument is handed the mutes the chip kept")
    void testRestore() {
        Saa1099Chip chip = EmulatedPlugin.of(new Saa1099Inst()).chipRegister.chip(Saa1099Chip.class);
        used = chip;
        chip.setMuted(0, 0, true);

        // the next song: the same chip, a new emulator
        Saa1099Inst inst = new Saa1099Inst();
        EmulatedPlugin plugin = EmulatedPlugin.of(inst);
        saaTone(chip);
        assertTrue(swing(inst) > 0x100, "a new emulator knows nothing of the mute");

        plugin.chipRegister.restoreMutes();
        assertEquals(0, swing(inst));
    }

    @Test
    @DisplayName("the register writes mute: sn76489, a held note goes quiet at once")
    void testRegisters() {
        ChipFmDspSourceTest.useEmulator(Setting.getInstance().getSN76489Type());
        Sn76489Inst inst = new Sn76489Inst();
        EmulatedPlugin plugin = EmulatedPlugin.of(inst);
        Sn76489Chip chip = plugin.chipRegister.chip(Sn76489Chip.class);
        used = chip;
        chip.write(0, 0x80 | 0x0e, EnmModel.VirtualModel); // tone 0 period, low
        chip.write(0, 0x0f, EnmModel.VirtualModel); // and high
        chip.write(0, 0x90, EnmModel.VirtualModel); // tone 0 at full level
        assertTrue(swing(inst) > 0x100, "silent before the mute");

        chip.setMuted(0, 0, true);
        assertEquals(0, swing(inst));

        chip.setMuted(0, 0, false);
        assertTrue(swing(inst) > 0x100, "silent after the mute");
    }

    @Test
    @DisplayName("the register writes mute: ay8910, re-sent from the emulator's own registers")
    void testRegistersReadBack() {
        ChipFmDspSourceTest.useEmulator(Setting.getInstance().getAY8910Type());
        Ay8910Inst inst = new Ay8910Inst();
        EmulatedPlugin plugin = EmulatedPlugin.of(inst);
        Ay8910Chip chip = plugin.chipRegister.chip(Ay8910Chip.class);
        used = chip;
        chip.write(0, 0x00, 0x80, EnmModel.VirtualModel); // tone A period
        chip.write(0, 0x01, 0x00, EnmModel.VirtualModel);
        chip.write(0, 0x07, 0x3e, EnmModel.VirtualModel); // tone A on, the rest off
        chip.write(0, 0x08, 0x0f, EnmModel.VirtualModel); // level A full
        assertTrue(swing(inst) > 0x100, "silent before the mute");

        chip.setMuted(0, 0, true);
        assertEquals(0, swing(inst));

        chip.setMuted(0, 0, false);
        assertTrue(swing(inst) > 0x100, "silent after the mute");
    }

    @Test
    @DisplayName("the register writes mute: ym2413, a held note keyed on again as it is un-muted")
    void testKeyRegisters() throws Exception {
        ChipFmDspSourceTest.useEmulator(Setting.getInstance().getYM2413Type());
        Ym2413Chip chip = EmulatedPlugin.of().chipRegister.chip(Ym2413Chip.class);
        // whichever of the four OPLLs the chip has picked: another test may have loaded settings
        // the chip singleton still holds an older copy of
        Instrument inst = chip.inst(0).getDeclaredConstructor().newInstance();
        EmulatedPlugin.of(inst);
        used = chip;
        chip.write(0, 0x30, 0x30, EnmModel.VirtualModel); // organ, full level
        chip.write(0, 0x10, 0xac, EnmModel.VirtualModel); // f-number
        chip.write(0, 0x20, 0x10 | (4 << 1), EnmModel.VirtualModel); // key on, block 4
        assertTrue(swing(inst) > 0x100, "silent before the mute");

        chip.setMuted(0, 0, true);
        swing(inst); // the release
        swing(inst);
        assertTrue(swing(inst) < 0x10, "still sounding after the mute");

        chip.setMuted(0, 0, false);
        assertTrue(swing(inst) > 0x100, "silent after the mute");
    }
}
