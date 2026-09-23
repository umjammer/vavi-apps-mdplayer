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
import mdplayer.chips.C352Chip;
import mdplayer.form.kb.PcmChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;
import mdsound.instrument.C352Inst;

import static mdplayer.form.FrameBuffer.rType;
import static mdplayer.form.kb.chip.FormC140.drawChC140_P;
import mdplayer.form.View;


public class FormC352 extends FormChipBase<FormC352.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormC352.class);

    public FormC352(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        bind(Common.getImage("planeC352"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("C352", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeC352").getWidth() * zoom, frameSizeH + Common.getImage("planeC352").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeC352").getWidth() * zoom, frameSizeH + Common.getImage("planeC352").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeC352").getWidth() * zoom, frameSizeH + Common.getImage("planeC352").getHeight() * zoom));
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
                    for (ch = 0; ch < 32; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(C352Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(C352Chip.class, chipId, ch);
                    }
                }
                return;
            }

            ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ch < 32) {
                if (ev.getButton() == MouseEvent.BUTTON1) {
                    parent.setChannelMask(C352Chip.class, chipId, ch);
                    return;
                }

                for (ch = 0; ch < 32; ch++) parent.resetChannelMask(C352Chip.class, chipId, ch);
            }
        }
    };

    @Override
    public void initScreen() {
        boolean C352Type = false; // (chipId == 0) ? parent.setting.C352Type.UseScci : parent.setting.C352SType.UseScci;
        int tp = C352Type ? 1 : 0;
        for (int ch = 0; ch < 32; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                frameBuffer.drawKbn(32 + kx, ch * 8 + 8, kt, tp);
            }
            //frameBuffer.drawFont8(296, ch * 8 + 8, 1, "   ");
            frameBuffer.drawPanType2P(24, ch * 8 + 8, 0, tp);
            drawChC140_P(frameBuffer, 0, 8 + ch * 8, ch, false, tp);
            //frameBuffer.drawVolume(ch, 1,d, 0, tp);
            //frameBuffer.drawVolume(ch, 2,d, 0, tp);
        }
    }

    private int searchC352Note(int freq) {
        double m = Double.MAX_VALUE;

        int clock = clock(C352Inst.class);

        int n = 0;
        for (int i = 0; i < 12 * 8; i++) {
            int a = 0x10000 // Number of counts required to advance 1 sample
                    //8000.0
                    //Tables.pcmMulTbl[i % 12 + 12]
                    //Math.pow(2, (i / 12 - 3 + 2))
                    /
                    clock;

            if (freq > a) {
                m = a;
                n = i;
            }
        }
        return n;
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(C352Chip.class).getInfo(chipId);
        if (info.isEmpty()) return; // the song being played does not use this chip

        int[] c352Register = (int[]) info.get("register");
        int[] c352key = (int[]) info.get("flags");
        // the registers are read back into a buffer the chip reuses, and the visualizer reads the
        // same one, so nothing here may write to it
        boolean[] keyOff = new boolean[32];

        for (int ch = 0; ch < 32; ch++) {
            newParam.channels[ch].note = searchC352Note(c352Register[ch * 8 + 2]);

            if (c352key != null) {
                newParam.channels[ch].pan = ((c352Register[ch * 8 + 0] >> 12) & 0xf) | (((((short) c352Register[ch * 8 + 0] & 0xff) >> 4) & 0xf) << 4);
                if ((c352Register[ch * 8 + 3] & 0x4000) != 0 && (c352key[ch] & 0x8000) != 0) {
                    newParam.channels[ch].volumeL = Common.range((int) (((short) c352Register[ch * 8 + 0] >> 8) / 11.7), 0, 19);
                    newParam.channels[ch].volumeR = Common.range((int) (((short) c352Register[ch * 8 + 0] & 0xff) / 11.7), 0, 19);
                    newParam.channels[ch].volumeRL = Common.range((int) (((short) c352Register[ch * 8 + 1] >> 8) / 11.7), 0, 19);
                    newParam.channels[ch].volumeRR = Common.range((int) (((short) c352Register[ch * 8 + 1] & 0xff) / 11.7), 0, 19);
                }

                if (newParam.channels[ch].mask == null || newParam.channels[ch].mask) {
                    newParam.channels[ch].pan = 0;
                }

                if ((c352key[ch] & 0x8000) == 0) {
                    newParam.channels[ch].note = -1;

                    keyOff[ch] = true;
                    if (newParam.channels[ch].volumeL > 0) newParam.channels[ch].volumeL--;
                    if (newParam.channels[ch].volumeR > 0) newParam.channels[ch].volumeR--;
                    if (newParam.channels[ch].volumeRL > 0) newParam.channels[ch].volumeRL--;
                    if (newParam.channels[ch].volumeRR > 0) newParam.channels[ch].volumeRR--;
                }
            }

            // a channel the chip has finished with shows no key on, whatever the register says
            int d = keyOff[ch] ? c352Register[ch * 8 + 3] & 0xbfff : c352Register[ch * 8 + 3];
            newParam.channels[ch].bit[0] = (d & 0x8000) != 0;
            newParam.channels[ch].bit[1] = (d & 0x4000) != 0;
            newParam.channels[ch].bit[2] = (d & 0x2000) != 0;
            newParam.channels[ch].bit[3] = (d & 0x1000) != 0;
            newParam.channels[ch].bit[4] = (d & 0x0800) != 0;
            newParam.channels[ch].bit[5] = (d & 0x0400) != 0;
            newParam.channels[ch].bit[6] = (d & 0x0200) != 0;
            newParam.channels[ch].bit[7] = (d & 0x0100) != 0;
            newParam.channels[ch].bit[8] = (d & 0x0080) != 0;
            newParam.channels[ch].bit[9] = (d & 0x0040) != 0;
            newParam.channels[ch].bit[10] = (d & 0x0020) != 0;
            newParam.channels[ch].bit[11] = (d & 0x0010) != 0;
            newParam.channels[ch].bit[12] = (d & 0x0008) != 0;
            newParam.channels[ch].bit[13] = (d & 0x0004) != 0;
            newParam.channels[ch].bit[14] = (d & 0x0002) != 0;
            newParam.channels[ch].bit[15] = (d & 0x0001) != 0;

            newParam.channels[ch].freq = c352Register[ch * 8 + 2];
            newParam.channels[ch].bank = c352Register[ch * 8 + 4];
            newParam.channels[ch].sadr = c352Register[ch * 8 + 5];
            newParam.channels[ch].eadr = c352Register[ch * 8 + 6];
            newParam.channels[ch].ladr = c352Register[ch * 8 + 7];
        }
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(C352Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        Channel oyc;
        Channel nyc;

        for (int ch = 0; ch < 32; ch++) {
            oyc = oldParam.channels[ch];
            nyc = newParam.channels[ch];

            oyc.volumeL = frameBuffer.drawVolumeXY(105, ch * 2 + 2, 1, oyc.volumeL, nyc.volumeL, 0); // Front
            oyc.volumeR = frameBuffer.drawVolumeXY(105, ch * 2 + 3, 1, oyc.volumeR, nyc.volumeR, 0); // Front
            oyc.volumeRL = frameBuffer.drawVolumeXY(115, ch * 2 + 2, 1, oyc.volumeRL, nyc.volumeRL, 0); // Rear
            oyc.volumeRR = frameBuffer.drawVolumeXY(115, ch * 2 + 3, 1, oyc.volumeRR, nyc.volumeRR, 0); // Rear
            for (int b = 0; b < 16; b++) {
                frameBuffer.drawNESSw(64 * 4 + b * 4, ch * 8 + 8,
                        oldParam.channels[ch].bit[b], newParam.channels[ch].bit[b]);
            }
            oyc.freq = frameBuffer.font4Hex16Bit(4 * 81, ch * 8 + 8, 0, oyc.freq, nyc.freq);
            oyc.bank = frameBuffer.font4Hex16Bit(4 * 86, ch * 8 + 8, 0, oyc.bank, nyc.bank);
            oyc.sadr = frameBuffer.font4Hex16Bit(4 * 91, ch * 8 + 8, 0, oyc.sadr, nyc.sadr);
            oyc.eadr = frameBuffer.font4Hex16Bit(4 * 96, ch * 8 + 8, 0, oyc.eadr, nyc.eadr);
            oyc.ladr = frameBuffer.font4Hex16Bit(4 * 101, ch * 8 + 8, 0, oyc.ladr, nyc.ladr);
            oyc.note = drawKeyBoardToC352(frameBuffer, ch, oyc.note, nyc.note, 0);
            oyc.mask = drawChC352(frameBuffer, ch, oyc.mask, nyc.mask, 0);
            oyc.pan = frameBuffer.PanType2(ch, oyc.pan, nyc.pan, 0);
        }
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeC352");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(524, 264));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmC352
        //
        this.setPreferredSize(new Dimension(524, 264));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmC352");
        this.setTitle("C352Inst");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static int drawKeyBoardToC352(FrameBuffer screen, int y, int ot, int nt, int tp) {
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

        if (nt >= 0) {
            kx = Tables.kbl[(nt % 12) * 2] + nt / 12 * 28;
            kt = Tables.kbl[(nt % 12) * 2 + 1] + 4;
            screen.drawKbn(32 + kx, y, kt, tp);
            screen.drawFont8(500, y, 1, Tables.kbn[nt % 12]);
            if (nt / 12 < 8) {
                screen.drawFont8(516, y, 1, Tables.kbo[nt / 12]);
            }
        } else {
            screen.drawFont8(500, y, 1, "   ");
        }

        ot = nt;
        return ot;
    }

    public static void drawChC352_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(x, y, rType[tp * 2 + (mask ? 1 : 0)], 128, 16, 0, 16, 8);
//        if (ch < 9) drawFont8(screen, x + 16, y, mask ? 1 : 0, (1 + ch).toString());
//        else
        screen.drawFont4(x + 16, y, mask ? 1 : 0, "%2d".formatted(1 + ch));
    }

    public static Boolean drawChC352(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChC352_P(screen, 0, 8 + ch * 8, ch, nm != null && nm, tp);
        om = nm;
        return om;
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends PcmChannelParams {

        int volumeRL = -1;
        int volumeRR = -1;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final Channel[] channels = {
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(),

                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel()
        };
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "C352"; }
        @Override public String category() { return "pcm"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return C352Chip.class; }
        @Override public boolean hasRegisterDump() { return true; }
        @Override public String title(int chipId) { return "C352Inst (%s)".formatted(chipId == 0 ? "Primary" : "Secondary"); }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormC352(frm, chipId, zoom); }

        @Override public void setChannelMask(mdplayer.Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            C352Chip c = audio.plugin.chipRegister.chip(C352Chip.class);
            if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
        }

        @Override public void resetChannelMask(mdplayer.Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 32) {
                audio.plugin.chipRegister.chip(C352Chip.class).resetMask(chipId, ch);
            }
        }

        @Override public void forceChannelMask(mdplayer.Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (mask)
                audio.plugin.chipRegister.chip(C352Chip.class).setMask(chipId, ch);
            else
                audio.plugin.chipRegister.chip(C352Chip.class).resetMask(chipId, ch);
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 32; ch++)
                forceChannelMask(audio, C352Chip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(C352Chip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(40, MDSound.Chip.MAIN_TAG, C352Chip.class, "c352", 200));
        }
    }
}
