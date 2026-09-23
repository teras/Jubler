/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/externals/Recipe.h"

#include <QDir>
#include <QEventLoop>
#include <QFile>
#include <QFileInfo>
#include <QJsonArray>
#include <QJsonDocument>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QProcess>
#include <QProcessEnvironment>
#include <QRegularExpression>
#include <QSaveFile>
#include <QThread>
#include <mutex>

#include "core/i18n/I18N.h"
#include "core/options/Prefs.h"
#include "core/os/Debug.h"
#include "core/os/Encryption.h"
#include "core/os/SystemDependent.h"

// ---- OutputMode ------------------------------------------------------------------------------------

QString outputModeName(OutputMode m) {
    switch (m) {
        case OutputMode::REPLACE: return QStringLiteral("REPLACE");
        case OutputMode::PATCH_TEXT: return QStringLiteral("PATCH_TEXT");
        case OutputMode::PATCH_TIMING: return QStringLiteral("PATCH_TIMING");
        case OutputMode::PATCH_BOTH: return QStringLiteral("PATCH_BOTH");
    }
    return QStringLiteral("REPLACE");
}

QString outputModeLabel(OutputMode m) {
    switch (m) {
        case OutputMode::REPLACE: return __("Replace");
        case OutputMode::PATCH_TEXT: return __("Update text");
        case OutputMode::PATCH_TIMING: return __("Update timing");
        case OutputMode::PATCH_BOTH: return __("Update text + timing");
    }
    return __("Replace");
}

OutputMode outputModeFromName(const QString &name) {
    for (OutputMode m : {OutputMode::REPLACE, OutputMode::PATCH_TEXT, OutputMode::PATCH_TIMING, OutputMode::PATCH_BOTH})
        if (outputModeName(m) == name) return m;
    return OutputMode::REPLACE;
}

// ---- RecipeParam -----------------------------------------------------------------------------------

QList<RecipeParam::Type> RecipeParam::allTypes() {
    return {Type::TEXTBOX, Type::COMBOBOX, Type::CHECKBOX, Type::PATH, Type::LANGUAGE, Type::WINDOW, Type::VIDEO_SUBTITLE, Type::SECRET};
}

QString RecipeParam::typeName(Type t) {
    switch (t) {
        case Type::TEXTBOX: return QStringLiteral("TEXTBOX");
        case Type::COMBOBOX: return QStringLiteral("COMBOBOX");
        case Type::CHECKBOX: return QStringLiteral("CHECKBOX");
        case Type::PATH: return QStringLiteral("PATH");
        case Type::LANGUAGE: return QStringLiteral("LANGUAGE");
        case Type::WINDOW: return QStringLiteral("WINDOW");
        case Type::VIDEO_SUBTITLE: return QStringLiteral("VIDEO_SUBTITLE");
        case Type::SECRET: return QStringLiteral("SECRET");
    }
    return QStringLiteral("TEXTBOX");
}

QString RecipeParam::typeLabel(Type t) {
    switch (t) {
        case Type::TEXTBOX: return __("Text");
        case Type::COMBOBOX: return __("Dropdown");
        case Type::CHECKBOX: return __("Checkbox");
        case Type::PATH: return __("Path");
        case Type::LANGUAGE: return __("Language");
        case Type::WINDOW: return __("Window");
        case Type::VIDEO_SUBTITLE: return __("Subtitle stream");
        case Type::SECRET: return __("Secret");
    }
    return QString();
}

QString RecipeParam::typeDescription(Type t) {
    switch (t) {
        case Type::TEXTBOX: return __("A free-text field the user types into.");
        case Type::COMBOBOX: return __("A drop-down list of predefined choices.");
        case Type::CHECKBOX: return __("An on/off box that adds a fixed value to the command when checked.");
        case Type::PATH: return __("A file or folder picker with a Browse button.");
        case Type::LANGUAGE: return __("A language selector that emits the ISO language code.");
        case Type::WINDOW: return __("A drop-down of other open subtitle windows; that window's subtitles are saved to a temporary file and passed to the tool.");
        case Type::VIDEO_SUBTITLE: return __("A drop-down of the subtitle streams embedded in the attached video; the chosen stream's index (or id/language) is passed to the tool.");
        case Type::SECRET: return __("A masked password field. Store it once (encrypted, never shared) to reuse it silently, or leave it unstored to be asked for it live on every run.");
    }
    return QString();
}

