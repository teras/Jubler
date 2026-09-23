/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QComboBox>
#include <QDialog>
#include <QIcon>
#include <QMenu>
#include <QToolButton>
#include <QWidget>
#include <functional>
#include <optional>

#include "core/style/AlphaColor.h"
#include "core/style/StyleType.h"

class QSlider;
class QColorDialog;

// Icon of a colour with its alpha: left half opaque, right half over a
// grey checkerboard. Port of `JAlphaIcon`.
// The style controls' icons (colour swatches, B/I/U/S toggles, directions) are
// 28 px, as the Java (JAlphaIcon and the 28×28 SVGs).
constexpr int STYLE_ICON_SIZE = 28;
QIcon alphaColorIcon(const std::optional<AlphaColor> &c, int size);

// Colour + alpha chooser: alpha slider over a preview strip, colour dialog,
// OK/Cancel/Reset. Port of `JAlphaColorDialog`.
class AlphaColorDialog : public QDialog {
    Q_OBJECT
public:
    explicit AlphaColorDialog(QWidget *parent = nullptr);
    void setAlphaColor(const AlphaColor &c);
    // Null when cancelled.
    std::optional<AlphaColor> getAlphaColor() const { return result_; }
    // Convenience: returns the chosen colour or nullopt.
    static std::optional<AlphaColor> pick(QWidget *parent, const AlphaColor &initial);

private:
    class AlphaPanel;
    QColorDialog *chooser_;
    QSlider *slider_;
    AlphaPanel *preview_;
    AlphaColor orig_;
    std::optional<AlphaColor> result_;
};

// A 3×3 grid of placement buttons. Port of `JDirection`.
class DirectionGrid : public QWidget {
    Q_OBJECT
public:
    explicit DirectionGrid(QWidget *parent = nullptr);
    Direction getDirection() const;
    void setDirection(Direction d);
    static QIcon iconFor(Direction d);
    static QString tooltipFor(Direction d);
    int controlX() const;
    int controlY() const;

signals:
    void directionUpdated();

private:
    QToolButton *buttons_[9];
};

// The popup form of the grid, placed so the current cell is under the mouse.
// Port of `JDirectionDialog`.
class DirectionPopup : public QDialog {
    Q_OBJECT
public:
    explicit DirectionPopup(QWidget *parent = nullptr);
    void showAtMouse();
    DirectionGrid *grid() const { return grid_; }

signals:
    void directionUpdated();

private:
    DirectionGrid *grid_;
};

// Listener of inline-style edits from the override toolbar and the editor:
// (type, value) — Java `StyleChangeListener.changeStyle`.
using StyleChangeFn = std::function<void(StyleType::Id, const StyleValue &)>;

// Tri-state toggle: off / on / unspecified (mixed selection). Port of
// `TriToggleButton`.
class TriToggleButton : public QToolButton {
    Q_OBJECT
public:
    TriToggleButton(const QString &iconName, StyleType::Id type, QWidget *parent = nullptr);
    void setData(const std::optional<StyleValue> &v);
    void setListener(StyleChangeFn fn) { listener_ = std::move(fn); }

private:
    void apply();
    QString iconName_;
    StyleType::Id type_;
    int state_ = 0;
    StyleChangeFn listener_;
};

// Combo with a trailing "Unspecified" item. Port of `TriComboBox`.
class TriComboBox : public QComboBox {
    Q_OBJECT
public:
    TriComboBox(const QStringList &items, StyleType::Id type, QWidget *parent = nullptr);
    void setData(const std::optional<StyleValue> &v);
    void setListener(StyleChangeFn fn) { listener_ = std::move(fn); }

private:
    StyleType::Id type_;
    StyleChangeFn listener_;
    bool suppress_ = false;
};

// Colour swatch button for one of the four colour slots. Port of
// `TriColorButton`.
class TriColorButton : public QToolButton {
    Q_OBJECT
public:
    TriColorButton(const AlphaColor &initial, StyleType::Id type, QWidget *parent = nullptr);
    void setData(const std::optional<StyleValue> &v);
    void setListener(StyleChangeFn fn) { listener_ = std::move(fn); }
    std::optional<AlphaColor> color() const { return color_; }
    static QString labelFor(StyleType::Id type);
    static QString tooltipFor(StyleType::Id type);

private:
    StyleType::Id type_;
    std::optional<AlphaColor> color_;
    StyleChangeFn listener_;
};

// Alignment button with a popup grid. Port of `TriDirectionButton`.
class TriDirectionButton : public QToolButton {
    Q_OBJECT
public:
    explicit TriDirectionButton(QWidget *parent = nullptr);
    void setData(const std::optional<StyleValue> &v);
    void setListener(StyleChangeFn fn) { listener_ = std::move(fn); }

private:
    Direction current_ = Direction::BOTTOM;
    StyleChangeFn listener_;
};
