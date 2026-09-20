/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mfi;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import mdplayer.ChipFmDspSource;
import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;
import musicDriverInterface.MetaData.Tag;
import vavi.sound.mfi.MfiChip;
import vavi.sound.mfi.MfiChip.Vendor;
import vavi.sound.mfi.rohm.RohmRom;
import vavi.sound.mobile.AudioEngineMixer;
import vavi.sound.visualizer.fmdsp.TrackId;
import vavi.sound.visualizer.fmdsp.TrackStatus;
import vavi.util.event.GenericEvent;

import org.junit.jupiter.api.Test;

import static mdplayer.driver.mfi.MldDriver.condition;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * MFi (".mld") driver: which chip a file is for, and the song through the format, the plugin and
 * the driver on each synthesizer.
 * <p>
 * system properties
 * <ul>
 *  <li>{@code mdplayer.mfi.test.mld} ... the song to render</li>
 *  <li>{@code mdplayer.mfi.test.corpus} ... the directory of the ringtones named after the phones
 *      they are of, default {@code ~/Public/np2/mfi/Ringtones (MLD)}</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
class MldDriverTest {

    /** a fuetrek song of plain midi notes, every synthesizer sounds it */
    static final Path mld = Path.of(System.getProperty("mdplayer.mfi.test.mld",
            "../../vavi/vavi-sound/tmp/PoN_jar5/mld_1.mld"));

    /**
     * a NEC song without a midi note: the tune is in the machine dependent messages (fm voices,
     * stream pcm) only the Yamaha stand in takes
     */
    static final Path necMld = Path.of("../../vavi/vavi-sound/tmp/samples/n703id/02 TRANSPARENT.mld");

    static final Path corpus = Path.of(System.getProperty("mdplayer.mfi.test.corpus",
            System.getProperty("user.home") + "/Public/np2/mfi/Ringtones (MLD)"));

    @Test
    void theDatabaseSaysWhichChipAPhoneHas() {
        assertEquals(MfiChip.YAMAHA, MfiChip.byModel("N505i").chip());
        assertEquals(MfiChip.YAMAHA, MfiChip.byModel("so504i").chip());
        assertEquals(MfiChip.FUETREK, MfiChip.byModel("SH901iC").chip());
        assertEquals(MfiChip.ROHM, MfiChip.byModel("F901iC").chip());
        assertEquals("BU8709KN", MfiChip.byModel("F901iC").part());
    }

    @Test
    void aMakerAndAVersionSayWhichChip() {
        assertEquals(MfiChip.YAMAHA, Vendor.NEC.chip(0x0400, 4));
        assertEquals(MfiChip.ROHM, Vendor.SHARP.chip(0x0300, 3));
        assertEquals(MfiChip.FUETREK, Vendor.SHARP.chip(0x0301, 3));
        assertEquals(MfiChip.ROHM, Vendor.FUJITSU.chip(0x0400, 4));
        assertEquals(MfiChip.YAMAHA, Vendor.FUJITSU.chip(0x0100, 1));
        assertEquals(MfiChip.FUETREK, Vendor.PANASONIC.chip(0x0301, 3));
        assertEquals(MfiChip.ROHM, Vendor.PANASONIC.chip(0x0400, 4));
        assertEquals(MfiChip.YAMAHA, Vendor.SONY.chip(0x0301, 3));
    }

    /** the ringtones of a phone come out as the chip of that phone */
    @Test
    void theFilesOfAPhoneAreOfItsChip() throws IOException {
        theFilesOfAPhoneAreOfItsChip("Ringtones from Cuebus F901iC:ROHM");
        theFilesOfAPhoneAreOfItsChip("Ringtones from Cami P902i:FUETREK");
        theFilesOfAPhoneAreOfItsChip("90s anime songs from Cuebus N506iS:YAMAHA");
    }

