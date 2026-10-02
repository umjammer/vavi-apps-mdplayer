package mdplayer.driver.archive;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mdplayer.Common;
import mdplayer.M3U;
import mdplayer.PlayList;
import mdplayer.driver.BaseFileFormat;
import mdplayer.driver.FileFormat;
import mdplayer.driver.sampled.M3UFileFormat;
import mdplayer.driver.vgm.VGMFileFormat;
import mdplayer.driver.Plugin;
import vavi.util.archive.Archive;
import vavi.util.archive.Archives;
import vavi.util.archive.Entry;

import static vavi.util.compat.Util.changeExtension;
import static vavi.util.compat.Util.getExtension;


/**
 * LZHFileFormat.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-07 nsano initial version <br>
 */
public class LZHFileFormat extends BaseFileFormat {

    private static final Logger logger = System.getLogger(LZHFileFormat.class.getName());

    @Override
    public String[] getExtensions() {
        return new String[] {".lzh"};
    }

    @Override
    public List<PlayList.Music> getMusic(String file, byte[] buf, String zipFile /* = null */, Archive archive, Entry entry /* = null */) {
        PlayList.Music music = new PlayList.Music();
        return Collections.singletonList(music);
    }

    @Override
    public List<PlayList.Music> getMusic(PlayList.Music ms, byte[] buf, String zipFile /* = null */) {
        return getMusicCommon(ms, buf, zipFile);
    }

    @Override
    public Plugin getPlugin() {
        return null;
    }

