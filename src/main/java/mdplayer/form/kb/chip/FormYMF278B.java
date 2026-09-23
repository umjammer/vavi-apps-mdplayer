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
import mdplayer.chips.YmF278BChip;
import mdplayer.driver.moonDriver.BuiltInMoonDriver;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.form.View;
import mdplayer.form.inst.OpliInstWriter;
import mdplayer.form.inst.SendMml2vgmInstWriter;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;


public class FormYMF278B extends FormChipBase<FormYMF278B.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormYMF278B.class);

    public FormYMF278B(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());
        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeYMF278B"), null, zoom);
        boolean ymF278BType = (chipId == 0)
                ? parent.setting.getYMF278BType()[0].getUseReal()[0]
                : parent.setting.getYMF278BType()[1].getUseReal()[0];
        int ymF278BSoundLocation = (chipId == 0)
                ? parent.setting.getYMF278BType()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYMF278BType()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = ymF278BType ? 1 : 0;
        drawScreenInitYMF278B(frameBuffer, tp);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("YMF278B", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeYMF278B").getWidth() * zoom, frameSizeH + Common.getImage("planeYMF278B").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeYMF278B").getWidth() * zoom, frameSizeH + Common.getImage("planeYMF278B").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeYMF278B").getWidth() * zoom, frameSizeH + Common.getImage("planeYMF278B").getHeight() * zoom));
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
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(YmF278BChip.class).getInfo(chipId);
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
        newParam.channels[20].freq = register[2][0xf8] & 0x7; // FM MIX_L
        newParam.channels[21].freq = register[2][0xf8] >> 3;  // FM MIX_R
        newParam.channels[22].freq = register[2][0xf9] & 0x7; // PCM MIX_L
        newParam.channels[23].freq = register[2][0xf9] >> 3;  // PCM MIX_R

        // ConnectSelect
        for (int c = 0; c < 6; c++) {
            newParam.channels[c].dda = (register[1][0x04] & (0x1 << c)) != 0;
            newParam.channels[c].inst[34] = newParam.channels[c].dda ? 1 : 0;
            newParam.channels[c].inst[35] = newParam.channels[c].dda ? 2 : 0;
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

        int ko = (int) info.get("fmKeyOn");

        for (int c = 0; c < 18; c++) {
            Channel nyc = newParam.channels[c];

            int p = c / 9;
            int cadr = c % 9;

            int adr = chTbl[cadr];

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

            boolean fouropChannel = cadr < 6;
            boolean fouropControl = fouropChannel && cadr % 2 == 0;

            // The 4op flag is different from the keyboard layout.
            Channel ccnt = fouropChannel ? newParam.channels[(p * 3) + (cadr / 2)] : null;
            Channel csub = fouropControl ? newParam.channels[c + 1] : null;
            boolean fouropMode = ccnt != null && ccnt.dda;

            int cnt2 = fouropControl ? register[p][0xc3 + adr] & 1 : 0;

            boolean chmask = fouropMode && !fouropControl;

            if ((ko & (1 << (adr + p * 9))) != 0) {
                if (nyc.note != nt && !chmask) {
                    nyc.note = nt;

                    if (fouropMode) {
                        int tl1 = nyc.inst[5 + 0 * 17];
                        int tl2 = nyc.inst[5 + 1 * 17];
                        int tl3 = csub.inst[5 + 0 * 17];
                        int tl4 = csub.inst[5 + 1 * 17];

                        // cnt == 0 is TL4
                        int tl = tl4;

                        int cnt = (n << 1) + cnt2;
                        tl = switch (cnt) {
                            case 1 -> Math.min(tl2, tl4);
                            case 2 -> Math.min(tl1, tl4);
                            case 3 -> Math.min(tl1, Math.min(tl3, tl4));
                            default -> tl;
                        };

                        nyc.volumeL = (nyc.inst[36] & 2) != 0 ? (19 * (64 - tl) / 64) : 0;
                        nyc.volumeR = (nyc.inst[36] & 1) != 0 ? (19 * (64 - tl) / 64) : 0;
                    } else {
                        int tl1 = nyc.inst[5 + 0 * 17];
                        int tl2 = nyc.inst[5 + 1 * 17];
                        int tl = tl2;
                        if (n != 0) {
                            tl = Math.min(tl1, tl2);
                        }
                        nyc.volumeL = (nyc.inst[36] & 2) != 0 ? (19 * (64 - tl) / 64) : 0;
                        nyc.volumeR = (nyc.inst[36] & 1) != 0 ? (19 * (64 - tl) / 64) : 0;
                    }
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

        //audio.plugin.chipRegister.chip(YmF278BChip.class).resetFMKeyON(chipId);

        int r = (int) info.get("rhythmKeyOn");

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

        audio.plugin.chipRegister.chip(YmF278BChip.class).resetRhythmKeyOn(chipId);

        // PCM
        int[] pcmKey = (int[]) info.get("pcmKeyOn");
        int[] mdPCMKey = (audio.plugin.driverVirtual instanceof BuiltInMoonDriver moonDriver) ? moonDriver.getPCMKeyOn() : null; // TODO gross
        for (int c = 23; c < 23 + 24; c++) {
            Channel nyc = newParam.channels[c];
            // Pan
            nyc.pan = (register[2][0x68 + (c - 23)] & 0xf);
            nyc.pan = (nyc.pan == 8 ? 0 :
                    (
                            (nyc.pan < 8 ? (15 - nyc.pan * 2) : 15) + ((nyc.pan > 8 ? (nyc.pan * 2 - 18) : 15) << 4)
                    ));
            // Oct
            nyc.inst[13] = (register[2][0x38 + (c - 23)] >> 4);
            // F-Num
            nyc.inst[14] = (register[2][0x20 + (c - 23)] >> 1) + ((register[2][0x38 + (c - 23)] & 0x7) << 7);
            if (mdPCMKey == null) {
                // Other than moonDriver
                // Volume
                if (pcmKey[c - 23] == 1) {
                    // note
                    nyc.note = ((nyc.inst[13] + 7) & 0xf) * 12 + Common.searchPCMNote(nyc.inst[14], 1) - 5;
                    //logger.log(Level.TRACE, "%x %x".formatted(nyc.inst[13], nyc.inst[14]));
                    nyc.volumeL = (127 - (register[2][0x50 + (c - 23)] >> 1)) * (nyc.pan & 0xf) / 16 / 6;
                    nyc.volumeR = (127 - (register[2][0x50 + (c - 23)] >> 1)) * (nyc.pan >> 4) / 16 / 6;
                } else {
                    if (pcmKey[c - 23] == 2) {
                        nyc.note = -1;
                    }
                    nyc.volumeL--;
                    if (nyc.volumeL < 0) nyc.volumeL = 0;
                    nyc.volumeR--;
                    if (nyc.volumeR < 0) nyc.volumeR = 0;
                }
            } else {
                // In the case of moonDriver
                if (mdPCMKey[c - 23] > -1) {
                    // note
                    nyc.note = mdPCMKey[c - 23];
                    //logger.log(Level.TRACE, "%x %x".formatted(nyc.inst[13], nyc.inst[14]));
                    nyc.volumeL = (127 - (register[2][0x50 + (c - 23)] >> 1)) * (nyc.pan & 0xf) / 16 / 6;
                    nyc.volumeR = (127 - (register[2][0x50 + (c - 23)] >> 1)) * (nyc.pan >> 4) / 16 / 6;
                } else {
                    if (mdPCMKey[c - 23] == -1) nyc.note = -1;
                    nyc.volumeL--;
                    if (nyc.volumeL < 0) nyc.volumeL = 0;
                    nyc.volumeR--;
                    if (nyc.volumeR < 0) nyc.volumeR = 0;
                }
            }
            // AR
            nyc.inst[0] = (register[2][0x98 + (c - 23)] >> 4);
            // D1
            nyc.inst[1] = (register[2][0x98 + (c - 23)]) & 0xf;
            // DL
            nyc.inst[2] = (register[2][0xb0 + (c - 23)] >> 4);
            // D2
            nyc.inst[3] = (register[2][0xb0 + (c - 23)]) & 0xf;
            // RC
            nyc.inst[4] = (register[2][0xc8 + (c - 23)] >> 4);
            // RR
            nyc.inst[5] = (register[2][0xc8 + (c - 23)]) & 0xf;
            // AM
            nyc.inst[6] = (register[2][0xe0 + (c - 23)]) & 0x7;
            // Vib
            nyc.inst[7] = (register[2][0x80 + (c - 23)]) & 0x7;
            // Lfo
            nyc.inst[8] = (register[2][0x80 + (c - 23)] >> 3) & 0x7;
            // Reverb
            nyc.inst[9] = (register[2][0x38 + (c - 23)] >> 3) & 0x1;
            // LD
            nyc.inst[10] = (register[2][0x50 + (c - 23)] & 0x1);
            // TL
            nyc.inst[11] = (register[2][0x50 + (c - 23)] >> 1);
            // Wav
            nyc.inst[12] = (register[2][0x08 + (c - 23)]) + ((register[2][0x20 + (c - 23)] & 0x1) << 8);
        }

        audio.plugin.chipRegister.chip(YmF278BChip.class).resetPcmKeyOn(chipId);
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(YmF278BChip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        boolean YMF278BType = (chipId == 0)
                ? parent.setting.getYMF278BType()[0].getUseReal()[0]
                : parent.setting.getYMF278BType()[1].getUseReal()[0];
        int YMF278BSoundLocation = (chipId == 0)
                ? parent.setting.getYMF278BType()[0].getRealChipInfo()[0].getSoundLocation()
                : parent.setting.getYMF278BType()[1].getRealChipInfo()[0].getSoundLocation();
        int tp = YMF278BType ? 1 : 0;

        Channel oyc;
        Channel nyc;

        // FM
        for (int c = 0; c < 18; c++) {

            oyc = oldParam.channels[c];
            nyc = newParam.channels[c];

            for (int i = 0; i < 2; i++) {
                oyc.inst[16 + i * 17] = frameBuffer.drawSusFlag(81 + i * 34, c * 2 + 2, 1, oyc.inst[16 + i * 17], nyc.inst[16 + i * 17]);
                oyc.inst[0 + i * 17] = frameBuffer.font4Int2(336 + 4 + i * 136, c * 8 + 8, 0, 0, oyc.inst[0 + i * 17], nyc.inst[0 + i * 17]);  // AR
                oyc.inst[1 + i * 17] = frameBuffer.font4Int2(336 + 12 + i * 136, c * 8 + 8, 0, 0, oyc.inst[1 + i * 17], nyc.inst[1 + i * 17]); // DR
                oyc.inst[2 + i * 17] = frameBuffer.font4Int2(336 + 20 + i * 136, c * 8 + 8, 0, 0, oyc.inst[2 + i * 17], nyc.inst[2 + i * 17]); // SL
                oyc.inst[3 + i * 17] = frameBuffer.font4Int2(336 + 28 + i * 136, c * 8 + 8, 0, 0, oyc.inst[3 + i * 17], nyc.inst[3 + i * 17]); // RR

                oyc.inst[4 + i * 17] = frameBuffer.font4Int2(336 + 40 + i * 136, c * 8 + 8, 0, 0, oyc.inst[4 + i * 17], nyc.inst[4 + i * 17]); // KL
                oyc.inst[5 + i * 17] = frameBuffer.font4Int2(336 + 48 + i * 136, c * 8 + 8, 0, 0, oyc.inst[5 + i * 17], nyc.inst[5 + i * 17]); // TL

                oyc.inst[6 + i * 17] = frameBuffer.font4Int2(336 + 60 + i * 136, c * 8 + 8, 0, 0, oyc.inst[6 + i * 17], nyc.inst[6 + i * 17]); // MT

                oyc.inst[7 + i * 17] = frameBuffer.font4Int2(336 + 72 + i * 136, c * 8 + 8, 0, 0, oyc.inst[7 + i * 17], nyc.inst[7 + i * 17]); // AM
                oyc.inst[8 + i * 17] = frameBuffer.font4Int2(336 + 80 + i * 136, c * 8 + 8, 0, 0, oyc.inst[8 + i * 17], nyc.inst[8 + i * 17]); // VB
                oyc.inst[9 + i * 17] = frameBuffer.font4Int2(336 + 88 + i * 136, c * 8 + 8, 0, 0, oyc.inst[9 + i * 17], nyc.inst[9 + i * 17]); // EG
                oyc.inst[10 + i * 17] = frameBuffer.font4Int2(336 + 96 + i * 136, c * 8 + 8, 0, 0, oyc.inst[10 + i * 17], nyc.inst[10 + i * 17]);  // KR
                oyc.inst[13 + i * 17] = frameBuffer.font4Int2(336 + 108 + i * 136, c * 8 + 8, 0, 0, oyc.inst[13 + i * 17], nyc.inst[13 + i * 17]); // WS
            }

            oyc.inst[11] = frameBuffer.font4Int2(336 + 4 * 65, c * 8 + 8, 0, 0, oyc.inst[11], nyc.inst[11]); // BL
            oyc.inst[12] = frameBuffer.font4Hex12Bit(336 + 4 * 69, c * 8 + 8, 0, oyc.inst[12], nyc.inst[12]);   // F-Num
            oyc.inst[14] = frameBuffer.font4Int2(336 + 4 * 73, c * 8 + 8, 0, 0, oyc.inst[14], nyc.inst[14]); // CN
            oyc.inst[15] = frameBuffer.font4Int2(336 + 4 * 76, c * 8 + 8, 0, 0, oyc.inst[15], nyc.inst[15]); // FB
            int dmy = 99;
            { int[] r = frameBuffer.pan(24, 8 + c * 8, oyc.inst[36], nyc.inst[36], dmy, 0); oyc.inst[36] = r[0]; dmy = r[1]; }
            oyc.note = frameBuffer.drawKeyBoard(c, oyc.note, nyc.note, tp);
            oyc.volumeL = frameBuffer.drawVolumeXY(64, c * 2 + 2, 1, oyc.volumeL, nyc.volumeL, tp);
            oyc.volumeR = frameBuffer.drawVolumeXY(64, c * 2 + 3, 1, oyc.volumeR, nyc.volumeR, tp);
            oyc.mask = drawChYMF278B(frameBuffer, c, oyc.mask, nyc.mask, tp);

            //frameBuffer.drawInstNumber((c % 3) * 16 + 37, (c / 3) * 2 + 24,oyc.inst[0], nyc.inst[0]);
            //frameBuffer.drawSUSFlag((c % 3) * 16 + 41, (c / 3) * 2 + 24,oyc.inst[1], nyc.inst[1]);
            //frameBuffer.drawSUSFlag((c % 3) * 16 + 44, (c / 3) * 2 + 24,oyc.inst[2], nyc.inst[2]);
            //frameBuffer.drawInstNumber((c % 3) * 16 + 46, (c / 3) * 2 + 24,oyc.inst[3], nyc.inst[3]);
        }

        for (int c = 0; c < 6; c++) {
            // CS
            oldParam.channels[c].dda = frameBuffer.drawNESSw(79 * 4 + c * 4, 19 * 8, oldParam.channels[c].dda, newParam.channels[c].dda);
            int ch = (c < 3) ? c * 2 : ((c - 3) * 2 + 9);
            oldParam.channels[c].inst[34] = drawKakko(frameBuffer, 4 * 80, (c < 3 ? 0 : 24) + c * 16 + 8, 0, oldParam.channels[c].inst[34], newParam.channels[c].inst[34]);
            oldParam.channels[c].inst[35] = drawKakko(frameBuffer, 4 * 162, (c < 3 ? 0 : 24) + c * 16 + 8, 0, oldParam.channels[c].inst[35], newParam.channels[c].inst[35]);
        }
        oldParam.channels[18].dda = frameBuffer.drawNESSw(88 * 4, 19 * 8, oldParam.channels[18].dda, newParam.channels[18].dda); // DA
        oldParam.channels[19].dda = frameBuffer.drawNESSw(92 * 4, 19 * 8, oldParam.channels[19].dda, newParam.channels[19].dda); // DV
        oldParam.channels[20].freq = frameBuffer.font4Int1(109 * 4, 19 * 8, 0, oldParam.channels[20].freq, newParam.channels[20].freq); // FM MIX_L
        oldParam.channels[21].freq = frameBuffer.font4Int1(111 * 4, 19 * 8, 0, oldParam.channels[21].freq, newParam.channels[21].freq); // FM_MI_R
        oldParam.channels[22].freq = frameBuffer.font4Int1(100 * 4, 19 * 8, 0, oldParam.channels[22].freq, newParam.channels[22].freq); // PCM MIX_L
        oldParam.channels[23].freq = frameBuffer.font4Int1(102 * 4, 19 * 8, 0, oldParam.channels[23].freq, newParam.channels[23].freq); // PCM_MI_R

        for (int c = 18; c < 23; c++) {
            oldParam.channels[c].mask = drawChYMF278B(frameBuffer, c, oldParam.channels[c].mask, newParam.channels[c].mask, tp);
            oldParam.channels[c].volume = frameBuffer.drawVolumeXY(12 + (c - 18) * 13, 19 * 2, 0, oldParam.channels[c].volume, newParam.channels[c].volume, tp);
        }

        // PCM
        for (int c = 23; c < 23 + 24; c++) {
            oyc = oldParam.channels[c];
            nyc = newParam.channels[c];
            oyc.pan = frameBuffer.PanType2(c - 4, oyc.pan, nyc.pan, 0);
            oyc.inst[0] = frameBuffer.font4Int2(516 + 0, (c - 3) * 8, 0, 0, oyc.inst[0], nyc.inst[0]);  // AR
            oyc.inst[1] = frameBuffer.font4Int2(516 + 8, (c - 3) * 8, 0, 0, oyc.inst[1], nyc.inst[1]);  // D1
            oyc.inst[2] = frameBuffer.font4Int2(516 + 16, (c - 3) * 8, 0, 0, oyc.inst[2], nyc.inst[2]); // DL
            oyc.inst[3] = frameBuffer.font4Int2(516 + 24, (c - 3) * 8, 0, 0, oyc.inst[3], nyc.inst[3]); // D2
            oyc.inst[4] = frameBuffer.font4Int2(516 + 32, (c - 3) * 8, 0, 0, oyc.inst[4], nyc.inst[4]); // RC
            oyc.inst[5] = frameBuffer.font4Int2(516 + 40, (c - 3) * 8, 0, 0, oyc.inst[5], nyc.inst[5]); // RR
            oyc.inst[6] = frameBuffer.font4Int2(516 + 48, (c - 3) * 8, 0, 0, oyc.inst[6], nyc.inst[6]); // AM
            oyc.inst[7] = frameBuffer.font4Int2(516 + 56, (c - 3) * 8, 0, 0, oyc.inst[7], nyc.inst[7]); // VB
            oyc.inst[8] = frameBuffer.font4Int2(516 + 64, (c - 3) * 8, 0, 0, oyc.inst[8], nyc.inst[8]); // Lfo
            oyc.inst[9] = frameBuffer.font4Int2(516 + 72, (c - 3) * 8, 0, 0, oyc.inst[9], nyc.inst[9]); // RV
            oyc.inst[10] = frameBuffer.font4Int2(516 + 80, (c - 3) * 8, 0, 0, oyc.inst[10], nyc.inst[10]);  // LD
            oyc.inst[11] = frameBuffer.font4Int2(516 + 88, (c - 3) * 8, 0, 3, oyc.inst[11], nyc.inst[11]);  // TL
            oyc.inst[12] = frameBuffer.font4Int2(516 + 100, (c - 3) * 8, 0, 3, oyc.inst[12], nyc.inst[12]); // WV
            oyc.inst[13] = frameBuffer.font4Int2(516 + 112, (c - 3) * 8, 0, 0, oyc.inst[13], nyc.inst[13]); // Oct
            oyc.inst[14] = frameBuffer.font4Hex12Bit(516 + 128, (c - 3) * 8, 0, oyc.inst[14], nyc.inst[14]);   // F-Num

            oyc.mask = drawChYMF278B(frameBuffer, c, oyc.mask, nyc.mask, tp);
            oyc.volumeL = frameBuffer.drawVolumeXY(113, (c - 3) * 2 + 0, 1, oyc.volumeL, nyc.volumeL, tp);
            oyc.volumeR = frameBuffer.drawVolumeXY(113, (c - 3) * 2 + 1, 1, oyc.volumeR, nyc.volumeR, tp);
            oyc.note = drawKeyBoardToYMF278BPCM(frameBuffer, c - 4, oyc.note, nyc.note, tp);
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
                    for (ch = 0; ch < 47; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(YmF278BChip.class, chipId, ch);
                        else
                            parent.setChannelMask(YmF278BChip.class, chipId, ch);
                    }
                }
                return;
            }

            // Keyboard FM & RHM
            ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ch == 18) {
                int x = (px / 4 - 10);
                if (x < 0) return;
                x /= 13;
                if (x > 4) return;
                ch += x;
            } else if (ch > 18) {
                ch += 4;
            }

            if (ev.getButton() == MouseEvent.BUTTON1) {
                if (ch < 18) {
                    parent.getInstCh(YmF278BChip.class, ch, chipId);
                }

                // Mask.
                parent.setChannelMask(YmF278BChip.class, chipId, ch);
                return;
            }

            // Unmask.
            for (ch = 0; ch < 47; ch++) parent.resetChannelMask(YmF278BChip.class, chipId, ch);
        }
    };

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeYMF278B");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(656, 352));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmYMF278B
        //
        this.setPreferredSize(new Dimension(656, 352));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmYMF278B");
        this.setTitle("YMF278B");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static void drawScreenInitYMF278B(FrameBuffer screen, int tp) {
        for (int y = 0; y < 18; y++) {
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
            d = screen.drawVolumeM(256, 8 + y * 8, 0, d, 19, tp);
        }

        for (int y = 19; y < 19 + 24; y++) {
            // Note
            screen.drawFont8(296, y * 8 + 8, 1, "   ");

            // Keyboard
            for (int i = 0; i < 15 * 12; i++) {
                int kx = Tables.kbl[(i % 12) * 2] + i / 12 * 28;
                int kt = Tables.kbl[(i % 12) * 2 + 1];
                screen.drawKbn(32 + kx, y * 8 + 8, kt, tp);
            }

            // Volume
            int d = 99;
            d = drawVolumeSt(screen, 512 - 4 * 15, y, 1, d, 19);
            d = 99;
            d = drawVolumeSt(screen, 512 - 4 * 15, y + 4, 1, d, 19);
        }
    }

    private static int drawVolumeSt(FrameBuffer screen, int x, int y, int c, int ov, int nv) {
        if (ov == nv)
            return ov;

        int t = 0;
        int sy = 0;
        if (c == 1 || c == 2) {
            t = 4;
        }
        if (c == 2) {
            sy = 4;
        }
        y = (y + 1) * 8;

        // x=256
        for (int i = 0; i <= 19; i++) {
            screen.drawVolumeP(x + i * 2, y + sy, (1 + t), 0);
        }

        for (int i = 0; i <= nv; i++) {
            screen.drawVolumeP(x + i * 2, y + sy, i > 17 ? (2 + t) : (0 + t), 0);
        }

        ov = nv;
        return ov;
    }

    private static int drawKeyBoardToYMF278BPCM(FrameBuffer screen, int y, int ot, int nt, int tp) {
        if (ot == nt)
            return ot;

        int kx;
        int kt;

        y = (y + 1) * 8;

        if (ot >= 0 && ot < 12 * 15) {
            kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
            kt = Tables.kbl[(ot % 12) * 2 + 1];
            screen.drawKbn(32 + kx, y, kt, tp);
        }

        if (nt >= 0 && nt < 12 * 15) {
            kx = Tables.kbl[(nt % 12) * 2] + nt / 12 * 28;
            kt = Tables.kbl[(nt % 12) * 2 + 1] + 4;
            screen.drawKbn(32 + kx, y, kt, tp);
        }

        screen.drawFont8(300 + 8 * 24, y, 1, "   ");

        if (nt >= 0) {
            screen.drawFont8(300 + 8 * 24, y, 1, Tables.kbn[nt % 12]);
            if (nt / 12 < 15) {
                screen.drawFont8(300 + 8 * 26, y, 1, Tables.kbo[nt / 12]);
            }
        }

        ot = nt;
        return ot;
    }

    private static Boolean drawChYMF278B(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }
        drawChYMF278B_P(screen,
                0,
                ch < 18 ? (8 + ch * 8) : (ch < 23 ? (8 + 18 * 8) : (8 + (ch - 4) * 8)),
                YMF278BCh[ch],
                nm == null ? false : nm,
                tp);
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

    private static void drawChYMF278B_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        if (ch < 18) {
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 0, 16, 8);
            screen.drawFont4(x + 16, y, mask ? 1 : 0, "%2d".formatted(1 + ch));
        } else if (ch < 23) {
            switch (ch) {
                case 18:
                    screen.drawFont4((ch - 18) * 4 * 13 + 10 * 4, y, mask ? 1 : 0, "BD");
                    break;
                case 19:
                    screen.drawFont4((ch - 18) * 4 * 13 + 10 * 4, y, mask ? 1 : 0, "SD");
                    break;
                case 20:
                    screen.drawFont4((ch - 18) * 4 * 13 + 10 * 4, y, mask ? 1 : 0, "TM");
                    break;
                case 21:
                    screen.drawFont4((ch - 18) * 4 * 13 + 9 * 4, y, mask ? 1 : 0, "CYM"); // 3 character
                    break;
                case 22:
                    screen.drawFont4((ch - 18) * 4 * 13 + 10 * 4, y, mask ? 1 : 0, "HH");
                    break;
            }
        } else {
            screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 16, 0, 16, 8);
            ch -= 23;
            screen.drawFont4(x + 16, y, mask ? 1 : 0, "%2d".formatted(1 + ch));
        }
    }

    private static final byte[] YMF278BCh = {
            0, 3, 1, 4, 2, 5, 6, 7, 8, 9, 12, 10, 13, 11, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
            32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46
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
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), // Rhythm 5
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel() // PCM 24
        };
    }


    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "YMF278B"; }
        @Override public String menuText() { return "OPL4"; }
        @Override public String category() { return "opl"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return YmF278BChip.class; }
        @Override public boolean hasRegisterDump() { return true; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormYMF278B(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 47) {
                YmF278BChip c = audio.plugin.chipRegister.chip(YmF278BChip.class);
                if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(YmF278BChip.class).resetMask(chipId, ch);
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (ch >= 0 && ch < 47) {
                if (mask)
                    audio.plugin.chipRegister.chip(YmF278BChip.class).setMask(chipId, ch);
                else
                    audio.plugin.chipRegister.chip(YmF278BChip.class).resetMask(chipId, ch);
            }
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(21, MDSound.Chip.MAIN_TAG, YmF278BChip.class, "ymf278b", 200));
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
