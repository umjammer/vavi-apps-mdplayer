package mdplayer.form.sys.setting;

import java.awt.Dimension;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import mdplayer.Audio;
import mdplayer.RealChip.EnmRealChipType;
import mdplayer.Setting;
import mdplayer.Setting.ChipType2;
import mdplayer.chips.RealChipPlugin;
import mdplayer.form.Grid;
import mdplayer.form.SettingTab;
import mdplayer.form.sys.FormSetting;

import static java.lang.System.getLogger;

/** the "Module" page of the settings dialog, split out of the original FormSetting */
public class SettingModulePanel extends SettingTab {

    @Override
    public int order() {
        return 20;
    }

    private static final Logger logger = getLogger(SettingModulePanel.class.getName());

    private final JPanel groupBox3;
    private final JLabel label13;
    private final JLabel label12;
    private final JLabel label11;
    private final JTextField tbLatencyEmu;
    private final JTextField tbLatencySCCI;
    private final JLabel label10;
    private final JCheckBox cbHiyorimiMode;
    private final SettingInstrumentsPanel ucSI;
    private final JPanel groupBox1;
    private final JCheckBox cbUnuseRealChip;

    public SettingModulePanel() {
        this.groupBox1 = new JPanel();
        this.cbUnuseRealChip = new JCheckBox();
        this.ucSI = new SettingInstrumentsPanel();
        this.groupBox3 = new JPanel();
        this.cbHiyorimiMode = new JCheckBox();
        this.label13 = new JLabel();
        this.label12 = new JLabel();
        this.label11 = new JLabel();
        this.tbLatencyEmu = new JTextField();
        this.tbLatencySCCI = new JTextField();
        this.label10 = new JLabel();

        //
        // cbHiyorimiMode
        //
        this.cbHiyorimiMode.setName("cbHiyorimiMode");
        //
        // cbUnuseRealChip
        //
        this.cbUnuseRealChip.setName("cbUnuseRealChip");
        //
        // groupBox1
        //
        this.groupBox1.setName("groupBox1");
        //
        // groupBox3
        //
        this.groupBox3.setName("groupBox3");
        //
        // label10
        //
        this.label10.setName("label10");
        //
        // label11
        //
        this.label11.setName("label11");
        //
        // label12
        //
        this.label12.setName("label12");
        //
        // label13
        //
        this.label13.setName("label13");
        //
        // tbLatencyEmu
        //
        this.tbLatencyEmu.setName("tbLatencyEmu");
        //
        // tbLatencySCCI
        //
        this.tbLatencySCCI.setName("tbLatencySCCI");
        //
        // tpModule
        //
        this.setName("tpModule");
        // layout
        JScrollPane chips = new JScrollPane(this.ucSI);
        chips.getVerticalScrollBar().setUnitIncrement(16);
        // as wide as a chip's box, a few of them high: the list scrolls rather than sizes the dialog
        chips.setPreferredSize(new Dimension(chips.getPreferredSize().width, 280));
        new Grid(Grid.group(this.groupBox1))
                .span(this.cbUnuseRealChip).next()
                .grow(chips);
        this.tbLatencyEmu.setColumns(5);
        this.tbLatencySCCI.setColumns(5);
        new Grid(Grid.group(this.groupBox3))
                .add(Grid.row(this.label13, this.tbLatencyEmu, this.label10, this.label12, this.tbLatencySCCI, this.label11)).next()
                .add(this.cbHiyorimiMode).end();
        new Grid(this)
                .grow(this.groupBox1).next()
                .span(this.groupBox3).end();
        //
        // ucSI
        //
        this.ucSI.setName("ucSI");
    }

