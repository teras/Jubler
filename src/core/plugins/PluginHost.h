/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QString>
#include <functional>
#include <memory>

#include "jubler/PluginApi.h"

class Subtitles;
class QObject;

// A Subtitles document as the plugin API's Document.
class PluginDocument : public jubler::Document {
public:
    explicit PluginDocument(Subtitles &subs, QList<int> selection = {}, QString mediaPath = QString());
    int count() const override;
    jubler::Subtitle at(int index) const override;
    void replace(int index, const jubler::Subtitle &subtitle) override;
    void insert(int index, const jubler::Subtitle &subtitle) override;
    void remove(int index) override;
    QList<int> selection() const override { return selection_; }
    QStringList styleNames() const override;
    float fps() const override;
    QString filePath() const override;
    QString mediaPath() const override { return mediaPath_; }
    bool changed() const { return changed_; }

private:
    Subtitles &subs_;
    QList<int> selection_;
    QString mediaPath_;
    bool changed_ = false;
};

// The host side of the plugin API that needs no GUI: registration, and the
// adapters that put plugin formats, translators, spell checkers and
// command-line tools into Jubler's own registries. Tools, event listeners
// and Preferences pages are kept here for the application to use.
namespace PluginHost {

// Must be set before a plugin is registered (the application's services).
void setHostServices(jubler::HostServices *services);
// Where plugin subtitle providers go (the application's provider list).
void setProviderSink(std::function<void(std::shared_ptr<jubler::SubtitleProvider>)> sink);

// Registers the extensions of a loaded plugin root object; false (with
// `error`) when it is not a Jubler plugin or needs a newer API.
bool registerPlugin(QObject *root, const QString &pluginName, QString *error);

const QList<std::shared_ptr<jubler::Tool>> &tools();
const QList<std::shared_ptr<jubler::EventListener>> &eventListeners();
const QList<std::shared_ptr<jubler::PreferencesPage>> &preferencesPages();

}  // namespace PluginHost
