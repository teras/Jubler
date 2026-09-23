/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/media/MpvPlayer.h"

#include <QMetaObject>
#include <QOpenGLContext>
#include <QOpenGLFunctions>
#include <QStringList>
#include <cmath>

#include <mpv/client.h>
#include <mpv/render_gl.h>

#include "core/options/Options.h"
#include "core/os/Debug.h"

namespace {
void *getProcAddress(void *, const char *name) {
    QOpenGLContext *gl = QOpenGLContext::currentContext();
    return gl ? reinterpret_cast<void *>(gl->getProcAddress(name)) : nullptr;
}
}  // namespace

MpvPlayer::MpvPlayer(QWidget *parent) : QOpenGLWidget(parent) {
    setMinimumSize(160, 120);
    mpv_ = mpv_create();
    if (!mpv_) {
        Debug::debug(QStringLiteral("mpv: could not create the player context"));
        return;
    }
    mpv_set_option_string(mpv_, "vo", "libmpv");
    mpv_set_option_string(mpv_, "hr-seek", "yes");
    // Hardware decoding is opt-in (preference "videopreview.hardware").
    mpv_set_option_string(mpv_, "hwdec", Options::isVideoPreviewHardware() ? "auto-safe" : "no");
    mpv_set_option_string(mpv_, "keep-open", "yes");
    mpv_set_option_string(mpv_, "idle", "yes");
    mpv_set_option_string(mpv_, "pause", "yes");
    mpv_set_option_string(mpv_, "sub-visibility", "yes");
    mpv_set_option_string(mpv_, "sub-auto", "no");
    mpv_set_option_string(mpv_, "audio-file-auto", "no");
    mpv_set_option_string(mpv_, "osc", "no");
    mpv_set_option_string(mpv_, "input-default-bindings", "no");
    mpv_set_option_string(mpv_, "input-vo-keyboard", "no");
    mpv_set_option_string(mpv_, "terminal", "no");
    if (mpv_initialize(mpv_) < 0) {
        Debug::debug(QStringLiteral("mpv: could not initialise the player"));
        mpv_terminate_destroy(mpv_);
        mpv_ = nullptr;
        return;
    }
    mpv_observe_property(mpv_, 0, "time-pos", MPV_FORMAT_DOUBLE);
    mpv_observe_property(mpv_, 0, "duration", MPV_FORMAT_DOUBLE);
    mpv_observe_property(mpv_, 0, "pause", MPV_FORMAT_FLAG);
    mpv_observe_property(mpv_, 0, "eof-reached", MPV_FORMAT_FLAG);
    mpv_observe_property(mpv_, 0, "container-fps", MPV_FORMAT_DOUBLE);
    mpv_request_log_messages(mpv_, "warn");
    mpv_set_wakeup_callback(mpv_, &MpvPlayer::onWakeup, this);
}

MpvPlayer::~MpvPlayer() {
    release();
}

void MpvPlayer::release() {
    if (released_) return;
    released_ = true;
    makeCurrent();
    if (renderCtx_) {
        mpv_render_context_free(renderCtx_);
        renderCtx_ = nullptr;
    }
    doneCurrent();
    if (mpv_) {
        mpv_set_wakeup_callback(mpv_, nullptr, nullptr);
        mpv_terminate_destroy(mpv_);
        mpv_ = nullptr;
    }
}

void MpvPlayer::initializeGL() {
    if (!mpv_ || renderCtx_) return;
    mpv_opengl_init_params glInit{getProcAddress, nullptr};
    int advanced = 0;
    mpv_render_param params[]{
        {MPV_RENDER_PARAM_API_TYPE, const_cast<char *>(MPV_RENDER_API_TYPE_OPENGL)},
        {MPV_RENDER_PARAM_OPENGL_INIT_PARAMS, &glInit},
        {MPV_RENDER_PARAM_ADVANCED_CONTROL, &advanced},
        {MPV_RENDER_PARAM_INVALID, nullptr},
    };
    if (mpv_render_context_create(&renderCtx_, mpv_, params) < 0) {
        renderCtx_ = nullptr;
        renderImpossible_ = true;
        Debug::debug(QStringLiteral("mpv: no usable GL render context; video disabled"));
        // No picture will ever come, so the player stops waiting for one: without
        // a video output the sound, the clock and everything driven by them go on
        // (the panel shows the notice in place of the frame).
        setOption("vo", QStringLiteral("null"));
        if (loadDeferred_) {
            loadDeferred_ = false;
            loading_ = true;
            command({QStringLiteral("loadfile"), path_, QStringLiteral("replace")});
        }
        QMetaObject::invokeMethod(this, &MpvPlayer::renderUnavailable, Qt::QueuedConnection);   // not from inside initializeGL
        return;
    }
    mpv_render_context_set_update_callback(renderCtx_, &MpvPlayer::onUpdate, this);
    if (loadDeferred_) {
        loadDeferred_ = false;
        loading_ = true;
        command({QStringLiteral("loadfile"), path_, QStringLiteral("replace")});
    }
}

