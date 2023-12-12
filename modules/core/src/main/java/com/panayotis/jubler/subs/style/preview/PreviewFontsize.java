/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.preview;

import com.panayotis.jubler.subs.style.event.AbstractStyleover;

import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;

import static com.panayotis.jubler.os.UIUtils.scale;

public class PreviewFontsize extends PreviewElement {

    private AbstractStyleover over;
    private Object deflt;

    protected Attribute getStyle() {
        return TextAttribute.SIZE;
    }

    public PreviewFontsize(Object deflt, AbstractStyleover over) {
        super(deflt, over);
    }

    public void addAttribute(AttributedString str, Object value, int from, int to) {
        super.addAttribute(str, scale((int) value), from, to);
    }
}
