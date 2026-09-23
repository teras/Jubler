/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/style/StyleType.h"

#include "core/util/JavaCompat.h"

namespace {
const char *const kDirNames[] = {"TOP", "TOPRIGHT", "RIGHT", "BOTTOMRIGHT", "BOTTOM",
                                 "BOTTOMLEFT", "LEFT", "TOPLEFT", "CENTER"};
const char *const kTypeNames[] = {"FONTNAME", "FONTSIZE", "BOLD", "ITALIC", "UNDERLINE",
                                  "STRIKETHROUGH", "PRIMARY", "SECONDARY", "OUTLINE", "SHADOW",
                                  "BORDERSTYLE", "BORDERSIZE", "SHADOWSIZE", "LEFTMARGIN",
                                  "RIGHTMARGIN", "VERTICAL", "ANGLE", "SPACING", "XSCALE",
                                  "YSCALE", "DIRECTION", "UNKNOWN"};

}  // namespace

QString directionName(Direction d) {
    return QString::fromLatin1(kDirNames[int(d)]);
}

Direction directionFromName(const QString &name, Direction fallback, bool *ok) {
    for (int i = 0; i < 9; ++i)
        if (name == QLatin1String(kDirNames[i])) {
            if (ok) *ok = true;
            return Direction(i);
        }
    if (ok) *ok = false;
    return fallback;
}

QString styleValueToString(const StyleValue &v) {
    struct Visitor {
        QString operator()(const QString &s) const { return s; }
        QString operator()(int i) const { return QString::number(i); }
        QString operator()(float f) const { return jc::floatToString(f); }
        QString operator()(bool b) const { return b ? QStringLiteral("true") : QStringLiteral("false"); }
        QString operator()(const AlphaColor &c) const { return c.toString(); }
        QString operator()(Direction d) const { return directionName(d); }
    };
    return std::visit(Visitor{}, v);
}

bool styleValueEquals(const StyleValue &a, const StyleValue &b) {
    return a == b;
}

StyleType::Format StyleType::type(Id id) {
    switch (id) {
        case FONTNAME: return FORMAT_STRING;
        case FONTSIZE: case BORDERSTYLE: case LEFTMARGIN: case RIGHTMARGIN: case VERTICAL:
        case XSCALE: case YSCALE: return FORMAT_INTEGRAL;
        case BOLD: case ITALIC: case UNDERLINE: case STRIKETHROUGH: return FORMAT_FLAG;
        case PRIMARY: case SECONDARY: case OUTLINE: case SHADOW: return FORMAT_COLOR;
        case BORDERSIZE: case SHADOWSIZE: case ANGLE: case SPACING: return FORMAT_REAL;
        case DIRECTION: return FORMAT_DIRECTION;
        default: return FORMAT_UNDEFINED;
    }
}

QString StyleType::name(Id id) {
    return QString::fromLatin1(kTypeNames[int(id)]);
}

StyleValue StyleType::defaultValue(Id id) {
    switch (id) {
        case FONTNAME: return QStringLiteral("Arial");
        case FONTSIZE: return 24;  // format-agnostic "core" size; ASS/SSA scale it to PlayResY
        case BOLD: case ITALIC: case UNDERLINE: case STRIKETHROUGH: return false;
        case PRIMARY: return AlphaColor(QColor(Qt::white), 255);
        case SECONDARY: return AlphaColor(QColor(255, 255, 0), 255);    // java.awt.Color.YELLOW
        case OUTLINE: return AlphaColor(QColor(Qt::black), 180);
        case SHADOW: return AlphaColor(QColor(64, 64, 64), 180);         // java.awt.Color.DARK_GRAY
        case BORDERSTYLE: return 0;
        case BORDERSIZE: return 0.0f;
        case SHADOWSIZE: return 2.0f;
        case LEFTMARGIN: case RIGHTMARGIN: case VERTICAL: return 20;
        case ANGLE: case SPACING: return 0.0f;
        case XSCALE: case YSCALE: return 100;
        case DIRECTION: return Direction::BOTTOM;
        default: return QString();
    }
}

StyleValue StyleType::init(Id id, const StyleValue &val) {
    switch (type(id)) {
        case FORMAT_INTEGRAL:
            if (auto *f = std::get_if<float>(&val)) return int(*f);
            if (std::holds_alternative<int>(val)) return val;
            return 0;
        case FORMAT_REAL:
            if (auto *i = std::get_if<int>(&val)) return float(*i);
            if (std::holds_alternative<float>(val)) return val;
            return 0.0f;
        case FORMAT_FLAG:
            if (std::holds_alternative<bool>(val)) return val;
            if (auto *i = std::get_if<int>(&val)) return *i != 0;
            break;
        case FORMAT_COLOR:
            if (std::holds_alternative<AlphaColor>(val)) return val;
            break;
        case FORMAT_DIRECTION:
            if (std::holds_alternative<Direction>(val)) return val;
            break;
        case FORMAT_STRING:
            if (std::holds_alternative<QString>(val)) return val;
            return styleValueToString(val);
        default:
            return val;
    }
    // Any other representation: through its text form, else the default.
    if (auto *str = std::get_if<QString>(&val)) return init(id, *str);
    return defaultValue(id);
}

StyleValue StyleType::init(Id id, const QString &val) {
    bool ok = false;
    switch (type(id)) {
        case FORMAT_UNDEFINED:
            return defaultValue(id);
        case FORMAT_INTEGRAL: {
            const int v = val.toInt(&ok);
            return ok ? StyleValue(v) : defaultValue(id);
        }
        case FORMAT_REAL: {
            const float v = val.toFloat(&ok);
            return ok ? StyleValue(v) : defaultValue(id);
        }
        case FORMAT_FLAG: {
            const int v = val.toInt(&ok);
            if (ok) return v != 0;
            // Java Boolean.valueOf: "true" (any case) → true, everything else false.
            return val.compare(QLatin1String("true"), Qt::CaseInsensitive) == 0;
        }
        case FORMAT_COLOR:
            return AlphaColor(val);
        case FORMAT_DIRECTION: {
            const Direction d = directionFromName(val, Direction::BOTTOM, &ok);
            return ok ? StyleValue(d) : defaultValue(id);
        }
        default:
            return val;
    }
}

QString StyleType::get(Id id, const StyleValue &v) {
    QString res = styleValueToString(v);
    if (type(id) == FORMAT_REAL && res.endsWith(QLatin1String(".0")))
        res.chop(2);
    return res;
}
