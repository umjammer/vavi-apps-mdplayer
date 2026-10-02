/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.visualizer.fmdsp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * The frequency fluctuation display ("9" of FMDSP.DOC), which shares the note bar's row with the
 * bar itself and lies over the "M:" LFO field of the line below.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 */
class FreqWaveTest {

    /** the field, from {@code FREQ_X} out to the end of the LFO field, on the first row's bar line */
    private static final int X = 257;
    private static final int SLOTS = 8;
    private static final int FIELD = 20;
    private static final int MID_Y = 3;

    /** palette 0's bar colours: 2 lights the wave, 3 the field it rests on */
    private static final int ON = new Color(102, 136, 255).getRGB();
    private static final int OFF = new Color(68, 68, 119).getRGB();

    /** a source with one FM row, whose fluctuation the test dictates */
    private static FmDspVisualizer visualizer(boolean playing, int pitchDeviation, String status) {
        FmDspVisualizer vis = new FmDspVisualizer(60);
        vis.setSize(640, 400);
        vis.setDataSource(new FmDspDataSource() {
            @Override public FftDataSource fft() { return null; }
            @Override public LevelDataSource level() { return null; }
            @Override public WorkStateSource work() { return null; }
            @Override public TrackStatusSource trackStatus() {
                return (track, out) -> {
                    out.playing = track == TrackId.FM_1 && playing;
                    out.info = TrackInfo.NORMAL;
                    out.ticks = 64;
                    out.ticksLeft = 32;
                    out.key = out.playing ? 0x40 : 0xff;
                    out.actualKey = out.key;
                    out.toneNum = 0;
                    out.volume = 100;
                    out.gate = 8;
                    out.detune = 0;
                    out.pitchDeviation = out.playing ? pitchDeviation : 0;
                    out.status = status;
                    out.ppz8Ch = 0;
                    out.ssgTone = false;
                    out.ssgNoise = false;
                    out.ssgNoiseFreq = 0;
                };
            }
        });
        return vis;
    }

    /** paints {@code frames} times - the palette fades in over the first hundred or so */
    private static BufferedImage render(FmDspVisualizer vis, int frames) {
        BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        for (int i = 0; i < frames; i++) {
            vis.paint(g);
        }
        g.dispose();
        return image;
    }

    /** rows of the band each slot has lit, top to bottom, {@code -1} where a slot is dark */
    private static int[][] slots(BufferedImage image, int rgb) {
        int[][] lit = new int[SLOTS][];
        for (int i = 0; i < SLOTS; i++) {
            int[] rows = new int[6];
            int n = 0;
            for (int y = 0; y < 6; y++) {
                if (image.getRGB(X + 2 * i, 1 + y) == rgb) rows[n++] = 1 + y;
            }
            lit[i] = java.util.Arrays.copyOf(rows, n);
        }
        return lit;
    }

    @Test
    @DisplayName("a steady note lies flat: one lit pixel a slot, on the centre line")
    void steadyNoteIsFlat() {
        BufferedImage image = render(visualizer(true, 0, "--------"), 120);
        for (int[] rows : slots(image, ON)) {
            assertEquals(1, rows.length, "a steady slot is a single pixel");
            assertEquals(MID_Y, rows[0], "on the centre line");
        }
    }

    @Test
    @DisplayName("a note being bent swings off the centre line")
    void bentNoteWaves() {
        BufferedImage image = render(visualizer(true, 50, "--------"), 120);
        boolean above = false, below = false;
        for (int[] rows : slots(image, ON)) {
            for (int y : rows) {
                if (y < MID_Y) above = true;
                if (y > MID_Y) below = true;
            }
        }
        assertTrue(above, "the wave reaches over the centre line");
        assertTrue(below, "and under it");
    }

    @Test
    @DisplayName("the wave flows: the same note is drawn differently one frame later")
    void waveFlows() {
        FmDspVisualizer vis = visualizer(true, 50, "--------");
        BufferedImage first = render(vis, 120);
        BufferedImage second = render(vis, 1);
        boolean moved = false;
        for (int i = 0; i < SLOTS && !moved; i++) {
            for (int y = 1; y <= 6; y++) {
                if (first.getRGB(X + 2 * i, y) != second.getRGB(X + 2 * i, y)) {
                    moved = true;
                    break;
                }
            }
        }
        assertTrue(moved, "the wave should have travelled between the two frames");
    }

    @Test
    @DisplayName("a driver that only flags its LFO still waves")
    void lfoFlagAloneWaves() {
        // "P" heads both the FMP and the PMD field, and no deviation is measured
        BufferedImage image = render(visualizer(true, 0, "P-------"), 120);
        boolean off = false;
        for (int[] rows : slots(image, ON)) {
            for (int y : rows) {
                if (y != MID_Y) off = true;
            }
        }
        assertTrue(off, "an LFO flag alone is enough to move the display");
    }

    @Test
    @DisplayName("a silent row shows the field's dim strokes, with no wave over them")
    void silentRowKeepsTheField() {
        BufferedImage image = render(visualizer(false, 0, "--------"), 120);
        for (int i = 0; i < FIELD; i++) {
            for (int y = 1; y <= 4; y++) {
                assertEquals(OFF, image.getRGB(X + 2 * i, y), "field slot " + i + " row " + y);
            }
        }
        boolean lit = false;
        for (int[] rows : slots(image, ON)) {
            lit |= rows.length != 0;
        }
        assertFalse(lit, "nothing is playing, so nothing waves");
    }

    /**
     * Prints the field frame by frame, the way the FMDSP screen recording it was drawn from reads:
     * {@code *} the wave, {@code .} the field it rests on. One frame proves nothing here - what to
     * look at is the wave travelling across the frames. Run with {@code -Dvavi.test=diag}.
     */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void diagPrintWave() {
        FmDspVisualizer vis = visualizer(true, 50, "--------");
        BufferedImage image = render(vis, 120);
        for (int f = 0; f < 24; f++) {
            for (int y = 1; y <= 6; y++) {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < FIELD; i++) {
                    int rgb = image.getRGB(X + 2 * i, y);
                    b.append(rgb == ON ? '*' : rgb == OFF ? '.' : ' ');
                }
                System.out.printf("%2d %d %s%n", f, y, b);
            }
            image = render(vis, 1);
        }
    }

    @Test
    @DisplayName("the field's own strokes stay where the wave does not reach")
    void fieldSurvivesBesideTheWave() {
        BufferedImage image = render(visualizer(true, 50, "--------"), 120);
        for (int i = SLOTS; i < FIELD; i++) {
            for (int y = 1; y <= 4; y++) {
                assertEquals(OFF, image.getRGB(X + 2 * i, y), "field slot " + i + " row " + y);
            }
        }
    }
}
