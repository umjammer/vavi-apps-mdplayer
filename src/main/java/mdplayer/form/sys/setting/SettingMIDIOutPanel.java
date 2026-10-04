package mdplayer.form.sys.setting;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import mdplayer.ChipRegister;
import mdplayer.MidiOutInfo;
import mdplayer.Setting;
import mdplayer.chips.VstPlugin;
import mdplayer.form.Grid;
import mdplayer.form.SettingTab;
import mdplayer.form.sys.FormSetting;
import mdplayer.vst.VstFileChooser;
import mdplayer.vst.VstInfo;

import static java.lang.System.getLogger;

/** the "MIDIOut" page of the settings dialog, split out of the original FormSetting */
public class SettingMIDIOutPanel extends SettingTab {

    @Override
    public int order() {
        return 70;
    }

    private static final Logger logger = getLogger(SettingMIDIOutPanel.class.getName());

    private final JButton btnUP_A;
    private final JButton btnSubMIDIout;
    private final JButton btnDOWN_A;
    private final JButton btnAddMIDIout;
    private final JLabel label18;
    private final JTable dgvMIDIoutListA;
    private final JTable dgvMIDIoutPallet;
    private final JLabel label16;
    private final JTabbedPane tbcMIDIoutList;
    private final JPanel tabPage1;
    private final JPanel tabPage2;
    private final JPanel tabPage3;
    private final JPanel tabPage4;
    private final JButton btnUP_B;
    private final JButton btnDOWN_B;
    private final JButton btnUP_C;
    private final JButton btnDOWN_C;
    private final JButton btnUP_D;
    private final JButton btnDOWN_D;
    private final JPanel tabPage5;
    private final JButton btnUP_E;
    private final JButton btnDOWN_E;
    private final JPanel tabPage6;
    private final JButton btnUP_F;
    private final JButton btnDOWN_F;
    private final JPanel tabPage7;
    private final JButton btnUP_G;
    private final JButton btnDOWN_G;
    private final JPanel tabPage8;
    private final JButton btnUP_H;
    private final JButton btnDOWN_H;
    private final JPanel tabPage9;
    private final JButton btnUP_I;
    private final JButton btnDOWN_I;
    private final JPanel tabPage10;
    private final JButton button17;
    private final JButton btnDOWN_J;
    private final JButton btnAddVST;
    private final JTable dgvMIDIoutListB;
    private final JTable dgvMIDIoutListC;
    private final JTable dgvMIDIoutListD;
    private final JTable dgvMIDIoutListE;
    private final JTable dgvMIDIoutListF;
    private final JTable dgvMIDIoutListG;
    private final JTable dgvMIDIoutListH;
    private final JTable dgvMIDIoutListI;
    private final JTable dgvMIDIoutListJ;

    private final JTable[] dgv;

