/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import mdplayer.Common.EnmModel;
import mdplayer.RealChip.RSoundChip;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdsound.Instrument;
import mdsound.Instrument.AdpcmEnabledInstrument;
import mdsound.instrument.Ym2610Inst;
import mdsound.instrument.YmFmYm2610Inst;


/**
 * Ym2610Chip.
 * <p>
 * system property
 * <li>{@code mdplayer.variant.ym2610} ... active chip index</li>
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class Ym2610Chip extends BaseChip {

    private final Setting.ChipType2[] chipTypes = setting.getYM2610Type();

    private final RSoundChip[] realChips = {null, null};
    private final RSoundChip[] realChipsEA = {null, null};
    private final RSoundChip[] realChipsEB = {null, null};

    @Deprecated
    public final int[][][] register = {
            {null, null},
            {null, null}
    };

    @Deprecated
    public final int[][] keyOn = {null, null};

    @Deprecated
    public final int[][] volume = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    @Deprecated
    public final int[][] ch3SlotVolume = {new int[4], new int[4]};

    @Deprecated
    public final int[][][] rhythmVolume = {
            {new int[2], new int[2], new int[2], new int[2], new int[2], new int[2]},
            {new int[2], new int[2], new int[2], new int[2], new int[2], new int[2]}
    };

    @Deprecated
    public final int[][] adpcmVolume = {new int[2], new int[2]};

    @Deprecated
    public final int[] adpcmPan = {0, 0};

    /**
     * Which ADPCM-A channels are keyed, and how many times each has been struck.
     * <p>
     * The key register {@code 0x100} is one write for all six channels - a set of bits keys them
     * on, the same bits with {@code 0x80} keys them off - so the register file only ever holds the
     * last of those writes and says nothing about what is sounding now. A display polling it sees
     * a channel keyed on only if it happens to look between that write and the next; the state has
     * to be kept as the writes go past. The counter is for the same reason: two hits between two
     * polls are one register value, and a drum part is nothing but repeated hits.
     *
     * @see mdplayer.fmdsp.Ym2610Reader
     */
    public final boolean[][] adpcmAKeys = new boolean[2][6];

    public final int[][] adpcmAHits = new int[2][6];

    private final int[] nowFadeoutVol = {0, 0};

    @SuppressWarnings("unchecked")
    private Class<? extends AdpcmEnabledInstrument> _inst(int chipId) {
        return (Class<? extends AdpcmEnabledInstrument>) inst(chipId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Ym2610Inst.class, YmFmYm2610Inst.class};
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
                register[chipId][0][i] = 0; // -1;
                register[chipId][1][i] = 0; // -1;
            }
            register[chipId][0][0xb4] = 0xc0;
            register[chipId][0][0xb5] = 0xc0;
            register[chipId][0][0xb6] = 0xc0;
            register[chipId][1][0xb4] = 0xc0;
            register[chipId][1][0xb5] = 0xc0;
            register[chipId][1][0xb6] = 0xc0;
            keyOn[chipId] = new int[] {0, 0, 0, 0, 0, 0};
            Arrays.fill(adpcmAKeys[chipId], false);
            Arrays.fill(adpcmAHits[chipId], 0);

            nowFadeoutVol[chipId] = 0;
        }

        ym2610AdpcmA = new byte[][] {null, null};
        ym2610AdpcmB = new byte[][] {null, null};
    }

    @Override
    public void reset() {
        for (int chipId = 0; chipId < 2; chipId++) {
            for (int p = 0; p < 2; p++) {
                for (int c = 0; c < 3; c++) {
                    write(chipId, p, 0x40 + c, 127, EnmModel.RealModel);
                    write(chipId, p, 0x44 + c, 127, EnmModel.RealModel);
                    write(chipId, p, 0x48 + c, 127, EnmModel.RealModel);
                    write(chipId, p, 0x4c + c, 127, EnmModel.RealModel);
                }
            }

            // ssg
            write(chipId, 0, 0x08, 0, EnmModel.RealModel);
            write(chipId, 0, 0x09, 0, EnmModel.RealModel);
            write(chipId, 0, 0x0a, 0, EnmModel.RealModel);

            // rhythm
            write(chipId, 0, 0x11, 0, EnmModel.RealModel);

            // adpcm
            write(chipId, 1, 0x0b, 0, EnmModel.RealModel);
        }
    }

    @Override
    public void updateVol() {
        for (int chipId = 0; chipId < 2; chipId++) {
            for (int i = 0; i < 9; i++) {
                if (volume[chipId][i] > 0) {
                    volume[chipId][i] -= 50;
                    if (volume[chipId][i] < 0)
                        volume[chipId][i] = 0;
                }
            }
            for (int i = 0; i < 4; i++) {
                if (ch3SlotVolume[chipId][i] > 0) {
                    ch3SlotVolume[chipId][i] -= 50;
                    if (ch3SlotVolume[chipId][i] < 0)
                        ch3SlotVolume[chipId][i] = 0;
                }
            }
            for (int i = 0; i < 6; i++) {
                if (rhythmVolume[chipId][i][0] > 0) {
                    rhythmVolume[chipId][i][0] -= 50;
                    if (rhythmVolume[chipId][i][0] < 0)
                        rhythmVolume[chipId][i][0] = 0;
                }
                if (rhythmVolume[chipId][i][1] > 0) {
                    rhythmVolume[chipId][i][1] -= 50;
                    if (rhythmVolume[chipId][i][1] < 0)
                        rhythmVolume[chipId][i][1] = 0;
                }
            }

            if (adpcmVolume[chipId][0] > 0) {
                adpcmVolume[chipId][0] -= 50;
                if (adpcmVolume[chipId][0] < 0)
                    adpcmVolume[chipId][0] = 0;
            }
            if (adpcmVolume[chipId][1] > 0) {
                adpcmVolume[chipId][1] -= 50;
                if (adpcmVolume[chipId][1] < 0)
                    adpcmVolume[chipId][1] = 0;
            }
        }
    }

    public void write(int chipId, int port, int addr, int data, EnmModel model) {
        if (addr < 0 || data < 0) return;

        fireEventHappened("led.on", chipId);

        if ((model == EnmModel.VirtualModel && (chipTypes[chipId] == null || !chipTypes[chipId].getUseReal()[0]))
                || (model == EnmModel.RealModel && (realChips != null && realChips[chipId] != null))
        ) {
            if (port == 0 && (addr == 0x2d || addr == 0x2e || addr == 0x2f)) {
                register[chipId][0][0x2d] = addr - 0x2d;
            } else {
                register[chipId][port][addr] = data;
            }
        }

//logger.log(Level.TRACE, "OPNB p:%02X a:%02X D:%02X".formatted(port, addr, data));

        if ((model == EnmModel.RealModel && chipTypes[chipId].getUseReal()[0]) || (model == EnmModel.VirtualModel && !chipTypes[chipId].getUseReal()[0])) {
            //register[port][addr] = data;
            if (port == 0 && addr == 0x28) {
                int ch = (data & 0x3) + ((data & 0x4) > 0 ? 3 : 0);
                if (ch >= 0 && ch < 6 /* && (data & 0xf0) > 0 */) {
                    if (ch != 2 || (register[chipId][0][0x27] & 0xc0) != 0x40) {
                        if ((data & 0xf0) != 0) {
                            keyOn[chipId][ch] = (data & 0xf0) | 1;
                            volume[chipId][ch] = 256 * 6;
                        } else {
                            keyOn[chipId][ch] &= 0xfe;
                        }
                    } else {
                        keyOn[chipId][2] = data & 0xf0;
                        if ((data & 0x10) > 0) ch3SlotVolume[chipId][0] = 256 * 6;
                        if ((data & 0x20) > 0) ch3SlotVolume[chipId][1] = 256 * 6;
                        if ((data & 0x40) > 0) ch3SlotVolume[chipId][2] = 256 * 6;
                        if ((data & 0x80) > 0) ch3SlotVolume[chipId][3] = 256 * 6;
                    }
                }
            }

            // ADPCM B KEYON
            if (port == 0 && addr == 0x10) {
                if ((data & 0x80) != 0) {
                    int p = (register[chipId][0][0x11] & 0xc0) >> 6;
                    p = p == 0 ? 3 : p;
                    if (adpcmPan[chipId] != p)
                        adpcmPan[chipId] = p;

                    adpcmVolume[chipId][0] = ((adpcmPan[chipId] & 0x02) != 0 ? 1 : 0);
                    adpcmVolume[chipId][1] = ((adpcmPan[chipId] & 0x01) != 0 ? 1 : 0);
                } else {
                    adpcmVolume[chipId][0] = 0;
                    adpcmVolume[chipId][1] = 0;
                }
            }

            // ADPCM a KEYON
            if (port == 1 && addr == 0x00) {
                for (int i = 0; i < adpcmAKeys[chipId].length; i++) {
                    if ((data & (1 << i)) == 0) continue;
                    if ((data & 0x80) != 0) {
                        adpcmAKeys[chipId][i] = false; // the dump bit is this section's key off
                    } else {
                        adpcmAKeys[chipId][i] = true;
                        adpcmAHits[chipId][i]++;
                    }
                }
                if ((data & 0x80) == 0) {
                    int tl = register[chipId][1][0x01] & 0x3f;
                    for (int i = 0; i < 6; i++) {
                        if ((data & (0x1 << i)) != 0) {
                            //int il = register[chipId][1][0x08 + i] & 0x1f;
                            int pan = ((register[chipId][1][0x08 + i] & 0xc0) >> 6) * (((data & 0x80) == 0) ? 1 : 0);
                            //rhythmVolume[chipId][i][0] = (int)(256 * 6 * ((tl * il) >> 4) / 127.0) * ((pan & 2) > 0 ? 1 : 0);
                            //rhythmVolume[chipId][i][1] = (int)(256 * 6 * ((tl * il) >> 4) / 127.0) * ((pan & 1) > 0 ? 1 : 0);
                            rhythmVolume[chipId][i][0] = ((pan & 2) > 0 ? 1 : 0);
                            rhythmVolume[chipId][i][1] = ((pan & 1) > 0 ? 1 : 0);
                        } else {
                            rhythmVolume[chipId][i][0] = 0;
                            rhythmVolume[chipId][i][1] = 0;
                        }
                    }
                }
            }
        }

        if ((addr & 0xf0) == 0x40) { // TL
            int ch = (addr & 0x3);
            int slot = (addr & 0xc) >> 2;
            int al = register[chipId][port][0xb0 + ch] & 0x07; // AL
            data &= 0x7f;

            if (ch != 3) {
                if ((algM[al] & (1 << slot)) > 0) {
                    data = Math.min(data + nowFadeoutVol[chipId], 127);
                    data = isMuted(chipId, port * 3 + ch) ? 127 : data;
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
                        write(chipId, port, 0x40 + ch + tslot,
                                register[chipId][port][0x40 + ch + tslot], model);
                    }
                }
            }
        }

        // ssg mixer
        if (port == 0 && addr == 0x07) {
            int maskData = 0;
            if (isMuted(chipId, 6)) maskData |= 0x9 << 0;
            if (isMuted(chipId, 7)) maskData |= 0x9 << 1;
            if (isMuted(chipId, 8)) maskData |= 0x9 << 2;
            data |= maskData;
        }

        // ssg level
        if (port == 0 && (addr == 0x08 || addr == 0x09 || addr == 0x0a)) {
            int d = nowFadeoutVol[chipId] >> 3;
            data = Math.max(data - d, 0);
            data = isMuted(chipId, addr - 0x08 + 6) ? 0 : data;
        }

        // rhythm level
        if (port == 1 && addr == 0x01) {
            int d = nowFadeoutVol[chipId] >> 1;
            data = Math.max(data - d, 0);
            //data = isMuted(chipId, 12) ? 0 : data;
        }

        // Rhythm
        if (port == 1 && addr == 0x00) {
            if (isMuted(chipId, 12)) {
                data = 0xbf;
            }
        }

        // adpcm level
        if (port == 0 && addr == 0x1b) {
            int d = nowFadeoutVol[chipId] * 2;
            data = Math.max(data - d, 0);
            data = isMuted(chipId, 13) ? 0 : data;
        }

        // adpcm start
        if (port == 0 && addr == 0x10) {
            if ((data & 0x80) != 0 && isMuted(chipId, 13)) {
                data &= 0x7f;
            }
        }

        if (model == EnmModel.VirtualModel) {
            if ((chipTypes[chipId].getUseReal().length > 0 && !chipTypes[chipId].getUseReal()[0])
                    && (chipTypes[chipId].getUseReal().length < 2 || (chipTypes[chipId].getUseReal().length > 1 && !chipTypes[chipId].getUseReal()[1]))
            ) {
                context.mds.write(_inst(chipId), chipId, port, addr, data);
            }
        } else {
            if (realChips[chipId] != null) realChips[chipId].setRegister(port * 0x100 + addr, data);
            if (realChipsEA[chipId] != null) {
                int dReg = (port << 8) | addr;
                boolean bSend = true;
                // Mask the register and send
                if (dReg >= 0x100 && dReg <= 0x12d) {
                    // ADPCM-a
                    bSend = false;
                } else if (dReg >= 0x010 && dReg <= 0x01c) {
                    // ADPCM-B
                    bSend = false;
                }
                if (bSend) {
                    realChipsEA[chipId].setRegister((port << 8) | addr, data);
                }
            }
            if (realChipsEB[chipId] != null) {
                realChipsEB[chipId].setRegister((port << 8) | addr | 0x10000, data);
            }
        }
    }

    public void writeAdpcmA(int chipId, byte[] adpcmA, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
            context.mds.inst(_inst(chipId)).writeAdpcmA(chipId, adpcmA);
        } else {
            if (realChips[chipId] != null) {
                int dPort = 2;
                int startAddr = 0;
                realChips[chipId].setRegister((dPort << 8) | 0x00, 0x00);
                realChips[chipId].setRegister((dPort << 8) | 0x01, (startAddr >> 8) & 0xff);
                realChips[chipId].setRegister((dPort << 8) | 0x02, (startAddr >> 16) & 0xff);

                //pushReg(CMD_YM2610|0x02,0x03,0x01);
                realChips[chipId].setRegister((dPort << 8) | 0x03, 0x01);
                // Data Transfer
                for (byte b : adpcmA) {
                    //pushReg(CMD_YM2610|0x02,0x04,*m_pDump);
                    realChips[chipId].setRegister((dPort << 8) | 0x04, b & 0xff);
                }

                context.chipRegister.plugin(RealChipPlugin.class).realChip.sendData();
            }
            if (realChipsEB[chipId] != null) {
                int dPort = 2;
                int startAddr = 0;
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10000, 0x00);
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10001, (startAddr >> 8) & 0xff);
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10002, (startAddr >> 16) & 0xff);

                //pushReg(CMD_YM2610|0x02,0x03,0x01);
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10003, 0x01);
                // Data Transfer
                for (byte b : adpcmA) {
                    //pushReg(CMD_YM2610|0x02,0x04,*m_pDump);
                    realChipsEB[chipId].setRegister((dPort << 8) | 0x10004, b & 0xff);
                }

                context.chipRegister.plugin(RealChipPlugin.class).realChip.sendData();
            }
        }
    }

    private void writeAdpcmA(int chipId, EnmModel model, int startAddr, int length, byte[] buf, int srcStartAddr) {
        if (model == EnmModel.VirtualModel) {
        } else {
            if (realChips[chipId] != null) {
                int dPort = 2;
                realChips[chipId].setRegister((dPort << 8) | 0x00, 0x00);
                realChips[chipId].setRegister((dPort << 8) | 0x01, (startAddr >> 8) & 0xff);
                realChips[chipId].setRegister((dPort << 8) | 0x02, (startAddr >> 16) & 0xff);

                //pushReg(CMD_YM2610|0x02,0x03,0x01);
                realChips[chipId].setRegister((dPort << 8) | 0x03, 0x01);
                // Data Transfer
                for (int cnt = 0; cnt < length; cnt++) {
                    //pushReg(CMD_YM2610|0x02,0x04,*m_pDump);
                    realChips[chipId].setRegister((dPort << 8) | 0x04, buf[srcStartAddr + cnt] & 0xff);
                }

                context.chipRegister.plugin(RealChipPlugin.class).realChip.sendData();
            }
            if (realChipsEB[chipId] != null) {
                int dPort = 2;
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10000, 0x00);
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10001, (startAddr >> 8) & 0xff);
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10002, (startAddr >> 16) & 0xff);

                //pushReg(CMD_YM2610|0x02,0x03,0x01);
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10003, 0x01);
                // Data Transfer
                for (int cnt = 0; cnt < length; cnt++) {
                    //pushReg(CMD_YM2610|0x02,0x04,*m_pDump);
                    realChipsEB[chipId].setRegister((dPort << 8) | 0x10004, buf[srcStartAddr + cnt] & 0xff);
                }

                context.chipRegister.plugin(RealChipPlugin.class).realChip.sendData();
            }
        }
    }

    public void writeAdpcmB(int chipId, byte[] adpcmB, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
            context.mds.inst(_inst(chipId)).writeAdpcmB(chipId, adpcmB);
        } else {
            if (realChips[chipId] != null) {
                int dPort = 2;
                int startAddr = 0;
                realChips[chipId].setRegister((dPort << 8) | 0x00, 0x00);
                realChips[chipId].setRegister((dPort << 8) | 0x01, (startAddr >> 8) & 0xff);
                realChips[chipId].setRegister((dPort << 8) | 0x02, (startAddr >> 16) & 0xff);

                //pushReg(CMD_YM2610|0x02,0x03,0x01);
                realChips[chipId].setRegister((dPort << 8) | 0x03, 0x00);
                // Data Transfer
                for (byte b : adpcmB) {
                    //pushReg(CMD_YM2610|0x02,0x04,*m_pDump);
                    realChips[chipId].setRegister((dPort << 8) | 0x04, b & 0xff);
                }

                context.chipRegister.plugin(RealChipPlugin.class).realChip.sendData();
            }
            if (realChipsEB[chipId] != null) {
                int dPort = 2;
                int startAddr = 0;
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10000, 0x00);
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10001, (startAddr >> 8) & 0xff);
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10002, (startAddr >> 16) & 0xff);

                //pushReg(CMD_YM2610|0x02,0x03,0x01);
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10003, 0x00);
                // Data Transfer
                for (byte b : adpcmB) {
                    //pushReg(CMD_YM2610|0x02,0x04,*m_pDump);
                    realChipsEB[chipId].setRegister((dPort << 8) | 0x10004, b & 0xff);
                }

                context.chipRegister.plugin(RealChipPlugin.class).realChip.sendData();
            }
        }
    }

    private void writeAdpcmB(int chipId, EnmModel model, int startAddr, int length, byte[] buf, int srcStartAddr) {
        if (model == EnmModel.VirtualModel) {
        } else {
            if (realChips[chipId] != null) {
                int dPort = 2;
                realChips[chipId].setRegister((dPort << 8) | 0x00, 0x00);
                realChips[chipId].setRegister((dPort << 8) | 0x01, (startAddr >> 8) & 0xff);
                realChips[chipId].setRegister((dPort << 8) | 0x02, (startAddr >> 16) & 0xff);

                //pushReg(CMD_YM2610|0x02,0x03,0x01);
                realChips[chipId].setRegister((dPort << 8) | 0x03, 0x00);
                // Data Transfer
                for (int cnt = 0; cnt < length; cnt++) {
                    //pushReg(CMD_YM2610|0x02,0x04,*m_pDump);
                    realChips[chipId].setRegister((dPort << 8) | 0x04, buf[srcStartAddr + cnt] & 0xff);
                }

                context.chipRegister.plugin(RealChipPlugin.class).realChip.sendData();
            }
            if (realChipsEB[chipId] != null) {
                int dPort = 2;
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10000, 0x00);
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10001, (startAddr >> 8) & 0xff);
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10002, (startAddr >> 16) & 0xff);

                //pushReg(CMD_YM2610|0x02,0x03,0x01);
                realChipsEB[chipId].setRegister((dPort << 8) | 0x10003, 0x00);
                // Data Transfer
                for (int cnt = 0; cnt < length; cnt++) {
                    //pushReg(CMD_YM2610|0x02,0x04,*m_pDump);
                    realChipsEB[chipId].setRegister((dPort << 8) | 0x10004, buf[srcStartAddr + cnt] & 0xff);
                }

                context.chipRegister.plugin(RealChipPlugin.class).realChip.sendData();
            }
        }
    }

    /** FM ch3 and its extended slots mask as one */
    @Override
    protected long linked(int ch) {
        return ch == 2 || (ch >= 9 && ch < 12) ? 0b1110_0000_0100 : super.linked(ch);
    }

    /** the levels are filtered on their way in, see {@link #write}; this re-sends what the chip holds */
    @Override
    protected void applyMute(int chipId, int ch) {
        if (ch < 0) return;

        int c = (ch < 3) ? ch : (ch - 3);
        int p = (ch < 3) ? 0 : 1;

        if (ch < 6) {
            write(chipId, p, 0x40 + c, register[chipId][p][0x40 + c], EnmModel.VirtualModel);
            write(chipId, p, 0x44 + c, register[chipId][p][0x44 + c], EnmModel.VirtualModel);
            write(chipId, p, 0x48 + c, register[chipId][p][0x48 + c], EnmModel.VirtualModel);
            write(chipId, p, 0x4c + c, register[chipId][p][0x4c + c], EnmModel.VirtualModel);

            write(chipId, p, 0x40 + c, register[chipId][p][0x40 + c], EnmModel.RealModel);
            write(chipId, p, 0x44 + c, register[chipId][p][0x44 + c], EnmModel.RealModel);
            write(chipId, p, 0x48 + c, register[chipId][p][0x48 + c], EnmModel.RealModel);
            write(chipId, p, 0x4c + c, register[chipId][p][0x4c + c], EnmModel.RealModel);
        } else if (ch < 9) {
            write(chipId,
                    0,
                    0x08 + ch - 6,
                    register[chipId][0][0x08 + ch - 6],
                    EnmModel.VirtualModel);
            write(chipId, 0, 0x08 + ch - 6, register[chipId][0][0x08 + ch - 6], EnmModel.RealModel);
        } else if (ch < 12) {
            write(chipId, 0, 0x40 + 2, register[chipId][0][0x40 + 2], EnmModel.VirtualModel);
            write(chipId, 0, 0x44 + 2, register[chipId][0][0x44 + 2], EnmModel.VirtualModel);
            write(chipId, 0, 0x48 + 2, register[chipId][0][0x48 + 2], EnmModel.VirtualModel);
            write(chipId, 0, 0x4c + 2, register[chipId][0][0x4c + 2], EnmModel.VirtualModel);

            write(chipId, 0, 0x40 + 2, register[chipId][0][0x40 + 2], EnmModel.RealModel);
            write(chipId, 0, 0x44 + 2, register[chipId][0][0x44 + 2], EnmModel.RealModel);
            write(chipId, 0, 0x48 + 2, register[chipId][0][0x48 + 2], EnmModel.RealModel);
            write(chipId, 0, 0x4c + 2, register[chipId][0][0x4c + 2], EnmModel.RealModel);
        } else if (ch == 12) {
            if (this.isMuted(chipId, 12)) {
                write(chipId, 1, 0x00, 1, EnmModel.VirtualModel);
                write(chipId, 1, 0x00, 1, EnmModel.RealModel);
            }
        }
    }

    @Override
    public void setFadeout(int chipId, int v) {
        nowFadeoutVol[chipId] = v;
        for (int p = 0; p < 2; p++) {
            for (int c = 0; c < 3; c++) {
                write(chipId, p, 0x40 + c, register[chipId][p][0x40 + c], EnmModel.RealModel);
                write(chipId, p, 0x44 + c, register[chipId][p][0x44 + c], EnmModel.RealModel);
                write(chipId, p, 0x48 + c, register[chipId][p][0x48 + c], EnmModel.RealModel);
                write(chipId, p, 0x4c + c, register[chipId][p][0x4c + c], EnmModel.RealModel);
            }
        }

        // ssg
        write(chipId, 0, 0x08, register[chipId][0][0x08], EnmModel.RealModel);
        write(chipId, 0, 0x09, register[chipId][0][0x09], EnmModel.RealModel);
        write(chipId, 0, 0x0a, register[chipId][0][0x0a], EnmModel.RealModel);

        // rhythm
        write(chipId, 0, 0x11, register[chipId][0][0x11], EnmModel.RealModel);

        // adpcm
        write(chipId, 1, 0x0b, register[chipId][1][0x0b], EnmModel.RealModel);
    }

    @Override
    public Map<String, Object> getInfo(int chipId) {
        Instrument inst = context.mds.inst(inst(chipId));
        if (inst == null) return Collections.emptyMap();
        Map<String, Object> info = new HashMap<>();
        info.put("volume", volume[chipId]);
        info.put("rhythmVolume", rhythmVolume[chipId]);
        info.put("adpcmVolume", adpcmVolume[chipId]);
        info.put("ch3SlotVolume", /* ctYM2612.UseScci ? */ ch3SlotVolume[chipId] /* : context.mds.inst(_inst(chipId)).readFMCh3SlotVolume(); */);
        info.put("register", register[chipId]);
        info.put("keyOn", keyOn[chipId]);
        info.putAll(inst.getView(chipId, "info") != null ? inst.getView(chipId, "info") : Collections.emptyMap()); // some variants not implemented
        return info;
    }

    // TODO copied data
    private byte[][] ym2610AdpcmA = new byte[][] {null, null};
    // TODO copied data
    private byte[][] ym2610AdpcmB = new byte[][] {null, null};

    public void writeAdpcmA(int chipId, byte[] vgmBuf, int vgmAdr, int bLen, int startAddress, int romSize, EnmModel model) {
        if (ym2610AdpcmA[chipId] == null || ym2610AdpcmA[chipId].length != romSize)
            ym2610AdpcmA[chipId] = new byte[romSize];
        if (ym2610AdpcmA[chipId].length > 0) {
            for (int cnt = 0; cnt < bLen - 8; cnt++) {
                ym2610AdpcmA[chipId][startAddress + cnt] = vgmBuf[vgmAdr + 15 + cnt];
            }
            if (model == EnmModel.VirtualModel)
                writeAdpcmA(chipId, ym2610AdpcmA[chipId], model);
            else
                writeAdpcmA(chipId, model, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
        }

        dumpData(model, "ADPCMA", vgmAdr + 15, vgmBuf, bLen - 8);
    }

    public void writeAdpcmB(int chipId, byte[] vgmBuf, int vgmAdr, int bLen, int startAddress, int romSize, EnmModel model) {
        if (ym2610AdpcmB[chipId] == null || ym2610AdpcmB[chipId].length != romSize)
            ym2610AdpcmB[chipId] = new byte[romSize];
        if (ym2610AdpcmB[chipId].length > 0) {
            for (int cnt = 0; cnt < bLen - 8; cnt++) {
                ym2610AdpcmB[chipId][startAddress + cnt] = vgmBuf[vgmAdr + 15 + cnt];
            }
            if (model == EnmModel.VirtualModel)
                writeAdpcmB(chipId, ym2610AdpcmB[chipId], model);
            else
                writeAdpcmB(chipId, model, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
        }

        dumpData(model, "ADPCMB", vgmAdr + 15, vgmBuf, bLen - 8);
    }
}
