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
import mdsound.instrument.GigatronInst;


/**
 * Gigatron TTL microcomputer's 4 channel sound, which its rom makes in the video loop.
 * <p>
 * The "registers" are the machine's ram: {@code $0nFA-$0nFF} for channel n (1-4) wavA, wavX,
 * keyL, keyH, oscL, oscH, {@code $0700-$07FF} the wave table and {@code $21} the channel mask.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-24 nsano initial version <br>
 */
public class GigatronChip extends BaseChip {

    public static final int CHANNELS = 4;

    /** bit n: channel n muted */
    private final int[] mask = {0, 0};

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {GigatronInst.class};
    }

    /** @param address the ram address, 16 bit */
    public void write(int chipId, int address, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
            context.mds.write(inst(chipId), chipId, 0, address, data);
        }
    }

    /**
     * {@code clock} (scanlines/s), {@code channelMask}, and {@code channels}, a list of four maps
     * with {@code key}, {@code wavX}, {@code wavA}, {@code servings}, {@code level} and {@code mute}.
     */
    @Override
    public Map<String, Object> getInfo(int chipId) {
        GigatronInst inst = context.mds.inst(GigatronInst.class);
        return inst == null ? Collections.emptyMap() : inst.getView(chipId, "info");
    }

    @Override
    public void setMask(int chipId, int ch, boolean mask, Object... args) {
        GigatronInst inst = context.mds.inst(GigatronInst.class);
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
