/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.visualizer.fmdsp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.Timer;

import static vavi.sound.visualizer.fmdsp.FmDspSprites.*;


/**
 * Swing port of the {@code fmdsp} sound visualizer used by 98fmplayer.
 * <p>
 * Unlike a naive vector redraw, this renderer reproduces the original
 * <em>dot by dot</em>: it keeps an 8-bit palettized VRAM buffer
 * ({@code byte[640*400]}) and blits the very same pixel sprites and 5x6 / 6x8
 * bitmap fonts the C code uses ({@link FmDspSprites}), running the exact
 * {@code fmdsp_vram_init} / {@code fmdsp_track_init_10} / {@code fmdsp_update_10}
 * logic for the {@code FMDSP_DISPSTYLE_ORIGINAL} layout. The finished VRAM is
 * palette-mapped to an {@link BufferedImage} and scaled with nearest-neighbour,
 * so the on-screen result is pixel-identical to the original 640x400 FMDSP
 * screen.
 *
 * <h2>Data</h2>
 * The renderer pulls everything through {@link FmDspDataSource} each frame and
 * never touches the chip emulator. The original reads the OPNA itself for the
 * "track info" panels of {@link RightMode#TRACK_INFO}; here those come through
 * {@link TrackDetailSource} instead, so that they are neither OPNA specific nor
 * the renderer's business to fetch.
 *
 * <h2>Beyond the C source</h2>
 * One section is drawn here that 98fmplayer's fmdsp never had: the frequency fluctuation display
 * of the original PC-98 FMDSP, section 9 of its FMDSP.DOC, which lives in {@code FMDSP.COM} alone.
 * See {@link #freqWave}.
 *
 * <h2>Threading</h2>
 * VRAM is rebuilt on the EDT each repaint; guard cross-thread data-source
 * mutation on your side.
 */
public class FmDspVisualizer extends JComponent {

    // ---------- canvas ----------
    public static final int PC98_W = 640;
    public static final int PC98_H = 400;
    public static final int CANVAS_W = PC98_W;
    public static final int CANVAS_H = PC98_H;

    private static final int FMDSP_LEVEL_COUNT = 19;
    private static final int FFTDISPLEN = 70;

    // ---------- layout constants (verbatim from fmdsp_sprites.h) ----------
    private static final int TRACK_H = 32;
    private static final int TINFO_X = 47;
    private static final int TDETAIL_X = 67;
    private static final int TDETAIL_KN_V_X = TDETAIL_X + 13;
    private static final int TDETAIL_TN_X = TDETAIL_KN_V_X + 28;
    private static final int TDETAIL_TN_V_X = TDETAIL_TN_X + 13;
    private static final int TDETAIL_VL_X = TDETAIL_TN_V_X + 20;
    private static final int TDETAIL_VL_C_X = TDETAIL_VL_X + 9;
    private static final int TDETAIL_VL_V_X = TDETAIL_VL_X + 12;
    private static final int TDETAIL_GT_X = TDETAIL_VL_V_X + 19;
    private static final int TDETAIL_GT_V_X = TDETAIL_GT_X + 13;
    private static final int TDETAIL_DT_X = TDETAIL_GT_V_X + 23;
    private static final int TDETAIL_DT_S_X = TDETAIL_DT_X + 13;
    private static final int TDETAIL_DT_V_X = TDETAIL_DT_S_X + 4;
    private static final int TDETAIL_M_X = 249;
    private static final int TDETAIL_M_V_X = TDETAIL_M_X + 8;
    private static final int NUM_X = 31;
    private static final int NUM_W = 8;
    private static final int NUM_H = 11;
    private static final int KEY_X = 7;
    private static final int KEY_Y = 14;
    private static final int KEY_W = 35;
    private static final int KEY_H = 17;
    private static final int KEY_LEFT_X = 0;
    private static final int KEY_LEFT_W = 6;
    private static final int KEY_RIGHT_W = 11;
    private static final int KEY_OCTAVES = 8;
    private static final int BAR_L_X = 66;
    private static final int BAR_L_W = 14;
    private static final int BAR_X = BAR_L_X + BAR_L_W;
    private static final int BAR_Y = 1;
    private static final int BAR_W = 2;
    private static final int BAR_H = 4;
    private static final int BAR_CNT = 64;
    /**
     * The frequency fluctuation display of FMDSP.DOC's "9", which shares the note bar's row and
     * sits over the "M:" LFO field of the line below it, see {@link #freqWave}. Its strokes are
     * the note bar's own: one pixel every {@link #BAR_W}, in the same two colours.
     */
    private static final int FREQ_X = TDETAIL_M_V_X;
    /** slots the wave itself flows through */
    private static final int FREQ_CNT = 8;
    /** slots of the whole field, which runs out to the end of the LFO field below */
    private static final int FREQ_FIELD = 20;
    /** rows the wave swings above and below its centre line */
    private static final int FREQ_AMP = 2;
    /** the row the wave rests on, a full swing from the top of the note bar's band */
    private static final int FREQ_MID_Y = BAR_Y + FREQ_AMP;
    /**
     * Pitch movement, in cents, that swings the wave to {@link #FREQ_AMP}. A third of a semitone:
     * the depth a vibrato is usually written at, measured off a real PMD song, so that an ordinary
     * one fills the display and a deeper one only pins it.
     */
    private static final int FREQ_FULL_CENTS = 30;
    /** how far the wave travels each frame, one slot every other frame */
    private static final double FREQ_SPEED = Math.PI / FREQ_CNT;
    private static final int COMMENT_Y = 340;
    private static final int COMMENT_H = 19;
    private static final int PLAYING_X = 0;
    private static final int PLAYING_Y = 324;
    private static final int PLAYING_W = 72;
    private static final int PLAYING_H = 9;
    private static final int FILEBAR_X = 77;
    private static final int FILEBAR_MUS_X = FILEBAR_X + 6;
    private static final int FILEBAR_IC_X = FILEBAR_MUS_X + 14;
    private static final int FILEBAR_F_X = FILEBAR_IC_X + 11;
    private static final int FILEBAR_ILE_X = FILEBAR_F_X + 4;
    private static final int FILEBAR_W = 2;
    private static final int FILEBAR_H = 7;
    private static final int FILEBAR_TRI_X = FILEBAR_ILE_X + 17;
    private static final int FILEBAR_TRI_Y = PLAYING_Y + 4;
    private static final int FILEBAR_TRI_W = 3;
    private static final int FILEBAR_TRI_H = 3;
    private static final int FILEBAR_FILENAME_X = FILEBAR_TRI_X + 8;
    private static final int PCM1FILEBAR_X = 463;
    /** the room the song title has, up to the PCM1 bar that shares its row */
    private static final int FILEBAR_FILENAME_W = PCM1FILEBAR_X - 2 - FILEBAR_FILENAME_X;
    private static final int PCM1FILETXT_X = PCM1FILEBAR_X + 5;
    private static final int PCM1FILETRI_X = PCM1FILETXT_X + 21;
    private static final int PCM1FILENAME_X = PCM1FILETRI_X + 8;
    private static final int PCM2FILEBAR_X = 551;
    private static final int PCM1FILENAME_W = PCM2FILEBAR_X - 2 - PCM1FILENAME_X;
    private static final int PCM2FILETXT_X = PCM2FILEBAR_X + 5;
    private static final int PCM2FILETRI_X = PCM2FILETXT_X + 21;
    private static final int PCM2FILENAME_X = PCM2FILETRI_X + 8;
    private static final int PCM2FILENAME_W = PC98_W - 2 - PCM2FILENAME_X;
    private static final int DT_SIGN_W = 3;
    private static final int DT_SIGN_H = 3;
    private static final int SPECTRUM_X = 352;
    private static final int SPECTRUM_Y = 207;
    private static final int CPU_Y = 115;
    private static final int CPU_X = 320;
    private static final int CPU_BAR_X = CPU_X - 6;
    private static final int CPU_NUM_X = CPU_X + 56;
    private static final int CPU_NUM_Y = CPU_Y + 2;
    private static final int CPU_TRI_X = CPU_X + 43;
    private static final int CPU_TRI_Y = CPU_Y + 10;
    private static final int FPS_X = CPU_X + 100;
    private static final int FPS_BAR_X = FPS_X - 6;
    private static final int FPS_NUM_X = FPS_X + 61;
    private static final int FPS_TRI_X = FPS_X + 48;
    private static final int TIME_TEXT_X = 530;
    private static final int TIME_X = TIME_TEXT_X + 38;
    private static final int TIME_BAR_X = TIME_TEXT_X - 6;
    private static final int TIME_TRI_X = TIME_TEXT_X + 31;
    private static final int TIME_BAR_W = 3;
    private static final int TIME_BAR_H = 14;
    private static final int TIME_Y = 22;
    private static final int CLOCK_Y = TIME_Y + 19;
    private static final int TIMERB_Y = CLOCK_Y + 19;
    private static final int LOOPCNT_Y = TIMERB_Y + 19;
    private static final int VOLDOWN_Y = LOOPCNT_Y + 19;
    private static final int PGMNUM_Y = VOLDOWN_Y + 19;
    /**
     * The {@code VOLUME DOWN} counter, which shows one part's correction at a time: three digits
     * on the counter lattice like every other counter, a sign place left of them and the part's
     * name left of that. Measured off the PC-98 capture, whose whole row is offset four pixels
     * from this layout's counters - the digits go where our counters are, the rest keeps the
     * capture's spacing against them.
     */
    private static final int VOLDOWN_DIGITS = 3;
    private static final int VOLDOWN_NUM_X = TIME_X + NUM_W * (8 - VOLDOWN_DIGITS);
    /** the place the sign moves out to when the number itself fills the three digits */
    private static final int VOLDOWN_SIGN_X = VOLDOWN_NUM_X - NUM_W;
    private static final int VOLDOWN_PART_X = VOLDOWN_NUM_X - 27;
    private static final int VOLDOWN_PART_Y = VOLDOWN_Y + 5;
    /** the middle segment of a digit, which is the counter's minus: row 5, two pixels wide */
    private static final int VOLDOWN_BAR_X = 3;
    private static final int VOLDOWN_BAR_W = 2;
    private static final int VOLDOWN_BAR_Y = 5;
    private static final int LOGO_Y = 1;
    private static final int LOGO_FM_W = 31;
    private static final int LOGO_DS_W = 32;
    private static final int LOGO_P_W = 15;
    private static final int LOGO_H = 12;
    private static final int LOGO_FM_X = 312;
    private static final int LOGO_DS_X = LOGO_FM_X + LOGO_FM_W + 2;
    private static final int LOGO_P_X = LOGO_DS_X + LOGO_DS_W + 2;
    private static final int CIRCLE_W = 31;
    private static final int CIRCLE_H = 31;
    private static final int CIRCLE_X = 312;
    private static final int CIRCLE_Y = 70;
    private static final int TOP_MUS_X = 397;
    private static final int TOP_MUSIC_Y = 7;
    private static final int TOP_IC_X = TOP_MUS_X + 14;
    private static final int TOP_F_X = TOP_IC_X + 12;
    private static final int TOP_ILE_X = TOP_F_X + 4;
    private static final int TOP_SELECTOR_X = TOP_ILE_X + 17;
    private static final int TOP_AND_X = TOP_SELECTOR_X + 42;
    private static final int TOP_STATUS_X = TOP_AND_X + 7;
    private static final int TOP_D_X = TOP_STATUS_X + 32;
    private static final int TOP_ISPLAY_X = TOP_D_X + 4;
    private static final int TOP_VER_X = TOP_ISPLAY_X + 32;
    private static final int TOP_TEXT_W = 231;
    private static final int TOP_TEXT_H = 5;
    private static final int TOP_TEXT_Y = TOP_MUSIC_Y - 6;
    private static final int VER_W = 13;
    private static final int VER_H = 5;
    private static final int VER_Y = 8;
    private static final int VER_0_X = TOP_VER_X + 15;
    private static final int VER_1_X = VER_0_X + 7;
    private static final int VER_2_X = VER_1_X + 7;
    private static final int DRIVER_TEXT_X = 312;
    private static final int DRIVER_TEXT_Y = 27;
    private static final int DRIVER_TEXT_2_X = DRIVER_TEXT_X + 9;
    private static final int DRIVER_TRI_X = DRIVER_TEXT_2_X + 26;
    private static final int DRIVER_TRI_Y = DRIVER_TEXT_Y + 3;
    private static final int DRIVER_NAME_X = DRIVER_TRI_X + 8;
    private static final int DRIVER_NAME_Y = DRIVER_TEXT_Y - 1;
    private static final int CHIP_TEXT_Y = DRIVER_TEXT_Y + 19;
    private static final int CHIP_TRI_Y = CHIP_TEXT_Y + 3;
    private static final int CHIP_NAME_Y = CHIP_TEXT_Y - 1;
    private static final int CURL_W = 11;
    private static final int CURL_H = 11;
    private static final int CURL_LEFT_X = 347;
    private static final int CURL_RIGHT_X = 509;
    private static final int CURL_Y = 80;
    private static final int PLAY_W = 30;
    private static final int PLAY_H = 7;
    private static final int PLAY_X = 354;
    private static final int PLAY_Y = 77;
    private static final int STOP_W = 31;
    private static final int STOP_H = 7;
    private static final int STOP_X = 393;
    private static final int STOP_Y = 77;
    private static final int PAUSE_W = 37;
    private static final int PAUSE_H = 7;
    private static final int PAUSE_X = 433;
    private static final int PAUSE_Y = 77;
    private static final int FADE_W = 31;
    private static final int FADE_H = 7;
    private static final int FADE_X = 481;
    private static final int FADE_Y = 77;
    private static final int FF_W = 20;
    private static final int FF_H = 7;
    private static final int FF_X = 360;
    private static final int FF_Y = 87;
    private static final int REW_W = 26;
    private static final int REW_H = 7;
    private static final int REW_X = 392;
    private static final int REW_Y = 87;
    private static final int FLOPPY_W = 74;
    private static final int FLOPPY_H = 7;
    private static final int FLOPPY_X = 432;
    private static final int FLOPPY_Y = 87;
    private static final int LEVEL_TEXT_X = 318;
    private static final int LEVEL_TEXT_Y = 290;
    private static final int LEVEL_X = 353 - 16;
    private static final int LEVEL_Y = 227;
    private static final int LEVEL_DISP_W = 14;
    private static final int LEVEL_W = 16;
    private static final int PANPOT_W = 15;
    private static final int PANPOT_H = 15;
    private static final int PANPOT_Y = LEVEL_Y + 64;
    private static final int LEVEL_TRACK_Y = LEVEL_Y - 9;
    private static final int LEVEL_PROG_Y = PANPOT_Y + 15;
    private static final int LEVEL_KEY_Y = LEVEL_PROG_Y + 7;

