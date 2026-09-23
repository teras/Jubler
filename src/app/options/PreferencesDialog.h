/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QDialog>
#include <QMap>
#include <QString>

class QAction;
class QMenuBar;
class MainWindow;

// One page of the preferences dialog. Port of `OptionsHolder`.
class OptionsPage : public QWidget {
    Q_OBJECT
public:
    using QWidget::QWidget;
    virtual QString pageName() const = 0;
    virtual QString iconName() const = 0;
    virtual QString pageTooltip() const = 0;
    virtual void loadPreferences() = 0;
    virtual void savePreferences() = 0;
    virtual QIcon pageIcon() const;   // default: the theme icon `iconName()`
};

// The modal "Jubler Preferences" dialog: a strip of page buttons, the pages,
// Export / Import / Reset and Cancel / Accept. Port of `JPreferences` +
// `JOptionTabs`. One instance per application.
namespace PreferencesDialog {
void showPreferencesDialog(MainWindow *window);
// (Re)apply the user shortcuts to a window's named actions; the declared
// shortcut of an action is remembered as its default the first time.
void applyMenuShortcuts(const QMap<QString, QAction *> &actions);
// Persisted override of a command (empty = cleared, null = default).
QMap<QString, QString> loadShortcutOverrides();
void saveShortcutOverrides(const QMap<QString, QString> &overrides);
}  // namespace PreferencesDialog
