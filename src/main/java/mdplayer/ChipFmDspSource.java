/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import mdplayer.driver.BaseDriver;
import mdsound.MDSound;
import mdplayer.fmdsp.FmDspChannel;
import mdplayer.fmdsp.FmDspChipReader;
import mdplayer.fmdsp.FmDspChipReader.Group;
import mdplayer.driver.BasePlugin;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;
import vavi.sound.visualizer.fmdsp.FftAnalyzer;
import vavi.sound.visualizer.fmdsp.FftDataSource;
import vavi.sound.visualizer.fmdsp.FmDspDataSource;
import vavi.sound.visualizer.fmdsp.FmDspVisualizer;
import vavi.sound.visualizer.fmdsp.LevelDataSource;
import vavi.sound.visualizer.fmdsp.TrackDetail;
import vavi.sound.visualizer.fmdsp.TrackDetailSource;
import vavi.sound.visualizer.fmdsp.TrackId;
import vavi.sound.visualizer.fmdsp.TrackInfo;
import vavi.sound.visualizer.fmdsp.TrackStatus;
import vavi.sound.visualizer.fmdsp.TrackStatusSource;
import vavi.sound.visualizer.fmdsp.WorkStateSource;
import vavi.util.event.GenericEvent;


/**
 * Feeds {@link FmDspVisualizer} from the shared chip register caches, independent of the sound
 * driver playing.
 * <p>
 * Every driver funnels its chip writes through the {@link ChipRegister} chip wrappers, which cache
 * the register file and the key-on state. This source polls those caches through the
 * {@link FmDspChipReader}s found by {@link ServiceLoader} - it names no chip and no driver, so it
 * works for any format mdplayer plays. Subscribe {@link #update(GenericEvent)} to the audio event
 * stream: the "master" event carries the rendered PCM, which clocks the ~120 Hz snapshot and feeds
 * the spectrum.
 * <p>
 * The fmdsp rows are grouped ({@linkplain Group#FM FM}, {@linkplain Group#SSG SSG},
 * {@linkplain Group#PCM PCM}, the {@linkplain Group#RHYTHM drum meter}) and each group's rows are
 * assigned to the readers that report activity, in the order they become active - whatever chips a
 * song actually plays claim the screen. Compared to a driver-specific source
 * ({@link mdplayer.driver.pmd.PmdFmDspSource}, {@link mdplayer.driver.fmp.FmpFmDspSource}) three
 * things are approximations:
 * <ul>
 * <li>the MML columns, which belong to a driver's work area and are measured off the chip here
 * instead: the key and the detune are decoded from the frequency registers rather than read as the
 * driver's note and detune, the note length and the gate are timed between key ons and key offs
 * rather than read as the note's own, and of the "M:" flags only the two the registers can answer
 * are filled - see {@link #statusOf}. The tone number is whatever the chip calls one, and a chip
 * with no instruments has none.</li>
 * <li>the level meters, which are synthesized from the level registers and a decay envelope
 * retriggered on every key-on edge, exactly like the driver-specific sources do.</li>
 * <li>key-on edges. The caches are polled, so a channel retriggered with the very same register
 * state between two snapshots is missed.</li>
 * </ul>
 * Pre-timed streams such as VGM never program TimerB; the tick counter - and with it the clock and
 * the circle animation - would freeze, so it falls back to a synthesized {@link #defaultTimerB}
 * tempo. A driver that renders nothing at all - a MIDI one, whose sound is the synthesizer's -
 * leaves the analyzer nothing to measure, and its bars come from its notes instead: see
 * {@link #readFft}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-19 nsano initial version <br>
 */
