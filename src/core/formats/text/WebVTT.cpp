/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/formats/text/WebVTT.h"

#include <algorithm>
#include <cmath>

#include "core/formats/text/SubStationAlpha.h"
#include "core/os/Debug.h"
#include "core/util/JavaCompat.h"

const QRegularExpression &WebVTT::getPattern() {
    // Hours are optional and may have more than two digits; a blank line
    // holding only spaces or tabs still ends the cue.
    static const QRegularExpression pat(
        QStringLiteral("(?s)((\\d+)") + sp + nl + QStringLiteral(")?((\\d{2,}):)?(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)") + sp
        + QStringLiteral("-->") + sp + QStringLiteral("((\\d{2,}):)?(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)") + sp
        + QStringLiteral("(.*?)?") + nl
        + QStringLiteral("(.*?)(?:") + nl + sp + nl + QStringLiteral("|") + nl
        + QStringLiteral("(?=(?:\\d+[ \\t]*\\r?\\n)?(?:\\d{2,}:)?\\d\\d:\\d\\d\\.\\d\\d\\d[ \\t]*-->)|\\z)"));
    return pat;
}

const QRegularExpression &WebVTT::getTestPattern() {
    // "WEBVTT" optionally followed by a space and a title, then header lines
    // (Kind:, Language:, … — never a cue timing) up to a blank line or the
    // end of the input.
    static const QRegularExpression pat(QStringLiteral("(?is)WEBVTT(?:[ \\t][^\\r\\n]*)?[ \\t]*\\R(?:(?![^\\r\\n]*-->)[^\\r\\n]*\\S[^\\r\\n]*\\R)*(?:[ \\t]*\\R|\\z)"));
    return pat;
}

SubEntryPtr WebVTT::getSubEntry(const QRegularExpressionMatch &m) {
    const Time start(m.captured(4).isNull() ? QStringLiteral("00") : m.captured(4), m.captured(5), m.captured(6), m.captured(7));
    const Time finish(m.captured(9).isNull() ? QStringLiteral("00") : m.captured(9), m.captured(10), m.captured(11), m.captured(12));
    return makeWebVTTSubEntry(start, finish, m.captured(14), m.captured(13));
}

QString WebVTT::normalizeSpaces(const QString &text) {
    static const QRegularExpression spaces(QStringLiteral("  +"));
    QStringList lines = text.split(QLatin1Char('\n'));
    for (QString &line : lines)
        line.replace(spaces, QStringLiteral(" "));
    return lines.join(QLatin1Char('\n'));
}

SubEntryPtr WebVTT::makeWebVTTSubEntry(const Time &start, const Time &finish, const QString &input, const QString &settings) {
    static const QRegularExpression br(QStringLiteral("<br\\s*/?>"), QRegularExpression::CaseInsensitiveOption);
    QString processed = input;
    processed.replace(br, QStringLiteral("\n"));
    // The Java writer merged tags of one position into "<ibu>" / "</i/b/u>"
    // (JAVA_BUGS #11): read them back as the separate tags they stand for.
    static const QRegularExpression mergedOpen(QStringLiteral("<([ibu]{2,3})>"));
    static const QRegularExpression mergedClose(QStringLiteral("</([ibu](?:/[ibu]){1,2})>"));
    for (QRegularExpressionMatch mm = mergedOpen.match(processed); mm.hasMatch(); mm = mergedOpen.match(processed)) {
        QString tags;
        for (const QChar c : mm.captured(1)) tags += QLatin1Char('<') + c + QLatin1Char('>');
        processed.replace(mm.capturedStart(0), mm.capturedLength(0), tags);
    }
    for (QRegularExpressionMatch mm = mergedClose.match(processed); mm.hasMatch(); mm = mergedClose.match(processed)) {
        QString tags;
        for (const QString &c : mm.captured(1).split(QLatin1Char('/'))) tags += QStringLiteral("</") + c + QLatin1Char('>');
        processed.replace(mm.capturedStart(0), mm.capturedLength(0), tags);
    }
    processed = normalizeSpaces(processed);
    auto entry = std::make_shared<SubEntry>(start, finish, processed);
    entry->setStyle(subtitleList_->getStyleList().get(0));
    // Tags (and character references) first, so the cue settings cover the
    // final text.
    parseSubText(*entry);
    if (!jc::trim(settings).isEmpty())
        parseCueSettings(*entry, settings);
    return entry;
}

