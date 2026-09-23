/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/media/AppMediaFile.h"

#include <QCheckBox>
#include <QDialog>
#include <QDialogButtonBox>
#include <QDir>
#include <QFileDialog>
#include <QFileInfo>
#include <QHBoxLayout>
#include <QLabel>
#include <QLineEdit>
#include <QMessageBox>
#include <QPushButton>
#include <QVBoxLayout>

#include "app/media/MediaProbe.h"
#include "core/media/MediaCache.h"
#include "core/i18n/I18N.h"
#include "core/os/FileCommunicator.h"
#include "core/os/SystemDependent.h"
#include "core/subs/Subtitles.h"

// ---- filters ----------------------------------------------------------------------------------

namespace MediaFilters {

const QStringList &videoExtensions() {
    static const QStringList l{QStringLiteral(".avi"), QStringLiteral(".mpg"), QStringLiteral(".mpeg"), QStringLiteral(".m1v"), QStringLiteral(".m2v"),
                               QStringLiteral(".mov"), QStringLiteral(".mkv"), QStringLiteral(".ogm"), QStringLiteral(".divx"), QStringLiteral(".bin"),
                               QStringLiteral(".wmv"), QStringLiteral(".flv"), QStringLiteral(".mp4"), QStringLiteral(".m4v"), QStringLiteral(".webm")};
    return l;
}

const QStringList &audioExtensions() {
    static const QStringList l{QStringLiteral(".wav"), QStringLiteral(".mp3"), QStringLiteral(".ogg"), QStringLiteral(".ac3"), QStringLiteral(".m4a"),
                               QStringLiteral(".flac"), QStringLiteral(".aac"), QStringLiteral(".opus")};
    return l;
}

static bool endsWithAny(const QString &path, const QStringList &exts) {
    const QString name = QFileInfo(path).fileName().toLower();
    for (const QString &e : exts)
        if (name.endsWith(e)) return true;
    return false;
}

bool isVideo(const QString &path) { return endsWithAny(path, videoExtensions()); }
bool isAudio(const QString &path) { return endsWithAny(path, audioExtensions()); }
bool isMedia(const QString &path) { return isVideo(path) || isAudio(path); }

static QString globs(const QStringList &exts) {
    QStringList g;
    for (const QString &e : exts) g.append(QLatin1Char('*') + e);
    return g.join(QLatin1Char(' '));
}

QString videoFilter() { return __("All Video files") + QStringLiteral(" (") + globs(videoExtensions()) + QLatin1Char(')'); }
QString audioFilter() { return __("All Audio files") + QStringLiteral(" (") + globs(audioExtensions()) + QLatin1Char(')'); }
QString mediaFilter() { return __("All Media files") + QStringLiteral(" (") + globs(videoExtensions() + audioExtensions()) + QLatin1Char(')'); }

}  // namespace MediaFilters

// ---- MediaSelector ------------------------------------------------------------------------------

