package mdplayer.form.sys.setting;

import java.awt.event.ActionEvent;
import java.lang.System.Logger;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import mdplayer.Common.EnmInstFormat;
import mdplayer.Setting;
import mdplayer.form.Grid;
import mdplayer.form.SettingTab;
import mdplayer.form.sys.FormSetting;

import static java.lang.System.getLogger;

/** the "Other" page of the settings dialog, split out of the original FormSetting */
public class SettingOtherPanel extends SettingTab {

    @Override
    public int order() {
        return 140;
    }

    private static final Logger logger = getLogger(SettingOtherPanel.class.getName());

    /** the designer's captions, converted from frmSetting.resx */
    private static final ResourceBundle resources = ResourceBundle.getBundle("mdplayer/form/sys/frmSetting", Locale.getDefault());

    private final JCheckBox cbUseLoopTimes;
    private final JLabel lblLoopTimes;
    private final JTextField tbLoopTimes;
    private final JButton btnOpenSettingFolder;
    private final JCheckBox cbUseGetInst;
    private final JButton btnDataPath;
    private final JTextField tbDataPath;
    private final JLabel label19;
    private final JComboBox<String> cmbInstFormat;
    private final JLabel lblInstFormat;
    private final JLabel label30;
    private final JTextField tbScreenFrameRate;
    private final JLabel label29;
    private final JCheckBox cbAutoOpen;
    private final JPanel groupBox4;
    private final JCheckBox cbDumpSwitch;
    private final JPanel gbDump;
    private final JButton btnDumpPath;
    private final JLabel label6;
    private final JTextField tbDumpPath;
    private final JButton btnResetPosition;
    private final JCheckBox cbWavSwitch;
    private final JPanel gbWav;
    private final JButton btnWavPath;
    private final JLabel label7;
    private final JTextField tbWavPath;
    private final JCheckBox cbInitAlways;
    private final JCheckBox cbExALL;
    private final JCheckBox cbNonRenderingForPause;
    private final JButton btnSearchPath;
    private final JTextField tbSearchPath;
    private final JLabel label68;

