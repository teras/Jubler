/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QDomDocument>
#include <QHash>
#include <QSet>
#include <functional>
#include <optional>

#include "core/formats/TextSubFormat.h"

// Everything shared by the TTML family (W3C Timed Text, DFXP, TTML, iTunes
// Timed Text): region → placement mapping, tts style parsing, nested <span>
// inline styles, SMPTE/media/offset time parsing with drop-frame support,
// and the TTML document generator. Port of `AbstractXMLSubFormat` +
// `W3CFamily` (the live paths only).
class W3CFamily : public AbstractGenericTextSubFormat {
public:
    enum class TimeBase { MEDIA, SMPTE, CLOCK };
    enum class DropMode { NON_DROP, DROP_NTSC, DROP_PAL };
    enum class LanguageFormat { SIMPLE, FULL };            // "en" / "en-US"
    enum class TTMLProfile { DFXP_PRESENTATION, DFXP_TRANSFORMATION, DFXP_FULL, IMSC1_TEXT, IMSC1_IMAGE };
    enum class FontRestriction { NONE, SANS_SERIF_ONLY, BASIC_FONTS };
    enum class StructureRestriction { NONE, SINGLE_DIV, LIMITED_REGIONS };

    static const QString TTML_NS;
    static const QString TTML_STYLING_NS;
    static const QString TTML_PARAMETER_NS;
    static const QString TTML_METADATA_NS;

    std::unique_ptr<Subtitles> parse(const QString &input, float fps, const QString &file, bool debug) override;
    bool produce(const Subtitles &subs, const QString &outfile, const MediaFile *media, SaveError &error) override;
    // The whole document text (without the XML declaration).
    QString renderDocument(const Subtitles &subs, const MediaFile *media);
    QString render(const Subtitles &subs, const MediaFile *media) override { return renderDocument(subs, media); }
    bool supportsFPS() const override { return getTimeBase() == TimeBase::SMPTE; }
    float detectedFPS() const override { return declaredFrameRate_ && getTimeBase() == TimeBase::SMPTE ? float(effectiveFrameRate_) : -1.0f; }

protected:
    // --- format parameters ---
    virtual TimeBase getTimeBase() const = 0;
    virtual DropMode getDropMode() const = 0;
    virtual LanguageFormat getLanguageFormat() const = 0;
    virtual TTMLProfile getTTMLProfile() const = 0;
    virtual FontRestriction getFontRestriction() const = 0;
    virtual StructureRestriction getStructureRestriction() const = 0;
    virtual QMap<QString, QString> getAdditionalNamespaces() const { return {}; }
    virtual QString getDocumentTitle() const = 0;
    virtual bool supportsInlineFormatting() const { return true; }
    virtual QString getDefaultFontFamily() const { return QStringLiteral("sansSerif"); }
    virtual QString getDefaultFontSize() const { return QStringLiteral("100%"); }
    virtual QString getDefaultTextColor() const { return QStringLiteral("white"); }
    using Attrs = QList<QPair<QString, QString>>;
    virtual void addCustomStyleAttributes(Attrs &) {}
    virtual void addCustomRegionAttributes(Attrs &, Direction) {}

    // Unused by the XML path (parse/produce are overridden) but required.
    void appendSubEntry(const SubEntry &, QString &) override {}
    bool isSubtitleCompatible(const QString &input) override;
    QList<SubEntryPtr> loadSubtitles(const QString &, bool) override { return {}; }

    static QString timeBaseValue(TimeBase t);
    static QString dropModeValue(DropMode d);
    static QString profileValue(TTMLProfile p);
    bool isFontAllowed(const QString &family) const;

    // --- loading ---
    // Specified tts properties of an element (local name → value).
    using TtsMap = QHash<QString, QString>;
    struct RegionInfo {
        QString id, displayAlign, textAlign;
        // Origin/extent in percent of the root container.
        float ox = 0, oy = 80, ew = 0, eh = 0;
        bool hasExtent = false;
        // tts:writingMode: 0 horizontal, 1 tbrl, 2 tblr. In vertical modes
        // displayAlign places the column horizontally, textAlign vertically.
        int writing = 0;
        float originX() const { return ox; }
        float originY() const { return oy; }
        Direction direction() const;
        int leftMargin() const;
        int rightMargin() const;