    @Override
    public void load(Setting setting) {
        cbUnuseRealChip.setSelected(setting.getUnuseRealChip());
        if (ucSI != null) {
            setRealCombo(EnmRealChipType.YM2612,
                    ucSI.cmbYM2612P_SCCI, ucSI.rbYM2612P_SCCI,
                    ucSI.cmbYM2612S_SCCI, ucSI.rbYM2612S_SCCI
            );

            setRealCombo(EnmRealChipType.YM2413,
                    ucSI.cmbYM2413P_Real, ucSI.rbYM2413P_Real,
                    ucSI.cmbYM2413S_Real, ucSI.rbYM2413S_Real
            );

            setRealCombo(EnmRealChipType.AY8910,
                    ucSI.cmbAY8910P_Real, ucSI.rbAY8910P_Real,
                    ucSI.cmbAY8910S_Real, ucSI.rbAY8910S_Real
            );

            setRealCombo(EnmRealChipType.SN76489,
                    ucSI.cmbSN76489P_SCCI, ucSI.rbSN76489P_SCCI,
                    ucSI.cmbSN76489S_SCCI, ucSI.rbSN76489S_SCCI
            );

            setRealCombo(EnmRealChipType.YM2608,
                    ucSI.cmbYM2608P_SCCI, ucSI.rbYM2608P_SCCI,
                    ucSI.cmbYM2608S_SCCI, ucSI.rbYM2608S_SCCI
            );

            setRealCombo(EnmRealChipType.YM2610,
                    ucSI.cmbYM2610BP_SCCI, ucSI.rbYM2610BP_SCCI,
                    ucSI.cmbYM2610BS_SCCI, ucSI.rbYM2610BS_SCCI
            );

            setRealCombo(EnmRealChipType.YM2608,
                    ucSI.cmbYM2610BEP_SCCI, ucSI.rbYM2610BEP_SCCI,
                    ucSI.cmbYM2610BES_SCCI, ucSI.rbYM2610BES_SCCI
            );

            setRealCombo(EnmRealChipType.SPPCM,
                    ucSI.cmbSPPCMP_SCCI, null,
                    ucSI.cmbSPPCMS_SCCI, null
            );

            ucSI.rbYM2610BEP_SCCI.setEnabled((ucSI.cmbYM2610BEP_SCCI.isEnabled() || ucSI.cmbSPPCMP_SCCI.isEnabled()));
            ucSI.rbYM2610BES_SCCI.setEnabled((ucSI.cmbYM2610BES_SCCI.isEnabled() || ucSI.cmbSPPCMS_SCCI.isEnabled()));

            setRealCombo(EnmRealChipType.YM2151,
                    ucSI.cmbYM2151P_SCCI, ucSI.rbYM2151P_SCCI,
                    ucSI.cmbYM2151S_SCCI, ucSI.rbYM2151S_SCCI
            );

            setRealCombo(EnmRealChipType.YM2203,
                    ucSI.cmbYM2203P_SCCI, ucSI.rbYM2203P_SCCI,
                    ucSI.cmbYM2203S_SCCI, ucSI.rbYM2203S_SCCI
            );

            setRealCombo(EnmRealChipType.C140,
                    ucSI.cmbC140P_SCCI, ucSI.rbC140P_Real,
                    ucSI.cmbC140S_SCCI, ucSI.rbC140S_SCCI
            );

            setRealCombo(EnmRealChipType.SEGAPCM,
                    ucSI.cmbSEGAPCMP_SCCI, ucSI.rbSEGAPCMP_SCCI,
                    ucSI.cmbSEGAPCMS_SCCI, ucSI.rbSEGAPCMS_SCCI
            );

            setRealCombo(EnmRealChipType.YMF262,
                    ucSI.cmbYMF262P_SCCI, ucSI.rbYMF262P_SCCI,
                    ucSI.cmbYMF262S_SCCI, ucSI.rbYMF262S_SCCI
            );

            setRealCombo(EnmRealChipType.YM3526,
                    ucSI.cmbYM3526P_SCCI, ucSI.rbYM3526P_SCCI,
                    ucSI.cmbYM3526S_SCCI, ucSI.rbYM3526S_SCCI
            );

            setRealCombo(EnmRealChipType.YM3812,
                    ucSI.cmbYM3812P_SCCI, ucSI.rbYM3812P_SCCI,
                    ucSI.cmbYM3812S_SCCI, ucSI.rbYM3812S_SCCI
            );

            setRealCombo(EnmRealChipType.K051649,
                    ucSI.cmbK051649P_Real, ucSI.rbK051649P_Real,
                    ucSI.cmbK051649S_Real, ucSI.rbK051649S_Real
            );
        }
        if (ucSI != null) {
            setRealParam(setting.getYM2612Type()[0],
                    ucSI.rbYM2612P_Silent,
                    ucSI.rbYM2612P_Emu,
                    ucSI.rbYM2612P_SCCI,
                    ucSI.cmbYM2612P_SCCI,
                    null, null, null,
                    ucSI.rbYM2612P_EmuNuked,
                    ucSI.rbYM2612P_EmuMame);
            setRealParam(setting.getYM2612Type()[1],
                    ucSI.rbYM2612S_Silent,
                    ucSI.rbYM2612S_Emu,
                    ucSI.rbYM2612S_SCCI,
                    ucSI.cmbYM2612S_SCCI,
                    null, null, null,
                    ucSI.rbYM2612S_EmuNuked,
                    ucSI.rbYM2612S_EmuMame);

            if (setting.getYM2612Type() != null && setting.getYM2612Type().length > 0
                    && setting.getYM2612Type()[0].getRealChipInfo() != null
                    && setting.getYM2612Type()[0].getRealChipInfo().length > 0
                    && setting.getYM2612Type()[0].getRealChipInfo()[0] != null) {
                ucSI.cbSendWait.setSelected(setting.getYM2612Type()[0].getRealChipInfo()[0].getUseWait());
                ucSI.cbTwice.setSelected(setting.getYM2612Type()[0].getRealChipInfo()[0].getUseWaitBoost());
                ucSI.cbEmulationPCMOnly.setSelected(setting.getYM2612Type()[0].getRealChipInfo()[0].getOnlyPCMEmulation());
            }

            setRealParam(setting.getYM2610Type()[0],
                    ucSI.rbYM2610BP_Silent,
                    ucSI.rbYM2610BP_Emu,
                    ucSI.rbYM2610BP_SCCI,
                    ucSI.cmbYM2610BP_SCCI,
                    ucSI.rbYM2610BEP_SCCI,
                    ucSI.cmbYM2610BEP_SCCI,
                    ucSI.cmbSPPCMP_SCCI,
                    null, null);
            setRealParam(setting.getYM2610Type()[1],
                    ucSI.rbYM2610BS_Silent,
                    ucSI.rbYM2610BS_Emu,
                    ucSI.rbYM2610BS_SCCI,
                    ucSI.cmbYM2610BS_SCCI,
                    ucSI.rbYM2610BES_SCCI,
                    ucSI.cmbYM2610BES_SCCI,
                    ucSI.cmbSPPCMS_SCCI,
                    null, null);

            setRealParam(setting.getSN76489Type()[0],
                    ucSI.rbSN76489P_Silent,
                    ucSI.rbSN76489P_Emu,
                    ucSI.rbSN76489P_SCCI,
                    ucSI.cmbSN76489P_SCCI,
                    null, null, null,
                    ucSI.rbSN76489P_Emu2, null);
            setRealParam(setting.getSN76489Type()[1],
                    ucSI.rbSN76489S_Silent,
                    ucSI.rbSN76489S_Emu,
                    ucSI.rbSN76489S_SCCI,
                    ucSI.cmbSN76489S_SCCI,
                    null, null, null,
                    ucSI.rbSN76489S_Emu2, null);

            setRealParam(setting.getYM2608Type()[0],
                    ucSI.rbYM2608P_Silent,
                    ucSI.rbYM2608P_Emu,
                    ucSI.rbYM2608P_SCCI,
                    ucSI.cmbYM2608P_SCCI, null, null, null, null, null);
            setRealParam(setting.getYM2608Type()[1],
                    ucSI.rbYM2608S_Silent,
                    ucSI.rbYM2608S_Emu,
                    ucSI.rbYM2608S_SCCI,
                    ucSI.cmbYM2608S_SCCI, null, null, null, null, null);

            setRealParam(setting.getYM2151Type()[0],
                    ucSI.rbYM2151P_Silent,
                    ucSI.rbYM2151P_Emu,
                    ucSI.rbYM2151P_SCCI,
                    ucSI.cmbYM2151P_SCCI,
                    null, null, null,
                    ucSI.rbYM2151P_EmuMame,
                    ucSI.rbYM2151P_EmuX68Sound);
            setRealParam(setting.getYM2151Type()[1],
                    ucSI.rbYM2151S_Silent,
                    ucSI.rbYM2151S_Emu,
                    ucSI.rbYM2151S_SCCI,
                    ucSI.cmbYM2151S_SCCI,
                    null, null, null,
                    ucSI.rbYM2151S_EmuMame,
                    ucSI.rbYM2151S_EmuX68Sound);

            setRealParam(setting.getYM2203Type()[0],
                    ucSI.rbYM2203P_Silent,
                    ucSI.rbYM2203P_Emu,
                    ucSI.rbYM2203P_SCCI,
                    ucSI.cmbYM2203P_SCCI, null, null, null, null, null);
            setRealParam(setting.getYM2203Type()[1],
                    ucSI.rbYM2203S_Silent,
                    ucSI.rbYM2203S_Emu,
                    ucSI.rbYM2203S_SCCI,
                    ucSI.cmbYM2203S_SCCI, null, null, null, null, null);

            setRealParam(setting.getAY8910Type()[0],
                    ucSI.rbAY8910P_Silent,
                    ucSI.rbAY8910P_Emu,
                    ucSI.rbAY8910P_Real,
                    ucSI.cmbAY8910P_Real,
                    null, null, null,
                    ucSI.rbAY8910P_Emu2, null);

            setRealParam(setting.getAY8910Type()[1],
                    ucSI.rbAY8910S_Silent,
                    ucSI.rbAY8910S_Emu,
                    ucSI.rbAY8910S_Real,
                    ucSI.cmbAY8910S_Real,
                    null, null, null,
                    ucSI.rbAY8910S_Emu2, null);

            setRealParam(setting.getK051649Type()[0],
                    ucSI.rbK051649P_Silent,
                    ucSI.rbK051649P_Emu,
                    ucSI.rbK051649P_Real,
                    ucSI.cmbK051649P_Real, null, null, null, null, null);
            setRealParam(setting.getK051649Type()[1],
                    ucSI.rbK051649S_Silent,
                    ucSI.rbK051649S_Emu,
                    ucSI.rbK051649S_Real,
                    ucSI.cmbK051649S_Real, null, null, null, null, null);

            setRealParam(setting.getYM2413Type()[0],
                    ucSI.rbYM2413P_Silent,
                    ucSI.rbYM2413P_Emu,
                    ucSI.rbYM2413P_Real,
                    ucSI.cmbYM2413P_Real, null, null, null, null, null);
            setRealParam(setting.getYM2413Type()[1],
                    ucSI.rbYM2413S_Silent,
                    ucSI.rbYM2413S_Emu,
                    ucSI.rbYM2413S_Real,
                    ucSI.cmbYM2413S_Real, null, null, null, null, null);

            setRealParam(setting.getYM3526Type()[0],
                    ucSI.rbYM3526P_Silent,
                    ucSI.rbYM3526P_Emu,
                    ucSI.rbYM3526P_SCCI,
                    ucSI.cmbYM3526P_SCCI, null, null, null, null, null);
            setRealParam(setting.getYM3526Type()[1],
                    ucSI.rbYM3526S_Silent,
                    ucSI.rbYM3526S_Emu,
                    ucSI.rbYM3526S_SCCI,
                    ucSI.cmbYM3526S_SCCI, null, null, null, null, null);

            setRealParam(setting.getYM3812Type()[0],
                    ucSI.rbYM3812P_Silent,
                    ucSI.rbYM3812P_Emu,
                    ucSI.rbYM3812P_SCCI,
                    ucSI.cmbYM3812P_SCCI, null, null, null, null, null);
            setRealParam(setting.getYM3812Type()[1],
                    ucSI.rbYM3812S_Silent,
                    ucSI.rbYM3812S_Emu,
                    ucSI.rbYM3812S_SCCI,
                    ucSI.cmbYM3812S_SCCI, null, null, null, null, null);

            setRealParam(setting.getYMF262Type()[0],
                    ucSI.rbYMF262P_Silent,
                    ucSI.rbYMF262P_Emu,
                    ucSI.rbYMF262P_SCCI,
                    ucSI.cmbYMF262P_SCCI, null, null, null, null, null);
            setRealParam(setting.getYMF262Type()[1],
                    ucSI.rbYMF262S_Silent,
                    ucSI.rbYMF262S_Emu,
                    ucSI.rbYMF262S_SCCI,
                    ucSI.cmbYMF262S_SCCI, null, null, null, null, null);

            setRealParam(setting.getC140Type()[0],
                    ucSI.rbC140P_Silent,
                    ucSI.rbC140P_Emu,
                    ucSI.rbC140P_Real,
                    ucSI.cmbC140P_SCCI, null, null, null, null, null);
            setRealParam(setting.getC140Type()[1],
                    ucSI.rbC140S_Silent,
                    ucSI.rbC140S_Emu,
                    ucSI.rbC140S_SCCI,
                    ucSI.cmbC140S_SCCI, null, null, null, null, null);

            setRealParam(setting.getSEGAPCMType()[0],
                    ucSI.rbSEGAPCMP_Silent,
                    ucSI.rbSEGAPCMP_Emu,
                    ucSI.rbSEGAPCMP_SCCI,
                    ucSI.cmbSEGAPCMP_SCCI, null, null, null, null, null);
            setRealParam(setting.getSEGAPCMType()[1],
                    ucSI.rbSEGAPCMS_Silent,
                    ucSI.rbSEGAPCMS_Emu,
                    ucSI.rbSEGAPCMS_SCCI,
                    ucSI.cmbSEGAPCMS_SCCI, null, null, null, null, null);
        }
        tbLatencyEmu.setText(String.valueOf(setting.getLatencyEmulation()));
        tbLatencySCCI.setText(String.valueOf(setting.getLatencySCCI()));
        cbHiyorimiMode.setSelected(setting.getHiyorimiMode());
    }

