/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.preview;

import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator.Attribute;

public class PreviewBold extends PreviewBoolean {

    protected Object getEnabledValue() {
        return TextAttribute.WEIGHT_BOLD;
    }

    protected Attribute getStyle() {
        return TextAttribute.WEIGHT;
    }

    /**
     * Creates a new instance of PreviewBoolean
     */
    public PreviewBold(Object deflt, AbstractStyleover over) {
        super(deflt, over);
    }
}
