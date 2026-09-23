package mdplayer.form.sys;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import mdplayer.Audio;
import mdplayer.Chip;
import mdplayer.form.ChipLEDs;
import mdplayer.Common;
import mdplayer.Common.EnmModel;
import mdplayer.form.FrameBuffer;
import mdplayer.form.View;
import mdplayer.form.VisualizerProvider;
import mdplayer.form.kb.chip.FormRegTest;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.KeyboardHook;
import mdplayer.form.MmfControl;
import mdplayer.OpeManager;
import mdplayer.PlayList;
import mdplayer.Request;
import mdplayer.Request.enmRequest;
import mdplayer.form.ScreenPanel;
import mdplayer.Setting;
import mdplayer.TonePallet;
import mdplayer.YM2612MIDI;
import mdplayer.ChipRegister;
import mdplayer.chips.RealChipPlugin;
import mdplayer.chips.VstPlugin;
import mdplayer.vst.FormVSTeffectList;
import mdplayer.driver.BaseDriver;
import mdplayer.form.Layouts;
import mdplayer.driver.FileFormat;
import mdplayer.driver.sampled.M3UFileFormat;
import mdplayer.driver.archive.ZIPFileFormat;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.vgm.VGMPlugin;
import mdsound.Instrument;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;
import vavi.util.archive.Archives;
import vavi.util.compat.Tuple;
import vavi.util.compat.Tuple4;
import vavi.util.event.GenericEvent;

import static java.lang.System.getLogger;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;


public class FormMain extends JFrame {

    private static final Logger logger = getLogger(FormMain.class.getName());

    private static final ResourceBundle rb = ResourceBundle.getBundle("mdplayer/form/sys/frmMain");
    private static final ResourceBundle rb2 = ResourceBundle.getBundle("mdplayer/properties/resources");

    private final ChipLEDs chipLED = new ChipLEDs();
    private final ChipLEDs chipLED_old = new ChipLEDs();

    private final YM2612MIDI ym2612Midi = new YM2612MIDI(null); // TODO

    /** the state the MIDI keyboard panel plays into; its provider needs it to build the panel */
    public YM2612MIDI.Params ym2612MidiParams() {
        return ym2612Midi.ym2612Midi;
    }

    public static final Point empty = new Point(0, 0);

    private BufferedImage pbRf5c164Screen;
    private FrameBuffer mainScreen = new FrameBuffer();
    private int pWidth = 0;
    private int pHeight = 0;

    private FormInfo frmInfo = null;
    private FormPlayList frmPlayList = null;
    private boolean faderMasterHover = false;
    private boolean faderMasterDrag = false;
    private int faderMasterVal = 0;
    private boolean faderTimeLineHover = false;
    private boolean faderTimeLineDrag = false;
    private int faderTimeLineVal = 0;
    private int visVolumeMaster = 0;

    private static final int[] masterVolTbl = {
            -192, -188, -184, -180, -176, -172, -169, -165, -161, -158,
            -154, -150, -146, -142, -139, -135, -131, -128, -123, -120,
            -116, -112, -108, -104, -101, -97, -93, -89, -86, -82,
            -78, -74, -70, -66, -62, -58, -54, -50, -46, -42,
            -38, -34, -30, -26, -22, -18, -15, -11, -7, -3,
            0, 2, 5, 9, 13, 17, 20
    };
//    private frmVSTeffectList frmVSTeffectList = null;

    private FormMixer2 frmMixer2 = null;
    private FormVSTeffectList frmVSTeffectList;

    /** every chip/panel view the providers contribute, in provider order, indexed primary/secondary */
    private final Map<ViewProvider, View[]> views = new LinkedHashMap<>();

    {
        // filled here, not in the constructor: initializeComponent() builds the panel menus from it
        for (ViewProvider p : ViewProvider.providers()) {
            views.put(p, new View[p.instances()]);
        }
    }

    /** the main window's per-frame draw state, diffed new against old to decide what to redraw */
    public static class ScreenParams {

        int Cminutes = -1;
        int Csecond = -1;
        int Cmillisecond = -1;

        int TCminutes = -1;
        int TCsecond = -1;
        int TCmillisecond = -1;

        int LCminutes = -1;
        int LCsecond = -1;
        int LCmillisecond = -1;

        int Master = -255;
        int MasterVis = -255;
        int MasterHover = -255;
        int MasterDrag = -255;
        int TimeLine = -255;
        int TimeLineVis = -255;
        int TimeLineHover = -255;
        int TimeLineDrag = -255;
    }

    public ScreenParams oldParam = new ScreenParams();
    private final ScreenParams newParam = new ScreenParams();

    private final int[] oldButton = new int[18];
    private final int[] newButton = new int[18];
    private final int[] oldButtonMode = new int[18];
    private final int[] newButtonMode = new int[18];

    private boolean isRunning = false;
    private boolean stopped = false;

    private boolean isInitialOpenFolder = true;

    public Setting setting = Setting.load();

    /** the designer's captions, converted from frmMain.resx */
    private static final ResourceBundle resources = ResourceBundle.getBundle("mdplayer/form/sys/frmMain", Locale.getDefault());
    public final TonePallet tonePallet = TonePallet.load(null);

    private int frameSizeW = 0;
    private int frameSizeH = 0;

    private Transmitter midiin = null;
    private static final boolean forcedExit = false;
    private YM2612MIDI ym2612MIDI;
    private boolean flgReinit = false;
    private boolean reqAllScreenInit = true;

    private static final String[] modeTip = {
            "Mode\nNow:Step\nNext:Random",
            "Mode\nNow:Random\nNext:Loop",
            "Mode\nNow:Loop\nNext:LoopOne",
            "Mode\nNow:LoopOne\nNext:Step",
    };

    private static final String[] zoomTip = {
            "Zoom\nNow:x1\nNext:x2",
            "Zoom\nNow:x2\nNext:x3",
            "Zoom\nNow:x3\nNext:x4",
            "Zoom\nNow:x4\nNext:x1",
    };

    //private FileSystemWatcher watcher = null;
    private MmfControl mmf = null;
    private long now = 0;
    private String opeFolder = "";
    private final Object remoteLockObj = new Object();
    private boolean remoteBusy = false;
    private final List<String[]> remoteReq = new ArrayList<>();

    public FormMain() {
        logger.log(Level.INFO, "Startup process begins");
        logger.log(Level.INFO, "frmMain<init>:STEP 00");

        initializeComponent();
        FrameBuffer.init();

        logger.log(Level.INFO, "frmMain<init>:STEP 01");

        // Only if arguments are specified, does a process check, and if the same application as itself is running,
        // passes the arguments to it and terminates it.
//        if (Common.getCommandLineArgs().length > 1) {
//            Process prc = GetPreviousProcess();
//            if (prc != null) {
//                sendString(prc.MainWindowHandle, Environment.GetCommandLineArgs()[1]);
//                forcedExit = true;
//                try {
//                    this.setVisible(false);
//                } catch (Exception ignored) {
//                }
//                return;
//            }
//        }

        logger.log(Level.INFO, "frmMain<init>:STEP 02");

//        pbScreen.AllowDrop = true;

        logger.log(Level.INFO, "frmMain<init>:STEP 03");
        if (setting == null) {
            logger.log(Level.ERROR, "frmMain<init>:setting instanceof null");
        } else {
//            if ((Control.ModifierKeys & Keys.Shift) == Keys.Shift) {
//                int res = JOptionPane.showConfirmDialog(this,
//                        "Do you want to initialize window position information?",
//                        "MDPlayer",
//                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
//                if (res == JOptionPane.OK_OPTION) {
//                    ClearWindowPos();
//                }
//            }
        }

        logger.log(Level.INFO, "Audio initialization process begins at startup");

//        ym2612MIDI = new mdplayer.YM2612MIDI(audio.plugin.chipRegister.plugin(MidiPlugin.class).mds, newParam);
//        ym2612MIDI.fadeout = this::fadeout;
//        ym2612MIDI.next = this::next;
//        ym2612MIDI.ff = this::ff;
//        ym2612MIDI.pause = this::pause;
//        ym2612MIDI.play = this::play;
//        ym2612MIDI.prev = this::prev;
//        ym2612MIDI.slow = this::slow;
//        ym2612MIDI.stop = this::stop;

        logger.log(Level.INFO, "Audio initialization process completed at startup");

        // the chips and the drivers tell the view what they are doing; this is the view listening
        audio.addGenericListener(this::viewEventHappened);

        startMIDIInMonitoring();

        logger.log(Level.INFO, "frmMain<init>:STEP 04");

        setVisible(true);

        // Swing fires windowActivated before windowOpened, and again on every focus gain, so the
        // load/shown sequence is driven from here instead: shown starts the render loop, and it
        // must not run before load has created the child windows it needs.
        frmMain_Load(null);
        frmMain_Shown(null);
    }

    private void clearWindowPos() {
        setting.setLocation(new Setting.Location());
    }

    /**
     * Is this chip one of the ones the song being played is made of? A chip has a panel whatever
     * the song, but only the chips the song actually drives have an instrument behind them.
     */
    private boolean songUses(Class<? extends Chip> chip, int chipId) {
        Chip c = audio.plugin.chipRegister.chip(chip);
        if (c == null) return false;

        for (Class<? extends Instrument> instrument : c.implementations()) {
            if (audio.plugin.mds.inst(instrument, chipId) != null) return true;
        }
        return false;
    }

    /** Shows a panel for each chip the song uses, and hides the rest. */
    private void autoOpenPanels() {
        for (Map.Entry<ViewProvider, View[]> e : views.entrySet()) {
            ViewProvider p = e.getKey();
            if (p.chip() == null) continue;
            for (int chipId = 0; chipId < e.getValue().length; chipId++) {
                if (songUses(p.chip(), chipId)) {
                    openView(p, chipId, true);
                } else {
                    closeView(p, chipId);
                }
            }
        }
    }

    /**
     * Opens a provider's view, restoring its saved position; called again on an open view it
     * closes it instead (the menu items toggle), unless {@code force} keeps it open.
     */
    private void openView(ViewProvider p, int chipId, boolean force) {
        View[] slot = views.get(p);
        if (slot[chipId] != null) {
            if (!force) closeView(p, chipId);
            return;
        }

        View v = p.create(this, chipId, setting.getOther().getZoom());
        if (v == null) return;

        Point pos = setting.getLocation().pos(p.id(), chipId);
        if (pos == null) {
            Point off = p.defaultOffset();
            v.setDefaultLocation(this.getLocation().x + off.x, this.getLocation().y + off.y);
        } else {
            v.setDefaultLocation(pos.x, pos.y);
        }

        slot[chipId] = v;
        v.frame().setVisible(true);
        v.update();
        v.frame().setTitle(p.title(chipId));

        checkAndSetForm(v.frame());
    }

    private void closeView(ViewProvider p, int chipId) {
        View[] slot = views.get(p);
        if (slot[chipId] == null) return;

        try {
            slot[chipId].frame().setVisible(false);
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
        try {
            slot[chipId].frame().dispose();
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
        slot[chipId] = null;
    }

    /**
     * Opens the register-dump window on the given chip's page, or flips the already open window
     * to it.
     */
    private void openRegTest(Class<? extends Chip> selectedChip) {
        ViewProvider p = ViewProvider.byId("RegTest");
        FormRegTest frm = (FormRegTest) views.get(p)[0];
        if (frm != null && !frm.isClosed) {
            frm.changeChip(selectedChip);
            return;
        }
        if (frm != null) closeView(p, 0);

        openView(p, 0, false);
        frm = (FormRegTest) views.get(p)[0];
        if (frm != null) frm.changeChip(selectedChip);
    }

    /**
     * What the chips and the drivers have to say, as they play.
     * <p>
     * This runs on the audio thread, so it only records what happened — the screen loop is what
     * draws it.
     *
     * @see mdplayer.driver.BaseDriver#fireEventHappened
     */
    private void viewEventHappened(GenericEvent ev) {
        Object[] args = ev.getArguments();

        switch (ev.getName()) {
            case "led.on", "led.set" -> {
                int chipId = args != null && args.length > 0 && args[0] instanceof Number n ? n.intValue() : 0;
                chipLED.on(ev.getSource(), chipId);
            }
            case "led.reset" -> chipLED.clear();
            case "wave.buffer" -> {
                if (args == null || args.length < 2
                        || !(args[0] instanceof Number left) || !(args[1] instanceof Number right)) {
                    return;
                }

                visVolumeMaster = Math.max(Math.abs(left.intValue()), Math.abs(right.intValue()));

                for (VisualizerProvider p : VisualizerProvider.providers()) {
                    p.push(left.shortValue(), right.shortValue());
                }
                if (frmMixer2 != null && !frmMixer2.isClosed) {
                    // the mixer's master meter is the loudest of what just came out
                    frmMixer2.visVolume.put("master", visVolumeMaster);
                }
            }
            default -> {
            }
        }
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            Runtime.getRuntime().addShutdownHook(new Thread(FormMain.this::SystemEvents_SessionEnding));
        }

        @Override
        public void windowClosing(WindowEvent e) {
            frmMain_FormClosing(e);
        }
    };

    private void frmMain_Load(WindowEvent ev) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::SystemEvents_SessionEnding));

        logger.log(Level.INFO, "frmMain_Load:STEP 05");

        String testX = System.getProperty("mdplayer.test.x");
        String testY = System.getProperty("mdplayer.test.y");
        if (testX != null && testY != null) {
            this.setLocation(Integer.parseInt(testX), Integer.parseInt(testY));
        } else if (!setting.getLocation().getPMain().equals(empty)) {
            this.setLocation(setting.getLocation().getPMain());
        }

        // Creating a DoubleBuffer Object

        pbRf5c164Screen = new BufferedImage(320, 72, BufferedImage.TYPE_INT_ARGB);

        logger.log(Level.INFO, "frmMain_Load:STEP 06");

        mainScreen.add(pbScreen, Common.getImage("planeMain"), null, 1);
        reqAllScreenInit = true;

        logger.log(Level.INFO, "frmMain_Load:STEP 07");

        pWidth = pbScreen.getWidth();
        pHeight = pbScreen.getHeight();

        frmPlayList = new FormPlayList(this);
        frmPlayList.setVisible(true);
        frmPlayList.setVisible(false);
        frmPlayList.setOpacity(1.0f);
        //frmPlayList.setLocation(new Point(this.getLocation().x + 328, this.getLocation().y + 264));
        frmPlayList.refresh();

//        frmVSTeffectList = new frmVSTeffectList(this, setting);
//        frmVSTeffectList.setVisible(true);
//        frmVSTeffectList.setVisible(false);
//        frmVSTeffectList.setOpacity(1.0f);
//        //frmVSTeffectList.setLocation(new Point(this.getLocation().x + 328, this.getLocation().y + 264));
//        frmVSTeffectList.repaint();

        if (setting.getLocation().getOPlayList()) dispPlayList();
        if (setting.getLocation().getOInfo()) openInfo();
        if (setting.getLocation().getOMixer()) openMixer();
        for (VisualizerProvider p : VisualizerProvider.providers()) p.restore(this);
        if (setting.getLocation().getOpenVSTeffectList()) openVSTeffectList();

        for (Map.Entry<ViewProvider, View[]> e : views.entrySet()) {
            for (int i = 0; i < e.getValue().length; i++) {
                if (setting.getLocation().isOpen(e.getKey().id(), i)) openView(e.getKey(), i, false);
            }
        }

        logger.log(Level.INFO, "frmMain_Load:STEP 08");

        frameSizeW = this.getWidth() - this.getSize().width;
        frameSizeH = this.getHeight() - this.getSize().height;

        changeZoom();
        opeButtonMode.setToolTipText(modeTip[newButtonMode[9]]);
        lstOpeButtonControl = new JButton[] {
                opeButtonSetting,
                opeButtonStop,
                opeButtonPause,
                opeButtonFadeout,
                opeButtonPrevious,
                opeButtonSlow,
                opeButtonPlay,
                opeButtonFast,
                opeButtonNext,
                opeButtonMode,
                opeButtonOpen,
                opeButtonPlayList,
                opeButtonInformation,
                opeButtonMixer,
                opeButtonKBD,
//                opeButtonVST,
                opeButtonMIDIKBD,
                opeButtonZoom,
                opeButtonMode,
                opeButtonMode,
                opeButtonMode
        };

        logger.log(Level.INFO, "frmMain_Load:STEP 09");

        // Operation: Clear folder
        //opeFolder = mdplayer.Common.GetOperationFolder(true);
        //startWatch(opeFolder);
        mmf = new MmfControl(false, "MDPlayer", 1024 * 4);
    }

