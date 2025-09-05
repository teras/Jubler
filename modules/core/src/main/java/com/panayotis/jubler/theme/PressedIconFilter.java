/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.theme;

class PressedIconFilter extends ColorIconFilter {

    private static float inc = 0.7f;

    public PressedIconFilter() {
        super(inc, inc, inc);
    }
}
