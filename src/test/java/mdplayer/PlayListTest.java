/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


class PlayListTest {

    /** an MFi's "prot" chunk comes out as binary notes; the XML writer refused it and nothing was saved */
    @Test
    void saveBinaryTag(@TempDir Path dir) throws Exception {
        PlayList pl = new PlayList();
        PlayList.Music a = new PlayList.Music();
        a.fileName = "a.mld";
        a.notes = "PMLA\0\02\0\u0001garbage";
        a.title = "t\u0001i\u0008t\tle";
        PlayList.Music b = new PlayList.Music();
        b.fileName = "b.mld";
        pl.getMusics().add(a);
        pl.getMusics().add(b);
        pl.setLastPlayed(1);

        Path file = dir.resolve("list.xml");
        pl.save(file.toString());

        PlayList back = PlayList.load(file.toString());
        assertEquals(2, back.getMusics().size(), "the songs after the one with binary tags are saved too");
        assertEquals("PMLA", back.getMusics().getFirst().notes);
        assertEquals("tit\tle", back.getMusics().getFirst().title);
        assertEquals(1, back.getLastPlayed());
        assertFalse(Files.exists(dir.resolve("list.xml.tmp")));
    }
}
