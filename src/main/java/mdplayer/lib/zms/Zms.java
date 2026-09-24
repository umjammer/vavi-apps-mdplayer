package mdplayer.lib.zms;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import mdplayer.emu.nise68.IMemory;
import mdplayer.emu.nise68.IRegister;
import mdplayer.lib.mndrv.Reg;
import mdplayer.lib.mxdrv.MXDRV.Pcm8Interface;
import mdplayer.lib.mxdrv.MXDRV.Pcm8St;
import mdplayer.emu.common.FMTimer;
import mdplayer.emu.nise68.FileMng;
import mdplayer.emu.nise68.MemMng;
import mdplayer.emu.nise68.Nise68;
import vavi.util.StringUtil;
import vavi.util.compat.Tuple;

import static java.lang.System.getLogger;
import static vavi.util.compat.Util.changeExtension;
import static vavi.util.compat.Util.getExtension;


/**
 * ZMUSIC X68000
 *
 * @author kumatan
 */
public class Zms {

    private static final Logger logger = getLogger(Zms.class.getName());

    private Nise68 nise68;
    private FileMng fileMng = new FileMng(System.getProperty("user.dir"), "C:");

    /**
     * The ZPD the playing song loaded: ZMUSIC opens it by itself, from the ZMS {@code .ADPCM_BLOCK_DATA}
     * or the ZMD's own reference, so it is only known from what was asked of the file manager.
     *
     * @return the name as the song gave it, and whether it was there; null when none was asked for
     */
    public Map.Entry<String, Boolean> pcmFile() {
        Map.Entry<String, Boolean> last = null;
        for (Map.Entry<String, Boolean> e : fileMng.getLookups().entrySet()) {
            if (e.getKey().toUpperCase().endsWith(".ZPD")) last = e;
        }
        return last;
    }
    public Pcm8Interface pcm8;
    public MPcmInterface mpcm;
    public int frequency;
    /** zmusic.x etc. location */
    public String dir;
    /** .zpd file location */
    public String zpd;

    /** abstraction for mpcm chip implementation */
    public interface MPcmInterface {
        void keyOn(int ch);
        void keyOff(int ch);
        void writePcm(int ch, Zms.MPCMSt[] pcm, IMemory mem, IRegister reg, int n);
        void setFreq(int ch, int value);
        void setPitch(int ch, int value);
        void setVol(int ch, int value);
        void setPan(int ch, int value);
        void reset();
        void setVolTable(int type);
        void setVolTable(int type, int[] vtbl);
    }

    private int checkCounter = 0;
    private List<String> dirZPDs = new ArrayList<>();
    public int version = 0;
    private FMTimer timerOPM;
    private final Pcm8St[] pcm8St = {
            new Pcm8St(), new Pcm8St(), new Pcm8St(), new Pcm8St(),
            new Pcm8St(), new Pcm8St(), new Pcm8St(), new Pcm8St()
    };
    private final MPCMSt[] mpcmSt = {
            new MPCMSt(), new MPCMSt(), new MPCMSt(), new MPCMSt(),
            new MPCMSt(), new MPCMSt(), new MPCMSt(), new MPCMSt(),
            new MPCMSt(), new MPCMSt(), new MPCMSt(), new MPCMSt(),
            new MPCMSt(), new MPCMSt(), new MPCMSt(), new MPCMSt()
    };

    public static class MPCMSt {

        public boolean keyOn = false;
        public boolean keyOff = false;
        public byte type = 0;
        public byte orig = 0;
        public int adrs_ptr = 0;
        public int size = 0;
        public int start = 0;
        public int end = 0;
        public int count = 0;
        public int frq = 0;
        public int pitch = 0;
        public int volume = 0;
        public int pan = 0;
        float rate = 0;
        float base = 0;

        public void fill(IMemory mem, IRegister reg) {
            this.type = mem.readByte(0x00 + reg.getAl(1));
            this.orig = mem.readByte(0x01 + reg.getAl(1));
            this.adrs_ptr = mem.readInt(0x04 + reg.getAl(1));
            this.size = mem.readInt(0x08 + reg.getAl(1));
            this.start = mem.readInt(0x0c + reg.getAl(1));
            this.end = mem.readInt(0x10 + reg.getAl(1));
            this.count = mem.readInt(0x14 + reg.getAl(1));
            if (reg instanceof Reg)
                this.frq = (this.type & 0xff) == 0xff ? 4 : (this.type == 1 ? 8 : (this.type == 2 ? 0x10 : 0));
        }