    private:
        int vertical() const;    // 0 top, 1 centre, 2 bottom
        int horizontal() const;  // 0 left, 1 centre, 2 right
        bool withinScreen() const;
    };
    // Absolute timing of an element (ms), after the time containment of its ancestors.
    // `end` is clipped to the ancestors' ends (none: indefinite); `activeEnd`
    // is the element's own (unclipped) end; `never` when it never becomes
    // active (after its parent's end, or after an indefinite sibling of a seq).
    struct Timing { long long begin = 0; std::optional<long long> end, activeEnd; bool never = false; };
    RegionInfo regionFrom(const QString &id, const TtsMap &tts) const;
    bool lengthToPercent(const QString &value, bool horizontal, float &out) const;
    void parseRegions(const QDomDocument &doc);
    void parseStyles(const QDomDocument &doc, Subtitles &subs);
    void parseMetadata(const QDomElement &root, Subtitles &subs);
    // The entries of a paragraph: one, or one per interval between the
    // time boundaries of its timed spans.
    QList<SubEntryPtr> parseSubtitleElement(const QDomElement &p, Subtitles &subs);
    // The paragraph as shown during [begin, end); with `windowed` only the
    // content elements active during the whole interval are included.
    SubEntryPtr entryFor(const QDomElement &p, Subtitles &subs, long long begin, long long end, bool windowed);
    // Map tts properties onto a style. `restrictFonts` applies the format's
    // font restriction; `placement` maps textAlign/displayAlign to DIRECTION.
    void applyTts(const TtsMap &tts, SubStyle &style, bool restrictFonts, bool placement) const;
    TtsMap styleTts(const QString &id, QSet<QString> &visiting) const;
    TtsMap resolveStyleElement(const QDomElement &el, QSet<QString> &visiting) const;
    TtsMap specifiedTts(const QDomElement &el) const;
    static TtsMap ownTts(const QDomElement &el);
    Timing timingOf(const QDomElement &el) const;
    Timing computeTiming(const QDomElement &el) const;
    // The end from begin/end/dur (end relative to `syncBase`), if any.
    std::optional<long long> explicitEnd(const QDomElement &el, long long syncBase, long long begin) const;
    // The implicit end of an element beginning at `begin` (none: indefinite),
    // as a child of a seq container or not.
    std::optional<long long> implicitEnd(const QDomElement &el, long long begin, bool inSeq) const;
    void detectFrameRate(const QDomDocument &doc);
    Time parseTime(const QString &text) const;
    Time parseSMPTETime(const QString &text) const;
    static Time parseMediaTime(const QString &text);
    static bool parseColor(const QString &text, QColor &out);
    std::optional<double> parseLength(const QString &text) const;
    std::optional<int> parseFontSize(const QString &text) const;

    // --- saving ---
    QString formatTime(const Time &t) const;
    static QString formatTTMLColor(const AlphaColor &c);
    static QString regionIdForDirection(Direction d);
    // Region id for a placement and writing mode (0 horizontal, 1 tbrl, 2 tblr).
    static QString regionIdFor(Direction d, int writing);
    static Direction effectiveDirection(const SubEntry &e);
    // Vertical text: 1 tbrl for a whole-entry angle of 90, 2 tblr for -90, else 0.
    static int writingModeOf(const SubEntry &e);
    class XmlOut;
    void writeStyling(XmlOut &w, const Subtitles &subs);
    void writeLayout(XmlOut &w, const Subtitles &subs);
    void writeRegion(XmlOut &w, Direction d, int writing, const Subtitles &subs);
    void writeEntry(XmlOut &w, const SubEntry &sub);
    // tts:backgroundColor / tts:textOutline for a box (BORDERSTYLE 1), outline
    // thickness and outline (box) colour that differ from the base ones.
    static void boxOutlineAttrs(Attrs &a, int box, float size, const AlphaColor &col, int baseBox, float baseSize, const AlphaColor &baseCol);
    QString entryTextXml(const SubEntry &sub);
    static QString textWithBreaksXml(const QString &text);
    // The value of `type` at character `pos` (style value when no override).
    static StyleValue valueAt(const SubEntry &sub, StyleType::Id type, int pos);

    QHash<QString, TtsMap> regions_;
    QHash<QString, QDomElement> styleElements_;
    QHash<QString, SubStylePtr> styleMap_;
    double tickRate_ = 1.0;
    int cellColumns_ = 32, cellRows_ = 15;
    float rootWidth_ = 0, rootHeight_ = 0;   // tts:extent of <tt> in pixels, 0 if unknown
    double effectiveFrameRate_ = 30.0;
    bool declaredFrameRate_ = false;
    DropMode detectedDropMode_ = DropMode::NON_DROP;
    QDomDocument document_;
    // Timing of the loaded elements, by their (line, column) in the file.
    mutable QHash<QPair<int, int>, Timing> timingCache_;
};

