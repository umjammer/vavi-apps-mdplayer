/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.form;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import mdplayer.Audio;
import mdplayer.form.sys.FormMain;


/**
 * One entry of the main window's visualizer menu, found via {@link ServiceLoader}.
 * <p>
 * Adding a visualizer means implementing this next to its window and registering it in
 * {@code META-INF/services}: the menu, the song and pause notifications and closing it with the
 * main window all iterate the provider list instead of naming visualizers.
 * <p>
 * A provider is loaded once and lives as long as the player, so it owns its window: it creates
 * it on the first {@link #open} and keeps it for the next one.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-23 nsano initial version <br>
 */
public interface VisualizerProvider {

    /** stable identifier; names the generated menu item */
    String id();

    /** caption of this visualizer's menu item */
    default String menuText() {
        return id();
    }

    /**
     * Called once as the main window comes up, to open again what was open when the player was
     * last closed. The default opens nothing.
     */
    default void restore(FormMain main) {
    }

    /** opens the window, or brings it to the front when it is open already */
    void open(FormMain main);

    /**
     * Called as the main window closes: releases what {@link #open} took, and remembers whatever
     * {@link #restore} wants for the next run. Does nothing when this was never opened.
     */
    void close();

    /**
     * A new song is about to play, {@link Audio#plugin} is already the new one: called after
     * {@link Audio#init} and before a sample is rendered.
     */
    default void start(Audio audio) {
    }

    /** the player was paused or resumed, {@link Audio#isPaused()} tells which */
    default void pause(Audio audio) {
    }

    /** one rendered sample, as the driver produces it */
    default void push(short left, short right) {
    }

    /** every registered provider, in registration order */
    static List<VisualizerProvider> providers() {
        return Holder.providers;
    }

    /** loads once; the service file's order is the menu order */
    class Holder {

        private Holder() {
        }

        private static final List<VisualizerProvider> providers = new ArrayList<>();

        static {
            for (VisualizerProvider p : ServiceLoader.load(VisualizerProvider.class)) {
                providers.add(p);
            }
        }
    }
}
