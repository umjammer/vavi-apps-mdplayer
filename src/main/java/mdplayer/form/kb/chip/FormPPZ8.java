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
import mdplayer.chips.Ppz8Chip;
import mdplayer.chips.SegaPcmChip;
import mdplayer.form.kb.PcmChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdplayer.form.View;
import mdsound.MDSound;


public class FormPPZ8 extends FormChipBase<FormPPZ8.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormPPZ8.class);

    public FormPPZ8(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        bind(Common.getImage("planePPZ8"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("PPZ8", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planePPZ8").getWidth() * zoom, frameSizeH + Common.getImage("planePPZ8").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planePPZ8").getWidth() * zoom, frameSizeH + Common.getImage("planePPZ8").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planePPZ8").getWidth() * zoom, frameSizeH + Common.getImage("planePPZ8").getHeight() * zoom));
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
            //int px = ev.getX() / zoom;
            int py = ev.getY() / zoom;

            int ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ch < 8) {
                if (ev.getButton() == MouseEvent.BUTTON1) {
                    parent.setChannelMask(Ppz8Chip.class, chipId, ch);
                    return;
                }

                for (ch = 0; ch < 8; ch++) parent.resetChannelMask(Ppz8Chip.class, chipId, ch);
            }
        }
    };

    private int searchPPZ8Note(int freq) {
        double m = Double.MAX_VALUE;

        int clock = clock(audio.plugin.chipRegister.chip(Ppz8Chip.class).inst(0));
        if (clock >= 1000000)
            clock = clock / 384;

        int n = 0;
        for (int i = 0; i < 12 * 8; i++) {
            //double a = Math.abs(freq - ((0x0800 << 2) * Tables.pcmMulTbl[i % 12 + 12] * Math.pow(2, ((int)(i / 12) - 4))));
            int a = (int) (
                    65536.0 / 2.0 / clock
                    //8000.0
                    //Tables.pcmMulTbl[i % 12 + 12]
                    //Math.pow(2, (i / 12 - 3))
            );
            if (freq > a) {
                m = a;
                n = i;
            }
        }
        return n;
    }

    @Override
    public void initScreen() {
        boolean PPZ8Type = false; // (chipId == 0) ? parent.setting.PPZ8Type.UseScci : parent.setting.PPZ8SType.UseScci;
        int tp = PPZ8Type ? 1 : 0;
        for (int ch = 0; ch < 8; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                frameBuffer.drawKbn(32 + kx, ch * 8 + 8, kt, tp);
            }
            frameBuffer.drawFont8(4 * 754, ch * 8 + 8, 1, "   ");
            frameBuffer.drawPanType2P(24, ch * 8 + 8, 0, tp);
            //frameBuffer.ChPPZ8_P(0, 8 + ch * 8, ch, false, tp);
            //int d = 99;
            //frameBuffer.VolumeToPPZ8(ch, 1,d, 0, tp);
            //d = 99;
            //frameBuffer.VolumeToPPZ8(ch, 2,d, 0, tp);
        }
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(Ppz8Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        for (int ch = 0; ch < 8; ch++) {
            if (info.get("channels." + ch + ".pan") == null) continue;

            int pan = (int) info.get("channels." + ch + ".pan");
            newParam.channels[ch].pan = ((pan < 6) ? 0xf : (4 * (9 - pan))) |
                    (((pan > 4) ? 0xf : (4 * pan)) * 0x10);

            if ((boolean) info.get("channels." + ch + ".keyOn")) {
                newParam.channels[ch].volumeL = Math.min(((int) info.get("channels." + ch + ".volume") * (newParam.channels[ch].pan & 0xf)) / 8, 19);
                newParam.channels[ch].volumeR = Math.min(((int) info.get("channels." + ch + ".volume") * ((newParam.channels[ch].pan & 0xf0) >> 4)) / 8, 19);
            } else {
                newParam.channels[ch].volumeL -= newParam.channels[ch].volumeL > 0 ? 1 : 0;
                newParam.channels[ch].volumeR -= newParam.channels[ch].volumeR > 0 ? 1 : 0;
            }

            newParam.channels[ch].srcFreq = (int) info.get("channels." + ch + ".srcFrequency");
            newParam.channels[ch].freq = (int) info.get("channels." + ch + ".frequency");

            newParam.channels[ch].note = SegaPcmChip.searchSegaPCMNote((int) info.get("channels." + ch + ".frequency") / (double) 0x8000);
            if (!(boolean) info.get("channels." + ch + ".playing")) newParam.channels[ch].note = -1;

            newParam.channels[ch].dda = (boolean) info.get("channels." + ch + ".dda");
            newParam.channels[ch].flg16 = (int) info.get("channels." + ch + ".num");

            newParam.channels[ch].sadr = (int) info.get("channels." + ch + ".ptr");
            newParam.channels[ch].eadr = (int) info.get("channels." + ch + ".end");
            newParam.channels[ch].ladr = (int) info.get("channels." + ch + ".loopStartOffset");
            newParam.channels[ch].leadr = (int) info.get("channels." + ch + ".loopEndOffset");
            newParam.channels[ch].volumeRL = (int) info.get("channels." + ch + ".volume");
            newParam.channels[ch].volumeRR = (int) info.get("channels." + ch + ".pan");
        }
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(Ppz8Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        int tp = 0; // ((chipId == 0) ? parent.setting.PPZ8Type.UseScci : parent.setting.PPZ8SType.UseScci) ? 1 : 0;

        for (int c = 0; c < 8; c++) {

            Channel orc = oldParam.channels[c];
            Channel nrc = newParam.channels[c];

            orc.volumeL = frameBuffer.drawVolumeXY(64, c * 2 + 2, 1, orc.volumeL, nrc.volumeL, tp);
            orc.volumeR = frameBuffer.drawVolumeXY(64, c * 2 + 3, 1, orc.volumeR, nrc.volumeR, tp);
            orc.note = frameBuffer.drawKeyBoard(c, orc.note, nrc.note, tp);
            orc.pan = drawPanType3(frameBuffer, c, orc.pan, nrc.pan, tp);

            //orc.mask = frameBuffer.drawChC140(c, orc.mask, nrc.mask, tp);

            oldParam.channels[c].dda = frameBuffer.drawNESSw(4 * 4, c * 8 + 8 * 10, oldParam.channels[c].dda, newParam.channels[c].dda);

            int dmy;

            orc.flg16 = frameBuffer.font4Hex16Bit(4 * 9, c * 8 + 8 * 10, 0, orc.flg16, nrc.flg16);
            orc.srcFreq = frameBuffer.font4Hex16Bit(4 * 15, c * 8 + 8 * 10, 0, orc.srcFreq, nrc.srcFreq);

            dmy = orc.freq;
            dmy = drawFont4Hex32Bit(frameBuffer, 4 * 21, c * 8 + 8 * 10, 0, dmy, nrc.freq);
            orc.freq = dmy;

            dmy = orc.sadr;
            dmy = drawFont4Hex32Bit(frameBuffer, 4 * 31, c * 8 + 8 * 10, 0, dmy, nrc.sadr);
            orc.sadr = dmy;

            dmy = orc.eadr;
            dmy = drawFont4Hex32Bit(frameBuffer, 4 * 41, c * 8 + 8 * 10, 0, dmy, nrc.eadr);
            orc.eadr = dmy;

            dmy = orc.ladr;
            dmy = drawFont4Hex32Bit(frameBuffer, 4 * 51, c * 8 + 8 * 10, 0, dmy, nrc.ladr);
            orc.ladr = dmy;

            dmy = orc.leadr;
            dmy = drawFont4Hex32Bit(frameBuffer, 4 * 61, c * 8 + 8 * 10, 0, dmy, nrc.leadr);
            orc.leadr = dmy;

            orc.volumeRL = frameBuffer.font4HexByte(4 * 71, c * 8 + 8 * 10, 0, orc.volumeRL, nrc.volumeRL);
            orc.volumeRR = frameBuffer.font4HexByte(4 * 75, c * 8 + 8 * 10, 0, orc.volumeRR, nrc.volumeRR);
        }
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planePPZ8");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(321, 145));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmPPZ8
        //
        this.setPreferredSize(new Dimension(321, 145));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmPPZ8");
        this.setTitle("Ppz8Inst");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static int drawPanType3(FrameBuffer screen, int c, int ot, int nt, int tp) {
        if (ot == nt) {
            return ot;
        }

        drawPanType3P(screen, 24, 8 + c * 8, nt, tp);
        ot = nt;
        return ot;
    }

    private static int drawFont4Hex32Bit(FrameBuffer screen, int x, int y, int t, int on, int nn) {
        if (on == nn)
            return on;

        drawFont4Hex32Bit(screen, x, y, t, nn);
        on = nn;
        return on;
    }

    private static void drawFont4Hex32Bit(FrameBuffer screen, int x, int y, int t, int num) {
        if (screen == null)
            return;

        int n;
        num = Common.range(num, 0, 0xffff_ffff);

        n = num / 0x1000_0000;
        num -= n * 0x1000_0000;
        n = (n > 0xf) ? 0 : n;
        screen.drawFont4(x, y, t, Tables.hexCh[n]);

        n = num / 0x100_0000;
        num -= n * 0x100_0000;
        n = (n > 0xf) ? 0 : n;
        x += 4;
        screen.drawFont4(x, y, t, Tables.hexCh[n]);

        n = num / 0x10_0000;
        num -= n * 0x10_0000;
        n = (n > 0xf) ? 0 : n;
        x += 4;
        screen.drawFont4(x, y, t, Tables.hexCh[n]);

        n = num / 0x1_0000;
        num -= n * 0x1_0000;
        n = (n > 0xf) ? 0 : n;
        x += 4;
        screen.drawFont4(x, y, t, Tables.hexCh[n]);

        n = num / 0x1000;
        num -= n * 0x1000;
        n = (n > 0xf) ? 0 : n;
        x += 4;
        screen.drawFont4(x, y, t, Tables.hexCh[n]);

        n = num / 0x100;
        num -= n * 0x100;
        n = (n > 0xf) ? 0 : n;
        x += 4;
        screen.drawFont4(x, y, t, Tables.hexCh[n]);

        n = num / 0x10;
        num -= n * 0x10;
        n = (n > 0xf) ? 0 : n;
        x += 4;
        screen.drawFont4(x, y, t, Tables.hexCh[n]);

        n = num / 1;
        x += 4;
        screen.drawFont4(x, y, t, Tables.hexCh[n]);
    }

    private static void drawPanType3P(FrameBuffer screen, int x, int y, int t, int tp) {
        if (screen == null) {
            return;
        }

        int p = (t & 0x0f);
        p = p == 0 ? 0 : ((p + 1) / 4);
        screen.drawByteArray(x, y, FrameBuffer.rPan2[tp], 32, p * 4, 0, 4, 8);
        p = ((t & 0xf0) >> 4);
        p = p == 0 ? 0 : ((p + 1) / 4);
        screen.drawByteArray(x + 4, y, FrameBuffer.rPan2[tp], 32, p * 4, 0, 4, 8);
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends PcmChannelParams {

        boolean dda = false;
        int volumeRL = -1;
        int volumeRR = -1;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final Channel[] channels = {
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel()
        };
    }


    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "PPZ8"; }
        @Override public String category() { return "driver"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return Ppz8Chip.class; }
        @Override public String title(int chipId) { return "Ppz8Inst (%s)".formatted(chipId == 0 ? "Primary" : "Secondary"); }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormPPZ8(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            Ppz8Chip c = audio.plugin.chipRegister.chip(Ppz8Chip.class);
            if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 8) {
                audio.plugin.chipRegister.chip(Ppz8Chip.class).resetMask(chipId, ch);
            }
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(61, MDSound.Chip.MAIN_TAG, Ppz8Chip.class, "ppz8", 200));
        }
    }
}
