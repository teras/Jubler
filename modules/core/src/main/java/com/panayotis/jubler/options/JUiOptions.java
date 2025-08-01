/*
 * (c) 2005-2023 by Panayotis Katsaloulis
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

    /**
     * Creates new form JExternalToolsOptions
     */
    public JUiOptions() {
        initComponents();
        for (ThemeVariation v : ThemeVariation.values())
            themesC.addItem(v);
        if (!SystemDependent.shouldSupportChangeScaling()) {
            layoutP.remove(scalingP);
            layoutP.remove(scalingF);
        }
        loadPreferences();
    }

    @Override
    public void loadPreferences() {
        oldTooltipsDisabled = UIUtils.isTimestampTooltipsDisabled();
        oldThemeVariation = UIUtils.getThemeVariation();

        tooltipsC.setSelected(oldTooltipsDisabled);
        themesC.setSelectedItem(oldThemeVariation);

        if (SystemDependent.shouldSupportChangeScaling()) {
            float scaling = UIUtils.loadScaling();
            if (oldScaling == Float.POSITIVE_INFINITY)
                oldScaling = scaling;
            if (scaling > 0.1)
                scalingFactorT.setText(Double.toString(scaling));
        }
    }

    @Override
    public void savePreferences() {
        boolean shouldShowMessage = false;
        UIUtils.saveTimestampTooltipsDisabled(tooltipsC.isSelected());
        UIUtils.saveThemeVariation((ThemeVariation) themesC.getSelectedItem());
        if (oldTooltipsDisabled != tooltipsC.isSelected() || oldThemeVariation != themesC.getSelectedItem())
            shouldShowMessage = true;
        if (SystemDependent.shouldSupportChangeScaling()) {
            try {
                float newScaling = Float.parseFloat(scalingFactorT.getText());
                UIUtils.saveScaling(newScaling);
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
        return "UI Options";
    }

    @Override
    public String getTabTooltip() {
        return "Configure UI related parameters";
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

        add(layoutP, java.awt.BorderLayout.NORTH);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel layoutP;
    private javax.swing.Box.Filler scalingF;
    private javax.swing.JTextField scalingFactorT;
    private javax.swing.JLabel scalingL;
    private javax.swing.JPanel scalingP;
    private javax.swing.JComboBox<ThemeVariation> themesC;
    private javax.swing.JCheckBox tooltipsC;
    // End of variables declaration//GEN-END:variables
}

