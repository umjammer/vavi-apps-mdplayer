/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import mdplayer.ChipRegister;
import mdplayer.chips.BaseChip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackInfo;


/**
 * The NES APU: two pulses, the triangle and the noise on the wide row block, and the delta PCM
 * channel on a PCM row.
 * <p>
 * The registers come from the emulator in two halves, because it splits the APU that way: the
 * pulses at {@code 0x4000-0x4007} in one, the rest at {@code 0x4008-0x4013} in the other. Both
 * carry {@code 0x4015}, the enables, a bit a channel.
 * <ul>
 * <li>pulse 1 and 2: the volume in the low nibble of the first register, the eleven bit timer
 * over the last two</li>
 * <li>the triangle, which has a timer but no volume of its own</li>
 * <li>the noise, which has a volume but runs off a period table rather than a pitch</li>
 * <li>the delta PCM, whose {@code 0x4011} is the level its DAC sits at</li>
 * </ul>
 * A pulse counts {@code 1789773 / (16 * (timer + 1))} Hz and the triangle half that per step, so
 * unlike the sampled chips here these keys are real pitches. The chip is mono.
 * <p>
 * The same APU is reached two ways: a VGM writes to it through the chip the player registers, and
 * an NSF runs it inside its own machine. The registers are the same emulator's either way, so only
 * where to fetch them from differs - {@link NesReader} and {@link NpNesReader} say which.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 * @version 0.01 2026-07-20 nsano split from NesReader for the nsf driver <br>
 */
public abstract class NesApuReader implements FmDspChipReader {

    /** the NTSC CPU clock the APU divides */
    private static final double cpuClock = 1789773;

    /** pulse 1, pulse 2, triangle, noise - the delta PCM is on its own row */
    private static final int CHANNELS = 4;

    /** {@code 0x4015} in the pulse half, which is {@code 0x4000} relative */
    private static final int APU_STATUS = 0x15;

    /** the same register in the other half, which is {@code 0x4008} relative */
    private static final int DMC_STATUS = 0x0d;

    ChipRegister chipRegister;

    /** the pulses, {@code 0x4000} relative */
    private int[] regs;

    /** the triangle, noise and delta PCM, {@code 0x4008} relative */
    private int[] dmc;

    /** what each channel puts out now, from the emulator itself, or null to guess from registers */
    private int[] levels;

    private final boolean[] prevSoundings = new boolean[CHANNELS];
    private final int[] prevTimers = new int[CHANNELS];
    private boolean prevDmcOn;
    private boolean active;
    private boolean dmcActive;

    /** the pulse registers as they are now, or null when this song is not driving this APU */
    protected abstract int[] apuRegisters();

    /** the triangle, noise and delta PCM registers as they are now, or null */
    protected abstract int[] dmcRegisters();

    /** @param ch 0 - 3 for the APU channels, 4 for the delta PCM */
    protected abstract BaseChip.Channel mute(int ch);

    /**
     * What the four APU channels and the delta PCM put out now, in that order, when the source
     * can tell. Without it the volume is read off the registers, which is wrong for a channel on
     * its envelope: there the low nibble is the envelope period, and a decay never shows.
     *
     * @return null when only the registers are known
     */
    protected int[] levels() {
        return null;
    }

    @Override
    public void bind(ChipRegister chipRegister) {
        this.chipRegister = chipRegister;
    }

