package mdplayer.lib.mndrv;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.function.BiConsumer;

import mdplayer.lib.zms.Zms.MPCMSt;
import mdplayer.lib.zms.Zms.MPcmInterface;
import mdplayer.emu.common.FMTimer;
import mdplayer.emu.nise68.XMemory;
import mdplayer.lib.mndrv.MnWork.Cw;
import mdplayer.lib.mndrv.MnWork.Dw;
import mdplayer.lib.mndrv.MnWork.P;
import mdplayer.lib.mndrv.MnWork.W;
import vavi.util.compat.QuadConsumer;
import vavi.util.compat.Tuple;

import static java.lang.System.getLogger;


// MnDrv is MXDRV (SHARP X68000 series) for YMF288
public class MnDrv {

    private static final Logger logger = getLogger(MnDrv.class.getName());

    public List<Tuple<String, byte[]>> extendFile = null;

    private MPCMSt[] mpcmSt = {
            new MPCMSt(), new MPCMSt(), new MPCMSt(), new MPCMSt(),
            new MPCMSt(), new MPCMSt(), new MPCMSt(), new MPCMSt(),
            new MPCMSt(), new MPCMSt(), new MPCMSt(), new MPCMSt(),
            new MPCMSt(), new MPCMSt(), new MPCMSt(), new MPCMSt()
    };

    public MnDrv() {
        reg = new Reg();
        ab = new Ab();
        mm = new XMemory();
        comanalyze = new ComAnalyze();
        comcmds = new ComCmds();
        comlfo = new ComLfo();
        comwave = new ComWave();
        devmpcm = new DevMPcm();
        devopm = new DevOpm();
        devopn = new DevOpn();
        devopnemu = new DevOpnEmu();
        devpsg = new DevPsg();
        devpsgemu = new DevPsgEmu();
        devrhy = new DevRhy();
        interrupt = new Interrupt();

        comanalyze.reg = reg;
        comanalyze.ab = ab;
        comanalyze.mm = mm;
        comcmds.reg = reg;
        comcmds.ab = ab;
        comcmds.mm = mm;
        comcmds.mndrv = this;
        comcmds.comlfo = comlfo;
        comlfo.reg = reg;
        comlfo.mm = mm;
        comlfo.devopm = devopm;
        comwave.reg = reg;
        comwave.ab = ab;
        comwave.mm = mm;
        comwave.comlfo = comlfo;
        devmpcm.reg = reg;
        devmpcm.mm = mm;
        devmpcm.mndrv = this;
        devmpcm.comanalyze = comanalyze;
        devmpcm.comcmds = comcmds;
        devmpcm.comlfo = comlfo;
        devmpcm.comwave = comwave;
        devmpcm.devopm = devopm;
        devopm.reg = reg;
        devopm.mm = mm;
        devopm.mndrv = this;
        devopm.comanalyze = comanalyze;
        devopm.comcmds = comcmds;
        devopm.comlfo = comlfo;
        devopm.comwave = comwave;
        devopn.reg = reg;
        devopn.mm = mm;
        devopn.mndrv = this;
        devopn.comanalyze = comanalyze;
        devopn.comcmds = comcmds;
        devopn.comlfo = comlfo;
        devopn.comwave = comwave;
        devopnemu.reg = reg;
        devopnemu.mm = mm;
        devopnemu.mndrv = this;
        devopnemu.comanalyze = comanalyze;
        devopnemu.comcmds = comcmds;
        devopnemu.comlfo = comlfo;
        devopnemu.comwave = comwave;
        devopnemu.devopn = devopn;
        devopnemu.devopm = devopm;
        devpsg.reg = reg;
        devpsg.ab = ab;
        devpsg.mm = mm;
        devpsg.mndrv = this;
        devpsg.comanalyze = comanalyze;
        devpsg.comcmds = comcmds;
        devpsg.comlfo = comlfo;
        devpsg.comwave = comwave;
        devpsg.devopn = devopn;
        devpsgemu.reg = reg;
        devpsgemu.mm = mm;
        devpsgemu.mndrv = this;
        devpsgemu.comanalyze = comanalyze;
        devpsgemu.comcmds = comcmds;
        devpsgemu.comlfo = comlfo;
        devpsgemu.comwave = comwave;
        devpsgemu.devpsg = devpsg;
        devpsgemu.devopm = devopm;
        devrhy.reg = reg;
        devrhy.mm = mm;
        devrhy.mndrv = this;
        devrhy.comcmds = comcmds;
        devrhy.devopn = devopn;
        interrupt.reg = reg;
        interrupt.ab = ab;
        interrupt.mm = mm;
        interrupt.mndrv = this;
        interrupt.devpsg = devpsg;
        interrupt.devopn = devopn;
        interrupt.devopm = devopm;
        interrupt.devmpcm = devmpcm;
    }

    public final Reg reg;
    public final XMemory mm;
    private final ComAnalyze comanalyze;
    private final ComCmds comcmds;
    private final ComLfo comlfo;
    private final ComWave comwave;
    private final DevMPcm devmpcm;
    private final DevOpm devopm;
    private final DevOpn devopn;
    private final DevOpnEmu devopnemu;
    private final DevPsg devpsg;
    private final DevPsgEmu devpsgemu;
    private final DevRhy devrhy;
    private final Interrupt interrupt;
    private final Ab ab;
    private FMTimer timerOPM;
    private FMTimer timerOPN;

    final byte[] vtbl = new byte[128 * 2];
    public MPcmInterface mpcm;

    public QuadConsumer<Integer, Integer, Integer, Integer> ym2608Write;
    public BiConsumer<Integer, Integer> ym2151Write;
    private boolean isRealModel;
    public Runnable stop;

