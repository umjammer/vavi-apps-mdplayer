/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.chips;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import mdplayer.Common.EnmModel;
import mdplayer.RealChip.RSoundChip;
import mdplayer.Setting;
import mdplayer.Tables;
import mdsound.Instrument;
import mdsound.instrument.SegaPcmInst;
import musicDriverInterface.MetaData.Tag;
import vavi.util.ByteUtil;


/**
 * SegaPcmChip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-19 nsano initial version <br>
 */
public class SegaPcmChip extends BaseChip {

    private static final Logger logger = System.getLogger(SegaPcmChip.class.getName());

    private final Setting.ChipType2[] chipTypes = setting.getSEGAPCMType();

    private final RSoundChip[] realChips = {null, null};

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Instrument>[] implementations() {
        return new Class[] {SegaPcmInst.class};
    }

    /** {@link #write} keeps a muted channel switched off */
    @Override
    protected void applyMute(int chipId, int ch) {
    }

    public void write(int chipId, int offset, int data, EnmModel model) {
        fireEventHappened("led.on", chipId);

        if ((model == EnmModel.VirtualModel && (chipTypes[chipId] == null || !chipTypes[chipId].getUseReal()[0])) ||
                (model == EnmModel.RealModel && (realChips != null && realChips[chipId] != null))) {
            if ((offset & 0x87) == 0x86) {
                int ch = (offset >> 3) & 0xf;
                data = isMuted(chipId, ch) ? data | 0x01 : data;
            }
        }

        if (model == EnmModel.VirtualModel) {
            if (!chipTypes[chipId].getUseReal()[0])
                context.mds.write(inst(chipId), chipId, 0, offset, data);
//logger.log(Level.TRACE, "chipId=%d offset=%x data=%x ".formatted(chipId, offset, data));
        } else {
            if (realChips != null && realChips[chipId] != null)
                realChips[chipId].setRegister(offset, data);
        }
    }

    public void writePcm(int chipId,
                         int romSize,
                         int dataStart,
                         int dataLength,
                         byte[] romData,
                         int srcStartAdr,
                         EnmModel model) {
        fireEventHappened("led.on", chipId);

        if (model == EnmModel.VirtualModel) {
            context.mds.inst(SegaPcmInst.class).writePcm(chipId, romSize, dataStart, dataLength, romData, srcStartAdr);
        } else {
            if (realChips != null && realChips[chipId] != null) {
                // Start address setting
                realChips[chipId].setRegister(0x10000, dataStart);
                realChips[chipId].setRegister(0x10001, dataStart >> 8);
                realChips[chipId].setRegister(0x10002, dataStart >> 16);
                // Data Transfer
                for (int cnt = 0; cnt < dataLength; cnt++) {
                    realChips[chipId].setRegister(0x10004, romData[srcStartAdr + cnt] & 0xff);
                }
                realChips[chipId].setRegister(0x10006, romSize);

                context.chipRegister.plugin(RealChipPlugin.class).realChip.sendData();
            }
        }

        dumpDataForSegaPCM(model, "PCMData", dataLength, romData, srcStartAdr);
    }

    public void writeClock(int chipId, int clock, EnmModel model) {
        if (model == EnmModel.VirtualModel) {
        } else {
            if (realChips != null && realChips[chipId] != null) {
                realChips[chipId].setRegister(0x10005, clock);
            }
        }
    }

    @Override
    public Map<String, Object> getInfo(int chipId) {
        SegaPcmInst inst = context.mds.inst(SegaPcmInst.class);
        return inst == null ? Collections.emptyMap() : inst.getView(chipId, "info");
    }

    private void dumpDataForSegaPCM(EnmModel model, String chipName, int adr, byte[] romData, int len) {
        if (model == EnmModel.RealModel) return;
        if (!setting.getOther().getDumpSwitch()) return;

        try {
            String dFn = Path.of(setting.getOther().getDumpPath(), "%2$s_%3$s_%1$03d.wav".formatted(dumpCounter++, chipName, context.driverReal.metaData.getFirst(Tag.Title).replace("*", "").replace("?", "").replace(" ", "").replace("\"", ""))).toString();
            List<Byte> des = new ArrayList<>();

            // 'RIFF'
            des.add((byte) 'R');
            des.add((byte) 'I');
            des.add((byte) 'F');
            des.add((byte) 'F');
            // Size
            //int fsize = src.length + 36;
            int fsize = len + 36;
            des.add((byte) ((fsize & 0xff) >> 0));
            des.add((byte) ((fsize & 0xff00) >> 8));
            des.add((byte) ((fsize & 0xff_0000) >> 16));
            des.add((byte) ((fsize & 0xff00_0000) >>> 24));
            // 'WAVE'
            des.add((byte) 'W');
            des.add((byte) 'A');
            des.add((byte) 'V');
            des.add((byte) 'E');
            // 'fmt '
            des.add((byte) 'f');
            des.add((byte) 'm');
            des.add((byte) 't');
            des.add((byte) ' ');
            // Size(16)
            des.add((byte) 0x10);
            des.add((byte) 0);
            des.add((byte) 0);
            des.add((byte) 0);
            // Format(1)
            des.add((byte) 0x01);
            des.add((byte) 0x00);
            // Channel Number(mono)
            des.add((byte) 0x01);
            des.add((byte) 0x00);
            // Sampling Frquency(16KHz)
            des.add((byte) 0x80);
            des.add((byte) 0x3e);
            des.add((byte) 0);
            des.add((byte) 0);
            // Average Data Percentage(16K)
            des.add((byte) 0x80);
            des.add((byte) 0x3e);
            des.add((byte) 0);
            des.add((byte) 0);
            // Block size(1)
            des.add((byte) 0x01);
            des.add((byte) 0x00);
            // Bit depth(8bit)
            des.add((byte) 0x08);
            des.add((byte) 0x00);

            // 'data'
            des.add((byte) 'd');
            des.add((byte) 'a');
            des.add((byte) 't');
            des.add((byte) 'a');
            // Size(Data Size)
            des.add((byte) ((len & 0xff) >> 0));
            des.add((byte) ((len & 0xff00) >> 8));
            des.add((byte) ((len & 0xff_0000) >> 16));
            des.add((byte) ((len & 0xff00_0000) >>> 24));

            for (int i = 0; i < len; i++) {
                des.add(romData[adr + i]);
            }

            // output
            Files.write(Path.of(dFn), ByteUtil.toByteArray(des));

        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    public static int searchSegaPCMNote(double ml) {
        double m = Double.MAX_VALUE;
        int n = 0;
        for (int i = 0; i < 12 * 8; i++) {
            double a = Math.abs(ml - (Tables.pcmMulTbl[i % 12 + 12] * Math.pow(2, ((i / 12) - 4))));
            if (m > a) {
                m = a;
                n = i;
            }
        }
        return n;
    }
}
