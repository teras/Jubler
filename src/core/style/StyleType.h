/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <variant>

#include "core/style/AlphaColor.h"

// Text alignment/placement of a style, the Java `SubStyle.Direction` enum.
enum class Direction { TOP, TOPRIGHT, RIGHT, BOTTOMRIGHT, BOTTOM, BOTTOMLEFT, LEFT, TOPLEFT, CENTER };

QString directionName(Direction d);
// Java `Direction.valueOf`: throws on unknown; here returns `fallback`.
Direction directionFromName(const QString &name, Direction fallback = Direction::BOTTOM, bool *ok = nullptr);

// A style attribute value. Which alternative is active is fixed per StyleType
// (see StyleType::type()).
using StyleValue = std::variant<QString, int, float, bool, AlphaColor, Direction>;

// Java-compatible text of a value (Integer/Float/Boolean/AlphaColor/Direction
// toString()).
QString styleValueToString(const StyleValue &v);
bool styleValueEquals(const StyleValue &a, const StyleValue &b);

// The attributes a style (and an inline style override) can carry, in the
// Java enum order — the order is significant: it is the serialisation order
// of `SubStyle::getValues()` and the index of the styleover array.
class StyleType {
public:
    enum Format : unsigned char {
        FORMAT_UNDEFINED = 0, FORMAT_STRING = 1, FORMAT_INTEGRAL = 2, FORMAT_REAL = 3,
        FORMAT_FLAG = 4, FORMAT_COLOR = 5, FORMAT_DIRECTION = 6
    };
    enum Id : int {
        FONTNAME, FONTSIZE, BOLD, ITALIC, UNDERLINE, STRIKETHROUGH,
        PRIMARY, SECONDARY, OUTLINE, SHADOW,
        BORDERSTYLE, BORDERSIZE, SHADOWSIZE, LEFTMARGIN, RIGHTMARGIN, VERTICAL,
        ANGLE, SPACING, XSCALE, YSCALE, DIRECTION, UNKNOWN,
        COUNT
    };

    static Format type(Id id);
    static QString name(Id id);
    static StyleValue defaultValue(Id id);

    // Coerce a value to the type's representation (Java `init(Object)`):
    // integral/real values are converted, others pass through. Never fails.
    static StyleValue init(Id id, const StyleValue &val);
    // Parse a textual value (Java `init(String)`); on a parse failure the
    // default is returned. Flags accept "0"/"1"/any int, "true"/"false".
    static StyleValue init(Id id, const QString &val);

    // Textual value as written by formats (Java `StyleType.get(SubStyle)`):
    // reals lose a trailing ".0".
    static QString get(Id id, const StyleValue &v);
};
