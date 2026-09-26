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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.StringJoiner;
import javax.swing.JOptionPane;

import mdplayer.Common.EnmArcType;
import mdplayer.driver.FileFormat;
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

    /** the row of the song played last when the list was saved, or -1 */
    private int lastPlayed = -1;

    public int getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(int value) {
        lastPlayed = value;
    }

    @Override
    public PlayList clone() {
        PlayList playList = new PlayList();

        return playList;
    }

    /** one save at a time: the window closing and the shutdown hook both save the same file */
    private static final Object saveLock = new Object();

    /**
     * Writes the list. It goes to a temporary file first and replaces the old one only when it is
     * all written, so a save that fails or is cut short by the JVM ending leaves the old list as it
     * was, rather than a list cut off in the middle.
     */
    public void save(String fileName) {
        Path fullPath;

        if (fileName == null || fileName.isEmpty()) {
            fullPath = Common.settingFilePath;
            fullPath = fullPath != null ? fullPath.resolve("DefaultPlayList.xml") : Path.of("DefaultPlayList.xml");
        } else {
            fullPath = Path.of(fileName);
        }
        fullPath = fullPath.toAbsolutePath();

        // the songs as they are now; the window may go on adding to the list while it is written
        PlayList snapshot = new PlayList();
        snapshot.musics = new ArrayList<>(musics);
        snapshot.musics.removeIf(Objects::isNull); // a copy taken while songs were being put in
        snapshot.musics.forEach(PlayList::clean); // lists made before the songs were cleaned when read
        snapshot.lastPlayed = lastPlayed;
        snapshot.size = snapshot.musics.size();

        synchronized (saveLock) {
            Path tmp = fullPath.resolveSibling(fullPath.getFileName() + ".tmp");
            try {
                try (OutputStream sw = Files.newOutputStream(tmp)) {
                    Serdes.Util.serialize(snapshot, sw);
                }
                try {
                    Files.move(tmp, fullPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, fullPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException f) {
                    e.addSuppressed(f);
                }
                throw new UncheckedIOException(e);
            }
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
            keepBroken(fileName);
            return new PlayList();
        }
    }

    /**
     * A list that could not be read is copied aside: the empty list shown in its place is saved
     * over it when the player ends, and the songs would be gone for good.
     */
    private static void keepBroken(String fileName) {
        try {
            Path fullPath = fileName == null || fileName.isEmpty() ? Common.settingFilePath.resolve("DefaultPlayList.xml") : Path.of(fileName);
            if (!Files.exists(fullPath)) return;
            Path copy = fullPath.resolveSibling(fullPath.getFileName() + ".broken-" + System.currentTimeMillis());
            Files.copy(fullPath, copy);
            logger.log(Level.WARNING, "the play list could not be read, kept as " + copy);
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
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

    /**
     * The songs {@code filename} holds, read from the file (tags, an archive's or a list's entries)
     * without changing this or any list, so it can be done away from the event dispatch thread.
     *
     * @return the songs, none when the file cannot be read
     */
    public static List<Music> read(String filename) {
        try {
            Music mc = new Music();
            mc.format = FileFormat.getFileFormat(filename);
            mc.fileName = filename;

            // the indexed variants of the formats have fallen behind the plain ones (some drop
            // archive entries, zip needs the archive open), the place is the caller's to keep
            List<Music> added = mc.format.addFileLoop(mc, null, null);
            if (added == null) return new ArrayList<>();

            added = new ArrayList<>(added); // what a format hands back may not be changed
            added.removeIf(m -> m == null || m.fileName == null); // a song with no file cannot be played
            added.forEach(PlayList::clean);
            return added;
        } catch (Exception ex) {
            logger.log(Level.ERROR, filename + ": " + ex.getMessage(), ex);
            return new ArrayList<>();
        }
    }

    /** the text fields of {@link Music} */
    private static final List<Field> textFields = Arrays.stream(Music.class.getFields())
            .filter(f -> f.getType() == String.class && !Modifier.isStatic(f.getModifiers()))
            .toList();

    /**
     * Makes a song's text fit in XML. A tag is sometimes a fixed size block read as text, NULs and
     * binary included (an MFi's "prot" chunk comes out as the notes), and the XML writer refuses
     * those: the whole save failed on one such song and the list was not saved.
     * The text ends at a NUL, as a C string does, and other characters XML cannot hold are dropped.
     */
    static void clean(Music music) {
        for (Field f : textFields) {
            try {
                if (f.get(music) instanceof String s) {
                    String c = clean(s);
                    if (!c.equals(s)) f.set(music, c);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** {@code s} up to a NUL, without the characters XML 1.0 cannot hold */
    static String clean(String s) {
        int nul = s.indexOf('\0');
        if (nul >= 0) s = s.substring(0, nul);
        StringBuilder sb = null;
        for (int i = 0; i < s.length(); ) {
            int c = s.codePointAt(i);
            int n = Character.charCount(c);
            boolean ok = c == 0x9 || c == 0xa || c == 0xd || (c >= 0x20 && c <= 0xd7ff) ||
                    (c >= 0xe000 && c <= 0xfffd) || (c >= 0x10000 && c <= 0x10ffff);
            if (!ok && sb == null) sb = new StringBuilder(s.substring(0, i));
            if (ok && sb != null) sb.appendCodePoint(c);
            i += n;
        }
        return sb == null ? s : sb.toString();
    }

    public void addFile(String filename) {
        try {
            this.musics.addAll(read(filename));
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
                List<Music> added = read(filename);
                index[0] = Math.min(index[0], this.musics.size());
                this.musics.addAll(index[0], added);
                index[0] += added.size();
            }
            fireChanged();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Failed to add a file.\nDetail\nMessage=%s".formatted(ex.getMessage()),
                    "Error" , JOptionPane.ERROR_MESSAGE);
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }
    }
}
