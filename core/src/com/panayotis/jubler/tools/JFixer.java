/*
 * JFixer.java
 *
 * Created on 5 Ιούλιος 2005, 12:34 μμ
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

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.SubEntry;
import java.awt.BorderLayout;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.time.gui.JDuration;
import com.panayotis.jubler.time.gui.JTimeRegion;
import java.text.DecimalFormat;
import javax.swing.JFormattedTextField;

import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author  teras
 */
public class JFixer extends JTool {
    
    private JDuration mintime, maxtime;
    
    private boolean fix;
    private int pushmodel;
    
    private double min_abs, min_cps, max_abs, max_cps, gap;
    
    /** Creates new form JFixer */
    public JFixer() {
        super(false);
    }
    
    public void initialize() {
        initComponents();
        
        mintime = new JDuration();
        MinTimeP.add(mintime, BorderLayout.CENTER);
        maxtime = new JDuration();
        MaxTimeP.add(maxtime, BorderLayout.CENTER);
    }
    
    protected String getToolTitle() {
        return _("Fix time inconsistencies");
    }
    
    protected void storeSelections() {
        
        
        /* Sort subtitles first */
        if (SortB.isSelected())
            subs.sort(((JTimeRegion)pos).getStartTime(), ((JTimeRegion)pos).getFinishTime());
        
        /* What to do with the remaining duration */
        fix = FixT.isSelected();
        if ( fix ) {
            pushmodel = PushModelB.getSelectedIndex();
        }
        
        min_abs = mintime.getAbsTime();
        min_cps = mintime.getCPSTime();
        max_abs = maxtime.getAbsTime();
        max_cps = maxtime.getCPSTime();
        
        gap = 0;
        if ( GapB.isSelected()) {
            try {
                gap = Double.parseDouble(GapNum.getText()) / 1000;
            } catch (NumberFormatException e) {}
        }
    }
    
    public boolean execute(Jubler jub) {
        boolean res = super.execute(jub);
        if (res) {
            if (SortB.isSelected()) {
                subs.sort(((JTimeRegion)pos).getStartTime(), ((JTimeRegion)pos).getFinishTime());
                return true;
            }
        }
        return false;
    }
    
