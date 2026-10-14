/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import mdplayer.Common.EnmModel;
import mdplayer.RealChip.RSoundChip;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdsound.Instrument;
import mdsound.instrument.Ym3526Inst;


/**
 * Ym3526Chip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class Ym3526Chip extends BaseChip {

    private final Setting.ChipType2[] chipTypes = setting.getYM3526Type();

    private final RSoundChip[] realChips = {null, null};

    private final int[] fadeout = {0, 0};

    @Deprecated
    private final ChipKeyInfo[] keyInfo = {
            new ChipKeyInfo(14), new ChipKeyInfo(14)
    };

    /**
     * The registers as they were written. The visualizer does not read this - it asks the chip -
     * but the register dump and the instrument export need a raw file the OPL core cannot give.
     */
    @Deprecated
    private final int[][] register = {new int[0x100], new int[0x100]};

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Ym3526Inst.class};
    }

    @Override
    public void init(BasePlugin<? extends BaseDriver> context) {
        super.init(context);

        for (int chipId = 0; chipId < 2; chipId++) {

            fadeout[chipId] = 0;
        }
    }

    public void write(int chipId, int addr, int data, EnmModel model) {
        //if (chipTypes == null) return;

        fireEventHappened("led.on", chipId);

        register[chipId][addr] = data;

        if (addr >= 0x40 && addr <= 0x55) { // TL
            int ksl = data & 0xc0;
            int tl = data & 0x3f;
            int ch = addr - 0x40;
            boolean cr = false;
            int twoOpChannel = (ch / 8) * 3 + ((ch % 8) % 3);

            // Career determination during 2op
            if (ch % 8 > 2)
                cr = true;
            else {
                // the chip knows its own connection and rhythm settings, both written earlier
                cr = isCarrier(chipId, twoOpChannel, (ch % 8) / 3);
            }

            if (ch >= 0x10 && isRhythm(chipId)) {
                cr = true;
            }

            if (cr) {
                data = Math.min(tl + fadeout[chipId], 0x3f);
                data = ksl + (isMuted(chipId, twoOpChannel) ? 0x3f : data);
            }
        }

        /* if (model == EnmModel.VirtualModel) */
        {
            if (addr >= 0xb0 && addr <= 0xb8) {
                int ch = addr - 0xb0;
                int k = (data >> 5) & 1;
                if (k == 0) {
                    keyInfo[chipId].on[ch] = false;
                    keyInfo[chipId].off[ch] = true;
                } else {
                    keyInfo[chipId].on[ch] = true;
                }
                if (isMuted(chipId, ch))
                    data &= 0x1f;
            }

            if (addr == 0xbd) {

                for (int c = 0; c < 5; c++) {
                    if ((data & (0x10 >> c)) == 0) {
                        keyInfo[chipId].off[c + 9] = true;
                    } else {
                        if (keyInfo[chipId].off[c + 9])
                            keyInfo[chipId].on[c + 9] = true;
                        keyInfo[chipId].off[c + 9] = false;
                    }
                }

                if (isMuted(chipId, 9))
                    data &= 0xef;
                if (isMuted(chipId, 10))
                    data &= 0xf7;
                if (isMuted(chipId, 11))
                    data &= 0xfb;
                if (isMuted(chipId, 12))
                    data &= 0xfd;
                if (isMuted(chipId, 13))
                    data &= 0xfe;
            }
        }

        _write(chipId, addr, data, model);
    }

    private ChipKeyInfo getKeyInfo(int chipId) {
        ChipKeyInfo[] keyInfoRet = {new ChipKeyInfo(14), new ChipKeyInfo(14)}; // TODO out for memory usage?
        for (int ch = 0; ch < keyInfo[chipId].off.length; ch++) {
            keyInfoRet[chipId].off[ch] = keyInfo[chipId].off[ch];
            keyInfoRet[chipId].on[ch] = keyInfo[chipId].on[ch];
            keyInfo[chipId].on[ch] = false;
        }
        return keyInfoRet[chipId];
    }

    private void _write(int chipId, int addr, int data, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
            if (!chipTypes[chipId].getUseReal()[0]) {
                context.mds.write(inst(chipId), chipId, 0, addr, data);
            }
        } else {
            if (realChips[chipId] == null)
                return;

            realChips[chipId].setRegister(addr, data);
        }
    }

    private void softReset(int chipId, EnmModel model) {
        // FM All Channel Key Off
        for (int i = 0; i < 9; i++) {
            _write(chipId, 0xb0 + i, 0x00, model);
        }

        // FM TL=127
        for (int i = 0; i < 22; i++) {
            _write(chipId, 0x40 + i, 0x3f, model);
        }

        // SL=15 RR=15
        for (int i = 0; i < 22; i++) {
            _write(chipId, 0x80 + i, 0xff, model);
        }
    }

    /** {@link #write} keeps the key ons of a muted channel from the chip */
    @Override
    protected void applyMute(int chipId, int ch) {
    }

    @Override
    public void setFadeout(int chipId, int v) {
        fadeout[chipId] = v >> 1; // 0-63 (v range: 0-127)
        for (int c = 0; c < 22; c++) {
        }
    }

    public void writeClock(int chipId, int clock, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
        } else {
            if (realChips != null && realChips[chipId] != null) {
//                if (realChips[chipId] instanceof RC86ctlSoundChip
//                        && ((RC86ctlSoundChip) realChips[chipId]).ChipType == Nc86ctl.ChipType.CHIP_OPL3) clock *= 4;
//                realChips[chipId].dClock = realChips[chipId].SetMasterClock((int) clock);
            }
        }
    }

    /**
     * The channel state the visualizer reads, and beside it the registers as they were written.
     * <p>
     * The shared OPL core behind this chip decodes its registers into operators and keeps no file,
     * so the raw registers the dump panel and the instrument export want cannot be read back and
     * are still shadowed here. The channel state comes from the chip.
     */
    @Override
    public Map<String, Object> getInfo(int chipId) {
        Instrument inst = context.mds.inst(inst(chipId));
        if (inst == null) return Collections.emptyMap();
        Map<String, Object> info = new HashMap<>();
        info.put("register", register[chipId]);
        info.put("keyInfo", getKeyInfo(chipId));
        info.putAll(inst.getView(chipId, "info") != null ? inst.getView(chipId, "info") : Collections.emptyMap());
        return info;
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

    /** whether the operator is a carrier, as the chip has its connection set */
    private boolean isCarrier(int chipId, int ch, int slot) {
        Ym3526Inst inst = context.mds.inst(Ym3526Inst.class);
        return inst == null || inst.isCarrier(chipId, ch, slot);
    }

    /** whether the rhythm section is on, as the chip has it */
    private boolean isRhythm(int chipId) {
        Ym3526Inst inst = context.mds.inst(Ym3526Inst.class);
        return inst != null && inst.isRhythm(chipId);
    }
}
