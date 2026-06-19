/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.appenh.ThemeVariation;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.os.UIUtils;
import com.panayotis.jubler.theme.Theme;

import javax.swing.*;

import static com.panayotis.jubler.i18n.I18N.__;

public class JUiOptions extends JPanel implements OptionsHolder {
    private float oldScaling = Float.POSITIVE_INFINITY;
    private boolean oldTooltipsDisabled;
    private ThemeVariation oldThemeVariation;
    private String oldLanguage;

    /**
     * Creates new form JExternalToolsOptions
     */
    public JUiOptions() {
        initComponents();
        for (ThemeVariation v : ThemeVariation.values())
            themesC.addItem(v);
        
        languageC.addItem(new LanguageOption("auto", __("Automatic"), "flag-global"));
        languageC.insertItemAt(null, 1);
        languageC.addItem(new LanguageOption("cs", "Čeština", "flag-cs"));
        languageC.addItem(new LanguageOption("de", "Deutsch", "flag-de"));
        languageC.addItem(new LanguageOption("el", "Ελληνικά", "flag-el"));
        languageC.addItem(new LanguageOption("en", "English", "flag-en"));
        languageC.addItem(new LanguageOption("es", "Español", "flag-es"));
        languageC.addItem(new LanguageOption("fr", "Français", "flag-fr"));
        languageC.addItem(new LanguageOption("it", "Italiano", "flag-it"));
        languageC.addItem(new LanguageOption("nl", "Nederlands", "flag-nl"));
        languageC.addItem(new LanguageOption("pt", "Português", "flag-pt"));
        languageC.addItem(new LanguageOption("sr", "Српски", "flag-sr"));
        languageC.addItem(new LanguageOption("tr", "Türkçe", "flag-tr"));
        
        languageC.setRenderer(new LanguageCellRenderer());
        
        languageC.addActionListener(e -> {
            if (languageC.getSelectedItem() == null) {
                for (int i = 0; i < languageC.getItemCount(); i++) {
                    if (languageC.getItemAt(i) != null) {
                        languageC.setSelectedIndex(i);
                        break;
                    }
                }
            }
        });
        
        if (!SystemDependent.shouldSupportChangeScaling()) {
            layoutP.remove(scalingP);
            layoutP.remove(scalingF);
        }
        loadPreferences();
    }

    @Override
    public void loadPreferences() {
        oldTooltipsDisabled = Options.isTimestampTooltipsDisabled();
        oldThemeVariation = Options.getThemeVariation();
        oldLanguage = Options.getLanguage();

        tooltipsC.setSelected(oldTooltipsDisabled);
        themesC.setSelectedItem(oldThemeVariation);
        
        for (int i = 0; i < languageC.getItemCount(); i++) {
            LanguageOption option = languageC.getItemAt(i);
            if (option != null && option.getCode().equals(oldLanguage)) {
                languageC.setSelectedIndex(i);
                break;
            }
        }

        if (SystemDependent.shouldSupportChangeScaling()) {
            float scaling = Options.getScaling();
            if (oldScaling == Float.POSITIVE_INFINITY)
                oldScaling = scaling;
            scalingFactorT.setText(Double.toString(scaling));
        }
    }

    @Override
    public void savePreferences() {
        boolean shouldShowMessage = false;
        Options.setTimestampTooltipsDisabled(tooltipsC.isSelected());
        Options.setThemeVariation((ThemeVariation) themesC.getSelectedItem());
        
        LanguageOption selectedLanguage = (LanguageOption) languageC.getSelectedItem();
        if (selectedLanguage != null) {
            Options.setLanguage(selectedLanguage.getCode());
            if (!oldLanguage.equals(selectedLanguage.getCode()))
                shouldShowMessage = true;
        }
        
        if (oldTooltipsDisabled != tooltipsC.isSelected() || oldThemeVariation != themesC.getSelectedItem())
            shouldShowMessage = true;
        if (SystemDependent.shouldSupportChangeScaling()) {
            try {
                float newScaling = Float.parseFloat(scalingFactorT.getText());
                Options.setScaling(newScaling);
                if (Math.abs(newScaling - oldScaling) > 0.1)
                    shouldShowMessage = true;
            } catch (Exception ignored) {
            }
        }
        if (shouldShowMessage)
            JOptionPane.showMessageDialog(null, __("New UI elements will be performed after you restart Jubler"));
    }

    @Override
    public JPanel getTabPanel() {
        return this;
    }

    @Override
    public String getTabName() {
        return __("UI Options");
    }

    @Override
    public String getTabTooltip() {
        return __("Configure UI related parameters");
    }

    @Override
    public Icon getTabIcon() {
        return Theme.loadIcon("uioptions");
    }

    @Override
    public void changeProgram() {
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        layoutP = new javax.swing.JPanel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 8), new java.awt.Dimension(0, 8), new java.awt.Dimension(0, 8));
        scalingP = new javax.swing.JPanel();
        scalingFactorT = new javax.swing.JTextField();
        scalingL = new javax.swing.JLabel();
        scalingF = new javax.swing.Box.Filler(new java.awt.Dimension(0, 16), new java.awt.Dimension(0, 16), new java.awt.Dimension(0, 16));
        jPanel6 = new javax.swing.JPanel();
        tooltipsC = new javax.swing.JCheckBox();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 16), new java.awt.Dimension(0, 16), new java.awt.Dimension(0, 16));
        jPanel7 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        themesC = new javax.swing.JComboBox<>();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 16), new java.awt.Dimension(0, 16), new java.awt.Dimension(0, 16));
        jPanel8 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        languageC = new javax.swing.JComboBox<>();

        setLayout(new java.awt.BorderLayout());

        layoutP.setLayout(new javax.swing.BoxLayout(layoutP, javax.swing.BoxLayout.Y_AXIS));
        layoutP.add(filler1);

        scalingP.setLayout(new java.awt.BorderLayout());
        scalingP.add(scalingFactorT, java.awt.BorderLayout.CENTER);

        scalingL.setText(__("Scaling factor"));
        scalingP.add(scalingL, java.awt.BorderLayout.NORTH);

        layoutP.add(scalingP);
        layoutP.add(scalingF);

        jPanel6.setLayout(new java.awt.BorderLayout());

        tooltipsC.setText(__("Disable timestamp tooltips"));
        jPanel6.add(tooltipsC, java.awt.BorderLayout.CENTER);

        layoutP.add(jPanel6);
        layoutP.add(filler2);

        jPanel7.setLayout(new java.awt.BorderLayout(8, 0));

        jLabel1.setText(__("Theme variation"));
        jPanel7.add(jLabel1, java.awt.BorderLayout.WEST);
        jPanel7.add(themesC, java.awt.BorderLayout.CENTER);

        layoutP.add(jPanel7);
        layoutP.add(filler3);

        jPanel8.setLayout(new java.awt.BorderLayout(8, 0));

        jLabel2.setText(__("Language"));
        jPanel8.add(jLabel2, java.awt.BorderLayout.WEST);
        jPanel8.add(languageC, java.awt.BorderLayout.CENTER);

        layoutP.add(jPanel8);

        add(layoutP, java.awt.BorderLayout.NORTH);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JComboBox<LanguageOption> languageC;
    private javax.swing.JPanel layoutP;
    private javax.swing.Box.Filler scalingF;
    private javax.swing.JTextField scalingFactorT;
    private javax.swing.JLabel scalingL;
    private javax.swing.JPanel scalingP;
    private javax.swing.JComboBox<ThemeVariation> themesC;
    private javax.swing.JCheckBox tooltipsC;
    // End of variables declaration//GEN-END:variables
}

