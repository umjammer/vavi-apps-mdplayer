package mdplayer.form.sys.setting;

import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import mdplayer.Setting;
import mdplayer.form.Grid;
import mdplayer.form.SettingTab;
import static java.lang.System.getLogger;

/** the "SID" page of the settings dialog, split out of the original FormSetting */
public class SettingSIDPanel extends SettingTab {

    @Override
    public int order() {
        return 50;
    }

    private static final Logger logger = getLogger(SettingSIDPanel.class.getName());

    private final JPanel groupBox13;
    private final JLabel label22;
    private final JButton btnSIDCharacter;
    private final JButton btnSIDBasic;
    private final JButton btnSIDKernal;
    private final JTextField tbSIDCharacter;
    private final JTextField tbSIDBasic;
    private final JTextField tbSIDKernal;
    private final JLabel label24;
    private final JLabel label23;
    private final JPanel groupBox14;
    private final JLabel label27;
    private final JLabel label26;
    private final JLabel label25;
    private final JCheckBox rdSIDQ1;
    private final JCheckBox rdSIDQ3;
    private final JCheckBox rdSIDQ2;
    private final JCheckBox rdSIDQ4;
    private final JTextField tbSIDOutputBufferSize;
    private final JLabel label49;
    private final JLabel label51;
    private final JPanel groupBox28;
    private final JPanel groupBox27;
    private final JCheckBox rbSIDC64Model_PAL;
    private final JCheckBox rbSIDC64Model_DREAN;
    private final JCheckBox rbSIDC64Model_OLDNTSC;
    private final JCheckBox rbSIDC64Model_NTSC;
    private final JCheckBox rbSIDModel_8580;
    private final JCheckBox rbSIDModel_6581;
    private final JCheckBox cbSIDC64Model_Force;
    private final JCheckBox cbSIDModel_Force;
    private final JLabel lblSIDSongLength;
    private final JTextField tbSIDSongLength;
    private final JButton btnSIDSongLength;