    // font metrics (fmdsp small = 5x6, medium = 6x8)
    private static final int SFW = 5, SFH = 6, SFB = 6;
    private static final int MFW = 6, MFH = 8, MFB = 8;

    private static final int[] DIVTAB = {
        32, 16, 8, 8, 4, 4, 4, 4, 2, 2, 2, 2, 2, 2, 2, 2,
    };

    // version shown top-right, matches FMPLAYER_VERSION_*
    private static final String VER0 = "0", VER1 = "1", VER2 = "14";

    /**
     * a-z for the small font, which carries only the ~46 glyphs the original ever needed - and the
     * medium font is no help either, its lowercase codepoints being copies of its uppercase glyphs.
     * Neither bitmap font in {@code fmdsp.dat} has a lowercase shape, which is why the original
     * draws its "for UNIX/LINUX..." line as the {@code s_text} sprite rather than as text.
     * <p>
     * Drawn in the small font's own 4 wide x 5 tall cell: the uppercase sits on rows 1-5, so these
     * keep the baseline on row 5, put ascenders on row 1 and hang descenders below it on row 6 -
     * which is why {@link #SMALL_LC} is 7 rows to the stock font's 6. Only {@link #setSubtitle} uses
     * this; the rest of the display stays dot-by-dot original.
     */
    private static final String[][] LOWERCASE = {
        {".....", ".....", ".##..", "#..#.", "#..#.", ".###.", "....."}, // a
        {".....", "#....", "###..", "#..#.", "#..#.", "###..", "....."}, // b
        {".....", ".....", ".###.", "#....", "#....", ".###.", "....."}, // c
        {".....", "...#.", ".###.", "#..#.", "#..#.", ".###.", "....."}, // d
        {".....", ".....", ".##..", "####.", "#....", ".###.", "....."}, // e
        {".....", "..##.", ".#...", "###..", ".#...", ".#...", "....."}, // f
        {".....", ".....", ".###.", "#..#.", ".###.", "...#.", "###.."}, // g
        {".....", "#....", "###..", "#..#.", "#..#.", "#..#.", "....."}, // h
        {".....", ".#...", ".....", ".#...", ".#...", ".#...", "....."}, // i
        {".....", "..#..", ".....", "..#..", "..#..", "..#..", "##..."}, // j
        {".....", "#....", "#.#..", "##...", "#.#..", "#..#.", "....."}, // k
        {".....", "##...", ".#...", ".#...", ".#...", ".###.", "....."}, // l
        {".....", ".....", "####.", "#.#.#", "#.#.#", "#.#.#", "....."}, // m
        {".....", ".....", "###..", "#..#.", "#..#.", "#..#.", "....."}, // n
        {".....", ".....", ".##..", "#..#.", "#..#.", ".##..", "....."}, // o
        {".....", ".....", "###..", "#..#.", "###..", "#....", "#...."}, // p
        {".....", ".....", ".###.", "#..#.", ".###.", "...#.", "...#."}, // q
        {".....", ".....", "#.##.", "##...", "#....", "#....", "....."}, // r
        {".....", ".....", ".###.", "##...", "..##.", "###..", "....."}, // s
        {".....", ".#...", "###..", ".#...", ".#...", "..##.", "....."}, // t
        {".....", ".....", "#..#.", "#..#.", "#..#.", ".###.", "....."}, // u
        {".....", ".....", "#..#.", "#..#.", "#..#.", ".##..", "....."}, // v
        {".....", ".....", "#.#.#", "#.#.#", "#.#.#", ".#.#.", "....."}, // w
        {".....", ".....", "#..#.", ".##..", ".##..", "#..#.", "....."}, // x
        {".....", ".....", "#..#.", "#..#.", ".###.", "...#.", "###.."}, // y
        {".....", ".....", "####.", "..#..", ".#...", "####.", "....."}, // z
    };

    /** rows per {@link #SMALL_LC} glyph: the stock 6 plus one for the descenders. */
    private static final int SLCB = 7;

    /** {@link #fontRom} with {@link #LOWERCASE} patched in over a-z, at a 7 row stride. */
    private static final byte[] SMALL_LC = smallWithLowercase();

    private static byte[] smallWithLowercase() {
        byte[] font = new byte[256 * SLCB];
        for (int c = 0; c < 256; c++) {
            System.arraycopy(fontdat, c * SFB, font, c * SLCB, SFB);
        }
        for (int i = 0; i < LOWERCASE.length; i++) {
            String[] glyph = LOWERCASE[i];
            for (int y = 0; y < SLCB; y++) {
                int bits = 0;
                for (int x = 0; x < glyph[y].length(); x++) {
                    if (glyph[y].charAt(x) == '#') bits |= 1 << (7 - x);
                }
                font[('a' + i) * SLCB + y] = (byte) bits;
            }
        }
        return font;
    }

    /** track_type_table[TrackId.ordinal()] : {type, num}. */
    private static final TrackType[] TYPE_OF = {
        TrackType.FM, TrackType.FM, TrackType.FM, TrackType.FM, TrackType.FM, TrackType.FM,
        TrackType.FM, TrackType.FM, TrackType.FM,
        TrackType.SSG, TrackType.SSG, TrackType.SSG,
        TrackType.ADPCM,
        TrackType.PPZ8, TrackType.PPZ8, TrackType.PPZ8, TrackType.PPZ8,
        TrackType.PPZ8, TrackType.PPZ8, TrackType.PPZ8, TrackType.PPZ8,
    };
    private static final int[] NUM_OF = {
        1, 2, 3, 3, 3, 3, 4, 5, 6, 1, 2, 3, 1, 1, 2, 3, 4, 5, 6, 7, 8,
    };

    /** original 10-track display table (track_disp_table_default). */
    private static final TrackId[] TRACK_DISP = {
        TrackId.FM_1, TrackId.FM_2, TrackId.FM_3, TrackId.FM_4, TrackId.FM_5, TrackId.FM_6,
        TrackId.SSG_1, TrackId.SSG_2, TrackId.SSG_3, TrackId.ADPCM,
    };

    /** {@link LeftMode#OPN}: the FM3 extended tracks in place of FM 4-6 (track_disp_table_opn). */
    private static final TrackId[] TRACK_DISP_OPN = {
        TrackId.FM_1, TrackId.FM_2, TrackId.FM_3,
        TrackId.FM_3_EX_1, TrackId.FM_3_EX_2, TrackId.FM_3_EX_3,
        TrackId.SSG_1, TrackId.SSG_2, TrackId.SSG_3, TrackId.ADPCM,
    };

    /** {@link LeftMode#PPZ8}: the eight PPZ8 tracks (track_disp_table_ppz8). */
    private static final TrackId[] TRACK_DISP_PPZ8 = {
        TrackId.PPZ8_1, TrackId.PPZ8_2, TrackId.PPZ8_3, TrackId.PPZ8_4,
        TrackId.PPZ8_5, TrackId.PPZ8_6, TrackId.PPZ8_7, TrackId.PPZ8_8,
        TrackId.ADPCM,
    };

    /** how many track rows fit in the left half */
    private static final int TRACK_DISP_CNT = 10;

    private static final String[] KEY_TABLE = {
        "C ", "C+", "D ", "D+", "E ", "F ", "F+", "G ", "G+", "A ", "A+", "B ",
    };

    /** Strings are Unicode; this is only used to index the JIS addressed font ROM. */
    private static final Charset JIS0208 = Charset.forName("x-JIS0208");

    /**
     * The NEC extensions the ROM has glyphs for but JIS X 0208 has no code for - its row 13
     * symbols, and its rows 89 to 92 of IBM characters - are reached through Shift_JIS instead.
     */
    private static final Charset MS932 = Charset.forName("MS932");

    /** Size of a PC-98 font ROM dump. */
    private static final int FONT_ROM_SIZE = 0x46800;

    /** The ANK glyphs start here, 16 bytes each. */
    private static final int ROM_ANK = 0x800;

    private static final int ROM_W = 8, ROM_H = 16;

    private final Map<Character, Integer> jisCache = new HashMap<>();

    // ---------- state ----------
    private FmDspDataSource source;

    /** @see #setSubtitle(String) */
    private String subtitle = "for JAVA/SWING vavi-apps-mdplayer";

    /** @see #setTitle(String) */
    private String title;

    /** @see #setVersion(String) */
    private String version;

    /** PC-98 font ROM, null when it was not supplied */
    private byte[] fontRom;
    private final Palette palette = new Palette();
    private LeftMode leftMode = LeftMode.AUTO;
    private RightMode rightMode = RightMode.DEFAULT;

    private final byte[] vram = new byte[PC98_W * PC98_H];
    private final int[] rgb = new int[Palette.COLORS];
    private final BufferedImage image =
            new BufferedImage(PC98_W, PC98_H, BufferedImage.TYPE_INT_RGB);
    private final int[] pixels =
            ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

    // peak-hold state (mirrors struct fmdsp)
    private final int[] fftData = new int[FFTDISPLEN];
    private final int[] fftCnt = new int[FFTDISPLEN];
    private final int[] fftDropDiv = new int[FFTDISPLEN];
    private final int[] levelData = new int[FMDSP_LEVEL_COUNT];
    private final int[] levelCnt = new int[FMDSP_LEVEL_COUNT];
    private final int[] levelDropDiv = new int[FMDSP_LEVEL_COUNT];