void MpvPlayer::paintGL() {
    if (!renderCtx_) return;
    const qreal dpr = devicePixelRatioF();
    mpv_opengl_fbo fbo{int(defaultFramebufferObject()), int(width() * dpr), int(height() * dpr), 0};
    int flipY = 1;
    mpv_render_param params[]{
        {MPV_RENDER_PARAM_OPENGL_FBO, &fbo},
        {MPV_RENDER_PARAM_FLIP_Y, &flipY},
        {MPV_RENDER_PARAM_INVALID, nullptr},
    };
    mpv_render_context_render(renderCtx_, params);
    // mpv leaves the context in the OpenGL standard defaults, and the default framebuffer binding is 0;
    // QOpenGLWidget requires paintGL() to return with its own framebuffer bound, and goes on to discard it
    // with attachment names only a framebuffer object accepts. Left unbound, that discard hits the default
    // framebuffer instead (GL_INVALID_ENUM every frame, which mpv then reported as its own error).
    if (QOpenGLContext *glc = QOpenGLContext::currentContext())
        glc->functions()->glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebufferObject());
}

void MpvPlayer::onUpdate(void *ctx) {
    // Render thread → GUI thread.
    QMetaObject::invokeMethod(static_cast<MpvPlayer *>(ctx), [ctx]() { static_cast<MpvPlayer *>(ctx)->update(); }, Qt::QueuedConnection);
}

void MpvPlayer::onWakeup(void *ctx) {
    QMetaObject::invokeMethod(static_cast<MpvPlayer *>(ctx), [ctx]() { static_cast<MpvPlayer *>(ctx)->handleEvents(); }, Qt::QueuedConnection);
}

void MpvPlayer::handleEvents() {
    while (mpv_ && !released_) {
        mpv_event *event = mpv_wait_event(mpv_, 0);
        if (event->event_id == MPV_EVENT_NONE) break;
        handleEvent(event);
    }
}

