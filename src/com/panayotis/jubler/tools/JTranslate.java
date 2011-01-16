/*
 * JTranslate.java
 *
 * Created on 9 Ιούλιος 2005, 11:20 πμ
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

import com.google.api.translate.Language;
import com.panayotis.jubler.events.menu.tool.translate.GoogleTranslationDirect;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import static com.panayotis.jubler.i18n.I18N._;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;

/**
 *
 * @author  teras
 */
public class JTranslate extends JTool {

    public static Map<Language, String> languageMap = new HashMap<Language, String>();
    public static Map<String, Language> languageReversedMap = new HashMap<String, Language>();
    public static Vector<String> languageNames = new Vector<String>();

    //private static AvailTranslators translators;


    static {
        //translators = new AvailTranslators();

        /**
         * This routine loads the value pairs
         * [language code (3 characters), language display name] of every languages
         * on Earth as defined ISO-639
         */
        String[] languages = Locale.getISOLanguages();
        for (String lang : languages) {
            Language lang_entry = Language.fromString(lang);
            Locale loc = new Locale(lang);
            String lang_name = loc.getDisplayLanguage();
            languageNames.add(lang_name);
            languageMap.put(lang_entry, lang_name);
            languageReversedMap.put(lang_name, lang_entry);
        }//end for(String language : languages)
    }//end static

    /** Creates new form JRounder */
    public JTranslate() {
        super(true);
    }

    public void initialize() {
        initComponents();
        Vector<String> machines = new Vector<String>();
        machines.add(_("Google Translation (API 0.94)"));
        TransMachine.setModel(new DefaultComboBoxModel(machines));

        FromLang.setModel(new DefaultComboBoxModel(languageNames));
        FromLang.setSelectedItem(languageMap.get(Language.ENGLISH));

        ToLang.setModel(new DefaultComboBoxModel(languageNames));
        ToLang.setSelectedItem(languageMap.get(Language.VIETNAMESE));
    }

    protected String getToolTitle() {
        return _("Translate text");
    }

    protected void storeSelections() {
    }

    protected void affect(int index) {
    }

    public Language getFromLanguage() {
        String lang_name = (String) FromLang.getSelectedItem();
        Language from_language = languageReversedMap.get(lang_name);
        return from_language;
    }

    public Language getToLanguage() {
        String lang_name = (String) ToLang.getSelectedItem();
        Language to_language = languageReversedMap.get(lang_name);
        return to_language;
    }

    public boolean isReplaceCurrent() {
        boolean is_replace = this.ReplaceCurrentText.isSelected();
        return is_replace;
    }

    protected boolean finalizing() {
        //return trans.translate(affected_list, FromLang.getSelectedItem().toString(), ToLang.getSelectedItem().toString());
        return performTranslation(affected_list);
    }

    public boolean performTranslation(SubEntry[] subs) {
        try{
            Vector<SubEntry> affected_list = new Vector<SubEntry>();
            for(int i=0; i < subs.length; i++){
                affected_list.add(subs[i]);
            }//end for
            return performTranslation(affected_list);
        }catch(Exception ex){
            DEBUG.debug(ex.toString());
            return false;
        }        
    }
    
    public boolean performTranslation(Vector<SubEntry> subs) {
        boolean result = false;
        try {

            Language from_language = this.getFromLanguage();
            Language to_language = this.getToLanguage();

            boolean is_same = (from_language.toString().equals(to_language.toString()));
            if (is_same) {
                throw new RuntimeException(_("It is not logical to perform translation from the same set of languages"));
            }

            boolean is_replace = this.isReplaceCurrent();

            GoogleTranslationDirect tran = new GoogleTranslationDirect();
            tran.setAffectedList(subs);
            tran.setFromLanguage(from_language);
            tran.setToLanguage(to_language);
            tran.setReplaceCurrentText(is_replace);
            result = tran.performTranslation();
        } catch (Exception ex) {
            DEBUG.debug(ex.toString());
        }
        return result;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        TransMachine = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        ReplaceCurrentText = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        FromLang = new javax.swing.JComboBox();
        jPanel5 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        ToLang = new javax.swing.JComboBox();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        setLayout(new java.awt.BorderLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel2.setLayout(new java.awt.BorderLayout());

        jLabel1.setText(_("Translator"));
        jPanel2.add(jLabel1, java.awt.BorderLayout.WEST);

        TransMachine.setToolTipText(_("Selection of translation machine"));
        TransMachine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TransMachineActionPerformed(evt);
            }
        });
        jPanel2.add(TransMachine, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel2, java.awt.BorderLayout.NORTH);

        jPanel3.setLayout(new java.awt.GridLayout(1, 3));

        jPanel7.setLayout(new java.awt.BorderLayout());

        ReplaceCurrentText.setText(_("Replace Text"));
        ReplaceCurrentText.setToolTipText(_("Replace current text with translated text."));
        jPanel7.add(ReplaceCurrentText, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel7);

        jPanel4.setLayout(new java.awt.BorderLayout());

        jLabel2.setText(_("From"));
        jPanel4.add(jLabel2, java.awt.BorderLayout.CENTER);

        FromLang.setToolTipText(_("Original language"));
        jPanel4.add(FromLang, java.awt.BorderLayout.PAGE_END);

        jPanel3.add(jPanel4);

        jPanel5.setLayout(new java.awt.BorderLayout());

        jLabel3.setText(_("To"));
        jPanel5.add(jLabel3, java.awt.BorderLayout.CENTER);

        ToLang.setToolTipText(_("Target language"));
        jPanel5.add(ToLang, java.awt.BorderLayout.PAGE_END);

        jPanel3.add(jPanel5);

        jPanel1.add(jPanel3, java.awt.BorderLayout.CENTER);

        jPanel6.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        jTextArea1.setBackground(javax.swing.UIManager.getDefaults().getColor("Label.background"));
        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setFont(jTextArea1.getFont().deriveFont((jTextArea1.getFont().getStyle() | java.awt.Font.ITALIC)));
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(2);
        jTextArea1.setText(_("Computer Translated subtitles should be only for personal use, and not for distribution."));
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane1.setViewportView(jTextArea1);

        jPanel6.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel6, java.awt.BorderLayout.PAGE_END);

        add(jPanel1, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

private void TransMachineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransMachineActionPerformed
    //trans = translators.get(TransMachine.getSelectedIndex());
}//GEN-LAST:event_TransMachineActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox FromLang;
    private javax.swing.JCheckBox ReplaceCurrentText;
    private javax.swing.JComboBox ToLang;
    private javax.swing.JComboBox TransMachine;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables
}
