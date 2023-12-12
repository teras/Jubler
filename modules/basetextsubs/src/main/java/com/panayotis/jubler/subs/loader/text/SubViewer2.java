/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader.text;

import java.util.regex.Pattern;

import static com.panayotis.jubler.i18n.I18N.__;

public class SubViewer2 extends SubViewer {

    private static final Pattern testpat;

    /**
     * Creates a new instance of SubFormat
     */
    static {
        testpat = Pattern.compile("(?i)(?s)\\[INFORMATION\\].*?"
                + "(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d)"
                + sp + nl + "(.*?)\\[br\\](.*?)" + nl + nl);
    }

    @Override
    protected Pattern getTestPattern() {
        return testpat;
    }

    @Override
    public String getName() {
        return "SubViewer2";
    }

    @Override
    public String getExtendedName() {
        return "SubViewer V2";
    }

    @Override
    protected String subreplace(String sub) {
        return sub.replace("\n", "[br]");
    }
}