    public SettingMIDIOutPanel() {
        this.btnAddVST = new JButton();
        this.tbcMIDIoutList = new JTabbedPane();
        this.tabPage1 = new JPanel();
        this.dgvMIDIoutListA = new JTable();
        this.btnUP_A = new JButton();
        this.btnDOWN_A = new JButton();
        this.tabPage2 = new JPanel();
        this.dgvMIDIoutListB = new JTable();
        this.btnUP_B = new JButton();
        this.btnDOWN_B = new JButton();
        this.tabPage3 = new JPanel();
        this.dgvMIDIoutListC = new JTable();
        this.btnUP_C = new JButton();
        this.btnDOWN_C = new JButton();
        this.tabPage4 = new JPanel();
        this.dgvMIDIoutListD = new JTable();
        this.btnUP_D = new JButton();
        this.btnDOWN_D = new JButton();
        this.tabPage5 = new JPanel();
        this.dgvMIDIoutListE = new JTable();
        this.btnUP_E = new JButton();
        this.btnDOWN_E = new JButton();
        this.tabPage6 = new JPanel();
        this.dgvMIDIoutListF = new JTable();
        this.btnUP_F = new JButton();
        this.btnDOWN_F = new JButton();
        this.tabPage7 = new JPanel();
        this.dgvMIDIoutListG = new JTable();
        this.btnUP_G = new JButton();
        this.btnDOWN_G = new JButton();
        this.tabPage8 = new JPanel();
        this.dgvMIDIoutListH = new JTable();
        this.btnUP_H = new JButton();
        this.btnDOWN_H = new JButton();
        this.tabPage9 = new JPanel();
        this.dgvMIDIoutListI = new JTable();
        this.btnUP_I = new JButton();
        this.btnDOWN_I = new JButton();
        this.tabPage10 = new JPanel();
        this.dgvMIDIoutListJ = new JTable();
        this.button17 = new JButton();
        this.btnDOWN_J = new JButton();
        this.btnSubMIDIout = new JButton();
        this.btnAddMIDIout = new JButton();
        this.label18 = new JLabel();
        this.dgvMIDIoutPallet = new JTable();
        this.label16 = new JLabel();

        //
        // btnAddMIDIout
        //
        this.btnAddMIDIout.setName("btnAddMIDIout");
        this.btnAddMIDIout.addActionListener(this::btnAddMIDIout_Click);
        //
        // btnAddVST
        //
        this.btnAddVST.setName("btnAddVST");
        this.btnAddVST.addActionListener(this::btnAddVST_Click);
        //
        // btnDOWN_A
        //
        this.btnDOWN_A.setName("btnDOWN_A");
        this.btnDOWN_A.addActionListener(this::btnDOWN_Click);
        //
        // btnDOWN_B
        //
        this.btnDOWN_B.setName("btnDOWN_B");
        this.btnDOWN_B.addActionListener(this::btnDOWN_Click);
        //
        // btnDOWN_C
        //
        this.btnDOWN_C.setName("btnDOWN_C");
        this.btnDOWN_C.addActionListener(this::btnDOWN_Click);
        //
        // btnDOWN_D
        //
        this.btnDOWN_D.setName("btnDOWN_D");
        this.btnDOWN_D.addActionListener(this::btnDOWN_Click);
        //
        // btnDOWN_E
        //
        this.btnDOWN_E.setName("btnDOWN_E");
        this.btnDOWN_E.addActionListener(this::btnDOWN_Click);
        //
        // btnDOWN_F
        //
        this.btnDOWN_F.setName("btnDOWN_F");
        this.btnDOWN_F.addActionListener(this::btnDOWN_Click);
        //
        // btnDOWN_G
        //
        this.btnDOWN_G.setName("btnDOWN_G");
        this.btnDOWN_G.addActionListener(this::btnDOWN_Click);
        //
        // btnDOWN_H
        //
        this.btnDOWN_H.setName("btnDOWN_H");
        this.btnDOWN_H.addActionListener(this::btnDOWN_Click);
        //
        // btnDOWN_I
        //
        this.btnDOWN_I.setName("btnDOWN_I");
        this.btnDOWN_I.addActionListener(this::btnDOWN_Click);
        //
        // btnDOWN_J
        //
        this.btnDOWN_J.setName("btnDOWN_J");
        this.btnDOWN_J.addActionListener(this::btnDOWN_Click);
        //
        // btnSubMIDIout
        //
        this.btnSubMIDIout.setName("btnSubMIDIout");
        this.btnSubMIDIout.addActionListener(this::btnSubMIDIout_Click);
        //
        // btnUP_A
        //
        this.btnUP_A.setName("btnUP_A");
        this.btnUP_A.addActionListener(this::btnUP_Click);
        //
        // btnUP_B
        //
        this.btnUP_B.setName("btnUP_B");
        this.btnUP_B.addActionListener(this::btnUP_Click);
        //
        // btnUP_C
        //
        this.btnUP_C.setName("btnUP_C");
        this.btnUP_C.addActionListener(this::btnUP_Click);
        //
        // btnUP_D
        //
        this.btnUP_D.setName("btnUP_D");
        this.btnUP_D.addActionListener(this::btnUP_Click);
        //
        // btnUP_E
        //
        this.btnUP_E.setName("btnUP_E");
        this.btnUP_E.addActionListener(this::btnUP_Click);
        //
        // btnUP_F
        //
        this.btnUP_F.setName("btnUP_F");
        this.btnUP_F.addActionListener(this::btnUP_Click);
        //
        // btnUP_G
        //
        this.btnUP_G.setName("btnUP_G");
        this.btnUP_G.addActionListener(this::btnUP_Click);
        //
        // btnUP_H
        //
        this.btnUP_H.setName("btnUP_H");
        this.btnUP_H.addActionListener(this::btnUP_Click);
        //
        // btnUP_I
        //
        this.btnUP_I.setName("btnUP_I");
        this.btnUP_I.addActionListener(this::btnUP_Click);
        //
        // button17
        //
        this.button17.setName("button17");
        this.button17.addActionListener(this::btnUP_Click);
        //
        // dgvMIDIoutListA
        //
        this.dgvMIDIoutListA.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.dgvMIDIoutListA.setName("dgvMIDIoutListA");
        //
        // dgvMIDIoutListB
        //
        this.dgvMIDIoutListB.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.dgvMIDIoutListB.setName("dgvMIDIoutListB");
        //
        // dgvMIDIoutListC
        //
        this.dgvMIDIoutListC.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.dgvMIDIoutListC.setName("dgvMIDIoutListC");
        //
        // dgvMIDIoutListD
        //
        this.dgvMIDIoutListD.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.dgvMIDIoutListD.setName("dgvMIDIoutListD");
        //
        // dgvMIDIoutListE
        //
        this.dgvMIDIoutListE.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.dgvMIDIoutListE.setName("dgvMIDIoutListE");
        //
        // dgvMIDIoutListF
        //
        this.dgvMIDIoutListF.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.dgvMIDIoutListF.setName("dgvMIDIoutListF");
        //
        // dgvMIDIoutListG
        //
        this.dgvMIDIoutListG.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.dgvMIDIoutListG.setName("dgvMIDIoutListG");
        //
        // dgvMIDIoutListH
        //
        this.dgvMIDIoutListH.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.dgvMIDIoutListH.setName("dgvMIDIoutListH");
        //
        // dgvMIDIoutListI
        //
        this.dgvMIDIoutListI.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.dgvMIDIoutListI.setName("dgvMIDIoutListI");
        //
        // dgvMIDIoutListJ
        //
        this.dgvMIDIoutListJ.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.dgvMIDIoutListJ.setName("dgvMIDIoutListJ");
        //
        // dgvMIDIoutPallet
        //
        this.dgvMIDIoutPallet.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.dgvMIDIoutPallet.setName("dgvMIDIoutPallet");
        //
        // label16
        //
        this.label16.setName("label16");
        //
        // label18
        //
        this.label18.setName("label18");
        //
        // tabPage1
        //
        this.tabPage1.setName("tabPage1");
        //
        // tabPage10
        //
        this.tabPage10.setName("tabPage10");
        //
        // tabPage2
        //
        this.tabPage2.setName("tabPage2");
        //
        // tabPage3
        //
        this.tabPage3.setName("tabPage3");
        //
        // tabPage4
        //
        this.tabPage4.setName("tabPage4");
        //
        // tabPage5
        //
        this.tabPage5.setName("tabPage5");
        //
        // tabPage6
        //
        this.tabPage6.setName("tabPage6");
        //
        // tabPage7
        //
        this.tabPage7.setName("tabPage7");
        //
        // tabPage8
        //
        this.tabPage8.setName("tabPage8");
        //
        // tabPage9
        //
        this.tabPage9.setName("tabPage9");
        //
        // tbcMIDIoutList
        //
        this.tbcMIDIoutList.setName("tbcMIDIoutList");
        //
        // tpMIDIOut
        //
        this.setName("tpMIDIOut");
        // layout
        page(this.tabPage1, this.dgvMIDIoutListA, this.btnUP_A, this.btnDOWN_A);
        page(this.tabPage2, this.dgvMIDIoutListB, this.btnUP_B, this.btnDOWN_B);
        page(this.tabPage3, this.dgvMIDIoutListC, this.btnUP_C, this.btnDOWN_C);
        page(this.tabPage4, this.dgvMIDIoutListD, this.btnUP_D, this.btnDOWN_D);
        page(this.tabPage5, this.dgvMIDIoutListE, this.btnUP_E, this.btnDOWN_E);
        page(this.tabPage6, this.dgvMIDIoutListF, this.btnUP_F, this.btnDOWN_F);
        page(this.tabPage7, this.dgvMIDIoutListG, this.btnUP_G, this.btnDOWN_G);
        page(this.tabPage8, this.dgvMIDIoutListH, this.btnUP_H, this.btnDOWN_H);
        page(this.tabPage9, this.dgvMIDIoutListI, this.btnUP_I, this.btnDOWN_I);
        page(this.tabPage10, this.dgvMIDIoutListJ, this.button17, this.btnDOWN_J);
        this.dgvMIDIoutPallet.setPreferredScrollableViewportSize(new Dimension(450, 120));
        new Grid(this)
                .add(this.label16).next()
                .span(new JScrollPane(this.dgvMIDIoutPallet)).next()
                .add(this.label18).span(Grid.row(this.btnAddMIDIout, this.btnSubMIDIout, this.btnAddVST)).next()
                .grow(this.tbcMIDIoutList);

        dgv = new JTable[] {
                dgvMIDIoutListA, dgvMIDIoutListB, dgvMIDIoutListC, dgvMIDIoutListD, dgvMIDIoutListE,
                dgvMIDIoutListF, dgvMIDIoutListG, dgvMIDIoutListH, dgvMIDIoutListI, dgvMIDIoutListJ
        };
    }

