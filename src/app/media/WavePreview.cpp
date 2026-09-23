/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/media/WavePreview.h"

#include <QHBoxLayout>
#include <QMouseEvent>
#include <QPainter>
#include <QProgressBar>
#include <QPushButton>
#include <QVBoxLayout>

#include "app/media/AppMediaFile.h"
#include "app/media/Timeline.h"
#include "core/i18n/I18N.h"
#include "core/subs/Subtitles.h"

namespace {
// java.awt.Color darker()/brighter() (factor 0.7, alpha kept).
QColor javaDarker(const QColor &c) {
    return QColor(std::max(int(c.red() * 0.7), 0), std::max(int(c.green() * 0.7), 0), std::max(int(c.blue() * 0.7), 0), c.alpha());
}
QColor javaBrighter(const QColor &c) {
    int r = c.red(), g = c.green(), b = c.blue();
    const int i = int(1.0 / (1.0 - 0.7));
    if (r == 0 && g == 0 && b == 0) return QColor(i, i, i, c.alpha());
    if (r > 0 && r < i) r = i;
    if (g > 0 && g < i) g = i;
    if (b > 0 && b < i) b = i;
    return QColor(std::min(int(r / 0.7), 255), std::min(int(g / 0.7), 255), std::min(int(b / 0.7), 255), c.alpha());
}
// Graphics.fill3DRect(x, y, w, h, raised = false): a sunken box; its border is
// one device pixel (`px`) inside the box.
void fill3DRect(QPainter &p, const QRectF &r, const QColor &c, double px) {
    const QColor dark = javaDarker(c), bright = javaBrighter(c);
    const double x = r.x(), y = r.y(), w = r.width(), h = r.height();
    auto rect = [&p](double rx, double ry, double rw, double rh, const QColor &col) {
        if (rw > 0 && rh > 0) p.fillRect(QRectF(rx, ry, rw, rh), col);   // AWT draws nothing for empty sizes
    };
    rect(x + px, y + px, w - 2 * px, h - 2 * px, dark);
    rect(x, y, px, h, dark);
    rect(x + px, y, w - 2 * px, px, dark);
    rect(x + px, y + h - px, w - px, px, bright);
    rect(x + w - px, y, px, h - px, bright);
}

const QColor BackEven(0, 20, 0), BackOdd(0, 20, 20);
const QColor WaveColor(150, 220, 150);
constexpr double DT = 0.002;
}  // namespace

// One channel strip.
class WavePreview::Channel : public QWidget {
public:
    Channel(WavePreview *owner, int index) : QWidget(owner), owner_(owner), index_(index) {
        setMouseTracking(true);
        setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
    }

protected:
    void paintEvent(QPaintEvent *) override {
        QPainter p(this);
        const int w = width(), h = height();
        p.fillRect(0, 0, w - 1, h - 1, index_ % 2 == 0 ? BackEven : BackOdd);
        // Selected subtitles first (behind the peaks), placed as on the timeline.
        const PixelMap map(this);
        for (const auto &s : owner_->timeline_->getSelectedList()) {
            double a = s->startPercent, b = s->endPercent;
            if (a > b) std::swap(a, b);
            fill3DRect(p, map.span(a, b, h), Timeline::SelectColor, map.onePixel());
        }
        p.setPen(Qt::white);
        p.drawRect(0, 0, w - 1, h - 1);
        if (const AudioPreviewData *d = owner_->data_.get()) {
            if (index_ < d->channels) {
                for (int i = 0; i < AudioPreviewData::LENGTH; ++i) {
                    const double x1 = map.x(double(i) / AudioPreviewData::LENGTH), x2 = map.x(double(i + 1) / AudioPreviewData::LENGTH);
                    // Positive samples upward: 1 → top, 0 → bottom.
                    int y1 = int(h * (1 - d->maxOf(index_, i))), y2 = int(h * (1 - d->minOf(index_, i)));
                    if (y1 > y2) std::swap(y1, y2);
                    p.fillRect(QRectF(x1, y1, std::max(map.onePixel(), x2 - x1), std::max(1, y2 - y1)), WaveColor);
                }
            }
        }
        p.setPen(Qt::lightGray);
        p.drawLine(1, h / 2, w - 1, h / 2);
        const double start = owner_->start_, span = owner_->end_ - owner_->start_, head = owner_->timeline_->playhead();
        if (head >= 0 && span > 0)
            p.fillRect(QRectF(map.x((head - start) / span), 0, map.onePixel(), h), Timeline::PlayheadColor);
    }
    void mousePressEvent(QMouseEvent *e) override { owner_->timeline_->forwardMousePress(e); }
    void mouseMoveEvent(QMouseEvent *e) override { owner_->timeline_->forwardMouseMove(e); }
    void mouseReleaseEvent(QMouseEvent *e) override { owner_->timeline_->forwardMouseRelease(e); }
    void wheelEvent(QWheelEvent *e) override { owner_->timeline_->forwardWheel(e); }

private:
    WavePreview *owner_;
    int index_;
};

