package mdplayer.form.sys.setting;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.System.Logger;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import mdplayer.Setting;
import mdplayer.form.Grid;
import mdplayer.form.SettingTab;
import static java.lang.System.getLogger;

/** the "Balance" page of the settings dialog, split out of the original FormSetting */
public class SettingBalancePanel extends SettingTab {

    @Override
    public int order() {
        return 120;
    }

    private static final Logger logger = getLogger(SettingBalancePanel.class.getName());

    private final JCheckBox cbAutoBalanceUseThis;
    private final JPanel groupBox18;
    private final JPanel groupBox24;
    private final JPanel groupBox21;
    private final JCheckBox rbAutoBalanceNotSaveSongBalance;
    private final JCheckBox rbAutoBalanceSamePositionAsSongData;
    private final JCheckBox rbAutoBalanceSaveSongBalance;
    private final JPanel groupBox22;
    private final JLabel label4;
    private final JPanel groupBox23;
    private final JPanel groupBox19;
    private final JCheckBox rbAutoBalanceNotLoadSongBalance;
    private final JCheckBox rbAutoBalanceLoadSongBalance;
    private final JPanel groupBox20;
    private final JCheckBox rbAutoBalanceNotLoadDriverBalance;
    private final JCheckBox rbAutoBalanceLoadDriverBalance;
    private final JPanel groupBox25;
    private final JCheckBox rbAutoBalanceNotSamePositionAsSongData;

    public SettingBalancePanel() {
        this.groupBox25 = new JPanel();
        this.rbAutoBalanceNotSamePositionAsSongData = new JCheckBox();
        this.rbAutoBalanceSamePositionAsSongData = new JCheckBox();
        this.cbAutoBalanceUseThis = new JCheckBox();
        this.groupBox18 = new JPanel();
        this.groupBox24 = new JPanel();
        this.groupBox21 = new JPanel();
        this.rbAutoBalanceNotSaveSongBalance = new JCheckBox();
        this.rbAutoBalanceSaveSongBalance = new JCheckBox();
        this.groupBox22 = new JPanel();
        this.label4 = new JLabel();
        this.groupBox23 = new JPanel();
        this.groupBox19 = new JPanel();
        this.rbAutoBalanceNotLoadSongBalance = new JCheckBox();
        this.rbAutoBalanceLoadSongBalance = new JCheckBox();
        this.groupBox20 = new JPanel();
        this.rbAutoBalanceNotLoadDriverBalance = new JCheckBox();
        this.rbAutoBalanceLoadDriverBalance = new JCheckBox();

        //
        // cbAutoBalanceUseThis
        //
        this.cbAutoBalanceUseThis.setSelected(true);
        this.cbAutoBalanceUseThis.setName("cbAutoBalanceUseThis");
        //
        // groupBox18
        //
        this.groupBox18.setName("groupBox18");
        //
        // groupBox19
        //
        this.groupBox19.setName("groupBox19");
        //
        // groupBox20
        //
        this.groupBox20.setName("groupBox20");
        this.groupBox20.addFocusListener(this.groupBox20_Enter);
        //
        // groupBox21
        //
        this.groupBox21.setName("groupBox21");
        //
        // groupBox22
        //
        this.groupBox22.setName("groupBox22");
        //
        // groupBox23
        //
        this.groupBox23.setName("groupBox23");
        //
        // groupBox24
        //
        this.groupBox24.setName("groupBox24");
        //
        // groupBox25
        //
        this.groupBox25.setName("groupBox25");
        //
        // label4
        //
        this.label4.setName("label4");
        //
        // rbAutoBalanceLoadDriverBalance
        //
        this.rbAutoBalanceLoadDriverBalance.setSelected(true);
        this.rbAutoBalanceLoadDriverBalance.setName("rbAutoBalanceLoadDriverBalance");
        //
        // rbAutoBalanceLoadSongBalance
        //
        this.rbAutoBalanceLoadSongBalance.setName("rbAutoBalanceLoadSongBalance");
        //
        // rbAutoBalanceNotLoadDriverBalance
        //
        this.rbAutoBalanceNotLoadDriverBalance.setName("rbAutoBalanceNotLoadDriverBalance");
        //
        // rbAutoBalanceNotLoadSongBalance
        //
        this.rbAutoBalanceNotLoadSongBalance.setSelected(true);
        this.rbAutoBalanceNotLoadSongBalance.setName("rbAutoBalanceNotLoadSongBalance");
        //
        // rbAutoBalanceNotSamePositionAsSongData
        //
        this.rbAutoBalanceNotSamePositionAsSongData.setSelected(true);
        this.rbAutoBalanceNotSamePositionAsSongData.setName("rbAutoBalanceNotSamePositionAsSongData");
        //
        // rbAutoBalanceNotSaveSongBalance
        //
        this.rbAutoBalanceNotSaveSongBalance.setSelected(true);
        this.rbAutoBalanceNotSaveSongBalance.setName("rbAutoBalanceNotSaveSongBalance");
        //
        // rbAutoBalanceSamePositionAsSongData
        //
        this.rbAutoBalanceSamePositionAsSongData.setName("rbAutoBalanceSamePositionAsSongData");
        //
        // rbAutoBalanceSaveSongBalance
        //
        this.rbAutoBalanceSaveSongBalance.setName("rbAutoBalanceSaveSongBalance");
        //
        // tpBalance
        //
        this.setName("tpBalance");
        // layout
        new Grid(Grid.group(this.groupBox19))
                .add(this.rbAutoBalanceLoadSongBalance).next()
                .add(this.rbAutoBalanceNotLoadSongBalance).end();
        new Grid(Grid.group(this.groupBox20))
                .add(this.rbAutoBalanceLoadDriverBalance).next()
                .add(this.rbAutoBalanceNotLoadDriverBalance).end();
        new Grid(Grid.group(this.groupBox23))
                .span(this.groupBox19).next()
                .span(this.groupBox20).end();
        new Grid(Grid.group(this.groupBox21))
                .add(this.rbAutoBalanceSaveSongBalance).next()
                .add(this.rbAutoBalanceNotSaveSongBalance).end();
        new Grid(Grid.group(this.groupBox22))
                .add(this.label4).end();
        new Grid(Grid.group(this.groupBox24))
                .span(this.groupBox21).next()
                .span(this.groupBox22).end();
        new Grid(Grid.group(this.groupBox18))
                .add(this.cbAutoBalanceUseThis).next()
                .span(this.groupBox23).next()
                .span(this.groupBox24).end();
        new Grid(Grid.group(this.groupBox25))
                .add(this.rbAutoBalanceNotSamePositionAsSongData).add(this.rbAutoBalanceSamePositionAsSongData).end();
        new Grid(this)
                .span(this.groupBox18).next()
                .span(this.groupBox25).end();
    }

