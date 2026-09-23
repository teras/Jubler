/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/media/SubPreview.h"

#include <QApplication>
#include <QDir>
#include <QFile>
#include <QHBoxLayout>
#include <QLabel>
#include <QSlider>
#include <QSplitter>
#include <QTimer>
#include <QToolBar>
#include <QToolButton>
#include <QVBoxLayout>
#include <cmath>

#include "app/Theme.h"
#include "app/media/AppMediaFile.h"
#include "app/media/MpvPlayer.h"
#include "app/media/PreviewControls.h"
#include "app/media/WavePreview.h"
#include "app/tools/ToolRunner.h"
#include "app/ui/MainWindow.h"
#include "core/formats/SubFormat.h"
#include "core/i18n/I18N.h"
#include "core/media/MediaCache.h"
#include "core/options/AutoSaveOptions.h"
#include "core/os/Debug.h"
#include "core/os/FileCommunicator.h"
#include "core/subs/SubFile.h"
#include "core/subs/Subtitles.h"

namespace {
constexpr int ZOOM_INITIAL = 30;
constexpr int REFRESH_DEBOUNCE_MS = 100;   // the Java waited 300 ms (VLC)
}  // namespace

SubPreview::SubPreview(MainWindow *parent, QWidget *widget) : QWidget(widget), parent_(parent) {
    buildUi();
    refreshTimer_ = new QTimer(this);
    refreshTimer_->setSingleShot(true);
    refreshTimer_->setInterval(REFRESH_DEBOUNCE_MS);
    connect(refreshTimer_, &QTimer::timeout, this, &SubPreview::doRefreshSubtitles);
}

SubPreview::~SubPreview() {
    release();
}

