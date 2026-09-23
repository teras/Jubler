/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/media/PreviewControls.h"

#include <QCursor>
#include <QEvent>
#include <QToolTip>
#include <cmath>
#include <QHBoxLayout>
#include <QLabel>
#include <QMenu>
#include <QStyleOptionSlider>
#include <QMouseEvent>
#include <QWheelEvent>
#include <QSlider>
#include <climits>
#include <QToolBar>
#include <QToolButton>
#include <QVBoxLayout>
#include <QWidgetAction>

#include "app/Theme.h"
#include "app/media/MpvPlayer.h"
#include "core/i18n/I18N.h"

namespace {
const double SPEEDS[] = {0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0};
}

namespace {
// The seek bar: a click beside the handle moves it there at once (a slider
// pages towards it by default); the press then drags on from there. A press on
// the handle itself only starts a drag, it never moves the value.
class JumpSlider : public QSlider {
public:
    using QSlider::QSlider;

protected:
    void mousePressEvent(QMouseEvent *e) override {
        if (e->button() == Qt::LeftButton) {
            QStyleOptionSlider opt;
            initStyleOption(&opt);
            const QRect handle = style()->subControlRect(QStyle::CC_Slider, &opt, QStyle::SC_SliderHandle, this);
            if (!handle.contains(e->position().toPoint())) {
                const QRect groove = style()->subControlRect(QStyle::CC_Slider, &opt, QStyle::SC_SliderGroove, this);
                const int x = int(e->position().x()) - groove.x() - handle.width() / 2;
                setValue(QStyle::sliderValueFromPosition(minimum(), maximum(), x, groove.width() - handle.width(), opt.upsideDown));
            }
        }
        QSlider::mousePressEvent(e);   // on the handle now: a drag starts
    }
    // The wheel zooms the preview; over the seek bar it does nothing (one
    // notch here would be a few milliseconds).
    void wheelEvent(QWheelEvent *e) override { e->ignore(); }
};
}  // namespace

