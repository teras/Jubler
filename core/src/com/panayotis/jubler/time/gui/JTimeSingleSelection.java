/*
 * JTimeSingleSelection.java
 *
 * Created on 25 Ιούνιος 2005, 3:31 μμ
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.panayotis.jubler.time.gui;

import com.panayotis.jubler.time.Time;
import java.awt.BorderLayout;

/**
 *
 * @author  teras
 */
public class JTimeSingleSelection extends javax.swing.JPanel {

    private JTimeSpinner splitpos;
    private String DialogLabel;

    /** Creates new form JJoin */
    public JTimeSingleSelection(Time t, String DialogLabel) {
        this(DialogLabel);
        setTime(t);
    }

    public JTimeSingleSelection(String DialogLabel) {
        splitpos = new JTimeSpinner();
        this.DialogLabel = DialogLabel;
        initComponents();
        ViewP.add(splitpos, BorderLayout.CENTER);
    }

    public void setToolTip(String txt) {
        splitpos.setToolTipText(txt);
    }

    public void setLabel(String txt) {
    }

    public void setTime(Time t) {
        splitpos.setValue(t);
    }

    public Time getTime() {
        return (Time) splitpos.getValue();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        ViewP = new javax.swing.JPanel();
        TimeLabel = new javax.swing.JLabel();

        setLayout(new java.awt.BorderLayout());

        ViewP.setLayout(new java.awt.BorderLayout());

        TimeLabel.setText(DialogLabel);
        ViewP.add(TimeLabel, java.awt.BorderLayout.NORTH);

        add(ViewP, java.awt.BorderLayout.NORTH);

    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel TimeLabel;
    private javax.swing.JPanel ViewP;
    // End of variables declaration//GEN-END:variables
}
