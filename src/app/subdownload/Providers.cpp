/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/subdownload/Providers.h"

#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QLocale>
#include <QMap>
#include <QRegularExpression>
#include <QUrl>
#include <algorithm>
#include <memory>

#include "app/subdownload/HostServices.h"
#include "core/i18n/I18N.h"

using jubler::Candidate;
using jubler::DownloadData;
using jubler::HostServices;
using jubler::HttpResponse;
using jubler::ProviderException;
using jubler::SearchRequest;
using jubler::SubtitleProvider;
using Kind = jubler::ProviderError::Kind;

namespace {

QString encode(const QString &s) { return QString::fromLatin1(QUrl::toPercentEncoding(s)); }

QString jsonMessage(const QString &body) {
    const QJsonDocument doc = QJsonDocument::fromJson(body.toUtf8());
    return doc.isObject() ? doc.object().value(QStringLiteral("message")).toString() : QString();
}

// Shared shape of the three keyed providers: an encrypted key under a
// preference, cached in memory once unlocked.
class KeyedProvider : public SubtitleProvider {
public:
    KeyedProvider(HostServices *host, QString prefKey) : host_(host), prefKey_(std::move(prefKey)) {}
    QString isReady() override {
        if (!apiKey_.isEmpty() || host_->isSecretStored(prefKey_)) return QString();
        return host_->tr(notSetMessage());
    }
    QString ensureReady(QWidget *parent) override {
        const QString r = isReady();
        if (!r.isNull()) return r;
        if (apiKey_.isEmpty()) {
            apiKey_ = host_->loadSecret(parent, prefKey_);
            if (apiKey_.isEmpty()) return host_->tr(unlockFailedMessage());
        }
        return QString();
    }
    void configure(QWidget *parent) override {
        const QString typed = host_->askApiKey(parent, host_->tr(configureTitle()), configureHtml(), host_->tr("API key"), host_->isSecretStored(prefKey_));
        if (typed.isEmpty()) return;
        if (host_->storeSecret(parent, prefKey_, typed)) apiKey_ = typed;
    }
    void cancelSearch() override {
        if (const auto t = std::atomic_load(&searchToken_)) t->cancel();   // the worker thread stores it
    }
    jubler::ProviderResult<QList<Candidate>> search(const SearchRequest &req) final {
        return jubler::guarded([&] { return find(req); });
    }
    jubler::ProviderResult<DownloadData> download(const Candidate &candidate) final {
        return jubler::guarded([&] { return fetch(candidate); });
    }

protected:
    // The work, free to throw ProviderException; search()/download() return it as a result.
    virtual QList<Candidate> find(const SearchRequest &req) = 0;
    virtual DownloadData fetch(const Candidate &candidate) = 0;
    // The host services with their failures thrown.
    HttpResponse get(const QString &url, const QMap<QString, QString> &headers, bool follow, std::shared_ptr<jubler::CancelToken> *token = nullptr) {
        return checked(host_->get(url, headers, follow, token));
    }
    HttpResponse post(const QString &url, const QMap<QString, QString> &headers, const QByteArray &body, bool follow, std::shared_ptr<jubler::CancelToken> *token = nullptr) {
        return checked(host_->post(url, headers, body, follow, token));
    }
    QByteArray extract(const QByteArray &payload) {
        auto r = host_->extractSubtitleBytes(payload);
        if (r.error) throw ProviderException(r.error);
        return r.value;
    }
    virtual const char *notSetMessage() const = 0;
    virtual const char *unlockFailedMessage() const = 0;
    virtual const char *configureTitle() const = 0;
    virtual QString configureHtml() const = 0;
    HostServices *host_;
    QString prefKey_;
    QString apiKey_;
    std::shared_ptr<jubler::CancelToken> searchToken_;

private:
    static HttpResponse checked(HttpResponse r) {
        if (r.error) throw ProviderException(r.error);
        return r;
    }
};

// ---- OpenSubtitles -------------------------------------------------------------------------------

class OpenSubtitlesProvider : public KeyedProvider {
public:
    explicit OpenSubtitlesProvider(HostServices *host) : KeyedProvider(host, QStringLiteral("subdownload.opensubtitles.enckey")) {}
    QString getName() const override { return QStringLiteral("OpenSubtitles"); }
    int priority() const override { return 20; }
    HashSupport hashSupport() const override { return HashSupport::HASH_OPTIONAL; }