    public void clock() {
        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x20) == 0) {
            timerOPN.timer();
            if ((timerOPN.readStatus() & 3) != 0) interrupt._opn_entry();
        } else {
            timerOPM.timer();
            if ((timerOPM.readStatus() & 3) != 0) interrupt._opm_entry();
        }
    }

    public void init(byte[] data, boolean isRealModel, int sampleRate) {
        timerOPM = new FMTimer(true, null, 4000000, sampleRate);
        timerOPN = new FMTimer(false, null, 8000000, sampleRate);
        interrupt.timerOPM = timerOPM;
        interrupt.timerOPN = timerOPN;

        this.isRealModel = isRealModel;

        int memPtr = 0x03_0000;
        mm.alloc(memPtr + data.length * 2 + 4);
        for (int i = 0; i < data.length; i++) {
            mm.write(memPtr + data.length + i, data[i]);
        }

        // For debugging
        //if (model == enmModel.RealModel) return true;

        // Starting mndrv
        start();

        reg.setD0_B(0x01); // MND Data Transfer
        reg.a1 = memPtr + data.length;
        reg.D1_L = data.length;
        _trap4_entry();
        if (reg.D0_L < 0) {
            stop.run();
            throw new IllegalStateException("trap4: 0x01");
        }
        memPtr += data.length;

        // pcm transfer
        if (extendFile != null && isRealModel) {
            for (Tuple<String, byte[]> stringTuple : extendFile) {
                mm.realloc(memPtr + stringTuple.getItem2().length * 2 + 4);
                // Copy pcm file to x68 memory
                for (int i = 0; i < stringTuple.getItem2().length; i++) {
                    mm.write(memPtr + stringTuple.getItem2().length + i, stringTuple.getItem2()[i]);
                }
                reg.setD0_B(0x02); // PCM Data Transfer
                reg.a1 = memPtr + stringTuple.getItem2().length;
                reg.D1_L = stringTuple.getItem2().length;
                _trap4_entry();
                if (reg.D0_L < 0) {
                    stop.run();
                    throw new IllegalStateException("trap4: 0x02");
                }
                memPtr += stringTuple.getItem2().length;
            }
        }

        mpcmSt = new MPCMSt[] {
                new MPCMSt(), new MPCMSt(), new MPCMSt(), new MPCMSt(),
                new MPCMSt(), new MPCMSt(), new MPCMSt(), new MPCMSt(),
                new MPCMSt(), new MPCMSt(), new MPCMSt(), new MPCMSt(),
                new MPCMSt(), new MPCMSt(), new MPCMSt(), new MPCMSt()
        };

        reg.setD0_B(0x03); // MND begins playing
        _trap4_entry();
        if (reg.D0_L < 0) {
            stop.run();
            throw new IllegalStateException(playError(reg.D0_L, data));
        }
    }

    /**
     * Why {@code __MN_PLAYMUSIC} would not play this data, in words. It answers {@code -1} for
     * data that is not an MND at all and {@code -2} for an MND it cannot handle, and the second
     * is nearly always the version: we play data versions {@value #MNDVERMIN} to
     * {@value #MNDVER} - 1.
     */
    private static String playError(int code, byte[] data) {
        if (code != -2) return "mnd play: not mnd data (mncall $03 returned " + code + ")";
        int version = data.length > 5 ? data[5] & 0xff : 0;
        return "mnd play: mndrv does not handle mnd data version %d (it plays %d to %d)"
                .formatted(version, MNDVERMIN, MNDVER - 1);
    }

    // Trap processing (effectively MPCM control)
    public void trap(int n) {
        if (isRealModel) return;

        int ch = reg.getD0_B() & 0xf;

        switch ((reg.getD0_W() >> 8) & 0xff) {
        case 0x00:
            mpcm.keyOn(ch);
            mpcmSt[ch].keyOn = true;
            break;
        case 0x01:
            mpcm.keyOff(ch);
            mpcmSt[ch].keyOff = true;
            break;
        case 0x02:
            mpcm.writePcm(ch, mpcmSt, mm, reg, n);
            break;
        case 0x04:
            mpcm.setPitch(ch, reg.D1_L);
            mpcmSt[ch].pitch = reg.D1_L;
            break;
        case 0x05:
            mpcm.setVol(ch, reg.getD1_B());
            mpcmSt[ch].volume = reg.getD1_B() & 0xff;
            break;
        case 0x06:
            mpcm.setPan(ch, reg.getD1_B());
            mpcmSt[ch].pan = reg.getD1_B() & 0xff;
            break;
        case 0x80:
            switch (reg.getD0_B()) {
            case 0x02:
                mpcm.reset();
                break;
            case 0x05:
                mpcm.setVolTable(reg.D1_L);
                break;
            }
            break;
        }
    }

    /*
     * MnDrv Music driver
     * Copyright(C)1997,1998,1999,2000 s.Tsuyuzaki
     *
     * reference: SORCERIAN for X680x0  - PROPN
     * Music creative driver            - MCDRV
     * 	    FMP SYSTEM                  - FMP
     * 	    Professional Music Driver   - PMD
     * 	    Z-MUSIC PERFORMANCE MANAGER - ZMSC3
     * 	    MXDRV Music driver          - mxdrv16y
     */

    private static final int MNDVER = 17 + 1;
    /**
     * Oldest MND data version we play. mndrv.x 1.37 itself stops at 2 -- it turns version 1 away
     * with {@code cmpi.b #1,d2 / beq _play_music_ver_err} -- but the v1 container and track table
     * are identical to v2 and the MML grammar differs in only a few opcodes, which
     * {@code ComAnalyze#V1_CMD_MAP} maps back. See {@code MndV1AnalyzerTest} for the evidence.
     */
    private static final int MNDVERMIN = 1;
    private static final String DRVVER = "1.37";

    /**
     * top:
     * .dc.W    $0137
     * .dc.W    $0000
     * .dc.b    '-MnDrv-',0
     *
     * trap 4 entry
     */
    private void _trap4_entry() {
        Reg spReg = new Reg();
        spReg.D1_L = reg.D1_L;
        spReg.D2_L = reg.D2_L;
        spReg.D3_L = reg.D3_L;
        spReg.D4_L = reg.D4_L;
        spReg.D5_L = reg.D5_L;
        spReg.D6_L = reg.D6_L;
        spReg.D7_L = reg.D7_L;
        spReg.a0 = reg.a0;
        spReg.a2 = reg.a2;
        spReg.a3 = reg.a3;
        spReg.a4 = reg.a4;
        spReg.a5 = reg.a5;
        spReg.a6 = reg.a6;

        reg.a6 = _work_top; //  mm.Readint(_work_top);
        mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) | 0x04));
        reg.setD0_W(reg.getD0_W() & 0xff);
        reg.setD0_W(reg.getD0_W() + 1);
        reg.setD0_W(reg.getD0_W() + (int) (short) reg.getD0_W());

        // _trap_table
        switch (reg.getD0_W()) {
        case 2:
            _t_release(); // 00 driver release
            break;
        case 4:
            _t_trans_mnd(); // 01 copy to buffer
            break;
        case 6:
            _t_trans_pcm(); // 02 copy to buffer
            break;
        case 8:
            _t_play_music(); // 03 Start playing
            break;
        case 10:
            _t_pause(); // 04 Pause
            break;
        case 12:
            _t_stop_music(); // 05 Stop playing
            break;
        case 14:
            _t_get_title(); // 06 Title Acquisition
            break;
        case 16:
            _t_get_work(); // 07 System work acquisition
            break;
        case 18:
            _t_get_track_work(); // 08 Get trackwork address
            break;
        case 20:
            _t_get_trwork_size(); //  09 Get track work size
            break;
        case 22:
            _t_set_master_vol(); // 0A Master Volume
            break;
        case 24:
            _t_track_mask(); // 0B Track Mask
            break;
        case 26:
            _t_key_mask(); // 0C Key Control
            break;
        case 28:
            _t_fadeout(); // 0D FADEOUT
            break;
        case 30:
            _t_purge(); // 0E memory purge
            break;
        case 32:
            _t_set_pcmname(); // 0F pcmname set
            break;
        case 34:
            _t_get_pcmname(); // 10 pcmname get
            break;
        case 36:
            _t_chk_pcmname(); // 11 pcmname check
            break;
        case 38:
            _t_get_loopcount(); // 12 __MN_GETLOOPCOUNT
            break;
        case 40:
            _t_intexec(); // 13 __MN_INTEXEC
            break;
        case 42:
            _t_set_subevent(); // 14 __MN_SETSUBEVENT
            break;
        case 44:
            _t_unremove(); // 15 __MN_UNREMOVE
            break;
        case 46:
            _t_get_status(); // 16 __MN_GETSTATUS
            break;
        case 48:
            _t_get_tempo(); // 17 __MN_GETTEMPO
            break;
        case 50:
            _t_nop(); // 18
            break;
        case 52:
            _t_nop(); // 19
            break;
        case 54:
            _t_nop(); // 1A
            break;
        case 56:
            _t_nop(); // 1B
            break;
        case 58:
            _t_nop(); // 1C
            break;
        case 60:
            _t_nop(); // 1D
            break;
        case 62:
            _t_nop(); // 1E
            break;
        case 64:
            _t_nop(); // 1F
            break;
        }

        mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0xfb));
        reg.D1_L = spReg.D1_L;
        reg.D2_L = spReg.D2_L;
        reg.D3_L = spReg.D3_L;
        reg.D4_L = spReg.D4_L;
        reg.D5_L = spReg.D5_L;
        reg.D6_L = spReg.D6_L;
        reg.D7_L = spReg.D7_L;
        reg.a0 = spReg.a0;
        reg.a2 = spReg.a2;
        reg.a3 = spReg.a3;
        reg.a4 = spReg.a4;
        reg.a5 = spReg.a5;
        reg.a6 = spReg.a6;
    }

    private void _t_nop() {
        reg.D0_L = 0xffff_ffff;
    }

    public static final String M_keeptitle = "MnDrv Music driver";

    /**
     * MNCALL 0
     * Uninstall (actually just stops playback and initializes)
     */
    private void _t_release() {
        if (mm.readShort(reg.a6 + Dw.UNREMOVE) != 0) {
            reg.D0_L = 0xffff_ffff;
            return;
        }

        int sp = reg.a1;
        _d_stop_music();
        mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) | 0x20));
        _dev_reset();
        reg.D0_L = 0xffff_ffff;
        SUBEVENT();
        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x40) != 0) {
            reg.setD0_W(0x8001);
            reg.a1 = 0; // M_keeptitle(pc)
            trap(1);
        }

        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x08) != 0) {
            reg.D1_L = 0xffff_ffff; // -1
            reg.setD0_W(0x8001);
            trap(3);
        }

        _vec_release();
        reg.a1 = sp;

        // Residency release process

        reg.D0_L = 0;
    }

    /**
     * MNCALL 1
     * Copy the data to an internal buffer
     * <pre>
     * in	d1 : len
     * 	a1 : pointer
     * </pre>
     */
    private void _t_trans_mnd() {
        int sp = reg.D1_L;
        _d_stop_music();
        _reset_work();
        _dev_reset();
        reg.D0_L = mm.readInt(reg.a6 + Dw.MMLBUFADR);
        if (reg.D0_L != 0) {
            reg.D1_L = reg.D0_L;
            _MCMFREE();
        }

        reg.D1_L = sp;
        if (reg.D1_L == 0) {
            reg.D0_L = 0; // If len is 0, the process ends successfully without copying.
            return;
        }
        if (_MCMALLOC() < 0) {
            // Failed to secure
            reg.D0_L = 0xffff_ffff;
            return;
        }

        sp = reg.a1;
        mm.write(reg.a6 + Dw.MMLBUFADR, reg.D0_L);
        reg.a0 = reg.a1;
        reg.a1 = reg.D0_L;
        reg.D0_L = reg.D1_L;
        HSCOPY();
        reg.a1 = sp;
        reg.D0_L = 0;
    }

    /**
     * MNCALL 2
     * Copy PCM data to internal buffer
     * <pre>
     * in	d1 : len
     * 	a1 : pointer
     * </pre>
     */
    private void _t_trans_pcm() {
        mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0xed));
        if (reg.D1_L != 0) {
            _t_trans_pcm_();
            return;
        }

        reg.D1_L = mm.readInt(reg.a6 + Dw.PCMBUFADR);
        if (reg.D1_L != 0) {
            _MCMFREE();
            mm.write(reg.a6 + Dw.PCMBUFADR, 0);
        }
        reg.D1_L = mm.readInt(reg.a6 + Dw.MPCMWORKADR);
        if (reg.D1_L != 0) {
            _MCMFREE();
            mm.write(reg.a6 + Dw.MPCMWORKADR, 0);
        }
        reg.D0_L = 0;
    }

    private void _t_trans_pcm_() {
        int sp = reg.D1_L;

        reg.D1_L = mm.readInt(reg.a6 + Dw.PCMBUFADR);
        if (reg.D1_L != 0) {
            _MCMFREE();
            mm.write(reg.a6 + Dw.PCMBUFADR, 0);
        }
        reg.D1_L = mm.readInt(reg.a6 + Dw.MPCMWORKADR);
        if (reg.D1_L != 0) {
            _MCMFREE();
            mm.write(reg.a6 + Dw.MPCMWORKADR, 0);
        }
        reg.D1_L = sp;
        if (_MCMALLOC() < 0) {
//_t_trans_pcm_err:
            reg.D0_L = 0xffff_ffff;
            return;
        }

        mm.write(reg.a6 + Dw.PCMBUFADR, reg.D0_L);
        mm.write(reg.a6 + Dw.PCMBUF_ENDADR, reg.D0_L);
        mm.write(reg.a6 + Dw.PCMBUF_ENDADR, mm.readInt(reg.a6 + Dw.PCMBUF_ENDADR) + reg.D1_L);
        sp = reg.a1;
        reg.a0 = reg.a1;
        reg.a1 = reg.D0_L;
        reg.D0_L = reg.D1_L;
        HSCOPY();
        reg.a1 = sp;

        // PCM data analyze
        reg.a0 = mm.readInt(reg.a6 + Dw.PCMBUFADR);
        int vl = mm.readInt(reg.a0);
        reg.a0 += 4;
        if (vl - 0x1a5a_6d61 != 0) {
            _trans_nozpd();
            return;
        }
        vl = mm.readInt(reg.a0);
        reg.a0 += 4;
        if (vl - 0x4450_634d != 0) // 'DPcM'
        {
            _trans_nozpd();
            return;
        }

        reg.a0 += 4;
        reg.D1_L = mm.readInt(reg.a0);
        reg.a0 += 4;
        mm.write(reg.a6 + Dw.ZPDCOUNT, reg.D1_L);
        reg.D7_L = reg.D1_L;
        reg.D1_L = P._pcm_work_size * reg.D1_L;
        if (_MCMALLOC() < 0) {
            reg.D0_L = 0xffff_ffff;
            return;
        }

        mm.write(reg.a6 + Dw.MPCMWORKADR, reg.D0_L);
        reg.a2 = reg.D0_L;
        reg.D7_L -= 1;

//_t_trans_pcm_ana:
        while (true) {
            mm.write(reg.a2, mm.readShort(reg.a0));
            reg.a0 += 2;
            reg.a2 += 2;
            mm.write(reg.a2, mm.readInt(reg.a0));
            reg.a0 += 4;
            reg.a2 += 4;
            reg.D0_L = mm.readInt(reg.a0);
            reg.a0 += 4;
            reg.D0_L += reg.a0;
            mm.write(reg.a2, reg.D0_L);
            reg.a2 += 4;
            mm.write(reg.a2, mm.readInt(reg.a0));
            reg.a0 += 4;
            reg.a2 += 4;
            mm.write(reg.a2, mm.readInt(reg.a0));
            reg.a0 += 4;
            reg.a2 += 4;
            mm.write(reg.a2, mm.readInt(reg.a0));
            reg.a0 += 4;
            reg.a2 += 4;
            mm.write(reg.a2, mm.readInt(reg.a0));
            reg.a0 += 4;
            reg.a2 += 4;
            reg.D0_L = mm.readInt(reg.a0);
            reg.a0 += 4;
            reg.D0_L = mm.readInt(reg.a0);
            reg.a0 += 4;

            byte vb;
            do {
                vb = mm.readByte(reg.a0++);
            } while (vb != 0);

            reg.D0_L = reg.a0;
            byte cf = (byte) (reg.getD0_B() & 1);
            reg.setD0_B((reg.getD0_B() & 0xff) >> 1);
            if (cf != 0) {
                reg.a0 += 1;
            }
            if (reg.getAndDecD7_W() != 0) continue; // break _t_trans_pcm_ana;
            break;
        }
        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x40) != 0) {
            mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) | 0x02));
        }
        reg.D0_L = 0;
    }

    /** */
    private void _trans_nozpd() {
        reg.D1_L = P._pcm_work_size;
        if (_MCMALLOC() < 0) {
            reg.D0_L = 0xffff_ffff;
            return;
        }
        mm.write(reg.a6 + Dw.MPCMWORKADR, reg.D0_L);
        reg.a3 = reg.D0_L;
        reg.D0_L = 0;
        mm.write(reg.a6 + Dw.ZPDCOUNT, reg.D0_L);
        mm.write(reg.a3, (short) reg.getD0_W());
        reg.a3 += 2; // Registration number
        mm.write(reg.a3, (byte) 0xff);
        reg.a3++; // Registration type (assuming ADPCM)
        mm.write(reg.a3, (byte) 0xff);
        reg.a3++; // Original Key
        mm.write(reg.a3, (byte) reg.getD0_B());
        reg.a3++; // attribute
        mm.write(reg.a3, (byte) reg.getD0_B());
        reg.a3++; // reserved
        mm.write(reg.a3, reg.D0_L);
        reg.a3 += 4; // Data Address
        mm.write(reg.a3, reg.D0_L);
        reg.a3 += 4; // Data size
        mm.write(reg.a3, reg.D0_L);
        reg.a3 += 4; // Loop Start Point
        mm.write(reg.a3, reg.D0_L);
        reg.a3 += 4; // Loop End Point
        mm.write(reg.a3, (byte) 1); // Loop Count

        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x40) != 0) {
            mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) | 0x12));
        }
        reg.D0_L = 0;
    }

    /**
     * High-speed data transfer routine
     * in	d0.l	Number of bytes of data
     * a0	Source address (absolute even number)
     * a1	Forwarding address (ditto)
     *
     * from MCDRV.s (MCDRV)
     */
    private void HSCOPY() {
        Reg spReg = new Reg();
        spReg.D1_L = reg.D1_L;
        spReg.D2_L = reg.D2_L;
        spReg.D3_L = reg.D3_L;
        spReg.D4_L = reg.D4_L;
        spReg.D5_L = reg.D5_L;
        spReg.D6_L = reg.D6_L;
        spReg.D7_L = reg.D7_L;
        spReg.a0 = reg.a0;
        spReg.a1 = reg.a1;
        spReg.a2 = reg.a2;
        spReg.a3 = reg.a3;

        reg.D1_L = reg.D0_L;
        if (reg.a1 <= reg.a0) { // break hscopy_rf; //  To forward when source is lower than destination
            if (reg.a1 != reg.a0) { // break hscopy90;

//hscopy_fr:
                reg.setD0_W(reg.getD0_W() & 0x7f); //  Prepare to transfer remaining 128 bytes
                reg.D1_L >>>= 7; //  Prepare to transfer in 128-byte units

//                break hscopy18;
//hscopy18:
//hscopy10:
                while (--reg.D1_L >= 0) { // break hscopy10;
                    for (int i = 0; i < 32; i++) {
                        mm.write(reg.a1, mm.readInt(reg.a0));
                        reg.a0 += 4;
                        reg.a1 += 4;
                    }
                }

//                break hscopy28;
//hscopy28:
//hscopy20:
                while (reg.getAndDecD0_W() != 0) { // break hscopy20;
                    mm.write(reg.a1, mm.readInt(reg.a0));
                    reg.a1++;
                    reg.a0++;
                }
            }
//            break hscopy90; //  Transfer completed
        } else {
//hscopy_rf:
            reg.a0 += reg.D0_L; //  Transfer from behind the block
            reg.a1 += reg.D0_L;
            reg.setD0_W(reg.getD0_W() & 0x7f); //  Prepare to transfer remaining 128 bytes
            reg.D1_L >>>= 7; //  Prepare to transfer in 128-byte units

//            break hscopy68;
//hscopy68:
//hscopy60:
            while (reg.getAndDecD0_W() != 0) { // break hscopy60;
                reg.a1--;
                reg.a0--; //  Copy the remaining 128 bytes
                mm.write(reg.a1, mm.readInt(reg.a0));
            }

//            break hscopy58;
//hscopy58:
//hscopy50:
            while (--reg.D1_L >= 0) { // break hscopy50;
                for (int i = 0; i < 32; i++) {
                    mm.write(reg.a1, mm.readInt(reg.a0));
                    reg.a0 -= 4;
                    reg.a1 -= 4;
                }
            }
        }
//hscopy90:
        reg.D1_L = spReg.D1_L;
        reg.D2_L = spReg.D2_L;
        reg.D3_L = spReg.D3_L;
        reg.D4_L = spReg.D4_L;
        reg.D5_L = spReg.D5_L;
        reg.D6_L = spReg.D6_L;
        reg.D7_L = spReg.D7_L;
        reg.a0 = spReg.a0;
        reg.a1 = spReg.a1;
        reg.a2 = spReg.a2;
        reg.a3 = spReg.a3;
    }

    /**
     * MNCALL 3
     * Analyze the data and start playing
     */
    public void _t_play_music() {
        _d_stop_music();
        _reset_work();
        _dev_reset();

        reg.a0 = mm.readInt(reg.a6 + Dw.MMLBUFADR);
        if (mm.readInt(reg.a0) - 0x4d4e441a != 0) {
//_play_music_error:
            reg.D0_L = -1;
            return;
//_play_music_ver_err:
//            reg.D0_L = -2;
//            return;
        }
//L1:
        // ori.W	//#$700,sr		; Wed Mar 22 06:09 JST 2000 (saori)
        int sr = 0;
        sr |= 0x700; // ?

        reg.D0_L = 0;
        mm.write(reg.a6 + Dw.LFO_FLAG, (byte) reg.getD0_B());
        mm.write(reg.a6 + Dw.DRV_FLAG2, (byte) reg.getD0_B());
        mm.write(reg.a6 + Dw.DRV_FLAG3, (byte) reg.getD0_B());
        mm.write(reg.a6 + Dw.VOL_PTR, reg.D0_L);
        mm.write(reg.a6 + Dw.ENV_PTR, reg.D0_L);
        mm.write(reg.a6 + Dw.WAVE_PTR, reg.D0_L);
        mm.write(reg.a6 + Dw.TITLE_PTR, reg.D0_L);
        mm.write(reg.a6 + Dw.TONE_PTR, reg.D0_L);
        mm.write(reg.a6 + Dw.SEQ_DATA_PTR, reg.D0_L);
        mm.write(reg.a6 + Dw.FADEFLAG, reg.D0_L);
        mm.write(reg.a6 + Dw.MASTER_VOL_FM, reg.D0_L);
        mm.write(reg.a6 + Dw.CH3KOM, (byte) reg.getD0_B());
        mm.write(reg.a6 + Dw.CH3KOS, (byte) reg.getD0_B());
        mm.write(reg.a6 + Dw.CH3MODEM, (byte) reg.getD0_B());
        mm.write(reg.a6 + Dw.CH3MODES, (byte) reg.getD0_B());
        mm.write(reg.a6 + Dw.TEMPO2, (byte) reg.getD0_B());
        mm.write(reg.a6 + Dw.LOOP_COUNTER, (short) reg.getD0_W());
        mm.write(reg.a6 + Dw.VOLMODE, (byte) reg.getD0_B());
        mm.write(reg.a6 + Dw.EMUMODE, (byte) reg.getD0_B());
        mm.write(reg.a6 + Dw.NOISE_M, (byte) reg.getD0_B());
        mm.write(reg.a6 + Dw.NOISE_S, (byte) reg.getD0_B());
        mm.write(reg.a6 + Dw.NOISE_O, (byte) reg.getD0_B());
        mm.write(reg.a6 + Dw.PSGMIX_M, (short) 0x3838);
        mm.write(reg.a6 + Dw.DIV, (short) 0xc0);
        mm.write(reg.a6 + Dw.TEMPO, (byte) 0xc6);
        mm.write(reg.a6 + Dw.MUTE, (byte) 0xff);
        mm.write(reg.a6 + Dw.DRV_STATUS, (byte) (mm.readByte(reg.a6 + Dw.DRV_STATUS) & 0xfe));
        mm.write(reg.a6 + Dw.RHY_TV, (byte) 63);
        mm.write(reg.a6 + Dw.RHY_DAT, (byte) 0);
        mm.write(reg.a6 + Dw.RHY_DAT2, (byte) 0);

        reg.D0_L = 4; // mnd version
        reg.setD2_W(mm.readShort(reg.a0 + (int) (short) reg.getD0_W()) & 0xffff);
        if (reg.getD2_W() == 0) { // break _play_music_error;
            reg.D0_L = -1;
            return;
        }
        if ((reg.getD2_B() & 0xff) >= MNDVER) { // break _play_music_ver_err;
            reg.D0_L = -2;
            return;
        }
        // mndrv.x has `cmpi.b #1,d2 / beq _play_music_ver_err` here; we accept version 1 too and
        // fix up the handful of opcodes it numbers differently in ComAnalyze#_track_ana_fetch.
        if ((reg.getD2_B() & 0xff) < MNDVERMIN) { // break _play_music_ver_err;
            reg.D0_L = -2;
            return;
        }
        mm.write(reg.a6 + Dw.MND_VER, (byte) reg.getD2_B());

        Runnable act = comanalyze::_track_ana_rest_old;
        if ((mm.readByte(reg.a6 + Dw.MND_VER) & 0xff) >= 13) {
            act = comanalyze::_track_ana_rest_new;
        }
        mm.write(reg.a6 + Dw.TRKANA_RESTADR, Ab.dummyAddress); // dummy
        ab.hlTRKANA_RESTADR.remove(reg.a6);
        ab.hlTRKANA_RESTADR.put(reg.a6, act);

        mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) | 0x20));
        if ((reg.D2_L & 0x4000) != 0) {
            mm.write(reg.a6 + Dw.DRV_FLAG2, (byte) 0x80);
        }
        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x1) == 0) {
            if ((reg.getD2_B() & 0xff) < 6) {
                mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0xdf));
            }
            if ((reg.D2_L & 0x8000) != 0) {
                mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0xdf));
            }
        }

        reg.setD0_W(reg.getD0_W() + 2);
        reg.setD3_W(mm.readShort(reg.a0 + (int) (short) reg.getD0_W()) & 0xffff);
        reg.setD0_W(reg.getD0_W() + 2);
        if (reg.getD0_W() - reg.getD3_W() == 0) { // break _play_music_error;
            reg.D0_L = -1;
            return;
        }

        //
        // tone
        //
        reg.D1_L = mm.readInt(reg.a0 + (int) (short) reg.getD0_W());
        reg.setD0_W(reg.getD0_W() + 4);
        // version 1 data with no FM track at all leaves the tone pointer null; taking the normal
        // path would point TONE_PTR at the file header and read VOICENUM out of the version word.
        if (reg.D1_L == 0 && (mm.readByte(reg.a6 + Dw.MND_VER) & 0xff) == 1) {
            mm.write(reg.a6 + Dw.TONE_PTR, 0);
            mm.write(reg.a6 + Dw.VOICENUM, (short) 0);
            reg.a2 = reg.a0;
        } else {
            reg.a2 = reg.a0 + reg.D1_L;
            mm.write(reg.a6 + Dw.TONE_PTR, reg.a2);
            reg.a2 += 4;
            reg.setD4_W(mm.readShort(reg.a2) & 0xffff);
            reg.a2 += 2;
            mm.write(reg.a6 + Dw.VOICENUM, (short) reg.getD4_W());
        }
        //
        // title
        //
        if (reg.getD0_W() - reg.getD3_W() == 0) { // break _play_music_error;
            reg.D0_L = -1;
            return;
        }
        reg.a2 -= reg.a2;
        reg.D1_L = mm.readInt(reg.a0 + (int) (short) reg.getD0_W());
        if (reg.D1_L != 0) {
            reg.a2 = reg.a0 + reg.D1_L;
        }
        mm.write(reg.a6 + Dw.TITLE_PTR, reg.a2);
        reg.setD0_W(reg.getD0_W() + 4);
        if (reg.getD0_W() - reg.getD3_W() == 0) { // break _play_music_error;
            reg.D0_L = -1;
            return;
        }
        //
        // sequence data
        //
        reg.D1_L = mm.readInt(reg.a0 + (int) (short) reg.getD0_W());
        if (reg.D1_L != 0) {
            reg.a2 = reg.a0 + reg.D1_L;
            mm.write(reg.a6 + Dw.SEQ_DATA_PTR, reg.a2);
        }
        reg.setD0_W(reg.getD0_W() + 4);
        if (reg.getD0_W() - reg.getD3_W() != 0) { // break _track_ana;
            //
            // pcm table
            //
            reg.D1_L = mm.readInt(reg.a0 + (int) (short) reg.getD0_W());
            if (reg.D1_L != 0) {
                reg.a2 = reg.a0 + reg.D1_L;
            }
            reg.setD0_W(reg.getD0_W() + 4);
            if (reg.getD0_W() - reg.getD3_W() != 0) { // break _track_ana;
                //
                // wave data
                //
                reg.D1_L = mm.readInt(reg.a0 + (int) (short) reg.getD0_W());
                if (reg.D1_L != 0) {
                    reg.a2 = reg.a0 + reg.D1_L;
                    mm.write(reg.a6 + Dw.WAVE_PTR, reg.a2);
                }
                reg.setD0_W(reg.getD0_W() + 4);
                if (reg.getD0_W() - reg.getD3_W() != 0) { // break _track_ana;
                    //
                    // envelope data
                    //
                    reg.D1_L = mm.readInt(reg.a0 + (int) (short) reg.getD0_W());
                    if (reg.D1_L != 0) {
                        reg.a2 = reg.a0 + reg.D1_L;
                        mm.write(reg.a6 + Dw.ENV_PTR, reg.a2);
                        reg.a2 += 4;
                        reg.setD4_W(mm.readShort(reg.a2) & 0xffff);
                        reg.a2 += 2;
                        mm.write(reg.a6 + Dw.ENVNUM, (short) reg.getD4_W());
                    }
                    reg.setD0_W(reg.getD0_W() + 4);
                    if (reg.getD0_W() - reg.getD3_W() != 0) { // break _track_ana;
                        //
                        // volume table
                        //
                        reg.D1_L = mm.readInt(reg.a0 + (int) (short) reg.getD0_W());
                        if (reg.D1_L != 0) {
                            reg.a2 = reg.a0 + reg.D1_L;
                            mm.write(reg.a6 + Dw.VOL_PTR, reg.a2);
                        }
                        reg.setD0_W(reg.getD0_W() + 4);
                        if (reg.getD0_W() - reg.getD3_W() != 0) { // break _track_ana;
                            //
                            // common command
                            //
                            reg.D1_L = mm.readInt(reg.a0 + (int) (short) reg.getD0_W());
                            if (reg.D1_L != 0) {
                                reg.a2 = reg.a0 + reg.D1_L;
                                _common_analyze();
                            }
                        }
                    }
                }
            }
        }
        //
//_track_ana:
        reg.a2 = mm.readInt(reg.a6 + Dw.SEQ_DATA_PTR);
        reg.a0 = reg.a2;
        reg.setD0_W(mm.readShort(reg.a2) & 0xffff);
        reg.a2 += 2;
        mm.write(reg.a6 + Dw.USE_TRACK, (short) reg.getD0_W());
        reg.a5 = reg.a6 + Dw.TRACKWORKADR;
        reg.setD3_B(reg.getD2_B());

