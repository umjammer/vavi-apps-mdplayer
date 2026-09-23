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
import mdplayer.chips.SegaPcmChip;
import mdplayer.form.VisVolume;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.Meters;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdplayer.form.View;
import mdsound.MDSound;


public class FormSegaPCM extends FormChipBase<FormSegaPCM.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormSegaPCM.class);

    public FormSegaPCM(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        bind(Common.getImage("planeSEGAPCM"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("SegaPCM", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeSEGAPCM").getWidth() * zoom, frameSizeH + Common.getImage("planeSEGAPCM").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeSEGAPCM").getWidth() * zoom, frameSizeH + Common.getImage("planeSEGAPCM").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeSEGAPCM").getWidth() * zoom, frameSizeH + Common.getImage("planeSEGAPCM").getHeight() * zoom));
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
            // For top label row, do nothing
            if (py < 1 * 8) {
                // However, if you click on ch, the mask will be inverted.
                if (px < 8) {
                    for (ch = 0; ch < 16; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(SegaPcmChip.class, chipId, ch);
                        else
                            parent.setChannelMask(SegaPcmChip.class, chipId, ch);
                    }
                }
                return;
            }

            ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ch < 16) {
                if (ev.getButton() == MouseEvent.BUTTON1) {
                    parent.setChannelMask(SegaPcmChip.class, chipId, ch);
                    return;
                }

                for (ch = 0; ch < 16; ch++) parent.resetChannelMask(SegaPcmChip.class, chipId, ch);
            }
        }
    };

    @Override
    public void initScreen() {
        boolean SEGAPCMType = (chipId == 0) ? parent.setting.getSEGAPCMType()[0].getUseReal()[0] : parent.setting.getSEGAPCMType()[1].getUseReal()[0];
        int tp = SEGAPCMType ? 1 : 0;
        for (int ch = 0; ch < 16; ch++) {
            int o = -1;
            o = frameBuffer.drawVolumeM(256, 8 + ch * 8, 1, o, 0, tp);
            o = -1;
            o = frameBuffer.drawVolumeM(256, 8 + ch * 8, 2, o, 0, tp);
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                frameBuffer.drawKbn(32 + kx, ch * 8 + 8, kt, tp);
            }
            frameBuffer.drawFont8(296, ch * 8 + 8, 1, "   ");
            frameBuffer.drawPanType2P(24, ch * 8 + 8, 0, tp);
            drawChSegaPCM_P(frameBuffer, 0, 8 + ch * 8, ch, false, tp);
        }
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(SegaPcmChip.class).getInfo(chipId);
        if (info.isEmpty()) return; // the song being played does not use this chip

        byte[] register = (byte[]) info.get("register");

        for (int ch = 0; ch < 16; ch++) {
            int l = register[ch * 8 + 2] & 0x7f;
            int r = register[ch * 8 + 3] & 0x7f;
            int dt = register[ch * 8 + 7] & 0xff;
            int ctrl = register[ch * 8 + 0x86] & 0xff;
            double ml = dt / 256.0;

            // the chip has no key on of its own to read, so a sounding channel is one
            boolean playing = (ctrl & 0x01) == 0 && dt > 0 && (l | r) != 0;

            if (playing) {
                newParam.channels[ch].note = SegaPcmChip.searchSegaPCMNote(ml);
                newParam.channels[ch].volumeL = Math.clamp((l * 1) >> 1, 0, 19);
                newParam.channels[ch].volumeR = Math.clamp((r * 1) >> 1, 0, 19);
            } else {
                newParam.channels[ch].volumeL -= newParam.channels[ch].volumeL > 0 ? 1 : 0;
                newParam.channels[ch].volumeR -= newParam.channels[ch].volumeR > 0 ? 1 : 0;

                if (newParam.channels[ch].volumeL == 0 && newParam.channels[ch].volumeR == 0) {
                    newParam.channels[ch].note = -1;
                }
            }

            newParam.channels[ch].pan = ((l >> 3) & 0xf) | (((r >> 3) & 0xf) << 4);
        }

        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(SegaPcmChip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        int tp = ((chipId == 0) ? parent.setting.getSEGAPCMType()[0].getUseReal()[0] : parent.setting.getSEGAPCMType()[1].getUseReal()[0]) ? 1 : 0;

        for (int c = 0; c < 16; c++) {

            ChannelParams orc = oldParam.channels[c];
            ChannelParams nrc = newParam.channels[c];

            orc.volumeL = frameBuffer.drawVolumeM(256, 8 + c * 8, 1, orc.volumeL, nrc.volumeL, tp);
            orc.volumeR = frameBuffer.drawVolumeM(256, 8 + c * 8, 2, orc.volumeR, nrc.volumeR, tp);
            orc.note = frameBuffer.drawKeyBoard(c, orc.note, nrc.note, tp);
            orc.pan = frameBuffer.PanType2(c, orc.pan, nrc.pan, tp);

            orc.mask = drawChSegaPCM(frameBuffer, c, orc.mask, nrc.mask, tp);
        }
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeSEGAPCM");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 136));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmSegaPCM
        //
        this.setPreferredSize(new Dimension(320, 136));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmSegaPCM");
        this.setTitle("SegaPCM");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static Boolean drawChSegaPCM(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChSegaPCM_P(screen, 0, 8 + ch * 8, ch, nm != null && nm, tp);
        om = nm;
        return om;
    }

    private static void drawChSegaPCM_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 16, 0, 16, 8);
        //if (ch < 9) screen.drawFont8(x + 16, y, mask ? 1 : 0, (1 + ch).toString());
        //else
        screen.drawFont4(x + 16, y, mask ? 1 : 0, "%2d".formatted(1 + ch));
    }

//#endregion

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final ChannelParams[] channels = {
                new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams(),
                new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams(),
                new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams(),
                new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams()
        };
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "SegaPCM"; }
        @Override public String menuText() { return "SEGA PCM"; }
        @Override public String category() { return "pcm"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return SegaPcmChip.class; }
        @Override public boolean hasRegisterDump() { return true; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormSegaPCM(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            SegaPcmChip c = audio.plugin.chipRegister.chip(SegaPcmChip.class);
            if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 16) {
                audio.plugin.chipRegister.chip(SegaPcmChip.class).resetMask(chipId, ch);
            }
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (mask)
                audio.plugin.chipRegister.chip(SegaPcmChip.class).setMask(chipId, ch);
            else
                audio.plugin.chipRegister.chip(SegaPcmChip.class).resetMask(chipId, ch);
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 16; ch++)
                forceChannelMask(audio, SegaPcmChip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(SegaPcmChip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(41, MDSound.Chip.MAIN_TAG, SegaPcmChip.class, "segaPCM", 200));
        }

        @Override public void updateMeters(Audio audio, VisVolume visVolume) {
            int val = 0;
            try {
                byte[] reg = (byte[]) Meters.chipInfo(audio, SegaPcmChip.class, "register");
                if (reg != null) {
                    for (int ch = 0; ch < 16; ch++) {
                        int v = 0;
                        if ((reg[0x86 + ch * 8] & 1) == 0) {
                            int l = reg[ch * 8 + 2] & 0x7f;
                            int r = reg[ch * 8 + 3] & 0x7f;
                            v = Math.max(l, r) * 70;
                        }
                        if (v > val) val = v;
                    }
                }
            } catch (Exception _) {
            }
            visVolume.put("segaPCM", val * 3);
        }
    }
}
