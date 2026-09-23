/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QSet>
#include <QString>

// Drop-in plugins of the per-user plugins directory: discovered at startup,
// loaded only when enabled in Preferences ▸ Plugins. Port of
// `PluginRegistry`/`DynamicClassLoader` for Qt plugins.
namespace PluginRegistry {
struct PluginInfo {
    QString key;           // the file name
    QString path, name, description;
    bool isNew = false;
    bool enabled = false;
};
QString directory();
// Scan the directory, register every plugin, mark the new ones known and
// load the enabled ones (their providers are added to the registry).
// `rememberKnown` false (command-line runs): the GUI still announces new plugins.
void discover(bool rememberKnown = true);
const QList<PluginInfo> &plugins();
bool hasPlugins();
QList<PluginInfo> newPlugins();
QSet<QString> enabledKeys();
void setEnabledKeys(const QSet<QString> &keys);
bool isEnabled(const QString &key);
}  // namespace PluginRegistry
