/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/formats/text/W3CFamily.h"

#include <cmath>
#include <numeric>

#include "core/formats/SimpleStyledTextSubFormat.h"
#include "core/os/Debug.h"
#include "core/util/JavaCompat.h"

const QString W3CFamily::TTML_NS = QStringLiteral("http://www.w3.org/ns/ttml");
const QString W3CFamily::TTML_STYLING_NS = QStringLiteral("http://www.w3.org/ns/ttml#styling");
const QString W3CFamily::TTML_PARAMETER_NS = QStringLiteral("http://www.w3.org/ns/ttml#parameter");
const QString W3CFamily::TTML_METADATA_NS = QStringLiteral("http://www.w3.org/ns/ttml#metadata");

QString W3CFamily::timeBaseValue(TimeBase t) {
    switch (t) { case TimeBase::SMPTE: return QStringLiteral("smpte"); case TimeBase::CLOCK: return QStringLiteral("clock"); default: return QStringLiteral("media"); }
}
QString W3CFamily::dropModeValue(DropMode d) {
    switch (d) { case DropMode::DROP_NTSC: return QStringLiteral("dropNTSC"); case DropMode::DROP_PAL: return QStringLiteral("dropPAL"); default: return QStringLiteral("nonDrop"); }
}
QString W3CFamily::profileValue(TTMLProfile p) {
    switch (p) {
        case TTMLProfile::DFXP_TRANSFORMATION: return QStringLiteral("http://www.w3.org/ns/ttml/profile/dfxp-transformation");
        case TTMLProfile::DFXP_FULL: return QStringLiteral("http://www.w3.org/ns/ttml/profile/dfxp-full");
        case TTMLProfile::IMSC1_TEXT: return QStringLiteral("http://www.w3.org/ns/ttml/profile/imsc1/text");
        case TTMLProfile::IMSC1_IMAGE: return QStringLiteral("http://www.w3.org/ns/ttml/profile/imsc1/image");
        default: return QStringLiteral("http://www.w3.org/ns/ttml/profile/dfxp-presentation");
    }
}

bool W3CFamily::isFontAllowed(const QString &family) const {
    switch (getFontRestriction()) {
        case FontRestriction::SANS_SERIF_ONLY: return family == QLatin1String("sansSerif");
        case FontRestriction::BASIC_FONTS:
            return family == QLatin1String("sansSerif") || family == QLatin1String("serif") || family == QLatin1String("monospace");
        default: return true;
    }
}

// ---- RegionInfo ---------------------------------------------------------------

namespace {
// 0 top/left, 1 centre, 2 bottom/right, -1 unknown (case-insensitive, as the Java).
int displayAlignValue(const QString &v) {
    const QString a = v.trimmed().toLower();
    if (a == QLatin1String("before")) return 0;
    if (a == QLatin1String("center")) return 1;
    if (a == QLatin1String("after")) return 2;
    return -1;
}
int textAlignValue(const QString &v) {
    const QString a = v.trimmed().toLower();
    if (a == QLatin1String("left") || a == QLatin1String("start")) return 0;
    if (a == QLatin1String("center")) return 1;
    if (a == QLatin1String("right") || a == QLatin1String("end")) return 2;
    return -1;
}
Direction directionOf(int vertical, int horizontal) {
    static const Direction table[3][3] = {
        {Direction::TOPLEFT, Direction::TOP, Direction::TOPRIGHT},
        {Direction::LEFT, Direction::CENTER, Direction::RIGHT},
        {Direction::BOTTOMLEFT, Direction::BOTTOM, Direction::BOTTOMRIGHT}};
    return table[vertical][horizontal];
}

// tts:writingMode: tbrl (and its shorthand tb) and tblr are vertical text.
int writingModeValue(const QString &v) {
    const QString m = v.trimmed().toLower();
    if (m == QLatin1String("tbrl") || m == QLatin1String("tb")) return 1;
    if (m == QLatin1String("tblr")) return 2;
    return 0;
}
}  // namespace

int W3CFamily::RegionInfo::vertical() const {
    // Vertical text: textAlign runs along the column (start is the top).
    const int a = writing ? textAlignValue(textAlign) : displayAlignValue(displayAlign);
    if (a >= 0) return a;
    // With an extent, judge by the region's centre; otherwise by its origin.
    const float y = hasExtent ? oy + eh / 2 : oy;
    return y < 33 ? 0 : (y < 67 ? 1 : 2);
}

int W3CFamily::RegionInfo::horizontal() const {
    // Vertical text: displayAlign stacks the columns from the right (tbrl) or the left (tblr).
    int a = writing ? displayAlignValue(displayAlign) : textAlignValue(textAlign);
    if (a >= 0 && writing == 1) a = 2 - a;
    if (a >= 0) return a;
    if (hasExtent) {
        const float cx = ox + ew / 2;
        return cx < 33 ? 0 : (cx > 67 ? 2 : 1);
    }
    if (ox <= 5) return 1;
    return ox < 33 ? 0 : (ox < 67 ? 1 : 2);
}

Direction W3CFamily::RegionInfo::direction() const {
    return directionOf(vertical(), horizontal());
}

// A region within the screen: its margins are the space left and right of it.
bool W3CFamily::RegionInfo::withinScreen() const {
    return hasExtent && ew > 0 && ox + ew <= 100.01f;
}

int W3CFamily::RegionInfo::leftMargin() const {
    const int h = horizontal();
    if (h == 0 || h == 1 || withinScreen()) return std::max(0, std::min(100, int(ox)));
    return 20;
}

int W3CFamily::RegionInfo::rightMargin() const {
    if (withinScreen())
        return std::max(0, std::min(100, jc::roundJava(100 - ox - ew)));
    if (horizontal() == 2) return std::max(0, std::min(100, int(100 - ox)));
    return 20;
}

// ---- loading ------------------------------------------------------------------

bool W3CFamily::isSubtitleCompatible(const QString &input) {
    return input.trimmed().startsWith(QLatin1String("<?xml")) || input.contains(QLatin1String("<tt"))
        || input.contains(QLatin1String("<dfxp")) || input.contains(QLatin1String("xmlns"));
}

namespace {
const QString XML_NS = QStringLiteral("http://www.w3.org/XML/1998/namespace");

QString idOf(const QDomElement &el) {
    if (el.hasAttributeNS(XML_NS, QStringLiteral("id")))
        return el.attributeNS(XML_NS, QStringLiteral("id"));
    if (el.hasAttribute(QStringLiteral("xml:id")))
        return el.attribute(QStringLiteral("xml:id"));
    return el.attribute(QStringLiteral("id"));
}

QString xmlSpaceOf(const QDomElement &el) {
    if (el.hasAttributeNS(XML_NS, QStringLiteral("space")))
        return el.attributeNS(XML_NS, QStringLiteral("space"));
    return el.attribute(QStringLiteral("xml:space"));
}

QString localNameOf(const QDomNode &n) {
    if (!n.localName().isEmpty()) return n.localName();
    const QString name = n.nodeName();
    const int colon = name.indexOf(QLatin1Char(':'));
    return colon >= 0 ? name.mid(colon + 1) : name;
}

// A node of a TTML vocabulary (any TTML/TTAF version: "#styling",
// "#parameter", "#metadata"), or with the conventional prefix when the
// namespace is not declared.
bool inVocabulary(const QDomNode &n, const QString &suffix, const QString &prefix) {
    if (n.namespaceURI().endsWith(suffix)) return true;
    return n.namespaceURI().isEmpty() && n.nodeName().startsWith(prefix + QLatin1Char(':'));
}

QString ttpParameter(const QDomElement &root, const QString &name) {
    const QDomNamedNodeMap attrs = root.attributes();
    for (int i = 0; i < attrs.count(); ++i) {
        const QDomNode a = attrs.item(i);
        if (localNameOf(a) == name && inVocabulary(a, QStringLiteral("#parameter"), QStringLiteral("ttp")))
            return a.nodeValue().trimmed();
    }
    return QString();
}

QList<QDomElement> elementsByLocalName(const QDomNode &root, const QString &name) {
    QList<QDomElement> out;
    for (QDomNode n = root.firstChild(); !n.isNull(); n = n.nextSibling()) {
        if (n.isElement()) {
            const QDomElement e = n.toElement();
            if (localNameOf(e) == name)
                out.append(e);
            out.append(elementsByLocalName(n, name));
        }
    }
    return out;
}

void mergeInto(QHash<QString, QString> &into, const QHash<QString, QString> &from) {
    for (auto it = from.begin(); it != from.end(); ++it)
        into.insert(it.key(), it.value());
}

// style="a b": a list of style ids.
QStringList styleRefs(const QDomElement &el) {
    return el.attribute(QStringLiteral("style")).simplified().split(QLatin1Char(' '), Qt::SkipEmptyParts);
}

// The nearest value of a (non-namespaced) attribute on the element or its ancestors.
QString inheritedAttribute(const QDomElement &el, const QString &name) {
    for (QDomNode n = el; n.isElement(); n = n.parentNode())
        if (n.toElement().hasAttribute(name))
            return n.toElement().attribute(name);
    return QString();
}

// Content elements from <body> down to `el` (inclusive).
QList<QDomElement> contentChain(const QDomElement &el) {
    QList<QDomElement> chain;
    for (QDomNode n = el; n.isElement() && localNameOf(n) != QLatin1String("tt"); n = n.parentNode())
        chain.prepend(n.toElement());
    return chain;
}
}  // namespace

