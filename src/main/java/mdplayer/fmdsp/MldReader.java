/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.driver.MidiChannels;
import mdplayer.driver.mfi.MldDriver;
import vavi.sound.mfi.MfiChip;


/**
 * MFi (".mld"), which is played by a synthesizer of vavi-apps-mfiplayer and emulates no chip here.
 * <p>
 * Everything the rows are made of is {@link MidiChannelsReader}'s; what is this reader's own is the
 * chip the song was made for: the rows are FM for a Yamaha song, PCM for a FueTrek or a Rohm one.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 *          0.01 2026-09-21 nsano the rows moved to MidiChannelsReader <br>
 */
public class MldReader extends MidiChannelsReader {

    private MldDriver mld() {
        return driver != null && driver.get() instanceof MldDriver d ? d : null;
    }

    @Override
    protected MidiChannels channels() {
        MldDriver mld = mld();
        return mld == null ? null : mld.getChannels();
    }

    private MfiChip chip() {
        MldDriver mld = mld();
        return mld == null || mld.getDetection() == null ? null : mld.getDetection().chip();
    }

    @Override
    public String chipName() {
        MldDriver mld = mld();
        return mld == null || mld.getDetection() == null ? "MFI" : mld.getDetection().name();
    }

    /** the rows of the chip: the fm ones for Yamaha, the pcm ones for the others */
    @Override
    protected Group group() {
        return chip() == MfiChip.YAMAHA ? Group.FM : Group.PCM;
    }

    @Override
    public int priority() {
        return 45;
    }

    @Override
    public boolean ready() {
        return super.ready() && chip() != null;
    }
}
