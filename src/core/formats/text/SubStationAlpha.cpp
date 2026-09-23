/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/formats/text/SubStationAlpha.h"

#include <QRegularExpression>

#include "core/media/MediaFile.h"

using DM = StyledFormat::DirectionMap;

const DM &SubStationAlpha::ssaDirections() {
    static const DM m = {{QStringLiteral("6"), Direction::TOP}, {QStringLiteral("7"), Direction::TOPRIGHT},
                         {QStringLiteral("11"), Direction::RIGHT}, {QStringLiteral("3"), Direction::BOTTOMRIGHT},
                         {QStringLiteral("2"), Direction::BOTTOM}, {QStringLiteral("1"), Direction::BOTTOMLEFT},
                         {QStringLiteral("9"), Direction::LEFT}, {QStringLiteral("5"), Direction::TOPLEFT},
                         {QStringLiteral("10"), Direction::CENTER}};
    return m;
}

const DM &SubStationAlpha::assDirections() {
    static const DM m = {{QStringLiteral("8"), Direction::TOP}, {QStringLiteral("9"), Direction::TOPRIGHT},
                         {QStringLiteral("6"), Direction::RIGHT}, {QStringLiteral("3"), Direction::BOTTOMRIGHT},
                         {QStringLiteral("2"), Direction::BOTTOM}, {QStringLiteral("1"), Direction::BOTTOMLEFT},
                         {QStringLiteral("4"), Direction::LEFT}, {QStringLiteral("7"), Direction::TOPLEFT},
                         {QStringLiteral("5"), Direction::CENTER}};
    return m;
}

int SubStationAlpha::directionToAn(Direction d) {
    switch (d) {
        case Direction::BOTTOMLEFT: return 1;
        case Direction::BOTTOM: return 2;
        case Direction::BOTTOMRIGHT: return 3;
        case Direction::LEFT: return 4;
        case Direction::CENTER: return 5;
        case Direction::RIGHT: return 6;
        case Direction::TOPLEFT: return 7;
        case Direction::TOP: return 8;
        case Direction::TOPRIGHT: return 9;
    }
    return -1;
}

namespace {
// Literal braces of the text are written "\{" / "\}" so they never read as
// tags; private-use stand-ins keep the character positions while tags are
// inserted or parsed. A brace comment ({text without a backslash}) is kept as
// an unknown tag starting with COMMENT_MARK.
constexpr char16_t OPEN_STANDIN = 0xE000, CLOSE_STANDIN = 0xE001, COMMENT_MARK = 0xE002;
constexpr char16_t NBSP = 0x00A0;

// The format data keys of the document and of its styles.
const QString KEY_PLAYRESX = QStringLiteral("ssa.PlayResX");
const QString KEY_PLAYRESY = QStringLiteral("ssa.PlayResY");
const QString KEY_INFO = QStringLiteral("ssa.info");        // other [Script Info] lines
const QString KEY_HEAD = QStringLiteral("ssa.head");        // unknown sections before [Events]
const QString KEY_TAIL = QStringLiteral("ssa.tail");        // unknown sections after it
const QString KEY_EVENTS = QStringLiteral("ssa.events");    // Comment: and other non-Dialogue events

// The written [Events] columns (the first one is Marked or Layer).
const QStringList EVENT_COLUMNS = {QStringLiteral("layer"), QStringLiteral("start"), QStringLiteral("end"), QStringLiteral("style"),
                                   QStringLiteral("name"), QStringLiteral("marginl"), QStringLiteral("marginr"),
                                   QStringLiteral("marginv"), QStringLiteral("effect"), QStringLiteral("text")};

// "Primary Colour" → "primarycolour"
QString columnKey(const QString &name) {
    QString key = name.trimmed().toLower();
    key.remove(QLatin1Char(' '));
    return key;
}

QStringList formatColumns(const QString &list) {
    QStringList cols;
    for (const QString &c : list.split(QLatin1Char(',')))
        cols.append(columnKey(c));
    return cols;
}

// Index of a column; Marked/Layer and Name/Actor are the same column.
int columnIndex(const QStringList &format, const QString &key) {
    int idx = format.indexOf(key);
    if (idx < 0 && (key == QLatin1String("layer") || key == QLatin1String("marked")))
        idx = format.indexOf(key == QLatin1String("layer") ? QStringLiteral("marked") : QStringLiteral("layer"));
    if (idx < 0 && key == QLatin1String("name"))
        idx = format.indexOf(QStringLiteral("actor"));
    return idx;
}

// The fields of a line after its "Type:", as many as the columns: the last one
// takes the rest (the Text may contain commas).
QStringList splitFields(const QString &data, int count) {
    QStringList fields;
    int from = 0;
    while (fields.size() < count - 1) {
        const int comma = data.indexOf(QLatin1Char(','), from);
        if (comma < 0)
            break;
        fields.append(data.mid(from, comma - from));
        from = comma + 1;
    }
    fields.append(data.mid(from));
    return fields;
}

// A comma would start a new field: Name, Effect and style names get ';'.
QString fieldText(QString text) {
    return text.replace(QLatin1Char(','), QLatin1Char(';')).replace(QLatin1Char('\n'), QLatin1Char(' '));
}

bool parseTime(const QString &text, Time &time) {
    // Hours may have one or two digits; centiseconds are padded to ms by Time.
    static const QRegularExpression pat(QStringLiteral("^\\s*(\\d{1,2}):(\\d\\d):(\\d\\d)\\.(\\d\\d)\\s*$"));
    const QRegularExpressionMatch m = pat.match(text);
    if (!m.hasMatch())
        return false;
    time = Time(m.captured(1), m.captured(2), m.captured(3), m.captured(4));
    return true;
}
}  // namespace

