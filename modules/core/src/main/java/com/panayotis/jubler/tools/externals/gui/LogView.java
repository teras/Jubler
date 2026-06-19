/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Reusable, collapsible log widget: a scrolling, read-only text area with an
 * {@code addLine} sink and a "Show / Hide log" toggle.
 */
public class LogView extends JPanel {

    private final JTextArea area = new JTextArea(14, 64);
    private final JScrollPane scroll = new JScrollPane(area);
    private final JButton toggle = new JButton();
    private boolean expanded = true;

    public LogView() {
        setOpaque(false);
        setLayout(new BorderLayout());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setLineWrap(false);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        toggle.setBorderPainted(false);
        toggle.setContentAreaFilled(false);
        toggle.setFocusPainted(false);
        toggle.addActionListener(e -> setExpanded(!expanded));
        header.add(toggle);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        updateToggle();
    }

    public void addLine(String line) {
        area.append(line + "\n");
        area.setCaretPosition(area.getDocument().getLength());
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        scroll.setVisible(expanded);
        updateToggle();
        revalidate();
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null)
            w.pack();
    }

    private void updateToggle() {
        toggle.setText(expanded ? __("Hide log") : __("Show log"));
        scroll.setPreferredSize(expanded ? null : new Dimension(0, 0));
    }
}
