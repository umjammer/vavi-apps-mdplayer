package mdplayer.lib.nsf;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;

import mdsound.np.DCFilter;
import mdsound.np.Device;
import mdsound.np.Filter;
import mdsound.np.LoopDetector;
import mdsound.np.chip.NesApu;
import mdsound.np.chip.NesDmc;
import mdsound.np.chip.NesFds;
import mdsound.np.chip.NesFme7;
import mdsound.np.chip.NesMmc5;
import mdsound.np.chip.NesN106;
import mdsound.np.chip.NesVrc6;
import mdsound.np.chip.NesVrc7;
import mdsound.np.cpu.Km6502;
import mdsound.np.memory.NesBank;
import mdsound.np.memory.NesMem;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


/** Nsf player powered by np */
public class Nsf {

    private static final Logger logger = getLogger(Nsf.class.getName());

    private static final int NsfClock = 1789773;

    public static final int FCC_NSF = 0x4d53454e; // "NESM"

    public Function<Integer, Map<String, Object>> getVolume;
    public Consumer<byte[]> setOptions;
    public BiConsumer<Short, Short> enq;

    public NesBank bank = null;
    public NesMem mem = null;
    private Km6502 cpu = null;

    public NesApu apu = null;
    public NesDmc dmc = null;
    public NesFds fds = null;
    public NesN106 n106 = null;
    public NesVrc6 vrc6 = null;
    public NesMmc5 mmc5 = null;
    public NesFme7 fme7 = null;
    public NesVrc7 vrc7 = null;

    private int version;
    public int songs;
    private int start;
    private int load_address;
    private int initAddress;
    private int playAddress;
    private String filename;
    // margin 64 chars.
    private String printTitle;
    private String title_nsf;
    private String artist_nsf;
    private String copyrightNsf;
    public String title;
    public String artist;
    public String copyright;
    // NSFe only
    private String ripper;
    // NSFe only
    private String text;
    // NSFe only
    private int text_len;
    private int speedNtsc;
    private final byte[] bankSwitch = new byte[8];
    private int speedPal;
    private int palNtsc;
    private int soundChip;
    public boolean useVrc7;
    public boolean useVrc6;
    public boolean useFds;
    public boolean useFme7;
    public boolean useMmc5;
    public boolean useN106;
    private final byte[] extra = new byte[4];
    private byte[] body;
    private int bodySize;
    private byte[] nsfeImage;
    public int[] nsfePlst;
    public int nsfePlstSize;
    private static final int NSFE_ENTRIES = 256;

    private static class NsfeEntry {

        public int[] tlbl;
        public int time;
        public int fade;
    }

    public NsfeEntry[] nsfeEntry = new NsfeEntry[NSFE_ENTRIES];

    /**
     * Currently selected track number
     */
    public int song;

    private Device.Layer layer;
    /** DC filter applied to the final output stage */
    private DCFilter dcf;
    /** Low-pass filter applied to the final output */
    private Filter lpf;

    private LoopDetector.NESDetector ld = null;
//    private NESDetectorEx ld = null;

    public int sampleRate;
    private double cpu_clock_rest;
    private double apu_clock_rest;
    private int time_in_ms;
    /**
     * Fractional part of {@link #time_in_ms}. {@code Audio} renders in chunks of two samples, and
     * two samples are 0.045 ms: truncating that to an int per call left the clock at 0 forever, so
     * {@link #detectLoop} never got past its {@code match_interval} guard and the song never ended.
     */
    private double time_in_ms_rest;
    private long silent_length = 0;
    private int last_out = 0;

    public boolean isRealModel;
    public Consumer<Boolean> updateAtTrail;
    public Runnable updateAtMiddle;
    public BiConsumer<Long, Long> updateAtDetectLoop;
    public LongConsumer updateAtDetectSilent;
    public DoubleSupplier speed;
    public IntSupplier getCounter;
    public IntConsumer incCounter;

