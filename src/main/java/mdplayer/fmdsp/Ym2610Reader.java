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
import java.util.function.Supplier;

import mdplayer.Common;
import mdplayer.chips.BaseChip;
import mdplayer.driver.BaseDriver;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackDetail;
import vavi.sound.visualizer.fmdsp.TrackInfo;
import mdplayer.chips.Ym2610Chip;


/**
 * OPNB: the four FM channels (the chip skips slots 1 and 6 of the OPNA layout, which simply stay
 * dark), SSG 1-3, and both ADPCM sections on the PCM rows - the six ADPCM-A voices a Neo Geo plays
 * its samples with, and the single ADPCM-B one.
 * <p>
 * A whole song can live in the ADPCM-A section alone, and until it was read such a song lit
 * nothing at all: no rows, no meters, a display that said the music was not playing. What it
 * cannot show is a key, because ADPCM-A has no pitch - a channel plays its sample at the chip's
 * one rate and picks the sound by which sample it points at - so those rows say
 * {@link TrackInfo#STREAM} and leave the keyboard alone. ADPCM-B has a rate of its own and gets a
 * key like any other sampled voice.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class Ym2610Reader extends OpnFmReader {

    /** ADPCM-A voices, and the row the one ADPCM-B voice takes after them */
    private static final int ADPCM_A = 6, ADPCM_B = 6;

    /**
     * What the ADPCM-A section plays its samples at [Hz]: the master clock over 432, which is the
     * one rate it has - the reason those rows have no key.
     */
    private static final double adpcmARate = 8000000.0 / 432;

    /** ADPCM-A addresses count 256 byte blocks, and a byte is two samples */
    private static final int adpcmABlock = 256, adpcmASamplesPerByte = 2;

    /** one step of an ADPCM level register, in dB - both the channel's and the section's */
    private static final double adpcmAStepDb = 0.75;

    /** ADPCM-B delta-N of an unshifted sample, o4 c, as {@link Ym2608Reader} has it */
    private static final int adpcmBaseDeltaN = 0x49ba;

    private Supplier<BaseDriver> driver;

    /** hit count of each ADPCM-A channel at the previous poll, for the key on edge */
    private final int[] prevHits = new int[ADPCM_A];

    /** the driver's sample counter when each ADPCM-A channel was struck, {@code -1} for never */
    private final long[] hitCounters = new long[ADPCM_A];

    private int prevAdpcmBCtrl;

    private boolean pcmActive;

    @Override
    protected Ym2610Chip chip() {
        return chipRegister.chip(Ym2610Chip.class);
    }

    @Override
    public void bind(Supplier<BaseDriver> driver) {
        this.driver = driver;
    }

    @Override
    public void reset() {
        super.reset();
        Arrays.fill(prevHits, 0);
        Arrays.fill(hitCounters, -1);
        prevAdpcmBCtrl = 0;
        pcmActive = false;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM, Group.SSG, Group.PCM);
    }

    @Override
    public int channels(Group group) {
        return group == Group.PCM ? ADPCM_A + 1 : super.channels(group);
    }

    @Override
    public boolean active(Group group) {
        if (group != Group.PCM) return super.active(group);
        if (!pcmActive) {
            for (int ch = 0; ch < ADPCM_A && !pcmActive; ch++) pcmActive = chip().adpcmAKeys[chipId][ch];
            pcmActive |= adpcmBPlaying();
        }
        return pcmActive;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        if (group != Group.PCM) {
            super.read(group, ch, out);
        } else if (ch == ADPCM_B) {
            readAdpcmB(out);
        } else {
            readAdpcmA(ch, out);
        }
    }

    @Override
    public BaseChip.Channel muteChannel(Group group, int ch) {
        // the section masks as one: its key register is one write for all six voices
        return group == Group.PCM ? chip().channel(chipId, ch == ADPCM_B ? 13 : 12) : super.muteChannel(group, ch);
    }

    /** port 1 of the register file, where the ADPCM-A section is */
    private int regA(int addr) {
        int[][] regs = chip().register[chipId];
        return regs == null ? 0 : regs[1][addr] & 0xff;
    }

    /** port 0, where the SSG, the FM and the ADPCM-B section are */
    private int regB(int addr) {
        int[][] regs = chip().register[chipId];
        return regs == null ? 0 : regs[0][addr] & 0xff;
    }

    /**
     * One ADPCM-A voice: a sample, struck and left to run out.
     * <p>
     * Nothing reports when it has run out, so the length of the sample says: the start and end
     * registers are 256 byte blocks of the chip's memory, a byte is two samples, and they all play
     * at {@link #adpcmARate}. Without that a hit would sit on the row lit up until the next one -
     * for the last hit of a song, forever.
     */
    private void readAdpcmA(int ch, FmDspChannel out) {
        out.name = "ADPCM";
        out.num = ch + 1;
        out.pcmCh = ch + 1;
        // it plays a sample at a fixed rate: there is no note to show, and this says so rather
        // than leaving a row that carries a whole drum part looking like a silent one
        out.sampled = true;
        out.info = TrackInfo.STREAM;

        int hits = chip().adpcmAHits[chipId][ch];
        boolean struck = hits != prevHits[ch];
        prevHits[ch] = hits;
        if (struck) hitCounters[ch] = counter();

        out.keyOn = struck;
        out.sounding = chip().adpcmAKeys[chipId][ch] && !finished(ch);
        // the sample it points at, which is as near as this section comes to an instrument number
        out.toneNum = regA(0x10 + ch) | regA(0x18 + ch) << 8;

        int level = regA(0x08 + ch) & 0x1f;
        out.volume = level;
        // both levels attenuate, 0.75 dB a step, and both count down from all bits set
        out.amplitude = out.sounding
                ? Math.pow(10, -((0x1f - level) + (0x3f - (regA(0x01) & 0x3f))) * adpcmAStepDb / 20)
                : 0;
        out.pan = opnPan(regA(0x08 + ch) & 0xc0);
    }

    /** whether the channel's sample has had time to run out since it was struck */
    private boolean finished(int ch) {
        if (hitCounters[ch] < 0) return false;
        int start = regA(0x10 + ch) | regA(0x18 + ch) << 8;
        int end = regA(0x20 + ch) | regA(0x28 + ch) << 8;
        if (end < start) return false; // nothing sensible in the registers, let the key say
        double seconds = (end - start + 1) * (double) adpcmABlock * adpcmASamplesPerByte / adpcmARate;
        return (counter() - hitCounters[ch]) / (double) Common.VGMProcSampleRate > seconds;
    }

    /** the driver's own sample counter, which is the only clock a reader has */
    private long counter() {
        BaseDriver d = driver != null ? driver.get() : null;
        return d != null ? d.counter : 0;
    }

    private boolean adpcmBPlaying() {
        int ctrl = regB(0x10);
        return (ctrl & 0x80) != 0 && (ctrl & 0x01) == 0;
    }

    /**
     * The ADPCM-B voice, which is the streamed one: a rate of its own, so it gets the key the rate
     * comes nearest the way every other sampled row does.
     */
    private void readAdpcmB(FmDspChannel out) {
        out.name = "ADPCM";
        out.num = ADPCM_B + 1;
        out.pcmCh = ADPCM_B + 1;
        out.sampled = true;

        int ctrl = regB(0x10);
        boolean playing = adpcmBPlaying();
        out.sounding = playing;
        out.keyOn = playing && ctrl != prevAdpcmBCtrl;
        prevAdpcmBCtrl = ctrl;

        int deltaN = regB(0x19) | regB(0x1a) << 8;
        out.pitchOfRatio(playing && deltaN > 0 ? deltaN / (double) adpcmBaseDeltaN : 0);
        int level = regB(0x1b);
        out.volume = level;
        out.amplitude = playing ? level / 255.0 : 0;
        out.pan = opnPan(regB(0x11) & 0xc0);
    }

    /**
     * The PCM rows' panel: where in the chip's memory the sample runs and what it is played at,
     * which is all either section has to show.
     */
    @Override
    public boolean readDetail(Group group, int ch, TrackDetail out) {
        if (group != Group.PCM) return super.readDetail(group, ch, out);
        out.lines = 0;
        if (ch == ADPCM_B) {
            out.header = "LEVEL DELTA-N START  END";
            out.text = "%3d   %04X    %04X   %04X".formatted(regB(0x1b),
                    regB(0x19) | regB(0x1a) << 8, regB(0x12) | regB(0x13) << 8,
                    regB(0x14) | regB(0x15) << 8);
        } else {
            out.header = "LEVEL PAN START  END    KEY";
            out.text = "%3d   %3s %04X   %04X   %3s".formatted(regA(0x08 + ch) & 0x1f,
                    switch (regA(0x08 + ch) & 0xc0) {
                        case 0x80 -> "L";
                        case 0x40 -> "R";
                        case 0xc0 -> "LR";
                        default -> "-";
                    },
                    regA(0x10 + ch) | regA(0x18 + ch) << 8, regA(0x20 + ch) | regA(0x28 + ch) << 8,
                    chip().adpcmAKeys[chipId][ch] ? " ON" : "OFF");
        }
        return true;
    }

    @Override protected boolean hasSsg() { return true; }

    /** the OPNB's usual 8 MHz clock */
    @Override protected double fnumK() { return 8000000.0 / 144; }

    @Override public String chipName() { return "OPNB"; }

    @Override public int priority() { return 30; }

    @Override
    protected BaseChip.Channel fmMute(int ch) {
        return chip().channel(chipId, ch < 6 ? ch : 9 + ch - 6);
    }

    @Override
    protected BaseChip.Channel ssgMute(int s) {
        return chip().channel(chipId, 6 + s);
    }
    /** the chip's channel state, read back once a frame */
    private Map<String, Object> info;

    private int intOf(String key) {
        Object value = info.isEmpty() ? null : info.get(key);
        return value instanceof Integer i ? i : 0;
    }

    private boolean boolOf(String key) {
        Object value = info.isEmpty() ? null : info.get(key);
        return value instanceof Boolean b && b;
    }

    @Override
    public void poll() {
        info = Collections.emptyMap();
        keys = new int[6];
        try {
            info = chip().getInfo(chipId);
        } catch (RuntimeException ignore) {
            return; // the chip exists but the song never loaded it
        }
        if (info.isEmpty()) return;
        // the rows want a channel that sounds to have bit 0, and channel 3's extended mode reads
        // the slot bits, so build the shape they expect out of what the chip reports
        for (int ch = 0; ch < keys.length; ch++) {
            int mask = 0;
            for (int slot = 0; slot < 4; slot++) {
                if (ch == 2 && boolOf("channels.2.slots." + slot + ".keyOn")) mask |= 0x10 << slot;
            }
            if (boolOf("channels." + ch + ".keyOn")) mask |= ch == 2 ? 0x81 : 1;
            keys[ch] = mask;
        }
    }

    private int[] keys = new int[6];

    @Override protected int[] keyOns() { return keys; }

    @Override protected boolean ch3Extended() { return boolOf("ch3ex"); }

    @Override protected int fmFnum(int ch) { return intOf("channels." + ch + ".fnum"); }

    @Override protected int fmBlock(int ch) { return intOf("channels." + ch + ".block"); }

    @Override protected int fmTotalLevel(int ch) { return intOf("channels." + ch + ".totalLevel"); }

    @Override protected Pan fmPan(int ch) { return opnPan(intOf("channels." + ch + ".pan") << 6); }

    @Override protected int lfoRegister() { return intOf("lfo"); }

    @Override protected int fmSensitivity(int ch) { return intOf("channels." + ch + ".sensitivity"); }

    @Override protected boolean fmAmOn(int ch) { return boolOf("channels." + ch + ".amOn"); }

    @Override protected int exFnum(int x) { return intOf("channels.2.slots." + x + ".fnum"); }

    @Override protected int exBlock(int x) { return intOf("channels.2.slots." + x + ".block"); }

    @Override protected int[] ssgRegs() {
        return info.get("ssg.register") instanceof int[] r ? r : null;
    }

    @Override protected int timerBRegister() { return intOf("timerB"); }

    @Override protected int[][] ports() { return noPorts; }

    // the operators of the TRACK_INFO panel, which the fmgen core answers for itself

    /** what the core says about one operator, or null when it is not answering */
    private Object slotOf(int ch, int slot, String field) {
        return info == null ? null : info.get("channels." + ch + ".slots." + slot + "." + field);
    }

    @Override protected int slotTotalLevel(int ch, int slot) {
        return slotOf(ch, slot, "totalLevel") instanceof Integer tl ? tl : super.slotTotalLevel(ch, slot);
    }

    @Override protected int slotEnvelope(int ch, int slot) {
        return slotOf(ch, slot, "envelope") instanceof Integer envelope ? envelope : -1;
    }

    @Override protected String slotPhase(int ch, int slot) {
        return slotOf(ch, slot, "phase") instanceof String phase ? phase : null;
    }

    @Override protected boolean slotCarrier(int ch, int slot) {
        return slotOf(ch, slot, "carrier") instanceof Boolean carrier ? carrier : super.slotCarrier(ch, slot);
    }

    @Override protected int[][] toneRegs() { return chip() != null ? chip().register[chipId] : null; }

    private static final int[][] noPorts = {new int[0x100], new int[0x100]};

}
