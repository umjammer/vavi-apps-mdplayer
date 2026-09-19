/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mfi;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


/**
 * What an MFi (".mld") file says about itself, read straight out of the bytes.
 * <p>
 * This is only what picking a synthesizer and filling the play list take: the header's sub
 * chunks and the vendor bytes of the machine dependent messages of the tracks. The song itself
 * is read by vavi-sound's {@code MfiMidiFileReader}.
 * <pre>
 *  "melo" length(4) headerLength(2) major(1) minor(1) tracks(1)
 *      sub chunk: type(4) length(2) data ...     (to 10 + headerLength)
 *  "trac" length(4) messages ...
 *      message: delta(1) status(1) data(1) [data(1): when "note" says so]
 *      status 0x3f, 0x7f, 0xbf, 0xff: data 0xf#: length(2) data ... / else: value(1)
 *      0xff 0xff: machine dependent, data[0] is vendor(high nibble) | carrier(low nibble)
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
public class MldFile {

    /** the header's text */
    private static final Charset SJIS = Charset.forName("MS932");

    private final Map<String, byte[]> subChunks = new LinkedHashMap<>();

    /** vendor | carrier bytes of the machine dependent messages */
    private final Set<Integer> vendorCarriers = new TreeSet<>();

    /** audio formats of "ainf" */
    private final List<Integer> audioFormats = new ArrayList<>();

    private int majorType;
    private int minorType;

    private MldFile() {
    }

    /** @return true when the bytes start as an mfi */
    public static boolean isMfi(byte[] b) {
        return b != null && b.length >= 13 && b[0] == 'm' && b[1] == 'e' && b[2] == 'l' && b[3] == 'o';
    }

    /**
     * @throws IllegalArgumentException not an mfi
     */
    public static MldFile decode(byte[] b) {
        if (!isMfi(b)) throw new IllegalArgumentException("not an mfi");

        MldFile file = new MldFile();
        int headerEnd = Math.min(b.length, 10 + u16(b, 8));
        file.majorType = b[10] & 0xff;
        file.minorType = b[11] & 0xff;

        int p = 13;
        while (p + 6 <= headerEnd) {
            String type = new String(b, p, 4, SJIS);
            int length = u16(b, p + 4);
            int end = Math.min(b.length, p + 6 + length);
            byte[] data = new byte[end - (p + 6)];
            System.arraycopy(b, p + 6, data, 0, data.length);
            file.subChunks.putIfAbsent(type, data);
            p += 6 + length;
        }

        byte[] ainf = file.subChunks.get("ainf");
        if (ainf != null && ainf.length >= 2) {
            int q = 2;
            for (int i = 0; i < (ainf[1] & 0xff) && q + 3 <= ainf.length; i++) {
                file.audioFormats.add(ainf[q] & 0xff);
                q += 3 + u16(ainf, q + 1);
            }
        }

        byte[] note = file.subChunks.get("note");
        boolean longNote = note != null && note.length >= 2 && u16(note, 0) == 1;

        p = headerEnd;
        while (p + 8 <= b.length) {
            boolean track = b[p] == 't' && b[p + 1] == 'r' && b[p + 2] == 'a' && b[p + 3] == 'c';
            long length = s32(b, p + 4) & 0xffff_ffffL;
            int end = (int) Math.min(b.length, p + 8 + length);
            if (track) {
                file.scanTrack(b, p + 8, end, longNote);
            }
            if (length <= 0) break;
            p = end;
        }
        return file;
    }

    private void scanTrack(byte[] b, int q, int end, boolean longNote) {
        while (q + 3 <= end) {
            int status = b[q + 1] & 0xff;
            if (status == 0xff || status == 0x3f || status == 0x7f || status == 0xbf) {
                int data = b[q + 2] & 0xff;
                if ((data & 0xf0) == 0xf0) {
                    if (q + 5 > end) return;
                    int length = u16(b, q + 3);
                    if (status == 0xff && data == 0xff && length > 0 && q + 5 < end) {
                        vendorCarriers.add(b[q + 5] & 0xff);
                    }
                    q += 5 + length;
                } else {
                    q += 4;
                }
            } else {
                q += longNote ? 4 : 3;
            }
        }
    }

    private static int u16(byte[] b, int p) {
        return ((b[p] & 0xff) << 8) | (b[p + 1] & 0xff);
    }

    private static int s32(byte[] b, int p) {
        return ((b[p] & 0xff) << 24) | ((b[p + 1] & 0xff) << 16) | ((b[p + 2] & 0xff) << 8) | (b[p + 3] & 0xff);
    }

    private String text(String type) {
        byte[] data = subChunks.get(type);
        if (data == null) return null;
        int length = data.length;
        while (length > 0 && data[length - 1] == 0) length--;
        String s = new String(data, 0, length, SJIS).trim();
        return s.isEmpty() ? null : s;
    }

    /** "titl", nullable */
    public String getTitle() {
        return text("titl");
    }

    /** "copy", nullable */
    public String getCopyright() {
        return text("copy");
    }

    /** "prot", the authoring tool, nullable */
    public String getProtector() {
        return text("prot");
    }

    /** "supt", what made the file for which phone, nullable */
    public String getSupport() {
        return text("supt");
    }

    /** "date", nullable */
    public String getDate() {
        return text("date");
    }

    /**
     * "vers" as a number, {@code 0x0400} for "0400"
     *
     * @return -1 when none or not a number
     */
    public int getVersion() {
        String vers = text("vers");
        if (vers == null) return -1;
        try {
            return Integer.parseInt(vers, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** the mfi generation, 1 ~ 5, -1: not told */
    public int getMajorVersion() {
        int v = getVersion();
        return v < 0 ? -1 : v >> 8;
    }

    /** vendor | carrier bytes of the machine dependent messages */
    public Set<Integer> getVendorCarriers() {
        return Collections.unmodifiableSet(vendorCarriers);
    }

    /** the audio formats of "ainf", 0x80: rohm, 0x81: fuetrek, 0x82: yamaha */
    public List<Integer> getAudioFormats() {
        return Collections.unmodifiableList(audioFormats);
    }

    public int getMajorType() {
        return majorType;
    }

    public int getMinorType() {
        return minorType;
    }

    /** the sub chunk types of the header */
    public Set<String> getSubChunkTypes() {
        return Collections.unmodifiableSet(subChunks.keySet());
    }
}
