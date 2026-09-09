package mdplayer.lib.gbs;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;


public class Cpu {

    private static final Logger logger = System.getLogger(Cpu.class.getName());

    public final Registers reg = new Registers();
    private final Memory mem;
    public final int clock;
    public boolean isHalt = false;
    public boolean isStop = false;
    private String nimo = "";
    private boolean cbSwitch = false;
    private int ime = 1;

    public Cpu(int clock, Memory memory) {
        this.clock = clock;
        this.mem = memory;
        initInsts();
    }

    public void init() {
        reg.pc = 0x100;
        isHalt = false;
        isStop = false;
        cbSwitch = false;
        ime = 1;
    }

    public int executeOneStep() {
        if (isHalt || isStop) {
            return 4;
        }

        int pc;
        int cycle;

        do {
            pc = reg.pc;

            // Read instruction
            int c = mem.peekB(reg.pc) & 0xFF;
            reg.pc++;
            reg.pc &= 0xFFFF;
            if (cbSwitch) {
                c += 0x100;
                cbSwitch = false;
            }
            // Execute instruction
            cycle = insts[c].meth.get();

            StringBuilder smem = new StringBuilder();
            int len = Math.min(insts[c].length, 4);
            for (int i = 0; i < len; i++)
                smem.append("%02X ".formatted(mem.peekB((pc + i) & 0xFFFF) & 0xFF));
            // Pad with spaces - "            ".substring(len * 3) equivalent
            int padding = 12 - (len * 3);
            if (padding > 0) {
                smem.repeat(" ", padding);
            }
            logger.log(Level.TRACE, "$%04X : %-20s %s %s %s cycle:%d".formatted(
                    pc & 0xffff, nimo, smem.toString(), reg.toString(), mem.getBank(), cycle));
            nimo = "";

            if (cycle == 0) throw new UnsupportedOperationException();
        } while (cbSwitch);

        return cycle;
    }

    private Inst[] insts;

    // Reference:
    // https://www.pastraiser.com/cpu/gameboy/gameboy_opcodes.html

