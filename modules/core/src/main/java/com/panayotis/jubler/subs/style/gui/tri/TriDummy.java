/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.gui.tri;

import com.panayotis.jubler.subs.style.StyleChangeListener;
import com.panayotis.jubler.subs.style.StyleType;
import javax.swing.JPanel;

public class TriDummy extends JPanel implements TriObject {

    /**
     * Creates a new instance of TriDummy
     */
    public TriDummy() {
    }
    private StyleType styletype;
    private StyleChangeListener listener;

    public void setStyle(StyleType style) {
        styletype = style;
    }

    public void setListener(StyleChangeListener listener) {
        this.listener = listener;
    }

    public void setData(Object data) {
    }
}
