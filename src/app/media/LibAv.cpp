/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/media/LibAv.h"

#include "core/i18n/I18N.h"
#include "core/os/Debug.h"

namespace LibAv {

namespace {

int interruptCallback(void *opaque) {
    const auto *irq = static_cast<const Interrupt *>(opaque);
    if (irq->cancel && irq->cancel->load()) return 1;
    return std::chrono::steady_clock::now() > irq->deadline ? 1 : 0;
}

bool checkAbi() {
    struct Lib { const char *name; unsigned runtime, header; };
    const Lib libs[] = {
        {"avformat", avformat_version(), LIBAVFORMAT_VERSION_INT},
        {"avcodec", avcodec_version(), LIBAVCODEC_VERSION_INT},
        {"avutil", avutil_version(), LIBAVUTIL_VERSION_INT},
        {"swresample", swresample_version(), LIBSWRESAMPLE_VERSION_INT},
    };
    bool ok = true;
    for (const Lib &l : libs)
        if ((l.runtime >> 16) != (l.header >> 16)) {
            Debug::debug(QStringLiteral("lib%1 %2 is loaded but Jubler was built for %3; audio waveform and media probing are disabled")
                             .arg(QLatin1String(l.name)).arg(l.runtime >> 16).arg(l.header >> 16));
            ok = false;
        }
    // FFmpeg's log callback is one per process and libmpv sets its own: taking
    // it over means mpv's messages arrive labelled as ours (they are its
    // hardware-decoding probes, not our decoding) and mpv loses them from its
    // own log. Whatever we ask libav for, we report from what it returns.
    return ok;
}

}  // namespace

bool available() {
    static const bool ok = checkAbi();
    return ok;
}

FormatPtr open(const QString &path, Interrupt *irq, QString *error) {
    if (!available()) {
        if (error) *error = __("FFmpeg libraries unavailable");
        return nullptr;
    }
    AVFormatContext *ctx = avformat_alloc_context();
    if (!ctx) return nullptr;
    if (irq) {
        ctx->interrupt_callback.callback = interruptCallback;
        ctx->interrupt_callback.opaque = irq;
    }
    // On failure avformat_open_input frees the context itself.
    int err = avformat_open_input(&ctx, path.toUtf8().constData(), nullptr, nullptr);
    if (err < 0) {
        if (error) *error = errorString(err);
        return nullptr;
    }
    FormatPtr fmt(ctx);
    err = avformat_find_stream_info(ctx, nullptr);
    if (err < 0) {
        if (error) *error = errorString(err);
        return nullptr;
    }
    return fmt;
}

QString errorString(int err) {
    char buf[AV_ERROR_MAX_STRING_SIZE] = {};
    av_strerror(err, buf, sizeof(buf));
    return QString::fromUtf8(buf);
}

}  // namespace LibAv
