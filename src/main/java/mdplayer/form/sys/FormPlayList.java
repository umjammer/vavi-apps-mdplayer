package mdplayer.form.sys;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import mdplayer.Common;
import mdplayer.Common.EnmArcType;
import mdplayer.PlayList;
import mdplayer.Setting;
import mdplayer.driver.FileFormat;
import vavi.awt.dnd.Droppable;
import vavi.util.compat.Tuple4;

import static java.lang.System.getLogger;
import static vavi.util.compat.Util.getExtension;
import static vavi.util.compat.Util.getFileNameWithoutExtension;


/**
 * The play list window.
 * <p>
 * The table shows {@link PlayList#getMusics()} as it is, so the list and the rows cannot drift
 * apart: every edit is made to the list and then the table is told. The song being played is
 * followed by identity, not by row number, so that moving, sorting, adding or removing rows does
 * not lose it.
 */
public class FormPlayList extends JFrame {

    private static final ResourceBundle rb = ResourceBundle.getBundle("mdplayer/properties/resources");

    private static final Logger logger = getLogger(FormPlayList.class.getName());

    /** true while the window is not shown */
    public boolean isClosed = true;
    public Setting setting;

    public FileFormat playFormat = FileFormat.unknown;
    public EnmArcType playArcType = EnmArcType.unknown;

    private PlayList playList;
    private final FormMain frmMain;

    /** set while the song on the list is playing; the screen loop reads it off the EDT */
    private volatile boolean playing = false;

    /** the song being played, or the last one that was */
    private PlayList.Music playingMusic;
    /**
     * where {@link #playingMusic} was when it was last seen; when that song has been removed, the
     * song after this row is the next one
     */
    private int lastPlayIndex = -1;

    /** the kind of list last opened or saved, so that saving offers the same kind back */
    private boolean m3u = false;

    private final Random rand = new Random();
    /** the songs a random play went through, most recent last, for "previous" to walk back */
    private final Deque<PlayList.Music> randomStack = new ArrayDeque<>();
    private boolean isInitialOpenFolder = true;

