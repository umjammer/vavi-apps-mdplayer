/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.ay;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import mdplayer.Common.EnmModel;
import mdplayer.chips.Ay8910Chip;
import mdplayer.chips.ZxBeepChip;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.BasePlugin.HasSongNo;
import mdsound.MDSound;
import mdsound.instrument.MameAy8910Inst;

import static java.lang.System.getLogger;
import static mdsound.MDSound.Chip.MAIN_TAG;


/**
 * AyPlugin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-02-19 nsano initial version <br>
 */
public class AyPlugin extends BasePlugin<AyDriver> implements HasSongNo {

    private static final Logger logger = getLogger(AyPlugin.class.getName());

    @Override
    public void setSongNo(int songNo) {
logger.log(Level.INFO, "songNo: " + songNo);
        this.songNo = songNo;
    }

    @Override
    public void prepare() {
        driverVirtual = new AyDriver(this);

        driverReal = null;
//        if (setting.getOutputDevice().getDeviceType() != Common.DEV_Null) {
//            audio.driverReal = new AyDriver(this);
//        }

        super.prepare();
        initChips();
    }

    @Override
    protected void initChips() {
        MDSound.Chip chip = new MDSound.Chip();
        chip.id = 0;
        chip.instrument = chipRegister.chip(Ay8910Chip.class).instrument(0);
        chip.option = null;
        if (chip.instrument instanceof MameAy8910Inst) {
            chip.option = new Object[] {
                    (setting.getAY8910Type()[0].getYM2149mode() ? 0x10 : 0x00), // chip_type 0x10: YM2149, 0x00: AY
                    0x00 // chip_flag
            };
        }
        chip.samplingRate = setting.getOutputDevice().getSampleRate();
        chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ay8910Chip.class);
        chip.clock = 1789773 / 2;
        put(Ay8910Chip.class, chip);

        chip = new MDSound.Chip();
        chip.id = 0;
        chip.instrument = chipRegister.chip(ZxBeepChip.class).instrument(0);
        chip.option = null;
        chip.samplingRate = setting.getOutputDevice().getSampleRate();
        chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ay8910Chip.class);
        chip.clock = 1789773 / 2;
        put(ZxBeepChip.class, chip);

        mds.init(setting.getOutputDevice().getSampleRate(), BUFFER_SIZE, flatten());

        driverVirtual.init(EnmModel.VirtualModel,
                setting.getOutputDevice().getSampleRate() * setting.getLatencyEmulation() / 1000,
                setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000,
                songNo);
        if (driverReal != null) {
            driverReal.init(EnmModel.RealModel,
                    setting.getOutputDevice().getSampleRate() * setting.getLatencySCCI() / 1000,
                    setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000,
                    songNo);
        }
    }
}
