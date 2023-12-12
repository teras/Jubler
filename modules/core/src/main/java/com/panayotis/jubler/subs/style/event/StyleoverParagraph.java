/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.event;

public class StyleoverParagraph extends AbstractStyleover {

    public StyleoverParagraph(Object style) {
        super(style);
    }

    protected int findPrevEdge(int start, String txt) {
        int where = txt.lastIndexOf('\n', start - 1);
        if (where >= 0)
            return where + 1;
        return 0;
    }

    protected int findNextEdge(int end, String txt) {
        int where = txt.indexOf('\n', end);
        if (where >= 0)
            return where + 1;
        return -1;
    }

    protected boolean deleteDependingOnStyle(AbstractStyleover.Entry entry, String subtext) {
        if (entry.prev.position == 0)
            return false;
        if (entry.prev.position > 0 && subtext.charAt(entry.prev.position - 1) == '\n')
            return false;
        return true;
    }

    protected int offsetByParagraph() {
        return 1;
    }
}
