/*
 * JPrefsFrame.java
 *
 * Created on 3 Ιούλιος 2005, 2:45 μμ
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

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.JIDialog;
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.format.SubFormat;
import java.awt.BorderLayout;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Properties;
import java.util.SortedMap;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 *
 * @author  teras
 */
public class JPreferences extends JPanel implements OptionsHolder {
    
    public static final String []AvailEncodings;
    public static final String []DefaultEncodings;
    
    public JLoadOptions jload;
    public JSaveOptions jsave;
    public ShortcutsModel smodel;
    
    private boolean load_state, save_state;
    
    static {
        SortedMap encs = Charset.availableCharsets();
        AvailEncodings = new String[encs.size()];
        int pos;
        String item;
        
        pos = 0;
        for (Iterator it = encs.keySet().iterator() ; it.hasNext() ; ) {
            item = it.next().toString();
            AvailEncodings[pos++] = item;
        }
        
        DefaultEncodings = new String[3];
        DefaultEncodings[0] = "ISO-8859-1";
        DefaultEncodings[1] = "UTF-8";
        DefaultEncodings[2] = "UTF-16";
    }
    
    
    /** Creates new form JPreferences */
    public JPreferences(Jubler jub) {
        smodel = new ShortcutsModel(jub.JublerMenuBar);
        
        initComponents();
        jload = new JLoadOptions();
        jsave = new JSaveOptions();
        OptionsIO.loadSystemPreferences(this);
        
        LoadPanel.add(jload, BorderLayout.NORTH);
        SavePanel.add(jsave, BorderLayout.NORTH);
        
        ShortT.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                //Ignore extra messages.
                if (e.getValueIsAdjusting()) return;
                ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                if (lsm.isSelectionEmpty()) return;
                smodel.setSelection(ShortT.getSelectedRow());
            }
        });
        ShortT.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
    
    
    /* Various methods to get the preferences to the othe components */
    public float getLoadFPS() {
        return jload.getFPS();
    }
    public String[] getLoadEncodings() {
        return jload.getEncodings();
    }
    public String getSaveEncoding() {
        return jsave.getEncoding();
    }
    public float getSaveFPS() {
        return jsave.getFPS();
    }
    public SubFormat getSaveFormat() {
        return jsave.getFormat();
    }
    
    
    /* The following two methods display the load/save panels in a dialog
     * and reattach them back to their position, after the selection was done.
     */
    public void showLoadDialog(JFrame parent) {
        flipflopPanel(parent, jload, LoadPanel, LSelect);
    }
    public void showSaveDialog(JFrame parent) {
        flipflopPanel(parent, jsave, SavePanel, SSelect);
    }
    
    private void flipflopPanel(JFrame parent, JOptionsGUI obj, JPanel container, JCheckBox allow) {
        if ( !allow.isSelected() ) return;
        
        JIDialog.message(parent, obj, _("File preferences"), JIDialog.QUESTION_MESSAGE);
        container.add(obj, BorderLayout.NORTH);
    }
    
    
    /* Save preferences stored in this frame */
    public void savePreferences(Properties props) {
        props.setProperty("System.ShowLoadDialog", LSelect.isSelected() ? "true" : "false");
        props.setProperty("System.ShowSaveDialog", SSelect.isSelected() ? "true" : "false");
    }
    
    /* Save preferences stored in this frame */
    public void loadPreferences(Properties props) {
        LSelect.setSelected(props.getProperty("System.ShowLoadDialog", "true").equals("true"));
        SSelect.setSelected(props.getProperty("System.ShowSaveDialog", "true").equals("true"));
    }
    
    public void saveState() {
        /* Private objects */
        load_state = LSelect.isSelected();
        save_state = SSelect.isSelected();
        
        /* Other objects */
        jload.saveState();
        jsave.saveState();
        smodel.saveState();
        
    }
    
    public void restoreState() {
        LSelect.setSelected(load_state);
        SSelect.setSelected(save_state);
        
        /* Other objects */
        jload.restoreState();
        jsave.restoreState();
        smodel.restoreState();
    }
    
    public void setMenuShortcuts(JMenuBar bar) {
        smodel.applyMenuShortcuts(bar);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        Tabs = new javax.swing.JTabbedPane();
        LoadPanel = new javax.swing.JPanel();
        LSelect = new javax.swing.JCheckBox();
        SavePanel = new javax.swing.JPanel();
        SSelect = new javax.swing.JCheckBox();
        ShortcutsPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        ShortT = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        ClearSB = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        ResetSB = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        LoadPanel.setLayout(new java.awt.BorderLayout());

        LSelect.setText(_(" Show load preferences while loading file"));
        LSelect.setToolTipText(_("Show preferences every time the user loads a subtitle file"));
        LoadPanel.add(LSelect, java.awt.BorderLayout.SOUTH);

        Tabs.addTab(_("Load"), LoadPanel);

        SavePanel.setLayout(new java.awt.BorderLayout());

        SSelect.setText(_(" Show save preferences while saving file"));
        SSelect.setToolTipText(_("Show preferences every time the user loads a subtitle file"));
        SavePanel.add(SSelect, java.awt.BorderLayout.SOUTH);

        Tabs.addTab(_("Save"), SavePanel);

        ShortcutsPanel.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setPreferredSize(new java.awt.Dimension(200, 200));
        ShortT.setModel(smodel);
        ShortT.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                ShortTKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                ShortTKeyReleased(evt);
            }
        });

        jScrollPane1.setViewportView(ShortT);

        ShortcutsPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel2.setLayout(new java.awt.BorderLayout());

        ClearSB.setText(_("Clear current shortcut"));
        ClearSB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClearSBActionPerformed(evt);
            }
        });

        jPanel3.add(ClearSB);

        jPanel2.add(jPanel3, java.awt.BorderLayout.EAST);

        ResetSB.setText(_("Reset all to defaults"));
        ResetSB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetSBActionPerformed(evt);
            }
        });

        jPanel4.add(ResetSB);

        jPanel2.add(jPanel4, java.awt.BorderLayout.WEST);

        ShortcutsPanel.add(jPanel2, java.awt.BorderLayout.SOUTH);

        Tabs.addTab(_("Shortcuts"), ShortcutsPanel);

        add(Tabs, java.awt.BorderLayout.CENTER);

    }// </editor-fold>//GEN-END:initComponents

    private void ClearSBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClearSBActionPerformed
        smodel.removeShortcut();
    }//GEN-LAST:event_ClearSBActionPerformed

    private void ResetSBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResetSBActionPerformed
        smodel.resetAllShortcuts();
    }//GEN-LAST:event_ResetSBActionPerformed
    
    private void ShortTKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_ShortTKeyReleased
        smodel.keyReleased(evt.getKeyCode());
    }//GEN-LAST:event_ShortTKeyReleased
    
    private void ShortTKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_ShortTKeyPressed
        smodel.keyPressed(evt.getKeyCode());
    }//GEN-LAST:event_ShortTKeyPressed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ClearSB;
    private javax.swing.JCheckBox LSelect;
    private javax.swing.JPanel LoadPanel;
    private javax.swing.JButton ResetSB;
    private javax.swing.JCheckBox SSelect;
    private javax.swing.JPanel SavePanel;
    private javax.swing.JTable ShortT;
    private javax.swing.JPanel ShortcutsPanel;
    private javax.swing.JTabbedPane Tabs;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
    
}
