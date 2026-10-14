/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.util.Collections;
import java.util.Map;

import mdplayer.Common.EnmModel;
import mdplayer.Setting;
import mdsound.Instrument;
import mdsound.instrument.HuC6280Inst;


/**
 * HuC6280Chip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class HuC6280Chip extends BaseChip {

    private final Setting.ChipType2[] chipTypes = setting.getHuC6280Type();

    @Deprecated
    private final int[] currentCh = {
            0, 0
    };

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {HuC6280Inst.class};
    }

    public void write(int chipId, int addr, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
            if (!chipTypes[chipId].getUseReal()[0]) {
                if (addr == 0) {
                    currentCh[chipId] = data & 7;
                }
                if (addr == 4) {
                    data = isMuted(chipId, currentCh[chipId]) ? 0 : data;
                }
//logger.log(Level.TRACE, "chipId:%d adr:%d Dat:%d".formatted(chipId, addr, data));
                context.mds.write(inst(chipId), chipId, 0, addr, data);
            }
        } else {
//            if (scHuC6280[chipId] == null) return;
        }
    }

    public int read(int chipId, int adr, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
            return context.mds.inst(inst(chipId)).read(chipId, adr);
        }

        return 0;
    }

    /** {@link #write} keeps the level of a muted channel from the chip */
    @Override
    protected void applyMute(int chipId, int ch) {
    }

    @Override
    public Map<String, Object> getInfo(int chipId) {
        HuC6280Inst inst = context.mds.inst(HuC6280Inst.class);
        return inst == null ? Collections.emptyMap() : inst.getView(chipId, "info");
    }
}
