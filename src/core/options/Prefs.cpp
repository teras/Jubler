/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/i18n/I18N.h"
#include "core/options/Prefs.h"

#include "core/options/JavaPrefs.h"
#include "core/os/SystemDependent.h"

#include <QCoreApplication>
#include <QFile>
#include <QStandardPaths>
#include <QFileInfo>
#include <QDir>
#include <QRegularExpression>
#include <QSettings>
#include <QTextStream>

#include <charconv>

#include <memory>

namespace Prefs {

namespace {
std::unique_ptr<QSettings> g_settings;
QString g_override;

QSettings &store() {
    if (!g_settings) {
        QString path = g_override;
        if (path.isEmpty()) {
            path = SystemDependent::getConfigDirPath() + QStringLiteral("/prefs.ini");
            // Early port builds kept it under a vendor folder: move it once.
            if (!QFile::exists(path)) {
                const QString old = QStandardPaths::writableLocation(QStandardPaths::GenericConfigLocation)
                                    + QStringLiteral("/Panayotis/Jubler/prefs.ini");
                if (QFile::exists(old) && QDir().mkpath(QFileInfo(path).path())) QFile::copy(old, path);
            }
        }
        g_settings = std::make_unique<QSettings>(path, QSettings::IniFormat);
    }
    return *g_settings;
}
}  // namespace

void useStore(const QString &path) {
    g_override = path;
    g_settings.reset();
}

bool storeExists() {
    return QFile::exists(storePath());
}

QString storePath() {
    return store().fileName();
}

QString floatString(float value) {
    char buf[32];
    const auto r = std::to_chars(buf, buf + sizeof(buf), value);
    QString s = QString::fromLatin1(buf, int(r.ptr - buf));
    if (!s.contains(QLatin1Char('.')) && !s.contains(QLatin1Char('e')) && !s.contains(QLatin1String("inf")) && !s.contains(QLatin1String("nan")))
        s += QLatin1String(".0");
    return s;
}

void set(const QString &key, float value) {
    store().setValue(key, floatString(value));
    store().sync();
}

void set(const QString &key, int value) {
    store().setValue(key, value);
    store().sync();
}

void set(const QString &key, bool value) {
    store().setValue(key, value ? QStringLiteral("true") : QStringLiteral("false"));
    store().sync();
}

void set(const QString &key, const QString &value) {
    if (value.isNull())
        store().remove(key);
    else
        store().setValue(key, value);
    store().sync();
}

void remove(const QString &key) {
    store().remove(key);
    store().sync();
}

int getInt(const QString &key, int deflt) {
    bool ok = false;
    const int v = store().value(key).toString().toInt(&ok);
    return ok ? v : deflt;
}

QString getString(const QString &key, const QString &deflt) {
    const QVariant v = store().value(key);
    return v.isValid() ? v.toString() : deflt;
}

bool getBoolean(const QString &key, bool deflt) {
    // java.util.prefs: "true"/"false" (case-insensitive), anything else → default.
    const QString v = store().value(key).toString();
    if (v.compare(QLatin1String("true"), Qt::CaseInsensitive) == 0) return true;
    if (v.compare(QLatin1String("false"), Qt::CaseInsensitive) == 0) return false;
    return deflt;
}

float getFloat(const QString &key, float deflt) {
    bool ok = false;
    const float v = store().value(key).toString().toFloat(&ok);
    return ok ? v : deflt;
}

bool contains(const QString &key) {
    return store().contains(key);
}

QStringList keys() {
    return store().allKeys();
}

QString exportPrefs(const QString &path) {
    QSettings out(path, QSettings::IniFormat);
    out.clear();
    for (const QString &k : store().allKeys())
        out.setValue(k, store().value(k));
    out.sync();
    if (out.status() != QSettings::NoError)
        return __("Cannot write {0}", path);
    if (!QFileInfo::exists(path)) {   // QSettings writes nothing for an empty store
        QFile empty(path);
        if (!empty.open(QIODevice::WriteOnly))
            return __("Cannot write {0}", path);
    }
    return QString();
}

QString importPrefs(const QString &path) {
    QFile f(path);
    if (!f.open(QIODevice::ReadOnly))
        return __("File not found: {0}", path);
    QByteArray head = f.read(256);
    if (head.startsWith("\xEF\xBB\xBF")) head.remove(0, 3);   // a UTF-8 BOM (an edited export)
    if (head.trimmed().startsWith('<')) {
        f.seek(0);
        QString err;
        const QMap<QString, QString> java = JavaPrefs::parseXml(f.readAll(), &err);
        if (java.isEmpty())
            return err;
        JavaPrefs::apply(java);
        return QString();
    }
    f.close();
    QSettings in(path, QSettings::IniFormat);
    if (in.status() != QSettings::NoError)
        return __("Cannot read {0}", path);
    // QSettings reads anything; only an export (dotted Jubler keys) may replace the store.
    static const QRegularExpression keyRe(QStringLiteral("^[A-Za-z][A-Za-z0-9_\\-]*(\\.[A-Za-z0-9_\\-]+)+$"));
    const QStringList keys = in.allKeys();
    bool valid = !keys.isEmpty();
    for (const QString &k : keys)
        if (!keyRe.match(k).hasMatch()) valid = false;
    if (!valid)
        return __("Not a Jubler preferences file: {0}", path);
    store().clear();
    for (const QString &k : in.allKeys())
        store().setValue(k, in.value(k));
    store().sync();
    return QString();
}

QString resetPrefs() {
    store().clear();
    store().sync();
    return QString();
}

QMap<QString, QVariant> snapshot() {
    QMap<QString, QVariant> out;
    for (const QString &k : store().allKeys()) out.insert(k, store().value(k));
    return out;
}

void restore(const QMap<QString, QVariant> &snapshot) {
    store().clear();
    for (auto it = snapshot.constBegin(); it != snapshot.constEnd(); ++it) store().setValue(it.key(), it.value());
    store().sync();
}

void dump() {
    QTextStream out(stdout);
    for (const QString &k : store().allKeys())
        out << k << " = " << store().value(k).toString() << '\n';
}

}  // namespace Prefs