const QRegularExpression &SubStationAlpha::getPattern() {
    // One Dialogue line; its fields are split with the [Events] Format: columns.
    static const QRegularExpression pat(QStringLiteral("(?im)^[ \\t\\x{FEFF}]*Dialogue:([^\\r\\n]*)"));
    return pat;
}

const QRegularExpression &SubStationAlpha::getTestPattern() {
    static const QRegularExpression pat(
        QStringLiteral("(?i)(?s)\\[Script Info\\].*?\\[V4 Styles\\].*?Dialogue:.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?") + nl);
    return pat;
}

const QRegularExpression &AdvancedSubStation::getTestPattern() {
    static const QRegularExpression pat(
        QStringLiteral("(?i)(?s)\\[Script Info\\].*?\\[v4(?:(\\+ Styles)|( Styles\\+))\\].*?Dialogue:.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?") + nl);
    return pat;
}

const QRegularExpression &SubStationAlpha::getStylePattern() {
    static const QRegularExpression pat(QStringLiteral("\\{(.*?)\\}"));
    return pat;
}

// Split an override block on '\' — but not inside parentheses, so function
// tags such as \t(0,1000,\fs60) stay whole.
QStringList SubStationAlpha::tokenize(const QString &body) {
    QStringList tokens;
    QString cur;
    int depth = 0;
    for (const QChar c : body) {
        if (c == QLatin1Char('(')) ++depth;
        else if (c == QLatin1Char(')') && depth > 0) --depth;
        if (c == QLatin1Char('\\') && depth == 0) {
            if (!cur.isEmpty()) tokens.append(cur);
            cur.clear();
        } else
            cur += c;
    }
    if (!cur.isEmpty()) tokens.append(cur);
    return tokens;
}

// A block without any '\' is a comment, not tags: kept verbatim.
bool SubStationAlpha::handleTagEvent(const QString &body, int pos, SubEntry &entry, const SubStylePtr &style) {
    Q_UNUSED(style);
    if (body.contains(QLatin1Char('\\')))
        return false;
    entry.addOverStyle(StyleType::UNKNOWN, StyleValue(QString(QChar(COMMENT_MARK)) + body), pos);
    return true;
}

bool SubStationAlpha::acceptsTagValue(const StyledFormat &sf, const QString &value) {
    switch (StyleType::type(sf.style)) {
        case StyleType::FORMAT_FLAG:
            // Exactly \b1 / \b0: \b100 or \b700 is a font weight, kept as it is.
            return value.trimmed().isEmpty();
        case StyleType::FORMAT_COLOR:
            // A colour is "&H…" or a decimal number: \clip is not \c.
            return value.isEmpty() || value.at(0) == QLatin1Char('&') || value.at(0).isDigit();
        default:
            return true;
    }
}

const QList<StyledFormat> &SubStationAlpha::getStylesDictionary() {
    static const QList<StyledFormat> dict = {
        StyledFormat(StyleType::ITALIC, QStringLiteral("i0"), false), StyledFormat(StyleType::ITALIC, QStringLiteral("i1"), true),
        StyledFormat(StyleType::BOLD, QStringLiteral("b0"), false), StyledFormat(StyleType::BOLD, QStringLiteral("b1"), true),
        StyledFormat(StyleType::FONTNAME, QStringLiteral("fn"), nullptr), StyledFormat(StyleType::FONTSIZE, QStringLiteral("fs"), nullptr),
        StyledFormat(StyleType::PRIMARY, QStringLiteral("c"), StyledFormat::COLOR_REVERSE),
        StyledFormat(StyleType::PRIMARY, QStringLiteral("alpha"), StyledFormat::COLOR_ALPHA_REVERSE),
        StyledFormat(StyleType::DIRECTION, QStringLiteral("an"), assDirections(), false),  // read only: must precede "a"
        StyledFormat(StyleType::DIRECTION, QStringLiteral("a"), ssaDirections()),
        StyledFormat(StyleType::UNKNOWN, QString(QLatin1String("")), nullptr)};  // must be LAST: matches every tag
    return dict;
}

