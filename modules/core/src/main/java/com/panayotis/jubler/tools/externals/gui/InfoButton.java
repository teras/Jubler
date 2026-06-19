/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals.gui;

import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.theme.Theme;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.Component;
import java.awt.Image;
import java.awt.Insets;
import java.util.function.Supplier;

/**
 * A small "info" button that opens an explanatory popup instead of relying on a tooltip.
 * The popup itself is the shared {@link #show} function, so its look and behaviour can be
 * changed in one place for the whole application.
 */
public class InfoButton extends JButton {

    private static final int SIZE = 24;

    public InfoButton(String title, String help) {
        this(title, () -> help);
    }

    /** Variant whose help text is computed at click time (e.g. depends on a current selection). */
    public InfoButton(String title, Supplier<String> help) {
        super(infoIcon());
        if (getIcon() == null)
            setText("i");
        setMargin(new Insets(2, 4, 2, 4));
        setFocusable(false);
        setToolTipText(title);
        addActionListener(e -> show(this, title, help.get()));
    }

    private static Icon infoIcon() {
        ImageIcon base = Theme.loadIcon("info");
        if (base == null)
            return null;
        return new ImageIcon(base.getImage().getScaledInstance(SIZE, SIZE, Image.SCALE_SMOOTH));
    }

    /** Shared explanatory popup — change this once to restyle every info popup. */
    public static void show(Component parent, String title, String help) {
        JIDialog.info(parent, "<html><body style='width:340px'>" + help + "</body></html>", title);
    }
}
