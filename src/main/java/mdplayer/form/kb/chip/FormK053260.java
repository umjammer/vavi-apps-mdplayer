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

import mdplayer.Common;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.Tables;
import mdplayer.chips.K053260Chip;
import mdplayer.form.kb.PcmChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;
import mdsound.instrument.K053260Inst;

import static mdplayer.form.kb.chip.FormC352.drawChC352_P;
import mdplayer.form.View;


public class FormK053260 extends FormChipBase<FormK053260.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormK053260.class);

    public FormK053260(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        bind(Common.getImage("planeK053260"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("K053260", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeK053260").getWidth() * zoom, frameSizeH + Common.getImage("planeK053260").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeK053260").getWidth() * zoom, frameSizeH + Common.getImage("planeK053260").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeK053260").getWidth() * zoom, frameSizeH + Common.getImage("planeK053260").getHeight() * zoom));
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
            if (py < 1 * 8) {
                if (px < 8) {
                    for (ch = 0; ch < 4; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(K053260Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(K053260Chip.class, chipId, ch);
                    }
                }
                return;
            }

            ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ch < 4) {
                if (ev.getButton() == MouseEvent.BUTTON1) {
                    parent.setChannelMask(K053260Chip.class, chipId, ch);
                    return;
                }

                for (ch = 0; ch < 4; ch++) parent.resetChannelMask(K053260Chip.class, chipId, ch);
            }
        }
    };

    @Override
    public void initScreen() {
        for (int ch = 0; ch < 4; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                frameBuffer.drawKbn(33 + kx, ch * 8 + 8, kt, 0);
            }
            oldParam.channels[ch].panL = drawPanType5(frameBuffer, 4 * 6 + 1, ch * 8 + 8, oldParam.channels[ch].panL, 0, 0);
            oldParam.channels[ch].panR = drawPanType5(frameBuffer, 4 * 7 + 1, ch * 8 + 8, oldParam.channels[ch].panR, 0, 0);
        }
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(K053260Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int clock = clock(K053260Inst.class);
        for (int ch = 0; ch < 4; ch++) {
            newParam.channels[ch].freq = (int) info.get("channels." + ch + ".freq");
            newParam.channels[ch].eadr = (int) info.get("channels." + ch + ".size");
            newParam.channels[ch].sadr = (int) info.get("channels." + ch + ".start");
            newParam.channels[ch].bank = (int) info.get("channels." + ch + ".bank");
            newParam.channels[ch].pan = (int) info.get("channels." + ch + ".pan");
            newParam.channels[ch].panL = 8 - newParam.channels[ch].pan;
            newParam.channels[ch].panR = newParam.channels[ch].pan;
            int volume = (int) info.get("channels." + ch + ".volume");
            newParam.channels[ch].volume = volume / 2;
            newParam.channels[ch].bit[0] = (int) info.get("channels." + ch + ".play") != 0;
            newParam.channels[ch].bit[1] = (int) info.get("channels." + ch + ".dir") != 0;
            newParam.channels[ch].bit[2] = (int) info.get("channels." + ch + ".loop") != 0;
            newParam.channels[ch].bit[3] = (int) info.get("channels." + ch + ".ppcm") != 0;

            if (newParam.channels[ch].bit[0]) {
                newParam.channels[ch].volumeL = Math.min(volume * newParam.channels[ch].panL / 8 / 5 / 2, 19);
                newParam.channels[ch].volumeR = Math.min(volume * newParam.channels[ch].panR / 8 / 5 / 2, 19);
            } else {
                if (newParam.channels[ch].volumeL > 0) newParam.channels[ch].volumeL--;
                if (newParam.channels[ch].volumeR > 0) newParam.channels[ch].volumeR--;
            }
            newParam.channels[ch].panL /= 2;
            newParam.channels[ch].panR /= 2;

            int delta = (int) info.get("channels." + ch + ".delta");
            newParam.channels[ch].note = !newParam.channels[ch].bit[0] ? -1 : searchNote(delta, clock);
        }
    }

    private static int searchNote(int freq, int clock) {
        int n = 0;
        for (int i = 0; i < 12 * 8; i++) {
            int a = (int) (0x10000
                    * 8000.0
                    * Tables.pcmMulTbl[i % 12 + 12]
                    * Math.pow(2, (i / 12 - 3 + 2))
                    / clock
                    * 6
                    * 2);

            if (freq > a) {
                n = i;
            }
        }
        return Math.clamp(n - 2, 0, 95);
    }

    @Override
    public void drawScreenParams() {
        PcmChannelParams oyc;
        PcmChannelParams nyc;

        for (int ch = 0; ch < 4; ch++) {
            oyc = oldParam.channels[ch];
            nyc = newParam.channels[ch];

            oyc.freq = frameBuffer.font4Hex16Bit(4 * 69 + 1, ch * 8 + 8, 0, oyc.freq, nyc.freq);
            oyc.bank = frameBuffer.font4HexByte(4 * 74 + 1, ch * 8 + 8, 0, oyc.bank, nyc.bank);
            oyc.sadr = frameBuffer.font4Hex16Bit(4 * 77 + 1, ch * 8 + 8, 0, oyc.sadr, nyc.sadr);
            oyc.eadr = frameBuffer.font4Hex16Bit(4 * 82 + 1, ch * 8 + 8, 0, oyc.eadr, nyc.eadr);
            oyc.pan = frameBuffer.font4HexByte(4 * 87 + 1, ch * 8 + 8, 0, oyc.pan, nyc.pan);
            oyc.volume = frameBuffer.font4HexByte(4 * 90 + 1, ch * 8 + 8, 0, oyc.volume, nyc.volume);

            for (int b = 0; b < 4; b++) {
                oldParam.channels[ch].bit[b] = frameBuffer.drawNESSw(64 * 4 + b * 4 + 1, ch * 8 + 8, oldParam.channels[ch].bit[b], newParam.channels[ch].bit[b]);
            }

            oyc.panL = drawPanType5(frameBuffer, 4 * 6 + 1, ch * 8 + 8, oyc.panL, nyc.panL, 0);
            oyc.panR = drawPanType5(frameBuffer, 4 * 7 + 1, ch * 8 + 8, oyc.panR, nyc.panR, 0);
            oyc.volumeL = drawVolumeXY1(frameBuffer, 4 * 92 + 1, ch * 8 + 8, 1, oyc.volumeL, nyc.volumeL, 0);
            oyc.volumeR = drawVolumeXY1(frameBuffer, 4 * 92 + 1, ch * 8 + 12, 1, oyc.volumeR, nyc.volumeR, 0);

            oyc.note = drawKeyBoardXYFX(frameBuffer, 4 * 8 + 1, 4 * 103 + 1, ch * 8 + 8, oyc.note, nyc.note, 0);
            oyc.mask = drawChK053260(frameBuffer, ch, oyc.mask, nyc.mask, 0);
        }
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        this.image = Common.getImage("planeK053260");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);

        this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmK053260");
        this.setTitle("K053260");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static int drawVolumeXY1(FrameBuffer screen, int x, int y, int c, int ov, int nv, int tp) {
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

        for (int i = 0; i <= 19; i++) {
            screen.drawVolumeP(x + i * 2, y + sy, (1 + t), tp);
        }

        for (int i = 0; i <= nv; i++) {
            screen.drawVolumeP(x + i * 2, y + sy, i > 17 ? (2 + t) : (0 + t), tp);
        }

        ov = nv;
        return ov;
    }

    private static int drawKeyBoardXYFX(FrameBuffer screen, int x, int fx, int y, int ot, int nt, int tp) {
        if (ot == nt)
            return ot;

        int kx;
        int kt;

        if (ot >= 0 && ot < 12 * 8) {
            kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
            kt = Tables.kbl[(ot % 12) * 2 + 1];
            screen.drawKbn(x + kx, y, kt, tp);
        }

        if (nt >= 0 && nt < 12 * 8) {
            kx = Tables.kbl[(nt % 12) * 2] + nt / 12 * 28;
            kt = Tables.kbl[(nt % 12) * 2 + 1] + 4;
            screen.drawKbn(x + kx, y, kt, tp);
        }

        screen.drawFont8(fx, y, 1, "   ");

        if (nt >= 0) {
            screen.drawFont8(fx, y, 1, Tables.kbn[nt % 12]);
            if (nt / 12 < 10) {
                screen.drawFont8(16 + fx, y, 1, Tables.kbo[nt / 12]);
            }
        }

        ot = nt;
        return ot;
    }

    private static int drawPanType5(FrameBuffer screen, int x, int y, int ot, int nt, int tp) {
        if (ot == nt)
            return ot;

        drawPanType5P(screen, x, y, nt, tp);
        ot = nt;
        return ot;
    }

    private static void drawPanType5P(FrameBuffer screen, int x, int y, int t, int tp) {
        if (screen == null) {
            return;
        }

        int p = t;
        screen.drawByteArray(x, y, FrameBuffer.rPan2[tp], 32, p * 4, 0, 4, 8);
    }

    private static Boolean drawChK053260(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChC352_P(screen, 1, 8 + ch * 8, ch, nm != null && nm, tp);
        om = nm;
        return om;
    }

//#endregion

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final PcmChannelParams[] channels = {
                new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams()
        };
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "K053260"; }
        @Override public String category() { return "pcm"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return K053260Chip.class; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormK053260(frm, chipId, zoom); }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(44, MDSound.Chip.MAIN_TAG, K053260Chip.class, "k053260", 200));
        }
    }
}
