/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.os;

import com.panayotis.jubler.theme.Theme;

import javax.swing.*;
import java.awt.*;

import static com.panayotis.jubler.i18n.I18N.__;

public class JIDialog extends JOptionPane {

    public static final Object[] ok_opts = {__("OK")};
    public static final Object[] ok_cancel_opts = {__("OK"), __("Cancel")};
    public static final Object[] yes_no_opts = {__("Yes"), __("No")};

    public static void about(Component parent, Object message, String title, String iconpath) {
        ImageIcon icon = Theme.loadIcon(iconpath, 0.2f);
        showMessage(parent, message, title, PLAIN_MESSAGE, icon, ok_opts);
    }

    public static void info(Component parent, Object message, String title) {
        showMessage(parent, message, title, INFORMATION_MESSAGE, null, ok_opts);
    }

    public static boolean action(Component parent, Object message, String title) {
        return showMessage(parent, message, title, PLAIN_MESSAGE, null, ok_cancel_opts);
    }

    public static boolean question(Component parent, Object message, String title) {
        return showMessage(parent, message, title, QUESTION_MESSAGE, null, yes_no_opts);
    }

    public static void warning(Component parent, Object message, String title) {
        showMessage(parent, message, title, WARNING_MESSAGE, null, ok_opts);
    }

    public static void error(Component parent, Object message, String title) {
        showMessage(parent, message, title, ERROR_MESSAGE, null, ok_opts);
    }

    private static boolean showMessage(Component parent, Object message, String title, int type, ImageIcon icon, Object[] buttons) {
        return showOptionDialog(parent, message, title, DEFAULT_OPTION, type, icon, buttons, buttons[0]) == OK_OPTION;
    }
}
