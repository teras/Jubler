/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/ui/StyleWidgets.h"

#include <QColorDialog>
#include <QCursor>
#include <QDialogButtonBox>
#include <QGridLayout>
#include <QGroupBox>
#include <QHBoxLayout>
#include <QLabel>
#include <QPainter>
#include <QPushButton>
#include <QScreen>
#include <QSlider>
#include <QVBoxLayout>

#include "app/Theme.h"
#include "core/i18n/I18N.h"
#include "core/options/Options.h"

// ---- alphaColorIcon -------------------------------------------------------------

QIcon alphaColorIcon(const std::optional<AlphaColor> &c, int size) {
    QPixmap pm(size, size);
    pm.fill(Qt::black);
    if (c) {
        QPainter p(&pm);
        const int w = size - 2, h = size - 2;
        p.fillRect(1, 1, w / 2, h, c->color());
        const QColor gray = c->mixed(QColor(128, 128, 128));
        const QColor dark = c->mixed(QColor(64, 64, 64));
        p.fillRect(1 + w / 2, 1, w - w / 2, h, gray);
        const int cw = std::max(1, (w - w / 2) / 3), ch = std::max(1, h / 3);
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 3; ++j)
                if ((i + j) % 2 == 1)
                    p.fillRect(1 + w / 2 + i * cw, 1 + j * ch, cw, ch, dark);
    }
    return QIcon(pm);
}

// ---- AlphaColorDialog -----------------------------------------------------------

class AlphaColorDialog::AlphaPanel : public QWidget {
public:
    explicit AlphaPanel(QWidget *parent) : QWidget(parent) { setFixedSize(512, 48); }
    void setAlphaColor(const AlphaColor &c) { color_ = c; update(); }
    void setColorOnly(const QColor &c) { color_ = AlphaColor(c, color_.alpha()); update(); }
    void setAlphaOnly(int a) { color_ = AlphaColor(color_.color(), a); update(); }

protected:
    void paintEvent(QPaintEvent *) override {
        QPainter p(this);
        for (int i = 0; i < 256; ++i) {
            const QColor c1 = color_.mixed(QColor(128, 128, 128), i);
            const QColor c2 = color_.mixed(QColor(64, 64, 64), i);
            const bool swap = (i / 4) % 2 == 1;
            for (int row = 0; row < 3; ++row) {
                p.fillRect(2 * i, row * 16, 2, 8, swap ? c2 : c1);
                p.fillRect(2 * i, row * 16 + 8, 2, 8, swap ? c1 : c2);
            }
        }
    }

private:
    AlphaColor color_{QColor(Qt::white), 200};
};

AlphaColorDialog::AlphaColorDialog(QWidget *parent) : QDialog(parent) {
    setWindowTitle(__("Color Chooser"));
    setModal(true);
    auto *layout = new QVBoxLayout(this);
    auto *alphaGroup = new QGroupBox(__("Alpha Channel"), this);
    auto *al = new QVBoxLayout(alphaGroup);
    slider_ = new QSlider(Qt::Horizontal, alphaGroup);
    slider_->setRange(0, 255);
    slider_->setValue(200);
    preview_ = new AlphaPanel(alphaGroup);
    al->addWidget(slider_);
    al->addWidget(preview_, 0, Qt::AlignHCenter);
    layout->addWidget(alphaGroup);
    chooser_ = new QColorDialog(this);
    chooser_->setWindowFlags(Qt::Widget);
    chooser_->setOptions(QColorDialog::NoButtons | QColorDialog::DontUseNativeDialog);
    layout->addWidget(chooser_);
    auto *buttons = new QDialogButtonBox(this);
    QPushButton *ok = buttons->addButton(__("OK"), QDialogButtonBox::AcceptRole);
    QPushButton *cancel = buttons->addButton(__("Cancel"), QDialogButtonBox::RejectRole);
    QPushButton *reset = buttons->addButton(__("Reset"), QDialogButtonBox::ResetRole);
    layout->addWidget(buttons);
    connect(chooser_, &QColorDialog::currentColorChanged, this, [this](const QColor &c) { preview_->setColorOnly(c); });
    connect(slider_, &QSlider::valueChanged, this, [this](int a) { preview_->setAlphaOnly(a); });
    connect(ok, &QPushButton::clicked, this, [this]() {
        result_ = AlphaColor(chooser_->currentColor(), slider_->value());
        accept();
    });
    connect(cancel, &QPushButton::clicked, this, [this]() { result_.reset(); reject(); });
    connect(reset, &QPushButton::clicked, this, [this]() { setAlphaColor(orig_); });
}

void AlphaColorDialog::setAlphaColor(const AlphaColor &c) {
    chooser_->setCurrentColor(c.color());
    slider_->setValue(c.alpha());
    preview_->setAlphaColor(c);
    orig_ = c;
    result_.reset();
}

