/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals.gui;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.externals.Recipe;
import com.panayotis.jubler.tools.externals.RecipeExecutor;
import com.panayotis.jubler.tools.externals.RecipeMonitor;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Modal lifecycle dialog for a running recipe: a {@link ProgressView} on top, a
 * collapsible {@link LogView} below, and a single button that acts as Cancel while
 * running and Close once finished. Implements {@link RecipeMonitor}, so the executor
 * drives it directly. Replaces the single-use {@code JExternalConsole}.
 */
public class JRecipeProgress extends JDialog implements RecipeMonitor {

    private final ProgressView progress = new ProgressView();
    private final LogView log = new LogView();
    private final JButton actionB = new JButton(__("Cancel"));
    private final String recipeName;

    private volatile boolean cancelled = false;
    private volatile boolean finished = false;
    private volatile Process process;

    public JRecipeProgress(JubFrame parent, String recipeName) {
        super(parent, true);
        this.recipeName = recipeName;
        setTitle(recipeName);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout());
        content.add(progress, BorderLayout.NORTH);
        content.add(log, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionB.addActionListener(e -> onAction());
        buttons.add(actionB);
        content.add(buttons, BorderLayout.SOUTH);

        setContentPane(content);
        progress.setStatus(__("Running {0}…", recipeName));
        pack();
        setLocationRelativeTo(parent);
    }

    /** Start the recipe on a worker thread and block on the modal dialog. */
    public void execute(JubFrame jubler, Recipe recipe, Map<String, String> values,
                        List<SubEntry> scope, Map<String, JubFrame> windowSelections) {
        Thread worker = new Thread(
                () -> RecipeExecutor.execute(jubler, recipe, values, scope, windowSelections, this),
                "Recipe " + recipe.getName());
        worker.start();
        setVisible(true);
    }

    private void onAction() {
        if (finished) {
            dispose();
        } else {
            cancelled = true;
            actionB.setEnabled(false);
            actionB.setText(__("Cancelling…"));
            Process p = process;
            if (p != null)
                p.destroy();
        }
    }

    /* ===================== RecipeMonitor ===================== */

    @Override
    public void log(String line) {
        if (line != null && line.startsWith("@progress ")) {
            try {
                int pct = Integer.parseInt(line.substring("@progress ".length()).trim());
                SwingUtilities.invokeLater(() -> progress.setProgress(Math.max(0, Math.min(100, pct)), 100));
                return;
            } catch (NumberFormatException ignored) {
                // fall through and show as a normal log line
            }
        }
        SwingUtilities.invokeLater(() -> log.addLine(line));
    }

    @Override
    public void setProcess(Process process) {
        this.process = process;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void finished(boolean success, String message) {
        SwingUtilities.invokeLater(() -> {
            finished = true;
            progress.stop(success);
            progress.setStatus((success ? "" : __("Failed") + ": ") + (message == null ? "" : message));
            setTitle(recipeName + " — " + (success ? __("Success") : __("Failure")));
            actionB.setEnabled(true);
            actionB.setText(__("Close"));
        });
    }
}
