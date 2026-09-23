/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QWidget>
#include <memory>

#include "app/media/AudioPeaks.h"

class AppMediaFile;
class Timeline;
class QProgressBar;
class QPushButton;
class QVBoxLayout;

// The waveform area: one panel per audio channel drawing the min/max peaks of
// the visible window with the selected subtitles highlighted, a progress row
// while the audio is analysed (the waveform fills in meanwhile), "maximize" normalisation and the "play
// current subtitle" trigger. Port of `JWavePreview` + `JAudioLoader`.
class WavePreview : public QWidget, public AudioPeaksListener {
    Q_OBJECT
public:
    explicit WavePreview(Timeline *timeline, QWidget *parent = nullptr);

    void updateMediaFile(AppMediaFile *mfile);
    // Set the window; a change below 2 ms only repaints the highlight.
    void setTime(double start, double end);
    void setWaveEnabled(bool enabled);
    void setMaximized(bool maximized);

    // AudioPeaksListener (GUI thread)
    void startPeaks() override;
    void stopPeaks() override;
    void updatePeaks(float position) override;
    QObject *peaksContext() override { return this; }

    QSize sizeHint() const override;
    QSize minimumSizeHint() const override;

private:
    class Channel;
    void updateWave();

    Timeline *timeline_;
    AppMediaFile *mfile_ = nullptr;
    std::unique_ptr<AudioPreviewData> data_;
    double start_ = -1, end_ = -1;
    bool enabled_ = false, maximized_ = false;
    QVBoxLayout *layout_;
    QWidget *loaderRow_;
    QProgressBar *progress_;
    QList<Channel *> channels_;
};
