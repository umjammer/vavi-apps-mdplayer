/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.sid;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import mdplayer.Setting;
import mdplayer.lib.sid.libsidplayfp.builders.resid_builder.ReSidBuilder;
import mdplayer.lib.sid.libsidplayfp.sidplayfp.SidConfig;
import mdplayer.lib.sid.libsidplayfp.sidplayfp.SidTune;
import mdplayer.lib.sid.libsidplayfp.sidplayfp.playSidFp;
import vavi.util.Debug;

import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static vavi.sound.SoundUtil.volume;


/**
 * SidTestProgram.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-01-25 nsano initial version <br>
 */
class SidTestProgram {

    private static final int SamplingRate = 44100;

    private SourceDataLine line = null;
    private Thread playbackThread;
    private volatile boolean isPlaying = false;

    private playSidFp engine;
    private SidTune tune;
    private volatile int validBuffers = 0;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: SidTestProgram <sid_file> [song_no]");
            return;
        }
        int song = 1;
        if (args.length > 1) {
            song = Integer.parseInt(args[1]);
        }

        new SidTestProgram().play(args[0], song);
    }

    private void play(String filename, int song) throws Exception {
        byte[] fileBuffer = Files.readAllBytes(Paths.get(filename));

        init(fileBuffer, song);

        AtomicReference<CountDownLatch> cdl = new AtomicReference<>(new CountDownLatch(1));
        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyReleased(NativeKeyEvent event) {
                int keyCode = event.getKeyCode();
//Debug.println("keyTyped: " + keyCode + ", " + ((event.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0));
                if (keyCode == NativeKeyEvent.VC_ENTER) {
Debug.print("countdown");
                    cdl.get().countDown();
                }
            }
        });

        AudioFormat format = new AudioFormat(SamplingRate, 16, 2, true, false);
        line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        volume(line, Double.parseDouble(System.getProperty("mdsound.volume", "0.2")));
        line.start();

        isPlaying = true;
        playbackThread = new Thread(this::playbackLoop);
        playbackThread.start();

        Debug.println("Playing " + filename + " song " + song);
        Debug.println("Press Enter to stop...");
        cdl.get().await();

        isPlaying = false;
        playbackThread.join();
        line.close();

        if (validBuffers == 0) {
            throw new RuntimeException("FAILURE: No valid audio signal detected at all! (RMS > 50 and Peak > 2000 not met)");
        }

        Debug.println("SUCCESS: Audio signal detected.");
        playbackThread.join();
    }

    private void init(byte[] buf, int song) {
        // Mock Setting for ReSidBuilder
        Setting setting = mock(Setting.class, Mockito.RETURNS_DEEP_STUBS);
        when(setting.getOutputDevice().getSampleRate()).thenReturn(SamplingRate);

        engine = new playSidFp(SamplingRate);
        engine.setRoms(null, null, null); // No ROMs for now

        ReSidBuilder rs = new ReSidBuilder("ReSid", setting.getOutputDevice().getSampleRate());
        rs.create(1); // Create 1 SID

        tune = new SidTune(buf, buf.length);
        tune.selectSong(song);

        if (!engine.load(tune)) {
            System.err.println("Error loading tune: " + engine.error());
            return;
        }

        SidConfig cfg = new SidConfig(SamplingRate);
        cfg.frequency = SamplingRate;
        cfg.samplingMethod = SidConfig.SamplingMethod.RESAMPLE_INTERPOLATE;
        cfg.fastSampling = false;
        cfg.playback = SidConfig.Playback.STEREO;
        cfg.sidEmulation = rs;

        if (!engine.config(cfg)) {
            System.err.println("Error configuring engine: " + engine.error());
        }
    }

    private void playbackLoop() {
        int bufferSize = 2048; // samples
        short[] sampleBuffer = new short[bufferSize * 2]; // stereo
        byte[] audioBuffer = new byte[bufferSize * 4]; // 16-bit stereo

        long totalSquared = 0;
        int totalSamples = 0;
        int peak = 0;
        int bufferCount = 0;
        validBuffers = 0;

        while (isPlaying) {
            int produced = engine.play(sampleBuffer, bufferSize * 2);

            for (int i = 0; i < produced; i++) {
                short val = sampleBuffer[i];
                totalSquared += (long) val * val;
                int absVal = Math.abs(val);
                if (absVal > peak) peak = absVal;
                totalSamples++;

                audioBuffer[i * 2] = (byte) (val & 0xff);
                audioBuffer[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
            }

            bufferCount++;
            if (bufferCount % 10 == 0 && totalSamples > 0) { // Check every ~0.9s
                double rms = Math.sqrt((double) totalSquared / totalSamples);
//                System.err.printf("Stats: RMS=%.2f Peak=%d Samples=%d%n", rms, peak, totalSamples);

                // Strict assertion: expect significant signal (RMS > 50, Peak > 2000)
                // Ignore first few buffers to avoid transients/clicks (user requirement)
                if (bufferCount > 20) {
                    if (rms > 50.0 && peak > 2000) {
                        validBuffers++;
                    }
                }

                totalSquared = 0;
                totalSamples = 0;
                peak = 0;
            }

            if (produced > 0) {

                line.write(audioBuffer, 0, produced * 2);
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
