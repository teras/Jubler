/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/Theme.h"

#include <QApplication>
#include <QHash>
#include <QPainter>
#include <QPalette>
#include <QStyle>
#include <QStyleHints>
#include <QSvgRenderer>


namespace Theme {

int iconSize() {
    return 20;
}

QFont adjustFont(QFont font, double javaPixels) {
    if (font.pointSizeF() > 0)
        font.setPointSizeF(font.pointSizeF() + javaPixels * 0.75);
    else
        font.setPixelSize(qRound(font.pixelSize() + javaPixels));
    return font;
}

QSize naturalSize(const QString &name, double factor) {
    const QSvgRenderer r(QStringLiteral(":/icons/%1.svg").arg(name));
    const QSize n = r.isValid() ? r.defaultSize() : QSize(iconSize(), iconSize());
    return QSize(qRound(n.width() * factor), qRound(n.height() * factor));
}

bool isDark() {
    const QColor bg = QApplication::palette().color(QPalette::Window);
    return bg.lightness() < 128;
}

namespace {
QPalette darkPalette() {
    QPalette p;
    const QColor window(0x35, 0x35, 0x35), base(0x2A, 0x2A, 0x2A), text(0xE6, 0xE6, 0xE6), disabled(0x80, 0x80, 0x80);
    p.setColor(QPalette::Window, window);
    p.setColor(QPalette::WindowText, text);
    p.setColor(QPalette::Base, base);
    p.setColor(QPalette::AlternateBase, window);
    p.setColor(QPalette::ToolTipBase, base);
    p.setColor(QPalette::ToolTipText, text);
    p.setColor(QPalette::PlaceholderText, disabled);
    p.setColor(QPalette::Text, text);
    p.setColor(QPalette::Button, window);
    p.setColor(QPalette::ButtonText, text);
    p.setColor(QPalette::BrightText, Qt::red);
    p.setColor(QPalette::Light, QColor(0x4A, 0x4A, 0x4A));
    p.setColor(QPalette::Midlight, QColor(0x40, 0x40, 0x40));
    p.setColor(QPalette::Mid, QColor(0x25, 0x25, 0x25));
    p.setColor(QPalette::Dark, QColor(0x20, 0x20, 0x20));
    p.setColor(QPalette::Shadow, Qt::black);
    p.setColor(QPalette::Link, QColor(0x5A, 0xA9, 0xE6));
    p.setColor(QPalette::Highlight, QColor(0x2A, 0x82, 0xDA));
    p.setColor(QPalette::HighlightedText, Qt::white);
    for (QPalette::ColorRole r : {QPalette::WindowText, QPalette::Text, QPalette::ButtonText})
        p.setColor(QPalette::Disabled, r, disabled);
    return p;
}

// `width` wide, as tall as the SVG's own aspect ratio makes it (flags are 20×15).
QImage render(const QString &name, int width) {
    QSvgRenderer r(QStringLiteral(":/icons/%1.svg").arg(name));
    const QSize natural = r.isValid() ? r.defaultSize() : QSize(1, 1);
    const int height = natural.width() > 0 ? qMax(1, qRound(width * double(natural.height()) / natural.width())) : width;
    QImage img(width, height, QImage::Format_ARGB32_Premultiplied);
    img.fill(Qt::transparent);
    if (!r.isValid()) {
        // Missing icon: a neutral placeholder rather than a crash.
        QPainter p(&img);
        p.setPen(QPen(Qt::gray, 2));
        p.drawRect(2, 2, width - 4, height - 4);
        return img;
    }
    QPainter p(&img);
    r.render(&p);
    return img;
}

int clamp255(double v) { return v < 0 ? 0 : (v > 255 ? 255 : int(v)); }
}  // namespace

QImage filtered(const QImage &srcIn, IconStatus status) {
    if (status == IconStatus::NORMAL)
        return srcIn;
    QImage src = srcIn.convertToFormat(QImage::Format_ARGB32);
    if (status == IconStatus::SELECTED_PEN) {
        const QImage dot = render(QStringLiteral("pendot"), src.width() / 2).convertToFormat(QImage::Format_ARGB32);
        for (int y = 0; y < dot.height() && y < src.height(); ++y)
            for (int x = 0; x < dot.width() && x < src.width(); ++x)
                if (qAlpha(dot.pixel(x, y)))
                    src.setPixel(x, y, dot.pixel(x, y));
        return src;
    }
    for (int y = 0; y < src.height(); ++y) {
        QRgb *line = reinterpret_cast<QRgb *>(src.scanLine(y));
        for (int x = 0; x < src.width(); ++x) {
            const QRgb px = line[x];
            const int a = qAlpha(px), r = qRed(px), g = qGreen(px), b = qBlue(px);
            int nr = r, ng = g, nb = b;
            switch (status) {
                case IconStatus::PRESSED: nr = clamp255(r * 0.7); ng = clamp255(g * 0.7); nb = clamp255(b * 0.7); break;
                case IconStatus::ROLLOVER: nr = clamp255(r * 1.3); ng = clamp255(g * 1.3); nb = clamp255(b * 1.3); break;
                case IconStatus::ERROR: {
                    const double avg = (r + g + b) / (3.0 * 255);
                    nr = clamp255(255 * avg); ng = clamp255(40 * avg); nb = clamp255(40 * avg);
                    break;
                }
                case IconStatus::MONOCHROME: {
                    const int gray = clamp255(0.4 * r + 0.7 * g + 0.2 * b);
                    nr = ng = nb = gray;
                    break;
                }
                case IconStatus::PINK: nr = clamp255(r * 255 / 255.0); ng = clamp255(g * 175 / 255.0); nb = clamp255(b * 175 / 255.0); break;
                case IconStatus::YELLOW: nb = 0; break;
                case IconStatus::DARK: {
                    const int medhalf = (r + g + b) / 7;
                    nr = int(r * 0.3) + medhalf;
                    ng = int(g * 0.3) + medhalf;
                    nb = int(b * 0.3) + medhalf;
                    break;
                }
                case IconStatus::CYAN: nr = 0; break;
                default: break;
            }
            line[x] = qRgba(nr, ng, nb, a);
        }
    }
    return src;
}

// Rendered at the screen's pixel ratio (sharp on HiDPI); `size` is the width in
// logical pixels.
QPixmap pixmap(const QString &name, int size, IconStatus status) {
    static QHash<QString, QPixmap> cache;
    const qreal dpr = qApp ? qApp->devicePixelRatio() : 1.0;
    const QString key = QStringLiteral("%1/%2/%3/%4").arg(name).arg(size).arg(int(status)).arg(dpr);
    auto it = cache.find(key);
    if (it != cache.end())
        return *it;
    QPixmap pm = QPixmap::fromImage(filtered(render(name, qRound(size * dpr)), status));
    pm.setDevicePixelRatio(dpr);
    cache.insert(key, pm);
    return pm;
}

namespace {
QPixmap rawPixmap(const QString &name, int width, IconStatus status) {
    static QHash<QString, QPixmap> cache;
    const QString key = QStringLiteral("%1/%2/%3").arg(name).arg(width).arg(int(status));
    auto it = cache.find(key);
    if (it != cache.end())
        return *it;
    QPixmap pm = QPixmap::fromImage(filtered(render(name, width), status));
    cache.insert(key, pm);
    return pm;
}
}  // namespace

QIcon icon(const QString &name, IconStatus status, double resize) {
    QIcon ic;
    for (int base : {16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 56, 64, 72, 96, 128}) {
        const int size = qRound(base * resize);
        ic.addPixmap(rawPixmap(name, size, status));   // also when checked: the Java kept the colours
    }
    return ic;
}

void applyVariation(int variation) {
    if (variation != 1 && variation != 2)
        return;  // AUTO keeps the platform scheme
    const bool dark = variation == 2;
    const Qt::ColorScheme wanted = dark ? Qt::ColorScheme::Dark : Qt::ColorScheme::Light;
    QGuiApplication::styleHints()->setColorScheme(wanted);
    if (QGuiApplication::styleHints()->colorScheme() == wanted && isDark() == dark)
        return;
    // The platform theme ignored the request (e.g. KDE, whose style reads the
    // desktop colour scheme itself): Fusion with an explicit palette.
    QApplication::setStyle(QStringLiteral("Fusion"));
    QApplication::setPalette(dark ? darkPalette() : QApplication::style()->standardPalette());
}

QIcon logo() {
    QIcon ic;
    QSvgRenderer r(QStringLiteral(":/logo/logo.svg"));
    for (int size : {16, 32, 48, 64, 128, 256}) {
        QImage img(size, size, QImage::Format_ARGB32_Premultiplied);
        img.fill(Qt::transparent);
        QPainter p(&img);
        r.render(&p);
        ic.addPixmap(QPixmap::fromImage(img));
    }
    return ic;
}

}  // namespace Theme
