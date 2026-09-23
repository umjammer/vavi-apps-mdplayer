/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.util.Objects;

import mdplayer.Common.EnmModel;
import mdsound.Instrument;
import mdsound.instrument.Pcm8PPInst;
import mdsound.instrument.X68kYm2151Inst;
import mdsound.x68sound.X68Sound;


/**
 * PCM8 (X68000).
 * <p>
 * system property
 * <li>{@code mdplayer.variant.pcm8} ... active chip index, {@link #X68SOUND} or {@link #PCM8PP}, or
 * {@link #AUTO} to let the song decide (MDX only, see {@code Pcm8Detector})</li>
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-02-09 nsano initial version <br>
 */
public class Pcm8Chip extends BaseChip {

    /** X68Sound's own PCM8 */
    public static final int X68SOUND = 0;
    /** PCM8PP (PCM8++), the Mercury-Unit driver, a superset of PCM8 */
    public static final int PCM8PP = 1;
    /** whichever of the two the song needs */
    public static final int AUTO = -1;

    // not view
    private final boolean[][] mask = {
            {false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false}
    };

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {X68kYm2151Inst.class, Pcm8PPInst.class};
    }

    /**
     * The driver whose song is playing decides this, not a section of its own: this one chip is
     * shared by ZMS, MDX and RCS, each of which keeps its own setting and registers the instrument
     * to match in its {@code initChips}. Asking the plugin back is what keeps the two agreeing.
     *
     * @see mdplayer.Setting#pcm8Type
     */
    @Override
    public int activeIndex(int chipId) {
        return setting.pcm8Type(context);
    }

    // not view
    public static boolean isFromDF(int v) {
        //noinspection ConstantValue
        return switch (v) {
            case X68Sound.SNDERR_DLL,
                 X68Sound.SNDERR_FUNC -> true;
            default -> true; // original is so
        };
    }

    // not view
    public static boolean isFromPTM(int v) {
        return switch (v) {
            case X68Sound.SNDERR_PCMOUT,
                 X68Sound.SNDERR_TIMER,
                 X68Sound.SNDERR_MEMORY -> true;
            default -> false;
        };
    }

    public void writePcm(int chipId, int bank, int mode, byte[] pcmData, EnmModel model) {
        if (model != EnmModel.VirtualModel)
            return;

        fireEventHappened("led.on", chipId);

        switch (setting.pcm8Type(context)) {
            case 0 -> context.mds.inst(X68kYm2151Inst.class).writePcm(chipId, pcmData, 0, pcmData.length);
            case 1 -> { try { Objects.requireNonNull(context.mds.inst(Pcm8PPInst.class)).writePcm(chipId, pcmData, 0, pcmData.length); } catch (NullPointerException _) {}}
            default -> { assert false; }
        }
    }

    /**
     * IOCS {@code _OPMSET}: an OPM register write, which is what MXDRV drives its own timer with.
     * It is not one of the PCM8 calls below and must not follow {@link #activeIndex}: PCM8PP is a
     * PCM emulator whose {@code write} is a no-op, so routing this there would drop every register
     * the driver writes - including TimerB, whose overflow is the interrupt that makes the song
     * play at all. The OPM is X68Sound's either way, the same one {@link #getPcm} renders.
     */
    public void write(int chipId, int port, int addr, int data, EnmModel model) {
        if (model != EnmModel.VirtualModel)
            return;

        fireEventHappened("led.on", chipId);

        if (port == -1 && addr == -1 && data == -1)
            return;
        context.mds.inst(X68kYm2151Inst.class).write(chipId, port, addr, data);
    }

    public void setMask(int chipId, int ch, boolean mask) {
        this.mask[chipId][ch] = mask;
    }

    //

    /**
     * {@code mode} goes to either driver as it is: PCM8PP reads data format codes {@code $00}-{@code $06}
     * the way PCM8 does (PCM8PP.TEC). The {@code + 0x0800} the C# original added for PCM8PP moved
     * every format up by 8, so ADPCM was played as 16bit PCM - the noise any ordinary PCM8 song made
     * with PCM8PP selected - and it was applied to ZMS here too, which the original never did.
     */
    public void keyOn(int chipId, int ch, int addr, int mode, int len) {
        switch (setting.pcm8Type(context)) {
            case 0 -> context.mds.inst(X68kYm2151Inst.class).pcm8Out(chipId, ch, addr, mode, len);
            case 1 -> { try { Objects.requireNonNull(context.mds.inst(Pcm8PPInst.class)).keyOn(chipId, ch, addr, mode, len); } catch (NullPointerException _) {}}
            default -> { assert false; }
        }
    }

    public void keyOff(int chipId, int ch) {
        switch (setting.pcm8Type(context)) {
            case 0 -> context.mds.inst(X68kYm2151Inst.class).pcm8Out(chipId, ch, 0, 0, 0);
            case 1 -> { try { Objects.requireNonNull(context.mds.inst(Pcm8PPInst.class)).keyOff(chipId, ch); } catch (NullPointerException _) {}}
            default -> { assert false; }
        }
    }

    public void abort(int chipId) {
        switch (setting.pcm8Type(context)) {
            case 0 -> context.mds.inst(X68kYm2151Inst.class).abort(chipId);
            case 1 -> {}
            default -> { assert false; }
        }
    }

    // mxd

    public int getPcm(int chipId, short[] buffer, int offset, int length) {
        return context.mds.inst(X68kYm2151Inst.class).getPcm(chipId, buffer, offset, length);
    }

    public int start(int chipId, int sampleRate, int opmFlag, int adpcmFlag, int betw, int pcmBuf, int late, double rev) {
        return context.mds.inst(X68kYm2151Inst.class).start(chipId, sampleRate, opmFlag, adpcmFlag, betw, pcmBuf, late, rev);
    }

    public void initIocs(int chipId) {
        context.mds.inst(X68kYm2151Inst.class).initIocs(chipId);
    }

    public void opmInt(int chipId, Runnable func) {
        context.mds.inst(X68kYm2151Inst.class).opmInt(chipId, func);
    }

    public int opmWait(int chipId, int wait) {
        return context.mds.inst(X68kYm2151Inst.class).opmWait(chipId, wait);
    }

    public int totalVolume(int chipId, int vol) {
        return context.mds.inst(X68kYm2151Inst.class).totalVolume(chipId, vol);
    }

    public void stop(int chipId) {
        context.mds.inst(X68kYm2151Inst.class).stop(chipId);
    }

    /**
     * IOCS {@code _ADPCMOUT}: {@code mode} is frequency (0-4) * 256 + pan, the same codes PCM8PP
     * takes, with no volume in it. PCM8PP plays it on channel 0 at its original volume 8, as PCM8
     * does the IOCS calls it takes over. The C# original added {@code 0x0c00}, which made 15.6 kHz
     * ADPCM into 8bit PCM.
     */
    public void keyOnAdpcm(int chipId, int addr, int mode, int len) {
        switch (setting.pcm8Type(context)) {
            case 0 -> context.mds.inst(X68kYm2151Inst.class).keyOnAdpcm(chipId, addr, mode, len);
            case 1 -> context.mds.inst(Pcm8PPInst.class).keyOn(chipId, 0, addr, 0x08_0000 | (mode & 0xffff), len);
            default -> {assert false;}
        }
    }

    public void adpcmMod(int chipId, int mode) {
        switch (setting.pcm8Type(context)) {
            case 0 -> context.mds.inst(X68kYm2151Inst.class).adpcmMod(chipId, mode);
            case 1 -> context.mds.inst(Pcm8PPInst.class).keyOff(chipId, 0);
            default -> {assert false;}
        }
    }
}
