/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.sampled;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import mdplayer.Common;
import mdplayer.M3U;
import mdplayer.PlayList;
import mdplayer.driver.archive.ZIPFileFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import vavi.util.archive.Archives;

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

    @Test
    void nezplugFieldsInMs932(@TempDir Path dir) throws Exception {
        Path m3u = dir.resolve("list.m3u");
        Files.write(m3u, "foo.nsf::NSF,2,\u30c6\u30b9\u30c8,1:00,,5\nbar.hes::HES,$1A,Title\\, with comma\n".getBytes(Common.charset));

        PlayList pl = M3U.loadM3U(m3u.toString(), dir.toString());

        assertEquals(2, pl.getMusics().size());
        PlayList.Music nsf = pl.getMusics().get(0);
        assertEquals(dir.resolve("foo.nsf").toString(), nsf.fileName);
        assertEquals(1, nsf.songNo); // nsf counts from 1
        assertEquals("\u30c6\u30b9\u30c8", nsf.title);
        assertEquals("1:00", nsf.time);
        assertEquals("5", nsf.fadeoutTime);
        PlayList.Music hes = pl.getMusics().get(1);
        assertEquals(0x1a, hes.songNo);
        assertEquals("Title, with comma", hes.title);
    }

    @Test
    void extinfTitle(@TempDir Path dir) throws Exception {
        Path m3u = dir.resolve("list.m3u8");
        Files.writeString(m3u, "#EXTM3U\n#EXTINF:180,Artist - Title\nsong.vgm\n");

        PlayList pl = M3U.loadM3U(m3u.toString(), dir.toString());

        assertEquals(1, pl.getMusics().size());
        assertEquals(dir.resolve("song.vgm").toString(), pl.getMusics().getFirst().fileName);
        assertEquals("Artist - Title", pl.getMusics().getFirst().title);
    }

    @Test
    void inZip(@TempDir Path dir) throws Exception {
        Path zip = dir.resolve("songs.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("list.m3u"));
            zos.write("#EXTINF:-1,Ronda\ntest.vgm\n".getBytes(Common.charset));
            zos.putNextEntry(new ZipEntry("test.vgm"));
            zos.write(Files.readAllBytes(Path.of("src/test/resources/test.vgm")));
        }

        PlayList.Music mc = new PlayList.Music();
        mc.fileName = zip.toString();
        List<PlayList.Music> musics = new ZIPFileFormat().addFileLoop(mc, Archives.getArchive(zip.toFile()), null);

        assertEquals(1, musics.size());
        PlayList.Music m = musics.getFirst();
        assertEquals("test.vgm", m.fileName);
        assertEquals("Tim 7: Ronda Alla Turca", m.title);
        assertEquals(zip.toString(), m.arcFileName);
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
