package mdplayer.form.sys.setting;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.form.SettingTab;

import static java.lang.System.getLogger;


/** the "About" page of the settings dialog, split out of the original FormSetting */
public class SettingAboutPanel extends SettingTab {

    @Override
    public int order() {
        return 160;
    }

    private static final Logger logger = getLogger(SettingAboutPanel.class.getName());

    private static final ResourceBundle rb2 = ResourceBundle.getBundle("mdplayer/properties/resources");
    private static final ResourceBundle rb = ResourceBundle.getBundle("mdplayer/form/sys/frmSetting");

    private final JComponent tableLayoutPanel;
    private final JLabel logoBufferedImage;
    private final JLabel labelProductName;
    private final JLabel labelVersion;
    private final JLabel labelCopyright;
    private final JLabel labelCompanyName;
    private final JTextArea textBoxDescription;
    private final JLabel llOpenGithub;

    public SettingAboutPanel() {
        this.tableLayoutPanel = new JComponent() {};
        this.logoBufferedImage = new JLabel();
        this.labelProductName = new JLabel();
        this.labelVersion = new JLabel();
        this.labelCopyright = new JLabel();
        this.labelCompanyName = new JLabel();
        this.textBoxDescription = new JTextArea();
        this.llOpenGithub = new JLabel();

        //
        // labelCompanyName
        //
        this.labelCompanyName.setName("labelCompanyName");
        //
        // labelCopyright
        //
        this.labelCopyright.setName("labelCopyright");
        //
        // labelProductName
        //
        this.labelProductName.setName("labelProductName");
        //
        // labelVersion
        //
        this.labelVersion.setName("labelVersion");
        //
        // llOpenGithub
        //
        this.llOpenGithub.setName("llOpenGithub");
        this.llOpenGithub.addMouseListener(this.llOpenGithub_LinkClicked);
        this.llOpenGithub.setForeground(Color.BLUE);
        this.llOpenGithub.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        //
        // logoBufferedImage
        //
        this.logoBufferedImage.setIcon(new ImageIcon(Common.getImage("FeliAndMD2_bmp")));
        this.logoBufferedImage.setName("logoPictureBox");
        this.logoBufferedImage.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12)); // 12px right margin
        //
        // textBoxDescription
        //
        this.textBoxDescription.setEditable(false);
        this.textBoxDescription.setLineWrap(true);
        this.textBoxDescription.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(this.textBoxDescription);
        scrollPane.setName("textBoxDescription");
        //
        // rightPanel
        //
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 8, 0); // bottom margin of 8px

        rightPanel.add(this.labelProductName, gbc);

        gbc.gridy++;
        rightPanel.add(this.labelVersion, gbc);

        gbc.gridy++;
        rightPanel.add(this.labelCopyright, gbc);

        gbc.gridy++;
        rightPanel.add(this.labelCompanyName, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        rightPanel.add(scrollPane, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(4, 0, 0, 0); // top margin of 4px
        rightPanel.add(this.llOpenGithub, gbc);

        //
        // tableLayoutPanel
        //
        this.tableLayoutPanel.setLayout(new BorderLayout());
        this.tableLayoutPanel.add(this.logoBufferedImage, BorderLayout.WEST);
        this.tableLayoutPanel.add(rightPanel, BorderLayout.CENTER);
        this.tableLayoutPanel.setName("tableLayoutPanel");
        //
        // tpAbout
        //
        this.setName("tpAbout");
        // layout
        this.setLayout(new BorderLayout());
        this.add(this.tableLayoutPanel);
    }

    @Override
    public void load(Setting setting) {
        this.labelProductName.setText(getAssemblyProduct());
        this.labelVersion.setText("version %s".formatted(getAssemblyVersion()));
        this.labelCopyright.setText(getAssemblyCopyright());
        this.labelCompanyName.setText(getAssemblyCompany());
        this.textBoxDescription.setText(getAssemblyDescription());
        this.textBoxDescription.setCaretPosition(0);
        String linkText = "Open latest version page of Github.";
        try {
            linkText = rb.getString("llOpenGithub.Text");
        } catch (Exception ignored) {
        }
        this.llOpenGithub.setText("<html><u>" + linkText + "</u></html>");
    }

    @Override
    public void apply(Setting setting) {

    }

    private final MouseListener llOpenGithub_LinkClicked = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            try {
                Desktop.getDesktop().browse(URI.create("https://github.com/umjammer/vavi-apps-mdplayer/releases/latest"));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    };

    public String getAssemblyTitle() {
        return "vavi-apps-mdplayer";
    }

    public String getAssemblyVersion() {
        return Common.version;
    }

    public String getAssemblyDescription() {
        return loadDescriptionFromRawFile();
    }

    private String loadDescriptionFromRawFile() {
        Locale locale = Locale.getDefault();
        String baseName = "/mdplayer/properties/resources";
        String ext = ".properties";

        String[] candidates = {
            baseName + "_" + locale.getLanguage() + "_" + locale.getCountry() + ext,
            baseName + "_" + locale.getLanguage() + ext,
            baseName + ext
        };

        for (String name : candidates) {
            try (InputStream is = SettingAboutPanel.class.getResourceAsStream(name)) {
                if (is == null) continue;

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    boolean found = false;
                    while ((line = reader.readLine()) != null) {
                        if (!found) {
                            if (line.startsWith("cntDescription=")) {
                                found = true;
                                String val = line.substring("cntDescription=".length());
                                if (val.endsWith("\\")) {
                                    sb.append(val, 0, val.length() - 1).append("\n");
                                } else {
                                    sb.append(val);
                                    break;
                                }
                            }
                        } else {
                            String val = line;
                            if (val.endsWith("\\")) {
                                sb.append(val, 0, val.length() - 1).append("\n");
                            } else {
                                sb.append(val);
                                break;
                            }
                        }
                    }
                    if (found) {
                        return unescape(sb.toString());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return rb2.getString("cntDescription");
    }

    private String unescape(String str) {
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c == '\\') {
                if (i + 1 < len) {
                    char next = str.charAt(i + 1);
                    if (next == 'u') {
                        if (i + 5 < len) {
                            try {
                                int code = Integer.parseInt(str.substring(i + 2, i + 6), 16);
                                sb.append((char) code);
                                i += 5;
                                continue;
                            } catch (NumberFormatException ignored) {}
                        }
                    } else if (next == 'n') {
                        sb.append('\n');
                        i++;
                        continue;
                    } else if (next == 'r') {
                        sb.append('\r');
                        i++;
                        continue;
                    } else if (next == 't') {
                        sb.append('\t');
                        i++;
                        continue;
                    } else if (next == '\\') {
                        sb.append('\\');
                        i++;
                        continue;
                    } else if (next == ' ') {
                        sb.append(' ');
                        i++;
                        continue;
                    }
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public String getAssemblyProduct() {
        return "vavi-apps-mdplayer";
    }

    public String getAssemblyCopyright() {
        return "Copyright (C) 2018-2023 kuma4649, umjammer";
    }

    public String getAssemblyCompany() {
        return "kuma4649, umjammer";
    }
}
