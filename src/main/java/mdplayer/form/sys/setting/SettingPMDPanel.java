package mdplayer.form.sys.setting;

import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import mdplayer.Setting;
import mdplayer.form.Grid;
import mdplayer.form.SettingTab;
import static java.lang.System.getLogger;

/** the "PMDDotNET" page of the settings dialog, split out of the original FormSetting */
public class SettingPMDPanel extends SettingTab {

    @Override
    public int order() {
        return 60;
    }

    private static final Logger logger = getLogger(SettingPMDPanel.class.getName());

    private final JCheckBox rbPMDManual;
    private final JCheckBox rbPMDAuto;
    private final JButton btnPMDResetDriverArguments;
    private final JLabel label54;
    private final JButton btnPMDResetCompilerArhguments;
    private final JTextField tbPMDDriverArguments;
    private final JLabel label55;
    private final JTextField tbPMDCompilerArguments;
    private final JPanel gbPMDManual;
    private final JCheckBox cbPMDSetManualVolume;
    private final JCheckBox cbPMDUsePPZ8;
    private final JPanel groupBox32;
    private final JCheckBox rbPMD86B;
    private final JCheckBox rbPMDSpbB;
    private final JCheckBox rbPMDNrmB;
    private final JCheckBox cbPMDUsePPSDRV;
    private final JPanel gbPPSDRV;
    private final JPanel groupBox33;
    private final JCheckBox rbPMDUsePPSDRVManualFreq;
    private final JLabel label56;
    private final JCheckBox rbPMDUsePPSDRVFreqDefault;
    private final JButton btnPMDPPSDRVManualWait;
    private final JLabel label57;
    private final JTextField tbPMDPPSDRVFreq;
    private final JLabel label58;
    private final JTextField tbPMDPPSDRVManualWait;
    private final JPanel gbPMDSetManualVolume;
    private final JLabel label59;
    private final JLabel label60;
    private final JTextField tbPMDVolumeAdpcm;
    private final JLabel label61;
    private final JTextField tbPMDVolumeRhythm;
    private final JLabel label62;
    private final JTextField tbPMDVolumeSSG;
    private final JLabel label63;
    private final JTextField tbPMDVolumeGIMICSSG;
    private final JLabel label64;
    private final JTextField tbPMDVolumeFM;

