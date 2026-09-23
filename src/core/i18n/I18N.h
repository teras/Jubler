/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QCoreApplication>
#include <QString>

// Translation entry point, port of the Java `I18N.__()`. Every user-visible
// string goes through `__()`, keyed by the English source text in ONE
// context ("jubler") so the existing JSON translation tables convert 1:1 to
// Qt .ts files. Placeholders use the Java MessageFormat form "{0}", "{1}", …
namespace i18n_detail {
// Translated menu titles use "&x" for a Latin mnemonic and "&<non-Latin><Latin>"
// when the displayed letter is not Latin (e.g. "&ΑAρχείο"): the Latin helper
// is dropped so the text reads naturally and Qt keeps the "&" mnemonic.
inline QString fixMnemonic(QString s) {
    for (int i = 0; i + 2 < s.length(); ++i) {
        if (s[i] != QLatin1Char('&')) continue;
        const QChar shown = s[i + 1], helper = s[i + 2];
        const bool shownLatin = shown.unicode() < 128 && shown.isLetter();
        const bool helperLatin = helper.unicode() < 128 && helper.isLetter();
        if (!shownLatin && shown.isLetter() && helperLatin) s.remove(i + 2, 1);
        break;
    }
    return s;
}
}  // namespace i18n_detail

// The Latin helper letter of a translated "&<non-Latin><Latin>" title (the
// "A" of "&ΑAρχείο"), or a null QChar: Alt+<helper> opens the menu with a
// Latin keyboard layout too, as in the Java build.
inline QChar latinMnemonic(const char *text) {
    const QString s = QCoreApplication::translate("jubler", text);
    const int i = s.indexOf(QLatin1Char('&'));
    if (i < 0 || i + 2 >= s.length()) return QChar();
    const QChar shown = s[i + 1], helper = s[i + 2];
    const bool shownLatin = shown.unicode() < 128 && shown.isLetter();
    return !shownLatin && shown.isLetter() && helper.unicode() < 128 && helper.isLetter() ? helper.toUpper() : QChar();
}

inline QString __(const char *text) {
    const QString t = QCoreApplication::translate("jubler", text);
    return t.contains(QLatin1Char('&')) ? i18n_detail::fixMnemonic(t) : t;
}
inline QString __(const QString &text) {
    return __(text.toUtf8().constData());
}

namespace i18n_detail {
inline QString substitute(QString s, int) { return s; }
template <typename T, typename... Rest>
QString substitute(QString s, int idx, const T &first, const Rest &...rest) {
    QString v;
    if constexpr (std::is_same_v<T, QString> || std::is_same_v<T, const char *> || std::is_same_v<T, QLatin1String>)
        v = QString(first);
    else if constexpr (std::is_arithmetic_v<T>)
        v = QString::number(first);
    else
        v = QString(first);
    s.replace(QStringLiteral("{%1}").arg(idx), v);
    return substitute(s, idx + 1, rest...);
}
}  // namespace i18n_detail

template <typename... Args>
QString __(const char *text, const Args &...args) {
    return i18n_detail::substitute(__(text), 0, args...);
}

// Marks a string for extraction without translating it now (Java `N_()`-like
// usage where translation happens at display time).
#define N__(x) x
