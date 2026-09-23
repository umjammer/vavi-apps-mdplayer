/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver.mxdrv;

import java.util.BitSet;

import mdplayer.chips.Pcm8Chip;


/**
 * Tells from an MDX alone whether it was written for PCM8 or for PCM8PP (PCM8++, the Mercury-Unit
 * driver).
 * <p>
 * Neither the MDX nor the PDX says which one it wants: on the real machine it is whichever the
 * user happened to have resident, and MXDRV talks to both through the one PCM8 {@code trap #2}
 * interface. What does tell is the {@code F} command ({@code $ED}) on the PCM parts. MXDRV hands
 * its value to PCM8 untouched as the data format code in bits 15-8 of {@code D1}, and according to
 * PCM8PP.TEC (PCM8++ 0.83d) codes {@code $00}-{@code $06} mean the same to both drivers: ADPCM at
 * 3.9-15.6 kHz, 16bit and 8bit PCM at 15.6 kHz. Everything from {@code $07} up is a PCM8PP-only
 * format (the Mercury rates, stereo, variable frequency), which plain PCM8 cannot play. So a song
 * that never asks for a code above {@code $06} is a PCM8 song, played exactly by either driver,
 * and one that does is a Mercury-Unit song.
 * <p>
 * The scan is a linear walk of each part's commands, not a playback: every {@code $ED} in a
 * part is seen whether or not the song ever reaches it, which is what a decision taken before the
 * first note wants anyway. The operand lengths are those of MXDRV 2.06+17's own command table
 * ({@code L001252}).
 * <p>
 * Stock MXDRV only passes the low three bits of {@code F} on, so a Mercury song needs
 * {@link mdplayer.lib.mxdrv.MXDRV#pcmFormatMask} widened as well, which {@link MxDriver} does when
 * PCM8PP is chosen. Not one of about 120,000 MDX swept goes above {@code F5}; the codes above
 * {@code $06} are read as PCM8PP.TEC has them, where PCM8A has a table of its own for some of them.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-23 nsano initial version <br>
 * @see <a href="http://retropc.net/x68000/software/hardware/mercury/pcm8pp/">PCM8PP.X</a>
 */
public final class Pcm8Detector {

    /** the highest data format code PCM8 and PCM8PP read alike */
    public static final int PCM8_MAX_CODE = 0x06;

    /** MXDRV parts A-H are FM, P (the ADPCM one) and Q-W (PCM8's extra channels) follow */
    private static final int FIRST_PCM_PART = 8;

    /** MXDRV sets up this many parts whatever the song has */
    private static final int MAX_PARTS = 16;

    private Pcm8Detector() {}

    /**
     * @param mdx a whole MDX file, packed by LZX or not
     * @return {@link Pcm8Chip#PCM8PP} when a PCM part asks for a PCM8PP-only format,
     *         {@link Pcm8Chip#X68SOUND} otherwise, and for anything that cannot be read as MDX
     */
    public static int detect(byte[] mdx) {
        BitSet codes = frequencyCodes(mdx);
        return codes.nextSetBit(PCM8_MAX_CODE + 1) >= 0 ? Pcm8Chip.PCM8PP : Pcm8Chip.X68SOUND;
    }

    /**
     * @param mdx a whole MDX file, packed by LZX or not
     * @return every data format code the song's PCM parts give their {@code F} command, empty for
     *         a song without one or for anything that cannot be read as MDX
     */
    public static BitSet frequencyCodes(byte[] mdx) {
        BitSet codes = new BitSet();
        try {
            byte[][] buf = new byte[1][];
            int[] size = new int[1];
            MxDriver.makeMdxBuf(mdx, buf, size, new String[1]);
            new Scanner(buf[0], (u16(buf[0], 4)), size[0], codes).scan();
        } catch (RuntimeException e) {
            // not an MDX this can read; the driver will have its own say about that
        }
        return codes;
    }

    private static int u16(byte[] b, int p) {
        return ((b[p] & 0xff) << 8) | (b[p + 1] & 0xff);
    }

    /** one walk over the parts of one MDX body */
    private static class Scanner {

        final byte[] b;
        final int body;
        final int end;
        final BitSet codes;

        Scanner(byte[] b, int body, int end, BitSet codes) {
            this.b = b;
            this.body = body;
            this.end = end;
            this.codes = codes;
        }

        void scan() {
            // the offset table runs up to whatever the first offset in it points at: a 9 part
            // song has voice data where the 16 part one keeps parts J-W, and reading that as
            // offsets would make up parts out of voice parameters
            int first = u16(b, body);
            for (int i = 1; i <= MAX_PARTS && body + i * 2 + 2 <= end; i++) {
                first = Math.min(first, u16(b, body + i * 2));
            }
            int parts = Math.min(MAX_PARTS, first / 2 - 1);
            // FM parts too: $e7 $04 lets any part send a PCM part its F
            for (int part = 0; part < parts; part++) {
                int p = body + u16(b, body + 2 + part * 2);
                while (p >= body && p < end) {
                    int next = command(p, part);
                    if (next < 0) break;
                    p = next;
                }
            }
        }

        /** @return where the command after the one at {@code p} starts, -1 at the end of the part */
        int command(int p, int part) {
            int c = b[p] & 0xff;
            if (c < 0x80) return p + 1; // rest
            if (c < 0xe0) return p + 2; // note, length
            return switch (c) {
                case 0xff, 0xfd, 0xfc, 0xfb, 0xf8, 0xf0, 0xef, 0xe9 -> p + 2;
                case 0xfe, 0xf6, 0xf5, 0xf4, 0xf3, 0xf2 -> p + 3;
                case 0xfa, 0xf9, 0xf7, 0xee, 0xe8 -> p + 1;
                case 0xed -> {
                    if (part >= FIRST_PCM_PART) codes.set(b[p + 1] & 0x3f); // MXDRV keeps it as F << 2 | pan
                    yield p + 2;
                }
                // LFOs: a negative first operand is just on or off
                case 0xec, 0xeb, 0xea -> b[p + 1] < 0 ? p + 2 : p + 6;
                case 0xe7 -> extended(p);
                default -> -1; // $f1 ends or loops the part, $e0-$e6 are not commands
            };
        }

        /** {@code $e7}, whose second byte picks one of MXDRV's extended commands */
        int extended(int p) {
            return switch (b[p + 1] & 0xff) {
                case 0x01, 0x03, 0x05, 0x06 -> p + 3;
                case 0x02 -> p + 8;
                case 0x04 -> { // a command for another part, which follows as it is
                    int part = b[p + 2] & 0xff;
                    yield part < MAX_PARTS ? command(p + 3, part) : -1;
                }
                default -> -1;
            };
        }
    }
}
