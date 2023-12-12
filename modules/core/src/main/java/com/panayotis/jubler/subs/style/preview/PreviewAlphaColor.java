/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.preview;

import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import java.text.AttributedString;

public class PreviewAlphaColor extends PreviewColor {

    public PreviewAlphaColor(Object deflt, AbstractStyleover over) {
        super(deflt, over);
    }

    public void addAttribute(AttributedString str, Object value, int from, int to) {
        super.addAttribute(str, ((AlphaColor) value).getAColor(), from, to);
    }
}
