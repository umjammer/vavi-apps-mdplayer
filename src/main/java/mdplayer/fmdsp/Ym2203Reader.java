/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Map;

import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import mdplayer.chips.BaseChip;
import mdplayer.chips.Ym2203Chip;


/**
 * OPN: FM 1-3 with the ch3 slots and SSG 1-3. The chip is mono, every pan shows centre.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class Ym2203Reader extends OpnFmReader {

    @Override
    protected Ym2203Chip chip() {
        return chipRegister.chip(Ym2203Chip.class);
    }

    @Override protected int fmCount() { return 3; }

    @Override protected boolean hasSsg() { return true; }

    @Override protected boolean hasPan() { return false; }

    /** the OPN divides its clock by 72 where the OPNA divides its doubled clock by 144 */
    @Override protected double fnumK() { return 3993600.0 / 72; }

    @Override public String chipName() { return "OPN"; }

    @Override public int priority() { return 40; }

    @Override
    protected BaseChip.Channel fmMute(int ch) {
        // the chip counts FM 1-3 as 0-2 and the ch3 slots as 6-8, which mute with ch3
        return chip().channel(chipId, ch);
    }

    @Override
    protected BaseChip.Channel ssgMute(int s) {
        return chip().channel(chipId, 3 + s);
    }

    /** the chip's channel state, read back once a frame */
    private Map<String, Object> info;

    private int intOf(String key) {
        Object value = info == null ? null : info.get(key);
        return value instanceof Integer i ? i : 0;
    }

    private boolean boolOf(String key) {
        Object value = info == null ? null : info.get(key);
        return value instanceof Boolean b && b;
    }

    @Override
    public void poll() {
        info = null;
        keys = new int[3];
        try {
            info = chip().getInfo(chipId);
        } catch (RuntimeException ignore) {
            return; // the chip exists but the song never loaded it
        }
        if (info == null) return;
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

    private int[] keys = new int[3];

    @Override protected int[] keyOns() { return keys; }

    @Override protected boolean ch3Extended() { return boolOf("ch3ex"); }

    @Override protected int fmFnum(int ch) { return intOf("channels." + ch + ".fnum"); }

    @Override protected int fmBlock(int ch) { return intOf("channels." + ch + ".block"); }

    @Override protected int fmTotalLevel(int ch) { return intOf("channels." + ch + ".totalLevel"); }

    @Override protected Pan fmPan(int ch) { return opnPan(intOf("channels." + ch + ".pan") << 6); }

    @Override protected int exFnum(int x) { return intOf("channels.2.slots." + x + ".fnum"); }

    @Override protected int exBlock(int x) { return intOf("channels.2.slots." + x + ".block"); }

    @Override protected int[] ssgRegs() {
        return info != null && info.get("ssg.register") instanceof int[] r ? r : null;
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

    // the fmgen core decodes the operator registers away, so the voice is read from the shadow
    // the chip wrapper keeps of what the driver wrote; the OPN is single ported
    @Override protected int[][] toneRegs() {
        int[] regs = chip() != null ? chip().fmRegister[chipId] : null;
        return regs != null ? new int[][] {regs} : null;
    }

    private static final int[][] noPorts = {new int[0x100], new int[0x100]};

}
