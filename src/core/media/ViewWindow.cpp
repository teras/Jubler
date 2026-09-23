/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/media/ViewWindow.h"

#include <algorithm>
#include <cmath>

bool ViewWindow::setWindow(double start, double end, bool doNotResize) {
    if (held_) return false;
    if (end - start > duration_) doNotResize = false;
    if (doNotResize) {
        const double center = (start + end) / 2;
        start = center - duration_ / 2;
        end = center + duration_ / 2;
    }
    if (start < 0) {
        end -= start;
        start = 0;
    }
    if (end - start < MINIMUM_DURATION) end = start + MINIMUM_DURATION;
    if (videoDuration_ > 0 && end - start > videoDuration_) {
        start = 0;
        end = videoDuration_;
    }
    if (videoDuration_ > 0 && end > videoDuration_) {   // never past the end either
        start = std::max(0.0, start - (end - videoDuration_));
        end = videoDuration_;
    }
    start_ = start;
    duration_ = end - start;
    return true;
}

double ViewWindow::zoomDuration(int value, double videoDuration) {
    return 2 * std::pow(videoDuration / 2, double(value) / ZOOM_MAX);
}

int ViewWindow::zoomValue(double duration, double videoDuration) {
    if (videoDuration <= MINIMUM_DURATION || duration <= 0) return -1;
    const double v = ZOOM_MAX * std::log(duration / 2) / std::log(videoDuration / 2);
    return std::clamp(int(std::lround(v)), 0, ZOOM_MAX);
}
