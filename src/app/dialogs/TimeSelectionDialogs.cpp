/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/dialogs/TimeSelectionDialogs.h"

#include <QButtonGroup>
#include <QCheckBox>
#include <QComboBox>
#include <QDialogButtonBox>
#include <QGridLayout>
#include <QIntValidator>
#include <QGroupBox>
#include <QHBoxLayout>
#include <QLabel>
#include <QLineEdit>
#include <QMenu>
#include <QRadioButton>
#include <QToolButton>
#include <QVBoxLayout>

#include "app/Theme.h"
#include "app/ui/TimeSpinner.h"
#include "core/i18n/I18N.h"
#include "core/options/Options.h"
#include "core/options/Prefs.h"
#include "core/subs/Subtitles.h"

// ---- TimeSelector -------------------------------------------------------------------------------

namespace {
QString regionPrefKey(bool isBegin) {
    return isBegin ? QStringLiteral("options.time.selection.region.from") : QStringLiteral("options.time.selection.region.to");
}
}  // namespace

TimeSelector::TimeSelector(bool isBegin, QWidget *parent) : QWidget(parent), isBegin_(isBegin) {
    auto *lay = new QVBoxLayout(this);
    lay->setContentsMargins(0, 0, 0, 0);
    label_ = new QLabel(isBegin ? __("Begin") : __("Finish"), this);
    lay->addWidget(label_);
    auto *row = new QHBoxLayout();
    spinner_ = new TimeSpinner(this);
    dropdown_ = new QToolButton(this);
    dropdown_->setIcon(Theme::icon(QStringLiteral("dropdown")));
    dropdown_->setIconSize(Theme::naturalSize(QStringLiteral("dropdown")));
    dropdown_->setToolTip(__("Use predefined time positions"));
    dropdown_->setAutoRaise(true);
    dropdown_->setPopupMode(QToolButton::InstantPopup);
    auto *menu = new QMenu(this);
    menu->addAction(isBegin ? __("Set time to start of selected subtitles") : __("Set time to end of selected subtitles"), this, [this]() {
        Prefs::set(regionPrefKey(isBegin_), true);
        applyPreset(true);
    });
    menu->addAction(isBegin ? __("Set time to minimum") : __("Set time to maximum"), this, [this]() {
        Prefs::set(regionPrefKey(isBegin_), false);
        applyPreset(false);
    });
    dropdown_->setMenu(menu);
    row->addWidget(spinner_, 1);
    row->addWidget(dropdown_);
    lay->addLayout(row);
}

void TimeSelector::applyPreset(bool selection) {
    if (selection)
        spinner_->setTimeValue(last_);
    else
        spinner_->setTimeValue(isBegin_ ? Time(0.0) : Time(double(Time::MAX_TIME)));
}

void TimeSelector::updateData(const Time &t) {
    last_ = t;
    applyPreset(Prefs::getBoolean(regionPrefKey(isBegin_), true));
}

double TimeSelector::getTime() const { return spinner_->seconds(); }
void TimeSelector::setTime(const Time &t) { spinner_->setTimeValue(t); }

void TimeSelector::setSelectorEnabled(bool enabled) {
    label_->setEnabled(enabled);
    spinner_->setEnabled(enabled);
    dropdown_->setEnabled(enabled);
}

// ---- TimeArea -----------------------------------------------------------------------------------

TimeArea::TimeArea(QWidget *parent) : QWidget(parent) {
    colorRow_ = new QWidget(this);
    auto *rl = new QHBoxLayout(colorRow_);
    rl->setContentsMargins(0, 4, 0, 0);
    rl->setSpacing(12);
    changeColor_ = new QCheckBox(__("Change affected subtitles' color"), colorRow_);
    color_ = new QComboBox(colorRow_);
    for (int i = 0; i < SubEntry::MARK_COUNT; ++i) color_->addItem(SubEntry::markName(i));
    color_->setEnabled(false);
    connect(changeColor_, &QCheckBox::toggled, color_, &QWidget::setEnabled);
    rl->addWidget(changeColor_);
    rl->addWidget(color_, 1);
}

void TimeArea::updateData(Subtitles *subs, const QList<int> &selected) {
    subs_ = subs;
    selected_ = selected;
}

void TimeArea::updateSubsMark(const QList<SubEntryPtr> &affected) const {
    if (!changeColor_->isChecked()) return;
    for (const SubEntryPtr &e : affected) e->setMark(color_->currentIndex());
}

Time TimeArea::findFirstInList() const {
    Time best{double(Time::MAX_TIME)};
    bool found = false;
    for (int r : selected_) {
        if (!subs_ || r < 0 || r >= subs_->size()) continue;
        const Time &t = subs_->get(r)->getStartTime();
        if (t < best) { best = t; found = true; }
    }
    return found ? best : Time(0.0);
}

Time TimeArea::findLastInList() const {
    Time best(0.0);
    for (int r : selected_) {
        if (!subs_ || r < 0 || r >= subs_->size()) continue;
        const Time &t = subs_->get(r)->getFinishTime();
        if (t > best) best = t;
    }
    return best;
}

