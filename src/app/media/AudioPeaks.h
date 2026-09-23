/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QObject>
#include <QString>
#include <QVector>
#include <atomic>
#include <mutex>
#include <memory>
#include <thread>
#include <vector>

// The waveform window data: per channel 1000 buckets, each with the maximum
// and minimum sample mapped to 0..1 (0.5 = silence). Port of
// `AudioPreviewData`.
struct AudioPreviewData {
    static constexpr int LENGTH = 1000;
    int channels = 0;
    // bucket-major: data[(bucket * channels + channel) * 2 + {0: max, 1: min}]
    QVector<float> data;

    static AudioPreviewData demo();   // one silent channel
    float maxOf(int channel, int bucket) const { return data[(bucket * channels + channel) * 2]; }
    float minOf(int channel, int bucket) const { return data[(bucket * channels + channel) * 2 + 1]; }
    // "Maximize waveform": scale every channel around 0.5 so its loudest
    // peak touches 0 or 1; a flat window stays flat.
    void normalize();
};

// Progress callbacks of the waveform analysis (the waveform panel), port of
// `AudioPreview.AudioStateCallback`.
class AudioPeaksListener {
public:
    virtual ~AudioPeaksListener() = default;
    virtual void startPeaks() = 0;
    virtual void stopPeaks() = 0;
    virtual void updatePeaks(float position) = 0;   // 0..1, or -1 when unknown
    // The object whose destruction drops pending notifications.
    virtual QObject *peaksContext() = 0;
};

// The waveform of one audio source: its default audio stream decoded once
// (in the background, libav) to 16-bit min/max peaks per 2 ms held in memory,
// shared by every window showing the same file. The peaks decoded so far can
// be drawn while the analysis runs. Complete peaks are kept in the user cache
// folder (about 12 MB per hour of stereo, 512 MB in total, least recently used
// dropped first) so a file is decoded
// once, not at every start.
class AudioPeaks : public QObject {
    Q_OBJECT
public:
    // The instance of `path` as it is now on disk (MediaCache::identity),
    // shared while anyone holds it: a file changed under the same name is
    // another instance, analysed again.
    static std::shared_ptr<AudioPeaks> forFile(const QString &path);
    ~AudioPeaks() override;

    QString path() const { return path_; }
    // The version of the file these peaks are of.
    QString identity() const { return identity_; }
    // Starts the analysis unless it is done or running; true when ready.
    bool start();
    bool isReady() const { return ready_; }
    bool isRunning() const { return running_; }
    void cancel() { cancel_ = true; }
    // Peaks of [from,to] seconds (the part decoded so far while running), or
    // null before the first samples.
    std::unique_ptr<AudioPreviewData> getAudioPreview(double from, double to) const;
    // The video key frames (seconds) within [from,to], sorted.
    QVector<double> keyframes(double from, double to) const;

signals:
    void started();
    void progress(float position);   // 0..1, or -1 when unknown; new peaks are available
    void finished(bool success);

private:
    AudioPeaks(const QString &path, const QString &identity);
    // The deleter of the shared instances: never waits for a worker stuck in
    // I/O (a stale network mount) on the GUI thread — a running worker is
    // cancelled and deletes the object itself when it ends.
    static void release(AudioPeaks *p);
    void run();
    bool analyse();
    bool loadCache();
    void saveCache() const;
    void rebuildCoarse();

    static constexpr int COARSE = 64;   // fine bins per coarse bin
    QString path_, identity_;
    QString cacheFile_;   // named after the version analysed, not the one on disk later
    std::atomic<bool> running_{false}, ready_{false}, cancel_{false};
    std::mutex lifeMutex_;   // running_ ↔ orphaned_ hand-over
    bool orphaned_ = false;
    std::thread worker_;
    mutable std::mutex mutex_;   // guards the fields below while the worker writes them
    int rate_ = 0, channels_ = 0;
    qint64 binFrames_ = 1, bins_ = 0;
    std::vector<qint16> fine_, coarse_;   // [bin][channel][max, min]
    QVector<double> keyframes_;           // sorted once the analysis is complete
};
