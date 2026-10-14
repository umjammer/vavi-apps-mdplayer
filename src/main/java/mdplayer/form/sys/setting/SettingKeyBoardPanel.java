package mdplayer.form.sys.setting;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.lang.System.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.form.Grid;
import mdplayer.form.SettingTab;
import mdplayer.form.sys.FormMain;

import static java.lang.System.getLogger;

/** the "KeyBoard" page of the settings dialog, split out of the original FormSetting */
public class SettingKeyBoardPanel extends SettingTab {

    @Override
    public int order() {
        return 110;
    }

    private static final Logger logger = getLogger(SettingKeyBoardPanel.class.getName());

    private final JLabel pictureBox10;
    private final JLabel pictureBox11;
    private final JLabel pictureBox12;
    private final JLabel pictureBox13;
    private final JLabel pictureBox14;
    private final JLabel pictureBox15;
    private final JLabel pictureBox16;
    private final JLabel pictureBox17;
    private final JCheckBox cbUseKeyBoardHook;
    private final JPanel gbUseKeyBoardHook;
    private final JButton btPrevClr;
    private final JButton btPauseClr;
    private final JButton btFadeoutClr;
    private final JButton btStopClr;
    private final JButton btNextSet;
    private final JButton btPrevSet;
    private final JButton btPlaySet;
    private final JButton btPauseSet;
    private final JButton btFastSet;
    private final JButton btFadeoutSet;
    private final JButton btSlowSet;
    private final JButton btStopSet;
    private final JLabel label50;
    private final JLabel lblNextKey;
    private final JLabel lblFastKey;
    private final JLabel lblPlayKey;
    private final JLabel lblSlowKey;
    private final JLabel lblPrevKey;
    private final JLabel lblFadeoutKey;
    private final JLabel lblPauseKey;
    private final JLabel lblStopKey;
    private final JCheckBox cbNextAlt;
    private final JCheckBox cbFastAlt;
    private final JCheckBox cbPlayAlt;
    private final JCheckBox cbSlowAlt;
    private final JCheckBox cbPrevAlt;
    private final JCheckBox cbFadeoutAlt;
    private final JCheckBox cbPauseAlt;
    private final JLabel label37;
    private final JCheckBox cbStopAlt;
    private final JLabel label45;
    private final JCheckBox cbNextWin;
    private final JLabel label46;
    private final JCheckBox cbFastWin;
    private final JLabel label47;
    private final JCheckBox cbPlayWin;
    private final JLabel label48;
    private final JCheckBox cbSlowWin;
    private final JLabel label38;
    private final JCheckBox cbPrevWin;
    private final JLabel label39;
    private final JCheckBox cbFadeoutWin;
    private final JLabel label40;
    private final JCheckBox cbPauseWin;
    private final JLabel label41;
    private final JCheckBox cbStopWin;
    private final JLabel label42;
    private final JCheckBox cbNextCtrl;
    private final JLabel label43;
    private final JCheckBox cbFastCtrl;
    private final JLabel label44;
    private final JCheckBox cbPlayCtrl;
    private final JCheckBox cbStopShift;
    private final JCheckBox cbSlowCtrl;
    private final JCheckBox cbPauseShift;
    private final JCheckBox cbPrevCtrl;
    private final JCheckBox cbFadeoutShift;
    private final JCheckBox cbFadeoutCtrl;
    private final JCheckBox cbPrevShift;
    private final JCheckBox cbPauseCtrl;
    private final JCheckBox cbSlowShift;
    private final JCheckBox cbStopCtrl;
    private final JCheckBox cbPlayShift;
    private final JCheckBox cbNextShift;
    private final JCheckBox cbFastShift;
    private final JButton btNextClr;
    private final JButton btPlayClr;
    private final JButton btFastClr;
    private final JButton btSlowClr;
    private final JLabel lblKeyBoardHookNotice;

    private static JLabel lblKey = null;
    private static JLabel lblNotice = null;
    private static JButton btSet = null;
    private static JButton btClr = null;
    private static JButton btOK = null;

    private final JButton btnOK;

    public static void keyHookMeth(NativeKeyEvent e) {
//        if (e.UpDown != HongliangSoft.Utilities.Gui.KeyboardUpDown.Up) return;

//        lblKey.setForeground(Color.ControlText);
        lblKey.setText(String.valueOf(e.getKeyCode()));
        lblNotice.setVisible(false);

        FormMain.keyHookMeth = null;
        btSet.setEnabled(true);
        btOK.setEnabled(true);
        btClr.setEnabled(true);
    }