    private static void theFilesOfAPhoneAreOfItsChip(String arg) throws IOException {
        String[] a = arg.split(":");
        Path dir = corpus.resolve(a[0]);
        if (!Files.isDirectory(dir)) {
System.err.println(dir + " is missing");
            return;
        }

        Map<MfiChip, Integer> count = new EnumMap<>(MfiChip.class);
        try (Stream<Path> s = Files.list(dir)) {
            for (Path p : s.filter(p -> p.toString().toLowerCase().endsWith(".mld")).toList()) {
                count.merge(MfiChip.detect(condition(MldFile.decode(Files.readAllBytes(p)))).chip(), 1, Integer::sum);
            }
        }
System.err.println(a[0] + ": " + count);
        MfiChip expected = MfiChip.valueOf(a[1]);
        int total = count.values().stream().mapToInt(Integer::intValue).sum();
        assertTrue(count.getOrDefault(expected, 0) * 10 >= total * 9, a[0] + ": " + count);
    }

    @Test
    void readsWhatTheFileSaysAboutItself() throws Exception {
        assumeTrue(Files.exists(mld), mld + " is missing");

        byte[] b = Files.readAllBytes(mld);
        MldFile file = MldFile.decode(b);
        assertTrue(file.getVersion() > 0);

        var md = new MldDriver().retrieveMetaData(b);
        assertNotNull(md);
        assertEquals(Objects.requireNonNullElse(file.getTitle(), ""), md.getFirst(Tag.Title));
System.err.println(mld + ": " + MfiChip.detect(condition(file)) + ", supt: " + file.getSupport());
    }

    @Test
    void theFormatIsFoundByItsHeader() throws Exception {
        assumeTrue(Files.exists(mld), mld + " is missing");

        try (BufferedInputStream is = new BufferedInputStream(Files.newInputStream(mld))) {
            assertInstanceOf(MldFileFormat.class, FileFormat.getFileFormat(is));
        }
        assertInstanceOf(MldFileFormat.class, FileFormat.getFileFormat("song.MLD"));
    }

    @Test
    void playsOnNuked() throws Exception {
        playsThroughTheDriver("nuked", mld);
    }

    @Test
    void playsTheNecMachineDependentOnes() throws Exception {
        assumeTrue(Files.exists(necMld), necMld + " is missing");
        assertEquals(MfiChip.YAMAHA, MfiChip.detect(condition(MldFile.decode(Files.readAllBytes(necMld)))).chip());
        playsThroughTheDriver(null, necMld);
        // the opl3 stand in leaves the song's adpcm to vavi-sound's engine, sion sounds it itself
        playsThroughTheDriver("nuked", necMld);
    }

    @Test
    void playsOnSion() throws Exception {
        playsThroughTheDriver("sion", mld);
    }

    @Test
    void playsOnUcs() throws Exception {
        playsThroughTheDriver("ucs", mld);
    }

    @Test
    void playsOnRohm() throws Exception {
        playsThroughTheDriver("rohm", mld);
    }

    /** a song of a rohm phone, on the synthesizer of its chip, whose ainf tells no audio format */
    @Test
    void playsARohmSong() throws Exception {
        assumeTrue(RohmRom.isAvailable(), "no rt_synth_2.dll, set -Dvavi.sound.mfi.faith.path");
        Path rohm = corpus.resolve("Ringtones from Cuebus F901iC/110_8981100010347092588F.MLD");
        assumeTrue(Files.exists(rohm), rohm + " is missing");
        assertEquals(MfiChip.ROHM, MfiChip.detect(condition(MldFile.decode(Files.readAllBytes(rohm)))).chip());
        playsThroughTheDriver(null, rohm);
    }

    @Test
    void playsOnGervill() throws Exception {
        playsThroughTheDriver("gervill", mld);
    }

    @Test
    void playsOnMa7() throws Exception {
        playsThroughTheDriver("ma7", mld);
    }

    @Test
    void playsOnOpenDojaMa3() throws Exception {
        playsThroughTheDriver("opendoja.ma3", mld);
    }

    @Test
    void playsOnOpenDojaFuetrek() throws Exception {
        playsThroughTheDriver("opendoja.fuetrek", mld);
    }