W3CFamily::TtsMap W3CFamily::ownTts(const QDomElement &el) {
    TtsMap out;
    const QDomNamedNodeMap attrs = el.attributes();
    for (int i = 0; i < attrs.count(); ++i) {
        const QDomNode a = attrs.item(i);
        if (inVocabulary(a, QStringLiteral("#styling"), QStringLiteral("tts")))
            out.insert(localNameOf(a), a.nodeValue());
    }
    return out;
}

// A <style>: the styles it references (in order), then its own attributes.
W3CFamily::TtsMap W3CFamily::resolveStyleElement(const QDomElement &el, QSet<QString> &visiting) const {
    TtsMap out;
    for (const QString &ref : styleRefs(el))
        mergeInto(out, styleTts(ref, visiting));
    mergeInto(out, ownTts(el));
    return out;
}

W3CFamily::TtsMap W3CFamily::styleTts(const QString &id, QSet<QString> &visiting) const {
    if (!styleElements_.contains(id) || visiting.contains(id))
        return {};
    visiting.insert(id);
    const TtsMap out = resolveStyleElement(styleElements_.value(id), visiting);
    visiting.remove(id);
    return out;
}

// What an element specifies itself: its referenced styles, inline <style>
// children (regions), then its own tts attributes.
W3CFamily::TtsMap W3CFamily::specifiedTts(const QDomElement &el) const {
    TtsMap out;
    QSet<QString> visiting;
    for (const QString &ref : styleRefs(el))
        mergeInto(out, styleTts(ref, visiting));
    for (QDomNode n = el.firstChild(); !n.isNull(); n = n.nextSibling())
        if (n.isElement() && localNameOf(n) == QLatin1String("style") && idOf(n.toElement()).isEmpty())
            mergeInto(out, resolveStyleElement(n.toElement(), visiting));
    mergeInto(out, ownTts(el));
    return out;
}

std::unique_ptr<Subtitles> W3CFamily::parse(const QString &inputIn, float fps, const QString &file, bool debug) {
    Q_UNUSED(file);
    Q_UNUSED(fps);
    if (!isSubtitleCompatible(inputIn))
        return nullptr;
    if (debug)
        Debug::debug(QStringLiteral("Parsing XML subtitle file: ") + getExtendedName());
    // The text is already decoded: drop any XML declaration charset so the DOM
    // parser does not re-decode it.
    QString input = inputIn;
    static const QRegularExpression decl(QStringLiteral("^\\s*<\\?xml[^>]*\\?>"));
    input.remove(decl);
    QDomDocument doc;
    // Whitespace-only text is significant ("<span>a</span> <span>b</span>").
    QDomDocument::ParseResult res = doc.setContent(QAnyStringView(input), QDomDocument::ParseOptions(QDomDocument::ParseOption::UseNamespaceProcessing)
                                                              | QDomDocument::ParseOption::PreserveSpacingOnlyNodes);
    if (!res) {
        if (debug)
            Debug::debug(QStringLiteral("XML parse error: ") + res.errorMessage);
        return nullptr;
    }
    const QDomElement root = doc.documentElement();
    if (localNameOf(root) != QLatin1String("tt"))
        return nullptr;
    document_ = doc;
    timingCache_.clear();
    declaredFrameRate_ = false;
    // Without ttp:frameRate frames count at 30 fps (the TTML default), whatever the document FPS.
    effectiveFrameRate_ = 30.0;
    detectedDropMode_ = DropMode::NON_DROP;
    detectFrameRate(doc);
    auto subs = std::make_unique<Subtitles>();
    subtitleList_ = subs.get();
    regions_.clear();
    styleMap_.clear();
    styleElements_.clear();
    parseStyles(doc, *subs);
    parseRegions(doc);
    parseMetadata(root, *subs);
    // The document language, written back on save.
    const QString lang = (root.hasAttributeNS(XML_NS, QStringLiteral("lang")) ? root.attributeNS(XML_NS, QStringLiteral("lang"))
                                                                              : root.attribute(QStringLiteral("xml:lang"))).trimmed();
    if (!lang.isEmpty()) {
        QMap<QString, QString> data = subs->getFormatData();
        data.insert(QStringLiteral("ttml.lang"), lang);
        subs->setFormatData(data);
    }
    for (const QDomElement &p : elementsByLocalName(root, QStringLiteral("p")))
        for (const SubEntryPtr &e : parseSubtitleElement(p, *subs))
            subs->add(e);
    subtitleList_ = nullptr;
    document_ = QDomDocument();
    timingCache_.clear();
    styleElements_.clear();
    if (subs->isEmpty())
        return nullptr;
    return subs;
}

void W3CFamily::detectFrameRate(const QDomDocument &doc) {
    const QDomElement root = doc.documentElement();
    const QString dropMode = ttpParameter(root, QStringLiteral("dropMode"));
    if (!dropMode.isEmpty()) {
        if (dropMode.compare(QLatin1String("dropNTSC"), Qt::CaseInsensitive) == 0) detectedDropMode_ = DropMode::DROP_NTSC;
        else if (dropMode.compare(QLatin1String("dropPAL"), Qt::CaseInsensitive) == 0) detectedDropMode_ = DropMode::DROP_PAL;
        else detectedDropMode_ = DropMode::NON_DROP;
    }
    const QString frameRate = ttpParameter(root, QStringLiteral("frameRate"));
    if (!frameRate.isEmpty()) {
        bool ok = false;
        const double base = frameRate.toDouble(&ok);
        if (ok && base > 0) {
            declaredFrameRate_ = true;
            effectiveFrameRate_ = base;
            const QStringList mult = ttpParameter(root, QStringLiteral("frameRateMultiplier")).simplified().split(QLatin1Char(' '), Qt::SkipEmptyParts);
            if (mult.size() == 2) {
                const double num = mult[0].toDouble(), den = mult[1].toDouble();
                if (num > 0 && den > 0)
                    effectiveFrameRate_ = base * (num / den);
            }
        }
    } else if (getTimeBase() == TimeBase::SMPTE)
        Debug::debug(QStringLiteral("SMPTE timebase without ttp:frameRate, using %1 fps").arg(effectiveFrameRate_));

    // Ticks: ttp:tickRate, else frame rate × sub-frame rate when a frame rate is declared, else 1.
    tickRate_ = 1.0;
    bool ok = false;
    const double tick = ttpParameter(root, QStringLiteral("tickRate")).toDouble(&ok);
    if (ok && tick > 0)
        tickRate_ = tick;
    else if (declaredFrameRate_) {
        const double sub = ttpParameter(root, QStringLiteral("subFrameRate")).toDouble(&ok);
        tickRate_ = effectiveFrameRate_ * (ok && sub > 0 ? sub : 1.0);
    }
    cellColumns_ = 32;
    cellRows_ = 15;
    const QStringList cells = ttpParameter(root, QStringLiteral("cellResolution")).simplified().split(QLatin1Char(' '), Qt::SkipEmptyParts);
    if (cells.size() == 2) {
        bool ok1 = false, ok2 = false;
        const int c = cells[0].toInt(&ok1), r = cells[1].toInt(&ok2);
        if (ok1 && ok2 && c > 0 && r > 0) { cellColumns_ = c; cellRows_ = r; }
    }
    rootWidth_ = rootHeight_ = 0;
    const QStringList ext = ownTts(root).value(QStringLiteral("extent")).simplified().split(QLatin1Char(' '), Qt::SkipEmptyParts);
    if (ext.size() == 2 && ext[0].endsWith(QLatin1String("px")) && ext[1].endsWith(QLatin1String("px"))) {
        rootWidth_ = ext[0].chopped(2).toFloat();
        rootHeight_ = ext[1].chopped(2).toFloat();
    }
}

// An origin/extent coordinate in percent of the root: %, px (with a pixel
// tts:extent on <tt>) or c (cells of ttp:cellResolution).
bool W3CFamily::lengthToPercent(const QString &valueIn, bool horizontal, float &out) const {
    const QString value = valueIn.trimmed();
    bool ok = false;
    if (value.endsWith(QLatin1Char('%'))) {
        out = value.chopped(1).toFloat(&ok);
    } else if (value.endsWith(QLatin1String("px"))) {
        const float root = horizontal ? rootWidth_ : rootHeight_;
        const float v = value.chopped(2).toFloat(&ok);
        ok = ok && root > 0;
        if (ok) out = v * 100 / root;
    } else if (value.endsWith(QLatin1Char('c'))) {
        const float v = value.chopped(1).toFloat(&ok);
        if (ok) out = v * 100 / (horizontal ? cellColumns_ : cellRows_);
    }
    return ok;
}