    QList<Candidate> find(const SearchRequest &req) override {
        QMap<QString, QString> params;   // QMap keeps the keys sorted, as the API asks
        if (!req.languageCode.isEmpty()) params.insert(QStringLiteral("languages"), req.languageCode.toLower());
        if (req.useHash) {
            const auto hash = host_->movieHash(req.videoPath);
            if (!hash) {
                QFile video(req.videoPath);
                if (req.videoPath.isEmpty() || !video.open(QIODevice::ReadOnly))
                    throw ProviderException(Kind::NETWORK, host_->tr("Could not read the video file to compute its fingerprint."));
                throw ProviderException(Kind::NETWORK, host_->tr("The video file is too small to fingerprint."));
            }
            params.insert(QStringLiteral("moviehash"), *hash);
        } else {
            const auto ep = host_->parseQuery(req.query);
            params.insert(QStringLiteral("query"), encode(ep.title));
            if (ep.hasSeason()) {
                params.insert(QStringLiteral("season_number"), QString::number(*ep.season));
                if (ep.hasEpisode()) params.insert(QStringLiteral("episode_number"), QString::number(*ep.episode));
                params.insert(QStringLiteral("type"), QStringLiteral("episode"));
            }
        }
        QString url = BASE + QStringLiteral("/subtitles");
        QChar sep = QLatin1Char('?');
        for (auto it = params.constBegin(); it != params.constEnd(); ++it) {
            url += sep + it.key() + QLatin1Char('=') + it.value();
            sep = QLatin1Char('&');
        }
        const HttpResponse resp = get(url, headers(), true, &searchToken_);
        if (resp.code == 401 || resp.code == 403) throw ProviderException(Kind::AUTH, host_->tr("Authentication failed — check your API key."));
        if (resp.code != 200) throw ProviderException(Kind::NETWORK, host_->tr("Search failed (HTTP {0}).").replace(QLatin1String("{0}"), QString::number(resp.code)));
        QList<Candidate> out;
        const QJsonDocument doc = QJsonDocument::fromJson(resp.body);
        if (!doc.isObject() || !doc.object().value(QStringLiteral("data")).isArray()) throw ProviderException(Kind::PARSE, host_->tr("Could not read the search results."));
        for (const QJsonValue &item : doc.object().value(QStringLiteral("data")).toArray()) {
            const QJsonObject attr = item.toObject().value(QStringLiteral("attributes")).toObject();
            const QJsonArray files = attr.value(QStringLiteral("files")).toArray();
            if (files.isEmpty()) continue;
            const QJsonObject file = files.first().toObject();
            const int fileId = file.value(QStringLiteral("file_id")).toInt(0);
            if (fileId == 0) continue;
            Candidate c;
            c.provider = getName();
            c.releaseName = attr.value(QStringLiteral("release")).toString(file.value(QStringLiteral("file_name")).toString(QStringLiteral("?")));
            c.language = attr.value(QStringLiteral("language")).toString();
            c.downloads = QString::number(attr.value(QStringLiteral("download_count")).toInt(0));
            const double rating = attr.value(QStringLiteral("ratings")).toDouble(0);
            // Java String.valueOf(double): shortest form, whole numbers with ".0".
            QString ratingText = QString::number(rating, 'g', QLocale::FloatingPointShortest);
            if (!ratingText.contains(QLatin1Char('.')) && !ratingText.contains(QLatin1Char('e'))) ratingText += QStringLiteral(".0");
            c.rating = rating > 0 ? ratingText : QString(QLatin1String(""));
            c.handle = QString::number(fileId);
            c.fileHint = file.value(QStringLiteral("file_name")).toString();
            out.append(c);
        }
        return out;
    }

