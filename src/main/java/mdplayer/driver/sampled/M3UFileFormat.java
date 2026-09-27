package mdplayer.driver.sampled;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mdplayer.M3U;
import mdplayer.PlayList;
import mdplayer.driver.BaseFileFormat;
import mdplayer.driver.Plugin;
import vavi.util.archive.Archive;
import vavi.util.archive.Entry;


/**
 * M3UFileFormat.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-07 nsano initial version <br>
 */
public class M3UFileFormat extends BaseFileFormat {

    private static final Logger logger = System.getLogger(M3UFileFormat.class.getName());

    @Override
    public String[] getExtensions() {
        return new String[] {".m3u"};
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

        Path parent = Path.of(mc.fileName).getParent();
        String rootPath = parent != null ? parent.toString() : "";
        PlayList pl;
        if (entry == null) pl = M3U.loadM3U(mc.fileName, rootPath);
        else pl = M3U.loadM3U(archive, entry, mc.arcFileName);
        if (pl == null) return null;
        if (pl.getMusics() == null || pl.getMusics().isEmpty()) return null;

        List<PlayList.Music> musics = new ArrayList<>();
        // each song is read by its own format, the m3u only names them
        for (PlayList.Music m : pl.getMusics()) {
            if (m.format == null) continue;
            Entry e = entry != null ? archive.getEntry(m.fileName) : null;
            if (entry != null && e == null) continue;
            try {
                List<PlayList.Music> added = m.format.addFileLoop(m, archive, e);
                if (added != null) musics.addAll(added);
            } catch (IOException ex) {
                logger.log(Level.ERROR, ex.getMessage(), ex);
            }
        }
        return musics;
    }

    @Override
    public List<PlayList.Music> addFileLoop(int index, PlayList.Music mc, Archive archive, Entry entry /* = null */) {

        Path parent = Path.of(mc.fileName).getParent();
        String rootPath = parent != null ? parent.toString() : "";
        PlayList pl;
        if (entry == null) pl = M3U.loadM3U(mc.fileName, rootPath);
        else pl = M3U.loadM3U(archive, entry, mc.arcFileName);
        if (pl == null) return null;
        if (pl.getMusics() == null || pl.getMusics().isEmpty()) return null;

        List<PlayList.Music> musics = new ArrayList<>();
        // each song is read by its own format, the m3u only names them
        for (PlayList.Music m : pl.getMusics()) {
            if (m.format == null) continue;
            Entry e = entry != null ? archive.getEntry(m.fileName) : null;
            if (entry != null && e == null) continue;
            try {
                List<PlayList.Music> added = m.format.addFileLoop(index, m, archive, e);
                if (added != null) musics.addAll(added);
            } catch (IOException ex) {
                logger.log(Level.ERROR, ex.getMessage(), ex);
            }
        }
        return musics;
    }

    @Override
    public int getMarkSize() {
        return 0;
    }

    @Override
    public boolean isSupported(InputStream is) throws IOException {
//        if (isCompressedStream(is)) return false;
//        return Arrays.stream(getExtensions()).anyMatch(e -> Path.of(SoundUtil.getSource(is)).toString().toLowerCase().endsWith(e));
        return false;
    }
}
