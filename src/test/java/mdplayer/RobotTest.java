package mdplayer;

import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;
import mdplayer.form.sys.FormMain;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


@EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
@PropsEntity(url = "file:local.properties")
class RobotTest {

    static boolean localPropertiesExists() {
            return Files.exists(Paths.get("local.properties"));
        }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        System.setProperty("mdplayer.volume", "%4.2f".formatted(volume));
        // it moves and closes the player: keep that off the user's own Setting.xml
        System.setProperty("mdplayer.setting.dir", "target/test-setting");
    }

    @Test
    public void testArgumentStartupDelayAndFadeoutState() throws Exception {
        File testSong = new File("src/test/resources/test.vgm");
        String songPath = testSong.getAbsolutePath();
        System.out.println("Starting test with song path: " + songPath);

        // Start mdplayer in GUI mode in a separate thread passing the song as an argument
        Thread t = new Thread(() -> {
            try {
                Program.main(new String[]{songPath});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();

        // 1. Wait for the main window to open and become visible
        FormMain mainFrame = null;
        long windowOpenedTime = 0;
        
        for (int i = 0; i < 200; i++) {
            Thread.sleep(20);
            for (Frame f : Frame.getFrames()) {
                if (f instanceof FormMain fm) {
                    if (fm.isVisible()) {
                        mainFrame = fm;
                        windowOpenedTime = System.currentTimeMillis();
                        break;
                    }
                }
            }
            if (mainFrame != null) break;
        }

        assertNotNull(mainFrame, "FormMain failed to become visible within timeout");
        System.out.println("FormMain window opened at timestamp: " + windowOpenedTime);

        // 2. Poll the startup phase: monitor Fadeout button state and wait for song to start playing
        Field activeField = FormMain.class.getDeclaredField("lstOpeButtonActive");
        activeField.setAccessible(true);
        Field audioFieldObj = FormMain.class.getDeclaredField("audio");
        audioFieldObj.setAccessible(true);

        long songStartedTime = 0;
        boolean fadeoutButtonFlashedOrange = false;

        // Poll every 50 ms for up to 5 seconds
        for (int i = 0; i < 100; i++) {
            Thread.sleep(50);
            
            // Check button states on FormMain
            boolean[] activeButtons = (boolean[]) activeField.get(mainFrame);
            if (activeButtons != null && activeButtons[3]) {
                fadeoutButtonFlashedOrange = true;
            }

            // Check if audio has started playing (stopped goes false)
            Audio audioObj = (Audio) audioFieldObj.get(mainFrame);
            if (audioObj != null && audioObj.plugin != null && !audioObj.plugin.stopped) {
                songStartedTime = System.currentTimeMillis();
                break;
            }
        }

        assertTrue(songStartedTime > 0, "Song failed to start playing within timeout");
        long latency = songStartedTime - windowOpenedTime;
        System.out.println("Song started playing at timestamp: " + songStartedTime);
        System.out.println("Measured latency (Window Opened -> Playback Started): " + latency + " ms");

        // 3. Assert the requirements:
        // A. Fadeout button (3rd button) must NOT have turned orange at startup
        assertFalse(fadeoutButtonFlashedOrange, "The 3rd button (Fadeout button) became orange at startup!");
        
        // B. Latency must be less than 1.5 seconds (1500 ms)
        assertTrue(latency < 1500, "Song start took too long: " + latency + " ms");

        // Let the song play for 3 seconds
        Thread.sleep(3000);

        // 4. Simulate closing the window (WINDOW_CLOSING) while playing
        System.out.println("Simulating window close button (WINDOW_CLOSING) while playing...");
        FormMain finalMain = mainFrame;
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            finalMain.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
            finalMain.dispatchEvent(new WindowEvent(finalMain, WindowEvent.WINDOW_CLOSING));
        });

        // Sleep to verify thread closes cleanly without locking the GUI/AWT queue
        Thread.sleep(5000);
        System.out.println("Test complete. Window closed successfully.");
    }

    @Test
    public void testCloseButtonExternalProcess() throws Exception {
        File testSong = new File("src/test/resources/test.vgm");
        String songPath = testSong.getAbsolutePath();
        System.out.println("Starting external process test with song path: " + songPath);

        // 1st. Run mdplayer w/ a song argument as normal process
        int windowX = 100;
        int windowY = 100;
        ProcessBuilder playerBuilder = new ProcessBuilder(
            "java",
            "-Dmdplayer.test.x=" + windowX,
            "-Dmdplayer.test.y=" + windowY,
            // keeps the spawned player's Setting.xml off the user's own
            "-Dmdplayer.setting.dir=" + System.getProperty("mdplayer.setting.dir"),
            "-cp",
            System.getProperty("java.class.path"),
            "mdplayer.Program",
            songPath
        );
        playerBuilder.inheritIO();
        Process playerProcess = playerBuilder.start();
        long pid = playerProcess.pid();
        System.out.println("Spawned mdplayer process with PID: " + pid);

        // Wait for mdplayer to startup and start playing
        Thread.sleep(5000);

        // 2nd. Use robot controller as a different jvm process
        ProcessBuilder robotBuilder = new ProcessBuilder(
            "java",
            "-cp",
            System.getProperty("java.class.path"),
            "mdplayer.RobotController",
            String.valueOf(pid),
            String.valueOf(windowX),
            String.valueOf(windowY)
        );
        robotBuilder.inheritIO();
        Process robotProcess = robotBuilder.start();
        int robotExit = robotProcess.waitFor();
        System.out.println("Robot controller process exited with code: " + robotExit);
        assertEquals(0, robotExit, "Robot controller failed to execute click.");

        // Wait for player process to terminate
        boolean exitedCleanly = playerProcess.waitFor(15, TimeUnit.SECONDS);
        
        if (!exitedCleanly) {
            System.err.println("DEADLOCK DETECTED! Printing thread dump using jstack for PID: " + pid);
            try {
                ProcessBuilder jstackBuilder = new ProcessBuilder("jstack", String.valueOf(pid));
                jstackBuilder.redirectErrorStream(true);
                Process jstackProcess = jstackBuilder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(jstackProcess.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println("  [jstack] " + line);
                }
                jstackProcess.waitFor();
            } catch (Exception e) {
                System.err.println("Failed to run jstack: " + e.getMessage());
            }
            // Force destroy the process to avoid leaving it running if there is a deadlock
            playerProcess.destroyForcibly();
        }
        
        assertTrue(exitedCleanly, "mdplayer process failed to close via window 'x' button (deadlock/hang detected)!");
        System.out.println("mdplayer process exited successfully.");
    }
}