RecipeParam::Type RecipeParam::typeFromName(const QString &name) {
    for (Type t : allTypes())
        if (typeName(t) == name) return t;
    return Type::TEXTBOX;
}

QStringList RecipeParam::getChoiceList() const {
    const QString c = choices.trimmed();
    if (c.isEmpty()) return {};
    static const QRegularExpression sep(QStringLiteral("\\s*\\|\\s*"));
    return c.split(sep);
}

QString RecipeParam::validateKey(const QString &key, const QSet<QString> &existing) {
    if (key.length() < 2) return __("Key must be at least 2 characters (single-character keys are reserved).");
    static const QRegularExpression re(QStringLiteral("^[a-zA-Z][a-zA-Z0-9]*$"));
    if (!re.match(key).hasMatch()) return __("Key must start with a letter and contain only letters and digits.");
    if (existing.contains(key)) return __("A parameter with this key already exists.");
    return QString();
}

QJsonObject RecipeParam::toJson(bool forSharing) const {
    QJsonObject o;
    o.insert(QStringLiteral("key"), key);
    o.insert(QStringLiteral("label"), label);
    o.insert(QStringLiteral("help"), help);
    o.insert(QStringLiteral("type"), typeName(type));
    if (!defaultValue.isEmpty() && !(forSharing && type == Type::SECRET)) o.insert(QStringLiteral("default"), defaultValue);
    if (!choices.isEmpty()) o.insert(QStringLiteral("choices"), choices);
    if (folder) o.insert(QStringLiteral("folder"), true);
    if (!checkedValue.isEmpty()) o.insert(QStringLiteral("checkedValue"), checkedValue);
    if (type == Type::VIDEO_SUBTITLE && field != QLatin1String("index")) o.insert(QStringLiteral("field"), field);
    if (type == Type::VIDEO_SUBTITLE && accept != QLatin1String("any")) o.insert(QStringLiteral("accept"), accept);
    return o;
}

RecipeParam RecipeParam::fromJson(const QJsonObject &o) {
    RecipeParam p;
    p.key = o.value(QStringLiteral("key")).toString(QStringLiteral("param"));
    p.label = o.value(QStringLiteral("label")).toString();
    p.help = o.value(QStringLiteral("help")).toString();
    p.type = typeFromName(o.value(QStringLiteral("type")).toString());
    p.defaultValue = o.value(QStringLiteral("default")).toString();
    p.choices = o.value(QStringLiteral("choices")).toString();
    p.folder = o.value(QStringLiteral("folder")).toBool(false);
    p.checkedValue = o.value(QStringLiteral("checkedValue")).toString();
    p.field = o.value(QStringLiteral("field")).toString(QStringLiteral("index"));
    p.accept = o.value(QStringLiteral("accept")).toString(QStringLiteral("any"));
    return p;
}

// ---- Recipe ------------------------------------------------------------------------------------------

const QString Recipe::DEFAULT_COMMAND = QStringLiteral("%x --input %i --output %o");

Recipe::Recipe() : Recipe(QStringLiteral("Recipe")) {}
Recipe::Recipe(const QString &n) : name(n), command(DEFAULT_COMMAND) {}

SubFormatPtr Recipe::defaultFormat() {
    const AvailSubFormats &f = Availabilities::formats();
    if (SubFormatPtr srt = f.findFromExtension(QStringLiteral("srt"))) return srt;
    return f.size() > 0 ? f.get(0) : nullptr;
}

SubFormatPtr Recipe::getFormat() const { return format_ ? format_ : defaultFormat(); }

QSet<QString> Recipe::keysExcept(const RecipeParam *self) const {
    QSet<QString> out;
    for (const RecipeParam &p : params)
        if (&p != self) out.insert(p.key);
    return out;
}

bool Recipe::hasSecretValue() const {
    for (const RecipeParam &p : params)
        if (p.isSecret() && !p.defaultValue.isEmpty()) return true;
    return false;
}