const QList<StyledFormat> &AdvancedSubStation::getStylesDictionary() {
    static const QList<StyledFormat> dict = {
        StyledFormat(StyleType::ITALIC, QStringLiteral("i0"), false), StyledFormat(StyleType::ITALIC, QStringLiteral("i1"), true),
        StyledFormat(StyleType::BOLD, QStringLiteral("b0"), false), StyledFormat(StyleType::BOLD, QStringLiteral("b1"), true),
        StyledFormat(StyleType::UNDERLINE, QStringLiteral("u0"), false), StyledFormat(StyleType::UNDERLINE, QStringLiteral("u1"), true),
        StyledFormat(StyleType::STRIKETHROUGH, QStringLiteral("s0"), false), StyledFormat(StyleType::STRIKETHROUGH, QStringLiteral("s1"), true),
        StyledFormat(StyleType::UNKNOWN, QStringLiteral("fsc"), nullptr, false),   // guards: \fscx/\fscy/\fsp are not \fs
        StyledFormat(StyleType::UNKNOWN, QStringLiteral("fsp"), nullptr, false),
        StyledFormat(StyleType::FONTNAME, QStringLiteral("fn"), nullptr), StyledFormat(StyleType::FONTSIZE, QStringLiteral("fs"), nullptr),
        StyledFormat(StyleType::PRIMARY, QStringLiteral("1c"), StyledFormat::COLOR_REVERSE),
        StyledFormat(StyleType::PRIMARY, QStringLiteral("c"), StyledFormat::COLOR_REVERSE, false),
        StyledFormat(StyleType::PRIMARY, QStringLiteral("1a"), StyledFormat::COLOR_ALPHA_REVERSE),
        StyledFormat(StyleType::PRIMARY, QStringLiteral("alpha"), StyledFormat::COLOR_ALPHA_REVERSE, false),
        StyledFormat(StyleType::SECONDARY, QStringLiteral("2c"), StyledFormat::COLOR_REVERSE),
        StyledFormat(StyleType::SECONDARY, QStringLiteral("2a"), StyledFormat::COLOR_ALPHA_REVERSE),
        StyledFormat(StyleType::OUTLINE, QStringLiteral("3c"), StyledFormat::COLOR_REVERSE),
        StyledFormat(StyleType::OUTLINE, QStringLiteral("3a"), StyledFormat::COLOR_ALPHA_REVERSE),
        StyledFormat(StyleType::SHADOW, QStringLiteral("4c"), StyledFormat::COLOR_REVERSE),
        StyledFormat(StyleType::SHADOW, QStringLiteral("4a"), StyledFormat::COLOR_ALPHA_REVERSE),
        StyledFormat(StyleType::DIRECTION, QStringLiteral("an"), assDirections()),
        StyledFormat(StyleType::DIRECTION, QStringLiteral("a"), ssaDirections(), false),
        StyledFormat(StyleType::UNKNOWN, QString(QLatin1String("")), nullptr)};
    return dict;
}

const QMap<QString, QString> &SubStationAlpha::getStylePairs() {
    static const QMap<QString, QString> none;
    return none;
}

QString SubStationAlpha::styleFormat() const {
    return QStringLiteral("Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, TertiaryColour, BackColour, Bold, Italic, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, AlphaLevel, Encoding");
}

QString AdvancedSubStation::styleFormat() const {
    return QStringLiteral("Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding");
}

