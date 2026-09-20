/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.smaf;

import mdplayer.Common;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;


/**
 * SMAF (".mmf") Plugin.
 * <p>
 * The driver is {@link SmafDriver2}, the MA-7 in pure java. {@link SmafDriver} - mmftoolc.exe on an
 * emulated PC - is the one this used to build and is still here: the lines that build it are kept
 * commented out below, and the fmdsp reader that goes with it is kept the same way in
 * {@code META-INF/services/mdplayer.fmdsp.FmDspChipReader}. See the readme.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-08-10 nsano initial version <br>
 *          0.01 2026-09-21 nsano SmafDriver2 <br>
 */
public class SmafPlugin extends BasePlugin<SmafDriver2> {

    /**
     * Which of the two this builds, for whoever wants to know without playing a song - the tests
     * of the driver that is not in use skip themselves against it. It moves with the type
     * parameter above and the line in {@link #prepare}, which is the whole of the switch.
     */
    public static final Class<? extends BaseDriver> DRIVER = SmafDriver2.class;

    @Override
    public void prepare() {
        driverVirtual = new SmafDriver2(this);
//        driverVirtual = new SmafDriver(this); // the emulated PC, see the class comment

        driverReal = null;

        super.prepare();
        initChips();
    }

    @Override
    protected void initChips() {
        // the synthesizer renders the audio itself, so no mdsound chip is registered

        driverVirtual.init(Common.EnmModel.VirtualModel,
                setting.getOutputDevice().getSampleRate() * setting.getLatencyEmulation() / 1000,
                setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000);
    }

    /** the synthesizer is the song's */
    @Override
    public void stop() {
        if (driverVirtual != null) {
            driverVirtual.stopSynth();
//            driverVirtual.stopPlayer(); // SmafDriver: the emulated machine is a JVM-wide singleton
        }
        super.stop();
    }
}