QJsonObject Recipe::toJson(bool forSharing) const {
    QJsonObject o;
    o.insert(QStringLiteral("name"), name);
    o.insert(QStringLiteral("description"), description);
    o.insert(QStringLiteral("url"), url);
    o.insert(QStringLiteral("path"), path);
    o.insert(QStringLiteral("command"), command);
    const SubFormatPtr f = getFormat();
    o.insert(QStringLiteral("format"), f ? f->getName() : QString());
    if (outputFolder) o.insert(QStringLiteral("outputFolder"), true);
    o.insert(QStringLiteral("output"), outputModeName(outputMode));
    QJsonArray arr;
    for (const RecipeParam &p : params) arr.append(p.toJson(forSharing));
    o.insert(QStringLiteral("params"), arr);
    return o;
}

QString Recipe::toJsonString(bool forSharing) const {
    return QString::fromUtf8(QJsonDocument(toJson(forSharing)).toJson(QJsonDocument::Indented));
}

Recipe Recipe::fromJson(const QJsonObject &o) {
    Recipe r(o.value(QStringLiteral("name")).toString(QStringLiteral("Recipe")));
    r.description = o.value(QStringLiteral("description")).toString();
    r.url = o.value(QStringLiteral("url")).toString();
    r.path = o.value(QStringLiteral("path")).toString();
    r.command = o.value(QStringLiteral("command")).toString(DEFAULT_COMMAND);
    const QString fmt = o.value(QStringLiteral("format")).toString();
    r.format_ = fmt.isEmpty() ? nullptr : Availabilities::formats().findFromName(fmt);
    r.outputFolder = o.value(QStringLiteral("outputFolder")).toBool(false);
    r.outputMode = outputModeFromName(o.value(QStringLiteral("output")).toString());
    for (const QJsonValue &v : o.value(QStringLiteral("params")).toArray())
        if (v.isObject()) r.params.append(RecipeParam::fromJson(v.toObject()));
    return r;
}

std::optional<Recipe> Recipe::fromJsonString(const QString &json) {
    const QJsonDocument doc = QJsonDocument::fromJson(json.toUtf8());
    if (!doc.isObject()) return std::nullopt;
    return fromJson(doc.object());
}

// ---- Recipes ------------------------------------------------------------------------------------------

namespace Recipes {
namespace {
const QString KEY_PREFIX = QStringLiteral("external.recipes.recipe");
const QString LEGACY_PREFIX = QStringLiteral("external.tools.tool");
QList<Recipe> g_list;
// Stored recipes this build cannot parse: written back verbatim (after the
// parsed ones) so a save does not destroy them.
QStringList g_unparsable;
bool g_loaded = false;
}  // namespace

QList<Recipe> &getList() {
    if (!g_loaded) load();
    return g_list;
}

void load() {
    g_loaded = true;
    g_list.clear();
    g_unparsable.clear();
    for (int i = 1;; ++i) {
        const QString key = KEY_PREFIX + QString::number(i);
        if (!Prefs::contains(key)) break;
        const QString json = Prefs::getString(key, QString());
        const auto r = Recipe::fromJsonString(json);
        if (r)
            g_list.append(*r);
        else {
            Debug::debug(QStringLiteral("Could not parse recipe ") + key);
            g_unparsable.append(json);
        }
    }
    if (!g_list.isEmpty() || !g_unparsable.isEmpty()) return;
    // Legacy external.tools.toolN.* quadruples.
    for (int i = 1;; ++i) {
        const QString base = LEGACY_PREFIX + QString::number(i);
        const QString name = Prefs::getString(base + QStringLiteral(".name"), QString());
        const QString path = Prefs::getString(base + QStringLiteral(".path"), QString());
        const QString command = Prefs::getString(base + QStringLiteral(".command"), QString());
        if (name.isNull() || path.isNull() || command.isNull()) break;
        Recipe r(name);
        r.path = path;
        r.command = command;
        QString fmt = Prefs::getString(base + QStringLiteral(".format"), QString());
        fmt = fmt.mid(fmt.lastIndexOf(QLatin1Char('.')) + 1);
        SubFormatPtr f = Availabilities::formats().findFromName(fmt);
        if (!f) f = Availabilities::formats().findFromClassId(fmt);
        r.setFormat(f);
        g_list.append(r);
    }
}

void save() {
    for (int i = 0; i < g_list.size(); ++i) Prefs::set(KEY_PREFIX + QString::number(i + 1), g_list[i].toJsonString(false));
    for (int i = 0; i < g_unparsable.size(); ++i) Prefs::set(KEY_PREFIX + QString::number(g_list.size() + i + 1), g_unparsable[i]);
    Prefs::remove(KEY_PREFIX + QString::number(g_list.size() + g_unparsable.size() + 1));
    for (int i = 1;; ++i) {
        const QString base = LEGACY_PREFIX + QString::number(i);
        if (!Prefs::contains(base + QStringLiteral(".name")) && !Prefs::contains(base + QStringLiteral(".path"))) break;
        for (const char *s : {".name", ".path", ".command", ".format"}) Prefs::remove(base + QLatin1String(s));
    }
}

QString saveToFile(const Recipe &r, const QString &path) {
    QSaveFile f(path);
    if (!f.open(QIODevice::WriteOnly) || f.write(r.toJsonString(true).toUtf8()) < 0 || !f.commit()) return f.errorString();
    return QString();
}

QList<Recipe> loadFromFile(const QString &path, QString *error) {
    QList<Recipe> out;
    QFile f(path);
    if (!f.open(QIODevice::ReadOnly)) {
        if (error) *error = f.errorString();
        return out;
    }
    QJsonParseError perr;
    const QJsonDocument doc = QJsonDocument::fromJson(f.readAll(), &perr);
    if (doc.isObject())
        out.append(Recipe::fromJson(doc.object()));
    else if (doc.isArray()) {
        for (const QJsonValue &v : doc.array())
            if (v.isObject()) out.append(Recipe::fromJson(v.toObject()));
    } else if (error)
        *error = perr.errorString();
    return out;
}
}  // namespace Recipes

