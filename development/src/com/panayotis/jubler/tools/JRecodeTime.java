/*
 * JRecodeTime.java
 *
 * Created on 25 Ιούνιος 2005, 1:53 πμ
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
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.SubEntry;
import java.awt.BorderLayout;
import com.panayotis.jubler.media.console.TimeSync;
import com.panayotis.jubler.options.gui.JRateChooser;

/**
 *
 * @author  teras
 */
public class JRecodeTime extends JToolRealTime {

    private double factor;
    private double center;
    private TimeSync t1,  t2;
    private JRateChooser FromR,  ToR;

    /** Creates new form JRecodeTime */
    public JRecodeTime() {
        super(true);
    }

    public void initialize() {
        initComponents();

        FromR = new JRateChooser();
        FromP.add(FromR, BorderLayout.CENTER);
        ToR = new JRateChooser();
        ToP.add(ToR, BorderLayout.CENTER);

        t1 = t2 = null;
    }

    protected String getToolTitle() {
        return _("Recode time");
    }

    public boolean setValues(TimeSync first, TimeSync second) {
        super.setValues(first, second);

        if (first.smallerThan(second)) {
            t1 = first;
            t2 = second;
        } else {
            t1 = second;
            t2 = first;
        }

        double given_factor, given_center;

        given_center = (t2.timediff * t1.timepos - t1.timediff * t2.timepos) / (t2.timediff - t1.timediff);
        if (Double.isInfinite(given_center) || Double.isNaN(given_center)) {
            t1 = t2 = null;
            given_center = given_factor = 0;
            return false;
        }

        given_factor = (t1.timepos - t2.timepos + t1.timediff - t2.timediff) / (t1.timepos - t2.timepos);
        if (Double.isInfinite(given_factor) || Double.isNaN(given_factor)) {
            t1 = t2 = null;
            given_center = given_factor = 0;
            return false;
        }
        /* Set recode parameters */
        CustomC.setText(Double.toString(given_center));
        CustomF.setText(Double.toString(given_factor));

        /* Set default selections */
        CustomB.setSelected(true);

        return true;
    }

    public void updateData(Jubler j) {
        super.updateData(j);
        /* Set other values */
        FromR.setDataFiles(j.getMediaFile(), j.getSubtitles());
        ToR.setDataFiles(j.getMediaFile(), j.getSubtitles());
    }

    public void storeSelections() {
        center = 0;
        factor = 1;
        try {
            if (AutoB.isSelected()) {
                factor = FromR.getFPSValue() / ToR.getFPSValue();
            } else {
                factor = Double.parseDouble(CustomF.getText());
            }
            center = Double.parseDouble(CustomC.getText());
        } catch (NumberFormatException e) {
        }
    }

    protected void affect(int index) {
        SubEntry sub = affected_list.elementAt(index);
        sub.getStartTime().recodeTime(center, factor);
        sub.getFinishTime().recodeTime(center, factor);
    }

    protected void toggleRecodeMode(boolean status) {
        boolean nostatus = !status;
        FromR.setEnabled(status);
        ToR.setEnabled(status);
        ArrowL.setEnabled(status);
        CustomF.setEnabled(nostatus);
        RecodeL.setEnabled(nostatus);
        CustomC.setEnabled(nostatus);
        CentralL.setEnabled(nostatus);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Factor = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        AutoB = new javax.swing.JRadioButton();
        jPanel3 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        ArrowL = new javax.swing.JLabel();
        FromP = new javax.swing.JPanel();
        ToP = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        CustomB = new javax.swing.JRadioButton();
        jPanel6 = new javax.swing.JPanel();
        RecodeL = new javax.swing.JLabel();
        CustomF = new javax.swing.JTextField();
        jPanel7 = new javax.swing.JPanel();
        CentralL = new javax.swing.JLabel();
        CustomC = new javax.swing.JTextField();

        setLayout(new java.awt.BorderLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(15, 0, 0, 0), javax.swing.BorderFactory.createTitledBorder(_("Use the following factor"))));
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

        jPanel4.setLayout(new java.awt.BorderLayout());

        Factor.add(AutoB);
        AutoB.setSelected(true);
        AutoB.setText(_("Automatically compute based on FPS"));
        AutoB.setToolTipText(_("Use the following FPS in order to automatically compute the desired recoding"));
        AutoB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AutoBActionPerformed(evt);
            }
        });
        jPanel4.add(AutoB, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel4);

        jPanel3.setLayout(new java.awt.GridLayout(1, 2));

        jPanel2.setLayout(new java.awt.BorderLayout());

        ArrowL.setText(" -> ");
        ArrowL.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jPanel2.add(ArrowL, java.awt.BorderLayout.EAST);

        FromP.setLayout(new java.awt.BorderLayout());
        jPanel2.add(FromP, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel2);

        ToP.setLayout(new java.awt.BorderLayout());
        jPanel3.add(ToP);

        jPanel1.add(jPanel3);

        jPanel5.setLayout(new java.awt.BorderLayout());

        Factor.add(CustomB);
        CustomB.setText(_("Custom"));
        CustomB.setToolTipText(_("Use a custom factor in order to perform the recoding"));
        CustomB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CustomBActionPerformed(evt);
            }
        });
        jPanel5.add(CustomB, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel5);

        jPanel6.setLayout(new java.awt.GridLayout(1, 2));

        RecodeL.setText(_("Recoding factor"));
        RecodeL.setEnabled(false);
        jPanel6.add(RecodeL);

        CustomF.setText("1.0");
        CustomF.setToolTipText(_("The value of the custom factor which will do the recoding"));
        CustomF.setEnabled(false);
        jPanel6.add(CustomF);

        jPanel1.add(jPanel6);

        jPanel7.setLayout(new java.awt.GridLayout(1, 2));

        CentralL.setText(_("Central time"));
        CentralL.setEnabled(false);
        jPanel7.add(CentralL);

        CustomC.setText("0.0");
        CustomC.setToolTipText(_("The central time point which the recoding occurs. Usually left to 0 to apply evenly to the whole file."));
        CustomC.setEnabled(false);
        jPanel7.add(CustomC);

        jPanel1.add(jPanel7);

        add(jPanel1, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents
    private void AutoBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AutoBActionPerformed
        toggleRecodeMode(true);
    }//GEN-LAST:event_AutoBActionPerformed

    private void CustomBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CustomBActionPerformed
        toggleRecodeMode(false);
    }//GEN-LAST:event_CustomBActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel ArrowL;
    private javax.swing.JRadioButton AutoB;
    private javax.swing.JLabel CentralL;
    private javax.swing.JRadioButton CustomB;
    private javax.swing.JTextField CustomC;
    private javax.swing.JTextField CustomF;
    private javax.swing.ButtonGroup Factor;
    private javax.swing.JPanel FromP;
    private javax.swing.JLabel RecodeL;
    private javax.swing.JPanel ToP;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    // End of variables declaration//GEN-END:variables
}
