/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.event;

public class StyleoverCharacter extends AbstractStyleover {

    public StyleoverCharacter(Object style) {
        super(style);
    }

    protected int findPrevEdge(int start, String txt) {
        return start;
    }

    protected int findNextEdge(int end, String txt) {
        // return -1 if there is no next edge
        return end;
    }

    protected boolean deleteDependingOnStyle(AbstractStyleover.Entry entry, String subtext) {
        return false;
    }

    protected int offsetByParagraph() {
        return 0;
    }
}
