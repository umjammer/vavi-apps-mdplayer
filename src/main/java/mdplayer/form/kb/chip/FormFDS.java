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
import mdplayer.Chip;
import mdplayer.Common;
import mdplayer.Tables;
import mdplayer.chips.NesChip;
import mdplayer.chips.NesChip.FdsChip;
import mdplayer.chips.NpNesChip;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.form.View;
import mdplayer.form.VisVolume;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.Meters;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;


public class FormFDS extends FormChipBase<FormFDS.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormFDS.class);

    public FormFDS(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeFDS"), null, zoom);
        drawScreenInitFDS(frameBuffer);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("FDS", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeFDS").getWidth() * zoom, frameSizeH + Common.getImage("planeFDS").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeFDS").getWidth() * zoom, frameSizeH + Common.getImage("planeFDS").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeFDS").getWidth() * zoom, frameSizeH + Common.getImage("planeFDS").getHeight() * zoom));
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
        Map<String, Object> info = audio.plugin.chipRegister.chip(NpNesChip.FdsChip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int freq = (int) info.get("freq");
        int vol = (int) info.get("vol");

        int note = -15 + (int) ((12 * (Math.log(freq) / LOG_2 - LOG2_440) + NOTE_440HZ + 0.5));
        note = note < 0 ? -1 : (note > 120 ? -1 : note);
        note = vol == 0 ? -1 : note;
        vol = note == -1 ? 0 : vol;
        newParam.channel.note = note;
        newParam.channel.volume = Math.min((int) ((vol) * 0.5), 19);

        int[][] wave = (int[][]) info.get("wave");
        for (int i = 0; i < 32; i++) {
            newParam.wave[i] = (wave[1][i * 2 + 0] + wave[1][i * 2 + 1]) >> 2;
            newParam.mod[i] = (wave[0][i * 2 + 0] + wave[0][i * 2 + 1]) << 1;
        }

        newParam.VolDir = (boolean) info.get("VolDir");
        newParam.VolSpd = (int) info.get("VolSpd");
        newParam.VolGain = (int) info.get("VolGain");
        newParam.VolDi = (boolean) info.get("VolDi");
        newParam.VolFrq = (int) info.get("VolFrq");
        newParam.VolHlR = (boolean) info.get("VolHlR");

        newParam.ModDir = (boolean) info.get("ModDir");
        newParam.ModSpd = (int) info.get("ModSpd");
        newParam.ModGain = (int) info.get("ModGain");
        newParam.ModDi = (boolean) info.get("ModDi");
        newParam.ModFrq = (int) info.get("ModFrq");
        newParam.ModCnt = (int) info.get("ModCnt");

        newParam.EnvSpd = (int) info.get("EnvSpd");
        newParam.EnvVolSw = (boolean) info.get("EnvVolSw");
        newParam.EnvModSw = (boolean) info.get("EnvModSw");

        newParam.MasterVol = (int) info.get("MasterVol");
        newParam.WE = (boolean) info.get("WE");

        newParam.channel.mask = audio.plugin.chipRegister.chip(NpNesChip.FdsChip.class).getMask(chipId, -1);
    }

    @Override
    public void drawScreenParams() {
        oldParam.channel.note = frameBuffer.drawKeyBoard(0, oldParam.channel.note, newParam.channel.note, 0);
        oldParam.channel.volume = frameBuffer.drawVolumeM(256, 8 + 0 * 8, 0, oldParam.channel.volume, newParam.channel.volume, 0);

        drawWaveFormToFDS(frameBuffer, 0, oldParam.wave, newParam.wave);
        drawWaveFormToFDS(frameBuffer, 1, oldParam.mod, newParam.mod);

        oldParam.VolDir = frameBuffer.drawNESSw(20 * 4, 6 * 4, oldParam.VolDir, newParam.VolDir);
        oldParam.VolSpd = frameBuffer.font4Int2(19 * 4, 8 * 4, 0, 2, oldParam.VolSpd, newParam.VolSpd);
        oldParam.VolGain = frameBuffer.font4Int2(19 * 4, 10 * 4, 0, 2, oldParam.VolGain, newParam.VolGain);
        oldParam.VolDi = frameBuffer.drawNESSw(20 * 4, 12 * 4, oldParam.VolDi, newParam.VolDi);
        oldParam.VolFrq = frameBuffer.font4Hex12Bit(26 * 4, 6 * 4, 0, oldParam.VolFrq, newParam.VolFrq);
        oldParam.VolHlR = frameBuffer.drawNESSw(28 * 4, 8 * 4, oldParam.VolHlR, newParam.VolHlR);

        oldParam.ModDir = frameBuffer.drawNESSw(48 * 4, 6 * 4, oldParam.ModDir, newParam.ModDir);
        oldParam.ModSpd = frameBuffer.font4Int2(47 * 4, 8 * 4, 0, 2, oldParam.ModSpd, newParam.ModSpd);
        oldParam.ModGain = frameBuffer.font4Int2(47 * 4, 10 * 4, 0, 2, oldParam.ModGain, newParam.ModGain);
        oldParam.ModDi = frameBuffer.drawNESSw(48 * 4, 12 * 4, oldParam.ModDi, newParam.ModDi);
        oldParam.ModFrq = frameBuffer.font4Hex12Bit(54 * 4, 6 * 4, 0, oldParam.ModFrq, newParam.ModFrq);
        oldParam.ModCnt = frameBuffer.font4Int3(54 * 4, 8 * 4, 0, 3, oldParam.ModCnt, newParam.ModCnt);

        oldParam.EnvSpd = frameBuffer.font4Int3(65 * 4, 6 * 4, 0, 3, oldParam.EnvSpd, newParam.EnvSpd);
        oldParam.EnvVolSw = frameBuffer.drawNESSw(67 * 4, 8 * 4, oldParam.EnvVolSw, newParam.EnvVolSw);
        oldParam.EnvModSw = frameBuffer.drawNESSw(67 * 4, 10 * 4, oldParam.EnvModSw, newParam.EnvModSw);

        oldParam.MasterVol = frameBuffer.font4Int2(76 * 4, 6 * 4, 0, 2, oldParam.MasterVol, newParam.MasterVol);
        oldParam.WE = frameBuffer.drawNESSw(77 * 4, 8 * 4, oldParam.WE, newParam.WE);

        oldParam.channel.mask = drawChFDS(frameBuffer, 0, oldParam.channel.mask, newParam.channel.mask, 0);
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
                    if (newParam.channel.mask)
                        parent.resetChannelMask(FdsChip.class, chipId, 0);
                    else
                        parent.setChannelMask(FdsChip.class, chipId, 0);
                }
                return;
            }

            // keyboard
            if (py < 2 * 8) {
                if (ev.getButton() == MouseEvent.BUTTON2) {
                    // Unmask.
                    parent.resetChannelMask(FdsChip.class, chipId, 0);
                    return;
                }

                if (ev.getButton() == MouseEvent.BUTTON1) {
                    // Mask.
                    parent.setChannelMask(FdsChip.class, chipId, 0);
                }
            }
        }
    };

    @Override
    public void initScreen() {
        newParam.channel.note = -1;
        newParam.channel.volume = -1;
        for (int i = 0; i < 32; i++) {
            newParam.wave[i] = 0;
            newParam.mod[i] = 0;
        }

        newParam.VolDir = false;
        newParam.VolSpd = 0;
        newParam.VolGain = 0;
        newParam.VolDi = false;
        newParam.VolFrq = 0;
        newParam.VolHlR = false;

        newParam.ModDir = false;
        newParam.ModSpd = 0;
        newParam.ModGain = 0;
        newParam.ModDi = false;
        newParam.ModFrq = 0;
        newParam.ModCnt = 0;

        newParam.EnvSpd = 0;
        newParam.EnvVolSw = false;
        newParam.EnvModSw = false;

        newParam.MasterVol = 0;
        newParam.WE = false;
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeFDS");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 56));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmFDS
        //
        this.setPreferredSize(new Dimension(320, 56));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmFDS");
        this.setTitle("FDS");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static void drawScreenInitFDS(FrameBuffer screen) {
        if (screen == null)
            return;

        for (int ot = 0; ot < 12 * 8; ot++) {
            int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
            int kt = Tables.kbl[(ot % 12) * 2 + 1];
            screen.drawKbn(32 + kx, 8, kt, 0);
        }
        screen.drawFont8(296, 8, 1, "   ");
        boolean m = true;
        drawChFDS(screen, 0, m, false, 0);
    }

    private static Boolean drawChFDS(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChFDS_P(screen, ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static void drawWaveFormToFDS(FrameBuffer screen, int c, int[] oi, int[] ni) {
        for (int i = 0; i < 32; i++) {
            if (oi[i] == ni[i])
                continue;

            int n = ni[i];
            int x = i + c * 4 * 31 + 8;
            int y = 8 * 6;

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

    private static void drawChFDS_P(FrameBuffer screen, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        screen.drawByteArray(0, 8, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 14 * 8, 0 * 8, 16, 8);
    }

//#endregion

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final ChannelParams channel = new ChannelParams();
        final int[] wave = new int[32];
        final int[] mod = new int[32];

        boolean VolDir = false;
        int VolSpd = 0;
        int VolGain = 0;
        boolean VolDi = false;
        int VolFrq = 0;
        boolean VolHlR = false;

        boolean ModDir = false;
        int ModSpd = 0;
        int ModGain = 0;
        boolean ModDi = false;
        int ModFrq = 0;
        int ModCnt = 0;

        int EnvSpd = 0;
        boolean EnvVolSw = false;
        boolean EnvModSw = false;

        int MasterVol = 0;
        boolean WE = false;
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "FDS"; }
        @Override public String category() { return "nes"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return NpNesChip.FdsChip.class; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormFDS(frm, chipId, zoom); }

        @Override public List<Class<? extends Chip>> maskChips() {
            return List.of(NpNesChip.FdsChip.class, NesChip.FdsChip.class);
        }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            NpNesChip.FdsChip c = audio.plugin.chipRegister.chip(NpNesChip.FdsChip.class);
            if (!c.getMask(chipId, -1)) c.setMask(chipId, -1); else c.resetMask(chipId, -1);
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            // the reset always went to the vgm-side FDS, kept as the original had it
            audio.plugin.chipRegister.chip(NesChip.FdsChip.class).setMask(chipId, -1);
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            resetChannelMask(audio, NpNesChip.FdsChip.class, chipId, 0);
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(50, MDSound.Chip.MAIN_TAG, NpNesChip.FdsChip.class, "FDS", 200));
        }

        @Override public void updateMeters(Audio audio, VisVolume visVolume) {
            int fds = Meters.npNesVolume(audio, 2);
            if (fds >= 0) visVolume.put("FDS", fds * 15);
        }
    }
}