QString WebVTT::decodeEntities(const QString &text) {
    if (!text.contains(QLatin1Char('&')))
        return text;
    static const QRegularExpression ref(QStringLiteral("&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[A-Za-z]+);"));
    static const QHash<QString, QString> named = {
        {QStringLiteral("amp"), QStringLiteral("&")}, {QStringLiteral("lt"), QStringLiteral("<")},
        {QStringLiteral("gt"), QStringLiteral(">")}, {QStringLiteral("nbsp"), QString(QChar(0x00A0))},
        {QStringLiteral("lrm"), QString(QChar(0x200E))}, {QStringLiteral("rlm"), QString(QChar(0x200F))},
        {QStringLiteral("quot"), QStringLiteral("\"")}, {QStringLiteral("apos"), QStringLiteral("'")}};
    QString out;
    int last = 0;
    auto it = ref.globalMatch(text);
    while (it.hasNext()) {
        const QRegularExpressionMatch m = it.next();
        const QString name = m.captured(1);
        QString rep;
        if (name.startsWith(QLatin1Char('#'))) {
            bool ok = false;
            const bool hex = name.size() > 1 && (name.at(1) == QLatin1Char('x') || name.at(1) == QLatin1Char('X'));
            const uint cp = hex ? name.mid(2).toUInt(&ok, 16) : name.mid(1).toUInt(&ok, 10);
            if (ok && cp > 0 && cp <= 0x10FFFF && !(cp >= 0xD800 && cp <= 0xDFFF)) {
                const char32_t c = cp;
                rep = QString::fromUcs4(&c, 1);
            }
        } else
            rep = named.value(name);
        if (rep.isNull())
            continue;   // unknown reference: kept as written
        out += text.mid(last, m.capturedStart(0) - last) + rep;
        last = m.capturedEnd(0);
    }
    return out + text.mid(last);
}

namespace {
const QHash<QString, QColor> &classColors() {
    // The WebVTT default colour classes ("lime" is the standard name of pure
    // green; "green" is kept as the Java read it).
    static const QHash<QString, QColor> colors = {
        {QStringLiteral("white"), QColor(255, 255, 255)}, {QStringLiteral("lime"), QColor(0, 255, 0)},
        {QStringLiteral("green"), QColor(0, 255, 0)}, {QStringLiteral("cyan"), QColor(0, 255, 255)},
        {QStringLiteral("red"), QColor(255, 0, 0)}, {QStringLiteral("yellow"), QColor(255, 255, 0)},
        {QStringLiteral("magenta"), QColor(255, 0, 255)}, {QStringLiteral("blue"), QColor(0, 0, 255)},
        {QStringLiteral("black"), QColor(0, 0, 0)}};
    return colors;
}
}  // namespace

void WebVTT::beginParseSubText(SubEntry &entry) {
    SimpleStyledTextSubFormat::beginParseSubText(entry);
    classStack_.clear();
}

bool WebVTT::handleTagEvent(const QString &bodyIn, int pos, SubEntry &entry, const SubStylePtr &style) {
    const QString body = bodyIn.trimmed();
    int nameEnd = 0;
    while (nameEnd < body.length() && !body.at(nameEnd).isSpace() && body.at(nameEnd) != QLatin1Char('.'))
        ++nameEnd;
    const QString name = body.left(nameEnd).toLower();
    if (name == QLatin1String("c")) {
        // <c.class1.class2 …>: the first colour class colours the span, in the
        // coordinates of the text without tags.
        std::optional<AlphaColor> color;
        QString classPart = body.mid(nameEnd);
        for (int i = 0; i < classPart.length(); ++i)
            if (classPart.at(i).isSpace()) { classPart.truncate(i); break; }
        for (const QString &c : classPart.split(QLatin1Char('.'), Qt::SkipEmptyParts))
            if (classColors().contains(c.toLower())) {
                color = AlphaColor(classColors().value(c.toLower()), 255);
                break;
            }
        classStack_.append(color);
        if (color)
            entry.addOverStyle(StyleType::PRIMARY, StyleValue(*color), pos);
        return true;
    }
    if (name == QLatin1String("/c")) {
        if (classStack_.isEmpty())
            return true;   // stray </c>
        const std::optional<AlphaColor> closed = classStack_.takeLast();
        if (closed) {
            // Back to the colour of the enclosing class, else the style's.
            AlphaColor restore = style->color(StyleType::PRIMARY);
            for (int i = int(classStack_.size()) - 1; i >= 0; --i)
                if (classStack_[i]) { restore = *classStack_[i]; break; }
            entry.addOverStyle(StyleType::PRIMARY, StyleValue(restore), pos);
        }
        return true;
    }
    // <v …>, <lang …>, <ruby>, <rt>, timestamps and anything else unknown to
    // SubRip are dropped there.
    return SimpleStyledTextSubFormat::handleTagEvent(bodyIn, pos, entry, style);
}