    public SettingKeyBoardPanel(JButton dialogOK) {
        this.btnOK = dialogOK;
        this.cbUseKeyBoardHook = new JCheckBox();
        this.gbUseKeyBoardHook = new JPanel();
        this.lblKeyBoardHookNotice = new JLabel();
        this.btNextClr = new JButton();
        this.btPrevClr = new JButton();
        this.btPlayClr = new JButton();
        this.btPauseClr = new JButton();
        this.btFastClr = new JButton();
        this.btFadeoutClr = new JButton();
        this.btSlowClr = new JButton();
        this.btStopClr = new JButton();
        this.btNextSet = new JButton();
        this.btPrevSet = new JButton();
        this.btPlaySet = new JButton();
        this.btPauseSet = new JButton();
        this.btFastSet = new JButton();
        this.btFadeoutSet = new JButton();
        this.btSlowSet = new JButton();
        this.btStopSet = new JButton();
        this.label50 = new JLabel();
        this.lblNextKey = new JLabel();
        this.lblFastKey = new JLabel();
        this.lblPlayKey = new JLabel();
        this.lblSlowKey = new JLabel();
        this.lblPrevKey = new JLabel();
        this.lblFadeoutKey = new JLabel();
        this.lblPauseKey = new JLabel();
        this.lblStopKey = new JLabel();
        this.pictureBox14 = new JLabel();
        this.pictureBox17 = new JLabel();
        this.cbNextAlt = new JCheckBox();
        this.pictureBox16 = new JLabel();
        this.cbFastAlt = new JCheckBox();
        this.pictureBox15 = new JLabel();
        this.cbPlayAlt = new JCheckBox();
        this.pictureBox13 = new JLabel();
        this.cbSlowAlt = new JCheckBox();
        this.pictureBox12 = new JLabel();
        this.cbPrevAlt = new JCheckBox();
        this.pictureBox11 = new JLabel();
        this.cbFadeoutAlt = new JCheckBox();
        this.pictureBox10 = new JLabel();
        this.cbPauseAlt = new JCheckBox();
        this.label37 = new JLabel();
        this.cbStopAlt = new JCheckBox();
        this.label45 = new JLabel();
        this.label46 = new JLabel();
        this.label48 = new JLabel();
        this.label38 = new JLabel();
        this.label39 = new JLabel();
        this.label40 = new JLabel();
        this.label41 = new JLabel();
        this.label42 = new JLabel();
        this.cbNextCtrl = new JCheckBox();
        this.label43 = new JLabel();
        this.cbFastCtrl = new JCheckBox();
        this.label44 = new JLabel();
        this.cbPlayCtrl = new JCheckBox();
        this.cbStopShift = new JCheckBox();
        this.cbSlowCtrl = new JCheckBox();
        this.cbPauseShift = new JCheckBox();
        this.cbPrevCtrl = new JCheckBox();
        this.cbFadeoutShift = new JCheckBox();
        this.cbFadeoutCtrl = new JCheckBox();
        this.cbPrevShift = new JCheckBox();
        this.cbPauseCtrl = new JCheckBox();
        this.cbSlowShift = new JCheckBox();
        this.cbStopCtrl = new JCheckBox();
        this.cbPlayShift = new JCheckBox();
        this.cbNextShift = new JCheckBox();
        this.cbFastShift = new JCheckBox();
        this.label47 = new JLabel();
        this.cbStopWin = new JCheckBox();
        this.cbPauseWin = new JCheckBox();
        this.cbFadeoutWin = new JCheckBox();
        this.cbPrevWin = new JCheckBox();
        this.cbSlowWin = new JCheckBox();
        this.cbPlayWin = new JCheckBox();
        this.cbFastWin = new JCheckBox();
        this.cbNextWin = new JCheckBox();

        //
        // btFadeoutClr
        //
        this.btFadeoutClr.setName("btFadeoutClr");
        this.btFadeoutClr.addActionListener(this::btFadeoutClr_Click);
        //
        // btFadeoutSet
        //
        this.btFadeoutSet.setName("btFadeoutSet");
        this.btFadeoutSet.addActionListener(this::btFadeoutSet_Click);
        //
        // btFastClr
        //
        this.btFastClr.setName("btFastClr");
        this.btFastClr.addActionListener(this::btFastClr_Click);
        //
        // btFastSet
        //
        this.btFastSet.setName("btFastSet");
        this.btFastSet.addActionListener(this::btFastSet_Click);
        //
        // btNextClr
        //
        this.btNextClr.setName("btNextClr");
        this.btNextClr.addActionListener(this::btNextClr_Click);
        //
        // btNextSet
        //
        this.btNextSet.setName("btNextSet");
        this.btNextSet.addActionListener(this::btNextSet_Click);
        //
        // btPauseClr
        //
        this.btPauseClr.setName("btPauseClr");
        this.btPauseClr.addActionListener(this::btPauseClr_Click);
        //
        // btPauseSet
        //
        this.btPauseSet.setName("btPauseSet");
        this.btPauseSet.addActionListener(this::btPauseSet_Click);
        //
        // btPlayClr
        //
        this.btPlayClr.setName("btPlayClr");
        this.btPlayClr.addActionListener(this::btPlayClr_Click);
        //
        // btPlaySet
        //
        this.btPlaySet.setName("btPlaySet");
        this.btPlaySet.addActionListener(this::btPlaySet_Click);
        //
        // btPrevClr
        //
        this.btPrevClr.setName("btPrevClr");
        this.btPrevClr.addActionListener(this::btPrevClr_Click);
        //
        // btPrevSet
        //
        this.btPrevSet.setName("btPrevSet");
        this.btPrevSet.addActionListener(this::btPrevSet_Click);
        //
        // btSlowClr
        //
        this.btSlowClr.setName("btSlowClr");
        this.btSlowClr.addActionListener(this::btSlowClr_Click);
        //
        // btSlowSet
        //
        this.btSlowSet.setName("btSlowSet");
        this.btSlowSet.addActionListener(this::btSlowSet_Click);
        //
        // btStopClr
        //
        this.btStopClr.setName("btStopClr");
        this.btStopClr.addActionListener(this::btStopClr_Click);
        //
        // btStopSet
        //
        this.btStopSet.setName("btStopSet");
        this.btStopSet.addActionListener(this::btStopSet_Click);
        //
        // cbFadeoutAlt
        //
        this.cbFadeoutAlt.setName("cbFadeoutAlt");
        //
        // cbFadeoutCtrl
        //
        this.cbFadeoutCtrl.setName("cbFadeoutCtrl");
        //
        // cbFadeoutShift
        //
        this.cbFadeoutShift.setName("cbFadeoutShift");
        //
        // cbFadeoutWin
        //
        this.cbFadeoutWin.setName("cbFadeoutWin");
        //
        // cbFastAlt
        //
        this.cbFastAlt.setName("cbFastAlt");
        //
        // cbFastCtrl
        //
        this.cbFastCtrl.setName("cbFastCtrl");
        //
        // cbFastShift
        //
        this.cbFastShift.setName("cbFastShift");
        //
        // cbFastWin
        //
        this.cbFastWin.setName("cbFastWin");
        //
        // cbNextAlt
        //
        this.cbNextAlt.setName("cbNextAlt");
        //
        // cbNextCtrl
        //
        this.cbNextCtrl.setName("cbNextCtrl");
        //
        // cbNextShift
        //
        this.cbNextShift.setName("cbNextShift");
        //
        // cbNextWin
        //
        this.cbNextWin.setName("cbNextWin");
        //
        // cbPauseAlt
        //
        this.cbPauseAlt.setName("cbPauseAlt");
        //
        // cbPauseCtrl
        //
        this.cbPauseCtrl.setName("cbPauseCtrl");
        //
        // cbPauseShift
        //
        this.cbPauseShift.setName("cbPauseShift");
        //
        // cbPauseWin
        //
        this.cbPauseWin.setName("cbPauseWin");
        //
        // cbPlayAlt
        //
        this.cbPlayAlt.setName("cbPlayAlt");
        //
        // cbPlayCtrl
        //
        this.cbPlayCtrl.setName("cbPlayCtrl");
        //
        // cbPlayShift
        //
        this.cbPlayShift.setName("cbPlayShift");
        //
        // cbPlayWin
        //
        this.cbPlayWin.setName("cbPlayWin");
        //
        // cbPrevAlt
        //
        this.cbPrevAlt.setName("cbPrevAlt");
        //
        // cbPrevCtrl
        //
        this.cbPrevCtrl.setName("cbPrevCtrl");
        //
        // cbPrevShift
        //
        this.cbPrevShift.setName("cbPrevShift");
        //
        // cbPrevWin
        //
        this.cbPrevWin.setName("cbPrevWin");
        //
        // cbSlowAlt
        //
        this.cbSlowAlt.setName("cbSlowAlt");
        //
        // cbSlowCtrl
        //
        this.cbSlowCtrl.setName("cbSlowCtrl");
        //
        // cbSlowShift
        //
        this.cbSlowShift.setName("cbSlowShift");
        //
        // cbSlowWin
        //
        this.cbSlowWin.setName("cbSlowWin");
        //
        // cbStopAlt
        //
        this.cbStopAlt.setName("cbStopAlt");
        //
        // cbStopCtrl
        //
        this.cbStopCtrl.setName("cbStopCtrl");
        //
        // cbStopShift
        //
        this.cbStopShift.setName("cbStopShift");
        //
        // cbStopWin
        //
        this.cbStopWin.setName("cbStopWin");
        //
        // cbUseKeyBoardHook
        //
        this.cbUseKeyBoardHook.setName("cbUseKeyBoardHook");
        this.cbUseKeyBoardHook.addChangeListener(this::cbUseKeyBoardHook_CheckedChanged);
        //
        // gbUseKeyBoardHook
        //
        this.gbUseKeyBoardHook.setName("gbUseKeyBoardHook");
        //
        // label37
        //
        this.label37.setName("label37");
        //
        // label38
        //
        this.label38.setName("label38");
        //
        // label39
        //
        this.label39.setName("label39");
        //
        // label40
        //
        this.label40.setName("label40");
        //
        // label41
        //
        this.label41.setName("label41");
        //
        // label42
        //
        this.label42.setName("label42");
        //
        // label43
        //
        this.label43.setName("label43");
        //
        // label44
        //
        this.label44.setName("label44");
        //
        // label45
        //
        this.label45.setName("label45");
        //
        // label46
        //
        this.label46.setName("label46");
        //
        // label47
        //
        this.label47.setName("label47");
        //
        // label48
        //
        this.label48.setName("label48");
        //
        // label50
        //
        this.label50.setName("label50");
        //
        // lblFadeoutKey
        //
        this.lblFadeoutKey.setName("lblFadeoutKey");
        //
        // lblFastKey
        //
        this.lblFastKey.setName("lblFastKey");
        //
        // lblKeyBoardHookNotice
        //
        this.lblKeyBoardHookNotice.setForeground(Color.red);
        this.lblKeyBoardHookNotice.setName("lblKeyBoardHookNotice");
        //
        // lblNextKey
        //
        this.lblNextKey.setName("lblNextKey");
        //
        // lblPauseKey
        //
        this.lblPauseKey.setName("lblPauseKey");
        //
        // lblPlayKey
        //
        this.lblPlayKey.setName("lblPlayKey");
        //
        // lblPrevKey
        //
        this.lblPrevKey.setName("lblPrevKey");
        //
        // lblSlowKey
        //
        this.lblSlowKey.setName("lblSlowKey");
        //
        // lblStopKey
        //
        this.lblStopKey.setName("lblStopKey");
        //
        // pictureBox10
        //
        this.pictureBox10.setIcon(new ImageIcon(Common.getImage("ccNext")));
        this.pictureBox10.setName("pictureBox10");
        //
        // pictureBox11
        //
        this.pictureBox11.setIcon(new ImageIcon(Common.getImage("ccFast")));
        this.pictureBox11.setName("pictureBox11");
        //
        // pictureBox12
        //
        this.pictureBox12.setIcon(new ImageIcon(Common.getImage("ccPlay")));
        this.pictureBox12.setName("pictureBox12");
        //
        // pictureBox13
        //
        this.pictureBox13.setIcon(new ImageIcon(Common.getImage("ccSlow")));
        this.pictureBox13.setName("pictureBox13");
        //
        // pictureBox14
        //
        this.pictureBox14.setIcon(new ImageIcon(Common.getImage("ccStop")));
        this.pictureBox14.setName("pictureBox14");
        //
        // pictureBox15
        //
        this.pictureBox15.setIcon(new ImageIcon(Common.getImage("ccPause")));
        this.pictureBox15.setName("pictureBox15");
        //
        // pictureBox16
        //
        this.pictureBox16.setIcon(new ImageIcon(Common.getImage("ccPrevious")));
        this.pictureBox16.setName("pictureBox16");
        //
        // pictureBox17
        //
        this.pictureBox17.setIcon(new ImageIcon(Common.getImage("ccFadeout")));
        this.pictureBox17.setName("pictureBox17");
        //
        // tpKeyBoard
        //
        this.setName("tpKeyBoard");
        // layout
        new Grid(Grid.group(this.gbUseKeyBoardHook))
                .skip().skip().add(this.label45).add(this.label46).add(this.label48).add(this.label47).add(this.label50).next()
                .add(this.pictureBox14).add(this.label37).add(this.cbStopShift).add(this.cbStopCtrl).add(this.cbStopAlt).add(this.cbStopWin)
                .add(this.lblStopKey).add(this.btStopSet).add(this.btStopClr).next()
                .add(this.pictureBox15).add(this.label38).add(this.cbPauseShift).add(this.cbPauseCtrl).add(this.cbPauseAlt).add(this.cbPauseWin)
                .add(this.lblPauseKey).add(this.btPauseSet).add(this.btPauseClr).next()
                .add(this.pictureBox17).add(this.label39).add(this.cbFadeoutShift).add(this.cbFadeoutCtrl).add(this.cbFadeoutAlt).add(this.cbFadeoutWin)
                .add(this.lblFadeoutKey).add(this.btFadeoutSet).add(this.btFadeoutClr).next()
                .add(this.pictureBox16).add(this.label40).add(this.cbPrevShift).add(this.cbPrevCtrl).add(this.cbPrevAlt).add(this.cbPrevWin)
                .add(this.lblPrevKey).add(this.btPrevSet).add(this.btPrevClr).next()
                .add(this.pictureBox13).add(this.label41).add(this.cbSlowShift).add(this.cbSlowCtrl).add(this.cbSlowAlt).add(this.cbSlowWin)
                .add(this.lblSlowKey).add(this.btSlowSet).add(this.btSlowClr).next()
                .add(this.pictureBox12).add(this.label42).add(this.cbPlayShift).add(this.cbPlayCtrl).add(this.cbPlayAlt).add(this.cbPlayWin)
                .add(this.lblPlayKey).add(this.btPlaySet).add(this.btPlayClr).next()
                .add(this.pictureBox11).add(this.label43).add(this.cbFastShift).add(this.cbFastCtrl).add(this.cbFastAlt).add(this.cbFastWin)
                .add(this.lblFastKey).add(this.btFastSet).add(this.btFastClr).next()
                .add(this.pictureBox10).add(this.label44).add(this.cbNextShift).add(this.cbNextCtrl).add(this.cbNextAlt).add(this.cbNextWin)
                .add(this.lblNextKey).add(this.btNextSet).add(this.btNextClr).next()
                .span(this.lblKeyBoardHookNotice).end();
        new Grid(this)
                .add(this.cbUseKeyBoardHook).next()
                .grow(this.gbUseKeyBoardHook);
    }

