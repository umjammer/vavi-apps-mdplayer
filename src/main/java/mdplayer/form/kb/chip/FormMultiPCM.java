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
import mdplayer.chips.BaseChip;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.Tables;
import mdplayer.chips.MultiPcmChip;
import mdplayer.form.VisVolume;
import mdplayer.form.kb.PcmChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdplayer.form.View;
import mdsound.MDSound;


public class FormMultiPCM extends FormChipBase<FormMultiPCM.Params> {

    private static final Preferences prefs = Preferences.userNodeForPackage(FormMultiPCM.class);

    public FormMultiPCM(FormMain frm, int chipId, int zoom) {
        super(frm, chipId, zoom, new Params(), new Params());

        initializeComponent();

        bind(Common.getImage("planeMultiPCM"));
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("MultiPCM", chipId, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeMultiPCM").getWidth() * zoom, frameSizeH + Common.getImage("planeMultiPCM").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeMultiPCM").getWidth() * zoom, frameSizeH + Common.getImage("planeMultiPCM").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeMultiPCM").getWidth() * zoom, frameSizeH + Common.getImage("planeMultiPCM").getHeight() * zoom));
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
                    for (ch = 0; ch < 28; ch++) {
                        if (newParam.channels[ch].mask)
                            parent.resetChannelMask(MultiPcmChip.class, chipId, ch);
                        else
                            parent.setChannelMask(MultiPcmChip.class, chipId, ch);
                    }
                }
                return;
            }

            ch = (py / 8) - 1;
            if (ch < 0) return;

            if (ch < 28) {
                if (ev.getButton() == MouseEvent.BUTTON1) {
                    parent.setChannelMask(MultiPcmChip.class, chipId, ch);
                    return;
                }

                for (ch = 0; ch < 28; ch++) parent.resetChannelMask(MultiPcmChip.class, chipId, ch);

            }
        }
    };

    @Override
    public void initScreen() {
        boolean multiPCMType = false;// (chipId == 0) ? parent.setting.multiPCMType.UseScci : parent.setting.MultiPCMSType.UseScci;
        int tp = multiPCMType ? 1 : 0;
        for (int ch = 0; ch < 28; ch++) {
            for (int ot = 0; ot < 12 * 8; ot++) {
                int kx = Tables.kbl[(ot % 12) * 2] + ot / 12 * 28;
                int kt = Tables.kbl[(ot % 12) * 2 + 1];
                frameBuffer.drawKbn(32 + kx, ch * 8 + 8, kt, tp);
            }
            //frameBuffer.drawFont8(296, ch * 8 + 8, 1, "   ");
            //frameBuffer.drawPanType2P(24, ch * 8 + 8, 0, tp);
            //frameBuffer.ChMultiPCM_P(0, 8 + ch * 8, ch, false, tp);
            //frameBuffer.Volume(ch, 1,d, 0, tp);
            //frameBuffer.Volume(ch, 2,d, 0, tp);
        }
    }

    private static int searchMultiPCMNote(int freq) {
        //double m = Double.MAX_VALUE;

        //int clock = audio.clockMultiPCM;

        int n = 0;
        //for (int i = 0; i < 12 * 8; i++) {
        //    int a = (int) (0x10000 // Number of counts required to advance 1 sample
        //        * 8000.0
        //        * Tables.pcmMulTbl[i % 12 + 12]
        //        * Math.pow(2, (i / 12 - 3 + 2))
        //        / clock
        //        );

        //    if (freq > a) {
        //        m = a;
        //        n = i;
        //    }
        //}
        return n;
    }

    @Override
    public void changeScreenParams() {
        Map<String, Object> info = audio.plugin.chipRegister.chip(MultiPcmChip.class).getInfo(chipId);
        if (info.isEmpty()) return;

        for (int ch = 0; ch < 28; ch++) {
            newParam.channels[ch].pan = (int) info.get("channels." + ch + ".pan");

            newParam.channels[ch].bit[0] = (boolean) info.get("channels." + ch + ".bit.0");
            newParam.channels[ch].freq = (int) info.get("channels." + ch + ".freq");
            newParam.channels[ch].bit[1] = (boolean) info.get("channels." + ch + ".bit.1");
            newParam.channels[ch].inst[1] = (int) info.get("channels." + ch + ".inst.1");
            newParam.channels[ch].inst[2] = (int) info.get("channels." + ch + ".inst.2");
            newParam.channels[ch].inst[3] = (int) info.get("channels." + ch + ".inst.3");
            newParam.channels[ch].inst[4] = (int) info.get("channels." + ch + ".inst.4");

            if (info.get("channels." + ch + ".sadr") != null) {
                newParam.channels[ch].inst[0] = (int) info.get("channels." + ch + ".inst.0");
                newParam.channels[ch].sadr = (int) info.get("channels." + ch + ".sadr");
                newParam.channels[ch].eadr = (int) info.get("channels." + ch + ".eadr");
                newParam.channels[ch].ladr = (int) info.get("channels." + ch + ".ladr");
                newParam.channels[ch].inst[5] = (int) info.get("channels." + ch + ".inst.5");
                newParam.channels[ch].inst[6] = (int) info.get("channels." + ch + ".inst.6");
                newParam.channels[ch].inst[7] = (int) info.get("channels." + ch + ".inst.7");
                newParam.channels[ch].inst[8] = (int) info.get("channels." + ch + ".inst.8");
                newParam.channels[ch].inst[9] = (int) info.get("channels." + ch + ".inst.9");
                newParam.channels[ch].inst[10] = (int) info.get("channels." + ch + ".inst.10");
                newParam.channels[ch].inst[11] = (int) info.get("channels." + ch + ".inst.11");
                newParam.channels[ch].inst[12] = (int) info.get("channels." + ch + ".inst.12");
            }

            if (newParam.channels[ch].bit[0]) {
                newParam.channels[ch].volumeL =
                        Math.min((int) (((0x7f - newParam.channels[ch].inst[1]) * ((newParam.channels[ch].pan >> 4) & 0xf) / (double) 0xf) / 4.5), 19);
                newParam.channels[ch].volumeR =
                        Math.min((int) (((0x7f - newParam.channels[ch].inst[1]) * ((newParam.channels[ch].pan) & 0xf) / (double) 0xf) / 4.5), 19);
            } else {
                newParam.channels[ch].note = -1;
                if (newParam.channels[ch].volumeL > 0) newParam.channels[ch].volumeL--;
                if (newParam.channels[ch].volumeR > 0) newParam.channels[ch].volumeR--;
            }
        }
    }

    @Override
    public void drawScreenParams() {
        PcmChannelParams oyc;
        PcmChannelParams nyc;

        for (int ch = 0; ch < 28; ch++) {
            oyc = oldParam.channels[ch];
            nyc = newParam.channels[ch];

            oyc.pan = frameBuffer.PanType2(ch, oyc.pan, nyc.pan, 0);

            oldParam.channels[ch].bit[0] = frameBuffer.drawNESSw(64 * 4, ch * 8 + 8, oldParam.channels[ch].bit[0], newParam.channels[ch].bit[0]);
            oyc.inst[0] = frameBuffer.font4HexByte(4 * 66, ch * 8 + 8, 0, oyc.inst[0], nyc.inst[0]);
            oyc.freq = frameBuffer.font4Hex12Bit(4 * 69, ch * 8 + 8, 0, oyc.freq, nyc.freq);
            oldParam.channels[ch].bit[1] = frameBuffer.drawNESSw(72 * 4, ch * 8 + 8, oldParam.channels[ch].bit[1], newParam.channels[ch].bit[1]); // TL Interpolation
            oyc.inst[1] = frameBuffer.font4HexByte(4 * 74, ch * 8 + 8, 0, oyc.inst[1], nyc.inst[1]); // TL
            oyc.inst[2] = frameBuffer.font4Hex4Bit(4 * 77, ch * 8 + 8, 0, oyc.inst[2], nyc.inst[2]); // LFO freq
            oyc.inst[3] = frameBuffer.font4Hex4Bit(4 * 79, ch * 8 + 8, 0, oyc.inst[3], nyc.inst[3]); // PLFO
            oyc.inst[4] = frameBuffer.font4Hex4Bit(4 * 81, ch * 8 + 8, 0, oyc.inst[4], nyc.inst[4]); // ALFO
            oyc.sadr = frameBuffer.font4Hex24Bit(4 * 83, ch * 8 + 8, 0, oyc.sadr, nyc.sadr);
            oyc.eadr = frameBuffer.font4Hex16Bit(4 * 90, ch * 8 + 8, 0, oyc.eadr, nyc.eadr);
            oyc.ladr = frameBuffer.font4Hex16Bit(4 * 95, ch * 8 + 8, 0, oyc.ladr, nyc.ladr);
            oyc.inst[5] = frameBuffer.font4HexByte(4 * 100, ch * 8 + 8, 0, oyc.inst[5], nyc.inst[5]); // LFOVIB
            oyc.inst[6] = frameBuffer.font4Hex4Bit(4 * 103, ch * 8 + 8, 0, oyc.inst[6], nyc.inst[6]); // AR
            oyc.inst[7] = frameBuffer.font4Hex4Bit(4 * 105, ch * 8 + 8, 0, oyc.inst[7], nyc.inst[7]); // DR1
            oyc.inst[8] = frameBuffer.font4Hex4Bit(4 * 107, ch * 8 + 8, 0, oyc.inst[8], nyc.inst[8]); // DR2
            oyc.inst[9] = frameBuffer.font4Hex4Bit(4 * 109, ch * 8 + 8, 0, oyc.inst[9], nyc.inst[9]); // DL
            oyc.inst[10] = frameBuffer.font4Hex4Bit(4 * 111, ch * 8 + 8, 0, oyc.inst[10], nyc.inst[10]); // RR
            oyc.inst[11] = frameBuffer.font4Hex4Bit(4 * 113, ch * 8 + 8, 0, oyc.inst[11], nyc.inst[11]); // KRS
            oyc.inst[12] = frameBuffer.font4HexByte(4 * 115, ch * 8 + 8, 0, oyc.inst[12], nyc.inst[12]); // AM

            oyc.volumeL = frameBuffer.drawVolumeXY(117, ch * 2 + 2, 1, oyc.volumeL, nyc.volumeL, 0); // Front
            oyc.volumeR = frameBuffer.drawVolumeXY(117, ch * 2 + 3, 1, oyc.volumeR, nyc.volumeR, 0); // Front

            oyc.note = drawKeyBoardToMultiPCM(frameBuffer, ch, oyc.note, nyc.note, 0);
            //FrameBuffer.ChMultiPCM(frameBuffer, ch,oyc.mask, nyc.mask, 0);
        }
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeMultiPCM");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(527, 225));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmMultiPCM
        //
        this.setPreferredSize(new Dimension(527, 225));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmMultiPCM");
        this.setTitle("MultiPCM");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;

//#region draw buffer

    private static int drawKeyBoardToMultiPCM(FrameBuffer screen, int y, int ot, int nt, int tp) {
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
            screen.drawFont8(63 * 8 + 4, y, 1, Tables.kbn[nt % 12]);
            if (nt / 12 < 8) {
                screen.drawFont8(65 * 8 + 4, y, 1, Tables.kbo[nt / 12]);
            }
        } else {
            screen.drawFont8(63 * 8 + 4, y, 1, "   ");
        }

        ot = nt;
        return ot;
    }

