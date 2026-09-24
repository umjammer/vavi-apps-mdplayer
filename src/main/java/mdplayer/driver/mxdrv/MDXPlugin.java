package mdplayer.driver.mxdrv;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.BiConsumer;

import mdplayer.Common;
import mdplayer.Common.EnmModel;
import mdplayer.chips.Pcm8Chip;
import mdplayer.chips.RealChipPlugin;
import mdplayer.chips.Ym2151Chip;
import mdplayer.driver.BasePlugin;
import mdsound.Instrument;
import mdsound.MDSound;
import mdsound.MDSound.Chip;
import mdsound.instrument.Pcm8PPInst;
import mdsound.instrument.X68kYm2151Inst;

import static java.lang.System.getLogger;
import static mdsound.MDSound.Chip.MAIN_TAG;


/**
 * MXDRV (X68000) Plugin.
 * <p>
 * extendFile[0]: pdx data bytes
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-08 nsano initial version <br>
 */
public class MDXPlugin extends BasePlugin<MxDriver> {

    private static final Logger logger = getLogger(MDXPlugin.class.getName());

    /** what {@link Pcm8Detector} made of the song being played */
    private int detectedPcm8Type = Pcm8Chip.X68SOUND;

    /**
     * Which PCM8 the song being played needs, for {@link mdplayer.Setting#pcm8Type} to answer
     * {@link Pcm8Chip#AUTO} with.
     */
    public int detectedPcm8Type() {
        return detectedPcm8Type;
    }

    @Override
    public void prepare() {
        detectedPcm8Type = Pcm8Detector.detect(getData());
        driverVirtual = new MxDriver(this);
        driverVirtual.setExtendFile((extendFiles != null && !extendFiles.isEmpty()) ? extendFiles.getFirst() : null);

        driverReal = null;
//        if (setting.getOutputDevice().getDeviceType() != Common.DEV_Null) {
//            driverReal = new MxDriver(this);
//            driverReal.setExtendFile((extendFile != null && !extendFile.isEmpty()) ? extendFile.get(0) : null);
//        }

        super.prepare();
        initChips();
    }

    @Override
    protected void initChips() {
        if (setting.getOutputDevice().getSampleRate() != 44100) {
            throw new IllegalStateException("supported sample rate is only 44100: " + setting.getOutputDevice().getSampleRate());
        }

        int hiyorimiDeviceFlag = 3;

        boolean isFirstOpmX68 = false;
        Chip chip = new MDSound.Chip();
        chip.id = 0;
        chip.instrument = chipRegister.chip(Ym2151Chip.class).instrument(0);
        chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym2151Chip.class);
        chip.clock = 4000000;
        if (chip.instrument instanceof X68kYm2151Inst) {
            chip.option = new Object[] {1, 0, 0};
            isFirstOpmX68 = true; // TODO this causes pcm8 off (maybe because of global is singleton)
        }
        chip.samplingRate = chip.clock / 64;
        put(Ym2151Chip.class, chip);

        chip = new MDSound.Chip();
        chip.id = isFirstOpmX68 ? 1 : 0;
        chip.instrument = Instrument.getInstrument(X68kYm2151Inst.class);
        chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym2151Chip.class);
        chip.clock = 4000000;
        chip.samplingRate = chip.clock / 64;
        BiConsumer<Runnable, Boolean> clock = driverVirtual::clock;
        chip.option = new Object[] { -1, 1, 0, clock };
        put(Pcm8Chip.class, chip); // this is different from the original c#, using proper mds way

//        X68kYm2151Inst mdxPCM_R = Instrument.getInstrument(X68kYm2151Inst.class); // real
//        X68kYm2151Inst mdxPCM_P = Instrument.getInstrument(X68kYm2151Inst.class); // piano roll

        if (setting.pcm8Type(this) == 0) {
            // mxdrv is special and requires PCM8
        } else {
            chip = new MDSound.Chip();
            chip.id = 0;
            chip.instrument = Instrument.getInstrument(Pcm8PPInst.class);
            chip.volume = 0; // not PCM8Volume (yet): the calibrated MDX preset says 6, measured while this ignored it
            chip.clock = 4_000_000;
            chip.samplingRate = setting.getOutputDevice().getSampleRate();
            chip.option = new Object[] {setting.pcm8ppsOption(this)};
            put(Pcm8Chip.class, chip);
        }
logger.log(Level.INFO, "pcm8Type: " + setting.pcm8Type(this) + " (detected: " + detectedPcm8Type + "), " + chip.instrument.getClass().getName());

        chipRegister.plugin(RealChipPlugin.class).initChip(hiyorimiDeviceFlag);

        mds.init(setting.getOutputDevice().getSampleRate(), BUFFER_SIZE, flatten());

        setVolume(MAIN_TAG, Ym2151Chip.class, true, setting.getBalance().getVolume(MAIN_TAG, Ym2151Chip.class));

        if (contains(Ym2151Chip.class, 0))
            chipRegister.chip(Ym2151Chip.class).writeClock((byte) 0, 4000000, Common.EnmModel.RealModel);
//            chipRegister.chip(Ym2151Chip.class).writeClock(1, 4000000, enmModel.RealModel);
        chipRegister.chip(Ym2151Chip.class).getUse4MYM2151scci()[0] = false;
        if (setting.getYM2151Type()[0].getUseRealChipFreqDiff() != null
                && setting.getYM2151Type()[0].getUseRealChipFreqDiff().length > 0
                && setting.getYM2151Type()[0].getUseRealChipFreqDiff()[0]) {
            chipRegister.chip(Ym2151Chip.class).getUse4MYM2151scci()[0] = true;
        }

        chipRegister.chip(Ym2151Chip.class).setCorrection(EnmModel.VirtualModel, 4000000);
        if (driverReal != null) chipRegister.chip(Ym2151Chip.class).setCorrection(EnmModel.RealModel, 4000000);
//        chipRegister.chip(Ym2203Chip.class).setSsgVolume(0, setting.getBalance().getGimicOPNAVolume(), EnmModel.RealModel);
//        chipRegister.chip(Ym2203Chip.class).setSsgVolume(1, setting.getBalance().getGimicOPNAVolume(), EnmModel.RealModel);
//        chipRegister.chip(Ym2203Chip.class).setSsgVolume(0, setting.getBalance().getGimicOPNAVolume(), EnmModel.RealModel);
//        chipRegister.chip(Ym2203Chip.class).setSsgVolume(1, setting.getBalance().getGimicOPNAVolume(), EnmModel.RealModel);

        driverVirtual.init(Common.EnmModel.VirtualModel,
                setting.getOutputDevice().getSampleRate() * setting.getLatencyEmulation() / 1000,
                setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000);
        if (driverReal != null) {
            driverReal.init(Common.EnmModel.RealModel,
                    setting.getOutputDevice().getSampleRate() * setting.getLatencySCCI() / 1000,
                    setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000);
        }
    }
}
