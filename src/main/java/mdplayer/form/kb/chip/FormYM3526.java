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
import mdplayer.Chip.ChipKeyInfo;
import mdplayer.Common;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.Tables;
import mdplayer.chips.SegaPcmChip;
import mdplayer.chips.Ym3526Chip;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;
import mdsound.instrument.Ym3526Inst;
import mdplayer.form.View;


public class FormYM3526 extends FormChipBase<FormYM3526.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormYM3526.class);

    public FormYM3526(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());
        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeYM3526"), null, zoom);
        boolean YM3526Type = (chipId == 0)
                ? parent.setting.getYM3526Type()[0].getUseReal()[0]
                : parent.setting.getYM3526Type()[1].getUseReal()[0];
        int YM3526SoundLocation = (chipId == 0)
                ? parent.setting.getYM3526Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYM3526Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !YM3526Type ? 0 : (YM3526SoundLocation < 0 ? 2 : 1);

        drawScreenInitYM3526(frameBuffer, tp);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("YM3526", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeYM3526").getWidth() * zoom, frameSizeH + Common.getImage("planeYM3526").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeYM3526").getWidth() * zoom, frameSizeH + Common.getImage("planeYM3526").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeYM3526").getWidth() * zoom, frameSizeH + Common.getImage("planeYM3526").getHeight() * zoom));
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
    public void initScreen() {
        for (int c = 0; c < newParam.channels.length; c++) {
            newParam.channels[c].note = -1;
        }
    }

    private static final int[] slot1Tbl = {0, 1, 2, 6, 7, 8, 12, 13, 14};
    private static final int[] slot2Tbl = {3, 4, 5, 9, 10, 11, 15, 16, 17};
    private static final byte[] rhythmAdr = {0x53, 0x54, 0x52, 0x55, 0x51};

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(Ym3526Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int[] register = (int[]) info.get("register");
        ChipKeyInfo ki = (ChipKeyInfo) info.get("keyInfo");

        mdsound.MDSound.Chip chipInfo = audio.plugin.mds.getChipInfo(Ym3526Inst.class);
        int masterClock = chipInfo == null ? 3579545 : chipInfo.clock; // 3579545 -> Default master clock

        // FM
        for (int c = 0; c < 9; c++) {
            Channel nyc = newParam.channels[c];
            for (int i = 0; i < 2; i++) {

                int slot;
                if (i == 0) {
                    slot = slot1Tbl[c];
                } else {
                    slot = slot2Tbl[c];
                }
                slot = (slot % 6) + 8 * (slot / 6);

                // AR
                nyc.inst[0 + i * 17] = register[0x60 + slot] >> 4;
                // DR
                nyc.inst[1 + i * 17] = register[0x60 + slot] & 0xf;
                // SL
                nyc.inst[2 + i * 17] = register[0x80 + slot] >> 4;
                // RR
                nyc.inst[3 + i * 17] = register[0x80 + slot] & 0xf;
                // KL
                nyc.inst[4 + i * 17] = register[0x40 + slot] >> 6;
                // TL
                nyc.inst[5 + i * 17] = register[0x40 + slot] & 0x3f;
                // MT
                nyc.inst[6 + i * 17] = register[0x20 + slot] & 0xf;
                // AM
                nyc.inst[7 + i * 17] = register[0x20 + slot] >> 7;
                // VB
                nyc.inst[8 + i * 17] = (register[0x20 + slot] >> 6) & 1;
                // EG
                nyc.inst[9 + i * 17] = (register[0x20 + slot] >> 5) & 1;
                // KR
                nyc.inst[10 + i * 17] = (register[0x20 + slot] >> 4) & 1;
            }

            // BL
            nyc.inst[11] = (register[0xb0 + c] >> 2) & 7;
            // FNUM
            nyc.inst[12] = register[0xa0 + c] + ((register[0xb0 + c] & 3) << 8);

            // FB
            nyc.inst[15] = (register[0xc0 + c] >> 1) & 7;
            // CN
            nyc.inst[14] = (register[0xc0 + c] & 1);

            // FNUM / (2^19) * (mClock/72) * (2 ^ (block - 1))
            double fmus = (double) nyc.inst[12] / (1 << 19) * (masterClock / 72.0) * (1 << nyc.inst[11]);
            nyc.note = SegaPcmChip.searchSegaPCMNote(fmus / 523.3);//523.3 -> c4

            if (ki.on[c]) {
                int tl1 = nyc.inst[5 + 0 * 17];
                int tl2 = nyc.inst[5 + 1 * 17];
                int tl = tl2;
                if (nyc.inst[14] != 0) {
                    tl = Math.min(tl1, tl2);
                }
                nyc.volume = (19 * (64 - tl) / 64);
            } else {
                if ((register[0xb0 + c] & 0x20) == 0) nyc.note = -1;
                nyc.volume--;
                if (nyc.volume < 0) nyc.volume = 0;
            }

        }
        newParam.channels[9].dda = ((register[0xbd] >> 7) & 0x01) != 0;  // DA
        newParam.channels[10].dda = ((register[0xbd] >> 6) & 0x01) != 0; // DV

//#region Acquisition of rhythm information

        // slot14 TL 0x51 HH
        // slot15 TL 0x52 TOM
        // slot16 TL 0x53 BD
        // slot17 TL 0x54 SD
        // slot18 TL 0x55 CYM

        for (int i = 0; i < 5; i++) {
            if (ki.on[i + 9]) {
                newParam.channels[i + 9].volume = 19 - ((register[rhythmAdr[i]] & 0x3f) >> 2);
            } else {
                newParam.channels[i + 9].volume--;
                if (newParam.channels[i + 9].volume < 0) newParam.channels[i + 9].volume = 0;
            }
        }

//#endregion
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(Ym3526Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        boolean YM3526Type = (chipId == 0)
                ? parent.setting.getYM3526Type()[0].getUseReal()[0]
                : parent.setting.getYM3526Type()[1].getUseReal()[0];
        int YM3526SoundLocation = (chipId == 0)
                ? parent.setting.getYM3526Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYM3526Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !YM3526Type ? 0 : (YM3526SoundLocation < 0 ? 2 : 1);
        Channel oyc;
        Channel nyc;

        // FM
        for (int c = 0; c < 9; c++) {

            oyc = oldParam.channels[c];
            nyc = newParam.channels[c];

            for (int i = 0; i < 2; i++) {
                oyc.inst[0 + i * 17] = frameBuffer.font4Int2(16 + 4 + i * 132, c * 8 + 96, 0, 0, oyc.inst[0 + i * 17], nyc.inst[0 + i * 17]);//AR
                oyc.inst[1 + i * 17] = frameBuffer.font4Int2(16 + 12 + i * 132, c * 8 + 96, 0, 0, oyc.inst[1 + i * 17], nyc.inst[1 + i * 17]);//DR
                oyc.inst[2 + i * 17] = frameBuffer.font4Int2(16 + 20 + i * 132, c * 8 + 96, 0, 0, oyc.inst[2 + i * 17], nyc.inst[2 + i * 17]);//SL
                oyc.inst[3 + i * 17] = frameBuffer.font4Int2(16 + 28 + i * 132, c * 8 + 96, 0, 0, oyc.inst[3 + i * 17], nyc.inst[3 + i * 17]);//RR

                oyc.inst[4 + i * 17] = frameBuffer.font4Int2(16 + 40 + i * 132, c * 8 + 96, 0, 0, oyc.inst[4 + i * 17], nyc.inst[4 + i * 17]);//KL
                oyc.inst[5 + i * 17] = frameBuffer.font4Int2(16 + 48 + i * 132, c * 8 + 96, 0, 0, oyc.inst[5 + i * 17], nyc.inst[5 + i * 17]);//TL

                oyc.inst[6 + i * 17] = frameBuffer.font4Int2(16 + 60 + i * 132, c * 8 + 96, 0, 0, oyc.inst[6 + i * 17], nyc.inst[6 + i * 17]);//MT

                oyc.inst[7 + i * 17] = frameBuffer.font4Int2(16 + 72 + i * 132, c * 8 + 96, 0, 0, oyc.inst[7 + i * 17], nyc.inst[7 + i * 17]);//AM
                oyc.inst[8 + i * 17] = frameBuffer.font4Int2(16 + 80 + i * 132, c * 8 + 96, 0, 0, oyc.inst[8 + i * 17], nyc.inst[8 + i * 17]);//VB
                oyc.inst[9 + i * 17] = frameBuffer.font4Int2(16 + 88 + i * 132, c * 8 + 96, 0, 0, oyc.inst[9 + i * 17], nyc.inst[9 + i * 17]);//EG
                oyc.inst[10 + i * 17] = frameBuffer.font4Int2(16 + 96 + i * 132, c * 8 + 96, 0, 0, oyc.inst[10 + i * 17], nyc.inst[10 + i * 17]);//KR
            }

            oyc.inst[11] = frameBuffer.font4Int2(16 + 4 * 64, c * 8 + 96, 0, 0, oyc.inst[11], nyc.inst[11]);//BL
            oyc.inst[12] = frameBuffer.font4Hex12Bit(16 + 4 * 68, c * 8 + 96, 0, oyc.inst[12], nyc.inst[12]);//F-Num
            oyc.inst[14] = frameBuffer.font4Int2(16 + 4 * 72, c * 8 + 96, 0, 0, oyc.inst[14], nyc.inst[14]);//CN
            oyc.inst[15] = frameBuffer.font4Int2(16 + 4 * 75, c * 8 + 96, 0, 0, oyc.inst[15], nyc.inst[15]);//FB
            oyc.note = frameBuffer.drawKeyBoard(c, oyc.note, nyc.note, tp);
            oyc.volume = frameBuffer.drawVolumeXY(64, c * 2 + 2, 0, oyc.volume, nyc.volume, tp);
            oyc.mask = drawChYM3526(frameBuffer, c, oyc.mask, nyc.mask, tp);
        }

        oldParam.channels[9].dda = frameBuffer.drawNESSw(76 * 4, 10 * 8, oldParam.channels[9].dda, newParam.channels[9].dda);//DA
        oldParam.channels[10].dda = frameBuffer.drawNESSw(80 * 4, 10 * 8, oldParam.channels[10].dda, newParam.channels[10].dda);//DV

        for (int c = 9; c < 14; c++) {
            oldParam.channels[c].mask = drawChYM3526(frameBuffer, c, oldParam.channels[c].mask, newParam.channels[c].mask, tp);
            oldParam.channels[c].volume = frameBuffer.drawVolumeXY(3 + (c - 9) * 15, 10 * 2, 0, oldParam.channels[c].volume, newParam.channels[c].volume, tp);
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
                    for (ch = 0; ch < 9 + 5; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(Ym3526Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(Ym3526Chip.class, chipId, ch);
                    }
                }
                return;
            }

            // keyboard FM & RHM
            ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ch == 9) {
                int x = (px / 4 - 1);
                if (x < 0) return;
                x /= 15;
                if (x > 4) return;
                ch += x;
            }

            if (ev.getButton() == MouseEvent.BUTTON1) {
                // Mask.
                parent.setChannelMask(Ym3526Chip.class, chipId, ch);
                return;
            }

            // Unmask.
            for (ch = 0; ch < 9 + 5; ch++) parent.resetChannelMask(Ym3526Chip.class, chipId, ch);
        }
    };

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeYM3526");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(328, 168));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmYM3526
        //
        this.setPreferredSize(new Dimension(328, 168));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmYM3526");
        this.setTitle("YM3526");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static void drawScreenInitYM3526(FrameBuffer screen, int tp) {
        for (int y = 0; y < 9; y++) {
            // Note
            screen.drawFont8(296, y * 8 + 8, 1, "   ");

            // Keyboard
            for (int i = 0; i < 96; i++) {
                int kx = Tables.kbl[(i % 12) * 2] + i / 12 * 28;
                int kt = Tables.kbl[(i % 12) * 2 + 1];
                screen.drawKbn(32 + kx, y * 8 + 8, kt, tp);
            }

            boolean dm = true;
            dm = drawChYM3526(screen, y, dm, false, tp);

            // Volume
            int d = 99;
            d = screen.drawVolumeM(256, 8 + y * 8, 0, d, 19, tp);
        }
    }

    private static Boolean drawChYM3526(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }
        drawChYM3526_P(screen, 0, ch < 9 ? (8 + ch * 8) : (8 + 9 * 8), ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static void drawChYM3526_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        if (ch < 9) {
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 0, 16, 8);
            screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(1 + ch));
        } else if (ch < 14) {
            switch (ch) {
                case 9:
                    screen.drawFont4((ch - 9) * 4 * 15 + 1 * 4, y, mask ? 1 : 0, "BD");
                    break;
                case 10:
                    screen.drawFont4((ch - 9) * 4 * 15 + 1 * 4, y, mask ? 1 : 0, "SD");
                    break;
                case 11:
                    screen.drawFont4((ch - 9) * 4 * 15 + 1 * 4, y, mask ? 1 : 0, "TM");
                    break;
                case 12:
                    screen.drawFont4((ch - 9) * 4 * 15 + 0 * 4, y, mask ? 1 : 0, "CYM");// 3
                    // character
                    break;
                case 13:
                    screen.drawFont4((ch - 9) * 4 * 15 + 1 * 4, y, mask ? 1 : 0, "HH");
                    break;
            }
        }
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        boolean dda = false;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final Channel[] channels = {
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(), // FM 9
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel() // Rhythm 5
        };
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "YM3526"; }
        @Override public String menuText() { return "OPL"; }
        @Override public String category() { return "opl"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return Ym3526Chip.class; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormYM3526(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 14) {
                Ym3526Chip c = audio.plugin.chipRegister.chip(Ym3526Chip.class);
                if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(Ym3526Chip.class).resetMask(chipId, ch);
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (ch >= 0 && ch < 14) {
                if (mask)
                    audio.plugin.chipRegister.chip(Ym3526Chip.class).setMask(chipId, ch);
                else
                    audio.plugin.chipRegister.chip(Ym3526Chip.class).resetMask(chipId, ch);
            }
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(17, MDSound.Chip.MAIN_TAG, Ym3526Chip.class, "ym3526", 200));
        }
    }
}
