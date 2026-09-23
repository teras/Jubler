/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/media/MediaProbe.h"

#include <QCoreApplication>
#include <QDialog>
#include <QElapsedTimer>
#include <QEventLoop>
#include <QFileInfo>
#include <QLabel>
#include <QProgressBar>
#include <QSet>
#include <QThread>
#include <QTimer>
#include <QVBoxLayout>
#include <QtConcurrent>
#include <mutex>


#include "app/media/LibAv.h"
#include "core/i18n/I18N.h"
#include "core/media/MediaFile.h"
#include "core/os/Debug.h"

QString SubtitleStreamInfo::getField(const QString &field) const {
    if (field == QLatin1String("id")) return QString::number(id);
    if (field == QLatin1String("language")) return language;
    return QString::number(index);
}

namespace {

QString metadata(const AVDictionary *dict, const char *key) {
    const AVDictionaryEntry *e = av_dict_get(dict, key, nullptr, 0);
    return e ? QString::fromUtf8(e->value) : QString();
}

// The main video stream: the best one that is not cover art.
const AVStream *videoStream(AVFormatContext *ctx) {
    const int best = av_find_best_stream(ctx, AVMEDIA_TYPE_VIDEO, -1, -1, nullptr, 0);
    if (best >= 0 && !(ctx->streams[best]->disposition & AV_DISPOSITION_ATTACHED_PIC))
        return ctx->streams[best];
    for (unsigned i = 0; i < ctx->nb_streams; ++i) {
        const AVStream *st = ctx->streams[i];
        if (st->codecpar->codec_type == AVMEDIA_TYPE_VIDEO && !(st->disposition & AV_DISPOSITION_ATTACHED_PIC))
            return st;
    }
    return nullptr;
}

}  // namespace

namespace MediaProbe {

MediaInfo probe(const QString &path, int timeoutMs) {
    MediaInfo info;
    if (path.isEmpty() || !QFileInfo::exists(path)) return info;
    LibAv::Interrupt irq;
    irq.setTimeout(timeoutMs);
    QString err;
    const LibAv::FormatPtr fmt = LibAv::open(path, &irq, &err);
    if (!fmt) {
        Debug::debug(QStringLiteral("Unable to read media information of %1: %2").arg(path, err));
        return info;
    }
    AVFormatContext *ctx = fmt.get();
    info.ok = true;
    if (ctx->duration > 0) info.duration = double(ctx->duration) / AV_TIME_BASE;
    if (const AVStream *st = videoStream(ctx)) {
        info.width = st->codecpar->width;
        info.height = st->codecpar->height;
        info.fps = av_q2d(av_guess_frame_rate(ctx, const_cast<AVStream *>(st), nullptr));
    }
    int subIndex = 0;
    for (unsigned i = 0; i < ctx->nb_streams; ++i) {
        const AVStream *st = ctx->streams[i];
        if (st->codecpar->codec_type != AVMEDIA_TYPE_SUBTITLE) continue;
        const AVCodecDescriptor *desc = avcodec_descriptor_get(st->codecpar->codec_id);
        SubtitleStreamInfo s;
        s.index = subIndex++;
        s.id = st->index;
        s.language = metadata(st->metadata, "language");
        if (s.language == QLatin1String("und")) s.language.clear();   // ISO 639 "undetermined"
        s.title = metadata(st->metadata, "title");
        s.codecName = QString::fromUtf8(avcodec_get_name(st->codecpar->codec_id));
        s.codecDescription = desc && desc->long_name ? QString::fromUtf8(desc->long_name) : s.codecName;
        s.extractable = desc && (desc->props & AV_CODEC_PROP_TEXT_SUB);
        info.subtitleStreams.append(s);
    }
    return info;
}

namespace {
std::mutex g_mutex;
QSet<VideoFile *> g_pending;   // probes still running
}  // namespace

void start(const std::shared_ptr<VideoFile> &video) {
    if (!video) return;
    if (!video->exists()) {
        video->setInformation(video->getWidth(), video->getHeight(), video->getLength(), video->getFPS());
        return;
    }
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_pending.insert(video.get());
    }
    const QString path = video->getPath();
    auto future = QtConcurrent::run([video, path]() {
        const MediaInfo info = probe(path);
        // Apply on the GUI thread so readers never race the writer.
        QMetaObject::invokeMethod(qApp, [video, info]() {
            video->setInformation(info.width > 0 ? info.width : VideoFile::DEFAULT_WIDTH,
                                  info.height > 0 ? info.height : VideoFile::DEFAULT_HEIGHT,
                                  info.duration > 0 ? float(info.duration) : VideoFile::DEFAULT_LENGTH,
                                  info.fps > 0 ? float(info.fps) : VideoFile::DEFAULT_FPS);
            std::lock_guard<std::mutex> lock(g_mutex);
            g_pending.remove(video.get());
        }, Qt::QueuedConnection);
    });
    Q_UNUSED(future);
}

namespace {
// The wait dialog cannot be dismissed (Escape included): it closes itself.
class WaitDialog : public QDialog {
public:
    using QDialog::QDialog;
    void reject() override {}
};
}  // namespace

void await(VideoFile *video, int timeoutMs, QWidget *parent) {
    if (!video || video->isInfoReady()) return;
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        if (!g_pending.contains(video)) return;
    }
    WaitDialog dlg(parent);
    dlg.setWindowTitle(__("Reading media information…"));
    dlg.setWindowFlags(dlg.windowFlags() & ~Qt::WindowCloseButtonHint);
    auto *lay = new QVBoxLayout(&dlg);
    lay->setContentsMargins(20, 16, 20, 16);
    lay->setSpacing(12);
    lay->addWidget(new QLabel(__("Reading media information…"), &dlg));
    auto *bar = new QProgressBar(&dlg);
    bar->setRange(0, 0);
    lay->addWidget(bar);
    QEventLoop loop;
    QTimer poll;
    poll.setInterval(50);
    QObject::connect(&poll, &QTimer::timeout, &loop, [&]() { if (video->isInfoReady()) loop.quit(); });
    QTimer::singleShot(timeoutMs, &loop, &QEventLoop::quit);
    poll.start();
    dlg.setModal(true);
    // Most probes end within milliseconds: show the dialog only for a slow one (network mounts).
    QTimer::singleShot(400, &dlg, [&dlg]() { dlg.show(); });
    loop.exec(QEventLoop::ExcludeUserInputEvents);   // nothing may be started or closed meanwhile
    dlg.hide();
}

QList<SubtitleStreamInfo> subtitleStreams(const QString &path) {
    return probe(path, 15000).subtitleStreams;
}

}  // namespace MediaProbe
