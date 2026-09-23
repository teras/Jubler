/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/formats/SimpleStyledTextSubFormat.h"

#include <algorithm>
#include <cmath>

#include "core/os/Debug.h"

const QRegularExpression &SimpleStyledTextSubFormat::getStylePattern() {
    // A tag starts with its name (or '/'): "a < b and c > d" is text (the
    // Java took " b and c " for a tag and lost it).
    static const QRegularExpression pat(QStringLiteral("<(/?[A-Za-z][^<>]*)>"));
    return pat;
}

const QMap<QString, QString> &SimpleStyledTextSubFormat::getStylePairs() {
    static const QMap<QString, QString> pairs = {
        {QStringLiteral("i"), QStringLiteral("/i")}, {QStringLiteral("b"), QStringLiteral("/b")},
        {QStringLiteral("u"), QStringLiteral("/u")}, {QStringLiteral("s"), QStringLiteral("/s")}};
    return pairs;
}

const QList<StyledFormat> &SimpleStyledTextSubFormat::getStylesDictionary() {
    static const QList<StyledFormat> dict = {
        StyledFormat(StyleType::ITALIC, QStringLiteral("i"), true),
        StyledFormat(StyleType::ITALIC, QStringLiteral("/i"), false),
        StyledFormat(StyleType::BOLD, QStringLiteral("b"), true),
        StyledFormat(StyleType::BOLD, QStringLiteral("/b"), false),
        StyledFormat(StyleType::UNDERLINE, QStringLiteral("u"), true),
        StyledFormat(StyleType::UNDERLINE, QStringLiteral("/u"), false),
        StyledFormat(StyleType::STRIKETHROUGH, QStringLiteral("s"), true),
        StyledFormat(StyleType::STRIKETHROUGH, QStringLiteral("/s"), false),
        // Per-character font runs: the engine emits these internal tags on
        // save and rebuildSubText() turns them into <font …>.
        StyledFormat(StyleType::FONTNAME, QStringLiteral("fn"), QString(QLatin1String("")), true),
        StyledFormat(StyleType::FONTSIZE, QStringLiteral("fs"), QString(QLatin1String("")), true),
        StyledFormat(StyleType::PRIMARY, QStringLiteral("fc"), StyledFormat::COLOR_NORMAL, true)};
    return dict;
}

bool SimpleStyledTextSubFormat::produce(const Subtitles &subs, const QString &outfile, const MediaFile *media, SaveError &error) {
    exportDefaultStyle_ = subs.getStyleList().size() ? subs.getStyleList().get(0).get() : nullptr;
    const bool ok = GenericStyledTextSubFormat::produce(subs, outfile, media, error);
    exportDefaultStyle_ = nullptr;
    return ok;
}

void SimpleStyledTextSubFormat::initSaver(const Subtitles &subs, const MediaFile *, QString &) {
    exportDefaultStyle_ = subs.getStyleList().size() ? subs.getStyleList().get(0).get() : nullptr;
}

namespace {
class TagConverter : public SimpleStyledTextSubFormat {
public:
    QString getExtension() const override { return QString(); }
    QString getName() const override { return QString(); }
    bool supportsFPS() const override { return false; }
    std::shared_ptr<SubFormat> newInstance() const override { return nullptr; }
    void convert(SubEntry &entry) { parseSubText(entry); }

protected:
    const QRegularExpression &getPattern() override { static const QRegularExpression r; return r; }
    const QRegularExpression &getTestPattern() override { return getPattern(); }
    SubEntryPtr getSubEntry(const QRegularExpressionMatch &) override { return nullptr; }
    void appendSubEntry(const SubEntry &, QString &) override {}
    bool isEventCompact() override { return false; }
};
}  // namespace

void SimpleStyledTextSubFormat::htmlTagsToOverrides(SubEntry &entry) {
    TagConverter().convert(entry);
}

SubEntryPtr SimpleStyledTextSubFormat::makeSubEntry(const Time &start, const Time &finish, const QString &input) {
    auto entry = std::make_shared<SubEntry>(start, finish, input);
    entry->setStyle(subtitleList_->getStyleList().get(0));
    parseSubText(*entry);
    return entry;
}

