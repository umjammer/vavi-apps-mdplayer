package mdplayer.driver.zms;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import mdplayer.Common;
import mdplayer.Common.EnmModel;
import mdplayer.chips.MPcmChip;
import mdplayer.chips.MidiPlugin;
import mdplayer.chips.Pcm8Chip;
import mdplayer.chips.Ym2151Chip;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.BasePlugin.Compilable;
import mdsound.MDSound;
import mdsound.instrument.X68kYm2151Inst;
import mdsound.x68sound.SoundIocs;
import vavi.util.compat.Tuple;

import static java.lang.System.getLogger;
import static mdsound.MDSound.Chip.MAIN_TAG;
import static vavi.util.compat.Util.getExtension;


/**
 * ZMusic (X68000) Plugin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-07-12 nsano initial version <br>
 */
public class ZMSPlugin extends BasePlugin<ZmsDriver> implements Compilable {

    private static final Logger logger = getLogger(ZMSPlugin.class.getName());

    @Override
    public void compile() {

    }

    /** the song asks for a PCM format only MPCMPP has, see {@link MpcmDetector} */
    private boolean needsMpcmPP;

    /** @see mdplayer.Setting#mpcmType */
    public boolean needsMpcmPP() {
        return needsMpcmPP;
    }

    @Override
    public void prepare() {
        // before initChips puts one of the two MPCM back ends in
        needsMpcmPP = MpcmDetector.needsMpcmPP(dataBuf, playingFileName);
        if (needsMpcmPP) logger.log(Level.INFO, "mpcm: the song asks for a Mercury-Unit format, MPCMPP");
        driverVirtual = new ZmsDriver(this);

        driverReal = null;
        if (setting.getOutputDevice().getDeviceType() != Common.DEV_Null) {
//            driverReal = new ZmsDriver(this);
        }

        super.prepare();
        initChips();
    }

    private final String[] supportFile = null;
    private final String useCompiler = null;

