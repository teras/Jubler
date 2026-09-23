/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <memory>

#include "jubler/SubDownloadApi.h"

// The host side of the provider API: blocking HTTP (per-thread network
// manager, 15 s connect / 30 s read, 8 MiB cap), payload extraction, the
// OSDb hash, PIN-protected secrets, translation and logging. Port of
// `Http`, `Secrets` and the helpers the Java providers shared.
class JublerHostServices : public jubler::HostServices {
public:
    static JublerHostServices &instance();
    jubler::HttpResponse get(const QString &url, const QMap<QString, QString> &headers, bool followRedirects, std::shared_ptr<jubler::CancelToken> *token) override;
    jubler::HttpResponse post(const QString &url, const QMap<QString, QString> &headers, const QByteArray &body, bool followRedirects, std::shared_ptr<jubler::CancelToken> *token) override;
    QString userAgent() const override;
    QString browserUserAgent() const override;
    jubler::ProviderResult<QByteArray> extractSubtitleBytes(const QByteArray &payload) override;
    std::optional<QString> movieHash(const QString &path) override;
    bool isSecretStored(const QString &prefKey) override;
    bool storeSecret(QWidget *parent, const QString &prefKey, const QString &value) override;
    QString loadSecret(QWidget *parent, const QString &prefKey) override;
    QString tr(const char *english) override;
    void debug(const QString &message) override;
    ParsedQuery parseQuery(const QString &query) override;
    QString askApiKey(QWidget *parent, const QString &title, const QString &html, const QString &label, bool keyStored) override;
    QString setting(const QString &key, const QString &defaultValue) override;
    void setSetting(const QString &key, const QString &value) override;

private:
    jubler::HttpResponse request(const QString &url, const QMap<QString, QString> &headers, const QByteArray *body, bool followRedirects, std::shared_ptr<jubler::CancelToken> *token);
    QString sessionPin(QWidget *parent);
    QString pin_;
};
