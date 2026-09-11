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
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import mdplayer.Audio;
import mdplayer.ChipFmDspSource;
import mdplayer.Common;
import vavi.sound.visualizer.fmdsp.FmDspVisualizer;
import vavi.sound.visualizer.fmdsp.LeftMode;
import vavi.sound.visualizer.fmdsp.RightMode;
import vavi.util.event.GenericListener;


/**
 * FormFmdsp.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-12 nsano initial version <br>
 */
public class FormFmdsp extends JFrame {

    private static final Logger logger = System.getLogger(FormFmdsp.class.getName());

    private FmDspVisualizer fmdspVisualizerComponent;
    private ChipFmDspSource fmdspSource;
    private GenericListener fmdspListener;

    static byte[] fontRom = null;

    static {
        try {
            String name = System.getProperty("vavi.sound.visualizer.fmdsp.fontRom");
            if (name != null && Files.exists(Path.of(name))) {
                fontRom = Files.readAllBytes(Path.of(name));
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public void init() {
        if (fmdspSource != null) fmdspSource.reset();
    }

    public void open(Audio audio, Runnable pause) {
        if (this.isVisible()) {
            this.toFront();
            this.requestFocus();
            return;
        }

        if (fontRom == null) {
            JOptionPane.showMessageDialog(null, "vavi.sound.visualizer.fmdsp.fontRom is not set");
        }

        fmdspSource = new ChipFmDspSource();
        if (audio.plugin != null) {
            fmdspSource.bind(audio.plugin);
            fmdspSource.setFilename(audio.plugin.playingFileName);
        }

        fmdspVisualizerComponent = new FmDspVisualizer(60, 2);
        fmdspVisualizerComponent.setTitle("MDDSP");
        fmdspVisualizerComponent.setVersion(Common.version.equals("undefined") ? null : Common.version);
        fmdspVisualizerComponent.setDataSource(fmdspSource);
        fmdspVisualizerComponent.setFontRom(fontRom);

        this.setTitle((audio.plugin != null && audio.plugin.playingFileName != null ? audio.plugin.playingFileName : "") + " - MDDSP");
        this.setLayout(new BorderLayout());
        this.add(fmdspVisualizerComponent, BorderLayout.CENTER);
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code >= KeyEvent.VK_F1 && code <= KeyEvent.VK_F10) {
                    fmdspVisualizerComponent.setPaletteIndex(code - KeyEvent.VK_F1);
                } else if (code == KeyEvent.VK_F11) {
                    if (e.isShiftDown()) {
                        RightMode[] r = RightMode.values();
                        fmdspVisualizerComponent.setRightMode(r[(fmdspVisualizerComponent.getRightMode().ordinal() + 1) % r.length]);
                    } else {
                        LeftMode[] l = LeftMode.values();
                        fmdspVisualizerComponent.setLeftMode(l[(fmdspVisualizerComponent.getLeftMode().ordinal() + 1) % l.length]);
                    }
                } else if (code == KeyEvent.VK_SPACE) {
                    pause.run();
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

        fmdspListener = fmdspSource::update;
        audio.addGenericListener(fmdspListener);
        fmdspVisualizerComponent.start();
    }

    public void close(Audio audio) {
        if (this.isVisible()) return;

        if (fmdspListener != null) {
            audio.removeGenericListener(fmdspListener);
            fmdspListener = null;
        }
        if (fmdspVisualizerComponent != null) {
            try {
                fmdspVisualizerComponent.stop();
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
            fmdspVisualizerComponent = null;
        }
        fmdspSource = null;
        try {
            this.setVisible(false);
            this.dispose();
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    public void pause(Audio audio) {
        if (fmdspSource != null) {
            fmdspSource.setPaused(audio.isPaused());
        }
    }

    public void start(Audio audio) {
        if (fmdspSource != null && audio.plugin != null) {
            fmdspSource.reset();
            fmdspSource.bind(audio.plugin);
            fmdspSource.setFilename(audio.plugin.playingFileName);
            this.setTitle((audio.plugin.playingFileName != null ? audio.plugin.playingFileName : "") + " - MDDSP");
        }
    }
}
