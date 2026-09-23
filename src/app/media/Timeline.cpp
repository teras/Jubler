/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/media/AppMediaFile.h"
#include "app/media/Timeline.h"

#include <QMouseEvent>
#include <QPainter>
#include <QWheelEvent>
#include <algorithm>
#include <cmath>

#include "app/media/SubPreview.h"
#include "app/media/WavePreview.h"
#include "app/ui/MainWindow.h"
#include "core/i18n/I18N.h"
#include "core/subs/Subtitles.h"
#include "core/undo/UndoList.h"

// ---- Timeline --------------------------------------------------------------------------------

const QColor Timeline::BackColor = Qt::black;
const QColor Timeline::SubColor(30, 240, 50);
const QColor Timeline::SelectColor(20, 140, 255);
const QColor Timeline::OverlapColor(230, 50, 20);
const QColor Timeline::PlayheadColor(255, 60, 60);

namespace {
bool numbersEqual(double a, double b) {
    return std::abs(a - b) <= 1e-7 * std::max(1.0, std::max(std::abs(a), std::abs(b)));
}
// Sunken/raised 3D rectangle in the Swing sense, its one-pixel border inside
// the box (`px` = one device pixel).
void fill3D(QPainter &p, const QRectF &r, const QColor &c, bool raised, double px) {
    if (r.width() <= 0 || r.height() <= 0) return;
    p.fillRect(r, c);
    const QColor light = c.lighter(140), dark = c.darker(140);
    const QColor topLeft = raised ? light : dark, bottomRight = raised ? dark : light;
    p.fillRect(QRectF(r.left(), r.top(), r.width(), px), topLeft);
    p.fillRect(QRectF(r.left(), r.top(), px, r.height()), topLeft);
    p.fillRect(QRectF(r.left(), r.bottom() - px, r.width(), px), bottomRight);
    p.fillRect(QRectF(r.right() - px, r.top(), px, r.height()), bottomRight);
}
}  // namespace

Timeline::Timeline(MainWindow *parent, ViewWindow *view, SubPreview *preview, QWidget *widget)
    : QWidget(widget), parent_(parent), view_(view), preview_(preview) {
    setMouseTracking(true);
    setMinimumSize(20, 20);
}

void Timeline::repaintAll() {
    update();
    if (wave_) wave_->update();
}

QList<Timeline::SubInfoPtr> Timeline::others(const SubInfoPtr &self) const {
    QList<SubInfoPtr> out;
    for (const auto &s : selected_) if (s != self) out.append(s);
    for (const auto &s : remaining_) if (s != self) out.append(s);
    return out;
}

Timeline::Hit Timeline::findAction(const QList<SubInfoPtr> &list, bool selected, int x) {
    const double w = width();
    for (const SubInfoPtr &info : list) {
        const double sp = info->startPercent * w, ep = info->endPercent * w;
        if (std::abs(ep - x) <= MARGIN) {
            // A zero-length subtitle butted against a following one can only
            // be expanded to the left.
            if (numbersEqual(info->startPercent, info->endPercent))
                for (const SubInfoPtr &o : others(info))
                    if (numbersEqual(o->startPercent, info->endPercent)) return {MouseLocation::LEFT, selected, info};
            return {MouseLocation::RIGHT, selected, info};
        }
        if (std::abs(sp - x) <= MARGIN) return {MouseLocation::LEFT, selected, info};
        const double f = x / w;
        if (f >= info->startPercent && f <= info->endPercent) return {MouseLocation::IN, selected, info};
    }
    return {};
}

void Timeline::updateCursor(const Hit &h, bool mouseDown) {
    Qt::CursorShape c = Qt::ArrowCursor;
    switch (h.location) {
        case MouseLocation::OUT: c = Qt::ArrowCursor; break;
        case MouseLocation::IN: c = mouseDown ? Qt::SizeAllCursor : Qt::PointingHandCursor; break;
        case MouseLocation::LEFT: c = Qt::SizeHorCursor; break;
        case MouseLocation::RIGHT: c = Qt::SizeHorCursor; break;
    }
    setCursor(c);
    if (wave_) wave_->setCursor(c);
}