    private void initInsts() {
        insts = new Inst[] {
                // 0x00
                new Inst(this::NOP, 1, "4    ", "----"), new Inst(this::LD_BC_d16, 3, "12   ", "----"),
                new Inst(this::LD_pBCs_A, 1, "8    ", "----"), new Inst(this::INC_BC, 1, "8    ", "----"),
                new Inst(this::INC_B, 1, "4    ", "Z0H-"), new Inst(this::DEC_B, 1, "4    ", "Z1H-"),
                new Inst(this::LD_B_d8, 2, "8    ", "----"), new Inst(this::RLCA, 1, "4    ", "000C"),
                // 0x08
                new Inst(this::LD_pa16s_SP, 3, "20   ", "----"), new Inst(this::ADD_HL_BC, 1, "8    ", "-0HC"),
                new Inst(this::LD_A_pBCs, 1, "8    ", "----"), new Inst(this::DEC_BC, 1, "8    ", "----"),
                new Inst(this::INC_C, 1, "4    ", "Z0H-"), new Inst(this::DEC_C, 1, "4    ", "Z1H-"),
                new Inst(this::LD_C_d8, 2, "8    ", "----"), new Inst(this::RRCA, 1, "4    ", "000C"),
                // 0x10
                new Inst(this::STOP_0, 2, "4    ", "----"), new Inst(this::LD_DE_d16, 3, "12   ", "----"),
                new Inst(this::LD_pDEs_A, 1, "8    ", "----"), new Inst(this::INC_DE, 1, "8    ", "----"),
                new Inst(this::INC_D, 1, "4    ", "Z0H-"), new Inst(this::DEC_D, 1, "4    ", "Z1H-"),
                new Inst(this::LD_D_d8, 2, "8    ", "----"), new Inst(this::RLA, 1, "4    ", "000C"),
                // 0x18
                new Inst(this::JR_r8, 2, "12   ", "----"), new Inst(this::ADD_HL_DE, 1, "8    ", "-0HC"),
                new Inst(this::LD_A_pDEs, 1, "8    ", "----"), new Inst(this::DEC_DE, 1, "8    ", "----"),
                new Inst(this::INC_E, 1, "4    ", "Z0H-"), new Inst(this::DEC_E, 1, "4    ", "Z1H-"),
                new Inst(this::LD_E_d8, 2, "8    ", "----"), new Inst(this::RRA, 1, "4    ", "000C"),
                // 0x20
                new Inst(this::JR_NZ_r8, 2, "12/8 ", "----"), new Inst(this::LD_HL_d16, 3, "12   ", "----"),
                new Inst(this::LD_pHLplss_A, 1, "8    ", "----"), new Inst(this::INC_HL, 1, "8    ", "----"),
                new Inst(this::INC_H, 1, "4    ", "Z0H-"), new Inst(this::DEC_H, 1, "4    ", "Z1H-"),
                new Inst(this::LD_H_d8, 2, "8    ", "----"), new Inst(this::DAA, 1, "4    ", "Z-0C"),
                // 0x28
                new Inst(this::JR_Z_r8, 2, "12/8 ", "----"), new Inst(this::ADD_HL_HL, 1, "8    ", "-0HC"),
                new Inst(this::LD_A_pHLplss, 1, "8    ", "----"), new Inst(this::DEC_HL, 1, "8    ", "----"),
                new Inst(this::INC_L, 1, "4    ", "Z0H-"), new Inst(this::DEC_L, 1, "4    ", "Z1H-"),
                new Inst(this::LD_L_d8, 2, "8    ", "----"), new Inst(this::CPL, 1, "4    ", "-11-"),
                // 0x30
                new Inst(this::JR_NC_r8, 2, "12/8 ", "----"), new Inst(this::LD_SP_d16, 3, "12   ", "----"),
                new Inst(this::LD_pHLmiss_A, 1, "8    ", "----"), new Inst(this::INC_SP, 1, "8    ", "----"),
                new Inst(this::INC_pHLs, 1, "12   ", "Z0H-"), new Inst(this::DEC_pHLs, 1, "12   ", "Z1H-"),
                new Inst(this::LD_pHLs_d8, 2, "12   ", "----"), new Inst(this::SCF, 1, "4    ", "-001"),
                // 0x38
                new Inst(this::JR_C_r8, 2, "12/8 ", "----"), new Inst(this::ADD_HL_SP, 1, "8    ", "-0HC"),
                new Inst(this::LD_A_pHLmiss, 1, "8    ", "----"), new Inst(this::DEC_SP, 1, "8    ", "----"),
                new Inst(this::INC_A, 1, "4    ", "Z0H-"), new Inst(this::DEC_A, 1, "4    ", "Z1H-"),
                new Inst(this::LD_A_d8, 2, "8    ", "----"), new Inst(this::CCF, 1, "4    ", "-00C"),
                // 0x40
                new Inst(this::LD_B_B, 1, "4    ", "----"), new Inst(this::LD_B_C, 1, "4    ", "----"),
                new Inst(this::LD_B_D, 1, "4    ", "----"), new Inst(this::LD_B_E, 1, "4    ", "----"),
                new Inst(this::LD_B_H, 1, "4    ", "----"), new Inst(this::LD_B_L, 1, "4    ", "----"),
                new Inst(this::LD_B_pHLs, 1, "8    ", "----"), new Inst(this::LD_B_A, 1, "4    ", "----"),
                // 0x48
                new Inst(this::LD_C_B, 1, "4    ", "----"), new Inst(this::LD_C_C, 1, "4    ", "----"),
                new Inst(this::LD_C_D, 1, "4    ", "----"), new Inst(this::LD_C_E, 1, "4    ", "----"),
                new Inst(this::LD_C_H, 1, "4    ", "----"), new Inst(this::LD_C_L, 1, "4    ", "----"),
                new Inst(this::LD_C_pHLs, 1, "8    ", "----"), new Inst(this::LD_C_A, 1, "4    ", "----"),
                // 0x50
                new Inst(this::LD_D_B, 1, "4    ", "----"), new Inst(this::LD_D_C, 1, "4    ", "----"),
                new Inst(this::LD_D_D, 1, "4    ", "----"), new Inst(this::LD_D_E, 1, "4    ", "----"),
                new Inst(this::LD_D_H, 1, "4    ", "----"), new Inst(this::LD_D_L, 1, "4    ", "----"),
                new Inst(this::LD_D_pHLs, 1, "8    ", "----"), new Inst(this::LD_D_A, 1, "4    ", "----"),
                // 0x58
                new Inst(this::LD_E_B, 1, "4    ", "----"), new Inst(this::LD_E_C, 1, "4    ", "----"),
                new Inst(this::LD_E_D, 1, "4    ", "----"), new Inst(this::LD_E_E, 1, "4    ", "----"),
                new Inst(this::LD_E_H, 1, "4    ", "----"), new Inst(this::LD_E_L, 1, "4    ", "----"),
                new Inst(this::LD_E_pHLs, 1, "8    ", "----"), new Inst(this::LD_E_A, 1, "4    ", "----"),
                // 0x60
                new Inst(this::LD_H_B, 1, "4    ", "----"), new Inst(this::LD_H_C, 1, "4    ", "----"),
                new Inst(this::LD_H_D, 1, "4    ", "----"), new Inst(this::LD_H_E, 1, "4    ", "----"),
                new Inst(this::LD_H_H, 1, "4    ", "----"), new Inst(this::LD_H_L, 1, "4    ", "----"),
                new Inst(this::LD_H_pHLs, 1, "8    ", "----"), new Inst(this::LD_H_A, 1, "4    ", "----"),
                // 0x68
                new Inst(this::LD_L_B, 1, "4    ", "----"), new Inst(this::LD_L_C, 1, "4    ", "----"),
                new Inst(this::LD_L_D, 1, "4    ", "----"), new Inst(this::LD_L_E, 1, "4    ", "----"),
                new Inst(this::LD_L_H, 1, "4    ", "----"), new Inst(this::LD_L_L, 1, "4    ", "----"),
                new Inst(this::LD_L_pHLs, 1, "8    ", "----"), new Inst(this::LD_L_A, 1, "4    ", "----"),
                // 0x70
                new Inst(this::LD_pHLs_B, 1, "8    ", "----"), new Inst(this::LD_pHLs_C, 1, "8    ", "----"),
                new Inst(this::LD_pHLs_D, 1, "8    ", "----"), new Inst(this::LD_pHLs_E, 1, "8    ", "----"),
                new Inst(this::LD_pHLs_H, 1, "8    ", "----"), new Inst(this::LD_pHLs_L, 1, "8    ", "----"),
                new Inst(this::HALT, 1, "4    ", "----"), new Inst(this::LD_pHLs_A, 1, "8    ", "----"),
                // 0x78
                new Inst(this::LD_A_B, 1, "4    ", "----"), new Inst(this::LD_A_C, 1, "4    ", "----"),
                new Inst(this::LD_A_D, 1, "4    ", "----"), new Inst(this::LD_A_E, 1, "4    ", "----"),
                new Inst(this::LD_A_H, 1, "4    ", "----"), new Inst(this::LD_A_L, 1, "4    ", "----"),
                new Inst(this::LD_A_pHLs, 1, "8    ", "----"), new Inst(this::LD_A_A, 1, "4    ", "----"),
                // 0x80
                new Inst(this::ADD_A_B, 1, "4    ", "Z0HC"), new Inst(this::ADD_A_C, 1, "4    ", "Z0HC"),
                new Inst(this::ADD_A_D, 1, "4    ", "Z0HC"), new Inst(this::ADD_A_E, 1, "4    ", "Z0HC"),
                new Inst(this::ADD_A_H, 1, "4    ", "Z0HC"), new Inst(this::ADD_A_L, 1, "4    ", "Z0HC"),
                new Inst(this::ADD_A_pHLs, 1, "8    ", "Z0HC"), new Inst(this::ADD_A_A, 1, "4    ", "Z0HC"),
                // 0x88
                new Inst(this::ADC_A_B, 1, "4    ", "Z0HC"), new Inst(this::ADC_A_C, 1, "4    ", "Z0HC"),
                new Inst(this::ADC_A_D, 1, "4    ", "Z0HC"), new Inst(this::ADC_A_E, 1, "4    ", "Z0HC"),
                new Inst(this::ADC_A_H, 1, "4    ", "Z0HC"), new Inst(this::ADC_A_L, 1, "4    ", "Z0HC"),
                new Inst(this::ADC_A_pHLs, 1, "8    ", "Z0HC"), new Inst(this::ADC_A_A, 1, "4    ", "Z0HC"),
                // 0x90
                new Inst(this::SUB_B, 1, "4    ", "Z1HC"), new Inst(this::SUB_C, 1, "4    ", "Z1HC"),
                new Inst(this::SUB_D, 1, "4    ", "Z1HC"), new Inst(this::SUB_E, 1, "4    ", "Z1HC"),
                new Inst(this::SUB_H, 1, "4    ", "Z1HC"), new Inst(this::SUB_L, 1, "4    ", "Z1HC"),
                new Inst(this::SUB_pHLs, 1, "8    ", "Z1HC"), new Inst(this::SUB_A, 1, "4    ", "Z1HC"),
                // 0x98
                new Inst(this::SBC_A_B, 1, "4    ", "Z1HC"), new Inst(this::SBC_A_C, 1, "4    ", "Z1HC"),
                new Inst(this::SBC_A_D, 1, "4    ", "Z1HC"), new Inst(this::SBC_A_E, 1, "4    ", "Z1HC"),
                new Inst(this::SBC_A_H, 1, "4    ", "Z1HC"), new Inst(this::SBC_A_L, 1, "4    ", "Z1HC"),
                new Inst(this::SBC_A_pHLs, 1, "8    ", "Z1HC"), new Inst(this::SBC_A_A, 1, "4    ", "Z1HC"),
                // 0xa0
                new Inst(this::AND_B, 1, "4    ", "Z010"), new Inst(this::AND_C, 1, "4    ", "Z010"),
                new Inst(this::AND_D, 1, "4    ", "Z010"), new Inst(this::AND_E, 1, "4    ", "Z010"),
                new Inst(this::AND_H, 1, "4    ", "Z010"), new Inst(this::AND_L, 1, "4    ", "Z010"),
                new Inst(this::AND_pHLs, 1, "8    ", "Z010"), new Inst(this::AND_A, 1, "4    ", "Z010"),
                // 0xa8
                new Inst(this::XOR_B, 1, "4    ", "Z000"), new Inst(this::XOR_C, 1, "4    ", "Z000"),
                new Inst(this::XOR_D, 1, "4    ", "Z000"), new Inst(this::XOR_E, 1, "4    ", "Z000"),
                new Inst(this::XOR_H, 1, "4    ", "Z000"), new Inst(this::XOR_L, 1, "4    ", "Z000"),
                new Inst(this::XOR_pHLs, 1, "8    ", "Z000"), new Inst(this::XOR_A, 1, "4    ", "Z000"),
                // 0xb0
                new Inst(this::OR_B, 1, "4    ", "Z000"), new Inst(this::OR_C, 1, "4    ", "Z000"),
                new Inst(this::OR_D, 1, "4    ", "Z000"), new Inst(this::OR_E, 1, "4    ", "Z000"),
                new Inst(this::OR_H, 1, "4    ", "Z000"), new Inst(this::OR_L, 1, "4    ", "Z000"),
                new Inst(this::OR_pHLs, 1, "8    ", "Z000"), new Inst(this::OR_A, 1, "4    ", "Z000"),
                // 0xb8
                new Inst(this::CP_B, 1, "4    ", "Z1HC"), new Inst(this::CP_C, 1, "4    ", "Z1HC"),
                new Inst(this::CP_D, 1, "4    ", "Z1HC"), new Inst(this::CP_E, 1, "4    ", "Z1HC"),
                new Inst(this::CP_H, 1, "4    ", "Z1HC"), new Inst(this::CP_L, 1, "4    ", "Z1HC"),
                new Inst(this::CP_pHLs, 1, "8    ", "Z1HC"), new Inst(this::CP_A, 1, "4    ", "Z1HC"),
                // 0xc0
                new Inst(this::RET_NZ, 1, "20/8 ", "----"), new Inst(this::POP_BC, 1, "12   ", "----"),
                new Inst(this::JP_NZ_a16, 3, "16/12", "----"), new Inst(this::JP_a16, 3, "16   ", "----"),
                new Inst(this::CALL_NZ_a16, 3, "24/12", "----"), new Inst(this::PUSH_BC, 1, "16   ", "----"),
                new Inst(this::ADD_A_d8, 2, "8    ", "Z0HC"), new Inst(this::RST_00H, 1, "16   ", "----"),
                // 0xc8
                new Inst(this::RET_Z, 1, "20/8 ", "----"), new Inst(this::RET, 1, "16   ", "----"),
                new Inst(this::JP_Z_a16, 3, "16/12", "----"), new Inst(this::PREFIX_CB, 1, "4    ", "----"),
                new Inst(this::CALL_Z_a16, 3, "24/12", "----"), new Inst(this::CALL_a16, 3, "24   ", "----"),
                new Inst(this::ADC_A_d8, 2, "8    ", "Z0HC"), new Inst(this::RST_08H, 1, "16   ", "----"),
                // 0xd0
                new Inst(this::RET_NC, 1, "20/8 ", "----"), new Inst(this::POP_DE, 1, "12   ", "----"),
                new Inst(this::JP_NC_a16, 3, "16/12", "----"), null,
                new Inst(this::CALL_NC_a16, 3, "24/12", "----"), new Inst(this::PUSH_DE, 1, "16   ", "----"),
                new Inst(this::SUB_d8, 2, "8    ", "Z1HC"), new Inst(this::RST_10H, 1, "16   ", "----"),
                // 0xd8
                new Inst(this::RET_C, 1, "20/8 ", "----"), new Inst(this::RETI, 1, "16   ", "----"),
                new Inst(this::JP_C_a16, 3, "16/12", "----"), null,
                new Inst(this::CALL_C_a16, 3, "24/12", "----"), null,
                new Inst(this::SBC_A_d8, 2, "8    ", "Z1HC"), new Inst(this::RST_18H, 1, "16   ", "----"),
                // 0xe0
                new Inst(this::LDH_pa8s_A, 2, "12   ", "----"), new Inst(this::POP_HL, 1, "12   ", "----"),
                new Inst(this::LD_pCs_A, 1, "8    ", "----"), null,
                null, new Inst(this::PUSH_HL, 1, "16   ", "----"),
                new Inst(this::AND_d8, 2, "8    ", "Z010"), new Inst(this::RST_20H, 1, "16   ", "----"),
                // 0xe8
                new Inst(this::ADD_SP_r8, 2, "16   ", "00HC"), new Inst(this::JP_pHLs, 1, "4    ", "----"),
                new Inst(this::LD_pa16s_A, 3, "16   ", "----"), null,
                null, null,
                new Inst(this::XOR_d8, 2, "8    ", "Z000"), new Inst(this::RST_28H, 1, "16   ", "----"),
                // 0xf0
                new Inst(this::LDH_A_pa8s, 2, "12   ", "----"), new Inst(this::POP_AF, 1, "12   ", "ZNHC"),
                new Inst(this::LD_A_pCs, 1, "8    ", "----"), new Inst(this::DI, 1, "4    ", "----"),
                null, new Inst(this::PUSH_AF, 1, "16   ", "----"),
                new Inst(this::OR_d8, 2, "8    ", "Z000"), new Inst(this::RST_30H, 1, "16   ", "----"),
                // 0xf8
                new Inst(this::LD_HL_SPplsr8, 2, "12   ", "00HC"), new Inst(this::LD_SP_HL, 1, "8    ", "----"),
                new Inst(this::LD_A_pa16s, 3, "16   ", "----"), new Inst(this::EI, 1, "4    ", "----"),
                null, null,
                new Inst(this::CP_d8, 2, "8    ", "Z1HC"), new Inst(this::RST_38H, 1, "16   ", "----"),
                // 0x00 (CB)
                new Inst(this::RLC_B, 1, "4    ", "Z00C"), new Inst(this::RLC_C, 1, "4    ", "Z00C"),
                new Inst(this::RLC_D, 1, "4    ", "Z00C"), new Inst(this::RLC_E, 1, "4    ", "Z00C"),
                new Inst(this::RLC_H, 1, "4    ", "Z00C"), new Inst(this::RLC_L, 1, "4    ", "Z00C"),
                new Inst(this::RLC_pHLs, 1, "12   ", "Z00C"), new Inst(this::RLC_A, 1, "4    ", "Z00C"),
                // 0x08 (CB)
                new Inst(this::RRC_B, 1, "4    ", "Z00C"), new Inst(this::RRC_C, 1, "4    ", "Z00C"),
                new Inst(this::RRC_D, 1, "4    ", "Z00C"), new Inst(this::RRC_E, 1, "4    ", "Z00C"),
                new Inst(this::RRC_H, 1, "4    ", "Z00C"), new Inst(this::RRC_L, 1, "4    ", "Z00C"),
                new Inst(this::RRC_pHLs, 1, "12   ", "Z00C"), new Inst(this::RRC_A, 1, "4    ", "Z00C"),
                // 0x10 (CB)
                new Inst(this::RL_B, 1, "4    ", "Z00C"), new Inst(this::RL_C, 1, "4    ", "Z00C"),
                new Inst(this::RL_D, 1, "4    ", "Z00C"), new Inst(this::RL_E, 1, "4    ", "Z00C"),
                new Inst(this::RL_H, 1, "4    ", "Z00C"), new Inst(this::RL_L, 1, "4    ", "Z00C"),
                new Inst(this::RL_pHLs, 1, "12   ", "Z00C"), new Inst(this::RL_A, 1, "4    ", "Z00C"),
                // 0x18 (CB)
                new Inst(this::RR_B, 1, "4    ", "Z00C"), new Inst(this::RR_C, 1, "4    ", "Z00C"),
                new Inst(this::RR_D, 1, "4    ", "Z00C"), new Inst(this::RR_E, 1, "4    ", "Z00C"),
                new Inst(this::RR_H, 1, "4    ", "Z00C"), new Inst(this::RR_L, 1, "4    ", "Z00C"),
                new Inst(this::RR_pHLs, 1, "12   ", "Z00C"), new Inst(this::RR_A, 1, "4    ", "Z00C"),
                // 0x20 (CB)
                new Inst(this::SLA_B, 1, "4    ", "Z00C"), new Inst(this::SLA_C, 1, "4    ", "Z00C"),
                new Inst(this::SLA_D, 1, "4    ", "Z00C"), new Inst(this::SLA_E, 1, "4    ", "Z00C"),
                new Inst(this::SLA_H, 1, "4    ", "Z00C"), new Inst(this::SLA_L, 1, "4    ", "Z00C"),
                new Inst(this::SLA_pHLs, 1, "12   ", "Z00C"), new Inst(this::SLA_A, 1, "4    ", "Z00C"),
                // 0x28 (CB)
                new Inst(this::SRA_B, 1, "4    ", "Z000"), new Inst(this::SRA_C, 1, "4    ", "Z000"),
                new Inst(this::SRA_D, 1, "4    ", "Z000"), new Inst(this::SRA_E, 1, "4    ", "Z000"),
                new Inst(this::SRA_H, 1, "4    ", "Z000"), new Inst(this::SRA_L, 1, "4    ", "Z000"),
                new Inst(this::SRA_pHLs, 1, "12   ", "Z000"), new Inst(this::SRA_A, 1, "4    ", "Z000"),
                // 0x30 (CB)
                new Inst(this::SWAP_B, 1, "4    ", "Z000"), new Inst(this::SWAP_C, 1, "4    ", "Z000"),
                new Inst(this::SWAP_D, 1, "4    ", "Z000"), new Inst(this::SWAP_E, 1, "4    ", "Z000"),
                new Inst(this::SWAP_H, 1, "4    ", "Z000"), new Inst(this::SWAP_L, 1, "4    ", "Z000"),
                new Inst(this::SWAP_pHLs, 1, "12   ", "Z000"), new Inst(this::SWAP_A, 1, "4    ", "Z000"),
                // 0x38 (CB)
                new Inst(this::SRL_B, 1, "4    ", "Z00C"), new Inst(this::SRL_C, 1, "4    ", "Z00C"),
                new Inst(this::SRL_D, 1, "4    ", "Z00C"), new Inst(this::SRL_E, 1, "4    ", "Z00C"),
                new Inst(this::SRL_H, 1, "4    ", "Z00C"), new Inst(this::SRL_L, 1, "4    ", "Z00C"),
                new Inst(this::SRL_pHLs, 1, "12   ", "Z00C"), new Inst(this::SRL_A, 1, "4    ", "Z00C"),
                // 0x40 (CB)
                new Inst(this::BIT_0_B, 1, "4    ", "Z01-"), new Inst(this::BIT_0_C, 1, "4    ", "Z01-"),
                new Inst(this::BIT_0_D, 1, "4    ", "Z01-"), new Inst(this::BIT_0_E, 1, "4    ", "Z01-"),
                new Inst(this::BIT_0_H, 1, "4    ", "Z01-"), new Inst(this::BIT_0_L, 1, "4    ", "Z01-"),
                new Inst(this::BIT_0_pHLs, 1, "12   ", "Z01-"), new Inst(this::BIT_0_A, 1, "4    ", "Z01-"),
                // 0x48 (CB)
                new Inst(this::BIT_1_B, 1, "4    ", "Z01-"), new Inst(this::BIT_1_C, 1, "4    ", "Z01-"),
                new Inst(this::BIT_1_D, 1, "4    ", "Z01-"), new Inst(this::BIT_1_E, 1, "4    ", "Z01-"),
                new Inst(this::BIT_1_H, 1, "4    ", "Z01-"), new Inst(this::BIT_1_L, 1, "4    ", "Z01-"),
                new Inst(this::BIT_1_pHLs, 1, "12   ", "Z01-"), new Inst(this::BIT_1_A, 1, "4    ", "Z01-"),
                // 0x50 (CB)
                new Inst(this::BIT_2_B, 1, "4    ", "Z01-"), new Inst(this::BIT_2_C, 1, "4    ", "Z01-"),
                new Inst(this::BIT_2_D, 1, "4    ", "Z01-"), new Inst(this::BIT_2_E, 1, "4    ", "Z01-"),
                new Inst(this::BIT_2_H, 1, "4    ", "Z01-"), new Inst(this::BIT_2_L, 1, "4    ", "Z01-"),
                new Inst(this::BIT_2_pHLs, 1, "12   ", "Z01-"), new Inst(this::BIT_2_A, 1, "4    ", "Z01-"),
                // 0x58 (CB)
                new Inst(this::BIT_3_B, 1, "4    ", "Z01-"), new Inst(this::BIT_3_C, 1, "4    ", "Z01-"),
                new Inst(this::BIT_3_D, 1, "4    ", "Z01-"), new Inst(this::BIT_3_E, 1, "4    ", "Z01-"),
                new Inst(this::BIT_3_H, 1, "4    ", "Z01-"), new Inst(this::BIT_3_L, 1, "4    ", "Z01-"),
                new Inst(this::BIT_3_pHLs, 1, "12   ", "Z01-"), new Inst(this::BIT_3_A, 1, "4    ", "Z01-"),
                // 0x60 (CB)
                new Inst(this::BIT_4_B, 1, "4    ", "Z01-"), new Inst(this::BIT_4_C, 1, "4    ", "Z01-"),
                new Inst(this::BIT_4_D, 1, "4    ", "Z01-"), new Inst(this::BIT_4_E, 1, "4    ", "Z01-"),
                new Inst(this::BIT_4_H, 1, "4    ", "Z01-"), new Inst(this::BIT_4_L, 1, "4    ", "Z01-"),
                new Inst(this::BIT_4_pHLs, 1, "12   ", "Z01-"), new Inst(this::BIT_4_A, 1, "4    ", "Z01-"),
                // 0x68 (CB)
                new Inst(this::BIT_5_B, 1, "4    ", "Z01-"), new Inst(this::BIT_5_C, 1, "4    ", "Z01-"),
                new Inst(this::BIT_5_D, 1, "4    ", "Z01-"), new Inst(this::BIT_5_E, 1, "4    ", "Z01-"),
                new Inst(this::BIT_5_H, 1, "4    ", "Z01-"), new Inst(this::BIT_5_L, 1, "4    ", "Z01-"),
                new Inst(this::BIT_5_pHLs, 1, "12   ", "Z01-"), new Inst(this::BIT_5_A, 1, "4    ", "Z01-"),
                // 0x70 (CB)
                new Inst(this::BIT_6_B, 1, "4    ", "Z01-"), new Inst(this::BIT_6_C, 1, "4    ", "Z01-"),
                new Inst(this::BIT_6_D, 1, "4    ", "Z01-"), new Inst(this::BIT_6_E, 1, "4    ", "Z01-"),
                new Inst(this::BIT_6_H, 1, "4    ", "Z01-"), new Inst(this::BIT_6_L, 1, "4    ", "Z01-"),
                new Inst(this::BIT_6_pHLs, 1, "12   ", "Z01-"), new Inst(this::BIT_6_A, 1, "4    ", "Z01-"),
                // 0x78 (CB)
                new Inst(this::BIT_7_B, 1, "4    ", "Z01-"), new Inst(this::BIT_7_C, 1, "4    ", "Z01-"),
                new Inst(this::BIT_7_D, 1, "4    ", "Z01-"), new Inst(this::BIT_7_E, 1, "4    ", "Z01-"),
                new Inst(this::BIT_7_H, 1, "4    ", "Z01-"), new Inst(this::BIT_7_L, 1, "4    ", "Z01-"),
                new Inst(this::BIT_7_pHLs, 1, "12   ", "Z01-"), new Inst(this::BIT_7_A, 1, "4    ", "Z01-"),
                // 0x80 (CB)
                new Inst(this::RES_0_B, 1, "4    ", "----"), new Inst(this::RES_0_C, 1, "4    ", "----"),
                new Inst(this::RES_0_D, 1, "4    ", "----"), new Inst(this::RES_0_E, 1, "4    ", "----"),
                new Inst(this::RES_0_H, 1, "4    ", "----"), new Inst(this::RES_0_L, 1, "4    ", "----"),
                new Inst(this::RES_0_pHLs, 1, "12   ", "----"), new Inst(this::RES_0_A, 1, "4    ", "----"),
                // 0x88 (CB)
                new Inst(this::RES_1_B, 1, "4    ", "----"), new Inst(this::RES_1_C, 1, "4    ", "----"),
                new Inst(this::RES_1_D, 1, "4    ", "----"), new Inst(this::RES_1_E, 1, "4    ", "----"),
                new Inst(this::RES_1_H, 1, "4    ", "----"), new Inst(this::RES_1_L, 1, "4    ", "----"),
                new Inst(this::RES_1_pHLs, 1, "12   ", "----"), new Inst(this::RES_1_A, 1, "4    ", "----"),
                // 0x90 (CB)
                new Inst(this::RES_2_B, 1, "4    ", "----"), new Inst(this::RES_2_C, 1, "4    ", "----"),
                new Inst(this::RES_2_D, 1, "4    ", "----"), new Inst(this::RES_2_E, 1, "4    ", "----"),
                new Inst(this::RES_2_H, 1, "4    ", "----"), new Inst(this::RES_2_L, 1, "4    ", "----"),
                new Inst(this::RES_2_pHLs, 1, "12   ", "----"), new Inst(this::RES_2_A, 1, "4    ", "----"),
                // 0x98 (CB)
                new Inst(this::RES_3_B, 1, "4    ", "----"), new Inst(this::RES_3_C, 1, "4    ", "----"),
                new Inst(this::RES_3_D, 1, "4    ", "----"), new Inst(this::RES_3_E, 1, "4    ", "----"),
                new Inst(this::RES_3_H, 1, "4    ", "----"), new Inst(this::RES_3_L, 1, "4    ", "----"),
                new Inst(this::RES_3_pHLs, 1, "12   ", "----"), new Inst(this::RES_3_A, 1, "4    ", "----"),
                // 0xa0 (CB)
                new Inst(this::RES_4_B, 1, "4    ", "----"), new Inst(this::RES_4_C, 1, "4    ", "----"),
                new Inst(this::RES_4_D, 1, "4    ", "----"), new Inst(this::RES_4_E, 1, "4    ", "----"),
                new Inst(this::RES_4_H, 1, "4    ", "----"), new Inst(this::RES_4_L, 1, "4    ", "----"),
                new Inst(this::RES_4_pHLs, 1, "12   ", "----"), new Inst(this::RES_4_A, 1, "4    ", "----"),
                // 0xa8 (CB)
                new Inst(this::RES_5_B, 1, "4    ", "----"), new Inst(this::RES_5_C, 1, "4    ", "----"),
                new Inst(this::RES_5_D, 1, "4    ", "----"), new Inst(this::RES_5_E, 1, "4    ", "----"),
                new Inst(this::RES_5_H, 1, "4    ", "----"), new Inst(this::RES_5_L, 1, "4    ", "----"),
                new Inst(this::RES_5_pHLs, 1, "12   ", "----"), new Inst(this::RES_5_A, 1, "4    ", "----"),
                // 0xb0 (CB)
                new Inst(this::RES_6_B, 1, "4    ", "----"), new Inst(this::RES_6_C, 1, "4    ", "----"),
                new Inst(this::RES_6_D, 1, "4    ", "----"), new Inst(this::RES_6_E, 1, "4    ", "----"),
                new Inst(this::RES_6_H, 1, "4    ", "----"), new Inst(this::RES_6_L, 1, "4    ", "----"),
                new Inst(this::RES_6_pHLs, 1, "12   ", "----"), new Inst(this::RES_6_A, 1, "4    ", "----"),
                // 0xb8 (CB)
                new Inst(this::RES_7_B, 1, "4    ", "----"), new Inst(this::RES_7_C, 1, "4    ", "----"),
                new Inst(this::RES_7_D, 1, "4    ", "----"), new Inst(this::RES_7_E, 1, "4    ", "----"),
                new Inst(this::RES_7_H, 1, "4    ", "----"), new Inst(this::RES_7_L, 1, "4    ", "----"),
                new Inst(this::RES_7_pHLs, 1, "12   ", "----"), new Inst(this::RES_7_A, 1, "4    ", "----"),
                // 0xc0 (CB)
                new Inst(this::SET_0_B, 1, "4    ", "----"), new Inst(this::SET_0_C, 1, "4    ", "----"),
                new Inst(this::SET_0_D, 1, "4    ", "----"), new Inst(this::SET_0_E, 1, "4    ", "----"),
                new Inst(this::SET_0_H, 1, "4    ", "----"), new Inst(this::SET_0_L, 1, "4    ", "----"),
                new Inst(this::SET_0_pHLs, 1, "12   ", "----"), new Inst(this::SET_0_A, 1, "4    ", "----"),
                // 0xc8 (CB)
                new Inst(this::SET_1_B, 1, "4    ", "----"), new Inst(this::SET_1_C, 1, "4    ", "----"),
                new Inst(this::SET_1_D, 1, "4    ", "----"), new Inst(this::SET_1_E, 1, "4    ", "----"),
                new Inst(this::SET_1_H, 1, "4    ", "----"), new Inst(this::SET_1_L, 1, "4    ", "----"),
                new Inst(this::SET_1_pHLs, 1, "12   ", "----"), new Inst(this::SET_1_A, 1, "4    ", "----"),
                // 0xd0 (CB)
                new Inst(this::SET_2_B, 1, "4    ", "----"), new Inst(this::SET_2_C, 1, "4    ", "----"),
                new Inst(this::SET_2_D, 1, "4    ", "----"), new Inst(this::SET_2_E, 1, "4    ", "----"),
                new Inst(this::SET_2_H, 1, "4    ", "----"), new Inst(this::SET_2_L, 1, "4    ", "----"),
                new Inst(this::SET_2_pHLs, 1, "12   ", "----"), new Inst(this::SET_2_A, 1, "4    ", "----"),
                // 0xd8 (CB)
                new Inst(this::SET_3_B, 1, "4    ", "----"), new Inst(this::SET_3_C, 1, "4    ", "----"),
                new Inst(this::SET_3_D, 1, "4    ", "----"), new Inst(this::SET_3_E, 1, "4    ", "----"),
                new Inst(this::SET_3_H, 1, "4    ", "----"), new Inst(this::SET_3_L, 1, "4    ", "----"),
                new Inst(this::SET_3_pHLs, 1, "12   ", "----"), new Inst(this::SET_3_A, 1, "4    ", "----"),
                // 0xe0 (CB)
                new Inst(this::SET_4_B, 1, "4    ", "----"), new Inst(this::SET_4_C, 1, "4    ", "----"),
                new Inst(this::SET_4_D, 1, "4    ", "----"), new Inst(this::SET_4_E, 1, "4    ", "----"),
                new Inst(this::SET_4_H, 1, "4    ", "----"), new Inst(this::SET_4_L, 1, "4    ", "----"),
                new Inst(this::SET_4_pHLs, 1, "12   ", "----"), new Inst(this::SET_4_A, 1, "4    ", "----"),
                // 0xe8 (CB)
                new Inst(this::SET_5_B, 1, "4    ", "----"), new Inst(this::SET_5_C, 1, "4    ", "----"),
                new Inst(this::SET_5_D, 1, "4    ", "----"), new Inst(this::SET_5_E, 1, "4    ", "----"),
                new Inst(this::SET_5_H, 1, "4    ", "----"), new Inst(this::SET_5_L, 1, "4    ", "----"),
                new Inst(this::SET_5_pHLs, 1, "12   ", "----"), new Inst(this::SET_5_A, 1, "4    ", "----"),
                // 0xf0 (CB)
                new Inst(this::SET_6_B, 1, "4    ", "----"), new Inst(this::SET_6_C, 1, "4    ", "----"),
                new Inst(this::SET_6_D, 1, "4    ", "----"), new Inst(this::SET_6_E, 1, "4    ", "----"),
                new Inst(this::SET_6_H, 1, "4    ", "----"), new Inst(this::SET_6_L, 1, "4    ", "----"),
                new Inst(this::SET_6_pHLs, 1, "12   ", "----"), new Inst(this::SET_6_A, 1, "4    ", "----"),
                // 0xf8 (CB)
                new Inst(this::SET_7_B, 1, "4    ", "----"), new Inst(this::SET_7_C, 1, "4    ", "----"),
                new Inst(this::SET_7_D, 1, "4    ", "----"), new Inst(this::SET_7_E, 1, "4    ", "----"),
                new Inst(this::SET_7_H, 1, "4    ", "----"), new Inst(this::SET_7_L, 1, "4    ", "----"),
                new Inst(this::SET_7_pHLs, 1, "12   ", "----"), new Inst(this::SET_7_A, 1, "4    ", "----"),
        };
    }