    @Override
    public void load(Setting setting) {
        cbUseKeyBoardHook.setSelected(setting.getKeyboardHook().getUseKeyBoardHook());
        gbUseKeyBoardHook.setEnabled(setting.getKeyboardHook().getUseKeyBoardHook());
        cbStopShift.setSelected(setting.getKeyboardHook().getStop().getShift());
        cbStopCtrl.setSelected(setting.getKeyboardHook().getStop().getCtrl());
        cbStopWin.setSelected(setting.getKeyboardHook().getStop().getWin());
        cbStopAlt.setSelected(setting.getKeyboardHook().getStop().getAlt());
        lblStopKey.setText(setting.getKeyboardHook().getStop().getKey());
        btStopClr.setEnabled((!lblStopKey.getText().equals("(None)") && (lblStopKey.getText() != null && !lblStopKey.getText().isEmpty())));
        cbPauseShift.setSelected(setting.getKeyboardHook().getPause().getShift());
        cbPauseCtrl.setSelected(setting.getKeyboardHook().getPause().getCtrl());
        cbPauseWin.setSelected(setting.getKeyboardHook().getPause().getWin());
        cbPauseAlt.setSelected(setting.getKeyboardHook().getPause().getAlt());
        lblPauseKey.setText(setting.getKeyboardHook().getPause().getKey());
        btPauseClr.setEnabled((!lblPauseKey.getText().equals("(None)") && (lblPauseKey.getText() != null && !lblPauseKey.getText().isEmpty())));
        cbFadeoutShift.setSelected(setting.getKeyboardHook().getFadeout().getShift());
        cbFadeoutCtrl.setSelected(setting.getKeyboardHook().getFadeout().getCtrl());
        cbFadeoutWin.setSelected(setting.getKeyboardHook().getFadeout().getWin());
        cbFadeoutAlt.setSelected(setting.getKeyboardHook().getFadeout().getAlt());
        lblFadeoutKey.setText(setting.getKeyboardHook().getFadeout().getKey());
        btFadeoutClr.setEnabled((!lblFadeoutKey.getText().equals("(None)") && (lblFadeoutKey.getText() != null && !lblFadeoutKey.getText().isEmpty())));
        cbPrevShift.setSelected(setting.getKeyboardHook().getPrev().getShift());
        cbPrevCtrl.setSelected(setting.getKeyboardHook().getPrev().getCtrl());
        cbPrevWin.setSelected(setting.getKeyboardHook().getPrev().getWin());
        cbPrevAlt.setSelected(setting.getKeyboardHook().getPrev().getAlt());
        lblPrevKey.setText(setting.getKeyboardHook().getPrev().getKey());
        btPrevClr.setEnabled((!lblPrevKey.getText().equals("(None)") && (lblPrevKey.getText() != null && !lblPrevKey.getText().isEmpty())));
        cbSlowShift.setSelected(setting.getKeyboardHook().getSlow().getShift());
        cbSlowCtrl.setSelected(setting.getKeyboardHook().getSlow().getCtrl());
        cbSlowWin.setSelected(setting.getKeyboardHook().getSlow().getWin());
        cbSlowAlt.setSelected(setting.getKeyboardHook().getSlow().getAlt());
        lblSlowKey.setText(setting.getKeyboardHook().getSlow().getKey());
        btSlowClr.setEnabled((!lblSlowKey.getText().equals("(None)") && (lblSlowKey.getText() != null && !lblSlowKey.getText().isEmpty())));
        cbPlayShift.setSelected(setting.getKeyboardHook().getPlay().getShift());
        cbPlayCtrl.setSelected(setting.getKeyboardHook().getPlay().getCtrl());
        cbPlayWin.setSelected(setting.getKeyboardHook().getPlay().getWin());
        cbPlayAlt.setSelected(setting.getKeyboardHook().getPlay().getAlt());
        lblPlayKey.setText(setting.getKeyboardHook().getPlay().getKey());
        btPlayClr.setEnabled((!lblPlayKey.getText().equals("(None)") && (lblPlayKey.getText() != null && !lblPlayKey.getText().isEmpty())));
        cbFastShift.setSelected(setting.getKeyboardHook().getFast().getShift());
        cbFastCtrl.setSelected(setting.getKeyboardHook().getFast().getCtrl());
        cbFastWin.setSelected(setting.getKeyboardHook().getFast().getWin());
        cbFastAlt.setSelected(setting.getKeyboardHook().getFast().getAlt());
        lblFastKey.setText(setting.getKeyboardHook().getFast().getKey());
        btFastClr.setEnabled((!lblFastKey.getText().equals("(None)") && (lblFastKey.getText() != null && !lblFastKey.getText().isEmpty())));
        cbNextShift.setSelected(setting.getKeyboardHook().getNext().getShift());
        cbNextCtrl.setSelected(setting.getKeyboardHook().getNext().getCtrl());
        cbNextWin.setSelected(setting.getKeyboardHook().getNext().getWin());
        cbNextAlt.setSelected(setting.getKeyboardHook().getNext().getAlt());
        lblNextKey.setText(setting.getKeyboardHook().getNext().getKey());
        btNextClr.setEnabled((!lblNextKey.getText().equals("(None)") && (lblNextKey.getText() != null && !lblNextKey.getText().isEmpty())));
    }