QString SubStationAlpha::initLoader(const QString &inputIn) {
    const QString input = GenericStyledTextSubFormat::initLoader(inputIn);
    enum { NONE, INFO, STYLES, EVENTS, OTHER } section = NONE;
    bool afterEvents = false;
    QString info, unknown;
    QStringList keptInfo, head, tail, styleLines;
    QMap<QString, QString> fixedInfo;
    QList<QPair<QString, QString>> keptEvents;   // (type, fields in the file's order)
    QStringList styleCols = formatColumns(styleFormat());
    eventFormat_ = EVENT_COLUMNS;
    hardSoftBreaks_ = false;
    int playResX = 0, playResY = 0;
    auto flushUnknown = [&]() {
        while (unknown.endsWith(QLatin1Char('\n')))
            unknown.chop(1);
        if (!unknown.isEmpty())
            (afterEvents ? tail : head).append(unknown);
        unknown.clear();
    };
    for (QString line : input.split(QLatin1Char('\n'))) {
        if (line.endsWith(QLatin1Char('\r')))
            line.chop(1);
        QString t = line.trimmed();
        if (t.startsWith(QChar(0xFEFF)))
            t = t.mid(1).trimmed();
        if (t.startsWith(QLatin1Char('[')) && t.endsWith(QLatin1Char(']'))) {
            flushUnknown();
            const QString name = t.mid(1, t.length() - 2).trimmed().toLower();
            if (name == QLatin1String("script info"))
                section = INFO;
            else if (name == QLatin1String("v4 styles") || name == QLatin1String("v4+ styles") || name == QLatin1String("v4 styles+"))
                section = STYLES;
            else if (name == QLatin1String("events")) {
                section = EVENTS;
                afterEvents = true;
            } else {
                section = OTHER;   // [Fonts], [Graphics], editor data…: kept verbatim
                unknown = t + QLatin1Char('\n');
            }
            continue;
        }
        const int colon = t.indexOf(QLatin1Char(':'));
        const QString key = colon > 0 ? t.left(colon).trimmed().toLower() : QString();
        const QString value = colon > 0 ? t.mid(colon + 1) : QString();
        if (key == QLatin1String("dialogue"))
            continue;   // read by getPattern(), in any section
        switch (section) {
            case INFO:
                info += line + QLatin1Char('\n');
                if (t.isEmpty() || t.startsWith(QLatin1Char(';')) || colon <= 0)
                    break;
                if (key == QLatin1String("playresx"))
                    playResX = value.trimmed().toInt();
                else if (key == QLatin1String("playresy"))
                    playResY = value.trimmed().toInt();
                else if (key == QLatin1String("collisions") || key == QLatin1String("playdepth") || key == QLatin1String("timer"))
                    fixedInfo.insert(key, value.trimmed());   // written in their usual place
                else if (key != QLatin1String("title") && key != QLatin1String("original script")
                         && key != QLatin1String("update details") && key != QLatin1String("scripttype")) {
                    if (key == QLatin1String("wrapstyle"))
                        hardSoftBreaks_ = value.trimmed() == QLatin1String("2");
                    keptInfo.append(t);
                }
                break;
            case STYLES:
                if (key == QLatin1String("format"))
                    styleCols = formatColumns(value);
                else if (key == QLatin1String("style"))
                    styleLines.append(value);
                break;
            case EVENTS:
                if (key == QLatin1String("format"))
                    eventFormat_ = formatColumns(value);
                else if (colon > 0)
                    keptEvents.append({t.left(colon).trimmed(), value});
                break;
            case OTHER:
                unknown += line + QLatin1Char('\n');
                break;
            default:
                break;
        }
    }
    flushUnknown();

    // The canvas, with the ASS rule for a missing side (4:3; 1280 ↔ 1024).
    if (playResX <= 0 && playResY <= 0) {
        playResY = FONT_DEFAULT_RES;
        playResX = FONT_DEFAULT_RES * 4 / 3;
    } else if (playResY <= 0)
        playResY = playResX == 1280 ? 1024 : playResX * 3 / 4;
    else if (playResX <= 0)
        playResX = playResY == 1024 ? 1280 : playResY * 4 / 3;
    playResY_ = playResY;
    fontFactor_ = playResY / float(FONT_REF);

    SubStyleList &list = subtitleList_->getStyleList();
    const SubStylePtr deflt = list.clearList();
    for (const QString &data : styleLines)
        readStyle(styleCols, data, list);
    if (list.size() == 0)
        list.add(deflt);
    list.get(0)->setDefault(true);

    // Kept events are written with the standard columns.
    QStringList events;
    for (const auto &ev : keptEvents) {
        const QStringList fields = splitFields(ev.second, eventFormat_.size());
        QStringList out;
        for (const QString &col : EVENT_COLUMNS) {
            const int idx = columnIndex(eventFormat_, col);
            const QString f = idx >= 0 && idx < fields.size() ? fields.at(idx) : QString(QLatin1String(""));
            out.append(col == QLatin1String("text") ? f : f.trimmed());
        }
        events.append(ev.first + QLatin1String(": ") + out.join(QLatin1Char(',')));
    }
    QMap<QString, QString> data;
    data.insert(KEY_PLAYRESX, QString::number(playResX));
    data.insert(KEY_PLAYRESY, QString::number(playResY));
    for (auto it = fixedInfo.begin(); it != fixedInfo.end(); ++it)
        data.insert(QStringLiteral("ssa.") + it.key(), it.value());
    if (!keptInfo.isEmpty()) data.insert(KEY_INFO, keptInfo.join(QLatin1Char('\n')));
    if (!head.isEmpty()) data.insert(KEY_HEAD, head.join(QLatin1String("\n\n")));
    if (!tail.isEmpty()) data.insert(KEY_TAIL, tail.join(QLatin1String("\n\n")));
    if (!events.isEmpty()) data.insert(KEY_EVENTS, events.join(QLatin1Char('\n')));
    subtitleList_->setFormatData(data);

    static const QRegularExpression title(QStringLiteral("(?i)Title:") + sp + QStringLiteral("(.*?)") + nl);
    static const QRegularExpression author(QStringLiteral("(?i)Original Script:") + sp + QStringLiteral("(.*?)") + nl);
    static const QRegularExpression source(QStringLiteral("(?i)Update Details:") + sp + QStringLiteral("(.*?)") + nl);
    static const QRegularExpression comments(QStringLiteral(";(.*?)") + nl);
    updateAttributes(info, title, author, source, comments);
    return input;
}

void SubStationAlpha::readStyle(const QStringList &format, const QString &data, SubStyleList &list) {
    const QStringList fields = splitFields(data, format.size());
    auto has = [&](const char *key) { const int i = format.indexOf(QLatin1String(key)); return i >= 0 && i < fields.size(); };
    auto field = [&](const char *key) { const int i = format.indexOf(QLatin1String(key)); return i >= 0 && i < fields.size() ? fields.at(i).trimmed() : QString(); };
    auto st = std::make_shared<SubStyle>(field("name"));
    // Integral columns accept decimals (22.5, 100.5), rounded; the file text
    // is kept to be written back while the model value is unchanged.
    auto setRounded = [&](StyleType::Id id, const char *key, float factor, const QString &keep) {
        bool ok = false;
        const double v = field(key).toDouble(&ok);
        if (!ok)
            return;
        const int value = qRound(v / factor);
        st->set(id, StyleValue(value));
        if (!keep.isNull()) {
            st->setFormatData(QStringLiteral("ssa.") + keep, field(key));
            st->setFormatData(QStringLiteral("ssa.") + keep + QStringLiteral(".model"),
                              id == StyleType::FONTSIZE ? fontSizeModel(value) : QString::number(value));
        }
    };
    if (has("fontname")) st->set(StyleType::FONTNAME, field("fontname"));
    setRounded(StyleType::FONTSIZE, "fontsize", getFontFactor(), QStringLiteral("Fontsize"));
    // SSA keeps the alpha of the first three colours in AlphaLevel.
    const QString alpha = has("alphalevel") ? field("alphalevel") : QString();
    if (has("primarycolour")) st->set(StyleType::PRIMARY, StyleValue(stringToAlphaColor(field("primarycolour"), alpha)));
    if (has("secondarycolour")) st->set(StyleType::SECONDARY, StyleValue(stringToAlphaColor(field("secondarycolour"), alpha)));
    if (has("outlinecolour")) st->set(StyleType::OUTLINE, StyleValue(stringToAlphaColor(field("outlinecolour"), alpha)));
    else if (has("tertiarycolour")) st->set(StyleType::OUTLINE, StyleValue(stringToAlphaColor(field("tertiarycolour"), alpha)));
    if (has("backcolour")) st->set(StyleType::SHADOW, StyleValue(stringToAlphaColor(field("backcolour"), QString())));
    if (has("bold")) st->set(StyleType::BOLD, field("bold"));
    if (has("italic")) st->set(StyleType::ITALIC, field("italic"));
    if (has("underline")) st->set(StyleType::UNDERLINE, field("underline"));
    if (has("strikeout")) st->set(StyleType::STRIKETHROUGH, field("strikeout"));
    setRounded(StyleType::XSCALE, "scalex", 1, QStringLiteral("ScaleX"));
    setRounded(StyleType::YSCALE, "scaley", 1, QStringLiteral("ScaleY"));
    if (has("spacing")) st->set(StyleType::SPACING, field("spacing"));
    if (has("angle")) st->set(StyleType::ANGLE, field("angle"));
    if (has("borderstyle")) st->set(StyleType::BORDERSTYLE, StyleValue(field("borderstyle") == QLatin1String("3") ? 1 : 0));
    if (has("outline")) st->set(StyleType::BORDERSIZE, field("outline"));
    if (has("shadow")) st->set(StyleType::SHADOWSIZE, field("shadow"));
    if (styleDirections().contains(field("alignment")))
        st->set(StyleType::DIRECTION, StyleValue(styleDirections().value(field("alignment"))));
    setRounded(StyleType::LEFTMARGIN, "marginl", 1, QString());
    setRounded(StyleType::RIGHTMARGIN, "marginr", 1, QString());
    setRounded(StyleType::VERTICAL, "marginv", 1, QString());
    if (has("encoding")) st->setFormatData(QStringLiteral("ssa.Encoding"), field("encoding"));
    list.add(st);
}

