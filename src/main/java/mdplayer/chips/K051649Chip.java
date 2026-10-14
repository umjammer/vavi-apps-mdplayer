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
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdsound.Instrument;
import mdsound.chips.K051649;
import mdsound.instrument.K051649Inst;


/**
 * K051649Chip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class K051649Chip extends BaseChip {

    private final Setting.ChipType2[] chipTypes = setting.getK051649Type();

    private final RSoundChip[] realChips = {null, null};

    @Deprecated
    private final K051649 scc_k051649 = new K051649();

    @Deprecated
    private int sccR_port;

    @Deprecated
    private int sccR_offset;

    @Deprecated
    private int sccR_dat;

    @Deprecated
    public final byte[] keyOnOff = {
            0, 0
    };

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {K051649Inst.class};
    }

    @Override
    public void init(BasePlugin<? extends BaseDriver> context) {
        super.init(context);

        K051649Inst chip = Instrument.getInstrument(K051649Inst.class); // ugly
        chip.start(0, 100, 200);
        chip.start(1, 100, 200);
    }

    /** the key on register is filtered on its way in, see {@link #write} */
    @Override
    protected void applyMute(int chipId, int ch) {
        if (ch < 0) return;
        // re-send the key ons the chip already has, so the mute takes on the running note
        write(chipId, (3 << 1) | 1, keyOnOff[chipId], EnmModel.VirtualModel);
    }

    public void write(int chipId, int adr, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if ((adr & 1) != 0) {
            if ((adr >> 1) == 3) { // keyonoff
                keyOnOff[chipId] = (byte) data;
                data &= isMuted(chipId, 0) ? 0xfe : 0xff;
                data &= isMuted(chipId, 1) ? 0xfd : 0xff;
                data &= isMuted(chipId, 2) ? 0xfb : 0xff;
                data &= isMuted(chipId, 3) ? 0xf7 : 0xff;
                data &= isMuted(chipId, 4) ? 0xef : 0xff;
            }
        }

        if (model == EnmModel.VirtualModel) {
            if (!chipTypes[chipId].getUseReal()[0]) {
                context.mds.write(inst(chipId), chipId, 0, adr, data);

                // Save register data
                scc_k051649.write(adr, data);
            }
        } else {
            if (realChips[chipId] == null)
                return;

            // Save register data
            context.mds.inst(inst(chipId)).write(chipId, 0, adr, data);

            if ((adr & 1) == 0) {
                sccR_port = (adr >> 1);
                sccR_offset = data;
            } else {
                sccR_dat = data;

                switch (sccR_port) {
                    case 0x00:
                        sccR_offset += 0x00;
                        break;
                    case 0x01:
                        sccR_offset += 0x80;
                        break;
                    case 0x02:
                        sccR_offset += 0x8a;
                        break;
                    case 0x03:
                        sccR_offset += 0x8f;
                        break;
                }

                realChips[chipId].setRegister(setting.getDebug_SCCbaseAddress() | sccR_offset, sccR_dat);
            }
        }
    }

    private void softReset(int chipId, EnmModel model) {
        // All channel volume zero
        for (int i = 0; i < 5; i++) {
            write(chipId, (0x00 << 1) + 0, i, model);
            write(chipId, (0x02 << 1) + 1, 0x00, model);
            write(chipId, (0x00 << 1) + 0, i, model);
            write(chipId, (0x03 << 1) + 1, 0x00, model);
        }
    }

    @Override
    public Map<String, Object> getInfo(int chipId) {
        K051649Inst inst = context.mds.inst(K051649Inst.class);
        return inst == null ? Collections.emptyMap() : inst.getView(chipId, "info");
    }

    @Override
    public void softReset(EnmModel model) {
        softReset(0, model);
        softReset(1, model);
    }
}
