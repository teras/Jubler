/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <QStringList>

// Font family fallbacks for uninstalled names, port of `WebSafeFonts`.
namespace WebSafeFonts {
extern const QStringList COMMON;   // Arial, Helvetica, Verdana, ...
bool isInstalled(const QString &name);
// The family to render with: the name itself when installed, else a
// generic sans-serif/serif/monospace family by category or name hints.
QString renderFamily(const QString &name);
QString sansSerif();
QString serif();
QString monospace();
}  // namespace WebSafeFonts
