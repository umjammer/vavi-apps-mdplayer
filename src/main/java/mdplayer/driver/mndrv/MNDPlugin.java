package mdplayer.driver.mndrv;

import java.io.InputStream;
import java.lang.System.Logger;
import java.util.function.Function;

import mdplayer.Common.EnmModel;
import mdplayer.chips.MPcmChip;
import mdplayer.chips.RealChipPlugin;
import mdplayer.chips.Ym2151Chip;
import mdplayer.chips.Ym2203Chip;
import mdplayer.chips.Ym2608Chip;
import mdplayer.driver.BasePlugin;
import mdsound.MDSound;
import mdsound.instrument.X68kYm2151Inst;
import mdsound.instrument.Ym2608Inst;

import static java.lang.System.getLogger;
import static mdsound.MDSound.Chip.MAIN_TAG;


/**
 * MNDRV (X68000) Plugin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-08 nsano initial version <br>
 */
public class MNDPlugin extends BasePlugin<MnDriver> {

    private static final Logger logger = getLogger(MNDPlugin.class.getName());

    @Override
    public void prepare() {
        driverVirtual = new MnDriver(this);
        driverVirtual.setExtendFile(extendFiles);

        driverReal = null;
//        if (setting.getOutputDevice().getDeviceType() != Common.DEV_Null) {
//            driverReal = new MnDriver(this);
//            driverReal.setExtendFile(extendFile);
//        }

        super.prepare();
        initChips();
    }

    @Override
    protected void initChips() {
        int hiyorimiDeviceFlag = 3;

        MDSound.Chip chip = new MDSound.Chip();
        chip.id = 0;
        chip.instrument = chipRegister.chip(Ym2151Chip.class).instrument(0);
        chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym2151Chip.class);
        chip.clock = 4000000;
        chip.samplingRate = chip.clock / 64;
        if (chip.instrument instanceof X68kYm2151Inst) {
            chip.option = new Object[] { 1, 0, 0 };
        } else {
            chip.option = null;
        }
        put(Ym2151Chip.class, chip);

        if (setting.getYM2608Type()[0].getUseEmu()[0]) {
            chip = new MDSound.Chip();
            chip.id = 0;
            chip.instrument = chipRegister.chip(Ym2608Chip.class).instrument(0);
            chip.samplingRate = 55467; // (int) setting.getoutputDevice().getSampleRate();
            chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym2608Chip.class);
            chip.clock = 8000000; // 7987200;
            if (chip.instrument instanceof Ym2608Inst opna) {
                chip.setVolumes.put("FM", opna::setVolume);
                chip.setVolumes.put("SSG", opna::setVolume);
                chip.setVolumes.put("RHYTHM", opna::setVolume);
                chip.setVolumes.put("ADPCM", opna::setVolume);
            }
            Function<String, InputStream> fn = chipRegister.chip(Ym2608Chip.class)::getOPNARyhthmStream;
            chip.option = new Object[] {fn};
            put(Ym2608Chip.class, chip);
        }

        if (setting.getYM2608Type()[1].getUseEmu()[0]) {
            chip = new MDSound.Chip();
            chip.id = 1;
            chip.instrument = chipRegister.chip(Ym2608Chip.class).instrument(1);
            chip.samplingRate = 55467; // (int) setting.getoutputDevice().getSampleRate();
            chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym2608Chip.class);
            chip.clock = 8000000; // 7987200;
            if (chip.instrument instanceof Ym2608Inst opna) {
                chip.setVolumes.put("FM", opna::setVolume);
                chip.setVolumes.put("SSG", opna::setVolume);
                chip.setVolumes.put("RHYTHM", opna::setVolume);
                chip.setVolumes.put("ADPCM", opna::setVolume);
            }
            Function<String, InputStream> fn = chipRegister.chip(Ym2608Chip.class)::getOPNARyhthmStream;
            chip.option = new Object[] {fn};