W3CFamily::RegionInfo W3CFamily::regionFrom(const QString &id, const TtsMap &tts) const {
    RegionInfo r;
    r.id = id;
    const QStringList origin = tts.value(QStringLiteral("origin")).simplified().split(QLatin1Char(' '), Qt::SkipEmptyParts);
    float v = 0;
    if (origin.size() >= 1 && lengthToPercent(origin[0], true, v)) r.ox = v;
    if (origin.size() >= 2 && lengthToPercent(origin[1], false, v)) r.oy = v;
    const QStringList extent = tts.value(QStringLiteral("extent")).simplified().split(QLatin1Char(' '), Qt::SkipEmptyParts);
    float w = 0, h = 0;
    r.hasExtent = extent.size() >= 2 && lengthToPercent(extent[0], true, w) && lengthToPercent(extent[1], false, h);
    if (r.hasExtent) { r.ew = w; r.eh = h; }
    r.displayAlign = tts.value(QStringLiteral("displayAlign"));
    r.textAlign = tts.value(QStringLiteral("textAlign"));
    return r;
}

void W3CFamily::parseRegions(const QDomDocument &doc) {
    for (const QDomElement &el : elementsByLocalName(doc.documentElement(), QStringLiteral("region"))) {
        const QString id = idOf(el);
        if (!id.isEmpty())
            regions_.insert(id, specifiedTts(el));
    }
}

// ttm:title / ttm:desc / ttm:copyright of the head → title / comments / author.
void W3CFamily::parseMetadata(const QDomElement &root, Subtitles &subs) {
    QString title, desc, copyright;
    for (QDomNode head = root.firstChild(); !head.isNull(); head = head.nextSibling()) {
        if (!head.isElement() || localNameOf(head) != QLatin1String("head")) continue;
        for (QDomNode md = head.firstChild(); !md.isNull(); md = md.nextSibling()) {
            if (!md.isElement() || localNameOf(md) != QLatin1String("metadata")) continue;
            for (QDomNode n = md.firstChild(); !n.isNull(); n = n.nextSibling()) {
                if (!n.isElement() || !inVocabulary(n, QStringLiteral("#metadata"), QStringLiteral("ttm"))) continue;
                const QString name = localNameOf(n), text = n.toElement().text().simplified();
                if (name == QLatin1String("title") && title.isEmpty()) { title = text; }
                else if (name == QLatin1String("desc") && desc.isEmpty()) { desc = text; }
                else if (name == QLatin1String("copyright") && copyright.isEmpty()) { copyright = text; }
            }
        }
    }
    // The fixed titles Jubler writes are not document titles.
    static const QStringList generated = {QStringLiteral("W3C Timed Text Document"), QStringLiteral("DFXP Document"),
                                          QStringLiteral("TTML Document"), QStringLiteral("iTunes Timed Text")};
    if (generated.contains(title))
        title.clear();
    if (!(title.isEmpty() && desc.isEmpty() && copyright.isEmpty()))
        subs.setAttribs(SubAttribs(title, copyright.isEmpty() ? QString() : copyright, QString(), desc.isEmpty() ? QString() : desc));
}

namespace {
// The first family of a tts:fontFamily list that `allowed` accepts.
QString fontFamilyFrom(const QString &value, const std::function<bool(const QString &)> &allowed) {
    for (QString f : value.split(QLatin1Char(','))) {
        f = f.trimmed();
        if (f.size() >= 2 && (f.startsWith(QLatin1Char('"')) || f.startsWith(QLatin1Char('\''))) && f.endsWith(f.at(0)))
            f = f.mid(1, f.size() - 2);
        if (!f.isEmpty() && allowed(f))
            return f;
    }
    return QString();
}
}  // namespace

void W3CFamily::applyTts(const TtsMap &tts, SubStyle &style, bool restrictFonts, bool placement) const {
    if (tts.contains(QStringLiteral("fontFamily"))) {
        const QString family = fontFamilyFrom(tts.value(QStringLiteral("fontFamily")),
                                              [&](const QString &f) { return !restrictFonts || isFontAllowed(f); });
        if (!family.isEmpty())
            style.set(StyleType::FONTNAME, StyleValue(family));
    }
    if (const auto size = parseFontSize(tts.value(QStringLiteral("fontSize"))))
        style.set(StyleType::FONTSIZE, StyleValue(*size));
    QColor c;
    const bool hasColor = parseColor(tts.value(QStringLiteral("color")), c);
    if (hasColor)
        style.set(StyleType::PRIMARY, StyleValue(AlphaColor(c, c.alpha())));
    if (tts.contains(QStringLiteral("opacity"))) {
        bool ok = false;
        const double o = tts.value(QStringLiteral("opacity")).trimmed().toDouble(&ok);
        if (ok) {
            const AlphaColor p = style.color(StyleType::PRIMARY);
            const int alpha = hasColor ? c.alpha() : 255;
            style.set(StyleType::PRIMARY, StyleValue(AlphaColor(p.color(), jc::roundJava(alpha * std::clamp(o, 0.0, 1.0)))));
        }
    }
    const QString weight = tts.value(QStringLiteral("fontWeight")).trimmed();
    if (!weight.isEmpty())
        style.set(StyleType::BOLD, StyleValue(weight.compare(QLatin1String("bold"), Qt::CaseInsensitive) == 0));
    const QString fstyle = tts.value(QStringLiteral("fontStyle")).trimmed();
    if (!fstyle.isEmpty())
        style.set(StyleType::ITALIC, StyleValue(fstyle.compare(QLatin1String("italic"), Qt::CaseInsensitive) == 0
                                                || fstyle.compare(QLatin1String("oblique"), Qt::CaseInsensitive) == 0));
    for (const QString &t : tts.value(QStringLiteral("textDecoration")).simplified().split(QLatin1Char(' '), Qt::SkipEmptyParts)) {
        const QString d = t.toLower();
        if (d == QLatin1String("none")) {
            style.set(StyleType::UNDERLINE, StyleValue(false));
            style.set(StyleType::STRIKETHROUGH, StyleValue(false));
        } else if (d == QLatin1String("underline")) style.set(StyleType::UNDERLINE, StyleValue(true));
        else if (d == QLatin1String("nounderline")) style.set(StyleType::UNDERLINE, StyleValue(false));
        else if (d == QLatin1String("linethrough")) style.set(StyleType::STRIKETHROUGH, StyleValue(true));
        else if (d == QLatin1String("nolinethrough")) style.set(StyleType::STRIKETHROUGH, StyleValue(false));
    }
    // "[colour] thickness [blur]" → outline colour and border size.
    const QString outline = tts.value(QStringLiteral("textOutline")).simplified();
    if (outline.compare(QLatin1String("none"), Qt::CaseInsensitive) == 0) {
        style.set(StyleType::BORDERSIZE, StyleValue(0.0f));
    } else if (!outline.isEmpty()) {
        bool sized = false;
        for (const QString &part : outline.split(QLatin1Char(' '))) {
            QColor oc;
            if (parseColor(part, oc))
                style.set(StyleType::OUTLINE, StyleValue(AlphaColor(oc, oc.alpha())));
            else if (!sized)
                if (const auto w = parseLength(part)) { style.set(StyleType::BORDERSIZE, StyleValue(float(*w))); sized = true; }
        }
    }
    // A visible background: an opaque box in that colour; a transparent one: no box.
    QColor bg;
    if (parseColor(tts.value(QStringLiteral("backgroundColor")), bg)) {
        if (bg.alpha() > 0) {
            style.set(StyleType::BORDERSTYLE, StyleValue(1));
            style.set(StyleType::OUTLINE, StyleValue(AlphaColor(bg, bg.alpha())));
        } else
            style.set(StyleType::BORDERSTYLE, StyleValue(0));
    }
    if (placement) {
        const int v = displayAlignValue(tts.value(QStringLiteral("displayAlign")));
        const int h = textAlignValue(tts.value(QStringLiteral("textAlign")));
        if (v >= 0 || h >= 0)
            style.set(StyleType::DIRECTION, StyleValue(directionOf(v >= 0 ? v : 2, h >= 0 ? h : 1)));
    }
}

void W3CFamily::parseStyles(const QDomDocument &doc, Subtitles &subs) {
    SubStyleList &list = subs.getStyleList();
    QStringList order;
    for (const QDomElement &el : elementsByLocalName(doc.documentElement(), QStringLiteral("style"))) {
        const QString id = idOf(el);
        if (id.isEmpty())
            continue;
        if (!styleElements_.contains(id))
            order.append(id);
        styleElements_.insert(id, el);   // the last definition of an id wins
    }
    for (const QString &id : order) {
        SubStylePtr style;
        if (id.compare(QLatin1String("default"), Qt::CaseInsensitive) == 0 || list.findStyleIndex(id) != 0) {
            // The file's default style becomes the document's Default; a
            // second definition of an existing name updates it.
            style = id.compare(QLatin1String("default"), Qt::CaseInsensitive) == 0 ? list.get(0) : list.getStyleByName(id);
        } else {
            style = std::make_shared<SubStyle>(id);
            list.add(style);
        }
        QSet<QString> visiting;
        applyTts(styleTts(id, visiting), *style, true, true);
        styleMap_.insert(id, style);
    }
}

