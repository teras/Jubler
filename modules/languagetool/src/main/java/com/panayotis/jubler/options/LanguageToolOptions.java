/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.tools.languagetool.LanguagePackageInfo;
import com.panayotis.jubler.tools.languagetool.LanguagePackageManager;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import static com.panayotis.jubler.i18n.I18N.__;

public class LanguageToolOptions extends JExtBasicOptions {

    private JList<LanguagePackageInfo> languageList;
    private DefaultListModel<LanguagePackageInfo> listModel;
    private JButton addButton;
    private JButton removeButton;
    private JComboBox<LanguagePackageInfo> activeLanguageCombo;

    public LanguageToolOptions(String family, String name) {
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
        JLabel infoLabel = new JLabel(__("Installed Languages:"));
        centerPanel.add(infoLabel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        languageList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(languageList);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        topPanel.add(centerPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        addButton = new JButton(__("Add Language..."));
        addButton.addActionListener(e -> onAddLanguage());
        buttonPanel.add(addButton);

        removeButton = new JButton(__("Remove"));
        removeButton.addActionListener(e -> onRemoveLanguage());
        removeButton.setEnabled(false);
        buttonPanel.add(removeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        languageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                LanguagePackageInfo selected = languageList.getSelectedValue();
                removeButton.setEnabled(selected != null && !"en".equals(selected.getCode()));
            }
        });
    }

    private void refreshLanguageList() {
        listModel.clear();
        activeLanguageCombo.removeAllItems();
        
        String savedLangCode = JublerPrefs.getString("languagetool.language", "en");
        int selectedIndex = 0;
        int index = 0;
        
        for (LanguagePackageInfo lang : LanguagePackageManager.getInstalledLanguages()) {
            listModel.addElement(lang);
            activeLanguageCombo.addItem(lang);
            if (lang.getCode().equals(savedLangCode)) {
                selectedIndex = index;
            }
            index++;
        }
        
        if (activeLanguageCombo.getItemCount() > 0) {
            activeLanguageCombo.setSelectedIndex(selectedIndex);
        }
    }

    private void onAddLanguage() {
        LanguageDownloadDialog dialog = new LanguageDownloadDialog(
            SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        
        if (dialog.wasLanguageDownloaded()) {
            refreshLanguageList();
            JOptionPane.showMessageDialog(this,
                __("Language installed successfully. Please restart Jubler to use the new language."),
                __("Restart Required"),
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onRemoveLanguage() {
        LanguagePackageInfo selected = languageList.getSelectedValue();
        if (selected == null || "en".equals(selected.getCode())) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
            __("Remove language: {0}?", selected.getName()),
            __("Confirm Removal"),
            JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            if (LanguagePackageManager.deleteLanguage(selected)) {
                refreshLanguageList();
                JOptionPane.showMessageDialog(this,
                    __("Language removed. Please restart Jubler."),
                    __("Restart Required"),
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    __("Failed to remove language."),
                    __("Error"),
                    JOptionPane.ERROR_MESSAGE);
            }
        }
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
        LanguagePackageInfo selected = (LanguagePackageInfo) activeLanguageCombo.getSelectedItem();
        if (selected != null) {
            JublerPrefs.set("languagetool.language", selected.getCode());
        }
    }
    
    public String getSelectedLanguageCode() {
        LanguagePackageInfo selected = (LanguagePackageInfo) activeLanguageCombo.getSelectedItem();
        if (selected != null) {
            return selected.getCode();
        }
        
        String savedLang = JublerPrefs.getString("languagetool.language", "en");
        System.err.println("LanguageToolOptions: No language selected in UI, using saved preference: " + savedLang);
        return savedLang;
    }

    @Override
    protected void updateOptionsPanel() {
        refreshLanguageList();
    }
}