QString SubStationAlpha::keptStyleText(const SubStyle &style, const QString &column, const QString &model, const QString &computed) {
    const QString text = style.formatData(QStringLiteral("ssa.") + column);
    return !text.isEmpty() && style.formatData(QStringLiteral("ssa.") + column + QStringLiteral(".model")) == model ? text : computed;
}

QString SubStationAlpha::fontSizeFor(const SubStyle &style, int core) {
    return keptStyleText(style, QStringLiteral("Fontsize"), fontSizeModel(core), QString::number(qRound(core * getFontFactor())));
}

QString SubStationAlpha::fontSizeText(const SubEntry &entry, int core) {
    // The file's own text ("40.5") while the size and the canvas are unchanged.
    const QString kept = entry.keptFontSizeText(core, getFontFactor());
    if (!kept.isEmpty()) return kept;
    return entry.getStyle() ? fontSizeFor(*entry.getStyle(), core) : GenericStyledTextSubFormat::fontSizeText(entry, core);
}

SubStylePtr SubStationAlpha::styleForName(const QString &nameIn) const {
    QString name = nameIn.trimmed();
    if (name.startsWith(QLatin1Char('*')))
        name = name.mid(1);
    return subtitleList_->getStyleList().getStyleByName(name);
}

SubEntryPtr SubStationAlpha::getSubEntry(const QRegularExpressionMatch &m) {
    const QStringList fields = splitFields(m.captured(1), eventFormat_.size());
    if (fields.size() < eventFormat_.size())
        return nullptr;
    auto field = [&](const QString &key) { const int i = columnIndex(eventFormat_, key); return i >= 0 ? fields.at(i) : QString(); };
    Time start, finish;
    if (!parseTime(field(QStringLiteral("start")), start) || !parseTime(field(QStringLiteral("end")), finish))
        return nullptr;
    const int textIdx = columnIndex(eventFormat_, QStringLiteral("text"));
    QString text = textIdx >= 0 ? fields.at(textIdx) : fields.last();
    // "\{" / "\}" are literal braces; "\N" a line break, "\h" a hard space;
    // "\n" is a break only with WrapStyle 2, else it stays as written.
    text.replace(QLatin1String("\\{"), QString(QChar(OPEN_STANDIN))).replace(QLatin1String("\\}"), QString(QChar(CLOSE_STANDIN)));
    text.replace(QLatin1String("\\N"), QLatin1String("\n")).replace(QLatin1String("\\h"), QString(QChar(NBSP)));
    if (hardSoftBreaks_)
        text.replace(QLatin1String("\\n"), QLatin1String("\n"));
    // "\b<weight>" inside override blocks: 700 and heavier is bold, 400 and
    // lighter is not (as libass draws them); 500/600 stay as written.
    static const QRegularExpression block(QStringLiteral("\\{[^}]*\\}"));
    static const QRegularExpression weight(QStringLiteral("\\\\b(\\d{3,})"));
    for (auto it = block.globalMatch(text); it.hasNext();) {
        const auto b = it.next();
        QString tags = b.captured(0);
        const QString before = tags;
        for (auto w = weight.globalMatch(before); w.hasNext();) {
            const auto m = w.next();
            const int v = m.captured(1).toInt();
            if (v >= 700) tags.replace(m.captured(0), QStringLiteral("\\b1"));
            else if (v <= 400) tags.replace(m.captured(0), QStringLiteral("\\b0"));
        }
        if (tags != before) {
            text.replace(b.capturedStart(), b.capturedLength(), tags);
            it = block.globalMatch(text, b.capturedStart() + tags.length());
        }
    }
    auto entry = std::make_shared<SubEntry>(start, finish, text);
    entry->setStyle(styleForName(field(QStringLiteral("style"))));
    QString layer = field(QStringLiteral("layer")).trimmed();
    if (layer.startsWith(QLatin1String("Marked="), Qt::CaseInsensitive))  // SSA "Marked=0" → plain layer
        layer = layer.mid(7).trimmed();
    entry->setLayer(layer);
    entry->setName(field(QStringLiteral("name")).trimmed());
    entry->setMarginL(field(QStringLiteral("marginl")).trimmed());
    entry->setMarginR(field(QStringLiteral("marginr")).trimmed());
    entry->setMarginV(field(QStringLiteral("marginv")).trimmed());
    entry->setEffect(field(QStringLiteral("effect")).trimmed());
    parseSubText(*entry);
    entry->setText(QString(entry->getText()).replace(QChar(OPEN_STANDIN), QLatin1Char('{')).replace(QChar(CLOSE_STANDIN), QLatin1Char('}')));
    return entry;
}

