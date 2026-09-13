package mdplayer.lib.mndrv;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import mdplayer.lib.mndrv.MndV1Analyzer.FileResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Evidence that the MND version 1 opcode map in {@link ComAnalyze#V1_CMD_MAP} is right.
 * <p>
 * mndrv.x 1.37 turns version 1 data away rather than carry a second dispatch table, so there is
 * no reference renderer to diff against and the map had to be recovered from the data. The
 * argument runs:
 * <ol>
 * <li>{@link MndV1Analyzer} runs a song through the real interpreter and watches each track's
 *     data pointer. A parse that has lost step walks out of the sequence data, which is
 *     detectable without knowing what the music should sound like.</li>
 * <li>The analyzer is calibrated on version 2 and 3 data, which mndrv does play. Those come back
 *     clean, so the check does not simply pass everything.</li>
 * <li>With the map, all 62 version 1 files come back clean. Without it they all fail -- most by
 *     escaping the sequence data outright, several by faulting inside a handler fed garbage
 *     operands.</li>
 * <li>The one entry in the map, {@code $ED -> $C0}, is corroborated by opcode census: {@code $ED}
 *     occurs thousands of times in version 1 data and never in version 2 or 3, while {@code $C0}
 *     is the reverse. Both name a six-operand software envelope. The opcode was renumbered.</li>
 * </ol>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 */
@EnabledIf("samplesExist")
class MndV1AnalyzerTest {

    static final Path V1 = Path.of("tmp/mnd/MND_SXP1");
    static final Path V2 = Path.of("tmp/mnd/MND_BRA1");
    static final Path V3 = Path.of("tmp/mnd/MND9827/eve98@27.mnd");

    /** long enough for the slowest song here to reach its end command */
    static final int FRAMES = 30_000_000;

    static boolean samplesExist() {
        return Files.isDirectory(V1) && Files.isDirectory(V2) && Files.exists(V3);
    }

    static List<Path> mndFiles(Path dir) throws Exception {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().toLowerCase().endsWith(".mnd")).sorted().toList();
        }
    }

    static void assertAllClean(List<Path> files) throws Exception {
        StringBuilder dirty = new StringBuilder();
        for (Path f : files) {
            FileResult r = MndV1Analyzer.analyze(f, FRAMES);
            if (r.clean()) continue;
            dirty.append('\n').append(r.summary());
            if (r.failure() != null) dirty.append("\n    ").append(r.failure());
            r.tracks().stream().filter(t -> !t.clean())
                    .forEach(t -> dirty.append("\n    ").append(t));
        }
        assertTrue(dirty.isEmpty(), "tracks did not parse cleanly:" + dirty);
    }

    @Test
    @DisplayName("v2 data parses cleanly, so the check is calibrated")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void v2ParsesCleanly() throws Exception {
        List<Path> files = mndFiles(V2);
        assertEquals(24, files.size());
        assertAllClean(files);
    }

    @Test
    @DisplayName("v3 data parses cleanly, so the check is calibrated")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void v3ParsesCleanly() throws Exception {
        assertAllClean(List.of(V3));
    }

    @Test
    @DisplayName("v1 data parses cleanly with the v1 opcode map")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void v1ParsesCleanly() throws Exception {
        List<Path> files = mndFiles(V1);
        assertEquals(62, files.size());
        assertAllClean(files);
    }

    @Test
    @DisplayName("$ED belongs to v1 and $C0 to v2+, which is why one maps to the other")
    void opcodeCensusSupportsTheMapping() throws Exception {
        int v1Ed = 0, v1C0 = 0, laterEd = 0, laterC0 = 0;
        for (Path f : mndFiles(V1)) {
            v1Ed += count(f, 0xed);
            v1C0 += count(f, 0xc0);
        }
        for (Path f : Stream.concat(mndFiles(V2).stream(), Stream.of(V3)).toList()) {
            laterEd += count(f, 0xed);
            laterC0 += count(f, 0xc0);
        }
        // exact, not merely lopsided: v2 retired $ED and v1 had not yet minted $C0. The handful
        // of $c0 bytes in v1 data are operands (a tempo of 192, say), never opcodes.
        assertEquals(0, laterEd, "v2+ data should never use $ED");
        assertTrue(v1Ed > 1000, "v1 data leans on $ED heavily, saw " + v1Ed);
        assertTrue(laterC0 > 1000, "v2+ data leans on $C0 heavily, saw " + laterC0);
        assertTrue(v1C0 * 100 < v1Ed, "v1 $C0 should be noise next to $ED, saw " + v1C0);
    }

    /** occurrences of one byte value in a song's sequence data */
    static int count(Path path, int value) throws Exception {
        byte[] d = Files.readAllBytes(path);
        int seq = ((d[0x10] & 0xff) << 24) | ((d[0x11] & 0xff) << 16)
                | ((d[0x12] & 0xff) << 8) | (d[0x13] & 0xff);
        int n = 0;
        for (int i = seq; i < d.length; i++) {
            if ((d[i] & 0xff) == value) n++;
        }
        return n;
    }
}