    DownloadData fetch(const Candidate &candidate) override {
        QMap<QString, QString> h = headers();
        h.insert(QStringLiteral("Content-Type"), QStringLiteral("application/json"));
        QJsonObject body;
        body.insert(QStringLiteral("file_id"), candidate.handle.toInt());
        const HttpResponse resp = post(BASE + QStringLiteral("/download"), h, QJsonDocument(body).toJson(QJsonDocument::Compact), true);
        if (resp.code == 401 || resp.code == 403) throw ProviderException(Kind::AUTH, host_->tr("Authentication failed — check your API key."));
        if (resp.code == 406 || resp.code == 429) {
            const QString msg = jsonMessage(resp.text());
            if (!msg.isEmpty()) {
                host_->debug(QStringLiteral("OpenSubtitles quota response: ") + msg);
                throw ProviderException(Kind::QUOTA, getName() + QStringLiteral(": ") + msg);
            }
            throw ProviderException(Kind::QUOTA, host_->tr("Daily download quota exhausted."));
        }
        const QJsonDocument doc = QJsonDocument::fromJson(resp.body);
        if (!doc.isObject()) throw ProviderException(Kind::NETWORK, host_->tr("Unexpected download response."));
        const QString link = doc.object().value(QStringLiteral("link")).toString();
        if (resp.code != 200 || link.isEmpty()) {
            const QString msg = doc.object().value(QStringLiteral("message")).toString();
            if (!msg.isEmpty()) host_->debug(QStringLiteral("OpenSubtitles download refused: ") + msg);
            throw ProviderException(Kind::QUOTA, msg.isEmpty() ? host_->tr("Download was refused.") : getName() + QStringLiteral(": ") + msg);
        }
        const HttpResponse file = get(link, headers(), true);
        if (file.code != 200) throw ProviderException(Kind::NETWORK, host_->tr("Fetching the subtitle failed (HTTP {0}).").replace(QLatin1String("{0}"), QString::number(file.code)));
        return {extract(file.body), file.contentType};
    }

protected:
    const char *notSetMessage() const override { return N__("OpenSubtitles API key not set — use Configure."); }
    const char *unlockFailedMessage() const override { return N__("Could not unlock the OpenSubtitles API key (wrong PIN?)."); }
    const char *configureTitle() const override { return N__("Configure OpenSubtitles"); }
    QString configureHtml() const override {
        return host_->tr("Enter your OpenSubtitles.com API key.") + QStringLiteral("<br/>") +
               host_->tr("Register a free account and create an API key at %1").replace(QLatin1String("%1"), QStringLiteral("<a href=\"https://www.opensubtitles.com/en/consumers\">opensubtitles.com</a>"));
    }

private:
    QMap<QString, QString> headers() const {
        return {{QStringLiteral("Api-Key"), apiKey_}, {QStringLiteral("User-Agent"), host_->userAgent()}, {QStringLiteral("Accept"), QStringLiteral("application/json")}};
    }
    const QString BASE = QStringLiteral("https://api.opensubtitles.com/api/v1");
};

// ---- SubDL ------------------------------------------------------------------------------------------

class SubDLProvider : public KeyedProvider {
public:
    explicit SubDLProvider(HostServices *host) : KeyedProvider(host, QStringLiteral("subdownload.subdl.enckey")) {}
    QString getName() const override { return QStringLiteral("SubDL"); }
    int priority() const override { return 30; }

    QList<Candidate> find(const SearchRequest &req) override {
        const auto ep = host_->parseQuery(req.query);
        const QString filmName = ep.hasSeason() && !ep.title.isEmpty() ? ep.title : req.query;
        QString url = BASE + QStringLiteral("?api_key=") + encode(apiKey_) + QStringLiteral("&film_name=") + encode(filmName) + QStringLiteral("&subs_per_page=30");
        if (ep.hasSeason()) {
            url += QStringLiteral("&type=tv&season_number=") + QString::number(*ep.season);
            url += ep.hasEpisode() ? QStringLiteral("&episode_number=") + QString::number(*ep.episode) : QStringLiteral("&full_season=1");
        }
        if (!req.languageCode.isEmpty()) url += QStringLiteral("&languages=") + req.languageCode.toUpper();
        const HttpResponse resp = get(url, {{QStringLiteral("User-Agent"), host_->userAgent()}}, true, &searchToken_);
        if (resp.code == 401 || resp.code == 403) throw ProviderException(Kind::AUTH, host_->tr("Authentication failed — check your API key."));
        const QJsonDocument doc = QJsonDocument::fromJson(resp.body);
        if (!doc.isObject()) throw ProviderException(Kind::PARSE, host_->tr("Could not read the search results."));
        const QJsonObject root = doc.object();
        QList<Candidate> out;
        if (!root.value(QStringLiteral("status")).toBool(false)) {
            const QString error = root.value(QStringLiteral("error")).toString();
            if (!error.isEmpty()) host_->debug(QStringLiteral("SubDL search error: ") + error);
            const QString lower = error.toLower();
            if (lower.contains(QLatin1String("can't find")) || lower.contains(QLatin1String("cant find")) || lower.contains(QLatin1String("find movie or tv"))) return out;
            if (lower.contains(QLatin1String("unsafe")) || lower.contains(QLatin1String("film name"))) throw ProviderException(Kind::NETWORK, host_->tr("SubDL rejected this title — try simplifying it."));
            throw ProviderException(lower.contains(QLatin1String("api")) ? Kind::AUTH : Kind::NETWORK, error.isEmpty() ? host_->tr("Search failed.") : getName() + QStringLiteral(": ") + error);
        }
        for (const QJsonValue &item : root.value(QStringLiteral("subtitles")).toArray()) {
            const QJsonObject sub = item.toObject();
            const QString path = sub.value(QStringLiteral("url")).toString();
            if (path.isEmpty()) continue;
            Candidate c;
            c.provider = getName();
            c.releaseName = sub.value(QStringLiteral("release_name")).toString(QStringLiteral("?"));
            c.language = sub.value(QStringLiteral("language")).toString();
            c.downloads = QString(QLatin1String(""));
            c.rating = QString(QLatin1String(""));
            c.handle = path;
            c.fileHint = QString(QLatin1String(""));
            out.append(c);
        }
        return out;
    }