    /** one of the MIDI out list tabs: the table, with the buttons moving a row up and down at its side */
    private void page(JPanel page, JTable table, JButton up, JButton down) {
        JPanel buttons = new JPanel(new BorderLayout());
        buttons.add(up, BorderLayout.NORTH);
        buttons.add(down, BorderLayout.SOUTH);
        page.setLayout(new BorderLayout());
        page.add(new JScrollPane(table));
        page.add(buttons, BorderLayout.EAST);
        this.tbcMIDIoutList.add(page);
    }

    @Override
    public void load(Setting setting) {
        copyFromMIDIoutListA(dgvMIDIoutListB);
        copyFromMIDIoutListA(dgvMIDIoutListC);
        copyFromMIDIoutListA(dgvMIDIoutListD);
        copyFromMIDIoutListA(dgvMIDIoutListE);
        copyFromMIDIoutListA(dgvMIDIoutListF);
        copyFromMIDIoutListA(dgvMIDIoutListG);
        copyFromMIDIoutListA(dgvMIDIoutListH);
        copyFromMIDIoutListA(dgvMIDIoutListI);
        copyFromMIDIoutListA(dgvMIDIoutListJ);
        if (setting.getMidiOut().getMidiOutInfos() != null && !setting.getMidiOut().getMidiOutInfos().isEmpty()) {
            for (int i = 0; i < setting.getMidiOut().getMidiOutInfos().size(); i++) {
                DefaultTableModel m = (DefaultTableModel) dgv[i].getModel();
                m.setRowCount(0);
                Set<Integer> midioutNotFound = new HashSet<>();
                if (setting.getMidiOut().getMidiOutInfos().get(i) != null && setting.getMidiOut().getMidiOutInfos().get(i).length > 0) {
                    for (int j = 0; j < setting.getMidiOut().getMidiOutInfos().get(i).length; j++) {
                        MidiOutInfo moi = setting.getMidiOut().getMidiOutInfos().get(i)[j];
                        int found = -999;
                        int k = 0;
                        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
                            try {
                                MidiDevice device = MidiSystem.getMidiDevice(info);
                                if (device.getMaxReceivers() == 0) {
                                    continue;
                                }
                                if (moi.name.equals(device.getDeviceInfo().getName())) {
                                    midioutNotFound.add(k);
                                    found = k++;
                                    break;
                                }
                            } catch (MidiUnavailableException e) {
                                logger.log(Level.ERROR, e.getMessage(), e);
                            }
                        }

                        moi.id = found;

                        String stype = switch (moi.type) {
                            case 1 -> "XG";
                            case 2 -> "GS";
                            case 3 -> "LA";
                            case 4 -> "GS(SC-55_1)";
                            case 5 -> "GS(SC-55_2)";
                            default -> "GM";
                        };

                        String sbeforeSend = switch (moi.beforeSendType) {
                            case 1 -> "GM Reset";
                            case 2 -> "XG Reset";
                            case 3 -> "GS Reset";
                            case 4 -> "Custom";
                            default -> "None";
                        };

                        m.addRow(new Object[] {
                                moi.id,
                                moi.isVST,
                                moi.fileName,
                                moi.name,
                                stype,
                                sbeforeSend,
                                moi.isVST ? moi.vendor : (moi.manufacturer != -1 ? String.valueOf(moi.manufacturer) : "Unknown")
                        });
                    }
                }
            }
        }