    // 0x00
    private int NOP() {
        nimo = "NOP";
        return insts[0].cycle[0];
    }

    private int LD_BC_d16() {
        int d = mem.peekW(reg.pc);
        reg.pc += 2;
        reg.setBc(d);

        nimo = "LD BC,$%04X".formatted(d & 0xffff);

        return insts[0x01].cycle[0];
    }

    private int LD_pBCs_A() {
        mem.pokeB(reg.getBc(), reg.a);

        nimo = "LD (BC),A";

        return insts[0x02].cycle[0];
    }

    private int INC_BC() {
        reg.setBc(reg.getBc() + 1);

        nimo = "INC BC";

        return insts[0x03].cycle[0];
    }

    private int INC_B() {
        byte d = reg.b;
        reg.b++;

        reg.setZ(reg.b == 0);
        reg.setS(false);
        reg.setH(((d & 0xf) == 0xf));
        //reg.C =;

        nimo = "INC B";

        return insts[0x04].cycle[0];
    }

    private int DEC_B() {
        byte d = reg.b;
        reg.b--;

        reg.setZ(reg.b == 0);
        reg.setS(true);
        reg.setH(((d & 0xf) == 0x0));
        //reg.C =;

        nimo = "DEC B";

        return insts[0x05].cycle[0];
    }

    private int LD_B_d8() {
        byte d = mem.peekB(reg.pc++);
        reg.pc &= 0xffff;
        reg.b = d;

        nimo = "LD B,$%02X".formatted(d & 0xffff);

        return insts[0x06].cycle[0];
    }

    private int RLCA() {
        byte d = reg.a;
        reg.setC((d & 0x80) != 0);

        d = (byte) ((d << 1) | (reg.isC() ? 0x01 : 0));
        reg.a = d;

        reg.setZ(false);
        reg.setS(false);
        reg.setH(false);

        nimo = "RLCA";

        return insts[0x07].cycle[0];
    }

    // 0x08
    private int LD_pa16s_SP() {
        int a = mem.peekW(reg.pc);
        reg.pc += 2;
        mem.pokeW(a, reg.sp);

        nimo = "LD ($%04X),SP".formatted(a & 0xff);

        return insts[0x08].cycle[0];
    }

    private int ADD_HL_BC() {
        int hl = reg.getHl();
        int rr = reg.getBc();
        int res = hl + rr;
        reg.setHl(res & 0xFFFF);

        reg.setS(false);
        reg.setH(((hl & 0x0FFF) + (rr & 0x0FFF)) > 0x0FFF);
        reg.setC(res > 0xFFFF);

        nimo = "ADD HL,BC";

        return insts[0x09].cycle[0];
    }

    private int LD_A_pBCs() {
        reg.a = mem.peekB(reg.getBc());

        nimo = "LD A,(BC)";

        return insts[0x0a].cycle[0];
    }

    private int DEC_BC() {
        reg.setBc(reg.getBc() - 1);

        nimo = "DEC BC";

        return insts[0x0b].cycle[0];
    }

    private int INC_C() {
        byte d = reg.c;
        reg.c++;

        reg.setZ(reg.c == 0);
        reg.setS(false);
        reg.setH(((d & 0xf) == 0xf));
        //reg.C =;

        nimo = "INC C";

        return insts[0x0c].cycle[0];
    }

    private int DEC_C() {
        byte d = reg.c;
        reg.c--;

        reg.setZ(reg.c == 0);
        reg.setS(true);
        reg.setH(((d & 0xf) == 0x0));
        //reg.C =;

        nimo = "DEC C";

        return insts[0x0d].cycle[0];
    }

    private int LD_C_d8() {
        byte d = mem.peekB(reg.pc++);
        reg.pc &= 0xffff;
        reg.c = d;

        nimo = "LD C,$%02X".formatted(d & 0xffff);

        return insts[0x0e].cycle[0];
    }

    private int RRCA() {
        byte d = reg.a;
        reg.setC((d & 0x01) != 0);

        d = (byte) (((d & 0xFF) >>> 1) | (reg.isC() ? 0x80 : 0));
        reg.a = d;

        reg.setZ(false);
        reg.setS(false);
        reg.setH(false);

        nimo = "RRCA";

        return insts[0x0f].cycle[0];
    }

    // 0x10
    private int STOP_0() {
        reg.pc++;
        reg.pc &= 0xFFFF;
        isStop = true;
        nimo = "STOP 0";
        return insts[0x10].cycle[0];
    }

    private int LD_DE_d16() {
        int d = mem.peekW(reg.pc);
        reg.pc += 2;
        reg.setDe(d);

        nimo = "LD DE,$%04X".formatted(d & 0xffff);

        return insts[0x11].cycle[0];
    }

    private int LD_pDEs_A() {
        mem.pokeB(reg.getDe(), reg.a);

        nimo = "LD (DE),A";

        return insts[0x12].cycle[0];
    }

    private int INC_DE() {
        reg.setDe(reg.getDe() + 1);

        nimo = "INC DE";

        return insts[0x13].cycle[0];
    }

    private int INC_D() {
        byte d = reg.d;
        reg.d++;

        reg.setZ(reg.d == 0);
        reg.setS(false);
        reg.setH(((d & 0xf) == 0xf));
        //reg.C =;

        nimo = "INC D";

        return insts[0x14].cycle[0];
    }

    private int DEC_D() {
        byte d = reg.d;
        reg.d--;

        reg.setZ(reg.d == 0);
        reg.setS(true);
        reg.setH(((d & 0xf) == 0x0));
        //reg.C =;

        nimo = "DEC D";

        return insts[0x15].cycle[0];
    }

    private int LD_D_d8() {
        byte d = mem.peekB(reg.pc++);
        reg.pc &= 0xffff;
        reg.d = d;

        nimo = "LD D,$%02X".formatted(d & 0xffff);

        return insts[0x16].cycle[0];
    }

    private int RLA() {
        byte d = reg.a;
        byte e = (byte) (reg.isC() ? 0x01 : 0);
        reg.setC((d & 0x80) != 0);

        d = (byte) ((d << 1) | e);
        reg.a = d;

        reg.setZ(false);
        reg.setS(false);
        reg.setH(false);

        nimo = "RLA";

        return insts[0x17].cycle[0];
    }

    // 0x18
    private int JR_r8() {
        byte b = mem.peekB(reg.pc);
        int d = b; // signed
        reg.pc++;
        reg.pc &= 0xFFFF;
        int c = insts[0x18].cycle[0];
        reg.pc = (reg.pc + d) & 0xFFFF;

        nimo = "JR $%02X".formatted(d & 0xffff);

        return c;
    }

    private int ADD_HL_DE() {
        int hl = reg.getHl();
        int rr = reg.getDe();
        int res = hl + rr;
        reg.setHl(res & 0xFFFF);

        reg.setS(false);
        reg.setH(((hl & 0x0FFF) + (rr & 0x0FFF)) > 0x0FFF);
        reg.setC(res > 0xFFFF);

        nimo = "ADD HL,DE";

        return insts[0x19].cycle[0];
    }

    private int LD_A_pDEs() {
        reg.a = mem.peekB(reg.getDe());

        nimo = "LD A,(DE)";

        return insts[0x1a].cycle[0];
    }

    private int DEC_DE() {
        reg.setDe(reg.getDe() - 1);

        nimo = "DEC DE";

        return insts[0x1b].cycle[0];
    }

    private int INC_E() {
        byte d = reg.e;
        reg.e++;

        reg.setZ(reg.e == 0);
        reg.setS(false);
        reg.setH(((d & 0xf) == 0xf));
        //reg.c =;

        nimo = "INC E";

        return insts[0x1c].cycle[0];
    }

    private int DEC_E() {
        byte d = reg.e;
        reg.e--;

        reg.setZ(reg.e == 0);
        reg.setS(true);
        reg.setH(((d & 0xf) == 0x0));
        //reg.C =;

        nimo = "DEC E";

        return insts[0x1d].cycle[0];
    }

    private int LD_E_d8() {
        byte d = mem.peekB(reg.pc++);
        reg.pc &= 0xffff;
        reg.e = d;

        nimo = "LD E,$%02X".formatted(d & 0xffff);

        return insts[0x1e].cycle[0];
    }

    private int RRA() {
        byte d = reg.a;
        byte e = (byte) (reg.isC() ? 0x80 : 0);
        reg.setC((d & 0x01) != 0);

        d = (byte) (((d & 0xFF) >>> 1) | e);
        reg.a = d;

        reg.setZ(false);
        reg.setS(false);
        reg.setH(false);

        nimo = "RRA";

        return insts[0x1f].cycle[0];
    }

    // 0x20
    private int JR_NZ_r8() {
        byte b = mem.peekB(reg.pc);
        int d = b; // signed
        reg.pc++;
        reg.pc &= 0xFFFF;
        int c = insts[0x20].cycle[1];
        if (!reg.isZ()) {
            reg.pc = (reg.pc + d) & 0xFFFF;
            c = insts[0x20].cycle[0];
        }

        nimo = "JR NZ,$%02X".formatted(d & 0xffff);

        return c;
    }

    private int LD_HL_d16() {
        int d = mem.peekW(reg.pc);
        reg.pc += 2;
        reg.setHl(d);

        nimo = "LD HL,$%04X".formatted(d & 0xffff);

        return insts[0x21].cycle[0];
    }

    private int LD_pHLplss_A() {
        mem.pokeB(reg.getHl(), reg.a);
        reg.setHl(reg.getHl() + 1);

        nimo = "LD (HL+),A";

        return insts[0x22].cycle[0];
    }

    private int INC_HL() {
        reg.setHl(reg.getHl() + 1);

        nimo = "INC HL";

        return insts[0x23].cycle[0];
    }

    private int INC_H() {
        byte d = reg.h;
        reg.h++;

        reg.setZ(reg.h == 0);
        reg.setS(false);
        reg.setH(((d & 0xf) == 0xf));
        //reg.C =;

        nimo = "INC H";

        return insts[0x24].cycle[0];
    }

    private int DEC_H() {
        byte d = reg.h;
        reg.h--;

        reg.setZ(reg.h == 0);
        reg.setS(true);
        reg.setH(((d & 0xf) == 0x0));
        //reg.C =;

        nimo = "DEC H";

        return insts[0x25].cycle[0];
    }

    private int LD_H_d8() {
        byte d = mem.peekB(reg.pc++);
        reg.pc &= 0xffff;
        reg.h = d;

        nimo = "LD H,$%02X".formatted(d);

        return insts[0x26].cycle[0];
    }

    private int DAA() {
        int a = reg.a & 0xFF;
        if (!reg.isS()) {
            if (reg.isH() || (a & 0x0F) > 0x09) {
                a += 0x06;
            }
            if (reg.isC() || a > 0x9F) {
                a += 0x60;
                reg.setC(true);
            }
        } else {
            if (reg.isH()) {
                a -= 0x06;
            }
            if (reg.isC()) {
                a -= 0x60;
            }
        }

        reg.a = (byte) a;
        reg.setZ(reg.a == 0);
        reg.setH(false);

        nimo = "DAA";

        return insts[0x27].cycle[0];
    }

    // 0x28
    private int JR_Z_r8() {
        byte b = mem.peekB(reg.pc);
        int d = b; // signed
        reg.pc++;
        reg.pc &= 0xFFFF;
        int c = insts[0x28].cycle[1];
        if (reg.isZ()) {
            reg.pc = (reg.pc + d) & 0xFFFF;
            c = insts[0x28].cycle[0];
        }

        nimo = "JR Z,$%02X".formatted(d & 0xffff);

        return c;
    }

    private int ADD_HL_HL() {
        int hl = reg.getHl();
        int res = hl + hl;
        reg.setHl(res & 0xFFFF);

        reg.setS(false);
        reg.setH(((hl & 0x0FFF) + (hl & 0x0FFF)) > 0x0FFF);
        reg.setC(res > 0xFFFF);

        nimo = "ADD HL,HL";

        return insts[0x29].cycle[0];
    }

    private int LD_A_pHLplss() {
        byte d = mem.peekB(reg.getHl());
        reg.a = d;
        reg.setHl(reg.getHl() + 1);

        nimo = "LD A,(HL+)";

        return insts[0x2a].cycle[0];
    }

    private int DEC_HL() {
        reg.setHl(reg.getHl() - 1);

        nimo = "DEC HL";

        return insts[0x2b].cycle[0];
    }

    private int INC_L() {
        byte d = reg.l;
        reg.l++;

        reg.setZ(reg.l == 0);
        reg.setS(false);
        reg.setH(((d & 0xf) == 0xf));
        //reg.c =;

        nimo = "INC L";

        return insts[0x2c].cycle[0];
    }

    private int DEC_L() {
        byte d = reg.l;
        reg.l--;

        reg.setZ(reg.l == 0);
        reg.setS(true);
        reg.setH(((d & 0xf) == 0x0));
        //reg.c =;

        nimo = "DEC L";

        return insts[0x2d].cycle[0];
    }

    private int LD_L_d8() {
        byte d = mem.peekB(reg.pc++);
        reg.pc &= 0xffff;
        reg.l = d;

        nimo = "LD L,$%02X".formatted(d & 0xffff);

        return insts[0x2e].cycle[0];
    }

    private int CPL() {
        reg.a = (byte) (~reg.a);

        nimo = "CPL";

        return insts[0x2f].cycle[0];
    }

    // 0x30
    private int JR_NC_r8() {
        byte b = mem.peekB(reg.pc);
        int d = b; // signed
        reg.pc++;
        reg.pc &= 0xFFFF;
        int c = insts[0x30].cycle[1];
        if (!reg.isC()) {
            reg.pc = (reg.pc + d) & 0xFFFF;
            c = insts[0x30].cycle[0];
        }

        nimo = "JR NC,$%02X".formatted(d & 0xffff);

        return c;
    }

    private int LD_SP_d16() {
        int d = mem.peekW(reg.pc);
        reg.pc += 2;
        reg.sp = d;

        nimo = "LD SP,$%04X".formatted(d & 0xffff);

        return insts[0x31].cycle[0];
    }

    private int LD_pHLmiss_A() {
        mem.pokeB(reg.getHl(), reg.a);
        reg.setHl(reg.getHl() - 1);

        nimo = "LD (HL-),A";

        return insts[0x32].cycle[0];
    }

    private int INC_SP() {
        reg.sp++;
        reg.sp &= 0xFFFF;

        nimo = "INC SP";

        return insts[0x33].cycle[0];
    }

    private int INC_pHLs() {
        byte d = mem.peekB(reg.getHl());
        byte e = (byte) (d + 1);
        mem.pokeB(reg.getHl(), e);

        reg.setZ(e == 0);
        reg.setS(false);
        reg.setH(((d & 0xf) == 0xf));
        //reg.C =;

        nimo = "INC (HL)";

        return insts[0x34].cycle[0];
    }

    private int DEC_pHLs() {
        byte d = mem.peekB(reg.getHl());
        byte e = (byte) (d - 1);
        mem.pokeB(reg.getHl(), e);

        reg.setZ(e == 0);
        reg.setS(true);
        reg.setH(((d & 0xf) == 0x0));
        //reg.C =;

        nimo = "DEC (HL)";

        return insts[0x35].cycle[0];
    }

    private int LD_pHLs_d8() {
        byte d = mem.peekB(reg.pc++);
        reg.pc &= 0xffff;
        mem.pokeB(reg.getHl(), d);

        nimo = "LD (HL),$%02X".formatted(d & 0xffff);

        return insts[0x36].cycle[0];
    }

    private int SCF() {
        reg.setS(false);
        reg.setH(false);
        reg.setC(true);

        nimo = "SCF";

        return insts[0x37].cycle[0];
    }

    // 0x38
    private int JR_C_r8() {
        byte b = mem.peekB(reg.pc);
        int d = b; // signed
        reg.pc++;
        reg.pc &= 0xFFFF;
        int c = insts[0x38].cycle[1];
        if (reg.isC()) {
            reg.pc = (reg.pc + d) & 0xFFFF;
            c = insts[0x38].cycle[0];
        }

        nimo = "JR C,$%02X".formatted(d & 0xffff);

        return c;
    }

    private int ADD_HL_SP() {
        int hl = reg.getHl();
        int sp = reg.sp;
        int res = hl + sp;
        reg.setHl(res & 0xFFFF);

        reg.setS(false);
        reg.setH(((hl & 0x0FFF) + (sp & 0x0FFF)) > 0x0FFF);
        reg.setC(res > 0xFFFF);

        nimo = "ADD HL,SP";

        return insts[0x39].cycle[0];
    }

