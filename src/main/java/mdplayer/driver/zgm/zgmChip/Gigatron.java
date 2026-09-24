package mdplayer.driver.zgm.zgmChip;

import java.util.Map;

import mdplayer.ChipRegister;
import mdplayer.Common.EnmModel;
import mdplayer.Setting;
import mdplayer.chips.GigatronChip;
import mdplayer.driver.zgm.EnmZGMDevice;
import mdplayer.driver.zgm.ZgmDriver;


/**
 * Gigatron as mml2vgm writes it: {@code cmd, adrL, adrH, data}, the address being the machine's ram.
 */
public class Gigatron extends ZgmChip {

    public Gigatron(ChipRegister chipRegister, Setting setting, byte[] vgmBuf) {
        super(GigatronChip.CHANNELS);

        this.chipRegister = chipRegister;
        this.setting = setting;
        this.vgmBuf = vgmBuf;

        use = true;
        device = EnmZGMDevice.Gigatron;
        name = "Gigatron";
        model = EnmModel.VirtualModel;
        number = 0;
        hosei = 0;
    }

    @Override
    public int setUp(int chipIndex, int dataPos, Map<Integer, ZgmDriver.Command> cmdTable) {
        int next = super.setUp(chipIndex, dataPos, cmdTable);

        cmdTable.put(defineInfo.commandNo, this::sendPort0);
        return next;
    }

    private int sendPort0(int od, int vgmAdr) {
        int adr = (vgmBuf[vgmAdr + 1] & 0xff) | ((vgmBuf[vgmAdr + 2] & 0xff) << 8);
        int data = vgmBuf[vgmAdr + 3] & 0xff;
        chipRegister.chip(GigatronChip.class).write(index, adr, data, model);
        return vgmAdr + 4;
    }
}