    DownloadData fetch(const Candidate &candidate) override {
        const QString url = candidate.handle.startsWith(QLatin1String("http")) ? candidate.handle : DOWNLOAD_BASE + candidate.handle;
        const HttpResponse file = get(url, {{QStringLiteral("User-Agent"), host_->userAgent()}}, true);
        if (file.code == 401 || file.code == 403) throw ProviderException(Kind::AUTH, host_->tr("Authentication failed — check your API key."));
        if (file.code != 200) throw ProviderException(Kind::NETWORK, host_->tr("Fetching the subtitle failed (HTTP {0}).").replace(QLatin1String("{0}"), QString::number(file.code)));
        return {extract(file.body), file.contentType};
    }

protected:
    const char *notSetMessage() const override { return N__("SubDL API key not set — use Configure."); }
    const char *unlockFailedMessage() const override { return N__("Could not unlock the SubDL API key (wrong PIN?)."); }
    const char *configureTitle() const override { return N__("Configure SubDL"); }
    QString configureHtml() const override {
        return host_->tr("Enter your SubDL API key.") + QStringLiteral("<br/>") +
               host_->tr("Get a free API key from your account panel at %1").replace(QLatin1String("%1"), QStringLiteral("<a href=\"https://subdl.com/panel/api\">subdl.com/panel/api</a>"));
    }

private:
    const QString BASE = QStringLiteral("https://api.subdl.com/api/v1/subtitles");
    const QString DOWNLOAD_BASE = QStringLiteral("https://dl.subdl.com");
};

// ---- SubSource ----------------------------------------------------------------------------------------

class SubSourceProvider : public KeyedProvider {
public:
    explicit SubSourceProvider(HostServices *host) : KeyedProvider(host, QStringLiteral("subdownload.subsource.enckey")) {}
    QString getName() const override { return QStringLiteral("SubSource"); }
    int priority() const override { return 10; }

    QString isReady() override {
        if (!apiKey_.isEmpty() || host_->isSecretStored(prefKey_)) return QString();
        return host_->tr("SubSource downloads need an API key (searching works without one) — use Configure.");
    }
    QString ensureReady(QWidget *parent) override {
        // Searching is keyless; unlock a stored key opportunistically.
        if (apiKey_.isEmpty() && host_->isSecretStored(prefKey_)) {
            const QString key = host_->loadSecret(parent, prefKey_);
            if (!key.isEmpty()) apiKey_ = key;
        }
        return QString();
    }

