package mdplayer.driver.pmd;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import mdplayer.Common;
import mdplayer.Common.EnmModel;
import mdplayer.chips.P86Chip;
import mdplayer.chips.PpsChip;
import mdplayer.chips.Ppz8Chip;
import mdplayer.chips.Ym2608Chip;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.FileFormat;
import mdplayer.driver.BasePlugin;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.CompilerInfo;
import musicDriverInterface.ICompiler;
import musicDriverInterface.IDriver;
import musicDriverInterface.MetaData;
import musicDriverInterface.MmlDatum;
import vavi.util.compat.Tuple;

import static java.lang.System.getLogger;
import static pmd.common.Common.createFileReader;


/**
 * PMD
 * <p>
 * environment variable
 * <li>{@code mdplayer.pmd.pmd} ... search path for the files a song names ({@code .PPC}, {@code .P86}, {@code .PPS},
 *     {@code .PZI}, MML includes), separated by {@link File#pathSeparator}. the song's own folder is searched first,
 *     a relative entry is relative to the song's folder, e.g. {@code ..} for the parent</li>
 * <li>{@code mdplayer.pmd.opt} ... </li>
 *
 * @author kumatan
 */
public class PmdDriver extends BaseDriver {

    private static final Logger logger = getLogger(PmdDriver.class.getName());

    private ICompiler pmdCompiler = null;
    private IDriver pmdDriver = null;

    /** driver work area, the source of the "pmd" event */
    private Map<String, Object> work = null;

    /** how many {@link #processOneFrame()} calls between two "pmd" events */
    private static final int visualizeInterval = Common.VGMProcSampleRate / 120;

    private int visualizeCounter;

    private static String[] envPmd = null;
    private static String[] envPmdOpt = null;

    private boolean isNRM;
    private boolean isSPB;
    private boolean isVA;
    private boolean usePPS;
    private boolean usePPZ;

    public static final int baseClock = 7987200;

    public PmdDriver(BasePlugin<? extends BaseDriver> plugin) {
        super(plugin);
    }

    public PmdDriver() {
        this(null); // gross
    }

    @Override
    public MetaData retrieveMetaData(byte[] buf, Object... args) {
        PMDFileType mType;
        if (args == null || args.length == 0) {
            mType = checkFileType(buf);
        } else {
            mType = (PMDFileType) args[0];
        }

        MetaData metaData;

        if (mType == PMDFileType.MML) {
            envPmd = System.getProperty("mdplayer.pmd.pmd", "").split(File.pathSeparator);
            envPmdOpt = System.getProperty("mdplayer.pmd.opt", "").split(File.pathSeparator);

            pmdCompiler = ICompiler.factory("pmd.compiler.Compiler");
            pmdCompiler.setCompileSwitch(fileReader());
            metaData = pmdCompiler.getMetaData(buf);
        } else {
            pmdDriver = IDriver.factory("pmd.driver.Driver");
            //pmdDriver.SetDriverSwitch((Func<String, Stream>) appendFileReaderCallback);
            metaData = pmdDriver.getMetaData(buf);
        }

        return metaData;
    }

    /**
     * @param args 0: FileFormat
     */
    @Override
    public void init(EnmModel model, int latency, int waitTime, Object... args) {

        FileFormat fileFormat = (FileFormat) args[0];
        PMDFileType mType = fileFormat instanceof MMLFileFormat ? PMDFileType.MML : PMDFileType.M;
        metaData = retrieveMetaData(dataBuf, mType);

        this.model = model;
        this.latency = latency;
        this.waitTime = waitTime;

        counter = 0;
        totalCounter = 0;
        loopCounter = 0;
        curLoop = 0;
        stopped = false;
        frameCounter = -latency - waitTime;
        speed = 1;

//#if DEBUG
        // The actual chip thread skips processing (for debugging)
        if (model == EnmModel.RealModel)
            return;
//#endif

        if (mType == PMDFileType.MML)
            initMML();
        else
            initM();
    }

