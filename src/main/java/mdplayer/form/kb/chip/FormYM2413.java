package mdplayer.form.kb.chip;

import java.awt.Component;
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
import mdplayer.Chip.ChipKeyInfo;
import mdplayer.Common;
import mdplayer.Common.EnmInstFormat;
import mdplayer.Setting;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.Tables;
import mdplayer.chips.SegaPcmChip;
import mdplayer.chips.Ym2413Chip;
import mdplayer.form.inst.MgscInstWriter;
import mdplayer.form.inst.SendMml2vgmInstWriter;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdplayer.form.View;


public class FormYM2413 extends FormChipBase<FormYM2413.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormYM2413.class);

    public FormYM2413(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());
        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeYM2413"), null, zoom);

        boolean YM2413Type = (chipId == 0)
                ? parent.setting.getYM2413Type()[0].getUseReal()[0]
                : parent.setting.getYM2413Type()[1].getUseReal()[0];
        int YM2413SoundLocation = (chipId == 0)
                ? parent.setting.getYM2413Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYM2413Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !YM2413Type ? 0 : (YM2413SoundLocation < 0 ? 2 : 1);

        screenInitYM2413(frameBuffer, tp);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("YM2413", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeYM2413").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2413").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeYM2413").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2413").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeYM2413").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2413").getHeight() * zoom));
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
        Map<String, Object> info = audio.plugin.chipRegister.chip(Ym2413Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int[] register = (int[]) info.get("register");
        ChipKeyInfo ki = (ChipKeyInfo) info.get("keyInfo");

        for (int ch = 0; ch < 9; ch++) {
            ChannelParams nyc = newParam.channels[ch];

            nyc.inst[0] = (register[0x30 + ch] & 0xf0) >> 4;
            nyc.inst[1] = (register[0x20 + ch] & 0x20) >> 5;
            nyc.inst[2] = (register[0x20 + ch] & 0x10) >> 4;
            nyc.inst[3] = (register[0x30 + ch] & 0x0f);

            int freq = register[0x10 + ch] + ((register[0x20 + ch] & 0x1) << 8);
            int oct = ((register[0x20 + ch] & 0xe) >> 1);

            nyc.note = SegaPcmChip.searchSegaPCMNote(freq / 172.0) + (oct - 4) * 12;

            if (ki.on[ch]) {
                nyc.volumeL = (19 - nyc.inst[3]);
            } else {
                if (nyc.inst[2] == 0) nyc.note = -1;
                nyc.volumeL--;
                if (nyc.volumeL < 0) nyc.volumeL = 0;
            }
        }

        //int r = audio.plugin.chipRegister.chip(Ym2413Chip.class).getRhythmKeyON(chipId);

        // BD
        if (ki.on[9]) {
            newParam.channels[9].volume = (19 - (register[0x36] & 0x0f));
        } else {
            newParam.channels[9].volume--;
            if (newParam.channels[9].volume < 0) newParam.channels[9].volume = 0;
        }

        // SD
        if (ki.on[10]) {
            newParam.channels[10].volume = (19 - (register[0x37] & 0x0f));
        } else {
            newParam.channels[10].volume--;
            if (newParam.channels[10].volume < 0) newParam.channels[10].volume = 0;
        }

        // TOM
        if (ki.on[11]) {
            newParam.channels[11].volume = 19 - ((register[0x38] & 0xf0) >> 4);
        } else {
            newParam.channels[11].volume--;
            if (newParam.channels[11].volume < 0) newParam.channels[11].volume = 0;
        }

        // CYM
        if (ki.on[12]) {
            newParam.channels[12].volume = 19 - (register[0x38] & 0x0f);
        } else {
            newParam.channels[12].volume--;
            if (newParam.channels[12].volume < 0) newParam.channels[12].volume = 0;
        }

        // HH
        if (ki.on[13]) {
            newParam.channels[13].volume = 19 - ((register[0x37] & 0xf0) >> 4);
        } else {
            newParam.channels[13].volume--;
            if (newParam.channels[13].volume < 0) newParam.channels[13].volume = 0;
        }

        newParam.channels[0].inst[4] = (register[0x02] & 0x3f); // TL
        newParam.channels[0].inst[5] = (register[0x03] & 0x07); // FB

        newParam.channels[0].inst[6] = (register[0x04] & 0xf0) >> 4;  // AR
        newParam.channels[0].inst[7] = (register[0x04] & 0x0f);       // DR
        newParam.channels[0].inst[8] = (register[0x06] & 0xf0) >> 4;  // SL
        newParam.channels[0].inst[9] = (register[0x06] & 0x0f);       // RR
        newParam.channels[0].inst[10] = (register[0x02] & 0x80) >> 7; // KL
        newParam.channels[0].inst[11] = (register[0x00] & 0x0f);      // MT
        newParam.channels[0].inst[12] = (register[0x00] & 0x80) >> 7; // AM
        newParam.channels[0].inst[13] = (register[0x00] & 0x40) >> 6; // VB
        newParam.channels[0].inst[14] = (register[0x00] & 0x20) >> 5; // EG
        newParam.channels[0].inst[15] = (register[0x00] & 0x10) >> 4; // KR
        newParam.channels[0].inst[16] = (register[0x03] & 0x08) >> 3; // DM
        newParam.channels[0].inst[17] = (register[0x05] & 0xf0) >> 4; // AR
        newParam.channels[0].inst[18] = (register[0x05] & 0x0f);      // DR
        newParam.channels[0].inst[19] = (register[0x07] & 0xf0) >> 4; // SL
        newParam.channels[0].inst[20] = (register[0x07] & 0x0f);      // RR
        newParam.channels[0].inst[21] = (register[0x03] & 0x80) >> 7; // KL
        newParam.channels[0].inst[22] = (register[0x01] & 0x0f);      // MT
        newParam.channels[0].inst[23] = (register[0x01] & 0x80) >> 7; // AM
        newParam.channels[0].inst[24] = (register[0x01] & 0x40) >> 6; // VB
        newParam.channels[0].inst[25] = (register[0x01] & 0x20) >> 5; // EG
        newParam.channels[0].inst[26] = (register[0x01] & 0x10) >> 4; // KR
        newParam.channels[0].inst[27] = (register[0x03] & 0x10) >> 4; // DC
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(Ym2413Chip.class).getMask(chipId, mch);
    }

    private void screenInitYM2413(FrameBuffer screen, int tp) {

        for (int y = 0; y < 9; y++) {
            // Note
            screen.drawFont8(296, y * 8 + 8, 1, "   ");

            // Keyboard
            for (int i = 0; i < 96; i++) {
                int kx = Tables.kbl[(i % 12) * 2] + i / 12 * 28;
                int kt = Tables.kbl[(i % 12) * 2 + 1];
                screen.drawKbn(32 + kx, y * 8 + 8, kt, tp);
            }

            // Volume
            int d = 99;
            d = screen.drawVolumeM(256, 8 + y * 8, 0, d, 0, tp);

            Boolean db = null;
            db = drawChYM2413(screen, y, db, newParam.channels[y].mask, tp);
        }

        Boolean dm;
        dm = null;
        dm = drawChYM2413(frameBuffer, 9, dm, newParam.channels[9].mask, tp);
        dm = null;
        dm = drawChYM2413(frameBuffer, 10, dm, newParam.channels[10].mask, tp);
        dm = null;
        dm = drawChYM2413(frameBuffer, 11, dm, newParam.channels[11].mask, tp);
        dm = null;
        dm = drawChYM2413(frameBuffer, 12, dm, newParam.channels[12].mask, tp);
        dm = null;
        dm = drawChYM2413(frameBuffer, 13, dm, newParam.channels[13].mask, tp);
        int dv;
        dv = 99;
        dv = frameBuffer.drawVolumeXY(6, 20, 0, dv, newParam.channels[9].volume, tp);
        dv = 99;
        dv = frameBuffer.drawVolumeXY(21, 20, 0, dv, newParam.channels[10].volume, tp);
        dv = 99;
        dv = frameBuffer.drawVolumeXY(36, 20, 0, dv, newParam.channels[11].volume, tp);
        dv = 99;
        dv = frameBuffer.drawVolumeXY(51, 20, 0, dv, newParam.channels[12].volume, tp);
        dv = 99;
        dv = frameBuffer.drawVolumeXY(66, 20, 0, dv, newParam.channels[13].volume, tp);
    }

    @Override
    public void drawScreenParams() {
        boolean YM2413Type = (chipId == 0)
                ? parent.setting.getYM2413Type()[0].getUseReal()[0]
                : parent.setting.getYM2413Type()[1].getUseReal()[0];
        int YM2413SoundLocation = (chipId == 0)
                ? parent.setting.getYM2413Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYM2413Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !YM2413Type ? 0 : (YM2413SoundLocation < 0 ? 2 : 1);

        ChannelParams oyc;
        ChannelParams nyc;

        for (int c = 0; c < 9; c++) {

            oyc = oldParam.channels[c];
            nyc = newParam.channels[c];

            oyc.volumeL = frameBuffer.drawVolumeM(256, 8 + c * 8, 0, oyc.volumeL, nyc.volumeL, tp);
            oyc.note = frameBuffer.drawKeyBoard(c, oyc.note, nyc.note, tp);

            oyc.inst[0] = drawInstNumber(frameBuffer, (c % 3) * 16 + 37, (c / 3) * 2 + 24, oyc.inst[0], nyc.inst[0]);
            oyc.inst[1] = frameBuffer.drawSusFlag((c % 3) * 16 + 41, (c / 3) * 2 + 24, 0, oyc.inst[1], nyc.inst[1]);
            oyc.inst[2] = frameBuffer.drawSusFlag((c % 3) * 16 + 44, (c / 3) * 2 + 24, 0, oyc.inst[2], nyc.inst[2]);
            oyc.inst[3] = drawInstNumber(frameBuffer, (c % 3) * 16 + 46, (c / 3) * 2 + 24, oyc.inst[3], nyc.inst[3]);

            oyc.mask = drawChYM2413(frameBuffer, c, oyc.mask, nyc.mask, tp);
        }

        oldParam.channels[9].mask = drawChYM2413(frameBuffer, 9, oldParam.channels[9].mask, newParam.channels[9].mask, tp);
        oldParam.channels[10].mask = drawChYM2413(frameBuffer, 10, oldParam.channels[10].mask, newParam.channels[10].mask, tp);
        oldParam.channels[11].mask = drawChYM2413(frameBuffer, 11, oldParam.channels[11].mask, newParam.channels[11].mask, tp);
        oldParam.channels[12].mask = drawChYM2413(frameBuffer, 12, oldParam.channels[12].mask, newParam.channels[12].mask, tp);
        oldParam.channels[13].mask = drawChYM2413(frameBuffer, 13, oldParam.channels[13].mask, newParam.channels[13].mask, tp);
        oldParam.channels[9].volume = frameBuffer.drawVolumeXY(6, 20, 0,  oldParam.channels[9].volume, newParam.channels[9].volume, tp);
        oldParam.channels[10].volume = frameBuffer.drawVolumeXY(21, 20, 0, oldParam.channels[10].volume, newParam.channels[10].volume, tp);
        oldParam.channels[11].volume = frameBuffer.drawVolumeXY(36, 20, 0, oldParam.channels[11].volume, newParam.channels[11].volume, tp);
        oldParam.channels[12].volume = frameBuffer.drawVolumeXY(51, 20, 0, oldParam.channels[12].volume, newParam.channels[12].volume, tp);
        oldParam.channels[13].volume = frameBuffer.drawVolumeXY(66, 20, 0, oldParam.channels[13].volume, newParam.channels[13].volume, tp);

        oyc = oldParam.channels[0];
        nyc = newParam.channels[0];
        oyc.inst[4] = drawInstNumber(frameBuffer, 9, 22, oyc.inst[4], nyc.inst[4]); // TL
        oyc.inst[5] = drawInstNumber(frameBuffer, 14, 22, oyc.inst[5], nyc.inst[5]); // FB

        for (int c = 0; c < 11; c++) {
            oyc.inst[6 + c] = drawInstNumber(frameBuffer, c * 3, 26, oyc.inst[6 + c], nyc.inst[6 + c]);
            oyc.inst[17 + c] = drawInstNumber(frameBuffer, c * 3, 28, oyc.inst[17 + c], nyc.inst[17 + c]);
        }
    }

    @Override
    public void initScreen() {
        for (int ch = 0; ch < 9; ch++) {
            newParam.channels[ch].inst[0] = 0;
            newParam.channels[ch].inst[1] = 0;
            newParam.channels[ch].inst[2] = 0;
            newParam.channels[ch].inst[3] = 0;
            newParam.channels[ch].note = -1;
            newParam.channels[ch].volumeL = 0;
        }

        newParam.channels[9].volume = 0;
        newParam.channels[10].volume = 0;
        newParam.channels[11].volume = 0;
        newParam.channels[12].volume = 0;
        newParam.channels[13].volume = 0;

        newParam.channels[0].inst[4] = 0;
        newParam.channels[0].inst[5] = 0;
        newParam.channels[0].inst[6] = 0;
        newParam.channels[0].inst[7] = 0;
        newParam.channels[0].inst[8] = 0;
        newParam.channels[0].inst[9] = 0;
        newParam.channels[0].inst[10] = 0;
        newParam.channels[0].inst[11] = 0;
        newParam.channels[0].inst[12] = 0;
        newParam.channels[0].inst[13] = 0;
        newParam.channels[0].inst[14] = 0;
        newParam.channels[0].inst[15] = 0;
        newParam.channels[0].inst[16] = 0;
        newParam.channels[0].inst[17] = 0;
        newParam.channels[0].inst[18] = 0;
        newParam.channels[0].inst[19] = 0;
        newParam.channels[0].inst[20] = 0;
        newParam.channels[0].inst[21] = 0;
        newParam.channels[0].inst[22] = 0;
        newParam.channels[0].inst[23] = 0;
        newParam.channels[0].inst[24] = 0;
        newParam.channels[0].inst[25] = 0;
        newParam.channels[0].inst[26] = 0;
        newParam.channels[0].inst[27] = 0;

        boolean YM2413Type = (chipId == 0)
                ? parent.setting.getYM2413Type()[0].getUseReal()[0]
                : parent.setting.getYM2413Type()[1].getUseReal()[0];
        int YM2413SoundLocation = (chipId == 0)
                ? parent.setting.getYM2413Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYM2413Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !YM2413Type ? 0 : (YM2413SoundLocation < 0 ? 2 : 1);

        screenInitYM2413(frameBuffer, tp);
        update();
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
                    for (int ch = 0; ch < 14; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(Ym2413Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(Ym2413Chip.class, chipId, ch);
                    }
                }
                return;
            }

            // keyboard
            if (py < 11 * 8) {
                int ch = (py / 8) - 1;
                if (ch < 0) return;

                if (ch == 9) {
                    int x = (px / 4 - 4);
                    if (x < 0) return;
                    x /= 15;
                    if (x > 4) return;
                    ch += x;
                }

                if (ev.getButton() == MouseEvent.BUTTON1) {
                    // Mask.
                    parent.setChannelMask(Ym2413Chip.class, chipId, ch);
                    return;
                }

                // Unmask.
                for (ch = 0; ch < 14; ch++) parent.resetChannelMask(Ym2413Chip.class, chipId, ch);
                return;
            }

            // Tone column
            if (py < 15 * 8 && px < 16 * 8) {
                // Copying a tone to the clipboard
                parent.getInstCh(Ym2413Chip.class, 0, chipId);
            }
        }
    };

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeYM2413");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 120));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmYM2413
        //
        this.setPreferredSize(new Dimension(320, 120));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmYM2413");
        this.setTitle("YM2413");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static int drawInstNumber(FrameBuffer screen, int x, int y, /* ref */ int oi, int ni) {
        if (oi != ni) {
            screen.drawFont4Int(x * 4, y * 4, 0, 2, ni);
            oi = ni;
        }
        return oi;
    }

    private static Boolean drawChYM2413(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChYM2413_P(screen, 0, ch < 9 ? (8 + ch * 8) : (8 + 9 * 8), ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static void drawChYM2413_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        if (ch < 9) {
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 0, 16, 8);
            screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(1 + ch));
        } else {
            switch (ch) {
                case 9:
                    screen.drawFont4((ch - 9) * 4 * 15 + 4 * 4, y, mask ? 1 : 0, "BD");
                    break;
                case 10:
                    screen.drawFont4((ch - 9) * 4 * 15 + 4 * 4, y, mask ? 1 : 0, "SD");
                    break;
                case 11:
                    screen.drawFont4((ch - 9) * 4 * 15 + 4 * 4, y, mask ? 1 : 0, "TM");
                    break;
                case 12:
                    screen.drawFont4((ch - 9) * 4 * 15 + 3 * 4, y, mask ? 1 : 0, "CYM");// 3
                    // character
                    break;
                case 13:
                    screen.drawFont4((ch - 9) * 4 * 15 + 4 * 4, y, mask ? 1 : 0, "HH");
                    break;
            }
        }
    }

