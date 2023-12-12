/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.options.gui;

import java.awt.Dimension;

/**
 * The progress bar using to report operation progress. It requires three
 * values, the min value, the max value and the current value.
 *
 * @author Hoang Duy Tran
 */
public class ProgressBar extends javax.swing.JPanel {

    private int minValue;
    private int maxValue;
    private int value;
    private String title;

    /**
     * Creates new dialog form for a progress bar and packing the dialog, ready
     * for use.
     */
    public ProgressBar() {
        initComponents();
        progressBar.setStringPainted(true);
        getDlg().setPreferredSize(new Dimension(200, 0));
        getDlg().add(this.getProgressBar());
        //getDlg().setLocationRelativeTo( null );
        //getDlg().setAlwaysOnTop(true);
        getDlg().pack();
    }

    private void reset() {
        minValue = maxValue = value = 0;
        title = null;
    }

    /**
     * Checking to see if the progress bar is visible or not.
     *
     * @return True if the progress bar is visible, false otherwise.
     */
    public boolean isOn() {
        return dlg.isVisible();
    }

    /**
     * Checking to see if the progress bar is invisible or not.
     *
     * @return True if the progress bar is not visible, false otherwise.
     */
    public boolean isOff() {
        return !isOn();
    }

    /**
     * Sets the display ON. This action makes the progress-bar visible on the
     * screen.
     */
    public void on() {
        dlg.setVisible(true);
    }

    /**
     * Sets the display OFF and reset internal values back to zero.
     */
    public void off() {
        dlg.setVisible(false);
        reset();
        dlg.dispose();
    }

    /**
     * Gets the reference to the dialogue
     *
     * @return reference to the dialog.
     */
    public javax.swing.JDialog getDlg() {
        return dlg;
    }

    /**
     * Gets a reference to the progress bar.
     *
     * @return Reference to the progress bar.
     */
    public javax.swing.JProgressBar getProgressBar() {
        return progressBar;
    }

    /**
     * Get to minimum value .
     *
     * @return Minimum value.
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * Set minimum value.
     *
     * @param minValue Minimum value.
     */
    public void setMinValue(int minValue) {
        this.minValue = minValue;
        this.progressBar.setMinimum(minValue);
    }

    /**
     * Get maximum value.
     *
     * @return Maximum value.
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * Set maximum value.
     *
     * @param maxValue Maximum value.
     */
    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
        this.progressBar.setMaximum(maxValue);
    }

    /**
     * Get current value.
     *
     * @return Current value.
     */
    public int getValue() {
        return value;
    }

    /**
     * Set current value.
     *
     * @param value Current value.
     */
    public void setValue(int value) {
        this.value = value;
        this.progressBar.setValue(value);
    }

    /**
     * Gets a reference to the title of the dialogue.
     *
     * @return Reference to the dialogue's title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title for the dialogue.
     *
     * @param title The string representing the title for the dialogue.
     */
    public void setTitle(String title) {
        this.title = title;
        dlg.setTitle(title);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        dlg = new javax.swing.JDialog();
        progressPanel = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();

        dlg.getContentPane().setLayout(new java.awt.GridLayout(1, 0));

        setLayout(new java.awt.BorderLayout());

        progressPanel.setLayout(new java.awt.GridLayout(1, 0));
        progressPanel.add(progressBar);

        add(progressPanel, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDialog dlg;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel progressPanel;
    // End of variables declaration//GEN-END:variables
}
