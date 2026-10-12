/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import mdplayer.Chip;
import mdplayer.Common.EnmModel;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdsound.Instrument;
import musicDriverInterface.MetaData.Tag;


/**
 * BaseChip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-04-13 nsano initial version <br>
 */
public abstract class BaseChip implements Chip {

    private static final Logger logger = System.getLogger(BaseChip.class.getName());

    BasePlugin<? extends BaseDriver> context;

    int dumpCounter = 0;

    final Setting setting = Setting.getInstance();

    // for ym chips TODO
    static final byte[] algM = {0x08, 0x08, 0x08, 0x08, 0x0c, 0x0e, 0x0e, 0x0f};

    @Override
    public void init(BasePlugin<? extends BaseDriver> context) {
        this.context = context;
    }

    @Override
    public void reset() {
    }

    @Override
    public void updateVol() {
    }

    /** @return not null */
    public Map<String, Object> getInfo(int chipId) {
        return Collections.emptyMap();
    }

    /**
     * How many of this chip the song being played declared, {@code 0} when it uses none.
     * <p>
     * A VGM may hold two of most chips - a Sega System 18 board is two YM3438s - and the plugin
     * registers one mixer instance per declaration. Everything that reads a chip by id (the
     * visualizer's readers, the chip views) has to know which ids are really there: the caches are
     * sized for two whatever the song does, so their contents alone never say.
     */
    public int instances() {
        var infos = context == null ? null : context.getChipInstances().get(getClass());
        return infos == null ? 0 : infos.size();
    }

    void fireEventHappened(String name, Object... args) {
        if (context.getDriver() != null) context.getDriver().fireEventHappened(this, name, args);
    }

    void dumpData(EnmModel model, String name, int adr, byte[] rom, int len) {
        if (model == EnmModel.RealModel) return;
        if (!setting.getOther().getDumpSwitch()) return;

        try {
            Path fn = Path.of(setting.getOther().getDumpPath(), "%2$s_%3$s_%1$03d.bin".formatted(dumpCounter++, getClass().getSimpleName().replace("Chip", "_") + name, context.getDriver().metaData.getFirst(Tag.Title).replace("*", "").replace("?", "").replace(" ", "").replace("\"", "").replace("/", "")));
            try (OutputStream fs = Files.newOutputStream(fn)) {
                fs.write(rom, adr, len);
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            // Ignore the error
        }
    }

    /**
     * Which channels of each instance are muted, bit n for channel n. This is the only place a
     * mute is kept: the chip views, the visualizer and the emulators all read it from here or are
     * handed it by {@link #applyMute}. The chips are shared singletons, so a mute outlives the
     * song it was set in, the way the views show it.
     */
    private final long[] mutes = new long[2];

    /** whether channel {@code ch} of instance {@code chipId} is muted */
    public final boolean isMuted(int chipId, int ch) {
        return ch >= 0 && ch < Long.SIZE && (mutes[chipId] & (1L << ch)) != 0;
    }

    /** all the channels of instance {@code chipId} that are muted, bit n for channel n */
    public final long muteMask(int chipId) {
        return mutes[chipId];
    }

    /**
     * Mutes or un-mutes channel {@code ch} of instance {@code chipId}, together with the channels
     * {@linkplain #linked linked} to it.
     */
    public final void setMuted(int chipId, int ch, boolean muted) {
        if (ch < 0 || ch >= Long.SIZE) return;
        long bits = linked(ch);
        if (muted) mutes[chipId] |= bits; else mutes[chipId] &= ~bits;
        applyMute(chipId, ch);
    }

    /**
     * The channels that mute as one with channel {@code ch}, bit n for channel n, {@code ch} itself
     * included. Every channel is on its own by default; an OPN's FM3 and its extension slots are
     * one channel of the chip shown as four rows.
     */
    protected long linked(int ch) {
        return 1L << ch;
    }

    /** flips channel {@code ch} of instance {@code chipId} between muted and sounding */
    public final void toggleMute(int chipId, int ch) {
        setMuted(chipId, ch, !isMuted(chipId, ch));
    }

    /**
     * Hands the mutes over to the song that is about to play: its instruments are built anew and
     * know nothing of them. Call once the plugin has set its chips up.
     */
    public final void restoreMutes() {
        for (int chipId = 0; chipId < mutes.length; chipId++) {
            if (mutes[chipId] != 0) applyMute(chipId, -1);
        }
    }

    /**
     * Makes {@link #muteMask} take effect on the chip. Called after channel {@code ch} changed, and
     * with {@code ch} -1 by {@link #restoreMutes} when every muted channel has to be applied anew.
     * <p>
     * By default the instrument does the muting: it is handed the whole mask. A chip whose
     * instrument cannot mute instead leaves the muted channels' levels out of the register writes
     * as they go by, see {@link #isMuted}; on a change it only re-sends what the chip already
     * holds, so a held note goes quiet at once, and there is nothing to apply as a song starts.
     */
    protected void applyMute(int chipId, int ch) {
        muteInstrument(chipId, muteMask(chipId));
    }

    /** channel {@code ch} of instance {@code chipId}, to mute and ask about */
    public final Channel channel(int chipId, int ch) {
        return new Channel(this, chipId, ch);
    }

    /**
     * One channel of one chip instance: what a view shows as a row, a meter or a key, handed out
     * so that the view can mute it without knowing which chip class and number it really is.
     */
    public record Channel(BaseChip chip, int chipId, int ch) {

        public boolean muted() {
            return chip.isMuted(chipId, ch);
        }

        public void setMuted(boolean muted) {
            chip.setMuted(chipId, ch, muted);
        }

        public void toggle() {
            chip.toggleMute(chipId, ch);
        }
    }

    /** hands {@code mask} to the instrument instance {@code chipId} plays on, if the song uses one */
    protected final void muteInstrument(int chipId, long mask) {
        if (context == null || implementations().length == 0) return;
        Instrument instrument = context.mds.inst(inst(chipId));
        if (instrument != null) instrument.setMuteMask(chipId, mask);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName().replace("Chip", "");
    }
}
