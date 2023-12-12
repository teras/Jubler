/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.i18n;

import com.eclipsesource.json.Json;
import com.panayotis.jubler.os.DEBUG;

import java.io.File;
import java.io.FileReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.panayotis.jubler.os.SystemFileFinder.AppPath;

@SuppressWarnings("StaticNonFinalUsedInInitialization")
public class I18N {

    private static final Map<String, String> transl = new HashMap<>();

    static {
        populateLanguage();
    }

    private static void populateLanguage() {
        String ls = Locale.getDefault().getLanguage();
        if (!ls.equals("en")) {
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
