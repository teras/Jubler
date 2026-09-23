/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QDialog>
#include <QList>

#include "core/update/VersionData.h"

class MainWindow;

// Startup check of the GitHub releases; a newer release lights the "New
// version!" toolbar button of every window. Port of `AutoUpdater`/
// `UIUpdater`, with an opt-out preference and at most one check per day.
namespace AutoUpdater {
bool isEnabled();                 // "autoupdate.enabled", default true
void setEnabled(bool enabled);
// Run the check (asynchronously) when enabled, not Flatpak, not packaged
// and not already checked today.
void checkAtStartup();
const QList<ReleaseInfo> &newerReleases();
// Give a window the callback when releases are already known.
void attachWindow(MainWindow *w);
}  // namespace AutoUpdater

// "Jubler has a new version!": the notes of every newer release and a link
// to the newest one. Port of `JUpdateInfo`.
class UpdateInfoDialog : public QDialog {
    Q_OBJECT
public:
    explicit UpdateInfoDialog(QWidget *parent, const QList<ReleaseInfo> &releases);
};