//            chip.option = new Object[] {Common.getApplicationFolder()};
            put(Ym2608Chip.class, chip);
        }

        chip = new MDSound.Chip();
        chip.id = 0;
        chip.instrument = chipRegister.chip(MPcmChip.class).instrument(0);
        chip.samplingRate = setting.getOutputDevice().getSampleRate();
        chip.clock = 15600;
        chip.volume = setting.getBalance().getVolume(MAIN_TAG, MPcmChip.class);
        chip.option = null;
        put(MPcmChip.class, chip);

        chipRegister.plugin(RealChipPlugin.class).initChip(hiyorimiDeviceFlag);

        mds.init(setting.getOutputDevice().getSampleRate(), BUFFER_SIZE, flatten());

        if (contains(Ym2151Chip.class, 0) || contains(Ym2151Chip.class, 1)) {
            setVolume(MAIN_TAG, Ym2151Chip.class, true, setting.getBalance().getVolume(MAIN_TAG, Ym2151Chip.class));
        }

        if (contains(Ym2608Chip.class, 0) || contains(Ym2608Chip.class, 1)) {
            setVolume(MAIN_TAG, Ym2608Chip.class, true, setting.getBalance().getVolume(MAIN_TAG, Ym2608Chip.class));
            setVolume("FM", Ym2608Chip.class, true, setting.getBalance().getVolume("FM", Ym2608Chip.class));
            setVolume("SSG", Ym2608Chip.class, true, setting.getBalance().getVolume("SSG", Ym2608Chip.class));
            setVolume("RHYTHM", Ym2608Chip.class, true, setting.getBalance().getVolume("RHYTHM", Ym2608Chip.class));
            setVolume("ADPCM", Ym2608Chip.class, true, setting.getBalance().getVolume("ADPCM", Ym2608Chip.class));
        }

        if (contains(Ym2608Chip.class, 0)) {
            chipRegister.chip(Ym2608Chip.class).write(0, 0, 0x2d, 0x00, EnmModel.VirtualModel);
            chipRegister.chip(Ym2608Chip.class).write(0, 0, 0x2d, 0x00, EnmModel.RealModel);
            chipRegister.chip(Ym2608Chip.class).write(0, 0, 0x29, 0x82, EnmModel.VirtualModel);
            chipRegister.chip(Ym2608Chip.class).write(0, 0, 0x29, 0x82, EnmModel.RealModel);
            chipRegister.chip(Ym2608Chip.class).write(0, 0, 0x07, 0x38, EnmModel.VirtualModel); // Reset with Psg TONE
            chipRegister.chip(Ym2608Chip.class).write(0, 0, 0x07, 0x38, EnmModel.RealModel);
            chipRegister.chip(Ym2608Chip.class).writeClock((byte) 0, 8000000, EnmModel.RealModel);
            chipRegister.chip(Ym2608Chip.class).setSsgVolume((byte) 0, setting.getBalance().getGimicOPNAVolume(), EnmModel.RealModel);
        }

        if (contains(Ym2608Chip.class, 0)) {
            chipRegister.chip(Ym2608Chip.class).write(1, 0, 0x2d, 0x00, EnmModel.VirtualModel);
            chipRegister.chip(Ym2608Chip.class).write(1, 0, 0x2d, 0x00, EnmModel.RealModel);
            chipRegister.chip(Ym2608Chip.class).write(1, 0, 0x29, 0x82, EnmModel.VirtualModel);
            chipRegister.chip(Ym2608Chip.class).write(1, 0, 0x29, 0x82, EnmModel.RealModel);
            chipRegister.chip(Ym2608Chip.class).write(1, 0, 0x07, 0x38, EnmModel.VirtualModel); // Reset with Psg TONE
            chipRegister.chip(Ym2608Chip.class).write(1, 0, 0x07, 0x38, EnmModel.RealModel);
            chipRegister.chip(Ym2608Chip.class).writeClock((byte) 1, 8000000, EnmModel.RealModel);
            chipRegister.chip(Ym2608Chip.class).setSsgVolume((byte) 1, setting.getBalance().getGimicOPNAVolume(), EnmModel.RealModel);
        }

        if (contains(Ym2151Chip.class, 0))
            chipRegister.chip(Ym2151Chip.class).writeClock((byte) 0, 4000000, EnmModel.RealModel);
        if (contains(Ym2151Chip.class, 1))
            chipRegister.chip(Ym2151Chip.class).writeClock((byte) 1, 4000000, EnmModel.RealModel);

        chipRegister.chip(Ym2151Chip.class).setCorrection(EnmModel.VirtualModel, 4000000);
        if (driverReal != null) chipRegister.chip(Ym2151Chip.class).setCorrection(EnmModel.RealModel, 4000000);

        if (contains(Ym2203Chip.class, 0))
            chipRegister.chip(Ym2203Chip.class).setSsgVolume((byte) 0, setting.getBalance().getGimicOPNVolume(), EnmModel.RealModel);
        if (contains(Ym2203Chip.class, 1))
            chipRegister.chip(Ym2203Chip.class).setSsgVolume((byte) 1, setting.getBalance().getGimicOPNVolume(), EnmModel.RealModel);

        driverVirtual.init(EnmModel.VirtualModel,
                setting.getOutputDevice().getSampleRate() * setting.getLatencyEmulation() / 1000,
                setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000);
        if (driverReal != null) {
            driverReal.init(EnmModel.RealModel,
                    setting.getOutputDevice().getSampleRate() * setting.getLatencySCCI() / 1000,
                    setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000);
        }
    }
}
