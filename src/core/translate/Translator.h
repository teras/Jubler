/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QPair>
#include <QString>
#include <functional>
#include <memory>

#include "core/subs/Subtitles.h"

class QWidget;

// A translation language; a null id means "auto detect".
struct Language {
    QString id, displayName;
    bool isAuto() const { return id.isNull(); }
};

// Progress/cancel hooks of a long translation (the app shows a dialog).
class TranslateProgress {
public:
    virtual ~TranslateProgress() = default;
    virtual void setValues(int size, const QString &label) = 0;
    virtual void updateProgress(int done) = 0;
    virtual bool isCancelled() const = 0;
};

// The translator contract, port of `Translator`. GUI parts (configuration
// dialog, PIN prompt) are injected by the app.
class Translator {
public:
    virtual ~Translator() = default;
    virtual QList<Language> getSourceLanguages() const = 0;
    virtual QList<Language> getDestinationLanguagesFor(const Language &from) const = 0;
    virtual Language getDefaultSourceLanguage() const = 0;
    virtual Language getDefaultDestinationLanguage() const = 0;
    virtual QString getDefinition() const = 0;
    // Translate in place; false on failure (the error is in lastError()).
    virtual bool translate(const QList<SubEntryPtr> &subs, const Language &from, const Language &to, TranslateProgress *progress) = 0;
    // Null when ready, otherwise the (translated) reason.
    virtual QString isReady() = 0;
    // The "Configure" button of translators configured by themselves
    // (plugins); the built-in ones are configured by the application.
    virtual void configure(QWidget *parent) { Q_UNUSED(parent); }
    const QString &lastError() const { return lastError_; }

protected:
    QString lastError_;
};

// HTTP translator with batching. Port of `WebTranslator`.
class WebTranslator : public Translator {
public:
    bool translate(const QList<SubEntryPtr> &subs, const Language &from, const Language &to, TranslateProgress *progress) override;
    void setBlockSize(int n) { blockSize_ = n; }
    void setTimeouts(int connectMs, int readMs) { connectTimeout_ = connectMs; readTimeout_ = readMs; }

protected:
    virtual QString getTranslationURL(const Language &from, const Language &to) const = 0;
    virtual QList<QPair<QString, QString>> getRequestProperties() const = 0;
    virtual bool isPost() const { return true; }
    virtual QString getBody(const QList<SubEntryPtr> &group, const Language &from, const Language &to) const = 0;
    // Null on success, else the error.
    virtual QString parseResults(const QList<SubEntryPtr> &group, const QString &response) = 0;
    // One block; null on success.
    QString translatePart(const QList<SubEntryPtr> &group, const Language &from, const Language &to);

    int blockSize_ = 200;
    int connectTimeout_ = 10000, readTimeout_ = 15000;
};

// The fixed language table shared by the simple web translators. Port of
// `SimpleWebTranslator`.
class SimpleWebTranslator : public WebTranslator {
public:
    SimpleWebTranslator() { setBlockSize(100); }
    QList<Language> getSourceLanguages() const override;
    QList<Language> getDestinationLanguagesFor(const Language &from) const override;
    Language getDefaultSourceLanguage() const override;
    Language getDefaultDestinationLanguage() const override;
};

// Azure Translator (REST/JSON). Port of `AzureJSONTranslator`; the
// subscription key is stored AES-encrypted with a PIN held in memory only.
class AzureTranslator : public SimpleWebTranslator {
public:
    AzureTranslator();
    QString getDefinition() const override;
    QString isReady() override;
    // Prompt for the PIN (empty = cancelled); set by the app.
    void setPinPrompt(std::function<QString()> prompt) { pinPrompt_ = std::move(prompt); }
    // Legacy plain key present? (the app warns and clears it)
    bool hasLegacyKey() const;
    void clearLegacyKey();
    // Configuration (from the dialog): null `key` keeps the stored one.
    QString baseUrl() const;
    QString region() const;
    bool hasStoredKey() const;
    QString saveConfiguration(const QString &baseUrl, const QString &typedKey, const QString &region);   // null on success
    const QString &pin() const { return pin_; }
    void setPin(const QString &p) { pin_ = p; }

protected:
    QString getTranslationURL(const Language &from, const Language &to) const override;
    QList<QPair<QString, QString>> getRequestProperties() const override;
    QString getBody(const QList<SubEntryPtr> &group, const Language &from, const Language &to) const override;
    QString parseResults(const QList<SubEntryPtr> &group, const QString &response) override;

private:
    QString decryptedKey() const;
    QString pin_;
    std::function<QString()> pinPrompt_;
};

namespace AvailTranslators {
QList<std::shared_ptr<Translator>> &all();
void registerBuiltin();
}  // namespace AvailTranslators
