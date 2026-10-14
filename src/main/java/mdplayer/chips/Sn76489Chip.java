/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.util.Map;

import mdplayer.Common;
import mdplayer.Common.EnmModel;
import mdplayer.RealChip.RSoundChip;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdsound.Instrument;
import mdsound.Instrument.PannableInstrument;
import mdsound.instrument.Sn76489Inst;
import mdsound.instrument.Sn76496Inst;


/**
 * Sn76489Chip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class Sn76489Chip extends BaseChip {

    private final Setting.ChipType2[] chipTypes = setting.getSN76489Type();

    private final RSoundChip[] realChips = {null, null};

    /**
     * The registers as they were written. Views must not read this - {@link #getInfo} asks the
     * chip instead. It survives because the real chip path has no emulator to ask: when the song
     * is driving hardware over SCCI, this is the only record of what was sent.
     */
    @Deprecated
    private final int[][] register = {null, null};

    @Deprecated
    public final int[] pan = {0xff, 0xff};

    @Deprecated
    public final int[][][] volumes = {
            {new int[2], new int[2], new int[2], new int[2]},
            {new int[2], new int[2], new int[2], new int[2]}
    };

    private final int[] fadeout = {0, 0};

    @Deprecated
    private final int[] latchedRegister = {
            0, 0
    };

    @Deprecated
    private final int[] noiseFreq = {
            0, 0
    };


    @SuppressWarnings("unchecked")
    private Class<? extends PannableInstrument> _inst(int chipId) {
        return (Class<? extends PannableInstrument>) inst(chipId);
    }

    @Override
    public int activeIndex(int chipId) {
        return chipTypes[chipId].getEnabledId();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Sn76489Inst.class, Sn76496Inst.class};
    }

    @Override
    public void init(BasePlugin<? extends BaseDriver> context) {
        super.init(context);

        for (int chipId = 0; chipId < 2; chipId++) {
            register[chipId] = new int[] {0, 15, 0, 15, 0, 15, 0, 15};

            fadeout[chipId] = 0;
        }
    }

    @Override
    public void reset() {
        for (int chipId = 0; chipId < 2; chipId++) {
            for (int c = 0; c < 4; c++) {
                write(chipId, 0x90 + (c << 5) + 0xf, EnmModel.RealModel);
            }
        }
    }

    public void write(int chipId, int data, EnmModel model) {

        fireEventHappened("led.on", chipId);

        write(chipId, data);

        if ((data & 0x10) != 0) {
            if (latchedRegister[chipId] != 0 && latchedRegister[chipId] != 2 && latchedRegister[chipId] != 4 &&
                    latchedRegister[chipId] != 6) {
                volumes[chipId][(data & 0x60) >> 5][0] = (15 - (data & 0xf)) *
                        ((pan[chipId] >> (((data & 0x60) >> 5) + 4)) &
                                0x1);
                volumes[chipId][(data & 0x60) >> 5][1] = (15 - (data & 0xf)) *
                        ((pan[chipId] >> ((data & 0x60) >> 5)) & 0x1);

                int v = data & 0xf;
                v = v + fadeout[chipId];
                v = isMuted(chipId, (data & 0x60) >> 5) ? 15 : v;
                v = Math.min(v, 15);
                data = (data & 0xf0) | v;
            }
        }

        if (model == EnmModel.RealModel) {
            if (chipTypes[chipId].getUseReal()[0]) {
                if (realChips[chipId] == null)
                    return;
                realChips[chipId].setRegister(0, data);
            }
        } else {
            if (!chipTypes[chipId].getUseReal()[0]) {
                context.mds.write(_inst(chipId), chipId, 0, 0, data);
            }
        }
    }

    public void setPan(int chipId, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.RealModel) {
            if (chipTypes[chipId].getUseReal()[0]) {
                if (realChips[chipId] == null) {
                }
            }
        } else {
            if (!chipTypes[chipId].getUseReal()[0]) {
                context.mds.inst(_inst(chipId)).setPan(chipId, data);
                pan[chipId] = data;
            }
        }
    }

    private void write(int chipId, int data) {
        if ((data & 0x80) != 0) {
            // Latch/data byte %1 cc t dddd
            latchedRegister[chipId] = (data >> 4) & 0x07;
            register[chipId][latchedRegister[chipId]] = (register[chipId][latchedRegister[chipId]] &
                    0x3f0) | // zero low 4 bits
                    (data & 0xf); // and replace with data
        } else {
            // data byte %0 - dddddd
            if ((latchedRegister[chipId] % 2) == 0 && (latchedRegister[chipId] < 5))
                // Tone register
                register[chipId][latchedRegister[chipId]] = (register[chipId][latchedRegister[chipId]] &
                        0x00f) |  // zero high 6 bits
                        ((data & 0x3f) << 4); // and replace with data
            else
                // Other register
                register[chipId][latchedRegister[chipId]] = data & 0x0f; // Replace with data
        }
        switch (latchedRegister[chipId]) {
            case 0:
            case 2:
            case 4: // Tone channels
//                if (register[chipId][latchedRegister[chipId]] == 0)
//                    register[chipId][latchedRegister[chipId]] = 1; // Zero frequency changed to 1 to avoid div/0
                break;
            case 6: // Noise
                noiseFreq[chipId] = 0x10 << (register[chipId][6] & 0x3); // set noise signal generator frequency
                break;
        }
    }

    @Override
    public void setFadeout(int chipId, int v) {
        fadeout[chipId] = (v & 0x78) >> 3;
        for (int c = 0; c < 4; c++) {

            write(chipId, 0x90 + (c << 5) + register[chipId][1 + (c << 1)], EnmModel.RealModel);
        }
    }

    /**
     * The registers as the chip has them, beside the state the player keeps itself - the panning
     * and the Neo Geo Pocket flag are the player's own, not the chip's.
     */
    @Override
    public Map<String, Object> getInfo(int chipId) {
        Instrument inst = context.mds.inst(inst(chipId));
        int[] regs = inst == null ? new int[8]
                : (int[]) inst.getView(chipId, "register").get(inst.getName());
        return Map.of(
                "volumes", volumes[chipId],
                "register", regs == null ? new int[8] : regs,
                "pan", pan[chipId],
                "flag", ngpFlag
        );
    }

    /** the levels are filtered on their way in, see {@link #write} */
    @Override
    protected void applyMute(int chipId, int ch) {
        // re-send the level the chip already has, so the mute takes on the running note
        if (ch >= 0 && ch < 4) sendVolumeForced(chipId, ch);
    }

    private void sendVolumeForced(int chipId, int ch) {
        Setting.ChipType2 ct = setting.getSN76489Type()[chipId];
        write(chipId, (0x90 |
                        ((ch & 3) << 5) |
                        (15 - (Math.max(volumes[chipId][ch][0], volumes[chipId][ch][1]) & 0xf))),
                ct.getUseEmu()[0] ? Common.EnmModel.VirtualModel : Common.EnmModel.RealModel);
    }

    // ??? vgm
    public boolean ngpFlag = false;

    @Override
    public void clearFadeout() {
        setFadeout(0, 0);
        setFadeout(1, 0);
    }
}
