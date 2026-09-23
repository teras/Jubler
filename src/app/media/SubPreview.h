/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QWidget>
#include <limits>
#include <optional>

#include "app/media/Timeline.h"
#include "core/tools/CoreTools.h"

class MainWindow;
class AppMediaFile;
class MpvPlayer;
class PreviewControls;
class WavePreview;
class AudioPeaksListener;
class QSplitter;
class QSlider;
class QLabel;
class QToolButton;
class QTimer;
class QTemporaryFile;

// The preview panel: the video (with transport) on one side and, on the
// other, the waveform, the subtitle timeline with its ruler, the scrollbar,
// the "start -> end" label, the zoom slider and the tool column. Drives the
// view window from the table selection, follows playback, re-exports the
// subtitles to the player and runs the two-point synchronisation "pipette".
// Port of `JSubPreview`.
class SubPreview : public QWidget {
    Q_OBJECT
public:
    SubPreview(MainWindow *parent, QWidget *widget = nullptr);
    ~SubPreview() override;

    bool isPreviewEnabled() const { return enabled_; }
    void setPreviewEnabled(bool enabled);
    void updateMediaFile(AppMediaFile *mfile);
    AppMediaFile *mediaFile() const { return mfile_; }
    // The table selection changed (rows) — recentre the window and seek.
    // `userPicked`: the user really selected that row (a click or a keyboard
    // move in the table), not a redisplay of the same selection — only then
    // does a waiting pipette take its point.
    void subsHaveChanged(const QList<int> &rows, bool userPicked = false);
    void windowHasChanged(const QList<int> *rows);
    void updateSelectedTime();
    // Debounced re-export of the document to the player.
    void refreshSubtitles();
    void reloadSubtitlesNow();
    void setOrientation(bool horizontal);
    void setMaxWave(bool maximized);
    void setSnapToSubtitle(bool snap);
    void playbackWave();
    void release();
    AudioPeaksListener *decoderListener();
    // Wheel zoom: one slider step per notch.
    void zoomBy(int notches);
    QPoint frameLocation() const;

private:
    void buildUi();
    double videoDuration() const;
    void mediaDurationChanged();
    void onZoomChanged(int value);
    // Centre the view on `t` seconds / on the playhead, when it follows the video.
    void centreView(double t);
    void followPlayhead();
    void onPipetteClicked();
    void cancelPipette();
    // The waveform of the media in hand, while the preview is showing it.
    void startPeaks();
    void captureGrabPoint(int row);
    void applySyncMarks();
    void onPlaybackProgress(qint64 ms, bool playing);
    int findActiveSub(double seconds) const;
    void doRefreshSubtitles();
    void exportSubtitles();

    MainWindow *parent_;
    ViewWindow view_;
    Timeline *timeline_;
    Ruler *ruler_;
    WavePreview *wave_;
    MpvPlayer *player_ = nullptr;
    PreviewControls *controls_ = nullptr;
    QWidget *framePanel_;
    QSplitter *split_;
    bool following_ = true;    // the view follows the video position
    QSlider *zoom_;
    QLabel *timePos_;
    QToolButton *maxWaveB_, *snapB_;
    QTimer *refreshTimer_;
    AppMediaFile *mfile_ = nullptr;
    std::optional<QString> lastMediaKey_;
    QString subFilePath_;
    QByteArray lastExported_;
    bool enabled_ = false;
    bool ignoreSlider_ = false;
    bool ignoreZoom_ = false;
    static constexpr int UNKNOWN_INDEX = std::numeric_limits<int>::min();   // vs -1 = "in a gap"
    int followedIndex_ = UNKNOWN_INDEX;
    // Pipette.
    bool grabMode_ = false;
    bool wasPlaying_ = false;
    std::optional<TimeSync> sync1_, sync2_;
};
