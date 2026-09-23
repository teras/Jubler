/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QMap>
#include <QString>
#include <QVariant>

class QSettings;

// The persistent key/value preference store, port of the Java `JublerPrefs`
// (java.util.prefs). Every write is flushed immediately. Keys are the same
// dotted names the Java build used so the semantics documented there carry
// over unchanged.
namespace Prefs {

// A float as Java's Float.toString writes it ("25.0", "23.976"): the
// shortest text that reads back the same, with at least one decimal.
QString floatString(float value);

void set(const QString &key, float value);
void set(const QString &key, int value);
void set(const QString &key, bool value);
// A null string removes the key.
void set(const QString &key, const QString &value);
void remove(const QString &key);

int getInt(const QString &key, int deflt);
QString getString(const QString &key, const QString &deflt);
bool getBoolean(const QString &key, bool deflt);
float getFloat(const QString &key, float deflt);
bool contains(const QString &key);
QStringList keys();

// Export all keys to `path`; returns an empty string on success or the error.
QString exportPrefs(const QString &path);
// Replace all keys with the content of `path`: an INI export of the port or a
// Java XML export (`Preferences.exportNode`) of the Java Jubler.
QString importPrefs(const QString &path);
QString resetPrefs();
// Every key with its stored value, and putting such a snapshot back (the
// store becomes exactly the snapshot): Cancel of the Preferences dialog.
QMap<QString, QVariant> snapshot();
void restore(const QMap<QString, QVariant> &snapshot);
void dump();

// False until the first preference is written (first run).
bool storeExists();

// Where the store lives (for diagnostics / the Flatpak sandbox notes).
QString storePath();

// Test hook: use a throw-away file instead of the user's store.
void useStore(const QString &path);

}  // namespace Prefs