namespace {
bool hasTiming(const QDomElement &el) {
    return el.hasAttribute(QStringLiteral("begin")) || el.hasAttribute(QStringLiteral("end")) || el.hasAttribute(QStringLiteral("dur"));
}

// Not content: metadata and animation.
bool isContent(const QDomElement &el) {
    const QString tag = localNameOf(el);
    return !(tag == QLatin1String("metadata") || tag == QLatin1String("set") || tag == QLatin1String("animate")
             || inVocabulary(el, QStringLiteral("#metadata"), QStringLiteral("ttm")));
}

bool isSeq(const QDomNode &n) {
    return n.isElement() && n.toElement().attribute(QStringLiteral("timeContainer")).trimmed() == QLatin1String("seq");
}

// Text that is an anonymous span (not whitespace only).
bool isTextContent(const QDomNode &n) {
    return (n.isText() || n.isCDATASection()) && !n.nodeValue().trimmed().isEmpty();
}

// A child with its own time interval: timed, or in a sequence.
bool timedChild(const QDomElement &el) {
    return hasTiming(el) || isSeq(el.parentNode());
}
}  // namespace

std::optional<long long> W3CFamily::explicitEnd(const QDomElement &el, long long syncBase, long long begin) const {
    auto ms = [&](const QString &t) { return (long long)parseTime(t).getMillis(); };
    const QString end = el.attribute(QStringLiteral("end")).trimmed(), dur = el.attribute(QStringLiteral("dur")).trimmed();
    std::optional<long long> out;
    if (!end.isEmpty()) out = syncBase + ms(end);
    // With both, the shorter interval.
    if (!dur.isEmpty()) out = out ? std::min(*out, begin + ms(dur)) : begin + ms(dur);
    return out;
}

std::optional<long long> W3CFamily::implicitEnd(const QDomElement &el, long long begin, bool inSeq) const {
    auto ms = [&](const QString &t) { return (long long)parseTime(t).getMillis(); };
    QList<QDomElement> children;
    bool text = false;
    for (QDomNode n = el.firstChild(); !n.isNull(); n = n.nextSibling()) {
        if (n.isElement() && isContent(n.toElement())) children.append(n.toElement());
        else if (isTextContent(n)) text = true;
    }
    // A leaf (br, or span of text only): indefinite in a par, zero in a seq.
    if (children.isEmpty() && (text || localNameOf(el) == QLatin1String("br") || localNameOf(el) == QLatin1String("span")))
        return inSeq ? std::optional<long long>(begin) : std::nullopt;
    if (isSeq(el)) {
        // The end of the last child (anonymous spans last zero).
        long long cursor = begin;
        for (const QDomElement &ch : children) {
            const long long b = cursor + ms(ch.attribute(QStringLiteral("begin")).trimmed());
            const auto e = explicitEnd(ch, cursor, b);
            const auto end = e ? e : implicitEnd(ch, b, true);
            if (!end) return std::nullopt;
            cursor = *end;
        }
        return cursor;
    }
    // par: the end of all children (endsync "all"); text is indefinite.
    if (text) return std::nullopt;
    long long last = begin;
    for (const QDomElement &ch : children) {
        const long long b = begin + ms(ch.attribute(QStringLiteral("begin")).trimmed());
        const auto e = explicitEnd(ch, begin, b);
        const auto end = e ? e : implicitEnd(ch, b, false);
        if (!end) return std::nullopt;
        last = std::max(last, *end);
    }
    return last;
}

// begin/end/dur relative to the parent's begin (in a seq: to the previous
// sibling's end), clipped to the parent's end.
W3CFamily::Timing W3CFamily::timingOf(const QDomElement &el) const {
    // Sequences make each sibling depend on the previous ones: computed once.
    const QPair<int, int> key(el.lineNumber(), el.columnNumber());
    const bool cacheable = key.first >= 0 && key.second >= 0;
    if (cacheable)
        if (const auto it = timingCache_.constFind(key); it != timingCache_.constEnd())
            return *it;
    Timing t = computeTiming(el);
    if (cacheable)
        timingCache_.insert(key, t);
    return t;
}

W3CFamily::Timing W3CFamily::computeTiming(const QDomElement &el) const {
    Timing parent;
    const QDomNode up = el.parentNode();
    const bool inTree = up.isElement() && localNameOf(up) != QLatin1String("tt");
    if (inTree)
        parent = timingOf(up.toElement());
    Timing t;
    if (parent.never) { t.never = true; return t; }
    const bool inSeq = inTree && isSeq(up);
    long long syncBase = parent.begin;
    if (inSeq) {
        for (QDomNode n = el.previousSibling(); !n.isNull(); n = n.previousSibling()) {
            if (!n.isElement() || !isContent(n.toElement())) continue;
            const Timing prev = timingOf(n.toElement());
            if (prev.never || !prev.activeEnd) { t.never = true; return t; }
            syncBase = *prev.activeEnd;
            break;
        }
    }
    t.begin = syncBase + (long long)parseTime(el.attribute(QStringLiteral("begin")).trimmed()).getMillis();
    const auto e = explicitEnd(el, syncBase, t.begin);
    t.activeEnd = e ? e : implicitEnd(el, t.begin, inSeq);
    t.end = t.activeEnd;
    if (parent.end && (!t.end || *t.end > *parent.end))
        t.end = parent.end;
    if (parent.end && t.begin >= *parent.end)
        t.never = true;
    return t;
}

QList<SubEntryPtr> W3CFamily::parseSubtitleElement(const QDomElement &p, Subtitles &subs) {
    const Timing timing = timingOf(p);
    // Never active, unbounded, or of zero implicit duration (text in a sequence).
    if (timing.never || !timing.end || (!hasTiming(p) && timing.activeEnd == timing.begin))
        return {};
    const long long pBegin = timing.begin, pEnd = *timing.end;
    // Timed content inside the paragraph: every begin and end is a cut.
    QList<long long> cuts = {pBegin, pEnd};
    // A sequence inside shows its text only within its children's intervals.
    bool timed = isSeq(p);
    std::function<void(const QDomElement &)> collect = [&](const QDomElement &el) {
        for (QDomNode n = el.firstChild(); !n.isNull(); n = n.nextSibling()) {
            if (!n.isElement() || !isContent(n.toElement())) continue;
            const QDomElement ch = n.toElement();
            if (isSeq(ch))
                timed = true;
            if (timedChild(ch)) {
                timed = true;
                const Timing t = timingOf(ch);
                if (t.never) continue;
                cuts.append(std::clamp(t.begin, pBegin, pEnd));
                if (t.end) cuts.append(std::clamp(*t.end, pBegin, pEnd));
            }
            collect(ch);
        }
    };
    collect(p);
    if (!timed) {
        if (SubEntryPtr e = entryFor(p, subs, pBegin, pEnd, false))
            return {e};
        return {};
    }
    std::sort(cuts.begin(), cuts.end());
    cuts.erase(std::unique(cuts.begin(), cuts.end()), cuts.end());
    QList<SubEntryPtr> out;
    for (int i = 0; i + 1 < cuts.size(); ++i)
        if (SubEntryPtr e = entryFor(p, subs, cuts[i], cuts[i + 1], true); e && !e->getText().isEmpty())
            out.append(e);
    return out;
}

