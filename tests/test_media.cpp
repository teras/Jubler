/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

// The recent-file records of media (packed SubFile) and the cache of data
// derived from a media file (waveform peaks, embedded subtitles).
#include "TestSupport.h"

#include <QDateTime>
#include <QDir>
#include <QFile>
#include <QCryptographicHash>
#include <QTemporaryDir>

#include "core/media/MediaCache.h"
#include "core/subs/SubFile.h"

static void testPackPlainFile() {
    SubFile sf(QStringLiteral("/tmp/a b.srt"), SubFile::EXTENSION_GIVEN);
    sf.setEncoding(QStringLiteral("UTF-8"));
    sf.setFPS(25);
    CHECK_EQ(sf.getPacked(), QStringLiteral(";UTF-8;25.0;/tmp/a b.srt"), "the Java's packed form is unchanged");
    SubFile back;
    CHECK(SubFile::unpack(sf.getPacked(), back), "a packed file parses");
    CHECK_EQ(back.getSaveFile(), sf.getSaveFile(), "same path");
    CHECK_EQ(back.getEncoding(), QStringLiteral("UTF-8"), "same encoding");
    CHECK_EQ(back.getEmbeddedStream(), -1, "an ordinary file has no stream");
}

static void testPackJavaAndBareForms() {
    SubFile sf;
    CHECK(SubFile::unpack(QStringLiteral(";UTF-8;25.0;/home/a/film.srt"), sf), "a Java entry parses");
    CHECK_EQ(sf.getSaveFile(), QStringLiteral("/home/a/film.srt"), "Java entry: path");
    CHECK_EQ(sf.getEmbeddedStream(), -1, "Java entry: no stream");
    CHECK(SubFile::unpack(QStringLiteral("/home/a/film.srt"), sf), "a bare path parses");
    CHECK_EQ(sf.getSaveFile(), QStringLiteral("/home/a/film.srt"), "bare path");
    CHECK(!SubFile::unpack(QString(), sf), "empty: no entry");
    CHECK(!SubFile::unpack(QStringLiteral(";UTF-8;/home/a/film.srt"), sf), "a missing field: no entry");
    CHECK(!SubFile::unpack(QStringLiteral("E;UTF-8;25;ell;/film.mkv"), sf), "an embedded entry without a stream: no entry");
    CHECK(!SubFile::unpack(QStringLiteral("E;UTF-8;25;x;ell;/film.mkv"), sf), "a stream that is not a number: no entry");
}

static void testPackEmbedded() {
    SubFile sf(QStringLiteral("/home/a/film;2.mkv"), SubFile::EXTENSION_GIVEN);   // a path may hold semicolons
    sf.setEncoding(QStringLiteral("UTF-8"));
    sf.setFPS(25);
    sf.setEmbedded(3, QStringLiteral("ell"));
    CHECK_EQ(sf.getPacked(), QStringLiteral("E;UTF-8;25.0;3;ell;/home/a/film;2.mkv"), "the embedded form is marked");
    SubFile back;
    CHECK(SubFile::unpack(sf.getPacked(), back), "an embedded entry parses");
    CHECK_EQ(back.getSaveFile(), sf.getSaveFile(), "the path is the rest of the line");
    CHECK_EQ(back.getEmbeddedStream(), 3, "same stream");
    CHECK_EQ(back.getEmbeddedLanguage(), QStringLiteral("ell"), "same language");
    CHECK(sf == back, "an entry equals its own round trip");

    SubFile other = sf;
    other.setEmbedded(4, QStringLiteral("eng"));
    CHECK(!(sf == other), "two streams of one file are two entries");

    SubFile noLanguage(QStringLiteral("/film.mkv"), SubFile::EXTENSION_GIVEN);
    noLanguage.setEmbedded(0, QString());
    CHECK(SubFile::unpack(noLanguage.getPacked(), back), "a stream without a language parses");
    CHECK_EQ(back.getEmbeddedStream(), 0, "stream 0 is a stream");
    CHECK(back.getEmbeddedLanguage().isEmpty(), "no language");

    SubFile odd(QStringLiteral("/film.mkv"), SubFile::EXTENSION_GIVEN);
    odd.setEmbedded(1, QStringLiteral("gr;ec"));   // a container writes what it likes
    CHECK(SubFile::unpack(odd.getPacked(), back), "a language with a semicolon parses");
    CHECK_EQ(back.getSaveFile(), QStringLiteral("/film.mkv"), "the path survives it");
}

