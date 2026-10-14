/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import mdplayer.ChipRegister;
import mdplayer.chips.BaseChip;
import mdsound.np.chip.DeviceInfo.BasicTrackInfo;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * The shape the NSF expansion chips report in: an array of track info, one a channel, each with a
 * real frequency in Hz, a volume against its own maximum, and a key.
 * <p>
 * The frequency is already worked out by the emulator, so unlike the sampled chips these are true
 * pitches. They sit on the wider block rather than the three SSG rows, since most of these chips
 * have more channels than that, and are mono.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public abstract class NsfTrackInfoReader extends ChipReader {

    private BasicTrackInfo[] tracks;

    private boolean[] prevSoundings;
    private int[] prevKeys;
    private boolean active;

    protected abstract int channelCount();

    @Override
    protected abstract BaseChip chip();

    @Override
    public void bind(ChipRegister chipRegister) {
        super.bind(chipRegister);
        reset();
    }

    @Override
    public void reset() {
        if (prevSoundings == null) {
            prevSoundings = new boolean[channelCount()];
            prevKeys = new int[channelCount()];
        }
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevKeys, 0);
        active = false;
        tracks = null;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public void poll() {
        tracks = null;
        try {
            Map<String, Object> info = chip().getInfo(chipId);
            if (info != null && info.get("tracksInfo") instanceof Object[] raw) {
                BasicTrackInfo[] got = new BasicTrackInfo[raw.length];
                for (int i = 0; i < raw.length; i++) {
                    got[i] = raw[i] instanceof BasicTrackInfo t ? t : null;
                }
                tracks = got;
            }
        } catch (RuntimeException ignore) {
            // the chip exists but the song never loaded it
        }
    }

    private BasicTrackInfo track(int ch) {
        return tracks != null && ch < tracks.length ? tracks[ch] : null;
    }

    private boolean sounding(int ch) {
        BasicTrackInfo t = track(ch);
        return t != null && t.key && t.volume > 0;
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int ch = 0; ch < channelCount() && !active; ch++) active = sounding(ch);
        }
        return active;
    }

    @Override
    public int channels(Group group) {
        return channelCount();
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        out.name = "SSG";
        out.num = ch + 1;
        out.info = TrackInfo.SSG;
        out.pan = Pan.CENTER; // these are all mono
        BasicTrackInfo t = track(ch);
        if (t == null) return;

        boolean sounding = sounding(ch);
        int key = t.freq > 0 ? Notes.noteOf(t.freq) : -1;

        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || key != prevKeys[ch]);
        prevSoundings[ch] = sounding;
        prevKeys[ch] = key;

        out.volume = t.volume;
        out.amplitude = sounding && t.maxVolume > 0 ? t.volume / (double) t.maxVolume : 0;
        out.ssgTone = true;
        out.toneNum = t.tone;
        out.note = sounding ? key : -1;
    }
}
