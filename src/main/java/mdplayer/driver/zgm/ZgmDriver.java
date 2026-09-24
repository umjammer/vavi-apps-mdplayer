package mdplayer.driver.zgm;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import mdplayer.Common;
import mdplayer.Common.EnmModel;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.zgm.zgmChip.ChipFactory;
import mdplayer.driver.zgm.zgmChip.ZgmChip;
import mdplayer.driver.BasePlugin;
import mdplayer.ChipRegister;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


/**
 * ZGM
 *
 * @author kumatan
 */
public class ZgmDriver extends BaseDriver {

    private static final Logger logger = getLogger(ZgmDriver.class.getName());

    private static final int FCC_ZGM = 0x204D475A;  // "ZGM "
    private static final int FCC_GD3 = 0x20336447;  // "Gd3 "
    private static final int FCC_DEF = 0x666544;  // "Def"
    private static final int FCC_TRK = 0x6b7254;  // "Trk"

    private int vgmEof;
    private int vgmLoopOffset = 0;
    private int chipCommandSize = 1;
    private int vgmDataOffset = 0;
    private int vgmAdr;
    private int vgmWait;
    private boolean vgmAnalyze;

    private String version = "";
    private String usedChips = "";

    /** a command handler, gets the command at {@code vgmAdr} and returns the address of the next one */
    @FunctionalInterface
    public interface Command {
        int run(int cmd, int vgmAdr);
    }

    private final Map<Integer, Command> vgmCmdTbl = new HashMap<>();

    /** the chips the song defines, in definition order */
    private final List<ZgmChip> chips = new ArrayList<>();

    /** chip command number to chip */
    private final Map<Integer, ZgmChip> dicChipCmdNo = new HashMap<>();

    public ZgmDriver(BasePlugin<? extends BaseDriver> plugin) {
        super(plugin);
    }

    public ZgmDriver() {
        this(null); // gross
    }

    /** the chips the song defines, valid after {@link #init} */
    public List<ZgmChip> getChips() {
        return chips;
    }

    @Override
    public MetaData retrieveMetaData(byte[] buf, Object... args) {
        return getZGMGD3Info(buf, null);
    }

    @Override
    public void init(EnmModel model, int latency, int waitTime, Object... args) {
        this.model = model;
        this.latency = latency;
        this.waitTime = waitTime;
        this.dataBuf = plugin.getData(); // the ctor's copy is stale for a replay

        counter = 0;
        totalCounter = 0;
        loopCounter = 0;
        curLoop = 0;
        stopped = false;
        frameCounter = -latency - waitTime;
        speed = 1;
        speedCounter = 0;

        getZGMInfo(dataBuf);

        vgmAdr = vgmDataOffset;
        vgmWait = 0;
        vgmAnalyze = true;
        counter = 0;
        setCommands();
    }

