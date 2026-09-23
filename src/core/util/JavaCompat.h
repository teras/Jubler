/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <charconv>

#include <QLocale>
#include <QRegularExpression>
#include <cmath>
#include <QString>
#include <QStringList>

// Helpers that reproduce java.lang semantics the ported code relies on.
namespace jc {

// java.lang.String.split(regex): trailing empty strings are removed; a match
// at position 0 with non-zero width yields a leading empty string; an empty
// input yields [""].
inline QStringList split(const QString &s, const QRegularExpression &re) {
    if (s.isEmpty())
        return {QString(QLatin1String(""))};
    QStringList parts;
    int last = 0;
    auto it = re.globalMatch(s);
    while (it.hasNext()) {
        const QRegularExpressionMatch m = it.next();
        if (m.capturedLength(0) == 0) {
            if (m.capturedStart(0) == 0) continue;  // zero-width at start: no leading ""
            if (m.capturedStart(0) >= s.length()) break;
        }
        parts.append(s.mid(last, m.capturedStart(0) - last));
        last = m.capturedEnd(0);
    }
    parts.append(s.mid(last));
    while (!parts.isEmpty() && parts.last().isEmpty())
        parts.removeLast();
    if (parts.isEmpty())
        parts.append(QString(QLatin1String("")));
    return parts;
}

inline QStringList split(const QString &s, const QString &regex) {
    return split(s, QRegularExpression(regex));
}

// Java Character.isWhitespace: Unicode space separators except non-breaking
// ones, plus the ASCII control whitespace.
inline bool isJavaWhitespace(QChar c) {
    const ushort u = c.unicode();
    if (u == 0x00A0 || u == 0x2007 || u == 0x202F) return false;
    if (u == 0x09 || u == 0x0A || u == 0x0B || u == 0x0C || u == 0x0D
        || u == 0x1C || u == 0x1D || u == 0x1E || u == 0x1F) return true;
    return c.isSpace() && c.category() != QChar::Other_Control;
}

// Java String.trim(): strips the characters <= U+0020 only (a no-break or
// other Unicode space stays, unlike QString::trimmed).
inline QString trim(const QString &s) {
    int b = 0, e = int(s.length());
    while (b < e && s.at(b).unicode() <= 0x20) ++b;
    while (e > b && s.at(e - 1).unicode() <= 0x20) --e;
    return s.mid(b, e - b);
}

// Java Character.isLetterOrDigit
inline bool isLetterOrDigit(QChar c) {
    return c.isLetter() || c.category() == QChar::Number_DecimalDigit;
}

// Java Math.round(float/double): floor(x + 0.5)
inline int roundJava(double v) { return int(std::floor(v + 0.5)); }
inline long long roundJavaL(double v) { return (long long)std::floor(v + 0.5); }

// Java Float.toString(): shortest round-trip text, always with a fraction.
inline QString floatToString(float f) {
    // The shortest digits of the float itself (QLocale works on the value
    // promoted to double: 2.8f became "2.799999952316284").
    char buf[32];
    const auto r = std::to_chars(buf, buf + sizeof(buf), f);
    QString s = QString::fromLatin1(buf, int(r.ptr - buf));
    if (!s.contains(QLatin1Char('.')) && !s.contains(QLatin1Char('e'), Qt::CaseInsensitive)
        && !s.contains(QLatin1String("inf")) && !s.contains(QLatin1String("nan")))
        s += QLatin1String(".0");
    return s;
}

// The regexes of the Java CommonDef interface.
inline const QRegularExpression &reNl() { static const QRegularExpression r(QStringLiteral("([\\r\\n]+)")); return r; }
inline const QRegularExpression &reSp() { static const QRegularExpression r(QStringLiteral("([ \\t]+)")); return r; }
inline const QRegularExpression &reWhiteSp() { static const QRegularExpression r(QStringLiteral("(\\s+)")); return r; }

}  // namespace jc
