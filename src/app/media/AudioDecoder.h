/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <QtGlobal>
#include <atomic>
#include <functional>

// Decodes the default audio stream of a media file with libavformat/
// libavcodec to interleaved signed 16-bit samples (libswresample converts the
// rate, layout and format). Shared by the waveform peaks and the recipe WAV.
namespace AudioDecoder {

struct Output {
    int rate = 0;       // samples per second
    int channels = 0;   // 1 or 2
};

struct Callbacks {
    // Once, before any samples.
    std::function<void(const Output &)> configured;
    // A block of `frames` interleaved frames starting at output frame
    // `position`, placed by the stream timestamps (rebased on the file start
    // as the player's clock is): a gap in the stream skips positions.
    std::function<void(qint64 position, const qint16 *samples, int frames)> samples;
    // 0..1 by bytes read, or -1 when the size is unknown.
    std::function<void(float)> progress;
    // Optional: the time (seconds, same clock) of every key frame of the main
    // video stream, read from the packet flags in the same pass (no decoding).
    std::function<void(double)> keyframe;
};

// `rate`/`channels` 0 keep the source rate / min(source channels, 2); more
// than two source channels are downmixed. False (and `error`) on failure or
// when `cancel` becomes true.
bool decode(const QString &path, int rate, int channels, const std::atomic<bool> &cancel, const Callbacks &cb, QString *error = nullptr);

// The audio of `path` as a 16-bit PCM WAV file `dst` (gaps in the stream are
// filled with silence, so file time = media time). `dst` is removed on failure.
bool writeWav(const QString &path, const QString &dst, int rate, int channels, const std::atomic<bool> &cancel,
              const std::function<void(float)> &progress, QString *error = nullptr);

}  // namespace AudioDecoder
