/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/ui/OverStylesBar.h"

#include <QFontDatabase>
#include <QHBoxLayout>
#include <QMenu>

#include "app/Theme.h"
#include "core/i18n/I18N.h"

OverStylesBar::OverStylesBar(QWidget *parent) : QWidget(parent) {
    auto *outer = new QVBoxLayout(this);
    outer->setContentsMargins(0, 0, 0, 0);
    outer->setSpacing(0);
    fontRow_ = new QWidget(this);
    auto *row = new QHBoxLayout(fontRow_);
    row->setContentsMargins(0, 0, 0, 0);
    row->setSpacing(2);
    if (SubStyle::fontNames().isEmpty())
        SubStyle::setFontNames(QFontDatabase::families());
    fontName_ = new TriComboBox(SubStyle::fontNames(), StyleType::FONTNAME, fontRow_);
    fontName_->setToolTip(__("Font name"));
    fontName_->setSizeAdjustPolicy(QComboBox::AdjustToMinimumContentsLengthWithIcon);
    fontName_->setMinimumContentsLength(12);
    QStringList sizes;
    for (int s : SubStyle::FontSizes) sizes.append(QString::number(s));
    fontSize_ = new TriComboBox(sizes, StyleType::FONTSIZE, fontRow_);
    fontSize_->setToolTip(__("Font size"));
    row->addWidget(fontName_, 1);
    row->addWidget(fontSize_);
    row->addSpacing(6);
    const char *icons[4] = {"bold", "italics", "underline", "strike"};
    const StyleType::Id types[4] = {StyleType::BOLD, StyleType::ITALIC, StyleType::UNDERLINE, StyleType::STRIKETHROUGH};
    const QString tips[4] = {__("Bold"), __("Italic"), __("Underline"), __("Strikethrough")};
    for (int i = 0; i < 4; ++i) {
        toggles_[i] = new TriToggleButton(QLatin1String(icons[i]), types[i], fontRow_);
        toggles_[i]->setToolTip(tips[i]);
        row->addWidget(toggles_[i]);
    }
    row->addSpacing(18);
    picker_ = new QToolButton(fontRow_);
    picker_->setAutoRaise(true);
    picker_->setIcon(Theme::icon(QStringLiteral("colorpicker")));
    picker_->setIconSize(QSize(STYLE_ICON_SIZE, STYLE_ICON_SIZE));
    picker_->setToolTip(__("Choose which color to edit"));
    row->addWidget(picker_);
    colorStack_ = new QStackedWidget(fontRow_);
    const StyleType::Id ctypes[4] = {StyleType::PRIMARY, StyleType::SECONDARY, StyleType::OUTLINE, StyleType::SHADOW};
    const int alphas[4] = {255, 180, 180, 180};
    for (int i = 0; i < 4; ++i) {
        colors_[i] = new TriColorButton(AlphaColor(QColor(Qt::white), alphas[i]), ctypes[i], colorStack_);
        colorStack_->addWidget(colors_[i]);
    }
    colorStack_->setCurrentIndex(0);
    row->addWidget(colorStack_);
    connect(picker_, &QToolButton::clicked, this, [this]() {
        QMenu menu(this);
        menu.setStyleSheet(QStringLiteral("QMenu { icon-size: %1px; }").arg(STYLE_ICON_SIZE));   // the swatches at their size
        for (int i = 0; i < 4; ++i) {
            QAction *a = menu.addAction(colors_[i]->icon(), TriColorButton::labelFor(StyleType::Id(StyleType::PRIMARY + i)));
            connect(a, &QAction::triggered, this, [this, i]() { switchToColor(i); });
        }
        menu.exec(picker_->mapToGlobal(QPoint(0, picker_->height())));
    });
    row->addSpacing(18);
    direction_ = new TriDirectionButton(fontRow_);
    row->addWidget(direction_);
    outer->addWidget(fontRow_);
}

void OverStylesBar::switchToColor(int slot) {
    colorStack_->setCurrentIndex(slot);
}

void OverStylesBar::setStyleChangeListener(StyleChangeFn fn) {
    fontName_->setListener(fn);
    fontSize_->setListener(fn);
    for (auto *t : toggles_) t->setListener(fn);
    for (auto *c : colors_) c->setListener(fn);
    direction_->setListener(fn);
}

void OverStylesBar::setFontPanelVisible(bool v) {
    fontRow_->setVisible(v);
}

void OverStylesBar::updateVisualData(const SubEntry &entry, int start, int end) {
    fontName_->setData(entry.overValue(StyleType::FONTNAME, start, end));
    fontSize_->setData(entry.overValue(StyleType::FONTSIZE, start, end));
    const StyleType::Id types[4] = {StyleType::BOLD, StyleType::ITALIC, StyleType::UNDERLINE, StyleType::STRIKETHROUGH};
    for (int i = 0; i < 4; ++i) toggles_[i]->setData(entry.overValue(types[i], start, end));
    const StyleType::Id ctypes[4] = {StyleType::PRIMARY, StyleType::SECONDARY, StyleType::OUTLINE, StyleType::SHADOW};
    for (int i = 0; i < 4; ++i) colors_[i]->setData(entry.overValue(ctypes[i], start, end));
    direction_->setData(entry.overValue(StyleType::DIRECTION, start, end));
}
