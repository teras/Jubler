/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/media/AudioPeaks.h"

#include <QCoreApplication>
#include <QDataStream>
#include <QDir>
#include <QFileInfo>
#include <QHash>
#include <QSaveFile>
#include <algorithm>
#include <cmath>
#include <limits>

#include "app/media/AudioDecoder.h"
#include "core/media/MediaCache.h"
#include "core/os/Debug.h"

// ---- AudioPreviewData ---------------------------------------------------------------------

AudioPreviewData AudioPreviewData::demo() {
    AudioPreviewData d;
    d.channels = 1;
    d.data = QVector<float>(LENGTH * 2, 0.5f);
    return d;
}

void AudioPreviewData::normalize() {
    for (int ch = 0; ch < channels; ++ch) {
        float max = 0, min = 1;
        for (int b = 0; b < LENGTH; ++b) {
            max = std::max(max, maxOf(ch, b));
            min = std::min(min, minOf(ch, b));
        }
        min = 0.5f - min;
        max = max - 0.5f;
        float factor = 0.5f / std::max(min, max);
        if (!std::isfinite(factor) || factor > 254.5f) factor = 0;   // flat window: keep it flat
        const float adder = (1 - factor) * 0.5f;
        for (int b = 0; b < LENGTH; ++b) {
            float &mx = data[(b * channels + ch) * 2];
            float &mn = data[(b * channels + ch) * 2 + 1];
            mx = factor * mx + adder;
            mn = factor * mn + adder;
        }
    }
}

// ---- AudioPeaks -----------------------------------------------------------------------------

namespace {
constexpr qint16 EMPTY_MAX = std::numeric_limits<qint16>::min();   // a bin without samples
constexpr qint16 EMPTY_MIN = std::numeric_limits<qint16>::max();

// Grows a [bin][channel][max,min] array to `bins` bins of empty peaks.
void grow(std::vector<qint16> &v, qint64 bins, int channels) {
    const size_t want = size_t(bins) * channels * 2;
    if (v.size() >= want) return;
    const size_t old = v.size();
    const size_t binSize = size_t(channels) * 2;
    v.resize(std::max(want, (old + old / 2) / binSize * binSize));   // whole bins only
    for (size_t i = old; i < v.size(); i += 2) {
        v[i] = EMPTY_MAX;
        v[i + 1] = EMPTY_MIN;
    }
}
// The Java's waveform cache, third generation: v1 wrote "JACACHE\1" and the peaks as bytes from the native
// ffdecode, v2 (the vlcj era) a plain PCM WAV under the same name. Ours keeps the magic and the version byte.
constexpr char CACHE_MAGIC[] = "JACACHE";
constexpr int MAGIC_SIZE = sizeof(CACHE_MAGIC) - 1;
constexpr int CACHE_VERSION = 3;   // bump when the peak algorithm or the layout changes
constexpr qint64 CACHE_LIMIT = qint64(512) << 20;

const QString CACHE_KIND = QStringLiteral("peaks");
const QString CACHE_SUFFIX = QStringLiteral(".jacache");

QString cacheDir() { return MediaCache::dir(CACHE_KIND); }
QString cacheFile(const QString &media) { return MediaCache::file(CACHE_KIND, media, QString(), CACHE_VERSION, CACHE_SUFFIX); }

}  // namespace

std::shared_ptr<AudioPeaks> AudioPeaks::forFile(const QString &path) {
    static QHash<QString, std::weak_ptr<AudioPeaks>> registry;   // GUI thread only
    const QString id = MediaCache::identity(path);
    if (auto existing = registry.value(id).lock()) return existing;
    std::shared_ptr<AudioPeaks> p(new AudioPeaks(path, id), &AudioPeaks::release);
    registry.insert(id, p);
    return p;
}

AudioPeaks::AudioPeaks(const QString &path, const QString &identity)
    : path_(path), identity_(identity), cacheFile_(cacheFile(path)) {}

AudioPeaks::~AudioPeaks() {
    cancel_ = true;
    if (worker_.joinable()) worker_.join();   // only reached once the worker has ended
}

void AudioPeaks::release(AudioPeaks *p) {
    p->cancel_ = true;
    {
        std::lock_guard<std::mutex> lock(p->lifeMutex_);
        if (p->running_) {
            p->orphaned_ = true;   // the worker finishes the job
            return;
        }
    }
    delete p;
}

