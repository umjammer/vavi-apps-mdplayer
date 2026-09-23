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
import mdplayer.chips.YmZ280BChip;
import mdplayer.form.ScreenPanel;
import mdplayer.form.View;
import mdplayer.form.kb.PcmChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;


public class FormYMZ280B extends FormChipBase<FormYMZ280B.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormYMZ280B.class);

    public FormYMZ280B(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        bind(Common.getImage("planeYMZ280B"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("YMZ280B", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeYMZ280B").getWidth() * zoom, frameSizeH + Common.getImage("planeYMZ280B").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeYMZ280B").getWidth() * zoom, frameSizeH + Common.getImage("planeYMZ280B").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeYMZ280B").getWidth() * zoom, frameSizeH + Common.getImage("planeYMZ280B").getHeight() * zoom));
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
                    for (ch = 0; ch < 8; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(YmZ280BChip.class, chipId, ch);
                        else
                            parent.setChannelMask(YmZ280BChip.class, chipId, ch);
                    }
                }
                return;
            }

            ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ch < 8) {
                if (ev.getButton() == MouseEvent.BUTTON1) {
                    parent.setChannelMask(YmZ280BChip.class, chipId, ch);
                    return;
                }

                for (ch = 0; ch < 8; ch++) parent.resetChannelMask(YmZ280BChip.class, chipId, ch);
            }
        }
    };

    @Override
    public void initScreen() {
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(YmZ280BChip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int[] reg = (int[]) info.get("register");

        for (int ch = 0; ch < 8; ch++) {
            newParam.channels[ch].freq = (reg[0x0 + ch * 4] & 0xff) +
                    ((reg[0x1 + ch * 4] & 1) << 8);
            newParam.channels[ch].nfrq = reg[0x2 + ch * 4] & 0xff;
            newParam.channels[ch].pan = reg[0x3 + ch * 4] & 0xf;
            newParam.channels[ch].sadr = ((reg[0x20 + ch * 4] & 0xff) << 16) +
                    ((reg[0x40 + ch * 4] & 0xff) << 8) +
                    (reg[0x60 + ch * 4] & 0xff);
            newParam.channels[ch].ladr = ((reg[0x21 + ch * 4] & 0xff) << 16) +
                    ((reg[0x41 + ch * 4] & 0xff) << 8) +
                    (reg[0x61 + ch * 4] & 0xff);
            newParam.channels[ch].leadr = ((reg[0x22 + ch * 4] & 0xff) << 16) +
                    ((reg[0x42 + ch * 4] & 0xff) << 8) +
                    (reg[0x62 + ch * 4] & 0xff);
            newParam.channels[ch].eadr = ((reg[0x23 + ch * 4] & 0xff) << 16) +
                    ((reg[0x43 + ch * 4] & 0xff) << 8) +
                    (reg[0x63 + ch * 4] & 0xff);

            newParam.channels[ch].dda = (reg[0x1 + ch * 4] & 0x80) != 0;
            newParam.channels[ch].ex = (reg[0x1 + ch * 4] & 0x40) != 0;
            newParam.channels[ch].noise = (reg[0x1 + ch * 4] & 0x20) != 0;
            newParam.channels[ch].loopFlg = (reg[0x1 + ch * 4] & 0x10) != 0;
        }
    }

    @Override
    public void drawScreenParams() {
        for (int ch = 0; ch < 8; ch++) {
            Channel orc = oldParam.channels[ch];
            Channel nrc = newParam.channels[ch];

            orc.pan = frameBuffer.font4Hex4Bit(4 * 7, ch * 8 + 8, 0, orc.pan, nrc.pan);
            orc.dda = frameBuffer.drawNESSw(4 * 8, ch * 8 + 8, orc.dda, nrc.dda);
            orc.ex = frameBuffer.drawNESSw(4 * 9, ch * 8 + 8, orc.ex, nrc.ex);
            orc.noise = frameBuffer.drawNESSw(4 * 10, ch * 8 + 8, orc.noise, nrc.noise);
            orc.loopFlg = frameBuffer.drawNESSw(4 * 11, ch * 8 + 8, orc.loopFlg, nrc.loopFlg);
            orc.sadr = frameBuffer.font4Hex24Bit(4 * 13, ch * 8 + 8, 0, orc.sadr, nrc.sadr);
            orc.ladr = frameBuffer.font4Hex24Bit(4 * 20, ch * 8 + 8, 0, orc.ladr, nrc.ladr);
            orc.leadr = frameBuffer.font4Hex24Bit(4 * 27, ch * 8 + 8, 0, orc.leadr, nrc.leadr);
            orc.eadr = frameBuffer.font4Hex24Bit(4 * 34, ch * 8 + 8, 0, orc.eadr, nrc.eadr);
            orc.freq = frameBuffer.font4Hex12Bit(4 * 41, ch * 8 + 8, 0, orc.freq, nrc.freq); // PITCH
            orc.nfrq = frameBuffer.font4HexByte(4 * 45, ch * 8 + 8, 0, orc.nfrq, nrc.nfrq); // TL
        }
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeYMZ280B");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(240, 72));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmYMZ280B
        //
        this.setPreferredSize(new Dimension(240, 72));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmYMZ280B");
        this.setTitle("YMZ280B");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends PcmChannelParams {

        boolean dda = false;
        boolean ex = false;
        boolean noise = false;
        boolean loopFlg = false;
        int nfrq = -1;
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

        @Override public String id() { return "YMZ280B"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return YmZ280BChip.class; }
        @Override public boolean hasRegisterDump() { return true; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormYMZ280B(frm, chipId, zoom); }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(22, MDSound.Chip.MAIN_TAG, YmZ280BChip.class, "ymz280b", 200));
        }
    }
}
