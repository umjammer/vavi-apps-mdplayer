package mdplayer.form;

import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.swing.SwingUtilities;

import static java.lang.System.getLogger;


/**
 * Keeps the player's windows together in the stacking order: when one of them comes up from
 * behind another application's window, the rest are brought up with it, so the player never ends
 * up half buried.
 * <p>
 * Only an activation that comes from outside the player counts — the opposite window of such an
 * event is {@code null} — so moving the focus between the player's own windows reorders nothing.
 * The others are raised without being focused, and the window the user picked is raised last, so
 * it stays on top and keeps the focus.
 * <p>
 * {@code -Dmdplayer.raiseTogether=false} turns it off.
 */
public final class WindowGroup {

    private static final Logger logger = getLogger(WindowGroup.class.getName());

    private static boolean installed;

    /** set while this raises windows, so what that stirs up is not taken for the user */
    private static boolean raising;

    private WindowGroup() {
    }

    /** starts watching the player's windows; calling it again does nothing */
    public static synchronized void install() {
        if (installed) return;
        installed = true;
        if (!Boolean.parseBoolean(System.getProperty("mdplayer.raiseTogether", "true"))) return;

        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
                if (e.getID() != WindowEvent.WINDOW_ACTIVATED) return;
                WindowEvent we = (WindowEvent) e;
                if (raising || we.getOppositeWindow() != null) return;
                if (we.getWindow() instanceof Frame f) raiseWith(f);
            }, AWTEvent.WINDOW_EVENT_MASK);
        } catch (SecurityException | UnsupportedOperationException e) {
            logger.log(Level.WARNING, "windows will not be raised together: " + e);
        }
    }

    /** brings every other shown frame of the player up, then {@code active} over them */
    static void raiseWith(Frame active) {
        raising = true;
        try {
            for (Window w : Window.getWindows()) {
                if (w == active || !(w instanceof Frame f) || !f.isShowing()) continue;
                if ((f.getExtendedState() & Frame.ICONIFIED) != 0) continue;

                boolean auto = f.isAutoRequestFocus();
                f.setAutoRequestFocus(false);
                try {
                    f.toFront();
                } finally {
                    f.setAutoRequestFocus(auto);
                }
            }
            active.toFront();
        } finally {
            // the platform answers the raises with events of its own, let those pass first
            SwingUtilities.invokeLater(() -> raising = false);
        }
    }
}