        public void fill(float[] rateBase) {
            this.rate = rateBase[0];
            this.base = rateBase[1];
        }
    }

    public String playingFileName;

    public String playingArcFileName;

    public List<Tuple<byte[], String>> supportFileBinaryAndName;
    public byte[] compiledData;

    public Runnable stop;
    public IntConsumer loop;
    public IntSupplier wait;
    public BiConsumer<Integer, Integer> ym2151Write;
    public BiConsumer<Integer, byte[]> midiSend;
    public Charset charset;

    public void trap() {
        if (version == 2) {
            timerOPM.timer();
            while ((timerOPM.readStatus() & 3) != 0) {
                nise68.trapOPM(); // true, true, true);
            }
        } else {
            //virtualFrameCounter++;
            while (nise68.intTimer()) {
//#if DEBUG
                //if (model != EnmModel.RealModel)
                //    nise68.Trap(0x8e, true, true, true);
//                            nise68.Trap(0x8e);
//#else
                nise68.trap(0x8e);
//#endif
            }
        }
    }

    public void clock() {
        checkCounter--;
        if (checkCounter < 0) {
            checkCounter = 100;
            if (version == 2) {
                // Check if playing
                nise68.reg.setDl(1, 0x09); // m_stat
                nise68.reg.setDl(2, 0); // Check mode (0: Check all channels)
                nise68.trap(3 + 32);
                int d0 = nise68.reg.getDl(0);
                if (d0 == 0) {
                    if (preData.isEmpty()) stop.run();
                    else {
                        preData.removeFirst();
                        byte[] zmd = null;
                        if (preData.isEmpty()) {
                            //if (nise68.hmn.fb.containsKey(fnZMD)) zmd = nise68.hmn.fb[fnZMD];
                            if (fileMng.existsFile(fnZMD.toString())) zmd = fileMng.vReadAllBytes(fnZMD.toString());
                        } else {
                            //if (nise68.hmn.fb.containsKey(preData[0])) zmd = nise68.hmn.fb[preData[0]];
                            if (fileMng.existsFile(preData.getFirst())) zmd = fileMng.vReadAllBytes(preData.getFirst());
                        }
                        int fileSize = zmd.length;
                        int filePtr = nise68.hmn.memMng.malloc(fileSize);
                        for (int i = 0; i < zmd.length; i++) {
                            nise68.mem.pokeB(filePtr + i, zmd[i]);
                        }

                        nise68.reg.setDl(1, 0x11); // play_cnv_data
                        nise68.reg.setDl(2, zmd.length - 7);
                        nise68.reg.setAl(1, filePtr + 7);
                        nise68.trap(trp); // , true, true, true);
                        waitNextPlay = wait.getAsInt();
                    }
                }

                // Loop count check
                nise68.reg.setDl(1, 0x4d); // get_loop_time
                nise68.trap(3 + 32);
                d0 = nise68.reg.getDl(0);
                loop.accept(d0 - 1);
            } else {
                // Check if playing
                nise68.reg.setDl(0, 0x0b); // ZM_PLAY_STATUS
                nise68.reg.setDl(1, 0); // Check mode (0: Check all channels)
                nise68.reg.setAl(1, 0); // Inspection result storage buffer address (set to 0 to return simplified inspection results)
                nise68.trap(3 + 32);
                int d0 = nise68.reg.getDl(0);
                if (d0 == 0)
                    stop.run();

                // Loop count check
                nise68.reg.setDl(0, 0x59); // ZM_LOOP_CONTROL
                nise68.reg.setDl(1, 0xffff_ffff); // Control mode (-1 = get loop count)
                nise68.trap(3 + 32);
                d0 = nise68.reg.getDl(0);
                loop.accept(d0 - 1);
            }
        }
    }