bool SimpleStyledTextSubFormat::parseColor(const QString &textIn, QColor &out) {
    if (textIn.isEmpty())
        return false;
    const QString text = textIn.trimmed().toLower();
    static const QHash<QString, QColor> named = {
        {QStringLiteral("black"), QColor(0, 0, 0)}, {QStringLiteral("white"), QColor(255, 255, 255)},
        {QStringLiteral("red"), QColor(255, 0, 0)}, {QStringLiteral("green"), QColor(0, 255, 0)},
        {QStringLiteral("blue"), QColor(0, 0, 255)}, {QStringLiteral("yellow"), QColor(255, 255, 0)},
        {QStringLiteral("cyan"), QColor(0, 255, 255)}, {QStringLiteral("magenta"), QColor(255, 0, 255)},
        {QStringLiteral("gray"), QColor(128, 128, 128)}, {QStringLiteral("grey"), QColor(128, 128, 128)},
        {QStringLiteral("orange"), QColor(255, 200, 0)}, {QStringLiteral("pink"), QColor(255, 175, 175)},
        {QStringLiteral("darkred"), QColor(178, 0, 0)}, {QStringLiteral("darkgreen"), QColor(0, 178, 0)},
        {QStringLiteral("darkblue"), QColor(0, 0, 178)},
        {QStringLiteral("lightgray"), QColor(192, 192, 192)}, {QStringLiteral("lightgrey"), QColor(192, 192, 192)},
        {QStringLiteral("darkgray"), QColor(64, 64, 64)}, {QStringLiteral("darkgrey"), QColor(64, 64, 64)}};
    if (named.contains(text)) {
        out = named.value(text);
        return true;
    }
    static const QRegularExpression hex(QStringLiteral("#([0-9a-fA-F]{6})"));
    QRegularExpressionMatch m = hex.match(text);
    if (m.hasMatch()) {
        out = QColor(QLatin1Char('#') + m.captured(1));
        return out.isValid();
    }
    static const QRegularExpression rgb(QStringLiteral("rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)"));
    m = rgb.match(text);
    if (m.hasMatch()) {
        const int r = m.captured(1).toInt(), g = m.captured(2).toInt(), b = m.captured(3).toInt();
        if (r > 255 || g > 255 || b > 255)
            return false;
        out = QColor(r, g, b);
        return true;
    }
    return false;
}

void SimpleStyledTextSubFormat::beginParseSubText(SubEntry &) {
    fontStack_.clear();
}

void SimpleStyledTextSubFormat::endParseSubText(SubEntry &entry) {
    if (!fontStack_.isEmpty())
        Debug::debug(QStringLiteral("Font tag without closing tag found: ") + entry.getText());
    fontStack_.clear();
}

bool SimpleStyledTextSubFormat::handleTagEvent(const QString &bodyIn, int pos, SubEntry &entry, const SubStylePtr &style) {
    static const QRegularExpression attr(QStringLiteral("(color|size|face)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))"),
                                         QRegularExpression::CaseInsensitiveOption);
    const QString body = bodyIn.trimmed();
    // The tag name: up to a space or a WebVTT class ("b.loud"), compared
    // exactly and case-insensitively (<span> is not <s>, <br> not <b>, <B> is
    // <b>; the Java matched name prefixes, case-sensitively).
    int nameEnd = 0;
    while (nameEnd < body.length() && !body.at(nameEnd).isSpace() && body.at(nameEnd) != QLatin1Char('.'))
        ++nameEnd;
    const QString lower = body.left(nameEnd).toLower();
    static const QHash<QString, std::pair<StyleType::Id, bool>> flags = {
        {QStringLiteral("i"), {StyleType::ITALIC, true}}, {QStringLiteral("/i"), {StyleType::ITALIC, false}},
        {QStringLiteral("b"), {StyleType::BOLD, true}}, {QStringLiteral("/b"), {StyleType::BOLD, false}},
        {QStringLiteral("u"), {StyleType::UNDERLINE, true}}, {QStringLiteral("/u"), {StyleType::UNDERLINE, false}},
        {QStringLiteral("s"), {StyleType::STRIKETHROUGH, true}}, {QStringLiteral("/s"), {StyleType::STRIKETHROUGH, false}},
        // <em>/<strong> are the semantic twins of <i>/<b>.
        {QStringLiteral("em"), {StyleType::ITALIC, true}}, {QStringLiteral("/em"), {StyleType::ITALIC, false}},
        {QStringLiteral("strong"), {StyleType::BOLD, true}}, {QStringLiteral("/strong"), {StyleType::BOLD, false}}};
    if (const auto it = flags.constFind(lower); it != flags.constEnd()) {
        entry.addOverStyle(it->first, StyleValue(it->second), pos);
        return true;
    }
    if (lower == QLatin1String("/font")) {
        if (fontStack_.isEmpty())
            return true;  // stray </font>
        const FontState closed = fontStack_.takeLast();
        auto current = [&](auto member) -> decltype(member(FontState{})) {
            for (int i = fontStack_.size() - 1; i >= 0; --i)
                if (member(fontStack_[i])) return member(fontStack_[i]);
            return {};
        };
        if (closed.color) {
            auto c = current([](const FontState &s) { return s.color; });
            entry.addOverStyle(StyleType::PRIMARY, StyleValue(c ? *c : style->color(StyleType::PRIMARY)), pos);
        }
        if (closed.size) {
            auto c = current([](const FontState &s) { return s.size; });
            entry.addOverStyle(StyleType::FONTSIZE, StyleValue(c ? *c : style->fontSize()), pos);
        }
        if (closed.name) {
            auto c = current([](const FontState &s) { return s.name; });
            entry.addOverStyle(StyleType::FONTNAME, StyleValue(c ? *c : style->fontName()), pos);
        }
        return true;
    }
    if (lower != QLatin1String("font"))
        return true;   // any other tag is dropped
    FontState s;
    auto ait = attr.globalMatch(body.mid(4));
    while (ait.hasNext()) {
        const QRegularExpressionMatch a = ait.next();
        const QString key = a.captured(1).toLower();
        QString val = a.captured(2);
        if (val.isNull()) val = a.captured(3);
        if (val.isNull()) val = a.captured(4);
        if (key == QLatin1String("color")) {
            QColor c;
            if (parseColor(val, c)) s.color = AlphaColor(c, 255);
            else Debug::debug(QStringLiteral("Invalid color in font tag: ") + val);
        } else if (key == QLatin1String("size")) {
            const QString v = val.trimmed();
            bool ok = false;
            const int sz = v.toInt(&ok);
            if (ok && (v.startsWith(QLatin1Char('+')) || v.startsWith(QLatin1Char('-')))) {
                // HTML relative size: steps on the 1…7 scale from the base 3,
                // applied to the style's size (3 = the style size).
                static const int px[] = {10, 13, 16, 18, 24, 32, 48};
                const int step = std::clamp(3 + sz, 1, 7);
                s.size = std::max(1, int(std::lround(style->fontSize() * px[step - 1] / 16.0)));
            } else if (ok && sz > 0)
                s.size = sz;
        } else if (key == QLatin1String("face")) {
            if (!val.trimmed().isEmpty()) s.name = val.trimmed();
        }
    }
    fontStack_.append(s);
    if (s.color) entry.addOverStyle(StyleType::PRIMARY, StyleValue(*s.color), pos);
    if (s.size) entry.addOverStyle(StyleType::FONTSIZE, StyleValue(*s.size), pos);
    if (s.name) entry.addOverStyle(StyleType::FONTNAME, StyleValue(*s.name), pos);
    return true;
}

