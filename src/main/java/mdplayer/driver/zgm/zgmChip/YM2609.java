package mdplayer.driver.zgm.zgmChip;

import java.util.Map;

import mdplayer.ChipRegister;
import mdplayer.Common.EnmModel;
import mdplayer.Setting;
import mdplayer.driver.zgm.EnmZGMDevice;
import mdplayer.driver.zgm.ZgmDriver;


public class YM2609 extends ZgmChip {

    public YM2609(ChipRegister chipRegister, Setting setting, byte[] vgmBuf) {
        super(12 + 6 + 12 + 6 + 3);
        this.chipRegister = chipRegister;
        this.setting = setting;
        this.vgmBuf = vgmBuf;

        use = true;
        device = EnmZGMDevice.YM2609;
        name = "YM2609";
        model = EnmModel.VirtualModel;
        number = 0;
        hosei = 0;
    }

    @Override
    public int setUp(int chipIndex, int dataPos, Map<Integer, ZgmDriver.Command> cmdTable) {
        int next = super.setUp(chipIndex, dataPos, cmdTable);

        // there is no YM2609 emulator to send to, the 4 ports are skipped
        for (int port = 0; port < 4; port++) {
            cmdTable.put(defineInfo.commandNo + port, YM2609::sendPort);
        }
        return next;
    }

    private static int sendPort(int od, int vgmAdr) {
        // chipRegister.YM2609SetRegister(od, Audio.DriverSeqCounter, Index, port,
        // dataBuf[vgmAdr + 1], dataBuf[vgmAdr + 2]);
        return vgmAdr + 3;
    }
}