MediaSelector::MediaSelector(QWidget *parent) : QWidget(parent) {
    auto *lay = new QVBoxLayout(this);
    lay->setContentsMargins(0, 0, 0, 0);
    auto row = [&](QLineEdit *&field, QPushButton *&button, const QString &fieldTip, const QString &buttonTip) {
        auto *h = new QHBoxLayout();
        field = new QLineEdit(this);
        field->setReadOnly(true);
        field->setMinimumWidth(320);
        field->setToolTip(fieldTip);
        button = new QPushButton(__("Browse"), this);
        button->setToolTip(buttonTip);
        h->addWidget(field, 1);
        h->addWidget(button);
        lay->addLayout(h);
    };
    lay->addWidget(new QLabel(__("Use the following audio/video file"), this));
    row(vfName_, vBrowse_, __("Filename of the audio/video file. Use the \"Browse\" button to change it."), __("Change the audio/video filename"));
    lay->addSpacing(6);
    externalAudio_ = new QCheckBox(__("Use a different audio stream"), this);
    externalAudio_->setToolTip(__("Use an audio stream outside from this video file.\n(E.g. a WAV or MP3 file)"));
    lay->addWidget(externalAudio_);
    row(afName_, aBrowse_, __("Filename of the external audio file. Use the \"Browse\" button to change it."), __("Change the external audio filename"));

    connect(vBrowse_, &QPushButton::clicked, this, [this]() {
        if (!mfile_) return;
        const QString cur = mfile_->getVideoFile() ? mfile_->getVideoFile()->getPath() : QString();
        const QString f = chooseFile(cur, MediaFilters::mediaFilter() + QStringLiteral(";;") + MediaFilters::videoFilter() + QStringLiteral(";;") + MediaFilters::audioFilter());
        if (f.isEmpty()) return;
        FileCommunicator::setDefaultDir(QFileInfo(f).path());
        mfile_->setVideoPath(f);
        updateFiles();
    });
    connect(aBrowse_, &QPushButton::clicked, this, [this]() {
        if (!mfile_) return;
        const QString cur = mfile_->getAudioFile() ? mfile_->getAudioFile()->getPath() : QString();
        const QString f = chooseFile(cur, MediaFilters::audioFilter());
        if (!f.isEmpty()) {
            mfile_->setAudioPath(f);
            FileCommunicator::setDefaultDir(QFileInfo(f).path());
        }
        updateFiles();   // even on cancel: the checkbox follows the media state
    });
    connect(externalAudio_, &QCheckBox::clicked, this, [this](bool checked) {
        if (!mfile_) return;
        if (checked)
            aBrowse_->click();
        else {
            mfile_->setAudioFileUnused();
            updateFiles();
        }
    });
}

QString MediaSelector::chooseFile(const QString &current, const QString &filter) {
    QString dir = FileCommunicator::getDefaultDirPath();
    QString start = dir;
    if (!current.isEmpty()) {
        const QFileInfo fi(current);
        if (fi.dir().exists()) dir = fi.dir().path();
        start = dir + QLatin1Char('/') + fi.fileName();
    }
    return QFileDialog::getOpenFileName(this, __("Select media"), start, filter);
}

void MediaSelector::setMediaFile(AppMediaFile *mfile) {
    mfile_ = mfile;
    updateFiles();
}

void MediaSelector::updateFiles() {
    const VideoFile *v = mfile_ ? mfile_->getVideoFile() : nullptr;
    const AudioFile *a = mfile_ ? mfile_->getAudioFile() : nullptr;
    vfName_->setText(v && v->exists() ? SystemDependent::displayPath(v->getPath()) : QString());
    const bool external = a && !a->isSameAsVideo();
    if (!external) {
        afName_->clear();
        afName_->setEnabled(false);
        externalAudio_->setChecked(false);
        aBrowse_->setEnabled(false);
    } else {
        afName_->setText(QFileInfo::exists(a->getPath()) ? SystemDependent::displayPath(a->getPath()) : QString());
        afName_->setEnabled(true);
        externalAudio_->setChecked(true);
        aBrowse_->setEnabled(true);
    }
    emit selectionChanged();
}

bool MediaSelector::isValidSelection() const {
    const VideoFile *v = mfile_ ? mfile_->getVideoFile() : nullptr;
    const AudioFile *a = mfile_ ? mfile_->getAudioFile() : nullptr;
    if (!v || !v->exists()) return false;
    return !a || a->isSameAsVideo() || QFileInfo::exists(a->getPath());
}

void MediaSelector::setSelectorEnabled(bool enabled) {
    vBrowse_->setEnabled(enabled);
    externalAudio_->setEnabled(enabled);
    const AudioFile *a = mfile_ ? mfile_->getAudioFile() : nullptr;
    aBrowse_->setEnabled(enabled && a && !a->isSameAsVideo());
}

// ---- AppMediaFile -------------------------------------------------------------------------------

AppMediaFile::AppMediaFile() = default;

AppMediaFile::AppMediaFile(const AppMediaFile &other) : MediaFile(other) {}

AppMediaFile::~AppMediaFile() {
    disconnectListener();
}

