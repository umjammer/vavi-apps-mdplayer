package mdplayer.driver;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mdplayer.ChipRegister;
import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.chips.MidiPlugin;
import mdplayer.chips.RealChipPlugin;
import mdsound.MDSound;
import mdsound.MDSound.Chip;
import vavi.util.compat.Tuple;

import static java.lang.System.getLogger;


/**
 * BasePlugin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-08 nsano initial version <br>
 */
public abstract class BasePlugin<T extends BaseDriver> implements Plugin {

    private static final Logger logger = getLogger(BasePlugin.class.getName());

    protected final Setting setting = Setting.getInstance();

    public interface Compilable {
        void compile();
    }

    /** for spi, 0 origin */
    public interface HasSongNo {
        void setSongNo(int songNo);
    }

    public final MDSound mds;

    public final ChipRegister chipRegister;

    // TODO driver should be one, instruments should be separated virtual and real
    public T driverVirtual = null;

    public T driverReal = null;

    protected byte[] dataBuf = null;
    protected double speed;

    public boolean oneTimeReset = false;

    protected FileFormat fileFormat;

    public int procTimePer1Frame = 0;
    public int stepCounter = 0;

    public String playingFileName;
    public String playingArcFileName;
    protected int songNo = 0;
    protected List<Tuple<String, byte[]>> extendFiles = null;

    public List<Tuple<String, byte[]>> getExtendFiles() {
        return extendFiles;
    }
    public Path playingFilePath;

    public boolean flgReinit = false;
    public boolean stopped = false;
    public boolean paused = false;

    public boolean fadeout;
    public double fadeoutCounter;
    public double fadeoutCounterV;

    // TODO variable?
    public static final int BUFFER_SIZE = 1024;

    public byte[] getData() {
        return dataBuf;
    }

    /** used chips */
    protected final Map<Class<? extends mdplayer.Chip>, List<Chip>> chips = new HashMap<>();

    public final Set<Class<? extends mdplayer.Chip>> getChips() {
        return chips.keySet();
    }

    /** used chips with their mixer instances; for measurement / calibration tooling (mixer tags live in {@code Chip.setVolumes}) */
    public final Map<Class<? extends mdplayer.Chip>, List<Chip>> getChipInstances() {
        return chips;
    }

    /** put used chips */
    protected void put(Class<? extends mdplayer.Chip> chip, Chip info) {
        if (chips.containsKey(chip)) {
            chips.get(chip).add(info);
        } else {
            chips.put(chip, new ArrayList<>(List.of(info)));
        }

        // view
        getDriver().fireEventHappened(chipRegister.chip(chip), "led.set", info.id);
    }

    /** check a chip existence */
    public boolean contains(Class<? extends mdplayer.Chip> chip) {
        return chips.get(chip) != null;
    }

    /** check a chip existence */
    protected boolean contains(Class<? extends mdplayer.Chip> chip, int index) {
        var infos = chips.get(chip);
        return infos != null && index < infos.size();
    }

    /** list for MDSound */
    protected List<Chip> flatten() {
        return chips.values().stream().flatMap(Collection::stream).toList();
    }

    protected BasePlugin() {
        mds = new MDSound();

        chipRegister = new ChipRegister();

        init();
    }

    @Override
    public void init() {
        oneTimeReset = false;

        chipRegister.init(this);
        // midi out released
        chipRegister.plugin(MidiPlugin.class).releaseAll();

        this.paused = false;
        this.stopped = true;
        this.fadeout = false;
logger.log(Level.TRACE, "stop: " + this.stopped + ", " + this.hashCode());
    }

    public int getLatency() {
        if (setting.getOutputDevice().getDeviceType() != Common.DEV_AsioOut) {
            return setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getLatency() / 1000;
        }
        return 0; // naudioWrap.getAsioLatency(); TODO
    }

    /** {@code super#prepare()} must be called inside inherited this method */
    @Override
    public void prepare() {
        // The chip and sub-plugin instances are shared singletons (loaded once via
        // ServiceLoader and reused by every format plugin's ChipRegister), so their
        // owning `context` is only pointed at THIS plugin in the constructor. When a
        // previously-constructed plugin is replayed after another one has played, the
        // shared context is left pointing at that other plugin, breaking chip
        // registration/MIDI routing. Re-point it to the active plugin each song.
        chipRegister.init(this);

        this.fadeout = false;
        this.fadeoutCounter = 1.0;
        this.fadeoutCounterV = 0.00001;
        this.speed = 1;
        chipRegister.plugin(RealChipPlugin.class).realFadeoutVol = 0;
        chipRegister.plugin(RealChipPlugin.class).realFadeoutVolWait = 4;

        chips.clear();
        resetFadeOutParam();

        chipRegister.reset();
        chipRegister.clearFadeoutVolume();
        masterVolume = setting.getBalance().getMasterVolume();

        getDriver().fireEventHappened(this, "led.reset");
    }