void SubPreview::buildUi() {
    auto *outer = new QVBoxLayout(this);
    outer->setContentsMargins(0, 0, 0, 0);
    split_ = new QSplitter(this);
    outer->addWidget(split_);

    // Video side.
    framePanel_ = new QWidget(split_);
    auto *fl = new QVBoxLayout(framePanel_);
    fl->setContentsMargins(0, 0, 0, 0);
    fl->setSpacing(0);
    player_ = new MpvPlayer(framePanel_);
    if (player_->isValid()) {
        connect(player_, &MpvPlayer::durationAvailable, this, [this]() { mediaDurationChanged(); });
        player_->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
        fl->addWidget(player_, 1);
        controls_ = new PreviewControls(player_, framePanel_);
        controls_->setPipetteListener([this]() { onPipetteClicked(); });
        controls_->setPlaybackObserver([this](qint64 ms, bool playing) { onPlaybackProgress(ms, playing); });
        fl->addWidget(controls_);
    }
    auto *unavailable = new QLabel(__("Video preview unavailable: the media player could not be initialized"), framePanel_);
    unavailable->setAlignment(Qt::AlignCenter);
    unavailable->setWordWrap(true);
    fl->insertWidget(0, unavailable, 1);
    if (player_->isValid()) {
        unavailable->hide();
        // The player works but cannot draw (no GL render context): the notice takes the frame's place.
        connect(player_, &MpvPlayer::renderUnavailable, unavailable, [this, unavailable]() {
            player_->hide();
            unavailable->show();
        });
    } else
        player_->hide();
    split_->addWidget(framePanel_);

    // Waveform/timeline side.
    auto *mainPanel = new QWidget(split_);
    auto *ml = new QHBoxLayout(mainPanel);
    ml->setContentsMargins(0, 0, 0, 0);
    ml->setSpacing(0);
    auto *audioCol = new QVBoxLayout();
    audioCol->setContentsMargins(0, 0, 0, 0);
    audioCol->setSpacing(0);
    timeline_ = new Timeline(parent_, &view_, this, mainPanel);
    ruler_ = new Ruler(&view_, mainPanel);
    connect(ruler_, &Ruler::wheelTurned, this, [this](QWheelEvent *e) { timeline_->forwardWheel(e); });
    connect(ruler_, &Ruler::panRequested, this, [this](double start) {
        if (!view_.setWindow(start, start + view_.getDuration(), false)) return;   // held under a subtitle drag
        following_ = false;   // looking elsewhere: the view no longer follows the video
        windowHasChanged(nullptr);
    });
    wave_ = new WavePreview(timeline_, mainPanel);
    // No scrollbar of its own: the video's seek bar moves the preview (it
    // centres on the video position), the ruler pans it.
    audioCol->addWidget(wave_, 1);
    audioCol->addWidget(timeline_);
    audioCol->addWidget(ruler_);
    // Info row: "start -> end" + zoom.
    auto *info = new QHBoxLayout();
    info->setContentsMargins(0, 0, 16, 0);
    timePos_ = new QLabel(mainPanel);
    timePos_->setMinimumWidth(50);
    info->addWidget(timePos_, 1);
    auto *zoomOut = new QLabel(mainPanel);
    zoomOut->setPixmap(Theme::pixmap(QStringLiteral("zoomout"), Theme::naturalSize(QStringLiteral("zoomout")).width()));
    zoomOut->setToolTip(__("Zoom out"));
    zoom_ = new QSlider(Qt::Horizontal, mainPanel);
    zoom_->setRange(0, ViewWindow::ZOOM_MAX);
    zoom_->setValue(ZOOM_INITIAL);
    zoom_->setInvertedAppearance(true);
    zoom_->setToolTip(__("Subtitle zoom factor"));
    zoom_->setFixedWidth(100);
    auto *zoomIn = new QLabel(mainPanel);
    zoomIn->setPixmap(Theme::pixmap(QStringLiteral("zoomin"), Theme::naturalSize(QStringLiteral("zoomin")).width()));
    zoomIn->setToolTip(__("Zoom in"));
    info->addWidget(zoomOut);
    info->addWidget(zoom_);
    info->addWidget(zoomIn);
    audioCol->addLayout(info);
    ml->addLayout(audioCol, 1);

    // Tool column.
    auto *tools = new QToolBar(mainPanel);
    tools->setOrientation(Qt::Vertical);
    tools->setMovable(false);
    tools->setIconSize(Theme::naturalSize(QStringLiteral("magnet")));
    auto tb = [&](const QString &icon, const QString &tip, bool checkable) {
        auto *b = new QToolButton(tools);
        b->setIcon(Theme::icon(icon));
        b->setIconSize(Theme::naturalSize(icon));   // added with addWidget: the bar's size does not apply
        b->setToolTip(tip);
        b->setAutoRaise(true);
        b->setCheckable(checkable);
        b->setFocusPolicy(Qt::NoFocus);
        tools->addWidget(b);
        return b;
    };
    maxWaveB_ = tb(QStringLiteral("wavenorm"), __("Maximize waveform visualization"), true);
    connect(maxWaveB_, &QToolButton::toggled, this, [this](bool on) { setMaxWave(on); });
    tools->addSeparator();
    snapB_ = tb(QStringLiteral("magnet"),
                __("Snap subtitles to edges") + QLatin1Char('\n') + __("Hold Shift to select up to the selected subtitle") + QLatin1Char('\n') +
                    __("Hold Ctrl to select multiple subtitles") + QLatin1Char('\n') + __("Hold Alt to skip snapping and freely move subtitles"),
                true);
    snapB_->setChecked(true);
    connect(snapB_, &QToolButton::toggled, this, [this](bool on) { setSnapToSubtitle(on); });
    tools->addSeparator();
    QToolButton *audioPlay = tb(QStringLiteral("playback"), __("Play current subtitle"), false);
    connect(audioPlay, &QToolButton::clicked, this, [this]() { playbackWave(); });
    auto *spacer = new QWidget(tools);
    spacer->setSizePolicy(QSizePolicy::Preferred, QSizePolicy::Expanding);
    tools->addWidget(spacer);
    tools->addSeparator();
    QToolButton *newSub = tb(QStringLiteral("newsub"), __("New subtitle after current one"), false);
    connect(newSub, &QToolButton::clicked, this, [this]() { parent_->addNewSubtitleAfter(); });
    ml->addWidget(tools);
    split_->addWidget(mainPanel);
    split_->setStretchFactor(0, 1);
    split_->setStretchFactor(1, 1);

    connect(zoom_, &QSlider::valueChanged, this, &SubPreview::onZoomChanged);
    // A played subtitle ends with the player standing at its start: the view
    // goes there too (the landing is reported paused, which follows nothing).
    if (player_) connect(player_, &MpvPlayer::rangeEnded, this, [this](qint64 startMs) {
        following_ = true;
        ruler_->stopInertia();
        centreView(startMs / 1000.0);
    });
    // The video moved from its controls: the view follows it again, at once.
    // On where it is going, not on the playhead: the player reports the frame
    // it reached (at most one later) only a moment after this signal.
    if (controls_) connect(controls_, &PreviewControls::userNavigated, this, [this]() {
        following_ = true;
        ruler_->stopInertia();
        centreView(player_ && player_->hasMedia() ? player_->timeMs() / 1000.0 : timeline_->playhead());
    });
}