    public void run(byte[] data) throws IOException {
        String fn = playingFileName;
        String withoutExtFn = fn.substring(0, fn.lastIndexOf('.') > 0 ? fn.lastIndexOf('.') : fn.length());
        Path dn = Path.of(fn).getParent();
        if (dn == null) dn = Path.of(System.getProperty("user.dir"));
        Path fnZMD = dn.resolve(withoutExtFn + ".ZMD");
        Path fnZMS = dn.resolve(withoutExtFn + ".ZMS");

        nise68 = new Nise68();
        nise68.setMPcm(version == 2 ? this::pcm8CallBack : this::mPcmCallBack);
        nise68.setOpm(this::opmCallBack);
        nise68.setMidi(this::midiCallBack, frequency);
        nise68.setSCC_A(this::sccCallBack, frequency);
        if (playingArcFileName != null && !playingArcFileName.isEmpty()) {
            if (playingFileName.toUpperCase().endsWith(".ZDF")) {
                UnZDF cmd = new UnZDF();
                cmd.dir = dir;
                fileMng = cmd.unpack(playingArcFileName, charset);
            }

        } else {
            fileMng = new FileMng(dn.toString(), "C:");
        }
        nise68.init(dirZPDs, version == 2, fileMng, charset);

        fileMng.setVFile(fnZMD.getFileName().toString(), data);
        //nise68.hmn.fb.add(fnZMD, dataBuf);
        //if (format == EnmFileFormat.ZMD) nise68.hmn.fb.add(fnZMD, dataBuf);
        //else {
        //    // compile
        //    if (model != EnmModel.RealModel || compiledData == null) compile(dataBuf);
        //    else {
        //        // Real receives the compilation result of virtual
        //        nise68.hmn.fb.add(fnZMS, dataBuf);
        //        nise68.hmn.fb.add(fnZMD, compiledData);
        //    }
        //}

        play();
    }

    private final List<String> preData = new ArrayList<>();
    private Path fnZMD;
    private int trp = 3 + 32;
    public int waitNextPlay = 0;
    private int rc;

