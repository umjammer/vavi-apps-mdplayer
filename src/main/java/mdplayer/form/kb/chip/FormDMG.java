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
import mdplayer.chips.DmgChip;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdplayer.form.View;
import mdsound.MDSound;


public class FormDMG extends FormChipBase<FormDMG.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormDMG.class);

    public FormDMG(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        bind(Common.getImage("planeDMG"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("DMG", chipId, getLocation());
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
        setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeDMG").getWidth() * zoom, frameSizeH + Common.getImage("planeDMG").getHeight() * zoom));
        setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeDMG").getWidth() * zoom, frameSizeH + Common.getImage("planeDMG").getHeight() * zoom));
        setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeDMG").getWidth() * zoom, frameSizeH + Common.getImage("planeDMG").getHeight() * zoom));
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
        Map<String, Object> info = audio.plugin.chipRegister.chip(DmgChip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        // pan
        newParam.channels[0].pan = (int) info.get("channels.0.pan");
        newParam.channels[1].pan = (int) info.get("channels.1.pan");
        newParam.channels[2].pan = (int) info.get("channels.2.pan");
        newParam.channels[3].pan = (int) info.get("channels.3.pan");

        // freq
        newParam.channels[0].freq = (int) info.get("channels.0.freq");
        newParam.channels[1].freq = (int) info.get("channels.1.freq");
        newParam.channels[2].freq = (int) info.get("channels.2.freq");
        newParam.channels[3].freq = (int) info.get("channels.3.freq");
        newParam.channels[3].bit[47] = (boolean) info.get("channels.3.bit.47");
        newParam.channels[3].srcFreq = (int) info.get("channels.3.srcFreq");

        // CC
        newParam.channels[0].bit[0] = (boolean) info.get("channels.0.bit.0");
        newParam.channels[1].bit[0] = (boolean) info.get("channels.1.bit.0");
        newParam.channels[2].bit[0] = (boolean) info.get("channels.2.bit.0");
        newParam.channels[3].bit[0] = (boolean) info.get("channels.3.bit.0");

        // Ini
        newParam.channels[0].bit[1] = (boolean) info.get("channels.0.bit.1");
        newParam.channels[1].bit[1] = (boolean) info.get("channels.1.bit.1");
        newParam.channels[2].bit[1] = (boolean) info.get("channels.2.bit.1");
        newParam.channels[3].bit[1] = (boolean) info.get("channels.3.bit.1");

        // Env.Dir
        newParam.channels[0].bit[2] = (boolean) info.get("channels.0.bit.2");
        newParam.channels[1].bit[2] = (boolean) info.get("channels.1.bit.2");
        //newParam.channels[2].bit[2] = nothing
        newParam.channels[3].bit[2] = (boolean) info.get("channels.3.bit.2");

        // Sweep Dec
        newParam.channels[0].bit[3] = (boolean) info.get("channels.0.bit.3");

        // Env.Spd
        newParam.channels[0].inst[0] = (int) info.get("channels.0.inst.0");
        newParam.channels[1].inst[0] = (int) info.get("channels.1.inst.0");
        //newParam.channels[2].inst[0] = nothing
        newParam.channels[3].inst[0] = (int) info.get("channels.3.inst.0");

        // Env.Vol
        newParam.channels[0].inst[1] = (int) info.get("channels.0.inst.1");
        newParam.channels[1].inst[1] = (int) info.get("channels.1.inst.1");
        //newParam.channels[2].inst[1] = nothing
        newParam.channels[3].inst[1] = (int) info.get("channels.3.inst.1");

        // Len
        newParam.channels[0].inst[2] = (int) info.get("channels.0.inst.2");
        newParam.channels[1].inst[2] = (int) info.get("channels.1.inst.2");
        //newParam.channels[2].inst[2] = nothing
        newParam.channels[3].inst[2] = (int) info.get("channels.3.inst.2");

        // Duty
        newParam.channels[0].inst[3] = (int) info.get("channels.0.inst.3");
        newParam.channels[1].inst[3] = (int) info.get("channels.1.inst.3");
        // newParam.channels[2].inst[3] = nothing
        // newParam.channels[3].inst[3] = nothing

        // Sweep time
        newParam.channels[0].inst[4] = (int) info.get("channels.0.inst.4");
        // Sweep shift
        newParam.channels[0].inst[5] = (int) info.get("channels.0.inst.5");

        // Len
        newParam.channels[2].inst[4] = (int) info.get("channels.2.inst.4");
        // Vol
        newParam.channels[2].inst[5] = (int) info.get("channels.2.inst.5");

        // wf
        for (int i = 0; i < 16; i++) {
            newParam.wf[i * 2] = (byte) info.get("wf." + i * 2);
            newParam.wf[i * 2 + 1] = (byte) info.get("wf." + i * 2 + 1);
        }

        int r = 10;
        newParam.channels[0].volumeL = (int) info.get("channels.0.volumeL");
        newParam.channels[0].volumeR = (int) info.get("channels.0.volumeR");
        newParam.channels[1].volumeL = (int) info.get("channels.1.volumeL");
        newParam.channels[1].volumeR = (int) info.get("channels.1.volumeR");
        newParam.channels[2].volumeL = (int) info.get("channels.2.volumeL");
        newParam.channels[2].volumeR = (int) info.get("channels.2.volumeR");
        newParam.channels[3].volumeL = (int) info.get("channels.3.volumeL");
        newParam.channels[3].volumeR = (int) info.get("channels.3.volumeR");

        float ftone;

        for (int i = 0; i < 3; i++) {
            newParam.channels[i].note = -1;
            if (newParam.channels[i].volumeL != 0 || newParam.channels[i].volumeR != 0) {
                ftone = 4194304.0f / (4 * 2 * (2048.0f - (float) newParam.channels[i].freq));
                newParam.channels[i].note = Math.clamp(searchSSGNote(ftone), 0, 8 * 12);
            }
        }
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(DmgChip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        Channel oyc = oldParam.channels[0];
        Channel nyc = newParam.channels[0];
        { int[] r = frameBuffer.pan(24, 8, oyc.pan, nyc.pan, oyc.pantp, 0); oyc.pan = r[0]; oyc.pantp = r[1]; }
        oyc.freq = frameBuffer.font4Hex12Bit(260, 8, 0, oyc.freq, nyc.freq);
        oyc.volumeL = frameBuffer.drawVolumeXY(68, 2, 1, oyc.volumeL, nyc.volumeL, 0);
        oyc.volumeR = frameBuffer.drawVolumeXY(68, 3, 1, oyc.volumeR, nyc.volumeR, 0);
        oyc.bit[0] = frameBuffer.drawNESSw(60, 40, oyc.bit[0], nyc.bit[0]); // CC
        oyc.bit[1] = frameBuffer.drawNESSw(60, 48, oyc.bit[1], nyc.bit[1]); // Ini
        oyc.bit[2] = frameBuffer.drawNESSw(28, 64, oyc.bit[2], nyc.bit[2]); // Env.Dir
        oyc.bit[3] = frameBuffer.drawNESSw(88, 56, oyc.bit[3], nyc.bit[3]); // Sweep Dec
        oyc.inst[0] = frameBuffer.font4Int1(28, 48, 0, oyc.inst[0], nyc.inst[0]); // Env. Spd
        oyc.inst[1] = frameBuffer.font4Int2(24, 56, 0, 1, oyc.inst[1], nyc.inst[1]); // Env. Vol
        oyc.inst[2] = frameBuffer.font4Int2(56, 64, 0, 1, oyc.inst[2], nyc.inst[2]); // Len
        oyc.inst[3] = frameBuffer.font4Int1(60, 56, 0, oyc.inst[3], nyc.inst[3]); // Duty
        oyc.inst[4] = frameBuffer.font4Int1(88, 48, 0, oyc.inst[4], nyc.inst[4]); // Sweep time
        oyc.inst[5] = frameBuffer.font4Int1(88, 64, 0, oyc.inst[5], nyc.inst[5]); // Sweep shift
        oyc.note = drawKeyBoardDMG(frameBuffer, 0, oyc.note, nyc.note, 0);
        oyc.mask = drawChDMG(frameBuffer, 0, oyc.mask, nyc.mask, 0);

        oyc = oldParam.channels[1];
        nyc = newParam.channels[1];
        { int[] r = frameBuffer.pan(24, 16, oyc.pan, nyc.pan, oyc.pantp, 0); oyc.pan = r[0]; oyc.pantp = r[1]; }
        oyc.freq = frameBuffer.font4Hex12Bit(260, 16, 0, oyc.freq, nyc.freq);
        oyc.volumeL = frameBuffer.drawVolumeXY(68, 4, 1, oyc.volumeL, nyc.volumeL, 0);
        oyc.volumeR = frameBuffer.drawVolumeXY(68, 5, 1, oyc.volumeR, nyc.volumeR, 0);
        oyc.bit[0] = frameBuffer.drawNESSw(152, 40, oyc.bit[0], nyc.bit[0]); // CC
        oyc.bit[1] = frameBuffer.drawNESSw(152, 48, oyc.bit[1], nyc.bit[1]); // Ini
        oyc.bit[2] = frameBuffer.drawNESSw(120, 64, oyc.bit[2], nyc.bit[2]); // Env.Dir
        oyc.inst[0] = frameBuffer.font4Int1(120, 48, 0, oyc.inst[0], nyc.inst[0]); // Env. Spd
        oyc.inst[1] = frameBuffer.font4Int2(116, 56, 0, 1, oyc.inst[1], nyc.inst[1]); // Env. Vol
        oyc.inst[2] = frameBuffer.font4Int2(148, 64, 0, 1, oyc.inst[2], nyc.inst[2]); // Len
        oyc.inst[3] = frameBuffer.font4Int1(152, 56, 0, oyc.inst[3], nyc.inst[3]); // Duty
        oyc.note = drawKeyBoardDMG(frameBuffer, 1, oyc.note, nyc.note, 0);
        oyc.mask = drawChDMG(frameBuffer, 1, oyc.mask, nyc.mask, 0);

        oyc = oldParam.channels[2];
        nyc = newParam.channels[2];
        { int[] r = frameBuffer.pan(24, 24, oyc.pan, nyc.pan, oyc.pantp, 0); oyc.pan = r[0]; oyc.pantp = r[1]; }
        oyc.freq = frameBuffer.font4Hex12Bit(260, 24, 0, oyc.freq, nyc.freq);
        oyc.volumeL = frameBuffer.drawVolumeXY(68, 6, 1, oyc.volumeL, nyc.volumeL, 0);
        oyc.volumeR = frameBuffer.drawVolumeXY(68, 7, 1, oyc.volumeR, nyc.volumeR, 0);
        oyc.bit[0] = frameBuffer.drawNESSw(228, 40, oyc.bit[0], nyc.bit[0]); // CC
        oyc.bit[1] = frameBuffer.drawNESSw(228, 48, oyc.bit[1], nyc.bit[1]); // Ini
        // no Env.Dir
        oyc.inst[4] = frameBuffer.font4Int2(220, 56, 0, 3, oyc.inst[4], nyc.inst[4]); // Len
        oyc.inst[5] = frameBuffer.font4Int1(228, 64, 0, oyc.inst[5], nyc.inst[5]); // Vol
        oyc.note = drawKeyBoardDMG(frameBuffer, 2, oyc.note, nyc.note, 0);
        oyc.mask = drawChDMG(frameBuffer, 2, oyc.mask, nyc.mask, 0);

        oyc = oldParam.channels[3];
        nyc = newParam.channels[3];
        { int[] r = frameBuffer.pan(24, 32, oyc.pan, nyc.pan, oyc.pantp, 0); oyc.pan = r[0]; oyc.pantp = r[1]; }
        oyc.volumeL = frameBuffer.drawVolumeXY(68, 8, 1, oyc.volumeL, nyc.volumeL, 0);
        oyc.volumeR = frameBuffer.drawVolumeXY(68, 9, 1, oyc.volumeR, nyc.volumeR, 0);
        oyc.freq = frameBuffer.font4Int1(316, 40, 0, oyc.freq, nyc.freq);
        oyc.bit[47] = frameBuffer.drawNESSw(316, 48, oyc.bit[47], nyc.bit[47]);
        oyc.srcFreq = frameBuffer.font4Int2(312, 56, 0, 1, oyc.srcFreq, nyc.srcFreq);
        oyc.bit[0] = frameBuffer.drawNESSw(288, 40, oyc.bit[0], nyc.bit[0]); // CC
        oyc.bit[1] = frameBuffer.drawNESSw(288, 48, oyc.bit[1], nyc.bit[1]); // Ini
        oyc.bit[2] = frameBuffer.drawNESSw(260, 64, oyc.bit[2], nyc.bit[2]); // Env.Dir
        oyc.inst[0] = frameBuffer.font4Int1(260, 48, 0, oyc.inst[0], nyc.inst[0]); // Env. Spd
        oyc.inst[1] = frameBuffer.font4Int2(256, 56, 0, 1, oyc.inst[1], nyc.inst[1]); // Env. Vol
        oyc.inst[2] = frameBuffer.font4Int2(284, 64, 0, 1, oyc.inst[2], nyc.inst[2]); // Len
        oyc.mask = drawChDMG(frameBuffer, 3, oyc.mask, nyc.mask, 0);

        WaveFormToDMG(frameBuffer, 168, 58, oldParam.wf, newParam.wf); // wave form
    }

    @Override
    public void initScreen() {
        for (int c = 0; c < 3; c++) {
            newParam.channels[c].note = -1;
            newParam.channels[c].volume = -1;
            newParam.channels[c].tn = -1;
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                frameBuffer.drawKbn(32 + kx, c * 8 + 8, kt, 0);
            }
        }
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
                            parent.resetChannelMask(DmgChip.class, chipId, ch);
                        else
                            parent.setChannelMask(DmgChip.class, chipId, ch);
                    }
                }
                return;
            }

            // keyboard
            if (py < 5 * 8) {
                int ch = (py / 8) - 1;
                if (ch < 0) return;

                if (ev.getButton() == MouseEvent.BUTTON1) {
                    // mask
                    parent.setChannelMask(DmgChip.class, chipId, ch);
                    return;
                }

                // Unmask
                for (ch = 0; ch < 4; ch++) parent.resetChannelMask(DmgChip.class, chipId, ch);
            }
        }
    };

    private static int searchSSGNote(float freq) {
        float m = Float.MAX_VALUE;
        int n = 0;
        for (int i = 0; i < 12 * 9; i++) {
            float a = Math.abs((freq / (1 << (6 - 4))) - Tables.freqTbl[i]); // 6: Normal range 4: Correction
            if (m > a) {
                m = a;
                n = i;
            } else break;
        }
        return n;
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        // 
        // pbScreen
        // 
        this.image = Common.getImage("planeDMG");
        this.pbScreen.setPreferredSize(new Dimension(336, 72));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        // 
        // frmDMG
        // 
        this.setPreferredSize(new Dimension(336, 72));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmDMG");
        this.setTitle("DMG");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static int drawKeyBoardDMG(FrameBuffer screen, int y, int ot, int nt, int tp) {
        if (ot == nt)
            return ot;

        int kx;
        int kt;

        y = (y + 1) * 8;

        if (ot >= 0 && ot < 12 * 8) {
            kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
            kt = Tables.kbl[(ot % 12) * 2 + 1];
            screen.drawKbn(32 + kx, y, kt, tp);
        }

        if (nt >= 0 && nt < 12 * 8) {
            kx = Tables.kbl[(nt % 12) * 2] + nt / 12 * 28;
            kt = Tables.kbl[(nt % 12) * 2 + 1] + 4;
            screen.drawKbn(32 + kx, y, kt, tp);
        }

        screen.drawFont8(312, y, 1, "   ");

        if (nt >= 0) {
            screen.drawFont8(312, y, 1, Tables.kbn[nt % 12]);
            if (nt / 12 < 10) {
                screen.drawFont8(328, y, 1, Tables.kbo[nt / 12]);
            }
        }

        ot = nt;
        return ot;
    }

    private static Boolean drawChDMG(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm)
            return om;

        ChDMG_P(screen, ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static void WaveFormToDMG(FrameBuffer screen, int x, int y, byte[] oi, byte[] ni) {
        for (int i = 0; i < 32; i++) {
            if (oi[i] == ni[i])
                continue;

            int n = ni[i];

            int m;
            m = (n > 7) ? 8 : n;
            screen.drawByteArray(x + i, y, FrameBuffer.rWavGraph, 64, m, 0, 1, 8);
            m = (n > 15) ? 8 : (Math.max((n - 8), 0));
            screen.drawByteArray(x + i, y - 8, FrameBuffer.rWavGraph, 64, m, 0, 1, 8);
            //m = (n > 23) ? 8 : ((n - 16) < 0 ? 0 : (n - 16));
            //screen.drawByteArray(x + i, y - 16, FrameBuffer.rWavGraph, 64, m, 0, 1, 8);
            //m = (n > 31) ? 8 : ((n - 24) < 0 ? 0 : (n - 24));
            //screen.drawByteArray(x + i, y - 23, FrameBuffer.rWavGraph, 64, m + 1, 0, 1, 7);

            oi[i] = ni[i];
        }
    }

    private static void ChDMG_P(FrameBuffer screen, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        switch (ch) {
            case 0:
                screen.drawByteArray(0, 8, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 48, 8, 16, 8);
                screen.drawFont8(16, 8, mask ? 1 : 0, "1");
                break;
            case 1:
                screen.drawByteArray(0, 16, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 48, 8, 16, 8);
                screen.drawFont8(16, 16, mask ? 1 : 0, "2");
                break;
            case 2:
                screen.drawByteArray(0, 24, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 112, 0, 16, 8);
                break;
            case 3:
                screen.drawByteArray(0, 32, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 96, 8, 24, 8);
                break;
        }
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        int tn = 0;
        int srcFreq = -1;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final byte[] wf = new byte[32];
        final Channel[] channels = {new Channel(), new Channel(), new Channel(), new Channel()};
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "DMG"; }
        @Override public String category() { return "nes"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return DmgChip.class; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormDMG(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 4) {
                DmgChip c = audio.plugin.chipRegister.chip(DmgChip.class);
                if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(DmgChip.class).resetMask(chipId, ch);
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (mask)
                audio.plugin.chipRegister.chip(DmgChip.class).setMask(chipId, ch);
            else
                audio.plugin.chipRegister.chip(DmgChip.class).resetMask(chipId, ch);
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 4; ch++)
                forceChannelMask(audio, DmgChip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(DmgChip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(56, MDSound.Chip.MAIN_TAG, DmgChip.class, "DMG", 50));
        }
    }
}
