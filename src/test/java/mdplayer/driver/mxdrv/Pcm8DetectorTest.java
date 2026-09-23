/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mxdrv;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import mdplayer.chips.Pcm8Chip;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * {@link Pcm8Detector}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-23 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class Pcm8DetectorTest {

    /** a tree of MDX to sweep, when there is one to point this at */
    @Property(name = "mdx.dir")
    String mdxDir;

    @BeforeEach
    void setup() throws Exception {
        if (Files.exists(Paths.get("local.properties"))) {
            PropsEntity.Util.bind(this);
        }
    }

    /**
     * An MDX with {@code parts} parts, the given ones holding {@code data} and the rest ending at
     * once, followed by {@code voice} as its voice data.
     */
    private static byte[] mdx(int parts, Map<Integer, byte[]> data, byte[] voice) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("TEST\r\n\u001a".getBytes());
        out.write(0); // no PDX

        ByteArrayOutputStream tracks = new ByteArrayOutputStream();
        int[] offsets = new int[parts];
        int table = (1 + parts) * 2;
        for (int i = 0; i < parts; i++) {
            offsets[i] = table + tracks.size();
            tracks.writeBytes(data.getOrDefault(i, new byte[0]));
            tracks.writeBytes(new byte[] {(byte) 0xf1, 0x00});
        }
        int voiceOffset = table + tracks.size();
        writeShort(out, voiceOffset);
        for (int offset : offsets) writeShort(out, offset);
        out.writeBytes(tracks.toByteArray());
        out.writeBytes(voice);
        return out.toByteArray();
    }

    private static void writeShort(ByteArrayOutputStream out, int v) {
        out.write(v >> 8);
        out.write(v);
    }

    private static byte[] bytes(int... v) {
        byte[] b = new byte[v.length];
        for (int i = 0; i < v.length; i++) b[i] = (byte) v[i];
        return b;
    }

    @Test
    void pcm8Song() {
        // part P: F4, a note, F0 after an LFO and a repeat
        byte[] mdx = mdx(9, Map.of(8, bytes(0xed, 0x04, 0x80, 0x30, 0xec, 0x00, 0x00, 0x10, 0x00, 0x20,
                0xf6, 0x02, 0x00, 0xed, 0x00, 0x81, 0x30, 0xf5, 0x00, 0x05)), new byte[0]);
        assertEquals(bits(0, 4), Pcm8Detector.frequencyCodes(mdx));
        assertEquals(Pcm8Chip.X68SOUND, Pcm8Detector.detect(mdx));
    }

    @Test
    void mercurySong() {
        // part R: 32 kHz 16bit PCM, PCM8PP only
        byte[] mdx = mdx(16, Map.of(10, bytes(0xe8, 0xed, 0x0c, 0x80, 0x30)), new byte[0]);
        assertEquals(bits(0x0c), Pcm8Detector.frequencyCodes(mdx));
        assertEquals(Pcm8Chip.PCM8PP, Pcm8Detector.detect(mdx));
    }

    @Test
    void noiseIsNotPcm() {
        // on an FM part $ed is the noise frequency, which says nothing about PCM8
        byte[] mdx = mdx(9, Map.of(7, bytes(0xed, 0x9f, 0x80, 0x30)), new byte[0]);
        assertEquals(new BitSet(), Pcm8Detector.frequencyCodes(mdx));
        assertEquals(Pcm8Chip.X68SOUND, Pcm8Detector.detect(mdx));
    }

    @Test
    void commandForAnotherPart() {
        // $e7 $04: part A sends F$10 to part Q
        byte[] mdx = mdx(16, Map.of(0, bytes(0xe7, 0x04, 0x09, 0xed, 0x10, 0x80, 0x30)), new byte[0]);
        assertEquals(bits(0x10), Pcm8Detector.frequencyCodes(mdx));
        assertEquals(Pcm8Chip.PCM8PP, Pcm8Detector.detect(mdx));
    }

    @Test
    void voiceDataIsNotParts() {
        // a 9 part song: what follows its offsets is part data, not parts J-W, even where it
        // would point at an $ed $3f if read as offsets
        byte[] voice = bytes(0xed, 0x3f, 0xed, 0x3f, 0xed, 0x3f, 0xed, 0x3f);
        byte[] mdx = mdx(9, Map.of(), voice);
        assertEquals(Pcm8Chip.X68SOUND, Pcm8Detector.detect(mdx));
    }

    @Test
    void notMdx() {
        assertEquals(Pcm8Chip.X68SOUND, Pcm8Detector.detect(new byte[] {0x00, 0x01}));
    }

    private static BitSet bits(int... v) {
        BitSet b = new BitSet();
        for (int i : v) b.set(i);
        return b;
    }

    /**
     * Which {@code F}s the MDX in {@code mdx.dir} give their PCM parts, and so how many of them
     * {@link Pcm8Chip#AUTO} would hand to PCM8PP.
     */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void sweep() throws Exception {
        assumeTrue(mdxDir != null && Files.isDirectory(Path.of(mdxDir)));
        Map<Integer, Integer> histogram = new TreeMap<>();
        int[] count = new int[2];
        try (Stream<Path> files = Files.walk(Path.of(mdxDir))) {
            files.filter(p -> p.toString().toLowerCase().endsWith(".mdx")).forEach(p -> {
                try {
                    byte[] mdx = Files.readAllBytes(p);
                    BitSet codes = Pcm8Detector.frequencyCodes(mdx);
                    codes.stream().forEach(c -> histogram.merge(c, 1, Integer::sum));
                    int type = Pcm8Detector.detect(mdx);
                    count[type]++;
                    if (type == Pcm8Chip.PCM8PP) System.err.println("PCM8PP: " + codes + " " + p);
                } catch (Exception e) {
                    System.err.println(e + ": " + p);
                }
            });
        }
        System.err.println("F histogram (songs): " + histogram);
        System.err.println("PCM8: " + count[0] + ", PCM8PP: " + count[1]);
    }
}
