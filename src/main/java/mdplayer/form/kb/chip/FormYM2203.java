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
import mdplayer.chips.Ym2203Chip;
import mdplayer.chips.Ym2608Chip;
import mdplayer.form.VisVolume;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.Meters;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;
import mdsound.instrument.YmFmYm2203Inst;

import static mdplayer.form.kb.chip.FormYM2612.drawCh3YM2612_P;
import mdplayer.form.View;


public class FormYM2203 extends FormChipBase<FormYM2203.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormYM2203.class);

    public FormYM2203(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());
        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeYM2203"), null, zoom);
        boolean YM2203Type = (chipId == 0)
                ? parent.setting.getYM2203Type()[0].getUseReal()[0]
                : parent.setting.getYM2203Type()[1].getUseReal()[0];
        int YM2203SoundLocation = (chipId == 0)
                ? parent.setting.getYM2203Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYM2203Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !YM2203Type ? 0 : (YM2203SoundLocation < 0 ? 2 : 1);
        drawScreenInitYM2203(frameBuffer, tp);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("YM2203", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeYM2203").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2203").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeYM2203").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2203").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeYM2203").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2203").getHeight() * zoom));
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

    private static final float[] fmDivTbl = {6, 3, 2};
    private static final float[] ssgDivTbl = {4, 2, 1};

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(Ym2203Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int[] ym2203Register = (int[]) info.get("register");
        int[] fmKeyYM2203 = (int[]) info.get("keyOn");
        int[] ym2203Vol = (int[]) info.get("volume");
        int[] ym2203Ch3SlotVol = (int[]) info.get("ch3SlotVolume");

        boolean isFmEx = (ym2203Register[0x27] & 0x40) > 0;
        newParam.channels[2].ex = isFmEx;

        int defaultMasterClock = 7987200 / 2;
        float ssgMul = 1.0f;
        int masterClock = defaultMasterClock;
        if (clock(YmFmYm2203Inst.class) != 0) {
            ssgMul = clock(YmFmYm2203Inst.class) / (float) defaultMasterClock;
            masterClock = clock(YmFmYm2203Inst.class);
        }

        int divInd = ym2203Register[0x2d];
        if (divInd < 0 || divInd > 2) divInd = 0;
        float fmDiv = fmDivTbl[divInd];
        float ssgDiv = ssgDivTbl[divInd];
        ssgMul = ssgMul * ssgDiv / 4;

        for (int ch = 0; ch < 3; ch++) {
            int c = ch;
            for (int i = 0; i < 4; i++) {
                int ops = (i == 0) ? 0 : ((i == 1) ? 8 : ((i == 2) ? 4 : 12));
                newParam.channels[ch].inst[i * 11 + 0] = ym2203Register[0x50 + ops + c] & 0x1f; // AR
                newParam.channels[ch].inst[i * 11 + 1] = ym2203Register[0x60 + ops + c] & 0x1f; // DR
                newParam.channels[ch].inst[i * 11 + 2] = ym2203Register[0x70 + ops + c] & 0x1f; // SR
                newParam.channels[ch].inst[i * 11 + 3] = ym2203Register[0x80 + ops + c] & 0x0f; // RR
                newParam.channels[ch].inst[i * 11 + 4] = (ym2203Register[0x80 + ops + c] & 0xf0) >> 4; // SL
                newParam.channels[ch].inst[i * 11 + 5] = ym2203Register[0x40 + ops + c] & 0x7f; // TL
                newParam.channels[ch].inst[i * 11 + 6] = (ym2203Register[0x50 + ops + c] & 0xc0) >> 6; // KS
                newParam.channels[ch].inst[i * 11 + 7] = ym2203Register[0x30 + ops + c] & 0x0f; // ML
                newParam.channels[ch].inst[i * 11 + 8] = (ym2203Register[0x30 + ops + c] & 0x70) >> 4; // DT
                newParam.channels[ch].inst[i * 11 + 9] = (ym2203Register[0x60 + ops + c] & 0x80) >> 7; // AM
                newParam.channels[ch].inst[i * 11 + 10] = ym2203Register[0x90 + ops + c] & 0x0f;//SG
            }
            newParam.channels[ch].inst[44] = ym2203Register[0xb0 + c] & 0x07; // AL
            newParam.channels[ch].inst[45] = (ym2203Register[0xb0 + c] & 0x38) >> 3; // FB
            newParam.channels[ch].inst[46] = (ym2203Register[0xb4 + c] & 0x38) >> 4; // AMS
            newParam.channels[ch].inst[47] = ym2203Register[0xb4 + c] & 0x07; // FMS

            newParam.channels[ch].pan = 3;

            int freq = 0;
            int octav = 0;
            int n = -1;
            if (ch != 2 || !isFmEx) {
                octav = (ym2203Register[0xa4 + c] & 0x38) >> 3;
                freq = ym2203Register[0xa0 + c] + (ym2203Register[0xa4 + c] & 0x07) * 0x100;
                float ff = freq / ((2 << 20) / (masterClock / (12 * fmDiv))) * (2 << (octav + 2));
                ff /= 1038f;

                if ((fmKeyYM2203[ch] & 1) != 0)
                    n = Math.clamp(Ym2608Chip.searchYM2608Adpcm(ff) - 1, 0, 95);

                byte con = (byte) (fmKeyYM2203[ch]);
                int v = 127;
                int m = md[ym2203Register[0xb0 + c] & 7];
                // OP1
                v = (((con & 0x10) != 0) && ((m & 0x10) != 0) && v > (ym2203Register[0x40 + c] & 0x7f)) ? (ym2203Register[0x40 + c] & 0x7f) : v;
                // OP3
                v = (((con & 0x20) != 0) && ((m & 0x20) != 0) && v > (ym2203Register[0x44 + c] & 0x7f)) ? (ym2203Register[0x44 + c] & 0x7f) : v;
                // OP2
                v = (((con & 0x40) != 0) && ((m & 0x40) != 0) && v > (ym2203Register[0x48 + c] & 0x7f)) ? (ym2203Register[0x48 + c] & 0x7f) : v;
                // OP4
                v = (((con & 0x80) != 0) && ((m & 0x80) != 0) && v > (ym2203Register[0x4c + c] & 0x7f)) ? (ym2203Register[0x4c + c] & 0x7f) : v;
                newParam.channels[ch].volumeL = Math.clamp((int) ((127 - v) / 127.0 * ym2203Vol[ch] / 80.0), 0, 19);
            } else {
                int m = md[ym2203Register[0xb0 + 2] & 7];
                if (parent.setting.getOther().getExAll()) m = 0xf0;
                freq = ym2203Register[0xa9] + (ym2203Register[0xad] & 0x07) * 0x100;
                octav = (ym2203Register[0xad] & 0x38) >> 3;
                float ff = freq / ((2 << 20) / (masterClock / (12 * fmDiv))) * (2 << (octav + 2));
                ff /= 1038f;

                if ((fmKeyYM2203[2] & 0x10) != 0 && ((m & 0x10) != 0))
                    n = Math.clamp(Ym2608Chip.searchYM2608Adpcm(ff) - 1, 0, 95);

                int v = ((m & 0x10) != 0) ? ym2203Register[0x40 + c] : 127;
                newParam.channels[2].volumeL = Math.clamp((int) ((127 - v) / 127.0 * ym2203Ch3SlotVol[0] / 80.0), 0, 19);
            }
            newParam.channels[ch].note = n;
        }

        for (int ch = 3; ch < 6; ch++) { // FM EX
            int[] exReg = {2, 0, -6};
            int c = exReg[ch - 3];

            newParam.channels[ch].pan = 0;

            if (isFmEx) {
                int m = md[ym2203Register[0xb0 + 2] & 7];
                if (parent.setting.getOther().getExAll()) m = 0xf0;
                int op = ch - 2;
                op = op == 1 ? 2 : (op == 2 ? 1 : op);

                int freq = ym2203Register[0xa8 + c] + (ym2203Register[0xac + c] & 0x07) * 0x100;
                int octav = (ym2203Register[0xac + c] & 0x38) >> 3;
                int n = -1;
                if ((fmKeyYM2203[2] & (0x20 << (ch - 3))) != 0 && ((m & (0x10 << op)) != 0)) {
                    float ff = freq / ((2 << 20) / (masterClock / (12 * fmDiv))) * (2 << (octav + 2));
                    ff /= 1038f;
                    n = Math.clamp(Ym2608Chip.searchYM2608Adpcm(ff) - 1, 0, 95);
                }
                newParam.channels[ch].note = n;

                int v = ((m & (0x10 << op)) != 0) ? ym2203Register[0x42 + op * 4] : 127;
                newParam.channels[ch].volumeL = Math.clamp((int) ((127 - v) / 127.0 * ym2203Ch3SlotVol[ch - 2] / 80.0), 0, 19);
            } else {
                newParam.channels[ch].note = -1;
                newParam.channels[ch].volumeL = 0;
            }
        }

        for (int ch = 0; ch < 3; ch++) { // SSG
            Channel channel = newParam.channels[ch + 6];

            boolean t = (ym2203Register[0x07] & (0x1 << ch)) == 0;
            boolean n = (ym2203Register[0x07] & (0x8 << ch)) == 0;
            channel.tn = (t ? 1 : 0) + (n ? 2 : 0);
            channel.volume = (int) (((t || n) ? 1 : 0) * (ym2203Register[0x08 + ch] & 0xf) * (20.0 / 16.0));
            if (!t && !n && channel.volume > 0) {
                channel.volume--;
            }

            if (channel.volume == 0) {
                channel.note = -1;
            } else {
                int ft = ym2203Register[0x00 + ch * 2];
                int ct = ym2203Register[0x01 + ch * 2];
                int tp = (ct << 8) | ft;
                if (tp == 0) tp = 1;
                float ftone = 7987200.0f / (64.0f * (float) tp) * ssgMul; // 7987200 = MasterClock (The method below uses a table based on 7987200)
                channel.note = Common.searchSSGNote(ftone);
            }
        }

        newParam.nfrq = ym2203Register[0x06] & 0x1f;
        newParam.efrq = ym2203Register[0x0c] * 0x100 + ym2203Register[0x0b];
        newParam.etype = (ym2203Register[0x0d] & 0xf);
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(Ym2203Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        boolean YM2203Type = (chipId == 0)
                ? parent.setting.getYM2203Type()[0].getUseReal()[0]
                : parent.setting.getYM2203Type()[1].getUseReal()[0];
        int YM2203SoundLocation = (chipId == 0)
                ? parent.setting.getYM2203Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYM2203Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !YM2203Type ? 0 : (YM2203SoundLocation < 0 ? 2 : 1);

        for (int c = 0; c < 6; c++) {

            Channel oyc = oldParam.channels[c];
            Channel nyc = newParam.channels[c];

            if (c == 2) {
                oyc.volumeL = frameBuffer.drawVolumeM(256, 8 + c * 8, 0, oyc.volumeL, nyc.volumeL, tp);
                oyc.note = frameBuffer.drawKeyBoard(c, oyc.note, nyc.note, tp);
                frameBuffer.drawInst(1, 12, c, oyc.inst, nyc.inst);
                Boolean[] r = drawCh3YM2203(frameBuffer, c, oyc.mask, nyc.mask, oyc.ex, nyc.ex, tp);
                oyc.mask = r[0]; oyc.ex = r[1];
            } else if (c < 3) {
                oyc.volumeL = frameBuffer.drawVolumeM(256, 8 + c * 8, 0, oyc.volumeL, nyc.volumeL, tp);
                oyc.note = frameBuffer.drawKeyBoard(c, oyc.note, nyc.note, tp);
                frameBuffer.drawInst(1, 12, c, oyc.inst, nyc.inst);
                oyc.mask = drawChYM2203(frameBuffer, c, oyc.mask, nyc.mask, tp);
            } else {
                oyc.volumeL = frameBuffer.drawVolumeM(256, 8 + (c + 3) * 8, 0, oyc.volumeL, nyc.volumeL, tp);
                oyc.note = frameBuffer.drawKeyBoard(c + 3, oyc.note, nyc.note, tp);
                oyc.mask = drawChYM2203(frameBuffer, c, oyc.mask, nyc.mask, tp);
            }
        }

        for (int c = 0; c < 3; c++) {
            Channel oyc = oldParam.channels[c + 6];
            Channel nyc = newParam.channels[c + 6];

            oyc.volume = frameBuffer.drawVolumeM(256, 8 + (c + 3) * 8, 0, oyc.volume, nyc.volume, tp);
            oyc.note = frameBuffer.drawKeyBoard(c + 3, oyc.note, nyc.note, tp);
            int[] r = drawTn(frameBuffer, 6, 2, c + 3, oyc.tn, nyc.tn, oyc.tntp, tp * 2);
            oyc.tn = r[0]; oyc.tntp = r[1];

            oyc.mask = drawChYM2203(frameBuffer, c + 6, oyc.mask, nyc.mask, tp);
        }

        oldParam.nfrq = frameBuffer.Nfrq(5, 32, oldParam.nfrq, newParam.nfrq);
        oldParam.efrq = frameBuffer.drawEfrq(18, 32, oldParam.efrq, newParam.efrq);
        oldParam.etype = frameBuffer.drawEType(33, 32, oldParam.etype, newParam.etype);
    }

    @Override
    public void initScreen() {
        boolean YM2203Type = (chipId == 0)
                ? parent.setting.getYM2203Type()[0].getUseReal()[0]
                : parent.setting.getYM2203Type()[1].getUseReal()[0];
        int YM2203SoundLocation = (chipId == 0)
                ? parent.setting.getYM2203Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYM2203Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !YM2203Type ? 0 : (YM2203SoundLocation < 0 ? 2 : 1);

        for (int ch = 0; ch < 3; ch++) {
            for (int i = 0; i < 4; i++) {
                newParam.channels[ch].inst[i * 11 + 0] = 0;
                newParam.channels[ch].inst[i * 11 + 1] = 0;
                newParam.channels[ch].inst[i * 11 + 2] = 0;
                newParam.channels[ch].inst[i * 11 + 3] = 0;
                newParam.channels[ch].inst[i * 11 + 4] = 0;
                newParam.channels[ch].inst[i * 11 + 5] = 0;
                newParam.channels[ch].inst[i * 11 + 6] = 0;
                newParam.channels[ch].inst[i * 11 + 7] = 0;
                newParam.channels[ch].inst[i * 11 + 8] = 0;
                newParam.channels[ch].inst[i * 11 + 9] = 0;
                newParam.channels[ch].inst[i * 11 + 10] = 0;
            }
            newParam.channels[ch].inst[44] = 0;
            newParam.channels[ch].inst[45] = 0;
            newParam.channels[ch].inst[46] = 0;
            newParam.channels[ch].inst[47] = 0;
            newParam.channels[ch].pan = 3;
            newParam.channels[ch].volumeL = 0;
            newParam.channels[ch].note = -1;
        }

        for (int ch = 3; ch < 6; ch++) { //FM EX
            newParam.channels[ch].pan = 0;
            newParam.channels[ch].note = -1;
            newParam.channels[ch].volumeL = 0;
            newParam.channels[ch].note = -1;
        }

        for (int ch = 0; ch < 3; ch++) { // SSG
            Channel channel = newParam.channels[ch + 6];
            channel.tn = 0;
            channel.volume = 0;
            channel.note = -1;
        }

        newParam.nfrq = 0;
        newParam.efrq = 0;
        newParam.etype = 0;

        drawScreenInitYM2203(frameBuffer, tp);
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
                            parent.resetChannelMask(Ym2203Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(Ym2203Chip.class, chipId, ch);
                    }
                }
                return;
            }

            // keyboard
            if (py < 10 * 8) {
                int ch = (py / 8) - 1;
                if (ch < 0) return;

                if (ev.getButton() == MouseEvent.BUTTON1) {
                    // Mask.
                    if (newParam.channels[ch].mask)
                        parent.resetChannelMask(Ym2203Chip.class, chipId, ch);
                    else
                        parent.setChannelMask(Ym2203Chip.class, chipId, ch);
                    return;
                }

                // Unmask.
                for (ch = 0; ch < 9; ch++) parent.resetChannelMask(Ym2203Chip.class, chipId, ch);
                return;
            }

            // Right-clicking on the sound does nothing.
            if (ev.getButton() == MouseEvent.BUTTON2) return;

            // Judgment in the tone display field
            int instCh = Math.min(px / (13 * 8), 2);

            if (instCh < 3) {
                // Copying a tone to the clipboard
                parent.getInstCh(Ym2203Chip.class, instCh, chipId);
            }
        }
    };

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeYM2203");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 136));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmYM2203
        //
        this.setPreferredSize(new Dimension(320, 136));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmYM2203");
        this.setTitle("YM2203");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static void drawScreenInitYM2203(FrameBuffer screen, int tp) {
        if (screen == null)
            return;

        // YM2203
        for (int y = 0; y < 3 + 3 + 3; y++) {

            screen.drawFont8(296, y * 8 + 8, 1, "   ");
            for (int i = 0; i < 96; i++) {
                int kx = Tables.kbl[(i % 12) * 2] + i / 12 * 28;
                int kt = Tables.kbl[(i % 12) * 2 + 1];
                screen.drawKbn(32 + kx, y * 8 + 8, kt, tp);
            }

            int d = 99;
            d = screen.drawVolumeM(256, 8 + y * 8, 0, d, 0, tp);

            if (y < 9) {
                drawChYM2203_P(screen, 0, y * 8 + 8, y, false, tp);
            }
        }
    }

    private static Boolean drawChYM2203(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChYM2203_P(screen, 0, 8 + ch * 8, ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static Boolean[] drawCh3YM2203(FrameBuffer screen, int ch, Boolean om, Boolean nm, boolean oe, boolean ne, int tp) {
        if (om == nm && oe == ne) {
            return new Boolean[] {om, oe};
        }

        drawCh3YM2612_P(screen, 0, 8 + ch * 8, ch, nm == null ? false : nm, ne, tp);
        om = nm;
        oe = ne;
        return new Boolean[] {om, oe};
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

    private static void drawChYM2203_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        if (ch < 3) {
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 0, 16, 8);
            screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(1 + ch));
        } else if (ch < 6) {
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 32, 0, 16, 8);
            screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(1 + ch - 3));
        } else if (ch < 9) {
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 32 * (ch - 5), 24, 32, 8);
        } else {
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 0, 24, 8);
        }
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
        int nfrq = -1;
        int efrq = -1;
        int etype = -1;
        final Channel[] channels = {
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel()
        };
    }


    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "YM2203"; }
        @Override public String menuText() { return "OPN"; }
        @Override public String category() { return "opn"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return Ym2203Chip.class; }
        @Override public boolean hasRegisterDump() { return true; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormYM2203(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 9) {
                audio.plugin.chipRegister.chip(Ym2203Chip.class).setMask(chipId, ch);
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 9) {
                audio.plugin.chipRegister.chip(Ym2203Chip.class).setMask(chipId, ch, false, audio.plugin.stopped);
            }
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (ch >= 0 && ch < 9) {
                if (mask)
                    audio.plugin.chipRegister.chip(Ym2203Chip.class).setMask(chipId, ch);
                else
                    audio.plugin.chipRegister.chip(Ym2203Chip.class).setMask(chipId, ch, false, audio.plugin.stopped);
            }
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 9; ch++)
                forceChannelMask(audio, Ym2203Chip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(Ym2203Chip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(2, MDSound.Chip.MAIN_TAG, Ym2203Chip.class, "ym2203", 200),
                    new MixerSlot(3, "FM", Ym2203Chip.class, "ym2203FM", 200),
                    new MixerSlot(4, "PSG", Ym2203Chip.class, "ym2203SSG", 120));
        }

        @Override public void updateMeters(Audio audio, VisVolume visVolume) {
            int fm = Meters.chipVolume(audio, Ym2203Chip.class) * 5;
            int ssg = 0;
            try {
                int[] reg = (int[]) Meters.chipInfo(audio, Ym2203Chip.class, "register");
                if (reg != null) {
                    int mixer = reg[0x07];
                    for (int ch = 0; ch < 3; ch++) {
                        boolean toneOn = (mixer & (0x01 << ch)) == 0;
                        boolean noiseOn = (mixer & (0x08 << ch)) == 0;
                        if (toneOn || noiseOn) {
                            int v = (reg[0x08 + ch] & 0xf) * 600;
                            if (v > ssg) ssg = v;
                        }
                    }
                }
            } catch (Exception _) {
            }
            visVolume.put("ym2203FM", fm);
            visVolume.put("ym2203SSG", ssg);
            visVolume.put("ym2203", Math.max(fm, ssg));
        }
    }
}
