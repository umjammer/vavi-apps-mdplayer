/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import javax.swing.JFrame;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import mdplayer.Audio;
import mdplayer.ChipFmDspSource;
import mdplayer.Common;
import mdplayer.MidiOutInfo;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.FileFormat;
import mdplayer.driver.BasePlugin;
import musicDriverInterface.MetaData;
import vavi.sound.visualizer.fmdsp.FmDspVisualizer;
import vavi.sound.visualizer.fmdsp.LeftMode;
import vavi.sound.visualizer.fmdsp.RightMode;
import vavi.util.Debug;
import vavi.util.archive.Archives;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavi.util.serdes.Serdes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-06-03 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    /**
     * how many times a song loops before it fades out and {@link Audio#play()} returns.
     * 0 or less means loop forever (no fade out, stop it by hand).
     * overridable by {@code -Dvavi.test.loopTimes=...}
     */
    @Property(name = "vavi.test.loopTimes", useSystem = true)
    int loopTimes = 2;

    @Property
    String file;

    /** 1 origin */
    @Property
    int track;

    @Property(name = "mdplayer.pmd.dir")
    String pmdDir;
    @Property(name = "mdplayer.fmp.dir")
    String fmpDir;
    @Property(name = "mdplayer.fmp.pvi")
    String fmpPvi;
    @Property(name = "mdplayer.zms.dir")
    String zmsDir;
    @Property(name = "mdplayer.mgs.dir")
    String mgsDir;
    @Property(name = "mdplayer.ndp.dir")
    String ndpDir;
    @Property(name = "mdplayer.musica.dir")
    String musicaDir;
    @Property(name = "muap.dir.dta")
    String muapDirDta;
    @Property(name = "muap.dir.pcm")
    String muapDirPcm;

    /** where the chip roms are kept - the OPL4's wave rom, the OPNA's rhythm samples */
    @Property(name = "mdsound.pcm.path")
    String pcmPath;
    @Property(name = "mdplayer.ma7.path")
    String ma7Path;

    @Property(name = "mdplayer.variant.pcm8")
    int variantPcm8;
    @Property(name = "mdplayer.variant.mpcm")
    int variantMpcm;
    @Property(name = "mdplayer.variant.ym2151")
    int variantYm2151;
    @Property(name = "mdplayer.variant.ym2413")
    int variantYm2413;
    @Property(name = "mdplayer.variant.ymf262")
    int variantYmf262;
    @Property(name = "mdplayer.variant.ay8910")
    int variantAy8910;

    @Property
    String dir;

    @Property
    String ext;

    /** PC-98 font rom, the comment lines have no Japanese glyphs without it */
    @Property(name = "fmdsp.fontRom")
    String fontRom;

    @Property(name = "multi.source")
    String multiSource;

    @Property(name = "multi.1")
    String multi1;

    @Property(name = "multi.2")
    String multi2;

    @Property
    String effects;

    static final boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static final long time = onIde ? 1000 * 1000 : 10 * 1000;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);

            // pmd
            System.setProperty("mdplayer.pmd.pmd", pmdDir);
            // fmp
            System.setProperty("mdplayer.fmp.dir", fmpDir);
            System.setProperty("mdplayer.fmp.pvi", fmpPvi);
            // zms
            System.setProperty("mdplayer.zms.dir", zmsDir);
            // mgsdrv
            System.setProperty("mdplayer.mgs.dir", mgsDir);
            // ndp
            System.setProperty("mdplayer.ndp.dir", ndpDir);
            // musica
            System.setProperty("mdplayer.musica.dir", musicaDir);
            // muap
            System.setProperty("muap.dir.dta", muapDirDta);
            System.setProperty("muap.dir.pcm", muapDirPcm);
            // the chip roms, which are nobody's to ship
            System.setProperty("mdsound.pcm.path", pcmPath);
