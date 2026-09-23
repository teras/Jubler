/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/media/AudioDecoder.h"

#include <QFile>
#include <QtEndian>
#include <algorithm>
#include <cmath>
#include <vector>

#include "core/i18n/I18N.h"
#include "app/media/LibAv.h"
#include "core/os/Debug.h"

namespace AudioDecoder {

namespace {

// The resampler follows the decoded frames: rebuilt when their format changes.
class Converter {
public:
    Converter(const Output &out) : out_(out) { av_channel_layout_default(&outLayout_, out.channels); }
    ~Converter() { av_channel_layout_uninit(&outLayout_); av_channel_layout_uninit(&inLayout_); }

    SwrContext *forFrame(const AVFrame *f) {
        if (swr_ && f->format == inFormat_ && f->sample_rate == inRate_ && av_channel_layout_compare(&f->ch_layout, &inLayout_) == 0)
            return swr_.get();
        av_channel_layout_uninit(&inLayout_);
        if (f->ch_layout.order == AV_CHANNEL_ORDER_UNSPEC)
            av_channel_layout_default(&inLayout_, f->ch_layout.nb_channels);
        else
            av_channel_layout_copy(&inLayout_, &f->ch_layout);
        inFormat_ = f->format;
        inRate_ = f->sample_rate;
        SwrContext *s = nullptr;
        if (swr_alloc_set_opts2(&s, &outLayout_, AV_SAMPLE_FMT_S16, out_.rate, &inLayout_, AVSampleFormat(inFormat_), inRate_, 0, nullptr) < 0
            || swr_init(s) < 0) {
            swr_free(&s);
            swr_.reset();
            return nullptr;
        }
        swr_.reset(s);
        return s;
    }
    SwrContext *current() const { return swr_.get(); }

private:
    Output out_;
    AVChannelLayout outLayout_{}, inLayout_{};
    int inFormat_ = -1, inRate_ = 0;
    LibAv::SwrPtr swr_;
};

}  // namespace

bool decode(const QString &path, int rate, int channels, const std::atomic<bool> &cancel, const Callbacks &cb, QString *error) {
    auto fail = [error](const QString &msg) {
        if (error) *error = msg;
        return false;
    };
    LibAv::Interrupt irq;
    irq.cancel = &cancel;
    QString err;
    LibAv::FormatPtr fmt = LibAv::open(path, &irq, &err);
    if (!fmt) return fail(err);
    AVFormatContext *ctx = fmt.get();
    const AVCodec *codec = nullptr;
    const int si = av_find_best_stream(ctx, AVMEDIA_TYPE_AUDIO, -1, -1, &codec, 0);
    if (si < 0 || !codec) return fail(__("No audio stream"));
    int vi = -1;   // the video stream whose key frames are reported
    if (cb.keyframe) {
        vi = av_find_best_stream(ctx, AVMEDIA_TYPE_VIDEO, -1, -1, nullptr, 0);
        if (vi >= 0 && (ctx->streams[vi]->disposition & AV_DISPOSITION_ATTACHED_PIC)) vi = -1;   // cover art
    }
    for (unsigned i = 0; i < ctx->nb_streams; ++i)
        if (int(i) != si && int(i) != vi) ctx->streams[i]->discard = AVDISCARD_ALL;
    AVStream *st = ctx->streams[si];

    LibAv::CodecPtr dec(avcodec_alloc_context3(codec));
    if (!dec || avcodec_parameters_to_context(dec.get(), st->codecpar) < 0) return fail(__("Unsupported audio stream"));
    dec->thread_count = 0;
    dec->pkt_timebase = st->time_base;
    if (int r = avcodec_open2(dec.get(), codec, nullptr); r < 0) return fail(LibAv::errorString(r));
    const int srcChannels = dec->ch_layout.nb_channels;
    if (srcChannels <= 0 || dec->sample_rate <= 0) return fail(__("Unsupported audio stream"));

    const Output out{rate > 0 ? rate : dec->sample_rate, channels > 0 ? channels : std::min(srcChannels, 2)};
    if (cb.configured) cb.configured(out);
    Converter conv(out);
    const int64_t start = ctx->start_time != AV_NOPTS_VALUE ? ctx->start_time : 0;
    const int64_t size = ctx->pb ? avio_size(ctx->pb) : -1;
    if (cb.progress && size <= 0) cb.progress(-1);

    std::vector<qint16> buf;
    qint64 written = 0;
    bool first = true;
    const qint64 tolerance = out.rate / 20;   // 50 ms: timestamp jitter below this is ignored
    // Larger jumps are gaps (or overlaps) the samples are placed at, unless
    // they are implausible (a corrupt timestamp, a reset in concatenated
    // recordings): then the samples just continue where the previous ended.
    const qint64 maxForward = qint64(out.rate) * 600, maxBackward = qint64(out.rate) * 5;

    // Converts `f` (null: flush the resampler) and hands the samples on.
    auto deliver = [&](const AVFrame *f) {
        SwrContext *swr = f ? conv.forFrame(f) : conv.current();
        if (!swr) return;
        const qint64 pending = swr_get_delay(swr, out.rate);
        const int maxOut = swr_get_out_samples(swr, f ? f->nb_samples : 0);
        if (maxOut <= 0) return;
        buf.resize(size_t(maxOut) * out.channels);
        auto *o = reinterpret_cast<uint8_t *>(buf.data());
        int got = swr_convert(swr, &o, maxOut, f ? const_cast<const uint8_t **>(f->extended_data) : nullptr, f ? f->nb_samples : 0);
        if (got <= 0) return;
        const qint16 *data = buf.data();
        if (f && f->best_effort_timestamp != AV_NOPTS_VALUE) {
            const int64_t us = av_rescale_q(f->best_effort_timestamp, st->time_base, AV_TIME_BASE_Q) - start;
            qint64 at = qint64(std::llround(double(us) * out.rate / AV_TIME_BASE)) - pending;
            const bool plausible = first || (at - written <= maxForward && written - at <= maxBackward);
            if (plausible && (first || std::abs(at - written) > tolerance)) {
                if (at < 0) {   // before the file start (priming, pre-roll): drop that part
                    const int skip = int(std::min<qint64>(-at, got));
                    data += size_t(skip) * out.channels;
                    got -= skip;
                    at = 0;
                }
                written = at;
            }
        }
        first = false;
        if (got <= 0) return;
        cb.samples(written, data, got);
        written += got;
    };

    LibAv::PacketPtr pkt(av_packet_alloc());
    LibAv::FramePtr frame(av_frame_alloc());
    auto drain = [&]() {
        while (avcodec_receive_frame(dec.get(), frame.get()) >= 0) {
            deliver(frame.get());
            av_frame_unref(frame.get());
        }
    };
    int lastPercent = -1;
    while (!cancel) {
        const int r = av_read_frame(ctx, pkt.get());
        if (r == AVERROR_EOF) break;
        if (r < 0) {   // a read error is a failure, never a shorter result
            if (cancel) break;
            return fail(LibAv::errorString(r));
        }
        if (pkt->stream_index == si && avcodec_send_packet(dec.get(), pkt.get()) >= 0)
            drain();
        else if (pkt->stream_index == vi && (pkt->flags & AV_PKT_FLAG_KEY)) {
            const int64_t ts = pkt->pts != AV_NOPTS_VALUE ? pkt->pts : pkt->dts;
            if (ts != AV_NOPTS_VALUE)
                cb.keyframe(double(av_rescale_q(ts, ctx->streams[vi]->time_base, AV_TIME_BASE_Q) - start) / AV_TIME_BASE);
        }
        av_packet_unref(pkt.get());
        if (cb.progress && size > 0) {
            const int percent = int(avio_tell(ctx->pb) * 100 / size);
            if (percent != lastPercent) {
                lastPercent = percent;
                cb.progress(std::clamp(percent / 100.0f, 0.0f, 0.99f));
            }
        }
    }
    if (cancel) return fail(QStringLiteral("Cancelled"));
    avcodec_send_packet(dec.get(), nullptr);
    drain();
    deliver(nullptr);
    if (cb.progress) cb.progress(1);
    return true;
}

namespace {
QByteArray wavHeader(int rate, int channels, quint32 dataBytes) {
    QByteArray h(44, '\0');
    auto put32 = [&h](int at, quint32 v) { qToLittleEndian(v, h.data() + at); };
    auto put16 = [&h](int at, quint16 v) { qToLittleEndian(v, h.data() + at); };
    h.replace(0, 4, "RIFF");
    put32(4, 36 + dataBytes);
    h.replace(8, 8, "WAVEfmt ");
    put32(16, 16);
    put16(20, 1);   // PCM
    put16(22, quint16(channels));
    put32(24, quint32(rate));
    put32(28, quint32(rate * channels * 2));
    put16(32, quint16(channels * 2));
    put16(34, 16);
    h.replace(36, 4, "data");
    put32(40, dataBytes);
    return h;
}
}  // namespace

bool writeWav(const QString &path, const QString &dst, int rate, int channels, const std::atomic<bool> &cancel,
              const std::function<void(float)> &progress, QString *error) {
    QFile out(dst);
    if (!out.open(QIODevice::WriteOnly)) {
        if (error) *error = out.errorString();
        return false;
    }
    out.write(wavHeader(rate, channels, 0));
    Output fmt;
    qint64 written = 0;   // frames in the file
    bool ioError = false;
    Callbacks cb;
    cb.configured = [&fmt](const Output &o) { fmt = o; };
    cb.samples = [&](qint64 position, const qint16 *samples, int frames) {
        if (ioError) return;
        const int frameBytes = fmt.channels * 2;
        if (position > written) {   // a gap: silence
            const QByteArray silence(int(std::min<qint64>(position - written, qint64(1) << 20)) * frameBytes, '\0');
            for (qint64 left = position - written; left > 0 && !ioError && !cancel;) {
                const qint64 n = std::min<qint64>(left, silence.size() / frameBytes);
                ioError |= out.write(silence.constData(), n * frameBytes) != n * frameBytes;
                left -= n;
            }
            written = position;
        }
        const qint64 overlap = std::min<qint64>(written - position, frames);   // already written: keep the first
        if (frames - overlap <= 0) return;
        const qint64 bytes = (frames - overlap) * frameBytes;
        ioError |= out.write(reinterpret_cast<const char *>(samples + overlap * fmt.channels), bytes) != bytes;
        written += frames - overlap;
    };
    cb.progress = progress;
    QString err;
    bool ok = decode(path, rate, channels, cancel, cb, &err);
    const qint64 dataBytes = written * qint64(fmt.channels) * 2;
    if (ok && (ioError || dataBytes > 0xFFFFFFFFLL - 36)) {
        ok = false;
        err = ioError ? out.errorString() : __("Audio too long for a WAV file");
    }
    if (ok) {
        out.seek(0);
        ok = out.write(wavHeader(fmt.rate, fmt.channels, quint32(dataBytes))) == 44;
        if (!ok) err = out.errorString();
    }
    out.close();
    if (!ok) {
        QFile::remove(dst);
        if (error) *error = err;
    }
    return ok;
}

}  // namespace AudioDecoder