//#endregion

    /** this panel's per-frame draw state, diffed new against old (see {@link FormChipBase}) */
    static class Params {

        final PcmChannelParams[] channels = {
                new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(),
                new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(),
                new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(),
                new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(),

                new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(),
                new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(),
                new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams(), new PcmChannelParams()
        };
    }

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "MultiPCM"; }
        @Override public String category() { return "pcm"; }
        @Override public Class<? extends mdplayer.Chip> chip() { return MultiPcmChip.class; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormMultiPCM(frm, chipId, zoom); }

        @Override public List<MixerSlot> mixerSlots() {
            return List.of(new MixerSlot(42, MDSound.Chip.MAIN_TAG, MultiPcmChip.class, "multiPCM", 200));
        }

        @Override public void updateMeters(mdplayer.Audio audio, VisVolume visVolume) {
            int val = 0;
            try {
                mdplayer.Chip chip = audio.plugin.chipRegister.chip(MultiPcmChip.class);
                if (chip instanceof BaseChip base) {
                    Map<String, Object> info = base.getInfo(0);
                    if (!info.isEmpty()) {
                        for (int ch = 0; ch < 28; ch++) {
                            Boolean bit = (Boolean) info.get("channels." + ch + ".bit.0");
                            if (bit != null && bit) {
                                Integer inst1 = (Integer) info.get("channels." + ch + ".inst.1");
                                Integer pan = (Integer) info.get("channels." + ch + ".pan");
                                if (inst1 != null && pan != null) {
                                    int panL = (pan >> 4) & 0xf;
                                    int panR = pan & 0xf;
                                    int l = (0x7f - inst1) * panL / 0xf;
                                    int r = (0x7f - inst1) * panR / 0xf;
                                    int v = Math.max(l, r) * 70;
                                    if (v > val) val = v;
                                }
                            }
                        }
                    }
                }
            } catch (Exception _) {
            }
            visVolume.put("multiPCM", val);
        }
    }
}
