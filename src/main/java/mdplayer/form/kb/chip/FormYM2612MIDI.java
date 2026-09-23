package mdplayer.form.kb.chip;

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
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import mdplayer.Common;
import mdplayer.Tables;
import mdplayer.YM2612MIDI;
import mdplayer.chips.MidiPlugin;
import mdplayer.form.FormBase;
import mdplayer.form.FrameBuffer;
import mdplayer.form.ScreenPanel;
import mdplayer.form.SettingTab;
import mdplayer.form.View;
import mdplayer.form.kb.ChannelParams;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdplayer.form.sys.FormTPGet;
import mdplayer.form.sys.FormTPPut;
import mdplayer.form.sys.setting.SettingMIDIKBDPanel;

import static java.lang.System.getLogger;


public class FormYM2612MIDI extends FormBase implements View {

    private static final Logger logger = getLogger(FormYM2612MIDI.class.getName());

    private boolean isClosed = false;
    private int x = -1;
    private int y = -1;

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void setDefaultLocation(int x, int y) {
        this.x = x;
        this.y = y;
        // there at once, not only once opened: a window closed before its windowOpened ran
        // would otherwise record the platform's default spot as where it was
        setLocation(x, y);
    }
    private int frameSizeW = 0;
    private int frameSizeH = 0;
    private int zoom = 1;

    private final YM2612MIDI.Params newParam;
    private final YM2612MIDI.Params oldParam = new YM2612MIDI.Params();
    private final FrameBuffer frameBuffer = new FrameBuffer();
    private boolean hasError = false;

    private static final Preferences prefs = Preferences.userNodeForPackage(FormYM2612MIDI.class);

    public FormYM2612MIDI(FormMain frm, int zoom, YM2612MIDI.Params newParam) {
        super(frm);
        this.zoom = zoom;

        initializeComponent();
        this.addMouseWheelListener(this::frmYM2612MIDI_MouseWheel);

        this.newParam = newParam;
        frameBuffer.add(pbScreen, Common.getImage("planeYM2612MIDI"), null, zoom);
        screenInitYM2612MIDI(frameBuffer);
        update();
    }

    @Override
    public void update() {
        try {
            frameBuffer.refresh(null);
        } catch (Exception e) {
            if (!hasError) {
                logger.log(Level.WARNING, "Error in MIDI keyboard update: " + e.getMessage());
                hasError = true;
            }
        }
    }

