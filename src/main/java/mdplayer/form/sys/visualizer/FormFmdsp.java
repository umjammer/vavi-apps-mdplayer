/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.form.sys.visualizer;

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
import mdplayer.form.VisualizerProvider;
import mdplayer.Common;
import mdplayer.form.sys.FormMain;
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

    /** the player this window is showing, set by {@link #open} */
    private Audio audio;
    /** pauses the player as the main window does, set by {@link #open} */
    private Runnable pause;

    /**
     * The listeners belong to the frame, which is reused from one open to the next, so they are
     * added once here: adding them in {@link #open} stacked one more of each on every reopen.
     */
    public FormFmdsp() {
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.setLayout(new BorderLayout());
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (fmdspVisualizerComponent == null) return;
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
                } else if (code == KeyEvent.VK_ESCAPE) {
                    close(audio);
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

    public void open(Audio audio, Runnable pause) {
        if (fmdspVisualizerComponent != null) {
            this.toFront();
            this.requestFocus();
            return;
        }

        if (fontRom == null) {
            JOptionPane.showMessageDialog(null, "vavi.sound.visualizer.fmdsp.fontRom is not set");
        }

        this.audio = audio;
        this.pause = pause;

        fmdspSource = new ChipFmDspSource();
        if (audio.plugin != null) {
            fmdspSource.bind(audio.plugin);
            fmdspSource.setFilename(audio.plugin.playingFileName);
        }
        fmdspSource.setPaused(audio.isPaused());

        fmdspVisualizerComponent = new FmDspVisualizer(60, 2);
        fmdspVisualizerComponent.setTitle("MDDSP");
        fmdspVisualizerComponent.setVersion(Common.version.equals("undefined") ? null : Common.version);
        fmdspVisualizerComponent.setDataSource(fmdspSource);
        fmdspVisualizerComponent.setFontRom(fontRom);

        this.setTitle(title(audio));
        this.add(fmdspVisualizerComponent, BorderLayout.CENTER);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.requestFocusInWindow();

        fmdspListener = fmdspSource::update;
        audio.addGenericListener(fmdspListener);
        fmdspVisualizerComponent.start();
    }

    /** releases what {@link #open} took; does nothing when this is not open */
    public void close(Audio audio) {
        if (fmdspVisualizerComponent == null) return;

        if (fmdspListener != null) {
            audio.removeGenericListener(fmdspListener);
            fmdspListener = null;
        }
        try {
            fmdspVisualizerComponent.stop();
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
        // dispose() leaves the children in place, the next open would show two of them
        this.remove(fmdspVisualizerComponent);
        fmdspVisualizerComponent = null;
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

    /**
     * Points this at a new song: the plugin is a new one for every song, and the chips it shares
     * with the last one still hold that song's state. Call after {@link Audio#init} and before the
     * song starts playing.
     */
    public void start(Audio audio) {
        if (fmdspSource != null && audio.plugin != null) {
            fmdspSource.reset();
            fmdspSource.bind(audio.plugin);
            fmdspSource.setFilename(audio.plugin.playingFileName);
            this.setTitle(title(audio));
        }
    }

    private static String title(Audio audio) {
        return (audio.plugin != null && audio.plugin.playingFileName != null ? audio.plugin.playingFileName : "") + " - MDDSP";
    }

    /** the {@link VisualizerProvider} registration; the window is made on the first open */
    public static class Provider implements VisualizerProvider {

        private FormFmdsp form;

        @Override public String id() { return "fmdsp"; }
        @Override public String menuText() { return "FMDSP Style"; }

        @Override public void open(FormMain main) {
            if (form == null) form = new FormFmdsp();
            form.open(Audio.getInstance(), main::pause);
        }

        @Override public void close() { if (form != null) form.close(Audio.getInstance()); }
        @Override public void start(Audio audio) { if (form != null) form.start(audio); }
        @Override public void pause(Audio audio) { if (form != null) form.pause(audio); }
    }
}