void WebVTT::parseCueSettings(SubEntry &entry, const QString &settings) {
    const int len = entry.getText().length();
    // Margins are kept twice: as overrides in percent (as TTML placements,
    // for the WebVTT/TTML writers) and in the entry's SSA margin fields, in
    // pixels of the canvas written without a video (683x384), for SSA/ASS
    // and the preview.
    auto setInt = [&](StyleType::Id id, int v) {
        entry.setOverStyle(id, StyleValue(v), 0, len);
        const auto px = [](int pct, int res) { return QStringLiteral("%1").arg(int(std::lround(pct * res / 100.0)), 4, 10, QLatin1Char('0')); };
        if (id == StyleType::LEFTMARGIN) entry.setMarginL(px(v, SubStationAlpha::REF_WIDTH));
        else if (id == StyleType::RIGHTMARGIN) entry.setMarginR(px(v, SubStationAlpha::REF_WIDTH));
        else if (id == StyleType::VERTICAL) entry.setMarginV(px(v, SubStationAlpha::FONT_REF));
    };
    auto percent = [](QString v, bool &ok) { return v.remove(QLatin1Char('%')).trimmed().toFloat(&ok); };
    // Vertical (line) and horizontal (align/position) placement combine into
    // one direction: -1 = unset, 0 = top/left, 1 = centre, 2 = bottom/right.
    int vert = -1, horiz = -1;
    bool anyDir = false;
    std::optional<Direction> forced;
    // The cue box: position and size (percent of the width) and what the
    // position refers to (0 line-left, 1 centre, 2 line-right).
    std::optional<float> position, size;
    int alignBox = 1, positionAlign = -1;
    for (const QString &setting : jc::trim(settings).split(QRegularExpression(QStringLiteral("\\s+")), Qt::SkipEmptyParts)) {
        const int colon = setting.indexOf(QLatin1Char(':'));
        if (colon < 0)
            continue;
        const QString key = setting.left(colon).trimmed().toLower();
        const QString full = setting.mid(colon + 1).trimmed();
        // "position:30%,line-left" / "line:90%,end": the value and its alignment.
        const QString value = full.section(QLatin1Char(','), 0, 0);
        const QString sub = full.section(QLatin1Char(','), 1).trimmed().toLower();
        bool ok = false;
        if (key == QLatin1String("align")) {
            const QString v = value.toLower();
            if (v == QLatin1String("start") || v == QLatin1String("left")) { horiz = 0; alignBox = 0; anyDir = true; }
            else if (v == QLatin1String("middle") || v == QLatin1String("center")) { horiz = 1; alignBox = 1; anyDir = true; }
            else if (v == QLatin1String("end") || v == QLatin1String("right")) { horiz = 2; alignBox = 2; anyDir = true; }
        } else if (key == QLatin1String("position")) {
            const float p = percent(value, ok);
            if (!ok) { Debug::debug(QStringLiteral("Error parsing WebVTT position: ") + full); continue; }
            anyDir = true;
            position = p;
            horiz = p <= 25 ? 0 : (p >= 75 ? 2 : 1);
            if (sub == QLatin1String("line-left")) positionAlign = 0;
            else if (sub == QLatin1String("center")) positionAlign = 1;
            else if (sub == QLatin1String("line-right")) positionAlign = 2;
        } else if (key == QLatin1String("size")) {
            const float sz = percent(value, ok);
            if (!ok) { Debug::debug(QStringLiteral("Error parsing WebVTT size: ") + full); continue; }
            size = sz;
        } else if (key == QLatin1String("line")) {
            if (value.contains(QLatin1Char('%'))) {
                const float pct = percent(value, ok);
                if (!ok) { Debug::debug(QStringLiteral("Error parsing WebVTT line: ") + full); continue; }
                anyDir = true;
                vert = pct <= 25 ? 0 : (pct >= 75 ? 2 : 1);
                // The vertical margin (percent of the height) where the anchor
                // is plain: the top of a top cue, the bottom of an end-aligned
                // bottom cue.
                if (vert == 0)
                    setInt(StyleType::VERTICAL, std::max(0, int(std::lround(pct))));
                else if (vert == 2 && sub == QLatin1String("end"))
                    setInt(StyleType::VERTICAL, std::max(0, int(std::lround(100 - pct))));
            } else {
                // Line numbers count from the top (0, 1, …) or from the bottom
                // (-1, -2, …); a line is taken as 5 % of the height.
                const int lineNumber = value.toInt(&ok);
                if (!ok) { Debug::debug(QStringLiteral("Error parsing WebVTT line: ") + full); continue; }
                anyDir = true;
                vert = lineNumber >= 0 ? 0 : 2;
                setInt(StyleType::VERTICAL, (lineNumber >= 0 ? lineNumber : -lineNumber - 1) * 5);
            }
        } else if (key == QLatin1String("vertical")) {
            const QString v = value.toLower();
            if (v == QLatin1String("rl")) { entry.setOverStyle(StyleType::ANGLE, StyleValue(90.0f), 0, len); forced = Direction::RIGHT; }
            else if (v == QLatin1String("lr")) { entry.setOverStyle(StyleType::ANGLE, StyleValue(-90.0f), 0, len); forced = Direction::LEFT; }
            else Debug::debug(QStringLiteral("Unknown WebVTT vertical value: ") + value);
        }
    }
    if (position || size) {
        // The cue box as a WebVTT renderer computes it, kept as the left and
        // right margins in percent of the width.
        const int anchor = positionAlign >= 0 ? positionAlign : alignBox;
        const float pos = position ? std::clamp(*position, 0.0f, 100.0f) : (anchor == 0 ? 0.0f : anchor == 2 ? 100.0f : 50.0f);
        const float maxSize = anchor == 0 ? 100 - pos : anchor == 2 ? pos : 2 * std::min(pos, 100 - pos);
        const float boxSize = std::min(size ? std::clamp(*size, 0.0f, 100.0f) : 100.0f, maxSize);
        const float left = anchor == 0 ? pos : anchor == 2 ? pos - boxSize : pos - boxSize / 2;
        setInt(StyleType::LEFTMARGIN, std::max(0, int(std::lround(left))));
        setInt(StyleType::RIGHTMARGIN, std::max(0, int(std::lround(100 - left - boxSize))));
    }
    if (forced) {
        entry.setOverStyle(StyleType::DIRECTION, StyleValue(*forced), 0, len);
        return;
    }
    if (!anyDir)
        return;
    if (vert < 0) vert = 2;
    if (horiz < 0) horiz = 1;
    static const Direction table[3][3] = {
        {Direction::TOPLEFT, Direction::TOP, Direction::TOPRIGHT},
        {Direction::LEFT, Direction::CENTER, Direction::RIGHT},
        {Direction::BOTTOMLEFT, Direction::BOTTOM, Direction::BOTTOMRIGHT}};
    entry.setOverStyle(StyleType::DIRECTION, StyleValue(table[vert][horiz]), 0, len);
}

