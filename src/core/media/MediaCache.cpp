/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/media/MediaCache.h"

#include <QCryptographicHash>
#include <QDateTime>
#include <QDir>
#include <QFile>
#include <QFileInfo>

#include <algorithm>

#include "core/os/SystemDependent.h"

namespace MediaCache {

QString dir(const QString &kind) {
    return SystemDependent::getCacheDirPath() + QLatin1Char('/') + kind;
}

QString identity(const QString &media) {
    const QFileInfo fi(media);
    return fi.absoluteFilePath() + QLatin1Char('|') + QString::number(fi.size()) + QLatin1Char('|')
           + QString::number(fi.lastModified().toMSecsSinceEpoch());
}

QString file(const QString &kind, const QString &media, const QString &extra, int version, const QString &suffix) {
    const QString id = identity(media) + QLatin1Char('|') + extra + QLatin1Char('|') + QString::number(version);
    return dir(kind) + QLatin1Char('/') + QString::fromLatin1(QCryptographicHash::hash(id.toUtf8(), QCryptographicHash::Sha1).toHex()) + suffix;
}

void touch(const QString &file) {
    QFile f(file);
    if (f.open(QIODevice::ReadWrite)) f.setFileTime(QDateTime::currentDateTime(), QFileDevice::FileModificationTime);
}

void prune(const QString &kind, const QString &suffix, qint64 limit) {
    const QDir folder(dir(kind));
    // QSaveFile writes to "<name>.XXXXXX" and renames on commit; killed in between, it leaves that one behind and
    // nothing else would ever remove it. Only the old ones: a recent one may be a write going on right now.
    const QDateTime stale = QDateTime::currentDateTime().addSecs(-3600);
    for (const QFileInfo &f : folder.entryInfoList({QLatin1Char('*') + suffix + QLatin1String(".*")}, QDir::Files))
        if (f.lastModified() < stale) QFile::remove(f.filePath());

    QFileInfoList files = folder.entryInfoList({QLatin1Char('*') + suffix}, QDir::Files);
    // Least recently used first (QDir's own time sorting is newest first).
    std::sort(files.begin(), files.end(), [](const QFileInfo &a, const QFileInfo &b) { return a.lastModified() < b.lastModified(); });
    qint64 total = 0;
    for (const QFileInfo &f : files) total += f.size();
    for (const QFileInfo &f : files) {
        if (total <= limit) break;
        total -= f.size();
        QFile::remove(f.filePath());
    }
}

}  // namespace MediaCache