    @Override
    public void processOneFrame() {

//#if DEBUG
        // The actual chip thread skips processing (for debugging)
        if (model == EnmModel.RealModel) {
            stopped = true;
            return;
        }
//#endif

        try {
            speedCounter += (double) Common.VGMProcSampleRate / setting.getOutputDevice().getSampleRate() * speed;
            while (speedCounter >= 1.0) {
                speedCounter -= 1.0;

                pmdDriver.render();

                counter++;
                frameCounter++;
            }

            int lp = pmdDriver.getNowLoopCounter();
            lp = Math.max(lp, 0);
            curLoop = lp;

            if (work != null && work.get("work") != null && ++visualizeCounter >= visualizeInterval) {
                visualizeCounter = 0;
                fireEventHappened(this, "pmd", work.get("work"));
            }

            if (pmdDriver.getStatus() < 1) {
                if (pmdDriver.getStatus() == 0) {
                    Thread.sleep((int) (latency * 2.0)); // Wait for latency*2 until the actual voice is fully pronounced
                }
                stopped = true;
            }
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    public String pcmType(int index) {
        return switch (index) {
            case 0 -> "PPC";
            case 1 -> "PPZ1";
            case 2 -> "PPZ2";
            case 3 -> "PPS";
            default -> null;
        };
    }

    @Override
    public String pcmFilename(int index) {
        if (work == null) return null;
        Object pwObj = work.get("work");
        if (pwObj instanceof pmd.driver.PW pw) { // TODO expose pmd package
            return switch (index) {
                case 0 -> pw.ppcFile;
                case 1 -> pw.ppz1File;
                case 2 -> pw.ppz2File;
                case 3 -> pw.ppsFile;
                default -> null;
            };
        }
        return null;
    }

    @Override
    public boolean pcmError(int index) {
        if (work == null) return false;
        Object pwObj = work.get("work");
        if (pwObj instanceof pmd.driver.PW pw) { // TODO expose pmd package
            return switch (index) {
                case 0 -> pw.ppcError;
                case 1 -> pw.ppz1Error;
                case 2 -> pw.ppz2Error;
                case 3 -> pw.ppsError;
                default -> false;
            };
        }
        return false;
    }

    public enum PMDFileType {
        unknown,
        MML,
        M
    }

    private static PMDFileType checkFileType(byte[] buf) {
        if (buf == null || buf.length < 4) {
            return PMDFileType.unknown;
        }

        if (buf[0] == 0x4d && buf[1] == 0x55 && buf[2] == 0x43 && buf[3] == 0x38) {
            return PMDFileType.M;
        }
        if (buf[0] == 0x4d && buf[1] == 0x55 && buf[2] == 0x42 && buf[3] == 0x38) {
            return PMDFileType.M;
        }

        return PMDFileType.MML;
    }

    private void initMML() {
        pmdCompiler.init();

        MmlDatum[] ret;
        CompilerInfo info;
        try {
            pmdCompiler.setCompileSwitch("PmdOption=%s \"%s\"".formatted(
                    setting.getPmd().compilerArguments, plugin.playingFileName));
            try (InputStream sourceMML = new ByteArrayInputStream(dataBuf)) {
                ret = pmdCompiler.compile(sourceMML, fileReader());// wrkMUCFullPath, disp);
            }

            info = pmdCompiler.getCompilerInfo();

        } catch (Exception e) {
            throw new IllegalStateException("error in compiling", e);
        }

        if (!info.errorList.isEmpty()) {
//            if (model == EnmModel.VirtualModel) {
//                JOptionPane.showMessageDialog(null, "Compile error");
//            }
            throw new IllegalArgumentException("Compile error: " + info.errorList.stream().map(t -> t.getItem1() + ", " + t.getItem2() + ", " + t.getItem3()).toList());
        }

        if (pmdDriver == null) pmdDriver = IDriver.factory("pmd.driver.Driver");

//        boolean notSoundBoard2 = false;
        boolean isLoadADPCM = true;
        boolean loadADPCMOnly = false;

        isNRM = setting.getPmd().soundBoard == 0;
        isSPB = setting.getPmd().soundBoard == 1;
        isVA = false;
        usePPS = setting.getPmd().usePPSDRV;
        usePPZ = setting.getPmd().usePPZ8;

        envPmd = System.getProperty("mdplayer.pmd.pmd", "").split(File.pathSeparator);
        envPmdOpt = System.getProperty("mdplayer.pmd.opt", "").split(File.pathSeparator);

        Object[] driverOption = {
                isLoadADPCM, // boolean
                loadADPCMOnly, // boolean
                setting.getPmd().isAuto, // boolean isAUTO;
                isVA, // boolean
                isNRM, // boolean
                usePPS, // boolean
                usePPZ, // boolean
                isSPB, // boolean
                envPmd, // String[] Environment variable PMD
                envPmdOpt, // String[] Environment variable PMDOpt
                plugin.playingFileName, // String srcFile;
                "", // String PPCFileHeader is ignored (no setting required)
                null // the driver searches the song's folder, then envPmd
        };

        String[] commandLineOption = getPMDOption();

        pmdDriver.init(
                null,
                ret,
                null, // This callback is unused
                driverOption, // driver option
                commandLineOption, // command line option
                (Function<ChipDatum, Integer>) this::writePPZ8,
                (Function<ChipDatum, Integer>) this::writePPSDRV,
                (Function<ChipDatum, Integer>) this::writeP86,
                (Consumer<ChipDatum>) this::writeOPNA1,
                (BiConsumer<Long, Integer>) this::sendOPNAWait);

        work = pmdDriver.getWork();

        pmdDriver.startRendering(Common.VGMProcSampleRate, new Tuple<>("YM2608", baseClock));
        pmdDriver.startMusic(0);
    }

    private void initM() {
        if (pmdDriver == null)
            pmdDriver = IDriver.factory("pmd.driver.Driver");

        // boolean notSoundBoard2 = false;
        boolean isLoadADPCM = true;
        boolean loadADPCMOnly = false;
        List<MmlDatum> buf = new ArrayList<>();
        for (byte b : dataBuf)
            buf.add(new MmlDatum(b & 0xff));

        isNRM = setting.getPmd().soundBoard == 0;
        isSPB = setting.getPmd().soundBoard == 1;
        isVA = false;
        usePPS = setting.getPmd().usePPSDRV;
        usePPZ = setting.getPmd().usePPZ8;

        envPmd = System.getProperty("mdplayer.pmd.pmd", "").split(File.pathSeparator);
        envPmdOpt = System.getProperty("mdplayer.pmd.opt", "").split(File.pathSeparator);

        Object[] driverOption = {
                isLoadADPCM, // boolean
                loadADPCMOnly, // boolean
                setting.getPmd().isAuto, // boolean isAUTO;
                isVA, // boolean
                isNRM, // boolean
                usePPS, // boolean
                usePPZ, // boolean
                isSPB, // boolean
                envPmd, // String[] Environment variable PMD
                envPmdOpt, // String[] Environment variable PMDOpt
                plugin.playingFileName, // String srcFile;
                "", // String PPCFileHeader is ignored (no setting required)
                null // the driver searches the song's folder, then envPmd
        };

        String[] commandLineOption = getPMDOption();

        pmdDriver.init(null,
                buf.toArray(MmlDatum[]::new),
                null, // This callback is unused
                driverOption, // driver option
                commandLineOption, // command line option
                (Function<ChipDatum, Integer>) this::writePPZ8,
                (Function<ChipDatum, Integer>) this::writePPSDRV,
                (Function<ChipDatum, Integer>) this::writeP86,
                (Consumer<ChipDatum>) this::writeOPNA1,
                (BiConsumer<Long, Integer>) this::sendOPNAWait);

        work = pmdDriver.getWork();

        pmdDriver.startRendering(Common.VGMProcSampleRate, new Tuple<>("YM2608", baseClock));
        pmdDriver.startMusic(0);
    }

    private void writeOPNA1(ChipDatum cd) {
        if (cd == null)
            return;
        if (cd.address == -1)
            return;
        if (cd.data == -1)
            return;
        if (cd.port == -1 || cd.port == 10000) // vavi
            return;

        plugin.chipRegister.chip(Ym2608Chip.class).write(0, cd.port, cd.address, cd.data, model);
    }

    private void sendOPNAWait(long size, int elapsed) {
        if (model == EnmModel.VirtualModel) {
//            JOptionPane.showMessageDialog("elapsed:%d size:%d".formatted(elapsed, size));
//            int n = Math.max((int) (size / 20 - elapsed), 0); // 20 Threshold (magic number)
//            Thread.sleep(n);
            return;
        }

        // Add additional weight based on size and elapsed time.
        int m = Math.max((int) (size / 20 - elapsed), 0); // 20 Threshold (magic number)
        try { Thread.sleep(m); } catch (InterruptedException _) {}
    }

    private int writePPSDRV(ChipDatum arg) {
        if (arg == null)
            return 0;

        if (arg.port == 0x05) {
            plugin.chipRegister.chip(PpsChip.class).writePcm(0, (byte[]) arg.additionalData, model);
        } else {
            plugin.chipRegister.chip(PpsChip.class).write(0, arg.port, arg.address, arg.data, model);
        }

        return 0;
    }

    private int writeP86(ChipDatum arg) {
        if (arg == null)
            return 0;

        if (arg.port == 0x00) {
            plugin.chipRegister.chip(P86Chip.class).writePcm(0, arg.address, arg.data, (byte[]) arg.additionalData, model);
        } else {
            plugin.chipRegister.chip(P86Chip.class).write(0, arg.port, arg.address, arg.data, model);
        }

        return 0;
    }

    private int writePPZ8(ChipDatum arg) {
        if (arg == null)
            return 0;

        if (arg.port == 0x03) {
            plugin.chipRegister.chip(Ppz8Chip.class).writePcm(0, arg.address, arg.data, (byte[][]) arg.additionalData, model);
        } else {
            plugin.chipRegister.chip(Ppz8Chip.class).write(0, arg.port, arg.address, arg.data, model);
        }

        return 0;
    }

    /** the files a song names, searched in the song's folder, then in {@link #envPmd} */
    private Function<String, InputStream> fileReader() {
        return createFileReader(Path.of(plugin.playingFileName).toAbsolutePath().getParent(), envPmd);
    }

    private static String[] getPMDOption() {
        List<String> op = new ArrayList<>();

        // envPMDOpt
        if (envPmdOpt != null && envPmdOpt.length > 0)
            op.addAll(Arrays.asList(envPmdOpt));

        // Arguments (optional in the IDE)
        String[] drvArgs = setting.getPmd().driverArguments.split("\\s");
        if (drvArgs.length > 0)
            op.addAll(Arrays.asList(drvArgs));

        return op.toArray(String[]::new);
    }
}
