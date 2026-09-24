package mdplayer.driver.rcp;

import java.lang.System.Logger;

import mdplayer.Common;
import mdplayer.chips.MidiPlugin;
import mdplayer.chips.Pcm8Chip;
import mdplayer.driver.BasePlugin;
import mdsound.MDSound;
import mdsound.instrument.Pcm8PPInst;
import mdsound.instrument.X68kYm2151Inst;
import mdsound.x68sound.SoundIocs;

import static java.lang.System.getLogger;
import static mdsound.MDSound.Chip.MAIN_TAG;


/**
 * RCSPlugin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-07-12 nsano initial version <br>
 */
public class RCSPlugin extends BasePlugin<RcsDriver> {

    private static final Logger logger = getLogger(RCSPlugin.class.getName());

    @Override
    public void prepare() {
        driverVirtual = new RcsDriver(this);
        driverVirtual.setExtendFile(extendFiles);

        driverReal = null;
        if (setting.getOutputDevice().getDeviceType() != Common.DEV_Null) {
            driverReal = new RcsDriver(this);
            driverReal.setExtendFile(extendFiles);
        }

        super.prepare();
        initChips();
    }

    private final String[] supportFile = null; // TODO

    @Override
    protected void initChips() {
        // The .RCS half of the song is a PCM8 bank played on the X68000's PCM8, driven straight from
        // the driver rather than through register writes; which of the two back ends is live follows
        // `setting.pcm8Type`, the same shared Pcm8Chip that ZMS and MDX register.
        int pcm8type = setting.pcm8Type(this);
        MDSound.Chip chip = new MDSound.Chip();
        chip.id = 0;
        chip.instrument = chipRegister.chip(Pcm8Chip.class).instrument(0);
        chip.volume = setting.getBalance().getVolume(MAIN_TAG, Pcm8Chip.class);
        chip.clock = 4_000_000;
        X68kYm2151Inst opmPCM = null;
        Pcm8PPInst pcm8pp = null;
        if (chip.instrument instanceof X68kYm2151Inst inst) {
            opmPCM = inst;
            opmPCM.soundIocs[0] = new SoundIocs(opmPCM.chips[0]);
            chip.samplingRate = 4_000_000 / 64;
            chip.option = new Object[] {0, 1, 0};
        } else {
            pcm8pp = (Pcm8PPInst) chip.instrument;
            chip.samplingRate = setting.getOutputDevice().getSampleRate();
            chip.option = new Object[] {setting.pcm8ppsOption(this)};
        }
        put(Pcm8Chip.class, chip);

        mds.init(setting.getOutputDevice().getSampleRate(), BUFFER_SIZE, flatten());

        getDriver().fireEventHappened(chipRegister.plugin(MidiPlugin.class), "led.set", 0);
        getDriver().fireEventHappened(chipRegister.plugin(MidiPlugin.class), "led.set", 1);

        chipRegister.plugin(MidiPlugin.class).releaseAll();
        chipRegister.plugin(MidiPlugin.class).make();
        chipRegister.plugin(MidiPlugin.class).setOutInfos();

        String support = (supportFile == null || supportFile.length < 1) ? null : supportFile[0];
        driverVirtual.setSupportFileName(support);
        // only the virtual model renders the PCM; the real one just forwards MIDI
        driverVirtual.setPcm8(pcm8type, opmPCM, pcm8pp);
        if (driverReal != null) {
            driverReal.setSupportFileName(support);
        }

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