    @Override
    public void apply(Setting setting) {
        setting.getKeyboardHook().setUseKeyBoardHook(cbUseKeyBoardHook.isSelected());
        setting.getKeyboardHook().getStop().setShift(cbStopShift.isSelected());
        setting.getKeyboardHook().getStop().setCtrl(cbStopCtrl.isSelected());
        setting.getKeyboardHook().getStop().setWin(cbStopWin.isSelected());
        setting.getKeyboardHook().getStop().setAlt(cbStopAlt.isSelected());
        setting.getKeyboardHook().getStop().setKey(lblStopKey.getText() == null || lblStopKey.getText().isEmpty() ? "(None)" : lblStopKey.getText());
        setting.getKeyboardHook().getPause().setShift(cbPauseShift.isSelected());
        setting.getKeyboardHook().getPause().setCtrl(cbPauseCtrl.isSelected());
        setting.getKeyboardHook().getPause().setWin(cbPauseWin.isSelected());
        setting.getKeyboardHook().getPause().setAlt(cbPauseAlt.isSelected());
        setting.getKeyboardHook().getPause().setKey(lblPauseKey.getText() == null || lblPauseKey.getText().isEmpty() ? "(None)" : lblPauseKey.getText());
        setting.getKeyboardHook().getFadeout().setShift(cbFadeoutShift.isSelected());
        setting.getKeyboardHook().getFadeout().setCtrl(cbFadeoutCtrl.isSelected());
        setting.getKeyboardHook().getFadeout().setWin(cbFadeoutWin.isSelected());
        setting.getKeyboardHook().getFadeout().setAlt(cbFadeoutAlt.isSelected());
        setting.getKeyboardHook().getFadeout().setKey(lblFadeoutKey.getText() == null || lblFadeoutKey.getText().isEmpty() ? "(None)" : lblFadeoutKey.getText());
        setting.getKeyboardHook().getPrev().setShift(cbPrevShift.isSelected());
        setting.getKeyboardHook().getPrev().setCtrl(cbPrevCtrl.isSelected());
        setting.getKeyboardHook().getPrev().setWin(cbPrevWin.isSelected());
        setting.getKeyboardHook().getPrev().setAlt(cbPrevAlt.isSelected());
        setting.getKeyboardHook().getPrev().setKey(lblPrevKey.getText() == null || lblPrevKey.getText().isEmpty() ? "(None)" : lblPrevKey.getText());
        setting.getKeyboardHook().getSlow().setShift(cbSlowShift.isSelected());
        setting.getKeyboardHook().getSlow().setCtrl(cbSlowCtrl.isSelected());
        setting.getKeyboardHook().getSlow().setWin(cbSlowWin.isSelected());
        setting.getKeyboardHook().getSlow().setAlt(cbSlowAlt.isSelected());
        setting.getKeyboardHook().getSlow().setKey(lblSlowKey.getText() == null || lblSlowKey.getText().isEmpty() ? "(None)" : lblSlowKey.getText());
        setting.getKeyboardHook().getPlay().setShift(cbPlayShift.isSelected());
        setting.getKeyboardHook().getPlay().setCtrl(cbPlayCtrl.isSelected());
        setting.getKeyboardHook().getPlay().setWin(cbPlayWin.isSelected());
        setting.getKeyboardHook().getPlay().setAlt(cbPlayAlt.isSelected());
        setting.getKeyboardHook().getPlay().setKey(lblPlayKey.getText() == null || lblPlayKey.getText().isEmpty() ? "(None)" : lblPlayKey.getText());
        setting.getKeyboardHook().getFast().setShift(cbFastShift.isSelected());
        setting.getKeyboardHook().getFast().setCtrl(cbFastCtrl.isSelected());
        setting.getKeyboardHook().getFast().setWin(cbFastWin.isSelected());
        setting.getKeyboardHook().getFast().setAlt(cbFastAlt.isSelected());
        setting.getKeyboardHook().getFast().setKey(lblFastKey.getText() == null || lblFastKey.getText().isEmpty() ? "(None)" : lblFastKey.getText());
        setting.getKeyboardHook().getNext().setShift(cbNextShift.isSelected());
        setting.getKeyboardHook().getNext().setCtrl(cbNextCtrl.isSelected());
        setting.getKeyboardHook().getNext().setWin(cbNextWin.isSelected());
        setting.getKeyboardHook().getNext().setAlt(cbNextAlt.isSelected());
        setting.getKeyboardHook().getNext().setKey(lblNextKey.getText() == null || lblNextKey.getText().isEmpty() ? "(None)" : lblNextKey.getText());
    }