QString SubStationAlpha::timeformat(const Time &t) {
    // "HH:MM:SS.mmm" → "H:MM:SS.cc" (centiseconds by truncation; hours ≥ 10 keep both digits)
    QString res = t.getSeconds(QLatin1Char('.'));
    if (res.startsWith(QLatin1Char('0')))
        res = res.mid(1);
    res.chop(1);
    return res;
}

QString SubStationAlpha::stripAlignmentTags(QString text) {
    static const QRegularExpression an(QStringLiteral("\\{([^}]*)\\\\an[1-9]([^}]*)\\}"));
    static const QRegularExpression a(QStringLiteral("\\{([^}]*)\\\\a[0-9]+([^}]*)\\}"));
    static const QRegularExpression empty(QStringLiteral("\\{\\s*\\}"));
    text.replace(an, QStringLiteral("{\\1\\2}"));
    text.replace(a, QStringLiteral("{\\1\\2}"));
    text.remove(empty);
    return text;
}

QString SubStationAlpha::rebuildSubTextWithOverrides(const SubEntry &sub) {
    QString text = stripAlignmentTags(rebuildSubText(sub));
    // Comments come out as "\<mark>text" inside a tag block: each gets its own
    // block again.
    static const QRegularExpression comment(QStringLiteral("\\\\\\x{E002}([^\\\\}]*)"));
    if (text.contains(QChar(COMMENT_MARK))) {
        text.replace(comment, QStringLiteral("}{\\1}{"));
        text.remove(QStringLiteral("{}"));
    }
    if (sub.getStyle()) {
        if (const Styleover *over = sub.getStyleover(StyleType::DIRECTION); over && over->size()) {
            const Direction styleDir = sub.getStyle()->direction();
            Styleover copy(*over);
            const auto value = copy.getValue(0, text.length(), StyleValue(styleDir), sub.getText());
            if (value && std::holds_alternative<Direction>(*value) && std::get<Direction>(*value) != styleDir)
                text = alignmentTag(std::get<Direction>(*value)) + text;
        }
        // A whole-entry rotation (WebVTT vertical text).
        if (const auto angle = leadingOverride(sub, StyleType::ANGLE); angle && std::holds_alternative<float>(*angle)
            && std::get<float>(*angle) != sub.getStyle()->real(StyleType::ANGLE))
            text = angleTag(std::get<float>(*angle)) + text;
    }
    return text;
}

QString SubStationAlpha::rebuildEscapedSubText(const SubEntry &sub) {
    SubEntry copy(sub);
    copy.setText(QString(sub.getText()).replace(QLatin1Char('{'), QChar(OPEN_STANDIN)).replace(QLatin1Char('}'), QChar(CLOSE_STANDIN)));
    QString out = sub.hasStyleovers() ? rebuildSubTextWithOverrides(copy) : copy.getText();
    return out.replace(QChar(OPEN_STANDIN), QLatin1String("\\{")).replace(QChar(CLOSE_STANDIN), QLatin1String("\\}"));
}

// SSA writes the legacy {\a#} numbering, ASS the numpad {\an#}.
QString SubStationAlpha::alignmentTag(Direction d) {
    return QStringLiteral("{\\a%1}").arg(getDirectionKey(ssaDirections(), d));
}

QString AdvancedSubStation::alignmentTag(Direction d) {
    return QStringLiteral("{\\an%1}").arg(directionToAn(d));
}

void SubStationAlpha::appendSubEntry(const SubEntry &sub, QString &str) {
    str += QLatin1String("Dialogue: ");
    if (getLayerTitle() == QLatin1String("Marked"))  // SSA: a 0/1 flag, not a layer number
        str += QLatin1String("Marked=") + (sub.getLayer() == QLatin1String("1") ? QStringLiteral("1") : QStringLiteral("0")) + QLatin1Char(',');
    else
        str += sub.getLayer() + QLatin1Char(',');
    str += timeformat(sub.getStartTime()) + QLatin1Char(',');
    str += timeformat(sub.getFinishTime()) + QLatin1Char(',');
    // Style names are written plain (the legacy "*Default" marker is only
    // understood on input).
    str += fieldText(sub.getStyle() ? sub.getStyle()->getName() : QStringLiteral("Default"));
    str += QLatin1Char(',') + fieldText(sub.getName()) + QLatin1Char(',');
    str += sub.getMarginL() + QLatin1Char(',') + sub.getMarginR() + QLatin1Char(',') + sub.getMarginV() + QLatin1Char(',');
    str += fieldText(sub.getEffect()) + QLatin1Char(',');
    QString text = rebuildEscapedSubText(sub);
    str += text.replace(QLatin1String("\n"), QLatin1String("\\N")).replace(QChar(NBSP), QLatin1String("\\h"));
    str += QLatin1Char('\n');
}

