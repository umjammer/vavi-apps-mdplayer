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
import mdplayer.chips.Rf5C164Chip;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdplayer.form.View;
import mdsound.MDSound;


public class FormMegaCD extends FormChipBase<FormMegaCD.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormMegaCD.class);

    public FormMegaCD(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeC"), null, zoom);
        drawScreenInitRF5C164(frameBuffer);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("RF5C164", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeC").getWidth() * zoom, frameSizeH + Common.getImage("planeC").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeC").getWidth() * zoom, frameSizeH + Common.getImage("planeC").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeC").getWidth() * zoom, frameSizeH + Common.getImage("planeC").getHeight() * zoom));
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
        Map<String, Object> info = audio.plugin.chipRegister.chip(Rf5C164Chip.class).getInfo(chipId);
        if (info.isEmpty()) return; // the song being played does not use this chip

        for (int ch = 0; ch < 8; ch++) {
            if ((boolean) info.get("channels." + ch + ".enable")) {
                newParam.channels[ch].note = searchRf5c164Note((int) info.get("channels." + ch + ".stepB"));
                newParam.channels[ch].volumeL = Math.clamp((int) info.get("channels." + ch + ".mulL") / 3, 0, 19);
                newParam.channels[ch].volumeR = Math.clamp((int) info.get("channels." + ch + ".mulR") / 3, 0, 19);
            } else {
                newParam.channels[ch].note = -1;
                newParam.channels[ch].volumeL = 0;
                newParam.channels[ch].volumeR = 0;
            }
            newParam.channels[ch].pan = (int) info.get("channels." + ch + ".pan");
        }
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(Rf5C164Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        for (int c = 0; c < 8; c++) {

            ChannelParams orc = oldParam.channels[c];
            ChannelParams nrc = newParam.channels[c];

            orc.volumeL = frameBuffer.drawVolumeM(256, 8 + c * 8, 1, orc.volumeL, nrc.volumeL, 0);
            orc.volumeR = frameBuffer.drawVolumeM(256, 8 + c * 8, 2, orc.volumeR, nrc.volumeR, 0);
            orc.note = frameBuffer.drawKeyBoard(c, orc.note, nrc.note, 0);
            orc.pan = frameBuffer.PanType2(c, orc.pan, nrc.pan, 0);
            orc.mask = drawChRF5C164(frameBuffer, c, orc.mask, nrc.mask, 0);
        }
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
                    for (ch = 0; ch < 8; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(Rf5C164Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(Rf5C164Chip.class, chipId, ch);
                    }
                }
                return;
            }

            ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ev.getButton() == MouseEvent.BUTTON1) {
                parent.setChannelMask(Rf5C164Chip.class, chipId, ch);
                return;
            }

            for (ch = 0; ch < 8; ch++) parent.resetChannelMask(Rf5C164Chip.class, chipId, ch);
        }
    };

    private static int searchRf5c164Note(int freq) {
        double m = Double.MAX_VALUE;
        int n = 0;
        for (int i = 0; i < 12 * 8; i++) {
            double a = Math.abs(freq - (0x0800 * Tables.pcmMulTbl[i % 12 + 12] * Math.pow(2, ((i / 12) - 4))));
            if (m > a) {
                m = a;
                n = i;
            }
        }
        return n;
    }

    @Override
    public void initScreen() {
        for (int c = 0; c < newParam.channels.length; c++) {
            newParam.channels[c].note = -1;
            newParam.channels[c].volumeL = -1;
            newParam.channels[c].volumeR = -1;
            newParam.channels[c].pan = -1;
        }
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeC");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 72));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmMegaCD
        //
        this.setPreferredSize(new Dimension(320, 72));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setMaximumSize(new Dimension(336, 111));
        this.setMinimumSize(new Dimension(336, 111));
        this.setName("frmMegaCD");
        this.setTitle("MEGA-CD");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static void drawScreenInitRF5C164(FrameBuffer screen) {
        // RF5C164
        for (int ch = 0; ch < 8; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                screen.drawKbn(32 + kx, ch * 8 + 8, kt, 0);
            }
            screen.drawFont8(296, ch * 8 + 8, 1, "   ");
            screen.drawPanType2P(24, ch * 8 + 8, 0, 0);
        }
    }

    private static Boolean drawChRF5C164(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChRF5C164_P(screen, 0, 8 + ch * 8, ch, nm != null && nm, tp);
        om = nm;
        return om;
    }

    private static void drawChRF5C164_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 16, 0, 16, 8);
        screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(ch + 1));
    }

//#endregion

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final ChannelParams[] channels = {
                new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams(),
                new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams()
        };
    }


    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "RF5C164"; }
        @Override public String category() { return "pcm"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return Rf5C164Chip.class; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormMegaCD(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            Rf5C164Chip c = audio.plugin.chipRegister.chip(Rf5C164Chip.class);
            if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(Rf5C164Chip.class).resetMask(chipId, ch);
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (mask)
                audio.plugin.chipRegister.chip(Rf5C164Chip.class).setMask(chipId, ch);
            else
                audio.plugin.chipRegister.chip(Rf5C164Chip.class).resetMask(chipId, ch);
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 8; ch++)
                forceChannelMask(audio, Rf5C164Chip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(Rf5C164Chip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(34, MDSound.Chip.MAIN_TAG, Rf5C164Chip.class, "rf5c164", 200));
        }
    }
}