// ---- window --------------------------------------------------------------------------------

void SubPreview::windowHasChanged(const QList<int> *rows) {
    ignoreSlider_ = true;
    // The inverse of onZoomChanged (duration = 2·(videoDuration/2)^(value/MAX)),
    // so a window that did not change size keeps the slider where it is.
    if (!ignoreZoom_) {
        const int v = ViewWindow::zoomValue(view_.getDuration(), view_.getVideoDuration());
        if (v >= 0) zoom_->setValue(v);
    }
    timeline_->windowHasChanged(rows);
    wave_->setTime(view_.getStart(), view_.getStart() + view_.getDuration());
    // Seek to the first selected row — unless the selection came from playback.
    if (player_ && player_->isValid() && rows && !rows->isEmpty() && !parent_->isPlaybackDrivenSelection()) {
        Subtitles *subs = parent_->getSubtitles();
        const int r = rows->first();
        if (subs && r >= 0 && r < subs->size() && player_->hasMedia())
            player_->seek(subs->get(r)->getStartTime().getMillis());
    }
    ruler_->update();
    ignoreSlider_ = false;
}

void SubPreview::subsHaveChanged(const QList<int> &rows, bool userPicked) {
    // The pipette takes the user's pick (the playback selects no rows meanwhile).
    // Only a pick: the same routine shows the entry again whenever its text or
    // times change, and a point taken then was never chosen by anybody.
    if (grabMode_ && userPicked && !rows.isEmpty()) {
        captureGrabPoint(rows.first());
        return;
    }
    Subtitles *subs = parent_->getSubtitles();
    view_.setVideoDuration(videoDuration());
    // Playback selected the row: the view is placed by the playhead (or stays
    // where the user put it), not by the row.
    if (parent_->isPlaybackDrivenSelection()) {
        timeline_->windowHasChanged(&rows);
        updateSelectedTime();
        return;
    }
    following_ = true;   // a row click brings the preview back to the video
    ruler_->stopInertia();
    double min = 0, max = 0;
    if (!rows.isEmpty() && subs) {
        min = 1e300;
        for (int r : rows) {
            if (r < 0 || r >= subs->size()) continue;
            min = std::min(min, subs->get(r)->getStartTime().toSeconds());
            max = std::max(max, subs->get(r)->getFinishTime().toSeconds());
        }
        if (min > max) min = 0;
    }
    view_.setWindow(min, max, true);
    windowHasChanged(&rows);
    updateSelectedTime();
}

// The timeline length: the last subtitle + 10 s, or the media when it is longer (the player's duration, else the
// probed one).
double SubPreview::videoDuration() const {
    double duration = 0;
    if (Subtitles *subs = parent_->getSubtitles())
        for (const SubEntryPtr &e : subs->entries()) duration = std::max(duration, e->getFinishTime().toSeconds());
    duration += 10;
    if (player_ && player_->durationMs() > 0) duration = std::max(duration, player_->durationMs() / 1000.0);
    else if (mfile_ && mfile_->getVideoFile() && mfile_->getVideoFile()->isInfoReady())
        duration = std::max(duration, double(mfile_->getVideoFile()->getLength()));
    return duration;
}