    QList<Candidate> find(const SearchRequest &req) override {
        const auto q = host_->parseQuery(req.query);
        QList<Candidate> out;
        const auto title = resolveTitle(q.title.isEmpty() ? req.query : q.title);
        if (!title) return out;
        const bool bySeason = title->second && q.hasSeason();
        QString url = SEARCH_BASE + QStringLiteral("/subtitles/") + title->first + (bySeason ? QStringLiteral("/season-") + QString::number(*q.season) : QString());
        HttpResponse resp = get(url, headers(), true, &searchToken_);
        if (resp.code != 200 && bySeason) resp = get(SEARCH_BASE + QStringLiteral("/subtitles/") + title->first, headers(), true, &searchToken_);
        if (resp.code != 200) throw ProviderException(Kind::NETWORK, host_->tr("Search failed (HTTP {0}).").replace(QLatin1String("{0}"), QString::number(resp.code)));
        const QString token = req.languageCode.isEmpty() ? QString() : langToken(req.languageCode.toLower());
        const bool filterEpisode = title->second && q.hasEpisode();
        const QJsonDocument doc = QJsonDocument::fromJson(resp.body);
        if (!doc.isObject()) throw ProviderException(Kind::PARSE, host_->tr("Could not read the search results."));
        for (const QJsonValue &item : doc.object().value(QStringLiteral("subtitles")).toArray()) {
            const QJsonObject sub = item.toObject();
            const QString language = sub.value(QStringLiteral("language")).toString();
            if (!token.isNull() && !language.toLower().contains(token)) continue;
            const qint64 id = sub.value(QStringLiteral("id")).toVariant().toLongLong();
            if (id == 0) continue;
            const QString releaseInfo = sub.value(QStringLiteral("release_info")).toString(sub.value(QStringLiteral("caption")).toString(QStringLiteral("?")));
            if (filterEpisode && !matchesEpisode(releaseInfo, *q.season, *q.episode)) continue;
            QString rating = sub.value(QStringLiteral("rating")).toVariant().toString();
            if (rating.compare(QLatin1String("unrated"), Qt::CaseInsensitive) == 0 || rating == QLatin1String("0")) rating = QString(QLatin1String(""));
            Candidate c;
            c.provider = getName();
            c.releaseName = htmlUnescape(releaseInfo);
            c.language = QString(language).replace(QLatin1Char('_'), QLatin1Char(' '));
            c.downloads = QString(QLatin1String(""));
            c.rating = rating;
            c.handle = QString::number(id);
            c.fileHint = QString(QLatin1String(""));
            out.append(c);
        }
        return out;
    }

    DownloadData fetch(const Candidate &candidate) override {
        if (apiKey_.isEmpty()) throw ProviderException(Kind::AUTH, host_->tr("Set your SubSource API key to download subtitles — use Configure."));
        const QString url = API_BASE + QStringLiteral("/subtitles/") + encode(candidate.handle) + QStringLiteral("/download?api_key=") + encode(apiKey_);
        QMap<QString, QString> h = headers();
        h.insert(QStringLiteral("X-API-Key"), apiKey_);
        const HttpResponse file = get(url, h, true);
        if (file.code == 400 || file.code == 401 || file.code == 403) {
            const QString msg = jsonMessage(file.text());
            if (!msg.isEmpty()) {
                host_->debug(QStringLiteral("SubSource download refused: ") + msg);
                throw ProviderException(Kind::AUTH, getName() + QStringLiteral(": ") + msg);
            }
            throw ProviderException(Kind::AUTH, host_->tr("Authentication failed — check your API key."));
        }
        if (file.code != 200) throw ProviderException(Kind::NETWORK, host_->tr("Fetching the subtitle failed (HTTP {0}).").replace(QLatin1String("{0}"), QString::number(file.code)));
        return {extract(file.body), file.contentType};
    }

protected:
    const char *notSetMessage() const override { return N__("SubSource downloads need an API key (searching works without one) — use Configure."); }
    const char *unlockFailedMessage() const override { return ""; }
    const char *configureTitle() const override { return N__("Configure SubSource"); }
    QString configureHtml() const override {
        return host_->tr("Enter your SubSource API key.") + QStringLiteral("<br/>") + host_->tr("Searching needs no key; downloads do.") + QStringLiteral("<br/>") +
               host_->tr("Register a free account and create an API key at %1").replace(QLatin1String("%1"), QStringLiteral("<a href=\"https://subsource.net/api-docs\">subsource.net/api-docs</a>"));
    }

private:
    QMap<QString, QString> headers() const { return {{QStringLiteral("User-Agent"), host_->userAgent()}, {QStringLiteral("Accept"), QStringLiteral("application/json")}}; }

