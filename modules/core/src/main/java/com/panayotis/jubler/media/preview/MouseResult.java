/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview;

import java.awt.*;

import static java.awt.Cursor.*;

class MouseResult {

    final MouseLocation location;
    final boolean isSelected;
    final SubInfo subInfo;

    MouseResult(MouseLocation location, boolean isSelected, SubInfo subInfo) {
        this.location = location;
        this.isSelected = isSelected;
        this.subInfo = subInfo;
    }

    void setCursor(Component component, boolean mouseDown, boolean isEdit) {
        if (isEdit) {
            if (mouseDown && location == MouseLocation.IN)
                component.setCursor(getPredefinedCursor(MOVE_CURSOR));
            else
                component.setCursor(location.cursor);
        } else {
            if (location == MouseLocation.OUT)
                component.setCursor(location.cursor);
            else
                component.setCursor(MouseLocation.IN.cursor);
        }
    }
}