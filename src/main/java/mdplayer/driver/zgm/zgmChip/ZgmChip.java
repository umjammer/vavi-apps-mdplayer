package mdplayer.driver.zgm.zgmChip;

import java.util.Map;

import mdplayer.ChipRegister;
import mdplayer.Setting;
import mdplayer.driver.zgm.ZgmDriver;
import vavi.util.ByteUtil;


public abstract class ZgmChip extends Chip {

    ChipRegister chipRegister;

    Setting setting;

    byte[] vgmBuf;

    public String name;

    ZgmDriver.DefineInfo defineInfo;

    ZgmChip(int ch) {
        super(ch);

    }

    /** the n-th of this chip in the song, the mixer's chip id */
    public int getIndex() {
        return index;
    }

    public ZgmDriver.DefineInfo getDefineInfo() {
        return defineInfo;
    }

    /** @return the position of the next define */
    public int setUp(int chipIndex, int dataPos, Map<Integer, ZgmDriver.Command> cmdTable) {
        this.index = chipIndex;
        defineInfo = new ZgmDriver.DefineInfo();
        defineInfo.length = vgmBuf[dataPos + 0x03] & 0xff;
        defineInfo.chipIdentNo = ByteUtil.readLeInt(vgmBuf, dataPos + 0x4);
        defineInfo.commandNo = ByteUtil.readLeShort(vgmBuf, dataPos + 0x8);
        defineInfo.clock = ByteUtil.readLeInt(vgmBuf, dataPos + 0xa);
        defineInfo.option = null;
        if (defineInfo.length > 14) {
            defineInfo.option = new byte[defineInfo.length - 14];
            for (int j = 0; j < defineInfo.length - 14; j++) {
                defineInfo.option[j] = vgmBuf[dataPos + 0x0e + j];
            }
        }

        return dataPos + defineInfo.length;
    }
}
