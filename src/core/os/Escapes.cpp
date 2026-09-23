/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/os/Escapes.h"

namespace Escapes {

QString unescapeJavaLenient(const QString &s) {
    QString out;
    for (int i = 0; i < s.length(); ++i) {
        const QChar c = s.at(i);
        if (c != QLatin1Char('\\')) { out += c; continue; }
        if (i + 1 >= s.length()) { out += QLatin1Char('\\'); break; }
        const QChar e = s.at(++i);
        switch (e.unicode()) {
            case 'b': out += QChar(8); break;
            case 't': out += QLatin1Char('\t'); break;
            case 'n': out += QLatin1Char('\n'); break;
            case 'f': out += QChar(12); break;
            case 'r': out += QLatin1Char('\r'); break;
            case '"': out += QLatin1Char('"'); break;
            case '\'': out += QLatin1Char('\''); break;
            case '\\': out += QLatin1Char('\\'); break;
            case 'u': {
                int j = i + 1, cp = 0, got = 0;
                while (j < s.length() && got < 4) {
                    bool ok = false;
                    const int d = QString(s.at(j)).toInt(&ok, 16);
                    if (!ok) break;
                    cp = (cp << 4) | d; ++j; ++got;
                }
                if (got == 4) { out += QChar(cp); i = j - 1; }
                else { out += QLatin1String("\\u"); out += s.mid(i + 1, got); i += got; }
                break;
            }
            default:
                if (e >= QLatin1Char('0') && e <= QLatin1Char('7')) {
                    int val = e.unicode() - '0', count = 1;
                    while (count < 3 && i + 1 < s.length()) {
                        const QChar n = s.at(i + 1);
                        if (n < QLatin1Char('0') || n > QLatin1Char('7')) break;
                        ++i; ++count; val = (val << 3) + (n.unicode() - '0');
                    }
                    out += QChar(val);
                } else
                    out += QLatin1Char('\\') + QString(e);
        }
    }
    return out;
}

QStringList parseParametersWithEscaping(const QString &input) {
    QStringList result;
    QString current;
    for (int i = 0; i < input.length(); ++i) {
        const QChar c = input.at(i);
        if (c == QLatin1Char('\\') && i + 1 < input.length()) {
            const QChar n = input.at(i + 1);
            // Only the separator is consumed here; every other escape (\\, \=,
            // regex escapes such as \d) is passed on to the value.
            if (n == QLatin1Char(':')) { current += n; ++i; }
            else { current += c; current += n; ++i; }
        } else if (c == QLatin1Char(':')) { result.append(current); current.clear(); }
        else current += c;
    }
    if (!current.isEmpty())
        result.append(current);
    return result;
}

QString unescapeParameterValue(const QString &value) {
    if (!value.contains(QLatin1Char('\\')))
        return value;
    QString result;
    bool escaped = false;
    for (const QChar c : value) {
        if (escaped) {
            if (c == QLatin1Char(':') || c == QLatin1Char('=') || c == QLatin1Char('\\')) result += c;
            else { result += QLatin1Char('\\'); result += c; }
            escaped = false;
        } else if (c == QLatin1Char('\\'))
            escaped = true;
        else
            result += c;
    }
    if (escaped)
        result += QLatin1Char('\\');
    return result;
}

}  // namespace Escapes