std::optional<AlphaColor> AlphaColorDialog::pick(QWidget *parent, const AlphaColor &initial) {
    AlphaColorDialog dlg(parent);
    dlg.setAlphaColor(initial);
    dlg.exec();
    return dlg.getAlphaColor();
}

// ---- DirectionGrid --------------------------------------------------------------

namespace {
const Direction kGridOrder[9] = {Direction::TOPLEFT, Direction::TOP, Direction::TOPRIGHT, Direction::LEFT, Direction::CENTER,
                                 Direction::RIGHT, Direction::BOTTOMLEFT, Direction::BOTTOM, Direction::BOTTOMRIGHT};
int gridIndex(Direction d) {
    for (int i = 0; i < 9; ++i) if (kGridOrder[i] == d) return i;
    return 4;
}
}  // namespace

QIcon DirectionGrid::iconFor(Direction d) {
    switch (d) {
        case Direction::TOPLEFT: return Theme::icon(QStringLiteral("upleft"));
        case Direction::TOP: return Theme::icon(QStringLiteral("up"));
        case Direction::TOPRIGHT: return Theme::icon(QStringLiteral("upright"));
        case Direction::LEFT: return Theme::icon(QStringLiteral("left"));
        case Direction::CENTER: return Theme::icon(QStringLiteral("center"));
        case Direction::RIGHT: return Theme::icon(QStringLiteral("right"));
        case Direction::BOTTOMLEFT: return Theme::icon(QStringLiteral("downleft"));
        case Direction::BOTTOM: return Theme::icon(QStringLiteral("down"));
        case Direction::BOTTOMRIGHT: return Theme::icon(QStringLiteral("downright"));
    }
    return QIcon();
}

QString DirectionGrid::tooltipFor(Direction d) {
    switch (d) {
        case Direction::TOPLEFT: return __("Top left");
        case Direction::TOP: return __("Top");
        case Direction::TOPRIGHT: return __("Top right");
        case Direction::LEFT: return __("Left");
        case Direction::CENTER: return __("Center");
        case Direction::RIGHT: return __("Right");
        case Direction::BOTTOMLEFT: return __("Bottom left");
        case Direction::BOTTOM: return __("Bottom");
        case Direction::BOTTOMRIGHT: return __("Bottom right");
    }
    return QString();
}

DirectionGrid::DirectionGrid(QWidget *parent) : QWidget(parent) {
    auto *g = new QGridLayout(this);
    g->setSpacing(0);
    g->setContentsMargins(0, 0, 0, 0);
    for (int i = 0; i < 9; ++i) {
        auto *b = new QToolButton(this);
        b->setCheckable(true);
        b->setAutoExclusive(true);
        b->setIcon(iconFor(kGridOrder[i]));
        b->setToolTip(tooltipFor(kGridOrder[i]));
        b->setIconSize(QSize(STYLE_ICON_SIZE, STYLE_ICON_SIZE));
        g->addWidget(b, i / 3, i % 3);
        buttons_[i] = b;
        connect(b, &QToolButton::clicked, this, [this]() { emit directionUpdated(); });
    }
    buttons_[4]->setChecked(true);
}

Direction DirectionGrid::getDirection() const {
    for (int i = 0; i < 9; ++i) if (buttons_[i]->isChecked()) return kGridOrder[i];
    return Direction::CENTER;
}

void DirectionGrid::setDirection(Direction d) {
    buttons_[gridIndex(d)]->setChecked(true);
}

int DirectionGrid::controlX() const { return gridIndex(getDirection()) % 3; }
int DirectionGrid::controlY() const { return gridIndex(getDirection()) / 3; }

DirectionPopup::DirectionPopup(QWidget *parent) : QDialog(parent, Qt::Popup) {
    auto *l = new QVBoxLayout(this);
    l->setContentsMargins(2, 2, 2, 2);
    grid_ = new DirectionGrid(this);
    l->addWidget(grid_);
    connect(grid_, &DirectionGrid::directionUpdated, this, [this]() { emit directionUpdated(); hide(); });
}

void DirectionPopup::showAtMouse() {
    adjustSize();
    const QPoint m = QCursor::pos();
    const int w = width(), h = height();
    move(m - QPoint(w / 6 + grid_->controlX() * w / 3, h / 6 + grid_->controlY() * h / 3));
    show();
    grid_->setFocus();
}

// ---- TriToggleButton ------------------------------------------------------------

TriToggleButton::TriToggleButton(const QString &iconName, StyleType::Id type, QWidget *parent)
    : QToolButton(parent), iconName_(iconName), type_(type) {
    setCheckable(true);
    setIconSize(QSize(STYLE_ICON_SIZE, STYLE_ICON_SIZE));
    setAutoRaise(true);
    apply();
    connect(this, &QToolButton::clicked, this, [this]() {
        state_ = (state_ + 1) % 2;  // 0→1, 1→0, unspecified→1
        apply();
        if (listener_) listener_(type_, StyleValue(state_ == 1));
    });
}