//    private void startWatch(String opeFolder) {
//        if (watcher != null) return;
//
//        watcher = new FileSystemWatcher();
//        watcher.Path = Path.GetDirectoryName(opeFolder);
//        watcher.NotifyFilter = (
//            NotifyFilters.LastAccess
//            | NotifyFilters.LastWrite
//            | NotifyFilters.FileName
//            | NotifyFilters.DirectoryName
//            | NotifyFilters.CreationTime
//            | NotifyFilters.Attributes
//            );
//        watcher.Filter = ""; //  Path.getFileName(opeFolder);
//        watcher.SynchronizingObject = this;
//
//        watcher.Changed += new FileSystemEventHandler(watcher_Changed);
//        watcher.Created += new FileSystemEventHandler(watcher_Changed);
//
//        watcher.EnableRaisingEvents = true;
//    }
//
//    private void stopWatch() {
//        watcher.EnableRaisingEvents = false;
//        watcher.dispose();
//        watcher = null;
//    }

    private void watcher_Changed(WatchEvent<?> e) {
        String trgFile = Path.of(opeFolder, "ope.txt").toString();

        synchronized (remoteLockObj) {
            if (remoteBusy) {
                try {
                    Files.delete(Path.of(trgFile));
                } catch (Exception deleteEx) {
                    logger.log(Level.ERROR, deleteEx.getMessage(), deleteEx);
                }
                return;
            }
            remoteBusy = true;
        }

        try {
            WatchEvent.Kind<?> kind = e.kind();
            if (kind == ENTRY_MODIFY || kind == ENTRY_CREATE) {

                long n = Instant.now().toEpochMilli() / 1_000_000L;
                if (now == n) {
                    try {
                        Files.delete(Path.of(trgFile));
                    } catch (Exception deleteEx) {
                        logger.log(Level.ERROR, deleteEx.getMessage(), deleteEx);
                    }
                    return;
                }
                now = n;

                if (!Files.exists(Path.of(trgFile))) return;
                List<String> lins = null;
                int retry = 30;
                while (retry > 0) {
                    try {
                        lins = Files.readAllLines(Paths.get(trgFile));
                        retry = 0;
                    } catch (IOException e1) {
                        logger.log(Level.WARNING, e);
                        Thread.sleep(100);
                        retry--;
                    }
                }

                try {
                    Files.delete(Path.of(trgFile));
                } catch (Exception deleteEx) {
                    logger.log(Level.ERROR, deleteEx.getMessage(), deleteEx);
                }

                remoteReq.add(lins.toArray(String[]::new));
            }
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        } finally {
            synchronized (remoteLockObj) {
                remoteBusy = false;
            }
        }
    }

    private void remote(String line) {
        try {
            int n = Math.min(
                    line.indexOf(' ') == -1 ? Integer.MAX_VALUE : line.indexOf(' '),
                    line.indexOf('\t') == -1 ? Integer.MAX_VALUE : line.indexOf('\t')
            );
            String command = line;
            String optionLine = "";
            if (n != Integer.MAX_VALUE) {
                command = line.substring(0, n + 1).toUpperCase().trim();
                optionLine = line.substring(n).trim();
            }

            switch (command) {
                case "PLAY":
                    if (!optionLine.isEmpty()) {
                        if (optionLine.charAt(0) == '\"' && optionLine.charAt(optionLine.length() - 1) == '\"') {
                            optionLine = optionLine.substring(1, optionLine.length() - 2);
                        }
                        addFileAndPlay(new String[] {optionLine});
                    } else
                        tsmiPlay_Click(null);
                    break;
                case "STOP":
                    tsmiStop_Click(null);
                    break;
                case "NEXT":
                    next();
                    oldParam = new ScreenParams();
                    break;
                case "PREV":
                    opeButtonPrevious_Click(null);
                    break;
                case "FADEOUT":
                    tsmiFadeOut_Click(null);
                    break;
                case "FAST":
                    ff();
                    break;
                case "SLOW":
                    slow();
                    break;
                case "PAUSE":
                    pause();
                    break;
                case "CLOSE":
                    setVisible(false);
                    break;
                case "LOOP":
                    playMode();
                    break;
                case "MIXER":
                    openMixer();
                    break;
                case "INFO":
                    openInfo();
                    break;
                case "SPLAY":

                    String lin = optionLine.trim();
                    String mName = lin.substring(0, lin.indexOf(" "));
                    lin = lin.substring(lin.indexOf(" ")).trim();
                    int count = Integer.parseInt(lin.substring(0, lin.indexOf(" ")));
                    lin = lin.substring(lin.indexOf(" ")).trim();
                    String path = lin.trim();
                    MmfControl mml2vgmMmf = new MmfControl(true, mName, count);
                    byte[] buf = mml2vgmMmf.getBytes();

                    bufferPlay(buf, path);

                    break;
            }
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }

    private void SystemEvents_SessionEnding() {
        this.setVisible(false);
    }

    private void changeZoom() {
        opeButtonZoom.setToolTipText(zoomTip[setting.getOther().getZoom() - 1]);

        int zoom = setting.getOther().getZoom();
        int w = Common.getImage("planeMain").getWidth() * zoom;
        int h = Common.getImage("planeMain").getHeight() * zoom;

        // the skin fills the client area; size the content pane to it and let pack() add the window
        // chrome. (The old code added frameSizeW/H, the WinForms "Width - ClientSize" chrome, but in
        // Swing getWidth() == getSize().width so that was always zero and the skin got clipped by the
        // title bar.)
        pbScreen.setBounds(0, 0, w, h);
        getContentPane().setPreferredSize(new Dimension(w, h));

        componentListener.componentResized(null);
        relocateOpeButton(zoom);
        pack();

        for (Map.Entry<ViewProvider, View[]> e : views.entrySet()) {
            View[] slot = e.getValue();
            for (int i = 0; i < slot.length; i++) {
                if (slot[i] != null && !slot[i].isClosed()) {
                    closeView(e.getKey(), i);
                    openView(e.getKey(), i, false);
                }
            }
        }

        if (frmPlayList != null) {
            frmPlayList.setZoom(zoom);
        }

        if (frmMixer2 != null && !frmMixer2.isClosed) {
            openMixer();
            openMixer();
        }

    }

    private void frmMain_Shown(WindowEvent ev) {
        logger.log(Level.INFO, "frmMain_Shown:STEP 09");

        Thread trd = new Thread(this::screenMainLoop);
        trd.setPriority(Thread.MIN_PRIORITY);
        trd.start();

        new Thread(() -> {
            String[] args = Common.getCommandLineArgs();

            if (args.length < 1 || args[0].isBlank()) {
                return;
            }

            logger.log(Level.INFO, "frmMain_Shown:STEP 10");

            String fileName = args[0];

            try {
                // Do the slow file check / metadata reading in the background thread first
                FileFormat format = FileFormat.getFileFormat(fileName);
                format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(Path.of(fileName)))), null);

                // Now update the UI and playlist on the EDT (block background thread while EDT updates)
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        frmPlayList.stop();

                        PlayList pl = frmPlayList.getPlayList();
                        if (pl.getMusics().isEmpty() || !Objects.equals(pl.getMusics().getLast().fileName, fileName)) {
                            pl.addFile(fileName);
                        }
                    } catch (Exception ex) {
                        logger.log(Level.ERROR, ex.getMessage(), ex);
                    }
                });

                // Call loadAndPlay in the background thread (takes 2 seconds, but does NOT block EDT)
                if (!loadAndPlay(0, 0, fileName, "")) {
                    SwingUtilities.invokeLater(() -> {
                        frmPlayList.stop();
                        OpeManager.requestToAudio(new Request(enmRequest.Stop, null, null));
                    });
                    return;
                }

                // Finalize play state on the EDT
                SwingUtilities.invokeLater(() -> {
                    frmPlayList.setStart(-1);
                    oldParam = new ScreenParams();
                    frmPlayList.play();
                });

            } catch (Exception ex) {
                logger.log(Level.ERROR, ex.getMessage(), ex);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(FormMain.this, "Failed to read file."));
            }

            logger.log(Level.INFO, "frmMain_Shown:STEP 11");
            logger.log(Level.INFO, "Startup process complete");
        }, "mdplayer-startup-loader").start();
    }

    private final ComponentListener componentListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            // Reallocate when resizing
//            if (screen != null) screen.setVisible(false);

            mainScreen.add(pbScreen, Common.getImage("planeMain"), null, setting.getOther().getZoom());
            reqAllScreenInit = true;
            //screen.screenInitAll();
        }
    };

    private void frmMain_FormClosing(WindowEvent e) {
        if (forcedExit) return;

        logger.log(Level.ERROR, "Termination process begins");
        logger.log(Level.ERROR, "frmMain_FormClosing:STEP 00");

        frmPlayList.stop();
        frmPlayList.save();

        tonePallet.save(null);

        logger.log(Level.ERROR, "frmMain_FormClosing:STEP 01");

        StopMIDIInMonitoring();
        Request req = new Request(enmRequest.Die, null, null);
        OpeManager.requestToAudio(req);
        while (!req.getEnd()) {  // No callbacks for suicide requests
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }

        logger.log(Level.ERROR, "frmMain_FormClosing:STEP 02");

        isRunning = false;
        while (!stopped) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
//            Application.DoEvents();
        }

        logger.log(Level.ERROR, "frmMain_FormClosing:STEP 03");

        if (ym2612MIDI != null)
            ym2612MIDI.close();

        // release
        closeScreen();

        setting.getLocation().setOInfo(false);
        setting.getLocation().setOPlayList(false);
        setting.getLocation().setOMixer(false);
        setting.getLocation().clearOpen();

        logger.log(Level.ERROR, "frmMain_FormClosing:STEP 04");

        if (e.getNewState() == WindowEvent.WINDOW_OPENED) {
            setting.getLocation().setPMain(getLocation());
        } else {
            setting.getLocation().setPMain(getBounds().getLocation());
        }
        if (frmPlayList != null && !frmPlayList.isClosed) {
            setting.getLocation().setPPlayList(frmPlayList.getLocation());
            setting.getLocation().setPPlayListWH(new Dimension(frmPlayList.getWidth(), frmPlayList.getHeight()));
            frmPlayList.setVisible(false);
            setting.getLocation().setOPlayList(true);
        } else {
            setting.getLocation().setOPlayList(false);
        }
        if (frmInfo != null && !frmInfo.isClosed) {
            setting.getLocation().setPInfo(frmInfo.getLocation());
            frmInfo.setVisible(false);
            setting.getLocation().setOInfo(true);
        }
        if (frmMixer2 != null && !frmMixer2.isClosed) {
            setting.getLocation().setPosMixer(frmMixer2.getLocation());
            frmMixer2.setVisible(false);
            setting.getLocation().setOMixer(true);
        }

        for (Map.Entry<ViewProvider, View[]> entry : views.entrySet()) {
            View[] slot = entry.getValue();
            for (int i = 0; i < slot.length; i++) {
                if (slot[i] != null && !slot[i].isClosed()) {
                    setting.getLocation().setPos(entry.getKey().id(), i, slot[i].frame().getLocation());
                    slot[i].frame().setVisible(false);
                    setting.getLocation().setOpen(entry.getKey().id(), i, true);
                }
            }
        }

        for (VisualizerProvider p : VisualizerProvider.providers()) p.close();

        setting.getLocation().setOpenVSTeffectList(frmVSTeffectList != null && !frmVSTeffectList.isClosed);
        if (frmVSTeffectList != null && !frmVSTeffectList.isClosed) {
            setting.getLocation().setPosVSTeffectList(frmVSTeffectList.getLocation());
            frmVSTeffectList.setVisible(false);
        }

        logger.log(Level.ERROR, "frmMain_FormClosing:STEP 05");

        // the VST plug-ins outlive every song - this is the one place they are let go of, and it
        // has to be before the settings are written, since it is what puts the effect chain in them
        VstPlugin vst = ChipRegister.shared(VstPlugin.class);
        if (vst != null) vst.shutdown();

        setting.save();

        logger.log(Level.ERROR, "frmMain_FormClosing:STEP 06");

        mmf.close();

        logger.log(Level.ERROR, "Termination process complete");
    }

    private final MouseMotionListener pbScreen_MouseMove = new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent ev) {
            int px = ev.getX() / setting.getOther().getZoom();
            int py = ev.getY() / setting.getOther().getZoom();

            checkMouseHover(px, py);

            if (py < 9) {
                Arrays.fill(newButton, 0);
                return;
            }

            for (int n = 0; n < newButton.length; n++) {
                //if (px >= 320 - (16 - n) * 16 && px < 320 - (15 - n) * 16) newButton[n] = 1;
                if (px >= n * 16 + 17 && px < n * 16 + 33) newButton[n] = 1;
                else newButton[n] = 0;
            }
        }

        @Override
        public void mouseDragged(MouseEvent ev) {
            int px = ev.getX() / setting.getOther().getZoom();
            int py = ev.getY() / setting.getOther().getZoom();

            checkMouseHover(px, py);

            if (faderMasterDrag) {
                faderMasterVal = Common.range(px - 184, 0, 56);
                if (audio.plugin != null) {
                    audio.plugin.setMasterVolume(true, masterVolTbl[faderMasterVal]);
                }
            }

            if (faderTimeLineDrag) {
                faderTimeLineVal = Common.range(px - 184, 0, 56);
            }
        }
    };

    private final MouseListener pbScreen_MouseClick = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent ev) {
            showMainPopup(ev);

            int px = ev.getX() / setting.getOther().getZoom();
            int py = ev.getY() / setting.getOther().getZoom();

            checkMouseHover(px, py);

            if (faderMasterHover) {
                faderMasterDrag = true;
                faderMasterVal = Common.range(px - 184, 0, 56);
                if (audio.plugin != null) {
                    audio.plugin.setMasterVolume(true, masterVolTbl[faderMasterVal]);
                }
            }
            if (faderTimeLineHover) {
                faderTimeLineDrag = true;
                faderTimeLineVal = px - 184;
            }
        }

        @Override
        public void mouseReleased(MouseEvent ev) {
            showMainPopup(ev);

            int px = ev.getX() / setting.getOther().getZoom();
            int py = ev.getY() / setting.getOther().getZoom();

            checkMouseHover(px, py);

            if (faderTimeLineDrag) {
                faderTimeLineVal = Common.range(px - 184, 0, 56);
                audio.seek(faderTimeLineVal / 56.0);
            }

            faderMasterDrag = false;
            faderTimeLineDrag = false;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            Arrays.fill(newButton, 0);
            faderMasterHover = false;
            faderTimeLineHover = false;
        }
    };

    private void checkMouseHover(int px, int py) {
        faderMasterHover = (px >= 184 && px < 243 && py >= 13 && py < 20);
        faderTimeLineHover = (px >= 184 && px < 243 && py >= 29 && py < 36);
    }

    private void showMainPopup(MouseEvent ev) {
        if (ev.isPopupTrigger()) {
            cmsMenu.show(pbScreen, ev.getX(), ev.getY());
        }
    }

    /**
     * Builds and shows the effect chain window.
     * <p>
     * Nothing ever built this class before: the effects an old settings file named could be
     * loaded but never added to or taken away, because {@link #dispVSTList} - all the VST button
     * and menu item ever did - had no body.
     */
    private void openVSTeffectList() {
        if (frmVSTeffectList != null) {
            if (!frmVSTeffectList.isClosed) {
                frmVSTeffectList.requestFocus();
                return;
            }
            frmVSTeffectList.dispose();
        }

        frmVSTeffectList = new FormVSTeffectList(this, setting);
        Point p = setting.getLocation().getPosVSTeffectList();
        if (p == null || p.equals(empty)) {
            frmVSTeffectList.setLocation(this.getLocation().x, this.getLocation().y + 264);
        } else {
            frmVSTeffectList.setLocation(p);
        }
        frmVSTeffectList.setVisible(true);
        frmVSTeffectList.dispPluginList();
    }

    /** Shows what the player is logging. */
    private void openConsole() {
        if (frmConsole != null && !frmConsole.isClosed) {
            frmConsole.setVisible(false);
            frmConsole.dispose();
            frmConsole = null;
            return;
        }

        frmConsole = new FormConsole(this);
        frmConsole.setLocation(this.getLocation().x, this.getLocation().y + 100);
        frmConsole.setVisible(true);
    }

    private void openInfo() {
        if (frmInfo != null && !frmInfo.isClosed) {
            try {
                frmInfo.setVisible(false);
                frmInfo.dispose();
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            } finally {
                frmInfo = null;
            }
            return;
        }

        if (frmInfo != null) {
            try {
                frmInfo.setVisible(false);
                frmInfo.dispose();
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            } finally {
                frmInfo = null;
            }
        }

        frmInfo = new FormInfo(this);
        if (setting.getLocation().getPInfo().equals(empty)) {
            frmInfo.x = this.getLocation().x + 328;
            frmInfo.y = this.getLocation().y;
        } else {
            frmInfo.x = setting.getLocation().getPInfo().x;
            frmInfo.y = setting.getLocation().getPInfo().y;
        }

        Rectangle s = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        Rectangle rc = new Rectangle(frmInfo.getLocation(), frmInfo.getSize());
        if (s.contains(rc)) {
            frmInfo.setLocation(rc.getLocation());
            frmInfo.setPreferredSize(rc.getSize());
        } else {
            frmInfo.setLocation(new Point(100, 100));
        }

        frmInfo.setting = setting;
        frmInfo.setVisible(true);
        frmInfo.update();
    }

    private void openMIDIKeyboard() {
        openView(ViewProvider.byId("YM2612MIDI"), 0, false);
    }

    private void openSetting() {
        try {
            FormSetting frm = new FormSetting(setting);
            if (frm.showDialog() == JFileChooser.APPROVE_OPTION) {
                flgReinit = true;
                reinit(frm.setting);
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, "Could not open settings panel: " + e.getMessage(), e);
        }
    }

    final Audio audio = Audio.getInstance();

    private void reinit(Setting setting) {
        if (!flgReinit) return;

        StopMIDIInMonitoring();
        frmPlayList.stop();

        Request req = new Request(enmRequest.Stop, null, null);
        OpeManager.requestToAudio(req);
        while (!req.getEnd()) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }

        req = new Request(enmRequest.Die, null, null);
        OpeManager.requestToAudio(req);
        while (!req.getEnd()) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }

        //audio.Stop();
        audio.close();

        this.setting = setting;
        this.setting.save();

        frmPlayList.setting = this.setting;
        //oldParam = new ScreenParams();
        //newParam = new ScreenParams();
        reqAllScreenInit = true;
        //screen.screenInitAll();

        logger.log(Level.ERROR, "The settings have been changed, so the audio initialization process will start again.");

        if (audio.plugin != null) {
            audio.plugin.init();
        }

        logger.log(Level.ERROR, "Audio initialization process complete");

