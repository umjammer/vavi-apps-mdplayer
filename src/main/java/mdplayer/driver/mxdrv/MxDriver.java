package mdplayer.driver.mxdrv;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mdplayer.Common;
import mdplayer.Common.EnmModel;
import mdplayer.chips.Pcm8Chip;
import mdplayer.chips.Ym2151Chip;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.lib.mxdrv.Depend;
import mdplayer.lib.mxdrv.MXDRV;
import mdplayer.lib.mxdrv.MXDRV.MXDRV_WORK;
import mdplayer.lib.mxdrv.MXDRV.MXWORK_CH;
import mdplayer.lib.mxdrv.MXDRV.MdxPcmInterface;
import mdplayer.lib.mxdrv.MXDRV.Pcm8Interface;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;
import vavi.util.ByteUtil;
import vavi.util.compat.Tuple;

import static java.lang.System.getLogger;


/**
 * MxDriver
 *
 * @author kumatan
 */
public class MxDriver extends BaseDriver {

    private static final Logger logger = getLogger(MxDriver.class.getName());

    private final MXDRV mxdrv;

    private Tuple<String, byte[]> extendFile = null;

    public MxDriver(BasePlugin<? extends BaseDriver> plugin) {
        super(plugin);

        this.mxdrv = new MXDRV();
        // called the same timing as mdxPcm.getPcm
        mxdrv.ym2151Write = (a, d) -> plugin.chipRegister.chip(Ym2151Chip.class).write(0, 0, a, d, model, plugin.chipRegister.chip(Ym2151Chip.class).corrections[0], frameCounter);
        mxdrv.isFromDF = Pcm8Chip::isFromDF;
        mxdrv.isFromPTM = Pcm8Chip::isFromPTM;
        mxdrv.mdxPCM = new MdxPcmInterface() {
            @Override
            public int getPcm(short[] buffer, int offset, int length) {
                return plugin.chipRegister.chip(Pcm8Chip.class).getPcm(0, buffer, offset, length);
            }

            @Override
            public int start(int sampleRate, int opmFlag, int adpcmFlag, int betw, int pcmBuf, int late, double rev) {
                return plugin.chipRegister.chip(Pcm8Chip.class).start(0, sampleRate, opmFlag, adpcmFlag, betw, pcmBuf, late, rev);
            }

            @Override
            public int startPcm(int sampleRate, int opmFlag, int adpcmFlag, int pcmBuf) {
                return 0; // done directly at mds.start
            }

            @Override
            public void initIocs() {
                plugin.chipRegister.chip(Pcm8Chip.class).initIocs(0);
            }

            @Override
            public void opmInt(Runnable func) {
                plugin.chipRegister.chip(Pcm8Chip.class).opmInt(0, func);
            }

            @Override
            public int opmWait(int wait) {
                return plugin.chipRegister.chip(Pcm8Chip.class).opmWait(0, wait);
            }

            @Override
            public int totalVolume(int vol) {
                return plugin.chipRegister.chip(Pcm8Chip.class).totalVolume(0, vol);
            }

            @Override
            public void free() {
                plugin.chipRegister.chip(Pcm8Chip.class).stop(0);
            }

            @Override
            public void abort() {
                plugin.chipRegister.chip(Pcm8Chip.class).abort(0);
            }

            @Override
            public void opmSetIocs(int addr, int data) {
                plugin.chipRegister.chip(Pcm8Chip.class).write(0, 0, addr, data, model);
            }

            @Override
            public void keyOnAdpcm(int addr, int mode, int len) {
                plugin.chipRegister.chip(Pcm8Chip.class).keyOnAdpcm(0, addr, mode, len);
            }

            @Override
            public void adpcmMod(int mode) {
                plugin.chipRegister.chip(Pcm8Chip.class).adpcmMod(0, mode);
            }
        };
        mxdrv.pcm8pp = new Pcm8Interface() {
            @Override
            public void writePcm(byte[] pcm, int offset, int length) {
                // done directly at #init
            }

            @Override
            public void keyOn(int ch, int adr, int mode, int len) {
                plugin.chipRegister.chip(Pcm8Chip.class).keyOn(0, ch, adr, mode, len);
            }

            @Override
            public void keyOff(int ch) {
                plugin.chipRegister.chip(Pcm8Chip.class).keyOff(0, ch);
            }

            @Override
            public void abort() {
                plugin.chipRegister.chip(Pcm8Chip.class).abort(0);
            }
        };
    }

