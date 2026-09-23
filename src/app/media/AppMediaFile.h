/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QMetaObject>
#include <QStringList>
#include <QWidget>
#include <memory>

#include "app/media/AudioPeaks.h"
#include "core/media/MediaFile.h"

class Subtitles;
class QLineEdit;
class QCheckBox;
class QPushButton;
class AppMediaFile;

// Media file name filters, port of `VideoFileFilter`/`AudioFileFilter`/
// `AnyMediaFileFilter`.
namespace MediaFilters {
const QStringList &videoExtensions();   // ".avi", ".mpg", … ".m4v"
const QStringList &audioExtensions();   // ".wav", ".mp3", …
bool isVideo(const QString &path);
bool isAudio(const QString &path);
bool isMedia(const QString &path);
QString videoFilter();   // "All Video files (*.avi …)"
QString audioFilter();
QString mediaFilter();
}  // namespace MediaFilters

// The "Select media" panel (also embedded in the Information dialog): the
// video/audio file and an optional external audio stream, each with a Browse
// button. Port of `JVideofileSelector` (without the VLC-era audio cache row).
class MediaSelector : public QWidget {
    Q_OBJECT
public:
    explicit MediaSelector(QWidget *parent = nullptr);
    void setMediaFile(AppMediaFile *mfile);
    void updateFiles();
    // OK is allowed when the video exists and the audio is embedded or exists.
    bool isValidSelection() const;
    void setSelectorEnabled(bool enabled);

signals:
    void selectionChanged();

private:
    QString chooseFile(const QString &current, const QString &filter);
    AppMediaFile *mfile_ = nullptr;
    QLineEdit *vfName_, *afName_;
    QCheckBox *externalAudio_;
    QPushButton *vBrowse_, *aBrowse_;
};

// The media bundle of a document with the app-side behaviour: guessing the
// video from the subtitle name, the selection dialog, the waveform peaks and
// the clip playback. Port of `MediaFile`/`VideoFile.guessFile`.
class AppMediaFile : public MediaFile {
public:
    AppMediaFile();
    AppMediaFile(const AppMediaFile &other);
    ~AppMediaFile() override;

    static bool isVideoFile(const QString &path) { return MediaFilters::isVideo(path); }
    static QString videoFilterString() { return MediaFilters::videoFilter(); }

    // Make sure a valid video is set: true immediately when one exists (and
    // not forced), otherwise guess and ask the user. False when cancelled.
    bool validateMediaFile(const Subtitles *subs, bool forceNew, QWidget *parent);
    void guessMediaFiles(const Subtitles *subs);
    static std::shared_ptr<VideoFile> guessVideo(const Subtitles *subs);

    bool equals(const AppMediaFile &other) const;
    void setVideoPath(const QString &path);
    void setAudioPath(const QString &path);
    void setAudioFileUnused();
    // "New from video file": set the video and mark the audio embedded.
    void setNewVideoFile(const QString &path);

    MediaSelector *selector();
    void setSelectorEnabled(bool enabled);

    // Waveform: start (or join) the analysis of the audio source; true when
    // the peaks are already complete. `listener` follows its progress.
    bool startPeaks(AudioPeaksListener *listener);
    // Preview closed: an unfinished analysis stops, finished peaks are kept.
    void stopPeaks();
    void cancelPeaks();
    std::unique_ptr<AudioPreviewData> getAudioPreview(double from, double to) const;
    // Video key frames (seconds) within [from,to], known once the audio source was analysed.
    QVector<double> keyframes(double from, double to) const;

private:
    void audioChanged();
    void disconnectListener();
    std::shared_ptr<AudioPeaks> peaks_;
    AudioPeaksListener *listener_ = nullptr;
    QList<QMetaObject::Connection> listenerConnections_;
    std::unique_ptr<MediaSelector> selector_;
};
