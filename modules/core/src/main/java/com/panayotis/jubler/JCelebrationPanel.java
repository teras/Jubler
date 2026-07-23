/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A one-off celebration backdrop shown in the empty central area of the very first
 * Jubler window at application startup (version 10 / 21st anniversary). Fireworks
 * rise and converge to form a rotating message, hold the shape, then fall apart as
 * sparks; confetti drifts down throughout. Click anywhere to launch an extra one.
 * <p>
 * Resolution independent: everything lives in a logical space whose short side is
 * BASE units; a single uniform factor maps logical->pixels, and the scene is
 * rendered at a fixed resolution (depending only on the aspect ratio, not the
 * window's pixel size) then blitted once, scaled, to the panel.
 * <p>
 * Call {@link #start()} after adding it and {@link #stop()} before removing it.
 */
public final class JCelebrationPanel extends JPanel implements ActionListener {

    private static final double BASE = 1000.0; // logical units along the short side
    private static final double SCENE_SHORT = 600.0; // offscreen pixels along the short side
    private static final double SCENE_SCALE = SCENE_SHORT / BASE; // logical -> scene pixel factor

    private final Random rnd = new Random();
    private final Timer timer = new Timer(16, this); // ~60fps
    private final List<Rocket> rockets = new ArrayList<Rocket>();
    private final List<Particle> particles = new ArrayList<Particle>();
    private final List<Confetti> confetti = new ArrayList<Confetti>();

    // Pre-rendered glow sprites keyed by quantized color and size; life-based alpha
    // is applied at draw time through the composite so the cache stays small.
    private final Map<Integer, BufferedImage> glowCache = new HashMap<Integer, BufferedImage>();
    // SRC_OVER composites cached per alpha bucket (alpha>>3) to avoid per-particle allocation.
    private final AlphaComposite[] alphaCache = new AlphaComposite[33];

    private BufferedImage scene, bg;    // persistent scene buffer + cached background gradient
    private boolean seeded = false;
    private int tick = 0;
    private int stepsSincePaint = 0;    // physics steps run since the last paint (drives the fade)
    private double stepAcc = 0;         // fractional-step accumulator used while unfocused

    private final Font baseFont = new Font("SansSerif", Font.BOLD, 24);

    private final String[] msgs = {
            "Version 10", "21 Years",
            "All grown up", "21 years of perfect timing",
            "Still perfectly in sync", "Coming of age, frame by frame",
            "Always on cue", "Caption this moment",
            "Roll the credits", "Every second counts",
            "Reading between the lines", "Fluent in every language",
            "Down to the millisecond", "Press play on 21"};
    private final Color[] msgColors = {
            new Color(70, 200, 255), new Color(255, 195, 50),
            new Color(255, 90, 200), new Color(110, 255, 150),
            new Color(170, 120, 255), new Color(255, 110, 90),
            new Color(70, 230, 200), new Color(255, 180, 120),
            new Color(255, 100, 140), new Color(120, 255, 230),
            new Color(255, 130, 40), new Color(140, 210, 255),
            new Color(200, 255, 90), new Color(255, 120, 220)};
    private int formStep = 0;                       // how many messages shown so far
    private final List<Integer> bag = new ArrayList<Integer>(); // shuffled pool drawn after the first two
    private static final int FORM_INTERVAL = 230;

    private final Color[] palette = {
            new Color(255, 80, 80), new Color(255, 195, 50), new Color(120, 255, 120),
            new Color(70, 200, 255), new Color(190, 120, 255), new Color(255, 90, 200),
            new Color(255, 255, 255)};

    public JCelebrationPanel() {
        setOpaque(true);
        setBackground(Color.BLACK);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                double s = s();
                launchRocket(e.getX() / s, Math.max(BASE * 0.06, e.getY() / s), -1);
            }
        });
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    // ----- logical <-> pixel helpers -----

    private double s() {
        int m = Math.min(getWidth(), getHeight());
        return m <= 0 ? 1.0 : m / BASE;
    }

    private double wLog() { return getWidth() / s(); }

    private double hLog() { return getHeight() / s(); }

    // ----- update -----

    public void actionPerformed(ActionEvent e) {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        if (!sceneVisible()) return; // pause while hidden or minimized
        if (!seeded) {
            for (int i = 0; i < 140; i++) confetti.add(newConfetti(true));
            seeded = true;
        }
        // Full 60fps while focused; drop to 40fps otherwise but keep the 60Hz physics cadence
        // via a fractional accumulator (1.5 steps/tick -> alternating 1 and 2 steps).
        int steps;
        if (windowFocused()) {
            timer.setDelay(16);
            stepAcc = 0;
            steps = 1;
        } else {
            timer.setDelay(25);
            stepAcc += 1.5;
            steps = (int) stepAcc;
            stepAcc -= steps;
        }
        for (int i = 0; i < steps; i++) step();
        stepsSincePaint += steps;
        repaint();
    }

    private boolean windowFocused() {
        Window w = SwingUtilities.getWindowAncestor(this);
        return w != null && w.isFocused();
    }

    /** True while the panel is on screen and its window is not iconified. */
    private boolean sceneVisible() {
        if (!isShowing()) return false;
        Window w = SwingUtilities.getWindowAncestor(this);
        return !(w instanceof Frame) || (((Frame) w).getExtendedState() & Frame.ICONIFIED) == 0;
    }

    private void step() {
        tick++;
        if (tick % FORM_INTERVAL == 1)
            launchRocket(wLog() / 2, hLog() * 0.45, nextMsg());
        if (rnd.nextDouble() < 0.03)
            launchRocket(rnd.nextDouble() * wLog(), hLog() * (0.2 + rnd.nextDouble() * 0.4), -2);
        updateRockets();
        updateParticles();
        updateConfetti();
    }

    /**
     * First "Version 10" then "21 Years"; afterwards a shuffled bag that holds those two
     * twice each plus every pun once, so version and age keep showing (and more often).
     */
    private int nextMsg() {
        if (formStep < 2)
            return formStep++;
        if (bag.isEmpty()) {
            bag.add(0); bag.add(0); // "Version 10" twice
            bag.add(1); bag.add(1); // "21 Years" twice
            for (int i = 2; i < msgs.length; i++)
                bag.add(i);         // each pun once
            Collections.shuffle(bag, rnd);
        }
        return bag.remove(bag.size() - 1);
    }

    private void launchRocket(double x, double targetY, int textMsg) {
        Rocket r = new Rocket();
        r.x = x;
        r.y = hLog();
        r.targetY = targetY;
        r.vx = (rnd.nextDouble() - 0.5) * 1.2;
        r.vy = -(15 + rnd.nextDouble() * 4);
        r.color = textMsg >= 0 ? msgColors[textMsg] : palette[rnd.nextInt(palette.length)];
        r.textMsg = textMsg;
        rockets.add(r);
    }

    private void updateRockets() {
        for (int i = rockets.size() - 1; i >= 0; i--) {
            Rocket r = rockets.get(i);
            r.x += r.vx;
            r.y += r.vy;
            r.vy += 0.25;
            if (r.vy >= 0 || r.y <= r.targetY) {
                if (r.textMsg >= 0) spawnTextBurst(msgs[r.textMsg], r.color);
                else spawnBurst(r.x, r.y, r.color, r.textMsg == -2);
                rockets.remove(i);
            }
        }
    }

    private void spawnBurst(double x, double y, Color base, boolean small) {
        int n = small ? 45 + rnd.nextInt(30) : 150 + rnd.nextInt(120);
        double power = small ? 4.0 : 6.0 + rnd.nextDouble() * 3.5;
        boolean multi = rnd.nextDouble() < 0.4;
        boolean ring = rnd.nextDouble() < 0.5;
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double sp = ring ? power * (0.85 + rnd.nextDouble() * 0.2) : power * rnd.nextDouble();
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            p.vx = Math.cos(a) * sp;
            p.vy = Math.sin(a) * sp;
            p.color = multi ? palette[rnd.nextInt(palette.length)] : jitter(base);
            p.size = 2.5f + rnd.nextFloat() * 2.5f;
            p.decay = 0.008 + rnd.nextDouble() * 0.01;
            p.glitter = rnd.nextDouble() < 0.5;
            particles.add(p);
        }
    }

    private void spawnTextBurst(String msg, Color col) {
        List<Point2D> pts = textPoints(msg);
        double ox = wLog() / 2.0, oy = hLog() * 0.45;
        for (Point2D pt : pts) {
            Particle p = new Particle();
            p.x = ox + (rnd.nextDouble() - 0.5) * BASE * 0.06;
            p.y = oy + (rnd.nextDouble() - 0.5) * BASE * 0.06;
            p.tx = pt.getX();
            p.ty = pt.getY();
            p.seeking = true;
            p.holdFrames = 95 + rnd.nextInt(25);
            p.color = jitter(col);
            p.size = 3f;
            p.decay = 0.012;
            p.glitter = rnd.nextDouble() < 0.4;
            particles.add(p);
        }
    }

    private void updateParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            if (p.seeking) {
                p.x += (p.tx - p.x) * 0.14 + (rnd.nextDouble() - 0.5) * 0.6;
                p.y += (p.ty - p.y) * 0.14 + (rnd.nextDouble() - 0.5) * 0.6;
                p.holdFrames--;
                if (p.holdFrames <= 0) {
                    p.seeking = false;
                    p.vx = (rnd.nextDouble() - 0.5) * 1.5;
                    p.vy = rnd.nextDouble() * 0.5;
                }
            } else {
                p.vy += 0.12;
                p.vx *= 0.985;
                p.vy *= 0.985;
                p.x += p.vx;
                p.y += p.vy;
                p.life -= p.decay;
                if (p.life <= 0) particles.remove(i);
            }
        }
    }

    private Confetti newConfetti(boolean anywhere) {
        Confetti c = new Confetti();
        c.x = rnd.nextDouble() * wLog();
        c.y = anywhere ? rnd.nextDouble() * hLog() : -10;
        c.vx = (rnd.nextDouble() - 0.5) * 0.8;
        c.vy = 2.5 + rnd.nextDouble() * 3.5;
        c.angle = rnd.nextDouble() * Math.PI;
        c.spin = (rnd.nextDouble() - 0.5) * 0.3;
        c.w = 8 + rnd.nextDouble() * 8;
        c.h = 5 + rnd.nextDouble() * 5;
        c.phase = rnd.nextDouble() * Math.PI * 2;
        c.color = palette[rnd.nextInt(palette.length)];
        return c;
    }

    private void updateConfetti() {
        double hl = hLog(), wl = wLog();
        for (int i = 0; i < confetti.size(); i++) {
            Confetti c = confetti.get(i);
            c.y += c.vy;
            c.x += c.vx + Math.sin(tick * 0.05 + c.phase) * 0.7;
            c.angle += c.spin;
            if (c.y > hl + 14) {
                Confetti n = newConfetti(false);
                n.x = rnd.nextDouble() * wl;
                confetti.set(i, n);
            }
        }
    }

    // ----- text sampling (logical space) -----

    private List<Point2D> textPoints(String msg) {
        int fontSize = (int) (BASE * 0.22);
        Font font = baseFont.deriveFont(Font.BOLD, (float) fontSize);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        pg.setFont(font);
        FontMetrics fm = pg.getFontMetrics();
        int tw = fm.stringWidth(msg);
        double maxW = wLog() * 0.82;
        if (tw > maxW && tw > 0) {
            fontSize = (int) (fontSize * maxW / tw);
            font = baseFont.deriveFont(Font.BOLD, (float) fontSize);
            pg.setFont(font);
            fm = pg.getFontMetrics();
            tw = fm.stringWidth(msg);
        }
        int th = fm.getHeight();
        pg.dispose();

        BufferedImage img = new BufferedImage(Math.max(1, tw + 4), Math.max(1, th + 4), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.drawString(msg, 2, 2 + fm.getAscent());
        g.dispose();

        int step = Math.max(6, fontSize / 13); // ~1/3 density
        List<Point2D> pts = new ArrayList<Point2D>();
        double offx = (wLog() - img.getWidth()) / 2.0;
        double offy = hLog() * 0.45 - img.getHeight() / 2.0;
        for (int y = 0; y < img.getHeight(); y += step)
            for (int x = 0; x < img.getWidth(); x += step)
                if ((img.getRGB(x, y) >>> 24) > 128)
                    pts.add(new Point2D.Double(x + offx, y + offy));
        Collections.shuffle(pts, rnd);
        return pts;
    }

    // ----- rendering -----

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        int sw = (int) Math.round(wLog() * SCENE_SCALE);
        int sh = (int) Math.round(hLog() * SCENE_SCALE);
        if (sw < 1 || sh < 1) return;
        if (scene == null || scene.getWidth() != sw || scene.getHeight() != sh) {
            bg = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
            Graphics2D bgg = bg.createGraphics();
            bgg.setPaint(new GradientPaint(0, 0, new Color(22, 10, 42), 0, sh, new Color(6, 3, 14)));
            bgg.fillRect(0, 0, sw, sh);
            bgg.dispose();
            scene = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
            Graphics2D sg0 = scene.createGraphics();
            sg0.drawImage(bg, 0, 0, null); // start on the full gradient, not black
            sg0.dispose();
            stepsSincePaint = 0;
        }

        // 1) advance the persistent scene buffer. Drawing the gradient over it with SRC_OVER at
        // the fade alpha is equivalent to the old "DST_OUT fade then compose over gradient": every
        // pixel decays toward the gradient, so trails fade identically (8-bit rounding may leave a
        // ~1-2 level ghost, the same class of artifact as the old DST_OUT rounding). Glow sprites
        // are pre-rendered at scene resolution and blitted 1:1; logical coords are mapped in drawGlow.
        int n = stepsSincePaint;
        stepsSincePaint = 0;
        if (n > 0) {
            Graphics2D sg = scene.createGraphics();
            sg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha(n)));
            sg.drawImage(bg, 0, 0, null);
            sg.setComposite(AlphaComposite.SrcOver);
            for (Rocket r : rockets) drawGlow(sg, r.x, r.y, 5f, r.color, 255);
            for (Particle p : particles) {
                int a = (int) (clamp01(p.life) * 255);
                if (p.glitter && rnd.nextDouble() < 0.25) a = (int) (a * 0.4);
                drawGlow(sg, p.x, p.y, p.size, p.color, a);
            }
            sg.dispose();
        }

        // 2) upscale the scene to the panel, then draw confetti on top in window coordinates so
        // they leave no trail (not part of the persistent buffer) and stay crisp (drawn with AA,
        // not through the bilinear upscale).
        Graphics2D screen = (Graphics2D) g;
        screen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        screen.drawImage(scene, 0, 0, w, h, null);
        double sc = s();
        screen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform saved = screen.getTransform();
        for (Confetti c : confetti) {
            screen.translate(c.x * sc, c.y * sc);
            screen.rotate(c.angle);
            screen.setColor(c.color);
            screen.fillRect((int) (-c.w / 2 * sc), (int) (-c.h / 2 * sc), (int) (c.w * sc), (int) (c.h * sc));
            screen.setTransform(saved);
        }
    }

    /** Fade fraction for n elapsed physics steps: 1 - 0.86^n (0.14 for 1, 0.2604 for 2). */
    private float fadeAlpha(int n) {
        if (n <= 1) return 0.14f;
        if (n == 2) return 0.2604f;
        return (float) (1 - Math.pow(0.86, n));
    }

    private void drawGlow(Graphics2D g, double x, double y, float size, Color c, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        if (alpha == 0) return;
        BufferedImage sprite = glowSprite(c, size);
        double half = sprite.getWidth() / 2.0;
        Composite old = g.getComposite();
        g.setComposite(srcOver(alpha));
        g.drawImage(sprite, (int) Math.round(x * SCENE_SCALE - half), (int) Math.round(y * SCENE_SCALE - half), null);
        g.setComposite(old);
    }

    private AlphaComposite srcOver(int alpha) {
        int i = alpha >> 3; // 0..31 (255 -> 31)
        AlphaComposite ac = alphaCache[i];
        if (ac == null) {
            ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(255, i * 8 + 4) / 255f);
            alphaCache[i] = ac;
        }
        return ac;
    }

    /** A halo+core glow sprite for the color/size bucket, rendered once and cached. */
    private BufferedImage glowSprite(Color c, float size) {
        int rq = c.getRed() >> 5, gq = c.getGreen() >> 5, bq = c.getBlue() >> 5; // 8 levels/channel
        int sq = Math.max(1, Math.round(size * 2f));                              // 0.5-unit buckets
        int key = ((sq * 8 + rq) * 8 + gq) * 8 + bq;
        BufferedImage img = glowCache.get(key);
        if (img == null) {
            img = renderGlow(rq * 32 + 16, gq * 32 + 16, bq * 32 + 16, sq / 2f);
            glowCache.put(key, img);
        }
        return img;
    }

    /**
     * Draws the glow at scene resolution and full opacity so per-particle alpha can come from
     * the composite: a dim halo (alpha 255/6) and a brightened core (alpha 255) matching the old
     * direct draw. The logical size is pre-multiplied by SCENE_SCALE so sprites blit 1:1.
     */
    private BufferedImage renderGlow(int r, int gr, int bl, float size) {
        r = Math.min(255, r);
        gr = Math.min(255, gr);
        bl = Math.min(255, bl);
        size *= (float) SCENE_SCALE;
        float halo = size * 3f;
        int dim = (int) Math.ceil(halo * 2f) + 2;
        BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double cx = dim / 2.0, cy = dim / 2.0;
        g.setColor(new Color(r, gr, bl, 255 / 6));
        g.fill(new Ellipse2D.Double(cx - halo, cy - halo, halo * 2, halo * 2));
        g.setColor(new Color(Math.min(255, r + 70), Math.min(255, gr + 70), Math.min(255, bl + 70), 255));
        g.fill(new Ellipse2D.Double(cx - size, cy - size, size * 2, size * 2));
        g.dispose();
        return img;
    }

    private double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    private Color jitter(Color c) {
        int d = 30;
        int r = Math.max(0, Math.min(255, c.getRed() + rnd.nextInt(d * 2) - d));
        int gr = Math.max(0, Math.min(255, c.getGreen() + rnd.nextInt(d * 2) - d));
        int bl = Math.max(0, Math.min(255, c.getBlue() + rnd.nextInt(d * 2) - d));
        return new Color(r, gr, bl);
    }

    // ---------------------------------------------------------------------

    private static final class Rocket {
        double x, y, vx, vy, targetY;
        Color color;
        int textMsg = -1; // >=0 form text idx, -1 normal, -2 small ambient
    }

    private static final class Particle {
        double x, y, vx, vy, tx, ty;
        double life = 1.0, decay = 0.01;
        boolean seeking = false, glitter = false;
        int holdFrames = 0;
        Color color = Color.WHITE;
        float size = 2f;
    }

    private static final class Confetti {
        double x, y, vx, vy, angle, spin, phase, w, h;
        Color color;
    }
}
