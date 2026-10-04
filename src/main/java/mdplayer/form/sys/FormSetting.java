package mdplayer.form.sys;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.form.Grid;
import mdplayer.form.Layouts;
import mdplayer.form.SettingTab;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.setting.SettingAboutPanel;
import mdplayer.form.sys.setting.SettingBalancePanel;
import mdplayer.form.sys.setting.SettingKeyBoardPanel;
import mdplayer.form.sys.setting.SettingModulePanel;
import mdplayer.form.sys.setting.SettingOmakePanel;
import mdplayer.form.sys.setting.SettingOtherPanel;
import mdplayer.form.sys.setting.SettingOutputPanel;
import mdplayer.form.sys.setting.SettingPMDPanel;
import mdplayer.form.sys.setting.SettingPlayListPanel;

import static java.lang.System.getLogger;


public class FormSetting extends JDialog {

    private static final Logger logger = getLogger(FormSetting.class.getName());

    /** the designer's captions, converted from frmSetting.resx */
    private static final ResourceBundle resources = ResourceBundle.getBundle("mdplayer/form/sys/frmSetting", Locale.getDefault());

    public static final boolean asioSupported = true;
    public static final boolean wasapiSupported = true;
    public final Setting setting;

    private int dialogResult;

    /** the pages that have been split out of this class, generic ones and the providers', in tab order */
    private final List<SettingTab> settingTabs = new ArrayList<>();

    private SettingTab register(SettingTab tab) {
        tab.setSetting(setting);
        settingTabs.add(tab);
        return tab;
    }

    int showDialog() {
        setVisible(true);
        return dialogResult;
    }

    public FormSetting(Setting setting) {
        setModal(true);
        this.setting = setting.clone();

        initializeComponent();

        init();
    }

    private void init() {

        for (SettingTab tab : settingTabs) {
            tab.load(setting);
        }
    }

    public static void btnASIOControlPanel_Click(ActionEvent ev) {
        try {
//            try (AsioOut asio = new AsioOut(cmbAsioDevice.getSelectedItem().toString())) {
//                asio.ShowControlPanel();
//            }
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }
    }

    private void btnOK_Click(ActionEvent ev) {
        for (SettingTab tab : settingTabs) {
            if (!tab.check()) return;
        }

        for (SettingTab tab : settingTabs) {
            tab.apply(setting);
        }

        this.dialogResult = JFileChooser.APPROVE_OPTION;
        this.setVisible(false);
    }