    public MxDriver() {
        this(null); // gross
    }

    public void setExtendFile(Tuple<String,byte[]> extendFile) {
        this.extendFile = extendFile;
    }

    /**
     * The driver itself, whose work area is the only place an MDX's PCM parts exist: they are
     * played through PCM8, which reports nothing back.
     *
     * @see mdplayer.fmdsp.MdxPcmReader
     */
    public MXDRV getMxdrv() {
        return mxdrv;
    }

    @Override
    public MetaData retrieveMetaData(byte[] buf, Object... args) {
        MetaData md = new MetaData();

        List<Byte> lst = new ArrayList<>();
        int i = 0;
        while (i < buf.length && (buf[i] != 0xd && buf[i] != 0xa)) {
            lst.add(buf[i]);
            i++;
        }
        String n = new String(ByteUtil.toByteArray(lst), Common.charset);
        md.set(Tag.Title, n);
        md.set(Tag.TitleJ, n);
        byte[][] mdx = new byte[1][];
        int[] mdxSize = new int[1];
        String[] pdxFileName = new String[1];
        makeMdxBuf(buf, mdx, mdxSize, pdxFileName);

        return md;
    }

    /**
     * @param mdx OUT
     * @param mdxSize OUT
     * @param pdxFileName OUT
     */
    static void makeMdxBuf(byte[] buf, byte[][] mdx, int[] mdxSize, String[] pdxFileName) {
        // a song packed by LZX.X keeps its header and hides its sequence behind a 68000 stub, so
        // it has to be expanded before any of the rest of this can read it (returns buf as it is
        // for a song that is not packed)
        buf = Lzx.expand(buf);

        // Skip title
        int p = 8;
        int c;
        mdxSize[0] = buf.length;
        mdx[0] = new byte[buf.length + 8];
        System.arraycopy(buf, 0, mdx[0], 8, mdxSize[0]);

        while (true) {
            c = mdx[0][p++] & 0xff;
            if (c == 0x0d) break;
            if (c == 0x0a) break;
            if (c < 0x20) {
                if (c != 0x1b) throw new IllegalArgumentException("illegal char: " + Integer.toHexString(c));
            }
        }
        p--;
        mdx[0][p++] = 0x00;
        if ((p & 0x01) != 0) {
            mdx[0][p++] = 0x00;
        }
        int p2 = p;
        if (c != 0x0d) {
            while (mdx[0][p2++] != 0x0d) ;
        }

        // Load PDX
        byte havePdx = (byte) 0xff;
        List<Byte> lstPdxFileName;
        while (mdx[0][p2++] != 0x1a) ;
        if (mdx[0][p2] != 0) {
            havePdx = 0x00;
            lstPdxFileName = new ArrayList<>();
            while (mdx[0][p2] != 0x00) {
                lstPdxFileName.add(mdx[0][p2]);
                p2++;
            }
            pdxFileName[0] = new String(ByteUtil.toByteArray(lstPdxFileName), Common.charset);
        }
        p2++;

        // Process MDX so that it can be passed to MXDRV
        int mdxBodyPtr = p;
        while (p2 < mdx[0].length) {
            mdx[0][p++] = mdx[0][p2++];
        }
        mdxSize[0] = p;

        mdx[0][0] = 0x00;
        mdx[0][1] = 0x00;
        mdx[0][2] = havePdx;
        mdx[0][3] = havePdx;
        mdx[0][4] = (byte) ((mdxBodyPtr & 0xffff) >>> 8);
        mdx[0][5] = (byte) (mdxBodyPtr & 0xff);
        mdx[0][6] = 0x00;
        mdx[0][7] = 0x08;
    }

    /** how many part offsets follow the voice data offset at the head of an MDX body */
    private static final int MDX_PARTS = 9;

