/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mfi;

import mdplayer.Common;
import mdplayer.driver.BasePlugin;


/**
 * MFi (".mld") Plugin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
public class MldPlugin extends BasePlugin<MldDriver> {

    @Override
    public void prepare() {
        driverVirtual = new MldDriver(this);

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
        }
        super.stop();
    }
}
