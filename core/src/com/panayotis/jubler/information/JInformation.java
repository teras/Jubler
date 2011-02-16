/*
 * JInformation.java
 *
 * Created on 26 Αύγουστος 2005, 5:42 μμ
 *
 * This file is part of JubFrame.
 *
 * JubFrame is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * JubFrame is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JubFrame; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.panayotis.jubler.information;

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.subs.SubAttribs;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.TotalSubMetrics;
import java.awt.BorderLayout;
import javax.swing.JComboBox;

import javax.swing.JDialog;

/**
 *
 * @author  teras
 */
public class JInformation extends JDialog {
    private Subtitles subs;
    private MediaFile media;

    private static final String[] CTTypesData = {_("per line"), _("per second")};
    
    /** Creates new form JProperties */
    public JInformation(JubFrame parent) {
        super(parent, true);
        
        subs = parent.getSubtitles();
        media = parent.getMediaFile();
        
        initComponents();
        MaxColC.removeItemAt(0);
        
        SubAttribs attr = parent.getSubtitles().getAttribs();
        TitleT.setText(attr.getTitle());
        AuthorT.setText(attr.getAuthor());
        SourceT.setText(attr.getSource());
        CommentsT.setText(attr.getComments());
        FilePathT.setText(parent.getSubtitles().getSubFile().getStrippedFile().getPath());
        
        NumberT.setText(Integer.toString(parent.getSubtitles().size()));
        TotalSubMetrics m = parent.getSubtitles().getTotalMetrics();
        TotalSubSizeT.setText(Integer.toString(m.totallength));
        TotalLinesT.setText(Integer.toString(m.totallines));
        MaxSubSizeT.setText(Integer.toString(m.length));
        MaxLinesT.setText(Integer.toString(m.lines));
        MaxLengthT.setText(Integer.toString(m.maxlength));
        
        VSelectorP.add(parent.getMediaFile().videoselector, BorderLayout.CENTER);
        
        MaxInfUserB.setSelected(attr.isMaxCharsEnabled());
        updateMaxCharsWidgets();
        MaxColC.setSelectedIndex(attr.getMaxColor()-1);
        MaxCharsS.setValue(attr.getMaxCharacters());
        CPType.setSelectedIndex(attr.isMaxCPS()?1:0);

        pack();
        setLocationRelativeTo(null);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        PTabs = new javax.swing.JTabbedPane();
        InfoP = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        CommentsT = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        TitleL = new javax.swing.JLabel();
        AuthorL = new javax.swing.JLabel();
        SourceL = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        TitleT = new javax.swing.JTextField();
        AuthorT = new javax.swing.JTextField();
        SourceT = new javax.swing.JTextField();
        MediaP = new javax.swing.JPanel();
        VSelectorP = new javax.swing.JPanel();
        SubFileInfoP = new javax.swing.JPanel();
        FilePathL = new javax.swing.JLabel();
        FilePathT = new javax.swing.JTextField();
        StatsP = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        NumberL = new javax.swing.JLabel();
        NumberT = new javax.swing.JLabel();
        TotalSubSizeL = new javax.swing.JLabel();
        TotalSubSizeT = new javax.swing.JLabel();
        TotalLinesL = new javax.swing.JLabel();
        TotalLinesT = new javax.swing.JLabel();
        MaxSubSizeL = new javax.swing.JLabel();
        MaxSubSizeT = new javax.swing.JLabel();
        MaxLinesL = new javax.swing.JLabel();
        MaxLinesT = new javax.swing.JLabel();
        MaxLengthL = new javax.swing.JLabel();
        MaxLengthT = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        MaxInfUserB = new javax.swing.JCheckBox();
        jPanel11 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        MaxColL = new javax.swing.JLabel();
        MaxColC = new javax.swing.JComboBox();
        jPanel12 = new javax.swing.JPanel();
        MaxCharsS = new javax.swing.JSlider();
        jPanel7 = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        CPType = new JComboBox(CTTypesData);
        MaxCharsL = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        OKB = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(_("Project Properties"));
        setResizable(false);

        PTabs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                PTabsStateChanged(evt);
            }
        });

        InfoP.setName("info"); // NOI18N
        InfoP.setOpaque(false);
        InfoP.setLayout(new java.awt.BorderLayout());

        jPanel2.setBorder(SystemDependent.getBorder(_("Comments")));
        jPanel2.setOpaque(false);
        jPanel2.setPreferredSize(new java.awt.Dimension(350, 150));
        jPanel2.setLayout(new java.awt.BorderLayout());

        CommentsT.setToolTipText(_("Comments about these subtitles"));
        jScrollPane1.setViewportView(CommentsT);

        jPanel2.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        InfoP.add(jPanel2, java.awt.BorderLayout.CENTER);

        jPanel1.setOpaque(false);
        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
        jPanel4.setOpaque(false);
        jPanel4.setLayout(new java.awt.GridLayout(0, 1));

        TitleL.setText(_("Title"));
        jPanel4.add(TitleL);

        AuthorL.setText(_("Author"));
        jPanel4.add(AuthorL);

        SourceL.setText(_("Source"));
        jPanel4.add(SourceL);

        jPanel1.add(jPanel4, java.awt.BorderLayout.WEST);

        jPanel3.setOpaque(false);
        jPanel3.setLayout(new java.awt.GridLayout(0, 1));

        TitleT.setToolTipText(_("Title for this subtitle file"));
        jPanel3.add(TitleT);

        AuthorT.setToolTipText(_("Author of this subtitle file"));
        jPanel3.add(AuthorT);

        SourceT.setToolTipText(_("Original source of this subtitle file"));
        jPanel3.add(SourceT);

        jPanel1.add(jPanel3, java.awt.BorderLayout.CENTER);

        InfoP.add(jPanel1, java.awt.BorderLayout.NORTH);

        PTabs.addTab(_("Information"), InfoP);

        MediaP.setName("media"); // NOI18N
        MediaP.setOpaque(false);
        MediaP.setLayout(new java.awt.BorderLayout());

        VSelectorP.setOpaque(false);
        VSelectorP.setLayout(new java.awt.BorderLayout());

        SubFileInfoP.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 8, 0));
        SubFileInfoP.setOpaque(false);
        SubFileInfoP.setLayout(new java.awt.BorderLayout());

        FilePathL.setText(_("Subtitle File"));
        SubFileInfoP.add(FilePathL, java.awt.BorderLayout.WEST);

        FilePathT.setEditable(false);
        FilePathT.setToolTipText(_("The file of this subtitle"));
        SubFileInfoP.add(FilePathT, java.awt.BorderLayout.SOUTH);

        VSelectorP.add(SubFileInfoP, java.awt.BorderLayout.NORTH);

        MediaP.add(VSelectorP, java.awt.BorderLayout.NORTH);

        PTabs.addTab(_("Media"), MediaP);

        StatsP.setName("stats"); // NOI18N
        StatsP.setOpaque(false);
        StatsP.setLayout(new java.awt.BorderLayout());

        jPanel9.setOpaque(false);
        jPanel9.setLayout(new java.awt.BorderLayout());

        jPanel8.setOpaque(false);
        jPanel8.setLayout(new java.awt.GridLayout(0, 2, 0, 4));

        NumberL.setText(_("Number of subtitles"));
        jPanel8.add(NumberL);
        jPanel8.add(NumberT);

        TotalSubSizeL.setText(_("Total subtitle characters"));
        jPanel8.add(TotalSubSizeL);
        jPanel8.add(TotalSubSizeT);

        TotalLinesL.setText(_("Total subtitle lines"));
        jPanel8.add(TotalLinesL);
        jPanel8.add(TotalLinesT);

        MaxSubSizeL.setText(_("Maximum subtitle length"));
        jPanel8.add(MaxSubSizeL);
        jPanel8.add(MaxSubSizeT);

        MaxLinesL.setText(_("Maximum subtitle lines"));
        jPanel8.add(MaxLinesL);
        jPanel8.add(MaxLinesT);

        MaxLengthL.setText(_("Maximum subtitle characters per line"));
        jPanel8.add(MaxLengthL);
        jPanel8.add(MaxLengthT);

        jPanel9.add(jPanel8, java.awt.BorderLayout.NORTH);

        jPanel10.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 1, 0, 1));
        jPanel10.setOpaque(false);
        jPanel10.setLayout(new java.awt.BorderLayout());

        MaxInfUserB.setSelected(true);
        MaxInfUserB.setText(_("Inform user on exceeding subtitle length"));
        MaxInfUserB.setMargin(new java.awt.Insets(0, 0, 0, 0));
        MaxInfUserB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MaxInfUserBActionPerformed(evt);
            }
        });
        jPanel10.add(MaxInfUserB, java.awt.BorderLayout.NORTH);

        jPanel11.setOpaque(false);
        jPanel11.setLayout(new java.awt.BorderLayout());

        jPanel13.setOpaque(false);
        jPanel13.setLayout(new java.awt.BorderLayout());

        MaxColL.setText(_("Color to use"));
        MaxColL.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 12));
        jPanel13.add(MaxColL, java.awt.BorderLayout.WEST);

        MaxColC.setModel(new javax.swing.DefaultComboBoxModel(SubEntry.MarkNames));
        jPanel13.add(MaxColC, java.awt.BorderLayout.CENTER);

        jPanel11.add(jPanel13, java.awt.BorderLayout.WEST);

        jPanel10.add(jPanel11, java.awt.BorderLayout.CENTER);

        jPanel12.setOpaque(false);
        jPanel12.setLayout(new java.awt.BorderLayout());

        MaxCharsS.setMajorTickSpacing(10);
        MaxCharsS.setMinimum(10);
        MaxCharsS.setMinorTickSpacing(1);
        MaxCharsS.setPaintLabels(true);
        MaxCharsS.setPaintTicks(true);
        MaxCharsS.setSnapToTicks(true);
        MaxCharsS.setToolTipText(_("The maximum number of characters per line which are permitted"));
        MaxCharsS.setValue(40);
        jPanel12.add(MaxCharsS, java.awt.BorderLayout.SOUTH);

        jPanel7.setOpaque(false);
        jPanel7.setLayout(new java.awt.BorderLayout());

        jPanel14.setOpaque(false);
        jPanel14.setLayout(new java.awt.BorderLayout());
        jPanel14.add(CPType, java.awt.BorderLayout.CENTER);

        MaxCharsL.setText(_("Maximum number of characters:"));
        jPanel14.add(MaxCharsL, java.awt.BorderLayout.WEST);

        jPanel7.add(jPanel14, java.awt.BorderLayout.WEST);

        jPanel12.add(jPanel7, java.awt.BorderLayout.NORTH);

        jPanel10.add(jPanel12, java.awt.BorderLayout.SOUTH);

        jPanel9.add(jPanel10, java.awt.BorderLayout.CENTER);

        StatsP.add(jPanel9, java.awt.BorderLayout.NORTH);

        PTabs.addTab(_("Statistics"), StatsP);

        getContentPane().add(PTabs, java.awt.BorderLayout.CENTER);

        jPanel5.setOpaque(false);
        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel6.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 6, 16));
        jPanel6.setOpaque(false);
        jPanel6.setLayout(new java.awt.GridLayout(1, 2));

        OKB.setText(_("OK"));
        OKB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OKBActionPerformed(evt);
            }
        });
        jPanel6.add(OKB);

        jPanel5.add(jPanel6, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel5, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void PTabsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_PTabsStateChanged
        if (PTabs.getSelectedComponent().getName().equals("media")) {
            media.guessMediaFiles(subs);
        }
    }//GEN-LAST:event_PTabsStateChanged
    
    private void MaxInfUserBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MaxInfUserBActionPerformed
        updateMaxCharsWidgets();
    }//GEN-LAST:event_MaxInfUserBActionPerformed
    
    private void OKBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OKBActionPerformed
        setVisible(false);
    }//GEN-LAST:event_OKBActionPerformed
    
    public SubAttribs getAttribs() {
        return new SubAttribs(TitleT.getText(), AuthorT.getText(), SourceT.getText(), CommentsT.getText(),
                MaxInfUserB.isSelected() ? MaxCharsS.getValue() : - MaxCharsS.getValue(),
                MaxColC.getSelectedIndex()+1, CPType.getSelectedIndex()==1 );
    }
    
    
    private void updateMaxCharsWidgets() {
        boolean status = MaxInfUserB.isSelected();
        MaxColC.setEnabled(status);
        MaxColL.setEnabled(status);
        MaxCharsS.setEnabled(status);
        MaxCharsL.setEnabled(status);
        CPType.setEnabled(status);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel AuthorL;
    private javax.swing.JTextField AuthorT;
    private javax.swing.JComboBox CPType;
    private javax.swing.JTextArea CommentsT;
    private javax.swing.JLabel FilePathL;
    private javax.swing.JTextField FilePathT;
    private javax.swing.JPanel InfoP;
    private javax.swing.JLabel MaxCharsL;
    private javax.swing.JSlider MaxCharsS;
    private javax.swing.JComboBox MaxColC;
    private javax.swing.JLabel MaxColL;
    private javax.swing.JCheckBox MaxInfUserB;
    private javax.swing.JLabel MaxLengthL;
    private javax.swing.JLabel MaxLengthT;
    private javax.swing.JLabel MaxLinesL;
    private javax.swing.JLabel MaxLinesT;
    private javax.swing.JLabel MaxSubSizeL;
    private javax.swing.JLabel MaxSubSizeT;
    private javax.swing.JPanel MediaP;
    private javax.swing.JLabel NumberL;
    private javax.swing.JLabel NumberT;
    private javax.swing.JButton OKB;
    private javax.swing.JTabbedPane PTabs;
    private javax.swing.JLabel SourceL;
    private javax.swing.JTextField SourceT;
    private javax.swing.JPanel StatsP;
    private javax.swing.JPanel SubFileInfoP;
    private javax.swing.JLabel TitleL;
    private javax.swing.JTextField TitleT;
    private javax.swing.JLabel TotalLinesL;
    private javax.swing.JLabel TotalLinesT;
    private javax.swing.JLabel TotalSubSizeL;
    private javax.swing.JLabel TotalSubSizeT;
    private javax.swing.JPanel VSelectorP;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
    
}