QString SimpleStyledTextSubFormat::applyLineStylesToText(const SubEntry &entry, int &prefixLen) {
    prefixLen = 0;
    const QString text = entry.getText();
    const SubStylePtr style = entry.getStyle();
    if (text.isEmpty() || !style || style->getName() == QLatin1String("Default"))
        return text;
    QString defaultFamily = QStringLiteral("Arial");
    int defaultSize = 16;
    QString defaultColor = QStringLiteral("#ffffff");
    if (exportDefaultStyle_) {
        defaultFamily = exportDefaultStyle_->fontName();
        defaultSize = exportDefaultStyle_->fontSize();
        defaultColor = QStringLiteral("#%1").arg(exportDefaultStyle_->color(StyleType::PRIMARY).rgb(), 6, 16, QLatin1Char('0'));
    }
    const bool bold = style->flag(StyleType::BOLD), italic = style->flag(StyleType::ITALIC), underline = style->flag(StyleType::UNDERLINE);
    const QString colorValue = QStringLiteral("#%1").arg(style->color(StyleType::PRIMARY).rgb(), 6, 16, QLatin1Char('0'));
    const bool needsColor = colorValue != defaultColor;
    const bool needsSize = style->fontSize() != defaultSize;
    const QString family = style->fontName();
    const bool needsFamily = !family.trimmed().isEmpty() && family != defaultFamily;
    QString prefix, suffix;
    if (needsColor || needsSize || needsFamily) {
        prefix += QLatin1String("<font");
        if (needsColor) prefix += QLatin1String(" color=\"") + colorValue + QLatin1Char('"');
        if (needsSize) prefix += QLatin1String(" size=\"") + QString::number(style->fontSize()) + QLatin1Char('"');
        if (needsFamily) prefix += QLatin1String(" face=\"") + family + QLatin1Char('"');
        prefix += QLatin1Char('>');
        suffix = QLatin1String("</font>");
    }
    if (bold) { prefix += QLatin1String("<b>"); suffix = QLatin1String("</b>") + suffix; }
    if (italic) { prefix += QLatin1String("<i>"); suffix = QLatin1String("</i>") + suffix; }
    if (underline) { prefix += QLatin1String("<u>"); suffix = QLatin1String("</u>") + suffix; }
    prefixLen = prefix.length();
    return prefix + text + suffix;
}

