/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.driver.MidiChannels;
import mdplayer.driver.smaf.SmafDriver2;


/**
 * SMAF (".mmf") as {@link SmafDriver2} plays it, on the yamaha MA-7 of vavi-apps-mfiplayer, which
 * emulates no chip here.
 * <p>
 * The sound source reports nothing of what it is sounding, but every message of the song goes past
 * the driver on its way to it, so what is shown is what it sent - see {@link MidiChannelsReader}.
 * The MA-7's voices are fm and wave table, so the rows are the fm ones.
 * <p>
 * {@link SmafReader}, which shows the score mmftoolc.exe writes out for {@code SmafDriver}, is
 * still registered beside this one and simply never becomes {@linkplain #ready ready} while this
 * driver plays - going back to the old driver needs nothing done here.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-21 nsano initial version <br>
 */
public class Smaf2Reader extends MidiChannelsReader {

    private SmafDriver2 smaf() {
        return driver != null && driver.get() instanceof SmafDriver2 d ? d : null;
    }

    @Override
    protected MidiChannels channels() {
        SmafDriver2 smaf = smaf();
        return smaf == null || smaf.getSynthesizer() == null ? null : smaf.getChannels();
    }

    @Override
    public String chipName() {
        return "MA-7";
    }

    @Override
    protected Group group() {
        return Group.FM;
    }

    /** beside {@link SmafReader}, which only ever answers for the other driver */
    @Override
    public int priority() {
        return 44;
    }
}
