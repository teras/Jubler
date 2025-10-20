/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.os;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    public void uncaughtException(Thread t, Throwable e) {
        DEBUG.debug(e);

        // Show error dialog to user on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            String message = e.getMessage();
            if (message == null || message.isEmpty()) {
                message = e.getClass().getSimpleName();
            }
            JOptionPane.showMessageDialog(
                null,
                "An unexpected error occurred:\n" + message + "\n\nPlease check the log file for details.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        });
    }
}
