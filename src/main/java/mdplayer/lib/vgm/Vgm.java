package mdplayer.lib.vgm;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


public class Vgm {

    private static final Logger logger = getLogger(Vgm.class.getName());

    public static final int FCC_VGM = 0x206D6756; // "Vgm "
    public static final int FCC_GD3 = 0x20336447; // "Gd3 "

    public static final int DefaultSN76489ClockValue = 3579545;
    public static final int DefaultYM2612ClockValue = 7670454;
    public static final int DefaultRF5C164ClockValue = 12500000;
    public static final int DefaultPWMClockValue = 23011361;
    public static final int DefaultC140ClockValue = 21390;
    public static final int DefaultC140Type = 2;
    public static final int DefaultOKIM6258ClockValue = 4000000;
    public static final int DefaultOKIM6295ClockValue = 4000000;
    public static final int DefaultSEGAPCMClockValue = 4000000;
    public static final int DefaultAY8910ClockValue = 1789750;

    public int sn76489ClockValue = DefaultSN76489ClockValue;
    public int ym2612ClockValue = DefaultYM2612ClockValue;
    public int rf5C68ClockValue = 12500000;
    public int rf5C164ClockValue = DefaultRF5C164ClockValue;
    public int pwmClockValue = DefaultPWMClockValue;
    public int c140ClockValue = DefaultC140ClockValue;
    public int C140Type = DefaultC140Type;
    public int okiM6258ClockValue = DefaultOKIM6258ClockValue;
    public int okiM6258Type = 0;
    public int okiM6295ClockValue = DefaultOKIM6295ClockValue;
    public int segaPCMClockValue = DefaultSEGAPCMClockValue;
    public int segaPCMInterface = 0;
    public int ym2151ClockValue;
    public int ym2608ClockValue;
    public int ym2203ClockValue;
    public int ym2610ClockValue;
    public int ym3812ClockValue;
    public int ym3526ClockValue;
    public int y8950ClockValue;
    public int ymF262ClockValue;
    public int ymF271ClockValue;
    public int ymF278BClockValue;
    public int ymZ280BClockValue;
    public int ay8910ClockValue;
    public int ym2413ClockValue;
    public int huC6280ClockValue;
    public int qSoundClockValue;
    public int saa1099ClockValue;
    public int wSwanClockValue;
    public int es5503ClockValue;
    public int es5505ClockValue;
    public int msm5205ClockValue;
    public int msm5232ClockValue;
    public int x1_010ClockValue;
    public int c352ClockValue;
    public int c352ClockDivider;
    public int ga20ClockValue;
    public int k053260ClockValue;
    public int k054539ClockValue;
    public int k054539Flags;
    public int k051649ClockValue;
    public int dmgClockValue;
    public int nesClockValue;
    public int multiPCMClockValue;
    public int uPD7759ClockValue;
    public int pokeyClockValue;

    /**
     * Header "Volume Modifier" (0x7c), raw. The file asks the player to play it at
     * {@code 2^(volumeModifier / 0x20)}; 0 (the default) means 100%. See {@link #getVolumeGain()}.
     */
    private int volumeModifier;

    /**
     * The gain the header's volume modifier asks for. 0x01..0xc0 are +1..+192 (up to x64),
     * 0xc1..0xff are -63..-1, where -63 counts as -64 so the smallest factor is exactly 0.25.
     */
    public double getVolumeGain() {
        if (volumeModifier == 0) return 1.0;
        int v = volumeModifier > 0xc0 ? volumeModifier - 0x100 : volumeModifier;
        if (v == -63) v = -64;
        return Math.pow(2.0, v / 32.0);
    }

    public boolean ym2612DualChipFlag;
    public boolean ym2151DualChipFlag;
    public boolean ym2203DualChipFlag;
    public boolean ym2608DualChipFlag;
    public boolean ym2610DualChipFlag;
    public boolean ym2610BFlag;
    public boolean ym3812DualChipFlag;
    public boolean ym3526DualChipFlag;
    public boolean y8950DualChipFlag;
    public boolean ymF262DualChipFlag;
    public boolean ymF271DualChipFlag;
    public boolean ymF278BDualChipFlag;
    public boolean ymZ280BDualChipFlag;
    public boolean okiM6295DualChipFlag;
    public boolean sn76489DualChipFlag;
    public boolean sn76489NGPFlag;
    public Object[] sn76489Option;
    public boolean rf5C68DualChipFlag;
    public boolean rf5C164DualChipFlag;
    public boolean ay8910DualChipFlag;
    public boolean ym2413DualChipFlag;
    public boolean ym2413VRC7Flag;
    public boolean huC6280DualChipFlag;
    public boolean c140DualChipFlag;
    public boolean saA1099DualChipFlag;
    public boolean wSwanDualChipFlag;
    public boolean es5503DualChipFlag;
    public boolean es5505DualChipFlag;
    public boolean msm5205DualChipFlag;
    public boolean msm5232DualChipFlag;
    public boolean x1_010DualChipFlag;
    public boolean c352DualChipFlag;
    public boolean ga20DualChipFlag;
    public boolean k053260DualChipFlag;
    public boolean k054539DualChipFlag;
    public boolean k051649DualChipFlag;
    public boolean dmgDualChipFlag;
    public boolean nesDualChipFlag;
    public boolean multiPCMDualChipFlag;
    public boolean uPD7759DualChipFlag;
    public boolean pokeyDualChipFlag;

    private IVgm ivgm;
    private IDac dacControl;
    public boolean isPcmRAMWrite = false;
    public boolean useChipYM2612Ch6 = false;
    public int es5503Ch = 2;
    public int es5505Ch = 1;
    /** bit 1-0: prescaler (S1, S2), bit 2: 4 bit ADPCM, bit 7: MSM6585 */
    public int msm5205Flags = 0x06;

    public final Runnable[] vgmCmdTbl = new Runnable[0x100];

    public int vgmAdr;
    public int vgmWait;
    public int vgmLoopOffset = 0;
    public int vgmEof;
    public boolean vgmAnalyze;

    private int vgmDataOffset = 0;

    private static final int PCM_BANK_COUNT = 0x40;
    private final VgmPcmBank[] pcmBank = new VgmPcmBank[PCM_BANK_COUNT];
    private final PcmBankTbl pcmTbl = new PcmBankTbl();
    private int dacCtrlUsed;
    private final byte[] dacCtrlUsg = new byte[0xff];
    private final DacCtrlData[] dacCtrl = new DacCtrlData[0xff];

    public byte[] vgmBuf;

    public void setIVgm(IVgm ivgm) {
        this.ivgm = ivgm;
        this.dacControl = new DacControl(ivgm);
    }

    public void init() {
        if (!getInformationHeader()) throw new IllegalArgumentException("invalid vgm header");

        vgmAdr = vgmDataOffset;
        vgmWait = 0;
        vgmAnalyze = true;

        for (int i = 0; i < PCM_BANK_COUNT; i++) pcmBank[i] = new VgmPcmBank();
        dacControl.refresh();
        dacCtrlUsed = 0x00;
        for (int curChip = 0x00; curChip < 0xff; curChip++) {
            dacCtrl[curChip] = new DacCtrlData();
            dacCtrl[curChip].enable = false;
        }

        setCommands();

        isPcmRAMWrite = false;
    }

    public void oneFrameVGMStream() {
        for (int curChip = 0x00; curChip < dacCtrlUsed; curChip++) {
            dacControl.update(dacCtrlUsg[curChip] & 0xff, 1);
        }
    }