Timeline::Hit Timeline::findAnyAction(int x, bool mouseDown) {
    Hit h = findAction(selected_, true, x);
    if (h.location == MouseLocation::OUT) h = findAction(remaining_, false, x);
    updateCursor(h, mouseDown);
    return h;
}

void Timeline::mouseMoveEvent(QMouseEvent *e) {
    if (!mouseDown_) {
        findAnyAction(int(e->position().x()), false);
        return;
    }
    // Drag.
    dataHasChanged_ = true;
    double dt = double(int(e->position().x()) - mouseDownPixels_) / width();
    const bool noSnap = e->modifiers() & Qt::AltModifier;
    if (!noSnap) dt = dt < 0 ? std::max(dt, -leftSnap_) : std::min(dt, rightSnap_);
    // Key frames are magnets (never past the neighbour limits above): a dragged
    // edge — or, for subtitles dragged by their middle, the first start —
    // is caught when it comes within a few pixels of one or jumps across it,
    // and stays there until the pointer is well past.
    if (snap_ && !noSnap && current_.info && current_.location != MouseLocation::OUT) {
        QList<double> edges;   // where the moving edges were when the drag began
        if (current_.location == MouseLocation::LEFT) edges = {current_.info->initialStart};
        else if (current_.location == MouseLocation::RIGHT) edges = {current_.info->initialEnd};
        else if (!selected_.isEmpty()) {
            // Only the start: a subtitle's end follows its text, not the picture.
            double first = 1e300;
            for (const SubInfoPtr &s : selected_) first = std::min(first, std::min(s->initialStart, s->initialEnd));
            edges = {first};
        }
        const double px = 1.0 / width();   // logical pixels, as the rest of the UI
        const auto allowed = [&](double f, double edge) { return f - edge >= -leftSnap_ && f - edge <= rightSnap_; };
        if (!std::isnan(stuckKeyframe_) &&
            (std::abs(stuckEdge_ + dt - stuckKeyframe_) > KEYFRAME_RELEASE * px || !allowed(stuckKeyframe_, stuckEdge_)))
            stuckKeyframe_ = std::nan("");
        if (std::isnan(stuckKeyframe_)) {
            double best = 1e300;
            const QList<double> keyframes = keyframeFractions();
            for (const double edge : edges) {
                const double raw = edge + dt;
                for (const double f : keyframes) {
                    const bool near = std::abs(raw - f) <= KEYFRAME_CATCH * px;
                    const bool crossed = !std::isnan(lastRawDelta_) && (edge + lastRawDelta_ - f) * (raw - f) < 0;
                    if ((near || crossed) && allowed(f, edge) && std::abs(raw - f) < best) {
                        best = std::abs(raw - f);
                        stuckKeyframe_ = f;
                        stuckEdge_ = edge;
                    }
                }
            }
            // Jumped so far across it that it would let go at once: pass.
            if (!std::isnan(stuckKeyframe_) && best > KEYFRAME_RELEASE * px) stuckKeyframe_ = std::nan("");
        }
        lastRawDelta_ = dt;
        if (!std::isnan(stuckKeyframe_)) dt = stuckKeyframe_ - stuckEdge_;
    }
    if (current_.location == MouseLocation::IN) {
        for (const SubInfoPtr &inf : selected_) {
            inf->setDeltaStart(dt);
            inf->setDeltaEnd(dt);
        }
    } else if (current_.location == MouseLocation::LEFT && current_.info) {
        // The start never crosses the end, even without snapping.
        dt = std::min(dt, current_.info->initialEnd - current_.info->initialStart);
        current_.info->setDeltaStart(dt);
    } else if (current_.location == MouseLocation::RIGHT && current_.info) {
        dt = std::max(dt, current_.info->initialStart - current_.info->initialEnd);
        current_.info->setDeltaEnd(dt);
    }
    calcOverlaps();
    repaintAll();
    preview_->updateSelectedTime();
}

