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
import mdplayer.chips.Ga20Chip;
import mdplayer.form.kb.PcmChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;
import mdsound.instrument.Ga20Inst;

import static mdplayer.form.kb.chip.FormC140.drawChC140_P;
import static mdplayer.form.kb.chip.FormC352.drawChC352;
import mdplayer.form.View;


public class FormGA20 extends FormChipBase<FormGA20.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormGA20.class);

    public FormGA20(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        bind(Common.getImage("planeGA20"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("GA20", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeGA20").getWidth() * zoom, frameSizeH + Common.getImage("planeGA20").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeGA20").getWidth() * zoom, frameSizeH + Common.getImage("planeGA20").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeGA20").getWidth() * zoom, frameSizeH + Common.getImage("planeGA20").getHeight() * zoom));
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
                    for (ch = 0; ch < 4; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(Ga20Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(Ga20Chip.class, chipId, ch);
                    }
                }
                return;
            }

            ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ch < 4) {
                if (ev.getButton() == MouseEvent.BUTTON1) {
                    parent.setChannelMask(Ga20Chip.class, chipId, ch);
                    return;
                }

                for (ch = 0; ch < 4; ch++) parent.resetChannelMask(Ga20Chip.class, chipId, ch);
            }
        }
    };

    @Override
    public void initScreen() {
        int tp = 0;
        for (int ch = 0; ch < 32; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                frameBuffer.drawKbn(32 + kx, ch * 8 + 8, kt, tp);
            }
            frameBuffer.drawPanType2P(24, ch * 8 + 8, 0, tp);
            drawChC140_P(frameBuffer, 0, 8 + ch * 8, ch, false, tp);
        }
    }

    private static int searchGA20Note(int freq, int clock) {
        int hz = clock / (256 - freq);

        int n = 0;
        for (int i = 0; i < 12 * 8; i++) {
            int a = (int) (4000.0
                    * Tables.pcmMulTbl[i % 12 + 12]
                    * Math.pow(2, (i / 12 - 3 + 2)));

            if (hz > a) {
                n = i;
            }
        }
        return n;
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(Ga20Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int clock = clock(Ga20Inst.class) / 4;
        for (int ch = 0; ch < 4; ch++) {
            newParam.channels[ch].freq = (int) info.get("channels." + ch + ".freq");
            newParam.channels[ch].sadr = (int) info.get("channels." + ch + ".sadr");
            newParam.channels[ch].eadr = (int) info.get("channels." + ch + ".eadr");
            newParam.channels[ch].ladr = (int) info.get("channels." + ch + ".ladr");
            newParam.channels[ch].volume = (int) info.get("channels." + ch + ".volume");
            newParam.channels[ch].note = searchGA20Note(newParam.channels[ch].freq, clock);

            boolean play = (boolean) info.get("channels." + ch + ".play");
            if (play) {
                newParam.channels[ch].volumeL = Common.range((256 - newParam.channels[ch].volume) / 13, 0, 19);
            } else {
                if (newParam.channels[ch].volumeL > 0) newParam.channels[ch].volumeL--;
                else newParam.channels[ch].note = -1;
            }
        }
    }

    @Override
    public void drawScreenParams() {
        PcmChannelParams oyc;
        PcmChannelParams nyc;

        for (int ch = 0; ch < 4; ch++) {
            oyc = oldParam.channels[ch];
            nyc = newParam.channels[ch];

            oyc.sadr = frameBuffer.font4Hex20Bit(4 * 65, ch * 8 + 8, 0, oyc.sadr, nyc.sadr);
            oyc.eadr = frameBuffer.font4Hex20Bit(4 * 71, ch * 8 + 8, 0, oyc.eadr, nyc.eadr);
            oyc.ladr = frameBuffer.font4Hex20Bit(4 * 77, ch * 8 + 8, 0, oyc.ladr, nyc.ladr);
            oyc.freq = frameBuffer.font4HexByte(4 * 83, ch * 8 + 8, 0, oyc.freq, nyc.freq);
            oyc.volume = frameBuffer.font4HexByte(4 * 86, ch * 8 + 8, 0, oyc.volume, nyc.volume);
            oyc.note = drawKeyBoardToGA20(frameBuffer, ch, oyc.note, nyc.note, 0);
            oyc.volumeL = frameBuffer.drawVolume(4 * 88, ch * 8 + 8, 0, oyc.volumeL, nyc.volumeL, 0);
            oyc.mask = drawChC352(frameBuffer, ch, oyc.mask, nyc.mask, 0);
        }
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();
        this.image = Common.getImage("planeGA20");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);

        this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmGA20");
        this.setTitle("GA20");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static int drawKeyBoardToGA20(FrameBuffer screen, int y, int ot, int nt, int tp) {
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

        screen.drawFont8(296 + 4 * 24, y, 1, "   ");

        if (nt >= 0) {
            screen.drawFont8(296 + 4 * 24, y, 1, Tables.kbn[nt % 12]);
            if (nt / 12 < 10) {
                screen.drawFont8(312 + 4 * 24, y, 1, Tables.kbo[nt / 12]);
            }
        }

        ot = nt;
        return ot;
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

        @Override public String id() { return "GA20"; }
        @Override public String category() { return "pcm"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return Ga20Chip.class; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormGA20(frm, chipId, zoom); }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(47, MDSound.Chip.MAIN_TAG, Ga20Chip.class, "ga20", 200));
        }
    }
}
