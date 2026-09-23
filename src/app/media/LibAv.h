/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <atomic>
#include <chrono>
#include <memory>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libswresample/swresample.h>
}

// Thin helpers over the FFmpeg libraries libmpv already loads: ownership
// wrappers, cancellable opening and the runtime ABI check.
namespace LibAv {

// True when the loaded FFmpeg libraries have the major versions the headers
// describe (checked once; logged). When false, every libav feature is off and
// callers fall back to their defaults.
bool available();

struct FormatCloser { void operator()(AVFormatContext *c) const { avformat_close_input(&c); } };
struct CodecCloser { void operator()(AVCodecContext *c) const { avcodec_free_context(&c); } };
struct SwrCloser { void operator()(SwrContext *c) const { swr_free(&c); } };
struct FrameCloser { void operator()(AVFrame *f) const { av_frame_free(&f); } };
struct PacketCloser { void operator()(AVPacket *p) const { av_packet_free(&p); } };
using FormatPtr = std::unique_ptr<AVFormatContext, FormatCloser>;
using CodecPtr = std::unique_ptr<AVCodecContext, CodecCloser>;
using SwrPtr = std::unique_ptr<SwrContext, SwrCloser>;
using FramePtr = std::unique_ptr<AVFrame, FrameCloser>;
using PacketPtr = std::unique_ptr<AVPacket, PacketCloser>;

// Stops reading on cancel or past the deadline. FFmpeg checks it between
// reads (and inside network protocols); a single read() blocked on a stale
// local/NFS mount is not interrupted, which is why no GUI-thread code waits
// for a libav worker. Must outlive the context opened with it.
struct Interrupt {
    const std::atomic<bool> *cancel = nullptr;
    std::chrono::steady_clock::time_point deadline = std::chrono::steady_clock::time_point::max();
    void setTimeout(int ms) { deadline = std::chrono::steady_clock::now() + std::chrono::milliseconds(ms); }
};

// Opens `path` (UTF-8, also on Windows) and reads the stream information.
FormatPtr open(const QString &path, Interrupt *irq, QString *error = nullptr);

QString errorString(int err);

}  // namespace LibAv