    private void play() throws IOException {
logger.log(Level.INFO, "PLAY " + playingFileName + " --------");
        String fn = playingFileName;
        String withoutExtFn = fn.substring(0, fn.lastIndexOf('.') > 0 ? fn.lastIndexOf('.') : fn.length());
        Path dn = Path.of(fn).getParent();
        if (dn == null) dn = Path.of(System.getProperty("user.dir"));
        fnZMD = dn.resolve(withoutExtFn + ".ZMD");
        Path crntDir = Path.of(dir);

        Path zmsc3 = crntDir.resolve("ZMSC3.X");
        if (!Files.exists(zmsc3)) {
            throw new IllegalStateException("system file not found: %s".formatted(zmsc3));
        }
        fileMng.setVFile(zmsc3.toString());

        Path zmusic = crntDir.resolve("ZMUSIC.X"); // ver2
        if (!Files.exists(zmusic)) {
            throw new IllegalStateException("system file not found: %s".formatted(zmusic));
        }
        fileMng.setVFile(zmusic.toString());

        trp = 3 + 32;

        if (version == 2) {
            timerOPM = new FMTimer(true, null, 4000000, frequency);

            // If zpd is specified, specify zmusic to preload
            String optionZpd = "";
            String optionZmd = "";
            preData.clear();
            if (supportFileBinaryAndName != null) {
                for (Tuple<byte[], String> s : supportFileBinaryAndName) {
                    String ext = getExtension(s.getItem2()).toUpperCase();
                    if (ext.equals(".ZPD")) {
                        optionZpd = " -B" + Path.of(s.getItem2()).getFileName();
                        if (!fileMng.existsFile(s.getItem2())) {
                            fileMng.setVFile(s.getItem2(), s.getItem1());
                        }
                    }
                    if (ext.equals(".ZMD") || ext.equals(".ZMS")) {
                        String f = s.getItem2();
                        f = changeExtension(f, ".ZMD");
                        optionZmd = " -N" + Path.of(f).getFileName();
                        if (!fileMng.existsFile(f)) {
                            fileMng.setVFile(f, s.getItem1());
                            preData.add(f);
                        }
                    }
                }
            }

            nise68.hmn.memMng = new MemMng(0x0001_2000 + (9212 + 2048) * 1024 + Files.readAllBytes(zmusic).length);

            //if (nise68.loadRun(zmusic, "-P9212 -T2048" + optionZpd + optionZmd, Path.GetDirectoryName(fnZMD), 0x00012000,
            // true, true, true
            //) != 0) throw new Exception("zmusic regident Error");
            if ((rc = nise68.loadRun(zmusic.getFileName().toString(), "-P9212 -T2048" + optionZpd + optionZmd, 0x0001_2000,
                    true, true, true,
                    100_000_000, 0
            )) != 0) throw new IllegalStateException("zmusic resident Error: " + rc);

            pcm8.writePcm(nise68.mem.mem, 0, nise68.mem.mem.length);

            // play
            byte[] zmd = null;
            if (preData.isEmpty()) {
                if (Files.exists(fnZMD)) {
                    zmd = Files.readAllBytes(fnZMD);
                    //if (!nise68.hmn.fb.containsKey(fnZMD)) {
                    //    nise68.hmn.fb.add(fnZMD, zmd);
                    //}
                    if (!fileMng.existsFile(fnZMD.getFileName().toString())) {
                        fileMng.setVFile(fnZMD.getFileName().toString(), zmd);
                    }
                } else {
                    //if (nise68.hmn.fb.containsKey(fnZMD)) {
                    //    zmd = nise68.hmn.fb[fnZMD];
                    //}
                    if (fileMng.existsFile(fnZMD.getFileName().toString())) {
                        zmd = fileMng.vReadAllBytes(fnZMD.getFileName().toString());
                    }
                }
            } else {
                //if (nise68.hmn.fb.containsKey(preData[0])) {
                //    zmd = nise68.hmn.fb[preData[0]];
                //}
                if (fileMng.existsFile(preData.getFirst())) {
                    zmd = fileMng.vReadAllBytes(preData.getFirst());
                }
            }
            if (zmd == null) {
                throw new IllegalStateException("Zmd[%s] file not found. ".formatted(fnZMD));
            }
            int fileSize = zmd.length;
            int filePtr = nise68.hmn.memMng.malloc(fileSize);
            for (int i = 0; i < zmd.length; i++) {
                nise68.mem.pokeB(filePtr + i, zmd[i]);
            }

            nise68.reg.setDl(1, 0x11); // play_cnv_data
            nise68.reg.setDl(2, zmd.length - 7);
            nise68.reg.setAl(1, filePtr + 7);
            nise68.trap(trp); // , true, true, true);

            return;
        }

        // zmsc3 resident
        nise68.hmn.memMng = new MemMng(0x0004_0000);

        //if (nise68.loadRun(zmsc3, "-w", Path.getDirectoryName(fnZMD), 0x0001_2000,
        //      true, true, true
        //) != 0) throw new IllegalStateException("zmsc3 resident Error");
        if ((rc = nise68.loadRun(zmsc3.getFileName().toString(), "-w", 0x0001_2000,
                true, true, true,
                100_000_000, 0
        )) != 0) throw new IllegalStateException("zmsc3 resident Error: " + rc);

        // play

        //if ((rc = nise68.LoadRun("C:\\ZP3.R", "-PC:\\SAMPLE1\\SAMPLE.ZMS", "C:\\", 0x00042000,
        //    true, true, true
        //)) != 0) Environment.Exit(rc);
        {
            // Error stock buffer release
            nise68.reg.setDl(0, 0x73); // ZM_FREE_MEM2
            nise68.reg.setDl(3, 0x82645252); // 'ＥRR'
            nise68.trap(trp);
        }
        {
            // ZMD release
            nise68.reg.setDl(0, 0x73); // ZM_FREE_MEM2
            nise68.reg.setDl(3, 0x5a826c44); // 'ZＭD'
            nise68.trap(trp);
        }
        {
            // ZM_COMPILER DETECT
            nise68.reg.setDl(0, 0x6b); // ZM_HOOK_FNC_SERVICE
            nise68.reg.setDl(1, 2); // ZM_COMPILER
            nise68.reg.setAl(1, 0xffff_ffff); // detect_mode
            nise68.trap(trp);
        }
        {
            // Version check (just call)
            nise68.reg.setDl(0, 0x7e); // ZM_ZMUSIC_MODE
            nise68.reg.setDl(1, 0xffff_ffff); //
            nise68.trap(trp);
        }
        {
            // play
            byte[] zmd = fileMng.vReadAllBytes(fnZMD.getFileName().toString()); // nise68.hmn.fb[fnZMD];
logger.log(Level.TRACE, zmd.length + " bytes\n" + StringUtil.getDump(zmd, 32));
if (!Arrays.equals(zmd, 1, 7, magic, 0, 6)) { throw new IllegalStateException("output file is not .zmd"); }
            int fileSize = zmd.length;
            int filePtr = nise68.hmn.memMng.malloc(fileSize);
            for (int i = 0; i < zmd.length; i++) {
                nise68.mem.pokeB(filePtr + i, zmd[i]);
            }
            nise68.reg.setDl(0, 0x10); // ZM_PLAY_ZMD
            nise68.reg.setDl(2, fileSize);
            nise68.reg.setAl(1, filePtr + 8);
            nise68.trap(trp);
        }
        {
            // Check for errors
            nise68.reg.setDl(0, 0x6e); // ZM_STORE_ERROR
            nise68.reg.setDl(1, 0xffff_ffff);
            nise68.reg.setDl(2, 0x0000_0000);
            nise68.trap(trp);
            // The number of errors is returned in D0.l
        }
    }

