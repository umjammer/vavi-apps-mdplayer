/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.form.sys.visualizer;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import javax.swing.JFrame;

import mdplayer.Audio;
import mdplayer.ChipFmDspSource;
import mdplayer.form.VisualizerProvider;
import mdplayer.form.sys.FormMain;
import vavi.sound.visualizer.boids.BoidsParams;
import vavi.sound.visualizer.boids.BoidsVisualizer;
import vavi.util.event.GenericListener;


/**
 * FormBoids.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-12 nsano initial version <br>
 */
public class FormBoids extends JFrame {

    private static final Logger logger = System.getLogger(FormBoids.class.getName());

    private BoidsVisualizer boidsVisualizerComponent;
    private ChipFmDspSource boidsSource;
    private GenericListener boidsListener;

    /** the player this window is showing, set by {@link #open} */
    private Audio audio;
    /** pauses the player as the main window does, set by {@link #open} */
    private Runnable pause;

    private int preset;

    /**
     * The listeners belong to the frame, which is reused from one open to the next, so they are
     * added once here: adding them in {@link #open} stacked one more of each on every reopen.
     */
    public FormBoids() {
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.setLayout(new BorderLayout());
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (boidsVisualizerComponent == null) return;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE -> pause.run(); // FormMain#pause tells us back through #pause
                    case KeyEvent.VK_P -> {
                        List<String> names = BoidsParams.presetNames();
                        preset = (preset + 1) % names.size();
                        boidsVisualizerComponent.getParams().applyPreset(names.get(preset));
                    }
                    case KeyEvent.VK_ESCAPE -> close(audio);
                    default -> {}
                }
            }
        });
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close(audio);
            }
        });
    }

    /** @param at where to put the window, null to centre it */
    public void open(Audio audio, Runnable pause, Point at) {
        if (boidsVisualizerComponent != null) {
            this.toFront();
            this.requestFocus();
            return;
        }

        this.audio = audio;
        this.pause = pause;

        boidsSource = new ChipFmDspSource();
        if (audio.plugin != null) {
            boidsSource.bind(audio.plugin);
            boidsSource.setFilename(audio.plugin.playingFileName);
        }
        boidsSource.setPaused(audio.isPaused());

        boidsVisualizerComponent = new BoidsVisualizer(60, 1280, 960);
        boidsVisualizerComponent.setDataSource(boidsSource);

        this.setTitle(title(audio));
        this.add(boidsVisualizerComponent, BorderLayout.CENTER);
        this.pack();
        if (at != null) this.setLocation(at); else this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.requestFocusInWindow();

        boidsListener = boidsSource::update;
        audio.addGenericListener(boidsListener);
        boidsVisualizerComponent.start();
    }

    /** releases what {@link #open} took; does nothing when this is not open */
    public void close(Audio audio) {
        if (boidsVisualizerComponent == null) return;

        if (boidsListener != null) {
            audio.removeGenericListener(boidsListener);
            boidsListener = null;
        }
        try {
            boidsVisualizerComponent.stop();
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
        // dispose() leaves the children in place, the next open would show two of them
        this.remove(boidsVisualizerComponent);
        boidsVisualizerComponent = null;
        boidsSource = null;
        try {
            this.setVisible(false);
            this.dispose();
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    public void pause(Audio audio) {
        if (boidsSource != null) {
            boidsSource.setPaused(audio.isPaused());
        }
    }

    /**
     * Points this at a new song: the plugin is a new one for every song, and the chips it shares
     * with the last one still hold that song's state. Call after {@link Audio#init} and before the
     * song starts playing.
     */
    public void start(Audio audio) {
        if (boidsSource != null && audio.plugin != null) {
            boidsSource.reset();
            boidsSource.bind(audio.plugin);
            boidsSource.setFilename(audio.plugin.playingFileName);
            boidsVisualizerComponent.reset();
            this.setTitle(title(audio));
        }
    }

    private static String title(Audio audio) {
        return (audio.plugin != null && audio.plugin.playingFileName != null ? audio.plugin.playingFileName : "") + " - boids";
    }

    /** the {@link VisualizerProvider} registration; the window is made on the first open */
    public static class Provider implements VisualizerProvider {

        private FormBoids form;

        @Override public String id() { return "boids"; }
        @Override public String menuText() { return "BOIDS"; }

        @Override public void open(FormMain main) {
            if (form == null) form = new FormBoids();
            form.open(Audio.getInstance(), main::pause, savedLocation());
        }

        @Override public void close() { if (form != null) form.close(Audio.getInstance()); }
        @Override public java.awt.Window window() { return form; }
        @Override public void start(Audio audio) { if (form != null) form.start(audio); }
        @Override public void pause(Audio audio) { if (form != null) form.pause(audio); }
    }
}