// Generic W3C Timed Text (.xml).
class W3CTimedText : public W3CFamily {
public:
    QString getExtension() const override { return QStringLiteral("xml"); }
    QString getName() const override { return QStringLiteral("W3CTimedText"); }
    QString getExtendedName() const override { return QStringLiteral("W3C Timed Text"); }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<W3CTimedText>(); }

protected:
    TimeBase getTimeBase() const override { return TimeBase::MEDIA; }
    DropMode getDropMode() const override { return DropMode::NON_DROP; }
    LanguageFormat getLanguageFormat() const override { return LanguageFormat::SIMPLE; }
    TTMLProfile getTTMLProfile() const override { return TTMLProfile::DFXP_PRESENTATION; }
    FontRestriction getFontRestriction() const override { return FontRestriction::NONE; }
    StructureRestriction getStructureRestriction() const override { return StructureRestriction::NONE; }
    QString getDocumentTitle() const override { return QStringLiteral("W3C Timed Text Document"); }
};

// DFXP (.dfxp): basic fonts only, limited regions.
class DFXP : public W3CFamily {
public:
    QString getExtension() const override { return QStringLiteral("dfxp"); }
    QString getName() const override { return QStringLiteral("DFXP"); }
    QString getExtendedName() const override { return QStringLiteral("DFXP (TTML - Timed Text ML)"); }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<DFXP>(); }

protected:
    TimeBase getTimeBase() const override { return TimeBase::MEDIA; }
    DropMode getDropMode() const override { return DropMode::NON_DROP; }
    LanguageFormat getLanguageFormat() const override { return LanguageFormat::SIMPLE; }
    TTMLProfile getTTMLProfile() const override { return TTMLProfile::DFXP_PRESENTATION; }
    FontRestriction getFontRestriction() const override { return FontRestriction::BASIC_FONTS; }
    StructureRestriction getStructureRestriction() const override { return StructureRestriction::LIMITED_REGIONS; }
    QString getDocumentTitle() const override { return QStringLiteral("DFXP Document"); }
    void addCustomRegionAttributes(Attrs &a, Direction d) override;
};

// TTML (.ttml): dfxp-full profile, any fonts.
class TTML : public W3CFamily {
public:
    QString getExtension() const override { return QStringLiteral("ttml"); }
    QString getName() const override { return QStringLiteral("TTML"); }
    QString getExtendedName() const override { return QStringLiteral("TTML (Timed Text ML)"); }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<TTML>(); }

protected:
    TimeBase getTimeBase() const override { return TimeBase::MEDIA; }
    DropMode getDropMode() const override { return DropMode::NON_DROP; }
    LanguageFormat getLanguageFormat() const override { return LanguageFormat::SIMPLE; }
    TTMLProfile getTTMLProfile() const override { return TTMLProfile::DFXP_FULL; }
    FontRestriction getFontRestriction() const override { return FontRestriction::NONE; }
    StructureRestriction getStructureRestriction() const override { return StructureRestriction::NONE; }
    QString getDocumentTitle() const override { return QStringLiteral("TTML Document"); }
};

// iTunes Timed Text (.itt): SMPTE non-drop, en-US, sansSerif only.
class ITT : public W3CFamily {
public:
    QString getExtension() const override { return QStringLiteral("itt"); }
    QString getName() const override { return QStringLiteral("iTunes Timed Text"); }
    QString getExtendedName() const override { return QStringLiteral("iTunes Timed Text (ITT)"); }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<ITT>(); }

protected:
    TimeBase getTimeBase() const override { return TimeBase::SMPTE; }
    DropMode getDropMode() const override { return DropMode::NON_DROP; }
    LanguageFormat getLanguageFormat() const override { return LanguageFormat::FULL; }
    TTMLProfile getTTMLProfile() const override { return TTMLProfile::DFXP_PRESENTATION; }
    FontRestriction getFontRestriction() const override { return FontRestriction::SANS_SERIF_ONLY; }
    StructureRestriction getStructureRestriction() const override { return StructureRestriction::SINGLE_DIV; }
    QMap<QString, QString> getAdditionalNamespaces() const override;
    QString getDocumentTitle() const override { return QStringLiteral("iTunes Timed Text"); }
    void addCustomRegionAttributes(Attrs &a, Direction d) override;
};