    @Override
    public void stop() {
logger.log(Level.TRACE, "stop enter: " + this.stopped);
        if (!this.stopped) {
            this.stopped = true;
            // a MIDI driver leaves its notes held down at the synthesizer, see MidiPlugin#allSoundOff
            chipRegister.plugin(MidiPlugin.class).allSoundOff();
logger.log(Level.INFO, "stop: " + this.stopped);
        }
    }

    private void resetFadeOutParam() {
        this.fadeout = false;
        this.fadeoutCounter = 1.0;
        this.fadeoutCounterV = 0.00001;
        this.speed = 1;
        chipRegister.plugin(RealChipPlugin.class).realFadeoutVol = 0;
        chipRegister.plugin(RealChipPlugin.class).realFadeoutVolWait = 4;

        chipRegister.clearFadeoutVolume();

        chipRegister.reset();
    }

    /** @param params tags: fileName, arcFileName, midiMode, songNo */
    public void setParams(FileFormat format, Map<String, Object> params) {
        this.fileFormat = format;
        this.dataBuf = format.getData();
        this.playingFileName = params.get("fileName") != null ? (String) params.get("fileName") : format.getCompiledFilename();
        this.playingArcFileName = (String) params.get("arcFileName");
        chipRegister.plugin(MidiPlugin.class).midiMode = (int) params.getOrDefault("midiMode", 0);
        this.songNo = (int) params.getOrDefault("songNo", 0);
        chipRegister.plugin(MidiPlugin.class).setFileName(playingFileName); // for ExportMIDI
        extendFiles = format.getExtendFiles(); // Additional files
        this.playingFilePath = Path.of(playingFileName).getParent();

        loadPresetMixerBalance();
        setting.getBalance().applySystemProperties();
    }