    @Override
    public void reset() {
        Arrays.fill(prevSoundings, false);
        Arrays.fill(prevTimers, 0);
        prevDmcOn = false;
        active = false;
        dmcActive = false;
        regs = null;
        dmc = null;
        levels = null;
    }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM, Group.PCM);
    }

    @Override
    public int priority() {
        return 92;
    }

    @Override
    public void poll() {
        regs = apuRegisters();
        dmc = dmcRegisters();
        levels = levels();
    }

    @Override
    public boolean active(Group group) {
        if (group == Group.PCM) {
            if (!dmcActive) dmcActive = dmcOn();
            return dmcActive;
        }
        if (!active) {
            for (int ch = 0; ch < CHANNELS && !active; ch++) active = sounding(ch);
        }
        return active;
    }

    /**
     * The enable bit of a channel. The two halves each keep their own copy of {@code 0x4015};
     * a pulse is enabled in the one that owns it and everything else in the other.
     */
    private boolean enabled(int ch) {
        if (ch < 2) {
            return regs != null && (regs[APU_STATUS] & (1 << ch)) != 0;
        }
        return dmc != null && (dmc[DMC_STATUS] & (1 << ch)) != 0;
    }

    private boolean dmcOn() {
        // a sample still left to play, when the emulator says; the enable bit stays up after it ends
        if (levels != null) return enabled(4) && levels[4] > 0;
        return enabled(4);
    }

    /** the first register of a channel, in whichever half holds it */
    private int[] bankOf(int ch) {
        return ch < 2 ? regs : dmc;
    }

    /** the pulses start at 0 in their half; the triangle and noise at 0 and 4 in the other */
    private int baseOf(int ch) {
        return ch < 2 ? ch * 4 : (ch - 2) * 4;
    }

    /** the timer of a pulse or the triangle, eleven bits over two registers */
    private int timerOf(int ch) {
        int[] bank = bankOf(ch);
        int base = baseOf(ch);
        if (bank == null) return 0;
        return (bank[base + 2] & 0xff) | ((bank[base + 3] & 0x07) << 8);
    }

    private int volumeOf(int ch) {
        int[] bank = bankOf(ch);
        if (bank == null) return 0;
        if (levels != null) return levels[ch];
        // the triangle has no volume register, it is either running or not
        if (ch == 2) return enabled(2) ? 15 : 0;
        return bank[baseOf(ch)] & 0x0f;
    }

    private boolean sounding(int ch) {
        if (bankOf(ch) == null || !enabled(ch)) return false;
        if (ch == 3) return volumeOf(3) > 0; // the noise has no timer to check
        return volumeOf(ch) > 0 && timerOf(ch) > 0;
    }

    @Override
    public int channels(Group group) {
        return group == Group.PCM ? 1 : CHANNELS;
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        if (group == Group.PCM) {
            readDmc(out);
            return;
        }
        out.name = "SSG";
        out.num = ch + 1;
        out.info = TrackInfo.SSG;
        out.pan = Pan.CENTER; // the APU is mono
        if (bankOf(ch) == null) return;

        boolean sounding = sounding(ch);
        int timer = ch == 3 ? 0 : timerOf(ch);
        out.sounding = sounding;
        out.keyOn = sounding && (!prevSoundings[ch] || timer != prevTimers[ch]);
        prevSoundings[ch] = sounding;
        prevTimers[ch] = timer;

        int volume = volumeOf(ch);
        out.volume = volume;
        out.amplitude = sounding ? volume / 15.0 : 0;
        out.ssgTone = ch != 3;
        out.ssgNoise = ch == 3;
        // the noise runs off a period table rather than a pitch, so it has no key
        out.pitch(ch == 3 || !sounding || timer == 0 ? 0
                : cpuClock / ((ch == 2 ? 32 : 16) * (timer + 1)));
        // a pulse shows its duty as the tone number, the way an FM part shows its instrument
        out.toneNum = ch < 2 ? (regs[ch * 4] >> 6) & 0x03 : 0;
    }

    private void readDmc(FmDspChannel out) {
        out.name = "PCM";
        out.num = 1;
        out.pan = Pan.CENTER;
        if (dmc == null) return;

        boolean on = dmcOn();
        out.sounding = on;
        out.keyOn = on && !prevDmcOn;
        prevDmcOn = on;
        // the delta PCM plays a sample, so it has a level but no pitch to speak of
        out.note = -1;
        int dac = dmc[0x09] & 0x7f; // 0x4011, the level its DAC sits at
        out.volume = dac;
        out.amplitude = on ? dac / 127.0 : 0;
        out.toneNum = dmc[0x08] & 0x0f; // 0x4010, its rate index
    }

    @Override
    public BaseChip.Channel muteChannel(Group group, int ch) {
        // the delta PCM comes after the four the APU has
        return mute(group == Group.PCM ? 4 : ch);
    }
}
