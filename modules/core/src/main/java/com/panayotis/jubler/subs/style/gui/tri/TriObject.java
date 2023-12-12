/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.gui.tri;

import com.panayotis.jubler.subs.style.StyleChangeListener;
import com.panayotis.jubler.subs.style.StyleType;

public interface TriObject {

    /* Display the current style data on this visual object */
    public void setData(Object data);

    /* For visual objects which can hold more than one style (i.e. bold, italic), use this function to 
     * inform the widget which is the style they manage */
    public void setStyle(StyleType info);

    /* When the user clicks on a selection, then *this* listener will be informed for the change */
    public void setListener(StyleChangeListener listener);
}
