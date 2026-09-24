/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.tool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import mdplayer.emu.nise68.FileMng;
import mdplayer.emu.nise68.Nise68;


/**
 * Builds the ZPD a Mercury-UNIT ZMUSIC set leaves to the user to make.
 * <p>
 * Such a set (ZMUSICpp + PCM8pp) ships ADPCM {@code .n44} samples, a {@code makeZPD.bat} and a {@code .zpl}, but
 * not the ZPD its ZMS names, and the tools the batch runs are hard to find. This does what the batch does and hands
 * the result to the real ZPCNV3.R (Z-MUSIC v3) running on nise68:
 * <ul>
 * <li>{@code ad2pcm} ... X68000 (MSM6258) ADPCM {@code .n44} -> 16bit big-endian mono {@code .m44}</li>
 * <li>{@code mpca -vN} ... volume N/256</li>
 * <li>{@code pcm3pcm -vN} ... volume N%, {@code -dN} ... resample from N Hz to 44.1kHz (pitch down)</li>
 * <li>{@code mixp16 a b c} ... c = a + b</li>
 * <li>{@code .zpl} (v2 syntax) -> v3 {@code .CNF}: {@code .16BITPCM_TONE bank,note,name {file}},
 *     {@code ,cO,S} -> {@code .TRUNCATE O,S}, {@code .ADPCM_BANK n} -> tone set n</li>
 * </ul>
 * The songs compile with Z-MUSIC v3 and play the 16bit tones through MPCM.
 *
 * <pre>{@code
 *   mvn -o test-compile
 *   mvn -o -P zpd antrun:run -Dargs="$HOME/Public/np2/ZMS/MERCURY/VF_LION_"
 *   mvn -o -P zpd antrun:run -Dargs='--base VICT_N44 VICT_44B'   (a set that is a diff over another)
 * }</pre>
 * The ZPD is written next to the set's ZMS unless {@code --out} says otherwise; the {@code .m44} and the
 * {@code .CNF} are made in a temporary directory. {@code --zpcnv} points at ZPCNV3.R
 * (default {@code tmp/ZM302C_X/ZPCNV3.R}).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-24 nsano initial version <br>
 */
final class MercuryZpdBuilder {

    static final Charset MS932 = Charset.forName("MS932");

