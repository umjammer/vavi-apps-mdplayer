/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer;

import mdplayer.Common.EnmModel;
import mdplayer.chips.Rf5C68Chip;
import mdplayer.chips.SegaPcmChip;
import mdplayer.chips.Ym2151Chip;
import mdplayer.chips.Ym2203Chip;
import mdplayer.chips.Ym2608Chip;
import mdplayer.chips.Ym2612Chip;
import mdplayer.driver.BaseDriver;
import musicDriverInterface.MetaData;
import vavi.sound.visualizer.fmdsp.LevelDataSource;
import vavi.sound.visualizer.fmdsp.LevelDataSource.Pan;
import vavi.sound.visualizer.fmdsp.TrackId;
import vavi.sound.visualizer.fmdsp.TrackInfo;
import vavi.sound.visualizer.fmdsp.TrackStatus;
import vavi.sound.visualizer.fmdsp.WorkStateSource;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * {@link ChipFmDspSource}, on synthetic chip register states - no audio, no driver.
 * <p>
 * The chips are shared singletons served by {@link ChipRegister}, so each test rebuilds the
 * caches it reads from scratch.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class ChipFmDspSourceTest {

    EmulatedPlugin plugin;
    ChipRegister chipRegister;
    Ym2608Chip opna;
    Ym2151Chip opm;
    Ym2203Chip opn;
    SegaPcmChip segaPcm;
    Ym2612Chip opn2;
    Rf5C68Chip rf5c68;
    ChipFmDspSource source;
    final TrackStatus status = new TrackStatus();

    @BeforeEach
    void setup() {
        // the C352 and the SAA1099 keep no registers of their own: their state has to be written
        // to a real emulator and read back, so the chips need a context whose writes land somewhere
        // the OPN2 twice: a Sega System 18 VGM is two of them, and the second one has a reader of
        // its own to be tested
        mdsound.instrument.Ym2612Inst opn2Inst = new mdsound.instrument.Ym2612Inst();
        mdsound.instrument.Ym2151Inst opmInst = new mdsound.instrument.Ym2151Inst();
        plugin = EmulatedPlugin.of(new mdsound.instrument.C352Inst(), new mdsound.instrument.Saa1099Inst(),
                new mdsound.instrument.SegaPcmInst(), new mdsound.instrument.C140Inst(), new mdsound.instrument.Sn76489Inst(), new mdsound.instrument.NukedYmF262Inst(),
                new mdsound.instrument.Ym2608Inst(), new mdsound.instrument.Ym2203Inst(), opmInst, opmInst,
                new mdsound.instrument.YmF278BInst(), new mdsound.instrument.Ym3812Inst(),
                new mdsound.instrument.YmZ280BInst(), new mdsound.instrument.Rf5C68Inst(),
                opn2Inst, opn2Inst);
        chipRegister = plugin.chipRegister;
        // a chip only forwards writes to its emulator when the settings say to use one, which a
        // played song arranges and a test has to say for itself
        useEmulator(mdplayer.Setting.getInstance().getYM2608Type());
        useEmulator(mdplayer.Setting.getInstance().getYM2203Type());
        useEmulator(mdplayer.Setting.getInstance().getYM2151Type());
        useEmulator(mdplayer.Setting.getInstance().getYM3812Type());
        useEmulator(mdplayer.Setting.getInstance().getYM2612Type());
        // the OPL3 has five implementations and the plugin below registers only the nuked one
        useEmulator(mdplayer.Setting.getInstance().getYMF262Type(), 2);
        opna = chipRegister.chip(Ym2608Chip.class);
        opm = chipRegister.chip(Ym2151Chip.class);
        opn = chipRegister.chip(Ym2203Chip.class);
        segaPcm = chipRegister.chip(SegaPcmChip.class);
        opn2 = chipRegister.chip(Ym2612Chip.class);
        rf5c68 = chipRegister.chip(Rf5C68Chip.class);

        // rebuild the caches the source polls, without going through Chip#init (it needs a
        // plugin). The chips are JVM-wide singletons, so every cache a reader looks at has to be
        // scrubbed or another test's leftovers would claim rows.
        // key-off everything a played song may have left in the other chips' caches
        for (int chipId = 0; chipId < 2; chipId++) { // both OPN2s: each has a reader of its own
            chipRegister.chip(mdplayer.chips.Ym2612Chip.class).register[chipId] = new int[][] {new int[0x100], new int[0x100]};
            chipRegister.chip(mdplayer.chips.Ym2612Chip.class).keyOn[chipId] = new int[6];
        }
        chipRegister.chip(mdplayer.chips.Ym2610Chip.class).register[0] = new int[][] {new int[0x100], new int[0x100]};
        chipRegister.chip(mdplayer.chips.Ym2610Chip.class).keyOn[0] = new int[6];
        chipRegister.chip(mdplayer.chips.YmF278BChip.class).register[0] =
                new int[][] {new int[0x100], new int[0x100], new int[0x100]};

        source = new ChipFmDspSource();
        source.bind(chipRegister, null);
    }

    /** OPNA o4 c: F-number 0x26a at block 4 */
    void opnaKeyOnFm1() {
        // the chip powers up with every level at 127, which is silence - a driver writes them,
        // and the old register shadow only looked open because it was zero filled
        for (int op = 0; op < 4; op++) {
            opna.write(0, 0, 0x40 + op * 4, 0, EnmModel.VirtualModel);
        }
        opna.write(0, 0, 0xa4, (4 << 3) | (0x26a >> 8), EnmModel.VirtualModel);
        opna.write(0, 0, 0xa0, 0x26a & 0xff, EnmModel.VirtualModel);
        opna.write(0, 0, 0xb4, 0xc0, EnmModel.VirtualModel); // pan L+R
        opna.write(0, 0, 0x28, 0xf0, EnmModel.VirtualModel); // key on all four slots of ch1
    }

    @Test
    @DisplayName("fm key from the f-number registers")
    void testFmKey() {
        opnaKeyOnFm1();
        source.snapshot();

        source.readStatus(TrackId.FM_1, status);
        assertTrue(status.playing);
        assertEquals(0x40, status.key); // o4 c
        assertEquals(0x40, status.actualKey);
        assertEquals(127, status.volume); // all TL registers 0
        assertTrue(source.level(0) > 0);
        assertEquals(Pan.CENTER, source.pan(0));

        // untouched rows stay inert
        source.readStatus(TrackId.FM_2, status);
        assertFalse(status.playing);
        assertEquals(0xff, status.key);
    }

    @Test
    @DisplayName("the frequency fluctuation display gets the cents a held note has been bent by")
    void testPitchDeviation() {
        opnaKeyOnFm1();
        source.snapshot();
        source.readStatus(TrackId.FM_1, status);
        assertEquals(0, status.pitchDeviation, "a note sitting still has not moved");

        // the same note bent up a sixth of a tone, which is a vibrato and not a new note: the
        // f-number times 2^(30/1200)
        opna.write(0, 0, 0xa4, (4 << 3) | (0x275 >> 8), EnmModel.VirtualModel);
        opna.write(0, 0, 0xa0, 0x275 & 0xff, EnmModel.VirtualModel);
        source.snapshot();
        source.readStatus(TrackId.FM_1, status);
        assertEquals(0x40, status.key, "still the note it was struck at");
        assertTrue(Math.abs(status.pitchDeviation - 30) <= 6,
                "about 30 cents up, was " + status.pitchDeviation);

        // and it goes out with the key, rather than following the row to the next note
        opna.write(0, 0, 0x28, 0x00, EnmModel.VirtualModel);
        source.snapshot();
        source.readStatus(TrackId.FM_1, status);
        assertEquals(0, status.pitchDeviation);
    }

    @Test
    @DisplayName("the VOLUME DOWN counter reads the calibrated balance of the chip playing the part")
    void testVolumeDown() {
        mdplayer.Setting.Balance balance = mdplayer.Setting.getInstance().getBalance();
        int mainWas = balance.getVolume(mdsound.MDSound.Chip.MAIN_TAG, mdplayer.chips.Ym2608Chip.class);
        int fmWas = balance.getVolume("FM", mdplayer.chips.Ym2608Chip.class);
        try {
            balance.setVolume(mdsound.MDSound.Chip.MAIN_TAG, mdplayer.chips.Ym2608Chip.class, -18);
            balance.setVolume("FM", mdplayer.chips.Ym2608Chip.class, -4);

            // nothing has claimed a row yet, so there is no chip to read a correction off
            assertEquals(0, source.volumeDown(WorkStateSource.VolumePart.FM));

            opnaKeyOnFm1();
            source.snapshot();

            // the chip's own correction plus the part's trim, both of which the mixer applies
            assertEquals(-22, source.volumeDown(WorkStateSource.VolumePart.FM));
            // a part nothing is playing has no chip to read one off yet
            assertEquals(0, source.volumeDown(WorkStateSource.VolumePart.SSG));

            // the same chip's SSG part, once it sounds: its own correction, trimmed by nothing
            opna.write(0, 0, 0x00, 284 & 0xff, EnmModel.VirtualModel);
            opna.write(0, 0, 0x01, 284 >> 8, EnmModel.VirtualModel);
            opna.write(0, 0, 0x08, 15, EnmModel.VirtualModel);
            opna.write(0, 0, 0x07, 0x3e, EnmModel.VirtualModel);
            source.snapshot();
            assertEquals(-18, source.volumeDown(WorkStateSource.VolumePart.SSG));
        } finally {
            balance.setVolume(mdsound.MDSound.Chip.MAIN_TAG, mdplayer.chips.Ym2608Chip.class, mainWas);
            balance.setVolume("FM", mdplayer.chips.Ym2608Chip.class, fmWas);
        }
    }

    @Test
    @DisplayName("fm key off releases the key and the meter")
    void testFmKeyOff() {
        opnaKeyOnFm1();
        source.snapshot();
        int attack = source.level(0);

        opna.write(0, 0, 0x28, 0x00, EnmModel.VirtualModel); // key off ch1
        source.snapshot();

        source.readStatus(TrackId.FM_1, status);
        assertTrue(status.playing); // the row stays lit once used
        assertEquals(0xff, status.key); // but rests
        assertTrue(source.level(0) < attack);
    }

    @Test
    @DisplayName("fm carrier total level scales the meter")
    void testFmLevel() {
        opnaKeyOnFm1();
        source.snapshot();
        int loud = source.level(0);

        setup(); // fresh caches and source
        opnaKeyOnFm1();
        opna.write(0, 0, 0xb0, 7, EnmModel.VirtualModel); // algorithm 7: every slot is a carrier
        opna.write(0, 0, 0x40, 24, EnmModel.VirtualModel); // -18 dB on the softest one
        opna.write(0, 0, 0x44, 40, EnmModel.VirtualModel);
        opna.write(0, 0, 0x48, 40, EnmModel.VirtualModel);
        opna.write(0, 0, 0x4c, 40, EnmModel.VirtualModel);
        source.snapshot();

        assertEquals(127 - 24, sourceStatus(TrackId.FM_1).volume);
        assertTrue(source.level(0) < loud);
    }

    @Test
    @DisplayName("ssg key from the tone period, mixer bits shown")
    void testSsg() {
        int period = 284; // ~A4 at the OPNA's 1.9968 MHz SSG clock
        opna.write(0, 0, 0x00, period & 0xff, EnmModel.VirtualModel);
        opna.write(0, 0, 0x01, period >> 8, EnmModel.VirtualModel);
        opna.write(0, 0, 0x08, 15, EnmModel.VirtualModel);
        opna.write(0, 0, 0x07, 0x3e, EnmModel.VirtualModel); // tone on channel 1 only, noise off
        source.snapshot();

        source.readStatus(TrackId.SSG_1, status);
        assertTrue(status.playing);
        assertEquals(TrackInfo.SSG, status.info);
        assertEquals(0x49, status.key); // o4 a
        assertTrue(status.ssgTone);
        assertFalse(status.ssgNoise);
        assertEquals(15, status.volume);
        assertTrue(source.level(6) > 0);

        // silence it: level 0 releases the meter
        opna.write(0, 0, 0x08, 0, EnmModel.VirtualModel);
        source.snapshot();
        source.readStatus(TrackId.SSG_1, status);
        assertEquals(0xff, status.key);
    }

    @Test
    @DisplayName("adpcm from the control, delta-n and level registers")
    void testAdpcm() {
        opna.write(0, 1, 0x00, 0xa0, EnmModel.VirtualModel); // start | repeat
        opna.write(0, 1, 0x01, 0xc0, EnmModel.VirtualModel); // pan L+R
        opna.write(0, 1, 0x09, 0xba, EnmModel.VirtualModel);
        opna.write(0, 1, 0x0a, 0x49, EnmModel.VirtualModel); // delta-N 0x49ba: rate ratio 1.0
        opna.write(0, 1, 0x0b, 200, EnmModel.VirtualModel);
        source.snapshot();

        source.readStatus(TrackId.ADPCM, status);
        assertTrue(status.playing);
        assertEquals(0x40, status.key); // ratio 1.0 shows as o4 c
        assertEquals(200, status.volume);
        assertTrue(source.level(10) > 0);
        assertEquals(Pan.CENTER, source.pan(10));
    }

    @Test
    @DisplayName("rhythm key-on hits the drum meter")
    void testRhythm() {
        assertEquals(0, source.level(9));
        opna.write(0, 0, 0x10, 0x01, EnmModel.VirtualModel); // bass drum
        source.snapshot();
        int hit = source.level(9);
        assertTrue(hit > 0);

        source.snapshot(); // no new hit: the meter decays
        assertTrue(source.level(9) < hit);
    }

    @Test
    @DisplayName("opm takes the fm rows over when it keys on first")
    void testOpm() {
        for (int op = 0; op < 4; op++) {
            opm.write(0, 0, 0x60 + op * 8, 0, EnmModel.VirtualModel, 0, 0); // open the levels
        }
        opm.write(0, 0, 0x20, 0xc0 | 7, EnmModel.VirtualModel, 0, 0); // pan L+R, algorithm 7
        opm.write(0, 0, 0x28, 0x4a, EnmModel.VirtualModel, 0, 0); // o4 a
        opm.write(0, 0, 0x08, 0x78, EnmModel.VirtualModel, 0, 0); // key on all four slots of ch1
        source.snapshot();

        source.readStatus(TrackId.FM_1, status);
        assertTrue(status.playing);
        assertEquals(0x49, status.key); // o4 a
        assertTrue(source.level(0) > 0);
        assertEquals(Pan.CENTER, source.pan(0));
    }

    @Test
    @DisplayName("opna wins the fm rows over a silent opm")
    void testArbitration() {
        opnaKeyOnFm1();
        opm.register[0][0x28] = 0x4a; // a key code alone is not activity
        source.snapshot();

        source.readStatus(TrackId.FM_1, status);
        assertEquals(0x40, status.key); // the OPNA note, not the OPM one
    }

    @Test
    @DisplayName("reset clears the rows for the next song")
    void testReset() {
        opnaKeyOnFm1();
        source.snapshot();
        assertTrue(sourceStatus(TrackId.FM_1).playing);

        source.reset();
        assertFalse(sourceStatus(TrackId.FM_1).playing);
        assertEquals(0, source.level(0));
    }

    @Test
    @DisplayName("opn (ym2203) takes the fm and ssg rows, mono pans")
    void testOpn2203() {
        for (int op = 0; op < 4; op++) {
            opn.write(0, 0x40 + op * 4, 0, EnmModel.VirtualModel); // open the levels
        }
        opn.write(0, 0xa4, (4 << 3) | (0x26a >> 8), EnmModel.VirtualModel);
        opn.write(0, 0xa0, 0x26a & 0xff, EnmModel.VirtualModel);
        opn.write(0, 0x28, 0xf0, EnmModel.VirtualModel); // key on all four slots of ch1
        source.snapshot();

        source.readStatus(TrackId.FM_1, status);
        assertTrue(status.playing);
        assertEquals(0x40, status.key); // o4 c, the same F-number table as the OPNA
        assertEquals(Pan.CENTER, source.pan(0)); // the chip is mono
        assertTrue(source.level(0) > 0);

        // its SSG section claims the SSG rows
        opn.write(0, 0x00, 284 & 0xff, EnmModel.VirtualModel);
        opn.write(0, 0x01, 284 >> 8, EnmModel.VirtualModel);
        opn.write(0, 0x08, 15, EnmModel.VirtualModel);
        opn.write(0, 0x07, 0x3e, EnmModel.VirtualModel);
        source.snapshot();
        source.readStatus(TrackId.SSG_1, status);
        assertEquals(TrackInfo.SSG, status.info);
        assertEquals(0x49, status.key); // o4 a
    }

    /** SegaPCM channel {@code ch} keyed on at ratio 0.25, i.e. o2 c */
    void segaPcmKeyOn(int ch) {
        segaPcm.write(0, ch * 8 + 2, 100, EnmModel.VirtualModel); // left volume
        segaPcm.write(0, ch * 8 + 3, 100, EnmModel.VirtualModel); // right volume
        segaPcm.write(0, ch * 8 + 7, 0x40, EnmModel.VirtualModel); // ratio 0.25, two octaves down
        segaPcm.write(0, ch * 8 + 0x86, 0x00, EnmModel.VirtualModel); // bit 0 clear: keyed on
    }

    @Test
    @DisplayName("segapcm rows go to the channels that sound, wherever they sit")
    void testSegaPcm() {
        int ch = 9; // beyond the nine visible rows, the slot map has to bring it forward
        segaPcmKeyOn(ch);
        source.snapshot();

        source.readStatus(TrackId.ADPCM, status); // the first PCM row
        assertTrue(status.playing);
        assertEquals(0x20, status.key); // o2 c
        assertEquals(ch + 1, status.ppz8Ch);
        assertTrue(source.level(10) > 0);
        assertEquals(Pan.CENTER, source.pan(10));
    }

    /**
     * RF5C68 channel {@code ch} playing its samples at the chip's own rate, i.e. o4 c.
     * <p>
     * The chip decodes the writes and keeps no register file, so this goes through the emulator and
     * has to render a little before it reports the channel as sounding.
     */
    void rf5c68KeyOn(int ch) {
        for (int adr = 0; adr < 0x100; adr++) {
            rf5c68.writeMemory(0, adr, 0xc0, EnmModel.VirtualModel); // anything but 0xff, the end mark
        }
        rf5c68.write(0, 0x07, 0xc0 | ch, EnmModel.VirtualModel); // chip on, and this channel selected
        rf5c68.write(0, 0x00, 0xff, EnmModel.VirtualModel); // envelope
        rf5c68.write(0, 0x01, 0xff, EnmModel.VirtualModel); // pan, full both sides
        rf5c68.write(0, 0x02, 0x00, EnmModel.VirtualModel); // step 0x800: one sample a sample
        rf5c68.write(0, 0x03, 0x08, EnmModel.VirtualModel);
        rf5c68.write(0, 0x06, 0x00, EnmModel.VirtualModel); // start of memory
        rf5c68.write(0, 0x08, ~(1 << ch) & 0xff, EnmModel.VirtualModel); // key: the register is inverted
        plugin.settle();
    }

    @Test
    @DisplayName("rf5c68 channels reach the pcm rows")
    void testRf5C68() {
        rf5c68KeyOn(1);
        source.snapshot();

        source.readStatus(TrackId.ADPCM, status); // the first PCM row
        assertTrue(status.playing);
        assertEquals(0x40, status.key); // o4 c, the rate the sample was written at
        assertEquals(2, status.ppz8Ch); // the chip channel, one based
        assertEquals(255, status.volume);
        assertTrue(source.level(10) > 0);
    }

    /** OPN2 {@code chipId}'s channel {@code ch} (0-5) keyed on at o4 c */
    void opn2KeyOn(int chipId, int ch) {
        int port = ch / 3;
        int r = ch % 3;
        // F-number 0x284 at block 4: o4 c on the OPN2's 7.67 MHz clock, which is a different
        // number from the OPNA's for the same note
        int fnum = 0x284;
        for (int op = 0; op < 4; op++) {
            opn2.write(chipId, port, 0x40 + r + op * 4, 0, EnmModel.VirtualModel, 0);
        }
        opn2.write(chipId, port, 0xa4 + r, (4 << 3) | (fnum >> 8), EnmModel.VirtualModel, 0);
        opn2.write(chipId, port, 0xa0 + r, fnum & 0xff, EnmModel.VirtualModel, 0);
        opn2.write(chipId, port, 0xb4 + r, 0xc0, EnmModel.VirtualModel, 0); // pan L+R
        opn2.write(chipId, 0, 0x28, 0xf0 | (port << 2) | r, EnmModel.VirtualModel, 0);
        plugin.settle();
    }

    @Test
    @DisplayName("the second opn2 of a dual chip vgm gets the rows the first one leaves")
    void testOpn2SecondChip() {
        opn2KeyOn(0, 0);
        opn2KeyOn(1, 3);
        source.snapshot();

        // the first chip keeps the six FM rows, numbered as ever
        source.readStatus(TrackId.FM_1, status);
        assertTrue(status.playing);
        assertEquals(0x40, status.key); // o4 c
        assertEquals(1, source.trackNumber(TrackId.FM_1));
        assertEquals("FM", source.trackTypeName(TrackId.FM_1)); // not renamed: both chips are OPN2s

        // the second chip's channel 4 takes the first row left over, numbered on from the first
        source.readStatus(TrackId.FM_3_EX_1, status);
        assertTrue(status.playing);
        assertEquals(0x40, status.key);
        assertEquals(10, source.trackNumber(TrackId.FM_3_EX_1)); // FM10, i.e. the second chip's ch4
        assertEquals("FM", source.trackTypeName(TrackId.FM_3_EX_1));
    }

    /** OPM {@code chipId}'s channel {@code ch} (0-7) keyed on at o4 a */
    void opmKeyOn(int chipId, int ch) {
        for (int op = 0; op < 4; op++) {
            opm.write(chipId, 0, 0x60 + ch + op * 8, 0, EnmModel.VirtualModel, 0, 0);
        }
        opm.write(chipId, 0, 0x20 + ch, 0xc0 | 7, EnmModel.VirtualModel, 0, 0); // pan L+R, algorithm 7
        opm.write(chipId, 0, 0x28 + ch, 0x4a, EnmModel.VirtualModel, 0, 0); // o4 a
        opm.write(chipId, 0, 0x08, 0x78 | ch, EnmModel.VirtualModel, 0, 0); // key on all four slots
        plugin.settle();
    }

    @Test
    @DisplayName("a second chip's rows go to the channels that sound, not to its first ones")
    void testOpmSecondChip() {
        opmKeyOn(0, 0);
        // eight channels leave one FM row over, and the channel that sounds is the one to show
        opmKeyOn(1, 4);
        source.snapshot();

        source.readStatus(TrackId.FM_1, status);
        assertTrue(status.playing);
        assertEquals(0x49, status.key); // o4 a
        assertEquals(1, source.trackNumber(TrackId.FM_1));

        source.readStatus(TrackId.FM_3_EX_3, status); // the last FM row, all the first chip leaves
        assertTrue(status.playing);
        assertEquals(0x49, status.key);
        assertEquals(13, source.trackNumber(TrackId.FM_3_EX_3)); // FM13: the second chip's ch5
    }

    @Test
    @DisplayName("a sampled row held far too long to be a note is badged as a stream")
    void testStreamBadge() {
        BaseDriver driver = new BaseDriver(null) {
            @Override public void init(Common.EnmModel model, int latency, int waitTime, Object... args) {}
            @Override public void processOneFrame() {}
            @Override public MetaData retrieveMetaData(byte[] buf, Object... args) { return null; }
        };
        source.bind(chipRegister, () -> driver); // the note clock only runs off a driver's counter

        // a YMZ280B voice keyed on and left alone, which is how the Guwange rips play: the whole
        // song is one streamed sample rather than a sequence of notes
        var ymz = chipRegister.chip(mdplayer.chips.YmZ280BChip.class);
        int ch = 0;
        ymz.write(0, 0xff, 0x80, EnmModel.VirtualModel); // key on enable
        ymz.write(0, ch * 4 + 0x00, 0x00, EnmModel.VirtualModel); // pitch low
        ymz.write(0, ch * 4 + 0x02, 0xff, EnmModel.VirtualModel); // total level
        ymz.write(0, ch * 4 + 0x03, 0x08, EnmModel.VirtualModel); // pan centre
        ymz.write(0, ch * 4 + 0x01, 0xf1, EnmModel.VirtualModel); // key on, looping, 16-bit PCM
        source.snapshot();

        source.readStatus(TrackId.ADPCM, status); // the first PCM row
        assertTrue(status.playing);
        assertEquals(TrackInfo.NORMAL, status.info); // could still be a note being held
        assertNotEquals(0xff, status.key);

        // ten seconds later, never re-struck: this is one endless sample, not a note
        driver.counter = Common.VGMProcSampleRate * 10L;
        source.snapshot();
        source.readStatus(TrackId.ADPCM, status);
        assertEquals(TrackInfo.STREAM, status.info);
        assertEquals(0xff, status.key); // the key the rate happens to land on means nothing
        assertEquals(0, status.ticks);
        assertEquals(0, status.gate);
    }

    @Test
    @DisplayName("timer falls back to a synthesized tempo when no chip programs timerb")
    void testTimerFallback() {
        BaseDriver driver = new BaseDriver(null) {
            @Override public void init(Common.EnmModel model, int latency, int waitTime, Object... args) {}
            @Override public void processOneFrame() {}
            @Override public MetaData retrieveMetaData(byte[] buf, Object... args) { return null; }
        };
        source.bind(chipRegister, () -> driver);

        opnaKeyOnFm1(); // the OPNA claims the fm rows but its timerb register is 0
        driver.counter = Common.VGMProcSampleRate; // one second in
        source.snapshot();

        assertEquals(200, source.timerB());
        assertTrue(source.timerBCount() > 0); // the clock and the circle animation move
    }

    @Test
    @DisplayName("the driver's metadata fills the three comment lines")
    void testComments() {
        BaseDriver driver = new BaseDriver(null) {
            @Override public void init(Common.EnmModel model, int latency, int waitTime, Object... args) {}
            @Override public void processOneFrame() {}
            @Override public MetaData retrieveMetaData(byte[] buf, Object... args) { return null; }
        };
        // what a .mds carrying a metadata chunk hands over
        driver.metaData.set(MetaData.Tag.Title, "JAZZY NYC'91");
        driver.metaData.set(MetaData.Tag.Composer, "ctr");
        driver.metaData.set(MetaData.Tag.GameTitle, "Street Fighter III: 3rd Strike");
        source.bind(chipRegister, () -> driver);

        source.snapshot();

        assertEquals("JAZZY NYC'91", source.comment(0));
        assertEquals("ctr", source.comment(1));
        // no arranger, so the third line falls back to the game
        assertEquals("Street Fighter III: 3rd Strike", source.comment(2));
    }

    @Test
    @DisplayName("the credit line falls back to the artist, which is all MDSDRV's MML has")
    void testCommentArtistFallback() {
        BaseDriver driver = new BaseDriver(null) {
            @Override public void init(Common.EnmModel model, int latency, int waitTime, Object... args) {}
            @Override public void processOneFrame() {}
            @Override public MetaData retrieveMetaData(byte[] buf, Object... args) { return null; }
        };
        driver.metaData.set(MetaData.Tag.Title, "JAZZY NYC'91");
        driver.metaData.set(MetaData.Tag.Artist, "ctr"); // #author, with no #composer anywhere
        source.bind(chipRegister, () -> driver);

        source.snapshot();

        assertEquals("ctr", source.comment(1));
    }

    @Test
    @DisplayName("row titles, numbers and meter labels follow the claimed chips")
    void testDynamicLabels() {
        opm.write(0, 0, 0x28, 0x4a, EnmModel.VirtualModel, 0, 0);
        opm.write(0, 0, 0x08, 0x78, EnmModel.VirtualModel, 0, 0); // key on ch1
        source.snapshot();

        // rows and meters are both labelled by section, as the original strip is
        assertEquals("FM", source.trackTypeName(TrackId.FM_1));
        assertEquals(1, source.trackNumber(TrackId.FM_1));
        assertEquals("FM1", source.label(0));
        // an untouched row keeps the built-in label
        assertEquals(-1, source.trackNumber(TrackId.SSG_1));

        // the auto view shows the rows in use, nothing else yet
        TrackId[] disp = source.displayTracks();
        assertEquals(1, disp.length);
        assertEquals(TrackId.FM_1, disp[0]);
    }

    @Test
    @DisplayName("auto view collects used rows across groups, in group order")
    void testDisplayTracks() {
        assertNull(source.displayTracks()); // nothing sounded yet: default layout

        opnaKeyOnFm1();
        opna.write(0, 1, 0x00, 0xa0, EnmModel.VirtualModel); // and the ADPCM part
        opna.write(0, 1, 0x09, 0xba, EnmModel.VirtualModel);
        opna.write(0, 1, 0x0a, 0x49, EnmModel.VirtualModel);
        opna.write(0, 1, 0x0b, 200, EnmModel.VirtualModel);
        source.snapshot();
        source.snapshot(); // adpcm keyOn edge needs the ctrl change seen once

        TrackId[] disp = source.displayTracks();
        assertEquals(TrackId.FM_1, disp[0]);
        assertEquals(TrackId.ADPCM, disp[disp.length - 1]);
    }

    @Test
    @DisplayName("a 9 channel opl3 gets 9 rows and 9 meters, not the opna's 6")
    void testOpl3Meters() {
        var opl3 = chipRegister.chip(mdplayer.chips.YmF262Chip.class);
        for (int ch = 0; ch < 9; ch++) {
            opl3.write(0, 0, 0xa0 + ch, 0x40 + ch, EnmModel.VirtualModel); // an f-number each
            opl3.write(0, 0, 0xb0 + ch, 0x20 | (4 << 2), EnmModel.VirtualModel); // key on, block 4
            opl3.write(0, 0, 0xc0 + ch, 0x30, EnmModel.VirtualModel); // both outputs
        }
        plugin.settle();
        source.snapshot();

        // every one of the nine channels has its own row ...
        TrackId[] rows = {TrackId.FM_1, TrackId.FM_2, TrackId.FM_3, TrackId.FM_4, TrackId.FM_5,
                TrackId.FM_6, TrackId.FM_3_EX_1, TrackId.FM_3_EX_2, TrackId.FM_3_EX_3};
        for (int ch = 0; ch < rows.length; ch++) {
            // every row is an FM channel, numbered 1..9 - not nine repeats of the chip name
            assertEquals("FM", source.trackTypeName(rows[ch]), "row " + ch);
            assertEquals(ch + 1, source.trackNumber(rows[ch]), "row " + ch);
            assertTrue(sourceStatus(rows[ch]).playing, "row " + ch);
        }
        // ... and its own meter, where the OPNA's extension rows share FM3's
        for (int c = 0; c < 9; c++) {
            assertTrue(source.level(c) > 0, "meter " + c);
        }
        // a nine wide FM span is numbered every third column, the way the original marks FM1/FM4
        assertEquals("FM1", source.label(0));
        assertEquals("FM4", source.label(3));
        assertEquals("FM7", source.label(6));
        assertEquals(9, source.displayTracks().length);

        // the key/pan readout under each meter must follow the same channel, not the PC-98
        // assignment that puts SSG under columns 6..8
        for (int c = 0; c < 9; c++) {
            assertEquals(rows[c], source.track(c), "meter " + c + " readout");
        }
    }

    @Test
    @DisplayName("detune: the cents the f-number sits off the note it shows")
    void testFmDetune() {
        opnaKeyOnFm1();
        // the note's own f-number, which is a whole number and so a shade off the note itself
        assertEquals(0, detuneOfFnum(0x26a), 2, "o4 c as near as the chip can hold it");

        // a quarter tone up and down, which is as far as the reading can go before the key moves
        assertEquals(25, detuneOfFnum((int) Math.round(0x26a * Math.pow(2, 0.25 / 12))), 2);
        assertEquals(-25, detuneOfFnum((int) Math.round(0x26a * Math.pow(2, -0.25 / 12))), 2);

        // and the SSG's period, which reads the other way round
        opna.write(0, 0, 0x08, 15, EnmModel.VirtualModel);
        opna.write(0, 0, 0x07, 0x3e, EnmModel.VirtualModel);
        int period = 284; // about o4 c
        opna.write(0, 0, 0x00, period & 0xff, EnmModel.VirtualModel);
        opna.write(0, 0, 0x01, period >> 8, EnmModel.VirtualModel);
        source.snapshot();
        source.readStatus(TrackId.SSG_1, status);
        int inTune = status.detune;
        opna.write(0, 0, 0x00, (period - 4) & 0xff, EnmModel.VirtualModel); // a shorter period is sharp
        source.snapshot();
        source.readStatus(TrackId.SSG_1, status);
        assertTrue(status.detune > inTune, "a shorter period reads sharper");
    }

    private int detuneOfFnum(int fnum) {
        opna.write(0, 0, 0xa4, (4 << 3) | (fnum >> 8), EnmModel.VirtualModel);
        opna.write(0, 0, 0xa0, fnum & 0xff, EnmModel.VirtualModel);
        source.snapshot();
        source.readStatus(TrackId.FM_1, status);
        return status.detune;
    }

    @Test
    @DisplayName("the opl2 fills a row, keyboard and meter from its own channel view")
    void testOpl2() {
        var opl2 = chipRegister.chip(mdplayer.chips.Ym3812Chip.class);
        // channel 1: its two operators are register slots 0 and 3
        for (int slot : new int[] {0, 3}) {
            opl2.write(0, 0x20 + slot, 0x01, EnmModel.VirtualModel); // multiple 1
            opl2.write(0, 0x40 + slot, 0x00, EnmModel.VirtualModel); // full level
            opl2.write(0, 0x60 + slot, 0xf0, EnmModel.VirtualModel); // fastest attack
            opl2.write(0, 0x80 + slot, 0x0f, EnmModel.VirtualModel);
        }
        opl2.write(0, 0xc0, 0x01, EnmModel.VirtualModel); // both operators reach the output
        int fnum = 0x120; // an o3 note at block 4
        opl2.write(0, 0xa0, fnum & 0xff, EnmModel.VirtualModel);
        opl2.write(0, 0xb0, 0x20 | (4 << 2) | (fnum >> 8), EnmModel.VirtualModel); // key on
        source.snapshot();

        // the chip's own register file is not the point - the row is, and the meter beside it
        source.readStatus(TrackId.FM_1, status);
        assertTrue(status.playing, "the OPL2 has to claim a row for a channel it is playing");
        assertNotEquals(0xff, status.key, "and put a key on the keyboard");
        assertTrue(source.level(0) > 0, "and drive its volume slot");
        assertEquals("FM1", source.label(0)); // the section, the chip being the only one in it
    }

    @Test
    @DisplayName("the opl4's fm half fills rows too, not only its wave half")
    void testOpl4Fm() {
        int[][] banks = chipRegister.chip(mdplayer.chips.YmF278BChip.class).register[0];
        int[] fm = banks[0];
        for (int slot : new int[] {0, 3}) {
            fm[0x20 + slot] = 0x01;
            fm[0x40 + slot] = 0x00;
            fm[0x60 + slot] = 0xf0;
            fm[0x80 + slot] = 0x0f;
        }
        fm[0xc0] = 0x31; // both operators out, both sides
        int fnum = 0x120;
        fm[0xa0] = fnum & 0xff;
        fm[0xb0] = 0x20 | (4 << 2) | (fnum >> 8); // key on
        source.snapshot();

        source.readStatus(TrackId.FM_1, status);
        assertTrue(status.playing, "the OPL4's FM channels are OPL3 and have to show like any FM");
        assertNotEquals(0xff, status.key);
        assertEquals(Pan.CENTER, source.pan(0));
    }

    @Test
    @DisplayName("TN: numbers the distinct voices the chip is played with")
    void testToneNumber() {
        opnaKeyOnFm1();
        writeVoice(0, 0x71); // a voice on FM1, and the same one again on FM3
        writeVoice(2, 0x71);
        writeVoice(1, 0x42); // and a different one on FM2
        for (int ch = 1; ch < 3; ch++) {
            opna.write(0, 0, 0xa4 + ch, (4 << 3) | (0x26a >> 8), EnmModel.VirtualModel);
            opna.write(0, 0, 0xa0 + ch, 0x26a & 0xff, EnmModel.VirtualModel);
            opna.write(0, 0, 0xb4 + ch, 0xc0, EnmModel.VirtualModel);
            opna.write(0, 0, 0x28, 0xf0 | ch, EnmModel.VirtualModel);
        }
        source.snapshot();

        source.readStatus(TrackId.FM_1, status);
        int first = status.toneNum;
        source.readStatus(TrackId.FM_3, status);
        assertEquals(first, status.toneNum, "the same registers are the same voice");
        source.readStatus(TrackId.FM_2, status);
        assertNotEquals(first, status.toneNum, "and different ones are not");

        // the total level is the channel's volume as much as its voice: turning a part down must
        // not read as it having changed instrument
        for (int op = 0; op < 4; op++) {
            opna.write(0, 0, 0x40 + op * 4, 40, EnmModel.VirtualModel);
        }
        source.snapshot();
        source.readStatus(TrackId.FM_1, status);
        assertEquals(first, status.toneNum, "a volume change is not a voice change");
    }

    /** writes one operator register block, so that the channel has a voice to be numbered by */
    private void writeVoice(int ch, int value) {
        for (int op = 0; op < 4; op++) {
            for (int reg : new int[] {0x30, 0x50, 0x60, 0x70, 0x80, 0x90}) {
                opna.write(0, 0, reg + ch + op * 4, value ^ op, EnmModel.VirtualModel);
            }
        }
        opna.write(0, 0, 0xb0 + ch, value & 0x3f, EnmModel.VirtualModel);
    }

    @Test
    @DisplayName("M: shows the chip's own LFO where it reaches a channel")
    void testHardwareLfo() {
        opnaKeyOnFm1();
        source.snapshot();
        source.readStatus(TrackId.FM_1, status);
        assertEquals("--------", status.status, "no LFO written yet");

        // the LFO running is not enough on its own - the channel has to be sensitive to it
        opna.write(0, 0, 0x22, 0x08 | 3, EnmModel.VirtualModel);
        source.snapshot();
        source.readStatus(TrackId.FM_1, status);
        assertEquals("--------", status.status, "running, but PMS and AMS are both zero");

        // PMS in bits 0-2 of register 0xb4, over the pan bits the note already set
        opna.write(0, 0, 0xb4, 0xc0 | 5, EnmModel.VirtualModel);
        source.snapshot();
        source.readStatus(TrackId.FM_1, status);
        assertEquals("P-----H-", status.status, "the LFO is bending the pitch, and it is the chip's");

        // AMS in bits 4-5, which also needs an operator switched to follow it
        opna.write(0, 0, 0xb4, 0xc0 | 5 | (2 << 4), EnmModel.VirtualModel);
        source.snapshot();
        source.readStatus(TrackId.FM_1, status);
        assertEquals("P-----H-", status.status, "AMS alone, no operator following");
        opna.write(0, 0, 0x60, 0x80, EnmModel.VirtualModel);
        source.snapshot();
        source.readStatus(TrackId.FM_1, status);
        assertEquals("PA----H-", status.status);

        // and switching the LFO off takes both away again
        opna.write(0, 0, 0x22, 0, EnmModel.VirtualModel);
        source.snapshot();
        source.readStatus(TrackId.FM_1, status);
        assertEquals("--------", status.status);
    }

    @Test
    @DisplayName("the opna keeps the classic meter columns")
    void testOpnaMeterLayout() {
        opnaKeyOnFm1();
        opna.write(0, 0, 0x00, 284 & 0xff, EnmModel.VirtualModel); // and an SSG note
        opna.write(0, 0, 0x01, 284 >> 8, EnmModel.VirtualModel);
        opna.write(0, 0, 0x08, 15, EnmModel.VirtualModel);
        opna.write(0, 0, 0x07, 0x3e, EnmModel.VirtualModel);
        opna.write(0, 1, 0x00, 0xa0, EnmModel.VirtualModel); // and the ADPCM part
        opna.write(0, 1, 0x09, 0xba, EnmModel.VirtualModel);
        opna.write(0, 1, 0x0a, 0x49, EnmModel.VirtualModel);
        opna.write(0, 1, 0x0b, 200, EnmModel.VirtualModel);
        source.snapshot();

        // the original strip's own labels: FM1, FM4, SSG, ADP
        assertEquals("FM1", source.label(0));
        assertEquals("FM4", source.label(3));
        assertEquals("SSG", source.label(6));
        assertEquals("ADP", source.label(10)); // a one column span keeps to 3 characters
        assertEquals("FM", source.trackTypeName(TrackId.FM_1));
        assertEquals("SSG", source.trackTypeName(TrackId.SSG_1));
        assertTrue(source.level(0) > 0);
        assertTrue(source.level(6) > 0);

        // and the readouts sit where the original puts them
        assertEquals(TrackId.FM_1, source.track(0));
        assertEquals(TrackId.SSG_1, source.track(6));
        assertEquals(TrackId.ADPCM, source.track(10));
    }

    @Test
    @DisplayName("c352 channels take the pcm rows, wherever they sit")
    void testC352() {
        var c352 = chipRegister.chip(mdplayer.chips.C352Chip.class);
        int ch = 5;
        c352.write(0, ch * 8, (200 << 8) | 200, EnmModel.VirtualModel); // front volume, l and r
        c352.write(0, ch * 8 + 2, 0x8000, EnmModel.VirtualModel); // half the rate, an octave down
        c352.write(0, ch * 8 + 3, 0x4000, EnmModel.VirtualModel); // ask for a key on
        c352.write(0, ch * 8 + 4, 4, EnmModel.VirtualModel); // wave bank
        c352.write(0, 0x202, 0, EnmModel.VirtualModel); // execute it, raising the chip's busy bit
        source.snapshot();

        source.readStatus(TrackId.ADPCM, status); // the first PCM row
        assertTrue(status.playing);
        assertEquals(0x30, status.key); // ratio 0.5 is an octave under o4 c
        assertEquals(ch + 1, status.ppz8Ch);
        assertEquals("PCM", source.trackTypeName(TrackId.ADPCM));
        assertTrue(source.level(10) > 0);
        assertEquals(Pan.CENTER, source.pan(10));
        assertEquals(4, status.toneNum); // the wave bank, in the PROG column
        // the renderer blanks the key and pan of a PPZ8 or PDZF row: the C352 is neither
        assertNotEquals(TrackInfo.PPZ8, status.info);
        assertNotEquals(TrackInfo.PDZF, status.info);
    }

    @Test
    @DisplayName("opl4 wave channels are read from the slot registers, which start at 8")
    void testYmF278BWave() {
        int[] wave = chipRegister.chip(mdplayer.chips.YmF278BChip.class).register[0][2];
        int ch = 3;
        // group n of 24 starts at 8 + n * 24: F-number 0 and octave 0 is the unshifted rate
        wave[0x08 + ch] = 0x05; // wave number, low eight bits
        wave[0x20 + ch] = 0x01; // its ninth bit in bit 0; F-number low is bits 1-7, so 0
        wave[0x38 + ch] = 0; // F-number high and octave
        wave[0x50 + ch] = 0; // total level 0, full volume
        wave[0x68 + ch] = 0x80; // key on, pan centre
        source.snapshot();

        source.readStatus(TrackId.ADPCM, status);
        assertTrue(status.playing);
        assertEquals(0x40, status.key); // the unshifted rate shows as o4 c
        assertEquals(127, status.volume);
        assertEquals(0x105, status.toneNum); // the nine bit wave number, in the PROG column
        assertTrue(source.level(10) > 0);
        assertEquals(Pan.CENTER, source.pan(10));
        // the renderer blanks the key and pan of a PPZ8 or PDZF row: the OPL4 wave part is neither
        assertNotEquals(TrackInfo.PPZ8, status.info);
        assertNotEquals(TrackInfo.PDZF, status.info);

        // bit 4 is the DO1 pin, which a MoonSound leaves unwired
        wave[0x68 + ch] = 0x90;
        source.snapshot();
        assertEquals(Pan.NONE, source.pan(10));
    }

    @Test
    @DisplayName("saa1099's six channels take the wider fm rows, labelled for what they are")
    void testSaa1099() {
        var saa = chipRegister.chip(mdplayer.chips.Saa1099Chip.class);
        saa.write(0, 0x1c, 0x01, EnmModel.VirtualModel); // the chip enabled at all
        saa.write(0, 0x14, 0x3f, EnmModel.VirtualModel); // tone on every channel
        saa.write(0, 0x00, 0xa0, EnmModel.VirtualModel); // channel 0: right only
        saa.write(0, 0x08, 5, EnmModel.VirtualModel); // its frequency
        saa.write(0, 0x10, 0x54, EnmModel.VirtualModel); // octave 4 for ch 0, 5 for ch 1 - shared
        saa.write(0, 0x01, 0x0b, EnmModel.VirtualModel); // channel 1: left only
        saa.write(0, 0x09, 5, EnmModel.VirtualModel);
        source.snapshot();

        // 8 MHz / 256 * 2^4 / (511 - 5) = 988.1 Hz, which is B5
        source.readStatus(TrackId.FM_1, status);
        assertTrue(status.playing);
        assertEquals(0x5b, status.key); // o5 b
        assertEquals(TrackInfo.SSG, status.info);
        assertTrue(status.ssgTone);
        assertEquals("SSG", source.trackTypeName(TrackId.FM_1));
        assertEquals(Pan.RIGHT, source.pan(0));

        // the octave nibble of the odd channel is the high one: an octave up
        source.readStatus(TrackId.FM_2, status);
        assertEquals(0x6b, status.key); // o6 b
        assertEquals(Pan.LEFT, source.pan(1));
    }

    @Test
    @DisplayName("c140 channels take the pcm rows, an octave apart for double the pitch")
    void testC140() {
        var c140 = chipRegister.chip(mdplayer.chips.C140Chip.class);
        int ch = 2;
        c140.write(0, ch * 16, 0x40, EnmModel.VirtualModel); // right volume
        c140.write(0, ch * 16 + 1, 0x40, EnmModel.VirtualModel); // left volume
        c140.write(0, ch * 16 + 2, 0x05, EnmModel.VirtualModel); // pitch, high byte first
        c140.write(0, ch * 16 + 3, 0x74, EnmModel.VirtualModel);
        c140.write(0, ch * 16 + 4, 7, EnmModel.VirtualModel); // wave bank
        c140.write(0, ch * 16 + 5, 0x93, EnmModel.VirtualModel); // key on in bit 7
        source.snapshot();

        source.readStatus(TrackId.ADPCM, status); // the first PCM row
        assertTrue(status.playing);
        assertEquals(ch + 1, status.ppz8Ch);
        assertEquals(7, status.toneNum); // the bank, in the PROG column
        assertEquals("PCM", source.trackTypeName(TrackId.ADPCM));
        assertEquals(Pan.CENTER, source.pan(10));
        int key = status.key;
        assertTrue(key != 0xff, "the channel should sound");

        // twice the pitch register is exactly an octave up
        setup();
        c140 = chipRegister.chip(mdplayer.chips.C140Chip.class);
        c140.write(0, ch * 16, 0x40, EnmModel.VirtualModel);
        c140.write(0, ch * 16 + 1, 0x40, EnmModel.VirtualModel);
        c140.write(0, ch * 16 + 2, 0x0a, EnmModel.VirtualModel); // 0x0ae8, double 0x0574
        c140.write(0, ch * 16 + 3, 0xe8, EnmModel.VirtualModel);
        c140.write(0, ch * 16 + 5, 0x93, EnmModel.VirtualModel);
        source.snapshot();

        source.readStatus(TrackId.ADPCM, status);
        int octaveUp = status.key;
        assertEquals(12, (((octaveUp >> 4) & 0xf) * 12 + (octaveUp & 0xf))
                - (((key >> 4) & 0xf) * 12 + (key & 0xf)), "double the pitch is one octave");
    }

    @Test
    @DisplayName("readers that poll the emulator claim nothing when no song loaded one")
    void testEmulatorBackedReadersAreNullSafe() {
        // the NES, the NSF machine's NES and the HuC6280 keep no registers of their own: they ask
        // the emulator, which has nothing to give until a song loads the chip. Polling must stay
        // quiet rather than throw.
        assertDoesNotThrow(source::snapshot);
        assertDoesNotThrow(source::snapshot);

        // and nothing of theirs is on screen
        assertNull(source.displayTracks());
        for (int c = 0; c < LevelDataSource.COUNT; c++) {
            assertEquals(0, source.level(c), "meter " + c);
        }
    }

    @Test
    @DisplayName("a meter column mutes the chip channel behind it, and lets it sound again")
    void testMuteByColumn() {
        opnaKeyOnFm1();
        source.snapshot();
        assertEquals(TrackId.FM_1, source.track(0));
        try {
            source.toggleMute(0);
            assertTrue(opna.isMuted(0, 0));
            assertTrue(source.masked(TrackId.FM_1));
            assertTrue(source.masked(0));

            source.toggleMute(0);
            assertFalse(opna.isMuted(0, 0));
            assertFalse(source.masked(TrackId.FM_1));
        } finally {
            opna.setMuted(0, 0, false); // the chips outlive the test
        }
    }

    @Test
    @DisplayName("the drum meter has no row, and mutes the rhythm section")
    void testMuteRhythmColumn() {
        opna.write(0, 0, 0x10, 0x01, EnmModel.VirtualModel); // bass drum
        source.snapshot();
        assertNull(source.track(9));
        try {
            source.toggleMute(9);
            assertTrue(opna.isMuted(0, 13));
            assertTrue(source.masked(9));
        } finally {
            opna.setMuted(0, 13, false);
        }
    }

    @Test
    @DisplayName("a column nothing feeds mutes nothing")
    void testMuteEmptyColumn() {
        source.snapshot();
        assertDoesNotThrow(() -> source.toggleMute(0));
        assertFalse(source.masked(0));
        assertEquals(0, opna.muteMask(0));
    }

    @Test
    @DisplayName("fm3 and its extension slots are one channel of the chip")
    void testMuteLinked() {
        try {
            opna.setMuted(0, 10, true);
            assertEquals(1L << 2 | 1L << 9 | 1L << 10 | 1L << 11, opna.muteMask(0));
            opna.setMuted(0, 2, false);
            assertEquals(0, opna.muteMask(0));
        } finally {
            opna.setMuted(0, 2, false);
        }
    }

    TrackStatus sourceStatus(TrackId t) {
        source.readStatus(t, status);
        return status;
    }

    static void useEmulator(mdplayer.Setting.ChipType2[] types) {
        useEmulator(types, 0);
    }

    /**
     * Pins the emulator variant as well, for a chip with more than one implementation: the
     * variant a user's {@code local.properties} happens to select is not necessarily one this
     * test registered, and an unregistered one swallows every write.
     *
     * @param variant index into the chip's {@code implementations()}
     */
    static void useEmulator(mdplayer.Setting.ChipType2[] types, int variant) {
        for (mdplayer.Setting.ChipType2 type : types) {
            boolean[] useEmu = new boolean[variant + 2];
            useEmu[variant] = true;
            type.setUseEmu(useEmu);
            type.setUseReal(new boolean[] {false, false});
        }
    }
}
