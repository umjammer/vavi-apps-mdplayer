package mdplayer.form.sys.setting;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import mdplayer.Setting;
import mdplayer.form.Grid;
import mdplayer.form.SettingTab;
import static java.lang.System.getLogger;

/** the "NSF" page of the settings dialog, split out of the original FormSetting */
public class SettingNSFPanel extends SettingTab {

    @Override
    public int order() {
        return 40;
    }

    private static final Logger logger = getLogger(SettingNSFPanel.class.getName());

    private final JPanel groupBox8;
    private final JCheckBox cbNSFFDSWriteDisable8000;
    private final JPanel groupBox10;
    private final JCheckBox cbNSFDmc_RandomizeTri;
    private final JCheckBox cbNSFDmc_TriMute;
    private final JCheckBox cbNSFDmc_RandomizeNoise;
    private final JCheckBox cbNSFDmc_DPCMAntiClick;
    private final JCheckBox cbNSFDmc_EnablePNoise;
    private final JCheckBox cbNSFDmc_Enable4011;
    private final JCheckBox cbNSFDmc_NonLinearMixer;
    private final JCheckBox cbNSFDmc_UnmuteOnReset;
    private final JPanel groupBox12;
    private final JCheckBox cbNSFN160_Serial;
    private final JPanel groupBox11;
    private final JCheckBox cbNSFMmc5_PhaseRefresh;
    private final JCheckBox cbNSFMmc5_NonLinearMixer;
    private final JPanel groupBox9;
    private final JCheckBox cbNFSNes_DutySwap;
    private final JCheckBox cbNFSNes_PhaseRefresh;
    private final JCheckBox cbNFSNes_NonLinearMixer;
    private final JCheckBox cbNFSNes_UnmuteOnReset;
    private final JLabel label21;
    private final JLabel label20;
    private final JTextField tbNSFFds_LPF;
    private final JCheckBox cbNFSFds_4085Reset;
    private final JProgressBar trkbNSFLPF;
    private final JLabel label53;
    private final JLabel label52;
    private final JProgressBar trkbNSFHPF;
    private final JCheckBox cbNSFDmc_DPCMReverse;

