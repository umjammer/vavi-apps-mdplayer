/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.form.sys;

import java.awt.BorderLayout;
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

    public void init() {
        if (boidsSource != null) boidsSource.reset();
    }

    public void open(Audio audio, Runnable pause) {
        if (this.isVisible()) {
            this.toFront();
            this.requestFocus();
            return;
        }

        boidsSource = new ChipFmDspSource();
        if (audio.plugin != null) {
            boidsSource.bind(audio.plugin);
            boidsSource.setFilename(audio.plugin.playingFileName);
        }

        boidsVisualizerComponent = new BoidsVisualizer(60, 1280, 960);
        boidsVisualizerComponent.setDataSource(boidsSource);

        this.setTitle((audio.plugin != null && audio.plugin.playingFileName != null ? audio.plugin.playingFileName : "") + " - MDDSP");
        this.setLayout(new BorderLayout());
        this.add(boidsVisualizerComponent, BorderLayout.CENTER);
        this.addKeyListener(new KeyAdapter() {
            private int preset;

            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE -> {
                        pause.run();
                        boidsSource.setPaused(audio.isPaused());
                    }
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
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.requestFocusInWindow();

        boidsListener = boidsSource::update;
        audio.addGenericListener(boidsListener);
        boidsVisualizerComponent.start();
    }

    public void close(Audio audio) {
        if (this.isVisible()) return;

        if (boidsListener != null) {
            audio.removeGenericListener(boidsListener);
            boidsListener = null;
        }
        if (boidsVisualizerComponent != null) {
            try {
                boidsVisualizerComponent.stop();
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
            boidsVisualizerComponent = null;
        }
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

    public void start(Audio audio) {
        if (boidsSource != null && audio.plugin != null) {
            boidsSource.reset();
            boidsSource.bind(audio.plugin);
            boidsSource.setFilename(audio.plugin.playingFileName);
            this.setTitle((audio.plugin.playingFileName != null ? audio.plugin.playingFileName : "") + " - boids");
        }
    }
}
