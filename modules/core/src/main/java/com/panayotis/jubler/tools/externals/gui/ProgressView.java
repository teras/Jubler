/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals.gui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * Reusable progress widget supporting both indeterminate (the default, for tools with
 * no structured progress) and determinate modes, plus a status line.
 */
public class ProgressView extends JPanel {

    private final JProgressBar bar = new JProgressBar();
    private final JLabel status = new JLabel(" ");

    public ProgressView() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        bar.setIndeterminate(true);
        status.setAlignmentX(LEFT_ALIGNMENT);
        bar.setAlignmentX(LEFT_ALIGNMENT);
        add(status);
        add(bar);
    }

    public void setStatus(String text) {
        status.setText(text == null || text.isEmpty() ? " " : text);
    }

    public void setIndeterminate() {
        bar.setIndeterminate(true);
    }

    public void setProgress(int value, int max) {
        bar.setIndeterminate(false);
        bar.setMaximum(max);
        bar.setValue(value);
    }

    /** Stop the animation and show a full (success) or empty (failure) bar. */
    public void stop(boolean success) {
        bar.setIndeterminate(false);
        bar.setMaximum(1);
        bar.setValue(success ? 1 : 0);
    }
}
