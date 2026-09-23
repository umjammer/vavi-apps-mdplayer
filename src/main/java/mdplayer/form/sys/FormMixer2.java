package mdplayer.form.sys;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileFilter;

import mdplayer.Audio;
import mdplayer.Chip;
import mdplayer.chips.MidiPlugin;
import mdplayer.chips.RealChipPlugin;
import mdplayer.Common;
import mdplayer.form.FrameBuffer;
import mdplayer.PlayList;
import mdplayer.form.ScreenPanel;
import mdplayer.Setting;
import mdplayer.form.VisVolume;
import vavi.util.compat.Tuple;

import static java.lang.System.getLogger;
import mdplayer.form.kb.ViewProvider;


class FormMixer2 extends JFrame {

    private static final Logger logger = getLogger(FormMixer2.class.getName());

    public boolean isClosed = false;
    public int x = -1;
    public int y = -1;
    private final FormMain parent;
    private int frameSizeW = 0;
    private int frameSizeH = 0;
    private final int zoom;
    private int chipn = -1;

    private final FrameBuffer frameBuffer = new FrameBuffer();

    private static final Preferences prefs = Preferences.userNodeForPackage(FormMixer2.class);
    private final Audio audio = Audio.getInstance();

    /**
     * Draw state of one mixer slot — the fader position and the two-stage level meter.
     * View state owned by this panel; the balance settings and {@link #visVolume} are
     * the sources of truth.
     */
    private static class VolumeInfo {

        int volume = -9999;
        int visVolume1 = -1;
        int visVolume2 = -1;
        int visVol2Cnt = 30;
    }

    /** which meter of {@link #visVolume} feeds a slot, and its scaling */
    private record VisSource(String key, int div) {}

    /** per-slot meter sources, aligned with {@link #setVolume}; gimic slots have faders only */
    private final VisSource[] visSources = new VisSource[64];

    /** per-slot draw state, aligned with {@link #setVolume} plus the two gimic slots; null = unused slot */
    private final VolumeInfo[] newVolumes = new VolumeInfo[64];
    private final VolumeInfo[] oldVolumes = new VolumeInfo[64];

    /** the two gimic fader slots follow the chip slots */
    private static final int GIMIC_OPN = 62;
    private static final int GIMIC_OPNA = 63;

    /**
     * The MIDI path's fader, in the slot the skin left blank between OPX and AY10 (its
     * {@code MIDI} label was added to {@code planeMixer.png} out of the skin's own glyphs).
     * <p>
     * It belongs to no chip -- a song played through {@link MidiPlugin} renders nothing into the
     * mixer at all -- so it is not a {@link ViewProvider.MixerSlot}; its fader moves
     * {@link Setting.Balance#getMidiVolume()} and its meter follows the MIDI notes themselves.
     */
    private static final int MIDI = 24;

    private void initVolumeSlots() {
        visSources[0] = new VisSource("master", 250);
        visSources[MIDI] = new VisSource("midi", 200);
        for (ViewProvider p : ViewProvider.providers()) {
            for (ViewProvider.MixerSlot s : p.mixerSlots()) {
                visSources[s.slot()] = new VisSource(s.visKey(), s.visDiv());
            }
        }

        for (int i = 0; i < newVolumes.length; i++) {
            boolean used = i == 0 || i == MIDI || i >= GIMIC_OPN
                    || (i < setVolume.length && setVolume[i] != null);
            if (used) {
                newVolumes[i] = new VolumeInfo();
                oldVolumes[i] = new VolumeInfo();
            }
        }
    }

    public FormMixer2(FormMain frm, int zoom) {
        parent = frm;
        this.zoom = zoom;

        initializeComponent();
        // closed, not just hidden: HIDE_ON_CLOSE (JFrame's default) never fires windowClosed, so
        // isClosed stayed false and the player remembered a closed window as open
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pbScreen.addMouseWheelListener(this.pbScreen_MouseWheel);

        initVolumeSlots();
        frameBuffer.add(pbScreen, Common.getImage("planeMixer"), null, zoom);
        screenInitMixer(frameBuffer);
        update();
        changeZoom();
    }

