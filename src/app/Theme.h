/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QSize>

#include <QFont>
#include <QIcon>
#include <QImage>
#include <QPixmap>
#include <QString>

// Icon loading with the Java `IconStatus` filters, port of `Theme`.
namespace Theme {

// DARK: the Java DarkIconFilter of the tri-state style buttons ("unspecified").
enum class IconStatus { NORMAL, PRESSED, ROLLOVER, ERROR, MONOCHROME, SELECTED_PEN, PINK, YELLOW, CYAN, DARK };

// Base size the Java build rendered icons at (Qt applies the UI scale).
int iconSize();
// The size an SVG icon declares (its width/height), as the Java shows it by default.
// `font` changed by the Java's deriveFont(size + javaPixels): the Java's sizes
// are pixels at 96 dpi, the port keeps points (0.75 pt a pixel) so that the
// desktop's font DPI setting still applies.
QFont adjustFont(QFont font, double javaPixels);
QSize naturalSize(const QString &name, double factor = 1.0);   // × the Java's loadIcon factor
// An icon by resource name ("bold" → :/icons/bold.svg) with an optional
// status filter and size multiplier.
QIcon icon(const QString &name, IconStatus status = IconStatus::NORMAL, double resize = 1.0);
QPixmap pixmap(const QString &name, int size, IconStatus status = IconStatus::NORMAL);
QImage filtered(const QImage &src, IconStatus status);
// True when the palette is dark (window background luminance).
bool isDark();
// Apply the "Theme variation" preference (0 AUTO, 1 LIGHT, 2 DARK) once,
// before the first window: through Qt's colour scheme where the platform
// supports it, otherwise with an explicit light/dark palette.
void applyVariation(int variation);
// The application logo in several sizes.
QIcon logo();

}  // namespace Theme
