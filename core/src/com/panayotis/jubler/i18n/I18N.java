/*
 * I18N.java
 *
 * Created on 21 Αύγουστος 2005, 7:18 πμ
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

package com.panayotis.jubler.i18n;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.DynamicClassLoader;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *
 * @author teras
 */
@SuppressWarnings("StaticNonFinalUsedInInitialization")
public class I18N {

    private static ResourceBundle b;
    private static final String PATH = "com.panayotis.jubler.i18n.Messages_";
    private static final DynamicClassLoader cl = new DynamicClassLoader();

    static {
        String ls = System.getProperty("user.language");
        String ll = ls + "_" + System.getProperty("user.country");

        cl.addPaths(new String[]{
            "../../../dist/i18n/" + ls + ".jar", "i18n/" + ls + ".jar",
            "../../../dist/i18n/" + ll + ".jar", "i18n/" + ll + ".jar"
        });

        b = loadClass(PATH + ll);
        if (b == null) {
            b = loadClass(PATH + ls);
            if (b != null)
                DEBUG.debug("Using language " + ls);
        } else
            DEBUG.debug("Using language " + ll);
    }

    @SuppressWarnings("UseSpecificCatch")
    private static ResourceBundle loadClass(String classname) {
        try {
            return (ResourceBundle) cl.loadClass(classname).newInstance();
        } catch (Exception e) {
        }
        return null;
    }

    public static String ngettext(String single, String plural, long n, Object... args) {
        String format = GettextResource.ngettext(b, single, plural, n);
        return MessageFormat.format(format.replaceAll("'", "''"), args);
    }

    public static String __(String msg, Object... args) {
        String format = GettextResource.gettext(b, msg);
        return MessageFormat.format(format.replaceAll("'", "''"), args);
    }
}
