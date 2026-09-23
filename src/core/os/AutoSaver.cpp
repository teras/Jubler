/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/os/AutoSaver.h"

#include <QDir>
#include <QFileInfo>
#include <QRandomGenerator>
#include <QTimer>

#include "core/formats/SubFormat.h"
#include "core/os/Debug.h"
#include "core/os/FileCommunicator.h"
#include "core/os/SystemDependent.h"
#include "core/subs/SubFile.h"
#include "core/subs/Subtitles.h"

namespace AutoSaver {

const QString AUTOSAVEPREFIX = QStringLiteral("autosave.");

namespace {
QTimer *g_timer = nullptr;
Provider g_provider;

QString olds() { return directory() + QStringLiteral("/olds"); }

bool isAutosaveName(const QString &name) {
    return name.startsWith(AUTOSAVEPREFIX);
}

void deleteDirContents(const QString &dir) {
    for (const QFileInfo &fi : QDir(dir).entryInfoList(QDir::Files))
        QFile::remove(fi.absoluteFilePath());
}
}  // namespace

QString directory() {
    return SystemDependent::getAppSupportDirPath() + QStringLiteral("/autosave");
}

void launch(Provider provider) {
    g_provider = std::move(provider);
    if (g_timer) return;
    g_timer = new QTimer();
    g_timer->setInterval(AUTOSAVE_SECONDS * 1000);
    QObject::connect(g_timer, &QTimer::timeout, []() { saveNow(); });
    QTimer::singleShot(1000, []() { saveNow(); if (g_timer) g_timer->start(); });
}

void saveNow() {
    const QString dir = directory();
    QDir().mkpath(olds());
    if (!QFileInfo(dir).isDir() || !QFileInfo(dir).isWritable() || !QFileInfo(olds()).isDir() || !QFileInfo(olds()).isWritable()) {
        Debug::debug(QStringLiteral("ERROR: Could not use autosave directory ") + dir);
        return;
    }
    for (const QFileInfo &fi : QDir(dir).entryInfoList(QDir::Files))
        if (isAutosaveName(fi.fileName()))
            QFile::rename(fi.absoluteFilePath(), olds() + QLatin1Char('/') + fi.fileName());
    if (g_provider) {
        for (const Candidate &c : g_provider()) {
            if (!c.subs) continue;
            Subtitles clone(*c.subs);
            // "<prefix>XXXX.<file name>.ass": Advanced SubStation in UTF-8 as the
            // Java (every format's content survives it); the recovery gives the
            // document back its name and format.
            const QString name = QStringLiteral("%1%2.%3").arg(AUTOSAVEPREFIX).arg(QRandomGenerator::global()->bounded(0x10000), 4, 16, QLatin1Char('0')).arg(c.fileName);
            const SubFormatPtr ass = Availabilities::formats().findFromName(QStringLiteral("AdvancedSubStation"));
            SubFile sf(QStringLiteral("UTF-8"), c.subs->getSubFile().getFPS(), ass ? ass->newInstance() : c.subs->getSubFile().getFormat(),
                       dir + QLatin1Char('/') + name, SubFile::EXTENSION_OMMITED);
            sf.updateFileByType();
            const QString err = FileCommunicator::save(clone, sf, nullptr);
            if (!err.isNull())
                Debug::debug(QStringLiteral("Autosave failed: ") + err);
        }
    }
    deleteDirContents(olds());
    QDir().rmdir(olds());
}

QStringList getAutoSaveListOnLoad() {
    const QString dir = directory();
    if (!QFileInfo(dir).isDir() || !QFileInfo(dir).isReadable())
        return {};
    for (const QFileInfo &fi : QDir(olds()).entryInfoList(QDir::Files))
        QFile::rename(fi.absoluteFilePath(), dir + QLatin1Char('/') + fi.fileName());
    deleteDirContents(olds());
    QDir().rmdir(olds());
    QStringList out;
    for (const QFileInfo &fi : QDir(dir).entryInfoList(QDir::Files))
        if (fi.fileName().startsWith(AUTOSAVEPREFIX))
            out.append(fi.absoluteFilePath());
    return out;
}

QString keepUnrecovered(const QString &path) {
    const QString dir = directory() + QStringLiteral("/unrecovered");
    QDir().mkpath(dir);
    const QString target = dir + QLatin1Char('/') + QFileInfo(path).fileName();
    QFile::remove(target);
    if (!QFile::rename(path, target)) return path;
    Debug::debug(QStringLiteral("Autosave file could not be recovered, kept as ") + target);
    return target;
}

void cleanup() {
    if (g_timer) {
        g_timer->stop();
        delete g_timer;
        g_timer = nullptr;
    }
    deleteDirContents(olds());
    deleteDirContents(directory());
    QDir().rmdir(olds());
}

}  // namespace AutoSaver
