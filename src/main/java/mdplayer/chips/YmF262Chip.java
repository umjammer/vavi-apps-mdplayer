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
import mdsound.instrument.CozYmF262Inst;
import mdsound.instrument.NukedYmF262Inst;
import mdsound.instrument.YmF262Inst;
import mdsound.instrument.YmFmYmF262Inst;

import static mdplayer.chips.YmF278BChip.channel;


/**
 * YmF262Chip.
 * <p>
 * system property
 * <li>{@code mdplayer.variant.ymf262} ... active chip index</li>
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class YmF262Chip extends BaseChip {

    private final Setting.ChipType2[] chipTypes = setting.getYMF262Type();

    private final RSoundChip[] realChips = {null, null};

    @Deprecated
    public final int[][][] register = {
            {null, null},
            {null, null}
    };

    @Deprecated
    private final int[] registerFm = {0, 0};

    @Deprecated
    private final int[] registerRhythmB = {0, 0};

    @Deprecated
    private final int[] registerRhythm = {0, 0};

    private final int[] fadeout = {0, 0};

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {YmF262Inst.class, YmF262Inst.class, NukedYmF262Inst.class, CozYmF262Inst.class, YmFmYmF262Inst.class};
    }

    @Override
    public int activeIndex(int chipId) {
        return chipTypes[chipId].getEnabledId();
    }

    @Override
    public void init(BasePlugin<? extends BaseDriver> context) {
        super.init(context);

        for (int chipId = 0; chipId < 2; chipId++) {
            register[chipId] = new int[][] {new int[0x100], new int[0x100]};
            for (int i = 0; i < 0x100; i++) {
                register[chipId][0][i] = 0;
                register[chipId][1][i] = 0;
            }

            fadeout[chipId] = 0;
        }
    }

    private int getRhythmKeyON(int chipId) {
        int r = registerRhythm[chipId];
        registerRhythm[chipId] = 0;
        return r;
    }

    private int getFmKeyON(int chipId) {
        return registerFm[chipId];
    }

    public void write(int chipId, int port, int addr, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        register[chipId][port][addr] = data;

        if (addr >= 0x40 && addr <= 0x55) { // TL
            int ksl = (data & 0xc0);
            int tl = (data & 0x3f);
            int ch = addr - 0x40;
            int conSel = register[chipId][1][4] & 0x3f;
            boolean cr = false;

            int twoOpChannel = (ch / 8) * 3 + ((ch % 8) % 3);
            int fourOpChannel = twoOpChannel > 5 ? -1 : ((twoOpChannel % 3) + port * 3);
            boolean fourOpMode = fourOpChannel != -1 && ((conSel & (1 << fourOpChannel)) != 0);
            int slotNumber = ((ch % 8) / 3) + (twoOpChannel > 2 ? 2 : 0);
            twoOpChannel += port * 9;

            if (!fourOpMode) {
                // Career determination during 2op
                if (ch % 8 > 2)
                    cr = true;
                else {
                    int cnt = register[chipId][port][0xc0 + (ch / 8) * 3 + (ch % 8)] & 1;
                    if (cnt == 1)
                        cr = true;
                }
            } else {
                if (slotNumber == 3)
                    cr = true;
                else {
                    int cnt0 = register[chipId][port][0xc0 + (fourOpChannel % 3)] & 1;
                    int cnt1 = register[chipId][port][0xc3 + (fourOpChannel % 3)] & 1;
                    if (cnt0 == 0) {
                        if (cnt1 == 1 && slotNumber == 1)
                            cr = true;
                    } else {
                        if (cnt1 == 0) {
                            if (slotNumber == 0)
                                cr = true;
                        } else {
                            if (slotNumber != 1)
                                cr = true;
                        }
                    }
                }
            }

            if (ch >= 0x10 && port == 0 && (register[chipId][port][0xbd] & 0x20) != 0) {
                cr = true;
            }

            if (cr) {
                data = Math.min(tl + fadeout[chipId], 0x3f);
                data = ksl + (isMuted(chipId, twoOpChannel) ? 0x3f : data);
            }
        }

        if (addr >= 0xb0 && addr <= 0xb8) {
            int ch = addr - 0xb0 + port * 9;
            int k = (data >> 5) & 1;
            if (k == 0) {
                registerFm[chipId] &= ~(1 << ch);
            } else {
                registerFm[chipId] |= (1 << ch);
            }
            registerFm[chipId] &= 0x3ffff;
            if (isMuted(chipId, ch))
                data &= 0x1f;
        }

        if (addr == 0xbd && port == 0) {
            if ((registerRhythmB[chipId] & 0x10) == 0 && (data & 0x10) != 0)
                registerRhythm[chipId] |= 0x10;
            if ((registerRhythmB[chipId] & 0x08) == 0 && (data & 0x08) != 0)
                registerRhythm[chipId] |= 0x08;
            if ((registerRhythmB[chipId] & 0x04) == 0 && (data & 0x04) != 0)
                registerRhythm[chipId] |= 0x04;
            if ((registerRhythmB[chipId] & 0x02) == 0 && (data & 0x02) != 0)
                registerRhythm[chipId] |= 0x02;
            if ((registerRhythmB[chipId] & 0x01) == 0 && (data & 0x01) != 0)
                registerRhythm[chipId] |= 0x01;
            registerRhythmB[chipId] = data;

            if (isMuted(chipId, 18))
                data &= 0xef;
            if (isMuted(chipId, 19))
                data &= 0xf7;
            if (isMuted(chipId, 20))
                data &= 0xfb;
            if (isMuted(chipId, 21))
                data &= 0xfd;
            if (isMuted(chipId, 22))
                data &= 0xfe;
        }

        _write(chipId, port, addr, data, model);
    }

    private void _write(int chipId, int port, int addr, int data, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
            if (!chipTypes[chipId].getUseReal()[0]) {
                context.mds.write(inst(chipId), chipId, port, addr, data);
            }
        } else {
            if (realChips[chipId] != null) {
                realChips[chipId].setRegister(port * 0x100 + addr, data);
            }
        }
    }

    private void softReset(int chipId, EnmModel model) {
        // FM All Channel Key Off
        for (int i = 0; i < 9; i++) {
            _write(chipId, 0, 0xb0 + i, 0x00, model);
            _write(chipId, 1, 0xb0 + i, 0x00, model);
        }

        // FM TL=127
        for (int i = 0; i < 22; i++) {
            _write(chipId, 0, 0x40 + i, 0x3f, model);
            _write(chipId, 1, 0x40 + i, 0x3f, model);
        }

        // SL=15 RR=15
        for (int i = 0; i < 22; i++) {
            _write(chipId, 0, 0x80 + i, 0xff, model);
            _write(chipId, 1, 0x80 + i, 0xff, model);
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
            write(chipId, 0, 0x40 + c, register[chipId][0][0x40 + c], EnmModel.RealModel);
            write(chipId, 1, 0x40 + c, register[chipId][1][0x40 + c], EnmModel.RealModel);
        }
    }

    public void writeClock(int chipId, int clock, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
        } else {
            if (realChips != null && realChips[chipId] != null) {
                realChips[chipId].dClock = realChips[chipId].setMasterClock(clock);
            }
        }
    }

    @Override
    public Map<String, Object> getInfo(int chipId) {
        Instrument inst = context.mds.inst(inst(chipId));
        if (inst == null) return Collections.emptyMap();
        Map<String, Object> info = new HashMap<>();
        info.put("register", register[chipId]);
        info.put("rhythmKeyON", getRhythmKeyON(chipId));
        info.put("fmKeyON", getFmKeyON(chipId));
        info.putAll(inst.getView(chipId, "info") != null ? inst.getView(chipId, "info") : Collections.emptyMap()); // some variants not implemented
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
}