void MpvPlayer::handleEvent(void *ev) {
    auto *event = static_cast<mpv_event *>(ev);
    switch (event->event_id) {
        case MPV_EVENT_PROPERTY_CHANGE: {
            auto *prop = static_cast<mpv_event_property *>(event->data);
            if (!prop->data) break;
            if (qstrcmp(prop->name, "time-pos") == 0 && prop->format == MPV_FORMAT_DOUBLE) {
                const qint64 ms = qint64(std::llround(*static_cast<double *>(prop->data) * 1000.0));
                if (seeking_) {
                    landingMs_ = ms;   // believed only once the seek has landed
                    break;
                }
                timeMs_ = ms;
                emit timeChanged(timeMs_);
            } else if (qstrcmp(prop->name, "duration") == 0 && prop->format == MPV_FORMAT_DOUBLE) {
                const qint64 d = qint64(std::llround(*static_cast<double *>(prop->data) * 1000.0));
                if (d > 0 && d != durationMs_) {
                    durationMs_ = d;
                    // Seeks made before the length was known could not be clamped.
                    timeMs_ = std::min(timeMs_, durationMs_);
                    if (pendingSeekMs_ > durationMs_) pendingSeekMs_ = durationMs_;
                    emit durationAvailable(durationMs_);
                }
            } else if (qstrcmp(prop->name, "pause") == 0 && prop->format == MPV_FORMAT_FLAG) {
                const bool playing = *static_cast<int *>(prop->data) == 0 && loaded_;
                if (playing != playing_) {
                    playing_ = playing;
                    emit playingStateChanged(playing_);
                }
            } else if (qstrcmp(prop->name, "container-fps") == 0 && prop->format == MPV_FORMAT_DOUBLE) {
                const double fps = *static_cast<double *>(prop->data);
                frameMs_ = fps > 0 ? 1000.0 / fps : 0;
            } else if (qstrcmp(prop->name, "eof-reached") == 0 && prop->format == MPV_FORMAT_FLAG) {
                atEof_ = *static_cast<int *>(prop->data) != 0;
                if (atEof_) {
                    // keep-open: the player pauses on the last frame.
                    if (playing_) {
                        playing_ = false;
                        emit playingStateChanged(false);
                    }
                    // The end of a played subtitle (mpv stopped the file there),
                    // or the end of the media for one that reaches past it. The
                    // pause flag of keep-open stays off, so it is set here.
                    if (rangeActive_) {
                        const qint64 back = rangeStartMs_;
                        dropRange();
                        pause();
                        applySeek(back);
                        emit rangeEnded(back);
                    }
                }
            }
            break;
        }
        case MPV_EVENT_FILE_LOADED: {
            loaded_ = true;
            loading_ = false;
            emit mediaLoaded();
            // Options set before the load apply to it; re-apply the user state.
            setOption("volume", QString::number(volume_));
            setOption("speed", QString::number(speed_));
            if (!subPath_.isEmpty()) command({QStringLiteral("sub-add"), subPath_, QStringLiteral("select")});
            if (pendingSeekMs_ >= 0) {
                applySeek(pendingSeekMs_);   // the move already asked for, not a new one
                pendingSeekMs_ = -1;
            }
            emit timeChanged(timeMs_);
            if (durationMs_ > 0) emit durationAvailable(durationMs_);
            // A play() issued while loading: the pause flag is already off
            // (never read synchronously: the core may be waiting on the GUI thread).
            if (!pauseRequested_ && !playing_) {
                playing_ = true;
                emit playingStateChanged(true);
            }
            break;
        }
        case MPV_EVENT_COMMAND_REPLY:
            // Only the newest seek matters: the replies of the ones mpv dropped
            // in favour of it carry an older serial.
            if (event->reply_userdata == seekSerial_ && seeking_) {
                if (event->error < 0) {
                    // Refused (a stream that cannot be sought, nothing loaded):
                    // there will be no landing to wait for.
                    Debug::debug(QStringLiteral("mpv: the seek was refused: ") + QString::fromUtf8(mpv_error_string(event->error)));
                    seeking_ = seekAccepted_ = false;
                } else {
                    seekAccepted_ = true;   // the next restart of playback is its landing
                }
            }
            break;
        case MPV_EVENT_PLAYBACK_RESTART:
            if (seekAccepted_) {   // our seek landed
                seeking_ = seekAccepted_ = false;
                if (landingMs_ >= 0) {
                    timeMs_ = landingMs_;
                    emit timeChanged(timeMs_);
                }
            }
            break;
        case MPV_EVENT_LOG_MESSAGE: {
            auto *msg = static_cast<mpv_event_log_message *>(event->data);
            Debug::debug(QStringLiteral("mpv [%1] %2").arg(QString::fromUtf8(msg->prefix), QString::fromUtf8(msg->text).trimmed()));
            break;
        }
        case MPV_EVENT_END_FILE: {
            auto *ef = static_cast<mpv_event_end_file *>(event->data);
            if (ef && ef->reason == MPV_END_FILE_REASON_ERROR)
                Debug::debug(QStringLiteral("mpv: playback failed: ") + QString::fromUtf8(mpv_error_string(ef->error)));
            loaded_ = false;
            dropRange();   // whatever was being played is gone with the file
            // "loadfile … replace" ends the outgoing file: that report is about
            // the one being dropped, while ours is still on its way.
            if (!ef || ef->reason != MPV_END_FILE_REASON_STOP) loading_ = false;
            seeking_ = seekAccepted_ = false;
            if (playing_) {
                playing_ = false;
                emit playingStateChanged(false);
            }
            break;
        }
        default:
            break;
    }
}