SubEntryPtr W3CFamily::entryFor(const QDomElement &p, Subtitles &subs, long long begin, long long end, bool windowed) {
    const Time start = Time::fromMillis(int(begin));
    const Time finish = Time::fromMillis(int(end));
    // In a window, a timed element shows only when active all through it.
    auto active = [&](const QDomElement &el) {
        if (!windowed || !timedChild(el)) return true;
        const Timing t = timingOf(el);
        return !t.never && t.begin <= begin && (!t.end || *t.end >= end);
    };

    // Text with nested span styles: each span range keeps the tts properties
    // of its spans (outer ones included) and of the enclosing spans.
    struct Range { int start, end; TtsMap tts, outer; };
    QString text;
    QList<Range> ranges;
    auto trimTrailingSpace = [&]() { while (text.endsWith(QLatin1Char(' '))) text.chop(1); };
    bool preserve = false;
    for (QDomNode n = p; n.isElement(); n = n.parentNode()) {
        const QString space = xmlSpaceOf(n.toElement());
        if (!space.isEmpty()) { preserve = space == QLatin1String("preserve"); break; }
    }
    std::function<void(const QDomNode &, const TtsMap &, bool)> walk = [&](const QDomNode &node, const TtsMap &spanTts, bool keepSpace) {
        if (node.isText() || node.isCDATASection()) {
            // Text directly in a sequence lasts zero time.
            if (windowed && isSeq(node.parentNode()))
                return;
            QString content = node.nodeValue();
            if (keepSpace) {
                content.replace(QLatin1String("\r\n"), QLatin1String("\n")).replace(QLatin1Char('\r'), QLatin1Char('\n'));
            } else {
                static const QRegularExpression ws(QStringLiteral("\\s+"));
                content.replace(ws, QStringLiteral(" "));
                // Collapsed across nodes too: "<span>a</span> <span> b</span>" is "a b".
                if (text.isEmpty() || text.endsWith(QLatin1Char('\n')) || text.endsWith(QLatin1Char(' ')))
                    if (content.startsWith(QLatin1Char(' ')))
                        content.remove(0, 1);
            }
            text += content;
        } else if (node.isElement()) {
            const QDomElement el = node.toElement();
            const QString tag = localNameOf(el);
            if (!isContent(el) || !active(el))
                return;
            const QString space = xmlSpaceOf(el);
            const bool childSpace = space.isEmpty() ? keepSpace : space == QLatin1String("preserve");
            if (tag == QLatin1String("br")) {
                if (!keepSpace) trimTrailingSpace();
                text += QLatin1Char('\n');
            } else if (tag == QLatin1String("span")) {
                TtsMap mine = spanTts;
                mergeInto(mine, specifiedTts(el));
                const int from = text.length();
                for (QDomNode ch = node.firstChild(); !ch.isNull(); ch = ch.nextSibling())
                    walk(ch, mine, childSpace);
                if (text.length() > from && !mine.isEmpty())
                    ranges.append(Range{from, int(text.length()), mine, spanTts});
            } else {
                for (QDomNode ch = node.firstChild(); !ch.isNull(); ch = ch.nextSibling())
                    walk(ch, spanTts, childSpace);
            }
        }
    };
    for (QDomNode ch = p.firstChild(); !ch.isNull(); ch = ch.nextSibling())
        walk(ch, TtsMap(), preserve);
    trimTrailingSpace();

    auto entry = std::make_shared<SubEntry>(start, finish, text);
    // The entry style: the first known style referenced by the paragraph or,
    // failing that, by its nearest styled container.
    SubStylePtr style;
    const QList<QDomElement> chain = contentChain(p);
    for (int i = int(chain.size()) - 1; i >= 0 && !style; --i)
        for (const QString &ref : styleRefs(chain[i]))
            if (styleMap_.contains(ref)) { style = styleMap_.value(ref); break; }
    if (!style)
        style = subs.getStyleList().get(0);
    entry->setStyle(style);

    // The paragraph's properties: its region's, then body → div → p; those
    // that differ from the entry style become whole-entry overrides. A
    // region's background fills the region, not the text: not a text box.
    const QString regionRef = inheritedAttribute(p, QStringLiteral("region"));
    TtsMap pTts = regions_.value(regionRef);
    pTts.remove(QStringLiteral("backgroundColor"));
    for (const QDomElement &el : chain)
        mergeInto(pTts, specifiedTts(el));
    static const StyleType::Id inlineTypes[] = {StyleType::BOLD, StyleType::ITALIC, StyleType::UNDERLINE, StyleType::STRIKETHROUGH,
                                                StyleType::PRIMARY, StyleType::FONTSIZE, StyleType::FONTNAME, StyleType::OUTLINE};
    // Box and outline thickness exist only for the whole entry.
    static const StyleType::Id entryTypes[] = {StyleType::BORDERSTYLE, StyleType::BORDERSIZE};
    const int len = text.length();
    SubStyle effective(*style);
    applyTts(pTts, effective, true, false);
    for (StyleType::Id t : inlineTypes)
        if (!styleValueEquals(effective.get(t), style->get(t)))
            entry->setOverStyle(t, effective.get(t), 0, len);
    for (StyleType::Id t : entryTypes)
        if (!styleValueEquals(effective.get(t), style->get(t)))
            entry->setOverStyle(t, effective.get(t), 0, len);

    // Inline ranges → overrides. Outer ranges first so inner ones win.
    std::sort(ranges.begin(), ranges.end(), [](const Range &a, const Range &b) {
        return a.start != b.start ? a.start < b.start : a.end > b.end;
    });
    const bool boxed = effective.integral(StyleType::BORDERSTYLE) == 1;
    for (Range r : ranges) {
        const bool whole = r.start == 0 && r.end >= len;
        if (!whole) {
            // Part of the text: the outline colour is the box colour inside a
            // box and the outline's outside one, so only the matching one applies.
            const QString drop = boxed ? QStringLiteral("textOutline") : QStringLiteral("backgroundColor");
            r.tts.remove(drop);
            r.outer.remove(drop);
        }
        SubStyle mine(effective), outer(effective);
        applyTts(r.tts, mine, false, false);
        applyTts(r.outer, outer, false, false);
        for (StyleType::Id t : inlineTypes)
            if (!styleValueEquals(mine.get(t), effective.get(t)) || !styleValueEquals(mine.get(t), outer.get(t)))
                entry->setOverStyle(t, mine.get(t), r.start, r.end);
        if (whole)
            for (StyleType::Id t : entryTypes)
                if (!styleValueEquals(mine.get(t), effective.get(t)))
                    entry->setOverStyle(t, mine.get(t), 0, len);
    }

    // Vertical text (tts:writingMode of the region) → a whole-entry rotation,
    // as WebVTT's "vertical:".
    const int writing = writingModeValue(pTts.value(QStringLiteral("writingMode")));
    if (writing) {
        const float angle = writing == 1 ? 90.0f : -90.0f;
        if (angle != style->real(StyleType::ANGLE))
            entry->setOverStyle(StyleType::ANGLE, StyleValue(angle), 0, len);
    }

    // Region → placement (as whole-entry overrides where they differ); the
    // paragraph's own textAlign wins over its region's.
    if (regions_.contains(regionRef)) {
        RegionInfo r = regionFrom(regionRef, regions_.value(regionRef));
        r.writing = writing;
        if (pTts.contains(QStringLiteral("textAlign")))
            r.textAlign = pTts.value(QStringLiteral("textAlign"));
        if (r.direction() != style->direction())
            entry->setOverStyle(StyleType::DIRECTION, StyleValue(r.direction()), 0, len);
        if (r.leftMargin() != style->integral(StyleType::LEFTMARGIN))
            entry->setOverStyle(StyleType::LEFTMARGIN, StyleValue(r.leftMargin()), 0, len);
        if (r.rightMargin() != style->integral(StyleType::RIGHTMARGIN))
            entry->setOverStyle(StyleType::RIGHTMARGIN, StyleValue(r.rightMargin()), 0, len);
    } else {
        SubStyle placed(*style);
        applyTts(pTts, placed, true, true);
        if (placed.direction() != style->direction())
            entry->setOverStyle(StyleType::DIRECTION, StyleValue(placed.direction()), 0, len);
    }
    entry->cleanupEvents();
    return entry;
}

Time W3CFamily::parseTime(const QString &textIn) const {
    const QString text = textIn.trimmed();
    if (text.isEmpty())
        return Time(0.0);
    // Offset times: "10s", "1.5s", "1500ms", "2m", "1h", "30f", "900t".
    static const QRegularExpression offset(QStringLiteral("^(\\d+(?:\\.\\d+)?)(h|m|s|ms|f|t)$"));
    const QRegularExpressionMatch om = offset.match(text);
    if (om.hasMatch()) {
        const double v = om.captured(1).toDouble();
        const QString unit = om.captured(2);
        if (unit == QLatin1String("h")) return Time(v * 3600);
        if (unit == QLatin1String("m")) return Time(v * 60);
        if (unit == QLatin1String("s")) return Time(v);
        if (unit == QLatin1String("ms")) return Time(v / 1000);
        if (unit == QLatin1String("f")) return Time(v / (effectiveFrameRate_ > 0 ? effectiveFrameRate_ : 30));
        return Time(v / (tickRate_ > 0 ? tickRate_ : 1));
    }
    if (getTimeBase() == TimeBase::SMPTE) {
        QString norm = text;
        norm.replace(QLatin1Char(';'), QLatin1Char(':'));
        if (norm.split(QLatin1Char(':')).size() == 4)
            return parseSMPTETime(text);
    } else if (text.count(QLatin1Char(':')) == 3)
        return parseSMPTETime(text);   // frame-based time in a media-time document
    return parseMediaTime(text);
}

Time W3CFamily::parseSMPTETime(const QString &text) const {
    // Drop frames: a ';' separator or the document's ttp:dropMode.
    const bool semicolon = text.contains(QLatin1Char(';'));
    const bool dropNTSC = semicolon || detectedDropMode_ == DropMode::DROP_NTSC;
    const bool dropPAL = !semicolon && detectedDropMode_ == DropMode::DROP_PAL;
    QString norm = text;
    norm.replace(QLatin1Char(';'), QLatin1Char(':'));
    const QStringList parts = norm.split(QLatin1Char(':'));
    if (parts.size() != 4)
        return Time(0.0);
    bool ok[4];
    const int h = parts[0].toInt(&ok[0]), m = parts[1].toInt(&ok[1]), s = parts[2].toInt(&ok[2]);
    // A sub-frame part ("ff.sf") is ignored.
    const int f = parts[3].section(QLatin1Char('.'), 0, 0).toInt(&ok[3]);
    if (!(ok[0] && ok[1] && ok[2] && ok[3]))
        return Time(0.0);
    const double fps = effectiveFrameRate_ > 0 ? effectiveFrameRate_ : 30.0;
    if (dropNTSC || dropPAL) {
        // NTSC: two frame codes skipped every minute except each tenth;
        // PAL: four every even minute except each twentieth.
        const int nominal = dropNTSC ? 30 : int(std::lround(fps));
        const int totalMinutes = h * 60 + m;
        const int dropped = totalMinutes <= 0 ? 0
            : dropNTSC ? 2 * (totalMinutes - totalMinutes / 10) : 4 * (totalMinutes / 2 - totalMinutes / 20);
        const long long actual = (long long)(totalMinutes) * 60 * nominal + (long long)s * nominal + f - dropped;
        return Time::fromMillis(jc::roundJava(actual * 1000.0 / fps));
    }
    const long long totalFrames = ((long long)h * 3600 + m * 60 + s) * (long long)std::lround(fps) + f;
    return Time::fromMillis(jc::roundJava(totalFrames * 1000.0 / fps));
}

