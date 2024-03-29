/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.gui;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.style.SubStyle.Direction;
import java.awt.MouseInfo;
import java.awt.Point;
import javax.swing.Icon;
import javax.swing.JWindow;

public class JDirectionDialog extends JWindow {

    private JDirection direction;

    /**
     * Creates new form JDirectionDIalog
     */
    public JDirectionDialog(JubFrame parent) {
        super(parent);
        initComponents();
        direction = new JDirection();
        add(direction);
        pack();
    }

    public void setVisible(boolean isVisible) {
        super.setVisible(isVisible);
        if (isVisible) {
            Point where = MouseInfo.getPointerInfo().getLocation();
            int width = getWidth();
            int height = getHeight();
            where.x -= width / 6 + direction.getControlX() * width / 3;
            where.y -= height / 6 + direction.getControlY() * height / 3;
            setLocation(where);
            direction.requestFocusInWindow();
        }
    }

    public Direction getDirection() {
        return direction.getDirection();
    }

    public Icon getIcon() {
        return direction.getIcon();
    }

    public Icon getIcon(Direction d) {
        return direction.getIcon(d);
    }

    public void setListener(DirectionListener listener) {
        direction.setListener(listener);
    }

    public void setDirection(Direction d) {
        direction.setDirection(d);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                formFocusLost(evt);
            }
        });

    }
    // </editor-fold>//GEN-END:initComponents

    private void formFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_formFocusLost
        setVisible(false);
    }//GEN-LAST:event_formFocusLost
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
