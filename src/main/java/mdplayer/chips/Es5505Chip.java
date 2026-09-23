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
import mdsound.instrument.Es5505Inst;


/**
 * Ensoniq ES5505 "OTIS".
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-24 nsano initial version <br>
 */
public class Es5505Chip extends BaseChip {

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Es5505Inst.class};
    }

    /** @param port bit 7: 16 bit data, bit 6-0: byte offset */
    public void write(int chipId, int port, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
            context.mds.write(inst(chipId), chipId, 0, port, data);
        }
    }

    public void writePcm(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
            context.mds.inst(Es5505Inst.class).writePcm(chipId, romData, dataStart, dataLength, srcStartAdr, romSize);
        }

        dumpData(model, "PCMData", srcStartAdr, romData, dataLength);
    }

    @Override
    public Map<String, Object> getInfo(int chipId) {
        Es5505Inst inst = context.mds.inst(Es5505Inst.class);
        return inst == null ? Collections.emptyMap() : inst.getView(chipId, "info");
    }
}
