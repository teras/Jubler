/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QStackedWidget>
#include <QWidget>

#include "app/ui/StyleWidgets.h"
#include "core/subs/SubEntry.h"

// The inline-override toolbar above the editor text: font name/size combos,
// bold/italic/underline/strike toggles, one colour slot at a time (with a
// slot picker) and the alignment button. Port of `JOverStyles`.
class OverStylesBar : public QWidget {
    Q_OBJECT
public:
    explicit OverStylesBar(QWidget *parent = nullptr);
    void setStyleChangeListener(StyleChangeFn fn);
    // Show the values in effect over [start, end] of the entry's text.
    void updateVisualData(const SubEntry &entry, int start, int end);
    void setFontPanelVisible(bool v);

private:
    void switchToColor(int slot);

    QWidget *fontRow_;
    TriComboBox *fontName_;
    TriComboBox *fontSize_;
    TriToggleButton *toggles_[4];
    TriColorButton *colors_[4];
    QStackedWidget *colorStack_;
    QToolButton *picker_;
    TriDirectionButton *direction_;
};
