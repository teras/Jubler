/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

// Minimal assertion helpers shared by the test executables (no framework).
#include <QCoreApplication>
#include <QFile>
#include <QString>
#include <QTextStream>
#include <cstdio>

#include "core/options/Prefs.h"

static int g_failures = 0;

#define CHECK(cond, msg)                                                          \
    do {                                                                          \
        if (!(cond)) {                                                            \
            std::fprintf(stderr, "FAIL %s:%d: %s\n", __FILE__, __LINE__, msg);    \
            ++g_failures;                                                         \
        }                                                                         \
    } while (0)

#define CHECK_EQ(a, b, msg)                                                       \
    do {                                                                          \
        const auto _a = (a);                                                      \
        const auto _b = (b);                                                      \
        if (!(_a == _b)) {                                                        \
            std::fprintf(stderr, "FAIL %s:%d: %s\n   got:      %s\n   expected: %s\n", \
                         __FILE__, __LINE__, msg, testStr(_a).toUtf8().constData(), \
                         testStr(_b).toUtf8().constData());                       \
            ++g_failures;                                                         \
        }                                                                         \
    } while (0)

inline QString testStr(const QString &s) { return s; }
inline QString testStr(const char *s) { return QString::fromUtf8(s); }
inline QString testStr(int v) { return QString::number(v); }
inline QString testStr(long long v) { return QString::number(v); }
inline QString testStr(double v) { return QString::number(v, 'g', 12); }
inline QString testStr(bool v) { return v ? QStringLiteral("true") : QStringLiteral("false"); }

inline QString readTextFile(const QString &path) {
    QFile f(path);
    if (!f.open(QIODevice::ReadOnly))
        return QString();
    QTextStream in(&f);
    in.setEncoding(QStringConverter::Utf8);
    return in.readAll();
}

// Use a throw-away preference store so tests never touch the user's.
inline void testInitPrefs() {
    Prefs::useStore(QStringLiteral("/tmp/jubler-qt-test-prefs-%1.ini").arg(QCoreApplication::applicationPid()));
    Prefs::resetPrefs();
}

inline int testFinish(const char *name) {
    if (g_failures)
        std::fprintf(stderr, "%s: %d failure(s)\n", name, g_failures);
    else
        std::printf("%s: all passed\n", name);
    return g_failures ? 1 : 0;
}
