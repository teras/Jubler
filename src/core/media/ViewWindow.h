/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

// The visible time window of the preview's timeline/waveform. Port of
// `ViewWindow`; the zoom slider mapping is here too (pure maths, tested).
class ViewWindow {
public:
    static constexpr double MINIMUM_DURATION = 2;
    static constexpr int ZOOM_MAX = 100;
    // Show [start,end]; with doNotResize keep the current duration centred on
    // the range (unless the range is wider). Clamped to [0, videoDuration].
    // False, and nothing moves, while the window is held.
    bool setWindow(double start, double end, bool doNotResize);
    // Held while a subtitle is dragged on it: the drag measures its moves as
    // fractions of this window, so it must not move under them.
    void setHeld(bool held) { held_ = held; }
    bool isHeld() const { return held_; }
    void setVideoDuration(double d) { videoDuration_ = d; }
    double getVideoDuration() const { return videoDuration_; }
    double getStart() const { return start_; }
    double getDuration() const { return duration_; }

    // The zoom slider (0..ZOOM_MAX) and the window duration, logarithmically:
    // duration = 2·(videoDuration/2)^(value/ZOOM_MAX), so 0 is MINIMUM_DURATION
    // and ZOOM_MAX the whole video. The two are inverse of each other.
    static double zoomDuration(int value, double videoDuration);
    // -1: no mapping (the video duration is unknown or too short).
    static int zoomValue(double duration, double videoDuration);

private:
    double start_ = 0, duration_ = 10, videoDuration_ = -1;
    bool held_ = false;
};
