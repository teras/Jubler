/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview;

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
