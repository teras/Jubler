/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QDialog>

class MainWindow;
class QLineEdit;
class QTextEdit;
class QCheckBox;
class QPushButton;

// Non-modal stepwise "Find & replace" over all subtitles starting at the
// selected row. Port of `JReplace`.
class ReplaceDialog : public QDialog {
    Q_OBJECT
public:
    ReplaceDialog(MainWindow *window, int startRow);

private:
    void findNextWord();
    void replaceCurrent();
    void showContext(const QString &text, int from, int len);

    MainWindow *window_;
    QTextEdit *context_;
    QLineEdit *find_, *replace_;
    QCheckBox *ignoreCase_;
    QPushButton *replaceB_;
    int row_ = 0, nextpos_ = 0, foundpos_ = -1, foundlen_ = 0;
    bool undoPushed_ = false;
};
