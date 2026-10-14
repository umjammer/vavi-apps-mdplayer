package mdplayer.form.sys;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import mdplayer.Common;
import mdplayer.form.FormBase;


/**
 * What the player is logging, as it happens.
 * <p>
 * The original keeps a logger of its own and hands it a callback; here the logging is
 * {@code java.util.logging}, so this window is simply another {@link Handler} on the root logger,
 * and the level menu is that handler's level.
 */
class FormConsole extends FormBase {

    /** how much of the log is kept before the oldest of it is dropped */
    private static final int MAX_LINES = 5000;

    public boolean isClosed = false;

    private JTextArea tbLog;

    /** what puts the log into {@link #tbLog} */
    private final Handler handler = new Handler() {

        private final SimpleFormatter formatter = new SimpleFormatter();

        @Override
        public void publish(LogRecord record) {
            if (!isLoggable(record)) return;

            String message = formatter.formatMessage(record);
            SwingUtilities.invokeLater(() -> append("[%s] %s".formatted(record.getLevel(), message)));
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    public FormConsole(FormMain frm) {
        super(frm);
        initializeComponent();

        handler.setLevel(Level.INFO);
        Logger.getLogger("").addHandler(handler);
    }

    private void append(String line) {
        tbLog.append(line + "\n");

        if (tbLog.getLineCount() > MAX_LINES) {
            try {
                tbLog.replaceRange("", 0, tbLog.getLineEndOffset(tbLog.getLineCount() - MAX_LINES));
            } catch (Exception ignored) {
            }
        }

        tbLog.setCaretPosition(tbLog.getDocument().getLength());
    }

    private void initializeComponent() {
        this.tbLog = new JTextArea();
        this.tbLog.setEditable(false);
        this.tbLog.setName("tbLog");
        this.tbLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JMenuBar menuBar = new JMenuBar();
        JMenu levels = new JMenu("Level");
        ButtonGroup group = new ButtonGroup();
        for (Level level : new Level[] {Level.FINEST, Level.FINE, Level.INFO, Level.WARNING, Level.SEVERE}) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(level.getName());
            item.setSelected(level == Level.INFO);
            item.addActionListener(e -> handler.setLevel(level));
            group.add(item);
            levels.add(item);
        }
        menuBar.add(levels);

        JMenuItem clear = new JMenuItem("Clear");
        clear.addActionListener(_ -> tbLog.setText(""));
        menuBar.add(clear);
        this.setJMenuBar(menuBar);

        this.getContentPane().add(new JScrollPane(this.tbLog), BorderLayout.CENTER);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmConsole");
        this.setTitle("Console");
        this.setSize(new Dimension(600, 400));
        this.addWindowListener(this.windowListener);
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            // stop logging into a window that is not there any more
            Logger.getLogger("").removeHandler(handler);
            isClosed = true;
        }
    };
}
