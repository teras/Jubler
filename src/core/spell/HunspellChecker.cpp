/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/spell/HunspellChecker.h"

#include "core/os/Charsets.h"

#include <QDir>
#include <QEventLoop>
#include <QFile>
#include <QFileInfo>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QSaveFile>
#include <QTimer>
#include <algorithm>
#include <hunspell.hxx>

#include "core/i18n/I18N.h"
#include "core/options/Prefs.h"
#include "core/os/Debug.h"
#include "core/os/SystemDependent.h"

namespace {
const QString LANG_KEY = QStringLiteral("hunspell.language");
const QString BASE_URL = QStringLiteral("https://raw.githubusercontent.com/wooorm/dictionaries/main/dictionaries/");

struct Known { const char *code; const char *name; };
const Known KNOWN[] = {
    {"en", N__("English")}, {"en-GB", N__("English (British)")}, {"en-AU", N__("English (Australian)")}, {"en-CA", N__("English (Canadian)")},
    {"bg", N__("Bulgarian")}, {"ca", N__("Catalan")}, {"cs", N__("Czech")}, {"da", N__("Danish")}, {"de", N__("German")}, {"de-AT", N__("German (Austria)")},
    {"de-CH", N__("German (Switzerland)")}, {"el", N__("Greek")}, {"es", N__("Spanish")}, {"et", N__("Estonian")}, {"eu", N__("Basque")}, {"fa", N__("Persian")},
    {"fr", N__("French")}, {"ga", N__("Irish")}, {"gl", N__("Galician")}, {"he", N__("Hebrew")}, {"hr", N__("Croatian")}, {"hu", N__("Hungarian")}, {"is", N__("Icelandic")},
    {"it", N__("Italian")}, {"lt", N__("Lithuanian")}, {"lv", N__("Latvian")}, {"nb", N__("Norwegian (Bokmål)")}, {"nl", N__("Dutch")}, {"nn", N__("Norwegian (Nynorsk)")},
    {"pl", N__("Polish")}, {"pt", N__("Portuguese")}, {"pt-PT", N__("Portuguese (Portugal)")}, {"ro", N__("Romanian")}, {"ru", N__("Russian")}, {"sk", N__("Slovak")},
    {"sl", N__("Slovenian")}, {"sr", N__("Serbian")}, {"sv", N__("Swedish")}, {"tr", N__("Turkish")}, {"uk", N__("Ukrainian")}, {"vi", N__("Vietnamese")}};

// GET `url` into `target` reporting progress; throws SpellException.
void fetch(const QString &url, const QString &target, DownloadProgress &progress) {
    QNetworkAccessManager nam;
    QNetworkRequest req{QUrl(url)};
    req.setTransferTimeout(10000);
    // Uncompressed transfer: the progress compares the bytes with Content-Length.
    req.setRawHeader("Accept-Encoding", "identity");
    QNetworkReply *reply = nam.get(req);
    QSaveFile out(target);
    if (!out.open(QIODevice::WriteOnly)) {
        reply->abort();
        reply->deleteLater();
        throw SpellException(__("Cannot write {0}", target));
    }
    QEventLoop loop;
    bool cancelled = false;
    qint64 done = 0;
    QObject::connect(reply, &QNetworkReply::readyRead, &loop, [&]() {
        if (progress.isCancelled()) {
            cancelled = true;
            reply->abort();
            return;
        }
        const QByteArray chunk = reply->readAll();
        out.write(chunk);
        done += chunk.size();
        const qint64 total = reply->header(QNetworkRequest::ContentLengthHeader).toLongLong();
        progress.onProgress(total > 0 ? int(done * 100 / total) : 0, done, total);
    });
    QObject::connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    // Cancel is honoured at once, also while the server sends nothing.
    QTimer cancelPoll;
    QObject::connect(&cancelPoll, &QTimer::timeout, &loop, [&]() {
        if (!cancelled && progress.isCancelled()) {
            cancelled = true;
            reply->abort();
        }
    });
    cancelPoll.start(100);
    loop.exec();
    cancelPoll.stop();
    const int status = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
    const QNetworkReply::NetworkError err = reply->error();
    reply->deleteLater();
    if (cancelled) {
        out.cancelWriting();
        throw SpellException(__("Download cancelled"));
    }
    if (err != QNetworkReply::NoError || status != 200) {
        out.cancelWriting();
        throw SpellException(__("Failed to download {0}: HTTP {1}", url, status));
    }
    if (!out.commit()) throw SpellException(__("Cannot write {0}", target));
}
}  // namespace

