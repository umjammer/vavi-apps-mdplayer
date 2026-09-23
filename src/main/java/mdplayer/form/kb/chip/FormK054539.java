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
import mdplayer.chips.K054539Chip;
import mdplayer.form.kb.PcmChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;
import mdsound.instrument.K054539Inst;

import static mdplayer.form.kb.chip.FormC352.drawChC352;
import mdplayer.form.View;


public class FormK054539 extends FormChipBase<FormK054539.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormK054539.class);

    private static final int[] pantbl = {
            0 * 5 + 4, 1 * 5 + 4, 1 * 5 + 4, 2 * 5 + 4, 2 * 5 + 4, 3 * 5 + 4, 3 * 5 + 4,
            4 * 5 + 4,
            4 * 5 + 3, 4 * 5 + 3, 4 * 5 + 2, 4 * 5 + 2, 4 * 5 + 1, 4 * 5 + 1, 4 * 5 + 0,
    };

    public FormK054539(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        bind(Common.getImage("planeK054539"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("K054539", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeK054539").getWidth() * zoom, frameSizeH + Common.getImage("planeK054539").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeK054539").getWidth() * zoom, frameSizeH + Common.getImage("planeK054539").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeK054539").getWidth() * zoom, frameSizeH + Common.getImage("planeK054539").getHeight() * zoom));
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
                    for (ch = 0; ch < 8; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(K054539Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(K054539Chip.class, chipId, ch);
                    }
                }
                return;
            }

            ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ch < 8) {
                if (ev.getButton() == MouseEvent.BUTTON1) {
                    parent.setChannelMask(K054539Chip.class, chipId, ch);
                    return;
                }

                for (ch = 0; ch < 8; ch++) parent.resetChannelMask(K054539Chip.class, chipId, ch);
            }
        }
    };

    @Override
    public void initScreen() {
        int tp = 0;
        for (int ch = 0; ch < 8; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                frameBuffer.drawKbn(32 + kx, ch * 8 + 8, kt, tp);
            }
            frameBuffer.drawPanType2P(24, ch * 8 + 8, 0, tp);
        }
    }

    private static int searchK054539Note(int freq, int clock) {
        if (clock >= 1000000) clock /= 384;
        int hz = (int) (clock / (0x10000 / (double) freq));

        int n = 0;
        for (int i = 0; i < 12 * 8; i++) {
            int a = (int) (4000.0
                    * Tables.pcmMulTbl[i % 12 + 12]
                    * Math.pow(2, (i / 12 - 3 + 2)));

            if (hz > a) {
                n = i;
            }
        }
        return n + 1;
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(K054539Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int[] regs = (int[]) info.get("register");
        int clock = clock(K054539Inst.class);

        for (int ch = 0; ch < 8; ch++) {
            int pan = regs[0x20 * ch + 0x05];
            if (pan >= 0x81 && pan <= 0x8f)
                pan -= 0x81;
            else if (pan >= 0x11 && pan <= 0x1f)
                pan -= 0x11;
            else
                pan = 0x18 - 0x11;
            newParam.channels[ch].pan = pantbl[pan];

            newParam.channels[ch].sadr = regs[0x20 * ch + 0x0c]
                    + (regs[0x20 * ch + 0x0d] << 8)
                    + (regs[0x20 * ch + 0x0e] << 16);
            newParam.channels[ch].eadr = regs[0x20 * ch + 0x08]
                    + (regs[0x20 * ch + 0x09] << 8)
                    + (regs[0x20 * ch + 0x0a] << 16);
            newParam.channels[ch].echo = regs[0x20 * ch + 0x00]
                    + (regs[0x20 * ch + 0x01] << 8)
                    + (regs[0x20 * ch + 0x02] << 16);
            newParam.channels[ch].freq = regs[0x20 * ch + 0x06]
                    + (regs[0x20 * ch + 0x07] << 8);
            newParam.channels[ch].kf = regs[0x20 * ch + 0x04];
            newParam.channels[ch].bank = (regs[0x200 + 0x2 * ch] & 0xc) >> 2;
            newParam.channels[ch].loopFlg = (regs[0x200 + 0x2 * ch + 1] & 0x1) != 0;
            newParam.channels[ch].ex = (regs[0x200 + 0x2 * ch] & 0x20) != 0;
            newParam.channels[ch].volume = regs[0x20 * ch + 0x03];
            int vol = 0x40 - Common.range(newParam.channels[ch].volume, 0, 0x40);
            newParam.channels[ch].dda = (regs[0x214] & (1 << ch)) != 0;
            newParam.channels[ch].noise = (regs[0x215] & (1 << ch)) != 0;

            if ((regs[0x22c] & (1 << ch)) != 0) {
                newParam.channels[ch].volumeL = Common.range(vol * 19 * (newParam.channels[ch].pan / 5) / 4 / 0x40, 0, 19);
                newParam.channels[ch].volumeR = Common.range(vol * 19 * (newParam.channels[ch].pan % 5) / 4 / 0x40, 0, 19);
                newParam.channels[ch].note = searchK054539Note(newParam.channels[ch].echo, clock);
            } else {
                if (newParam.channels[ch].volumeL > 0) newParam.channels[ch].volumeL--;
                if (newParam.channels[ch].volumeR > 0) newParam.channels[ch].volumeR--;
                newParam.channels[ch].note = -1;
            }
        }
    }

    @Override
    public void drawScreenParams() {
        Channel oyc;
        Channel nyc;

        for (int ch = 0; ch < 8; ch++) {
            oyc = oldParam.channels[ch];
            nyc = newParam.channels[ch];

            oyc.sadr = frameBuffer.font4Hex24Bit(4 * 9, ch * 8 + 8 * 10, 0, oyc.sadr, nyc.sadr);
            oyc.eadr = frameBuffer.font4Hex24Bit(4 * 17, ch * 8 + 8 * 10, 0, oyc.eadr, nyc.eadr);
            oyc.echo = frameBuffer.font4Hex24Bit(4 * 25, ch * 8 + 8 * 10, 0, oyc.echo, nyc.echo);
            oyc.freq = frameBuffer.font4Hex16Bit(4 * 33, ch * 8 + 8 * 10, 0, oyc.freq, nyc.freq);
            oyc.kf = frameBuffer.font4HexByte(4 * 39, ch * 8 + 8 * 10, 0, oyc.kf, nyc.kf);
            oyc.bank = frameBuffer.font4HexByte(4 * 43, ch * 8 + 8 * 10, 0, oyc.bank, nyc.bank);
            oyc.loopFlg = frameBuffer.drawNESSw(4 * 46, ch * 8 + 8 * 10, oyc.loopFlg, nyc.loopFlg);
            oyc.ex = frameBuffer.drawNESSw(4 * 48, ch * 8 + 8 * 10, oyc.ex, nyc.ex);
            oyc.volume = frameBuffer.font4HexByte(4 * 51, ch * 8 + 8 * 10, 0, oyc.volume, nyc.volume);
            oyc.dda = frameBuffer.drawNESSw(4 * 54, ch * 8 + 8 * 10, oyc.dda, nyc.dda);
            oyc.noise = frameBuffer.drawNESSw(4 * 56, ch * 8 + 8 * 10, oyc.noise, nyc.noise);

            oyc.pan = drawPanType4(frameBuffer, 4 * 6, ch * 8 + 8 * 1, oyc.pan, nyc.pan, 0);
            oyc.note = drawKeyBoard(frameBuffer, ch, oyc.note, nyc.note, 0);
            oyc.volumeL = frameBuffer.drawVolume(4 * 64, ch * 8 + 8, 1, oyc.volumeL, nyc.volumeL, 0);
            oyc.volumeR = frameBuffer.drawVolume(4 * 64, ch * 8 + 12, 1, oyc.volumeR, nyc.volumeR, 0);
            oyc.mask = drawChC352(frameBuffer, ch, oyc.mask, nyc.mask, 0);
        }
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        this.image = Common.getImage("planeK054539");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);

        this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmK054539");
        this.setTitle("K054539");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static int drawKeyBoard(FrameBuffer screen, int y, int ot, int nt, int tp) {
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

        screen.drawFont8(296, y, 1, "   ");

        if (nt >= 0) {
            screen.drawFont8(296, y, 1, Tables.kbn[nt % 12]);
            if (nt / 12 < 10) {
                screen.drawFont8(312, y, 1, Tables.kbo[nt / 12]);
            }
        }

        ot = nt;
        return ot;
    }

    private static int drawPanType4(FrameBuffer screen, int x, int y, int ot, int nt, int tp) {
        if (ot == nt)
            return ot;

        drawPanType4P(screen, x, y, nt, tp);
        ot = nt;
        return ot;
    }

    private static void drawPanType4P(FrameBuffer screen, int x, int y, int t, int tp) {
        if (screen == null) {
            return;
        }

        int p = t / 5;
        screen.drawByteArray(x, y, FrameBuffer.rPan2[tp], 32, p * 4, 0, 4, 8);
        p = t % 5;
        screen.drawByteArray(x + 4, y, FrameBuffer.rPan2[tp], 32, p * 4, 0, 4, 8);
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends PcmChannelParams {

        boolean dda = false;
        int echo = -1;
        boolean ex = false;
        int kf = -1;
        boolean loopFlg = false;
        boolean noise = false;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final Channel[] channels = {
                new Channel(), new Channel(), new Channel(), new Channel(),
                new Channel(), new Channel(), new Channel(), new Channel()
        };
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "K054539"; }
        @Override public String category() { return "pcm"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return K054539Chip.class; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormK054539(frm, chipId, zoom); }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(45, MDSound.Chip.MAIN_TAG, K054539Chip.class, "k054539", 200));
        }
    }
}
