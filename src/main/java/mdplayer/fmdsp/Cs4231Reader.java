/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.Cs4231Chip;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * The CS4231 codec of an X68000 Mercury unit, which MUAP plays its PCM through: one row, and what
 * is on it is the sound itself.
 * <p>
 * There is nothing else to read. A codec has no channels, no notes and no level registers - the
 * driver hands it a stream and it plays it - so where every other reader here decodes registers,
 * this one meters the output the emulator reports and leaves the key and the note length alone.
 * A song played entirely this way (MUAP's {@code INIT.O} is one) lit nothing at all before, and a
 * display with every row dark says the music has stopped.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-31 nsano initial version <br>
 */
public class Cs4231Reader extends PcmSlotReader {

    /** the one row: the codec plays one stream, whatever the driver mixed into it */
    private static final int CHANNELS = 1;

    @Override
    public String chipName() {
        return "CS42";
    }

    @Override
    public int priority() {
        return 47;
    }

    @Override
    protected int channelCount() {
        return CHANNELS;
    }

    @Override
    protected BaseChip chip() {
        return chipRegister.chip(Cs4231Chip.class);
    }

    /**
     * Whether anything is coming out. A codec that is fed silence is a codec that is not playing,
     * and there is no key register to ask instead - so the row lights while the meter reads
     * anything at all, which is the peak follower's business rather than one sample's.
     */
    @Override
    protected boolean sounding(int ch) {
        return outputLevel() > 0;
    }

    /** no rate register: the stream plays at whatever the driver set the codec to */
    @Override
    protected int rateOf(int ch) {
        return 0;
    }

    @Override
    protected void readChannel(int ch, FmDspChannel out) {
        out.name = "PCM";
        double level = outputLevel();
        out.amplitude = Math.max(level, 0);
        // the number is the sound, not a register asking for it, so it is already an envelope
        out.measured = true;
        out.volume = (int) Math.round(out.amplitude * 255);
        // a stream from the first sample, not once it has run long enough to look like one: there
        // is no note here to wait for, and a row that says so is not a row that looks broken
        out.info = TrackInfo.STREAM;
    }
}
