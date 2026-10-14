package mdplayer;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mdplayer.driver.FileFormat;
import vavi.util.archive.Archive;
import vavi.util.archive.Entry;
import vavi.util.archive.m3u.M3uArchiveSpi;
import vavi.util.archive.spi.ArchiveSpi;

import static java.lang.System.getLogger;
import static mdplayer.Common.charset;


public class M3U {

    private static final Logger logger = getLogger(M3U.class.getName());

    /** reads the playlists, ms932 is for the ones that are not UTF-8 */
    private static final ArchiveSpi m3uSpi = new M3uArchiveSpi();

    /** an m3u needs no #EXTM3U header, so the spi is asked directly instead of {@link vavi.util.archive.Archives} */
    private static Archive open(Object m3u) throws IOException {
        return m3uSpi.createArchiveInstance(m3u, Map.of(M3uArchiveSpi.ENV_KEY_CHARSET, charset));
    }

    public static PlayList loadM3U(String filename, String rootPath) {
        try {
            return toPlayList(open(new File(filename)), rootPath, null);
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            return new PlayList();
        }
    }

    public static PlayList loadM3U(Archive archive, Entry entry, String zipFileName) {
        try {
            return toPlayList(open(archive.getInputStream(entry)), "", zipFileName);
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            return new PlayList();
        }
    }

    /** an entry's name is the whole line, nezplug's "::" fields included */
    private static PlayList toPlayList(Archive m3u, String rootPath, String zipFileName) throws IOException {
        PlayList pl = new PlayList();
        try {
            for (Entry e : m3u.entries()) {
                PlayList.Music ms = analyzeLine(e.getName().trim(), rootPath);
                if (ms == null) continue;
                // an #EXTINF title stands in when the line gives none
                if (ms.title == null || ms.title.isEmpty()) {
                    String title = e.getComment();
                    if (title != null && !title.isEmpty()) {
                        ms.title = title;
                        ms.titleJ = title;
                    }
                }
                ms.format = FileFormat.getFileFormat(ms.fileName);
                if (zipFileName != null) ms.arcFileName = zipFileName;
                pl.getMusics().add(ms);
            }
        } finally {
            m3u.close();
        }
        return pl;
    }

    private static PlayList.Music analyzeLine(String line, String rootPath) {
        PlayList.Music ms = new PlayList.Music();

        try {
            // If there is no "::", the whole file will be treated as a file name and processing will end.
            if (!line.contains("::")) {
                ms.fileName = line;
                if (!Path.of(ms.fileName).isAbsolute() && !rootPath.isEmpty()) {
                    ms.fileName = Path.of(rootPath, ms.fileName).toString();
                }

                return ms;
            }

            String[] buf = line.split("::");

            ms.fileName = buf[0].trim();
            if (!Path.of(ms.fileName).isAbsolute() && !rootPath.isEmpty()) {
                ms.fileName = Path.of(rootPath, ms.fileName).toString();
            }
            if (buf.length < 1) return ms;

            buf = buf[1].split(",");
            List<String> lbuf = new ArrayList<>();
            for (int i = 0; i < buf.length; ) {
                StringBuilder s = new StringBuilder();
                boolean flg;
                do {
                    flg = false;
                    s.append(buf[i]);
                    if (!buf[i].isEmpty() && buf[i].lastIndexOf('\\') == buf[i].length() - 1) {
                        s.append(",");
                        flg = true;
                    }
                    i++;
                } while (flg);
                lbuf.add(s.toString().replace("\\", ""));
            }
            buf = lbuf.toArray(String[]::new);

            String fType = buf[0].trim().toUpperCase();
            if (buf.length < 2) return ms;

            ms.songNo = analyzeSongNo(buf[1].trim()) - (fType.equals("NSF") ? 1 : 0);
            if (buf.length < 3) return ms;

            ms.title = buf[2].trim();
            ms.titleJ = buf[2].trim();
            if (buf.length < 4) return ms;

            ms.time = buf[3].trim();
            if (buf.length < 5) return ms;

            analyzeLoopTime(buf[4], ms);
            if (buf.length < 6) return ms;

            ms.fadeoutTime = buf[5].trim();
            if (buf.length < 7) return ms;

            try { ms.loopCount = Integer.parseInt(buf[6].trim()); } catch (NumberFormatException e) { ms.loopCount = -1; }
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            return null;
        }

        return ms;
    }

    private static int analyzeSongNo(String s) {
        int n;

        if (!s.isEmpty() && s.charAt(0) == '$') {
            try {
                n = Integer.parseInt(s.substring(1), 16);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, e);
                return -1;
            }
        } else {
            try {
                n = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, e);
                return -1;
            }
        }

        return n;
    }

    private static void analyzeLoopTime(String s, PlayList.Music ms) {
        ms.loopStartTime = "";
        ms.loopEndTime = "";

        if (!s.isEmpty() && s.charAt(s.length() - 1) == '-') {
            ms.loopStartTime = s.substring(0, s.length() - 1);
            return;
        }

        ms.loopEndTime = s;
    }
}