//_track_ana_loop:
        do {
            reg.D1_L = mm.readInt(reg.a2);
            reg.a2 += 4;
            reg.a3 = reg.a0 + reg.D1_L;
            mm.write(reg.a5 + W.dataptr, reg.a3);

//#if DEBUG
            logger.log(Level.DEBUG, "TrackWorkAdr:%x".formatted(reg.a5));
            logger.log(Level.DEBUG, "DataPtr:%x".formatted(reg.a3));
//#endif

            reg.D1_L = 0;
            reg.setD1_B(mm.readByte(reg.a2++) & 0xff);
            mm.write(reg.a5 + W.ch, (byte) reg.getD1_B());
            reg.a3 = Cw._ch_table;
            //mm.write(Reg.a5 + W.dev, mm.readByte(Reg.a3 + reg.getD1_W()));
            mm.write(reg.a5 + W.dev, _ch_table[reg.getD1_W()]);

            reg.setD2_B(mm.readByte(reg.a2++) & 0xff);
            if ((reg.getD3_B() & 0xff) >= 7) {
                mm.write(reg.a5 + W.track_vol, (byte) reg.getD2_B());
            }
            reg.a2 += 2;
            //
            // set track work
            //
            mm.write(reg.a5 + W.len, (byte) 2);
            mm.write(reg.a5 + W.flag2, (byte) (mm.readByte(reg.a5 + W.flag2) | 1));
            mm.write(reg.a5 + W.reverb, (byte) (mm.readByte(reg.a5 + W.reverb) | 0x10));
            mm.write(reg.a5 + W.q, (byte) 0x10);
            mm.write(reg.a5 + W.banktone, (short) 0xff);
            act = comcmds::_atq_16;
            reg.a3 = Ab.dummyAddress;
            mm.write(reg.a5 + W.qtjob, reg.a3);
            ab.hlw_qtjob.remove(reg.a5);
            ab.hlw_qtjob.put(reg.a5, act);
            mm.write(reg.a5 + W.volmode, mm.readByte(reg.a6 + Dw.VOLMODE));

            reg.D4_L = 0;
            mm.write(reg.a5 + W.addkeycode2, (short) reg.getD4_W());
            mm.write(reg.a5 + W.addvolume2, (short) reg.getD4_W());

            _track_init();

            reg.a5 = reg.a5 + W._track_work_size;
            reg.setD0_W(reg.getD0_W() - 1);
        } while (reg.getD0_W() != 0); // break _track_ana_loop;

        //
        reg.D0_L = 0xc0;
        reg.a4 = reg.a6 + Dw.M_BD;

        mm.write(reg.a4++, (byte) reg.getD0_B());
        mm.write(reg.a4++, (byte) reg.getD0_B());
        mm.write(reg.a4++, (byte) reg.getD0_B());
        mm.write(reg.a4++, (byte) reg.getD0_B());
        mm.write(reg.a4++, (byte) reg.getD0_B());
        mm.write(reg.a4++, (byte) reg.getD0_B());

        mm.write(reg.a4++, (byte) reg.getD0_B());
        mm.write(reg.a4++, (byte) reg.getD0_B());
        mm.write(reg.a4++, (byte) reg.getD0_B());
        mm.write(reg.a4++, (byte) reg.getD0_B());
        mm.write(reg.a4++, (byte) reg.getD0_B());
        mm.write(reg.a4++, (byte) reg.getD0_B());

//_timer_start:
        reg.setD0_B(mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0xff);
        reg.setD0_B(reg.getD0_B() & 0b0010_0001);
        if (reg.getD0_B() == 0) { // break _start_opm;

            reg.D7_L = 0;
            reg.D1_L = 0x26; //timer-B
            reg.setD0_B(mm.readByte(reg.a6 + Dw.TEMPO) & 0xff);
            _OPN_WRITE();

            reg.D1_L = 0x29;
            reg.D0_L = 0x83;
            _OPN_WRITE();

            reg.D1_L = 0x27;
            reg.D0_L = 0x3f;
            _OPN_WRITE();

            reg.D0_L = 0x1c;
            reg.setD1_B(mm.readByte(reg.a6 + Dw.DRV_FLAG3) & 0xff);
            reg.setD1_B(reg.getD1_B() & 0xc0);
            if (reg.getD1_B() == 0) {
                reg.D0_L = 0x1d;
            }
            reg.D7_L = 0x03;
            reg.D1_L = 0x10;
            _OPN_WRITE();

//            break _play_exit;
        } else {
//_start_opm:
            reg.D1_L = 0x12;
            reg.setD0_B(mm.readByte(reg.a6 + Dw.TEMPO) & 0xff);
            _OPM_WRITE();

            reg.D1_L = 0x14;
            reg.D0_L = 0x3f;
            _OPM_WRITE();
        }