    static QString langToken(const QString &code) {
        static const QMap<QString, QString> tokens{
            {QStringLiteral("ar"), QStringLiteral("arabic")}, {QStringLiteral("bg"), QStringLiteral("bulgarian")}, {QStringLiteral("zh"), QStringLiteral("chinese")}, {QStringLiteral("hr"), QStringLiteral("croatian")},
            {QStringLiteral("cs"), QStringLiteral("czech")}, {QStringLiteral("da"), QStringLiteral("danish")}, {QStringLiteral("nl"), QStringLiteral("dutch")}, {QStringLiteral("en"), QStringLiteral("english")},
            {QStringLiteral("et"), QStringLiteral("estonian")}, {QStringLiteral("fi"), QStringLiteral("finnish")}, {QStringLiteral("fr"), QStringLiteral("french")}, {QStringLiteral("de"), QStringLiteral("german")},
            {QStringLiteral("el"), QStringLiteral("greek")}, {QStringLiteral("he"), QStringLiteral("hebrew")}, {QStringLiteral("hi"), QStringLiteral("hindi")}, {QStringLiteral("hu"), QStringLiteral("hungarian")},
            {QStringLiteral("id"), QStringLiteral("indonesian")}, {QStringLiteral("it"), QStringLiteral("italian")}, {QStringLiteral("ja"), QStringLiteral("japanese")}, {QStringLiteral("ko"), QStringLiteral("korean")},
            {QStringLiteral("lv"), QStringLiteral("latvian")}, {QStringLiteral("lt"), QStringLiteral("lithuanian")}, {QStringLiteral("no"), QStringLiteral("norwegian")}, {QStringLiteral("pl"), QStringLiteral("polish")},
            {QStringLiteral("pt"), QStringLiteral("portuguese")}, {QStringLiteral("ro"), QStringLiteral("romanian")}, {QStringLiteral("ru"), QStringLiteral("russian")}, {QStringLiteral("sr"), QStringLiteral("serbian")},
            {QStringLiteral("sk"), QStringLiteral("slovak")}, {QStringLiteral("sl"), QStringLiteral("slovenian")}, {QStringLiteral("es"), QStringLiteral("spanish")}, {QStringLiteral("sv"), QStringLiteral("swedish")},
            {QStringLiteral("th"), QStringLiteral("thai")}, {QStringLiteral("tr"), QStringLiteral("turkish")}, {QStringLiteral("uk"), QStringLiteral("ukrainian")}, {QStringLiteral("vi"), QStringLiteral("vietnamese")}};
        return tokens.value(code);   // null when unknown → no filter
    }

    // {season, episodeStart, episodeEnd}; start/end −1 for a season-only
    // pack; nullopt when no season marker is found.
    static std::optional<std::array<int, 3>> parseSeasonEpisode(const QString &s) {
        static const QRegularExpression sxxeyy(QStringLiteral("(?i)S(\\d{1,2})E(\\d{1,3})(?:\\s*-\\s*E?(\\d{1,3}))?"));
        static const QRegularExpression nxnn(QStringLiteral("(?i)\\b(\\d{1,2})x(\\d{1,3})\\b"));
        static const QRegularExpression seasonOnly(QStringLiteral("(?i)(?:\\bS|\\bSeason\\s*)(\\d{1,2})(?![\\dE])"));
        auto m = sxxeyy.match(s);
        if (m.hasMatch()) {
            const int e1 = m.captured(2).toInt();
            const int e2 = m.captured(3).isEmpty() ? e1 : m.captured(3).toInt();
            return std::array<int, 3>{m.captured(1).toInt(), e1, std::max(e1, e2)};
        }
        m = nxnn.match(s);
        if (m.hasMatch()) {
            const int ep = m.captured(2).toInt();
            return std::array<int, 3>{m.captured(1).toInt(), ep, ep};
        }
        m = seasonOnly.match(s);
        if (m.hasMatch()) return std::array<int, 3>{m.captured(1).toInt(), -1, -1};
        return std::nullopt;
    }

    static bool matchesEpisode(const QString &release, int season, int episode) {
        const auto se = parseSeasonEpisode(release);
        if (!se) return true;
        if ((*se)[0] != season) return false;
        if ((*se)[1] < 0) return true;
        return episode >= (*se)[1] && episode <= (*se)[2];
    }

