/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/style/WebSafeFonts.h"

#include <QHash>
#include <QSet>

#include "core/style/SubStyle.h"

namespace WebSafeFonts {

const QStringList COMMON = {QStringLiteral("Arial"), QStringLiteral("Helvetica"), QStringLiteral("Verdana"), QStringLiteral("Tahoma"),
                            QStringLiteral("Trebuchet MS"), QStringLiteral("Times New Roman"), QStringLiteral("Georgia"),
                            QStringLiteral("Courier New"), QStringLiteral("Comic Sans MS"), QStringLiteral("Impact")};

QString sansSerif() { return QStringLiteral("Sans Serif"); }
QString serif() { return QStringLiteral("Serif"); }
QString monospace() { return QStringLiteral("Monospace"); }

namespace {
const QHash<QString, QString> &category() {
    static QHash<QString, QString> c;
    if (c.isEmpty()) {
        for (const char *s : {"arial", "helvetica", "verdana", "tahoma", "trebuchet ms", "comic sans ms", "impact", "segoe ui", "calibri", "geneva", "roboto", "noto sans"})
            c.insert(QLatin1String(s), sansSerif());
        for (const char *s : {"times new roman", "times", "georgia", "garamond", "palatino", "palatino linotype", "book antiqua", "cambria", "noto serif"})
            c.insert(QLatin1String(s), serif());
        for (const char *s : {"courier new", "courier", "consolas", "monaco", "menlo", "lucida console", "dejavu sans mono"})
            c.insert(QLatin1String(s), monospace());
    }
    return c;
}
}  // namespace

bool isInstalled(const QString &name) {
    if (name.isEmpty()) return false;
    static QSet<QString> installed;
    static int seenCount = -1;
    if (seenCount != SubStyle::fontNames().size()) {
        installed.clear();
        for (const QString &f : SubStyle::fontNames()) installed.insert(f.toLower());
        seenCount = SubStyle::fontNames().size();
    }
    return installed.contains(name.toLower());
}

QString renderFamily(const QString &name) {
    if (name.trimmed().isEmpty()) return sansSerif();
    if (isInstalled(name)) return name;
    const QString key = name.toLower();
    if (category().contains(key)) return category().value(key);
    if (key.contains(QLatin1String("mono")) || key.contains(QLatin1String("courier")) || key.contains(QLatin1String("console")) || key.contains(QLatin1String("typewriter")))
        return monospace();
    if (key.contains(QLatin1String("sans"))) return sansSerif();
    if (key.contains(QLatin1String("serif")) || key.contains(QLatin1String("times")) || key.contains(QLatin1String("roman")) || key.contains(QLatin1String("georgia")))
        return serif();
    return sansSerif();
}

}  // namespace WebSafeFonts
