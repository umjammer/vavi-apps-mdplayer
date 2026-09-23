package mdplayer.form.kb.chip;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import mdplayer.Audio;
import mdplayer.Common;
import mdplayer.Tables;
import mdplayer.chips.Ym2608Chip;
import mdplayer.chips.Ym2612Chip;
import mdplayer.driver.xgm.XgmDriver;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.form.View;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdplayer.form.sys.setting.SettingNukedPanel;
import mdplayer.driver.FileFormat;
import mdplayer.driver.xgm.XGMFileFormat;
import mdsound.MDSound;
import mdsound.instrument.Ym2610Inst;

import static mdplayer.form.FrameBuffer.rType;


public class FormYM2612 extends FormChipBase<FormYM2612.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormYM2612.class);

    public FormYM2612(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());
        initializeComponent();

        // initScreen (via bind) draws the XGM variant of the skin from the song format; with no song
        // loaded there is none yet, and changeScreenParams picks it up once one plays
        if (audio.plugin != null) newParam.fileFormat = audio.plugin.getFileFormat();
        bind(Common.getImage("planeYM2612"));
    }

    @Override
    public void initScreen() {
        boolean YM2612Type = (chipId == 0) ? parent.setting.getYM2612Type()[0].getUseReal()[0] : parent.setting.getYM2612Type()[1].getUseReal()[0];
        int tp = YM2612Type ? 1 : 0;
        drawScreenInitYM2612(frameBuffer, tp, (chipId == 0)
                        ? parent.setting.getYM2612Type()[0].getRealChipInfo()[0].getOnlyPCMEmulation()
                        : parent.setting.getYM2612Type()[1].getRealChipInfo()[0].getOnlyPCMEmulation(),
                newParam.fileFormat instanceof XGMFileFormat);
        newParam.channels[5].pcmBuff = 100;
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("YM2612", chipId, getLocation());
            isClosed = true;
        }

        @Override
        public void windowOpened(WindowEvent e) {
            setLocation(new Point(x, y));

            frameSizeW = getWidth() - getSize().width;
            frameSizeH = getHeight() - getSize().height;

            changeZoom();
        }
    };

    private void changeZoom() {
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeYM2612").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2612").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeYM2612").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2612").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeYM2612").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2612").getHeight() * zoom));
        componentListener.componentResized(null);
    }

    private final ComponentListener componentListener = new ComponentAdapter() {
        @Override
        public void componentMoved(ComponentEvent e) {
            prefs.putInt("x", e.getComponent().getX());
            prefs.putInt("y", e.getComponent().getY());
        }

        @Override
        public void componentResized(ComponentEvent e) {
        }
    };

    private static final byte[] md = {
            (byte) (0x08 << 4),
            (byte) (0x08 << 4),
            (byte) (0x08 << 4),
            (byte) (0x08 << 4),
            (byte) (0x0c << 4),
            (byte) (0x0e << 4),
            (byte) (0x0e << 4),
            (byte) (0x0f << 4)
    };

    private static int intOf(Map<String, Object> info, String key) {
        return info.get(key) instanceof Integer i ? Math.abs(i) : 0;
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(Ym2612Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        newParam.fileFormat = audio.plugin.getFileFormat();

        int[] fmVol = new int[9];
        for (int ch = 0; ch < 6; ch++) {
            fmVol[ch] = Math.max(intOf(info, "channels." + ch + ".volumeL"),
                    intOf(info, "channels." + ch + ".volumeR"));
        }

        int[] fmCh3SlotVol = new int[4];
        for (int slot = 0; slot < 4; slot++) {
            fmCh3SlotVol[slot] = intOf(info, "channels.2.slots." + slot + ".volume");
        }

        int[][] fmRegister = (int[][]) info.get("register");
        int[] fmKey = (int[]) info.get("keyOn");

        boolean isFmEx = (fmRegister[0][0x27] & 0x40) != 0;
        newParam.channels[2].ex = isFmEx;

        newParam.lfoSw = (fmRegister[0][0x22] & 0x8) != 0;
        newParam.lfoFrq = (fmRegister[0][0x22] & 0x7);
        newParam.timerA = fmRegister[0][0x24] | ((fmRegister[0][0x25] & 0x3) << 8);
        newParam.timerB = fmRegister[0][0x26];

        //int masterClock = audio.chipRegister.getChipInfo(Ym2610Inst.class).clock;
        //int defaultMasterClock = 8000000;
        //float mul = 1.0f;
        //if (masterClock != 0)
        //    mul = masterClock / (float)defaultMasterClock;

        int defaultMasterClock = 8000000;
        float ssgMul = 1.0f;
        int masterClock = defaultMasterClock;
        int clock = clock(Ym2610Inst.class);
        if (clock != 0) {
            ssgMul = clock / (float) defaultMasterClock;
            masterClock = clock;
        }

        float fmDiv = 6;
        float ssgDiv = 4;
        ssgMul = ssgMul * ssgDiv / 4;

        for (int ch = 0; ch < 6; ch++) {
            int p = (ch > 2) ? 1 : 0;
            int c = (ch > 2) ? ch - 3 : ch;
            newParam.channels[ch].slot = (byte) (fmKey[ch] >> 4);
            for (int i = 0; i < 4; i++) {
                int ops = (i == 0) ? 0 : ((i == 1) ? 8 : ((i == 2) ? 4 : 12));
                newParam.channels[ch].inst[i * 11 + 0] = fmRegister[p][0x50 + ops + c] & 0x1f; //AR
                newParam.channels[ch].inst[i * 11 + 1] = fmRegister[p][0x60 + ops + c] & 0x1f; //DR
                newParam.channels[ch].inst[i * 11 + 2] = fmRegister[p][0x70 + ops + c] & 0x1f; //SR
                newParam.channels[ch].inst[i * 11 + 3] = fmRegister[p][0x80 + ops + c] & 0x0f; //RR
                newParam.channels[ch].inst[i * 11 + 4] = (fmRegister[p][0x80 + ops + c] & 0xf0) >> 4;//SL
                newParam.channels[ch].inst[i * 11 + 5] = fmRegister[p][0x40 + ops + c] & 0x7f;//TL
                newParam.channels[ch].inst[i * 11 + 6] = (fmRegister[p][0x50 + ops + c] & 0xc0) >> 6;//KS
                newParam.channels[ch].inst[i * 11 + 7] = fmRegister[p][0x30 + ops + c] & 0x0f;//ML
                newParam.channels[ch].inst[i * 11 + 8] = (fmRegister[p][0x30 + ops + c] & 0x70) >> 4;//DT
                newParam.channels[ch].inst[i * 11 + 9] = (fmRegister[p][0x60 + ops + c] & 0x80) >> 7;//AM
                newParam.channels[ch].inst[i * 11 + 10] = fmRegister[p][0x90 + ops + c] & 0x0f;//SG
            }
            newParam.channels[ch].inst[44] = fmRegister[p][0xb0 + c] & 0x07;//AL
            newParam.channels[ch].inst[45] = (fmRegister[p][0xb0 + c] & 0x38) >> 3;//FB
            newParam.channels[ch].inst[46] = (fmRegister[p][0xb4 + c] & 0x38) >> 4;//AMS
            newParam.channels[ch].inst[47] = fmRegister[p][0xb4 + c] & 0x07;//FMS

            newParam.channels[ch].pan = (fmRegister[p][0xb4 + c] & 0xc0) >> 6;

            int freq = 0;
            int octav;
            int n = -1;
            if (ch != 2 || !isFmEx) {
                freq = fmRegister[p][0xa0 + c] + (fmRegister[p][0xa4 + c] & 0x07) * 0x100;
                octav = (fmRegister[p][0xa4 + c] & 0x38) >> 3;
                newParam.channels[ch].freq = (freq & 0x7ff) | ((octav & 7) << 11);
                float ff = freq / ((2 << 20) / (masterClock / (24 * fmDiv))) * (2 << (octav + 2));
                ff /= 1038f;

                if ((fmKey[ch] & 1) != 0) {
                    n = Math.clamp(Ym2608Chip.searchYM2608Adpcm(ff) - 1, 0, 95);
                    //if (ch == 0) {
                    //    logger.log(Level.TRACE, "freq:%d  masterClock:%d  fmDiv:%d  octav:%d ff:%d  n:%d".formatted(freq, masterClock, fmDiv, octav,ff,n));
                    //}
                }

                byte con = (byte) (fmKey[ch]);
                int v = 127;
                int m = md[fmRegister[p][0xb0 + c] & 7];

                // OP1
                v = (((con & 0x10) != 0) && ((m & 0x10) != 0) && v > (fmRegister[p][0x40 + c] & 0x7f)) ? (fmRegister[p][0x40 + c] & 0x7f) : v;
                // OP3
                v = (((con & 0x20) != 0) && ((m & 0x20) != 0) && v > (fmRegister[p][0x44 + c] & 0x7f)) ? (fmRegister[p][0x44 + c] & 0x7f) : v;
                // OP2
                v = (((con & 0x40) != 0) && ((m & 0x40) != 0) && v > (fmRegister[p][0x48 + c] & 0x7f)) ? (fmRegister[p][0x48 + c] & 0x7f) : v;
                // OP4
                v = (((con & 0x80) != 0) && ((m & 0x80) != 0) && v > (fmRegister[p][0x4c + c] & 0x7f)) ? (fmRegister[p][0x4c + c] & 0x7f) : v;
                newParam.channels[ch].volumeL = Math.clamp((int) ((127 - v) / 127.0 * ((fmRegister[p][0xb4 + c] & 0x80) != 0 ? 1 : 0) * fmVol[ch] / 80.0), 0, 19);
                newParam.channels[ch].volumeR = Math.clamp((int) ((127 - v) / 127.0 * ((fmRegister[p][0xb4 + c] & 0x40) != 0 ? 1 : 0) * fmVol[ch] / 80.0), 0, 19);
            } else {
                int m = md[fmRegister[0][0xb0 + 2] & 7];
                if (parent.setting.getOther().getExAll()) m = 0xf0;
                freq = fmRegister[0][0xa9] + (fmRegister[0][0xad] & 0x07) * 0x100;
                octav = (fmRegister[0][0xad] & 0x38) >> 3;
                newParam.channels[2].freq = (freq & 0x7ff) | ((octav & 7) << 11);
                float ff = freq / ((2 << 20) / (masterClock / (24 * fmDiv))) * (2 << (octav + 2));
                ff /= 1038f;

                if ((fmKey[2] & 0x10) != 0 && ((m & 0x10) != 0))
                    n = Math.clamp(Ym2608Chip.searchYM2608Adpcm(ff) - 1, 0, 95);

                int v = ((m & 0x10) != 0) ? fmRegister[p][0x40 + c] : 127;
                newParam.channels[2].volumeL = Math.clamp((int) ((127 - v) / 127.0 * ((fmRegister[0][0xb4 + 2] & 0x80) != 0 ? 1 : 0) * fmCh3SlotVol[0] / 80.0), 0, 19);
                newParam.channels[2].volumeR = Math.clamp((int) ((127 - v) / 127.0 * ((fmRegister[0][0xb4 + 2] & 0x40) != 0 ? 1 : 0) * fmCh3SlotVol[0] / 80.0), 0, 19);
            }
            newParam.channels[ch].note = n;

        }

        for (int ch = 6; ch < 9; ch++) {
            // Operator 1′s frequency instanceof : A9 and ADH
            // Operator 2′s frequency instanceof : AA and AEH
            // Operator 3′s frequency instanceof : A8 and ACH
            // Operator 4′s frequency instanceof : A2 and A6H

            int[] exReg = {2, 0, -6};
            int c = exReg[ch - 6];

            newParam.channels[ch].pan = 0;

            if (isFmEx) {
                int m = md[fmRegister[0][0xb0 + 2] & 7];
                if (parent.setting.getOther().getExAll()) m = 0xf0;
                int op = ch - 5;
                op = op == 1 ? 2 : (op == 2 ? 1 : op);

                int freq = fmRegister[0][0xa8 + c] + (fmRegister[0][0xac + c] & 0x07) * 0x100;
                int octav = (fmRegister[0][0xac + c] & 0x38) >> 3;
                newParam.channels[ch].freq = (freq & 0x7ff) | ((octav & 7) << 11);
                int n = -1;
                if ((fmKey[2] & (0x10 << (ch - 5))) != 0 && ((m & (0x10 << op)) != 0)) {
                    float ff = freq / ((2 << 20) / (masterClock / (24 * fmDiv))) * (2 << (octav + 2));
                    ff /= 1038f;
                    n = Math.clamp(Ym2608Chip.searchYM2608Adpcm(ff) - 1, 0, 95);
                }
                newParam.channels[ch].note = n;

                int v = ((m & (0x10 << op)) != 0) ? fmRegister[0][0x42 + op * 4] : 127;
                newParam.channels[ch].volumeL = Math.clamp((int) ((127 - v) / 127.0 * fmCh3SlotVol[ch - 5] / 80.0), 0, 19);
            } else {
                newParam.channels[ch].note = -1;
                newParam.channels[ch].volumeL = 0;
            }
        }

        newParam.channels[5].pcmMode = (fmRegister[0][0x2b] & 0x80) >> 7;
        if (newParam.channels[5].pcmBuff > 0)
            newParam.channels[5].pcmBuff--;
        if (newParam.channels[5].pcmMode != 0) {
            newParam.channels[5].volumeL = Math.clamp(fmVol[5] / 80, 0, 19);
            newParam.channels[5].volumeR = Math.clamp(fmVol[5] / 80, 0, 19);
        }

        if (newParam.fileFormat instanceof XGMFileFormat && audio.plugin.driverVirtual instanceof XgmDriver) {

            if (audio.plugin.driverVirtual != null && ((XgmDriver) audio.plugin.driverVirtual).getXgmPcm() != null) {
                for (int i = 0; i < 4; i++) {
                    if (((XgmDriver) audio.plugin.driverVirtual).getXgmPcm()[i].isPlaying) {
                        newParam.xpcmInst[i] = ((XgmDriver) audio.plugin.driverVirtual).getXgmPcm()[i].inst;
                        int d = (((XgmDriver) audio.plugin.driverVirtual).getXgmPcm()[i].data / 6);
                        d = Math.min(d, 19);
                        newParam.xpcmVolL[i] = d;
                        newParam.xpcmVolR[i] = d;
                    } else {
                        newParam.xpcmInst[i] = 0;
                        newParam.xpcmVolL[i] = 0;
                        newParam.xpcmVolR[i] = 0;
                    }
                }
            }
        }
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(Ym2612Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        for (int c = 0; c < 9; c++) {

            Channel oyc = oldParam.channels[c];
            Channel nyc = newParam.channels[c];

            boolean YM2612type = (chipId == 0)
                    ? parent.setting.getYM2612Type()[0].getUseReal()[0]
                    : parent.setting.getYM2612Type()[1].getUseReal()[0];
            int tp = YM2612type ? 1 : 0;

            if (c == 2) {
                oyc.volumeL = frameBuffer.drawVolumeM(289, 8 + c * 8, 1, oyc.volumeL, nyc.volumeL, tp);
                oyc.volumeR = frameBuffer.drawVolumeM(289, 8 + c * 8, 2, oyc.volumeR, nyc.volumeR, tp);
                { int[] r = frameBuffer.pan(25, 8 + c * 8, oyc.pan, nyc.pan, oyc.pantp, tp); oyc.pan = r[0]; oyc.pantp = r[1]; }
                oyc.note = drawKeyBoardOPNM(frameBuffer, c, oyc.note, nyc.note, tp);
                InstOPN2(frameBuffer, 13, 96, c, oyc.inst, nyc.inst);
                Boolean[] r = drawCh3YM2612(frameBuffer, c, oyc.mask, nyc.mask, oyc.ex, nyc.ex, tp);
                oyc.mask = r[0]; oyc.ex = r[1];
                oyc.slot = frameBuffer.drawSlot(1 + 4 * 64, 8 + c * 8, oyc.slot, nyc.slot);
                oyc.freq = frameBuffer.font4Hex16Bit(1 + 4 * 68, 8 + c * 8, 0, oyc.freq, nyc.freq);
            } else if (c < 5) {
                oyc.volumeL = frameBuffer.drawVolumeM(289, 8 + c * 8, 1, oyc.volumeL, nyc.volumeL, tp);
                oyc.volumeR = frameBuffer.drawVolumeM(289, 8 + c * 8, 2, oyc.volumeR, nyc.volumeR, tp);
                { int[] r = frameBuffer.pan(25, 8 + c * 8, oyc.pan, nyc.pan, oyc.pantp, tp); oyc.pan = r[0]; oyc.pantp = r[1]; }
                oyc.note = drawKeyBoardOPNM(frameBuffer, c, oyc.note, nyc.note, tp);
                InstOPN2(frameBuffer, 13, 96, c, oyc.inst, nyc.inst);
                oyc.mask = drawChYM2612(frameBuffer, c, oyc.mask, nyc.mask, tp);
                oyc.slot = frameBuffer.drawSlot(1 + 4 * 64, 8 + c * 8, oyc.slot, nyc.slot);
                oyc.freq = frameBuffer.font4Hex16Bit(1 + 4 * 68, 8 + c * 8, 0, oyc.freq, nyc.freq);
            } else if (c == 5) {
                int tp6 = tp;
                int tp6v = tp;
                if (tp6 == 1 && parent.setting.getYM2612Type()[0].getRealChipInfo()[0].getOnlyPCMEmulation()) {
                    tp6v = newParam.channels[5].pcmMode == 0 ? 1 : 0; // The mode is determined based on volume only.
//                    tp6 = 0;
                }

                { int[] r = frameBuffer.pan(25, 8 + c * 8, oyc.pan, nyc.pan, oyc.pantp, tp6v); oyc.pan = r[0]; oyc.pantp = r[1]; }
                InstOPN2(frameBuffer, 13, 96, c, oyc.inst, nyc.inst);

                if (!(newParam.fileFormat instanceof XGMFileFormat)) {
                    if (oldParam.fileFormat != newParam.fileFormat) {
                        //
                        oyc.pcmMode = 1;
                        nyc.pcmMode = 0;
                        Object[] r = drawCh6YM2612XGM(frameBuffer, nyc.pcmBuff, oyc.pcmMode, nyc.pcmMode, oyc.mask, nyc.mask, oyc.tp, tp6v);
                        oyc.pcmMode = (int) r[0]; oyc.mask = (Boolean) r[1]; oyc.tp = (int) r[2];
                        oldParam.fileFormat = newParam.fileFormat;
                    }

                    Object[] r = drawCh6YM2612(frameBuffer, nyc.pcmBuff, oyc.pcmMode, nyc.pcmMode, oyc.mask, nyc.mask, oyc.tp, tp6v);
                    oyc.pcmMode = (int) r[0]; oyc.mask = (Boolean) r[1]; oyc.tp = (int) r[2];
                    oyc.volumeL = frameBuffer.drawVolumeM(289, 8 + c * 8, 1, oyc.volumeL, nyc.volumeL, tp6v);
                    oyc.volumeR = frameBuffer.drawVolumeM(289, 8 + c * 8, 2, oyc.volumeR, nyc.volumeR, tp6v);
                    oyc.note = drawKeyBoardOPNM(frameBuffer, c, oyc.note, nyc.note, tp6v);
                    oyc.slot = frameBuffer.drawSlot(1 + 4 * 64, 8 + c * 8, oyc.slot, nyc.slot);
                    oyc.freq = frameBuffer.font4Hex16Bit(1 + 4 * 68, 8 + c * 8, 0, oyc.freq, nyc.freq);
                } else {
                    if (oldParam.fileFormat != newParam.fileFormat) {
                        initScreen();
                        oldParam.fileFormat = newParam.fileFormat;
                    }

                    Object[] r = drawCh6YM2612XGM(frameBuffer, nyc.pcmBuff, oyc.pcmMode, nyc.pcmMode, oyc.mask, nyc.mask, oyc.tp, tp6v);
                    oyc.pcmMode = (int) r[0]; oyc.mask = (Boolean) r[1]; oyc.tp = (int) r[2];
                    if (newParam.channels[5].pcmMode == 0) {
                        oyc.volumeL = frameBuffer.drawVolumeM(289, 8 + c * 8, 1, oyc.volumeL, nyc.volumeL, tp6v);
                        oyc.volumeR = frameBuffer.drawVolumeM(289, 8 + c * 8, 2, oyc.volumeR, nyc.volumeR, tp6v);
                        oyc.note = drawKeyBoardOPNM(frameBuffer, c, oyc.note, nyc.note, tp6v);
                        oyc.slot = frameBuffer.drawSlot(1 + 4 * 64, 8 + c * 8, oyc.slot, nyc.slot);
                        oyc.freq = frameBuffer.font4Hex16Bit(1 + 4 * 68, 8 + c * 8, 0, oyc.freq, nyc.freq);
                    } else {
                        for (int i = 0; i < 4; i++) {
                            oldParam.xpcmVolL[i] = drawVolumeXYOPN2(frameBuffer, (13 + i * 17) * 4 + 1, 12 * 4, 1, oldParam.xpcmVolL[i], newParam.xpcmVolL[i], tp6v);
                            oldParam.xpcmVolR[i] = drawVolumeXYOPN2(frameBuffer, (13 + i * 17) * 4 + 1, 12 * 4, 2, oldParam.xpcmVolR[i], newParam.xpcmVolR[i], tp6v);
                            if (oldParam.xpcmInst[i] != newParam.xpcmInst[i]) {
                                frameBuffer.drawFont4Int2(45 + i * 17 * 4, 48, tp6v, 2, newParam.xpcmInst[i]);
                                oldParam.xpcmInst[i] = newParam.xpcmInst[i];
                            }
                        }
                    }
                }
            } else {
                oyc.volumeL = frameBuffer.drawVolumeM(289, 8 + c * 8, 0, oyc.volumeL, nyc.volumeL, tp);
                oyc.note = drawKeyBoardOPNM(frameBuffer, c, oyc.note, nyc.note, tp);
                oyc.mask = drawChYM2612(frameBuffer, c, oyc.mask, nyc.mask, tp);
                oyc.freq = 0;
                oyc.freq = frameBuffer.font4Hex16Bit(1 + 4 * 68, 8 + c * 8, 0, oyc.freq, nyc.freq);
            }
        }

        oldParam.lfoSw = frameBuffer.drawLfoSw(16 + 1, 176, oldParam.lfoSw, newParam.lfoSw);
        oldParam.lfoFrq = frameBuffer.drawLfoFrq(64 + 1, 176, oldParam.lfoFrq, newParam.lfoFrq);
        oldParam.timerA = frameBuffer.font4Hex12Bit(1 + 29 * 4, 44 * 4, 0, oldParam.timerA, newParam.timerA);
        oldParam.timerB = frameBuffer.font4HexByte(1 + 43 * 4, 44 * 4, 0, oldParam.timerB, newParam.timerB);
    }

    private final MouseListener pbScreen_MouseClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
            int px = ev.getX() / zoom;
            int py = ev.getY() / zoom;

            // For top label row, do nothing
            if (py < 1 * 8) {
                // However, if you click on ch, the mask will be inverted.
                if (px < 8) {
                    for (int ch = 0; ch < 6; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(Ym2612Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(Ym2612Chip.class, chipId, ch);
                    }
                }
                return;
            }

            // keyboard
            if (py < 10 * 8) {
                int ch = (py / 8) - 1;
                if (ch < 0) return;
                if (6 <= ch && ch <= 8) {
                    ch = 2;
                }

                if (ev.getButton() == MouseEvent.BUTTON1) {
                    // Mask.
                    if (newParam.channels[ch].mask)
                        parent.resetChannelMask(Ym2612Chip.class, chipId, ch);
                    else
                        parent.setChannelMask(Ym2612Chip.class, chipId, ch);
                    return;
                }

                // Unmask.
                for (ch = 0; ch < 6; ch++) parent.resetChannelMask(Ym2612Chip.class, chipId, ch);
                return;
            }

            // Right-clicking on the sound does nothing.
            if (ev.getButton() == MouseEvent.BUTTON2) return;

            // Judgment in the tone display field
            int h = (py - 10 * 8) / (6 * 8);
            int w = Math.min(px / (29 * 4), 2);
            int instCh = h * 3 + w;

            if (instCh < 6) {
                // Copying a tone to the clipboard
                parent.getInstCh(Ym2612Chip.class, instCh, chipId);
            }
        }
    };

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeYM2612");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 184));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmYM2612
        //
        this.setPreferredSize(new Dimension(320, 184));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmYM2612");
        this.setTitle("Ym2612Inst");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static void drawScreenInitYM2612(FrameBuffer screen, int tp, boolean onlyPCM, boolean isXGM) {
        if (screen == null)
            return;

        for (int y = 0; y < 9; y++) {

            int d;
            // boolean YM2612type = chipId==0 ?
            // parent.setting.YM2612Type.UseScci : setting.YM2612SType.UseScci;
            int tp6 = tp;
            if (tp6 == 1 && onlyPCM) {
                // tp6 = 0;
            }

            // note
            screen.drawFont8(297, y * 8 + 8, 1, "   ");

            // keyboard
            for (int i = 0; i < 96; i++) {
                int kx = Tables.kbl[(i % 12) * 2] + i / 12 * 28;
                int kt = Tables.kbl[(i % 12) * 2 + 1];
                if (y != 5) {
                    screen.drawKbn(33 + kx, y * 8 + 8, kt, tp);
                } else {
                    if (!isXGM)
                        screen.drawKbn(33 + kx, y * 8 + 8, kt, tp6);
                }
            }

            if (isXGM) {
                drawCh6YM2612XGM_P(screen, 1, 48, 0, false, tp6);
            }

            if (y != 5) {
                d = -1;
                d = screen.drawVolumeM(289, 8 + y * 8, 0, d, 0, tp);
            }

            if (y < 6) {
                d = 99;
                int[] r = screen.pan(25, 8 + y * 8, d, 3, d, tp);
                d = r[0]; d = r[1];
                int b = 255;
                b = screen.drawSlot(257, 8 + y * 8, b, 0);
            }
            d = 1;
            d = screen.font4Hex16Bit(273, 8 + y * 8, 0, d, 0);

            if (y != 5) {
                //screen.drawChYM2612_P(1, y * 8 + 8, y, false, tp);
            } else {
                //screen.drawCh6YM2612_P(1, y * 8 + 8, 0, false, tp6);
                d = -1;
                d = screen.drawVolumeM(289, 8 + y * 8, 0, d, 0, tp6);
                d = -1;
                int[] r = screen.pan(25, 8 + y * 8, d, 3, d, tp6);
                d = r[0]; d = r[1];
            }
        }
    }

    private static void InstOPN2(FrameBuffer screen, int x, int y, int c, int[] oi, int[] ni) {
        int sx = (c % 3) * 4 * 29 + x;
        int sy = (c / 3) * 8 * 6 + y;

        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 11; i++) {
                if (oi[i + j * 11] != ni[i + j * 11]) {
                    screen.drawFont4Int(sx + i * 8 + (i > 5 ? 4 : 0), sy + j * 8, 0, (i == 5) ? 3 : 2, ni[i + j * 11]);
                    oi[i + j * 11] = ni[i + j * 11];
                }
            }
        }

        if (oi[44] != ni[44]) {
            screen.drawFont4Int(sx + 8 * 4, sy - 16, 0, 2, ni[44]);
            oi[44] = ni[44];
        }
        if (oi[45] != ni[45]) {
            screen.drawFont4Int(sx + 8 * 6, sy - 16, 0, 2, ni[45]);
            oi[45] = ni[45];
        }
        if (oi[46] != ni[46]) {
            screen.drawFont4Int(sx + 8 * 8 + 4, sy - 16, 0, 2, ni[46]);
            oi[46] = ni[46];
        }
        if (oi[47] != ni[47]) {
            screen.drawFont4Int(sx + 8 * 11, sy - 16, 0, 2, ni[47]);
            oi[47] = ni[47];
        }
    }

    private static int drawVolumeXYOPN2(FrameBuffer screen, int x, int y, int c, int ov, int nv, int tp) {
        if (ov == nv)
            return ov;

        int t = 0;
        int sy = 0;
        if (c == 1 || c == 2) {
            t = 4;
        }
        if (c == 2) {
            sy = 4;
        }

        // y *= 4;
        // x *= 4;

        for (int i = 0; i <= 19; i++) {
            screen.drawVolumeP(x + i * 2, y + sy, (1 + t), tp);
        }

        for (int i = 0; i <= nv; i++) {
            screen.drawVolumeP(x + i * 2, y + sy, i > 17 ? (2 + t) : (0 + t), tp);
        }

        ov = nv;
        return ov;
    }

    private static int drawKeyBoardOPNM(FrameBuffer screen, int y, int ot, int nt, int tp) {
        if (ot == nt)
            return ot;

        int kx;
        int kt;

        y = (y + 1) * 8;

        if (ot >= 0 && ot < 12 * 8) {
            kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
            kt = Tables.kbl[(ot % 12) * 2 + 1];
            screen.drawKbn(33 + kx, y, kt, tp);
        }

        if (nt >= 0 && nt < 12 * 8) {
            kx = Tables.kbl[(nt % 12) * 2] + nt / 12 * 28;
            kt = Tables.kbl[(nt % 12) * 2 + 1] + 4;
            screen.drawKbn(33 + kx, y, kt, tp);
        }

        screen.drawFont8(329, y, 1, "   ");

        if (nt >= 0) {
            screen.drawFont8(329, y, 1, Tables.kbn[nt % 12]);
            if (nt / 12 < 10) {
                screen.drawFont8(345, y, 1, Tables.kbo[nt / 12]);
            }
        }

        ot = nt;
        return ot;
    }

    private static Boolean drawChYM2612(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChYM2612_P(screen, 1, 8 + ch * 8, ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static Boolean[] drawCh3YM2612(FrameBuffer screen, int ch, Boolean om, Boolean nm, boolean oe, boolean ne, int tp) {
        if (om == nm && oe == ne) {
            return new Boolean[] {om, oe};
        }

        drawCh3YM2612_P(screen, 1, 8 + ch * 8, ch, nm == null ? false : nm, ne, tp);
        om = nm;
        oe = ne;
        return new Boolean[] {om, oe};
    }

    private static Object[] drawCh6YM2612(FrameBuffer screen, int buff, int ot, int nt, Boolean om, Boolean nm, int otp, int ntp) {
        if (buff == 0) {
            if (ot == nt && om == nm && otp == ntp) {
                return new Object[] {ot, om, otp};
            }
        }

        drawCh6YM2612_P(screen, 1, 48, nt, nm == null ? false : nm, ntp);
        ot = nt;
        om = nm;
        otp = ntp;
        return new Object[] {ot, om, otp};
    }

    private static Object[] drawCh6YM2612XGM(FrameBuffer screen, int buff, int ot, int nt, Boolean om, Boolean nm, int otp, int ntp) {
        if (buff == 0) {
            if (ot == nt && om == nm && otp == ntp) {
                return new Object[] {ot, om, otp};
            }
        }

        drawCh6YM2612XGM_P(screen, 1, 48, nt, nm == null ? false : nm, ntp);
        ot = nt;
        om = nm;
        otp = ntp;
        return new Object[] {ot, om, otp};
    }

    private static void drawCh6YM2612_P(FrameBuffer screen, int x, int y, int m, boolean mask, int tp) {
        if (m == 0) {
            screen.drawByteArray(x, y, rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 0, 16, 8);
            screen.drawFont8(x + 16, y, mask ? 1 : 0, "6");
        } else {
            screen.drawByteArray(x, y, rType[tp * 2 + (mask ? 1 : 0)], 128, 16, 0, 16, 8);
            screen.drawFont8(x + 16, y, 0, " ");
        }
    }

    private static void drawChYM2612_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (ch == 5) {
            return;
        }

        if (ch < 5) {
            screen.drawByteArray(x, y, rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 0, 16, 8);
            screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(ch + 1));
        } else if (ch < 10) {
            screen.drawByteArray(x, y, rType[tp * 2 + (mask ? 1 : 0)], 128, 32 * (ch - 5), 24, 32, 8);
        }
    }

    private static void drawCh6YM2612XGM_P(FrameBuffer screen, int x, int y, int m, boolean mask, int tp) {
        if (m == 0) {
            // FM mode

            screen.drawByteArray(x, y, rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 0, 16, 8);
            screen.drawFont8(x + 16, y, mask ? 1 : 0, "6");
            for (int i = 0; i < 96; i++) {
                int kx = Tables.kbl[(i % 12) * 2] + i / 12 * 28;
                int kt = Tables.kbl[(i % 12) * 2 + 1];
                screen.drawKbn(33 + kx, y, kt, tp);
            }
        } else {
            // PCM mode

            screen.drawByteArray(x, y, rType[tp * 2 + (mask ? 1 : 0)], 128, 16, 0, 16, 8);
            screen.drawFont8(x + 16, y, 0, " ");
            screen.drawFont4(x + 32, y, 0, " 1C00             2C00             3C00             4C00                ");
        }
    }

    public static void drawCh3YM2612_P(FrameBuffer screen, int x, int y, int ch, boolean mask, boolean ex, int tp) {
        if (!ex) {
            screen.drawByteArray(x, y, rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 0, 16, 8);
            screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(ch + 1));
        } else {
            screen.drawByteArray(x, y, rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 24, 24, 8);
        }
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        boolean ex = false;
        int pcmBuff = 0;
        int pcmMode = -1;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        FileFormat fileFormat = FileFormat.unknown;
        boolean lfoSw = false;
        int lfoFrq = -1;
        int timerA = -1;
        int timerB = -1;
        final int[] xpcmVolL = {-1, -1, -1, -1};
        final int[] xpcmVolR = {-1, -1, -1, -1};
        final int[] xpcmInst = {-1, -1, -1, -1};

        final Channel[] channels = {
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel()
        };
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "YM2612"; }
        @Override public String menuText() { return "OPN2"; }
        @Override public String category() { return "opn"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return Ym2612Chip.class; }
        @Override public boolean hasRegisterDump() { return true; }
        @Override public String title(int chipId) { return "Ym2612Inst (%s)".formatted(chipId == 0 ? "Primary" : "Secondary"); }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormYM2612(frm, chipId, zoom); }
        @Override public List<mdplayer.form.SettingTab> settingTabs() { return List.of(new SettingNukedPanel()); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 9) {
                audio.plugin.chipRegister.chip(Ym2612Chip.class).setMask(chipId, ch);
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 9) {
                audio.plugin.chipRegister.chip(Ym2612Chip.class).resetMask(chipId, ch);
            }
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (ch >= 0 && ch < 9) {
                if (mask)
                    audio.plugin.chipRegister.chip(Ym2612Chip.class).setMask(chipId, ch);
                else
                    audio.plugin.chipRegister.chip(Ym2612Chip.class).resetMask(chipId, ch);
            }
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 6; ch++)
                forceChannelMask(audio, Ym2612Chip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(Ym2612Chip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(5, MDSound.Chip.MAIN_TAG, Ym2612Chip.class, "ym2612", 200));
        }
    }
}
