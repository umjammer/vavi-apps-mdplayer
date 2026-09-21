package mdplayer;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import mdplayer.form.sys.FormMain;
import mdplayer.form.sys.FormPlayList;
import mdplayer.form.sys.FormSetting;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


// set "vavi.sound.visualizer.fmdsp.fontRom" prevents this test from message dialog showing
@EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
@PropsEntity(url = "file:local.properties")
class RefactoringTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    /**
     * Keeps the windows off the visible screen.
     * <p>
     * This test opens and closes the whole UI, which flashes the monitor every run - unpleasant to
     * sit next to. AWT cannot do this headless, a real frame needs a display, but it can be put
     * where no one has to look at it. Pass {@code -Dmdplayer.test.gui.visible=true} to watch it.
     */
    private static void hideWindowsOffScreen() {
        if (Boolean.getBoolean("mdplayer.test.gui.visible")) return;
        System.setProperty("apple.awt.UIElement", "true"); // no dock icon, no focus stealing
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event.getSource() instanceof Window w && w.getX() > -30000) {
                w.setLocation(-32000, -32000);
            }
        }, AWTEvent.WINDOW_EVENT_MASK | AWTEvent.COMPONENT_EVENT_MASK);
    }

    @BeforeEach
    void setup() throws Exception {
        hideWindowsOffScreen();

        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        System.setProperty("mdplayer.volume", "%4.2f".formatted(volume));
    }

    private static final Set<String> IGNORED_NAMES = Set.of(
        "tsmiOpenFile",
        "tsmiExit",
        "tsmiOption",
        "tsmiPlay",
        "tsmiStop",
        "tsmiPause",
        "tsmiFadeOut",
        "tsmiSlow",
        "tsmiFf",
        "tsmiNext",
        "tsmiPlayMode"
    );

    private static void getMenuItems(Component comp, List<JMenuItem> list) {
        if (comp instanceof JMenu menu) {
            for (Component sub : menu.getMenuComponents()) {
                getMenuItems(sub, list);
            }
        } else if (comp instanceof JMenuItem) {
            list.add((JMenuItem) comp);
        } else if (comp instanceof JPopupMenu popup) {
            for (Component sub : popup.getComponents()) {
                getMenuItems(sub, list);
            }
        } else if (comp instanceof Container container) {
            for (Component sub : container.getComponents()) {
                getMenuItems(sub, list);
            }
        }
    }

    private static void cleanupExtraWindows(FormMain mainFrame, FormPlayList playlistFrame) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (Window w : Window.getWindows()) {
                if (w != mainFrame && w != playlistFrame && w.isVisible()) {
                    w.setVisible(false);
                    w.dispose();
                }
            }
        });
    }

    @Test
    public void testAllFormExecutions() throws Exception {
        // Start mdplayer in GUI mode in a separate thread
        Thread t = new Thread(() -> {
            try {
                Program.main(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();

        // Wait for the main frame to be fully constructed, visible, and fields initialized
        FormMain mainFrame = null;
        Field audioField = FormMain.class.getDeclaredField("audio");
        audioField.setAccessible(true);
        Field frmPlayListField = FormMain.class.getDeclaredField("frmPlayList");
        frmPlayListField.setAccessible(true);
        
        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);
            for (Frame f : Frame.getFrames()) {
                if (f instanceof FormMain fm) {
                    try {
                        if (fm.isVisible() && audioField.get(fm) != null && frmPlayListField.get(fm) != null) {
                            mainFrame = fm;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (mainFrame != null) break;
        }

        if (mainFrame == null) {
            throw new RuntimeException("frmMain not found or not visible");
        }

        FormMain finalMain = mainFrame;
        FormPlayList playlistFrame = (FormPlayList) frmPlayListField.get(finalMain);

        System.out.println("frmMain found! Preparing test song in playlist...");
        File testSong = new File("src/test/resources/test.vgm");
        String songPath = testSong.getAbsolutePath();
        System.out.println("Song path: " + songPath);
        
        // Add file to playlist and refresh to populate UI table
        SwingUtilities.invokeAndWait(() -> {
            playlistFrame.getPlayList().getMusics().clear();
            playlistFrame.getPlayList().addFile(songPath);
            playlistFrame.refresh();
        });

        System.out.println("Starting playback of the song from playlist...");
        SwingUtilities.invokeAndWait(finalMain::play);

        Thread.sleep(2000); // Let it play for 2 seconds

        // Get cmsMenu and cmsOpenOtherPanel using reflection
        Field cmsMenuField = FormMain.class.getDeclaredField("cmsMenu");
        cmsMenuField.setAccessible(true);
        JPopupMenu cmsMenu = (JPopupMenu) cmsMenuField.get(finalMain);

        Field cmsOpenOtherPanelField = FormMain.class.getDeclaredField("cmsOpenOtherPanel");
        cmsOpenOtherPanelField.setAccessible(true);
        JPopupMenu cmsOpenOtherPanel = (JPopupMenu) cmsOpenOtherPanelField.get(finalMain);

        List<JMenuItem> items = new ArrayList<>();
        getMenuItems(cmsMenu, items);
        getMenuItems(cmsOpenOtherPanel, items);

        System.out.println("Found " + items.size() + " menu items to execute.");

        for (JMenuItem item : items) {
            String name = item.getName();
            if (name == null || IGNORED_NAMES.contains(name)) {
                continue;
            }

            System.out.println("Executing menu item: " + name + " (" + item.getText() + ")");
            try {
                SwingUtilities.invokeAndWait(item::doClick);
                Thread.sleep(200);
            } catch (Exception e) {
                System.err.println("Failed on menu item: " + name);
                throw e;
            } finally {
                cleanupExtraWindows(finalMain, playlistFrame);
                Thread.sleep(50);
            }
        }

        System.out.println("All form execution tests completed successfully!");
        SwingUtilities.invokeAndWait(finalMain::dispose);
        Thread.sleep(1000);
    }

    @Test
    public void testFormSettingTabsTraverse() throws Exception {
        // Start mdplayer in GUI mode in a separate thread
        Thread t = new Thread(() -> {
            try {
                Program.main(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();

        // Wait for the main frame to be fully constructed, visible, and fields initialized
        FormMain mainFrame = null;
        Field audioField = FormMain.class.getDeclaredField("audio");
        audioField.setAccessible(true);
        Field frmPlayListField = FormMain.class.getDeclaredField("frmPlayList");
        frmPlayListField.setAccessible(true);
        
        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);
            for (Frame f : Frame.getFrames()) {
                if (f instanceof FormMain fm) {
                    try {
                        if (fm.isVisible() && audioField.get(fm) != null && frmPlayListField.get(fm) != null) {
                            mainFrame = fm;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (mainFrame != null) break;
        }

        if (mainFrame == null) {
            throw new RuntimeException("frmMain not found or not visible");
        }

        FormMain finalMain = mainFrame;
        FormPlayList playlistFrame = (FormPlayList) frmPlayListField.get(finalMain);

        // Get cmsMenu using reflection
        Field cmsMenuField = FormMain.class.getDeclaredField("cmsMenu");
        cmsMenuField.setAccessible(true);
        JPopupMenu cmsMenu = (JPopupMenu) cmsMenuField.get(finalMain);

        List<JMenuItem> items = new ArrayList<>();
        getMenuItems(cmsMenu, items);

        JMenuItem tsmiOption = null;
        for (JMenuItem item : items) {
            if ("tsmiOption".equals(item.getName())) {
                tsmiOption = item;
                break;
            }
        }

        if (tsmiOption == null) {
            throw new RuntimeException("tsmiOption menu item not found");
        }

        System.out.println("Opening settings dialog...");
        JMenuItem finalOption = tsmiOption;
        SwingUtilities.invokeLater(finalOption::doClick);

        FormSetting settingFrame = null;
        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);
            for (Window w : Window.getWindows()) {
                if (w instanceof FormSetting && w.isVisible()) {
                    settingFrame = (FormSetting) w;
                    break;
                }
            }
            if (settingFrame != null) break;
        }

        if (settingFrame == null) {
            throw new RuntimeException("FormSetting dialog not found or not visible");
        }

        System.out.println("FormSetting found! Starting tab traversal...");

        // Find JTabbedPane tcSetting recursively
        List<JTabbedPane> tabbedPanes = findComponents(settingFrame, JTabbedPane.class);
        System.out.println("Found " + tabbedPanes.size() + " tabbed panes initially.");

        JTabbedPane tcSetting = null;
        for (JTabbedPane tp : tabbedPanes) {
            if ("tcSetting".equals(tp.getName())) {
                tcSetting = tp;
                break;
            }
        }

        if (tcSetting == null) {
            throw new RuntimeException("tcSetting tabbed pane not found");
        }

        JTabbedPane finalTcSetting = tcSetting;
        int tcCount = finalTcSetting.getTabCount();
        System.out.println("Selecting each tab in tcSetting (" + tcCount + " tabs)...");

        for (int i = 0; i < tcCount; i++) {
            int index = i;
            String tabTitle = finalTcSetting.getTitleAt(index);
            System.out.println("Selecting tab: " + tabTitle);
            SwingUtilities.invokeAndWait(() -> finalTcSetting.setSelectedIndex(index));
            Thread.sleep(100);

            // Scan for nested tabbed panes within this selected tab
            List<JTabbedPane> nestedPanes = findComponents(settingFrame, JTabbedPane.class);
            for (JTabbedPane nested : nestedPanes) {
                if (nested != finalTcSetting) {
                    int nestedCount = nested.getTabCount();
                    String nestedName = nested.getName();
                    System.out.println("Selecting each tab in nested tabbed pane: " + nestedName + " (" + nestedCount + " tabs)...");
                    for (int j = 0; j < nestedCount; j++) {
                        int nestedIdx = j;
                        String nestedTitle = nested.getTitleAt(nestedIdx);
                        System.out.println("Selecting nested tab: " + nestedTitle);
                        SwingUtilities.invokeAndWait(() -> nested.setSelectedIndex(nestedIdx));
                        Thread.sleep(50);
                    }
                }
            }
        }

        System.out.println("Clicking OK button on settings dialog...");
        List<JButton> buttons = findComponents(settingFrame, JButton.class);
        JButton btnOK = null;
        for (JButton btn : buttons) {
            if ("btnOK".equals(btn.getName())) {
                btnOK = btn;
                break;
            }
        }

        if (btnOK == null) {
            throw new RuntimeException("btnOK button not found");
        }

        JButton finalBtnOK = btnOK;
        SwingUtilities.invokeAndWait(finalBtnOK::doClick);
        Thread.sleep(500);

        System.out.println("All setting tabs traversed and closed successfully!");
        SwingUtilities.invokeAndWait(finalMain::dispose);
        Thread.sleep(1000);
    }

    private static <T extends Component> List<T> findComponents(Container container, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        for (Component comp : container.getComponents()) {
            if (clazz.isInstance(comp)) {
                result.add(clazz.cast(comp));
            }
            if (comp instanceof Container) {
                result.addAll(findComponents((Container) comp, clazz));
            }
        }
        return result;
    }
}
