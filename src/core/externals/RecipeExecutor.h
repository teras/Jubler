/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QMap>
#include <QObject>
#include <QProcess>
#include <QString>
#include <QStringList>
#include <atomic>
#include <functional>
#include <memory>
#include <thread>
#include <optional>

#include "core/externals/Recipe.h"
#include "core/subs/Subtitles.h"

// Everything a run needs, gathered by the run dialog.
struct RecipeRun {
    Recipe recipe;
    QMap<QString, QString> values;                  // param key → value (secrets decrypted)
    std::optional<QList<SubEntryPtr>> scope;        // nullopt = the whole document
    QMap<QString, const Subtitles *> windows;       // WINDOW params → the chosen document
    QString audioPath, videoPath;                   // %a %v (empty when unavailable)
    // %w: writes the audio of the media as a 16 kHz mono 16-bit WAV to `dst`
    // (run on a worker thread; `progress` 0..1 or -1). Null when no media.
    std::function<bool(const QString &dst, const std::atomic<bool> &cancel, const std::function<void(float)> &progress, QString *error)> makeWav;
    const Subtitles *current = nullptr;
    bool replaceInCurrent = true;
};

// Runs a recipe: serialises the inputs into a temporary folder, builds the
// argument list, launches the process with live log capture and
// cancellation, and hands the parsed output back. Port of `RecipeExecutor`;
// the apply-back (undo, table refresh, new window) is done by the caller.
class RecipeExecutor : public QObject {
    Q_OBJECT
public:
    explicit RecipeExecutor(QObject *parent = nullptr);
    ~RecipeExecutor() override;

    void start(const RecipeRun &run);
    void cancel();
    bool isRunning() const { return extracting_ || (process_ && process_->state() != QProcess::NotRunning); }

    // The command line for the template with the given substitutions
    // (public for the tests).
    static QStringList buildCommandLine(const QString &templ, const Recipe &recipe, const QMap<QString, QString> &values,
                                        const QMap<QChar, QString> &singles);
    static QStringList tokenize(const QString &text);
    // Copy times/text from `result` into `scope` by index; null on success.
    static QString applyPatch(const Subtitles &result, const QList<SubEntryPtr> &scope, OutputMode mode);
    // The subtitle file a folder-mode tool left in `dir`, or null.
    static QString pickOutputFile(const QString &dir, const QString &wireExtension);

signals:
    void log(const QString &line);
    // The parsed output (REPLACE: a new document; PATCH: apply with applyPatch).
    void resultReady(std::shared_ptr<Subtitles> result, const RecipeRun &run);
    void finished(bool success, const QString &message);

private:
    void fail(const QString &message);
    void onProcessFinished(int exitCode, QProcess::ExitStatus status);
    void cleanup();
    void launch(const QMap<QString, QString> &values, const QMap<QChar, QString> &singles);

    RecipeRun run_;
    // The extraction worker is detached (never joined on the GUI thread, a
    // stale network mount can block it); it shares only this flag.
    std::shared_ptr<std::atomic<bool>> wavCancel_;
    bool extracting_ = false;
    QString tempDir_, outputPath_;
    bool outputIsFolder_ = false;
    bool cancelled_ = false;
    std::unique_ptr<QProcess> process_;
    QByteArray pending_;
};
