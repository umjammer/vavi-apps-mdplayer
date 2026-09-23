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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import mdplayer.Audio;
import mdplayer.Common;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.Tables;
import mdplayer.chips.HuC6280Chip;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;

import static mdplayer.Common.searchSSGNote;
import mdplayer.form.View;
import mdsound.MDSound;


public class FormHuC6280 extends FormChipBase<FormHuC6280.Params> {

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeHuC6280");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 151));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmHuC6280
        //
        this.setPreferredSize(new Dimension(320, 151));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmHuC6280");
        this.setTitle("Huc6280");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

    private static final Preferences prefs = Preferences.userNodeForPackage(FormHuC6280.class);

    public FormHuC6280(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeHuC6280"), null, zoom);
        drawIcreenInitHuC6280(frameBuffer);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("HuC6280", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeHuC6280").getWidth() * zoom, frameSizeH + Common.getImage("planeHuC6280").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeHuC6280").getWidth() * zoom, frameSizeH + Common.getImage("planeHuC6280").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeHuC6280").getWidth() * zoom, frameSizeH + Common.getImage("planeHuC6280").getHeight() * zoom));
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
    public void changeScreenParams() {
        Map<String, Object> chip = audio.plugin.chipRegister.chip(HuC6280Chip.class).getInfo(chipId);
        if (chip.isEmpty()) return;

        //logger.log(Level.TRACE, "%d  %d".formatted(chips.MainVolumeL,chips.MainVolumeR));
        for (int ch = 0; ch < 6; ch++) {
            if (chip.get("channels." + ch + ".volumeL") == null) continue;
            Channel channel = newParam.channels[ch];
            //logger.log(Level.TRACE, "%d  %d".formatted(psg.outVolumeL, psg.outVolumeR));
            channel.volumeL = (int) chip.get("channels." + ch + ".outVolumeL");
            channel.volumeR = (int) chip.get("channels." + ch + ".outVolumeR");
            channel.volumeL = Math.min(channel.volumeL, 19);
            channel.volumeR = Math.min(channel.volumeR, 19);

            channel.pan = (int) chip.get("channels." + ch + ".pan");

            channel.inst = (int[]) chip.get("channels." + ch + ".wave");

            channel.dda = (boolean) chip.get("channels." + ch + ".dda");

            float ftone = (float) chip.get("channels." + ch + ".ftone");
            channel.note = searchSSGNote(ftone);
            if (channel.volumeL == 0 && channel.volumeR == 0) channel.note = -1;

            if (ch < 4) continue;

            channel.noise = (boolean) chip.get("channels." + ch + ".bNoiseOn");
            channel.nfrq = (int) chip.get("channels." + ch + ".noiseFrq");
        }

        newParam.mvolL = (int) chip.get("mainVolumeL");
        newParam.mvolR = (int) chip.get("mainVolumeR");
        newParam.LfoCtrl = (int) chip.get("lfoControl");
        newParam.LfoFrq = (int) chip.get("lfoFreq");
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(HuC6280Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        int tp = parent.setting.getHuC6280Type()[0].getUseReal()[0] ? 1 : 0;

        for (int c = 0; c < 6; c++) {

            Channel oyc = oldParam.channels[c];
            Channel nyc = newParam.channels[c];

            oyc.note = frameBuffer.drawKeyBoard(c, oyc.note, nyc.note, tp);

            oyc.volumeL = drawVolumeToHuC6280(frameBuffer, c, 1, oyc.volumeL, nyc.volumeL);
            oyc.volumeR = drawVolumeToHuC6280(frameBuffer, c, 2, oyc.volumeR, nyc.volumeR);
            oyc.pan = frameBuffer.PanType2(c, oyc.pan, nyc.pan, tp);

            WaveFormToHuC6280(frameBuffer, c, oyc.inst, nyc.inst);
            oyc.dda = drawDDAToHuC6280(frameBuffer, c, oyc.dda, nyc.dda);

            oyc.mask = ChHuC6280(frameBuffer, c, oyc.mask, nyc.mask, tp);

            if (c < 4) continue;

            oyc.noise = drawNoiseToHuC6280(frameBuffer, c, oyc.noise, nyc.noise);
            oyc.nfrq = drawNoiseFrqToHuC6280(frameBuffer, c, oyc.nfrq, nyc.nfrq);
        }

        oldParam.mvolL = drawMainVolumeToHuC6280(frameBuffer, 0, oldParam.mvolL, newParam.mvolL);
        oldParam.mvolR = drawMainVolumeToHuC6280(frameBuffer, 1, oldParam.mvolR, newParam.mvolR);

        oldParam.LfoCtrl = drawLfoCtrlToHuC6280(frameBuffer, oldParam.LfoCtrl, newParam.LfoCtrl);
        oldParam.LfoFrq = drawLfoFrqToHuC6280(frameBuffer, oldParam.LfoFrq, newParam.LfoFrq);
    }

    private final MouseListener pbScreen_MouseClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
            int py = ev.getY() / zoom;
            int px = ev.getX() / zoom;

            // For top label row, do nothing
            if (py < 1 * 8) {
                // However, if you click on ch, the mask will be inverted.
                if (px < 8) {
                    for (int ch = 0; ch < 6; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(HuC6280Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(HuC6280Chip.class, chipId, ch);
                    }
                }
                return;
            }

            // keyboard
            if (py < 7 * 8) {
                int ch = (py / 8) - 1;
                if (ch < 0) return;

                if (ev.getButton() == MouseEvent.BUTTON1) {
                    // Mask.
                    parent.setChannelMask(HuC6280Chip.class, chipId, ch);
                    return;
                }

                // Unmask.
                for (ch = 0; ch < 6; ch++) parent.resetChannelMask(HuC6280Chip.class, chipId, ch);
                return;
            }

            // Right-clicking on a tone does nothing
            if (ev.getButton() == MouseEvent.BUTTON2) return;

            // Judgment of tone display column
            int h = (py - 7 * 8) / (5 * 8);
            int w = Math.min(px / (13 * 8), 2);
            int instCh = h * 3 + w;

            if (instCh < 6) {
                // Copying a tone to the clipboard
                parent.getInstCh(HuC6280Chip.class, instCh, chipId);
            }
        }
    };

    @Override
    public void initScreen() {
        for (int c = 0; c < newParam.channels.length; c++) {
            newParam.channels[c].note = -1;
            newParam.channels[c].volumeL = -1;
            newParam.channels[c].volumeR = -1;
            newParam.channels[c].pan = -1;
            Arrays.fill(newParam.channels[c].inst, 0);
            newParam.channels[c].dda = false;
            newParam.channels[c].noise = false;
            newParam.channels[c].nfrq = 0;
        }
        newParam.mvolL = 0;
        newParam.mvolR = 0;
        newParam.LfoCtrl = 0;
        newParam.LfoFrq = 0;
    }

//#region draw buffer

    private static void drawIcreenInitHuC6280(FrameBuffer screen) {
        for (int ch = 0; ch < 6; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                screen.drawKbn(32 + kx, ch * 8 + 8, kt, 0);
            }
            screen.drawFont8(296, ch * 8 + 8, 1, "   ");
        }
    }