//_play_exit:
        reg.D0_L = 3;
        SUBEVENT();

        mm.write(reg.a6 + Dw.DRV_STATUS, (byte) 0x80);
        reg.D0_L = 0;
    }

    /** */
    private void _track_init() {
        if ((reg.getD1_B() & 0xff) >= 0xB0) {
            _track_nop();
            return;
        }
        reg.setD1_W(reg.getD1_W() + 1);
        reg.setD1_W(reg.getD1_W() + (int) (short) reg.getD1_W());
        switch (reg.getD1_W()) {
        case 2:
        case 4:
        case 6:
        case 8:
        case 10:
        case 12:
        case 14:
        case 16:
        case 18:
        case 20:
        case 22:
        case 24:
            logger.log(Level.DEBUG, "Track : OPN %d".formatted(reg.getD1_W() / 2));
            _track_opn();
            break;
        case 26:
        case 28:
        case 30:
        case 32:
            _track_nop();
            break;
        case 34:
        case 36:
        case 38:
        case 40:
        case 42:
        case 44:
        case 46:
        case 48:
        case 50:
        case 52:
        case 54:
        case 56:
        case 58:
        case 60:
        case 62:
        case 64:
            _track_nop();
            break;
        case 66:
        case 68:
        case 70:
        case 72:
        case 74:
        case 76:
            logger.log(Level.DEBUG, "Track : Psg %d".formatted((reg.getD1_W() - 64) / 2));
            _track_psg();
            break;
        case 78:
        case 80:
        case 82:
        case 84:
        case 86:
        case 88:
        case 90:
        case 92:
        case 94:
        case 96:
            _track_nop();
            break;
        case 98:
        case 100:
        case 102:
        case 104:
        case 106:
        case 108:
        case 110:
        case 112:
        case 114:
        case 116:
        case 118:
        case 120:
        case 122:
        case 124:
        case 126:
        case 128:
            _track_nop();
            break;
        case 130:
        case 132:
            logger.log(Level.DEBUG, "Track : RHY %d".formatted( (reg.getD1_W() - 128) / 2));
            _track_rhy();
            break;
        case 134:
        case 136:
        case 138:
        case 140:
        case 142:
        case 144:
        case 146:
        case 148:
        case 150:
        case 152:
        case 154:
        case 156:
        case 158:
        case 160:
            _track_nop();
            break;
        case 162:
        case 164:
        case 166:
        case 168:
        case 170:
        case 172:
        case 174:
        case 176:
        case 178:
        case 180:
        case 182:
        case 184:
        case 186:
        case 188:
        case 190:
        case 192:
            _track_nop();
            break;
        case 194:
        case 196:
        case 198:
        case 200:
        case 202:
        case 204:
        case 206:
        case 208:
        case 210:
        case 212:
        case 214:
        case 216:
        case 218:
        case 220:
        case 222:
        case 224:
            _track_nop();
            break;
        case 226:
        case 228:
        case 230:
        case 232:
        case 234:
        case 236:
        case 238:
        case 240:
        case 242:
        case 244:
        case 246:
        case 248:
        case 250:
        case 252:
        case 254:
        case 256:
            _track_nop();
            break;
        case 258:
        case 260:
        case 262:
        case 264:
        case 266:
        case 268:
        case 270:
        case 272:
            logger.log(Level.DEBUG, "Track : OPM %d".formatted((reg.getD1_W() - 256) / 2));
            _track_opm();
            break;
        case 274:
        case 276:
        case 278:
        case 280:
        case 282:
        case 284:
        case 286:
        case 288:
            _track_nop();
            break;
        case 290:
        case 292:
        case 294:
        case 296:
        case 298:
        case 300:
        case 302:
        case 304:
        case 306:
        case 308:
        case 310:
        case 312:
        case 314:
        case 316:
        case 318:
        case 320:
            _track_nop();
            break;
        case 322:
        case 324:
        case 326:
        case 328:
        case 330:
        case 332:
        case 334:
        case 336:
        case 338:
        case 340:
        case 342:
        case 344:
        case 346:
        case 348:
        case 350:
        case 352:
            logger.log(Level.DEBUG, "Track : PCM %d".formatted((reg.getD1_W() - 320) / 2));
            _track_pcm();
            break;
        }
    }

    private void _track_nop() {
    }

    private void _track_rhy() {
        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 1) != 0) return;

        mm.write(reg.a5 + W.flag, (byte) (mm.readByte(reg.a5 + W.flag) | 0x80));
        mm.write(reg.a5 + W.pan_ampm, (byte) 0xc0);

        reg.a3 = Ab.dummyAddress;
        Runnable act = devrhy::_ch_rhythm;
        mm.write(reg.a5 + W.mmljob_adrs, reg.a3);
        ab.hlw_mmljob_adrs.remove(reg.a5);
        ab.hlw_mmljob_adrs.put(reg.a5, act);

        act = devopn::_fm_effect_ycommand;
        mm.write(reg.a5 + W.we_ycom_adrs, reg.a3);
        ab.hlw_we_ycom_adrs.remove(reg.a5);
        ab.hlw_we_ycom_adrs.put(reg.a5, act);

        act = this::_track_nop;
        mm.write(reg.a5 + W.we_tone_adrs, reg.a3);
        ab.hlw_we_tone_adrs.remove(reg.a5);
        ab.hlw_we_tone_adrs.put(reg.a5, act);

        mm.write(reg.a5 + W.we_pan_adrs, reg.a3);
        ab.hlw_we_pan_adrs.remove(reg.a5);
        ab.hlw_we_pan_adrs.put(reg.a5, act);

    }

    private void _track_opn() {
        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 1) != 0) return;

        mm.write(reg.a5 + W.flag, (byte) (mm.readByte(reg.a5 + W.flag) | 0x80));
        reg.D2_L = 16;
        mm.write(reg.a5 + W.vol, (byte) reg.getD2_B());
        mm.write(reg.a5 + W.vol2, (byte) reg.getD2_B());
        mm.write(reg.a5 + W.smask, (byte) 0xf0);
        mm.write(reg.a5 + W.pan_ampm, (byte) 0xc0);

        Runnable act = devopn::_ch_fm_mml_job;
        reg.a3 = Ab.dummyAddress;
        mm.write(reg.a5 + W.mmljob_adrs, reg.a3);
        ab.hlw_mmljob_adrs.remove(reg.a5);
        ab.hlw_mmljob_adrs.put(reg.a5, act);

        act = devopn::_ch_fm_lfo_job;
        mm.write(reg.a5 + W.lfojob_adrs, reg.a3);
        ab.hlw_lfojob_adrs.remove(reg.a5);
        ab.hlw_lfojob_adrs.put(reg.a5, act);

        act = devopn::_ch_fm_softenv_job;
        mm.write(reg.a5 + W.softenv_adrs, reg.a3);
        ab.hlw_softenv_adrs.remove(reg.a5);
        ab.hlw_softenv_adrs.put(reg.a5, act);

        act = devopn::_FM_RR_cut;
        mm.write(reg.a5 + W.rrcut_adrs, reg.a3);
        ab.hlw_rrcut_adrs.remove(reg.a5);
        ab.hlw_rrcut_adrs.put(reg.a5, act);

        act = devopn::_FM_echo;
        mm.write(reg.a5 + W.echo_adrs, reg.a3);
        ab.hlw_echo_adrs.remove(reg.a5);
        ab.hlw_echo_adrs.put(reg.a5, act);

        act = devopn::_fm_keyoff;
        mm.write(reg.a5 + W.keyoff_adrs, reg.a3);
        ab.hlw_keyoff_adrs.remove(reg.a5);
        ab.hlw_keyoff_adrs.put(reg.a5, act);

        act = devopn::_fm_keyoff;
        mm.write(reg.a5 + W.keyoff_adrs2, reg.a3);
        ab.hlw_keyoff_adrs2.remove(reg.a5);
        ab.hlw_keyoff_adrs2.put(reg.a5, act);

        act = devopn::_fm_command;
        mm.write(reg.a5 + W.subcmd_adrs, reg.a3);
        ab.hlw_subcmd_adrs.remove(reg.a5);
        ab.hlw_subcmd_adrs.put(reg.a5, act);

        act = devopn::_fm_note_set;
        mm.write(reg.a5 + W.setnote_adrs, reg.a3);
        ab.hlw_setnote_adrs.remove(reg.a5);
        ab.hlw_setnote_adrs.put(reg.a5, act);

        act = devopn::_init_hlfo;
        mm.write(reg.a5 + W.inithlfo_adrs, reg.a3);
        ab.hlw_inithlfo_adrs.remove(reg.a5);
        ab.hlw_inithlfo_adrs.put(reg.a5, act);

        act = devopn::_fm_effect_ycommand;
        mm.write(reg.a5 + W.we_ycom_adrs, reg.a3);
        ab.hlw_we_ycom_adrs.remove(reg.a5);
        ab.hlw_we_ycom_adrs.put(reg.a5, act);

        act = devopn::_fm_effect_tone;
        mm.write(reg.a5 + W.we_tone_adrs, reg.a3);
        ab.hlw_we_tone_adrs.remove(reg.a5);
        ab.hlw_we_tone_adrs.put(reg.a5, act);

        act = devopn::_fm_effect_pan;
        mm.write(reg.a5 + W.we_pan_adrs, reg.a3);
        ab.hlw_we_pan_adrs.remove(reg.a5);
        ab.hlw_we_pan_adrs.put(reg.a5, act);


        reg.a3 = reg.a5 + W.voltable;
        //Reg.a4 = _fm_volume_table;
        reg.D1_L = 16 - 1;
        int i = 0;
        do {
            mm.write(reg.a3++, _fm_volume_table[i++]);
        } while (reg.getAndDecD1_W() != 0);
        mm.write(reg.a5 + W.volcount, (byte) 16);

    }

    private void _track_psg() {
        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 1) != 0) return;

        mm.write(reg.a5 + W.flag, (byte) (mm.readByte(reg.a5 + W.flag) | 0x80));
        reg.D2_L = 8;
        mm.write(reg.a5 + W.vol, (byte) reg.getD2_B());
        mm.write(reg.a5 + W.vol2, (byte) reg.getD2_B());
        mm.write(reg.a5 + W.e_sw, (byte) (mm.readByte(reg.a5 + W.e_sw) | 0x80));

        Runnable act = devpsg::_ch_psg_mml_job;
        reg.a3 = Ab.dummyAddress;
        mm.write(reg.a5 + W.mmljob_adrs, reg.a3);
        ab.hlw_mmljob_adrs.remove(reg.a5);
        ab.hlw_mmljob_adrs.put(reg.a5, act);

        act = devpsg::_ch_psg_lfo_job;
        mm.write(reg.a5 + W.lfojob_adrs, reg.a3);
        ab.hlw_lfojob_adrs.remove(reg.a5);
        ab.hlw_lfojob_adrs.put(reg.a5, act);

        act = devpsg::_psg_env;
        mm.write(reg.a5 + W.softenv_adrs, reg.a3);
        ab.hlw_softenv_adrs.remove(reg.a5);
        ab.hlw_softenv_adrs.put(reg.a5, act);

        act = this::_track_nop;
        mm.write(reg.a5 + W.rrcut_adrs, reg.a3);
        ab.hlw_rrcut_adrs.remove(reg.a5);
        ab.hlw_rrcut_adrs.put(reg.a5, act);

        act = devpsg::_psg_echo;
        mm.write(reg.a5 + W.echo_adrs, reg.a3);
        ab.hlw_echo_adrs.remove(reg.a5);
        ab.hlw_echo_adrs.put(reg.a5, act);

        act = devpsg::_psg_env_keyoff;
        mm.write(reg.a5 + W.keyoff_adrs, reg.a3);
        ab.hlw_keyoff_adrs.remove(reg.a5);
        ab.hlw_keyoff_adrs.put(reg.a5, act);

        act = devpsg::_psg_keyoff;
        mm.write(reg.a5 + W.keyoff_adrs2, reg.a3);
        ab.hlw_keyoff_adrs2.remove(reg.a5);
        ab.hlw_keyoff_adrs2.put(reg.a5, act);

        act = devpsg::_psg_command;
        mm.write(reg.a5 + W.subcmd_adrs, reg.a3);
        ab.hlw_subcmd_adrs.remove(reg.a5);
        ab.hlw_subcmd_adrs.put(reg.a5, act);

        act = devpsg::_psg_note_set;
        mm.write(reg.a5 + W.setnote_adrs, reg.a3);
        ab.hlw_setnote_adrs.remove(reg.a5);
        ab.hlw_setnote_adrs.put(reg.a5, act);

        act = this::_track_nop;
        mm.write(reg.a5 + W.inithlfo_adrs, reg.a3);
        ab.hlw_inithlfo_adrs.remove(reg.a5);
        ab.hlw_inithlfo_adrs.put(reg.a5, act);

        act = devopn::_fm_effect_ycommand;
        mm.write(reg.a5 + W.we_ycom_adrs, reg.a3);
        ab.hlw_we_ycom_adrs.remove(reg.a5);
        ab.hlw_we_ycom_adrs.put(reg.a5, act);

        act = this::_track_nop;
        mm.write(reg.a5 + W.we_tone_adrs, reg.a3);
        ab.hlw_we_tone_adrs.remove(reg.a5);
        ab.hlw_we_tone_adrs.put(reg.a5, act);
        mm.write(reg.a5 + W.we_pan_adrs, reg.a3);
        ab.hlw_we_pan_adrs.remove(reg.a5);
        ab.hlw_we_pan_adrs.put(reg.a5, act);

        reg.D2_L = 0;
        reg.setD2_B(mm.readByte(reg.a5 + W.ch) & 0xff);
        reg.setD2_B(reg.getD2_B() + 8);
        reg.a3 = Cw._ch_table;
        //reg.getD2_B() = mm.readByte(Reg.a3 + reg.getD2_W());
        reg.setD2_B(_ch_table[reg.getD2_W()] & 0xff);

        reg.a3 = reg.a6 + Dw.SOFTENV_PATTERN;
        reg.a3 = reg.a3 + (int) (short) reg.getD2_W();
        mm.write(reg.a5 + W.psgenv_adrs, reg.a3);

        reg.a3 = reg.a5 + W.voltable;
        //Reg.a4 = _psg_volume_table;
        reg.D1_L = 16 - 1;
        int i = 0;
        do {
            mm.write(reg.a3++, _psg_volume_table[i++]);
        } while (reg.getAndDecD1_W() != 0);
        mm.write(reg.a5 + W.volcount, (byte) 16);
    }

    private void _track_opm() {
        reg.setD2_B(mm.readByte(reg.a6 + Dw.EMUMODE) & 0xff);
        if (reg.getD2_B() != 0) { // break _track_opm_normal;

            boolean _track_opm_psg_emu = false;
            reg.setD2_B(reg.getD2_B() - 3);
            if (reg.getD2_B() != 0) { // break _track_opm_fm7;
                reg.setD2_B(reg.getD2_B() + 1);
                if (reg.getD2_B() != 0) { // break _track_opm_fm6;

                    reg.setD2_B(mm.readByte(reg.a5 + W.dev) & 0xff);
                    if ((reg.getD2_B() & 0xff) >= (4 + 1)) { // break _track_opm_psg_emu;
                        _track_opm_psg_emu = true;
                    }
//                    break _track_opm_fm_emu;
                } else {
//_track_opm_fm6:
                    reg.setD2_B(mm.readByte(reg.a5 + W.dev) & 0xff);
                    if ((reg.getD2_B() & 0xff) >= (5 + 1)) { // break _track_opm_psg_emu;
                        _track_opm_psg_emu = true;
                    }
//                break _track_opm_fm_emu;
                }
            } else {
//_track_opm_fm7:
                reg.setD2_B(mm.readByte(reg.a5 + W.dev) & 0xff);
                if ((reg.getD2_B() & 0xff) >= (6 + 1)) { // break _track_opm_psg_emu;
                    _track_opm_psg_emu = true;
                }
//                break _track_opm_fm_emu;
            }
//_track_opm_fm_emu:
            if (!_track_opm_psg_emu) {
                mm.write(reg.a5 + W.flag3, (byte) (mm.readByte(reg.a5 + W.flag3) | 0x01));
                mm.write(reg.a5 + W.flag, (byte) (mm.readByte(reg.a5 + W.flag) | 0x80));
                reg.D2_L = 16;
                mm.write(reg.a5 + W.vol, (byte) reg.getD2_B());
                mm.write(reg.a5 + W.vol2, (byte) reg.getD2_B());
                mm.write(reg.a5 + W.smask, (byte) 4);
                mm.write(reg.a5 + W.pan_ampm, (byte) 0xc0);

                Runnable act = devopnemu::_ch_fme_mml_job;
                reg.a3 = Ab.dummyAddress;
                mm.write(reg.a5 + W.mmljob_adrs, reg.a3);
                ab.hlw_mmljob_adrs.remove(reg.a5);
                ab.hlw_mmljob_adrs.put(reg.a5, act);

                act = devopnemu::_ch_fme_lfo_job;
                mm.write(reg.a5 + W.lfojob_adrs, reg.a3);
                ab.hlw_lfojob_adrs.remove(reg.a5);
                ab.hlw_lfojob_adrs.put(reg.a5, act);

                act = devopnemu::_fme_command;
                mm.write(reg.a5 + W.subcmd_adrs, reg.a3);
                ab.hlw_subcmd_adrs.remove(reg.a5);
                ab.hlw_subcmd_adrs.put(reg.a5, act);

                act = devopnemu::_fme_note_set;
                mm.write(reg.a5 + W.setnote_adrs, reg.a3);
                ab.hlw_setnote_adrs.remove(reg.a5);
                ab.hlw_setnote_adrs.put(reg.a5, act);

//                break _track_opm_common;
            } else {
//_track_opm_psg_emu:
                mm.write(reg.a5 + W.flag3, (byte) (mm.readByte(reg.a5 + W.flag3) | 0x03));
                mm.write(reg.a5 + W.flag, (byte) (mm.readByte(reg.a5 + W.flag) | 0x80));
                reg.D2_L = 8;
                mm.write(reg.a5 + W.vol, (byte) reg.getD2_B());
                mm.write(reg.a5 + W.vol2, (byte) reg.getD2_B());
                mm.write(reg.a5 + W.smask, (byte) 4);
                mm.write(reg.a5 + W.pan_ampm, (byte) 0xc0);

                Runnable act = devpsgemu::_ch_psge_mml_job;
                reg.a3 = Ab.dummyAddress;
                mm.write(reg.a5 + W.mmljob_adrs, reg.a3);
                ab.hlw_mmljob_adrs.remove(reg.a5);
                ab.hlw_mmljob_adrs.put(reg.a5, act);

                act = devpsgemu::_ch_psge_lfo_job;
                mm.write(reg.a5 + W.lfojob_adrs, reg.a3);
                ab.hlw_lfojob_adrs.remove(reg.a5);
                ab.hlw_lfojob_adrs.put(reg.a5, act);

                act = devpsgemu::_psge_command;
                mm.write(reg.a5 + W.subcmd_adrs, reg.a3);
                ab.hlw_subcmd_adrs.remove(reg.a5);
                ab.hlw_subcmd_adrs.put(reg.a5, act);

                act = devpsgemu::_psge_note_set;
                mm.write(reg.a5 + W.setnote_adrs, reg.a3);
                ab.hlw_setnote_adrs.remove(reg.a5);
                ab.hlw_setnote_adrs.put(reg.a5, act);

//                break _track_opm_common;
            }
        } else {
//_track_opm_normal:
            mm.write(reg.a5 + W.flag, (byte) (mm.readByte(reg.a5 + W.flag) | 0x80));
            reg.D2_L = 16;
            mm.write(reg.a5 + W.vol, (byte) reg.getD2_B());
            mm.write(reg.a5 + W.vol2, (byte) reg.getD2_B());
            mm.write(reg.a5 + W.smask, (byte) 4);
            mm.write(reg.a5 + W.pan_ampm, (byte) 0xc0);

            Runnable act = devopm::_ch_opm_mml_job;
            reg.a3 = Ab.dummyAddress;
            mm.write(reg.a5 + W.mmljob_adrs, reg.a3);
            ab.hlw_mmljob_adrs.remove(reg.a5);
            ab.hlw_mmljob_adrs.put(reg.a5, act);

            act = devopm::_ch_opm_lfo_job;
            mm.write(reg.a5 + W.lfojob_adrs, reg.a3);
            ab.hlw_lfojob_adrs.remove(reg.a5);
            ab.hlw_lfojob_adrs.put(reg.a5, act);

            act = devopm::_opm_command;
            mm.write(reg.a5 + W.subcmd_adrs, reg.a3);
            ab.hlw_subcmd_adrs.remove(reg.a5);
            ab.hlw_subcmd_adrs.put(reg.a5, act);

            act = devopm::_opm_note_set;
            mm.write(reg.a5 + W.setnote_adrs, reg.a3);
            ab.hlw_setnote_adrs.remove(reg.a5);
            ab.hlw_setnote_adrs.put(reg.a5, act);
        }
//_track_opm_common:
        reg.a3 = reg.a5 + W.voltable;
        //reg.a4 = _fm_volume_table;
        reg.D1_L = 16 - 1;
        int i = 0;
        do {
            mm.write(reg.a3++, _fm_volume_table[i++]);
        } while (reg.getAndDecD1_W() > 0);
        mm.write(reg.a5 + W.volcount, (byte) 16);

        Runnable act = devopm::_ch_opm_softenv_job;
        reg.a3 = Ab.dummyAddress;
        mm.write(reg.a5 + W.softenv_adrs, reg.a3);
        ab.hlw_softenv_adrs.remove(reg.a5);
        ab.hlw_softenv_adrs.put(reg.a5, act);

        act = devopm::_OPM_RR_cut;
        mm.write(reg.a5 + W.rrcut_adrs, reg.a3);
        ab.hlw_rrcut_adrs.remove(reg.a5);
        ab.hlw_rrcut_adrs.put(reg.a5, act);

        act = devopm::_OPM_echo;
        mm.write(reg.a5 + W.echo_adrs, reg.a3);
        ab.hlw_echo_adrs.remove(reg.a5);
        ab.hlw_echo_adrs.put(reg.a5, act);

        act = devopm::_opm_keyoff;
        mm.write(reg.a5 + W.keyoff_adrs, reg.a3);
        ab.hlw_keyoff_adrs.remove(reg.a5);
        ab.hlw_keyoff_adrs.put(reg.a5, act);

        act = devopm::_opm_keyoff;
        mm.write(reg.a5 + W.keyoff_adrs2, reg.a3);
        ab.hlw_keyoff_adrs2.remove(reg.a5);
        ab.hlw_keyoff_adrs2.put(reg.a5, act);

        act = devopm::_init_opm_hlfo;
        mm.write(reg.a5 + W.inithlfo_adrs, reg.a3);
        ab.hlw_inithlfo_adrs.remove(reg.a5);
        ab.hlw_inithlfo_adrs.put(reg.a5, act);

        act = devopm::_opm_effect_tone;
        mm.write(reg.a5 + W.we_tone_adrs, reg.a3);
        ab.hlw_we_tone_adrs.remove(reg.a5);
        ab.hlw_we_tone_adrs.put(reg.a5, act);

        act = devopm::_opm_effect_pan;
        mm.write(reg.a5 + W.we_pan_adrs, reg.a3);
        ab.hlw_we_pan_adrs.remove(reg.a5);
        ab.hlw_we_pan_adrs.put(reg.a5, act);

        act = devopm::_opm_effect_ycommand;
        mm.write(reg.a5 + W.we_ycom_adrs, reg.a3);
        ab.hlw_we_ycom_adrs.remove(reg.a5);
        ab.hlw_we_ycom_adrs.put(reg.a5, act);
    }

    private void _track_pcm() {
        mm.write(reg.a5 + W.flag, (byte) (mm.readByte(reg.a5 + W.flag) | 0x80));
        mm.write(reg.a5 + W.pcmmode, (byte) 0xff);
        reg.D2_L = 64;
        mm.write(reg.a5 + W.vol, (byte) reg.getD2_B());
        mm.write(reg.a5 + W.vol2, (byte) reg.getD2_B());
        mm.write(reg.a5 + W.pan_ampm, (byte) 3);

        Runnable act = devmpcm::_ch_mpcm_mml_job;
        reg.a3 = Ab.dummyAddress;
        mm.write(reg.a5 + W.mmljob_adrs, reg.a3);
        ab.hlw_mmljob_adrs.remove(reg.a5);
        ab.hlw_mmljob_adrs.put(reg.a5, act);

        act = devmpcm::_ch_mpcm_lfo_job;
        mm.write(reg.a5 + W.lfojob_adrs, reg.a3);
        ab.hlw_lfojob_adrs.remove(reg.a5);
        ab.hlw_lfojob_adrs.put(reg.a5, act);

        act = devmpcm::_ch_mpcm_softenv_job;
        mm.write(reg.a5 + W.softenv_adrs, reg.a3);
        ab.hlw_softenv_adrs.remove(reg.a5);
        ab.hlw_softenv_adrs.put(reg.a5, act);

        act = this::_track_nop;
        mm.write(reg.a5 + W.rrcut_adrs, reg.a3);
        ab.hlw_rrcut_adrs.remove(reg.a5);
        ab.hlw_rrcut_adrs.put(reg.a5, act);

        act = devmpcm::_mpcm_echo;
        mm.write(reg.a5 + W.echo_adrs, reg.a3);
        ab.hlw_echo_adrs.remove(reg.a5);
        ab.hlw_echo_adrs.put(reg.a5, act);

        act = devmpcm::_mpcm_keyoff;
        mm.write(reg.a5 + W.keyoff_adrs, reg.a3);
        ab.hlw_keyoff_adrs.remove(reg.a5);
        ab.hlw_keyoff_adrs.put(reg.a5, act);

        act = devmpcm::_mpcm_keyoff2;
        mm.write(reg.a5 + W.keyoff_adrs2, reg.a3);
        ab.hlw_keyoff_adrs2.remove(reg.a5);
        ab.hlw_keyoff_adrs2.put(reg.a5, act);

        act = devmpcm::_mpcm_command;
        mm.write(reg.a5 + W.subcmd_adrs, reg.a3);
        ab.hlw_subcmd_adrs.remove(reg.a5);
        ab.hlw_subcmd_adrs.put(reg.a5, act);

        act = devmpcm::_mpcm_note_set;
        mm.write(reg.a5 + W.setnote_adrs, reg.a3);
        ab.hlw_setnote_adrs.remove(reg.a5);
        ab.hlw_setnote_adrs.put(reg.a5, act);

        act = this::_track_nop;
        mm.write(reg.a5 + W.inithlfo_adrs, reg.a3);
        ab.hlw_inithlfo_adrs.remove(reg.a5);
        ab.hlw_inithlfo_adrs.put(reg.a5, act);

        act = devopm::_opm_effect_ycommand;
        mm.write(reg.a5 + W.we_ycom_adrs, reg.a3);
        ab.hlw_we_ycom_adrs.remove(reg.a5);
        ab.hlw_we_ycom_adrs.put(reg.a5, act);

        act = devmpcm::_mpcm_effect_tone;
        mm.write(reg.a5 + W.we_tone_adrs, reg.a3);
        ab.hlw_we_tone_adrs.remove(reg.a5);
        ab.hlw_we_tone_adrs.put(reg.a5, act);

        act = devmpcm::_mpcm_effect_pan;
        mm.write(reg.a5 + W.we_pan_adrs, reg.a3);
        ab.hlw_we_pan_adrs.remove(reg.a5);
        ab.hlw_we_pan_adrs.put(reg.a5, act);

        reg.a3 = reg.a5 + W.voltable;
        //Reg.a4 = _mpcm_vol_table;
        reg.D1_L = 16 - 1;
        int i = 0;
        do {
            mm.write(reg.a3++, _mpcm_vol_table[i++]);
        } while (reg.getAndDecD1_W() > 0);
        mm.write(reg.a5 + W.volcount, (byte) 16);

    }

    /** */
    private static final byte[] _fm_volume_table = {
            0x2A, 0x28, 0x25, 0x22, 0x20, 0x1D, 0x1A, 0x18, 0x15, 0x12, 0x10, 0x0D, 0x0A, 0x08, 0x05, 0x02
    };
    private static final byte[] _mpcm_vol_table = {
            0x01, 0x08, 0x10, 0x18, 0x20, 0x28, 0x30, 0x38, 0x40, 0x48, 0x50, 0x58, 0x60, 0x68, 0x70, 0x78
    };
    private static final byte[] _psg_volume_table = {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
    };

    /**
     * Common Command Analysis
     */
    private void _common_analyze() {
        do {
            reg.D4_L = 0;
            reg.setD4_B(mm.readByte(reg.a2++) & 0xff);
            if (reg.getD4_B() == 0) return;
            reg.setD4_W(reg.getD4_W() + (int) (short) reg.getD4_W());

            switch (reg.getD4_W()) {
            case 2:
                _common_timer(); //   01: Drive timer
                break;
            case 4:
                _common_lfotimer(); //   02: LFO Timer
                break;
            case 6:
                _common_psgtimer(); //   03: PSG Timer
                break;
            case 8:
                _common_tempo(); //   04: Tempo
                break;
            case 10:
                _common_tie(); //   05: Ties Operation
                break;
            case 12:
                _common_lfo(); //   06: LFO Operation
                break;
            case 14:
                _common_clock(); //   07: Whole note clock
                break;
            case 16:
                _common_volume(); //   08: Volume Mode
                break;
            case 18:
                _common_opnemu(); //   09: OPN Emulation Mode
                break;
            case 20:
                _common_q_mode(); //   0A: @q mode
                break;
            case 22:
                _common_env_mode(); //   0B: env mode
                break;
            case 24:
            case 26:
            case 28:
            case 30:
                _common_nop();
                break;
            }
        } while (true);
    }

    private void _common_nop() {
        // rts
    }

    /**
     * TEMPO Drive Timer
     */
    private void _common_timer() {
        reg.setD4_B(mm.readByte(reg.a2++) & 0xff);
        if (reg.getD4_B() != 0) {
            mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0xdf));
            return;
        }
        mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) | 0x20));
    }

    /**
     * LFO Driven Timer
     */
    private void _common_lfotimer() {
        reg.setD4_B(mm.readByte(reg.a2++) & 0xff);
        if (reg.getD4_B() != 0) {
            mm.write(reg.a6 + Dw.DRV_FLAG3, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG3) | 0x80)); //  use TIMER-a
            return;
        }
        mm.write(reg.a6 + Dw.DRV_FLAG3, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG3) & 0x7f)); //  use TIEMR-B
    }

    /**
     * Psg Drive Timer
     */
    private void _common_psgtimer() {
        reg.setD4_B(mm.readByte(reg.a2++) & 0xff);
        if (reg.getD4_B() != 0) {
            mm.write(reg.a6 + Dw.DRV_FLAG3, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG3) | 0x40)); //  use TIMER-a
            return;
        }
        mm.write(reg.a6 + Dw.DRV_FLAG3, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG3) & 0xbf)); //  use TIEMR-B
    }

    /**
     * Initial Tempo
     */
    private void _common_tempo() {
        mm.write(reg.a6 + Dw.TEMPO, mm.readByte(reg.a2++));
    }

    /**
     * Tie Operation Mode
     */
    private void _common_tie() {
        mm.write(reg.a6 + Dw.DRV_FLAG2, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG2) & 0x3f));
        reg.setD4_B(mm.readByte(reg.a2++) & 0xff);
        if (reg.getD4_B() == 0) return;
        reg.setD4_B(reg.getD4_B() - 1);
        if (reg.getD4_B() == 0) {
            mm.write(reg.a6 + Dw.DRV_FLAG2, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG2) | 0x80));
            return;
        }
        mm.write(reg.a6 + Dw.DRV_FLAG2, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG2) | 0x40));
    }

    /**
     * LFO Operation Mode
     */
    private void _common_lfo() {
        reg.setD4_B(mm.readByte(reg.a2++) & 0xff);
        if (reg.getD4_B() == 0) return;
        reg.setD4_B(reg.getD4_B() - 1);
        if (reg.getD4_B() == 0) {
            mm.write(reg.a6 + Dw.LFO_FLAG, (byte) (mm.readByte(reg.a6 + Dw.LFO_FLAG) | 0x80));
            return;
        }
        mm.write(reg.a6 + Dw.LFO_FLAG, (byte) (mm.readByte(reg.a6 + Dw.LFO_FLAG) | 0xc0));
    }

    /**
     * Whole Note Clock
     */
    private void _common_clock() {
        reg.setD0_W(mm.readShort(reg.a2) & 0xffff);
        reg.a2 += 2;
        mm.write(reg.a6 + Dw.DIV, (short) reg.getD0_W());
    }

    /**
     * Relative Volume Mode
     */
    private void _common_volume() {
        reg.setD4_B(mm.readByte(reg.a2++) & 0xff);
        if (reg.getD4_B() != 0) {
            mm.write(reg.a6 + Dw.VOLMODE, (byte) 0xff);
        }
    }

    /**
     * OPN Emulation Mode
     */
    private void _common_opnemu() {
        mm.write(reg.a6 + Dw.EMUMODE, mm.readByte(reg.a2++));
    }

    /**
     * Quantize Mode
     */
    private void _common_q_mode() {
        mm.write(reg.a6 + Dw.DRV_FLAG2, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG2) & 0xef));
        if (mm.readByte(reg.a2++) == 0) return;
        mm.write(reg.a6 + Dw.DRV_FLAG2, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG2) | 0x10));
    }

    /**
     * Software Envelope
     */
    private void _common_env_mode() {
        mm.write(reg.a6 + Dw.DRV_FLAG2, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG2) & 0xfb));
        if (mm.readByte(reg.a2++) == 0) return;
        mm.write(reg.a6 + Dw.DRV_FLAG2, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG2) | 0x04));
    }

    /**
     * MNCALL 4
     * Playback pause
     */
    public void _t_pause() {
        if ((mm.readByte(reg.a6 + Dw.DRV_STATUS) & 0x20) != 0) return;
        int v = mm.readByte(reg.a6 + Dw.DRV_STATUS) & 0x40;
        mm.write(reg.a6 + Dw.DRV_STATUS, (byte) (mm.readByte(reg.a6 + Dw.DRV_STATUS) ^ 0x40));
        if (v != 0) {
            _pause_release();
            return;
        }
        reg.D0_L = 4;
        SUBEVENT();
        _all_mute();
    }

    private void _all_mute() {
        reg.a5 = reg.a6 + Dw.TRACKWORKADR;
        reg.setD7_W(mm.readShort(reg.a6 + Dw.USE_TRACK) & 0xffff);

//_pause_loop:
        while (reg.getD7_W() != 0) {
            int x;
            if ((byte) ((mm.readByte(reg.a5 + W.ch) & 0xff) - 0xa0) >= 0) { x = 9; } // break L9;
            else if ((byte) ((mm.readByte(reg.a5 + W.ch) & 0xff) - 0x80) >= 0) { x = 2; } // break L2;
            else if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x1) != 0) { x = 9; } // break L9;
            else if ((byte) ((mm.readByte(reg.a5 + W.ch) & 0xff) - 0x40) >= 0) { x = 9; } // break L9;
            else if ((byte) ((mm.readByte(reg.a5 + W.ch) & 0xff) - 0x20) >= 0) { x = 1; } // break L1;
            else {
                reg.D4_L = 0x7f;
                devopn._FM_F2_set();
                x = 9;
//                break L9;
            }

            if (x == 1) {
//L1:
                reg.D0_L = 0;
                devpsg._psg_volume_set2();
//                break L9;
            }
            if (x == 2) {
//L2:
                reg.D4_L = 0x7f;
                devopm._OPM_F2_set();
            }
