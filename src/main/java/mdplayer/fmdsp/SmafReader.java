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
import mdplayer.driver.smaf.SmafDriver;
import mdplayer.lib.smaf.SmafScore;
import mdplayer.lib.smaf.SmafTelemetry;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackDetail;


/**
 * SMAF, which is played by a real MA-2/3/5 player on an emulated PC and emulates no chip here.
 * <p>
 * There is nothing to read back and, unlike the MIDI drivers, nothing even goes past on its way
 * out: the Yamaha dll inside the emulated PC is handed the file and sequences it itself. So what
 * is shown is the score the player wrote out before it started - see {@link SmafTelemetry} - read
 * against the audio it has since produced. A SMAF note is a MIDI note with the length it is held
 * for written next to it, so these keys are exact and their rows let go on their own.
 * <p>
 * Sixteen channels do not fit nine rows, so they are taken in the order they first sound, the way
 * {@link MidiReader} does. The analyzer bars are not this reader's business: SMAF audio is
 * rendered through mdplayer's own mixer, so they are measured off the sound itself.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-08-11 nsano initial version <br>
 */
public class SmafReader implements FmDspChipReader {

    private static final int CHANNELS = SmafScore.CHANNELS;

    /** how far a pitch bend goes at its extreme, in semitones - the usual two */
    private static final double BEND_SEMITONES = 2;

    private Supplier<BaseDriver> driver;

    /** SMAF channel shown on each row, -1 = none yet */
    private final int[] slotChannels = new int[CHANNELS];

    private int mappedChannels;

    private boolean active;

    @Override
    public String chipName() {
        return "SMAF";
    }

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
        mappedChannels = 0;
        active = false;
    }

    private SmafDriver smaf() {
        return driver != null && driver.get() instanceof SmafDriver d ? d : null;
    }

    private SmafScore score() {
        SmafDriver smaf = smaf();
        if (smaf == null || smaf.getPlayer() == null) {
            return null;
        }
        SmafTelemetry telemetry = smaf.getPlayer().getTelemetry();
        return telemetry.isSynchronized() ? telemetry.getScore() : null;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 44;
    }

    @Override
    public boolean ready() {
        return score() != null;
    }

    /**
     * Walks the score up to where the audio being handed over has got to, which is the one thing
     * this does per snapshot - everything below reads what it left.
     */
    @Override
    public void poll() {
        SmafScore score = score();
        SmafDriver smaf = smaf();
        if (score == null || smaf == null) {
            return;
        }
        double millis = smaf.getSongMillis();
        if (Double.isNaN(millis)) {
            return;
        }
        // the edges of the previous snapshot have been read by now; a strike from this one has
        // to survive until the rows are filled, so they are cleared here rather than after
        score.clearEdges();
        score.seek(millis);

        for (int ch = 0; ch < CHANNELS && mappedChannels < slotChannels.length; ch++) {
            if (score.channel(ch).sounding() && slotOf(ch) < 0) {
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
        return active;
    }

    @Override
    public int channels(Group group) {
        return CHANNELS;
    }

    @Override
    public void read(Group group, int slot, FmDspChannel out) {
        SmafScore score = score();
        int ch = slotChannels[slot];
        if (score == null || ch < 0) {
            return;
        }
        SmafScore.Channel channel = score.channel(ch);
        SmafScore.Note note = channel.top();

        out.num = ch + 1; // the SMAF channel, wherever the slot map put it
        out.sounding = note != null;
        out.keyOn = channel.keyOn;
        out.toneNum = channel.program;
        out.volume = note != null ? note.velocity() : 0;
        out.pan = panOf(channel.pan);
        out.lfoPitch = channel.modulation > 0;

        if (note == null) {
            out.note = -1;
            out.amplitude = 0;
            return;
        }

        // a SMAF note is a note already, and the display counts from the same C; what the wheel
        // adds to it is a note further up and the rest of the way in cents
        double cents = (channel.bend - 0x2000) / 8192.0 * BEND_SEMITONES * 100;
        int semitones = (int) Math.round(cents / 100);
        out.note = note.key() + semitones;
        out.detune = (int) Math.round(Math.clamp(cents - semitones * 100.0, -50, 50));
        out.amplitude = amplitude(channel, note);
    }

    /**
     * How loud the channel is playing, {@code 0..1}: the velocity the note was struck at, through
     * the volume the part sits at and the expression it is being swelled or faded with. What the
     * dll made of that is not reported back, so this is as close as the display gets.
     */
    private static double amplitude(SmafScore.Channel channel, SmafScore.Note note) {
        return note.velocity() * channel.volume * channel.expression / (127.0 * 127 * 127);
    }

    /** controller 10: 0 hard left, 64 centre, 127 hard right */
    private static Pan panOf(int pan) {
        if (pan < 16) return Pan.LEFT;
        if (pan < 56) return Pan.MID_LEFT;
        if (pan <= 72) return Pan.CENTER;
        if (pan <= 112) return Pan.MID_RIGHT;
        return Pan.RIGHT;
    }

    /**
     * The right half, which for a part that is only ever described - never read back - is what
     * the file last said about it.
     */
    @Override
    public boolean readDetail(Group group, int slot, TrackDetail out) {
        SmafScore score = score();
        int ch = slot < slotChannels.length ? slotChannels[slot] : -1;
        if (score == null || ch < 0) {
            return false;
        }
        SmafScore.Channel channel = score.channel(ch);
        SmafScore.Note note = channel.top();

        out.header = " CH TONE BANK  VOL  EXP  PAN  MOD   BEND  GATE";
        out.text = " %2d  %3d  %3d  %3d  %3d  %3d  %3d  %+5d %5s".formatted(
                ch + 1, channel.program, channel.bankMsb, channel.volume, channel.expression,
                channel.pan, channel.modulation, channel.bend - 0x2000,
                note == null ? "--" : String.valueOf(note.gateMs()));
        return true;
    }
}