    public void initInfo(byte[] buf, Charset charset) {
        version = buf[0x05] & 0xff;
        songs = buf[0x06] & 0xff;
        start = buf[0x07] & 0xff;
        load_address = (buf[0x08] & 0xff) | ((buf[0x09] & 0xff) << 8);
        initAddress = (buf[0x0a] & 0xff) | ((buf[0x0B] & 0xff) << 8);
        playAddress = (buf[0x0c] & 0xff) | ((buf[0x0D] & 0xff) << 8);

        List<Byte> strLst = new ArrayList<>();
        int tagAdr = 0x0e;
        for (int i = 0; i < 32; i++) {
            if (buf[tagAdr] == 0) break;
            strLst.add(buf[tagAdr++]);
        }
        title_nsf = new String(ByteUtil.toByteArray(strLst), charset);
        title = title_nsf;

        strLst.clear();
        tagAdr = 0x2e;
        for (int i = 0; i < 32; i++) {
            if (buf[tagAdr] == 0) break;
            strLst.add(buf[tagAdr++]);
        }
        artist_nsf = new String(ByteUtil.toByteArray(strLst), charset);
        artist = artist_nsf;

        //memcpy(copyright_nsf, image + 0x4e, 32);
        //copyright_nsf[31] = '\0';
        strLst.clear();
        tagAdr = 0x4e;
        for (int i = 0; i < 32; i++) {
            if (buf[tagAdr] == 0) break;
            strLst.add(buf[tagAdr++]);
        }
        copyrightNsf = new String(ByteUtil.toByteArray(strLst), charset);
        copyright = copyrightNsf;

        ripper = ""; // NSFe only
        text = ""; // NSFe only
        text_len = 0; // NSFe only
        speedNtsc = (buf[0x6e] & 0xff) | ((buf[0x6f] & 0xff) << 8);
        System.arraycopy(buf, 112, bankSwitch, 0, 8);
        speedPal = (buf[0x78] & 0xff) | ((buf[0x79] & 0xff) << 8);
        palNtsc = buf[0x7a] & 0xff;

        if (speedPal == 0)
            speedPal = 0x4e20;
        if (speedNtsc == 0)
            speedNtsc = 0x411A;

        soundChip = buf[0x7b] & 0xff;

        useVrc6 = (soundChip & 1) != 0;
        useVrc7 = (soundChip & 2) != 0;
        useFds = (soundChip & 4) != 0;
        useMmc5 = (soundChip & 8) != 0;
        useN106 = (soundChip & 16) != 0;
        useFme7 = (soundChip & 32) != 0;
        logger.log(Level.INFO, "%s%s%s%s%s%s".formatted(useVrc6 ? "6" : "_", useVrc7 ? "7" : "_", useFds ? "F" : "_", useMmc5 ? "M" : "_", useN106 ? "N" : "_", useFme7 ? "F" : "_"));

        System.arraycopy(buf, 124, extra, 0, 4);

        //delete[] body;
        //body = new UINT8[size - 0x80];
        body = new byte[buf.length - 0x80];
        System.arraycopy(buf, 128, body, 0, buf.length - 0x80);

        bodySize = buf.length - 0x80;

        //song = start - 1;
    }

