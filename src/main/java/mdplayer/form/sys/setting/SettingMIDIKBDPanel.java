package mdplayer.form.sys.setting;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.form.Grid;
import mdplayer.form.SettingTab;
import mdplayer.form.sys.FormSetting;

import static java.lang.System.getLogger;

/** the "MIDIKBD" page of the settings dialog, split out of the original FormSetting */
public class SettingMIDIKBDPanel extends SettingTab {

    @Override
    public int order() {
        return 100;
    }

    private static final Logger logger = getLogger(SettingMIDIKBDPanel.class.getName());

    private final JPanel gbMIDIKeyboard;
    private final JPanel gbUseChannel;
    private final JCheckBox cbFM1;
    private final JCheckBox cbFM2;
    private final JCheckBox cbFM3;
    private final JCheckBox cbUseMIDIKeyboard;
    private final JCheckBox cbFM4;
    private final JCheckBox cbFM5;
    private final JCheckBox cbFM6;
    private final JComboBox<String> cmbMIDIIN;
    private final JLabel label5;
    private final JCheckBox rbMONO;
    private final JCheckBox rbPOLY;
    private final JPanel groupBox7;
    private final JCheckBox rbFM6;
    private final JCheckBox rbFM3;
    private final JCheckBox rbFM5;
    private final JCheckBox rbFM2;
    private final JCheckBox rbFM4;
    private final JCheckBox rbFM1;
    private final JPanel groupBox2;
    private final JTextField tbCCFadeout;
    private final JTextField tbCCPause;
    private final JTextField tbCCSlow;
    private final JTextField tbCCPrevious;
    private final JTextField tbCCNext;
    private final JTextField tbCCFast;
    private final JTextField tbCCStop;
    private final JTextField tbCCPlay;
    private final JTextField tbCCCopyLog;
    private final JLabel label17;
    private final JTextField tbCCDelLog;
    private final JLabel label15;
    private final JTextField tbCCChCopy;
    private final JLabel label9;
    private final JLabel label8;
    private final JLabel pictureBox1;
    private final JLabel pictureBox4;
    private final JLabel pictureBox3;
    private final JLabel pictureBox2;
    private final JLabel pictureBox8;
    private final JLabel pictureBox7;
    private final JLabel pictureBox6;
    private final JLabel pictureBox5;

