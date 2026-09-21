/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.smaf;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sound.midi.Instrument;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import mdplayer.lib.smaf.MmfToolPlayer;

import static java.lang.System.getLogger;
import static vavi.sound.SoundUtil.volume;


/**
 * A {@link Synthesizer} that is the yamaha MA-5 dll ({@code M5_EmuSmw5.dll}) played by mmftool
 * ({@code mmftoolc.exe}) on jdosbox - what {@link mdplayer.driver.smaf.SmafDriver} played a song on
 * before {@link mdplayer.driver.smaf.SmafDriver2}, kept here as a synthesizer.
 * <p>
 * mmftool plays a whole file, it sequences the song itself: so the song is given as a file, by
 * {@link #play(byte[])} or by a meta message of {@link #META_SMAF} carrying the file. The channel
 * messages of a sequence are not played: one read by vavi-sound out of the same file would only be a
 * second copy of what the dll plays. The universal master volume is taken, as the gain of the line
 * of its own that what comes out goes to.
 * <p>
 * {@code -Dmdplayer.smaf.mmftool=<dir>} is where mmftoolc.exe and its dlls are, see
 * {@link MmfToolPlayer}. jdosbox keeps its machine in statics, one song at a time in a JVM.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-21 nsano initial version <br>
 * @see "vavi-apps-mfiplayer vavi.sound.midi.faith.FaithSynthesizer"
 */
public class SmafMa5Synthesizer implements Synthesizer {

    private static final Logger logger = getLogger(SmafMa5Synthesizer.class.getName());

    /** a meta message of this type whose data is a whole SMAF file plays it */
    public static final int META_SMAF = 0x7f;

    /** the device information */
    static final Info info =
            new Info("SMAF MA-5 (mmftool) MIDI Synthesizer",
                     "vavi",
                     "Software synthesizer for SMAF, M5_EmuSmw5.dll played by mmftoolc.exe on jdosbox",
                     "Version " + SmafMa5MidiDeviceProvider.version) {};

    /** how long to wait for the emulated player before playing silence [ms] */
    private static final long READ_TIMEOUT_MILLIS = 500;

    private final List<Receiver> receivers = new CopyOnWriteArrayList<>();

    private MmfToolPlayer player;

    private SourceDataLine line;

    /** carries what the emulated player makes to the line */
    private Thread pump;

    private volatile boolean open;

    private long start;

    /** the listener's volume, universal master volume, 0 ~ 1 */
    private volatile float gain = 1;

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    public synchronized void open() throws MidiUnavailableException {
        if (!MmfToolPlayer.isAvailable()) {
            throw new MidiUnavailableException("no mmftoolc.exe under " + MmfToolPlayer.toolDirectory()
                    + ", set -D" + MmfToolPlayer.MMFTOOL_PATH_KEY + "=<dir>");
        }
        open = true;
        start = System.nanoTime();
    }

    /**
     * Plays a SMAF file, stopping the one playing: boots the machine and returns, the sound comes a
     * few seconds later when the player has loaded (see {@link MmfToolPlayer}).
     */
    public synchronized void play(byte[] mmf) throws MidiUnavailableException {
        if (!open) {
            throw new MidiUnavailableException("not open");
        }
        stopPlayer();
        MmfToolPlayer player = new MmfToolPlayer(mmf);
        try {
            player.start();
        } catch (IOException e) {
            player.stop();
            throw (MidiUnavailableException) new MidiUnavailableException(e.getMessage()).initCause(e);
        }
        this.player = player;
        pump = new Thread(() -> play(player), "smaf-ma5-pump");
        pump.setDaemon(true);
        pump.start();
    }

    /** what the emulated player makes, to a line opened at the rate it turns out to play at */
    private void play(MmfToolPlayer player) {
        byte[] buffer = new byte[4096];
        while (open && this.player == player && !player.isFinished()) {
            int n = player.read(buffer, 0, buffer.length, READ_TIMEOUT_MILLIS);
            if (n <= 0) {
                continue;
            }
            try {
                if (line == null) {
                    AudioFormat format = new AudioFormat(player.getSampleRate(), 16, player.getChannels(), true, false);
                    line = AudioSystem.getSourceDataLine(format);
                    line.open(format);
                    line.start();
                    volume(line, gain);
logger.log(Level.DEBUG, "smaf ma5: " + format);
                }
                line.write(buffer, 0, n);
            } catch (LineUnavailableException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
                return;
            }
        }
        if (line != null) {
            line.drain();
        }
    }