    @Override
    public void apply(Setting setting) {
        setting.getYM2612Type()[0] = new ChipType2();

        setting.getYM2612Type()[1] = new ChipType2();
        if (setting.getYM2612Type()[0].getRealChipInfo() == null) {
            setting.getYM2612Type()[0].setRealChipInfo(new ChipType2.RealChipInfo[] {new ChipType2.RealChipInfo()});
        }

        //setting.YM2612Type.LatencyForEmulation = 0;
        //if (Integer.parseInt(tbYM2612EmuDelay.getText(),  i)) {
        //    setting.YM2612Type.LatencyForEmulation = Math.max(Math.min(i, 999), 0);
        //}
        //setting.YM2612Type.LatencyForScci = 0;
        //if (Integer.parseInt(tbYM2612ScciDelay.getText(),  i)) {
        //    setting.YM2612Type.LatencyForScci = Math.max(Math.min(i, 999), 0);
        //}

        setting.setSN76489Type(new ChipType2[2]);
        setting.getSN76489Type()[0] = new ChipType2();

        setting.getSN76489Type()[1] = new ChipType2();

        //setting.SN76489Type.LatencyForEmulation = 0;
        //if (Integer.parseInt(tbSN76489EmuDelay.getText(),  i)) {
        //    setting.SN76489Type.LatencyForEmulation = Math.max(Math.min(i, 999), 0);
        //}
        //setting.SN76489Type.LatencyForScci = 0;
        //if (Integer.parseInt(tbSN76489ScciDelay.getText(),  i)) {
        //    setting.SN76489Type.LatencyForScci = Math.max(Math.min(i, 999), 0);
        //}

        setting.setYM2608Type(new ChipType2[2]);
        setting.getYM2608Type()[0] = new ChipType2();

        setting.getYM2608Type()[1] = new ChipType2();

        //setting.YM2608Type.UseWaitBoost = cbYM2608UseWaitBoost.isSelected();
        //setting.YM2608Type.OnlyPCMEmulation = cbOnlyPCMEmulation.isSelected();
        //setting.YM2608Type.LatencyForEmulation = 0;
        //if (Integer.parseInt(tbYM2608EmuDelay.getText(),  i)) {
        //    setting.YM2608Type.LatencyForEmulation = Math.max(Math.min(i, 999), 0);
        //}
        //setting.YM2608Type.LatencyForScci = 0;
        //if (Integer.parseInt(tbYM2608ScciDelay.getText(),  i)) {
        //    setting.YM2608Type.LatencyForScci = Math.max(Math.min(i, 999), 0);
        //}

        setting.setYM2610Type(new ChipType2[2]);
        setting.getYM2610Type()[0] = new ChipType2();

        setting.getYM2610Type()[1] = new ChipType2();

        setting.setYM2151Type(new ChipType2[2]);
        setting.getYM2151Type()[0] = new ChipType2();

        setting.getYM2151Type()[1] = new ChipType2();

        setting.setYM2203Type(new ChipType2[2]);
        setting.getYM2203Type()[0] = new ChipType2();

        setting.getYM2203Type()[1] = new ChipType2();

        setting.setAY8910Type(new ChipType2[2]);
        setting.getAY8910Type()[0] = new ChipType2();

        setting.getAY8910Type()[1] = new ChipType2();

        setting.setK051649Type(new ChipType2[2]);
        setting.getK051649Type()[0] = new ChipType2();

        setting.getK051649Type()[1] = new ChipType2();


        setting.setYM2413Type(new ChipType2[2]);
        setting.getYM2413Type()[0] = new ChipType2();

        setting.getYM2413Type()[1] = new ChipType2();

        setting.setC140Type(new ChipType2[2]);
        setting.getC140Type()[0] = new ChipType2();

        setting.getC140Type()[1] = new ChipType2();


        setting.setSEGAPCMType(new ChipType2[2]);
        setting.getSEGAPCMType()[0] = new ChipType2();

        setting.getSEGAPCMType()[1] = new ChipType2();

        setting.setYM3526Type(new ChipType2[2]);
        setting.getYM3526Type()[0] = new ChipType2();

        setting.getYM3526Type()[1] = new ChipType2();

        setting.setYM3812Type(new ChipType2[2]);
        setting.getYM3812Type()[0] = new ChipType2();

        setting.getYM3812Type()[1] = new ChipType2();

        setting.setYMF262Type(new ChipType2[2]);
        setting.getYMF262Type()[0] = new ChipType2();

        setting.getYMF262Type()[1] = new ChipType2();

        setting.setUnuseRealChip(cbUnuseRealChip.isSelected());
        setChipType2FromControls(
                setting.getYM2612Type()[0],
                ucSI.rbYM2612P_SCCI,
                ucSI.cmbYM2612P_SCCI,
                ucSI.rbYM2612P_Emu,
                ucSI.rbYM2612P_EmuNuked,
                ucSI.rbYM2612P_EmuMame,
                null,
                null,
                null
        );
        setChipType2FromControls(
                setting.getYM2612Type()[1],
                ucSI.rbYM2612S_SCCI,
                ucSI.cmbYM2612S_SCCI,
                ucSI.rbYM2612S_Emu,
                ucSI.rbYM2612S_EmuNuked,
                ucSI.rbYM2612S_EmuMame,
                null,
                null,
                null
        );
        setting.getYM2612Type()[0].getRealChipInfo()[0].setUseWait(ucSI.cbSendWait.isSelected());
        setting.getYM2612Type()[0].getRealChipInfo()[0].setUseWaitBoost(ucSI.cbTwice.isSelected());
        setting.getYM2612Type()[0].getRealChipInfo()[0].setOnlyPCMEmulation(ucSI.cbEmulationPCMOnly.isSelected());
        setChipType2FromControls(
                setting.getSN76489Type()[0],
                ucSI.rbSN76489P_SCCI,
                ucSI.cmbSN76489P_SCCI,
                ucSI.rbSN76489P_Emu,
                ucSI.rbSN76489P_Emu2,
                null,
                null,
                null,
                null
        );
        setChipType2FromControls(
                setting.getSN76489Type()[1],
                ucSI.rbSN76489S_SCCI,
                ucSI.cmbSN76489S_SCCI,
                ucSI.rbSN76489S_Emu,
                ucSI.rbSN76489S_Emu2,
                null,
                null,
                null,
                null
        );
        setChipType2FromControls(
                setting.getYM2608Type()[0],
                ucSI.rbYM2608P_SCCI,
                ucSI.cmbYM2608P_SCCI,
                ucSI.rbYM2608P_Emu,
                null,
                null,
                null,
                null,
                null
        );
        setChipType2FromControls(
                setting.getYM2608Type()[1],
                ucSI.rbYM2608S_SCCI,
                ucSI.cmbYM2608S_SCCI,
                ucSI.rbYM2608S_Emu,
                null,
                null,
                null,
                null,
                null
        );
        setChipType2FromControls(
                setting.getYM2610Type()[0],
                ucSI.rbYM2610BP_SCCI,
                ucSI.cmbYM2610BP_SCCI,
                ucSI.rbYM2610BP_Emu,
                null,
                null,
                ucSI.rbYM2610BEP_SCCI,
                ucSI.cmbYM2610BEP_SCCI,
                ucSI.cmbSPPCMP_SCCI
        );
        setChipType2FromControls(
                setting.getYM2610Type()[1],
                ucSI.rbYM2610BS_SCCI,
                ucSI.cmbYM2610BS_SCCI,
                ucSI.rbYM2610BS_Emu,
                null,
                null,
                ucSI.rbYM2610BES_SCCI,
                ucSI.cmbYM2610BES_SCCI,
                ucSI.cmbSPPCMS_SCCI
        );
        setChipType2FromControls(
                setting.getYM2151Type()[0],
                ucSI.rbYM2151P_SCCI,
                ucSI.cmbYM2151P_SCCI,
                ucSI.rbYM2151P_Emu,
                ucSI.rbYM2151P_EmuMame,
                ucSI.rbYM2151P_EmuX68Sound,
                null,
                null,
                null
        );
        setChipType2FromControls(
                setting.getYM2151Type()[1],
                ucSI.rbYM2151S_SCCI,
                ucSI.cmbYM2151S_SCCI,
                ucSI.rbYM2151S_Emu,
                ucSI.rbYM2151S_EmuMame,
                ucSI.rbYM2151S_EmuX68Sound,
                null,
                null,
                null
        );
        setChipType2FromControls(
                setting.getYM2203Type()[0],
                ucSI.rbYM2203P_SCCI,
                ucSI.cmbYM2203P_SCCI,
                ucSI.rbYM2203P_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getYM2203Type()[1],
                ucSI.rbYM2203S_SCCI,
                ucSI.cmbYM2203S_SCCI,
                ucSI.rbYM2203S_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getAY8910Type()[0],
                ucSI.rbAY8910P_Real,
                ucSI.cmbAY8910P_Real,
                ucSI.rbAY8910P_Emu,
                ucSI.rbAY8910P_Emu2,
                null, null, null, null
        );
        setChipType2FromControls(
                setting.getAY8910Type()[1],
                ucSI.rbAY8910S_Real,
                ucSI.cmbAY8910S_Real,
                ucSI.rbAY8910S_Emu,
                ucSI.rbAY8910S_Emu2,
                null, null, null, null
        );
        setChipType2FromControls(
                setting.getK051649Type()[0],
                ucSI.rbK051649P_Real,
                ucSI.cmbK051649P_Real,
                ucSI.rbK051649P_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getK051649Type()[1],
                ucSI.rbK051649S_Real,
                ucSI.cmbK051649S_Real,
                ucSI.rbK051649S_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getYM2413Type()[0],
                ucSI.rbYM2413P_Real,
                ucSI.cmbYM2413P_Real,
                ucSI.rbYM2413P_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getYM2413Type()[1],
                ucSI.rbYM2413S_Real,
                ucSI.cmbYM2413S_Real,
                ucSI.rbYM2413S_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getC140Type()[0],
                ucSI.rbC140P_Real,
                ucSI.cmbC140P_SCCI,
                ucSI.rbC140P_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getC140Type()[1],
                ucSI.rbC140S_SCCI,
                ucSI.cmbC140S_SCCI,
                ucSI.rbC140S_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getSEGAPCMType()[0],
                ucSI.rbSEGAPCMP_SCCI,
                ucSI.cmbSEGAPCMP_SCCI,
                ucSI.rbSEGAPCMP_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getSEGAPCMType()[1],
                ucSI.rbSEGAPCMS_SCCI,
                ucSI.cmbSEGAPCMS_SCCI,
                ucSI.rbSEGAPCMS_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getYM3526Type()[0],
                ucSI.rbYM3526P_SCCI,
                ucSI.cmbYM3526P_SCCI,
                ucSI.rbYM3526P_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getYM3526Type()[1],
                ucSI.rbYM3526S_SCCI,
                ucSI.cmbYM3526S_SCCI,
                ucSI.rbYM3526S_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getYM3812Type()[0],
                ucSI.rbYM3812P_SCCI,
                ucSI.cmbYM3812P_SCCI,
                ucSI.rbYM3812P_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getYM3812Type()[1],
                ucSI.rbYM3812S_SCCI,
                ucSI.cmbYM3812S_SCCI,
                ucSI.rbYM3812S_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getYMF262Type()[0],
                ucSI.rbYMF262P_SCCI,
                ucSI.cmbYMF262P_SCCI,
                ucSI.rbYMF262P_Emu,
                null, null, null, null, null
        );
        setChipType2FromControls(
                setting.getYMF262Type()[1],
                ucSI.rbYMF262S_SCCI,
                ucSI.cmbYMF262S_SCCI,
                ucSI.rbYMF262S_Emu,
                null, null, null, null, null
        );
        setting.setLatencyEmulation(FormSetting.parseIntSafe(tbLatencyEmu.getText(), 0, 999, setting.getLatencyEmulation()));
        setting.setLatencySCCI(FormSetting.parseIntSafe(tbLatencySCCI.getText(), 0, 999, setting.getLatencySCCI()));
        setting.setHiyorimiMode(cbHiyorimiMode.isSelected());
    }

