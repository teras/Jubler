/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.theme;

class RolloverIconFilter extends ColorIconFilter {

    private static float inc = 1.3f;

    public RolloverIconFilter() {
        super(inc, inc, inc);
    }
}