bool AudioPeaks::start() {
    if (ready_) return true;
    if (running_ || path_.isEmpty()) return false;
    if (worker_.joinable()) worker_.join();
    running_ = true;
    cancel_ = false;
    worker_ = std::thread([this]() { run(); });
    return false;
}

void AudioPeaks::run() {
    const bool ok = loadCache() || analyse();
    ready_ = ok;
    emit finished(ok);
    std::lock_guard<std::mutex> lock(lifeMutex_);
    running_ = false;
    if (orphaned_)   // nobody holds it any more: delete it on the GUI thread
        QMetaObject::invokeMethod(qApp, [this]() { delete this; }, Qt::QueuedConnection);
}

bool AudioPeaks::analyse() {
    emit started();
    {
        std::lock_guard<std::mutex> lock(mutex_);
        fine_.clear();
        coarse_.clear();
        keyframes_.clear();
        bins_ = 0;
    }
    AudioDecoder::Callbacks cb;
    cb.configured = [this](const AudioDecoder::Output &out) {
        std::lock_guard<std::mutex> lock(mutex_);
        rate_ = out.rate;
        channels_ = out.channels;
        binFrames_ = std::max(1, out.rate / 500);   // 2 ms: the finest bucket the timeline asks for
    };
    cb.samples = [this](qint64 position, const qint16 *samples, int frames) {
        std::lock_guard<std::mutex> lock(mutex_);
        const int ch = channels_;
        const qint64 lastBin = (position + frames - 1) / binFrames_;
        grow(fine_, lastBin + 1, ch);
        grow(coarse_, lastBin / COARSE + 1, ch);
        bins_ = std::max(bins_, lastBin + 1);
        for (int f = 0; f < frames; ++f) {
            const qint64 bin = (position + f) / binFrames_;
            qint16 *fb = &fine_[size_t(bin) * ch * 2];
            qint16 *cbin = &coarse_[size_t(bin / COARSE) * ch * 2];
            for (int c = 0; c < ch; ++c) {
                const qint16 s = samples[f * ch + c];
                fb[c * 2] = std::max(fb[c * 2], s);
                fb[c * 2 + 1] = std::min(fb[c * 2 + 1], s);
                cbin[c * 2] = std::max(cbin[c * 2], s);
                cbin[c * 2 + 1] = std::min(cbin[c * 2 + 1], s);
            }
        }
    };
    cb.progress = [this](float p) { emit progress(p); };
    cb.keyframe = [this](double t) {
        std::lock_guard<std::mutex> lock(mutex_);
        if (keyframes_.isEmpty() || t > keyframes_.last())
            keyframes_.append(t);
        else   // B-frame reordering or a timestamp jump: keep it sorted
            keyframes_.insert(std::lower_bound(keyframes_.begin(), keyframes_.end(), t), t);
    };
    QString err;
    bool ok = false;
    try {
        ok = AudioDecoder::decode(path_, 0, 0, cancel_, cb, &err);
    } catch (const std::bad_alloc &) {   // never let the worker take the application down
        err = QStringLiteral("Out of memory");
    }
    if (!ok && !cancel_) Debug::debug(QStringLiteral("Waveform of %1 unavailable: %2").arg(path_, err));
    if (ok) saveCache();
    return ok;
}

QVector<double> AudioPeaks::keyframes(double from, double to) const {
    std::lock_guard<std::mutex> lock(mutex_);
    const auto a = std::lower_bound(keyframes_.begin(), keyframes_.end(), from);
    const auto b = std::upper_bound(a, keyframes_.end(), to);
    return QVector<double>(a, b);
}

