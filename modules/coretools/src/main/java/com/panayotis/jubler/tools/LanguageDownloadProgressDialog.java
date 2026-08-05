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

/** Modal progress while a {@link SpellChecker} downloads one language; works with any checker. */
class LanguageDownloadProgressDialog extends JDialog {

    private final SpellChecker checker;
    private final SpellLanguage language;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton cancelButton;
    private volatile boolean cancelled = false;
    private boolean successful = false;
    private Thread downloadThread;

    LanguageDownloadProgressDialog(Window parent, SpellChecker checker, SpellLanguage language) {
        super(parent, __("Downloading Language"), ModalityType.APPLICATION_MODAL);
        this.checker = checker;
        this.language = language;
        initComponents();
        setSize(460, 150);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        startDownload();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        statusLabel = new JLabel(__("Downloading {0}…", language.getName()));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(statusLabel);
        content.add(Box.createVerticalStrut(10));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(progressBar);

        add(content, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancelButton = new JButton(__("Cancel"));
        cancelButton.addActionListener(e -> onCancel());
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);
    }

    private void startDownload() {
        downloadThread = new Thread(() -> {
            try {
                checker.downloadLanguage(language, new SpellChecker.DownloadProgress() {
                    @Override
                    public void onProgress(int percent, long downloaded, long total) {
                        SwingUtilities.invokeLater(() -> progressBar.setValue(percent));
                    }

                    @Override
                    public boolean isCancelled() {
                        return cancelled;
                    }
                });
                if (!cancelled)
                    SwingUtilities.invokeLater(() -> {
                        successful = true;
                        dispose();
                    });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    if (!cancelled) {
                        String msg = e.getMessage();
                        if (msg == null || msg.isEmpty())
                            msg = e.getClass().getSimpleName();
                        JOptionPane.showMessageDialog(this,
                                __("Failed to download language: {0}", msg),
                                __("Error"), JOptionPane.ERROR_MESSAGE);
                    }
                    dispose();
                });
            }
        }, "Language-download");
        downloadThread.start();
    }

    private void onCancel() {
        cancelled = true;
        cancelButton.setEnabled(false);
        statusLabel.setText(__("Cancelling…"));
        new Thread(() -> {
            try {
                if (downloadThread != null)
                    downloadThread.join(5000);
            } catch (InterruptedException ignored) {
            }
            SwingUtilities.invokeLater(this::dispose);
        }, "Language-download-cancel").start();
    }

    boolean wasSuccessful() {
        return successful;
    }
}