Time W3CFamily::parseMediaTime(const QString &text) {
    const QStringList parts = text.split(QLatin1Char(':'));
    if (parts.size() != 3)
        return Time(0.0);
    bool ok[3];
    const int h = parts[0].toInt(&ok[0]);
    const int m = parts[1].toInt(&ok[1]);
    const QStringList sec = parts[2].split(QLatin1Char('.'));
    const int s = sec[0].toInt(&ok[2]);
    if (!(ok[0] && ok[1] && ok[2]))
        return Time(0.0);
    int millis = 0;
    if (sec.size() > 1) {
        QString frac = sec[1].left(3);
        while (frac.length() < 3) frac += QLatin1Char('0');
        millis = frac.toInt();
    }
    return Time::fromMillis((h * 3600 + m * 60 + s) * 1000 + millis);
}

// #rrggbb, #rrggbbaa, rgb(r,g,b), rgba(r,g,b,a) and the CSS colour names,
// case-insensitive; the alpha is kept in the colour.
bool W3CFamily::parseColor(const QString &textIn, QColor &out) {
    const QString text = textIn.trimmed().toLower();
    if (text.isEmpty())
        return false;
    if (text.startsWith(QLatin1Char('#'))) {
        if (text.length() == 7) { out = QColor(text); return out.isValid(); }
        if (text.length() == 9) {
            out = QColor(text.left(7));
            bool ok = false;
            const int a = text.mid(7).toInt(&ok, 16);
            if (!ok || !out.isValid()) return false;
            out.setAlpha(a);
            return true;
        }
        return false;
    }
    if (text.startsWith(QLatin1String("rgb"))) {
        static const QRegularExpression rgb(QStringLiteral("^rgba?\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*(?:,\\s*(\\d+)\\s*)?\\)$"));
        const QRegularExpressionMatch m = rgb.match(text);
        if (!m.hasMatch()) return false;
        auto channel = [&](int i) { return std::min(255, m.captured(i).toInt()); };
        out = QColor(channel(1), channel(2), channel(3), m.captured(4).isEmpty() ? 255 : channel(4));
        return out.isValid();
    }
    if (text == QLatin1String("transparent")) { out = QColor(0, 0, 0, 0); return true; }
    if (!QColor::isValidColorName(text))
        return false;
    out = QColor::fromString(text);
    return out.isValid();
}

// A length in pixels: px (or a bare number), pt, % and em of the 16 px base
// size, c of the cell height.
std::optional<double> W3CFamily::parseLength(const QString &textIn) const {
    const QString text = textIn.trimmed();
    if (text.isEmpty()) return std::nullopt;
    bool ok = false;
    double v = 0;
    if (text.endsWith(QLatin1Char('%'))) {
        v = text.chopped(1).toDouble(&ok) * 16 / 100;
    } else if (text.endsWith(QLatin1String("px"))) {
        v = text.chopped(2).toDouble(&ok);
    } else if (text.endsWith(QLatin1String("pt"))) {
        v = text.chopped(2).toDouble(&ok) * 4 / 3;
    } else if (text.endsWith(QLatin1String("em"))) {
        v = text.chopped(2).toDouble(&ok) * 16;
    } else if (text.endsWith(QLatin1Char('c'))) {
        // Without a pixel root, 1c of the default 15-row grid is the 16 px base.
        const double cell = rootHeight_ > 0 ? double(rootHeight_) / cellRows_ : 240.0 / cellRows_;
        v = text.chopped(1).toDouble(&ok) * cell;
    } else {
        v = text.toDouble(&ok);
    }
    if (!ok) return std::nullopt;
    return v;
}

std::optional<int> W3CFamily::parseFontSize(const QString &textIn) const {
    // "w h": the second value is the height.
    const QStringList parts = textIn.simplified().split(QLatin1Char(' '), Qt::SkipEmptyParts);
    if (parts.isEmpty()) return std::nullopt;
    const auto v = parseLength(parts.last());
    if (!v) return std::nullopt;
    return jc::roundJava(*v);
}

// ---- saving -------------------------------------------------------------------

namespace {
QString xmlEscape(const QString &t) {
    QString r = t;
    r.replace(QLatin1Char('&'), QLatin1String("&amp;")).replace(QLatin1Char('<'), QLatin1String("&lt;"))
        .replace(QLatin1Char('>'), QLatin1String("&gt;")).replace(QLatin1Char('"'), QLatin1String("&quot;"));
    return r;
}
}  // namespace

// Minimal indenting serializer: element content given as raw XML stays on
// one line (mixed content must not gain whitespace).
class W3CFamily::XmlOut {
public:
    void open(const QString &name, const Attrs &attrs = {}) { line(tag(name, attrs, false)); ++depth_; }
    void close(const QString &name) { --depth_; line(QLatin1String("</") + name + QLatin1Char('>')); }
    void empty(const QString &name, const Attrs &attrs = {}) { line(tag(name, attrs, true)); }
    void leaf(const QString &name, const Attrs &attrs, const QString &rawContent) {
        line(tag(name, attrs, false) + rawContent + QLatin1String("</") + name + QLatin1Char('>'));
    }
    QString result() const { return buf_; }

private:
    QString tag(const QString &name, const Attrs &attrs, bool empty) const {
        QString t = QLatin1Char('<') + name;
        for (const auto &a : attrs)
            t += QLatin1Char(' ') + a.first + QLatin1String("=\"") + xmlEscape(a.second) + QLatin1Char('"');
        return t + (empty ? QLatin1String("/>") : QLatin1String(">"));
    }
    void line(const QString &t) { buf_ += QString(depth_ * 2, QLatin1Char(' ')) + t + QLatin1Char('\n'); }
    QString buf_;
    int depth_ = 0;
};

QString W3CFamily::formatTime(const Time &t) const {
    if (getTimeBase() == TimeBase::SMPTE) {
        // Frames at the effective rate, labelled at the nominal (integer) rate.
        const double fps = effectiveFrameRate_ > 0 ? effectiveFrameRate_ : 30.0;
        const long long nominal = std::max(1L, std::lround(fps));
        const long long total = jc::roundJavaL(t.getMillis() * fps / 1000.0);
        const long long frames = total % nominal, secs = total / nominal;
        const long long hours = secs / 3600, minutes = (secs % 3600) / 60, seconds = secs % 60;
        return QStringLiteral("%1:%2:%3:%4").arg(hours, 2, 10, QLatin1Char('0')).arg(minutes, 2, 10, QLatin1Char('0'))
            .arg(seconds, 2, 10, QLatin1Char('0')).arg(frames, 2, 10, QLatin1Char('0'));
    }
    return t.getSeconds(QLatin1Char('.'));
}

QString W3CFamily::formatTTMLColor(const AlphaColor &c) {
    const QString rgb = QStringLiteral("#%1").arg(c.rgb(), 6, 16, QLatin1Char('0'));
    return c.alpha() == 255 ? rgb : rgb + QStringLiteral("%1").arg(c.alpha(), 2, 16, QLatin1Char('0'));
}

QString W3CFamily::regionIdForDirection(Direction d) {
    switch (d) {
        case Direction::TOP: return QStringLiteral("top");
        case Direction::TOPLEFT: return QStringLiteral("topleft");
        case Direction::TOPRIGHT: return QStringLiteral("topright");
        case Direction::CENTER: return QStringLiteral("center");
        case Direction::LEFT: return QStringLiteral("left");
        case Direction::RIGHT: return QStringLiteral("right");
        case Direction::BOTTOMLEFT: return QStringLiteral("bottomleft");
        case Direction::BOTTOMRIGHT: return QStringLiteral("bottomright");
        default: return QStringLiteral("bottom");
    }
}

Direction W3CFamily::effectiveDirection(const SubEntry &e) {
    const Direction base = e.getStyle() ? e.getStyle()->direction() : Direction::BOTTOM;
    if (const Styleover *over = e.getStyleover(StyleType::DIRECTION); over && over->size()) {
        Styleover copy(*over);
        const auto v = copy.getValue(0, e.getText().length(), StyleValue(base), e.getText());
        if (v && std::holds_alternative<Direction>(*v))
            return std::get<Direction>(*v);
    }
    return base;
}

QString W3CFamily::regionIdFor(Direction d, int writing) {
    const QString id = regionIdForDirection(d);
    return writing == 1 ? id + QLatin1String("_tbrl") : writing == 2 ? id + QLatin1String("_tblr") : id;
}

int W3CFamily::writingModeOf(const SubEntry &e) {
    const StyleValue v = valueAt(e, StyleType::ANGLE, 0);
    if (!std::holds_alternative<float>(v)) return 0;
    const float angle = std::get<float>(v);
    return angle == 90.0f ? 1 : angle == -90.0f ? 2 : 0;
}

StyleValue W3CFamily::valueAt(const SubEntry &sub, StyleType::Id type, int pos) {
    const StyleValue basic = sub.getStyle() ? sub.getStyle()->get(type) : StyleType::defaultValue(type);
    if (const Styleover *over = sub.getStyleover(type); over && over->size()) {
        Styleover copy(*over);
        const auto v = copy.getValue(pos, pos + 1, basic, sub.getText());
        if (v) return *v;
    }
    return basic;
}

