/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/subs/SubAttribs.h"

#include <QProcessEnvironment>

QString SubAttribs::userName() {
    const QProcessEnvironment env = QProcessEnvironment::systemEnvironment();
    QString u = env.value(QStringLiteral("USER"));
    if (u.isEmpty()) u = env.value(QStringLiteral("USERNAME"));
    if (u.isEmpty()) u = env.value(QStringLiteral("LOGNAME"));
    return u;
}

SubAttribs::SubAttribs(const QString &t, const QString &a, const QString &s, const QString &c)
    : title(t.isNull() ? QString(QLatin1String("")) : t),
      author(a.isNull() ? userName() : a),
      source(s.isNull() ? QString(QLatin1String("")) : s),
      comments(c.isNull() ? QStringLiteral("Edited with Jubler subtitle editor") : c) {}
