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
import mdplayer.chips.Ym2151Chip;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdplayer.form.View;
import mdsound.MDSound;


public class FormYM2151 extends FormChipBase<FormYM2151.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormYM2151.class);

    public FormYM2151(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());
        initializeComponent();

        bind(Common.getImage("planeE"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("YM2151", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeE").getWidth() * zoom, frameSizeH + Common.getImage("planeE").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeE").getWidth() * zoom, frameSizeH + Common.getImage("planeE").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeE").getWidth() * zoom, frameSizeH + Common.getImage("planeE").getHeight() * zoom));
        componentListener.componentResized(null);
    }

    private final ComponentListener componentListener = new ComponentAdapter() {
        @Override
        public void componentMoved(ComponentEvent e) {
            prefs.putInt("x", e.getComponent().getX());
            prefs.putInt("y", e.getComponent().getY());
        }
    };

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
                    for (ch = 0; ch < 8; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(Ym2151Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(Ym2151Chip.class, chipId, ch);
                    }
                }
                return;
            }

            ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ch < 8) {
                if (ev.getButton() == MouseEvent.BUTTON1) {
                    parent.setChannelMask(Ym2151Chip.class, chipId, ch);
                    return;
                }

                for (ch = 0; ch < 8; ch++) parent.resetChannelMask(Ym2151Chip.class, chipId, ch);
                return;

            }

            // Judgment of tone display column

            int h = (py - 9 * 8) / (6 * 8);
            int w = Math.min(px / (13 * 8), 2);
            int instCh = h * 3 + w;

            if (instCh < 8) {
                // Copying a tone to the clipboard
                parent.getInstCh(Ym2151Chip.class, instCh, chipId);
            }
        }
    };

    @Override
    public void initScreen() {
        boolean YM2151Type = (chipId == 0)
                ? parent.setting.getYM2151Type()[0].getUseReal()[0]
                : parent.setting.getYM2151Type()[1].getUseReal()[0];
        int YM2151SoundLocation = (chipId == 0)
                ? parent.setting.getYM2151Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYM2151Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !YM2151Type ? 0 : (YM2151SoundLocation < 0 ? 2 : 1);

        for (int ch = 0; ch < 8; ch++) {

            frameBuffer.drawFont8(296, ch * 8 + 8, 1, "   ");

            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                frameBuffer.drawKbn(32 + kx, ch * 8 + 8, kt, tp);
            }

            drawChYM2151_P(frameBuffer, 0, ch * 8 + 8, ch, false, tp);
            frameBuffer.drawPanP(24, ch * 8 + 8, 3, tp);
            int d = 99;
            d = frameBuffer.drawVolumeM(256, 8 + ch * 8, 1, d, 0, tp);
            d = 99;
            d = frameBuffer.drawVolumeM(256, 8 + ch * 8, 2, d, 0, tp);
        }
    }

    // A table that converts CON connections into an OP mask sequence
    // 7  6   5   4   3   2  1  0
    // x, C2, M2, C1, M1, x, x, x

    private static final byte[] md = {
            0x40,
            0x40,
            0x40,
            0x40,
            0x50,
            0x70,
            0x70,
            0x78,
    };

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(Ym2151Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int[] ym2151Register = (int[]) info.get("register");
        int[] fmKeyYM2151 = (int[]) info.get("keyOn");
        int[] fmYM2151Vol = (int[]) info.get("volume");

        for (int ch = 0; ch < 8; ch++) {
            for (int i = 0; i < 4; i++) {
                int ops = (i == 0) ? 0 : ((i == 1) ? 16 : ((i == 2) ? 8 : 24));
                newParam.channels[ch].inst[i * 11 + 0] = ym2151Register[0x80 + ops + ch] & 0x1f;  // AR
                newParam.channels[ch].inst[i * 11 + 1] = ym2151Register[0xa0 + ops + ch] & 0x1f;  // DR
                newParam.channels[ch].inst[i * 11 + 2] = ym2151Register[0xc0 + ops + ch] & 0x1f;  // SR
                newParam.channels[ch].inst[i * 11 + 3] = ym2151Register[0xe0 + ops + ch] & 0x0f;  // RR
                newParam.channels[ch].inst[i * 11 + 4] = (ym2151Register[0xe0 + ops + ch] & 0xf0) >> 4; // SL
                newParam.channels[ch].inst[i * 11 + 5] = ym2151Register[0x60 + ops + ch] & 0x7f; // TL
                newParam.channels[ch].inst[i * 11 + 6] = (ym2151Register[0x80 + ops + ch] & 0xc0) >> 6; // KS
                newParam.channels[ch].inst[i * 11 + 7] = ym2151Register[0x40 + ops + ch] & 0x0f; // ML
                newParam.channels[ch].inst[i * 11 + 8] = (ym2151Register[0x40 + ops + ch] & 0x70) >> 4; // DT
                newParam.channels[ch].inst[i * 11 + 9] = (ym2151Register[0xc0 + ops + ch] & 0xc0) >> 6; // DT2
                newParam.channels[ch].inst[i * 11 + 10] = (ym2151Register[0xa0 + ops + ch] & 0x80) >> 7; // AM
            }
            newParam.channels[ch].inst[44] = ym2151Register[0x20 + ch] & 0x07; // AL
            newParam.channels[ch].inst[45] = (ym2151Register[0x20 + ch] & 0x38) >> 3; // FB
            newParam.channels[ch].inst[46] = (ym2151Register[0x38 + ch] & 0x3); // AMS
            newParam.channels[ch].inst[47] = (ym2151Register[0x38 + ch] & 0x70) >> 4; // PMS

            int p = (ym2151Register[0x20 + ch] & 0xc0) >> 6;
            newParam.channels[ch].pan = p == 1 ? 2 : (p == 2 ? 1 : p);
            int note = (ym2151Register[0x28 + ch] & 0x0f);
            note = (note < 3) ? note : (note < 7 ? note - 1 : (note < 11 ? note - 2 : note - 3));
            int oct = ((ym2151Register[0x28 + ch] & 0x70) >> 4);
            //newParam.ym2151[chipId].channels[ch].note = (fmKeyYM2151[ch] > 0) ? (oct * 12 + note + audio.vgmReal.YM2151Hosei + 1 + 9) : -1;
            int hosei = 0;
            if (audio.plugin.driverVirtual != null) { // is Vgm)
                hosei = audio.plugin.chipRegister.chip(Ym2151Chip.class).corrections[chipId];
            }
            newParam.channels[ch].note = ((fmKeyYM2151[ch] & 1) != 0) ? (oct * 12 + note + hosei) : -1;

            byte con = (byte) fmKeyYM2151[ch];
            int v = 127;
            byte m = md[ym2151Register[0x20 + ch] & 7];

            byte carrierOp = (byte) (con & m);

            // OP1 M1
            v = (((carrierOp & 0x08) != 0) && v > (ym2151Register[0x60 + ch] & 0x7f)) ? (ym2151Register[0x60 + ch] & 0x7f) : v;
            // OP3 C1
            v = (((carrierOp & 0x10) != 0) && v > (ym2151Register[0x68 + ch] & 0x7f)) ? (ym2151Register[0x68 + ch] & 0x7f) : v;
            // OP2 M2
            v = (((carrierOp & 0x20) != 0) && v > (ym2151Register[0x70 + ch] & 0x7f)) ? (ym2151Register[0x70 + ch] & 0x7f) : v;
            // OP4 C2
            v = (((carrierOp & 0x40) != 0) && v > (ym2151Register[0x78 + ch] & 0x7f)) ? (ym2151Register[0x78 + ch] & 0x7f) : v;

            newParam.channels[ch].volumeL = Math.clamp((int) ((127 - v) / 127.0 * ((ym2151Register[0x20 + ch] & 0x80) != 0 ? 1 : 0) * fmYM2151Vol[ch] / 80.0), 0, 19);
            newParam.channels[ch].volumeR = Math.clamp((int) ((127 - v) / 127.0 * ((ym2151Register[0x20 + ch] & 0x40) != 0 ? 1 : 0) * fmYM2151Vol[ch] / 80.0), 0, 19);

            newParam.channels[ch].kf = ((ym2151Register[0x30 + ch] & 0xfc) >> 2);
        }
        newParam.ne = ((ym2151Register[0x0f] & 0x80) >> 7);
        newParam.nfrq = ((ym2151Register[0x0f] & 0x1f) >> 0);
        newParam.lfrq = ((ym2151Register[0x18] & 0xff) >> 0);
        newParam.pmd = (int) info.get("pmd");
        newParam.amd = (int) info.get("amd");
        newParam.waveform = ((ym2151Register[0x1b] & 0x3) >> 0);
        newParam.lfosync = ((ym2151Register[0x01] & 0x02) >> 1);
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(Ym2151Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        for (int c = 0; c < 8; c++) {
            Channel oyc = oldParam.channels[c];
            Channel nyc = newParam.channels[c];

            boolean YM2151Type = (chipId == 0)
                    ? parent.setting.getYM2151Type()[0].getUseReal()[0]
                    : parent.setting.getYM2151Type()[1].getUseReal()[0];
            int YM2151SoundLocation = (chipId == 0)
                    ? parent.setting.getYM2151Type()[0].getRealChipInfo()[0].getSoundLocation()
                    : parent.setting.getYM2151Type()[1].getRealChipInfo()[0].getSoundLocation();
            int tp = !YM2151Type ? 0 : (YM2151SoundLocation < 0 ? 2 : 1);

            frameBuffer.drawInst(1, 11, c, oyc.inst, nyc.inst);

            { int[] r = frameBuffer.pan(24, 8 + c * 8, oyc.pan, nyc.pan, oyc.pantp, tp); oyc.pan = r[0]; oyc.pantp = r[1]; }
            oyc.note = frameBuffer.drawKeyBoard(c, oyc.note, nyc.note, tp);

            oyc.volumeL = frameBuffer.drawVolumeM(256, 8 + c * 8, 1, oyc.volumeL, nyc.volumeL, tp);
            oyc.volumeR = frameBuffer.drawVolumeM(256, 8 + c * 8, 2, oyc.volumeR, nyc.volumeR, tp);

            oyc.mask = drawChYM2151(frameBuffer, c, oyc.mask, nyc.mask, tp);

            oyc.kf = drawKfYM2151(frameBuffer, c, oyc.kf, nyc.kf);
        }

        oldParam.ne = drawNeYM2151(frameBuffer, oldParam.ne, newParam.ne);
        oldParam.nfrq = drawNfrqYM2151(frameBuffer, oldParam.nfrq, newParam.nfrq);
        oldParam.lfrq = drawLfrqYM2151(frameBuffer, oldParam.lfrq, newParam.lfrq);
        oldParam.amd = drawAmdYM2151(frameBuffer, oldParam.amd, newParam.amd);
        oldParam.pmd = drawPmdYM2151(frameBuffer, oldParam.pmd, newParam.pmd);
        oldParam.waveform = drawWaveFormYM2151(frameBuffer, oldParam.waveform, newParam.waveform);
        oldParam.lfosync = drawLfoSyncYM2151(frameBuffer, oldParam.lfosync, newParam.lfosync);
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeE");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 216));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmYM2151
        //
        this.setPreferredSize(new Dimension(320, 216));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmYM2151");
        this.setTitle("OPM");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static Boolean drawChYM2151(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChYM2151_P(screen, 0, 8 + ch * 8, ch, nm != null && nm, tp);
        om = nm;
        return om;
    }

    private static int drawKfYM2151(FrameBuffer screen, int ch, int ok, int nk) {
        if (ok == nk) {
            return ok;
        }

        int x = (ch % 4) * 4 * 3 + 4 * 67;
        int y = (ch / 4) * 8 + 8 * 22;
        screen.drawFont4Int(x, y, 0, 2, nk);
        ok = nk;
        return ok;
    }

    private static int drawNeYM2151(FrameBuffer screen, int one, int nne) {
        if (one == nne) {
            return one;
        }

        int x = 4 * 60;
        int y = 8 * 22;
        screen.drawFont4Int(x, y, 0, 1, nne);

        one = nne;
        return one;
    }

    private static int drawNfrqYM2151(FrameBuffer screen, int onfrq, int nnfrq) {
        if (onfrq == nnfrq) {
            return onfrq;
        }

        int x = 4 * 60;
        int y = 8 * 23;
        screen.drawFont4Int(x, y, 0, 2, nnfrq);

        onfrq = nnfrq;
        return onfrq;
    }

    private static int drawLfrqYM2151(FrameBuffer screen, int olfrq, int nlfrq) {
        if (olfrq == nlfrq) {
            return olfrq;
        }

        int x = 4 * 59;
        int y = 8 * 24;
        screen.drawFont4Int(x, y, 0, 3, nlfrq);

        olfrq = nlfrq;
        return olfrq;
    }

    private static int drawAmdYM2151(FrameBuffer screen, int oamd, int namd) {
        if (oamd == namd) {
            return oamd;
        }

        int x = 4 * 59;
        int y = 8 * 26;
        screen.drawFont4Int(x, y, 0, 3, namd);

        oamd = namd;
        return oamd;
    }

    private static int drawPmdYM2151(FrameBuffer screen, int opmd, int npmd) {
        if (opmd == npmd) {
            return opmd;
        }

        int x = 4 * 59;
        int y = 8 * 25;
        screen.drawFont4Int(x, y, 0, 3, npmd);

        opmd = npmd;
        return opmd;
    }

    private static int drawWaveFormYM2151(FrameBuffer screen, int owaveform, int nwaveform) {
        if (owaveform == nwaveform) {
            return owaveform;
        }

        int x = 4 * 68;
        int y = 8 * 24;
        screen.drawFont4Int(x, y, 0, 1, nwaveform);

        owaveform = nwaveform;
        return owaveform;
    }

    private static int drawLfoSyncYM2151(FrameBuffer screen, int olfosync, int nlfosync) {
        if (olfosync == nlfosync) {
            return olfosync;
        }

        int x = 4 * 68;
        int y = 8 * 25;
        screen.drawFont4Int(x, y, 0, 1, nlfosync);

        olfosync = nlfosync;
        return olfosync;
    }

    private static void drawChYM2151_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 0, 16, 8);
        screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(1 + ch));
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        int kf = -1;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        int ne = -1;
        int nfrq = -1;
        int lfrq = -1;
        int pmd = -1;
        int amd = -1;
        int waveform = -1;
        int lfosync = -1;
        final Channel[] channels = {
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel()
        };
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "YM2151"; }
        @Override public String menuText() { return "OPM"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return Ym2151Chip.class; }
        @Override public boolean hasRegisterDump() { return true; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormYM2151(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            Ym2151Chip c = audio.plugin.chipRegister.chip(Ym2151Chip.class);
            if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.setMask(chipId, ch, false, audio.plugin.stopped);
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(Ym2151Chip.class).setMask(chipId, ch, false, audio.plugin.stopped);
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (mask)
                audio.plugin.chipRegister.chip(Ym2151Chip.class).setMask(chipId, ch);
            else
                audio.plugin.chipRegister.chip(Ym2151Chip.class).setMask(chipId, ch, false, audio.plugin.stopped);
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 8; ch++)
                forceChannelMask(audio, Ym2151Chip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(Ym2151Chip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(1, MDSound.Chip.MAIN_TAG, Ym2151Chip.class, "ym2151", 200));
        }
    }
}