    @Override
    public void load(Setting setting) {
        cbAutoBalanceUseThis.setSelected(setting.getAutoBalance().getUseThis());
        rbAutoBalanceLoadSongBalance.setSelected(setting.getAutoBalance().getLoadSongBalance());
        rbAutoBalanceNotLoadSongBalance.setSelected(!setting.getAutoBalance().getLoadSongBalance());
        rbAutoBalanceLoadDriverBalance.setSelected(setting.getAutoBalance().getLoadDriverBalance());
        rbAutoBalanceNotLoadDriverBalance.setSelected(!setting.getAutoBalance().getLoadDriverBalance());
        rbAutoBalanceSaveSongBalance.setSelected(setting.getAutoBalance().getSaveSongBalance());
        rbAutoBalanceNotSaveSongBalance.setSelected(!setting.getAutoBalance().getSaveSongBalance());
        rbAutoBalanceSamePositionAsSongData.setSelected(setting.getAutoBalance().getSamePositionAsSongData());
        rbAutoBalanceNotSamePositionAsSongData.setSelected(!setting.getAutoBalance().getSamePositionAsSongData());
    }

    @Override
    public void apply(Setting setting) {
        setting.setAutoBalance(new Setting.AutoBalance());
        setting.getAutoBalance().setUseThis(cbAutoBalanceUseThis.isSelected());
        setting.getAutoBalance().setLoadSongBalance(rbAutoBalanceLoadSongBalance.isSelected());
        setting.getAutoBalance().setLoadDriverBalance(rbAutoBalanceLoadDriverBalance.isSelected());
        setting.getAutoBalance().setSaveSongBalance(rbAutoBalanceSaveSongBalance.isSelected());
        setting.getAutoBalance().setSamePositionAsSongData(rbAutoBalanceSamePositionAsSongData.isSelected());
    }

    private final FocusListener groupBox20_Enter = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
        }
    };
}