    public static int parseMidiCtrl(String text) {
        if (text == null || text.trim().isEmpty()) return -1;
        try {
            return Math.clamp(Integer.parseInt(text.trim()), 0, 127);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static int parseIntSafe(String text, int min, int max, int defaultValue) {
        if (text == null || text.trim().isEmpty()) return defaultValue;
        try {
            return Math.clamp(Integer.parseInt(text.trim()), min, max);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int parseHexSafe(String text, int defaultValue) {
        if (text == null || text.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(text.trim(), 16);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static void btnOpenSettingFolder_Click(ActionEvent ev) {
        try {
            Path fullPath = Common.settingFilePath;
            new ProcessBuilder(fullPath.toString()).start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            if (FormMain.keyHookMeth != null /* this::keyHookMeth */) { // TODO
                FormMain.keyHookMeth = null;
            }
        }

        @Override
        public void windowOpened(WindowEvent e) {
        }
    };

    private void initializeComponent() {
        this.btnOK = new JButton();
        this.btnCancel = new JButton();
        this.tcSetting = new JTabbedPane();
        this.JListTextBoxColumn1 = new JTextField();
        this.clmIsVST = new JCheckBox();
        this.clmFileName = new JTextArea();
        this.JListTextBoxColumn2 = new JTextArea();
        this.clmType = new JComboBox<>();
        this.clmBeforeSend = new JComboBox<>();
        this.JListTextBoxColumn3 = new JTextArea();
        this.JListTextBoxColumn4 = new JTextArea();
        this.clmID = new JTextArea();
        this.clmDeviceName = new JTextArea();
        this.clmManufacturer = new JTextArea();
        this.clmSpacer = new JTextArea();

        //
        // btnOK
        //
        this.btnOK.setName("btnOK");
        this.btnOK.addActionListener(this::btnOK_Click);
        //
        // btnCancel
        //
        this.btnCancel.addActionListener(e -> {
            dialogResult = JFileChooser.CANCEL_OPTION;
            setVisible(false);
        });
        this.btnCancel.setName("btnCancel");
        //
        // tcSetting
        //
        register(new SettingOutputPanel());
        register(new SettingModulePanel());
        register(new SettingPMDPanel());
        register(new SettingKeyBoardPanel(this.btnOK));
        register(new SettingBalancePanel());
        register(new SettingPlayListPanel());
        register(new SettingOtherPanel());
        register(new SettingOmakePanel());
        register(new SettingAboutPanel());
        for (ViewProvider p : ViewProvider.providers()) {
            for (SettingTab tab : p.settingTabs()) {
                register(tab);
            }
        }
        settingTabs.sort(Comparator.comparingInt(SettingTab::order));
        for (SettingTab tab : settingTabs) {
            this.tcSetting.add(tab);
        }
        this.tcSetting.setName("tcSetting");
        this.tcSetting.setSelectedIndex(0);
        //
        // JListTextBoxColumn1
        //
        this.JListTextBoxColumn1.setEditable(false);
        this.JListTextBoxColumn1.setName("JListTextBoxColumn1");
        //
        // clmIsVST
        //
        this.clmIsVST.setName("clmIsVST");
        //
        // clmFileName
        //
        this.clmFileName.setName("clmFileName");
        //
        // JListTextBoxColumn2
        //
        this.JListTextBoxColumn2.setName("JListTextBoxColumn2");
        this.JListTextBoxColumn2.setEditable(true);
        //
        // clmType
        //
        this.clmType.setModel(new DefaultComboBoxModel<>(new String[] {
                "GM",
                "XG",
                "GS",
                "LA",
                "GS(SC-55_1)",
                "GS(SC-55_2)"}));
        this.clmType.setName("clmType");
        //
        // clmBeforeSend
        //
        this.clmBeforeSend.setModel(new DefaultComboBoxModel<>(new String[] {
                "None",
                "GM Reset",
                "XG Reset",
                "GS Reset",
                "Custom"}));
        this.clmBeforeSend.setName("clmBeforeSend");
        //
        // JListTextBoxColumn3
        //
        this.JListTextBoxColumn3.setName("JListTextBoxColumn3");
        this.JListTextBoxColumn3.setEditable(true);
        //
        // JListTextBoxColumn4
        //
        this.JListTextBoxColumn4.setName("JListTextBoxColumn4");
        this.JListTextBoxColumn4.setEditable(true);
        //
        // clmID
        //
        this.clmID.setName("clmID");
        this.clmID.setEditable(true);
        //
        // clmDeviceName
        //
        this.clmDeviceName.setName("clmDeviceName");
        this.clmDeviceName.setEditable(true);
        //
        // clmManufacturer
        //
        this.clmManufacturer.setName("clmManufacturer");
        this.clmManufacturer.setEditable(true);
        //
        // clmSpacer
        //
        this.clmSpacer.setName("clmSpacer");
        this.clmSpacer.setEditable(true);
        //
        // frmSetting
        //
        this.getContentPane().add(this.tcSetting, BorderLayout.CENTER);
        this.getContentPane().add(Grid.trailing(this.btnOK, this.btnCancel), BorderLayout.SOUTH);
        this.setTitle("frmSetting");
        this.addWindowListener(this.windowListener);
        // the layout is in the code now, the .resx only gives the captions
        Layouts.labels(this.getContentPane(), resources);
        this.pack();
        this.setMinimumSize(this.getSize());
    }

    private JButton btnOK;
    private JButton btnCancel;
    private JTabbedPane tcSetting;
    private JTextArea clmID;
    private JTextArea clmDeviceName;
    private JTextArea clmManufacturer;
    private JTextArea clmSpacer;
    private JTextField JListTextBoxColumn1;
    private JCheckBox clmIsVST;
    private JTextArea clmFileName;
    private JTextArea JListTextBoxColumn2;
    private JComboBox<String> clmType;
    private JComboBox<String> clmBeforeSend;
    private JTextArea JListTextBoxColumn3;
    private JTextArea JListTextBoxColumn4;
    //private ucSettingInstruments ucSettingInstruments1;

    static class BindData implements PropertyChangeListener {

        PropertyChangeListener propertyChanged;

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (propertyChanged != null) {
                propertyChanged.propertyChange(new PropertyChangeEvent(this, "Value", null, null));
            }
        }

        int _value;

        public int getValue() {
            return _value;
        }

        public void setValue(int value) {
            if (value != _value) {
                _value = value;
                propertyChange(new PropertyChangeEvent(this, "Value", _value, value));
            }
        }
    }

    public static class Manufacturers {

        static final Properties props = new Properties();

        static {
            try {
                props.load(Manufacturers.class.getResourceAsStream("manufacturers.properties"));
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        public static String byId(int id) {
            return props.keySet().stream().filter(k -> k.equals(String.valueOf(id))).map(props::get).findFirst().orElseThrow().toString();
        }

        public static int byManufacture(String manufacture) {
            return props.entrySet().stream().filter(e -> e.getValue().equals(manufacture)).map(e -> Integer.decode((String) props.get(e.getKey()))).findFirst().orElseThrow();
        }
    }
}
