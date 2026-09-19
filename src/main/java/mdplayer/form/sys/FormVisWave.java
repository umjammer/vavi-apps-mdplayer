package mdplayer.form.sys;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.Timer;

import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.form.FormBase;
import vavi.util.SplitRadixFft;
import vavi.util.compat.Tuple;

import static mdplayer.form.sys.FormMain.empty;


public class FormVisWave extends FormBase {

    private static final Logger logger = System.getLogger(FormVisWave.class.getName());

    public int x = -1;
    public int y = -1;

    private final short[][] buf = {new short[2048], new short[2048]};
    private Graphics2D g;
    private final BufferedImage bmp;
    private int dispType = 1;
    private double dispHeight = 1.0;
    private boolean fft = false;

    private static final Preferences prefs = Preferences.userNodeForPackage(FormVisWave.class);

    public Setting setting = Setting.load();

    /** where in {@link #buf} the next sample goes */
    private int writeIndex;

    public FormVisWave() {
        bmp = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        // the timer starts drawing at once, so it needs somewhere to draw
        g = (Graphics2D) bmp.getGraphics();
        initializeComponent();
    }

    public void init(FormMain frm) {
        parent = frm;
    }

    public void open() {
        if (this.isVisible()) {
            this.requestFocus();
            return;
        }

        if (setting.getLocation().getPosVisWave().equals(empty)) {
            this.x = this.getLocation().x;
            this.y = this.getLocation().y + 264;
        } else {
            this.x = setting.getLocation().getPosVisWave().x;
            this.y = setting.getLocation().getPosVisWave().y;
        }

        this.setVisible(true);

        FormMain.checkAndSetForm(this);
    }

    public void close() {
        if (this.isVisible()) return;

        setting.getLocation().setPosVisWave(this.getLocation());
        setting.getLocation().setOpenVisWave(true);

        this.setVisible(false);
    }

    /**
     * Takes one rendered sample, as the driver produces it. This is what there is to draw — the
     * wave is fed to us rather than copied out of the driver, so nothing here reaches into it.
     *
     * @see mdplayer.driver.BaseDriver#fireEventHappened
     */
    public void push(short left, short right) {
        buf[0][writeIndex] = left;
        buf[1][writeIndex] = right;
        writeIndex = (writeIndex + 1) % buf[0].length;
    }

    private void timer1_Tick(ActionEvent ev) {

        g.setColor(Color.black);
        g.fillRect(0, 0, bmp.getWidth(), bmp.getHeight());

        if (fft) {
            float[] a = convertTo(buf[0]);
            processFFT(a);
            buf[0] = convertTo(a);

            a = convertTo(buf[1]);
            processFFT(a);
            buf[1] = convertTo2(a);
        }

        for (int ch = 0; ch < 2; ch++) {
            if (dispType <= 1) {
                int hPos = bmp.getHeight() / 4 + ch * bmp.getHeight() / 2;
                int ox = 0;
                int oy = hPos;

                for (int i = 0; i < 2048; i++) {
                    int x = (i * bmp.getWidth()) / 2048;
                    int y = (int) (buf[ch][i] * dispHeight) * bmp.getHeight() / 65536 + hPos;
                    g.setColor(new Color(0x46, 0x82, 0xb4)); // SteelBlue
                    g.drawLine(ox, oy, x, y);
                    ox = x;
                    oy = y;
                }
            } else {
                int hPos = bmp.getHeight() / 4 + ch * bmp.getHeight() / 2;
                for (int i = 0; i < 2048; i++) {
                    int x = (i * bmp.getWidth()) / 2048;
                    int y = (int) (buf[ch][i] * dispHeight) * bmp.getHeight() / 65536 + hPos;
                    g.setColor(new Color(0xF0, 0xE6, 0x8C)); // Khaki
                    g.drawLine(x, hPos, x, y);
                }
            }
        }

        // Scale bmp to fit the label and update the display
        int w = toolStripContainer1.getWidth();
        int h = toolStripContainer1.getHeight();
        if (w > 0 && h > 0) {
            Image scaled = bmp.getScaledInstance(w, h, Image.SCALE_FAST);
            toolStripContainer1.setIcon(new ImageIcon(scaled));
        } else {
            toolStripContainer1.setIcon(new ImageIcon(bmp));
        }
        toolStripContainer1.repaint();
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            if (e.getNewState() == WindowEvent.WINDOW_OPENED) {
                parent.setting.getLocation().setPosVisWave(getLocation());
            } else {
                parent.setting.getLocation().setPosVisWave(new Point(prefs.getInt("x", 0), prefs.getInt("y", 0)));
            }
        }

        @Override
        public void windowOpened(WindowEvent e) {
            g = (Graphics2D) bmp.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
//        pictureBox1.SizeMode = BufferedImageSizeMode.StretchImage;
        }

