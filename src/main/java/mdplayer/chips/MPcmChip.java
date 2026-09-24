/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.lang.System.Logger;

import mdplayer.Common.EnmModel;
import mdsound.Instrument;
import mdsound.Instrument.PcmEnabledInstrument;
import mdsound.instrument.MPcmPPInst;
import mdsound.instrument.X68kMPcmInst;


/**
 * MPcm (X68000).
 * <p>
 * system property
 * <li>{@code mdplayer.variant.mpcm} ... active chip index</li>
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-04-01 nsano initial version <br>
 */
public class MPcmChip extends BaseChip {

    private static final Logger logger = System.getLogger(MPcmChip.class.getName());

    private final boolean[][] mask = {
            {false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false}
    };

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {X68kMPcmInst.class, MPcmPPInst.class};
    }

    /** shared by ZMS and MNDRV, each with its own setting - see {@link mdplayer.chips.Pcm8Chip#activeIndex} */
    @Override
    public int activeIndex(int chipId) {
        return setting.mpcmType(context);
    }

    public void writePcm(int chipId, int bank, int mode, byte[] pcmData, EnmModel model) {
        if (model != EnmModel.VirtualModel)
            return;

        fireEventHappened("led.on", chipId);

        context.mds.inst((Class<PcmEnabledInstrument>) inst(chipId)).writePcm(chipId, pcmData, 0, pcmData.length);
    }

    public void write(int chipId, int port, int addr, int data, EnmModel model) {
        if (model != EnmModel.VirtualModel)
            return;

        fireEventHappened("led.on", chipId);

        if (port == -1 && addr == -1 && data == -1)
            return;
        context.mds.inst(inst(chipId)).write(chipId, port, addr, data);
    }

    @Override
    protected void setMask(int chipId, int ch, boolean mask, Object... args) {
        this.mask[chipId][ch] = mask;
    }

    public void keyOn(int chipId, int ch) {
        switch (context.mds.inst(inst(chipId))) {
            case X68kMPcmInst mpcm -> mpcm.keyOn(chipId, ch);
            case MPcmPPInst mpcmpp -> mpcmpp.keyOn(chipId, ch);
            default -> {assert false;}
        }
    }

    public void keyOff(int chipId, int ch) {
        switch (context.mds.inst(inst(chipId))) {
            case X68kMPcmInst mpcm -> mpcm.keyOff(chipId, ch);
            case MPcmPPInst mpcmpp -> mpcmpp.keyOff(chipId, ch);
            default -> {assert false;}
        }
    }

    public float[] getBaseRate(int chipId) {
        return switch (context.mds.inst(inst(chipId))) {
            case X68kMPcmInst mpcm -> new float[] {mpcm.chips[chipId].rate, mpcm.chips[chipId].base};
            case MPcmPPInst mpcmpp -> new float[] {mpcmpp.chips[chipId].rate, mpcmpp.chips[chipId].base};
            default -> throw new IllegalStateException();
        };
    }

    /** */
    public void writePcm(int chipId, int ch, byte[] mem, byte type, byte orig, int adrsPtr, int size, int start, int end, int count, boolean isMnd, int frq, int n) {
        switch (context.mds.inst(inst(chipId))) {
            case X68kMPcmInst mpcm -> mpcm.writePcm(chipId, ch, mem, type, orig, adrsPtr, size, start, end, count);
            case MPcmPPInst mpcmpp -> {
                if (isMnd) mpcmpp.setFreq(0, ch, frq);
                mpcmpp.setPcm(chipId, ch, mem, type, orig, adrsPtr, size, start, end, count);
            }
            default -> {assert false;}
        }
    }

    public void setFreq(int chipId, int ch, int value) {
        switch (context.mds.inst(inst(chipId))) {
            case X68kMPcmInst mpcm -> mpcm.setFreq(chipId, ch, value);
            case MPcmPPInst mpcmpp -> mpcmpp.setFreq(chipId, ch, value);
            default -> {assert false;}
        }
    }

    public void setPitch(int chipId, int ch, int value) {
        switch (context.mds.inst(inst(chipId))) {
            case X68kMPcmInst mpcm -> mpcm.setPitch(chipId, ch, value);
            case MPcmPPInst mpcmpp -> mpcmpp.setPitch(chipId, ch, value);
            default -> {assert false;}
        }
    }

    public void setVol(int chipId, int ch, int value) {
        switch (context.mds.inst(inst(chipId))) {
            case X68kMPcmInst mpcm -> mpcm.setVol(chipId, ch, value);
            case MPcmPPInst mpcmpp -> mpcmpp.setVol(chipId, ch, value);
            default -> {assert false;}
        }
    }

    public void setPan(int chipId, int ch, int value) {
        switch (context.mds.inst(inst(chipId))) {
            case X68kMPcmInst mpcm -> mpcm.setPan(chipId, ch, value);
            case MPcmPPInst mpcmpp -> mpcmpp.setPan(chipId, ch, value);
            default -> {assert false;}
        }
    }

    public void reset(int chipId) {
        switch (context.mds.inst(inst(chipId))) {
            case X68kMPcmInst mpcm -> mpcm.reset(chipId);
            case MPcmPPInst mpcmpp -> mpcmpp.reset(chipId);
            default -> {assert false;}
        }
    }

    public void setVolTable(int chipId, int type) {
        switch (context.mds.inst(inst(chipId))) {
            case X68kMPcmInst mpcm -> mpcm.setVolTable(chipId, type);
            case MPcmPPInst mpcmpp -> mpcmpp.setVolTable(chipId, type);
            default -> {assert false;}
        }
    }

    public void setVolTable(int chipId, int type, int[] vtbl) {
        switch (context.mds.inst(inst(chipId))) {
            case X68kMPcmInst mpcm -> mpcm.setVolTable(chipId, type, vtbl);
            case MPcmPPInst mpcmpp -> mpcmpp.setVolTable(chipId, type, vtbl);
            default -> {assert false;}
        }
    }
}
