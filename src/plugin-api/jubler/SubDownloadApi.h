/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

// The public API of Jubler's subtitle-download providers. An out-of-tree
// provider is a Qt plugin (QPluginLoader) implementing `SubtitleProviderPlugin`
// and compiled against this header only; everything it needs from the host
// (HTTP, archive extraction, secrets, translation, logging) comes through
// `HostServices`, so the plugin depends on Qt and nothing else of Jubler.
//
// No C++ exception crosses between Jubler and a plugin: failures travel as
// values (`ProviderError`, `ProviderResult`, `HttpResponse::error`). Each
// module has its own copy of an inline exception class's type info, so a
// throw from one module is not reliably caught by type in another (macOS
// with hidden visibility in particular).
//
// Plugins are discovered in the per-user plugins directory
// (Linux ~/.local/share/jubler/plugins, macOS ~/Library/Application Support/
// Jubler/plugins, Windows %APPDATA%\Jubler\plugins) and enabled in
// Preferences ▸ Plugins.

#include <QByteArray>
#include <QList>
#include <QMap>
#include <QString>
#include <QtPlugin>
#include <memory>
#include <optional>
#include <stdexcept>
#include <utility>

class QWidget;

namespace jubler {

// One search hit.
struct Candidate {
    QString provider, releaseName, language, downloads, rating;
    QString handle;     // provider-specific (file id, url, …)
    QString fileHint;   // preferred file name, may be empty
};

// A subtitle payload as downloaded.
struct DownloadData {
    QByteArray bytes;
    QString contentType;
};

struct SearchRequest {
    QString query;
    QString languageCode;   // ISO 639-1, or "" for any
    bool useHash = false;
    QString videoPath;      // may be empty
};

// A user-facing provider failure; NONE is no failure.
struct ProviderError {
    enum class Kind { NONE, NETWORK, AUTH, QUOTA, PARSE };
    Kind kind = Kind::NONE;
    QString message;
    explicit operator bool() const { return kind != Kind::NONE; }
};

// A value or the failure that replaced it.
template <typename T>
struct ProviderResult {
    T value;
    ProviderError error;
    ProviderResult() = default;
    ProviderResult(T v) : value(std::move(v)) {}
    ProviderResult(ProviderError e) : error(std::move(e)) {}
    bool ok() const { return !error; }
};

// A convenience for code inside ONE module: throw it anywhere in a provider
// and let `guarded()` turn it into the returned result. Never let it (or any
// exception) leave a call Jubler makes into the plugin.
class ProviderException : public std::runtime_error {
public:
    using Kind = ProviderError::Kind;
    ProviderException(Kind kind, const QString &message) : std::runtime_error(message.toStdString()), kind_(kind), message_(message) {}
    explicit ProviderException(const ProviderError &error) : ProviderException(error.kind, error.message) {}
    Kind kind() const { return kind_; }
    const QString &message() const { return message_; }

private:
    Kind kind_;
    QString message_;
};

// Runs `body` and returns its value, or the failure it threw. Inline, so the
// throw and the catch both live in the calling module.
template <typename F>
auto guarded(F &&body) -> ProviderResult<decltype(body())> {
    try {
        return body();
    } catch (const ProviderException &e) {
        return ProviderError{e.kind(), e.message()};
    } catch (const std::exception &e) {
        return ProviderError{ProviderError::Kind::NETWORK, QString::fromUtf8(e.what())};
    } catch (...) {
        return ProviderError{ProviderError::Kind::NETWORK, QStringLiteral("Unknown error")};
    }
}

struct HttpResponse {
    ProviderError error;   // a transport failure (no usable response); HTTP error statuses are in `code`
    int code = 0;
    QByteArray body;
    QString contentType;
    QMap<QString, QStringList> headers;   // lower-cased names
    QString text() const { return QString::fromUtf8(body); }
    QString header(const QString &name) const {
        const QStringList v = headers.value(name.toLower());
        return v.isEmpty() ? QString() : v.first();
    }
};

// Cancellation handle of an in-flight request (set by the host while a
// request runs; `cancel()` aborts it).
class CancelToken {
public:
    virtual ~CancelToken() = default;
    virtual void cancel() = 0;
};

// Everything the host offers to a provider.
class HostServices {
public:
    virtual ~HostServices() = default;
    // Blocking HTTP with the host's timeouts and size cap (8 MiB). A
    // transport error (also a cancel) is in `error` (NETWORK); status codes
    // are returned. `token` (optional) receives a handle to abort the request; the
    // host sets it with std::atomic_store, so read it (from another thread, to
    // cancel) with std::atomic_load.
    virtual HttpResponse get(const QString &url, const QMap<QString, QString> &headers, bool followRedirects, std::shared_ptr<CancelToken> *token = nullptr) = 0;
    virtual HttpResponse post(const QString &url, const QMap<QString, QString> &headers, const QByteArray &body, bool followRedirects, std::shared_ptr<CancelToken> *token = nullptr) = 0;
    // The default user agent ("Jubler v<version>") and a browser-like one.
    virtual QString userAgent() const = 0;
    virtual QString browserUserAgent() const = 0;
    // A subtitle out of a raw payload (zip/gzip/plain); PARSE on failure.
    virtual ProviderResult<QByteArray> extractSubtitleBytes(const QByteArray &payload) = 0;
    // The OSDb hash of a video ("%016x"), or nullopt when the file is too
    // small or unreadable.
    virtual std::optional<QString> movieHash(const QString &path) = 0;
    // Encrypted per-provider secrets under a preference key (session PIN).
    virtual bool isSecretStored(const QString &prefKey) = 0;
    virtual bool storeSecret(QWidget *parent, const QString &prefKey, const QString &value) = 0;
    virtual QString loadSecret(QWidget *parent, const QString &prefKey) = 0;   // "" when unavailable
    // Translation of a UI string and the diagnostic log.
    virtual QString tr(const char *english) = 0;
    virtual void debug(const QString &message) = 0;
    // Parsed query: title plus optional season/episode (see QueryParse).
    struct ParsedQuery {
        QString title;
        std::optional<int> season, episode;
        bool hasSeason() const { return season.has_value(); }
        bool hasEpisode() const { return season.has_value() && episode.has_value(); }
    };
    virtual ParsedQuery parseQuery(const QString &query) = 0;
    // "Configure <name>" dialog helper: html intro, label and a masked key
    // field; returns the typed key (null when unchanged/cancelled).
    virtual QString askApiKey(QWidget *parent, const QString &title, const QString &html, const QString &label, bool keyStored) = 0;
    // Plain settings of a plugin, stored with the Jubler
    // preferences under "plugins.data.<key>"; use your plugin id as a prefix.
    virtual QString setting(const QString &key, const QString &defaultValue) = 0;
    virtual void setSetting(const QString &key, const QString &value) = 0;
};

class SubtitleProvider {
public:
    enum class HashSupport { TEXT_ONLY, HASH_OPTIONAL };
    virtual ~SubtitleProvider() = default;
    virtual QString getName() const = 0;
    virtual int priority() const { return 0; }   // menu order, lower first
    virtual HashSupport hashSupport() const { return HashSupport::TEXT_ONLY; }
    virtual bool needsConfiguration() const { return true; }
    // Null when ready, otherwise the reason (informational).
    virtual QString isReady() = 0;
    // GUI thread: may prompt (PIN) and cache the key; null or the reason.
    virtual QString ensureReady(QWidget *parent) = 0;
    virtual void configure(QWidget *parent) = 0;
    // Worker thread: metadata only, must never spend quota. Must not throw.
    virtual ProviderResult<QList<Candidate>> search(const SearchRequest &request) = 0;
    // Worker thread: the only quota-spending call. Must not throw.
    virtual ProviderResult<DownloadData> download(const Candidate &candidate) = 0;
    virtual void cancelSearch() = 0;
};

// The plugin entry point.
class SubtitleProviderPlugin {
public:
    virtual ~SubtitleProviderPlugin() = default;
    virtual QList<std::shared_ptr<SubtitleProvider>> createProviders(HostServices *host) = 0;
};

}  // namespace jubler

#define JublerSubtitleProviderPlugin_iid "org.jubler.SubtitleProviderPlugin/2.0"
Q_DECLARE_INTERFACE(jubler::SubtitleProviderPlugin, JublerSubtitleProviderPlugin_iid)