    public void init(int hpf_, int lpf_) {
        this.bank = new NesBank();
        this.mem = new NesMem();
        this.cpu = new Km6502(true);
        this.apu = new NesApu();
        this.dmc = new NesDmc();
        this.fds = new NesFds();
        this.n106 = new NesN106();
        this.vrc6 = new NesVrc6();
        this.mmc5 = new NesMmc5();
        this.fme7 = new NesFme7();
        this.vrc7 = new NesVrc7();

        this.apu.apu.init(NsfClock, this.sampleRate);
        this.apu.reset();
        this.dmc.dmc.init(NsfClock, this.sampleRate);
        this.dmc.reset();
        this.fds.fds.init(NsfClock, this.sampleRate);
        this.fds.reset();
        this.n106.setClock(NsfClock);
        this.n106.setRate(this.sampleRate);
        this.n106.reset();
        this.vrc6.setClock(NsfClock);
        this.vrc6.setRate(this.sampleRate);
        this.vrc6.reset();
        this.mmc5.setClock(NsfClock);
        this.mmc5.setRate(this.sampleRate);
        this.mmc5.reset();
        this.mmc5.setCPU(this.cpu);
        this.fme7.setClock(NsfClock);
        this.fme7.setRate(this.sampleRate);
        this.fme7.reset();
        this.vrc7.setClock(NsfClock);
        this.vrc7.setRate(this.sampleRate);
        this.vrc7.reset();

        this.dmc.dmc.nes_apu = this.apu.apu;
        this.dmc.dmc.setAPU(this.apu.apu);

        Device.Bus stack = new Device.Bus();
        layer = new Device.Layer();
        Device.Bus apuBus = new Device.Bus();

        dcf = new DCFilter();
        lpf = new Filter();
        lpf.setRate(this.sampleRate);
        lpf.reset();
        dcf.setRate(this.sampleRate);
        dcf.reset();
        dcf.setParam(270, 256 - hpf_); // HPF:256-(Range0-256(Def:92))
        lpf.setParam(4700.0, lpf_); // LPF:(Range 0-400(Def:112))
//logger.log(Level.TRACE, "dcf:%d".formatted(dcf.getFactor()));
//logger.log(Level.TRACE, "lpf:%d".formatted(lpf.getFactor()));

        int bmax = 0;

        for (int i = 0; i < 8; i++)
            if (bmax < (bankSwitch[i] & 0xff))
                bmax = bankSwitch[i] & 0xff;

        this.mem.setImage(body, load_address & 0xffff, bodySize);

        if (bmax != 0) {
            this.bank.setImage(body, load_address & 0xffff, bodySize);
            for (int i = 0; i < 8; i++)
                this.bank.setBankDefault(i + 8, bankSwitch[i] & 0xff);
        }

        stack.detachAll();
        layer.detachAll();
        apuBus.detachAll();

        // The detector matches the last 30 s of writes against every earlier point in the ring, so
        // the ring has to hold a whole loop plus those 30 s. 16 bits - what the C# had - holds
        // 65536, which is plenty for an APU only song writing a few hundred a second but nowhere
        // near an expansion chip one: Thunder Force IV's N163 song writes ~7000 a second, so its
        // 30 s signature alone wants 210000 and the detector gave up before comparing anything.
        // 20 bits holds 1048576: that song's 55 s loop plus its signature needs ~610000, and even
        // at 7000 writes a second there is room for a two and a half minute loop. 8 MB per song.
        ld = new LoopDetector.NESDetector(20);
        ld.reset();
        stack.attach(ld);

        apuBus.attach(this.apu);
        apuBus.attach(this.dmc);

        setOptions.accept(bankSwitch);
        if (useFds) {
            apuBus.attach(this.fds);
        }
        if (useN106) {
            apuBus.attach(this.n106);
        }
        if (useVrc6) {
            apuBus.attach(this.vrc6);
        }
        if (useMmc5) {
            apuBus.attach(this.mmc5);
        }
        if (useFme7) {
            apuBus.attach(this.fme7);
        }
        if (useVrc7) {
            apuBus.attach(this.vrc7);
        }

        if (bmax > 0) layer.attach(this.bank);
        layer.attach(this.mem);

        stack.attach(apuBus);
        stack.attach(layer);

        this.cpu.setMemory(stack);
        this.dmc.setMemory(stack);

        this.apu.apu.squareTable[0] = 0;
        for (int i = 1; i < 32; i++)
            this.apu.apu.squareTable[i] = (int) ((8192.0 * 95.88) / (8128.0 / i + 100));

        for (int c = 0; c < 2; ++c)
            for (int t = 0; t < 2; ++t)
                this.apu.apu.sm[c][t] = 128;

        reset();
    }

    public enum Region {
        NTSC,
        PAL,
        DENDY
    }

    private void reset() {
        apu_clock_rest = 0.0;
        cpu_clock_rest = 0.0;
        silent_length = 0;
        time_in_ms = 0;
        time_in_ms_rest = 0.0;
        playtime_detected = false;

        Region region = getRegion(palNtsc);
        double speed;
        speed = 1000000.0 / ((region == Region.NTSC) ? speedNtsc : speedPal);

        layer.reset();
        this.cpu.reset();

        this.cpu.start(initAddress, playAddress, speed, song, (region == Region.PAL) ? 1 : 0, 0);
    }

    private static Region getRegion(int flags) {
        int pref = 0;

        // user forced region
        if (pref == 3) return Region.NTSC;
        if (pref == 4) return Region.PAL;
        if (pref == 5) return Region.DENDY;

        // single-mode NSF
        if (flags == 0) return Region.NTSC;
        if (flags == 1) return Region.PAL;

        if ((flags & 2) != 0) { // dual mode
            if (pref == 1) return Region.NTSC;
            if (pref == 2) return Region.PAL;
            // else pref == 0 or invalid, use auto setting based on Flags bit
            return ((flags & 1) != 0) ? Region.PAL : Region.NTSC;
        }

        return Region.NTSC; // fallback for invalid Flags
    }

private int CC;
    public int render(short[] b, int length) {
        return render(b, length, 0);
    }