//            System.setProperty("muap.dir.udp", muapDirUdp);
//            System.setProperty("muap.dir.sud", muapDirSud);

            // mfi
            System.setProperty("vavi.sound.ma7.path", ma7Path);
        }

        if (System.getProperty("os.name").startsWith("Mac") && effects != null) {
Debug.println("on mac, use AudioUnit effects: " + effects);
            System.setProperty("javax.sound.sampled.SourceDataLine", "#Rococoa Mixer"); // audio out is AudioUnit fixed
            System.setProperty("vavi.sound.sampled.rococoa.RococoaSourceDataLine.effects", effects);
        }

        System.setProperty("mdplayer.variant.pcm8", String.valueOf(variantPcm8));
        System.setProperty("mdplayer.variant.mpcm", String.valueOf(variantMpcm));
        System.setProperty("mdplayer.variant.ym2151", String.valueOf(variantYm2151));
        System.setProperty("mdplayer.variant.ym2413", String.valueOf(variantYm2413));
        System.setProperty("mdplayer.variant.ay8910", String.valueOf(variantAy8910));
        System.setProperty("mdplayer.variant.ymf262", String.valueOf(variantYmf262));

        System.setProperty("mdplayer.volume", "%4.2f".formatted(volume));
Debug.println("volume: " + volume + ", player.volume: " + System.getProperty("mdplayer.volume") + ", cwd: " + System.getProperty("user.dir") + ", time: " + time);
Debug.println("settings\n" +
        "mdplayer.pmd.pmd: " + System.getProperty("mdplayer.pmd.pmd") + "\n" +
        "mdplayer.fmp.dir: " + System.getProperty("mdplayer.fmp.dir") + "\n" +
        "mdplayer.fmp.pvi: " + System.getProperty("mdplayer.fmp.pvi") + "\n" +
        "mdplayer.zms.dir: " + System.getProperty("mdplayer.zms.dir") + "\n" +
        "mdplayer.mgs.dir: " + System.getProperty("mdplayer.mgs.dir") + "\n" +
        "mdplayer.ndp.dir: " + System.getProperty("mdplayer.ndp.dir") + "\n" +
        "mdplayer.musica.dir: " + System.getProperty("mdplayer.musica.dir") + "\n" +
        "vavi.sound.ma7.path: " + System.getProperty("vavi.sound.ma7.path") + "\n" +
        "muap.dir.dta: " + System.getProperty("muap.dir.dta") + "\n" +
        "muap.dir.pcm: " + System.getProperty("muap.dir.pcm") + "\n" +
        "mdplayer.variant.pcm8: " + System.getProperty("mdplayer.variant.pcm8") + "\n" +
        "mdplayer.variant.mpcm: " + System.getProperty("mdplayer.variant.mpcm") + "\n" +
        "mdplayer.variant.ay8910: " + System.getProperty("mdplayer.variant.ay8910") + "\n" +
        "mdplayer.variant.ym2413: " + System.getProperty("mdplayer.variant.ym2413") + "\n" +
        "mdplayer.variant.ymf262: " + System.getProperty("mdplayer.variant.ymf262") + "\n" +
        "mdplayer.variant.ym2151: " + System.getProperty("mdplayer.variant.ym2151"));

        System.setProperty("mdplayer.mfi.chip.default", "random");
        System.setProperty("vavi.sound.mobile.AudioEngine.disabled", "true");

        // the end-of-song detection in Audio#play() reads these: loop this many times, then fade out
        Setting.getInstance().getOther().setUseLoopTimes(loopTimes > 0);
        if (loopTimes > 0) {
            Setting.getInstance().getOther().setLoopTimes(loopTimes);
        }
Debug.println("loopTimes: " + (loopTimes > 0 ? String.valueOf(loopTimes) : "∞"));

        audio = Audio.getInstance(); // ⚠️ caution settings and system properties race condition
    }

    private Audio audio;

    /** */
    void play() throws Exception {
Debug.println("filename: " + file);
        FileFormat format = FileFormat.getFileFormat(file);
Debug.println("format: " + format.getClass().getSimpleName());
        format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(Path.of(file)))), null);
        var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of(
                "fileName", file,
                "midiMode", 0,
                "songNo", track - 1)
        );
Debug.println("plugin: " + plugin.getClass().getSimpleName());
        audio.init(plugin);
        audio.play();