    public SettingPMDPanel() {
        this.rbPMDManual = new JCheckBox();
        this.rbPMDAuto = new JCheckBox();
        this.btnPMDResetDriverArguments = new JButton();
        this.label54 = new JLabel();
        this.btnPMDResetCompilerArhguments = new JButton();
        this.tbPMDDriverArguments = new JTextField();
        this.label55 = new JLabel();
        this.tbPMDCompilerArguments = new JTextField();
        this.gbPMDManual = new JPanel();
        this.cbPMDSetManualVolume = new JCheckBox();
        this.cbPMDUsePPZ8 = new JCheckBox();
        this.groupBox32 = new JPanel();
        this.rbPMD86B = new JCheckBox();
        this.rbPMDSpbB = new JCheckBox();
        this.rbPMDNrmB = new JCheckBox();
        this.cbPMDUsePPSDRV = new JCheckBox();
        this.gbPPSDRV = new JPanel();
        this.groupBox33 = new JPanel();
        this.rbPMDUsePPSDRVManualFreq = new JCheckBox();
        this.label56 = new JLabel();
        this.rbPMDUsePPSDRVFreqDefault = new JCheckBox();
        this.btnPMDPPSDRVManualWait = new JButton();
        this.label57 = new JLabel();
        this.tbPMDPPSDRVFreq = new JTextField();
        this.label58 = new JLabel();
        this.tbPMDPPSDRVManualWait = new JTextField();
        this.gbPMDSetManualVolume = new JPanel();
        this.label59 = new JLabel();
        this.label60 = new JLabel();
        this.tbPMDVolumeAdpcm = new JTextField();
        this.label61 = new JLabel();
        this.tbPMDVolumeRhythm = new JTextField();
        this.label62 = new JLabel();
        this.tbPMDVolumeSSG = new JTextField();
        this.label63 = new JLabel();
        this.tbPMDVolumeGIMICSSG = new JTextField();
        this.label64 = new JLabel();
        this.tbPMDVolumeFM = new JTextField();

        //
        // btnPMDPPSDRVManualWait
        //
        this.btnPMDPPSDRVManualWait.setName("btnPMDPPSDRVManualWait");
        this.btnPMDPPSDRVManualWait.addActionListener(this::btnPMDPPSDRVManualWait_Click);
        //
        // btnPMDResetCompilerArhguments
        //
        this.btnPMDResetCompilerArhguments.setName("btnPMDResetCompilerArhguments");
        this.btnPMDResetCompilerArhguments.addActionListener(this::btnPMDResetCompilerArhguments_Click);
        //
        // btnPMDResetDriverArguments
        //
        this.btnPMDResetDriverArguments.setName("btnPMDResetDriverArguments");
        this.btnPMDResetDriverArguments.addActionListener(this::btnPMDResetDriverArguments_Click);
        //
        // cbPMDSetManualVolume
        //
        this.cbPMDSetManualVolume.setName("cbPMDSetManualVolume");
        this.cbPMDSetManualVolume.addChangeListener(this::cbPMDSetManualVolume_CheckedChanged);
        //
        // cbPMDUsePPSDRV
        //
        this.cbPMDUsePPSDRV.setName("cbPMDUsePPSDRV");
        this.cbPMDUsePPSDRV.addChangeListener(this::cbPMDUsePPSDRV_CheckedChanged);
        //
        // cbPMDUsePPZ8
        //
        this.cbPMDUsePPZ8.setName("cbPMDUsePPZ8");
        //
        // gbPMDManual
        //
        this.gbPMDManual.setName("gbPMDManual");
        //
        // gbPMDSetManualVolume
        //
        this.gbPMDSetManualVolume.setName("gbPMDSetManualVolume");
        //
        // gbPPSDRV
        //
        this.gbPPSDRV.setName("gbPPSDRV");
        //
        // groupBox32
        //
        this.groupBox32.setName("groupBox32");
        //
        // groupBox33
        //
        this.groupBox33.setName("groupBox33");
        //
        // label54
        //
        this.label54.setName("label54");
        //
        // label55
        //
        this.label55.setName("label55");
        //
        // label56
        //
        this.label56.setName("label56");
        //
        // label57
        //
        this.label57.setName("label57");
        //
        // label58
        //
        this.label58.setName("label58");
        //
        // label59
        //
        this.label59.setName("label59");
        //
        // label60
        //
        this.label60.setName("label60");
        //
        // label61
        //
        this.label61.setName("label61");
        //
        // label62
        //
        this.label62.setName("label62");
        //
        // label63
        //
        this.label63.setName("label63");
        //
        // label64
        //
        this.label64.setName("label64");
        //
        // rbPMD86B
        //
        this.rbPMD86B.setName("rbPMD86B");
        //
        // rbPMDAuto
        //
        this.rbPMDAuto.setName("rbPMDAuto");
        //
        // rbPMDManual
        //
        this.rbPMDManual.setName("rbPMDManual");
        this.rbPMDManual.addChangeListener(this::rbPMDManual_CheckedChanged);
        //
        // rbPMDNrmB
        //
        this.rbPMDNrmB.setName("rbPMDNrmB");
        //
        // rbPMDSpbB
        //
        this.rbPMDSpbB.setName("rbPMDSpbB");
        //
        // rbPMDUsePPSDRVFreqDefault
        //
        this.rbPMDUsePPSDRVFreqDefault.setName("rbPMDUsePPSDRVFreqDefault");
        //
        // rbPMDUsePPSDRVManualFreq
        //
        this.rbPMDUsePPSDRVManualFreq.setName("rbPMDUsePPSDRVManualFreq");
        this.rbPMDUsePPSDRVManualFreq.addChangeListener(this::rbPMDUsePPSDRVManualFreq_CheckedChanged);
        //
        // tbPMDCompilerArguments
        //
        this.tbPMDCompilerArguments.setName("tbPMDCompilerArguments");
        //
        // tbPMDDriverArguments
        //
        this.tbPMDDriverArguments.setName("tbPMDDriverArguments");
        //
        // tbPMDPPSDRVFreq
        //
        this.tbPMDPPSDRVFreq.setName("tbPMDPPSDRVFreq");
        this.tbPMDPPSDRVFreq.addFocusListener(this.tbPMDPPSDRVFreq_Click);
        this.tbPMDPPSDRVFreq.addMouseListener(this.tbPMDPPSDRVFreq_MouseClick);
        //
        // tbPMDPPSDRVManualWait
        //
        this.tbPMDPPSDRVManualWait.setName("tbPMDPPSDRVManualWait");
        //
        // tbPMDVolumeAdpcm
        //
        this.tbPMDVolumeAdpcm.setName("tbPMDVolumeAdpcm");
        //
        // tbPMDVolumeFM
        //
        this.tbPMDVolumeFM.setName("tbPMDVolumeFM");
        //
        // tbPMDVolumeGIMICSSG
        //
        this.tbPMDVolumeGIMICSSG.setName("tbPMDVolumeGIMICSSG");
        //
        // tbPMDVolumeRhythm
        //
        this.tbPMDVolumeRhythm.setName("tbPMDVolumeRhythm");
        //
        // tbPMDVolumeSSG
        //
        this.tbPMDVolumeSSG.setName("tbPMDVolumeSSG");
        //
        // tpPMDDotNET
        //
        this.setName("tpPMDDotNET");
        // layout
        new Grid(Grid.group(this.groupBox32))
                .add(this.rbPMDNrmB).add(this.rbPMDSpbB).add(this.rbPMD86B).end();
        for (JTextField field : new JTextField[] {this.tbPMDVolumeFM, this.tbPMDVolumeSSG, this.tbPMDVolumeRhythm, this.tbPMDVolumeAdpcm, this.tbPMDVolumeGIMICSSG, this.tbPMDPPSDRVManualWait}) {
            field.setColumns(4);
        }
        new Grid(Grid.group(this.gbPMDSetManualVolume))
                .add(this.cbPMDSetManualVolume).next()
                .add(this.label59).next()
                .add(Grid.row(this.label61, this.tbPMDVolumeFM, this.label62, this.tbPMDVolumeSSG,
                        this.label63, this.tbPMDVolumeRhythm, this.label64, this.tbPMDVolumeAdpcm)).next()
                .add(Grid.row(this.label60, this.tbPMDVolumeGIMICSSG)).end();
        this.tbPMDPPSDRVFreq.setColumns(6);
        new Grid(Grid.group(this.groupBox33))
                .add(this.label56).add(this.rbPMDUsePPSDRVFreqDefault).next()
                .skip().add(Grid.row(this.rbPMDUsePPSDRVManualFreq, this.tbPMDPPSDRVFreq, this.label57)).next()
                .add(this.label58).add(Grid.row(this.tbPMDPPSDRVManualWait, this.btnPMDPPSDRVManualWait)).end();
        new Grid(Grid.group(this.gbPPSDRV))
                .add(this.cbPMDUsePPSDRV).next()
                .span(this.groupBox33).end();
        new Grid(Grid.group(this.gbPMDManual))
                .add(this.groupBox32).next()
                .span(this.gbPMDSetManualVolume).next()
                .span(this.gbPPSDRV).next()
                .add(this.cbPMDUsePPZ8).end();
        new Grid(this)
                .add(this.label55).fill(this.tbPMDCompilerArguments).add(this.btnPMDResetCompilerArhguments).next()
                .add(this.rbPMDAuto).next()
                .add(this.rbPMDManual).next()
                .span(this.gbPMDManual)
                .spring()
                .add(this.label54).fill(this.tbPMDDriverArguments).add(this.btnPMDResetDriverArguments).end();
    }