        @Override
        public void windowActivated(WindowEvent e) {
            setLocation(new Point(x, y));
        }
    };

    protected boolean getShowWithoutActivation() {
        return true;
    }

    private void tsbDispType2_Click(ActionEvent ev) {
        dispType = 2;
    }

    private void tsbDispType1_Click(ActionEvent ev) {
        dispType = 1;
    }

    private void tsbHeight3_Click(ActionEvent ev) {
        dispHeight = 3;
    }

    private void tsbHeight2_Click(ActionEvent ev) {
        dispHeight = 1.0;
    }

    private void tsbHeight1_Click(ActionEvent ev) {
        dispHeight = 0.3;
    }

    private float[] convertTo(short[] src) {
        for (int i = 0; i < src.length; i++) {
            destF[i] = src[i] / 32768.0f;
        }
        return destF;
    }

    private short[] convertTo(float[] src) {
        for (int i = 0; i < src.length / 2; i++) {
            destS[i * 2] = (short) (Math.clamp(-src[i] * 150.0f * 32768.0f * 0.6, Short.MIN_VALUE, Short.MAX_VALUE));
            destS[i * 2 + 1] = (short) (Math.clamp(-src[i] * 150.0f * 32768.0f * 0.6, Short.MIN_VALUE, Short.MAX_VALUE));
        }
        return destS;
    }

    private short[] convertTo2(float[] src) {
        for (int i = 0; i < src.length / 2; i++) {
            destS2[i * 2] = (short) (Math.clamp(-src[i] * 150.0f * 32768.0f * 0.6, Short.MIN_VALUE, Short.MAX_VALUE));
            destS2[i * 2 + 1] = (short) (Math.clamp(-src[i] * 150.0f * 32768.0f * 0.6, Short.MIN_VALUE, Short.MAX_VALUE));
        }
        return destS2;
    }

    private final float[] destF = new float[2048];
    private final short[] destS = new short[2048];
    private final short[] destS2 = new short[2048];
    private final Tuple<Float, Float>[] fftsample; // Complex

    {
        fftsample = new Tuple[2048];
        for (int i = 0; i < fftsample.length; i++) {
            fftsample[i] = new Tuple<>(0f, 0f);
        }
    }

    // @see "https://raptorcafeterrace.hatenablog.com/entry/2017/05/08/191704"
    private void processFFT(float[] sdata) {
        FFT fft = new FFT();

        for (int i = 0; i < sdata.length; i++) {
            fftsample[i].setItem1((float) (sdata[i] * fft.win(i, sdata.length)));
            fftsample[i].setItem2(0f);
        }

        fft.fft(true, (int) (Math.log(sdata.length) / Math.log(2)), fftsample);

        for (int i = 0; i < sdata.length; i++) {
            sdata[i] = (float) Math.sqrt(fftsample[i].getItem1() * fftsample[i].getItem1() + fftsample[i].getItem2() * fftsample[i].getItem2()); // Power Spectrum
        }
    }

    static class FFT {

        private static final int M = 15;

        private final double[] fact = new double[M + 1];

        private static final double aa = 96;

        private double iza;

        private static double alpha(double a) {
            if (a <= 21) {
                return 0;
            }
            if (a <= 50) {
                return 0.5842 * Math.pow(a - 21, 0.4) + 0.07886 * (a - 21);
            }
            return 0.1102 * (a - 8.7);
        }

        private double win(double n, int N) {
            return izero(alpha(aa) * Math.sqrt(1 - 4 * n * n / ((N - 1) * (N - 1)))) / iza;
        }

        private double izero(double x) {
            double ret = 1;

            for (int m = 1; m <= M; m++) {
                double t = Math.pow(x / 2, m) / fact[m];
                ret += t * t;
            }

            return ret;
        }

        private static void rfft(int n, int isign, double[] x) {
            int ipsize = 0, wsize = 0;
            int[] ip = null;
            double[] w = null;
            int newipsize, newwsize;

            if (n == 0) {
                ip = null;
                ipsize = 0;
                w = null;
                wsize = 0;
                return;
            }

            newipsize = (int) (2 + Math.sqrt(n / 2.));
            if (newipsize > ipsize) {
                ipsize = newipsize;
                ip = new int[ipsize];
                ip[0] = 0;
            }

            newwsize = n / 2;
            if (newwsize > wsize) {
                wsize = newwsize;
                w = new double[wsize];
            }

            SplitRadixFft.rdft(n, isign, x, ip, w);
        }

        void fft(boolean b, int i, Tuple<Float, Float>[] fftsample) {
            // TODO
        }
    }

    private void tsbFFT_Click(ActionEvent ev) {
        fft = tsbFFT.isSelected();
    }

    private void initializeComponent() {
        this.pictureBox1 = new BufferedImage(224, 176, BufferedImage.TYPE_INT_ARGB);
        this.timer1 = new Timer(10, this::timer1_Tick);
        this.toolStripContainer1 = new JLabel();
        this.toolStrip1 = new JToolBar();
        this.tsbHeight1 = new JButton();
        this.tsbHeight2 = new JButton();
        this.tsbHeight3 = new JButton();
        this.tsbDispType1 = new JButton();
        this.tsbDispType2 = new JButton();
        this.tsbFFT = new JToggleButton();

        //
        // timer1
        //
        this.timer1.start();
        //
        // toolStripContainer1 (ContentPanel — displays the waveform image)
        //
        this.toolStripContainer1.setIcon(new ImageIcon(this.pictureBox1));
        this.toolStripContainer1.setPreferredSize(new Dimension(261, 226));
        this.toolStripContainer1.setName("toolStripContainer1");
        //
        // toolStrip1 (TopToolStripPanel — the toolbar with buttons)
        //
        this.toolStrip1.setFloatable(false);
        this.toolStrip1.setName("toolStrip1");
        //
        // tsbHeight1
        //
        this.tsbHeight1.setIcon(new ImageIcon(Common.getImage("vHeight1")));
        this.tsbHeight1.setName("tsbHeight1");
        this.tsbHeight1.setToolTipText("Height x 0.3");
        this.tsbHeight1.setFocusable(false);
        this.tsbHeight1.addActionListener(this::tsbHeight1_Click);
        //
        // tsbHeight2
        //
        this.tsbHeight2.setIcon(new ImageIcon(Common.getImage("vHeight2")));
        this.tsbHeight2.setName("tsbHeight2");
        this.tsbHeight2.setToolTipText("Height x 1.0");
        this.tsbHeight2.setFocusable(false);
        this.tsbHeight2.addActionListener(this::tsbHeight2_Click);
        //
        // tsbHeight3
        //
        this.tsbHeight3.setIcon(new ImageIcon(Common.getImage("vHeight3")));
        this.tsbHeight3.setName("tsbHeight3");
        this.tsbHeight3.setToolTipText("Height x 3.0");
        this.tsbHeight3.setFocusable(false);
        this.tsbHeight3.addActionListener(this::tsbHeight3_Click);
        //
        // tsbDispType1
        //
        this.tsbDispType1.setIcon(new ImageIcon(Common.getImage("vType1")));
        this.tsbDispType1.setName("tsbDispType1");
        this.tsbDispType1.setToolTipText("type 1");
        this.tsbDispType1.setFocusable(false);
        this.tsbDispType1.addActionListener(this::tsbDispType1_Click);
        //
        // tsbDispType2
        //
        this.tsbDispType2.setIcon(new ImageIcon(Common.getImage("vType2")));
        this.tsbDispType2.setName("tsbDispType2");
        this.tsbDispType2.setToolTipText("type 2");
        this.tsbDispType2.setFocusable(false);
        this.tsbDispType2.addActionListener(this::tsbDispType2_Click);
        //
        // tsbFFT (toggle button, matching C# CheckOnClick)
        //
        this.tsbFFT.setIcon(new ImageIcon(Common.getImage("vType3")));
        this.tsbFFT.setName("tsbFFT");
        this.tsbFFT.setToolTipText("FFT");
        this.tsbFFT.setFocusable(false);
        this.tsbFFT.addActionListener(this::tsbFFT_Click);
        //
        // toolStrip1 — add buttons
        //
        this.toolStrip1.add(this.tsbHeight1);
        this.toolStrip1.add(this.tsbHeight2);
        this.toolStrip1.add(this.tsbHeight3);
        this.toolStrip1.addSeparator();
        this.toolStrip1.add(this.tsbDispType1);
        this.toolStrip1.add(this.tsbDispType2);
        this.toolStrip1.add(this.tsbFFT);
        //
        // this
        //
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(this.toolStrip1, BorderLayout.NORTH);
        this.getContentPane().add(this.toolStripContainer1, BorderLayout.CENTER);
        this.setPreferredSize(new Dimension(261, 251));
        this.pack();
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("this");
        // no setOpacity(): Swing only allows a translucent frame if it is undecorated, and this one
        // has a title bar to drag it by
        this.setTitle("Visualizer");
        this.addWindowListener(this.windowListener);
    }

    private BufferedImage pictureBox1;
    private Timer timer1;
    private JLabel toolStripContainer1;
    private JToolBar toolStrip1;
    private JButton tsbHeight1;
    private JButton tsbHeight2;
    private JButton tsbHeight3;
    private JButton tsbDispType1;
    private JButton tsbDispType2;
    private JToggleButton tsbFFT;
}