    private void btFadeoutClr_Click(ActionEvent ev) {
        lblFadeoutKey.setText("(None)");
        btFadeoutClr.setEnabled(false);
    }

    private void btFadeoutSet_Click(ActionEvent ev) {
        lblKey = lblFadeoutKey;
        btSet = btFadeoutSet;
        btClr = btFadeoutClr;
        btOK = btnOK;
        btFadeoutSet.setEnabled(false);
        btnOK.setEnabled(false);
        lblKey.setText("Waiting for input");
        lblKey.setForeground(Color.red);

        lblNotice = lblKeyBoardHookNotice;
        lblKeyBoardHookNotice.setVisible(true);

        FormMain.keyHookMeth = SettingKeyBoardPanel::keyHookMeth;
    }

    private void btFastClr_Click(ActionEvent ev) {
        lblFastKey.setText("(None)");
        btFastClr.setEnabled(false);
    }

    private void btFastSet_Click(ActionEvent ev) {
        lblKey = lblFastKey;
        btSet = btFastSet;
        btClr = btFastClr;
        btOK = btnOK;
        btFastSet.setEnabled(false);
        btnOK.setEnabled(false);
        lblKey.setText("Waiting for input");
        lblKey.setForeground(Color.red);

        lblNotice = lblKeyBoardHookNotice;
        lblKeyBoardHookNotice.setVisible(true);

        FormMain.keyHookMeth = SettingKeyBoardPanel::keyHookMeth;
    }

