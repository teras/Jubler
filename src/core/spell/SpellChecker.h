/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QString>
#include <QStringList>
#include <memory>
#include <stdexcept>

// One misspelling: offset in the checked text, the word and suggestions.
struct SpellError {
    int position = 0;
    QString original;
    QStringList alternatives;
};

struct SpellLanguage {
    QString code, name;
    bool builtin = false;
    bool operator==(const SpellLanguage &o) const { return code == o.code; }
};

// Progress of a dictionary download; `isCancelled` is polled between chunks.
class DownloadProgress {
public:
    virtual ~DownloadProgress() = default;
    virtual void onProgress(int percent, qint64 downloaded, qint64 total) = 0;
    virtual bool isCancelled() const = 0;
};

class SpellException : public std::runtime_error {
public:
    explicit SpellException(const QString &msg) : std::runtime_error(msg.toStdString()), msg_(msg) {}
    const QString &message() const { return msg_; }

private:
    QString msg_;
};

// The spell-checker contract, port of `SpellChecker` (+ the `ExtProgram`
// naming). Only Hunspell implements it in this build.
class SpellChecker {
public:
    virtual ~SpellChecker() = default;
    virtual QString getName() const = 0;
    virtual QString getDescriptiveName() const { return getName(); }
    virtual void start() = 0;   // throws SpellException
    virtual QList<SpellError> checkSpelling(const QString &text) = 0;
    virtual void stop() = 0;
    virtual bool supportsInsert() const { return false; }
    virtual bool insertWord(const QString &) { return false; }
    virtual QList<SpellLanguage> getInstalledLanguages() const { return {}; }
    virtual std::optional<SpellLanguage> getActiveLanguage() const { return std::nullopt; }
    virtual void setActiveLanguage(const SpellLanguage &) {}
    virtual bool supportsDownload() const { return false; }
    virtual QList<SpellLanguage> getDownloadableLanguages() const { return {}; }
    virtual void downloadLanguage(const SpellLanguage &, DownloadProgress &) {}   // throws SpellException
    virtual bool canRemove(const SpellLanguage &) const { return false; }
    virtual bool removeLanguage(const SpellLanguage &) { return false; }
};

// The registered spell checkers, in registration order.
namespace AvailSpellCheckers {
QList<std::shared_ptr<SpellChecker>> &all();
void registerBuiltin();
}  // namespace AvailSpellCheckers