    @Override
    public void load(Setting setting) {
        tbPMDCompilerArguments.setText(setting.getPmd().compilerArguments);
        rbPMDAuto.setSelected(setting.getPmd().isAuto);
        rbPMDManual.setSelected(!setting.getPmd().isAuto);
        rbPMDNrmB.setSelected(setting.getPmd().soundBoard == 0);
        rbPMDSpbB.setSelected(setting.getPmd().soundBoard == 1);
        rbPMD86B.setSelected(setting.getPmd().soundBoard == 2);
        cbPMDSetManualVolume.setSelected(setting.getPmd().setManualVolume);
        cbPMDUsePPSDRV.setSelected(setting.getPmd().usePPSDRV);
        cbPMDUsePPZ8.setSelected(setting.getPmd().usePPZ8);
        tbPMDDriverArguments.setText(setting.getPmd().driverArguments);
        rbPMDUsePPSDRVFreqDefault.setSelected(setting.getPmd().usePPSDRVUseInterfaceDefaultFreq);
        rbPMDUsePPSDRVManualFreq.setSelected(!setting.getPmd().usePPSDRVUseInterfaceDefaultFreq);
        tbPMDPPSDRVFreq.setText(String.valueOf(setting.getPmd().ppsDrvManualFreq));
        tbPMDPPSDRVManualWait.setText(String.valueOf(setting.getPmd().ppsDrvManualWait));
        tbPMDVolumeFM.setText(String.valueOf(setting.getPmd().volumeFM));
        tbPMDVolumeSSG.setText(String.valueOf(setting.getPmd().volumeSSG));
        tbPMDVolumeRhythm.setText(String.valueOf(setting.getPmd().volumeRhythm));
        tbPMDVolumeAdpcm.setText(String.valueOf(setting.getPmd().volumeAdpcm));
        tbPMDVolumeGIMICSSG.setText(String.valueOf(setting.getPmd().volumeGIMICSSG));

        rbPMDManual_CheckedChanged(null);
        cbPMDSetManualVolume_CheckedChanged(null);
        cbPMDUsePPSDRV_CheckedChanged(null);
        rbPMDUsePPSDRVManualFreq_CheckedChanged(null);
    }