    /** a property picks one for a chip, or for every chip, and the best one available otherwise */
    @Test
    void aPropertyPicksTheSynthesizer() {
        try {
            System.setProperty(MldSynth.SYNTH_KEY + ".rohm", "nuked");
            try (MldSynth synth = MldSynth.forChip(MfiChip.ROHM)) {
                assertEquals("nuked", synth.getName());
            }
            System.setProperty(MldSynth.SYNTH_KEY, "gervill");
            try (MldSynth synth = MldSynth.forChip(MfiChip.ROHM)) {
                assertEquals("nuked", synth.getName(), "the chip's own wins");
            }
            try (MldSynth synth = MldSynth.forChip(MfiChip.YAMAHA)) {
                assertEquals("gervill", synth.getName());
            }
            System.setProperty(MldSynth.SYNTH_KEY, "no such one");
            assertThrows(IllegalArgumentException.class, () -> MldSynth.forChip(MfiChip.YAMAHA));
        } finally {
            System.clearProperty(MldSynth.SYNTH_KEY + ".rohm");
            System.clearProperty(MldSynth.SYNTH_KEY);
        }
        for (MfiChip chip : MfiChip.values()) {
            MldSynth best = MldSynth.providers().stream()
                    .filter(p -> p.getChips().contains(chip) && p.isAvailable()).findFirst().orElseThrow();
            try (MldSynth synth = MldSynth.forChip(chip)) {
System.err.println(chip + " → " + synth.getName());
                assertEquals(best.getName(), synth.getName());
            }
        }
    }