    private void btNextClr_Click(ActionEvent ev) {
        lblNextKey.setText("(None)");
        btNextClr.setEnabled(false);
    }

    private void btNextSet_Click(ActionEvent ev) {
        lblKey = lblNextKey;
        btSet = btNextSet;
        btClr = btNextClr;
        btOK = btnOK;
        btNextSet.setEnabled(false);
        btnOK.setEnabled(false);
        lblKey.setText("Waiting for input");
        lblKey.setForeground(Color.red);

        lblNotice = lblKeyBoardHookNotice;
        lblKeyBoardHookNotice.setVisible(true);

        FormMain.keyHookMeth = SettingKeyBoardPanel::keyHookMeth;
    }

    private void btPauseClr_Click(ActionEvent ev) {
        lblPauseKey.setText("(None)");
        btPauseClr.setEnabled(false);
    }

    private void btPauseSet_Click(ActionEvent ev) {
        lblKey = lblPauseKey;
        btSet = btPauseSet;
        btOK = btnOK;
        btClr = btPauseClr;
        btPauseSet.setEnabled(false);
        btnOK.setEnabled(false);
        lblKey.setText("Waiting for input");
        lblKey.setForeground(Color.red);

        lblNotice = lblKeyBoardHookNotice;
        lblKeyBoardHookNotice.setVisible(true);

        FormMain.keyHookMeth = SettingKeyBoardPanel::keyHookMeth;
    }