QString W3CFamily::renderDocument(const Subtitles &subs, const MediaFile *) {
    if (getTimeBase() == TimeBase::SMPTE)
        effectiveFrameRate_ = FPS_ > 0 ? FPS_ : 30.0;
    XmlOut w;
    // The loaded document's language, else English in the format's form.
    QString lang = subs.getFormatData().value(QStringLiteral("ttml.lang"));
    if (lang.isEmpty())
        lang = getLanguageFormat() == LanguageFormat::FULL ? QStringLiteral("en-US") : QStringLiteral("en");
    Attrs root = {{QStringLiteral("xml:lang"), lang},
                  {QStringLiteral("xmlns"), TTML_NS}, {QStringLiteral("xmlns:tts"), TTML_STYLING_NS},
                  {QStringLiteral("xmlns:ttp"), TTML_PARAMETER_NS}, {QStringLiteral("xmlns:ttm"), TTML_METADATA_NS}};
    const QMap<QString, QString> extra = getAdditionalNamespaces();
    for (auto it = extra.begin(); it != extra.end(); ++it)
        root.append({QStringLiteral("xmlns:") + it.key(), it.value()});
    root.append({QStringLiteral("ttp:timeBase"), timeBaseValue(getTimeBase())});
    if (getTimeBase() == TimeBase::SMPTE) {
        root.append({QStringLiteral("ttp:dropMode"), dropModeValue(getDropMode())});
        // Integer frame rates are written directly; NTSC ones (23.976,
        // 29.97, n·1000/1001) as n + "1000 1001"; any other fractional rate as
        // the nominal (rounded) rate and the exact multiplier.
        const double fps = effectiveFrameRate_;
        const long long rounded = std::max(1L, std::lround(fps));
        const double ntsc = fps * 1001.0 / 1000.0;
        if (std::abs(fps - rounded) < 0.001)
            root.append({QStringLiteral("ttp:frameRate"), QString::number(rounded)});
        else if (std::abs(ntsc - std::lround(ntsc)) < 0.001) {
            root.append({QStringLiteral("ttp:frameRate"), QString::number(std::lround(ntsc))});
            root.append({QStringLiteral("ttp:frameRateMultiplier"), QStringLiteral("1000 1001")});
        } else {
            long long num = jc::roundJavaL(fps * 1000), den = rounded * 1000;
            const long long g = std::gcd(num, den);
            num /= g;
            den /= g;
            root.append({QStringLiteral("ttp:frameRate"), QString::number(rounded)});
            root.append({QStringLiteral("ttp:frameRateMultiplier"), QStringLiteral("%1 %2").arg(num).arg(den)});
        }
    }
    root.append({QStringLiteral("ttp:profile"), profileValue(getTTMLProfile())});
    w.open(QStringLiteral("tt"), root);
    w.open(QStringLiteral("head"));
    w.open(QStringLiteral("metadata"));
    const QString title = subs.getAttribs().title.trimmed();
    w.leaf(QStringLiteral("ttm:title"), {}, xmlEscape(title.isEmpty() ? getDocumentTitle() : title));
    w.close(QStringLiteral("metadata"));
    writeStyling(w, subs);
    writeLayout(w, subs);
    w.close(QStringLiteral("head"));
    w.open(QStringLiteral("body"));
    w.open(QStringLiteral("div"));
    for (int i = 0; i < subs.size(); ++i)
        writeEntry(w, *subs.elementAt(i));
    w.close(QStringLiteral("div"));
    w.close(QStringLiteral("body"));
    w.close(QStringLiteral("tt"));
    return w.result();
}

void W3CFamily::writeStyling(XmlOut &w, const Subtitles &subs) {
    w.open(QStringLiteral("styling"));
    Attrs def = {{QStringLiteral("xml:id"), QStringLiteral("default")}, {QStringLiteral("tts:fontFamily"), getDefaultFontFamily()},
                 {QStringLiteral("tts:fontSize"), getDefaultFontSize()}, {QStringLiteral("tts:color"), getDefaultTextColor()}};
    addCustomStyleAttributes(def);
    w.empty(QStringLiteral("style"), def);
    // One <style> per distinct entry style name, in first-use order.
    QSet<QString> seen;
    for (int i = 0; i < subs.size(); ++i) {
        const SubStylePtr style = subs.elementAt(i)->getStyle();
        if (!style || style->getName() == QLatin1String("default") || seen.contains(style->getName()))
            continue;
        seen.insert(style->getName());
        Attrs a = {{QStringLiteral("xml:id"), style->getName()}};
        if (isFontAllowed(style->fontName()))
            a.append({QStringLiteral("tts:fontFamily"), style->fontName()});
        a.append({QStringLiteral("tts:fontSize"), QString::number(style->fontSize()) + QLatin1String("px")});
        a.append({QStringLiteral("tts:color"), formatTTMLColor(style->color(StyleType::PRIMARY))});
        if (style->flag(StyleType::BOLD)) a.append({QStringLiteral("tts:fontWeight"), QStringLiteral("bold")});
        if (style->flag(StyleType::ITALIC)) a.append({QStringLiteral("tts:fontStyle"), QStringLiteral("italic")});
        QStringList decoration;
        if (style->flag(StyleType::UNDERLINE)) decoration.append(QStringLiteral("underline"));
        if (style->flag(StyleType::STRIKETHROUGH)) decoration.append(QStringLiteral("lineThrough"));
        if (!decoration.isEmpty()) a.append({QStringLiteral("tts:textDecoration"), decoration.join(QLatin1Char(' '))});
        boxOutlineAttrs(a, style->integral(StyleType::BORDERSTYLE), style->real(StyleType::BORDERSIZE), style->color(StyleType::OUTLINE),
                        std::get<int>(StyleType::defaultValue(StyleType::BORDERSTYLE)), std::get<float>(StyleType::defaultValue(StyleType::BORDERSIZE)),
                        std::get<AlphaColor>(StyleType::defaultValue(StyleType::OUTLINE)));
        w.empty(QStringLiteral("style"), a);
    }
    w.close(QStringLiteral("styling"));
}

void W3CFamily::writeLayout(XmlOut &w, const Subtitles &subs) {
    // A region for every non-bottom placement and every vertical text in use
    // (from styles or overrides).
    QList<QPair<Direction, int>> used;
    for (int i = 0; i < subs.size(); ++i) {
        const QPair<Direction, int> key(effectiveDirection(*subs.elementAt(i)), writingModeOf(*subs.elementAt(i)));
        if ((key.first != Direction::BOTTOM || key.second) && !used.contains(key))
            used.append(key);
    }
    if (used.isEmpty()) {
        w.empty(QStringLiteral("layout"));
        return;
    }
    w.open(QStringLiteral("layout"));
    for (const auto &key : used)
        writeRegion(w, key.first, key.second, subs);
    w.close(QStringLiteral("layout"));
}

void W3CFamily::writeRegion(XmlOut &w, Direction d, int writing, const Subtitles &subs) {
    // Margins: from the first entry placed in this direction (default 20).
    int leftMargin = 20, rightMargin = 20;
    for (int i = 0; i < subs.size(); ++i) {
        const SubEntry &e = *subs.elementAt(i);
        if (effectiveDirection(e) == d && writingModeOf(e) == writing) {
            leftMargin = std::get<int>(valueAt(e, StyleType::LEFTMARGIN, 0));
            rightMargin = std::get<int>(valueAt(e, StyleType::RIGHTMARGIN, 0));
            break;
        }
    }
    // The region spans the screen between the margins; textAlign places the
    // text inside it.
    const int left = std::max(0, std::min(100, leftMargin));
    const int right = std::max(0, std::min(100 - left, rightMargin));
    const QString originX = QString::number(left) + QLatin1Char('%');
    const QString width = QString::number(100 - left - right) + QLatin1Char('%');
    QString originY, displayAlign, textAlign;
    switch (d) {
        case Direction::TOPLEFT: case Direction::LEFT: case Direction::BOTTOMLEFT: textAlign = QStringLiteral("left"); break;
        case Direction::TOPRIGHT: case Direction::RIGHT: case Direction::BOTTOMRIGHT: textAlign = QStringLiteral("right"); break;
        default: textAlign = QStringLiteral("center");
    }
    switch (d) {
        case Direction::TOP: case Direction::TOPLEFT: case Direction::TOPRIGHT: originY = QStringLiteral("15%"); displayAlign = QStringLiteral("before"); break;
        case Direction::CENTER: case Direction::LEFT: case Direction::RIGHT: originY = QStringLiteral("50%"); displayAlign = QStringLiteral("center"); break;
        default: originY = QStringLiteral("85%"); displayAlign = QStringLiteral("after");
    }
    QString height = QStringLiteral("15%");
    if (writing) {
        // Vertical text: a column area between the margins, from 10% to 90%
        // of the height; displayAlign places the column, textAlign the text in it.
        const int h = textAlign == QLatin1String("left") ? 0 : textAlign == QLatin1String("right") ? 2 : 1;
        const int v = displayAlign == QLatin1String("before") ? 0 : displayAlign == QLatin1String("after") ? 2 : 1;
        const int stack = writing == 1 ? 2 - h : h;
        displayAlign = stack == 0 ? QStringLiteral("before") : stack == 2 ? QStringLiteral("after") : QStringLiteral("center");
        textAlign = v == 0 ? QStringLiteral("start") : v == 2 ? QStringLiteral("end") : QStringLiteral("center");
        originY = QStringLiteral("10%");
        height = QStringLiteral("80%");
    }
    Attrs a = {{QStringLiteral("xml:id"), regionIdFor(d, writing)}, {QStringLiteral("tts:origin"), originX + QLatin1Char(' ') + originY},
               {QStringLiteral("tts:extent"), width + QLatin1Char(' ') + height}, {QStringLiteral("tts:displayAlign"), displayAlign},
               {QStringLiteral("tts:textAlign"), textAlign}};
    addCustomRegionAttributes(a, d);
    if (writing) {
        const QString mode = writing == 1 ? QStringLiteral("tbrl") : QStringLiteral("tblr");
        auto it = std::find_if(a.begin(), a.end(), [](const QPair<QString, QString> &p) { return p.first == QLatin1String("tts:writingMode"); });
        if (it != a.end()) it->second = mode;
        else a.append({QStringLiteral("tts:writingMode"), mode});
    }
    w.empty(QStringLiteral("region"), a);
}

