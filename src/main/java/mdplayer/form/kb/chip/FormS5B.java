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
import mdplayer.chips.NpNesChip;
import mdplayer.chips.NpNesChip.Fme7Chip;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.form.View;
import mdplayer.form.VisVolume;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.Meters;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;

import static mdplayer.Common.searchSSGNote;


public class FormS5B extends FormChipBase<FormS5B.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormS5B.class);

    public FormS5B(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        bind(Common.getImage("planeS5B"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("S5B", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeS5B").getWidth() * zoom, frameSizeH + Common.getImage("planeS5B").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeS5B").getWidth() * zoom, frameSizeH + Common.getImage("planeS5B").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeS5B").getWidth() * zoom, frameSizeH + Common.getImage("planeS5B").getHeight() * zoom));
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
        Map<String, Object> info = audio.plugin.chipRegister.chip(Fme7Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        byte[] register = (byte[]) info.get("register");

        for (int ch = 0; ch < 3; ch++) { //SSG
            Channel channel = newParam.channels[ch];

            boolean t = (register[0x07] & (0x1 << ch)) == 0;
            boolean n = (register[0x07] & (0x8 << ch)) == 0;
            //logger.log(Level.TRACE, "r[8]=%x r[9]=%x r[10]=%x".formatted(register[0x8], register[0x9], register[0xa]));
            channel.tn = (t ? 1 : 0) + (n ? 2 : 0);
            newParam.nfrq = register[0x06] & 0x1f;
            newParam.efrq = (register[0x0c] & 0xff) * 0x100 + (register[0x0b] & 0xff);
            newParam.etype = (register[0x0d] & 0xf);

            int v = (register[0x08 + ch] & 0x1f);
            v = Math.min(v, 15);
            channel.volume = (int) (((t || n) ? 1 : 0) * v * (20.0 / 16.0));
            if (!t && !n && channel.volume > 0) {
                channel.volume--;
            }

            if (channel.volume == 0) {
                channel.note = -1;
            } else {
                int ft = register[0x00 + ch * 2];
                int ct = register[0x01 + ch * 2];
                int tp = (ct << 8) | ft;
                if (tp == 0) tp = 1;
                float ftone = 1789772 / (8.0f * (float) tp);
                channel.note = searchSSGNote(ftone);
            }
        }
    }

    @Override
    public void drawScreenParams() {
        //int tp = setting.S5BType.UseScci ? 1 : 0;
        int tp = 0;

        for (int c = 0; c < 3; c++) {

            Channel oyc = oldParam.channels[c];
            Channel nyc = newParam.channels[c];

            oyc.volume = frameBuffer.drawVolumeM(256, 8 + c * 8, 0, oyc.volume, nyc.volume, tp);
            oyc.note = frameBuffer.drawKeyBoard(c, oyc.note, nyc.note, tp);
            int[] r = drawToneNoise(frameBuffer, 6, 2, c, oyc.tn, nyc.tn, oyc.tntp, tp);
            oyc.tn = r[0]; oyc.tntp = r[1];

            oyc.mask = drawChS5B(frameBuffer, c, oyc.mask, nyc.mask, tp);
        }

        oldParam.nfrq = frameBuffer.Nfrq(5, 8, oldParam.nfrq, newParam.nfrq);
        oldParam.efrq = frameBuffer.drawEfrq(18, 8, oldParam.efrq, newParam.efrq);
        oldParam.etype = frameBuffer.drawEType(33, 8, oldParam.etype, newParam.etype);
    }

    @Override
    public void initScreen() {
        for (int c = 0; c < newParam.channels.length; c++) {
            newParam.channels[c].note = -1;
            newParam.channels[c].volume = -1;
            newParam.channels[c].tn = -1;
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                frameBuffer.drawKbn(32 + kx, c * 8 + 8, kt, 0);
            }
        }
        newParam.nfrq = 0;
        newParam.efrq = 0;
        newParam.etype = 0;
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
                    for (int ch = 0; ch < 3; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(Fme7Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(Fme7Chip.class, chipId, ch);
                    }
                }
                return;
            }

            // keyboard
            if (py < 4 * 8) {
                int ch = (py / 8) - 1;
                if (ch < 0) return;

                if (ev.getButton() == MouseEvent.BUTTON1) {
                    // Mask.
                    parent.setChannelMask(Fme7Chip.class, chipId, ch);
                    return;
                }

                // Unmask.
                for (ch = 0; ch < 3; ch++) parent.resetChannelMask(Fme7Chip.class, chipId, ch);
            }
        }
    };

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeS5B");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 40));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmS5B
        //
        this.setPreferredSize(new Dimension(320, 40));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmS5B");
        this.setTitle("S5B(FME)");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static Boolean drawChS5B(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChS5B_P(screen, 0, 8 + ch * 8, ch, nm != null && nm, tp);
        om = nm;
        return om;
    }

    private static int[] drawToneNoise(FrameBuffer screen, int x, int y, int c, int ot, int nt, int otp, int ntp) {
        if (ot == nt && otp == ntp) {
            return new int[] {ot, otp};
        }

        drawToneNoiseP(screen, x * 4, y * 4 + c * 8, nt, ntp);
        ot = nt;
        otp = ntp;
        return new int[] {ot, otp};
    }

    private static void drawToneNoiseP(FrameBuffer screen, int x, int y, int t, int tp) {
        if (screen == null)
            return;
        screen.drawByteArray(x, y, FrameBuffer.rPSGMode[tp], 32, 8 * t, 0, 8, 8);
    }

    private static void drawChS5B_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 32, 0, 16, 8);
        screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(1 + ch));
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        int tn = 0;
        int tntp = -1;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        int nfrq = -1;
        int efrq = -1;
        int etype = -1;
        final Channel[] channels = {new Channel(), new Channel(), new Channel()};
    }


    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "S5B"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return NpNesChip.Fme7Chip.class; }
        @Override public String menuText() { return "S5B(FME7)"; }
        @Override public String category() { return "nes"; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormS5B(frm, chipId, zoom); }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(55, mdsound.MDSound.Chip.MAIN_TAG, NpNesChip.Fme7Chip.class, "FME7", 50));
        }

        @Override public void updateMeters(Audio audio, VisVolume visVolume) {
            int fme7 = Meters.npNesVolume(audio, 6);
            if (fme7 >= 0) visVolume.put("FME7", fme7 * 15);
        }
    }
}