// ---- RecipeValues ----------------------------------------------------------------------------------------

namespace RecipeValues {
namespace {
const QString KEY = QStringLiteral("external.recipes.values");
QJsonObject root() {
    const QJsonDocument doc = QJsonDocument::fromJson(Prefs::getString(KEY, QStringLiteral("{}")).toUtf8());
    return doc.isObject() ? doc.object() : QJsonObject();
}
}  // namespace

QMap<QString, QString> get(const QString &recipeName) {
    QMap<QString, QString> out;
    const QJsonObject sub = root().value(recipeName).toObject();
    for (auto it = sub.constBegin(); it != sub.constEnd(); ++it)
        if (it.value().isString()) out.insert(it.key(), it.value().toString());
    return out;
}

void put(const QString &recipeName, const QMap<QString, QString> &values) {
    QJsonObject r = root();
    QJsonObject sub;
    for (auto it = values.constBegin(); it != values.constEnd(); ++it) sub.insert(it.key(), it.value().isNull() ? QString() : it.value());
    r.insert(recipeName, sub);
    Prefs::set(KEY, QString::fromUtf8(QJsonDocument(r).toJson(QJsonDocument::Compact)));
}
}  // namespace RecipeValues

// ---- RecipeCatalog ---------------------------------------------------------------------------------------

namespace RecipeCatalog {
const QString &url() {
    static const QString u = QStringLiteral("https://jubler.org/recipes.json");
    return u;
}

QString cachePath() {
    const QString dir = SystemDependent::getAppSupportDirPath() + QStringLiteral("/recipes");
    QDir().mkpath(dir);
    return dir + QStringLiteral("/catalog.json");
}

std::optional<QList<Recipe>> parse(const QByteArray &data) {
    const QJsonDocument doc = QJsonDocument::fromJson(data);
    if (!doc.isArray()) return std::nullopt;
    QList<Recipe> out;
    for (const QJsonValue &v : doc.array())
        if (v.isObject()) out.append(Recipe::fromJson(v.toObject()));
    return out;
}

std::optional<QList<Recipe>> cached() {
    QFile f(cachePath());
    if (!f.open(QIODevice::ReadOnly)) return std::nullopt;
    return parse(f.readAll());
}

std::optional<QList<Recipe>> fetch() {
    QNetworkAccessManager nam;
    QNetworkRequest req{QUrl(url())};
    req.setRawHeader("User-Agent", "Jubler");
    req.setTransferTimeout(5000);
    req.setAttribute(QNetworkRequest::RedirectPolicyAttribute, QNetworkRequest::NoLessSafeRedirectPolicy);
    QNetworkReply *reply = nam.get(req);
    QEventLoop loop;
    QObject::connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    loop.exec();
    const int status = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
    const QByteArray data = reply->readAll();
    const bool ok = reply->error() == QNetworkReply::NoError && status == 200;
    if (!ok) Debug::debug(QStringLiteral("Recipe catalog download failed: ") + reply->errorString());
    reply->deleteLater();
    if (!ok) return cached();
    const auto parsed = parse(data);
    if (!parsed) return cached();
    QSaveFile f(cachePath());
    if (f.open(QIODevice::WriteOnly)) {
        f.write(data);
        f.commit();
    }
    return parsed;
}
}  // namespace RecipeCatalog

