/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.jubler.tools.hunspell.HunspellDictInfo;
import com.panayotis.jubler.tools.hunspell.HunspellDictManager;

import javax.swing.*;
import java.awt.*;

import static com.panayotis.jubler.i18n.I18N.__;

public class HunspellDownloadProgressDialog extends JDialog {

    private final HunspellDictInfo language;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton cancelButton;
    private volatile boolean cancelled = false;
    private boolean successful = false;
    private Thread downloadThread;

    public HunspellDownloadProgressDialog(Window parent, HunspellDictInfo language) {
        super(parent, __("Downloading Language"), Dialog.ModalityType.APPLICATION_MODAL);
        this.language = language;
        initComponents();
        setSize(450, 150);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        startDownload();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        statusLabel = new JLabel(__("Downloading {0}...", language.getName()));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(statusLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(progressBar);

        add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancelButton = new JButton(__("Cancel"));
        cancelButton.addActionListener(e -> onCancel());
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void startDownload() {
        downloadThread = new Thread(() -> {
            try {
                HunspellDictManager.downloadDict(language, new HunspellDictManager.DownloadProgressListener() {
                    @Override
                    public void onProgress(int percent, long downloaded, long total) {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(percent);
                            statusLabel.setText(__("Downloading {0}...", language.getName()));
                        });
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
        });
        downloadThread.start();
    }

    private void onCancel() {
        cancelled = true;
        cancelButton.setEnabled(false);
        statusLabel.setText(__("Cancelling..."));
        new Thread(() -> {
            try {
                if (downloadThread != null)
                    downloadThread.join(5000);
            } catch (InterruptedException ignored) {
            }
            SwingUtilities.invokeLater(this::dispose);
        }).start();
    }

    public boolean wasSuccessful() {
        return successful;
    }
}
