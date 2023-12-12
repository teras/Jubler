/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.preview;

import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;

public class PreviewSingle extends PreviewElement {

    /**
     * Creates a new instance of PreviewSingle
     */
    public PreviewSingle(Object deflt, AbstractStyleover over) {
        super(deflt, over);
    }

    /* Ignore these methods, we don't need them, since this attribute is not character based */
    protected Attribute getStyle() {
        return null;
    }

    public void addAttribute(AttributedString str, Object value, int from, int to) {
    }
}
