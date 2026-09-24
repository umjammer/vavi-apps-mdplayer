/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.util.Collections;
import java.util.Map;

import mdplayer.Common.EnmModel;
import mdsound.Instrument;
import mdsound.instrument.K005289Inst;


/**
 * Konami 005289 2 channel wavetable sound, of the Bubble System and Nemesis boards.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-24 nsano initial version <br>
 */
public class K005289Chip extends BaseChip {

    public static final int CHANNELS = 2;

    /** bit n: channel n muted */
    private final int[] mask = {0, 0};

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {K005289Inst.class};
    }

    /** @param port 0/1: control A/B, 2/3: LD1/LD2 (latch the 12 bit data), 4/5: TG1/TG2 */
    public void write(int chipId, int port, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
            context.mds.write(inst(chipId), chipId, 0, port, data);
        }
    }

    /** the waveform prom, 0x100 bytes per channel */
    public void writeProm(int chipId, int stAdr, int dataSize, byte[] vgmBuf, int vgmAdr, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
            K005289Inst inst = context.mds.inst(K005289Inst.class);
            if (inst != null) inst.writeProm(chipId, stAdr, vgmBuf, vgmAdr, dataSize);
        }

        dumpData(model, "PROMData", vgmAdr, vgmBuf, dataSize);
    }

    /**
     * {@code clock} and {@code voices}, a list of two maps with {@code freq} (the period less one
     * of each of the wave's 32 steps, in clocks), {@code volume} (0..15), {@code waveform} (0..7),
     * {@code wave} (the 32 steps of it, -8..7) and {@code mute}.
     */
    @Override
    public Map<String, Object> getInfo(int chipId) {
        K005289Inst inst = context.mds.inst(K005289Inst.class);
        return inst == null ? Collections.emptyMap() : inst.getView(chipId, "info");
    }

    @Override
    public void setMask(int chipId, int ch, boolean mask, Object... args) {
        K005289Inst inst = context.mds.inst(K005289Inst.class);
        if (mask) {
            this.mask[chipId] |= 1 << ch;
            if (inst != null) inst.setMask(chipId, ch);
        } else {
            this.mask[chipId] &= ~(1 << ch);
            if (inst != null) inst.resetMask(chipId, ch);
        }
    }

    @Override
    public boolean getMask(int chipId, int ch) {
        return ch < CHANNELS && (mask[chipId] & (1 << ch)) != 0;
    }
}