    @Override
    protected void initChips() {

        MDSound.Chip chip = new MDSound.Chip();
        chip.id = 0;
        chip.instrument = chipRegister.chip(Ym2151Chip.class).instrument(0);
        chip.volume = setting.getBalance().getVolume(MAIN_TAG, Ym2151Chip.class);
        chip.clock = 4000000;
        chip.samplingRate = chip.clock / 64;
        chip.option = null;
        put(Ym2151Chip.class, chip);

        chip = new MDSound.Chip();
        chip.id = 0;
        chip.instrument = chipRegister.chip(MPcmChip.class).instrument(0);
        chip.samplingRate = setting.getOutputDevice().getSampleRate();
        chip.clock = 15600;
        chip.volume = setting.getBalance().getVolume(MAIN_TAG, MPcmChip.class);
        chip.option = null;
        put(MPcmChip.class, chip);

        //audio.chipLED.put("PriMPCM", 1);

        chip = new MDSound.Chip();
        chip.id = 0;
        chip.instrument = chipRegister.chip(Pcm8Chip.class).instrument(0);
        chip.volume = setting.getBalance().getVolume(MAIN_TAG, Pcm8Chip.class);
        chip.clock = 4_000_000;
        if (chip.instrument instanceof X68kYm2151Inst opmPCM) {
            opmPCM.soundIocs[0] = new SoundIocs(opmPCM.chips[0]);
            chip.samplingRate = 4_000_000 / 64;
            chip.option = new Object[] { 0, 1, 0 };
        } else {
            chip.samplingRate = setting.getOutputDevice().getSampleRate();
            chip.option = new Object[] {setting.pcm8ppsOption(this)};
        }
        put(Pcm8Chip.class, chip);

        mds.init(setting.getOutputDevice().getSampleRate(), BUFFER_SIZE, flatten());

        chipRegister.plugin(MidiPlugin.class).releaseAll();
        chipRegister.plugin(MidiPlugin.class).make();
//        chipRegister.plugin(MidiPlugin.class).set(setting.getMidiOut().getMidiOutInfos().get(chipRegister.plugin(MidiPlugin.class).midiMode));

        if (contains(Ym2151Chip.class, 0))
            chipRegister.chip(Ym2151Chip.class).writeClock(0, 4000000, EnmModel.RealModel);

        chipRegister.chip(Ym2151Chip.class).corrections[0] = 4000000;
        chipRegister.chip(Ym2151Chip.class).corrections[1] = 4000000;

        getDriver().fireEventHappened(chipRegister.chip(Pcm8Chip.class), "led.set", 0);
        getDriver().fireEventHappened(chipRegister.chip(Pcm8Chip.class), "led.set", 0, "x68k");

        try {
            // Compiler usage priority
            int compilePriority = setting.getZMusic().compilePriority;
            if (useCompiler != null) {
                if (useCompiler.equals("zmusic v2")) compilePriority = 3; // 3: v2 only
                else if (useCompiler.equals("zmusic v3")) compilePriority = 2; // 2: v3 only
            }
            // Loading/compiling support files
            List<Tuple<byte[], String>> supportFileBinary = new ArrayList<>();
            if (supportFile != null) {
                for (String sf : supportFile) {
                    byte[] buf;
                    if (isExt(sf, ".ZMS")) {
                        buf = Files.readAllBytes(Path.of(sf));
                        switch (compilePriority) {
                            case 0:
                                // Version 3 is preferred
                                if (driverVirtual.compile(buf, sf)) buf = driverVirtual.getCompiledData();
                                else if (driverVirtual.compileV2(buf, sf)) buf = driverVirtual.getCompiledData();
                                else throw new IllegalArgumentException("Compile Error.Check console log.");
                                break;
                            case 1:
                                // Version 2 is preferred
                                if (driverVirtual.compileV2(buf, sf)) buf = driverVirtual.getCompiledData();
                                else if (driverVirtual.compile(dataBuf, sf))
                                    buf = driverVirtual.getCompiledData();
                                else throw new IllegalArgumentException("Compile Error.Check console log.");
                                break;
                            case 2:
                                // Version 3 only
                                if (driverVirtual.compile(buf, sf)) buf = driverVirtual.getCompiledData();
                                else throw new IllegalArgumentException("Compile Error.Check console log.");
                                break;
                            case 3:
                                // Version 2 only
                                if (driverVirtual.compileV2(buf, sf)) buf = driverVirtual.getCompiledData();
                                else throw new IllegalArgumentException("Compile Error.Check console log.");
                                break;
                        }
                        supportFileBinary.add(new Tuple<>(buf, Path.of(sf).getFileName().toString()));
                        continue;
                    }
                    buf = Files.readAllBytes(Path.of(sf));
                    supportFileBinary.add(new Tuple<>(buf, Path.of(sf).getFileName().toString()));
                }
            }

            driverVirtual.setSupportFileBinaryAndName(supportFileBinary);
            if (driverReal != null) driverReal.setSupportFileBinaryAndName(supportFileBinary);
//            if (driverPianoRoll != null) (driverPianoRoll).supportFileBinaryAndName = supportFileBinary;

logger.log(Level.INFO, "compilePriority: " + compilePriority);
            // In the case of ZMS, compilation is performed in advance
            if (isExt(playingFileName, ".ZMS")) {
                switch (compilePriority) {
                    case 0:
                        // Version 3 is preferred
                        if (driverVirtual.compile(dataBuf, playingFileName)) {
                            setVgmBufV3();
                            getDriver().fireEventHappened(chipRegister.chip(Pcm8Chip.class), "led.set", 0, "x68k");
                        } else if (driverVirtual.compileV2(dataBuf, playingFileName)) {
                            setVgmBufV2();
                            getDriver().fireEventHappened(chipRegister.chip(Pcm8Chip.class), "led.set", 0);
                        } else {
                            // compile error
                            throw new IllegalArgumentException("Compile Error.Check console log.");
                        }
                        break;
                    case 1:
                        // Version 2 is preferred
                        if (driverVirtual.compileV2(dataBuf, playingFileName)) {
                            setVgmBufV2();
                            getDriver().fireEventHappened(chipRegister.chip(Pcm8Chip.class), "led.set", 0);
                        } else if (driverVirtual.compile(dataBuf, playingFileName)) {
                            setVgmBufV3();
                            getDriver().fireEventHappened(chipRegister.chip(Pcm8Chip.class), "led.set", 0, "x68k");
                        } else {
                            // compile error
                            throw new IllegalArgumentException("Compile Error.Check console log.");
                        }
                        break;
                    case 2:
                        // Version 3 only
                        if (driverVirtual.compile(dataBuf, playingFileName)) {
                            setVgmBufV3();
                            getDriver().fireEventHappened(chipRegister.chip(Pcm8Chip.class), "led.set", 0, "x68k");
                            //logger.log("c:\\temp\\ge.zmd", dataBuf);
                        } else {
                            // compile error
                            throw new IllegalArgumentException("Compile Error.Check console log.");
                        }
                        break;
                    case 3:
                        // Version 2 only
                        if (driverVirtual.compileV2(dataBuf, playingFileName)) {
                            setVgmBufV2();
                            getDriver().fireEventHappened(chipRegister.chip(Pcm8Chip.class), "led.set", 0);
                        } else {
                            // compile error
                            throw new IllegalArgumentException("Compile Error.Check console log.");
                        }
                        break;
                }
            } else {
                driverVirtual.retrieveMetaData(dataBuf, 0);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (driverVirtual.getVersion() != 2) {
            // Check the sound source composition used from ZMD
            boolean useFM = dataBuf[0x48] != 0;
            boolean useMPCM = dataBuf[0x49] != 0;
            boolean useMIDI1 = dataBuf[0x4a] != 0;
            boolean useMIDI2 = dataBuf[0x4b] != 0;
            boolean useMIDI3 = dataBuf[0x4c] != 0;
            boolean useMIDI4 = dataBuf[0x4d] != 0;
            if (useFM) getDriver().fireEventHappened(chipRegister.chip(Ym2151Chip.class), "led.set", 0);
            if (useMIDI1) getDriver().fireEventHappened(chipRegister.plugin(MidiPlugin.class), "led.set", 0);
            if (useMIDI2) getDriver().fireEventHappened(chipRegister.plugin(MidiPlugin.class), "led.set", 1);
            if (useMIDI3) getDriver().fireEventHappened(chipRegister.plugin(MidiPlugin.class), "led.set", 2);
            if (useMIDI4) getDriver().fireEventHappened(chipRegister.plugin(MidiPlugin.class), "led.set", 3);
            if (useMPCM) getDriver().fireEventHappened(chipRegister.chip(Pcm8Chip.class), "led.set", 0, "x68k");
        } else {
            getDriver().fireEventHappened(chipRegister.chip(Pcm8Chip.class), "led.set", 0);
        }

        driverVirtual.init(EnmModel.VirtualModel,
                setting.getOutputDevice().getSampleRate() * setting.getLatencyEmulation() / 1000,
                setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000);
        if (driverReal != null) {
            driverReal.init(EnmModel.RealModel,
                    setting.getOutputDevice().getSampleRate() * setting.getLatencySCCI() / 1000,
                    setting.getOutputDevice().getSampleRate() * setting.getOutputDevice().getWaitTime() / 1000);
        }
    }

    private void setVgmBufV3() {
        dataBuf = driverVirtual.getCompiledData();
        driverVirtual.setCompiledData(dataBuf);
        if (driverReal != null) driverReal.setCompiledData(dataBuf);
//        if (driverPianoRoll != null) (driverPianoRoll).compiledData = dataBuf;
    }

    private void setVgmBufV2() {
        dataBuf = driverVirtual.getCompiledData();
        driverVirtual.setCompiledData(dataBuf);
        driverVirtual.setVersion(2);
        if (driverReal != null) {
            driverReal.setCompiledData(dataBuf);
            driverReal.setVersion(2);
        }
//        if (driverPianoRoll != null) {
//            driverPianoRoll.setCompiledData(dataBuf);
//            driverPianoRoll.setVersion(2);
//        }
    }

    private static boolean isExt(String filename, String ext) {
        return getExtension(filename).toUpperCase().equals(ext);
    }
}
