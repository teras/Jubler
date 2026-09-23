/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

// The preview's window maths: ViewWindow clamps, its hold under a drag and the zoom slider mapping.
#include "TestSupport.h"

#include "core/media/ViewWindow.h"

static bool near(double a, double b) { return std::abs(a - b) < 1e-9; }

static void testClamps() {
    ViewWindow w;
    w.setVideoDuration(100);
    w.setWindow(-5, 15, false);   // before the start: shifted, size kept
    CHECK(near(w.getStart(), 0) && near(w.getDuration(), 20), "shifted to 0");
    w.setWindow(90, 110, false);   // past the end: shifted back, size kept
    CHECK(near(w.getStart(), 80) && near(w.getDuration(), 20), "shifted back from the end");
    w.setWindow(-10, 200, false);   // wider than the video: the whole video
    CHECK(near(w.getStart(), 0) && near(w.getDuration(), 100), "whole video");
    w.setWindow(50, 50.5, false);   // narrower than the minimum
    CHECK(near(w.getStart(), 50) && near(w.getDuration(), ViewWindow::MINIMUM_DURATION), "minimum duration");
    w.setWindow(10, 30, false);
    w.setWindow(58, 62, true);   // keep the size (20), centred on the range
    CHECK(near(w.getStart(), 50) && near(w.getDuration(), 20), "doNotResize keeps the size");
    w.setWindow(0, 40, true);   // a wider range resizes anyway
    CHECK(near(w.getStart(), 0) && near(w.getDuration(), 40), "doNotResize with a wider range");
}

static void testUnknownDuration() {
    ViewWindow w;   // no video duration yet: no upper clamp, no collapse
    w.setWindow(100, 130, false);
    CHECK(near(w.getStart(), 100) && near(w.getDuration(), 30), "no clamp without a duration");
    w.setVideoDuration(110);   // the duration arrives: a re-clamp keeps the size
    w.setWindow(w.getStart(), w.getStart() + w.getDuration(), false);
    CHECK(near(w.getStart(), 80) && near(w.getDuration(), 30), "re-clamped to the new duration");
}

// Held under a subtitle drag: nothing moves it, not even a new duration.
static void testHeld() {
    ViewWindow w;
    w.setVideoDuration(100);
    w.setWindow(40, 60, false);
    w.setHeld(true);
    CHECK(!w.setWindow(0, 10, false), "held: the move is refused");
    CHECK(near(w.getStart(), 40) && near(w.getDuration(), 20), "held: the window stays");
    w.setVideoDuration(50);   // the media got shorter meanwhile
    CHECK(!w.setWindow(w.getStart(), w.getStart() + w.getDuration(), false), "held: no re-clamp either");
    CHECK(near(w.getStart(), 40) && near(w.getDuration(), 20), "held: still where the drag started");
    w.setHeld(false);
    CHECK(w.setWindow(w.getStart(), w.getStart() + w.getDuration(), false), "released: it moves again");
    CHECK(near(w.getStart(), 30) && near(w.getDuration(), 20), "released: clamped to the new length");
}

static void testZoomMapping() {
    const double video = 3600;
    CHECK(near(ViewWindow::zoomDuration(0, video), ViewWindow::MINIMUM_DURATION), "value 0: the minimum");
    CHECK(near(ViewWindow::zoomDuration(ViewWindow::ZOOM_MAX, video), video), "value MAX: the whole video");
    for (int v = 0; v <= ViewWindow::ZOOM_MAX; ++v)
        CHECK_EQ(ViewWindow::zoomValue(ViewWindow::zoomDuration(v, video), video), v, "zoom mapping is inverse");
    // A longer window is a higher value (the wheel never zooms the wrong way).
    CHECK(ViewWindow::zoomValue(100, video) > ViewWindow::zoomValue(10, video), "monotonic");
    CHECK_EQ(ViewWindow::zoomValue(1, video), 0, "below the minimum clamps to 0");
    CHECK_EQ(ViewWindow::zoomValue(2 * video, video), ViewWindow::ZOOM_MAX, "above the video clamps to MAX");
    CHECK_EQ(ViewWindow::zoomValue(10, -1), -1, "unknown video duration: no mapping");
    CHECK_EQ(ViewWindow::zoomValue(10, 2), -1, "video no longer than the minimum: no mapping");
}

int main(int argc, char **argv) {
    QCoreApplication app(argc, argv);
    testClamps();
    testUnknownDuration();
    testHeld();
    testZoomMapping();
    return testFinish("test_preview");
}