// ---- TimeRegion ---------------------------------------------------------------------------------

TimeRegion::TimeRegion(QWidget *parent) : TimeArea(parent) {
    auto *lay = new QVBoxLayout(this);
    lay->setContentsMargins(0, 0, 0, 0);
    auto *row = new QHBoxLayout();
    from_ = new TimeSelector(true, this);
    to_ = new TimeSelector(false, this);
    row->addWidget(from_);
    row->addWidget(to_);
    lay->addLayout(row);
    lay->addWidget(colorRow());
}

void TimeRegion::updateData(Subtitles *subs, const QList<int> &selected) {
    TimeArea::updateData(subs, selected);
    from_->updateData(findFirstInList());
    to_->updateData(findLastInList());
}

QList<SubEntryPtr> TimeRegion::getAffectedSubs() const {
    QList<SubEntryPtr> out;
    if (!subs_) return out;
    const double from = from_->getTime(), to = to_->getTime();
    for (const SubEntryPtr &e : subs_->entries()) {
        const double s = e->getStartTime().toSeconds();
        if (s >= from && s <= to) out.append(e);
    }
    return out;
}

double TimeRegion::getStartTime() const { return from_->getTime(); }
double TimeRegion::getFinishTime() const { return to_->getTime(); }

void TimeRegion::setRegionToMaximum() {
    from_->setTime(Time(0.0));
    to_->setTime(Time(double(Time::MAX_TIME)));
}

void TimeRegion::setRegionEnabled(bool enabled) {
    from_->setSelectorEnabled(enabled);
    to_->setSelectorEnabled(enabled);
}

// ---- TimeFullSelection --------------------------------------------------------------------------

namespace {
const QString MODE_KEY = QStringLiteral("options.time.selection");
}

TimeFullSelection::TimeFullSelection(QWidget *parent) : TimeArea(parent) {
    auto *outer = new QVBoxLayout(this);
    outer->setContentsMargins(0, 0, 0, 0);
    auto *box = new QGroupBox(__("Select subtitles to work on"), this);
    auto *lay = new QVBoxLayout(box);
    bySelection_ = new QRadioButton(__("By user selection"), box);
    bySelection_->setToolTip(__("Select subtitles depending on user selection"));
    byColor_ = new QRadioButton(__("By color marking"), box);
    byColor_->setToolTip(__("Select subtitles depending on their color"));
    color_ = new QComboBox(box);
    for (int i = 0; i < SubEntry::MARK_COUNT; ++i) color_->addItem(SubEntry::markName(i));
    color_->setToolTip(__("The subtitle color to use for selection"));
    color_->setEnabled(false);
    byStyle_ = new QRadioButton(__("By theme"), box);
    byStyle_->setToolTip(__("Select subtitles depending on their theme"));
    style_ = new QComboBox(box);
    style_->setToolTip(__("The theme to use for selection"));
    style_->setEnabled(false);
    byRange_ = new QRadioButton(__("By time range"), box);
    byRange_->setToolTip(__("Select subtitles depending on a specified region"));
    lay->addWidget(bySelection_);
    auto *cr = new QHBoxLayout();
    cr->addWidget(byColor_);
    cr->addWidget(color_, 1);
    lay->addLayout(cr);
    auto *sr = new QHBoxLayout();
    sr->addWidget(byStyle_);
    sr->addWidget(style_, 1);
    lay->addLayout(sr);
    lay->addWidget(byRange_);
    region_ = new TimeRegion(box);
    region_->colorRow()->hide();   // our own colour row sits below
    auto *rr = new QHBoxLayout();
    rr->setContentsMargins(16, 0, 0, 0);
    rr->addWidget(region_);
    lay->addLayout(rr);
    outer->addWidget(box);
    outer->addWidget(colorRow());
    auto *group = new QButtonGroup(this);
    group->addButton(bySelection_, 0);
    group->addButton(byColor_, 1);
    group->addButton(byStyle_, 2);
    group->addButton(byRange_, 3);
    connect(group, &QButtonGroup::idClicked, this, [this](int id) { applyMode(id, true); });
    bySelection_->setChecked(true);
    applyMode(0, false);
}

void TimeFullSelection::applyMode(int mode, bool persist) {
    QRadioButton *buttons[] = {bySelection_, byColor_, byStyle_, byRange_};
    buttons[std::clamp(mode, 0, 3)]->setChecked(true);
    color_->setEnabled(mode == 1);
    style_->setEnabled(mode == 2);
    region_->setRegionEnabled(mode == 3);
    if (persist) Prefs::set(MODE_KEY, mode);
}

