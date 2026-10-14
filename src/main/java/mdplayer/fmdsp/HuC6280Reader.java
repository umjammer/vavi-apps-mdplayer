/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import mdplayer.chips.HuC6280Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * The PC Engine's HuC6280: six wave table channels, the last two of which can play noise instead.
 * <p>
 * The chip keeps no registers here, so the state comes from the emulator's own view: a channel's
 * left and right output levels, its pan, its wave number and - already worked out for us - the
 * frequency it is running at, which makes these keys real pitches rather than the rate ratios the
 * sampled chips have to settle for.
 * <p>
 * Six channels do not fit the three SSG rows, so they take the wider FM block.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class HuC6280Reader extends ChipReader {

    private static final int CHANNELS = 6;

    /** the level the emulator's view tops out at, the value its own panel clamps to */
    private static final double levelMax = 19;

    private Map<String, Object> info;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevNotes = new int[CHANNELS];
    private boolean active;

    @Override
    protected HuC6280Chip chip() {
        return chipRegister.chip(HuC6280Chip.class);
    }

    @Override
    public String chipName() {
        return "HuC6";
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevNotes, -1);
        info = Collections.emptyMap();
        active = false;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 93;
    }

    @Override
    public void poll() {
        try {
            info = chip().getInfo(chipId);
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
            info = Collections.emptyMap();
        }
    }

    /** the emulator leaves a channel out of its view entirely when the song never set it up */
    private boolean has(int ch) {
        return info != null && info.get("channels." + ch + ".volumeL") != null;
    }

    private int levelOf(int ch) {
        return Math.max((int) info.get("channels." + ch + ".volumeL"),
                (int) info.get("channels." + ch + ".volumeR"));
    }

    @Override
    public boolean active(Group group) {
        if (!active && info != null) {
            for (int ch = 0; ch < CHANNELS && !active; ch++) {
                active = has(ch) && levelOf(ch) > 0;
            }
        }
        return active;
    }

    @Override
    public int channels(Group group) {
        return CHANNELS;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        out.name = "SSG";
        out.num = ch + 1;
        out.info = TrackInfo.SSG;
        if (!has(ch)) return;

        int level = levelOf(ch);
        boolean noise = ch >= 4 && Boolean.TRUE.equals(info.get("channels." + ch + ".noise"));
        boolean sounding = level > 0;
        // the view hands us the frequency itself, so the key needs no guessing
        float ftone = (float) info.get("channels." + ch + ".ftone");
        int note = noise ? -1 : Notes.noteOf(ftone);

        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || note != prevNotes[ch]);
        prevSoundings[ch] = sounding;
        prevNotes[ch] = note;

        out.note = note;
        out.volume = level;
        out.amplitude = sounding ? Math.min(level, levelMax) / levelMax : 0;
        out.ssgTone = !noise;
        out.ssgNoise = noise;
        out.toneNum = ch; // its wave is a table rather than a number, so show the channel
        out.pan = panOf((int) info.get("channels." + ch + ".pan"));
    }

    /** the view packs the two four bit output levels, left in the low nibble */
    private static Pan panOf(int pan) {
        int l = pan & 0x0f;
        int r = (pan >> 4) & 0x0f;
        if (l == 0 && r == 0) return Pan.NONE;
        if (r == 0) return Pan.LEFT;
        if (l == 0) return Pan.RIGHT;
        double balance = r / (double) (l + r);
        if (balance < 0.25) return Pan.LEFT;
        if (balance < 0.45) return Pan.MID_LEFT;
        if (balance <= 0.55) return Pan.CENTER;
        if (balance <= 0.75) return Pan.MID_RIGHT;
        return Pan.RIGHT;
    }
}
