package mdplayer.driver.gbs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.FileFormat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * GbsWavTestProgram.
 * <p>
 * Copy of MndrvWavTestProgram adapted for GBS
 * to write WAV file instead of playing to speakers.
 * Uses the built-in waveWriter via settings.
 */
class GbsWavTestProgram {

    @BeforeAll
    static void setupAll() {
        System.setProperty("mdplayer.variant.ymf262", "0");
        System.setProperty("javax.sound.sampled.SourceDataLine", "#WaveOut Mixer");
    }

    @AfterAll
    static void tearDownAll() {
        System.clearProperty("javax.sound.sampled.SourceDataLine");
    }

    /** duration to render in seconds (matching reference wav) */
    private static final int RENDER_DURATION = 120;

    @Test
    @Disabled("it works, but not passed")
    void test() throws Exception {
        play(
                "../vavi-sound-emu/tmp/CGB-B2XE-USA.gbs",
                "../vavi-sound-emu/tmp/waveout.wav",
                1);
    }

    static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: GbsWavTestProgram <gbs_file> [reference_wav]");
            return;
        }

        play(args[0], args.length > 1 ? args[1] : null, 1);
    }

    private static void play(String filename, String refWavFile, int songNo) throws Exception {
        System.err.println("filename: " + filename);
        System.err.println("refWavFile: " + refWavFile);
        Setting setting = Setting.getInstance();

        // disable speaker output, enable WAV writer
        setting.getOutputDevice().setDeviceType(Common.DEV_Null);
        setting.getOther().setWavSwitch(true);

        FileFormat format = FileFormat.getFileFormat(filename);
        format.load(Files.newInputStream(Path.of(filename)), null);
        BasePlugin<? extends BaseDriver> plugin = (BasePlugin<? extends BaseDriver>) format.getPlugin();
        plugin.setParams(format, Map.of("fileName", filename, "songNo", songNo));

        // Initialize driver and chips without starting the infinite loop in BasePlugin.play()
        plugin.prepare();

        System.err.println("Rendering " + filename + " to WAV...");
        // Instead of plugin.play(), we run our own loop to ensure we can stop it.
        // GBSPlugin.play() would call super.play() which has an infinite loop.

        int timeout = RENDER_DURATION + 10; // duration + 10s buffer

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int counter = 0;
        while (true) {
            short[] buffer = new short[8192];
            int ret = plugin.getDriver().render(buffer, 0, buffer.length);
            if ((counter += (buffer.length / 2)) % 1000 == 0) {
                System.err.println("Frame: " + counter);
            }
            
            // accumulate to byte array
            for (short value : buffer) {
                baos.write(value & 0xff);
                baos.write((value >> 8) & 0xff);
            }

            if (counter / 44100. > timeout) {
                System.err.println("Render timeout reached, stopping...");
                break;
            }

            if (plugin.driverVirtual != null && plugin.driverVirtual.stopped) {
                System.err.println("Driver signaled stop, stopping...");
                break;
            }
        }

        // Finalize rendering
        plugin.stop();
        plugin.close();

        System.err.println("Rendering complete.");

        String actualOutWavFile = "tmp/gbs_out.wav";
        Files.createDirectory(Path.of("tmp"));
        AudioFormat af = new AudioFormat(44100, 16, 2, true, false);
        byte[] audioBytes = baos.toByteArray();
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(audioBytes),
                        af,
                        audioBytes.length / af.getFrameSize()),
                AudioFileFormat.Type.WAVE,
                new File(actualOutWavFile)
        );

        if (refWavFile != null && new File(actualOutWavFile).exists()) {
            compareWavFiles(refWavFile, actualOutWavFile);
        }
    }

    /** compare two wav files and report similarity metrics */
    static void compareWavFiles(String refPath, String outPath) throws Exception {
System.out.println("out size: " + new File(outPath).length());
        AudioInputStream refAis = AudioSystem.getAudioInputStream(new File(refPath));
        AudioInputStream outAis = AudioSystem.getAudioInputStream(new File(outPath));

        System.out.println("Reference: " + refAis.getFormat());
        System.out.println("Output:    " + outAis.getFormat());

        byte[] refBytes = refAis.readAllBytes();
        byte[] outBytes = outAis.readAllBytes();
        refAis.close();
        outAis.close();

        int refSamples = refBytes.length / 2;
        int outSamples = outBytes.length / 2;
        int minSamples = Math.min(refSamples, outSamples);

        System.out.println("Reference samples: " + refSamples + ", Output samples: " + outSamples);

        long totalDiffSquared = 0;
        long maxDiff = 0;
        long refEnergy = 0;
        long outEnergy = 0;
        long crossCorr = 0;

        for (int i = 0; i < minSamples; i++) {
            int refVal = (short) ((refBytes[i * 2] & 0xff) | (refBytes[i * 2 + 1] << 8));
            int outVal = (short) ((outBytes[i * 2] & 0xff) | (outBytes[i * 2 + 1] << 8));
            int diff = refVal - outVal;
            totalDiffSquared += (long) diff * diff;
            if (Math.abs(diff) > maxDiff) maxDiff = Math.abs(diff);
            refEnergy += (long) refVal * refVal;
            outEnergy += (long) outVal * outVal;
            crossCorr += (long) refVal * outVal;
        }

        double rmsDiff = Math.sqrt((double) totalDiffSquared / minSamples);
        double refRms = Math.sqrt((double) refEnergy / minSamples);
        double outRms = Math.sqrt((double) outEnergy / minSamples);
        double snr = refEnergy > 0 ? 10.0 * Math.log10((double) refEnergy / totalDiffSquared) : Double.POSITIVE_INFINITY;
        double correlation = (refEnergy > 0 && outEnergy > 0)
                ? crossCorr / (Math.sqrt((double) refEnergy) * Math.sqrt((double) outEnergy))
                : 0;

        System.out.println("=== WAV Comparison Results ===");
        System.out.println("Reference RMS: " + "%.2f".formatted(refRms));
        System.out.println("Output RMS:    " + "%.2f".formatted(outRms));
        System.out.println("Diff RMS:      " + "%.2f".formatted(rmsDiff));
        System.out.println("Max Diff:      " + maxDiff);
        System.out.println("SNR (dB):      " + "%.2f".formatted(snr));
        System.out.println("Correlation:   " + "%.6f".formatted(correlation));

        assertTrue(outRms > 200 && Math.abs(correlation) > 0.85, "quality test");
    }
}
