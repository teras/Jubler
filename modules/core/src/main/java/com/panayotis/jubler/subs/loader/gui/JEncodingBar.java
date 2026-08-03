/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.gui;

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.gui.JRateChooser;
import com.panayotis.jubler.subs.Subtitles;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

/**
 * Transient bar shown above the subtitle table right after a file is loaded, mirroring the bottom
 * edit bar. It hosts the shared {@link JEncodingChooser} and (only for frame-based formats) an FPS
 * chooser: changing either re-reads the buffered bytes and re-parses in place. The bar is closed
 * either explicitly (round red button) or on the first edit, after which encoding/FPS become plain
 * document properties.
 */
public class JEncodingBar extends JPanel {

    private final JEncodingChooser chooser = new JEncodingChooser();
    private final JRateChooser rate = new JRateChooser();
    private final JPanel fpsPanel;
    private boolean updating;

    public JEncodingBar(Runnable onReload, Runnable onClose) {
        super(new BorderLayout());
        chooser.setChangeListener(enc -> {
            if (!updating)
                onReload.run();
        });
        rate.addChangeListener(() -> {
            if (!updating)
                onReload.run();
        });

        setOpaque(true);
        setBackground(rowColor());
        setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 8));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        left.setOpaque(false);
        left.add(new JLabel(__("Encoding") + ":"));
        left.add(chooser);

        fpsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        fpsPanel.setOpaque(false);
        fpsPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));   // space between the two groups
        fpsPanel.add(new JLabel(__("FPS") + ":"));
        fpsPanel.add(rate);
        left.add(fpsPanel);

        // Wrap so BorderLayout.EAST does not stretch the button vertically — keep it square/round.
        JPanel closeWrap = new JPanel(new GridBagLayout());
        closeWrap.setOpaque(false);
        closeWrap.add(closeButton(onClose));

        add(left, BorderLayout.WEST);
        add(closeWrap, BorderLayout.EAST);
        setVisible(false);
    }

    /** A subtle blue tint over the current theme's panel colour, so it reads on both light and dark. */
    private static Color rowColor() {
        Color bg = UIManager.getColor("Panel.background");
        if (bg == null)
            bg = new Color(0xEE, 0xEE, 0xEE);
        double lum = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255.0;
        return lum < 0.5 ? new Color(0x2C, 0x3E, 0x57)   // dark theme: deep muted blue
                : new Color(0xD3, 0xE6, 0xF8);            // light theme: soft light blue
    }

    /** Plain button to dismiss the bar (native look-and-feel, hover/pressed for free). */
    private static JButton closeButton(Runnable onClose) {
        JButton b = new JButton(__("Hide"));
        b.setFocusable(false);
        b.addActionListener(e -> onClose.run());
        return b;
    }

    /**
     * Show the bar set to the given (detected) encoding and, for frame-based formats, the document
     * FPS. The FPS group is hidden entirely for time-based formats where FPS is meaningless.
     */
    public void showFor(String encoding, MediaFile mfile, Subtitles subs) {
        updating = true;
        chooser.setEncoding(encoding);
        boolean fps = subs.getSubFile().getFormat().supportsFPS();
        fpsPanel.setVisible(fps);
        if (fps) {
            rate.setDataFiles(mfile, subs);
            rate.setFPS(subs.getSubFile().getFPS());
        }
        updating = false;
        setVisible(true);
        revalidate();
        repaint();
    }

    public String getEncoding() {
        return chooser.getEncoding();
    }

    public float getFPSValue() {
        return rate.getFPSValue();
    }

    public void hideBar() {
        setVisible(false);
        revalidate();
        repaint();
    }
}