    public SettingNSFPanel() {
        this.trkbNSFLPF = new JProgressBar();
        this.label53 = new JLabel();
        this.label52 = new JLabel();
        this.trkbNSFHPF = new JProgressBar();
        this.groupBox10 = new JPanel();
        this.cbNSFDmc_DPCMReverse = new JCheckBox();
        this.cbNSFDmc_RandomizeTri = new JCheckBox();
        this.cbNSFDmc_TriMute = new JCheckBox();
        this.cbNSFDmc_RandomizeNoise = new JCheckBox();
        this.cbNSFDmc_DPCMAntiClick = new JCheckBox();
        this.cbNSFDmc_EnablePNoise = new JCheckBox();
        this.cbNSFDmc_Enable4011 = new JCheckBox();
        this.cbNSFDmc_NonLinearMixer = new JCheckBox();
        this.cbNSFDmc_UnmuteOnReset = new JCheckBox();
        this.groupBox12 = new JPanel();
        this.cbNSFN160_Serial = new JCheckBox();
        this.groupBox11 = new JPanel();
        this.cbNSFMmc5_PhaseRefresh = new JCheckBox();
        this.cbNSFMmc5_NonLinearMixer = new JCheckBox();
        this.groupBox9 = new JPanel();
        this.cbNFSNes_DutySwap = new JCheckBox();
        this.cbNFSNes_PhaseRefresh = new JCheckBox();
        this.cbNFSNes_NonLinearMixer = new JCheckBox();
        this.cbNFSNes_UnmuteOnReset = new JCheckBox();
        this.groupBox8 = new JPanel();
        this.label21 = new JLabel();
        this.label20 = new JLabel();
        this.tbNSFFds_LPF = new JTextField();
        this.cbNFSFds_4085Reset = new JCheckBox();
        this.cbNSFFDSWriteDisable8000 = new JCheckBox();

        //
        // cbNFSFds_4085Reset
        //
        this.cbNFSFds_4085Reset.setName("cbNFSFds_4085Reset");
        //
        // cbNFSNes_DutySwap
        //
        this.cbNFSNes_DutySwap.setName("cbNFSNes_DutySwap");
        //
        // cbNFSNes_NonLinearMixer
        //
        this.cbNFSNes_NonLinearMixer.setName("cbNFSNes_NonLinearMixer");
        //
        // cbNFSNes_PhaseRefresh
        //
        this.cbNFSNes_PhaseRefresh.setName("cbNFSNes_PhaseRefresh");
        //
        // cbNFSNes_UnmuteOnReset
        //
        this.cbNFSNes_UnmuteOnReset.setName("cbNFSNes_UnmuteOnReset");
        //
        // cbNSFDmc_DPCMAntiClick
        //
        this.cbNSFDmc_DPCMAntiClick.setName("cbNSFDmc_DPCMAntiClick");
        //
        // cbNSFDmc_DPCMReverse
        //
        this.cbNSFDmc_DPCMReverse.setName("cbNSFDmc_DPCMReverse");
        //
        // cbNSFDmc_Enable4011
        //
        this.cbNSFDmc_Enable4011.setName("cbNSFDmc_Enable4011");
        //
        // cbNSFDmc_EnablePNoise
        //
        this.cbNSFDmc_EnablePNoise.setName("cbNSFDmc_EnablePNoise");
        //
        // cbNSFDmc_NonLinearMixer
        //
        this.cbNSFDmc_NonLinearMixer.setName("cbNSFDmc_NonLinearMixer");
        //
        // cbNSFDmc_RandomizeNoise
        //
        this.cbNSFDmc_RandomizeNoise.setName("cbNSFDmc_RandomizeNoise");
        //
        // cbNSFDmc_RandomizeTri
        //
        this.cbNSFDmc_RandomizeTri.setName("cbNSFDmc_RandomizeTri");
        //
        // cbNSFDmc_TriMute
        //
        this.cbNSFDmc_TriMute.setName("cbNSFDmc_TriMute");
        //
        // cbNSFDmc_UnmuteOnReset
        //
        this.cbNSFDmc_UnmuteOnReset.setName("cbNSFDmc_UnmuteOnReset");
        //
        // cbNSFFDSWriteDisable8000
        //
        this.cbNSFFDSWriteDisable8000.setName("cbNSFFDSWriteDisable8000");
        //
        // cbNSFMmc5_NonLinearMixer
        //
        this.cbNSFMmc5_NonLinearMixer.setName("cbNSFMmc5_NonLinearMixer");
        //
        // cbNSFMmc5_PhaseRefresh
        //
        this.cbNSFMmc5_PhaseRefresh.setName("cbNSFMmc5_PhaseRefresh");
        //
        // cbNSFN160_Serial
        //
        this.cbNSFN160_Serial.setName("cbNSFN160_Serial");
        //
        // groupBox10
        //
        this.groupBox10.setName("groupBox10");
        //
        // groupBox11
        //
        this.groupBox11.setName("groupBox11");
        //
        // groupBox12
        //
        this.groupBox12.setName("groupBox12");
        //
        // groupBox8
        //
        this.groupBox8.setName("groupBox8");
        //
        // groupBox9
        //
        this.groupBox9.setName("groupBox9");
        //
        // label20
        //
        this.label20.setName("label20");
        //
        // label21
        //
        this.label21.setName("label21");
        //
        // label52
        //
        this.label52.setName("label52");
        //
        // label53
        //
        this.label53.setName("label53");
        //
        // tbNSFFds_LPF
        //
        this.tbNSFFds_LPF.setName("tbNSFFds_LPF");
        //
        // tpNSF
        //
        this.setName("tpNSF");
        // layout
        new Grid(Grid.group(this.groupBox9))
                .add(this.cbNFSNes_UnmuteOnReset).next()
                .add(this.cbNFSNes_NonLinearMixer).next()
                .add(this.cbNFSNes_PhaseRefresh).next()
                .add(this.cbNFSNes_DutySwap).end();
        this.tbNSFFds_LPF.setColumns(6);
        new Grid(Grid.group(this.groupBox8))
                .add(Grid.row(this.label20, this.tbNSFFds_LPF, this.label21)).next()
                .add(this.cbNFSFds_4085Reset).next()
                .add(this.cbNSFFDSWriteDisable8000).end();
        new Grid(Grid.group(this.groupBox11))
                .add(this.cbNSFMmc5_NonLinearMixer).next()
                .add(this.cbNSFMmc5_PhaseRefresh).end();
        new Grid(Grid.group(this.groupBox10))
                .add(this.cbNSFDmc_UnmuteOnReset).next()
                .add(this.cbNSFDmc_NonLinearMixer).next()
                .add(this.cbNSFDmc_Enable4011).next()
                .add(this.cbNSFDmc_EnablePNoise).next()
                .add(this.cbNSFDmc_DPCMAntiClick).next()
                .add(this.cbNSFDmc_RandomizeNoise).next()
                .add(this.cbNSFDmc_TriMute).next()
                .add(this.cbNSFDmc_RandomizeTri).next()
                .add(this.cbNSFDmc_DPCMReverse).end();
        new Grid(Grid.group(this.groupBox12))
                .add(this.cbNSFN160_Serial).end();
        JPanel left = Grid.panel()
                .wide(this.groupBox9).next()
                .wide(this.groupBox8).next()
                .wide(this.groupBox11).build();
        JPanel right = Grid.panel()
                .wide(this.groupBox10).next()
                .wide(this.groupBox12).build();
        new Grid(this)
                .add(left).add(right).next()
                .span(Grid.row(this.label52, this.trkbNSFHPF, this.label53, this.trkbNSFLPF)).end();
        //
        // trkbNSFHPF
        //
        this.trkbNSFHPF.setMaximum(256);
        this.trkbNSFHPF.setName("trkbNSFHPF");
        //
        // trkbNSFLPF
        //
        this.trkbNSFLPF.setMaximum(400);
        this.trkbNSFLPF.setName("trkbNSFLPF");
    }