    protected boolean getShowWithoutActivation() {
        return true;
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("YM2612MIDI", 0, getLocation());
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
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeYM2612MIDI").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2612MIDI").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeYM2612MIDI").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2612MIDI").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeYM2612MIDI").getWidth() * zoom, frameSizeH + Common.getImage("planeYM2612MIDI").getHeight() * zoom));
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
        try {
            if (newParam == null || parent == null || parent.setting == null || parent.setting.getMidiKbd() == null) {
                return;
            }
            int[][] fmRegister = null;
            if (audio.plugin != null && audio.plugin.chipRegister != null) {
                MidiPlugin midiPlugin = audio.plugin.chipRegister.plugin(MidiPlugin.class);
                if (midiPlugin != null) {
                    fmRegister = midiPlugin.readYM2612();
                }
            }
            if (fmRegister == null) {
                fmRegister = new int[2][256];
            }

            newParam.IsMONO = parent.setting.getMidiKbd().isMono();
            if (parent.setting.getMidiKbd().isMono()) {
                for (int i = 0; i < 6; i++) {
                    newParam.useChannel[i] = (parent.setting.getMidiKbd().getUseMonoChannel() == i);
                }
            } else {
                for (int i = 0; i < 6; i++) {
                    newParam.useChannel[i] = parent.setting.getMidiKbd().getUseChannel()[i];
                }
            }

            newParam.useFormat = parent.setting.getMidiKbd().getUseFormat();

            for (int ch = 0; ch < 6; ch++) {
                int p = (ch > 2) ? 1 : 0;
                int c = (ch > 2) ? ch - 3 : ch;
                for (int i = 0; i < 4; i++) {
                    int ops = (i == 0) ? 0 : ((i == 1) ? 8 : ((i == 2) ? 4 : 12));
                    newParam.channels[ch].inst[i * 11 + 0] = fmRegister[p][0x50 + ops + c] & 0x1f; //AR
                    newParam.channels[ch].inst[i * 11 + 1] = fmRegister[p][0x60 + ops + c] & 0x1f; //DR
                    newParam.channels[ch].inst[i * 11 + 2] = fmRegister[p][0x70 + ops + c] & 0x1f; //SR
                    newParam.channels[ch].inst[i * 11 + 3] = fmRegister[p][0x80 + ops + c] & 0x0f; //RR
                    newParam.channels[ch].inst[i * 11 + 4] = (fmRegister[p][0x80 + ops + c] & 0xf0) >> 4;//SL
                    newParam.channels[ch].inst[i * 11 + 5] = fmRegister[p][0x40 + ops + c] & 0x7f;//TL
                    newParam.channels[ch].inst[i * 11 + 6] = (fmRegister[p][0x50 + ops + c] & 0xc0) >> 6;//KS
                    newParam.channels[ch].inst[i * 11 + 7] = fmRegister[p][0x30 + ops + c] & 0x0f;//ML
                    newParam.channels[ch].inst[i * 11 + 8] = (fmRegister[p][0x30 + ops + c] & 0x70) >> 4;//DT
                    newParam.channels[ch].inst[i * 11 + 9] = (fmRegister[p][0x60 + ops + c] & 0x80) >> 7;//AM
                    newParam.channels[ch].inst[i * 11 + 10] = fmRegister[p][0x90 + ops + c] & 0x0f;//SG
                }
                newParam.channels[ch].inst[44] = fmRegister[p][0xb0 + c] & 0x07;//AL
                newParam.channels[ch].inst[45] = (fmRegister[p][0xb0 + c] & 0x38) >> 3;//FB
                newParam.channels[ch].inst[46] = (fmRegister[p][0xb4 + c] & 0x38) >> 4;//AMS
                newParam.channels[ch].inst[47] = fmRegister[p][0xb4 + c] & 0x07;//FMS

                newParam.channels[ch].pan = (fmRegister[p][0xb4 + c] & 0xc0) >> 6;

                if (newParam.selectCh != -1 && newParam.selectParam != -1) {
                    if (oldParam.selectCh != -1 && oldParam.selectParam != -1) {
                        newParam.channels[oldParam.selectCh].typ[oldParam.selectParam] = 0;
                    }
                    newParam.channels[newParam.selectCh].typ[newParam.selectParam] = 1;
                    oldParam.selectCh = newParam.selectCh;
                    oldParam.selectParam = newParam.selectParam;
                }

                //int freq = 0;
                //int octav = 0;
                //int n = -1;
                //freq = register[p][0xa0 + c] + (register[p][0xa4 + c] & 0x07) * 0x100;
                //octav = (register[p][0xa4 + c] & 0x38) >> 3;

                //if (fmKey[ch] > 0) n = Math.min(Math.max(octav * 12 + searchFMNote(freq), 0), 95);

                //newParam.channels[ch].volumeL = Math.min(Math.max(fmVol[ch][0] / 80, 0), 19);
                //newParam.channels[ch].volumeR = Math.min(Math.max(fmVol[ch][1] / 80, 0), 19);
                //newParam.channels[ch].note = n;
            }
        } catch (Exception e) {
            if (!hasError) {
                logger.log(Level.WARNING, "Error in MIDI keyboard parameters: " + e.getMessage(), e);
                hasError = true;
            }
        }
    }

    @Override
    public void drawScreenParams() {
        try {
            if (newParam == null || oldParam == null || parent == null || parent.setting == null
                    || parent.setting.getYM2612Type() == null || parent.setting.getYM2612Type().length == 0
                    || parent.setting.getYM2612Type()[0] == null
                    || parent.setting.getYM2612Type()[0].getUseReal() == null
                    || parent.setting.getYM2612Type()[0].getUseReal().length == 0) {
                return;
            }
            for (int c = 0; c < 6; c++) {

                ChannelParams oyc = oldParam.channels[c];
                ChannelParams nyc = newParam.channels[c];

                boolean YM2612type = parent.setting.getYM2612Type()[0].getUseReal()[0];
                int tp = YM2612type ? 1 : 0;

                frameBuffer.drawInst(1, 6 + (c > 2 ? 3 : 0), c, oyc.inst, nyc.inst, oyc.typ, nyc.typ);

                int[] onl = oldParam.noteLog[c];
                int[] nnl = newParam.noteLog[c];

                for (int n = 0; n < 10; n++) {
                    NoteLogYM2612MIDI(frameBuffer, (c % 3) * 13 * 8 + 2 * 8 + n * 8, (c / 3) * 18 * 4 + 24 * 4, onl[n], nnl[n]);
                }

                UseChannelYM2612MIDI(frameBuffer, (c % 3) * 13 * 8, (c / 3) * 9 * 8 + 4 * 8, oldParam.useChannel[c], newParam.useChannel[c]);
            }

            MONOPOLYYM2612MIDI(frameBuffer, oldParam.IsMONO, newParam.IsMONO);

            oldParam.lfoSw = frameBuffer.drawLfoSw(16, 176, oldParam.lfoSw, newParam.lfoSw);
            oldParam.lfoFrq = frameBuffer.drawLfoFrq(64, 176, oldParam.lfoFrq, newParam.lfoFrq);
            ToneFormat(frameBuffer, 16, 6, oldParam.useFormat, newParam.useFormat);
        } catch (Exception e) {
            if (!hasError) {
                logger.log(Level.WARNING, "Error in MIDI keyboard drawing: " + e.getMessage());
                hasError = true;
            }
        }
    }

    private final MouseListener pbScreen_MouseClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
            int px = ev.getX() / parent.setting.getOther().getZoom();
            int py = ev.getY() / parent.setting.getOther().getZoom();

            // Top label
            if (py < 8) return;

            if (py < 16) {
                //logger.log(Level.TRACE, "Keyboard");
            } else if (py < 32) {
                //logger.log(Level.TRACE, "Each Function Menu");
                int u = (py - 16) / 8;
                int p = -1;
                if (px >= 1 * 8 && px < 6 * 8) p = 0;
                else if (px >= 8 * 8 && px < 14 * 8) p = 1;
                else if (px >= 15 * 8 && px < 20 * 8) p = 2;
                else if (px >= 23 * 8 && px < 29 * 8) p = 3;
                else if (px >= 32 * 8 && px < 38 * 8) p = 4;

                if (p == -1) return;

                switch (u * 5 + p) {
                    case 0:
                        //logger.log(Level.TRACE, "MONO");
//                        cmdSetMode(0);
                        break;
                    case 1:
                        break;
                    case 2:
                        //logger.log(Level.TRACE, "PANIC");
//                        cmdAllNoteOff();
                        break;
                    case 3:
                        //logger.log(Level.TRACE, "TP.PUT");
                        cmdTPPut();
                        break;
                    case 4:
                        //logger.log(Level.TRACE, "T.LOAD");
                        cmdTLoad();
                        break;
                    case 5:
                        //logger.log(Level.TRACE, "POLY");
//                    cmdSetMode(1);
                        break;
                    case 6:
                        parent.setting.getMidiKbd().setUseFormat(parent.setting.getMidiKbd().getUseFormat() + 1);
                        if (parent.setting.getMidiKbd().getUseFormat() > 4) parent.setting.getMidiKbd().setUseFormat(0);
                        break;
                    case 7:
                        //logger.log(Level.TRACE, "L.CLS");
//                    cmdLogClear();
                        break;
                    case 8:
                        //logger.log(Level.TRACE, "TP.GET");
                        cmdTPGet();
                        break;
                    case 9:
                        //logger.log(Level.TRACE, "T.SAVE");
                        cmdTSave();
                        break;
                }
            } else if (py < 40) {
                if ((px / 8) % 13 == 0) {
                    //logger.log(Level.TRACE, "Channel selection");
//                    cmdSelectChannel(px / 8 / 13);
                } else {
                    //logger.log(Level.TRACE, "Tone selection(1-3Ch)");
                    cmdSelectTone(px, py, ev);// / 8 / 13, e);
                }
            } else if (py < 80) {
                //logger.log(Level.TRACE, "Tone selection(1-3Ch)");
                cmdSelectTone(px, py, ev);
            } else if (py < 104) {
                if (py < 88 && (px / 8) % 13 == 3) {
                    //logger.log(Level.TRACE, "Log Clear");
//                    cmdLogClear(px / 8 / 13);
                } else {
                    //logger.log(Level.TRACE, "Log -> MML conversion(1-3Ch)");
//                    cmdLog2MML(px / 8 / 13);
                }
            } else if (py < 112) {
                if ((px / 8) % 13 == 0) {
                    //logger.log(Level.TRACE, "Channel selection");
//                    cmdSelectChannel((px / 8 / 13) + 3);
                } else {
                    //logger.log(Level.TRACE, "Tone selection(4-6Ch)");
                    cmdSelectTone(px, py, ev);
                }
            } else if (py < 152) {
                //logger.log(Level.TRACE, "Tone selection(4-6Ch)");
                cmdSelectTone(px, py, ev);
            } else if (py < 176) {
                if (py < 160 && (px / 8) % 13 == 3) {
                    //logger.log(Level.TRACE, "Log Clear");
//                    cmdLogClear((px / 8 / 13) + 3);
                } else {
                    //logger.log(Level.TRACE, "Log -> MML conversion(4-6Ch)");
//                    cmdLog2MML((px / 8 / 13) + 3);
                }
            }
        }
    };

