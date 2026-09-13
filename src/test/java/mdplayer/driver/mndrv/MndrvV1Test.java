package mdplayer.driver.mndrv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Renders MND version 1 songs end to end, through the plugin and the PSG emulation, and checks
 * that sound actually comes out.
 * <p>
 * {@code MndV1AnalyzerTest} settles the question of whether the sequencer reads the data
 * correctly; this one settles the separate question of whether the notes it reads reach a chip.
 * There is no reference rendering to compare against -- mndrv.x refuses this data, so nothing has
 * ever played it -- so the assertions here are about signal, not about fidelity.
 * <p>
 * 41 of the 62 songs render audibly. The rest are held silent by the ported PSG software
 * envelope: when {@code $C0} arrives after the volume command, {@code DevPsg#_PSG_C0} switches
 * the envelope on and it collapses to zero on the first step, so the channel is set to volume 15
 * and back to 0 within one frame. That is not a version 1 problem. {@code BRAN126.MND}, ordinary
 * version 2 data that mndrv plays, goes silent in exactly the same place, and both come back when
 * {@code _PSG_C0} is stopped from switching the envelope on. The port matches its C# reference
 * line for line here, so whatever is wrong is upstream of the Java and wants its own
 * investigation of the envelope stepping in {@code _ex_soft*}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 */
@EnabledIf("samplesExist")
class MndrvV1Test {

    static final Path V1 = Path.of("tmp/mnd/MND_SXP1");

    static final int SAMPLE_RATE = 44100;

    static boolean samplesExist() {
        return Files.isDirectory(V1);
    }

    static List<Path> v1Files() throws Exception {
        try (Stream<Path> s = Files.list(V1)) {
            return s.filter(p -> p.toString().toLowerCase().endsWith(".mnd")).sorted().toList();
        }
    }

    /** what one rendered song looked like */
    record Render(double rms, int peak, long frames) {}

    /**
     * Renders {@code seconds} of a song. Returns null rather than throwing when the song is
     * shorter than that; the caller only cares about the audio it produced.
     */
    static Render render(Path path, double seconds, Path wavOut) throws Exception {
        Setting setting = Setting.getInstance();
        setting.getOutputDevice().setDeviceType(Common.DEV_Null);
        setting.getOther().setWavSwitch(true);

        FileFormat format = FileFormat.getFileFormat(path.toString());
        format.load(Files.newInputStream(path), null);
        @SuppressWarnings("unchecked")
        BasePlugin<? extends BaseDriver> plugin =
                (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", path.toString()));
        plugin.prepare();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        double energy = 0;
        int peak = 0;
        long n = 0;
        try {
            while (n < (long) (seconds * SAMPLE_RATE) && !plugin.driverVirtual.stopped) {
                short[] buffer = new short[8192];
                plugin.getDriver().render(buffer, 0, buffer.length);
                for (short v : buffer) {
                    energy += (double) v * v;
                    peak = Math.max(peak, Math.abs(v));
                    if (wavOut != null) {
                        baos.write(v & 0xff);
                        baos.write((v >> 8) & 0xff);
                    }
                }
                n += buffer.length / 2;
            }
        } finally {
            plugin.stop();
            plugin.close();
        }

        if (wavOut != null) {
            AudioFormat af = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
            byte[] bytes = baos.toByteArray();
            AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(bytes), af,
                            bytes.length / af.getFrameSize()),
                    AudioFileFormat.Type.WAVE, wavOut.toFile());
        }
        return new Render(Math.sqrt(energy / Math.max(1, n * 2)), peak, n);
    }

    @Test
    @DisplayName("a v1 song renders audible audio")
    void v1Renders() throws Exception {
        new File("tmp").mkdirs();
        Render r = render(V1.resolve("SXP000.MND"), 30, Path.of("tmp/mnd_v1_out.wav"));
        assertTrue(r.rms() > 200, "SXP000 should be audible, rms was " + r.rms());
        assertTrue(r.peak() > 2000, "SXP000 should have real peaks, peak was " + r.peak());
    }

    /**
     * How many of the 62 songs are audible today. Raising this is the point of fixing the PSG
     * software envelope described in the class comment; it should never fall.
     */
    static final int AUDIBLE = 41;

    @Test
    @DisplayName("every v1 song renders without faulting")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "diag")
    void allV1Render() throws Exception {
        List<Path> files = v1Files();
        assertEquals(62, files.size());
        int audible = 0;
        StringBuilder quiet = new StringBuilder();
        for (Path f : files) {
            Render r = render(f, 8, null);
            assertTrue(r.frames() > 0, f + " produced no audio at all");
            if (r.rms() > 200) {
                audible++;
            } else {
                quiet.append("%n    %s rms %.1f peak %d".formatted(
                        f.getFileName(), r.rms(), r.peak()));
            }
        }
        assertTrue(audible >= AUDIBLE,
                "expected at least %d audible songs, got %d; quiet ones:%s"
                        .formatted(AUDIBLE, audible, quiet));
    }

    /** {@code MndrvV1Test <mnd file>...}, one song per run so nothing is carried between them */
    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            Render r = render(Path.of(arg), Double.parseDouble(
                    System.getProperty("seconds", "8")), null);
            System.out.printf("%-40s rms %8.1f peak %6d frames %d%n",
                    arg, r.rms(), r.peak(), r.frames());
        }
    }

    @Test
    @DisplayName("v1 metadata reads the title, not the tone data")
    void v1Title() throws Exception {
        byte[] buf = Files.readAllBytes(V1.resolve("SXP000.MND"));
        assertEquals("-SORCERIAN X1-", new MnDriver().retrieveMetaData(buf)
                .getFirst(musicDriverInterface.MetaData.Tag.Title).substring(0, 14));
    }
}
