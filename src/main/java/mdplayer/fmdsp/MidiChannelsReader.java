/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

import mdplayer.ChipRegister;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.MidiChannels;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackDetail;


/**
 * The rows of a driver that plays a converted midi sequence on a sound source of its own and
 * emulates no chip here - the mfi one ({@link MldReader}) and the smaf one ({@link Smaf2Reader}).
 * <p>
 * None of those sound sources reports what it is sounding, but every message of the song goes past
 * its driver on the way to one, so what is shown is what it sent - see {@link MidiChannels}. A midi
 * note is already a note, so these keys are exact.
 * <p>
 * Sixteen channels do not fit nine rows, so they are taken in the order they first sound, the way
 * {@link MidiReader} does. The analyzer bars are not this reader's business: the song is rendered
 * through mdplayer's own mixer, so they are measured off the sound itself.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-21 nsano extracted from MldReader <br>
 */
public abstract class MidiChannelsReader implements FmDspChipReader {

    private static final int CHANNELS = MidiChannels.CHANNELS;

    /** how far a pitch bend goes at its extreme, in semitones - the usual two */
    private static final double BEND_SEMITONES = 2;

    /** the driver of the song about to play, see {@link #bind(Supplier)} */
    protected Supplier<BaseDriver> driver;

    /** channel shown on each row, -1 = none yet */
    private final int[] slotChannels = new int[CHANNELS];

    private int mappedChannels;

    /** note ons of a channel the previous snapshot saw, a strike is spotted against it */
    private final int[] prevStrikes = new int[CHANNELS];

    private boolean active;

    /** what the song has left the channels at, null while no song of this reader's kind plays */
    protected abstract MidiChannels channels();

    /** the row group the song's sound source is shown on */
    protected abstract Group group();

    @Override
    public void bind(ChipRegister chipRegister) {
    }

    @Override
    public void bind(Supplier<BaseDriver> driver) {
        this.driver = driver;
    }

    @Override
    public void reset() {
        Arrays.fill(slotChannels, -1);
        Arrays.fill(prevStrikes, 0);
        mappedChannels = 0;
        active = false;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(group());
    }

    @Override
    public boolean ready() {
        return channels() != null;
    }

    @Override
    public void poll() {
        MidiChannels channels = channels();
        if (channels == null) return;
        for (int ch = 0; ch < CHANNELS && mappedChannels < slotChannels.length; ch++) {
            // a strike counts even when the key was let go before this snapshot: a drum hit is
            // over in a few milliseconds, sooner than the display looks
            if ((channels.sounding(ch) || channels.strikes(ch) != 0) && slotOf(ch) < 0) {
                slotChannels[mappedChannels++] = ch;
            }
        }
    }

    private int slotOf(int ch) {
        for (int s = 0; s < mappedChannels; s++) {
            if (slotChannels[s] == ch) return s;
        }
        return -1;
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            active = mappedChannels > 0;
        }
        return active && group == group();
    }

    @Override
    public int channels(Group group) {
        return CHANNELS;
    }

    @Override
    public void read(Group group, int slot, FmDspChannel out) {
        MidiChannels channels = channels();
        int ch = slotChannels[slot];
        if (channels == null || ch < 0) {
            return;
        }

        int strikes = channels.strikes(ch);
        boolean struck = strikes != prevStrikes[ch];
        prevStrikes[ch] = strikes;
        // a key struck and let go between two snapshots is still shown struck, once
        boolean sounding = channels.sounding(ch) || struck;

        // the pcm rows are titled ADPCM and PPZ8 after the PC-98 by default, which these are not
        if (group == Group.PCM) out.name = "PCM";
        out.num = ch + 1; // the midi channel, wherever the slot map put it
        out.pcmCh = ch + 1;
        out.sounding = sounding;
        out.keyOn = struck;
        out.toneNum = channels.program(ch);
        out.volume = channels.velocity(ch);
        out.pan = panOf(channels.pan(ch));
        out.lfoPitch = channels.modulation(ch) > 0;

        if (!sounding) {
            out.note = -1;
            out.amplitude = 0;
            return;
        }

        // a midi note is a note already, and the display counts from the same C; what the wheel
        // adds to it is a note further up and the rest of the way in cents
        double cents = (channels.bend(ch) - 0x2000) / 8192.0 * BEND_SEMITONES * 100;
        int semitones = (int) Math.round(cents / 100);
        out.note = channels.note(ch) + semitones;
        out.detune = (int) Math.round(Math.max(-50, Math.min(50, cents - semitones * 100.0)));
        out.amplitude = channels.velocity(ch) * channels.volume(ch) * channels.expression(ch) / (127.0 * 127 * 127);
    }

    /** controller 10: 0 hard left, 64 centre, 127 hard right */
    private static Pan panOf(int pan) {
        if (pan < 16) return Pan.LEFT;
        if (pan < 56) return Pan.MID_LEFT;
        if (pan <= 72) return Pan.CENTER;
        if (pan <= 112) return Pan.MID_RIGHT;
        return Pan.RIGHT;
    }

    /** the right half: what the song last said about the channel */
    @Override
    public boolean readDetail(Group group, int slot, TrackDetail out) {
        MidiChannels channels = channels();
        int ch = slot < slotChannels.length ? slotChannels[slot] : -1;
        if (channels == null || ch < 0) {
            return false;
        }
        out.header = " CH TONE BANK  VOL  EXP  PAN  MOD   BEND";
        out.text = " %2d  %3d  %3d  %3d  %3d  %3d  %3d  %+5d".formatted(
                ch + 1, channels.program(ch), channels.bank(ch), channels.volume(ch), channels.expression(ch),
                channels.pan(ch), channels.modulation(ch), channels.bend(ch) - 0x2000);
        return true;
    }
}
