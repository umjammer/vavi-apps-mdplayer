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
import mdsound.instrument.Ppz8Inst;


/**
 * FMP/PMD.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class Ppz8Chip extends BaseChip {

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Ppz8Inst.class};
    }

    @Override
    public Map<String, Object> getInfo(int chipId) {
        Ppz8Inst inst = context.mds.inst(Ppz8Inst.class);
        return inst == null ? Collections.emptyMap() : inst.getView(chipId, "info");
    }

    public void writePcm(int chipId, int bank, int mode, byte[][] pcmData, EnmModel model) {
        if (model != EnmModel.VirtualModel)
            return;

        fireEventHappened("led.on", chipId);

        context.mds.inst(Ppz8Inst.class).writePcm(chipId, bank, mode, pcmData);
    }

    public void write(int chipId, int dPort, int dAddr, int dData, EnmModel model) {
        if (model != EnmModel.VirtualModel)
            return;

        fireEventHappened("led.on", chipId);

        if (dPort == -1 && dAddr == -1 && dData == -1)
            return;
        context.mds.inst(inst(chipId)).write(chipId, dPort, dAddr, dData);
    }
}
