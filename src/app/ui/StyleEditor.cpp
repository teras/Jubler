/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/ui/StyleEditor.h"

#include <QApplication>
#include <QDialogButtonBox>
#include <QFontDatabase>
#include <QFormLayout>
#include <QGridLayout>
#include <QGroupBox>
#include <QHBoxLayout>
#include <QLabel>
#include <QMessageBox>
#include <QRegularExpression>
#include <QTextDocument>
#include <QVBoxLayout>
#include <cmath>

#include "app/Theme.h"
#include "core/i18n/I18N.h"
#include "core/options/Prefs.h"
#include "core/style/WebSafeFonts.h"

StyleEditor::StyleEditor(SubStyleList &styles, QWidget *parent) : QDialog(parent), styles_(styles) {
    setWindowTitle(__("Style Editor"));
    setModal(true);
    auto *outer = new QVBoxLayout(this);

    // Name row.
    auto *nameRow = new QHBoxLayout;
    dirty_ = new QWidget(this);
    dirty_->setAutoFillBackground(true);
    auto *dl = new QHBoxLayout(dirty_);
    dl->setContentsMargins(3, 3, 3, 3);
    name_ = new QLineEdit(dirty_);
    name_->setToolTip(__("The name of this style. Remember to hit [RETURN] to store the name"));
    dl->addWidget(name_);
    nameRow->addWidget(dirty_, 1);
    cloneB_ = new QPushButton(__("Clone"), this);
    cloneB_->setToolTip(__("Create a new style based on the current one"));
    deleteB_ = new QPushButton(__("Delete"), this);
    deleteB_->setToolTip(__("Delete the current style"));
    saveB_ = new QToolButton(this);
    saveB_->setIcon(Theme::icon(QStringLiteral("save")));
    saveB_->setIconSize(Theme::naturalSize(QStringLiteral("save")));
    saveB_->setToolTip(__("Save default style"));
    nameRow->addWidget(cloneB_);
    nameRow->addWidget(deleteB_);
    nameRow->addWidget(saveB_);
    outer->addLayout(nameRow);
    connect(name_, &QLineEdit::textEdited, this, [this]() {
        QPalette p = dirty_->palette();
        p.setColor(QPalette::Window, Qt::red);
        dirty_->setPalette(p);
    });
    connect(name_, &QLineEdit::returnPressed, this, &StyleEditor::commitName);
    connect(cloneB_, &QPushButton::clicked, this, &StyleEditor::cloneStyle);
    connect(deleteB_, &QPushButton::clicked, this, &StyleEditor::deleteStyle);
    connect(saveB_, &QToolButton::clicked, this, &StyleEditor::saveDefault);

    // Font group.
    auto *fontGroup = new QGroupBox(__("Font"), this);
    auto *fl = new QHBoxLayout(fontGroup);
    fontName_ = new QComboBox(fontGroup);
    fontName_->setToolTip(__("Font name"));
    QStringList families = SubStyle::fontNames().isEmpty() ? QFontDatabase::families() : SubStyle::fontNames();
    if (SubStyle::fontNames().isEmpty()) SubStyle::setFontNames(families);
    families += WebSafeFonts::COMMON;
    QSet<QString> seen;
    QStringList uniq;
    for (const QString &f : families)
        if (!seen.contains(f.toLower())) { seen.insert(f.toLower()); uniq.append(f); }
    std::sort(uniq.begin(), uniq.end(), [](const QString &a, const QString &b) { return a.compare(b, Qt::CaseInsensitive) < 0; });
    fontName_->addItems(uniq);
    fontSize_ = new QComboBox(fontGroup);
    fontSize_->setToolTip(__("Font size"));
    for (int s : SubStyle::FontSizes) fontSize_->addItem(QString::number(s));
    fl->addWidget(fontName_, 1);
    fl->addWidget(fontSize_);
    auto mk = [&](const char *icon, const QString &tip) {
        auto *b = new QToolButton(fontGroup);
        b->setCheckable(true);
        b->setAutoRaise(true);
        b->setIcon(Theme::icon(QLatin1String(icon)));
        b->setIconSize(QSize(STYLE_ICON_SIZE, STYLE_ICON_SIZE));
        b->setToolTip(tip);
        fl->addWidget(b);
        connect(b, &QToolButton::toggled, this, [this]() { readBasicValues(); });
        return b;
    };
    bold_ = mk("bold", __("Bold"));
    italic_ = mk("italics", __("Italic"));
    underline_ = mk("underline", __("Underline"));
    strike_ = mk("strike", __("Strikethrough"));
    connect(fontName_, &QComboBox::currentIndexChanged, this, [this]() { readBasicValues(); });
    connect(fontSize_, &QComboBox::currentIndexChanged, this, [this]() { readBasicValues(); });
    outer->addWidget(fontGroup);

    // Colours group.
    auto *colorGroup = new QGroupBox(__("Colors"), this);
    auto *cl = new QGridLayout(colorGroup);
    cl->setSpacing(4);
    const StyleType::Id ctypes[4] = {StyleType::PRIMARY, StyleType::SECONDARY, StyleType::OUTLINE, StyleType::SHADOW};
    for (int i = 0; i < 4; ++i) {
        colors_[i] = new QToolButton(colorGroup);
        colors_[i]->setText(TriColorButton::labelFor(ctypes[i]));
        colors_[i]->setToolTip(TriColorButton::tooltipFor(ctypes[i]));
        colors_[i]->setToolButtonStyle(Qt::ToolButtonTextBesideIcon);
        colors_[i]->setIconSize(QSize(STYLE_ICON_SIZE, STYLE_ICON_SIZE));
        colorValues_[i] = AlphaColor(QColor(Qt::white), 180);
        colors_[i]->setIcon(alphaColorIcon(colorValues_[i], STYLE_ICON_SIZE));
        cl->addWidget(colors_[i], 0, i);
        connect(colors_[i], &QToolButton::clicked, this, [this, i]() {
            const auto picked = AlphaColorDialog::pick(this, colorValues_[i] ? *colorValues_[i] : AlphaColor(QColor(Qt::white), 180));
            if (!picked) return;
            colorValues_[i] = *picked;
            colors_[i]->setIcon(alphaColorIcon(colorValues_[i], STYLE_ICON_SIZE));
            readBasicValues();
        });
    }
    outer->addWidget(colorGroup);

    // Preview.
    preview_ = new QTextEdit(this);
    preview_->setReadOnly(true);
    preview_->setToolTip(__("Demo subtitles text"));
    preview_->setFixedHeight(80);   // until the first preview
    preview_->setVerticalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
    preview_->setHorizontalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
    preview_->setFrameShape(QFrame::StyledPanel);
    outer->addWidget(preview_);

    // Advanced panel.
    advanced_ = new QWidget(this);
    auto *al = new QVBoxLayout(advanced_);
    al->setContentsMargins(0, 0, 0, 0);
    auto *row1 = new QHBoxLayout;
    auto *borderGroup = new QGroupBox(__("Border"), advanced_);
    auto *bf = new QFormLayout(borderGroup);
    borderStyle_ = new QComboBox(borderGroup);
    borderStyle_->addItems({__("Outline"), __("Opaque box")});
    borderStyle_->setToolTip(__("Border style"));
    borderSize_ = new QDoubleSpinBox(borderGroup);
    borderSize_->setRange(0, 100); borderSize_->setDecimals(3); borderSize_->setToolTip(__("Border size"));
    shadowSize_ = new QDoubleSpinBox(borderGroup);
    shadowSize_->setRange(0, 100); shadowSize_->setDecimals(3); shadowSize_->setToolTip(__("Shadow size"));
    bf->addRow(__("Style"), borderStyle_);
    bf->addRow(__("Size"), borderSize_);
    bf->addRow(__("Shadow"), shadowSize_);
    row1->addWidget(borderGroup);
    auto *marginGroup = new QGroupBox(__("Margins transformations"), advanced_);
    auto *mf = new QFormLayout(marginGroup);
    leftMargin_ = new QSpinBox(marginGroup); leftMargin_->setRange(0, 1000); leftMargin_->setToolTip(__("Left margin"));
    rightMargin_ = new QSpinBox(marginGroup); rightMargin_->setRange(0, 1000); rightMargin_->setToolTip(__("Right margin"));
    vertical_ = new QSpinBox(marginGroup); vertical_->setRange(0, 1000); vertical_->setToolTip(__("Vertical margin"));
    mf->addRow(__("Left"), leftMargin_);
    mf->addRow(__("Right"), rightMargin_);
    mf->addRow(__("Vertical"), vertical_);
    row1->addWidget(marginGroup);
    al->addLayout(row1);
    auto *row2 = new QHBoxLayout;
    auto *transGroup = new QGroupBox(__("Font transformations"), advanced_);
    auto *tf = new QFormLayout(transGroup);
    angle_ = new QDoubleSpinBox(transGroup); angle_->setRange(-180, 180); angle_->setDecimals(3); angle_->setToolTip(__("Font angle"));
    spacing_ = new QDoubleSpinBox(transGroup); spacing_->setRange(0, 100); spacing_->setDecimals(3); spacing_->setToolTip(__("Spacing in font"));
    xscale_ = new QSpinBox(transGroup); xscale_->setRange(1, 1000); xscale_->setToolTip(__("Scaling % on X axis"));
    yscale_ = new QSpinBox(transGroup); yscale_->setRange(1, 1000); yscale_->setToolTip(__("Scaling % on Y axis"));
    tf->addRow(__("Angle"), angle_);
    tf->addRow(__("Spacing"), spacing_);
    tf->addRow(__("X Scale"), xscale_);
    tf->addRow(__("Y Scale"), yscale_);
    row2->addWidget(transGroup);
    auto *alignGroup = new QGroupBox(__("Alignment"), advanced_);
    auto *agl = new QVBoxLayout(alignGroup);
    direction_ = new DirectionGrid(alignGroup);
    agl->addWidget(direction_);
    row2->addWidget(alignGroup);
    al->addLayout(row2);
    advanced_->hide();
    outer->addWidget(advanced_);

    // Bottom row.
    auto *bottom = new QHBoxLayout;
    advancedSelect_ = new QCheckBox(__("Advanced options"), this);
    advancedSelect_->setToolTip(__("Display the advanced options for this style"));
    // As the Java: the "tab" icon in place of the check mark, the text before it.
    advancedSelect_->setLayoutDirection(Qt::RightToLeft);
    advancedSelect_->setStyleSheet(QStringLiteral("QCheckBox { spacing: 10px; } QCheckBox::indicator { width: %1px; height: %1px; image: url(:/icons/tab.svg); }")
                                       .arg(Theme::naturalSize(QStringLiteral("tab")).width()));
    connect(advancedSelect_, &QCheckBox::toggled, this, [this](bool on) {
        advanced_->setVisible(on);
        advancedSelect_->setToolTip(on ? __("Hide the advanced options for this style") : __("Display the advanced options for this style"));
        adjustSize();
    });
    bottom->addWidget(advancedSelect_);
    bottom->addStretch(1);
    auto *buttons = new QDialogButtonBox(this);
    QPushButton *ok = buttons->addButton(__("OK"), QDialogButtonBox::AcceptRole);
    QPushButton *cancel = buttons->addButton(__("Cancel"), QDialogButtonBox::RejectRole);
    bottom->addWidget(buttons);
    outer->addLayout(bottom);
    connect(ok, &QPushButton::clicked, this, [this]() { cancelled_ = false; readOtherValues(); accept(); });
    connect(cancel, &QPushButton::clicked, this, &QDialog::reject);
}

