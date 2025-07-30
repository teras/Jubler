/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler;

import com.panayotis.jubler.os.DEBUG;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

public class Splash extends JFrame {

    private static Splash instance;

    public static void launch(Runnable r) {
        try {
            instance = new Splash(r);
        } catch (Exception e) {
            DEBUG.debug(e);
        }
    }

    public static void finish() {
        if (instance != null) {
            instance.setVisible(false);
            instance.dispose();
            instance = null;
        }
    }

    private transient Runnable r;

    private final double scale;

    public Splash(Runnable r) {
        this.r = r;
        Rectangle bounds = getGraphicsConfiguration().getBounds();
        scale = Math.min((bounds.width / 5.0) / 400, (bounds.height / 5.0) / 300);
        setSize((int) (320 * scale), (int) (320 * scale));
        setUndecorated(true);
        setLocationRelativeTo(null);
        setIconImage(new IconImage());
        setBackground(new Color(235, 240, 235, 255));
        setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform t = g2.getTransform();
        t.scale(scale, scale);
        g2.setTransform(t);
        Bird.draw(g2);
        if (r != null) {
            Runnable r2 = r;
            r = null;
            SwingUtilities.invokeLater(r2);
        }
    }

    private static class IconImage extends BufferedImage {
        public IconImage() {
            super(80, 80, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) getGraphics();
            g.setColor(new Color(235, 240, 235, 255));
            g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
            AffineTransform t = g.getTransform();
            t.scale(0.25, 0.25);
            g.setTransform(t);
            Bird.draw(g);
        }
    }

    static final class Bird {
        static void draw(Graphics2D g) {
            drawPath(g, createPath1(), new Color(171, 48, 177));
            drawPath(g, createPath2(), new Color(239, 184, 4));
            drawPath(g, createPath3(), new Color(0, 102, 171));
            drawPath(g, createPath4(), new Color(0, 169, 107));
            drawPath(g, createPath5(), new Color(48, 43, 65));
            drawPath(g, createPath6(), new Color(250, 250, 250));
        }

        private static void drawPath(Graphics g, GeneralPath path, Color fillColor) {
            g.setColor(fillColor);
            ((Graphics2D) g).fill(path);
        }

        private static GeneralPath createPath1() {
            GeneralPath path = new GeneralPath();
            path.moveTo(138.33f, 275.59f);
            path.lineTo(34.88f, 275.59f);
            path.curveTo(34.95f, 217.46f, 80.82f, 170.04f, 138.4f, 167.38f);
            path.closePath();
            return path;
        }

        private static GeneralPath createPath2() {
            GeneralPath path = new GeneralPath();
            path.moveTo(138.07f, 275.59f);
            path.moveTo(271.31f, 145.29f);
            path.curveTo(265.66f, 85.07f, 214.89f, 37.95f, 153.09f, 37.95f);
            path.curveTo(90.69f, 37.95f, 39.53f, 86.0f, 34.72f, 147.07f);
            path.closePath();
            return path;
        }

        private static GeneralPath createPath3() {
            GeneralPath path = new GeneralPath();
            path.moveTo(138.07f, 275.59f);
            path.lineTo(271.84f, 275.59f);
            path.lineTo(271.84f, 156.5f);
            path.curveTo(271.84f, 91.03f, 218.68f, 37.95f, 153.09f, 37.95f);
            path.curveTo(147.06f, 37.95f, 142.09f, 38.26f, 137.22f, 38.87f);
            path.closePath();
            return path;
        }

        private static GeneralPath createPath4() {
            GeneralPath path = new GeneralPath();
            path.moveTo(163.33f, 275.59f);
            path.lineTo(271.84f, 275.59f);
            path.lineTo(271.84f, 167.26f);
            path.curveTo(211.94f, 167.33f, 163.4f, 215.79f, 163.33f, 275.59f);
            path.closePath();
            return path;
        }

        private static GeneralPath createPath5() {
            GeneralPath path = new GeneralPath();
            path.moveTo(189.39f, 100.9f);
            path.append(new Arc2D.Float(175.96f, 87.09f, 27.639f, 27.639f, 0, 360, Arc2D.Float.OPEN), false);
            return path;
        }

        private static GeneralPath createPath6() {
            GeneralPath path = new GeneralPath();
            path.moveTo(191.39f, 103.0f);
            path.append(new Arc2D.Float(184.87f, 96.49f, 10.26f, 10.26f, 0, 360, Arc2D.Float.OPEN), false);
            return path;
        }

        private Bird() {
        }
    }
}