//L9:
            reg.a5 = reg.a5 + W._track_work_size;
            reg.setD7_W(reg.getD7_W() - 1);
        }

        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x40) == 0) return;

        reg.setD0_W(0x01ff);
        trap(1);
        reg.setD0_W(0x13ff);
        trap(1);
    }

    private void _pause_release() {
        reg.a5 = reg.a6 + Dw.TRACKWORKADR;
        reg.setD7_W(mm.readShort(reg.a6 + Dw.USE_TRACK) & 0xffff);
//_pause_rel_loop:
        do {
            int x;
            if ((mm.readByte(reg.a5 + W.ch) & 0xff) >= 0x80) { x = 2; } // break L2b;
            else if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x1) != 0) { x = 9; } // break L9b;
            else if ((mm.readByte(reg.a5 + W.ch) & 0xff) >= 0x40) { x = 9; } // break L9b;
            else if ((mm.readByte(reg.a5 + W.ch) & 0xff) >= 0x20) { x = 1; } // break L1b;
            else {
                reg.setD4_B(mm.readByte(reg.a5 + W.vol) & 0xff);
                reg.setD4_B(reg.getD4_B() + mm.readByte(reg.a6 + Dw.MASTER_VOL_FM) & 0xff);
                if ((byte) reg.getD4_B() >= 0) {
                    x = 8;
//                    break L8b;
                } else {
                    reg.D4_L = 0x7f;
                    x = 8;
                }
            }

            if (x == 8) {
//L8b:
                devopn._FM_F2_set();
                x = 9;
//                break L9b;
            }

            if (x == 1) {
//L1b:
                reg.setD0_B(mm.readByte(reg.a5 + W.e_ini) & 0xff);
                devpsg._psg_volume_set2();
                x = 9;
//                break L9b;
            }
            if (x == 2) {
//L2b:
                reg.setD4_B(mm.readByte(reg.a5 + W.vol) & 0xff);
                reg.setD4_B(reg.getD4_B() + mm.readByte(reg.a6 + Dw.MASTER_VOL_FM) & 0xff);
                if ((byte) reg.getD4_B() < 0) {
                    reg.D4_L = 0x7f;
                }
                devopm._OPM_F2_set();
                x = 9;
            }
            if (x == 9) {
//L9b:
                reg.a5 = reg.a5 + W._track_work_size;
                reg.setD7_W(reg.getD7_W() - 1);
            }
        } while (reg.getD7_W() != 0); // break _pause_rel_loop;
    }

    /**
     * MNCALL 5
     * Stop playing
     */
    private void _t_stop_music() {
        reg.D0_L = 2;
        SUBEVENT();
        _d_stop_music();
    }

    public void _d_stop_music() {
        mm.write(reg.a6 + Dw.DRV_STATUS, (byte) 0x20);
        mm.write(reg.a6 + Dw.TEMPO, (byte) 0);

        reg.setD0_B(mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0xff);
        reg.setD0_B(reg.getD0_B() & 0b0010_0001);
        if (reg.getD0_B() == 0) { // break _t_stop_music_opm;

            reg.D7_L = 0;
            reg.D1_L = 0x27;
            reg.D0_L = 0x30;
            _OPN_WRITE();
            reg.D1_L = 0x28;
            reg.D0_L = 0x80;
            _OPN_WRITE();
            _all_mute();
//            return;
        } else {
//_t_stop_music_opm:
            reg.D0_L = 0x30;
            reg.D1_L = 0x14;
            _OPN_WRITE();
            _all_mute();
        }
    }

    /**
     * MNCALL 6
     * Get a pointer to the title data
     * out	a1 : title pointer
     */
    private void _t_get_title() {
        reg.a1 = mm.readInt(reg.a6 + Dw.TITLE_PTR);
        reg.D0_L = reg.a1;
    }

    /**
     * MNCALL 7
     * Get work address
     * out	a1 : work pointer
     */
    private void _t_get_work() {
        reg.a1 = _work_top; //  mm.Readint(_work_top);
        reg.D0_L = reg.a1;
    }

    /**
     * MNCALL 8
     * Get trackwork address
     * out	a1 : work pointer
     */
    private void _t_get_track_work() {
        reg.a1 = reg.a6 + Dw.TRACKWORKADR;
        reg.D0_L = reg.a1;
    }

    /**
     * MNCALL 9
     * Get track work size
     * out	d0 : work pointer
     */
    private void _t_get_trwork_size() {
        reg.D0_L = W._track_work_size; //  Dw._trackworksize;
    }

    /**
     * MNCALL $0A
     * Master Volume Settings
     * in	d1 : device
     * d2 : volume
     */
    private void _t_set_master_vol() {
        reg.D6_L = 1;
        reg.setD6_B((reg.getD6_B() & 0xff) + (reg.getD1_B() & 0xff));
        reg.setD6_W(reg.getD6_W() + (int) (short) reg.getD6_W());
        switch (reg.getD6_W()) {
        case 2:
            mm.write(reg.a6 + Dw.MASTER_VOL_FM, (byte) reg.getD2_B());
            break;
        case 4:
            mm.write(reg.a6 + Dw.MASTER_VOL_PSG, (byte) reg.getD2_B());
            break;
        case 6:
            mm.write(reg.a6 + Dw.MASTER_VOL_RHY, (byte) reg.getD2_B());
            break;
        case 8:
            mm.write(reg.a6 + Dw.MASTER_VOL_PCM, (byte) reg.getD2_B());
            break;
        }
    }

    /**
     * MNCALL $0B
     * Track Mask
     * in	d1 : track
     */
    private void _t_track_mask() {
        reg.a5 = reg.a6 + Dw.TRACKWORKADR;
        reg.setD1_W(reg.getD1_W() - 1);
//L1:
        do {
            if (reg.getD1_B() == 0) break; // L2;
            reg.a5 = reg.a5 + W._track_work_size; // Dw._trackworksize;
        } while (reg.getAndDecD1_W() != 0); // break L1;

//L2:
        int v = mm.readByte(reg.a5 + W.flag2) & 0x80;
        mm.write(reg.a5 + W.flag2, (byte) (mm.readByte(reg.a5 + W.flag2) ^ 0x80));
        if (v != 0) return;

        if ((mm.readByte(reg.a5 + W.ch) & 0xff) >= 0x40) return;
        if ((mm.readByte(reg.a5 + W.ch) & 0xff) >= 0x20) {
            devpsg._psg_env_keyoff();
            return;
        }
        devopn._fm_keyoff();
    }

    /**
     * MNCALL $0C
     * Key Control
     * in	d1 : enabe / disable
     */
    private void _t_key_mask() {
        if (reg.getD1_B() != 0) {
            mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) | 0x80));
        } else {
            mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x7f));
        }
    }

    /**
     * MNCALL $0D
     * FADEOUT
     */
    private void _t_fadeout() {
        mm.write(reg.a6 + Dw.DRV_STATUS, (byte) (mm.readByte(reg.a6 + Dw.DRV_STATUS) | 0x10));
        mm.write(reg.a6 + Dw.FADEFLAG, (byte) 1);
        mm.write(reg.a6 + Dw.FADESPEED, (byte) 7);
        mm.write(reg.a6 + Dw.FADESPEED_WORK, (byte) 7);
        mm.write(reg.a6 + Dw.FADECOUNT, (byte) 3);

        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x01) != 0) return;

        while ((mm.readByte(reg.a6 + Dw.DRV_STATUS) & 0x01) != 0) ;

        // ori.W	//#$700,sr

        reg.D7_L = 3;
        reg.D1_L = 0x10;
        reg.D0_L = 0x1c;
        _OPN_WRITE();
    }

    /**
     * MNCALL $0E
     * memory purge
     */
    private void _t_purge() {
        reg.D1_L = mm.readInt(reg.a6 + Dw.MMLBUFADR);
        if (reg.D1_L != 0) {
            _MCMFREE();
            mm.write(reg.a6 + Dw.MMLBUFADR, 0);
        }
        reg.D1_L = mm.readInt(reg.a6 + Dw.PCMBUFADR);
        if (reg.D1_L != 0) {
            _MCMFREE();
            mm.write(reg.a6 + Dw.PCMBUFADR, 0);
        }
        reg.D1_L = mm.readInt(reg.a6 + Dw.MPCMWORKADR);
        if (reg.D1_L != 0) {
            _MCMFREE();
            mm.write(reg.a6 + Dw.MPCMWORKADR, 0);
        }
    }

    /**
     * MNCALL $0F
     * name set
     */
    private void _t_set_pcmname() {
        reg.a2 = reg.a1;
        reg.a0 = reg.a6 + Dw.ADPCMNAME;
        reg.D0_L = 96 - 1;
//L1:
        do {
            mm.write(reg.a0, mm.readByte(reg.a1));
            reg.a0++;
            reg.a1++;
            reg.D0_L--;
        } while (reg.D0_L != 0);  // Maybe not.
        // dbeq	d0,1b
        if (reg.D0_L != 0) {
            reg.a0--;
            mm.write(reg.a0, (byte) 0);
        }
        reg.a1 = reg.a2;
    }

    /**
     * MNCALL $10
     * name get
     */
    private void _t_get_pcmname() {
        reg.a1 = reg.a6 + Dw.ADPCMNAME;
        reg.D0_L = reg.a1;
    }


    /**
     * MNCALL $11
     * name check
     */
    private void _t_chk_pcmname() {
        int sp = reg.a1;
        reg.a0 = reg.a6 + Dw.ADPCMNAME;
        reg.D7_L = 0xdf;
        reg.D0_L = 96 - 1;

//cmpapn10:
        boolean f;
        do {
            reg.setD1_B(mm.readByte(reg.a1++) & 0xff);
            if (reg.getD1_B() == 0) { // break cmpapn50;
                f = true;
                break;
            }
            if ((byte) reg.getD1_B() < 0) {
                reg.setD3_B(0xff);
            }
            reg.setD2_B(mm.readByte(reg.a0++) & 0xff);
            if ((byte) reg.getD2_B() < 0) {
                reg.setD4_B(0xff);
            }
            reg.setD3_B((reg.getD3_B() & 0xff) ^ (reg.getD4_B() & 0xff));
            if (reg.getD3_B() != 0) { // break cmpadpn80;
                f = false;
                break;
            }
            if ((byte) reg.getD4_B() >= 0) {
                if ((reg.getD1_B() & 0xff) >= 0x61) {
                    if ((reg.getD1_B() & 0xff) <= 0x7a) {
                        reg.setD1_B((reg.getD1_B() & 0xff) & (reg.getD7_B() & 0xff));
                        reg.setD2_B((reg.getD2_B() & 0xff) & (reg.getD7_B() & 0xff));
                    }
                }
            }
            f = ((reg.getD2_B() & 0xff) - (reg.getD1_B() & 0xff) != 0);
            reg.D0_L--;
        } while (reg.D0_L != 0 && f); // break cmpapn10;

        if (!f) { // break cmpadpn80;
//            break cmpapn60;
//cmpapn50:
            if (mm.readByte(reg.a0) != 0) { // break cmpadpn80;
                reg.D0_L = 0xffffffff;
            } else {
//cmpapn60:
                reg.D0_L = 0;
//                break cmpadpn90;
            }
        } else {
//cmpadpn80:
            reg.D0_L = 0xffffffff;
        }
//cmpadpn90:
        reg.a1 = sp;
    }

    /**
     * MNCALL $12
     * get loopcount
     */
    private void _t_get_loopcount() {
        reg.setD0_W(mm.readShort(reg.a6 + Dw.LOOP_COUNTER) & 0xffff);
        reg.D0_L = (short) reg.getD0_W();
    }

    /**
     * MNCALL $13
     * set intexec
     */
    private void _t_intexec() {
        reg.setD1_W(mm.readShort(reg.a6 + Dw.INTEXECNUM) & 0xffff);
        reg.D0_L = 0xffffffff;
        if (reg.getD1_W() - 8 != 0) {
            reg.setD1_W(reg.getD1_W() + (int) (short) reg.getD1_W());
            reg.setD1_W(reg.getD1_W() + (int) (short) reg.getD1_W());
            reg.a5 = reg.a6 + Dw.INTEXECBUF;
            mm.write(reg.a5 + (int) (short) reg.getD1_W(), reg.a1);
            mm.write(reg.a6 + Dw.INTEXECNUM, (short) ((mm.readShort(reg.a6 + Dw.INTEXECNUM) & 0xffff) + 1));
            reg.D0_L = 0;
        }
    }

    /**
     * MNCALL $14
     * set subevent
     */
    private void _t_set_subevent() {
        reg.setD1_W(reg.getD1_W() + (int) (short) reg.getD1_W());
        switch (reg.getD1_W()) {
        case 0:
//_t_se_mode0:
            SRCHSSEID();
            reg.a1 = reg.a0;
            return;
        case 2:
            boolean pl = SRCHSSEID();
            if (pl) break;
            reg.a0 = reg.a6 + Dw.SUBEVENTADR;
            reg.D0_L = 8 - 1;
            int v;
            do {
                v = mm.readInt(reg.a0);
                reg.a0 += 4;
                reg.D0_L--;
                if (v == 0) break;
            } while (reg.D0_L != 0);

            if (v != 0) break;
            reg.a0 -= 4;
            mm.write(reg.a0, reg.a1);
            mm.write(reg.a0 + 8 * 4, reg.D2_L);
            mm.write(reg.a6 + Dw.SUBEVENTNUM, (short) ((mm.readShort(reg.a6 + Dw.SUBEVENTNUM) & 0xffff) + 1));
            reg.D0_L = 0;
            return;
        case 4:
            pl = SRCHSSEID();
            if (!pl) return;
            mm.write(reg.a6 + Dw.SUBEVENTNUM, (short) ((mm.readShort(reg.a6 + Dw.SUBEVENTNUM) & 0xffff) - 1));
            mm.write(reg.a0, 0);
            mm.write(reg.a0 - 8 * 4, 0);
//L1:
            return;
        }

        reg.D0_L = 0xffff_ffff;
    }

    /**
     * from MCDRV
     *
     * ID Search
     * in	d2.l	ID Name
     * out	d1.l	address
     * a0	Address containing ID
     */
    private boolean SRCHSSEID() {
        reg.a0 = reg.a6 + Dw.SUBEVENTID;
        reg.D0_L = 0xffff_ffff; // -1
        reg.D1_L = 8 - 1;

//srchsseid10:
        boolean flg = false;
        do {
            int v = mm.readInt(reg.a0);
            reg.a0 += 4;
            if (reg.D2_L - v == 0) {
                flg = true;
                break;
            }
            reg.D1_L--;
        } while (reg.D1_L != 0);
        if (flg) { // break srchsseid90; // Not Found

            reg.a0 -= 4;
            reg.D0_L = mm.readInt(reg.a0 - 8 * 4); // Enter the address and finish
        }
//srchsseid90:
        return reg.D0_L >= 0; // To ccr?
    }

    /**
     * form MCDRV
     * Sub-event call
     * in	d0	Event number
     */
    public void SUBEVENT() {
        Reg spReg = new Reg();
        spReg.D6_L = reg.D6_L;
        spReg.D7_L = reg.D7_L;
        spReg.a0 = reg.a0;
        spReg.a2 = reg.a2;

        reg.a2 = reg.a6 + Dw.SUBEVENTADR;
        reg.setD7_W(mm.readShort(reg.a6 + Dw.SUBEVENTNUM) & 0xffff);
//subevent20:
        while (reg.getAndDecD7_W() > 0) { // break subevent10;
//subevent10:
            do {
                reg.D6_L = mm.readInt(reg.a2);
                if (reg.D6_L != 0) break;
                reg.setD7_W(reg.getD7_W() - 1);
            } while (reg.getD7_W() > 0); // 0=Not registered

            if (reg.D6_L == 0) break; // subevent90;
            reg.a0 = reg.D6_L;
            // actSUBEVENT(Reg.a0); //  Subroutine Call
        }
//subevent90:
        reg.D6_L = spReg.D6_L;
        reg.D7_L = spReg.D7_L;
        reg.a0 = spReg.a0;
        reg.a2 = spReg.a2;
    }

    /**
     * MNCALL $15
     * unremove
     */
    private void _t_unremove() {
        mm.write(reg.a6 + Dw.UNREMOVE, (short) ((mm.readShort(reg.a6 + Dw.UNREMOVE) & 0xffff) + (short) reg.getD1_W()));
        reg.setD0_W(mm.readShort(reg.a6 + Dw.UNREMOVE) & 0xffff);
        reg.D0_L = (short) reg.getD0_W();
    }

    /**
     * MNCALL $16
     * get status
     */
    private void _t_get_status() {
        reg.setD0_W((short) ((mm.readByte(reg.a6 + Dw.DRV_STATUS) & 0xff) * 0x100));
        reg.setD0_B(mm.readByte(reg.a6 + Dw.DRV_FLAG2) & 0xff);
        reg.D0_L = (reg.D0_L >>> 16) | (reg.D0_L << 16);
        reg.setD0_W((short) ((mm.readByte(reg.a6 + Dw.DRV_FLAG3) & 0xff) * 0x100));
        reg.setD0_B(mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0xff);
    }

    /**
     * MNCALL $17
     * get tempo
     */
    private void _t_get_tempo() {
        reg.D0_L = 0;
        reg.setD0_W(mm.readShort(reg.a6 + Dw.DIV) & 0xffff);
        reg.D0_L = (reg.D0_L >>> 16) | (reg.D0_L << 16);
        reg.setD0_B(mm.readByte(reg.a6 + Dw.TEMPO) & 0xff);
    }

    /** */
    public static final byte[] _ch_table = {
            0x00, 0x01, 0x02, 0x00, 0x01, 0x02,              // FM MASTER
            0x00, 0x01, 0x02, 0x00, 0x01, 0x02,              // FM SLAVE
            0x00, 0x00, 0x00, 0x00,
            // 0x10 ~
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 0x20 ~
            0x00, 0x01, 0x02, 0x00, 0x01, 0x02, 0x00, 0x00,  // Psg
            0x00, 0x10, 0x20, 0x30, 0x40, 0x50, 0x00, 0x00,  // for softenv
            // 0x30 ~
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 0x40 ~
            0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  // RHYTHM
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 0x50 ~
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 0x60 ~
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 0x70 ~
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 0x80 ~
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,  // OPM
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 0x90 ~
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 0xA0 ~
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,  // PCM
            0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            // 0xB0 ~
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,  // PCM
            0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            // 0xC0 ~
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 0xD0 ~
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 0xE0 ~
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // 0xF0 ~
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    /**
     * OPN3 Write
     * <p>
     * break d6-d7/a0/a3
     */
    public void _OPN_WRITE4() {
        reg.D6_L = 0;
        reg.setD6_B(mm.readByte(reg.a5 + W.ch) & 0xff);
        reg.setD1_B((reg.getD1_B() & 0xff) + (mm.readByte(reg.a5 + W.dev) & 0xff));
        _OPN_WRITE_();
    }

    public void _OPN_WRITE3() {
        reg.a0 = 0xecc0c1;
        reg.D6_L = 0;
        if ((mm.readByte(reg.a5 + W.ch) & 0xff) < 6) {
            _opn_write_direct();
            return;
        }
        reg.a0 += 8;
        reg.setD6_W(0x200);
        _opn_write_direct();
    }

    public void _OPN_WRITE() {
        reg.D6_L = reg.D7_L;
        _OPN_WRITE_();
    }

    public void _OPN_WRITE2() {
        reg.D6_L = 0;
        reg.setD6_B(mm.readByte(reg.a5 + W.ch) & 0xff);
        _OPN_WRITE_();
    }

    private void _OPN_WRITE_() {
        reg.setD6_W(reg.getD6_W() + (int) (short) reg.getD6_W());
        reg.setD6_W(_reg_table[reg.getD6_W() / 2]);
        reg.a0 = reg.D5_L;
        reg.D5_L = 0xecc0c0;
        reg.setD5_B((reg.getD5_B() & 0xff) + (reg.getD6_B() & 0xff));
        int a = reg.a0;
        reg.a0 = reg.D5_L;
        reg.D5_L = a;
        _opn_write_direct();
    }

    private void _opn_write_direct() {
        reg.setD6_B(reg.getD1_B());
        reg.a3 = reg.a6 + Dw.REGWORKADR;
        mm.write(reg.a3 + (reg.getD6_W() & 0xffff), (byte) reg.getD0_B());

        //while ((byte)mm.readByte(Reg.a0) < 0) ; //OPN wait?
        //mm.Write(Reg.a0, (byte)reg.getD1_B());
        //logger.log(Level.TRACE, "adr:%x dat:%x".formatted(Reg.a0, reg.getD1_B())));
        //while ((byte)mm.readByte(Reg.a0) < 0) ; //OPN wait?
        //mm.Write(Reg.a0 + 2, (byte)reg.getD0_B());
        //logger.log(Level.TRACE, "adr:%x dat:%x".formatted(Reg.a0+2, reg.getD0_B())));

        switch (reg.a0) {
        case 0xec_c0c1:
//            if ((reg.getD1_B() & 0xff) == 0x27) {
//                logger.log(Level.INFO, "Timer A Write: %02x".formatted(reg.getD0_B() & 0xff));
//            }
            ym2608Write.accept(0, 0, reg.getD1_B(), reg.getD0_B());
            timerOPN.writeReg((byte) reg.getD1_B(), (byte) reg.getD0_B());
            //logger.log(Level.TRACE, "DEV:0 PRT:0 radr:%x rdat:%x".formatted(reg.getD1_B(), reg.getD0_B())));
            //if (reg.getD1_B() < 0x10) {
            //    logger.log(Level.TRACE, "SSG : radr:%x rdat:%x".formatted(reg.getD1_B(), reg.getD0_B())));
            //}
            break;
        case 0xe_cc0c5:
            ym2608Write.accept(0, 1, reg.getD1_B() & 0xff, reg.getD0_B() & 0xff);
            //logger.log(Level.TRACE, "DEV:0 PRT:1 radr:%x rdat:%x".formatted(reg.getD1_B(), reg.getD0_B())));
            break;
        case 0xe_cc0c9:
            ym2608Write.accept(1, 0, reg.getD1_B() & 0xff, reg.getD0_B() & 0xff);
            //logger.log(Level.TRACE, "DEV:1 PRT:0 radr:%x rdat:%x".formatted(reg.getD1_B(), reg.getD0_B())));
            break;
        case 0xe_cc0cd:
            ym2608Write.accept(1, 1, reg.getD1_B() & 0xff, reg.getD0_B() & 0xff);
            //logger.log(Level.TRACE, "DEV:1 PRT:1 radr:%x rdat:%x".formatted(reg.getD1_B(), reg.getD0_B()));
            break;
        }
    }

    private static final short[] _reg_table = {
            0x0001, // 00 FM1
            0x0001, // 01 FM2
            0x0001, // 02 FM3
            0x0105, // 03 FM4
            0x0105, // 04 FM5
            0x0105, // 05 FM6
            0x0209, // 06 FM1
            0x0209, // 07 FM2
            0x0209, // 08 FM3
            0x030D, // 09 FM4
            0x030D, // 0A FM5
            0x030D, // 0B FM6
            0x0000, // 0C
            0x0000, // 0D
            0x0000, // 0E
            0x0000, // 0F
            //
            0x0000, // 10
            0x0000, // 11
            0x0000, // 12
            0x0000, // 13
            0x0000, // 14
            0x0000, // 15
            0x0000, // 16
            0x0000, // 17
            0x0000, // 18
            0x0000, // 19
            0x0000, // 1A
            0x0000, // 1B
            0x0000, // 1C
            0x0000, // 1D
            0x0000, // 1E
            0x0000, // 1F
            //
            0x0001, // 20 PSG1
            0x0001, // 21 PSG2
            0x0001, // 22 PSG3
            0x0209, // 23 PSG1
            0x0209, // 24 PSG2
            0x0209, // 25 PSG3
            0x0000, // 26
            0x0000, // 27
            0x0000, // 28
            0x0000, // 29
            0x0000, // 2A
            0x0000, // 2B
            0x0000, // 2C
            0x0000, // 2D
            0x0000, // 2E
            0x0000, // 2F
            //
            0x0000, // 30
            0x0000, // 31
            0x0000, // 32
            0x0000, // 33
            0x0000, // 34
            0x0000, // 35
            0x0000, // 36
            0x0000, // 37
            0x0000, // 38
            0x0000, // 39
            0x0000, // 3A
            0x0000, // 3B
            0x0000, // 3C
            0x0000, // 3D
            0x0000, // 3E
            0x0000, // 3F
            //
            0x0001, // 40 RYTHM1
            0x0209  // 41 RYTHM2
    };

    /**
     * OPM Write
     *
     * break a3
     */
    public void _OPM_WRITE4() {
        reg.setD1_B((reg.getD1_B() & 0xff) + (mm.readByte(reg.a5 + W.dev) & 0xff));
        _OPM_WRITE();
    }

    public void _OPM_WRITE() {
        reg.a0 = 0xe90003;

        //while ((byte)mm.readByte(Reg.a0) < 0) ; //wait?
        //mm.write(Reg.a0 - 2, (byte)reg.getD1_B());
        //logger.log(Level.TRACE, "adr:%x dat:%x".formatted(Reg.a0-2, reg.getD1_B())));

        reg.a3 = reg.a6 + Dw.OPMREGWORK;
        reg.setD1_W(reg.getD1_W() & 0xff);
        mm.write(reg.a3 + (reg.getD1_W() & 0xffff), (byte) (reg.getD0_B() & 0xff));

        //while ((byte)mm.readByte(Reg.a0) < 0) ; //wait?
        //mm.write(Reg.a0, (byte)reg.getD0_B());
        //logger.log(Level.TRACE, "adr:%x dat:%x".formatted(Reg.a0, reg.getD0_B())));
        ym2151Write.accept(reg.getD1_B() & 0xff, reg.getD0_B() & 0xff);
        timerOPM.writeReg((byte) reg.getD1_B(), (byte) reg.getD0_B());
    }

    /**
     * OPN/OPM RESET
     *
     * SAVEREG:	.Reg	d0-d1/d5-d7/a0-a1/a3
     */
    private void _dev_reset() {
        Reg spReg = new Reg();
        spReg.D0_L = reg.D0_L;
        spReg.D1_L = reg.D1_L;
        spReg.D5_L = reg.D5_L;
        spReg.D6_L = reg.D6_L;
        spReg.D7_L = reg.D7_L;
        spReg.a0 = reg.a0;
        spReg.a1 = reg.a1;
        spReg.a3 = reg.a3;

        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x1) == 0) { // break _opn_reset_end;

            reg.D7_L = 3;
            reg.D1_L = 0x10;
            reg.D0_L = 0x9f;
            _OPN_WRITE();

            reg.a1 = 0; // _opn_reset_table1;
//L1a:
            while (true) {
                reg.setD1_B(_opn_reset_table1[reg.a1++]); // mm.readByte(Reg.a1++);
                if (reg.getD1_B() - 0xff == 0) break; // L1b;
                reg.setD0_B(_opn_reset_table1[reg.a1++]); // mm.readByte(Reg.a1++);
                reg.D7_L = 0;
                _OPN_WRITE();
                reg.setD7_W(reg.getD7_W() + 6);
                _OPN_WRITE();
//                break L1a;
            }
//L1b:
            reg.a1 = 0; //  _opn_reset_table2;
//L1c:
            while (true) {
                reg.setD1_B(_opn_reset_table2[reg.a1++]); // mm.readByte(Reg.a1++);
                if (reg.getD1_B() - 0xff == 0) break; // _opn_reset_end;
                reg.setD0_B(_opn_reset_table2[reg.a1++]); // mm.readByte(Reg.a1++);
                reg.D7_L = 0;
                _OPN_WRITE();
                reg.setD7_W(reg.getD7_W() + 3);
                _OPN_WRITE();
                reg.setD7_W(reg.getD7_W() + 3);
                _OPN_WRITE();
                reg.setD7_W(reg.getD7_W() + 3);
                _OPN_WRITE();

//                break L1c;
            }
        }