// Cancel, Escape and the window close button all discard a pending clone.
void StyleEditor::reject() {
    if (clone_) { styles_.remove(clone_); clone_.reset(); }
    cancelled_ = true;
    QDialog::reject();
}

StyleEditor::Result StyleEditor::editStyle(const SubStylePtr &style) {
    current_ = style;
    clone_.reset();
    deleted_ = false;
    cancelled_ = true;
    cloneB_->setEnabled(true);
    setValues();
    exec();
    if (!deleted_ && !cancelled_)
        readOtherValues();
    sortStyles();
    Result r;
    r.cancelled = cancelled_ && !deleted_;
    r.deleted = deleted_;
    r.style = current_;
    return r;
}

void StyleEditor::sortStyles() {
    QList<SubStylePtr> all = styles_.all();
    if (all.isEmpty()) return;
    SubStylePtr def = all.takeFirst();
    std::sort(all.begin(), all.end(), [](const SubStylePtr &a, const SubStylePtr &b) { return a->getName().compare(b->getName()) < 0; });
    styles_.clearList();
    styles_.add(def);
    for (const SubStylePtr &s : all) styles_.add(s);
}

void StyleEditor::addFontIfMissing(const QString &name) {
    if (name.isEmpty()) return;
    for (int i = 0; i < fontName_->count(); ++i)
        if (fontName_->itemText(i).compare(name, Qt::CaseInsensitive) == 0) return;
    fontName_->addItem(name);
}

