/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.form;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;


/**
 * Fills a container row by row with a {@link GridBagLayout}.
 * <p>
 * Cells take their preferred size and line up in columns; {@link #fill} gives a cell the width
 * left over, {@link #span} runs to the end of the row, {@link #grow} also takes the height left
 * over. {@link #end()} keeps everything at the top left when no cell takes what is left over.
 *
 * <pre>
 * new Grid(panel)
 *     .add(label).fill(field).add(button).next()
 *     .span(group).next()
 *     .end();
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-04 nsano initial version <br>
 */
public final class Grid {

    private final Container container;

    private final GridBagConstraints c = new GridBagConstraints();

    private int row;
    private int column;
    private int columns;
    /** a cell takes the width left over, so the rows need no filler on the right */
    private boolean widened;
    /** a cell takes the height left over, so the rows need no filler below */
    private boolean grown;

    public Grid(Container container) {
        this.container = container;
        container.setLayout(new GridBagLayout());
    }

    /** a grid on a new transparent panel, which {@link #build()} returns */
    public static Grid panel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return new Grid(panel);
    }

    /** a cell of its preferred size */
    public Grid add(Component component) {
        return put(component, 1, GridBagConstraints.NONE, 0, 0);
    }

    /** a cell of its preferred size, {@code columns} wide */
    public Grid add(Component component, int columns) {
        return put(component, columns, GridBagConstraints.NONE, 0, 0);
    }

    /** a cell as wide as its column, which is as wide as its widest cell */
    public Grid wide(Component component) {
        return put(component, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    }

    /** a cell taking the width left over */
    public Grid fill(Component component) {
        return put(component, 1, GridBagConstraints.HORIZONTAL, 1, 0);
    }

    /** a cell taking the width left over, {@code columns} wide */
    public Grid fill(Component component, int columns) {
        return put(component, columns, GridBagConstraints.HORIZONTAL, 1, 0);
    }

    /** a cell running to the end of the row, as wide as the container */
    public Grid span(Component component) {
        return put(component, GridBagConstraints.REMAINDER, GridBagConstraints.HORIZONTAL, 1, 0);
    }

    /** a cell running to the end of the row, taking the width and the height left over */
    public Grid grow(Component component) {
        grown = true;
        return put(component, GridBagConstraints.REMAINDER, GridBagConstraints.BOTH, 1, 1);
    }

    /** an empty row taking the height left over, which pushes the rows after it to the bottom */
    public Grid spring() {
        if (column != 0) next();
        grown = true;
        put(Box.createGlue(), GridBagConstraints.REMAINDER, GridBagConstraints.BOTH, 0, 1);
        return next();
    }

    /** an empty cell */
    public Grid skip() {
        column++;
        return this;
    }

    /** starts the next row */
    public Grid next() {
        row++;
        column = 0;
        return this;
    }

    /** keeps the cells at the top left, unless one of them is to take what is left over */
    public void end() {
        if (!widened) {
            c.gridx = columns;
            c.gridy = 0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 1;
            c.weighty = 0;
            container.add(Box.createGlue(), c);
        }
        if (!grown) {
            if (column != 0) next();
            put(Box.createGlue(), GridBagConstraints.REMAINDER, GridBagConstraints.BOTH, 0, 1);
        }
    }

    /** {@link #end()}s a grid made by {@link #panel()} and returns the panel */
    public JPanel build() {
        end();
        return (JPanel) container;
    }

    private Grid put(Component component, int span, int fill, double weightx, double weighty) {
        c.gridx = column;
        c.gridy = row;
        c.gridwidth = span;
        c.fill = fill;
        c.weightx = weightx;
        c.weighty = weighty;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(2, 3, 2, 3);
        container.add(component, c);
        column += span == GridBagConstraints.REMAINDER ? 1 : span;
        columns = Math.max(columns, column);
        if (weightx > 0) widened = true;
        return this;
    }

    /**
     * Makes {@code panel} a group box. The caption is left empty for {@link Layouts#labels} to
     * fill from the resources; a group without one keeps just its frame.
     */
    public static <T extends JComponent> T group(T panel) {
        return group(panel, "");
    }

    /** Makes {@code panel} a group box captioned {@code title}. */
    public static <T extends JComponent> T group(T panel, String title) {
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    /** components side by side at their preferred sizes */
    public static JPanel row(Component... components) {
        return flow(FlowLayout.LEADING, components);
    }

    /** components side by side at their preferred sizes, pushed to the trailing edge */
    public static JPanel trailing(Component... components) {
        return flow(FlowLayout.TRAILING, components);
    }

    private static JPanel flow(int align, Component... components) {
        JPanel panel = new JPanel(new FlowLayout(align, 4, 0));
        panel.setOpaque(false);
        for (Component component : components) panel.add(component);
        return panel;
    }
}