void SubStationAlpha::initSaver(const Subtitles &subs, const MediaFile *media, QString &header) {
    saveData_ = subs.getFormatData();
    auto fixedInfo = [this](const char *key, const char *deflt) {
        const QString value = saveData_.value(QStringLiteral("ssa.") + QLatin1String(key));
        return value.isEmpty() ? QString::fromLatin1(deflt) : value;
    };
    header += QLatin1String("[Script Info]\n");
    const SubAttribs &attr = subs.getAttribs();
    QString com = attr.comments;
    if (!com.trimmed().isEmpty()) {
        com.replace(QLatin1String("\n"), QLatin1String("\n; "));
        header += QLatin1String("; ") + com + QLatin1Char('\n');
    }
    header += QLatin1String("Title: ") + attr.title;
    header += QLatin1String("\nOriginal Script: ") + attr.author;
    header += QLatin1String("\nUpdate Details: ") + attr.source;
    header += QLatin1String("\nScriptType: v4.00") + getExtraVersion();
    header += QLatin1String("\nCollisions: ") + fixedInfo("collisions", "Normal") + QLatin1Char('\n');
    // A loaded SSA/ASS keeps its canvas: positions, margins and borders are in
    // its units. Otherwise PlayResX/Y are always emitted so the file is
    // self-consistent: the style font sizes are scaled to this height. With a
    // video its size, else the reference height (core sizes verbatim) on a
    // 16:9 canvas.
    int playResY = saveData_.value(KEY_PLAYRESY).toInt();
    int playResX = saveData_.value(KEY_PLAYRESX).toInt();
    if (playResX <= 0 || playResY <= 0) {
        int videoH = 0, videoW = 0;
        if (media && media->getVideoFile()) {
            videoH = media->getVideoFile()->getHeight();
            videoW = media->getVideoFile()->getWidth();
        }
        playResY = videoH > 0 ? videoH : FONT_REF;
        playResX = videoW > 0 ? videoW : qRound(playResY * 16.0f / 9.0f);
    }
    playResY_ = playResY;
    fontFactor_ = playResY / float(FONT_REF);
    header += QLatin1String("PlayResX: ") + QString::number(playResX);
    header += QLatin1String("\nPlayResY: ") + QString::number(playResY) + QLatin1Char('\n');
    header += QLatin1String("PlayDepth: ") + fixedInfo("playdepth", "0") + QLatin1Char('\n');
    header += QLatin1String("Timer: ") + fixedInfo("timer", "100,0000") + QLatin1Char('\n');
    if (saveData_.contains(KEY_INFO))
        header += saveData_.value(KEY_INFO) + QLatin1Char('\n');
    if (saveData_.contains(KEY_HEAD))
        header += QLatin1Char('\n') + saveData_.value(KEY_HEAD) + QLatin1Char('\n');
    header += QLatin1String("\n[V4") + getExtraVersion() + QLatin1String(" Styles]\n");
    appendStyles(subs, header);
    header += QLatin1String("\n[Events]\nFormat: ") + getLayerTitle();
    header += QLatin1String(", Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");
}

void SubStationAlpha::cleanupSaver(QString &str) {
    if (saveData_.contains(KEY_EVENTS))
        str += saveData_.value(KEY_EVENTS) + QLatin1Char('\n');
    if (saveData_.contains(KEY_TAIL))
        str += QLatin1Char('\n') + saveData_.value(KEY_TAIL) + QLatin1Char('\n');
}

AlphaColor SubStationAlpha::stringToAlphaColor(const QString &revRGB, const QString &alphaStr) const {
    const long long lrgb = parseNumber(revRGB);
    const unsigned rgb = unsigned(lrgb & 0xffffff);
    int alpha = int((lrgb >> 24) & 0xff);
    if (!alphaStr.isNull())
        alpha = int(parseNumber(alphaStr));
    return AlphaColor((unsigned(invertAlpha(alpha)) << 24) | reverseByteOrder(rgb));
}

QString SubStationAlpha::alphaColorToString(const AlphaColor &c, bool storeAlpha) const {
    const long long rgb = reverseByteOrder(c.rgb());
    const long long alpha = storeAlpha ? (static_cast<long long>(invertAlpha(c.alpha())) << 24) : 0;
    return produceHexNumber(alpha | rgb, false, storeAlpha ? 8 : 6);
}

