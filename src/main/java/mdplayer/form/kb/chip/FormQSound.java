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
import mdplayer.chips.QSoundChip;
import mdplayer.chips.SegaPcmChip;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.form.View;
import mdplayer.form.kb.PcmChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;


public class FormQSound extends FormChipBase<FormQSound.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormQSound.class);

    public FormQSound(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        bind(Common.getImage("planeQSound"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("QSound", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeQSound").getWidth() * zoom, frameSizeH + Common.getImage("planeQSound").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeQSound").getWidth() * zoom, frameSizeH + Common.getImage("planeQSound").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeQSound").getWidth() * zoom, frameSizeH + Common.getImage("planeQSound").getHeight() * zoom));
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
                    for (ch = 0; ch < 19; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(QSoundChip.class, chipId, ch);
                        else
                            parent.setChannelMask(QSoundChip.class, chipId, ch);
                    }
                }
                return;
            }

            ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ch < 19) {
                if (ev.getButton() == MouseEvent.BUTTON1) {
                    parent.setChannelMask(QSoundChip.class, chipId, ch);
                    return;
                }

                for (ch = 0; ch < 19; ch++) parent.resetChannelMask(QSoundChip.class, chipId, ch);
            }
        }
    };

    @Override
    public void initScreen() {
        for (int ch = 0; ch < 16; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                frameBuffer.drawKbn(32 + kx, ch * 8 + 8, kt, 0);
            }
        }
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(QSoundChip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int[] register = (int[]) info.get("register");

        // PCM 16ch
        for (int ch = 0; ch < 16; ch++) {
            newParam.channels[ch].echo = register[ch + 0xba];
            newParam.channels[ch].freq = register[(ch << 3) + 2];
            newParam.channels[ch].bank = register[(((ch + 15) % 16) << 3) + 0];
            newParam.channels[ch].sadr = register[(ch << 3) + 1];
            newParam.channels[ch].eadr = register[(ch << 3) + 5];
            newParam.channels[ch].ladr = register[(ch << 3) + 4];
            //newParam.channels[ch].ladr = register[(ch << 3) + 3];
            int vol = register[(ch << 3) + 6];
            int pan = register[ch + 0x80] - 0x110;
            if (pan >= 97) pan = 16; // center?
            int panL = (int) (15.0 / 16.0 * (pan > 16 ? (16 - (33 - pan)) : 16));
            int panR = (int) (15.0 / 16.0 * (pan < 16 ? (16 - pan) : 16));
            newParam.channels[ch].pan = (panR << 4) | panL;
            newParam.channels[ch].volumeL = Math.clamp(vol * panL / 256 / 16, 0, 19);
            newParam.channels[ch].volumeR = Math.clamp(vol * panR / 256 / 16, 0, 19);

            newParam.channels[ch].note = Math.clamp(SegaPcmChip.searchSegaPCMNote(newParam.channels[ch].freq / 16.0 / 166.0), 0, 7 * 12);
            if (vol == 0) newParam.channels[ch].note = -1;
        }
        // ADPCM 3ch
        for (int ch = 0; ch < 3; ch++) {
            newParam.channels[ch + 16].bank = register[(ch << 2) + 0xcc];
            newParam.channels[ch + 16].sadr = register[(ch << 2) + 0xca];
            newParam.channels[ch + 16].eadr = register[(ch << 2) + 0xcb];
            int vol = (register[(ch << 2) + 0xcd] >> 16);
            int pan = register[ch + 16 + 0x80] - 0x110;
            if (pan >= 97) pan = 16; // center?
            int panL = (int) (15.0 / 16.0 * (pan > 16 ? (16 - (33 - pan)) : 16));
            int panR = (int) (15.0 / 16.0 * (pan < 16 ? (16 - pan) : 16));
            newParam.channels[ch + 16].pan = (panR << 4) | panL;
            newParam.channels[ch + 16].volumeL = Math.clamp(vol * panL / 10, 0, 19);
            newParam.channels[ch + 16].volumeR = Math.clamp(vol * panR / 10, 0, 19);
        }

        // echo
        newParam.channels[0].inst[0] = register[0x93]; // feedback
        newParam.channels[0].inst[1] = register[0xd9]; // end_pos
        newParam.channels[0].inst[2] = register[0xe2]; // delay_update
        newParam.channels[0].inst[3] = register[0xe3]; // next_state
        // Wet
        newParam.channels[0].inst[4] = register[0xde]; // delay left
        newParam.channels[0].inst[5] = register[0xe0]; // delay right
        newParam.channels[0].inst[6] = register[0xe4]; // volume_left
        newParam.channels[0].inst[7] = register[0xe6]; // volume right
        // Dry
        newParam.channels[0].inst[8] = register[0xdf];  // delay left
        newParam.channels[0].inst[9] = register[0xe1];  // delay right
        newParam.channels[0].inst[10] = register[0xe5]; // volume_left
        newParam.channels[0].inst[11] = register[0xe7]; // volume right
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(QSoundChip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        Channel oyc;
        Channel nyc;

        // PCM 16ch
        for (int ch = 0; ch < 16; ch++) {
            oyc = oldParam.channels[ch];
            nyc = newParam.channels[ch];

            oyc.echo = frameBuffer.font4Hex16Bit(4 * 65, ch * 8 + 8, 0, oyc.echo, nyc.echo);
            oyc.freq = frameBuffer.font4Hex16Bit(4 * 70, ch * 8 + 8, 0, oyc.freq, nyc.freq);
            oyc.bank = frameBuffer.font4Hex16Bit(4 * 75, ch * 8 + 8, 0, oyc.bank, nyc.bank);
            oyc.sadr = frameBuffer.font4Hex16Bit(4 * 80, ch * 8 + 8, 0, oyc.sadr, nyc.sadr);
            oyc.eadr = frameBuffer.font4Hex16Bit(4 * 85, ch * 8 + 8, 0, oyc.eadr, nyc.eadr);
            oyc.ladr = frameBuffer.font4Hex16Bit(4 * 90, ch * 8 + 8, 0, oyc.ladr, nyc.ladr);
            oyc.pan = frameBuffer.PanType2(ch, oyc.pan, nyc.pan, 0);
            oyc.volumeL = frameBuffer.drawVolumeXY(94, ch * 2 + 2, 1, oyc.volumeL, nyc.volumeL, 0);
            oyc.volumeR = frameBuffer.drawVolumeXY(94, ch * 2 + 3, 1, oyc.volumeR, nyc.volumeR, 0);
            oyc.note = drawKeyBoardToQSound(frameBuffer, ch, oyc.note, nyc.note, 0);

            oyc.mask = drawChQSound(frameBuffer, ch, oyc.mask, nyc.mask, 0);
        }
        // ADPCM 3ch
        for (int ch = 0; ch < 3; ch++) {
            oyc = oldParam.channels[ch + 16];
            nyc = newParam.channels[ch + 16];
            oyc.bank = frameBuffer.font4Hex16Bit(4 * 75, (ch + 16) * 8 + 8, 0, oyc.bank, nyc.bank);
            oyc.sadr = frameBuffer.font4Hex16Bit(4 * 80, (ch + 16) * 8 + 8, 0, oyc.sadr, nyc.sadr);
            oyc.eadr = frameBuffer.font4Hex16Bit(4 * 85, (ch + 16) * 8 + 8, 0, oyc.eadr, nyc.eadr);
            //frameBuffer.PanType2((ch + 16),oyc.pan, nyc.pan, 0);
            oyc.volumeL = frameBuffer.drawVolumeXY(94, (ch + 16) * 2 + 2, 1, oyc.volumeL, nyc.volumeL, 0);
            oyc.volumeR = frameBuffer.drawVolumeXY(94, (ch + 16) * 2 + 3, 1, oyc.volumeR, nyc.volumeR, 0);

            oyc.mask = drawChQSound(frameBuffer, ch + 16, oyc.mask, nyc.mask, 0);
        }

        // echo
        oldParam.channels[0].inst[0] = frameBuffer.font4Hex16Bit(4 * 36, 17 * 8 + 8, 0, oldParam.channels[0].inst[0], newParam.channels[0].inst[0]);//feedback
        oldParam.channels[0].inst[1] = frameBuffer.font4Hex16Bit(4 * 36, 18 * 8 + 8, 0, oldParam.channels[0].inst[1], newParam.channels[0].inst[1]);//end_pos
        oldParam.channels[0].inst[2] = frameBuffer.font4Hex16Bit(4 * 51, 17 * 8 + 8, 0, oldParam.channels[0].inst[2], newParam.channels[0].inst[2]);//delay_update
        oldParam.channels[0].inst[3] = frameBuffer.font4Hex16Bit(4 * 51, 18 * 8 + 8, 0, oldParam.channels[0].inst[3], newParam.channels[0].inst[3]);//next_state

        // Wet
        oldParam.channels[0].inst[4] = frameBuffer.font4Hex16Bit(4 * 7, 17 * 8 + 8, 0, oldParam.channels[0].inst[4], newParam.channels[0].inst[4]);//delay l
        oldParam.channels[0].inst[5] = frameBuffer.font4Hex16Bit(4 * 12, 17 * 8 + 8, 0, oldParam.channels[0].inst[5], newParam.channels[0].inst[5]);//delay r
        oldParam.channels[0].inst[6] = frameBuffer.font4Hex16Bit(4 * 7, 18 * 8 + 8, 0, oldParam.channels[0].inst[6], newParam.channels[0].inst[6]);//vol l
        oldParam.channels[0].inst[7] = frameBuffer.font4Hex16Bit(4 * 12, 18 * 8 + 8, 0, oldParam.channels[0].inst[7], newParam.channels[0].inst[7]);//vol r

        // Dry
        oldParam.channels[0].inst[8] = frameBuffer.font4Hex16Bit(4 * 18, 17 * 8 + 8, 0, oldParam.channels[0].inst[8], newParam.channels[0].inst[8]);//delay l
        oldParam.channels[0].inst[9] = frameBuffer.font4Hex16Bit(4 * 23, 17 * 8 + 8, 0, oldParam.channels[0].inst[9], newParam.channels[0].inst[9]);//delay r
        oldParam.channels[0].inst[10] = frameBuffer.font4Hex16Bit(4 * 18, 18 * 8 + 8, 0, oldParam.channels[0].inst[10], newParam.channels[0].inst[10]);//vol l
        oldParam.channels[0].inst[11] = frameBuffer.font4Hex16Bit(4 * 23, 18 * 8 + 8, 0, oldParam.channels[0].inst[11], newParam.channels[0].inst[11]);//vol r
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeQSound");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(440, 177));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmQSound
        //
        this.setPreferredSize(new Dimension(440, 177));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmQSound");
        this.setTitle("QSoundInst");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static int drawKeyBoardToQSound(FrameBuffer screen, int y, int ot, int nt, int tp) {
        if (ot == nt)
            return ot;

        int kx;
        int kt;

        y = (y + 1) * 8;

        if (ot >= 0) {
            kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
            kt = Tables.kbl[(ot % 12) * 2 + 1];
            screen.drawKbn(32 + kx, y, kt, tp);
        }

        int x = 52;
        if (nt >= 0) {
            kx = Tables.kbl[(nt % 12) * 2] + nt / 12 * 28;
            kt = Tables.kbl[(nt % 12) * 2 + 1] + 4;
            screen.drawKbn(32 + kx, y, kt, tp);
            screen.drawFont8(x * 8, y, 1, Tables.kbn[nt % 12]);
            if (nt / 12 < 8) {
                screen.drawFont8((x + 2) * 8, y, 1, Tables.kbo[nt / 12]);
            }
        } else {
            screen.drawFont8(x * 8, y, 1, "   ");
        }

        ot = nt;
        return ot;
    }

    private static Boolean drawChQSound(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }
        if (ch < 16) {
            drawChQSound_P(screen, 0, 8 + ch * 8, ch, nm != null && nm, tp);
        } else {
            drawChQSoundAdpcm_P(screen, 224, 8 + ch * 8, ch - 16, nm != null && nm, tp);
        }
        om = nm;
        return om;
    }

    private static void drawChQSound_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 16, 0, 16, 8);
        //if (ch < 9) screen.drawFont8(x + 16, y, mask ? 1 : 0, (1 + ch).toString());
        //else
        screen.drawFont4(x + 16, y, mask ? 1 : 0, "%2d".formatted(1 + ch));
    }

    private static void drawChQSoundAdpcm_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 88, 0, 20, 8);
        //if (ch < 9) screen.drawFont8(x + 16, y, mask ? 1 : 0, (1 + ch).toString());
        //else
        screen.drawFont4(x + 20, y, mask ? 1 : 0, "%1d".formatted(1 + ch));
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends PcmChannelParams {

        int echo = -1;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final Channel[] channels = {
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(),

                new Channel(), new Channel(), new Channel()
        };
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "QSound"; }
        @Override public String category() { return "pcm"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return QSoundChip.class; }
        @Override public boolean hasRegisterDump() { return true; }
        @Override public String title(int chipId) { return "QSoundInst (%s)".formatted(chipId == 0 ? "Primary" : "Secondary"); }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormQSound(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            QSoundChip c = audio.plugin.chipRegister.chip(QSoundChip.class);
            if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 19) {
                audio.plugin.chipRegister.chip(QSoundChip.class).resetMask(chipId, ch);
            }
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (mask)
                audio.plugin.chipRegister.chip(QSoundChip.class).setMask(chipId, ch);
            else
                audio.plugin.chipRegister.chip(QSoundChip.class).resetMask(chipId, ch);
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 19; ch++)
                forceChannelMask(audio, QSoundChip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(QSoundChip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(46, MDSound.Chip.MAIN_TAG, QSoundChip.class, "qSound", 200));
        }
    }
}
