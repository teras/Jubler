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


/**
 *
 * @author teras
 */
public class JIDialog extends JOptionPane {

    public static final Object[] ok_opts = {_("OK")};
    public static final Object[] ok_cancel_opts = {_("OK"), _("Cancel")};
    public static final Object[] yes_no_opts = {_("Yes"), _("No")};

    public static int info(Component parent, Object message, String title) {
        return showMessage(parent, message, title, INFORMATION_MESSAGE, ok_opts);
    }
    public static int action(Component parent, Object message, String title) {
        return showMessage(parent, message, title, INFORMATION_MESSAGE, ok_cancel_opts);
    }
    public static int question(Component parent, Object message, String title) {
        return showMessage(parent, message, title, WARNING_MESSAGE, yes_no_opts);
    }
    public static int warning(Component parent, Object message, String title) {
        return showMessage(parent, message, title, WARNING_MESSAGE, ok_opts);
    }
    public static int error(Component parent, Object message, String title) {
        return showMessage(parent, message, title, ERROR_MESSAGE, ok_opts);
    }
    
    private static int showMessage(Component parent, Object message, String title, int type, Object[] buttons) {
        return showOptionDialog(parent, message, title, DEFAULT_OPTION, type, null, buttons, buttons[0]);
    }
}
