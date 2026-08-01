/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.jubler.tools.languagetool.LanguagePackageInfo;
import com.panayotis.jubler.tools.languagetool.LanguagePackageManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.panayotis.jubler.i18n.I18N.__;

public class LanguageDownloadDialog extends JDialog {

    private JList<LanguagePackageInfo> languageList;
    private DefaultListModel<LanguagePackageInfo> listModel;
    private JButton downloadButton;
    private JButton cancelButton;
    private boolean languageDownloaded = false;

    public LanguageDownloadDialog(Window parent) {
        super(parent, __("Add Language"), Dialog.ModalityType.APPLICATION_MODAL);
        initComponents();
        loadAvailableLanguages();
        setSize(400, 500);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JLabel infoLabel = new JLabel(__("Select a language to download:"));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(infoLabel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        languageList = new JList<>(listModel);
        languageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(languageList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        cancelButton = new JButton(__("Cancel"));
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        downloadButton = new JButton(__("Download"));
        downloadButton.setEnabled(false);
        downloadButton.addActionListener(e -> onDownload());
        buttonPanel.add(downloadButton);

        add(buttonPanel, BorderLayout.SOUTH);

        languageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                downloadButton.setEnabled(languageList.getSelectedValue() != null);
            }
        });
    }

    private void loadAvailableLanguages() {
        List<LanguagePackageInfo> available = LanguagePackageManager.getAvailableLanguages();
        for (LanguagePackageInfo lang : available) {
            listModel.addElement(lang);
        }
    }

    private void onDownload() {
        LanguagePackageInfo selected = languageList.getSelectedValue();
        if (selected == null) {
            return;
        }

        LanguageDownloadProgressDialog progressDialog = new LanguageDownloadProgressDialog(
            getOwner(), selected);
        progressDialog.setVisible(true);

        if (progressDialog.wasSuccessful()) {
            languageDownloaded = true;
            dispose();
        }
    }

    public boolean wasLanguageDownloaded() {
        return languageDownloaded;
    }
}