    /**
     * Puts the song's preset mixer balance into the settings, so the calibrated per-chip volumes
     * and {@code MasterVolume} are in place before {@link #prepare()} pushes them into the chips.
     * <p>
     * This lives here rather than in the main form because every playback path goes through
     * {@link #setParams}: the form, but also headless callers (tests, tools) that never build one.
     * A caller that drives the balance itself -- the volume calibrator, say -- turns it off with
     * {@code setting.getAutoBalance().setUseThis(false)}.
     * <p>
     * A hand-made {@code .mbc} under {@code <settings>/MixerBalance} wins; otherwise the preset
     * bundled in the jar is read straight from the classpath. Nothing is written here: seeding the
     * {@code .mbc} on first play (which is what this used to do) meant a recalibrated preset was
     * then shadowed forever by that first copy.
     */
    private void loadPresetMixerBalance() {
        if (!setting.getAutoBalance().getUseThis()) return;

        try {
            Path settingPath = Common.settingFilePath != null
                    ? Common.settingFilePath : Common.getApplicationDataFolder(true);
            if (settingPath == null) return;
            Path dir = settingPath.resolve("MixerBalance");
            Path mbc = null;        // the user's own balance file, when there is one
            String bundled = null;  // classpath path of the driver's calibrated preset

            // Song-specific preset: "<song>.mbc", either beside the song data or in the settings folder
            if (setting.getAutoBalance().getLoadSongBalance()) {
                boolean archived = playingArcFileName != null && !playingArcFileName.isEmpty();
                String name = Path.of(archived ? playingArcFileName : playingFileName)
                        .getFileName() + ".mbc";
                Path songDir = setting.getAutoBalance().getSamePositionAsSongData()
                        ? Path.of(archived ? playingArcFileName : playingFileName).getParent() : dir;
                if (songDir != null && Files.exists(songDir.resolve(name))) mbc = songDir.resolve(name);
            }

            // Driver-specific preset: "DriverBalance_<driver>.mbc" / the bundled DefaultVolumeBalance_*.xml
            if (mbc == null && setting.getAutoBalance().getLoadDriverBalance()) {
                String[] fns = fileFormat.getPresetMixerBalance();
                if (fns != null) {
                    if (Files.exists(dir.resolve(fns[0]))) mbc = dir.resolve(fns[0]);
                    else bundled = "/mdplayer" + fns[1];
                }
            }

            Setting.Balance balance = null;
            if (mbc != null) {
                balance = Setting.Balance.load(mbc);
            } else if (bundled != null) {
                try (InputStream in = getClass().getResourceAsStream(bundled)) {
                    if (in != null) balance = Setting.Balance.load(in);
                }
            }
            if (balance == null) return;

            setting.setBalance(balance);
logger.log(Level.DEBUG, "balance: " + (mbc != null ? mbc : bundled) + ", master: " + balance.getMasterVolume());
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    @Override
    public void ff() {
        if (driverVirtual == null) return;
        speed(speed == 1 ? 4 : 1);
    }

    public void slow() {
        speed(speed == 1 ? 0.25 : 1);
    }

    /** Plays at this rate, 1 being the rate the music was written at. */
    public void speed(double value) {
        if (driverVirtual == null) return;

        speed = value;
        driverVirtual.speed = speed;
        if (driverReal != null) driverReal.speed = speed;
    }

    public void resetSlow() {
        speed = 1;
        driverVirtual.speed = speed;
        if (driverReal != null) driverReal.speed = speed;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    public boolean isFadeOut() {
        return this.fadeout;
    }

    public boolean isSlow() {
        return !isStopped() && (speed < 1.0);
    }

    public boolean isFF() {
        return !isStopped() && (speed > 1.0);
    }

    @Override
    public void close() {
logger.log(Level.INFO, "close enter");
        stop();
    }

    public void resetTimeCounter() {
        if (driverVirtual == null && driverReal == null) return;
        if (driverVirtual != null) {
            driverVirtual.counter = 0;
            driverVirtual.totalCounter = 0;
            driverVirtual.loopCounter = 0;
        }

        if (driverReal != null) {
            driverReal.counter = 0;
            driverReal.totalCounter = 0;
            driverReal.loopCounter = 0;
        }
    }

    public long getCounter() {
        if (driverVirtual == null && driverReal == null) return -1;

        if (driverVirtual == null) return driverReal.counter;
        if (driverReal == null) return driverVirtual.counter;

        return Math.max(driverVirtual.counter, driverReal.counter);
    }

    public long getTotalCounter() {
        if (driverVirtual == null) return -1;

        return driverVirtual.totalCounter;
    }

    public long getLoopCounter() {
        if (driverVirtual == null) return -1;

        return driverVirtual.loopCounter;
    }

    public void updateVol() {
        chipRegister.updateVol();
    }

    public int getCurLoopCounter() {
        int cnt = 0;

        if (driverVirtual != null) {
            cnt = driverVirtual.curLoop;
        }
        if (driverReal != null) {
            cnt = Math.min(driverReal.curLoop, cnt);
        }

        return cnt;
    }

    public boolean getStopped() {
        boolean v;
        boolean r;

        v = driverVirtual == null || driverVirtual.stopped;
        r = driverReal == null || driverReal.stopped;
        return v && r;
    }

    public boolean isDataBlock(Common.EnmModel model) {

        if (model == Common.EnmModel.VirtualModel) {
            if (driverVirtual == null) return false;
            return driverVirtual.isDataBlock;
        } else {
            if (driverReal == null) return false;
            return driverReal.isDataBlock;
        }
    }

    public long getVirtualFrameCounter() {
        if (driverVirtual == null) return -1;
        return driverVirtual.frameCounter;
    }

    public long getRealFrameCounter() {
        if (driverReal == null) return -1;
        return driverReal.frameCounter;
    }

    @Override
    public BaseDriver getDriver() {
        return driverVirtual;
    }

    protected abstract void initChips();

    public long getDriverCounter() {
        if (driverVirtual == null && driverReal == null) return -1;

        if (driverVirtual == null) {
            return driverReal.getDriverCounter();
        }
        if (driverReal == null) {
            return driverVirtual.getDriverCounter();
        }

        return driverVirtual.whichCounter(driverReal.getDriverCounter(), driverVirtual.getDriverCounter());
    }

    public void setVolume(String tag, Class<? extends mdplayer.Chip> c, boolean isAbs, int volume) {
        try {
            int v = Common.range((isAbs ? 0 : setting.getBalance().getVolume(tag, c)) + volume, -192, 20);
            mdplayer.Chip chip = chipRegister.chip(c);
            if (chip != null) {
                mds.setVolume(tag, chip.inst(0), v); // TODO vavi
            }
            setting.getBalance().setVolume(tag, c, v);
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    /**
     * Moves one chip's mixer volume by {@code offset} from the balance, for the song being played only:
     * unlike {@link #setVolume} the balance is left as it is, so the next song starts from it again.
     *
     * @param offset 2&times;dB
     */
    public void setSongVolume(String tag, Class<? extends mdplayer.Chip> c, int offset) {
        try {
            int v = Common.range(setting.getBalance().getVolume(tag, c) + offset, -192, 20);
            mdplayer.Chip chip = chipRegister.chip(c);
            if (chip != null) mds.setVolume(tag, chip.inst(0), v);
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    public int masterVolume = 0;

    /**
     * Moves the MIDI path's fader.
     *
     * @see Setting.Balance#getMidiVolume()
     */
    public void setMidiVolume(boolean isAbs, int volume) {
        int v = Common.range((isAbs ? 0 : setting.getBalance().getMidiVolume()) + volume, -192, 20);
        setting.getBalance().setMidiVolume(v);
        chipRegister.plugin(MidiPlugin.class).applyVolume();
    }

    public void setMasterVolume(boolean isAbs, int volume) {
        masterVolume = Common.range((isAbs ? 0 : setting.getBalance().getMasterVolume()) + volume, -192, 20);
        setting.getBalance().setMasterVolume(masterVolume);
        // a MIDI song is not in the mix this volume multiplies, it has to be told
        chipRegister.plugin(MidiPlugin.class).applyVolume();
    }

    public FileFormat getFileFormat() {
        return fileFormat;
    }
}
