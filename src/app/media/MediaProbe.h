/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QString>
#include <memory>

class VideoFile;
class QWidget;

// One embedded subtitle stream of a media file, port of `SubtitleStreamInfo`
// (consumed by the external-tool recipes).
struct SubtitleStreamInfo {
    int index = 0;        // position among the subtitle streams (ffmpeg 0:s:N)
    int id = 0;           // container track id (mkvextract)
    QString language, codecName, codecDescription, title;
    bool extractable = false;   // text based, convertible to SRT
    QString getField(const QString &field) const;
};

// Media metadata as read by libavformat.
struct MediaInfo {
    bool ok = false;
    int width = 0, height = 0;
    double duration = 0;   // seconds
    double fps = 0;
    QList<SubtitleStreamInfo> subtitleStreams;
};

namespace MediaProbe {
// Blocking probe (call off the GUI thread or accept the wait): opens the file
// without decoding and reads the container information.
MediaInfo probe(const QString &path, int timeoutMs = 10000);
// Start the asynchronous probe of a video file; its information is filled in
// on the GUI thread when done (defaults stand on failure).
void start(const std::shared_ptr<VideoFile> &video);
// Wait (with a small modal progress dialog, shown after 400 ms) up to `timeoutMs` for the probe
// of `video` to finish. No-op when there is nothing to wait for.
void await(VideoFile *video, int timeoutMs, QWidget *parent);
// Embedded subtitle streams; never throws, empty when unknown.
QList<SubtitleStreamInfo> subtitleStreams(const QString &path);
}  // namespace MediaProbe