//        frmVSTeffectList.dispPluginList();
        startMIDIInMonitoring();

        isInitialOpenFolder = true;
        flgReinit = false;

//        for (int i = 0; i < 5; i++) {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            Application.DoEvents();
//        }
    }

    private void openMixer() {
        if (frmMixer2 != null && !frmMixer2.isClosed) {
            try {
                frmMixer2.setVisible(false);
                frmMixer2.dispose();
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            } finally {
                frmMixer2 = null;
            }
            return;
        }

        if (frmMixer2 != null) {
            try {
                frmMixer2.setVisible(false);
                frmMixer2.dispose();
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            } finally {
                frmMixer2 = null;
            }
        }

        frmMixer2 = new FormMixer2(this, setting.getOther().getZoom());
        if (setting.getLocation().getPosMixer().equals(empty)) {
            frmMixer2.x = this.getLocation().x + 328;
            frmMixer2.y = this.getLocation().y;
        } else {
            frmMixer2.x = setting.getLocation().getPosMixer().x;
            frmMixer2.y = setting.getLocation().getPosMixer().y;
        }

//        Screen s = Screen.FromControl(frmMixer2);
        Rectangle s = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        Rectangle rc = new Rectangle(frmMixer2.getLocation(), frmMixer2.getSize());
        if (s.contains(rc)) {
            frmMixer2.setLocation(rc.getLocation());
            frmMixer2.setPreferredSize(rc.getSize());
        } else {
            frmMixer2.setLocation(new Point(100, 100));
        }

        //frmMixer.setting = setting;
        //screen.AddMixer(frmMixer2.pbScreen, Properties.Resources.planeMixer);
        frmMixer2.setVisible(true);
        frmMixer2.update();
        //screen.screenInitMixer();
        oldParam = new ScreenParams();
    }

//    private void pbScreen_DragEnter(DragEvent e) {
//        e.Effect = DragDropEffects.All;
//    }

    private void pbScreen_DragDrop(List<File> files) {
        String filename = files.getFirst().getPath();

        try {
            // Stop the song
            frmPlayList.stop();
            this.stop();
//            while (!audio.isStopped())
//                Application.DoEvents();

            frmPlayList.getPlayList().addFile(filename);
            //frmPlayList.AddList(filename);

            if (filename.toLowerCase().lastIndexOf(".zip") == -1) {
                loadAndPlay(0, 0, filename, null);
                frmPlayList.setStart(-1);
                oldParam = new ScreenParams();

                frmPlayList.play();
            }
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            JOptionPane.showMessageDialog(null, "Failed to read file.");
        }
    }