    @Override
    public boolean check() {
        HashSet<String> hsSCCIs = new HashSet<>();
        boolean ret = false;

        // SCCI duplicate setting check

        if (this.ucSI.rbYM2612P_SCCI.isSelected())
            if (this.ucSI.cmbYM2612P_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2612P_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2612P_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbYM2612S_SCCI.isSelected())
            if (this.ucSI.cmbYM2612S_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2612S_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2612S_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbSN76489P_SCCI.isSelected())
            if (this.ucSI.cmbSN76489P_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbSN76489P_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbSN76489P_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbSN76489S_SCCI.isSelected())
            if (this.ucSI.cmbSN76489S_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbSN76489S_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbSN76489S_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbYM2608P_SCCI.isSelected())
            if (this.ucSI.cmbYM2608P_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2608P_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2608P_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbYM2608S_SCCI.isSelected())
            if (this.ucSI.cmbYM2608S_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2608S_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2608S_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbYM2151P_SCCI.isSelected())
            if (this.ucSI.cmbYM2151P_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2151P_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2151P_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbYM2151S_SCCI.isSelected())
            if (this.ucSI.cmbYM2151S_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2151S_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2151S_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbYM2203P_SCCI.isSelected())
            if (this.ucSI.cmbYM2203P_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2203P_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2203P_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbYM2203S_SCCI.isSelected())
            if (this.ucSI.cmbYM2203S_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2203S_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2203S_SCCI.getSelectedItem().toString());
                else ret = true;


        if (this.ucSI.rbYM2413P_Real.isSelected())
            if (this.ucSI.cmbYM2413P_Real.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2413P_Real.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2413P_Real.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbYM2413S_Real.isSelected())
            if (this.ucSI.cmbYM2413S_Real.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2413S_Real.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2413S_Real.getSelectedItem().toString());
                else ret = true;


        if (this.ucSI.rbYM2610BP_SCCI.isSelected())
            if (this.ucSI.cmbYM2610BP_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2610BP_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2610BP_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbYM2610BS_SCCI.isSelected())
            if (this.ucSI.cmbYM2610BS_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2610BS_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2610BS_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbYM2610BEP_SCCI.isSelected())
            if (this.ucSI.cmbYM2610BEP_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2610BEP_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2610BEP_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbYM2610BES_SCCI.isSelected())
            if (this.ucSI.cmbYM2610BES_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbYM2610BES_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbYM2610BES_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbYM2610BEP_SCCI.isSelected())
            if (this.ucSI.cmbSPPCMP_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbSPPCMP_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbSPPCMP_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbYM2610BES_SCCI.isSelected())
            if (this.ucSI.cmbSPPCMS_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbSPPCMS_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbSPPCMS_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbC140P_Real.isSelected())
            if (this.ucSI.cmbC140P_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbC140P_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbC140P_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbC140S_SCCI.isSelected())
            if (this.ucSI.cmbC140S_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbC140S_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbC140S_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbSEGAPCMP_SCCI.isSelected())
            if (this.ucSI.cmbSEGAPCMP_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbSEGAPCMP_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbSEGAPCMP_SCCI.getSelectedItem().toString());
                else ret = true;

        if (this.ucSI.rbSEGAPCMS_SCCI.isSelected())
            if (this.ucSI.cmbSEGAPCMS_SCCI.getSelectedItem() != null)
                if (!hsSCCIs.contains(this.ucSI.cmbSEGAPCMS_SCCI.getSelectedItem().toString()))
                    hsSCCIs.add(this.ucSI.cmbSEGAPCMS_SCCI.getSelectedItem().toString());
                else ret = true;

        if (ret) {
            return JOptionPane.showConfirmDialog(null,
                    "Duplicate SCCI/GIMIC devices are configured. Do you want to continue?",
                    "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
            ) == JOptionPane.YES_OPTION;
        }

        return true;
    }

