package mdplayer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;
import javax.swing.JOptionPane;

import mdplayer.Common.EnmArcType;
import mdplayer.driver.FileFormat;
import vavi.util.archive.Archive;
import vavi.util.archive.Entry;
import vavi.util.serdes.Element;
import vavi.util.serdes.JacksonXMLBeanBinder;
import vavi.util.serdes.Serdes;

import static java.lang.System.getLogger;
import static mdplayer.Common.charset;


@Serdes(beanBinder = JacksonXMLBeanBinder.class)
public class PlayList implements Serializable, Cloneable {

    private static final Logger logger = getLogger(PlayList.class.getName());

    @Serdes(beanBinder = JacksonXMLBeanBinder.class)
    public static class Music {
        @com.fasterxml.jackson.annotation.JsonIgnore
        public transient FileFormat format;
        public String playingNow;
        public String fileName;
        public String arcFileName;
        public EnmArcType arcType = EnmArcType.unknown;
        public String type = "-";

        public String title;
        public String game;
        public String system;
        public String composer;
        public String titleJ;
        public String gameJ;
        public String systemJ;
        public String composerJ;

        public String converted;
        public String notes;
        public String vgmby;
        public String remark;
        public String duration;

        public String time = "";
        public String loopStartTime = "";
        public String loopEndTime = "";
        public String fadeoutTime = "";
        public int loopCount = -1;

        public int songNo = -1;

        @Override public String toString() {
            return new StringJoiner(", ", Music.class.getSimpleName() + "[", "]")
                    .add("format=" + format)
                    .add("playingNow='" + playingNow + "'")
                    .add("fileName='" + fileName + "'")
                    .add("arcFileName='" + arcFileName + "'")
                    .add("arcType=" + arcType)
                    .add("type='" + type + "'")
                    .add("title='" + title + "'")
                    .add("game='" + game + "'")
                    .add("system='" + system + "'")
                    .add("composer='" + composer + "'")
                    .add("titleJ='" + titleJ + "'")
                    .add("gameJ='" + gameJ + "'")
                    .add("systemJ='" + systemJ + "'")
                    .add("composerJ='" + composerJ + "'")
                    .add("converted='" + converted + "'")
                    .add("notes='" + notes + "'")
                    .add("vgmby='" + vgmby + "'")
                    .add("remark='" + remark + "'")
                    .add("duration='" + duration + "'")
                    .add("time='" + time + "'")
                    .add("loopStartTime='" + loopStartTime + "'")
                    .add("loopEndTime='" + loopEndTime + "'")
                    .add("fadeoutTime='" + fadeoutTime + "'")
                    .add("loopCount=" + loopCount)
                    .add("songNo=" + songNo)
                    .toString();
        }
    }

    @Element(sequence = 1)
    private int size;

    @Element(sequence = 2, value = "$1")
    private List<Music> musics = new ArrayList<>();

    public List<Music> getMusics() {
        return musics;
    }

    public void setMusics(List<Music> value) {
        musics = value;
    }

    @Override
    public PlayList clone() {
        PlayList playList = new PlayList();

        return playList;
    }