    private int LD_A_pHLmiss() {
        byte d = mem.peekB(reg.getHl());
        reg.a = d;
        reg.setHl(reg.getHl() - 1);

        nimo = "LD A,(HL-)";

        return insts[0x3a].cycle[0];
    }

    private int DEC_SP() {
        reg.sp--;
        reg.sp &= 0xFFFF;

        nimo = "DEC SP";

        return insts[0x3b].cycle[0];
    }

    private int INC_A() {
        byte d = reg.a;
        reg.a++;

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((d & 0xf) == 0xf));
        //reg.C =;

        nimo = "INC A";

        return insts[0x3c].cycle[0];
    }

    private int DEC_A() {
        byte d = reg.a;
        reg.a--;

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((d & 0xf) == 0x0));
        //reg.C =;

        nimo = "DEC A";

        return insts[0x3d].cycle[0];
    }

    private int LD_A_d8() {
        byte d = mem.peekB(reg.pc++);
        reg.pc &= 0xffff;
        reg.a = d;

        nimo = "LD A,$%02X".formatted(d & 0xffff);

        return insts[0x3e].cycle[0];
    }

    private int CCF() {
        reg.setS(false);
        reg.setH(false);
        reg.setC(!reg.isC());

        nimo = "CCF";

        return insts[0x3f].cycle[0];
    }

    // 0x40
    private int LD_B_B() {
//        reg.b = reg.b;

        nimo = "LD B,B";

        return insts[0x40].cycle[0];
    }

    private int LD_B_C() {
        reg.b = reg.c;

        nimo = "LD B,C";

        return insts[0x41].cycle[0];
    }

    private int LD_B_D() {
        reg.b = reg.d;

        nimo = "LD B,D";

        return insts[0x42].cycle[0];
    }

    private int LD_B_E() {
        reg.b = reg.e;

        nimo = "LD B,E";

        return insts[0x43].cycle[0];
    }

    private int LD_B_H() {
        reg.b = reg.h;

        nimo = "LD B,H";

        return insts[0x44].cycle[0];
    }

    private int LD_B_L() {
        reg.b = reg.l;

        nimo = "LD B,L";

        return insts[0x45].cycle[0];
    }

    private int LD_B_pHLs() {
        byte d = mem.peekB(reg.getHl());
        reg.b = d;

        nimo = "LD B,(HL)";

        return insts[0x46].cycle[0];
    }

    private int LD_B_A() {
        reg.b = reg.a;

        nimo = "LD B,A";

        return insts[0x47].cycle[0];
    }

    // 0x48
    private int LD_C_B() {
        reg.c = reg.b;

        nimo = "LD C,B";

        return insts[0x48].cycle[0];
    }

    private int LD_C_C() {
//        reg.c = reg.c;

        nimo = "LD C,C";

        return insts[0x49].cycle[0];
    }

    private int LD_C_D() {
        reg.c = reg.d;

        nimo = "LD C,D";

        return insts[0x4a].cycle[0];
    }

    private int LD_C_E() {
        reg.c = reg.e;

        nimo = "LD C,E";

        return insts[0x4b].cycle[0];
    }

    private int LD_C_H() {
        reg.c = reg.h;

        nimo = "LD C,H";

        return insts[0x4c].cycle[0];
    }

    private int LD_C_L() {
        reg.c = reg.l;

        nimo = "LD C,L";

        return insts[0x4d].cycle[0];
    }

    private int LD_C_pHLs() {
        byte d = mem.peekB(reg.getHl());
        reg.c = d;

        nimo = "LD C,(HL)";

        return insts[0x4e].cycle[0];
    }

    private int LD_C_A() {
        reg.c = reg.a;

        nimo = "LD C,A";

        return insts[0x4f].cycle[0];
    }

    // 0x50
    private int LD_D_B() {
        reg.d = reg.b;

        nimo = "LD D,B";

        return insts[0x50].cycle[0];
    }

    private int LD_D_C() {
        reg.d = reg.c;

        nimo = "LD D,C";

        return insts[0x51].cycle[0];
    }

    private int LD_D_D() {
//        reg.d = reg.d;

        nimo = "LD D,D";

        return insts[0x52].cycle[0];
    }

    private int LD_D_E() {
        reg.d = reg.e;

        nimo = "LD D,E";

        return insts[0x53].cycle[0];
    }

    private int LD_D_H() {
        reg.d = reg.h;

        nimo = "LD D,H";

        return insts[0x54].cycle[0];
    }

    private int LD_D_L() {
        reg.d = reg.l;

        nimo = "LD D,L";

        return insts[0x55].cycle[0];
    }

    private int LD_D_pHLs() {
        byte d = mem.peekB(reg.getHl());
        reg.d = d;

        nimo = "LD D,(HL)";

        return insts[0x56].cycle[0];
    }

    private int LD_D_A() {
        reg.d = reg.a;

        nimo = "LD D,A";

        return insts[0x57].cycle[0];
    }

    // 0x58
    private int LD_E_B() {
        reg.e = reg.b;

        nimo = "LD E,B";

        return insts[0x58].cycle[0];
    }

    private int LD_E_C() {
        reg.e = reg.c;

        nimo = "LD E,C";

        return insts[0x59].cycle[0];
    }

    private int LD_E_D() {
        reg.e = reg.d;

        nimo = "LD E,D";

        return insts[0x5a].cycle[0];
    }

    private int LD_E_E() {
//        reg.e = reg.e;

        nimo = "LD E,E";

        return insts[0x5b].cycle[0];
    }

    private int LD_E_H() {
        reg.e = reg.h;

        nimo = "LD E,H";

        return insts[0x5c].cycle[0];
    }

    private int LD_E_L() {
        reg.e = reg.l;

        nimo = "LD E,L";

        return insts[0x5d].cycle[0];
    }

    private int LD_E_pHLs() {
        byte d = mem.peekB(reg.getHl());
        reg.e = d;

        nimo = "LD E,(HL)";

        return insts[0x5e].cycle[0];
    }

    private int LD_E_A() {
        reg.e = reg.a;

        nimo = "LD E,A";

        return insts[0x5f].cycle[0];
    }

    // 0x60
    private int LD_H_B() {
        reg.h = reg.b;

        nimo = "LD H,B";

        return insts[0x60].cycle[0];
    }

    private int LD_H_C() {
        reg.h = reg.c;

        nimo = "LD H,C";

        return insts[0x61].cycle[0];
    }

    private int LD_H_D() {
        reg.h = reg.d;

        nimo = "LD H,D";

        return insts[0x62].cycle[0];
    }

    private int LD_H_E() {
        reg.h = reg.e;

        nimo = "LD H,E";

        return insts[0x63].cycle[0];
    }

    private int LD_H_H() {
//        reg.h = reg.h;

        nimo = "LD H,H";

        return insts[0x64].cycle[0];
    }

    private int LD_H_L() {
        reg.h = reg.l;

        nimo = "LD H,L";

        return insts[0x65].cycle[0];
    }

    private int LD_H_pHLs() {
        byte d = mem.peekB(reg.getHl());
        reg.h = d;

        nimo = "LD H,(HL)";

        return insts[0x66].cycle[0];
    }

    private int LD_H_A() {
        reg.h = reg.a;

        nimo = "LD H,A";

        return insts[0x67].cycle[0];
    }

    // 0x68
    private int LD_L_B() {
        reg.l = reg.b;

        nimo = "LD L,B";

        return insts[0x68].cycle[0];
    }

    private int LD_L_C() {
        reg.l = reg.c;

        nimo = "LD L,C";

        return insts[0x69].cycle[0];
    }

    private int LD_L_D() {
        reg.l = reg.d;

        nimo = "LD L,D";

        return insts[0x6a].cycle[0];
    }

    private int LD_L_E() {
        reg.l = reg.e;

        nimo = "LD L,E";

        return insts[0x6b].cycle[0];
    }

    private int LD_L_H() {
        reg.l = reg.h;

        nimo = "LD L,H";

        return insts[0x6c].cycle[0];
    }

    private int LD_L_L() {
//        reg.l = reg.l;

        nimo = "LD L,L";

        return insts[0x6d].cycle[0];
    }

    private int LD_L_pHLs() {
        byte d = mem.peekB(reg.getHl());
        reg.l = d;

        nimo = "LD L,(HL)";

        return insts[0x6e].cycle[0];
    }

    private int LD_L_A() {
        reg.l = reg.a;

        nimo = "LD L,A";

        return insts[0x6f].cycle[0];
    }

    // 0x70
    private int LD_pHLs_B() {
        mem.pokeB(reg.getHl(), reg.b);

        nimo = "LD (HL),B";

        return insts[0x70].cycle[0];
    }

    private int LD_pHLs_C() {
        mem.pokeB(reg.getHl(), reg.c);

        nimo = "LD (HL),C";

        return insts[0x71].cycle[0];
    }

    private int LD_pHLs_D() {
        mem.pokeB(reg.getHl(), reg.d);

        nimo = "LD (HL),D";

        return insts[0x72].cycle[0];
    }

    private int LD_pHLs_E() {
        mem.pokeB(reg.getHl(), reg.e);

        nimo = "LD (HL),E";

        return insts[0x73].cycle[0];
    }

    private int LD_pHLs_H() {
        mem.pokeB(reg.getHl(), reg.h);

        nimo = "LD (HL),H";

        return insts[0x74].cycle[0];
    }

    private int LD_pHLs_L() {
        mem.pokeB(reg.getHl(), reg.l);

        nimo = "LD (HL),L";

        return insts[0x75].cycle[0];
    }

    private int HALT() {
        isHalt = true;

        nimo = "HALT";

        return insts[0x76].cycle[0];
    }

    private int LD_pHLs_A() {
        mem.pokeB(reg.getHl(), reg.a);

        nimo = "LD (HL),A";

        return insts[0x77].cycle[0];
    }

    // 0x78
    private int LD_A_B() {
        reg.a = reg.b;

        nimo = "LD A,B";

        return insts[0x78].cycle[0];
    }

    private int LD_A_C() {
        reg.a = reg.c;

        nimo = "LD A,C";

        return insts[0x79].cycle[0];
    }

    private int LD_A_D() {
        reg.a = reg.d;

        nimo = "LD A,D";

        return insts[0x7a].cycle[0];
    }

    private int LD_A_E() {
        reg.a = reg.e;

        nimo = "LD A,E";

        return insts[0x7b].cycle[0];
    }

    private int LD_A_H() {
        reg.a = reg.h;

        nimo = "LD A,H";

        return insts[0x7c].cycle[0];
    }

    private int LD_A_L() {
        reg.a = reg.l;

        nimo = "LD A,L";

        return insts[0x7d].cycle[0];
    }

    private int LD_A_pHLs() {
        byte d = mem.peekB(reg.getHl());
        reg.a = d;

        nimo = "LD A,(HL)";

        return insts[0x7e].cycle[0];
    }

    private int LD_A_A() {
//        reg.a = reg.a;

        nimo = "LD A,A";

        return insts[0x7f].cycle[0];
    }

    // 0x80
    private int ADD_A_B() {
        int a = reg.a & 0xFF;
        int b = reg.b & 0xFF;
        reg.a = (byte) ((a + b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf)) > 0xf);
        reg.setC((a + b) > 0xff);

        nimo = "ADD A,B";

        return insts[0x80].cycle[0];
    }

    private int ADD_A_C() {
        int a = reg.a & 0xFF;
        int b = reg.c & 0xFF;
        reg.a = (byte) ((a + b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf)) > 0xf);
        reg.setC((a + b) > 0xff);

        nimo = "ADD A,C";

        return insts[0x81].cycle[0];
    }

    private int ADD_A_D() {
        int a = reg.a & 0xFF;
        int b = reg.d & 0xFF;
        reg.a = (byte) ((a + b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf)) > 0xf);
        reg.setC((a + b) > 0xff);

        nimo = "ADD A,D";

        return insts[0x82].cycle[0];
    }

    private int ADD_A_E() {
        int a = reg.a & 0xFF;
        int b = reg.e & 0xFF;
        reg.a = (byte) ((a + b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf)) > 0xf);
        reg.setC((a + b) > 0xff);

        nimo = "ADD A,E";

        return insts[0x83].cycle[0];
    }

    private int ADD_A_H() {
        int a = reg.a & 0xFF;
        int b = reg.h & 0xFF;
        reg.a = (byte) ((a + b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf)) > 0xf);
        reg.setC((a + b) > 0xff);

        nimo = "ADD A,H";

        return insts[0x84].cycle[0];
    }

    private int ADD_A_L() {
        int a = reg.a & 0xFF;
        int b = reg.l & 0xFF;
        reg.a = (byte) ((a + b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf)) > 0xf);
        reg.setC((a + b) > 0xff);

        nimo = "ADD A,L";

        return insts[0x85].cycle[0];
    }

    private int ADD_A_pHLs() {
        int a = reg.a & 0xFF;
        int b = mem.peekB(reg.getHl()) & 0xFF;
        reg.a = (byte) ((a + b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf)) > 0xf);
        reg.setC((a + b) > 0xff);

        nimo = "ADD A,(HL)";

        return insts[0x86].cycle[0];
    }

    private int ADD_A_A() {
        int a = reg.a & 0xFF;
        int b = reg.a & 0xFF;
        reg.a = (byte) ((a + b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf)) > 0xf);
        reg.setC((a + b) > 0xff);

        nimo = "ADD A,A";

        return insts[0x87].cycle[0];
    }

    // 0x88
    private int ADC_A_B() {
        int a = reg.a & 0xFF;
        int b = reg.b & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a + b + carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf) + carry) > 0xf);
        reg.setC((a + b + carry) > 0xff);

        nimo = "ADC A,B";

        return insts[0x88].cycle[0];
    }

    private int ADC_A_C() {
        int a = reg.a & 0xFF;
        int b = reg.c & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a + b + carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf) + carry) > 0xf);
        reg.setC((a + b + carry) > 0xff);

        nimo = "ADC A,C";

        return insts[0x89].cycle[0];
    }

    private int ADC_A_D() {
        int a = reg.a & 0xFF;
        int b = reg.d & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a + b + carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf) + carry) > 0xf);
        reg.setC((a + b + carry) > 0xff);

        nimo = "ADC A,D";

        return insts[0x8a].cycle[0];
    }

    private int ADC_A_E() {
        int a = reg.a & 0xFF;
        int b = reg.e & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a + b + carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf) + carry) > 0xf);
        reg.setC((a + b + carry) > 0xff);

        nimo = "ADC A,E";

        return insts[0x8b].cycle[0];
    }

    private int ADC_A_H() {
        int a = reg.a & 0xFF;
        int b = reg.h & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a + b + carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf) + carry) > 0xf);
        reg.setC((a + b + carry) > 0xff);

        nimo = "ADC A,H";

        return insts[0x8c].cycle[0];
    }

    private int ADC_A_L() {
        int a = reg.a & 0xFF;
        int b = reg.l & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a + b + carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf) + carry) > 0xf);
        reg.setC((a + b + carry) > 0xff);

        nimo = "ADC A,L";

        return insts[0x8d].cycle[0];
    }

    private int ADC_A_pHLs() {
        int a = reg.a & 0xFF;
        int b = mem.peekB(reg.getHl()) & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a + b + carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf) + carry) > 0xf);
        reg.setC((a + b + carry) > 0xff);

        nimo = "ADC A,(HL)";

        return insts[0x8e].cycle[0];
    }

    private int ADC_A_A() {
        int a = reg.a & 0xFF;
        int b = reg.a & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a + b + carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf) + carry) > 0xf);
        reg.setC((a + b + carry) > 0xff);

        nimo = "ADC A,A";

        return insts[0x8f].cycle[0];
    }

    // 0x90
    private int SUB_B() {
        int a = reg.a & 0xFF;
        int b = reg.b & 0xFF;
        reg.a = (byte) ((a - b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf)) < 0);
        reg.setC((a - b) < 0);

        nimo = "SUB B,$%02X".formatted(b & 0xff);

        return insts[0x90].cycle[0];
    }

    private int SUB_C() {
        int a = reg.a & 0xFF;
        int b = reg.c & 0xFF;
        reg.a = (byte) ((a - b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf)) < 0);
        reg.setC((a - b) < 0);

        nimo = "SUB C,$%02X".formatted(b & 0xff);

        return insts[0x91].cycle[0];
    }

    private int SUB_D() {
        int a = reg.a & 0xFF;
        int b = reg.d & 0xFF;
        reg.a = (byte) ((a - b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf)) < 0);
        reg.setC((a - b) < 0);

        nimo = "SUB D,$%02X".formatted(b & 0xff);

        return insts[0x92].cycle[0];
    }

    private int SUB_E() {
        int a = reg.a & 0xFF;
        int b = reg.e & 0xFF;
        reg.a = (byte) ((a - b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf)) < 0);
        reg.setC((a - b) < 0);

        nimo = "SUB E,$%02X".formatted(b & 0xff);

        return insts[0x93].cycle[0];
    }

    private int SUB_H() {
        int a = reg.a & 0xFF;
        int b = reg.h & 0xFF;
        reg.a = (byte) ((a - b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf)) < 0);
        reg.setC((a - b) < 0);

        nimo = "SUB H,$%02X".formatted(b & 0xff);

        return insts[0x94].cycle[0];
    }

    private int SUB_L() {
        int a = reg.a & 0xFF;
        int b = reg.l & 0xFF;
        reg.a = (byte) ((a - b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf)) < 0);
        reg.setC((a - b) < 0);

        nimo = "SUB L,$%02X".formatted(b & 0xff);

        return insts[0x95].cycle[0];
    }

    private int SUB_pHLs() {
        int a = reg.a & 0xFF;
        int b = mem.peekB(reg.getHl()) & 0xFF;
        reg.a = (byte) ((a - b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf)) < 0);
        reg.setC((a - b) < 0);

        nimo = "SUB (HL),$%02X".formatted(b & 0xff);

        return insts[0x96].cycle[0];
    }

    private int SUB_A() {
        int a = reg.a & 0xFF;
        int b = reg.a & 0xFF;
        reg.a = (byte) ((a - b) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf)) < 0);
        reg.setC((a - b) < 0);

        nimo = "SUB A,$%02X".formatted(b & 0xff);

        return insts[0x97].cycle[0];
    }

    // 0x98
    private int SBC_A_B() {
        int a = reg.a & 0xFF;
        int b = reg.b & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a - b - carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf) - carry) < 0);
        reg.setC((a - b - carry) < 0);

        nimo = "SBC A,B";

        return insts[0x98].cycle[0];
    }

    private int SBC_A_C() {
        int a = reg.a & 0xFF;
        int b = reg.c & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a - b - carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf) - carry) < 0);
        reg.setC((a - b - carry) < 0);

        nimo = "SBC A,C";

        return insts[0x99].cycle[0];
    }

    private int SBC_A_D() {
        int a = reg.a & 0xFF;
        int b = reg.d & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a - b - carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf) - carry) < 0);
        reg.setC((a - b - carry) < 0);

        nimo = "SBC A,D";

        return insts[0x9a].cycle[0];
    }

    private int SBC_A_E() {
        int a = reg.a & 0xFF;
        int b = reg.e & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a - b - carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf) - carry) < 0);
        reg.setC((a - b - carry) < 0);

        nimo = "SBC A,E";

        return insts[0x9b].cycle[0];
    }

    private int SBC_A_H() {
        int a = reg.a & 0xFF;
        int b = reg.h & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a - b - carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf) - carry) < 0);
        reg.setC((a - b - carry) < 0);

        nimo = "SBC A,H";

        return insts[0x9c].cycle[0];
    }

    private int SBC_A_L() {
        int a = reg.a & 0xFF;
        int b = reg.l & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a - b - carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf) - carry) < 0);
        reg.setC((a - b - carry) < 0);

        nimo = "SBC A,L";

        return insts[0x9d].cycle[0];
    }

    private int SBC_A_pHLs() {
        int a = reg.a & 0xFF;
        int b = mem.peekB(reg.getHl()) & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a - b - carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf) - carry) < 0);
        reg.setC((a - b - carry) < 0);

        nimo = "SBC A,(HL)";

        return insts[0x9e].cycle[0];
    }

    private int SBC_A_A() {
        int a = reg.a & 0xFF;
        int b = reg.a & 0xFF;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a - b - carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf) - carry) < 0);
        reg.setC((a - b - carry) < 0);

        nimo = "SBC A,A";

        return insts[0x9f].cycle[0];
    }

    // 0xa0
    private int AND_B() {
        reg.a = (byte) (reg.a & reg.b);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(true);
        reg.setC(false);

        nimo = "AND B";

        return insts[0xa0].cycle[0];
    }

    private int AND_C() {
        reg.a = (byte) (reg.a & reg.c);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(true);
        reg.setC(false);

        nimo = "AND C";

        return insts[0xa1].cycle[0];
    }

    private int AND_D() {
        reg.a = (byte) (reg.a & reg.d);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(true);
        reg.setC(false);

        nimo = "AND D";

        return insts[0xa2].cycle[0];
    }

    private int AND_E() {
        reg.a = (byte) (reg.a & reg.e);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(true);
        reg.setC(false);

        nimo = "AND E";

        return insts[0xa3].cycle[0];
    }

    private int AND_H() {
        reg.a = (byte) (reg.a & reg.h);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(true);
        reg.setC(false);

        nimo = "AND H";

        return insts[0xa4].cycle[0];
    }

    private int AND_L() {
        reg.a = (byte) (reg.a & reg.l);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(true);
        reg.setC(false);

        nimo = "AND L";

        return insts[0xa5].cycle[0];
    }

    private int AND_pHLs() {
        byte d = mem.peekB(reg.getHl());
        reg.a = (byte) (reg.a & d);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(true);
        reg.setC(false);

        nimo = "AND (HL)";

        return insts[0xa6].cycle[0];
    }

    private int AND_A() {
        reg.a = (byte) (reg.a & reg.a);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(true);
        reg.setC(false);

        nimo = "AND A";

        return insts[0xa7].cycle[0];
    }

    // 0xa8
    private int XOR_B() {
        reg.a = (byte) (reg.a ^ reg.b);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "XOR B";

        return insts[0xa8].cycle[0];
    }

    private int XOR_C() {
        reg.a = (byte) (reg.a ^ reg.c);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "XOR C";

        return insts[0xa9].cycle[0];
    }

    private int XOR_D() {
        reg.a = (byte) (reg.a ^ reg.d);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "XOR D";

        return insts[0xaa].cycle[0];
    }

    private int XOR_E() {
        reg.a = (byte) (reg.a ^ reg.e);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "XOR E";

        return insts[0xab].cycle[0];
    }

    private int XOR_H() {
        reg.a = (byte) (reg.a ^ reg.h);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "XOR H";

        return insts[0xac].cycle[0];
    }

    private int XOR_L() {
        reg.a = (byte) (reg.a ^ reg.l);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "XOR L";

        return insts[0xad].cycle[0];
    }

    private int XOR_pHLs() {
        byte d = mem.peekB(reg.getHl());
        reg.a = (byte) (reg.a ^ d);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "XOR (HL)";

        return insts[0xae].cycle[0];
    }

    private int XOR_A() {
        reg.a = (byte) (reg.a ^ reg.a);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "XOR A";

        return insts[0xaf].cycle[0];
    }

    // 0xb0
    private int OR_B() {
        reg.a = (byte) (reg.a | reg.b);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "OR B";

        return insts[0xb0].cycle[0];
    }

    private int OR_C() {
        reg.a = (byte) (reg.a | reg.c);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "OR C";

        return insts[0xb1].cycle[0];
    }

    private int OR_D() {
        reg.a = (byte) (reg.a | reg.d);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "OR D";

        return insts[0xb2].cycle[0];
    }

    private int OR_E() {
        reg.a = (byte) (reg.a | reg.e);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "OR E";

        return insts[0xb3].cycle[0];
    }

    private int OR_H() {
        reg.a = (byte) (reg.a | reg.h);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "OR H";

        return insts[0xb4].cycle[0];
    }

    private int OR_L() {
        reg.a = (byte) (reg.a | reg.l);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "OR L";

        return insts[0xb5].cycle[0];
    }

    private int OR_pHLs() {
        byte d = mem.peekB(reg.getHl());
        reg.a = (byte) (reg.a | d);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "OR (HL)";

        return insts[0xb6].cycle[0];
    }

    private int OR_A() {
        reg.a = (byte) (reg.a | reg.a);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "OR A";

        return insts[0xb7].cycle[0];
    }

    // 0xb8
    private int CP_B() {
        int a = reg.b & 0xFF;
        int b = (reg.a & 0xFF) - a;
        int h = (reg.a & 0xf) - (a & 0xf);

        reg.setZ(b == 0);
        reg.setS(true);
        reg.setH((h & 0xf0) != 0);
        reg.setC((b & 0xf00) != 0);

        nimo = "CP B";

        return insts[0xb8].cycle[0];
    }

    private int CP_C() {
        int a = reg.c & 0xFF;
        int b = (reg.a & 0xFF) - a;
        int h = (reg.a & 0xf) - (a & 0xf);

        reg.setZ(b == 0);
        reg.setS(true);
        reg.setH((h & 0xf0) != 0);
        reg.setC((b & 0xf00) != 0);

        nimo = "CP C";

        return insts[0xb9].cycle[0];
    }

    private int CP_D() {
        int a = reg.d & 0xFF;
        int b = (reg.a & 0xFF) - a;
        int h = (reg.a & 0xf) - (a & 0xf);

        reg.setZ(b == 0);
        reg.setS(true);
        reg.setH((h & 0xf0) != 0);
        reg.setC((b & 0xf00) != 0);

        nimo = "CP D";

        return insts[0xba].cycle[0];
    }

    private int CP_E() {
        int a = reg.e & 0xFF;
        int b = (reg.a & 0xFF) - a;
        int h = (reg.a & 0xf) - (a & 0xf);

        reg.setZ(b == 0);
        reg.setS(true);
        reg.setH((h & 0xf0) != 0);
        reg.setC((b & 0xf00) != 0);

        nimo = "CP E";

        return insts[0xbb].cycle[0];
    }

    private int CP_H() {
        int a = reg.h & 0xFF;
        int b = (reg.a & 0xFF) - a;
        int h = (reg.a & 0xf) - (a & 0xf);

        reg.setZ(b == 0);
        reg.setS(true);
        reg.setH((h & 0xf0) != 0);
        reg.setC((b & 0xf00) != 0);

        nimo = "CP H";

        return insts[0xbc].cycle[0];
    }

    private int CP_L() {
        int a = reg.l & 0xFF;
        int b = (reg.a & 0xFF) - a;
        int h = (reg.a & 0xf) - (a & 0xf);

        reg.setZ(b == 0);
        reg.setS(true);
        reg.setH((h & 0xf0) != 0);
        reg.setC((b & 0xf00) != 0);

        nimo = "CP L";

        return insts[0xbd].cycle[0];
    }

    private int CP_pHLs() {
        int a = mem.peekB(reg.getHl()) & 0xFF;
        int b = (reg.a & 0xFF) - a;
        int h = (reg.a & 0xf) - (a & 0xf);

        reg.setZ(b == 0);
        reg.setS(true);
        reg.setH((h & 0xf0) != 0);
        reg.setC((b & 0xf00) != 0);

        nimo = "CP (HL)";

        return insts[0xbe].cycle[0];
    }

    private int CP_A() {
        int a = reg.a & 0xFF;
        int b = (reg.a & 0xFF) - a;
        int h = (reg.a & 0xf) - (a & 0xf);

        reg.setZ(b == 0);
        reg.setS(true);
        reg.setH((h & 0xf0) != 0);
        reg.setC((b & 0xf00) != 0);

        nimo = "CP A";

        return insts[0xbf].cycle[0];
    }

    // 0xc0
    private int RET_NZ() {
        int c = insts[0xc0].cycle[1];
        if (!reg.isZ()) {
            reg.pc = pop();
            c = insts[0xc0].cycle[0];
        }

        nimo = "RET NZ";

        return c;
    }

    private int POP_BC() {
        reg.setBc(pop());

        nimo = "POP BC";

        return insts[0xc1].cycle[0];
    }

    private int JP_NZ_a16() {
        int d = mem.peekW(reg.pc);
        reg.pc += 2;
        reg.pc &= 0xFFFF;
        int c = insts[0xc2].cycle[1];
        if (!reg.isZ()) {
            reg.pc = d;
            c = insts[0xc2].cycle[0];
        }

        nimo = "JP NZ,$%04X".formatted(d & 0xffff);

        return c;
    }

    private int JP_a16() {
        int d = mem.peekW(reg.pc);
        reg.pc = d;

        nimo = "JP $%04X".formatted(d & 0xffff);

        return insts[0xc3].cycle[0];
    }

    private int CALL_NZ_a16() {
        int d = mem.peekW(reg.pc);
        int cycle = 0;
        if (!reg.isZ()) {
            push((reg.pc + 2) & 0xFFFF);
            reg.pc = d;
        } else {
            reg.pc += 2;
            reg.pc &= 0xFFFF;
            cycle = 1;
        }

        nimo = "CALL NZ,$%04X".formatted(d & 0xffff);

        return insts[0xc4].cycle[cycle];
    }

    private int PUSH_BC() {
        push(reg.getBc());

        nimo = "PUSH BC";

        return insts[0xc5].cycle[0];
    }

    private int ADD_A_d8() {
        int a = reg.a & 0xFF;
        int b = mem.peekB(reg.pc) & 0xFF;
        reg.a = (byte) ((a + b) & 0xFF);
        reg.pc++;
        reg.pc &= 0xffff;

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf)) > 0xf);
        reg.setC((a + b) > 0xff);

        nimo = "ADD A,$%02X".formatted(b & 0xff);

        return insts[0xc6].cycle[0];
    }

    private int RST_00H() {
        push(reg.pc);
        reg.pc = 0x00;

        nimo = "RST $00";

        return insts[0xc7].cycle[0];
    }

    // 0xc8
    private int RET_Z() {
        int c = insts[0xc8].cycle[1];
        if (reg.isZ()) {
            reg.pc = pop();
            c = insts[0xc8].cycle[0];
        }

        nimo = "RET Z";

        return c;
    }

    private int RET() {
        reg.pc = pop();

        nimo = "RET";

        return insts[0xc9].cycle[0];
    }

    private int JP_Z_a16() {
        int d = mem.peekW(reg.pc);
        reg.pc += 2;
        reg.pc &= 0xFFFF;
        int c = insts[0xca].cycle[1];
        if (reg.isZ()) {
            reg.pc = d;
            c = insts[0xca].cycle[0];
        }

        nimo = "JP Z,$%04X".formatted(d & 0xffff);

        return c;
    }

    private int PREFIX_CB() {
        cbSwitch = true;

        nimo = "PREFIX CB";

        return insts[0xcb].cycle[0];
    }

    private int CALL_Z_a16() {
        int d = mem.peekW(reg.pc);
        int cycle = 0;
        if (reg.isZ()) {
            push((reg.pc + 2) & 0xFFFF);
            reg.pc = d;
        } else {
            reg.pc += 2;
            reg.pc &= 0xFFFF;
            cycle = 1;
        }

        nimo = "CALL Z,$%04X".formatted(d);

        return insts[0xcc].cycle[cycle];
    }

    private int CALL_a16() {
        push((reg.pc + 2) & 0xFFFF);
        reg.pc = mem.peekW(reg.pc);

        nimo = "CALL $%04X".formatted(reg.pc & 0xffff);

        return insts[0xcd].cycle[0];
    }

    private int ADC_A_d8() {
        int a = reg.a & 0xFF;
        int b = mem.peekB(reg.pc) & 0xFF;
        reg.pc++;
        reg.pc &= 0xffff;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a + b + carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(((a & 0xf) + (b & 0xf) + carry) > 0xf);
        reg.setC((a + b + carry) > 0xff);

        nimo = "ADC A,$%02X".formatted(b & 0xff);

        return insts[0xce].cycle[0];
    }

    private int RST_08H() {
        push(reg.pc);
        reg.pc = 0x08;

        nimo = "RST $08";

        return insts[0xcf].cycle[0];
    }

    // 0xd0
    private int RET_NC() {
        int c = insts[0xd0].cycle[1];
        if (!reg.isC()) {
            reg.pc = pop();
            c = insts[0xd0].cycle[0];
        }

        nimo = "RET NC";

        return c;
    }

    private int POP_DE() {
        reg.setDe(pop());

        nimo = "POP DE";

        return insts[0xd1].cycle[0];
    }

    private int JP_NC_a16() {
        int d = mem.peekW(reg.pc);
        reg.pc += 2;
        reg.pc &= 0xFFFF;
        int c = insts[0xd2].cycle[1];
        if (!reg.isC()) {
            reg.pc = d;
            c = insts[0xd2].cycle[0];
        }

        nimo = "JP NC,$%04X".formatted(d & 0xffff);

        return c;
    }

    // 0xd3 is not present (no op)

    private int CALL_NC_a16() {
        int d = mem.peekW(reg.pc);
        int cycle = 0;
        if (!reg.isC()) {
            push((reg.pc + 2) & 0xFFFF);
            reg.pc = d;
        } else {
            reg.pc += 2;
            reg.pc &= 0xFFFF;
            cycle = 1;
        }

        nimo = "CALL NC,$%04X".formatted(d & 0xffff);

        return insts[0xd4].cycle[cycle];
    }

    private int PUSH_DE() {
        push(reg.getDe());

        nimo = "PUSH DE";

        return insts[0xd5].cycle[0];
    }

    private int SUB_d8() {
        int a = reg.a & 0xFF;
        int b = mem.peekB(reg.pc) & 0xFF;
        reg.a = (byte) ((a - b) & 0xFF);
        reg.pc++;
        reg.pc &= 0xffff;

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf)) < 0);
        reg.setC((a - b) < 0);

        nimo = "SUB $%02X".formatted(b & 0xff);

        return insts[0xd6].cycle[0];
    }

    private int RST_10H() {
        push(reg.pc);
        reg.pc = 0x10;

        nimo = "RST $10";

        return insts[0xd7].cycle[0];
    }

    // 0xd8
    private int RET_C() {
        int c = insts[0xd8].cycle[1];
        if (reg.isC()) {
            reg.pc = pop();
            c = insts[0xd8].cycle[0];
        }

        nimo = "RET C";

        return c;
    }

    private int RETI() {
        int c = insts[0xd9].cycle[0];
        reg.pc = pop();
        ime = 1;

        nimo = "RETI";

        return c;
    }

    private int JP_C_a16() {
        int d = mem.peekW(reg.pc);
        reg.pc += 2;
        reg.pc &= 0xFFFF;
        int c = insts[0xda].cycle[1];
        if (reg.isC()) {
            reg.pc = d;
            c = insts[0xda].cycle[0];
        }

        nimo = "JP C,$%04X".formatted(d & 0xffff);

        return c;
    }

    // 0xdb nasi (no op)

    private int CALL_C_a16() {
        int cycle = 0;
        int d = mem.peekW(reg.pc);

        if (reg.isC()) {
            push((reg.pc + 2) & 0xFFFF);
            reg.pc = d;
        } else {
            reg.pc += 2;
            reg.pc &= 0xFFFF;
            cycle = 1;
        }

        nimo = "CALL C,$%04X".formatted(d);

        return insts[0xdc].cycle[cycle];
    }

    private int SBC_A_d8() {
        int a = reg.a & 0xFF;
        int b = mem.peekB(reg.pc) & 0xFF;
        reg.pc++;
        reg.pc &= 0xffff;
        int carry = reg.isC() ? 1 : 0;
        reg.a = (byte) ((a - b - carry) & 0xFF);

        reg.setZ(reg.a == 0);
        reg.setS(true);
        reg.setH(((a & 0xf) - (b & 0xf) - carry) < 0);
        reg.setC((a - b - carry) < 0);

        nimo = "SBC A,$%02X".formatted(b & 0xff);

        return insts[0xde].cycle[0];
    }

    private int RST_18H() {
        push(reg.pc);
        reg.pc = 0x18;

        nimo = "RST $18";

        return insts[0xdf].cycle[0];
    }

    // 0xe0
    private int LDH_pa8s_A() {
        byte p = mem.peekB(reg.pc++);
        reg.pc &= 0xffff;
        mem.pokeB(0xff00 + (p & 0xFF), reg.a);

        nimo = "LD ($FF00+$%02X),A".formatted(p & 0xff);

        return insts[0xe0].cycle[0];
    }

    private int POP_HL() {
        reg.setHl(pop());

        nimo = "POP HL";

        return insts[0xe1].cycle[0];
    }

    private int LD_pCs_A() {
        mem.pokeB(0xff00 + (reg.c & 0xFF), reg.a);

        nimo = "LD ($ff00+C),A";

        return insts[0xe2].cycle[0];
    }

    private int PUSH_HL() {
        push(reg.getHl());

        nimo = "PUSH HL";

        return insts[0xe5].cycle[0];
    }

    private int AND_d8() {
        byte d = mem.peekB(reg.pc++);
        reg.pc &= 0xffff;
        reg.a = (byte) (reg.a & d);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(true);
        reg.setC(false);

        nimo = "AND $%02X".formatted(d & 0xffff);

        return insts[0xe6].cycle[0];
    }

    private int RST_20H() {
        push(reg.pc);
        reg.pc = 0x20;

        nimo = "RST $20";

        return insts[0xe7].cycle[0];
    }

    // 0xe8
    private int ADD_SP_r8() {
        int sp = reg.sp;
        byte b = mem.peekB(reg.pc);
        int r8 = b & 0xFF;
        int offset = b;

        reg.sp = (sp + offset) & 0xFFFF;
        reg.pc++;
        reg.pc &= 0xffff;

        reg.setZ(false);
        reg.setS(false);
        reg.setH(((sp & 0x0F) + (r8 & 0x0F)) > 0x0F);
        reg.setC(((sp & 0xFF) + (r8 & 0xFF)) > 0xFF);

        nimo = "ADD SP,$%02X".formatted(b & 0xff);

        return insts[0xe8].cycle[0];
    }

    private int JP_pHLs() {
        int d = reg.getHl();
        reg.pc = d;

        nimo = "JP HL";

        return insts[0xe9].cycle[0];
    }

    private int LD_pa16s_A() {
        int d = mem.peekW(reg.pc);
        reg.pc += 2;
        reg.pc &= 0xFFFF;
        mem.pokeB(d, reg.a);

        nimo = "LD ($%04X),A".formatted(d & 0xffff);

        return insts[0xea].cycle[0];
    }

    private int XOR_d8() {
        byte a = mem.peekB(reg.pc);
        reg.pc++;
        reg.pc &= 0xffff;
        reg.a = (byte) (reg.a ^ a);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "XOR $%02X".formatted(a & 0xff);

        return insts[0xee].cycle[0];
    }

    private int RST_28H() {
        push(reg.pc);
        reg.pc = 0x28;

        nimo = "RST $28";

        return insts[0xef].cycle[0];
    }

    // 0xf0
    private int LDH_A_pa8s() {
        byte p = mem.peekB(reg.pc++);
        reg.pc &= 0xffff;
        reg.a = mem.peekB(0xff00 + (p & 0xFF));

        nimo = "LD A,($FF00+$%02X)".formatted(p & 0xff);

        return insts[0xf0].cycle[0];
    }

    private int POP_AF() {
        reg.setAf(pop());
        reg.setAf(reg.getAf() & 0xfff0);

        nimo = "POP AF";

        return insts[0xf1].cycle[0];
    }

    private int LD_A_pCs() {
        reg.a = mem.peekB(0xff00 + (reg.c & 0xFF));

        nimo = "LD A,($ff00+C)";

        return insts[0xf2].cycle[0];
    }

    private int DI() {
        ime = 0;

        nimo = "DI";

        return insts[0xf3].cycle[0];
    }

    private int PUSH_AF() {
        push(reg.getAf());

        nimo = "PUSH AF";

        return insts[0xf5].cycle[0];
    }

    private int OR_d8() {
        byte d = mem.peekB(reg.pc++);
        reg.pc &= 0xffff;
        reg.a = (byte) (reg.a | d);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "OR $%02X".formatted(d & 0xffff);

        return insts[0xf6].cycle[0];
    }

    private int RST_30H() {
        push(reg.pc);
        reg.pc = 0x30;

        nimo = "RST $30";

        return insts[0xf7].cycle[0];
    }

    // 0xf8
    private int LD_HL_SPplsr8() {
        int sp = reg.sp;
        byte b = mem.peekB(reg.pc);
        int r8 = b & 0xFF;
        int offset = b;

        reg.setHl((sp + offset) & 0xFFFF);
        reg.pc++;
        reg.pc &= 0xFFFF;

        reg.setZ(false);
        reg.setS(false);
        reg.setH(((sp & 0x0F) + (r8 & 0x0F)) > 0x0F);
        reg.setC(((sp & 0xFF) + (r8 & 0xFF)) > 0xFF);

        nimo = "LD HL,SP+$%02X".formatted(b & 0xff);

        return insts[0xf8].cycle[0];
    }

    private int LD_SP_HL() {
        reg.sp = reg.getHl();

        nimo = "LD SP,HL";

        return insts[0xf9].cycle[0];
    }

    private int LD_A_pa16s() {
        int d = mem.peekW(reg.pc);
        reg.pc += 2;
        reg.pc &= 0xFFFF;
        reg.a = mem.peekB(d);

        nimo = "LD A,($%04X)".formatted(d & 0xffff);

        return insts[0xfa].cycle[0];
    }

    private int EI() {
        ime = 1;

        nimo = "EI";

        return insts[0xfb].cycle[0];
    }

    private int CP_d8() {
        int a = mem.peekB(reg.pc) & 0xFF;
        int b = (reg.a & 0xFF) - a;
        int h = (reg.a & 0xf) - (a & 0xf);

        reg.pc++;
        reg.pc &= 0xffff;

        reg.setZ(b == 0);
        reg.setS(true);
        reg.setH((h & 0xf0) != 0);
        reg.setC((b & 0xf00) != 0);

        nimo = "CP $%02X".formatted(a);

        return insts[0xfe].cycle[0];
    }

    private int RST_38H() {
        push(reg.pc);
        reg.pc = 0x38;

        nimo = "RST $38";

        return insts[0xff].cycle[0];
    }

    // 0x00 (CB) - 0x100
    private int RLC_B() {
        byte d = reg.b;
        reg.setC((d & 0x80) != 0);
        d = (byte)((d << 1) | (reg.isC() ? 1 : 0));
        reg.b = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RLC B";

        return insts[0x100].cycle[0];
    }

    private int RLC_C() {
        byte d = reg.c;
        reg.setC((d & 0x80) != 0);
        d = (byte)((d << 1) | (reg.isC() ? 1 : 0));
        reg.c = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RLC C";

        return insts[0x101].cycle[0];
    }

    private int RLC_D() {
        byte d = reg.d;
        reg.setC((d & 0x80) != 0);
        d = (byte)((d << 1) | (reg.isC() ? 1 : 0));
        reg.d = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RLC D";

        return insts[0x102].cycle[0];
    }

    private int RLC_E() {
        byte d = reg.e;
        reg.setC((d & 0x80) != 0);
        d = (byte)((d << 1) | (reg.isC() ? 1 : 0));
        reg.e = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RLC E";

        return insts[0x103].cycle[0];
    }

    private int RLC_H() {
        byte d = reg.h;
        reg.setC((d & 0x80) != 0);
        d = (byte)((d << 1) | (reg.isC() ? 1 : 0));
        reg.h = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RLC H";

        return insts[0x104].cycle[0];
    }

    private int RLC_L() {
        byte d = reg.l;
        reg.setC((d & 0x80) != 0);
        d = (byte)((d << 1) | (reg.isC() ? 1 : 0));
        reg.l = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RLC L";

        return insts[0x105].cycle[0];
    }

    private int RLC_pHLs() {
        byte d = mem.peekB(reg.getHl());
        reg.setC((d & 0x80) != 0);
        d = (byte)((d << 1) | (reg.isC() ? 1 : 0));
        mem.pokeB(reg.getHl(), d);

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RLC (HL)";

        return insts[0x106].cycle[0];
    }

    private int RLC_A() {
        byte d = reg.a;
        reg.setC((d & 0x80) != 0);
        d = (byte)((d << 1) | (reg.isC() ? 1 : 0));
        reg.a = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RLC A";

        return insts[0x107].cycle[0];
    }

    // 0x08 (CB) - 0x108
    private int RRC_B() {
        byte d = reg.b;
        reg.setC((d & 0x01) != 0);
        d = (byte)(((d & 0xFF) >>> 1) | (reg.isC() ? 0x80 : 0));
        reg.b = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RRC B";

        return insts[0x108].cycle[0];
    }

    private int RRC_C() {
        byte d = reg.c;
        reg.setC((d & 0x01) != 0);
        d = (byte)(((d & 0xFF) >>> 1) | (reg.isC() ? 0x80 : 0));
        reg.c = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RRC C";

        return insts[0x109].cycle[0];
    }

    private int RRC_D() {
        byte d = reg.d;
        reg.setC((d & 0x01) != 0);
        d = (byte)(((d & 0xFF) >>> 1) | (reg.isC() ? 0x80 : 0));
        reg.d = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RRC D";

        return insts[0x10a].cycle[0];
    }

    private int RRC_E() {
        byte d = reg.e;
        reg.setC((d & 0x01) != 0);
        d = (byte)(((d & 0xFF) >>> 1) | (reg.isC() ? 0x80 : 0));
        reg.e = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RRC E";

        return insts[0x10b].cycle[0];
    }

    private int RRC_H() {
        byte d = reg.h;
        reg.setC((d & 0x01) != 0);
        d = (byte)(((d & 0xFF) >>> 1) | (reg.isC() ? 0x80 : 0));
        reg.h = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RRC H";

        return insts[0x10c].cycle[0];
    }

    private int RRC_L() {
        byte d = reg.l;
        reg.setC((d & 0x01) != 0);
        d = (byte)(((d & 0xFF) >>> 1) | (reg.isC() ? 0x80 : 0));
        reg.l = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RRC L";

        return insts[0x10d].cycle[0];
    }

    private int RRC_pHLs() {
        byte d = mem.peekB(reg.getHl());
        reg.setC((d & 0x01) != 0);
        d = (byte)(((d & 0xFF) >>> 1) | (reg.isC() ? 0x80 : 0));
        mem.pokeB(reg.getHl(), d);

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RRC (HL)";

        return insts[0x10e].cycle[0];
    }

    private int RRC_A() {
        byte d = reg.a;
        reg.setC((d & 0x01) != 0);
        d = (byte)(((d & 0xFF) >>> 1) | (reg.isC() ? 0x80 : 0));
        reg.a = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RRC A";

        return insts[0x10f].cycle[0];
    }

    // 0x10 (CB) - 0x110
    private int RL_B() {
        byte d = reg.b;
        byte e = (byte)(reg.isC() ? 0x01 : 0);
        reg.setC((d & 0x80) != 0);

        d = (byte)((d << 1) | e);
        reg.b = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RL B";

        return insts[0x110].cycle[0];
    }

    private int RL_C() {
        byte d = reg.c;
        byte e = (byte)(reg.isC() ? 0x01 : 0);
        reg.setC((d & 0x80) != 0);

        d = (byte)((d << 1) | e);
        reg.c = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RL C";

        return insts[0x111].cycle[0];
    }

    private int RL_D() {
        byte d = reg.d;
        byte e = (byte)(reg.isC() ? 0x01 : 0);
        reg.setC((d & 0x80) != 0);

        d = (byte)((d << 1) | e);
        reg.d = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RL D";

        return insts[0x112].cycle[0];
    }

    private int RL_E() {
        byte d = reg.e;
        byte e = (byte)(reg.isC() ? 0x01 : 0);
        reg.setC((d & 0x80) != 0);

        d = (byte)((d << 1) | e);
        reg.e = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RL E";

        return insts[0x113].cycle[0];
    }

    private int RL_H() {
        byte d = reg.h;
        byte e = (byte)(reg.isC() ? 0x01 : 0);
        reg.setC((d & 0x80) != 0);

        d = (byte)((d << 1) | e);
        reg.h = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RL H";

        return insts[0x114].cycle[0];
    }

    private int RL_L() {
        byte d = reg.l;
        byte e = (byte)(reg.isC() ? 0x01 : 0);
        reg.setC((d & 0x80) != 0);

        d = (byte)((d << 1) | e);
        reg.l = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RL L";

        return insts[0x115].cycle[0];
    }

    private int RL_pHLs() {
        byte d = mem.peekB(reg.getHl());
        byte e = (byte)(reg.isC() ? 0x01 : 0);
        reg.setC((d & 0x80) != 0);

        d = (byte)((d << 1) | e);
        mem.pokeB(reg.getHl(), d);

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RL (HL)";

        return insts[0x116].cycle[0];
    }

    private int RL_A() {
        byte d = reg.a;
        byte e = (byte)(reg.isC() ? 0x01 : 0);
        reg.setC((d & 0x80) != 0);

        d = (byte)((d << 1) | e);
        reg.a = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RL A";

        return insts[0x117].cycle[0];
    }

    // 0x18 (CB) - 0x118
    private int RR_B() {
        byte d = reg.b;
        byte e = (byte)(reg.isC() ? 0x80 : 0);
        reg.setC((d & 0x01) != 0);

        d = (byte)(((d & 0xFF) >>> 1) | e);
        reg.b = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RR B";

        return insts[0x118].cycle[0];
    }

    private int RR_C() {
        byte d = reg.c;
        byte e = (byte)(reg.isC() ? 0x80 : 0);
        reg.setC((d & 0x01) != 0);

        d = (byte)(((d & 0xFF) >>> 1) | e);
        reg.c = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RR C";

        return insts[0x119].cycle[0];
    }

    private int RR_D() {
        byte d = reg.d;
        byte e = (byte)(reg.isC() ? 0x80 : 0);
        reg.setC((d & 0x01) != 0);

        d = (byte)(((d & 0xFF) >>> 1) | e);
        reg.d = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RR D";

        return insts[0x11a].cycle[0];
    }

    private int RR_E() {
        byte d = reg.e;
        byte e = (byte)(reg.isC() ? 0x80 : 0);
        reg.setC((d & 0x01) != 0);

        d = (byte)(((d & 0xFF) >>> 1) | e);
        reg.e = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RR E";

        return insts[0x11b].cycle[0];
    }

    private int RR_H() {
        byte d = reg.h;
        byte e = (byte)(reg.isC() ? 0x80 : 0);
        reg.setC((d & 0x01) != 0);

        d = (byte)(((d & 0xFF) >>> 1) | e);
        reg.h = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RR H";

        return insts[0x11c].cycle[0];
    }

    private int RR_L() {
        byte d = reg.l;
        byte e = (byte)(reg.isC() ? 0x80 : 0);
        reg.setC((d & 0x01) != 0);

        d = (byte)(((d & 0xFF) >>> 1) | e);
        reg.l = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RR L";

        return insts[0x11d].cycle[0];
    }

    private int RR_pHLs() {
        byte d = mem.peekB(reg.getHl());
        byte e = (byte)(reg.isC() ? 0x80 : 0);
        reg.setC((d & 0x01) != 0);

        d = (byte)(((d & 0xFF) >>> 1) | e);
        mem.pokeB(reg.getHl(), d);

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RR (HL)";

        return insts[0x11e].cycle[0];
    }

    private int RR_A() {
        byte d = reg.a;
        byte e = (byte)(reg.isC() ? 0x80 : 0);
        reg.setC((d & 0x01) != 0);

        d = (byte)(((d & 0xFF) >>> 1) | e);
        reg.a = d;

        reg.setZ(d == 0);
        reg.setS(false);
        reg.setH(false);

        nimo = "RR A";

        return insts[0x11f].cycle[0];
    }

    // 0x20 (CB) - 0x120
    private int SLA_B() {
        byte a = reg.b;
        reg.b = (byte)(a << 1);

        reg.setZ(reg.b == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x80) != 0);

        nimo = "SLA B";

        return insts[0x120].cycle[0];
    }

    private int SLA_C() {
        byte a = reg.c;
        reg.c = (byte)(a << 1);

        reg.setZ(reg.c == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x80) != 0);

        nimo = "SLA C";

        return insts[0x121].cycle[0];
    }

    private int SLA_D() {
        byte a = reg.d;
        reg.d = (byte)(a << 1);

        reg.setZ(reg.d == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x80) != 0);

        nimo = "SLA D";

        return insts[0x122].cycle[0];
    }

    private int SLA_E() {
        byte a = reg.e;
        reg.e = (byte)(a << 1);

        reg.setZ(reg.e == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x80) != 0);

        nimo = "SLA E";

        return insts[0x123].cycle[0];
    }

    private int SLA_H() {
        byte a = reg.h;
        reg.h = (byte)(a << 1);

        reg.setZ(reg.h == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x80) != 0);

        nimo = "SLA H";

        return insts[0x124].cycle[0];
    }

    private int SLA_L() {
        byte a = reg.l;
        reg.l = (byte)(a << 1);

        reg.setZ(reg.l == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x80) != 0);

        nimo = "SLA L";

        return insts[0x125].cycle[0];
    }

    private int SLA_pHLs() {
        byte a = mem.peekB(reg.getHl());
        byte b = (byte)(a << 1);
        mem.pokeB(reg.getHl(), b);

        reg.setZ(b == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x80) != 0);

        nimo = "SLA (HL)";

        return insts[0x126].cycle[0];
    }

    private int SLA_A() {
        byte a = reg.a;
        reg.a = (byte)(a << 1);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x80) != 0);

        nimo = "SLA A";

        return insts[0x127].cycle[0];
    }

    // 0x28 (CB) - 0x128
    private int SRA_B() {
        byte a = reg.b;
        reg.b = (byte)(((a & 0xFF) >>> 1) | (a & 0x80));

        reg.setZ(reg.b == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRA B";

        return insts[0x128].cycle[0];
    }

    private int SRA_C() {
        byte a = reg.c;
        reg.c = (byte)(((a & 0xFF) >>> 1) | (a & 0x80));

        reg.setZ(reg.c == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRA C";

        return insts[0x129].cycle[0];
    }

    private int SRA_D() {
        byte a = reg.d;
        reg.d = (byte)(((a & 0xFF) >>> 1) | (a & 0x80));

        reg.setZ(reg.d == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRA D";

        return insts[0x12a].cycle[0];
    }

    private int SRA_E() {
        byte a = reg.e;
        reg.e = (byte)(((a & 0xFF) >>> 1) | (a & 0x80));

        reg.setZ(reg.e == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRA E";

        return insts[0x12b].cycle[0];
    }

    private int SRA_H() {
        byte a = reg.h;
        reg.h = (byte)(((a & 0xFF) >>> 1) | (a & 0x80));

        reg.setZ(reg.h == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRA H";

        return insts[0x12c].cycle[0];
    }

    private int SRA_L() {
        byte a = reg.l;
        reg.l = (byte)(((a & 0xFF) >>> 1) | (a & 0x80));

        reg.setZ(reg.l == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRA L";

        return insts[0x12d].cycle[0];
    }

    private int SRA_pHLs() {
        byte a = mem.peekB(reg.getHl());
        byte b = (byte)(((a & 0xFF) >>> 1) | (a & 0x80));
        mem.pokeB(reg.getHl(), b);

        reg.setZ(b == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRA (HL)";

        return insts[0x12e].cycle[0];
    }

    private int SRA_A() {
        byte a = reg.a;
        reg.a = (byte)(((a & 0xFF) >>> 1) | (a & 0x80));

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRA A";

        return insts[0x12f].cycle[0];
    }

    // 0x30 (CB) - 0x130
    private int SWAP_B() {
        byte a = reg.b;
        reg.b = (byte)((((a & 0xFF) >>> 4) & 0xf) | ((a << 4) & 0xf0));
        reg.setZ(reg.b == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "SWAP B";

        return insts[0x130].cycle[0];
    }

    private int SWAP_C() {
        byte a = reg.c;
        reg.c = (byte)((((a & 0xFF) >>> 4) & 0xf) | ((a << 4) & 0xf0));
        reg.setZ(reg.c == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "SWAP C";

        return insts[0x131].cycle[0];
    }

    private int SWAP_D() {
        byte a = reg.d;
        reg.d = (byte)((((a & 0xFF) >>> 4) & 0xf) | ((a << 4) & 0xf0));
        reg.setZ(reg.d == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "SWAP D";

        return insts[0x132].cycle[0];
    }

    private int SWAP_E() {
        byte a = reg.e;
        reg.e = (byte)((((a & 0xFF) >>> 4) & 0xf) | ((a << 4) & 0xf0));
        reg.setZ(reg.e == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "SWAP E";

        return insts[0x133].cycle[0];
    }

    private int SWAP_H() {
        byte a = reg.h;
        reg.h = (byte)((((a & 0xFF) >>> 4) & 0xf) | ((a << 4) & 0xf0));
        reg.setZ(reg.h == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "SWAP H";

        return insts[0x134].cycle[0];
    }

    private int SWAP_L() {
        byte a = reg.l;
        reg.l = (byte)((((a & 0xFF) >>> 4) & 0xf) | ((a << 4) & 0xf0));
        reg.setZ(reg.l == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "SWAP L";

        return insts[0x135].cycle[0];
    }

    private int SWAP_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a = (byte)((((a & 0xFF) >>> 4) & 0xf) | ((a << 4) & 0xf0));
        mem.pokeB(reg.getHl(), a);
        reg.setZ(a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "SWAP (HL)";

        return insts[0x136].cycle[0];
    }

    private int SWAP_A() {
        byte a = reg.a;
        reg.a = (byte)((((a & 0xFF) >>> 4) & 0xf) | ((a << 4) & 0xf0));
        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC(false);

        nimo = "SWAP A";

        return insts[0x137].cycle[0];
    }

    // 0x38 (CB) - 0x138
    private int SRL_B() {
        byte a = reg.b;
        reg.b = (byte)((a & 0xFF) >>> 1);

        reg.setZ(reg.b == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRL B";

        return insts[0x138].cycle[0];
    }

    private int SRL_C() {
        byte a = reg.c;
        reg.c = (byte)((a & 0xFF) >>> 1);

        reg.setZ(reg.c == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRL C";

        return insts[0x139].cycle[0];
    }

    private int SRL_D() {
        byte a = reg.d;
        reg.d = (byte)((a & 0xFF) >>> 1);

        reg.setZ(reg.d == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRL D";

        return insts[0x13a].cycle[0];
    }

    private int SRL_E() {
        byte a = reg.e;
        reg.e = (byte)((a & 0xFF) >>> 1);

        reg.setZ(reg.e == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRL E";

        return insts[0x13b].cycle[0];
    }

    private int SRL_H() {
        byte a = reg.h;
        reg.h = (byte)((a & 0xFF) >>> 1);

        reg.setZ(reg.h == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRL H";

        return insts[0x13c].cycle[0];
    }

    private int SRL_L() {
        byte a = reg.l;
        reg.l = (byte)((a & 0xFF) >>> 1);

        reg.setZ(reg.l == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRL L";

        return insts[0x13d].cycle[0];
    }

    private int SRL_pHLs() {
        byte a = mem.peekB(reg.getHl());
        byte b = (byte)((a & 0xFF) >>> 1);
        mem.pokeB(reg.getHl(), b);

        reg.setZ(b == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRL (HL)";

        return insts[0x13e].cycle[0];
    }

    private int SRL_A() {
        byte a = reg.a;
        reg.a = (byte)((a & 0xFF) >>> 1);

        reg.setZ(reg.a == 0);
        reg.setS(false);
        reg.setH(false);
        reg.setC((a & 0x01) != 0);

        nimo = "SRL A";

        return insts[0x13f].cycle[0];
    }

    // 0x40 (CB) - 0x140
    private int BIT_0_B() {
        boolean a = (reg.b & 0x1) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 0,B";

        return insts[0x140].cycle[0];
    }

    private int BIT_0_C() {
        boolean a = (reg.c & 0x1) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 0,C";

        return insts[0x141].cycle[0];
    }

    private int BIT_0_D() {
        boolean a = (reg.d & 0x1) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 0,D";

        return insts[0x142].cycle[0];
    }

    private int BIT_0_E() {
        boolean a = (reg.e & 0x1) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 0,E";

        return insts[0x143].cycle[0];
    }

    private int BIT_0_H() {
        boolean a = (reg.h & 0x1) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 0,H";

        return insts[0x144].cycle[0];
    }

    private int BIT_0_L() {
        boolean a = (reg.l & 0x1) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 0,L";

        return insts[0x145].cycle[0];
    }

    private int BIT_0_pHLs() {
        boolean a = (mem.peekB(reg.getHl()) & 0x1) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 0,(HL)";

        return insts[0x146].cycle[0];
    }

    private int BIT_0_A() {
        boolean a = (reg.a & 0x1) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 0,A";

        return insts[0x147].cycle[0];
    }

    // 0x48 (CB) - 0x148
    private int BIT_1_B() {
        boolean a = (reg.b & 0x2) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 1,B";

        return insts[0x148].cycle[0];
    }

    private int BIT_1_C() {
        boolean a = (reg.c & 0x2) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 1,C";

        return insts[0x149].cycle[0];
    }

    private int BIT_1_D() {
        boolean a = (reg.d & 0x2) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 1,D";

        return insts[0x14a].cycle[0];
    }

    private int BIT_1_E() {
        boolean a = (reg.e & 0x2) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 1,E";

        return insts[0x14b].cycle[0];
    }

    private int BIT_1_H() {
        boolean a = (reg.h & 0x2) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 1,H";

        return insts[0x14c].cycle[0];
    }

    private int BIT_1_L() {
        boolean a = (reg.l & 0x2) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 1,L";

        return insts[0x14d].cycle[0];
    }

    private int BIT_1_pHLs() {
        boolean a = (mem.peekB(reg.getHl()) & 0x2) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 1,(HL)";

        return insts[0x14e].cycle[0];
    }

    private int BIT_1_A() {
        boolean a = (reg.a & 0x2) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 1,A";

        return insts[0x14f].cycle[0];
    }

    // 0x50 (CB) - 0x150
    private int BIT_2_B() {
        boolean a = (reg.b & 0x4) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 2,B";

        return insts[0x150].cycle[0];
    }

    private int BIT_2_C() {
        boolean a = (reg.c & 0x4) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 2,C";

        return insts[0x151].cycle[0];
    }

    private int BIT_2_D() {
        boolean a = (reg.d & 0x4) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 2,D";

        return insts[0x152].cycle[0];
    }

    private int BIT_2_E() {
        boolean a = (reg.e & 0x4) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 2,E";

        return insts[0x153].cycle[0];
    }

    private int BIT_2_H() {
        boolean a = (reg.h & 0x4) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 2,H";

        return insts[0x154].cycle[0];
    }

    private int BIT_2_L() {
        boolean a = (reg.l & 0x4) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 2,L";

        return insts[0x155].cycle[0];
    }

    private int BIT_2_pHLs() {
        boolean a = (mem.peekB(reg.getHl()) & 0x4) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 2,(HL)";

        return insts[0x156].cycle[0];
    }

    private int BIT_2_A() {
        boolean a = (reg.a & 0x4) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 2,A";

        return insts[0x157].cycle[0];
    }

    // 0x58 (CB) - 0x158
    private int BIT_3_B() {
        boolean a = (reg.b & 0x8) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 3,B";

        return insts[0x158].cycle[0];
    }

    private int BIT_3_C() {
        boolean a = (reg.c & 0x8) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 3,C";

        return insts[0x159].cycle[0];
    }

    private int BIT_3_D() {
        boolean a = (reg.d & 0x8) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 3,D";

        return insts[0x15a].cycle[0];
    }

    private int BIT_3_E() {
        boolean a = (reg.e & 0x8) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 3,E";

        return insts[0x15b].cycle[0];
    }

    private int BIT_3_H() {
        boolean a = (reg.h & 0x8) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 3,H";

        return insts[0x15c].cycle[0];
    }

    private int BIT_3_L() {
        boolean a = (reg.l & 0x8) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 3,L";

        return insts[0x15d].cycle[0];
    }

    private int BIT_3_pHLs() {
        boolean a = (mem.peekB(reg.getHl()) & 0x8) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 3,(HL)";

        return insts[0x15e].cycle[0];
    }

    private int BIT_3_A() {
        boolean a = (reg.a & 0x8) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 3,A";

        return insts[0x15f].cycle[0];
    }

    // 0x60 (CB) - 0x160
    private int BIT_4_B() {
        boolean a = (reg.b & 0x10) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 4,B";

        return insts[0x160].cycle[0];
    }

    private int BIT_4_C() {
        boolean a = (reg.c & 0x10) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 4,C";

        return insts[0x161].cycle[0];
    }

    private int BIT_4_D() {
        boolean a = (reg.d & 0x10) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 4,D";

        return insts[0x162].cycle[0];
    }

    private int BIT_4_E() {
        boolean a = (reg.e & 0x10) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 4,E";

        return insts[0x163].cycle[0];
    }

    private int BIT_4_H() {
        boolean a = (reg.h & 0x10) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 4,H";

        return insts[0x164].cycle[0];
    }

    private int BIT_4_L() {
        boolean a = (reg.l & 0x10) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 4,L";

        return insts[0x165].cycle[0];
    }

    private int BIT_4_pHLs() {
        boolean a = (mem.peekB(reg.getHl()) & 0x10) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 4,(HL)";

        return insts[0x166].cycle[0];
    }

    private int BIT_4_A() {
        boolean a = (reg.a & 0x10) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 4,A";

        return insts[0x167].cycle[0];
    }

    // 0x68 (CB) - 0x168
    private int BIT_5_B() {
        boolean a = (reg.b & 0x20) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 5,B";

        return insts[0x168].cycle[0];
    }

    private int BIT_5_C() {
        boolean a = (reg.c & 0x20) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 5,C";

        return insts[0x169].cycle[0];
    }

    private int BIT_5_D() {
        boolean a = (reg.d & 0x20) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 5,D";

        return insts[0x16a].cycle[0];
    }

    private int BIT_5_E() {
        boolean a = (reg.e & 0x20) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 5,E";

        return insts[0x16b].cycle[0];
    }

    private int BIT_5_H() {
        boolean a = (reg.h & 0x20) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 5,H";

        return insts[0x16c].cycle[0];
    }

    private int BIT_5_L() {
        boolean a = (reg.l & 0x20) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 5,L";

        return insts[0x16d].cycle[0];
    }

    private int BIT_5_pHLs() {
        boolean a = (mem.peekB(reg.getHl()) & 0x20) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 5,(HL)";

        return insts[0x16e].cycle[0];
    }

    private int BIT_5_A() {
        boolean a = (reg.a & 0x20) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 5,A";

        return insts[0x16f].cycle[0];
    }

    // 0x70 (CB) - 0x170
    private int BIT_6_B() {
        boolean a = (reg.b & 0x40) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 6,B";

        return insts[0x170].cycle[0];
    }

    private int BIT_6_C() {
        boolean a = (reg.c & 0x40) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 6,C";

        return insts[0x171].cycle[0];
    }

    private int BIT_6_D() {
        boolean a = (reg.d & 0x40) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 6,D";

        return insts[0x172].cycle[0];
    }

    private int BIT_6_E() {
        boolean a = (reg.e & 0x40) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 6,E";

        return insts[0x173].cycle[0];
    }

    private int BIT_6_H() {
        boolean a = (reg.h & 0x40) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 6,H";

        return insts[0x174].cycle[0];
    }

    private int BIT_6_L() {
        boolean a = (reg.l & 0x40) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 6,L";

        return insts[0x175].cycle[0];
    }

    private int BIT_6_pHLs() {
        byte b = mem.peekB(reg.getHl());
        boolean a = (b & 0x40) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 6,(HL)";

        return insts[0x176].cycle[0];
    }

    private int BIT_6_A() {
        boolean a = (reg.a & 0x40) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 6,A";

        return insts[0x177].cycle[0];
    }

    // 0x78 (CB) - 0x178
    private int BIT_7_B() {
        boolean a = (reg.b & 0x80) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 7,B";

        return insts[0x178].cycle[0];
    }

    private int BIT_7_C() {
        boolean a = (reg.c & 0x80) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 7,C";

        return insts[0x179].cycle[0];
    }

    private int BIT_7_D() {
        boolean a = (reg.d & 0x80) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 7,D";

        return insts[0x17a].cycle[0];
    }

    private int BIT_7_E() {
        boolean a = (reg.e & 0x80) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 7,E";

        return insts[0x17b].cycle[0];
    }

    private int BIT_7_H() {
        boolean a = (reg.h & 0x80) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 7,H";

        return insts[0x17c].cycle[0];
    }

    private int BIT_7_L() {
        boolean a = (reg.l & 0x80) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 7,L";

        return insts[0x17d].cycle[0];
    }

    private int BIT_7_pHLs() {
        byte b = mem.peekB(reg.getHl());
        boolean a = (b & 0x80) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 7,(HL)";

        return insts[0x17e].cycle[0];
    }

    private int BIT_7_A() {
        boolean a = (reg.a & 0x80) == 0;

        reg.setZ(a);
        reg.setS(false);
        reg.setH(true);

        nimo = "BIT 7,A";

        return insts[0x17f].cycle[0];
    }

    // 0x80 (CB) - 0x180
    private int RES_0_B() {
        reg.b &= (byte) ~(1 << 0);

        nimo = "RES 0,B";

        return insts[0x180].cycle[0];
    }

    private int RES_0_C() {
        reg.c &= (byte) ~(1 << 0);

        nimo = "RES 0,C";

        return insts[0x181].cycle[0];
    }

    private int RES_0_D() {
        reg.d &= (byte) ~(1 << 0);

        nimo = "RES 0,D";

        return insts[0x182].cycle[0];
    }

    private int RES_0_E() {
        reg.e &= (byte) ~(1 << 0);

        nimo = "RES 0,E";

        return insts[0x183].cycle[0];
    }

    private int RES_0_H() {
        reg.h &= (byte) ~(1 << 0);

        nimo = "RES 0,H";

        return insts[0x184].cycle[0];
    }

    private int RES_0_L() {
        reg.l &= (byte) ~(1 << 0);

        nimo = "RES 0,L";

        return insts[0x185].cycle[0];
    }

    private int RES_0_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a &= (byte) ~(1 << 0);
        mem.pokeB(reg.getHl(), a);

        nimo = "RES 0,(HL)";

        return insts[0x186].cycle[0];
    }

    private int RES_0_A() {
        reg.a &= (byte) ~(1 << 0);

        nimo = "RES 0,A";

        return insts[0x187].cycle[0];
    }

    // 0x88 (CB) - 0x188
    private int RES_1_B() {
        reg.b &= (byte) ~(1 << 1);

        nimo = "RES 1,B";

        return insts[0x188].cycle[0];
    }

    private int RES_1_C() {
        reg.c &= (byte) ~(1 << 1);

        nimo = "RES 1,C";

        return insts[0x189].cycle[0];
    }

    private int RES_1_D() {
        reg.d &= (byte) ~(1 << 1);

        nimo = "RES 1,D";

        return insts[0x18a].cycle[0];
    }

    private int RES_1_E() {
        reg.e &= (byte) ~(1 << 1);

        nimo = "RES 1,E";

        return insts[0x18b].cycle[0];
    }

    private int RES_1_H() {
        reg.h &= (byte) ~(1 << 1);

        nimo = "RES 1,H";

        return insts[0x18c].cycle[0];
    }

    private int RES_1_L() {
        reg.l &= (byte) ~(1 << 1);

        nimo = "RES 1,L";

        return insts[0x18d].cycle[0];
    }

    private int RES_1_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a &= (byte) ~(1 << 1);
        mem.pokeB(reg.getHl(), a);

        nimo = "RES 1,(HL)";

        return insts[0x18e].cycle[0];
    }

    private int RES_1_A() {
        reg.a &= (byte) ~(1 << 1);

        nimo = "RES 1,A";

        return insts[0x18f].cycle[0];
    }

    // 0x90 (CB) - 0x190
    private int RES_2_B() {
        reg.b &= (byte) ~(1 << 2);

        nimo = "RES 2,B";

        return insts[0x190].cycle[0];
    }

    private int RES_2_C() {
        reg.c &= (byte) ~(1 << 2);

        nimo = "RES 2,C";

        return insts[0x191].cycle[0];
    }

    private int RES_2_D() {
        reg.d &= (byte) ~(1 << 2);

        nimo = "RES 2,D";

        return insts[0x192].cycle[0];
    }

    private int RES_2_E() {
        reg.e &= (byte) ~(1 << 2);

        nimo = "RES 2,E";

        return insts[0x193].cycle[0];
    }

    private int RES_2_H() {
        reg.h &= (byte) ~(1 << 2);

        nimo = "RES 2,H";

        return insts[0x194].cycle[0];
    }

    private int RES_2_L() {
        reg.l &= (byte) ~(1 << 2);

        nimo = "RES 2,L";

        return insts[0x195].cycle[0];
    }

    private int RES_2_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a &= (byte) ~(1 << 2);
        mem.pokeB(reg.getHl(), a);

        nimo = "RES 2,(HL)";

        return insts[0x196].cycle[0];
    }

    private int RES_2_A() {
        reg.a &= (byte) ~(1 << 2);

        nimo = "RES 2,A";

        return insts[0x197].cycle[0];
    }

    // 0x98 (CB) - 0x198
    private int RES_3_B() {
        reg.b &= (byte) ~(1 << 3);

        nimo = "RES 3,B";

        return insts[0x198].cycle[0];
    }

    private int RES_3_C() {
        reg.c &= (byte) ~(1 << 3);

        nimo = "RES 3,C";

        return insts[0x199].cycle[0];
    }

    private int RES_3_D() {
        reg.d &= (byte) ~(1 << 3);

        nimo = "RES 3,D";

        return insts[0x19a].cycle[0];
    }

    private int RES_3_E() {
        reg.e &= (byte) ~(1 << 3);

        nimo = "RES 3,E";

        return insts[0x19b].cycle[0];
    }

    private int RES_3_H() {
        reg.h &= (byte) ~(1 << 3);

        nimo = "RES 3,H";

        return insts[0x19c].cycle[0];
    }

    private int RES_3_L() {
        reg.l &= (byte) ~(1 << 3);

        nimo = "RES 3,L";

        return insts[0x19d].cycle[0];
    }

    private int RES_3_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a &= (byte) ~(1 << 3);
        mem.pokeB(reg.getHl(), a);

        nimo = "RES 3,(HL)";

        return insts[0x19e].cycle[0];
    }

    private int RES_3_A() {
        reg.a &= (byte) ~(1 << 3);

        nimo = "RES 3,A";

        return insts[0x19f].cycle[0];
    }

    // 0xa0 (CB) - 0x1a0
    private int RES_4_B() {
        reg.b &= (byte) ~(1 << 4);

        nimo = "RES 4,B";

        return insts[0x1a0].cycle[0];
    }

    private int RES_4_C() {
        reg.c &= (byte) ~(1 << 4);

        nimo = "RES 4,C";

        return insts[0x1a1].cycle[0];
    }

    private int RES_4_D() {
        reg.d &= (byte) ~(1 << 4);

        nimo = "RES 4,D";

        return insts[0x1a2].cycle[0];
    }

    private int RES_4_E() {
        reg.e &= (byte) ~(1 << 4);

        nimo = "RES 4,E";

        return insts[0x1a3].cycle[0];
    }

    private int RES_4_H() {
        reg.h &= (byte) ~(1 << 4);

        nimo = "RES 4,H";

        return insts[0x1a4].cycle[0];
    }

    private int RES_4_L() {
        reg.l &= (byte) ~(1 << 4);

        nimo = "RES 4,L";

        return insts[0x1a5].cycle[0];
    }

    private int RES_4_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a &= (byte) ~(1 << 4);
        mem.pokeB(reg.getHl(), a);

        nimo = "RES 4,(HL)";

        return insts[0x1a6].cycle[0];
    }

    private int RES_4_A() {
        reg.a &= (byte) ~(1 << 4);

        nimo = "RES 4,A";

        return insts[0x1a7].cycle[0];
    }

    // 0xa8 (CB) - 0x1a8
    private int RES_5_B() {
        reg.b &= (byte) ~(1 << 5);

        nimo = "RES 5,B";

        return insts[0x1a8].cycle[0];
    }

    private int RES_5_C() {
        reg.c &= (byte) ~(1 << 5);

        nimo = "RES 5,C";

        return insts[0x1a9].cycle[0];
    }

    private int RES_5_D() {
        reg.d &= (byte) ~(1 << 5);

        nimo = "RES 5,D";

        return insts[0x1aa].cycle[0];
    }

    private int RES_5_E() {
        reg.e &= (byte) ~(1 << 5);

        nimo = "RES 5,E";

        return insts[0x1ab].cycle[0];
    }

    private int RES_5_H() {
        reg.h &= (byte) ~(1 << 5);

        nimo = "RES 5,H";

        return insts[0x1ac].cycle[0];
    }

    private int RES_5_L() {
        reg.l &= (byte) ~(1 << 5);

        nimo = "RES 5,L";

        return insts[0x1ad].cycle[0];
    }

    private int RES_5_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a &= (byte) ~(1 << 5);
        mem.pokeB(reg.getHl(), a);

        nimo = "RES 5,(HL)";

        return insts[0x1ae].cycle[0];
    }

    private int RES_5_A() {
        reg.a &= (byte) ~(1 << 5);

        nimo = "RES 5,A";

        return insts[0x1af].cycle[0];
    }

    // 0xb0 (CB) - 0x1b0
    private int RES_6_B() {
        reg.b &= (byte) ~(1 << 6);

        nimo = "RES 6,B";

        return insts[0x1b0].cycle[0];
    }

    private int RES_6_C() {
        reg.c &= (byte) ~(1 << 6);

        nimo = "RES 6,C";

        return insts[0x1b1].cycle[0];
    }

    private int RES_6_D() {
        reg.d &= (byte) ~(1 << 6);

        nimo = "RES 6,D";

        return insts[0x1b2].cycle[0];
    }

    private int RES_6_E() {
        reg.e &= (byte) ~(1 << 6);

        nimo = "RES 6,E";

        return insts[0x1b3].cycle[0];
    }

    private int RES_6_H() {
        reg.h &= (byte) ~(1 << 6);

        nimo = "RES 6,H";

        return insts[0x1b4].cycle[0];
    }

    private int RES_6_L() {
        reg.l &= (byte) ~(1 << 6);

        nimo = "RES 6,L";

        return insts[0x1b5].cycle[0];
    }

    private int RES_6_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a &= (byte) ~(1 << 6);
        mem.pokeB(reg.getHl(), a);

        nimo = "RES 6,(HL)";

        return insts[0x1b6].cycle[0];
    }

    private int RES_6_A() {
        reg.a &= (byte) ~(1 << 6);

        nimo = "RES 6,A";

        return insts[0x1b7].cycle[0];
    }

    // 0xb8 (CB) - 0x1b8
    private int RES_7_B() {
        reg.b &= (byte) ~(1 << 7);

        nimo = "RES 7,B";

        return insts[0x1b8].cycle[0];
    }

    private int RES_7_C() {
        reg.c &= (byte) ~(1 << 7);

        nimo = "RES 7,C";

        return insts[0x1b9].cycle[0];
    }

    private int RES_7_D() {
        reg.d &= (byte) ~(1 << 7);

        nimo = "RES 7,D";

        return insts[0x1ba].cycle[0];
    }

    private int RES_7_E() {
        reg.e &= (byte) ~(1 << 7);

        nimo = "RES 7,E";

        return insts[0x1bb].cycle[0];
    }

    private int RES_7_H() {
        reg.h &= (byte) ~(1 << 7);

        nimo = "RES 7,H";

        return insts[0x1bc].cycle[0];
    }

    private int RES_7_L() {
        reg.l &= (byte) ~(1 << 7);

        nimo = "RES 7,L";

        return insts[0x1bd].cycle[0];
    }

    private int RES_7_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a &= (byte) ~(1 << 7);
        mem.pokeB(reg.getHl(), a);

        nimo = "RES 7,(HL)";

        return insts[0x1be].cycle[0];
    }

    private int RES_7_A() {
        reg.a &= (byte) ~(1 << 7);

        nimo = "RES 7,A";

        return insts[0x1bf].cycle[0];
    }

    // 0xc0 (CB) - 0x1c0
    private int SET_0_B() {
        reg.b |= (byte) (1 << 0);

        nimo = "SET 0,B";

        return insts[0x1c0].cycle[0];
    }

    private int SET_0_C() {
        reg.c |= (byte) (1 << 0);

        nimo = "SET 0,C";

        return insts[0x1c1].cycle[0];
    }

    private int SET_0_D() {
        reg.d |= (byte) (1 << 0);

        nimo = "SET 0,D";

        return insts[0x1c2].cycle[0];
    }

    private int SET_0_E() {
        reg.e |= (byte) (1 << 0);

        nimo = "SET 0,E";

        return insts[0x1c3].cycle[0];
    }

    private int SET_0_H() {
        reg.h |= (byte) (1 << 0);

        nimo = "SET 0,H";

        return insts[0x1c4].cycle[0];
    }

    private int SET_0_L() {
        reg.l |= (byte) (1 << 0);

        nimo = "SET 0,L";

        return insts[0x1c5].cycle[0];
    }

    private int SET_0_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a |= (byte) (1 << 0);
        mem.pokeB(reg.getHl(), a);

        nimo = "SET 0,(HL)";

        return insts[0x1c6].cycle[0];
    }

    private int SET_0_A() {
        reg.a |= (byte) (1 << 0);

        nimo = "SET 0,A";

        return insts[0x1c7].cycle[0];
    }

    // 0xc8 (CB) - 0x1c8
    private int SET_1_B() {
        reg.b |= (byte) (1 << 1);

        nimo = "SET 1,B";

        return insts[0x1c8].cycle[0];
    }

    private int SET_1_C() {
        reg.c |= (byte) (1 << 1);

        nimo = "SET 1,C";

        return insts[0x1c9].cycle[0];
    }

    private int SET_1_D() {
        reg.d |= (byte) (1 << 1);

        nimo = "SET 1,D";

        return insts[0x1ca].cycle[0];
    }

    private int SET_1_E() {
        reg.e |= (byte) (1 << 1);

        nimo = "SET 1,E";

        return insts[0x1cb].cycle[0];
    }

    private int SET_1_H() {
        reg.h |= (byte) (1 << 1);

        nimo = "SET 1,H";

        return insts[0x1cc].cycle[0];
    }

    private int SET_1_L() {
        reg.l |= (byte) (1 << 1);

        nimo = "SET 1,L";

        return insts[0x1cd].cycle[0];
    }

    private int SET_1_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a |= (byte) (1 << 1);
        mem.pokeB(reg.getHl(), a);

        nimo = "SET 1,(HL)";

        return insts[0x1ce].cycle[0];
    }

    private int SET_1_A() {
        reg.a |= (byte) (1 << 1);

        nimo = "SET 1,A";

        return insts[0x1cf].cycle[0];
    }

    // 0xd0 (CB) - 0x1d0
    private int SET_2_B() {
        reg.b |= (byte) (1 << 2);

        nimo = "SET 2,B";

        return insts[0x1d0].cycle[0];
    }

    private int SET_2_C() {
        reg.c |= (byte) (1 << 2);

        nimo = "SET 2,C";

        return insts[0x1d1].cycle[0];
    }

    private int SET_2_D() {
        reg.d |= (byte) (1 << 2);

        nimo = "SET 2,D";

        return insts[0x1d2].cycle[0];
    }

    private int SET_2_E() {
        reg.e |= (byte) (1 << 2);

        nimo = "SET 2,E";

        return insts[0x1d3].cycle[0];
    }

    private int SET_2_H() {
        reg.h |= (byte) (1 << 2);

        nimo = "SET 2,H";

        return insts[0x1d4].cycle[0];
    }

    private int SET_2_L() {
        reg.l |= (byte) (1 << 2);

        nimo = "SET 2,L";

        return insts[0x1d5].cycle[0];
    }

    private int SET_2_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a |= (byte) (1 << 2);
        mem.pokeB(reg.getHl(), a);

        nimo = "SET 2,(HL)";

        return insts[0x1d6].cycle[0];
    }

    private int SET_2_A() {
        reg.a |= (byte) (1 << 2);

        nimo = "SET 2,A";

        return insts[0x1d7].cycle[0];
    }

    // 0xd8 (CB) - 0x1d8
    private int SET_3_B() {
        reg.b |= (byte) (1 << 3);

        nimo = "SET 3,B";

        return insts[0x1d8].cycle[0];
    }

    private int SET_3_C() {
        reg.c |= (byte) (1 << 3);

        nimo = "SET 3,C";

        return insts[0x1d9].cycle[0];
    }

    private int SET_3_D() {
        reg.d |= (byte) (1 << 3);

        nimo = "SET 3,D";

        return insts[0x1da].cycle[0];
    }

    private int SET_3_E() {
        reg.e |= (byte) (1 << 3);

        nimo = "SET 3,E";

        return insts[0x1db].cycle[0];
    }

    private int SET_3_H() {
        reg.h |= (byte) (1 << 3);

        nimo = "SET 3,H";

        return insts[0x1dc].cycle[0];
    }

    private int SET_3_L() {
        reg.l |= (byte) (1 << 3);

        nimo = "SET 3,L";

        return insts[0x1dd].cycle[0];
    }

    private int SET_3_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a |= (byte) (1 << 3);
        mem.pokeB(reg.getHl(), a);

        nimo = "SET 3,(HL)";

        return insts[0x1de].cycle[0];
    }

    private int SET_3_A() {
        reg.a |= (byte) (1 << 3);

        nimo = "SET 3,A";

        return insts[0x1df].cycle[0];
    }

    // 0xe0 (CB) - 0x1e0
    private int SET_4_B() {
        reg.b |= (byte) (1 << 4);

        nimo = "SET 4,B";

        return insts[0x1e0].cycle[0];
    }

    private int SET_4_C() {
        reg.c |= (byte) (1 << 4);

        nimo = "SET 4,C";

        return insts[0x1e1].cycle[0];
    }

    private int SET_4_D() {
        reg.d |= (byte) (1 << 4);

        nimo = "SET 4,D";

        return insts[0x1e2].cycle[0];
    }

    private int SET_4_E() {
        reg.e |= (byte) (1 << 4);

        nimo = "SET 4,E";

        return insts[0x1e3].cycle[0];
    }

    private int SET_4_H() {
        reg.h |= (byte) (1 << 4);

        nimo = "SET 4,H";

        return insts[0x1e4].cycle[0];
    }

    private int SET_4_L() {
        reg.l |= (byte) (1 << 4);

        nimo = "SET 4,L";

        return insts[0x1e5].cycle[0];
    }

    private int SET_4_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a |= (byte) (1 << 4);
        mem.pokeB(reg.getHl(), a);

        nimo = "SET 4,(HL)";

        return insts[0x1e6].cycle[0];
    }

    private int SET_4_A() {
        reg.a |= (byte) (1 << 4);

        nimo = "SET 4,A";

        return insts[0x1e7].cycle[0];
    }

    // 0xe8 (CB) - 0x1e8
    private int SET_5_B() {
        reg.b |= (byte) (1 << 5);

        nimo = "SET 5,B";

        return insts[0x1e8].cycle[0];
    }

    private int SET_5_C() {
        reg.c |= (byte) (1 << 5);

        nimo = "SET 5,C";

        return insts[0x1e9].cycle[0];
    }

    private int SET_5_D() {
        reg.d |= (byte) (1 << 5);

        nimo = "SET 5,D";

        return insts[0x1ea].cycle[0];
    }

    private int SET_5_E() {
        reg.e |= (byte) (1 << 5);

        nimo = "SET 5,E";

        return insts[0x1eb].cycle[0];
    }

    private int SET_5_H() {
        reg.h |= (byte) (1 << 5);

        nimo = "SET 5,H";

        return insts[0x1ec].cycle[0];
    }

    private int SET_5_L() {
        reg.l |= (byte) (1 << 5);

        nimo = "SET 5,L";

        return insts[0x1ed].cycle[0];
    }

    private int SET_5_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a |= (byte) (1 << 5);
        mem.pokeB(reg.getHl(), a);

        nimo = "SET 5,(HL)";

        return insts[0x1ee].cycle[0];
    }

    private int SET_5_A() {
        reg.a |= (byte) (1 << 5);

        nimo = "SET 5,A";

        return insts[0x1ef].cycle[0];
    }

    // 0xf0 (CB) - 0x1f0
    private int SET_6_B() {
        reg.b |= (byte) (1 << 6);

        nimo = "SET 6,B";

        return insts[0x1f0].cycle[0];
    }

    private int SET_6_C() {
        reg.c |= (byte) (1 << 6);

        nimo = "SET 6,C";

        return insts[0x1f1].cycle[0];
    }

    private int SET_6_D() {
        reg.d |= (byte) (1 << 6);

        nimo = "SET 6,D";

        return insts[0x1f2].cycle[0];
    }

    private int SET_6_E() {
        reg.e |= (byte) (1 << 6);

        nimo = "SET 6,E";

        return insts[0x1f3].cycle[0];
    }

    private int SET_6_H() {
        reg.h |= (byte) (1 << 6);

        nimo = "SET 6,H";

        return insts[0x1f4].cycle[0];
    }

    private int SET_6_L() {
        reg.l |= (byte) (1 << 6);

        nimo = "SET 6,L";

        return insts[0x1f5].cycle[0];
    }

    private int SET_6_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a |= (byte) (1 << 6);
        mem.pokeB(reg.getHl(), a);

        nimo = "SET 6,(HL)";

        return insts[0x1f6].cycle[0];
    }

    private int SET_6_A() {
        reg.a |= (byte) (1 << 6);

        nimo = "SET 6,A";

        return insts[0x1f7].cycle[0];
    }

    // 0xf8 (CB) - 0x1f8
    private int SET_7_B() {
        reg.b |= (byte) (1 << 7);

        nimo = "SET 7,B";

        return insts[0x1f8].cycle[0];
    }

    private int SET_7_C() {
        reg.c |= (byte) (1 << 7);

        nimo = "SET 7,C";

        return insts[0x1f9].cycle[0];
    }

    private int SET_7_D() {
        reg.d |= (byte) (1 << 7);

        nimo = "SET 7,D";

        return insts[0x1fa].cycle[0];
    }

    private int SET_7_E() {
        reg.e |= (byte) (1 << 7);

        nimo = "SET 7,E";

        return insts[0x1fb].cycle[0];
    }

    private int SET_7_H() {
        reg.h |= (byte) (1 << 7);

        nimo = "SET 7,H";

        return insts[0x1fc].cycle[0];
    }

    private int SET_7_L() {
        reg.l |= (byte) (1 << 7);

        nimo = "SET 7,L";

        return insts[0x1fd].cycle[0];
    }

    private int SET_7_pHLs() {
        byte a = mem.peekB(reg.getHl());
        a |= (byte) (1 << 7);
        mem.pokeB(reg.getHl(), a);

        nimo = "SET 7,(HL)";

        return insts[0x1fe].cycle[0];
    }

    private int SET_7_A() {
        reg.a |= (byte) (1 << 7);

        nimo = "SET 7,A";

        return insts[0x1ff].cycle[0];
    }

    private void push(int dat) {
        reg.sp = (reg.sp - 2) & 0xFFFF;
        mem.pokeW(reg.sp, dat & 0xFFFF);
    }

    private int pop() {
        int dat = mem.peekW(reg.sp);
        reg.sp = (reg.sp + 2) & 0xFFFF;
        return dat;
    }
}