    public SettingSIDPanel() {
        this.groupBox28 = new JPanel();
        this.cbSIDModel_Force = new JCheckBox();
        this.rbSIDModel_8580 = new JCheckBox();
        this.rbSIDModel_6581 = new JCheckBox();
        this.groupBox27 = new JPanel();
        this.cbSIDC64Model_Force = new JCheckBox();
        this.rbSIDC64Model_DREAN = new JCheckBox();
        this.rbSIDC64Model_OLDNTSC = new JCheckBox();
        this.rbSIDC64Model_NTSC = new JCheckBox();
        this.rbSIDC64Model_PAL = new JCheckBox();
        this.groupBox14 = new JPanel();
        this.label27 = new JLabel();
        this.label26 = new JLabel();
        this.label25 = new JLabel();
        this.rdSIDQ1 = new JCheckBox();
        this.rdSIDQ3 = new JCheckBox();
        this.rdSIDQ2 = new JCheckBox();
        this.rdSIDQ4 = new JCheckBox();
        this.groupBox13 = new JPanel();
        this.btnSIDBasic = new JButton();
        this.btnSIDCharacter = new JButton();
        this.btnSIDKernal = new JButton();
        this.tbSIDCharacter = new JTextField();
        this.tbSIDBasic = new JTextField();
        this.tbSIDKernal = new JTextField();
        this.label24 = new JLabel();
        this.label23 = new JLabel();
        this.label22 = new JLabel();
        this.tbSIDOutputBufferSize = new JTextField();
        this.label51 = new JLabel();
        this.label49 = new JLabel();
        this.lblSIDSongLength = new JLabel();
        this.tbSIDSongLength = new JTextField();
        this.btnSIDSongLength = new JButton();

        //
        // btnSIDBasic
        //
        this.btnSIDBasic.setName("btnSIDBasic");
        this.btnSIDBasic.addActionListener(this::btnSIDBasic_Click);
        //
        // btnSIDSongLength
        //
        this.btnSIDSongLength.setName("btnSIDSongLength");
        this.btnSIDSongLength.addActionListener(this::btnSIDSongLength_Click);
        //
        // btnSIDCharacter
        //
        this.btnSIDCharacter.setName("btnSIDCharacter");
        this.btnSIDCharacter.addActionListener(this::btnSIDCharacter_Click);
        //
        // btnSIDKernal
        //
        this.btnSIDKernal.setName("btnSIDKernal");
        this.btnSIDKernal.addActionListener(this::btnSIDKernal_Click);
        //
        // cbSIDC64Model_Force
        //
        this.cbSIDC64Model_Force.setName("cbSIDC64Model_Force");
        //
        // cbSIDModel_Force
        //
        this.cbSIDModel_Force.setName("cbSIDModel_Force");
        //
        // groupBox13
        //
        this.groupBox13.setName("groupBox13");
        //
        // groupBox14
        //
        this.groupBox14.setName("groupBox14");
        //
        // groupBox27
        //
        this.groupBox27.setName("groupBox27");
        //
        // groupBox28
        //
        this.groupBox28.setName("groupBox28");
        //
        // label22
        //
        this.label22.setName("label22");
        //
        // label23
        //
        this.label23.setName("label23");
        //
        // label24
        //
        this.label24.setName("label24");
        //
        // label25
        //
        this.label25.setName("label25");
        //
        // label26
        //
        this.label26.setName("label26");
        //
        // label27
        //
        this.label27.setName("label27");
        //
        // label49
        //
        this.label49.setName("label49");
        //
        // label51
        //
        this.label51.setName("label51");
        //
        // rbSIDC64Model_DREAN
        //
        this.rbSIDC64Model_DREAN.setName("rbSIDC64Model_DREAN");
        //
        // rbSIDC64Model_NTSC
        //
        this.rbSIDC64Model_NTSC.setName("rbSIDC64Model_NTSC");
        //
        // rbSIDC64Model_OLDNTSC
        //
        this.rbSIDC64Model_OLDNTSC.setName("rbSIDC64Model_OLDNTSC");
        //
        // rbSIDC64Model_PAL
        //
        this.rbSIDC64Model_PAL.setSelected(true);
        this.rbSIDC64Model_PAL.setName("rbSIDC64Model_PAL");
        //
        // rbSIDModel_6581
        //
        this.rbSIDModel_6581.setSelected(true);
        this.rbSIDModel_6581.setName("rbSIDModel_6581");
        //
        // rbSIDModel_8580
        //
        this.rbSIDModel_8580.setName("rbSIDModel_8580");
        //
        // rdSIDQ1
        //
        this.rdSIDQ1.setSelected(true);
        this.rdSIDQ1.setName("rdSIDQ1");
        //
        // rdSIDQ2
        //
        this.rdSIDQ2.setName("rdSIDQ2");
        //
        // rdSIDQ3
        //
        this.rdSIDQ3.setName("rdSIDQ3");
        //
        // rdSIDQ4
        //
        this.rdSIDQ4.setName("rdSIDQ4");
        //
        // tbSIDBasic
        //
        this.tbSIDBasic.setName("tbSIDBasic");
        //
        // tbSIDCharacter
        //
        this.tbSIDCharacter.setName("tbSIDCharacter");
        //
        // tbSIDKernal
        //
        this.tbSIDKernal.setName("tbSIDKernal");
        //
        // lblSIDSongLength
        //
        this.lblSIDSongLength.setName("lblSIDSongLength");
        //
        // tbSIDSongLength
        //
        this.tbSIDSongLength.setName("tbSIDSongLength");
        //
        // tbSIDOutputBufferSize
        //
        this.tbSIDOutputBufferSize.setName("tbSIDOutputBufferSize");
        //
        // tpSID
        //
        this.setName("tpSID");
        // layout
        new Grid(Grid.group(this.groupBox13))
                .add(this.label22).fill(this.tbSIDKernal).add(this.btnSIDKernal).next()
                .add(this.label23).fill(this.tbSIDBasic).add(this.btnSIDBasic).next()
                .add(this.label24).fill(this.tbSIDCharacter).add(this.btnSIDCharacter).end();
        new Grid(Grid.group(this.groupBox14))
                .add(this.rdSIDQ4).add(this.label25).next()
                .add(this.rdSIDQ3).next()
                .add(this.rdSIDQ2).add(this.label26).next()
                .add(this.rdSIDQ1).add(this.label27).end();
        new Grid(Grid.group(this.groupBox27))
                .add(this.rbSIDC64Model_PAL).add(this.rbSIDC64Model_NTSC).next()
                .add(this.rbSIDC64Model_OLDNTSC).add(this.rbSIDC64Model_DREAN).next()
                .skip().add(this.cbSIDC64Model_Force).end();
        new Grid(Grid.group(this.groupBox28))
                .add(this.rbSIDModel_6581).next()
                .add(this.rbSIDModel_8580).next()
                .add(this.cbSIDModel_Force).end();
        this.tbSIDOutputBufferSize.setColumns(8);
        new Grid(this)
                .span(this.groupBox13).next()
                .add(this.groupBox14, 3).next()
                .add(this.label49).span(Grid.row(this.tbSIDOutputBufferSize, this.label51)).next()
                .span(Grid.row(this.groupBox27, this.groupBox28)).next()
                .add(this.lblSIDSongLength).fill(this.tbSIDSongLength).add(this.btnSIDSongLength).end();
    }