// ---- RecipeResolver --------------------------------------------------------------------------------------

namespace RecipeResolver {
namespace {
std::mutex g_mutex;
QStringList g_loginPath;
bool g_probeStarted = false;

bool isWindows() { return SystemDependent::getAssetTag() == QLatin1String("win"); }

QStringList windowsCandidates(const QString &name) {
    QStringList exts = QProcessEnvironment::systemEnvironment().value(QStringLiteral("PATHEXT")).split(QLatin1Char(';'), Qt::SkipEmptyParts);
    if (exts.isEmpty()) exts = {QStringLiteral(".COM"), QStringLiteral(".EXE"), QStringLiteral(".BAT"), QStringLiteral(".CMD")};
    for (const QString &e : exts)
        if (name.endsWith(e, Qt::CaseInsensitive)) return {name};
    QStringList out;
    for (const QString &e : exts) out.append(name + e);
    out.append(name);
    return out;
}

QString executableAt(const QString &candidate) {
    const QFileInfo fi(candidate);
    return fi.isFile() && fi.isExecutable() ? fi.absoluteFilePath() : QString();
}
}  // namespace

void startLoginShellProbe() {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_probeStarted || isWindows() || SystemDependent::isFlatpak()) return;
    g_probeStarted = true;
    QThread *thread = QThread::create([]() {
        QString shell = QProcessEnvironment::systemEnvironment().value(QStringLiteral("SHELL"));
        if (shell.isEmpty()) shell = QStringLiteral("/bin/sh");
        QProcess p;
        p.start(shell, {QStringLiteral("-lc"), QStringLiteral("echo __JUBLER_PATH__:$PATH")});
        if (!p.waitForFinished(3000)) {
            p.kill();
            Debug::debug(QStringLiteral("Login shell PATH probe timed out"));
            return;
        }
        for (const QString &line : QString::fromLocal8Bit(p.readAllStandardOutput()).split(QLatin1Char('\n'))) {
            const int at = line.indexOf(QLatin1String("__JUBLER_PATH__:"));
            if (at < 0) continue;
            std::lock_guard<std::mutex> lock(g_mutex);
            g_loginPath = line.mid(at + 16).split(QDir::listSeparator(), Qt::SkipEmptyParts);
        }
    });
    QObject::connect(thread, &QThread::finished, thread, &QObject::deleteLater);
    thread->start();
}

QStringList searchDirs() {
    QStringList out;
    auto add = [&](const QString &d) { if (!d.isEmpty() && !out.contains(d)) out.append(d); };
    for (const QString &d : QProcessEnvironment::systemEnvironment().value(QStringLiteral("PATH")).split(QDir::listSeparator(), Qt::SkipEmptyParts)) add(d);
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        for (const QString &d : g_loginPath) add(d);
    }
    if (!isWindows()) {
        for (const char *d : {"/usr/local/bin", "/opt/homebrew/bin", "/opt/local/bin", "/usr/bin", "/bin"})
            if (QFileInfo(QLatin1String(d)).isDir()) add(QLatin1String(d));
        const QString local = QDir::homePath() + QStringLiteral("/.local/bin");
        if (QFileInfo(local).isDir()) add(local);
        const QDir py(QDir::homePath() + QStringLiteral("/Library/Python"));
        for (const QFileInfo &v : py.entryInfoList(QDir::Dirs | QDir::NoDotAndDotDot)) {
            const QString bin = v.filePath() + QStringLiteral("/bin");
            if (QFileInfo(bin).isDir()) add(bin);
        }
    }
    return out;
}

QString augmentedPath() { return searchDirs().join(QDir::listSeparator()); }

