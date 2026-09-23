/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QMargins>
#include <QRect>
#include <QString>

#include "core/subs/Subtitles.h"
#include "core/subs/SubFile.h"

class MainWindow;

// Application-wide state shared by all windows, port of the static parts of
// `JubFrame` and `StaticJubler`: the window list, the entry clipboard, the
// recent-files list and the cascading window geometry.
namespace AppContext {

constexpr int DEFAULT_WIDTH = 800;
constexpr int DEFAULT_HEIGHT = 600;
// The first window's width when no size was stored yet: room for the menus and
// every toolbar button on the one row they share.
constexpr int FIRST_WIDTH = 1100;
constexpr int SCREEN_DELTAX = 24;
constexpr int SCREEN_DELTAY = 24;

QList<MainWindow *> &windows();
MainWindow *currentWindow();
void setCurrentWindow(MainWindow *w);
void addWindow(MainWindow *w);
void removeWindow(MainWindow *w);

// The entry clipboard shared by all windows (not the system clipboard).
QList<SubEntryPtr> &copyBuffer();

// Recent files (most recent last), persisted through Options.
QList<SubFile> &recentFiles();
void addRecentFile(const QString &path);
// The same, for a media file opened for the subtitles inside it: the stream
// is part of the entry, so it opens the same track again.
void addRecentFile(const SubFile &file);
// Refresh the list from the open documents, persist it and rebuild every
// window's Recent menu.
void updateRecents();

// Window geometry: remember `w`'s geometry (persisting it when `save`) and
// advance the cascade; place a new window at the remembered geometry.
void setWindowPosition(MainWindow *w, bool save);
void putWindowPosition(MainWindow *w);
void jumpWindowPosition(bool forth);
QRect fitToScreen(QRect wanted, const QMargins &frame = QMargins());

// Ask to quit: lists unsaved documents; false = aborted.
bool requestQuit(MainWindow *request, MainWindow *confirmed = nullptr);  // `confirmed`: its discard was already accepted
void showAbout();
// Re-apply user shortcuts to every window's menus.
void updateAllMenus();
// Rebuild the Externals menu of every window from the recipe registry.
void updateExternals();

// Whether the celebration panel has been shown this run.
bool celebrationShown();
void setCelebrationShown();

}  // namespace AppContext
