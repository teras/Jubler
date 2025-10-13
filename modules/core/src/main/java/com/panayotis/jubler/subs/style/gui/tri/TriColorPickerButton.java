/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.style.gui.tri;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.style.gui.JColorPickerDialog;
import com.panayotis.jubler.theme.Theme;

import javax.swing.*;
import java.awt.*;

public class TriColorPickerButton extends JButton {

    private JColorPickerDialog colorPicker;
    private JPanel containerPanel;
    private int currentIndex = 0;

    public TriColorPickerButton(JubFrame parent, JPanel container, TriObject[] allButtons) {
        this.containerPanel = container;
        
        setIcon(Theme.loadIcon("colorpicker"));
        setFocusable(false);
        
        colorPicker = new JColorPickerDialog(parent, this, allButtons);
        
        addActionListener(e -> colorPicker.showBelow(this));
    }

    public void switchToColor(int newIndex) {
        currentIndex = newIndex;
        colorPicker.setCurrentIndex(newIndex);
        
        CardLayout cl = (CardLayout) containerPanel.getLayout();
        cl.show(containerPanel, String.valueOf(newIndex + 6));
    }
}
