/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.visualizer.fmdsp;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import vavi.util.event.GenericEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * A click on a level meter's pan circle is handed out as {@link FmDspVisualizer#EVENT_MUTE}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-12 nsano initial version <br>
 */
class PanpotClickTest {

    /** the meters' left edge, their pitch and the pan circles' top, as fmdsp_sprites.h has them */
    static final int LEVEL_X = 353 - 16, LEVEL_W = 16, PANPOT_Y = 227 + 64;

    static final int ZOOM = 2;

    FmDspVisualizer vis;
    final List<GenericEvent> events = new ArrayList<>();

    @BeforeEach
    void setup() {
        vis = new FmDspVisualizer(60, ZOOM);
        vis.setSize(FmDspVisualizer.CANVAS_W * ZOOM, FmDspVisualizer.CANVAS_H * ZOOM);
        vis.addGenericListener(events::add);
    }

    void click(int x, int y) {
        vis.dispatchEvent(new MouseEvent(vis, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                x * ZOOM, y * ZOOM, 1, false, MouseEvent.BUTTON1));
    }

    @Test
    @DisplayName("the pan circle under a meter names that meter's column")
    void testColumn() {
        click(LEVEL_X - 1 + LEVEL_W * 3 + 7, PANPOT_Y + 7);

        assertEquals(1, events.size());
        assertEquals(FmDspVisualizer.EVENT_MUTE, events.getFirst().getName());
        assertEquals(3, events.getFirst().getArguments()[0]);
    }

    @Test
    @DisplayName("the last column, and nothing past it")
    void testEdges() {
        click(LEVEL_X - 1 + LEVEL_W * 18 + 1, PANPOT_Y + 1);
        assertEquals(18, events.getFirst().getArguments()[0]);

        events.clear();
        click(LEVEL_X - 1 + LEVEL_W * 19 + 1, PANPOT_Y + 1);
        click(LEVEL_X - 2, PANPOT_Y + 7);
        click(LEVEL_X - 1 + LEVEL_W * 3 + 7, PANPOT_Y - 1); // the meter itself
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("no meters, nothing to click on")
    void testTrackInfoMode() {
        vis.setRightMode(RightMode.TRACK_INFO);
        click(LEVEL_X - 1 + LEVEL_W * 3 + 7, PANPOT_Y + 7);
        assertTrue(events.isEmpty());
    }
}
