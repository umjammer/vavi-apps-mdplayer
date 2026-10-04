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
                }
            }
        }
    }

    // vgm
    public static class DmcChip extends NpNesChip {

        private int dmcMask = 0;

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {DmcInst.class};
        }

        @Override
        public Map<String, Object> getInfo(int chipId) {
            if (nsf == null || nsf.dmc == null || chipId == 1) return Collections.emptyMap();
            return Map.of("register", nsf.dmc.dmc.reg);
        }

        @Override
        protected void setMask(int chipId, int ch, boolean mask, Object... args) {
            if (mask) {
                super.setMask(chipId, ch + 2);
            } else {
                super.resetMask(chipId, ch + 2);
            }
        }

        @Override
        public boolean getMask(int chipId, int ch) {
            return (dmcMask & (1 << ch)) != 0;
        }
    }

    public static class Mmc5Chip extends NpNesChip {

        private int mmc5Mask = 0;

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {Mmc5Inst.class};
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

        @Override
        protected void setMask(int chipId, int ch, boolean mask, Object... args) {
            if (mask) {
                mmc5Mask |= 1 << ch;
                // there is no nsf behind this unless an nsf is what is playing
                if (nsf != null && nsf.mmc5 != null)
                    nsf.mmc5.setMask(mmc5Mask);
            } else  {
                mmc5Mask &= ~(1 << ch);
                if (nsf != null && nsf.mmc5 != null)
                    nsf.mmc5.setMask(mmc5Mask);
            }
        }

        @Override
        public boolean getMask(int chipId, int ch) {
            return (mmc5Mask & (1 << ch)) != 0;
        }
    }

    public static class Vrc6Chip extends NpNesChip {

        private int vrc6Mask = 0;

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {Vrc6Inst.class};
        }

        @Override
        protected void setMask(int chipId, int ch, boolean mask, Object... args) {
            if (mask) {
                if (chipId != 0)
                    return;
                vrc6Mask |= 1 << ch;
                if (nsf != null && nsf.vrc6 != null)
                    nsf.vrc6.setMask(vrc6Mask);
            } else {
                if (chipId != 0)
                    return;
                vrc6Mask &= ~(1 << ch);
                if (nsf != null && nsf.vrc6 != null)
                    nsf.vrc6.setMask(vrc6Mask);
            }
        }

        @Override
        public boolean getMask(int chipId, int ch) {
            return (vrc6Mask & (1 << ch)) != 0;
        }

        @Override
        public Map<String, Object> getInfo(int chipId) {
            if (nsf == null || nsf.vrc6 == null || chipId != 0) return Collections.emptyMap();
            return Map.of("tracksInfo", nsf.vrc6.getTracksInfo()); // TODO not abstracted
        }
    }

    public static class Vrc7Chip extends NpNesChip {

        private int vrc7Mask = 0;

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {Vrc7Inst.class};
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

        @Override
        protected void setMask(int chipId, int ch, boolean mask, Object... args) {
            if (mask) {
                vrc7Mask |= 1 << ch;
                if (nsf != null && nsf.vrc7 != null)
                    nsf.vrc7.setMask(vrc7Mask);
            } else {
                vrc7Mask &= ~(1 << ch);
                if (nsf != null && nsf.vrc7 != null)
                    nsf.vrc7.setMask(vrc7Mask);
            }
        }

        @Override
        public boolean getMask(int chipId, int ch) {
            return (vrc7Mask & (1 << ch)) != 0;
        }
    }

    public static class N163Chip extends NpNesChip {

        private int n163Mask = 0;

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {N160Inst.class};
        }

        @Override
        public Map<String, Object> getInfo(int chipId) {
            if (nsf == null || nsf.n106 == null || chipId != 0) return Collections.emptyMap();
            return Map.of("tracksInfo", nsf.n106.getTracksInfo());
        }

        @Override
        protected void setMask(int chipId, int ch, boolean mask, Object... args) {
            if (mask) {
                if (chipId != 0)
                    return;
                n163Mask |= 1 << ch;
                if (nsf != null && nsf.n106 != null)
                    nsf.n106.setMask(n163Mask);
            } else {
                if (chipId != 0)
                    return;
                n163Mask &= ~(1 << ch);
                if (nsf != null && nsf.n106 != null)
                    nsf.n106.setMask(n163Mask);
            }
        }

        @Override
        public boolean getMask(int chipId, int ch) {
            return (n163Mask & (1 << ch)) != 0;
        }
    }

    // vgm
    public static class FdsChip extends NpNesChip {

        private int fdsMask = 0;

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {FdsInst.class};
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

        @Override
        protected void setMask(int chipId, int ch, boolean mask, Object... args) {
            if (mask) {
                fdsMask |= 1;
                if (nsf != null && nsf.fds != null)
                    nsf.fds.setMask(fdsMask);
            } else {
                fdsMask &= ~1;
                if (nsf != null && nsf.fds != null)
                    nsf.fds.setMask(fdsMask);
            }
        }

        @Override
        public boolean getMask(int chipId, int ch) {
            return (fdsMask & 1) != 0;
        }
    }

    public static class Fme7Chip extends NpNesChip {

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Instrument>[] implementations() {
            return new Class[] {Fme7Inst.class};
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

    private int apuMask = 0;

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {NpNesInst.class};
    }

    // vgm
    @Override
    protected void setMask(int chipId, int ch, boolean mask, Object... args) {
        if (mask) {
            if (chipId == 0) {
                switch (ch) {
                    case 0:
                    case 1:
                        apuMask |= 1 << ch;
                        if (nsf != null && nsf.apu != null)
                            nsf.apu.setMask(apuMask);
                        break;
                    case 2:
                    case 3:
                    case 4:
                        context.chipRegister.chip(DmcChip.class).dmcMask |= 1 << (ch - 2);
                        if (nsf != null && nsf.dmc != null)
                            nsf.dmc.setMask(context.chipRegister.chip(DmcChip.class).dmcMask);
                        break;
                }
            }
            Instrument instrument = context.mds.inst(NpNesInst.class);
            if (instrument == null) return; // the song being played does not use this chip
            instrument.setMask(chipId, ch);
        } else {
            if (chipId == 0) {
                switch (ch) {
                    case 0:
                    case 1:
                        apuMask &= ~(1 << ch);
                        if (nsf != null && nsf.apu != null)
                            nsf.apu.setMask(apuMask);
                        break;
                    case 2:
                    case 3:
                    case 4:
                        context.chipRegister.chip(DmcChip.class).dmcMask &= ~(1 << (ch - 2));
                        if (nsf != null && nsf.dmc != null)
                            nsf.dmc.setMask(context.chipRegister.chip(DmcChip.class).dmcMask);
                        break;
                }
            }
            Instrument instrument = context.mds.inst(NpNesInst.class);
            if (instrument == null) return; // the song being played does not use this chip
            instrument.resetMask(chipId, ch);
        }
    }

    @Override
    public boolean getMask(int chipId, int ch) {
        return (apuMask & (1 << ch)) != 0;
    }

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
