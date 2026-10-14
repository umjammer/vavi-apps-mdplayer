/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.util.Collections;
import java.util.Map;

import mdplayer.Common.EnmModel;
import mdsound.Instrument;
import mdsound.instrument.Msm5205Inst;


/**
 * OKI MSM5205 ADPCM.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-24 nsano initial version <br>
 */
public class Msm5205Chip extends BaseChip {

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Msm5205Inst.class};
    }

    /** @param port 0: reset, 1: data, 2: VCK, 4: prescaler, 5: bit width */
    public void write(int chipId, int port, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
            context.mds.write(inst(chipId), chipId, 0, port, data);
        }
    }

    /**
     * {@code masterClock}, {@code rate}, {@code reset}, {@code mute}, {@code signal} (12 bit),
     * {@code step} (0..48), {@code data} (the last nibble), {@code bitWidth} and
     * {@code idleSamples}, the output samples since a nibble was last decoded.
     */
    @Override
    public Map<String, Object> getInfo(int chipId) {
        Msm5205Inst inst = context.mds.inst(Msm5205Inst.class);
        return inst == null ? Collections.emptyMap() : inst.getView(chipId, "info");
    }
}
