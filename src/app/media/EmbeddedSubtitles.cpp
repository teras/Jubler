/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/media/EmbeddedSubtitles.h"

#include <QDataStream>
#include <QDir>
#include <QList>
#include <QSaveFile>
#include <QStringList>
#include <algorithm>

#include "app/media/LibAv.h"
#include "core/i18n/I18N.h"
#include "core/media/MediaCache.h"
#include "core/os/Debug.h"

namespace EmbeddedSubtitles {

namespace {

enum class Kind { Srt, Vtt, AssRaw, Decode };

// Reading a stream means demuxing the whole container (its packets are spread
// over the file), so the text is kept: the same file and stream is then read
// back in no time. Small entries (text), so a small folder.
const QString CACHE_KIND = QStringLiteral("subs");
const QString CACHE_SUFFIX = QStringLiteral(".jsub");
constexpr char CACHE_MAGIC[] = "JSUB";
constexpr int MAGIC_SIZE = sizeof(CACHE_MAGIC) - 1;
constexpr int CACHE_VERSION = 1;   // bump when what is written changes
constexpr qint64 CACHE_LIMIT = qint64(32) << 20;

QString cacheFile(const QString &media, int streamIndex) {
    return MediaCache::file(CACHE_KIND, media, QString::number(streamIndex), CACHE_VERSION, CACHE_SUFFIX);
}

bool loadCache(const QString &media, int streamIndex, Result &r) {
    const QString name = cacheFile(media, streamIndex);
    QFile f(name);
    if (!f.open(QIODevice::ReadOnly)) return false;
    QDataStream in(&f);
    char magic[MAGIC_SIZE] = {};
    QString extension;
    QByteArray packed;
    in.readRawData(magic, MAGIC_SIZE);
    in >> extension >> packed;
    if (in.status() != QDataStream::Ok || qstrncmp(magic, CACHE_MAGIC, MAGIC_SIZE) != 0 || extension.isEmpty()) return false;
    const QByteArray text = qUncompress(packed);
    if (text.isEmpty()) return false;
    f.close();
    MediaCache::touch(name);
    r.extension = extension;
    r.text = QString::fromUtf8(text);
    return true;
}

void saveCache(const QString &media, int streamIndex, const Result &r) {
    QDir().mkpath(MediaCache::dir(CACHE_KIND));
    QSaveFile f(cacheFile(media, streamIndex));
    if (!f.open(QIODevice::WriteOnly)) return;
    QDataStream out(&f);
    out.writeRawData(CACHE_MAGIC, MAGIC_SIZE);
    out << r.extension << qCompress(r.text.toUtf8(), 6);
    if (!f.commit()) {
        Debug::debug(QStringLiteral("Could not store the subtitles of %1: %2").arg(media, f.errorString()));
        return;
    }
    MediaCache::prune(CACHE_KIND, CACHE_SUFFIX, CACHE_LIMIT);
}

struct Event {
    qint64 start = 0, end = -1;   // ms; end -1 = unknown
    QString text;                 // SRT/VTT: the cue text; ASS: "ReadOrder,Layer,Style,…,Text"
};

QString clock(qint64 ms, QChar msSep, int digits) {
    ms = std::max<qint64>(ms, 0);
    if (digits == 2) ms = (ms + 5) / 10 * 10;   // centiseconds: the nearest one
    const qint64 frac = digits == 3 ? ms % 1000 : (ms % 1000) / 10;
    return QStringLiteral("%1:%2:%3%4%5")
        .arg(ms / 3600000, digits == 3 ? 2 : 1, 10, QLatin1Char('0'))
        .arg(ms / 60000 % 60, 2, 10, QLatin1Char('0'))
        .arg(ms / 1000 % 60, 2, 10, QLatin1Char('0'))
        .arg(msSep)
        .arg(frac, digits, 10, QLatin1Char('0'));
}

const char *ASS_EVENTS = "\n[Events]\nFormat: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n";

// An ASS event line "ReadOrder,Layer,Style,Name,MarginL,MarginR,MarginV,Effect,Text"
// as a Dialogue line with the given times.
QString dialogue(const Event &e) {
    const QStringList f = e.text.split(QLatin1Char(','));
    if (f.size() < 9) return QString();
    const QString text = e.text.section(QLatin1Char(','), 8);   // the text may contain commas
    return QStringLiteral("Dialogue: %1,%2,%3,%4,%5,%6,%7,%8,%9,%10\n")
        .arg(f[1], clock(e.start, QLatin1Char('.'), 2), clock(e.end, QLatin1Char('.'), 2), f[2], f[3], f[4], f[5], f[6], f[7], text);
}

}  // namespace

Result extract(const QString &path, int streamIndex, const std::atomic<bool> &cancel, const std::function<void(float)> &progress) {
    Result r;
    if (loadCache(path, streamIndex, r)) {
        if (progress) progress(1);
        return r;
    }
    LibAv::Interrupt irq;
    irq.cancel = &cancel;
    LibAv::FormatPtr fmt = LibAv::open(path, &irq, &r.error);
    if (!fmt) return r;
    AVFormatContext *ctx = fmt.get();
    if (streamIndex < 0 || unsigned(streamIndex) >= ctx->nb_streams || ctx->streams[streamIndex]->codecpar->codec_type != AVMEDIA_TYPE_SUBTITLE) {
        r.error = __("No such subtitle stream.");
        return r;
    }
    for (unsigned i = 0; i < ctx->nb_streams; ++i)
        if (int(i) != streamIndex) ctx->streams[i]->discard = AVDISCARD_ALL;
    AVStream *st = ctx->streams[streamIndex];
    const AVCodecID id = st->codecpar->codec_id;
    const AVCodecDescriptor *desc = avcodec_descriptor_get(id);

    Kind kind;
    if (id == AV_CODEC_ID_SUBRIP || id == AV_CODEC_ID_SRT || id == AV_CODEC_ID_TEXT)
        kind = Kind::Srt;
    else if (id == AV_CODEC_ID_WEBVTT)
        kind = Kind::Vtt;
    else if ((id == AV_CODEC_ID_ASS || id == AV_CODEC_ID_SSA) && st->codecpar->extradata_size > 0)
        kind = Kind::AssRaw;
    else if (desc && (desc->props & AV_CODEC_PROP_TEXT_SUB))
        kind = Kind::Decode;
    else {
        r.error = __("Only text subtitles can be imported; this stream holds images.");
        return r;
    }

    LibAv::CodecPtr dec;
    if (kind == Kind::Decode) {
        const AVCodec *codec = avcodec_find_decoder(id);
        dec.reset(codec ? avcodec_alloc_context3(codec) : nullptr);
        if (!dec || avcodec_parameters_to_context(dec.get(), st->codecpar) < 0) {
            r.error = __("Unsupported subtitle stream.");
            return r;
        }
        dec->pkt_timebase = st->time_base;
        if (const int e = avcodec_open2(dec.get(), codec, nullptr); e < 0) {
            r.error = LibAv::errorString(e);
            return r;
        }
    }

    const qint64 startMs = ctx->start_time != AV_NOPTS_VALUE ? ctx->start_time / 1000 : 0;
    auto toMs = [&](int64_t ts) { return av_rescale_q(ts, st->time_base, AVRational{1, 1000}) - startMs; };
    const int64_t size = ctx->pb ? avio_size(ctx->pb) : -1;
    QList<Event> events;
    LibAv::PacketPtr pkt(av_packet_alloc());
    int lastPercent = -1;
    while (!cancel) {
        if (const int e = av_read_frame(ctx, pkt.get()); e < 0) {
            if (e == AVERROR_EOF || cancel) break;
            r.error = LibAv::errorString(e);   // a read error: never a partial import
            return r;
        }
        const int64_t ts = pkt->pts != AV_NOPTS_VALUE ? pkt->pts : pkt->dts;
        if (pkt->stream_index == streamIndex && ts != AV_NOPTS_VALUE) {
            Event e;
            e.start = toMs(ts);
            if (pkt->duration > 0) e.end = e.start + av_rescale_q(pkt->duration, st->time_base, AVRational{1, 1000});
            if (kind == Kind::Decode) {
                AVSubtitle sub;
                int got = 0;
                if (avcodec_decode_subtitle2(dec.get(), &sub, &got, pkt.get()) >= 0 && got) {
                    for (unsigned i = 0; i < sub.num_rects; ++i)
                        if (sub.rects[i]->ass) {
                            Event part = e;
                            part.start = e.start + sub.start_display_time;
                            if (sub.end_display_time > sub.start_display_time && sub.end_display_time < 0x7FFFFFFFu)
                                part.end = e.start + sub.end_display_time;
                            part.text = QString::fromUtf8(sub.rects[i]->ass);
                            events.append(part);
                        }
                    avsubtitle_free(&sub);
                }
            } else {
                e.text = QString::fromUtf8(reinterpret_cast<const char *>(pkt->data), pkt->size).trimmed();
                events.append(e);
            }
        }
        av_packet_unref(pkt.get());
        if (progress && size > 0) {
            const int percent = int(avio_tell(ctx->pb) * 100 / size);
            if (percent != lastPercent) progress(std::clamp((lastPercent = percent) / 100.0f, 0.0f, 0.99f));
        }
    }
    if (cancel) {
        r.error = __("Cancelled.");
        return r;
    }
    if (events.isEmpty()) {
        r.error = __("The subtitle stream is empty.");
        return r;
    }
    std::stable_sort(events.begin(), events.end(), [](const Event &a, const Event &b) { return a.start < b.start; });
    // Unknown ends: until the next subtitle, at most 5 seconds.
    for (int i = 0; i < events.size(); ++i)
        if (events[i].end <= events[i].start) {
            qint64 end = events[i].start + 5000;
            for (int j = i + 1; j < events.size(); ++j)
                if (events[j].start > events[i].start) {
                    end = std::min(end, events[j].start);
                    break;
                }
            events[i].end = end;
        }

    QString out;
    switch (kind) {
        case Kind::Srt:
            for (int i = 0; i < events.size(); ++i)
                out += QStringLiteral("%1\n%2 --> %3\n%4\n\n").arg(i + 1).arg(clock(events[i].start, QLatin1Char(','), 3), clock(events[i].end, QLatin1Char(','), 3), events[i].text);
            r.extension = QStringLiteral("srt");
            break;
        case Kind::Vtt:
            out = QStringLiteral("WEBVTT\n\n");
            for (const Event &e : events)
                out += QStringLiteral("%1 --> %2\n%3\n\n").arg(clock(e.start, QLatin1Char('.'), 3), clock(e.end, QLatin1Char('.'), 3), e.text);
            r.extension = QStringLiteral("vtt");
            break;
        default: {
            const uint8_t *hdr = kind == Kind::AssRaw ? st->codecpar->extradata : dec->subtitle_header;
            const int hdrSize = kind == Kind::AssRaw ? st->codecpar->extradata_size : dec->subtitle_header_size;
            out = hdr ? QString::fromUtf8(reinterpret_cast<const char *>(hdr), hdrSize).trimmed() : QString();
            if (!out.contains(QLatin1String("[Events]")))
                out += QString::fromLatin1(ASS_EVENTS);
            else if (!out.endsWith(QLatin1Char('\n')))
                out += QLatin1Char('\n');
            for (const Event &e : events) out += dialogue(e);
            r.extension = QStringLiteral("ass");
        }
    }
    if (progress) progress(1);
    r.text = out;
    saveCache(path, streamIndex, r);
    return r;
}

}  // namespace EmbeddedSubtitles
