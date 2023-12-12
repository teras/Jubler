/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.preview;

import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import java.text.AttributedString;

public abstract class PreviewBoolean extends PreviewElement {

    protected abstract Object getEnabledValue();

    public PreviewBoolean(Object deflt, AbstractStyleover over) {
        super(deflt, over);
    }

    public void addAttribute(AttributedString str, Object value, int from, int to) {
        if ((Boolean) value)
            super.addAttribute(str, getEnabledValue(), from, to);
    }
}
