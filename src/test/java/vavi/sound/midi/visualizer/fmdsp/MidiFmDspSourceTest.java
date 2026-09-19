/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.visualizer.fmdsp;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import vavi.sound.visualizer.fmdsp.FftDataSource;
import vavi.sound.visualizer.fmdsp.FmDspVisualizer;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.RightMode;
import vavi.sound.visualizer.fmdsp.TrackDetail;
import vavi.sound.visualizer.fmdsp.TrackId;
import vavi.sound.visualizer.fmdsp.TrackStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * {@link MidiFmDspSource}, on a hand-written MIDI stream - no sequencer, no synthesizer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-28 nsano initial version <br>
 */
class MidiFmDspSourceTest {

    /** what the source is handed the stream on to, so the tap can be shown to pass it through */
    static class Sink implements Receiver {
        final List<MidiMessage> received = new ArrayList<>();
        boolean closed;
        @Override public void send(MidiMessage message, long timeStamp) { received.add(message); }
        @Override public void close() { closed = true; }
    }

    Sink sink;
    MidiFmDspSource source;
    final TrackStatus status = new TrackStatus();

    @BeforeEach
    void setup() {
        sink = new Sink();
        source = new MidiFmDspSource(sink);
    }

    void send(int command, int channel, int data1, int data2) {
        try {
            source.send(new ShortMessage(command, channel, data1, data2), -1);
        } catch (InvalidMidiDataException e) {
            throw new IllegalArgumentException(e);
        }
    }

    void noteOn(int channel, int note, int velocity) {
        send(ShortMessage.NOTE_ON, channel, note, velocity);
    }

    void noteOff(int channel, int note) {
        send(ShortMessage.NOTE_OFF, channel, note, 0);
    }

    void cc(int channel, int controller, int value) {
        send(ShortMessage.CONTROL_CHANGE, channel, controller, value);
    }

    TrackStatus status(int channel) {
        source.trackStatus().readStatus(source.level().track(channel), status);
        return status;
    }