    protected void affect(int index) {
        SubEntry sub = affected_list.elementAt(index);
        
        double curstart; /* The original start time */
        double curdur;  /* The original duration of the subtitle */
        int charcount;  /* The number of characters of the subtitle */
        double mindur, maxdur;  /* minimum & maximum duration of the subtitle */
        double lowerlimit, upperlimit; /* limits dictated by the neighbour subtitles */
        double avail; /* Maximum space available for the subtitle */
        
        /* Get current information */
        curstart = sub.getStartTime().toSeconds();
        curdur = sub.getFinishTime().toSeconds() - curstart;
        charcount = sub.getText().length();
        
        /* initialize minimum/maximum duration */
        mindur = maxdur = -1;
        /* Calculate minimum /maximum duration */
        if ( min_abs >= 0 ) mindur = min_abs;
        else if (min_cps >= 0 ) mindur = charcount * min_cps;
        if ( max_abs >= 0 ) maxdur = max_abs;
        else if (max_cps >= 0 ) maxdur = charcount * max_cps;
        
        /* Make sure min & max have a valid value */
        if ( mindur < 0 && maxdur < 0) {
            mindur = maxdur = curdur;
        } else if ( mindur < 0 ) {
            mindur = (curdur > maxdur) ? maxdur : curdur;
        } else if ( maxdur < 0 ) {
            maxdur = (curdur < mindur) ? mindur : curdur;
        }
        
        /* Fix duration depending on their values */
        if (curdur>maxdur) curdur = maxdur;
        if (curdur<mindur) curdur = mindur;

        /* If smart duration fix is not wanted, set duration time and exit */
        if ( !fix ) {
            /* Do not fix time */
            sub.getFinishTime().setTime(curstart+curdur);
            /* That was easy! */
            return;
        }
        
        /* The following part of the code is executed *only* when the user wanted
         to fix the subtitle times
         */
        
        /* Find limits depending on their neighbour subtitles */
        if (index==0) lowerlimit = 0;
        else lowerlimit = affected_list.elementAt(index-1).getFinishTime().toSeconds();
        if (index==(affected_list.size()-1)) upperlimit = Time.MAX_TIME;
        else upperlimit = affected_list.elementAt(index+1).getStartTime().toSeconds();
        
        /* Fix time by pushing the subtitles up */
        if (pushmodel == 2) {
            sub.getFinishTime().setTime(curstart + curdur);   /* Calculate new finish time */
            double dt = curstart + curdur + gap - upperlimit;
            if (dt > 0) {
                /* Ooops, time is not enough, we have to push everything up from now on */
                SubEntry uppersub;
                for (int i = index + 1; i < affected_list.size(); i++) {
                    uppersub = affected_list.elementAt(i);
                    uppersub.getStartTime().addTime(dt);
                    uppersub.getFinishTime().addTime(dt);
                }
            }
            return;
        }

        /* Fix time by equally divide overlapped subtitle time */
        if (pushmodel == 1) {
            if (index > 0) {
                double timesplit = (lowerlimit - curstart + gap) / 2d;
                if (timesplit > 0) {
                    sub.getStartTime().setTime(curstart + timesplit);
                    affected_list.elementAt(index - 1).getFinishTime().setTime(lowerlimit - timesplit);
                }
            }
            return;
        }


        /* Try to cleverly rearrange the subtitles */
        
        /* Available space */
        avail = upperlimit - lowerlimit;
        
        /* The real work starts here */
        if (  (curdur+2*gap) <= avail ) {
            /* We have enough space, phewwww... */
            double fulldur = curdur + 2 * gap;  /* The new full duration, i.e. subtitle + 2 gaps */
            double center = curstart + (curdur/2) ; /* Calculate the old center of the subtitle display */
            double newstart = center - (fulldur/2); /* Calculate the new *full* start of the subtitle */
            double newfinish = center + (fulldur/2);    /* Calculate the new *full* finish of the subtitle */
            double dt = 0;  /* Initialize deviation of the desired center */
            if ( newfinish > upperlimit ) dt = upperlimit - newfinish;   /* Make sure duration does not leak to the right */
            else if ( newstart < lowerlimit ) dt = lowerlimit - newstart;   /* Make sure duration does not leak to the left */
            newstart += dt + gap;   /* Calculate new start WITHOUT the gap */
            sub.getStartTime().setTime(newstart);
            sub.getFinishTime().setTime(newstart+curdur);
        } else {
            if ( mindur >= avail ) {
                /* We don't have space at all... */
                sub.getStartTime().setTime(lowerlimit);
                sub.getFinishTime().setTime(lowerlimit+avail);
            } else {
                /* We try to evenly distribute the time between the subtitles and the gap */
                double dcur = curdur - mindur;  /* The difference between current and minimum subtitle */
                double factor = (avail-mindur) / (dcur+2*gap); /* calculate normalization factor */
                double newdur = (dcur * factor) + mindur; /* Calculate new duration */
                
                double newbegin = lowerlimit;    /* Calculate new beginnig */
                if(index>0) newbegin += (gap *factor);     /* Add the gap only if it's between subtitles (to fix an error with the first subtitle)
                /* Fianlly, set net times */
                sub.getStartTime().setTime(newbegin);
                sub.getFinishTime().setTime(newbegin+newdur);
            }
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        SortB = new javax.swing.JCheckBox();
        jPanel7 = new javax.swing.JPanel();
        FixT = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        PushModelB = new javax.swing.JComboBox();
        jPanel5 = new javax.swing.JPanel();
        GapB = new javax.swing.JCheckBox();
        GapNum = new JFormattedTextField( new DecimalFormat("#######") );
        jPanel4 = new javax.swing.JPanel();
        MinTimeP = new javax.swing.JPanel();
        MaxTimeP = new javax.swing.JPanel();

        setLayout(new java.awt.BorderLayout());

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 4, 0));
        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.Y_AXIS));

        jPanel6.setLayout(new java.awt.BorderLayout());

        SortB.setSelected(true);
        SortB.setText(_("Sort first  (strongly recommended)"));
        jPanel6.add(SortB, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel6);

        jPanel7.setLayout(new java.awt.BorderLayout());

        FixT.setSelected(true);
        FixT.setText(_("Prevent overlapping"));
        FixT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FixTActionPerformed(evt);
            }
        });
        jPanel7.add(FixT, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel7);

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 20, 1, 1));
        jPanel2.setLayout(new java.awt.BorderLayout());

        PushModelB.setModel(new javax.swing.DefaultComboBoxModel(new String[] {
            _("Evenly distribute subtitles"),
            _("Equally divide overriding duration"),
            _("Shift subtitles")
        }));
        PushModelB.setToolTipText(_("Model how to solve overriding subtitles"));
        jPanel2.add(PushModelB, java.awt.BorderLayout.PAGE_END);

        jPanel3.add(jPanel2);

        jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 20, 1, 1));
        jPanel5.setLayout(new java.awt.BorderLayout());

        GapB.setText(_("Leave gap between subtitles (in milliseconds)") + "  ");
        GapB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GapBActionPerformed(evt);
            }
        });
        jPanel5.add(GapB, java.awt.BorderLayout.WEST);

        GapNum.setText("100");
        GapNum.setEnabled(false);
        jPanel5.add(GapNum, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel5);

        add(jPanel3, java.awt.BorderLayout.NORTH);

        jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        jPanel4.setLayout(new java.awt.GridLayout(1, 0));

        MinTimeP.setBorder(javax.swing.BorderFactory.createTitledBorder(_("Minimum subtitle duration")));
        MinTimeP.setLayout(new java.awt.BorderLayout());
        jPanel4.add(MinTimeP);

        MaxTimeP.setBorder(javax.swing.BorderFactory.createTitledBorder(_("Maximum subtitle duration")));
        MaxTimeP.setLayout(new java.awt.BorderLayout());
        jPanel4.add(MaxTimeP);

        add(jPanel4, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents
    
    private void GapBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GapBActionPerformed
        GapNum.setEnabled(GapB.isSelected());
    }//GEN-LAST:event_GapBActionPerformed
    
    private void FixTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FixTActionPerformed
        boolean en = FixT.isSelected();
        PushModelB.setEnabled(en);
        GapB.setEnabled(en);
        
        if ( en ) GapNum.setEnabled(GapB.isSelected());
        else GapNum.setEnabled(false);
    }//GEN-LAST:event_FixTActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox FixT;
    private javax.swing.JCheckBox GapB;
    private javax.swing.JFormattedTextField GapNum;
    private javax.swing.JPanel MaxTimeP;
    private javax.swing.JPanel MinTimeP;
    private javax.swing.JComboBox PushModelB;
    private javax.swing.JCheckBox SortB;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    // End of variables declaration//GEN-END:variables
    
}
