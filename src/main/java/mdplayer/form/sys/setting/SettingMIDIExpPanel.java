package mdplayer.form.sys.setting;

import java.awt.event.ActionEvent;
import java.lang.System.Logger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import mdplayer.Setting;
import mdplayer.form.Grid;
import mdplayer.form.SettingTab;
import static java.lang.System.getLogger;

/** the "tabMIDIExp" page of the settings dialog, split out of the original FormSetting */
public class SettingMIDIExpPanel extends SettingTab {

    @Override
    public int order() {
        return 90;
    }

    private static final Logger logger = getLogger(SettingMIDIExpPanel.class.getName());

    private final JCheckBox cbUseMIDIExport;
    private final JPanel gbMIDIExport;
    private final JCheckBox cbMIDIUseVOPM;
    private final JPanel groupBox6;
    private final JCheckBox cbMIDIYM2612;
    private final JCheckBox cbMIDISN76489Sec;
    private final JCheckBox cbMIDIYM2612Sec;
    private final JCheckBox cbMIDISN76489;
    private final JCheckBox cbMIDIYM2151;
    private final JCheckBox cbMIDIYM2610BSec;
    private final JCheckBox cbMIDIYM2151Sec;
    private final JCheckBox cbMIDIYM2610B;
    private final JCheckBox cbMIDIYM2203;
    private final JCheckBox cbMIDIYM2608Sec;
    private final JCheckBox cbMIDIYM2203Sec;
    private final JCheckBox cbMIDIYM2608;
    private final JCheckBox cbMIDIPlayless;
    private final JButton btnMIDIOutputPath;
    private final JLabel lblOutputPath;
    private final JTextField tbMIDIOutputPath;
    private final JCheckBox cbMIDIKeyOnFnum;

    public SettingMIDIExpPanel() {
        this.cbUseMIDIExport = new JCheckBox();
        this.gbMIDIExport = new JPanel();
        this.cbMIDIKeyOnFnum = new JCheckBox();
        this.cbMIDIUseVOPM = new JCheckBox();
        this.groupBox6 = new JPanel();
        this.cbMIDIYM2612 = new JCheckBox();
        this.cbMIDISN76489Sec = new JCheckBox();
        this.cbMIDIYM2612Sec = new JCheckBox();
        this.cbMIDISN76489 = new JCheckBox();
        this.cbMIDIYM2151 = new JCheckBox();
        this.cbMIDIYM2610BSec = new JCheckBox();
        this.cbMIDIYM2151Sec = new JCheckBox();
        this.cbMIDIYM2610B = new JCheckBox();
        this.cbMIDIYM2203 = new JCheckBox();
        this.cbMIDIYM2608Sec = new JCheckBox();
        this.cbMIDIYM2203Sec = new JCheckBox();
        this.cbMIDIYM2608 = new JCheckBox();
        this.cbMIDIPlayless = new JCheckBox();
        this.btnMIDIOutputPath = new JButton();
        this.lblOutputPath = new JLabel();
        this.tbMIDIOutputPath = new JTextField();

        //
        // btnMIDIOutputPath
        //
        this.btnMIDIOutputPath.setName("btnMIDIOutputPath");
        this.btnMIDIOutputPath.addActionListener(this::btnMIDIOutputPath_Click);
        //
        // cbMIDIKeyOnFnum
        //
        this.cbMIDIKeyOnFnum.setName("cbMIDIKeyOnFnum");
        //
        // cbMIDIPlayless
        //
        this.cbMIDIPlayless.setName("cbMIDIPlayless");
        //
        // cbMIDISN76489
        //
        this.cbMIDISN76489.setName("cbMIDISN76489");
        //
        // cbMIDISN76489Sec
        //
        this.cbMIDISN76489Sec.setName("cbMIDISN76489Sec");
        //
        // cbMIDIUseVOPM
        //
        this.cbMIDIUseVOPM.setName("cbMIDIUseVOPM");
        //
        // cbMIDIYM2151
        //
        this.cbMIDIYM2151.setName("cbMIDIYM2151");
        //
        // cbMIDIYM2151Sec
        //
        this.cbMIDIYM2151Sec.setName("cbMIDIYM2151Sec");
        //
        // cbMIDIYM2203
        //
        this.cbMIDIYM2203.setName("cbMIDIYM2203");
        //
        // cbMIDIYM2203Sec
        //
        this.cbMIDIYM2203Sec.setName("cbMIDIYM2203Sec");
        //
        // cbMIDIYM2608
        //
        this.cbMIDIYM2608.setName("cbMIDIYM2608");
        //
        // cbMIDIYM2608Sec
        //
        this.cbMIDIYM2608Sec.setName("cbMIDIYM2608Sec");
        //
        // cbMIDIYM2610B
        //
        this.cbMIDIYM2610B.setName("cbMIDIYM2610B");
        //
        // cbMIDIYM2610BSec
        //
        this.cbMIDIYM2610BSec.setName("cbMIDIYM2610BSec");
        //
        // cbMIDIYM2612
        //
        this.cbMIDIYM2612.setSelected(true);
        this.cbMIDIYM2612.setName("cbMIDIYM2612");
        //
        // cbMIDIYM2612Sec
        //
        this.cbMIDIYM2612Sec.setName("cbMIDIYM2612Sec");
        //
        // cbUseMIDIExport
        //
        this.cbUseMIDIExport.setName("cbUseMIDIExport");
        this.cbUseMIDIExport.addChangeListener(this::cbUseMIDIExport_CheckedChanged);
        //
        // gbMIDIExport
        //
        this.gbMIDIExport.setName("gbMIDIExport");
        //
        // groupBox6
        //
        this.groupBox6.setName("groupBox6");
        //
        // lblOutputPath
        //
        this.lblOutputPath.setName("lblOutputPath");
        //
        // tabMIDIExp
        //
        this.setName("tabMIDIExp");
        // layout
        new Grid(Grid.group(this.groupBox6))
                .add(this.cbMIDIYM2612).add(this.cbMIDIYM2612Sec).next()
                .add(this.cbMIDIYM2151).add(this.cbMIDIYM2151Sec).next()
                .add(this.cbMIDIYM2203).add(this.cbMIDIYM2203Sec).next()
                .add(this.cbMIDIYM2608).add(this.cbMIDIYM2608Sec).next()
                .add(this.cbMIDIYM2610B).add(this.cbMIDIYM2610BSec).next()
                .add(this.cbMIDISN76489).add(this.cbMIDISN76489Sec).end();
        new Grid(Grid.group(this.gbMIDIExport))
                .add(this.cbMIDIPlayless, 3).next()
                .add(this.cbMIDIUseVOPM, 3).next()
                .add(this.cbMIDIKeyOnFnum, 3).next()
                .add(this.lblOutputPath).fill(this.tbMIDIOutputPath).add(this.btnMIDIOutputPath).next()
                .add(this.groupBox6, 3).end();
        new Grid(this)
                .add(this.cbUseMIDIExport).next()
                .grow(this.gbMIDIExport);
        //
        // tbMIDIOutputPath
        //
        this.tbMIDIOutputPath.setName("tbMIDIOutputPath");
    }

