/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.style.gui;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.style.gui.tri.TriColorPickerButton;
import com.panayotis.jubler.subs.style.gui.tri.TriObject;

import javax.swing.*;
import java.awt.*;

import static com.panayotis.jubler.i18n.I18N.__;

public class JColorPickerDialog extends JWindow {

    private TriObject[] allButtons;
    private TriColorPickerButton pickerButton;
    private JPanel panel;
    private int currentIndex = 0;

    public JColorPickerDialog(JubFrame parent, TriColorPickerButton picker, TriObject[] all) {
        super(parent);
        this.pickerButton = picker;
        this.allButtons = all;
        initComponents();
    }

    private void initComponents() {
        panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1, 0, 2));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        panel.setBackground(UIManager.getColor("Panel.background"));
        panel.setFocusable(true);
        panel.setRequestFocusEnabled(true);
        panel.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                setVisible(false);
            }
        });

        add(panel);

        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
            }
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
                setVisible(false);
            }
        });

        addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                setVisible(false);
            }
        });
    }

    private void updatePanel() {
        panel.removeAll();

        String[] labels = {__("Primary"), __("Secondary"), __("Outline"), __("Shadow")};

        for (int i = 0; i < allButtons.length; i++) {
            final int index = i;
            JLabel label = new JLabel(labels[i]);
            label.setIcon(((AbstractButton) allButtons[i]).getIcon());
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (index != currentIndex) {
                        pickerButton.switchToColor(index);
                    }
                    currentIndex = index;
                    setVisible(false);
                }
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    label.setOpaque(true);
                    label.setBackground(UIManager.getColor("List.selectionBackground"));
                    label.setForeground(UIManager.getColor("List.selectionForeground"));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    label.setOpaque(false);
                    label.setForeground(UIManager.getColor("Label.foreground"));
                }
            });
            panel.add(label);
        }

        pack();
    }

    public void setCurrentIndex(int index) {
        this.currentIndex = index;
    }

    public void showBelow(JComponent component) {
        updatePanel();
        Point location = component.getLocationOnScreen();
        setLocation(location.x, location.y + component.getHeight());
        setVisible(true);
        if (!panel.requestFocusInWindow()) {
            panel.requestFocus();
        }
    }
}
