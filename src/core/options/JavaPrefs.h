/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QMap>
#include <QString>

// Migration of the preferences of the Java Jubler (java.util.prefs node
// `/com/panayotis/jubler`). The keys are the same in both builds; only the
// shortcut overrides change form (Java key codes → Qt portable key strings).
namespace JavaPrefs {

// Reads a Java XML preferences file: either a `Preferences.exportNode` export
// (`<preferences><root><node name="com">…`) or the Linux backing store
// (`<map><entry key=… value=…/>`). Empty map + `error` set on failure.
QMap<QString, QString> parseXml(const QByteArray &xml, QString *error = nullptr);

// `TAG=(keycode,modifiers)` list of the Java "shortcut.keys" → the port form
// `TAG=(Ctrl+S)`. Unknown key codes are dropped (the default stays).
QString convertShortcuts(const QString &javaValue);

// The Java preferences of the current user in the platform store (Linux
// ~/.java/.userPrefs, macOS plist, Windows registry); empty if none.
QMap<QString, QString> readNativeStore();

// A registry value name (`value` false) or data (`value` true) as the Java
// WindowsPreferences escapes them: "/X" is an upper-case X, "\" is "/", "//"
// is "\" and, in data only, "/uXXXX" is a UTF-16 unit. Names in the "/!"
// base64 form are not Jubler keys and are skipped by the caller.
QString fromRegistry(const QString &s, bool value);

// Replaces all preferences with `javaPrefs`, converting what differs.
void apply(const QMap<QString, QString> &javaPrefs);

}  // namespace JavaPrefs
