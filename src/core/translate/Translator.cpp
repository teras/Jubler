/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/translate/Translator.h"

#include <QEventLoop>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QTimer>
#include <QUrl>

#include "core/i18n/I18N.h"
#include "core/options/Prefs.h"
#include "core/os/Encryption.h"

// ---- WebTranslator ---------------------------------------------------------------------------------

bool WebTranslator::translate(const QList<SubEntryPtr> &subs, const Language &from, const Language &to, TranslateProgress *progress) {
    lastError_ = QString();
    if (progress) progress->setValues(subs.size(), __("Translating to {0}", to.displayName));
    for (int i = 0; i < subs.size(); i += blockSize_) {
        if (progress && progress->isCancelled()) break;
        if (progress) progress->updateProgress(i);
        const QList<SubEntryPtr> group = subs.mid(i, std::min(blockSize_, int(subs.size()) - i));
        const QString err = translatePart(group, from, to);
        if (!err.isNull()) {
            lastError_ = err;
            return false;
        }
    }
    if (progress) progress->updateProgress(subs.size());
    return true;
}

QString WebTranslator::translatePart(const QList<SubEntryPtr> &group, const Language &from, const Language &to) {
    QString url = getTranslationURL(from, to);
    const QString body = getBody(group, from, to);
    if (!isPost()) url += QLatin1Char('&') + body;
    QNetworkAccessManager nam;
    nam.setTransferTimeout(std::max(connectTimeout_, readTimeout_));
    QNetworkRequest req{QUrl(url)};
    for (const auto &p : getRequestProperties()) req.setRawHeader(p.first.toUtf8(), p.second.toUtf8());
    QNetworkReply *reply = isPost() ? nam.post(req, body.toUtf8()) : nam.get(req);
    QEventLoop loop;
    QObject::connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    loop.exec();
    QString result;
    if (reply->error() != QNetworkReply::NoError) {
        // Azure answers 401/403/429 with a JSON body: report the status, not a parse failure.
        result = reply->errorString();
        const QByteArray body = reply->readAll().left(200).trimmed();
        if (!body.isEmpty()) result += QStringLiteral(": ") + QString::fromUtf8(body);
    } else {
        // Lines concatenated without newlines, as the Java reader did.
        const QString text = QString::fromUtf8(reply->readAll());
        result = parseResults(group, QString(text).remove(QLatin1Char('\n')).remove(QLatin1Char('\r')));
    }
    reply->deleteLater();
    return result;
}

// ---- SimpleWebTranslator ---------------------------------------------------------------------------

namespace {
struct LangDef { const char *id; const char *name; };
const LangDef LANGS[] = {
    {"en", N__("English")}, {"sq", N__("Albanian")}, {"ar", N__("Arabic")}, {"bg", N__("Bulgarian")}, {"ca", N__("Catalan")}, {"zh-CN", N__("Chinese (Simplified)")},
    {"zh-TW", N__("Chinese (Traditional)")}, {"hr", N__("Croatian")}, {"cs", N__("Czech")}, {"da", N__("Danish")}, {"nl", N__("Dutch")}, {"et", N__("Estonian")},
    {"tl", N__("Filipino")}, {"fi", N__("Finnish")}, {"fr", N__("French")}, {"gl", N__("Galician")}, {"de", N__("German")}, {"el", N__("Greek")}, {"hi", N__("Hindi")},
    {"hu", N__("Hungarian")}, {"id", N__("Indonesian")}, {"it", N__("Italian")}, {"ja", N__("Japanese")}, {"ko", N__("Korean")}, {"lv", N__("Latvian")}, {"lt", N__("Lithuanian")},
    {"mt", N__("Maltese")}, {"no", N__("Norwegian")}, {"pl", N__("Polish")}, {"pt", N__("Portuguese")}, {"ro", N__("Romanian")}, {"ru", N__("Russian")}, {"sr", N__("Serbian")},
    {"sk", N__("Slovak")}, {"sl", N__("Slovenian")}, {"es", N__("Spanish")}, {"sv", N__("Swedish")}, {"th", N__("Thai")}, {"tr", N__("Turkish")}, {"uk", N__("Ukrainian")},
    {"vi", N__("Vietnamese")}};
}  // namespace

QList<Language> SimpleWebTranslator::getSourceLanguages() const {
    QList<Language> out;
    out.append({QString(), __("<Auto detect>")});
    for (const LangDef &l : LANGS) out.append({QString::fromLatin1(l.id), __(l.name)});
    return out;
}

QList<Language> SimpleWebTranslator::getDestinationLanguagesFor(const Language &) const {
    QList<Language> out;
    for (const LangDef &l : LANGS) out.append({QString::fromLatin1(l.id), __(l.name)});
    return out;
}