PreviewControls::PreviewControls(MpvPlayer *player, QWidget *parent) : QWidget(parent), player_(player) {
    auto *lay = new QVBoxLayout(this);
    lay->setContentsMargins(0, 0, 0, 0);
    lay->setSpacing(0);
    const int pad = 8;

    // Seek row.
    auto *seekRow = new QHBoxLayout();
    seekRow->setContentsMargins(pad, 0, pad, 0);
    seekRow->setSpacing(pad);
    seek_ = new JumpSlider(Qt::Horizontal, this);
    seek_->setRange(0, 1000);   // milliseconds once the duration is known (exact positions)
    seek_->setFocusPolicy(Qt::NoFocus);
    seek_->setToolTip(__("Drag to move through the video"));
    seek_->setMinimumWidth(300);
    timeLabel_ = new QLabel(QStringLiteral("0:00:00.0"), this);
    timeLabel_->setAlignment(Qt::AlignRight | Qt::AlignVCenter);
    seekRow->addWidget(seek_, 1);
    seekRow->addWidget(timeLabel_);
    lay->addLayout(seekRow);

    // Transport bar.
    bar_ = new QToolBar(this);
    bar_->setMovable(false);
    bar_->setFloatable(false);
    // The Java loads these 48 px SVGs at half size (24).
    bar_->setIconSize(Theme::naturalSize(QStringLiteral("play"), 0.5));
    auto button = [&](const QString &icon, const QString &tip, std::function<void()> fn) {
        auto *b = new QToolButton(bar_);
        b->setIcon(Theme::icon(icon));
        b->setIconSize(Theme::naturalSize(icon, 0.5));   // added with addWidget: the bar's size does not apply
        b->setToolTip(tip);
        b->setAutoRaise(true);
        b->setFocusPolicy(Qt::NoFocus);
        connect(b, &QToolButton::clicked, this, [this, fn]() {
            fn();
            emit userNavigated();
        });
        bar_->addWidget(b);
        transport_.append(b);
        return b;
    };
    // The icon follows the player's state (playingStateChanged), not the click.
    playPause_ = button(QStringLiteral("play"), __("Play/Pause video playback"), [this]() {
        hidePopups();
        player_->togglePlayPause();
    });
    bar_->addSeparator();
    button(QStringLiteral("bbmovie"), __("Go backwards by 30 seconds"), [this]() { hidePopups(); player_->skip(-30000); });
    button(QStringLiteral("bmovie"), __("Go backwards by 10 seconds"), [this]() { hidePopups(); player_->skip(-10000); });
    button(QStringLiteral("fmovie"), __("Go forwards by 10 seconds"), [this]() { hidePopups(); player_->skip(10000); });
    button(QStringLiteral("ffmovie"), __("Go forwards by 30 seconds"), [this]() { hidePopups(); player_->skip(30000); });
    bar_->addSeparator();

    auto popup = [&](QToolButton *&btn, QMenu *&menu, QSlider *&slider, QLabel *&label, const QString &icon, int max, int value, int majorTicks) {
        menu = new QMenu(this);
        auto *w = new QWidget(menu);
        auto *v = new QVBoxLayout(w);
        v->setContentsMargins(4, 4, 4, 4);
        auto *ic = new QLabel(w);
        ic->setPixmap(Theme::pixmap(icon, Theme::naturalSize(icon, 0.45).width()));
        ic->setAlignment(Qt::AlignHCenter);
        slider = new QSlider(Qt::Vertical, w);
        slider->setRange(0, max);
        slider->setValue(value);
        slider->setTickInterval(majorTicks);
        slider->setTickPosition(QSlider::TicksRight);
        slider->setMinimumHeight(160);
        label = new QLabel(w);
        label->setAlignment(Qt::AlignHCenter);
        QFont f = Theme::adjustFont(label->font(), -1);   // the Java's max(9, size - 1) pixels
        if (f.pointSizeF() > 0) f.setPointSizeF(std::max(9 * 0.75, f.pointSizeF()));
        label->setFont(f);
        v->addWidget(ic);
        v->addWidget(slider, 1, Qt::AlignHCenter);
        v->addWidget(label);
        auto *act = new QWidgetAction(menu);
        act->setDefaultWidget(w);
        menu->addAction(act);
        btn = new QToolButton(bar_);
        btn->setIcon(Theme::icon(icon));
        btn->setIconSize(Theme::naturalSize(icon, 0.5));
        btn->setAutoRaise(true);
        btn->setFocusPolicy(Qt::NoFocus);
        bar_->addWidget(btn);
        transport_.append(btn);
        QMenu *m = menu;
        QToolButton *b = btn;
        connect(btn, &QToolButton::clicked, this, [this, b, m]() { showSliderPopup(b, m); });
    };
    popup(volumeB_, volumePopup_, volumeS_, volumeL_, QStringLiteral("audio"), 10, 10, 5);
    popup(speedB_, speedPopup_, speedS_, speedL_, QStringLiteral("speed"), 6, 3, 3);
    // Their tooltips show at once on hover (they carry the current value).
    volumeB_->installEventFilter(this);
    speedB_->installEventFilter(this);
    auto volumeText = [this]() {
        const QString t = QString::number(volumeS_->value() * 10) + QLatin1Char('%');
        volumeL_->setText(t);
        volumeB_->setToolTip(__("Change audio volume ({0})", t));
    };
    auto speedText = [this]() {
        const QString t = speedLabel(SPEEDS[speedS_->value()]);
        speedL_->setText(t);
        speedB_->setToolTip(__("Change playback speed ({0})", t));
    };
    volumeText();
    speedText();
    connect(volumeS_, &QSlider::valueChanged, this, [volumeText]() { volumeText(); });
    connect(speedS_, &QSlider::valueChanged, this, [speedText]() { speedText(); });
    // Applied when the value settles: drag release, wheel, keys, track clicks (Java !getValueIsAdjusting).
    auto applyVolume = [this]() { hidePopups(); player_->setVolume(volumeS_->value() * 10); };
    auto applySpeed = [this]() { hidePopups(); player_->setSpeed(SPEEDS[speedS_->value()]); };
    connect(volumeS_, &QSlider::valueChanged, this, [this, applyVolume]() { if (!volumeS_->isSliderDown()) applyVolume(); });
    connect(speedS_, &QSlider::valueChanged, this, [this, applySpeed]() { if (!speedS_->isSliderDown()) applySpeed(); });
    connect(volumeS_, &QSlider::sliderReleased, this, applyVolume);
    connect(speedS_, &QSlider::sliderReleased, this, applySpeed);
    bar_->addSeparator();
    pipetteB_ = button(QStringLiteral("textpick"),
                       __("Synchronize subtitles using the video") + QLatin1Char('\n') +
                           __("Play the video. Click to pause at the right moment, then pick the subtitle that belongs there.") + QLatin1Char('\n') +
                           __("Repeat for a second point; the subtitles are then shifted or stretched to match."),
                       [this]() { hidePopups(); if (pipette_) pipette_(); });
    lay->addWidget(bar_);

    setControlsEnabled(false);
    connect(player_, &MpvPlayer::playingStateChanged, this, &PreviewControls::onPlayingStateChanged);
    connect(player_, &MpvPlayer::timeChanged, this, &PreviewControls::onTimeChanged);
    connect(player_, &MpvPlayer::durationAvailable, this, &PreviewControls::onDurationAvailable);
    connect(player_, &MpvPlayer::mediaLoaded, this, &PreviewControls::onMediaLoaded);
    connect(player_, &MpvPlayer::mediaLoading, this, &PreviewControls::onMediaLoading);

    connect(seek_, &QSlider::sliderPressed, this, [this]() {
        scrubbing_ = true;
        moved_ = false;
    });
    connect(seek_, &QSlider::valueChanged, this, [this](int value) {
        if (ignoringSlider_) return;
        const qint64 ms = durationMs_ > 0 ? value : 0;
        setTimeLabel(ms);
        if (scrubbing_) {
            // Every step: the player sends only as many seeks as mpv keeps up with.
            moved_ = true;
            scrubMs_ = ms;
            player_->scrubTo(ms);
            emit userNavigated();   // the preview follows the drag
        } else {
            hidePopups();
            player_->seek(ms);
            emit userNavigated();
        }
    });
    connect(seek_, &QSlider::sliderReleased, this, [this]() {
        scrubbing_ = false;
        hidePopups();
        // Pressed and let go without dragging: nothing moved.
        if (!moved_) return;
        player_->seek(durationMs_ > 0 ? seek_->value() : 0);   // exact, after the key-frame steps
        emit userNavigated();
    });
}

