/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QImage>
#include <QWidget>
#include <QPoint>
#include <QString>

struct mpv_handle;
struct mpv_render_context;
class QOpenGLWidget;

// The video preview widget: libmpv decodes and renders (render API on a GL
// context; works on Wayland/X11/macOS/Windows alike; without OpenGL its
// software renderer draws into an image instead), subtitles are drawn by
// libass from a file the preview exports. Port of the user-visible behaviour
// of `VLCPreview` without the paused-seek "nudge": mpv seeks frame-exactly
// (hr-seek) and redraws the paused frame when the subtitle file is reloaded.
class MpvPlayer : public QWidget {
    Q_OBJECT
public:
    explicit MpvPlayer(QWidget *parent = nullptr);
    ~MpvPlayer() override;

    // Whether libmpv could be created at all (false → the preview shows a label).
    bool isValid() const { return mpv_ != nullptr; }
    // Whether an OpenGL context can be made current here. Without one no
    // QOpenGLWidget may exist: it moves its whole window onto OpenGL, and a
    // window that cannot get it is never painted.
    static bool openGLAvailable();

    // Load a media file; playback starts paused at `startMs`.
    void loadMedia(const QString &path, qint64 startMs);
    bool hasMedia() const { return !path_.isEmpty(); }
    QString mediaPath() const { return path_; }
    // `path` is open, being opened or waiting for the render context — not a
    // file whose load failed (that one is only remembered).
    bool holds(const QString &path) const { return path_ == path && (loaded_ || loading_ || loadDeferred_); }

    void play();
    // Play from `startMs` to `endMs` ("play current subtitle"), then stand at
    // `startMs` again: mpv ends the file at `endMs` itself, frame exactly.
    // Any other navigation (a seek, a skip, play, pause, a new media) drops it.
    void playRange(qint64 startMs, qint64 endMs);
    void pause();
    void togglePlayPause();
    bool isPlaying() const { return playing_; }
    // Absolute, frame-exact seek (ms). Reported back through timeChanged.
    void seek(qint64 ms);
    // A drag of the seek bar: a fast (key-frame) seek.
    void scrubTo(qint64 ms);
    // Forwards or back from where the player is (mpv counts it, and stops at
    // the ends of the media).
    void skip(qint64 deltaMs);
    // Where the player stands, as it last reported — or, while a seek of ours
    // is still on its way, where it was asked to go. `timeChanged` carries the
    // reported positions only.
    double time() const { return timeMs_ / 1000.0; }
    qint64 timeMs() const { return timeMs_; }
    qint64 durationMs() const { return durationMs_; }
    void setSpeed(double rate);
    void setVolume(int percent);

    // Show the subtitles of `path` (ASS); reloading the same path re-reads it.
    void setSubtitleFile(const QString &path);
    void release();

signals:
    void playingStateChanged(bool playing);
    void timeChanged(qint64 ms);
    void durationAvailable(qint64 ms);
    void renderUnavailable();   // no render context: no video frames
    void mediaLoading(qint64 startMs);   // another file: nothing of the old one holds
    void mediaLoaded();         // the file is open: it can be played and sought
    void rangeEnded(qint64 startMs);   // a played subtitle is over, standing at its start
    void doubleClicked();       // on the picture, with a media open

public:
    QSize sizeHint() const override { return QSize(400, 256); }

protected:
    void paintEvent(QPaintEvent *e) override;   // the software renderer's frames
    // A click on the picture plays or pauses, as in media players.
    void mousePressEvent(QMouseEvent *e) override;
    void mouseReleaseEvent(QMouseEvent *e) override;
    void mouseDoubleClickEvent(QMouseEvent *e) override;

private:
    class GLView;
    // The render context: OpenGL in `glView_` (from its initializeGL) or, without
    // OpenGL, the software renderer (from the constructor).
    void createRenderContext(bool gl);
    void renderGL();
    void handleEvents();
    void handleEvent(void *event);
    // `serial` comes back with MPV_EVENT_COMMAND_REPLY; 0 for the answers we
    // have no use for.
    void command(const QStringList &args, quint64 serial = 0);
    void setOption(const char *name, const QString &value);
    // The seek itself; unlike seek() it does not end a played range. mpv keeps
    // only the newest of the seeks it has not performed yet, so each one goes
    // out at once; its serial tells its reply from those of the ones before.
    void applySeek(qint64 ms, bool exact = true);
    // The seek command, wherever the target came from (absolute or relative).
    void sendSeek(const QString &target, const QString &flags);
    // The player already shows the frame `ms` falls in (so seeking there would
    // only step the picture back to the frame before it).
    bool standsAt(qint64 ms) const;
    // Forget a played range (any navigation of the user's own does).
    void dropRange();
    // Set the end of a played range on mpv (playRange, after its play()).
    void armRange(qint64 startMs, qint64 endMs);
    static void onUpdate(void *ctx);
    static void onWakeup(void *ctx);

    mpv_handle *mpv_ = nullptr;
    mpv_render_context *renderCtx_ = nullptr;
    QOpenGLWidget *glView_ = nullptr;   // the picture with OpenGL; without it this widget paints `frame_`
    QImage frame_;
    QString path_, subPath_;
    bool playing_ = false;
    // The pause flag we last set: our intention only. mpv's reports of the flag
    // drive `playing_`, never this — a report of an older setting arriving
    // after a newer one would otherwise undo it.
    bool pauseRequested_ = true;
    bool atEof_ = false;        // paused on the last frame (keep-open)
    bool released_ = false;
    bool loaded_ = false;
    QPoint pressPos_;           // where the left button went down on the picture (screen)
    bool leftPressed_ = false;  // ... and has not come up yet
    struct Playback { bool playing = false; qint64 ms = 0; bool range = false; qint64 rangeStartMs = 0, rangeEndMs = 0; };
    Playback beforeClick_;      // at that press
    bool loading_ = false;
    bool loadDeferred_ = false;   // until the render context exists
    bool renderImpossible_ = false;   // the context failed: nothing to wait for
    qint64 pendingSeekMs_ = -1;   // asked for while the file was still loading
    qint64 loadStartMs_ = 0;      // where the load on its way will open the file
    // A seek of ours is on its way: until it lands, mpv reports the position we
    // are leaving. `seekAccepted_` says mpv has taken the newest one, so the
    // next restart of playback is its landing, not a file that just started.
    bool seeking_ = false;
    bool seekAccepted_ = false;
    // The newest report while the seek was on its way: mpv may report where it
    // lands *before* the restart, and never again if the position stays.
    qint64 landingMs_ = -1;
    quint64 seekSerial_ = 0;
    bool rangeActive_ = false;    // an A-B loop of ours is set on mpv
    qint64 rangeStartMs_ = 0;     // where it stands again once the range is over
    qint64 rangeEndMs_ = 0;
    double frameMs_ = 0;          // one frame of the media, 0 while unknown
    qint64 timeMs_ = 0, durationMs_ = 0;
    int volume_ = 100;
    double speed_ = 1.0;
};