Language SimpleWebTranslator::getDefaultSourceLanguage() const { return {QString(), __("<Auto detect>")}; }
Language SimpleWebTranslator::getDefaultDestinationLanguage() const { return {QStringLiteral("en"), __("English")}; }

// ---- AzureTranslator -------------------------------------------------------------------------------

namespace {
const QString AZ_URL = QStringLiteral("azure.translation.baseurl");
const QString AZ_REGION = QStringLiteral("azure.translation.region");
const QString AZ_ENCKEY = QStringLiteral("azure.translation.enckey");
const QString AZ_LEGACY = QStringLiteral("azure.translation.key");
}  // namespace

AzureTranslator::AzureTranslator() = default;

QString AzureTranslator::getDefinition() const { return __("Azure translate") + QStringLiteral(" (API)"); }
QString AzureTranslator::baseUrl() const { return Prefs::getString(AZ_URL, QString()); }
QString AzureTranslator::region() const { return Prefs::getString(AZ_REGION, QString()); }
bool AzureTranslator::hasStoredKey() const { return !Prefs::getString(AZ_ENCKEY, QString()).isEmpty(); }
bool AzureTranslator::hasLegacyKey() const { return !Prefs::getString(AZ_LEGACY, QString()).isEmpty(); }
void AzureTranslator::clearLegacyKey() { Prefs::remove(AZ_LEGACY); }

QString AzureTranslator::saveConfiguration(const QString &url, const QString &typedKey, const QString &reg) {
    if (pin_.isEmpty() && pinPrompt_) pin_ = pinPrompt_();
    if (pin_.isEmpty()) return __("Azure PIN should be provided.");
    Prefs::set(AZ_URL, url);
    if (!typedKey.isNull()) {
        const auto enc = Encryption::encrypt(typedKey, pin_);
        if (!enc) return __("Azure key not provided yet.");
        Prefs::set(AZ_ENCKEY, *enc);
    }
    Prefs::set(AZ_REGION, reg);
    return QString();
}

QString AzureTranslator::isReady() {
    if (baseUrl().isEmpty()) return __("Base URL not provided yet.");
    if (region().isEmpty()) return __("Region not provided yet.");
    if (!hasStoredKey()) return __("Azure key not provided yet.");
    if (pin_.isEmpty() && pinPrompt_) pin_ = pinPrompt_();
    if (pin_.isEmpty()) return __("Azure PIN should be provided.");
    return QString();
}

QString AzureTranslator::decryptedKey() const {
    const auto key = Encryption::decrypt(Prefs::getString(AZ_ENCKEY, QString()), pin_);
    return key ? *key : QString();
}

QString AzureTranslator::getTranslationURL(const Language &from, const Language &to) const {
    QString url = baseUrl();
    url += url.contains(QLatin1Char('?')) ? QLatin1Char('&') : QLatin1Char('?');
    if (!from.isAuto()) url += QStringLiteral("from=") + from.id + QLatin1Char('&');
    url += QStringLiteral("to=") + to.id;
    return url;
}

QList<QPair<QString, QString>> AzureTranslator::getRequestProperties() const {
    return {{QStringLiteral("Ocp-Apim-Subscription-Key"), decryptedKey()},
            {QStringLiteral("Ocp-Apim-Subscription-Region"), region()},
            {QStringLiteral("Content-Type"), QStringLiteral("application/json")}};
}

QString AzureTranslator::getBody(const QList<SubEntryPtr> &group, const Language &, const Language &) const {
    QJsonArray arr;
    for (const SubEntryPtr &e : group) {
        QJsonObject o;
        o.insert(QStringLiteral("Text"), e->getText());
        arr.append(o);
    }
    return QString::fromUtf8(QJsonDocument(arr).toJson(QJsonDocument::Compact));
}

QString AzureTranslator::parseResults(const QList<SubEntryPtr> &group, const QString &response) {
    const QJsonDocument doc = QJsonDocument::fromJson(response.toUtf8());
    if (!doc.isArray()) return __("Wrong translation output");
    const QJsonArray arr = doc.array();
    QStringList texts;
    for (const QJsonValue &v : arr) {
        const QJsonArray tr = v.toObject().value(QStringLiteral("translations")).toArray();
        texts.append(tr.isEmpty() ? QString() : tr.first().toObject().value(QStringLiteral("text")).toString());
    }
    if (texts.size() != group.size()) return __("Original text and translation does not match!");
    for (int i = 0; i < group.size(); ++i) group[i]->setText(texts[i]);
    return QString();
}

// ---- registry --------------------------------------------------------------------------------------

namespace AvailTranslators {
QList<std::shared_ptr<Translator>> &all() {
    static QList<std::shared_ptr<Translator>> list;
    return list;
}
void registerBuiltin() {
    if (all().isEmpty()) all().append(std::make_shared<AzureTranslator>());
}
}  // namespace AvailTranslators
