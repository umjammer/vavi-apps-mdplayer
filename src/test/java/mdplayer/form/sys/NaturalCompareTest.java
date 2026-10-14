package mdplayer.form.sys;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/** The play list's sort order for names with numbers in them. */
class NaturalCompareTest {

    static List<String> sorted(String... s) {
        List<String> l = new ArrayList<>(List.of(s));
        l.sort(FormPlayList::naturalCompare);
        return l;
    }

    @Test
    void trackNumbers() {
        assertEquals(List.of("Nightshade - Trk 1", "Nightshade - Trk 2", "Nightshade - Trk 3", "Nightshade - Trk 10", "Nightshade - Trk 11", "Nightshade - Trk 15"),
                sorted("Nightshade - Trk 1", "Nightshade - Trk 10", "Nightshade - Trk 11", "Nightshade - Trk 15", "Nightshade - Trk 2", "Nightshade - Trk 3"));
    }

    @Test
    void numberBeforeExtension() {
        assertEquals(List.of("1.m", "2.m", "9.m", "10.m", "12.m", "19.m"),
                sorted("1.m", "10.m", "12.m", "19.m", "2.m", "9.m"));
    }

    @Test
    void folders() {
        // the play list sorts file names as full paths with '\0' for separators
        assertEquals(List.of("a\0" + "2\0x", "a\0" + "10\0x", "a-b\0x"),
                sorted("a-b\0x", "a\0" + "10\0x", "a\0" + "2\0x"));
    }

    @Test
    void others() {
        assertTrue(FormPlayList.naturalCompare("abc", "ABD") < 0, "case is ignored");
        assertEquals(0, FormPlayList.naturalCompare("Abc", "aBC"));
        assertTrue(FormPlayList.naturalCompare("a", "a1") < 0);
        assertTrue(FormPlayList.naturalCompare("a1", "a01") < 0, "equal numbers, fewer zeros first");
        assertTrue(FormPlayList.naturalCompare("a01b", "a1c") < 0, "zeros only break a tie");
        assertTrue(FormPlayList.naturalCompare("a99999999999999999999", "a100000000000000000000") < 0, "beyond long");
        assertEquals(List.of("x0", "x00", "x1"), sorted("x1", "x00", "x0"));
    }
}