//_opn_reset_end:
        reg.a1 = 0; //  _opm_reset_table;
//L1d:
        while (true) {
            reg.setD1_B(_opm_reset_table[reg.a1++]); //  mm.readByte(Reg.a1++);
            if (reg.getD1_B() - 0xff == 0) break; // L1e;
            reg.setD0_B(_opm_reset_table[reg.a1++]); //  mm.readByte(Reg.a1++);
            _OPM_WRITE();
//            break L1d;
        }
//L1e:
        reg.D1_L = 0x60;
        reg.D0_L = 0x7f;
//L1f:
        do {
            _OPM_WRITE();
            reg.setD1_B(reg.getD1_B() + 1);
        } while ((byte) reg.getD1_B() >= 0); // break L1f;

        reg.D1_L = 0xe0;
        reg.D0_L = 0xff;
//L1g:
        do {
            _OPM_WRITE();
            reg.setD1_B(reg.getD1_B() + 1);
        } while ((byte) reg.getD1_B() < 0); // break L1g;

        //move.W	sr,-(sp)  ?
        //ori.W	// #$700,sr  ?
        reg.setD0_B(mm.readByte(0x9da));
        reg.setD0_B(reg.getD0_B() & 0x40);
        mm.write(0x9da, (byte) reg.getD0_B());
        //move.W	(sp)+,sr  ?
        reg.D1_L = 0x1b;
        _OPM_WRITE();

        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x40) != 0) {
            _mpcm_init();
        }

        reg.D0_L = spReg.D0_L;
        reg.D1_L = spReg.D1_L;
        reg.D5_L = spReg.D5_L;
        reg.D6_L = spReg.D6_L;
        reg.D7_L = spReg.D7_L;
        reg.a0 = spReg.a0;
        reg.a1 = spReg.a1;
        reg.a3 = spReg.a3;
    }

    private static final byte[] _opn_reset_table1 = {
            0x28, 0x00, 0x28, 0x01, 0x28, 0x02,
            0x28, 0x04, 0x28, 0x05, 0x28, 0x06,

            0x06, 0x00,
            0x07, 0x38,
            0x08, 0x00, 0x09, 0x00, 0x0A, 0x00,
            0x0B, 0x00, 0x0C, 0x00, 0x0D, 0x00,

            0x10, (byte) (byte) 0xBF,
            0x11, 0x3F,
            0x18, (byte) 0xC0, 0x19, (byte) 0xC0, 0x1A, (byte) 0xC0,
            0x1B, (byte) 0xC0, 0x1C, (byte) 0xC0, 0x1D, (byte) 0xC0,

            0x20, 0x00,
            0x22, 0x00,
            0x24, 0x00,
            0x25, 0x00,
            0x26, 0x00,
            0x27, 0x30,
            0x29, (byte) 0x80,

            (byte) 0xff, 0x00
    };

    private static final byte[] _opn_reset_table2 = {
            0x40, 0x7F, 0x41, 0x7F, 0x42, 0x7F,
            0x44, 0x7F, 0x45, 0x7F, 0x46, 0x7F,
            0x48, 0x7F, 0x49, 0x7F, 0x4A, 0x7F,
            0x4C, 0x7F, 0x4D, 0x7F, 0x4E, 0x7F,

            (byte) 0x80, (byte) 0xff, (byte) 0x81, (byte) 0xff, (byte) 0x82, (byte) 0xff,
            (byte) 0x84, (byte) 0xff, (byte) 0x85, (byte) 0xff, (byte) 0x86, (byte) 0xff,
            (byte) 0x88, (byte) 0xff, (byte) 0x89, (byte) 0xff, (byte) 0x8A, (byte) 0xff,
            (byte) 0x8C, (byte) 0xff, (byte) 0x8D, (byte) 0xff, (byte) 0x8E, (byte) 0xff,

            (byte) 0x90, 0x00, (byte) 0x91, 0x00, (byte) 0x92, 0x00,
            (byte) 0x94, 0x00, (byte) 0x95, 0x00, (byte) 0x96, 0x00,
            (byte) 0x98, 0x00, (byte) 0x99, 0x00, (byte) 0x9A, 0x00,
            (byte) 0x9C, 0x00, (byte) 0x9D, 0x00, (byte) 0x9E, 0x00,

            (byte) 0xB4, (byte) 0xC0, (byte) 0xB5, (byte) 0xC0, (byte) 0xB6, (byte) 0xC0,

            (byte) 0xff, 0x00
    };

    private static final byte[] _opm_reset_table = {
            0x01, 0x02, 0x01, 0x00,
            0x08, 0x00, 0x08, 0x01, 0x08, 0x02, 0x08, 0x03,
            0x08, 0x04, 0x08, 0x05, 0x08, 0x06, 0x08, 0x07,

            0x0F, 0x00,
            0x10, 0x00, 0x11, 0x00,
            0x12, 0x00, 0x14, 0x30,

            0x18, 0x00, 0x19, 0x00, 0x19, (byte) 0x80,

            0x38, 0x00, 0x39, 0x00, 0x3A, 0x00, 0x3B, 0x00,
            0x3C, 0x00, 0x3D, 0x00, 0x3E, 0x00, 0x3F, 0x00,

            (byte) 0xff, 0x00
    };

    /** */
    private static final int _data_work_size = 384 * 1024;
    public static final int _work_top = 0x01_0000;
    private static final int _buffer_top = 0x03_0000;
    private int _old_trap4_vec = 0;
    private int _old_opn_vec = 0;
    private int _old_opm_vec = 0;
    private byte _old_merc_vec = 0;

    /**
     * program start
     */
    private void start() {
        logger.log(Level.DEBUG, M_title);

        // Supervisor processing not required

        //ori.W	//#$700,sr ?

        _sw_chk();
        if (_mndrv_check() == 0) {
            _mndrv_already();
            return;
        }
        if (_trap4_check() != 0) {
            _trap4_already();
            return;
        }
        if (_opm_check() != 0) {
            _opm_used();
            return;
        }
        if (_get_mem() != 0) {
            _memory_err();
            return;
        }
        _work_init();
        _make_table();
        _mercury_check();
        _mpcm_check();
        _zdd_check();
        _vec_set();
        _dev_reset();
        _print_information();

        // Supervisor processing not required

        // Resident processing not required
    }

    /** */
    private void putdec() {
        logger.log(Level.DEBUG, "{%d}".formatted(reg.D0_L));
    }

    /** */
    private void _sw_chk() {
        reg.D7_L = 0;
        reg.a2 += 1;

        boolean sw_help = false;
//_sw_chk_loop:
        do {
            reg.setD0_B(mm.readByte(reg.a2++) & 0xff);
            if (reg.getD0_B() == 0) return; // break sw_end;
            if (reg.getD0_B() - ' ' == 0) continue; // break _sw_chk_loop;
            //if (reg.getD0_B() - ' ' == 0) break _sw_chk_loop; // No need to skip full-width spaces
            if (reg.getD0_B() - '-' != 0) { // break sw_set;
                sw_help = true;
                break; // sw_help;
            }
//sw_set:
            reg.setD0_B(mm.readByte(reg.a2++) & 0xff);
            if (reg.getD0_B() == 0) return; // break sw_end;
            if (reg.getD0_B() - 'h' == 0) { // break sw_help;
                sw_help = true;
                break;
            }
            int x;
            if (reg.getD0_B() - 'r' == 0) { x = 0; } // break sw_release;
            else if (reg.getD0_B() - 'k' == 0) { x = 1; } //  break sw_keyoff;
            else if (reg.getD0_B() - 'b' == 0) { x = 2; } //  break sw_bufsize;
            else {
                sw_help = true;
                break; // sw_help;
            }
//sw_end:
//            return;
            switch (x) {
            case 1:
//sw_keyoff:
                reg.setD7_B(reg.getD7_B() | 0x80);
                continue; // break _sw_chk_loop;
            case 2:
//sw_bufsize:
                GETNUM();
                reg.D1_L = 1024 * reg.getD1_W();
                reg.a6 = _data_work_size;
                mm.write(reg.a6, reg.D1_L);
                break;

            case 0:
//sw_release:
                if (_mndrv_check() == 0) { // break _mndrv_not_kept;
                    reg.D0_L = 0;
                    trap(4);
                    if (reg.D0_L == 0) { // break _release_false;

                        // Supervisor processing not required

                        logger.log(Level.DEBUG, M_release);

                        return; // The program is actually over
                    }
                } else {
//_mndrv_not_kept:
                    _not_kept();
                    return; // break sw_end;
                }
//_release_false:
                _not_remove();
                return; // break sw_end;
            }
        } while (reg.D1_L != 0); // break _sw_chk_loop;

        if (!sw_help) {
            _numover();
//            return; // break sw_end;
        } else {
//sw_help:
            _help_exit();
//            return; // break sw_end;
        }
    }

    /**
     * from option.s (MCDRV)
     */
    private void GETNUM() {
        reg.D1_L = 0;
        reg.D0_L = 0;
//getnum10:
        while (true) {
            reg.setD0_B(mm.readByte(reg.a2++));
            if (reg.getD0_B() == 0) break; // getnum20;
            if (reg.getD0_B() - ':' == 0) continue; // break getnum10;
            boolean cf = (reg.getD0_B() & 0xff) < 0x30;
            reg.setD0_B(reg.getD0_B() - 0x30);
            if (cf) break; // getnum20;
            if ((reg.getD0_B() & 0xff) >= 10) break; // getnum20;
            reg.D1_L *= 10;
            reg.D1_L += reg.D0_L;
            if (reg.D1_L < 65535) continue; // break getnum10;
            _numover();
            return;
        }
//getnum20:
        reg.a2--;
    }

    /** */
    private int _get_mem() {
        // _SETBLOCK processing not required

        reg.a0 = _work_top;
        mm.write(reg.a0, reg.a1);
        reg.a1 += Dw._work_size;
        reg.a0 = _buffer_top;
        mm.write(reg.a0, reg.a1);

        reg.a0 = _work_top; //  mm.Readint(_work_top);
        reg.setD1_W((reg.getD1_W() & 0xffff) >> 1);
        do {
            mm.write(reg.a0, (short) 0);
            reg.a0 += 2;
        } while (reg.getAndDecD1_W() != 0);

        // make memory block
        reg.D0_L = 0;
        reg.a0 = reg.a1;
        mm.write(reg.a0, reg.D0_L);
        reg.a0 += 4;
        mm.write(reg.a0, reg.D0_L);
        reg.a0 += 4;
        reg.D1_L = _data_work_size;
        reg.D1_L -= 16;
        mm.write(reg.a0, reg.D1_L);
        reg.a0 += 4;
        mm.write(reg.a0, reg.D0_L);
        reg.a0 += 4;
        reg.D0_L = 0;
        return reg.D0_L;
    }

    /**
     * from Mem.s (MCDRV)
     *
     * Obtaining a memory block
     * in	d1.l	size
     * out	d0.l	Address of the allocated memory block + $10
     */
    private int bufferPtr = 0;

    private byte _MCMALLOC() {
        if (bufferPtr == 0) {
            bufferPtr = _buffer_top;
        }
        reg.D0_L = bufferPtr;
        if ((reg.D1_L & 1) != 0) // Odd size?
        {
            reg.D1_L++; // If so, +1
        }
        bufferPtr += reg.D1_L;
        return 0;

//        Reg spReg = new Reg();
//        spReg.D1_L = Reg.D1_L;
//        spReg.a0 = Reg.a0;
//        spReg.a1 = Reg.a1;
//        spReg.a6 = Reg.a6;
//
//        Reg.a0 = _buffer_top;
//        if ((Reg.D1_L & 1) != 0) { // Odd size?
//            Reg.D1_L++; // If so, +1
//        }
//mcmalloc10:
//        if (mm.readByte(Reg.a0 + 13) == 0) {
//            if (Reg.D1_L - mm.Readint(Reg.a0 + 8) <= 0) break mcmalloc30; // Is the size enough?
//        }
////mcmalloc20:
//        Reg.D0_L = mm.Readint(Reg.a0 + 4); // Follow empty blocks from the front
//        if (Reg.D0_L == 0) break mcmalloc80; // No free memory
//        Reg.a0 = Reg.D0_L;
//        break mcmalloc10;
//mcmalloc30:
//        Reg.a1 = Reg.a0 + Reg.D1_L + 16; // a1 = address of the next memory block
//        Reg.D0_L = (unchecked((int) (-16))); // size
//        Reg.D0_L += mm.Readint(Reg.a0 + 8);
//        Reg.D0_L -= Reg.D1_L; // Can I create a managed area?
//        if ((int) Reg.D0_L < 0) break mcmalloc40; // If you can't make it, it's over.
//        mm.Write(Reg.a1 + 8, Reg.D0_L);
//        mm.Write(Reg.a0 + 8, Reg.D1_L);
//        Reg.D0_L = 0;
//        mm.Write(Reg.a1 + 12, Reg.D0_L);
//
//        Reg.D0_L = mm.Readint(Reg.a0 + 4);
//        mm.Write(Reg.a0 + 4, Reg.a1);
//        mm.Write(Reg.a1, Reg.a0);
//        mm.Write(Reg.a1 + 4, Reg.D0_L); // link
//        if (mm.Readint(Reg.a1 + 4) == 0) break mcmalloc40;
//        int v = Reg.a0;
//        Reg.a0 = Reg.D0_L;
//        Reg.D0_L = v;
//        mm.Write(Reg.a0, Reg.a1);
//        Reg.a0 = Reg.D0_L;
//mcmalloc40:
//        mm.Write(Reg.a0 + 12, 0);
//        mm.Write(Reg.a0 + 13, 0xff);
//        Reg.D0_L = 0x16;
//        Reg.D0_L += Reg.a0;
//        break mcmalloc90;
//mcmalloc80:
//        Reg.D0_L = 0xffff_ffff; // -1
//mcmalloc90:
//        Reg.D1_L = spReg.D1_L;
//        Reg.a0 = spReg.a0;
//        Reg.a1 = spReg.a1;
//        Reg.a6 = spReg.a6;
//
//        return (byte) ((Reg.D0_L == 0xffff_ffff) ? -1 : 0);
    }

    /**
     * Freeing a memory block
     * in	d1.l	Address of the memory block to be freed
     */
    private void _MCMFREE() {
        Reg spReg = new Reg();
        spReg.D1_L = reg.D1_L;
        spReg.a0 = reg.a0;
        spReg.a1 = reg.a1;
        spReg.a6 = reg.a6;
        try {
            reg.a6 = _buffer_top;
            reg.a0 = -16;
            reg.a0 += reg.D1_L;
            if (mm.readByte(reg.a0 + 12) != 0) { // break mcmfree80; // If locked, an error occurs
                reg.D0_L = -1;
                return;
            }

            reg.D1_L = 0;
//mcmfree10:
            reg.D0_L = mm.readInt(reg.a0);
            if (reg.D0_L != 0) { // break mcmfree20;
                reg.a1 = reg.D0_L;
                if (reg.a0 - mm.readInt(reg.a1 + 4) != 0) { // break mcmfree80; // If you are not properly linked to the previous one, an error will occur.
                    reg.D0_L = -1;
                    return;
                }
            }
//mcmfree20:
            reg.D0_L = mm.readInt(reg.a0 + 4);
            if (reg.D0_L != 0) { // break mcmfree30;
                reg.a1 = reg.D0_L;
                if (reg.a0 - mm.readInt(reg.a1) != 0) { // break mcmfree80; // If you are not properly linked to the next one, an error will occur.
                    reg.D0_L = -1;
                    return;
                }
            }
//mcmfree30:
            mm.write(reg.a0 + 13, 0); // Change yourself to an empty block
            reg.D0_L = mm.readInt(reg.a0);
            if (reg.D0_L != 0) { // break mcmfree40;
                reg.a1 = reg.D0_L;
                if (mm.readByte(reg.a1 + 13) == 0) { // break mcmfree40; // Was that an empty block before?
                    reg.D0_L = 16;
                    reg.D0_L += mm.readInt(reg.a1 + 8);
                    reg.D0_L += reg.a1;
                    if (reg.a0 - reg.D0_L == 0) { // break mcmfree40; // Also, is it a continuous block?
                        reg.D0_L = 0x16; // If it's empty, stick it together
                        reg.D0_L += mm.readInt(reg.a0 + 8);
                        mm.write(reg.a1 + 8, mm.readInt(reg.a1 + 8) + reg.D0_L); // Add size
                        reg.a0 = mm.readInt(reg.a0 + 4);
                        mm.write(reg.a1 + 4, reg.a0); // link
                        reg.D0_L = reg.a0;
                        if (reg.D0_L != 0) { // break mcmfree35;
                            mm.write(reg.a0, reg.a1);
                        }
//mcmfree35:
                        reg.a0 = reg.a1;
                    }
                }
            }
//mcmfree40:
            reg.D0_L = mm.readInt(reg.a0 + 4);
            if (reg.D0_L != 0) { // break mcmfree50;
                reg.a1 = reg.D0_L;
                if (mm.readByte(reg.a1 + 13) == 0) { // break mcmfree50; // Is there an empty block behind it?
                    reg.D0_L = 16;
                    reg.D0_L += mm.readInt(reg.a0 + 8);
                    reg.D0_L += reg.a0;
                    if (reg.a1 - reg.D0_L == 0) { // break mcmfree50; // Also, is it a continuous block?
                        reg.D0_L = 16; // If it's empty, stick it together
                        reg.D0_L += mm.readInt(reg.a1 + 8);
                        mm.write(reg.a1 + 8, mm.readInt(reg.a1 + 8) + reg.D0_L); // Add size
                        reg.a1 = mm.readInt(reg.a1 + 4);
                        mm.write(reg.a0 + 4, reg.a1); // link
                        reg.D0_L = reg.a1;
                        if (reg.D0_L != 0) { // break mcmfree45;
                            mm.write(reg.a1, reg.a0);
                        }
//mcmfree45:
                    }
                }
            }
//mcmfree50:
            reg.D0_L = 0;
//            break mcmfree90;
//mcmfree80:
//        reg.D0_L = -1;
//mcmfree90:
        } finally {
            reg.D1_L = spReg.D1_L;
            reg.a0 = spReg.a0;
            reg.a1 = spReg.a1;
            reg.a6 = spReg.a6;
        }
    }

    /**
     *
     */
    private void _reset_work() {
        // move.W	sr,-(sp)
        // ori.W	//#$700,sr

        reg.a6 = _work_top; //  mm.Readint(_work_top);
        reg.a5 = reg.a6 + Dw.OPMREGWORK;
        reg.setD1_W(Dw._trackworksize / 2 - 1);
        reg.setD1_W(reg.getD1_W() + 1024);
        reg.D0_L = 0;
        do {
            mm.write(reg.a5, (short) reg.getD0_W());
            reg.a5 += 2;
        } while (reg.getAndDecD1_W() != 0);
        _work_init_env_();
    }

    private void _work_init() {
        // move.W	sr,-(sp)
        // ori.W	//#$700,sr

        reg.a6 = _work_top; //  mm.Readint(_work_top);
        reg.a5 = reg.a6;
        reg.setD1_W(Dw._work_size / 2 - 1);
        reg.D0_L = 0;
        do {
            mm.write(reg.a5, (short) reg.getD0_W());
            reg.a5 += 2;
        } while (reg.getAndDecD1_W() != 0);
        mm.write(reg.a6 + Dw.RANDOMESEED, 0x12345678);
        _work_init_env_();
    }

    private void _work_init_env_() {
        reg.a5 = reg.a6 + Dw.SOFTENV_PATTERN;
        reg.D1_L = 7 - 1;
//_work_init_env:
        do {
            reg.a4 = 0; // _psg_env_pattern;
            reg.D0_L = 16 - 1;
            do {
                mm.write(reg.a5, _psg_env_pattern[reg.a4]); // mm.readByte(Reg.a4));
                reg.a4++;
                reg.a5++;
            } while (reg.getAndDecD0_W() != 0);
        } while (reg.getAndDecD1_W() != 0);

        reg.D0_L = MnWork.TRACK - 1;
        reg.a5 = reg.a6 + Dw.TRACKWORKADR;
        reg.a4 = Ab.dummyAddress; // _work_init_nop;
        do {
//1:
            mm.write(reg.a5 + W.mmljob_adrs, reg.a4);
            ab.hlw_mmljob_adrs.remove(reg.a5);
            ab.hlw_mmljob_adrs.put(reg.a5, this::_work_init_nop);

            mm.write(reg.a5 + W.softenv_adrs, reg.a4);
            ab.hlw_softenv_adrs.remove(reg.a5);
            ab.hlw_softenv_adrs.put(reg.a5, this::_work_init_nop);

            mm.write(reg.a5 + W.lfojob_adrs, reg.a4);
            ab.hlw_lfojob_adrs.remove(reg.a5);
            ab.hlw_lfojob_adrs.put(reg.a5, this::_work_init_nop);

            mm.write(reg.a5 + W.psgenv_adrs, reg.a4);
            //ab.hlw_psgenv_adrs.remove(Reg.a5);
            //Ab.hlw_psgenv_adrs.put(Reg.a5, _work_init_nop);

            mm.write(reg.a5 + W.qtjob, reg.a4);
            ab.hlw_qtjob.remove(reg.a5);
            ab.hlw_qtjob.put(reg.a5, this::_work_init_nop);

            mm.write(reg.a5 + W.rrcut_adrs, reg.a4);
            ab.hlw_rrcut_adrs.remove(reg.a5);
            ab.hlw_rrcut_adrs.put(reg.a5, this::_work_init_nop);

            mm.write(reg.a5 + W.echo_adrs, reg.a4);
            ab.hlw_echo_adrs.remove(reg.a5);
            ab.hlw_echo_adrs.put(reg.a5, this::_work_init_nop);

            mm.write(reg.a5 + W.keyoff_adrs, reg.a4);
            ab.hlw_keyoff_adrs.remove(reg.a5);
            ab.hlw_keyoff_adrs.put(reg.a5, this::_work_init_nop);

            mm.write(reg.a5 + W.keyoff_adrs2, reg.a4);
            ab.hlw_keyoff_adrs2.remove(reg.a5);
            ab.hlw_keyoff_adrs2.put(reg.a5, this::_work_init_nop);

            mm.write(reg.a5 + W.subcmd_adrs, reg.a4);
            ab.hlw_subcmd_adrs.remove(reg.a5);
            ab.hlw_subcmd_adrs.put(reg.a5, this::_work_init_nop);

            mm.write(reg.a5 + W.setnote_adrs, reg.a4);
            ab.hlw_setnote_adrs.remove(reg.a5);
            ab.hlw_setnote_adrs.put(reg.a5, this::_work_init_nop);

            mm.write(reg.a5 + W.inithlfo_adrs, reg.a4);
            ab.hlw_inithlfo_adrs.remove(reg.a5);
            ab.hlw_inithlfo_adrs.put(reg.a5, this::_work_init_nop);

            mm.write(reg.a5 + W.we_ycom_adrs, reg.a4);
            ab.hlw_we_ycom_adrs.remove(reg.a5);
            ab.hlw_we_ycom_adrs.put(reg.a5, this::_work_init_nop);

            mm.write(reg.a5 + W.we_tone_adrs, reg.a4);
            ab.hlw_we_tone_adrs.remove(reg.a5);
            ab.hlw_we_tone_adrs.put(reg.a5, this::_work_init_nop);

            reg.a5 = reg.a5 + W._track_work_size;
        } while (reg.getAndDecD0_W() != 0);

        mm.write(reg.a6 + Dw.TRKANA_RESTADR, reg.a4);
        ab.hlTRKANA_RESTADR.remove(reg.a6);
        ab.hlTRKANA_RESTADR.put(reg.a6, this::_work_init_nop);
    }

    private void _work_init_nop() {
        // rts
    }

    /** */
    private final byte[] _psg_env_pattern = {
            0x00, 0x01, (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x81, 0x00, 0x00, 0x00, (byte) 0x81, 0x00, 0x00, (byte) 0xff, (byte) 0x81, 0x00, 0x00
    };

    /** Probably not */
    private void _vec_set() {
    }

    /** Probably not */
    private void _vec_release() {
        mm.write(0xe88009, (byte) (mm.readByte(0xe88009) & 0xf7));
        mm.write(0xe88015, (byte) (mm.readByte(0xe88015) & 0xf7));

        // move.W	sr,-(sp)
        // ori.W	//#$700,sr

        //wait Probably not necessary
        //while (mm.readByte(0xe9a001) != 0 || (mm.readByte(Reg.a6 + Dw.DRV_STATUS) & 1) != 0) ;

        mm.write(0x10c, _old_opm_vec);
        mm.write(0x90, _old_trap4_vec);

        //wait Probably not necessary
        //while ((mm.readByte(Reg.a6 + Dw.DRV_FLAG) & 1) != 0) ;

        mm.write(0xecc0b1, _old_merc_vec);
        mm.write(0x3fc, _old_opn_vec);

        // move.W	(sp)+,sr
    }

    /**
     * trap check
     */
    private int _trap4_check() {
        return 0;
        //return (mm.readByte(0x90) - 0x24) == 0 ? 0 : 1;
    }

    /**
     * driver check
     */
    private int _mndrv_check() {
        // No check required (not always resident)
        return 1;
    }

    /**
     *
     */
    private void _print_information() {
        if ((byte) reg.getD7_B() < 0) {
            mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) | 0x80));
        }
        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 1) == 0) {
            logger.log(Level.DEBUG, M_merc);
        }
        reg.D1_L = 0;
        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x40) != 0) {
            logger.log(Level.DEBUG, M_MPCM);
            reg.setD1_B(0xff);
        }
        if ((mm.readByte(reg.a6 + Dw.DRV_FLAG) & 0x08) != 0) { // break L2;
            if (reg.getD1_B() != 0) { // break L1;
                logger.log(Level.DEBUG, ",");
            }
//L1:
            logger.log(Level.DEBUG, M_zdd);
            reg.setD1_B(0xff);
        }
