/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.gui;

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.gui.JRateChooser;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.SubFormat;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;

/**
 * Bar shown above the subtitle table, mirroring the bottom edit bar. It hosts the shared
 * {@link JEncodingChooser}, an FPS chooser and a subtitle-format chooser — all plain document
 * properties. Right after a load, while the raw bytes are still cached, changing encoding or FPS
 * re-parses the buffered bytes in place (live re-decode); once the bytes are released (first edit or
 * explicit close) it stops re-decoding and the choosers simply reflect/edit the document properties.
 * The bar auto-appears on load and auto-hides on the first edit, but can be toggled open again from
 * the toolbar, in which case it stays open.
 */
public class JEncodingBar extends JPanel {

    private final JEncodingChooser chooser = new JEncodingChooser();
    private final JRateChooser rate = new JRateChooser();
    private final JComboBox<SubFormat> format = new JComboBox<>();
    private final JLabel fpsLabel = new JLabel(__("FPS") + ":");
    private boolean updating;

    public JEncodingBar(Runnable onReload, Consumer<SubFormat> onFormat) {
        super(new BorderLayout());
        chooser.setChangeListener(enc -> {
            if (!updating)
                onReload.run();
        });
        rate.addChangeListener(() -> {
            if (!updating)
                onReload.run();
        });
        for (SubFormat f : Availabilities.formats.getFormats())
            format.addItem(f);   // shown via SubFormat.toString() (extended name + extension)
        format.addActionListener(e -> {
            if (!updating) {
                SubFormat sel = (SubFormat) format.getSelectedItem();
                setFpsEnabled(sel != null && sel.supportsFPS());
                onFormat.accept(sel);
            }
        });

        setOpaque(true);
        setBackground(rowColor());
        setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 8));

        // One shared row: every field fills vertically, so the bare format combo takes the same height
        // as the encoding/FPS choosers (which are stretched taller by their sibling icon buttons) —
        // the row height drives it, with no hardcoded sizes.
        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = GridBagConstraints.VERTICAL;
        addField(left, g, 0, new JLabel(__("Encoding") + ":"), chooser);
        addField(left, g, 20, fpsLabel, rate);
        addField(left, g, 20, new JLabel(__("Format") + ":"), format);

        add(left, BorderLayout.WEST);
        setVisible(false);
    }

    /** Add a "Label: field" pair to the shared row, with a left gap before the label. */
    private static void addField(JPanel row, GridBagConstraints g, int leftGap, JLabel label, JComponent field) {
        g.insets = new Insets(0, leftGap, 0, 4);
        row.add(label, g);
        g.insets = new Insets(0, 0, 0, 0);
        row.add(field, g);
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

    /**
     * Show the bar reflecting the document's current encoding, FPS and format. FPS is always visible;
     * the format chooser is populated on construction and merely selects the document's format here
     * (it does not yet drive save — that wiring comes later).
     */
    public void showFor(String encoding, MediaFile mfile, Subtitles subs) {
        updating = true;
        chooser.setEncoding(encoding);
        SubFormat current = subs.getSubFile().getFormat();
        selectFormat(current);
        rate.setDataFiles(mfile, subs);
        rate.setFPS(subs.getSubFile().getFPS());
        setFpsEnabled(current != null && current.supportsFPS());
        updating = false;
        setVisible(true);
        revalidate();
        repaint();
    }

    /** Grey out the FPS group (kept visible) for formats where frame rate is meaningless. */
    private void setFpsEnabled(boolean enabled) {
        fpsLabel.setEnabled(enabled);
        rate.setEnabled(enabled);
    }

    /** Select the combo item matching the document's format (by name), without firing a reload. */
    private void selectFormat(SubFormat current) {
        if (current == null)
            return;
        for (int i = 0; i < format.getItemCount(); i++)
            if (format.getItemAt(i).getName().equals(current.getName())) {
                format.setSelectedIndex(i);
                return;
            }
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