    private void btPlayClr_Click(ActionEvent ev) {
        lblPlayKey.setText("(None)");
        btPlayClr.setEnabled(false);
    }

    private void btPlaySet_Click(ActionEvent ev) {
        lblKey = lblPlayKey;
        btSet = btPlaySet;
        btClr = btPlayClr;
        btOK = btnOK;
        btPlaySet.setEnabled(false);
        btnOK.setEnabled(false);
        lblKey.setText("Waiting for input");
        lblKey.setForeground(Color.red);

        lblNotice = lblKeyBoardHookNotice;
        lblKeyBoardHookNotice.setVisible(true);

        FormMain.keyHookMeth = SettingKeyBoardPanel::keyHookMeth;
    }

    private void btPrevClr_Click(ActionEvent ev) {
        lblPrevKey.setText("(None)");
        btPrevClr.setEnabled(false);
    }

    private void btPrevSet_Click(ActionEvent ev) {
        lblKey = lblPrevKey;
        btSet = btPrevSet;
        btClr = btPrevClr;
        btOK = btnOK;
        btPrevSet.setEnabled(false);
        btnOK.setEnabled(false);
        lblKey.setText("Waiting for input");
        lblKey.setForeground(Color.red);

        lblNotice = lblKeyBoardHookNotice;
        lblKeyBoardHookNotice.setVisible(true);

        FormMain.keyHookMeth = SettingKeyBoardPanel::keyHookMeth;
    }

