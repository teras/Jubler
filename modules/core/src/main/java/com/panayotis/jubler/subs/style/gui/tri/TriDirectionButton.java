/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.gui.tri;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.style.StyleChangeListener;
import com.panayotis.jubler.subs.style.SubStyle.Direction;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.gui.DirectionListener;
import com.panayotis.jubler.subs.style.gui.JDirectionDialog;
import javax.swing.ImageIcon;
import javax.swing.JButton;

public class TriDirectionButton extends JButton implements TriObject, DirectionListener {

    private JDirectionDialog direction;
    private Direction dir;
    private ImageIcon disabled;

    /**
     * Creates a new instance of TriDirButton
     */
    public TriDirectionButton(JubFrame parent) {

        direction = new JDirectionDialog(parent);
        direction.setListener(this);
        dir = Direction.BOTTOM;
        setIcon(direction.getIcon(dir));
        disabled = DarkIconFilter.getDisabledIcon((ImageIcon) direction.getIcon(Direction.CENTER));

        addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (ignore_element_changes)
                    return;
                direction.setDirection(dir);
                direction.setVisible(true);
            }
        });

    }
    private StyleChangeListener listener;

    public void setStyle(StyleType style) {
    } // We already know the style!

    public void setListener(StyleChangeListener listener) {
        this.listener = listener;
    }
    private boolean ignore_element_changes = false;

    public void setData(Object data) {
        ignore_element_changes = true;
        dir = (Direction) data;
        if (dir == null)
            setIcon(disabled);
        else
            setIcon(direction.getIcon(dir));
        ignore_element_changes = false;
    }

    public void directionUpdated() {
        direction.setVisible(false);
        setIcon(direction.getIcon());
        dir = direction.getDirection();
        listener.changeStyle(StyleType.DIRECTION, dir);
    }

    public void focusLost() {
        direction.setVisible(false);
    }
}