//    @Override protected boolean getShowWithoutActivation() {
//        return true;
//    }

    private void allScreenInit() {
        //oldParam = new ScreenParams();
        drawTimer(mainScreen, 0, oldParam.Cminutes, oldParam.Csecond, oldParam.Cmillisecond, newParam.Cminutes, newParam.Csecond, newParam.Cmillisecond);
        drawTimer(mainScreen, 1, oldParam.TCminutes, oldParam.TCsecond, oldParam.TCmillisecond, newParam.TCminutes, newParam.TCsecond, newParam.TCmillisecond);
        drawTimer(mainScreen, 2, oldParam.LCminutes, oldParam.LCsecond, oldParam.LCmillisecond, newParam.LCminutes, newParam.LCsecond, newParam.LCmillisecond);
        screenInit(null);

        for (View[] slot : views.values()) {
            for (View v : slot) {
                if (v != null) v.initScreen();
            }
        }

        if (frmMixer2 != null) frmMixer2.screenInit();
        if (frmInfo != null) frmInfo.screenInit();

        reqAllScreenInit = false;
    }

    /**
     * !!This method is not running on the main thread!!
     */
    private void screenMainLoop() {
        double nextFrame = (double) System.currentTimeMillis();
        isRunning = true;
        stopped = false;

        while (isRunning) {

            if (reqAllScreenInit) {
                allScreenInit();
            }

            float period = 1000f / (float) setting.getOther().getScreenFrameRate();
            double tickCount = (double) System.currentTimeMillis();

            if (tickCount < nextFrame) {
                if (nextFrame - tickCount > 1) {
                    try {
                        Thread.sleep((long) (nextFrame - tickCount));
                    } catch (InterruptedException ignored) {
                    }
                }
                continue;
            }

            try {
                screenChangeParams();
                screenChangeParamsForms();
            } catch (Exception e) {
                logger.log(Level.ERROR, "Exception in screenChangeParams loop: " + e.getMessage(), e);
            }

            if ((double) System.currentTimeMillis() >= nextFrame + period) {
                nextFrame += period;
                continue;
            }

            // this loop is not the EDT, so hand the drawing over to it
            SwingUtilities.invokeLater(() -> {
                screenDrawParams();
                screenDrawParamsForms();
            });

            nextFrame += period;

            // Audio.play() runs on its own thread, and does not clear the stopped flag until it has
            // torn the previous song down. Until it does, a stopped plugin means "this song has not
            // begun", not "this song has ended". This is about the audio, not about the play list.
            if (songStarting && audio.isRendering()) {
                songStarting = false;

                // Only now is it known which chips the song is made of: the instruments behind them
                // are not built until play() has prepared the plugin, which happens on that thread,
                // after playData() asked for the song.
                if (setting.getOther().getAutoOpen()) {
                    SwingUtilities.invokeLater(this::autoOpenPanels);
                }
            }

            if (frmPlayList != null && frmPlayList.isPlaying()) {
                if (!songStarting) {
                    if ((setting.getOther().getUseLoopTimes() && audio.plugin.getCurLoopCounter() > setting.getOther().getLoopTimes() - 1)
                            || audio.plugin.getStopped()) {
                        fadeout();
                    }
                    if (audio.plugin.isStopped()) {
                        nextPlayMode();
                    }
                }
            }

            if (getFatalError()) {
                logger.log(Level.ERROR, "A FatalError occurred in audio. Restart audio initialization process.");

                frmPlayList.stop();
                try {
                    Request req = new Request(enmRequest.Stop, null, null);
                    OpeManager.requestToAudio(req);
                    while (!req.getEnd()) Thread.sleep(1);
                    //audio.Stop();
                } catch (Exception ex) {
                    logger.log(Level.ERROR, ex.getMessage(), ex);
                }

                try {
                    audio.close();
                } catch (Exception ex) {
                    logger.log(Level.ERROR, ex.getMessage(), ex);
                }

                setFatalError(false);
                audio.plugin.init();

                logger.log(Level.ERROR, "Audio initialization process complete");
            }
        }

        stopped = true;
    }

    private void screenChangeParams() {
        if (audio.plugin == null) return;

        long w = audio.plugin.getCounter();
        double sec = (double) w / (double) Common.VGMProcSampleRate;
        newParam.Cminutes = (int) (sec / 60);
        sec -= newParam.Cminutes * 60;
        newParam.Csecond = (int) sec;
        sec -= newParam.Csecond;
        newParam.Cmillisecond = (int) (sec * 100.0);

        w = audio.plugin.getTotalCounter();
        sec = (double) w / (double) Common.VGMProcSampleRate;
        newParam.TCminutes = (int) (sec / 60);
        sec -= newParam.TCminutes * 60;
        newParam.TCsecond = (int) sec;
        sec -= newParam.TCsecond;
        newParam.TCmillisecond = (int) (sec * 100.0);

        w = audio.plugin.getLoopCounter();
        sec = (double) w / (double) Common.VGMProcSampleRate;
        newParam.LCminutes = (int) (sec / 60);
        sec -= newParam.LCminutes * 60;
        newParam.LCsecond = (int) sec;
        sec -= newParam.LCsecond;
        newParam.LCmillisecond = (int) (sec * 100.0);

        // Fader (Master Volume)
        int val;
        if (faderMasterDrag) {
            newParam.Master = Common.range(faderMasterVal, 0, 56);
        } else {
            val = Common.range(setting.getBalance().getMasterVolume(), -192, 20) + 192;
            val = (int) (val * ((7.0 * 8) / (20.0 - (-192))));
            newParam.Master = val;
        }

        val = Common.range(visVolumeMaster / 220, 0, 56);
        if (newParam.MasterVis > 0) newParam.MasterVis--;
        newParam.MasterVis = Math.max(newParam.MasterVis, val);

        newParam.MasterHover = faderMasterHover ? 0 : 1;
        newParam.MasterDrag = faderMasterDrag ? 0 : 1;

        // Fader (Timeline)
        double gc = (double) audio.plugin.getCounter();
        double tc = (double) audio.plugin.getTotalCounter();
        if (tc > 0.0) {
            newParam.TimeLineVis = (int) (57.0 * gc / tc) % 57;
        } else {
            newParam.TimeLineVis = 0;
        }

        if (faderTimeLineDrag) {
            newParam.TimeLine = faderTimeLineVal;
        } else {
            newParam.TimeLine = newParam.TimeLineVis;
        }

        newParam.TimeLineHover = faderTimeLineHover ? 0 : 1;
        newParam.TimeLineDrag = faderTimeLineDrag ? 0 : 1;

        updateOpeButtonActiveState();
    }

    private void screenChangeParamsForms() {
        if (audio == null || audio.plugin == null) return;

        for (View[] slot : views.values()) {
            for (int i = 0; i < slot.length; i++) {
                if (slot[i] != null && !slot[i].isClosed()) slot[i].changeScreenParams();
                else slot[i] = null;
            }
        }

        if (frmMixer2 != null && !frmMixer2.isClosed) frmMixer2.screenChangeParams();
        else frmMixer2 = null;
    }

    private void screenDrawParams() {
        if (mainScreen == null) return;
        // drawing

        for (int i = 0; i < lstOpeButtonActive.length; i++) {
            if (lstOpeButtonActive[i] != lstOpeButtonActiveOld[i]) {
                lstOpeButtonActiveOld[i] = lstOpeButtonActive[i];
                redrawButton(lstOpeButtonControl[i], setting.getOther().getZoom(),
                        lstOpeButtonActive[i] ? lstOpeButtonActiveImage[i] : lstOpeButtonLeaveImage[i]
                );
            }
        }

        drawTimer(mainScreen, 0, oldParam.Cminutes, oldParam.Csecond, oldParam.Cmillisecond, newParam.Cminutes, newParam.Csecond, newParam.Cmillisecond);
        drawTimer(mainScreen, 1, oldParam.TCminutes, oldParam.TCsecond, oldParam.TCmillisecond, newParam.TCminutes, newParam.TCsecond, newParam.TCmillisecond);
        drawTimer(mainScreen, 2, oldParam.LCminutes, oldParam.LCsecond, oldParam.LCmillisecond, newParam.LCminutes, newParam.LCsecond, newParam.LCmillisecond);

        // C# took these by ref, so drawTimer marked them itself; Java has to do it here or every
        // frame would redraw the digits
        oldParam.Cminutes = newParam.Cminutes;
        oldParam.Csecond = newParam.Csecond;
        oldParam.Cmillisecond = newParam.Cmillisecond;
        oldParam.TCminutes = newParam.TCminutes;
        oldParam.TCsecond = newParam.TCsecond;
        oldParam.TCmillisecond = newParam.TCmillisecond;
        oldParam.LCminutes = newParam.LCminutes;
        oldParam.LCsecond = newParam.LCsecond;
        oldParam.LCmillisecond = newParam.LCmillisecond;

        // nothing is loaded yet: the skin, the buttons and the timers are all there is to show
        if (audio.plugin == null) {
            refreshScreen();
            return;
        }

        // The chip-name lamps are not drawn: this skin (planeMain) spends that row on the timers,
        // which is why the original has its drawChipName() calls commented out too. The lamps are
        // still lit and faded — they are how we know which chips a song is actually using.
        chipLED.fade();

        mainScreen.drawFont4(1, 9, 1, audio.plugin.isDataBlock(EnmModel.VirtualModel) ? "VD" : "  ");
        mainScreen.drawFont4(321 - 16, 9, 1, isPcmRAMWrite(EnmModel.VirtualModel) ? "VP" : "  ");
        mainScreen.drawFont4(1, 17, 1, audio.plugin.isDataBlock(EnmModel.RealModel) ? "RD" : "  ");
        mainScreen.drawFont4(321 - 16, 17, 1, isPcmRAMWrite(EnmModel.RealModel) ? "RP" : "  ");

        if (setting.getDebug_DispFrameCounter()) {
            long v = audio.plugin.getVirtualFrameCounter();
            if (v != -1) mainScreen.drawFont8(0, 0, 0, "EMU        : %12d ".formatted(v));
            long r = audio.plugin.getRealFrameCounter();
            if (r != -1) mainScreen.drawFont8(0, 8, 0, "REAL CHIP  : %12d ".formatted(r));
            long d = r - v;
            if (r != -1 && v != -1)
                mainScreen.drawFont8(0, 16, 0, "R.CHIP-EMU : %12d ".formatted(d));
            mainScreen.drawFont8(0, 24, 0, "PROC TIME  : %12d ".formatted(audio.plugin.procTimePer1Frame));
        }

        int[] od = {oldParam.MasterDrag};
        int[] ov = {oldParam.MasterHover};
        int[] oval1 = {oldParam.Master};
        int[] oval2 = {oldParam.MasterVis};
        drawFaderH(mainScreen, 23 * 8, 14,
                newParam.MasterDrag, newParam.MasterHover, newParam.Master, newParam.MasterVis,
                od, ov, oval1, oval2);
        oldParam.MasterDrag = od[0];
        oldParam.MasterHover = ov[0];
        oldParam.Master = oval1[0];
        oldParam.MasterVis = oval2[0];

        int[] tod = {oldParam.TimeLineDrag};
        int[] tov = {oldParam.TimeLineHover};
        int[] toval1 = {oldParam.TimeLine};
        int[] toval2 = {oldParam.TimeLineVis};
        drawFaderH(mainScreen, 23 * 8, 30,
                newParam.TimeLineDrag, newParam.TimeLineHover, newParam.TimeLine, newParam.TimeLineVis,
                tod, tov, toval1, toval2);
        oldParam.TimeLineDrag = tod[0];
        oldParam.TimeLineHover = tov[0];
        oldParam.TimeLine = toval1[0];
        oldParam.TimeLineVis = toval2[0];

        refreshScreen();

        audio.plugin.updateVol();

        String newInfo;
        MetaData metaData = (audio.plugin.driverVirtual != null) ? audio.plugin.driverVirtual.metaData : null;
        if (metaData != null) {
            String title = metaData.getFirst(Tag.Title);
            String usedChips = metaData.getFirst(Tag.Chip);
            newInfo = "MDPlayer - [%s] %s".formatted(usedChips, title);
        } else {
            newInfo = "MDPlayer";
        }

        try {
            setTitle(newInfo);
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    /** Only a vgm writes to PCM RAM, and only once it has a driver behind it. */
    private boolean isPcmRAMWrite(Common.EnmModel model) {
        return audio.plugin instanceof VGMPlugin vgmPlugin
                && vgmPlugin.driverVirtual != null
                && vgmPlugin.driverVirtual.vgm.isPcmRAMWrite;
    }

    private void screenDrawParamsForms() {
        for (View[] slot : views.values()) {
            for (int i = 0; i < slot.length; i++) {
                if (slot[i] != null && !slot[i].isClosed()) {
                    slot[i].drawScreenParams();
                    slot[i].update();
                } else slot[i] = null;
            }
        }

        if (frmMixer2 != null && !frmMixer2.isClosed) {
            frmMixer2.screenDrawParams();
            frmMixer2.update();
        } else frmMixer2 = null;
    }

    @Override
    public void setTitle(String newInfo) {
        if (!this.getTitle().equals(newInfo)) {
            super.setTitle(newInfo);
        }
    }

    private void screenInit(Object dmy) {

        // nothing is on the screen yet, so no lamp has been drawn
        chipLED_old.clear();
        chipLED.clear();

        //byte[] chips = audio.GetChipStatus();
        //mainScreen.drawChipName(14 * 4, 0 * 8, 0,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriOPN, chips[0]);
        //mainScreen.drawChipName(18 * 4, 0 * 8, 1,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriOPN2, chips[1]);
        //mainScreen.drawChipName(23 * 4, 0 * 8, 2,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriOPNA, chips[2]);
        //mainScreen.drawChipName(28 * 4, 0 * 8, 3,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriOPNB, chips[3]);
        //mainScreen.drawChipName(33 * 4, 0 * 8, 4,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriOPM, chips[4]);
        //mainScreen.drawChipName(37 * 4, 0 * 8, 5,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriDCSG, chips[5]);
        //mainScreen.drawChipName(42 * 4, 0 * 8, 6,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriRF5C, chips[6]);
        //mainScreen.drawChipName(47 * 4, 0 * 8, 7,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriPWM, chips[7]);
        //mainScreen.drawChipName(51 * 4, 0 * 8, 8,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriOKI5, chips[8]);
        //mainScreen.drawChipName(56 * 4, 0 * 8, 9,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriOKI9, chips[9]);
        //mainScreen.drawChipName(61 * 4, 0 * 8, 10,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriC140, chips[10]);
        //mainScreen.drawChipName(66 * 4, 0 * 8, 11,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriSPCM, chips[11]);
        //mainScreen.drawChipName(4 * 4, 0 * 8, 12,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriAY10, chips[12]);
        //mainScreen.drawChipName(9 * 4, 0 * 8, 13,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriOPLL, chips[13]);
        //mainScreen.drawChipName(71 * 4, 0 * 8, 14,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.PriHuC8, chips[14]);

        //mainScreen.drawChipName(14 * 4, 1 * 8, 0,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecOPN, chips[128 + 0]);
        //mainScreen.drawChipName(18 * 4, 1 * 8, 1,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecOPN2, chips[128 + 1]);
        //mainScreen.drawChipName(23 * 4, 1 * 8, 2,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecOPNA, chips[128 + 2]);
        //mainScreen.drawChipName(28 * 4, 1 * 8, 3,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecOPNB, chips[128 + 3]);
        //mainScreen.drawChipName(33 * 4, 1 * 8, 4,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecOPM, chips[128 + 4]);
        //mainScreen.drawChipName(37 * 4, 1 * 8, 5,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecDCSG, chips[128 + 5]);
        //mainScreen.drawChipName(42 * 4, 1 * 8, 6,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecRF5C, chips[128 + 6]);
        //mainScreen.drawChipName(47 * 4, 1 * 8, 7,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecPWM, chips[128 + 7]);
        //mainScreen.drawChipName(51 * 4, 1 * 8, 8,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecOKI5, chips[128 + 8]);
        //mainScreen.drawChipName(56 * 4, 1 * 8, 9,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecOKI9, chips[128 + 9]);
        //mainScreen.drawChipName(61 * 4, 1 * 8, 10,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecC140, chips[128 + 10]);
        //mainScreen.drawChipName(66 * 4, 1 * 8, 11,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecSPCM, chips[128 + 11]);
        //mainScreen.drawChipName(4 * 4, 1 * 8, 12,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecAY10, chips[128 + 12]);
        //mainScreen.drawChipName(9 * 4, 1 * 8, 13,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecOPLL, chips[128 + 13]);
        //mainScreen.drawChipName(71 * 4, 0 * 8, 14,audio.plugin.chipRegister.chip(chipLEDChip.class).chipLED_old.SecHuC8, chips[128 + 14]);

        mainScreen.drawFont4(1, 9, 1, audio.plugin != null && audio.plugin.isDataBlock(EnmModel.VirtualModel) ? "VD" : "  ");
        mainScreen.drawFont4(321 - 16, 9, 1, isPcmRAMWrite(EnmModel.VirtualModel) ? "VP" : "  ");
        mainScreen.drawFont4(1, 17, 1, audio.plugin != null && audio.plugin.isDataBlock(EnmModel.RealModel) ? "RD" : "  ");
        mainScreen.drawFont4(321 - 16, 17, 1, isPcmRAMWrite(EnmModel.RealModel) ? "RP" : "  ");

        oldParam.Cminutes = -1;
        oldParam.Csecond = -1;
        oldParam.Cmillisecond = -1;
        oldParam.TCminutes = -1;
        oldParam.TCsecond = -1;
        oldParam.TCmillisecond = -1;
        oldParam.LCminutes = -1;
        oldParam.LCsecond = -1;
        oldParam.LCmillisecond = -1;
    }

    public void stop() {
        if (audio.isPaused()) {
            audio.pause();
        }

        if (audio.plugin != null && audio.plugin.chipRegister != null) {
            RealChipPlugin realChip = audio.plugin.chipRegister.plugin(RealChipPlugin.class);
            if (realChip != null && realChip.isThreadStopped() && audio.plugin.isStopped()) {
                audio.plugin.resetTimeCounter();
            }
        }

        frmPlayList.stop();
        OpeManager.requestToAudio(new Request(enmRequest.Stop, null, this::screenInit));
        //audio.stop();
        //screenInit();
    }

    public void pause() {
        audio.pause();
        for (VisualizerProvider p : VisualizerProvider.providers()) p.pause(audio);
    }

    private void fadeout() {
        audio.fadeout();
    }

    private void prev() {
        if (audio.isPaused()) {
            audio.pause();
        }

        frmPlayList.prevPlay(newButtonMode[9]);
    }

    public void play() {
//        if (audio.isPaused()) {
//            audio.pause();
//        }

        String[] fn;
        Tuple4<Integer, Integer, String, String> playFn;

        frmPlayList.stop();

        if (frmPlayList.getMusicCount() < 1) {
            fn = fileOpen(false);
            if (fn == null) return;
            frmPlayList.getPlayList().addFile(fn[0]);
            //frmPlayList.AddList(fn[0]);
            playFn = frmPlayList.setStart(-1); // last
        } else {
            fn = new String[] {""};
            playFn = frmPlayList.setStart(-2); // first
        }
        if (playFn == null) return; // nothing could be added

        reqAllScreenInit = true;

        if (loadAndPlay(playFn.getItem1(), playFn.getItem2(), playFn.getItem3(), playFn.getItem4())) {
            frmPlayList.play();
        }
    }

    /** the thread {@link Audio#play()} renders the current song on */
    private Thread audioThread;

    /** set while a song has been asked for but has not started coming out yet */
    private volatile boolean songStarting;

    /**
     * Starts rendering the loaded song.
     * <p>
     * {@link Audio#play()} does not return until the song ends — it is the render loop — so it
     * cannot be run on the event dispatch thread, or the whole GUI would freeze for the length of
     * the song. It stops whatever was playing before, so the previous thread ends by itself.
     */
    private void startAudio() {
        songStarting = true;
        audioThread = new Thread(() -> {
            try {
                if (!audio.play()) {
                    SwingUtilities.invokeLater(() -> {
                        frmPlayList.stop();
                        OpeManager.requestToAudio(new Request(enmRequest.Stop, null, null));
                    });
                }
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }, "mdplayer-audio");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    private void playData() {
        try {
            if (audio.isPaused()) {
                audio.pause();
            }

            reqAllScreenInit = true;

            if (setting.getOther().getWavSwitch()) {
                if (!Files.exists(Path.of(setting.getOther().getWavPath()))) {
                    int res = JOptionPane.showConfirmDialog(this,
                            "The path set for the wav file output destination does not exist. Create it and continue playing?",
                            "Confirmation of Path Creation",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (res == JOptionPane.NO_OPTION) {
                        throw new IllegalStateException("cancel");
                    }
                    try {
                        Files.createDirectory(Path.of(setting.getOther().getWavPath()));
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this,
                                "Failed to create path. Stop playing.", "Creation failed", JOptionPane.ERROR_MESSAGE);
                        throw new IllegalStateException("cancel");
                    }
                }
            }

            // before a sample is rendered, so they take the chips as the last song left them
            for (VisualizerProvider p : VisualizerProvider.providers()) p.start(audio);

            startAudio();

            for (int chipId = 0; chipId < 2; chipId++) {
                for (ViewProvider p : views.keySet()) {
                    p.reapplyChannelMasks(audio, chipId);
                }
            }

//logger.log(Level.TRACE, "stopped: " + audio.stopped + ", " + audio.hashCode());

            if (frmInfo != null) {
                frmInfo.update();
            }


            // the panels the song wants are opened once it is actually playing, from the screen
            // loop — until then there is nothing to ask about which chips it uses
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    private void ff() {
        if (audio.isPaused()) {
            audio.pause();
        }

        audio.plugin.ff();
    }

    private void next() {
        if (audio.isPaused()) {
            audio.pause();
        }

        Request req = new Request(enmRequest.Stop, null, null);
        OpeManager.requestToAudio(req);
        while (!req.getEnd()) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }

        screenInit(null);

        frmPlayList.nextPlayMode(newButtonMode[9]);
    }

    private void nextPlayMode() {
        frmPlayList.nextPlayMode(newButtonMode[9]);
    }

    private void slow() {
        if (audio.isPaused()) {
            // upstream STBL506 dropped the frame-advance button: slow while paused now creeps
            // forward at a hundredth of the speed rather than stepping a fixed number of frames
            audio.plugin.speed(0.01);
            audio.pause();
            return;
        }

        if (audio.plugin.isStopped()) {
            play();
        }

        audio.plugin.slow();
    }

    private void playMode() {
        newButtonMode[9]++;
        if (newButtonMode[9] > 3) newButtonMode[9] = 0;
        opeButtonMode.setToolTipText(modeTip[newButtonMode[9]]);
    }

    private static final Preferences prefs = Preferences.userNodeForPackage(FormMain.class);

    private String[] fileOpen(boolean isMultiSelection) {
        JFileChooser ofd = new JFileChooser();

        Common.toFileFilters(rb2.getString("cntSupportFile")).forEach(ofd::setFileFilter);
        String lastPath = prefs.get("mdplayer.lasPath", null);
        if (lastPath != null) ofd.setCurrentDirectory(new File(lastPath));
        ofd.setDialogTitle("Select a file");
        int filterIndex = setting.getOther().getFilterIndex();
        FileFilter[] filters = ofd.getChoosableFileFilters();
        if (filterIndex >= 0 && filterIndex < filters.length) {
            ofd.setFileFilter(filters[filterIndex]);
        }

        if (!setting.getOther().getDefaultDataPath().isEmpty() && Files.exists(Path.of(setting.getOther().getDefaultDataPath())) && isInitialOpenFolder) {
            ofd.setCurrentDirectory(new File(setting.getOther().getDefaultDataPath()));
//        } else {
//            ofd.restoreDirectory = true;
        }
//        ofd.checkPathExists = true;
        ofd.setMultiSelectionEnabled(isMultiSelection);

        if (ofd.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        prefs.put("mdplayer.lasPath", ofd.getCurrentDirectory().getPath());

        isInitialOpenFolder = false;
        setting.getOther().setFilterIndex(Common.getFilterIndex(ofd));

        return ofd.getSelectedFile().list();
    }

    private void dispPlayList() {
        frmPlayList.setting = setting;
        //if (!setting.getlocation().PPlayList.equals(empty)) {
        //    frmPlayList.setLocation(setting.getlocation().PPlayList);
        //}
        //if (!setting.getlocation().PPlayListWH.equals(empty)) {
        //    frmPlayList.getWidth() = setting.getlocation().PPlayListWH.x;
        //    frmPlayList.getHeight() = setting.getlocation().PPlayListWH.y;
        //}
        frmPlayList.setVisible(!frmPlayList.isVisible());
        if (frmPlayList.isVisible()) {
            checkAndSetForm(frmPlayList);
            frmPlayList.toFront();
            frmPlayList.requestFocus();
        }
    }

    /**
     * Shows or hides the VST effect chain.
     * <p>
     * This is what the VST button on the main window and the VST entry of the window menu have
     * always called; it was an empty method, so both did nothing. The window is built on first
     * use rather than with the rest of them, since most sessions never open it.
     */
    private void dispVSTList() {
        if (frmVSTeffectList == null || frmVSTeffectList.isClosed) {
            openVSTeffectList();
            return;
        }

        frmVSTeffectList.setVisible(!frmVSTeffectList.isVisible());
        if (frmVSTeffectList.isVisible()) {
            checkAndSetForm(frmVSTeffectList);
            frmVSTeffectList.dispPluginList();
            frmVSTeffectList.toFront();
            frmVSTeffectList.requestFocus();
        }
    }

    private void showContextMenu() {
        // a JPopupMenu must be shown via show(invoker, x, y); setVisible(true) alone leaves it
        // without an invoker, so it comes up unplaced and mislaid-out
        PointerInfo pi = MouseInfo.getPointerInfo();
        Point p = pi.getLocation();
        SwingUtilities.convertPointFromScreen(p, this);
        cmsOpenOtherPanel.show(this, p.x, p.y);
    }

    /**
     * Copies the tone of one channel to the clipboard, in the instrument-text format the user
     * chose; the chip's provider knows which formats fit it.
     */
    public void getInstCh(Class<? extends Chip> chip, int ch, int chipId) {
        try {
            ym2612MIDI.setVoiceFromChipRegister(chip, chipId, ch);

            if (!setting.getOther().getUseGetInst()) return;

            ViewProvider p = chipProviders.get(chip);
            if (p != null) p.getInstCh(this, audio, setting, ch, chipId);
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Sound output error", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean loadAndPlay(int m, int songNo, String fn, String zfn /* = null */) {
        try {
            // the plugin of the song played so far; there is none before the first one
            if (audio.plugin != null && audio.plugin.flgReinit) flgReinit = true;
            if (setting.getOther().getInitAlways()) flgReinit = true;
            reinit(setting);

            if (audio.isPaused()) {
                audio.pause();
            }

            String playingFileName = fn;
            String playingArcFileName = "";
            FileFormat format = FileFormat.getFileFormat(fn);
            List<Tuple<String, byte[]>> extFile;

            if (zfn != null && !zfn.isEmpty()) {
                playingArcFileName = zfn;
                playingFileName = fn;
                format = FileFormat.getFileFormat(zfn);
            }
            // .vgz and friends are compressed: reading the file raw fails the format's header check
            format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(Path.of(fn)))), null);

            BasePlugin<? extends BaseDriver> plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
            // setParams applies the song's preset volume balance, before prepare() reaches the chips
            plugin.setParams(format, Map.of(
                    "fileName", playingFileName,
                    "arcFileName", playingArcFileName,
                    "midiMode", m,
                    "songNo", songNo)
            );
            updateMixerView();
            plugin.init();
            audio.init(plugin);

            SwingUtilities.invokeLater(this::playData);

        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this,
                    "Failed to load file.\nMessage=%s".formatted(ex.getMessage()),
                    "MDPlayer", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private boolean bufferPlay(byte[] buf, String fullPath) {
        try {
            // the plugin of the song played so far; there is none before the first one
            if (audio.plugin != null && audio.plugin.flgReinit) flgReinit = true;
            if (setting.getOther().getInitAlways()) flgReinit = true;
            reinit(setting);

            if (audio.isPaused()) {
                audio.pause();
            }

            String playingFileName = fullPath;
            String playingArcFileName = "";
            FileFormat format = FileFormat.getFileFormat(fullPath);
            format.load(new ByteArrayInputStream(buf), null);

            // TODO buf reaches the format, but setParams only names a file, so a plugin that reads
            //  the file itself still needs fullPath to exist. The SPLAY remote (mml2vgm preview)
            //  can pass a buffer with no file behind it.
            BasePlugin<? extends BaseDriver> plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
            // setParams applies the song's preset volume balance, before prepare() reaches the chips
            plugin.setParams(format, Map.of(
                    "fileName", playingFileName,
                    "arcFileName", playingArcFileName,
                    "midiMode", 0,
                    "songNo", 0)
            );
            updateMixerView();
            audio.init(plugin);

            SwingUtilities.invokeLater(this::playData);

        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this,
                    "Failed to load file.\nMessage=%s".formatted(ex.getMessage()),
                    "MDPlayer", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        frmPlayList.play();
        return true;
    }

    /** which provider answers for which chip — channel masks, tone copies */
    private final Map<Class<? extends Chip>, ViewProvider> chipProviders = new LinkedHashMap<>();

    {
        for (ViewProvider p : views.keySet()) {
            for (Class<? extends Chip> c : p.maskChips()) {
                chipProviders.put(c, p);
            }
        }
    }

    /**
     * Toggles the mute of one channel (the OPN family only ever masks here; unmasking goes through
     * {@link #resetChannelMask}). The chip's own mask state is the single source of truth, and the
     * chip's provider knows how to flip it.
     */
    public void setChannelMask(Class<? extends Chip> chip, int chipId, int ch) {
        ViewProvider p = chipProviders.get(chip);
        if (p != null) p.setChannelMask(audio, chip, chipId, ch);
    }

    public void resetChannelMask(Class<? extends Chip> chip, int chipId, int ch) {
        ViewProvider p = chipProviders.get(chip);
        if (p != null) p.resetChannelMask(audio, chip, chipId, ch);
    }

    /** Reapplies a channel's mute to the (freshly initialized) chip, e.g. when a new song starts. */
    public void forceChannelMask(Class<? extends Chip> chip, int chipId, int ch, boolean mask) {
        ViewProvider p = chipProviders.get(chip);
        if (p != null) p.forceChannelMask(audio, chip, chipId, ch, mask);
    }

//#region midi

    private void startMIDIInMonitoring() {

        if (setting.getMidiKbd().getMidiInDeviceName().isEmpty()) {
            return;
        }

        if (midiin != null) {
            try {
                midiin.close();
                midiIn_MessageReceived.close();
                midiin = null;
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
                midiin = null;
            }
        }

        if (midiin == null) {
            MidiDevice.Info[] midiDeviceInfos = MidiSystem.getMidiDeviceInfo();
            for (var info : midiDeviceInfos) {
                MidiDevice device;
                try {
                    device = MidiSystem.getMidiDevice(info);
                } catch (MidiUnavailableException e) {
                    throw new RuntimeException(e);
                }
                if (device.getMaxTransmitters() == 0) {
                    continue;
                }
                if (setting.getMidiKbd().getMidiInDeviceName().equals(info.getName())) {
                    try {
                        midiin = device.getTransmitter();
                        midiin.setReceiver(midiIn_MessageReceived);
                    } catch (Exception e) {
                        logger.log(Level.ERROR, e.getMessage(), e);
                        midiin = null;
                    }
                }
            }
        }
    }

//    void midiIn_ErrorReceived(Object source, MidiInMessageEventArgs e) {
//        logger.log(Level.ERROR, "Error Time %s Message 0x%08x Event %s".formatted(e.Timestamp, e.RawMessage, e.MidiEvent));
//    }

    private void StopMIDIInMonitoring() {
        if (midiin != null) {
            try {
                midiin.close();
                this.midiIn_MessageReceived.close();
                midiin = null;
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
                midiin = null;
            }
        }
    }

    private final Receiver midiIn_MessageReceived = new Receiver() {
        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!setting.getMidiKbd().getUseMIDIKeyboard()) return;
            ym2612MIDI.midiIn_MessageReceived(message);
        }

        @Override
        public void close() {
        }
    };

    public void ym2612Midi_ClearNoteLog() {
        ym2612MIDI.clearNoteLog();
    }

    public void ym2612Midi_ClearNoteLog(int ch) {
        ym2612MIDI.clearNoteLog(ch);
    }

    public void ym2612Midi_Log2MML(int ch) {
        ym2612MIDI.log2MML(ch);
    }

    public void ym2612Midi_Log2MML66(int ch) {
        ym2612MIDI.log2MML66(ch);
    }

    public void ym2612Midi_AllNoteOff() {
        ym2612MIDI.allNoteOff();
    }

    public void ym2612Midi_SetMode(int m) {
        ym2612MIDI.setMode(m);
    }

    public void ym2612Midi_SelectChannel(int ch) {
        ym2612MIDI.selectChannel(ch);
    }

    public void ym2612Midi_SetTonesToSetting() {
        ym2612MIDI.setTonesToSettng();
    }

    public void ym2612Midi_SetTonesFromSetting() {
        ym2612MIDI.setTonesFromSettng();
    }

    /**
     * @param tp 1 origin
     */
    public void ym2612Midi_SaveTonePallet(String fn, int tp) {
        ym2612MIDI.saveTonePallet(fn, tp, tonePallet);
    }

    /**
     * @param tp 1 origin
     */
    public void ym2612Midi_LoadTonePallet(String fn, int tp) {
        ym2612MIDI.loadTonePallet(fn, tp, tonePallet);
    }

    public void ym2612Midi_CopyToneToClipboard() {
        if (setting.getMidiKbd().isMono()) {
            ym2612MIDI.copyToneToClipboard(new int[] {setting.getMidiKbd().getUseMonoChannel()});
        } else {
            List<Integer> uc = new ArrayList<>();
            for (int i = 0; i < setting.getMidiKbd().getUseChannel().length; i++) {
                if (setting.getMidiKbd().getUseChannel()[i]) uc.add(i);
            }
            ym2612MIDI.copyToneToClipboard(uc.stream().mapToInt(Integer::intValue).toArray());
        }
    }

    public void ym2612Midi_PasteToneFromClipboard() {
        if (setting.getMidiKbd().isMono()) {
            ym2612MIDI.pasteToneFromClipboard(new int[] {setting.getMidiKbd().getUseMonoChannel()});
        } else {
            List<Integer> uc = new ArrayList<>();
            for (int i = 0; i < setting.getMidiKbd().getUseChannel().length; i++) {
                if (setting.getMidiKbd().getUseChannel()[i]) uc.add(i);
            }
            ym2612MIDI.pasteToneFromClipboard(uc.stream().mapToInt(Integer::intValue).toArray());
        }
    }

    public void ym2612Midi_CopyToneToClipboard(int ch) {
        ym2612MIDI.copyToneToClipboard(new int[] {ch});
    }

    public void ym2612Midi_PasteToneFromClipboard(int ch) {
        ym2612MIDI.pasteToneFromClipboard(new int[] {ch});
    }

    public void ym2612Midi_SetSelectInstParam(int ch, int n) {
        ym2612MIDI.ym2612Midi.selectCh = ch;
        ym2612MIDI.ym2612Midi.selectParam = n;
    }

    public void ym2612Midi_AddSelectInstParam(int n) {
        int p = ym2612MIDI.ym2612Midi.selectParam;
        p += n;
        if (p > 47) p = 0;
        ym2612MIDI.ym2612Midi.selectParam = p;
    }

    public void ym2612Midi_ChangeSelectedParamValue(int n) {
        ym2612MIDI.changeSelectedParamValue(n);
    }

//#endregion

    /** the preset balance is loaded by {@link BasePlugin#setParams} now, so the view just catches up */
    private void updateMixerView() {
        SwingUtilities.invokeLater(() -> {
            if (frmMixer2 != null) frmMixer2.update();
        });
    }

    private void manualSavePresetMixerBalance(boolean isDriverBalance, String playingFileName, String playingArcFileName, FileFormat format, Setting.Balance balance) {
        if (!setting.getAutoBalance().getUseThis()) return;

        try {
            Path fullPath = Common.settingFilePath;
            fullPath = fullPath.resolve("MixerBalance");
            if (!Files.exists(fullPath)) Files.createDirectory(fullPath);
            String fn = "";

            if (isDriverBalance) {
                String[] fns = format.getPresetMixerBalance();
                if (fns != null) {
                    fn = fns[0];
                }

                fullPath = fullPath.resolve(fn);

            } else {

            }

            balance.save(fullPath);
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }

    public String saveDriverBalance(Setting.Balance balance) {
        PlayList.Music music = frmPlayList.getPlayingSongInfo();
        if (music == null) {
            throw new IllegalStateException("Performance information could not be obtained. Please try again during or immediately after performance has finished.");
        }

        FileFormat fmt = music.format;
        manualSavePresetMixerBalance(true, "", "", fmt, balance);

        return fmt.toString();
    }

    public PlayList.Music getPlayingMusicInfo() {
        PlayList.Music music = frmPlayList.getPlayingSongInfo();
        return music;
    }

    public static Consumer<NativeKeyEvent> keyHookMeth = null;

    private final NativeKeyListener keyboardHook1_KeyboardHooked = new NativeKeyListener() {
        @Override
        public void nativeKeyPressed(NativeKeyEvent e) {
            logger.log(Level.TRACE, "Key Pressed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));

            if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
                try {
                    GlobalScreen.unregisterNativeHook();
                } catch (NativeHookException nativeHookException) {
                    logger.log(Level.ERROR, nativeHookException.getMessage(), nativeHookException);
                }
            }

            if (keyHookMeth != null) {
                keyHookMeth.accept(e);
                return;
            }

            String k = String.valueOf(e.getKeyCode());
            boolean shift = (e.getModifiers() & NativeKeyEvent.SHIFT_MASK) != 0;
            boolean ctrl = (e.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0;
            boolean alt = (e.getModifiers() & NativeKeyEvent.ALT_MASK) != 0;
            Setting.KeyBoardHook.HookKeyInfo info;

            info = setting.getKeyboardHook().getStop();
            if (info.getKey().equals(k) && info.getShift() == shift && info.getCtrl() == ctrl && info.getAlt() == alt) {
                stop();
                return;
            }

            info = setting.getKeyboardHook().getPause();
            if (info.getKey().equals(k) && info.getShift() == shift && info.getCtrl() == ctrl && info.getAlt() == alt) {
                pause();
                return;
            }

            info = setting.getKeyboardHook().getFadeout();
            if (info.getKey().equals(k) && info.getShift() == shift && info.getCtrl() == ctrl && info.getAlt() == alt) {
                fadeout();
                return;
            }

            info = setting.getKeyboardHook().getPrev();
            if (info.getKey().equals(k) && info.getShift() == shift && info.getCtrl() == ctrl && info.getAlt() == alt) {
                prev();
                return;
            }

            info = setting.getKeyboardHook().getSlow();
            if (info.getKey().equals(k) && info.getShift() == shift && info.getCtrl() == ctrl && info.getAlt() == alt) {
                slow();
                return;
            }

            info = setting.getKeyboardHook().getPlay();
            if (info.getKey().equals(k) && info.getShift() == shift && info.getCtrl() == ctrl && info.getAlt() == alt) {
                play();
                return;
            }

            info = setting.getKeyboardHook().getNext();
            if (info.getKey().equals(k) && info.getShift() == shift && info.getCtrl() == ctrl && info.getAlt() == alt) {
                next();
                return;
            }

            info = setting.getKeyboardHook().getFast();
            if (info.getKey().equals(k) && info.getShift() == shift && info.getCtrl() == ctrl && info.getAlt() == alt) {
                ff();
            }
        }
    };

    /**
     * The open-other-panel and register-dump menus, built from the provider list: one item per
     * view under its category ("psg", "opl" ... — the subpackage its form came from), once for
     * the primary chips and once for the secondary ones.
     */
    private void buildViewMenus() {
        JMenu[] roots = {primaryToolStripMenuItem, sencondryToolStripMenuItem};
        for (int chipId = 0; chipId < roots.length; chipId++) {
            Map<String, JMenu> categories = new LinkedHashMap<>();
            for (ViewProvider p : views.keySet()) {
                if (!p.hasMenuItem() || chipId >= p.instances()) continue;

                JMenuItem item = new JMenuItem(p.menuText());
                item.setName("tsmi%s%s".formatted(chipId == 0 ? "P" : "S", p.id()));
                int id = chipId;
                item.addActionListener(ev -> openView(p, id, false));

                if (p.category() == null) {
                    roots[chipId].add(item);
                } else {
                    JMenu root = roots[chipId];
                    int prefix = chipId;
                    categories.computeIfAbsent(p.category(), c -> {
                        JMenu menu = new JMenu(c.toUpperCase());
                        menu.setName("tsmiC%s%s".formatted(prefix == 0 ? "P" : "S", c.toUpperCase()));
                        root.add(menu);
                        return menu;
                    }).add(item);
                }
            }
        }

        for (ViewProvider p : views.keySet()) {
            if (!p.hasRegisterDump()) continue;

            JMenuItem item = new JMenuItem(p.id());
            item.setName("tsmiRD" + p.id());
            item.addActionListener(ev -> openRegTest(p.chip()));
            registerDumpDisplayToolStripMenuItem.add(item);
        }
    }

    public static void checkAndSetForm(JFrame frm) {
        frm.pack();
        Rectangle s = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        Rectangle rc = new Rectangle(frm.getLocation(), frm.getSize());
        if (s.contains(rc)) {
            frm.setLocation(rc.getLocation());
            frm.setPreferredSize(rc.getSize());
        } else {
            frm.setLocation(new Point(100, 100));
        }
    }

    private void tsmiOpenFile_Click(ActionEvent ev) {
        String[] fn = fileOpen(true);

        if (fn != null)
            addFileAndPlay(fn);
    }

    private void addFileAndPlay(String[] fn) {
        if (audio.isPaused()) {
            audio.pause();
        }

        if (fn.length == 1) {
            frmPlayList.stop();

            frmPlayList.getPlayList().addFile(fn[0]);

            if (!(FileFormat.getFileFormat(fn[0]) instanceof M3UFileFormat) && !(FileFormat.getFileFormat(fn[0]) instanceof ZIPFileFormat)) {
                if (!loadAndPlay(0, 0, fn[0], "")) return;
                frmPlayList.setStart(-1);
            }
            oldParam = new ScreenParams();

            frmPlayList.play();
        } else {
            frmPlayList.stop();

            try {
                for (String f : fn) {
                    frmPlayList.getPlayList().addFile(f);
                }
            } catch (Exception ex) {
                logger.log(Level.ERROR, ex.getMessage(), ex);
            }
        }
    }

    private void tsmiPlay_Click(ActionEvent ev) {
        int n = frmPlayList.getMusicCount();

        if (newButtonMode[9] != 1 || n < 1) {
            play();
            oldParam = new ScreenParams();
        } else {
            next();
        }
    }

    private void tsmiStop_Click(ActionEvent ev) {
        frmPlayList.stop();
        stop();
    }

    private void tsmiFadeOut_Click(ActionEvent ev) {
        fadeout();
        frmPlayList.stop();
    }

    private void tsmiChangeZoom_Click(ActionEvent ev) {
        if (ev != null && ev.getSource() == tsmiChangeZoomX1) setting.getOther().setZoom(1);
        else if (ev != null && ev.getSource() == tsmiChangeZoomX2) setting.getOther().setZoom(2);
        else if (ev != null && ev.getSource() == tsmiChangeZoomX3) setting.getOther().setZoom(3);
        else if (ev != null && ev.getSource() == tsmiChangeZoomX4) setting.getOther().setZoom(4);
        else
            setting.getOther().setZoom((setting.getOther().getZoom() == 4) ? 1 : (setting.getOther().getZoom() + 1));

        changeZoom();
    }

    private final BufferedImage[] lstOpeButtonEnterImage = {
            Common.getImage("chSetting"),
            Common.getImage("chStop"),
            Common.getImage("chPause"),
            Common.getImage("chFadeout"),
            Common.getImage("chPrevious"),
            Common.getImage("chSlow"),
            Common.getImage("chPlay"),
            Common.getImage("chFast"),
            Common.getImage("chNext"),
            Common.getImage("chStep"),
            Common.getImage("chOpenFolder"),
            Common.getImage("chPlayList"),
            Common.getImage("chInformation"),
            Common.getImage("chMixer"),
            Common.getImage("chKBD"),
            Common.getImage("chVST"),
            Common.getImage("chMIDIKBD"),
            Common.getImage("chZoom"),
            Common.getImage("chRandom"),
            Common.getImage("chLoop"),
            Common.getImage("chLoopOne")
    };
    private final BufferedImage[] lstOpeButtonLeaveImage = {
            Common.getImage("ccSetting"),
            Common.getImage("ccStop"),
            Common.getImage("ccPause"),
            Common.getImage("ccFadeout"),
            Common.getImage("ccPrevious"),
            Common.getImage("ccSlow"),
            Common.getImage("ccPlay"),
            Common.getImage("ccFast"),
            Common.getImage("ccNext"),
            Common.getImage("ccStep"),
            Common.getImage("ccOpenFolder"),
            Common.getImage("ccPlayList"),
            Common.getImage("ccInformation"),
            Common.getImage("ccMixer"),
            Common.getImage("ccKBD"),
            Common.getImage("ccVST"),
            Common.getImage("ccMIDIKBD"),
            Common.getImage("ccZoom"),
            Common.getImage("ccRandom"),
            Common.getImage("ccLoop"),
            Common.getImage("ccLoopOne")
    };
    private final BufferedImage[] lstOpeButtonActiveImage = {
            Common.getImage("ciSetting"),
            Common.getImage("ciStop"),
            Common.getImage("ciPause"),
            Common.getImage("ciFadeout"),
            Common.getImage("ciPrevious"),
            Common.getImage("ciSlow"),
            Common.getImage("ciPlay"),
            Common.getImage("ciFast"),
            Common.getImage("ciNext"),
            Common.getImage("ciStep"),
            Common.getImage("ciOpenFolder"),
            Common.getImage("ciPlayList"),
            Common.getImage("ciInformation"),
            Common.getImage("ciMixer"),
            Common.getImage("ciKBD"),
            Common.getImage("ciVST"),
            Common.getImage("ciMIDIKBD"),
            Common.getImage("ciZoom"),
            Common.getImage("ciRandom"),
            Common.getImage("ciLoop"),
            Common.getImage("ciLoopOne")
    };
    private final boolean[] lstOpeButtonActive = {
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false
    };
    private final boolean[] lstOpeButtonActiveOld = {
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false
    };
    private JButton[] lstOpeButtonControl;

    private void relocateOpeButton(int zoom) {
        // the skin lays the buttons out in two rows — transport on top (y 9), the rest below
        // (y 25) — not the single row the earlier port flattened them into; under the content
        // pane's null layout a bare setLocation() would also leave each button 0x0 and invisible,
        // so give each its 16x16 (times zoom) skin cell as bounds
        int s = 16 * zoom;
        opeButtonStop.setBounds((17 + 16 * 0) * zoom, 9 * zoom, s, s);
        opeButtonPause.setBounds((17 + 16 * 1) * zoom, 9 * zoom, s, s);
        opeButtonFadeout.setBounds((17 + 16 * 2) * zoom, 9 * zoom, s, s);
        opeButtonPrevious.setBounds((17 + 16 * 3) * zoom, 9 * zoom, s, s);
        opeButtonSlow.setBounds((17 + 16 * 4) * zoom, 9 * zoom, s, s);
        opeButtonPlay.setBounds((17 + 16 * 5) * zoom, 9 * zoom, s, s);
        opeButtonFast.setBounds((17 + 16 * 6) * zoom, 9 * zoom, s, s);
        opeButtonNext.setBounds((17 + 16 * 7) * zoom, 9 * zoom, s, s);
        opeButtonMode.setBounds((17 + 16 * 8) * zoom, 9 * zoom, s, s);

        opeButtonSetting.setBounds((17 + 16 * 0) * zoom, 25 * zoom, s, s);
        opeButtonOpen.setBounds((17 + 16 * 1) * zoom, 25 * zoom, s, s);
        opeButtonPlayList.setBounds((17 + 16 * 2) * zoom, 25 * zoom, s, s);
        opeButtonInformation.setBounds((17 + 16 * 3) * zoom, 25 * zoom, s, s);
        opeButtonMixer.setBounds((17 + 16 * 4) * zoom, 25 * zoom, s, s);
        opeButtonKBD.setBounds((17 + 16 * 5) * zoom, 25 * zoom, s, s);
        opeButtonVST.setBounds((17 + 16 * 6) * zoom, 25 * zoom, s, s);
        opeButtonMIDIKBD.setBounds((17 + 16 * 7) * zoom, 25 * zoom, s, s);
        opeButtonZoom.setBounds((17 + 16 * 8) * zoom, 25 * zoom, s, s);

        redrawButton(opeButtonSetting, setting.getOther().getZoom(), lstOpeButtonLeaveImage[0]);
        redrawButton(opeButtonStop, setting.getOther().getZoom(), lstOpeButtonLeaveImage[1]);
        redrawButton(opeButtonPause, setting.getOther().getZoom(), lstOpeButtonLeaveImage[2]);
        redrawButton(opeButtonFadeout, setting.getOther().getZoom(), lstOpeButtonLeaveImage[3]);
        redrawButton(opeButtonPrevious, setting.getOther().getZoom(), lstOpeButtonLeaveImage[4]);
        redrawButton(opeButtonSlow, setting.getOther().getZoom(), lstOpeButtonLeaveImage[5]);
        redrawButton(opeButtonPlay, setting.getOther().getZoom(), lstOpeButtonLeaveImage[6]);
        redrawButton(opeButtonFast, setting.getOther().getZoom(), lstOpeButtonLeaveImage[7]);
        redrawButton(opeButtonNext, setting.getOther().getZoom(), lstOpeButtonLeaveImage[8]);
        int m = newButtonMode[9] == 0 ? 9 : (newButtonMode[9] == 1 ? 18 : (newButtonMode[9] == 2 ? 19 : 20));
        redrawButton(opeButtonMode, setting.getOther().getZoom(), lstOpeButtonLeaveImage[m]);
        redrawButton(opeButtonOpen, setting.getOther().getZoom(), lstOpeButtonLeaveImage[10]);
        redrawButton(opeButtonPlayList, setting.getOther().getZoom(), lstOpeButtonLeaveImage[11]);
        redrawButton(opeButtonInformation, setting.getOther().getZoom(), lstOpeButtonLeaveImage[12]);
        redrawButton(opeButtonMixer, setting.getOther().getZoom(), lstOpeButtonLeaveImage[13]);
        redrawButton(opeButtonKBD, setting.getOther().getZoom(), lstOpeButtonLeaveImage[14]);
        redrawButton(opeButtonVST, setting.getOther().getZoom(), lstOpeButtonLeaveImage[15]);
        redrawButton(opeButtonMIDIKBD, setting.getOther().getZoom(), lstOpeButtonLeaveImage[16]);
        redrawButton(opeButtonZoom, setting.getOther().getZoom(), lstOpeButtonLeaveImage[17]);
    }

    private final MouseListener opeButton_Mouse = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent ev) {
            JButton btn = (JButton) ev.getSource();
            int index = Integer.parseInt(btn.getActionCommand());
            int m = index;
            if (m == 9) {
                m = newButtonMode[9] == 0 ? 9 : (newButtonMode[9] == 1 ? 18 : (newButtonMode[9] == 2 ? 19 : 20));
            }
            redrawButton(btn, setting.getOther().getZoom(), lstOpeButtonEnterImage[m]);
        }

        @Override
        public void mouseExited(MouseEvent ev) {
            JButton btn = (JButton) ev.getSource();
            int index = Integer.parseInt(btn.getActionCommand());
            int m = index;
            if (m == 9) {
                m = newButtonMode[9] == 0 ? 9 : (newButtonMode[9] == 1 ? 18 : (newButtonMode[9] == 2 ? 19 : 20));
            }

            redrawButton(btn, setting.getOther().getZoom(), lstOpeButtonActive[m] ? lstOpeButtonActiveImage[m] : lstOpeButtonLeaveImage[m]);
        }
    };

    private static void redrawButton(JButton button, int zoom, BufferedImage image) {
        try {
            final int size = 16; // the sprites are 16x16 at 1x
            int dim = size * zoom;
            button.setPreferredSize(new Dimension(dim, dim));

            BufferedImage canvas = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setColor(Color.black);
            g.fillRect(0, 0, dim, dim);
            // scale the whole 16x16 sprite up to the zoomed button; the source rect must be the
            // sprite's own size, not the destination size, or only its top-left corner is copied and
            // the icon comes out at 1x in a corner of the cell
            g.drawImage(image, 0, 0, dim, dim, 0, 0, size, size, null);
            g.dispose();

            button.setIcon(new ImageIcon(canvas));
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    private void opeButtonPrevious_Click(ActionEvent ev) {
        prev();
        oldParam = new ScreenParams();
    }

    private void opeButtonMode_Click(ActionEvent ev) {
        playMode();
        opeButton_Mouse.mouseEntered(new MouseEvent(opeButtonMode, 0, 0, 0, 0, 0, 0, 0, 0, false, 0)); // opeButtonMode
    }

    private void updateOpeButtonActiveState() {
        lstOpeButtonActive[1] = (audio.plugin.isStopped()); // STOP button
        lstOpeButtonActive[2] = audio.plugin.isStopped() ? false : audio.isPaused(); // PAUSE button
        lstOpeButtonActive[3] = audio.plugin.isStopped() ? false : audio.plugin.isFadeOut(); // Fade button
        lstOpeButtonActive[5] = audio.plugin.isSlow(); // Slowbutton
        lstOpeButtonActive[6] = audio.isPaused() ? false : (audio.plugin.isSlow() || audio.plugin.isFF() || audio.plugin.isFadeOut() ? false : !audio.plugin.isStopped()); // PLAY button
        lstOpeButtonActive[7] = audio.plugin.isFF(); // FFbutton
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();
        this.cmsOpenOtherPanel = new JPopupMenu();
        this.primaryToolStripMenuItem = new JMenu();
        this.sencondryToolStripMenuItem = new JMenu();
        this.cmsMenu = new JPopupMenu();
        this.fileToolStripMenuItem = new JMenu();
        this.tsmiOpenFile = new JMenuItem();
        this.tsmiExit = new JMenuItem();
        this.operationToolStripMenuItem = new JMenu();
        this.tsmiPlay = new JMenuItem();
        this.tsmiStop = new JMenuItem();
        this.tsmiPause = new JMenuItem();
        this.tsmiFadeOut = new JMenuItem();
        this.tsmiSlow = new JMenuItem();
        this.tsmiFf = new JMenuItem();
        this.tsmiNext = new JMenuItem();
        this.tsmiPlayMode = new JMenuItem();
        this.tsmiOption = new JMenuItem();
        this.tsmiPlayList = new JMenuItem();
        this.tsmiOpenInfo = new JMenuItem();
        this.tsmiOpenMixer = new JMenuItem();
        this.anotherWindowDisplayToolStripMenuItem = new JMenu();
        this.tsmiKBrd = new JMenuItem();
        this.tsmiVST = new JMenuItem();
        this.tsmiMIDIkbd = new JMenuItem();
        this.tsmiChangeZoom = new JMenu();
        this.tsmiChangeZoomX1 = new JMenuItem();
        this.tsmiChangeZoomX2 = new JMenuItem();
        this.tsmiChangeZoomX3 = new JMenuItem();
        this.tsmiChangeZoomX4 = new JMenuItem();
        this.registerDumpDisplayToolStripMenuItem = new JMenu();
        this.visualizerMenu = new JMenu();
        this.tsmiConsole = new JMenuItem();
        this.opeButtonSetting = new JButton();
        this.toolTip1 = new JToolTip();
        this.opeButtonStop = new JButton();
        this.opeButtonPause = new JButton();
        this.opeButtonFadeout = new JButton();
        this.opeButtonPrevious = new JButton();
        this.opeButtonSlow = new JButton();
        this.opeButtonPlay = new JButton();
        this.opeButtonFast = new JButton();
        this.opeButtonNext = new JButton();
        this.opeButtonZoom = new JButton();
        this.opeButtonMIDIKBD = new JButton();
        this.opeButtonVST = new JButton();
        this.opeButtonKBD = new JButton();
        this.opeButtonMixer = new JButton();
        this.opeButtonInformation = new JButton();
        this.opeButtonPlayList = new JButton();
        this.opeButtonOpen = new JButton();
        this.opeButtonMode = new JButton();
        this.keyboardHook1 = new KeyboardHook();

        //
        // pbScreen
        //
        this.pbScreen.setBackground(Color.black);
        new DropTarget(this.pbScreen, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        // the skin is blitted in as the frame buffer's background, see DoubleBuffer in frmMain_Load
        this.pbScreen.setName("pbScreen");
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        this.pbScreen.addMouseMotionListener(this.pbScreen_MouseMove);
        //
        // cmsOpenOtherPanel
        //
        this.cmsOpenOtherPanel.add(this.primaryToolStripMenuItem);
        this.cmsOpenOtherPanel.add(this.sencondryToolStripMenuItem);
        this.cmsOpenOtherPanel.setName("cmsOpenOtherPanel");
        buildViewMenus();
        //
        // primaryToolStripMenuItem
        //
        this.primaryToolStripMenuItem.setName("primaryToolStripMenuItem");
        //
        // sencondryToolStripMenuItem
        //
        this.sencondryToolStripMenuItem.setName("sencondryToolStripMenuItem");
        //
        // cmsMenu
        //
        this.cmsMenu.add(this.fileToolStripMenuItem);
        this.cmsMenu.add(this.operationToolStripMenuItem);
        this.cmsMenu.add(this.tsmiOption);
        this.cmsMenu.add(this.tsmiPlayList);
        this.cmsMenu.add(this.tsmiOpenInfo);
        this.cmsMenu.add(this.tsmiOpenMixer);
        this.cmsMenu.add(this.anotherWindowDisplayToolStripMenuItem);
        this.cmsMenu.add(this.tsmiChangeZoom);
        this.cmsMenu.add(this.registerDumpDisplayToolStripMenuItem);
        this.cmsMenu.add(this.visualizerMenu);
        this.cmsMenu.add(this.tsmiConsole);
        this.cmsMenu.setName("contextMenuStrip1");
        //
        // fileToolStripMenuItem
        //
        this.fileToolStripMenuItem.add(this.tsmiOpenFile);
        this.fileToolStripMenuItem.add(this.tsmiExit);
        this.fileToolStripMenuItem.setIcon(new ImageIcon(Common.getImage("ccOpenFolder")));
        // the caption is keyed on the name the designer gave it
        this.fileToolStripMenuItem.setName("fileToolStripMenuItem");
        //
        // tsmiOpenFile
        //
        this.tsmiOpenFile.setName("tsmiOpenFile");
        this.tsmiOpenFile.addActionListener(this::tsmiOpenFile_Click);
        //
        // tsmiExit
        //
        this.tsmiExit.setName("tsmiExit");
        this.tsmiExit.addActionListener(_ -> this.setVisible(false));
        //
        // operationToolStripMenuItem
        //
        this.operationToolStripMenuItem.add(this.tsmiPlay);
        this.operationToolStripMenuItem.add(this.tsmiStop);
        this.operationToolStripMenuItem.add(this.tsmiPause);
        this.operationToolStripMenuItem.add(this.tsmiFadeOut);
        this.operationToolStripMenuItem.add(this.tsmiSlow);
        this.operationToolStripMenuItem.add(this.tsmiFf);
        this.operationToolStripMenuItem.add(this.tsmiNext);
        this.operationToolStripMenuItem.add(this.tsmiPlayMode);
        // the caption is keyed on the name the designer gave it
        this.operationToolStripMenuItem.setIcon(new ImageIcon(Common.getImage("empty")));
        this.operationToolStripMenuItem.setName("operationToolStripMenuItem");
        //
        // tsmiPlay
        //
        this.tsmiPlay.setIcon(new ImageIcon(Common.getImage("ccPlay")));
        this.tsmiPlay.setName("tsmiPlay");
        this.tsmiPlay.addActionListener(this::tsmiPlay_Click);
        //
        // tsmiStop
        //
        this.tsmiStop.setIcon(new ImageIcon(Common.getImage("ccStop")));
        this.tsmiStop.setName("tsmiStop");
        this.tsmiStop.addActionListener(this::tsmiStop_Click);
        //
        // tsmiPause
        //
        this.tsmiPause.setIcon(new ImageIcon(Common.getImage("ccPause")));
        this.tsmiPause.setName("tsmiPause");
        this.tsmiPause.addActionListener(_ -> pause());
        //
        // tsmiFadeOut
        //
        this.tsmiFadeOut.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.tsmiFadeOut.setName("tsmiFadeOut");
        this.tsmiFadeOut.addActionListener(this::tsmiFadeOut_Click);
        //
        // tsmiSlow
        //
        this.tsmiSlow.setIcon(new ImageIcon(Common.getImage("ccSlow")));
        this.tsmiSlow.setName("tsmiSlow");
        this.tsmiSlow.addActionListener(_ -> slow());
        //
        // tsmiFf
        //
        this.tsmiFf.setIcon(new ImageIcon(Common.getImage("ccFast")));
        this.tsmiFf.setName("tsmiFf");
        this.tsmiFf.addActionListener(_ -> ff());
        //
        // tsmiNext
        //
        this.tsmiNext.setIcon(new ImageIcon(Common.getImage("ccNext")));
        this.tsmiNext.setName("tsmiNext");
        this.tsmiNext.addActionListener(_ -> {
            next();
            oldParam = new ScreenParams();
        });
        //
        // tsmiPlayMode
        //
        this.tsmiPlayMode.setIcon(new ImageIcon(Common.getImage("ccStep")));
        this.tsmiPlayMode.setName("tsmiPlayMode");
        this.tsmiPlayMode.addActionListener(ev -> playMode());
        //
        // tsmiOption
        //
        this.tsmiOption.setIcon(new ImageIcon(Common.getImage("ccSetting")));
        this.tsmiOption.setName("tsmiOption");
        this.tsmiOption.addActionListener(_ -> openSetting());
        //
        // tsmiPlayList
        //
        this.tsmiPlayList.setIcon(new ImageIcon(Common.getImage("ccPlayList")));
        this.tsmiPlayList.setName("tsmiPlayList");
        this.tsmiPlayList.addActionListener(_ -> dispPlayList());
        //
        // tsmiOpenInfo
        //
        this.tsmiOpenInfo.setIcon(new ImageIcon(Common.getImage("ccInformation")));
        this.tsmiOpenInfo.setName("tsmiOpenInfo");
        this.tsmiOpenInfo.addActionListener(_ -> openInfo());
        //
        // tsmiOpenMixer
        //
        this.tsmiOpenMixer.setIcon(new ImageIcon(Common.getImage("ccMixer")));
        this.tsmiOpenMixer.setName("tsmiOpenMixer");
        this.tsmiOpenMixer.addActionListener(_ -> openMixer());
        //
        // anotherWindowDisplayToolStripMenuItem
        //
        this.anotherWindowDisplayToolStripMenuItem.add(this.tsmiKBrd);
        this.anotherWindowDisplayToolStripMenuItem.add(this.tsmiVST);
        this.anotherWindowDisplayToolStripMenuItem.add(this.tsmiMIDIkbd);
        // the caption is keyed on the name the designer gave it
        this.anotherWindowDisplayToolStripMenuItem.setName("anotherWindowDisplayToolStripMenuItem");
        //
        // tsmiKBrd
        //
        this.tsmiKBrd.setIcon(new ImageIcon(Common.getImage("ccKBD")));
        this.tsmiKBrd.setName("tsmiKBrd");
        this.tsmiKBrd.addActionListener(_ -> showContextMenu());
        //
        // tsmiVST
        //
        this.tsmiVST.setIcon(new ImageIcon(Common.getImage("ccVST")));
        this.tsmiVST.setName("tsmiVST");
        this.tsmiVST.addActionListener(_ -> dispVSTList());
        //
        // tsmiMIDIkbd
        //
        this.tsmiMIDIkbd.setIcon(new ImageIcon(Common.getImage("ccMIDIKBD")));
        this.tsmiMIDIkbd.setName("tsmiMIDIkbd");
        this.tsmiMIDIkbd.addActionListener(_ -> openMIDIKeyboard());
        //
        // tsmiChangeZoom
        //
        this.tsmiChangeZoom.add(this.tsmiChangeZoomX1);
        this.tsmiChangeZoom.add(this.tsmiChangeZoomX2);
        this.tsmiChangeZoom.add(this.tsmiChangeZoomX3);
        this.tsmiChangeZoom.add(this.tsmiChangeZoomX4);
        this.tsmiChangeZoom.setIcon(new ImageIcon(Common.getImage("ccZoom")));
        this.tsmiChangeZoom.setName("tsmiChangeZoom");
        this.tsmiChangeZoom.addActionListener(this::tsmiChangeZoom_Click);
        //
        // tsmiChangeZoomX1
        //
        this.tsmiChangeZoomX1.setName("tsmiChangeZoomX1");
        this.tsmiChangeZoomX1.addActionListener(this::tsmiChangeZoom_Click);
        //
        // tsmiChangeZoomX2
        //
        this.tsmiChangeZoomX2.setName("tsmiChangeZoomX2");
        this.tsmiChangeZoomX2.addActionListener(this::tsmiChangeZoom_Click);
        //
        // tsmiChangeZoomX3
        //
        this.tsmiChangeZoomX3.setName("tsmiChangeZoomX3");
        this.tsmiChangeZoomX3.addActionListener(this::tsmiChangeZoom_Click);
        //
        // tsmiChangeZoomX4
        //
        this.tsmiChangeZoomX4.setName("tsmiChangeZoomX4");
        this.tsmiChangeZoomX4.addActionListener(this::tsmiChangeZoom_Click);
        //
        // registerDumpDisplayToolStripMenuItem
        //
        // the caption is keyed on the name the designer gave it
        this.registerDumpDisplayToolStripMenuItem.setIcon(new ImageIcon(Common.getImage("empty")));
        this.registerDumpDisplayToolStripMenuItem.setName("registerDumpDisplayToolStripMenuItem");
        //
        // visualizerMenu, one item per VisualizerProvider
        //
        this.visualizerMenu.setIcon(new ImageIcon(Common.getImage("empty")));
        this.visualizerMenu.setName("visualizer");
        for (VisualizerProvider p : VisualizerProvider.providers()) {
            JMenuItem item = new JMenuItem(p.menuText());
            item.setName("tsmiVis" + p.id());
            item.addActionListener(_ -> p.open(this));
            this.visualizerMenu.add(item);
        }
        //
        // tsmiConsole
        //
        this.tsmiConsole.setIcon(new ImageIcon(Common.getImage("empty")));
        this.tsmiConsole.setName("tsmiConsole");
        this.tsmiConsole.setText("Console");
        this.tsmiConsole.addActionListener(_ -> openConsole());
        //
        // opeButtonSetting
        //
//        this.opeButtonSetting.AllowDrop = true;
        new DropTarget(this.opeButtonSetting, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonSetting.setBackground(Color.black);
        this.opeButtonSetting.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonSetting.setName("opeButtonSetting");
        this.opeButtonSetting.setActionCommand("0");
        this.opeButtonSetting.setToolTipText(rb.getString("opeButtonSetting.ToolTip"));
        this.opeButtonSetting.addActionListener(ev -> openSetting());
        this.opeButtonSetting.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonStop
        //
        new DropTarget(this.opeButtonStop, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonStop.setBackground(Color.black);
        this.opeButtonStop.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonStop.setName("opeButtonStop");
        this.opeButtonStop.setActionCommand("1");
        this.opeButtonStop.addActionListener(this::tsmiStop_Click);
        this.opeButtonStop.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonPause
        //
        new DropTarget(this.opeButtonPause, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonPause.setBackground(Color.black);
        this.opeButtonPause.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonPause.setName("opeButtonPause");
        this.opeButtonPause.setActionCommand("2");
//        this.opeButtonPause.setToolTipText(Resources.getResourceManager().getString("opeButtonPause.ToolTip"));
        this.opeButtonPause.addActionListener(_ -> pause());
        this.opeButtonPause.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonFadeout
        //
        new DropTarget(this.opeButtonFadeout, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonFadeout.setBackground(Color.black);
        this.opeButtonFadeout.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonFadeout.setName("opeButtonFadeout");
        this.opeButtonFadeout.setActionCommand("3");
        this.opeButtonFadeout.addActionListener(_ -> tsmiFadeOut_Click(null));
        this.opeButtonFadeout.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonPrevious
        //
        new DropTarget(this.opeButtonPrevious, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonPrevious.setBackground(Color.black);
        this.opeButtonPrevious.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonPrevious.setName("opeButtonPrevious");
        this.opeButtonPrevious.setActionCommand("4");
        this.opeButtonPrevious.addActionListener(this::opeButtonPrevious_Click);
        this.opeButtonPrevious.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonSlow
        //
        new DropTarget(this.opeButtonSlow, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonSlow.setBackground(Color.black);
        this.opeButtonSlow.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonSlow.setName("opeButtonSlow");
        this.opeButtonSlow.setActionCommand("5");
//        this.opeButtonSlow.setToolTipText(Resources.getResourceManager().getString("opeButtonSlow.ToolTip"));
        this.opeButtonSlow.addActionListener(ev -> slow());
        this.opeButtonSlow.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonPlay
        //
        new DropTarget(this.opeButtonPlay, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonPlay.setBackground(Color.black);
        this.opeButtonPlay.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonPlay.setName("opeButtonPlay");
        this.opeButtonPlay.setActionCommand("6");
//        this.opeButtonPlay.setToolTipText(Resources.getResourceManager().getString("opeButtonPlay.ToolTip"));
        this.opeButtonPlay.addActionListener(this::tsmiPlay_Click);
        this.opeButtonPlay.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonFast
        //
        new DropTarget(this.opeButtonFast, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonFast.setBackground(Color.black);
        this.opeButtonFast.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonFast.setName("opeButtonFast");
        this.opeButtonFast.setActionCommand("7");
//        this.opeButtonFast.setToolTipText(Resources.getResourceManager().getString("opeButtonFast.ToolTip"));
        this.opeButtonFast.addActionListener(_ -> ff());
        this.opeButtonFast.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonNext
        //
        new DropTarget(this.opeButtonNext, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonNext.setBackground(Color.black);
        this.opeButtonNext.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonNext.setName("opeButtonNext");
        this.opeButtonNext.setActionCommand("8");
//        this.opeButtonNext.setToolTipText(Resources.getResourceManager().getString("opeButtonNext.ToolTip"));
        this.opeButtonNext.addActionListener(_ -> {
            next();
            oldParam = new ScreenParams();
        });
        this.opeButtonNext.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonZoom
        //
        new DropTarget(this.opeButtonZoom, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonZoom.setBackground(Color.black);
        this.opeButtonZoom.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonZoom.setName("opeButtonZoom");
        this.opeButtonZoom.setActionCommand("17");
//        this.opeButtonZoom.setToolTipText(Resources.getResourceManager().getString("opeButtonZoom.ToolTip"));
        this.opeButtonZoom.addActionListener(_ -> tsmiChangeZoom_Click(null));
        this.opeButtonZoom.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonMIDIKBD
        //
        new DropTarget(this.opeButtonMIDIKBD, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonMIDIKBD.setBackground(Color.black);
        this.opeButtonMIDIKBD.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonMIDIKBD.setName("opeButtonMIDIKBD");
        this.opeButtonMIDIKBD.setActionCommand("16");
//        this.opeButtonMIDIKBD.setToolTipText(Resources.getResourceManager().getString("opeButtonMIDIKBD.ToolTip"));
        this.opeButtonMIDIKBD.addActionListener(_ -> openMIDIKeyboard());
        this.opeButtonMIDIKBD.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonVST
        //
        new DropTarget(this.opeButtonVST, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonVST.setBackground(Color.black);
        this.opeButtonVST.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonVST.setName("opeButtonVST");
        this.opeButtonVST.setActionCommand("15");
//        this.opeButtonVST.setToolTipText(Resources.getResourceManager().getString("opeButtonVST.ToolTip"));
        this.opeButtonVST.addActionListener(_ -> dispVSTList());
        this.opeButtonVST.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonKBD
        //
        new DropTarget(this.opeButtonKBD, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonKBD.setBackground(Color.black);
        this.opeButtonKBD.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonKBD.setName("opeButtonKBD");
        this.opeButtonKBD.setActionCommand("14");
//        this.opeButtonKBD.setToolTipText(Resources.getResourceManager().getString("opeButtonKBD.ToolTip"));
        this.opeButtonKBD.addActionListener(_ -> showContextMenu());
        this.opeButtonKBD.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonMixer
        //
        new DropTarget(this.opeButtonMixer, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonMixer.setBackground(Color.black);
        this.opeButtonMixer.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonMixer.setName("opeButtonMixer");
        this.opeButtonMixer.setActionCommand("13");
//        this.opeButtonMixer.setToolTipText(Resources.getResourceManager().getString("opeButtonMixer.ToolTip"));
        this.opeButtonMixer.addActionListener(_ -> openMixer());
        this.opeButtonMixer.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonInformation
        //
        new DropTarget(this.opeButtonInformation, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonInformation.setBackground(Color.black);
        this.opeButtonInformation.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonInformation.setName("opeButtonInformation");
        this.opeButtonInformation.setActionCommand("12");
//        this.opeButtonInformation.setToolTipText(Resources.getResourceManager().getString("opeButtonInformation.ToolTip"));
        this.opeButtonInformation.addActionListener(_ -> openInfo());
        this.opeButtonInformation.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonPlayList
        //
        new DropTarget(this.opeButtonPlayList, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonPlayList.setBackground(Color.black);
        this.opeButtonPlayList.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonPlayList.setName("opeButtonPlayList");
        this.opeButtonPlayList.setActionCommand("11");
//        this.opeButtonPlayList.setToolTipText(Resources.getResourceManager().getString("opeButtonPlayList.ToolTip"));
        this.opeButtonPlayList.addActionListener(_ -> dispPlayList());
        this.opeButtonPlayList.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonOpen
        //
        new DropTarget(this.opeButtonOpen, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonOpen.setBackground(Color.black);
        this.opeButtonOpen.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonOpen.setName("opeButtonOpen");
        this.opeButtonOpen.setActionCommand("10");
//        this.opeButtonOpen.setToolTipText(Resources.getResourceManager().getString("opeButtonOpen.ToolTip"));
        this.opeButtonOpen.addActionListener(this::tsmiOpenFile_Click);
        this.opeButtonOpen.addMouseListener(this.opeButton_Mouse);
        //
        // opeButtonMode
        //
        new DropTarget(this.opeButtonMode, DnDConstants.ACTION_COPY_OR_MOVE, new Common.DTListener(this::pbScreen_DragDrop), true);
        this.opeButtonMode.setBackground(Color.black);
        this.opeButtonMode.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.opeButtonMode.setName("opeButtonMode");
        this.opeButtonMode.setActionCommand("9");
//        this.opeButtonMode.setToolTipText(Resources.getResourceManager().getString("opeButtonMode.ToolTip"));
        this.opeButtonMode.addActionListener(this::opeButtonMode_Click);
        this.opeButtonMode.addMouseListener(this.opeButton_Mouse);
        //
        // keyboardHook1
        //
        this.keyboardHook1.addKeyboardHooked(this.keyboardHook1_KeyboardHooked);
        //
        // frmMain
        //
        JPanel main = new JPanel();
        main.setLayout(null);
        // the operation buttons sit on top of the skin at absolute pixel positions (see
        // RelocateOpeButton). They are children of pbScreen, not siblings of it: pbScreen is opaque
        // and repaints every frame, so a sibling drawn on top of it would be painted over each
        // time — as its children they are painted together with, and after, the skin.
        this.pbScreen.setLayout(null);
        this.pbScreen.add(this.opeButtonZoom);
        this.pbScreen.add(this.opeButtonMIDIKBD);
        this.pbScreen.add(this.opeButtonVST);
        this.pbScreen.add(this.opeButtonKBD);
        this.pbScreen.add(this.opeButtonMixer);
        this.pbScreen.add(this.opeButtonInformation);
        this.pbScreen.add(this.opeButtonPlayList);
        this.pbScreen.add(this.opeButtonOpen);
        this.pbScreen.add(this.opeButtonMode);
        this.pbScreen.add(this.opeButtonNext);
        this.pbScreen.add(this.opeButtonFast);
        this.pbScreen.add(this.opeButtonPlay);
        this.pbScreen.add(this.opeButtonSlow);
        this.pbScreen.add(this.opeButtonPrevious);
        this.pbScreen.add(this.opeButtonFadeout);
        this.pbScreen.add(this.opeButtonPause);
        this.pbScreen.add(this.opeButtonStop);
        this.pbScreen.add(this.opeButtonSetting);
        main.add(this.pbScreen);
        this.setContentPane(main);
        this.setName("frmMain");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
        // the window is sized to the skin by changeZoom()/pack() (which derives the frame size from
        // the content pane plus the title-bar inset); an explicit frame preferred size here would
        // instead make pack() use it verbatim and clip the skin, so it is deliberately not set.
        // Like the WinForms original (FixedSingle), the window is not user-resizable.
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // the menu items were named but never captioned — their text was in the .resx, and the
        // ApplyResources() calls that would have fetched it did not survive the port, so without
        // this every entry of both menus comes up blank
        Layouts.captions(this.cmsMenu, resources);
        Layouts.captions(this.cmsOpenOtherPanel, resources);

        this.pack();
    }

    private ScreenPanel pbScreen;
    private JPopupMenu cmsOpenOtherPanel;
    private JMenu primaryToolStripMenuItem;
    private JMenu sencondryToolStripMenuItem;
    private KeyboardHook keyboardHook1;
    private JPopupMenu cmsMenu;
    private JMenu fileToolStripMenuItem;
    private JMenuItem tsmiOpenFile;
    private JMenuItem tsmiExit;
    private JMenu operationToolStripMenuItem;
    private JMenuItem tsmiPlay;
    private JMenuItem tsmiStop;
    private JMenuItem tsmiPause;
    private JMenuItem tsmiFadeOut;
    private JMenuItem tsmiSlow;
    private JMenuItem tsmiFf;
    private JMenuItem tsmiNext;
    private JMenuItem tsmiPlayMode;
    private JMenuItem tsmiOption;
    private JMenuItem tsmiPlayList;
    private JMenuItem tsmiOpenInfo;
    private JMenuItem tsmiOpenMixer;
    private JMenu anotherWindowDisplayToolStripMenuItem;
    private JMenuItem tsmiKBrd;
    private JMenuItem tsmiVST;
    private JMenuItem tsmiMIDIkbd;
    private JMenu tsmiChangeZoom;
    private JMenu registerDumpDisplayToolStripMenuItem;
    private JMenuItem tsmiChangeZoomX1;
    private JMenuItem tsmiChangeZoomX2;
    private JMenuItem tsmiChangeZoomX3;
    private JMenuItem tsmiChangeZoomX4;

    private JButton opeButtonSetting;
    private JToolTip toolTip1;
    private JButton opeButtonStop;
    private JButton opeButtonPause;
    private JButton opeButtonFadeout;
    private JButton opeButtonPrevious;
    private JButton opeButtonSlow;
    private JButton opeButtonPlay;
    private JButton opeButtonFast;
    private JButton opeButtonNext;
    private JButton opeButtonZoom;
    private JButton opeButtonMIDIKBD;
    private JButton opeButtonVST;
    private JButton opeButtonKBD;
    private JButton opeButtonMixer;
    private JButton opeButtonInformation;
    private JButton opeButtonPlayList;
    private JButton opeButtonOpen;
    private JButton opeButtonMode;
    private JMenu visualizerMenu;
    private JMenuItem tsmiConsole;
    private FormConsole frmConsole;

    private int[] getChipStatus() {
        int[] chips = new int[256];
/* TODO led
        chips[0] = audio.plugin.chipLED.get("PriOPN");
        audio.plugin.chipLED.put("PriOPN", audio.plugin.chipLED.get("PriOPN"));
        chips[1] = audio.plugin.chipLED.get("PriOPN2");
        audio.plugin.chipLED.put("PriOPN2", audio.plugin.chipLED.get("PriOPN2"));
        chips[2] = audio.plugin.chipLED.get("PriOPNA");
        audio.plugin.chipLED.put("PriOPNA", audio.plugin.chipLED.get("PriOPNA"));
        chips[3] = audio.plugin.chipLED.get("PriOPNB");
        audio.plugin.chipLED.put("PriOPNB", audio.plugin.chipLED.get("PriOPNB"));

        chips[4] = audio.plugin.chipLED.get("PriOPM");
        audio.plugin.chipLED.put("PriOPM", audio.plugin.chipLED.get("PriOPM"));
        chips[5] = audio.plugin.chipLED.get("PriDCSG");
        audio.plugin.chipLED.put("PriDCSG", audio.plugin.chipLED.get("PriDCSG"));
        chips[6] = audio.plugin.chipLED.get("PriRF5C");
        audio.plugin.chipLED.put("PriRF5C", audio.plugin.chipLED.get("PriRF5C"));
        chips[7] = audio.plugin.chipLED.get("PriPWM");
        audio.plugin.chipLED.put("PriPWM", audio.plugin.chipLED.get("PriPWM"));

        chips[8] = audio.plugin.chipLED.get("PriOKI5");
        audio.plugin.chipLED.put("PriOKI5", audio.plugin.chipLED.get("PriOKI5"));
        chips[9] = audio.plugin.chipLED.get("PriOKI9");
        audio.plugin.chipLED.put("PriOKI9", audio.plugin.chipLED.get("PriOKI9"));
        chips[10] = audio.plugin.chipLED.get("PriC140");
        audio.plugin.chipLED.put("PriC140", audio.plugin.chipLED.get("PriC140"));
        chips[11] = audio.plugin.chipLED.get("PriSPCM");
        audio.plugin.chipLED.put("PriSPCM", audio.plugin.chipLED.get("PriSPCM"));

        chips[12] = audio.plugin.chipLED.get("PriAY10");
        audio.plugin.chipLED.put("PriAY10", audio.plugin.chipLED.get("PriAY10"));
        chips[13] = audio.plugin.chipLED.get("PriOPLL");
        audio.plugin.chipLED.put("PriOPLL", audio.plugin.chipLED.get("PriOPLL"));
        chips[14] = audio.plugin.chipLED.get("PriHuC");
        audio.plugin.chipLED.put("PriHuC", audio.plugin.chipLED.get("PriHuC"));
        chips[15] = audio.plugin.chipLED.get("PriC352");
        audio.plugin.chipLED.put("PriC352", audio.plugin.chipLED.get("PriC352"));
        chips[16] = audio.plugin.chipLED.get("PriK054539");
        audio.plugin.chipLED.put("PriK054539", audio.plugin.chipLED.get("PriK054539"));
        chips[17] = audio.plugin.chipLED.get("PriRF5C68");
        audio.plugin.chipLED.put("PriRF5C68", audio.plugin.chipLED.get("PriRF5C68"));

        chips[128 + 0] = audio.plugin.chipLED.get("SecOPN");
        audio.plugin.chipLED.put("SecOPN", audio.plugin.chipLED.get("SecOPN"));
        chips[128 + 1] = audio.plugin.chipLED.get("SecOPN2");
        audio.plugin.chipLED.put("SecOPN2", audio.plugin.chipLED.get("SecOPN2"));
        chips[128 + 2] = audio.plugin.chipLED.get("SecOPNA");
        audio.plugin.chipLED.put("SecOPNA", audio.plugin.chipLED.get("SecOPNA"));
        chips[128 + 3] = audio.plugin.chipLED.get("SecOPNB");
        audio.plugin.chipLED.put("SecOPNB", audio.plugin.chipLED.get("SecOPNB"));

        chips[128 + 4] = audio.plugin.chipLED.get("SecOPM");
        audio.plugin.chipLED.put("SecOPM", audio.plugin.chipLED.get("SecOPM"));
        chips[128 + 5] = audio.plugin.chipLED.get("SecDCSG");
        audio.plugin.chipLED.put("SecDCSG", audio.plugin.chipLED.get("SecDCSG"));
        chips[128 + 6] = audio.plugin.chipLED.get("SecRF5C");
        audio.plugin.chipLED.put("SecRF5C", audio.plugin.chipLED.get("SecRF5C"));
        chips[128 + 7] = audio.plugin.chipLED.get("SecPWM");
        audio.plugin.chipLED.put("SecPWM", audio.plugin.chipLED.get("SecPWM"));

        chips[128 + 8] = audio.plugin.chipLED.get("SecOKI5");
        audio.plugin.chipLED.put("SecOKI5", audio.plugin.chipLED.get("SecOKI5"));
        chips[128 + 9] = audio.plugin.chipLED.get("SecOKI9");
        audio.plugin.chipLED.put("SecOKI9", audio.plugin.chipLED.get("SecOKI9"));
        chips[128 + 10] = audio.plugin.chipLED.get("SecC140");
        audio.plugin.chipLED.put("SecC140", audio.plugin.chipLED.get("SecC140"));
        chips[128 + 11] = audio.plugin.chipLED.get("SecSPCM");
        audio.plugin.chipLED.put("SecSPCM", audio.plugin.chipLED.get("SecSPCM"));

        chips[128 + 12] = audio.plugin.chipLED.get("SecAY10");
        audio.plugin.chipLED.put("SecAY10", audio.plugin.chipLED.get("SecAY10"));
        chips[128 + 13] = audio.plugin.chipLED.get("SecOPLL");
        audio.plugin.chipLED.put("SecOPLL", audio.plugin.chipLED.get("SecOPLL"));
        chips[128 + 14] = audio.plugin.chipLED.get("SecHuC");
        audio.plugin.chipLED.put("SecHuC", audio.plugin.chipLED.get("SecHuC"));
        chips[128 + 15] = audio.plugin.chipLED.get("SecC352");
        audio.plugin.chipLED.put("SecC352", audio.plugin.chipLED.get("SecC352"));
        chips[128 + 16] = audio.plugin.chipLED.get("SecK054539");
        audio.plugin.chipLED.put("SecK054539", audio.plugin.chipLED.get("SecK054539"));
        chips[128 + 17] = audio.plugin.chipLED.get("SecRF5C68");
        audio.plugin.chipLED.put("SecRF5C68", audio.plugin.chipLED.get("SecRF5C68"));
*/
        return chips;
    }

    private final Object lockObj = new Object();
    private boolean _fatalError = false;

    private boolean getFatalError() {
        synchronized (lockObj) {
            return _fatalError;
        }
    }

    private void setFatalError(boolean value) {
        synchronized (lockObj) {
            _fatalError = value;
        }
    }

//#region double buffer

    private void refreshScreen() {
        try {
            if (mainScreen != null) {
                mainScreen.refresh(null);
            }
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }

    private void closeScreen() {
        if (mainScreen != null) {
            mainScreen.remove(null);
            mainScreen = null;
        }
    }

//#endregion

//#region draw buffer

    private static void drawFaderH(FrameBuffer screen, int x, int y, int d, int v, int val1, int val2, int[] od, int[] ov, int[] oval1, int[] oval2) {
        if (d == od[0] && v == ov[0] && val1 == oval1[0] && val2 == oval2[0]) {
            return;
        }

        od[0] = d;
        ov[0] = v;
        oval1[0] = val1;
        oval2[0] = val2;

        drawFaderHP(screen, x, y, 2, v);
        for (int i = 0; i < 7 * 8 + 1; i++) {
            drawFaderHP(screen, x + 1 + i, y, (i < val2 ? (v == 0 ? 4 : 5) : 3), v);
        }
        drawFaderHP(screen, x + 2 + 7 * 8, y, 2, v);

        drawFaderHP(screen, x + val1 + 1, y, d, v);
    }

    private static void drawFaderHP(FrameBuffer screen, int x, int y, int c, int v) {
        c += v * 6;
        switch (c) {
            case 0:
                screen.drawByteArray(x - 1, y, FrameBuffer.rFaderH, 32, 0, 0, 3, 6);
                break;
            case 1:
                screen.drawByteArray(x - 1, y, FrameBuffer.rFaderH, 32, 3, 0, 3, 6);
                break;
            case 2:
                screen.drawByteArray(x, y, FrameBuffer.rFaderH, 32, 6, 0, 1, 6);
                break;
            case 3:
                screen.drawByteArray(x, y, FrameBuffer.rFaderH, 32, 7, 0, 1, 6);
                break;
            case 4:
                screen.drawByteArray(x, y, FrameBuffer.rFaderH, 32, 8, 0, 1, 6);
                break;
            case 5:
                screen.drawByteArray(x, y, FrameBuffer.rFaderH, 32, 9, 0, 1, 6);
                break;
            case 6:
                screen.drawByteArray(x - 1, y, FrameBuffer.rFaderH, 32, 0, 8, 3, 6);
                break;
            case 7:
                screen.drawByteArray(x - 1, y, FrameBuffer.rFaderH, 32, 3, 8, 3, 6);
                break;
            case 8:
                screen.drawByteArray(x, y, FrameBuffer.rFaderH, 32, 6, 8, 1, 6);
                break;
            case 9:
                screen.drawByteArray(x, y, FrameBuffer.rFaderH, 32, 7, 8, 1, 6);
                break;
            case 10:
                screen.drawByteArray(x, y, FrameBuffer.rFaderH, 32, 8, 8, 1, 6);
                break;
            case 11:
                screen.drawByteArray(x, y, FrameBuffer.rFaderH, 32, 9, 8, 1, 6);
                break;
        }
    }

    private static void drawTimer(FrameBuffer screen, int c, int ot1, int ot2, int ot3, int nt1, int nt2, int nt3) {
        if (ot1 != nt1) {
            //drawFont4Int2(mainScreen, 4 * 30 + c * 4 * 11, 0, 0, 3, nt1);
            drawFont8Int2(screen, 8 * 5 - 16 + c * 8 * 11 + 1, 1, 0, 3, nt1);
        }
        if (ot2 != nt2) {
            drawFont8Int2(screen, 8 * 9 - 16 + c * 8 * 11 + 1, 1, 0, 2, nt2);
            //drawFont4Int2(mainScreen, 4 * 34 + c * 4 * 11, 0, 0, 2, nt2);
        }
        if (ot3 != nt3) {
            drawFont8Int2(screen, 8 * 12 - 16 + c * 8 * 11 + 1, 1, 0, 2, nt3);
            //drawFont4Int2(mainScreen, 4 * 37 + c * 4 * 11, 0, 0, 2, nt3);
        }
    }

    private static void drawFont8Int2(FrameBuffer screen, int x, int y, int t, int k, int num) {
        if (screen == null)
            return;

        int n;
        if (k == 3) {
            n = num / 100;
            num -= n * 100;

            n = (n > 9) ? 0 : n;
            if (n == 0)
                screen.drawByteArray(x, y, FrameBuffer.rFont1[t], 128, 0, 0, 8, 8);
            else
                screen.drawByteArray(x, y, FrameBuffer.rFont1[t], 128, 0, 8, 8, 8);

            n = num / 10;
            num -= n * 10;
            x += 8;
            screen.drawByteArray(x, y, FrameBuffer.rFont1[t], 128, n * 8, 8, 8, 8);

            n = num / 1;
            x += 8;
            screen.drawByteArray(x, y, FrameBuffer.rFont1[t], 128, n * 8, 8, 8, 8);
            return;
        }

        n = num / 10;
        num -= n * 10;
        n = (n > 9) ? 0 : n;
        screen.drawByteArray(x, y, FrameBuffer.rFont1[t], 128, n * 8, 8, 8, 8);

        n = num / 1;
        x += 8;
        screen.drawByteArray(x, y, FrameBuffer.rFont1[t], 128, n * 8, 8, 8, 8);
    }

//#endregion
}