void StyleEditor::setValues() {
    suppress_ = true;
    name_->setText(current_->getName());
    QPalette p = dirty_->palette();
    p.setColor(QPalette::Window, Qt::green);
    dirty_->setPalette(p);
    const bool isDefault = current_->isDefault() || current_ == styles_.get(0);
    name_->setReadOnly(isDefault);
    deleteB_->setEnabled(!isDefault);
    saveB_->setEnabled(isDefault);
    for (const SubStylePtr &s : styles_.all()) addFontIfMissing(s->fontName());
    addFontIfMissing(current_->fontName());
    int fi = -1;
    for (int i = 0; i < fontName_->count(); ++i)
        if (fontName_->itemText(i).compare(current_->fontName(), Qt::CaseInsensitive) == 0) { fi = i; break; }
    fontName_->setCurrentIndex(fi);
    const int si = fontSize_->findText(QString::number(current_->fontSize()));
    if (si < 0) { fontSize_->addItem(QString::number(current_->fontSize())); fontSize_->setCurrentIndex(fontSize_->count() - 1); }
    else fontSize_->setCurrentIndex(si);
    bold_->setChecked(current_->flag(StyleType::BOLD));
    italic_->setChecked(current_->flag(StyleType::ITALIC));
    underline_->setChecked(current_->flag(StyleType::UNDERLINE));
    strike_->setChecked(current_->flag(StyleType::STRIKETHROUGH));
    const StyleType::Id ctypes[4] = {StyleType::PRIMARY, StyleType::SECONDARY, StyleType::OUTLINE, StyleType::SHADOW};
    for (int i = 0; i < 4; ++i) {
        colorValues_[i] = current_->color(ctypes[i]);
        colors_[i]->setIcon(alphaColorIcon(colorValues_[i], STYLE_ICON_SIZE));
    }
    // Border style 0 = outline, 1 = opaque box (a raw ASS 3 is a box too).
    const int bs = current_->integral(StyleType::BORDERSTYLE);
    borderStyle_->setCurrentIndex(bs == 1 || bs == 3 ? 1 : 0);
    // A loaded value outside a spinner's usual range widens it: OK must not
    // change values the user did not touch (as the Java spinners).
    const auto real = [this](QDoubleSpinBox *box, StyleType::Id type, double min, double max) {
        const double v = current_->real(type);
        box->setRange(std::min(min, v), std::max(max, v));
        box->setValue(v);
    };
    const auto integral = [this](QSpinBox *box, StyleType::Id type, int min, int max) {
        const int v = current_->integral(type);
        box->setRange(std::min(min, v), std::max(max, v));
        box->setValue(v);
    };
    real(borderSize_, StyleType::BORDERSIZE, 0, 100);
    real(shadowSize_, StyleType::SHADOWSIZE, 0, 100);
    integral(leftMargin_, StyleType::LEFTMARGIN, 0, 1000);
    integral(rightMargin_, StyleType::RIGHTMARGIN, 0, 1000);
    integral(vertical_, StyleType::VERTICAL, 0, 1000);
    real(angle_, StyleType::ANGLE, -180, 180);
    real(spacing_, StyleType::SPACING, 0, 100);
    integral(xscale_, StyleType::XSCALE, 1, 1000);
    integral(yscale_, StyleType::YSCALE, 1, 1000);
    direction_->setDirection(current_->direction());
    suppress_ = false;
    updatePreview();
}