std::unique_ptr<AudioPreviewData> AudioPeaks::getAudioPreview(double from, double to) const {
    std::lock_guard<std::mutex> lock(mutex_);
    if (bins_ <= 0 || channels_ <= 0 || to <= from) return nullptr;
    const int ch = channels_;
    const double binsPerSecond = double(rate_) / binFrames_;
    auto out = std::make_unique<AudioPreviewData>();
    out->channels = ch;
    out->data = QVector<float>(AudioPreviewData::LENGTH * ch * 2, 0.5f);
    const double span = to - from;
    for (int b = 0; b < AudioPreviewData::LENGTH; ++b) {
        qint64 f0 = qint64(std::floor((from + span * b / AudioPreviewData::LENGTH) * binsPerSecond));
        qint64 f1 = qint64(std::floor((from + span * (b + 1) / AudioPreviewData::LENGTH) * binsPerSecond));
        f1 = std::max(f1, f0 + 1);
        f0 = std::max<qint64>(f0, 0);
        f1 = std::min(f1, bins_);
        if (f0 >= f1) continue;   // outside the media: silence
        // Wide buckets read the coarse level (bucket edges rounded to it).
        const bool coarse = f1 - f0 >= 4 * COARSE;
        const std::vector<qint16> &v = coarse ? coarse_ : fine_;
        const qint64 i0 = coarse ? f0 / COARSE : f0;
        const qint64 i1 = coarse ? (f1 + COARSE - 1) / COARSE : f1;
        for (int c = 0; c < ch; ++c) {
            int mx = EMPTY_MAX, mn = EMPTY_MIN;
            for (qint64 i = i0; i < i1; ++i) {
                mx = std::max<int>(mx, v[size_t(i) * ch * 2 + c * 2]);
                mn = std::min<int>(mn, v[size_t(i) * ch * 2 + c * 2 + 1]);
            }
            if (mn > mx) continue;   // no samples: silence
            out->data[(b * ch + c) * 2] = float(mx + 32768) / 65535.0f;
            out->data[(b * ch + c) * 2 + 1] = float(mn + 32768) / 65535.0f;
        }
    }
    return out;
}

// ---- disk cache -------------------------------------------------------------------------------

void AudioPeaks::rebuildCoarse() {
    const int ch = channels_;
    coarse_.clear();
    grow(coarse_, (bins_ + COARSE - 1) / COARSE, ch);
    for (qint64 bin = 0; bin < bins_; ++bin) {
        const qint16 *fb = &fine_[size_t(bin) * ch * 2];
        qint16 *cbin = &coarse_[size_t(bin / COARSE) * ch * 2];
        for (int c = 0; c < ch * 2; c += 2) {
            cbin[c] = std::max(cbin[c], fb[c]);
            cbin[c + 1] = std::min(cbin[c + 1], fb[c + 1]);
        }
    }
}

bool AudioPeaks::loadCache() {
    QFile f(cacheFile_);
    if (!f.open(QIODevice::ReadOnly)) return false;
    QDataStream in(&f);
    char magic[MAGIC_SIZE] = {};
    quint8 version = 0;
    qint32 rate = 0, channels = 0;
    qint64 binFrames = 0, bins = 0;
    QByteArray packed;
    QVector<double> keyframes;
    in.readRawData(magic, MAGIC_SIZE);
    in >> version >> rate >> channels >> binFrames >> bins >> packed >> keyframes;
    if (in.status() != QDataStream::Ok || qstrncmp(magic, CACHE_MAGIC, MAGIC_SIZE) != 0 || version != CACHE_VERSION || rate <= 0 || channels < 1 || channels > 2
        || binFrames < 1 || bins < 1)
        return false;
    const QByteArray raw = qUncompress(packed);
    if (raw.size() != bins * channels * 2 * qint64(sizeof(qint16))) return false;
    f.close();
    MediaCache::touch(cacheFile_);   // "recently used" for the pruning
    std::lock_guard<std::mutex> lock(mutex_);
    rate_ = rate;
    channels_ = channels;
    binFrames_ = binFrames;
    bins_ = bins;
    keyframes_ = keyframes;
    fine_.assign(reinterpret_cast<const qint16 *>(raw.constData()), reinterpret_cast<const qint16 *>(raw.constData()) + raw.size() / 2);
    rebuildCoarse();
    return true;
}

void AudioPeaks::saveCache() const {
    QByteArray packed;
    qint32 rate, channels;
    qint64 binFrames, bins;
    QVector<double> keyframes;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        rate = rate_;
        channels = channels_;
        binFrames = binFrames_;
        bins = bins_;
        keyframes = keyframes_;
        packed = qCompress(reinterpret_cast<const uchar *>(fine_.data()), int(std::min<size_t>(size_t(bins) * channels * 2, fine_.size()) * sizeof(qint16)), 6);
    }
    QDir().mkpath(cacheDir());
    QSaveFile f(cacheFile_);
    if (!f.open(QIODevice::WriteOnly)) return;
    QDataStream out(&f);
    out.writeRawData(CACHE_MAGIC, MAGIC_SIZE);
    out << quint8(CACHE_VERSION) << rate << channels << binFrames << bins << packed << keyframes;
    if (!f.commit()) {
        Debug::debug(QStringLiteral("Could not store the waveform of %1: %2").arg(path_, f.errorString()));
        return;
    }
    MediaCache::prune(CACHE_KIND, CACHE_SUFFIX, CACHE_LIMIT);
}