    /** @param synth null: the one for the chip */
    private static void playsThroughTheDriver(String synth, Path song) throws Exception {
        assumeTrue(Files.exists(song), song + " is missing");
        if (synth != null) {
            MldSynth provider = MldSynth.providers().stream().filter(p -> p.getName().equals(synth)).findFirst().orElseThrow();
            assumeTrue(provider.isAvailable(), provider.getRequirement());
        }

        Setting setting = Setting.getInstance();
        setting.getOutputDevice().setDeviceType(Common.DEV_Null);
        int sampleRate = setting.getOutputDevice().getSampleRate();

        if (synth != null) System.setProperty(MldSynth.SYNTH_KEY, synth);
        // the adpcm of vavi-sound plays into a line of its own
        System.setProperty("vavi.sound.mobile.AudioEngine.volume", "0.02");
        try {
            String filename = song.toString();
            FileFormat format = FileFormat.getFileFormat(filename);
            format.load(new BufferedInputStream(Files.newInputStream(song)), null);

            @SuppressWarnings("unchecked")
            BasePlugin<? extends BaseDriver> plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
            plugin.setParams(format, Map.of("fileName", filename));
            plugin.prepare();

            BaseDriver driver = plugin.getDriver();
            MldDriver mldDriver = assertInstanceOf(MldDriver.class, driver);
System.err.println(synth + ": " + mldDriver.getDetection() + " → " + mldDriver.getSynth().getDescription());

            int seconds = Integer.getInteger("mdplayer.mfi.test.seconds", 15);
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();
            short[] buffer = new short[2048];
            int peak = 0;
            int rendered = 0;
            long start = System.currentTimeMillis();
            boolean adpcm = false;
            while (rendered < sampleRate * seconds && !driver.stopped) {
                driver.render(buffer, 0, buffer.length);
                adpcm |= AudioEngineMixer.isPlaying();
                for (short s : buffer) {
                    peak = Math.max(peak, Math.abs(s));
                    pcm.write(s & 0xff);
                    pcm.write((s >> 8) & 0xff);
                }
                rendered += buffer.length / 2;
            }
            long elapsed = System.currentTimeMillis() - start;

            plugin.stop();
            plugin.close();

            byte[] b = pcm.toByteArray();
            Path out = Path.of("tmp/mld-" + (synth != null ? synth : "auto")
                    + (song.equals(mld) ? "" : "-" + song.getFileName().toString().split("[ .]")[0]) + ".wav");
            Files.createDirectories(out.getParent());
            AudioFormat af = new AudioFormat(sampleRate, 16, 2, true, false);
            AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(b), af, b.length / 4),
                    AudioFileFormat.Type.WAVE, out.toFile());
System.err.printf("%s: %.1fs of audio in %.1fs, peak %d -> %s%n", synth, rendered / (double) sampleRate, elapsed / 1000.0, peak, out);
            assertTrue(peak > 100, "silent: " + peak);
            if (song.equals(necMld) && "nuked".equals(synth)) {
                // its adpcm (ainf 0x82) is played in the song, not to a line of its own
                assertTrue(adpcm, "no adpcm went through the mixer");
            }
        } finally {
            System.clearProperty(MldSynth.SYNTH_KEY);
        }
    }

    /** the keys of a song light the rows of its chip, and the chip is named in upper case */
    @Test
    void theVisualizerShowsTheKeys() throws Exception {
        assumeTrue(Files.exists(mld), mld + " is missing");
        // mld_1 is a fuetrek song: pcm rows
        assertTrue(litRows(mld, "PPZ8", "ADPCM") > 0);

        Path yamaha = corpus.resolve("90s anime songs from Cuebus N506iS/region_0550.mld");
        if (Files.exists(yamaha)) {
            assertTrue(litRows(yamaha, "FM") > 0);
        }
        assertEquals("YAMAHA MA-3", MfiChip.byModel("N504i").name());
    }

    /** @return how many rows of the named kind (by {@link TrackId} name prefix) showed a key */
    private static int litRows(Path file, String... rowKinds) throws Exception {
        Setting.getInstance().getOutputDevice().setDeviceType(Common.DEV_Null);
        FileFormat format = FileFormat.getFileFormat(file.toString());
        format.load(new BufferedInputStream(Files.newInputStream(file)), null);
        @SuppressWarnings("unchecked")
        BasePlugin<? extends BaseDriver> plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", file.toString()));
        plugin.prepare();
        plugin.stopped = false;
        plugin.paused = false;
        plugin.fadeout = false;
        try {
            ChipFmDspSource source = new ChipFmDspSource();
            source.bind(plugin);
            BaseDriver driver = plugin.getDriver();
            short[] buffer = new short[1024];
            TrackStatus status = new TrackStatus();
            java.util.Set<TrackId> lit = java.util.EnumSet.noneOf(TrackId.class);
            for (int i = 0; i < 44100 * 2 * 5 / buffer.length; i++) {
                driver.render(buffer, 0, buffer.length);
                source.update(new GenericEvent(driver, "master", buffer, 0));
                for (TrackId row : TrackId.values()) {
                    source.readStatus(row, status);
                    if (status.playing && status.key != 0xff
                            && Stream.of(rowKinds).anyMatch(k -> row.name().startsWith(k))) {
                        lit.add(row);
                    }
                }
            }
System.err.println(file.getFileName() + ": lit " + lit);
            return lit.size();
        } finally {
            plugin.stop();
            plugin.close();
        }
    }

    /** every file of the corpus is read and gets a chip */
    @Test
    void everyFileGetsAChip() throws IOException {
        assumeTrue(Files.isDirectory(corpus), corpus + " is missing");

        Map<MfiChip, Integer> count = new EnumMap<>(MfiChip.class);
        List<Path> files;
        try (Stream<Path> s = Files.walk(corpus)) {
            files = s.filter(p -> p.toString().toLowerCase().endsWith(".mld")).toList();
        }
        for (Path p : files) {
            byte[] b = Files.readAllBytes(p);
            if (!MldFile.isMfi(b)) continue;
            count.merge(MfiChip.detect(condition(MldFile.decode(b))).chip(), 1, Integer::sum);
        }
System.err.println(corpus + ": " + count);
        assertTrue(count.values().stream().mapToInt(Integer::intValue).sum() > 0);
    }
}