void WebVTT::initSaver(const Subtitles &subs, const MediaFile *media, QString &header) {
    SimpleStyledTextSubFormat::initSaver(subs, media, header);
    header += QLatin1String("WEBVTT\n\n");
}

namespace {
// Stand-ins (non-characters) for the characters written as references, of
// the same length so the override positions stay valid while the tags are
// inserted.
const QChar AMP(0xFDD0), LT(0xFDD1), GT(0xFDD2);
}  // namespace

QString WebVTT::rebuildSubText(const SubEntry &entry) {
    SubEntry copy(entry);
    QString text = entry.getText();
    text.replace(QLatin1Char('&'), AMP).replace(QLatin1Char('<'), LT).replace(QLatin1Char('>'), GT);
    copy.setText(text);
    QString out = fontTagsToClasses(SimpleStyledTextSubFormat::rebuildSubText(copy));
    out.replace(AMP, QLatin1String("&amp;")).replace(LT, QLatin1String("&lt;")).replace(GT, QLatin1String("&gt;"));
    return out;
}

QString WebVTT::fontTagsToClasses(const QString &text) {
    static const QRegularExpression font(QStringLiteral("<(/?)font\\b([^>]*)>"), QRegularExpression::CaseInsensitiveOption);
    static const QRegularExpression color(QStringLiteral("color\\s*=\\s*\"?#([0-9a-fA-F]{6})"), QRegularExpression::CaseInsensitiveOption);
    static const QHash<QString, QString> classes = {
        {QStringLiteral("ffffff"), QStringLiteral("white")}, {QStringLiteral("00ff00"), QStringLiteral("lime")},
        {QStringLiteral("00ffff"), QStringLiteral("cyan")}, {QStringLiteral("ff0000"), QStringLiteral("red")},
        {QStringLiteral("ffff00"), QStringLiteral("yellow")}, {QStringLiteral("ff00ff"), QStringLiteral("magenta")},
        {QStringLiteral("0000ff"), QStringLiteral("blue")}, {QStringLiteral("000000"), QStringLiteral("black")}};
    QStringList closers;
    QString out;
    int last = 0;
    auto it = font.globalMatch(text);
    while (it.hasNext()) {
        const QRegularExpressionMatch m = it.next();
        out += text.mid(last, m.capturedStart(0) - last);
        last = m.capturedEnd(0);
        if (m.captured(1).isEmpty()) {
            // A colour with a WebVTT class becomes that class; sizes, faces
            // and other colours have no WebVTT form.
            const QRegularExpressionMatch c = color.match(m.captured(2));
            const QString cls = c.hasMatch() ? classes.value(c.captured(1).toLower()) : QString();
            if (cls.isEmpty())
                closers.append(QString());
            else {
                out += QLatin1String("<c.") + cls + QLatin1Char('>');
                closers.append(QStringLiteral("</c>"));
            }
        } else if (!closers.isEmpty())
            out += closers.takeLast();
    }
    out += text.mid(last);
    static const QRegularExpression empty(QStringLiteral("<c\\.[a-z]+></c>"));
    out.remove(empty);
    return out;
}

