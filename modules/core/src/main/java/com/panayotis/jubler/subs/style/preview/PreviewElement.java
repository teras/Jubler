/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.preview;

import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import com.panayotis.jubler.subs.style.event.StyleoverEvent;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;

public abstract class PreviewElement {

    private AbstractStyleover over;
    private Object deflt;

    protected abstract Attribute getStyle();

    public PreviewElement() {
        this(null, null);
    }

    public PreviewElement(Object deflt, AbstractStyleover over) {
        this.deflt = deflt;
        this.over = over;
    }

    public Object getDefault() {
        return deflt;
    }

    public int countStyleover() {
        if (over == null)
            return -1;
        return over.size();
    }

    public StyleoverEvent getEvent(int which) {
        return over.getEvent(which);
    }

    public void addAttribute(AttributedString str, Object value, int from, int to) {
        if (from != to)
            str.addAttribute(getStyle(), value, from, to);
    }
}