    public int render(short[] b, int length, int offset) {
        assert isRealModel;

        if (getCounter.getAsInt() < 0) {
            incCounter.accept(length);
//logger.log(Level.DEBUG, "frameCounter: " + frameCounter);
            return length;
        }

        int[] buf = new int[2];
        int[] out = new int[2];

        int master_volume = 0x80;

        double apu_clock_per_sample = this.cpu.NES_BASECYCLES / sampleRate;
        double cpu_clock_per_sample = apu_clock_per_sample * speed.getAsDouble();

        for (int i = 0; i < length; i++) {
            //total_render++;
            updateAtMiddle.run();

            // tick CPU
            cpu_clock_rest += cpu_clock_per_sample;
            int cpu_clocks = (int) cpu_clock_rest;
            if (cpu_clocks > 0) {
                int real_cpu_clocks = this.cpu.exec(cpu_clocks);
                cpu_clock_rest -= real_cpu_clocks;

                // tick APU frame sequencer
                this.dmc.dmc.tickFrameSequence(real_cpu_clocks);
                if (useMmc5)
                    this.mmc5.tickFrameSequence(real_cpu_clocks);
            }

//            updateInfo();

            // tick APU / expansions
            apu_clock_rest += apu_clock_per_sample;
            int apu_clocks = (int) apu_clock_rest;
            if (apu_clocks > 0) {
                apu_clock_rest -= apu_clocks;
            }

//            // render Output
//            this.apu.tick(apu_clocks);
//            this.apu.render(buf);
//
//            int mul = (int) (16384.0 * Math.pow(10.0, getVolume.apply(0) / 40.0)); // apu
//            out[0] = (buf[0] * mul) >> 13;
//            out[1] = (buf[1] * mul) >> 13;
//
//            this.dmc.tick(apu_clocks);
//            this.dmc.render(buf);
//            mul = (int) (16384.0 * Math.pow(10.0, getVolume.apply(1) / 40.0)); // dmc
//            out[0] += (buf[0] * mul) >> 13;
//            out[1] += (buf[1] * mul) >> 13;
//
//            if (useFds) {
//                this.fds.tick(apu_clocks);
//                this.fds.render(buf);
//                mul = (int) (16384.0 * Math.pow(10.0, getVolume.apply(2) / 40.0)); // fds
//                out[0] += (buf[0] * mul) >> 13;
//                out[1] += (buf[1] * mul) >> 13;
//            }
//
//            if (useN106) {
//                this.n106.tick(apu_clocks);
//                this.n106.render(buf);
//                mul = (int) (16384.0 * Math.pow(10.0, getVolume.apply(3) / 40.0)); // n160
//                out[0] += (buf[0] * mul) >> 10;
//                out[1] += (buf[1] * mul) >> 10;
//            }
//
//            if (useVrc6) {
//                this.vrc6.tick(apu_clocks);
//                this.vrc6.render(buf);
//                mul = (int) (16384.0 * Math.pow(10.0, getVolume.apply(4) / 40.0)); // vrc6
//                out[0] += (buf[0] * mul) >> 10;
//                out[1] += (buf[1] * mul) >> 10;
//            }
//
//            if (useMmc5) {
//                this.mmc5.tick(apu_clocks);
//                this.mmc5.render(buf);
//                mul = (int) (16384.0 * Math.pow(10.0, getVolume.apply(5) / 40.0)); // mmc5
//                out[0] += (buf[0] * mul) >> 10;
//                out[1] += (buf[1] * mul) >> 10;
//            }
//
//            if (useFme7) {
//                this.fme7.tick(apu_clocks);
//                this.fme7.render(buf);
//                mul = (int) (16384.0 * Math.pow(10.0, getVolume.apply(6) / 40.0)); // fme7
//                out[0] += (buf[0] * mul) >> 9;
//                out[1] += (buf[1] * mul) >> 9;
//            }
//
//            if (useVrc7) {
//                this.vrc7.tick(apu_clocks);
//                this.vrc7.render(buf);
//                mul = (int) (16384.0 * Math.pow(10.0, getVolume.apply(7) / 40.0)); // vrc7
//                out[0] += (buf[0] * mul) >> 10;
//                out[1] += (buf[1] * mul) >> 10;
//            }

            // TODO the original doesn't work why? (above comment out)
            this.apu.tick(apu_clocks);
            this.apu.render(buf);
            out[0] = buf[0] * 2;
            out[1] = buf[1] * 2;

            this.dmc.tick(apu_clocks);
            this.dmc.render(buf);
            out[0] += buf[0] * 2;
            out[1] += buf[1] * 2;

            if (useFds) {
                this.fds.tick(apu_clocks);
                this.fds.render(buf);
                out[0] += buf[0] * 2;
                out[1] += buf[1] * 2;
            }

            if (useN106) {
                this.n106.tick(apu_clocks);
                this.n106.render(buf);
                out[0] += buf[0] * 16;
                out[1] += buf[1] * 16;
            }

            if (useVrc6) {
                this.vrc6.tick(apu_clocks);
                this.vrc6.render(buf);
                out[0] += buf[0] * 16;
                out[1] += buf[1] * 16;
            }

            if (useMmc5) {
                this.mmc5.tick(apu_clocks);
                this.mmc5.render(buf);
                out[0] += buf[0] * 16;
                out[1] += buf[1] * 16;
            }

            if (useFme7) {
                this.fme7.tick(apu_clocks);
                this.fme7.render(buf);
                out[0] += buf[0] * 32;
                out[1] += buf[1] * 32;
            }

            if (useVrc7) {
                this.vrc7.tick(apu_clocks);
                this.vrc7.render(buf);
                out[0] += buf[0] * 16;
                out[1] += buf[1] * 16;
            }

            int outM = (out[0] + out[1]); // >> 1; // mono mix
            if (outM == last_out) silent_length++;
            else silent_length = 0;
            last_out = outM;

            dcf.fastRender(out);
            lpf.fastRender(out);

            out[0] = (out[0] * master_volume) >> 9;
            out[1] = (out[1] * master_volume) >> 9;

            if (out[0] < -32767)
                out[0] = -32767;
            else if (32767 < out[0])
                out[0] = 32767;

            if (out[1] < -32767)
                out[1] = -32767;
            else if (32767 < out[1])
                out[1] = 32767;

//            if (nch == 2) {
            b[offset + i * 2] = (short) out[0];
            b[offset + i * 2 + 1] = (short) out[1];

            enq.accept((short) out[0], (short) out[1]);
//            } else { // if not 2 channels, presume mono
//                outM = (out[0] + out[1]) >> 1;
//                for (int i = 0; i<nch; ++i)
//                    b[0] = outM;
//            }
//            b += nch;
        }

        time_in_ms_rest += 1000. * length / sampleRate * speed.getAsDouble();
        int elapsed = (int) time_in_ms_rest;
        time_in_ms += elapsed;
        time_in_ms_rest -= elapsed;

        //checkTerminal();
        detectLoop();
        detectSilent();
        detectTimeout();
        updateAtTrail.accept(playtime_detected);

if (CC++ % INTERVAL == 0) { logger.log(Level.TRACE, "NSF: %d, %d, pc: %04x".formatted(out[0], out[1], this.cpu.p)); }
        return length;
    }
private static final int INTERVAL = 1024;

