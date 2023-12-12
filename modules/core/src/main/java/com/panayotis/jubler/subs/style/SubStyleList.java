/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style;

import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.subs.SubEntry;

import java.util.ArrayList;

public class SubStyleList extends ArrayList<SubStyle> implements NameList {

    private static final SubStyle default_style;

    static {
        default_style = new SubStyle("Default");
        default_style.setDefault(true);
        default_style.setValues(Options.getOption("Styles.Default", ""));
    }

    /**
     * Creates a new instance of SubStyleList
     */
    public SubStyleList() {
        add(new SubStyle(default_style));
        get(0).setDefault(true);
    }

    public SubStyleList(SubStyleList old) {
        for (int i = 0; i < old.size(); i++)
            add(new SubStyle(old.get(i)));
        get(0).setDefault(true);
    }

    public String getNameAt(int i) {
        return get(i).Name;
    }

    public int getStyleIndex(SubEntry entry) {
        SubStyle style = entry.getStyle();
        int res;
        if (style == null || (res = indexOf(style)) < 0) {
            entry.setStyle(get(0));
            return 0;
        }
        return res;
    }

    public int findStyleIndex(String name) {
        for (int i = 0; i < size(); i++)
            if (name.equals(get(i).Name))
                return i;
        return 0;
    }

    public SubStyle getStyleByName(String name) {
        return get(findStyleIndex(name));
    }

    public SubStyle clearList() {
        SubStyle d = get(0);
        clear();
        return d;
    }

    public SubStyle getElementAt(int i) {
        return get(i);
    }
}
