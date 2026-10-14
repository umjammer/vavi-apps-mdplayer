/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

import mdplayer.Common;
import mdplayer.chips.BaseChip;
import mdplayer.chips.ZxBeepChip;
import mdplayer.driver.BaseDriver;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * The ZX Spectrum's beeper, on one SSG row: the one voice here that is watched rather than read.
 * <p>
 * Every other chip is asked what it is playing. This one cannot be - a beeper is a single bit the
 * CPU flips, with no note register, no level and no key - and a {@code .ay} whose music is all
 * beeper (ProjectAY's demo tunes are) left every row, meter and bar of the display empty while it
 * played. What can be seen is that the speaker is moving, and that is what the row says.
 * <p>
 * Not what note it is playing, though the flips are counted and it is tempting: the beeper engines
 * that play two and three parts at once do it by interleaving short pulses, and their speaker runs
 * at twenty kilohertz whatever the tune - {@code Beeper Demo part 3} measures 19.5 kHz through a
 * melody in the middle of the keyboard. Half the flips per second is the pitch of a plain square
 * wave and of nothing else, so the row is marked {@link TrackInfo#STREAM} and the keyboard is left
 * alone. Pulling the parts back out of a one bit stream is the driver's knowledge, not the
 * speaker's.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-31 nsano initial version <br>
 */
public class ZxBeepReader extends ChipReader {

    /**
     * How long the flips are counted over before the row is judged again [s]. A poll is a
     * hundredth of a second, and a rest that falls across two of them would flicker the row.
     */
    private static final double WINDOW = 1 / 30.0;

    /** how loud a bit is: it is on or it is off, so the meter says the same throughout */
    private static final double LEVEL = 0.75;

    private Supplier<BaseDriver> driver;

    /** where the open window started: the flip count and the driver's sample counter */
    private long windowFlips;
    private long windowCounter;

    /** whether the last closed window had the speaker moving at all, and whether one ever has */
    private boolean sounding;
    private boolean wasSounding;
    private boolean active;

    @Override
    protected ZxBeepChip chip() {
        return chipRegister == null ? null : chipRegister.chip(ZxBeepChip.class);
    }

    @Override
    public String chipName() {
        return "BEEP";
    }

    @Override
    public void bind(Supplier<BaseDriver> driver) {
        this.driver = driver;
    }

    @Override
    public void reset() {
        windowFlips = chip() != null ? chip().flips[0] : 0;
        windowCounter = counter();
        sounding = false;
        wasSounding = false;
        active = false;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.SSG);
    }

    /** behind the PSG, which takes the SSG rows when a song plays both */
    @Override
    public int priority() {
        return 86;
    }

    /** closes the window when enough of the song has gone past it: was the speaker moving in it */
    @Override
    public void poll() {
        ZxBeepChip chip = chip();
        if (chip == null) return;
        long flips = chip.flips[0];
        long counter = counter();
        if ((counter - windowCounter) / (double) Common.VGMProcSampleRate < WINDOW) return;

        sounding = flips > windowFlips;
        windowFlips = flips;
        windowCounter = counter;
        active |= sounding;
    }

    @Override
    public boolean active(Group group) {
        return active;
    }

    @Override
    public int channels(Group group) {
        return 1;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        out.name = "BEEP";
        out.num = 1;
        out.sounding = sounding;
        // the speaker starting to move again is the only thing here that reads as a key on
        out.keyOn = sounding && !wasSounding;
        wasSounding = sounding;
        out.volume = sounding ? 1 : 0; // one bit, and it is on
        out.amplitude = sounding ? LEVEL : 0;
        out.pan = Pan.CENTER;
        // no note, and the row says so rather than sitting there looking like a silent part
        out.info = TrackInfo.STREAM;
    }

    /** the driver's own sample counter, which is the only clock a reader has */
    private long counter() {
        BaseDriver d = driver != null ? driver.get() : null;
        return d != null ? d.counter : 0;
    }

    /** the chip cannot mute a channel */
    @Override
    public BaseChip.Channel muteChannel(Group group, int ch) {
        return null;
    }
}
