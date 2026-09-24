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
import mdsound.instrument.Msm5232Inst;


/**
 * OKI MSM5232 8 channel tone generator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-24 nsano initial version <br>
 */
public class Msm5232Chip extends BaseChip {

    public static final int CHANNELS = 8;

    /** bit n: voice n muted */
    private final int[] mask = {0, 0};

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {Msm5232Inst.class};
    }

    /** @param port 00-07: voices, 08-0d: envelopes and controls, 10-1f: libvgm's mixer, 20-23: clock */
    public void write(int chipId, int port, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
            context.mds.write(inst(chipId), chipId, 0, port, data);
        }
    }

    /**
     * {@code clock}, {@code control1}/{@code control2} (bit 4 ARM, bit 3-0 2'/4'/8'/16' enable),
     * {@code extVol1}/{@code extVol2} (0..15), and {@code voices}, a list of eight maps with
     * {@code pitch} (the pitch code, -1 before the first key on), {@code keyOn}, {@code noise},
     * {@code egSection} (0 attack, 1 decay, 2 release, -1 off), {@code egVolume} (0..2048) and
     * {@code mute}.
     */
    @Override
    public Map<String, Object> getInfo(int chipId) {
        Msm5232Inst inst = context.mds.inst(Msm5232Inst.class);
        return inst == null ? Collections.emptyMap() : inst.getView(chipId, "info");
    }

    @Override
    public void setMask(int chipId, int ch, boolean mask, Object... args) {
        Msm5232Inst inst = context.mds.inst(Msm5232Inst.class);
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