void Timeline::mousePressEvent(QMouseEvent *e) {
    if (e->button() != Qt::LeftButton) return;
    mouseDownPixels_ = int(e->position().x());
    mouseDown_ = true;
    view_->setHeld(true);   // the drag is measured on this window
    current_ = findAnyAction(mouseDownPixels_, true);
    const bool keepSelection = e->modifiers() & Qt::ControlModifier;
    const bool extend = e->modifiers() & Qt::ShiftModifier;
    if (current_.location == MouseLocation::OUT) {
        if (!keepSelection && !extend && !selected_.isEmpty()) {
            remaining_.append(selected_);
            selected_.clear();
        } else
            return;   // nothing to do
    } else if (extend) {
        // Select every subtitle between the selection and the clicked one.
        Subtitles *subs = parent_->getSubtitles();
        int lo = current_.info->pos, hi = current_.info->pos;
        for (const SubInfoPtr &s : selected_) {
            lo = std::min(lo, s->pos);
            hi = std::max(hi, s->pos);
        }
        for (int row = lo; row <= hi && subs && row < subs->size(); ++row) {
            bool present = false;
            for (const SubInfoPtr &s : selected_) if (s->pos == row) { present = true; break; }
            if (present) continue;
            SubInfoPtr found;
            for (const SubInfoPtr &s : remaining_) if (s->pos == row) { found = s; break; }
            if (found) {
                remaining_.removeOne(found);
                selected_.append(found);
            } else {
                const SubEntryPtr entry = subs->get(row);
                const double vs = view_->getStart(), vd = view_->getDuration();
                selected_.append(std::make_shared<SubInfo>(row, (entry->getStartTime().toSeconds() - vs) / vd, (entry->getFinishTime().toSeconds() - vs) / vd));
            }
        }
        current_.isSelected = true;
    } else if (keepSelection) {
        if (current_.isSelected) {
            if (current_.location == MouseLocation::IN) {
                selected_.removeOne(current_.info);
                remaining_.append(current_.info);
            }
        } else {
            selected_.append(current_.info);
            remaining_.removeOne(current_.info);
        }
    } else if (!current_.isSelected) {
        remaining_.append(selected_);
        remaining_.removeOne(current_.info);
        selected_.clear();
        selected_.append(current_.info);
    }

    leftSnap_ = 1;
    rightSnap_ = 1;
    stuckKeyframe_ = stuckEdge_ = lastRawDelta_ = std::nan("");
    if (snap_ && current_.info) {
        const SubInfoPtr &cur = current_.info;
        if (current_.location == MouseLocation::LEFT) {
            rightSnap_ = cur->endPercent - cur->startPercent;
            leftSnap_ = std::min(leftSnap_, cur->startPercent);
            for (const SubInfoPtr &o : others(cur)) {
                const double d = cur->startPercent - o->endPercent;
                if (d >= 0 && d < leftSnap_) leftSnap_ = d;
            }
        } else if (current_.location == MouseLocation::RIGHT) {
            leftSnap_ = cur->endPercent - cur->startPercent;
            rightSnap_ = std::min(rightSnap_, 1 - cur->endPercent);
            for (const SubInfoPtr &o : others(cur)) {
                const double d = o->startPercent - cur->endPercent;
                if (d >= 0 && d < rightSnap_) rightSnap_ = d;
            }
        } else if (current_.location == MouseLocation::IN) {
            for (const SubInfoPtr &sel : selected_) {
                leftSnap_ = std::min(leftSnap_, sel->startPercent);
                rightSnap_ = std::min(rightSnap_, 1 - sel->endPercent);
                for (const SubInfoPtr &o : remaining_) {
                    const double l = sel->startPercent - o->endPercent;
                    if (l >= 0 && l < leftSnap_) leftSnap_ = l;
                    const double r = o->startPercent - sel->endPercent;
                    if (r >= 0 && r < rightSnap_) rightSnap_ = r;
                }
            }
        }
    }
    calcOverlaps();
    repaintAll();
}

