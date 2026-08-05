/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.tools.spell.SpellChecker;
import com.panayotis.jubler.tools.spell.SpellLanguage;

import javax.swing.*;
import java.awt.*;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Manage a {@link SpellChecker}'s languages: remove installed ones and download new ones. Driven entirely
 * through the checker's language API, so it works for any speller that advertises downloadable languages.
 */
class LanguageManagerDialog extends JDialog {

    private final SpellChecker checker;
    private final DefaultListModel<SpellLanguage> installedModel = new DefaultListModel<>();
    private final DefaultListModel<SpellLanguage> availableModel = new DefaultListModel<>();
    private JList<SpellLanguage> installedList;
    private JList<SpellLanguage> availableList;
    private JButton removeButton;
    private JButton downloadButton;
    private boolean changed = false;

    LanguageManagerDialog(Window parent, SpellChecker checker) {
        super(parent, __("Languages"), ModalityType.APPLICATION_MODAL);
        this.checker = checker;
        initComponents();
        refresh();
        setSize(420, 520);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        installedList = new JList<>(installedModel);
        installedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        installedList.setCellRenderer(new LanguageRenderer());
        removeButton = new JButton(__("Remove"));
        removeButton.setEnabled(false);
        removeButton.addActionListener(e -> onRemove());
        content.add(section(__("Installed languages"), installedList, removeButton));

        content.add(Box.createVerticalStrut(12));

        availableList = new JList<>(availableModel);
        availableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        downloadButton = new JButton(__("Download"));
        downloadButton.setEnabled(false);
        downloadButton.addActionListener(e -> onDownload());
        content.add(section(__("Available to download"), availableList, downloadButton));

        installedList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                SpellLanguage sel = installedList.getSelectedValue();
                removeButton.setEnabled(sel != null && checker.canRemove(sel));
            }
        });
        availableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                downloadButton.setEnabled(availableList.getSelectedValue() != null);
        });

        JButton closeButton = new JButton(__("Close"));
        closeButton.addActionListener(e -> dispose());
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.add(closeButton);

        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);
        add(closePanel, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(closeButton);
    }

    /** A labelled list with a single action button under it. */
    private static JPanel section(String title, JList<?> list, JButton action) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(new JLabel(title), BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(action);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void refresh() {
        SpellLanguage selInstalled = installedList.getSelectedValue();
        installedModel.clear();
        for (SpellLanguage l : checker.getInstalledLanguages())
            installedModel.addElement(l);
        availableModel.clear();
        for (SpellLanguage l : checker.getDownloadableLanguages())
            availableModel.addElement(l);
        if (selInstalled != null)
            installedList.setSelectedValue(selInstalled, true);
        removeButton.setEnabled(installedList.getSelectedValue() != null
                && checker.canRemove(installedList.getSelectedValue()));
        downloadButton.setEnabled(availableList.getSelectedValue() != null);
    }

    private void onRemove() {
        SpellLanguage sel = installedList.getSelectedValue();
        if (sel == null || !checker.canRemove(sel))
            return;
        if (JOptionPane.showConfirmDialog(this, __("Remove language: {0}?", sel.getName()),
                __("Confirm Removal"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;
        if (checker.removeLanguage(sel)) {
            changed = true;
            refresh();
        } else
            JOptionPane.showMessageDialog(this, __("Failed to remove language."),
                    __("Error"), JOptionPane.ERROR_MESSAGE);
    }

    private void onDownload() {
        SpellLanguage sel = availableList.getSelectedValue();
        if (sel == null)
            return;
        LanguageDownloadProgressDialog progress = new LanguageDownloadProgressDialog(this, checker, sel);
        progress.setVisible(true);
        if (progress.wasSuccessful()) {
            changed = true;
            refresh();
        }
    }

    /** Whether any language was added or removed (so the caller can refresh its language list). */
    boolean wasChanged() {
        return changed;
    }

    /** Marks the always-present built-in language so the user sees why it cannot be removed. */
    private static class LanguageRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof SpellLanguage && ((SpellLanguage) value).isBuiltin())
                setText(((SpellLanguage) value).getName() + "  " + __("(built-in)"));
            return this;
        }
    }
}
