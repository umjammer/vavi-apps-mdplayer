package mdplayer.form.sys;

import java.awt.Font;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import mdplayer.PlayList;
import mdplayer.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Drives the play list window without the rest of the player: the main window is a mock that
 * "plays" whatever it is asked to.
 */
@EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
class FormPlayListTest {

    FormPlayList form;
    JTable table;
    PlayList playList;

    @BeforeEach
    void setUp() throws Exception {
        FormMain main = mock(FormMain.class);
        main.setting = Setting.getInstance();
        when(main.loadAndPlay(anyInt(), anyInt(), any(), any())).thenReturn(true);
        SwingUtilities.invokeAndWait(() -> {
            form = new FormPlayList(main);
            playList = form.getPlayList();
            playList.getMusics().clear(); // not the user's default list
            form.refresh();
        });
        Field f = FormPlayList.class.getDeclaredField("dgvList");
        f.setAccessible(true);
        table = (JTable) f.get(form);
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> form.dispose());
    }

    /** Puts songs on the list the way a file format would, without reading any file. */
    void add(String... titles) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (String t : titles) {
                PlayList.Music m = new PlayList.Music();
                m.fileName = "/tmp/" + t + ".vgm";
                m.title = t;
                playList.getMusics().add(m);
            }
            form.refresh();
        });
    }

    List<String> titles() {
        return playList.getMusics().stream().map(m -> m.title).toList();
    }

    Object call(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = FormPlayList.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        Object[] r = new Object[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                r[0] = m.invoke(form, args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return r[0];
    }

    void select(int... rows) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            table.clearSelection();
            for (int r : rows) table.addRowSelectionInterval(r, r);
        });
    }

    @Test
    void addFile() throws Exception {
        String vgm = Path.of("src/test/resources/test.vgm").toAbsolutePath().toString();
        SwingUtilities.invokeAndWait(() -> playList.addFile(vgm));
        assertEquals(1, playList.getMusics().size());
        assertEquals(1, table.getRowCount());
        assertEquals("test.vgm", table.getValueAt(0, FormPlayList.cols.clmDispFileName.ordinal()));
    }

    @Test
    void drop() throws Exception {
        add("a", "b");
        Path dir = Path.of("src/test/resources").toAbsolutePath();
        boolean r = (boolean) call("dropped", new Class<?>[] {Path.class}, dir);
        assertTrue(r);
        assertEquals(3, table.getRowCount(), "only test.vgm in the folder is a song");
        assertEquals("test.vgm", table.getValueAt(2, FormPlayList.cols.clmDispFileName.ordinal()));
        assertEquals(2, table.getSelectedRow(), "what was dropped is selected");
    }

    @Test
    void playNextPrev() throws Exception {
        add("a", "b", "c");
        SwingUtilities.invokeAndWait(() -> { form.setStart(-2); form.play(); });
        assertEquals(">", table.getValueAt(0, FormPlayList.cols.clmPlayingNow.ordinal()));
        SwingUtilities.invokeAndWait(() -> form.nextPlayMode(0));
        assertEquals("b", form.getPlayingSongInfo().title);
        SwingUtilities.invokeAndWait(() -> form.nextPlayMode(0));
        SwingUtilities.invokeAndWait(() -> form.nextPlayMode(0));
        assertFalse(form.isPlaying(), "normal mode stops after the last one");
        SwingUtilities.invokeAndWait(() -> { form.play(); form.nextPlayMode(2); });
        assertEquals("a", form.getPlayingSongInfo().title, "all loop goes round");
        SwingUtilities.invokeAndWait(() -> form.prevPlay(2));
        assertEquals("c", form.getPlayingSongInfo().title);
        SwingUtilities.invokeAndWait(() -> form.prevPlay(0));
        assertEquals("b", form.getPlayingSongInfo().title);
        SwingUtilities.invokeAndWait(() -> form.nextPlayMode(3));
        assertEquals("b", form.getPlayingSongInfo().title, "one song loop");
        for (int i = 0; i < 5; i++) SwingUtilities.invokeAndWait(() -> form.nextPlayMode(1));
        String last = form.getPlayingSongInfo().title;
        assertNotNull(last);
        SwingUtilities.invokeAndWait(() -> form.prevPlay(1));
        assertTrue(form.isPlaying());
    }

    @Test
    void moveKeepsPlaying() throws Exception {
        add("a", "b", "c", "d");
        SwingUtilities.invokeAndWait(() -> { form.setStart(1); form.play(); }); // b
        select(1, 3);
        call("move", new Class<?>[] {int.class}, -1);
        assertEquals(List.of("b", "a", "d", "c"), titles());
        assertEquals(2, table.getSelectedRowCount());
        assertTrue(table.isRowSelected(0) && table.isRowSelected(2));
        call("move", new Class<?>[] {int.class}, -1);
        assertEquals(List.of("b", "d", "a", "c"), titles(), "the one at the top stays");
        assertEquals(">", table.getValueAt(0, FormPlayList.cols.clmPlayingNow.ordinal()));
        select(3);
        call("move", new Class<?>[] {int.class}, 1);
        assertEquals(List.of("b", "d", "a", "c"), titles(), "the one at the bottom stays");
        SwingUtilities.invokeAndWait(() -> form.nextPlayMode(0));
        assertEquals("d", form.getPlayingSongInfo().title, "next follows the list as moved");
    }

    @Test
    void sort() throws Exception {
        add("c", "a", "b");
        int col = FormPlayList.cols.clmTitle.ordinal();
        call("sortBy", new Class<?>[] {int.class}, col);
        assertEquals(List.of("a", "b", "c"), titles());
        assertTrue(table.getColumnModel().getColumn(col).getHeaderValue().toString().endsWith("▲"));
        call("sortBy", new Class<?>[] {int.class}, col);
        assertEquals(List.of("c", "b", "a"), titles());
    }

    @Test
    void remove() throws Exception {
        add("a", "b", "c", "d");
        SwingUtilities.invokeAndWait(() -> { form.setStart(2); form.play(); }); // c
        select(0, 2);
        call("tsmiDelThis_Click", new Class<?>[] {java.awt.event.ActionEvent.class}, (Object) null);
        assertEquals(List.of("b", "d"), titles());
        assertEquals(2, table.getRowCount());
        SwingUtilities.invokeAndWait(() -> form.nextPlayMode(0));
        assertEquals("d", form.getPlayingSongInfo().title, "the song that took the removed one's place is next");
    }

    @Test
    void setType() throws Exception {
        add("a", "b");
        select(1);
        javax.swing.JMenuItem item = new javax.swing.JMenuItem("C");
        call("tsmiA_Click", new Class<?>[] {java.awt.event.ActionEvent.class}, new java.awt.event.ActionEvent(item, 0, ""));
        assertEquals("C", playList.getMusics().get(1).type);
        assertEquals("-", playList.getMusics().get(0).type);
    }

    /** a song dropped under the one playing is the next one, and so is the song after it */
    @Test
    void dropUnderPlayingIsNext() throws Exception {
        add("a", "b");
        SwingUtilities.invokeAndWait(() -> { form.setStart(0); form.play(); }); // a
        Path vgm = Path.of("src/test/resources/test.vgm").toAbsolutePath();
        call("addFiles", new Class<?>[] {List.class, int.class}, List.of(vgm.toString()), 1);
        assertEquals(List.of("a.vgm", "test.vgm", "b.vgm"),
                playList.getMusics().stream().map(m -> Path.of(m.fileName).getFileName().toString()).toList());
        SwingUtilities.invokeAndWait(() -> form.nextPlayMode(0));
        assertEquals("test.vgm", Path.of(form.getPlayingSongInfo().fileName).getFileName().toString());
        SwingUtilities.invokeAndWait(() -> form.nextPlayMode(0));
        assertEquals("b", form.getPlayingSongInfo().title);
    }

    /** "next" after a stop goes on from the song played last, not from the top */
    @Test
    void nextAfterStop() throws Exception {
        add("a", "b", "c");
        SwingUtilities.invokeAndWait(() -> { form.setStart(1); form.play(); form.stop(); }); // b, stopped
        SwingUtilities.invokeAndWait(() -> form.nextPlayMode(0));
        assertEquals("c", form.getPlayingSongInfo().title);
        assertTrue(form.isPlaying());
    }

    @Test
    void emptyList() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertNull(form.setStart(-1));
            form.play();
            form.nextPlayMode(1);
            form.nextPlayMode(3);
            form.prevPlay(1);
        });
        assertFalse(form.isPlaying());
    }

    @Test
    void zoom() throws Exception {
        Field f = FormPlayList.class.getDeclaredField("tsbAddMusic");
        f.setAccessible(true);
        AbstractButton b = (AbstractButton) f.get(form);
        SwingUtilities.invokeAndWait(() -> form.setZoom(1));
        Font f1 = table.getFont();
        int h1 = table.getRowHeight();
        int i1 = b.getIcon().getIconWidth();
        int w1 = form.getWidth();
        SwingUtilities.invokeAndWait(() -> form.setZoom(2));
        assertEquals(f1.getSize2D() * 2, table.getFont().getSize2D());
        assertEquals(h1 * 2, table.getRowHeight());
        assertEquals(i1 * 2, b.getIcon().getIconWidth());
        assertEquals(w1 * 2, form.getWidth());
        assertNotNull(b.getToolTipText());
        SwingUtilities.invokeAndWait(() -> form.setZoom(1));
        assertEquals(w1, form.getWidth());
    }

    @Test
    void tooltips() throws Exception {
        for (String name : new String[] {"tsbOpenPlayList", "tsbSavePlayList", "tsbAddMusic", "tsbAddFolder", "tsbUp", "tsbDown",
                "tsbAll", "tsbEnglish", "tsbJapanese", "tsbTextExt", "tsbMMLExt", "tsbImgExt"}) {
            Field f = FormPlayList.class.getDeclaredField(name);
            f.setAccessible(true);
            String tip = ((AbstractButton) f.get(form)).getToolTipText();
            assertNotNull(tip, name);
            assertFalse(tip.isBlank(), name);
        }
    }

    @Test
    void playingSongInfo() throws Exception {
        add("a");
        SwingUtilities.invokeAndWait(() -> form.setStart(0));
        PlayList.Music m = form.getPlayingSongInfo();
        assertSame(playList.getMusics().getFirst(), m);
        assertNotNull(m.format, "the format is filled in for a list read from a file");
    }
}