    /**
     * Rejects a file whose body is not MDX sequence data at all.
     * <p>
     * The body opens with the offset of the voice data and one offset per part, all counted from
     * the body itself, and nothing in the driver ever questions one. A file carrying something
     * else there sends it reading wherever those bytes happen to point, which is where the endless
     * {@code index is out of bounds} such a file used to play as comes from.
     * <p>
     * A song that cannot play should say so rather than sound like a broken driver. The one kind
     * of file this used to be aimed at - an MDX packed by {@code LZX.X}, which keeps its title and
     * its PDX name and has a 68000 stub where the sequence should be - plays now: {@link Lzx}
     * expands it on the way in, well before this sees it.
     * <p>
     * Only what cannot be sequence data at all is refused. The offsets themselves are deliberately
     * not checked against the length of the body: plenty of MDXs that play perfectly well point a
     * part or their voice data just past their own data - an empty part is written that way, and
     * an unused one as an offset with the top bit set - and the driver reads the zeroes that
     * follow as rests. Refusing those would lose songs to a stricter reading than MXDRV's own.
     *
     * @param mdx the buffer {@link #makeMdxBuf} built, its body offset in bytes 4 and 5
     * @param size how much of it {@code makeMdxBuf} filled
     * @throws IllegalStateException if the body cannot be sequence data
     */
    private static void checkMdx(byte[] mdx, int size) {
        int body = ByteUtil.readBeShort(mdx, 4) & 0xffff;
        int length = size - body;
        if (length < (1 + MDX_PARTS) * 2) {
            throw new IllegalArgumentException("Not MDX data: the body is only %d bytes.".formatted(length));
        }
    }

    /**
     * @param pdx OUT
     * @param pdxSize OUT
     */
    private void makePdxBuf(String pdxFileName, byte[][] pdx, int[] pdxSize) {
        if (extendFile == null) {
            logger.log(Level.DEBUG, "extendFile is null");
            return;
        }

        pdx[0] = new byte[extendFile.getItem2().length + pdxFileName.length() + 8 + 1];
        System.arraycopy(pdxFileName.getBytes(StandardCharsets.US_ASCII), 0, pdx[0], 8, pdxFileName.length());
        System.arraycopy(extendFile.getItem2(), 0, pdx[0], 8 + pdxFileName.length() + 1, extendFile.getItem2().length);
        pdx[0][0] = 0x00;
        pdx[0][1] = 0x00;
        pdx[0][2] = 0x00;
        pdx[0][3] = 0x00;
        pdx[0][4] = (byte) (((8 + pdxFileName.length() + 2) & 0xffff_fffe) >>> 8);
        pdx[0][5] = (byte) ((8 + pdxFileName.length() + 2) & 0xffff_fffe);
        pdx[0][4] = (byte) (((8 + pdxFileName.length() + 1) & 0xffff) >>> 8);
        pdx[0][5] = (byte) ((8 + pdxFileName.length() + 1) & 0xff);
        pdx[0][6] = (byte) (((pdxFileName.length() + 1) & 0xff00) >>> 8);
        pdx[0][7] = (byte) ((pdxFileName.length() + 1) & 0xff);
        pdxSize[0] = pdx[0].length;
    }

    public static void getPDXFileName(byte[] buf, String[] pdx, Charset charset) {
        int p = 0;
        int c;
        while (true) {
            c = buf[p++] & 0xff;
            if (c == 0x0d || c == 0x0a) break;
            if (c < 0x20 && c != 0x1b) throw new IllegalArgumentException("illegal char: " + Integer.toHexString(c));
        }
        if ((p & 0x01) != 0) p++;
        if (c != 0x0d) while (buf[p++] != 0x0d) ;
        while (buf[p++] != 0x1a) ;
        if (buf[p] == 0) return;
        List<Byte> lstPdxFileName = new ArrayList<>();
        while (buf[p] != 0x00) lstPdxFileName.add(buf[p++]);
        pdx[0] = new String(ByteUtil.toByteArray(lstPdxFileName), charset);
    }