    private void setCommands() {

        Arrays.fill(vgmCmdTbl, null);

        vgmCmdTbl[0x30] = this::vcPSG;
        vgmCmdTbl[0x31] = this::vcDummy1Ope;
        vgmCmdTbl[0x32] = this::vcMsm5205;
        vgmCmdTbl[0x33] = this::vcDummy1Ope;
        vgmCmdTbl[0x34] = this::vcDummy1Ope;
        vgmCmdTbl[0x35] = this::vcDummy1Ope;
        vgmCmdTbl[0x36] = this::vcDummy1Ope;
        vgmCmdTbl[0x37] = this::vcDummy1Ope;

        vgmCmdTbl[0x38] = this::vcDummy1Ope;
        vgmCmdTbl[0x39] = this::vcDummy1Ope;
        vgmCmdTbl[0x3a] = this::vcDummy1Ope;
        vgmCmdTbl[0x3b] = this::vcDummy1Ope;
        vgmCmdTbl[0x3c] = this::vcDummy1Ope;
        vgmCmdTbl[0x3d] = this::vcDummy1Ope;
        vgmCmdTbl[0x3e] = this::vcDummy1Ope;
        vgmCmdTbl[0x3f] = this::vcGGPSGPort06;

        vgmCmdTbl[0x40] = this::vcDummy2Ope;
        vgmCmdTbl[0x41] = this::vcDummy2Ope;
        vgmCmdTbl[0x42] = this::vcDummy2Ope;
        vgmCmdTbl[0x43] = this::vcMsm5232;
        vgmCmdTbl[0x44] = this::vcDummy2Ope;
        vgmCmdTbl[0x45] = this::vcDummy2Ope;
        vgmCmdTbl[0x46] = this::vcDummy2Ope;
        vgmCmdTbl[0x47] = this::vcDummy2Ope;

        vgmCmdTbl[0x48] = this::vcDummy2Ope;
        vgmCmdTbl[0x49] = this::vcDummy2Ope;
        vgmCmdTbl[0x4a] = this::vcDummy2Ope;
        vgmCmdTbl[0x4b] = this::vcDummy2Ope;
        vgmCmdTbl[0x4c] = this::vcDummy2Ope;
        vgmCmdTbl[0x4d] = this::vcDummy2Ope;
        vgmCmdTbl[0x4e] = this::vcDummy2Ope;

        vgmCmdTbl[0x4f] = this::vcGGPSGPort06;
        vgmCmdTbl[0x50] = this::vcPSG;

        vgmCmdTbl[0x51] = this::vcYM2413;
        vgmCmdTbl[0x52] = this::vcYM2612Port0;
        vgmCmdTbl[0x53] = this::vcYM2612Port1;

        vgmCmdTbl[0x54] = this::vcYM2151;
        vgmCmdTbl[0x55] = this::vcYM2203;
        vgmCmdTbl[0x56] = this::vcYM2608Port0;
        vgmCmdTbl[0x57] = this::vcYM2608Port1;

        vgmCmdTbl[0x58] = this::vcYM2610Port0;
        vgmCmdTbl[0x59] = this::vcYM2610Port1;
        vgmCmdTbl[0x5a] = this::vcYM3812;
        vgmCmdTbl[0x5b] = this::vcYM3526;
        vgmCmdTbl[0x5c] = this::vcY8950;
        vgmCmdTbl[0x5d] = this::vcYMZ280B;
        vgmCmdTbl[0x5e] = this::vcYMF262Port0;
        vgmCmdTbl[0x5f] = this::vcYMF262Port1;

        vgmCmdTbl[0x61] = this::vcWaitNSamples;
        vgmCmdTbl[0x62] = this::vcWait735Samples;
        vgmCmdTbl[0x63] = this::vcWait882Samples;
        vgmCmdTbl[0x64] = this::vcOverrideLength;

        vgmCmdTbl[0x66] = this::vcEndOfSoundData;
        vgmCmdTbl[0x67] = this::vcDataBlock;
        vgmCmdTbl[0x68] = this::vcPCMRamWrite;

        vgmCmdTbl[0x70] = this::vcWaitN1Samples;
        vgmCmdTbl[0x71] = this::vcWaitN1Samples;
        vgmCmdTbl[0x72] = this::vcWaitN1Samples;
        vgmCmdTbl[0x73] = this::vcWaitN1Samples;
        vgmCmdTbl[0x74] = this::vcWaitN1Samples;
        vgmCmdTbl[0x75] = this::vcWaitN1Samples;
        vgmCmdTbl[0x76] = this::vcWaitN1Samples;
        vgmCmdTbl[0x77] = this::vcWaitN1Samples;

        vgmCmdTbl[0x78] = this::vcWaitN1Samples;
        vgmCmdTbl[0x79] = this::vcWaitN1Samples;
        vgmCmdTbl[0x7a] = this::vcWaitN1Samples;
        vgmCmdTbl[0x7b] = this::vcWaitN1Samples;
        vgmCmdTbl[0x7c] = this::vcWaitN1Samples;
        vgmCmdTbl[0x7d] = this::vcWaitN1Samples;
        vgmCmdTbl[0x7e] = this::vcWaitN1Samples;
        vgmCmdTbl[0x7f] = this::vcWaitN1Samples;

        vgmCmdTbl[0x80] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x81] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x82] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x83] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x84] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x85] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x86] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x87] = this::vcWaitNSamplesAndSendYM26120x2a;

        vgmCmdTbl[0x88] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x89] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x8a] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x8b] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x8c] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x8d] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x8e] = this::vcWaitNSamplesAndSendYM26120x2a;
        vgmCmdTbl[0x8f] = this::vcWaitNSamplesAndSendYM26120x2a;

        vgmCmdTbl[0x90] = this::vcSetupStreamControl;
        vgmCmdTbl[0x91] = this::vcSetStreamData;
        vgmCmdTbl[0x92] = this::vcSetStreamFrequency;
        vgmCmdTbl[0x93] = this::vcStartStream;
        vgmCmdTbl[0x94] = this::vcStopStream;
        vgmCmdTbl[0x95] = this::vcStartStreamFastCall;

        vgmCmdTbl[0xa0] = this::vcAY8910;
        vgmCmdTbl[0xa1] = this::vcYM2413;
        vgmCmdTbl[0xa2] = this::vcYM2612Port0;
        vgmCmdTbl[0xa3] = this::vcYM2612Port1;
        vgmCmdTbl[0xa4] = this::vcYM2151;
        vgmCmdTbl[0xa5] = this::vcYM2203;
        vgmCmdTbl[0xa6] = this::vcYM2608Port0;
        vgmCmdTbl[0xa7] = this::vcYM2608Port1;

        vgmCmdTbl[0xa8] = this::vcYM2610Port0;
        vgmCmdTbl[0xa9] = this::vcYM2610Port1;
        vgmCmdTbl[0xaa] = this::vcYM3812;
        vgmCmdTbl[0xab] = this::vcYM3526;
        vgmCmdTbl[0xac] = this::vcY8950;
        vgmCmdTbl[0xad] = this::vcYMZ280B;
        vgmCmdTbl[0xae] = this::vcYMF262Port0;
        vgmCmdTbl[0xaf] = this::vcYMF262Port1;

        vgmCmdTbl[0xb0] = this::vcRf5c68;
        vgmCmdTbl[0xb1] = this::vcRf5c164;
        vgmCmdTbl[0xb2] = this::vcPWM;
        vgmCmdTbl[0xb3] = this::vcDMG;
        vgmCmdTbl[0xb4] = this::vcNES;
        vgmCmdTbl[0xb5] = this::vcMultiPCM;
        vgmCmdTbl[0xb6] = this::vcuPD7759;
        vgmCmdTbl[0xb7] = this::vcOKIM6258;

        vgmCmdTbl[0xb8] = this::vcOKIM6295;
        vgmCmdTbl[0xb9] = this::vcHuC6280;
        vgmCmdTbl[0xba] = this::vcK053260;
        vgmCmdTbl[0xbb] = this::vcPOKEY;
        vgmCmdTbl[0xbc] = this::vcWSwan;
        vgmCmdTbl[0xbd] = this::vcSAA1099;
        vgmCmdTbl[0xbe] = this::vcEs5505;
        vgmCmdTbl[0xbf] = this::vcGA20;

        vgmCmdTbl[0xc0] = this::vcSEGAPCM;
        vgmCmdTbl[0xc1] = this::vcRf5c68MemoryWrite;
        vgmCmdTbl[0xc2] = this::vcRf5c164MemoryWrite;
        vgmCmdTbl[0xc3] = this::vcMultiPCMSetBank;
        vgmCmdTbl[0xc4] = this::vcQSound;
        vgmCmdTbl[0xc5] = this::vcDummy3Ope;
        vgmCmdTbl[0xc6] = this::vcWSwanMem;
        vgmCmdTbl[0xc7] = this::vcDummy3Ope;

        vgmCmdTbl[0xc8] = this::vcX1_010;
        vgmCmdTbl[0xc9] = this::vcDummy3Ope;
        vgmCmdTbl[0xca] = this::vcDummy3Ope;
        vgmCmdTbl[0xcb] = this::vcDummy3Ope;
        vgmCmdTbl[0xcc] = this::vcDummy3Ope;
        vgmCmdTbl[0xcd] = this::vcDummy3Ope;
        vgmCmdTbl[0xce] = this::vcDummy3Ope;
        vgmCmdTbl[0xcf] = this::vcDummy3Ope;

        vgmCmdTbl[0xd0] = this::vcYMF278B;
        vgmCmdTbl[0xd1] = this::vcYMF271;
        vgmCmdTbl[0xd2] = this::vcK051649;
        vgmCmdTbl[0xd3] = this::vcK054539;
        vgmCmdTbl[0xd4] = this::vcC140;
        vgmCmdTbl[0xd5] = this::vcEs5503;
        vgmCmdTbl[0xd6] = this::vcEs5505_16;
        vgmCmdTbl[0xd7] = this::vcDummy3Ope;

        vgmCmdTbl[0xd8] = this::vcDummy3Ope;
        vgmCmdTbl[0xd9] = this::vcDummy3Ope;
        vgmCmdTbl[0xda] = this::vcDummy3Ope;
        vgmCmdTbl[0xdb] = this::vcDummy3Ope;
        vgmCmdTbl[0xdc] = this::vcDummy3Ope;
        vgmCmdTbl[0xdd] = this::vcDummy3Ope;
        vgmCmdTbl[0xde] = this::vcDummy3Ope;
        vgmCmdTbl[0xdf] = this::vcDummy3Ope;

        vgmCmdTbl[0xe0] = this::vcSeekToOffsetInPCMDataBank;
        vgmCmdTbl[0xe1] = this::vcC352;
        vgmCmdTbl[0xe2] = this::vcDummy4Ope;
        vgmCmdTbl[0xe3] = this::vcDummy4Ope;
        vgmCmdTbl[0xe4] = this::vcDummy4Ope;
        vgmCmdTbl[0xe5] = this::vcDummy4Ope;
        vgmCmdTbl[0xe6] = this::vcDummy4Ope;
        vgmCmdTbl[0xe7] = this::vcDummy4Ope;

        vgmCmdTbl[0xe8] = this::vcDummy4Ope;
        vgmCmdTbl[0xe9] = this::vcDummy4Ope;
        vgmCmdTbl[0xea] = this::vcDummy4Ope;
        vgmCmdTbl[0xeb] = this::vcDummy4Ope;
        vgmCmdTbl[0xec] = this::vcDummy4Ope;
        vgmCmdTbl[0xed] = this::vcDummy4Ope;
        vgmCmdTbl[0xee] = this::vcDummy4Ope;
        vgmCmdTbl[0xef] = this::vcDummy4Ope;

        vgmCmdTbl[0xf0] = this::vcDummy4Ope;
        vgmCmdTbl[0xf1] = this::vcDummy4Ope;
        vgmCmdTbl[0xf2] = this::vcDummy4Ope;
        vgmCmdTbl[0xf3] = this::vcDummy4Ope;
        vgmCmdTbl[0xf4] = this::vcDummy4Ope;
        vgmCmdTbl[0xf5] = this::vcDummy4Ope;
        vgmCmdTbl[0xf6] = this::vcDummy4Ope;
        vgmCmdTbl[0xf7] = this::vcDummy4Ope;

        vgmCmdTbl[0xf8] = this::vcDummy4Ope;
        vgmCmdTbl[0xf9] = this::vcDummy4Ope;
        vgmCmdTbl[0xfa] = this::vcDummy4Ope;
        vgmCmdTbl[0xfb] = this::vcDummy4Ope;
        vgmCmdTbl[0xfc] = this::vcDummy4Ope;
        vgmCmdTbl[0xfd] = this::vcDummy4Ope;
        vgmCmdTbl[0xfe] = this::vcDummy4Ope;
        vgmCmdTbl[0xff] = this::vcDummy4Ope;
    }

    private void vcDummy1Ope() {
        //logger.log(Level.TRACE, "(%02X:%02X)".formatted(dataBuf[vgmAdr], dataBuf[vgmAdr + 1]));
        vgmAdr += 2;
    }

    private void vcDummy2Ope() {
        //logger.log(Level.TRACE, "(%02X:%02X:%02X)".formatted(dataBuf[vgmAdr], dataBuf[vgmAdr + 1], dataBuf[vgmAdr + 2]));
        vgmAdr += 3;
    }

    private void vcDummy3Ope() {
        //logger.log(Level.TRACE, "(%02X:%02X:%02X:%02X)".formatted(dataBuf[vgmAdr], dataBuf[vgmAdr + 1], dataBuf[vgmAdr + 2], dataBuf[vgmAdr + 3]));
        vgmAdr += 4;
    }

    private void vcDummy4Ope() {
        //logger.log(Level.TRACE, "unknown command:Adr:%x(%02X:%02X:%02X:%02X:%02X)".formatted(vgmAdr, dataBuf[vgmAdr], dataBuf[vgmAdr + 1], dataBuf[vgmAdr + 2], dataBuf[vgmAdr + 3], dataBuf[vgmAdr + 4]));
        vgmAdr += 5;
    }

    private void vcGGPSGPort06() {
        ivgm.setPanSn76489(vgmBuf[vgmAdr] == 0x4f ? 0 : 1, vgmBuf[vgmAdr + 1] & 0xff);
        vgmAdr += 2;
    }

    private void vcPSG() {
        ivgm.writeSn76489(vgmBuf[vgmAdr] == 0x50 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0xff);
        vgmAdr += 2;
    }

    private void vcAY8910() {
        ivgm.writeAy8910((vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0x7f, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcDMG() {
        ivgm.writeDmg((vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0x7f, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcNES() {
        ivgm.writeNes((vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0x7f, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcMultiPCM() {
        ivgm.writeMultiPcm((vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0x7f, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcuPD7759() {
        //if (ivgm.isVirtual()) logger.log(Level.TRACE, "adr:%d data:%02x".formatted(vgmBuf[vgmAdr + 1] & 0x7f, vgmBuf[vgmAdr + 2] & 0xff));
        ivgm.writeUpd7759((vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0x7f, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcMultiPCMSetBank() {
        ivgm.setBankMultiPcm((vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0x7f, (vgmBuf[vgmAdr + 2] & 0xff) + (vgmBuf[vgmAdr + 3] & 0xff) * 0x100);
        vgmAdr += 4;
    }

    private void vcQSound() {
        ivgm.writeQSound(0, vgmBuf[vgmAdr + 1] & 0xff, vgmBuf[vgmAdr + 2] & 0xff, vgmBuf[vgmAdr + 3] & 0xff);
        vgmAdr += 4;
    }

    private void vcX1_010() {
        ivgm.writeX1_010((vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0x7f, vgmBuf[vgmAdr + 2] & 0xff, vgmBuf[vgmAdr + 3] & 0xff);
        vgmAdr += 4;
    }

    private void vcYM2413() {
        ivgm.writeYm2413((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0xff, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcYM3812() {
        ivgm.writeYm3812((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0xff, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcHuC6280() {
        ivgm.writeHuC6280((vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0x7f, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcGA20() {
        ivgm.writeGa20((vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0x7f, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcYM2612Port0() {
        ivgm.writeYm2612((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, 0, vgmBuf[vgmAdr + 1] & 0xff, vgmBuf[vgmAdr + 2] & 0xff, ivgm.frameCounter());
        vgmAdr += 3;
    }

    private void vcYM2612Port1() {
        ivgm.writeYm2612((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, 1, vgmBuf[vgmAdr + 1] & 0xff, vgmBuf[vgmAdr + 2] & 0xff, ivgm.frameCounter());
        vgmAdr += 3;
    }

    private void vcYM2203() {
        ivgm.writeYm2203((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0xff, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcYM2608Port0() {
        ivgm.writeYm2608((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, 0, vgmBuf[vgmAdr + 1] & 0xff, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcYM2608Port1() {
        int adr = vgmBuf[vgmAdr + 1] & 0xff;
        int dat = vgmBuf[vgmAdr + 2] & 0xff;
//        if (adr >= 0x00 && adr <= 0x10 && model == enmModel.RealModel) {
//            logger.log(Level.TRACE, "%2X:%2X".formatted(adr, dat));
//        }
//        if (adr == 0x01) {
//            //dat &= 0xfd;
//            //dat |= 1;
//        }
//        if (adr == 0x00 && (dat & 0x20) != 0) {
//            //dat &= 0xdf;
//        }
        ivgm.writeYm2608((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, 1, adr, dat);
        vgmAdr += 3;
    }

    private void vcYM2610Port0() {
        ivgm.writeYm2610((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, 0, vgmBuf[vgmAdr + 1] & 0xff, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcYM2610Port1() {
        int adr = vgmBuf[vgmAdr + 1] & 0xff;
        int dat = vgmBuf[vgmAdr + 2] & 0xff;
        ivgm.writeYm2610((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, 1, adr, dat);
        vgmAdr += 3;
    }

    private void vcYMF262Port0() {
        ivgm.writeYmF262((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, 0, vgmBuf[vgmAdr + 1] & 0xff, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcYMF262Port1() {
        int adr = vgmBuf[vgmAdr + 1] & 0xff;
        int dat = vgmBuf[vgmAdr + 2] & 0xff;
        ivgm.writeYmF262((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, 1, adr, dat);
        vgmAdr += 3;
    }

    private void vcYM3526() {
        ivgm.writeYm3526((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0xff, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcY8950() {
        ivgm.writeY8950((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0xff, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcYMZ280B() {
        ivgm.writeYmZ280B((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0xff, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcYMF271() {
        ivgm.writeYmF271(
                (vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1,
                vgmBuf[vgmAdr + 1] & 0x7f,
                vgmBuf[vgmAdr + 2] & 0xff,
                vgmBuf[vgmAdr + 3] & 0xff);
        vgmAdr += 4;
    }

    private void vcYMF278B() {
        ivgm.writeYmF278B(
                (vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1,
                vgmBuf[vgmAdr + 1] & 0x7f,
                vgmBuf[vgmAdr + 2] & 0xff,
                vgmBuf[vgmAdr + 3] & 0xff);
//logger.log(Level.TRACE, "fm:%02x:%02x:%02x:".formatted(dataBuf[vgmAdr + 1] & 0x7f, dataBuf[vgmAdr + 2], dataBuf[vgmAdr + 3]));
        vgmAdr += 4;
    }

    private void vcYM2151() {
        ivgm.writeYm2151((vgmBuf[vgmAdr] & 0x80) == 0 ? 0 : 1, 0, vgmBuf[vgmAdr + 1] & 0xff, vgmBuf[vgmAdr + 2] & 0xff, vgmBuf[vgmAdr] & 0x80, ivgm.frameCounter());
        vgmAdr += 3;
    }

    private void vcOKIM6258() {
        ivgm.writeOkiM6258(0, vgmBuf[vgmAdr + 0x01] & 0x7f, vgmBuf[vgmAdr + 0x02] & 0xff);
        vgmAdr += 3;
    }

    private void vcOKIM6295() {
        ivgm.writeOkiM6295((vgmBuf[vgmAdr + 0x01] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 0x01] & 0x7f, vgmBuf[vgmAdr + 0x02] & 0xff);
        vgmAdr += 3;
    }

    private void vcSAA1099() {
        ivgm.writeSaa1099((vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0x7f, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcWSwan() {
        ivgm.writeWSwan((vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0x7f, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcWSwanMem() {
        ivgm.writeMemWSwan(0, (vgmBuf[vgmAdr + 0x02] & 0xff) | ((vgmBuf[vgmAdr + 0x01] & 0xff) << 8), vgmBuf[vgmAdr + 0x03] & 0xff);
        vgmAdr += 4;
    }

    private void vcPOKEY() {
        ivgm.writePokey((vgmBuf[vgmAdr + 1] & 0x80) == 0 ? 0 : 1, vgmBuf[vgmAdr + 1] & 0x7f, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcSEGAPCM() {
//logger.log(Level.TRACE, "%4X %4X".formatted(dataBuf[vgmAdr + 0x01], dataBuf[vgmAdr + 0x02]));
        ivgm.writeSegaPcm(0, (vgmBuf[vgmAdr + 0x01] & 0xff) | ((vgmBuf[vgmAdr + 0x02] & 0xff) << 8), vgmBuf[vgmAdr + 0x03] & 0xff);
        vgmAdr += 4;
    }

    private void vcWaitNSamples() {
        vgmWait += ByteUtil.readLeShort(vgmBuf, vgmAdr + 1) & 0xffff;
        vgmAdr += 3;
    }

    private void vcWait735Samples() {
        vgmWait += 735;
        vgmAdr++;
    }

    private void vcWait882Samples() {
        vgmWait += 882;
        vgmAdr++;
    }

    private void vcOverrideLength() {
        vgmAdr += 4;
    }

    private void vcEndOfSoundData() {
        vgmAdr = vgmBuf.length;
    }

    private void vcDataBlock() {

        ivgm.dataBlock(true);

        int bAdr = vgmAdr + 7;
        int bType = vgmBuf[vgmAdr + 2] & 0xff;
        int bLen = ByteUtil.readLeInt(vgmBuf, vgmAdr + 3);
        int chipId = 0;
        if ((bLen & 0x8000_0000) != 0) {
            bLen &= 0x7fff_ffff;
            chipId = 1;
        }

        switch (bType & 0xe0) {
        case 0x00:
        case 0x40:
            addPCMData(bType, bLen, bAdr);
            vgmAdr += bLen + 7;
            break;
        case 0x80:
            int romSize = ByteUtil.readLeInt(vgmBuf, vgmAdr + 7);
            int startAddress = ByteUtil.readLeInt(vgmBuf, vgmAdr + 0x0B);
            switch (bType & 0xff) {
            case 0x80:
                 // SEGA PCM
                ivgm.writePcmSegaPcm(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;
            case 0x81:
                // YM2608
                ivgm.writePcmYm2608(chipId, vgmBuf, vgmAdr, bLen, startAddress);
                break;
            case 0x82:
                ivgm.writeAdpcmAYm2610(chipId, vgmBuf, vgmAdr, bLen, startAddress, romSize);
                break;
            case 0x83:
                ivgm.writeAdpcmBYm2610(chipId, vgmBuf, vgmAdr, bLen, startAddress, romSize);
                break;

            case 0x84:
                // YMF278B
                ivgm.writePcmYmF278B(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x85:
                // YMF271
                ivgm.writePcmYmF271(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x86:
                // YMZ280B
                ivgm.writePcmYmZ280B(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x87:
                // YMF278B
                ivgm.writeRamYmF278B(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x88:
                // Y8950
                ivgm.writePcmY8950(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x89:
                // MultiPCM
                ivgm.writePcmMultiPcm(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x8a:
                // uPD7759
                ivgm.writePcmUpd7759(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

                case 0x8b:
                // OKIM6295
                ivgm.writePcmOkiM6295(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x8c:
                // K054539
                ivgm.writePcmK054539(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x8d:
                // C140
                ivgm.writePcmC140(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x8e:
                // K053260
                ivgm.writePcmK053260(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x8f:
                // QSound
                ivgm.writePcmQSound(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x90:
                // ES5505/ES5506
                ivgm.writePcmEs5505(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x91:
                // X1-010
                ivgm.writePcmX1_010(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x92:
                // C352
                ivgm.writePcmC352(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;

            case 0x93:
                // GA20
                ivgm.writePcmGa20(chipId, romSize, startAddress, bLen - 8, vgmBuf, vgmAdr + 15);
                break;
            }
            vgmAdr += bLen + 7;
            break;
        case 0xc0:
            int stAdr = ByteUtil.readLeShort(vgmBuf, vgmAdr + 7) & 0xffff;
            int dataSize = bLen - 2;
            int romData = vgmAdr + 9;
            if ((bType & 0x20) != 0) {
                stAdr = ByteUtil.readLeInt(vgmBuf, vgmAdr + 7);
                dataSize = bLen - 4;
                romData = vgmAdr + 11;
            }

            try {
                switch (bType & 0xff) {
                case 0xc0:
                    ivgm.writePcmRf5C68(chipId, stAdr, dataSize, vgmBuf, vgmAdr + 9);
                    break;
                case 0xc1:
                    ivgm.writePcmRf5C164(chipId, stAdr, dataSize, vgmBuf, vgmAdr + 9);
                    break;
                case 0xc2:
                    ivgm.writePcmNes(chipId, stAdr, dataSize, vgmBuf, vgmAdr + 9);
                    break;
                }
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }

            vgmAdr += bLen + 7;
            break;
        case 0xe0:
            int stAdr_E = ByteUtil.readLeInt(vgmBuf, vgmAdr + 7);
            int dataSize_E = bLen - 2;
            int ROMData_E = vgmAdr + 9;
            if ((bType & 0x20) != 0) {
                stAdr_E = ByteUtil.readLeInt(vgmBuf, vgmAdr + 7);
                dataSize_E = bLen - 4;
                ROMData_E = vgmAdr + 11;
            }

            try {
                switch (bType) {
                    case 0xe1:
                        ivgm.writePcmEs5503(chipId, stAdr_E, dataSize_E, vgmBuf, vgmAdr + 11);
                        break;
                }
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }

            vgmAdr += bLen + 7;
            break;
        default:
            vgmAdr += bLen + 7;
            break;
        }

        ivgm.dataBlock(false);
    }

    private void vcPCMRamWrite() {

        isPcmRAMWrite = true;

        int bType = vgmBuf[vgmAdr + 2] & 0x7f;
        //currentChip = (dataBuf[vgmAdr + 2] & 0x80) >> 7;
        int bReadOffset = ByteUtil.readLe24(vgmBuf, vgmAdr + 3);
        int bWriteOffset = ByteUtil.readLe24(vgmBuf, vgmAdr + 6);
        int bSize = ByteUtil.readLe24(vgmBuf, vgmAdr + 9);
        if (bSize == 0) bSize = 0x100_0000;
        Integer pcmAdr = getPCMAddressFromPCMBank(bType, bReadOffset);
        if (pcmAdr != null) {
            if (bType == 0x01) {
                ivgm.writePcmRf5C68(0, bWriteOffset, bSize, pcmBank[bType].data, pcmAdr);
            }
            if (bType == 0x02) {
                ivgm.writePcmRf5C164(0, bWriteOffset, bSize, pcmBank[bType].data, pcmAdr);
            }
        }

        vgmAdr += 12;

        isPcmRAMWrite = false;
    }

    private void vcWaitN1Samples() {
//logger.log(Level.DEBUG, vgmAdr + ": " + (dataBuf[vgmAdr] & 0xff) + ", " + ((dataBuf[vgmAdr] & 0xff) - 0x6f));
        vgmWait += (vgmBuf[vgmAdr] & 0xff) - 0x6f;
        vgmAdr++;
    }

    private void vcWaitNSamplesAndSendYM26120x2a() {
        int dat = getDACFromPCMBank();

        vgmWait += (vgmBuf[vgmAdr] & 0xff) - 0x80;

        ivgm.writeYm2612(0, 0, 0x2a, dat, ivgm.frameCounter());

        vgmAdr++;
    }

    private void vcSetupStreamControl() {
//        if (model != enmModel.VirtualModel) {
//            vgmAdr += 5;
//            return;
//        }

        int si = vgmBuf[vgmAdr + 1] & 0xff;
        if (si == 0xff) {
            vgmAdr += 5;
            return;
        }
        if (!dacCtrl[si].enable) {
            dacControl.deviceStart(si);
            dacControl.deviceReset(si);
            dacCtrl[si].enable = true;
            dacCtrlUsg[dacCtrlUsed] = (byte) si;
            dacCtrlUsed++;
        }
        int chipId = vgmBuf[vgmAdr + 2] & 0xff;
        int port = vgmBuf[vgmAdr + 3] & 0xff;
        int cmd = vgmBuf[vgmAdr + 4] & 0xff;

        dacControl.setupChip(si, chipId & 0x7F, (chipId & 0x80) >> 7, port * 0x100 + cmd);
        vgmAdr += 5;
    }

    private void vcSetStreamData() {
//        if (model != enmModel.VirtualModel) {
//            vgmAdr += 5;
//            return;
//        }

        int si = vgmBuf[vgmAdr + 1] & 0xff;
        if (si == 0xff) {
            vgmAdr += 5;
            return;
        }
        dacCtrl[si].bank = vgmBuf[vgmAdr + 2] & 0xff;
        if (dacCtrl[si].bank >= PCM_BANK_COUNT)
            dacCtrl[si].bank = 0x00;

        VgmPcmBank tempPCM = pcmBank[dacCtrl[si].bank];
        //last95Max = tempPCM->BankCount;
        dacControl.setData(si, tempPCM.data, tempPCM.dataSize, vgmBuf[vgmAdr + 3] & 0xff, vgmBuf[vgmAdr + 4] & 0xff);

        vgmAdr += 5;
    }

    private void vcSetStreamFrequency() {
//        if (model != enmModel.VirtualModel) {
//            vgmAdr += 6;
//            return;
//        }

        int si = vgmBuf[vgmAdr + 1] & 0xff;
        if (si == 0xff || !dacCtrl[si].enable) {
            vgmAdr += 0x06;
            return;
        }
        int tempLng = ByteUtil.readLeInt(vgmBuf, vgmAdr + 2);
        //last95Freq = tempLng;
        dacControl.setFrequency(si, tempLng);
        vgmAdr += 6;
    }

    private void vcStartStream() {
//        if (model != enmModel.VirtualModel) {
//            vgmAdr += 8;
//            return;
//        }

        int si = vgmBuf[vgmAdr + 1] & 0xff;
        if (si == 0xff || !dacCtrl[si].enable || pcmBank[dacCtrl[si].bank].bankCount == 0) {
            vgmAdr += 0x08;
            return;
        }
        int dataStart = ByteUtil.readLeInt(vgmBuf, vgmAdr + 2);
        //last95Drum = 0xffFF;
        int tempByt = vgmBuf[vgmAdr + 6] & 0xff;
        int dataLen = ByteUtil.readLeInt(vgmBuf, vgmAdr + 7);
        dacControl.start(si, dataStart, tempByt, dataLen);
        vgmAdr += 0x0B;
    }

    private void vcStopStream() {
//        if (model != enmModel.VirtualModel) {
//            vgmAdr += 2;
//            return;
//        }

        int si = vgmBuf[vgmAdr + 1] & 0xff;
        if (!dacCtrl[si].enable) {
            vgmAdr += 0x02;
            return;
        }
        // last95Drum = 0xffFF;
        if (si < 0xff) {
            dacControl.stop(si);
        } else {
            for (si = 0x00; si < 0xff; si++)
                dacControl.stop(si);
        }
        vgmAdr += 0x02;
    }

    private void vcStartStreamFastCall() {
//        if (model != enmModel.VirtualModel) {
//            vgmAdr += 5;
//            return;
//        }

        int curChip = vgmBuf[vgmAdr + 1] & 0xff;
        if (curChip == 0xff || !dacCtrl[curChip].enable ||
                pcmBank[dacCtrl[curChip].bank].bankCount == 0) {
            vgmAdr += 0x05;
            return;
        }
        VgmPcmBank tempPCM = pcmBank[dacCtrl[curChip].bank];
        int TempSht = ByteUtil.readLeShort(vgmBuf, vgmAdr + 2) & 0xffff;
        //Last95Drum = TempSht;
        //Last95Max = tempPCM.BankCount;
        if (TempSht >= tempPCM.bankCount)
            TempSht = 0x00;
        VgmPcmData tempBnk = tempPCM.bank.get(TempSht);

        int tempByt = IDac.DCTRL_LMODE_BYTES |
                (vgmBuf[vgmAdr + 4] & 0x10) |         // Reverse Mode
                ((vgmBuf[vgmAdr + 4] & 0x01) << 7);   // Looping
        dacControl.start(curChip, tempBnk.dataStart, tempByt, tempBnk.dataSize);
        vgmAdr += 0x05;
    }

    private void vcSeekToOffsetInPCMDataBank() {
        pcmBank[0x00].dataPos = ByteUtil.readLeInt(vgmBuf, vgmAdr + 1);
        vgmAdr += 5;
    }

    private void vcRf5c68() {
        int id = (vgmBuf[vgmAdr + 1] & 0x80) != 0 ? 1 : 0;
        int cmd = vgmBuf[vgmAdr + 1] & 0x7f;
        ivgm.writeRf5C68(id, cmd, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcRf5c68MemoryWrite() {
        int offset = ByteUtil.readLeShort(vgmBuf, vgmAdr + 1) & 0xffff;
        ivgm.writeMemoryRf5C68(0, offset, vgmBuf[vgmAdr + 3] & 0xff);
        vgmAdr += 4;
    }

    private void vcRf5c164() {
        int id = (vgmBuf[vgmAdr + 1] & 0x80) != 0 ? 1 : 0;
        int cmd = vgmBuf[vgmAdr + 1] & 0x7f;
        ivgm.writeRf5C164(id, cmd, vgmBuf[vgmAdr + 2] & 0xff);
        vgmAdr += 3;
    }

    private void vcRf5c164MemoryWrite() {
        int offset = ByteUtil.readLeShort(vgmBuf, vgmAdr + 1) & 0xffff;
        ivgm.writeMemoryRf5C164(0, offset, vgmBuf[vgmAdr + 3] & 0xff);
        vgmAdr += 4;
    }

    private void vcPWM() {
        int cmd = (vgmBuf[vgmAdr + 1] & 0xf0) >> 4;
        int data = (vgmBuf[vgmAdr + 1] & 0xf) * 0x100 + (vgmBuf[vgmAdr + 2] & 0xff);
        ivgm.writePwm(0, cmd, data);
        vgmAdr += 3;
    }

    private void vcK051649() {
        int scc1_port = vgmBuf[vgmAdr + 1] & 0x7f;
        int scc1_offset = vgmBuf[vgmAdr + 2] & 0xff;
        int rDat = vgmBuf[vgmAdr + 3] & 0xff;
        int scc1_chipId = (vgmBuf[vgmAdr + 1] & 0x80) != 0 ? 1 : 0;
        vgmAdr += 4;
        ivgm.writeK051649(scc1_chipId, (scc1_port << 1) | 0x00, scc1_offset);
        ivgm.writeK051649(scc1_chipId, (scc1_port << 1) | 0x01, rDat);
    }

    private void vcK053260() {
        int id = (vgmBuf[vgmAdr + 1] & 0x80) != 0 ? 1 : 0;
        int adr = vgmBuf[vgmAdr + 1] & 0x7f;
        int data = vgmBuf[vgmAdr + 2] & 0xff;
        ivgm.writeK053260(id, adr, data);
        vgmAdr += 3;
    }

    private void vcK054539() {
        int id = (vgmBuf[vgmAdr + 1] & 0x80) != 0 ? 1 : 0;
        int adr = (vgmBuf[vgmAdr + 1] & 0x7f) * 0x100 + (vgmBuf[vgmAdr + 2] & 0xff);
        int data = vgmBuf[vgmAdr + 3] & 0xff;
        ivgm.writeK054539(id, adr, data);
        vgmAdr += 4;
    }

    private void vcC140() {
        int id = (vgmBuf[vgmAdr + 1] & 0x80) != 0 ? 1 : 0;
        int adr = (vgmBuf[vgmAdr + 1] & 0x7f) * 0x100 + (vgmBuf[vgmAdr + 2] & 0xff);
        int data = vgmBuf[vgmAdr + 3] & 0xff;
        ivgm.writeC140(id, adr, data);
        vgmAdr += 4;
    }

    private void vcMsm5205() { // 0x32 rd
        int id = (vgmBuf[vgmAdr + 1] & 0x80) != 0 ? 1 : 0;
        int adr = (vgmBuf[vgmAdr + 1] & 0x70) >> 4;
        int data = vgmBuf[vgmAdr + 1] & 0x0f;
        ivgm.writeMsm5205(id, adr, data);
        vgmAdr += 2;
    }

    private void vcMsm5232() { // 0x43 aa dd
        int id = (vgmBuf[vgmAdr + 1] & 0x80) != 0 ? 1 : 0;
        int adr = vgmBuf[vgmAdr + 1] & 0x7f;
        int data = vgmBuf[vgmAdr + 2] & 0xff;
        ivgm.writeMsm5232(id, adr, data);
        vgmAdr += 3;
    }

    private void vcEs5505() { // 0xBE aa dd
        int id = (vgmBuf[vgmAdr + 1] & 0x80) != 0 ? 1 : 0;
        int adr = vgmBuf[vgmAdr + 1] & 0x7f;
        int data = vgmBuf[vgmAdr + 2] & 0xff;
        ivgm.writeEs5505(id, adr, data);
        vgmAdr += 3;
    }

    private void vcEs5505_16() { // 0xD6 aa dd dd
        int id = (vgmBuf[vgmAdr + 1] & 0x80) != 0 ? 1 : 0;
        int adr = 0x80 | (vgmBuf[vgmAdr + 1] & 0x7f);
        int data = ((vgmBuf[vgmAdr + 2] & 0xff) << 8) | (vgmBuf[vgmAdr + 3] & 0xff);
        ivgm.writeEs5505(id, adr, data);
        vgmAdr += 4;
    }

    private void vcEs5503() { // 0xD5 pp aa dd
        int id = (vgmBuf[vgmAdr + 1] & 0x80) != 0 ? 1 : 0;
        int adr = (vgmBuf[vgmAdr + 1] & 0x7f) * 0x100 + (vgmBuf[vgmAdr + 2] & 0xff);
        int data = vgmBuf[vgmAdr + 3] & 0xff;
        ivgm.writeEs5503(id, adr, data);
        vgmAdr += 4;
    }

    private void vcC352() {
        int id = (vgmBuf[vgmAdr + 1] & 0x80) != 0 ? 1 : 0;
        int adr = (vgmBuf[vgmAdr + 1] & 0x7f) * 0x100 + (vgmBuf[vgmAdr + 2] & 0xff);
        int data = (vgmBuf[vgmAdr + 3] & 0xff) * 0x100 + (vgmBuf[vgmAdr + 4] & 0xff);
        ivgm.writeC352(id, adr, data);
        vgmAdr += 5;
    }

    private void addPCMData(int Type, int dataSize, int adr) {
        int curBnk;
        VgmPcmBank tempPCM;
        VgmPcmData tempBnk;
        int bankSize;
//        boolean retVal;
        int bnkType;
        int curDAC;

        bnkType = Type & 0x3F;
        if (bnkType >= PCM_BANK_COUNT || ivgm.loop() > 0)
            return;

        if (Type == 0x7F) {
            readPCMTable(dataSize, adr);
            return;
        }

        tempPCM = pcmBank[bnkType];
        tempPCM.bnkPos++;
        if (tempPCM.bnkPos <= tempPCM.bankCount)
            return; // Speed hack for restarting playback (skip already loaded blocks)
        curBnk = tempPCM.bankCount;
        tempPCM.bankCount++;
        tempPCM.bank.add(new VgmPcmData());

        if ((Type & 0x40) == 0)
            bankSize = dataSize;
        else
            bankSize = ByteUtil.readLeInt(vgmBuf, adr + 1);

        byte[] newData = new byte[tempPCM.dataSize + bankSize];
        if (tempPCM.data != null && tempPCM.data.length > 0)
            System.arraycopy(tempPCM.data, 0, newData, 0, tempPCM.data.length);
        tempPCM.data = newData;

        tempBnk = tempPCM.bank.get(curBnk);
        tempBnk.dataStart = tempPCM.dataSize;
        tempBnk.data = new byte[bankSize];
        boolean retVal = true;
        if ((Type & 0x40) == 0) {
            tempBnk.dataSize = dataSize;
            for (int i = 0; i < dataSize; i++) {
                tempPCM.data[i + tempBnk.dataStart] = vgmBuf[adr + i];
                tempBnk.data[i] = vgmBuf[adr + i];
            }
        } else {
            retVal = decompressDataBlk(tempBnk, dataSize, adr);
            if (!retVal) {
                tempBnk.data = null;
                tempBnk.dataSize = 0x00;
            } else {
                System.arraycopy(tempBnk.data, 0, tempPCM.data, tempBnk.dataStart, bankSize);
            }
        }
        if (retVal)
            tempPCM.dataSize += bankSize;

        // realloc may've moved the Bank block, so refresh all DAC Streams
        for (curDAC = 0x00; curDAC < dacCtrlUsed; curDAC++) {
            if (dacCtrl[dacCtrlUsg[curDAC] & 0xff].bank == bnkType)
                dacControl.refreshData(dacCtrlUsg[curDAC] & 0xff, tempPCM.data, tempPCM.dataSize);
        }
    }

    private boolean decompressDataBlk(VgmPcmData bank, int dataSize, int adr) {
        int comprType;
        int bitDec;
        int bitCmp;
        int cmpSubType;
        int addVal;
        int inPos;
        int inDataEnd;
        int outPos;
        int outDataEnd;
        int inVal;
        int outVal = 0;
        int valSize;
        int inShift;
        int outShift;
        int ent1B = 0;
        int ent2B = 0;
//#if defined(_DEBUG) && defined(WIN32)
//        UINT32 Time;
//#endif

        // ReadBits Variables
        int bitsToRead;
        int bitReadVal;
        int inValB;
        int bitMask;
        int outBit;

        // Variables for DPCM
        int outMask;

//#if defined(_DEBUG) && defined(WIN32)
//        Time = GetTickCount();
//#endif
        comprType = vgmBuf[adr + 0] & 0xff;
        bank.dataSize = ByteUtil.readLeInt(vgmBuf, adr + 1);

        switch (comprType) {
        case 0x00:  // n-Bit compression
            bitDec = vgmBuf[adr + 5] & 0xff;
            bitCmp = vgmBuf[adr + 6] & 0xff;
            cmpSubType = vgmBuf[adr + 7] & 0xff;
            addVal = ByteUtil.readLeShort(vgmBuf, adr + 8) & 0xffff;

            if (cmpSubType == 0x02) {
                //bank.dataSize = 0x00;
                //return false;

                ent1B = 0; // Big Endian note: Those are stored : LE and converted when reading.
                ent2B = 0;
                if (pcmTbl.entryCount == 0) {
                    bank.dataSize = 0x00;
//logger.log(Level.ERROR, "loading table-compressed data block! No table loaded!");
                    return false;
                } else if (bitDec != pcmTbl.bitDec || bitCmp != pcmTbl.bitCmp) {
                    bank.dataSize = 0x00;
//logger.log(Level.WARNING, "data block and loaded value table incompatible!");
                    return false;
                }
            }

            valSize = (byte) ((bitDec + 7) / 8);
            inPos = adr + 0x0A;
            inDataEnd = adr + dataSize;
            inShift = 0;
            outShift = bitDec - bitCmp;
//            outDataEnd = bank.Data + bank.dataSize;
            outDataEnd = bank.dataSize;

            for (outPos = 0; outPos < outDataEnd && inPos < inDataEnd; outPos += valSize) {
                //inVal = ReadBits(Data, inPos, &inShift, bitCmp);
                // inlined - instanceof 30% faster
                outBit = 0x00;
                inVal = 0x0000;
                bitsToRead = bitCmp;
                while (bitsToRead != 0) {
                    bitReadVal = Math.min(bitsToRead, 8);
                    bitsToRead -= bitReadVal;
                    bitMask = (1 << bitReadVal) - 1;

                    inShift += bitReadVal;
                    inValB = ((vgmBuf[inPos] & 0xff) << inShift >> 8) & bitMask;
                    if (inShift >= 8) {
                        inShift -= 8;
                        inPos++;
                        if (inShift != 0)
                            inValB |= ((vgmBuf[inPos] & 0xff) << inShift >> 8) & bitMask;
                    }

                    inVal |= inValB << outBit;
                    outBit += bitReadVal;
                }

                outVal = switch (cmpSubType) {
                    case 0x00 ->  // Copy
                            inVal + addVal;
                    case 0x01 ->  // Shift Left
                            (inVal << outShift) + addVal;
                    case 0x02 ->  // Table
                        //#endif
                            switch (valSize) {
                                case 0x01 -> pcmTbl.entries[ent1B + inVal] & 0xff;
                                case 0x02 ->
//#ifndef BIG_ENDIAN
//                        outVal = ent2B[inVal];
//#else
                                        (pcmTbl.entries[ent2B + inVal * 2] & 0xff) + (pcmTbl.entries[ent2B + inVal * 2 + 1] & 0xff) * 0x100;
                                default -> outVal;
                            };
                    default -> outVal;
                };

//#ifndef BIG_ENDIAN
//                //memcpy(outPos, &outVal, valSize);
//                if (valSize == 0x01)
//                    *((UINT8 *) outPos) =(UINT8) outVal;
//                else //if (valSize == 0x02)
//                    *((UINT16 *) outPos) =(UINT16) outVal;
//#else
                if (valSize == 0x01) {
                    bank.data[outPos] = (byte) outVal;
                } else { // if (valSize == 0x02)
                    bank.data[outPos + 0x00] = (byte) ((outVal & 0x00FF) >> 0);
                    bank.data[outPos + 0x01] = (byte) ((outVal & 0xff00) >> 8);
                }
//#endif
            }
            break;
        case 0x01:  // Delta-PCM
            bitDec = vgmBuf[adr + 5] & 0xff; // data[0x05];
            bitCmp = vgmBuf[adr + 6] & 0xff; // data[0x06];
            outVal = ByteUtil.readLeShort(vgmBuf, adr + 8) & 0xffff; // ReadLE16(&Data[0x08]);

            ent1B = 0; // (UINT8*)PCMTbl.Entries;
            ent2B = 0; // (UINT16*)PCMTbl.Entries;
            if (pcmTbl.entryCount == 0) {
                bank.dataSize = 0x00;
                //printf("Error loading table-compressed data block! No table loaded!\n");
                return false;
            } else if (bitDec != pcmTbl.bitDec || bitCmp != pcmTbl.bitCmp) {
                bank.dataSize = 0x00;
                //printf("Warning! data block and loaded value table incompatible!\n");
                return false;
            }

            valSize = (bitDec + 7) / 8;
            outMask = (1 << bitDec) - 1;
            inPos = adr + 0xa;
            inDataEnd = adr + dataSize;
            inShift = 0;
            outShift = bitDec - bitCmp;
            outDataEnd = bank.dataSize;// bank.Data + bank.dataSize;
            addVal = 0x0000;

            for (outPos = 0; outPos < outDataEnd && inPos < inDataEnd; outPos += valSize) {
                // inVal = ReadBits(Data, inPos, &inShift, bitCmp);
                // inlined - instanceof 30% faster
                outBit = 0x00;
                inVal = 0x0000;
                bitsToRead = bitCmp;
                while (bitsToRead != 0) {
                    bitReadVal = Math.min(bitsToRead, 8);
                    bitsToRead -= bitReadVal;
                    bitMask = (1 << bitReadVal) - 1;

                    inShift += bitReadVal;
                    inValB = ((vgmBuf[inPos] & 0xff) << inShift >> 8) & bitMask;
                    if (inShift >= 8) {
                        inShift -= 8;
                        inPos++;
                        if (inShift != 0)
                            inValB |= ((vgmBuf[inPos] & 0xff) << inShift >> 8) & bitMask;
                    }

                    inVal |= inValB << outBit;
                    outBit += bitReadVal;
                }

                switch (valSize) {
                case 0x01:
                    addVal = pcmTbl.entries[ent1B + inVal] & 0xff;
                    outVal += addVal;
                    outVal &= outMask;
                    bank.data[outPos] = (byte) outVal;
                    break;
                case 0x02:
//#ifndef BIG_ENDIAN
//                    addVal = ent2B[inVal];
//#else
                    addVal = (pcmTbl.entries[ent2B + inVal] & 0xff) + (pcmTbl.entries[ent2B + inVal + 1] & 0xff) * 0x100;
                    //addVal = ReadLE16((UINT8*)&ent2B[inVal]);
//#endif
                    outVal += addVal;
                    outVal &= outMask;
//#ifndef BIG_ENDIAN
//                    *((UINT16*)outPos) = (UINT16)outVal;
//#else
                    bank.data[outPos + 0x00] = (byte) ((outVal & 0x00ff) >> 0);
                    bank.data[outPos + 0x01] = (byte) ((outVal & 0xff00) >> 8);
//#endif
                    break;
                }
            }
            break;
        default:
            //printf("Error: Unknown data block compression!\n");
            return false;
        }

//#if defined(_DEBUG) && defined(WIN32)
//        Time = GetTickCount() - Time;
//        printf("Decompression Time: %lu\n", Time);
//#endif

        return true;
    }

    private void readPCMTable(int dataSize, int adr) {
        int valSize;
        int tblSize;

        pcmTbl.comprType = vgmBuf[adr + 0] & 0xff;
        pcmTbl.cmpSubType = vgmBuf[adr + 1] & 0xff;
        pcmTbl.bitDec = vgmBuf[adr + 2] & 0xff;
        pcmTbl.bitCmp = vgmBuf[adr + 3] & 0xff;
        pcmTbl.entryCount = ByteUtil.readLeShort(vgmBuf, adr + 4) & 0xffff;

        valSize = (pcmTbl.bitDec + 7) / 8;
        tblSize = pcmTbl.entryCount * valSize;

        pcmTbl.entries = new byte[tblSize];
        for (int i = 0; i < tblSize; i++) pcmTbl.entries[i] = vgmBuf[adr + 6 + i];

        if (dataSize < 0x06 + tblSize) {
logger.log(Level.TRACE, "Bad PCM Table Length!");
        }
    }

    private int getDACFromPCMBank() {
        // for Ym2612 DAC data only
//        VgmPcmBank* TempPCM;
//        UINT32 CurBnk;
        int dataPos;

//        TempPCM = &PCMBank[0x00];
//        dataPos = TempPCM -> dataPos;
//        for (CurBnk = 0x00; CurBnk < TempPCM -> BankCount; CurBnk++) {
//            if (dataPos < TempPCM -> Bank[CurBnk].DataSize) {
//                if (TempPCM -> dataPos < TempPCM -> dataSize)
//                    TempPCM -> dataPos++;
//                return TempPCM -> Bank[CurBnk].Data[dataPos];
//            }
//            dataPos -= TempPCM -> Bank[CurBnk].DataSize;
//        }
//        return 0x80;

        dataPos = pcmBank[0x00].dataPos;
        if (dataPos >= pcmBank[0x00].dataSize)
            return 0x80;

        pcmBank[0x00].dataPos++;
        return pcmBank[0x00].bank.getFirst().data[dataPos] & 0xff;
    }

    /** @return nullable */
    private Integer getPCMAddressFromPCMBank(int type, int dataPos) {
        if (type >= PCM_BANK_COUNT)
            return null;

        if (dataPos >= pcmBank[type].dataSize)
            return null;

        return dataPos;
    }

    private boolean getInformationHeader() {
        List<String> chips = new ArrayList<>();
        ivgm.setUsedChips("");

        sn76489ClockValue = 0; // defaultSN76489ClockValue;
        ym2612ClockValue = 0; // defaultYM2612ClockValue;
        ym2151ClockValue = 0;
        segaPCMClockValue = 0;
        ym2203ClockValue = 0;
        ym2608ClockValue = 0;
        ym2610ClockValue = 0;
        ym3812ClockValue = 0;
        ymF262ClockValue = 0;
        rf5C68ClockValue = 0;
        rf5C164ClockValue = 0; // defaultRF5C164ClockValue;
        pwmClockValue = 0; // defaultPWMClockValue;
        okiM6258ClockValue = 0; // defaultOKIM6258ClockValue;
        c140ClockValue = 0; // defaultC140ClockValue;
        okiM6295ClockValue = 0; // defaultOKIM6295ClockValue;
        ay8910ClockValue = 0;
        ym2413ClockValue = 0;
        huC6280ClockValue = 0;
        k054539ClockValue = 0;
        nesClockValue = 0;
        multiPCMClockValue = 0;
        uPD7759ClockValue = 0;
        saa1099ClockValue = 0;
        x1_010ClockValue = 0;
        wSwanClockValue = 0;
        es5503ClockValue = 0;
        es5505ClockValue = 0;
        msm5205ClockValue = 0;
        msm5232ClockValue = 0;
        msm5205Flags = 0x06;
        volumeModifier = 0;

        // Check if the header is large enough to read
        if (vgmBuf.length < 0x40) return false;

        // Get information from the header

        int vgm = ByteUtil.readLeInt(vgmBuf, 0x00);
        if (vgm != FCC_VGM) return false;

        vgmEof = ByteUtil.readLeInt(vgmBuf, 0x04) + 4;

        int version = ByteUtil.readLeInt(vgmBuf, 0x08);
        ivgm.setVersion("%d.%d%d".formatted((version & 0xf00) / 0x100, (version & 0xf0) / 0x10, (version & 0xf)));
        // Version Check
        if (version < 0x0101) {
            logger.log(Level.WARNING, "This file instanceof older version(%s).".formatted(ivgm.getVersion()));
            //return false;
        }

        // in a clock word, bit 30 (0x4000_0000) is the dual chip flag and bit 31 (0x8000_0000)
        // selects a chip variant (T6W28, VRC7, YM2610B, ...), per vgmspec171 "Dual Chip Support".
        int SN76489clock = ByteUtil.readLeInt(vgmBuf, 0x0c);
        if (SN76489clock != 0) {
            sn76489ClockValue = SN76489clock & 0x3fff_ffff;
            sn76489DualChipFlag = (SN76489clock & 0x4000_0000) != 0;
            sn76489NGPFlag = (SN76489clock & 0x8000_0000) != 0;
            if (version < 0x0150) {
                sn76489Option = new Object[] {
                        9,
                        0,
                        16,
                        0
                };
            } else {
                sn76489Option = new Object[] {
                        vgmBuf[0x28] & 0xff,
                        vgmBuf[0x29] & 0xff,
                        vgmBuf[0x2a] & 0xff,
                        vgmBuf[0x2b] & 0xff
                };
            }
            if (sn76489DualChipFlag) chips.add("SN76489x2");
            else chips.add("SN76489");
        }

        int YM2413clock = ByteUtil.readLeInt(vgmBuf, 0x10);
        if (YM2413clock != 0) {
            ym2413ClockValue = YM2413clock & 0x3fff_ffff;
            ym2413DualChipFlag = (YM2413clock & 0x4000_0000) != 0;
            ym2413VRC7Flag = (YM2413clock & 0x8000_0000) != 0;
            if (!ym2413VRC7Flag) {
                if (ym2413DualChipFlag) chips.add("YM2413x2");
                else chips.add("YM2413");
            } else {
                if (ym2413DualChipFlag) chips.add("VRC7x2");
                else chips.add("VRC7");
            }
        }

        if (version == 0x0101) {
            int YM2612clock = ByteUtil.readLeInt(vgmBuf, 0x10);
            if (YM2612clock != 0) {
                ym2612ClockValue = YM2612clock & 0x3fff_ffff;
                ym2612DualChipFlag = (YM2612clock & 0x4000_0000) != 0;
                if (ym2612DualChipFlag) chips.add("YM2612x2");
                else chips.add("Ym2612");
            }

            int YM2151clock = ByteUtil.readLeInt(vgmBuf, 0x10);
            if (YM2151clock != 0) {
                ym2151ClockValue = YM2151clock & 0x3fff_ffff;
                ym2151DualChipFlag = (YM2151clock & 0x4000_0000) != 0;
                if (ym2151DualChipFlag) chips.add("YM2151x2");
                else chips.add("YM2151");
            }
        }

        ivgm.setTotalCounter(ByteUtil.readLeInt(vgmBuf, 0x18));
        if (ivgm.getTotalCounter() < 0) return false;

        vgmLoopOffset = ByteUtil.readLeInt(vgmBuf, 0x1c);

        ivgm.setLoopCounter(ByteUtil.readLeInt(vgmBuf, 0x20));

        if (version > 0x0101) {

            int YM2612clock = ByteUtil.readLeInt(vgmBuf, 0x2c);
            if (YM2612clock != 0) {
                ym2612ClockValue = YM2612clock & 0x3fff_ffff;
                ym2612DualChipFlag = (YM2612clock & 0x4000_0000) != 0;
                if (ym2612DualChipFlag) chips.add("YM2612x2");
                else chips.add("Ym2612");
            }

            int YM2151clock = ByteUtil.readLeInt(vgmBuf, 0x30);
            if (YM2151clock != 0) {
                ym2151ClockValue = YM2151clock & 0x3fff_ffff;
                ym2151DualChipFlag = (YM2151clock & 0x4000_0000) != 0;
                if (ym2151DualChipFlag) chips.add("YM2151x2");
                else chips.add("YM2151");
            }

            //setYM2151Hosei();

            vgmDataOffset = ByteUtil.readLeInt(vgmBuf, 0x34);
            if (vgmDataOffset == 0) {
                vgmDataOffset = 0x40;
            } else {
                vgmDataOffset += 0x34;
            }

            // the header ends where the extra header starts (v1.70), the rest is not chip clocks
            int headerLimit = vgmDataOffset;
            if (headerLimit > 0xbc) {
                int extraHeaderOffset = ByteUtil.readLeInt(vgmBuf, 0xbc);
                if (extraHeaderOffset != 0)
                    headerLimit = Math.min(headerLimit, 0xbc + extraHeaderOffset);
            }

            //if (version >= 0x0151)
            {
                if (headerLimit > 0x38) {
                    int segaPCMClock = ByteUtil.readLeInt(vgmBuf, 0x38);
                    int SPCMInterface = ByteUtil.readLeInt(vgmBuf, 0x3c);
                    if (segaPCMClock != 0 && SPCMInterface != 0) {
                        chips.add("Sega PCM");
                        segaPCMClockValue = segaPCMClock;
                        segaPCMInterface = SPCMInterface;
                    }
                }

                if (headerLimit > 0x40) {
                    int RF5C68clock = ByteUtil.readLeInt(vgmBuf, 0x40);
                    if (RF5C68clock != 0) {
                        rf5C68ClockValue = RF5C68clock & 0x3fff_ffff;
                        rf5C68DualChipFlag = (RF5C68clock & 0x4000_0000) != 0;
                        if (rf5C68DualChipFlag) chips.add("RF5C68x2");
                        else chips.add("RF5C68");
                    }
                }

                if (headerLimit > 0x44) {
                    int YM2203clock = ByteUtil.readLeInt(vgmBuf, 0x44);
                    if (YM2203clock != 0) {
                        ym2203ClockValue = YM2203clock & 0x3fff_ffff;
                        ym2203DualChipFlag = (YM2203clock & 0x4000_0000) != 0;
                        if (ym2203DualChipFlag) chips.add("YM2203x2");
                        else chips.add("YM2203");
                    }
                }

                if (headerLimit > 0x48) {
                    int YM2608clock = ByteUtil.readLeInt(vgmBuf, 0x48);
                    if (YM2608clock != 0) {
                        ym2608ClockValue = YM2608clock & 0x3fff_ffff;
                        ym2608DualChipFlag = (YM2608clock & 0x4000_0000) != 0;
                        if (ym2608DualChipFlag) chips.add("YM2608x2");
                        else chips.add("YM2608");

                        ivgm.updateRamTypeYm2608(vgmBuf, vgmDataOffset);
                    }
                }

                if (headerLimit > 0x4c) {
                    int YM2610Bclock = ByteUtil.readLeInt(vgmBuf, 0x4c);
                    if (YM2610Bclock != 0) {
                        ym2610ClockValue = YM2610Bclock & 0x3fff_ffff;
                        ym2610DualChipFlag = (YM2610Bclock & 0x4000_0000) != 0;
                        ym2610BFlag = (YM2610Bclock & 0x8000_0000) != 0;
                        String ym2610Name = ym2610BFlag ? "YM2610B" : "YM2610";
                        chips.add(ym2610DualChipFlag ? ym2610Name + "x2" : ym2610Name);
                    }
                }

                if (headerLimit > 0x50) {
                    int YM3812clock = ByteUtil.readLeInt(vgmBuf, 0x50);
                    if (YM3812clock != 0) {
                        ym3812ClockValue = YM3812clock & 0x3fff_ffff;
                        ym3812DualChipFlag = (YM3812clock & 0x4000_0000) != 0;
                        if (ym2610DualChipFlag) chips.add("YM3812x2");
                        else chips.add("YM3812");
                    }
                }

                if (headerLimit > 0x54) {
                    int YM3526clock = ByteUtil.readLeInt(vgmBuf, 0x54);
                    if (YM3526clock != 0) {
                        ym3526ClockValue = YM3526clock & 0x3fff_ffff;
                        ym3526DualChipFlag = (YM3526clock & 0x4000_0000) != 0;
                        if (ym3526DualChipFlag) chips.add("YM3526x2");
                        else chips.add("YM3526");
                    }
                }

                if (headerLimit > 0x58) {
                    int Y8950clock = ByteUtil.readLeInt(vgmBuf, 0x58);
                    if (Y8950clock != 0) {
                        y8950ClockValue = Y8950clock & 0x3fff_ffff;
                        y8950DualChipFlag = (Y8950clock & 0x4000_0000) != 0;
                        if (y8950DualChipFlag) chips.add("Y8950x2");
                        else chips.add("Y8950");
                    }
                }

                if (headerLimit > 0x5c) {
                    int YMF262clock = ByteUtil.readLeInt(vgmBuf, 0x5c);
                    if (YMF262clock != 0) {
                        ymF262ClockValue = YMF262clock & 0x3fff_ffff;
                        ymF262DualChipFlag = (YMF262clock & 0x4000_0000) != 0;
                        if (ymF262DualChipFlag) chips.add("YMF262x2");
                        else chips.add("YMF262");
                    }
                }

                if (headerLimit > 0x60) {
                    int YMF278Bclock = ByteUtil.readLeInt(vgmBuf, 0x60);
                    if (YMF278Bclock != 0) {
                        ymF278BClockValue = YMF278Bclock & 0x3fff_ffff;
                        ymF278BDualChipFlag = (YMF278Bclock & 0x4000_0000) != 0;
                        if (ymF278BDualChipFlag) chips.add("YMF278Bx2");
                        else chips.add("YMF278B");
                    }
                }

                if (headerLimit > 0x64) {
                    int YMF271clock = ByteUtil.readLeInt(vgmBuf, 0x64);
                    if (YMF271clock != 0) {
                        ymF271ClockValue = YMF271clock & 0x3fff_ffff;
                        ymF271DualChipFlag = (YMF271clock & 0x4000_0000) != 0;
                        if (ymF271DualChipFlag) chips.add("YMF271x2");
                        else chips.add("YMF271");
                    }
                }

                if (headerLimit > 0x68) {
                    int YMZ280Bclock = ByteUtil.readLeInt(vgmBuf, 0x68);
                    if (YMZ280Bclock != 0) {
                        ymZ280BClockValue = YMZ280Bclock & 0x3fff_ffff;
                        ymZ280BDualChipFlag = (YMZ280Bclock & 0x4000_0000) != 0;
                        if (ymZ280BDualChipFlag) chips.add("YMZ280Bx2");
                        else chips.add("YMZ280B");
                    }
                }

                if (headerLimit > 0x6c) {
                    int RF5C164clock = ByteUtil.readLeInt(vgmBuf, 0x6c);
                    if (RF5C164clock != 0) {
                        rf5C164ClockValue = RF5C164clock & 0x3fff_ffff;
                        rf5C164DualChipFlag = (RF5C164clock & 0x4000_0000) != 0;
                        if (rf5C164DualChipFlag) chips.add("RF5C164x2");
                        else chips.add("RF5C164");
                    }
                }


                if (headerLimit > 0x70) {
                    int PWMclock = ByteUtil.readLeInt(vgmBuf, 0x70);
                    if (PWMclock != 0) {
                        chips.add("PWM");
                        pwmClockValue = PWMclock;
                    }
                }

                if (headerLimit > 0x74) {
                    int AY8910clock = ByteUtil.readLeInt(vgmBuf, 0x74);
                    if (AY8910clock != 0) {
                        ay8910ClockValue = AY8910clock & 0x3fff_ffff;
                        ay8910DualChipFlag = (AY8910clock & 0x4000_0000) != 0;
                        if (ay8910DualChipFlag) chips.add("AY8910x2");
                        else chips.add("AY8910");
                    }
                }

                if (headerLimit > 0x7c) {
                    volumeModifier = vgmBuf[0x7c] & 0xff;
                }
            }

//            okiM6258ClockValue = 0;
//            huC6280ClockValue = 0;
//            okiM6295ClockValue = 0;

//            if (version >= 0x0161)
            {
                if (headerLimit > 0x80) {
                    int DMGclock = ByteUtil.readLeInt(vgmBuf, 0x80);
                    if (DMGclock != 0) {
                        dmgClockValue = DMGclock & 0x3fff_ffff;
                        dmgDualChipFlag = (DMGclock & 0x4000_0000) != 0;
                        if (dmgDualChipFlag) chips.add("DMGx2");
                        else chips.add("DMG");
                    }
                }

                if (headerLimit > 0x84) {
                    int NESclock = ByteUtil.readLeInt(vgmBuf, 0x84);
                    if (NESclock != 0) {
                        nesClockValue = NESclock & 0xbfff_ffff;
                        nesDualChipFlag = (NESclock & 0x4000_0000) != 0;
                        if (nesDualChipFlag) chips.add("NES_APUx2");
                        else chips.add("NES_APU");
                    }
                }

                if (headerLimit > 0x88) {
                    int MultiPCMclock = ByteUtil.readLeInt(vgmBuf, 0x88);
                    if (MultiPCMclock != 0) {
                        multiPCMClockValue = MultiPCMclock & 0x3fff_ffff;
                        multiPCMDualChipFlag = (MultiPCMclock & 0x4000_0000) != 0;
                        if (multiPCMDualChipFlag) chips.add("MultiPCMx2");
                        else chips.add("MultiPCM");
                    }
                }

                if (headerLimit > 0x8c) {
                    int uPD7759clock = ByteUtil.readLeInt(vgmBuf, 0x8c);
                    if (uPD7759clock != 0) {
                        uPD7759ClockValue = uPD7759clock & 0xbfff_ffff;
                        uPD7759DualChipFlag = (uPD7759clock & 0x4000_0000) != 0;
                        if (uPD7759DualChipFlag) chips.add("uPD7759x2");
                        else chips.add("uPD7759");
                    }
                }

                if (headerLimit > 0x90) {
                    int OKIM6258clock = ByteUtil.readLeInt(vgmBuf, 0x90);
                    if (OKIM6258clock != 0) {
                        chips.add("OKIM6258");
                        okiM6258ClockValue = OKIM6258clock;
                        okiM6258Type = vgmBuf[0x94] & 0xff;
                    }
                }

                if (headerLimit > 0x9c) {
                    int K051649clock = ByteUtil.readLeInt(vgmBuf, 0x9c);
                    if (K051649clock != 0) {
                        k051649ClockValue = K051649clock & 0x3fff_ffff;
                        k051649DualChipFlag = (K051649clock & 0x4000_0000) != 0;
                        if (k051649DualChipFlag) chips.add("K051649x2");
                        else chips.add("K051649");
                    }
                }

                if (headerLimit > 0xa0) {
                    int K054539clock = ByteUtil.readLeInt(vgmBuf, 0xa0);
                    if (K054539clock != 0) {
                        k054539ClockValue = K054539clock & 0x3fff_ffff;
                        k054539DualChipFlag = (K054539clock & 0x4000_0000) != 0;
                        if (k054539DualChipFlag) chips.add("K054539x2");
                        else chips.add("K054539");
                        k054539Flags = vgmBuf[0x95] & 0xff;
                    }
                }

                if (headerLimit > 0xa4) {

                    int HuC6280clock = ByteUtil.readLeInt(vgmBuf, 0xa4);
                    if (HuC6280clock != 0) {
                        chips.add("OotakeHuC6280");
                        huC6280ClockValue = HuC6280clock;
                    }
                }

                if (headerLimit > 0xa8) {

                    int C140clock = ByteUtil.readLeInt(vgmBuf, 0xa8);
                    if (C140clock != 0) {
                        c140ClockValue = C140clock & 0x3fff_ffff;
                        c140DualChipFlag = (C140clock & 0x4000_0000) != 0;
                        if (c140DualChipFlag) chips.add("C140x2");
                        else chips.add("C140");

                        C140Type = vgmBuf[0x96] & 0xff;
                    }
                }

                if (headerLimit > 0xac) {

                    int k053260clock = ByteUtil.readLeInt(vgmBuf, 0xac);
                    if (k053260clock != 0) {
                        k053260ClockValue = k053260clock & 0x3fff_ffff;
                        k053260DualChipFlag = (k053260clock & 0x4000_0000) != 0;
                        if (k053260DualChipFlag) chips.add("K053260x2");
                        else chips.add("K053260");
                    }
                }

                if (headerLimit > 0xb0) {

                    int pokeyClock = ByteUtil.readLeInt(vgmBuf, 0xb0);
                    if (pokeyClock != 0) {
                        pokeyClockValue = pokeyClock & 0x3fff_ffff;
                        pokeyDualChipFlag = (pokeyClock & 0x4000_0000) != 0;
                        if (pokeyDualChipFlag) chips.add("POKEYx2");
                        else chips.add("POKEY");
                    }
                }

                if (headerLimit > 0xb4) {

                    int qSoundClock = ByteUtil.readLeInt(vgmBuf, 0xb4);
                    if (qSoundClock != 0) {
                        chips.add("QSound");
                        qSoundClockValue = qSoundClock;
                    }
                }

                if (headerLimit > 0x98) {
                    int okiM6295clock = ByteUtil.readLeInt(vgmBuf, 0x98);
                    if (okiM6295clock != 0) {
                        okiM6295DualChipFlag = (okiM6295clock & 0x4000_0000) != 0;
                        if (okiM6295DualChipFlag) {
                            chips.add("OKIM6295x2");
                        } else {
                            chips.add("OKIM6295");
                        }
                        okiM6295ClockValue = okiM6295clock & 0xbfff_ffff;
                    }
                }
            }
            if (version >= 0x0171) {
                if (headerLimit > 0xc0) {

                    int wSwanClock = ByteUtil.readLeInt(vgmBuf, 0xc0);
                    if (wSwanClock != 0) {
                        wSwanClockValue = wSwanClock & 0x3fff_ffff;
                        wSwanDualChipFlag = (wSwanClock & 0x4000_0000) != 0;
                        if (wSwanDualChipFlag) chips.add("WSwanx2");
                        else chips.add("WSwan");
                    }
                }

                if (headerLimit > 0xc8) {

                    int saa1099Clock = ByteUtil.readLeInt(vgmBuf, 0xc8);
                    if (saa1099Clock != 0) {
                        saa1099ClockValue = saa1099Clock & 0x3fff_ffff;
                        saA1099DualChipFlag = (saa1099Clock & 0x4000_0000) != 0;
                        if (saA1099DualChipFlag) chips.add("SAA1099x2");
                        else chips.add("SAA1099");
                    }
                }

                if (headerLimit > 0xcc) {

                    int es5503clock = ByteUtil.readLeInt(vgmBuf, 0xcc);
                    if (es5503clock != 0)
                    {
                        es5503ClockValue = es5503clock & 0x3fff_ffff; // def=7159090
                        es5503DualChipFlag = (es5503clock & 0x4000_0000) != 0;
                        es5503Ch = vgmBuf[0xd4];
                        //if (es5503Ch == 1) es5503Ch = 2;
                        if (es5503DualChipFlag) chips.add("ES5503x2");
                        else chips.add("ES5503");
                    }
                }

                if (headerLimit > 0xd0) {

                    int es5506clock = ByteUtil.readLeInt(vgmBuf, 0xd0);
                    if (es5506clock != 0) {
                        if ((es5506clock & 0x8000_0000) != 0) {
                            logger.log(Level.WARNING, "ES5506 is not supported");
                        } else {
                            es5505ClockValue = es5506clock & 0x3fff_ffff;
                            es5505DualChipFlag = (es5506clock & 0x4000_0000) != 0;
                            es5505Ch = vgmBuf[0xd5] & 0xff;
                            if (es5505DualChipFlag) chips.add("ES5505x2");
                            else chips.add("ES5505");
                        }
                    }
                }

                if (headerLimit > 0xd8) {

                    int x1_010Clock = ByteUtil.readLeInt(vgmBuf, 0xd8);
                    if (x1_010Clock != 0) {
                        x1_010ClockValue = x1_010Clock & 0x3fff_ffff;
                        x1_010DualChipFlag = (x1_010Clock & 0x4000_0000) != 0;
                        if (x1_010DualChipFlag) chips.add("X1_010x2");
                        else chips.add("X1_010");
                    }
                }

                if (headerLimit > 0xdc) {

                    int c352clock = ByteUtil.readLeInt(vgmBuf, 0xdc);
                    if (c352clock != 0) {
                        c352ClockValue = c352clock & 0x3fff_ffff;
                        c352DualChipFlag = (c352clock & 0x4000_0000) != 0;
                        if (c352DualChipFlag) chips.add("C352x2");
                        else chips.add("C352");

                        c352ClockDivider = vgmBuf[0xd6] & 0xff;
                    }
                }

                if (headerLimit > 0xe0) {

                    int ga20Clock = ByteUtil.readLeInt(vgmBuf, 0xe0);
                    if (ga20Clock != 0) {
                        ga20DualChipFlag = (ga20Clock & 0x4000_0000) != 0;
                        if (ga20DualChipFlag) {
                            ga20ClockValue = ga20Clock & 0x3fff_ffff;
                            chips.add("GA20x2");
                        } else {
                            ga20ClockValue = ga20Clock & 0xbfff_ffff;
                            chips.add("GA20");
                        }
                    }
                }

                // libvgm puts it at 0xF0 with its pins in 0xD7, the draft it was logged with before
                // (e.g. eito's Darius rip) put it at 0xEC, where libvgm now has the K005289, and
                // had no pins: those are the arcade's usual 384 kHz / 48, 4 bit
                int msm5205Clock = headerLimit > 0xf0 ? ByteUtil.readLeInt(vgmBuf, 0xf0) : 0;
                if (msm5205Clock != 0) {
                    msm5205Flags = (vgmBuf[0xd7] & 0x07) | ((msm5205Clock & 0x8000_0000) != 0 ? 0x80 : 0);
                } else if (headerLimit > 0xec) {
                    msm5205Clock = ByteUtil.readLeInt(vgmBuf, 0xec);
                    if ((msm5205Clock & 0x3fff_ffff) >= 1_000_000) msm5205Clock = 0; // a K005289 (3.58 MHz)
                }
                if (msm5205Clock != 0) {
                    msm5205ClockValue = msm5205Clock & 0x3fff_ffff;
                    msm5205DualChipFlag = (msm5205Clock & 0x4000_0000) != 0;
                    String name = (msm5205Flags & 0x80) != 0 ? "MSM6585" : "MSM5205";
                    chips.add(msm5205DualChipFlag ? name + "x2" : name);
                }

                if (headerLimit > 0xf4) {
                    int msm5232Clock = ByteUtil.readLeInt(vgmBuf, 0xf4);
                    if (msm5232Clock != 0) {
                        msm5232ClockValue = msm5232Clock & 0x3fff_ffff;
                        msm5232DualChipFlag = (msm5232Clock & 0x4000_0000) != 0;
                        chips.add(msm5232DualChipFlag ? "MSM5232x2" : "MSM5232");
                    }
                }
            }
        } else {
            vgmDataOffset = 0x40;
        }

        ivgm.setUsedChips(String.join(", ", chips));
logger.log(Level.INFO, "usedChips: " + ivgm.getUsedChips());

        int vgmGd3 = ByteUtil.readLeInt(vgmBuf, 0x14);
        if (vgmGd3 != 0) {
            int vgmGd3Id = ByteUtil.readLeInt(vgmBuf, vgmGd3 + 0x14);
            if (vgmGd3Id != FCC_GD3) return false;
            ivgm.updateMetaData(vgmBuf, vgmGd3);
        }

        return true;
    }

    static class VgmPcmData {
        int dataSize;
        byte[] data;
        int dataStart;
    }

    static class VgmPcmBank {
        int bankCount;
        final List<VgmPcmData> bank = new ArrayList<>();
        int dataSize;
        byte[] data;
        int dataPos;
        int bnkPos;
    }

    static class DacCtrlData {
        boolean enable;
        int bank;
    }

    static class PcmBankTbl {
        int comprType;
        int cmpSubType;
        int bitDec;
        int bitCmp;
        int entryCount;
        byte[] entries;
    }

    public interface IVgm {
        void setPanSn76489(int chipId, int data);
        void writeSn76489(int chipId, int data);
        void writeAy8910(int chipId, int addr, int data);
        void writeDmg(int chipId, int addr, int data);
        void writeNes(int chipId, int addr, int data);
        void writeMultiPcm(int chipId, int addr, int data);
        void writeUpd7759(int chipId, int addr, int data);
        void setBankMultiPcm(int chipId, int ch, int addr);
        void writeQSound(int chipId, int mm, int ll, int rr);
        void writeX1_010(int chipId, int mm, int ll, int rr);
        void writeYm2413(int chipId, int addr, int data);
        void writeYm3812(int chipId, int addr, int data);
        void writeHuC6280(int chipId, int addr, int data);
        void writeGa20(int chipId, int addr, int data);
        void writeYm2612(int chipId, int port, int addr, int data, int frameCounter);
        void writeYm2203(int chipId, int addr, int data);
        void writeYm2608(int chipId, int port, int addr, int data);
        void updateRamTypeYm2608(byte[] vgmBuf, int vgmDataOffset);
        void writeYm2610(int chipId, int port, int addr, int data);
        void writeYmF262(int chipId, int port, int addr, int data);
        void writeYm3526(int chipId, int addr, int data);
        void writeY8950(int chipId, int addr, int data);
        void writeYmZ280B(int chipId, int addr, int data);
        void writeYmF271(int chipId, int port, int addr, int data);
        void writeYmF278B(int chipId, int port, int addr, int data);
        void writeYm2151(int chipId, int port, int addr, int data, int correction, int frameCounter);
        void writeOkiM6258(int chipId, int port, int data);
        void writeOkiM6295(int chipId, int port, int data);
        void writeSaa1099(int chipId, int addr, int data);
        void writeWSwan(int chipId, int port, int data);
        void writeMemWSwan(int chipId, int port, int data);
        void writePokey(int chipId, int port, int data);
        void writeSegaPcm(int chipId, int offset, int data);
        void writePcmSegaPcm(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmYm2608(int chipId, byte[] vgmBuf, int vgmAdr, int bLen, int startAddress);
        void writeAdpcmAYm2610(int chipId, byte[] vgmBuf, int vgmAdr, int bLen, int startAddress, int romSize);
        void writeAdpcmBYm2610(int chipId, byte[] vgmBuf, int vgmAdr, int bLen, int startAddress, int romSize);
        void writePcmYmF278B(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writeRamYmF278B(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmYmF271(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmYmZ280B(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmY8950(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmMultiPcm(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmUpd7759(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmOkiM6295(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmK054539(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmC140(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmK053260(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmQSound(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmX1_010(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmC352(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmGa20(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePcmRf5C68(int chipId, int offset, int length, byte[] buf, int srcOffset);
        void writePcmRf5C164(int chipId, int offset, int length, byte[] buf, int srcOffset);
        void writePcmNes(int chipId, int stAdr, int dataSize, byte[] vgmBuf, int vgmAdr);
        void writePcmEs5503(int chipId, int offset, int length, byte[] buf, int srcOffset);
        void writePcmEs5505(int chipId, int romSize, int dataStart, int dataLength, byte[] romData, int srcStartAdr);
        void writePCMRamRf5C68(int chipId, int offset, int length, byte[] buf, int srcOffset);
        void writePCMRamRf5C164(int chipId, int offset, int length, byte[] buf, int srcOffset);
        void writeRf5C68(int chipId, int addr, int data);
        void writeMemoryRf5C68(int chipId, int offset, int data);
        void writeRf5C164(int chipId, int addr, int data);
        void writeMemoryRf5C164(int chipId, int offset, int data);
        void writePwm(int chipId, int addr, int data);
        void writeK051649(int chipId, int addr, int data);
        void writeK053260(int chipId, int addr, int data);
        void writeK054539(int chipId, int addr, int data);
        void writeC140(int chipId, int addr, int data);
        void writeEs5503(int chipId, int addr, int data);
        /** @param addr bit 7: 16 bit data, bit 6-0: byte offset */
        void writeEs5505(int chipId, int addr, int data);
        /** @param addr 0: reset, 1: data, 2: VCK, 4: prescaler, 5: bit width */
        void writeMsm5205(int chipId, int addr, int data);
        /** @param addr 00-07: voices, 08-0d: envelopes and controls, 10-1f: libvgm's mixer, 20-23: clock */
        void writeMsm5232(int chipId, int addr, int data);
        void writeC352(int chipId, int addr, int data);
        int readHuC6280(int chipId, int addr);
        boolean isVirtual();
        int frameCounter();
        void dataBlock(boolean b);
        long getTotalCounter();
        void setTotalCounter(long v);
        void setLoopCounter(long v);
        int loop();
        void setUsedChips(String s);
        String getUsedChips();
        void setVersion(String s);
        String getVersion();
        void updateMetaData(byte[] b, Object... o);
    }

    public interface IDac {
        int DCTRL_LMODE_IGNORE = 0x00;
        int DCTRL_LMODE_CMDS = 0x01;
        int DCTRL_LMODE_MSEC = 0x02;
        int DCTRL_LMODE_TOEND = 0x03;
        int DCTRL_LMODE_BYTES = 0x0F;

        void refresh();
        int deviceStart(int chipId);
        void deviceReset(int chipId);
        void setupChip(int chipId, int chType, int chNum, int command);
        void setData(int chipId, byte[] data, int dataLen, int stepSize, int stepBase);
        void setFrequency(int chipId, int frequency);
        void start(int chipId, int dataPos, int lenMode, int length);
        void stop(int chipId);
        void refreshData(int chipId, byte[] data, int dataLen);
        void update(int chipId, int samples);
    }
}
