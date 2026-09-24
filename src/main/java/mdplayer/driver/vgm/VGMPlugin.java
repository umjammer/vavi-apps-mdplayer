package mdplayer.driver.vgm;

import java.io.InputStream;
import java.lang.System.Logger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import mdplayer.Common;
import mdplayer.Common.EnmModel;
import mdplayer.chips.*;
import mdplayer.chips.NesChip.DmcChip;
import mdplayer.chips.NesChip.FdsChip;
import mdplayer.driver.BasePlugin;
import mdsound.MDSound;
import mdsound.chips.C352;
import mdsound.chips.Ym3438Const;
import mdsound.instrument.C140Inst;
import mdsound.instrument.C352Inst;
import mdsound.instrument.MameAy8910Inst;
import mdsound.instrument.Sn76496Inst;
import mdsound.instrument.Ym2203Inst;
import mdsound.instrument.Ym2608Inst;
import mdsound.instrument.Ym2610Inst;
import mdsound.instrument.Ym2612Inst;
import mdsound.instrument.Ym3438Inst;
import mdsound.instrument.YmF262Inst;
import musicDriverInterface.MetaData.Tag;

import static java.lang.System.getLogger;
import static mdsound.MDSound.Chip.MAIN_TAG;


/**
 * VGMPlugin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-08 nsano initial version <br>
 */
public class VGMPlugin extends BasePlugin<VgmDriver> {

    private static final Logger logger = getLogger(VGMPlugin.class.getName());

    @Override
    public void prepare() {
        driverVirtual = new VgmDriver(this);

        driverReal = null;
//        if (setting.getOutputDevice().getDeviceType() != Common.DEV_Null) {
//            driverReal = new VgmDriver(this);
//        }

        super.prepare();
        initChips();
    }