    private void btSlowClr_Click(ActionEvent ev) {
        lblSlowKey.setText("(None)");
        btSlowClr.setEnabled(false);
    }

    private void btSlowSet_Click(ActionEvent ev) {
        lblKey = lblSlowKey;
        btSet = btSlowSet;
        btClr = btSlowClr;
        btOK = btnOK;
        btSlowSet.setEnabled(false);
        btnOK.setEnabled(false);
        lblKey.setText("Waiting for input");
        lblKey.setForeground(Color.red);

        lblNotice = lblKeyBoardHookNotice;
        lblKeyBoardHookNotice.setVisible(true);

        FormMain.keyHookMeth = SettingKeyBoardPanel::keyHookMeth;
    }

    private void btStopClr_Click(ActionEvent ev) {
        lblStopKey.setText("(None)");
        btStopClr.setEnabled(false);
    }

    private void btStopSet_Click(ActionEvent ev) {

        lblKey = lblStopKey;
        btSet = btStopSet;
        btClr = btStopClr;
        btOK = btnOK;
        btStopSet.setEnabled(false);
        btnOK.setEnabled(false);
        lblKey.setText("Waiting for input");
        lblKey.setForeground(Color.red);

        lblNotice = lblKeyBoardHookNotice;
        lblKeyBoardHookNotice.setVisible(true);

        FormMain.keyHookMeth = SettingKeyBoardPanel::keyHookMeth;
    }

    private void cbUseKeyBoardHook_CheckedChanged(ChangeEvent ev) {
        gbUseKeyBoardHook.setEnabled(cbUseKeyBoardHook.isSelected());
    }
}
