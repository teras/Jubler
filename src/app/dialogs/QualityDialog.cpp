/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/dialogs/QualityDialog.h"

#include <QCheckBox>
#include <QComboBox>
#include <QDialogButtonBox>
#include <QDoubleSpinBox>
#include <QFormLayout>
#include <QGridLayout>
#include <QLabel>
#include <QSpinBox>
#include <QVBoxLayout>

#include "core/i18n/I18N.h"
#include "core/options/Options.h"
#include "core/subs/SubEntry.h"

namespace {
// Any number of decimals, shown without trailing zeros (the Java FloatRangeFilter field).
class FreeDecimalsSpinBox : public QDoubleSpinBox {
public:
    using QDoubleSpinBox::QDoubleSpinBox;
    QString textFromValue(double value) const override { return locale().toString(float(value), 'g', QLocale::FloatingPointShortest); }
};
}  // namespace

QualityDialog::QualityDialog(QWidget *parent) : QDialog(parent) {
    setWindowTitle(__("Quality settings"));
    setModal(true);
    auto *lay = new QVBoxLayout(this);

    auto *colorRow = new QFormLayout();
    auto *color = new QComboBox(this);
    for (int i = 1; i < SubEntry::MARK_COUNT; ++i) color->addItem(SubEntry::markName(i));
    color->setCurrentIndex(Options::getErrorColor() - 1);
    connect(color, &QComboBox::currentIndexChanged, this, [](int idx) { if (idx >= 0) Options::setErrorColor(idx + 1); });
    colorRow->addRow(__("Color to use"), color);
    lay->addLayout(colorRow);

    auto *checks = new QGridLayout();
    auto check = [&](const QString &text, bool value, void (*setter)(bool), int row, int col) {
        auto *c = new QCheckBox(text, this);
        c->setChecked(value);
        connect(c, &QCheckBox::toggled, this, [setter](bool on) { setter(on); });
        checks->addWidget(c, row, col);
    };
    check(__("Treat space as character"), Options::isSpaceChars(), &Options::setSpaceChars, 0, 0);
    check(__("Treat newline as character"), Options::isNewlineChars(), &Options::setNewlineChars, 0, 1);
    check(__("Prefer compact subtitles"), Options::isCompactSubs(), &Options::setCompactSubs, 1, 0);
    check(__("Treat non-alphanumeric as character"), Options::isOtherChars(), &Options::setOtherChars, 1, 1);
    lay->addLayout(checks);

    auto *grid = new QGridLayout();
    auto intField = [&](const QString &label, int min, int max, int value, void (*setter)(int), int row, int col) {
        auto *s = new QSpinBox(this);
        s->setRange(min, max);
        s->setValue(value);
        connect(s, &QSpinBox::valueChanged, this, [setter](int v) { setter(v); });
        grid->addWidget(new QLabel(label, this), row, col * 2);
        grid->addWidget(s, row, col * 2 + 1);
    };
    auto floatField = [&](const QString &label, double min, double max, double value, void (*setter)(float), int row, int col) {
        auto *s = new FreeDecimalsSpinBox(this);
        s->setRange(min, max);
        s->setDecimals(6);
        s->setValue(value);
        connect(s, &QDoubleSpinBox::valueChanged, this, [setter](double v) { setter(float(v)); });
        grid->addWidget(new QLabel(label, this), row, col * 2);
        grid->addWidget(s, row, col * 2 + 1);
    };
    intField(__("Maximum number of lines per subtitle"), 1, 100, Options::getMaxLines(), &Options::setMaxLines, 0, 0);
    floatField(__("Line fill percentage"), 0, 100, Options::getFillPercent(), &Options::setFillPercent, 0, 1);
    intField(__("Maximum number of characters per line"), 1, 1000, Options::getMaxLineLength(), &Options::setMaxLineLength, 1, 0);
    intField(__("Maximum number of characters per second"), 1, 1000, Options::getMaxCPS(), &Options::setMaxCPS, 2, 0);
    floatField(__("Minimum duration in seconds"), 0, 1000, Options::getMinDuration(), &Options::setMinDuration, 3, 0);
    floatField(__("Maximum duration in seconds"), 0, 1000, Options::getMaxDuration(), &Options::setMaxDuration, 3, 1);
    lay->addLayout(grid);

    auto *buttons = new QDialogButtonBox(QDialogButtonBox::Ok, this);
    connect(buttons, &QDialogButtonBox::accepted, this, &QDialog::accept);
    lay->addWidget(buttons);
}
