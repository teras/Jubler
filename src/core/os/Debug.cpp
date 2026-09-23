/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/os/Debug.h"

#include <QDateTime>
#include <QDir>
#include <QFile>
#include <QTextStream>
#include <cstdio>
#include <mutex>

#include "core/os/SystemDependent.h"

namespace Debug {

namespace {
QFile *g_log = nullptr;
}

QString logFilePath() {
    if (SystemDependent::isMacOS())
        return QDir::homePath() + QStringLiteral("/Library/Logs/Jubler.log");
    return SystemDependent::getAppSupportDirPath() + (SystemDependent::isWindows() ? QStringLiteral("/log.txt") : QStringLiteral("/Jubler.log"));
}

void init() {
    const QString path = logFilePath();
    QDir().mkpath(QFileInfo(path).path());
    if (QFile::exists(path)) {
        QFile::remove(path + QStringLiteral(".1"));
        QFile::rename(path, path + QStringLiteral(".1"));
    }
    g_log = new QFile(path);
    if (!g_log->open(QIODevice::WriteOnly | QIODevice::Text)) {
        delete g_log;
        g_log = nullptr;
    }
}

void debug(const QString &msg) {
    const QString line = QDateTime::currentDateTime().toString(Qt::ISODateWithMs) + QLatin1Char(' ') + msg;
    static std::mutex mutex;   // worker threads and FFmpeg's own threads log too
    std::lock_guard<std::mutex> lock(mutex);
    std::fprintf(stderr, "%s\n", line.toUtf8().constData());
    if (g_log) {
        QTextStream out(g_log);
        out << line << '\n';
        out.flush();
    }
}

void debug(const std::exception &e) {
    debug(QString::fromUtf8(e.what()));
}

}  // namespace Debug
