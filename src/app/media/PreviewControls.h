/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QWidget>
#include <functional>

class MpvPlayer;
class QSlider;
class QLabel;
class QToolButton;
class QToolBar;
class QMenu;

// The transport bar under the video: seek slider with time label, play/pause,
// ±10 s / ±30 s, volume and speed popup sliders and the synchronisation
// "pipette". Port of `JEmbeddedPreviewControls`.
class PreviewControls : public QWidget {
    Q_OBJECT
public:
    enum class PipetteState { IDLE, SEARCHING, CAPTURED };

    explicit PreviewControls(MpvPlayer *player, QWidget *parent = nullptr);

    void setPipetteState(PipetteState s);
    void setPipetteListener(std::function<void()> fn) { pipette_ = std::move(fn); }
    // (timeMs, playing) on every time tick.
    void setPlaybackObserver(std::function<void(qint64, bool)> fn) { observer_ = std::move(fn); }

signals:
    // The user moved the video (seek bar, skip or play buttons): the preview
    // follows it again.
    void userNavigated();

protected:
    bool eventFilter(QObject *obj, QEvent *e) override;

private:
    void onPlayingStateChanged(bool playing);
    void onTimeChanged(qint64 ms);
    void onDurationAvailable(qint64 ms);
    void onMediaLoaded();
    void onMediaLoading(qint64 startMs);
    void setControlsEnabled(bool enabled);
    void hidePopups();
    void showSliderPopup(QToolButton *button, QMenu *popup);
    void setTimeLabel(qint64 ms);
    static QString formatTime(qint64 ms);
    static QString speedLabel(double v);

    MpvPlayer *player_;
    QSlider *seek_;
    QLabel *timeLabel_;
    QToolBar *bar_;
    QToolButton *playPause_, *volumeB_, *speedB_, *pipetteB_;
    QList<QWidget *> transport_;
    QMenu *volumePopup_, *speedPopup_;
    QSlider *volumeS_, *speedS_;
    QLabel *volumeL_, *speedL_;
    bool ignoringSlider_ = false;
    bool scrubbing_ = false;
    bool moved_ = false;    // the handle was dragged since it was pressed
    qint64 scrubMs_ = -1;   // the latest drag target (mpv's other reports are skipped)
    qint64 durationMs_ = 0;
    std::function<void()> pipette_;
    std::function<void(qint64, bool)> observer_;
};