//#endregion

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final ChannelParams[] channels = {
                new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams(), // FM 9
                new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams() // Rhythm 5
        };
    }


    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "YM2413"; }
        @Override public String menuText() { return "OPLL"; }
        @Override public String category() { return "opl"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return Ym2413Chip.class; }
        @Override public boolean hasRegisterDump() { return true; }
        @Override public String title(int chipId) { return "YM2413/VRC7 (%s)".formatted(chipId == 0 ? "Primary" : "Secondary"); }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormYM2413(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 14) {
                Ym2413Chip c = audio.plugin.chipRegister.chip(Ym2413Chip.class);
                if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(Ym2413Chip.class).resetMask(chipId, ch);
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (ch >= 0 && ch < 14) {
                if (mask)
                    audio.plugin.chipRegister.chip(Ym2413Chip.class).setMask(chipId, ch);
                else
                    audio.plugin.chipRegister.chip(Ym2413Chip.class).resetMask(chipId, ch);
            }
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 14; ch++)
                forceChannelMask(audio, Ym2413Chip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(Ym2413Chip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(16, mdsound.MDSound.Chip.MAIN_TAG, Ym2413Chip.class, "ym2413", 200));
        }

        @Override public void getInstCh(Component parent, Audio audio, Setting setting, int ch, int chipId) {
            if (setting.getOther().getInstFormat() == EnmInstFormat.MML2VGM) {
                // the OPLL has no MML2VGM writer
            } else if (setting.getOther().getInstFormat() == EnmInstFormat.SendMML2VGM) {
                new SendMml2vgmInstWriter().write(parent, audio, chip(), ch, chipId);
            } else {
                new MgscInstWriter().write(parent, audio, chip(), ch, chipId);
            }
        }
    }
}
