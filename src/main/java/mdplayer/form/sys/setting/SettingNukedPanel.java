package mdplayer.form.sys.setting;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import mdplayer.Setting;
import mdplayer.form.Grid;
import mdplayer.form.SettingTab;

/** the "Nuked" page of the settings dialog, split out of the original FormSetting */
public class SettingNukedPanel extends SettingTab {

    @Override
    public int order() {
        return 30;
    }

    private final JPanel groupBox26;
    private final JCheckBox rbNukedOPN2OptionYM2612u;
    private final JCheckBox rbNukedOPN2OptionYM2612;
    private final JCheckBox rbNukedOPN2OptionDiscrete;
    private final JCheckBox rbNukedOPN2OptionASIC;
    private final JCheckBox rbNukedOPN2OptionASIClp;
    private final JPanel groupBox29;
    private final JCheckBox cbGensSSGEG;
    private final JCheckBox cbGensDACHPF;

    public SettingNukedPanel() {
        this.groupBox29 = new JPanel();
        this.cbGensSSGEG = new JCheckBox();
        this.cbGensDACHPF = new JCheckBox();
        this.groupBox26 = new JPanel();
        this.rbNukedOPN2OptionYM2612u = new JCheckBox();
        this.rbNukedOPN2OptionYM2612 = new JCheckBox();
        this.rbNukedOPN2OptionDiscrete = new JCheckBox();
        this.rbNukedOPN2OptionASIClp = new JCheckBox();
        this.rbNukedOPN2OptionASIC = new JCheckBox();

        //
        // cbGensDACHPF
        //
        this.cbGensDACHPF.setName("cbGensDACHPF");
        //
        // cbGensSSGEG
        //
        this.cbGensSSGEG.setName("cbGensSSGEG");
        //
        // groupBox26
        //
        this.groupBox26.setName("groupBox26");
        //
        // groupBox29
        //
        this.groupBox29.setName("groupBox29");
        //
        // rbNukedOPN2OptionASIC
        //
        this.rbNukedOPN2OptionASIC.setName("rbNukedOPN2OptionASIC");
        //
        // rbNukedOPN2OptionASIClp
        //
        this.rbNukedOPN2OptionASIClp.setName("rbNukedOPN2OptionASIClp");
        //
        // rbNukedOPN2OptionDiscrete
        //
        this.rbNukedOPN2OptionDiscrete.setName("rbNukedOPN2OptionDiscrete");
        //
        // rbNukedOPN2OptionYM2612
        //
        this.rbNukedOPN2OptionYM2612.setName("rbNukedOPN2OptionYM2612");
        //
        // rbNukedOPN2OptionYM2612u
        //
        this.rbNukedOPN2OptionYM2612u.setName("rbNukedOPN2OptionYM2612u");
        //
        // tpNuked
        //
        this.setName("tpNuked");
        // layout
        new Grid(Grid.group(this.groupBox26))
                .add(this.rbNukedOPN2OptionDiscrete).next()
                .add(this.rbNukedOPN2OptionASIC).next()
                .add(this.rbNukedOPN2OptionYM2612).next()
                .add(this.rbNukedOPN2OptionYM2612u).next()
                .add(this.rbNukedOPN2OptionASIClp).end();
        new Grid(Grid.group(this.groupBox29))
                .add(this.cbGensDACHPF).next()
                .add(this.cbGensSSGEG).end();
        new Grid(this)
                .wide(this.groupBox26).next()
                .wide(this.groupBox29).end();
    }

    @Override
    public void load(Setting setting) {
        switch (setting.getNukedOPN2().emuType) {
        case 0:
            rbNukedOPN2OptionDiscrete.setSelected(true);
            break;
        case 1:
            rbNukedOPN2OptionASIC.setSelected(true);
            break;
        case 2:
            rbNukedOPN2OptionYM2612.setSelected(true);
            break;
        case 3:
            rbNukedOPN2OptionYM2612u.setSelected(true);
            break;
        case 4:
            rbNukedOPN2OptionASIClp.setSelected(true);
            break;
        }
        cbGensDACHPF.setSelected(setting.getNukedOPN2().gensDACHPF);
        cbGensSSGEG.setSelected(setting.getNukedOPN2().gensSSGEG);
    }

    @Override
    public void apply(Setting setting) {
        setting.setNukedOPN2(new Setting.NukedOPN2());
        if (rbNukedOPN2OptionYM2612.isSelected()) setting.getNukedOPN2().emuType = 2;
        if (rbNukedOPN2OptionASIC.isSelected()) setting.getNukedOPN2().emuType = 1;
        if (rbNukedOPN2OptionDiscrete.isSelected()) setting.getNukedOPN2().emuType = 0;
        if (rbNukedOPN2OptionYM2612u.isSelected()) setting.getNukedOPN2().emuType = 3;
        if (rbNukedOPN2OptionASIClp.isSelected()) setting.getNukedOPN2().emuType = 4;
        setting.getNukedOPN2().gensDACHPF = cbGensDACHPF.isSelected();
        setting.getNukedOPN2().gensSSGEG = cbGensSSGEG.isSelected();
    }
}
