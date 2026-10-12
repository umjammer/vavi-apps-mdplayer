/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackDetail;
import vavi.sound.visualizer.fmdsp.TrackInfo;

import mdplayer.chips.BaseChip;


/**
 * The OPN family's shared shape: FM channels with the ch3 extended slots on the three extra
 * channels 6-8, and optionally the SSG section. Subclasses supply the chip's register banks and
 * key-on cache.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public abstract class OpnFmReader extends ChipReader {

    /** carrier mask per algorithm, {@code mdplayer.chips.BaseChip#algM} */
    private static final byte[] algM = {0x08, 0x08, 0x08, 0x08, 0x0c, 0x0e, 0x0e, 0x0f};

    /** ch3 extended slots 1-3: F-number registers and the reg 0x28 key bit of each */
    private static final int[] exFnumLo = {0xa9, 0xaa, 0xa8};
    private static final int[] exFnumHi = {0xad, 0xae, 0xac};
    private static final int[] exKeyBit = {0x10, 0x20, 0x40};

    private final int[] prevKeyOns = new int[6];
    private final boolean[] prevExOns = new boolean[3];
    private final boolean[] prevSsgSoundings = new boolean[3];
    private final int[] prevSsgPeriods = new int[3];
    private boolean fmActive;
    private boolean ssgActive;

    private final ToneNumbers tones = new ToneNumbers();

    /** the chip's register banks; a single-port chip returns one bank */
    protected abstract int[][] ports();

    /** the chip's 6-entry key-on cache */
    protected abstract int[] keyOns();

    /** FM channels the chip actually has, 3 or 6 */
    int fmCount() {
        return 6;
    }

    boolean hasSsg() {
        return false;
    }

    /** the mono members of the family have no register 0xb4 */
    boolean hasPan() {
        return true;
    }

    /** F-number coefficient [Hz]: {@code freq = fnum * 2^(block-1) * fnumK / 2^20} */
    double fnumK() {
        return 7987200.0 / 144;
    }

    /** the chip channel of FM channel {@code ch}, 0-8 with the ch3 slots at 6-8; null when it cannot be muted */
    BaseChip.Channel fmMute(int ch) {
        return null;
    }

    /** the chip channel of SSG channel {@code s}; null when it cannot be muted */
    BaseChip.Channel ssgMute(int s) {
        return null;
    }

    @Override
    public void reset() {
        Arrays.fill(prevKeyOns, 0);
        Arrays.fill(prevExOns, false);
        Arrays.fill(prevSsgSoundings, false);
        Arrays.fill(prevSsgPeriods, 0);
        fmActive = false;
        ssgActive = false;
        extendedSeen = false;
        slots.reset();
        tones.reset();
    }

    @Override
    public Set<Group> groups() {
        return hasSsg() ? EnumSet.of(Group.FM, Group.SSG) : EnumSet.of(Group.FM);
    }

    @Override
    public boolean active(Group group) {
        return switch (group) {
            case FM -> {
                if (!fmActive) fmActive = Arrays.stream(keyOns()).anyMatch(k -> k != 0);
                yield fmActive;
            }
            case SSG -> {
                if (!ssgActive) {
                    for (int s = 0; s < 3 && !ssgActive; s++) ssgActive = ssgSounding(s);
                }
                yield ssgActive;
            }
            default -> false;
        };
    }

    /**
     * The FM rows this chip wants: its six channels, and the three ch3 extension rows behind them
     * once the song has used that mode.
     * <p>
     * The extension rows are the operator slots of channel 3, so a song that never switches ch3
     * into its special mode leaves them blank - and a row held blank is a row the chip behind this
     * one does not get. A Sega System 18 VGM is two YM3438s, and the second one, which carries most
     * of the music, had nowhere to go. Once the mode is seen the rows are kept for the rest of the
     * song rather than handed back, so the layout does not move under a song that only uses it now
     * and then.
     * <p>
     * Only for a chip with all six channels: on the plain OPN the rows of the channels it does not
     * have sit between its three and the extension rows, and those keep their places.
     */
    @Override
    public int channels(Group group) {
        if (group != Group.FM) return 3;
        // a second chip takes what the first leaves, and lays its channels out over that itself
        if (chipId > 0) return fmCount();
        if (!extendedSeen && fmCount() == 6 && ready() && ch3Extended()) {
            extendedSeen = true;
        }
        return fmCount() < 6 || extendedSeen ? 9 : 6;
    }

    /** whether ch3's extended mode has been seen this song, see {@link #channels} */
    private boolean extendedSeen;

    /** which channels a second chip's rows show, see {@link RowSlots} */
    private final RowSlots slots = new RowSlots(6);

    /** the channel a row shows: itself for the first chip, whatever claimed it for a second */
    private int fmChannel(int slot) {
        if (chipId == 0) return slot;
        slots.claim(fmCount(), ch -> (keyOns()[ch] & 1) != 0);
        return slots.channelOf(slot);
    }

    @Override
    public int meters(Group group) {
        return switch (group) {
            case FM -> fmCount();
            case SSG -> 3;
            default -> channels(group);
        };
    }

    /** the ch3 extension rows are operator slots of FM3, they have no meter of their own */
    @Override
    public int meterOf(Group group, int ch) {
        if (group != Group.FM) return ch;
        return ch < fmCount() ? ch : -1;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        if (group == Group.SSG) {
            readSsg(ch, out);
            return;
        }
        int channel = fmChannel(ch);
        out.name = "FM";
        if (channel >= 0) readFm(channel, out);
    }

    @Override
    public BaseChip.Channel muteChannel(Group group, int ch) {
        return switch (group) {
            case FM -> {
                int channel = fmChannel(ch);
                yield channel >= 0 ? fmMute(channel) : null;
            }
            case SSG -> ssgMute(ch);
            default -> null;
        };
    }

    // ----- the TRACK_INFO panel -----

    @Override
    public boolean readDetail(Group group, int ch, TrackDetail out) {
        if (group == Group.SSG) return ssgDetail(ch, out);
        int channel = fmChannel(ch);
        return group == Group.FM && channel >= 0 && fmDetail(channel, out);
    }

    /**
     * The channel's four operators and its F-number.
     * <p>
     * In the ch3 extended mode each of the four carries a pitch of its own, so every line gets an
     * F-number instead of only the first - and the three extension rows are that same channel 3,
     * which is why they show the whole panel over again, as the original's do.
     */
    private boolean fmDetail(int ch, TrackDetail out) {
        boolean extension = ch >= 6;
        if (extension && !ch3Extended()) return false;
        if (!extension && ch >= fmCount()) return false;
        int c = extension ? 2 : ch;
        boolean perSlot = c == 2 && ch3Extended();

        out.lines = 4;
        for (int op = 0; op < 4; op++) {
            slot(c, op, out, op);
            if (perSlot) {
                out.extra[op] = "%04X".formatted(op < 3 ? exBlock(op) << 11 | exFnum(op)
                        : fmBlock(c) << 11 | fmFnum(c));
            }
        }
        if (!perSlot) out.extra[0] = "%04X".formatted(fmBlock(c) << 11 | fmFnum(c));
        return true;
    }

    /**
     * One operator's line, out of the core's envelope generator where it has one to read and out
     * of the total level register where it has not.
     */
    private void slot(int ch, int op, TrackDetail out, int line) {
        int envelope = slotEnvelope(ch, op);
        if (envelope < 0) {
            out.modelled = true;
            FmDetail.operator(out, line, slotTotalLevel(ch, op), slotCarrier(ch, op));
        } else {
            FmDetail.operator(out, line, slotTotalLevel(ch, op), envelope, slotPhase(ch, op));
        }
    }

    /**
     * An operator's total level, {@code slot} in the register order M1, M2, C1, C2. Read out of
     * {@link #toneRegs the registers as written}, which every member of the family has whether or
     * not its core keeps a register file of its own.
     */
    int slotTotalLevel(int ch, int slot) {
        int[][] regs = toneRegs();
        return regs == null ? 127 : regs[ch / 3][0x40 + ch % 3 + slot * 4] & 0x7f;
    }

    /**
     * The attenuation an operator's envelope adds, 0 (wide open) to 1023 (silent), or {@code -1}
     * from a core that keeps its envelope generators to itself - then the bar is drawn from the
     * total level alone and animated by the source, see {@link TrackDetail#modelled}.
     */
    int slotEnvelope(int ch, int slot) {
        return -1;
    }

    /** which part of its envelope an operator is in, as {@code Fmgen} names it, or null */
    String slotPhase(int ch, int slot) {
        return null;
    }

    /** whether an operator is heard directly rather than only modulating another */
    boolean slotCarrier(int ch, int slot) {
        int[][] regs = toneRegs();
        return regs != null && FmDetail.carrier(regs[ch / 3][0xb0 + ch % 3] & 0x07, slot);
    }

    /**
     * The channel's level and its tone period, laid out as the original's is: the left half of the
     * bar always lit and the level growing out of the middle of it.
     */
    private boolean ssgDetail(int s, TrackDetail out) {
        int[] regs = ssgRegs();
        if (regs == null) return false;
        int level = regs[0x08 + s] & 0x1f;
        if ((level & 0x10) != 0) level = 15; // on the hardware envelope, and so at full scale
        int period = (regs[s * 2] & 0xff) | ((regs[s * 2 + 1] & 0x0f) << 8);

        out.lines = 1;
        // the SSG's sixteen levels over the far half of the bar, as the original has them
        out.bar[0] = TrackDetail.COLUMNS / 2 + Math.min(level, 15) * 2;
        out.mark[0] = 0;
        out.markWidth[0] = TrackDetail.COLUMNS / 2;
        out.extra[0] = " %03X".formatted(period);
        return true;
    }

    @Override
    public int timerB() {
        return timerBRegister();
    }

    // ----- FM -----

    private void readFm(int ch, FmDspChannel out) {
        boolean ch3ex = ch3Extended();

        out.name = "FM";
        if (ch < 6) {
            out.num = ch + 1;
            if (ch >= fmCount()) return;
            int kc = keyOns()[ch];
            boolean on = ch == 2 && ch3ex ? (kc & 0x80) != 0 : (kc & 1) != 0;
            out.keyOn = kc != prevKeyOns[ch] && on;
            prevKeyOns[ch] = kc;
            out.sounding = on;
            out.note = fmNote(fmFnum(ch), fmBlock(ch));
            out.detune = fmDetune(fmFnum(ch), fmBlock(ch));
            fmLfo(ch, out);
            out.toneNum = fmTone(ch);
            if (ch == 2 && ch3ex) {
                out.info = TrackInfo.FM3EX;
                Arrays.fill(out.fmSlotMask, true);
                out.fmSlotMask[3] = false;
            } else {
                out.info = TrackInfo.NORMAL;
                Arrays.fill(out.fmSlotMask, false);
            }
            int tl = fmTotalLevel(ch);
            out.volume = 127 - tl;
            out.amplitude = Math.pow(10, -tl * 0.75 / 20);
            out.pan = hasPan() ? fmPan(ch) : Pan.CENTER;
        } else {
            int x = ch - 6;
            out.num = 3; // the ch3 slots number as FM3, like the C fmdsp
            if (!ch3ex) {
                prevExOns[x] = false;
                out.info = TrackInfo.NORMAL;
                Arrays.fill(out.fmSlotMask, false);
                return;
            }
            boolean on = (keyOns()[2] & exKeyBit[x]) != 0;
            out.info = TrackInfo.FM3EX;
            Arrays.fill(out.fmSlotMask, true);
            if (x >= 0 && x < 3) {
                out.fmSlotMask[x] = false;
            }
            out.sounding = on;
            out.keyOn = on && !prevExOns[x];
            prevExOns[x] = on;
            out.note = fmNote(exFnum(x), exBlock(x));
            out.detune = fmDetune(exFnum(x), exBlock(x));
            fmLfo(2, out);
            out.toneNum = fmTone(2);
            out.volume = 127 - fmTotalLevel(2);
        }
    }

    /** the value of register 0x26, which paces the display's clock */
    int timerBRegister() {
        return ports()[0][0x26] & 0xff;
    }

    // ----- what a channel is doing; register backed chips answer from their bank -----

    /** the ch3 special mode: {@code 0x40} in register 0x27, CSM keying the chip as normal */
    boolean ch3Extended() {
        return (ports()[0][0x27] & 0xc0) == 0x40;
    }

    int fmFnum(int ch) {
        int[] regs = ports()[ch / 3];
        int r = ch % 3;
        return ((regs[0xa4 + r] & 0x07) << 8) | (regs[0xa0 + r] & 0xff);
    }

    int fmBlock(int ch) {
        return (ports()[ch / 3][0xa4 + ch % 3] >> 3) & 0x07;
    }

    int fmTotalLevel(int ch) {
        return carrierTl(ports()[ch / 3], 0xb0 + ch % 3, 0x40 + ch % 3);
    }

    Pan fmPan(int ch) {
        return opnPan(ports()[ch / 3][0xb4 + ch % 3]);
    }

    int exFnum(int x) {
        int[] regs = ports()[0];
        return ((regs[exFnumHi[x]] & 0x07) << 8) | (regs[exFnumLo[x]] & 0xff);
    }

    int exBlock(int x) {
        return (ports()[0][exFnumHi[x]] >> 3) & 0x07;
    }

    /** semitones above C0 of an OPN F-number and block, -1 when no note was set */
    private int fmNote(int fnum, int block) {
        if (fnum == 0) return -1;
        return Notes.noteOf(fmFreq(fnum, block));
    }

    /** cents an OPN F-number and block sit off the note {@link #fmNote} rounds them to */
    private int fmDetune(int fnum, int block) {
        return fnum == 0 ? 0 : Notes.centsOf(fmFreq(fnum, block));
    }

    private double fmFreq(int fnum, int block) {
        return fnum * Math.pow(2, block - 1) * fnumK() / (1 << 20);
    }

    /**
     * Whether the chip's own LFO reaches channel {@code ch}, which fmdsp shows behind "M:" - the
     * register file's only LFO, a driver's software ones living outside the chip entirely.
     * <p>
     * It takes both halves to be audible: the LFO has to be running at all (register 0x22 bit 3,
     * which the plain OPN has no register for and reads back as off) and the channel has to be
     * sensitive to it, through the PMS and AMS fields of its register 0xb4. AMS also needs an
     * operator switched to follow it, which the four 0x60 registers say.
     */
    private void fmLfo(int ch, FmDspChannel out) {
        if ((lfoRegister() & 0x08) == 0) return;
        int sens = fmSensitivity(ch);
        out.lfoPitch = (sens & 0x07) != 0;
        out.lfoVolume = ((sens >> 4) & 0x03) != 0 && fmAmOn(ch);
    }

    /**
     * The voice a channel is playing, numbered by {@link ToneNumbers}: the operator registers that
     * make it up, folded together and looked up. The total level is deliberately left out - it is
     * the channel's volume as much as its voice, and a driver moves it note by note, so a number
     * that followed it would say the voice had changed at every note.
     */
    private int fmTone(int ch) {
        int[][] banks = toneRegs();
        if (banks == null) return 0;
        int[] regs = banks[ch / 3];
        int c = ch % 3;
        long fingerprint = ToneNumbers.fold(ToneNumbers.seed(), regs[0xb0 + c]); // feedback, algorithm
        int written = regs[0xb0 + c];
        for (int slot = 0; slot < 4; slot++) {
            int r = c + slot * 4;
            for (int reg : new int[] {0x30, 0x50, 0x60, 0x70, 0x80, 0x90}) {
                fingerprint = ToneNumbers.fold(fingerprint, regs[reg + r]);
                written |= regs[reg + r];
            }
        }
        // nothing written yet is not a voice, it is a chip that has not been given one
        return written == 0 ? 0 : tones.numberOf(fingerprint);
    }

    /**
     * The register banks a voice can be read out of, or {@code null} for a chip whose core decodes
     * them away. Defaults to {@link #ports()}, which the register backed members of the family
     * already answer from.
     */
    int[][] toneRegs() {
        return ports();
    }

    /** register 0x22; the plain OPN has no LFO and so no register to answer with */
    int lfoRegister() {
        return ports()[0][0x22];
    }

    /** register 0xb4 of a channel: PMS in bits 0-2, AMS in bits 4-5 */
    int fmSensitivity(int ch) {
        return ports()[ch / 3][0xb4 + ch % 3];
    }

    /** whether any of the channel's operators follows the amplitude modulation, register 0x60 */
    boolean fmAmOn(int ch) {
        int[] regs = ports()[ch / 3];
        for (int slot = 0; slot < 4; slot++) {
            if ((regs[0x60 + ch % 3 + slot * 4] & 0x80) != 0) return true;
        }
        return false;
    }

    /**
     * The softest carrier's total level of a channel, the closest thing the register file has to
     * the part volume.
     */
    private static int carrierTl(int[] regs, int algReg, int tlReg) {
        int alg = regs[algReg] & 0x07;
        int tl = 127;
        for (int slot = 0; slot < 4; slot++) {
            if ((algM[alg] & (1 << slot)) == 0) continue;
            // logical slot order is M1,M2,C1,C2 while the register file is M1,C1,M2,C2
            int t = slot == 1 ? 2 : slot == 2 ? 1 : slot;
            tl = Math.min(tl, regs[tlReg + t * 4] & 0x7f);
        }
        return tl;
    }

    /** the OPN register 0xb4 pan bits */
    static Pan opnPan(int reg) {
        boolean left = (reg & 0x80) != 0;
        boolean right = (reg & 0x40) != 0;
        if (left && right) return Pan.CENTER;
        if (left) return Pan.LEFT;
        if (right) return Pan.RIGHT;
        return Pan.NONE;
    }

    // ----- SSG -----

    /** the SSG section's registers; the OPN family keeps them in its PSG */
    int[] ssgRegs() {
        return ports()[0];
    }

    private boolean ssgSounding(int s) {
        int[] regs = ssgRegs();
        if (regs == null) return false;
        int level = regs[0x08 + s] & 0x1f;
        boolean tone = (regs[0x07] & (1 << s)) == 0;
        boolean noise = (regs[0x07] & (8 << s)) == 0;
        return (tone || noise) && level > 0;
    }

    private void readSsg(int s, FmDspChannel out) {
        int[] regs = ssgRegs();
        if (regs == null) return;
        int period = (regs[s * 2] & 0xff) | ((regs[s * 2 + 1] & 0x0f) << 8);
        int level = regs[0x08 + s] & 0x1f;
        // a part on the hardware envelope has no readable level, meter it at full scale
        if ((level & 0x10) != 0) level = 15;
        boolean tone = (regs[0x07] & (1 << s)) == 0;
        boolean noise = (regs[0x07] & (8 << s)) == 0;
        boolean sounding = (tone || noise) && level > 0;

        out.name = "SSG";
        out.num = s + 1;
        out.info = (regs[0x08 + s] & 0x10) != 0 ? TrackInfo.SSGEFF : TrackInfo.SSG;
        out.sounding = sounding;
        out.keyOn = sounding && (!prevSsgSoundings[s] || period != prevSsgPeriods[s]);
        prevSsgSoundings[s] = sounding;
        prevSsgPeriods[s] = period;
        // the SSG section runs at half the FM clock input, one tone step is clock/2 / 16
        double freq = period > 0 ? fnumK() * 144 / 4 / (16.0 * period) : 0;
        out.note = period > 0 ? Notes.noteOf(freq) : -1;
        out.detune = Notes.centsOf(freq);
        out.volume = level;
        out.ssgTone = tone;
        out.ssgNoise = noise;
        out.ssgNoiseFreq = regs[0x06] & 0x1f;
        // one step of the SSG's 16 level table is 3 dB
        out.amplitude = Math.pow(10, (Math.min(level, 15) - 15) * 3.0 / 20);
        out.pan = Pan.CENTER;
    }
}