    private static int drawVolumeToHuC6280(FrameBuffer screen, int y, int c, int ov, int nv) {
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
        y = (y + 1) * 8;

        for (int i = 0; i <= 19; i++) {
            screen.drawVolumeP(256 + i * 2, y + sy, (1 + t), 0);
        }

        for (int i = 0; i <= nv; i++) {
            screen.drawVolumeP(256 + i * 2, y + sy, i > 17 ? (2 + t) : (0 + t), 0);
        }

        return ov;
    }

    private static Boolean ChHuC6280(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChHuC6280_P(screen, 0, 8 + ch * 8, ch, nm != null && nm, tp);
        om = nm;
        return om;
    }

    private static void WaveFormToHuC6280(FrameBuffer screen, int c, int[] oi, int[] ni) {
        for (int i = 0; i < 32; i++) {
            if (oi[i] == ni[i])
                continue;

            int n = (17 - ni[i]);
            int x = i + (((c > 2) ? c - 3 : c) * 8 * 13) + 4 * 7;
            int y = (((c > 2) ? 1 : 0) * 8 * 5) + 4 * 22;

            int m;
            m = (n > 7) ? 8 : n;
            screen.drawByteArray(x, y, FrameBuffer.rWavGraph, 64, m, 0, 1, 8);
            m = (n > 15) ? 8 : (Math.max((n - 8), 0));
            screen.drawByteArray(x, y - 8, FrameBuffer.rWavGraph, 64, m, 0, 1, 8);
            m = (n > 23) ? 8 : (Math.max((n - 16), 0));
            screen.drawByteArray(x, y - 16, FrameBuffer.rWavGraph, 64, m, 0, 1, 8);
            m = (n > 31) ? 8 : (Math.max((n - 24), 0));
            screen.drawByteArray(x, y - 23, FrameBuffer.rWavGraph, 64, m + 1, 0, 1, 7);

            oi[i] = ni[i];
        }
    }

