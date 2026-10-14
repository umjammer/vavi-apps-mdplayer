/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import mdplayer.chips.BaseChip;
import mdplayer.chips.K054539Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;


/**
 * Konami K054539: eight sampled channels on the PCM rows.
 * <p>
 * The chip hands over its whole register file, {@code 0x20} bytes a channel:
 * <ul>
 * <li>{@code +0} to {@code +2} the pitch, a 16.16 increment, so {@code 0x10000} is the chip's own
 * rate</li>
 * <li>{@code +3} the volume, which counts the wrong way about - 0 is loudest and 0x40 is -36 dB</li>
 * <li>{@code +5} the pan: 0x11 hard right through 0x18 middle to 0x1f hard left</li>
 * </ul>
 * and {@code 0x22c} says which channels are active, a bit each.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-20 nsano initial version <br>
 */
public class K054539Reader extends PcmSlotReader {

    private static final int CHANNELS = 8;

    /** twenty bytes a channel */
    private static final int STRIDE = 0x20;

    /** the register holding a bit for each active channel */
    private static final int ACTIVE = 0x22c;

    /** the volume register reaches -36 dB here, which is as good as silent on the display */
    private static final double volumeFloor = 0x40;

    /** how far the pan register runs, 0x11 to 0x1f taken as a position from 0 */
    private static final int panMax = 0xe;

    private int[] regs;

    @Override
    public String chipName() {
        return "K054";
    }

    @Override
    public int priority() {
        return 55;
    }

    @Override
    protected int channelCount() {
        return CHANNELS;
    }

    @Override
    protected BaseChip chip() {
        return chipRegister.chip(K054539Chip.class);
    }

    @Override
    public void poll() {
        super.poll();
        regs = info.get("register") instanceof int[] r ? r : null;
    }

    @Override
    protected boolean sounding(int ch) {
        return regs != null && ACTIVE < regs.length && (regs[ACTIVE] & (1 << ch)) != 0;
    }

    @Override
    protected int rateOf(int ch) {
        if (regs == null) return 0;
        int base = STRIDE * ch;
        return regs[base] | (regs[base + 1] << 8) | (regs[base + 2] << 16);
    }

    @Override
    protected void readChannel(int ch, FmDspChannel out) {
        if (regs == null) return;
        int base = STRIDE * ch;
        int delta = rateOf(ch);
        int volume = regs[base + 3] & 0xff;

        // the register attenuates, so turn it back into something that grows with loudness
        out.volume = (int) Math.max(0, volumeFloor - volume);
        out.amplitude = Math.max(0, 1 - volume / volumeFloor);
        out.pan = panOf(regs[base + 5] & 0xff);
        out.note = delta > 0 ? Notes.noteOfRatio(delta / 65536.0) : -1;
    }

    /**
     * The register is a position across the field rather than a distance from its middle: 0x11 is
     * hard right, 0x18 the middle and 0x1f hard left, with 0x81 to 0x8f a second spelling of the
     * same run (DJ Main writes those). Anything else the chip takes as the middle.
     */
    private static Pan panOf(int pan) {
        int position = 0x18 - 0x11;
        if (pan >= 0x11 && pan <= 0x1f) position = pan - 0x11;
        else if (pan >= 0x81 && pan <= 0x8f) position = pan - 0x81;
        // the chip mixes the channel at these weights, which hold the power constant across the run
        return PcmSlotReader.panOf(weight(position), weight(panMax - position));
    }

    /** a pan weight as a whole number, {@link PcmSlotReader#panOf} only taking the two in ratio */
    private static int weight(int position) {
        return (int) (Math.sqrt(position) * 1000);
    }
}