    @Override
    public void load(Setting setting) {
        tbSIDKernal.setText(setting.getSid().romKernalPath);
        tbSIDBasic.setText(setting.getSid().romBasicPath);
        tbSIDCharacter.setText(setting.getSid().romCharacterPath);
        tbSIDSongLength.setText(setting.getSid().songLengthPath);
        switch (setting.getSid().quality) {
        case 0:
            rdSIDQ1.setSelected(true);
            break;
        case 1:
            rdSIDQ2.setSelected(true);
            break;
        case 2:
            rdSIDQ3.setSelected(true);
            break;
        case 3:
            rdSIDQ4.setSelected(true);
            break;
        }
        tbSIDOutputBufferSize.setText(String.valueOf(setting.getSid().outputBufferSize));
        rbSIDC64Model_PAL.setSelected((setting.getSid().c64model == 0));
        rbSIDC64Model_NTSC.setSelected((setting.getSid().c64model == 1));
        rbSIDC64Model_OLDNTSC.setSelected((setting.getSid().c64model == 2));
        rbSIDC64Model_DREAN.setSelected((setting.getSid().c64model == 3));
        cbSIDC64Model_Force.setSelected(setting.getSid().c64modelForce);
        rbSIDModel_6581.setSelected((setting.getSid().sidModel == 0));
        rbSIDModel_8580.setSelected((setting.getSid().sidModel == 1));
        cbSIDModel_Force.setSelected(setting.getSid().sidmodelForce);
    }

    @Override
    public void apply(Setting setting) {
        // the page starts from a fresh SID, so anything it has no control for - the give-up time
        // the sid driver ends undetectable tunes by - has to be carried over by hand
        int maxPlayTime = setting.getSid().maxPlayTime;
        setting.setSid(new Setting.SID());
        setting.getSid().maxPlayTime = maxPlayTime;
        setting.getSid().romKernalPath = tbSIDKernal.getText();
        setting.getSid().romBasicPath = tbSIDBasic.getText();
        setting.getSid().romCharacterPath = tbSIDCharacter.getText();
        setting.getSid().songLengthPath = tbSIDSongLength.getText();
        if (rdSIDQ1.isSelected()) setting.getSid().quality = 0;
        if (rdSIDQ2.isSelected()) setting.getSid().quality = 1;
        if (rdSIDQ3.isSelected()) setting.getSid().quality = 2;
        if (rdSIDQ4.isSelected()) setting.getSid().quality = 3;
        try {
            setting.getSid().outputBufferSize = Math.clamp(Integer.parseInt(tbSIDOutputBufferSize.getText()), 100, 999999);
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            setting.getSid().outputBufferSize = 5000;
        }
        setting.getSid().c64model = rbSIDC64Model_PAL.isSelected() ? 0 : (
                rbSIDC64Model_NTSC.isSelected() ? 1 : (
                        rbSIDC64Model_OLDNTSC.isSelected() ? 2 : (
                                rbSIDC64Model_DREAN.isSelected() ? 3 : 0)));
        setting.getSid().c64modelForce = cbSIDC64Model_Force.isSelected();
        setting.getSid().sidModel = rbSIDModel_6581.isSelected() ? 0 : (
                rbSIDModel_8580.isSelected() ? 1 : 0);
        setting.getSid().sidmodelForce = cbSIDModel_Force.isSelected();
    }

    private void btnSIDSongLength_Click(ActionEvent ev) {
        choose(tbSIDSongLength, "Select the songlength database (Songlengths.md5)");
    }

    private void btnSIDBasic_Click(ActionEvent ev) {
        choose(tbSIDBasic, "Select a file");
    }

    private void btnSIDCharacter_Click(ActionEvent ev) {
        choose(tbSIDCharacter, "Select a file");
    }

    private void btnSIDKernal_Click(ActionEvent ev) {
        choose(tbSIDKernal, "Select a file");
    }

    /**
     * Lets the user pick the file a path box holds.
     * <p>
     * The whole path goes in, not just the file name the browse buttons used to leave there: the
     * driver opens what the box says, and a bare name only ever resolves by accident.
     */
    private void choose(JTextField box, String title) {
        JFileChooser ofd = new JFileChooser();
        ofd.setFileFilter(ofd.getAcceptAllFileFilter());
        ofd.setDialogTitle(title);
        ofd.setMultiSelectionEnabled(false);
        if (!box.getText().isEmpty()) {
            ofd.setSelectedFile(new File(box.getText()));
        }

        if (ofd.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        box.setText(ofd.getSelectedFile().getAbsolutePath());
    }
}
