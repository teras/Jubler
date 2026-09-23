/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <atomic>
#include <functional>

// Reads a text subtitle stream embedded in a media file (mkv, mp4, ts…) with
// libavformat and returns it as the text of a subtitle file, so the normal
// format loaders parse it: SubRip and WebVTT streams as SRT/VTT (millisecond
// times), SSA/ASS as ASS with the file's styles, every other text codec
// decoded by libavcodec into ASS. Times are rebased on the file start, as
// the player's clock is.
namespace EmbeddedSubtitles {

struct Result {
    QString text;        // null on failure
    QString extension;   // "srt", "vtt" or "ass"
    QString error;
};

// `streamIndex` is the container stream index (SubtitleStreamInfo::id).
Result extract(const QString &path, int streamIndex, const std::atomic<bool> &cancel, const std::function<void(float)> &progress);

}  // namespace EmbeddedSubtitles
