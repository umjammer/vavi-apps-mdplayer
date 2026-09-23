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
import mdplayer.chips.NpNesChip;
import mdplayer.chips.NpNesChip.Mmc5Chip;
import mdplayer.form.VisVolume;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.Meters;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdplayer.form.View;
import mdsound.MDSound;


public class FormMMC5 extends FormChipBase<FormMMC5.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormMMC5.class);

    public FormMMC5(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeMMC5"), null, zoom);
        drawScreenInitNESDMC(frameBuffer);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("MMC5", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeMMC5").getWidth() * zoom, frameSizeH + Common.getImage("planeMMC5").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeMMC5").getWidth() * zoom, frameSizeH + Common.getImage("planeMMC5").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeMMC5").getWidth() * zoom, frameSizeH + Common.getImage("planeMMC5").getHeight() * zoom));
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
        Map<String, Object> info = audio.plugin.chipRegister.chip(Mmc5Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        byte[] reg = (byte[]) info.get("register");
        if (reg != null) {
            for (int i = 0; i < 2; i++) {
                int freq = (reg[3 + i * 4] & 0x07) * 0x100 + reg[2 + i * 4];
                int vol = reg[i * 4] & 0xf;
                int note = 104 - (int) ((12 * (Math.log(freq) / LOG_2 - LOG2_440) + NOTE_440HZ + 0.5));
                note = vol == 0 ? -1 : note;
                newParam.sqrChannels[i].note = note;
                newParam.sqrChannels[i].volume = Math.min((int) ((vol) * 1.33), 19);
                newParam.sqrChannels[i].pantp = (reg[3 + i * 4] & 0xf8) >> 3; // Length counter load
                newParam.sqrChannels[i].kf = (reg[i * 4] & 0xc0) >> 6; // Duty
                newParam.sqrChannels[i].dda = ((reg[i * 4] & 0x20) >> 5) != 0; // LengthCounter
                newParam.sqrChannels[i].noise = ((reg[i * 4] & 0x10) >> 4) != 0; // constantVolume
            }

            newParam.pcmChannel.dda = (reg[8] & 0x80) != 0;
            newParam.pcmChannel.noise = (reg[8] & 0x01) != 0;
            newParam.pcmChannel.note = (reg[9] & 0xff);
            newParam.pcmChannel.volume = (reg[9] & 0xff) >> 3;
            newParam.pcmChannel.volume = Math.min(newParam.pcmChannel.volume, 19);
        }
    
        newParam.sqrChannels[0].mask = audio.plugin.chipRegister.chip(NpNesChip.Mmc5Chip.class).getMask(chipId, 0);
        newParam.sqrChannels[1].mask = audio.plugin.chipRegister.chip(NpNesChip.Mmc5Chip.class).getMask(chipId, 1);
        newParam.pcmChannel.mask = audio.plugin.chipRegister.chip(NpNesChip.Mmc5Chip.class).getMask(chipId, 2);
    }

    @Override
    public void drawScreenParams() {
        for (int i = 0; i < 2; i++) {
            oldParam.sqrChannels[i].note = frameBuffer.drawKeyBoard(i * 2, oldParam.sqrChannels[i].note, newParam.sqrChannels[i].note, 0);
            oldParam.sqrChannels[i].volume = frameBuffer.drawVolumeM(256, 8 + i * 2 * 8, 0, oldParam.sqrChannels[i].volume, newParam.sqrChannels[i].volume, 0);
            oldParam.sqrChannels[i].pantp = frameBuffer.font4Int2(22 * 4, (2 + i * 2) * 8, 0, 2, oldParam.sqrChannels[i].pantp, newParam.sqrChannels[i].pantp);
            oldParam.sqrChannels[i].kf = frameBuffer.drawDuty(24, (1 + i * 2) * 8, oldParam.sqrChannels[i].kf, newParam.sqrChannels[i].kf);
            oldParam.sqrChannels[i].dda = frameBuffer.drawNESSw(32, (2 + i * 2) * 8, oldParam.sqrChannels[i].dda, newParam.sqrChannels[i].dda);
            oldParam.sqrChannels[i].noise = frameBuffer.drawNESSw(40, (2 + i * 2) * 8, oldParam.sqrChannels[i].noise, newParam.sqrChannels[i].noise);
            oldParam.sqrChannels[i].mask = drawChMMC5(frameBuffer, i, oldParam.sqrChannels[i].mask, newParam.sqrChannels[i].mask, 0);
        }

        oldParam.pcmChannel.volume = frameBuffer.drawVolumeM(256, 8 + 3 * 8, 0, oldParam.pcmChannel.volume, newParam.pcmChannel.volume, 0);
        oldParam.pcmChannel.dda = frameBuffer.drawNESSw(148, 32, oldParam.pcmChannel.dda, newParam.pcmChannel.dda);
        oldParam.pcmChannel.noise = frameBuffer.drawNESSw(160, 32, oldParam.pcmChannel.noise, newParam.pcmChannel.noise);
        oldParam.pcmChannel.note = frameBuffer.font4HexByte(196, 32, 0, oldParam.pcmChannel.note, newParam.pcmChannel.note);
        oldParam.pcmChannel.mask = drawChMMC5(frameBuffer, 2, oldParam.pcmChannel.mask, newParam.pcmChannel.mask, 0);
    }

    @Override
    public void initScreen() {
        for (int c = 0; c < newParam.sqrChannels.length; c++) {
            newParam.sqrChannels[c].note = -1;
            newParam.sqrChannels[c].volume = 0;
            newParam.sqrChannels[c].pantp = 0;
            newParam.sqrChannels[c].kf = 0;
            newParam.sqrChannels[c].dda = false;
            newParam.sqrChannels[c].noise = false;
        }
        newParam.pcmChannel.dda = false;
        newParam.pcmChannel.noise = false;
        newParam.pcmChannel.note = -1;
        newParam.pcmChannel.volume = 0;
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
                    for (int ch = 0; ch < 2; ch++) {
                        if (newParam.sqrChannels[ch].mask)
                            parent.resetChannelMask(Mmc5Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(Mmc5Chip.class, chipId, ch);
                    }

                    if (newParam.pcmChannel.mask)
                        parent.resetChannelMask(Mmc5Chip.class, chipId, 0);
                    else
                        parent.setChannelMask(Mmc5Chip.class, chipId, 0);

                }
                return;
            }

            // keyboard
            if (py < 5 * 8) {
                if (ev.getButton() == MouseEvent.BUTTON2) {
                    for (int i = 0; i < 3; i++) {
                        // unmask
                        if (i < 3) parent.resetChannelMask(Mmc5Chip.class, chipId, i);
                    }

                    return;
                }

                int ch = (py / 8) - 1;

                if (ev.getButton() == MouseEvent.BUTTON1) {
                    switch (ch) {
                        case 0:
                            ch = 0;
                            break;
                        case 2:
                            ch = 1;
                            break;
                        case 3:
                            ch = 2;
                            break;
                        default:
                            return;
                    }
                    // mask
                    parent.setChannelMask(Mmc5Chip.class, chipId, ch);

                }
            }
        }
    };

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeMMC5");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(286, 40));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmMMC5
        //
        this.setPreferredSize(new Dimension(286, 40));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmMMC5");
        this.setTitle("MMC5");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static void drawScreenInitNESDMC(FrameBuffer screen) {
        if (screen == null)
            return;

        for (int ch = 0; ch < 3; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                screen.drawKbn(32 + kx, ch * 16 + 8, kt, 0);
            }
            screen.drawFont8(296, ch * 16 + 8, 1, "   ");
            boolean m = true;
            m = drawChNESDMC(screen, ch, m, false, 0);
        }
    }

    private static Boolean drawChNESDMC(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChNESDMC_P(screen, ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static Boolean drawChMMC5(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChMMC5_P(screen, ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static void drawChNESDMC_P(FrameBuffer screen, int ch, boolean mask, int tp) {
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
                screen.drawByteArray(0, 40, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 64, 8, 32, 8);
                break;
            case 3:
                screen.drawByteArray(112, 32, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 96, 8, 24, 8);
                break;
            case 4:
                screen.drawByteArray(112, 48, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 16, 16, 8);
                break;
        }
    }

    private static void drawChMMC5_P(FrameBuffer screen, int ch, boolean mask, int tp) {
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
                screen.drawByteArray(112, 32, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 16, 0, 16, 8);
                break;
        }
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        boolean dda = false;
        int kf = -1;
        boolean noise = false;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final Channel[] sqrChannels = {new Channel(), new Channel()};
        final Channel pcmChannel = new Channel();
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "MMC5"; }
        @Override public String category() { return "nes"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return NpNesChip.Mmc5Chip.class; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormMMC5(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 3) {
                NpNesChip.Mmc5Chip c = audio.plugin.chipRegister.chip(NpNesChip.Mmc5Chip.class);
                if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(NpNesChip.Mmc5Chip.class).resetMask(chipId, ch);
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 3; ch++)
                resetChannelMask(audio, NpNesChip.Mmc5Chip.class, chipId, ch);
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(51, MDSound.Chip.MAIN_TAG, NpNesChip.Mmc5Chip.class, "MMC5", 50));
        }

        @Override public void updateMeters(Audio audio, VisVolume visVolume) {
            int mmc5 = Meters.npNesVolume(audio, 5);
            if (mmc5 >= 0) visVolume.put("MMC5", mmc5 * 15);
        }
    }
}
