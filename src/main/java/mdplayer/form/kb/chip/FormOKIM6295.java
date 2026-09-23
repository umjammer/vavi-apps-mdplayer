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
import mdplayer.chips.OkiM6295Chip;
import mdplayer.form.kb.PcmChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdplayer.form.View;
import mdsound.MDSound;


public class FormOKIM6295 extends FormChipBase<FormOKIM6295.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormOKIM6295.class);

    public FormOKIM6295(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeMSM6295"), null, zoom);
        drawScreenInitOKIM6295(frameBuffer);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("OKIM6295", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeMSM6295").getWidth() * zoom, frameSizeH + Common.getImage("planeMSM6295").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeMSM6295").getWidth() * zoom, frameSizeH + Common.getImage("planeMSM6295").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeMSM6295").getWidth() * zoom, frameSizeH + Common.getImage("planeMSM6295").getHeight() * zoom));
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
        Map<String, Object> info = audio.plugin.chipRegister.chip(OkiM6295Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        for (int c = 0; c < 4; c++) {
            PcmChannelParams nyc = newParam.channels[c];

            if ((boolean) info.get("channels." + c + ".keyon")) {
                nyc.volume = 19;
            } else {
                nyc.volume -= (nyc.volume > 0) ? 1 : 0;
            }
            nyc.sadr = (int) info.get("channels." + c + ".sadr");
            nyc.eadr = (int) info.get("channels." + c + ".eadr");
        }

        newParam.masterClock = (int) info.get("masterClock");
        newParam.pin7State = (int) info.get("pin7State");
        newParam.nmkBank[0] = (int) info.get("nmkBank.0");
        newParam.nmkBank[1] = (int) info.get("nmkBank.1");
        newParam.nmkBank[2] = (int) info.get("nmkBank.2");
        newParam.nmkBank[3] = (int) info.get("nmkBank.3");
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(OkiM6295Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        int tp = parent.setting.getHuC6280Type()[0].getUseReal()[0] ? 1 : 0;

        for (int c = 0; c < 4; c++) {
            PcmChannelParams oyc = oldParam.channels[c];
            PcmChannelParams nyc = newParam.channels[c];

            oyc.mask = drawChOKIM6295(frameBuffer, c, oyc.mask, nyc.mask, tp);
            oyc.volume = drawVolumeToOKIM6295(frameBuffer, c, oyc.volume, nyc.volume);
            oyc.sadr = frameBuffer.font4Hex20Bit(36, 8 + c * 8, 0, oyc.sadr, nyc.sadr);
            oyc.eadr = frameBuffer.font4Hex20Bit(60, 8 + c * 8, 0, oyc.eadr, nyc.eadr);

            oldParam.nmkBank[c] = frameBuffer.font4HexByte(36 + c * 16, 48, 0, oldParam.nmkBank[c], newParam.nmkBank[c]);
        }

        oldParam.masterClock = drawFont4Hex32Bit(frameBuffer, 24, 40, 0, oldParam.masterClock, newParam.masterClock);
        oldParam.pin7State = frameBuffer.font4HexByte(80, 40, 0, oldParam.pin7State, newParam.pin7State);
    }

    @Override
    public void initScreen() {
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
                    for (int ch = 0; ch < 4; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(OkiM6295Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(OkiM6295Chip.class, chipId, ch);
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
                    parent.setChannelMask(OkiM6295Chip.class, chipId, ch);
                    return;
                }

                // Unmask.
                for (ch = 0; ch < 4; ch++) parent.resetChannelMask(OkiM6295Chip.class, chipId, ch);
            }
        }
    };

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeMSM6295");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 40));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmOKIM6295
        //
        this.setPreferredSize(new Dimension(320, 40));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmOKIM6295");
        this.setTitle("OKIM6295");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static void drawScreenInitOKIM6295(FrameBuffer screen) {
    }

    private static int drawVolumeToOKIM6295(FrameBuffer screen, int y, /* ref */ int ov, int nv) {
        if (ov == nv)
            return ov;

        int t = 0;
        int sy = 0;
        y = (y + 1) * 8;

        for (int i = 0; i <= 19; i++) {
            screen.drawVolumeP(80 + i * 2, y + sy, (1 + t), 0);
        }

        for (int i = 0; i <= nv; i++) {
            screen.drawVolumeP(80 + i * 2, y + sy, i > 17 ? (2 + t) : (0 + t), 0);
        }

        ov = nv;
        return ov;
    }

    private static boolean drawChOKIM6295(FrameBuffer screen, int ch, /* ref */ Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChOKIM6295_P(screen, 0, 8 + ch * 8, ch, nm != null && nm, tp);
        om = nm;
        return om;
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

    private static void drawChOKIM6295_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 64, 0, 24, 8);
        screen.drawFont8(x + 24, y, mask ? 1 : 0, String.valueOf(1 + ch));
    }

//#endregion

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final PcmChannelParams[] channels = {new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams()};

        int masterClock = 0;
        int pin7State = 0;
        final int[] nmkBank = new int[4];
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "OKIM6295"; }
        @Override public String category() { return "pcm"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return OkiM6295Chip.class; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormOKIM6295(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            OkiM6295Chip c = audio.plugin.chipRegister.chip(OkiM6295Chip.class);
            if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(OkiM6295Chip.class).resetMask(chipId, ch);
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (mask)
                audio.plugin.chipRegister.chip(OkiM6295Chip.class).setMask(chipId, ch);
            else
                audio.plugin.chipRegister.chip(OkiM6295Chip.class).resetMask(chipId, ch);
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 4; ch++)
                forceChannelMask(audio, OkiM6295Chip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(OkiM6295Chip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(38, MDSound.Chip.MAIN_TAG, OkiM6295Chip.class, "okim6295", 200));
        }
    }
}
