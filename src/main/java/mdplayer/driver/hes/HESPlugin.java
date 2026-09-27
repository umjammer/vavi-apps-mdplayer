package mdplayer.driver.hes;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import mdplayer.Common;
import mdplayer.chips.HuC6280Chip;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.BasePlugin.HasSongNo;
import mdsound.MDSound;

import static java.lang.System.getLogger;
import static mdsound.MDSound.Chip.MAIN_TAG;


/**
 * HES (PC-Engine) Plugin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-08 nsano initial version <br>
 */
public class HESPlugin extends BasePlugin<HesDriver> implements HasSongNo {

    private static final Logger logger = getLogger(HESPlugin.class.getName());

    @Override
    public void setSongNo(int songNo) {
logger.log(Level.INFO, "songNo: " + songNo);
        this.songNo = songNo;
    }

    @Override
    public void prepare() {
        driverVirtual = new HesDriver(this);

        driverReal = null;
//        if (setting.getoutputDevice().deviceType != Common.DEV_Null) {
//            driverReal = new HesDriver(this);
//        }

        super.prepare();
        initChips();
    }

    @Override
    protected void initChips() {

        MDSound.Chip chip = new MDSound.Chip();
        chip.id = 0;
        chip.instrument = chipRegister.chip(HuC6280Chip.class).instrument(0);
        chip.additionalUpdate = driverVirtual::additionalUpdate;
        chip.samplingRate = setting.getOutputDevice().getSampleRate();
        chip.volume = setting.getBalance().getVolume(MAIN_TAG, HuC6280Chip.class);
        chip.clock = 3579545;
        chip.option = null;
        put(HuC6280Chip.class, chip);

        mds.init(setting.getOutputDevice().getSampleRate(), BUFFER_SIZE, flatten());

        driverVirtual.init(Common.EnmModel.VirtualModel,
                setting.getOutputDevice().getSampleRate() * setting.getLatencyEmulation() / 1000,
                setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000,
                songNo);
        if (driverReal != null) {
            driverReal.init(Common.EnmModel.RealModel,
                    setting.getOutputDevice().getSampleRate() * setting.getLatencySCCI() / 1000,
                    setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000,
                    songNo);
        }
    }
}