    @Override
    public void load(Setting setting) {
        trkbNSFHPF.setValue(setting.getNsf().getHPF());
        trkbNSFLPF.setValue(setting.getNsf().getLPF());
        cbNFSNes_UnmuteOnReset.setSelected(setting.getNsf().getNESUnmuteOnReset());
        cbNFSNes_NonLinearMixer.setSelected(setting.getNsf().getNESNonLinearMixer());
        cbNFSNes_PhaseRefresh.setSelected(setting.getNsf().getNESPhaseRefresh());
        cbNFSNes_DutySwap.setSelected(setting.getNsf().getNESDutySwap());
        tbNSFFds_LPF.setText(String.valueOf(setting.getNsf().getFDSLpf()));
        cbNFSFds_4085Reset.setSelected(setting.getNsf().getFDS4085Reset());
        cbNSFFDSWriteDisable8000.setSelected(setting.getNsf().getFDSWriteDisable8000());
        cbNSFDmc_UnmuteOnReset.setSelected(setting.getNsf().getDMCUnmuteOnReset());
        cbNSFDmc_NonLinearMixer.setSelected(setting.getNsf().getDMCNonLinearMixer());
        cbNSFDmc_Enable4011.setSelected(setting.getNsf().getDMCEnable4011());
        cbNSFDmc_EnablePNoise.setSelected(setting.getNsf().getDMCEnablePnoise());
        cbNSFDmc_DPCMAntiClick.setSelected(setting.getNsf().getDMCDPCMAntiClick());
        cbNSFDmc_RandomizeNoise.setSelected(setting.getNsf().getDMCRandomizeNoise());
        cbNSFDmc_TriMute.setSelected(setting.getNsf().getDMCTRImute());
        cbNSFDmc_RandomizeTri.setSelected(setting.getNsf().getDMCRandomizeTRI());
        cbNSFDmc_DPCMReverse.setSelected(setting.getNsf().getDMCDPCMReverse());
        cbNSFMmc5_NonLinearMixer.setSelected(setting.getNsf().getMMC5NonLinearMixer());
        cbNSFMmc5_PhaseRefresh.setSelected(setting.getNsf().getMMC5PhaseRefresh());
        cbNSFN160_Serial.setSelected(setting.getNsf().getN160Serial());
    }

    @Override
    public void apply(Setting setting) {
        int i;

        setting.getNsf().setNESUnmuteOnReset(cbNFSNes_UnmuteOnReset.isSelected());
        setting.getNsf().setNESNonLinearMixer(cbNFSNes_NonLinearMixer.isSelected());
        setting.getNsf().setNESPhaseRefresh(cbNFSNes_PhaseRefresh.isSelected());
        setting.getNsf().setNESDutySwap(cbNFSNes_DutySwap.isSelected());
        try {
            i = Integer.parseInt(tbNSFFds_LPF.getText());
            setting.getNsf().setFDSLpf(Math.clamp(i, 0, 99999));
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, e);
        }
        setting.getNsf().setFDS4085Reset(cbNFSFds_4085Reset.isSelected());
        setting.getNsf().setFDSWriteDisable8000(cbNSFFDSWriteDisable8000.isSelected());
        setting.getNsf().setDMCUnmuteOnReset(cbNSFDmc_UnmuteOnReset.isSelected());
        setting.getNsf().setDMCNonLinearMixer(cbNSFDmc_NonLinearMixer.isSelected());
        setting.getNsf().setDMCEnable4011(cbNSFDmc_Enable4011.isSelected());
        setting.getNsf().setDMCEnablePnoise(cbNSFDmc_EnablePNoise.isSelected());
        setting.getNsf().setDMCDPCMAntiClick(cbNSFDmc_DPCMAntiClick.isSelected());
        setting.getNsf().setDMCRandomizeNoise(cbNSFDmc_RandomizeNoise.isSelected());
        setting.getNsf().setDMCTRImute(cbNSFDmc_TriMute.isSelected());
        setting.getNsf().setDMCRandomizeTRI(cbNSFDmc_RandomizeTri.isSelected());
        setting.getNsf().setDMCDPCMReverse(cbNSFDmc_DPCMReverse.isSelected());
        setting.getNsf().setMMC5NonLinearMixer(cbNSFMmc5_NonLinearMixer.isSelected());
        setting.getNsf().setMMC5PhaseRefresh(cbNSFMmc5_PhaseRefresh.isSelected());
        setting.getNsf().setN160Serial(cbNSFN160_Serial.isSelected());
        setting.getNsf().setHPF(trkbNSFHPF.getValue());
        setting.getNsf().setLPF(trkbNSFLPF.getValue());
    }
}