    @Override
    public void processOneFrame() {
        try {
            speedCounter += (double) Common.VGMProcSampleRate / setting.getOutputDevice().getSampleRate() * speed;
            while (speedCounter >= 1.0) {
                speedCounter -= 1.0;
                if (frameCounter > -1) {
                    oneFrameVGMMain();
                } else {
                    frameCounter++;
                }
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    private void oneFrameVGMMain() {
        if (vgmWait > 0) {
            vgmWait--;
            counter++;
            frameCounter++;
            return;
        }

        if (!vgmAnalyze) {
            stopped = true;
            return;
        }

        // commands up to the next wait all happen in this sample
        boolean looped = false;
        while (vgmWait <= 0) {
            if (vgmAdr >= dataBuf.length || vgmAdr >= vgmEof) {
                if (loopCounter != 0 && vgmLoopOffset != 0 && !looped) { // a loop without a wait would spin here
                    looped = true;
                    vgmAdr = vgmLoopOffset;
                    curLoop++;
                    counter = 0;
                } else {
                    vgmAnalyze = false;
                    return;
                }
            }

            int cmd = dataBuf[vgmAdr] & 0xff;
            Command c = vgmCmdTbl.get(cmd);
            if (c != null) {
                vgmAdr = c.run(cmd, vgmAdr);
            } else {
                logger.log(Level.DEBUG, "unknown command: Adr:%X Dat:%X".formatted(vgmAdr, cmd));
                vgmAdr++;
            }
        }

        vgmWait--;
        counter++;
        frameCounter++;
    }

    private void setCommands() {
        // the chips' own commands were put while their defines were read
        vgmCmdTbl.put(0x01, this::vcWaitNSamples);
        vgmCmdTbl.put(0x02, this::vcWait735Samples);
        vgmCmdTbl.put(0x03, this::vcWait882Samples);
        vgmCmdTbl.put(0x07, this::vcDataBlock);
        vgmCmdTbl.put(0x09, this::vcDummyChip);
    }

    private int vcWaitNSamples(int od, int vgmAdr) {
        vgmWait += ByteUtil.readLeShort(dataBuf, vgmAdr + 1) & 0xffff;
        return vgmAdr + 3;
    }

    private int vcWait735Samples(int od, int vgmAdr) {
        vgmWait += 735;
        return vgmAdr + 1;
    }

    private int vcWait882Samples(int od, int vgmAdr) {
        vgmWait += 882;
        return vgmAdr + 1;
    }

    /** no supported chip takes a data block, they are skipped */
    private int vcDataBlock(int od, int vgmAdr) {
        int chipCommandNumber = chipCommandSize == 1 ? dataBuf[vgmAdr + 1] & 0xff : ByteUtil.readLeShort(dataBuf, vgmAdr + 2) & 0xffff;
        vgmAdr += chipCommandSize == 1 ? 0 : 2;
        int bLen = ByteUtil.readLeInt(dataBuf, vgmAdr + 3);
        if (!dicChipCmdNo.containsKey(chipCommandNumber)) {
            logger.log(Level.DEBUG, "data block for an undefined chip command: %02x".formatted(chipCommandNumber));
        }
        return vgmAdr + bLen + 7;
    }

    private int vcDummyChip(int od, int vgmAdr) {
        return vgmAdr + 3;
    }

    /**
     * @param chipRegister null for the metadata only, then the chips are not set up
     */
    private MetaData getZGMGD3Info(byte[] buf, ChipRegister chipRegister) {
        if (buf == null) throw new IllegalArgumentException("null buffer");

        int vgmGd3 = ByteUtil.readLeInt(buf, 0x18);
        if (vgmGd3 == 0) throw new IllegalArgumentException("invalid zgm gd3 value");
        int vgmGd3Id = ByteUtil.readLeInt(buf, vgmGd3);
        if (vgmGd3Id != FCC_GD3) throw new IllegalArgumentException("data is not gd3");

        int version = ByteUtil.readLeInt(buf, 0x08);
        // Version Check
        if (version < 10) throw new IllegalArgumentException("invalid version");
        this.version = "%d.%d%d".formatted((version & 0xf00) / 0x100, (version & 0xf0) / 0x10, (version & 0xf));

        totalCounter = ByteUtil.readLeInt(buf, 0x0c);
        if (totalCounter < 0) throw new IllegalArgumentException("invalid total counter");
        loopCounter = ByteUtil.readLeInt(buf, 0x10);

        int defineAddress = ByteUtil.readLeInt(buf, 0x1c);
        int defineCount = ByteUtil.readLeShort(buf, 0x24) & 0xffff;
        // Check number of sound source definitions
        if (defineCount < 1) throw new IllegalArgumentException("invalid define count");

        chipCommandSize = (defineCount > 128) ? 2 : 1;

        int trackAddress = ByteUtil.readLeInt(buf, 0x20);
        int trackCounter = ByteUtil.readLeShort(buf, 0x26) & 0xffff;
        vgmDataOffset = trackAddress + 11;
        // Track Count Check
        if (trackCounter != 1) throw new IllegalArgumentException("invalid track counter");
        int fcc = ByteUtil.readLe24(buf, trackAddress);
        if (fcc != FCC_TRK) throw new IllegalArgumentException("invalid fcc track value");
        int trackLength = ByteUtil.readLeInt(buf, trackAddress + 3);
        vgmLoopOffset = ByteUtil.readLeInt(buf, trackAddress + 7);
        if (vgmLoopOffset != 0) loopCounter = 1;
        vgmEof = trackAddress + trackLength;

        int pos = defineAddress;

        chips.clear();
        dicChipCmdNo.clear();
        vgmCmdTbl.clear();
        Map<String, Integer> chipCount = new HashMap<>();
        for (int i = 0; i < defineCount; i++) {
            fcc = ByteUtil.readLe24(buf, pos);
            if (fcc != FCC_DEF) throw new IllegalArgumentException("invalid fcc def value");
            int chipNum = ByteUtil.readLeInt(buf, pos + 0x4);
            ZgmChip chip = (new ChipFactory()).create(chipNum, chipRegister, setting, buf);
            if (chip == null) {
                // only the conductor, the YM2609 (skipped, no emulator) and the Gigatron have
                // a sender, every other chip is still a placeholder as in the C# original, so
                // a zgm that uses one cannot be played at all (not merely played silently)
                throw new IllegalArgumentException("not supported chip: 0x%08x".formatted(chipNum));
            }

            chipCount.merge(chip.name, 0, (a, b) -> a + 1);

            pos = chip.setUp(chipCount.get(chip.name), pos, vgmCmdTbl);
            chips.add(chip);
            dicChipCmdNo.put(chip.getDefineInfo().commandNo, chip);
        }

        usedChips = getUsedChipsString(chips);

        vgmGd3 += 12; // + 0x14;
        MetaData metaData = Common.getMetaData(buf, vgmGd3);
        metaData.set(Tag.Chip, usedChips);
        return metaData;
    }

    private void getZGMInfo(byte[] vgmBuf) {
        if (vgmBuf == null) throw new IllegalArgumentException("null buffer");

        try {
            if (ByteUtil.readLeInt(vgmBuf, 0) != FCC_ZGM) throw new IllegalArgumentException("buffer is not zgm");

            metaData = getZGMGD3Info(vgmBuf, plugin.chipRegister);
        } catch (Exception e) {
logger.log(Level.ERROR, "An exception occurred while getting ZGM information. Message=[%s]".formatted(e.getMessage()), e);
            throw e;
        }
    }

    /** same chips are counted as " x9" */
    private static String getUsedChipsString(List<ZgmChip> chips) {
        Map<String, Integer> c = new LinkedHashMap<>();
        for (ZgmChip chip : chips) c.merge(chip.name, 1, Integer::sum);
        return c.entrySet().stream()
                .map(e -> e.getKey() + (e.getValue() < 2 ? "" : " x%d".formatted(e.getValue())))
                .collect(Collectors.joining(" , "));
    }

    public static class DefineInfo {
        public int length = 14;
        public int chipIdentNo = 0;
        public int commandNo = 0;
        public int clock = 0;
        public byte[] option = null;

        //public ClsChip chips = null;
        public int offset = 0;
    }
}
