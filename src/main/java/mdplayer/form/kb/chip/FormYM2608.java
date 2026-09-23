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
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.Tables;
import mdplayer.chips.Ym2608Chip;
import mdplayer.form.VisVolume;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.Meters;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;
import mdsound.instrument.Ym2608Inst;

import static mdplayer.form.kb.chip.FormYM2612.drawCh3YM2612_P;
import mdplayer.form.View;


public class FormYM2608 extends FormChipBase<FormYM2608.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormYM2608.class);

    public FormYM2608(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());
        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeD"), null, zoom);
        boolean YM2608Type = (chipId == 0)
                ? parent.setting.getYM2608Type()[0].getUseReal()[0]
                : parent.setting.getYM2608Type()[1].getUseReal()[0];
        int YM2608SoundLocation = (chipId == 0)
                ? parent.setting.getYM2608Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYM2608Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !YM2608Type ? 0 : (YM2608SoundLocation < 0 ? 2 : 1);
        drawScreenInitYM2608(frameBuffer, tp);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("YM2608", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeD").getWidth() * zoom, frameSizeH + Common.getImage("planeD").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeD").getWidth() * zoom, frameSizeH + Common.getImage("planeD").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeD").getWidth() * zoom, frameSizeH + Common.getImage("planeD").getHeight() * zoom));
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

    @Override
    public void initScreen() {
        for (int c = 0; c < newParam.channels.length; c++) {
            newParam.channels[c].note = -1;
        }
        boolean YM2608Type = (chipId == 0)
                ? parent.setting.getYM2608Type()[0].getUseReal()[0]
                : parent.setting.getYM2608Type()[1].getUseReal()[0];
        int YM2608SoundLocation = (chipId == 0)
                ? parent.setting.getYM2608Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYM2608Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !YM2608Type ? 0 : (YM2608SoundLocation < 0 ? 2 : 1);
        drawScreenInitYM2608(frameBuffer, tp);
    }

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

    private static final float[] fmDivTbl = {6, 3, 2};
    private static final float[] ssgDivTbl = {4, 2, 1};

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(Ym2608Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int[][] ym2608Register = (int[][]) info.get("register");
        int[] fmKeyYM2608 = (int[]) info.get("keyOn");
        int[] ym2608Vol = (int[]) info.get("volume");
        int[] ym2608Ch3SlotVol = (int[]) info.get("ch3SlotVolume");
        int[][] ym2608Rhythm = (int[][]) info.get("rhythmVolume");
        int[] ym2608AdpcmVol = (int[]) info.get("adpcmVolume");

        newParam.timerA = ym2608Register[0][0x24] | ((ym2608Register[0][0x25] & 0x3) << 8);
        newParam.timerB = ym2608Register[0][0x26];
        newParam.rhythmTotalLevel = ym2608Register[0][0x11];
        newParam.adpcmLevel = ym2608Register[1][0x0b];

        boolean isFmEx = (ym2608Register[0][0x27] & 0x40) > 0;
        newParam.channels[2].ex = isFmEx;

        int defaultMasterClock = 7987200;
        float ssgMul = 1.0f;
        int masterClock = defaultMasterClock;
        int clock = clock(Ym2608Inst.class);
        if (clock != 0) {
            ssgMul = clock / (float) defaultMasterClock;
            masterClock = clock;
        }

        int divInd = ym2608Register[0][0x2d];
        if (divInd < 0 || divInd > 2) divInd = 0;
        float fmDiv = fmDivTbl[divInd];
        float ssgDiv = ssgDivTbl[divInd];
        ssgMul = ssgMul * ssgDiv / 4;

        newParam.lfoSw = (ym2608Register[0][0x22] & 0x8) != 0;
        newParam.lfoFrq = (ym2608Register[0][0x22] & 0x7);

        for (int ch = 0; ch < 6; ch++) {
            int p = (ch > 2) ? 1 : 0;
            int c = (ch > 2) ? ch - 3 : ch;
            for (int i = 0; i < 4; i++) {
                int ops = (i == 0) ? 0 : ((i == 1) ? 8 : ((i == 2) ? 4 : 12));
                newParam.channels[ch].inst[i * 11 + 0] = ym2608Register[p][0x50 + ops + c] & 0x1f; // AR
                newParam.channels[ch].inst[i * 11 + 1] = ym2608Register[p][0x60 + ops + c] & 0x1f; // DR
                newParam.channels[ch].inst[i * 11 + 2] = ym2608Register[p][0x70 + ops + c] & 0x1f; // SR
                newParam.channels[ch].inst[i * 11 + 3] = ym2608Register[p][0x80 + ops + c] & 0x0f; // RR
                newParam.channels[ch].inst[i * 11 + 4] = (ym2608Register[p][0x80 + ops + c] & 0xf0) >> 4; // SL
                newParam.channels[ch].inst[i * 11 + 5] = ym2608Register[p][0x40 + ops + c] & 0x7f; // TL
                newParam.channels[ch].inst[i * 11 + 6] = (ym2608Register[p][0x50 + ops + c] & 0xc0) >> 6; // KS
                newParam.channels[ch].inst[i * 11 + 7] = ym2608Register[p][0x30 + ops + c] & 0x0f; // ML
                newParam.channels[ch].inst[i * 11 + 8] = (ym2608Register[p][0x30 + ops + c] & 0x70) >> 4; // DT
                newParam.channels[ch].inst[i * 11 + 9] = (ym2608Register[p][0x60 + ops + c] & 0x80) >> 7; // AM
                newParam.channels[ch].inst[i * 11 + 10] = ym2608Register[p][0x90 + ops + c] & 0x0f; // SG
            }
            newParam.channels[ch].inst[44] = ym2608Register[p][0xb0 + c] & 0x07; // AL
            newParam.channels[ch].inst[45] = (ym2608Register[p][0xb0 + c] & 0x38) >> 3; // FB
            newParam.channels[ch].inst[46] = (ym2608Register[p][0xb4 + c] & 0x38) >> 4; // AMS
            newParam.channels[ch].inst[47] = ym2608Register[p][0xb4 + c] & 0x07; // FMS

            newParam.channels[ch].pan = (ym2608Register[p][0xb4 + c] & 0xc0) >> 6;
            newParam.channels[ch].slot = (byte) (fmKeyYM2608[ch] >> 4);

            int freq;
            int octav;
            int n = -1;
            if (ch != 2 || !isFmEx) {
                octav = (ym2608Register[p][0xa4 + c] & 0x38) >> 3;
                freq = ym2608Register[p][0xa0 + c] + (ym2608Register[p][0xa4 + c] & 0x07) * 0x100;
                newParam.channels[ch].freq = (freq & 0x7ff) | ((octav & 7) << 11);
                float ff = freq / ((2 << 20) / (masterClock / (24 * fmDiv))) * (2 << (octav + 2));
                ff /= 1038f;

                if ((fmKeyYM2608[ch] & 1) != 0)
                    n = Math.clamp(Ym2608Chip.searchYM2608Adpcm(ff) - 1, 0, 95);

                byte con = (byte) (fmKeyYM2608[ch]);
                int v = 127;
                int m = md[ym2608Register[p][0xb0 + c] & 7];
                // OP1
                v = (((con & 0x10) != 0) && ((m & 0x10) != 0) && v > (ym2608Register[p][0x40 + c] & 0x7f)) ? (ym2608Register[p][0x40 + c] & 0x7f) : v;
                // OP3
                v = (((con & 0x20) != 0) && ((m & 0x20) != 0) && v > (ym2608Register[p][0x44 + c] & 0x7f)) ? (ym2608Register[p][0x44 + c] & 0x7f) : v;
                // OP2
                v = (((con & 0x40) != 0) && ((m & 0x40) != 0) && v > (ym2608Register[p][0x48 + c] & 0x7f)) ? (ym2608Register[p][0x48 + c] & 0x7f) : v;
                // OP4
                v = (((con & 0x80) != 0) && ((m & 0x80) != 0) && v > (ym2608Register[p][0x4c + c] & 0x7f)) ? (ym2608Register[p][0x4c + c] & 0x7f) : v;
                newParam.channels[ch].volumeL = Math.clamp((int) ((127 - v) / 127.0 * ((ym2608Register[p][0xb4 + c] & 0x80) != 0 ? 1 : 0) * ym2608Vol[ch] / 80.0), 0, 19);
                newParam.channels[ch].volumeR = Math.clamp((int) ((127 - v) / 127.0 * ((ym2608Register[p][0xb4 + c] & 0x40) != 0 ? 1 : 0) * ym2608Vol[ch] / 80.0), 0, 19);

            } else {
                int m = md[ym2608Register[0][0xb0 + 2] & 7];
                if (parent.setting.getOther().getExAll()) m = 0xf0;
                freq = ym2608Register[0][0xa9] + (ym2608Register[0][0xad] & 0x07) * 0x100;
                octav = (ym2608Register[0][0xad] & 0x38) >> 3;
                newParam.channels[2].freq = (freq & 0x7ff) | ((octav & 7) << 11);
                float ff = freq / ((2 << 20) / (masterClock / (24 * fmDiv))) * (2 << (octav + 2));
                ff /= 1038f;

                if ((fmKeyYM2608[2] & 0x10) > 0 && ((m & 0x10) != 0))
                    n = Math.clamp(Ym2608Chip.searchYM2608Adpcm(ff) - 1, 0, 95);

                int v = ((m & 0x10) != 0) ? ym2608Register[p][0x40 + c] : 127;
                newParam.channels[2].volumeL = Math.clamp((int) ((127 - v) / 127.0 * ((ym2608Register[0][0xb4 + 2] & 0x80) != 0 ? 1 : 0) * ym2608Ch3SlotVol[0] / 80.0), 0, 19);
                newParam.channels[2].volumeR = Math.clamp((int) ((127 - v) / 127.0 * ((ym2608Register[0][0xb4 + 2] & 0x40) != 0 ? 1 : 0) * ym2608Ch3SlotVol[0] / 80.0), 0, 19);
            }
            newParam.channels[ch].note = n;
        }

        for (int ch = 6; ch < 9; ch++) { // FM EX
            int[] exReg = {2, 0, -6};
            int c = exReg[ch - 6];

            newParam.channels[ch].pan = 0;

            if (isFmEx) {
                int m = md[ym2608Register[0][0xb0 + 2] & 7];
                if (parent.setting.getOther().getExAll()) m = 0xf0;
                int op = ch - 5;
                op = op == 1 ? 2 : (op == 2 ? 1 : op);

                int freq = ym2608Register[0][0xa8 + c] + (ym2608Register[0][0xac + c] & 0x07) * 0x100;
                int octav = (ym2608Register[0][0xac + c] & 0x38) >> 3;
                newParam.channels[ch].freq = (freq & 0x7ff) | ((octav & 7) << 11);
                int n = -1;
                if ((fmKeyYM2608[2] & (0x10 << (ch - 5))) != 0 && ((m & (0x10 << op)) != 0)) {
                    float ff = freq / ((2 << 20) / (masterClock / (24 * fmDiv))) * (2 << (octav + 2));
                    ff /= 1038f;
                    n = Math.clamp(Ym2608Chip.searchYM2608Adpcm(ff) - 1, 0, 95);
                }
                newParam.channels[ch].note = n;

                int v = ((m & (0x10 << op)) != 0) ? ym2608Register[0][0x42 + op * 4] : 127;
                newParam.channels[ch].volumeL = Math.clamp((int) ((127 - v) / 127.0 * ym2608Ch3SlotVol[ch - 5] / 80.0), 0, 19);
            } else {
                newParam.channels[ch].note = -1;
                newParam.channels[ch].volumeL = 0;
            }
        }

        for (int ch = 0; ch < 3; ch++) { // SSG
            Channel channel = newParam.channels[ch + 9];

            boolean t = (ym2608Register[0][0x07] & (0x1 << ch)) == 0;
            boolean n = (ym2608Register[0][0x07] & (0x8 << ch)) == 0;
            channel.tn = (t ? 1 : 0) + (n ? 2 : 0);

            channel.volume = (int) (((t || n) ? 1 : 0) * (ym2608Register[0][0x08 + ch] & 0xf) * (20.0 / 16.0));
            if (!t && !n && channel.volume > 0) {
                channel.volume--;
            }

            if (channel.volume == 0) {
                channel.note = -1;
            } else {
                int ft = ym2608Register[0][0x00 + ch * 2];
                int ct = ym2608Register[0][0x01 + ch * 2];
                int tp = (ct << 8) | ft;
                channel.freq = tp;
                if (tp == 0) {
                    channel.note = -1;
                    //channel.volume = 0;
                } else {
                    float ftone = masterClock / (64.0f * (float) tp) * ssgMul; // 7987200 = MasterClock
                    channel.note = Common.searchSSGNote(ftone);
                }
            }
        }

        newParam.nfrq = ym2608Register[0][0x06] & 0x1f;
        newParam.efrq = ym2608Register[0][0x0c] * 0x100 + ym2608Register[0][0x0b];
        newParam.etype = (ym2608Register[0][0x0d] & 0xf);

        //ADPCM
        newParam.channels[12].pan = (ym2608Register[1][0x01] & 0xc0) >> 6; // ((ym2608Register[1][0x01] & 0xc0) >> 6) != 0 ? ((ym2608Register[1][0x01] & 0xc0) >> 6) : newParam.channels[12].pan;
        newParam.channels[12].volumeL = Math.clamp(ym2608AdpcmVol[0] / 80, 0, 19);
        newParam.channels[12].volumeR = Math.clamp(ym2608AdpcmVol[1] / 80, 0, 19);
        int delta = (ym2608Register[1][0x0a] << 8) | ym2608Register[1][0x09];
        newParam.channels[12].freq = delta;
        float frq = delta / 9447.0f;
        newParam.channels[12].note = (ym2608Register[1][0x00] & 0x80) != 0 ? (Ym2608Chip.searchYM2608Adpcm(frq) - 1) : -1;
        if ((ym2608Register[1][0x01] & 0xc0) == 0) {
            newParam.channels[12].note = -1;
        }

        for (int ch = 13; ch < 19; ch++) { // RHYTHM
            newParam.channels[ch].pan = (ym2608Register[0][0x18 + ch - 13] & 0xc0) >> 6;
            newParam.channels[ch].volumeL = Math.clamp(ym2608Rhythm[ch - 13][0] / 80, 0, 19);
            newParam.channels[ch].volumeR = Math.clamp(ym2608Rhythm[ch - 13][1] / 80, 0, 19);
            newParam.channels[ch].volumeRL = ym2608Register[0][ch - 13 + 0x18] & 0x1f;
        }
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(Ym2608Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        boolean ChipType2 = (chipId == 0)
                ? parent.setting.getYM2608Type()[0].getUseReal()[0]
                : parent.setting.getYM2608Type()[1].getUseReal()[0];
        int chipSoundLocation = (chipId == 0)
                ? parent.setting.getYM2608Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYM2608Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !ChipType2 ? 0 : (chipSoundLocation < 0 ? 2 : 1);

        for (int c = 0; c < 9; c++) {

            Channel oyc = oldParam.channels[c];
            Channel nyc = newParam.channels[c];

            if (c == 2) {
                oyc.volumeL = frameBuffer.drawVolumeM(288 + 1, 8 + c * 8, 1, oyc.volumeL, nyc.volumeL, tp);
                oyc.volumeR = frameBuffer.drawVolumeM(288 + 1, 8 + c * 8, 2, oyc.volumeR, nyc.volumeR, tp);
                { int[] r = frameBuffer.pan(25, 8 + c * 8, oyc.pan, nyc.pan, oyc.pantp, tp); oyc.pan = r[0]; oyc.pantp = r[1]; }
                oyc.note = drawKeyBoardOPNA(frameBuffer, 33, 8 + c * 8, oyc.note, nyc.note, tp);
                drawInstOPNA(frameBuffer, 4, 17 * 8, c, oyc.inst, nyc.inst);
                Boolean[] r = drawCh3YM2608(frameBuffer, c, oyc.mask, nyc.mask, oyc.ex, nyc.ex, tp);
                oyc.mask = r[0]; oyc.ex = r[1];
                oyc.slot = frameBuffer.drawSlot(1 + 4 * 64, 8 + c * 8, oyc.slot, nyc.slot);
                oyc.freq = frameBuffer.font4Hex16Bit(1 + 4 * 68, 8 + c * 8, 0, oyc.freq, nyc.freq);
            } else if (c < 6) {
                oyc.volumeL = frameBuffer.drawVolumeM(288 + 1, 8 + c * 8, 1, oyc.volumeL, nyc.volumeL, tp);
                oyc.volumeR = frameBuffer.drawVolumeM(288 + 1, 8 + c * 8, 2, oyc.volumeR, nyc.volumeR, tp);
                { int[] r = frameBuffer.pan(25, 8 + c * 8, oyc.pan, nyc.pan, oyc.pantp, tp); oyc.pan = r[0]; oyc.pantp = r[1]; }
                oyc.note = drawKeyBoardOPNA(frameBuffer, 33, 8 + c * 8, oyc.note, nyc.note, tp);
                drawInstOPNA(frameBuffer, 4, 17 * 8, c, oyc.inst, nyc.inst);
                oyc.mask = drawChYM2608(frameBuffer, c, oyc.mask, nyc.mask, tp);
                oyc.slot = frameBuffer.drawSlot(1 + 4 * 64, 8 + c * 8, oyc.slot, nyc.slot);
                oyc.freq = frameBuffer.font4Hex16Bit(1 + 4 * 68, 8 + c * 8, 0, oyc.freq, nyc.freq);
            } else {
                oyc.volumeL = frameBuffer.drawVolumeM(288 + 1, 8 + (c + 3) * 8, 0, oyc.volumeL, nyc.volumeL, tp);
//                if (c == 7 && oyc.note != nyc.note) {
//                    logger.log(Level.TRACE, "note:%d".formatted(nyc.note));
//                    int[][] ym2608Register = audio.GetYM2608Register(chipId);
//                    int freq1 = ym2608Register[0][0xa9] + (ym2608Register[0][0xad]) * 0x100;
//                    int freq2 = ym2608Register[0][0xa8] + (ym2608Register[0][0xac]) * 0x100;
//                    int freq3 = ym2608Register[0][0xaa] + (ym2608Register[0][0xae]) * 0x100;
//                    int freq4 = ym2608Register[0][0xa2] + (ym2608Register[0][0xa6]) * 0x100;
//                    logger.log(Level.TRACE, "frq:%4x %4x %4x %4x".formatted(freq1, freq2, freq3, freq4));
//                }
                oyc.note = drawKeyBoardOPNA(frameBuffer, 33, 8 + (c + 3) * 8, oyc.note, nyc.note, tp);
                oyc.mask = drawChYM2608(frameBuffer, c, oyc.mask, nyc.mask, tp);
                oyc.freq = frameBuffer.font4Hex16Bit(1 + 4 * 68, 8 + (c + 3) * 8, 0, oyc.freq, nyc.freq);
            }
        }
        // SSG
        for (int c = 0; c < 3; c++) {
            Channel oyc = oldParam.channels[c + 9];
            Channel nyc = newParam.channels[c + 9];

            oyc.volume = frameBuffer.drawVolumeM(289, 8 + (c + 6) * 8, 0, oyc.volume, nyc.volume, tp);
            oyc.note = drawKeyBoardOPNA(frameBuffer, 33, (c + 6) * 8 + 8, oyc.note, nyc.note, tp);
            int[] r = drawTnOPNA(frameBuffer, 6, 2, c + 6, oyc.tn, nyc.tn, oyc.tntp, tp * 2);
            oyc.tn = r[0]; oyc.tntp = r[1];

            oyc.mask = drawChYM2608(frameBuffer, c + 9, oyc.mask, nyc.mask, tp);
            oyc.freq = frameBuffer.font4Hex16Bit(1 + 4 * 68, 8 + (c + 6) * 8, 0, oyc.freq, nyc.freq);
        }

        // ADPCM
        oldParam.channels[12].volumeL = frameBuffer.drawVolumeM(289, 8 + 12 * 8, 1, oldParam.channels[12].volumeL, newParam.channels[12].volumeL, tp);
        oldParam.channels[12].volumeR = frameBuffer.drawVolumeM(289, 8 + 12 * 8, 2, oldParam.channels[12].volumeR, newParam.channels[12].volumeR, tp);
        { int[] r = frameBuffer.pan(25, 8 + 12 * 8, oldParam.channels[12].pan, newParam.channels[12].pan, oldParam.channels[12].pantp, tp); oldParam.channels[12].pan = r[0]; oldParam.channels[12].pantp = r[1]; }
        oldParam.channels[12].note = drawKeyBoardOPNA(frameBuffer, 33, 8 + 12 * 8, oldParam.channels[12].note, newParam.channels[12].note, tp);
        drawChYM2608(frameBuffer, 12, oldParam.channels[12].mask, newParam.channels[12].mask, tp);
        oldParam.channels[12].freq = frameBuffer.font4Hex16Bit(1 + 4 * 68, 8 + 12 * 8, 0, oldParam.channels[12].freq, newParam.channels[12].freq);

        // Rhythm
        for (int c = 0; c < 6; c++) {
            Channel oyc = oldParam.channels[c + 13];
            Channel nyc = newParam.channels[c + 13];

            oyc.volumeL = drawVolumeYM2608Rhythm(frameBuffer, c, 1, oyc.volumeL, nyc.volumeL, tp);
            oyc.volumeR = drawVolumeYM2608Rhythm(frameBuffer, c, 2, oyc.volumeR, nyc.volumeR, tp);
            int [] r = drawPanYM2608Rhythm(frameBuffer, c, oyc.pan, nyc.pan, oyc.pantp, tp);
            oyc.pan = r[0]; oyc.pantp = r[1];
            oyc.volumeRL = frameBuffer.font4Int2(c * 4 * 15 + 4, 28 * 4, 0, 0, oyc.volumeRL, nyc.volumeRL);
        }
        oldParam.channels[13].mask = drawChYM2608Rhythm(frameBuffer, 0, oldParam.channels[13].mask, newParam.channels[13].mask, tp);

        oldParam.timerA = frameBuffer.font4Hex12Bit(85 * 4, 30 * 4, 0, oldParam.timerA, newParam.timerA);
        oldParam.timerB = frameBuffer.font4HexByte(85 * 4, 32 * 4, 0, oldParam.timerB, newParam.timerB);

        oldParam.lfoSw = frameBuffer.drawLfoSw(84 * 4, 18 * 8, oldParam.lfoSw, newParam.lfoSw);
        oldParam.lfoFrq = frameBuffer.drawLfoFrq(84 * 4, 19 * 8, oldParam.lfoFrq, newParam.lfoFrq);

        oldParam.nfrq = frameBuffer.Nfrq(84, 42, oldParam.nfrq, newParam.nfrq);
        oldParam.efrq = frameBuffer.drawEfrq(84, 44, oldParam.efrq, newParam.efrq);
        oldParam.etype = frameBuffer.drawEType(84, 46, oldParam.etype, newParam.etype);

        oldParam.rhythmTotalLevel = frameBuffer.font4Int2(84 * 4, 50 * 4, 0, 0, oldParam.rhythmTotalLevel, newParam.rhythmTotalLevel);
        oldParam.adpcmLevel = frameBuffer.font4Int3(84 * 4, 52 * 4, 0, 3, oldParam.adpcmLevel, newParam.adpcmLevel);
    }

    private final MouseListener pbScreen_MouseClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
            int px = ev.getX() / zoom;
            int py = ev.getY() / zoom;
            int ch;

            // For top label row, do nothing
            if (py < 1 * 8) {
                // However, if you click on ch, the mask will be inverted.
                if (px < 8) {
                    for (ch = 0; ch < 14; ch++) {
                        if (ch >= 9 && ch <= 11) continue;

                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(Ym2608Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(Ym2608Chip.class, chipId, ch);
                    }
                }
                return;
            }

            ch = (py / 8) - 1;

            if (ch < 0) return;

            if (ch < 14) {
                if (ev.getButton() == MouseEvent.BUTTON1) {
                    // Mask.
                    if (newParam.channels[ch].mask)
                        parent.resetChannelMask(Ym2608Chip.class, chipId, ch);
                    else
                        parent.setChannelMask(Ym2608Chip.class, chipId, ch);
                    return;
                }

                for (ch = 0; ch < 14; ch++) parent.resetChannelMask(Ym2608Chip.class, chipId, ch);
                return;
            }

            // Judgment in the tone display field

            int h = (py - 15 * 8) / (6 * 8);
            int w = Math.min(px / (13 * 8), 2);
            int instCh = h * 3 + w;

            if (instCh < 6) {
                // Copying a tone to the clipboard
                parent.getInstCh(Ym2608Chip.class, instCh, chipId);
            }
        }
    };

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeD");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(360, 216));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmYM2608
        //
        this.setPreferredSize(new Dimension(360, 216));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmYM2608");
        this.setTitle("OPNA");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static void drawScreenInitYM2608(FrameBuffer screen, int tp) {
        // YM2608
        for (int y = 0; y < 6 + 3 + 3 + 1; y++) {

            screen.drawFont8(296, y * 8 + 8, 1, "   ");
            for (int i = 0; i < 96; i++) {
                int kx = Tables.kbl[(i % 12) * 2] + i / 12 * 28;
                int kt = Tables.kbl[(i % 12) * 2 + 1];
                screen.drawKbn(33 + kx, y * 8 + 8, kt, tp);
            }

            if (y < 13) {
                drawChYM2608_P(screen, 1, y * 8 + 8, y, false, tp);
            }

            if (y < 6 || y == 12) {
                screen.drawPanP(25, y * 8 + 8, 3, tp);
            }

            int d = 99;
            if (y > 5 && y < 9) {
                d = screen.drawVolumeM(289, 8 + y * 8, 0, d, 0, tp);
            } else {
                d = screen.drawVolumeM(289, 8 + y * 8, 1, d, 0, tp);
                d = 99;
                d = screen.drawVolumeM(289, 8 + y * 8, 2, d, 0, tp);
            }
        }

        for (int y = 0; y < 6; y++) {
            int d = 99;
            int[] r = drawPanYM2608Rhythm(screen, y, d, 3, d, tp);
            d = r[0]; d = r[1];
            d = 99;
            d = drawVolumeYM2608Rhythm(screen, y, 1, d, 0, tp);
            d = 99;
            d = drawVolumeYM2608Rhythm(screen, y, 2, d, 0, tp);
        }
    }

    private static void drawInstOPNA(FrameBuffer screen, int x, int y, int c, int[] oi, int[] ni) {
        int sx = (c % 3) * 4 * 25 + x;
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

    private static int drawVolumeYM2608Rhythm(FrameBuffer screen, int x, int c, int ov, int nv, int tp) {
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
        x = x * 4 * 15 + 20;

        for (int i = 0; i <= 19; i++) {
            screen.drawVolumeP(x + i * 2, sy + 8 * 14, (1 + t), tp);
        }

        for (int i = 0; i <= nv; i++) {
            screen.drawVolumeP(x + i * 2, sy + 8 * 14, i > 17 ? (2 + t) : (0 + t), tp);
        }

        ov = nv;
        return ov;
    }

    private static int drawKeyBoardOPNA(FrameBuffer screen, int x, int y, int ot, int nt, int tp) {
        if (ot == nt)
            return ot;

        int kx;
        int kt;

        if (ot >= 0 && ot < 12 * 8) {
            kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
            kt = Tables.kbl[(ot % 12) * 2 + 1];
            screen.drawKbn(x + kx, y, kt, tp);
        }

        if (nt >= 0 && nt < 12 * 8) {
            kx = Tables.kbl[(nt % 12) * 2] + nt / 12 * 28;
            kt = Tables.kbl[(nt % 12) * 2 + 1] + 4;
            screen.drawKbn(x + kx, y, kt, tp);
        }

        screen.drawFont8(296 + x, y, 1, "   ");

        if (nt >= 0) {
            screen.drawFont8(296 + x, y, 1, Tables.kbn[nt % 12]);
            if (nt / 12 < 10) {
                screen.drawFont8(312 + x, y, 1, Tables.kbo[nt / 12]);
            }
        }

        ot = nt;
        return ot;
    }

    private static int[] drawPanYM2608Rhythm(FrameBuffer screen, int c, int ot, int nt, int otp, int ntp) {
        if (ot == nt && otp == ntp) {
            return new int[] {ot, otp};
        }

        screen.drawPanP(c * 4 * 15 + 12, 8 * 14, nt, ntp);
        ot = nt;
        otp = ntp;
        return new int[] {ot, otp};
    }

    private static Boolean drawChYM2608(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChYM2608_P(screen, 1, 8 + ch * 8, ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static Boolean[] drawCh3YM2608(FrameBuffer screen, int ch, Boolean om, Boolean nm, boolean oe, boolean ne, int tp) {
        if (om == nm && oe == ne) {
            return new Boolean[] {om, oe};
        }

        drawCh3YM2612_P(screen, 1, 8 + ch * 8, ch, nm == null ? false : nm, ne, tp);
        om = nm;
        oe = ne;
        return new Boolean[] {om, oe};
    }

    private static Boolean drawChYM2608Rhythm(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChYM2608Rhythm_P(screen, 0, 8 * 14, ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static int[] drawTnOPNA(FrameBuffer screen, int x, int y, int c, int ot, int nt, int otp, int ntp) {
        if (ot == nt && otp == ntp) {
            return new int[] {ot, otp};
        }

        screen.drawTnP(x * 4 + 1, y * 4 + c * 8, nt, ntp);
        ot = nt;
        otp = ntp;
        return new int[] {ot, otp};
    }

    private static void drawChYM2608_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        if (ch < 6) {
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 0, 16, 8);
            screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(1 + ch));
        } else if (ch < 9) {
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 32, 0, 16, 8);
            screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(1 + ch - 6));
        } else if (ch < 12) {
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 32 * (ch - 8), 24, 32, 8);
        } else {
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 64, 0, 24, 8);
        }
    }

    private static void drawChYM2608Rhythm_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawFont4(x + 0 * 4, y, mask ? 1 : 0, "B");
        screen.drawFont4(x + 15 * 4, y, mask ? 1 : 0, "s");
        screen.drawFont4(x + 30 * 4, y, mask ? 1 : 0, "C");
        screen.drawFont4(x + 45 * 4, y, mask ? 1 : 0, "H");
        screen.drawFont4(x + 60 * 4, y, mask ? 1 : 0, "T");
        screen.drawFont4(x + 75 * 4, y, mask ? 1 : 0, "R");
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        boolean ex = false;
        int tn = 0;
        int tntp = -1;
        int volumeRL = -1;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        boolean lfoSw = false;
        int lfoFrq = -1;
        int nfrq = -1;
        int efrq = -1;
        int etype = -1;
        int timerA = -1;
        int timerB = -1;
        int rhythmTotalLevel = -1;
        int adpcmLevel = -1;

        final Channel[] channels = {
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), // FM 0
                new Channel(), new Channel(), new Channel(), // SSG 9
                new Channel(), // ADPCM 12
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel() // RHYTHM 13
        };
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "YM2608"; }
        @Override public String menuText() { return "OPNA"; }
        @Override public String category() { return "opn"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return Ym2608Chip.class; }
        @Override public boolean hasRegisterDump() { return true; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormYM2608(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 14) {
                audio.plugin.chipRegister.chip(Ym2608Chip.class).setMask(chipId, ch);
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 14) {
                audio.plugin.chipRegister.chip(Ym2608Chip.class).setMask(chipId, ch, false, audio.plugin.stopped);
            }
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            // reapplying the YM2608 mask was disabled in the original and stays so
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(6, MDSound.Chip.MAIN_TAG, Ym2608Chip.class, "ym2608", 200),
                    new MixerSlot(7, "FM", Ym2608Chip.class, "ym2608FM", 200),
                    new MixerSlot(8, "PSG", Ym2608Chip.class, "ym2608SSG", 120),
                    new MixerSlot(9, "Rhythm", Ym2608Chip.class, "ym2608Rtm", 200),
                    new MixerSlot(10, "Adpcm", Ym2608Chip.class, "ym2608APCM", 200));
        }

        @Override public void updateMeters(Audio audio, VisVolume visVolume) {
            int fm = Meters.chipVolume(audio, Ym2608Chip.class) * 5;
            int ssg = 0;
            try {
                int[][] reg2d = (int[][]) Meters.chipInfo(audio, Ym2608Chip.class, "register");
                if (reg2d != null && reg2d.length > 0) {
                    int mixer = reg2d[0][0x07];
                    for (int ch = 0; ch < 3; ch++) {
                        boolean toneOn = (mixer & (0x01 << ch)) == 0;
                        boolean noiseOn = (mixer & (0x08 << ch)) == 0;
                        if (toneOn || noiseOn) {
                            int v = (reg2d[0][0x08 + ch] & 0xf) * 600;
                            if (v > ssg) ssg = v;
                        }
                    }
                }
            } catch (Exception _) {
            }
            int apcm = 0;
            try {
                apcm = Meters.maxVolume(Meters.chipInfo(audio, Ym2608Chip.class, "adpcmVolume")) * 5;
            } catch (Exception _) {
            }
            int rtm = 0;
            try {
                rtm = Meters.maxVolume(Meters.chipInfo(audio, Ym2608Chip.class, "rhythmVolume")) * 5;
            } catch (Exception _) {
            }
            visVolume.put("ym2608FM", fm);
            visVolume.put("ym2608SSG", ssg);
            visVolume.put("ym2608APCM", apcm);
            visVolume.put("ym2608Rtm", rtm);
            visVolume.put("ym2608", Math.max(Math.max(fm, ssg), Math.max(apcm, rtm)));
        }
    }
}
