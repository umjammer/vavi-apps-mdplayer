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

import mdplayer.chips.Ppz8Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackDetail;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * PPZ8: eight channels from the chip's info view. The chip has no note, only a playback rate, so
 * the key is the note whose pitch that rate comes closest to.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class Ppz8Reader extends ChipReader {

    private Map<String, Object> info;
    private final boolean[] prevKeyOns = new boolean[8];
    private final int[] prevNotes = new int[8];
    private boolean active;

    @Override
    protected Ppz8Chip chip() {
        return chipRegister.chip(Ppz8Chip.class);
    }

    @Override
    public String chipName() {
        return "PPZ8";
    }

    @Override
    public void reset() {
        info = Collections.emptyMap();
        Arrays.fill(prevKeyOns, false);
        Arrays.fill(prevNotes, -1);
        active = false;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.PCM);
    }

    @Override
    public int priority() {
        return 55;
    }

    @Override
    public void poll() {
        try {
            info = chip().getInfo(chipId);
        } catch (RuntimeException ignore) {
            // the chip exists but was never initialized for this song
            info = Collections.emptyMap();
        }
        if (!info.containsKey("channels.0.playing")) info = Collections.emptyMap();
    }

    @Override
    public boolean active(Group group) {
        if (!active && !info.isEmpty()) {
            for (int ch = 0; ch < 8 && !active; ch++) {
                active = (boolean) info.get("channels." + ch + ".playing")
                        || (boolean) info.get("channels." + ch + ".keyOn");
            }
        }
        return active;
    }

    @Override
    public int channels(Group group) {
        return 8;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        if (info.isEmpty()) return;

        boolean playing = (boolean) info.get("channels." + ch + ".playing");
        boolean keyOn = (boolean) info.get("channels." + ch + ".keyOn");
        int volume = (int) info.get("channels." + ch + ".volume");
        int pan = (int) info.get("channels." + ch + ".pan");
        out.pitchOfRatio(playing
                ? (int) info.get("channels." + ch + ".frequency") / (double) 0x8000 : 0);
        int note = out.note;

        out.sounding = playing;
        out.keyOn = keyOn && (!prevKeyOns[ch] || note != prevNotes[ch]);
        prevKeyOns[ch] = keyOn;
        prevNotes[ch] = note;

        out.name = "PPZ8";
        out.num = ch + 1;
        out.info = TrackInfo.PPZ8;
        out.pcmCh = ch + 1;
        out.volume = volume;
        out.toneNum = (int) info.get("channels." + ch + ".flg16");
        // one step of PPZ8's 16 level table is 1.5 dB
        out.amplitude = playing ? Math.pow(10, (Math.min(volume, 15) - 15) * 1.5 / 20) : 0;
        out.pan = panOf(pan);
    }

    /**
     * The channel's own registers, laid out as the original lays them: where the sample sits, how
     * far in it has got and where it loops back to.
     */
    @Override
    public boolean readDetail(Group group, int ch, TrackDetail out) {
        if (info.isEmpty()) return false;

        out.header = "PAN VOL     FREQ      PTR      END    LOOPS    LOOPE";
        out.text = " %2s %03d %08X %08X %08X %08X %08X".formatted(
                panText((int) info.get("channels." + ch + ".pan")),
                (int) info.get("channels." + ch + ".volume"),
                (int) info.get("channels." + ch + ".frequency"),
                (int) info.get("channels." + ch + ".sadr"),
                (int) info.get("channels." + ch + ".eadr"),
                (int) info.get("channels." + ch + ".ladr"),
                (int) info.get("channels." + ch + ".leadr"));
        return true;
    }

    /** the pan as the original writes it: which side it leans and how far, {@code --} for none */
    private static String panText(int pan) {
        if (pan == 0) return "--";
        int offset = pan - 5;
        return "%c%d".formatted(offset < 0 ? 'L' : offset > 0 ? 'R' : ' ', Math.abs(offset));
    }

    /** PPZ8 pans over 0..9, 5 being the centre. 0 is a silent channel, the chip skips it */
    private static Pan panOf(int pan) {
        return switch (pan) {
            case 0 -> Pan.NONE;
            case 1, 2 -> Pan.LEFT;
            case 3, 4 -> Pan.MID_LEFT;
            case 5 -> Pan.CENTER;
            case 6, 7 -> Pan.MID_RIGHT;
            default -> Pan.RIGHT;
        };
    }
}
