package mdplayer.driver.zgm.zgmChip;

import java.util.Map;

import mdplayer.ChipRegister;
import mdplayer.Common.EnmModel;
import mdplayer.Setting;
import mdplayer.driver.zgm.EnmZGMDevice;
import mdplayer.driver.zgm.ZgmDriver;


public class Conductor extends ZgmChip {

    public Conductor(ChipRegister chipRegister, Setting setting, byte[] vgmBuf) {
        super(2);

        this.chipRegister = chipRegister;
        this.setting = setting;
        this.vgmBuf = vgmBuf;

        use = true;
        device = EnmZGMDevice.Conductor;
        name = "CONDUCTOR";
        model = EnmModel.VirtualModel;
        number = 0;
        hosei = 0;
    }

    @Override
    public int setUp(int chipIndex, int dataPos, Map<Integer, ZgmDriver.Command> cmdTable) {
        int next = super.setUp(chipIndex, dataPos, cmdTable);

        cmdTable.put(defineInfo.commandNo, Conductor::sendPort0);
        return next;
    }

    /** the conductor's commands carry nothing a player needs */
    private static int sendPort0(int od, int vgmAdr) {
        return vgmAdr + 3;
    }
}
