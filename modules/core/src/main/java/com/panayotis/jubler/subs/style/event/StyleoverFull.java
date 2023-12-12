/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.event;

public class StyleoverFull extends AbstractStyleover {

    public StyleoverFull(Object style) {
        super(style);
    }

    protected int findPrevEdge(int start, String txt) {
        return 0;
    }

    protected int findNextEdge(int end, String txt) {
        return txt.length();
    }

    protected int offsetByParagraph() {
        return 0;
    }

    protected boolean deleteDependingOnStyle(AbstractStyleover.Entry entry, String subtext) {
        if (entry.prev.position == 0)
            return false;
        return true;
    }

    public void addEvent(Object event, int start, int end, Object basic, String txt) {
        super.addEvent(event, 0, txt.length() - 1, basic, txt);
        cleanupEvents(basic, txt);
    }
}