namespace HunspellDicts {

const QString &builtinCode() {
    static const QString c = QStringLiteral("en");
    return c;
}

QString directory() {
    const QString dir = SystemDependent::getAppSupportDirPath() + QStringLiteral("/hunspelldicts");
    QDir().mkpath(dir);
    return dir;
}

QString dicPath(const QString &code) { return directory() + QLatin1Char('/') + code + QStringLiteral(".dic"); }
QString affPath(const QString &code) { return directory() + QLatin1Char('/') + code + QStringLiteral(".aff"); }

void ensureBuiltinEnglish() {
    for (const char *ext : {"dic", "aff"}) {
        const QString target = directory() + QStringLiteral("/en.") + QLatin1String(ext);
        if (QFileInfo(target).size() > 0) continue;
        QFile res(QStringLiteral(":/dicts/en.") + QLatin1String(ext));
        if (!res.open(QIODevice::ReadOnly)) throw SpellException(__("Missing bundled resource: {0}", QStringLiteral("dicts/en.") + QLatin1String(ext)));
        QSaveFile out(target);
        if (!out.open(QIODevice::WriteOnly) || out.write(res.readAll()) < 0 || !out.commit())
            throw SpellException(__("Cannot write {0}", target));
    }
}

QString displayName(const QString &code) {
    for (const Known &k : KNOWN)
        if (code == QLatin1String(k.code)) return __(k.name);
    return code;
}

QList<SpellLanguage> installedLanguages() {
    QList<SpellLanguage> out;
    out.append({builtinCode(), displayName(builtinCode()), true});
    QList<SpellLanguage> others;
    const QDir dir(directory());
    for (const QFileInfo &fi : dir.entryInfoList(QDir::Files)) {
        // ".dic" exactly (the name filters would also take "X.DIC").
        if (!fi.fileName().endsWith(QLatin1String(".dic"))) continue;
        const QString code = fi.fileName().chopped(4);
        if (code == builtinCode() || !QFileInfo::exists(affPath(code))) continue;
        others.append({code, displayName(code), false});
    }
    // By language code, as the Java listed them.
    std::sort(others.begin(), others.end(), [](const SpellLanguage &a, const SpellLanguage &b) { return a.code.compare(b.code, Qt::CaseInsensitive) < 0; });
    out.append(others);
    return out;
}

QList<SpellLanguage> downloadableLanguages() {
    const QList<SpellLanguage> installed = installedLanguages();
    QList<SpellLanguage> out;
    for (const Known &k : KNOWN) {
        const QString code = QString::fromLatin1(k.code);
        bool have = false;
        for (const SpellLanguage &l : installed) if (l.code == code) have = true;
        if (!have) out.append({code, QString::fromUtf8(k.name), false});
    }
    return out;
}

void download(const QString &code, DownloadProgress &progress) {
    const QString dic = dicPath(code), aff = affPath(code);
    try {
        fetch(BASE_URL + code + QStringLiteral("/index.dic"), dic, progress);
        fetch(BASE_URL + code + QStringLiteral("/index.aff"), aff, progress);
    } catch (...) {
        QFile::remove(dic);
        QFile::remove(aff);
        throw;
    }
}

bool remove(const QString &code) {
    if (code == builtinCode()) return false;
    const bool a = QFile::remove(dicPath(code));
    const bool b = QFile::remove(affPath(code));
    return a && b;
}

QList<std::pair<int, QString>> tokenize(const QString &text) {
    QList<std::pair<int, QString>> out;
    const int n = text.length();
    int i = 0;
    while (i < n) {
        const QChar c = text[i];
        if (c == QLatin1Char('<') || c == QLatin1Char('{')) {
            // Skip a tag run; an unterminated one runs to the end.
            const QChar close = c == QLatin1Char('<') ? QLatin1Char('>') : QLatin1Char('}');
            const int end = text.indexOf(close, i + 1);
            i = end < 0 ? n : end + 1;
            continue;
        }
        if (!c.isLetter()) {
            ++i;
            continue;
        }
        const int start = i;
        while (i < n) {
            const QChar w = text[i];
            if (w.isLetter()) { ++i; continue; }
            if ((w == QLatin1Char('\'') || w == QChar(0x2019)) && i + 1 < n && text[i + 1].isLetter()) { ++i; continue; }
            break;
        }
        out.append({start, text.mid(start, i - start)});
    }
    return out;
}

}  // namespace HunspellDicts

