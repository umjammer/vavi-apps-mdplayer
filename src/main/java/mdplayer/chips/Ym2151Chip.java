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
import mdplayer.Tables;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdsound.Instrument;
import mdsound.instrument.MameYm2151Inst;
import mdsound.instrument.X68kYm2151Inst;
import mdsound.instrument.Ym2151Inst;
import mdsound.instrument.YmFmYm2151Inst;


/**
 * Ym2151Chip.
 * <p>
 * system property
 * <li>{@code mdplayer.variant.ym2151} ... active chip index</li>
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class Ym2151Chip extends BaseChip {

    private final Setting.ChipType2[] chipTypes = setting.getYM2151Type();

    private final RSoundChip[] realChips = {null, null};

    @Deprecated
    public final int[][] register = {null, null};
    @Deprecated
    public final int[][] keyOn = {null, null};
    @Deprecated
    public final int[][] volume = {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0}
    };

    private final int[] fadeout = {0, 0};

    @Deprecated
    public final int[] amd = {-1, -1};
    @Deprecated
    public final int[] pmd = {-1, -1};

    // TODO check cache or not
    private final boolean[] use4MYM2151scci = {false, false};

    public boolean[] getUse4MYM2151scci() {
        return use4MYM2151scci;
    }

    public final int[] corrections = {0, 0};

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Ym2151Inst.class, MameYm2151Inst.class, X68kYm2151Inst.class, YmFmYm2151Inst.class};
    }

    @Override
    public int activeIndex(int chipId) {
        return chipTypes[chipId].getEnabledId();
    }

    @Override
    public void init(BasePlugin<? extends BaseDriver> context) {
        super.init(context);

        for (int chipId = 0; chipId < 2; chipId++) {
            register[chipId] = new int[0x100];
            for (int i = 0; i < 0x100; i++) {
                register[chipId][i] = 0;
            }
            keyOn[chipId] = new int[] {0, 0, 0, 0, 0, 0, 0, 0};

            fadeout[chipId] = 0;
        }
    }

    @Override
    public void reset() {
        for (int chipId = 0; chipId < 2; chipId++) {
            for (int c = 0; c < 8; c++) {
                write(chipId, 0, 0x60 + c, 127, EnmModel.RealModel, 0, -1);
                write(chipId, 0, 0x68 + c, 127, EnmModel.RealModel, 0, -1);
                write(chipId, 0, 0x70 + c, 127, EnmModel.RealModel, 0, -1);
                write(chipId, 0, 0x78 + c, 127, EnmModel.RealModel, 0, -1);
            }
        }
    }

    @Override
    public void updateVol() {
        for (int chipId = 0; chipId < 2; chipId++) {
            for (int i = 0; i < 8; i++) {
                if (volume[chipId][i] > 0) {
                    volume[chipId][i] -= 50;
                    if (volume[chipId][i] < 0)
                        volume[chipId][i] = 0;
                }
            }
        }
    }

    public void write(int chipId,
                      int port,
                      int addr,
                      int data,
                      EnmModel model,
                      int hosei,
                      long frameCounter) {
        int correction = hosei == 0 ? this.corrections[0] : this.corrections[1];

        fireEventHappened("led.on", chipId);

        if ((model == EnmModel.VirtualModel && (chipTypes[chipId] == null || !chipTypes[chipId].getUseReal()[0])) ||
                (model == EnmModel.RealModel && (realChips != null && realChips[chipId] != null))) {
            register[chipId][addr] = data;
            context.chipRegister.plugin(MidiPlugin.class).export.outMIDIData(Ym2151Chip.class, chipId, port, addr, data, correction, frameCounter);
        }

        if ((model == EnmModel.RealModel && chipTypes[chipId].getUseReal()[0]) ||
                (model == EnmModel.VirtualModel && !chipTypes[chipId].getUseReal()[0])) {
            if (addr == 0x08) { // Key-On/Off
                int ch = data & 0x7;
                if (ch >= 0 && ch < 8) {
                    if ((data & 0x78) != 0) {
                        int con = data & 0x78;
                        keyOn[chipId][ch] = con | 1;
                        volume[chipId][ch] = 256 * 6;
                    } else {
                        keyOn[chipId][ch] &= 0xfe;
                    }
                }
            }
        }

        // AMD/PMD
        if (addr == 0x19) {
            if ((data & 0x80) != 0) {
                pmd[chipId] = data & 0x7f;
            } else {
                amd[chipId] = data & 0x7f;
            }
        }

        if ((addr & 0xf8) == 0x20) {
            int al = data & 0x07; // AL
            int ch = addr & 0x7;

            for (int i = 0; i < 4; i++) {
                int slot = (i == 0) ? 0 : ((i == 1) ? 2 : ((i == 2) ? 1 : 3));
                if ((algM[al] & (1 << slot)) > 0) {
                    if (isMuted(chipId, ch)) {
                        if (model == EnmModel.VirtualModel) {
                            if (!chipTypes[chipId].getUseReal()[0]) {
                                context.mds.write(inst(chipId), chipId, 0, 0x60 + i * 8 + ch, 127);
                            }
                        } else {
                            if (realChips != null && realChips[chipId] != null)
                                realChips[chipId].setRegister(0x60 + i * 8 + ch, 127);
                        }
                    }
                }
            }
        }

        if ((addr & 0xf0) == 0x60 || (addr & 0xf0) == 0x70) { // TL
            int ch = addr & 0x7;
            data &= 0x7f;

            data = Math.min(data + fadeout[chipId], 127);
            data = isMuted(chipId, ch) ? 127 : data;
        }

        if (model == EnmModel.VirtualModel) {
            if (!chipTypes[chipId].getUseReal()[0]) {
                context.mds.write(inst(chipId), chipId, 0, addr, data);
            }
        } else {
            if (realChips[chipId] == null)
                return;

            if (addr >= 0x28 && addr <= 0x2f) {
                if (correction == 0) {
                    realChips[chipId].setRegister(addr, data);
                } else {
                    int oct = (data & 0x70) >> 4;
                    int note = data & 0xf;
                    note = (note < 3) ? note : ((note < 7) ? (note - 1) : ((note < 11) ? (note - 2) : (note - 3)));
                    note += correction - 1;
                    if (note < 0) {
                        oct += (note / 12) - 1;
                        note = (note % 12) + 12;
                    } else {
                        oct += (note / 12);
                        note %= 12;
                    }

                    note = (note < 3) ? note : ((note < 6) ? (note + 1) : ((note < 9) ? (note + 2) : (note + 3)));
                    if (realChips[chipId] != null)
                        realChips[chipId].setRegister(addr, (oct << 4) | note);
                }
            } else {
                realChips[chipId].setRegister(addr, data);
            }
        }
    }

    private void write(int chipId, int port, int addr, int data, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
            if (!chipTypes[chipId].getUseReal()[0]) {
                // the song may not have registered this chip index - MDX starts one OPM under
                // Ym2151Chip and the PCM8's second one under Pcm8Chip - so its emulator was never
                // started (its operators are null). Skip, mirroring the realChips guard below;
                // otherwise softReset's key-off writes to chip 1 hit an uninitialised Ym2151.
                if (context.mds.inst(inst(chipId), chipId) == null) return;
                context.mds.write(inst(chipId), chipId, 0, addr, data);
            }
        } else {
            if (realChips[chipId] != null)
                realChips[chipId].setRegister(addr, data);
        }
    }

    private void softReset(int chipId, EnmModel model) {

        // FM all channel keys off
        for (int i = 0; i < 8; i++) {
            // note off
            write(chipId, 0, 0x08, 0x00 + i, model);
        }

        write(chipId, 0, 0x0f, 0x00, model); // FM NOISE ENABLE/NOISE FREQ
        write(chipId, 0, 0x18, 0x00, model); // FM HW LFO FREQ
        write(chipId, 0, 0x19, 0x80, model); // FM PMD/VALUE
        write(chipId, 0, 0x19, 0x00, model); // FM AMD/VALUE
        write(chipId, 0, 0x1b, 0x00, model); // FM HW LFO WAVEFORM

        // FM HW LFO RESET
        write(chipId, 0, 0x01, 0x02, model);
        write(chipId, 0, 0x01, 0x00, model);

        write(chipId, 0, 0x10, 0x00, model); // FM Timer-a(H)
        write(chipId, 0, 0x11, 0x00, model); // FM Timer-a(L)
        write(chipId, 0, 0x12, 0x00, model); // FM Timer-B
        write(chipId, 0, 0x14, 0x00, model); // FM Timer Control

        for (int i = 0; i < 8; i++) {
            // FB/ALG/PAN
            write(chipId, 0, 0x20 + i, 0x00, model);
            // KC
            write(chipId, 0, 0x28 + i, 0x00, model);
            // KF
            write(chipId, 0, 0x30 + i, 0x00, model);
            // PMS/AMS
            write(chipId, 0, 0x38 + i, 0x00, model);
        }
        for (int i = 0; i < 0x20; i++) {
            // DT1/ML
            write(chipId, 0, 0x40 + i, 0x00, model);
            // TL=127
            write(chipId, 0, 0x60 + i, 0x7f, model);
            // KS/AR
            write(chipId, 0, 0x80 + i, 0x1F, model);
            // AMD/D1R
            write(chipId, 0, 0xa0 + i, 0x00, model);
            // DT2/D2R
            write(chipId, 0, 0xc0 + i, 0x00, model);
            // D1L/RR
            write(chipId, 0, 0xe0 + i, 0x0F, model);
        }
    }

    /** the levels are filtered on their way in, see {@link #write}; this re-sends what the chip holds */
    @Override
    protected void applyMute(int chipId, int ch) {
        if (ch < 0) return;
        // a stopped song is left as it is when it is un-muted
        if (!isMuted(chipId, ch) && context.stopped) return;

        write(chipId, 0, 0x60 + ch, register[chipId][0x60 + ch], EnmModel.VirtualModel, 0, -1);
        write(chipId, 0, 0x68 + ch, register[chipId][0x68 + ch], EnmModel.VirtualModel, 0, -1);
        write(chipId, 0, 0x70 + ch, register[chipId][0x70 + ch], EnmModel.VirtualModel, 0, -1);
        write(chipId, 0, 0x78 + ch, register[chipId][0x78 + ch], EnmModel.VirtualModel, 0, -1);

        write(chipId, 0, 0x60 + ch, register[chipId][0x60 + ch], EnmModel.RealModel, 0, -1);
        write(chipId, 0, 0x68 + ch, register[chipId][0x68 + ch], EnmModel.RealModel, 0, -1);
        write(chipId, 0, 0x70 + ch, register[chipId][0x70 + ch], EnmModel.RealModel, 0, -1);
        write(chipId, 0, 0x78 + ch, register[chipId][0x78 + ch], EnmModel.RealModel, 0, -1);
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

    private int getClock(int chipId) {
        if (realChips[chipId] == null)
            return -1;

        return realChips[chipId].dClock;
    }

    @Override
    public void setFadeout(int chipId, int v) {
        fadeout[chipId] = v;
        for (int c = 0; c < 8; c++) {
            write(chipId, 0, 0x60 + c, register[chipId][0x60 + c], EnmModel.RealModel, 0, -1);
            write(chipId, 0, 0x68 + c, register[chipId][0x68 + c], EnmModel.RealModel, 0, -1);
            write(chipId, 0, 0x70 + c, register[chipId][0x70 + c], EnmModel.RealModel, 0, -1);
            write(chipId, 0, 0x78 + c, register[chipId][0x78 + c], EnmModel.RealModel, 0, -1);
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

    /**
     * What the panels have always read, and beside it the channel state as the chip has it. The
     * fmgen OPM keeps no register file - it decodes into operators - so the visualizer reads the
     * channel view while the register dump still gets the shadow.
     * <p>
     * Not every instrument answers for itself: the X68Sound OPM behind MDX and ZMS decodes straight
     * into its operators and offers no view at all, which used to leave the visualizer with nothing
     * to show. The channel state is therefore derived from this class's own register shadow first,
     * and only then overwritten by whatever the instrument does report - that one is closer to the
     * chip, this one merely always exists.
     */
    @Override
    public Map<String, Object> getInfo(int chipId) {
        Instrument inst = context.mds.inst(inst(chipId));
        if (inst == null) return Collections.emptyMap();
        Map<String, Object> info = new HashMap<>();
        info.put("volume", volume[chipId]);
        info.put("register", register[chipId]);
        info.put("keyOn", keyOn[chipId]);
        info.put("pmd", pmd[chipId]);
        info.put("amd", amd[chipId]);
        info.putAll(shadowInfo(chipId));
        info.putAll(inst.getView(chipId, "info") != null ? inst.getView(chipId, "info") : Collections.emptyMap()); // some variants not implemented
        return info;
    }

    /** the channel state the visualizer wants, read out of the register shadow */
    private Map<String, Object> shadowInfo(int chipId) {
        Map<String, Object> info = new HashMap<>();
        info.put("timerB", register[chipId][0x12]);
        for (int ch = 0; ch < 8; ch++) {
            int panFlCon = register[chipId][0x20 + ch];
            info.put("channels." + ch + ".keyOn", (keyOn[chipId][ch] & 1) != 0);
            info.put("channels." + ch + ".keyCode", register[chipId][0x28 + ch]);
            info.put("channels." + ch + ".totalLevel", carrierTotalLevel(chipId, ch, panFlCon & 0x07));
            info.put("channels." + ch + ".pan", (panFlCon >> 6) & 0x03);
            // the key fraction of register 0x30, the pitch between the key code's note and the
            // next in sixty-fourths of a semitone - where a detune and a portamento end up
            info.put("channels." + ch + ".keyFraction", (register[chipId][0x30 + ch] >> 2) & 0x3f);
            // register 0x38 carries PMS in bits 4-6 and AMS in bits 0-1; hand it over the way the
            // OPN's 0xb4 has them, so that one reader can read either chip
            int ms = register[chipId][0x38 + ch];
            info.put("channels." + ch + ".sensitivity", ((ms >> 4) & 0x07) | ((ms & 0x03) << 4));
            boolean amOn = false;
            for (int slot = 0; slot < 4; slot++) {
                amOn |= (register[chipId][0xa0 + ch + slot * 8] & 0x80) != 0;
            }
            info.put("channels." + ch + ".amOn", amOn);
        }
        return info;
    }

    /** the loudest carrier of channel {@code ch}, i.e. the one that sets how loud the channel is */
    private int carrierTotalLevel(int chipId, int ch, int al) {
        int tl = 127;
        for (int i = 0; i < 4; i++) {
            // the TL registers run m1, m2, c1, c2 while the algorithm mask runs m1, c1, m2, c2
            int slot = (i == 0) ? 0 : ((i == 1) ? 2 : ((i == 2) ? 1 : 3));
            if ((algM[al] & (1 << slot)) != 0) {
                tl = Math.min(tl, register[chipId][0x60 + i * 8 + ch] & 0x7f);
            }
        }
        return tl;
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

    public void setCorrection(EnmModel model, float clockValue) {
        for (int chipId = 0; chipId < 2; chipId++) {
            corrections[chipId] = getCorrection(clockValue, 3579545);
            if (model == EnmModel.RealModel) {
                corrections[chipId] = 0;
                int clock = context.chipRegister.chip(Ym2151Chip.class).getClock(chipId);
                if (clock != -1) {
                    corrections[chipId] = getCorrection(clockValue, clock);
                }
            }
        }
    }

    private static int getCorrection(float clockValue, float baseClock) {
        int ret = 0;

        float delta = clockValue / baseClock;
        float d;
        float oldD = Float.MAX_VALUE;
        for (int i = 0; i < Tables.pcmMulTbl.length; i++) {
            d = Math.abs(delta - Tables.pcmMulTbl[i]);
            ret = i;
            if (d > oldD) break;
            oldD = d;
        }
        ret -= 12;

        return ret;
    }
}
