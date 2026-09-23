/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/style/AlphaColor.h"

AlphaColor::AlphaColor(const QString &text) {
    const int len = text.length();
    const int split = len > 6 ? len - 6 : 0;
    bool ok = false;
    rgb_ = text.mid(split).toUInt(&ok, 16) & 0xffffff;
    if (!ok)
        rgb_ = 0;  // Java would throw NumberFormatException; callers pass valid data
    alpha_ = len > 6 ? int(text.left(split).toUInt(&ok, 16) & 0xff) : 0;
}

namespace {
int calc(int c1, int c2, float a, float na) {
    int ret = int(c1 * a + c2 * na);
    return ret < 0 ? 0 : (ret > 255 ? 255 : ret);
}
}  // namespace

QColor AlphaColor::mixed(const QColor &other, int newAlpha) const {
    const float a = newAlpha / 255.0f;
    const float na = 1 - a;
    return QColor(calc(red(), other.red(), a, na), calc(green(), other.green(), a, na),
                  calc(blue(), other.blue(), a, na));
}

QString AlphaColor::toString() const {
    return QStringLiteral("%1%2")
        .arg(alpha_, 2, 16, QLatin1Char('0'))
        .arg(rgb_, 6, 16, QLatin1Char('0'));
}