void TimeFullSelection::updateData(Subtitles *subs, const QList<int> &selected) {
    TimeArea::updateData(subs, selected);
    region_->updateData(subs, selected);
    const int prev = style_->currentIndex();
    style_->clear();
    if (subs)
        for (const SubStylePtr &s : subs->getStyleList().all()) style_->addItem(s->getName());
    style_->setCurrentIndex(prev >= 0 && prev < style_->count() ? prev : 0);
    int mode = Prefs::getInt(MODE_KEY, 0);
    if (style_->count() < 2) {
        byStyle_->setEnabled(false);
        style_->setEnabled(false);
        if (mode == 2) mode = 0;
    } else
        byStyle_->setEnabled(true);
    applyMode(mode, false);
}

QList<SubEntryPtr> TimeFullSelection::getAffectedSubs() const {
    QList<SubEntryPtr> out;
    if (!subs_) return out;
    if (bySelection_->isChecked()) {
        for (int r : selected_)
            if (r >= 0 && r < subs_->size()) out.append(subs_->get(r));
    } else if (byColor_->isChecked()) {
        for (const SubEntryPtr &e : subs_->entries())
            if (e->getMark() == color_->currentIndex()) out.append(e);
    } else if (byStyle_->isChecked()) {
        const int idx = style_->currentIndex();
        if (idx >= 0 && idx < subs_->getStyleList().size()) {
            const SubStylePtr st = subs_->getStyleList().get(idx);
            for (const SubEntryPtr &e : subs_->entries())
                if (e->getStyle() == st) out.append(e);
        }
    } else
        out = region_->getAffectedSubs();
    return out;
}

void TimeFullSelection::forceFullRangeSelection() {
    byRange_->setChecked(true);
    color_->setEnabled(false);
    style_->setEnabled(false);
    region_->setRegionEnabled(true);
    region_->setRegionToMaximum();
}

// ---- TimeSingleSelectionDialog ------------------------------------------------------------------

TimeSingleSelectionDialog::TimeSingleSelectionDialog(const Time &initial, const QString &label, const QString &tooltip, const QString &title, QWidget *parent) : QDialog(parent) {
    setWindowTitle(title);
    setModal(true);
    auto *lay = new QVBoxLayout(this);
    lay->addWidget(new QLabel(label, this));
    spinner_ = new TimeSpinner(this);
    spinner_->setTimeValue(initial);
    spinner_->setToolTip(tooltip);
    lay->addWidget(spinner_);
    auto *buttons = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel, this);
    connect(buttons, &QDialogButtonBox::accepted, this, &QDialog::accept);
    connect(buttons, &QDialogButtonBox::rejected, this, &QDialog::reject);
    lay->addWidget(buttons);
}

Time TimeSingleSelectionDialog::getTime() const { return spinner_->getTimeValue(); }

// ---- DurationChooser ----------------------------------------------------------------------------

DurationChooser::DurationChooser(bool minimum, QWidget *parent) : QWidget(parent) {
    auto *lay = new QGridLayout(this);
    lay->setContentsMargins(0, 0, 0, 0);
    ignore_ = new QRadioButton(__("Ignore"), this);
    ignore_->setToolTip(__("Do not use this"));
    ignore_->setChecked(true);
    absolute_ = new QRadioButton(__("Absolute time  (in milliseconds)"), this);
    absolute_->setToolTip(__("Define the duration time in absolute milliseconds"));
    absField_ = new QLineEdit(QStringLiteral("4000"), this);
    absField_->setValidator(new QIntValidator(0, 9999999, absField_));
    absField_->setToolTip(__("Time in milliseconds"));
    absField_->setEnabled(false);
    perChar_ = new QRadioButton(__("Characters per second"), this);
    perChar_->setToolTip(minimum ? __("The fastest reading speed: the subtitle lasts at least its characters divided by this value, in seconds")
                                 : __("The slowest reading speed: the subtitle lasts at most its characters divided by this value, in seconds"));
    cpsField_ = new QLineEdit(QString::number(minimum ? Options::getMaxCPS() : 5), this);
    cpsField_->setValidator(new QIntValidator(1, 999, cpsField_));
    cpsField_->setToolTip(__("Characters per second"));
    cpsField_->setEnabled(false);
    lay->addWidget(ignore_, 0, 0, 1, 2);
    lay->addWidget(absolute_, 1, 0);
    lay->addWidget(absField_, 1, 1);
    lay->addWidget(perChar_, 2, 0);
    lay->addWidget(cpsField_, 2, 1);
    auto sync = [this]() {
        absField_->setEnabled(absolute_->isChecked());
        cpsField_->setEnabled(perChar_->isChecked());
    };
    connect(ignore_, &QRadioButton::toggled, this, sync);
    connect(absolute_, &QRadioButton::toggled, this, sync);
    connect(perChar_, &QRadioButton::toggled, this, sync);
}

double DurationChooser::getAbsTime() const {
    if (!absolute_->isChecked()) return -1;
    bool ok = false;
    const double v = absField_->text().toDouble(&ok);
    return ok ? v / 1000 : -1;
}

double DurationChooser::getCPS() const {
    if (!perChar_->isChecked()) return -1;
    bool ok = false;
    const double v = cpsField_->text().toDouble(&ok);
    return ok && v > 0 ? v : -1;
}