void SubStationAlpha::appendStyles(const Subtitles &subs, QString &header) {
    header += QLatin1String("Format: ") + styleFormat() + QLatin1Char('\n');
    for (const SubStylePtr &style : subs.getStyleList().all()) {
        header += QLatin1String("Style: ");
        header += fieldText(style->getName()) + QLatin1Char(',');
        header += style->fontName() + QLatin1Char(',');
        header += fontSizeFor(*style, style->fontSize()) + QLatin1Char(',');
        header += alphaColorToString(style->color(StyleType::PRIMARY), false) + QLatin1Char(',');
        header += alphaColorToString(style->color(StyleType::SECONDARY), false) + QLatin1Char(',');
        header += alphaColorToString(style->color(StyleType::OUTLINE), false) + QLatin1Char(',');
        header += alphaColorToString(style->color(StyleType::SHADOW), false) + QLatin1Char(',');
        header += QString::number(booleanToInt(style->flag(StyleType::BOLD))) + QLatin1Char(',');
        header += QString::number(booleanToInt(style->flag(StyleType::ITALIC))) + QLatin1Char(',');
        header += QString::number(style->integral(StyleType::BORDERSTYLE) == 0 ? 1 : 3) + QLatin1Char(',');
        header += StyleType::get(StyleType::BORDERSIZE, style->get(StyleType::BORDERSIZE)) + QLatin1Char(',');
        header += StyleType::get(StyleType::SHADOWSIZE, style->get(StyleType::SHADOWSIZE)) + QLatin1Char(',');
        header += getDirectionKey(ssaDirections(), style->direction()) + QLatin1Char(',');
        header += QString::number(style->integral(StyleType::LEFTMARGIN)) + QLatin1Char(',');
        header += QString::number(style->integral(StyleType::RIGHTMARGIN)) + QLatin1Char(',');
        header += QString::number(style->integral(StyleType::VERTICAL)) + QLatin1Char(',');
        // AlphaLevel uses the file convention (0 = opaque), the same one the
        // reader applies.
        header += QString::number(invertAlpha(style->color(StyleType::PRIMARY).alpha())) + QLatin1Char(',');
        header += keptStyleText(*style, QStringLiteral("Encoding"), QString(), QStringLiteral("0")) + QLatin1Char('\n');
    }
}

void AdvancedSubStation::appendStyles(const Subtitles &subs, QString &header) {
    header += QLatin1String("Format: ") + styleFormat() + QLatin1Char('\n');
    for (const SubStylePtr &style : subs.getStyleList().all()) {
        header += QLatin1String("Style: ");
        header += fieldText(style->getName()) + QLatin1Char(',');
        header += style->fontName() + QLatin1Char(',');
        header += fontSizeFor(*style, style->fontSize()) + QLatin1Char(',');
        header += alphaColorToString(style->color(StyleType::PRIMARY), true) + QLatin1Char(',');
        header += alphaColorToString(style->color(StyleType::SECONDARY), true) + QLatin1Char(',');
        header += alphaColorToString(style->color(StyleType::OUTLINE), true) + QLatin1Char(',');
        header += alphaColorToString(style->color(StyleType::SHADOW), true) + QLatin1Char(',');
        header += QString::number(booleanToInt(style->flag(StyleType::BOLD))) + QLatin1Char(',');
        header += QString::number(booleanToInt(style->flag(StyleType::ITALIC))) + QLatin1Char(',');
        header += QString::number(booleanToInt(style->flag(StyleType::UNDERLINE))) + QLatin1Char(',');
        header += QString::number(booleanToInt(style->flag(StyleType::STRIKETHROUGH))) + QLatin1Char(',');
        header += keptStyleText(*style, QStringLiteral("ScaleX"), QString::number(style->integral(StyleType::XSCALE)),
                                QString::number(style->integral(StyleType::XSCALE))) + QLatin1Char(',');
        header += keptStyleText(*style, QStringLiteral("ScaleY"), QString::number(style->integral(StyleType::YSCALE)),
                                QString::number(style->integral(StyleType::YSCALE))) + QLatin1Char(',');
        header += StyleType::get(StyleType::SPACING, style->get(StyleType::SPACING)) + QLatin1Char(',');
        header += StyleType::get(StyleType::ANGLE, style->get(StyleType::ANGLE)) + QLatin1Char(',');
        header += QString::number(style->integral(StyleType::BORDERSTYLE) == 0 ? 1 : 3) + QLatin1Char(',');
        header += StyleType::get(StyleType::BORDERSIZE, style->get(StyleType::BORDERSIZE)) + QLatin1Char(',');
        header += StyleType::get(StyleType::SHADOWSIZE, style->get(StyleType::SHADOWSIZE)) + QLatin1Char(',');
        header += getDirectionKey(assDirections(), style->direction()) + QLatin1Char(',');
        header += QString::number(style->integral(StyleType::LEFTMARGIN)) + QLatin1Char(',');
        header += QString::number(style->integral(StyleType::RIGHTMARGIN)) + QLatin1Char(',');
        header += QString::number(style->integral(StyleType::VERTICAL)) + QLatin1Char(',');
        header += keptStyleText(*style, QStringLiteral("Encoding"), QString(), QStringLiteral("0")) + QLatin1Char('\n');
    }
}

QString AdvancedSubStation::toTaggedText(const SubEntry &entry) {
    return AdvancedSubStation().rebuildEscapedSubText(entry);
}

void AdvancedSubStation::setTaggedText(SubEntry &entry, const QString &text) {
    entry.resetOverStyle();
    entry.setText(QString(text).replace(QLatin1String("\\{"), QString(QChar(OPEN_STANDIN))).replace(QLatin1String("\\}"), QString(QChar(CLOSE_STANDIN))));
    AdvancedSubStation f;
    f.parseSubText(entry);
    entry.setText(QString(entry.getText()).replace(QChar(OPEN_STANDIN), QLatin1Char('{')).replace(QChar(CLOSE_STANDIN), QLatin1Char('}')));
}
