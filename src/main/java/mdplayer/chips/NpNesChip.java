/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.lang.System.Logger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import mdplayer.driver.BaseDriver;
import mdplayer.lib.nsf.Nsf;
import mdplayer.driver.nsf.NsfMdDriver;
import mdplayer.driver.BasePlugin;
import mdsound.Instrument;
import mdsound.instrument.NesInst;
import mdsound.instrument.NpNesInst;
import mdsound.instrument.NpNesInst.DmcInst;
import mdsound.instrument.NpNesInst.FdsInst;
import mdsound.instrument.NpNesInst.Fme7Inst;
import mdsound.instrument.NpNesInst.Mmc5Inst;
import mdsound.instrument.NpNesInst.N160Inst;
import mdsound.instrument.NpNesInst.Vrc6Inst;
import mdsound.instrument.NpNesInst.Vrc7Inst;
import mdsound.np.Device.SoundChip;
import mdsound.np.chip.NesVrc7;

import static java.lang.System.getLogger;


/**
 * NpNesChip. (for nsf)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-04-15 nsano initial version <br>
 */
public class NpNesChip extends BaseChip {

    private static final Logger logger = getLogger(NpNesChip.class.getName());

    // TODO
    public Nsf nsf;

    /**
     * Drops the machine the last song ran on. These chips are JVM-wide singletons, so an NSF's
     * machine would otherwise still be here when the next song is a VGM, and a view reading it
     * would show that song's channels playing under this one.
     */
    @Override
    public void init(BasePlugin<? extends BaseDriver> context) {
        super.init(context);
        nsf = null;
    }

    /**
     * Hands the machine the song is running on to every chip of the family, not just this one.
     * <p>
     * Each of the subclasses below is its own singleton with its own {@link #nsf} field, and each
     * reads it to answer {@link #getInfo}, but the plugin has only the one to call this on. Left
     * to itself the APU would be the only chip that ever saw the machine, and the triangle, the
     * noise, the delta PCM and every expansion chip would report nothing at all.
     */
    public void init() {
        if (context.driverVirtual instanceof NsfMdDriver nsfMdDriver) {
            for (Class<? extends mdplayer.Chip> clazz : context.chipRegister.chips()) {
                if (context.chipRegister.chip(clazz) instanceof NpNesChip chip) {
                    chip.nsf = nsfMdDriver.nsf;
                    // the machine is new, and has none of the mutes
                    chip.restoreMutes();
                }
            }
        }
    }

    // vgm
    public static class DmcChip extends NpNesChip {

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {DmcInst.class};
        }

        @Override
        SoundChip device() {
            return nsf.dmc;
        }

