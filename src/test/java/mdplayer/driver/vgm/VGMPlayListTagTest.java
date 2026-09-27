/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.vgm;

import java.nio.file.Files;
import java.nio.file.Path;

import mdplayer.PlayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * A vgm added to the play list shows its GD3 tags there, as the fmdsp does.
 */
class VGMPlayListTagTest {

    static final Path VGM = Path.of("src/test/resources/test.vgm");
    static final Path VGZ = Path.of("tmp/vgm/SWJ-SQRC01_1C_trimmed_optimized.vgz");

    static boolean vgzExists() {
        return Files.exists(VGZ);
    }

    static void assertTagged(Path path) {
        PlayList pl = new PlayList();
        pl.addFile(path.toString());
        assertEquals(1, pl.getMusics().size());
        PlayList.Music m = pl.getMusics().getFirst();
        assertNotNull(m.title);
        assertFalse(m.title.isEmpty() && (m.titleJ == null || m.titleJ.isEmpty()));
    }

    @Test
    void vgm() {
        assertTagged(VGM);
    }

    @Test
    @EnabledIf("vgzExists")
    void vgz() {
        assertTagged(VGZ);
    }
}
