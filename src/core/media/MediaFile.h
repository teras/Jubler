/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <memory>

// The video/audio pair attached to a document. Only the data part
// lives in the core; probing (duration, fps, size via libavformat) and the media
// selector are in the app layer. Port of `VideoFile`/`AudioFile`/
// `MediaFile` (the guessing/probing parts are added in the media phase).
class VideoFile {
public:
    static constexpr int DEFAULT_WIDTH = 320;
    static constexpr int DEFAULT_HEIGHT = 288;
    static constexpr float DEFAULT_LENGTH = 60;   // seconds
    static constexpr float DEFAULT_FPS = 25;

    VideoFile() = default;
    explicit VideoFile(const QString &path) : path_(path) {}

    QString getPath() const { return path_; }
    bool exists() const;
    void setInformation(int width, int height, float length, float fps) {
        width_ = width; height_ = height; length_ = length; fps_ = fps; infoReady_ = true;
    }
    int getWidth() const { return width_; }
    int getHeight() const { return height_; }
    float getLength() const { return length_; }
    float getFPS() const { return fps_; }
    bool isInfoReady() const { return infoReady_; }

private:
    QString path_;
    int width_ = DEFAULT_WIDTH;
    int height_ = DEFAULT_HEIGHT;
    float length_ = DEFAULT_LENGTH;
    float fps_ = DEFAULT_FPS;
    bool infoReady_ = false;
};

class AudioFile {
public:
    AudioFile() = default;
    AudioFile(const QString &path, const VideoFile &video) : path_(path), sameAsVideo_(path == video.getPath()) {}
    QString getPath() const { return path_; }
    bool isSameAsVideo() const { return sameAsVideo_; }

private:
    QString path_;
    bool sameAsVideo_ = false;
};

class MediaFile {
public:
    virtual ~MediaFile() = default;
    const VideoFile *getVideoFile() const { return video_ ? video_.get() : nullptr; }
    VideoFile *getVideoFile() { return video_ ? video_.get() : nullptr; }
    const AudioFile *getAudioFile() const { return audio_ ? audio_.get() : nullptr; }
    void setVideoFile(std::shared_ptr<VideoFile> v) { video_ = std::move(v); }
    void setAudioFile(std::shared_ptr<AudioFile> a) { audio_ = std::move(a); }

protected:
    std::shared_ptr<VideoFile> video_;
    std::shared_ptr<AudioFile> audio_;
};
