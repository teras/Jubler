/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/DesktopIntegration.h"

#include <QCryptographicHash>
#include <QDir>
#include <QFile>
#include <QFileInfo>
#include <QImage>
#include <QPainter>
#include <QProcess>
#include <QStandardPaths>
#include <QSvgRenderer>
#include <QUrl>
#include <QtConcurrent/QtConcurrentRun>

#include "core/os/Debug.h"
#include "core/os/SystemDependent.h"

namespace DesktopIntegration {

namespace {

// The name the Java release registered under: this entry replaces its entry.
const QString BASENAME = QStringLiteral("jubler");

QImage logo(int size) {
    QImage img(size, size, QImage::Format_ARGB32);
    img.fill(Qt::transparent);
    QPainter p(&img);
    QSvgRenderer(QStringLiteral(":/logo/logo.svg")).render(&p);
    return img;
}

// A desktop entry Exec argument: quoted when it holds anything the format reserves.
QString execArgument(const QString &path) {
    static const QString reserved = QStringLiteral(" \t\n\"'\\><~|&;$*?#()`");
    bool plain = true;
    for (QChar c : path)
        if (reserved.contains(c)) plain = false;
    if (plain) return path;
    QString quoted = path;
    for (const char *c : {"\\", "\"", "`", "$"}) quoted.replace(QLatin1String(c), QLatin1Char('\\') + QLatin1String(c));
    return QLatin1Char('"') + quoted + QLatin1Char('"');
}

void run(const QString &tool, const QStringList &args) {
    if (QStandardPaths::findExecutable(tool).isEmpty()) return;
    QProcess p;
    p.start(tool, args);
    if (!p.waitForFinished(10000)) p.kill();
}

void registerNow(const QString &appImage) {
    const QString data = QStandardPaths::writableLocation(QStandardPaths::GenericDataLocation);
    const QString cache = QStandardPaths::writableLocation(QStandardPaths::GenericCacheLocation);
    if (data.isEmpty()) return;

    // The icons, in the user's hicolor theme.
    QString icon128;
    for (int size : {32, 64, 128}) {
        const QString dir = QStringLiteral("%1/icons/hicolor/%2x%2/apps").arg(data).arg(size);
        const QString file = dir + QLatin1Char('/') + BASENAME + QStringLiteral(".png");
        if (!QDir().mkpath(dir) || !logo(size).save(file, "PNG")) {
            Debug::debug(QStringLiteral("Desktop integration: cannot write the icon ") + file);
            return;
        }
        if (size == 128) icon128 = file;
    }

    // The menu entry.
    // Exec is a command line (quoted); TryExec a plain path (string escapes only).
    QString tryExec = appImage;
    tryExec.replace(QLatin1Char('\\'), QStringLiteral("\\\\")).replace(QLatin1Char('\n'), QStringLiteral("\\n"));
    const QString entry = QStringLiteral("[Desktop Entry]\n"
                                         "Type=Application\n"
                                         "Name=Jubler\n"
                                         "Exec=%1 %U\n"
                                         "TryExec=%2\n"
                                         "Categories=AudioVideo;AudioVideoEditing;\n"
                                         "Comment=Jubler is a tool to edit text-based subtitles\n"
                                         "Icon=%3\n"
                                         "StartupWMClass=Jubler\n")
                              .arg(execArgument(appImage), tryExec, BASENAME);
    const QString dir = data + QStringLiteral("/applications");
    const QString path = dir + QLatin1Char('/') + BASENAME + QStringLiteral(".desktop");
    QFile old(path);
    const bool same = old.open(QIODevice::ReadOnly) && QString::fromUtf8(old.readAll()) == entry;
    old.close();
    if (!same) {
        QFile f(path);
        if (!QDir().mkpath(dir) || !f.open(QIODevice::WriteOnly | QIODevice::Truncate) || f.write(entry.toUtf8()) < 0) {
            Debug::debug(QStringLiteral("Desktop integration: cannot write ") + path);
            return;
        }
        f.close();
        f.setPermissions(f.permissions() | QFileDevice::ExeOwner | QFileDevice::ExeGroup | QFileDevice::ExeOther);
    }

    // The AppImage file itself shows the logo in file managers: its GIO icon
    // and its thumbnail (freedesktop thumbnail specification, "normal" size).
    const QString uri = QString::fromUtf8(QUrl::fromLocalFile(appImage).toEncoded());
    run(QStringLiteral("gio"), {QStringLiteral("set"), QStringLiteral("-t"), QStringLiteral("string"), appImage,
                                QStringLiteral("metadata::custom-icon"), QString::fromUtf8(QUrl::fromLocalFile(icon128).toEncoded())});
    if (!cache.isEmpty()) {
        const QString thumbs = cache + QStringLiteral("/thumbnails/normal");
        QImage thumb = logo(128);
        thumb.setText(QStringLiteral("Thumb::URI"), uri);
        thumb.setText(QStringLiteral("Thumb::MTime"), QString::number(QFileInfo(appImage).lastModified().toSecsSinceEpoch()));
        const QString name = QString::fromLatin1(QCryptographicHash::hash(uri.toUtf8(), QCryptographicHash::Md5).toHex());
        if (QDir().mkpath(thumbs)) thumb.save(thumbs + QLatin1Char('/') + name + QStringLiteral(".png"), "PNG");
    }

    if (!same) run(QStringLiteral("xdg-desktop-menu"), {QStringLiteral("forceupdate")});
}

}  // namespace

void registerAppImage() {
#ifdef Q_OS_LINUX
    if (SystemDependent::isFlatpak()) return;
    const QString appImage = qEnvironmentVariable("APPIMAGE");
    if (appImage.isEmpty() || !QFileInfo(appImage).isFile()) return;
    (void)QtConcurrent::run([appImage]() { registerNow(QFileInfo(appImage).absoluteFilePath()); });
#endif
}

}  // namespace DesktopIntegration