void TriToggleButton::setData(const std::optional<StyleValue> &v) {
    if (!v || !std::holds_alternative<bool>(*v)) state_ = 2;
    else state_ = std::get<bool>(*v) ? 1 : 0;
    apply();
}

void TriToggleButton::apply() {
    setChecked(state_ != 0);
    setIcon(Theme::icon(iconName_, state_ == 2 ? Theme::IconStatus::DARK : Theme::IconStatus::NORMAL));
}

// ---- TriComboBox ----------------------------------------------------------------

TriComboBox::TriComboBox(const QStringList &items, StyleType::Id type, QWidget *parent) : QComboBox(parent), type_(type) {
    addItems(items);
    addItem(__("Unspecified"));
    connect(this, &QComboBox::activated, this, [this](int idx) {
        if (suppress_ || idx < 0 || idx == count() - 1) return;
        if (listener_) listener_(type_, StyleType::init(type_, itemText(idx)));
    });
}

void TriComboBox::setData(const std::optional<StyleValue> &v) {
    suppress_ = true;
    if (!v)
        setCurrentIndex(count() - 1);
    else {
        const QString text = styleValueToString(*v);
        const int idx = findText(text);
        if (idx >= 0) setCurrentIndex(idx);
        // Unknown value (uninstalled font, odd size): the previous selection stays.
    }
    suppress_ = false;
}

// ---- TriColorButton -------------------------------------------------------------

QString TriColorButton::labelFor(StyleType::Id type) {
    switch (type) {
        case StyleType::SECONDARY: return __("Secondary");
        case StyleType::OUTLINE: return __("Outline");
        case StyleType::SHADOW: return __("Shadow");
        default: return __("Primary");
    }
}

QString TriColorButton::tooltipFor(StyleType::Id type) {
    switch (type) {
        case StyleType::SECONDARY: return __("Set the secondary color of the style");
        case StyleType::OUTLINE: return __("Set the outline color of the style");
        case StyleType::SHADOW: return __("Set the shadow (or the background) color of the style");
        default: return __("Set the primary color of the style");
    }
}

TriColorButton::TriColorButton(const AlphaColor &initial, StyleType::Id type, QWidget *parent) : QToolButton(parent), type_(type), color_(initial) {
    setAutoRaise(true);
    setIconSize(QSize(STYLE_ICON_SIZE, STYLE_ICON_SIZE));
    setIcon(alphaColorIcon(color_, STYLE_ICON_SIZE));
    setToolTip(tooltipFor(type));
    connect(this, &QToolButton::clicked, this, [this]() {
        const AlphaColor start = color_ ? *color_ : AlphaColor(QColor(Qt::white), 255);
        const auto picked = AlphaColorDialog::pick(this, start);
        if (!picked)
            return;  // cancelled: nothing changes
        color_ = *picked;
        setIcon(alphaColorIcon(color_, STYLE_ICON_SIZE));
        if (listener_) listener_(type_, StyleValue(*color_));
    });
}

void TriColorButton::setData(const std::optional<StyleValue> &v) {
    if (v && std::holds_alternative<AlphaColor>(*v)) color_ = std::get<AlphaColor>(*v);
    else color_.reset();
    setIcon(alphaColorIcon(color_, STYLE_ICON_SIZE));
}

// ---- TriDirectionButton ---------------------------------------------------------

TriDirectionButton::TriDirectionButton(QWidget *parent) : QToolButton(parent) {
    setAutoRaise(true);
    setIconSize(QSize(STYLE_ICON_SIZE, STYLE_ICON_SIZE));
    setIcon(DirectionGrid::iconFor(current_));
    setToolTip(__("Alignment"));
    connect(this, &QToolButton::clicked, this, [this]() {
        auto *popup = new DirectionPopup(this);
        popup->setAttribute(Qt::WA_DeleteOnClose);
        popup->grid()->setDirection(current_);
        connect(popup, &DirectionPopup::directionUpdated, this, [this, popup]() {
            current_ = popup->grid()->getDirection();
            setIcon(DirectionGrid::iconFor(current_));
            if (listener_) listener_(StyleType::DIRECTION, StyleValue(current_));
        });
        popup->showAtMouse();
    });
}

void TriDirectionButton::setData(const std::optional<StyleValue> &v) {
    if (v && std::holds_alternative<Direction>(*v)) {
        current_ = std::get<Direction>(*v);
        setIcon(DirectionGrid::iconFor(current_));
    } else
        setIcon(Theme::icon(QStringLiteral("center"), Theme::IconStatus::DARK));
}
