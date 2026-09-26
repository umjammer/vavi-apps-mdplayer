/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.visualizer.fmdsp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * The {@code PGM NUMBER} counter, which shows the song number of a file holding several songs.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 */
class PgmNumberTest {

    /** the counter row, one below {@code VOLUME DOWN}, its three digits in the same places */
    private static final int ROW_Y = 22 + 19 * 5;
    private static final int NUM_X = 568 + 8 * 5;

    /** palette 0, fully faded in */
    private static final int[] RGB = {
            new Color(0, 0, 0).getRGB(), new Color(170, 170, 153).getRGB(),
            new Color(102, 136, 255).getRGB(), new Color(68, 68, 119).getRGB(),
    };

    /** a source whose song number the test dictates */
    private static class Work implements WorkStateSource {
        int songNo;
        @Override public long generatedFrames() { return 0; }
        @Override public long timerBCount() { return 0; }
        @Override public int timerB() { return 200; }
        @Override public int loopCount() { return 0; }
        @Override public long loopTimerBCount() { return 0; }
        @Override public long timerBCountLoop() { return 0; }
        @Override public boolean playing() { return true; }
        @Override public boolean paused() { return false; }
        @Override public int songNo() { return songNo; }
    }

    private static BufferedImage render(Work work) {
        FmDspVisualizer vis = new FmDspVisualizer(60);
        vis.setSize(640, 400);
        if (work != null) {
            vis.setDataSource(new FmDspDataSource() {
                @Override public FftDataSource fft() { return null; }
                @Override public LevelDataSource level() { return null; }
                @Override public TrackStatusSource trackStatus() { return null; }
                @Override public WorkStateSource work() { return work; }
            });
        }
        BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        // the palette fades in: paint until it is up
        for (int i = 0; i < 120; i++) {
            vis.paint(g);
        }
        g.dispose();
        return image;
    }

    /** the glyph an 8x11 cell holds, compared by colour; 10 is the blank glyph, -1 no match */
    private static int digitAt(BufferedImage image, int x, int y) {
        glyph:
        for (int g = 0; g < 11; g++) {
            for (int dy = 0; dy < 11; dy++) {
                for (int dx = 0; dx < 8; dx++) {
                    if (image.getRGB(x + dx, y + dy) != RGB[FmDspSprites.s_num[g * 88 + dy * 8 + dx] & 0xff]) {
                        continue glyph;
                    }
                }
            }
            return g;
        }
        return -1;
    }

    /** the three places as digits, 10 for a blank one */
    private static int[] cells(BufferedImage image) {
        int[] cells = new int[3];
        for (int i = 0; i < 3; i++) {
            cells[i] = digitAt(image, NUM_X + 8 * i, ROW_Y);
        }
        return cells;
    }

    @Test
    @DisplayName("one digit song number: only the last place is lit")
    void oneDigit() {
        Work work = new Work();
        work.songNo = 7;
        assertEquals("[10, 10, 7]", Arrays.toString(cells(render(work))));
    }

    @Test
    @DisplayName("two digits, no leading zero")
    void twoDigits() {
        Work work = new Work();
        work.songNo = 12;
        assertEquals("[10, 1, 2]", Arrays.toString(cells(render(work))));
    }

    @Test
    @DisplayName("three digits fill the field, a zero inside the number is lit")
    void threeDigits() {
        Work work = new Work();
        work.songNo = 105;
        assertEquals("[1, 0, 5]", Arrays.toString(cells(render(work))));
    }

    @Test
    @DisplayName("a file without song numbers leaves the counter blank")
    void noSongNo() {
        assertEquals("[10, 10, 10]", Arrays.toString(cells(render(new Work()))));
        assertEquals("[10, 10, 10]", Arrays.toString(cells(render(null))));
    }
}