    public boolean compile(byte[] vgmBuf, String fn) {
logger.log(Level.INFO, "COMPILE v3 --------");
        String withoutExtFn = fn.substring(0, fn.lastIndexOf('.') > 0 ? fn.lastIndexOf('.') : fn.length());
        Path dn = Path.of(fn).getParent();
        if (dn == null) dn = Path.of(System.getProperty("user.dir"));
        Path fnZMD = dn.resolve(withoutExtFn + ".ZMD");
        Path fnZMS = dn.resolve(withoutExtFn + ".ZMS");
        Path zmc = Path.of(dir, "ZMC.X");
        if (!Files.exists(zmc)) {
            throw new IllegalStateException("system file not found: %s".formatted(zmc));
        }
        fileMng = new FileMng(dn.toString(), "C:"); // Set the path of the music file to the current physical drive. The current virtual drive is "C:" (default).
        fileMng.setVFile(zmc.toString());

        nise68 = new Nise68();
        nise68.setMPcm(this::mPcmCallBack);
        nise68.setOpm(this::opmCallBack);
        nise68.setMidi(this::midiCallBack, frequency);
        nise68.setSCC_A(this::sccCallBack, frequency);
        nise68.init(null, false, fileMng, charset);

        // compile
        //nise68.hmn.fb.add(fnZMS, dataBuf);
        fileMng.setVFile(fnZMS.toString(), vgmBuf);
        //if (nise68.LoadRun(zmc, Path.GetFileName(fnZMS), Path.GetDirectoryName(fnZMS), 0x00012000,
        // true, true, true
        // ) != 0)
        if (nise68.loadRun(zmc.getFileName().toString(), fnZMS.getFileName().toString(), 0x0001_2000,
                true, true, true,
                100_000_000, 0
        ) != 0) {
            logger.log(Level.INFO, "v3 Compile Error " + zmc);
            return false;
        }
        //compiledData = nise68.hmn.fb[fnZMD];
        compiledData = fileMng.vReadAllBytes(fnZMD.getFileName().toString());
logger.log(Level.INFO, compiledData.length + " bytes\n" + StringUtil.getDump(compiledData, 32));
if (!Arrays.equals(compiledData, 1, 7, magic, 0, 6) || compiledData.length <= 80) { logger.log(Level.WARNING, "compile v3: output is not correct .zmd"); return false; }
        return true;
    }

    private static final byte[] magic = {0x5A, 0x6D, 0x75, 0x53, 0x69, 0x43};

