package mdplayer.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Stream;

import mdplayer.Common;
import mdplayer.Setting;
import mdplayer.driver.BaseDriver;
import mdplayer.driver.BasePlugin;
import mdplayer.driver.mfi.MldDriver;
import vavi.sound.mfi.MfiChip;


/**
 * Measures how loud each MFi synthesizer plays, for {@link mdplayer.driver.mfi.MldSynth#getGain()}.
 * <p>
 * The mfi driver renders on a different synthesizer per chip, and they are ~17 dB apart for the
 * same kind of song, so one {@code MasterVolume} can't level them. This picks songs of each chip
 * at random from a directory (the default synthesizer of that chip plays them), or plays a list
 * of songs on the one {@code -Dmdplayer.mfi.synth=<name>} names, and prints each synthesizer's
 * median rms (the gain is already applied: set every {@code getGain()} to 1 to measure afresh).
 *
 * <pre>{@code
 *   java -cp <cp> mdplayer.tool.MldSynthLevelProbe <dir | @list> [songs per chip] [seconds]
 * }</pre>
 */
final class MldSynthLevelProbe {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        int perChip = args.length > 1 ? Integer.parseInt(args[1]) : 12;
        VolumeBalanceCalibrator.seconds = args.length > 2 ? Integer.parseInt(args[2]) : 15;

        VolumeBalanceCalibrator.applyLocalProperties();
        Setting setting = Setting.getInstance();
        setting.getOutputDevice().setDeviceType(Common.DEV_Null);
        setting.getOther().setWavSwitch(true);
        setting.getAutoBalance().setUseThis(false);

        List<Path> songs;
        if (args[0].startsWith("@")) {
            songs = Files.readAllLines(Path.of(args[0].substring(1))).stream().map(Path::of).toList();
        } else {
            try (Stream<Path> s = Files.walk(Path.of(args[0]))) {
                songs = new ArrayList<>(s.filter(p -> p.toString().toLowerCase().endsWith(".mld")).sorted().toList());
            }
            Collections.shuffle(songs, new Random(1));
        }

        Map<MfiChip, Integer> count = new EnumMap<>(MfiChip.class);
        Map<String, List<Double>> bySynth = new TreeMap<>();
        for (Path song : songs) {
            if (count.size() == MfiChip.values().length && count.values().stream().allMatch(n -> n >= perChip)) break;
            try {
                BasePlugin<? extends BaseDriver> plugin = VolumeBalanceCalibrator.build(song);
                try {
                    MldDriver driver = (MldDriver) plugin.getDriver();
                    MfiChip chip = driver.getDetection().chip();
                    if (count.getOrDefault(chip, 0) >= perChip || driver.getSynth() == null) continue;
                    String synth = driver.getSynth().getName();
                    double rms = VolumeBalanceCalibrator.renderMeas(plugin).full();
                    if (rms < 20) continue; // a song without notes (machine dependent only)
                    count.merge(chip, 1, Integer::sum);
                    bySynth.computeIfAbsent(synth, k -> new ArrayList<>()).add(rms);
                    System.out.printf("%-8s %-18s rms=%8.1f %s%n", chip, synth, rms, song);
                } finally {
                    VolumeBalanceCalibrator.close(plugin);
                }
            } catch (Exception e) {
                System.out.printf("skipped %s: %s%n", song, e);
            }
        }

        System.out.println();
        for (var e : bySynth.entrySet()) {
            List<Double> v = e.getValue().stream().sorted().toList();
            double median = v.size() % 2 == 1 ? v.get(v.size() / 2) : (v.get(v.size() / 2 - 1) + v.get(v.size() / 2)) / 2;
            System.out.printf("%-18s n=%2d median rms=%8.1f%n", e.getKey(), v.size(), median);
        }
    }
}