    public SettingOtherPanel() {
        DefaultComboBoxModel<String> m;

        this.btnSearchPath = new JButton();
        this.tbSearchPath = new JTextField();
        this.label68 = new JLabel();
        this.cbNonRenderingForPause = new JCheckBox();
        this.cbWavSwitch = new JCheckBox();
        this.cbUseGetInst = new JCheckBox();
        this.groupBox4 = new JPanel();
        this.cmbInstFormat = new JComboBox<>();
        this.lblInstFormat = new JLabel();
        this.cbDumpSwitch = new JCheckBox();
        this.gbWav = new JPanel();
        this.btnWavPath = new JButton();
        this.label7 = new JLabel();
        this.tbWavPath = new JTextField();
        this.gbDump = new JPanel();
        this.btnDumpPath = new JButton();
        this.label6 = new JLabel();
        this.tbDumpPath = new JTextField();
        this.label30 = new JLabel();
        this.tbScreenFrameRate = new JTextField();
        this.label29 = new JLabel();
        this.lblLoopTimes = new JLabel();
        this.btnDataPath = new JButton();
        this.tbLoopTimes = new JTextField();
        this.tbDataPath = new JTextField();
        this.label19 = new JLabel();
        this.btnResetPosition = new JButton();
        this.btnOpenSettingFolder = new JButton();
        this.cbExALL = new JCheckBox();
        this.cbInitAlways = new JCheckBox();
        this.cbAutoOpen = new JCheckBox();
        this.cbUseLoopTimes = new JCheckBox();

        //
        // btnDataPath
        //
        this.btnDataPath.setName("btnDataPath");
        this.btnDataPath.addActionListener(this::btnDataPath_Click);
        //
        // btnDumpPath
        //
        this.btnDumpPath.setName("btnDumpPath");
        this.btnDumpPath.addActionListener(this::btnDumpPath_Click);
        //
        // btnOpenSettingFolder
        //
        this.btnOpenSettingFolder.setName("btnOpenSettingFolder");
        this.btnOpenSettingFolder.addActionListener(FormSetting::btnOpenSettingFolder_Click);
        //
        // btnResetPosition
        //
        this.btnResetPosition.setName("btnResetPosition");
        this.btnResetPosition.addActionListener(this::btnResetPosition_Click);
        //
        // btnSearchPath
        //
        this.btnSearchPath.setName("btnSearchPath");
        this.btnSearchPath.addActionListener(this::btnSearchPath_Click);
        //
        // btnWavPath
        //
        this.btnWavPath.setName("btnWavPath");
        this.btnWavPath.addActionListener(this::btnWavPath_Click);
        //
        // cbAutoOpen
        //
        this.cbAutoOpen.setName("cbAutoOpen");
        this.cbAutoOpen.addChangeListener(this::cbUseLoopTimes_CheckedChanged);
        //
        // cbDumpSwitch
        //
        this.cbDumpSwitch.setName("cbDumpSwitch");
        this.cbDumpSwitch.addChangeListener(this::cbDumpSwitch_CheckedChanged);
        //
        // cbExALL
        //
        this.cbExALL.setName("cbExALL");
        this.cbExALL.addChangeListener(this::cbUseLoopTimes_CheckedChanged);
        //
        // cbInitAlways
        //
        this.cbInitAlways.setName("cbInitAlways");
        this.cbInitAlways.addChangeListener(this::cbUseLoopTimes_CheckedChanged);
        //
        // cbNonRenderingForPause
        //
        this.cbNonRenderingForPause.setName("cbNonRenderingForPause");
        //
        // cbUseGetInst
        //
        this.cbUseGetInst.setName("cbUseGetInst");
        this.cbUseGetInst.addChangeListener(this::cbUseGetInst_CheckedChanged);
        //
        // cbUseLoopTimes
        //
        this.cbUseLoopTimes.setName("cbUseLoopTimes");
        this.cbUseLoopTimes.addChangeListener(this::cbUseLoopTimes_CheckedChanged);
        //
        // cbWavSwitch
        //
        this.cbWavSwitch.setName("cbWavSwitch");
        this.cbWavSwitch.addChangeListener(this::cbWavSwitch_CheckedChanged);
        //
        // cmbInstFormat
        //
        m = ((DefaultComboBoxModel<String>) this.cmbInstFormat.getModel());
        m.addElement(resources.getString("cmbInstFormat.Items"));
        m.addElement(resources.getString("cmbInstFormat.Items1"));
        m.addElement(resources.getString("cmbInstFormat.Items2"));
        m.addElement(resources.getString("cmbInstFormat.Items3"));
        m.addElement(resources.getString("cmbInstFormat.Items4"));
        m.addElement(resources.getString("cmbInstFormat.Items5"));
        m.addElement(resources.getString("cmbInstFormat.Items6"));
        m.addElement(resources.getString("cmbInstFormat.Items7"));
        m.addElement(resources.getString("cmbInstFormat.Items8"));
        m.addElement(resources.getString("cmbInstFormat.Items9"));
        m.addElement(resources.getString("cmbInstFormat.Items10"));
        m.addElement(resources.getString("cmbInstFormat.Items11"));
        m.addElement(resources.getString("cmbInstFormat.Items12"));
        m.addElement(resources.getString("cmbInstFormat.Items13"));
        m.addElement(resources.getString("cmbInstFormat.Items14"));
        m.addElement(resources.getString("cmbInstFormat.Items15"));
        m.addElement(resources.getString("cmbInstFormat.Items16"));
        this.cmbInstFormat.setName("cmbInstFormat");
        //
        // gbDump
        //
        this.gbDump.setName("gbDump");
        //
        // gbWav
        //
        this.gbWav.setName("gbWav");
        //
        // groupBox4
        //
        this.groupBox4.setName("groupBox4");
        //
        // label19
        //
        this.label19.setName("label19");
        //
        // label29
        //
        this.label29.setName("label29");
        //
        // label30
        //
        this.label30.setName("label30");
        //
        // label6
        //
        this.label6.setName("label6");
        //
        // label68
        //
        this.label68.setName("label68");
        //
        // label7
        //
        this.label7.setName("label7");
        //
        // lblInstFormat
        //
        this.lblInstFormat.setName("lblInstFormat");
        //
        // lblLoopTimes
        //
        this.lblLoopTimes.setName("lblLoopTimes");
        //
        // tbDataPath
        //
        this.tbDataPath.setName("tbDataPath");
        //
        // tbDumpPath
        //
        this.tbDumpPath.setName("tbDumpPath");
        //
        // tbLoopTimes
        //
        this.tbLoopTimes.setName("tbLoopTimes");
        //
        // tbScreenFrameRate
        //
        this.tbScreenFrameRate.setName("tbScreenFrameRate");
        //
        // tbSearchPath
        //
        this.tbSearchPath.setName("tbSearchPath");
        //
        // tbWavPath
        //
        this.tbWavPath.setName("tbWavPath");
        //
        // tpOther
        //
        this.setName("tpOther");
        // layout
        new Grid(Grid.group(this.groupBox4))
                .add(this.cbUseGetInst).next()
                .add(Grid.row(this.lblInstFormat, this.cmbInstFormat)).end();
        new Grid(Grid.group(this.gbDump))
                .add(this.cbDumpSwitch, 3).next()
                .add(this.label6).fill(this.tbDumpPath).add(this.btnDumpPath).end();
        new Grid(Grid.group(this.gbWav))
                .add(this.cbWavSwitch, 3).next()
                .add(this.label7).fill(this.tbWavPath).add(this.btnWavPath).end();
        this.tbLoopTimes.setColumns(4);
        this.tbScreenFrameRate.setColumns(4);
        new Grid(this)
                .span(Grid.row(this.cbUseLoopTimes, this.tbLoopTimes, this.lblLoopTimes)).next()
                .span(this.groupBox4).next()
                .span(this.cbAutoOpen).next()
                .add(this.label19).fill(this.tbDataPath).add(this.btnDataPath).next()
                .add(this.label68).fill(this.tbSearchPath).add(this.btnSearchPath).next()
                .add(this.label29).span(Grid.row(this.tbScreenFrameRate, this.label30, this.cbNonRenderingForPause)).next()
                .span(this.gbDump).next()
                .span(this.gbWav).next()
                .span(this.cbInitAlways).next()
                .span(this.cbExALL)
                .spring()
                .span(Grid.trailing(this.btnResetPosition, this.btnOpenSettingFolder)).end();
    }