Debug.print("done audio.play");
    }

    @Test
    @DisplayName("play one in local.properties")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {
        play();
    }

    // ^N to next song
    @Test
    @DisplayName("play random one in local.properties")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test2() throws Exception {
        playMulti(listFilesInLocalProperties());
    }

    /** files in local.properties includes commented out also */
    static List<Path> listFilesInLocalProperties() throws IOException {
        List<Path> paths = new ArrayList<>();
        Files.readAllLines(Paths.get("local.properties")).forEach(line -> {
            if (line.matches("^#?\\w+\\s*?=.*$")) {
                String file = line.substring(line.indexOf("=") + 1);
//System.err.println(file);
                Path path = Path.of(file);
                if (Files.exists(path) && !Files.isDirectory(path))
                    paths.add(path);
            }
        });
        return paths;
    }

    /** play list, nexting by hitting ^n */
    void playMulti(List<Path> files) throws Exception {
        AtomicReference<CountDownLatch> cdl = new AtomicReference<>();
        Random random = new Random(System.currentTimeMillis());

        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyReleased(NativeKeyEvent event) {
                int keyCode = event.getKeyCode();
//Debug.println("keyTyped: " + keyCode + ", " + ((event.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0));
                if ((event.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0 && keyCode == NativeKeyEvent.VC_N) {
Debug.print("countdown");
                    cdl.get().countDown();
                }
            }
        });

        int c = files.size();
        while (c > 0) {
            Path path = files.get(random.nextInt(files.size()));
            this.file = path.toString();
            cdl.set(new CountDownLatch(1));
Debug.print("play: " + file + " ---------------------------------------------------------------------");
            ExecutorService es = Executors.newSingleThreadExecutor();
            es.submit(() -> { try { play(); cdl.get().countDown(); } catch (Exception e) { Debug.printStackTrace(e); }});
Debug.print("await");
            cdl.get().await();
Debug.println("await: broke");
            es.shutdownNow();
Debug.println("stop");
            audio.stop();
            audio.close();
            c--;
        }
    }

    /**
     * @param dir separated by ';'
     * @param ext separated by ','
     */
    static List<Path> listFilesUnderDirFilteredByExt(String dir, String ext) {
Debug.println("dir: " + dir);
Debug.println("ext: " + ext);
        Predicate<Path> x = p -> Arrays.stream(ext.split(",")).anyMatch(e -> p.getFileName().toString().toUpperCase().endsWith(e));
        return Arrays.stream(dir.split(File.pathSeparator)).flatMap(d -> {
            try {
                return Files.walk(Paths.get(d)).filter(x);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).toList();
    }

    @Test
    @DisplayName("show meta data in the dir filtered by ext")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test3() throws Exception {
        listFilesUnderDirFilteredByExt(dir, ext).forEach(p -> {
            try {
                FileFormat format = FileFormat.getFileFormat(p.toString());
Debug.println(p);
                format.load(Files.newInputStream(p), null);
                MetaData music = format.getMetaData();
Debug.println(music);
            } catch (Exception _) {
            }
        });
    }

    @Test
    @DisplayName("play random one in the dir filtered by ext")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test4() throws Exception {
        playMulti(listFilesUnderDirFilteredByExt(dir, ext));
    }

    /** play list, nexting by time */
    void playMultiForTest(List<Path> files) throws Exception {
        for (Path path : files) {
            this.file = path.toString();
Debug.print("play: " + file + " ---------------------------------------------------------------------");
            FileFormat format = FileFormat.getFileFormat(file);
            format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(path))), null);
            var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
            plugin.setParams(format, Map.of("fileName", file));

            audio.init(plugin);
            CountDownLatch cdl = new CountDownLatch(1);
            ExecutorService es = Executors.newSingleThreadExecutor();
            es.submit(() -> { try { audio.play(); cdl.countDown(); } catch (Exception e) { Debug.printStackTrace(e); }});
Debug.print("await");
            cdl.await();
Debug.println("await: broke");
            es.shutdownNow();
Debug.println("stop");
            audio.stop();
            audio.close();
Debug.println("close");
        }
    }

    @Test
    @DisplayName("multi in local.properties")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test21() throws Exception {
        playMultiForTest(List.of(Path.of(multi1), Path.of(multi2)));
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void testX() throws Exception {
        mdplayer.Program.main(new String[] {});

        CountDownLatch cdl = new CountDownLatch(1);
        cdl.await();
    }

    @Test
    @DisplayName("test stop performance")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void testStopPerformance() throws Exception {
        String testFile = file;
        if (testFile == null || !Files.exists(Path.of(testFile))) {
            testFile = "src/test/resources/test.vgm";
        }
        int testTrack = track > 0 ? track : 1;

        FileFormat format = FileFormat.getFileFormat(testFile);
        format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(Path.of(testFile)))), null);
        var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of(
                "fileName", testFile,
                "midiMode", 0,
                "songNo", testTrack - 1)
        );
        audio.init(plugin);

        ExecutorService es = Executors.newSingleThreadExecutor();
        es.submit(() -> { try { audio.play(); } catch (Exception e) { Debug.printStackTrace(e); }});

        // Wait 1.5 seconds for the audio thread to start playing/rendering
        Thread.sleep(1500);

        long start = System.currentTimeMillis();
        audio.stop();
        long duration = System.currentTimeMillis() - start;

