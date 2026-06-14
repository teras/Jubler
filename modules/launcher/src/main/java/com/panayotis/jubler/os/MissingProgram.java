/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.os;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.HashSet;
import java.util.Set;

/**
 * Shows a one-time, operating-system specific dialog when an external program
 * required to perform an action is missing, explaining how to install it on the
 * user's particular system instead of failing silently or aborting.
 * <p>
 * The caller passes already-translated text; the only logic here is choosing the
 * instructions for the current operating system and making sure the same program
 * is not reported more than once per session.
 */
public class MissingProgram {

    private static final Set<String> warned = new HashSet<>();

    /**
     * Warn the user, at most once per program per session, that an external
     * program is missing, showing how to install it on the current platform.
     *
     * @param programName        dedup key (e.g. "VLC")
     * @param title              dialog title
     * @param description        what the program is needed for
     * @param macInstructions    install instructions shown on macOS
     * @param winInstructions    install instructions shown on Windows
     * @param linuxInstructions  install instructions shown on Linux and other systems
     */
    public static void warn(String programName, String title, String description,
                            String macInstructions, String winInstructions, String linuxInstructions) {
        synchronized (warned) {
            if (!warned.add(programName))
                return;
        }
        String instructions = SystemDependent.IS_MACOSX ? macInstructions
                : SystemDependent.IS_WINDOWS ? winInstructions
                : linuxInstructions;
        String message = (instructions == null || instructions.isEmpty())
                ? description
                : description + "\n\n" + instructions;
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE));
    }
}