    public static void main(String[] args) throws Exception {
        Path base = null, out = null, zpcnv = Path.of("tmp/ZM302C_X/ZPCNV3.R");
        List<Path> sets = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
            case "--base" -> base = Path.of(args[++i]);
            case "--out" -> out = Path.of(args[++i]);
            case "--zpcnv" -> zpcnv = Path.of(args[++i]);
            default -> sets.add(Path.of(args[i]));
            }
        }
        if (sets.isEmpty()) {
            System.err.println("usage: MercuryZpdBuilder [--base dir] [--out dir] [--zpcnv ZPCNV3.R] set_dir...");
            System.exit(1);
        }
        int rc = 0;
        for (Path set : sets) {
            try {
                Path zpd = build(set, base, out != null ? out : set, zpcnv.toAbsolutePath());
                System.out.println("OK " + zpd + " " + Files.size(zpd));
            } catch (Exception e) {
                System.out.println("NG " + set + ": " + e);
                rc = 1;
            }
        }
        System.exit(rc);
    }

    /** @return the ZPD made */
    static Path build(Path set, Path base, Path outDir, Path zpcnv) throws IOException {
        Path work = Files.createTempDirectory("zpd");
        if (base != null) copyFiles(base, work);
        copyFiles(set, work);

        String zpl = runBat(work);
        Path zms = list(work).filter(p -> lower(p).endsWith(".zms")).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no .zms in " + set));
        Matcher m = Pattern.compile("\\.adpcm_block_data\\s*=?\\s*(\\S+)", Pattern.CASE_INSENSITIVE)
                .matcher(Files.readString(zms, MS932));
        if (!m.find()) throw new IllegalArgumentException(zms + " names no .ADPCM_BLOCK_DATA");
        String zpd = m.group(1);
        String cnf = zpd.substring(0, zpd.lastIndexOf('.')) + ".CNF";
        int tones = zplToCnf(find(work, zpl), work.resolve(cnf));
        System.out.println(set.getFileName() + ": " + tones + " tones -> " + zpd);

        byte[] data = zpcnv(work, zpcnv, cnf, zpd);
        Files.createDirectories(outDir);
        Path result = outDir.resolve(zpd);
        Files.write(result, data);
        return result;
    }

    private static void copyFiles(Path from, Path to) throws IOException {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(from, Files::isRegularFile)) {
            for (Path p : ds) Files.copy(p, to.resolve(p.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Stream<Path> list(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.toList().stream();
        }
    }

    private static String lower(Path p) {
        return p.getFileName().toString().toLowerCase(Locale.ROOT);
    }

    /** the dos tools did not care about case */
    private static Path find(Path dir, String name) throws IOException {
        return list(dir).filter(p -> p.getFileName().toString().equalsIgnoreCase(name)).findFirst()
                .orElse(dir.resolve(name));
    }

    // ---- makeZPD.bat

    /** @return the .zpl the batch hands to zpcnv */
    static String runBat(Path work) throws IOException {
        String zpl = null;
        for (String line : Files.readString(find(work, "makeZPD.bat"), MS932).split("\r?\n")) {
            String[] t = line.trim().split("\\s+");
            if (t.length < 2) continue;
            List<String> opts = new ArrayList<>(), files = new ArrayList<>();
            for (int i = 1; i < t.length; i++) (t[i].startsWith("-") ? opts : files).add(t[i]);
            switch (t[0].toLowerCase(Locale.ROOT)) {
            case "ad2pcm" -> {
                for (String f : files) {
                    List<Path> ins = f.contains("*")
                            ? list(work).filter(p -> lower(p).endsWith(".n44")).toList()
                            : List.of(find(work, f));
                    for (Path in : ins) {
                        short[] pcm = decodeAdpcm(Files.readAllBytes(in));
                        String name = in.getFileName().toString();
                        writeM44(work.resolve(name.substring(0, name.lastIndexOf('.')) + ".m44"), pcm);
                    }
                }
            }
            case "mpca" -> writeM44(work.resolve(files.get(1)),
                    scale(readM44(find(work, files.get(0))), option(opts, "-v") / 256.0));
            case "pcm3pcm" -> {
                short[] pcm = readM44(find(work, files.get(0)));
                for (String o : opts) {
                    String k = o.toLowerCase(Locale.ROOT);
                    if (k.startsWith("-v")) pcm = scale(pcm, Integer.parseInt(o.substring(2)) / 100.0);
                    else if (k.startsWith("-d")) pcm = resample(pcm, Integer.parseInt(o.substring(2)), 44100);
                }
                writeM44(work.resolve(files.get(1)), pcm);
            }
            case "mixp16" -> writeM44(work.resolve(files.get(2) + ".m44"),
                    mix(readM44(find(work, files.get(0) + ".m44")), readM44(find(work, files.get(1) + ".m44"))));
            case "zpcnv" -> zpl = files.getFirst();
            default -> {
                continue;
            }
            }
            System.out.println("  " + line.trim());
        }
        if (zpl == null) throw new IllegalArgumentException("makeZPD.bat runs no zpcnv");
        return zpl;
    }

    private static int option(List<String> opts, String key) {
        return opts.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(key))
                .map(o -> Integer.parseInt(o.substring(key.length()))).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no " + key + " in " + opts));
    }

    private static final int[] STEP = {
            16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60, 66, 73, 80, 88, 97, 107, 118, 130, 143, 157,
            173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449, 494, 544, 598, 658, 724, 796, 876, 963, 1060,
            1166, 1282, 1411, 1552
    };
    private static final int[] ADJ = {-1, -1, -1, -1, 2, 4, 6, 8};

    /**
     * X68000 ADPCM (MSM6258, low nibble first), 12bit output widened to 16bit.
     * how loud ad2pcm itself made it is unknown.
     */
    static short[] decodeAdpcm(byte[] data) {
        short[] out = new short[data.length * 2];
        int x = 0, idx = 0, o = 0;
        for (byte b : data) {
            for (int n : new int[] {b & 0x0f, (b >> 4) & 0x0f}) {
                int s = STEP[idx];
                int d = s >> 3;
                if ((n & 1) != 0) d += s >> 2;
                if ((n & 2) != 0) d += s >> 1;
                if ((n & 4) != 0) d += s;
                x = Math.clamp((n & 8) != 0 ? x - d : x + d, -2048, 2047);
                idx = Math.clamp(idx + ADJ[n & 7], 0, 48);
                out[o++] = (short) (x << 4);
            }
        }
        return out;
    }

    static short[] readM44(Path p) throws IOException {
        byte[] b = Files.readAllBytes(p);
        short[] pcm = new short[b.length / 2];
        ByteBuffer.wrap(b).asShortBuffer().get(pcm);
        return pcm;
    }

    static void writeM44(Path p, short[] pcm) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(pcm.length * 2);
        bb.asShortBuffer().put(pcm);
        Files.write(p, bb.array());
    }

    private static short clip(double v) {
        return (short) Math.clamp(Math.round(v), Short.MIN_VALUE, Short.MAX_VALUE);
    }

    static short[] scale(short[] pcm, double gain) {
        short[] out = new short[pcm.length];
        for (int i = 0; i < pcm.length; i++) out[i] = clip(pcm[i] * gain);
        return out;
    }

    static short[] mix(short[] a, short[] b) {
        short[] out = new short[Math.max(a.length, b.length)];
        for (int i = 0; i < out.length; i++) out[i] = clip((i < a.length ? a[i] : 0) + (i < b.length ? b[i] : 0));
        return out;
    }

    /** linear interpolation: pcm is taken as sampled at {@code from} Hz */
    static short[] resample(short[] pcm, int from, int to) {
        short[] out = new short[(int) ((long) pcm.length * to / from)];
        double step = (double) from / to;
        for (int i = 0; i < out.length; i++) {
            double x = i * step;
            int j = (int) x;
            int a = pcm[j], b = j + 1 < pcm.length ? pcm[j + 1] : a;
            out[i] = clip(a + (b - a) * (x - j));
        }
        return out;
    }

    // ---- .zpl -> .CNF

    private static final Pattern TONE = Pattern.compile("\\.?(o-?\\d[a-g][+#-]?)\\s*=\\s*([^,\\s]+)\\s*(.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BANK = Pattern.compile("\\.ADPCM_BANK\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CUT = Pattern.compile(",\\s*c(\\d+)\\s*,\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    /** @return the number of tones */
    static int zplToCnf(Path zpl, Path cnf) throws IOException {
        List<String> out = new ArrayList<>();
        int bank = 1;
        for (String line : Files.readString(zpl, MS932).split("\r?\n")) {
            String body = line.split("/", 2)[0].trim();
            if (body.isEmpty()) continue;
            Matcher b = BANK.matcher(body);
            if (b.matches()) {
                bank = Integer.parseInt(b.group(1)); // v2 bank n: the tones that follow go to v3 tone set n
                continue;
            }
            Matcher m = TONE.matcher(body);
            if (!m.matches()) {
                System.out.println("  skip: " + body);
                continue;
            }
            String file = m.group(2), rest = m.group(3).trim();
            StringBuilder ppc = new StringBuilder();
            Matcher c = CUT.matcher(rest);
            if (c.lookingAt()) ppc.append(",.TRUNCATE ").append(c.group(1)).append(',').append(c.group(2));
            else if (!rest.isEmpty()) System.out.println("  options dropped: " + body);
            String name = file.substring(0, Math.max(file.lastIndexOf('.'), 0));
            out.add(".16BITPCM_TONE %d,%s,%s {%s%s}".formatted(bank, m.group(1).toUpperCase(Locale.ROOT),
                    name.length() > 16 ? name.substring(0, 16) : name, file, ppc));
        }
        Files.writeString(cnf, String.join("\r\n", out) + "\r\n", MS932);
        return out.size();
    }

    // ---- ZPCNV3.R on nise68

    static byte[] zpcnv(Path work, Path zpcnv, String cnf, String zpd) {
        FileMng fm = new FileMng(work.toString(), "C:");
        fm.setVFile(zpcnv.toString());
        Nise68 nise68 = new Nise68();
        nise68.setMPcm(x -> 0);
        nise68.setOpm((a, d) -> 0);
        nise68.setMidi((a, d) -> 0, 44100);
        nise68.setSCC_A((a, d) -> 0, 44100);
        nise68.init(null, false, fm, MS932);
        int rc = nise68.loadRun(zpcnv.getFileName().toString(), cnf + " " + zpd, 0x0001_2000,
                false, true, false, 2_000_000_000L, 0);
        byte[] data = fm.vReadAllBytes(zpd);
        if (rc != 0 || data == null) throw new IllegalStateException("ZPCNV3 failed: rc=" + rc);
        return data;
    }
}