static void testMediaCache() {
    QTemporaryDir tmp;
    CHECK(tmp.isValid(), "temporary directory");
    const QString media = tmp.path() + QStringLiteral("/film.mkv");
    QFile f(media);
    CHECK(f.open(QIODevice::WriteOnly), "media file");
    f.write(QByteArray(1000, 'x'));
    f.close();

    const QString id = MediaCache::identity(media);
    CHECK_EQ(id, MediaCache::identity(media), "the same media: the same identity");
    const QString a = MediaCache::file(QStringLiteral("subs"), media, QStringLiteral("3"), 1, QStringLiteral(".jsub"));
    // The entries written before the identity was factored out keep their names.
    CHECK_EQ(a, MediaCache::dir(QStringLiteral("subs")) + QLatin1Char('/')
                    + QString::fromLatin1(QCryptographicHash::hash((id + QStringLiteral("|3|1")).toUtf8(), QCryptographicHash::Sha1).toHex())
                    + QStringLiteral(".jsub"),
             "the entry name is the hash of identity|extra|version");
    CHECK_EQ(a, MediaCache::file(QStringLiteral("subs"), media, QStringLiteral("3"), 1, QStringLiteral(".jsub")), "the same media: the same entry");
    CHECK(a.startsWith(MediaCache::dir(QStringLiteral("subs"))), "inside the folder of its kind");
    CHECK(a.endsWith(QStringLiteral(".jsub")), "keeps the suffix");
    CHECK(a != MediaCache::file(QStringLiteral("subs"), media, QStringLiteral("4"), 1, QStringLiteral(".jsub")), "another stream: another entry");
    CHECK(a != MediaCache::file(QStringLiteral("subs"), media, QStringLiteral("3"), 2, QStringLiteral(".jsub")), "another version: another entry");
    CHECK(a != MediaCache::file(QStringLiteral("peaks"), media, QStringLiteral("3"), 1, QStringLiteral(".jsub")), "another kind: another entry");

    CHECK(f.open(QIODevice::WriteOnly | QIODevice::Append), "media file grows");
    f.write(QByteArray(10, 'x'));
    f.close();
    CHECK(a != MediaCache::file(QStringLiteral("subs"), media, QStringLiteral("3"), 1, QStringLiteral(".jsub")), "a changed media: another entry");
    CHECK(id != MediaCache::identity(media), "a changed media: another identity");

    // Rewritten with the same size: only the modification time tells.
    const QString grown = MediaCache::identity(media);
    CHECK(f.open(QIODevice::ReadWrite), "media file rewritten");
    f.write(QByteArray(10, 'y'));
    f.flush();   // the write would stamp the time again on close
    f.setFileTime(f.fileTime(QFileDevice::FileModificationTime).addSecs(60), QFileDevice::FileModificationTime);
    f.close();
    CHECK(grown != MediaCache::identity(media), "same size, another time: another identity");
}

static void testMediaCachePrune() {
    // The cache folder of this test run (the prefs store is a throw-away one,
    // but the cache directory is the user's: an own kind, removed afterwards).
    const QString kind = QStringLiteral("test-prune-%1").arg(QCoreApplication::applicationPid());
    const QString dir = MediaCache::dir(kind);
    CHECK(QDir().mkpath(dir), "cache folder");
    const QString suffix = QStringLiteral(".bin");
    QStringList names;
    for (int i = 0; i < 4; ++i) {
        const QString name = dir + QStringLiteral("/entry%1").arg(i) + suffix;
        QFile e(name);
        CHECK(e.open(QIODevice::WriteOnly), "cache entry");
        e.write(QByteArray(1000, 'x'));
        e.close();
        // The time is set on a second, empty open: writing sets it again when
        // the file is closed. entry0 is then the least recently used.
        QFile t(name);
        CHECK(t.open(QIODevice::ReadWrite), "cache entry again");
        CHECK(t.setFileTime(QDateTime::currentDateTime().addSecs(i - 10), QFileDevice::FileModificationTime), "entry time");
        t.close();
        names.append(name);
    }
    MediaCache::touch(names.first());   // reading it makes it the newest
    MediaCache::prune(kind, suffix, 2500);
    CHECK(QFile::exists(names.at(0)), "the touched entry stays");
    CHECK(!QFile::exists(names.at(1)), "the oldest used entry goes");
    CHECK(QFile::exists(names.at(3)), "the newest entry stays");
    MediaCache::prune(kind, suffix, 0);
    CHECK(!QFile::exists(names.at(3)), "nothing survives a zero limit");

    // What a killed QSaveFile leaves behind: the abandoned one goes, a recent
    // one may be the temporary of a write going on right now.
    const QString abandoned = dir + QStringLiteral("/entry8") + suffix + QStringLiteral(".AbCdEf");
    const QString writing = dir + QStringLiteral("/entry9") + suffix + QStringLiteral(".GhIjKl");
    for (const QString &name : {abandoned, writing}) {
        QFile e(name);
        CHECK(e.open(QIODevice::WriteOnly), "leftover temporary");
        e.write(QByteArray(1000, 'x'));
        e.close();
    }
    QFile old(abandoned);
    CHECK(old.open(QIODevice::ReadWrite), "leftover temporary again");
    CHECK(old.setFileTime(QDateTime::currentDateTime().addSecs(-7200), QFileDevice::FileModificationTime), "leftover time");
    old.close();
    MediaCache::prune(kind, suffix, 0);
    CHECK(!QFile::exists(abandoned), "an abandoned temporary goes");
    CHECK(QFile::exists(writing), "a temporary being written stays");
    QDir(dir).removeRecursively();
}

int main(int argc, char **argv) {
    QCoreApplication app(argc, argv);
    testInitPrefs();
    testPackPlainFile();
    testPackJavaAndBareForms();
    testPackEmbedded();
    testMediaCache();
    testMediaCachePrune();
    return testFinish("test_media");
}
