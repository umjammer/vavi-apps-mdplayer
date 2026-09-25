package mdplayer.driver.vgm;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.zip.GZIPInputStream;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import mdplayer.PlayList;
import mdplayer.driver.BaseFileFormat;
import mdplayer.lib.vgm.Vgm;
import mdplayer.driver.Plugin;
import musicDriverInterface.MetaData;
import vavi.sound.sampled.md.MdEncoding;
import vavi.sound.sampled.md.MdFileFormatType;
import vavi.util.ByteUtil;
import vavi.util.archive.Archive;
import vavi.util.archive.Archives;
import vavi.util.archive.Entry;

import static vavi.util.compat.Util.changeExtension;
import static vavi.util.compat.Util.getExtension;


/**
 * VGMFileFormat.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-07 nsano initial version <br>
 */
public class VGMFileFormat extends BaseFileFormat {

    private static final ResourceBundle rb = ResourceBundle.getBundle("mdplayer/properties/resources");

    private static final Logger logger = System.getLogger(VGMFileFormat.class.getName());

    @Override
    public String[] getExtensions() {
        return new String[] {".vgm", ".vgz"};
    }

    @Override
    public List<PlayList.Music> getMusic(String file, byte[] buf, String zipFile /* = null */, Archive archive, Entry entry /* = null */) {
        if (buf == null) return Collections.singletonList(new PlayList.Music());
        // a .vgz comes here as it is on disk
        if (buf.length > 2 && (buf[0] & 0xff) == 0x1f && (buf[1] & 0xff) == 0x8b) {
            try (InputStream is = new GZIPInputStream(new ByteArrayInputStream(buf))) {
                buf = is.readAllBytes();
            } catch (IOException ex) {
                logger.log(Level.ERROR, ex.getMessage(), ex);
                return Collections.singletonList(new PlayList.Music());
            }
        }
        return getMusicCommon(null, buf, zipFile);
    }

    @Override
    public List<PlayList.Music> getMusic(PlayList.Music ms, byte[] buf, String zipFile /* = null */) {
        return getMusicCommon(ms, buf, zipFile);
    }

    private static final int FCC_VGM = 0x206D6756; // "Vgm "

    @Override
    public String[] getPresetMixerBalance() {

        return new String[] {
                "DriverBalance_VGM.mbc",
                rb.getString("DefaultVolumeBalance_VGM")
        };
    }

    @Override
    protected boolean isMatchFcc(int fcc) {
        return fcc == Vgm.FCC_VGM;
    }

    @Override
    protected boolean isMatchMetaData(int fcc) {
        return fcc == Vgm.FCC_GD3;
    }

    @Override
    public MetaData getMetaData() {
        int vgmGd3 = ByteUtil.readLeInt(this.srcBuf, 0x14);
        return new VgmDriver().retrieveMetaData(this.srcBuf, vgmGd3);
    }

    @Override
    protected MetaData getMetaData(byte[] buf, Object... args) {
        return new VgmDriver().retrieveMetaData(buf, args);
    }

    @Override
    protected byte[] getBytesFromZipFileInternal(Archive archive, Entry entry, byte[] buf) {
        try {
            int vgm = ByteUtil.readLeInt(buf);
            if (vgm != VGMFileFormat.FCC_VGM) {

                try (InputStream inStream = archive.getInputStream(entry);
                     InputStream decompStream = Archives.getInputStream(new BufferedInputStream(inStream))
                ) {
                    return decompStream.readAllBytes();
                }
            }
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    protected byte[] addFileLoopInternal(PlayList.Music mc) {
        if (getExtension(mc.fileName).equalsIgnoreCase(".vgm")) {
            mc.fileName = changeExtension(mc.fileName, ".vgz");
        } else {
            mc.fileName = changeExtension(mc.fileName, ".Vgm");
        }
        try {
            return Files.readAllBytes(Path.of(mc.fileName));
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public Plugin getPlugin() {
        return Plugin.getPlugin(VGMPlugin.class);
    }

    @Override
    public Encoding getEncoding() {
        return new MdEncoding("VGM", "vgm,vgz");
    }

    @Override
    public Type getType() {
        return new MdFileFormatType("VGM", "vgm");
//        return new MdFileFormatType("VGZ", "vgz");
    }

    @Override
    public int getMarkSize() {
        return 128;
    }

    @Override
    public boolean isSupported(InputStream is) throws IOException {
//logger.log(Level.INFO, "\n" + StringUtil.getDump(is, 0, 32));
        byte[] buf = new byte[getMarkSize()];
        is.readNBytes(buf, 0, buf.length);
//logger.log(Level.INFO, "%x, %x, %s".formatted(FCC_VGM, ByteUtil.readLeInt(buf), StringUtil.getDump(buf)));
        return FCC_VGM == ByteUtil.readLeInt(buf);
    }
}
