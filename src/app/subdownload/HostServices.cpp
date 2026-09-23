/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/subdownload/HostServices.h"

#include <QDialog>
#include <QDialogButtonBox>
#include <QEventLoop>
#include <QFormLayout>
#include <QInputDialog>
#include <QLabel>
#include <QLineEdit>
#include <QMetaObject>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QPointer>
#include <QSslConfiguration>
#include <QThread>
#include <QTimer>
#include <QVBoxLayout>
#include <atomic>
#include <memory>
#include <mutex>

#include "app/Version.h"
#include "core/i18n/I18N.h"
#include "core/options/Prefs.h"
#include "core/os/Debug.h"
#include "core/os/Encryption.h"
#include "core/subdownload/QueryParse.h"

namespace {
constexpr int CONNECT_TIMEOUT = 15000, READ_TIMEOUT = 30000;
constexpr qint64 MAX_BODY = 8 * 1024 * 1024;

// Aborts the reply from any thread.
class ReplyToken : public jubler::CancelToken {
public:
    explicit ReplyToken(QNetworkReply *r) : reply_(r) {}
    void cancel() override {
        QNetworkReply *r = reply_;
        if (r) QMetaObject::invokeMethod(r, [r]() { r->abort(); }, Qt::QueuedConnection);
    }

private:
    QPointer<QNetworkReply> reply_;
};
}  // namespace

JublerHostServices &JublerHostServices::instance() {
    static JublerHostServices s;
    return s;
}

QString JublerHostServices::userAgent() const { return QStringLiteral("Jubler v") + Version::current(); }
QString JublerHostServices::browserUserAgent() const {
    return QStringLiteral("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0 Safari/537.36");
}

jubler::HttpResponse JublerHostServices::get(const QString &url, const QMap<QString, QString> &headers, bool follow, std::shared_ptr<jubler::CancelToken> *token) {
    return request(url, headers, nullptr, follow, token);
}

jubler::HttpResponse JublerHostServices::post(const QString &url, const QMap<QString, QString> &headers, const QByteArray &body, bool follow, std::shared_ptr<jubler::CancelToken> *token) {
    return request(url, headers, &body, follow, token);
}

jubler::HttpResponse JublerHostServices::request(const QString &url, const QMap<QString, QString> &headers, const QByteArray *body, bool follow, std::shared_ptr<jubler::CancelToken> *token) {
    // One manager per call: providers run in worker threads.
    QNetworkAccessManager nam;
    nam.setTransferTimeout(READ_TIMEOUT);
    QNetworkRequest req{QUrl(url)};
    req.setAttribute(QNetworkRequest::RedirectPolicyAttribute, follow ? QNetworkRequest::NoLessSafeRedirectPolicy : QNetworkRequest::ManualRedirectPolicy);
    req.setHeader(QNetworkRequest::UserAgentHeader, userAgent());
    if (req.url().scheme() == QLatin1String("https")) {
        // Offer only modern signature schemes (no SHA-1): Cloudflare's bot
        // filter answers 403 to handshakes that still list the old ones.
        QSslConfiguration ssl = QSslConfiguration::defaultConfiguration();
        ssl.setBackendConfigurationOption(QByteArrayLiteral("SignatureAlgorithms"),
                                          QByteArrayLiteral("ECDSA+SHA256:ECDSA+SHA384:ECDSA+SHA512:ed25519:ed448:"
                                                            "rsa_pss_rsae_sha256:rsa_pss_rsae_sha384:rsa_pss_rsae_sha512:"
                                                            "rsa_pss_pss_sha256:rsa_pss_pss_sha384:rsa_pss_pss_sha512:"
                                                            "RSA+SHA256:RSA+SHA384:RSA+SHA512"));
        req.setSslConfiguration(ssl);
    }
    for (auto it = headers.constBegin(); it != headers.constEnd(); ++it) req.setRawHeader(it.key().toUtf8(), it.value().toUtf8());
    QNetworkReply *reply = body ? nam.post(req, *body) : nam.get(req);
    auto tok = std::make_shared<ReplyToken>(reply);
    if (token) std::atomic_store(token, std::shared_ptr<jubler::CancelToken>(tok));   // read by cancel() on the GUI thread
    QEventLoop loop;
    QTimer connectTimer;
    connectTimer.setSingleShot(true);
    bool tooBig = false, timedOut = false;
    // The connect timeout covers connecting only (Java: 15 s); once the request
    // is out, the transfer timeout (30 s without data) applies.
    QObject::connect(reply, &QNetworkReply::requestSent, &loop, [&]() { connectTimer.stop(); });
    QObject::connect(reply, &QNetworkReply::metaDataChanged, &loop, [&]() { connectTimer.stop(); });
    QObject::connect(reply, &QNetworkReply::readyRead, &loop, [&]() {
        if (reply->bytesAvailable() > MAX_BODY || reply->size() > MAX_BODY) {
            tooBig = true;
            reply->abort();
        }
    });
    QObject::connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    QObject::connect(&connectTimer, &QTimer::timeout, &loop, [&]() {
        timedOut = true;
        reply->abort();
    });
    connectTimer.start(CONNECT_TIMEOUT);
    loop.exec();
    jubler::HttpResponse out;
    out.code = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
    out.body = reply->readAll();
    out.contentType = reply->header(QNetworkRequest::ContentTypeHeader).toString();
    for (const auto &h : reply->rawHeaderPairs()) out.headers[QString::fromLatin1(h.first).toLower()].append(QString::fromUtf8(h.second));
    const QNetworkReply::NetworkError err = reply->error();
    const QString errText = reply->errorString();
    reply->deleteLater();
    if (token) std::atomic_store(token, std::shared_ptr<jubler::CancelToken>());
    const auto fail = [&out](const QString &message) {
        out.error = jubler::ProviderError{jubler::ProviderError::Kind::NETWORK, message};
        return out;
    };
    if (tooBig || out.body.size() > MAX_BODY) return fail(__("Response exceeds size limit"));
    // Transport failures (no HTTP status) are errors; HTTP errors are
    // returned with their body for the provider to interpret.
    if (timedOut || err == QNetworkReply::TimeoutError || err == QNetworkReply::OperationCanceledError)
        return fail(__("Network error: {0}", timedOut ? __("timeout") : errText));
    // A transport failure is an error even after a status line (a connection
    // closed mid-body); HTTP error statuses (content/protocol/server classes)
    // keep their body for the provider.
    const int e = int(err);
    if (err != QNetworkReply::NoError && (out.code == 0 || (e >= 1 && e < 200)))
        return fail(__("Network error: {0}", errText));
    bool lengthOk = false;
    const qint64 declared = out.header(QStringLiteral("content-length")).toLongLong(&lengthOk);
    if (lengthOk && out.code >= 200 && out.code < 300 && out.header(QStringLiteral("content-encoding")).isEmpty() && out.body.size() < declared)
        return fail(__("Network error: {0}", __("incomplete download")));
    return out;
}

