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
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.Tables;
import mdplayer.chips.K051649Chip;
import mdplayer.form.inst.MgscInstWriter;
import mdplayer.form.inst.MgscSccPlainInstWriter;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;
import mdsound.instrument.K051649Inst;

import static mdplayer.Common.searchSSGNote;
import mdplayer.form.View;


public class FormK051649 extends FormChipBase<FormK051649.Params> {

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeK051649");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 144));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmK051649
        //
        this.setPreferredSize(new Dimension(320, 144));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmK051649");
        this.setTitle("K051649Inst");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

    private static final Preferences prefs = Preferences.userNodeForPackage(FormK051649.class);

    public FormK051649(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeK051649"), null, zoom);
        drawScreenInitK051649(frameBuffer);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("K051649", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeK051649").getWidth() * zoom, frameSizeH + Common.getImage("planeK051649").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeK051649").getWidth() * zoom, frameSizeH + Common.getImage("planeK051649").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeK051649").getWidth() * zoom, frameSizeH + Common.getImage("planeK051649").getHeight() * zoom));
        componentListener.componentResized(null);
    }

    private final ComponentListener componentListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
        }
    };

    @Override
    public void changeScreenParams() {
        Map<String, Object> chip = audio.plugin.chipRegister.chip(K051649Chip.class).getInfo(chipId);
        if (chip.isEmpty()) return;

        for (int ch = 0; ch < 5; ch++) {
            if (chip.get("channels." + ch + ".freq") == null) continue;

            Channel channel = newParam.channels[ch];
            for (int i = 0; i < 32; i++) channel.inst[i] = (int) chip.get("channels." + ch + ".inst." + i);
            float fTone = clock(K051649Inst.class) / (8.0f * (float) chip.get("channels." + ch + ".frequency"));
            channel.freq = (int) chip.get("channels." + ch + ".frequency");
            channel.volume = (int) chip.get("channels." + ch + ".volume");
            channel.volumeL = (int) chip.get("channels." + ch + ".volumeL");
            channel.note = ((int) chip.get("channels." + ch + ".key") != 0 && channel.volume != 0) ? searchSSGNote(fTone) : -1;
            channel.dda = (boolean) chip.get("channels." + ch + ".dda");
        }
    
        // the chip itself is the source of truth for channel muting
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(K051649Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        int tp = parent.setting.getK051649Type()[0].getUseReal()[0] ? 1 : 0;

        for (int c = 0; c < 5; c++) {

            Channel oyc = oldParam.channels[c];
            Channel nyc = newParam.channels[c];
            int x = c % 3;
            int y = c / 3;

            oyc.note = frameBuffer.drawKeyBoard(c, oyc.note, nyc.note, tp);
            oyc.volume = frameBuffer.drawVolumeM(256, 8 + c * 8, 0, oyc.volume, nyc.volume, tp);
            oyc.freq = frameBuffer.font4Hex12Bit(x * 4 * 26 + 4 * 14, y * 8 * 6 + 8 * 11, tp, oyc.freq, nyc.freq);
            oyc.volumeL = frameBuffer.font4Hex4Bit(x * 4 * 26 + 4 * 22, y * 8 * 6 + 8 * 11, tp, oyc.volumeL, nyc.volumeL);
            oyc.dda = frameBuffer.drawNESSw(x * 4 * 26 + 4 * 25, y * 8 * 6 + 8 * 11, oyc.dda, nyc.dda);
            drawWaveFormToK051649(frameBuffer, c, oyc.typ, nyc.inst);

            oyc.mask = drawChK051649(frameBuffer, c, oyc.mask, nyc.mask, tp);

            for (int i = 0; i < 32; i++) {
                int fx = i % 8;
                int fy = i / 8;
                oyc.inst[i] = frameBuffer.font4HexByte(x * 4 * 26 + 4 * 10 + fx * 8, y * 8 * 6 + 8 * 7 + fy * 8, 0, oyc.inst[i], nyc.inst[i]);
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
                    for (int ch = 0; ch < 5; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(K051649Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(K051649Chip.class, chipId, ch);
                    }
                }
                return;
            }

            // keyboard
            if (py < 6 * 8) {
                int ch = (py / 8) - 1;
                if (ch < 0) return;

                if (ev.getButton() == MouseEvent.BUTTON1) {
                    // Mask.
                    parent.setChannelMask(K051649Chip.class, chipId, ch);
                    return;
                }

                // Unmask.
                for (ch = 0; ch < 5; ch++) parent.resetChannelMask(K051649Chip.class, chipId, ch);
                return;
            }

            // Right-clicking on a tone does nothing
            if (ev.getButton() == MouseEvent.BUTTON2) return;

            // Judgment of tone display column
            int instCh = ((py < 12 * 8) ? 0 : 3) + px / (8 * 13);

            if (instCh < 5) {
                // Copying a tone to the clipboard
                parent.getInstCh(K051649Chip.class, instCh, chipId);
            }
        }
    };

    @Override
    public void initScreen() {
        for (int c = 0; c < newParam.channels.length; c++) {
            newParam.channels[c].note = -1;
            newParam.channels[c].volume = -1;
            newParam.channels[c].volumeL = -1;
        }
    }

//#region draw buffer

    private static void drawScreenInitK051649(FrameBuffer screen) {
        for (int ch = 0; ch < 5; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                screen.drawKbn(32 + kx, ch * 8 + 8, kt, 0);
            }
            screen.drawFont8(296, ch * 8 + 8, 1, "   ");
        }
    }

    private static Boolean drawChK051649(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChK051649_P(screen, 0, 8 + ch * 8, ch, nm != null && nm, tp);
        om = nm;
        return om;
    }

    private static void drawWaveFormToK051649(FrameBuffer screen, int c, int[] oi, int[] ni) {
        for (int i = 0; i < 32; i++) {
            if (oi[i] == ni[i])
                continue;

            int n = (ni[i] / 8) + 16;
            int x = c % 3;
            x = x * 104 + i + 4;
            int y = c / 3;
            y = y * 48 + 80;

            int m;
            m = (n > 7) ? 8 : n;
            screen.drawByteArray(x, y, FrameBuffer.rWavGraph, 64, m, 0, 1, 8);
            m = (n > 15) ? 8 : (Math.max((n - 8), 0));
            screen.drawByteArray(x, y - 8, FrameBuffer.rWavGraph, 64, m, 0, 1, 8);
            m = (n > 23) ? 8 : (Math.max((n - 16), 0));
            screen.drawByteArray(x, y - 16, FrameBuffer.rWavGraph, 64, m, 0, 1, 8);
            m = (n > 31) ? 8 : (Math.max((n - 24), 0));
            screen.drawByteArray(x, y - 23, FrameBuffer.rWavGraph, 64, m + 1, 0, 1, 7);

            oi[i] = ni[i];
        }
    }

    private static void drawChK051649_P(FrameBuffer screen, int x, int y, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(x, y, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 112, 0, 16, 8);
        screen.drawFont8(x + 16, y, mask ? 1 : 0, String.valueOf(1 + ch));
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        boolean dda = false;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final Channel[] channels = {
                new Channel(), new Channel(), new Channel(), new Channel(), new Channel()
        };
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "K051649"; }
        @Override public String category() { return "wf"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return K051649Chip.class; }
        @Override public String title(int chipId) { return "K051649Inst (%s)".formatted(chipId == 0 ? "Primary" : "Secondary"); }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormK051649(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 5) {
                K051649Chip c = audio.plugin.chipRegister.chip(K051649Chip.class);
                if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(K051649Chip.class).resetMask(chipId, ch);
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(43, MDSound.Chip.MAIN_TAG, K051649Chip.class, "k051649", 200));
        }

        @Override public void getInstCh(Component parent, Audio audio, Setting setting, int ch, int chipId) {
            if (setting.getOther().getInstFormat() == EnmInstFormat.MGSCSCC_PLAIN) {
                new MgscSccPlainInstWriter().write(parent, audio, chip(), ch, chipId);
            } else {
                new MgscInstWriter().write(parent, audio, chip(), ch, chipId);
            }
        }
    }
}