    @Override
    public void init(EnmModel model, int latency, int waitTime, Object... args) {
        this.model = model;
        this.latency = latency;
        this.waitTime = waitTime;

        metaData = retrieveMetaData(dataBuf);
        counter = 0;
        totalCounter = 0;
        loopCounter = 0;
        curLoop = 0;
        stopped = false;
        frameCounter = -latency - waitTime;
        speed = 1;

        plugin.chipRegister.chip(Ym2151Chip.class).setCorrection(model, 4000000);

        byte[][] mdx = new byte[1][];
        int[] mdxSize = new int[1];
        byte[][] pdx = new byte[1][];
        int[] pdxSize = new int[1];
        String[] pdxFileName = new String[1];
        makeMdxBuf(dataBuf, mdx, mdxSize, pdxFileName);
        checkMdx(mdx[0], mdxSize[0]);
        makePdxBuf(pdxFileName[0], pdx, pdxSize);
        if ((pdxFileName[0] != null && !pdxFileName[0].isEmpty()) && pdx[0] == null) {
            logger.log(Level.WARNING, "pdxFileName: %s, pdx: %s".formatted(pdxFileName[0], pdx[0]));
            throw new IllegalStateException("Failed to load PCM file [%s].".formatted(pdxFileName[0]));
        }

        // PCM8PP takes a wider F than stock MXDRV passes on, see Pcm8Detector
        mxdrv.pcmFormatMask = setting.pcm8Type(plugin) == Pcm8Chip.PCM8PP ? 0xfc : 0x1c;

        int ret;
        if (model == EnmModel.VirtualModel) {
            ret = mxdrv.MXDRV_Start(setting.getOutputDevice().getSampleRate(), 0, 0, 0, mdxSize[0], pdxSize[0], 0, -1, 1);
        } else {
            ret = mxdrv.MXDRV_Start(setting.getOutputDevice().getSampleRate(), 0, 0, 0, mdxSize[0], pdxSize[0], 0, -1, -1);
        }

        int[] mdxPtr = new int[1], pdxPtr = new int[1];
        mxdrv.initializeMemory(mdxSize[0], pdxSize[0], mdx[0], pdx[0], mdxPtr, pdxPtr);

        // one call, whichever back end is behind it: writePcm already dispatches on the setting,
        // so the second call this used to make when it was 1 only mounted PCM8PP's memory twice
        plugin.chipRegister.chip(Pcm8Chip.class).writePcm(0, 0, 0, mxdrv.getMemory().mm, model);

        // a song that loops with a repeat around the whole part instead of a jump at its end has
        // no loop to count without this, and neither the measurement below nor the player's loop
        // limit would ever come round
        mxdrv.MXDRV_RepeatIsLoop(true);

        int playtime = mxdrv.MXDRV_MeasurePlayTime(mdx[0], mdxSize[0], mdxPtr[0], pdx[0], pdxSize[0], pdxPtr[0], 1, Depend.TRUE);
//logger.log(Level.TRACE, "(%d:%02d) %d".formatted(playtime / 1000 / 60, playtime / 1000 % 60, ""));
        // the sample counter this is compared against ({@link #counter}) runs at the player's own
        // rate, not the device's - counting it at the device rate made the total time and both
        // progress bars run long by whatever the device is tuned away from 44100
        totalCounter = (long) playtime * Common.VGMProcSampleRate / 1000;
        // the player decides how many loops to play (Setting.Other#loopTimes), the driver only reports them
        mxdrv.MXDRV_PlaySetup(Integer.MAX_VALUE, false);
        mxdrv.MXDRV_Play(mdx[0], mdxSize[0], mdxPtr[0], pdx[0], pdxSize[0], pdxPtr[0]);
logger.log(Level.TRACE, "MXDRV_Start: " + ret);
    }

    /** ticks {@link #clock} runs the OPM timer on, scaled by {@link #speed} */
    private double opmCounter;