    /** the VOLUME DOWN counter's last reading of each part, and the part it is showing */
    private final int[] volDown = new int[WorkStateSource.VolumePart.values().length];
    private WorkStateSource.VolumePart volDownPart;

    /** where each row's frequency fluctuation wave has flowed to, and how far it swings */
    private final double[] freqPhase = new double[TrackId.COUNT];
    private final double[] freqAmp = new double[TrackId.COUNT];

    private long framecnt;
    private int cpuusage;
    private int fps;
    private long fpsT0 = System.nanoTime();
    private int fpsFrames;

    private final int[] fftScratch = new int[FFTDISPLEN];
    private final TrackStatus scratch = new TrackStatus();

    private final Timer timer;

    /**
     * @param fps target frame rate, e.g. {@code 60}.
     */
    public FmDspVisualizer(int fps) {
        this(fps, 1);
    }

    /**
     * @param fps target frame rate, e.g. {@code 60}.
     */
    public FmDspVisualizer(int fps, int zoom) {
        if (fps <= 0) fps = 60;
        setPreferredSize(new Dimension(CANVAS_W * zoom, CANVAS_H * zoom));
        setBackground(Color.BLACK);
        setOpaque(true);
        timer = new Timer(1000 / fps, e -> repaint());
        timer.setRepeats(true);
    }

    public FmDspVisualizer() {
        this(60);
    }

    /** Set the data source. May be replaced at any time. */
    public void setDataSource(FmDspDataSource source) {
        this.source = source;
        // the next song's corrections are not this one's changing, so the counter picks its part
        // afresh rather than jumping to whichever one happens to differ
        volDownPart = null;
    }

    /**
     * Supplies the PC-98 font ROM the comment lines are drawn with, as the original does. Without
     * it the comments fall back to the ANK only medium font, so Japanese text stays blank.
     *
     * @param rom a font ROM dump of {@link #FONT_ROM_SIZE} bytes, or null to use the fallback
     */
    public void setFontRom(byte[] rom) {
        if (rom != null && rom.length < FONT_ROM_SIZE) {
            throw new IllegalArgumentException("not a PC-98 font rom: " + rom.length + " bytes");
        }
        this.fontRom = rom;
    }

    public LeftMode getLeftMode() {
        return leftMode;
    }

    /**
     * The rows the left half shows. {@link LeftMode#THIRTEEN} is drawn as {@link LeftMode#OPNA}
     * for now: thirteen rows only fit in the half fmdsp gives them with the compact geometry it
     * switches to for that mode, which this renderer does not have.
     */
    private TrackId[] tracks() {
        return switch (leftMode) {
            case AUTO -> {
                TrackStatusSource ts = source != null ? source.trackStatus() : null;
                TrackId[] disp = ts != null ? ts.displayTracks() : null;
                yield disp != null && disp.length > 0 ? disp : TRACK_DISP;
            }
            case OPN -> TRACK_DISP_OPN;
            case PPZ8 -> TRACK_DISP_PPZ8;
            default -> TRACK_DISP;
        };
    }

    public void setLeftMode(LeftMode mode) {
        this.leftMode = mode;
    }

    public RightMode getRightMode() {
        return rightMode;
    }

    public void setRightMode(RightMode mode) {
        this.rightMode = mode;
    }

    /** Switch palette index (0..{@link Palette#COUNT}-1). */
    public void setPaletteIndex(int index) {
        palette.select(index);
    }

    /**
     * The line above "MUSIC FILE SELECTOR & STATUS DISPLAY", where the original has the
     * pre-rendered "for UNIX/LINUX GTK+3..." bitmap. {@code null} restores that bitmap.
     * <p>
     * Drawn with the small font plus {@link #LOWERCASE}, and ANK only. At 5 px a character the
     * 48th runs into the right edge and the text is clipped there.
     */
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    /**
     * Replaces the pre-rendered "FMDSP" logo with a custom title.
     * <p>
     * A title that the logo art can spell - anything made of F, M, D, S and P, such as
     * {@code "MDDSP"} - is cut from the artwork itself and pasted letter by letter, so it is the
     * original lettering rather than an imitation of it. Any other title is rasterized with a
     * bold vector font sized to the 12 px band, since the logo is art and the bundled bitmap
     * fonts only go up to 6x8. {@code null} restores the logo.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Replaces the version shown next to "VER", where the original has "0.1.14". ANK only.
     * {@code null} restores the built-in version.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /** Start the repaint timer. */
    public void start() {
        timer.start();
    }

    /** Stop the repaint timer. */
    public void stop() {
        timer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // measure fps
        fpsFrames++;
        long now = System.nanoTime();
        if (now - fpsT0 >= 1_000_000_000L) {
            fps = (int) (fpsFrames * 1_000_000_000L / (now - fpsT0));
            fpsFrames = 0;
            fpsT0 = now;
        }

        // build the whole VRAM this frame (static chrome first, then dynamic).
        vramInit();
        trackInit10();
        update10();
        framecnt++;

        // palette fade, then map VRAM -> image.
        palette.tick();
        for (int i = 0; i < Palette.COLORS; i++) {
            rgb[i] = palette.color(i).getRGB();
        }
        for (int i = 0; i < vram.length; i++) {
            pixels[i] = rgb[vram[i] & 0xff];
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
        } finally {
            g2.dispose();
        }
    }

    // ==================================================================
    // low-level blit primitives (verbatim from fmdsp.c)
    // ==================================================================

    private void vramblit(int x, int y, byte[] data, int off, int w, int h) {
        for (int yi = 0; yi < h; yi++) {
            int row = (y + yi) * PC98_W + x;
            int drow = off + yi * w;
            if (w >= 0) System.arraycopy(data, drow + 0, vram, row + 0, w);
        }
    }

    private void vramblitColor(int x, int y, byte[] data, int off, int w, int h, int color) {
        for (int yi = 0; yi < h; yi++) {
            int row = (y + yi) * PC98_W + x;
            int drow = off + yi * w;
            for (int xi = 0; xi < w; xi++) {
                vram[row + xi] = data[drow + xi] != 0 ? (byte) color : 0;
            }
        }
    }

    private void vramblitColorSub(int dstX, int dstY, byte[] data, int totalW, int subX, int subW, int h, int color) {
        for (int yi = 0; yi < h; yi++) {
            int row = (dstY + yi) * PC98_W + dstX + subX;
            int drow = yi * totalW + subX;
            for (int xi = 0; xi < subW; xi++) {
                if (data[drow + xi] != 0) vram[row + xi] = (byte) color;
            }
        }
    }

    private void vramblitKey(int x, int y, byte[] data, int off, int w, int h, int key, int color) {
        for (int yi = 0; yi < h; yi++) {
            int row = (y + yi) * PC98_W + x;
            int drow = off + yi * w;
            for (int xi = 0; xi < w; xi++) {
                if ((data[drow + xi] & 0xff) == (key + 1)) {
                    vram[row + xi] = (byte) color;
                }
            }
        }
    }

    private void vramPutchar(byte[] font, int foff, int x, int y, int w, int h, int color, boolean bg) {
        for (int yi = 0; yi < h; yi++) {
            int fb = font[foff + yi] & 0xff;
            int row = (y + yi) * PC98_W + x;
            for (int xi = 0; xi < w; xi++) {
                if ((fb & (1 << (7 - xi))) != 0) {
                    vram[row + xi] = (byte) color;
                } else if (bg) {
                    vram[row + xi] = 0;
                }
            }
        }
    }

    /** Draw a string with the fmdsp small font (5x6). */
    private void putSmall(String s, int x, int y, int color, boolean bg) {
        putline(s, fontdat, SFW, SFH, SFB, x, y, color, bg);
    }

    /** Draw a string with the fmdsp medium font (6x8). */
    private void putMedium(String s, int x, int y, int color, boolean bg) {
        putline(s, fmdsp_medium_dat, MFW, MFH, MFB, x, y, color, bg);
    }

    /** the custom title as a 1-bit mask at the logo's height, see {@link #blitTitle} */
    private boolean[] titleMask;
    private int titleMaskW;
    private String renderedTitle;

    /** the room the title has: the logo band, up to the "MUSIC FILE SELECTOR" line beside it */
    private static final int LOGO_MAX_W = TOP_MUS_X - 2 - LOGO_FM_X;

    /** the gap the logo art leaves between two of its letters */
    private static final int LOGO_GAP = 2;

    /** a letter of the "FMDSP" logo art: which sprite it is in, and its columns there */
    private record LogoGlyph(byte[] sprite, int spriteW, int x, int w) {}

    /**
     * The letters the logo art actually contains. A title spelled only from these is set in the
     * original artwork rather than a font - "MDDSP" is the same M, D, S and P, with the D reused.
     */
    private static final Map<Character, LogoGlyph> LOGO_GLYPHS = Map.of(
            'F', new LogoGlyph(s_logo_fm, LOGO_FM_W, 0, 14),
            'M', new LogoGlyph(s_logo_fm, LOGO_FM_W, 16, 15),
            'D', new LogoGlyph(s_logo_ds, LOGO_DS_W, 0, 15),
            'S', new LogoGlyph(s_logo_ds, LOGO_DS_W, 17, 15),
            'P', new LogoGlyph(s_logo_p, LOGO_P_W, 0, 15));

    /**
     * Sets the title in the logo's own letters, cut from the sprites and pasted side by side with
     * the spacing the art uses. Returns false when a letter is not in the artwork or the result
     * would not fit, leaving the caller to fall back to the font.
     */
    private boolean blitTitleFromLogo() {
        int w = 0;
        for (int i = 0; i < title.length(); i++) {
            LogoGlyph glyph = LOGO_GLYPHS.get(Character.toUpperCase(title.charAt(i)));
            if (glyph == null) return false;
            w += (i > 0 ? LOGO_GAP : 0) + glyph.w();
        }
        if (w == 0 || w > LOGO_MAX_W) return false;

        int x = LOGO_FM_X;
        for (int i = 0; i < title.length(); i++) {
            LogoGlyph glyph = LOGO_GLYPHS.get(Character.toUpperCase(title.charAt(i)));
            if (i > 0) x += LOGO_GAP;
            vramblitSub(x, LOGO_Y, glyph.sprite(), glyph.spriteW(), glyph.x(), glyph.w(), LOGO_H);
            x += glyph.w();
        }
        return true;
    }

    /** {@link #vramblit} of a column range of a wider sprite, shading and all */
    private void vramblitSub(int x, int y, byte[] data, int srcW, int srcX, int w, int h) {
        for (int yi = 0; yi < h; yi++) {
            int row = (y + yi) * PC98_W + x;
            int drow = yi * srcW + srcX;
            if (w >= 0) System.arraycopy(data, drow + 0, vram, row + 0, w);
        }
    }

    /**
     * Draws {@link #title} where the logo sits.
     * <p>
     * The logo is sprite art and the bundled bitmap fonts only go up to 6x8, so the title is
     * rasterized with a vector font instead - bold, aliased (the VRAM is palette indexed), and
     * sized to the largest that still fits the 12 px band and the width up to the "VER" sprite.
     * A short title therefore comes out as heavy as the original art; a long one shrinks rather
     * than overrun the header. The mask is rebuilt only when the title changes.
     */
    private void blitTitle() {
        // a title the logo art can spell is cut from the art itself
        if (blitTitleFromLogo()) return;

        int maxW = LOGO_MAX_W;
        if (!title.equals(renderedTitle)) {
            titleMaskW = 0;
            titleMask = new boolean[maxW * LOGO_H];
            BufferedImage mask = new BufferedImage(maxW, LOGO_H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = mask.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                g.setColor(Color.WHITE);
                for (int size = LOGO_H * 2; size >= 6; size--) {
                    Font font = new Font(Font.SANS_SERIF, Font.BOLD, size);
                    // the ink, not the metrics: the band has no room for the font's leading
                    Rectangle ink = font.createGlyphVector(g.getFontRenderContext(), title)
                            .getPixelBounds(null, 0, 0);
                    if (ink.width > maxW || ink.height > LOGO_H) continue;

                    g.setFont(font);
                    // flush left, centred in what is left of the band
                    g.drawString(title, -ink.x, -ink.y + (LOGO_H - ink.height) / 2);
                    titleMaskW = ink.width;
                    break;
                }
            } finally {
                g.dispose();
            }
            for (int y = 0; y < LOGO_H; y++) {
                for (int x = 0; x < maxW; x++) {
                    titleMask[y * maxW + x] = (mask.getRGB(x, y) & 0xff) > 127;
                }
            }
            renderedTitle = title;
        }
        for (int y = 0; y < LOGO_H; y++) {
            for (int x = 0; x < titleMaskW; x++) {
                if (titleMask[y * maxW + x]) {
                    vram[(LOGO_Y + y) * PC98_W + LOGO_FM_X + x] = 2;
                }
            }
        }
    }

