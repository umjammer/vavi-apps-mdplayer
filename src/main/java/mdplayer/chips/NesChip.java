/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mdplayer.ChipRegister;
import mdplayer.Common.EnmModel;
import mdsound.Instrument;
import mdsound.instrument.NesInst;
import mdsound.instrument.NesInst.DmcInst;
import mdsound.instrument.NesInst.FdsInst;


/**
 * NsfChip. (for vgm)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class NesChip extends BaseChip {

    // vgm
    public static class DmcChip extends NesChip {

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {DmcInst.class};
        }

        @Override
        public Map<String, Object> getInfo(int chipId) {
            fireEventHappened("led.on", chipId);

//            if (!ctNES[chipId].UseScci) {
            return Map.of("register", context.mds.inst(NesInst.class).readDmc(chipId));
//            }
        }

    }

    // vgm
    public static class FdsChip extends NesChip {

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {FdsInst.class};
        }

        @Override
        public Map<String, Object> getInfo(int chipId) {
            fireEventHappened("led.on", chipId);

//            if (!ctNES[chipId].UseScci) {
            return context.mds.inst(FdsInst.class).getView(chipId, "info");
//            }
        }

    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {NesInst.class};
    }

    /**
     * The one emulator plays all three: the pulses are channels 0-1 of the APU, the triangle, the
     * noise and the delta PCM 0-2 of the DMC and the FDS 0 of its own, and the emulator counts
     * them on as 0-1, 2-4 and 5. Each instrument of the family gets the lot.
     */
    @Override
    protected void applyMute(int chipId, int ch) {
        ChipRegister chips = context.chipRegister;
        long mask = (chips.chip(NesChip.class).muteMask(chipId) & 0x03)
                | (chips.chip(DmcChip.class).muteMask(chipId) & 0x07) << 2
                | (chips.chip(FdsChip.class).muteMask(chipId) & 0x01) << 5;
        for (Class<? extends NesInst> c : List.of(NesInst.class, DmcInst.class, FdsInst.class)) {
            NesInst inst = context.mds.inst(c);
            if (inst != null) inst.setMuteMask(chipId, mask);
        }
    }

    // vgm
    public void write(int chipId, int addr, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
//            if (!ctNES[chipId].UseScci) {
                context.mds.write(inst(chipId), chipId, 0, addr, data);
//            }
        } else {
//            if (scNES[chipId] == null) return;
//
//            scNES[chipId].setRegister(addr, data);
        }
    }

    // vgm
    @Override
    public Map<String, Object> getInfo(int chipId) {
        fireEventHappened("led.on", chipId);

//        if (!ctNES[chipId].UseScci) {
        NesInst inst = context.mds.inst(NesInst.class);
        if (inst == null) return Collections.emptyMap();
        Map<String, Object> info = new HashMap<>();
        info.put("register", inst.readApu(chipId));
        info.put("dmcRegister", inst.readDmc(chipId));
        return info;
//        }
    }

    // vgm

    // vgm
    public void writePcm(int chipId, int stAdr, int dataSize, byte[] vgmBuf, int vgmAdr, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel)
            context.mds.inst(NesInst.class).writePcm(chipId, vgmBuf, vgmAdr, dataSize, stAdr);

        dumpData(model, "PCMData", vgmAdr, vgmBuf, dataSize);
    }

    @Override
    public String getName() {
        return getClass() == NesChip.class ? "Apu" : super.getName(); // considered sub-classes
    }
}
