/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <QStringList>
#include <functional>

class Subtitles;

// Periodic autosave of unsaved documents into <appdata>/autosave, port of
// `AutoSaver`. The app registers a provider of the documents to save.
namespace AutoSaver {
extern const QString AUTOSAVEPREFIX;   // "autosave."
constexpr int AUTOSAVE_SECONDS = 30;

struct Candidate {
    const Subtitles *subs;
    QString fileName;   // the document's file name, with its extension
};
using Provider = std::function<QList<Candidate>()>;

// Start the timer (idempotent); `provider` yields the unsaved, non-empty
// documents at each tick.
void launch(Provider provider);
// One autosave pass (also used by the timer).
void saveNow();
// Leftover autosave files from a previous run, to reopen as unsaved documents.
QStringList getAutoSaveListOnLoad();
// A leftover that could not be reopened: moved to the "unrecovered" folder
// (never rotated or cleaned up); returns its new path.
QString keepUnrecovered(const QString &path);
// Remove the autosave files. Called on a confirmed quit for the documents
// that were saved or discarded.
void cleanup();
QString directory();
}  // namespace AutoSaver
