package mdplayer.driver.zgm.zgmChip;

import mdplayer.ChipRegister;
import mdplayer.Setting;


public class ChipFactory {

    public ZgmChip create(int chipIdentNo, ChipRegister chipRegister, Setting setting, byte[] vgmBuf) {
        return switch (chipIdentNo) {
            case 0x0000_000C -> null; // new SN76489(chipRegister, setting, dataBuf);
            case 0x0000_0010 -> null; // new YM2413(chipRegister, setting, dataBuf);
            case 0x0000_002c -> null; // new Ym2612Inst(chipRegister, setting, dataBuf);
            case 0x0000_0030 -> null; // new YM2151(chipRegister, setting, dataBuf);
            case 0x0000_0038 -> null; // new SEGAPCM(chipRegister, setting, dataBuf);
            case 0x0000_0040 -> null; // RF5C68
            case 0x0000_0044 -> null; // new YM2203(chipRegister, setting, dataBuf);
            case 0x0000_0048 -> null; // new YM2608(chipRegister, setting, dataBuf);
            case 0x0000_004C -> null; // new YM2610(chipRegister, setting, dataBuf);
            case 0x0000_0050 -> null; // YM3812
            case 0x0000_0054 -> null; // YM3526
            case 0x0000_0058 -> null; // Y8950Inst
            case 0x0000_005C -> null; // YMF262
            case 0x0000_0060 -> null; // YMF278B
            case 0x0000_0064 -> null; // YMF271
            case 0x0000_0068 -> null; // YMZ280B
            case 0x0000_006C -> null; // new RF5C164(chipRegister, setting, dataBuf);
            case 0x0000_0070 -> null; // PWM
            case 0x0000_0074 -> null; // AY8910
            case 0x0000_0080 -> null; // GameBoy DMG
            case 0x0000_0084 -> null; // NES APU
            case 0x0000_0088 -> null; // MultiPCM
            case 0x0000_008C -> null; // uPD7759
            case 0x0000_0090 -> null; // OKIM6258
            case 0x0000_0098 -> null; // OKIM6295
            case 0x0000_009C -> null; // new K051649Inst(chipRegister, setting, dataBuf);
            case 0x0000_00A0 -> null; // K054539Inst
            case 0x0000_00A4 -> null; // new OotakeHuC6280(chipRegister, setting, dataBuf);
            case 0x0000_00A8 -> null; // new C140Inst(chipRegister, setting, dataBuf);
            case 0x0000_00AC -> null; // new K053260Inst(chipRegister, setting, dataBuf);
            case 0x0000_00B0 -> null; // PokeyInst
            case 0x0000_00B4 -> null; // new QSoundInst(chipRegister, setting, dataBuf);
            case 0x0000_00B8 -> null; // SCSP
            case 0x0000_00C0 -> null; // WonderSwan
            case 0x0000_00C4 -> null; // Virtual Boy VSU
            case 0x0000_00C8 -> null; // SAA1099
            case 0x0000_00CC -> null; // ES5503
            case 0x0000_00D0 -> null; // ES5505/ES5506
            case 0x0000_00D8 -> null; // X1-010
            case 0x0000_00DC -> null; // C352Inst
            case 0x0000_00E0 -> null; // GA20
            case 0x0001_0000 -> new Conductor(chipRegister, setting, vgmBuf);
            case 0x0001_0004 -> null; // VRC6
            case 0x0001_0008 -> null; // VRC7
            case 0x0001_000C -> null; // MMC5
            case 0x0001_0010 -> null; // N106
            case 0x0001_0014 -> null; // S5B
            case 0x0001_0018 -> new Gigatron(chipRegister, setting, vgmBuf);
            case 0x0002_0001 -> new YM2609(chipRegister, setting, vgmBuf);
            case 0x0003_0000 -> null; // XG MU50
            case 0x0003_0001 -> null; // XG MU100
            case 0x0003_0002 -> null; // XG MU128
            case 0x0003_0003 -> null; // XG MU1000
            case 0x0003_0004 -> null; // XG MU2000
            case 0x0003_0005 -> null; // XG MU1000EX
            case 0x0003_0006 -> null; // XG MU2000EX
            case 0x0004_0000 -> null; // GS MT-32 LA
            case 0x0004_0001 -> null; // GS CM-64 LA
            case 0x0004_0002 -> null; // GS SC-55
            case 0x0004_0003 -> null; // GS SC-55mkII
            case 0x0004_0004 -> null; // GS SC-88
            case 0x0004_0005 -> null; // GS SC-88Pro
            case 0x0004_0006 -> null; // GS SC-8820
            case 0x0004_0007 -> null; // GS SC-8850
            case 0x0004_0008 -> null; // GS SD-90
            case 0x0004_0009 -> null; // GS Integra-7
            case 0x0005_0000 -> null; // new MidiGM(chipRegister, setting, dataBuf);
            case 0x0006_0000 -> null; // CSTi General
            case 0x0007_0000 -> null; // Wave General
            default -> throw new IllegalArgumentException("unknown chip: 0x%08x".formatted(chipIdentNo));
        };
    }
}
