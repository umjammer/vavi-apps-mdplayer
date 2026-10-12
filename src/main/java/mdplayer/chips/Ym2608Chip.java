/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import mdplayer.Common;
import mdplayer.Common.EnmModel;
import mdplayer.RealChip.RSoundChip;
import mdplayer.Setting;
import mdplayer.Tables;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdsound.Instrument;
import mdsound.instrument.Ym2608Inst;
import mdsound.instrument.YmFmYm2608Inst;
import vavi.util.ByteUtil;


/**
 * Ym2608Chip.
 * <p>
 * system property
 * <li>{@code mdplayer.variant.ym2608} ... active chip index</li>
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class Ym2608Chip extends BaseChip {

    private static final Logger logger = System.getLogger(Ym2608Chip.class.getName());

    private final Setting.ChipType2[] chipTypes = setting.getYM2608Type();

    private final RSoundChip[] realChips = {null, null};

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
    public final int[][] ch3SlotVolume = {
            new int[4], new int[4]
    };

    @Deprecated
    public final int[][][] rhythmVolume = {
            {new int[2], new int[2], new int[2], new int[2], new int[2], new int[2]},
            {new int[2], new int[2], new int[2], new int[2], new int[2], new int[2]}
    };

    @Deprecated
    public final int[][] adpcmVolume = {new int[2], new int[2]};

    @Deprecated
    public final int[] adpcmPan = {0, 0};

    private final int[] fadeout = {0, 0};

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Ym2608Inst.class, YmFmYm2608Inst.class};
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

            fadeout[chipId] = 0;
        }

        this.opnaRamType = 0;
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
//if (chipId == 0 && port == 1 && addr == 0x01) {
// logger.log(Level.TRACE, "FM P1 Out:Adr[%02x] val[%02x]".formatted((int) addr, (int) data));
//}
        if (addr < 0 || data < 0)
            return;

        fireEventHappened("led.on", chipId);

        if ((model == EnmModel.VirtualModel && (chipTypes[chipId] == null || !chipTypes[chipId].getUseReal()[0])) ||
                (model == EnmModel.RealModel && (realChips != null && realChips[chipId] != null))) {
            if (port == 0 && (addr == 0x2d || addr == 0x2e || addr == 0x2f)) {
                register[chipId][0][0x2d] = addr - 0x2d;
            } else {
                register[chipId][port][addr] = data;
            }
        }

        if ((model == EnmModel.RealModel && chipTypes[chipId].getUseReal()[0]) ||
                (model == EnmModel.VirtualModel && !chipTypes[chipId].getUseReal()[0])) {
            if (port == 0 && addr == 0x28) {
                int ch = (data & 0x3) + ((data & 0x4) > 0 ? 3 : 0);
                if (ch >= 0 && ch < 6) /* && (data & 0xf0) > 0) */ {
                    if (ch != 2 || (register[chipId][0][0x27] & 0xc0) != 0x40) {
                        if ((data & 0xf0) != 0) {
                            keyOn[chipId][ch] = (data & 0xf0) | 1;
                            volume[chipId][ch] = 256 * 6;
                        } else {
                            keyOn[chipId][ch] = (data & 0xf0) | 0;
                        }
                    } else {
                        keyOn[chipId][2] = data & 0xf0;
                        if ((data & 0x10) > 0)
                            ch3SlotVolume[chipId][0] = 256 * 6;
                        if ((data & 0x20) > 0)
                            ch3SlotVolume[chipId][1] = 256 * 6;
                        if ((data & 0x40) > 0)
                            ch3SlotVolume[chipId][2] = 256 * 6;
                        if ((data & 0x80) > 0)
                            ch3SlotVolume[chipId][3] = 256 * 6;
                    }
                }
            }

            if (port == 1 && addr == 0x01) {
                adpcmPan[chipId] = (data & 0xc0) >> 6;
                if (adpcmPan[chipId] > 0) {
                    adpcmVolume[chipId][0] = (int) ((256 * 6.0 * register[chipId][1][0x0b] / 64.0) *
                            ((adpcmPan[chipId] & 0x02) > 0 ? 1 : 0));
                    adpcmVolume[chipId][1] = (int) ((256 * 6.0 * register[chipId][1][0x0b] / 64.0) *
                            ((adpcmPan[chipId] & 0x01) > 0 ? 1 : 0));
                }
            }

            if (port == 0 && addr == 0x10) {
                int tl = register[chipId][0][0x11] & 0x3f;
                for (int i = 0; i < 6; i++) {
                    if ((data & (0x1 << i)) != 0) {
                        int il = (register[chipId][0][0x18 + i] & 0x1f) * (((data & 0x80) == 0) ? 1 : 0);
                        int pan = (register[chipId][0][0x18 + i] & 0xc0) >> 6;
                        rhythmVolume[chipId][i][0] = (int) (256 * 6 * ((tl * il) >> 4) / 127.0) * ((pan & 2) > 0 ? 1 : 0);
                        rhythmVolume[chipId][i][1] = (int) (256 * 6 * ((tl * il) >> 4) / 127.0) * ((pan & 1) > 0 ? 1 : 0);
                    }
                }
            }
        }

        if ((addr & 0xf0) == 0x40) { // TL
            int ch = (addr & 0x3);
            int al = register[chipId][port][0xb0 + ch] & 0x07;// AL
            int slot = (addr & 0xc) >> 2;
            data &= 0x7f;

            if (ch != 3) {
                if ((algM[al] & (1 << slot)) != 0) {
                    data = Math.min(data + fadeout[chipId], 127);
                    data = isMuted(chipId, port * 3 + ch) ? 127 : data;
                }
            }
        }

        if ((addr & 0xf0) == 0xb0) { // AL
            int ch = (addr & 0x3);
            int al = data & 0x07;// AL

            if (ch != 3 && isMuted(chipId, ch)) {
                for (int slot = 0; slot < 4; slot++) {
                    if ((algM[al] & (1 << slot)) > 0) {
                        int tslot = (slot == 1 ? 2 : (slot == 2 ? 1 : slot)) * 4;
                        write(chipId,
                                port,
                                0x40 + ch + tslot,
                                register[chipId][port][0x40 + ch + tslot],
                                model);
                    }
                }
            }
        }

        // ssg mixer
        if (port == 0 && addr == 0x07) {
            int maskData = 0;
            if (isMuted(chipId, 6))
                maskData |= 0x9 << 0;
            if (isMuted(chipId, 7))
                maskData |= 0x9 << 1;
            if (isMuted(chipId, 8))
                maskData |= 0x9 << 2;
            data |= maskData;
        }

        // ssg level
        if (port == 0 && (addr == 0x08 || addr == 0x09 || addr == 0x0a)) {
            int d = fadeout[chipId] >> 3;
            data = Math.max(data - d, 0);
            data = isMuted(chipId, addr - 0x08 + 6) ? 0 : data;
        }

        // rhythm level
        if (port == 0 && addr == 0x11) {
            int d = fadeout[chipId] >> 1;
            data = Math.max(data - d, 0);
        }

        // adpcm level
        if (port == 1 && addr == 0x0b) {
            int d = fadeout[chipId] * 2;
            data = Math.max(data - d, 0);
            data = isMuted(chipId, 12) ? 0 : data;
        }

        // adpcm start
        if (port == 1 && addr == 0x00) {
            if ((data & 0x80) != 0 && isMuted(chipId, 12)) {
                data &= 0x7f;
            }
        }

        // Rhythm
        if (port == 0 && addr == 0x10) {
            if (isMuted(chipId, 13)) {
                data = 0;
            }
        }

        if (model == EnmModel.VirtualModel) {
            if (!chipTypes[chipId].getUseReal()[0] && chipTypes[chipId].getUseEmu()[0]) {
//if (addr == 0x29) logger.log(Level.TRACE, "%2x:%2x:%2x ".formatted(port, addr, data));
                context.mds.write(inst(chipId), chipId, port, addr, data);
            }
        } else {
            if (realChips[chipId] == null)
                return;

            realChips[chipId].setRegister(port * 0x100 + addr, data);
        }
    }

    public int read(int chipId, int port, int addr, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
            return 0;
        } else {
            if (realChips[chipId] == null)
                return 0;

            return realChips[chipId].getRegister(port * 0x100 + addr);
        }
    }

    private void _write(int chipId, int port, int addr, int data, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
            if (!chipTypes[chipId].getUseReal()[0] && chipTypes[chipId].getUseEmu()[0]) {
                context.mds.write(inst(chipId), chipId, port, addr, data);
            }
        } else {
            if (realChips[chipId] == null)
                return;

            realChips[chipId].setRegister(port * 0x100 + addr, data);
        }
    }

    private void softReset(int chipId, EnmModel model) {
        // FM All Channel Key Off
        _write(chipId, 0, 0x28, 0x00, model);
        _write(chipId, 0, 0x28, 0x01, model);
        _write(chipId, 0, 0x28, 0x02, model);
        _write(chipId, 0, 0x28, 0x04, model);
        _write(chipId, 0, 0x28, 0x05, model);
        _write(chipId, 0, 0x28, 0x06, model);

        // FM TL=127
        for (int i = 0x40; i < 0x4F + 1; i++) {
            _write(chipId, 0, i, 0x7f, model);
            _write(chipId, 1, i, 0x7f, model);
        }
        // FM ML/DT
        for (int i = 0x30; i < 0x3F + 1; i++) {
            _write(chipId, 0, i, 0x0, model);
            _write(chipId, 1, i, 0x0, model);
        }
        // FM AR,DR,SR,KS,AMON
        for (int i = 0x50; i < 0x7F + 1; i++) {
            _write(chipId, 0, i, 0x0, model);
            _write(chipId, 1, i, 0x0, model);
        }
        // FM SL,RR
        for (int i = 0x80; i < 0x8F + 1; i++) {
            _write(chipId, 0, i, 0xff, model);
            _write(chipId, 1, i, 0xff, model);
        }
        // FM F-Num, FB/CONNECT
        for (int i = 0x90; i < 0xBF + 1; i++) {
            _write(chipId, 0, i, 0x0, model);
            _write(chipId, 1, i, 0x0, model);
        }
        // FM PAN/AMS/PMS
        for (int i = 0xB4; i < 0xB6 + 1; i++) {
            _write(chipId, 0, i, 0xc0, model);
            _write(chipId, 1, i, 0xc0, model);
        }
        _write(chipId, 0, 0x22, 0x00, model); // HW LFO
        _write(chipId, 0, 0x24, 0x00, model); // Timer-a(1)
        _write(chipId, 0, 0x25, 0x00, model); // Timer-a(2)
        _write(chipId, 0, 0x26, 0x00, model); // Timer-B
        _write(chipId, 0, 0x27, 0x30, model); // Timer Control
        _write(chipId, 0, 0x29, 0x80, model); // FM4-6 Enable

        // SSG Pitch (2byte*3ch)
        for (int i = 0x00; i < 0x05 + 1; i++) {
            _write(chipId, 0, i, 0x00, model);
        }
        _write(chipId, 0, 0x06, 0x00, model); // SSG Noise Frequency
        _write(chipId, 0, 0x07, 0x38, model); // SSG Mixer
        // SSG volume(3ch)
        for (int i = 0x08; i < 0x0A + 1; i++) {
            _write(chipId, 0, i, 0x00, model);
        }
        // SSG Envelope
        for (int i = 0x0B; i < 0x0D + 1; i++) {
            _write(chipId, 0, i, 0x00, model);
        }

        // RHYTHM
        _write(chipId, 0, 0x10, 0xBF, model); // Forced sound stop
        _write(chipId, 0, 0x11, 0x00, model); // Total Level
        _write(chipId, 0, 0x18, 0x00, model); // BD volume
        _write(chipId, 0, 0x19, 0x00, model); // SD volume
        _write(chipId, 0, 0x1A, 0x00, model); // CYM volume
        _write(chipId, 0, 0x1B, 0x00, model); // HH volume
        _write(chipId, 0, 0x1C, 0x00, model); // TOM volume
        _write(chipId, 0, 0x1D, 0x00, model); // RIM volume

        // ADPCM
        _write(chipId, 1, 0x00, 0x21, model); // ADPCM reset
        _write(chipId, 1, 0x01, 0x06, model); // ADPCM mute
        _write(chipId, 1, 0x10, 0x9C, model); // FLAG reset
    }

    /** FM ch3 and its extended slots mask as one */
    @Override
    protected long linked(int ch) {
        return ch == 2 || (ch >= 9 && ch < 12) ? 0b1110_0000_0100 : super.linked(ch);
    }

    /** the levels are filtered on their way in, see {@link #write}; this re-sends what the chip holds */
    @Override
    protected void applyMute(int chipId, int ch) {

        int c = (ch < 3) ? ch : (ch - 3);
        int p = (ch < 3) ? 0 : 1;

        if (ch < 0) return;
        // a stopped song is left as it is when it is un-muted
        if (!isMuted(chipId, ch) && context.stopped) return;

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
            write(chipId, 0, 0x08 + ch - 6, register[chipId][0][0x08 + ch - 6], EnmModel.VirtualModel);
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
        }
    }

    public void setSyncWait(int chipId, int wait) {
        if (realChips[chipId] != null && chipTypes[chipId].getRealChipInfo()[0].getUseWait()) {
            realChips[chipId].setRegister(-1, (int) (wait * (chipTypes[chipId].getRealChipInfo()[0].getUseWaitBoost() ? 2.0 : 1.0)));
        }
    }

    public void sendData(int chipId, EnmModel model) {
        if (model == EnmModel.VirtualModel)
            return;

        if (realChips[chipId] != null && chipTypes[chipId].getRealChipInfo()[0].getUseWait()) {
            context.chipRegister.plugin(RealChipPlugin.class).realChip.sendData();
            while (!realChips[chipId].isBufferEmpty()) {
            }
        }
    }

    public void sendData(int chipId, EnmModel model, int len, byte[] buf, int ofs) {
        if (model != EnmModel.VirtualModel)
            sendData(chipId, model);

        dumpData(model, "ADPCM", ofs, buf, len);
    }

    @Override
    public void setFadeout(int chipId, int v) {

        fadeout[chipId] = v;

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

    public void writeClock(int chipId, int clock, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
        } else {
            if (realChips != null && realChips[chipId] != null) {
                realChips[chipId].dClock = realChips[chipId].setMasterClock(clock);
            }
        }
    }

    public void setSsgVolume(int chipId, int vol, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
        } else {
            if (realChips != null && realChips[chipId] != null) {
                realChips[chipId].setSSGVolume(vol);
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
        info.put("volume", volume[chipId]);
        info.put("rhythmVolume", rhythmVolume[chipId]);
        info.put("ch3SlotVolume", /* ctYM2612.UseScci ? */ ch3SlotVolume[chipId] /* : context.mds.inst(inst[chipId]).readFMCh3SlotVolume(); */);
        info.put("adpcmVolume", adpcmVolume[chipId]);
        info.put("register", register[chipId]);
        info.put("keyOn", keyOn[chipId]);
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

    private int opnaRamType = 0;

    public void writePcm(int chipId, byte[] vgmBuf, int vgmAdr, int bLen, int startAddress, EnmModel model) {
        write(chipId, 0x1, 0x00, 0x20, model);
        write(chipId, 0x1, 0x00, 0x21, model);
        write(chipId, 0x1, 0x00, 0x00, model);

        write(chipId, 0x1, 0x10, 0x00, model);
        write(chipId, 0x1, 0x10, 0x80, model);

        write(chipId, 0x1, 0x00, 0x61, model);
        write(chipId, 0x1, 0x00, 0x68, model);
        write(chipId, 0x1, 0x01, opnaRamType, model);

        if (opnaRamType != 2) {
            write(chipId, 0x1, 0x02, (startAddress >> 2) & 0xff, model);
            write(chipId, 0x1, 0x03, (startAddress >> 10) & 0xff, model);
        } else {
            write(chipId, 0x1, 0x02, (startAddress >> 5) & 0xff, model);
            write(chipId, 0x1, 0x03, (startAddress >> 13) & 0xff, model);
        }
        write(chipId, 0x1, 0x04, 0xff, model);
        write(chipId, 0x1, 0x05, 0xff, model);
        write(chipId, 0x1, 0x0c, 0xff, model);
        write(chipId, 0x1, 0x0d, 0xff, model);

        // Data Transfer
        for (int cnt = 0; cnt < bLen - 8; cnt++) {
            write(chipId, 0x1, 0x08, vgmBuf[vgmAdr + 15 + cnt] & 0xff, model);
        }
        write(chipId, 0x1, 0x00, 0x00, model);
        write(chipId, 0x1, 0x10, 0x80, model);

//                write(0x1, 0x10, 0x13, model);
//                write(0x1, 0x10, 0x80, model);
//                write(0x1, 0x00, 0x60, model);
//                write(0x1, 0x01, 0x00, model);

//                write(0x1, 0x02, (int) ((startAddress >> 2) & 0xff), model);
//                write(0x1, 0x03, (int) ((startAddress >> 10) & 0xff), model);
//                write(0x1, 0x04, (int) (((startAddress + bLen - 8) >> 2) & 0xff), model);
//                write(0x1, 0x05, (int) (((startAddress + bLen - 8) >> 10) & 0xff), model);
//                write(0x1, 0x0c, 0xff, model);
//                chipRegister.setYM2608Register(0x1, 0x0d, 0xff, model);

//                for (int cnt = 0; cnt < bLen - 8; cnt++) {
//                    write(0x1, 0x08, dataBuf[vgmAdr + 15 + cnt], model);
//                    write(0x1, 0x10, 0x1b, model);
//                    write(0x1, 0x10, 0x13, model);
//                }

//                write(0x1, 0x00, 0x00, model);
//                write(0x1, 0x10, 0x80, model);

        while ((read(chipId, 0x1, 0x00, model) & 0xbf) != 0) {
            try {
                Thread.sleep(0);
            } catch (InterruptedException ignore) {
            }
        }
        if (model == EnmModel.RealModel) {
            if ((chipId == 0 && setting.getYM2608Type()[0].getUseReal()[0])
                    || (chipId == 1 && setting.getYM2608Type()[1].getUseReal()[0])) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignore) {
                }
            }
        }

        sendData(chipId, model, bLen - 8, vgmBuf, vgmAdr + 15);
    }

    /**
     * Check the RAMType of OPNA from the data
     *
     * @return true: x8bit, false: x1bit
     */
    private static boolean searchOpnaRamType(byte[] vgmBuf, int vgmDataOffset) {
        try {
            int adr = vgmDataOffset;

            while (adr < vgmBuf.length && (vgmBuf[adr] & 0xff) != 0x66) {
                int dat = vgmBuf[adr] & 0xff;
                if (dat < 0x51) adr += 2;
                else if (dat < 0x57) adr += 3;
                else if (dat == 0x57) {
                    int reg = vgmBuf[adr + 1] & 0xff;
                    int val = vgmBuf[adr + 2] & 0xff;
                    adr += 3;
                    if (reg == 1) {
                        if ((val & 2) != 0) {
                            return true;
                        }
                    }
                } else if (dat < 0x62) adr += 3;
                else if (dat < 0x64) adr++;
                else if (dat == 0x64) adr += 4;
                else if (dat == 0x66) adr++;
                else if (dat == 0x67) {
                    int bLen = ByteUtil.readLeInt(vgmBuf, adr + 3);
                    bLen &= 0x7fff_ffff;
                    adr += bLen + 7;
                } else if (dat == 0x68) {
                    adr += 12;
                } else if ((dat & 0xff) < 0x90) adr++;
                else if (dat == 0x90) adr += 5;
                else if (dat == 0x91) adr += 5;
                else if (dat == 0x92) adr += 6;
                else if (dat == 0x93) adr += 11;
                else if (dat == 0x94) adr += 2;
                else if (dat == 0x95) adr += 5;
                else if ((dat & 0xff) < 0xc0) adr += 3;
                else if ((dat & 0xff) < 0xe0) adr += 4;
                else adr += 5;
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }

        return false;
    }

    /** TODO consider is here ok? or in vgm */
    public void updateRamType(byte[] vgmBuf, int vgmDataOffset) {
        opnaRamType = searchOpnaRamType(vgmBuf, vgmDataOffset) ? 0x2 : 0x0;
    }

    public InputStream getOPNARyhthmStream(String fn) {
        try {
            Path ffn = Path.of(fn);

            Path chk;

            chk = context.playingFilePath.resolve(fn);
            if (Files.exists(chk))
                ffn = chk;
            else {
                chk = Common.getApplicationFolder().resolve(fn);
                if (Files.exists(chk)) ffn = chk;
                else {
                    // TODO mdsound in mdplayer
                    chk = Path.of(System.getProperty("mdsound.pcm.path", "")).resolve(fn);
                    if (Files.exists(chk)) ffn = chk;
                }
            }

            logger.log(Level.DEBUG, "rhythm file: " + ffn);
            if (!Files.exists(ffn)) return null;
            InputStream fs = Files.newInputStream(ffn);
            return fs;
        } catch (IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            return null;
        }
    }

    public static int searchYM2608Adpcm(float freq) {
        float m = Float.MAX_VALUE;
        int n = 0;

        for (int i = 0; i < 12 * 8; i++) {
            if (freq < Tables.pcmMulTbl[i % 12 + 12] * Math.pow(2, ((i / 12) - 3))) break;
            n = i;
            float a = Math.abs(freq - (float) (Tables.pcmMulTbl[i % 12 + 12] * Math.pow(2, ((i / 12) - 3))));
            if (m > a) {
                m = a;
                n = i;
            }
        }

        return n + 1;
    }

    public static int getOPENAIRRhythmStream(float freq) {
        float m = Float.MAX_VALUE;
        int n = 0;
        for (int i = 0; i < 12 * 8; i++) {
            //if (freq < Tables.freqTbl[i]) break;
            //n = i;
            float a = Math.abs(freq - Tables.freqTbl[i]);
            if (m > a) {
                m = a;
                n = i;
            }
        }
        return n;
    }
}
