/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.zms;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Tells from a ZMUSIC song alone whether plain MPCM can play it, or whether it needs MPCMPP (the
 * Mercury-Unit MPCM).
 * <p>
 * The PCM format of an MPCM voice is the {@code @f} command's code. Codes {@code 0}-{@code 6} are
 * MPCM.X's own (ADPCM at 3.9-15.6 kHz, 16bit and 8bit PCM); everything above is MPCMPP's - the
 * Mercury rates, 16bit 44.1 kHz {@code @f13} and the like - which X68Sound's MPCM ignores, so such a
 * song comes out as noise there. The same split as {@link mdplayer.driver.mxdrv.Pcm8Detector} makes
 * for PCM8.
 * <p>
 * A ZMS is looked at as MML text, a v3 ZMD as the compiled command {@code $A6 nn} (what ZMC.X makes of
 * {@code @f nn}). The ZMD scan is a byte search, not a walk of the tracks, so a data byte can look like
 * the command; that only ever picks MPCMPP, which plays every MPCM song as MPCM.X does, and a song the
 * header says has no MPCM track is not searched at all. A v2 ZMD plays
 * its PCM through PCM8, not MPCM.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-24 nsano initial version <br>
 */
public final class MpcmDetector {

    private MpcmDetector() {
    }

    /** the highest {@code @f} code MPCM.X itself has */
    public static final int MPCM_MAX_FREQ = 6;

    /** the highest code MPCMPP has, so a byte search does not take any byte for one */
    private static final int MPCMPP_MAX_FREQ = 0x2f;

    /** ZMC.X's code for {@code @f} */
    private static final int ZMD_SET_FREQ = 0xa6;

    private static final byte[] ZMD_V3_MAGIC = {0x1a, 'Z', 'm', 'u', 'S', 'i', 'C', '0'};

    private static final Pattern FREQ = Pattern.compile("@[fF]\\s*(\\d+)");

    /** @return true when the song asks for a PCM format only MPCMPP has */
    public static boolean needsMpcmPP(byte[] data, String fileName) {
        if (data == null) return false;
        if (fileName != null && fileName.toUpperCase().endsWith(".ZMS")) return zms(data);
        if (data.length >= ZMD_V3_MAGIC.length && Arrays.equals(data, 0, ZMD_V3_MAGIC.length, ZMD_V3_MAGIC, 0, ZMD_V3_MAGIC.length)) return zmd(data);
        return false;
    }

    /** MML: {@code @f} outside {@code /} comments; the text is Shift_JIS but the commands are ASCII */
    private static boolean zms(byte[] data) {
        for (String line : new String(data, StandardCharsets.ISO_8859_1).split("\r?\n")) {
            int comment = line.indexOf('/');
            Matcher m = FREQ.matcher(comment >= 0 ? line.substring(0, comment) : line);
            while (m.find()) {
                if (Integer.parseInt(m.group(1)) > MPCM_MAX_FREQ) return true;
            }
        }
        return false;
    }

    /** where a v3 ZMD header says how many MPCM (ADPCM) tracks the song has, see {@code ZMSPlugin} */
    private static final int ZMD_MPCM_TRACKS = 0x49;

    private static boolean zmd(byte[] data) {
        // no MPCM track, nothing to play on either: and the byte search then has nothing to false-match
        if (data.length <= ZMD_MPCM_TRACKS || data[ZMD_MPCM_TRACKS] == 0) return false;
        for (int i = ZMD_V3_MAGIC.length; i + 1 < data.length; i++) {
            if ((data[i] & 0xff) == ZMD_SET_FREQ) {
                int code = data[i + 1] & 0xff;
                if (code > MPCM_MAX_FREQ && code <= MPCMPP_MAX_FREQ) return true;
            }
        }
        return false;
    }
}