// The media duration became known after the timeline was laid out: extend it, keeping the current view.
void SubPreview::mediaDurationChanged() {
    const double duration = videoDuration();
    if (duration == view_.getVideoDuration()) return;
    view_.setVideoDuration(duration);
    // Re-clamped to the new length — not under a subtitle drag: its release
    // places the view again.
    if (view_.setWindow(view_.getStart(), view_.getStart() + view_.getDuration(), false)) windowHasChanged(nullptr);
}

void SubPreview::updateSelectedTime() {
    const QString info = Time(timeline_->getSelectionStart()).toString() + QStringLiteral(" -> ") + Time(timeline_->getSelectionEnd()).toString();
    timePos_->setText(info);
    timePos_->setToolTip(info);
}

void SubPreview::onZoomChanged(int value) {
    if (ignoreSlider_) return;
    ignoreZoom_ = true;
    // Around the playhead while the view follows it and it is in view (at an
    // end of the video it is not in the middle), else around the middle.
    const double head = timeline_->playhead(), vs = view_.getStart(), ve = vs + view_.getDuration();
    const double center = following_ && head >= vs && head <= ve ? head : vs + view_.getDuration() / 2;
    const double half = ViewWindow::zoomDuration(value, view_.getVideoDuration()) / 2;
    if (view_.setWindow(center - half, center + half, false)) windowHasChanged(nullptr);
    else if (const int v = ViewWindow::zoomValue(view_.getDuration(), view_.getVideoDuration()); v >= 0) {
        // Held under a subtitle drag: the slider goes back to the window shown.
        ignoreSlider_ = true;
        zoom_->setValue(v);
        ignoreSlider_ = false;
    }
    ignoreZoom_ = false;
}

void SubPreview::zoomBy(int notches) {
    zoom_->setValue(std::clamp(zoom_->value() + notches, 0, ViewWindow::ZOOM_MAX));
}

// The view centred on `t` seconds (keeping its zoom), while it follows the
// video: from a seek/play/row click until the ruler is dragged.
void SubPreview::centreView(double t) {
    if (!following_ || t < 0) return;
    const double half = view_.getDuration() / 2;
    if (view_.setWindow(t - half, t + half, false)) windowHasChanged(nullptr);   // not under a subtitle drag
}

void SubPreview::followPlayhead() {
    centreView(timeline_->playhead());
}

// ---- media / player ----------------------------------------------------------------------------

void SubPreview::updateMediaFile(AppMediaFile *mfile) {
    mfile_ = mfile;
    // The versions on disk, not only the names: a file replaced under its own
    // name (re-encoded, downloaded again) is new media.
    const QString video = mfile && mfile->getVideoFile() ? mfile->getVideoFile()->getPath() : QString();
    const QString key = mfile ? (video.isEmpty() ? QString() : MediaCache::identity(video)) + QLatin1Char('\n') +
                                    (mfile->getAudioFile() ? MediaCache::identity(mfile->getAudioFile()->getPath()) : QString())
                              : QString();
    // The same media is in hand only if the player managed to open it: after a
    // failed load (an unplugged share) the same file is tried again.
    const bool playerLost = player_ && player_->isValid() && !video.isEmpty() && !player_->holds(video);
    if (lastMediaKey_ && *lastMediaKey_ == key && !playerLost) return;
    lastMediaKey_ = key;
    cancelPipette();   // it was aimed at the media that is going
    wave_->updateMediaFile(mfile);
    startPeaks();   // of the new audio: every media change comes through here
    if (player_ && player_->isValid() && mfile && mfile->getVideoFile() && mfile->getVideoFile()->exists()) {
        const SubEntryPtr sel = parent_->getSelectedRow();
        player_->loadMedia(mfile->getVideoFile()->getPath(), sel ? sel->getStartTime().getMillis() : 0);
    }
    refreshSubtitles();
    mediaDurationChanged();
}

// The analysis runs while the preview is on and a media is in hand: either of
// the two can be the one that just arrived.
void SubPreview::startPeaks() {
    if (enabled_ && mfile_) mfile_->startPeaks(decoderListener());
}

