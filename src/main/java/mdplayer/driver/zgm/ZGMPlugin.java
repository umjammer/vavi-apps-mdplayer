package mdplayer.driver.zgm;

import java.lang.System.Logger;

import mdplayer.Common;
import mdplayer.chips.RealChipPlugin;
import mdplayer.chips.GigatronChip;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.zgm.zgmChip.Gigatron;
import mdplayer.driver.zgm.zgmChip.ZgmChip;
import mdsound.MDSound;

import static java.lang.System.getLogger;
import static mdsound.MDSound.Chip.MAIN_TAG;


/**
 * ZGMPlugin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-08 nsano initial version <br>
 */
public class ZGMPlugin extends BasePlugin<ZgmDriver> {

    private static final Logger logger = getLogger(ZGMPlugin.class.getName());

    @Override
    public void prepare() {
        driverVirtual = new ZgmDriver(this);

        driverReal = null;
        if (setting.getOutputDevice().getDeviceType() != Common.DEV_Null) {
            driverReal = new ZgmDriver(this);
        }

        super.prepare();
        initChips();
    }

    @Override
    protected void initChips() {
        // Sealed until MIDI is supported
//        startTrdVgmReal();

        // parses the defines, so the chips below are known
        driverVirtual.init(Common.EnmModel.VirtualModel,
                setting.getOutputDevice().getSampleRate() * setting.getLatencyEmulation() / 1000,
                setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000);

        // Sealed until MIDI is supported
//            if (driverReal != null)
//                 driverReal.init(dataBuf, this, EnmModel.RealModel,
//                    new EnmChip[] {EnmChip.YM2203},
//                    (int) (setting.getoutputDevice().getSampleRate() * setting.getLatencySCCI() / 1000),
//                    (int) (setting.getoutputDevice().getSampleRate() * setting.getoutputDevice().getWaitTime() / 1000));

        int hiyorimiDeviceFlag = 0;

        //
        // chips initialization
        //

        for (ZgmChip zc : driverVirtual.getChips()) {
            if (zc instanceof Gigatron) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = zc.getIndex();
                chip.instrument = chipRegister.chip(GigatronChip.class).instrument(chip.id);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, GigatronChip.class);
                chip.clock = zc.getDefineInfo().clock;
                chip.option = null;
                hiyorimiDeviceFlag |= 0x2;

                put(GigatronChip.class, chip);
            }
        }

        chipRegister.plugin(RealChipPlugin.class).initChip(hiyorimiDeviceFlag);

        mds.init(setting.getOutputDevice().getSampleRate(), BUFFER_SIZE, flatten());

        if (chips.containsKey(GigatronChip.class))
            setVolume(MAIN_TAG, GigatronChip.class, true, setting.getBalance().getVolume(MAIN_TAG, GigatronChip.class));
    }
}