void StyleEditor::readBasicValues() {
    if (suppress_ || !current_) return;
    if (fontName_->currentIndex() >= 0) current_->set(StyleType::FONTNAME, StyleValue(fontName_->currentText()));
    current_->set(StyleType::FONTSIZE, fontSize_->currentText());
    current_->set(StyleType::BOLD, StyleValue(bold_->isChecked()));
    current_->set(StyleType::ITALIC, StyleValue(italic_->isChecked()));
    current_->set(StyleType::UNDERLINE, StyleValue(underline_->isChecked()));
    current_->set(StyleType::STRIKETHROUGH, StyleValue(strike_->isChecked()));
    const StyleType::Id ctypes[4] = {StyleType::PRIMARY, StyleType::SECONDARY, StyleType::OUTLINE, StyleType::SHADOW};
    for (int i = 0; i < 4; ++i)
        if (colorValues_[i]) current_->set(ctypes[i], StyleValue(*colorValues_[i]));
    updatePreview();
}

void StyleEditor::readOtherValues() {
    if (!current_) return;
    current_->set(StyleType::BORDERSTYLE, StyleValue(borderStyle_->currentIndex()));
    current_->set(StyleType::BORDERSIZE, StyleValue(float(borderSize_->value())));
    current_->set(StyleType::SHADOWSIZE, StyleValue(float(shadowSize_->value())));
    current_->set(StyleType::LEFTMARGIN, StyleValue(leftMargin_->value()));
    current_->set(StyleType::RIGHTMARGIN, StyleValue(rightMargin_->value()));
    current_->set(StyleType::VERTICAL, StyleValue(vertical_->value()));
    current_->set(StyleType::ANGLE, StyleValue(float(angle_->value())));
    current_->set(StyleType::SPACING, StyleValue(float(spacing_->value())));
    current_->set(StyleType::XSCALE, StyleValue(xscale_->value()));
    current_->set(StyleType::YSCALE, StyleValue(yscale_->value()));
    current_->set(StyleType::DIRECTION, StyleValue(direction_->getDirection()));
}

