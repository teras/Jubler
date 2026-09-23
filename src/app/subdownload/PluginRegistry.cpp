/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/subdownload/PluginRegistry.h"

#include <QDir>
#include <QFileInfo>
#include <QJsonObject>
#include <QPluginLoader>

#include "app/subdownload/HostServices.h"
#include "app/subdownload/Providers.h"
#include "core/options/Prefs.h"
#include "core/plugins/PluginHost.h"
#include "core/os/Debug.h"
#include "core/os/SystemDependent.h"
#include "jubler/PluginApi.h"

namespace PluginRegistry {
namespace {
const QString ENABLED_KEY = QStringLiteral("plugins.enabled");
const QString KNOWN_KEY = QStringLiteral("plugins.known");
QList<PluginInfo> g_plugins;

QSet<QString> readSet(const QString &key) {
    QSet<QString> out;
    for (const QString &s : Prefs::getString(key, QString()).split(QLatin1Char('\n'))) {
        const QString t = s.trimmed();
        if (!t.isEmpty()) out.insert(t);
    }
    return out;
}

void writeSet(const QString &key, const QSet<QString> &set) {
    QStringList l(set.begin(), set.end());
    l.sort();
    Prefs::set(key, l.join(QLatin1Char('\n')));
}

bool isPluginFile(const QFileInfo &fi) {
    if (!fi.isFile()) return false;
    const QString s = fi.suffix().toLower();
    return s == QLatin1String("so") || s == QLatin1String("dll") || s == QLatin1String("dylib");
}
}  // namespace

QString directory() { return SystemDependent::getAppSupportDirPath() + QStringLiteral("/plugins"); }

void discover(bool rememberKnown) {
    g_plugins.clear();
    PluginHost::setHostServices(&JublerHostServices::instance());
    PluginHost::setProviderSink([](std::shared_ptr<jubler::SubtitleProvider> p) {
        SubtitleProviders::registerBuiltin();
        SubtitleProviders::add(std::move(p));
    });
    const QSet<QString> enabled = readSet(ENABLED_KEY);
    QSet<QString> known = readSet(KNOWN_KEY);
    const QDir dir(directory());
    for (const QFileInfo &fi : dir.entryInfoList(QDir::Files, QDir::Name)) {
        if (!isPluginFile(fi)) continue;
        QPluginLoader loader(fi.filePath());
        const QJsonObject meta = loader.metaData().value(QStringLiteral("MetaData")).toObject();
        const QString name = meta.value(QStringLiteral("name")).toString().trimmed();
        if (name.isEmpty()) {
            Debug::debug(QStringLiteral("Ignoring plugin without a name: ") + fi.fileName());
            continue;
        }
        PluginInfo info;
        info.key = fi.fileName();
        info.path = fi.filePath();
        info.name = name;
        info.description = meta.value(QStringLiteral("description")).toString();
        info.isNew = !known.contains(info.key);
        info.enabled = enabled.contains(info.key);
        known.insert(info.key);
        if (info.enabled) {
            QObject *root = loader.instance();
            QString error;
            if (!root) {
                Debug::debug(QStringLiteral("Could not load plugin ") + fi.fileName() + QStringLiteral(": ") + loader.errorString());
            } else {
                // The provider interface and the general one; a plugin may implement both.
                auto *providers = qobject_cast<jubler::SubtitleProviderPlugin *>(root);
                if (providers) {
                    SubtitleProviders::registerBuiltin();
                    for (const auto &p : providers->createProviders(&JublerHostServices::instance())) SubtitleProviders::add(p);
                }
                const bool general = qobject_cast<jubler::Plugin *>(root) != nullptr;
                if (general && !PluginHost::registerPlugin(root, name, &error))
                    Debug::debug(QStringLiteral("Could not load plugin %1: %2").arg(fi.fileName(), error));
                else if (providers || general)
                    Debug::debug(QStringLiteral("Loaded plugin ") + name);
                else
                    Debug::debug(QStringLiteral("Could not load plugin %1: not a Jubler plugin").arg(fi.fileName()));
            }
        }
        g_plugins.append(info);
    }
    if (rememberKnown) writeSet(KNOWN_KEY, known);
}

const QList<PluginInfo> &plugins() { return g_plugins; }
bool hasPlugins() { return !g_plugins.isEmpty(); }

QList<PluginInfo> newPlugins() {
    QList<PluginInfo> out;
    for (const PluginInfo &p : g_plugins)
        if (p.isNew) out.append(p);
    return out;
}

QSet<QString> enabledKeys() { return readSet(ENABLED_KEY); }
void setEnabledKeys(const QSet<QString> &keys) { writeSet(ENABLED_KEY, keys); }
bool isEnabled(const QString &key) { return readSet(ENABLED_KEY).contains(key); }

}  // namespace PluginRegistry