    public SettingMIDIKBDPanel() {
        this.cbUseMIDIKeyboard = new JCheckBox();
        this.gbMIDIKeyboard = new JPanel();
        this.pictureBox8 = new JLabel();
        this.pictureBox7 = new JLabel();
        this.pictureBox6 = new JLabel();
        this.pictureBox5 = new JLabel();
        this.pictureBox4 = new JLabel();
        this.pictureBox3 = new JLabel();
        this.pictureBox2 = new JLabel();
        this.pictureBox1 = new JLabel();
        this.tbCCFadeout = new JTextField();
        this.tbCCPause = new JTextField();
        this.tbCCSlow = new JTextField();
        this.tbCCPrevious = new JTextField();
        this.tbCCNext = new JTextField();
        this.tbCCFast = new JTextField();
        this.tbCCStop = new JTextField();
        this.tbCCPlay = new JTextField();
        this.tbCCCopyLog = new JTextField();
        this.label17 = new JLabel();
        this.tbCCDelLog = new JTextField();
        this.label15 = new JLabel();
        this.tbCCChCopy = new JTextField();
        this.label8 = new JLabel();
        this.label9 = new JLabel();
        this.gbUseChannel = new JPanel();
        this.rbMONO = new JCheckBox();
        this.rbPOLY = new JCheckBox();
        this.groupBox7 = new JPanel();
        this.rbFM6 = new JCheckBox();
        this.rbFM3 = new JCheckBox();
        this.rbFM5 = new JCheckBox();
        this.rbFM2 = new JCheckBox();
        this.rbFM4 = new JCheckBox();
        this.rbFM1 = new JCheckBox();
        this.groupBox2 = new JPanel();
        this.cbFM1 = new JCheckBox();
        this.cbFM6 = new JCheckBox();
        this.cbFM2 = new JCheckBox();
        this.cbFM5 = new JCheckBox();
        this.cbFM3 = new JCheckBox();
        this.cbFM4 = new JCheckBox();
        this.cmbMIDIIN = new JComboBox<>();
        this.label5 = new JLabel();

        //
        // cbFM1
        //
        this.cbFM1.setSelected(true);
        this.cbFM1.setName("cbFM1");
        //
        // cbFM2
        //
        this.cbFM2.setSelected(true);
        this.cbFM2.setName("cbFM2");
        //
        // cbFM3
        //
        this.cbFM3.setSelected(true);
        this.cbFM3.setName("cbFM3");
        //
        // cbFM4
        //
        this.cbFM4.setSelected(true);
        this.cbFM4.setName("cbFM4");
        //
        // cbFM5
        //
        this.cbFM5.setSelected(true);
        this.cbFM5.setName("cbFM5");
        //
        // cbFM6
        //
        this.cbFM6.setSelected(true);
        this.cbFM6.setName("cbFM6");
        //
        // cbUseMIDIKeyboard
        //
        this.cbUseMIDIKeyboard.setName("cbUseMIDIKeyboard");
        this.cbUseMIDIKeyboard.addChangeListener(this::cbUseMIDIKeyboard_CheckedChanged);
        //
        // cmbMIDIIN
        //
        this.cmbMIDIIN.setName("cmbMIDIIN");
        //
        // gbMIDIKeyboard
        //
        this.gbMIDIKeyboard.setName("gbMIDIKeyboard");
        //
        // gbUseChannel
        //
        this.gbUseChannel.setName("gbUseChannel");
        //
        // groupBox2
        //
        this.groupBox2.setName("groupBox2");
        //
        // groupBox7
        //
        this.groupBox7.setName("groupBox7");
        //
        // label15
        //
        this.label15.setName("label15");
        //
        // label17
        //
        this.label17.setName("label17");
        //
        // label5
        //
        this.label5.setName("label5");
        //
        // label8
        //
        this.label8.setName("label8");
        //
        // label9
        //
        this.label9.setName("label9");
        //
        // pictureBox1
        //
        this.pictureBox1.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.pictureBox1.setName("pictureBox1");
        //
        // pictureBox2
        //
        this.pictureBox2.setIcon(new ImageIcon(Common.getImage("ccPrevious")));
        this.pictureBox2.setName("pictureBox2");
        //
        // pictureBox3
        //
        this.pictureBox3.setIcon(new ImageIcon(Common.getImage("ccPause")));
        this.pictureBox3.setName("pictureBox3");
        //
        // pictureBox4
        //
        this.pictureBox4.setIcon(new ImageIcon(Common.getImage("ccStop")));
        this.pictureBox4.setName("pictureBox4");
        //
        // pictureBox5
        //
        this.pictureBox5.setIcon(new ImageIcon(Common.getImage("ccSlow")));
        this.pictureBox5.setName("pictureBox5");
        //
        // pictureBox6
        //
        this.pictureBox6.setIcon(new ImageIcon(Common.getImage("ccPlay")));
        this.pictureBox6.setName("pictureBox6");
        //
        // pictureBox7
        //
        this.pictureBox7.setIcon(new ImageIcon(Common.getImage("ccFast")));
        this.pictureBox7.setName("pictureBox7");
        //
        // pictureBox8
        //
        this.pictureBox8.setIcon(new ImageIcon(Common.getImage("ccNext")));
        this.pictureBox8.setName("pictureBox8");
        //
        // rbFM1
        //
        this.rbFM1.setSelected(true);
        this.rbFM1.setName("rbFM1");
        //
        // rbFM2
        //
        this.rbFM2.setName("rbFM2");
        //
        // rbFM3
        //
        this.rbFM3.setName("rbFM3");
        //
        // rbFM4
        //
        this.rbFM4.setName("rbFM4");
        //
        // rbFM5
        //
        this.rbFM5.setName("rbFM5");
        //
        // rbFM6
        //
        this.rbFM6.setName("rbFM6");
        //
        // rbMONO
        //
        this.rbMONO.setSelected(true);
        this.rbMONO.setName("rbMONO");
        //
        // rbPOLY
        //
        this.rbPOLY.setName("rbPOLY");
        //
        // tbCCChCopy
        //
        this.tbCCChCopy.setName("tbCCChCopy");
        //
        // tbCCCopyLog
        //
        this.tbCCCopyLog.setName("tbCCCopyLog");
        //
        // tbCCDelLog
        //
        this.tbCCDelLog.setName("tbCCDelLog");
        //
        // tbCCFadeout
        //
        this.tbCCFadeout.setName("tbCCFadeout");
        //
        // tbCCFast
        //
        this.tbCCFast.setName("tbCCFast");
        //
        // tbCCNext
        //
        this.tbCCNext.setName("tbCCNext");
        //
        // tbCCPause
        //
        this.tbCCPause.setName("tbCCPause");
        //
        // tbCCPlay
        //
        this.tbCCPlay.setName("tbCCPlay");
        //
        // tbCCPrevious
        //
        this.tbCCPrevious.setName("tbCCPrevious");
        //
        // tbCCSlow
        //
        this.tbCCSlow.setName("tbCCSlow");
        //
        // tbCCStop
        //
        this.tbCCStop.setName("tbCCStop");
        //
        // tpMIDIKBD
        //
        this.setName("tpMIDIKBD");
        // layout
        new Grid(Grid.group(this.groupBox7))
                .add(this.rbFM1).add(this.rbFM2).add(this.rbFM3).next()
                .add(this.rbFM4).add(this.rbFM5).add(this.rbFM6).end();
        new Grid(Grid.group(this.groupBox2))
                .add(this.cbFM1).add(this.cbFM2).add(this.cbFM3).next()
                .add(this.cbFM4).add(this.cbFM5).add(this.cbFM6).end();
        new Grid(Grid.group(this.gbUseChannel))
                .add(this.rbMONO).add(this.rbPOLY).next()
                .add(this.groupBox7).add(this.groupBox2).end();
        for (JTextField field : new JTextField[] {this.tbCCChCopy, this.tbCCDelLog, this.tbCCCopyLog,
                this.tbCCStop, this.tbCCPause, this.tbCCFadeout, this.tbCCPrevious,
                this.tbCCSlow, this.tbCCPlay, this.tbCCFast, this.tbCCNext}) {
            field.setColumns(3);
        }
        JPanel transport = Grid.panel()
                .add(this.tbCCStop).add(this.pictureBox4).add(this.tbCCPause).add(this.pictureBox3)
                .add(this.tbCCFadeout).add(this.pictureBox1).add(this.tbCCPrevious).add(this.pictureBox2).next()
                .add(this.tbCCSlow).add(this.pictureBox5).add(this.tbCCPlay).add(this.pictureBox6)
                .add(this.tbCCFast).add(this.pictureBox7).add(this.tbCCNext).add(this.pictureBox8).build();
        new Grid(Grid.group(this.gbMIDIKeyboard))
                .add(this.label5).fill(this.cmbMIDIIN).next()
                .span(this.gbUseChannel).next()
                .span(this.label8).next()
                .add(this.tbCCChCopy).span(this.label9).next()
                .add(this.tbCCDelLog).span(this.label15).next()
                .add(this.tbCCCopyLog).span(this.label17).next()
                .span(transport).end();
        new Grid(this)
                .add(this.cbUseMIDIKeyboard).next()
                .grow(this.gbMIDIKeyboard);
    }