    public void save(String fileName) {
        Path fullPath;

        if (fileName == null || fileName.isEmpty()) {
            fullPath = Common.settingFilePath;
            fullPath = fullPath != null ? fullPath.resolve("DefaultPlayList.xml") : Path.of("DefaultPlayList.xml");
        } else {
            fullPath = Path.of(fileName);
        }

        try (OutputStream sw = Files.newOutputStream(fullPath)) {
            this.size = musics.size();
            Serdes.Util.serialize(this, sw);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void saveM3U(String fileName) {
        Path basePath = Path.of(fileName).getParent();

        try (PrintWriter sw = new PrintWriter(new FileWriter(fileName))) {
            for (Music ms : this.musics) {
                Path path = Path.of(ms.fileName).getParent();
                if (path.equals(basePath)) {
                    sw.println(Path.of(ms.fileName).getFileName());
                } else {
                    sw.println(ms.fileName);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static PlayList load(String fileName) {
        try {
            Path fullPath;
            if (fileName == null || fileName.isEmpty()) {
                fullPath = Common.settingFilePath;
                fullPath = fullPath.resolve("DefaultPlayList.xml");
            } else {
                fullPath = Path.of(fileName);
            }

            if (Files.exists(fullPath) && Files.size(fullPath) > 10) {
                try (InputStream sr = Files.newInputStream(fullPath)) {
                    // the binder builds and returns a fresh bean; the one passed in stays empty
                    PlayList pl = Serdes.Util.deserialize(sr, new PlayList());
                    if (pl.musics == null) pl.musics = new ArrayList<>();
                    // a half-written entry has no file to show or play
                    pl.musics.removeIf(m -> m == null || m.fileName == null);
                    return pl;
                }
            }
            return new PlayList();
        } catch (NoSuchFileException ex) {
            logger.log(Level.ERROR, ex.toString());
            return new PlayList();
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            return new PlayList();
        }
    }

    public static PlayList loadM3U(String filename) {
        try {
            PlayList pl = new PlayList();

            try (Scanner sr = new Scanner(Files.newInputStream(Path.of(filename)), charset)) {
                String line;
                while (sr.hasNextLine()) {
                    line = sr.nextLine();
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (line.charAt(0) == '#') continue;

                    if (Path.of(line).getParent() != null) {
                        line = Path.of(filename).getParent().resolve(line).toString();
                    }
                    Music ms = new Music();
                    ms.fileName = line;
                    pl.musics.add(ms);
                }
            }

            return pl;
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            return new PlayList();
        }
    }

    /**
     * Called after this list's songs were added to by {@link #addFile} or {@link #insertFile}, on the
     * thread that added them; a view of the list refreshes itself from it.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public transient Runnable changed;

    private void fireChanged() {
        if (changed != null) changed.run();
    }

    public void addFile(String filename) {
        try {
            Music mc = new Music();
            mc.format = FileFormat.getFileFormat(filename);
            mc.fileName = filename;

            addFileLoop(mc, null, null);
            fireChanged();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Failed to add a file.\nDetail\nMessage=%s".formatted(ex.getMessage()),
                    "Error", JOptionPane.ERROR_MESSAGE);
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }

    public void insertFile(/* ref */ int[] index, String[] filenames) {
        try {
            for (String filename : filenames) {
                Music mc = new Music();
                mc.format = FileFormat.getFileFormat(filename);
                mc.fileName = filename;

                addFileLoop(index, mc, null, null);
            }
            fireChanged();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Failed to add a file.\nDetail\nMessage=%s".formatted(ex.getMessage()),
                    "Error" , JOptionPane.ERROR_MESSAGE);
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }

    private void addFileLoop(Music mc, Archive archive, Entry entry /* = null */) {
        try {
            List<Music> added = mc.format.addFileLoop(mc, archive, entry);
            if (added == null) return;

            added = new ArrayList<>(added); // what a format hands back may not be changed
            added.removeIf(m -> m == null || m.fileName == null); // a song with no file cannot be played
            this.musics.addAll(added);
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }

    private void addFileLoop(int[] index, Music mc, Archive archive, Entry entry /* = null */) {
        try {
            // the place is this list's to keep, and the indexed variants of the formats have fallen
            // behind the plain ones (some drop archive entries, zip needs the archive open)
            List<Music> added = mc.format.addFileLoop(mc, archive, entry);
            if (added == null) return;

            added = new ArrayList<>(added); // what a format hands back may not be changed
            added.removeIf(m -> m == null || m.fileName == null); // a song with no file cannot be played
            index[0] = Math.min(index[0], this.musics.size());
            this.musics.addAll(index[0], added);
            index[0] += added.size();
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }
}