Debug.println("Stop took: " + duration + " ms");
        es.shutdownNow();

        assertTrue(duration < 1000, "Stop took too long: " + duration + " ms");
    }

    @Test
    void testJacksonSerialization() throws Exception {
        Setting setting = new Setting();
        setting.init();
        var midiOut = setting.getMidiOut();
        var list = new ArrayList<MidiOutInfo[]>();
        MidiOutInfo info1 = new MidiOutInfo();
        info1.id = 1;
        info1.name = "TestMIDI1";
        MidiOutInfo info2 = new MidiOutInfo();
        info2.id = 2;
        info2.name = "TestMIDI2";
        list.add(new MidiOutInfo[] {info1, info2});
        midiOut.setMidiOutInfos(list);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Serdes.Util.serialize(setting, baos);
        String xml = baos.toString(StandardCharsets.UTF_8);
        System.out.println("Serialized XML:\n" + xml);

        Setting loaded = new Setting();
        Serdes.Util.deserialize(new ByteArrayInputStream(baos.toByteArray()), loaded);
        System.out.println("Deserialized successfully!");
    }

    @Test
    @DisplayName("play anything w/ fmdsp visualizer via the generic chip source")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void testPlay() throws Exception {
Debug.println("filename: " + file);
        Audio audio = Audio.getInstance();

        FileFormat format = FileFormat.getFileFormat(file);
        format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(Path.of(file)))), null);
        var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", file));

        ChipFmDspSource source = new ChipFmDspSource();
        source.bind(plugin);
        source.setFilename(Path.of(file).getFileName().toString());

        FmDspVisualizer visualizer = new FmDspVisualizer(60, 2);
        visualizer.setTitle("MDDSP");
        visualizer.setVersion(Common.version.equals("undefined") ? null : Common.version);
        visualizer.setDataSource(source);
        if (fontRom != null && Files.exists(Path.of(fontRom))) {
            visualizer.setFontRom(Files.readAllBytes(Path.of(fontRom)));
        }

        JFrame frame = new JFrame();
        frame.setTitle(Path.of(file).getFileName() + " - MDDSP");
        frame.setLayout(new BorderLayout());
        frame.add(visualizer, BorderLayout.CENTER);
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code >= KeyEvent.VK_F1 && code <= KeyEvent.VK_F10) {
                    visualizer.setPaletteIndex(code - KeyEvent.VK_F1);
                } else if (code == KeyEvent.VK_F11) {
                    if (e.isShiftDown()) {
                        RightMode[] r = RightMode.values();
                        visualizer.setRightMode(r[(visualizer.getRightMode().ordinal() + 1) % r.length]);
                    } else {
                        LeftMode[] l = LeftMode.values();
                        visualizer.setLeftMode(l[(visualizer.getLeftMode().ordinal() + 1) % l.length]);
                    }
                } else if (code == KeyEvent.VK_SPACE) {
                    audio.pause();
                    source.setPaused(audio.isPaused());
                }
            }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.requestFocusInWindow();

        audio.init(plugin);
        audio.addGenericListener(source::update);
        visualizer.start();
        audio.play();
        audio.close();
        visualizer.stop();
        frame.setVisible(false);
        frame.dispose();
    }

    @Test
    @DisplayName("play multiple w/ fmdsp visualizer via the generic chip source")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void testMultiPlay() throws Exception {
        Audio audio = Audio.getInstance();

        ChipFmDspSource source = new ChipFmDspSource();

        FmDspVisualizer visualizer = new FmDspVisualizer(60, 2);
        visualizer.setTitle("MDDSP");
        visualizer.setVersion(Common.version.equals("undefined") ? null : Common.version);
        visualizer.setDataSource(source);
        if (fontRom != null && Files.exists(Path.of(fontRom))) {
            visualizer.setFontRom(Files.readAllBytes(Path.of(fontRom)));
        }

        AtomicReference<CountDownLatch> cdl = new AtomicReference<>();

        JFrame frame = new JFrame();
        frame.setLayout(new BorderLayout());
        frame.add(visualizer, BorderLayout.CENTER);
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code >= KeyEvent.VK_F1 && code <= KeyEvent.VK_F10) {
                    visualizer.setPaletteIndex(code - KeyEvent.VK_F1);
                } else if (code == KeyEvent.VK_F11) {
                    if (e.isShiftDown()) {
                        RightMode[] r = RightMode.values();
                        visualizer.setRightMode(r[(visualizer.getRightMode().ordinal() + 1) % r.length]);
                    } else {
                        LeftMode[] l = LeftMode.values();
                        visualizer.setLeftMode(l[(visualizer.getLeftMode().ordinal() + 1) % l.length]);
                    }
                } else if (code == KeyEvent.VK_SPACE) {
                    audio.pause();
                    source.setPaused(audio.isPaused());
                } else if (code == KeyEvent.VK_N) {
                    if (e.isControlDown()) {
                        cdl.get().countDown();
                    }
                }
            }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.requestFocusInWindow();

        List<Path> files = switch (multiSource) {
            case "dirext" -> new ArrayList<>(listFilesUnderDirFilteredByExt(dir, ext));
            default -> listFilesInLocalProperties();
        };
        Collections.shuffle(files);

        // once for the whole play list: Audio keeps its listeners forever, so subscribing per
        // song has the source updated once per song played, each update pushing the same samples
        // into the fft and snapshotting the chips again
        audio.addGenericListener(source::update);

        for (Path path : files) {
            this.file = path.toString();
Debug.print("play: " + file + " ---------------------------------------------------------------------");
            // every song brings its own format, plugin and driver: hoisting these out of the loop
            // leaves every song after the first playing through the first one's plugin, so the
            // driver - and the name the display takes from it - never changes
            FileFormat format = FileFormat.getFileFormat(file);
            format.load(Archives.getInputStream(new BufferedInputStream(Files.newInputStream(path))), null);
            var plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
            plugin.setParams(format, Map.of("fileName", file));
            frame.setTitle(Path.of(file).getFileName() + " - MDDSP");

            audio.init(plugin);
            // the chips are shared singletons, so last song's state has to go before this one
            source.reset();
            source.bind(plugin);
            source.setFilename(Path.of(file).getFileName().toString());
            visualizer.start();
            ExecutorService es = Executors.newSingleThreadExecutor();
            cdl.set(new CountDownLatch(1));
            // count down in a finally: a song that fails to start would otherwise leave the
            // await below waiting forever - silence until ^N, looking like a song that plays
            // nothing rather than one that could not be played
            es.submit(() -> { try { audio.play(); } catch (Exception e) { Debug.printStackTrace(e); } finally { cdl.get().countDown(); }});
Debug.print("await");
            cdl.get().await();
Debug.println("await: broke");
            es.shutdownNow();
Debug.println("stop");
            audio.stop();
            audio.close();
            visualizer.stop();
        }

        frame.setVisible(false);
        frame.dispose();
    }

    /**
     * @param args 0: audio file
     */
    static void main(String[] args) throws Exception {
        TestCase app = new TestCase();
        if (args.length == 1)
            app.file = args[0];
        else
            app.setup();

        app.play();
    }
}