    private static boolean drawDDAToHuC6280(FrameBuffer screen, int c, boolean od, boolean nd) {
        if (od == nd)
            return od;

        int x = (((c > 2) ? c - 3 : c) * 8 * 13) + 4 * 22;
        int y = (((c > 2) ? 1 : 0) * 8 * 5) + 4 * 18;

        screen.drawFont4(x, y, 0, nd ? "ON " : "OFF");
        od = nd;
        return od;
    }

    private static boolean drawNoiseToHuC6280(FrameBuffer screen, int c, boolean od, boolean nd) {
        if (od == nd)
            return od;

        int x = (((c > 2) ? c - 3 : c) * 8 * 13) + 4 * 22;
        int y = (((c > 2) ? 1 : 0) * 8 * 5) + 4 * 20;

        screen.drawFont4(x, y, 0, nd ? "ON " : "OFF");
        od = nd;
        return od;
    }

    private static int drawNoiseFrqToHuC6280(FrameBuffer screen, int c, int od, int nd) {
        if (od == nd)
            return od;

        int x = (((c > 2) ? c - 3 : c) * 8 * 13) + 4 * 22;
        int y = (((c > 2) ? 1 : 0) * 8 * 5) + 4 * 22;

        screen.drawFont4(x, y, 0, "%2d".formatted(nd));
        od = nd;
        return od;
    }

    private static int drawMainVolumeToHuC6280(FrameBuffer screen, int c, int od, int nd) {
        if (od == nd)
            return od;

        int x = 8 * 9;
        int y = c * 8 + 8 * 17;

        screen.drawFont4(x, y, 0, "%2d".formatted(nd));
        od = nd;
        return od;
    }

    private static int drawLfoCtrlToHuC6280(FrameBuffer screen, int od, int nd) {
        if (od == nd)
            return od;

        int x = 8 * 17;
        int y = 8 * 17;

        screen.drawFont4(x, y, 0, "%1d".formatted(nd));
        od = nd;
        return od;
    }

    private static int drawLfoFrqToHuC6280(FrameBuffer screen, int od, int nd) {
        if (od == nd)
            return od;

        int x = 8 * 16;
        int y = 8 * 18;

        screen.drawFont4(x, y, 0, "%3d".formatted(nd));
        od = nd;
        return od;
    }

    private static void drawChHuC6280_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 112, 0, 16, 8);
        screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(1 + ch));
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        boolean dda = false;
        int nfrq = -1;
        boolean noise = false;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        int mvolL = -1;
        int mvolR = -1;
        int LfoCtrl = -1;
        int LfoFrq = -1;

        final Channel[] channels = {new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel()};
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "HuC6280"; }
        @Override public String category() { return "wf"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return HuC6280Chip.class; }
        @Override public String title(int chipId) { return "OotakeHuC6280 (%s)".formatted(chipId == 0 ? "Primary" : "Secondary"); }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormHuC6280(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            HuC6280Chip c = audio.plugin.chipRegister.chip(HuC6280Chip.class);
            if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(HuC6280Chip.class).resetMask(chipId, ch);
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (mask)
                audio.plugin.chipRegister.chip(HuC6280Chip.class).setMask(chipId, ch);
            else
                audio.plugin.chipRegister.chip(HuC6280Chip.class).resetMask(chipId, ch);
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 6; ch++)
                forceChannelMask(audio, HuC6280Chip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(HuC6280Chip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(27, MDSound.Chip.MAIN_TAG, HuC6280Chip.class, "huc6280", 120));
        }
    }
}