// ---- HunspellChecker ------------------------------------------------------------------------------

HunspellChecker::HunspellChecker() = default;
HunspellChecker::~HunspellChecker() = default;

std::optional<SpellLanguage> HunspellChecker::getActiveLanguage() const {
    const QString code = Prefs::getString(LANG_KEY, HunspellDicts::builtinCode());
    const QList<SpellLanguage> installed = HunspellDicts::installedLanguages();
    for (const SpellLanguage &l : installed)
        if (l.code == code) return l;
    if (!installed.isEmpty()) return installed.first();
    return std::nullopt;
}

void HunspellChecker::setActiveLanguage(const SpellLanguage &lang) {
    Prefs::set(LANG_KEY, lang.code);
}

void HunspellChecker::start() {
    HunspellDicts::ensureBuiltinEnglish();
    const auto lang = getActiveLanguage();
    if (!lang || lang->code.isEmpty()) throw SpellException(__("No language selected for spell checking"));
    const QString dic = HunspellDicts::dicPath(lang->code), aff = HunspellDicts::affPath(lang->code);
    if (!QFileInfo::exists(dic) || !QFileInfo::exists(aff)) throw SpellException(__("Dictionary for '{0}' is not installed.", lang->code));
    try {
        engine_ = std::make_unique<Hunspell>(aff.toLocal8Bit().constData(), dic.toLocal8Bit().constData());
        encoding_ = QByteArray(engine_->get_dic_encoding());
    } catch (const std::exception &e) {
        engine_.reset();
        throw SpellException(QString::fromUtf8(e.what()));
    }
    // An unreadable dictionary charset would make every word "correct".
    if (!encoding_.isEmpty() && !Charsets::isSupported(QString::fromLatin1(encoding_))) {
        const QString enc = QString::fromLatin1(encoding_);
        engine_.reset();
        throw SpellException(__("The dictionary for '{0}' uses the unsupported character set {1}.", lang->code, enc));
    }
}

void HunspellChecker::stop() {
    engine_.reset();
}

QList<SpellError> HunspellChecker::checkSpelling(const QString &text) {
    QList<SpellError> out;
    if (!engine_) return out;
    // The dictionary's own charset (its .aff SET), which may be one ICU lacks (ISO-8859-16).
    const QString cs = encoding_.isEmpty() ? QStringLiteral("UTF-8") : QString::fromLatin1(encoding_);
    for (const auto &[pos, word] : HunspellDicts::tokenize(text)) {
        const QByteArray bytes = Charsets::encode(word, cs);
        const std::string w(bytes.constData(), size_t(bytes.size()));
        bool correct = true;
        try {
            correct = engine_->spell(w);
        } catch (...) {
            correct = true;   // never block on an engine hiccup
        }
        if (correct) continue;
        SpellError e;
        e.position = pos;
        e.original = word;
        try {
            for (const std::string &s : engine_->suggest(w)) e.alternatives.append(Charsets::decode(QByteArray(s.data(), int(s.size())), cs));
        } catch (...) {
        }
        out.append(e);
    }
    return out;
}

bool HunspellChecker::insertWord(const QString &word) {
    if (!engine_) return false;
    const QByteArray bytes = Charsets::encode(word, encoding_.isEmpty() ? QStringLiteral("UTF-8") : QString::fromLatin1(encoding_));
    return engine_->add(std::string(bytes.constData(), size_t(bytes.size()))) == 0;
}

// ---- registry -------------------------------------------------------------------------------------

namespace AvailSpellCheckers {
QList<std::shared_ptr<SpellChecker>> &all() {
    static QList<std::shared_ptr<SpellChecker>> list;
    return list;
}
void registerBuiltin() {
    if (all().isEmpty()) all().append(std::make_shared<HunspellChecker>());
}
}  // namespace AvailSpellCheckers