void SubPreview::setPreviewEnabled(bool enabled) {
    enabled_ = enabled;
    if (!enabled) cancelPipette();
    else {
        startPeaks();
        refreshSubtitles();   // nothing was exported while it was off
    }
    wave_->setWaveEnabled(enabled);
    if (!enabled && player_ && player_->isValid()) player_->pause();
}

void SubPreview::refreshSubtitles() {
    // Only the file is written again: which row the playback follows is not
    // changed by an edit of its text or its times, and re-selecting it would
    // reload the editor under the user's hands (the caret goes to the end).
    if (player_ && player_->isValid() && player_->hasMedia()) refreshTimer_->start();
}

void SubPreview::reloadSubtitlesNow() {
    refreshTimer_->stop();
    followedIndex_ = UNKNOWN_INDEX;
    exportSubtitles();
}

// mpv redraws the shown frame with the reloaded subtitles by itself (the Java
// had to re-seek VLC to the selected row to get them drawn).
void SubPreview::doRefreshSubtitles() {
    exportSubtitles();
}

void SubPreview::exportSubtitles() {
    if (!player_ || !player_->isValid() || !player_->hasMedia()) return;
    Subtitles *subs = parent_->getSubtitles();
    if (!subs) return;
    if (subFilePath_.isEmpty()) {
        // One file per preview: every window has its own document.
        static int counter = 0;
        subFilePath_ = QDir::tempPath() + QStringLiteral("/jubler_preview_%1_%2.ass").arg(QCoreApplication::applicationPid()).arg(++counter);
    }
    const SubFormatPtr ass = Availabilities::formats().findFromName(QStringLiteral("AdvancedSubStation"));
    if (!ass) {
        Debug::debug(QStringLiteral("Preview: the AdvancedSubStation format is not available"));
        return;
    }
    SubFile sf(QStringLiteral("UTF-8"), subs->getSubFile().getFPS(), ass->newInstance(), subFilePath_, SubFile::EXTENSION_GIVEN);
    const QString err = FileCommunicator::save(*subs, sf, mfile_);
    if (!err.isNull()) {
        Debug::debug(QStringLiteral("Preview: could not export subtitles: ") + err);
        return;
    }
    QFile f(subFilePath_);
    QByteArray content;
    if (f.open(QIODevice::ReadOnly)) content = f.readAll();
    if (content == lastExported_) return;
    lastExported_ = content;
    player_->setSubtitleFile(subFilePath_);
}

void SubPreview::release() {
    refreshTimer_->stop();
    if (player_) player_->release();
    if (!subFilePath_.isEmpty()) QFile::remove(subFilePath_);
}

AudioPeaksListener *SubPreview::decoderListener() { return wave_; }

QPoint SubPreview::frameLocation() const {
    if (player_ && player_->isVisible()) return player_->mapToGlobal(QPoint(0, 0));
    return parent_->mapToGlobal(QPoint(0, 0));
}

// ---- orientation / tools -------------------------------------------------------------------------

void SubPreview::setOrientation(bool horizontal) {
    split_->setOrientation(horizontal ? Qt::Horizontal : Qt::Vertical);
}

void SubPreview::setMaxWave(bool maximized) {
    if (maxWaveB_->isChecked() != maximized) maxWaveB_->setChecked(maximized);
    maxWaveB_->setIcon(Theme::icon(maximized ? QStringLiteral("wavemax") : QStringLiteral("wavenorm")));
    parent_->setMaxWaveMenu(maximized);
    wave_->setMaximized(maximized);
}

void SubPreview::setSnapToSubtitle(bool snap) {
    if (snapB_->isChecked() != snap) snapB_->setChecked(snap);
    parent_->setSnapMenu(snap);
    timeline_->setSnap(snap);
}