void MpvPlayer::command(const QStringList &args, quint64 serial) {
    if (!mpv_ || released_) return;
    QList<QByteArray> bytes;
    QList<const char *> argv;
    for (const QString &a : args) bytes.append(a.toUtf8());
    for (const QByteArray &b : bytes) argv.append(b.constData());
    argv.append(nullptr);
    mpv_command_async(mpv_, serial, argv.data());
}

// Every call into libmpv from the GUI thread is asynchronous: the core may
// be waiting for us to render a frame, so a synchronous call would deadlock.
void MpvPlayer::setOption(const char *name, const QString &value) {
    if (!mpv_ || released_) return;
    QByteArray v = value.toUtf8();
    char *data = v.data();
    mpv_set_property_async(mpv_, 0, name, MPV_FORMAT_STRING, &data);
}

void MpvPlayer::loadMedia(const QString &path, qint64 startMs) {
    dropRange();
    path_ = path;
    loaded_ = false;
    durationMs_ = 0;
    timeMs_ = std::max<qint64>(0, startMs);
    pendingSeekMs_ = -1;   // the file opens where it should ("start" below)
    const bool wasAtEof = atEof_;   // keep-open leaves "pause" off at the end
    atEof_ = false;
    seeking_ = seekAccepted_ = false;   // of the file being left behind
    // The length, the position and everything drawn from them belong to the
    // file that is going: until the new one is open, only its start is known.
    emit mediaLoading(timeMs_);
    // Media changed while playing keeps playing (Java); otherwise it loads
    // paused — and one that had reached its end was not playing, whatever the
    // pause flag of keep-open says. Asked for, not reported: a Pause that mpv
    // has not applied yet is a pause already.
    pauseRequested_ = pauseRequested_ || wasAtEof || path.isEmpty();
    setOption("pause", pauseRequested_ ? QStringLiteral("yes") : QStringLiteral("no"));
    // The hardware-decoding preference applies from the next media load on.
    setOption("hwdec", Options::isVideoPreviewHardware() ? QStringLiteral("auto-safe") : QStringLiteral("no"));
    // mpv opens the file at the position we want (hr-seek): no seek of our own
    // after it loads, so the first frame shown is already the right one.
    loadStartMs_ = timeMs_;
    setOption("start", QString::number(timeMs_ / 1000.0, 'f', 3));
    // The video output needs the GL render context, created on first show.
    if (!renderCtx_ && !renderImpossible_) {
        loadDeferred_ = true;
        loading_ = false;
        return;
    }
    if (path.isEmpty()) {
        loading_ = false;
        command({QStringLiteral("stop")});   // no file at all, not a file named ""
        return;
    }
    loading_ = true;
    command({QStringLiteral("loadfile"), path, QStringLiteral("replace")});
}

void MpvPlayer::play() {
    dropRange();
    if (!hasMedia()) return;
    if (atEof_ && loaded_) seek(0);   // Play at the end starts over (Java)
    if (!loaded_ && !loading_ && !loadDeferred_) {
        loading_ = true;   // never loaded, or the file ended: open it again
        loadStartMs_ = timeMs_;
        setOption("start", QString::number(timeMs_ / 1000.0, 'f', 3));
        command({QStringLiteral("loadfile"), path_, QStringLiteral("replace")});
    }
    pauseRequested_ = false;
    setOption("pause", QStringLiteral("no"));
}

// mpv ends the range itself: "end" stops the file at that time, frame exactly
// and with the sound, and keep-open holds the last frame — the same
// "eof-reached" that the real end of a media raises, which is where we pause
// and go back to the start of the subtitle.
void MpvPlayer::playRange(qint64 startMs, qint64 endMs) {
    // A subtitle rarely begins on a frame: the player stands on the first frame
    // of it, while a seek to its time shows the frame that time falls in, the
    // one before — so seek() does nothing when the frame is already the one
    // shown, and the picture no longer steps back and forth at every press.
    seek(startMs);
    play();
    if (endMs <= startMs) return;
    rangeStartMs_ = std::max<qint64>(0, startMs);
    rangeActive_ = true;
    setOption("end", QString::number(endMs / 1000.0, 'f', 3));
}

void MpvPlayer::pause() {
    dropRange();
    pauseRequested_ = true;
    setOption("pause", QStringLiteral("yes"));
}

void MpvPlayer::togglePlayPause() {
    if (playing_) pause(); else play();
}

