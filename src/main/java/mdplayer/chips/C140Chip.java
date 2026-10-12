/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.util.Collections;
import java.util.Map;

import mdplayer.Common.EnmModel;
import mdplayer.RealChip.RSoundChip;
import mdplayer.Setting;
import mdsound.Instrument;
import mdsound.Instrument.PcmEnabledInstrument;
import mdsound.chips.C140;
import mdsound.instrument.C140Inst;
import mdsound.instrument.C219Inst;


/**
 * C140Chip.
 * <p>
 * system property
 * <li>{@code mdplayer.variant.c140} ... active chip index</li>
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class C140Chip extends BaseChip {

    private final Setting.ChipType2[] chipTypes = setting.getC140Type();

    private final RSoundChip[] realChips = {null, null};

    @SuppressWarnings("unchecked")
    private Class<? extends PcmEnabledInstrument> _inst(int chipId) {
        return (Class<? extends PcmEnabledInstrument>) inst(chipId);
    }

    @Override
    public int activeIndex(int chipId) {
        return chipTypes[chipId].getEnabledId();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {C140Inst.class, C219Inst.class};
    }

    /** {@link #write} keeps the key ons of a muted channel from the chip */
    @Override
    protected void applyMute(int chipId, int ch) {
    }

    public void write(int chipId, int adr, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if ((model == EnmModel.VirtualModel && (chipTypes[chipId] == null || !chipTypes[chipId].getUseReal()[0])) ||
                (model == EnmModel.RealModel && (realChips != null && realChips[chipId] != null))) {
            int ch = adr >> 4;
            // a muted channel never gets its key on through
            if ((adr & 0xf) == 0x05 && (data & 0x80) != 0 && isMuted(chipId, ch)) {
                data &= 0x7f;
            }
        }

        if (model == EnmModel.VirtualModel) {
            if (chipTypes[chipId] == null || !chipTypes[chipId].getUseReal()[0])
                context.mds.write(inst(chipId), chipId, 0, adr, data);
        } else {
            if (realChips != null && realChips[chipId] != null)
                realChips[chipId].setRegister(adr, data);
        }
    }

    public void writePcm(int chipId, int romSize, int offset, int length, byte[] buf, int srcOffset, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel)
            context.mds.inst(_inst(chipId)).writePcm(chipId, buf, offset, length, srcOffset, romSize);
        else {
            if (realChips != null && realChips[chipId] != null) {
                // Start address setting
                realChips[chipId].setRegister(0x1_0000, offset);
                realChips[chipId].setRegister(0x1_0001, offset >> 8);
                realChips[chipId].setRegister(0x1_0002, offset >> 16);
                // Data Transfer
                for (int i = 0; i < length; i++) {
                    realChips[chipId].setRegister(0x1_0004, buf[srcOffset + i]);
                }
//                realChips[chipId].setRegister(0x10006, romSize);

                context.chipRegister.plugin(RealChipPlugin.class).realChip.sendData();
            }
        }

        dumpData(model, "PCMData", srcOffset, buf, length);
    }

    public void writeType(int chipId, int type, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
        } else {
            if (realChips != null && realChips[chipId] != null) {
                switch (C140.Type.valueOf(type)) {
                    case SYSTEM2:
                        realChips[chipId].setRegister(0x1_0008, 0);
                        break;
                    case SYSTEM21:
                        realChips[chipId].setRegister(0x1_0008, 1);
                        break;
                    case ASIC219:
                        realChips[chipId].setRegister(0x1_0008, 2);
                        break;
                }
            }
        }
    }

    /**
     * The register file and the key state as the chip has them now. Its key clears itself when a
     * sample that does not loop runs out, which a copy taken on the way in could never show.
     */
    @Override
    public Map<String, Object> getInfo(int chipId) {
        Instrument inst = context.mds.inst(_inst(chipId));
        if (inst == null) return Collections.emptyMap();
        return inst.getView(chipId, "info");
    }
}
