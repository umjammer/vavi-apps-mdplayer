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
import mdplayer.Setting;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.Tables;
import mdplayer.chips.NpNesChip;
import mdplayer.chips.NpNesChip.N163Chip;
import mdplayer.form.VisVolume;
import mdplayer.form.inst.MckInstWriter;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.Meters;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;
import mdsound.np.chip.NesN106;
import mdplayer.form.View;


public class FormN106 extends FormChipBase<FormN106.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormN106.class);

    public FormN106(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        bind(Common.getImage("planeN106"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("N106", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeN106").getWidth() * zoom, frameSizeH + Common.getImage("planeN106").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeN106").getWidth() * zoom, frameSizeH + Common.getImage("planeN106").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeN106").getWidth() * zoom, frameSizeH + Common.getImage("planeN106").getHeight() * zoom));
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
                            parent.resetChannelMask(N163Chip.class, chipId, ch);
                        else
                            parent.setChannelMask(N163Chip.class, chipId, ch);
                    }
                }
                return;
            }

            ch = (py / 8) - 1;
            if (ch < 0) return;
            int m = ch % 3;
            ch /= 3;

            if (ev.getButton() == MouseEvent.BUTTON2) {
                for (int i = 0; i < 8; i++) {
                    // Unmask.
                    parent.resetChannelMask(N163Chip.class, chipId, i);
                }
                return;
            }

            if (m != 0) {
                // Copying a tone to the clipboard
                parent.getInstCh(N163Chip.class, ch, chipId);
            } else {
                // Mask.
                parent.setChannelMask(N163Chip.class, chipId, ch);
            }
        }
    };

    @Override
    public void initScreen() {
        boolean N106Type = false;
        int tp = N106Type ? 1 : 0;
        for (int ch = 0; ch < 8; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                frameBuffer.drawKbn(32 + kx, ch * 24 + 8, kt, tp);
            }
        }
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(NpNesChip.N163Chip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        NesN106.TrackInfo[] trackInfo = (NesN106.TrackInfo[]) info.get("tracksInfo"); // TODO not abstracted

        for (int ch = 0; ch < 8; ch++) {
            Channel nyc = newParam.channels[ch];

            nyc.bit[0] = trackInfo[ch].getKeyStatus();
            nyc.bit[1] = trackInfo[ch].getHalt();

            int v = trackInfo[ch].getVolume() * 2;
            nyc.volume = Math.min(v, 19);
            nyc.volumeR = trackInfo[ch].getVolume();

            nyc.freq = trackInfo[ch].getFreq();
            v = trackInfo[ch].getNote(trackInfo[ch].getFreqHz()) - 4 * 12;
            nyc.note = (nyc.volumeL == 0 || !nyc.bit[0]) ? -1 : v;

            nyc.bank = trackInfo[ch].waveLen & 127;
            nyc.bank = nyc.bank <= 0 ? (trackInfo[ch].waveLen > 127 ? 127 : 0) : nyc.bank;
            if (nyc.aryWave16bit == null) nyc.aryWave16bit = new short[280];
            for (int i = 0; i < 280; i++) {
                if (i < nyc.bank) {
                    nyc.aryWave16bit[i] = trackInfo[ch].wave[i];
                } else {
                    if (i != 279) nyc.aryWave16bit[i] = nyc.aryWave16bit[i + 1];
                    else {
                        int w = ((byte) trackInfo[ch].getOutput() >> 4) + 8;
                        nyc.aryWave16bit[i] = (short) (w + 16);
                    }
                }
            }
        }
    
        for (int mch = 0; mch < newParam.channels.length; mch++)
            newParam.channels[mch].mask = audio.plugin.chipRegister.chip(NpNesChip.N163Chip.class).getMask(chipId, mch);
    }

    @Override
    public void drawScreenParams() {
        Channel oyc;
        Channel nyc;

        for (int ch = 0; ch < 8; ch++) {
            oyc = oldParam.channels[ch];
            nyc = newParam.channels[ch];

            // Enable
            frameBuffer.drawNESSw(6 * 4, ch * 24 + 8,
                    oldParam.channels[ch].bit[1], newParam.channels[ch].bit[1]);

            // Key
            frameBuffer.drawNESSw(7 * 4, ch * 24 + 8,
                    oldParam.channels[ch].bit[0], newParam.channels[ch].bit[0]);

            // vol
            oyc.volume = frameBuffer.drawVolumeM(256, 8 + ch * 3 * 8, 0, oyc.volume, nyc.volume, 0);
            oyc.volumeR = frameBuffer.font4Hex4Bit(4 * 4, ch * 24 + 16, 0, oyc.volumeR, nyc.volumeR);

            // freq
            oyc.freq = frameBuffer.font4Hex20Bit(4 * 4, ch * 24 + 24, 0, oyc.freq, nyc.freq);

            // Note
            oyc.note = frameBuffer.drawKeyBoard(ch * 3, oyc.note, nyc.note, 0);

            if (oyc.aryWave16bit == null && nyc.aryWave16bit != null)
                oyc.aryWave16bit = new short[nyc.aryWave16bit.length];
            drawWaveFormToN106(frameBuffer, 10 * 4, ch * 24 + 16, oyc.aryWave16bit, nyc.aryWave16bit);
            oldParam.channels[ch].mask = drawChN163(frameBuffer, ch, oldParam.channels[ch].mask, newParam.channels[ch].mask, 0);
        }
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeN106");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 198));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmN106
        //
        this.setPreferredSize(new Dimension(320, 198));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmN106");
        this.setTitle("N163(N106)");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static Boolean drawChN163(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChN163_P(screen, ch, nm == null ? false : nm, tp);
        return om;
    }

    private static void drawWaveFormToN106(FrameBuffer screen, int x, int y, short[] oi, short[] ni) {
        if (ni == null)
            return;

        for (int i = 0; i < ni.length; i++) {
            if (oi[i] == ni[i])
                continue;

            screen.drawByteArray(x + i, y, FrameBuffer.rWavGraph2, 33, ni[i] % 33, 0, 1, 16);

            oi[i] = ni[i];
        }
    }

    private static void drawChN163_P(FrameBuffer screen, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(0, ch * 8 * 3 + 8, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 112, 0, 16, 8);
        screen.drawFont8(16, ch * 8 * 3 + 8, mask ? 1 : 0, String.valueOf(ch + 1));
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        int bank = -1;
        short[] aryWave16bit;
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

        @Override public String id() { return "N106"; }
        @Override public String menuText() { return "N163(N106)"; }
        @Override public String category() { return "nes"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return NpNesChip.N163Chip.class; }
        @Override public String title(int chipId) { return "N163(N106) (%s)".formatted(chipId == 0 ? "Primary" : "Secondary"); }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormN106(frm, chipId, zoom); }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (ch >= 0 && ch < 8) {
                NpNesChip.N163Chip c = audio.plugin.chipRegister.chip(NpNesChip.N163Chip.class);
                if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            audio.plugin.chipRegister.chip(NpNesChip.N163Chip.class).resetMask(chipId, ch);
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (mask)
                audio.plugin.chipRegister.chip(NpNesChip.N163Chip.class).setMask(chipId, ch);
            else
                audio.plugin.chipRegister.chip(NpNesChip.N163Chip.class).resetMask(chipId, ch);
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 8; ch++)
                forceChannelMask(audio, NpNesChip.N163Chip.class, chipId, ch,
                        audio.plugin.chipRegister.chip(NpNesChip.N163Chip.class).getMask(chipId, ch));
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(52, MDSound.Chip.MAIN_TAG, NpNesChip.N163Chip.class, "N160", 50));
        }

        @Override public void updateMeters(Audio audio, VisVolume visVolume) {
            int n160 = Meters.npNesVolume(audio, 3);
            if (n160 >= 0) visVolume.put("N160", n160 * 15);
        }

        @Override public void getInstCh(Component parent, Audio audio, Setting setting, int ch, int chipId) {
            new MckInstWriter().write(parent, audio, chip(), ch, chipId);
        }
    }
}
