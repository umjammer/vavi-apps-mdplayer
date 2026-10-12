/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.util.Collections;
import java.util.Map;

import mdplayer.Common.EnmModel;
import mdsound.Instrument;
import mdsound.instrument.Saa1099Inst;


/**
 * Saa1099Chip.
 * <p>
 * The chip decodes its registers into channel state as they are written and keeps no copy of the
 * registers themselves, so {@link #getInfo} asks the emulator for that state instead of shadowing
 * the writes. Muting goes through the emulator's own mute mask for the same reason: dropping the
 * amplitude on the way in would leave the view unable to tell a muted channel from a silent one.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class Saa1099Chip extends BaseChip {

    private static final int CHANNELS = 6;

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Saa1099Inst.class};
    }

    /** @param address the chip register, 0x00 - 0x1f; the parameter is named for the write port */
    public void write(int chipId, int address, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
            context.mds.write(inst(chipId), chipId, 0, address, data);
        }
    }

    /**
     * The channel state as the chip has it now: {@code channels.<ch>.volumeL} and {@code volumeR}
     * as amplitude nibbles, {@code frequency}, {@code octave}, {@code tone}, {@code noise} and
     * {@code mute}, with {@code enabled} for the whole chip.
     */
    @Override
    public Map<String, Object> getInfo(int chipId) {
        Saa1099Inst inst = context.mds.inst(Saa1099Inst.class);
        return inst == null ? Collections.emptyMap() : inst.getView(chipId, "channels");
    }
}