void Timeline::mouseReleaseEvent(QMouseEvent *e) {
    if (e->button() != Qt::LeftButton) return;
    findAnyAction(int(e->position().x()), false);
    mouseDownPixels_ = -1;
    leftSnap_ = rightSnap_ = -1;
    mouseDown_ = false;
    view_->setHeld(false);   // the reselection below places the view
    QList<int> sel;
    for (const SubInfoPtr &s : selected_) sel.append(s->pos);
    if (dataHasChanged_ && !selected_.isEmpty() && current_.location != MouseLocation::OUT) {
        Subtitles *subs = parent_->getSubtitles();
        const double offset = getOffsetTime();
        parent_->getUndoList()->addUndo(*subs, __("Subtitle time changes"));
        for (const SubInfoPtr &info : selected_) {
            const SubEntryPtr entry = subs->get(info->pos);
            entry->setStartTime(Time(view_->getStart() + view_->getDuration() * info->startPercent + offset));
            entry->setFinishTime(Time(view_->getStart() + view_->getDuration() * info->endPercent + offset));
        }
        for (int pos : sel) parent_->rowHasChanged(pos, false);
        // The player must know the new times before the reselection seeks.
        preview_->reloadSubtitlesNow();
    }
    dataHasChanged_ = false;
    parent_->setSelectedSub(sel, true);
}

void Timeline::wheelEvent(QWheelEvent *e) {
    // Not under a drag: its fractions would be applied to the zoomed window.
    if (mouseDown_) {
        e->accept();
        return;
    }
    const int notches = -e->angleDelta().y() / 120;
    if (notches != 0) preview_->zoomBy(notches);
    e->accept();
}

double Timeline::getOffsetTime() const {
    double min = 1e300;
    for (const SubInfoPtr &s : selected_) min = std::min(min, s->startPercent);
    if (selected_.isEmpty()) return 0;
    const double t = view_->getStart() + view_->getDuration() * min;
    return t < 0 ? -t : 0;
}

void Timeline::selectionHasChanged(const QList<int> *rows) {
    QList<int> ids;
    if (rows)
        ids = *rows;
    else
        for (const SubInfoPtr &s : selected_) ids.append(s->pos);
    selected_.clear();
    Subtitles *subs = parent_->getSubtitles();
    if (!subs) return;
    const double vs = view_->getStart(), vd = view_->getDuration();
    for (int row : ids) {
        if (row < 0 || row >= subs->size()) continue;
        const SubEntryPtr e = subs->get(row);
        selected_.append(std::make_shared<SubInfo>(row, (e->getStartTime().toSeconds() - vs) / vd, (e->getFinishTime().toSeconds() - vs) / vd));
        for (int i = remaining_.size() - 1; i >= 0; --i)
            if (remaining_[i]->pos == row) remaining_.removeAt(i);
    }
}

void Timeline::windowHasChanged(const QList<int> *rows) {
    if (mouseDown_) return;   // never under a drag (its SubInfos carry the deltas); the release reselects
    remaining_.clear();
    selectionHasChanged(rows);
    Subtitles *subs = parent_->getSubtitles();
    if (subs) {
        const double vs = view_->getStart(), vd = view_->getDuration(), ve = vs + vd;
        for (int i = 0; i < subs->size(); ++i) {
            const SubEntryPtr e = subs->get(i);
            const double s = e->getStartTime().toSeconds(), f = e->getFinishTime().toSeconds();
            if (s < ve && f > vs) {
                bool isSel = false;
                for (const SubInfoPtr &si : selected_) if (si->pos == i) { isSel = true; break; }
                if (!isSel) remaining_.append(std::make_shared<SubInfo>(i, (s - vs) / vd, (f - vs) / vd));
            }
        }
    }
    calcOverlaps();
    update();
}

