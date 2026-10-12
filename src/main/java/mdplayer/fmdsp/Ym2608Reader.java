/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.fmdsp;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import mdplayer.chips.BaseChip;
import mdplayer.chips.Ym2608Chip;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackDetail;


/**
 * OPNA: FM 1-6 with the ch3 slots, SSG 1-3, the ADPCM part on the PCM rows and the rhythm part
 * on the drum meter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class Ym2608Reader extends OpnFmReader {

    /** ADPCM delta-N of an unshifted sample, PMD's o4 c - rate ratio 1.0 maps to the o4 c key */
    private static final int adpcmBaseDeltaN = 0x49ba;

    private int prevAdpcmCtrl;
    private boolean adpcmActive;
    private final int[][] prevRhythmVolumes = new int[6][2];
    private int prevRhythmReg;
    private boolean rhythmActive;

    @Override
    protected Ym2608Chip chip() {
        return chipRegister.chip(Ym2608Chip.class);
    }

    @Override protected boolean hasSsg() { return true; }

    @Override public String chipName() { return "OPNA"; }

    @Override public int priority() { return 10; }

    @Override
    public Set<Group> groups() {
        return EnumSet.of(Group.FM, Group.SSG, Group.PCM, Group.RHYTHM);
    }

    @Override
    public void reset() {
        super.reset();
        prevAdpcmCtrl = 0;
        adpcmActive = false;
        Arrays.stream(prevRhythmVolumes).forEach(v -> Arrays.fill(v, 0));
        prevRhythmReg = 0;
        rhythmActive = false;
    }

    @Override
    public boolean active(Group group) {
        return switch (group) {
            case PCM -> {
                if (!adpcmActive) adpcmActive = (adpcmControl() & 0x80) != 0;
                yield adpcmActive;
            }
            case RHYTHM -> {
                if (!rhythmActive) {
                    int reg = rhythmKey();
                    rhythmActive = (reg & 0x80) == 0 && (reg & 0x3f) != 0;
                }
                yield rhythmActive;
            }
            default -> super.active(group);
        };
    }

    @Override
    public int channels(Group group) {
        return group == Group.PCM || group == Group.RHYTHM ? 1 : super.channels(group);
    }

    @Override
    public void read(Group group, int ch, FmDspChannel out) {
        switch (group) {
        case PCM -> readAdpcm(out);
        case RHYTHM -> readRhythm(out);
        default -> super.read(group, ch, out);
        }
    }

    private void readAdpcm(FmDspChannel out) {
        out.name = "ADPCM";
        out.num = 1;
        int ctrl = adpcmControl();
        boolean playing = (ctrl & 0x80) != 0 && (ctrl & 0x01) == 0;
        int deltaN = intOf("adpcm.deltaN");
        int level = intOf("adpcm.level");

        out.sounding = playing;
        out.keyOn = playing && ctrl != prevAdpcmCtrl;
        prevAdpcmCtrl = ctrl;
        // the chip knows the playback rate, not the note the sample means; show the key the rate
        // ratio comes closest to
        out.note = playing && deltaN > 0 ? Notes.noteOfRatio(deltaN / (double) adpcmBaseDeltaN) : -1;
        out.volume = level;
        // the ADPCM level register is linear over 0..255
        out.amplitude = level / 255.0;
        out.pan = opnPan(intOf("adpcm.pan") << 6);
    }

    /**
     * The ADPCM part's registers, which is the panel the original has for that row: the level, the
     * step it plays its sample at, and where in the chip's RAM the sample runs.
     */
    @Override
    public boolean readDetail(Group group, int ch, TrackDetail out) {
        if (group != Group.PCM) return super.readDetail(group, ch, out);
        if (info == null) return false;

        out.header = "VOL DELTA  START    PTR    END";
        out.text = "%3d  %04X %06X %06X %06X".formatted(intOf("adpcm.level"),
                intOf("adpcm.deltaN"), intOf("adpcm.start"), intOf("adpcm.pointer"),
                intOf("adpcm.stop"));
        return true;
    }

    /**
     * A hit shows as the key-on register changing or, via {@link Ym2608Chip#rhythmVolume}, as a
     * voice's cached meter jumping back up.
     */
    private void readRhythm(FmDspChannel out) {
        Ym2608Chip chip = chip();
        int reg = rhythmKey();
        boolean hit = reg != prevRhythmReg && (reg & 0x80) == 0 && (reg & 0x3f) != 0;
        prevRhythmReg = reg;
        for (int v = 0; v < 6; v++) {
            for (int lr = 0; lr < 2; lr++) {
                if (chip.rhythmVolume[chipId][v][lr] > prevRhythmVolumes[v][lr]) hit = true;
                prevRhythmVolumes[v][lr] = chip.rhythmVolume[chipId][v][lr];
            }
        }
        out.keyOn = hit;
        out.sounding = false;
        out.pan = Pan.CENTER;
    }

    @Override
    protected BaseChip.Channel fmMute(int ch) {
        // the chip counts FM 1-6 as 0-5 and the ch3 slots as 9-11
        return chip().channel(chipId, ch < 6 ? ch : 9 + ch - 6);
    }

    @Override
    protected BaseChip.Channel ssgMute(int s) {
        return chip().channel(chipId, 6 + s);
    }

    @Override
    public BaseChip.Channel muteChannel(Group group, int ch) {
        return switch (group) {
            case PCM -> chip().channel(chipId, 12);
            case RHYTHM -> chip().channel(chipId, 13);
            default -> super.muteChannel(group, ch);
        };
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
        keys = new int[6];
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

    // the fmgen core decodes the operator registers away, so the voice is read from the
    // shadow the chip wrapper keeps of what the driver wrote
    @Override protected int[][] toneRegs() { return chip() != null ? chip().register[chipId] : null; }

    private static final int[][] noPorts = {new int[0x100], new int[0x100]};

    /** the ADPCM control register, as the chip has it */
    private int adpcmControl() {
        return intOf("adpcm.control");
    }

    /** the rhythm key register, as the chip has it */
    private int rhythmKey() {
        return intOf("rhythm.key");
    }
}
