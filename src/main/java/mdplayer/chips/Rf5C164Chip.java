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
import mdsound.instrument.ScdPcmInst;


/**
 * Rf5C164Chip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class Rf5C164Chip extends BaseChip {

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {ScdPcmInst.class};
    }

    public void writePcm(int chipId, int offset, int length, byte[] buf, int srcOffset, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel)
            context.mds.inst(ScdPcmInst.class).writePcm(chipId, buf, offset, length, srcOffset);

        dumpData(model, "PCMData(8BitMonoSigned)", srcOffset, buf, length);
    }

    public void write(int chipId, int adr, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
            context.mds.write(inst(chipId), chipId, 0, adr, data);
        }
    }

    public void writeMemory(int chipId, int offset, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel)
            context.mds.inst(ScdPcmInst.class).writeMemory(chipId, offset, data);
    }

    @Override
    public Map<String, Object> getInfo(int chipId) {
        ScdPcmInst inst = context.mds.inst(ScdPcmInst.class);
        return inst == null ? Collections.emptyMap() : inst.getView(chipId, "info");
    }
}