QString resolve(const QString &pathIn) {
    const QString path = pathIn.trimmed();
    if (path.isEmpty()) return QString();
    const bool direct = path.contains(QLatin1Char('/')) || path.contains(QLatin1Char('\\')) || QFileInfo(path).isAbsolute();
    if (direct) {
        if (isWindows()) {
            for (const QString &c : windowsCandidates(path)) {
                const QString hit = executableAt(c);
                if (!hit.isEmpty()) return hit;
            }
            return QString();
        }
        return executableAt(path);
    }
    for (const QString &dir : searchDirs()) {
        if (isWindows()) {
            for (const QString &c : windowsCandidates(path)) {
                const QString hit = executableAt(dir + QLatin1Char('/') + c);
                if (!hit.isEmpty()) return hit;
            }
        } else {
            const QString hit = executableAt(dir + QLatin1Char('/') + path);
            if (!hit.isEmpty()) return hit;
        }
    }
    return QString();
}

bool isAvailable(const Recipe &r) { return !resolve(r.path).isEmpty(); }
}  // namespace RecipeResolver

// ---- RecipeSecrets ---------------------------------------------------------------------------------------

namespace RecipeSecrets {
namespace {
Prompts g_prompts;
QString g_pin;
}  // namespace

void setPrompts(Prompts p) { g_prompts = std::move(p); }

bool anySecretStored() {
    for (const Recipe &r : Recipes::getList())
        if (r.hasSecretValue()) return true;
    return false;
}

QString resolvePin() {
    if (!g_pin.isEmpty()) return g_pin;
    const QString env = qEnvironmentVariable("JUBLER_PIN");
    if (!env.isEmpty()) {
        g_pin = env;
        return g_pin;
    }
    if (anySecretStored()) {
        if (g_prompts.askExisting) g_pin = g_prompts.askExisting();
    } else if (g_prompts.askNew)
        g_pin = g_prompts.askNew(false);
    return g_pin;
}

void forgetPin() { g_pin.clear(); }

QString encrypt(const QString &plain) {
    if (plain.isEmpty()) return QString(QLatin1String(""));
    const QString pin = resolvePin();
    if (pin.isEmpty()) return QString(QLatin1String(""));
    const auto blob = Encryption::encrypt(plain, pin);
    return blob ? *blob : QString(QLatin1String(""));
}

std::optional<QString> tryDecrypt(const QString &blob) {
    if (blob.isEmpty()) return QString(QLatin1String(""));
    const QString pin = resolvePin();
    if (pin.isEmpty()) return std::nullopt;
    const auto plain = Encryption::decrypt(blob, pin);
    if (!plain) forgetPin();
    return plain;
}

QString decrypt(const QString &blob) {
    const auto plain = tryDecrypt(blob);
    if (plain) return *plain;
    if (g_prompts.warn) g_prompts.warn(__("Wrong PIN."), false);
    return QString(QLatin1String(""));
}

bool recodeForSecretChange(RecipeParam &param, bool nowSecret) {
    if (param.defaultValue.isEmpty()) return true;
    if (nowSecret) {
        const QString blob = encrypt(param.defaultValue);
        if (blob.isEmpty()) return false;   // PIN cancelled: silent (Java)
        param.defaultValue = blob;
        return true;
    }
    const auto plain = tryDecrypt(param.defaultValue);
    if (!plain) {
        if (g_prompts.warn) g_prompts.warn(__("Cannot convert this value: wrong or missing PIN."), false);
        return false;
    }
    param.defaultValue = *plain;
    return true;
}

bool changePin() {
    QList<Recipe> &list = Recipes::getList();
    QList<std::pair<RecipeParam *, QString>> plains;
    if (anySecretStored()) {
        const QString pin = resolvePin();
        if (pin.isEmpty()) return false;
        for (Recipe &r : list)
            for (RecipeParam &p : r.params) {
                if (!p.isSecret() || p.defaultValue.isEmpty()) continue;
                const auto plain = Encryption::decrypt(p.defaultValue, pin);
                if (!plain) {
                    if (g_prompts.warn) g_prompts.warn(__("Wrong PIN."), true);
                    forgetPin();
                    return false;
                }
                plains.append({&p, *plain});
            }
    }
    const QString next = g_prompts.askNew ? g_prompts.askNew(true) : QString();
    if (next.isEmpty()) return false;
    g_pin = next;
    for (auto &[param, plain] : plains) {
        const auto blob = Encryption::encrypt(plain, next);
        param->defaultValue = blob ? *blob : QString();
    }
    Recipes::save();
    return true;
}
}  // namespace RecipeSecrets
