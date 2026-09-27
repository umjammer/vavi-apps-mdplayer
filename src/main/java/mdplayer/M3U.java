package mdplayer;

import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import mdplayer.driver.FileFormat;
import vavi.util.archive.Archive;
import vavi.util.archive.Archives;
import vavi.util.archive.Entry;

import static java.lang.System.getLogger;
import static mdplayer.Common.charset;


public class M3U {

    private static final Logger logger = getLogger(M3U.class.getName());

    public static PlayList loadM3U(String filename, String rootPath) {
        try {
            PlayList pl = new PlayList();

            try (var sr = new Scanner(Files.newInputStream(Path.of(filename)), charset)) {
                String line;
                while (sr.hasNextLine()) {
                    line = sr.nextLine().trim();
                    if (line.isEmpty()) continue;
                    if (line.charAt(0) == '#') continue;

                    PlayList.Music ms = analyzeLine(line, rootPath);
                    if (ms != null) {
                        ms.format = FileFormat.getFileFormat(ms.fileName);
                        pl.getMusics().add(ms);
                    }
                }
            }

            return pl;

        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            return new PlayList();
        }
    }

    public static PlayList loadM3U(Archive archive, Entry entry, String zipFileName) {
        try {
            PlayList pl = new PlayList();

            try (Scanner sr = new Scanner(archive.getInputStream(entry), charset)) {
                String line;
                while (sr.hasNextLine()) {
                    line = sr.nextLine();
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (line.charAt(0) == '#') continue;

                    PlayList.Music ms = analyzeLine(line, "");
                    ms.format = FileFormat.getFileFormat(ms.fileName);
                    ms.arcFileName = zipFileName;
                    pl.getMusics().add(ms);
                }
            }

            return pl;

        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            return new PlayList();
        }
    }

    private static PlayList loadM3U(String archiveFile, String fileName, String zipFileName) {
        try {
            PlayList pl = new PlayList();
            Archive cmd = Archives.getArchive(new File(archiveFile));
            byte[] buf = cmd.getInputStream(cmd.getEntry(fileName)).readAllBytes();
            String[] text = new String(buf, charset).split("\n");

            for (String txt : text) {
                String line = txt.trim();
                if (line.isEmpty()) continue;
                if (line.charAt(0) == '#') continue;

                PlayList.Music ms = analyzeLine(line, "");
                ms.format = FileFormat.getFileFormat(ms.fileName);
                ms.arcFileName = zipFileName;
                pl.getMusics().add(ms);
            }

            return pl;

        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            return new PlayList();
        }
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
