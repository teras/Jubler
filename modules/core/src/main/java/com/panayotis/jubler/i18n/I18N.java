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

import com.eclipsesource.json.Json;
import com.panayotis.jubler.os.DEBUG;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.panayotis.jubler.os.SystemFileFinder.AppPath;

/**
 * @author teras
 */
@SuppressWarnings("StaticNonFinalUsedInInitialization")
public class I18N {

    private static final Map<String, String> transl = new HashMap<>();

    static {
        populateLanguage();
    }

    private static void populateLanguage() {
        String ls = Locale.getDefault().getLanguage();
        if (ls.equals("en")) {
            DEBUG.debug("Using default language");
        } else {
            String ll = ls + "_" + Locale.getDefault().getCountry();
            for (String p : new String[]{
                    "../../../../resources/i18n/" + ll + ".json",
                    "i18n/" + ll + ".json",

                    "../../../../resources/i18n/" + ls + ".json",
                    "i18n/" + ls + ".json"
            }) {
                File json = new File(AppPath, p);
                if (json.isFile()) {
                    try {
                        Json.parse(new FileReader(json)).asObject().forEach(member -> transl.put(member.getName(), member.getValue().asString()));
                        DEBUG.debug("Using language " + json.getName().substring(0, json.getName().length() - 5));
                        return;
                    } catch (Exception ignored) {
                    }
                }
            }
            DEBUG.debug("Unable to locate language " + ls);
        }
    }

    public static String __(String msg, Object... args) {
        String format = transl.getOrDefault(msg, msg);
        return MessageFormat.format(format.replaceAll("'", "''"), args);
    }
}