public class ChipFmDspSource implements FmDspDataSource, FftDataSource, LevelDataSource,
        TrackStatusSource, TrackDetailSource, WorkStateSource {

    private static final Logger logger = System.getLogger(ChipFmDspSource.class.getName());

    /** how fast a held note's meter sags, per snapshot */
    private static final double levelSustainDecay = 0.995;

    /** how far a held note's meter sags, as a fraction of the register level */
    private static final double levelSustain = 0.6;

    /** how fast a released note's meter falls, per snapshot */
    private static final double levelRelease = 0.95;

    /** the drum meter has no per-voice level, a hit shows at a fixed height */
    private static final double rhythmLevel = 0.75;

    /** one TimerB step [Hz], the OPNA's clock / 72 / 2 - what the fmdsp clock is calibrated to */
    private static final double timerBStepHz = 7987200.0 / 72 / 2;

    /** the tempo the tick counter falls back to when no chip programs TimerB (VGM and friends) */
    private static final int defaultTimerB = 200;

    /**
     * The clock the note length bar is measured against [Hz].
     * <p>
     * Deliberately not the TimerB tick: that runs at ~62 Hz for a stream that programs no TimerB
     * and ~133 Hz for a typical OPM song, so a sixteenth note measured barely one or two of the
     * bar's 64 columns - four counts each - and the bar read as broken. PMD and FMP do not have
     * this problem because they read a length out of the part in the driver's own clock, where a
     * quarter note is 96. This rate reproduces that scale: a quarter note at 120 bpm is 120 counts,
     * a sixteenth is 30, and the bar's 255 count full scale is a bit over a second.
     */
    private static final double noteTickHz = 240;

    /** snapshots per second, the cadence the driver-specific sources use as well */
    private static final int snapshotRate = 120;

    /** rows and level channels of each group; -1 marks a row without a meter */
    private static final Map<Group, int[]> rows = new EnumMap<>(Map.of(
            Group.FM, new int[] {
                    TrackId.FM_1.ordinal(), TrackId.FM_2.ordinal(), TrackId.FM_3.ordinal(),
                    TrackId.FM_4.ordinal(), TrackId.FM_5.ordinal(), TrackId.FM_6.ordinal(),
                    TrackId.FM_3_EX_1.ordinal(), TrackId.FM_3_EX_2.ordinal(), TrackId.FM_3_EX_3.ordinal()},
            Group.SSG, new int[] {
                    TrackId.SSG_1.ordinal(), TrackId.SSG_2.ordinal(), TrackId.SSG_3.ordinal()},
            Group.PCM, new int[] {
                    TrackId.ADPCM.ordinal(),
                    TrackId.PPZ8_1.ordinal(), TrackId.PPZ8_2.ordinal(), TrackId.PPZ8_3.ordinal(),
                    TrackId.PPZ8_4.ordinal(), TrackId.PPZ8_5.ordinal(), TrackId.PPZ8_6.ordinal(),
                    TrackId.PPZ8_7.ordinal(), TrackId.PPZ8_8.ordinal()}));

    /**
     * The order the meter columns are handed out in, which is the order of the original strip:
     * FM, SSG, the drum meter, then PCM.
     */
    private static final Group[] meterOrder = {Group.FM, Group.SSG, Group.RHYTHM, Group.PCM};

    /**
     * Columns each group keeps even when nothing claims it, so the familiar PC-98 strip - FM at
     * 0, SSG at 6, the drums at 9, PCM from 10 - survives a song that happens to use only some
     * of them. A group whose chips need more than this pushes the ones behind it along.
     */
    private static final Map<Group, Integer> meterMinimums = new EnumMap<>(Map.of(
            Group.FM, 6, Group.SSG, 3, Group.RHYTHM, 1, Group.PCM, 1));

    /** reused instances, like the chips themselves */
    private static final ServiceLoader<FmDspChipReader> readerLoader = ServiceLoader.load(FmDspChipReader.class);

    private final List<FmDspChipReader> readers = new ArrayList<>();

    /** latest snapshot, written by the driver thread, read by the EDT */
    private final TrackStatus[] tracks = new TrackStatus[TrackId.COUNT];

    /** a row is shown "playing" once its channel keyed on during this song */
    private final boolean[] used = new boolean[TrackId.COUNT];

    /** display name and number of each row, from the reader that fills it */
    private final String[] rowNames = new String[TrackId.COUNT];

    private final int[] rowNums = new int[TrackId.COUNT];

    /** meter column labels, set at the first column of each claimed span */
    private final String[] levelLabels = new String[LevelDataSource.COUNT];

    /** the row feeding each meter column, so its key/pan readout follows the same chip */
    private final TrackId[] levelTracks = new TrackId[LevelDataSource.COUNT];

    /** first meter column of each claimed reader, parallel to {@link #claims} */
    private final Map<Group, List<Integer>> meterBases = new EnumMap<>(Group.class);

    /** the rows in use, for {@code LeftMode.AUTO}; null until something sounds */
    private volatile TrackId[] dispTracks;

    private final double[] envelopes = new double[LevelDataSource.COUNT];

    /**
     * Tick count of each row's last key on, {@code -1} before the first one. The bar over the
     * keyboard wants the note's remaining length; a register file does not carry one, so the
     * length is measured between key ons instead - see {@link #noteLengths}.
     */
    private final long[] keyOnTicks = new long[TrackId.COUNT];

    /**
     * The measured length of each row's previous note, in {@link #noteTickHz} counts, {@code 0}
     * until the row has played two. A chip snapshot only learns a note's length once the next one
     * starts, so the bar shows the note that is playing counting down against the length of the one
     * before it - right for the steady runs that fill most parts, and re-scaled by the next key on
     * when the part changes. A sequenced driver reads the real length out of the part, which is why
     * {@code PmdFmDspSource} and {@code FmpFmDspSource} do not need any of this.
     */
    private final int[] noteLengths = new int[TrackId.COUNT];

    /** the note each row played at the last snapshot, {@code -1} while it rests */
    private final int[] lastNotes = new int[TrackId.COUNT];

    /** the base note of the currently sounding note before pitch bends */
    private final int[] baseNotes = new int[TrackId.COUNT];

    /**
     * The pitch each row was struck at, in cents, {@code -1} while it rests. The frequency
     * fluctuation display wants the movement since the key went down rather than the pitch itself,
     * so that a voice which simply sits off the note - a sample played at a rate between two of
     * them - reads as steady, which is what it is.
     */
    private final int[] baseCents = new int[TrackId.COUNT];

    /** tick count of each row's last key off, {@code -1} while the key is still down */
    private final long[] keyOffTicks = new long[TrackId.COUNT];

    /**
     * The measured gate of each row's previous note, in {@link #noteTickHz} counts: how much of it
     * the key was actually held for, against the {@link #noteLengths length} of the same note. A
     * sequenced driver has this as a number - PMD's q/Q - while a register file only shows the key
     * going down and coming up again, so it is timed the same way the note length is, and shown
     * against the same {@link #barScale} so that the two can be read together.
     */
    private final int[] gates = new int[TrackId.COUNT];

    /** the pitch each row played at the last snapshot, in cents; 0 while it rests */
    private final int[] lastPitches = new int[TrackId.COUNT];

    /**
     * How far a row's pitch has moved one way without being re-struck, in cents, negative while it
     * moves down; reset whenever it turns around. Moving on and on the same way is a slide rather
     * than a melody, which is the only thing a register file can be read for a portamento by -
     * see {@link #statusOf}.
     */
    private final int[] slides = new int[TrackId.COUNT];

    /** snapshots left before a row's "M:" portamento flag goes out again */
    private final int[] slideHolds = new int[TrackId.COUNT];

    /** the bar is 64 columns of 4 counts each, see {@code BAR_CNT} */
    private static final int barFullScale = 255;

    /**
     * What the note clock is divided by before it reaches the bar, so that the longest note being
     * played fits the bar's 255 counts instead of pinning it full.
     * <p>
     * The clock itself has to be fine - a sixteenth note is only a fraction of a second, and at a
     * coarse rate it rounds away to no columns at all - but a fine clock alone makes anything over
     * a second sit at full scale. Dividing by a power of two chosen from the notes actually
     * sounding keeps both ends: short notes stay detailed, long ones still count down. It is one
     * divider for every row, so rows remain comparable with each other, and it follows the music
     * because a row that has stopped no longer counts towards it.
     */
    private int barScale = 1;

    private final Pan[] pans = new Pan[LevelDataSource.COUNT];

    /** row assignment of this song, in the order the readers became active */
    private final Map<Group, List<FmDspChipReader>> claims = new EnumMap<>(Group.class);

    /** which reader/channel currently fills a row, for {@link #masked} */
    private final FmDspChipReader[] rowReaders = new FmDspChipReader[TrackId.COUNT];
    private final Group[] rowGroups = new Group[TrackId.COUNT];
    private final int[] rowChannels = new int[TrackId.COUNT];

    /** the meter column of each row, {@code -1} for a row that has none; see {@link #readDetail} */
    private final int[] rowLevels = new int[TrackId.COUNT];

    /** the level each row's registers ask for, which its envelope is read against; see there */
    private final double[] rowAmplitudes = new double[TrackId.COUNT];

    private final FmDspChannel channel = new FmDspChannel();

    private Supplier<BaseDriver> driver = () -> null;

    // work state

    private volatile BaseDriver work;
    private volatile boolean paused;
    private volatile long timerBCount;
    private volatile int timerB;
    private volatile long loopTimerBCount;
    private volatile long timerBCountLoop;

    private long lastCounter;
    private double timerBStep;
    /** the note length clock, see {@link #noteTickHz} */
    private long noteTicks;
    private double noteTickStep;
    private long loopStartTimerBCount;
    private int lastLoopCount;

    private String filename;
    private final String[] comments = new String[3];

    private final FftAnalyzer fft = new FftAnalyzer(Common.VGMProcSampleRate);

    /** frames pushed since the last snapshot */
    private int snapshotFrames;

    public ChipFmDspSource() {
        for (FmDspChipReader reader : readerLoader) {
            readers.add(reader);
            FmDspChipReader second = secondChipReader(reader);
            if (second != null) readers.add(second);
        }
        readers.sort(Comparator.comparingInt(FmDspChipReader::priority));
        for (Group g : Group.values()) {
            claims.put(g, new ArrayList<>());
            meterBases.put(g, new ArrayList<>());
        }
        Arrays.setAll(tracks, i -> new TrackStatus());
        Arrays.fill(pans, Pan.CENTER);
        reset();
    }

    /**
     * A reader for the second of a chip a VGM declared twice, or null for one that has no second
     * chip to show.
     * <p>
     * The ServiceLoader hands out one reader per chip, and a chip declared twice needs two of them.
     * They are made here, once, rather than per song: the copy is a fresh instance of the same
     * class - readers keep edge state, so it cannot be the same object - and stays inert until a
     * song turns up that really has the chip twice, which {@link FmDspChipReader#ready} decides.
     */
    private static FmDspChipReader secondChipReader(FmDspChipReader reader) {
        try {
            FmDspChipReader second = reader.getClass().getDeclaredConstructor().newInstance();
            return second.chipId(1) ? second : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            logger.log(Level.DEBUG, "no second reader for " + reader.getClass().getSimpleName(), e);
            return null;
        }
    }

    private BasePlugin<? extends BaseDriver> plugin;

    /** points this source at the playing plugin's chips and driver; call once per plugin */
    public void bind(BasePlugin<? extends BaseDriver> plugin) {
        this.plugin = plugin;
        bind(plugin.chipRegister, plugin::getDriver);
    }

    public void bind(ChipRegister chipRegister, Supplier<BaseDriver> driver) {
        this.driver = driver != null ? driver : () -> null;
        readers.forEach(r -> r.bind(chipRegister));
        readers.forEach(r -> r.bind(this.driver));
        prime();
    }

    /**
     * Takes what the chips are holding right now as the state this song starts from, rather than
     * as the first thing it played.
     * <p>
     * The chips are shared singletons and the song before this one left them keyed: an OPNA that
     * a PMD tune finished on still has notes down in it, and the next song may not even use that
     * chip. A reader spots a key on as a channel that was not keyed being keyed, and against a
     * cleared edge cache every one of those leftovers is a key on - which lights rows, and fills
     * meters, for a chip the song never writes to. So the readers are polled and read once here,
     * before the song has rendered a sample, and what they find becomes what they compare against.
     * <p>
     * Called from {@link #bind}, which is once per song and before playback: not on the first
     * snapshot, where it would swallow the song's own opening notes.
     */
    private void prime() {
        for (FmDspChipReader reader : readers) {
            if (!reader.ready()) continue;
            reader.poll();
            for (Group g : reader.groups()) {
                for (int ch = 0; ch < reader.channels(g); ch++) {
                    channel.clear();
                    reader.read(g, ch, channel);
                }
            }
        }
    }

    /**
     * Clears everything that belongs to one song. The chips are shared singletons whose caches
     * carry over between songs, and the displayed clock only ever moves forwards - call this
     * before every song of a play list.
     */
    public void reset() {
        readers.forEach(FmDspChipReader::reset);
        claims.values().forEach(List::clear);
        meterBases.values().forEach(List::clear);
        Arrays.fill(rowReaders, null);
        Arrays.fill(rowLevels, -1);
        Arrays.fill(rowAmplitudes, 0);
        Arrays.stream(tracks).forEach(ChipFmDspSource::clear);
        Arrays.fill(used, false);
        Arrays.fill(rowNames, null);
        Arrays.fill(rowNums, 0);
        Arrays.fill(levelLabels, null);
        Arrays.fill(levelTracks, null);
        dispTracks = null;
        Arrays.fill(envelopes, 0);
        Arrays.fill(keyOnTicks, -1);
        Arrays.fill(noteLengths, 0);
        Arrays.fill(lastNotes, -1);
        Arrays.fill(baseNotes, -1);
        Arrays.fill(baseCents, -1);
        Arrays.fill(keyOffTicks, -1);
        Arrays.fill(gates, 0);
        Arrays.fill(lastPitches, 0);
        Arrays.fill(slides, 0);
        Arrays.fill(slideHolds, 0);
        barScale = 1;
        Arrays.fill(pans, Pan.CENTER);
        Arrays.fill(comments, null);
        work = null;
        paused = false;
        timerBCount = 0;
        timerB = 0;
        timerBStep = 0;
        noteTicks = 0;
        noteTickStep = 0;
        lastCounter = 0;
        loopTimerBCount = 0;
        loopStartTimerBCount = 0;
        timerBCountLoop = 0;
        lastLoopCount = 0;
        snapshotFrames = 0;
        clock = 0;
        lastClockNanos = 0;
        shownFrames = 0;
    }

    /** the title shown on the file bar */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /** true while the player is paused, the chips don't know about it */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * @param event "master" ... args 0: pcm buffer, 1: offset
     */
    public void update(GenericEvent event) {
        if (!"master".equals(event.getName())) return;

        short[] buffer = (short[]) event.getArguments()[0];
        int offset = (int) event.getArguments()[1];
        for (int i = offset; i + 1 < buffer.length; i += 2) {
            fft.push((buffer[i] + buffer[i + 1]) / 2f);
            snapshotFrames++;
        }
        if (snapshotFrames >= Common.VGMProcSampleRate / snapshotRate) {
            snapshotFrames = 0;
            snapshot();
        }
    }

    // ----- snapshot -----

    /** polls the chip caches once; package-private so a test can clock it directly */
    void snapshot() {
        for (FmDspChipReader reader : readers) {
            if (reader.ready()) reader.poll();
        }

        // readers claim a group's rows when they first turn active; the assignment then sticks
        // for the song, so the rows don't jump around
        for (Group g : Group.values()) {
            List<FmDspChipReader> claimed = claims.get(g);
            for (FmDspChipReader reader : readers) {
                if (!claimed.contains(reader) && reader.ready()
                        && reader.groups().contains(g) && reader.active(g)) {
                    claimed.add(reader);
                }
            }
        }

        allocateMeters();
        // before fill: the per-row note length bar measures itself against the tick counter,
        // and its scale comes from the notes the previous snapshot left sounding
        state();
        scaleBar();

        fill(Group.FM);
        fill(Group.SSG);
        fill(Group.PCM);
        rhythm();

        nameCollisions();
        updateDisplayTracks();
    }

    /**
     * Hands the meter columns to the claimed readers, in the strip's own group order and as many
     * as each says it needs - so a nine channel FM chip gets nine meters where the OPNA's six
     * plus its three extension rows get six. Each group keeps at least its
     * {@linkplain #meterMinimums classic span}, so the layout only moves for a chip that really
     * is wider. A song with more voices than the strip has columns loses the tail.
     * <p>
     * The classic spacing is only kept while everything that can be shown still fits with it. An
     * MDX is eight FM parts and eight PCM ones, which fills the strip exactly - but not with three
     * columns held open for an SSG the song does not have, and a blank column is worth less than a
     * part that sounds.
     */
    private void allocateMeters() {
        int wanted = 0;
        for (Group g : meterOrder) wanted += Math.max(visibleSpan(g), meterMinimums.get(g));
        boolean keepSpacing = wanted <= LevelDataSource.COUNT;

        Arrays.fill(owned, false);
        int cursor = 0;
        for (Group g : meterOrder) {
            List<FmDspChipReader> claimed = claims.get(g);
            List<Integer> bases = meterBases.get(g);
            bases.clear();

            int span = 0;
            for (FmDspChipReader reader : claimed) {
                bases.add(Math.min(cursor + span, LevelDataSource.COUNT));
                span += reader.meters(g);
            }
            for (int c = cursor; c < Math.min(cursor + span, LevelDataSource.COUNT); c++) owned[c] = true;
            cursor += keepSpacing ? Math.max(span, meterMinimums.get(g)) : span;
            cursor = Math.min(cursor, LevelDataSource.COUNT);
        }
        // A reader claiming a group before the ones in front of it move the layout along - an MDX
        // whose PCM part sounds before its first FM note does - would leave what it wrote at its
        // old columns standing there for the rest of the song, a label over a bar that never moves
        // again. A column nothing owns any more shows nothing.
        for (int c = 0; c < LevelDataSource.COUNT; c++) {
            if (owned[c]) continue;
            levelLabels[c] = null;
            levelTracks[c] = null;
            envelopes[c] = 0;
        }
    }

    /**
     * The columns a group's claimed readers want between them, as far as they can be seen: a chip
     * with more channels than the group has rows - SegaPCM's sixteen against the PCM rows' nine -
     * is asking for columns that are never drawn, and those do not count towards the strip filling
     * up.
     */
    private int visibleSpan(Group g) {
        int span = 0;
        for (FmDspChipReader reader : claims.get(g)) span += reader.meters(g);
        int[] groupRows = rows.get(g);
        return Math.min(span, groupRows != null ? groupRows.length : 1); // the drum meter is one
    }

    /** meter columns a claimed reader fills, the rest are cleared out */
    private final boolean[] owned = new boolean[LevelDataSource.COUNT];

    /**
     * Names a row for its chip where the section alone would be ambiguous.
     * <p>
     * Rows are labelled for what they are - FM, SSG, PCM - which reads better than repeating one
     * chip's name down the whole strip. But two different chips can be the same sort of thing at
     * once: an MSX has the PSG on the SSG rows and the SCC on the wider block, and both of them
     * are square wave channels. Where that happens the section is no use as a name, so those rows
     * take their chip's instead.
     * <p>
     * Two of the same chip are not that case - a VGM with a second OPN2 has twelve FM channels, not
     * two kinds of FM - so a name they share tells the rows apart no better than the section does,
     * and they keep it.
     */
    private void nameCollisions() {
        // decide every row against the names as they stand, then rename: doing it in one pass
        // would have a renamed row no longer match the rows it collided with, and the second of
        // any pair would keep the ambiguous name
        boolean[] shared = new boolean[rowNames.length];
        for (int row = 0; row < rowNames.length; row++) {
            if (rowNames[row] == null || rowReaders[row] == null) continue;
            for (int other = 0; other < rowNames.length && !shared[row]; other++) {
                shared[row] = rowNames[row].equals(rowNames[other]) && rowReaders[other] != null
                        && rowReaders[other] != rowReaders[row]
                        && !Objects.equals(rowReaders[other].chipName(), rowReaders[row].chipName());
            }
        }
        for (int row = 0; row < rowNames.length; row++) {
            if (shared[row] && rowReaders[row].chipName() != null) {
                rowNames[row] = rowReaders[row].chipName();
            }
        }
    }

    /** the used rows in group order, capped at the 10 the left half fits */
    private void updateDisplayTracks() {
        List<TrackId> list = new ArrayList<>(10);
        for (Group g : new Group[] {Group.FM, Group.SSG, Group.PCM}) {
            for (int row : rows.get(g)) {
                if (used[row] && list.size() < 10) list.add(TrackId.values()[row]);
            }
        }
        dispTracks = list.isEmpty() ? null : list.toArray(new TrackId[0]);
    }

    private void fill(Group g) {
        int[] groupRows = rows.get(g);
        List<FmDspChipReader> claimed = claims.get(g);
        List<Integer> bases = meterBases.get(g);
        int slot = 0;
        for (int r = 0; r < claimed.size(); r++) {
            FmDspChipReader reader = claimed.get(r);
            int base = bases.get(r);
            for (int ch = 0; ch < reader.channels(g) && slot < groupRows.length; ch++, slot++) {
                int row = groupRows[slot];
                int meter = reader.meterOf(g, ch);
                int level = meter < 0 ? -1 : base + meter;
                if (level >= LevelDataSource.COUNT) level = -1;

                channel.clear();
                reader.read(g, ch, channel);

                if (channel.keyOn || channel.sounding) used[row] = true;
                rowNames[row] = channel.name;
                // the second of a doubled chip carries on from where the first one's numbers end,
                // so twelve OPN2 channels read FM1 to FM12 rather than FM1 to FM6 twice over
                rowNums[row] = (channel.num > 0 ? channel.num : ch + 1)
                        + reader.chipId() * reader.meters(g);
                // the strip labels the start of a span, and every third column of a wide FM one.
                // Written every snapshot, including the blanks: a column that changed hands when
                // the layout moved would otherwise keep the label its old owner left on it
                if (level >= 0) {
                    levelLabels[level] = meter == 0 || (g == Group.FM && meter % 3 == 0)
                            ? labelOf(g, reader, channel.name, meter) : null;
                }
                TrackStatus status = tracks[row];
                status.playing = used[row];
                status.info = channel.info;
                System.arraycopy(channel.fmSlotMask, 0, status.fmSlotMask, 0, 4);
                if (!channel.sounding) {
                    baseNotes[row] = -1;
                    baseCents[row] = -1;
                    status.key = 0xff;
                    status.actualKey = 0xff;
                    status.pitchDeviation = 0;
                } else {
                    if (channel.keyOn || baseNotes[row] < 0) {
                        baseNotes[row] = channel.note;
                        baseCents[row] = channel.note * 100 + channel.detune;
                    }
                    status.key = keyOf(baseNotes[row]);
                    status.actualKey = keyOf(channel.note);
                    // what the frequency fluctuation display swings on: a sample whose rate is not
                    // a note has nothing to say here, its pitch drifting every snapshot by nature
                    status.pitchDeviation = channel.sampled || channel.note < 0 ? 0
                            : channel.note * 100 + channel.detune - baseCents[row];
                }
                status.volume = channel.volume;
                status.toneNum = channel.toneNum;
                status.ssgTone = channel.ssgTone;
                status.ssgNoise = channel.ssgNoise;
                status.ssgNoiseFreq = channel.ssgNoiseFreq;
                status.ppz8Ch = channel.pcmCh;

                // a re-struck note re-attacks the meter. MXDRV keys off and back on inside one
                // snapshot, so its FM rows never show a key-on edge; without this their meters
                // would sit pinned at the sustain floor. A pitch change on a held row is the same
                // event, as it is for the note bar - but not for a streamed sample, whose pitch is
                // a playback rate that drifts every snapshot. Read before noteLength updates it.
                boolean restruck = channel.keyOn
                        || (!channel.sampled && channel.sounding && channel.note >= 0
                            && channel.note != lastNotes[row]);

                status.detune = channel.detune;
                status.status = statusOf(row, channel);

                if (noteLength(row, status, channel.keyOn, channel.sounding, channel.note, channel.sampled)) {
                    // streaming: the pitch is a playback rate, so the note it lands nearest is an
                    // artefact of the arithmetic and not something the song ever played
                    status.key = 0xff;
                    status.actualKey = 0xff;
                    status.gate = 0;
                    // say why the key and the length are blank, or a row carrying the whole song
                    // reads as a silent one. Not over a label the reader chose, which is more
                    // specific than this one.
                    if (status.info == TrackInfo.NORMAL) status.info = TrackInfo.STREAM;
                } else {
                    status.gate = Math.min(barFullScale, gates[row] / barScale);
                }

                if (level >= 0) {
                    envelope(level, restruck, channel.sounding, channel.amplitude, channel.measured);
                    pans[level] = channel.pan;
                    levelTracks[level] = TrackId.values()[row];
                }
                rowReaders[row] = reader;
                rowGroups[row] = g;
                rowChannels[row] = ch;
                rowLevels[row] = level;
                rowAmplitudes[row] = channel.amplitude;
            }
            if (slot >= groupRows.length) break;
        }
    }

    private void rhythm() {
        List<FmDspChipReader> claimed = claims.get(Group.RHYTHM);
        List<Integer> bases = meterBases.get(Group.RHYTHM);
        for (int r = 0; r < claimed.size(); r++) {
            int meter = bases.get(r);
            if (meter >= LevelDataSource.COUNT) break;

            channel.clear();
            claimed.get(r).read(Group.RHYTHM, 0, channel);
            levelLabels[meter] = channel.name != null ? channel.name : "RHY";
            if (channel.keyOn) {
                envelopes[meter] = channel.amplitude > 0 ? channel.amplitude : rhythmLevel;
            } else {
                envelopes[meter] *= levelRelease;
            }
            pans[meter] = Pan.CENTER;
        }
    }

    /**
     * The label over a meter column, the way the original strip has them: the section it belongs
     * to - FM1, FM4, SSG, RHY, ADP, PPZ8 - with a wide FM span numbered every third column so a
     * nine channel chip reads FM1, FM4, FM7.
     * <p>
     * The exception is a group two different chips share, where the section alone would not say
     * which is which: there each span carries its {@linkplain FmDspChipReader#chipName chip's
     * name} instead.
     *
     * @param meter the column within the reader's own span
     */
    private String labelOf(Group g, FmDspChipReader reader, String section, int meter) {
        String label;
        if (claims.get(g).size() > 1 && reader.chipName() != null) {
            if (meter != 0) return null; // one name per span, at its start
            label = reader.chipName();
        } else if (g == Group.FM && section != null) {
            label = section + (meter + 1);
        } else {
            label = section;
        }
        // a one column span has no room for the fourth character
        int room = reader.meters(g) > 1 ? 4 : 3;
        return label != null && label.length() > room ? label.substring(0, room) : label;
    }

    /**
     * Drives channel {@code c}'s level meter: it jumps to {@code amplitude} on key on, sags
     * towards the sustain level while the key is held, and releases otherwise. Note that this is
     * an envelope, not a measurement - see the class comment.
     */
    private void envelope(int c, boolean keyOn, boolean sounding, double amplitude, boolean measured) {
        if (measured) {
            // already an envelope, and the real one: hold the peak and let it fall, but do not
            // scale it down to a sustain level the chip is not actually at
            envelopes[c] = Math.max(amplitude, envelopes[c] * levelRelease);
            return;
        }
        if (keyOn) {
            envelopes[c] = amplitude;
        } else if (sounding) {
            envelopes[c] = Math.max(envelopes[c] * levelSustainDecay, amplitude * levelSustain);
        } else {
            envelopes[c] *= levelRelease;
        }
    }

    /**
     * Fills the note length bar of row {@code row}: {@link TrackStatus#ticks} is the length the
     * row's previous note ran for, which the renderer marks as the note's start, and
     * {@link TrackStatus#ticksLeft} is how much of it the note that is playing has left.
     * A note that outlives the measurement holds the bar at empty rather than wrapping around.
     * <p>
     * The lengths are kept at the full resolution of the note clock and only divided by
     * {@link #barScale} on the way out, so lengthening the notes never loses the short ones.
     */
    private boolean noteLength(int row, TrackStatus status, boolean keyOn, boolean sounding, int note,
                               boolean sampled) {
        // A key on is only seen when the key actually goes down between two snapshots. MXDRV keys
        // off and back on inside one, so its rows look permanently held and would never measure a
        // note at all; a pitch change on a held row is the same event as far as the bar cares.
        boolean started = keyOn || (sounding && note >= 0 && note != lastNotes[row]);
        lastNotes[row] = sounding ? note : -1;
        // the key coming up is what ends the gate; the note itself runs on until the next one
        if (!sounding && !started && keyOffTicks[row] < 0 && keyOnTicks[row] >= 0) {
            keyOffTicks[row] = noteTicks;
        }
        if (started) {
            if (keyOnTicks[row] >= 0) {
                long measured = noteTicks - keyOnTicks[row];
                noteLengths[row] = (int) Math.min(measured, maxNoteLength);
                // a note the key never came up on was played legato, so its gate is its length
                long held = keyOffTicks[row] < 0 ? measured : keyOffTicks[row] - keyOnTicks[row];
                gates[row] = Math.clamp(held, 0, maxNoteLength);
            }
            keyOnTicks[row] = noteTicks;
            keyOffTicks[row] = -1;
        }
        if (!sounding && !started) {
            // resting: nothing is counting down
            status.ticks = 0;
            status.ticksLeft = 0;
            return false;
        }
        long elapsed = keyOnTicks[row] < 0 ? 0 : noteTicks - keyOnTicks[row];
        if (sampled && elapsed > streamHold) {
            // A sampled voice that has been sounding this long without being re-struck is not
            // playing a note, it is streaming - a whole part, or a whole track, rendered as one
            // endless sample the way an arcade board does it. There is no note and no length in
            // that, only sound, so both meters stay off and the level meter carries the row.
            status.ticks = 0;
            status.ticksLeft = 0;
            return true;
        }
        if (noteLengths[row] == 0) {
            // Nothing measured yet, so this is the row's first note: it is being held and how long
            // it runs is not known yet. The bar says held, full, and empties when the note does. An
            // empty bar would say the row was silent, which is what every first note used to show.
            status.ticks = barFullScale;
            status.ticksLeft = barFullScale;
            return false;
        }
        status.ticks = noteLengths[row] / barScale;
        status.ticksLeft = (int) Math.max(0, noteLengths[row] - elapsed) / barScale;
        return false;
    }

    /**
     * How long a sampled voice may sound without being re-struck before it counts as streaming
     * rather than playing a note: longer than the bar itself can represent.
     * <p>
     * Deliberately not a number of seconds. The note clock is driven by the driver's own sample
     * counter, which not every driver advances at the same rate, so a threshold in seconds would
     * only be true for some of them. Measured against the bar it is exact whatever the rate - a
     * note too long for the bar to draw is one the bar has nothing to say about.
     * <p>
     * Four bars' worth rather than one, which lands somewhere between five and twenty seconds
     * depending on the driver. A single bar caught a PMD ADPCM part holding a long note and blanked
     * a key that was really being played; nothing sequenced holds one sample this long, while a
     * streamed track holds it for minutes, so the cost of waiting is a few seconds of a held note
     * being drawn before the row goes quiet.
     */
    private static final int streamHold = barFullScale * 4;

    /**
     * Picks {@link #barScale} from the notes that are sounding, before the rows are filled: the
     * smallest power of two that brings the longest of them inside the bar. A row that has stopped
     * does not count, so an opening drone does not coarsen the rest of the song.
     */
    private void scaleBar() {
        int longest = 0;
        for (int row = 0; row < lastNotes.length; row++) {
            if (lastNotes[row] >= 0) longest = Math.max(longest, noteLengths[row]);
        }
        int scale = 1;
        while (longest / scale > barFullScale) scale <<= 1;
        barScale = scale;
    }

    /**
     * The longest note the bar will measure, past which it stops counting down and simply stays
     * full - about a minute even at the coarsest scale, which no note outlives musically.
     */
    private static final int maxNoteLength = (int) (noteTickHz * 60);

    /** MML key: high nibble octave, low nibble note, 0xff while the part rests */
    private static int keyOf(int note) {
        return note < 0 ? 0xff : (note / 12) << 4 | note % 12;
    }

    /**
     * The eight character mnemonic fmdsp shows behind "M:", as much of it as a register file can
     * answer. The six software LFO slots stay blank - a software LFO is a driver's, and this source
     * has no driver to ask - and the two that are about the chip and the sound itself are filled:
     * <ul>
     * <li>"H", the chip's own LFO reaching this channel, from its sensitivity registers</li>
     * <li>"P", a pitch bend, measured rather than read: the pitch of a note that is still being
     * held has travelled {@link #slideCents} one way without turning back. A single jump is a
     * melody stepping to its next note on a driver that re-keys silently ({@code MXDRV} does), and
     * a vibrato turns around long before it gets that far, but a portamento or a pitch envelope
     * keeps going.</li>
     * </ul>
     * The flag is held on for {@link #slideHold} snapshots after the pitch settles so that it does
     * not flicker between the steps of a slow bend. What it cannot do is tell a driver's software
     * vibrato from its portamento: both are the driver bending a held note, and only the driver
     * knows which it called it.
     */
    private String statusOf(int row, FmDspChannel channel) {
        // in cents, so that a slide is caught while it is still inside one semitone - which is
        // most of a short one, and all of a slow one until it has been running for a while
        int pitch = channel.note * 100 + channel.detune;
        int moved = channel.sounding && !channel.keyOn && channel.note >= 0 && lastPitches[row] > 0
                ? pitch - lastPitches[row] : 0;
        lastPitches[row] = channel.sounding && channel.note >= 0 ? pitch : 0;

        if (moved == 0 || Integer.signum(slides[row]) != Integer.signum(moved)) {
            slides[row] = moved;
        } else {
            slides[row] += moved;
        }
        if (Math.abs(slides[row]) >= slideCents) slideHolds[row] = slideHold;
        else if (slideHolds[row] > 0) slideHolds[row]--;
        if (!channel.sounding) slideHolds[row] = 0;

        int i = (channel.lfoPitch ? 1 : 0) | (channel.lfoVolume ? 2 : 0) | (slideHolds[row] > 0 ? 4 : 0);
        return CHIP_STATUS[i];
    }

    /**
     * How far a held note's pitch has to travel one way before it counts as a slide, in cents.
     * A quarter tone: below that is a vibrato turning around or a driver's detune being nudged,
     * and a portamento worth drawing crosses it within a few snapshots.
     */
    private static final int slideCents = 50;

    /** how long the "M:" portamento flag stays on after the pitch stops moving, in snapshots */
    private static final int slideHold = 16;

    private static final String[] CHIP_STATUS = new String[8];

    static {
        for (int i = 0; i < CHIP_STATUS.length; i++) {
            boolean pitch = (i & 1) != 0, volume = (i & 2) != 0;
            CHIP_STATUS[i] = new String(new char[] {
                    pitch ? 'P' : '-',           // the chip's LFO is bending the pitch
                    volume ? 'A' : '-',          // and/or moving the level
                    '-', '-', '-', '-',          // a second LFO and the sync flags: driver state
                    pitch || volume ? 'H' : '-', // it is the chip's own LFO doing it, not a driver's
                    (i & 4) != 0 ? 'P' : '-',    // a measured portamento
            });
        }
    }

    private static void clear(TrackStatus status) {
        status.playing = false;
        status.info = TrackInfo.NORMAL;
        status.ticks = 0;
        status.ticksLeft = 0;
        status.key = 0xff;
        status.actualKey = 0xff;
        status.toneNum = 0;
        status.volume = 0;
        status.gate = 0;
        status.detune = 0;
        status.pitchDeviation = 0;
        status.status = CHIP_STATUS[0];
        status.ppz8Ch = 0;
        status.ssgTone = false;
        status.ssgNoise = false;
        status.ssgNoiseFreq = 0;

        Arrays.fill(status.fmSlotMask, false);
    }

    // ----- work state -----

    /**
     * The tick counter fmdsp wants for its clock, its loop bar and the circle animation, rebuilt
     * the way {@code OPNATimer} does it from the driver's sample counter: one TimerB step is
     * {@code opnaClock / 72 / 2} = 55467 Hz, and TimerB overflows every {@code (256 - timerB) * 16}
     * steps. The period comes from the FM chip that owns the rows; a song that never programs
     * TimerB gets {@link #defaultTimerB}.
     */
    /**
     * The first of {@code tags} the metadata has anything for, keeping what the line already shows
     * - a driver can fill its metadata in late, but never has a second thing to say on one line.
     * Only the first line of a tag is taken: there is room for one, and a note can run long.
     */
    private static String commentOf(MetaData md, String current, Tag... tags) {
        if (current != null && !current.isEmpty()) return current;
        for (Tag tag : tags) {
            String s = md.getFirst(tag);
            if (!s.isEmpty()) return s.lines().findFirst().orElse(s);
        }
        return current;
    }

    private void state() {
        BaseDriver d = driver.get();
        work = d;
        if (d == null) return;

        // a memo that is a screen image is shown as it was laid out, the tags having lost the indent
        String[] laidOut = d.getMetaData().getAll(Tag.Comments).toArray(String[]::new);
        if (laidOut.length > 0) {
            for (int i = 0; i < comments.length; i++) {
                if (comments[i] == null && i < laidOut.length) comments[i] = laidOut[i];
            }
        } else if (d.metaData != null) { // not every driver fills one in - the HES driver leaves it
            comments[0] = commentOf(d.metaData, comments[0], Tag.Title, Tag.TitleJ);
            // MDSDRV's MML credits the song to its #author rather than a #composer, and a line
            // that says who wrote it is what this one is for either way
            comments[1] = commentOf(d.metaData, comments[1],
                    Tag.Composer, Tag.ComposerJ, Tag.Artist, Tag.ArtistJ);
            // only PMD ever fills the arranger in, so without a fallback the third line is always
            // blank. A VGM's GD3 has the game instead, which beats leaving the line empty.
            comments[2] = commentOf(d.metaData, comments[2],
                    Tag.Arranger, Tag.ArrangerJ, Tag.GameTitle, Tag.GameTitleJ, Tag.Note, Tag.Maker);
        }

        int tb = 0;
        for (FmDspChipReader reader : claims.get(Group.FM)) {
            tb = reader.timerB();
            if (tb > 0) break;
        }
        // 256 is what fmgen works its register back out to when nothing has written one, and it
        // would leave the tick below no period at all to count - an endless loop on the mixer's
        // thread. Anything outside a register's range is no tempo, so it gets the fallback
        if (tb <= 0 || tb >= 256) tb = defaultTimerB;
        timerB = tb;

        long counter = d.counter;
        // a note clock hung off the sample counter alone stands still for a driver that does not
        // advance one, and every row would show an empty bar. Fall back to counting snapshots,
        // which arrive at a known rate in the player.
        noteTickStep += counter > lastCounter
                ? (counter - lastCounter) * (noteTickHz / Common.VGMProcSampleRate)
                : noteTickHz / snapshotRate;
        while (noteTickStep >= 1) {
            noteTickStep -= 1;
            noteTicks++;
        }
        timerBStep += counter > lastCounter
                ? (counter - lastCounter) * (timerBStepHz / Common.VGMProcSampleRate)
                : timerBStepHz / snapshotRate;
        lastCounter = counter;
        int overflow = (256 - (tb > 0 ? tb : 200)) << 4;
        while (timerBStep >= overflow) {
            timerBStep -= overflow;
            timerBCount++;
        }

        int loop = Math.max(d.curLoop, 0);
        if (loop != lastLoopCount) {
            if (timerBCount > loopStartTimerBCount) {
                loopTimerBCount = timerBCount - loopStartTimerBCount;
            }
            loopStartTimerBCount = timerBCount;
            lastLoopCount = loop;
        }
        timerBCountLoop = timerBCount - loopStartTimerBCount;
    }

    // ----- FmDspDataSource -----

    @Override public FftDataSource fft() { return this; }

    // ----- FftDataSource -----

    /** one reader's spectrum, read into before it is folded into the frame */
    private final int[] bars = new int[FftDataSource.LENGTH];

    /**
     * The analyzer bars: the rendered PCM, and over it whatever is being played that the rendered
     * PCM does not carry.
     * <p>
     * Everything mdplayer emulates ends up in the mixer, so the {@link FftAnalyzer} alone is the
     * spectrum for it - measured off the sound itself, which nothing worked out from registers can
     * improve on. A MIDI driver is the exception: its notes go out to a synthesizer that mixes its
     * own sound, mdplayer renders silence, and the bars stood empty for the whole song. Those
     * readers {@linkplain FmDspChipReader#spectrum hand over} a spectrum drawn from their notes
     * instead.
     * <p>
     * The two are taken per bar, the louder winning, so each stands where its own sound is: a song
     * that is entirely MIDI shows its notes over a silent mixer, one that is entirely emulated
     * never sees a note spectrum at all - a reader with nothing sounding contributes nothing, not
     * even its floor - and a song that is both shows both.
     */
    @Override
    public void readFft(int[] out) {
        fft.readFft(out);
        for (FmDspChipReader reader : readers) {
            FftDataSource notes = reader.ready() ? reader.spectrum() : null;
            if (notes == null) continue;
            notes.readFft(bars);
            for (int i = 0; i < FftDataSource.LENGTH; i++) out[i] = Math.max(out[i], bars[i]);
        }
    }

    @Override public LevelDataSource level() { return this; }

    @Override public TrackStatusSource trackStatus() { return this; }

    @Override public WorkStateSource work() { return this; }

    @Override public TrackDetailSource trackDetail() { return this; }

    // ----- TrackDetailSource -----

    /**
     * Hands the row's panel over to the reader that fills the row, and animates it.
     * <p>
     * A reader reads register caches, where the original reads the emulator's envelope generators,
     * so what it reports is the level the chip has been told to play at rather than the one it is
     * at. That is what {@link TrackDetail#modelled} says, and those bars are dropped here by how
     * far the row's envelope has fallen below its registers - the one thing this source knows that
     * the reader does not - so they attack and release with the note instead of standing still
     * under a held key, while the marker stays at the register level the way the original's does.
     * <p>
     * Called from the drawing thread, and only while the panel is on screen: the readers answer
     * out of the same caches they poll, and nothing here is computed for a display half that is
     * not being shown.
     */
    @Override
    public boolean readDetail(TrackId track, TrackDetail out) {
        int row = track.ordinal();
        FmDspChipReader reader = rowReaders[row];
        if (reader == null || !reader.readDetail(rowGroups[row], rowChannels[row], out)) {
            return false;
        }
        if (out.modelled) {
            int drop = envelopeDrop(row);
            for (int i = 0; i < out.lines; i++) {
                if (out.bar[i] > 0) out.bar[i] = Math.max(0, out.bar[i] - drop);
            }
        }
        return true;
    }

    /**
     * How far the row has fallen below the level its registers ask for, in bar columns.
     * <p>
     * A column is 1.5 dB - two steps of an FM total level, and the same scale the level meters are
     * drawn on - so the drop is the ratio between the envelope and the register level in decibels,
     * and a bar falls away at the rate the meter beside it does. A row whose reader measures its
     * own output never reads as dropped: the number it reports is the sound itself, not a ceiling
     * the sound sits under.
     */
    private int envelopeDrop(int row) {
        int level = rowLevels[row];
        double asked = rowAmplitudes[row];
        if (level < 0 || asked <= 0) return 0;
        double at = envelopes[level];
        if (at <= 0) return TrackDetail.COLUMNS;
        return Math.max(0, (int) Math.round(-20 * Math.log10(Math.min(at / asked, 1)) / 1.5));
    }

    // ----- TrackStatusSource -----

    @Override
    public void readStatus(TrackId track, TrackStatus out) {
        TrackStatus status = tracks[track.ordinal()];
        out.playing = status.playing;
        out.info = status.info;
        out.ticks = status.ticks;
        out.ticksLeft = status.ticksLeft;
        out.key = status.key;
        out.actualKey = status.actualKey;
        out.toneNum = status.toneNum;
        out.volume = status.volume;
        out.gate = status.gate;
        out.detune = status.detune;
        out.pitchDeviation = status.pitchDeviation;
        out.status = status.status;
        out.ppz8Ch = status.ppz8Ch;
        out.ssgTone = status.ssgTone;
        out.ssgNoise = status.ssgNoise;
        out.ssgNoiseFreq = status.ssgNoiseFreq;

        System.arraycopy(status.fmSlotMask, 0, out.fmSlotMask, 0, out.fmSlotMask.length);
    }

    @Override
    public boolean masked(TrackId track) {
        FmDspChipReader reader = rowReaders[track.ordinal()];
        return reader != null && reader.masked(rowGroups[track.ordinal()], rowChannels[track.ordinal()]);
    }

    @Override
    public String trackTypeName(TrackId track) {
        return rowNames[track.ordinal()];
    }

    @Override
    public int trackNumber(TrackId track) {
        int num = rowNums[track.ordinal()];
        return num > 0 ? num : -1;
    }

    @Override
    public TrackId[] displayTracks() {
        return dispTracks;
    }

    // ----- LevelDataSource -----

    @Override
    public int level(int channel) {
        return (int) (envelopes[channel] * 32767);
    }

    @Override
    public Pan pan(int channel) {
        return pans[channel];
    }

    @Override
    public String label(int channel) {
        return levelLabels[channel];
    }

    /** the row that fed this column this song; null for the drum meter and unclaimed columns */
    @Override
    public TrackId track(int channel) {
        return levelTracks[channel];
    }

    // ----- WorkStateSource -----

    @Override public int sampleRate() { return Common.VGMProcSampleRate; }

    /** time constant [s] with which the displayed clock is pulled onto the driver's own counter */
    private static final double clockSlew = 0.2;

    /**
     * The render loop blocks on the audio line and then renders a whole device period at once, so
     * the driver's sample counter advances in ~10 ms bursts of several hundred samples - reading it
     * straight makes the clock stutter. Playback is real time, so the displayed clock runs off the
     * wall clock instead and is slewed towards the driver's counter, which keeps it smooth without
     * letting it drift. It is frozen while paused and never runs backwards.
     */
    @Override
    public long generatedFrames() {
        BaseDriver d = work;
        if (d == null) return 0;

        long counter = d.counter;
        long now = System.nanoTime();
        if (lastClockNanos == 0) {
            clock = counter;
            lastClockNanos = now;
        }
        double elapsed = (now - lastClockNanos) / 1e9;
        lastClockNanos = now;

        if (!paused && playing()) {
            clock += elapsed * sampleRate();
        }
        // time based, so the result does not depend on how often the renderer asks
        clock += (counter - clock) * (1 - Math.exp(-elapsed / clockSlew));
        // a stall (a long pause, a seek) is not worth slewing through
        if (Math.abs(counter - clock) > sampleRate()) {
            clock = counter;
        }

        shownFrames = Math.max(shownFrames, (long) clock);
        return shownFrames;
    }

    private double clock;
    private long lastClockNanos;
    private long shownFrames;

    @Override public long timerBCount() { return timerBCount; }

    @Override public int timerB() { return timerB; }

    @Override public int loopCount() { BaseDriver d = work; return d != null ? Math.max(d.curLoop, 0) : 0; }

    @Override public long loopTimerBCount() { return loopTimerBCount; }

    @Override public long timerBCountLoop() { return timerBCountLoop; }

    @Override
    public long totalTimerBCount() {
        BaseDriver d = work;
        if (d == null) return 0;
        // Prefer the loop-period length (samples from loop point to end) when defined;
        // it is what the bar should represent - one trip around the loop.
        // Fall back to the total song length when there is no loop point.
        long samples = d.loopCounter > 0 ? d.loopCounter : d.totalCounter;
        if (samples <= 0) return 0;
        // Convert samples → timerB ticks (the same unit as timerBCount()) at the rate the ticks
        // have actually been counted at so far. Reading the tempo off the chip instead made the
        // slider wander: a song whose tempo falls between two TimerB values writes both by turns
        // - dq4_10.mdx flips between 239 and 240 ten times a second - and each flip moved the
        // length this is divided by by 6%, so the slider jumped back and forth a few pixels the
        // whole song. The average also stands up to a song that changes tempo for real.
        if (lastCounter > 0 && timerBCount > 0) {
            return Math.max(1, Math.round((double) samples * timerBCount / lastCounter));
        }
        // nothing counted yet (the first snapshots of a song): the tempo is all there is to go on
        int tb = this.timerB > 0 && this.timerB < 256 ? this.timerB : defaultTimerB;
        double overflow = (256 - tb) * 16.0;
        return Math.round(samples * (timerBStepHz / Common.VGMProcSampleRate) / overflow);
    }

    @Override public boolean playing() { BaseDriver d = work; return d != null && !d.stopped; }

    @Override public boolean paused() { return paused; }

    @Override
    public String driverName() {
        BaseDriver d = work;
        return d != null ? d.getName() : null;
    }

    /** the playing plugin's song, for a format that numbers them (NSF, GBS, SID, ...) */
    @Override
    public int songNo() {
        return plugin instanceof BasePlugin.HasSongNo hasSongNo ? hasSongNo.getSongNo() + 1 : 0;
    }

    /**
     * The tags a group's part may be trimmed with behind its chip's {@code MAIN} volume. They are
     * per chip rather than a convention - the OPNA spells its SSG {@code SSG} and the OPN
     * {@code PSG}, the OPNA's PCM part is {@code ADPCM} and the OPNA-B's two are {@code ADPCMA}
     * and {@code ADPCMB} - so each is tried in turn and the first the chip actually trims wins.
     */
    private static final Map<Group, String[]> volumeTags = new EnumMap<>(Map.of(
            Group.FM, new String[] {"FM"},
            Group.SSG, new String[] {"SSG", "PSG"},
            Group.RHYTHM, new String[] {"RHYTHM"},
            Group.PCM, new String[] {"ADPCM", "ADPCMA", "ADPCMB", "PCM"}));

    /**
     * What the fmdsp {@code VOLUME DOWN} counter shows: how far mdplayer's mixer is turning the
     * part down, in its own 2&times;dB unit, which for a played song is the calibrated balance of
     * the chip that claimed the part's rows - see {@code DefaultVolumeBalance_*.xml}.
     * <p>
     * A chip's {@code MAIN} volume and its part tags are independent multipliers, so the two are
     * added: what the part is turned down by is the chip's own correction plus the part's trim.
     * A part no chip claimed, or one whose reader is not a chip's, has no correction to show.
     */
    @Override
    public int volumeDown(VolumePart part) {
        Group g = switch (part) {
            case FM -> Group.FM;
            case SSG -> Group.SSG;
            case RHY -> Group.RHYTHM;
            case PCM -> Group.PCM;
        };
        List<FmDspChipReader> claimed = claims.get(g);
        if (claimed == null || claimed.isEmpty()) return 0;
        Class<? extends mdplayer.Chip> c = claimed.getFirst().chipClass();
        if (c == null) return 0;
        Setting.Balance balance = Setting.getInstance().getBalance();
        int v = balance.getVolume(MDSound.Chip.MAIN_TAG, c);
        for (String tag : volumeTags.get(g)) {
            int trim = balance.getVolume(tag, c);
            if (trim != 0) {
                v += trim;
                break;
            }
        }
        return v;
    }

    @Override
    public String chips() {
        if (plugin != null && !plugin.getChips().isEmpty()) {
            return plugin.getChips().stream()
                    .map(c -> plugin.chipRegister.chip(c).getName().toUpperCase())
                    .distinct()
                    .collect(Collectors.joining(" "));
        }
        BaseDriver d = work != null ? work : driver.get();
        if (d != null && d.metaData != null) {
            String chips = d.metaData.getFirst(Tag.Chip);
            if (chips != null && !chips.isEmpty()) return chips.replace(",", " ");
        }
        return null;
    }

    @Override public String filename() { return filename; }

    @Override public String comment(int line) { return comments[line]; }

    @Override
    public String pcmType(int index) {
        BaseDriver d = work != null ? work : driver.get();
        return d != null ? d.pcmType(index) : (index == 0 ? "PCM1" : (index == 1 ? "PCM2" : null));
    }

    @Override
    public String pcmFilename(int index) {
        BaseDriver d = work != null ? work : driver.get();
        return d != null ? d.pcmFilename(index) : null;
    }

    @Override
    public boolean pcmError(int index) {
        BaseDriver d = work != null ? work : driver.get();
        return d != null ? d.pcmError(index) : false;
    }
}
