/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QAbstractSpinBox>

#include "core/time/Time.h"

// A time editor "HH:MM:SS,mmm" whose step depends on the caret position
// (hours, tens of minutes, minutes, tens of seconds, seconds, tenths,
// hundredths, milliseconds). Port of `JTimeSpinner` + `TimeSpinnerModel`.
// Page Up / Page Down / Enter emit navigation events for the editor.
class TimeSpinner : public QAbstractSpinBox {
    Q_OBJECT
public:
    enum Navigation { PREVIOUS_LOCK = 0, NEXT_LOCK = 1, NEXT_TIME_SPINNER = 2 };

    explicit TimeSpinner(QWidget *parent = nullptr);

    Time getTimeValue() const { return value_; }
    void setTimeValue(const Time &t);
    double seconds() const { return value_.toSeconds(); }
    // Current step in seconds.
    double speed() const { return speed_; }
    void stepBy(int steps) override;
    QValidator::State validate(QString &input, int &pos) const override;
    void fixup(QString &input) const override;
    bool isSuppressed() const { return suppress_; }

signals:
    void timeChanged(const Time &t);
    void navigation(int event);

protected:
    StepEnabled stepEnabled() const override { return StepUpEnabled | StepDownEnabled; }
    void keyPressEvent(QKeyEvent *e) override;
    void focusInEvent(QFocusEvent *e) override;

private:
    void updateSpeedFromCaret();
    void commitText();
    void refreshText();

    Time value_{0.0};
    double speed_ = 1.0;
    int dot_ = 7;
    bool suppress_ = false;
};