//L2:
        if (reg.getD1_B() != 0) {
            logger.log(Level.DEBUG, M_PCMOUT);
        }

        reg.D0_L = _data_work_size;
        reg.D0_L /= 1024;
        putdec();

        logger.log(Level.DEBUG, M_buf);
    }

    /**
     * OPM interrupt check
     */
    private int _opm_check() {
        return 0;
        //return mm.readByte(0x10c) - 0x43;
    }

    /**
     * PCM driver resident check
     */
    private void _mpcm_check() {
        //Reg.a1 = mm.Readint(0x84);
        //if (mm.Readint(Reg.a1 - 8) - 0x4d50434d != 0) return;
        //if ((int)(mm.Readint(Reg.a1 - 4) - 0x2f303430) < 0) return;

        reg.setD0_W(0x8000);
        //Reg.a1 = M_keeptitle;
        trap(1);

        mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) | 0x40));

        reg.setD0_W(0x8002);
        trap(1);

        _mpcm_init();
    }

    private void _mpcm_init() {
        reg.setD0_W(0x01ff);
        trap(1);

        reg.setD0_W(0x03ff);
        reg.D1_L = 4;
        trap(1);

        reg.setD0_W(0x05ff);
        reg.D1_L = 0x40;
        trap(1);

        reg.setD0_W(0x06ff);
        reg.D1_L = 0x03;
        trap(1);

        reg.setD0_W(0x8005);
        reg.D1_L = 0xffff_ffff;
        //Reg.a1 = _mpcm_volume_table;
        trap(1);
    }

    /**
     * zdd resident check
     */
    private void _zdd_check() {
    }

    /**
     * "Mercury" existence check
     * with Xellent30 / YMF288 detection
     */
    private void _mercury_check() {
        Reg spReg = new Reg();
        spReg.a0 = reg.a0;
        spReg.a1 = reg.a1;
        spReg.a2 = reg.a2;

        if (_unit_check() == 0) { // break _notmerc;
            if (_opn_check() == 0) { // break _notmerc;
                mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) | 0x20));
                reg.a0 = spReg.a0;
                reg.a1 = spReg.a1;
                reg.a2 = spReg.a2;
                return;
            }
        }