    private final MouseWheelListener pbScreen_MouseWheel = new MouseAdapter() {
        @Override
        public void mouseWheelMoved(MouseWheelEvent ev) {
            int px = ev.getX() / parent.setting.getOther().getZoom();
            int py = ev.getY() / parent.setting.getOther().getZoom();
            chipn = px / 20 + (py / 72) * 16;
            int delta = (int) Math.signum(ev.getWheelRotation());
            setVolume(chipn, false, delta);
        }
    };

    private void setVolume(int i, boolean isAbs, int delta) {
        if (i == 0) {
            audio.plugin.setMasterVolume(isAbs, delta);
        } else if (i == MIDI) {
            audio.plugin.setMidiVolume(isAbs, delta);
        } else if (i == setVolume.length) {
            audio.plugin.chipRegister.plugin(RealChipPlugin.class).setGimicOPNVolume(false, delta);
        } else if (i == setVolume.length + 1) {
            audio.plugin.chipRegister.plugin(RealChipPlugin.class).setGimicOPNAVolume(false, delta);
        } else if (i > 0 && i < setVolume.length) {
            var t = setVolume[i];
            if (t != null) {
                audio.plugin.setVolume(t.getItem1(), t.getItem2(), isAbs, delta);
            }
        }
    }

    public void update() {
        frameBuffer.refresh(null);
    }

    protected boolean getShowWithoutActivation() {
        return true;
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPosMixer(getLocation());
            isClosed = true;
        }