Subtitles *Timeline::parentWindowSubtitles() const { return parent_->getSubtitles(); }

double Timeline::getSelectionStart() const {
    if (selected_.isEmpty()) return view_->getStart();
    return view_->getStart() + view_->getDuration() * selected_.first()->startPercent;
}

double Timeline::getSelectionEnd() const {
    if (selected_.isEmpty()) return view_->getStart();
    return view_->getStart() + view_->getDuration() * selected_.last()->endPercent;
}

void Timeline::calcOverlaps() {
    overlaps_.clear();
    auto check = [this](const SubInfoPtr &a, const SubInfoPtr &b) {
        if (a->startPercent < b->endPercent && a->endPercent > b->startPercent)
            overlaps_.append(std::make_shared<SubInfo>(-1, std::max(a->startPercent, b->startPercent), std::min(a->endPercent, b->endPercent)));
    };
    for (int i = 0; i < remaining_.size(); ++i) {
        for (int j = i + 1; j < remaining_.size(); ++j) check(remaining_[i], remaining_[j]);
        for (const SubInfoPtr &s : selected_) check(remaining_[i], s);
    }
    for (int i = 0; i < selected_.size(); ++i)
        for (int j = i + 1; j < selected_.size(); ++j) check(selected_[i], selected_[j]);
}

QList<double> Timeline::keyframeFractions() const {
    QList<double> out;
    const AppMediaFile *m = preview_ ? preview_->mediaFile() : nullptr;
    const double start = view_->getStart(), duration = view_->getDuration();
    if (!m || duration <= 0) return out;
    for (const double t : m->keyframes(start, start + duration)) out.append((t - start) / duration);
    return out;
}

void Timeline::paintEvent(QPaintEvent *) {
    QPainter p(this);
    const double h = height();
    const PixelMap map(this);
    p.fillRect(rect(), BackColor);
    for (const double f : keyframeFractions()) p.fillRect(QRectF(map.x(f), 0, map.onePixel(), h), QColor(255, 255, 255, 90));
    for (const SubInfoPtr &s : remaining_)
        fill3D(p, map.span(s->startPercent, s->endPercent, h), SubColor, true, map.onePixel());
    for (const SubInfoPtr &s : selected_) {
        double a = s->startPercent, b = s->endPercent;
        if (a > b) std::swap(a, b);
        fill3D(p, map.span(a, b, h), SelectColor, false, map.onePixel());
    }
    for (const SubInfoPtr &s : overlaps_)
        p.fillRect(map.span(s->startPercent, s->endPercent, h), OverlapColor);
    if (playhead_ >= 0 && view_->getDuration() > 0)
        p.fillRect(QRectF(map.x((playhead_ - view_->getStart()) / view_->getDuration()), 0, map.onePixel(), h), PlayheadColor);
}

// ---- Ruler -----------------------------------------------------------------------------------

namespace {
const double TICK_SIZE[] = {0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000};
const int TICK_SKIP[] = {10, 5, 2};
}  // namespace

Ruler::Ruler(ViewWindow *view, QWidget *parent) : QWidget(parent), view_(view), height_(30) {
    setFixedHeight(height_);
    setCursor(Qt::OpenHandCursor);   // it can be dragged to pan the view
    inertia_ = new QTimer(this);
    inertia_->setInterval(16);
    connect(inertia_, &QTimer::timeout, this, &Ruler::inertiaStep);
    clock_.start();
}

namespace {
constexpr qint64 VELOCITY_WINDOW_MS = 100;   // the drag's last moments give the throw speed
constexpr double FRICTION = 6.0;             // velocity × e^(−FRICTION·t): stops in well under a second
constexpr double MIN_SPEED_PX = 20;          // px/s: slower than this is a stop, not a throw
}  // namespace

