/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/os/SystemDependent.h"

#include <QDir>
#include <QFile>
#include <QFileInfo>
#include <QProcessEnvironment>
#include <QStandardPaths>

#ifdef Q_OS_LINUX
#include <sys/xattr.h>
#endif

namespace SystemDependent {

bool isLinux() {
#ifdef Q_OS_LINUX
    return true;
#else
    return false;
#endif
}

bool isWindows() {
#ifdef Q_OS_WIN
    return true;
#else
    return false;
#endif
}

bool isMacOS() {
#ifdef Q_OS_MACOS
    return true;
#else
    return false;
#endif
}

bool isFlatpak() {
    static const bool fp = isLinux() && QFile::exists(QStringLiteral("/.flatpak-info"));
    return fp;
}

QString hostPath(const QString &path) {
#ifdef Q_OS_LINUX
    if (!isFlatpak() || path.isEmpty()) return QString();
    const QByteArray p = QFile::encodeName(path);
    char buf[4096];
    const ssize_t n = getxattr(p.constData(), "user.document-portal.host-path", buf, sizeof(buf));
    if (n <= 0) return QString();
    return QString::fromUtf8(buf, int(n));
#else
    Q_UNUSED(path);
    return QString();
#endif
}

bool isPortalGranted(const QString &path) {
    return !hostPath(path).isNull();
}

QString displayPath(const QString &path) {
    if (path.isEmpty()) return QString(QLatin1String(""));
    const QString host = hostPath(path);
    if (!host.isNull()) return host;
    if (isFlatpak()) return QFileInfo(path).fileName();
    return path;
}

bool shouldSupportChangeScaling() { return !isMacOS(); }

QKeySequence getUpDownKeystroke(bool up) {
    if (isMacOS())
        return QKeySequence(Qt::CTRL | Qt::ALT | (up ? Qt::Key_Up : Qt::Key_Down));
    return QKeySequence(Qt::CTRL | (up ? Qt::Key_Up : Qt::Key_Down));
}

QString getKeyMods(Qt::KeyboardModifiers mods, bool withBraces) {
    QStringList parts;
    auto add = [&](const QString &mac, const QString &other) {
        const QString s = isMacOS() ? mac : other;
        parts.append(withBraces ? QLatin1Char('[') + s + QLatin1Char(']') : s);
    };
    if (mods & Qt::MetaModifier) add(QStringLiteral("⌘"), QStringLiteral("Meta"));
    if (mods & Qt::AltModifier) add(QStringLiteral("⌥"), QStringLiteral("Alt"));
    if (mods & Qt::ControlModifier) add(QStringLiteral("⌃"), QStringLiteral("Ctrl"));
    if (mods & Qt::ShiftModifier) add(QStringLiteral("⇧"), QStringLiteral("Shift"));
    return parts.join(QLatin1Char('+'));
}

bool canWrite(const QString &path) {
    const QFileInfo fi(path);
    if (!isWindows()) {
        if (fi.exists())
            return fi.isWritable();
        return QFileInfo(fi.absolutePath()).isWritable();
    }
    // Windows permission bits lie (ACLs, protected folders): try it for real.
    if (fi.isFile()) {
        const QString probe = path + QStringLiteral(".canWrite");
        if (!QFile::rename(path, probe))
            return false;
        QFile::rename(probe, path);
        return true;
    }
    QFile probe(fi.isDir() ? path + QStringLiteral("/canWrite") : path);
    if (!probe.open(QIODevice::WriteOnly))
        return false;
    const bool ok = probe.write(" ", 1) == 1;
    probe.close();
    probe.remove();
    return ok;
}

QString getAppSupportDirPath() {
    if (isWindows()) {
        const QString appdata = QProcessEnvironment::systemEnvironment().value(QStringLiteral("APPDATA"));
        return (appdata.isEmpty() ? QDir::homePath() : appdata) + QStringLiteral("/Jubler");
    }
    if (isMacOS())
        return QDir::homePath() + QStringLiteral("/Library/Application Support/Jubler");
    const QString xdg = QProcessEnvironment::systemEnvironment().value(QStringLiteral("XDG_DATA_HOME"));
    if (isFlatpak() && !xdg.trimmed().isEmpty())
        return xdg + QStringLiteral("/jubler");
    return QDir::homePath() + QStringLiteral("/.local/share/jubler");
}

QString getCacheDirPath() {
    const QProcessEnvironment env = QProcessEnvironment::systemEnvironment();
    if (isWindows()) {
        const QString local = env.value(QStringLiteral("LOCALAPPDATA"));
        return (local.isEmpty() ? getAppSupportDirPath() : local + QStringLiteral("/Jubler")) + QStringLiteral("/cache");
    }
    if (isMacOS())
        return QDir::homePath() + QStringLiteral("/Library/Caches/Jubler");
    const QString xdg = env.value(QStringLiteral("XDG_CACHE_HOME")).trimmed();
    if (!xdg.isEmpty() && QDir::isAbsolutePath(xdg))
        return xdg + QStringLiteral("/jubler");
    return QDir::homePath() + QStringLiteral("/.cache/jubler");
}

QString getConfigDirPath() {
    if (isWindows() || isMacOS())
        return getAppSupportDirPath();
    const QString xdg = QProcessEnvironment::systemEnvironment().value(QStringLiteral("XDG_CONFIG_HOME")).trimmed();
    if (!xdg.isEmpty() && QDir::isAbsolutePath(xdg))
        return xdg + QStringLiteral("/jubler");
    return QDir::homePath() + QStringLiteral("/.config/jubler");
}

QString getAssetExtension() { return isMacOS() ? QStringLiteral(".dmg") : isWindows() ? QStringLiteral(".exe") : QStringLiteral(".appimage"); }
QString getAssetTag() { return isMacOS() ? QStringLiteral("mac") : isWindows() ? QStringLiteral("win") : QStringLiteral("linux"); }

}  // namespace SystemDependent
