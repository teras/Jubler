/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.preview;

import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator.Attribute;

public class PreviewFontname extends PreviewElement {

    private AbstractStyleover over;
    private Object deflt;

    protected Attribute getStyle() {
        return TextAttribute.FAMILY;
    }

    public PreviewFontname(Object deflt, AbstractStyleover over) {
        super(deflt, over);
    }
}