    @Override
    public List<PlayList.Music> addFileLoop(PlayList.Music mc, Archive archive, Entry entry /* = null */) {
        if (entry != null) return null;

        mc.arcFileName = mc.fileName;
        mc.arcType = Common.EnmArcType.LZH;
        List<String> zipMember = new ArrayList<>();
        List<PlayList.Music> mMember = new ArrayList<>();

        for (Entry e : archive.entries()) {
            if (!(FileFormat.getFileFormat(e.getName()) instanceof M3UFileFormat)) {
                zipMember.add(entry.getName());
            } else {
                PlayList pl = M3U.loadM3U(archive, e, mc.arcFileName);
                mMember.addAll(pl.getMusics());
            }
        }

        for (String zm : zipMember) {
            boolean found = false;
            for (PlayList.Music m : mMember) {
                if (m.fileName.equals(zm)) {
                    found = true;
                    break;
                }
            }
            if (!found && FileFormat.getFileFormat(zm) instanceof VGMFileFormat) {
                String vzm;
                if (getExtension(zm).equalsIgnoreCase(".vgm")) vzm = changeExtension(zm, ".vgz");
                else vzm = changeExtension(zm, ".vgm");
                for (PlayList.Music m : mMember) {
                    if (m.fileName.equals(vzm)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                PlayList.Music zmc = new PlayList.Music();
                zmc.fileName = zm;
                zmc.arcFileName = mc.arcFileName;
                zmc.arcType = mc.arcType;
                mMember.add(zmc);
            }
        }

        List<PlayList.Music> musics = new ArrayList<>();

        for (Entry e : archive.entries()) {
            for (PlayList.Music m : mMember) {
                String vzm = "";
                if (getExtension(m.fileName).equalsIgnoreCase(".vgm"))
                    vzm = changeExtension(m.fileName, ".vgz");
                else if (getExtension(m.fileName).equalsIgnoreCase(".vgz"))
                    vzm = changeExtension(m.fileName, ".Vgm");

                if (e.getName().equals(m.fileName) || e.getName().equals(vzm)) {
                    m.format = FileFormat.getFileFormat(m.fileName);
                    m.arcFileName = mc.arcFileName;
                    m.arcType = mc.arcType;
                    // each member is read by its own format
                    try {
                        List<PlayList.Music> added = m.format != null ? m.format.addFileLoop(m, archive, e) : null;
                        if (added != null) musics.addAll(added);
                    } catch (IOException ex) {
                        logger.log(Level.ERROR, ex.getMessage(), ex);
                    }

                    // When multiple m3u files are included, there are cases where multiple files with the same name are added.
                    // To prevent this, we will break here.
                    break;
                }
            }
        }

        return musics;
    }

    @Override
    public List<PlayList.Music> addFileLoop(int index, PlayList.Music mc, Archive archive, Entry entry/* = null*/) {
        if (entry != null) return null;

        mc.arcFileName = mc.fileName;
        mc.arcType = Common.EnmArcType.LZH;
        List<String> zipMember = new ArrayList<>();
        List<PlayList.Music> mMember = new ArrayList<>();

        for (Entry ent : archive.entries()) {
            if (!(FileFormat.getFileFormat(ent.getName()) instanceof M3UFileFormat)) {
                zipMember.add(ent.getName());
            } else {
                PlayList pl = M3U.loadM3U(archive, ent, mc.arcFileName);
                mMember.addAll(pl.getMusics());
            }
        }

        for (String zm : zipMember) {
            boolean found = false;
            for (PlayList.Music m : mMember) {
                if (m.fileName.equals(zm)) {
                    found = true;
                    break;
                }
            }
            if (!found && FileFormat.getFileFormat(zm) instanceof VGMFileFormat) {
                String vzm;
                if (getExtension(zm).equalsIgnoreCase(".vgm")) vzm = changeExtension(zm, ".vgz");
                else vzm = changeExtension(zm, ".vgm");
                for (PlayList.Music m : mMember) {
                    if (m.fileName.equals(vzm)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                PlayList.Music zmc = new PlayList.Music();
                zmc.fileName = zm;
                zmc.arcFileName = mc.arcFileName;
                zmc.arcType = mc.arcType;
                mMember.add(zmc);
            }
        }

        List<PlayList.Music> musics = new ArrayList<>();

        for (Entry ent : archive.entries()) {
            for (PlayList.Music m : mMember) {
                String vzm = "";
                if (getExtension(m.fileName).equalsIgnoreCase(".vgm"))
                    vzm = changeExtension(m.fileName, ".vgz");
                else if (getExtension(m.fileName).equalsIgnoreCase(".vgz"))
                    vzm = changeExtension(m.fileName, ".Vgm");

                if (ent.getName().equals(m.fileName) || ent.getName().equals(vzm)) {
                    m.format = FileFormat.getFileFormat(m.fileName);
                    m.arcFileName = mc.arcFileName;
                    m.arcType = mc.arcType;
                    // each member is read by its own format
                    try {
                        List<PlayList.Music> added = m.format != null ? m.format.addFileLoop(index, m, archive, ent) : null;
                        if (added != null) musics.addAll(added);
                    } catch (IOException ex) {
                        logger.log(Level.ERROR, ex.getMessage(), ex);
                    }

                    // When multiple m3u files are included, there are cases where multiple files with the same name are added.
                    // To prevent this, we will break here.
                    break;
                }
            }
        }
        return musics;
    }

    @Override
    public void load(InputStream is, String fn) throws IOException {
        try {
            is.mark(2);
            Archive archive = Archives.getArchive(is);
            Entry entry = archive.getEntry(fn);
            this.realFormat = FileFormat.getFileFormat(fn);
            if (this.realFormat != FileFormat.unknown) {
                realFormat.load(archive.getInputStream(entry), null);
                this.filename = fn;
            } else {
                throw new FileNotFoundException(fn);
            }
        } catch (IOException e) {
            is.reset();
            throw e;
        }
    }

    private static final byte[] magic = {0x2D, 0x6C, 0x68}; // TODO more?

    @Override
    public int getMarkSize() {
        return magic.length;
    }

    @Override
    public boolean isSupported(InputStream is) throws IOException {
        byte[] buf = new byte[getMarkSize()];
        is.readNBytes(buf, 0, buf.length);
        return Arrays.equals(magic, buf);
    }
}
