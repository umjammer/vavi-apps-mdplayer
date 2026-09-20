/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;


/**
 * A midi sequence flattened onto the output clock, for the drivers that play one on a sound source
 * they render themselves - the mfi one ({@link mdplayer.driver.mfi.MldDriver}) and the smaf one
 * ({@link mdplayer.driver.smaf.SmafDriver2}).
 * <p>
 * They have no sequencer between them and the sound source: they render a frame at a time and send
 * what is due by it, so what they want is every message of every track in one list, at the output
 * frame it falls on with the tempo changes followed.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-21 nsano extracted from MldDriver <br>
 */
public final class MidiSchedule {

    private MidiSchedule() {
    }

    /** a message and the output frame it is due at */
    public record Event(long frame, MidiMessage message) {}

    /**
     * The messages of the sequence at the output frames they are due, the tempo changes followed.
     * End of track is kept - it is what the last frame of a song is measured from - and the other
     * meta messages are dropped, a synthesizer has nothing to do with them.
     *
     * @throws IllegalArgumentException the sequence is not in ticks per quarter note
     */
    public static List<Event> of(Sequence sequence, int outputRate) {
        if (sequence.getDivisionType() != Sequence.PPQ) {
            throw new IllegalArgumentException("division: " + sequence.getDivisionType());
        }
        List<MidiEvent> all = new ArrayList<>();
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                all.add(track.get(i));
            }
        }
        // stable: the order within a tick is the order in the file
        all.sort(Comparator.comparingLong(MidiEvent::getTick));

        int resolution = sequence.getResolution();
        List<Event> events = new ArrayList<>(all.size());
        long tempoTick = 0;
        double tempoMicros = 0;
        double microsPerTick = 500_000d / resolution;
        for (MidiEvent e : all) {
            double micros = tempoMicros + (e.getTick() - tempoTick) * microsPerTick;
            long frame = Math.round(micros * outputRate / 1_000_000d);
            MidiMessage m = e.getMessage();
            if (m instanceof MetaMessage meta) {
                if (meta.getType() == 0x51 && meta.getData().length >= 3) {
                    byte[] d = meta.getData();
                    int mpq = ((d[0] & 0xff) << 16) | ((d[1] & 0xff) << 8) | (d[2] & 0xff);
                    tempoTick = e.getTick();
                    tempoMicros = micros;
                    microsPerTick = (double) mpq / resolution;
                }
                if (meta.getType() == 0x2f) {
                    events.add(new Event(frame, m));
                }
                continue; // a synthesizer has nothing to do with the others
            }
            events.add(new Event(frame, m));
        }
        return events;
    }
}
