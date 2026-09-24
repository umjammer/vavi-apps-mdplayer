
package mdplayer.instruments;

import java.util.function.Consumer;

import mdsound.instrument.Ym2413Inst;
import mdsound.np.chip.NesVrc7;


public class Vrc7Inst extends Ym2413Inst {

    private final NesVrc7 chip;

    private double apu_clock_rest;

    private double rate;

    public Vrc7Inst() {
        chip = new NesVrc7();
    }

    @Override
    public String getName() {
        return "VRC7";
    }

    @Override
    public String getShortName() {
        return "VRC7";
    }

    @Override
    public void reset(int chipId) {
    }

    /**
     * @param option 0: (Consumer<int[]>) listener
     */
    @Override
    public int start(int chipId, int samplingRate, int clock, Object... option) {
        chip.setClock(clock / 2.); // masterclock(NES:1789773)
        chip.setRate(samplingRate); // samplerate
        chip.reset();
        chip.setListener(option != null ? (Consumer<int[]>) option[0] : _ -> {});
        rate = samplingRate;
        return samplingRate;
    }

    @Override
    public void stop(int chipId) {
    }

    private final int[] b = new int[2];

    @Override
    public void update(int chipId, int[][] outputs, int samples) {
        double apu_clock_per_sample = 1789773 / rate;

        for (int i = 0; i < samples; i++) {
            // tick APU / expansions
            apu_clock_rest += apu_clock_per_sample;
            int apu_clocks = (int) (apu_clock_rest);
            if (apu_clocks > 0) {
                apu_clock_rest -= apu_clocks;
            }

            chip.tick(apu_clocks);
            chip.render(b);
//            if (b[0] != 0) logger.log(Level.DEBUG, "%d".formatted(b[0]));
            outputs[0][i] += b[0] << 2; // TODO check, 11 dB louder
            outputs[1][i] += b[1] << 2;
        }
    }

    @Override
    public int write(int chipId, int port, int adr, int data) {
        chip.write(0x9010, adr);
        chip.write(0x9030, data);
        return 0;
    }
}
