package mdplayer.form;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import static java.lang.System.getLogger;


/**
 * Hook keyboard operations and inject arbitrary methods.
 *
 * @see "https://github.com/kwhat/jnativehook"
 */
public class KeyboardHook {

    private static final Logger logger = getLogger(KeyboardHook.class.getName());

    static {
        // jnativehook's default locator wants its own code source as a file, which a nested jar cannot give
        if (System.getProperty("jnativehook.lib.locator") == null) {
            System.setProperty("jnativehook.lib.locator", NativeHookLibraryLocator.class.getName());
        }
    }

    private static class GlobalKeyListener implements NativeKeyListener {
        @Override
        public void nativeKeyPressed(NativeKeyEvent e) {
            logger.log(Level.TRACE, "Key Pressed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));

            if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
                try {
                    GlobalScreen.unregisterNativeHook();
                } catch (NativeHookException nativeHookException) {
                    logger.log(Level.ERROR, nativeHookException.getMessage(), nativeHookException);
                }
            }
        }

        @Override
        public void nativeKeyReleased(NativeKeyEvent e) {
            logger.log(Level.TRACE, "Key Released: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
        }

        @Override
        public void nativeKeyTyped(NativeKeyEvent e) {
            logger.log(Level.TRACE, "Key Typed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
        }
    }

    /**
     * Occurs when the keyboard is operated.
     */
    public void addKeyboardHooked(NativeKeyListener handler) {
        GlobalScreen.addNativeKeyListener(handler);
    }

    public void removeKeyboardHooked(NativeKeyListener handler) {
        GlobalScreen.removeNativeKeyListener(handler);
    }

    /** Lets go of the native hook; its thread is not a daemon and keeps the jvm up until then. */
    public void close() {
        try {
            if (GlobalScreen.isNativeHookRegistered()) GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * Create a new instance.
     */
    public KeyboardHook() {
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            throw new IllegalStateException("There was a problem registering the native hook.", ex);
        }
    }

    /**
     * Create an instance by specifying the delegate to execute when the keyboard is operated.
     *
     * @param handler An event handler that represents the method to be executed when the keyboard is operated.
     */
    public KeyboardHook(NativeKeyListener handler) {
        this();
        GlobalScreen.addNativeKeyListener(handler);
    }
}