//    /**
//     * MONO/POLY
//     */
//    private void cmdSetMode(int m) {
//        parent.ym2612Midi_SetMode(m);
//    }
//
//    /**
//     * PANIC
//     */
//    private void cmdAllNoteOff() {
//        parent.ym2612Midi_AllNoteOff();
//    }
//
//    /**
//     * L.CLS
//     */
//    private void cmdLogClear() {
//        parent.ym2612Midi_ClearNoteLog();
//    }
//
//    /**
//     * LogClear
//     */
//    private void cmdLogClear(int ch) {
//        parent.ym2612Midi_ClearNoteLog(ch);
//    }
//
//    /**
//     * MML conversion
//     */
//    private void cmdLog2MML(int ch) {
//        parent.ym2612Midi_Log2MML(ch);
//    }
//
//    /**
//     *
//     */
//    private void cmdSelectChannel(int ch) {
//        parent.ym2612Midi_SelectChannel(ch);
//    }

    private void cmdTPPut() {
//        parent.ym2612Midi_SetTonesToSetting();
        FormTPPut frmTPPut = new FormTPPut();
        frmTPPut.ShowDialog(parent.setting, parent.tonePallet);
    }

    private void cmdTPGet() {
//        parent.ym2612Midi_SetTonesToSetting();
        FormTPGet frmTPGet = new FormTPGet();
        frmTPGet.ShowDialog(parent.setting, parent.tonePallet);
//        parent.ym2612Midi_SetTonesFromSetting();
    }

    private boolean isInitialOpenFolder = true;

    private static class MyFileFilter extends FileFilter {
        final String ext;
        final String desc;
        MyFileFilter(String ext, String desc) {
            this.ext = ext; this.desc = desc;
        }
        @Override public boolean accept(File f) {
            return f.getName().toLowerCase().endsWith(ext);
        }
        @Override public String getDescription() {
            return desc;
        }
    }

    private static final String[][] extDescs = {
        {".xml", "XML file(*.xml)"},
        {".gwi", "MML2VGM file(*.gwi)"},
        {".mwi", "FMP7 file(*.mwi)"},
        {".mml", "NRTDRV file(*.mml)"},
        {".mml", "MXDRV file(*.mml)"},
        {".mml", "MusicLALF file(*.mml)"},
    };

    private void cmdTSave() {
        JFileChooser sfd = new JFileChooser();
        Arrays.stream(extDescs).forEach(ed -> sfd.addChoosableFileFilter(new MyFileFilter(ed[0], ed[1])));
        sfd.setDialogTitle("Save TonePallet files");
        if (!parent.setting.getOther().getDefaultDataPath().isEmpty() && Files.exists(Path.of(parent.setting.getOther().getDefaultDataPath())) && isInitialOpenFolder) {
            sfd.setCurrentDirectory(new File(parent.setting.getOther().getDefaultDataPath()));
        } else {
//            sfd.testoreDirectory = true;
        }
//        sfd.checkPathExists = true;

        if (sfd.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        isInitialOpenFolder = false;

        try {
//            parent.ym2612Midi_SaveTonePallet(sfd.getSelectedFile().getPath(), Common.getFilterIndex(sfd) + 1);
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            JOptionPane.showMessageDialog(null, "File saving failed.");
        }
    }

    private void cmdTLoad() {
        JFileChooser ofd = new JFileChooser();
        Arrays.stream(extDescs).forEach(ed -> ofd.addChoosableFileFilter(new MyFileFilter(ed[0], ed[1])));
        ofd.setDialogTitle("Read TonePallet file");
        if (!parent.setting.getOther().getDefaultDataPath().isEmpty() && Files.exists(Path.of(parent.setting.getOther().getDefaultDataPath())) && isInitialOpenFolder) {
            ofd.setCurrentDirectory(new File(parent.setting.getOther().getDefaultDataPath()));
        } else {
//            ofd.restoreDirectory = true;
        }
//        ofd.checkPathExists = true;

        if (ofd.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        isInitialOpenFolder = false;

        try {
//            parent.ym2612Midi_LoadTonePallet(ofd.getSelectedFile().getPath(), Common.getFilterIndex(ofd) + 1);
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            JOptionPane.showMessageDialog(null, "Failed to load file.");
        }
    }

    private void cmdSelectTone(int px, int py, MouseEvent e) {
        int ch = px / 8 / 13 + (py < 104 ? 0 : 3);
        int row = -1;
        int col = 0;
        int n = -1;

        if (e.getButton() != MouseEvent.BUTTON2) {
            px %= 8 * 13;
            py = py >= 104 ? (py - 104) : (py - 32);
            col = px / 4;

            if (py < 8) {
                row = 0;
                n = switch (col) {
                    case 10, 11 -> 44;
                    case 14, 15 -> 45;
                    case 19, 20 -> 46;
                    case 24, 25 -> 47;
                    default -> n;
                };
            } else if (py < 16) {
                return;
            } else if (py < 48) {
                row = (py - 16) / 8 + 1;
                if (col < 12) {
                    n = col / 2;
                    if (n < 1) return;
                    n--;
                } else if (col < 15) {
                    n = 5;
                } else if (col < 25) {
                    n = (col + 1) / 2;
                    n -= 2;
                } else {
                    return;
                }
                n += (row - 1) * 11;
            }

            //logger.log(Level.TRACE, "row=%d col=%d ch=%d n=%d".formatted(row, col, ch, n));
//            parent.ym2612Midi_SetSelectInstParam(ch, n);
            return;
        }

        cmsMIDIKBD.setActionCommand(String.valueOf(ch));
        cmsMIDIKBD.setLocation(e.getX(), e.getY());
        cmsMIDIKBD.setVisible(true);
    }

    private final KeyListener frmYM2612MIDI_KeyDown = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.isControlDown()) {
                if (e.getKeyCode() == KeyEvent.VK_C) {
//                    parent.ym2612Midi_CopyToneToClipboard();
                } else if (e.getKeyCode() == KeyEvent.VK_V) {
//                    parent.ym2612Midi_PasteToneFromClipboard();
                }
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
//                parent.ym2612Midi_AddSelectInstParam(1);
            } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
//                parent.ym2612Midi_AddSelectInstParam(11);
            }
        }
    };

    private void ctsmiCopy_Click(ActionEvent ev) {
//        parent.ym2612Midi_CopyToneToClipboard(Integer.parseInt(cmsMIDIKBD.getActionCommand()));
    }

    private void ctsmiPaste_Click(ActionEvent ev) {
//        parent.ym2612Midi_PasteToneFromClipboard(Integer.parseInt(cmsMIDIKBD.getActionCommand()));
    }

    private void frmYM2612MIDI_MouseWheel(MouseWheelEvent ev) {
//        parent.ym2612Midi_ChangeSelectedParamValue((int) Math.signum(ev.getScrollAmount()));
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();
        this.cmsMIDIKBD = new JMenu();
        this.ctsmiCopy = new JMenuItem();
        this.ctsmiPaste = new JMenuItem();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeYM2612MIDI");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(320, 184));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // cmsMIDIKBD
        //
        this.cmsMIDIKBD.add(this.ctsmiCopy);
        this.cmsMIDIKBD.add(this.ctsmiPaste);
        this.cmsMIDIKBD.setName("cmsMIDIKBD");
        this.cmsMIDIKBD.setPreferredSize(new Dimension(131, 48));
        //
        // ctsmiCopy
        //
        this.ctsmiCopy.setName("ctsmiCopy");
        this.ctsmiCopy.setPreferredSize(new Dimension(130, 22));
        this.ctsmiCopy.setText("Copy (&C)");
        this.ctsmiCopy.addActionListener(this::ctsmiCopy_Click);
        //
        // ctsmiPaste
        //
        this.ctsmiPaste.setName("ctsmiPaste");
        this.ctsmiPaste.setPreferredSize(new Dimension(130, 22));
        this.ctsmiPaste.setText("Paste (&p)");
        this.ctsmiPaste.addActionListener(this::ctsmiPaste_Click);
        //
        // frmYM2612MIDI
        //
        this.setPreferredSize(new Dimension(320, 184));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmYM2612MIDI");
        this.setTitle("MIDI(Ym2612Inst)");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
        this.addKeyListener(this.frmYM2612MIDI_KeyDown);
    }

    private BufferedImage image;
    private ScreenPanel pbScreen;
    private JMenu cmsMIDIKBD;
    private JMenuItem ctsmiCopy;
    private JMenuItem ctsmiPaste;