void Ruler::inertiaStep() {
    const qint64 now = clock_.elapsed();
    const double dt = (now - lastStep_) / 1000.0;
    lastStep_ = now;
    velocity_ *= std::exp(-FRICTION * dt);
    const double pxPerSecond = std::abs(velocity_) * width() / view_->getDuration();
    const double before = view_->getStart();
    emit panRequested(before + velocity_ * dt);
    // Slow enough, or held by an end of the video: done.
    if (pxPerSecond < MIN_SPEED_PX || view_->getStart() == before) inertia_->stop();
}

// Grab the time and drag it: dragging right shows earlier times.
void Ruler::mousePressEvent(QMouseEvent *e) {
    if (e->button() != Qt::LeftButton) return;
    inertia_->stop();   // a touch stops a throw
    pressX_ = e->position().x();
    pressStart_ = view_->getStart();
    samples_ = {{clock_.elapsed(), pressX_}};
    setCursor(Qt::ClosedHandCursor);
}

void Ruler::mouseMoveEvent(QMouseEvent *e) {
    if (pressX_ < 0 || width() <= 0) return;
    const qint64 now = clock_.elapsed();
    samples_.append({now, e->position().x()});
    while (samples_.size() > 2 && now - samples_.first().first > VELOCITY_WINDOW_MS) samples_.removeFirst();
    emit panRequested(pressStart_ - (e->position().x() - pressX_) * view_->getDuration() / width());
}

void Ruler::mouseReleaseEvent(QMouseEvent *e) {
    if (e->button() != Qt::LeftButton) return;
    // The throw: the pointer's speed over the drag's last moments (none when
    // the hand stopped before letting go).
    const qint64 now = clock_.elapsed();
    samples_.append({now, e->position().x()});
    while (samples_.size() > 2 && now - samples_.first().first > VELOCITY_WINDOW_MS) samples_.removeFirst();
    const double span = (samples_.last().first - samples_.first().first) / 1000.0;
    const double pxPerSecond = span > 0 ? (samples_.last().second - samples_.first().second) / span : 0;
    const bool fresh = now - samples_.first().first <= VELOCITY_WINDOW_MS * 2;
    pressX_ = -1;
    samples_.clear();
    setCursor(Qt::OpenHandCursor);
    if (fresh && std::abs(pxPerSecond) >= MIN_SPEED_PX && width() > 0) {
        velocity_ = -pxPerSecond * view_->getDuration() / width();   // dragging right shows earlier times
        lastStep_ = now;
        inertia_->start();
    }
}

void Ruler::paintEvent(QPaintEvent *) {
    QPainter p(this);
    p.fillRect(rect(), Qt::lightGray);
    p.setPen(QColor(64, 64, 64));
    const int w = width(), h = height();
    if (w <= 0) return;
    const double value = 4 * view_->getDuration() / w;   // a tick at least 4 px apart
    const int count = int(sizeof(TICK_SIZE) / sizeof(TICK_SIZE[0]));
    int scale = 0;
    while (scale < count - 1 && value > TICK_SIZE[scale++]) {}
    const double minor = TICK_SIZE[scale];
    const int skip = TICK_SKIP[scale % 3];
    const double first = std::floor(view_->getStart() / minor) * minor;
    const double last = std::ceil((view_->getStart() + view_->getDuration()) / minor) * minor;
    long current = std::lround(first / minor);
    const PixelMap map(this);
    const QColor tick(64, 64, 64);
    for (double t = first; t <= last + minor / 2; t += minor, ++current) {
        const double x = map.x((t - view_->getStart()) / view_->getDuration());
        const auto line = [&](double len) { p.fillRect(QRectF(x, 0, map.onePixel(), len), tick); };
        if (current % skip == 0) {
            if (current % 10 == 0) {
                line(h * 3 / 4);
                QString label = QString::number(t, 'f', 2);
                for (int i = 0; i < 2 && label.endsWith(QLatin1Char('0')); ++i) label.chop(1);
                if (!label.isEmpty() && !label.back().isDigit()) label.chop(1);
                p.drawText(QPointF(x + 1, h - 1), label);
            } else
                line(h / 2);
        } else
            line(h / 4);
    }
}
