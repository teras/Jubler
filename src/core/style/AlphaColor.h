/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QColor>
#include <QString>

// Colour with an explicit alpha channel, port of the Java `AlphaColor`.
// Text form is "AARRGGBB" hex, lower-case, always 8 characters when written;
// when parsed the last 6 characters are RGB and whatever precedes them is the
// alpha (0 when absent). Alpha is opacity: 255 = fully opaque.
class AlphaColor {
public:
    AlphaColor() : rgb_(0), alpha_(0) {}
    AlphaColor(const QColor &c, int alpha) : rgb_(c.rgb() & 0xffffff), alpha_(alpha & 0xff) {}
    // 0xAARRGGBB
    explicit AlphaColor(unsigned int argb) : rgb_(argb & 0xffffff), alpha_((argb >> 24) & 0xff) {}
    explicit AlphaColor(const QString &text);

    int red() const { return (rgb_ >> 16) & 0xff; }
    int green() const { return (rgb_ >> 8) & 0xff; }
    int blue() const { return rgb_ & 0xff; }
    int alpha() const { return alpha_; }
    unsigned int rgb() const { return rgb_; }
    unsigned int argb() const { return rgb_ | (unsigned(alpha_) << 24); }

    // Opaque colour (alpha ignored), as Java `Color` methods returned.
    QColor color() const { return QColor(red(), green(), blue()); }
    // Colour carrying the alpha.
    QColor aColor() const { return QColor(red(), green(), blue(), alpha_); }

    // Blend this colour over `other` using `newAlpha` as opacity.
    QColor mixed(const QColor &other, int newAlpha) const;
    QColor mixed(const QColor &other) const { return mixed(other, alpha_); }

    QString toString() const;

    bool operator==(const AlphaColor &o) const { return rgb_ == o.rgb_ && alpha_ == o.alpha_; }
    bool operator!=(const AlphaColor &o) const { return !(*this == o); }

private:
    unsigned int rgb_;
    int alpha_;
};
