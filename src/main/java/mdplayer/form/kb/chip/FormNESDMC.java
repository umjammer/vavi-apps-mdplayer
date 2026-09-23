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
import mdplayer.chips.NesChip;
import mdplayer.chips.NesChip.DmcChip;
import mdplayer.chips.NpNesChip;
import mdplayer.form.SettingTab;
import mdplayer.form.VisVolume;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.Meters;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdplayer.form.View;
import mdplayer.form.sys.setting.SettingNSFPanel;
import mdsound.MDSound;


public class FormNESDMC extends FormChipBase<FormNESDMC.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormNESDMC.class);

    public FormNESDMC(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeNESDMC"), null, zoom);
        drawScreenInitNESDMC(frameBuffer);
        update();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("NESDMC", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeNESDMC").getWidth() * zoom, frameSizeH + Common.getImage("planeNESDMC").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeNESDMC").getWidth() * zoom, frameSizeH + Common.getImage("planeNESDMC").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeNESDMC").getWidth() * zoom, frameSizeH + Common.getImage("planeNESDMC").getHeight() * zoom));
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
        Map<String, Object> info = audio.plugin.chipRegister.chip(NpNesChip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        int[] reg = (int[]) info.get("register");
        for (int i = 0; i < 2; i++) {
            int freq = (reg[3 + i * 4] & 0x07) * 0x100 + reg[2 + i * 4];
            int vol = reg[i * 4] & 0xf;
            int note = 104 - (int) ((12 * (Math.log(freq) / LOG_2 - LOG2_440) + NOTE_440HZ + 0.5));
            note = vol == 0 ? -1 : note;
            newParam.sqrChannels[i].note = note;
            newParam.sqrChannels[i].volume = Math.min((int) ((vol) * 1.33), 19);
            newParam.sqrChannels[i].nfrq = (reg[1 + i * 4] & 0x70) >> 4;      // Period
            newParam.sqrChannels[i].pan = (reg[1 + i * 4] & 0x07);            // Shift
            newParam.sqrChannels[i].pantp = (reg[3 + i * 4] & 0xf8) >> 3;     // Length counter load
            newParam.sqrChannels[i].kf = (reg[i * 4] & 0xc0) >> 6;            // Duty
            newParam.sqrChannels[i].dda = ((reg[i * 4] & 0x20) >> 5) != 0;    // LengthCounter
            newParam.sqrChannels[i].noise = ((reg[i * 4] & 0x10) >> 4) != 0;  // constantVolume
            newParam.sqrChannels[i].volumeL = ((reg[1 + i * 4] & 0x80) >> 7); // Sweep unit enabled
            newParam.sqrChannels[i].volumeR = ((reg[1 + i * 4] & 0x08) >> 3); // negate
        }

        Map<String, Object> info2 = audio.plugin.chipRegister.chip(NpNesChip.DmcChip.class).getInfo(chipId);
        if (info2.isEmpty()) return;

        int[] reg2 = (int[]) info2.get("register");

        int tri = reg2[0x10];
        int noi = reg2[0x11];
        int dpc = reg2[0x12];

        int freq = (reg2[3] & 0x07) * 0x100 + reg2[2];
        int note = 92 - (int) ((12 * (Math.log(freq) / LOG_2 - LOG2_440) + NOTE_440HZ + 0.5));
        newParam.triChannel.note = (reg2[0] & 0x7f) == 0 ? -1 : note;
        if ((reg2[0] & 0x80) == 0) {
            if ((reg2[13] & 0x04) == 0)
                //if ((reg2[step + 1] & 0x4) == 0)
                newParam.triChannel.note = -1;
        }

        newParam.dmcChannel.volumeR = (reg2[9] & 0x7f); // Load counter
        tri = tri == 0 ? 0 : (10 + (128 - newParam.dmcChannel.volumeR) / 128 * 9);
        newParam.triChannel.volume = newParam.triChannel.note < 0 ? 0 : tri;
        newParam.triChannel.dda = (reg2[0] & 0x80) != 0; // LengthCounterHalt
        newParam.triChannel.nfrq = (reg2[0] & 0x7f); // linear counter load (R)
        newParam.triChannel.pantp = (reg2[3] & 0xf8) >> 3; // Length counter load

        newParam.noiseChannel.volume = Math.min((int) ((reg2[4] & 0xf) * 1.33), 19);
        newParam.noiseChannel.dda = (reg2[4] & 0x20) != 0; // Envelope loop / length counter halt
        newParam.noiseChannel.noise = (reg2[4] & 0x10) != 0; // constant volume
        newParam.noiseChannel.volumeL = (reg2[6] & 0x80) >> 7; // Loop noise
        newParam.noiseChannel.volumeR = reg2[6] & 0x0f; // noise period
        newParam.noiseChannel.nfrq = (reg2[7] & 0xf8) >> 3; // Length counter load
        //newParam.noiseChannel.volume = ((reg2[1] & 0x8) != 0) ? newParam.noiseChannel.volume : 0;
        noi = noi == 0 ? 0 : 1;// (10 + (128 - newParam.dmcChannel.volumeR) / 128 * 9);
        //newParam.noiseChannel.volume = ((reg2[13] & 0x8) != 0)
        //    ? ((reg2[4] & 0x10) != 0 ? newParam.noiseChannel.volume : (10 + (128 - newParam.dmcChannel.volumeR) / 128 * 9))
        //    : 0;
        newParam.noiseChannel.volume = ((reg2[13] & 0x8) != 0)
                ? ((reg2[4] & 0x10) != 0 ? newParam.noiseChannel.volume * noi : ((10 + (128 - newParam.dmcChannel.volumeR) / 128 * 9) * noi))
                : 0;

        dpc = dpc == 0 ? 0 : (10 + (128 - newParam.dmcChannel.volumeR) / 128 * 9);
        newParam.dmcChannel.dda = (reg2[8] & 0x80) != 0; //IRQ enable
        newParam.dmcChannel.noise = (reg2[8] & 0x40) != 0; //loop
        newParam.dmcChannel.volumeL = (reg2[8] & 0x0f); //frequency
        newParam.dmcChannel.nfrq = reg2[10]; //Sample address
        newParam.dmcChannel.pantp = reg2[11]; //Sample length
        newParam.dmcChannel.volume = ((reg2[13] & 0x10) == 0) ? 0 : dpc;
    
        // chip mask state is the source of truth (see FormMain.ForceChannelMaskNES for the split)
        newParam.sqrChannels[0].mask = audio.plugin.chipRegister.chip(NesChip.class).getMask(chipId, 0);
        newParam.sqrChannels[1].mask = audio.plugin.chipRegister.chip(NesChip.class).getMask(chipId, 1);
        newParam.triChannel.mask = audio.plugin.chipRegister.chip(NesChip.DmcChip.class).getMask(chipId, 0);
        newParam.noiseChannel.mask = audio.plugin.chipRegister.chip(NpNesChip.DmcChip.class).getMask(chipId, 1);
        newParam.dmcChannel.mask = audio.plugin.chipRegister.chip(NpNesChip.DmcChip.class).getMask(chipId, 2);
    }

    @Override
    public void drawScreenParams() {
        boolean ob;
        for (int i = 0; i < 2; i++) {
            oldParam.sqrChannels[i].note = frameBuffer.drawKeyBoard(i * 2, oldParam.sqrChannels[i].note, newParam.sqrChannels[i].note, 0);
            oldParam.sqrChannels[i].volume = frameBuffer.drawVolumeM(256, 8 + i * 2 * 8, 0, oldParam.sqrChannels[i].volume, newParam.sqrChannels[i].volume, 0);
            oldParam.sqrChannels[i].nfrq = frameBuffer.font4Int2(16 * 4, (2 + i * 2) * 8, 0, 2, oldParam.sqrChannels[i].nfrq, newParam.sqrChannels[i].nfrq);
            oldParam.sqrChannels[i].pan = frameBuffer.font4Int2(19 * 4, (2 + i * 2) * 8, 0, 2, oldParam.sqrChannels[i].pan, newParam.sqrChannels[i].pan);
            oldParam.sqrChannels[i].pantp = frameBuffer.font4Int2(22 * 4, (2 + i * 2) * 8, 0, 2, oldParam.sqrChannels[i].pantp, newParam.sqrChannels[i].pantp);
            oldParam.sqrChannels[i].kf = frameBuffer.drawDuty(24, (1 + i * 2) * 8, oldParam.sqrChannels[i].kf, newParam.sqrChannels[i].kf);
            oldParam.sqrChannels[i].dda = frameBuffer.drawNESSw(32, (2 + i * 2) * 8, oldParam.sqrChannels[i].dda, newParam.sqrChannels[i].dda);
            oldParam.sqrChannels[i].noise = frameBuffer.drawNESSw(40, (2 + i * 2) * 8, oldParam.sqrChannels[i].noise, newParam.sqrChannels[i].noise);
            ob = oldParam.sqrChannels[i].volumeL != 0;
            ob = frameBuffer.drawNESSw(48, (2 + i * 2) * 8, ob, newParam.sqrChannels[i].volumeL != 0);
            oldParam.sqrChannels[i].volumeL = ob ? 1 : 0;
            ob = oldParam.sqrChannels[i].volumeR != 0;
            ob = frameBuffer.drawNESSw(56, (2 + i * 2) * 8, ob, newParam.sqrChannels[i].volumeR != 0);
            oldParam.sqrChannels[i].volumeR = ob ? 1 : 0;
            oldParam.sqrChannels[i].mask = drawChNESDMC(frameBuffer, i, oldParam.sqrChannels[i].mask, newParam.sqrChannels[i].mask, 0);
        }

        oldParam.triChannel.note = frameBuffer.drawKeyBoard(4, oldParam.triChannel.note, newParam.triChannel.note, 0);
        oldParam.triChannel.volume = frameBuffer.drawVolumeM(256, 8 + 4 * 8, 0, oldParam.triChannel.volume, newParam.triChannel.volume, 0);
        oldParam.triChannel.dda = frameBuffer.drawNESSw(36, 6 * 8, oldParam.triChannel.dda, newParam.triChannel.dda);
        oldParam.triChannel.nfrq = frameBuffer.font4Int3(13 * 4, 6 * 8, 0, 3, oldParam.triChannel.nfrq, newParam.triChannel.nfrq);
        oldParam.triChannel.pantp = frameBuffer.font4Int2(19 * 4, 6 * 8, 0, 2, oldParam.triChannel.pantp, newParam.triChannel.pantp);
        oldParam.triChannel.mask = drawChNESDMC(frameBuffer, 2, oldParam.triChannel.mask, newParam.triChannel.mask, 0);

        oldParam.noiseChannel.volume = frameBuffer.drawVolumeM(256, 8 + 3 * 8, 0, oldParam.noiseChannel.volume, newParam.noiseChannel.volume, 0);
        oldParam.noiseChannel.dda = frameBuffer.drawNESSw(228, 32, oldParam.noiseChannel.dda, newParam.noiseChannel.dda);
        oldParam.noiseChannel.noise = frameBuffer.drawNESSw(144, 32, oldParam.noiseChannel.noise, newParam.noiseChannel.noise);
        ob = oldParam.noiseChannel.volumeL != 0;
        ob = frameBuffer.drawNESSw(160, 32, ob, newParam.noiseChannel.volumeL != 0);
        oldParam.noiseChannel.volumeL = ob ? 1 : 0;
        oldParam.noiseChannel.volumeR = frameBuffer.font4Int2(176, 32, 0, 2, oldParam.noiseChannel.volumeR, newParam.noiseChannel.volumeR);
        oldParam.noiseChannel.nfrq = frameBuffer.font4Int2(196, 32, 0, 2, oldParam.noiseChannel.nfrq, newParam.noiseChannel.nfrq);
        oldParam.noiseChannel.mask = drawChNESDMC(frameBuffer, 3, oldParam.noiseChannel.mask, newParam.noiseChannel.mask, 0);

        oldParam.dmcChannel.volume = frameBuffer.drawVolumeM(256, 8 + 5 * 8, 0, oldParam.dmcChannel.volume, newParam.dmcChannel.volume, 0);
        oldParam.dmcChannel.dda = frameBuffer.drawNESSw(144, 48, oldParam.dmcChannel.dda, newParam.dmcChannel.dda);
        oldParam.dmcChannel.noise = frameBuffer.drawNESSw(152, 48, oldParam.dmcChannel.noise, newParam.dmcChannel.noise);
        oldParam.dmcChannel.volumeL = frameBuffer.font4Int2(176, 48, 0, 2, oldParam.dmcChannel.volumeL, newParam.dmcChannel.volumeL);
        oldParam.dmcChannel.volumeR = frameBuffer.font4Int3(192, 48, 0, 3, oldParam.dmcChannel.volumeR, newParam.dmcChannel.volumeR);
        oldParam.dmcChannel.nfrq = frameBuffer.font4HexByte(220, 48, 0, oldParam.dmcChannel.nfrq, newParam.dmcChannel.nfrq);
        oldParam.dmcChannel.pantp = frameBuffer.font4HexByte(244, 48, 0, oldParam.dmcChannel.pantp, newParam.dmcChannel.pantp);
        oldParam.dmcChannel.mask = drawChNESDMC(frameBuffer, 4, oldParam.dmcChannel.mask, newParam.dmcChannel.mask, 0);
    }

    @Override
    public void initScreen() {
        for (int c = 0; c < newParam.sqrChannels.length; c++) {
            newParam.sqrChannels[c].note = -1;
            newParam.sqrChannels[c].volume = 0;
            newParam.sqrChannels[c].pan = 0;
            newParam.sqrChannels[c].pantp = 0;
            newParam.sqrChannels[c].kf = 0;
            newParam.sqrChannels[c].dda = false;
            newParam.sqrChannels[c].noise = false;
            newParam.sqrChannels[c].volumeL = 0;
            newParam.sqrChannels[c].volumeR = 0;
        }
        newParam.triChannel.dda = false;
        newParam.triChannel.note = -1;
        newParam.triChannel.volume = 0;
        newParam.triChannel.nfrq = 0;
        newParam.triChannel.pantp = 0;

        newParam.noiseChannel.volume = 0;
        newParam.noiseChannel.dda = false;
        newParam.noiseChannel.noise = false;
        newParam.noiseChannel.volumeL = 0;
        newParam.noiseChannel.volumeR = 0;
        newParam.noiseChannel.nfrq = 0;
        newParam.noiseChannel.volume = 0;

        newParam.dmcChannel.dda = false;
        newParam.dmcChannel.noise = false;
        newParam.dmcChannel.volumeL = 0;
        newParam.dmcChannel.volumeR = 0;
        newParam.dmcChannel.nfrq = 0;
        newParam.dmcChannel.pantp = 0;
        newParam.dmcChannel.volume = 0;
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
                    for (int ch = 0; ch < 2; ch++) {
                        if (newParam.sqrChannels[ch].mask)
                            parent.resetChannelMask(NesChip.class, chipId, ch);
                        else
                            parent.setChannelMask(NesChip.class, chipId, ch);
                    }

                    if (newParam.triChannel.mask)
                        parent.resetChannelMask(DmcChip.class, chipId, 0);
                    else
                        parent.setChannelMask(DmcChip.class, chipId, 0);

                    if (newParam.noiseChannel.mask)
                        parent.resetChannelMask(DmcChip.class, chipId, 1);
                    else
                        parent.setChannelMask(DmcChip.class, chipId, 1);

                    if (newParam.dmcChannel.mask)
                        parent.resetChannelMask(DmcChip.class, chipId, 2);
                    else
                        parent.setChannelMask(DmcChip.class, chipId, 2);
                }
                return;
            }

            // keyboard
            if (py < 7 * 8) {
                if (ev.getButton() == MouseEvent.BUTTON2) {
                    for (int i = 0; i < 5; i++) {
                        // Unmask.
                        if (i < 2) parent.resetChannelMask(NesChip.class, chipId, i);
                        else parent.resetChannelMask(DmcChip.class, chipId, i - 2);
                    }

                    return;
                }

                int ch = (py / 8) - 1;
                if (ch == 1) return;
                ch = ch == 3 ? 3 : (ch == 5 ? 4 : ch / 2);
                if (ch < 0) return;

                if (ev.getButton() == MouseEvent.BUTTON1) {
                    // Mask.
                    if (ch < 2) parent.setChannelMask(NesChip.class, chipId, ch);
                    else parent.setChannelMask(DmcChip.class, chipId, ch - 2);

                }
            }
        }
    };

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeNESDMC");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 56));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmNESDMC
        //
        this.setPreferredSize(new Dimension(320, 56));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmNESDMC");
        this.setTitle("NES & DMC");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static void drawScreenInitNESDMC(FrameBuffer screen) {
        if (screen == null)
            return;

        for (int ch = 0; ch < 3; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                screen.drawKbn(32 + kx, ch * 16 + 8, kt, 0);
            }
            screen.drawFont8(296, ch * 16 + 8, 1, "   ");
            boolean m = true;
            drawChNESDMC(screen, ch, m, false, 0);
        }
    }

    private static Boolean drawChNESDMC(FrameBuffer screen, int ch, Boolean om, Boolean nm, int tp) {
        if (om == nm) {
            return om;
        }

        drawChNESDMC_P(screen, ch, nm == null ? false : nm, tp);
        om = nm;
        return om;
    }

    private static void drawChNESDMC_P(FrameBuffer screen, int ch, boolean mask, int tp) {
        if (screen == null)
            return;

        switch (ch) {
            case 0:
                screen.drawByteArray(0, 8, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 48, 8, 16, 8);
                screen.drawFont8(16, 8, mask ? 1 : 0, "1");
                break;
            case 1:
                screen.drawByteArray(0, 24, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 48, 8, 16, 8);
                screen.drawFont8(16, 24, mask ? 1 : 0, "2");
                break;
            case 2:
                screen.drawByteArray(0, 40, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 64, 8, 32, 8);
                break;
            case 3:
                screen.drawByteArray(112, 32, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 96, 8, 24, 8);
                break;
            case 4:
                screen.drawByteArray(112, 48, FrameBuffer.rType[tp * 2 + (mask ? 1 : 0)], 128, 0, 16, 16, 8);
                break;
        }
    }