    @Test
    @DisplayName("the stream is passed on untouched")
    void tap() throws Exception {
        ShortMessage message = new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 100);
        source.send(message, 42);
        assertEquals(1, sink.received.size());
        assertSame(message, sink.received.getFirst());
        // the receiver it was built on belongs to whoever opened it
        source.close();
        assertFalse(sink.closed);
    }

    @Test
    @DisplayName("a channel the song never addresses shows nothing of its own")
    void untouched() {
        noteOn(0, 60, 100);
        // the meter strip prints every column's tone number, and a silent channel has none
        assertEquals(0, status(1).toneNum);
        assertEquals(0, status(1).volume);
        assertFalse(status(1).playing);

        send(ShortMessage.PROGRAM_CHANGE, 1, 30, 0);
        // addressed but not sounding: the part is there, it is simply not playing yet
        assertEquals(31, status(1).toneNum);
        assertFalse(status(1).playing);
    }

    @Test
    @DisplayName("a note keys the row's keyboard and lights its meter")
    void note() {
        assertEquals(0xff, status(0).key);
        assertFalse(status(0).playing);

        noteOn(0, 60, 100);

        // middle C is o4c: high nibble the octave, low nibble the note
        assertEquals(0x40, status(0).key);
        assertEquals(0x40, status(0).actualKey);
        assertTrue(status(0).playing);
        assertTrue(source.level().level(0) > 0);
        assertEquals(TrackId.FM_1, source.level().track(0));

        noteOff(0, 60);
        assertEquals(0xff, status(0).key);
        // the meter falls rather than cutting off
        assertTrue(source.level().level(0) > 0);
    }

    @Test
    @DisplayName("a chord's top note is the one the keyboard shows, and what is left of it when one goes")
    void chord() {
        noteOn(0, 60, 100);
        noteOn(0, 64, 100);
        noteOn(0, 67, 100);
        assertEquals(0x47, status(0).key); // o4g

        noteOff(0, 67);
        assertEquals(0x44, status(0).key); // back to the e below it
        noteOff(0, 64);
        noteOff(0, 60);
        assertEquals(0xff, status(0).key);
    }

    @Test
    @DisplayName("the damper pedal holds a note past its note-off")
    void damper() {
        cc(0, 64, 127);
        noteOn(0, 60, 100);
        noteOff(0, 60);
        assertEquals(0x40, status(0).key); // the pedal is still holding it

        cc(0, 64, 0);
        assertEquals(0xff, status(0).key);
    }

    @Test
    @DisplayName("all notes off lets go of everything")
    void allNotesOff() {
        noteOn(0, 60, 100);
        noteOn(0, 64, 100);
        cc(0, 123, 0);
        assertEquals(0xff, status(0).key);
    }

    @Test
    @DisplayName("a note-on at velocity 0 is a note-off")
    void runningStatusNoteOff() {
        noteOn(0, 60, 100);
        noteOn(0, 60, 0);
        assertEquals(0xff, status(0).key);
    }

    @Test
    @DisplayName("the controllers reach the columns they belong in")
    void controllers() {
        send(ShortMessage.PROGRAM_CHANGE, 1, 48, 0);
        cc(1, 7, 90);
        cc(1, 10, 0);
        cc(1, 1, 64);
        cc(1, 65, 127);
        send(ShortMessage.PITCH_BEND, 1, 0, 0x60); // 0x3000 = up half of the range
        noteOn(1, 72, 80);

        TrackStatus s = status(1);
        assertEquals(49, s.toneNum); // GM numbers its programs from 1
        assertEquals(90, s.volume);
        assertEquals(Pan.LEFT, source.level().pan(1));
        assertEquals(100, s.detune); // half of the default 2 semitone range, in cents
        assertEquals('P', s.status.charAt(0)); // the modulation wheel is an LFO
        assertEquals('H', s.status.charAt(6)); // and it is the synthesizer's own
        assertEquals('P', s.status.charAt(7)); // CC65 portamento
        assertEquals(8, s.status.length());
    }

    @Test
    @DisplayName("a channel volume of 0 silences the meter without releasing the key")
    void volumeFade() {
        noteOn(2, 60, 127);
        assertTrue(source.level().level(2) > 0);
        cc(2, 7, 0);
        assertEquals(0, source.level().level(2));
        assertEquals(0x40, status(2).key); // still held
    }

    @Test
    @DisplayName("the rows shown are the channels that have sounded, in channel order")
    void displayTracks() {
        assertNull(source.trackStatus().displayTracks());

        noteOn(5, 60, 100);
        noteOn(1, 60, 100);
        assertArrayEquals(new TrackId[] {TrackId.FM_2, TrackId.FM_6},
                source.trackStatus().displayTracks());

        // sixteen channels do not fit ten rows
        for (int ch = 0; ch < 16; ch++) noteOn(ch, 60, 100);
        assertEquals(10, source.trackStatus().displayTracks().length);
    }

    @Test
    @DisplayName("the rows and the meter columns say which MIDI channel they are")
    void labels() {
        assertEquals("MIDI", source.trackStatus().trackTypeName(TrackId.FM_1));
        assertEquals(1, source.trackStatus().trackNumber(TrackId.FM_1));
        assertEquals("CH1", source.level().label(0));
        // GM keeps the drums on channel 10, whose column is the one the original labels RHY
        assertEquals("DRUM", source.trackStatus().trackTypeName(TrackId.ADPCM));
        assertEquals(10, source.trackStatus().trackNumber(TrackId.ADPCM));
        assertEquals("DRM", source.level().label(9));
        assertEquals("C16", source.level().label(15));
        // the strip is wider than a MIDI port
        assertNull(source.level().label(16));
        assertNull(source.level().track(16));
        assertEquals(0, source.level().level(16));
    }

    /** frames the display, the way the renderer does - it is timed, so it has to be run */
    int[] settle(int millis) {
        int[] bars = new int[FftDataSource.LENGTH];
        long until = System.currentTimeMillis() + millis;
        do {
            source.fft().readFft(bars);
            try {
                Thread.sleep(5); // ~a 60 Hz frame, which the ballistics are timed against
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        } while (System.currentTimeMillis() < until);
        return bars;
    }

    static int peakBar(int[] bars) {
        int peak = 0, at = -1;
        for (int i = 0; i < bars.length; i++) {
            if (bars[i] > peak) { peak = bars[i]; at = i; }
        }
        return at;
    }

    @Test
    @DisplayName("the spectrum bars stand where the note's frequency is")
    void spectrum() {
        int[] bars = settle(50);
        for (int bar : bars) assertEquals(0, bar, "silence is not silent");

        noteOn(0, 60, 127); // middle C, 262 Hz, which the printed axis puts just past bar 10.5
        bars = settle(200);
        int at = peakBar(bars);
        assertEquals(11, at, "the fundamental is not on its bar");
        // its harmonics are an octave and an octave and a fifth above it, at 1/2 and 1/3
        assertTrue(bars[at + 12] > 0);
        assertTrue(bars[at + 19] > 0);
        assertTrue(bars[at + 12] < bars[at]);
        // the skirt either side of a partial, which is what keeps the curve from being a comb
        assertTrue(bars[at - 1] > 0 && bars[at + 1] > 0);
        // and the floor moving along under it
        assertTrue(bars[bars.length - 1] > 0, "no floor under the music");
    }

    @Test
    @DisplayName("the bars rise into a note and fall out of it rather than jumping")
    void ballistics() {
        int[] bars = new int[FftDataSource.LENGTH];
        source.fft().readFft(bars); // start the display's clock

        noteOn(0, 60, 127);
        source.fft().readFft(bars);
        int first = bars[11];
        int settled = settle(200)[11];
        assertTrue(first < settled, "the note jumped straight to its level: " + first);

        noteOff(0, 60);
        source.fft().readFft(bars);
        assertTrue(bars[11] > settled / 2, "the note-off cut the bar off: " + bars[11]);
        assertTrue(settle(600)[11] < bars[11], "the bar never fell");
    }

    @Test
    @DisplayName("a note held still keeps the bars moving")
    void shimmer() {
        noteOn(0, 60, 100);
        int[] first = settle(200).clone();
        int[] later = settle(400);
        assertFalse(Arrays.equals(first, later), "the spectrum stood still");
    }

    @Test
    @DisplayName("the note length bar is measured between note-ons")
    void noteLength() throws Exception {
        noteOn(0, 60, 100);
        // the first note is held: nothing has been measured, so the bar says full
        assertEquals(255, status(0).ticks);
        assertEquals(255, status(0).ticksLeft);

        // a sleep can overrun badly on a busy machine (a ci runner), so the bounds come from the
        // time that actually went by rather than from what was asked for
        long t0 = System.nanoTime();
        Thread.sleep(120);
        noteOff(0, 60);
        long held = System.nanoTime() - t0;
        Thread.sleep(80);
        noteOn(0, 62, 100);
        long length = System.nanoTime() - t0;

        TrackStatus s = status(0);
        // at 240 counts a second, the key down for the first part of it
        int ticks = (int) (length * 240 / 1_000_000_000L);
        int gate = (int) (held * 240 / 1_000_000_000L);
        assertTrue(ticks >= 48, "slept short: " + ticks);
        // past a second the bar rescales, and a stall that long says nothing about the meter
        assumeTrue(ticks <= 255, "stalled for " + ticks);
        assertTrue(Math.abs(s.ticks - ticks) <= 3, "measured " + s.ticks + ", elapsed " + ticks);
        assertTrue(Math.abs(s.gate - gate) <= 3 && s.gate < s.ticks, "measured " + s.gate + ", held " + gate);
        assertTrue(s.ticksLeft <= s.ticks);
    }

    @Test
    @DisplayName("the clock runs while playing and stops when paused")
    void clock() throws Exception {
        assertFalse(source.work().playing());
        assertEquals(0, source.work().generatedFrames());

        noteOn(0, 60, 100);
        assertTrue(source.work().playing());
        Thread.sleep(60);
        long frames = source.work().generatedFrames();
        assertTrue(frames > 0);
        assertTrue(source.work().timerBCount() > 0);

        source.setPaused(true);
        assertTrue(source.work().paused());
        long atPause = source.work().generatedFrames();
        assertTrue(atPause >= frames);
        Thread.sleep(60);
        assertEquals(atPause, source.work().generatedFrames());

        source.setPaused(false);
        Thread.sleep(60);
        assertTrue(source.work().generatedFrames() > atPause);

        source.stop();
        assertFalse(source.work().playing());
    }

    @Test
    @DisplayName("the panel dumps the channel's controllers, its columns lined up under their titles")
    void detail() {
        TrackDetail detail = new TrackDetail();
        detail.clear();
        // a channel that has said nothing has no panel
        assertFalse(source.trackDetail().readDetail(TrackId.FM_1, detail));

        send(ShortMessage.PROGRAM_CHANGE, 0, 0, 0);
        cc(0, 10, 96);
        noteOn(0, 60, 111);
        assertTrue(source.trackDetail().readDetail(TrackId.FM_1, detail));

        assertEquals(0, detail.lines); // a dump, not bars
        assertEquals(detail.header.length(), detail.text.length());
        assertTrue(detail.text.startsWith("001 100 127 111 R32"), detail.text);
        assertTrue(detail.text.endsWith("   1"), detail.text); // one key down
    }

    @Test
    @DisplayName("a rendered frame carries the notes onto the screen")
    void render() {
        FmDspVisualizer vis = new FmDspVisualizer(60);
        vis.setDataSource(source);
        vis.setSize(640, 400);

        noteOn(0, 60, 100);
        noteOn(9, 36, 120);
        cc(0, 10, 0);

        BufferedImage empty = paint(new FmDspVisualizer(60));
        BufferedImage frame = paint(vis);
        assertNotEquals(0, diff(empty, frame), "the source drew nothing");

        // and the other right half, which is the one this source fills with a register dump
        vis.setRightMode(RightMode.TRACK_INFO);
        assertDoesNotThrow(() -> paint(vis));
    }

    private static BufferedImage paint(FmDspVisualizer vis) {
        vis.setSize(640, 400);
        BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        vis.paint(g);
        g.dispose();
        return image;
    }

    /** pixels the two frames differ in */
    private static int diff(BufferedImage a, BufferedImage b) {
        int count = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) count++;
            }
        }
        return count;
    }
}
