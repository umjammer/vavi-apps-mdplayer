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
import mdplayer.chips.Sn76489Chip;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.form.View;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;
import mdsound.instrument.Sn76489Inst;

import static mdplayer.Common.searchSSGNote;


public class FormSN76489 extends FormChipBase<FormSN76489.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormSN76489.class);

    public FormSN76489(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeSN76489"), null, zoom);
        boolean SN76489Type = (chipId == 0) ? parent.setting.getSN76489Type()[0].getUseReal()[0] : parent.setting.getSN76489Type()[1].getUseReal()[0];
        int tp = SN76489Type ? 1 : 0;
        drawScreenInitSN76489(frameBuffer, tp);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("SN76489", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeSN76489").getWidth() * zoom, frameSizeH + Common.getImage("planeSN76489").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeSN76489").getWidth() * zoom, frameSizeH + Common.getImage("planeSN76489").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeSN76489").getWidth() * zoom, frameSizeH + Common.getImage("planeSN76489").getHeight() * zoom));
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
        Map<String, Object> info = audio.plugin.chipRegister.chip(Sn76489Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int[] register = (int[]) info.get("register");
        int registerPan = (int) info.get("pan");
        int[][] vol = (int[][]) info.get("volumes");
        boolean ngpFlag = (boolean) info.get("flag");
        newParam.ngp = ngpFlag;

        if (ngpFlag && chipId == 1) {
            for (int ch = 0; ch < 4; ch++) {
                newParam.channels[ch].note = -1;

                newParam.channels[ch].volumeL = 0;
                newParam.channels[ch].volumeR = 0;
                newParam.channels[ch].pan = 0;
            }
            // Noise Ch
            newParam.channels[3].freq = 0;
        } else {
            if (register != null) {
                if (ngpFlag) {
                    int[][] psgVol1 = (int[][]) audio.plugin.chipRegister.chip(Sn76489Chip.class).getInfo(1).get("volumes");
                    int[] psgRegister1 = (int[]) audio.plugin.chipRegister.chip(Sn76489Chip.class).getInfo(1).get("register");

                    // Tone Ch
                    for (int ch = 0; ch < 3; ch++) {
                        if (register[ch * 2 + 1] != 15) {
                            float fTone = clock(Sn76489Inst.class) / (2.0f * register[ch * 2] * 16.0f);

                            newParam.channels[ch].note = searchSSGNote(fTone);
                        } else {
                            newParam.channels[ch].note = -1;
                        }

                        newParam.channels[ch].volumeL = Math.clamp((int) ((vol[ch][0]) / (15.0 / 19.0)), 0, 19);
                        newParam.channels[ch].volumeR = Math.clamp((int) ((psgVol1[ch][0]) / (15.0 / 19.0)), 0, 19);
                        newParam.channels[ch].pan = Math.clamp(newParam.channels[ch].volumeR, 0, 15) * 0x10 +
                                Math.clamp(newParam.channels[ch].volumeL, 0, 15);
                    }

                    // Noise Ch
                    newParam.channels[3].note = psgRegister1[6];
                    newParam.channels[3].freq = psgRegister1[4];//ch3Freq
                    newParam.channels[3].volumeL = Math.clamp((int) ((vol[3][0]) / (15.0 / 19.0)), 0, 19);
                    newParam.channels[3].volumeR = Math.clamp((int) ((psgVol1[3][0]) / (15.0 / 19.0)), 0, 19);
                    newParam.channels[3].pan = Math.clamp(newParam.channels[3].volumeR, 0, 15) * 0x10 +
                            Math.clamp(newParam.channels[3].volumeL, 0, 15);
                } else {
                    // Tone Ch
                    for (int ch = 0; ch < 3; ch++) {
                        if (register[ch * 2 + 1] != 15) {
                            newParam.channels[ch].note = searchPSGNote(register[ch * 2]);
                        } else {
                            newParam.channels[ch].note = -1;
                        }

                        newParam.channels[ch].volumeL = Math.clamp((int) ((vol[ch][0]) / (15.0 / 19.0)), 0, 19);
                        newParam.channels[ch].volumeR = Math.clamp((int) ((vol[ch][1]) / (15.0 / 19.0)), 0, 19);
                        newParam.channels[ch].pan = (registerPan >> ch) & 0x11;
                        newParam.channels[ch].pan = ((newParam.channels[ch].pan) & 0x1) | (newParam.channels[ch].pan >> 3);
                    }

                    // Noise Ch
                    newParam.channels[3].note = register[6];
                    newParam.channels[3].freq = register[4];//ch3Freq
                    newParam.channels[3].volumeL = Math.clamp((int) ((vol[3][0]) / (15.0 / 19.0)), 0, 19);
                    newParam.channels[3].volumeR = Math.clamp((int) ((vol[3][1]) / (15.0 / 19.0)), 0, 19);
                    newParam.channels[3].pan = (registerPan >> 3) & 0x11;
                    newParam.channels[3].pan = ((newParam.channels[3].pan) & 0x1) | (newParam.channels[3].pan >> 3);
                }
            }
        }

        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(Sn76489Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        boolean sn76489Type = (chipId == 0) ? parent.setting.getSN76489Type()[0].getUseReal()[0] : parent.setting.getSN76489Type()[1].getUseReal()[0];
        int tp = sn76489Type ? 1 : 0;
        ChannelParams osc;
        ChannelParams nsc;
        // read by changeScreenParams: this also runs with no song loaded, when there is no chip to ask
        boolean ngpFlag = newParam.ngp;

        for (int c = 0; c < 3; c++) {
            osc = oldParam.channels[c];
            nsc = newParam.channels[c];

            osc.volumeL = frameBuffer.drawVolumeM(256, 8 + c * 8, 1, osc.volumeL, nsc.volumeL, tp);
            osc.volumeR = frameBuffer.drawVolumeM(256, 8 + c * 8, 2, osc.volumeR, nsc.volumeR, tp);
            osc.note = frameBuffer.drawKeyBoard(c, osc.note, nsc.note, tp);
            osc.mask = drawChSN76489(frameBuffer, c, osc.mask, nsc.mask, tp);
            if (ngpFlag) {
                osc.pan = frameBuffer.PanType2(c, osc.pan, nsc.pan, tp);
            } else {
                int[] r = frameBuffer.pan(24, 8 + c * 8, osc.pan, nsc.pan, osc.pantp, tp);
                osc.pan = r[0]; osc.pantp = r[1];
            }
        }

        osc = oldParam.channels[3];
        nsc = newParam.channels[3];
        osc.volumeL = frameBuffer.drawVolumeM(256, 8 + 3 * 8, 1, osc.volumeL, nsc.volumeL, tp);
        osc.volumeR = frameBuffer.drawVolumeM(256, 8 + 3 * 8, 2, osc.volumeR, nsc.volumeR, tp);
        osc.mask = drawChSN76489(frameBuffer, 3, osc.mask, nsc.mask, tp);
        drawChSN76489Noise(frameBuffer, osc, nsc, tp);
        if (ngpFlag) {
            osc.pan = frameBuffer.PanType2(3, osc.pan, nsc.pan, tp);
        } else {
            int[] r =  frameBuffer.pan(24, 8 + 3 * 8, osc.pan, nsc.pan, osc.pantp, tp);
            osc.pan = r[0]; osc.pantp = r[1];
        }
        if (osc.freq != nsc.freq) {
            frameBuffer.drawFont4(172, 32, 0, "%04d".formatted(nsc.freq));
            osc.freq = nsc.freq;
        }
    }

    @Override
    public void initScreen() {
        for (int ch = 0; ch < 3; ch++) {
            newParam.channels[ch].note = -1;

            newParam.channels[ch].volumeL = 0;
            newParam.channels[ch].volumeR = 0;
            newParam.channels[ch].pan = 0;
        }

        newParam.channels[3].note = 0;
        newParam.channels[3].freq = 0;
        newParam.channels[3].volumeL = 0;
        newParam.channels[3].volumeR = 0;
        newParam.channels[3].pan = 0;
    }

    private final MouseListener pbScreen_MouseClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
            int py = ev.getY() / zoom;
            int px = ev.getX() / zoom;

            // For top label row, do nothing
            if (py < 1 * 8) {
                // However, if you click on ch, the mask will be inverted.
                if (px < 8) {
                    for (int ch = 0; ch < 4; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(Sn76489Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(Sn76489Chip.class, chipId, ch);
                    }
                }
                return;
            }

            // keyboard
            if (py < 5 * 8) {
                int ch = (py / 8) - 1;
                if (ch < 0) return;

                boolean ngpFlag = newParam.ngp;

                if (ev.getButton() == MouseEvent.BUTTON1) {
                    // Mask.
                    parent.setChannelMask(Sn76489Chip.class, chipId, ch);
                    if (ngpFlag && chipId == 0) parent.setChannelMask(Sn76489Chip.class, 1, ch);
                    return;
                }

                // Unmask.
                for (ch = 0; ch < 4; ch++) {
                    parent.resetChannelMask(Sn76489Chip.class, chipId, ch);
                    if (ngpFlag && chipId == 0) parent.resetChannelMask(Sn76489Chip.class, 1, ch);
                }
            }
        }
    };

    private static int searchPSGNote(int freq) {
        int m = Integer.MAX_VALUE;
        int n = 0;

        for (int i = 0; i < 12 * 8; i++) {
            int a = Math.abs(freq - Tables.PsgFNum[i]);

            if (m > a) {
                m = a;
                n = i;
            }
        }

        return n;
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeSN76489");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 40));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmSN76489
        //
        this.setPreferredSize(new Dimension(320, 40));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmSN76489");
        this.setTitle("SN76489");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static void drawScreenInitSN76489(FrameBuffer screen, int tp) {
        for (int ch = 0; ch < 4; ch++) {
            if (ch != 3) {
                for (int ot = 0; ot < 12 * 8; ot++) {
                    int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                    int kt = Tables.kbl[(ot % 12) * 2 + 1];
                    screen.drawKbn(32 + kx, ch * 8 + 8, kt, tp);
                }
            } else {
            }

            screen.drawFont8(296, ch * 8 + 8, 1, "   ");
            drawChSN76489_P(screen, 0, ch * 8 + 8, ch, false, tp);

            int d = 99;
            d = screen.drawVolumeM(256, 8 + ch * 8, 0, d, 0, tp);
        }
    }

    private static Boolean drawChSN76489(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChSN76489_P(screen, 0, 8 + ch * 8, ch, nm != null && nm, tp);
        om = nm;
        return om;
    }

    private static void drawChSN76489Noise(FrameBuffer screen, ChannelParams osc, ChannelParams nsc, int tp) {
        if (osc.note == nsc.note)
            return;

        screen.drawFont4(56, 32, tp, (nsc.note & 0x4) != 0 ? "WHITE   " : "PERIODIC");
        screen.drawFont4(120,
                32,
                tp,
                (nsc.note & 0x3) == 0 ? "0  " : ((nsc.note & 0x3) == 1 ? "1  " : ((nsc.note & 0x3) == 2 ? "2  " : "CH3")));

        osc.note = nsc.note;
    }

    private static void drawChSN76489_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 32, 0, 16, 8);
        screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(1 + ch));
    }

//#endregion

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {
        final ChannelParams[] channels = {new ChannelParams(), new ChannelParams(), new ChannelParams(), new ChannelParams()};
        /** the NeoGeo Pocket pairing: T6W28 stereo, chip 1 carrying chip 0's noise */
        boolean ngp = false;
    }


    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "SN76489"; }
        @Override public String menuText() { return "DCSG"; }
        @Override public String category() { return "psg"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return Sn76489Chip.class; }
        @Override public boolean hasRegisterDump() { return true; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormSN76489(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            Sn76489Chip c = audio.plugin.chipRegister.chip(Sn76489Chip.class);
            if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(Sn76489Chip.class).resetMask(chipId, ch);
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (mask)
                audio.plugin.chipRegister.chip(Sn76489Chip.class).setMask(chipId, ch);
            else
                audio.plugin.chipRegister.chip(Sn76489Chip.class).resetMask(chipId, ch);
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 4; ch++)
                forceChannelMask(audio, Sn76489Chip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(Sn76489Chip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(26, MDSound.Chip.MAIN_TAG, Sn76489Chip.class, "sn76489", 120));
        }
    }
}
