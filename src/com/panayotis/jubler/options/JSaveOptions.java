/*
 * JSavePrefs.java
 *
 * Created on 23 Ιούνιος 2005, 2:32 μμ
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
package com.panayotis.jubler.options;

import com.panayotis.jubler.options.gui.JRateChooser;
import com.panayotis.jubler.subs.loader.AvailSubFormats;
import com.panayotis.jubler.subs.loader.SubFormat;
import java.awt.BorderLayout;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *
 * @author  teras
 */
public class JSaveOptions extends JFileOptions {

    private String enc_state,  fps_state,  format_state;
    private JRateChooser CFPS;
    private boolean remindAgain = true;
    private SubFormat selectedFormat = null;

    /** Creates new form JSavePrefs */
    public JSaveOptions() {
        super();
        initComponents();

        setRemindAgain(remindAgain);
        /* Fix DialogVisible */
        addDialogOption();
        updateDialogOption(_(" Show save preferences while saving file"), _("Show preferences every time the user saves a subtitle file"));

        CFPS = new JRateChooser();

        FPSPanel.add(CFPS, BorderLayout.CENTER);
        fillComponents();
        CFormat.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                setSelectedFormat(AvailSubFormats.findFromDescription(CFormat.getSelectedItem().toString()));
            }
        });

        addComponentListener(new ComponentAdapter() {

            public void componentShown(ComponentEvent e) {
                //DEBUG.debug("componentShown");
                setFormat(getSelectedFormat());
            }

            /**
             * Invoked when the component's size changes.
             */
            public void componentResized(ComponentEvent e) {
                //DEBUG.debug("componentResized");
                setFormat(getSelectedFormat());
            }
            /**
             * Invoked when the component has been made invisible.
             */
            /*
            public void componentHidden(ComponentEvent e) {
            //DEBUG.debug("componentHidden");
            setSelectedFormat(null);
            }
             *
             */
        });
    }

    public void updateVisuals(MediaFile mfile, Subtitles subs) {
        CFPS.setDataFiles(mfile, subs);
        updateVisualFPS(null);  // get the current SubFormat
    }

    private void fillComponents() {
        int i;

        for (i = 0; i < AvailSubFormats.Formats.length; i++) {
            CFormat.addItem(AvailSubFormats.Formats[i].getDescription());
        }
    }

    public float getFPS() {
        return CFPS.getFPSValue();
    }

    public String getEncoding() {
        return CEnc.getSelectedItem().toString();
    }

    public SubFormat getFormat() {
        boolean is_selected_format_set = (getSelectedFormat() != null);
        if (is_selected_format_set) {
            return this.getSelectedFormat();
        } else {
            return AvailSubFormats.findFromDescription(CFormat.getSelectedItem().toString());
        }//end if/else
    }//end public SubFormat getFormat()

    public void setFormat(int index) {
        CFormat.setSelectedIndex(index);
    }

    public void setFormat(SubFormat format) {
        try {
            setFormatUsingNameAndExtension(format.getName(), format.getExtension());
        } catch (Exception ex) {
            DEBUG.debug(ex.toString());
        }
    }

    public void setFormatUsingNameAndExtension(String format_name, String format_ext) {
        try {
            //DEBUG.debug("setFormatUsingNameAndExtension: " + format_name + " ext: " + format_ext);
            int found_index = AvailSubFormats.findFromNameAndExtension(format_name, format_ext);
            boolean is_found = (found_index >= 0);
            //DEBUG.debug("setFormatUsingNameAndExtension, is_found: " + is_found);
            if (is_found) {
                CFormat.setSelectedIndex(found_index);
            }
        } catch (Exception ex) {
            DEBUG.debug(ex.toString());
        }
    }

    public void savePreferences() {
        Options.setOption("Save.Encoding", CEnc.getSelectedItem().toString());
        Options.setOption("Save.FPS", CFPS.getFPS());
        Options.setOption("System.ShowSaveDialog", getDialogOption());

        SubFormat f = AvailSubFormats.findFromDescription(CFormat.getSelectedItem().toString());
        selectedFormat = f;
        Options.setOption("Save.Format", (f != null) ? f.getName() : "UNKNOWN");
    }

    public void loadPreferences() {
        String enc, fps, format;

        enc = Options.getOption("Save.Encoding", JPreferences.DefaultEncodings[0]);
        fps = Options.getOption("Save.FPS", JRateChooser.DefaultFPSEntry);
        format = Options.getOption("Save.Format", AvailSubFormats.Formats[0].getName());

        setListItem(CEnc, enc);
        CFPS.setFPS(fps);

        SubFormat f = AvailSubFormats.findFromName(format);
        selectedFormat = f;
        setCombo(CFormat, (f != null) ? f.getDescription() : "UNKNOWN", "UNKNOWN");
        updateVisualFPS(f);

        setDialogOption(Options.getOption("System.ShowSaveDialog", "true").equals("true"));
    }

    public String getTabName() {
        return _("Save");
    }

    public String getTabTooltip() {
        return _("Save subtitles options");
    }

    public Icon getTabIcon() {
        return new ImageIcon(getClass().getResource("/icons/save_small.png"));
    }

    /* Execute this method whenever the output format is changed (or this panel is displayed */
    private void updateVisualFPS(SubFormat f) {
        if (f == null) {
            f = getFormat();
        }
        boolean supports_fps = f.supportsFPS();
        FPSPanelL.setVisible(supports_fps);
        FPSPanel.setVisible(supports_fps);
    }

    public void setPreEncoding(String enc) {
        setListItem(CEnc, enc);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        OptsP = new javax.swing.JPanel();
        CFormatL = new javax.swing.JLabel();
        CFormat = new javax.swing.JComboBox();
        CEncL = new javax.swing.JLabel();
        CEnc = new javax.swing.JComboBox(AvailEncodings);
        FPSPanelL = new javax.swing.JLabel();
        FPSPanel = new javax.swing.JPanel();
        RemindAgainL = new javax.swing.JLabel();
        chkRemindAgain = new javax.swing.JCheckBox();

        setLayout(new java.awt.BorderLayout());

        OptsP.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 4, 0));
        OptsP.setLayout(new java.awt.GridLayout(4, 2));

        CFormatL.setText(_("Format"));
        OptsP.add(CFormatL);

        CFormat.setToolTipText(_("Subtitle format of the output file (SRT is prefered)"));
        CFormat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CFormatActionPerformed(evt);
            }
        });
        OptsP.add(CFormat);

        CEncL.setText(_("Encoding"));
        OptsP.add(CEncL);
        OptsP.add(CEnc);
        OptsP.add(CEnc);

        FPSPanelL.setText(_("FPS"));
        OptsP.add(FPSPanelL);

        FPSPanel.setLayout(new java.awt.BorderLayout());
        OptsP.add(FPSPanel);

        RemindAgainL.setText(_("Change setting"));
        OptsP.add(RemindAgainL);

        chkRemindAgain.setText(_("Remind again"));
        chkRemindAgain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkRemindAgainActionPerformed(evt);
            }
        });
        OptsP.add(chkRemindAgain);

        add(OptsP, java.awt.BorderLayout.NORTH);
    }// </editor-fold>//GEN-END:initComponents

    private void CFormatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CFormatActionPerformed
        updateVisualFPS(null);
    }//GEN-LAST:event_CFormatActionPerformed

    private void chkRemindAgainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkRemindAgainActionPerformed
        remindAgain = chkRemindAgain.isSelected();
    }//GEN-LAST:event_chkRemindAgainActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox CEnc;
    private javax.swing.JLabel CEncL;
    private javax.swing.JComboBox CFormat;
    private javax.swing.JLabel CFormatL;
    private javax.swing.JPanel FPSPanel;
    private javax.swing.JLabel FPSPanelL;
    private javax.swing.JPanel OptsP;
    private javax.swing.JLabel RemindAgainL;
    private javax.swing.JCheckBox chkRemindAgain;
    // End of variables declaration//GEN-END:variables

    /**
     * @return the remindAgain
     */
    public boolean isRemindAgain() {
        return remindAgain;
    }

    /**
     * @param remindAgain the remindAgain to set
     */
    public void setRemindAgain(boolean remindAgain) {
        this.remindAgain = remindAgain;
        chkRemindAgain.setSelected(remindAgain);
    }

    /**
     * @return the selectedFormat
     */
    public SubFormat getSelectedFormat() {
        return selectedFormat;
    }

    /**
     * @param selectedFormat the selectedFormat to set
     */
    public void setSelectedFormat(SubFormat selectedFormat) {
        this.selectedFormat = selectedFormat;
    }
}