    @Override
    public void load(Setting setting) {
        cbUseLoopTimes.setSelected(setting.getOther().getUseLoopTimes());
        tbLoopTimes.setEnabled(cbUseLoopTimes.isSelected());
        lblLoopTimes.setEnabled(cbUseLoopTimes.isSelected());
        tbLoopTimes.setText(String.valueOf(setting.getOther().getLoopTimes()));
        cbUseGetInst.setSelected(setting.getOther().getUseGetInst());
        tbDataPath.setText(setting.getOther().getDefaultDataPath());
        tbSearchPath.setText(setting.getFileSearchPathList());
        cmbInstFormat.setSelectedIndex(setting.getOther().getInstFormat().ordinal());
        tbScreenFrameRate.setText(String.valueOf(setting.getOther().getScreenFrameRate()));
        cbAutoOpen.setSelected(setting.getOther().getAutoOpen());
        cbDumpSwitch.setSelected(setting.getOther().getDumpSwitch());
        gbDump.setEnabled(cbDumpSwitch.isSelected());
        tbDumpPath.setText(setting.getOther().getDumpPath());
        cbWavSwitch.setSelected(setting.getOther().getWavSwitch());
        gbWav.setEnabled(cbWavSwitch.isSelected());
        tbWavPath.setText(setting.getOther().getWavPath());
        cbInitAlways.setSelected(setting.getOther().getInitAlways());
        cbExALL.setSelected(setting.getOther().getExAll());
        cbNonRenderingForPause.setSelected(setting.getOther().getNonRenderingForPause());

        cbUseGetInst_CheckedChanged(null);
    }

