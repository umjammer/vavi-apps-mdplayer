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
import mdplayer.Chip;
import mdplayer.Common;
import mdplayer.chips.OkiM6258Chip;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.form.View;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;


public class FormOKIM6258 extends FormChipBase<FormOKIM6258.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormOKIM6258.class);

    public FormOKIM6258(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeMSM6258"), null, zoom);
        drawScreenInitOKIM6258(frameBuffer);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("OKIM6258", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeMSM6258").getWidth() * zoom, frameSizeH + Common.getImage("planeMSM6258").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeMSM6258").getWidth() * zoom, frameSizeH + Common.getImage("planeMSM6258").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeMSM6258").getWidth() * zoom, frameSizeH + Common.getImage("planeMSM6258").getHeight() * zoom));
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
        Map<String, Object> info = audio.plugin.chipRegister.chip(OkiM6258Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        newParam.pan = switch (((int) info.get("pan")) & 0x3) {
            case 0, 3 -> 3;
            case 1 -> 2;
            case 2 -> 1;
            default -> 3; // TODO
        };

        newParam.masterFreq = (int) info.get("masterFreq");
        newParam.divider = (int) info.get("divider");
        newParam.pbFreq = (int) info.get("pbFreq");

        int v = (int) (((Math.abs(((int) info.get("dataIn")) - 128) * 2) >> 3) * 1.2);
        if ((((int) info.get("status")) & 0x2) == 0) v = 0;
        v = Math.min(v, 38);
        if (newParam.volumeL < v && ((newParam.pan & 0x2) != 0)) {
            newParam.volumeL = v;
        } else {
            newParam.volumeL--;
        }
        if (newParam.volumeR < v && ((newParam.pan & 0x1) != 0)) {
            newParam.volumeR = v;
        } else {
            newParam.volumeR--;
        }
    
        newParam.mask = audio.plugin.chipRegister.chip(OkiM6258Chip.class).getMask(chipId, 0);
    }

    @Override
    public void drawScreenParams() {
        Params ost = oldParam;
        Params nst = newParam;

        int[] r = drawPanToOKIM6258(frameBuffer, ost.pan, nst.pan, ost.pantp, 0);
        ost.pan = r[0]; ost.pantp = r[1];

        if (ost.masterFreq != nst.masterFreq) {
            frameBuffer.drawFont4(12 * 4, 8, 0, "{%5d}".formatted(nst.masterFreq));
            ost.masterFreq = nst.masterFreq;
        }

        if (ost.divider != nst.divider) {
            frameBuffer.drawFont4(19 * 4, 8, 0, "{%5d}".formatted(nst.divider));
            ost.divider = nst.divider;
        }

        if (ost.pbFreq != nst.pbFreq) {
            frameBuffer.drawFont4(26 * 4, 8, 0, "{%5d}".formatted(nst.pbFreq));
            ost.pbFreq = nst.pbFreq;
        }

        ost.volumeL = frameBuffer.drawVolumeM(256, 8 + 0 * 8, 1, ost.volumeL, nst.volumeL / 2, 0);
        ost.volumeR = frameBuffer.drawVolumeM(256, 8 + 0 * 8, 2, ost.volumeR, nst.volumeR / 2, 0);

        ost.mask = drawChOKIM6258(frameBuffer, ost.mask, nst.mask, 0);
    }

    @Override
    public void initScreen() {
        newParam.pan = 3;
        newParam.masterFreq = 0;
        newParam.divider = 0;
        newParam.pbFreq = 0;
        newParam.volumeL = 0;
        newParam.volumeR = 0;
    }

    private final MouseListener pbScreen_MouseClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
            int px = ev.getX() / zoom;
            int py = ev.getY() / zoom;

            //  For top label row, do nothing
            if (py < 1 * 8) {
                //  However, if you click on ch, the mask will be inverted.
                if (px < 8) {
                    if (newParam.mask)
                        parent.resetChannelMask(OkiM6258Chip.class, chipId, 0);
                    else
                        parent.setChannelMask(OkiM6258Chip.class, chipId, 0);
                }
                return;
            }

            // keyboard
            if (py < 2 * 8) {
                int ch = (py / 8) - 1;
                if (ch < 0) return;

                if (ev.getButton() == MouseEvent.BUTTON1) {
                    // mask
                    parent.setChannelMask(OkiM6258Chip.class, chipId, 0);
                    return;
                }

                //  Unmask.
                parent.resetChannelMask(OkiM6258Chip.class, chipId, 0);
            }
        }
    };

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeMSM6258");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 16));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmOKIM6258
        //
        this.setPreferredSize(new Dimension(320, 16));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmOKIM6258");
        this.setTitle("OKIM6258");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static void drawScreenInitOKIM6258(FrameBuffer screen) {
        int o;
        int n;

        o = 0;
        n = 3;
        int[] r = drawPanToOKIM6258(screen, o, n, o, 0);
        o = r[0]; o = r[1];

        screen.drawFont4(12 * 4, 8, 0, "%5d".formatted(0));
        screen.drawFont4(19 * 4, 8, 0, "%5d".formatted(0));
        screen.drawFont4(26 * 4, 8, 0, "%5d".formatted(0));

        o = 0;
        n = 38;
        o = screen.drawVolumeM(256, 8 + 0 * 8, 1, o, n / 2, 0);
        o = 0;
        n = 38;
        o = screen.drawVolumeM(256, 8 + 0 * 8, 2, o, n / 2, 0);
    }

    private static int[] drawPanToOKIM6258(FrameBuffer screen, int ot, int nt, int otp, int ntp) {
        if (ot == nt && otp == ntp) {
            return new int[] {ot, otp};
        }

        screen.drawPanP(24, 8, nt, ntp);
        ot = nt;
        otp = ntp;
        return new int[] {ot, otp};
    }

    private static Boolean drawChOKIM6258(FrameBuffer screen, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChOKIM6258_P(screen, 0, 8 + 0 * 8, nm != null && nm, tp);
        om = nm;
        return om;
    }

    private static void drawChOKIM6258_P(FrameBuffer screen, int x, int y, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 8 * 8, 0, 24, 8);
    }

//#endregion

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        int pan = -1;
        int pantp = -1;
        int masterFreq = -1;
        int divider = -1;
        int pbFreq = -1;
        int volumeL = -1;
        int volumeR = -1;
        public boolean keyon = false;
        Boolean mask = false;
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "OKIM6258"; }
        @Override public String category() { return "pcm"; }
        @Override public Class<? extends Chip> chip() { return OkiM6258Chip.class; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormOKIM6258(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends Chip> chip, int chipId, int ch) {
            OkiM6258Chip c = audio.plugin.chipRegister.chip(OkiM6258Chip.class);
            if (!c.getMask(chipId, 0)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(OkiM6258Chip.class).resetMask(chipId, ch);
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(37, MDSound.Chip.MAIN_TAG, OkiM6258Chip.class, "okim6258", 200));
        }
    }
}
