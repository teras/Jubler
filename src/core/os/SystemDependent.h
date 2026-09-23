/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QKeySequence>
#include <QString>

// Platform-dependent helpers, port of `SystemDependent`/`CommonDef`.
namespace SystemDependent {
bool isLinux();
bool isWindows();
bool isMacOS();
bool isFlatpak();   // Linux with /.flatpak-info
// The document-portal host path stamped on a granted file (Flatpak), or null.
QString hostPath(const QString &path);
bool isPortalGranted(const QString &path);
// Path shown in titles/menus: host path under the portal, the bare name in
// Flatpak otherwise, the full path elsewhere.
QString displayPath(const QString &path);
bool shouldSupportChangeScaling();   // not macOS
// Previous/next entry shortcuts: Ctrl+Alt+Up/Down on macOS, Ctrl+Up/Down elsewhere.
QKeySequence getUpDownKeystroke(bool up);
// Modifier names ("Ctrl+Shift", "⌘⇧") for tooltips.
QString getKeyMods(Qt::KeyboardModifiers mods, bool withBraces);
bool canWrite(const QString &path);
// Per-user application data directory (autosave/, langpacks/, plugins/, log).
QString getAppSupportDirPath();
// Per-user configuration directory (prefs.ini): the XDG config dir on Linux,
// the application data directory elsewhere.
QString getConfigDirPath();
// Per-user cache directory (regenerable data): XDG cache on Linux,
// ~/Library/Caches/Jubler on macOS, %LOCALAPPDATA%\\Jubler\\cache on Windows.
QString getCacheDirPath();
QString getAssetExtension();   // .dmg / .exe / .appimage
QString getAssetTag();         // mac / win / linux
}  // namespace SystemDependent