    static QString htmlUnescape(const QString &s) {
        QString out;
        int i = 0;
        while (i < s.length()) {
            if (s[i] != QLatin1Char('&')) { out += s[i++]; continue; }
            const int semi = s.indexOf(QLatin1Char(';'), i);
            if (semi < 0 || semi - i > 10) { out += s[i++]; continue; }
            const QString ent = s.mid(i + 1, semi - i - 1);
            QString rep;
            if (ent == QLatin1String("amp")) rep = QStringLiteral("&");
            else if (ent == QLatin1String("lt")) rep = QStringLiteral("<");
            else if (ent == QLatin1String("gt")) rep = QStringLiteral(">");
            else if (ent == QLatin1String("quot")) rep = QStringLiteral("\"");
            else if (ent == QLatin1String("apos")) rep = QStringLiteral("'");
            else if (ent == QLatin1String("nbsp")) rep = QStringLiteral(" ");
            else if (ent.startsWith(QLatin1String("#x")) || ent.startsWith(QLatin1String("#X"))) {
                bool ok = false;
                const uint cp = ent.mid(2).toUInt(&ok, 16);
                if (ok && cp > 0 && cp <= 0x10FFFF) rep = QString::fromUcs4(reinterpret_cast<const char32_t *>(&cp), 1);
            } else if (ent.startsWith(QLatin1Char('#'))) {
                bool ok = false;
                const uint cp = ent.mid(1).toUInt(&ok, 10);
                if (ok && cp > 0 && cp <= 0x10FFFF) rep = QString::fromUcs4(reinterpret_cast<const char32_t *>(&cp), 1);
            }
            if (rep.isNull()) { out += s[i++]; continue; }
            out += rep;
            i = semi + 1;
        }
        return out;
    }

    // {slug, isSeries} of the best match of POST /movie/search, or nullopt.
    std::optional<QPair<QString, bool>> resolveTitle(const QString &query) {
        QJsonObject body;
        body.insert(QStringLiteral("query"), query);
        body.insert(QStringLiteral("includeSeasons"), false);
        body.insert(QStringLiteral("limit"), 20);
        QMap<QString, QString> h = headers();
        h.insert(QStringLiteral("Content-Type"), QStringLiteral("application/json"));
        const HttpResponse resp = post(SEARCH_BASE + QStringLiteral("/movie/search"), h, QJsonDocument(body).toJson(QJsonDocument::Compact), true, &searchToken_);
        if (resp.code != 200) throw ProviderException(Kind::NETWORK, host_->tr("Search failed (HTTP {0}).").replace(QLatin1String("{0}"), QString::number(resp.code)));
        const QJsonDocument doc = QJsonDocument::fromJson(resp.body);
        if (!doc.isObject()) throw ProviderException(Kind::PARSE, host_->tr("Could not read the search results."));
        QString bestLink, bestType;
        double bestScore = 0;
        for (const QJsonValue &item : doc.object().value(QStringLiteral("results")).toArray()) {
            const QJsonObject r = item.toObject();
            const QString link = r.value(QStringLiteral("link")).toString();
            if (link.isEmpty()) continue;
            const double score = r.value(QStringLiteral("score")).toDouble(0);
            if (bestLink.isNull() || score > bestScore) {
                bestLink = link;
                bestScore = score;
                bestType = r.value(QStringLiteral("type")).toString();
            }
        }
        if (bestLink.isNull()) return std::nullopt;
        const QString slug = bestLink.mid(bestLink.lastIndexOf(QLatin1Char('/')) + 1);
        if (slug.isEmpty()) return std::nullopt;
        const QString type = bestType.toLower();
        return QPair<QString, bool>{slug, bestLink.startsWith(QLatin1String("/series/")) || type.contains(QLatin1String("series")) || type.contains(QLatin1String("tv"))};
    }

    const QString SEARCH_BASE = QStringLiteral("https://api.subsource.net/v1");
    const QString API_BASE = QStringLiteral("https://api.subsource.net/api/v1");
};

}  // namespace

namespace SubtitleProviders {

QList<std::shared_ptr<SubtitleProvider>> &all() {
    static QList<std::shared_ptr<SubtitleProvider>> list;
    return list;
}

void add(const std::shared_ptr<SubtitleProvider> &p) {
    all().append(p);
    std::stable_sort(all().begin(), all().end(), [](const auto &a, const auto &b) { return a->priority() < b->priority(); });
}

void registerBuiltin() {
    static bool done = false;
    if (done) return;
    done = true;
    HostServices *host = &JublerHostServices::instance();
    add(std::make_shared<SubSourceProvider>(host));
    add(std::make_shared<OpenSubtitlesProvider>(host));
    add(std::make_shared<SubDLProvider>(host));
}

}  // namespace SubtitleProviders