void SubPreview::playbackWave() {
    if (timeline_->getSelectedList().isEmpty()) return;
    // The document times of the first selected subtitle, not the dragged ones.
    Subtitles *subs = parent_->getSubtitles();
    const int row = timeline_->getSelectedList().first()->pos;
    if (!subs || row < 0 || row >= subs->size()) return;
    const SubEntryPtr e = subs->get(row);
    const double from = e->getStartTime().toSeconds(), to = e->getFinishTime().toSeconds();
    if (!player_) return;
    // Playing already: go to the subtitle and let it play on, nothing else.
    if (player_->isPlaying())
        player_->seek(qint64(std::llround(from * 1000)));
    else
        player_->playRange(qint64(std::llround(from * 1000)), qint64(std::llround(to * 1000)));
}

// ---- follow playback ----------------------------------------------------------------------------

int SubPreview::findActiveSub(double seconds) const {
    Subtitles *subs = parent_->getSubtitles();
    if (!subs || subs->isEmpty()) return -1;
    const int n = subs->size();
    const int from = std::clamp(followedIndex_, 0, n - 1);
    for (int i = from; i < n; ++i)
        if (subs->get(i)->isInTime(seconds)) return i;
    for (int i = from - 1; i >= 0; --i)
        if (subs->get(i)->isInTime(seconds)) return i;
    return -1;
}

void SubPreview::onPlaybackProgress(qint64 ms, bool playing) {
    timeline_->setPlayhead(ms / 1000.0);
    wave_->update();
    // Playing: the view stays centred on the playhead (a seek from the video's
    // controls centres it at once, a row click places the view itself).
    // Paused, it stays where it is — unless the playhead it follows has left it.
    if (!playing) {
        const double t = ms / 1000.0;
        if (t < view_.getStart() || t > view_.getStart() + view_.getDuration()) followPlayhead();
        return;
    }
    followPlayhead();
    Subtitles *subs = parent_->getSubtitles();
    if (!subs || subs->isEmpty()) return;
    // No row changes under a subtitle drag or while the pipette waits for a pick.
    if (timeline_->isDragging() || grabMode_) return;
    const double seconds = ms / 1000.0;
    if (followedIndex_ >= 0 && followedIndex_ < subs->size() && subs->get(followedIndex_)->isInTime(seconds)) return;
    const int idx = findActiveSub(seconds);
    if (idx == followedIndex_) return;
    followedIndex_ = idx;
    if (idx >= 0 && idx == parent_->getSelectedRowIdx()) return;   // already there
    parent_->followPlaybackSelection(idx);
}

// ---- pipette ------------------------------------------------------------------------------------

// A waiting pipette belongs to the media and the subtitles it was aimed at.
void SubPreview::cancelPipette() {
    if (!grabMode_ && !sync1_) return;
    const bool resume = grabMode_ && wasPlaying_;
    grabMode_ = false;
    sync1_.reset();
    sync2_.reset();
    if (controls_) controls_->setPipetteState(PreviewControls::PipetteState::IDLE);
    if (resume && player_) player_->play();
}

void SubPreview::onPipetteClicked() {
    if (!controls_) return;
    if (grabMode_) {
        cancelPipette();   // the whole session
        return;
    }
    grabMode_ = true;
    wasPlaying_ = player_->isPlaying();
    controls_->setPipetteState(PreviewControls::PipetteState::SEARCHING);
    player_->pause();   // (re-picking the selected row reaches captureGrabPoint as a re-click)
}

void SubPreview::captureGrabPoint(int row) {
    Subtitles *subs = parent_->getSubtitles();
    if (!subs || row < 0 || row >= subs->size() || !player_) return;
    const double subStart = subs->get(row)->getStartTime().toSeconds();
    const TimeSync sync{subStart, player_->time() - subStart};
    if (!sync1_) sync1_ = sync; else sync2_ = sync;
    grabMode_ = false;
    if (sync1_ && sync2_) {
        applySyncMarks();
        return;
    }
    controls_->setPipetteState(PreviewControls::PipetteState::CAPTURED);
    if (wasPlaying_) player_->play();
}

void SubPreview::applySyncMarks() {
    if (!ToolRunner::runSync(parent_, *sync1_, *sync2_)) QApplication::beep();
    sync1_.reset();
    sync2_.reset();
    controls_->setPipetteState(PreviewControls::PipetteState::IDLE);
}
