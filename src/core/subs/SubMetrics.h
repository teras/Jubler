/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <limits>

// Quality metrics of one entry, port of the Java `SubMetrics`.
struct SubMetrics {
    int length = 0;        // counted characters (see Options space/newline/other flags)
    int linelength = 0;    // longest line
    int lines = 1;
    float cps = 0;         // characters per second; +inf for a zero duration
    int fillpercent = 50;  // shortest line / longest line, in percent
};

// Extremes and totals over a whole document, port of `TotalSubMetrics`.
struct TotalSubMetrics {
    int maxlength = 0, minlength = std::numeric_limits<int>::max();
    int maxlines = 0, minlines = std::numeric_limits<int>::max();
    int maxlinelength = 0, minlinelength = std::numeric_limits<int>::max();
    float maxcps = 0, mincps = std::numeric_limits<float>::max();
    float maxcpm = 0, mincpm = std::numeric_limits<float>::max();
    int maxfillpercent = 0, minfillpercent = std::numeric_limits<int>::max();
    float maxduration = 0, minduration = std::numeric_limits<float>::max();
    int totallength = 0, totallines = 0;

    void updateToMaxValues(const SubMetrics &m) {
        if (maxlength < m.length) maxlength = m.length;
        if (minlength > m.length) minlength = m.length;
        if (maxlines < m.lines) maxlines = m.lines;
        if (minlines > m.lines) minlines = m.lines;
        if (maxlinelength < m.linelength) maxlinelength = m.linelength;
        if (minlinelength > m.linelength) minlinelength = m.linelength;
        if (maxcps < m.cps) maxcps = m.cps;
        if (mincps > m.cps) mincps = m.cps;
        const float cpm = m.cps * 60;
        if (maxcpm < cpm) maxcpm = cpm;
        if (mincpm > cpm) mincpm = cpm;
        if (maxfillpercent < m.fillpercent) maxfillpercent = m.fillpercent;
        if (minfillpercent > m.fillpercent) minfillpercent = m.fillpercent;
        totallength += m.length;
        totallines += m.lines;
    }
    void updateDuration(float duration) {
        if (maxduration < duration) maxduration = duration;
        if (minduration > duration) minduration = duration;
    }
};