void StyleEditor::setAdvancedShown(bool shown) { advancedSelect_->setChecked(shown); }
bool StyleEditor::advancedShown() const { return advancedSelect_->isChecked(); }

void StyleEditor::updatePreview() {
    if (!current_) return;
    // "Welcome to the (Jubler) world!": the parenthesised word in the
    // secondary colour, the rest in the primary colour over the shadow.
    QString demo = __("Welcome to the (Jubler) world!");
    const int a = demo.indexOf(QLatin1Char('(')), b = demo.indexOf(QLatin1Char(')'));
    QString before = demo, word, after;
    if (a >= 0 && b > a) { before = demo.left(a); word = demo.mid(a + 1, b - a - 1); after = demo.mid(b + 1); }
    QPalette p = preview_->palette();
    p.setColor(QPalette::Base, current_->color(StyleType::SHADOW).color());
    preview_->setPalette(p);
    preview_->clear();
    QTextCharFormat f;
    f.setFontFamilies({WebSafeFonts::renderFamily(current_->fontName())});
    f.setProperty(QTextFormat::FontPixelSize, std::max(1, current_->fontSize()));   // pixels, as Swing drew them
    f.setFontWeight(current_->flag(StyleType::BOLD) ? QFont::Bold : QFont::Normal);
    f.setFontItalic(current_->flag(StyleType::ITALIC));
    f.setFontUnderline(current_->flag(StyleType::UNDERLINE));
    f.setFontStrikeOut(current_->flag(StyleType::STRIKETHROUGH));
    QTextCursor c = preview_->textCursor();
    QTextBlockFormat bf;
    bf.setAlignment(Qt::AlignCenter);
    c.setBlockFormat(bf);
    f.setForeground(current_->color(StyleType::PRIMARY).color());
    c.insertText(before, f);
    f.setForeground(current_->color(StyleType::SECONDARY).color());
    c.insertText(word, f);
    f.setForeground(current_->color(StyleType::PRIMARY).color());
    c.insertText(after, f);
    // Sized to the text, as the Java packed the dialog after every change.
    QTextDocument *doc = preview_->document();
    doc->setTextWidth(preview_->viewport()->width());
    preview_->setFixedHeight(int(std::ceil(doc->size().height())) + 2 * preview_->frameWidth());
    adjustSize();
}

void StyleEditor::commitName() {
    static const QRegularExpression bad(QStringLiteral("[^0-9a-zA-Z\\-_.]"));
    QString newname = name_->text().trimmed();
    newname.remove(bad);
    if (newname.isEmpty() || !newname.at(0).isLetter()) {
        QApplication::beep();
        return;
    }
    current_->setName(newname, styles_);
    name_->setText(current_->getName());
    QPalette p = dirty_->palette();
    p.setColor(QPalette::Window, Qt::green);
    dirty_->setPalette(p);
}

void StyleEditor::cloneStyle() {
    readOtherValues();
    const bool isDefault = current_ == styles_.get(0);
    auto copy = std::make_shared<SubStyle>(*current_);
    copy->setDefault(false);
    copy->setNameRaw(isDefault ? QStringLiteral("Style1") : current_->getName());
    styles_.add(copy);
    copy->setName(copy->getName(), styles_);
    clone_ = copy;
    current_ = copy;
    setValues();
    cloneB_->setEnabled(false);
}

void StyleEditor::deleteStyle() {
    if (QMessageBox::question(this, __("Delete style"), __("Are you sure you want to delete this style?\nAll subtitles having this style will fall back to default")) != QMessageBox::Yes)
        return;
    deleted_ = true;
    cancelled_ = false;
    accept();
}

void StyleEditor::saveDefault() {
    readBasicValues();
    readOtherValues();
    Prefs::set(QStringLiteral("styles.default"), current_->getValues());
    SubStyleList::reloadDefaultStyle();
}
