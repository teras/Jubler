/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QDialog>
#include <QList>
#include <QWidget>

#include "core/subs/Subtitles.h"
#include "core/time/Time.h"

class Subtitles;
class TimeSpinner;
class QCheckBox;
class QComboBox;
class QLabel;
class QRadioButton;
class QLineEdit;
class QToolButton;

// One labelled time spinner ("Begin"/"Finish") with a preset dropdown; the
// chosen preset (selection edge vs absolute edge) is remembered per side.
// Port of `JTimeSelector`.
class TimeSelector : public QWidget {
    Q_OBJECT
public:
    explicit TimeSelector(bool isBegin, QWidget *parent = nullptr);
    void updateData(const Time &t);
    double getTime() const;
    void setTime(const Time &t);
    void setSelectorEnabled(bool enabled);

private:
    void applyPreset(bool selection);
    bool isBegin_;
    Time last_;
    QLabel *label_;
    TimeSpinner *spinner_;
    QToolButton *dropdown_;
};

// Base of the "which subtitles does this tool affect" panels: the document,
// the selection and the "change affected subtitles' colour" row. Port of
// `JTimeArea`.
class TimeArea : public QWidget {
    Q_OBJECT
public:
    explicit TimeArea(QWidget *parent = nullptr);
    virtual void updateData(Subtitles *subs, const QList<int> &selected);
    virtual QList<SubEntryPtr> getAffectedSubs() const = 0;
    // Apply the chosen mark when the checkbox is selected.
    void updateSubsMark(const QList<SubEntryPtr> &affected) const;
    Time findFirstInList() const;
    Time findLastInList() const;
    QWidget *colorRow() const { return colorRow_; }

protected:
    Subtitles *subs_ = nullptr;
    QList<int> selected_;

private:
    QWidget *colorRow_;
    QCheckBox *changeColor_;
    QComboBox *color_;
};

// A from/to time region; affected = entries whose start lies in it. Port of
// `JTimeRegion`.
class TimeRegion : public TimeArea {
    Q_OBJECT
public:
    explicit TimeRegion(QWidget *parent = nullptr);
    void updateData(Subtitles *subs, const QList<int> &selected) override;
    QList<SubEntryPtr> getAffectedSubs() const override;
    double getStartTime() const;
    double getFinishTime() const;
    void setRegionToMaximum();
    void setRegionEnabled(bool enabled);

private:
    TimeSelector *from_, *to_;
};

// The full "Select subtitles to work on" panel: by selection, colour, style
// or time range (mode remembered). Port of `JTimeFullSelection`.
class TimeFullSelection : public TimeArea {
    Q_OBJECT
public:
    explicit TimeFullSelection(QWidget *parent = nullptr);
    void updateData(Subtitles *subs, const QList<int> &selected) override;
    QList<SubEntryPtr> getAffectedSubs() const override;
    // The pipette: everything, without touching the remembered mode.
    void forceFullRangeSelection();

private:
    void applyMode(int mode, bool persist);
    QRadioButton *bySelection_, *byColor_, *byStyle_, *byRange_;
    QComboBox *color_, *style_;
    TimeRegion *region_;
};

// A single labelled time spinner in a dialog ("Go to time", "Splitting
// time"). Port of `JTimeSingleSelection`.
class TimeSingleSelectionDialog : public QDialog {
    Q_OBJECT
public:
    TimeSingleSelectionDialog(const Time &initial, const QString &label, const QString &tooltip, const QString &title, QWidget *parent = nullptr);
    Time getTime() const;

private:
    TimeSpinner *spinner_;
};

// A duration rule: ignore, absolute milliseconds or milliseconds per
// character. Port of `JDuration`.
class DurationChooser : public QWidget {
    Q_OBJECT
public:
    // `minimum`: the chooser of the minimum duration (its rate is the fastest
    // acceptable reading speed), otherwise of the maximum (the slowest one).
    explicit DurationChooser(bool minimum, QWidget *parent = nullptr);
    double getAbsTime() const;   // seconds, -1 when not chosen
    double getCPS() const;       // characters per second, -1 when not chosen

private:
    QRadioButton *ignore_, *absolute_, *perChar_;
    QLineEdit *absField_, *cpsField_;
};