    @Override
    public void apply(Setting setting) {
        setting.getOther().setUseLoopTimes(cbUseLoopTimes.isSelected());
        setting.getOther().setLoopTimes(FormSetting.parseIntSafe(tbLoopTimes.getText(), 1, 999, setting.getOther().getLoopTimes()));
        setting.getOther().setUseGetInst(cbUseGetInst.isSelected());
        setting.getOther().setDefaultDataPath(tbDataPath.getText());
        setting.setFileSearchPathList(tbSearchPath.getText());
        setting.getOther().setInstFormat(EnmInstFormat.values()[cmbInstFormat.getSelectedIndex()]);
        setting.getOther().setScreenFrameRate(FormSetting.parseIntSafe(tbScreenFrameRate.getText(), 10, 120, setting.getOther().getScreenFrameRate()));
        setting.getOther().setAutoOpen(cbAutoOpen.isSelected());
        setting.getOther().setDumpSwitch(cbDumpSwitch.isSelected());
        setting.getOther().setDumpPath(tbDumpPath.getText());
        setting.getOther().setWavSwitch(cbWavSwitch.isSelected());
        setting.getOther().setWavPath(tbWavPath.getText());
        setting.getOther().setInitAlways(cbInitAlways.isSelected());
        setting.getOther().setExAll(cbExALL.isSelected());
        setting.getOther().setNonRenderingForPause(cbNonRenderingForPause.isSelected());
    }

    private void btnDataPath_Click(ActionEvent ev) {
        JFileChooser fbd = new JFileChooser();
        fbd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fbd.setDialogTitle("Please specify a folder.");

        if (fbd.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        tbDataPath.setText(fbd.getSelectedFile().getPath());
    }

    private void btnDumpPath_Click(ActionEvent ev) {
        JFileChooser fbd = new JFileChooser();
        fbd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fbd.setDialogTitle("Please specify a folder.");

        if (fbd.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        tbDumpPath.setText(fbd.getSelectedFile().getPath());
    }

    private void btnResetPosition_Click(ActionEvent ev) {
        int res = JOptionPane.showConfirmDialog(null,
                "Reset all display positions. Are you sure? (The position of currently open windows cannot be reset.)",
                "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (res == JOptionPane.NO_OPTION) return;

        setting.setLocation(new Setting.Location());
    }

    private void btnSearchPath_Click(ActionEvent ev) {
        JFileChooser fbd = new JFileChooser();
        fbd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fbd.setDialogTitle("Please specify a folder.");

        if (fbd.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        tbSearchPath.setText(fbd.getSelectedFile().getPath());
    }

    private void btnWavPath_Click(ActionEvent ev) {
        JFileChooser fbd = new JFileChooser();
        fbd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fbd.setDialogTitle("Please specify a folder.");

        if (fbd.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        tbWavPath.setText(fbd.getSelectedFile().getPath());
    }

    private void cbDumpSwitch_CheckedChanged(ChangeEvent ev) {
        gbDump.setEnabled(cbDumpSwitch.isSelected());
    }

    private void cbUseGetInst_CheckedChanged(ChangeEvent ev) {
        lblInstFormat.setEnabled(cbUseGetInst.isSelected());
        cmbInstFormat.setEnabled(cbUseGetInst.isSelected());
    }

    private void cbUseLoopTimes_CheckedChanged(ChangeEvent ev) {
        tbLoopTimes.setEnabled(cbUseLoopTimes.isSelected());
        lblLoopTimes.setEnabled(cbUseLoopTimes.isSelected());
    }

    private void cbWavSwitch_CheckedChanged(ChangeEvent ev) {
        gbWav.setEnabled(cbWavSwitch.isSelected());
    }
}
