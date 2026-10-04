package mdplayer.form.sys.setting;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import mdplayer.form.Grid;
import javax.swing.event.ChangeEvent;


public class SettingInstrumentsPanel extends JPanel {

    public SettingInstrumentsPanel() {
        initializeComponent();
    }

    private void cbSendWait_CheckedChanged(ChangeEvent ev) {
        cbTwice.setEnabled(cbSendWait.isSelected());
    }

    private void initializeComponent() {
        this.cmbYM2151P_SCCI = new JComboBox<>();
        this.rbYM2151P_SCCI = new JCheckBox();
        this.rbYM2151P_Silent = new JCheckBox();
        this.rbYM2151P_Emu = new JCheckBox();
        this.groupBox1 = new JPanel();
        this.rbYM2151P_EmuX68Sound = new JCheckBox();
        this.rbYM2151P_EmuMame = new JCheckBox();
        this.groupBox2 = new JPanel();
        this.cmbYM2151S_SCCI = new JComboBox<>();
        this.rbYM2151S_SCCI = new JCheckBox();
        this.rbYM2151S_Silent = new JCheckBox();
        this.rbYM2151S_EmuX68Sound = new JCheckBox();
        this.rbYM2151S_EmuMame = new JCheckBox();
        this.rbYM2151S_Emu = new JCheckBox();
        this.cmbYM2203P_SCCI = new JComboBox<>();
        this.rbYM2203P_SCCI = new JCheckBox();
        this.rbYM2203P_Emu = new JCheckBox();
        this.groupBox3 = new JPanel();
        this.rbYM2203P_Silent = new JCheckBox();
        this.groupBox4 = new JPanel();
        this.cmbYM2203S_SCCI = new JComboBox<>();
        this.rbYM2203S_SCCI = new JCheckBox();
        this.rbYM2203S_Silent = new JCheckBox();
        this.rbYM2203S_Emu = new JCheckBox();
        this.cmbYM2608P_SCCI = new JComboBox<>();
        this.rbYM2608P_SCCI = new JCheckBox();
        this.rbYM2608P_Emu = new JCheckBox();
        this.groupBox5 = new JPanel();
        this.rbYM2608P_Silent = new JCheckBox();
        this.groupBox6 = new JPanel();
        this.cmbYM2608S_SCCI = new JComboBox<>();
        this.rbYM2608S_SCCI = new JCheckBox();
        this.rbYM2608S_Silent = new JCheckBox();
        this.rbYM2608S_Emu = new JCheckBox();
        this.cmbYM2610BP_SCCI = new JComboBox<>();
        this.rbYM2610BP_SCCI = new JCheckBox();
        this.rbYM2610BP_Emu = new JCheckBox();
        this.groupBox7 = new JPanel();
        this.label1 = new JLabel();
        this.cmbYM2610BEP_SCCI = new JComboBox<>();
        this.cmbSPPCMP_SCCI = new JComboBox<>();
        this.rbYM2610BEP_SCCI = new JCheckBox();
        this.rbYM2610BP_Silent = new JCheckBox();
        this.groupBox8 = new JPanel();
        this.label2 = new JLabel();
        this.cmbYM2610BS_SCCI = new JComboBox<>();
        this.cmbYM2610BES_SCCI = new JComboBox<>();
        this.rbYM2610BS_SCCI = new JCheckBox();
        this.cmbSPPCMS_SCCI = new JComboBox<>();
        this.rbYM2610BS_Silent = new JCheckBox();
        this.rbYM2610BS_Emu = new JCheckBox();
        this.rbYM2610BES_SCCI = new JCheckBox();
        this.cbEmulationPCMOnly = new JCheckBox();
        this.cbTwice = new JCheckBox();
        this.cbSendWait = new JCheckBox();
        this.cmbYM2612P_SCCI = new JComboBox<>();
        this.rbYM2612P_SCCI = new JCheckBox();
        this.rbYM2612P_Emu = new JCheckBox();
        this.groupBox9 = new JPanel();
        this.rbYM2612P_Silent = new JCheckBox();
        this.rbYM2612P_EmuNuked = new JCheckBox();
        this.rbYM2612P_EmuMame = new JCheckBox();
        this.groupBox10 = new JPanel();
        this.rbYM2612S_Silent = new JCheckBox();
        this.cmbYM2612S_SCCI = new JComboBox<>();
        this.rbYM2612S_SCCI = new JCheckBox();
        this.rbYM2612S_EmuNuked = new JCheckBox();
        this.rbYM2612S_EmuMame = new JCheckBox();
        this.rbYM2612S_Emu = new JCheckBox();
        this.cmbSN76489P_SCCI = new JComboBox<>();
        this.rbSN76489P_SCCI = new JCheckBox();
        this.rbSN76489P_Emu = new JCheckBox();
        this.groupBox11 = new JPanel();
        this.rbSN76489P_Silent = new JCheckBox();
        this.rbSN76489P_Emu2 = new JCheckBox();
        this.groupBox12 = new JPanel();
        this.cmbSN76489S_SCCI = new JComboBox<>();
        this.rbSN76489S_SCCI = new JCheckBox();
        this.rbSN76489S_Silent = new JCheckBox();
        this.rbSN76489S_Emu2 = new JCheckBox();
        this.rbSN76489S_Emu = new JCheckBox();
        this.groupBox13 = new JPanel();
        this.groupBox14 = new JPanel();
        this.cmbC140S_SCCI = new JComboBox<>();
        this.rbC140S_SCCI = new JCheckBox();
        this.rbC140S_Silent = new JCheckBox();
        this.rbC140S_Emu = new JCheckBox();
        this.groupBox15 = new JPanel();
        this.cmbC140P_SCCI = new JComboBox<>();
        this.rbC140P_Real = new JCheckBox();
        this.rbC140P_Silent = new JCheckBox();
        this.rbC140P_Emu = new JCheckBox();
        this.groupBox16 = new JPanel();
        this.cmbSEGAPCMS_SCCI = new JComboBox<>();
        this.rbSEGAPCMS_SCCI = new JCheckBox();
        this.rbSEGAPCMS_Silent = new JCheckBox();
        this.rbSEGAPCMS_Emu = new JCheckBox();
        this.groupBox17 = new JPanel();
        this.cmbSEGAPCMP_SCCI = new JComboBox<>();
        this.rbSEGAPCMP_SCCI = new JCheckBox();
        this.rbSEGAPCMP_Silent = new JCheckBox();
        this.rbSEGAPCMP_Emu = new JCheckBox();
        this.groupBox18 = new JPanel();
        this.cmbYMF262P_SCCI = new JComboBox<>();
        this.rbYMF262P_SCCI = new JCheckBox();
        this.rbYMF262P_Silent = new JCheckBox();
        this.rbYMF262P_Emu = new JCheckBox();
        this.groupBox19 = new JPanel();
        this.cmbYMF262S_SCCI = new JComboBox<>();
        this.rbYMF262S_SCCI = new JCheckBox();
        this.rbYMF262S_Silent = new JCheckBox();
        this.rbYMF262S_Emu = new JCheckBox();
        this.groupBox20 = new JPanel();
        this.cmbYM3812P_SCCI = new JComboBox<>();
        this.rbYM3812P_SCCI = new JCheckBox();
        this.rbYM3812P_Silent = new JCheckBox();
        this.rbYM3812P_Emu = new JCheckBox();
        this.groupBox21 = new JPanel();
        this.cmbYM3812S_SCCI = new JComboBox<>();
        this.rbYM3812S_SCCI = new JCheckBox();
        this.rbYM3812S_Silent = new JCheckBox();
        this.rbYM3812S_Emu = new JCheckBox();
        this.groupBox22 = new JPanel();
        this.cmbYM3526S_SCCI = new JComboBox<>();
        this.rbYM3526S_SCCI = new JCheckBox();
        this.rbYM3526S_Silent = new JCheckBox();
        this.rbYM3526S_Emu = new JCheckBox();
        this.groupBox23 = new JPanel();
        this.cmbYM3526P_SCCI = new JComboBox<>();
        this.rbYM3526P_SCCI = new JCheckBox();
        this.rbYM3526P_Silent = new JCheckBox();
        this.rbYM3526P_Emu = new JCheckBox();
        this.groupBox24 = new JPanel();
        this.cmbYM2413S_Real = new JComboBox<>();
        this.rbYM2413S_Real = new JCheckBox();
        this.rbYM2413S_Silent = new JCheckBox();
        this.rbYM2413S_Emu = new JCheckBox();
        this.groupBox25 = new JPanel();
        this.cmbYM2413P_Real = new JComboBox<>();
        this.rbYM2413P_Real = new JCheckBox();
        this.rbYM2413P_Silent = new JCheckBox();
        this.rbYM2413P_Emu = new JCheckBox();
        this.groupBox26 = new JPanel();
        this.cmbAY8910P_Real = new JComboBox<>();
        this.rbAY8910P_Real = new JCheckBox();
        this.rbAY8910P_Silent = new JCheckBox();
        this.rbAY8910P_Emu = new JCheckBox();
        this.groupBox27 = new JPanel();
        this.cmbAY8910S_Real = new JComboBox<>();
        this.rbAY8910S_Real = new JCheckBox();
        this.rbAY8910S_Silent = new JCheckBox();
        this.rbAY8910S_Emu = new JCheckBox();
        this.groupBox28 = new JPanel();
        this.cmbK051649S_Real = new JComboBox<>();
        this.rbK051649S_Real = new JCheckBox();
        this.rbK051649S_Silent = new JCheckBox();
        this.rbK051649S_Emu = new JCheckBox();
        this.groupBox29 = new JPanel();
        this.cmbK051649P_Real = new JComboBox<>();
        this.rbK051649P_Real = new JCheckBox();
        this.rbK051649P_Silent = new JCheckBox();
        this.rbK051649P_Emu = new JCheckBox();
        this.rbAY8910P_Emu2 = new JCheckBox();
        this.rbAY8910S_Emu2 = new JCheckBox();

        //
        // cmbYM2151P_SCCI
        //
        this.cmbYM2151P_SCCI.setName("cmbYM2151P_SCCI");
        //
        // rbYM2151P_SCCI
        //
        this.rbYM2151P_SCCI.setName("rbYM2151P_SCCI");
        this.rbYM2151P_SCCI.setText("Real");
        //
        // rbYM2151P_Silent
        //
        this.rbYM2151P_Silent.setName("rbYM2151P_Silent");
        this.rbYM2151P_Silent.setText("Silent");
        //
        // rbYM2151P_Emu
        //
        this.rbYM2151P_Emu.setSelected(true);
        this.rbYM2151P_Emu.setName("rbYM2151P_Emu");
        this.rbYM2151P_Emu.setText("Emulation (fmgen)");
        //
        // groupBox1
        //
        this.groupBox1.setName("groupBox1");
        this.groupBox1.setToolTipText("YM2151(Primary)");
        //
        // rbYM2151P_EmuX68Sound
        //
        this.rbYM2151P_EmuX68Sound.setName("rbYM2151P_EmuX68Sound");
        this.rbYM2151P_EmuX68Sound.setText("Emulation (X68Sound)");
        //
        // rbYM2151P_EmuMame
        //
        this.rbYM2151P_EmuMame.setName("rbYM2151P_EmuMame");
        this.rbYM2151P_EmuMame.setText("Emulation (mame)");
        //
        // groupBox2
        //
        this.groupBox2.setName("groupBox2");
        this.groupBox2.setToolTipText("YM2151(Secondary)");
        //
        // cmbYM2151S_SCCI
        //
        this.cmbYM2151S_SCCI.setName("cmbYM2151S_SCCI");
        //
        // rbYM2151S_SCCI
        //
        this.rbYM2151S_SCCI.setName("rbYM2151S_SCCI");
        this.rbYM2151S_SCCI.setText("Real");
        //
        // rbYM2151S_Silent
        //
        this.rbYM2151S_Silent.setName("rbYM2151S_Silent");
        this.rbYM2151S_Silent.setText("Silent");
        //
        // rbYM2151S_EmuX68Sound
        //
        this.rbYM2151S_EmuX68Sound.setName("rbYM2151S_EmuX68Sound");
        this.rbYM2151S_EmuX68Sound.setText("Emulation (X68Sound)");
        //
        // rbYM2151S_EmuMame
        //
        this.rbYM2151S_EmuMame.setName("rbYM2151S_EmuMame");
        this.rbYM2151S_EmuMame.setText("Emulation (mame)");
        //
        // rbYM2151S_Emu
        //
        this.rbYM2151S_Emu.setSelected(true);
        this.rbYM2151S_Emu.setName("rbYM2151S_Emu");
        this.rbYM2151S_Emu.setText("Emulation (fmgen)");
        //
        // cmbYM2203P_SCCI
        //
        this.cmbYM2203P_SCCI.setName("cmbYM2203P_SCCI");
        //
        // rbYM2203P_SCCI
        //
        this.rbYM2203P_SCCI.setName("rbYM2203P_SCCI");
        this.rbYM2203P_SCCI.setText("Real");
        //
        // rbYM2203P_Emu
        //
        this.rbYM2203P_Emu.setSelected(true);
        this.rbYM2203P_Emu.setName("rbYM2203P_Emu");
        this.rbYM2203P_Emu.setText("Emulation");
        //
        // groupBox3
        //
        this.groupBox3.setName("groupBox3");
        this.groupBox3.setToolTipText("YM2203(Primary)");
        //
        // rbYM2203P_Silent
        //
        this.rbYM2203P_Silent.setName("rbYM2203P_Silent");
        this.rbYM2203P_Silent.setText("Silent");
        //
        // groupBox4
        //
        this.groupBox4.setName("groupBox4");
        this.groupBox4.setToolTipText("YM2203(Secondary)");
        //
        // cmbYM2203S_SCCI
        //
        this.cmbYM2203S_SCCI.setName("cmbYM2203S_SCCI");
        //
        // rbYM2203S_SCCI
        //
        this.rbYM2203S_SCCI.setName("rbYM2203S_SCCI");
        this.rbYM2203S_SCCI.setText("Real");
        //
        // rbYM2203S_Silent
        //
        this.rbYM2203S_Silent.setName("rbYM2203S_Silent");
        this.rbYM2203S_Silent.setText("Silent");
        //
        // rbYM2203S_Emu
        //
        this.rbYM2203S_Emu.setSelected(true);
        this.rbYM2203S_Emu.setName("rbYM2203S_Emu");
        this.rbYM2203S_Emu.setText("Emulation");
        //
        // cmbYM2608P_SCCI
        //
        this.cmbYM2608P_SCCI.setName("cmbYM2608P_SCCI");
        //
        // rbYM2608P_SCCI
        //
        this.rbYM2608P_SCCI.setName("rbYM2608P_SCCI");
        this.rbYM2608P_SCCI.setText("Real");
        //
        // rbYM2608P_Emu
        //
        this.rbYM2608P_Emu.setSelected(true);
        this.rbYM2608P_Emu.setName("rbYM2608P_Emu");
        this.rbYM2608P_Emu.setText("Emulation");
        //
        // groupBox5
        //
        this.groupBox5.setName("groupBox5");
        this.groupBox5.setToolTipText("YM2608(Primary)");
        //
        // rbYM2608P_Silent
        //
        this.rbYM2608P_Silent.setName("rbYM2608P_Silent");
        this.rbYM2608P_Silent.setText("Silent");
        //
        // groupBox6
        //
        this.groupBox6.setName("groupBox6");
        this.groupBox6.setToolTipText("YM2608(Secondary)");
        //
        // cmbYM2608S_SCCI
        //
        this.cmbYM2608S_SCCI.setName("cmbYM2608S_SCCI");
        //
        // rbYM2608S_SCCI
        //
        this.rbYM2608S_SCCI.setName("rbYM2608S_SCCI");
        this.rbYM2608S_SCCI.setText("Real");
        //
        // rbYM2608S_Silent
        //
        this.rbYM2608S_Silent.setName("rbYM2608S_Silent");
        this.rbYM2608S_Silent.setText("Silent");
        //
        // rbYM2608S_Emu
        //
        this.rbYM2608S_Emu.setSelected(true);
        this.rbYM2608S_Emu.setName("rbYM2608S_Emu");
        this.rbYM2608S_Emu.setText("Emulation");
        //
        // cmbYM2610BP_SCCI
        //
        this.cmbYM2610BP_SCCI.setName("cmbYM2610BP_SCCI");
        //
        // rbYM2610BP_SCCI
        //
        this.rbYM2610BP_SCCI.setName("rbYM2610BP_SCCI");
        this.rbYM2610BP_SCCI.setText("Real");
        //
        // rbYM2610BP_Emu
        //
        this.rbYM2610BP_Emu.setSelected(true);
        this.rbYM2610BP_Emu.setName("rbYM2610BP_Emu");
        this.rbYM2610BP_Emu.setText("Emulation");
        //
        // groupBox7
        //
        this.groupBox7.setName("groupBox7");
        this.groupBox7.setToolTipText("YM2610/B(Primary)");
        //
        // label1
        //
        this.label1.setName("label1");
        this.label1.setText("+");
        //
        // cmbYM2610BEP_SCCI
        //
        this.cmbYM2610BEP_SCCI.setName("cmbYM2610BEP_SCCI");
        //
        // cmbSPPCMP_SCCI
        //
        this.cmbSPPCMP_SCCI.setName("cmbSPPCMP_SCCI");
        //
        // rbYM2610BEP_SCCI
        //
        this.rbYM2610BEP_SCCI.setName("rbYM2610BEP_SCCI");
        this.rbYM2610BEP_SCCI.setText("Real_2");
        //
        // rbYM2610BP_Silent
        //
        this.rbYM2610BP_Silent.setName("rbYM2610BP_Silent");
        this.rbYM2610BP_Silent.setText("Silent");
        //
        // groupBox8
        //
        this.groupBox8.setName("groupBox8");
        this.groupBox8.setToolTipText("YM2610/B(Secondary)");
        //
        // label2
        //
        this.label2.setName("label2");
        this.label2.setText("+");
        //
        // cmbYM2610BS_SCCI
        //
        this.cmbYM2610BS_SCCI.setName("cmbYM2610BS_SCCI");
        //
        // cmbYM2610BES_SCCI
        //
        this.cmbYM2610BES_SCCI.setName("cmbYM2610BES_SCCI");
        //
        // rbYM2610BS_SCCI
        //
        this.rbYM2610BS_SCCI.setName("rbYM2610BS_SCCI");
        this.rbYM2610BS_SCCI.setText("Real");
        //
        // cmbSPPCMS_SCCI
        //
        this.cmbSPPCMS_SCCI.setName("cmbSPPCMS_SCCI");
        //
        // rbYM2610BS_Silent
        //
        this.rbYM2610BS_Silent.setName("rbYM2610BS_Silent");
        this.rbYM2610BS_Silent.setText("Silent");
        //
        // rbYM2610BS_Emu
        //
        this.rbYM2610BS_Emu.setSelected(true);
        this.rbYM2610BS_Emu.setName("rbYM2610BS_Emu");
        this.rbYM2610BS_Emu.setText("Emulation");
        //
        // rbYM2610BES_SCCI
        //
        this.rbYM2610BES_SCCI.setName("rbYM2610BES_SCCI");
        this.rbYM2610BES_SCCI.setText("Real_2");
        //
        // cbEmulationPCMOnly
        //
        this.cbEmulationPCMOnly.setName("cbEmulationPCMOnly");
        this.cbEmulationPCMOnly.setText("Emulate PCM only");
        //
        // cbTwice
        //
        this.cbTwice.setName("cbTwice");
        this.cbTwice.setText("Double wait");
        //
        // cbSendWait
        //
        this.cbSendWait.setName("cbSendWait");
        this.cbSendWait.setText("Send Wait Signal");
        this.cbSendWait.addChangeListener(this::cbSendWait_CheckedChanged);
        //
        // cmbYM2612P_SCCI
        //
        this.cmbYM2612P_SCCI.setName("cmbYM2612P_SCCI");
        //
        // rbYM2612P_SCCI
        //
        this.rbYM2612P_SCCI.setName("rbYM2612P_SCCI");
        this.rbYM2612P_SCCI.setText("Real");
        //
        // rbYM2612P_Emu
        //
        this.rbYM2612P_Emu.setSelected(true);
        this.rbYM2612P_Emu.setName("rbYM2612P_Emu");
        this.rbYM2612P_Emu.setText("Emulation (Gens)");
        //
        // groupBox9
        //
        this.groupBox9.setName("groupBox9");
        this.groupBox9.setToolTipText("Ym2612Inst(Primary)");
        //
        // rbYM2612P_Silent
        //
        this.rbYM2612P_Silent.setName("rbYM2612P_Silent");
        this.rbYM2612P_Silent.setText("Silent");
        //
        // rbYM2612P_EmuNuked
        //
        this.rbYM2612P_EmuNuked.setName("rbYM2612P_EmuNuked");
        this.rbYM2612P_EmuNuked.setText("Emulation (Nuked-OPN2)");
        //
        // rbYM2612P_EmuMame
        //
        this.rbYM2612P_EmuMame.setName("rbYM2612P_EmuMame");
        this.rbYM2612P_EmuMame.setText("Emulation (mame)");
        //
        // groupBox10
        //
        this.groupBox10.setName("groupBox10");
        this.groupBox10.setToolTipText("Ym2612Inst(Secondary)");
        //
        // rbYM2612S_Silent
        //
        this.rbYM2612S_Silent.setName("rbYM2612S_Silent");
        this.rbYM2612S_Silent.setText("Silent");
        //
        // cmbYM2612S_SCCI
        //
        this.cmbYM2612S_SCCI.setName("cmbYM2612S_SCCI");
        //
        // rbYM2612S_SCCI
        //
        this.rbYM2612S_SCCI.setName("rbYM2612S_SCCI");
        this.rbYM2612S_SCCI.setText("Real");
        //
        // rbYM2612S_EmuNuked
        //
        this.rbYM2612S_EmuNuked.setName("rbYM2612S_EmuNuked");
        this.rbYM2612S_EmuNuked.setText("Emulation (Nuked-OPN2)");
        //
        // rbYM2612S_EmuMame
        //
        this.rbYM2612S_EmuMame.setName("rbYM2612S_EmuMame");
        this.rbYM2612S_EmuMame.setText("Emulation (mame)");
        //
        // rbYM2612S_Emu
        //
        this.rbYM2612S_Emu.setSelected(true);
        this.rbYM2612S_Emu.setName("rbYM2612S_Emu");
        this.rbYM2612S_Emu.setText("Emulation (Gens)");
        //
        // cmbSN76489P_SCCI
        //
        this.cmbSN76489P_SCCI.setName("cmbSN76489P_SCCI");
        //
        // rbSN76489P_SCCI
        //
        this.rbSN76489P_SCCI.setName("rbSN76489P_SCCI");
        this.rbSN76489P_SCCI.setText("Real");
        //
        // rbSN76489P_Emu
        //
        this.rbSN76489P_Emu.setSelected(true);
        this.rbSN76489P_Emu.setName("rbSN76489P_Emu");
        this.rbSN76489P_Emu.setText("Emulation (maxim)");
        //
        // groupBox11
        //
        this.groupBox11.setName("groupBox11");
        this.groupBox11.setToolTipText("SN76489(Primary)");
        //
        // rbSN76489P_Silent
        //
        this.rbSN76489P_Silent.setName("rbSN76489P_Silent");
        this.rbSN76489P_Silent.setText("Silent");
        //
        // rbSN76489P_Emu2
        //
        this.rbSN76489P_Emu2.setName("rbSN76489P_Emu2");
        this.rbSN76489P_Emu2.setText("Emulation (mame)");
        //
        // groupBox12
        //
        this.groupBox12.setName("groupBox12");
        this.groupBox12.setToolTipText("SN76489(Secondary)");
        //
        // cmbSN76489S_SCCI
        //
        this.cmbSN76489S_SCCI.setName("cmbSN76489S_SCCI");
        //
        // rbSN76489S_SCCI
        //
        this.rbSN76489S_SCCI.setName("rbSN76489S_SCCI");
        this.rbSN76489S_SCCI.setText("Real");
        //
        // rbSN76489S_Silent
        //
        this.rbSN76489S_Silent.setName("rbSN76489S_Silent");
        this.rbSN76489S_Silent.setText("Silent");
        //
        // rbSN76489S_Emu2
        //
        this.rbSN76489S_Emu2.setName("rbSN76489S_Emu2");
        this.rbSN76489S_Emu2.setText("Emulation (mame)");
        //
        // rbSN76489S_Emu
        //
        this.rbSN76489S_Emu.setSelected(true);
        this.rbSN76489S_Emu.setName("rbSN76489S_Emu");
        this.rbSN76489S_Emu.setText("Emulation (maxim)");
        //
        // groupBox13
        //
        this.groupBox13.setName("groupBox13");
        this.groupBox13.setToolTipText("Ym2612Inst(Use SCCI module Only!)");
        //
        // groupBox14
        //
        this.groupBox14.setName("groupBox14");
        this.groupBox14.setToolTipText("C140Inst(Secondary)");
        //
        // cmbC140S_SCCI
        //
        this.cmbC140S_SCCI.setName("cmbC140S_SCCI");
        //
        // rbC140S_SCCI
        //
        this.rbC140S_SCCI.setName("rbC140S_SCCI");
        this.rbC140S_SCCI.setText("Real");
        //
        // rbC140S_Silent
        //
        this.rbC140S_Silent.setName("rbC140S_Silent");
        this.rbC140S_Silent.setText("Silent");
        //
        // rbC140S_Emu
        //
        this.rbC140S_Emu.setSelected(true);
        this.rbC140S_Emu.setName("rbC140S_Emu");
        this.rbC140S_Emu.setText("Emulation");
        //
        // groupBox15
        //
        this.groupBox15.setName("groupBox15");
        this.groupBox15.setToolTipText("C140Inst(Primary)");
        //
        // cmbC140P_SCCI
        //
        this.cmbC140P_SCCI.setName("cmbC140P_SCCI");
        //
        // rbC140P_Real
        //
        this.rbC140P_Real.setName("rbC140P_Real");
        this.rbC140P_Real.setText("Real");
        //
        // rbC140P_Silent
        //
        this.rbC140P_Silent.setName("rbC140P_Silent");
        this.rbC140P_Silent.setText("Silent");
        //
        // rbC140P_Emu
        //
        this.rbC140P_Emu.setSelected(true);
        this.rbC140P_Emu.setName("rbC140P_Emu");
        this.rbC140P_Emu.setText("Emulation");
        //
        // groupBox16
        //
        this.groupBox16.setName("groupBox16");
        this.groupBox16.setToolTipText("SEGAPCM(Secondary)");
        //
        // cmbSEGAPCMS_SCCI
        //
        this.cmbSEGAPCMS_SCCI.setName("cmbSEGAPCMS_SCCI");
        //
        // rbSEGAPCMS_SCCI
        //
        this.rbSEGAPCMS_SCCI.setName("rbSEGAPCMS_SCCI");
        this.rbSEGAPCMS_SCCI.setText("Real");
        //
        // rbSEGAPCMS_Silent
        //
        this.rbSEGAPCMS_Silent.setName("rbSEGAPCMS_Silent");
        this.rbSEGAPCMS_Silent.setText("Silent");
        //
        // rbSEGAPCMS_Emu
        //
        this.rbSEGAPCMS_Emu.setSelected(true);
        this.rbSEGAPCMS_Emu.setName("rbSEGAPCMS_Emu");
        this.rbSEGAPCMS_Emu.setText("Emulation");
        //
        // groupBox17
        //
        this.groupBox17.setName("groupBox17");
        this.groupBox17.setToolTipText("SEGAPCM(Primary)");
        //
        // cmbSEGAPCMP_SCCI
        //
        this.cmbSEGAPCMP_SCCI.setName("cmbSEGAPCMP_SCCI");
        //
        // rbSEGAPCMP_SCCI
        //
        this.rbSEGAPCMP_SCCI.setName("rbSEGAPCMP_SCCI");
        this.rbSEGAPCMP_SCCI.setText("Real");
        //
        // rbSEGAPCMP_Silent
        //
        this.rbSEGAPCMP_Silent.setName("rbSEGAPCMP_Silent");
        this.rbSEGAPCMP_Silent.setText("Silent");
        //
        // rbSEGAPCMP_Emu
        //
        this.rbSEGAPCMP_Emu.setSelected(true);
        this.rbSEGAPCMP_Emu.setName("rbSEGAPCMP_Emu");
        this.rbSEGAPCMP_Emu.setText("Emulation");
        //
        // groupBox18
        //
        this.groupBox18.setName("groupBox18");
        this.groupBox18.setToolTipText("YMF262(Primary)");
        //
        // cmbYMF262P_SCCI
        //
        this.cmbYMF262P_SCCI.setName("cmbYMF262P_SCCI");
        //
        // rbYMF262P_SCCI
        //
        this.rbYMF262P_SCCI.setName("rbYMF262P_SCCI");
        this.rbYMF262P_SCCI.setText("Real");
        //
        // rbYMF262P_Silent
        //
        this.rbYMF262P_Silent.setName("rbYMF262P_Silent");
        this.rbYMF262P_Silent.setText("Silent");
        //
        // rbYMF262P_Emu
        //
        this.rbYMF262P_Emu.setSelected(true);
        this.rbYMF262P_Emu.setName("rbYMF262P_Emu");
        this.rbYMF262P_Emu.setText("Emulation");
        //
        // groupBox19
        //
        this.groupBox19.setName("groupBox19");
        this.groupBox19.setToolTipText("YMF262(Secondary)");
        //
        // cmbYMF262S_SCCI
        //
        this.cmbYMF262S_SCCI.setName("cmbYMF262S_SCCI");
        //
        // rbYMF262S_SCCI
        //
        this.rbYMF262S_SCCI.setName("rbYMF262S_SCCI");
        this.rbYMF262S_SCCI.setText("Real");
        //
        // rbYMF262S_Silent
        //
        this.rbYMF262S_Silent.setName("rbYMF262S_Silent");
        this.rbYMF262S_Silent.setText("Silent");
        //
        // rbYMF262S_Emu
        //
        this.rbYMF262S_Emu.setSelected(true);
        this.rbYMF262S_Emu.setName("rbYMF262S_Emu");
        this.rbYMF262S_Emu.setText("Emulation");
        //
        // groupBox20
        //
        this.groupBox20.setName("groupBox20");
        this.groupBox20.setToolTipText("YM3812(Primary)");
        //
        // cmbYM3812P_SCCI
        //
        this.cmbYM3812P_SCCI.setName("cmbYM3812P_SCCI");
        //
        // rbYM3812P_SCCI
        //
        this.rbYM3812P_SCCI.setName("rbYM3812P_SCCI");
        this.rbYM3812P_SCCI.setText("Real");
        //
        // rbYM3812P_Silent
        //
        this.rbYM3812P_Silent.setName("rbYM3812P_Silent");
        this.rbYM3812P_Silent.setText("Silent");
        //
        // rbYM3812P_Emu
        //
        this.rbYM3812P_Emu.setSelected(true);
        this.rbYM3812P_Emu.setName("rbYM3812P_Emu");
        this.rbYM3812P_Emu.setText("Emulation");
        //
        // groupBox21
        //
        this.groupBox21.setName("groupBox21");
        this.groupBox21.setToolTipText("YM3812(Secondary)");
        //
        // cmbYM3812S_SCCI
        //
        this.cmbYM3812S_SCCI.setName("cmbYM3812S_SCCI");
        //
        // rbYM3812S_SCCI
        //
        this.rbYM3812S_SCCI.setName("rbYM3812S_SCCI");
        this.rbYM3812S_SCCI.setText("Real");
        //
        // rbYM3812S_Silent
        //
        this.rbYM3812S_Silent.setName("rbYM3812S_Silent");
        this.rbYM3812S_Silent.setText("Silent");
        //
        // rbYM3812S_Emu
        //
        this.rbYM3812S_Emu.setSelected(true);
        this.rbYM3812S_Emu.setName("rbYM3812S_Emu");
        this.rbYM3812S_Emu.setText("Emulation");
        //
        // groupBox22
        //
        this.groupBox22.setName("groupBox22");
        this.groupBox22.setToolTipText("YM3526(Secondary)");
        //
        // cmbYM3526S_SCCI
        //
        this.cmbYM3526S_SCCI.setName("cmbYM3526S_SCCI");
        //
        // rbYM3526S_SCCI
        //
        this.rbYM3526S_SCCI.setName("rbYM3526S_SCCI");
        this.rbYM3526S_SCCI.setText("Real");
        //
        // rbYM3526S_Silent
        //
        this.rbYM3526S_Silent.setName("rbYM3526S_Silent");
        this.rbYM3526S_Silent.setText("Silent");
        //
        // rbYM3526S_Emu
        //
        this.rbYM3526S_Emu.setSelected(true);
        this.rbYM3526S_Emu.setName("rbYM3526S_Emu");
        this.rbYM3526S_Emu.setText("Emulation");
        //
        // groupBox23
        //
        this.groupBox23.setName("groupBox23");
        this.groupBox23.setToolTipText("YM3526(Primary)");
        //
        // cmbYM3526P_SCCI
        //
        this.cmbYM3526P_SCCI.setName("cmbYM3526P_SCCI");
        //
        // rbYM3526P_SCCI
        //
        this.rbYM3526P_SCCI.setName("rbYM3526P_SCCI");
        this.rbYM3526P_SCCI.setText("Real");
        //
        // rbYM3526P_Silent
        //
        this.rbYM3526P_Silent.setName("rbYM3526P_Silent");
        this.rbYM3526P_Silent.setText("Silent");
        //
        // rbYM3526P_Emu
        //
        this.rbYM3526P_Emu.setSelected(true);
        this.rbYM3526P_Emu.setName("rbYM3526P_Emu");
        this.rbYM3526P_Emu.setText("Emulation");
        //
        // groupBox24
        //
        this.groupBox24.setName("groupBox24");
        this.groupBox24.setToolTipText("YM2413(Secondary)");
        //
        // cmbYM2413S_Real
        //
        this.cmbYM2413S_Real.setName("cmbYM2413S_Real");
        //
        // rbYM2413S_Real
        //
        this.rbYM2413S_Real.setName("rbYM2413S_Real");
        this.rbYM2413S_Real.setText("Real");
        //
        // rbYM2413S_Silent
        //
        this.rbYM2413S_Silent.setName("rbYM2413S_Silent");
        this.rbYM2413S_Silent.setText("Silent");
        //
        // rbYM2413S_Emu
        //
        this.rbYM2413S_Emu.setSelected(true);
        this.rbYM2413S_Emu.setName("rbYM2413S_Emu");
        this.rbYM2413S_Emu.setText("Emulation");
        //
        // groupBox25
        //
        this.groupBox25.setName("groupBox25");
        this.groupBox25.setToolTipText("YM2413(Primary)");
        //
        // cmbYM2413P_Real
        //
        this.cmbYM2413P_Real.setName("cmbYM2413P_Real");
        //
        // rbYM2413P_Real
        //
        this.rbYM2413P_Real.setName("rbYM2413P_Real");
        this.rbYM2413P_Real.setText("Real");
        //
        // rbYM2413P_Silent
        //
        this.rbYM2413P_Silent.setName("rbYM2413P_Silent");
        this.rbYM2413P_Silent.setText("Silent");
        //
        // rbYM2413P_Emu
        //
        this.rbYM2413P_Emu.setSelected(true);
        this.rbYM2413P_Emu.setName("rbYM2413P_Emu");
        this.rbYM2413P_Emu.setText("Emulation");
        //
        // groupBox26
        //
        this.groupBox26.setName("groupBox26");
        this.groupBox26.setToolTipText("AY-3-8910(Primary)");
        //
        // cmbAY8910P_Real
        //
        this.cmbAY8910P_Real.setName("cmbAY8910P_Real");
        //
        // rbAY8910P_Real
        //
        this.rbAY8910P_Real.setName("rbAY8910P_Real");
        this.rbAY8910P_Real.setText("Real");
        //
        // rbAY8910P_Silent
        //
        this.rbAY8910P_Silent.setName("rbAY8910P_Silent");
        this.rbAY8910P_Silent.setText("Silent");
        //
        // rbAY8910P_Emu
        //
        this.rbAY8910P_Emu.setSelected(true);
        this.rbAY8910P_Emu.setName("rbAY8910P_Emu");
        this.rbAY8910P_Emu.setText("Emulation(fmgen)");
        //
        // groupBox27
        //
        this.groupBox27.setName("groupBox27");
        this.groupBox27.setToolTipText("AY-3-8910(Secondary)");
        //
        // cmbAY8910S_Real
        //
        this.cmbAY8910S_Real.setName("cmbAY8910S_Real");
        //
        // rbAY8910S_Real
        //
        this.rbAY8910S_Real.setName("rbAY8910S_Real");
        this.rbAY8910S_Real.setText("Real");
        //
        // rbAY8910S_Silent
        //
        this.rbAY8910S_Silent.setName("rbAY8910S_Silent");
        this.rbAY8910S_Silent.setText("Silent");
        //
        // rbAY8910S_Emu
        //
        this.rbAY8910S_Emu.setSelected(true);
        this.rbAY8910S_Emu.setName("rbAY8910S_Emu");
        this.rbAY8910S_Emu.setText("Emulation(fmgen)");
        //
        // groupBox28
        //
        this.groupBox28.setName("groupBox28");
        this.groupBox28.setToolTipText("K051649Inst(Secondary)");
        //
        // cmbK051649S_Real
        //
        this.cmbK051649S_Real.setName("cmbK051649S_Real");
        //
        // rbK051649S_Real
        //
        this.rbK051649S_Real.setName("rbK051649S_Real");
        this.rbK051649S_Real.setText("Real");
        //
        // rbK051649S_Silent
        //
        this.rbK051649S_Silent.setName("rbK051649S_Silent");
        this.rbK051649S_Silent.setText("Silent");
        //
        // rbK051649S_Emu
        //
        this.rbK051649S_Emu.setSelected(true);
        this.rbK051649S_Emu.setName("rbK051649S_Emu");
        this.rbK051649S_Emu.setText("Emulation");
        //
        // groupBox29
        //
        this.groupBox29.setName("groupBox29");
        this.groupBox29.setToolTipText("K051649Inst(Primary)");
        //
        // cmbK051649P_Real
        //
        this.cmbK051649P_Real.setName("cmbK051649P_Real");
        //
        // rbK051649P_Real
        //
        this.rbK051649P_Real.setName("rbK051649P_Real");
        this.rbK051649P_Real.setText("Real");
        //
        // rbK051649P_Silent
        //
        this.rbK051649P_Silent.setName("rbK051649P_Silent");
        this.rbK051649P_Silent.setText("Silent");
        //
        // rbK051649P_Emu
        //
        this.rbK051649P_Emu.setSelected(true);
        this.rbK051649P_Emu.setName("rbK051649P_Emu");
        this.rbK051649P_Emu.setText("Emulation");
        //
        // rbAY8910P_Emu2
        //
        this.rbAY8910P_Emu2.setName("rbAY8910P_Emu2");
        this.rbAY8910P_Emu2.setText("Emulation(mame)");
        //
        // rbAY8910S_Emu2
        //
        this.rbAY8910S_Emu2.setName("rbAY8910S_Emu2");
        this.rbAY8910S_Emu2.setText("Emulation(mame)");
        //
        // ucSettingInstruments
        //
        this.setName("ucSettingInstruments");
        // layout
        chip(this.groupBox1, this.rbYM2151P_SCCI, this.cmbYM2151P_SCCI, this.rbYM2151P_Silent, this.rbYM2151P_Emu, this.rbYM2151P_EmuMame, this.rbYM2151P_EmuX68Sound).end();
        chip(this.groupBox2, this.rbYM2151S_SCCI, this.cmbYM2151S_SCCI, this.rbYM2151S_Silent, this.rbYM2151S_Emu, this.rbYM2151S_EmuMame, this.rbYM2151S_EmuX68Sound).end();
        chip(this.groupBox3, this.rbYM2203P_SCCI, this.cmbYM2203P_SCCI, this.rbYM2203P_Silent, this.rbYM2203P_Emu).end();
        chip(this.groupBox4, this.rbYM2203S_SCCI, this.cmbYM2203S_SCCI, this.rbYM2203S_Silent, this.rbYM2203S_Emu).end();
        chip(this.groupBox25, this.rbYM2413P_Real, this.cmbYM2413P_Real, this.rbYM2413P_Silent, this.rbYM2413P_Emu).end();
        chip(this.groupBox24, this.rbYM2413S_Real, this.cmbYM2413S_Real, this.rbYM2413S_Silent, this.rbYM2413S_Emu).end();
        chip(this.groupBox5, this.rbYM2608P_SCCI, this.cmbYM2608P_SCCI, this.rbYM2608P_Silent, this.rbYM2608P_Emu).end();
        chip(this.groupBox6, this.rbYM2608S_SCCI, this.cmbYM2608S_SCCI, this.rbYM2608S_Silent, this.rbYM2608S_Emu).end();
        chip(this.groupBox7, this.rbYM2610BP_SCCI, this.cmbYM2610BP_SCCI, this.rbYM2610BP_Silent, this.rbYM2610BP_Emu).next()
                .span(Grid.panel().add(this.rbYM2610BEP_SCCI).fill(this.cmbYM2610BEP_SCCI).add(this.label1).fill(this.cmbSPPCMP_SCCI).build()).end();
        chip(this.groupBox8, this.rbYM2610BS_SCCI, this.cmbYM2610BS_SCCI, this.rbYM2610BS_Silent, this.rbYM2610BS_Emu).next()
                .span(Grid.panel().add(this.rbYM2610BES_SCCI).fill(this.cmbYM2610BES_SCCI).add(this.label2).fill(this.cmbSPPCMS_SCCI).build()).end();
        chip(this.groupBox9, this.rbYM2612P_SCCI, this.cmbYM2612P_SCCI, this.rbYM2612P_Silent, this.rbYM2612P_EmuNuked, this.rbYM2612P_Emu, this.rbYM2612P_EmuMame).end();
        chip(this.groupBox10, this.rbYM2612S_SCCI, this.cmbYM2612S_SCCI, this.rbYM2612S_Silent, this.rbYM2612S_EmuNuked, this.rbYM2612S_Emu, this.rbYM2612S_EmuMame).end();
        new Grid(Grid.group(this.groupBox13, this.groupBox13.getToolTipText()))
                .add(this.cbSendWait).add(this.cbEmulationPCMOnly).next()
                .add(this.cbTwice).end();
        chip(this.groupBox23, this.rbYM3526P_SCCI, this.cmbYM3526P_SCCI, this.rbYM3526P_Silent, this.rbYM3526P_Emu).end();
        chip(this.groupBox22, this.rbYM3526S_SCCI, this.cmbYM3526S_SCCI, this.rbYM3526S_Silent, this.rbYM3526S_Emu).end();
        chip(this.groupBox20, this.rbYM3812P_SCCI, this.cmbYM3812P_SCCI, this.rbYM3812P_Silent, this.rbYM3812P_Emu).end();
        chip(this.groupBox21, this.rbYM3812S_SCCI, this.cmbYM3812S_SCCI, this.rbYM3812S_Silent, this.rbYM3812S_Emu).end();
        chip(this.groupBox18, this.rbYMF262P_SCCI, this.cmbYMF262P_SCCI, this.rbYMF262P_Silent, this.rbYMF262P_Emu).end();
        chip(this.groupBox19, this.rbYMF262S_SCCI, this.cmbYMF262S_SCCI, this.rbYMF262S_Silent, this.rbYMF262S_Emu).end();
        chip(this.groupBox11, this.rbSN76489P_SCCI, this.cmbSN76489P_SCCI, this.rbSN76489P_Silent, this.rbSN76489P_Emu, this.rbSN76489P_Emu2).end();
        chip(this.groupBox12, this.rbSN76489S_SCCI, this.cmbSN76489S_SCCI, this.rbSN76489S_Silent, this.rbSN76489S_Emu, this.rbSN76489S_Emu2).end();
        chip(this.groupBox15, this.rbC140P_Real, this.cmbC140P_SCCI, this.rbC140P_Silent, this.rbC140P_Emu).end();
        chip(this.groupBox14, this.rbC140S_SCCI, this.cmbC140S_SCCI, this.rbC140S_Silent, this.rbC140S_Emu).end();
        chip(this.groupBox17, this.rbSEGAPCMP_SCCI, this.cmbSEGAPCMP_SCCI, this.rbSEGAPCMP_Silent, this.rbSEGAPCMP_Emu).end();
        chip(this.groupBox16, this.rbSEGAPCMS_SCCI, this.cmbSEGAPCMS_SCCI, this.rbSEGAPCMS_Silent, this.rbSEGAPCMS_Emu).end();
        chip(this.groupBox26, this.rbAY8910P_Real, this.cmbAY8910P_Real, this.rbAY8910P_Silent, this.rbAY8910P_Emu, this.rbAY8910P_Emu2).end();
        chip(this.groupBox27, this.rbAY8910S_Real, this.cmbAY8910S_Real, this.rbAY8910S_Silent, this.rbAY8910S_Emu, this.rbAY8910S_Emu2).end();
        chip(this.groupBox29, this.rbK051649P_Real, this.cmbK051649P_Real, this.rbK051649P_Silent, this.rbK051649P_Emu).end();
        chip(this.groupBox28, this.rbK051649S_Real, this.cmbK051649S_Real, this.rbK051649S_Silent, this.rbK051649S_Emu).end();
        Grid grid = new Grid(this);
        for (JPanel chip : new JPanel[] {
                this.groupBox1, this.groupBox2, this.groupBox3, this.groupBox4, this.groupBox25, this.groupBox24,
                this.groupBox5, this.groupBox6, this.groupBox7, this.groupBox8, this.groupBox9, this.groupBox10,
                this.groupBox13, this.groupBox23, this.groupBox22, this.groupBox20, this.groupBox21, this.groupBox18,
                this.groupBox19, this.groupBox11, this.groupBox12, this.groupBox15, this.groupBox14, this.groupBox17,
                this.groupBox16, this.groupBox26, this.groupBox27, this.groupBox29, this.groupBox28
        }) {
            grid.span(chip).next();
        }
        grid.end();
    }

