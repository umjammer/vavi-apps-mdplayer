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
import mdplayer.chips.Ym2610Chip;
import mdplayer.form.VisVolume;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.Meters;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;
import mdsound.instrument.Ym2610Inst;

import static mdplayer.form.kb.chip.FormYM2612.drawCh3YM2612_P;
import mdplayer.form.View;


public class FormYM2610 extends FormChipBase<FormYM2610.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormYM2610.class);

    public FormYM2610(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());
        initializeComponent();

        bind(Common.getImage("planeYM2610"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("YM2610", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeYM2610").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2610").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeYM2610").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2610").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeYM2610").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2610").getHeight() * zoom));
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

    private final MouseListener pbScreen_MouseClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
            int px = ev.getX() / zoom;
            int py = ev.getY() / zoom;
            int ch;
            int c;

            // For top label row, do nothing
            if (py < 1 * 8) {
                // However, if you click on ch, the mask will be inverted.
                if (px < 8) {
                    for (ch = 0; ch < 14; ch++) {
                        if (ch >= 9 && ch <= 11) continue;

                        c = ch;
                        if (ch == 12) c = 13;
                        if (ch == 13) c = 12;

                        if (newParam.channels[c].mask)
                            parent.resetChannelMask(Ym2610Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(Ym2610Chip.class, chipId, ch);
                    }
                }
                return;
            }

            ch = (py / 8) - 1;
            c = ch;
            if (ch == 12) c = 13;
            if (ch == 13) c = 12;

            if (ch < 0) return;

            if (ch < 14) {
                if (ev.getButton() == MouseEvent.BUTTON1) {
                    // Mask.
                    if (newParam.channels[c].mask)
                        parent.resetChannelMask(Ym2610Chip.class, chipId, ch);
                    else
                        parent.setChannelMask(Ym2610Chip.class, chipId, ch);
                    return;
                }

                for (ch = 0; ch < 14; ch++) parent.resetChannelMask(Ym2610Chip.class, chipId, ch);
                return;
            }

            // Tone display column judgment

            int h = (py - 15 * 8) / (6 * 8);
            int w = Math.min(px / (13 * 8), 2);
            int instCh = h * 3 + w;

            if (instCh < 6) {
                // Copying a tone to the clipboard
                parent.getInstCh(Ym2610Chip.class, instCh, chipId);
            }
        }
    };

    @Override
    public void initScreen() {
        int tp = ((chipId == 0)
                ? (parent.setting.getYM2610Type()[0].getUseReal()[0] || (parent.setting.getYM2610Type()[0].getUseReal().length > 1 && parent.setting.getYM2610Type()[0].getUseReal()[1]))
                : (parent.setting.getYM2610Type()[1].getUseReal()[0] || (parent.setting.getYM2610Type()[1].getUseReal().length > 1 && parent.setting.getYM2610Type()[1].getUseReal()[1]))
        )
                ? 1
                : 0;

        for (int y = 0; y < 14; y++) {
            frameBuffer.drawFont8(296, y * 8 + 8, 1, "   ");
            for (int i = 0; i < 96; i++) {
                int kx = Tables.kbl[(i % 12) * 2] + i / 12 * 28;
                int kt = Tables.kbl[(i % 12) * 2 + 1];
                frameBuffer.drawKbn(32 + kx, y * 8 + 8, kt, tp);
            }

            if (y < 13) {
                drawChYM2610_P(frameBuffer, 0, y * 8 + 8, y, false, tp);
            }

            if (y < 6 || y == 13) {
                frameBuffer.drawPanP(24, y * 8 + 8, 3, tp);
            }

            int d = 99;
            if (y > 5 && y < 9) {
                d = frameBuffer.drawVolumeM(256, 8 + y * 8, 0, d, 0, tp);
            } else {
                d = frameBuffer.drawVolumeM(256, 8 + y * 8, 1, d, 0, tp);
                d = 99;
                d = frameBuffer.drawVolumeM(256, 8 + y * 8, 2, d, 0, tp);
            }
        }

        for (int y = 0; y < 6; y++) {
            int d = 99;
            int[] r = drawPanYM2610Rhythm(frameBuffer, y, d, 3, d, tp);
            d = r[0]; d = r[1];
            d = 99;
            d = drawVolumeYM2610Rhythm(frameBuffer, y, 1, d, 0, tp);
            d = 99;
            d = drawVolumeYM2610Rhythm(frameBuffer, y, 2, d, 0, tp);
        }
        boolean f = true;
        f = drawChYM2610Rhythm(frameBuffer, 0, f, false, tp);
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
        Map<String, Object> info = audio.plugin.chipRegister.chip(Ym2610Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int[][] register = (int[][]) info.get("register");
        int[] fmKey = (int[]) info.get("keyOn");
        int[] vol = (int[]) info.get("volume");
        int[] ch3SlotVol = (int[]) info.get("ch3SlotVolume");
        int[][] rhythm = (int[][]) info.get("rhythmVolume");
        int[] adpcmVol = (int[]) info.get("adpcmVolume");

        boolean isFmEx = (register[chipId][0x27] & 0x40) > 0;
        newParam.channels[2].ex = isFmEx;

        int defaultMasterClock = 8000000;
        float ssgMul = 1.0f;
        int masterClock = defaultMasterClock;
        int clock = clock(Ym2610Inst.class);
        if (clock != 0) {
            ssgMul = clock / (float) defaultMasterClock;
            masterClock = clock;
        }

        int divInd = register[0][0x2d];
        if (divInd < 0 || divInd > 2) divInd = 0;
        float fmDiv = fmDivTbl[divInd];
        float ssgDiv = ssgDivTbl[divInd];
        ssgMul = ssgMul * ssgDiv / 4;

        //int masterClock = clock;
        //int defaultMasterClock = 8000000;
        //float mul = 1.0f;
        //if (masterClock != 0)
        //    mul = masterClock / (float)defaultMasterClock;

        newParam.lfoSw = (register[0][0x22] & 0x8) != 0;
        newParam.lfoFrq = (register[0][0x22] & 0x7);

        for (int ch = 0; ch < 6; ch++) {
            int p = (ch > 2) ? 1 : 0;
            int c = (ch > 2) ? ch - 3 : ch;
            for (int i = 0; i < 4; i++) {
                int ops = (i == 0) ? 0 : ((i == 1) ? 8 : ((i == 2) ? 4 : 12));
                newParam.channels[ch].inst[i * 11 + 0] = register[p][0x50 + ops + c] & 0x1f; // AR
                newParam.channels[ch].inst[i * 11 + 1] = register[p][0x60 + ops + c] & 0x1f; // DR
                newParam.channels[ch].inst[i * 11 + 2] = register[p][0x70 + ops + c] & 0x1f; // SR
                newParam.channels[ch].inst[i * 11 + 3] = register[p][0x80 + ops + c] & 0x0f; // RR
                newParam.channels[ch].inst[i * 11 + 4] = (register[p][0x80 + ops + c] & 0xf0) >> 4; // SL
                newParam.channels[ch].inst[i * 11 + 5] = register[p][0x40 + ops + c] & 0x7f; // TL
                newParam.channels[ch].inst[i * 11 + 6] = (register[p][0x50 + ops + c] & 0xc0) >> 6; // KS
                newParam.channels[ch].inst[i * 11 + 7] = register[p][0x30 + ops + c] & 0x0f; // ML
                newParam.channels[ch].inst[i * 11 + 8] = (register[p][0x30 + ops + c] & 0x70) >> 4; // DT
                newParam.channels[ch].inst[i * 11 + 9] = (register[p][0x60 + ops + c] & 0x80) >> 7; // AM
                newParam.channels[ch].inst[i * 11 + 10] = register[p][0x90 + ops + c] & 0x0f; // SG
            }
            newParam.channels[ch].inst[44] = register[p][0xb0 + c] & 0x07; // AL
            newParam.channels[ch].inst[45] = (register[p][0xb0 + c] & 0x38) >> 3; // FB
            newParam.channels[ch].inst[46] = (register[p][0xb4 + c] & 0x38) >> 4; // AMS
            newParam.channels[ch].inst[47] = register[p][0xb4 + c] & 0x07; // FMS

            newParam.channels[ch].pan = (register[p][0xb4 + c] & 0xc0) >> 6;

            int freq = 0;
            int octav = 0;
            int n = -1;
            if (ch != 2 || !isFmEx) {
                freq = register[p][0xa0 + c] + (register[p][0xa4 + c] & 0x07) * 0x100;
                octav = (register[p][0xa4 + c] & 0x38) >> 3;
                float ff = freq / ((2 << 20) / (masterClock / (24 * fmDiv))) * (2 << (octav + 2));
                ff /= 1038f;

                if ((fmKey[ch] & 1) != 0)
                    n = Math.clamp(Ym2608Chip.searchYM2608Adpcm(ff) - 1, 0, 95);

                byte con = (byte) (fmKey[ch]);
                int v = 127;
                int m = md[register[p][0xb0 + c] & 7];
                // OP1
                v = (((con & 0x10) != 0) && ((m & 0x10) != 0) && v > (register[p][0x40 + c] & 0x7f)) ? (register[p][0x40 + c] & 0x7f) : v;
                // OP3
                v = (((con & 0x20) != 0) && ((m & 0x20) != 0) && v > (register[p][0x44 + c] & 0x7f)) ? (register[p][0x44 + c] & 0x7f) : v;
                // OP2
                v = (((con & 0x40) != 0) && ((m & 0x40) != 0) && v > (register[p][0x48 + c] & 0x7f)) ? (register[p][0x48 + c] & 0x7f) : v;
                // OP4
                v = (((con & 0x80) != 0) && ((m & 0x80) != 0) && v > (register[p][0x4c + c] & 0x7f)) ? (register[p][0x4c + c] & 0x7f) : v;
                newParam.channels[ch].volumeL = Math.clamp((int) ((127 - v) / 127.0 * ((register[p][0xb4 + c] & 0x80) != 0 ? 1 : 0) * vol[ch] / 80.0), 0, 19);
                newParam.channels[ch].volumeR = Math.clamp((int) ((127 - v) / 127.0 * ((register[p][0xb4 + c] & 0x40) != 0 ? 1 : 0) * vol[ch] / 80.0), 0, 19);
            } else {
                int m = md[register[0][0xb0 + 2] & 7];
                if (parent.setting.getOther().getExAll()) m = 0xf0;
                freq = register[0][0xa9] + (register[0][0xad] & 0x07) * 0x100;
                octav = (register[0][0xad] & 0x38) >> 3;
                float ff = freq / ((2 << 20) / (masterClock / (24 * fmDiv))) * (2 << (octav + 2));
                ff /= 1038f;

                if ((fmKey[2] & 0x10) != 0 && ((m & 0x10) != 0))
                    n = Math.clamp(Ym2608Chip.searchYM2608Adpcm(ff) - 1, 0, 95);

                int v = ((m & 0x10) != 0) ? register[p][0x40 + c] : 127;
                newParam.channels[2].volumeL = Math.clamp((int) ((127 - v) / 127.0 * ((register[0][0xb4 + 2] & 0x80) != 0 ? 1 : 0) * ch3SlotVol[0] / 80.0), 0, 19);
                newParam.channels[2].volumeR = Math.clamp((int) ((127 - v) / 127.0 * ((register[0][0xb4 + 2] & 0x40) != 0 ? 1 : 0) * ch3SlotVol[0] / 80.0), 0, 19);
            }
            newParam.channels[ch].note = n;
        }

        for (int ch = 6; ch < 9; ch++) { // FM EX
            int[] exReg = {2, 0, -6};
            int c = exReg[ch - 6];

            newParam.channels[ch].pan = 0;

            if (isFmEx) {
                int m = md[register[0][0xb0 + 2] & 7];
                if (parent.setting.getOther().getExAll()) m = 0xf0;
                int op = ch - 5;
                op = op == 1 ? 2 : (op == 2 ? 1 : op);

                int freq = register[0][0xa8 + c] + (register[0][0xac + c] & 0x07) * 0x100;
                int octav = (register[0][0xac + c] & 0x38) >> 3;
                int n = -1;
                if ((fmKey[2] & (0x10 << (ch - 5))) != 0 && ((m & (0x10 << op)) != 0)) {
                    float ff = freq / ((2 << 20) / (masterClock / (24 * fmDiv))) * (2 << (octav + 2));
                    ff /= 1038f;
                    n = Math.clamp(Ym2608Chip.searchYM2608Adpcm(ff) - 1, 0, 95);
                }
                newParam.channels[ch].note = n;

                int v = ((m & (0x10 << op)) != 0) ? register[0][0x42 + op * 4] : 127;
                newParam.channels[ch].volumeL = Math.clamp((int) ((127 - v) / 127.0 * ch3SlotVol[ch - 5] / 80.0), 0, 19);
            } else {
                newParam.channels[ch].note = -1;
                newParam.channels[ch].volumeL = 0;
            }
        }

        for (int ch = 0; ch < 3; ch++) { // SSG
            Channel channel = newParam.channels[ch + 9];

            boolean t = (register[0][0x07] & (0x1 << ch)) == 0;
            boolean n = (register[0][0x07] & (0x8 << ch)) == 0;
            channel.tn = (t ? 1 : 0) + (n ? 2 : 0);

            channel.volume = (int) (((t || n) ? 1 : 0) * (register[0][0x08 + ch] & 0xf) * (20.0 / 16.0));
            if (!t && !n && channel.volume > 0) {
                channel.volume--;
            }

            if (channel.volume == 0) {
                channel.note = -1;
            } else {
                int ft = register[0][0x00 + ch * 2];
                int ct = register[0][0x01 + ch * 2];
                int tp = (ct << 8) | ft;
                if (tp == 0) {
                    channel.note = -1;
                    //channel.volume = 0;
                } else {
                    float ftone = masterClock / (64.0f * (float) tp) * ssgMul;// 7987200 = MasterClock
                    channel.note = Common.searchSSGNote(ftone);
                }
            }
        }

        newParam.nfrq = register[0][0x06] & 0x1f;
        newParam.efrq = register[0][0x0c] * 0x100 + register[0][0x0b];
        newParam.etype = (register[0][0x0d] & 0xf);

        // ADPCM B
        newParam.channels[12].pan = (register[0][0x11] & 0xc0) >> 6;
        if (adpcmVol[0] != 0) {
            newParam.channels[12].volumeL = Math.clamp((long) adpcmVol[0] * register[0][0x1b], 0, 19);
        } else {
            if (newParam.channels[12].volumeL > 0) newParam.channels[12].volumeL--;
        }
        if (adpcmVol[1] != 0) {
            newParam.channels[12].volumeR = Math.clamp((long) adpcmVol[1] * register[0][0x1b], 0, 19);
        } else {
            if (newParam.channels[12].volumeR > 0) newParam.channels[12].volumeR--;
        }
        int delta = (register[0][0x1a] << 8) | register[0][0x19];
        float frq = delta / 9447.0f; // Delta=9447 at freq=8kHz
        newParam.channels[12].note = (register[0][0x10] & 0x80) != 0 ? Ym2608Chip.searchYM2608Adpcm(frq) : -1;
        if ((register[0][0x11] & 0xc0) == 0) {
            newParam.channels[12].note = -1;
        }

        int tl = register[1][0x01] & 0x3f;
        for (int ch = 13; ch < 19; ch++) { // ADPCM a
            newParam.channels[ch].pan = (register[1][0x08 + ch - 13] & 0xc0) >> 6;
            //newParam.channels[ch].volumeL = Math.min(Math.max(rhythm[ch - 13][0] / 80, 0), 19);
            //newParam.channels[ch].volumeR = Math.min(Math.max(rhythm[ch - 13][1] / 80, 0), 19);
            int il = register[1][0x08 + ch - 13] & 0x1f;

            if (rhythm[ch - 13][0] != 0) {
                newParam.channels[ch].volumeL = Math.clamp((long) rhythm[ch - 13][0] * tl * il / 128, 0, 19);
                //newParam.channels[12].volumeR = Math.min(Math.max(adpcmVol[1] * register[0][0x1b], 0), 19);
            } else {
                if (newParam.channels[ch].volumeL > 0) newParam.channels[ch].volumeL--;
            }
            if (rhythm[ch - 13][1] != 0) {
                newParam.channels[ch].volumeR = Math.clamp((long) rhythm[ch - 13][1] * tl * il / 128, 0, 19);
                //newParam.channels[12].volumeR = Math.min(Math.max(adpcmVol[1] * register[0][0x1b], 0), 19);
            } else {
                if (newParam.channels[ch].volumeR > 0) newParam.channels[ch].volumeR--;
            }
        }
    
        // the chip itself is the source of truth for channel muting; display rows 12/13 are swapped
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(Ym2610Chip.class).getMask(chipId, mch == 12 ? 13 : (mch == 13 ? 12 : mch));
    }

    @Override
    public void drawScreenParams() {
        int tp = ((chipId == 0)
                ? (parent.setting.getYM2610Type()[0].getUseReal()[0] || (parent.setting.getYM2610Type()[0].getUseReal().length > 1 && parent.setting.getYM2610Type()[0].getUseReal()[1]))
                : (parent.setting.getYM2610Type()[1].getUseReal()[0] || (parent.setting.getYM2610Type()[1].getUseReal().length > 1 && parent.setting.getYM2610Type()[1].getUseReal()[1]))
        )
                ? 1
                : 0;

        // FM - SSG
        for (int c = 0; c < 9; c++) {

            Channel oyc = oldParam.channels[c];
            Channel nyc = newParam.channels[c];

            if (c == 2) {
                oyc.volumeL = frameBuffer.drawVolumeM(256, 8 + c * 8, 1, oyc.volumeL, nyc.volumeL, tp);
                oyc.volumeR = frameBuffer.drawVolumeM(256, 8 + c * 8, 2, oyc.volumeR, nyc.volumeR, tp);
                { int[] r = frameBuffer.pan(24, 8 + c * 8, oyc.pan, nyc.pan, oyc.pantp, tp); oyc.pan = r[0]; oyc.pantp = r[1]; }
                oyc.note = frameBuffer.drawKeyBoard(c, oyc.note, nyc.note, tp);
                frameBuffer.drawInst(1, 17, c, oyc.inst, nyc.inst);
                Boolean[] r = drawCh3YM2610(frameBuffer, c, oyc.mask, nyc.mask, oyc.ex, nyc.ex, tp);
                oyc.mask = r[0]; oyc.ex = r[1];
            } else if (c < 6) {
                oyc.volumeL = frameBuffer.drawVolumeM(256, 8 + c * 8, 1, oyc.volumeL, nyc.volumeL, tp);
                oyc.volumeR = frameBuffer.drawVolumeM(256, 8 + c * 8, 2, oyc.volumeR, nyc.volumeR, tp);
                { int[] r = frameBuffer.pan(24, 8 + c * 8, oyc.pan, nyc.pan, oyc.pantp, tp); oyc.pan = r[0]; oyc.pantp = r[1]; }
                oyc.note = frameBuffer.drawKeyBoard(c, oyc.note, nyc.note, tp);
                frameBuffer.drawInst(1, 17, c, oyc.inst, nyc.inst);
                oyc.mask = drawChYM2610(frameBuffer, c, oyc.mask, nyc.mask, tp);
            } else {
                oyc.volumeL = frameBuffer.drawVolumeM(256, 8 + (c + 3) * 8, 0, oyc.volumeL, nyc.volumeL, tp);
                oyc.note = frameBuffer.drawKeyBoard(c + 3, oyc.note, nyc.note, tp);
                oyc.mask = drawChYM2610(frameBuffer, c, oyc.mask, nyc.mask, tp);
            }
        }

        // FMex
        for (int c = 0; c < 3; c++) {
            Channel oyc = oldParam.channels[c + 9];
            Channel nyc = newParam.channels[c + 9];

            oyc.volume = frameBuffer.drawVolumeM(256, 8 + (c + 6) * 8, 0, oyc.volume, nyc.volume, tp);
            oyc.note = frameBuffer.drawKeyBoard(c + 6, oyc.note, nyc.note, tp);
            int[] r = drawTn(frameBuffer, 6, 2, c + 6, oyc.tn, nyc.tn, oyc.tntp, tp);
            oyc.tn = r[0]; oyc.tntp = r[1];

            oyc.mask = drawChYM2610(frameBuffer, c + 9, oyc.mask, nyc.mask, tp);
        }

        // ADPCM B
        oldParam.channels[12].volumeL = frameBuffer.drawVolumeM(256, 8 + 13 * 8, 1, oldParam.channels[12].volumeL, newParam.channels[12].volumeL, tp);
        oldParam.channels[12].volumeR = frameBuffer.drawVolumeM(256, 8 + 13 * 8, 2, oldParam.channels[12].volumeR, newParam.channels[12].volumeR, tp);
        { int[] r = frameBuffer.pan(24, 8 + 13 * 8, oldParam.channels[12].pan, newParam.channels[12].pan, oldParam.channels[12].pantp, tp); oldParam.channels[12].pan = r[0]; oldParam.channels[12].pantp = r[1]; }
        oldParam.channels[12].note = frameBuffer.drawKeyBoard(13, oldParam.channels[12].note, newParam.channels[12].note, tp);
        oldParam.channels[12].mask = drawChYM2610(frameBuffer, 13, oldParam.channels[12].mask, newParam.channels[12].mask, tp);

        // ADPCM a (Rhythm)
        for (int c = 0; c < 6; c++) {
            Channel oyc = oldParam.channels[c + 13];
            Channel nyc = newParam.channels[c + 13];

            oyc.volumeL = drawVolumeYM2610Rhythm(frameBuffer, c, 1, oyc.volumeL, nyc.volumeL, tp);
            oyc.volumeR = drawVolumeYM2610Rhythm(frameBuffer, c, 2, oyc.volumeR, nyc.volumeR, tp);
            int[] r = drawPanYM2610Rhythm(frameBuffer, c, oyc.pan, nyc.pan, oyc.pantp, tp);
            oyc.pan = r[0]; oyc.pantp = r[1];
        }
        oldParam.channels[13].mask = drawChYM2610Rhythm(frameBuffer, 0, oldParam.channels[13].mask, newParam.channels[13].mask, tp);

        // HardLFO NOISE ENV
        oldParam.lfoSw = frameBuffer.drawLfoSw(16, 216, oldParam.lfoSw, newParam.lfoSw);
        oldParam.lfoFrq = frameBuffer.drawLfoFrq(64, 216, oldParam.lfoFrq, newParam.lfoFrq);

        oldParam.nfrq = frameBuffer.Nfrq(25, 54, oldParam.nfrq, newParam.nfrq);
        oldParam.efrq = frameBuffer.drawEfrq(38, 54, oldParam.efrq, newParam.efrq);
        oldParam.etype = frameBuffer.drawEType(53, 54, oldParam.etype, newParam.etype);
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeYM2610");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 224));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmYM2610
        //
        this.setPreferredSize(new Dimension(320, 224));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmYM2610");
        this.setTitle("YM2610");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static int drawVolumeYM2610Rhythm(FrameBuffer screen, int x, int c, int ov, int nv, int tp) {
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
        x = x * 4 * 13 + 8 * 2;

        for (int i = 0; i <= 19; i++) {
            screen.drawVolumeP(x + i * 2, sy + 8 * 13, (1 + t), tp);
        }

        for (int i = 0; i <= nv; i++) {
            screen.drawVolumeP(x + i * 2, sy + 8 * 13, i > 17 ? (2 + t) : (0 + t), tp);
        }

        ov = nv;
        return ov;
    }

    private static int[] drawPanYM2610Rhythm(FrameBuffer screen, int c, int ot, int nt, int otp, int ntp) {
        if (ot == nt && otp == ntp) {
            return new int[] {ot, otp};
        }

        screen.drawPanP(c * 4 * 13 + 8, 8 * 13, nt, ntp);
        ot = nt;
        otp = ntp;
        return new int[] {ot, otp};
    }

    private static Boolean drawChYM2610(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChYM2610_P(screen, 0, 8 + ch * 8, ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static Boolean[] drawCh3YM2610(FrameBuffer screen, int ch, Boolean om, Boolean nm, boolean oe, boolean ne, int tp) {
        if (om == nm && oe == ne) {
            return new Boolean[] {om, oe};
        }

        drawCh3YM2612_P(screen, 0, 8 + ch * 8, ch, nm == null ? false : nm, ne, tp);
        om = nm;
        oe = ne;
        return new Boolean[] {om, oe};
    }

    private static Boolean drawChYM2610Rhythm(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChYM2610Rhythm_P(screen, 0, 8 * 13, ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static int[] drawTn(FrameBuffer screen, int x, int y, int c, int ot, int nt, int otp, int ntp) {
        if (ot == nt && otp == ntp) {
            return new int[] {ot, otp};
        }

        screen.drawTnP(x * 4, y * 4 + c * 8, nt, ntp);
        ot = nt;
        otp = ntp;
        return new int[] {ot, otp};
    }

    private static void drawChYM2610_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
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
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 88, 0, 24, 8);
        }
    }

    private static void drawChYM2610Rhythm_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawFont4(x + 0 * 4, y, mask ? 1 : 0, "A1");
        screen.drawFont4(x + 14 * 4, y, mask ? 1 : 0, "2");
        screen.drawFont4(x + 27 * 4, y, mask ? 1 : 0, "3");
        screen.drawFont4(x + 40 * 4, y, mask ? 1 : 0, "4");
        screen.drawFont4(x + 53 * 4, y, mask ? 1 : 0, "5");
        screen.drawFont4(x + 66 * 4, y, mask ? 1 : 0, "6");
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        boolean ex = false;
        int tn = 0;
        int tntp = -1;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        boolean lfoSw = false;
        int lfoFrq = -1;
        int nfrq = -1;
        int efrq = -1;
        int etype = -1;

        final Channel[] channels = {
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), // FM 0
                new Channel(), new Channel(), new Channel(), // SSG 9
                new Channel(), // ADPCM 12
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel() // RHYTHM 13
        };
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "YM2610"; }
        @Override public String menuText() { return "OPNB"; }
        @Override public String category() { return "opn"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return Ym2610Chip.class; }
        @Override public boolean hasRegisterDump() { return true; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormYM2610(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 14) {
                audio.plugin.chipRegister.chip(Ym2610Chip.class).setMask(chipId, ch);
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 14) {
                audio.plugin.chipRegister.chip(Ym2610Chip.class).resetMask(chipId, ch);
            }
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (ch >= 0 && ch < 14) {
                if (mask)
                    audio.plugin.chipRegister.chip(Ym2610Chip.class).setMask(chipId, ch);
                else
                    audio.plugin.chipRegister.chip(Ym2610Chip.class).resetMask(chipId, ch);
            }
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 14; ch++)
                forceChannelMask(audio, Ym2610Chip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(Ym2610Chip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(11, MDSound.Chip.MAIN_TAG, Ym2610Chip.class, "ym2610", 200),
                    new MixerSlot(12, "FM", Ym2610Chip.class, "ym2610FM", 200),
                    new MixerSlot(13, "PSG", Ym2610Chip.class, "ym2610SSG", 120),
                    new MixerSlot(14, "AdpcmA", Ym2610Chip.class, "ym2610APCMA", 200),
                    new MixerSlot(15, "AdpcmB", Ym2610Chip.class, "ym2610APCMB", 200));
        }

        @Override public void updateMeters(Audio audio, VisVolume visVolume) {
            int fm = Meters.chipVolume(audio, Ym2610Chip.class) * 5;
            int ssg = 0;
            try {
                int[][] reg2d = (int[][]) Meters.chipInfo(audio, Ym2610Chip.class, "register");
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
            int apcmA = 0;
            try {
                apcmA = Meters.maxVolume(Meters.chipInfo(audio, Ym2610Chip.class, "adpcmAVolume")) * 5;
            } catch (Exception _) {
            }
            int apcmB = 0;
            try {
                apcmB = Meters.maxVolume(Meters.chipInfo(audio, Ym2610Chip.class, "adpcmBVolume")) * 5;
            } catch (Exception _) {
            }
            visVolume.put("ym2610FM", fm);
            visVolume.put("ym2610SSG", ssg);
            visVolume.put("ym2610APCMA", apcmA);
            visVolume.put("ym2610APCMB", apcmB);
            visVolume.put("ym2610", Math.max(Math.max(fm, ssg), Math.max(apcmA, apcmB)));
        }
    }
}