        @Override
        public void windowOpened(WindowEvent ev) {
            setLocation(new Point(x, y));

            frameSizeW = getWidth() - getSize().width;
            frameSizeH = getHeight() - getSize().height;

            changeZoom();
        }
    };

    private void changeZoom() {
        int w = Common.getImage("planeMixer").getWidth() * zoom;
        int h = Common.getImage("planeMixer").getHeight() * zoom;

        // size the skin panel and let pack() add the title-bar inset; the old code added
        // frameSizeW/H (the WinForms Width-minus-ClientSize chrome, always zero in Swing) and never
        // packed, so the window came up the wrong height and the skin was clipped
        pbScreen.setPreferredSize(new Dimension(w, h));
        setResizable(false);
        componentListener.componentResized(null);
        setPreferredSize(null); // clear any explicit frame size so pack() derives it from the content
        pack();
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

    /** the meters: master is fed by frmMain from the rendered wave, the per-chip ones by nothing yet */
    public final VisVolume visVolume = new VisVolume();

    private void updateVisualVolumes() {
        if (audio.plugin == null || audio.plugin.chipRegister == null) {
            return;
        }

        for (ViewProvider p : ViewProvider.providers()) {
            p.updateMeters(audio, visVolume);
        }

        // no chip to ask, so the MIDI meter follows the loudest note the channels are holding
        int velocity = 0;
        MidiPlugin midi = audio.plugin.chipRegister.plugin(MidiPlugin.class);
        for (int ch = 0; ch < 16; ch++) velocity = Math.max(velocity, midi.velocity(ch));
        visVolume.put("midi", velocity * 70);
    }

    public void screenChangeParams() {
        updateVisualVolumes();

        newVolumes[0].volume = parent.setting.getBalance().getMasterVolume();
        newVolumes[MIDI].volume = parent.setting.getBalance().getMidiVolume();
        for (int i = 1; i < setVolume.length; i++) {
            if (setVolume[i] != null) {
                newVolumes[i].volume = parent.setting.getBalance().getVolume(setVolume[i].getItem1(), setVolume[i].getItem2());
            }
        }
        newVolumes[GIMIC_OPN].volume = parent.setting.getBalance().getGimicOPNVolume();
        newVolumes[GIMIC_OPNA].volume = parent.setting.getBalance().getGimicOPNAVolume();

        for (int i = 0; i < visSources.length; i++) {
            VisSource src = visSources[i];
            if (src == null) continue;
            VolumeInfo vi = newVolumes[i];
            vi.visVolume1 = Common.range(visVolume.get(src.key()) / src.div(), 0, 44);
            if (vi.visVolume2 <= vi.visVolume1) {
                vi.visVolume2 = vi.visVolume1;
                vi.visVol2Cnt = 30;
            }
        }
    }

    public void screenDrawParams() {
        for (int num = 0; num < newVolumes.length; num++) {
            if (newVolumes[num] == null) continue;
            if (num >= GIMIC_OPN) {
                drawGVolAndFader(num, oldVolumes[num], newVolumes[num]);
            } else {
                drawVolAndFader(num, oldVolumes[num], newVolumes[num]);
            }
        }
    }

    private void drawVolAndFader(int num, VolumeInfo oVI, VolumeInfo nVI) {
        drawFader(
                frameBuffer,
                5 + (num % 16) * 20,
                16 + (num / 16) * 8 * 9,
                num == 0 ? 0 : 1,
                oVI.volume,
                nVI.volume);
        nVI.visVol2Cnt--;
        if (nVI.visVol2Cnt == 0) {
            nVI.visVol2Cnt = 1;
            if (nVI.visVolume2 > 0) nVI.visVolume2--;
        }
        MixerVolume(
                frameBuffer,
                2 + (num % 16) * 20,
                10 + (num / 16) * 8 * 9,
                oVI.visVolume1,
                nVI.visVolume1,
                oVI.visVolume2,
                nVI.visVolume2);
        oVI.volume = nVI.volume;
        oVI.visVolume1 = nVI.visVolume1;
        oVI.visVolume2 = nVI.visVolume2;
    }

    private void drawGVolAndFader(int num, VolumeInfo oVI, VolumeInfo nVI) {
        drawGFader(
                frameBuffer,
                5 + (num % 16) * 20,
                16 + (num / 16) * 8 * 9,
                num == 0 ? 0 : 1,
                oVI.volume,
                nVI.volume);
        nVI.visVol2Cnt--;
        if (nVI.visVol2Cnt == 0) {
            nVI.visVol2Cnt = 1;
            if (nVI.visVolume2 > 0) nVI.visVolume2--;
        }
        MixerVolume(
                frameBuffer,
                2 + (num % 16) * 20,
                10 + (num / 16) * 8 * 9,
                oVI.visVolume1,
                nVI.visVolume1,
                oVI.visVolume2,
                nVI.visVolume2);
        oVI.volume = nVI.volume;
        oVI.visVolume1 = nVI.visVolume1;
        oVI.visVolume2 = nVI.visVolume2;
    }

    public void screenInit() {
        visVolume.put("master", -1);
        visVolume.put("midi", -1);
        for (ViewProvider p : ViewProvider.providers()) {
            for (ViewProvider.MixerSlot s : p.mixerSlots()) {
                visVolume.put(s.visKey(), -1);
            }
        }
    }

    private final KeyListener frmMixer2_KeyDown = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
        }
    };

    private final MouseListener frmMixer2_MouseClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
            int px = ev.getX() / parent.setting.getOther().getZoom();
            int py = ev.getY() / parent.setting.getOther().getZoom();

            chipn = px / 20 + (py / 72) * 16;
        }
    };

    private final MouseListener pbScreen_MouseClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
            int px = ev.getX() / parent.setting.getOther().getZoom();
            int py = ev.getY() / parent.setting.getOther().getZoom();
            chipn = px / 20 + (py / 72) * 16;
            if (ev.getButton() == MouseEvent.BUTTON3) {
                setVolume(chipn, true, 0);
            }
        }

        @Override
        public void mouseEntered(MouseEvent ev) {
            pbScreen.requestFocus();
        }
    };

    private final MouseMotionListener pbScreen_MouseDrag = new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent ev) {
            int px = ev.getX() / parent.setting.getOther().getZoom();
            int py = ev.getY() / parent.setting.getOther().getZoom();
            chipn = px / 20 + (py / 72) * 16;
            py = py % 72;

            int n;
            if (chipn < 62) {
                if (py < 18) {
                    n = Math.min((18 - py), 8);
                    n = (int) (n * 2.5);
                } else if (py == 18) {
                    n = 0;
                } else {
                    n = Math.max((18 - py), -35);
                    n = (int) (n * (192.0 / 35.0));
                }
            } else {
                n = (int) ((72 - py) * (127.0 / 72.0));
            }

            setVolume(chipn, true, n);
        }
    };

    private void tsmiLoadDriverBalance_Click(ActionEvent ev) {

    }

    private void tsmiLoadSongBalance_Click(ActionEvent ev) {

    }

    private void tsmiSaveDriverBalance_Click(ActionEvent ev) {
        try {
            String retMsg = parent.saveDriverBalance(parent.setting.getBalance().clone());
            if (!retMsg.isEmpty()) {
                JOptionPane.showMessageDialog(null, "The driver's Mixer-Balance [%s] has been saved to the settings folder.".formatted(retMsg), "Save", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            JOptionPane.showMessageDialog(null, "%s".formatted(ex.getMessage()), "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tsmiSaveSongBalance_Click(ActionEvent ev) {
        try {
            Setting.Balance bln = parent.setting.getBalance().clone();
            PlayList.Music ms = parent.getPlayingMusicInfo();
            if (ms == null) {
                JOptionPane.showMessageDialog(null, "Performance information could not be retrieved.\nPlease try again during or immediately after the performance.",
                        "Information acquisition failure", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JFileChooser sfd = new JFileChooser();
            sfd.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.getName().toLowerCase().endsWith(".mbc");
                }

                @Override
                public String getDescription() {
                    return " Mixer - Balance(*.mbc)";
                }
            });
            sfd.setDialogTitle(" Mixer - Save Balance");
            sfd.setCurrentDirectory(Path.of(ms.arcFileName == null || ms.arcFileName.isEmpty() ? ms.fileName : ms.arcFileName).getParent().toFile());
            if (!parent.setting.getAutoBalance().getSamePositionAsSongData())
                // resolved, not assigned: assigning here moved where Setting.xml itself is saved
                sfd.setCurrentDirectory(Common.settingFilePath.resolve("MixerBalance").toFile());

//            sfd.RestoreDirectory = false;
            sfd.setSelectedFile(Path.of(Path.of((ms.arcFileName == null || ms.arcFileName.isEmpty() ? ms.fileName : ms.arcFileName)).getFileName() + ".mbc").toFile());
//            sfd.CheckPathExists = true;

            if (sfd.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            bln.save(Path.of(sfd.getSelectedFile().getPath()));
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            JOptionPane.showMessageDialog(null, "%s".formatted(ex.getMessage()), "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();
        this.ctxtMenu = new JPopupMenu();
        this.tsmiLoadDriverBalance = new JMenuItem();
        this.tsmiLoadSongBalance = new JMenuItem();
        this.toolStripSeparator1 = new JSeparator();
        this.tsmiSaveDriverBalance = new JMenuItem();
        this.tsmiSaveSongBalance = new JMenuItem();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeMixer");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 288));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick); // TODO
        this.pbScreen.addMouseMotionListener(this.pbScreen_MouseDrag);
        this.pbScreen.addMouseWheelListener(this.pbScreen_MouseWheel); // TODO
        //
        // ctxtMenu
        //
        this.ctxtMenu.add(this.tsmiLoadDriverBalance);
        this.ctxtMenu.add(this.tsmiLoadSongBalance);
        this.ctxtMenu.add(this.toolStripSeparator1);
        this.ctxtMenu.add(this.tsmiSaveDriverBalance);
        this.ctxtMenu.add(this.tsmiSaveSongBalance);
        this.ctxtMenu.setName("ctxtMenu");
        this.ctxtMenu.setPreferredSize(new Dimension(224, 98));
        //
        // tsmiLoadDriverBalance
        //
        this.tsmiLoadDriverBalance.setEnabled(false);
        this.tsmiLoadDriverBalance.setName("tsmiLoadDriverBalance");
        this.tsmiLoadDriverBalance.setPreferredSize(new Dimension(223, 22));
        this.tsmiLoadDriverBalance.setText("Load Driver Mixer - Balance");
        this.tsmiLoadDriverBalance.addActionListener(this::tsmiLoadDriverBalance_Click);
        //
        // tsmiLoadSongBalance
        //
        this.tsmiLoadSongBalance.setEnabled(false);
        this.tsmiLoadSongBalance.setName("tsmiLoadSongBalance");
        this.tsmiLoadSongBalance.setPreferredSize(new Dimension(223, 22));
        this.tsmiLoadSongBalance.setText("Load Song Mixer - Balance");
        this.tsmiLoadSongBalance.addActionListener(this::tsmiLoadSongBalance_Click);
        //
        // toolStripSeparator1
        //
        this.toolStripSeparator1.setName("toolStripSeparator1");
        this.toolStripSeparator1.setPreferredSize(new Dimension(220, 6));
        //
        // tsmiSaveDriverBalance
        //
        this.tsmiSaveDriverBalance.setName("tsmiSaveDriverBalance");
        this.tsmiSaveDriverBalance.setPreferredSize(new Dimension(223, 22));
        this.tsmiSaveDriverBalance.setText("Save Driver Mixer - Balance");
        this.tsmiSaveDriverBalance.addActionListener(this::tsmiSaveDriverBalance_Click);
        //
        // tsmiSaveSongBalance
        //
        this.tsmiSaveSongBalance.setName("tsmiSaveSongBalance");
        this.tsmiSaveSongBalance.setPreferredSize(new Dimension(223, 22));
        this.tsmiSaveSongBalance.setText("Save Song Mixer - Balance");
        this.tsmiSaveSongBalance.addActionListener(this::tsmiSaveSongBalance_Click);
        //
        // frmMixer2
        //
        this.setPreferredSize(new Dimension(320, 288));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmMixer2");
        this.setTitle("Mixer");
        this.addWindowListener(this.windowListener);
        this.addKeyListener(this.frmMixer2_KeyDown);
        this.addMouseListener(this.frmMixer2_MouseClick); // TODO
        this.addMouseWheelListener(this.pbScreen_MouseWheel); // TODO
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;
    private ScreenPanel pbScreen;
    private JPopupMenu ctxtMenu;
    private JMenuItem tsmiSaveDriverBalance;
    private JMenuItem tsmiSaveSongBalance;
    private JMenuItem tsmiLoadDriverBalance;
    private JMenuItem tsmiLoadSongBalance;
    private JSeparator toolStripSeparator1;

    /**
     * fader slot -> (volume tag, chip), slot 0 is the master fader; filled from what each
     * provider says about its place on the mixer skin
     */
    @SuppressWarnings("unchecked")
    private final Tuple<String, Class<? extends Chip>>[] setVolume = new Tuple[GIMIC_OPN];

    {
        for (ViewProvider p : ViewProvider.providers()) {
            for (ViewProvider.MixerSlot s : p.mixerSlots()) {
                setVolume[s.slot()] = new Tuple<>(s.tag(), s.chip());
            }
        }
    }

//#region draw buffer

    private static void screenInitMixer(FrameBuffer screen) {
    }

    private static void drawFader(FrameBuffer screen, int x, int y, int t, int od, int nd) {
        if (od == nd)
            return;

        drawFaderSlitP(screen, x, y - 8);
        drawFont4IntM(screen, x, y + 48, 3, nd);

        int n;

        if (nd >= 0) {
            n = -(int) (nd / 20.0 * 8.0);
        } else {
            n = -(int) (nd / 192.0 * 35.0);
        }

        y += n;

        drawFaderP(screen, x, y, t);

        od = nd;
    }

    private static void drawGFader(FrameBuffer screen, int x, int y, int t, int od, int nd) {
        if (od == nd)
            return;

        drawFaderSlitP(screen, x, y - 8);
        drawFont4IntM(screen, x, y + 48, 3, nd);

        int n = 35 - (int) (nd / 127.0 * 43.0);
        y += n;

        drawFaderP(screen, x, y, t);

        od = nd;
    }

    private static void MixerVolume(FrameBuffer screen, int x, int y, int od, int nd, int ov, int nv) {
        if (od == nd && ov == nv)
            return;

        for (int i = 0; i < 44; i++) {
            int t = i < 8 ? 0 : 1;
            if (i % 2 != 0)
                t = 2;
            else if (44 - i > nd)
                t = 2;

            drawMixerVolumeP(screen, x, y + i, t);
        }

        drawMixerVolumeP(screen, x, y + (44 - nv), nv > 36 ? 0 : 1);

        od = nd;
        ov = nv;
    }

    private static void drawFont4IntM(FrameBuffer screen, int x, int y, int k, int num) {
        if (screen == null)
            return;

        int t = 0;
        int n;

        if (num < 0) {
            num = -num;
            screen.drawByteArray(x - 4, y, FrameBuffer.rFont2[t], 128, 52, 1, 4, 7);
        } else {
            if (num != 0)
                t = 1;
            screen.drawByteArray(x - 4, y, FrameBuffer.rFont2[t], 128, 24, 1, 4, 7);
        }

        if (k == 3) {
            boolean f = false;
            n = num / 100;
            num -= n * 100;
            n = (n > 9) ? 0 : n;
            if (n != 0) {
                screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, n * 4 + 64, 1, 4, 7);
                if (n != 0) {
                    f = true;
                }
            } else {
                screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, 0, 1, 4, 7);
            }

            n = num / 10;
            num -= n * 10;
            x += 4;
            if (n != 0 || f) {
                screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, n * 4 + 64, 1, 4, 7);
                if (n != 0) {
                    f = true;
                }
            } else {
                screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, 0, 1, 4, 7);
            }

            n = num / 1;
            x += 4;
            screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, n * 4 + 64, 1, 4, 7);
            return;
        }

        n = num / 10;
        num -= n * 10;
        n = (n > 9) ? 0 : n;
        if (n != 0) {
            screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, n * 4 + 64, 1, 4, 7);
        } else {
            screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, 0, 1, 4, 7);
        }

        n = num / 1;
        x += 4;
        screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, n * 4 + 64, 1, 4, 7);
    }

    private static void drawFaderSlitP(FrameBuffer screen, int x, int y) {
        screen.drawByteArray(x, y, FrameBuffer.rFader, 32, 16, 0, 8, 8);
        screen.drawByteArray(x, y + 8, FrameBuffer.rFader, 32, 16, 8, 8, 8);
        screen.drawByteArray(x, y + 16, FrameBuffer.rFader, 32, 16, 8, 8, 8);
        screen.drawByteArray(x, y + 24, FrameBuffer.rFader, 32, 16, 8, 8, 8);
        screen.drawByteArray(x, y + 32, FrameBuffer.rFader, 32, 16, 8, 8, 8);
        screen.drawByteArray(x, y + 40, FrameBuffer.rFader, 32, 16, 8, 8, 8);
        screen.drawByteArray(x, y + 48, FrameBuffer.rFader, 32, 24, 0, 8, 8);
    }

    private static void drawFaderP(FrameBuffer screen, int x, int y, int t) {
        screen.drawByteArray(x, y, FrameBuffer.rFader, 32, t == 0 ? 0 : 8, 0, 8, 13);
    }

    private static void drawMixerVolumeP(FrameBuffer screen, int x, int y, int t) {
        screen.drawByteArray(x, y, FrameBuffer.rFader, 32, 24, 8 + t, 2, 1);
    }

//#endregion
}