        DefaultTableModel m = (DefaultTableModel) dgvMIDIoutPallet.getModel();

        int i = 0;
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.getMaxReceivers() == 0) {
                    continue;
                }
                m.addRow(new Object[] {i++, device.getDeviceInfo().getName(), device.getDeviceInfo().getVendor()});
            } catch (MidiUnavailableException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }
    }

    @Override
    public void apply(Setting setting) {
        int i;

        setting.getMidiOut().setMidiOutInfos(new ArrayList<>());

        for (JTable d : dgv) {
            if (d.getRowCount() > 0) {
                List<MidiOutInfo> lstMoi = new ArrayList<>();
                for (i = 0; i < d.getRowCount(); i++) {
                    MidiOutInfo moi = new MidiOutInfo();
                    moi.id = (int) d.getModel().getValueAt(i, 0);
                    moi.isVST = (boolean) d.getModel().getValueAt(i, 1);
                    moi.fileName = (String) d.getModel().getValueAt(i, 2);
                    moi.name = (String) d.getModel().getValueAt(i, 3);
                    String stype = (String) d.getModel().getValueAt(i, 4);
                    // GM / XG / GS / LA / GS(SC - 55_1) / GS(SC - 55_2)
                    moi.type = 0;
                    if (stype.equals("XG")) moi.type = 1;
                    if (stype.equals("GS")) moi.type = 2;
                    if (stype.equals("LA")) moi.type = 3;
                    if (stype.equals("GS(SC - 55_1)")) moi.type = 4;
                    if (stype.equals("GS(SC - 55_2)")) moi.type = 5;
                    String sbeforeSend = (String) d.getModel().getValueAt(i, 5);
                    moi.beforeSendType = 0;
                    if (sbeforeSend.equals("GM Reset")) moi.beforeSendType = 1;
                    if (sbeforeSend.equals("XG Reset")) moi.beforeSendType = 2;
                    if (sbeforeSend.equals("GS Reset")) moi.beforeSendType = 3;
                    if (sbeforeSend.equals("Custom")) moi.beforeSendType = 4;

                    String mn = (String) d.getModel().getValueAt(i, 6);
                    if (moi.isVST) {
                        moi.vendor = mn;
                        moi.manufacturer = -1;
                    } else {
                        moi.vendor = "";
                        moi.manufacturer = mn == null || mn.equals("Unknown") ? -1 : FormSetting.Manufacturers.byManufacture(mn);
                    }

                    lstMoi.add(moi);
                }
                setting.getMidiOut().getMidiOutInfos().add(lstMoi.toArray(MidiOutInfo[]::new));
            } else {
                setting.getMidiOut().getMidiOutInfos().add(null);
            }
        }

    }

    private void btnAddMIDIout_Click(ActionEvent ev) {
        if (dgvMIDIoutPallet.getSelectedRowCount() < 1) return;

        int p = tbcMIDIoutList.getSelectedIndex();

        for (int row : dgvMIDIoutPallet.getSelectedRows()) {
            boolean found = false;
            for (int r = 0; r < dgv[p].getRowCount(); r++) {
                if (dgv[p].getModel().getValueAt(r, 1).equals(dgvMIDIoutPallet.getModel().getValueAt(row, 1))) {
                    found = true;
                    break;
                }
            }

            if (!found)
                ((DefaultTableModel) dgv[p].getModel()).addRow(new Object[] {
                        dgvMIDIoutPallet.getModel().getValueAt(row, 0),
                        false,
                        "",
                        dgvMIDIoutPallet.getModel().getValueAt(row, 1),
                        "GM",
                        "None",
                        dgvMIDIoutPallet.getModel().getValueAt(row, 2)
                });
        }
    }

    /**
     * Adds a VST instrument to the MIDI outs of the tab being edited.
     * <p>
     * The plug-in is loaded once here, only to be asked what it is called and who made it, so the
     * row reads like the rows for the real ports beside it. It is loaded again for real, and kept,
     * the first time a song is actually played through it.
     */
    private void btnAddVST_Click(ActionEvent ev) {
        JFileChooser ofd = new JFileChooser();
        ofd.setFileFilter(VstFileChooser.filter());
        ofd.setDialogTitle("Select a VST instrument");
        // a plug-in is a bundle - that is, a directory - on macOS, so one has to be selectable
        ofd.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        ofd.setMultiSelectionEnabled(false);

        String defaultPath = setting.getVst().getDefaultPath();
        if (defaultPath != null && !defaultPath.isEmpty() && Files.exists(Path.of(defaultPath))) {
            ofd.setCurrentDirectory(new File(defaultPath));
        } else {
            File known = VstFileChooser.defaultDirectory();
            if (known != null) ofd.setCurrentDirectory(known);
        }

        if (ofd.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selected = ofd.getSelectedFile();
        VstInfo s = ChipRegister.shared(VstPlugin.class).getInfo(selected.getAbsolutePath());
        if (s == null) {
            JOptionPane.showMessageDialog(this, selected.getName() + " could not be loaded as a VST plugin.",
                    "VST", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File directory = selected.getParentFile();
        if (directory != null) setting.getVst().setDefaultPath(directory.getAbsolutePath());

        int p = tbcMIDIoutList.getSelectedIndex();
        ((DefaultTableModel) dgv[p].getModel()).addRow(new Object[] {
                -999
                , true
                , s.fileName
                , s.effectName
                , "GM"
                , "None"
                , s.vendorName
        });
    }

    private void btnDOWN_Click(ActionEvent ev) {
        int p = tbcMIDIoutList.getSelectedIndex();

        if (dgv[p].getSelectedRowCount() < 1) return;

        for (int row : dgv[p].getSelectedRows()) {
            if (row > dgv[p].getRowCount() - 2) continue;

            int i = row + 1;
            DefaultTableModel m = (DefaultTableModel) dgv[p].getModel();
            m.insertRow(i + 2, new Object[] {
                    m.getValueAt(row, 0),
                    m.getValueAt(row, 1),
                    m.getValueAt(row, 2),
                    m.getValueAt(row, 3),
                    m.getValueAt(row, 4),
                    m.getValueAt(row, 5),
            });
            m.removeRow(row);
            dgv[p].setRowSelectionInterval(i, i);
        }
    }

    private void btnSubMIDIout_Click(ActionEvent ev) {
        int p = tbcMIDIoutList.getSelectedIndex();

        if (dgv[p].getSelectedRowCount() < 1) return;

        for (int row : dgv[p].getSelectedRows()) {
            ((DefaultTableModel) dgv[p].getModel()).removeRow(row);
        }
    }

    private void btnUP_Click(ActionEvent ev) {
        int p = tbcMIDIoutList.getSelectedIndex();

        if (dgv[p].getSelectedRowCount() < 1) return;

        for (int row : dgv[p].getSelectedRows()) {
            if (row < 1) continue;

            int i = row - 1;
            DefaultTableModel m = (DefaultTableModel) dgv[p].getModel();
            m.insertRow(i, new Object[] {
                    m.getValueAt(row, 0),
                    m.getValueAt(row, 1),
                    m.getValueAt(row, 2),
                    m.getValueAt(row, 3),
                    m.getValueAt(row, 4),
                    m.getValueAt(row, 5),
            });
            m.removeRow(row);
            dgv[p].setRowSelectionInterval(i, i);
        }
    }

    private void copyFromMIDIoutListA(JTable dgv) {

        dgv.setColumnModel(new DefaultTableColumnModel());

        Enumeration<TableColumn> columns = dgvMIDIoutListA.getColumnModel().getColumns();
        while (columns.hasMoreElements()) {
            dgv.getColumnModel().addColumn(columns.nextElement());
        }
    }
}
