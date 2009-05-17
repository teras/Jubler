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

import com.panayotis.jubler.os.DynamicClassLoader;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *
 * @author teras
 */
public class I18N {

    private static ResourceBundle b;
    private static final String PATH = "com.panayotis.jubler.i18n.Messages_";


    static {
        String lang = System.getProperty("user.language");
        String country = System.getProperty("user.country");

        setLang(lang + "_" + country);
        if (b == null)
            setLang(lang);
    }

    public static String ngettext(String single, String plural, long n, Object... args) {
        String format = GettextResource.ngettext(b, single, plural, n);
        return MessageFormat.format(format.replaceAll("'", "''"), args);
    }

    public static String _(String msg, Object... args) {
        String format = GettextResource.gettext(b, msg);
        return MessageFormat.format(format.replaceAll("'", "''"), args);
    }

    private static void setLang(String langcode) {
        try {
            ClassLoader cl = new DynamicClassLoader(new String[]{"dist/i18n/"+langcode+".jar", "i18n/"+langcode+".jar"}, false);
            b = (ResourceBundle) cl.loadClass(PATH + langcode).newInstance();
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
    }
}