MediaSelector *AppMediaFile::selector() {
    if (!selector_) {
        selector_ = std::make_unique<MediaSelector>();
        selector_->setMediaFile(this);
    }
    return selector_.get();
}

void AppMediaFile::setSelectorEnabled(bool enabled) {
    selector()->setSelectorEnabled(enabled);
}

std::shared_ptr<VideoFile> AppMediaFile::guessVideo(const Subtitles *subs) {
    QString base = subs ? subs->getSubFile().getStrippedFile() : QString();
    if (base.isEmpty()) base = FileCommunicator::getDefaultDirPath() + QLatin1Char('/') + __("Untitled");
    const QString placeholder = base + MediaFilters::videoExtensions().first();
    const QFileInfo bi(base);
    const QDir dir = bi.dir();
    if (!dir.exists()) return std::make_shared<VideoFile>(placeholder);
    // Longest common prefix of the lower-case names; first wins on ties.
    const QString wanted = bi.fileName().toLower();
    // Every video file of the folder is a candidate (the Java compared full
    // paths, so the directory always matched); the longest common name
    // prefix wins, the first candidate on ties.
    QString best;
    int matchcount = -1;
    for (const QFileInfo &fi : dir.entryInfoList(QDir::Files | QDir::Hidden, QDir::Name)) {
        if (!MediaFilters::isVideo(fi.fileName())) continue;
        const QString name = fi.fileName().toLower();
        int j = 0;
        while (j < wanted.length() && j < name.length() && wanted[j] == name[j]) ++j;
        if (j > matchcount) {
            matchcount = j;
            best = fi.filePath();
        }
    }
    auto v = std::make_shared<VideoFile>(best.isEmpty() ? placeholder : best);
    MediaProbe::start(v);
    return v;
}

void AppMediaFile::guessMediaFiles(const Subtitles *subs) {
    if (!video_ || !video_->exists()) {
        video_ = guessVideo(subs);
        if (!audio_ || !QFileInfo::exists(audio_->getPath())) setAudioFileUnused();
    }
    selector()->setMediaFile(this);
}

bool AppMediaFile::validateMediaFile(const Subtitles *subs, bool forceNew, QWidget *parent) {
    if (!forceNew && video_ && video_->exists()) {
        // A probe started a moment ago ("New from video") may still be running.
        MediaProbe::await(video_.get(), 10000, parent);
        return true;
    }
    auto oldVideo = video_;
    auto oldAudio = audio_;
    guessMediaFiles(subs);
    while (true) {
        QDialog dlg(parent);
        dlg.setWindowTitle(__("Select media"));
        auto *lay = new QVBoxLayout(&dlg);
        MediaSelector *sel = selector();
        sel->setParent(&dlg);
        lay->addWidget(sel);
        auto *buttons = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel, &dlg);
        lay->addWidget(buttons);
        QObject::connect(buttons, &QDialogButtonBox::accepted, &dlg, &QDialog::accept);
        QObject::connect(buttons, &QDialogButtonBox::rejected, &dlg, &QDialog::reject);
        auto gate = [sel, buttons]() { buttons->button(QDialogButtonBox::Ok)->setEnabled(sel->isValidSelection()); };
        QObject::connect(sel, &MediaSelector::selectionChanged, &dlg, gate);
        sel->show();
        gate();
        const int r = dlg.exec();
        lay->removeWidget(sel);
        sel->setParent(nullptr);
        if (r != QDialog::Accepted) {
            video_ = oldVideo;
            audio_ = oldAudio;
            audioChanged();
            return false;
        }
        if (video_ && video_->exists()) break;
        QMessageBox::warning(parent, __("Error in videofile selection"), __("This file does not exist.\nPlease provide a valid file name."));
    }
    // The FPS chooser and the ASS/QuickTime writers read the probed info right after.
    MediaProbe::await(video_.get(), 10000, parent);
    return true;
}

