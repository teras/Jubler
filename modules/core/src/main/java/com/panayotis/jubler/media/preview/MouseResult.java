/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview;

import com.panayotis.jubler.media.preview.JSubTimeline.SubInfo;

import java.awt.*;

import static java.awt.Cursor.*;

enum MouseLocation {
    OUT(getPredefinedCursor(DEFAULT_CURSOR)), // The mouse is positioned over an empty area
    IN(getPredefinedCursor(HAND_CURSOR)), // The mouse is positioned over a subtitle entry
    LEFT(getPredefinedCursor(W_RESIZE_CURSOR)), // The mouse is positioned over the start of a subtitle entry
    RIGHT(getPredefinedCursor(E_RESIZE_CURSOR)); // The mouse is positioned over the end of a subtitle entry

    final Cursor cursor;

    MouseLocation(Cursor cursor) {
        this.cursor = cursor;
    }
}

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
