/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.util.Collections;
import java.util.Map;

import mdplayer.Common.EnmModel;
import mdplayer.RealChip.RC86ctlSoundChip;
import mdplayer.RealChip.RSoundChip;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdsound.Instrument;
import mdsound.instrument.Ay8910Inst;
import mdsound.instrument.MameAy8910Inst;
import mdsound.instrument.NpYm2149Inst;


/**
 * Ay8910Chip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class Ay8910Chip extends BaseChip {

    private final Setting.ChipType2[] chipTypes = setting.getAY8910Type();

    private final RSoundChip[] realChips = {null, null};

    private final int[] fadeoutVolume = {0, 0};

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Ay8910Inst.class, MameAy8910Inst.class, NpYm2149Inst.class};
    }

    @Override
    public int activeIndex(int chipId) {
        return chipTypes[chipId].getEnabledId();
    }

    @Override
    public void init(BasePlugin<? extends BaseDriver> context) {
        super.init(context);

        for (int chipId = 0; chipId < 2; chipId++) {

            fadeoutVolume[chipId] = 0;
        }
    }

    public void write(int chipId, int addr, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        // psg level
        if ((addr == 0x08 || addr == 0x09 || addr == 0x0a)) {
            int d = fadeoutVolume[chipId] >> 3;
            data = Math.max(data - d, 0);
        }

        if (model == EnmModel.VirtualModel) {
            if (chipTypes[chipId].getUseReal()[0])
                return;
            context.mds.write(inst(chipId), chipId, 0, addr, data);
        } else {
            if (realChips[chipId] == null)
                return;
            realChips[chipId].setRegister(addr + 0x000, data);
        }
    }

    private void softReset(int chipId, EnmModel model) {

        // All Channel Key Off
        write(chipId, 0x07, 0x00, model);

        // Volume Off
        for (int ch = 0; ch < 3; ch++) {
            write(chipId, 0x8 + ch, 0x00, model);
        }

        // Noise Initialization
        write(chipId, 0x06, 0x00, model);
        // Envelope Initialization
        write(chipId, 0x0b, 0x00, model);
        write(chipId, 0x0c, 0x00, model);
        write(chipId, 0x0d, 0x00, model);
    }

    @Override
    public void setFadeout(int chipId, int v) {
        fadeoutVolume[chipId] = v;
        // the levels were re-sent here for the real chip only, which no longer has a path
    }

    public void writeClock(int chipId, int clock, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
        } else {
            if (realChips != null && realChips[chipId] != null) {
                if (realChips[chipId] instanceof RC86ctlSoundChip) {
//                    Nc86ctl.ChipType ct = ((RC86ctlSoundChip) realChips[chipId]).ChipType;
//                    // If YM2149 is selected, double the frequency
//                    if (ct == Nc86ctl.ChipType.CHIP_YM2149) {
//                        clock *= 2;
//                    }
                }
                realChips[chipId].dClock = realChips[chipId].setMasterClock(clock);
            }
        }
    }

    @Override
    public Map<String, Object> getInfo(int chipId) {
        Instrument inst = context.mds.inst(inst(chipId));
        if (inst == null) return Collections.emptyMap();
        // getView already answers a {"register": int[]} map; don't wrap it in another
        return inst.getView(chipId, "register");
    }

    @Override
    public void softReset(EnmModel model) {
        softReset(0, model);
        softReset(1, model);
    }

    @Override
    public void clearFadeout() {
        setFadeout(0, 0);
        setFadeout(1, 0);
    }
}