    public boolean compileV2(byte[] vgmBuf, String fn) {
logger.log(Level.INFO, "COMPILE v2 --------");
        String withoutExtFn = fn.substring(0, fn.lastIndexOf('.') > 0 ? fn.lastIndexOf('.') : fn.length());
        Path dn = Path.of(fn).getParent();
        if (dn == null) dn = Path.of(System.getProperty("user.dir"));
        Path fnZMD = dn.resolve(withoutExtFn + ".ZMD");
        Path fnZMS = dn.resolve(withoutExtFn + ".ZMS");
        Path zmusic = Path.of(dir, "ZMUSIC.X");
        if (!Files.exists(zmusic)) {
            throw new IllegalStateException("system file not found: %s".formatted(zmusic));
        }

        nise68 = new Nise68();
        nise68.setMPcm(this::pcm8CallBack);
        nise68.setOpm(this::opmCallBack);
        nise68.setMidi(this::midiCallBack, frequency);
        nise68.setSCC_A(this::sccCallBack, frequency);

        fileMng = new FileMng(dn.toString(), "C:"); // Set the path of the music file to the current physical drive. The current virtual drive is "C:" (default).
        fileMng.setVFile(zmusic.toString());

        nise68.init(null, false, fileMng, charset);

        // compile
        //nise68.hmn.fb.add(fnZMS, dataBuf);
        fileMng.setVFile(fnZMS.getFileName().toString(), vgmBuf);
        //if (nise68.loadRun(zmusic, "-C " + Path.getFileName(fnZMS), Path.getDirectoryName(fnZMS), 0x00012000,
        // true, true, true
        //) != 0)
        if (nise68.loadRun(zmusic.getFileName().toString(), "-C " + fnZMS.getFileName(), 0x00012000,
                true, true, true,
                100_000_000, 0
        ) != 0) {
            logger.log(Level.INFO, "v2 Compile Error", zmusic);
            return false;
        }
        //compiledData = nise68.hmn.fb[fnZMD];
        compiledData = fileMng.vReadAllBytes(fnZMD.getFileName().toString());
if (!Arrays.equals(compiledData, 1, 7, magic, 0, 6) || compiledData.length <= 80) { logger.log(Level.WARNING, "compile v2: output is not correct .zmd"); return false; }

        return true;
    }

