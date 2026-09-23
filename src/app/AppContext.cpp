/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/AppContext.h"

#include <QTimer>
#include <QApplication>
#include <QDialog>
#include <QDialogButtonBox>
#include <QFileInfo>
#include <QLabel>
#include <QListWidget>
#include <QMessageBox>
#include <QScreen>
#include <QVBoxLayout>
#include <QWindow>

#include "app/dialogs/AboutDialog.h"
#include "core/externals/Recipe.h"
#include "app/ui/MainWindow.h"
#include "core/i18n/I18N.h"
#include "core/options/Options.h"
#include "core/options/Prefs.h"
#include "core/os/AutoSaver.h"
#include "core/os/Debug.h"

namespace AppContext {

namespace {
QList<MainWindow *> g_windows;
MainWindow *g_current = nullptr;
QList<SubEntryPtr> g_copyBuffer;
QList<SubFile> g_recent;
bool g_recentLoaded = false;
bool g_celebration = false;
QRect g_geometry(-1, -1, 0, 0);
int g_state = 0;   // 0 normal, 1 maximised
bool g_geometryLoaded = false;

void loadWindowPosition() {
    if (g_geometryLoaded) return;
    g_geometryLoaded = true;
    // "((x,y),(w,h),state)"
    const QString s = Prefs::getString(QStringLiteral("system.windowstate"), QString());
    int vals[5] = {-1, -1, -1, -1, 0};
    int i = 0;
    for (const QString &tok : s.split(QRegularExpression(QStringLiteral("[(),]")), Qt::SkipEmptyParts)) {
        if (i >= 5) break;
        bool ok = false;
        const int v = tok.trimmed().toInt(&ok);
        if (ok) vals[i] = v;  // a corrupt token is ignored, the default stays
        ++i;
    }
    int w = vals[2], h = vals[3];
    if (w <= 0 || h <= 0) { w = FIRST_WIDTH; h = DEFAULT_HEIGHT; }
    g_geometry = QRect(vals[0], vals[1], w, h);
    g_state = vals[4];
}
}  // namespace

QList<MainWindow *> &windows() { return g_windows; }
MainWindow *currentWindow() { return g_current; }
void setCurrentWindow(MainWindow *w) { g_current = w; }
void addWindow(MainWindow *w) { if (!g_windows.contains(w)) g_windows.append(w); }
void removeWindow(MainWindow *w) { g_windows.removeOne(w); if (g_current == w) g_current = g_windows.isEmpty() ? nullptr : g_windows.last(); }

QList<SubEntryPtr> &copyBuffer() { return g_copyBuffer; }

QList<SubFile> &recentFiles() {
    if (!g_recentLoaded) {
        g_recentLoaded = true;
        g_recent = Options::loadFileList();
    }
    return g_recent;
}

void addRecentFile(const QString &path) {
    if (path.isEmpty() || !QFileInfo::exists(path)) return;
    addRecentFile(SubFile(path, SubFile::EXTENSION_GIVEN));
}

void addRecentFile(const SubFile &file) {
    if (file.getSaveFile().isEmpty() || !QFileInfo::exists(file.getSaveFile())) return;
    recentFiles().removeAll(file);
    recentFiles().append(file);
    updateRecents();
}

void updateRecents() {
    QList<SubFile> &list = recentFiles();
    for (MainWindow *w : g_windows) {
        if (!w->getSubtitles()) continue;
        const SubFile &sf = w->getSubtitles()->getSubFile();
        if (!sf.exists()) continue;
        list.removeAll(sf);
        list.append(sf);
    }
    Options::saveFileList(list);
    for (MainWindow *w : g_windows)
        w->rebuildRecentMenu();
}

void setWindowPosition(MainWindow *w, bool save) {
    loadWindowPosition();
    g_geometry = w->normalGeometry();
    g_state = w->isMaximized() ? 6 : 0;
    if (save && g_geometry.width() > 0)
        Prefs::set(QStringLiteral("system.windowstate"), QStringLiteral("((%1,%2),(%3,%4),%5)").arg(g_geometry.x()).arg(g_geometry.y()).arg(g_geometry.width()).arg(g_geometry.height()).arg(g_state));
    jumpWindowPosition(true);
}

// `frame`: the window decoration around the client area (title bar,
// borders); the whole window, frame included, is fitted.
QRect fitToScreen(QRect wanted, const QMargins &frame) {
    if (wanted.width() < DEFAULT_WIDTH) wanted.setWidth(DEFAULT_WIDTH);
    if (wanted.height() < DEFAULT_HEIGHT) wanted.setHeight(DEFAULT_HEIGHT);
    const QList<QScreen *> screens = QApplication::screens();
    if (screens.isEmpty()) return wanted;
    // Entirely covered by the union of screens → keep.
    QRegion all;
    for (QScreen *s : screens) all += s->geometry();
    const QRect framed = wanted.marginsAdded(frame);
    if ((QRegion(framed) - all).isEmpty())
        return wanted;
    QScreen *best = screens.first();
    long long bestArea = -1;
    for (QScreen *s : screens) {
        const QRect in = s->geometry().intersected(wanted);
        const long long area = in.isValid() ? (long long)in.width() * in.height() : 0;
        if (area > bestArea) { bestArea = area; best = s; }
    }
    const QRect avail = best->availableGeometry().marginsRemoved(frame);
    if (avail.isEmpty()) return wanted;
    QRect r = wanted;
    r.setWidth(std::min(r.width(), avail.width()));
    r.setHeight(std::min(r.height(), avail.height()));
    r.moveLeft(std::min(std::max(r.x(), avail.x()), avail.x() + avail.width() - r.width()));
    r.moveTop(std::min(std::max(r.y(), avail.y()), avail.y() + avail.height() - r.height()));
    return r;
}

void putWindowPosition(MainWindow *w) {
    loadWindowPosition();
    if (g_geometry.width() <= 0) return;
    const QRect r = fitToScreen(g_geometry);
    w->setGeometry(r);
    // Java's extended state: MAXIMIZED_BOTH = 6 (older port builds wrote 1).
    if (g_state == 1 || (g_state & 6) == 6) w->setWindowState(Qt::WindowMaximized);
    // The frame is known once the window manager has shown the window: fit again with it.
    QTimer::singleShot(300, w, [w]() {
        if (!w->isVisible() || w->isMaximized() || w->isFullScreen()) return;
        const QRect g = w->geometry(), f = w->frameGeometry();
        const QMargins frame(g.left() - f.left(), g.top() - f.top(), f.right() - g.right(), f.bottom() - g.bottom());
        if (frame.isNull()) return;
        const QRect fitted = fitToScreen(g, frame);
        if (fitted != g) w->setGeometry(fitted);
    });
    jumpWindowPosition(true);
}

void jumpWindowPosition(bool forth) {
    g_geometry.translate(forth ? SCREEN_DELTAX : -SCREEN_DELTAX, forth ? SCREEN_DELTAY : -SCREEN_DELTAY);
}

bool requestQuit(MainWindow *request, MainWindow *confirmed) {
    QStringList names;
    for (MainWindow *w : g_windows)
        if (w != confirmed && w->isUnsaved() && !w->isEmptyContent())
            names.append(w->documentName());
    if (!names.isEmpty()) {
        QDialog dlg(request);
        dlg.setWindowTitle(__("Quit Jubler"));
        auto *l = new QVBoxLayout(&dlg);
        l->addWidget(new QLabel(__("The following files are unsaved:"), &dlg));
        auto *list = new QListWidget(&dlg);
        list->addItems(names);
        l->addWidget(list);
        l->addWidget(new QLabel(__("Do you want to quit without saving?"), &dlg));
        auto *buttons = new QDialogButtonBox(QDialogButtonBox::Yes | QDialogButtonBox::No, &dlg);
        QObject::connect(buttons, &QDialogButtonBox::accepted, &dlg, &QDialog::accept);
        QObject::connect(buttons, &QDialogButtonBox::rejected, &dlg, &QDialog::reject);
        l->addWidget(buttons);
        if (dlg.exec() != QDialog::Accepted)
            return false;
    }
    MainWindow *w = request ? request : (g_windows.isEmpty() ? nullptr : g_windows.last());
    if (w) setWindowPosition(w, true);
    // Recovered autosaves of documents the user chose to discard are dropped
    // with the rest; saved documents no longer need theirs.
    AutoSaver::cleanup();
    return true;
}

void showAbout() {
    AboutDialog dlg(g_windows.isEmpty() ? nullptr : g_windows.first());
    dlg.exec();
}

void updateAllMenus() {
    for (MainWindow *w : g_windows) w->applyShortcuts();
}

void updateExternals() {
    Recipes::load();
    for (MainWindow *w : g_windows) {
        w->rebuildExternalsMenu();
        w->applyShortcuts();
    }
}

bool celebrationShown() { return g_celebration; }
void setCelebrationShown() { g_celebration = true; }

}  // namespace AppContext
