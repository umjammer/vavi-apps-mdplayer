/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.sampled;

import java.nio.file.Files;
import java.nio.file.Path;

import mdplayer.PlayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * An m3u's songs are named relative to it and read by their own formats.
 */
class M3UPlayListTest {

    @Test
    void relativeVgm(@TempDir Path dir) throws Exception {
        Files.copy(Path.of("src/test/resources/test.vgm"), dir.resolve("test.vgm"));
        Path m3u = dir.resolve("list.m3u");
        Files.writeString(m3u, "# comment\ntest.vgm\n");

        PlayList pl = new PlayList();
        pl.insertFile(new int[] {0}, new String[] {m3u.toString()});

        assertEquals(1, pl.getMusics().size());
        PlayList.Music m = pl.getMusics().getFirst();
        assertEquals(dir.resolve("test.vgm").toString(), m.fileName);
        assertEquals("Tim 7: Ronda Alla Turca", m.title);
    }

    static final Path HES = Path.of("tmp/hes/OutRun/NAPH-1016.m3u");

    static boolean hesExists() {
        return Files.exists(HES);
    }

    @Test
    @EnabledIf("hesExists")
    void hesSongs() throws Exception {
        long lines = Files.readAllLines(HES).stream().filter(l -> !l.isBlank() && !l.startsWith("#")).count();

        PlayList pl = new PlayList();
        pl.insertFile(new int[] {0}, new String[] {HES.toString()});

        assertEquals(lines, pl.getMusics().size());
        pl.getMusics().forEach(m -> assertTrue(m.songNo >= 0, m.toString()));
    }
}