    /** whether the song given is over, or there is none */
    public boolean isFinished() {
        MmfToolPlayer player = this.player;
        return player == null || player.isFinished();
    }

    /** the emulated player, for a test that wants to know how it got on */
    MmfToolPlayer getPlayer() {
        return player;
    }

    private synchronized void stopPlayer() {
        MmfToolPlayer player = this.player;
        this.player = null;
        if (player != null) {
            player.stop();
        }
        if (pump != null) {
            try {
                pump.join(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            pump = null;
        }
        if (line != null) {
            line.close();
            line = null;
        }
    }

    @Override
    public synchronized void close() {
        if (!open) {
            return;
        }
        for (Receiver receiver : List.copyOf(receivers)) {
            receiver.close();
        }
        stopPlayer();
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public long getMicrosecondPosition() {
        return open ? (System.nanoTime() - start) / 1_000 : 0;
    }

    @Override
    public int getMaxReceivers() {
        return -1;
    }

    @Override
    public int getMaxTransmitters() {
        return 0;
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return new SmafMa5Receiver();
    }

    @Override
    public List<Receiver> getReceivers() {
        return List.copyOf(receivers);
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        throw new MidiUnavailableException("No transmitter available");
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return Collections.emptyList();
    }

    @Override
    public int getMaxPolyphony() {
        return 32;
    }

    @Override
    public long getLatency() {
        return 0;
    }

    /** the dll sequences the song itself, there is nothing to play a channel of */
    @Override
    public MidiChannel[] getChannels() {
        return new MidiChannel[0];
    }

    @Override
    public VoiceStatus[] getVoiceStatus() {
        return new VoiceStatus[0];
    }

    // the dll's voices live inside it

    @Override
    public boolean isSoundbankSupported(Soundbank soundbank) {
        return false;
    }

    @Override
    public Soundbank getDefaultSoundbank() {
        return null;
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        return new Instrument[0];
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        return new Instrument[0];
    }

    @Override
    public boolean loadInstrument(Instrument instrument) {
        return false;
    }

    @Override
    public void unloadInstrument(Instrument instrument) {
    }

    @Override
    public boolean remapInstrument(Instrument from, Instrument to) {
        return false;
    }

    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        return false;
    }

    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
    }

    @Override
    public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
        return false;
    }

    @Override
    public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
    }

    /** takes a meta message of {@link #META_SMAF} as the song, and nothing else */
    private class SmafMa5Receiver implements MidiDeviceReceiver {

        private boolean receiverOpen;

        SmafMa5Receiver() {
            receivers.add(this);
            receiverOpen = true;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!receiverOpen) {
                throw new IllegalStateException("Receiver is not open");
            }
            if (message instanceof SysexMessage sysex) {
                byte[] data = sysex.getData();
                // Universal Realtime, Device Control, Master Volume
                if (data.length >= 6 && (data[0] & 0xff) == 0x7f && data[2] == 0x04 && data[3] == 0x01) {
                    gain = ((data[4] & 0x7f) | ((data[5] & 0x7f) << 7)) / 16383f;
                    SourceDataLine line = SmafMa5Synthesizer.this.line;
                    if (line != null) {
                        volume(line, gain);
                    }
                }
            } else if (message instanceof MetaMessage meta && meta.getType() == META_SMAF) {
                try {
                    play(meta.getData());
                } catch (MidiUnavailableException e) {
                    logger.log(Level.ERROR, e.getMessage(), e);
                }
            } else {
logger.log(Level.TRACE, "not played, the dll sequences the song: " + message);
            }
        }

        @Override
        public void close() {
            receiverOpen = false;
            receivers.remove(this);
        }

        @Override
        public MidiDevice getMidiDevice() {
            return SmafMa5Synthesizer.this;
        }
    }
}
