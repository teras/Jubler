/*
 * JIDialog.java
 *
 * Created on 22 Αύγουστος 2005, 3:28 πμ
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.panayotis.jubler.os;

import java.awt.Component;
import javax.swing.JOptionPane;

import static com.panayotis.jubler.i18n.I18N._;
import javax.swing.ImageIcon;


/**
 *
 * @author teras
 */
public class JIDialog extends JOptionPane {

    public static final Object[] ok_opts = {_("OK")};
    public static final Object[] ok_cancel_opts = {_("OK"), _("Cancel")};
    public static final Object[] yes_no_opts = {_("Yes"), _("No")};

    public static void about(Component parent, Object message, String title, String iconpath) {
        ImageIcon icon = new ImageIcon(JIDialog.class.getResource(iconpath));
        showMessage(parent, message, title, PLAIN_MESSAGE, icon, ok_opts);
    }
    public static void info(Component parent, Object message, String title) {
        showMessage(parent, message, title, INFORMATION_MESSAGE, null, ok_opts);
    }
    public static boolean action(Component parent, Object message, String title) {
        return showMessage(parent, message, title, INFORMATION_MESSAGE, null, ok_cancel_opts);
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
