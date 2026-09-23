package mdplayer.form.sys;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.BadLocationException;
import java.awt.Graphics;
import mdplayer.form.Layouts;

import mdplayer.Audio;
import mdplayer.Common;
import mdplayer.Setting;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;
import vavi.util.compat.Tuple3;

import static java.lang.System.getLogger;


class FormInfo extends JFrame {

    private static final Logger logger = getLogger(FormInfo.class.getName());

    public boolean isClosed = false;
    public int x = -1;
    public int y = -1;
    public final FormMain parent;
    public Setting setting = null;
    public List<Tuple3<Integer, Integer, String>> lyrics = null;
    public int lyricsIndex = 0;
    private Color culColor = new Color(192, 192, 255);

    static final Preferences prefs = Preferences.userNodeForPackage(FormInfo.class);

    public FormInfo(FormMain frm) {
        parent = frm;
        initializeComponent();
        // closed, not just hidden: HIDE_ON_CLOSE (JFrame's default) never fires windowClosed, so
        // isClosed stayed false and the player remembered a closed window as open
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        rtbLyrics.addFocusListener(RichTextBox1_GotFocus);
        update();
    }

    private final FocusListener RichTextBox1_GotFocus = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            lblComposer.requestFocus();
        }
    };

    public void update() {

        lblTitle.setText("");
        lblTitleJ.setText("");
        lblGame.setText("");
        lblGameJ.setText("");
        lblSystem.setText("");
        lblSystemJ.setText("");
        lblComposer.setText("");
        lblComposerJ.setText("");
        lblRelease.setText("");
        lblVGMBy.setText("");
        lblNotes.setText("");
        lblVersion.setText("");
        lblUsedChips.setText("");
        rtbLyrics.setText(null);

        Audio audio = Audio.getInstance();
        MetaData metaData = (audio.plugin != null && audio.plugin.driverVirtual != null) ? audio.plugin.driverVirtual.metaData : null;
        if (metaData == null) return;

        lblTitle.setText(metaData.getFirst(Tag.Title));
        lblTitleJ.setText(metaData.getFirst(Tag.TitleJ));
        lblGame.setText(metaData.getFirst(Tag.GameTitle));
        lblGameJ.setText(metaData.getFirst(Tag.GameTitleJ));
        lblSystem.setText(metaData.getFirst(Tag.GameSystem));
        lblSystemJ.setText(metaData.getFirst(Tag.GameSystemJ));
        lblComposer.setText(metaData.getFirst(Tag.Composer));
        lblComposerJ.setText(metaData.getFirst(Tag.ComposerJ));
        lblRelease.setText(metaData.getFirst(Tag.Converter));
        lblVGMBy.setText(metaData.getFirst(Tag.Maker));
        lblNotes.setText(metaData.getFirst(Tag.Note));
        lblVersion.setText(metaData.getFirst(Tag.SongObjVersion));
        lblUsedChips.setText(metaData.getFirst(Tag.Chip));

        if (metaData.getFirst(Tag.Lyric) == null) {
            timer.stop();
        } else {
            lyrics = new ArrayList<>();
            List<String> tmp = metaData.getAll(Tag.Lyric);
            for (String s : tmp) {
                String[] p = s.split(",");
                lyrics.add(new Tuple3<>(Integer.parseInt(p[0]), Integer.parseInt(p[1]), p[2]));
            }
            timer.start();
        }
    }

    public void screenInit() {
        lyricsIndex = 0;
        culColor = new Color(192, 192, 255);
    }

    protected boolean getShowWithoutActivation() {
        return true;
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            if (parent != null && parent.setting != null) {
                parent.setting.getLocation().setPInfo(getLocation());
            } else {
                prefs.putInt("x", getLocation().x);
                prefs.putInt("y", getLocation().y);
            }
            isClosed = true;
        }

        @Override
        public void windowOpened(WindowEvent e) {
            setLocation(new Point(x, y));
        }
    };

    private static void appendToTextPane(JTextPane tp, String msg, Color c) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
        int len = tp.getDocument().getLength();
        try {
            tp.getDocument().insertString(len, msg, aset);
        } catch (BadLocationException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    private void timer_Tick(ActionEvent ev) {
        if (lyrics == null || lyrics.isEmpty()) return;
        if (Audio.getInstance().plugin == null) return;

        long cnt = Audio.getInstance().plugin.getDriverCounter();

        try {
            if (cnt >= lyrics.get(lyricsIndex).getItem1()) {

                rtbLyrics.setText("");

                int ind = 0;
                StringBuilder currentChunk = new StringBuilder();
                while (ind < lyrics.get(lyricsIndex).getItem3().length()) {
                    char c = lyrics.get(lyricsIndex).getItem3().charAt(ind);
                    if (c == '\\') {
                        ind++;
                        c = lyrics.get(lyricsIndex).getItem3().charAt(ind);
                        switch (c) {
                            case '"':
                            case '\\':
                                break;
                            case 'c':
                                if (!currentChunk.isEmpty()) {
                                    appendToTextPane(rtbLyrics, currentChunk.toString(), culColor);
                                    currentChunk.setLength(0);
                                }
                                ind++;
                                String n = String.valueOf(lyrics.get(lyricsIndex).getItem3().charAt(ind++));
                                int r, g, b;
                                if (n.equals("s")) {
                                    r = 192;
                                    g = 192;
                                    b = 255;  // 192,192,255 system color
                                } else {
                                    n += lyrics.get(lyricsIndex).getItem3().charAt(ind++);
                                    r = Integer.parseInt(n, 16);
                                    n = String.valueOf(lyrics.get(lyricsIndex).getItem3().charAt(ind++));
                                    n += lyrics.get(lyricsIndex).getItem3().charAt(ind++);
                                    g = Integer.parseInt(n, 16);
                                    n = String.valueOf(lyrics.get(lyricsIndex).getItem3().charAt(ind++));
                                    n += lyrics.get(lyricsIndex).getItem3().charAt(ind++);
                                    b = Integer.parseInt(n, 16);
                                }
                                culColor = new Color(r, g, b);
                                continue;
                        }
                    }
                    currentChunk.append(c);
                    ind++;
                }
                if (!currentChunk.isEmpty()) {
                    appendToTextPane(rtbLyrics, currentChunk.toString(), culColor);
                }

                lyricsIndex++;

                if (lyricsIndex == lyrics.size()) {
                    timer.stop();
                }
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            try {
                rtbLyrics.setText("LYLIC PARSE ERROR");
            } catch (Exception ex) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }
    }

    private void initializeComponent() {
        this.lblTitle = new JLabel();
        this.lblTitleJ = new JLabel();
        this.lblGame = new JLabel();
        this.lblGameJ = new JLabel();
        this.lblSystem = new JLabel();
        this.lblComposer = new JLabel();
        this.lblRelease = new JLabel();
        this.lblVersion = new JLabel();
        this.lblVGMBy = new JLabel();
        this.lblNotes = new JLabel();
        this.lblUsedChips = new JLabel();
        this.lblSystemJ = new JLabel();
        this.lblComposerJ = new JLabel();
        this.timer = new Timer(10, this::timer_Tick);
        this.rtbLyrics = new JTextPane();

        //
        // lblTitle
        //
        this.lblTitle.setHorizontalAlignment(SwingConstants.LEFT);
        this.lblTitle.setVerticalAlignment(SwingConstants.TOP);
        this.lblTitle.setBackground(Color.black);
        this.lblTitle.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.lblTitle.setForeground(new Color(192, 192, 255));
        this.lblTitle.setLocation(new Point(80, 0));
        this.lblTitle.setName("lblTitle");
        this.lblTitle.setPreferredSize(new Dimension(240, 16));
        this.lblTitle.setText("01234567890123456789012345678901234567890123456789");
        //
        // lblTitleJ
        //
        this.lblTitleJ.setHorizontalAlignment(SwingConstants.LEFT);
        this.lblTitleJ.setVerticalAlignment(SwingConstants.TOP);
        this.lblTitleJ.setBackground(Color.black);
        this.lblTitleJ.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.lblTitleJ.setForeground(new Color(192, 192, 255));
        this.lblTitleJ.setLocation(new Point(80, 16));
        this.lblTitleJ.setName("lblTitleJ");
        this.lblTitleJ.setPreferredSize(new Dimension(240, 16));
        this.lblTitleJ.setText("01234567890123456789012345678901234567890123456789");
        //
        // lblGame
        //
        this.lblGame.setHorizontalAlignment(SwingConstants.LEFT);
        this.lblGame.setVerticalAlignment(SwingConstants.TOP);
        this.lblGame.setBackground(Color.black);
        this.lblGame.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.lblGame.setForeground(new Color(192, 192, 255));
        this.lblGame.setLocation(new Point(80, 32));
        this.lblGame.setName("lblGame");
        this.lblGame.setPreferredSize(new Dimension(240, 16));
        this.lblGame.setText("01234567890123456789012345678901234567890123456789");
        //
        // lblGameJ
        //
        this.lblGameJ.setHorizontalAlignment(SwingConstants.LEFT);
        this.lblGameJ.setVerticalAlignment(SwingConstants.TOP);
        this.lblGameJ.setBackground(Color.black);
        this.lblGameJ.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.lblGameJ.setForeground(new Color(192, 192, 255));
        this.lblGameJ.setLocation(new Point(80, 48));
        this.lblGameJ.setName("lblGameJ");
        this.lblGameJ.setPreferredSize(new Dimension(240, 16));
        this.lblGameJ.setText("01234567890123456789012345678901234567890123456789");
        //
        // lblSystem
        //
        this.lblSystem.setHorizontalAlignment(SwingConstants.LEFT);
        this.lblSystem.setVerticalAlignment(SwingConstants.TOP);
        this.lblSystem.setBackground(Color.black);
        this.lblSystem.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.lblSystem.setForeground(new Color(192, 192, 255));
        this.lblSystem.setLocation(new Point(80, 64));
        this.lblSystem.setName("lblSystem");
        this.lblSystem.setPreferredSize(new Dimension(240, 16));
        this.lblSystem.setText("01234567890123456789012345678901234567890123456789");
        //
        // lblComposer
        //
        this.lblComposer.setHorizontalAlignment(SwingConstants.LEFT);
        this.lblComposer.setVerticalAlignment(SwingConstants.TOP);
        this.lblComposer.setBackground(Color.black);
        this.lblComposer.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.lblComposer.setForeground(new Color(192, 192, 255));
        this.lblComposer.setLocation(new Point(80, 96));
        this.lblComposer.setName("lblComposer");
        this.lblComposer.setPreferredSize(new Dimension(240, 16));
        this.lblComposer.setText("01234567890123456789012345678901234567890123456789");
        //
        // lblRelease
        //
        this.lblRelease.setHorizontalAlignment(SwingConstants.LEFT);
        this.lblRelease.setVerticalAlignment(SwingConstants.TOP);
        this.lblRelease.setBackground(Color.black);
        this.lblRelease.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.lblRelease.setForeground(new Color(192, 192, 255));
        this.lblRelease.setLocation(new Point(80, 128));
        this.lblRelease.setName("lblRelease");
        this.lblRelease.setPreferredSize(new Dimension(240, 16));
        this.lblRelease.setText("01234567890123456789012345678901234567890123456789");
        //
        // lblVersion
        //
        this.lblVersion.setHorizontalAlignment(SwingConstants.LEFT);
        this.lblVersion.setVerticalAlignment(SwingConstants.TOP);
        this.lblVersion.setBackground(Color.black);
        this.lblVersion.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.lblVersion.setForeground(new Color(192, 192, 255));
        this.lblVersion.setLocation(new Point(80, 144));
        this.lblVersion.setName("lblVersion");
        this.lblVersion.setPreferredSize(new Dimension(240, 16));
        this.lblVersion.setText("01234567890123456789012345678901234567890123456789");
        //
        // lblVGMBy
        //
        this.lblVGMBy.setHorizontalAlignment(SwingConstants.LEFT);
        this.lblVGMBy.setVerticalAlignment(SwingConstants.TOP);
        this.lblVGMBy.setBackground(Color.black);
        this.lblVGMBy.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.lblVGMBy.setForeground(new Color(192, 192, 255));
        this.lblVGMBy.setLocation(new Point(80, 160));
        this.lblVGMBy.setName("lblVGMBy");
        this.lblVGMBy.setPreferredSize(new Dimension(240, 16));
        this.lblVGMBy.setText("01234567890123456789012345678901234567890123456789");
        //
        // lblNotes
        //
        this.lblNotes.setHorizontalAlignment(SwingConstants.LEFT);
        this.lblNotes.setVerticalAlignment(SwingConstants.TOP);
        this.lblNotes.setBackground(Color.black);
        this.lblNotes.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.lblNotes.setForeground(new Color(192, 192, 255));
        this.lblNotes.setLocation(new Point(80, 176));
        this.lblNotes.setName("lblNotes");
        this.lblNotes.setPreferredSize(new Dimension(240, 16));
        this.lblNotes.setText("01234567890123456789012345678901234567890123456789");
        //
        // lblUsedChips
        //
        this.lblUsedChips.setHorizontalAlignment(SwingConstants.LEFT);
        this.lblUsedChips.setVerticalAlignment(SwingConstants.TOP);
        this.lblUsedChips.setBackground(Color.black);
        this.lblUsedChips.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.lblUsedChips.setForeground(new Color(192, 192, 255));
        this.lblUsedChips.setLocation(new Point(80, 192));
        this.lblUsedChips.setName("lblUsedChips");
        this.lblUsedChips.setPreferredSize(new Dimension(240, 16));
        this.lblUsedChips.setText("01234567890123456789012345678901234567890123456789");
        //
        // lblSystemJ
        //
        this.lblSystemJ.setHorizontalAlignment(SwingConstants.LEFT);
        this.lblSystemJ.setVerticalAlignment(SwingConstants.TOP);
        this.lblSystemJ.setBackground(Color.black);
        this.lblSystemJ.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.lblSystemJ.setForeground(new Color(192, 192, 255));
        this.lblSystemJ.setLocation(new Point(80, 80));
        this.lblSystemJ.setName("lblSystemJ");
        this.lblSystemJ.setPreferredSize(new Dimension(240, 16));
        this.lblSystemJ.setText("01234567890123456789012345678901234567890123456789");
        //
        // lblComposerJ
        //
        this.lblComposerJ.setHorizontalAlignment(SwingConstants.LEFT);
        this.lblComposerJ.setVerticalAlignment(SwingConstants.TOP);
        this.lblComposerJ.setBackground(Color.black);
        this.lblComposerJ.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.lblComposerJ.setForeground(new Color(192, 192, 255));
        this.lblComposerJ.setLocation(new Point(80, 112));
        this.lblComposerJ.setName("lblComposerJ");
        this.lblComposerJ.setPreferredSize(new Dimension(240, 16));
        // this.lblComposerJ.TabIndex = 14
        this.lblComposerJ.setText("01234567890123456789012345678901234567890123456789");
        //
        // timer
        //
        this.timer.start();
        //
        // rtbLyrics
        //
        this.rtbLyrics.setBackground(Color.black);
        this.rtbLyrics.setBorder(null);
        this.rtbLyrics.setFont(new Font("Meyryo", Font.BOLD, 9));
        this.rtbLyrics.setForeground(new Color(192, 192, 255));
        this.rtbLyrics.setLocation(new Point(41, 209));
        this.rtbLyrics.setName("rtbLyrics");
        this.rtbLyrics.setEditable(false);
        this.rtbLyrics.setPreferredSize(new Dimension(283, 20));
        this.rtbLyrics.setText("");
        //
        // frmInfo
        //
        JPanel contentPane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (image != null) {
                    g.drawImage(image, 0, 0, this);
                }
            }
        };
        contentPane.setBackground(Color.BLACK);
        contentPane.setLayout(null);
        this.setContentPane(contentPane);

        this.image = Common.getImage("planeB");
        this.getContentPane().add(this.rtbLyrics);
        this.getContentPane().add(this.lblComposerJ);
        this.getContentPane().add(this.lblSystemJ);
        this.getContentPane().add(this.lblUsedChips);
        this.getContentPane().add(this.lblNotes);
        this.getContentPane().add(this.lblVGMBy);
        this.getContentPane().add(this.lblVersion);
        this.getContentPane().add(this.lblRelease);
        this.getContentPane().add(this.lblComposer);
        this.getContentPane().add(this.lblSystem);
        this.getContentPane().add(this.lblGameJ);
        this.getContentPane().add(this.lblGame);
        this.getContentPane().add(this.lblTitleJ);
        this.getContentPane().add(this.lblTitle);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmInfo");
        this.setTitle("Information");
        this.setResizable(false);
        this.addWindowListener(this.windowListener);

        Layouts.absolute(this.getContentPane());

        // Determine size dynamically from components and background image
        int width = 320;
        int height = 224;
        if (this.image != null) {
            width = this.image.getWidth();
            height = this.image.getHeight();
        }
        for (Component component : this.getContentPane().getComponents()) {
            width = Math.max(width, component.getX() + component.getWidth());
            height = Math.max(height, component.getY() + component.getHeight());
        }
        this.getContentPane().setPreferredSize(new Dimension(width, height));
        this.pack();
    }

    BufferedImage image;
    private JLabel lblTitle;
    private JLabel lblTitleJ;
    private JLabel lblGame;
    private JLabel lblGameJ;
    private JLabel lblSystem;
    private JLabel lblComposer;
    private JLabel lblRelease;
    private JLabel lblVersion;
    private JLabel lblVGMBy;
    private JLabel lblNotes;
    private JLabel lblUsedChips;
    private JLabel lblSystemJ;
    private JLabel lblComposerJ;
    private Timer timer;
    JTextPane rtbLyrics;
}