    /**
     * Runs the OPM timer the sequencer is driven by, held off until the player has mixed the
     * latency it starts out with.
     * <p>
     * X68Sound calls this back only while it renders at the output rate; the chip here runs at
     * its own 62500 Hz and takes the path that ticks the timer itself, so this is dead for most
     * songs. The sample counter must not be counted here for that reason - see
     * {@link #processOneFrame()}.
     *
     * @param firstFlg true on the first OPM tick of an output sample (unused, kept for the callback)
     */
    public void clock(Runnable timer, boolean firstFlg) {
        try {
            opmCounter += speed;
            while (opmCounter >= 1.0) {
                opmCounter -= 1.0;
                if (frameCounter > -1) {
                    timer.run();
                }
            }
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    public void processOneFrame() {
        //logger.log(Level.TRACE, "0:%08x".formatted(mm.readint(MXWORK_CHBUF_FM[8] + MXWORK_CH.S0012)));
        //logger.log(Level.TRACE, "1:%04x".formatted(mm.readshort(MXWORK_CHBUF_PCM[0] + MXWORK_CH.S0012) >> 6));
        //logger.log(Level.TRACE, "1a:%04x".formatted(mm.readshort(MXWORK_CHBUF_PCM[0] + MXWORK_CH.S0014) >> 6));
        //logger.log(Level.TRACE, "2:%d".formatted(mm.readint(MXWORK_CHBUF_PCM[1] + MXWORK_CH.S0004)));
        //logger.log(Level.TRACE, "3:%d".formatted(mm.readint(MXWORK_CHBUF_PCM[2] + MXWORK_CH.S0004)));
        //logger.log(Level.TRACE, "4:%d".formatted(mm.readint(MXWORK_CHBUF_PCM[3] + MXWORK_CH.S0004)));
        //logger.log(Level.TRACE, "5:%d".formatted(mm.readint(MXWORK_CHBUF_PCM[4] + MXWORK_CH.S0004)));
        //logger.log(Level.TRACE, "6:%d".formatted(mm.readint(MXWORK_CHBUF_PCM[5] + MXWORK_CH.S0004)));
        //logger.log(Level.TRACE, "7:%d".formatted(mm.readint(MXWORK_CHBUF_PCM[6] + MXWORK_CH.S0004)));

        // the mixer calls this once per rendered sample, which is the only tick this driver is
        // sure to get: the PCM8 chip clocks it back (#clock) only on the X68Sound path that
        // renders at the output rate, and the chip runs at 62500 Hz, which takes the other one.
        // Left uncounted, the elapsed time on the display stands still a fifth of a second in.
        speedCounter += (double) Common.VGMProcSampleRate / setting.getOutputDevice().getSampleRate() * speed;
        while (speedCounter >= 1.0) {
            speedCounter -= 1.0;
            counter++;
            frameCounter++;
        }

        curLoop = mxdrv.loopCount;
        if (mxdrv.terminatePlay) {
            stopped = true;
        }

        byte[] mm = mxdrv.getMemory().mm;
        int[] fms = (int[]) mxdrv.MXDRV_GetWork(MXDRV_WORK.FM);
        Map<String, Integer>[] fmMaps = new Map[fms.length];
        for (int i = 0; i < fms.length; i++) {
            fmMaps[i] = new HashMap<>();
            fmMaps[i].put("note", ByteUtil.readBeShort(mm, fms[i] + MXWORK_CH.S0012) & 0xffff);
            fmMaps[i].put("volume", mm[fms[i] + MXWORK_CH.S0022] & 0xff);
            fmMaps[i].put("keyOn", mm[fms[i] + MXWORK_CH.S0016] & 0xff);
            fmMaps[i].put("note2", ByteUtil.readBeShort(mm, fms[i] + MXWORK_CH.S0014) & 0xffff);
        }
        int[] pcms = (int[]) mxdrv.MXDRV_GetWork(MXDRV_WORK.PCM);
        Map<String, Object>[] pcmMaps = new Map[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            pcmMaps[i] = new HashMap<>();
            pcmMaps[i].put("note", ByteUtil.readBeShort(mm, pcms[i] + MXWORK_CH.S0012) & 0xffff);
            pcmMaps[i].put("volume", mm[pcms[i] + MXWORK_CH.S0022] & 0xff);
            pcmMaps[i].put("keyOn", mm[pcms[i] + MXWORK_CH.S0016] & 0xff);
        }
        fireEventHappened(this, "mxdrv",
                plugin.chipRegister.chip(Ym2151Chip.class).register[0], fmMaps, pcmMaps);
    }

    @Override
    public String getName() {
        return "MXDRV";
    }
}
