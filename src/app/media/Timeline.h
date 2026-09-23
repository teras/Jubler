/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QColor>
#include <QRectF>
#include <cmath>
#include <QList>
#include <QWidget>
#include <QTimer>
#include <QElapsedTimer>
#include <memory>

#include "core/media/ViewWindow.h"
#include "core/subs/Subtitles.h"

class MainWindow;
class Subtitles;
class SubPreview;
class WavePreview;

// Where the preview strips (waveform, timeline, ruler) draw a fraction of the
// view window: one mapping for all of them, so their boxes and lines meet on
// the same device pixel. Edges are rounded (a box is x(start)..x(end), so
// adjacent subtitles touch) and snapped to device pixels (sharp on HiDPI).
struct PixelMap {
    double width = 0, dpr = 1;
    explicit PixelMap(const QWidget *w) : width(w->width()), dpr(w->devicePixelRatioF()) {}
    double x(double fraction) const { return std::round(fraction * width * dpr) / dpr; }
    double onePixel() const { return 1.0 / dpr; }
    // The box from fraction a to b, `height` tall.
    QRectF span(double a, double b, double height) const {
        const double x1 = x(a), x2 = x(b);
        return QRectF(x1, 0, x2 - x1, height);
    }
};

// A subtitle as drawn: row index and start/end as fractions of the window;
// the initial fractions let drags apply as deltas. Port of `SubInfo`.
struct SubInfo {
    int pos = -1;   // -1: synthetic overlap rectangle
    double startPercent = 0, endPercent = 0;
    double initialStart = 0, initialEnd = 0;
    SubInfo() = default;
    SubInfo(int p, double s, double e) : pos(p), startPercent(s), endPercent(e), initialStart(s), initialEnd(e) {}
    void setDeltaStart(double d) { startPercent = initialStart + d; }
    void setDeltaEnd(double d) { endPercent = initialEnd + d; }
};

enum class MouseLocation { OUT, IN, LEFT, RIGHT };

// The subtitle bar: every subtitle of the window as a rectangle, selected
// ones highlighted, overlaps in red; mouse selection (Ctrl toggles, Shift
// extends), moving and edge resizing with snapping (Alt = free), wheel zoom.
// Port of `JSubTimeline`; the video key frames are drawn as thin lines and a
// dragged edge close to one sticks to it (an addition of the port).
class Timeline : public QWidget {
    Q_OBJECT
public:
    static const QColor BackColor, SubColor, SelectColor, OverlapColor;
    static constexpr int MARGIN = 4;   // px around an edge that hits it
    // Key-frame magnet (logical pixels): a dragged edge is caught this close to
    // a key frame and lets go this far past it.
    static constexpr int KEYFRAME_CATCH = 4;
    static constexpr int KEYFRAME_RELEASE = 10;

    Timeline(MainWindow *parent, ViewWindow *view, SubPreview *preview, QWidget *widget = nullptr);
    void setWavePreview(WavePreview *w) { wave_ = w; }
    void setSnap(bool snap) { snap_ = snap; }
    // Where the video is (seconds; negative: none), drawn as a line.
    void setPlayhead(double seconds) { playhead_ = seconds; update(); }
    double playhead() const { return playhead_; }
    // A subtitle or an edge is being dragged (the view must not move under it).
    bool isDragging() const { return mouseDown_; }
    static const QColor PlayheadColor;
    const QList<std::shared_ptr<SubInfo>> &getSelectedList() const { return selected_; }
    // Rebuild the lists for the current window; `rows` = the selection
    // (empty pointer = keep the current one).
    void windowHasChanged(const QList<int> *rows);
    void selectionHasChanged(const QList<int> *rows);
    double getSelectionStart() const;
    double getSelectionEnd() const;
    Subtitles *parentWindowSubtitles() const;
    // Forwarded from the waveform (same width, same fractions).
    void forwardMousePress(QMouseEvent *e) { mousePressEvent(e); }
    void forwardMouseMove(QMouseEvent *e) { mouseMoveEvent(e); }
    void forwardMouseRelease(QMouseEvent *e) { mouseReleaseEvent(e); }
    void forwardWheel(QWheelEvent *e) { wheelEvent(e); }

    QSize minimumSizeHint() const override { return QSize(20, 20); }
    QSize sizeHint() const override { return QSize(40, 40); }

protected:
    void paintEvent(QPaintEvent *) override;
    void mousePressEvent(QMouseEvent *e) override;
    void mouseMoveEvent(QMouseEvent *e) override;
    void mouseReleaseEvent(QMouseEvent *e) override;
    void wheelEvent(QWheelEvent *e) override;

private:
    using SubInfoPtr = std::shared_ptr<SubInfo>;
    struct Hit {
        MouseLocation location = MouseLocation::OUT;
        bool isSelected = false;
        SubInfoPtr info;
    };
    Hit findAnyAction(int x, bool mouseDown);
    Hit findAction(const QList<SubInfoPtr> &list, bool selected, int x);
    void updateCursor(const Hit &h, bool mouseDown);
    void calcOverlaps();
    double getOffsetTime() const;
    // Key frames of the window as fractions of it.
    QList<double> keyframeFractions() const;
    void repaintAll();
    // Every SubInfo except `self` of both lists.
    QList<SubInfoPtr> others(const SubInfoPtr &self) const;

    MainWindow *parent_;
    ViewWindow *view_;
    SubPreview *preview_;
    WavePreview *wave_ = nullptr;
    bool snap_ = true;
    double playhead_ = -1;
    bool mouseDown_ = false;
    bool dataHasChanged_ = false;
    int mouseDownPixels_ = -1;
    double leftSnap_ = 1, rightSnap_ = 1;
    // Key-frame magnet of a drag: the key frame and the edge (its place when
    // the drag began) stuck to it, and the previous unsnapped move (fractions;
    // NaN: none).
    double stuckKeyframe_ = std::nan(""), stuckEdge_ = std::nan(""), lastRawDelta_ = std::nan("");
    Hit current_;
    QList<SubInfoPtr> remaining_, selected_, overlaps_;
};

// The time ruler under the bar. Port of `JRuler`.
class Ruler : public QWidget {
    Q_OBJECT
public:
    explicit Ruler(ViewWindow *view, QWidget *parent = nullptr);
    QSize minimumSizeHint() const override { return QSize(20, height_); }
    QSize sizeHint() const override { return QSize(200, height_); }
    // A row click, a seek or play: a throw of the ruler stops.
    void stopInertia() { if (inertia_) inertia_->stop(); }

signals:
    // Dragged: the window should start at `start` seconds.
    void panRequested(double start);
    // The mouse wheel over the ruler: handled as over the timeline (zoom).
    void wheelTurned(QWheelEvent *e);

protected:
    void paintEvent(QPaintEvent *) override;
    void mousePressEvent(QMouseEvent *e) override;
    void mouseMoveEvent(QMouseEvent *e) override;
    void mouseReleaseEvent(QMouseEvent *e) override;
    void wheelEvent(QWheelEvent *e) override { emit wheelTurned(e); }

private:
    void inertiaStep();

    ViewWindow *view_;
    int height_;
    double pressX_ = -1, pressStart_ = 0;   // the drag: pointer and window start at the press
    // Inertia after the release: recent pointer positions (ms, x) and the
    // velocity in seconds of time per second, decaying with friction.
    QList<QPair<qint64, double>> samples_;
    QElapsedTimer clock_;
    QTimer *inertia_ = nullptr;
    double velocity_ = 0;
    qint64 lastStep_ = 0;
};
