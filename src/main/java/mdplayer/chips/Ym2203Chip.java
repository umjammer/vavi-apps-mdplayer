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
import mdplayer.RealChip.RC86ctlSoundChip;
import mdplayer.RealChip.RSoundChip;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdsound.Instrument;
import mdsound.instrument.Ym2203Inst;
import mdsound.instrument.YmFmYm2203Inst;


/**
 * Ym2203Chip.
 * <p>
 * system property
 * <li>{@code mdplayer.variant.ym2203} ... active chip index</li>
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class Ym2203Chip extends BaseChip {

    private final Setting.ChipType2[] chipTypes = setting.getYM2203Type();

    private final RSoundChip[] realChips = {null, null};

    @Deprecated
    public final int[][] fmRegister = {null, null};
    @Deprecated
    public final int[][] fmKeyOn = {null, null};
    @Deprecated
    public final int[][] fmCh3SlotVolume = {new int[4], new int[4]};
    @Deprecated
    private final int[] nowFadeoutVol = {0, 0};
    @Deprecated
    public final int[][] fmVolume = {new int[9], new int[9]};

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Ym2203Inst.class, YmFmYm2203Inst.class};
    }

    @Override
    public int activeIndex(int chipId) {
        return chipTypes[chipId].getEnabledId();
    }

    @Override
    public void init(BasePlugin<? extends BaseDriver> context) {
        super.init(context);

        for (int chipId = 0; chipId < 2; chipId++) {
            fmRegister[chipId] = new int[0x100];
            for (int i = 0; i < 0x100; i++) {
                fmRegister[chipId][i] = 0; // -1;
            }
            fmKeyOn[chipId] = new int[] {0, 0, 0, 0, 0, 0};

            nowFadeoutVol[chipId] = 0;
        }
    }

    @Override
    public void updateVol() {
        for (int chipId = 0; chipId < 2; chipId++) {

            for (int i = 0; i < 6; i++) {
                if (fmVolume[chipId][i] > 0) {
                    fmVolume[chipId][i] -= 50;
                    if (fmVolume[chipId][i] < 0)
                        fmVolume[chipId][i] = 0;
                }
            }
            for (int i = 0; i < 4; i++) {
                if (fmCh3SlotVolume[chipId][i] > 0) {
                    fmCh3SlotVolume[chipId][i] -= 50;
                    if (fmCh3SlotVolume[chipId][i] < 0)
                        fmCh3SlotVolume[chipId][i] = 0;
                }
            }
        }
    }

    public void write(int chipId, int addr, int data, EnmModel model) {
        if (addr < 0 || data < 0)
            return;

        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
            if (addr != 0x2d && addr != 0x2e && addr != 0x2f) {
                fmRegister[chipId][addr] = data;
            } else {
                fmRegister[chipId][0x2d] = addr - 0x2d;
            }
        }

        if ((model == EnmModel.RealModel && chipTypes[chipId].getUseReal()[0]) ||
                (model == EnmModel.VirtualModel && !chipTypes[chipId].getUseReal()[0])) {
            if (addr == 0x28) {
                int ch = data & 0x3;
                if (ch >= 0 && ch < 3) {
                    if (ch != 2 || (fmRegister[chipId][0x27] & 0xc0) != 0x40) {
                        if ((data & 0xf0) != 0) {
                            fmKeyOn[chipId][ch] = (data & 0xf0) | 1;
                            fmVolume[chipId][ch] = 256 * 6;
                        } else {
                            fmKeyOn[chipId][ch] &= 0xfe;
                        }
                    } else {
                        fmKeyOn[chipId][2] = (data & 0xf0);
                        if ((data & 0x10) > 0)
                            fmCh3SlotVolume[chipId][0] = 256 * 6;
                        if ((data & 0x20) > 0)
                            fmCh3SlotVolume[chipId][1] = 256 * 6;
                        if ((data & 0x40) > 0)
                            fmCh3SlotVolume[chipId][2] = 256 * 6;
                        if ((data & 0x80) > 0)
                            fmCh3SlotVolume[chipId][3] = 256 * 6;
                    }
                }
            }
        }

        if ((addr & 0xf0) == 0x40) { // TL
            int ch = (addr & 0x3);
            int slot = (addr & 0xc) >> 2;
            int al = fmRegister[chipId][0xb0 + ch] & 0x7;
            data &= 0x7f;

            if (ch != 3) {
                if ((algM[al] & (1 << slot)) != 0) {
                    data = Math.min(data + nowFadeoutVol[chipId], 127);
                    data = isMuted(chipId, ch) ? 127 : data;
                }
            }
        }

        if ((addr & 0xf0) == 0xb0) { // AL
            int ch = (addr & 0x3);
            int al = data & 0x07; // AL

            if (ch != 3 && isMuted(chipId, ch)) {
                for (int slot = 0; slot < 4; slot++) {
                    if ((algM[al] & (1 << slot)) != 0) {
                        int tslot = (slot == 1 ? 2 : (slot == 2 ? 1 : slot)) * 4;
                        write(chipId, 0x40 + ch + tslot, fmRegister[chipId][0x40 + ch + tslot], model);
                    }
                }
            }
        }

        // ssg mixer
        if (addr == 0x07) {
            int maskData = 0;
            if (isMuted(chipId, 3))
                maskData |= 0x9 << 0;
            if (isMuted(chipId, 4))
                maskData |= 0x9 << 1;
            if (isMuted(chipId, 5))
                maskData |= 0x9 << 2;
            data |= maskData;
        }

        // ssg level
        if ((addr == 0x08 || addr == 0x09 || addr == 0x0a)) {
            int d = nowFadeoutVol[chipId] >> 3;
            data = Math.max(data - d, 0);
            data = isMuted(chipId, addr - 0x08 + 3) ? 0 : data;
        }

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

    private void write(int chipId, int port, int addr, int data, EnmModel model) {
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
        // FM all channel key off
        write(chipId, 0, 0x28, 0x00, model);
        write(chipId, 0, 0x28, 0x01, model);
        write(chipId, 0, 0x28, 0x02, model);

        // FM TL=127
        for (int i = 0x40; i < 0x4F + 1; i++) {
            write(chipId, 0, i, 0x7f, model);
        }
        // FM ML/DT
        for (int i = 0x30; i < 0x3F + 1; i++) {
            write(chipId, 0, i, 0x0, model);
        }
        // FM AR,DR,SR,KS,AMON
        for (int i = 0x50; i < 0x7F + 1; i++) {
            write(chipId, 0, i, 0x0, model);
        }
        // FM SL,RR
        for (int i = 0x80; i < 0x8F + 1; i++) {
            write(chipId, 0, i, 0xff, model);
        }
        // FM F-Num, FB/CONNECT
        for (int i = 0x90; i < 0xBF + 1; i++) {
            write(chipId, 0, i, 0x0, model);
        }
        // FM PAN/AMS/PMS
        for (int i = 0xB4; i < 0xB6 + 1; i++) {
            write(chipId, 0, i, 0xc0, model);
        }
        write(chipId, 0, 0x22, 0x00, model); // HW LFO
        write(chipId, 0, 0x24, 0x00, model); // Timer-a(1)
        write(chipId, 0, 0x25, 0x00, model); // Timer-a(2)
        write(chipId, 0, 0x26, 0x00, model); // Timer-B
        write(chipId, 0, 0x27, 0x30, model); // Timer Control

        // SSG Pitch(2byte*3ch)
        for (int i = 0x00; i < 0x05 + 1; i++) {
            write(chipId, 0, i, 0x00, model);
        }
        write(chipId, 0, 0x06, 0x00, model); // SSG Noise Frequency
        write(chipId, 0, 0x07, 0x38, model); // SSG Mixer
        // SSG volume(3ch)
        for (int i = 0x08; i < 0x0A + 1; i++) {
            write(chipId, 0, i, 0x00, model);
        }
        // SSG Envelope
        for (int i = 0x0B; i < 0x0D + 1; i++) {
            write(chipId, 0, i, 0x00, model);
        }
    }

    /** FM ch3 and its extended slots mask as one */
    @Override
    protected long linked(int ch) {
        return ch == 2 || (ch >= 6 && ch < 9) ? 0b1_1100_0100 : super.linked(ch);
    }

    /** the levels are filtered on their way in, see {@link #write}; this re-sends what the chip holds */
    @Override
    protected void applyMute(int chipId, int ch) {
        if (ch < 0) return;
        // a stopped song is left as it is when it is un-muted
        if (!isMuted(chipId, ch) && context.stopped) return;

        int c = ch >= 6 ? 2 : ch; // an extended slot is ch3's
        if (c < 3) {
            write(chipId, 0x40 + c, fmRegister[chipId][0x40 + c], EnmModel.VirtualModel);
            write(chipId, 0x44 + c, fmRegister[chipId][0x44 + c], EnmModel.VirtualModel);
            write(chipId, 0x48 + c, fmRegister[chipId][0x48 + c], EnmModel.VirtualModel);
            write(chipId, 0x4c + c, fmRegister[chipId][0x4c + c], EnmModel.VirtualModel);

            write(chipId, 0x40 + c, fmRegister[chipId][0x40 + c], EnmModel.RealModel);
            write(chipId, 0x44 + c, fmRegister[chipId][0x44 + c], EnmModel.RealModel);
            write(chipId, 0x48 + c, fmRegister[chipId][0x48 + c], EnmModel.RealModel);
            write(chipId, 0x4c + c, fmRegister[chipId][0x4c + c], EnmModel.RealModel);
        } else if (c < 6) {
            write(chipId, 0x08 + c - 3, fmRegister[chipId][0x08 + c - 3], EnmModel.VirtualModel);
            write(chipId, 0x08 + c - 3, fmRegister[chipId][0x08 + c - 3], EnmModel.RealModel);
        }
    }

    private static final int[] algVolTbl = {
            8, 8, 8, 8, 0xa, 0xe, 0xe, 0xf
    };

    @Override
    public void setFadeout(int chipId, int v) {
        nowFadeoutVol[chipId] = v;
        for (int c = 0; c < 3; c++) {
            int alg = fmRegister[chipId][0xb0 + c] & 0x7;
            if ((algVolTbl[alg] & 1) != 0)
                write(chipId, 0x40 + c, fmRegister[chipId][0x40 + c], EnmModel.RealModel);
            if ((algVolTbl[alg] & 4) != 0)
                write(chipId, 0x44 + c, fmRegister[chipId][0x44 + c], EnmModel.RealModel);
            if ((algVolTbl[alg] & 2) != 0)
                write(chipId, 0x48 + c, fmRegister[chipId][0x48 + c], EnmModel.RealModel);
            if ((algVolTbl[alg] & 8) != 0)
                write(chipId, 0x4c + c, fmRegister[chipId][0x4c + c], EnmModel.RealModel);
        }
    }

    public void writeClock(int chipId, int clock, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
        } else {
            if (realChips != null && realChips[chipId] != null) {
                if (realChips[chipId] instanceof RC86ctlSoundChip) {
//                    Nc86ctl.ChipType ct = ((RC86ctlSoundChip) realChips[chipId]).ChipType;
//                    // If OPNA/OPN3L is selected, double the frequency
//                    if (ct == Nc86ctl.ChipType.CHIP_OPN3L || ct == Nc86ctl.ChipType.CHIP_OPNA) {
//                        clock *= 2;
//                    }
                }
                realChips[chipId].dClock = realChips[chipId].setMasterClock(clock);
            }
        }
    }

    public void setSsgVolume(int chipId, int vol, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
        } else {
            if (realChips != null && realChips[chipId] != null) {
                realChips[chipId].setSSGVolume((byte) vol);
            }
        }
    }

    /**
     * What the panels have always read, and beside it the channel state as the chip has it. The
     * fmgen core keeps no register file - it decodes into operators - so the visualizer reads the
     * channel view while the register dump still gets the shadow.
     */
    @Override
    public Map<String, Object> getInfo(int chipId) {
        Instrument inst = context.mds.inst(inst(chipId));
        if (inst == null) return Collections.emptyMap();
        Map<String, Object> info = new HashMap<>();
        info.put("volume", fmVolume[chipId]);
        info.put("ch3SlotVolume", /* ctYM2612.UseScci ? */ fmCh3SlotVolume[chipId] /*, context.mds.inst(inst[chipId]).readFMCh3SlotVolume(); */);
        info.put("register", fmRegister[chipId]);
        info.put("keyOn", fmKeyOn[chipId]);
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