    /** a chip's box, captioned by its tool tip: how it is played on the first row, the real chip on the second */
    private static Grid chip(JPanel group, JCheckBox real, JComboBox<String> device, JCheckBox... emulations) {
        Grid.group(group, group.getToolTipText());
        return new Grid(group)
                .span(Grid.row(emulations)).next()
                .add(real).fill(device);
    }

    private JPanel groupBox1;
    private JPanel groupBox2;
    private JPanel groupBox3;
    private JPanel groupBox4;
    private JPanel groupBox5;
    private JPanel groupBox6;
    private JPanel groupBox7;
    private JPanel groupBox8;
    private JPanel groupBox9;
    private JPanel groupBox10;
    private JPanel groupBox11;
    private JPanel groupBox12;
    public JComboBox<String> cmbYM2151P_SCCI;
    public JCheckBox rbYM2151P_SCCI;
    public JCheckBox rbYM2151P_Silent;
    public JCheckBox rbYM2151P_Emu;
    public JComboBox<String> cmbYM2151S_SCCI;
    public JCheckBox rbYM2151S_SCCI;
    public JCheckBox rbYM2151S_Silent;
    public JCheckBox rbYM2151S_Emu;
    public JComboBox<String> cmbYM2203P_SCCI;
    public JCheckBox rbYM2203P_SCCI;
    public JCheckBox rbYM2203P_Emu;
    public JCheckBox rbYM2203P_Silent;
    public JComboBox<String> cmbYM2203S_SCCI;
    public JCheckBox rbYM2203S_SCCI;
    public JCheckBox rbYM2203S_Silent;
    public JCheckBox rbYM2203S_Emu;
    public JComboBox<String> cmbYM2608P_SCCI;
    public JCheckBox rbYM2608P_SCCI;
    public JCheckBox rbYM2608P_Emu;
    public JCheckBox rbYM2608P_Silent;
    public JComboBox<String> cmbYM2608S_SCCI;
    public JCheckBox rbYM2608S_SCCI;
    public JCheckBox rbYM2608S_Silent;
    public JCheckBox rbYM2608S_Emu;
    public JComboBox<String> cmbYM2610BP_SCCI;
    public JCheckBox rbYM2610BP_SCCI;
    public JCheckBox rbYM2610BP_Emu;
    public JCheckBox rbYM2610BP_Silent;
    public JComboBox<String> cmbYM2610BS_SCCI;
    public JCheckBox rbYM2610BS_SCCI;
    public JCheckBox rbYM2610BS_Silent;
    public JCheckBox rbYM2610BS_Emu;
    public JCheckBox cbEmulationPCMOnly;
    public JCheckBox cbTwice;
    public JCheckBox cbSendWait;
    public JComboBox<String> cmbYM2612P_SCCI;
    public JCheckBox rbYM2612P_SCCI;
    public JCheckBox rbYM2612P_Emu;
    public JCheckBox rbYM2612P_Silent;
    public JCheckBox rbYM2612S_Silent;
    public JComboBox<String> cmbYM2612S_SCCI;
    public JCheckBox rbYM2612S_SCCI;
    public JCheckBox rbYM2612S_Emu;
    public JComboBox<String> cmbSN76489P_SCCI;
    public JCheckBox rbSN76489P_SCCI;
    public JCheckBox rbSN76489P_Emu;
    public JCheckBox rbSN76489P_Silent;
    public JComboBox<String> cmbSN76489S_SCCI;
    public JCheckBox rbSN76489S_SCCI;
    public JCheckBox rbSN76489S_Silent;
    public JCheckBox rbSN76489S_Emu;
    private JPanel groupBox13;
    private JPanel groupBox14;
    public JComboBox<String> cmbC140S_SCCI;
    public JCheckBox rbC140S_SCCI;
    public JCheckBox rbC140S_Silent;
    public JCheckBox rbC140S_Emu;
    private JPanel groupBox15;
    public JComboBox<String> cmbC140P_SCCI;
    public JCheckBox rbC140P_Real;
    public JCheckBox rbC140P_Silent;
    public JCheckBox rbC140P_Emu;
    private JPanel groupBox16;
    public JComboBox<String> cmbSEGAPCMS_SCCI;
    public JCheckBox rbSEGAPCMS_SCCI;
    public JCheckBox rbSEGAPCMS_Silent;
    public JCheckBox rbSEGAPCMS_Emu;
    private JPanel groupBox17;
    public JComboBox<String> cmbSEGAPCMP_SCCI;
    public JCheckBox rbSEGAPCMP_SCCI;
    public JCheckBox rbSEGAPCMP_Silent;
    public JCheckBox rbSEGAPCMP_Emu;
    private JLabel label1;
    public JComboBox<String> cmbYM2610BEP_SCCI;
    public JComboBox<String> cmbSPPCMP_SCCI;
    public JCheckBox rbYM2610BEP_SCCI;
    private JLabel label2;
    public JComboBox<String> cmbYM2610BES_SCCI;
    public JComboBox<String> cmbSPPCMS_SCCI;
    public JCheckBox rbYM2610BES_SCCI;
    public JCheckBox rbYM2151P_EmuMame;
    public JCheckBox rbYM2151S_EmuMame;
    public JCheckBox rbYM2151P_EmuX68Sound;
    public JCheckBox rbYM2151S_EmuX68Sound;
    public JCheckBox rbYM2612P_EmuNuked;
    public JCheckBox rbYM2612S_EmuNuked;
    private JPanel groupBox18;
    public JComboBox<String> cmbYMF262P_SCCI;
    public JCheckBox rbYMF262P_SCCI;
    public JCheckBox rbYMF262P_Silent;
    public JCheckBox rbYMF262P_Emu;
    private JPanel groupBox19;
    public JComboBox<String> cmbYMF262S_SCCI;
    public JCheckBox rbYMF262S_SCCI;
    public JCheckBox rbYMF262S_Silent;
    public JCheckBox rbYMF262S_Emu;
    private JPanel groupBox20;
    public JComboBox<String> cmbYM3812P_SCCI;
    public JCheckBox rbYM3812P_SCCI;
    public JCheckBox rbYM3812P_Silent;
    public JCheckBox rbYM3812P_Emu;
    private JPanel groupBox21;
    public JComboBox<String> cmbYM3812S_SCCI;
    public JCheckBox rbYM3812S_SCCI;
    public JCheckBox rbYM3812S_Silent;
    public JCheckBox rbYM3812S_Emu;
    private JPanel groupBox22;
    public JComboBox<String> cmbYM3526S_SCCI;
    public JCheckBox rbYM3526S_SCCI;
    public JCheckBox rbYM3526S_Silent;
    public JCheckBox rbYM3526S_Emu;
    private JPanel groupBox23;
    public JComboBox<String> cmbYM3526P_SCCI;
    public JCheckBox rbYM3526P_SCCI;
    public JCheckBox rbYM3526P_Silent;
    public JCheckBox rbYM3526P_Emu;
    public JCheckBox rbYM2612P_EmuMame;
    public JCheckBox rbYM2612S_EmuMame;
    private JPanel groupBox24;
    public JComboBox<String> cmbYM2413S_Real;
    public JCheckBox rbYM2413S_Real;
    public JCheckBox rbYM2413S_Silent;
    public JCheckBox rbYM2413S_Emu;
    private JPanel groupBox25;
    public JComboBox<String> cmbYM2413P_Real;
    public JCheckBox rbYM2413P_Real;
    public JCheckBox rbYM2413P_Silent;
    public JCheckBox rbYM2413P_Emu;
    public JCheckBox rbSN76489P_Emu2;
    public JCheckBox rbSN76489S_Emu2;
    private JPanel groupBox26;
    public JComboBox<String> cmbAY8910P_Real;
    public JCheckBox rbAY8910P_Real;
    public JCheckBox rbAY8910P_Silent;
    public JCheckBox rbAY8910P_Emu;
    private JPanel groupBox27;
    public JComboBox<String> cmbAY8910S_Real;
    public JCheckBox rbAY8910S_Real;
    public JCheckBox rbAY8910S_Silent;
    public JCheckBox rbAY8910S_Emu;
    private JPanel groupBox28;
    public JComboBox<String> cmbK051649S_Real;
    public JCheckBox rbK051649S_Real;
    public JCheckBox rbK051649S_Silent;
    public JCheckBox rbK051649S_Emu;
    private JPanel groupBox29;
    public JComboBox<String> cmbK051649P_Real;
    public JCheckBox rbK051649P_Real;
    public JCheckBox rbK051649P_Silent;
    public JCheckBox rbK051649P_Emu;
    public JCheckBox rbAY8910P_Emu2;
    public JCheckBox rbAY8910S_Emu2;
}
