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
import mdplayer.chips.NpNesChip.Vrc6Chip;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.form.View;
import mdplayer.form.VisVolume;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.Meters;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;
import mdsound.np.chip.DeviceInfo.BasicTrackInfo;


public class FormVRC6 extends FormChipBase<FormVRC6.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormVRC6.class);

    public FormVRC6(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        bind(Common.getImage("planeVRC6"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("VRC6", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeVRC6").getWidth() * zoom, frameSizeH + Common.getImage("planeVRC6").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeVRC6").getWidth() * zoom, frameSizeH + Common.getImage("planeVRC6").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeVRC6").getWidth() * zoom, frameSizeH + Common.getImage("planeVRC6").getHeight() * zoom));
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
                    for (ch = 0; ch < 3; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(Vrc6Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(Vrc6Chip.class, chipId, ch);
                    }
                }
                return;
            }

            if (ev.getButton() == MouseEvent.BUTTON2) {
                for (int i = 0; i < 3; i++) {
                    // Unmask.
                    parent.resetChannelMask(Vrc6Chip.class, chipId, i);
                }

                return;
            }

            ch = (py / 8) - 1;
            if (ch < 0) return;
            ch /= 2;
            if (ev.getButton() == MouseEvent.BUTTON1) {
                // Mask.
                parent.setChannelMask(Vrc6Chip.class, chipId, ch);
            }
        }
    };

    @Override
    public void initScreen() {
        boolean VRC6Type = false;
        int tp = VRC6Type ? 1 : 0;
        for (int ch = 0; ch < 3; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                frameBuffer.drawKbn(32 + kx, ch * 16 + 8, kt, tp);
            }
        }
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(NpNesChip.Vrc6Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        BasicTrackInfo[] trackInfo = (BasicTrackInfo[]) info.get("tracksInfo");

        for (int ch = 0; ch < 3; ch++) {
            Channel nyc = newParam.channels[ch];
            nyc.kf = trackInfo[ch].getTone();
            nyc.volumeR = trackInfo[ch].getTone() / 4;
            nyc.volumeL = trackInfo[ch].getVolume();
            int v = trackInfo[ch].getVolume();
            v = ch < 2 ? v * 2 : v / 3;
            nyc.volume = Math.min(v, 19);
            nyc.bit[0] = trackInfo[ch].getKeyStatus();
            nyc.freq = trackInfo[ch].getFreqP();
            nyc.bit[1] = trackInfo[ch].getHalt();
            v = trackInfo[ch].getNote(trackInfo[ch].getFreqHz()) - 4 * 12;
            nyc.note = nyc.volumeL == 0 ? -1 : v;
            nyc.sadr = trackInfo[ch].getFreqShift();
        }
    
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(NpNesChip.Vrc6Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        Channel oyc;
        Channel nyc;

        for (int ch = 0; ch < 3; ch++) {
            oyc = oldParam.channels[ch];
            nyc = newParam.channels[ch];

            oyc.note = frameBuffer.drawKeyBoard(ch * 2, oyc.note, nyc.note, 0);
            if (ch < 2) {
                oyc.volumeR = frameBuffer.drawDuty(24, (1 + ch * 2) * 8, oyc.volumeR, nyc.volumeR);
                oyc.kf = frameBuffer.font4Int2(6 * 4, ch * 16 + 16, 0, 2, oyc.kf, nyc.kf);
                oyc.volumeL = frameBuffer.font4Int2(10 * 4, ch * 16 + 16, 0, 2, oyc.volumeL, nyc.volumeL);
                oyc.volume = frameBuffer.drawVolumeM(256, 8 + ch * 2 * 8, 0, oyc.volume, nyc.volume, 0);
                oldParam.channels[ch].mask = drawChVRC6(frameBuffer, ch, oldParam.channels[ch].mask, newParam.channels[ch].mask, 0);
            } else {
                oyc.volumeL = frameBuffer.font4Int2(9 * 4, ch * 16 + 16, 0, 3, oyc.volumeL, nyc.volumeL);
                oyc.volume = frameBuffer.drawVolumeM(256, 8 + ch * 2 * 8, 0, oyc.volume, nyc.volume, 0);
                frameBuffer.drawNESSw(55 * 4, ch * 16 + 16,
                        oldParam.channels[ch].bit[1], newParam.channels[ch].bit[1]);
                oyc.sadr = frameBuffer.font4Int1(62 * 4, ch * 16 + 16, 0, oyc.sadr, nyc.sadr);
                oldParam.channels[ch].mask = drawChVRC6(frameBuffer, ch, oldParam.channels[ch].mask, newParam.channels[ch].mask, 0);
            }

            frameBuffer.drawNESSw(13 * 4, ch * 16 + 16,
                    oldParam.channels[ch].bit[0], newParam.channels[ch].bit[0]);

            oyc.freq = frameBuffer.font4Hex12Bit(16 * 4, ch * 16 + 16, 0, oyc.freq, nyc.freq);
        }
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeVRC6");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 56));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmVRC6
        //
        this.setPreferredSize(new Dimension(320, 56));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmVRC6");
        this.setTitle("Vrc6Inst");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static Boolean drawChVRC6(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChVRC6_P(screen, ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static void drawChVRC6_P(FrameBuffer screen, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        switch (ch) {
            case 0:
                screen.drawByteArray(0, 8, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 48, 8, 16, 8);
                screen.drawFont8(16, 8, mask ? 1 : 0, "1");
                break;
            case 1:
                screen.drawByteArray(0, 24, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 48, 8, 16, 8);
                screen.drawFont8(16, 24, mask ? 1 : 0, "2");
                break;
            case 2:
                screen.drawByteArray(0, 40, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 16, 16, 16, 8);
                break;
        }
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        int kf = -1;
        int sadr = -1;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final Channel[] channels = {
                new Channel(), new Channel(), new Channel()
        };
    }


    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "VRC6"; }
        @Override public String category() { return "nes"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return NpNesChip.Vrc6Chip.class; }
        @Override public String title(int chipId) { return "Vrc6Inst (%s)".formatted(chipId == 0 ? "Primary" : "Secondary"); }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormVRC6(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 3) {
                NpNesChip.Vrc6Chip c = audio.plugin.chipRegister.chip(NpNesChip.Vrc6Chip.class);
                if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(NpNesChip.Vrc6Chip.class).resetMask(chipId, ch);
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (mask)
                audio.plugin.chipRegister.chip(NpNesChip.Vrc6Chip.class).setMask(chipId, ch);
            else
                audio.plugin.chipRegister.chip(NpNesChip.Vrc6Chip.class).resetMask(chipId, ch);
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 3; ch++)
                forceChannelMask(audio, NpNesChip.Vrc6Chip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(NpNesChip.Vrc6Chip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(53, MDSound.Chip.MAIN_TAG, NpNesChip.Vrc6Chip.class, "VRC6", 50));
        }

        @Override public void updateMeters(Audio audio, VisVolume visVolume) {
            int vrc6 = Meters.npNesVolume(audio, 4);
            if (vrc6 >= 0) visVolume.put("VRC6", vrc6 * 15);
        }
    }
}
