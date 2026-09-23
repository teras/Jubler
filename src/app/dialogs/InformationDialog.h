/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QDialog>

#include "core/subs/SubAttribs.h"

class MainWindow;
class QLineEdit;
class QPlainTextEdit;

// "Project Properties": the document attributes, the media selector and the
// statistics. Port of `JInformation`.
class InformationDialog : public QDialog {
    Q_OBJECT
public:
    explicit InformationDialog(MainWindow *parent);
    bool isAccepted() const { return accepted_; }
    SubAttribs getAttribs() const;

private:
    MainWindow *window_;
    QLineEdit *title_, *author_, *source_;
    QPlainTextEdit *comments_;
    bool accepted_ = false;
};
