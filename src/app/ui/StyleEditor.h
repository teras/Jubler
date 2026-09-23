/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QCheckBox>
#include <QComboBox>
#include <QDialog>
#include <QLineEdit>
#include <QPushButton>
#include <QSpinBox>
#include <QDoubleSpinBox>
#include <QTextEdit>
#include <QToolButton>

#include "app/ui/StyleWidgets.h"
#include "core/style/SubStyle.h"

// The style editor dialog: name (with clone/delete/save-default), font,
// colours, live preview and the advanced ASS properties. Port of
// `JStyleEditor`.
class StyleEditor : public QDialog {
    Q_OBJECT
public:
    struct Result {
        bool cancelled = true;
        bool deleted = false;
        SubStylePtr style;   // the edited style (may be a clone)
    };

    StyleEditor(SubStyleList &styles, QWidget *parent = nullptr);
    // Edit `style` modally; the document's style list is re-sorted on close
    // (Default first).
    Result editStyle(const SubStylePtr &style);
    // The Advanced panel state (the Java kept one editor per subtitle editor).
    void setAdvancedShown(bool shown);
    bool advancedShown() const;
    void reject() override;

private:
    void setValues();
    void readBasicValues();
    void readOtherValues();
    void updatePreview();
    void commitName();
    void cloneStyle();
    void deleteStyle();
    void saveDefault();
    void sortStyles();
    void addFontIfMissing(const QString &name);

    SubStyleList &styles_;
    SubStylePtr current_;
    SubStylePtr clone_;
    bool suppress_ = false;
    bool deleted_ = false;
    bool cancelled_ = true;

    QLineEdit *name_;
    QWidget *dirty_;
    QPushButton *cloneB_, *deleteB_;
    QToolButton *saveB_;
    QComboBox *fontName_, *fontSize_;
    QToolButton *bold_, *italic_, *underline_, *strike_;
    QToolButton *colors_[4];
    std::optional<AlphaColor> colorValues_[4];
    QTextEdit *preview_;
    QWidget *advanced_;
    QComboBox *borderStyle_;
    QDoubleSpinBox *borderSize_, *shadowSize_, *angle_, *spacing_;
    QSpinBox *leftMargin_, *rightMargin_, *vertical_, *xscale_, *yscale_;
    DirectionGrid *direction_;
    QCheckBox *advancedSelect_;
};
