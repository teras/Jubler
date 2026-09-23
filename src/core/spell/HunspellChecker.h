/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <memory>

#include "core/spell/SpellChecker.h"

class Hunspell;

// The Hunspell dictionaries: a bundled English plus downloads from the
// wooorm/dictionaries repository into <appdata>/hunspelldicts. Port of
// `HunspellDictManager`.
namespace HunspellDicts {
const QString &builtinCode();       // "en"
QString directory();
QString dicPath(const QString &code);
QString affPath(const QString &code);
// Extract the bundled English if missing; throws SpellException.
void ensureBuiltinEnglish();
QString displayName(const QString &code);
QList<SpellLanguage> installedLanguages();
QList<SpellLanguage> downloadableLanguages();
void download(const QString &code, DownloadProgress &progress);   // throws SpellException
bool remove(const QString &code);
// Text-only helper for the tests: the words of `text` with their offsets
// (tags skipped, apostrophes kept inside words).
QList<std::pair<int, QString>> tokenize(const QString &text);
}  // namespace HunspellDicts

// Hunspell-backed checker, port of the `Hunspell` plugin.
class HunspellChecker : public SpellChecker {
public:
    HunspellChecker();
    ~HunspellChecker() override;
    QString getName() const override { return QStringLiteral("Hunspell"); }
    void start() override;
    QList<SpellError> checkSpelling(const QString &text) override;
    void stop() override;
    bool supportsInsert() const override { return true; }
    bool insertWord(const QString &word) override;
    QList<SpellLanguage> getInstalledLanguages() const override { return HunspellDicts::installedLanguages(); }
    std::optional<SpellLanguage> getActiveLanguage() const override;
    void setActiveLanguage(const SpellLanguage &lang) override;
    bool supportsDownload() const override { return true; }
    QList<SpellLanguage> getDownloadableLanguages() const override { return HunspellDicts::downloadableLanguages(); }
    void downloadLanguage(const SpellLanguage &lang, DownloadProgress &progress) override { HunspellDicts::download(lang.code, progress); }
    bool canRemove(const SpellLanguage &lang) const override { return !lang.builtin; }
    bool removeLanguage(const SpellLanguage &lang) override { return HunspellDicts::remove(lang.code); }

private:
    std::unique_ptr<Hunspell> engine_;
    QByteArray encoding_;
};