    private static void setRealCombo(EnmRealChipType realType, JComboBox<String> cmbP, JCheckBox rbP, JComboBox<String> cmbS, JCheckBox rbS) {

        if (rbP != null) rbP.setEnabled(false);
        cmbP.setEnabled(false);

        if (rbS != null) rbS.setEnabled(false);
        cmbS.setEnabled(false);

        // the real chip list hangs off the plugin, and there is none until a song is loaded, so
        // settings opened before that offers no real chip — same as having none attached
        if (Audio.getInstance().plugin == null) return;

        List<ChipType2> lstChip = Audio.getInstance().plugin.chipRegister.plugin(RealChipPlugin.class).getRealChipList(realType);
        if (lstChip == null || lstChip.isEmpty()) return;

        for (ChipType2 ct : lstChip) {
            if (ct == null || ct.getRealChipInfo() == null || ct.getRealChipInfo().length == 0 || ct.getRealChipInfo()[0] == null)
                continue;

            cmbP.addItem("(%s:%s:%s:%s)%s".formatted(
                    ct.getRealChipInfo()[0].getInterfaceName(),
                    ct.getRealChipInfo()[0].getSoundLocation(),
                    ct.getRealChipInfo()[0].getBusID(),
                    ct.getRealChipInfo()[0].getSoundChip(),
                    ct.getRealChipInfo()[0].getChipName()));

            cmbS.addItem("(%s:%s:%s:%s)%s".formatted(
                    ct.getRealChipInfo()[0].getInterfaceName(),
                    ct.getRealChipInfo()[0].getSoundLocation(),
                    ct.getRealChipInfo()[0].getBusID(),
                    ct.getRealChipInfo()[0].getSoundChip(),
                    ct.getRealChipInfo()[0].getChipName()));
        }

        if (cmbP.getItemCount() > 0) {
            cmbP.setSelectedIndex(0);
            if (rbP != null) rbP.setEnabled(true);
            cmbP.setEnabled(true);
        }

        if (cmbS.getItemCount() > 0) {
            cmbS.setSelectedIndex(0);
            if (rbS != null) rbS.setEnabled(true);
            cmbS.setEnabled(true);
        }
    }

