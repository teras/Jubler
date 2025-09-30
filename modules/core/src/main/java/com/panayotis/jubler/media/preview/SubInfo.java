/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview;

class SubInfo {

    final int pos;
    double startPercent, endPercent;
    private final double initialStartPercent, initialEndPercent;

    public SubInfo(int p, double s, double e) {
        pos = p;
        startPercent = initialStartPercent = s;
        endPercent = initialEndPercent = e;
    }

    public void setDeltaStartPercent(double delta) {
        startPercent = initialStartPercent + delta;
    }

    public void setDeltaEndPercent(double delta) {
        endPercent = initialEndPercent + delta;
    }
}
