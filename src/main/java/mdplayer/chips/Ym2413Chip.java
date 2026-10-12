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
import mdplayer.instruments.Vrc7Inst;
import mdplayer.driver.BasePlugin;
import mdsound.Instrument;
import mdsound.instrument.Emu2413Inst;
import mdsound.instrument.NpYm2413Inst;
import mdsound.instrument.Ym2413Inst;


/**
 * Ym2413Chip.
 * <p>
 * system property
 * <li>{@code mdplayer.variant.ym2413} ... active chip index</li>
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class Ym2413Chip extends BaseChip {

    private final Setting.ChipType2[] chipTypes = setting.getYM2413Type();

    private final RSoundChip[] realChips = {null, null};

    @Deprecated
    private final ChipKeyInfo[] keyInfo = {new ChipKeyInfo(14), new ChipKeyInfo(14)};

    private final int[] fadeout = {0, 0};

    @Deprecated
    private final boolean[] rm = {false, false};

    /**
     * The key registers - {@code $0e} and {@code $20-$28} - as the song wrote them, before
     * {@link #write} took the muted channels' keys out: the emulator holds them filtered, and an
     * un-muted note is keyed on again from here.
     */
    private final int[][] keyRegisters = new int[2][0x29];

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Ym2413Inst.class, Vrc7Inst.class, Emu2413Inst.class, NpYm2413Inst.class};
    }

    @Override
    public int activeIndex(int chipId) {
        return Integer.parseInt(System.getProperty("mdplayer.variant.ym2413", "0")); //!((Vgm) context.driverVirtual).ym2413VRC7Flag ? 0 : 1; // TODO setting
    }

    @Override
    public void init(BasePlugin<? extends BaseDriver> context) {
        super.init(context);

        for (int chipId = 0; chipId < 2; chipId++) {
//            registerRhythm[0] = 0;
//            registerRhythm[1] = 0;
//            registerRhythmB[0] = 0;
//            registerRhythmB[1] = 0;

            fadeout[chipId] = 0;
            rm[chipId] = false; // same as register[0x0e], the rhythm mode of the previous song must not be left
            Arrays.fill(keyRegisters[chipId], 0); // nor its keys
        }
    }

    public void write(int chipId, int addr, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel)

            if (addr == 0x0e) {
                rm[chipId] = (data & 0x20) != 0;
            }

        if (addr == 0x0e || (addr >= 0x20 && addr <= 0x28)) {
            keyRegisters[chipId][addr] = data;
        }

        if (addr >= 0x20 && addr <= 0x28) {
            int ch = addr - 0x20;
            int k = data & 0x10;
            if (k == 0) {
                keyInfo[chipId].off[ch] = true;
            } else {
                if (keyInfo[chipId].off[ch])
                    keyInfo[chipId].on[ch] = true;
                keyInfo[chipId].off[ch] = false;
            }

            // Apply mask
            if (isMuted(chipId, ch))
                data &= 0xef;
        }

        if (addr >= 0x30 && addr < 0x39) { // TL
            int inst = data & 0xf0;
            int tl = data & 0x0f;
            int ch = addr - 0x30;

            if (addr < 0x36 || !rm[chipId]) {
                data = Math.min(tl + fadeout[chipId], 0x0f);
                data = inst | data;
            } else {
                data = Math.min(tl + fadeout[chipId], 0x0f);
                if (addr > 0x36)
                    data = (data << 4) | data;
            }
        }

        if (addr == 0x0e) {
            for (int c = 0; c < 5; c++) {
                if ((data & (0x10 >> c)) == 0) {
                    keyInfo[chipId].off[c + 9] = true;
                } else {
                    if (keyInfo[chipId].off[c + 9])
                        keyInfo[chipId].on[c + 9] = true;
                    keyInfo[chipId].off[c + 9] = false;
                }
            }

            data = (data & 0x20) | (isMuted(chipId, 9) ? 0 : (data & 0x10)) |
                    (isMuted(chipId, 10) ? 0 : (data & 0x08)) | (isMuted(chipId, 11) ? 0 : (data & 0x04)) |
                    (isMuted(chipId, 12) ? 0 : (data & 0x02)) | (isMuted(chipId, 13) ? 0 : (data & 0x01));
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

    private ChipKeyInfo getKeyInfo(int chipId) {
        ChipKeyInfo[] keyInfoRet = {new ChipKeyInfo(14), new ChipKeyInfo(14)}; // TODO move to outer for memory usage? why copy?
        for (int ch = 0; ch < keyInfo[chipId].off.length; ch++) {
            keyInfoRet[chipId].off[ch] = keyInfo[chipId].off[ch];
            keyInfoRet[chipId].on[ch] = keyInfo[chipId].on[ch];
            keyInfo[chipId].on[ch] = false;
        }
        return keyInfoRet[chipId];
    }

    private void softReset(int chipId, EnmModel model) {
        // FM All Channel Key Off
        for (int ch = 0; ch < 9; ch++) {
            write(chipId, 0x20 + ch, 0x00, model);
        }
        write(chipId, 0x0e, 0x00, model);

        // FM TL=15
        for (int ch = 0; ch < 9; ch++) {
            write(chipId, 0x30 + ch, 0x0f, model);
        }
        write(chipId, 0x36, 0x0f, model);
        write(chipId, 0x37, 0xff, model);
        write(chipId, 0x38, 0xff, model);
    }

    /** the key ons are filtered on their way in, see {@link #write} */
    @Override
    protected void applyMute(int chipId, int ch) {
        if (ch < 0) return;
        // re-send the keys as the song has them, so the mute takes effect on the running note
        if (ch < 9) {
            write(chipId, 0x20 + ch, keyRegisters[chipId][0x20 + ch], EnmModel.VirtualModel);
        } else if (ch < 14) {
            write(chipId, 0x0e, keyRegisters[chipId][0x0e], EnmModel.VirtualModel);
        }
    }

    @Override
    public void setFadeout(int chipId, int v) {
        fadeout[chipId] = v / (128 / 16);
        // the levels were re-sent here for the real chip only, which no longer has a path
    }

    @Override
    public Map<String, Object> getInfo(int chipId) {
        Instrument inst = context.mds.inst(inst(chipId));
        if (inst == null) return Collections.emptyMap();
        Map<String, Object> info = new HashMap<>();
        info.put("keyInfo", getKeyInfo(chipId));
        info.putAll(inst.getView(chipId, "register") != null ? inst.getView(chipId, "register") : Collections.emptyMap()); // some variants not implemented
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