    @Override
    protected void initChips() {
        driverVirtual.init(EnmModel.VirtualModel,
                setting.getOutputDevice().getSampleRate() * setting.getLatencyEmulation() / 1000,
                setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000);

        if (driverReal != null)
            driverReal.init(EnmModel.RealModel,
                    setting.getOutputDevice().getSampleRate() * setting.getLatencySCCI() / 1000,
                setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000);

        int hiyorimiDeviceFlag = 0;

        if (driverVirtual.vgm.sn76489ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.sn76489DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.option = null;
                chip.instrument = chipRegister.chip(Sn76489Chip.class).instrument(i);
                if (chip.instrument instanceof Sn76496Inst) {
                    chip.option = driverVirtual.vgm.sn76489Option;
                }
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Sn76489Chip.class);
                chip.clock = driverVirtual.vgm.sn76489ClockValue |
                        (driverVirtual.vgm.sn76489NGPFlag ? 0x8000_0000 : 0);

                hiyorimiDeviceFlag |= (setting.getSN76489Type()[0].getUseReal()[0]) ? 0x1 : 0x2;
                chipRegister.chip(Sn76489Chip.class).ngpFlag = driverVirtual.vgm.sn76489NGPFlag;

                put(Sn76489Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.ym2612ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ym2612DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.option = null;
                chip.instrument = chipRegister.chip(Ym2612Chip.class).instrument(i);
                if (chip.instrument instanceof Ym2612Inst ||
                        setting.getYM2612Type()[i].getRealChipInfo()[0].getOnlyPCMEmulation() ||
                        setting.getYM2612Type()[i].getUseReal()[0]
                ) {
                    chip.option = new Object[] {
                            (setting.getNukedOPN2().gensDACHPF ? 0x01 : 0x00) | (setting.getNukedOPN2().gensSSGEG ? 0x02 : 0x00)
                    };
                } else if (chip.instrument instanceof Ym3438Inst ym3438) {
                    switch (setting.getNukedOPN2().emuType) {
                        case 0 -> ym3438.setChipType(Ym3438Const.Type.discrete);
                        case 1 -> ym3438.setChipType(Ym3438Const.Type.asic);
                        case 2 -> ym3438.setChipType(Ym3438Const.Type.ym2612);
                        case 3 -> ym3438.setChipType(Ym3438Const.Type.ym2612_u);
                        case 4 -> ym3438.setChipType(Ym3438Const.Type.asic_lp);
                    }
                }

                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym2612Chip.class);
                chip.clock = driverVirtual.vgm.ym2612ClockValue;

                hiyorimiDeviceFlag |= (setting.getYM2612Type()[0].getUseReal()[0]) ? 0x1 : 0x2;
                hiyorimiDeviceFlag |= (setting.getYM2612Type()[0].getUseReal()[0]
                        && setting.getYM2612Type()[0].getRealChipInfo()[0].getOnlyPCMEmulation()) ? 0x2 : 0x0;

                put(Ym2612Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.rf5C68ClockValue != 0) {

            for (int i = 0; i < (driverVirtual.vgm.rf5C68DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Rf5C68Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Rf5C68Chip.class);
                chip.clock = driverVirtual.vgm.rf5C68ClockValue;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(Rf5C68Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.rf5C164ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.rf5C164DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Rf5C164Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Rf5C164Chip.class);
                chip.clock = driverVirtual.vgm.rf5C164ClockValue;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(Rf5C164Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.pwmClockValue != 0) {
            MDSound.Chip chip = new MDSound.Chip();
            chip.id = 0;
            chip.instrument = chipRegister.chip(PwmChip.class).instrument(0);
            chip.samplingRate = setting.getOutputDevice().getSampleRate();
            chip.volume = setting.getBalance().getVolume(MAIN_TAG, PwmChip.class);
            chip.clock = driverVirtual.vgm.pwmClockValue;
            chip.option = null;

            hiyorimiDeviceFlag |= 0x2;

            put(PwmChip.class, chip);
        }

        if (driverVirtual.vgm.c140ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.c140DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(C140Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, C140Chip.class);
                chip.clock = driverVirtual.vgm.c140ClockValue;
                if (chip.instrument instanceof C140Inst) {
                    chip.option = new Object[] {driverVirtual.vgm.C140Type};
                }

                hiyorimiDeviceFlag |= 0x2;

                put(C140Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.multiPCMClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.multiPCMDualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(MultiPcmChip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, MultiPcmChip.class);
                chip.clock = driverVirtual.vgm.multiPCMClockValue;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(MultiPcmChip.class, chip);
            }
        }

        if (driverVirtual.vgm.uPD7759ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.uPD7759DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Upd7759Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Upd7759Chip.class);
                chip.clock = driverVirtual.vgm.uPD7759ClockValue;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(Upd7759Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.okiM6258ClockValue != 0) {
            MDSound.Chip chip = new MDSound.Chip();
            chip.id = 0;
            chip.instrument = chipRegister.chip(OkiM6258Chip.class).instrument(0);
            chip.samplingRate = setting.getOutputDevice().getSampleRate();
            chip.volume = setting.getBalance().getVolume(MAIN_TAG, OkiM6258Chip.class);
            chip.clock = driverVirtual.vgm.okiM6258ClockValue;
            BiConsumer<Integer, Integer> fn = chip::changeChipSampleRate;
            chip.option = new Object[] {
                    driverVirtual.vgm.okiM6258Type,
                    fn,
                    setting.getOutputDevice().getSampleRate()
            };

            hiyorimiDeviceFlag |= 0x2;

            put(OkiM6258Chip.class, chip);
        }

        if (driverVirtual.vgm.okiM6295ClockValue != 0) {
            for (byte i = 0; i < (driverVirtual.vgm.okiM6295DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(OkiM6295Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, OkiM6295Chip.class);
                chip.clock = driverVirtual.vgm.okiM6295ClockValue;
                BiConsumer<Integer, Integer> fn = chip::changeChipSampleRate;
                chip.option = new Object[] {
                        fn,
                        setting.getOutputDevice().getSampleRate()
                };

                hiyorimiDeviceFlag |= 0x2;

                put(OkiM6295Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.segaPCMClockValue != 0) {
            MDSound.Chip chip = new MDSound.Chip();
            chip.id = 0;
            chip.instrument = chipRegister.chip(SegaPcmChip.class).instrument(0);
            chip.samplingRate = setting.getOutputDevice().getSampleRate();
            chip.volume = setting.getBalance().getVolume(MAIN_TAG, SegaPcmChip.class);
            chip.clock = driverVirtual.vgm.segaPCMClockValue;
            chip.option = new Object[] {driverVirtual.vgm.segaPCMInterface};

            hiyorimiDeviceFlag |= 0x2;

            put(SegaPcmChip.class, chip);
        }

        if (driverVirtual.vgm.ym2608ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ym2608DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Ym2608Chip.class).instrument(i);
                if (chip.instrument instanceof Ym2608Inst ym2608) {
                    chip.setVolumes.put("FM", ym2608::setVolume);
                    chip.setVolumes.put("SSG", ym2608::setVolume);
                    chip.setVolumes.put("RHYTHM", ym2608::setVolume);
                    chip.setVolumes.put("ADPCM", ym2608::setVolume);
                }

                chip.samplingRate = 55467; // (int) setting.getoutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym2608Chip.class);
                chip.clock = driverVirtual.vgm.ym2608ClockValue;
                Function<String, InputStream> fn = chipRegister.chip(Ym2608Chip.class)::getOPNARyhthmStream;
                chip.option = new Object[] {fn};
                hiyorimiDeviceFlag |= 0x2;

                put(Ym2608Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.ym2151ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ym2151DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Ym2151Chip.class).instrument(i);
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym2151Chip.class);
                chip.clock = driverVirtual.vgm.ym2151ClockValue;
                chip.samplingRate = chip.clock / 64;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(Ym2151Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.ym2203ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ym2203DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Ym2203Chip.class).instrument(i);
                if (chip.instrument instanceof Ym2203Inst ym2203) {
                    chip.setVolumes.put("FM", ym2203::setVolume);
                    chip.setVolumes.put("SSG", ym2203::setVolume);
                }
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym2203Chip.class);
                chip.clock = driverVirtual.vgm.ym2203ClockValue;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(Ym2203Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.ym2610ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ym2610DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Ym2610Chip.class).instrument(i);
                if (chip.instrument instanceof Ym2610Inst ym2610) {
                    chip.setVolumes.put("FM", ym2610::setVolume);
                    chip.setVolumes.put("SSG", ym2610::setVolume);
                    chip.setVolumes.put("ADPCMA", ym2610::setVolume);
                    chip.setVolumes.put("ADPCMB", ym2610::setVolume);
                }

                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym2610Chip.class);
                chip.clock = driverVirtual.vgm.ym2610ClockValue & 0x7fff_ffff;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(Ym2610Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.ym3812ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ym3812DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Ym3812Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym3812Chip.class);
                chip.clock = driverVirtual.vgm.ym3812ClockValue & 0x7fff_ffff;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(Ym3812Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.ymF262ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ymF262DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(YmF262Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, YmF262Chip.class);
                chip.clock = driverVirtual.vgm.ymF262ClockValue & 0x7fff_ffff;
                chip.option = null;
                if (chip.instrument instanceof YmF262Inst) {
                    chip.option = new Object[] {chipRegister.chip(YmF262Chip.class).activeIndex(i)};
                }

                hiyorimiDeviceFlag |= 0x2;

                put(YmF262Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.ymF271ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ymF271DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(YmF271Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, YmF271Chip.class);
                chip.clock = driverVirtual.vgm.ymF271ClockValue & 0x7fff_ffff;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(YmF271Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.ymF278BClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ymF278BDualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(YmF278BChip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, YmF278BChip.class);
                chip.clock = driverVirtual.vgm.ymF278BClockValue & 0x7fff_ffff;
                chip.option = new Object[] {Common.getApplicationFolder()};

                hiyorimiDeviceFlag |= 0x2;

                put(YmF278BChip.class, chip);
            }
        }

        if (driverVirtual.vgm.ymZ280BClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ymZ280BDualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(YmZ280BChip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, YmZ280BChip.class);
                chip.clock = driverVirtual.vgm.ymZ280BClockValue & 0x7fff_ffff;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(YmZ280BChip.class, chip);
            }
        }

        if (driverVirtual.vgm.ay8910ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ay8910DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Ay8910Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ay8910Chip.class);
                chip.clock = (driverVirtual.vgm.ay8910ClockValue & 0x7fff_ffff) / 2;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(Ay8910Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.ym2413ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ym2413DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Ym2413Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym2413Chip.class);
                chip.clock = (driverVirtual.vgm.ym2413ClockValue & 0x7fff_ffff);
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(Ym2413Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.huC6280ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.huC6280DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(HuC6280Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, HuC6280Chip.class);
                chip.clock = (driverVirtual.vgm.huC6280ClockValue & 0x7fff_ffff);
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(HuC6280Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.qSoundClockValue != 0) {
            MDSound.Chip chip = new MDSound.Chip();
            chip.id = 0;
            chip.instrument = chipRegister.chip(QSoundChip.class).instrument(0);
            chip.samplingRate = setting.getOutputDevice().getSampleRate();
            chip.volume = setting.getBalance().getVolume(MAIN_TAG, QSoundChip.class);
            chip.clock = (driverVirtual.vgm.qSoundClockValue); // & 0x7fff_ffff);
            chip.option = null;

            hiyorimiDeviceFlag |= 0x2;

            put(QSoundChip.class, chip);
        }

        if (driverVirtual.vgm.saa1099ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.saA1099DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Saa1099Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Saa1099Chip.class);
                chip.clock = (driverVirtual.vgm.saa1099ClockValue & 0x3fff_ffff);
                hiyorimiDeviceFlag |= 0x2;

                put(Saa1099Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.wSwanClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.wSwanDualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(WSwanChip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, WSwanChip.class);
                chip.clock = (driverVirtual.vgm.wSwanClockValue & 0x3fff_ffff);
                hiyorimiDeviceFlag |= 0x2;

                put(WSwanChip.class, chip);
            }
        }

        if (driverVirtual.vgm.es5503ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.es5503DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Es5503Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Es5503Chip.class);
                chip.clock = (driverVirtual.vgm.es5503ClockValue & 0x3fff_ffff);
                Consumer<Integer> fn = sr -> chip.samplingRate = sr;
                chip.option = new Object[] {driverVirtual.vgm.es5503Ch, fn};
                hiyorimiDeviceFlag |= 0x2;

                put(Es5503Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.es5505ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.es5505DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Es5505Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Es5505Chip.class);
                chip.clock = driverVirtual.vgm.es5505ClockValue;
                chip.option = new Object[] {driverVirtual.vgm.es5505Ch};
                hiyorimiDeviceFlag |= 0x2;

                put(Es5505Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.msm5205ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.msm5205DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Msm5205Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Msm5205Chip.class);
                chip.clock = driverVirtual.vgm.msm5205ClockValue;
                BiConsumer<Integer, Integer> fn = chip::changeChipSampleRate;
                chip.option = new Object[] {
                        driverVirtual.vgm.msm5205Flags,
                        fn,
                        setting.getOutputDevice().getSampleRate()
                };
                hiyorimiDeviceFlag |= 0x2;

                put(Msm5205Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.pokeyClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.pokeyDualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(PokeyChip.class).instrument(i);
                chip.samplingRate = (driverVirtual.vgm.pokeyClockValue & 0x3fff_ffff); // (int) setting.getoutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, PokeyChip.class);
                chip.clock = (driverVirtual.vgm.pokeyClockValue & 0x3fff_ffff);
                hiyorimiDeviceFlag |= 0x2;

                put(PokeyChip.class, chip);
            }
        }

        if (driverVirtual.vgm.x1_010ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.x1_010DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(X1_010Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, X1_010Chip.class);
                chip.clock = (driverVirtual.vgm.x1_010ClockValue & 0x3fff_ffff);
                hiyorimiDeviceFlag |= 0x2;

                put(X1_010Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.c352ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.c352DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(C352Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, C352Chip.class);
                chip.clock = (driverVirtual.vgm.c352ClockValue & 0x7fff_ffff);
                if (chip.instrument instanceof C352Inst c352)
                    chip.setVolumes.put("Rear", c352::setRearMute);
                chip.option = new Object[] {(driverVirtual.vgm.c352ClockDivider)};
                int divider = (driverVirtual.vgm.c352ClockDivider) != 0 ? (driverVirtual.vgm.c352ClockDivider) : 288;
                C352.setOptions((driverVirtual.vgm.c352ClockValue >> 31));
                hiyorimiDeviceFlag |= 0x2;

                put(C352Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.ga20ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ga20DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Ga20Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ga20Chip.class);
                chip.clock = (driverVirtual.vgm.ga20ClockValue & 0x7fff_ffff);
                chip.option = null;
                hiyorimiDeviceFlag |= 0x2;

                put(Ga20Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.k053260ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.k053260DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(K053260Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, K053260Chip.class);
                chip.clock = driverVirtual.vgm.k053260ClockValue;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(K053260Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.k054539ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.k054539DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(K054539Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, K054539Chip.class);
                chip.clock = driverVirtual.vgm.k054539ClockValue;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(K054539Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.k051649ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.k051649DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(K051649Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, K051649Chip.class);
                chip.clock = driverVirtual.vgm.k051649ClockValue;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(K051649Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.ym3526ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.ym3526DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Ym3526Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym3526Chip.class);
                chip.clock = driverVirtual.vgm.ym3526ClockValue;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(Ym3526Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.y8950ClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.y8950DualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(Y8950Chip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, Y8950Chip.class);
                chip.clock = driverVirtual.vgm.y8950ClockValue;
                chip.option = null;
                if (chip.instrument instanceof MameAy8910Inst) {
                    chip.option = new Object[] {
                            0x00, // chip_type 0x10: YM2149, 0x00: AY // TODO setting
                            0x00  // chip_flag
                    };
                }

                hiyorimiDeviceFlag |= 0x2;

                put(Y8950Chip.class, chip);
            }
        }

        if (driverVirtual.vgm.dmgClockValue != 0) {
            for (int i = 0; i < (driverVirtual.vgm.dmgDualChipFlag ? 2 : 1); i++) {
                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(DmgChip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, DmgChip.class);
                chip.clock = driverVirtual.vgm.dmgClockValue;
                chip.option = null;

                hiyorimiDeviceFlag |= 0x2;

                put(DmgChip.class, chip);
            }
        }

        if (driverVirtual.vgm.nesClockValue != 0) {

            for (int i = 0; i < (driverVirtual.vgm.nesDualChipFlag ? 2 : 1); i++) {

                MDSound.Chip chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(NesChip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, NesChip.class);
                chip.clock = driverVirtual.vgm.nesClockValue;
                chip.option = null;

                put(NesChip.class, chip);

                chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(DmcChip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, DmcChip.class);
                chip.clock = driverVirtual.vgm.nesClockValue;
                chip.option = null;

                put(DmcChip.class, chip);

                chip = new MDSound.Chip();
                chip.id = i;
                chip.instrument = chipRegister.chip(FdsChip.class).instrument(i);
                chip.samplingRate = setting.getOutputDevice().getSampleRate();
                chip.volume = setting.getBalance().getVolume(MAIN_TAG, FdsChip.class);
                chip.clock = driverVirtual.vgm.nesClockValue;
                chip.option = null;

                put(FdsChip.class, chip);

                hiyorimiDeviceFlag |= 0x2;
            }
        }

        chipRegister.plugin(RealChipPlugin.class).initChip(hiyorimiDeviceFlag);

        mds.init(setting.getOutputDevice().getSampleRate(), BUFFER_SIZE, flatten());

        if (contains(Ym2203Chip.class, 0) || contains(Ym2203Chip.class, 1)) {
            chipRegister.chip(Ym2203Chip.class).write(0, 0x7, 0x3f, EnmModel.RealModel); // Output off
            chipRegister.chip(Ym2203Chip.class).write(1, 0x7, 0x3f, EnmModel.RealModel);
            chipRegister.chip(Ym2203Chip.class).write(0, 0x8, 0x0, EnmModel.RealModel);
            chipRegister.chip(Ym2203Chip.class).write(1, 0x8, 0x0, EnmModel.RealModel);
            chipRegister.chip(Ym2203Chip.class).write(0, 0x9, 0x0, EnmModel.RealModel);
            chipRegister.chip(Ym2203Chip.class).write(1, 0x9, 0x0, EnmModel.RealModel);
            chipRegister.chip(Ym2203Chip.class).write(0, 0xa, 0x0, EnmModel.RealModel);
            chipRegister.chip(Ym2203Chip.class).write(1, 0xa, 0x0, EnmModel.RealModel);
            setVolume("FM", Ym2203Chip.class, true, setting.getBalance().getVolume("FM", Ym2203Chip.class));
            setVolume("SSG", Ym2203Chip.class, true, setting.getBalance().getVolume("SSG", Ym2203Chip.class));
        }

        if (contains(Ym2608Chip.class, 0) || contains(Ym2608Chip.class, 1)) {
            setVolume("FM", Ym2608Chip.class, true, setting.getBalance().getVolume("FM", Ym2608Chip.class));
            setVolume("SSG", Ym2608Chip.class, true, setting.getBalance().getVolume("SSG", Ym2608Chip.class));
            setVolume("RHYTHM", Ym2608Chip.class, true, setting.getBalance().getVolume("RHYTHM", Ym2608Chip.class));
            setVolume("ADPCM", Ym2608Chip.class, true, setting.getBalance().getVolume("ADPCM", Ym2608Chip.class));
        }

        if (contains(Ym2610Chip.class, 0) || contains(Ym2610Chip.class, 1)) {
            setVolume("FM", Ym2610Chip.class, true, setting.getBalance().getVolume("FM", Ym2610Chip.class));
            setVolume("SSG", Ym2610Chip.class, true, setting.getBalance().getVolume("SSG", Ym2610Chip.class));
            setVolume("ADPCMA", Ym2610Chip.class, true, setting.getBalance().getVolume("ADPCMA", Ym2610Chip.class));
            setVolume("ADPCMB", Ym2610Chip.class, true, setting.getBalance().getVolume("ADPCMB", Ym2610Chip.class));
        }

        if (contains(Ay8910Chip.class, 0))
            chipRegister.chip(Ay8910Chip.class).writeClock((byte) 0, driverVirtual.vgm.ay8910ClockValue, EnmModel.RealModel);
        if (contains(Ay8910Chip.class, 1))
            chipRegister.chip(Ay8910Chip.class).writeClock((byte) 1, driverVirtual.vgm.ay8910ClockValue, EnmModel.RealModel);
        if (contains(Ym2151Chip.class, 0))
            chipRegister.chip(Ym2151Chip.class).writeClock((byte) 0, driverVirtual.vgm.ym2151ClockValue, EnmModel.RealModel);
        if (contains(Ym2151Chip.class, 1))
            chipRegister.chip(Ym2151Chip.class).writeClock((byte) 1, driverVirtual.vgm.ym2151ClockValue, EnmModel.RealModel);
        if (contains(Ym2203Chip.class, 0))
            chipRegister.chip(Ym2203Chip.class).writeClock((byte) 0, driverVirtual.vgm.ym2203ClockValue, EnmModel.RealModel);
        if (contains(Ym2203Chip.class, 1))
            chipRegister.chip(Ym2203Chip.class).writeClock((byte) 1, driverVirtual.vgm.ym2203ClockValue, EnmModel.RealModel);
        if (contains(Ym2608Chip.class, 0))
            chipRegister.chip(Ym2608Chip.class).writeClock((byte) 0, driverVirtual.vgm.ym2608ClockValue, EnmModel.RealModel);
        if (contains(Ym2608Chip.class, 1))
            chipRegister.chip(Ym2608Chip.class).writeClock((byte) 1, driverVirtual.vgm.ym2608ClockValue, EnmModel.RealModel);
        if (contains(Ym3526Chip.class, 0)) {
            chipRegister.chip(Ym3526Chip.class).write(0, 0xbd, 0, EnmModel.RealModel); // Rhythm mode off
            chipRegister.chip(Ym3526Chip.class).writeClock((byte) 0, driverVirtual.vgm.ym3526ClockValue, EnmModel.RealModel);
        }
        if (contains(Ym3526Chip.class, 1)) {
            chipRegister.chip(Ym3526Chip.class).write(1, 0xbd, 0, EnmModel.RealModel); // Rhythm mode off
            chipRegister.chip(Ym3526Chip.class).writeClock((byte) 1, driverVirtual.vgm.ym3526ClockValue, EnmModel.RealModel);
        }
        if (contains(Ym3812Chip.class, 0)) {
            chipRegister.chip(Ym3812Chip.class).write(0, 0xbd, 0, EnmModel.RealModel); // Rhythm mode off
            chipRegister.chip(Ym3812Chip.class).writeClock((byte) 0, driverVirtual.vgm.ym3812ClockValue, EnmModel.RealModel);
        }
        if (contains(Ym3812Chip.class, 1)) {
            chipRegister.chip(Ym3812Chip.class).write(1, 0xbd, 0, EnmModel.RealModel); // Rhythm mode off
            chipRegister.chip(Ym3812Chip.class).writeClock((byte) 1, driverVirtual.vgm.ym3812ClockValue, EnmModel.RealModel);
        }
        if (contains(YmF262Chip.class, 0)) {
            chipRegister.chip(YmF262Chip.class).write(0, 0, 0xbd, 0, EnmModel.RealModel); // Rhythm mode off
            chipRegister.chip(YmF262Chip.class).write(0, 1, 5, 1, EnmModel.RealModel); // opl3mode
            chipRegister.chip(YmF262Chip.class).writeClock((byte) 0, driverVirtual.vgm.ymF262ClockValue, EnmModel.RealModel);
        }
        if (contains(YmF262Chip.class, 1)) {
            chipRegister.chip(YmF262Chip.class).write(1, 0, 0xbd, 0, EnmModel.RealModel); // Rhythm mode off
            chipRegister.chip(YmF262Chip.class).write(1, 1, 5, 1, EnmModel.RealModel); // opl3mode
            chipRegister.chip(YmF262Chip.class).writeClock((byte) 1, driverVirtual.vgm.ymF262ClockValue, EnmModel.RealModel);
        }
        if (chipRegister.chip(Sn76489Chip.class).ngpFlag) {
            chipRegister.chip(Sn76489Chip.class).write(0, 0xe5, EnmModel.RealModel); // white noise mode
            chipRegister.chip(Sn76489Chip.class).write(1, 0xe5, EnmModel.RealModel); // white noise mode
            chipRegister.chip(Sn76489Chip.class).write(0, 0xe5, EnmModel.VirtualModel); // white noise mode
            chipRegister.chip(Sn76489Chip.class).write(1, 0xe5, EnmModel.VirtualModel); // white noise mode
        }
        if (contains(Ym2610Chip.class, 0)) {
            // control2 Presets register pan to center
            chipRegister.chip(Ym2610Chip.class).write(0, 0, 0x11, 0xc0, EnmModel.RealModel);
            chipRegister.chip(Ym2610Chip.class).write(0, 0, 0x11, 0xc0, EnmModel.VirtualModel);
        }
        if (contains(Ym2610Chip.class, 1)) {
            // control2 Presets register pan to center
            chipRegister.chip(Ym2610Chip.class).write(1, 0, 0x11, 0xc0, EnmModel.RealModel);
            chipRegister.chip(Ym2610Chip.class).write(1, 0, 0x11, 0xc0, EnmModel.VirtualModel);
        }
        if (contains(C140Chip.class, 0))
            chipRegister.chip(C140Chip.class).writeType((byte) 0, driverVirtual.vgm.C140Type, EnmModel.RealModel);
        if (contains(SegaPcmChip.class, 0))
            chipRegister.chip(SegaPcmChip.class).writeClock((byte) 0, driverVirtual.vgm.segaPCMClockValue, EnmModel.RealModel);

        int SSGVolumeFromTAG = -1;
        if (driverReal != null) {
            if (driverReal.metaData.getFirst(Tag.GameSystemJ).contains("9801")) SSGVolumeFromTAG = 31;
            if (driverReal.metaData.getFirst(Tag.GameSystemJ).contains("8801")) SSGVolumeFromTAG = 63;
            if (driverReal.metaData.getFirst(Tag.GameSystemJ).contains("pc-88")) SSGVolumeFromTAG = 63;
            if (driverReal.metaData.getFirst(Tag.GameSystemJ).contains("PC88")) SSGVolumeFromTAG = 63;
            if (driverReal.metaData.getFirst(Tag.GameSystemJ).contains("pc-98")) SSGVolumeFromTAG = 31;
            if (driverReal.metaData.getFirst(Tag.GameSystemJ).contains("PC98")) SSGVolumeFromTAG = 31;
            if (driverReal.metaData.getFirst(Tag.GameSystem).contains("9801")) SSGVolumeFromTAG = 31;
            if (driverReal.metaData.getFirst(Tag.GameSystem).contains("8801")) SSGVolumeFromTAG = 63;
            if (driverReal.metaData.getFirst(Tag.GameSystem).contains("pc-88")) SSGVolumeFromTAG = 63;
            if (driverReal.metaData.getFirst(Tag.GameSystem).contains("PC88")) SSGVolumeFromTAG = 63;
            if (driverReal.metaData.getFirst(Tag.GameSystem).contains("pc-98")) SSGVolumeFromTAG = 31;
            if (driverReal.metaData.getFirst(Tag.GameSystem).contains("PC98")) SSGVolumeFromTAG = 31;
        }

        if (SSGVolumeFromTAG == -1) {
            if (contains(Ym2203Chip.class, 0))
                chipRegister.chip(Ym2203Chip.class).setSsgVolume((byte) 0, setting.getBalance().getGimicOPNVolume(), EnmModel.RealModel);
            if (contains(Ym2203Chip.class, 1))
                chipRegister.chip(Ym2203Chip.class).setSsgVolume((byte) 1, setting.getBalance().getGimicOPNVolume(), EnmModel.RealModel);
            if (contains(Ym2608Chip.class, 0))
                chipRegister.chip(Ym2608Chip.class).setSsgVolume((byte) 0, setting.getBalance().getGimicOPNAVolume(), EnmModel.RealModel);
            if (contains(Ym2608Chip.class, 1))
                chipRegister.chip(Ym2608Chip.class).setSsgVolume((byte) 1, setting.getBalance().getGimicOPNAVolume(), EnmModel.RealModel);
        } else {
            if (contains(Ym2203Chip.class, 0))
                chipRegister.chip(Ym2203Chip.class).setSsgVolume((byte) 0, SSGVolumeFromTAG, EnmModel.RealModel);
            if (contains(Ym2203Chip.class, 1))
                chipRegister.chip(Ym2203Chip.class).setSsgVolume((byte) 1, SSGVolumeFromTAG, EnmModel.RealModel);
            if (contains(Ym2608Chip.class, 0))
                chipRegister.chip(Ym2608Chip.class).setSsgVolume((byte) 0, SSGVolumeFromTAG, EnmModel.RealModel);
            if (contains(Ym2608Chip.class, 1))
                chipRegister.chip(Ym2608Chip.class).setSsgVolume((byte) 1, SSGVolumeFromTAG, EnmModel.RealModel);
        }

        chipRegister.chip(Ym2151Chip.class).setCorrection(EnmModel.VirtualModel, driverVirtual.vgm.ym2151ClockValue);
        if (driverReal != null) chipRegister.chip(Ym2151Chip.class).setCorrection(EnmModel.RealModel, driverReal.vgm.ym2151ClockValue);
    }
}