bool AppMediaFile::equals(const AppMediaFile &o) const {
    auto same = [](const QString &a, const QString &b) { return a == b; };
    return same(video_ ? video_->getPath() : QString(), o.video_ ? o.video_->getPath() : QString()) &&
           same(audio_ ? audio_->getPath() : QString(), o.audio_ ? o.audio_->getPath() : QString());
}

void AppMediaFile::setVideoPath(const QString &path) {
    if (path.isEmpty() || !QFileInfo::exists(path)) return;
    video_ = std::make_shared<VideoFile>(path);
    MediaProbe::start(video_);
    if (!audio_ || audio_->isSameAsVideo()) setAudioFileUnused();
}

void AppMediaFile::setAudioPath(const QString &path) {
    if (path.isEmpty() || !QFileInfo::exists(path)) return;
    audio_ = std::make_shared<AudioFile>(path, video_ ? *video_ : VideoFile());
    audioChanged();
}

void AppMediaFile::setAudioFileUnused() {
    if (!video_) return;
    audio_ = std::make_shared<AudioFile>(video_->getPath(), *video_);
    audioChanged();
}

void AppMediaFile::setNewVideoFile(const QString &path) {
    if (path.isEmpty() || !QFileInfo::exists(path)) return;
    video_ = std::make_shared<VideoFile>(path);
    MediaProbe::start(video_);
    setAudioFileUnused();
}

// Other peaks now belong to this media: the old ones are released (and
// their analysis stops if nobody else uses them). The same file changed on
// disk is another media.
void AppMediaFile::audioChanged() {
    if (peaks_ && audio_ && peaks_->identity() == MediaCache::identity(audio_->getPath())) return;
    disconnectListener();
    peaks_.reset();
}

void AppMediaFile::disconnectListener() {
    for (const auto &c : listenerConnections_) QObject::disconnect(c);
    listenerConnections_.clear();
    // Whoever was watching this analysis hears no more of it: told now, so a
    // progress row of an audio that is gone does not stay on the screen.
    if (listener_) listener_->stopPeaks();
    listener_ = nullptr;
}

bool AppMediaFile::startPeaks(AudioPeaksListener *listener) {
    disconnectListener();
    if (!audio_) return false;
    // Kept since the preview was last on, but the file has changed since.
    if (peaks_ && peaks_->identity() != MediaCache::identity(audio_->getPath())) peaks_.reset();
    if (!peaks_) peaks_ = AudioPeaks::forFile(audio_->getPath());
    listener_ = listener;
    if (listener) {
        AudioPeaks *p = peaks_.get();
        QObject *ctx = listener->peaksContext();   // a destroyed panel receives nothing more
        listenerConnections_.append(QObject::connect(p, &AudioPeaks::started, ctx, [listener]() { listener->startPeaks(); }, Qt::QueuedConnection));
        listenerConnections_.append(QObject::connect(p, &AudioPeaks::progress, ctx, [listener](float v) { listener->updatePeaks(v); }, Qt::QueuedConnection));
        listenerConnections_.append(QObject::connect(p, &AudioPeaks::finished, ctx, [listener](bool) { listener->stopPeaks(); }, Qt::QueuedConnection));
        // Joining an analysis another window started: show its progress row too.
        if (p->isRunning()) listener->startPeaks();
    }
    return peaks_->start();
}

void AppMediaFile::stopPeaks() {
    disconnectListener();
    if (peaks_ && !peaks_->isReady()) peaks_.reset();
}

// Another window showing the same media keeps its analysis: this one only
// stops following it.
void AppMediaFile::cancelPeaks() {
    if (!peaks_) return;
    if (peaks_.use_count() > 1) {
        AudioPeaksListener *l = listener_;
        disconnectListener();
        peaks_.reset();
        if (l) l->stopPeaks();
        return;
    }
    peaks_->cancel();
}

std::unique_ptr<AudioPreviewData> AppMediaFile::getAudioPreview(double from, double to) const {
    return peaks_ ? peaks_->getAudioPreview(from, to) : nullptr;
}

QVector<double> AppMediaFile::keyframes(double from, double to) const {
    return peaks_ ? peaks_->keyframes(from, to) : QVector<double>();
}

