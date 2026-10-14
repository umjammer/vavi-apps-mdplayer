package mdplayer.form.sys.setting;

import java.awt.event.ActionEvent;
import java.lang.System.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import mdplayer.Setting;
import mdplayer.form.Grid;
import mdplayer.form.SettingTab;
import static java.lang.System.getLogger;

/** the "MIDIOut2" page of the settings dialog, split out of the original FormSetting */
public class SettingMIDIOut2Panel extends SettingTab {

    @Override
    public int order() {
        return 80;
    }

    private static final Logger logger = getLogger(SettingMIDIOut2Panel.class.getName());

    private final JPanel groupBox15;
    private final JButton btnBeforeSend_Default;
    private final JTextField tbBeforeSend_Custom;
    private final JTextField tbBeforeSend_XGReset;
    private final JLabel label34;
    private final JLabel label32;
    private final JTextField tbBeforeSend_GSReset;
    private final JLabel label33;
    private final JTextField tbBeforeSend_GMReset;
    private final JLabel label31;
    private final JLabel label35;

    public SettingMIDIOut2Panel() {
        this.groupBox15 = new JPanel();
        this.btnBeforeSend_Default = new JButton();
        this.tbBeforeSend_Custom = new JTextField();
        this.tbBeforeSend_XGReset = new JTextField();
        this.label35 = new JLabel();
        this.label34 = new JLabel();
        this.label32 = new JLabel();
        this.tbBeforeSend_GSReset = new JTextField();
        this.label33 = new JLabel();
        this.tbBeforeSend_GMReset = new JTextField();
        this.label31 = new JLabel();

        //
        // btnBeforeSend_Default
        //
        this.btnBeforeSend_Default.setName("btnBeforeSend_Default");
        this.btnBeforeSend_Default.addActionListener(this::btnBeforeSend_Default_Click);
        //
        // groupBox15
        //
        this.groupBox15.setName("groupBox15");
        //
        // label31
        //
        this.label31.setName("label31");
        //
        // label32
        //
        this.label32.setName("label32");
        //
        // label33
        //
        this.label33.setName("label33");
        //
        // label34
        //
        this.label34.setName("label34");
        //
        // label35
        //
        this.label35.setName("label35");
        //
        // tbBeforeSend_Custom
        //
        this.tbBeforeSend_Custom.setName("tbBeforeSend_Custom");
        //
        // tbBeforeSend_GMReset
        //
        this.tbBeforeSend_GMReset.setName("tbBeforeSend_GMReset");
        //
        // tbBeforeSend_GSReset
        //
        this.tbBeforeSend_GSReset.setName("tbBeforeSend_GSReset");
        //
        // tbBeforeSend_XGReset
        //
        this.tbBeforeSend_XGReset.setName("tbBeforeSend_XGReset");
        //
        // tpMIDIOut2
        //
        this.setName("tpMIDIOut2");
        // layout
        new Grid(Grid.group(this.groupBox15))
                .add(this.label31).fill(this.tbBeforeSend_GMReset).next()
                .add(this.label32).fill(this.tbBeforeSend_XGReset).next()
                .add(this.label33).fill(this.tbBeforeSend_GSReset).next()
                .add(this.label34).fill(this.tbBeforeSend_Custom).next()
                .span(Grid.trailing(this.btnBeforeSend_Default)).next()
                .span(this.label35).end();
        new Grid(this)
                .span(this.groupBox15).end();
    }

    @Override
    public void load(Setting setting) {
        tbBeforeSend_GMReset.setText(setting.getMidiOut().getGMReset());
        tbBeforeSend_XGReset.setText(setting.getMidiOut().getXGReset());
        tbBeforeSend_GSReset.setText(setting.getMidiOut().getGSReset());
        tbBeforeSend_Custom.setText(setting.getMidiOut().getCustom());
    }

    @Override
    public void apply(Setting setting) {
        setting.getMidiOut().setGMReset(tbBeforeSend_GMReset.getText());
        setting.getMidiOut().setXGReset(tbBeforeSend_XGReset.getText());
        setting.getMidiOut().setGSReset(tbBeforeSend_GSReset.getText());
        setting.getMidiOut().setCustom(tbBeforeSend_Custom.getText());
    }

    private void btnBeforeSend_Default_Click(ActionEvent ev) {
        Setting.MidiOut mo = new Setting.MidiOut();
        tbBeforeSend_GMReset.setText(mo.getGMReset());
        tbBeforeSend_XGReset.setText(mo.getXGReset());
        tbBeforeSend_GSReset.setText(mo.getGSReset());
        tbBeforeSend_Custom.setText(mo.getCustom());
    }
}
