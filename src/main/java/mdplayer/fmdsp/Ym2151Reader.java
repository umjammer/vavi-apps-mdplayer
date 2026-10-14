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

import mdplayer.chips.BaseChip;
import mdplayer.chips.Ym2151Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackDetail;


/**
 * OPM: eight FM channels, the note straight from the key code register.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class Ym2151Reader extends ChipReader {

    private final int[] prevKeyOns = new int[8];
    private boolean active;

    @Override
    protected Ym2151Chip chip() {
        return chipRegister.chip(Ym2151Chip.class);
    }

    @Override
    public String chipName() {
        return "OPM";
    }

    @Override
    public void reset() {
        Arrays.fill(prevKeyOns, 0);
        active = false;
        slots.reset();
        tones.reset();
    }

    /** which channels a second chip's rows show - it gets one row, so it has to be a used one */
    private final RowSlots slots = new RowSlots(8);

    /** the channel a row shows: itself for the first chip, whatever claimed it for a second */
    private int channelOf(int slot) {
        if (chipId == 0) return slot;
        slots.claim(8, ch -> boolOf("channels." + ch + ".keyOn"));
        return slots.channelOf(slot);
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM);
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public boolean active(Group group) {
        if (!active) {
            for (int ch = 0; ch < 8 && !active; ch++) active = boolOf("channels." + ch + ".keyOn");
        }
        return active;
    }

    @Override
    public int channels(Group group) {
        return 8;
    }

    @Override
    public void read(Group group, int slot, FmDspChannel out) {
        out.name = "FM";
        int ch = channelOf(slot);
        if (info == null || ch < 0) return;
        boolean on = boolOf("channels." + ch + ".keyOn");
        int kc = on ? 1 : 0;

        out.name = "FM";
        out.num = ch + 1;
        out.sounding = on;
        out.keyOn = kc != prevKeyOns[ch] && on;
        prevKeyOns[ch] = kc;

        int keyCode = intOf("channels." + ch + ".keyCode");
        int code = keyCode & 0x0f;
        // the OPM key code skips every fourth value: 0 is C#, 14 the next octave's C. The key
        // fraction then carries the pitch on up towards the next note, in sixty-fourths of one -
        // which is where a driver's detune, and every step of its portamento, end up, so the two
        // are read as one number and split again at the nearest note
        double semitones = ((keyCode >> 4) & 0x07) * 12 + code - (code >> 2) + 1
                + intOf("channels." + ch + ".keyFraction") / 64.0;
        out.note = (int) Math.round(semitones);
        out.detune = (int) Math.round((semitones - out.note) * 100);
        lfo(ch, out);
        out.toneNum = tone(ch);
        int tl = intOf("channels." + ch + ".totalLevel");
        out.volume = 127 - tl;
        out.amplitude = Math.pow(10, -tl * 0.75 / 20);
        out.pan = panOf(intOf("channels." + ch + ".pan") << 6);
    }

    /**
     * The voice the channel is playing, numbered by {@link ToneNumbers} out of the registers that
     * make one up. The OPM keeps its four operators eight apart, and the total level at 0x60 is
     * left out - it is the channel's volume as much as its voice, see {@code OpnFmReader#fmTone}.
     */
    private int tone(int ch) {
        if (!(info.get("register") instanceof int[] regs)) return 0;
        long fingerprint = ToneNumbers.fold(ToneNumbers.seed(), regs[0x20 + ch] & 0x3f); // feedback, connection
        int written = regs[0x20 + ch] & 0x3f;
        for (int slot = 0; slot < 4; slot++) {
            int r = ch + slot * 8;
            for (int reg : new int[] {0x40, 0x80, 0xa0, 0xc0, 0xe0}) {
                fingerprint = ToneNumbers.fold(fingerprint, regs[reg + r]);
                written |= regs[reg + r];
            }
        }
        return written == 0 ? 0 : tones.numberOf(fingerprint);
    }

    private final ToneNumbers tones = new ToneNumbers();

    /**
     * Whether the chip's LFO is audible on this channel. It always runs, so it takes a depth to
     * swing by - register 0x19, one for the pitch and one for the level - and a channel sensitive
     * to that half of it: PMS in bits 0-2 of register 0x38 and AMS in bits 4-5, plus an operator
     * switched to follow the amplitude side.
     */
    private void lfo(int ch, FmDspChannel out) {
        int sens = intOf("channels." + ch + ".sensitivity");
        out.lfoPitch = intOf("pmd") != 0 && (sens & 0x07) != 0;
        out.lfoVolume = intOf("amd") != 0 && ((sens >> 4) & 0x03) != 0
                && boolOf("channels." + ch + ".amOn");
    }

    /** the OPM register 0x20 pan bits, left in bit 6 and right in bit 7 */
    private static Pan panOf(int reg) {
        boolean left = (reg & 0x40) != 0;
        boolean right = (reg & 0x80) != 0;
        if (left && right) return Pan.CENTER;
        if (left) return Pan.LEFT;
        if (right) return Pan.RIGHT;
        return Pan.NONE;
    }

    @Override
    public BaseChip.Channel muteChannel(Group group, int slot) {
        return super.muteChannel(group, channelOf(slot));
    }

    /**
     * The channel's four operators, straight out of the core's envelope generators, with the key
     * code and the key fraction behind the first of them - the OPM's pitch, where the OPN
     * family's is an F-number and a block.
     */
    @Override
    public boolean readDetail(Group group, int row, TrackDetail out) {
        int ch = channelOf(row);
        if (info == null || ch < 0) return false;

        out.lines = 4;
        for (int op = 0; op < 4; op++) {
            String slot = "channels." + ch + ".slots." + op;
            if (info.get(slot + ".envelope") instanceof Integer envelope) {
                FmDetail.operator(out, op, intOf(slot + ".totalLevel"), envelope,
                        stringOf(slot + ".phase"));
            } else if (info.get("register") instanceof int[] regs) {
                // an MDX is played by the X68000's own core, which keeps no envelope anyone can
                // read; all there is to draw is what the driver wrote, which the wrapper shadows.
                // The OPM keeps its four operators eight registers apart
                out.modelled = true;
                FmDetail.operator(out, op, regs[0x60 + ch + op * 8],
                        FmDetail.carrier(regs[0x20 + ch] & 0x07, op));
            } else {
                return false;
            }
        }
        out.extra[0] = "%04X".formatted(intOf("channels." + ch + ".keyCode") << 6
                | intOf("channels." + ch + ".keyFraction"));
        return true;
    }

    @Override
    public int timerB() {
        return intOf("timerB");
    }

    /** the chip's channel state, read back once a frame */
    private Map<String, Object> info;

    @Override
    public void poll() {
        try {
            info = chip().getInfo(chipId);
        } catch (RuntimeException ignore) {
            info = null; // the chip exists but the song never loaded it
        }
    }

    private int intOf(String key) {
        Object value = info == null ? null : info.get(key);
        return value instanceof Integer i ? i : 0;
    }

    private boolean boolOf(String key) {
        Object value = info == null ? null : info.get(key);
        return value instanceof Boolean b && b;
    }

    private String stringOf(String key) {
        Object value = info == null ? null : info.get(key);
        return value instanceof String s ? s : null;
    }
}