//#region draw buffer

    private static void screenInitYM2612MIDI(FrameBuffer screen) {
        if (screen == null)
            return;

        for (int c = 0; c < 6; c++) {
            for (int n = 0; n < 10; n++) {
                drawFont4V(screen, (c % 3) * 13 * 8 + 2 * 8 + n * 8, (c / 3) * 18 * 4 + 24 * 4, 0, "   ");
            }
        }
    }

    private static void NoteLogYM2612MIDI(FrameBuffer screen, int x, int y, int oln, int nln) {
        if (oln == nln)
            return;
        if (nln == -1) {
            drawFont4V(screen, x, y, 0, "   ");
        } else {
            drawFont4V(screen, x, y, 0, Tables.kbnp[nln % 12]);
            drawFont4V(screen, x, y - 2 * 4, 0, Tables.kbo[nln / 12]);
        }
        oln = nln;
    }

    private static void UseChannelYM2612MIDI(FrameBuffer screen, int x, int y, boolean olm, boolean nlm) {
        // if (olm == nlm) return;

        screen.drawFont8(x, y, 1, nlm ? "^" : "-");

        olm = nlm;
    }

    private static void MONOPOLYYM2612MIDI(FrameBuffer screen, boolean olm, boolean nlm) {
        if (olm == nlm)
            return;

        screen.drawFont8(8, 16, 1, nlm ? "^" : "-");
        screen.drawFont8(8, 24, 1, nlm ? "-" : "^");

        olm = nlm;
    }

    private static void ToneFormat(FrameBuffer screen, int x, int y, int oToneFormat, int nToneFormat) {
        if (oToneFormat == nToneFormat) {
            return;
        }

        x *= 4;
        y *= 4;

        drawToneFormatP(screen, x, y, nToneFormat);

        oToneFormat = nToneFormat;
    }

    private static void drawFont4V(FrameBuffer screen, int x, int y, int t, String msg) {
        if (screen == null)
            return;

        for (char c : msg.toCharArray()) {
            int cd = c - 'A' + 0x20 + 1;
            screen.drawByteArray(x, y, FrameBuffer.rFont3[t], 128, (cd % 16) * 8, (cd / 16) * 4, 8, 4);
            y -= 4;
        }
    }

    private static void drawToneFormatP(FrameBuffer screen, int x, int y, int toneFormat) {
        screen.drawByteArray(x, y, FrameBuffer.rMenuButtons[1], 128, (toneFormat % 3) * 5 * 8, (6 + toneFormat / 3) * 8, 40, 8);
    }

//#endregion

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "YM2612MIDI"; }
        @Override public String category() { return "opn"; }
        @Override public boolean perChip() { return false; }
        @Override public boolean hasMenuItem() { return false; }
        @Override public Point defaultOffset() { return new Point(328, 0); }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormYM2612MIDI(frm, zoom, frm.ym2612MidiParams()); }
        @Override public List<SettingTab> settingTabs() { return List.of(new SettingMIDIKBDPanel()); }
    }
}
