/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals.gui;

import com.panayotis.jubler.theme.Theme;

import javax.swing.ImageIcon;
import javax.swing.JPasswordField;
import javax.swing.JToggleButton;
import java.awt.Image;
import java.awt.Insets;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Helpers for masked secret fields: a {@link JPasswordField} plus an eye toggle that reveals or
 * hides the value (mask off / on). Shared by the recipe editor and the run dialog so a secret
 * looks and behaves the same in both places.
 */
final class SecretField {

    private SecretField() {
    }

    /** An eye toggle bound to {@code field}: unselected = masked ({@code previewc}), selected = shown ({@code preview}). */
    static JToggleButton revealToggle(JPasswordField field) {
        final char echo = field.getEchoChar();
        JToggleButton b = new JToggleButton();
        ImageIcon hidden = scaled("previewc");
        ImageIcon shown = scaled("preview");
        if (hidden != null)
            b.setIcon(hidden);
        if (shown != null)
            b.setSelectedIcon(shown);
        if (hidden == null && shown == null)
            b.setText("👁");   // 👁 fallback when icons are missing
        b.setToolTipText(__("Show or hide the value"));
        b.setMargin(new Insets(2, 6, 2, 6));
        b.addActionListener(e -> field.setEchoChar(b.isSelected() ? (char) 0 : echo));
        return b;
    }

    private static ImageIcon scaled(String name) {
        ImageIcon base = Theme.loadIcon(name);
        return base == null ? null : new ImageIcon(base.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));
    }
}