void WebVTT::appendSubEntry(const SubEntry &sub, QString &str) {
    str += sub.getStartTime().getSeconds(QLatin1Char('.'));
    str += QLatin1String(" --> ");
    str += sub.getFinishTime().getSeconds(QLatin1Char('.'));
    auto intValue = [&sub](StyleType::Id id) -> std::optional<int> {
        const auto v = leadingOverride(sub, id);
        if (v && std::holds_alternative<int>(*v)) return std::get<int>(*v);
        return std::nullopt;
    };
    const auto angle = leadingOverride(sub, StyleType::ANGLE);
    if (angle && std::holds_alternative<float>(*angle) && std::abs(std::get<float>(*angle)) == 90.0f) {
        // Vertical text (as read from "vertical:"); its placement follows.
        str += std::get<float>(*angle) > 0 ? QLatin1String(" vertical:rl") : QLatin1String(" vertical:lr");
    } else {
        // Placement from the entry's direction (an override wins over the
        // style) and its margins.
        Direction direction = sub.getStyle() ? sub.getStyle()->direction() : Direction::BOTTOM;
        if (const auto d = leadingOverride(sub, StyleType::DIRECTION); d && std::holds_alternative<Direction>(*d))
            direction = std::get<Direction>(*d);
        const std::optional<int> vertical = intValue(StyleType::VERTICAL);
        switch (direction) {
            case Direction::TOP: case Direction::TOPLEFT: case Direction::TOPRIGHT:
                str += vertical ? QStringLiteral(" line:%1%").arg(std::clamp(*vertical, 0, 25)) : QStringLiteral(" line:10%");
                break;
            case Direction::CENTER: case Direction::LEFT: case Direction::RIGHT: str += QLatin1String(" line:50%"); break;
            default:
                if (vertical && *vertical > 0)
                    str += QStringLiteral(" line:%1%,end").arg(100 - std::clamp(*vertical, 0, 25));
                break;
        }
        int anchor = 1;
        switch (direction) {
            case Direction::TOPLEFT: case Direction::LEFT: case Direction::BOTTOMLEFT: str += QLatin1String(" align:start"); anchor = 0; break;
            case Direction::TOPRIGHT: case Direction::RIGHT: case Direction::BOTTOMRIGHT: str += QLatin1String(" align:end"); anchor = 2; break;
            default: break;
        }
        const std::optional<int> left = intValue(StyleType::LEFTMARGIN), right = intValue(StyleType::RIGHTMARGIN);
        if (left || right) {
            const int l = std::clamp(left.value_or(0), 0, 100), r = std::clamp(right.value_or(0), 0, 100 - l);
            const int boxSize = 100 - l - r;
            const double pos = anchor == 0 ? l : anchor == 2 ? 100 - r : l + boxSize / 2.0;
            str += QStringLiteral(" position:%1% size:%2%").arg(QString::number(pos), QString::number(boxSize));
        }
    }
    str += QLatin1Char('\n');
    str += rebuildSubText(sub);
    str += QLatin1String("\n\n");
}