jubler::ProviderResult<QByteArray> JublerHostServices::extractSubtitleBytes(const QByteArray &payload) {
    try {
        return Extract::subtitleBytes(payload);
    } catch (const std::runtime_error &e) {
        return jubler::ProviderError{jubler::ProviderError::Kind::PARSE, QString::fromUtf8(e.what())};
    }
}

std::optional<QString> JublerHostServices::movieHash(const QString &path) { return MovieHash::compute(path); }

QString JublerHostServices::sessionPin(QWidget *parent) {
    // Asked again (per operation) until a PIN is given.
    if (!pin_.isEmpty()) return pin_;
    bool ok = false;
    const QString pin = QInputDialog::getText(parent, __("PIN"), __("Enter a PIN to encrypt/decrypt your subtitle provider keys"), QLineEdit::Password, QString(), &ok);
    pin_ = ok ? pin : QString();
    return pin_;
}

bool JublerHostServices::isSecretStored(const QString &prefKey) { return !Prefs::getString(prefKey, QString()).isEmpty(); }

bool JublerHostServices::storeSecret(QWidget *parent, const QString &prefKey, const QString &value) {
    const QString pin = sessionPin(parent);
    if (pin.isEmpty()) return false;
    const auto blob = Encryption::encrypt(value, pin);
    if (!blob) return false;
    Prefs::set(prefKey, *blob);
    return true;
}

QString JublerHostServices::loadSecret(QWidget *parent, const QString &prefKey) {
    const QString blob = Prefs::getString(prefKey, QString());
    if (blob.isEmpty()) return QString(QLatin1String(""));
    const QString pin = sessionPin(parent);
    if (pin.isEmpty()) return QString(QLatin1String(""));
    const auto plain = Encryption::decrypt(blob, pin);
    return plain ? *plain : QString(QLatin1String(""));
}

QString JublerHostServices::tr(const char *english) { return __(english); }
void JublerHostServices::debug(const QString &message) { Debug::debug(message); }

jubler::HostServices::ParsedQuery JublerHostServices::parseQuery(const QString &query) {
    const QueryParse p = QueryParse::of(query);
    ParsedQuery out;
    out.title = p.title;
    out.season = p.season;
    out.episode = p.episode;
    return out;
}

QString JublerHostServices::askApiKey(QWidget *parent, const QString &title, const QString &html, const QString &label, bool keyStored) {
    const QString mask = QStringLiteral("****************");
    QDialog dlg(parent);
    dlg.setWindowTitle(title);
    auto *lay = new QVBoxLayout(&dlg);
    auto *text = new QLabel(html, &dlg);
    text->setOpenExternalLinks(true);
    text->setWordWrap(true);
    lay->addWidget(text);
    auto *form = new QFormLayout();
    auto *field = new QLineEdit(keyStored ? mask : QString(), &dlg);
    field->setMinimumWidth(320);
    form->addRow(label, field);
    lay->addLayout(form);
    auto *buttons = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel, &dlg);
    QObject::connect(buttons, &QDialogButtonBox::accepted, &dlg, &QDialog::accept);
    QObject::connect(buttons, &QDialogButtonBox::rejected, &dlg, &QDialog::reject);
    lay->addWidget(buttons);
    if (dlg.exec() != QDialog::Accepted) return QString();
    const QString typed = field->text().trimmed();
    if (typed.isEmpty() || typed == mask) return QString();
    return typed;
}

QString JublerHostServices::setting(const QString &key, const QString &defaultValue) {
    return Prefs::getString(QStringLiteral("plugins.data.") + key, defaultValue);
}

void JublerHostServices::setSetting(const QString &key, const QString &value) {
    Prefs::set(QStringLiteral("plugins.data.") + key, value);
}
