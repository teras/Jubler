/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/dialogs/PasterDialog.h"

#include <QCheckBox>
#include <QComboBox>
#include <QDialogButtonBox>
#include <QLabel>
#include <QVBoxLayout>

#include "app/ui/TimeSpinner.h"
#include "core/i18n/I18N.h"
#include "core/subs/SubEntry.h"

PasterDialog::PasterDialog(const Time &initial, QWidget *parent) : QDialog(parent) {
    setWindowTitle(__("Paste special options"));
    setModal(true);
    auto *lay = new QVBoxLayout(this);
    lay->addWidget(new QLabel(__("Where to start pasting subtitles"), this));
    spinner_ = new TimeSpinner(this);
    spinner_->setTimeValue(initial);
    lay->addWidget(spinner_);
    changeColor_ = new QCheckBox(__("Change Color"), this);
    changeColor_->setToolTip(__("Enable this option if you want to change the default color of the pasted subtitles"));
    lay->addWidget(changeColor_);
    color_ = new QComboBox(this);
    for (int i = 0; i < SubEntry::MARK_COUNT; ++i) color_->addItem(SubEntry::markName(i));
    color_->setToolTip(__("The color to use for the pasted subtitles"));
    color_->setEnabled(false);
    connect(changeColor_, &QCheckBox::toggled, color_, &QWidget::setEnabled);
    lay->addWidget(color_);
    auto *buttons = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel, this);
    connect(buttons, &QDialogButtonBox::accepted, this, &QDialog::accept);
    connect(buttons, &QDialogButtonBox::rejected, this, &QDialog::reject);
    lay->addWidget(buttons);
}

int PasterDialog::getMark() const { return changeColor_->isChecked() ? color_->currentIndex() : -1; }
Time PasterDialog::getStartTime() const { return spinner_->getTimeValue(); }