        @Override
        public Map<String, Object> getInfo(int chipId) {
            if (nsf == null || nsf.dmc == null || chipId == 1) return Collections.emptyMap();
            return Map.of("register", nsf.dmc.dmc.reg);
        }

    }

    public static class Mmc5Chip extends NpNesChip {

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {Mmc5Inst.class};
        }

        @Override
        SoundChip device() {
            return nsf.mmc5;
        }

        @Override
        public Map<String, Object> getInfo(int chipId) {
            fireEventHappened("led.on", chipId);

            // for nsf
            if (nsf == null || nsf.mmc5 == null || chipId == 1) return Collections.emptyMap();

            byte[] mmc5Regs = new byte[10];

            int[] dat = {0};
            for (int adr = 0x5000; adr < 0x5008; adr++) {
                nsf.mmc5.read(adr, dat);
                mmc5Regs[adr & 0x7] = (byte) dat[0];
            }

            nsf.mmc5.read(0x5010, dat);
            mmc5Regs[8] = (byte) (nsf.mmc5.pcmMode ? 1 : 0);
            mmc5Regs[9] = (byte) nsf.mmc5.pcm;

            return Map.of("register", mmc5Regs);
        }

    }

    public static class Vrc6Chip extends NpNesChip {

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {Vrc6Inst.class};
        }

        @Override
        SoundChip device() {
            return nsf.vrc6;
        }

        @Override
        public Map<String, Object> getInfo(int chipId) {
            if (nsf == null || nsf.vrc6 == null || chipId != 0) return Collections.emptyMap();
            return Map.of("tracksInfo", nsf.vrc6.getTracksInfo()); // TODO not abstracted
        }
    }

    public static class Vrc7Chip extends NpNesChip {

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {Vrc7Inst.class};
        }

        @Override
        SoundChip device() {
            return nsf.vrc7;
        }

        private ChipKeyInfo getVRC7KeyInfo(int chipId) {
            if (nsf == null || nsf.vrc7 == null)
                return null;
            if (chipId != 0)
                return null;

            NesVrc7.ChipKeyInfo ki = nsf.vrc7.getKeyInfo(chipId);

            ChipKeyInfo[] vrc7KeyOnRet = {new ChipKeyInfo(14), new ChipKeyInfo(14)};
            for (int ch = 0; ch < 6; ch++) {
                vrc7KeyOnRet[chipId].on[ch] = ki.on[ch];
                vrc7KeyOnRet[chipId].off[ch] = ki.off[ch];
            }
            return vrc7KeyOnRet[chipId];
        }

        private int[] readVrc7(int chipId) {
            if (nsf == null || nsf.vrc7 == null) return null;
            if (chipId != 0) return null;

            return nsf.vrc7.getRegs();
        }

        @Override
        public Map<String, Object> getInfo(int chipId) {
            Map<String, Object> map = new HashMap<>();
            ChipKeyInfo keyInfo = getVRC7KeyInfo(chipId);
            if (keyInfo != null) map.put("keyInfo", keyInfo);
            int[] register = readVrc7(chipId);
            if (register != null) map.put("register", register);
            return map;
        }

    }

    public static class N163Chip extends NpNesChip {

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {N160Inst.class};
        }

        @Override
        SoundChip device() {
            return nsf.n106;
        }

        @Override
        public Map<String, Object> getInfo(int chipId) {
            if (nsf == null || nsf.n106 == null || chipId != 0) return Collections.emptyMap();
            return Map.of("tracksInfo", nsf.n106.getTracksInfo());
        }

    }

    // vgm
    public static class FdsChip extends NpNesChip {

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {FdsInst.class};
        }

        @Override
        SoundChip device() {
            return nsf.fds;
        }

        @Override
        public Map<String, Object> getInfo(int chipId) {

            // for nsf - guard the chip this actually reads, not the APU next to it
            if (nsf == null || nsf.fds == null || nsf.fds.fds == null) return Collections.emptyMap();
            else if (chipId == 1) return Collections.emptyMap();
            else {
                return NesInst.toInfo(nsf.fds.fds);
            }
        }

    }

    public static class Fme7Chip extends NpNesChip {

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {Fme7Inst.class};
        }

        @Override
        SoundChip device() {
            return nsf.fme7;
        }

        @Override
        public Map<String, Object> getInfo(int chipId) {
            // for nsf
            if (nsf == null || nsf.fme7 == null || chipId == 1) return Collections.emptyMap();

            byte[] s5bRegs = new byte[0x20];
            int[] dat = {0};
            for (int adr = 0x00; adr < 0x20; adr++) {
                nsf.fme7.read(adr, dat);
                s5bRegs[adr] = (byte) dat[0];
            }
            return Map.of("register", s5bRegs);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {NpNesInst.class};
    }

    /** the sound device of the nsf machine this chip is; read only while {@link #nsf} is set */
    SoundChip device() {
        return nsf.apu;
    }

    /** the nsf machine does the muting, each device for its own channels */
    @Override
    protected void applyMute(int chipId, int ch) {
        // there is the one machine, and none unless an nsf is what is playing
        if (chipId != 0 || nsf == null) return;
        SoundChip device = device();
        if (device != null) device.setMask((int) muteMask(chipId));
    }

    // vgm

    @Override
    public Map<String, Object> getInfo(int chipId) {
        Map<String, Object> map = new HashMap<>();
        int volume = getVolume(chipId);
        map.put("volume", volume);
        map.put("register", apuRegisters(chipId));
        map.put("dmcRegister", dmcRegisters(chipId));
        int[] levels = levels(chipId);
        if (levels != null) map.put("levels", levels);
        return map;
    }

    /**
     * What the pulses, the triangle, the noise and the delta PCM are putting out now, in that
     * order, or null when no NSF is playing. The registers alone cannot say: a channel on its
     * envelope keeps only the envelope period there, and a length counter that ran out leaves
     * them as they were.
     */
    private int[] levels(int chipId) {
        if (nsf == null || nsf.apu == null || nsf.apu.apu == null
                || nsf.dmc == null || nsf.dmc.dmc == null || chipId == 1) return null;
        return new int[] {
                nsf.apu.apu.level(0), nsf.apu.apu.level(1),
                nsf.dmc.dmc.level(0), nsf.dmc.dmc.level(1), nsf.dmc.dmc.level(2)
        };
    }

    /**
     * The machine's live pulse registers, {@code 0x4000} relative, or null when no NSF is playing.
     * Unlike {@link #getInfo} this does no volume lookup: a view polling at frame rate has no use
     * for one.
     *
     * vgm
     */
    private int[] apuRegisters(int chipId) {
        if (nsf == null || nsf.apu == null || nsf.apu.apu == null || chipId == 1) return new int[0x20];
        return nsf.apu.apu.reg;
    }

    /**
     * The machine's live triangle, noise and delta PCM registers, {@code 0x4008} relative - the
     * APU proper only carries the two pulses. Null when no NSF is playing.
     */
    private int[] dmcRegisters(int chipId) {
        if (nsf == null || nsf.dmc == null || nsf.dmc.dmc == null || chipId == 1) return new int[0x20];
        return nsf.dmc.dmc.reg;
    }

    // nsf
    private int getVolume(int chip) {
        try {
//logger.log(Level.INFO, "VOL: %d, %d, %d, %d, %d, %d, %d, %d".formatted(
// context.mds.getChipInfo(NpNesInst.class).getTVolume(),
// context.mds.getChipInfo(NpNesInst.DMC.class).getTVolume(),
// context.mds.getChipInfo(NpNesInst.FDS.class).getTVolume(),
// context.mds.getChipInfo(NpNesInst.N160.class).getTVolume(),
// context.mds.getChipInfo(NpNesInst.VRC6.class).getTVolume(),
// context.mds.getChipInfo(NpNesInst.MMC5.class).getTVolume(),
// context.mds.getChipInfo(NpNesInst.FME7.class).getTVolume(),
// context.mds.getChipInfo(NpNesInst.VRC7.class).getTVolume()));
            return switch (chip) {
                case 0 -> context.mds.getChipInfo(NpNesInst.class).getTVolume();
                case 1 -> context.mds.getChipInfo(DmcInst.class).getTVolume();
                case 2 -> context.mds.getChipInfo(FdsInst.class).getTVolume();
                case 3 -> context.mds.getChipInfo(N160Inst.class).getTVolume();
                case 4 -> context.mds.getChipInfo(Vrc6Inst.class).getTVolume();
                case 5 -> context.mds.getChipInfo(Mmc5Inst.class).getTVolume();
                case 6 -> context.mds.getChipInfo(Fme7Inst.class).getTVolume();
                case 7 -> context.mds.getChipInfo(Vrc7Inst.class).getTVolume();
                default -> throw new IllegalArgumentException("Unexpected value: " + chip);
            };
        } catch (RuntimeException e) {
            return -1;
        }
    }

    @Override
    public String getName() {
        return getClass() == NpNesChip.class ? "Apu" : super.getName(); // considered sub-classes
    }
}
