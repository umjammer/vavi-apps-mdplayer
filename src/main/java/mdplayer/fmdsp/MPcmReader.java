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

import mdplayer.chips.MPcmChip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackDetail;


/**
 * MPCM (X68000, ZMUSIC v3 and MNDRV): sixteen voices from either back end's info view.
 * <p>
 * Unlike PPZ8 the chip is told a note, the pitch word ZMUSIC sends it ({@code note << 6 | fine}),
 * and that note counts like a MIDI one, so it goes to the keyboard as it is. For a drum kit tone
 * it is the kit's key, which is what the original shows too.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-24 nsano initial version <br>
 */
public class MPcmReader extends ChipReader {

    private static final int VOICES = 16;

    private Map<String, Object> info;
    private final boolean[] prevPlayings = new boolean[VOICES];
    private final int[] prevNotes = new int[VOICES];
    private final long[] prevPositions = new long[VOICES];
    private boolean active;

    @Override
    protected MPcmChip chip() {
        return chipRegister.chip(MPcmChip.class);
    }

    @Override
    public String chipName() {
        return "MPCM";
    }

    @Override
    public void reset() {
        info = Collections.emptyMap();
        Arrays.fill(prevPlayings, false);
        Arrays.fill(prevNotes, -1);
        Arrays.fill(prevPositions, 0);
        active = false;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.PCM);
    }

    @Override
    public int priority() {
        return 56;
    }

    @Override
    public void poll() {
        try {
            info = chip().getInfo(chipId);
        } catch (RuntimeException ignore) {
            // the chip exists but was never started for this song
            info = Collections.emptyMap();
        }
        if (!info.containsKey("channels.0.playing")) info = Collections.emptyMap();
    }

    private boolean playing(int ch) {
        return (boolean) info.get("channels." + ch + ".playing");
    }

    @Override
    public boolean active(Group group) {
        if (!active && !info.isEmpty()) {
            for (int ch = 0; ch < VOICES && !active; ch++) active = playing(ch);
        }
        return active;
    }

    @Override
    public int channels(Group group) {
        return VOICES;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        if (info.isEmpty()) return;

        boolean playing = playing(ch);
        int note = (int) info.get("channels." + ch + ".note");
        int volume = (int) info.get("channels." + ch + ".volume");
        long pos = (int) info.get("channels." + ch + ".pos");

        out.sounding = playing;
        out.note = playing ? note : -1;
        // a retrigger of the same note restarts the sample, which is a key-on though nothing else changed
        out.keyOn = playing && (!prevPlayings[ch] || note != prevNotes[ch] || pos < prevPositions[ch]);
        prevPlayings[ch] = playing;
        prevNotes[ch] = note;
        prevPositions[ch] = pos;

        out.name = "MPCM";
        out.num = ch + 1;
        out.pcmCh = ch + 1;
        out.volume = volume;
        out.toneNum = (int) info.get("channels." + ch + ".type");
        // 0x40 is the sample's own level and 0x7f about 6x; the meter shows where in that range it is set
        out.amplitude = playing ? Math.min(1, volume / 127.0) : 0;
        out.pan = panOf((int) info.get("channels." + ch + ".pan"));
    }

    @Override
    public boolean readDetail(Group group, int ch, TrackDetail out) {
        if (info.isEmpty()) return false;

        int type = (int) info.get("channels." + ch + ".type");
        out.header = "PAN VOL TYPE     RATE      POS     SIZE";
        out.text = " %2s %03d %4s %8.0f %08X %08X".formatted(
                switch ((int) info.get("channels." + ch + ".pan")) { case 1 -> "L"; case 2 -> "R"; case 3 -> "LR"; default -> "--"; },
                (int) info.get("channels." + ch + ".volume"),
                switch (type) { case 0xff -> "ADPC"; case 1 -> "16BT"; case 2 -> "8BIT"; default -> "----"; },
                (double) info.get("channels." + ch + ".rate"),
                (int) info.get("channels." + ch + ".pos"),
                (int) info.get("channels." + ch + ".size"));
        return true;
    }

    private static Pan panOf(int pan) {
        return switch (pan) {
            case 1 -> Pan.LEFT;
            case 2 -> Pan.RIGHT;
            case 3 -> Pan.CENTER;
            default -> Pan.NONE;
        };
    }

    @Override
    public boolean masked(Group group, int ch) {
        return chip().getMask(chipId, ch);
    }
}
