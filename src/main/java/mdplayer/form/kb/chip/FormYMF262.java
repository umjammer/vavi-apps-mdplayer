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
import mdplayer.Common;
import mdplayer.Common.EnmInstFormat;
import mdplayer.Setting;
import mdplayer.Tables;
import mdplayer.chips.SegaPcmChip;
import mdplayer.chips.YmF262Chip;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.form.View;
import mdplayer.form.inst.OpliInstWriter;
import mdplayer.form.inst.SendMml2vgmInstWriter;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;


public class FormYMF262 extends FormChipBase<FormYMF262.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormYMF262.class);

    public FormYMF262(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());
        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeYMF262"), null, zoom);
        boolean YMF262Type = (chipId == 0)
                ? parent.setting.getYMF262Type()[0].getUseReal()[0]
                : parent.setting.getYMF262Type()[1].getUseReal()[0];
        int YMF262SoundLocation = (chipId == 0)
                ? parent.setting.getYMF262Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYMF262Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !YMF262Type ? 0 : (YMF262SoundLocation < 0 ? 2 : 1);
        drawScreenInitYMF262(frameBuffer, tp);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("YMF262", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeYMF262").getWidth() * zoom, frameSizeH + Common.getImage("planeYMF262").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeYMF262").getWidth() * zoom, frameSizeH + Common.getImage("planeYMF262").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeYMF262").getWidth() * zoom, frameSizeH + Common.getImage("planeYMF262").getHeight() * zoom));
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

    private static final int[] slot1Tbl = {0, 6, 1, 7, 2, 8, 12, 13, 14, 18, 24, 19, 25, 20, 26, 30, 31, 32};
    private static final int[] slot2Tbl = {3, 9, 4, 10, 5, 11, 15, 16, 17, 21, 27, 22, 28, 23, 29, 33, 34, 35};
    private static final int[] chTbl = {0, 3, 1, 4, 2, 5, 6, 7, 8};

    @Override
    public void initScreen() {
        for (int c = 0; c < newParam.channels.length; c++) {
            newParam.channels[c].note = -1;
        }
        boolean YMF262Type = (chipId == 0)
                ? parent.setting.getYMF262Type()[0].getUseReal()[0]
                : parent.setting.getYMF262Type()[1].getUseReal()[0];
        int YMF262SoundLocation = (chipId == 0)
                ? parent.setting.getYMF262Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYMF262Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !YMF262Type ? 0 : (YMF262SoundLocation < 0 ? 2 : 1);
        drawScreenInitYMF262(frameBuffer, tp);
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(YmF262Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int[][] register = (int[][]) info.get("register");

        // FM
        for (int c = 0; c < 18; c++) {
            Channel nyc = newParam.channels[c];
            for (int i = 0; i < 2; i++) {

                int slot;
                int slotP;
                if (i == 0) {
                    slot = slot1Tbl[c] % 18;
                    slotP = slot1Tbl[c] / 18;
                } else {
                    slot = slot2Tbl[c] % 18;
                    slotP = slot2Tbl[c] / 18;
                }
                slot = (slot % 6) + 8 * (slot / 6);

                // AR
                nyc.inst[0 + i * 17] = register[slotP][0x60 + slot] >> 4;
                // DR
                nyc.inst[1 + i * 17] = register[slotP][0x60 + slot] & 0xf;
                // SL
                nyc.inst[2 + i * 17] = register[slotP][0x80 + slot] >> 4;
                // RR
                nyc.inst[3 + i * 17] = register[slotP][0x80 + slot] & 0xf;
                // KL
                nyc.inst[4 + i * 17] = register[slotP][0x40 + slot] >> 6;
                // TL
                nyc.inst[5 + i * 17] = register[slotP][0x40 + slot] & 0x3f;
                // MT
                nyc.inst[6 + i * 17] = register[slotP][0x20 + slot] & 0xf;
                // AM
                nyc.inst[7 + i * 17] = register[slotP][0x20 + slot] >> 7;
                // VB
                nyc.inst[8 + i * 17] = (register[slotP][0x20 + slot] >> 6) & 1;
                // EG
                nyc.inst[9 + i * 17] = (register[slotP][0x20 + slot] >> 5) & 1;
                // KR
                nyc.inst[10 + i * 17] = (register[slotP][0x20 + slot] >> 4) & 1;
                // WS
                nyc.inst[13 + i * 17] = (register[slotP][0xe0 + slot] & 7);
            }
        }

        newParam.channels[18].dda = ((register[1][0xbd] >> 7) & 0x01) != 0; // DA
        newParam.channels[19].dda = ((register[1][0xbd] >> 6) & 0x01) != 0; // DV

        // ConnectSelect
        for (int c = 0; c < 6; c++) {
            newParam.channels[c].dda = (register[1][0x04] & (0x1 << c)) != 0;
            newParam.channels[c].inst[34] = newParam.channels[c].dda ? 1 : 0; // [
            newParam.channels[c].inst[35] = newParam.channels[c].dda ? 2 : 0; // ]
            if (newParam.channels[c].dda) {
                // OP4 mode
                int ch = (c < 3) ? c * 2 : ((c - 3) * 2 + 9);
                // cnt=14
                int a = newParam.channels[ch].inst[14] * 2 + newParam.channels[ch].inst[14 + 17];
                // mod=16
                switch (a) {
                    case 0:
                        newParam.channels[ch].inst[16] = 0;
                        newParam.channels[ch].inst[16 + 17] = 0;
                        newParam.channels[ch + 1].inst[16] = 0;
                        newParam.channels[ch + 1].inst[16 + 17] = 1;
                        break;
                    case 1:
                        newParam.channels[ch].inst[16] = 0;
                        newParam.channels[ch].inst[16 + 17] = 1;
                        newParam.channels[ch + 1].inst[16] = 0;
                        newParam.channels[ch + 1].inst[16 + 17] = 1;
                        break;
                    case 2:
                        newParam.channels[ch].inst[16] = 1;
                        newParam.channels[ch].inst[16 + 17] = 0;
                        newParam.channels[ch + 1].inst[16] = 0;
                        newParam.channels[ch + 1].inst[16 + 17] = 1;
                        break;
                    case 3:
                        newParam.channels[ch].inst[16] = 1;
                        newParam.channels[ch].inst[16 + 17] = 0;
                        newParam.channels[ch + 1].inst[16] = 1;
                        newParam.channels[ch + 1].inst[16 + 17] = 1;
                        break;
                }
            }
        }

        int ko = (int) info.get("fmKeyON");

        for (int c = 0; c < 18; c++) {
            Channel nyc = newParam.channels[c];

            int p = c / 9;
            boolean isOp4 = false;
            int adr = c % 9;
            if (adr < 6) {
                if (newParam.channels[(adr / 2) + p * 3].dda) isOp4 = true;
            }
            int kadr = isOp4 ? (adr / 2) : adr;
            adr = chTbl[adr];

            // BL
            nyc.inst[11] = (register[p][0xb0 + adr] >> 2) & 7;
            // FNUM
            nyc.inst[12] = register[p][0xa0 + adr] + ((register[p][0xb0 + adr] & 3) << 8);

            // FB
            nyc.inst[15] = (register[p][0xc0 + adr] >> 1) & 7;
            // CN
            nyc.inst[14] = (register[p][0xc0 + adr] & 1);
            // PAN
            nyc.inst[36] = register[p][0xc0 + adr] & 0x30;
            nyc.inst[36] = ((nyc.inst[36] >> 5) & 1) | ((nyc.inst[36] >> 3) & 2); // 00RL0000 -> 000000LR
            // modFlg
            int n = register[p][0xc0 + adr] & 1;
            nyc.inst[16] = n == 0 ? 0 : 1;
            nyc.inst[33] = 1;

            int nt = SegaPcmChip.searchSegaPCMNote(nyc.inst[12] / 344.0) + (nyc.inst[11] - 4) * 12;
            if ((ko & (1 << (adr + p * 9))) != 0) {
                if (nyc.note != nt) {
                    nyc.note = nt;
                    int tl1 = nyc.inst[5 + 0 * 17];
                    int tl2 = nyc.inst[5 + 1 * 17];
                    int tl = tl2;
                    if (n != 0) {
                        tl = Math.min(tl1, tl2);
                    }
                    nyc.volumeL = (nyc.inst[36] & 2) != 0 ? (19 * (64 - tl) / 64) : 0;
                    nyc.volumeR = (nyc.inst[36] & 1) != 0 ? (19 * (64 - tl) / 64) : 0;
                } else {
                    nyc.volumeL--;
                    if (nyc.volumeL < 0) nyc.volumeL = 0;
                    nyc.volumeR--;
                    if (nyc.volumeR < 0) nyc.volumeR = 0;
                }
            } else {
                nyc.note = -1;
                nyc.volumeL--;
                if (nyc.volumeL < 0) {
                    nyc.volumeL = 0;
                }
                nyc.volumeR--;
                if (nyc.volumeR < 0) {
                    nyc.volumeR = 0;
                }
            }

        }

//#region Acquisition of rhythm information

        int r = (int) info.get("rhythmKeyON");

        // slot14 TL 0x51 HH
        // slot15 TL 0x52 TOM
        // slot16 TL 0x53 BD
        // slot17 TL 0x54 SD
        // slot18 TL 0x55 CYM

        // BD
        if ((r & 0x10) != 0) {
            newParam.channels[18].volume = 19 - ((register[0][0x53] & 0x3f) >> 2);
        } else {
            newParam.channels[18].volume--;
            if (newParam.channels[18].volume < 0) newParam.channels[18].volume = 0;
        }

        // SD
        if ((r & 0x08) != 0) {
            newParam.channels[19].volume = 19 - ((register[0][0x54] & 0x3f) >> 2);
        } else {
            newParam.channels[19].volume--;
            if (newParam.channels[19].volume < 0) newParam.channels[19].volume = 0;
        }

        // TOM
        if ((r & 0x04) != 0) {
            newParam.channels[20].volume = 19 - ((register[0][0x52] & 0x3f) >> 2);
        } else {
            newParam.channels[20].volume--;
            if (newParam.channels[20].volume < 0) newParam.channels[20].volume = 0;
        }

        // CYM
        if ((r & 0x02) != 0) {
            newParam.channels[21].volume = 19 - ((register[0][0x55] & 0x3f) >> 2);
        } else {
            newParam.channels[21].volume--;
            if (newParam.channels[21].volume < 0) newParam.channels[21].volume = 0;
        }

        // HH
        if ((r & 0x01) != 0) {
            newParam.channels[22].volume = 19 - ((register[0][0x51] & 0x3f) >> 2);
        } else {
            newParam.channels[22].volume--;
            if (newParam.channels[22].volume < 0) newParam.channels[22].volume = 0;
        }

        //audio.plugin.chipRegister.chip(YmF262Chip.class).resetRyhthmKeyON(chipId);

//#endregion

        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(YmF262Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        boolean ChipType2 = (chipId == 0)
                ? parent.setting.getYMF262Type()[0].getUseReal()[0]
                : parent.setting.getYMF262Type()[1].getUseReal()[0];
        int chipSoundLocation = (chipId == 0)
                ? parent.setting.getYMF262Type()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYMF262Type()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = !ChipType2 ? 0 : (chipSoundLocation < 0 ? 2 : 1);
        Channel oyc;
        Channel nyc;

        // FM
        for (int c = 0; c < 18; c++) {

            oyc = oldParam.channels[c];
            nyc = newParam.channels[c];

            for (int i = 0; i < 2; i++) {
                oyc.inst[16 + i * 17] = frameBuffer.drawSusFlag(2 + i * 33, c * 2 + 42, 1, oyc.inst[16 + i * 17], nyc.inst[16 + i * 17]);
                oyc.inst[0 + i * 17] = frameBuffer.font4Int2(16 + 4 + i * 132, c * 8 + 168, 0, 0, oyc.inst[0 + i * 17], nyc.inst[0 + i * 17]);  // AR
                oyc.inst[1 + i * 17] = frameBuffer.font4Int2(16 + 12 + i * 132, c * 8 + 168, 0, 0, oyc.inst[1 + i * 17], nyc.inst[1 + i * 17]); // DR
                oyc.inst[2 + i * 17] = frameBuffer.font4Int2(16 + 20 + i * 132, c * 8 + 168, 0, 0, oyc.inst[2 + i * 17], nyc.inst[2 + i * 17]); // SL
                oyc.inst[3 + i * 17] = frameBuffer.font4Int2(16 + 28 + i * 132, c * 8 + 168, 0, 0, oyc.inst[3 + i * 17], nyc.inst[3 + i * 17]); // RR

                oyc.inst[4 + i * 17] = frameBuffer.font4Int2(16 + 40 + i * 132, c * 8 + 168, 0, 0, oyc.inst[4 + i * 17], nyc.inst[4 + i * 17]); // KL
                oyc.inst[5 + i * 17] = frameBuffer.font4Int2(16 + 48 + i * 132, c * 8 + 168, 0, 0, oyc.inst[5 + i * 17], nyc.inst[5 + i * 17]); // TL

                oyc.inst[6 + i * 17] = frameBuffer.font4Int2(16 + 60 + i * 132, c * 8 + 168, 0, 0, oyc.inst[6 + i * 17], nyc.inst[6 + i * 17]); // MT

                oyc.inst[7 + i * 17] = frameBuffer.font4Int2(16 + 72 + i * 132, c * 8 + 168, 0, 0, oyc.inst[7 + i * 17], nyc.inst[7 + i * 17]); // AM
                oyc.inst[8 + i * 17] = frameBuffer.font4Int2(16 + 80 + i * 132, c * 8 + 168, 0, 0, oyc.inst[8 + i * 17], nyc.inst[8 + i * 17]); // VB
                oyc.inst[9 + i * 17] = frameBuffer.font4Int2(16 + 88 + i * 132, c * 8 + 168, 0, 0, oyc.inst[9 + i * 17], nyc.inst[9 + i * 17]); // EG
                oyc.inst[10 + i * 17] = frameBuffer.font4Int2(16 + 96 + i * 132, c * 8 + 168, 0, 0, oyc.inst[10 + i * 17], nyc.inst[10 + i * 17]);  // KR
                oyc.inst[13 + i * 17] = frameBuffer.font4Int2(16 + 108 + i * 132, c * 8 + 168, 0, 0, oyc.inst[13 + i * 17], nyc.inst[13 + i * 17]); // WS
            }

            oyc.inst[11] = frameBuffer.font4Int2(16 + 4 * 64, c * 8 + 168, 0, 0, oyc.inst[11], nyc.inst[11]); // BL
            oyc.inst[12] = frameBuffer.font4Hex12Bit(16 + 4 * 68, c * 8 + 168, 0, oyc.inst[12], nyc.inst[12]);   // F-Num
            oyc.inst[14] = frameBuffer.font4Int2(16 + 4 * 72, c * 8 + 168, 0, 0, oyc.inst[14], nyc.inst[14]); // CN
            oyc.inst[15] = frameBuffer.font4Int2(16 + 4 * 75, c * 8 + 168, 0, 0, oyc.inst[15], nyc.inst[15]); // FB
            int dmy = 99;
            { int[] r = frameBuffer.pan(24, 8 + c * 8, oyc.inst[36], nyc.inst[36], dmy, 0); oyc.inst[36] = r[0]; dmy = r[1]; }
            oyc.note = frameBuffer.drawKeyBoard(c, oyc.note, nyc.note, tp);
            oyc.volumeL = frameBuffer.drawVolumeXY(64, c * 2 + 2, 1, oyc.volumeL, nyc.volumeL, tp);
            oyc.volumeR = frameBuffer.drawVolumeXY(64, c * 2 + 3, 1, oyc.volumeR, nyc.volumeR, tp);
            oyc.mask = drawChYMF262(frameBuffer, c, oyc.mask, nyc.mask, tp);

        }

        for (int c = 0; c < 6; c++) {
            // CS
            oldParam.channels[c].dda = frameBuffer.drawNESSw(4 * 4 + c * 4, 39 * 8, oldParam.channels[c].dda, newParam.channels[c].dda);
            int ch = (c < 3) ? c * 2 : ((c - 3) * 2 + 9);
            oldParam.channels[c].inst[34] = drawKakko(frameBuffer, 4 * 0, (c < 3 ? 0 : 24) + c * 16 + 168, 0, oldParam.channels[c].inst[34], newParam.channels[c].inst[34]);
            oldParam.channels[c].inst[35] = drawKakko(frameBuffer, 4 * 163, (c < 3 ? 0 : 24) + c * 16 + 168, 0, oldParam.channels[c].inst[35], newParam.channels[c].inst[35]);
        }
        oldParam.channels[18].dda = frameBuffer.drawNESSw(13 * 4, 39 * 8, oldParam.channels[18].dda, newParam.channels[18].dda);//DA
        oldParam.channels[19].dda = frameBuffer.drawNESSw(17 * 4, 39 * 8, oldParam.channels[19].dda, newParam.channels[19].dda);//DV

        for (int c = 18; c < 23; c++) {
            oldParam.channels[c].mask = drawChYMF262(frameBuffer, c, oldParam.channels[c].mask, newParam.channels[c].mask, tp);
            oldParam.channels[c].volume = frameBuffer.drawVolumeXY(6 + (c - 18) * 15, 19 * 2, 0, oldParam.channels[c].volume, newParam.channels[c].volume, tp);
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
                    for (ch = 0; ch < 23; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(YmF262Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(YmF262Chip.class, chipId, ch);
                    }
                }
                return;
            }

            // Keyboard FM & RHM
            ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ch > 18) {
                if (ch >= 20 && ch < 38) {
                    // Click on the tone selection field.
                    // Copying a tone to the clipboard
                    if (ev.getButton() == MouseEvent.BUTTON1)
                        parent.getInstCh(YmF262Chip.class, ch - 20, chipId);
                }
                return;
            }

            if (ch == 18) {
                int x = (px / 4 - 4);
                if (x < 0) return;
                x /= 15;
                if (x > 4) return;
                ch += x;
            }

            if (ch > 22) return;

            if (ev.getButton() == MouseEvent.BUTTON1) {
                // Mask.
                parent.setChannelMask(YmF262Chip.class, chipId, ch);
                return;
            }

            // Unmask.
            for (ch = 0; ch < 18 + 5; ch++) parent.resetChannelMask(YmF262Chip.class, chipId, ch);
        }
    };

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeYMF262");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(328, 320));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmYMF262
        //
        this.setPreferredSize(new Dimension(328, 320));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmYMF262");
        this.setTitle("YMF262");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static void drawScreenInitYMF262(FrameBuffer screen, int tp) {
        if (screen == null)
            return;

        for (int y = 0; y < 18; y++) {
            // Note
            screen.drawFont8(296, y * 8 + 8, 1, "   ");

            // Keyboard
            for (int i = 0; i < 96; i++) {
                int kx = Tables.kbl[(i % 12) * 2] + i / 12 * 28;
                int kt = Tables.kbl[(i % 12) * 2 + 1];
                screen.drawKbn(32 + kx, y * 8 + 8, kt, tp);
            }

            //boolean bd = false;
            //ChYMF262(screen, y,bd, true, tp);
            //ChYMF262(screen, y,bd, false, tp);
            screen.drawPanP(24, y * 8 + 8, 3, tp);

            // Volume
            int d = 99;
            d = screen.drawVolumeM(256, 8 + y * 8, 0, d, 19, tp);
            d = screen.drawVolumeM(256, 8 + y * 8, 0, d, 0, tp);
        }
    }

    private static Boolean drawChYMF262(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }
        drawChYMF262_P(screen, 0, ch < 18 ? (8 + ch * 8) : (8 + 18 * 8), YMF262Ch[ch], nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static int drawKakko(FrameBuffer screen, int x, int y, int t, int ot, int nt) {
        if (ot != nt) {
            screen.drawByteArray(x, y, FrameBuffer.rKakko, 16, nt * 4, 0, 4, 8);
            for (int n = 0; n < t; n++) {
                screen.drawByteArray(x, y + n * 8 + 8, FrameBuffer.rKakko, 16, nt * 4, 8, 4, 8);
            }
            screen.drawByteArray(x, y + t * 8 + 8, FrameBuffer.rKakko, 16, nt * 4, 16, 4, 8);

            ot = nt;
        }
        return ot;
    }

    private static void drawChYMF262_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        if (ch < 18) {
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 0, 16, 8);
            //if (ch < 9) screen.drawFont8(x + 16, y, mask ? 1 : 0, (1 + ch).toString());
            //else
            screen.drawFont4(x + 16, y, mask ? 1 : 0, "%2d".formatted(1 + ch));
        } else if (ch < 23) {
            switch (ch) {
                case 18:
                    screen.drawFont4((ch - 18) * 4 * 15 + 4 * 4, y, mask ? 1 : 0, "BD");
                    break;
                case 19:
                    screen.drawFont4((ch - 18) * 4 * 15 + 4 * 4, y, mask ? 1 : 0, "SD");
                    break;
                case 20:
                    screen.drawFont4((ch - 18) * 4 * 15 + 4 * 4, y, mask ? 1 : 0, "TM");
                    break;
                case 21:
                    screen.drawFont4((ch - 18) * 4 * 15 + 3 * 4, y, mask ? 1 : 0, "CYM");// 3
                    // character
                    break;
                case 22:
                    screen.drawFont4((ch - 18) * 4 * 15 + 4 * 4, y, mask ? 1 : 0, "HH");
                    break;
            }
        }
    }

    private static final byte[] YMF262Ch = {
            0, 3, 1, 4, 2, 5, 6, 7, 8, 9, 12, 10, 13, 11, 14, 15, 16, 17, 18, 19, 20, 21, 22
    };

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        boolean dda = false;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final Channel[] channels = {
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), // FM 18
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel() // Rhythm 5
        };
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "YMF262"; }
        @Override public String menuText() { return "OPL3"; }
        @Override public String category() { return "opl"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return YmF262Chip.class; }
        @Override public boolean hasRegisterDump() { return true; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormYMF262(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 23) {
                YmF262Chip c = audio.plugin.chipRegister.chip(YmF262Chip.class);
                if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(YmF262Chip.class).resetMask(chipId, ch);
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (ch >= 0 && ch < 24) {
                if (mask)
                    audio.plugin.chipRegister.chip(YmF262Chip.class).setMask(chipId, ch);
                else
                    audio.plugin.chipRegister.chip(YmF262Chip.class).resetMask(chipId, ch);
            }
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(20, MDSound.Chip.MAIN_TAG, YmF262Chip.class, "ymf262", 200));
        }

        @Override public void getInstCh(Component parent, Audio audio, Setting setting, int ch, int chipId) {
            if (setting.getOther().getInstFormat() == EnmInstFormat.OPLI) {
                new OpliInstWriter().write(parent, audio, chip(), ch, chipId);
            } else {
                new SendMml2vgmInstWriter().write(parent, audio, chip(), ch, chipId);
            }
        }
    }
}
