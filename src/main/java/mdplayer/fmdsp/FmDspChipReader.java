/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Set;
import java.util.function.Supplier;

import mdplayer.ChipRegister;
import mdplayer.chips.BaseChip;
import mdplayer.driver.BaseDriver;
import vavi.sound.visualizer.fmdsp.FftDataSource;
import vavi.sound.visualizer.fmdsp.TrackDetail;


/**
 * Maps one chip's register caches onto the fmdsp view, discovered by
 * {@code mdplayer.ChipFmDspSource} through {@link java.util.ServiceLoader}. Adding a chip to the
 * visualizer is one implementation of this plus a line in
 * {@code META-INF/services/mdplayer.fmdsp.FmDspChipReader} - the source itself names no chip.
 * <p>
 * A reader feeds one or more row groups. The source assigns the rows of a group to the readers
 * that report {@linkplain #active} in the order they become active (ties broken by
 * {@link #priority}), so whichever chips a song actually plays claim the screen. Row groups:
 * <ul>
 * <li>{@link Group#FM}: 9 rows (FM 1-6 and the three FM3 extension rows)</li>
 * <li>{@link Group#SSG}: 3 rows</li>
 * <li>{@link Group#PCM}: 9 rows (the ADPCM row and PPZ8 1-8)</li>
 * <li>{@link Group#RHYTHM}: no row, only the drum level meter</li>
 * </ul>
 * Per snapshot the source calls {@link #poll} once, then {@link #active}, then {@link #read} once
 * per channel - readers may keep edge state (previous key-on values) between snapshots and clear
 * it in {@link #reset}. Instances are {@link java.util.ServiceLoader} singletons shared across
 * songs, like the chips themselves; the source adds one more of each reader that accepts
 * {@link #chipId(int)}, for the VGMs that declare two of one chip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public interface FmDspChipReader {

    /** fmdsp row group */
    enum Group { FM, SSG, PCM, RHYTHM }

    /**
     * The chip's own name, shown once over its meter span (e.g. {@code "OPL3"}, {@code "DCSG"}).
     * The track rows are titled by section - FM, SSG, PCM - instead, the way the original does,
     * so a nine channel chip does not repeat its name down the whole strip.
     */
    default String chipName() {
        return null;
    }

    /**
     * The chip class this reader shows, which is what the mixer keys its volumes by - see
     * {@code ChipFmDspSource#volumeDown}. Null - the default - from a reader whose state lives on
     * a driver rather than on a chip; {@link ChipReader} answers for its subclasses.
     */
    default Class<? extends mdplayer.Chip> chipClass() {
        return null;
    }

    /** points this reader at the playing plugin's chips */
    void bind(ChipRegister chipRegister);

    /** which of its chip's instances this reader shows; 0 unless the source made it a second one */
    default int chipId() {
        return 0;
    }

    /**
     * Points this reader at instance {@code chipId} of its chip, for the VGMs that declare two of
     * one - and answers whether it can. A reader whose state lives on a driver rather than on a
     * chip cannot, and that chip is shown once.
     * <p>
     * The source calls this on a spare copy of every reader as it is built, so a reader that says
     * yes must read its chip at {@link #chipId} throughout, and report itself
     * {@linkplain #ready not ready} while the song holds no such instance. {@link ChipReader} does
     * both for its subclasses.
     */
    default boolean chipId(int chipId) {
        return false;
    }

    /**
     * Hands over the driver of the song about to play, for the formats that emulate nothing.
     * <p>
     * Most chips are reached through {@link ChipRegister}, but a few drivers - HVL, AHX, SID, YM,
     * RCP - render their own audio and register no chip at all, so their state only exists on the
     * driver. Readers for those take it here; the rest need not care.
     */
    default void bind(Supplier<BaseDriver> driver) {
    }

    /** clears per-song state (edge caches, activity); call before every song */
    void reset();

    /** row groups this reader can feed */
    Set<Group> groups();

    /** row allocation tie-breaker, smaller wins when two readers become active together */
    default int priority() {
        return 100;
    }

    /** the chip exists and its caches are initialized */
    boolean ready();

    /** called once per snapshot before {@link #active}/{@link #read}, for readers that batch */
    default void poll() {
    }

    /** true once the chip made sound in the group since {@link #reset}; sticky */
    boolean active(Group group);

    /** channel count of the group; the source caps at the rows it has */
    int channels(Group group);

    /**
     * How many level meter columns the group needs. Defaults to one per channel; a chip whose
     * rows are not all separate voices - the OPNA's ch3 extension rows are operator slots of FM3 -
     * reports fewer and maps them with {@link #meterOf}.
     */
    default int meters(Group group) {
        return channels(group);
    }

    /**
     * Meter column of a channel, relative to this reader's span, or -1 when the channel has no
     * meter of its own.
     */
    default int meterOf(Group group, int ch) {
        return ch;
    }

    /** fills {@code out} with channel {@code ch}'s current state; called once per snapshot */
    void read(Group group, int ch, FmDspChannel out);

    /**
     * The chip channel a row shows, which is what muting the row mutes - see
     * {@link BaseChip#isMuted}. Null - the default - for a row that cannot be muted.
     */
    default BaseChip.Channel muteChannel(Group group, int ch) {
        return null;
    }

    /** true if the channel is muted */
    default boolean masked(Group group, int ch) {
        BaseChip.Channel channel = muteChannel(group, ch);
        return channel != null && channel.muted();
    }

    /**
     * Fills {@code out} with what the channel is doing inside the chip, which the visualizer's
     * {@link vavi.sound.visualizer.fmdsp.RightMode#TRACK_INFO} draws down its right half - an FM
     * channel's four operators, an SSG one's level and tone period, a sampled one's registers.
     * <p>
     * Unlike {@link #read}, this is called from the drawing thread and only while that half is on
     * screen, so a reader that has nothing to show there costs nothing. It reads the same caches
     * {@link #poll} does and keeps no state of its own.
     *
     * @return whether the channel has any detail; false leaves its row blank
     */
    default boolean readDetail(Group group, int ch, TrackDetail out) {
        return false;
    }

    /** the chip's TimerB period register if the song programs one, 0 otherwise */
    default int timerB() {
        return 0;
    }

    /**
     * The spectrum of what this chip is sounding, for a chip whose sound never reaches the mixer;
     * null - the default - for one whose does.
     * <p>
     * The analyzer bars are measured off the rendered PCM, which is the sound itself and beats
     * anything that can be worked out from registers. But not everything mdplayer plays is
     * rendered: a MIDI driver sends its notes to a synthesizer that mixes its own sound, and
     * leaves mdplayer's mixer silent - the meters move, the keyboards play, and the bars stand at
     * nothing. A reader in that position hands over a spectrum drawn from the notes instead, which
     * the source shows alongside the rendered one.
     * <p>
     * Called from the drawing thread, once per frame, like {@link #readDetail}.
     */
    default FftDataSource spectrum() {
        return null;
    }
}