bool PreviewControls::eventFilter(QObject *obj, QEvent *e) {
    if (obj == volumeB_ || obj == speedB_) {
        auto *b = static_cast<QToolButton *>(obj);
        if (e->type() == QEvent::Enter) QToolTip::showText(QCursor::pos(), b->toolTip(), b);
        else if (e->type() == QEvent::Leave) QToolTip::hideText();
    }
    return QWidget::eventFilter(obj, e);
}

QString PreviewControls::speedLabel(double v) {
    return (v == std::floor(v) ? QString::number(int(v)) : QString::number(v)) + QLatin1Char('x');
}

QString PreviewControls::formatTime(qint64 ms) {
    if (ms < 0) ms = 0;
    const qint64 tenths = (ms / 100) % 10, s = (ms / 1000) % 60, m = (ms / 60000) % 60, h = ms / 3600000;
    return QStringLiteral("%1:%2:%3.%4").arg(h).arg(m, 2, 10, QLatin1Char('0')).arg(s, 2, 10, QLatin1Char('0')).arg(tenths);
}

void PreviewControls::setTimeLabel(qint64 ms) { timeLabel_->setText(formatTime(ms)); }

void PreviewControls::setControlsEnabled(bool enabled) {
    for (QWidget *w : transport_) w->setEnabled(enabled);
    seek_->setEnabled(enabled);
    timeLabel_->setEnabled(enabled);
}

void PreviewControls::onPlayingStateChanged(bool playing) {
    playPause_->setIcon(Theme::icon(playing ? QStringLiteral("pause") : QStringLiteral("play")));
}

void PreviewControls::onTimeChanged(qint64 ms) {
    // During a drag the label, the slider and the preview show where the
    // pointer is: mpv's reports of the key frames it went to are not passed on.
    // Only once the drag has a target though: a click beside the handle seeks
    // and *then* starts the drag, and its report must not be taken for one of
    // an earlier drag.
    if (scrubbing_ && moved_) {
        if (ms != scrubMs_) return;
    } else {
        setTimeLabel(ms);
        if (durationMs_ > 0) {
            ignoringSlider_ = true;
            seek_->setValue(int(std::min<qint64>(ms, seek_->maximum())));
            ignoringSlider_ = false;
        }
    }
    if (observer_) observer_(ms, player_->isPlaying());
}

void PreviewControls::onMediaLoading(qint64 startMs) {
    // Nothing of the file being left behind survives: its length ruled the seek
    // bar, so a click on the bar meanwhile asked for a place of the old one.
    durationMs_ = 0;
    scrubbing_ = moved_ = false;
    scrubMs_ = -1;
    ignoringSlider_ = true;
    seek_->setRange(0, 0);
    ignoringSlider_ = false;
    setControlsEnabled(false);
    setTimeLabel(startMs);
    if (observer_) observer_(startMs, false);
}

void PreviewControls::onMediaLoaded() {
    // The transport belongs to the file, not to its length: a media whose
    // duration mpv never reports is still played, skipped through and paused.
    setControlsEnabled(true);
}

void PreviewControls::onDurationAvailable(qint64 ms) {
    durationMs_ = ms;
    ignoringSlider_ = true;
    seek_->setRange(0, int(std::min<qint64>(ms, INT_MAX)));   // one step per millisecond
    ignoringSlider_ = false;
    setControlsEnabled(true);
}

void PreviewControls::hidePopups() {
    volumePopup_->hide();
    speedPopup_->hide();
}

void PreviewControls::showSliderPopup(QToolButton *button, QMenu *popup) {
    if (popup->isVisible()) {
        popup->hide();
        return;
    }
    hidePopups();
    const QSize sz = popup->sizeHint();
    const QPoint at = button->mapToGlobal(QPoint((button->width() - sz.width()) / 2, -sz.height()));
    popup->popup(at);
}


void PreviewControls::setPipetteState(PipetteState s) {
    switch (s) {
        case PipetteState::IDLE: pipetteB_->setIcon(Theme::icon(QStringLiteral("textpick"))); break;
        case PipetteState::SEARCHING: pipetteB_->setIcon(Theme::icon(QStringLiteral("textpickask"))); break;
        case PipetteState::CAPTURED: pipetteB_->setIcon(Theme::icon(QStringLiteral("textpickfull"))); break;
    }
}