    @Override
    public void load(Setting setting) {
        cbUseMIDIExport.setSelected(setting.getMidiExport().getUseMIDIExport());
        gbMIDIExport.setEnabled(cbUseMIDIExport.isSelected());
        tbMIDIOutputPath.setText(setting.getMidiExport().getExportPath());
        cbMIDIUseVOPM.setSelected(setting.getMidiExport().getUseVOPMex());
        cbMIDIKeyOnFnum.setSelected(setting.getMidiExport().getKeyOnFnum());
        cbMIDIYM2151.setSelected(setting.getMidiExport().getUseYM2151Export());
        cbMIDIYM2612.setSelected(setting.getMidiExport().getUseYM2612Export());
    }

    @Override
    public void apply(Setting setting) {
        setting.getMidiExport().setUseMIDIExport(cbUseMIDIExport.isSelected());
        setting.getMidiExport().setExportPath(tbMIDIOutputPath.getText());
        setting.getMidiExport().setUseVOPMex(cbMIDIUseVOPM.isSelected());
        setting.getMidiExport().setKeyOnFnum(cbMIDIKeyOnFnum.isSelected());
        setting.getMidiExport().setUseYM2151Export(cbMIDIYM2151.isSelected());
        setting.getMidiExport().setUseYM2612Export(cbMIDIYM2612.isSelected());
    }

    private void btnMIDIOutputPath_Click(ActionEvent ev) {
        JFileChooser fbd = new JFileChooser();
        fbd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fbd.setDialogTitle("Please specify a folder.");

        if (fbd.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        tbMIDIOutputPath.setText(fbd.getSelectedFile().getPath());
    }

    private void cbUseMIDIExport_CheckedChanged(ChangeEvent ev) {
        gbMIDIExport.setEnabled(cbUseMIDIExport.isSelected());
    }
}
