/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.visualizer.boids;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.Timer;

import vavi.sound.visualizer.boids.BoidsParams.Param;
import vavi.sound.visualizer.fmdsp.FmDspDataSource;
import vavi.sound.visualizer.fmdsp.LevelDataSource;
import vavi.sound.visualizer.fmdsp.TrackId;
import vavi.sound.visualizer.fmdsp.TrackStatus;
import vavi.sound.visualizer.fmdsp.TrackStatusSource;


/**
 * A flock visualizer: every channel of the song is a small flock of boids loose in the window.
 * <p>
 * The rules are Reynolds' three - separation, alignment, cohesion - within a channel, and two more
 * between channels, which is what makes the picture a picture of a piece of music rather than of a
 * screensaver:
 * <ul>
 * <li>each flock is drawn towards the nearest <em>other</em> channel's flock, with a sideways share
 * ({@link Param#CROSS_SWIRL}) so that the two sweep through each other and out again instead of
 * merging into one lump - the parts cross over the way they do on a score;</li>
 * <li>every note the channel plays swells its flock: the boids are thrown outwards, grow, speed up
 * and spread, then gather again as the swell decays.</li>
 * </ul>
 * Colour is the key: the semitone picks the hue and the octave lifts the brightness, so a part
 * moving up a scale walks round the colour wheel and two parts in unison are the same colour.
 * Boids bounce off the window frame, or turn back before it if given a {@link Param#WALL_MARGIN}.
 *
 * <h2>Data</h2>
 * Everything comes through the same {@link FmDspDataSource} the FMDSP visualizer uses, so any
 * source written for that one - {@code mdplayer.ChipFmDspSource} for a chip song, {@code
 * MidiFmDspSource} for a MIDI one - drives this with no adapter. {@link TrackStatusSource} says
 * what key each row is playing and {@link LevelDataSource} how loud, and nothing here reaches into
 * a chip or a driver.
 *
 * <h2>Taste</h2>
 * None of the numbers are baked in: they all live in {@link BoidsParams}, are read every frame, and
 * {@link BoidsControlPanel} will build sliders for the lot of them. Presets are a starting point,
 * not a menu - the point is to drag until it looks right and then save it.
 *
 * <h2>Threading</h2>
 * The flock is stepped and drawn on the EDT, from a Swing timer; the data source is only read
 * there. Guard cross-thread mutation of the source on your side, as the FMDSP one asks.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-08-03 nsano initial version <br>
 */
public class BoidsVisualizer extends JComponent {

    /** the rows shown when the source names none of its own */
    private static final TrackId[] DEFAULT_TRACKS = {
            TrackId.FM_1, TrackId.FM_2, TrackId.FM_3, TrackId.FM_4, TrackId.FM_5, TrackId.FM_6,
            TrackId.SSG_1, TrackId.SSG_2, TrackId.SSG_3, TrackId.ADPCM,
    };

    /** dB the level meter spans, as the FMDSP strip has it: 32 steps of 1.5 dB */
    private static final double LEVEL_RANGE_DB = 48;

    /** seconds the loudness follower takes to catch up, against which a beat is a rise */
    private static final double LEVEL_FOLLOW = 0.14;

    /** the swell a note gets when nothing says how loud it is */
    private static final double MIN_BEAT_STRENGTH = 0.35;

    /** a key of this is silence; the low nibble of 0xf is a key off */
    private static final int KEY_SILENT = 0xff;

    private final BoidsParams params = new BoidsParams();

    private FmDspDataSource source;

    private final Timer timer;

    private final Map<TrackId, Flock> flocks = new EnumMap<>(TrackId.class);

    /** the flocks of {@code scratch.displayTracks()}, in row order, rebuilt only when the row set changes */
    private final List<Flock> shown = new ArrayList<>();

    private final TrackStatus scratch = new TrackStatus();

    private final Random random = new Random();

    /** what has been drawn so far, so that the trails can be left to fade on it */
    private BufferedImage canvas;

    /** {@link #canvas}'s own pixels, which the trail fade works on directly */
    private int[] pixels = new int[0];

    private long lastNanos;

    /** how long the frame being drawn took, in seconds */
    private double lastDt = 1 / 60d;

    /** seconds since the first frame, which the anchors travel by */
    private double time;

