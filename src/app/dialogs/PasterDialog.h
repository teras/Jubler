/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QDialog>

#include "core/time/Time.h"

class TimeSpinner;
class QCheckBox;
class QComboBox;

// "Paste special options": where to start pasting and an optional mark for
// the pasted entries. Port of `JPasterGUI`.
class PasterDialog : public QDialog {
    Q_OBJECT
public:
    explicit PasterDialog(const Time &initial, QWidget *parent = nullptr);
    int getMark() const;   // -1 when unchanged
    Time getStartTime() const;

private:
    TimeSpinner *spinner_;
    QCheckBox *changeColor_;
    QComboBox *color_;
};