QString SimpleStyledTextSubFormat::rebuildSubText(const SubEntry &entry) {
    // Per-character tags first (positions refer to the plain text), then the
    // line-level wrapping of a non-Default style around the result.
    const QString inner = convertFontRunsToTags(GenericStyledTextSubFormat::rebuildSubText(entry), entry);
    SubEntry tmp(entry);
    tmp.resetOverStyle();
    tmp.setText(inner);
    int prefixLen = 0;
    return repairNesting(applyLineStylesToText(tmp, prefixLen));
}

QString SimpleStyledTextSubFormat::repairNesting(const QString &text) {
    static const QRegularExpression tag(QStringLiteral("<(/?)([A-Za-z][A-Za-z0-9]*)([.][^\\s>]*)?((?:\\s[^>]*)?)>"));
    struct Open { QString name, full; };
    QList<Open> stack;
    QString out;
    int last = 0;
    auto it = tag.globalMatch(text);
    while (it.hasNext()) {
        const QRegularExpressionMatch m = it.next();
        out += text.mid(last, m.capturedStart(0) - last);
        last = m.capturedEnd(0);
        const QString name = m.captured(2).toLower();
        if (m.captured(1).isEmpty()) {
            stack.append({name, m.captured(0)});
            out += m.captured(0);
            continue;
        }
        int k = int(stack.size()) - 1;
        while (k >= 0 && stack[k].name != name) --k;
        if (k < 0)   // nothing open to close: dropped
            continue;
        for (int i = int(stack.size()) - 1; i > k; --i) out += QStringLiteral("</") + stack[i].name + QLatin1Char('>');
        out += m.captured(0);
        for (int i = k + 1; i < stack.size(); ++i) out += stack[i].full;
        stack.removeAt(k);
    }
    out += text.mid(last);
    // Reopened tags that close right away.
    static const QRegularExpression empty(QStringLiteral("<([A-Za-z][A-Za-z0-9]*)(?:[.][^\\s>]*)?(?:\\s[^>]*)?></\\1>"));
    QString prev;
    while (prev != out) {
        prev = out;
        out.replace(empty, QString());
    }
    return out;
}

// Turn the engine's internal <fn…>/<fs…>/<fc&HRRGGBB&> run markers into
// <font color/size/face> ranges. A run attribute equal to the entry style's
// own value needs no tag.
QString SimpleStyledTextSubFormat::convertFontRunsToTags(const QString &text, const SubEntry &entry) {
    static const QRegularExpression fontTag(QStringLiteral("<((?:fn|fs|fc)[^>]*)>"));
    if (!fontTag.match(text).hasMatch())
        return text;
    const SubStylePtr style = entry.getStyle();
    const QString styleColor = style ? QStringLiteral("#%1").arg(style->color(StyleType::PRIMARY).rgb(), 6, 16, QLatin1Char('0')) : QStringLiteral("#ffffff");
    const int styleSize = style ? style->fontSize() : 0;
    const QString styleName = style ? style->fontName() : QString();
    struct Attrs { QString name, color; int size = 0; bool hasSize = false; };
    Attrs cur;
    QString out;
    bool open = false;
    int last = 0;
    auto it = fontTag.globalMatch(text);
    auto flush = [&](const Attrs &a) {
        if (open) { out += QLatin1String("</font>"); open = false; }
        const bool needColor = !a.color.isEmpty() && a.color.compare(styleColor, Qt::CaseInsensitive) != 0;
        const bool needSize = a.hasSize && a.size != styleSize;
        const bool needName = !a.name.isEmpty() && a.name != styleName;
        if (needColor || needSize || needName) {
            out += QLatin1String("<font");
            if (needColor) out += QLatin1String(" color=\"") + a.color + QLatin1Char('"');
            if (needSize) out += QLatin1String(" size=\"") + QString::number(a.size) + QLatin1Char('"');
            if (needName) out += QLatin1String(" face=\"") + a.name + QLatin1Char('"');
            out += QLatin1Char('>');
            open = true;
        }
    };
    while (it.hasNext()) {
        const QRegularExpressionMatch m = it.next();
        out += text.mid(last, m.capturedStart(0) - last);
        last = m.capturedEnd(0);
        const QString body = m.captured(1);
        if (body.startsWith(QLatin1String("fs"))) {
            cur.size = body.mid(2).toInt();
            cur.hasSize = true;
        } else if (body.startsWith(QLatin1String("fc"))) {
            const long long n = parseNumber(body.mid(2));
            cur.color = QStringLiteral("#%1").arg(n & 0xffffff, 6, 16, QLatin1Char('0'));
        } else if (body.startsWith(QLatin1String("fn"))) {
            cur.name = body.mid(2).trimmed();
        }
        // Merge consecutive markers at the same position into one tag.
        if (it.hasNext()) {
            const QRegularExpressionMatch next = it.peekNext();
            if (next.capturedStart(0) == last)
                continue;
        }
        flush(cur);
    }
    out += text.mid(last);
    if (open)
        out += QLatin1String("</font>");
    return out;
}
