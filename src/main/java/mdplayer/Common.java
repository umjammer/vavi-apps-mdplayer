package mdplayer;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import mdplayer.form.sys.setting.SettingAboutPanel;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;
import vavi.awt.dnd.BasicDTListener;
import vavi.util.ByteUtil;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.util.function.Predicate.not;


public class Common {

    private static final Logger logger = getLogger(Common.class.getName());

    public static final Charset charset = Charset.forName("ms932");

    private static String[] args;

    static void setCommandLineArgs(String[] args) {
        Common.args = args;
    }

    public static String[] getCommandLineArgs() {
        return args;
    }

    /**
     *
     * @return 0 origin
     */
    /**
     * Turns a WinForms filter spec, {@code "VGM files(*.vgm;*.vgz)|*.vgm;*.vgz|MDX files(*.mdx)|*.mdx"},
     * into file chooser filters, in order. Directories are always accepted so that they can be
     * browsed into.
     */
    public static List<FileFilter> toFileFilters(String spec) {
        List<FileFilter> filters = new ArrayList<>();
        String[] p = spec.split("\\|");
        for (int i = 0; i + 1 < p.length; i += 2) {
            String description = p[i].trim();
            List<String> exts = new ArrayList<>();
            for (String pattern : p[i + 1].split(";")) {
                pattern = pattern.trim().toLowerCase();
                if (pattern.startsWith("*")) pattern = pattern.substring(1);
                if (!pattern.isEmpty()) exts.add(pattern);
            }
            filters.add(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) return true;
                    String name = f.getName().toLowerCase();
                    return exts.contains(".*") || exts.stream().anyMatch(name::endsWith);
                }

                @Override
                public String getDescription() {
                    return description;
                }
            });
        }
        return filters;
    }

    public static int getFilterIndex(JFileChooser fc) {
        FileFilter[] filters = fc.getChoosableFileFilters();
        for (int i = 0; i < filters.length; i++) {
            if (fc.getFileFilter() == filters[i]) {
                return i;
            }
        }
        return -1;
    }

    public static final int DEV_WaveOut = 0;
    public static final int DEV_DirectSound = 1;
    public static final int DEV_WasapiOut = 2;
    public static final int DEV_AsioOut = 3;
    public static final int DEV_SPPCM = 4;
    public static final int DEV_Null = 5;

    public static final int VGMProcSampleRate = 44100;
    public static Path settingFilePath;

    /**
     * find an asciiz string from a byte array
     * @return nullable
     */
    private static byte[] getByteArray(byte[] buf, int[] adr) {
        if (adr[0] >= buf.length) {
            logger.log(Level.TRACE, adr[0] + " > " + buf.length);
            return null;
        }

        List<Byte> ary = new ArrayList<>();
        while (buf[adr[0]] != 0 || buf[adr[0] + 1] != 0) {
            ary.add(buf[adr[0]]);
            adr[0]++;
            ary.add(buf[adr[0]]);
            adr[0]++;
        }
        adr[0] += 2;

        return ByteUtil.toByteArray(ary);
    }

    private static String getAsciiz(byte[] buf, int[] adr) {
        byte[] b = getByteArray(buf, adr);
        return b != null ? new String(b, UTF_16LE) : null;
    }

    public static MetaData getMetaData(byte[] buf, int adr) {
        MetaData metaData = new MetaData();

String x = null;
        try {
            int[] adr_ = {adr};
            String s = getAsciiz(buf, adr_); if (s != null) metaData.set(Tag.Title, s);
            s = getAsciiz(buf, adr_); if (s != null) metaData.set(Tag.TitleJ, s);
            s = getAsciiz(buf, adr_); if (s != null) metaData.set(Tag.GameTitle, s);
            s = getAsciiz(buf, adr_); if (s != null) metaData.set(Tag.GameTitleJ, s);
            s = getAsciiz(buf, adr_); if (s != null) metaData.set(Tag.GameSystem, s);
            s = getAsciiz(buf, adr_); if (s != null) metaData.set(Tag.GameSystemJ, s);
            s = getAsciiz(buf, adr_); if (s != null) metaData.set(Tag.Composer, s);
            s = getAsciiz(buf, adr_); if (s != null) metaData.set(Tag.ComposerJ, s);
            s = getAsciiz(buf, adr_); if (s != null) metaData.set(Tag.Converter, s);
            s = getAsciiz(buf, adr_); if (s != null) metaData.set(Tag.Maker, s);
            s = getAsciiz(buf, adr_); if (s != null) metaData.set(Tag.Note, s);
            // Lyric(Custom extensions)
            byte[] bLyric = Common.getByteArray(buf, adr_);
            if (bLyric != null) {
x = new String(bLyric);
logger.log(Level.TRACE, "\nlyric: " + StringUtil.getDump(bLyric));
                int i = 0;
                int st = 0;
                while (i < bLyric.length) {
                    int h = bLyric[i] & 0xff;
                    int l = bLyric[i + 1] & 0xff;
                    if ((h == 0x5b && l == 0x00 && i != 0) || i >= bLyric.length - 2) {
                        if ((i >= bLyric.length - 2) || (bLyric[i + 2] != 0x5b || bLyric[i + 3] != 0x00)) {
                            String m = new String(bLyric, st, i - st + ((i >= bLyric.length - 2) ? 2 : 0), UTF_16LE);
                            st = i;

                            int p = m.indexOf("]");
                            if (p > 0) {
                                int cnt = Integer.parseInt(m.substring(1, p - 1));
                                m = m.substring(m.indexOf("]") + 1);
                                metaData.add(Tag.Lyric, cnt + "," + cnt + "." + m);
                            } else {
                                metaData.add(Tag.Lyric, m);
                            }
                        }
                    }
                    i += 2;
                }
            }
        } catch (Exception ex) {
logger.log(Level.ERROR, "lyric: " + x);
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
logger.log(Level.INFO, "metaData: " + metaData);

        return metaData;
    }

    public static String getNRDString(byte[] buf, /* ref */ int[] index) {
        return getNRDString(buf, index, (byte) 0);
    }

    public static String getNRDString(byte[] buf, /* ref */ int[] index, byte del /* = 0 */) {
        if (buf == null || buf.length < 1 || index[0] < 0 || index[0] >= buf.length) return "";

        try {
            List<Byte> lst = new ArrayList<>();
            for (; buf[index[0]] != 0; index[0]++) {
                if (del == 0) {
                    if (buf.length > index[0] + 1 && buf[index[0]] == 0x1a && buf[index[0] + 1] == 0x00)
                        break;
                } else {
                    if (buf.length > index[0] + 1 && buf[index[0]] == del)
                        break;
                }
                lst.add(buf[index[0]]);
            }

            String n = new String(ByteUtil.toByteArray(lst), charset);
            index[0]++;

            return n;
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
        return "";
    }

    public static int range(int n, int min, int max) {
        return (n > max) ? max : Math.max(n, min);
    }

    public static int getV(byte[] buf, /* ref */ int[] musicPtr) {
        int s = 0, n = 0;

        do {
            n |= (buf[musicPtr[0]] & 0x7f) << s;
            s += 7;
        } while ((buf[musicPtr[0]++] & 0x80) > 0);

        return n;
    }

    public static int searchFMNote(int freq) {
        int m = Integer.MAX_VALUE;
        int n = 0;
        for (int i = 0; i < 12 * 5; i++) {
            //if (freq < Tables.FmFNum[i]) break;
            //n = i;
            int a = Math.abs(freq - Tables.FmFNum[i]);
            if (m > a) {
                m = a;
                n = i;
            }
        }
        return n - 12 * 3;
    }

    public static int searchSSGNote(float freq) {
        float m = Float.MAX_VALUE;
        int n = 0;
        for (int i = 0; i < 12 * 8; i++) {
            // if (freq < Tables.freqTbl[i]) break;
            // n = i;
            float a = Math.abs(freq - Tables.freqTbl[i]);
            if (m > a) {
                m = a;
                n = i;
            }
        }
        return n;
    }

    public static int searchPCMNote(int ml, int mul) {
        int m = Integer.MAX_VALUE;
        ml = ml % (1024 * mul);
        int n = 0;
        for (int i = 0; i < 12; i++) {
            int a = Math.abs(ml - Tables.pcmpitchTbl[i] * mul);
            if (m > a) {
                m = a;
                n = i;
            }
        }
        return n;
    }

    public static Path getApplicationFolder() {
        return Path.of(System.getProperty("user.dir"));
    }

    public static Path getApplicationDataFolder(boolean make /* = false */) {
        try {
            String appPath = System.getProperty("user.home");
            Path fullPath = Path.of(appPath, ".config/kuma", "mdplayer");
            if (!Files.exists(fullPath)) Files.createDirectories(fullPath);

            return fullPath;
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            return null;
        }
    }

    public static Path getOperationFolder(boolean make /* = false */) {
        try {
            Path appDataFolder = getApplicationDataFolder(false);
            if (appDataFolder == null) return null;
            Path fullPath = appDataFolder.resolve("operation");
            if (!Files.exists(fullPath)) Files.createDirectory(fullPath);
            else
                // If it exists, clear the contents of that folder.
                deleteDataUnderDirectory(fullPath);
            return fullPath;
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Empty the directory
     */
    private static void deleteDataUnderDirectory(Path directory) throws IOException {
logger.log(Level.DEBUG, "delete: " + directory);
        Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .filter(p -> p != directory)
                .map(Path::toFile)
                .forEach(File::delete);
    }

    /**
     * Change the attributes of a folder or file
     */
    public static void removeReadonlyAttribute(Path dir) {
logger.log(Level.DEBUG, "delete attributes: " + dir);
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .filter(p -> p != dir)
                    .map(Path::toFile)
                    .filter(not(File::canWrite))
                    .forEach(f -> f.setWritable(true));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Looks up localized resources of type BufferedImage.
     *
     * TODO "/mdplayer/resources/" should move to .properties inside
     */
    public static BufferedImage getImage(String name) {
        try {
            return ImageIO.read(Common.class.getResourceAsStream("/mdplayer/resources/" + name + ".png"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public enum EnmModel {
        VirtualModel, RealModel
    }

    public enum EnmInstFormat {
        FMP7,
        MDX,
        TFI,
        MUSICLALF,
        MUSICLALF2,
        MML2VGM,
        NRTDRV,
        HUSIC,
        VOPM,
        PMD,
        MUCOM88,
        DMP,
        OPNI,
        OPLI,
        MGSCSCC_PLAIN,
        RYM2612,
        SendMML2VGM
    }

    public enum EnmArcType {
        unknown,
        ZIP,
        LZH
    }

    public enum EnmRealModel {
        unknown,
        SCCI,
        GIMIC
    }

    public static String getClipboard() {
        Toolkit kit = Toolkit.getDefaultToolkit();
        Clipboard clip = kit.getSystemClipboard();

        try {
            return (String) clip.getData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void setClipboard(String str) {
        Toolkit kit = Toolkit.getDefaultToolkit();
        Clipboard clip = kit.getSystemClipboard();

        StringSelection ss = new StringSelection(str);
        clip.setContents(ss, ss);
    }

    private static Robot robot;

    public static void sendKey(int mod, int key) {
        if (robot == null) {
            try {
                robot = new Robot();
            } catch (AWTException e) {
                // github workflow headless mode causes exception
                logger.log(Level.WARNING, e.getMessage(), e);
                return;
            }
        }
        robot.setAutoWaitForIdle(true);
        robot.keyPress(mod);
        robot.keyPress(key);
        robot.keyRelease(mod);
    }

    /**
     * this is the DnD target sample for a file name from external applications
     * <pre>
     *   new DropTarget(component, DnDConstants.ACTION_COPY_OR_MOVE, new DTListener(), true);
     * </pre>
     */
    public static class DTListener extends BasicDTListener {

        private final Consumer<List<java.io.File>> drop;

        public DTListener(Consumer<List<java.io.File>> drop) {
            this.drop = drop;
            this.dragAction = DnDConstants.ACTION_COPY_OR_MOVE;
        }

        /**
         * Called by isDragOk
         * Checks to see if the flavor drag flavor is acceptable
         * @param ev the DropTargetDragEvent object
         * @return whether the flavor is acceptable
         */
        @Override
        protected boolean isDragFlavorSupported(DropTargetDragEvent ev) {
            return ev.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        /**
         * Called by drop
         * Checks the flavors and operations
         * @param ev the DropTargetDropEvent object
         * @return the chosen dataFlavor or null if none match
         */
        @Override
        protected DataFlavor chooseDropFlavor(DropTargetDropEvent ev) {
// logger.log(Level.TRACE, ev.getCurrentDataFlavorsAsList());
            if (ev.isLocalTransfer() && ev.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return DataFlavor.javaFileListFlavor;
            }
            DataFlavor chosen = null;
            if (ev.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                chosen = DataFlavor.javaFileListFlavor;
            }
            return chosen;
        }

        /**
         * You need to implement here dropping procedure.
         * data is deserialized clone
         * @param data dropped
         */
        @Override
        @SuppressWarnings("unchecked")
        protected boolean dropImpl(DropTargetDropEvent ev, Object data) {
            drop.accept((List<java.io.File>) data);
            return true;
        }
    }

    static {
        try {
            try (InputStream is = SettingAboutPanel.class.getResourceAsStream("/META-INF/maven/vavi/vavi-apps-mdplayer/pom.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    version = props.getProperty("version", "undefined in pom.properties");
                } else {
                    version = System.getProperty("vavi.test.version", "undefined");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static final String version;
}
