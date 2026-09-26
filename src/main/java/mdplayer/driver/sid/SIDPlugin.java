package mdplayer.driver.sid;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import mdplayer.Common;
import mdplayer.chips.SidChip;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.BasePlugin.HasSongNo;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


/**
 * SID (Commodore) Plugin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-08 nsano initial version <br>
 */
public class SIDPlugin extends BasePlugin<SidMdDriver> implements HasSongNo {

    private static final Logger logger = getLogger(SIDPlugin.class.getName());

    @Override
    public void prepare() {
        driverVirtual = new SidMdDriver(this);

        driverReal = null;
//        if (setting.getoutputDevice().deviceType != Common.DEV_Null) {
//            driverReal = new SidMdDriver(this);
//        }

        super.prepare();
        initChips();
    }

    @Override
    protected void initChips() {
        getDriver().fireEventHappened(chipRegister.chip(SidChip.class), "led.set", 0);

        driverVirtual.init(Common.EnmModel.VirtualModel,
                setting.getOutputDevice().getSampleRate() * setting.getLatencyEmulation() / 1000,
                setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000,
                song());
        if (driverReal != null) {
            driverReal.init(Common.EnmModel.RealModel,
                    setting.getOutputDevice().getSampleRate() * setting.getLatencySCCI() / 1000,
                    setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000,
                    song());
        }
    }

    /**
     * The sub-song to hand libsidplayfp, which numbers them from 1 and takes 0 as "the song this
     * tune starts at". Nothing selected (song no 0) has to mean that default: playing sub-song 1
     * of a tune that starts at another one lands on an unused, silent slot - Last_Ninja starts at
     * song 3, Wizball at 4, and both are dead quiet as song 1.
     */
    private int song() {
        return songNo <= 0 ? 0 : songNo + 1;
    }

    @Override
    public void setSongNo(int songNo) {
logger.log(Level.INFO, "songNo: " + songNo);
        // 0 origin, as every other HasSongNo plugin - initChips is what makes it 1 origin
        this.songNo = songNo;
    }

    /** nothing selected plays the tune's own start song, the header's {@code startSong} (1 origin) */
    @Override
    public int getSongNo() {
        if (songNo <= 0 && dataBuf != null && dataBuf.length >= 0x12) {
            return Math.max(1, ByteUtil.readBeShort(dataBuf, 0x10) & 0xffff) - 1;
        }
        return songNo;
    }
}