    private static void setChipType2FromControls(
            ChipType2 ct,
            JCheckBox rb_SCCI,
            JComboBox<String> cmb_SCCI,
            JCheckBox rb_EMU0,
            JCheckBox rb_EMU1,
            JCheckBox rb_EMU2,
            JCheckBox rb_SCCI_E,
            JComboBox<String> cmb_SCCI_E1,
            JComboBox<String> cmb_SCCI_E2
    ) {
        ct.setUseReal(new boolean[rb_SCCI_E == null ? 1 : 3]);
        ct.getUseReal()[0] = rb_SCCI.isSelected();
        ct.setRealChipInfo(new ChipType2.RealChipInfo[rb_SCCI_E == null ? 1 : 3]);
        for (int i = 0; i < ct.getRealChipInfo().length; i++) ct.getRealChipInfo()[i] = new ChipType2.RealChipInfo();
        int v;
        ChipType2.RealChipInfo rci;
        if (rb_SCCI.isSelected()) {
            if (cmb_SCCI.getSelectedItem() != null) {
                String n = cmb_SCCI.getSelectedItem().toString();
                int idx = n.indexOf(")");
                if (idx != -1 && n.startsWith("(")) {
                    n = n.substring(1, idx);
                    String[] ns = n.split(":");
                    if (ns.length >= 3) {
                        rci = ct.getRealChipInfo()[0];
                        rci.setInterfaceName(String.join(":", Arrays.copyOfRange(ns, 0, ns.length - 3)));
                        try {
                            v = Integer.parseInt(ns[ns.length - 3]);
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, e);
                            v = 0;
                        }
                        rci.setSoundLocation(v);
                        try {
                            v = Integer.parseInt(ns[ns.length - 2]);
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, e);
                            v = 0;
                        }
                        rci.setBusID(v);
                        try {
                            v = Integer.parseInt(ns[ns.length - 1]);
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, e);
                            v = 0;
                        }
                        rci.setSoundChip(v);
                    }
                }
            }
        }

        ct.setUseEmu(null);
        if (rb_EMU2 != null) ct.setUseEmu(new boolean[3]);
        else if (rb_EMU1 != null) ct.setUseEmu(new boolean[2]);
        else if (rb_EMU0 != null) ct.setUseEmu(new boolean[1]);
        if (rb_EMU0 != null)
            ct.getUseEmu()[0] = rb_EMU0.isSelected(); // (rb_EMU0.isSelected() || rb_SCCI.isSelected());
        if (rb_EMU1 != null) ct.getUseEmu()[1] = rb_EMU1.isSelected();
        if (rb_EMU2 != null) ct.getUseEmu()[2] = rb_EMU2.isSelected();

        if (rb_SCCI_E != null && rb_SCCI_E.isSelected()) {
            if (cmb_SCCI_E1.getSelectedItem() != null) {
                String n = cmb_SCCI_E1.getSelectedItem().toString();
                int idx = n.indexOf(")");
                if (idx != -1 && n.startsWith("(")) {
                    n = n.substring(1, idx);
                    String[] ns = n.split(":");
                    if (ns.length >= 3) {
                        rci = ct.getRealChipInfo()[1];
                        rci.setInterfaceName(String.join(":", Arrays.copyOfRange(ns, 0, ns.length - 3)));
                        try {
                            v = Integer.parseInt(ns[ns.length - 3]);
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, e);
                            v = 0;
                        }
                        rci.setSoundLocation(v);
                        try {
                            v = Integer.parseInt(ns[ns.length - 2]);
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, e);
                            v = 0;
                        }
                        rci.setBusID(v);
                        try {
                            v = Integer.parseInt(ns[ns.length - 1]);
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, e);
                            v = 0;
                        }
                        rci.setSoundChip(v);
                    }
                }
            }
            if (cmb_SCCI_E2.getSelectedItem() != null) {
                String n = cmb_SCCI_E2.getSelectedItem().toString();
                int idx = n.indexOf(")");
                if (idx != -1 && n.startsWith("(")) {
                    n = n.substring(1, idx);
                    String[] ns = n.split(":");
                    if (ns.length >= 3) {
                        rci = ct.getRealChipInfo()[2];
                        rci.setInterfaceName(String.join(":", Arrays.copyOfRange(ns, 0, ns.length - 3)));
                        try {
                            v = Integer.parseInt(ns[ns.length - 3]);
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, e);
                            v = 0;
                        }
                        rci.setSoundLocation(v);
                        try {
                            v = Integer.parseInt(ns[ns.length - 2]);
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, e);
                            v = 0;
                        }
                        rci.setBusID(v);
                        try {
                            v = Integer.parseInt(ns[ns.length - 1]);
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, e);
                            v = 0;
                        }
                        rci.setSoundChip(v);
                    }
                }
            }
        }
    }

    private static void setRealParam(ChipType2 chipType2,
                                     JCheckBox rbSilent,
                                     JCheckBox rbEmu,
                                     JCheckBox rbReal,
                                     JComboBox<String> cmbP,
                                     JCheckBox rbReal2 /* = null */,
                                     JComboBox<String> cmbP2A /* = null */,
                                     JComboBox<String> cmbP2B /* = null */,
                                     JCheckBox rbEmu2 /* = null */,
                                     JCheckBox rbEmu3/* = null */) {
        String n = "";

        if (chipType2.getRealChipInfo() != null && chipType2.getRealChipInfo().length > 0 && chipType2.getRealChipInfo()[0] != null) {
            n = "(%s:%s:%s:%s)".formatted(
                    chipType2.getRealChipInfo()[0].getInterfaceName(),
                    chipType2.getRealChipInfo()[0].getSoundLocation(),
                    chipType2.getRealChipInfo()[0].getBusID(),
                    chipType2.getRealChipInfo()[0].getSoundChip());
        }

        if (cmbP.getItemCount() > 0) {
            for (int i = 0; i < cmbP.getItemCount(); i++) {
                if (!cmbP.getItemAt(i).contains(n)) continue;
                cmbP.setSelectedItem(i);

                break;
            }
        }

        if (cmbP2A != null) {
            if (chipType2.getRealChipInfo() != null && chipType2.getRealChipInfo().length > 1 && chipType2.getRealChipInfo()[1] != null) {
                n = "(%s:%s:%s:%s)".formatted(
                        chipType2.getRealChipInfo()[1].getInterfaceName(),
                        chipType2.getRealChipInfo()[1].getSoundLocation(),
                        chipType2.getRealChipInfo()[1].getBusID(),
                        chipType2.getRealChipInfo()[1].getSoundChip());
            }

            if (cmbP2A.getItemCount() > 0) {
                for (int i = 0; i < cmbP2A.getItemCount(); i++) {
                    if (!cmbP2A.getItemAt(i).contains(n)) continue;
                    cmbP2A.setSelectedItem(i);

                    break;
                }
            }
        }

        if (cmbP2B != null) {
            if (chipType2.getRealChipInfo() != null && chipType2.getRealChipInfo().length > 2 && chipType2.getRealChipInfo()[2] != null) {
                n = "(%s:%s:%s:%s)".formatted(
                        chipType2.getRealChipInfo()[2].getInterfaceName(),
                        chipType2.getRealChipInfo()[2].getSoundLocation(),
                        chipType2.getRealChipInfo()[2].getBusID(),
                        chipType2.getRealChipInfo()[2].getSoundChip());
            }

            if (cmbP2B.getItemCount() > 0) {
                for (int i = 0; i < cmbP2B.getItemCount(); i++) {
                    if (!cmbP2B.getItemAt(i).contains(n)) continue;
                    cmbP2B.setSelectedItem(i);

                    break;
                }
            }
        }

        if (rbEmu2 != null && chipType2.getUseEmu().length > 1 && chipType2.getUseEmu()[1]) {
            rbEmu2.setSelected(true);
            return;
        }

        if (rbEmu3 != null && chipType2.getUseEmu().length > 2 && chipType2.getUseEmu()[2]) {
            rbEmu3.setSelected(true);
            return;
        }

        if ((chipType2.getUseReal().length > 0 && !chipType2.getUseReal()[0])
                && (chipType2.getUseReal().length > 1 && !chipType2.getUseReal()[1]))
        // rbSCCI2==null) || (!chipType2.UseScci && rbSCCI2 != null && !chipType2.UseScci2))
        {
            if (chipType2.getUseEmu()[0])
                rbEmu.setSelected(true);
            else
                rbSilent.setSelected(true);

            return;
        }

        if (((chipType2.getUseReal().length > 0 && chipType2.getUseReal()[0]) && !cmbP.isEnabled())
                || ((chipType2.getUseReal().length > 1 && chipType2.getUseReal()[1])
                && !cmbP2A.isEnabled()
                && !cmbP2B.isEnabled())
        ) {
            rbEmu.setSelected(true);

            return;
        }

        if (chipType2.getUseReal().length > 0 && chipType2.getUseReal()[0]) {
            rbReal.setSelected(true);
            return;
        }

        if (rbReal2 != null) rbReal2.setSelected(true);
    }
}