    /** the step every frame takes when it is not the clock that decides; 0 means the clock does */
    private double fixedStep;

    private static final Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

    /**
     * @param fps target frame rate, e.g. {@code 60}
     */
    public BoidsVisualizer(int fps) {
        this(fps, 640, 400);
    }

    /**
     * @param fps target frame rate, e.g. {@code 60}
     * @param width preferred width in px
     * @param height preferred height in px
     */
    public BoidsVisualizer(int fps, int width, int height) {
        if (fps <= 0) fps = 60;
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.BLACK);
        setOpaque(true);
        timer = new Timer(1000 / fps, e -> repaint());
        timer.setRepeats(true);
    }

    public BoidsVisualizer() {
        this(60);
    }

    /** Set the data source. May be replaced at any time. */
    public void setDataSource(FmDspDataSource source) {
        this.source = source;
    }

    /** The live parameter set: change anything on it and the next frame follows. */
    public BoidsParams getParams() {
        return params;
    }

    /**
     * Makes every frame advance the flock by the same amount of time instead of by however long
     * the last one really took. That is what a frame by frame render wants - a test painting into
     * an image, or a recording - since there the clock has nothing to do with the picture.
     *
     * @param seconds the step, or 0 to go back to measuring real time
     */
    public void setFixedTimeStep(double seconds) {
        this.fixedStep = Math.max(seconds, 0);
    }

    /** Start the repaint timer. */
    public void start() {
        lastNanos = 0;
        timer.start();
    }

    /** Stop the repaint timer. */
    public void stop() {
        timer.stop();
    }

    /** Forgets every flock and clears the screen, for a new song. */
    public void reset() {
        flocks.clear();
        shown.clear();
        time = 0;
        lastNanos = 0;
        if (canvas != null) {
            Graphics2D g = canvas.createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g.dispose();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = Math.max(getWidth(), 1);
        int h = Math.max(getHeight(), 1);
        prepareCanvas(w, h);

        long now = System.nanoTime();
        // the first frame after a start has no previous one to measure against, and a frame that
        // took a whole second - a song loading on the EDT - would fling the flock off the screen
        double dt = fixedStep > 0 ? fixedStep
                : lastNanos == 0 ? 1 / 60d : Math.clamp((now - lastNanos) / 1e9, 0, 0.1);
        lastNanos = now;
        lastDt = dt;
        time += dt;

        poll(dt, w, h);
        step(dt, w, h);
        render(w, h);

        g.drawImage(canvas, 0, 0, null);
    }

    private void prepareCanvas(int w, int h) {
        if (canvas != null && canvas.getWidth() == w && canvas.getHeight() == h) return;

        int oldW = canvas != null ? canvas.getWidth() : w;
        int oldH = canvas != null ? canvas.getHeight() : h;
        canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
        // the flock lives in window coordinates, so a resize has to take it along or every boid
        // outside the new frame comes back in a straight line from the corner it was left in
        double sx = (double) w / oldW;
        double sy = (double) h / oldH;
        for (Flock flock : flocks.values()) flock.rescale(sx, sy);
    }

    // ==================================================================
    // data
    // ==================================================================

    /** Reads the source once and turns what it says into each flock's key, level and beat. */
    private void poll(double dt, int w, int h) {
        TrackStatusSource ts = source != null ? source.trackStatus() : null;
        LevelDataSource ls = source != null ? source.level() : null;

        TrackId[] tracks = ts != null ? ts.displayTracks() : null;
        if (tracks == null || tracks.length == 0) tracks = DEFAULT_TRACKS;

        boolean changed = shown.size() != tracks.length;
        for (int i = 0; !changed && i < tracks.length; i++) changed = shown.get(i).track != tracks[i];
        if (changed) {
            shown.clear();
            for (int i = 0; i < tracks.length; i++) {
                TrackId track = tracks[i];
                Flock flock = flocks.computeIfAbsent(track, t -> new Flock(t, flocks.size()));
                flock.index = i;
                shown.add(flock);
            }
        }

        for (Flock flock : shown) {
            if (ts != null) {
                ts.readStatus(flock.track, scratch);
            } else {
                scratch.playing = false;
                scratch.key = KEY_SILENT;
                scratch.volume = 0;
            }
            boolean masked = ts != null && ts.masked(flock.track);
            flock.name = name(ts, flock.track);
            flock.update(scratch, level(ls, flock.track, scratch), masked, dt, w, h, params, time);
        }
    }

    /**
     * How loud the row is, 0..1.
     * <p>
     * The level strip is the one measure every chip's reader fills in the same units, so it is what
     * a beat is judged by; it is read the way the FMDSP meter reads it, as dB over a 48 dB range,
     * which is what makes a swell look like a swell rather than like a sample peak. A source with
     * no level strip - a bare {@link TrackStatusSource} in a test - falls back to the track's own
     * volume, whose scale is the chip's business but is at least monotonic.
     */
    private static double level(LevelDataSource ls, TrackId track, TrackStatus status) {
        if (ls != null) {
            for (int c = 0; c < LevelDataSource.COUNT; c++) {
                if (ls.track(c) != track) continue;
                double v = ls.level(c) / 32768d;
                if (v <= 0) return 0;
                double db = 20 * Math.log10(v);
                return Math.clamp((db + LEVEL_RANGE_DB) / LEVEL_RANGE_DB, 0, 1);
            }
        }
        if (!status.playing || status.key == KEY_SILENT) return 0;
        return Math.clamp(status.volume / 127d, 0, 1);
    }

    private static String name(TrackStatusSource ts, TrackId track) {
        if (ts == null) return track.name();
        String type = ts.trackTypeName(track);
        int num = ts.trackNumber(track);
        if (type == null) return track.name().replace('_', ' ');
        return num > 0 ? type + num : type;
    }

    // ==================================================================
    // flocking
    // ==================================================================

    private void step(double dt, int w, int h) {
        for (Flock flock : shown) flock.centroid();
        for (Flock flock : shown) flock.steer(shown, dt, w, h, params, random);
    }

    /** One channel's boids, and what the channel is doing to them. */
    private static final class Flock {

        final TrackId track;

        /** where the row sits among the shown ones; its anchor's orbit is derived from this */
        int index;

        String name;

        Boid[] boids = new Boid[0];

        /** MML key, high nibble octave and low nibble note; {@link #KEY_SILENT} while resting */
        int key = KEY_SILENT;

        /** the last key that was really a note, which the colour holds on to through the rests */
        int litKey = KEY_SILENT;

        /** loudness this frame, 0..1 */
        double loud;

        /** the follower a rise is measured against */
        double loudFollow;

        /** the swell, 0..1, as drawn */
        double pulse;

        /** what the swell is heading for, which decays on its own */
        double pulseTarget;

        /** true on the frame a note arrived */
        boolean beat;

        boolean masked;

        /** how present the channel is, 0..1, smoothed so that a rest does not blink */
        double presence;

        /** where the flock's anchor is this frame */
        double homeX, homeY;

        /** the middle of the flock, as of the last {@link #centroid} */
        double cx, cy;

        Flock(TrackId track, int seed) {
            this.track = track;
            this.index = seed;
            this.name = track.name();
        }

        void rescale(double sx, double sy) {
            for (Boid b : boids) b.rescale(sx, sy);
            cx *= sx;
            cy *= sy;
        }

        /** Takes this frame's status: the key, the loudness, and whether a note just landed. */
        void update(TrackStatus status, double loud, boolean masked, double dt,
                    int w, int h, BoidsParams p, double time) {
            this.masked = masked;
            int newKey = status.playing && !masked ? status.key : KEY_SILENT;
            boolean sounding = newKey != KEY_SILENT && (newKey & 0xf) != 0xf;

            this.loud = masked ? 0 : loud;
            // a key that arrives on a part playing quietly under everything else never shows up as
            // a rise in level, so the note itself counts as a beat as well
            boolean keyBeat = p.getFlag(Param.KEY_BEAT) && sounding && newKey != key;
            boolean levelBeat = this.loud - loudFollow > p.get(Param.BEAT_SENSE);
            beat = keyBeat || levelBeat;
            key = newKey;
            if (sounding) litKey = newKey;

            double follow = 1 - Math.exp(-dt / LEVEL_FOLLOW);
            loudFollow += (this.loud - loudFollow) * follow;

            if (beat) {
                pulseTarget = Math.max(pulseTarget, Math.max(this.loud, MIN_BEAT_STRENGTH));
                // a beat landing while the previous swell is still falling should not have to
                // climb from where that one got to
                loudFollow = Math.max(loudFollow, this.loud);
            }
            pulseTarget *= Math.exp(-dt / Math.max(p.get(Param.PULSE_DECAY), 1e-3));
            pulse += (pulseTarget - pulse) * (1 - Math.exp(-dt / Math.max(p.get(Param.PULSE_ATTACK), 1e-3)));

            double target = sounding ? 1 : Math.max(this.loud, pulse);
            presence += (target - presence) * (1 - Math.exp(-dt / 0.35));

            // the anchor each flock travels on: two sine waves whose frequencies differ from every
            // other flock's, so the paths keep meeting rather than settling into one shared round
            double spread = p.get(Param.ORBIT_SPREAD);
            double a = time * p.get(Param.ORBIT_SPEED) * 2 * Math.PI;
            double phase = index * 1.7;
            homeX = w / 2d + w * spread / 2 * Math.sin(a * (0.7 + 0.13 * index) + phase);
            homeY = h / 2d + h * spread / 2 * Math.sin(a * (0.9 + 0.17 * index) + phase * 1.3);
            // pitch lifts the flock up the window, so a bass part lies below a lead one
            double lift = p.get(Param.KEY_LIFT);
            if (lift > 0 && litKey != KEY_SILENT) {
                double pitch = Math.clamp(((litKey >> 4) * 12 + (litKey & 0xf)) / 96d, 0, 1);
                homeY += (0.5 - pitch) * h * lift;
            }
            homeX = Math.clamp(homeX, 0, w);
            homeY = Math.clamp(homeY, 0, h);
        }

        /** Makes sure there are as many boids as asked for, seeding new ones around the anchor. */
        private void populate(int want, Random random, int w, int h) {
            if (boids.length == want) return;
            Boid[] next = new Boid[want];
            for (int i = 0; i < want; i++) {
                next[i] = i < boids.length ? boids[i] : new Boid(
                        Math.clamp(homeX + random.nextGaussian() * 30, 0, w),
                        Math.clamp(homeY + random.nextGaussian() * 30, 0, h),
                        random.nextDouble() * 2 * Math.PI);
            }
            boids = next;
        }

        /** The middle of the flock: what its own cohesion and the other flocks both steer by. */
        void centroid() {
            if (boids.length == 0) return;
            double sx = 0, sy = 0;
            for (Boid b : boids) {
                sx += b.x;
                sy += b.y;
            }
            cx = sx / boids.length;
            cy = sy / boids.length;
        }

        void steer(List<Flock> all, double dt, int w, int h, BoidsParams p, Random random) {
            populate(p.getInt(Param.COUNT), random, w, h);
            if (boids.length == 0) return;
            if (cx == 0 && cy == 0) centroid();

            double maxSpeed = p.get(Param.MAX_SPEED) * (1 + p.get(Param.PULSE_SPEED) * pulse);
            double maxForce = p.get(Param.MAX_FORCE);
            double sepR = p.get(Param.SEPARATION_RADIUS) * (1 + p.get(Param.PULSE_SPREAD) * pulse);
            double nbrR = p.get(Param.NEIGHBOR_RADIUS);

            // the flock to cross with: the nearest other one, so two parts that have drifted
            // together carry on through each other rather than everyone converging on the middle
            Flock other = nearest(all);

            if (beat) kick(p.get(Param.PULSE_KICK) * Math.max(loud, MIN_BEAT_STRENGTH), nbrR, random);

            for (Boid b : boids) {
                double ax = 0, ay = 0;

                // ---- Reynolds, within the channel ----
                double sepX = 0, sepY = 0, aliX = 0, aliY = 0;
                int near = 0, close = 0;
                for (Boid o : boids) {
                    if (o == b) continue;
                    double dx = b.x - o.x, dy = b.y - o.y;
                    double d2 = dx * dx + dy * dy;
                    if (d2 < 1e-6) {
                        dx = random.nextDouble() - 0.5;
                        dy = random.nextDouble() - 0.5;
                        d2 = dx * dx + dy * dy + 1e-6;
                    }
                    if (d2 < sepR * sepR) {
                        double d = Math.sqrt(d2);
                        sepX += dx / d / d;
                        sepY += dy / d / d;
                        close++;
                    }
                    if (d2 < nbrR * nbrR) {
                        aliX += o.vx;
                        aliY += o.vy;
                        near++;
                    }
                }
                if (close > 0) {
                    steerTo(sepX, sepY, b, maxSpeed);
                    ax += steerX * p.get(Param.SEPARATION);
                    ay += steerY * p.get(Param.SEPARATION);
                }
                if (near > 0) {
                    steerTo(aliX / near, aliY / near, b, maxSpeed);
                    ax += steerX * p.get(Param.ALIGNMENT);
                    ay += steerY * p.get(Param.ALIGNMENT);
                }

                // ---- cohesion, to the whole flock rather than to whoever is in sight ----
                // A flock here is a channel, and a channel is one thing. Cohesion counted over
                // neighbours only lets a flock that has come apart carry on as two, each half
                // perfectly happy with its own company, and the channel is then drawn in two
                // places at once for the rest of the song
                steerTo(cx - b.x, cy - b.y, b, maxSpeed);
                ax += steerX * p.get(Param.COHESION);
                ay += steerY * p.get(Param.COHESION);

                // ---- the anchor ----
                steerTo(homeX - b.x, homeY - b.y, b, maxSpeed);
                ax += steerX * p.get(Param.ORBIT_PULL);
                ay += steerY * p.get(Param.ORBIT_PULL);

                // ---- crossing over ----
                if (other != null) {
                    double dx = other.cx - b.x, dy = other.cy - b.y;
                    double swirl = p.get(Param.CROSS_SWIRL);
                    // straight at it plus a turn across it: the sum is a spiral through the other
                    // flock, which is what a crossing looks like from the outside
                    steerTo(dx - dy * swirl, dy + dx * swirl, b, maxSpeed);
                    ax += steerX * p.get(Param.CROSSOVER);
                    ay += steerY * p.get(Param.CROSSOVER);
                }

                // ---- wander ----
                double wander = p.get(Param.WANDER);
                if (wander > 0) {
                    b.wander += (random.nextDouble() - 0.5) * 6 * dt;
                    ax += Math.cos(b.wander) * maxSpeed * wander;
                    ay += Math.sin(b.wander) * maxSpeed * wander;
                }

                // ---- the frame, before it is reached ----
                double margin = p.get(Param.WALL_MARGIN);
                if (margin > 0) {
                    double steer = p.get(Param.WALL_STEER) * maxSpeed;
                    if (b.x < margin) ax += steer * (1 - b.x / margin);
                    if (b.x > w - margin) ax -= steer * (1 - (w - b.x) / margin);
                    if (b.y < margin) ay += steer * (1 - b.y / margin);
                    if (b.y > h - margin) ay -= steer * (1 - (h - b.y) / margin);
                }

                double mag = Math.hypot(ax, ay);
                if (mag > maxForce) {
                    ax = ax / mag * maxForce;
                    ay = ay / mag * maxForce;
                }
                b.move(ax, ay, dt, maxSpeed, p.get(Param.MIN_SPEED), w, h, p.get(Param.BOUNCE),
                        p.get(Param.BOID_SIZE));
            }
            centroid();
        }

        /**
         * Throws the boids outwards from the middle: the flock catching the beat.
         * <p>
         * The throw fades out with distance, and beyond twice the neighbour radius it is not thrown
         * at all. Without that the swell is its own undoing: a boid that has strayed is pushed
         * further out by every note that follows, and since the middle it is pushed away from lies
         * between the strays, a flock that once came apart is driven apart again on every beat and
         * ends up as two clumps in opposite corners for the rest of the song.
         */
        private void kick(double speed, double reach, Random random) {
            for (Boid b : boids) {
                double dx = b.x - cx, dy = b.y - cy;
                double d = Math.hypot(dx, dy);
                if (d < 1e-3) {
                    double a = random.nextDouble() * 2 * Math.PI;
                    dx = Math.cos(a);
                    dy = Math.sin(a);
                    d = 1;
                }
                double falloff = Math.clamp(1 - d / (2 * reach), 0, 1);
                if (falloff <= 0) continue;
                b.vx += dx / d * speed * falloff;
                b.vy += dy / d * speed * falloff;
            }
        }

        private Flock nearest(List<Flock> all) {
            Flock best = null;
            double bestD = Double.MAX_VALUE;
            for (Flock f : all) {
                if (f == this || f.boids.length == 0) continue;
                double d = Math.hypot(f.cx - cx, f.cy - cy);
                if (d < bestD) {
                    bestD = d;
                    best = f;
                }
            }
            return best;
        }

        /** the last steering vector {@link #steerTo} worked out; a field, as it is wanted 60 times a second */
        private double steerX, steerY;

        /** The classic steering vector: where it wants to go at full speed, less where it is going. */
        private void steerTo(double dx, double dy, Boid b, double maxSpeed) {
            double d = Math.hypot(dx, dy);
            if (d < 1e-6) {
                steerX = 0;
                steerY = 0;
                return;
            }
            steerX = dx / d * maxSpeed - b.vx;
            steerY = dy / d * maxSpeed - b.vy;
        }
    }

    /** One boid. */
    private static final class Boid {

        double x, y, vx, vy;
        /** where it was drawn last frame, which the comet shape draws back to */
        double px, py;
        /** the heading its random wander is currently pushing towards */
        double wander;

        Boid(double x, double y, double wander) {
            this.x = this.px = x;
            this.y = this.py = y;
            this.wander = wander;
            this.vx = Math.cos(wander) * 40;
            this.vy = Math.sin(wander) * 40;
        }

        void rescale(double sx, double sy) {
            x *= sx;
            px *= sx;
            y *= sy;
            py *= sy;
        }

        void move(double ax, double ay, double dt, double maxSpeed, double minSpeed,
                  int w, int h, double bounce, double radius) {
            vx += ax * dt;
            vy += ay * dt;
            double speed = Math.hypot(vx, vy);
            if (speed > maxSpeed && speed > 0) {
                vx = vx / speed * maxSpeed;
                vy = vy / speed * maxSpeed;
            } else if (speed < minSpeed) {
                if (speed < 1e-6) {
                    vx = minSpeed;
                    vy = 0;
                } else {
                    vx = vx / speed * minSpeed;
                    vy = vy / speed * minSpeed;
                }
            }
            px = x;
            py = y;
            x += vx * dt;
            y += vy * dt;

            // the frame itself, which is met however hard the boid arrives at it
            double r = Math.min(radius, Math.min(w, h) / 4d);
            if (x < r) {
                x = r;
                vx = Math.abs(vx) * bounce;
            } else if (x > w - r) {
                x = w - r;
                vx = -Math.abs(vx) * bounce;
            }
            if (y < r) {
                y = r;
                vy = Math.abs(vy) * bounce;
            } else if (y > h - r) {
                y = h - r;
                vy = -Math.abs(vy) * bounce;
            }
        }
    }

    // ==================================================================
    // drawing
    // ==================================================================

    private final Path2D.Double dart = new Path2D.Double();

    private void render(int w, int h) {
        // the previous frame is left to fade rather than cleared, which is where the trails come
        // from. Measured per 1/60 s of real time, so that a trail is as long at 30 fps as it is at
        // 60 and does not stretch out when a frame is late
        fade(Math.pow(params.get(Param.TRAIL), 60 * lastDt));

        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            for (Flock flock : shown) drawFlock(g, flock);
        } finally {
            g.dispose();
        }
    }

    /**
     * Dims what is already on the canvas by {@code keep}.
     * <p>
     * Done on the raster rather than by painting black over it: blending a translucent black rounds
     * back to the value it started from once a pixel is dark enough, and what is left behind is a
     * grey haze of everywhere the flock has ever been. Here every lit pixel loses at least one
     * step, so a trail really does end.
     */
    private void fade(double keep) {
        int k = (int) Math.round(Math.clamp(keep, 0, 1) * 256);
        if (k >= 256) return;
        if (k <= 0) {
            Arrays.fill(pixels, 0);
            return;
        }
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            if (p == 0) continue;
            pixels[i] = decay((p >> 16) & 0xff, k) << 16
                    | decay((p >> 8) & 0xff, k) << 8
                    | decay(p & 0xff, k);
        }
    }

    private static int decay(int v, int k) {
        if (v == 0) return 0;
        int n = (v * k) >> 8;
        return n < v ? n : v - 1;
    }

    private void drawFlock(Graphics2D g, Flock flock) {
        if (flock.boids.length == 0) return;

        double idle = params.get(Param.IDLE_FADE);
        double presence = 1 - idle * (1 - flock.presence);
        double alpha = Math.clamp(presence * (1 - params.get(Param.LEVEL_ALPHA)
                + params.get(Param.LEVEL_ALPHA) * Math.max(flock.loud, flock.pulse)), 0, 1);
        if (alpha < 0.01) return;

        Color color = colorOf(flock);
        double size = params.get(Param.BOID_SIZE) * (1 + params.get(Param.PULSE_SIZE) * flock.pulse);
        int shape = params.getInt(Param.SHAPE);
        double glow = params.get(Param.GLOW);

        // ---- the threads between flockmates ----
        double linkAlpha = params.get(Param.LINK_ALPHA) * alpha;
        double linkR = params.get(Param.LINK_RADIUS);
        if (linkAlpha > 0.01 && linkR > 0) {
            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < flock.boids.length; i++) {
                for (int j = i + 1; j < flock.boids.length; j++) {
                    Boid a = flock.boids[i], b = flock.boids[j];
                    double d = Math.hypot(a.x - b.x, a.y - b.y);
                    if (d > linkR) continue;
                    g.setColor(alpha(color, linkAlpha * (1 - d / linkR)));
                    g.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);
                }
            }
        }

        // ---- the boids ----
        for (Boid b : flock.boids) {
            if (glow > 0.01) {
                g.setColor(alpha(color, alpha * 0.16 * glow));
                fill(g, b, size * 2.0, shape);
                g.setColor(alpha(color, alpha * 0.3 * glow));
                fill(g, b, size * 1.4, shape);
            }
            g.setColor(alpha(color, alpha));
            fill(g, b, size, shape);
        }

        // ---- who it is ----
        if (params.getFlag(Param.LABELS)) {
            g.setFont(labelFont);
            g.setColor(alpha(color, Math.min(alpha, 0.55)));
            g.drawString(flock.name, (float) (flock.cx + 6), (float) (flock.cy - 6));
        }
    }

    /** Draws one boid at the given radius, in whichever shape is asked for. */
    private void fill(Graphics2D g, Boid b, double size, int shape) {
        switch (shape) {
            case 1 -> { // a dart pointing where it flies
                double a = Math.atan2(b.vy, b.vx);
                double nose = size * 1.9, tail = size * 0.9;
                dart.reset();
                dart.moveTo(b.x + Math.cos(a) * nose, b.y + Math.sin(a) * nose);
                dart.lineTo(b.x + Math.cos(a + 2.5) * tail, b.y + Math.sin(a + 2.5) * tail);
                dart.lineTo(b.x + Math.cos(a - 2.5) * tail, b.y + Math.sin(a - 2.5) * tail);
                dart.closePath();
                g.fill(dart);
            }
            case 2 -> { // a comet, drawn back over the ground it covered this frame
                g.setStroke(new BasicStroke((float) Math.max(size, 1), BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g.drawLine((int) b.px, (int) b.py, (int) b.x, (int) b.y);
            }
            default -> {
                int d = (int) Math.max(size * 2, 1);
                g.fillOval((int) (b.x - size), (int) (b.y - size), d, d);
            }
        }
    }

    /**
     * The flock's colour: the semitone walks the hue round the wheel and the octave brightens it,
     * so unison parts match and a rising line sweeps the spectrum. A resting channel keeps the
     * colour of the last note it played rather than falling to grey.
     */
    private Color colorOf(Flock flock) {
        int key = flock.litKey;
        int note = key == KEY_SILENT ? 0 : key & 0xf;
        int octave = key == KEY_SILENT ? 4 : key >> 4;
        if (note > 11) note = 11; // a key off keeps the top of the scale rather than wrapping
        float hue = (float) (params.get(Param.HUE_OFFSET) + note / 12d * params.get(Param.HUE_SPAN));
        hue = (float) (hue - Math.floor(hue));
        float sat = (float) params.get(Param.SATURATION);
        float bright = (float) Math.clamp(0.62 + params.get(Param.OCTAVE_BRIGHT) * (octave - 4) / 5d, 0.15, 1);
        return Color.getHSBColor(hue, sat, bright);
    }

    private static Color alpha(Color c, double a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
                Math.clamp(Math.round(a * 255), 0, 255));
    }
}
