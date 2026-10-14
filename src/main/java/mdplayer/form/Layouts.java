/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.form;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.MenuElement;
import javax.swing.border.TitledBorder;


/**
 * Lays the ported WinForms dialogs out.
 * <p>
 * Their components are placed at absolute coordinates, never by a layout manager, but they are
 * added to a container that still has its default {@link java.awt.BorderLayout} — which lays out
 * only one child, leaving all the others invisible. So drop the layout manager and give every
 * component the bounds it is meant to have.
 * <p>
 * Where those bounds come from differs by form, because the C# designer split them the same way:
 * some forms set {@code setLocation()}/{@code setPreferredSize()} in code, while the bigger ones
 * (frmSetting, frmPlayList) left it all to {@code resources.ApplyResources()} and their {@code
 * .resx}. For the latter, pass the bundle converted from that {@code .resx} — the same
 * {@code name.Location} / {@code name.Size} keys, looked up by {@link Component#getName()}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-14 nsano initial version <br>
 */
public final class Layouts {

    private Layouts() {
    }

    /** Places every component where the code says it is. */
    public static void absolute(Container container) {
        absolute(container, null);
    }

    /** Places every component where {@code bundle} says it is, or, failing that, the code. */
    public static void absolute(Container container, ResourceBundle bundle) {
        container.setLayout(null);

        for (Component component : container.getComponents()) {
            Point at = point(bundle, component, ".Location", component.getLocation());

            text(bundle, component);

            Dimension size = fit(component, dimension(bundle, component, ".Size", component.getPreferredSize()));
            component.setBounds(at.x, at.y, size.width, size.height);

            descend(component, bundle);
        }
    }

    /**
     * A panel holds more of the same, so it is laid out the same way. A tabbed pane sizes its own
     * pages, so those are laid out but not placed. Anything else (a scroll pane, a table, a combo
     * box) manages what is inside it and is left alone.
     */
    private static void descend(Component component, ResourceBundle bundle) {
        switch (component) {
            case JPanel panel -> absolute(panel, bundle);
            case JTabbedPane tabbed -> {
                for (int i = 0; i < tabbed.getTabCount(); i++) {
                    Component page = tabbed.getComponentAt(i);
                    // the pages were added by add(page), which titles the tab after the component
                    // rather than after the caption the designer gave it
                    String title = string(bundle, page, ".Text");
                    if (title != null) tabbed.setTitleAt(i, title);

                    if (page instanceof Container container) {
                        absolute(container, bundle);
                    }
                }
            }
            default -> {
            }
        }
    }

    /**
     * Gives a menu and its submenus the captions the designer gave them. The menus are not laid out
     * — Swing does that — but their items were named and never captioned, so they come up blank.
     */
    public static void captions(MenuElement menu, ResourceBundle bundle) {
        for (MenuElement item : menu.getSubElements()) {
            text(bundle, item.getComponent());
            captions(item, bundle);
        }
    }

    /**
     * Gives a laid out tree the captions the designer gave it: buttons, labels, tabs and group
     * boxes made by {@link Grid#group}. Only the blank ones are filled, so a caption the code set
     * wins — the {@code .resx} names are not unique across the user controls that were merged in.
     */
    public static void labels(Container container, ResourceBundle bundle) {
        for (Component component : container.getComponents()) {
            String value = string(bundle, component, ".Text");
            if (value != null) {
                switch (component) {
                    case AbstractButton button when blank(button.getText()) -> button.setText(value);
                    case JLabel label when blank(label.getText()) -> label.setText(value);
                    case JComponent c when c.getBorder() instanceof TitledBorder border && blank(border.getTitle()) ->
                            border.setTitle(value);
                    default -> {
                    }
                }
            }

            if (component instanceof JTabbedPane tabbed) {
                for (int i = 0; i < tabbed.getTabCount(); i++) {
                    // the pages were added by add(page), which titles the tab after the component
                    String title = string(bundle, tabbed.getComponentAt(i), ".Text");
                    if (title != null) tabbed.setTitleAt(i, title);
                }
            }
            if (component instanceof Container child) {
                labels(child, bundle);
            }
        }
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }

    /** The designer put the labels and the button captions in the {@code .resx} too. */
    private static void text(ResourceBundle bundle, Component component) {
        String value = string(bundle, component, ".Text");
        if (value == null) return;

        switch (component) {
            case AbstractButton button -> button.setText(value);
            case JLabel label -> label.setText(value);
            default -> {
            }
        }
    }

    /**
     * A caption was sized by the designer for the font WinForms drew it in. Swing's is wider, so
     * the size in the {@code .resx} cuts the text off ("Output Dev..."); let it have the room it
     * asks for instead.
     */
    private static Dimension fit(Component component, Dimension size) {
        if (!(component instanceof AbstractButton || component instanceof JLabel)) return size;

        Dimension wanted = component.getPreferredSize();
        return new Dimension(Math.max(size.width, wanted.width), Math.max(size.height, wanted.height));
    }

    private static Point point(ResourceBundle bundle, Component component, String key, Point defaultValue) {
        int[] pair = pair(string(bundle, component, key));
        return pair == null ? defaultValue : new Point(pair[0], pair[1]);
    }

    private static Dimension dimension(ResourceBundle bundle, Component component, String key, Dimension defaultValue) {
        int[] pair = pair(string(bundle, component, key));
        return pair == null ? defaultValue : new Dimension(pair[0], pair[1]);
    }

    /** {@code "500, 437"} */
    private static int[] pair(String value) {
        if (value == null) return null;

        String[] parts = value.split(",");
        if (parts.length != 2) return null;

        try {
            return new int[] {Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String string(ResourceBundle bundle, Component component, String key) {
        if (component == null || bundle == null || component.getName() == null) return null;

        try {
            return bundle.getString(component.getName() + key);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    /** The size the designer gave the form itself, {@code $this.ClientSize}. */
    public static Dimension clientSize(ResourceBundle bundle, Dimension defaultValue) {
        try {
            int[] pair = pair(bundle.getString("$this.ClientSize"));
            return pair == null ? defaultValue : new Dimension(pair[0], pair[1]);
        } catch (MissingResourceException e) {
            return defaultValue;
        }
    }
}
