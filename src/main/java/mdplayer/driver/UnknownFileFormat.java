package mdplayer.driver;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import mdplayer.Common.EnmArcType;
import mdplayer.PlayList;
import mdplayer.Setting;
import mdplayer.lib.vgm.Vgm;
import mdplayer.driver.vgm.VgmDriver;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;
import vavi.util.ByteUtil;
import vavi.util.archive.Archive;
import vavi.util.archive.Archives;
import vavi.util.archive.Entry;
import vavi.util.archive.zip.JdkZipEntry;

import static java.lang.System.getLogger;


/**
 * UnknownFileFormat.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-07 nsano initial version <br>
 */
public class UnknownFileFormat extends BaseFileFormat {

    private static final Logger logger = getLogger(UnknownFileFormat.class.getName());

    @Override
    public String[] getExtensions() {
        return new String[] {};
    }

    @Override
    public List<PlayList.Music> getMusic(String file, byte[] buf, String zipFile /* = null */, Archive archive, Entry entry /* = null */) {
        List<PlayList.Music> musics = new ArrayList<>();
        PlayList.Music music = new PlayList.Music();

        music.format = this;
        music.fileName = file;
        music.arcFileName = zipFile;
        music.arcType = EnmArcType.unknown;
        if (zipFile != null && !zipFile.isEmpty())
            music.arcType = zipFile.toLowerCase().lastIndexOf(".zip") != -1 ? EnmArcType.ZIP : EnmArcType.LZH;
        music.title = "unknown";
        music.game = "unknown";
        music.type = "-";

        if (buf.length < 0x40) {
            musics.add(music);
            return musics;
        }
        if (ByteUtil.readLeInt(buf, 0x00) != Vgm.FCC_VGM) {
            // musics.add(Music);
            // return musics;
            // Might be VGZ so check.
            try {
                int num;
                buf = new byte[1024]; // Process 1Kbytes at a time

                if (entry == null || entry instanceof JdkZipEntry) {
                    if (archive == null && entry == null) {
                        archive = Archives.getArchive(new File(zipFile));
                        entry = archive.getEntry(file);
                    }
                    try (InputStream inStream = archive.getInputStream(entry);
                         InputStream decompStream = Archives.getInputStream(new BufferedInputStream(inStream));
                         ByteArrayOutputStream outStream = new ByteArrayOutputStream()
                    ) {
                        while ((num = decompStream.read(buf, 0, buf.length)) > 0) {
                            outStream.write(buf, 0, num);
                        }
                        buf = outStream.toByteArray();
                    }
                } else {
                    buf = archive.getInputStream(entry).readAllBytes();
                }
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
                // It was not vgz
            }
        }

        if (ByteUtil.readLeInt(buf, 0x00) != Vgm.FCC_VGM) {
            musics.add(music);
            return musics;
        }

        music.format = null; // TODO VGM
        int version = ByteUtil.readLeInt(buf, 0x08);
        String _version = "%d.%d%d".formatted((version & 0xf00) / 0x100, (version & 0xf0) / 0x10, (version & 0xf));

        int vgmGd3 = ByteUtil.readLeInt(buf, 0x14);
        MetaData metaData = new MetaData();
        if (vgmGd3 != 0) {
            int vgmGd3Id = ByteUtil.readLeInt(buf, vgmGd3 + 0x14);
            if (vgmGd3Id != Vgm.FCC_GD3) {
                musics.add(music);
                return musics;
            }
            metaData = (new VgmDriver()).retrieveMetaData(buf, vgmGd3);
        }

        int totalCounter = ByteUtil.readLeInt(buf, 0x18);
        int vgmLoopOffset = ByteUtil.readLeInt(buf, 0x1c);
        int loopCounter = ByteUtil.readLeInt(buf, 0x20);

        music.title = metaData.getFirst(Tag.Title);
        music.titleJ = metaData.getFirst(Tag.TitleJ);
        music.game = metaData.getFirst(Tag.GameTitle);
        music.gameJ = metaData.getFirst(Tag.GameTitleJ);
        music.composer = metaData.getFirst(Tag.Composer);
        music.composerJ = metaData.getFirst(Tag.ComposerJ);
        music.vgmby = metaData.getFirst(Tag.Maker);

        music.converted = metaData.getFirst(Tag.Converter);
        music.notes = metaData.getFirst(Tag.Note);

        double sec = (double) totalCounter / (double) Setting.getInstance().getOutputDevice().getSampleRate();
        int tcMinutes = (int) (sec / 60);
        sec -= tcMinutes * 60;
        int tcSecond = (int) sec;
        sec -= tcSecond;
        int tcMillisecond = (int) (sec * 100.0);
        music.duration = "%2d:%02d:%02d".formatted(tcMinutes, tcSecond, tcMillisecond);

        return musics;
    }

    @Override
    public List<PlayList.Music> getMusic(PlayList.Music ms, byte[] buf, String zipFile /* = null */) {
        List<PlayList.Music> musics = new ArrayList<>();
        PlayList.Music music = new PlayList.Music();

        music.format = this;
        music.fileName = ms.fileName;
        music.arcFileName = zipFile;
        music.title = "unknown";
        music.game = "unknown";
        music.type = "-";

        musics.add(music);
        return musics;
    }

    @Override
    public Plugin getPlugin() {
        return null; // TODO
    }

    @Override
    public List<PlayList.Music> addFileLoop(PlayList.Music mc, Archive archive, Entry entry) {
        return null;
    }

    @Override
    public List<PlayList.Music> addFileLoop(int index, PlayList.Music mc, Archive archive, Entry entry) {
        return null;
    }

    @Override
    public boolean isSupported(InputStream is) throws IOException {
        return false;
    }

    @Override
    public int getMarkSize() {
        return 0;
    }
}