    @Override
    public void load(Setting setting) {
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                ((DefaultComboBoxModel<String>) cmbMIDIIN.getModel()).addElement(device.getDeviceInfo().getName());
            } catch (MidiUnavailableException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }
        if (cmbMIDIIN.getItemCount() > 0)
            cmbMIDIIN.setSelectedIndex(0);
        if (cmbMIDIIN.getItemCount() > 0) {
            cmbMIDIIN.setSelectedIndex(0);
            for (int i = 0; i < cmbMIDIIN.getItemCount(); i++) {
                if (cmbMIDIIN.getItemAt(i).equals(setting.getMidiKbd().getMidiInDeviceName())) {
                    cmbMIDIIN.setSelectedIndex(i);
                }
            }
        }
        cbUseMIDIKeyboard.setSelected(setting.getMidiKbd().getUseMIDIKeyboard());
        cbFM1.setSelected(setting.getMidiKbd().getUseChannel()[0]);
        cbFM2.setSelected(setting.getMidiKbd().getUseChannel()[1]);
        cbFM3.setSelected(setting.getMidiKbd().getUseChannel()[2]);
        cbFM4.setSelected(setting.getMidiKbd().getUseChannel()[3]);
        cbFM5.setSelected(setting.getMidiKbd().getUseChannel()[4]);
        cbFM6.setSelected(setting.getMidiKbd().getUseChannel()[5]);
        rbMONO.setSelected(setting.getMidiKbd().isMono());
        rbPOLY.setSelected(!setting.getMidiKbd().isMono());
        rbFM1.setSelected(setting.getMidiKbd().getUseMonoChannel() == 0);
        rbFM2.setSelected(setting.getMidiKbd().getUseMonoChannel() == 1);
        rbFM3.setSelected(setting.getMidiKbd().getUseMonoChannel() == 2);
        rbFM4.setSelected(setting.getMidiKbd().getUseMonoChannel() == 3);
        rbFM5.setSelected(setting.getMidiKbd().getUseMonoChannel() == 4);
        rbFM6.setSelected(setting.getMidiKbd().getUseMonoChannel() == 5);
        tbCCChCopy.setText(setting.getMidiKbd().getMidiCtrl_CopyToneFromYM2612Ch1() == -1 ? "" : String.valueOf(setting.getMidiKbd().getMidiCtrl_CopyToneFromYM2612Ch1()));
        tbCCCopyLog.setText(setting.getMidiKbd().getMidiCtrl_CopySelecttingLogToClipbrd() == -1 ? "" : String.valueOf(setting.getMidiKbd().getMidiCtrl_CopySelecttingLogToClipbrd()));
        tbCCDelLog.setText(setting.getMidiKbd().getMidiCtrl_DelOneLog() == -1 ? "" : String.valueOf(setting.getMidiKbd().getMidiCtrl_DelOneLog()));
        tbCCFadeout.setText(setting.getMidiKbd().getMidiCtrl_Fadeout() == -1 ? "" : String.valueOf(setting.getMidiKbd().getMidiCtrl_Fadeout()));
        tbCCFast.setText(setting.getMidiKbd().getMidiCtrl_Fast() == -1 ? "" : String.valueOf(setting.getMidiKbd().getMidiCtrl_Fast()));
        tbCCNext.setText(setting.getMidiKbd().getMidiCtrl_Next() == -1 ? "" : String.valueOf(setting.getMidiKbd().getMidiCtrl_Next()));
        tbCCPause.setText(setting.getMidiKbd().getMidiCtrl_Pause() == -1 ? "" : String.valueOf(setting.getMidiKbd().getMidiCtrl_Pause()));
        tbCCPlay.setText(setting.getMidiKbd().getMidiCtrl_Play() == -1 ? "" : String.valueOf(setting.getMidiKbd().getMidiCtrl_Play()));
        tbCCPrevious.setText(setting.getMidiKbd().getMidiCtrl_Previous() == -1 ? "" : String.valueOf(setting.getMidiKbd().getMidiCtrl_Previous()));
        tbCCSlow.setText(setting.getMidiKbd().getMidiCtrlSlow() == -1 ? "" : String.valueOf(setting.getMidiKbd().getMidiCtrlSlow()));
        tbCCStop.setText(setting.getMidiKbd().getMidiCtrl_Stop() == -1 ? "" : String.valueOf(setting.getMidiKbd().getMidiCtrl_Stop()));
    }

    @Override
    public void apply(Setting setting) {
        setting.getMidiKbd().setMidiInDeviceName(cmbMIDIIN.getSelectedItem() != null ? cmbMIDIIN.getSelectedItem().toString() : "");
        setting.getMidiKbd().getUseChannel()[0] = cbFM1.isSelected();
        setting.getMidiKbd().getUseChannel()[1] = cbFM2.isSelected();
        setting.getMidiKbd().getUseChannel()[2] = cbFM3.isSelected();
        setting.getMidiKbd().getUseChannel()[3] = cbFM4.isSelected();
        setting.getMidiKbd().getUseChannel()[4] = cbFM5.isSelected();
        setting.getMidiKbd().getUseChannel()[5] = cbFM6.isSelected();
        setting.getMidiKbd().setUseMIDIKeyboard(cbUseMIDIKeyboard.isSelected());
        setting.getMidiKbd().setMono(rbMONO.isSelected());
        setting.getMidiKbd().setUseMonoChannel(rbFM1.isSelected() ? 0 : (rbFM2.isSelected() ? 1 : (rbFM3.isSelected() ? 2 : (rbFM4.isSelected() ? 3 : (rbFM5.isSelected() ? 4 : (rbFM6.isSelected() ? 5 : -1))))));
        setting.getMidiKbd().setMidiCtrl_CopySelecttingLogToClipbrd(FormSetting.parseMidiCtrl(tbCCCopyLog.getText()));
        setting.getMidiKbd().setMidiCtrl_CopyToneFromYM2612Ch1(FormSetting.parseMidiCtrl(tbCCChCopy.getText()));
        setting.getMidiKbd().setMidiCtrl_DelOneLog(FormSetting.parseMidiCtrl(tbCCDelLog.getText()));
        setting.getMidiKbd().setMidiCtrl_Fadeout(FormSetting.parseMidiCtrl(tbCCFadeout.getText()));
        setting.getMidiKbd().setMidiCtrl_Fast(FormSetting.parseMidiCtrl(tbCCFast.getText()));
        setting.getMidiKbd().setMidiCtrl_Next(FormSetting.parseMidiCtrl(tbCCNext.getText()));
        setting.getMidiKbd().setMidiCtrl_Pause(FormSetting.parseMidiCtrl(tbCCPause.getText()));
        setting.getMidiKbd().setMidiCtrl_Play(FormSetting.parseMidiCtrl(tbCCPlay.getText()));
        setting.getMidiKbd().setMidiCtrl_Previous(FormSetting.parseMidiCtrl(tbCCPrevious.getText()));
        setting.getMidiKbd().setMidiCtrlSlow(FormSetting.parseMidiCtrl(tbCCSlow.getText()));
        setting.getMidiKbd().setMidiCtrl_Stop(FormSetting.parseMidiCtrl(tbCCStop.getText()));
    }

    private void cbUseMIDIKeyboard_CheckedChanged(ChangeEvent ev) {
        gbMIDIKeyboard.setEnabled(cbUseMIDIKeyboard.isSelected());
    }
}