    /**
     * Draws with one of the fmdsp bitmap fonts, which are ANK only: a full width character has no
     * glyph, so nothing is drawn for it, but the cursor advances exactly as in the C code.
     */
    private void putline(String s, byte[] font, int fw, int fh, int glyphBytes,
                         int x, int y, int color, boolean bg) {
        if (s == null) return;
        int xo = 0;
        int yo = 0;
        int lineH = fh + 3;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r') {
                xo = 0;
                if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                }
                yo += lineH;
                continue;
            }
            if (c == '\n') {
                xo = 0;
                yo += lineH;
                continue;
            }
            if (c == '\t') {
                xo += fw * 8;
                xo -= xo % (fw * 8);
                continue;
            }
            if (c < 0x20) {
                continue;
            }
            int ank = ankOf(c);
            if (ank >= 0) {
                if (x + xo + fw > PC98_W) continue;
                if (y + yo + fh <= PC98_H) {
                    vramPutchar(font, ank * glyphBytes, x + xo, y + yo, fw, fh, color, bg);
                }
                xo += fw;
            } else {
                if (x + xo + fw * 2 > PC98_W) continue;
                xo += fw + 8;
            }
        }
    }

    /**
     * Draws with the PC-98 font ROM: ANK 8x16, kanji 16x16 as two halves. This is what the original
     * uses for the comment lines, and the only font here that has Japanese glyphs.
     */
    private void putRom(String s, int x, int y, int color, boolean bg) {
        if (s == null) return;
        int xo = 0;
        int yo = 0;
        int lineH = COMMENT_H;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r') {
                xo = 0;
                if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                }
                yo += lineH;
                continue;
            }
            if (c == '\n') {
                xo = 0;
                yo += lineH;
                continue;
            }
            if (c == '\t') {
                xo += ROM_W * 8;
                xo -= xo % (ROM_W * 8);
                continue;
            }
            if (c < 0x20) {
                continue;
            }
            int ank = ankOf(c);
            if (ank >= 0 || Pc98Gaiji.isHalfWidth(c)) {
                if (x + xo + ROM_W > PC98_W) continue;
                // a half width row's glyph is the left half of its cell, so both are one blit
                int glyph = ank >= 0 ? ROM_ANK + ank * ROM_H : romKanji(jisOf(c));
                if (glyph >= 0 && y + yo + ROM_H <= PC98_H) {
                    vramPutchar(fontRom, glyph, x + xo, y + yo, ROM_W, ROM_H, color, bg);
                }
                xo += ROM_W;
            } else {
                if (x + xo + ROM_W * 2 > PC98_W) continue;
                int glyph = romKanji(jisOf(c));
                // a gaiji is defined in the machine's RAM, so no ROM dump has one: rather than
                // leave a hole where a character was, mark it the way a missing one is marked.
                // Only a gaiji, mind - a full width space is a blank cell and means it
                if (glyph < 0 || (Pc98Gaiji.jisOf(c) != 0 && isBlank(glyph))) {
                    glyph = romKanji(jisOf(GETA));
                }
                if (glyph >= 0 && y + yo + ROM_H <= PC98_H) {
                    vramPutchar(fontRom, glyph, x + xo, y + yo, ROM_W, ROM_H, color, bg);
                    vramPutchar(fontRom, glyph + ROM_H, x + xo + ROM_W, y + yo, ROM_W, ROM_H, color, bg);
                }
                xo += ROM_W * 2;
            }
        }
    }

    /**
     * Clips {@code s} to {@code maxW} pixels, ending it with an ellipsis when it does not fit,
     * the way a text field does - the file bar has other labels to its right and
     * {@link #putline} would otherwise draw straight over them.
     * <p>
     * Widths follow putline's own advance rules, so a full width character counts as the
     * {@code fw + 8} it advances even though the ANK fonts have no glyph for it.
     */
    private static String stripPath(String path) {
        if (path == null || path.isEmpty()) return path;
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }
        return path;
    }

    private static String stripPathAndExtension(String path) {
        if (path == null || path.isEmpty()) return path;
        String name = stripPath(path);
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            return name.substring(0, dot);
        }
        return name;
    }

    private static String ellipsize(String s, int fw, int maxW) {
        if (s == null) return null;

        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += advanceOf(s.charAt(i), fw);
        }
        if (w <= maxW) return s;

        int room = maxW - 3 * fw; // "..." is three ANK characters
        if (room <= 0) return "";
        StringBuilder sb = new StringBuilder(s.length());
        int used = 0;
        for (int i = 0; i < s.length(); i++) {
            int advance = advanceOf(s.charAt(i), fw);
            if (used + advance > room) break;
            used += advance;
            sb.append(s.charAt(i));
        }
        return sb.append("...").toString();
    }

    /** what {@link #putline} moves the cursor by for {@code c} */
    private static int advanceOf(char c, int fw) {
        if (c < 0x20) return 0;
        return ankOf(c) >= 0 ? fw : fw + 8;
    }

    /** ANK code of {@code c}, or -1 when it is a full width character. */
    private static int ankOf(char c) {
        if (c < 0x80) return c;
        // half width katakana, which the ANK font carries at 0xa1..0xdf
        if (c >= 0xff61 && c <= 0xff9f) return c - 0xff61 + 0xa1;
        return -1;
    }

    /** JIS X 0208 code of {@code c}, or 0 when it has none. */
    private int jisOf(char c) {
        return jisCache.computeIfAbsent(c, ch -> {
            int gaiji = Pc98Gaiji.jisOf(ch);
            if (gaiji != 0) {
                return gaiji;
            }
            char v = jisVariantOf(ch);
            // x-JIS0208 encodes what it has no code for as a character of its own, so asking it
            // first would draw that one instead - every unmappable character sharing one glyph
            if (JIS0208.newEncoder().canEncode(v)) {
                byte[] b = String.valueOf(v).getBytes(JIS0208);
                return b.length == 2 ? (b[0] & 0xff) << 8 | (b[1] & 0xff) : 0;
            }
            if (MS932.newEncoder().canEncode(v)) {
                byte[] b = String.valueOf(v).getBytes(MS932);
                return b.length == 2 ? jisOfShiftJis(b[0] & 0xff, b[1] & 0xff) : 0;
            }
            return 0;
        });
    }

    /** JIS X 0208 code of a Shift_JIS pair, or 0 when it addresses no row of one. */
    private static int jisOfShiftJis(int b1, int b2) {
        int row = (b1 < 0xe0 ? b1 - 0x81 : b1 - 0xc1) * 2 + 1;
        int cell;
        if (b2 >= 0x9f) {
            row++;
            cell = b2 - 0x9f + 1;
        } else {
            cell = b2 - 0x40 + (b2 < 0x7f ? 1 : 0);
        }
        // the IBM characters Shift_JIS also keeps past 0xfa are beyond the 94 rows a JIS code has
        return row >= 1 && row <= 94 && cell >= 1 && cell <= 94 ? (row + 0x20) << 8 | (cell + 0x20) : 0;
    }

    /**
     * The few characters JIS X 0208 and Shift_JIS disagree about. Text decoded from Shift_JIS
     * carries the full width forms, which {@code x-JIS0208} cannot encode, so they would be lost -
     * a wave dash or a minus sign would drop out of a title. The glyph is the same either way.
     */
    private static char jisVariantOf(char c) {
        return switch (c) {
            case '－' -> '−'; // minus sign
            case '～' -> '〜'; // wave dash
            case '￥' -> '¥'; // yen sign
            case '￠' -> '¢'; // cent sign
            case '￡' -> '£'; // pound sign
            case '￢' -> '¬'; // not sign
            case '∥' -> '‖'; // double vertical line
            default -> c;
        };
    }

    /** what a character the ROM cannot draw is marked with */
    private static final char GETA = '〓';

    /** whether the ROM's cell at {@code glyph} is empty, which is how it holds an undefined one */
    private boolean isBlank(int glyph) {
        for (int i = 0; i < ROM_H * 2; i++) {
            if (fontRom[glyph + i] != 0) return false;
        }
        return true;
    }

    /** Offset of the left half of a kanji in the font ROM, or -1 when there is none. */
    private int romKanji(int jis) {
        if (fontRom == null || jis == 0) return -1;
        int offset = ROM_ANK + 0x60 * ROM_H * 2 * ((jis >> 8) - 0x20) + ((jis & 0xff) << 5);
        return offset >= 0 && offset + ROM_H * 2 <= fontRom.length ? offset : -1;
    }

    private void blitNum(int x, int y, int digit) {
        vramblit(x, y, s_num, digit * (NUM_W * NUM_H), NUM_W, NUM_H);
    }

    // ==================================================================
    // static chrome: fmdsp_vram_init
    // ==================================================================

    private void vramInit() {
        WorkStateSource w = source != null ? source.work() : null;
        Arrays.fill(vram, (byte) 0);

        vramblit(PLAYING_X, PLAYING_Y, s_playing, 0, PLAYING_W, PLAYING_H);
        vramblit(FILEBAR_X, PLAYING_Y, s_filebar, 0, FILEBAR_W, FILEBAR_H);
        putSmall("MUS", FILEBAR_MUS_X, PLAYING_Y + 1, 2, false);
        putSmall("IC", FILEBAR_IC_X, PLAYING_Y + 1, 2, false);
        putSmall("F", FILEBAR_F_X, PLAYING_Y + 1, 2, false);
        putSmall("ILE", FILEBAR_ILE_X, PLAYING_Y + 1, 2, false);
        vramblit(FILEBAR_TRI_X, FILEBAR_TRI_Y, s_filebar_tri, 0, FILEBAR_TRI_W, FILEBAR_TRI_H);
        for (int x = 74; x < PC98_W; x++) {
            vram[332 * PC98_W + x] = 7;
        }
        int pcmcount = 0;
        if (w != null) {
            for (int i = 0; i < 4; i++) {
                if (w.pcmType(i) != null || w.pcmFilename(i) != null) {
                    pcmcount = i + 1;
                }
            }
        }
        if (pcmcount == 0) {
            pcmcount = 2;
        }

        int firstPcmBarX = PCM2FILEBAR_X - (pcmcount - 1) * 88;
        int filenameW = Math.max(20, firstPcmBarX - FILEBAR_FILENAME_X - 2);

        putMedium(ellipsize(w != null ? stripPath(w.filename()) : null, MFW, filenameW),
                FILEBAR_FILENAME_X, PLAYING_Y, 2, false);

        for (int i = 0; i < pcmcount; i++) {
            int xoff = (pcmcount - i - 1) * 88;
            int barX = PCM2FILEBAR_X - xoff;
            int txtX = barX + 4;
            String pcmType = w != null ? w.pcmType(i) : null;
            if (pcmType == null || pcmType.isEmpty()) {
                pcmType = "PCM" + (i + 1);
            }
            int triX = txtX + pcmType.length() * 5 + 1;
            int nameX = triX + 7;
            int nameW = (i == pcmcount - 1) ? (PC98_W - nameX) : (barX + 88 - nameX - 2);
            if (nameW < 10) nameW = 50;

            vramblit(barX, PLAYING_Y, s_filebar, 0, FILEBAR_W, FILEBAR_H);
            putSmall(pcmType, txtX, PLAYING_Y + 1, 2, false);
            vramblit(triX, FILEBAR_TRI_Y, s_filebar_tri, 0, FILEBAR_TRI_W, FILEBAR_TRI_H);
            putMedium(ellipsize(w != null ? stripPathAndExtension(w.pcmFilename(i)) : null, MFW, nameW),
                    nameX, PLAYING_Y, 2 + (w != null && w.pcmError(i) ? 1 : 0), false);
        }

        int height = (16 + 3) * 3 + 8;
        for (int y = PC98_H - height; y < PC98_H; y++) {
            for (int x = 0; x < PC98_W; x++) {
                vram[y * PC98_W + x] = (byte) (((y & 1) ^ (x & 1)) != 0 ? 3 : 0);
            }
        }
        vram[(PC98_H - height) * PC98_W] = 0;
        vram[(PC98_H - 1) * PC98_W] = 0;

        // comments: the original draws these with the PC-98 ROM font, which is where the 16 px
        // line height comes from. Without a ROM we fall back to the medium font, which has no
        // kanji, so Japanese comments come out blank - see setFontRom().
        if (w != null) {
            for (int i = 0; i < 3; i++) {
                if (fontRom != null) {
                    putRom(w.comment(i), 0, COMMENT_Y + COMMENT_H * i, 2, false);
                } else {
                    putMedium(w.comment(i), 0, COMMENT_Y + COMMENT_H * i, 2, false);
                }
            }
        }
    }

    // ==================================================================
    // static chrome: fmdsp_track_init_10 (ORIGINAL style)
    // ==================================================================

    private void trackInit10() {
        for (int y = 0; y < TRACK_H * TRACK_DISP_CNT; y++) {
            for (int x = 0; x < PC98_W; x++) {
                vram[y * PC98_W + x] = 0;
            }
        }
        TrackId[] disp = tracks();
        TrackStatusSource ts = source != null ? source.trackStatus() : null;
        for (int i = 0; i < disp.length; i++) {
            TrackId t = disp[i];
            String trackType = ts != null ? ts.trackTypeName(t) : null;
            if (trackType != null) {
                trackType = (trackType + "     ").substring(0, 5);
            } else {
                trackType = switch (TYPE_OF[t.ordinal()]) {
                    case FM -> "FM   ";
                    case SSG -> "SSG  ";
                    case ADPCM -> "ADPCM";
                    case PPZ8 -> "PPZ8 ";
                    default -> "     ";
                };
            }
            putSmall(trackType, 1, TRACK_H * i, 2, true);
            putSmall("TRACK.", 1, TRACK_H * i + 6, 1, true);
            vramblit(KEY_LEFT_X, TRACK_H * i + KEY_Y, s_key_left, 0, KEY_LEFT_W, KEY_H);
            for (int j = 0; j < KEY_OCTAVES; j++) {
                vramblit(KEY_X + KEY_W * j, TRACK_H * i + KEY_Y, s_key_bg, 0, KEY_W, KEY_H);
            }
            vramblit(KEY_X + KEY_W * KEY_OCTAVES, TRACK_H * i + KEY_Y, s_key_right, 0, KEY_RIGHT_W, KEY_H);
            vramblitColor(BAR_L_X, TRACK_H * i + BAR_Y, s_bar_l, 0, BAR_L_W, BAR_H, 3);
            for (int j = 0; j < BAR_CNT; j++) {
                vramblitColor(BAR_X + BAR_W * j, TRACK_H * i + BAR_Y, s_bar, 0, BAR_W, BAR_H, 3);
            }
            freqField(TRACK_H * i);
        }

        // a short display list ({@link LeftMode#AUTO}) leaves rows over: draw their keyboard and
        // bar chrome without any labels, so they read as unused tracks rather than holes
        for (int i = disp.length; i < TRACK_DISP_CNT; i++) {
            vramblit(KEY_LEFT_X, TRACK_H * i + KEY_Y, s_key_left, 0, KEY_LEFT_W, KEY_H);
            for (int j = 0; j < KEY_OCTAVES; j++) {
                vramblit(KEY_X + KEY_W * j, TRACK_H * i + KEY_Y, s_key_bg, 0, KEY_W, KEY_H);
            }
            vramblit(KEY_X + KEY_W * KEY_OCTAVES, TRACK_H * i + KEY_Y, s_key_right, 0, KEY_RIGHT_W, KEY_H);
            vramblitColor(BAR_L_X, TRACK_H * i + BAR_Y, s_bar_l, 0, BAR_L_W, BAR_H, 3);
            for (int j = 0; j < BAR_CNT; j++) {
                vramblitColor(BAR_X + BAR_W * j, TRACK_H * i + BAR_Y, s_bar, 0, BAR_W, BAR_H, 3);
            }
            freqField(TRACK_H * i);
        }

        if (rightMode == RightMode.DEFAULT) initDefault();
    }

    /**
     * The static chrome of the right half: the logo, the counter frames, the spectrum scale and
     * the level meter scale. Only {@link RightMode#DEFAULT} has them - the other right modes put
     * their own content over the whole half, so nothing is drawn there for them at all
     * ({@code init_default}).
     */
    private void initDefault() {
        if (title == null) {
            vramblit(LOGO_FM_X, LOGO_Y, s_logo_fm, 0, LOGO_FM_W, LOGO_H);
            vramblit(LOGO_DS_X, LOGO_Y, s_logo_ds, 0, LOGO_DS_W, LOGO_H);
            vramblit(LOGO_P_X, LOGO_Y, s_logo_p, 0, LOGO_P_W, LOGO_H);
        } else {
            blitTitle();
        }
        putSmall("MUS", TOP_MUS_X, TOP_MUSIC_Y, 2, true);
        putSmall("IC", TOP_IC_X, TOP_MUSIC_Y, 2, true);
        putSmall("F", TOP_F_X, TOP_MUSIC_Y, 2, true);
        putSmall("ILE", TOP_ILE_X, TOP_MUSIC_Y, 2, true);
        putSmall("SELECTOR", TOP_SELECTOR_X, TOP_MUSIC_Y, 2, true);
        putSmall("&", TOP_AND_X, TOP_MUSIC_Y, 2, true);
        putSmall("STATUS", TOP_STATUS_X, TOP_MUSIC_Y, 2, true);
        putSmall("D", TOP_D_X, TOP_MUSIC_Y, 2, true);
        putSmall("ISPLAY", TOP_ISPLAY_X, TOP_MUSIC_Y, 2, true);
        vramblit(TOP_VER_X, VER_Y, s_ver, 0, VER_W, VER_H);
        if (version == null) {
            putSmall(VER0 + ".", VER_0_X, TOP_MUSIC_Y, 2, true);
            putSmall(VER1 + ".", VER_1_X, TOP_MUSIC_Y, 2, true);
            putSmall(VER2, VER_2_X, TOP_MUSIC_Y, 2, true);
        } else {
            putSmall(version, VER_0_X, TOP_MUSIC_Y, 2, true);
        }

        if (subtitle == null) {
            vramblit(TOP_MUS_X, TOP_TEXT_Y, s_text, 0, TOP_TEXT_W, TOP_TEXT_H);
        } else {
            // same origin as the sprite: the glyphs cover its 5 rows and descenders hang one row
            // below, which is free because the small font's blank row 0 keeps the
            // "MUSIC FILE SELECTOR" line beneath from painting before row 8.
            putline(subtitle, SMALL_LC, SFW, SLCB, SLCB, TOP_MUS_X, TOP_TEXT_Y, 2, false);
        }

        putSmall("DR", DRIVER_TEXT_X, DRIVER_TEXT_Y, 7, true);
        putSmall("IVER", DRIVER_TEXT_2_X, DRIVER_TEXT_Y, 7, true);
        vramblitColor(DRIVER_TRI_X, DRIVER_TRI_Y, s_filebar_tri, 0, FILEBAR_TRI_W, FILEBAR_TRI_H, 7);
        WorkStateSource work = source != null ? source.work() : null;
        int rightMaxW = TIME_BAR_X - 2 - DRIVER_NAME_X;
        putMedium(ellipsize(work != null ? work.driverName() : null, MFW, rightMaxW), DRIVER_NAME_X, DRIVER_NAME_Y, 2, false);

        putSmall("CH", DRIVER_TEXT_X, CHIP_TEXT_Y, 7, true);
        putSmall("IP", DRIVER_TEXT_2_X, CHIP_TEXT_Y, 7, true);
        vramblitColor(DRIVER_TRI_X, CHIP_TRI_Y, s_filebar_tri, 0, FILEBAR_TRI_W, FILEBAR_TRI_H, 7);
        putSmall(ellipsize(work != null ? work.chips() : null, SFW, rightMaxW), DRIVER_NAME_X, CHIP_TEXT_Y, 2, false);
        vramblit(CURL_LEFT_X, CURL_Y, s_curl_left, 0, CURL_W, CURL_H);
        vramblit(CURL_RIGHT_X, CURL_Y, s_curl_right, 0, CURL_W, CURL_H);

        for (int x = 0; x < 82; x++) {
            vram[14 * PC98_W + 312 + x] = 2;
        }
        for (int x = 0; x < 239; x++) {
            vram[14 * PC98_W + 395 + x] = 7;
        }
        for (int x = 0; x < TIME_BAR_W; x++) {
            for (int y = 0; y < TIME_BAR_H; y++) {
                vram[(TIME_Y - 2 + y) * PC98_W + TIME_BAR_X + x] = 2;
                vram[(CLOCK_Y - 2 + y) * PC98_W + TIME_BAR_X + x] = 2;
                vram[(TIMERB_Y - 2 + y) * PC98_W + TIME_BAR_X + x] = 2;
                vram[(LOOPCNT_Y - 2 + y) * PC98_W + TIME_BAR_X + x] = 2;
                vram[(VOLDOWN_Y - 2 + y) * PC98_W + TIME_BAR_X + x] = 2;
                vram[(PGMNUM_Y - 2 + y) * PC98_W + TIME_BAR_X + x] = 2;
            }
        }
        for (int i = 0; i < 6; i++) {
            vramblit(TIME_TRI_X, TIME_Y + 8 + 19 * i, s_filebar_tri, 0, FILEBAR_TRI_W, FILEBAR_TRI_H);
        }
        putSmall("PASSED", TIME_TEXT_X, TIME_Y - 2, 2, true);
        putSmall("T", TIME_TEXT_X + 11, TIME_Y + 5, 2, true);
        putSmall("IME", TIME_TEXT_X + 15, TIME_Y + 5, 2, true);
        putSmall("CLOCK", TIME_TEXT_X, CLOCK_Y - 2, 2, true);
        putSmall(" COUNT", TIME_TEXT_X, CLOCK_Y + 5, 2, true);
        putSmall("T", TIME_TEXT_X, TIMERB_Y - 2, 2, true);
        putSmall("IMER", TIME_TEXT_X + 4, TIMERB_Y - 2, 2, true);
        putSmall(" CYCLE", TIME_TEXT_X, TIMERB_Y + 5, 2, true);
        putSmall("LOOP", TIME_TEXT_X, LOOPCNT_Y - 2, 2, true);
        putSmall(" COUNT", TIME_TEXT_X, LOOPCNT_Y + 5, 2, true);
        putSmall("VOLUME", TIME_TEXT_X, VOLDOWN_Y - 2, 2, true);
        putSmall("  DOWN", TIME_TEXT_X, VOLDOWN_Y + 5, 2, true);
        putSmall("PGM", TIME_TEXT_X, PGMNUM_Y - 2, 2, true);
        putSmall("NUMBER", TIME_TEXT_X, PGMNUM_Y + 5, 2, true);

        for (int x = 0; x < TIME_BAR_W; x++) {
            for (int y = 0; y < TIME_BAR_H; y++) {
                vram[(CPU_Y + y) * PC98_W + CPU_BAR_X + x] = 2;
            }
        }
        putSmall("CPU", CPU_X, CPU_Y, 2, true);
        putSmall("POWER", CPU_X + 17, CPU_Y, 2, true);
        putSmall("COUNT", CPU_X + 17, CPU_Y + 7, 2, true);
        vramblit(CPU_TRI_X, CPU_TRI_Y, s_filebar_tri, 0, FILEBAR_TRI_W, FILEBAR_TRI_H);
        for (int x = 0; x < TIME_BAR_W; x++) {
            for (int y = 0; y < TIME_BAR_H; y++) {
                vram[(CPU_Y + y) * PC98_W + FPS_BAR_X + x] = 2;
            }
        }
        putSmall("FRAMES", FPS_X, CPU_Y, 2, true);
        putSmall("PER", FPS_X + 32, CPU_Y, 2, true);
        putSmall("SECOND", FPS_X + 17, CPU_Y + 7, 2, true);
        vramblit(FPS_TRI_X, CPU_TRI_Y, s_filebar_tri, 0, FILEBAR_TRI_W, FILEBAR_TRI_H);
        for (int x = 0; x < 322; x++) {
            vram[132 * PC98_W + 312 + x] = 7;
        }
        putSmall("SENS", SPECTRUM_X - 40, SPECTRUM_Y - 6, 7, true);
        putSmall("-48", SPECTRUM_X - 19, SPECTRUM_Y - 6, 7, true);
        putSmall("0", SPECTRUM_X - 9, SPECTRUM_Y - 63, 7, true);
        putSmall("dB", SPECTRUM_X - 14, SPECTRUM_Y - 71, 7, true);
        putSmall("SPECTRUM", SPECTRUM_X + 197, SPECTRUM_Y - 71, 7, true);
        putSmall("ANAL", SPECTRUM_X + 241, SPECTRUM_Y - 71, 7, true);
        putSmall("YzER", SPECTRUM_X + 260, SPECTRUM_Y - 71, 7, true);
        for (int y = 0; y < 63; y++) {
            vram[(SPECTRUM_Y - y) * PC98_W + SPECTRUM_X - 2] = 2;
            if ((y % 2) == 0) {
                vram[(SPECTRUM_Y - y) * PC98_W + SPECTRUM_X - 3] = 2;
            }
            if ((y % 8) == 0) {
                vram[(SPECTRUM_Y - y) * PC98_W + SPECTRUM_X - 4] = 2;
            }
        }
        putSmall("FREQ", SPECTRUM_X - 24, SPECTRUM_Y + 1, 1, true);
        for (int x = 0; x < 17; x++) {
            vram[(SPECTRUM_Y + 4) * PC98_W + SPECTRUM_X + 1 + 2 * x] = 1;
        }
        putSmall("250", SPECTRUM_X + 36, SPECTRUM_Y + 1, 1, true);
        for (int x = 0; x < 15; x++) {
            vram[(SPECTRUM_Y + 4) * PC98_W + SPECTRUM_X + 52 + 2 * x] = 1;
        }
        putSmall("500", SPECTRUM_X + 83, SPECTRUM_Y + 1, 1, true);
        for (int x = 0; x < 17; x++) {
            vram[(SPECTRUM_Y + 4) * PC98_W + SPECTRUM_X + 99 + 2 * x] = 1;
        }
        putSmall("1", SPECTRUM_X + 133, SPECTRUM_Y + 1, 1, true);
        putSmall("k", SPECTRUM_X + 133 + 6, SPECTRUM_Y + 1, 1, true);
        for (int x = 0; x < 19; x++) {
            vram[(SPECTRUM_Y + 4) * PC98_W + SPECTRUM_X + 144 + 2 * x] = 1;
        }
        putSmall("2k", SPECTRUM_X + 183, SPECTRUM_Y + 1, 1, true);
        for (int x = 0; x < 18; x++) {
            vram[(SPECTRUM_Y + 4) * PC98_W + SPECTRUM_X + 193 + 2 * x] = 1;
        }
        putSmall("4k", SPECTRUM_X + 230, SPECTRUM_Y + 1, 1, true);
        for (int x = 0; x < 20; x++) {
            vram[(SPECTRUM_Y + 4) * PC98_W + SPECTRUM_X + 240 + 2 * x] = 1;
        }
        putSmall("ON", LEVEL_TEXT_X + 5, LEVEL_TEXT_Y, 1, true);
        putSmall("PAN", LEVEL_TEXT_X, LEVEL_TEXT_Y + 8, 1, true);
        putSmall("PROG", LEVEL_TEXT_X - 5, LEVEL_TEXT_Y + 16, 1, true);
        putSmall("KEY", LEVEL_TEXT_X, LEVEL_TEXT_Y + 23, 1, true);
        LevelDataSource lv = source != null ? source.level() : null;
        boolean customLabels = false;
        if (lv != null) {
            for (int c = 0; c < FMDSP_LEVEL_COUNT; c++) {
                String label = lv.label(c);
                if (label != null) {
                    customLabels = true;
                    // the source keeps these short enough for the span it labels
                    putSmall(label.length() > 4 ? label.substring(0, 4) : label,
                            LEVEL_X + LEVEL_W * c, LEVEL_TRACK_Y, 7, true);
                }
            }
        }
        if (!customLabels) {
            putSmall("FM1", LEVEL_X + LEVEL_W * 0, LEVEL_TRACK_Y, 7, true);
            putSmall("FM4", LEVEL_X + LEVEL_W * 3, LEVEL_TRACK_Y, 7, true);
            putSmall("SSG", LEVEL_X + LEVEL_W * 6, LEVEL_TRACK_Y, 7, true);
            putSmall("RHY", LEVEL_X + LEVEL_W * 9, LEVEL_TRACK_Y, 7, true);
            putSmall("ADP", LEVEL_X + LEVEL_W * 10, LEVEL_TRACK_Y, 7, true);
            putSmall("PPZ", LEVEL_X + LEVEL_W * 11, LEVEL_TRACK_Y, 7, true);
        }
        for (int y = 0; y < 63; y++) {
            vram[(LEVEL_Y + y) * PC98_W + LEVEL_X - 2] = 2;
            if ((y % 2) == 0) vram[(LEVEL_Y + y) * PC98_W + LEVEL_X - 3] = 2;
            if ((y % 8) == 6) vram[(LEVEL_Y + y) * PC98_W + LEVEL_X - 4] = 2;
        }
        putSmall("0", LEVEL_X - 9, LEVEL_Y - 1, 7, true);
        putSmall("-48", LEVEL_X - 19, LEVEL_Y + 56, 7, true);
    }

    // ==================================================================
    // per-track dynamic row: fmdsp_track_without_key + keyboard
    // ==================================================================

    private void trackWithoutKey(TrackId t, TrackStatus track, boolean masked, int y) {
        int tracknum = NUM_OF[t.ordinal()];
        TrackStatusSource ts = source != null ? source.trackStatus() : null;
        if (ts != null) {
            int custom = ts.trackNumber(t);
            if (custom >= 0) tracknum = custom;
        }
        int d1 = masked ? 10 : (tracknum / 10) % 10;
        int d2 = masked ? 10 : tracknum % 10;
        blitNum(NUM_X + NUM_W * 0, y + 1, d1);
        blitNum(NUM_X + NUM_W * 1, y + 1, d2);

        String info1 = "    ";
        String info2 = "    ";
        if (track.playing || track.info == TrackInfo.SSGEFF) {
            switch (track.info) {
            case PPZ8: info2 = "PPZ8"; break;
            case PDZF: info2 = "PDZF"; break;
            // sits on the KN: line, whose key it stands in for
            case STREAM: info2 = "STRM"; break;
            case SSGEFF:
                info1 = "EFF ";
                // FALLTHRU
            case SSG:
                if (track.ssgNoise) {
                    info2 = "%c%02X ".formatted(track.ssgTone ? 'M' : 'N', track.ssgNoiseFreq & 0x1f);
                }
                break;
            case FM3EX:
                info1 = "EX  ";
                StringBuilder b = new StringBuilder(4);
                for (int c = 0; c < 4; c++) b.append(track.fmSlotMask[c] ? ' ' : (char) ('1' + c));
                info2 = b.toString();
                break;
            default:
                break;
            }
        }
        putSmall(info1, TINFO_X, y + 0, 2, true);
        putSmall(info2, TINFO_X, y + 6, 2, true);

        String notestr = " S  ";
        if (track.playing) {
            if ((track.key & 0xf) == 0xf) {
                notestr = " R  ";
            } else {
                String keystr = (track.key & 0xf) < KEY_TABLE.length ? KEY_TABLE[track.key & 0xf] : "  ";
                notestr = "o" + (track.key >> 4) + keystr;
            }
        }
        putSmall("KN:", TDETAIL_X, y + 6, 1, true);
        putSmall(notestr, TDETAIL_KN_V_X, y + 6, 1, true);
        putSmall("TN:", TDETAIL_TN_X, y + 6, 1, true);
        putSmall("%03d".formatted(track.toneNum), TDETAIL_TN_V_X, y + 6, 1, true);
        putSmall("Vl", TDETAIL_VL_X, y + 6, 1, true);
        putSmall(":", TDETAIL_VL_C_X, y + 6, 1, true);
        putSmall("%03d".formatted(track.volume), TDETAIL_VL_V_X, y + 6, 1, true);
        putSmall("GT:", TDETAIL_GT_X, y + 6, 1, true);
        putSmall("%03d".formatted(track.gate), TDETAIL_GT_V_X, y + 6, 1, true);
        putSmall("DT:", TDETAIL_DT_X, y + 6, 1, true);
        putSmall("%03d".formatted(track.detune > 0 ? track.detune : -track.detune),
                TDETAIL_DT_V_X, y + 6, 1, true);
        int sign = track.detune == 0 ? 0 : (track.detune < 0 ? 1 : 2);
        vramblit(TDETAIL_DT_S_X, y + 6 + 2, s_dt_sign, sign * (DT_SIGN_W * DT_SIGN_H), DT_SIGN_W, DT_SIGN_H);
        putSmall("M:", TDETAIL_M_X, y + 6, 1, true);
        putSmall(track.status, TDETAIL_M_V_X, y + 6, 1, true);

        int colorOn = ((track.key == 0xff) || masked) ? 7 : 2;
        if (!track.playing) colorOn = 3;
        vramblitColor(BAR_L_X, y + BAR_Y, s_bar_l, 0, BAR_L_W, BAR_H, colorOn);
        for (int i = 0; i < BAR_CNT; i++) {
            int c = (i < (track.ticksLeft >> 2)) ? colorOn : 3;
            vramblitColor(BAR_X + BAR_W * i, y + BAR_Y, s_bar, 0, BAR_W, BAR_H, c);
        }
        vramblitColor(BAR_X + BAR_W * (track.ticks >> 2), y + BAR_Y, s_bar, 0, BAR_W, BAR_H, 7);

        freqWave(t, track, masked, y);
    }

    /**
     * The dim strokes the frequency fluctuation display rests on, drawn with the rest of the row's
     * chrome so that a row nothing is playing on shows the field rather than a hole.
     */
    private void freqField(int y) {
        for (int i = 0; i < FREQ_FIELD; i++) {
            vramblitColor(FREQ_X + BAR_W * i, y + BAR_Y, s_bar, 0, BAR_W, BAR_H, 3);
        }
    }

    /**
     * FMDSP.DOC's "9", the frequency fluctuation display: a wave flows through the left of the
     * field whenever an LFO or a pitch bend is moving the note off the pitch it was struck at, and
     * the field lies flat while it is not.
     * <p>
     * The original is in FMDSP.COM alone - 98fmplayer's fmdsp never had it - so what is reproduced
     * here is what the FMDSP screen does: eight strokes of the note bar's own kind over the "M:"
     * LFO field, each drawn from the centre line out to the wave, so that no fluctuation is a flat
     * line of single pixels and a deep one a row of tall strokes. Like the original it is an
     * indicator and not a measurement - the doc says as much - the movement being a wave of a fixed
     * shape and speed whose depth follows the pitch, rather than the pitch curve itself.
     */
    private void freqWave(TrackId t, TrackStatus track, boolean masked, int y) {
        int row = t.ordinal();
        double target = fluctuation(track);
        // rises with the note and falls back slowly, so that a vibrato turning through zero twice
        // a cycle does not blink the display off and on again
        freqAmp[row] += (target - freqAmp[row]) * (target > freqAmp[row] ? 0.5 : 0.12);
        if (!track.playing) {
            freqAmp[row] = 0;
            freqPhase[row] = 0;
            return;
        }
        freqPhase[row] = (freqPhase[row] + FREQ_SPEED) % (2 * Math.PI);

        // the note bar's colours, so the two read as the one row: 7 for a keyless or masked row
        int color = (track.key == 0xff || masked) ? 7 : 2;
        for (int i = 0; i < FREQ_CNT; i++) {
            int x = FREQ_X + BAR_W * i;
            // the wave takes the slot over from the field's dim stroke rather than sitting in it
            for (int yi = 0; yi < BAR_H; yi++) {
                vram[(y + BAR_Y + yi) * PC98_W + x] = 0;
            }
            // one period of the wave spans the slots, and every other frame moves it one along
            double v = freqAmp[row] * FREQ_AMP
                    * Math.sin(freqPhase[row] - i * (2 * Math.PI / FREQ_CNT));
            int d = (int) Math.round(v);
            for (int yi = Math.min(0, d); yi <= Math.max(0, d); yi++) {
                vram[(y + FREQ_MID_Y + yi) * PC98_W + x] = (byte) color;
            }
        }
    }

    /**
     * How far off the pitch it was struck at a row is sounding, 0 (steady) to 1
     * ({@link #FREQ_FULL_CENTS} or more). A source that measures it says so in
     * {@link TrackStatus#pitchDeviation}; one that does not leaves the semitone the keyboard
     * already shows - {@link TrackStatus#actualKey} against {@link TrackStatus#key} - and its
     * driver's own LFO flags, which is all the original had to go on for the FM drivers it was
     * written for.
     */
    private static double fluctuation(TrackStatus track) {
        if (!track.playing || track.key == 0xff || (track.key & 0xf) == 0xf) return 0;
        int cents = Math.abs(track.pitchDeviation);
        if (cents == 0 && track.actualKey != 0xff && (track.actualKey & 0xf) != 0xf) {
            cents = Math.abs(noteOf(track.actualKey) - noteOf(track.key)) * 100;
        }
        double a = Math.min(1.0, cents / (double) FREQ_FULL_CENTS);
        if (a == 0 && pitchFlagged(track.status)) a = 0.5;
        return a;
    }

    /**
     * Whether the "M:" field says something is moving this row's pitch. Every driver spells a
     * pitch LFO or a portamento with P, and FMP its second and third with Q and R; A (volume), S
     * (sync) and e (envelope) move the level instead, and H is the chip's own hardware LFO, which
     * the original leaves out of this display and so does this.
     */
    private static boolean pitchFlagged(String status) {
        for (int i = 0; i < status.length(); i++) {
            char c = status.charAt(i);
            if (c == 'P' || c == 'Q' || c == 'R') return true;
        }
        return false;
    }

    /** semitones above C0 of a packed {@code octave << 4 | note} key */
    private static int noteOf(int key) {
        return (key >> 4) * 12 + (key & 0xf);
    }

    // ==================================================================
    // dynamic frame: fmdsp_update_10 (ORIGINAL style)
    // ==================================================================

    private void update10() {
        TrackStatusSource ts = source != null ? source.trackStatus() : null;

        TrackId[] disp = tracks();
        for (int it = 0; it < disp.length; it++) {
            TrackId t = disp[it];
            if (ts != null) {
                ts.readStatus(t, scratch);
            } else {
                resetStatus(scratch);
            }
            boolean masked = ts != null && ts.masked(t);
            trackWithoutKey(t, scratch, masked, TRACK_H * it);
            for (int i = 0; i < KEY_OCTAVES; i++) {
                vramblit(KEY_X + KEY_W * i, TRACK_H * it + KEY_Y, s_key_bg, 0, KEY_W, KEY_H);
                if (scratch.playing || scratch.info == TrackInfo.SSGEFF) {
                    if ((scratch.actualKey >> 4) == i) {
                        vramblitKey(KEY_X + KEY_W * i, TRACK_H * it + KEY_Y, s_key_mask, 0,
                                KEY_W, KEY_H, scratch.actualKey & 0xf, 8);
                    }
                    if ((scratch.key >> 4) == i) {
                        vramblitKey(KEY_X + KEY_W * i, TRACK_H * it + KEY_Y, s_key_mask, 0,
                                KEY_W, KEY_H, scratch.key & 0xf, masked ? 8 : 6);
                    }
                }
            }
        }

        if (rightMode == RightMode.TRACK_INFO) {
            trackInfo10(disp);
            return;
        }

        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        cpuusage = (int) (operatingSystemMXBean.getSystemLoadAverage() * 100);

        renderControlAndCounters();
        renderCircle();
        renderFft();
        renderLevel();
    }

    // ==================================================================
    // right half: update_track_info_10 (RightMode.TRACK_INFO)
    // ==================================================================

    /** x the right half starts at, which is the whole of it for a mode that is not the default */
    private static final int RIGHT_X = 320;

    /** the mnemonic of a bar line, at the end of the bar */
    private static final int TINFO_STATE_X = 130;

    /** the value behind the mnemonic */
    private static final int TINFO_VALUE_X = 150;

    /** the F-number and its like, right of both */
    private static final int TINFO_EXTRA_X = 170;

    /** one bar line, {@code TRACK_H} taking four of them */
    private static final int TINFO_LINE_H = 6;

    private final TrackDetail detail = new TrackDetail();

    /**
     * Draws what each displayed row is doing inside its chip down the right half, against the same
     * rows the left half shows ({@code update_track_info_10}). A row whose source has no detail for
     * it is left blank, the way the original leaves a track type it has no panel for.
     */
    private void trackInfo10(TrackId[] disp) {
        TrackDetailSource td = source != null ? source.trackDetail() : null;
        if (td == null) return;
        for (int it = 0; it < disp.length; it++) {
            detail.clear();
            if (td.readDetail(disp[it], detail)) {
                trackDetail(detail, RIGHT_X, TRACK_H * it);
            }
        }
    }

    /**
     * One row's panel: its bar lines, or - with none - the titled register dump
     * ({@code update_track_info_fm} and the three beside it).
     */
    private void trackDetail(TrackDetail d, int x, int y) {
        for (int li = 0; li < d.lines && li < TrackDetail.LINES; li++) {
            int ly = y + TINFO_LINE_H * li;
            if (d.bar[li] >= 0) {
                int mark = d.mark[li];
                int markEnd = mark + Math.max(1, d.markWidth[li]);
                for (int px = 0; px < TrackDetail.COLUMNS; px++) {
                    // the bar is drawn every other pixel, as the original's dotted one is
                    int color = px < d.bar[li] ? 2 : 3;
                    if (mark >= 0 && px >= mark && px < markEnd) color = 7;
                    for (int py = 0; py < 4; py++) {
                        vram[(ly + 2 + py) * PC98_W + x + px * 2] = (byte) color;
                    }
                }
            }
            putSmall(d.state[li], x + TINFO_STATE_X, ly, 1, false);
            putSmall(d.value[li], x + TINFO_VALUE_X, ly, 1, false);
            putSmall(d.extra[li], x + TINFO_EXTRA_X, ly, 1, false);
        }
        if (d.lines == 0) {
            putSmall(d.header, x, y, 1, false);
            putSmall(d.text, x, y + TINFO_LINE_H, 1, false);
        }
    }

    private static void resetStatus(TrackStatus s) {
        s.playing = false;
        s.info = TrackInfo.NORMAL;
        s.ticks = 0;
        s.ticksLeft = 0;
        s.key = 0xff;
        s.actualKey = 0xff;
        s.toneNum = 0;
        s.volume = 0;
        s.gate = 0;
        s.detune = 0;
        s.pitchDeviation = 0;
        s.status = "";
        Arrays.fill(s.fmSlotMask, false);
        s.ppz8Ch = 0;
        s.ssgTone = false;
        s.ssgNoise = false;
        s.ssgNoiseFreq = 0;
    }

    private void renderControlAndCounters() {
        WorkStateSource w = source != null ? source.work() : null;
        boolean playing = w != null && w.playing() && !w.paused();
        boolean stopped = w == null || !w.playing();
        boolean paused = w != null && w.paused();
        vramblitColor(PLAY_X, PLAY_Y, s_play, 0, PLAY_W, PLAY_H, playing ? 2 : 3);
        vramblitColor(STOP_X, STOP_Y, s_stop, 0, STOP_W, STOP_H, stopped ? 2 : 3);
        vramblitColor(PAUSE_X, PAUSE_Y, s_pause, 0, PAUSE_W, PAUSE_H, paused ? 2 : 3);
        vramblit(FADE_X, FADE_Y, s_fade, 0, FADE_W, FADE_H);
        vramblit(FF_X, FF_Y, s_ff, 0, FF_W, FF_H);
        vramblit(REW_X, REW_Y, s_rew, 0, REW_W, REW_H);
        vramblitColor(FLOPPY_X, FLOPPY_Y, s_floppy, 0, FLOPPY_W, FLOPPY_H, 3);
        boolean hasPcm0 = w != null && w.pcmFilename(0) != null;
        boolean hasPcm1 = w != null && w.pcmFilename(1) != null;
        boolean hasPcm2 = w != null && w.pcmFilename(2) != null;
        boolean hasPcm3 = w != null && w.pcmFilename(3) != null;
        boolean hasAnyPcm = hasPcm0 || hasPcm1 || hasPcm2 || hasPcm3;
        if (hasAnyPcm) {
            vramblitColorSub(FLOPPY_X, FLOPPY_Y, s_floppy, FLOPPY_W, 0, 50, FLOPPY_H, 2);
        }
        if (hasPcm0) {
            int c0 = w.pcmError(0) ? 8 : 2;
            vramblitColorSub(FLOPPY_X, FLOPPY_Y, s_floppy, FLOPPY_W, 50, 5, FLOPPY_H, c0);
        }
        if (hasPcm1) {
            int c1 = w.pcmError(1) ? 8 : 2;
            vramblitColorSub(FLOPPY_X, FLOPPY_Y, s_floppy, FLOPPY_W, 55, 7, FLOPPY_H, c1);
        }
        if (hasPcm2) {
            int c2 = w.pcmError(2) ? 8 : 2;
            vramblitColorSub(FLOPPY_X, FLOPPY_Y, s_floppy, FLOPPY_W, 62, 6, FLOPPY_H, c2);
        }
        if (hasPcm3) {
            int c3 = w.pcmError(3) ? 8 : 2;
            vramblitColorSub(FLOPPY_X, FLOPPY_Y, s_floppy, FLOPPY_W, 68, 6, FLOPPY_H, c3);
        }

        long frames = w != null ? w.generatedFrames() : 0L;
        int srate = w != null ? Math.max(1, w.sampleRate()) : 55467;
        int ssec = (int) ((frames % srate) * 100 / srate);
        long sec = frames / srate;
        long min = sec / 60;
        sec %= 60;
        blitNum(TIME_X + NUM_W * 0, TIME_Y, (int) ((min / 10) % 10));
        blitNum(TIME_X + NUM_W * 1, TIME_Y, (int) (min % 10));
        vramblit(TIME_X + NUM_W * 2, TIME_Y, s_num_colon, (int) (sec % 2) * (NUM_W * NUM_H), NUM_W, NUM_H);
        blitNum(TIME_X + NUM_W * 3, TIME_Y, (int) ((sec / 10) % 10));
        blitNum(TIME_X + NUM_W * 4, TIME_Y, (int) (sec % 10));
        vramblit(TIME_X + NUM_W * 5, TIME_Y, s_num_bar, 0, NUM_W, NUM_H);
        blitNum(TIME_X + NUM_W * 6, TIME_Y, (ssec / 10) % 10);
        blitNum(TIME_X + NUM_W * 7, TIME_Y, ssec % 10);

        long clock = w != null ? w.timerBCount() : 0L;
        long c = clock;
        for (int i = 0; i < 8; i++) {
            blitNum(TIME_X + NUM_W * (7 - i), CLOCK_Y, (int) (c % 10));
            c /= 10;
        }
        int timerb = w != null ? w.timerB() & 0xff : 0;
        int tb = timerb;
        for (int i = 0; i < 3; i++) {
            blitNum(TIME_X + NUM_W * (7 - i), TIMERB_Y, tb % 10);
            tb /= 10;
        }
        int loop = w != null ? w.loopCount() & 0xff : 0;
        int lp = loop;
        for (int i = 0; i < 4; i++) {
            blitNum(TIME_X + NUM_W * (7 - i), LOOPCNT_Y, lp % 10);
            lp /= 10;
        }

        renderVolumeDown(w);

        // loop / duration progress bar
        long loopLen = w != null ? w.loopTimerBCount() : 0L;
        long loopPos = w != null ? w.timerBCountLoop() : 0L;
        if (loopLen <= 0 && w != null) {
            // no loop measurement yet: fall back to total song length (if known)
            loopLen = w.totalTimerBCount();
            loopPos = w.timerBCount();
        }
        if (loopLen <= 0 && w != null) {
            // still no length known: spin the slider with a fixed 1200-tick period (~10 s at TimerB=200)
            loopLen = 1200;
            loopPos = w.timerBCount();
        }
        int pos = 0;
        if (loopLen > 0) {
            pos = (int) ((loopPos % loopLen) * (72 + 1 - 4) / loopLen);
        }
        pos = Math.clamp(pos, 0, 69);

        boolean wplaying = w != null && w.playing() && !w.paused();
        for (int x = 0; x < 72; x++) {
            if (x == 0 || x == 36 || x == 71) {
                vram[(70 - 2) * PC98_W + 352 + x * 2] = 7;
            } else if ((x % 9) == 0) {
                vram[(70 - 2) * PC98_W + 352 + x * 2] = 3;
            }
            int cc = 3;
            if (wplaying && (pos <= x) && (x < (pos + 4))) cc = 2;
            for (int y = 0; y < 4; y++) {
                vram[(70 + y) * PC98_W + 352 + x * 2] = (byte) cc;
            }
        }
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 4; y++) {
                vram[(70 + y) * PC98_W + 496 + x] = (byte) (loop != 0 ? 7 : 3);
            }
        }

        // cpu / fps
        int cu = cpuusage;
        for (int i = 0; i < 3; i++) {
            blitNum(CPU_NUM_X + NUM_W * (2 - i), CPU_NUM_Y, cu % 10);
            cu /= 10;
        }
        int fp = fps;
        for (int i = 0; i < 3; i++) {
            blitNum(FPS_NUM_X + NUM_W * (2 - i), CPU_NUM_Y, fp % 10);
            fp /= 10;
        }
    }

    /**
     * The {@code VOLUME DOWN} counter of FMDSP.DOC's {@code ▽各種ステータス表示}: how far the player
     * is turning a part down, for one part at a time - "表示は変更があったパートに自動的に切り替わり
     * ます", so the counter follows whichever of FM, SSG, RHY and PCM moved last.
     * <p>
     * The row is drawn but never filled by 98fmplayer's fmdsp, which has no such number to show;
     * here it comes through {@link WorkStateSource#volumeDown}, where mdplayer's generic source
     * answers with the calibrated mixer balance of the chip playing that part and a driver-specific
     * one with the driver's own correction. Laid out as the PC-98 capture has it: the part's name
     * in the small font, then three counter digits with no leading zeros, the minus sitting in the
     * blank place in front of the number.
     */
    private void renderVolumeDown(WorkStateSource w) {
        WorkStateSource.VolumePart[] parts = WorkStateSource.VolumePart.values();
        WorkStateSource.VolumePart changed = null;
        for (int i = 0; i < parts.length; i++) {
            int v = w != null ? w.volumeDown(parts[i]) : 0;
            // several parts move together when a song starts; the one worth showing is a part that
            // is actually turned down, not whichever of them is last in the enum
            if (volDownPart != null && v != volDown[i] && (changed == null || v != 0)) {
                changed = parts[i];
            }
            volDown[i] = v;
        }
        if (changed != null) {
            volDownPart = changed;
        } else if (volDownPart == null) {
            // the first reading is not a change: open on a part that has a correction
            volDownPart = parts[0];
            for (int i = 0; i < parts.length; i++) {
                if (volDown[i] != 0) {
                    volDownPart = parts[i];
                    break;
                }
            }
        }

        int v = volDown[volDownPart.ordinal()];
        putSmall((volDownPart.name() + "   ").substring(0, 3), VOLDOWN_PART_X, VOLDOWN_PART_Y, 2, true);

        int abs = Math.min(999, Math.abs(v));
        int digits = abs >= 100 ? 3 : abs >= 10 ? 2 : 1;
        boolean minus = v < 0;
        // the sign is a digit place of its own, lit as the middle segment alone - the counter has
        // no minus glyph, and the leading blank is exactly the shape one would be drawn on. Only a
        // number that fills all three digits pushes it out to the place beside the field, which
        // otherwise shows the bare segment unlit
        blitMidBar(VOLDOWN_SIGN_X, VOLDOWN_Y, minus && digits == VOLDOWN_DIGITS ? 2 : 3);
        for (int i = 0; i < VOLDOWN_DIGITS; i++) {
            int x = VOLDOWN_NUM_X + NUM_W * i;
            int place = VOLDOWN_DIGITS - 1 - i;
            if (place < digits) {
                int p = 1;
                for (int d = 0; d < place; d++) p *= 10;
                blitNum(x, VOLDOWN_Y, (abs / p) % 10);
            } else {
                // no leading zeros: the places the number does not reach are blank glyphs, and the
                // first of them carries the sign
                blitNum(x, VOLDOWN_Y, 10);
                if (minus && place == digits) blitMidBar(x, VOLDOWN_Y, 2);
            }
        }
    }

    /** the middle segment of a digit cell on its own, which is what the counter's minus is */
    private void blitMidBar(int x, int y, int color) {
        for (int i = 0; i < VOLDOWN_BAR_W; i++) {
            vram[(y + VOLDOWN_BAR_Y) * PC98_W + x + VOLDOWN_BAR_X + i] = (byte) color;
        }
    }

    private void renderCircle() {
        WorkStateSource w = source != null ? source.work() : null;
        boolean wplaying = w != null && w.playing();
        boolean wpaused = w != null && w.paused();
        long timerbcnt = w != null ? w.timerBCount() : 0L;
        int clock = (int) ((timerbcnt / 8) % 8);
        for (int y = 0; y < CIRCLE_H; y++) {
            for (int x = 0; x < CIRCLE_W; x++) {
                int cc = 0;
                int p = s_circle[y * CIRCLE_W + x] & 0xff;
                if (p != 0) {
                    cc = (wplaying && (!wpaused || (framecnt % 60) < 30) && (p == (clock + 1))) ? 2 : 3;
                }
                vram[(CIRCLE_Y + y) * PC98_W + CIRCLE_X + x] = (byte) cc;
            }
        }
    }

    private void renderFft() {
        FftDataSource fftSrc = source != null ? source.fft() : null;
        Arrays.fill(fftScratch, 0);
        if (fftSrc != null) {
            fftSrc.readFft(fftScratch);
        }
        for (int x = 0; x < FFTDISPLEN; x++) {
            int v = fftScratch[x];
            if (v < 0) v = 0;
            if (v > 32) v = 32;
            fftScratch[x] = v;
            for (int y = 0; y < 32; y++) {
                int px = SPECTRUM_X + x * 4;
                int py = SPECTRUM_Y - y * 2;
                int cc = y < v ? 2 : 3;
                int base = py * PC98_W + px;
                vram[base] = (byte) cc;
                vram[base + 1] = (byte) cc;
                vram[base + 2] = (byte) cc;
            }
        }
        for (int i = 0; i < FFTDISPLEN; i++) {
            if (fftData[i] <= fftScratch[i]) {
                fftData[i] = fftScratch[i];
                fftCnt[i] = 30;
            } else {
                if (fftCnt[i] != 0) {
                    fftCnt[i]--;
                } else if (fftData[i] != 0) {
                    if (fftDropDiv[i] != 0) {
                        fftDropDiv[i]--;
                    } else {
                        fftDropDiv[i] = DIVTAB[Math.min(15, fftData[i] / 2)];
                        fftData[i]--;
                    }
                }
            }
        }
        for (int x = 0; x < FFTDISPLEN; x++) {
            int px = SPECTRUM_X + x * 4;
            int py = SPECTRUM_Y - fftData[x] * 2;
            int base = py * PC98_W + px;
            vram[base] = 7;
            vram[base + 1] = 7;
            vram[base + 2] = 7;
        }
    }

    private void renderLevel() {
        LevelDataSource lvl = source != null ? source.level() : null;
        TrackStatusSource ts = source != null ? source.trackStatus() : null;

        for (int c = 0; c < FMDSP_LEVEL_COUNT; c++) {
            int level = lvl != null ? Math.clamp(lvl.level(c), 0, 32767) : 0;
            int pan = lvl != null ? panIndex(lvl.pan(c)) : 5;

            TrackId tid = levelToTrack(c);
            int prog = 0;
            int key = 0xff;
            boolean playing = false;
            boolean masked = false;
            if (c != 9 && tid != null && ts != null) {
                ts.readStatus(tid, scratch);
                prog = scratch.toneNum;
                key = scratch.key;
                playing = scratch.playing;
                masked = ts.masked(tid);
                if (scratch.info == TrackInfo.PDZF || scratch.info == TrackInfo.PPZ8) {
                    playing = false;
                }
            }
            if (!playing) pan = 5;

            int llevel = 0;
            if (level != 0) {
                double db = 20.0 * Math.log10((double) level / (1 << 15));
                double f = (db / 48.0 + 1.0) * 32.0;
                if (f > 0.0) llevel = (int) f;
            }
            if (levelData[c] <= llevel) {
                levelData[c] = llevel;
                levelCnt[c] = 30;
            } else {
                if (levelCnt[c] != 0) {
                    levelCnt[c]--;
                } else if (levelData[c] != 0) {
                    if (levelDropDiv[c] != 0) {
                        levelDropDiv[c]--;
                    } else {
                        levelDropDiv[c] = DIVTAB[Math.min(15, levelData[c] / 2)];
                        levelData[c]--;
                    }
                }
            }
            for (int y = 0; y < 64; y += 2) {
                int plevel = (63 - y) / 2;
                for (int x = 0; x < LEVEL_DISP_W; x++) {
                    int color = llevel > plevel ? 2 : 3;
                    if (plevel == levelData[c]) color = 7;
                    vram[(y + LEVEL_Y) * PC98_W + LEVEL_X + LEVEL_W * c + x] = (byte) color;
                }
            }
            vramblitColor(LEVEL_X + LEVEL_W * c - 1, PANPOT_Y, s_panpot, pan * (PANPOT_W * PANPOT_H),
                    PANPOT_W, PANPOT_H, masked ? 5 : 1);
            if (c != 9) {
                putSmall("%03d".formatted(prog), LEVEL_X + LEVEL_W * c, LEVEL_PROG_Y, 1, true);
            }
            String buf = "---";
            if (c != 9 && playing) {
                int oct = (key >> 4) & 0xf;
                int n = key & 0xf;
                if (n < 12) {
                    buf = "%03d".formatted(oct * 12 + n);
                }
            }
            putSmall(buf, LEVEL_X + LEVEL_W * c, LEVEL_KEY_Y, 1, true);
        }
    }

    private static int panIndex(LevelDataSource.Pan p) {
        if (p == null) return 5;
        return switch (p) {
            case LEFT -> 0;
            case MID_LEFT -> 1;
            case CENTER -> 2;
            case MID_RIGHT -> 3;
            case RIGHT -> 4;
            default -> 5;
        };
    }

    /** the row a meter column reads its key, pan and tone number from, see {@link LevelDataSource#track} */
    private TrackId levelToTrack(int channel) {
        LevelDataSource lv = source != null ? source.level() : null;
        return lv != null ? lv.track(channel) : null;
    }
}