    @Override
    public void apply(Setting setting) {
        int nn;

        setting.getPmd().compilerArguments = tbPMDCompilerArguments.getText();
        setting.getPmd().isAuto = rbPMDAuto.isSelected();
        setting.getPmd().soundBoard = rbPMDNrmB.isSelected() ? 0 : (rbPMDSpbB.isSelected() ? 1 : 2);
        setting.getPmd().setManualVolume = cbPMDSetManualVolume.isSelected();
        setting.getPmd().usePPSDRV = cbPMDUsePPSDRV.isSelected();
        setting.getPmd().usePPZ8 = cbPMDUsePPZ8.isSelected();
        setting.getPmd().driverArguments = tbPMDDriverArguments.getText();
        setting.getPmd().usePPSDRVUseInterfaceDefaultFreq = rbPMDUsePPSDRVFreqDefault.isSelected();
        try {
            nn = Integer.parseInt(tbPMDPPSDRVFreq.getText());
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, e);
            nn = 2000;
        }
        setting.getPmd().ppsDrvManualFreq = nn;
        try {
            nn = Integer.parseInt(tbPMDPPSDRVManualWait.getText());
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, e);
            nn = 1;
        }
        nn = Math.clamp(nn, 0, 100);
        setting.getPmd().ppsDrvManualWait = nn;
        try {
            nn = Integer.parseInt(tbPMDVolumeFM.getText());
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, e);
            nn = 0;
        }
        nn = Math.clamp(nn, -191, 20);
        setting.getPmd().volumeFM = nn;
        try {
            nn = Integer.parseInt(tbPMDVolumeSSG.getText());
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, e);
            nn = 0;
        }
        nn = Math.clamp(nn, -191, 20);
        setting.getPmd().volumeSSG = nn;
        try {
            nn = Integer.parseInt(tbPMDVolumeRhythm.getText());
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, e);
            nn = 0;
        }
        nn = Math.clamp(nn, -191, 20);
        setting.getPmd().volumeRhythm = nn;
        try {
            nn = Integer.parseInt(tbPMDVolumeAdpcm.getText());
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, e);
            nn = 0;
        }
        nn = Math.clamp(nn, -191, 20);
        setting.getPmd().volumeAdpcm = nn;
        try {
            nn = Integer.parseInt(tbPMDVolumeGIMICSSG.getText());
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, e);
            nn = 31;
        }
        nn = Math.clamp(nn, 0, 127);
        setting.getPmd().volumeGIMICSSG = nn;
    }

    private void btnPMDPPSDRVManualWait_Click(ActionEvent ev) {
        tbPMDPPSDRVManualWait.setText("1");
    }

    private void btnPMDResetCompilerArhguments_Click(ActionEvent ev) {
        tbPMDCompilerArguments.setText("/v /C");
    }

    private void btnPMDResetDriverArguments_Click(ActionEvent ev) {
        tbPMDDriverArguments.setText("");
    }

    private void cbPMDSetManualVolume_CheckedChanged(ChangeEvent ev) {
        gbPMDSetManualVolume.setEnabled(cbPMDSetManualVolume.isSelected());
    }

    private void cbPMDUsePPSDRV_CheckedChanged(ChangeEvent ev) {
        gbPPSDRV.setEnabled(cbPMDUsePPSDRV.isSelected());
    }

    private void rbPMDManual_CheckedChanged(ChangeEvent ev) {
        gbPMDManual.setEnabled(rbPMDManual.isSelected());
    }

    private void rbPMDUsePPSDRVManualFreq_CheckedChanged(ChangeEvent ev) {
        tbPMDPPSDRVFreq.setEnabled(rbPMDUsePPSDRVManualFreq.isSelected());
    }

    private final FocusListener tbPMDPPSDRVFreq_Click = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            rbPMDUsePPSDRVManualFreq_CheckedChanged(null);
        }
    };

    private final MouseListener tbPMDPPSDRVFreq_MouseClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            rbPMDUsePPSDRVManualFreq_CheckedChanged(null);
        }
    };
}