//#endregion

    /** this panel's channel row: the common core plus what only this chip displays */
    static class Channel extends ChannelParams {

        boolean dda = false;
        int kf = -1;
        int nfrq = -1;
        boolean noise = false;
    }

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final Channel[] sqrChannels = {new Channel(), new Channel()};
        final Channel triChannel = new Channel();
        final Channel noiseChannel = new Channel();
        final Channel dmcChannel = new Channel();
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "NESDMC"; }
        @Override public String menuText() { return "NES&DMC"; }
        @Override public String category() { return "nes"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return NesChip.class; }
        @Override public String title(int chipId) { return "NES&DMC (%s)".formatted(chipId == 0 ? "Primary" : "Secondary"); }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormNESDMC(frm, chipId, zoom); }
        @Override public List<SettingTab> settingTabs() { return List.of(new SettingNSFPanel()); }

        @Override public List<Class<? extends mdplayer.Chip>> maskChips() {
            return List.of(NesChip.class, NesChip.DmcChip.class);
        }

        @Override public void setChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (chip.equals(NesChip.class)) {
                NesChip c = audio.plugin.chipRegister.chip(NesChip.class);
                if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
                return;
            }
            switch (ch) {
                case 0: {
                    // the triangle channel goes through the vgm-side chip, the rest through the
                    // nsf-side one — kept as the original had it
                    NesChip.DmcChip c = audio.plugin.chipRegister.chip(NesChip.DmcChip.class);
                    if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
                    break;
                }
                case 1:
                case 2: {
                    NpNesChip.DmcChip c = audio.plugin.chipRegister.chip(NpNesChip.DmcChip.class);
                    if (!c.getMask(chipId, ch)) c.setMask(chipId, ch); else c.resetMask(chipId, ch);
                    break;
                }
            }
        }

        @Override public void resetChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch) {
            if (chip.equals(NesChip.class)) {
                switch (ch) {
                    case 0:
                    case 1:
                        audio.plugin.chipRegister.chip(NesChip.class).resetMask(chipId, ch);
                        break;
                    case 2:
                        audio.plugin.chipRegister.chip(NesChip.DmcChip.class).resetMask(chipId, 0);
                        break;
                    case 3:
                        audio.plugin.chipRegister.chip(NesChip.DmcChip.class).resetMask(chipId, 1);
                        break;
                    case 4:
                        audio.plugin.chipRegister.chip(NesChip.DmcChip.class).resetMask(chipId, 2);
                        break;
                }
                return;
            }
            if (ch >= 0 && ch < 3) {
                audio.plugin.chipRegister.chip(NesChip.DmcChip.class).resetMask(chipId, ch);
            }
        }

        @Override public void forceChannelMask(Audio audio, Class<? extends mdplayer.Chip> chip, int chipId, int ch, boolean mask) {
            if (ch == 0 || ch == 1) {
                if (audio.plugin.chipRegister.chip(NesChip.class).getMask(chipId, ch)) {
                    audio.plugin.chipRegister.chip(NesChip.class).setMask(chipId, ch);
                } else {
                    audio.plugin.chipRegister.chip(NesChip.class).resetMask(chipId, ch);
                }
            } else if (ch == 2) {
                // the triangle channel's mute lives on the vgm-side chip, the rest on the nsf-side
                // one — matching where setChannelMask toggles them
                if (audio.plugin.chipRegister.chip(NesChip.DmcChip.class).getMask(chipId, 0)) {
                    audio.plugin.chipRegister.chip(NesChip.DmcChip.class).setMask(chipId, 0);
                } else {
                    audio.plugin.chipRegister.chip(NesChip.DmcChip.class).resetMask(chipId, 0);
                }
            } else if (ch == 3 || ch == 4) {
                if (audio.plugin.chipRegister.chip(NpNesChip.DmcChip.class).getMask(chipId, ch - 2)) {
                    audio.plugin.chipRegister.chip(NesChip.DmcChip.class).setMask(chipId, ch - 2);
                } else {
                    audio.plugin.chipRegister.chip(NesChip.DmcChip.class).resetMask(chipId, ch - 2);
                }
            }
        }

        @Override public void reapplyChannelMasks(Audio audio, int chipId) {
            for (int ch = 0; ch < 5; ch++)
                forceChannelMask(audio, NesChip.class, chipId, ch, false /* state is read per channel */);
        }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(48, MDSound.Chip.MAIN_TAG, NesChip.class, "APU", 200),
                    new MixerSlot(49, MDSound.Chip.MAIN_TAG, NesChip.DmcChip.class, "DMC", 350));
        }

        @Override public void updateMeters(Audio audio, VisVolume visVolume) {
            int apu = Meters.npNesVolume(audio, 0);
            if (apu >= 0) visVolume.put("APU", apu * 15);
            int dmc = Meters.npNesVolume(audio, 1);
            if (dmc >= 0) visVolume.put("DMC", dmc * 15);
        }
    }
}