    private boolean playtime_detected = false;

    private void detectLoop() {
        if (ld.isLooped(time_in_ms, 30000, 5000) && !playtime_detected) {
            playtime_detected = true;
            updateAtDetectLoop.accept(
                    (long) ld.getLoopEnd() * this.sampleRate / 1000,
                    (long) (ld.getLoopEnd() - ld.getLoopStart()) * this.sampleRate / 1000
            );
        }
    }

    /**
     * A song whose frame by frame writes never repeat exactly is never matched by {@link #detectLoop}:
     * Capcom's driver keeps its tempo in a fraction, so the notes loop every 28.6 s but each pass sits
     * on different frames and the volume and pitch writes between them never line up again. Gives up
     * after {@link #maxPlayTimeMs} the way NSFPlay does with its default play time.
     */
    private void detectTimeout() {
        if (time_in_ms > maxPlayTimeMs && !playtime_detected) {
logger.log(Level.DEBUG, "end: gave up after %ds".formatted(maxPlayTimeMs / 1000));
            playtime_detected = true;
            updateAtDetectSilent.accept((long) time_in_ms * this.sampleRate / 1000);
        }
    }

    private static final int maxPlayTimeMs = 5 * 60 * 1000;

    private void detectSilent() {
        if (silent_length > sampleRate * 3L && !playtime_detected) {
            playtime_detected = true;
            updateAtDetectSilent.accept((long) ld.getLoopEnd() * this.sampleRate / 1000);
        }
    }
}