void MpvPlayer::scrubTo(qint64 ms) {
    dropRange();
    ms = std::max<qint64>(0, ms);
    if (!loaded_) {
        seek(ms);
        return;
    }
    timeMs_ = ms;
    applySeek(ms, false);   // a key-frame seek: the drag wants speed, not accuracy
    emit timeChanged(ms);   // the label, the slider and the preview follow at once
}

void MpvPlayer::seek(qint64 ms) {
    dropRange();   // going somewhere else ends a played range
    if (loaded_ && standsAt(ms)) return;   // the frame of that moment is the one shown
    applySeek(ms);
}

bool MpvPlayer::standsAt(qint64 ms) const {
    const qint64 frame = frameMs_ > 0 ? qint64(std::ceil(frameMs_)) : 1;
    return timeMs_ >= ms && timeMs_ < ms + frame;
}

void MpvPlayer::dropRange() {
    if (!rangeActive_) return;
    rangeActive_ = false;
    setOption("end", QStringLiteral("none"));
}

void MpvPlayer::applySeek(qint64 ms, bool exact) {
    ms = std::max<qint64>(0, ms);
    // Past the end the player does not move and reports nothing, so the
    // position we keep would stay in a place the media never had — and the
    // skip buttons would need as many presses back to return from it.
    if (durationMs_ > 0) ms = std::min(ms, durationMs_);
    timeMs_ = ms;
    if (!loaded_) {
        // The file is not open yet, so it is opened at this position: "start"
        // is read when the load begins, which is why a load already on its way
        // still needs the seek kept for when it is done.
        // A load already under way has read "start": that one needs the seek
        // kept for when it is done, unless it opens there anyway.
        pendingSeekMs_ = loading_ && ms != loadStartMs_ ? ms : -1;
        if (!loading_) {
            loadStartMs_ = ms;
            setOption("start", QString::number(ms / 1000.0, 'f', 3));
        }
        if (hasMedia() && !loading_ && !loadDeferred_) {
            loading_ = true;
            command({QStringLiteral("loadfile"), path_, QStringLiteral("replace")});
        }
        // Nothing will report back before the file is there: show where it goes.
        emit timeChanged(ms);
        return;
    }
    // The player can only stand on a frame, so it lands on the first one at or
    // after `ms`; the position comes from its own report of that landing.
    sendSeek(QString::number(ms / 1000.0, 'f', 3), exact ? QStringLiteral("absolute+exact") : QStringLiteral("absolute+keyframes"));
}

void MpvPlayer::sendSeek(const QString &target, const QString &flags) {
    atEof_ = false;
    seeking_ = true;
    seekAccepted_ = false;
    landingMs_ = -1;
    command({QStringLiteral("seek"), target, flags}, ++seekSerial_);
}

// Counted by mpv from where it really is, not from where we think it is: the
// ends of the media are its business too, so pressing past them costs nothing.
void MpvPlayer::skip(qint64 deltaMs) {
    dropRange();   // going somewhere else ends a played range
    if (!loaded_) {
        seek(std::max<qint64>(0, timeMs_ + deltaMs));   // nothing to be relative to yet
        return;
    }
    // Where it is going, as applySeek keeps it: the view is placed there at
    // once, and the report of the landing corrects it.
    timeMs_ = std::max<qint64>(0, timeMs_ + deltaMs);
    if (durationMs_ > 0) timeMs_ = std::min(timeMs_, durationMs_);
    sendSeek(QString::number(deltaMs / 1000.0, 'f', 3), QStringLiteral("relative+exact"));
}

void MpvPlayer::setSpeed(double rate) {
    speed_ = rate;
    setOption("speed", QString::number(rate));
}

void MpvPlayer::setVolume(int percent) {
    volume_ = std::clamp(percent, 0, 100);
    setOption("mute", QStringLiteral("no"));
    setOption("volume", QString::number(volume_));
}

void MpvPlayer::setSubtitleFile(const QString &path) {
    const bool same = path == subPath_;
    subPath_ = path;
    if (!loaded_) return;   // applied on FILE_LOADED
    if (same)
        command({QStringLiteral("sub-reload")});
    else
        command({QStringLiteral("sub-add"), path, QStringLiteral("select")});
}
