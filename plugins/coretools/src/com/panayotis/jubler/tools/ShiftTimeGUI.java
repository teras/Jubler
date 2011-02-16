/*
 * JShiftTime.java
 *
 * Created on 24 Ιούνιος 2005, 11:49 μμ
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
package com.panayotis.jubler.tools;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.os.SystemDependent;

import com.panayotis.jubler.time.gui.JTimeSpinner;
import java.awt.BorderLayout;

/**
 *
 * @author  teras
 */
public class ShiftTimeGUI extends ToolGUI {

    JTimeSpinner dt;

    @Override
    public void initialize() {
        dt = new JTimeSpinner();
        initComponents();
        PNewTime.add(dt, BorderLayout.CENTER);
        dt.setToolTipText(_("The amount of time in order to shift the subtitles"));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        PNewTime = new javax.swing.JPanel();
        CSign = new javax.swing.JComboBox();

        setOpaque(false);
        setLayout(new java.awt.BorderLayout());

        PNewTime.setBorder(SystemDependent.getBorder(_("Shift Subtitles")));
        PNewTime.setOpaque(false);
        PNewTime.setLayout(new java.awt.BorderLayout());

        CSign.setModel(new javax.swing.DefaultComboBoxModel(new String[] { " + ", " - " }));
        CSign.setToolTipText(_("Either increase or decrease the time"));
        PNewTime.add(CSign, java.awt.BorderLayout.WEST);

        add(PNewTime, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JComboBox CSign;
    private javax.swing.JPanel PNewTime;
    // End of variables declaration//GEN-END:variables
}