void W3CFamily::writeEntry(XmlOut &w, const SubEntry &sub) {
    Attrs a = {{QStringLiteral("begin"), formatTime(sub.getStartTime())}, {QStringLiteral("end"), formatTime(sub.getFinishTime())},
               {QStringLiteral("style"), sub.getStyle() ? sub.getStyle()->getName() : QStringLiteral("default")}};
    const Direction d = effectiveDirection(sub);
    const int writing = writingModeOf(sub);
    if (d != Direction::BOTTOM || writing)
        a.append({QStringLiteral("region"), regionIdFor(d, writing)});
    // The entry's box and outline (at its start; spans carry other outline colours).
    const SubStylePtr style = sub.getStyle();
    boxOutlineAttrs(a, std::get<int>(valueAt(sub, StyleType::BORDERSTYLE, 0)), std::get<float>(valueAt(sub, StyleType::BORDERSIZE, 0)),
                    std::get<AlphaColor>(valueAt(sub, StyleType::OUTLINE, 0)),
                    std::get<int>(style ? style->get(StyleType::BORDERSTYLE) : StyleType::defaultValue(StyleType::BORDERSTYLE)),
                    std::get<float>(style ? style->get(StyleType::BORDERSIZE) : StyleType::defaultValue(StyleType::BORDERSIZE)),
                    std::get<AlphaColor>(style ? style->get(StyleType::OUTLINE) : StyleType::defaultValue(StyleType::OUTLINE)));
    w.leaf(QStringLiteral("p"), a, entryTextXml(sub));
}

void W3CFamily::boxOutlineAttrs(Attrs &a, int box, float size, const AlphaColor &col, int baseBox, float baseSize, const AlphaColor &baseCol) {
    const QString outline = formatTTMLColor(col) + QLatin1Char(' ') + QString::number(size) + QLatin1String("px");
    if (box == 1) {
        // The outline colour is the box colour.
        if (baseBox != 1 || col != baseCol) a.append({QStringLiteral("tts:backgroundColor"), formatTTMLColor(col)});
        if (size != baseSize) a.append({QStringLiteral("tts:textOutline"), outline});
    } else {
        if (baseBox == 1) a.append({QStringLiteral("tts:backgroundColor"), QStringLiteral("transparent")});
        if (size != baseSize || (size > 0 && col != baseCol))
            a.append({QStringLiteral("tts:textOutline"), size > 0 ? outline : QStringLiteral("none")});
    }
}

QString W3CFamily::textWithBreaksXml(const QString &text) {
    QStringList lines = text.split(QLatin1Char('\n'));
    for (QString &l : lines)
        l = xmlEscape(l);
    return lines.join(QLatin1String("<br/>"));
}

QString W3CFamily::entryTextXml(const SubEntry &subIn) {
    // HTML-style tags typed into the text become inline runs too.
    SubEntry sub(subIn);
    if (!sub.hasStyleovers() && sub.getText().contains(QLatin1Char('<')) && sub.getText().contains(QLatin1Char('>')))
        SimpleStyledTextSubFormat::htmlTagsToOverrides(sub);
    const QString text = sub.getText();
    if (!supportsInlineFormatting() || !sub.hasStyleovers() || text.isEmpty())
        return textWithBreaksXml(text);
    static const StyleType::Id inline_[] = {StyleType::BOLD, StyleType::ITALIC, StyleType::UNDERLINE, StyleType::PRIMARY, StyleType::FONTSIZE, StyleType::FONTNAME,
                                            StyleType::STRIKETHROUGH, StyleType::OUTLINE};
    // The paragraph's box, outline thickness and outline colour (written on the <p>).
    const bool boxed = std::get<int>(valueAt(sub, StyleType::BORDERSTYLE, 0)) == 1;
    const float outlineSize = std::get<float>(valueAt(sub, StyleType::BORDERSIZE, 0));
    const AlphaColor pOutline = std::get<AlphaColor>(valueAt(sub, StyleType::OUTLINE, 0));
    auto stateAt = [&](int pos) {
        QList<StyleValue> v;
        for (StyleType::Id id : inline_) v.append(valueAt(sub, id, pos));
        return v;
    };
    QString out;
    int pos = 0;
    while (pos < text.length()) {
        const QList<StyleValue> state = stateAt(pos);
        int end = pos + 1;
        while (end < text.length() && stateAt(end) == state)
            ++end;
        const QString run = text.mid(pos, end - pos);
        // Attributes that differ from the entry style.
        Attrs attrs;
        const SubStylePtr style = sub.getStyle();
        if (std::get<bool>(state[0]) && !(style && style->flag(StyleType::BOLD))) attrs.append({QStringLiteral("tts:fontWeight"), QStringLiteral("bold")});
        if (std::get<bool>(state[1]) && !(style && style->flag(StyleType::ITALIC))) attrs.append({QStringLiteral("tts:fontStyle"), QStringLiteral("italic")});
        QStringList decoration;
        if (std::get<bool>(state[2]) && !(style && style->flag(StyleType::UNDERLINE))) decoration.append(QStringLiteral("underline"));
        if (std::get<bool>(state[6]) && !(style && style->flag(StyleType::STRIKETHROUGH))) decoration.append(QStringLiteral("lineThrough"));
        if (!decoration.isEmpty()) attrs.append({QStringLiteral("tts:textDecoration"), decoration.join(QLatin1Char(' '))});
        const AlphaColor col = std::get<AlphaColor>(state[3]);
        if (!style || col != style->color(StyleType::PRIMARY)) attrs.append({QStringLiteral("tts:color"), formatTTMLColor(col)});
        const int size = std::get<int>(state[4]);
        if (size > 0 && (!style || size != style->fontSize())) attrs.append({QStringLiteral("tts:fontSize"), QString::number(size) + QLatin1String("px")});
        const QString name = std::get<QString>(state[5]);
        if (!name.isEmpty() && (!style || name != style->fontName()) && isFontAllowed(name)) attrs.append({QStringLiteral("tts:fontFamily"), name});
        // A different outline colour: the box colour inside a box, else the outline's.
        const AlphaColor outline = std::get<AlphaColor>(state[7]);
        if (outline != pOutline) {
            if (boxed) attrs.append({QStringLiteral("tts:backgroundColor"), formatTTMLColor(outline)});
            else attrs.append({QStringLiteral("tts:textOutline"), formatTTMLColor(outline) + QLatin1Char(' ') + QString::number(outlineSize) + QLatin1String("px")});
        }
        if (attrs.isEmpty())
            out += textWithBreaksXml(run);
        else {
            QString span = QStringLiteral("<span");
            for (const auto &a : attrs)
                span += QLatin1Char(' ') + a.first + QLatin1String("=\"") + xmlEscape(a.second) + QLatin1Char('"');
            out += span + QLatin1Char('>') + textWithBreaksXml(run) + QLatin1String("</span>");
        }
        pos = end;
    }
    return out;
}

bool W3CFamily::produce(const Subtitles &subs, const QString &outfile, const MediaFile *media, SaveError &error) {
    const QString body = renderDocument(subs, media);
    const QString text = QStringLiteral("<?xml version=\"1.0\" encoding=\"%1\" standalone=\"no\"?>\n").arg(ENCODING_) + body;
    QByteArray bytes;
    const EncodeResult r = encodeText(text, ENCODING_, bytes);
    if (r != EncodeResult::Ok) {
        error = {SaveError::Encoding, encodeErrorMessage(r, ENCODING_)};
        return false;
    }
    QString detail;
    if (writeFileAtomically(outfile, bytes, detail)) return true;
    error = {SaveError::Io, detail};
    return false;
}

// ---- the four formats ---------------------------------------------------------

void DFXP::addCustomRegionAttributes(Attrs &a, Direction) {
    a.append({QStringLiteral("tts:writingMode"), QStringLiteral("lrtb")});
}

QMap<QString, QString> ITT::getAdditionalNamespaces() const {
    return {{QStringLiteral("itunes"), QStringLiteral("http://www.apple.com/itunes/importer")}};
}

void ITT::addCustomRegionAttributes(Attrs &a, Direction) {
    a.append({QStringLiteral("tts:writingMode"), QStringLiteral("lrtb")});
}