WavePreview::WavePreview(Timeline *timeline, QWidget *parent) : QWidget(parent), timeline_(timeline) {
    layout_ = new QVBoxLayout(this);
    layout_->setContentsMargins(0, 0, 0, 0);
    layout_->setSpacing(0);
    loaderRow_ = new QWidget(this);
    auto *lr = new QHBoxLayout(loaderRow_);
    lr->setContentsMargins(4, 2, 4, 2);
    progress_ = new QProgressBar(loaderRow_);
    progress_->setRange(0, 100);
    progress_->setTextVisible(true);
    auto *cancel = new QPushButton(__("Cancel"), loaderRow_);
    connect(cancel, &QPushButton::clicked, this, [this]() { if (mfile_) mfile_->cancelPeaks(); });
    lr->addWidget(progress_, 1);
    lr->addWidget(cancel);
    loaderRow_->hide();
    layout_->addWidget(loaderRow_);
    timeline_->setWavePreview(this);
    updateWave();
}

QSize WavePreview::sizeHint() const { return QSize(50, 50); }
QSize WavePreview::minimumSizeHint() const { return QSize(10, 20); }

void WavePreview::updateMediaFile(AppMediaFile *mfile) {
    mfile_ = mfile;
}

void WavePreview::setTime(double start, double end) {
    if (std::abs(start - start_) < DT && std::abs(end - end_) < DT) {
        for (Channel *c : channels_) c->update();
        return;
    }
    start_ = start;
    end_ = end;
    updateWave();
}

void WavePreview::setWaveEnabled(bool enabled) {
    enabled_ = enabled;
    updateWave();
}

void WavePreview::setMaximized(bool maximized) {
    maximized_ = maximized;
    updateWave();
}

void WavePreview::updateWave() {
    data_.reset();
    if (enabled_ && mfile_) data_ = mfile_->getAudioPreview(start_, end_);
    if (!data_) data_ = std::make_unique<AudioPreviewData>(AudioPreviewData::demo());
    if (maximized_) data_->normalize();
    if (channels_.size() == data_->channels) {   // same panels: repaint only
        for (Channel *c : channels_) c->update();
        return;
    }
    for (Channel *c : channels_) {
        layout_->removeWidget(c);
        c->deleteLater();
    }
    channels_.clear();
    for (int i = 0; i < data_->channels; ++i) {
        auto *c = new Channel(this, i);
        channels_.append(c);
        layout_->insertWidget(layout_->count() - 1, c, 1);   // before the loader row
    }
}

void WavePreview::startPeaks() {
    progress_->setRange(0, 100);
    progress_->setValue(0);
    loaderRow_->show();
}

// The peaks decoded so far are drawn while the analysis goes on.
void WavePreview::updatePeaks(float position) {
    if (position < 0)
        progress_->setRange(0, 0);   // busy indicator
    else
        progress_->setValue(int(position * 100));
    updateWave();
    timeline_->update();   // key frames arrive with the peaks
}

void WavePreview::stopPeaks() {
    loaderRow_->hide();
    updateWave();
    timeline_->update();
}