    private int mPcmCallBack(int n) {
        int ch = n & 0xf;
        switch (n & 0xfff0) {
            case 0x0000:
                //logger.log(Level.TRACE, "MPCM #M_KEY_ON($%04x)".formatted(n));
                mpcm.keyOn(ch);
                mpcmSt[ch].keyOn = true;
                break;
            case 0x0100:
                //logger.log(Level.TRACE, "MPCM #M_KEY_OFF($%04x)".formatted(n));
                mpcm.keyOff(ch);
                mpcmSt[ch].keyOff = true;
                break;
            case 0x0200:
                //logger.log(Level.TRACE, "MPCM #M_SET_PCM($%04x)".formatted(n));
                mpcm.writePcm(ch, mpcmSt, nise68.mem, nise68.reg, n);
                break;
            case 0x0300:
                //logger.log(Level.TRACE, "MPCM #M_SET_FRQ($%04x) D1$%08x".formatted(n, nise68.reg.GetDl(1)));
                mpcm.setFreq(ch, nise68.reg.getDl(1));
                mpcmSt[ch].frq = nise68.reg.getDl(1);
                break;
            case 0x0400:
                //logger.log(Level.TRACE, "MPCM #M_SET_PITCH($%04x) D1$%04x".formatted(n, nise68.reg.GetDl(1)));
                // a word, note << 6 | fine: A4 is $1140 (a byte left only the fine part)
                mpcm.setPitch(ch, nise68.reg.getDl(1));
                mpcmSt[ch].pitch = nise68.reg.getDl(1);
                break;
            case 0x0500:
                //logger.log(Level.TRACE, "MPCM #M_SET_VOL($%04x) = $%02x".formatted(n, nise68.reg.GetDb(1)));
                mpcm.setVol(ch, nise68.reg.getDb(1) & 0xff);
                mpcmSt[n & 0xf].volume = nise68.reg.getDb(1) & 0xff;
                break;
            case 0x0600:
                //logger.log(Level.TRACE, "MPCM #M_SET_PAN($%04x) = $%02x".formatted(n, nise68.reg.GetDb(1)));
                mpcm.setPan(ch, nise68.reg.getDb(1) & 0xff);
                mpcmSt[n & 0xf].pan = nise68.reg.getDb(1) & 0xff;
                break;
            case 0x8000: //
                switch (n & 0x000f) {
                    case 0x0:
                        //logger.log(Level.TRACE, "MPCM #M_LOCK($%04x)".formatted(n));
                        break;
                    case 0x2: //
                        //logger.log(Level.TRACE, "MPCM #M_INIT($%04x)".formatted(n));
                        mpcm.reset();
                        break;
                    case 0x5: //
                        //logger.log(Level.TRACE, "MPCM #M_SET_VOLTBL($%04x)".formatted(n));
                        int[] vtbl = new int[128];
                        for (int i = 0; i < 128; i++) {
                            vtbl[i] = nise68.mem.peekW(nise68.reg.getAl(1) + (i * 2)) & 0xffff;
                        }
                        mpcm.setVolTable(nise68.reg.getDl(1), vtbl);
                        break;
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return 0;
    }

    private int pcm8CallBack(int n) {
        int ch;
        switch (n & 0xfff0) {
            case 0x0000:
                //File.WriteAllBytes("c:\\temp\\test.bin", nise68.mem.mem);
                pcm8.keyOn(n & 0xff, nise68.reg.getAl(1), nise68.reg.getDl(1), nise68.reg.getDl(2)); // Start of specified channel sound
                //logger.log(Level.TRACE, "%d adrsPtr = 0x%08x;  mode = 0x%08x; len = 0x%08x;".formatted(n & 0xff, nise68.reg.getAl(1), nise68.reg.getDl(1), nise68.reg.getDl(2)));
                ch = (n & 0xff) % 8;
                pcm8St[ch].tablePtr = nise68.reg.getAl(1);
                pcm8St[ch].mode = nise68.reg.getDl(1);
                pcm8St[ch].length = nise68.reg.getDl(2);
                pcm8St[ch].keyOn = true;
                break;
            case 0x0100:
                switch (n & 0xffff) {
                    case 0x0100:
                        ch = (n & 0xff) % 8;
                        pcm8St[ch].tablePtr = 0;
                        pcm8St[ch].mode = 0;
                        pcm8St[ch].length = 0;
                        pcm8St[ch].keyOn = false;
                        pcm8.keyOff(n & 0xff); // Stop the specified channel
                        break;
                    case 0x0101:
                        pcm8.abort();
                        break;
                }
                break;
            case 0x01f0:
                switch (n & 0xffff) {
                    case 0x01FC:
                        nise68.reg.setDl(0, 1); // Stop all channels
                        break;
                }
                break;
            default:
                break;
        }
        return 0;
    }

    private int opmCallBack(int adr, int dat) {
        ym2151Write.accept(adr, dat);
        if (timerOPM != null) timerOPM.writeReg((byte) adr, (byte) dat);
        return 0;
    }

    private int midiCallBack(int n, byte dat) {
        //if (midiOutsBuff == null || midiOutsFrame == null) return 0;
        //if (n >= midiOutsBuff.count) return 0;

        //midiOutsBuff[n].add(dat);
        //midiOutsFrame[n].add(virtualFrameCounter);
        midiSend.accept(n, new byte[] {dat});
        return 0;
    }

    private int sccCallBack(int n, byte dat) {
        //if (midiOutsBuff == null || midiOutsFrame == null) return 0;
        //if (2 + n >= midiOutsBuff.count) return 0;

        //midiOutsBuff[2 + n].add(dat);
        //midiOutsFrame[2 + n].add(virtualFrameCounter);
        midiSend.accept(2 + n, new byte[] {dat});
        return 0;
    }

    public void setZPDSearchPath() {
        try {
            // Get the environment variable "ZPD"
            if (zpd != null && !zpd.isEmpty()) {
                dirZPDs = Arrays.asList(zpd.split(";"));
            }
        } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
            dirZPDs = Collections.emptyList();
        }
    }
}
