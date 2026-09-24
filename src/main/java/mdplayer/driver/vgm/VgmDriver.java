package mdplayer.driver.vgm;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import mdplayer.Common;
import mdplayer.Common.EnmModel;
import mdplayer.chips.*;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.lib.vgm.Vgm;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;

import static java.lang.System.getLogger;


/**
 * VGM
 *
 * @author kumatan
 */
public class VgmDriver extends BaseDriver {

    private static final Logger logger = getLogger(VgmDriver.class.getName());

    public final Vgm vgm;

    private String version = "";
    private String usedChips = "";

    public VgmDriver(BasePlugin<? extends BaseDriver> plugin) {
        super(plugin);

        this.vgm = new Vgm();
        vgm.setIVgm(new Vgm.IVgm() {
            @Override public void setPanSn76489(int chipId, int data) {
                plugin.chipRegister.chip(Sn76489Chip.class).setPan(chipId, data, model);
            }

            @Override public void writeSn76489(int chipId, int data) {
                plugin.chipRegister.chip(Sn76489Chip.class).write(chipId, data, model);
            }

            @Override public void writeAy8910(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Ay8910Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeDmg(int chipId, int addr, int data) {
                plugin.chipRegister.chip(DmgChip.class).write(chipId, addr, data, model);
            }

            @Override public void writeNes(int chipId, int addr, int data) {
                plugin.chipRegister.chip(NesChip.class).write(chipId, addr, data, model);
            }

            @Override public void writeMultiPcm(int chipId, int addr, int data) {
                plugin.chipRegister.chip(MultiPcmChip.class).write(chipId, addr, data, model);
            }

            @Override public void writeUpd7759(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Upd7759Chip.class).write(chipId, addr, data, model);
            }

            @Override public void setBankMultiPcm(int chipId, int ch, int addr) {
                plugin.chipRegister.chip(MultiPcmChip.class).setBank(chipId, ch, addr, model);
            }

            @Override public void writeQSound(int chipId, int mm, int ll, int rr) {
                plugin.chipRegister.chip(QSoundChip.class).write(chipId, mm, ll, rr, model);
            }

            @Override public void writeX1_010(int chipId, int mm, int ll, int rr) {
                plugin.chipRegister.chip(X1_010Chip.class).write(chipId, mm, ll, rr, model);
            }

            @Override public void writeYm2413(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Ym2413Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeYm3812(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Ym3812Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeHuC6280(int chipId, int addr, int data) {
                plugin.chipRegister.chip(HuC6280Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeGa20(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Ga20Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeYm2612(int chipId, int port, int addr, int data, int frameCounter) {
                plugin.chipRegister.chip(Ym2612Chip.class).write(chipId, port, addr, data, model, frameCounter);
            }

            @Override public void writeYm2203(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Ym2203Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeYm2608(int chipId, int port, int addr, int data) {
                plugin.chipRegister.chip(Ym2608Chip.class).write(chipId, port, addr, data, model);
            }

            @Override public void updateRamTypeYm2608(byte[] vgmBuf, int vgmDataOffset) {
                plugin.chipRegister.chip(Ym2608Chip.class).updateRamType(vgmBuf, vgmDataOffset);
            }

            @Override public void writeYm2610(int chipId, int port, int addr, int data) {
                plugin.chipRegister.chip(Ym2610Chip.class).write(chipId, port, addr, data, model);
            }

            @Override public void writeYmF262(int chipId, int port, int addr, int data) {
                plugin.chipRegister.chip(YmF262Chip.class).write(chipId, port, addr, data, model);
            }

            @Override public void writeYm3526(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Ym3526Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeY8950(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Y8950Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeYmZ280B(int chipId, int addr, int data) {
                plugin.chipRegister.chip(YmZ280BChip.class).write(chipId, addr, data, model);
            }

            @Override public void writeYmF271(int chipId, int port, int addr, int data) {
                plugin.chipRegister.chip(YmF271Chip.class).write(chipId, port, addr, data, model);
            }

            @Override public void writeYmF278B(int chipId, int port, int addr, int data) {
                plugin.chipRegister.chip(YmF278BChip.class).write(chipId, port, addr, data, model);
            }

            @Override public void writeYm2151(int chipId, int port, int addr, int data, int correction, int frameCounter) {
                plugin.chipRegister.chip(Ym2151Chip.class).write(chipId, port, addr, data, model, correction, frameCounter);
            }

            @Override public void writeOkiM6258(int chipId, int port, int data) {
                plugin.chipRegister.chip(OkiM6258Chip.class).write(chipId, port, data, model);
            }

            @Override public void writeOkiM6295(int chipId, int port, int data) {
                plugin.chipRegister.chip(OkiM6295Chip.class).write(chipId, port, data, model);
            }

            @Override public void writeSaa1099(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Saa1099Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeWSwan(int chipId, int port, int data) {
                plugin.chipRegister.chip(WSwanChip.class).write(chipId, port, data, model);
            }

            @Override public void writeMemWSwan(int chipId, int port, int data) {
                plugin.chipRegister.chip(WSwanChip.class).writeMemory(chipId, port, data, model);
            }

            @Override public void writePokey(int chipId, int port, int data) {
                plugin.chipRegister.chip(PokeyChip.class).write(chipId, port, data, model);
            }

            @Override public void writeSegaPcm(int chipId, int offset, int data) {
                plugin.chipRegister.chip(SegaPcmChip.class).write(chipId, offset, data, model);
            }

            @Override public void writePcmSegaPcm(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(SegaPcmChip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmYm2608(int chipId, byte[] vgmBuf, int vgmAdr, int bLen, int startAddress) {
                plugin.chipRegister.chip(Ym2608Chip.class).writePcm(chipId, vgmBuf, vgmAdr, bLen, startAddress, model);
            }
            @Override public void writeAdpcmAYm2610(int chipId, byte[] vgmBuf, int vgmAdr, int bLen, int startAddress, int romSize) {
                plugin.chipRegister.chip(Ym2610Chip.class).writeAdpcmA(chipId, vgmBuf, vgmAdr, bLen, startAddress, romSize, model);
            }
            @Override public void writeAdpcmBYm2610(int chipId, byte[] vgmBuf, int vgmAdr, int bLen, int startAddress, int romSize) {
                plugin.chipRegister.chip(Ym2610Chip.class).writeAdpcmB(chipId, vgmBuf, vgmAdr, bLen, startAddress, romSize, model);
            }
            @Override public void writePcmYmF278B(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(YmF278BChip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writeRamYmF278B(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(YmF278BChip.class).writeRam(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmYmF271(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(YmF271Chip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmYmZ280B(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(YmZ280BChip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmY8950(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(Y8950Chip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmMultiPcm(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(MultiPcmChip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmUpd7759(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(Upd7759Chip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmOkiM6295(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(OkiM6295Chip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmK054539(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(K054539Chip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmC140(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(C140Chip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmK053260(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(K053260Chip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmQSound(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(QSoundChip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmX1_010(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(X1_010Chip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmC352(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(C352Chip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmGa20(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(Ga20Chip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }
            @Override public void writePcmRf5C68(int chipId, int offset, int length, byte[] buf, int srcOffset) {
                plugin.chipRegister.chip(Rf5C68Chip.class).writePcm(chipId, offset, length, buf, srcOffset, model);
            }
            @Override public void writePcmRf5C164(int chipId, int offset, int length, byte[] buf, int srcOffset) {
                plugin.chipRegister.chip(Rf5C164Chip.class).writePcm(chipId, offset, length, buf, srcOffset, model);
            }
            @Override public void writePcmNes(int chipId, int stAdr, int dataSize, byte[] vgmBuf, int vgmAdr) {
                plugin.chipRegister.chip(NesChip.class).writePcm(chipId, stAdr, dataSize, vgmBuf, vgmAdr, model);
            }
            @Override public void writePcmEs5503(int chipId, int offset, int length, byte[] buf, int srcOffset) {
                plugin.chipRegister.chip(Es5503Chip.class).writePcm(chipId, offset, length, buf, srcOffset, model);
            }
            @Override public void writePcmEs5505(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr) {
                plugin.chipRegister.chip(Es5505Chip.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr, model);
            }

            @Override public void writePCMRamRf5C68(int chipId, int offset, int length, byte[] buf, int srcOffset) {
                plugin.chipRegister.chip(Rf5C68Chip.class).writePcm(chipId, offset, length, buf, srcOffset, model);
            }
            @Override public void writePCMRamRf5C164(int chipId, int offset, int length, byte[] buf, int srcOffset) {
                plugin.chipRegister.chip(Rf5C164Chip.class).writePcm(chipId, offset, length, buf, srcOffset, model);
            }

            @Override public void writeRf5C68(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Rf5C68Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeMemoryRf5C68(int chipId, int offset, int data) {
                plugin.chipRegister.chip(Rf5C68Chip.class).writeMemory(chipId, offset, data, model);
            }

            @Override public void writeRf5C164(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Rf5C164Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeMemoryRf5C164(int chipId, int offset, int data) {
                plugin.chipRegister.chip(Rf5C164Chip.class).writeMemory(chipId, offset, data, model);
            }

            @Override public void writePwm(int chipId, int addr, int data) {
                plugin.chipRegister.chip(PwmChip.class).write(chipId, addr, data, model);
            }

            @Override public void writeK051649(int chipId, int addr, int data) {
                plugin.chipRegister.chip(K051649Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeK053260(int chipId, int addr, int data) {
                plugin.chipRegister.chip(K053260Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeK054539(int chipId, int addr, int data) {
                plugin.chipRegister.chip(K054539Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeC140(int chipId, int addr, int data) {
                plugin.chipRegister.chip(C140Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeEs5503(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Es5503Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeEs5505(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Es5505Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeMsm5205(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Msm5205Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeMsm5232(int chipId, int addr, int data) {
                plugin.chipRegister.chip(Msm5232Chip.class).write(chipId, addr, data, model);
            }

            @Override public void writeC352(int chipId, int addr, int data) {
                plugin.chipRegister.chip(C352Chip.class).write(chipId, addr, data, model);
            }

            @Override public int readHuC6280(int chipId, int addr) {
                return plugin.chipRegister.chip(HuC6280Chip.class).read(chipId, addr, model);
            }

            @Override public boolean isVirtual() {
                return model == EnmModel.VirtualModel;
            }

            @Override public int frameCounter() { return frameCounter; }
            @Override public void dataBlock(boolean b) { isDataBlock = b; }
            @Override public long getTotalCounter() { return totalCounter; }
            @Override public void setTotalCounter(long v) { totalCounter = v; }
            @Override public void setLoopCounter(long v) { loopCounter = v; }
            @Override public int loop() { return curLoop; }
            @Override public void setUsedChips(String s) { usedChips = s; }
            @Override public String getUsedChips() { return usedChips; }
            @Override public void setVersion(String s) { version = s; }
            @Override public String getVersion() { return version; }
            @Override public void updateMetaData(byte[] b, Object... o) { metaData = retrieveMetaData(b, o); }
        });
    }

    public VgmDriver() {
        this(null);
    }

    @Override
    public void init(EnmModel model, int latency, int waitTime, Object... args) {
        this.model = model;
        this.latency = latency;
        this.waitTime = waitTime;

        counter = 0;
        frameCounter = -latency - waitTime;
        curLoop = 0;
        speed = 1;
        speedCounter = 0;

        stopped = false;
        isDataBlock = false;

        vgm.vgmBuf = dataBuf;

        vgm.init();

        vgm.useChipYM2612Ch6 = false;
        if (plugin.chipRegister.contains(Ym2612Chip.class) && false) { // TODO Ym2612Ch6
            vgm.useChipYM2612Ch6 = true;
        }
    }

    /**
     * Renders, then applies the gain the header's "Volume Modifier" (0x7c) asks for. Files that
     * were mastered quiet request a boost there (e.g. the AdLib demo songs use 51 = x3) and play
     * far too softly without it; a few request an attenuation instead. The field is only defined
     * from VGM 1.60, but files declaring 1.51 do set it, so it's honored whenever the header
     * actually reaches 0x7c -- the same rule the rest of {@code Vgm#getInformationHeader} uses.
     */
    @Override
    public int render(short[] buffer, int offset, int sampleCount) {
        int cnt = super.render(buffer, offset, sampleCount);

        double gain = vgm.getVolumeGain();
        if (gain != 1.0) {
            for (int i = offset; i < offset + cnt; i++) {
                buffer[i] = (short) Math.clamp((long) (buffer[i] * gain), Short.MIN_VALUE, Short.MAX_VALUE);
            }
        }

        return cnt;
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
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }

    private void oneFrameVGMMain() {
        if (vgm.vgmWait > 0) {
            vgm.oneFrameVGMStream();
            vgm.vgmWait--;
            counter++;
            frameCounter++;
//logger.log(Level.TRACE, "ret: wait: " + vgmWait + ".formatted(fc: " + frameCounter));
            return;
        }

        vgm.oneFrameVGMStream();

        if (!vgm.vgmAnalyze) {
            stopped = true;
            logger.log(Level.DEBUG, "ret: not analyze");
            return;
        }

        int countNum = 0;
        while (vgm.vgmWait <= 0) {
            if (vgm.vgmAdr >= dataBuf.length || (vgm.vgmEof != 0 && vgm.vgmAdr >= vgm.vgmEof)) {
                if (loopCounter != 0) {
                    vgm.vgmAdr = vgm.vgmLoopOffset + 0x1c;
                    curLoop++;
                    counter = 0;
                } else {
                    vgm.vgmAnalyze = false;
                    logger.log(Level.DEBUG, "ret: not analyze 2");
                    return;
                }
            }

            int cmd = dataBuf[vgm.vgmAdr] & 0xff;
//if (!List.of(0xc0).contains(cmd)) {
            logger.log(Level.DEBUG, "[%s]: adr: 0x%x, cmd: 0x%x".formatted(model, vgm.vgmAdr, cmd)); // ok
//}
            if (vgm.vgmCmdTbl[cmd] != null) {
                //if (model == EnmModel.VirtualModel) logger.log(Level.DEBUG, "%05x : %02x ".formatted(vgmAdr, dataBuf[vgmAdr]));
                vgm.vgmCmdTbl[cmd].run();
            } else {
                // Unknown command
                logger.log(Level.WARNING, "[%s]:unknown command: adr: 0x%x cmd: 0x%x".formatted(model, vgm.vgmAdr, dataBuf[vgm.vgmAdr]));
                vgm.vgmAdr++;
            }
            countNum++;
            if (countNum > 100) {
                if (model == EnmModel.RealModel && countNum % 100 == 0) {
                    isDataBlock = true;
                    plugin.chipRegister.plugin(RealChipPlugin.class).process1(model);
                }
            }
        }

        if (model == EnmModel.RealModel && isDataBlock) {
            isDataBlock = false;
            //logger.log(Level.TRACE, "%s countNum:%d".formatted(model, countNum));
            countNum = 0;
        }

        // Send wait
        if (model == EnmModel.RealModel) {
            if (speed == 1) { // Apply weight only when speed is constant
                plugin.chipRegister.plugin(RealChipPlugin.class).process3(vgm.useChipYM2612Ch6, vgm.vgmWait);
            }
        }

        vgm.vgmWait--;
        counter++;
        frameCounter++;
    }

    /**
     * @param args 0: vgmGd3
     */
    @Override
    public MetaData retrieveMetaData(byte[] buf, Object... args) {
        int vgmGd3 = (int) args[0];

        int adr = vgmGd3 + 12 + 0x14;
        metaData = Common.getMetaData(buf, adr);
        metaData.set(Tag.Chip, usedChips);

        return metaData;
    }

    @Override
    public long getDriverCounter() {
        return frameCounter;
    }

    @Override
    public long whichCounter(long real, long virtual) {
        return Math.min(real, virtual);
    }
}