//_notmerc:
        mm.write(reg.a6 + Dw.DRV_FLAG, (byte) (mm.readByte(reg.a6 + Dw.DRV_FLAG) | 0x21));
        reg.a0 = spReg.a0;
        reg.a1 = spReg.a1;
        reg.a2 = spReg.a2;
    }

    private int _opn_check() {
        return 0;
        //Reg.a0 = 0xecc0c1;
        //mm.Write(Reg.a0, (byte)0x20);
        //_wait();
        //mm.Write(Reg.a0 + 2, 0);
        //_wait();
        //mm.Write(Reg.a0, 0xff);
        //_wait();
        //return mm.readByte(Reg.a0 + 2) - 1;
    }

    public void _wait() {
        mm.readByte(0xe9a001);
        mm.readByte(0xe9a001);
        mm.readByte(0xe9a001);
        mm.readByte(0xe9a001);
    }

    private int _unit_check() {
        reg.a0 = 0xecc080;
        if (_bus_check() == 0) { // break _unit_check_notmerc;
            reg.a0 = 0xecc100;
            if (_bus_check() != 0) { // break _unit_check_notmerc;
                return 0; // ccr ?
            }
        }
//_unit_check_notmerc:
        reg.setD0_B(0xff);
        return 1;
    }

    private int _bus_check() {
        // 0xecc080 -> L1 and onwards are executed only if there is no merc
        // 0xecc100 -> L1 and onwards are executed only if merc is present
        if (reg.a0 == 0xecc080) return 0;
        return -1;

        //Reg.D6_L = sp;
        //Reg.a1 = L1;
        //Reg.a2 = mm.Readint(8);
        //mm.Write(8, Reg.a1);
        //reg.getD0_W() = mm.Readshort(Reg.a0);
        //mm.Write(8, Reg.a2);
        //Reg.D0_L = 0;
        //return;

//L1:
        //sp = Reg.D6_L;
        //mm.Write(8, Reg.a2);
        //Reg.D0_L = 0xffff_ffff;
        //return;
    }

    /**
     *
     */
    private void _make_table() {
        //
        // MAKE F-Number → OPM KC/KF CONVERT TABLE
        //
//MAKE_FNUM_TABLE:
        reg.a0 = 0; //  FNUM_BASE;
        reg.a1 = 0; //  FNUM_KC_BASE;
        reg.a2 = reg.a6 + Dw.FNUM_KC_TABLE;
        reg.setD0_W(0xc000);
        reg.setD1_W(FNUM_BASE[reg.a0]); //  mm.Readshort(Reg.a0);

//MAKE_FNUMTBL2:
        do {
            mm.write(reg.a2, (short) reg.getD0_W());
            reg.a2 += 2;  // !
            reg.setD1_W(reg.getD1_W() - 1);
        } while (reg.getD1_W() != 0); // break MAKE_FNUMTBL2;

        reg.D5_L = 0;

//MAKE_FNUMTBL3:
        while (true) {
            reg.setD6_B(FNUM_KC_BASE[reg.a1 + reg.getD5_W()]); // mm.readByte(Reg.a1 + reg.getD5_W());
            reg.setD5_B(reg.getD5_B() + (int) (byte) reg.getD5_B());
            reg.setD1_W(FNUM_BASE[reg.a0 + (reg.getD5_W()) / 2]); //mm.Readshort(Reg.a0 + reg.getD5_W());
            reg.setD3_W(reg.getD1_W());
            reg.setD2_W(FNUM_BASE[reg.a0 + (reg.getD5_W() + 2) / 2]); //mm.Readshort(Reg.a0 + reg.getD5_W() + 2);

            if (reg.getD2_W() >= 0x0800) break; // MAKE_FNUMTBL5;

            reg.setD5_B((reg.getD5_B() & 0xff) >> 1);
            reg.setD2_W(reg.getD2_W() - (int) (short) reg.getD1_W());
            reg.D1_L = 0;

//MAKE_FNUMTBL4:
            do {
                mm.write(reg.a2, (byte) reg.getD6_B());
                reg.a2++;
                reg.D0_L = 0;
                reg.setD0_W(reg.getD1_W());
                reg.D0_L <<= 8;
                reg.D0_L = ((reg.D0_L / (reg.getD2_W() & 0xffff)) & 0xffff) | (((reg.D0_L % (reg.getD2_W() & 0xffff)) & 0xffff) << 16);
                mm.write(reg.a2, (byte) reg.getD0_B());
                reg.a2++;
                reg.setD1_W(reg.getD1_W() + 1);

            } while (reg.getD1_W() - reg.getD2_W() != 0); // break MAKE_FNUMTBL4;

            reg.setD5_B(reg.getD5_B() + 1);
//            break MAKE_FNUMTBL3;
        }

//MAKE_FNUMTBL5:
        mm.write(reg.a2++, (byte) reg.getD6_B());
        mm.write(reg.a2++, 0);
        reg.setD0_W(0xc000);
        reg.setD1_W(reg.getD3_W());
        reg.setD1_W(reg.getD1_W() + 1);

//MAKE_FNUMTBL6:
        do {
            mm.write(reg.a2, (short) reg.getD0_W());
            reg.a2 += 2;
            reg.setD1_W(reg.getD1_W() + 1);
        } while (reg.getD1_W() - 0x0800 != 0); // break MAKE_FNUMTBL6;

        //
        // MAKE Frequency → OPM KC/KF CONVERT TABLE
        //
//MAKE_FREQ_TABLE:
        reg.a0 = 0; //  FREQ_BASE;
        reg.a1 = 0; //  FREQ_KC_BASE;
        reg.a2 = reg.a6 + Dw.FREQ_KC_TABLE;
        reg.setD0_W(0x7efc);
        reg.setD1_W(FREQ_BASE[reg.a0]); // mm.Readshort(Reg.a0);

//MAKE_FREQTBL2:
        do {
            mm.write(reg.a2, (short) reg.getD0_W());
            reg.a2 += 2;  // !
            reg.setD1_W(reg.getD1_W() - 1);
        } while (reg.getD1_W() != 0); // break MAKE_FREQTBL2;

        reg.D5_L = 0;

//MAKE_FREQTBL3:
        while (true) {
            reg.setD6_B(FREQ_KC_BASE[reg.a1 + reg.getD5_W()]); // mm.readByte(Reg.a1 + reg.getD5_W());
            reg.setD5_W(reg.getD5_W() + (short) reg.getD5_W());
            reg.setD1_W(FREQ_BASE[reg.a0 + reg.getD5_W() / 2]); // mm.Readshort(Reg.a0 + reg.getD5_W());
            reg.setD3_W(reg.getD1_W());
            reg.setD2_W(FREQ_BASE[reg.a0 + (reg.getD5_W() + 2) / 2]); // mm.Readshort(Reg.a0 + reg.getD5_W() + 2);

            if (reg.getD2_W() >= 0x1000) break; // MAKE_FREQTBL6;

            reg.setD5_W((reg.getD5_W() & 0xffff) >> 1);
            reg.setD2_W(reg.getD2_W() - (int) (short) reg.getD1_W());
            reg.D1_L = 0;
            mm.write(reg.a2++, (byte) reg.getD6_B());
            mm.write(reg.a2++, (byte) reg.getD1_B());
            reg.setD6_B(FREQ_KC_BASE[reg.a1 + reg.getD5_W() + 1]); // mm.readByte(Reg.a1 + reg.getD5_W() + 1);
            reg.setD1_W(reg.getD2_W());
            boolean MAKE_FREQTBL5;
            if (reg.getD1_W() == 0) { // break MAKE_FREQTBL5;

                reg.setD5_B(reg.getD5_B() + 1);
                continue; // break MAKE_FREQTBL3;
            } else {
                MAKE_FREQTBL5 = true;
            }
//MAKE_FREQTBL4:
            do {
                if (!MAKE_FREQTBL5) {
                    mm.write(reg.a2, (byte) reg.getD6_B());
                    reg.a2++;
                    reg.D0_L = 0;
                    reg.setD0_W(reg.getD1_W());
                    reg.D0_L <<= 8;
                    reg.D0_L = ((reg.D0_L / (reg.getD2_W() & 0xffff)) & 0xffff) | (((reg.D0_L % (reg.getD2_W() & 0xffff)) & 0xffff) << 16);
                    mm.write(reg.a2, (byte) reg.getD0_B());
                    reg.a2++;
                } else {
                    MAKE_FREQTBL5 = false;
                }
//MAKE_FREQTBL5:
                reg.setD1_W(reg.getD1_W() - 1);
            } while (reg.getD1_W() != 0); // break MAKE_FREQTBL4;

            reg.setD5_B(reg.getD5_B() + 1);
//            break MAKE_FREQTBL3;
        }

//MAKE_FREQTBL6:
        mm.write(reg.a2++, (byte) reg.getD6_B());
        mm.write(reg.a2++, 0);
        reg.D0_L = 0;
        reg.setD1_W(reg.getD3_W());
        reg.setD1_W(reg.getD1_W() + 1);
//MAKE_FREQTBL7:
        do {
            mm.write(reg.a2, (short) reg.getD0_W());
            reg.a2 += 2;
            reg.setD1_W(reg.getD1_W() + 1);

        } while (reg.getD1_W() - 0x1000 != 0); // break MAKE_FREQTBL7;
    }

    /**
     * error exit
     */
    private void _not_remove() {
        logger.log(Level.DEBUG, M_notremove);
    }

    private void _not_kept() {
        logger.log(Level.DEBUG, M_notkept);
    }

    private void _help_exit() {
        logger.log(Level.DEBUG, M_help);
    }

    private void _mndrv_already() {
        logger.log(Level.DEBUG, M_already);
    }

    private void _trap4_already() {
        logger.log(Level.DEBUG, M_trap4err);
    }

    private void _opm_used() {
        logger.log(Level.DEBUG, M_opmerr);
    }

    private void _numover() {
        logger.log(Level.DEBUG, M_numover);
    }

    private void _memory_err() {
        logger.log(Level.DEBUG, M_memory_msg);
    }

    /**
     * data section
     */
    public static final short[] _mpcm_volume_table = {
            0, 17, 18, 19, 20, 21, 22, 23,
            24, 25, 26, 27, 28, 29, 30, 31,
            32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43, 44, 45, 46, 47,
            48, 50, 52, 54, 56, 58, 60, 62,
            64, 66, 68, 70, 72, 74, 76, 78,
            80, 82, 84, 86, 88, 90, 92, 94,
            96, 100, 104, 108, 112, 116, 120, 124,
            128, 132, 136, 140, 144, 148, 152, 156,
            160, 164, 168, 172, 176, 180, 184, 188,
            192, 200, 208, 216, 224, 232, 240, 248,
            256, 264, 272, 280, 288, 296, 304, 312,
            320, 328, 336, 344, 352, 360, 368, 376,
            384, 400, 416, 432, 448, 464, 480, 496,
            512, 528, 544, 560, 576, 592, 608, 624,
            640, 656, 672, 688, 704, 720, 736, 752
    };

    /**
     * OPN → OPM TUNE CONVERT TABLE (DEFAULT)
     */
    private static final short[] FNUM_BASE = {
            0x00A3, 0x00AD, 0x00B7, 0x00C2,
            0x00CD, 0x00DA, 0x00E7, 0x00F4,
            0x0103, 0x0112, 0x0123, 0x0134,
            0x0146, 0x015A, 0x016F, 0x0184,
            0x019B, 0x01B4, 0x01CE, 0x01E9,
            0x0207, 0x0225, 0x0246, 0x0269,
            0x028D, 0x02B4, 0x02DE, 0x0309,
            0x0337, 0x0368, 0x039C, 0x03D3,
            0x040E, 0x044B, 0x048D, 0x04D2,
            0x051A, 0x0568, 0x05BC, 0x0612,
            0x066E, 0x06D0, 0x0738, 0x07A6,
            0x081C, 0x0896, 0x091A, 0x09A4
    };

    private static final byte[] FNUM_KC_BASE = {
            (byte) 0xDD, (byte) 0xDE, (byte) 0xE0, (byte) 0xE1, (byte) 0xE2, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6,
            (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xEC, (byte) 0xED, (byte) 0xEE, (byte) 0xF0, (byte) 0xF1,
            (byte) 0xF2, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA, (byte) 0xFC,
            (byte) 0xFD, (byte) 0xFE, 0x00, 0x01, 0x02, 0x04, 0x05, 0x06,
            0x08, 0x09, 0x0A, 0x0C, 0x0D, 0x0E, 0x10, 0x11,
            0x12, 0x14, 0x15, 0x16, 0x18, 0x19, 0x1A, 0x1C
    };

    private static final short[] FREQ_BASE = {
            0x000E, 0x000F, 0x0010, 0x0011,
            0x0012, 0x0013, 0x0015, 0x0016,
            0x0017, 0x0019, 0x001A, 0x001C,
            0x001D, 0x001F, 0x0021, 0x0023,
            0x0025, 0x0027, 0x002A, 0x002C,
            0x002F, 0x0032, 0x0035, 0x0038,
            0x003B, 0x003F, 0x0043, 0x0047,
            0x004B, 0x004F, 0x0054, 0x0059,
            0x005E, 0x0064, 0x006A, 0x0070,
            0x0077, 0x007E, 0x0086, 0x008E,
            0x0096, 0x009F, 0x00A8, 0x00B2,
            0x00BD, 0x00C8, 0x00D4, 0x00E1,
            0x00EE, 0x00FD, 0x010C, 0x011C,
            0x012C, 0x013E, 0x0151, 0x0165,
            0x017B, 0x0191, 0x01A9, 0x01C2,
            0x01DC, 0x01FA, 0x0218, 0x0238,
            0x0258, 0x027C, 0x02A2, 0x02CA,
            0x02F6, 0x0322, 0x0352, 0x0384,
            0x03B8, 0x03F4, 0x0430, 0x0470,
            0x04B0, 0x04F8, 0x0544, 0x0594,
            0x05EC, 0x0644, 0x06A4, 0x0708,
            0x0770, 0x07E8, 0x0860, 0x08E0,
            0x0960, 0x09F0, 0x0A88, 0x0B28,
            0x0BD8, 0x0C88, 0x0D48, 0x0E10,
            0x0EE0, 0x0FD0, 0x10C0, 0x11C0,
            0x12C0, 0x13E0, 0x1510, 0x1650,
            0x17B0, 0x1910, 0x1A90, 0x1C20
    };

    private static final byte[] FREQ_KC_BASE = {
            (byte) 0x8C, (byte) 0x8A, (byte) 0x89, (byte) 0x88, (byte) 0x86, (byte) 0x85, (byte) 0x84, (byte) 0x82,
            (byte) 0x81, (byte) 0x80, 0x7E, 0x7D, 0x7C, 0x7A, 0x79, 0x78,
            0x76, 0x75, 0x74, 0x72, 0x71, 0x70, 0x6E, 0x6D,
            0x6C, 0x6A, 0x69, 0x68, 0x66, 0x65, 0x64, 0x62,
            0x61, 0x60, 0x5E, 0x5D, 0x5C, 0x5A, 0x59, 0x58,
            0x56, 0x55, 0x54, 0x52, 0x51, 0x50, 0x4E, 0x4D,
            0x4C, 0x4A, 0x49, 0x48, 0x46, 0x45, 0x44, 0x42,
            0x41, 0x40, 0x3E, 0x3D, 0x3C, 0x3A, 0x39, 0x38,
            0x36, 0x35, 0x34, 0x32, 0x31, 0x30, 0x2E, 0x2D,
            0x2C, 0x2A, 0x29, 0x28, 0x26, 0x25, 0x24, 0x22,
            0x21, 0x20, 0x1E, 0x1D, 0x1C, 0x1A, 0x19, 0x18,
            0x16, 0x15, 0x14, 0x12, 0x11, 0x10, 0x0E, 0x0D,
            0x0C, 0x0A, 0x09, 0x08, 0x06, 0x05, 0x04, 0x02,
            0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    private static final String M_title =
            "X68k MnDrv mania driver version " +
                    DRVVER +
                    " (c)1997-2000 BEL.\n";
    private static final String M_merc = "Output is possible from 'Mercury Unit'\n";
    private static final String M_MPCM = "MPCM";
    private static final String M_zdd = "zdd";
    private static final String M_PCMOUT = "Multiplexing/pitch and volume conversion output is possible\n";
    private static final String M_buf = "KB buffer reserved\n";
    private static final String M_release = "released mndrv\n";
    private static final String M_already = "Already resident\n";
    private static final String M_notkept = "mndrv is not resident\n";
    private static final String M_notremove = "It is occupied and cannot be released\n";
    private static final String M_trap4err = "trap //#4 is already in use\n";
    private static final String M_opmerr = "OPM interrupt already in use\n";
    private static final String M_memory_msg = "Not enough memory\n";
    private static final String M_numover = "Number out of range\n";
    private static final String M_help = """
            usage: MnDrv [option]
            	-b[num]	Buffer size specification
            	-k	Key control disabled
            	-r	Cancel residency
            """;
}