    /**
     * the file extensions a drop or a folder is searched for: whatever a registered
     * {@link FileFormat} reads, so a new driver's files can be dropped as soon as it is registered
     */
    private static final Set<String> sext = ServiceLoader.load(FileFormat.class).stream()
            .map(ServiceLoader.Provider::get)
            .map(FileFormat::getExtensions)
            .filter(Objects::nonNull)
            .flatMap(Arrays::stream)
            .map(ex -> ex.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

    /** the column being sorted by and which way, -1 while the list is in the order it was made */
    private int sortColumn = -1;
    private boolean sortAscending = true;

    /** the window scale, as the main window's "zoom" */
    private int zoom = 1;

    public FormPlayList(FormMain frm) {
        frmMain = frm;
        setting = frm.setting;
        initializeComponent();

        attach(PlayList.load(null));

        restoreBounds();
    }

    /** Makes the table show {@code pl}. */
    private void attach(PlayList pl) {
        playList = pl;
        playList.changed = this::listChanged;
        playingMusic = null;
        lastPlayIndex = -1;
        randomStack.clear();
        clearSort();
        refresh();
    }

    /** {@link PlayList#changed}: songs were added, possibly from another thread. */
    private void listChanged() {
        if (SwingUtilities.isEventDispatchThread()) {
            refresh();
        } else {
            SwingUtilities.invokeLater(this::refresh);
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public int getMusicCount() {
        return playList.getMusics().size();
    }

    public PlayList getPlayList() {
        return playList;
    }

    /**
     * Marks a song as the one being played.
     *
     * @param n the row, or -1 for the last one, or -2 for the first one
     * @return the song's type, song number, file and archive, or null when the list is empty
     */
    public Tuple4<Integer, Integer, String, String> setStart(int n) {
        List<PlayList.Music> musics = playList.getMusics();
        if (musics.isEmpty()) return null;

        int i = n == -1 ? musics.size() - 1 : n == -2 ? 0 : n;
        if (i < 0 || i >= musics.size()) return null;

        PlayList.Music music = musics.get(i);
        markPlaying(music);
        return new Tuple4<>(typeOf(music), music.songNo, music.fileName, music.arcFileName);
    }

    public void play() {
        playing = true;
    }

    public void stop() {
        playing = false;
    }

    public void save() {
        if (setting.getOther().getEmptyPlayList()) {
            playList.setMusics(new ArrayList<>());
        }
        playList.save(null);
    }

    /** Makes the table show the list as it is now. */
    public void refresh() {
        model.fireTableDataChanged();
    }

    /** The row {@link #playingMusic} is at, or -1. */
    private int indexOf(PlayList.Music music) {
        if (music == null) return -1;
        List<PlayList.Music> musics = playList.getMusics();
        for (int i = 0; i < musics.size(); i++) {
            if (musics.get(i) == music) return i;
        }
        return -1;
    }

    /**
     * The row of the song being played. When that song is no longer on the list this is the row
     * before where it was, so that "next" goes on with the song that took its place.
     */
    private int playIndex() {
        int i = indexOf(playingMusic);
        if (i >= 0) {
            lastPlayIndex = i;
            return i;
        }
        return Math.min(lastPlayIndex, playList.getMusics().size() - 1);
    }

    private void markPlaying(PlayList.Music music) {
        playingMusic = music;
        lastPlayIndex = indexOf(music);
        playFormat = music.format != null ? music.format : FileFormat.unknown;
        playArcType = music.arcType != null ? music.arcType : EnmArcType.unknown;
        dgvList.repaint();
        if (lastPlayIndex >= 0) {
            SwingUtilities.invokeLater(() -> {
                int row = indexOf(playingMusic);
                if (row >= 0 && row < dgvList.getRowCount()) {
                    dgvList.scrollRectToVisible(dgvList.getCellRect(row, 0, true));
                }
            });
        }
    }

    /** The music driver type a song was set to, A to J, as an index. */
    private static int typeOf(PlayList.Music music) {
        if (music.type == null || music.type.isEmpty() || music.type.equals("-")) return 0;
        int m = music.type.charAt(0) - 'A';
        return m < 0 || m > 9 ? 0 : m;
    }

    /** Loads and starts a song and marks it as the one being played. */
    private boolean load(PlayList.Music music) {
        playing = false;
        try {
            if (!frmMain.loadAndPlay(typeOf(music), music.songNo, music.fileName, music.arcFileName)) {
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            return false;
        }
        markPlaying(music);
        playing = true;
        return true;
    }

    private void playRow(int row) {
        if (row < 0 || row >= playList.getMusics().size()) return;
        load(playList.getMusics().get(row));
    }

    public void nextPlay() {
        nextPlayMode(0);
    }

    /**
     * Plays the song after the one being played.
     *
     * @param mode 0: in order, stopping after the last one, 1: at random, 2: in order, going round,
     *             3: the same song again
     */
    public void nextPlayMode(int mode) {
        List<PlayList.Music> musics = playList.getMusics();
        int size = musics.size();
        // go on from the song played last even when it was stopped, as the list shows it
        int pi = playIndex();
        playing = false;
        if (size == 0) return;

        switch (mode) {
        case 0: // normal
            pi++;
            if (pi >= size) return;
            break;
        case 1: // random
            if (playingMusic != null && indexOf(playingMusic) >= 0) {
                randomStack.addLast(playingMusic);
                while (randomStack.size() > 1000) randomStack.removeFirst();
            }
            pi = rand.nextInt(size);
            break;
        case 2: // all songs loop
            pi++;
            if (pi >= size) pi = 0;
            break;
        case 3: // one song loop
            if (pi < 0) pi = 0;
            break;
        default:
            return;
        }

        load(musics.get(pi));
    }

    /**
     * Plays the song before the one being played.
     *
     * @param mode as {@link #nextPlayMode}; at random, this goes back through the songs played
     */
    public void prevPlay(int mode) {
        List<PlayList.Music> musics = playList.getMusics();
        if (musics.isEmpty()) return;

        PlayList.Music music = null;
        if (mode == 1) {
            while (!randomStack.isEmpty()) {
                PlayList.Music m = randomStack.removeLast();
                if (indexOf(m) >= 0) {
                    music = m;
                    break;
                }
            }
        }
        if (music == null) {
            int pi = playIndex();
            if (pi < 1) {
                if (mode != 2 || musics.size() < 2 || pi < 0) return;
                pi = musics.size();
            }
            music = musics.get(pi - 1);
        }

        load(music);
    }

    /** The song being played, as it is on the list, or null. */
    public PlayList.Music getPlayingSongInfo() {
        PlayList.Music music = playingMusic;
        if (music == null) return null;
        if (music.format == null && music.fileName != null) {
            // what a list read from a file carries does not include the format
            try {
                music.format = FileFormat.getFileFormat(music.fileName);
            } catch (Exception e) {
                logger.log(Level.DEBUG, e.getMessage(), e);
            }
        }
        return music;
    }

    // ---- editing

    /** The songs on the selected rows, in row order. */
    private List<PlayList.Music> selectedMusics() {
        List<PlayList.Music> musics = playList.getMusics();
        List<PlayList.Music> selected = new ArrayList<>();
        for (int r : dgvList.getSelectedRows()) {
            if (r < musics.size()) selected.add(musics.get(r));
        }
        return selected;
    }

    /** Selects the rows the songs are at. */
    private void select(List<PlayList.Music> selected) {
        dgvList.clearSelection();
        for (PlayList.Music music : selected) {
            int i = indexOf(music);
            if (i >= 0) dgvList.addRowSelectionInterval(i, i);
        }
        int lead = selected.isEmpty() ? -1 : indexOf(selected.getFirst());
        if (lead >= 0) dgvList.scrollRectToVisible(dgvList.getCellRect(lead, 0, true));
    }

    private void tsmiDelThis_Click(ActionEvent ev) {
        int[] rows = dgvList.getSelectedRows();
        if (rows.length < 1) return;

        List<PlayList.Music> musics = playList.getMusics();
        int pi = playIndex();
        Set<PlayList.Music> removed = Collections.newSetFromMap(new IdentityHashMap<>());
        removed.addAll(selectedMusics());
        int before = 0; // how many rows above the song being played go
        for (int r : rows) {
            if (r < pi) before++;
        }
        boolean playingRemoved = removed.contains(playingMusic);
        musics.removeIf(removed::contains);
        randomStack.removeIf(removed::contains);
        clearSort();
        refresh();

        if (playingRemoved) {
            lastPlayIndex = pi - before - 1;
        } else {
            playIndex();
        }

        if (!musics.isEmpty()) {
            int next = Math.min(rows[0], musics.size() - 1);
            dgvList.setRowSelectionInterval(next, next);
        }
    }

    private void tsmiDelAllMusic_Click(ActionEvent ev) {
        if (playList.getMusics().isEmpty()) return;

        int res = JOptionPane.showConfirmDialog(this, text("msgDelAllMusic", "All songs in the playlist will be removed. Is that okay?"),
                getTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.YES_OPTION) return;

        playList.getMusics().clear();
        randomStack.clear();
        playingMusic = null;
        lastPlayIndex = -1;
        clearSort();
        refresh();
    }

    /**
     * Moves the selected songs one row up or down, keeping them selected. A song that is already
     * against the end, or against another selected song that is, stays.
     */
    private void move(int direction) {
        List<PlayList.Music> selected = selectedMusics();
        if (selected.isEmpty()) return;

        List<PlayList.Music> musics = playList.getMusics();
        Set<PlayList.Music> sel = Collections.newSetFromMap(new IdentityHashMap<>());
        sel.addAll(selected);
        if (direction < 0) {
            for (int i = 1; i < musics.size(); i++) {
                if (sel.contains(musics.get(i)) && !sel.contains(musics.get(i - 1))) {
                    musics.add(i - 1, musics.remove(i));
                }
            }
        } else {
            for (int i = musics.size() - 2; i >= 0; i--) {
                if (sel.contains(musics.get(i)) && !sel.contains(musics.get(i + 1))) {
                    musics.add(i + 1, musics.remove(i));
                }
            }
        }
        clearSort();
        refresh();
        playIndex();
        select(selected);
    }

    private void tsbUp_Click(ActionEvent ev) {
        move(-1);
    }

    private void tsbDown_Click(ActionEvent ev) {
        move(1);
    }

    /** Sets the music driver type of the selected songs to the one on the menu item. */
    private void tsmiA_Click(ActionEvent ev) {
        String type = ((JMenuItem) ev.getSource()).getText();
        List<PlayList.Music> selected = selectedMusics();
        for (PlayList.Music music : selected) {
            music.type = type;
        }
        refresh();
        select(selected);
    }

    // ---- sorting

    /** Sorts the list by a column; the same column again turns the order round. */
    private void sortBy(int column) {
        if (column < 0 || column == cols.clmPlayingNow.ordinal()) return;
        List<PlayList.Music> selected = selectedMusics();

        sortAscending = column != sortColumn || !sortAscending;
        sortColumn = column;

        Comparator<PlayList.Music> c = Comparator.comparing(m -> sortKey(m, column), (a, b) -> {
            if (a == null) return b == null ? 0 : 1; // blanks go last either way
            if (b == null) return -1;
            int r = a instanceof Integer ia && b instanceof Integer ib
                    ? Integer.compare(ia, ib)
                    : String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString());
            return sortAscending ? r : -r;
        });
        playList.getMusics().sort(c); // a stable sort, so sorting by one column then another works
        updateHeaders();
        refresh();
        playIndex();
        select(selected);
    }

    private static Object sortKey(PlayList.Music m, int column) {
        Object v = value(m, cols.values()[column]);
        if (v instanceof String s && s.isBlank()) return null;
        return v;
    }

    /** The list order is no longer the sorted one. */
    private void clearSort() {
        if (sortColumn == -1) return;
        sortColumn = -1;
        updateHeaders();
    }

    private void updateHeaders() {
        for (cols c : cols.values()) {
            String h = header(c.name());
            if (c.ordinal() == sortColumn) h += sortAscending ? " ▲" : " ▼";
            dgvList.getColumnModel().getColumn(c.ordinal()).setHeaderValue(h);
        }
        dgvList.getTableHeader().repaint();
    }

    // ---- adding

    private void tsbOpenPlayList_Click(ActionEvent ev) {
        JFileChooser ofd = new JFileChooser();
        FileFilter xmlFilter = Common.toFileFilters("XML file(*.xml)|*.xml").getFirst();
        FileFilter m3uFilter = Common.toFileFilters("M3U file(*.m3u)|*.m3u").getFirst();
        ofd.addChoosableFileFilter(xmlFilter);
        ofd.addChoosableFileFilter(m3uFilter);
        ofd.setFileFilter(m3u ? m3uFilter : xmlFilter);
        ofd.setDialogTitle(text("dlgOpenPlayList", "Select a playlist file"));
        setInitialDirectory(ofd);

        if (ofd.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        isInitialOpenFolder = false;

        try {
            String filename = ofd.getSelectedFile().getPath();

            m3u = filename.toLowerCase().endsWith(".m3u");
            playing = false;

            if (!m3u) {
                attach(PlayList.load(filename));
            } else {
                PlayList pl = PlayList.loadM3U(filename);
                attach(new PlayList());
                for (PlayList.Music ms : pl.getMusics()) {
                    playList.addFile(ms.fileName);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this, text("msgLoadFailed", "File loading failed."));
        }
    }

    private void tsbSavePlayList_Click(ActionEvent ev) {
        JFileChooser sfd = new JFileChooser();
        FileFilter xmlFilter = Common.toFileFilters("XML file(*.xml)|*.xml").getFirst();
        FileFilter m3uFilter = Common.toFileFilters("M3U file(*.m3u)|*.m3u").getFirst();
        sfd.addChoosableFileFilter(xmlFilter);
        sfd.addChoosableFileFilter(m3uFilter);
        // upstream STBL546: a list opened as an m3u is offered back as an m3u
        sfd.setFileFilter(m3u ? m3uFilter : xmlFilter);
        sfd.setDialogTitle(text("dlgSavePlayList", "Save the playlist file"));
        setInitialDirectory(sfd);

        if (sfd.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        isInitialOpenFolder = false;
        String filename = sfd.getSelectedFile().getPath();

        if (getExtension(Path.of(filename).getFileName().toString()).isEmpty()) {
            filename += sfd.getFileFilter() == m3uFilter ? ".m3u" : ".xml";
        }

        try {
            m3u = filename.toLowerCase().endsWith(".m3u");

            if (!m3u)
                playList.save(filename);
            else
                playList.saveM3U(filename);
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this, text("msgSaveFailed", "File saving failed."));
        }
    }

    private void setInitialDirectory(JFileChooser fc) {
        String dataPath = setting.getOther().getDefaultDataPath();
        if (dataPath != null && !dataPath.isEmpty() && Files.exists(Path.of(dataPath)) && isInitialOpenFolder) {
            fc.setCurrentDirectory(new File(dataPath));
        }
    }

    private void tsbAddMusic_Click(ActionEvent ev) {
        JFileChooser ofd = new JFileChooser();
        Common.toFileFilters(rb.getString("cntSupportFile")).forEach(ofd::addChoosableFileFilter);
        ofd.setDialogTitle(text("dlgAddMusic", "Select a file"));
        int filterIndex = setting.getOther().getFilterIndex();
        FileFilter[] filters = ofd.getChoosableFileFilters();
        if (filterIndex >= 0 && filterIndex < filters.length) {
            ofd.setFileFilter(filters[filterIndex]);
        } else {
            ofd.setFileFilter(filters[filters.length - 1]); // all supported files
        }
        setInitialDirectory(ofd);
        ofd.setMultiSelectionEnabled(true);

        if (ofd.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        isInitialOpenFolder = false;
        setting.getOther().setFilterIndex(Common.getFilterIndex(ofd));

        addFiles(Arrays.stream(ofd.getSelectedFiles()).map(File::getAbsolutePath).toList(), -1);
    }

    private void tsbAddFolder_Click(ActionEvent ev) {
        JFileChooser fbd = new JFileChooser();
        fbd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fbd.setDialogTitle(text("dlgAddFolder", "Please specify the folder."));
        String dataPath = setting.getOther().getDefaultDataPath();
        if (dataPath != null && !dataPath.isEmpty() && Files.exists(Path.of(dataPath))) {
            fbd.setCurrentDirectory(new File(dataPath));
        }

        if (fbd.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        addFiles(List.of(fbd.getSelectedFile().getAbsolutePath()), -1);
    }

    /**
     * Adds songs to the list, what is in a folder included.
     *
     * @param files files or folders
     * @param row the row to insert them before, or -1 for after the last one
     * @return true when any was added
     */
    private boolean addFiles(List<String> files, int row) {
        List<String> filenames = new ArrayList<>();
        getTrueFileNameList(filenames, files);
        if (filenames.isEmpty()) return false;

        List<PlayList.Music> musics = playList.getMusics();
        int at = row < 0 || row > musics.size() ? musics.size() : row;
        int before = musics.size();

        Cursor cursor = getCursor();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            playList.insertFile(new int[] {at}, filenames.toArray(String[]::new));
        } finally {
            setCursor(cursor);
        }

        int added = musics.size() - before;
        if (added <= 0) return false;

        clearSort();
        refresh();
        playIndex();
        dgvList.setRowSelectionInterval(at, at + added - 1);
        dgvList.scrollRectToVisible(dgvList.getCellRect(at, 0, true));
        return true;
    }

    /** A file or a folder dropped on the list. */
    private boolean dropped(Path path) {
        try {
            return addFiles(List.of(path.toAbsolutePath().toString()), dropRow());
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this, text("msgLoadFailed", "File loading failed."));
            return false;
        }
    }

    /**
     * The row a drop goes before, or -1 for after the last one. The drop carries no place, but the
     * pointer is still where it was let go ({@link JComponent#getMousePosition()} is null during a
     * native drag on some platforms, so the screen position is used). The lower half of a row
     * means after it, so a song let go just under the one playing is played next.
     */
    private int dropRow() {
        PointerInfo pointer = MouseInfo.getPointerInfo();
        if (pointer == null) return -1;
        Point p = pointer.getLocation();
        SwingUtilities.convertPointFromScreen(p, dgvList);
        int row = dgvList.rowAtPoint(p);
        if (row < 0) return -1;
        Rectangle r = dgvList.getCellRect(row, 0, true);
        return p.y >= r.y + r.height / 2 ? row + 1 : row;
    }

    /** Lists the playable files among {@code files}, going into folders. */
    private static void getTrueFileNameList(List<String> res, List<String> files) {
        for (String f : files) {
            Path path = Path.of(f);
            if (Files.isDirectory(path)) {
                try (Stream<Path> s = Files.list(path)) {
                    getTrueFileNameList(res, s.map(Path::toString).sorted().toList());
                } catch (IOException e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                }
            } else if (Files.exists(path)) {
                if (!res.contains(f) && sext.contains(getExtension(f).toLowerCase())) {
                    res.add(f);
                }
            }
        }
    }

    // ---- the files that go with a song

    /** the song {@link #text}, {@link #mml} and {@link #img} were looked up for */
    private PlayList.Music checkedMusic;
    private String text = "";
    private String mml = "";
    private String img = "";

    /** Looks for a text, an MML and an image next to a song when it starts. */
    private void timer1_Tick(ActionEvent ev) {
        if (setting == null) return;
        PlayList.Music music = playing ? playingMusic : null;
        if (music == checkedMusic) return;
        checkedMusic = music;

        text = mml = img = "";
        String fn = music == null ? null : music.arcFileName != null && !music.arcFileName.isEmpty() ? music.arcFileName : music.fileName;
        Path dir = fn == null ? null : Path.of(fn).toAbsolutePath().getParent();
        if (dir != null) {
            String bfn = dir.resolve(getFileNameWithoutExtension(Path.of(fn).getFileName().toString())).toString();
            String bfnFld = dir.getFileName() != null ? dir.resolve(dir.getFileName()).toString() : bfn;

            text = find(bfn, bfnFld, setting.getOther().getTextExt());
            mml = find(bfn, bfnFld, setting.getOther().getMMLExt());
            img = find(bfn, bfnFld, setting.getOther().getImageExt());
        }

        tsbTextExt.setEnabled(!text.isEmpty());
        tsbMMLExt.setEnabled(!mml.isEmpty());
        tsbImgExt.setEnabled(!img.isEmpty());

        if (setting.getOther().getAutoOpenText()) open(text);
        if (setting.getOther().getAutoOpenMML()) open(mml);
        if (setting.getOther().getAutoOpenImg()) open(img);
    }

    /** The first of {@code base.ext} or {@code folderBase.ext} that is there, or "". */
    private static String find(String base, String folderBase, String exts) {
        if (exts == null) return "";
        for (String ext : exts.split(";")) {
            ext = ext.trim();
            if (ext.isEmpty()) continue;
            if (Files.exists(Path.of(base + "." + ext))) return base + "." + ext;
            if (Files.exists(Path.of(folderBase + "." + ext))) return folderBase + "." + ext;
        }
        return "";
    }

    /** Opens a file or a folder with what the desktop opens it with. */
    private void open(String path) {
        if (path == null || path.isEmpty()) return;
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception e) {
            logger.log(Level.WARNING, "cannot open " + path + ": " + e.getMessage());
        }
    }

    private void tsbTextExt_Click(ActionEvent ev) {
        open(text);
    }

    private void tsbMMLExt_Click(ActionEvent ev) {
        open(mml);
    }

    private void tsbImgExt_Click(ActionEvent ev) {
        open(img);
    }

    private void tsmiOpenFolder_Click(ActionEvent ev) {
        List<PlayList.Music> selected = selectedMusics();
        if (selected.isEmpty()) return;
        PlayList.Music music = selected.getFirst();
        String fn = music.arcFileName != null && !music.arcFileName.isEmpty() ? music.arcFileName : music.fileName;
        Path dir = Path.of(fn).toAbsolutePath().getParent();
        if (dir != null) open(dir.toString());
    }

    // ---- the window

    private void restoreBounds() {
        Point p = setting.getLocation().getPPlayList();
        if (p != null && !p.equals(Setting.EmptyPoint)) {
            setLocation(p);
        }
        Dimension d = setting.getLocation().getPPlayListWH();
        if (d != null && d.width > 0 && d.height > 0) {
            setSize(d);
            setPreferredSize(d);
        }
    }

    private void storeBounds() {
        setting.getLocation().setPPlayList(getLocation());
        setting.getLocation().setPPlayListWH(getSize());
    }

    @Override
    public void setVisible(boolean b) {
        if (!b && isVisible()) storeBounds();
        super.setVisible(b);
        isClosed = !b;
    }

    /**
     * Scales the window as the other windows are, fonts, rows, columns and tool bar icons.
     *
     * @param zoom the main window's zoom, 1 to 4
     */
    public void setZoom(int zoom) {
        zoom = Math.max(1, zoom);
        int old = this.zoom;
        this.zoom = zoom;

        Font font = baseFont.deriveFont(baseFont.getSize2D() * zoom);
        dgvList.setFont(font);
        dgvList.getTableHeader().setFont(font);
        dgvList.setRowHeight(baseRowHeight * zoom);
        applyZoom(cmsPlayList, font);
        for (Component c : toolStrip1.getComponents()) {
            c.setFont(font);
            if (c instanceof AbstractButton b && b.getClientProperty(BASE_ICON) instanceof BufferedImage image) {
                b.setIcon(scaled(image, zoom));
            }
        }
        updateColumnVisibility();

        if (old != zoom) {
            Dimension d = getSize();
            Dimension min = new Dimension(400 * zoom, 120 * zoom);
            setMinimumSize(min);
            Dimension size = new Dimension(Math.max(min.width, d.width * zoom / old), Math.max(min.height, d.height * zoom / old));
            setPreferredSize(size);
            setSize(size);
        }
        revalidate();
        repaint();
    }

    private static void applyZoom(JComponent c, Font font) {
        c.setFont(font);
        for (Component child : c.getComponents()) {
            if (child instanceof JMenu m) {
                applyZoom(m.getPopupMenu(), font);
                m.setFont(font);
            } else if (child instanceof JComponent jc) {
                applyZoom(jc, font);
            }
        }
    }

    private static ImageIcon scaled(BufferedImage image, int zoom) {
        return new ImageIcon(zoom == 1 ? image
                : image.getScaledInstance(image.getWidth() * zoom, image.getHeight() * zoom, Image.SCALE_REPLICATE));
    }

    private static final String BASE_ICON = "mdplayer.baseIcon";

    private static void icon(AbstractButton b, String name) {
        BufferedImage image = Common.getImage(name);
        b.putClientProperty(BASE_ICON, image);
        b.setIcon(new ImageIcon(image));
    }

    // ---- the table

    /**
     * The columns of {@link #dgvList}; an ordinal is the column index.
     */
    enum cols {
        clmKey,
        clmSongNo,
        clmZipFileName,
        clmFileName,
        clmPlayingNow,
        clmEXT,
        clmType,
        clmTitle,
        clmTitleJ,
        clmDispFileName,
        clmGame,
        clmGameJ,
        clmComposer,
        clmComposerJ,
        clmVGMby,
        clmConverted,
        clmNotes,
        clmDuration
    }

    /** {@code ".../foo.vgz"} is a {@code VGZ}, not everything up to the dot. */
    private static String extension(String fileName) {
        String name = Path.of(fileName).getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toUpperCase();
    }

    /** What a column shows of a song. */
    private static Object value(PlayList.Music music, cols column) {
        return switch (column) {
            case clmKey -> 0;
            case clmSongNo -> music.songNo;
            case clmZipFileName -> music.arcFileName;
            case clmFileName -> music.fileName;
            case clmPlayingNow -> " ";
            case clmEXT -> extension(music.fileName);
            case clmType -> music.type;
            case clmTitle -> music.title;
            case clmTitleJ -> music.titleJ;
            case clmDispFileName -> Path.of(music.fileName).getFileName().toString();
            case clmGame -> music.game;
            case clmGameJ -> music.gameJ;
            case clmComposer -> music.composer;
            case clmComposerJ -> music.composerJ;
            case clmVGMby -> music.vgmby;
            case clmConverted -> music.converted;
            case clmNotes -> music.notes;
            case clmDuration -> music.duration;
        };
    }

    /** {@link PlayList#getMusics()} as a table, one song a row. */
    private final AbstractTableModel model = new AbstractTableModel() {
        @Override
        public int getRowCount() {
            return playList == null ? 0 : playList.getMusics().size();
        }

        @Override
        public int getColumnCount() {
            return cols.values().length;
        }

        @Override
        public String getColumnName(int column) {
            return header(cols.values()[column].name());
        }

        @Override
        public Object getValueAt(int row, int column) {
            List<PlayList.Music> musics = playList.getMusics();
            if (row >= musics.size()) return null; // the list changed under a paint
            PlayList.Music music = musics.get(row);
            if (column == cols.clmPlayingNow.ordinal()) return music == playingMusic ? ">" : " ";
            return value(music, cols.values()[column]);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private static final Color foreground = new Color(192, 192, 255);
    private static final Color playingForeground = Color.green.brighter();

    /** Shows the song being played in green. */
    private final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            List<PlayList.Music> musics = playList.getMusics();
            boolean isPlaying = row < musics.size() && musics.get(row) == playingMusic;
            setForeground(isPlaying ? playingForeground : isSelected ? table.getSelectionForeground() : foreground);
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    };

    private void updateColumnVisibility() {
        setColumnVisibility(cols.clmKey, false);
        setColumnVisibility(cols.clmSongNo, false);
        setColumnVisibility(cols.clmZipFileName, false);
        setColumnVisibility(cols.clmFileName, false);

        boolean showEN = tsbAll.isSelected() || tsbEnglish.isSelected();
        boolean showJA = tsbAll.isSelected() || tsbJapanese.isSelected();

        setColumnVisibility(cols.clmPlayingNow, true);
        setColumnVisibility(cols.clmEXT, true);
        setColumnVisibility(cols.clmType, true);
        setColumnVisibility(cols.clmTitle, showEN);
        setColumnVisibility(cols.clmTitleJ, showJA);
        setColumnVisibility(cols.clmDispFileName, true);
        setColumnVisibility(cols.clmGame, showEN);
        setColumnVisibility(cols.clmGameJ, showJA);
        setColumnVisibility(cols.clmComposer, showEN);
        setColumnVisibility(cols.clmComposerJ, showJA);
        setColumnVisibility(cols.clmVGMby, true);
        setColumnVisibility(cols.clmConverted, true);
        setColumnVisibility(cols.clmNotes, true);
        setColumnVisibility(cols.clmDuration, true);
    }

    /** the columns' widths at x1, as the user left them */
    private final int[] baseWidths = new int[cols.values().length];

    private void setColumnVisibility(cols column, boolean visible) {
        TableColumn col = dgvList.getColumnModel().getColumn(column.ordinal());
        if (visible) {
            col.setMinWidth(15 * zoom);
            col.setMaxWidth(Integer.MAX_VALUE);
            col.setPreferredWidth(baseWidths[column.ordinal()] * zoom);
            col.setWidth(baseWidths[column.ordinal()] * zoom);
        } else {
            col.setMinWidth(0);
            col.setMaxWidth(0);
            col.setPreferredWidth(0);
            col.setWidth(0);
        }
    }

    /** the designer's captions and widths, converted from frmPlayList.resx */
    private static final ResourceBundle resources = ResourceBundle.getBundle("mdplayer/form/sys/frmPlayList", Locale.getDefault());

    /** The caption the designer gave a column, or its name if it has none. */
    private static String header(String column) {
        return text(column + ".HeaderText", column);
    }

    /** A string of the window's, or {@code defaultValue} when there is none. */
    private static String text(String key, String defaultValue) {
        try {
            return resources.getString(key);
        } catch (MissingResourceException e) {
            return defaultValue;
        }
    }

    /** The width the designer gave a column. */
    private static int designWidth(cols column) {
        String baseName = column.name();
        if (baseName.endsWith("J")) baseName = baseName.substring(0, baseName.length() - 1);
        try {
            return Integer.parseInt(resources.getString(baseName + ".Width").trim());
        } catch (MissingResourceException | NumberFormatException e) {
            return switch (column) {
                case clmTitleJ, clmGameJ -> 200;
                case clmComposer, clmComposerJ, clmDispFileName -> 150;
                default -> 100;
            };
        }
    }

    /** Names a tool bar button and gives it the designer's caption as its tool tip. */
    private void button(AbstractButton b, String name, String icon, String tip) {
        b.setName(name);
        if (icon != null) icon(b, icon);
        b.setToolTipText(text(name + ".ToolTipText", tip));
        b.setFocusable(false);
    }

    private void menuItem(JMenuItem item, String name, String defaultText) {
        item.setName(name);
        item.setText(text(name + ".Text", defaultText));
    }

    private void initializeComponent() {
        this.toolStripContainer1 = new JPanel();
        this.dgvList = new JTable(model);
        this.cmsPlayList = new JPopupMenu();
        this.typeSettingsToolStripMenuItem = new JMenu();
        this.tsmiPlayThis = new JMenuItem();
        this.tsmiDelThis = new JMenuItem();
        this.tsmiDelAllMusic = new JMenuItem();
        this.tsmiOpenFolder = new JMenuItem();
        this.toolStrip1 = new JToolBar();
        this.tsbOpenPlayList = new JButton();
        this.tsbSavePlayList = new JButton();
        this.tsbAddMusic = new JButton();
        this.tsbAddFolder = new JButton();
        this.tsbUp = new JButton();
        this.tsbDown = new JButton();
        this.tsbAll = new JToggleButton("ALL");
        this.tsbEnglish = new JToggleButton("EN");
        this.tsbJapanese = new JToggleButton();
        ButtonGroup langGroup = new ButtonGroup();
        langGroup.add(tsbAll);
        langGroup.add(tsbEnglish);
        langGroup.add(tsbJapanese);
        this.tsbTextExt = new JButton();
        this.tsbMMLExt = new JButton();
        this.tsbImgExt = new JButton();
        this.timer1 = new Timer(1000, null);

        //
        // toolStripContainer1
        //
        // a ToolStripContainer is a panel with the tool strip along its top and the content filling
        // the rest, so that is what it is here — the table needs a scroll pane of its own, or its
        // header does not show and a long list cannot be reached
        this.toolStripContainer1.setName("toolStripContainer1");
        this.toolStripContainer1.setLayout(new BorderLayout());
        this.toolStripContainer1.add(this.toolStrip1, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(this.dgvList);
        scrollPane.getViewport().setBackground(Color.BLACK);
        this.toolStripContainer1.add(scrollPane, BorderLayout.CENTER);
        //
        // dgvList
        //
        this.dgvList.setName("dgvList");
        this.dgvList.setBackground(Color.BLACK);
        this.dgvList.setForeground(foreground);
        this.dgvList.setSelectionBackground(Color.DARK_GRAY);
        this.dgvList.setSelectionForeground(Color.WHITE);
        this.dgvList.setGridColor(new Color(32, 32, 32));
        this.dgvList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.dgvList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.dgvList.setFillsViewportHeight(true); // so that a drop below the last row lands on it
        this.dgvList.setDefaultRenderer(Object.class, renderer);
        this.dgvList.setDefaultRenderer(Integer.class, renderer);
        this.dgvList.getTableHeader().setReorderingAllowed(false); // a column's index is its cols ordinal
        this.dgvList.getTableHeader().setToolTipText(text("dgvList.Header.ToolTipText", "Click to sort by this column"));
        for (cols column : cols.values()) {
            baseWidths[column.ordinal()] = designWidth(column);
        }
        // the width a column is dragged to is kept, so that it survives hiding it and zooming
        this.dgvList.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                TableColumn resizing = dgvList.getTableHeader().getResizingColumn();
                if (resizing != null && resizing.getWidth() > 0) {
                    baseWidths[resizing.getModelIndex()] = Math.max(1, resizing.getWidth() / zoom);
                }
            }
            @Override public void columnAdded(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnMoved(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
        });
        this.dgvList.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1 || dgvList.getTableHeader().getResizingColumn() != null) return;
                // a click on the edge between two columns is the end of a resize, not a sort
                if (dgvList.getTableHeader().getCursor().getType() == Cursor.E_RESIZE_CURSOR) return;
                sortBy(dgvList.convertColumnIndexToModel(dgvList.columnAtPoint(e.getPoint())));
            }
        });
        this.dgvList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPlayListPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPlayListPopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int row = dgvList.rowAtPoint(e.getPoint());
                if (row >= 0 && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    playRow(row);
                }
            }
        });
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "play", () -> {
            if (dgvList.getSelectedRowCount() > 0) playRow(dgvList.getSelectedRow());
        });
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "remove", () -> tsmiDelThis_Click(null));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "remove", () -> tsmiDelThis_Click(null));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), "moveUp", () -> move(-1));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), "moveDown", () -> move(1));
        Droppable.makeComponentSinglePathDroppable(this.dgvList, this::dropped);
        //
        // cmsPlayList
        //
        this.cmsPlayList.setName("cmsPlayList");
        this.cmsPlayList.add(this.typeSettingsToolStripMenuItem);
        this.cmsPlayList.addSeparator();
        this.cmsPlayList.add(this.tsmiPlayThis);
        this.cmsPlayList.add(this.tsmiDelThis);
        this.cmsPlayList.addSeparator();
        this.cmsPlayList.add(this.tsmiDelAllMusic);
        this.cmsPlayList.add(this.tsmiOpenFolder);
        //
        // typeSettingsToolStripMenuItem
        //
        menuItem(this.typeSettingsToolStripMenuItem, "typeSettingsToolStripMenuItem", "Set type");
        for (char c = 'A'; c <= 'J'; c++) {
            JMenuItem item = new JMenuItem();
            menuItem(item, "tsmi" + c, String.valueOf(c));
            item.addActionListener(this::tsmiA_Click);
            this.typeSettingsToolStripMenuItem.add(item);
        }
        //
        // tsmiPlayThis
        //
        menuItem(this.tsmiPlayThis, "tsmiPlayThis", "Play this song");
        this.tsmiPlayThis.addActionListener(e -> {
            if (dgvList.getSelectedRowCount() > 0) playRow(dgvList.getSelectedRow());
        });
        //
        // tsmiDelThis
        //
        menuItem(this.tsmiDelThis, "tsmiDelThis", "Remove this song");
        this.tsmiDelThis.addActionListener(this::tsmiDelThis_Click);
        //
        // tsmiDelAllMusic
        //
        menuItem(this.tsmiDelAllMusic, "tsmiDelAllMusic", "Remove all songs");
        this.tsmiDelAllMusic.addActionListener(this::tsmiDelAllMusic_Click);
        //
        // tsmiOpenFolder
        //
        menuItem(this.tsmiOpenFolder, "tsmiOpenFolder", "Open the folder");
        this.tsmiOpenFolder.addActionListener(this::tsmiOpenFolder_Click);
        //
        // toolStrip1
        //
        this.toolStrip1.setName("toolStrip1");
        this.toolStrip1.setFloatable(false);
        this.toolStrip1.add(this.tsbOpenPlayList);
        this.toolStrip1.add(this.tsbSavePlayList);
        this.toolStrip1.addSeparator();
        this.toolStrip1.add(this.tsbAddMusic);
        this.toolStrip1.add(this.tsbAddFolder);
        this.toolStrip1.addSeparator();
        this.toolStrip1.add(this.tsbUp);
        this.toolStrip1.add(this.tsbDown);
        this.toolStrip1.addSeparator();
        this.toolStrip1.add(this.tsbAll);
        this.toolStrip1.add(this.tsbEnglish);
        this.toolStrip1.add(this.tsbJapanese);
        this.toolStrip1.addSeparator();
        this.toolStrip1.add(this.tsbTextExt);
        this.toolStrip1.add(this.tsbMMLExt);
        this.toolStrip1.add(this.tsbImgExt);

        button(this.tsbOpenPlayList, "tsbOpenPlayList", "openPL", "Open a playlist file");
        this.tsbOpenPlayList.addActionListener(this::tsbOpenPlayList_Click);
        button(this.tsbSavePlayList, "tsbSavePlayList", "savePL", "Save the playlist file");
        this.tsbSavePlayList.addActionListener(this::tsbSavePlayList_Click);
        button(this.tsbAddMusic, "tsbAddMusic", "addPL", "Add songs");
        this.tsbAddMusic.addActionListener(this::tsbAddMusic_Click);
        button(this.tsbAddFolder, "tsbAddFolder", "addFolderPL", "Add the songs in a folder");
        this.tsbAddFolder.addActionListener(this::tsbAddFolder_Click);
        button(this.tsbUp, "tsbUp", "upPL", "Move the selected songs up (Alt+Up)");
        this.tsbUp.addActionListener(this::tsbUp_Click);
        button(this.tsbDown, "tsbDown", "downPL", "Move the selected songs down (Alt+Down)");
        this.tsbDown.addActionListener(this::tsbDown_Click);
        button(this.tsbAll, "tsbAll", null, "Show both English and Japanese titles");
        this.tsbAll.addActionListener(e -> updateColumnVisibility());
        this.tsbAll.setSelected(true);
        button(this.tsbEnglish, "tsbEnglish", null, "Show English titles");
        this.tsbEnglish.addActionListener(e -> updateColumnVisibility());
        button(this.tsbJapanese, "tsbJapanese", "japPL", "Show Japanese titles");
        this.tsbJapanese.addActionListener(e -> updateColumnVisibility());
        button(this.tsbTextExt, "tsbTextExt", "txtPL", "Open the text that goes with the song");
        this.tsbTextExt.addActionListener(this::tsbTextExt_Click);
        this.tsbTextExt.setEnabled(false);
        button(this.tsbMMLExt, "tsbMMLExt", "mmlPL", "Open the MML that goes with the song");
        this.tsbMMLExt.addActionListener(this::tsbMMLExt_Click);
        this.tsbMMLExt.setEnabled(false);
        button(this.tsbImgExt, "tsbImgExt", "imgPL", "Open the image that goes with the song");
        this.tsbImgExt.addActionListener(this::tsbImgExt_Click);
        this.tsbImgExt.setEnabled(false);
        //
        // timer1
        //
        this.timer1.addActionListener(this::timer1_Tick);
        this.timer1.start();
        //
        // frmPlayList
        //
        // the container fills the window, so BorderLayout is what is wanted here
        this.getContentPane().add(this.toolStripContainer1, BorderLayout.CENTER);
        this.setName("frmPlayList");
        this.setTitle(text("$this.Text", "play list"));
        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
        this.setSize(585, 270);
        this.setPreferredSize(new Dimension(585, 270));
        this.setMinimumSize(new Dimension(400, 120));
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                storeBounds();
                isClosed = true;
            }
        });
        // FormMain#checkAndSetForm packs the window when it shows it: keep what the user sized it to
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setPreferredSize(getSize());
            }
        });

        baseFont = dgvList.getFont();
        baseRowHeight = dgvList.getRowHeight();
        setZoom(setting.getOther().getZoom());
        updateHeaders();
    }

    private void bindKey(KeyStroke key, String name, Runnable action) {
        dgvList.getInputMap(JComponent.WHEN_FOCUSED).put(key, name);
        dgvList.getActionMap().put(name, new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void showPlayListPopup(MouseEvent e) {
        if (!e.isPopupTrigger()) return;

        int row = dgvList.rowAtPoint(e.getPoint());
        boolean onRow = row >= 0;
        if (onRow && !dgvList.isRowSelected(row)) {
            dgvList.setRowSelectionInterval(row, row);
        }

        typeSettingsToolStripMenuItem.setEnabled(onRow);
        tsmiPlayThis.setEnabled(onRow);
        tsmiDelThis.setEnabled(onRow);
        tsmiOpenFolder.setEnabled(onRow);
        tsmiDelAllMusic.setEnabled(!playList.getMusics().isEmpty());
        tsmiDelThis.setText(dgvList.getSelectedRowCount() > 1
                ? text("tsmiDelThis.Text.plural", "Remove the selected songs")
                : text("tsmiDelThis.Text", "Remove this song"));
        cmsPlayList.show(dgvList, e.getX(), e.getY());
    }

    private Font baseFont;
    private int baseRowHeight;

    private JTable dgvList;
    private JPopupMenu cmsPlayList;
    private JMenuItem tsmiPlayThis;
    private JMenuItem tsmiDelThis;
    private JPanel toolStripContainer1;
    private JToolBar toolStrip1;
    private JButton tsbOpenPlayList;
    private JButton tsbSavePlayList;
    private JButton tsbAddMusic;
    private JButton tsbUp;
    private JButton tsbDown;
    private JMenuItem tsmiDelAllMusic;
    private JButton tsbAddFolder;
    private JToggleButton tsbJapanese;
    private JToggleButton tsbEnglish;
    private JToggleButton tsbAll;
    private JMenu typeSettingsToolStripMenuItem;
    private JButton tsbTextExt;
    private JButton tsbMMLExt;
    private JButton tsbImgExt;
    private Timer timer1;
    private JMenuItem tsmiOpenFolder;
}
