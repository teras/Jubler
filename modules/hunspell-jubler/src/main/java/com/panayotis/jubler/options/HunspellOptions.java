/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.tools.hunspell.HunspellDictInfo;
import com.panayotis.jubler.tools.hunspell.HunspellDictManager;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import static com.panayotis.jubler.i18n.I18N.__;

public class HunspellOptions extends JExtBasicOptions {

    private static final String PREF_KEY = "hunspell.language";

    private JList<HunspellDictInfo> languageList;
    private DefaultListModel<HunspellDictInfo> listModel;
    private JButton removeButton;
    private JComboBox<HunspellDictInfo> activeLanguageCombo;

    public HunspellOptions(String family, String name) {
        super(family, name, name, null);
        initComponents();
        refreshLanguageList();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel activePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        activePanel.add(new JLabel(__("Active Language:")));
        activeLanguageCombo = new JComboBox<>();
        activePanel.add(activeLanguageCombo);
        topPanel.add(activePanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JLabel(__("Installed Languages:")), BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        languageList = new JList<>(listModel);
        centerPanel.add(new JScrollPane(languageList), BorderLayout.CENTER);

        topPanel.add(centerPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton addButton = new JButton(__("Add Language..."));
        addButton.addActionListener(e -> onAddLanguage());
        buttonPanel.add(addButton);

        removeButton = new JButton(__("Remove"));
        removeButton.addActionListener(e -> onRemoveLanguage());
        removeButton.setEnabled(false);
        buttonPanel.add(removeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        languageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                HunspellDictInfo selected = languageList.getSelectedValue();
                removeButton.setEnabled(selected != null && !selected.isBuiltin());
            }
        });
    }

    private void refreshLanguageList() {
        listModel.clear();
        activeLanguageCombo.removeAllItems();

        String saved = JublerPrefs.getString(PREF_KEY, HunspellDictManager.BUILTIN_CODE);
        int selectedIndex = 0, index = 0;

        for (HunspellDictInfo lang : HunspellDictManager.getInstalledDicts()) {
            listModel.addElement(lang);
            activeLanguageCombo.addItem(lang);
            if (lang.getCode().equals(saved))
                selectedIndex = index;
            index++;
        }
        if (activeLanguageCombo.getItemCount() > 0)
            activeLanguageCombo.setSelectedIndex(selectedIndex);
    }

    private void onAddLanguage() {
        HunspellDownloadDialog dialog = new HunspellDownloadDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.wasLanguageDownloaded())
            refreshLanguageList();
    }

    private void onRemoveLanguage() {
        HunspellDictInfo selected = languageList.getSelectedValue();
        if (selected == null || selected.isBuiltin())
            return;

        int result = JOptionPane.showConfirmDialog(this,
                __("Remove language: {0}?", selected.getName()),
                __("Confirm Removal"), JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            if (HunspellDictManager.deleteDict(selected))
                refreshLanguageList();
            else
                JOptionPane.showMessageDialog(this, __("Failed to remove language."),
                        __("Error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getSelectedLanguageCode() {
        HunspellDictInfo selected = (HunspellDictInfo) activeLanguageCombo.getSelectedItem();
        if (selected != null)
            return selected.getCode();
        return JublerPrefs.getString(PREF_KEY, HunspellDictManager.BUILTIN_CODE);
    }

    @Override
    public String getExecFileName() {
        return "builtin";
    }

    @Override
    protected void loadPreferences() {
        refreshLanguageList();
    }

    @Override
    protected void savePreferences() {
        HunspellDictInfo selected = (HunspellDictInfo) activeLanguageCombo.getSelectedItem();
        if (selected != null)
            JublerPrefs.set(PREF_KEY, selected.getCode());
    }

    @Override
    protected void updateOptionsPanel() {
        refreshLanguageList();
    }
}
