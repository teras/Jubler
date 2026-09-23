/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include "core/formats/StyledTextSubFormat.h"

// SubStation Alpha v4 (.ssa): [Script Info] attributes, PlayResY-based font
// scaling, the [V4 Styles] table and Dialogue events with {\…} overrides.
// The file is read section by section, honouring the Format: lines; what the
// model has no place for (other header keys, the PlayRes canvas, unknown
// sections, Comment: events, the style Encoding) is kept in the document's
// format data and written back.
class SubStationAlpha : public GenericStyledTextSubFormat {
public:
    static constexpr int FONT_REF = 384;          // PlayResY at which core sizes are verbatim
    static constexpr int FONT_DEFAULT_RES = 288;  // assumed when the file declares none
    // PlayResX written with FONT_REF when there is no video (16:9).
    static constexpr int REF_WIDTH = 683;

    QString getExtension() const override { return QStringLiteral("ssa"); }
    QString getName() const override { return QStringLiteral("SubStationAlpha"); }
    bool supportsFPS() const override { return false; }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<SubStationAlpha>(); }

    static const StyledFormat::DirectionMap &ssaDirections();
    static const StyledFormat::DirectionMap &assDirections();
    static int directionToAn(Direction d);

protected:
    const QRegularExpression &getPattern() override;
    const QRegularExpression &getTestPattern() override;
    const QRegularExpression &getStylePattern() override;
    QStringList tokenize(const QString &body) override;
    QString getEventIntro() override { return QStringLiteral("{"); }
    QString getEventFinal() override { return QStringLiteral("}"); }
    QString getEventMark() override { return QStringLiteral("\\"); }
    bool isEventCompact() override { return true; }
    float getFontFactor() override { return fontFactor_; }
    const QList<StyledFormat> &getStylesDictionary() override;
    const QMap<QString, QString> &getStylePairs() override;
    bool handleTagEvent(const QString &body, int pos, SubEntry &entry, const SubStylePtr &style) override;
    bool acceptsTagValue(const StyledFormat &sf, const QString &value) override;

    QString initLoader(const QString &input) override;
    SubEntryPtr getSubEntry(const QRegularExpressionMatch &m) override;
    void appendSubEntry(const SubEntry &sub, QString &str) override;
    void initSaver(const Subtitles &subs, const MediaFile *media, QString &header) override;
    void cleanupSaver(QString &str) override;

    virtual QString getExtraVersion() { return QString(QLatin1String("")); }
    virtual QString getLayerTitle() { return QStringLiteral("Marked"); }
    virtual void appendStyles(const Subtitles &subs, QString &header);
    virtual bool alphaInStyles() const { return false; }
    // The style table columns written (and assumed without a Format: line).
    virtual QString styleFormat() const;
    // Alignment numbering of the style table.
    virtual const StyledFormat::DirectionMap &styleDirections() const { return ssaDirections(); }

    // The text with its overrides re-emitted and a single leading {\an#}
    // when the entry's alignment differs from its style.
    QString rebuildSubTextWithOverrides(const SubEntry &sub);
    // As above, with the literal braces of the text written "\{" / "\}".
    QString rebuildEscapedSubText(const SubEntry &sub);
    static QString stripAlignmentTags(QString text);
    virtual QString alignmentTag(Direction d);
    // A text rotation override as a leading tag; SSA has none.
    virtual QString angleTag(float) { return QString(); }
    static QString timeformat(const Time &t);
    static int booleanToInt(bool b) { return b ? -1 : 0; }
    AlphaColor stringToAlphaColor(const QString &revRGB, const QString &alpha) const;
    QString alphaColorToString(const AlphaColor &c, bool storeAlpha) const;
    // The style a Dialogue line references ("*Default" → "Default").
    SubStylePtr styleForName(const QString &name) const;
    // A style column value as the file had it when the model value is still
    // the one it was read into (decimal sizes and scales survive a save),
    // otherwise `computed`.
    static QString keptStyleText(const SubStyle &style, const QString &column, const QString &model, const QString &computed);
    // A core font size in file units; the style's own size as the file had
    // it, so style sizes and returns to them ({\fs}) are written exactly.
    QString fontSizeFor(const SubStyle &style, int core);
    QString fontSizeModel(int core) const { return QString::number(core) + QLatin1Char('@') + QString::number(playResY_); }
    QString fontSizeText(const SubEntry &entry, int core) override;

    float fontFactor_ = 1.0f;
    int playResY_ = FONT_REF;           // of the file being read or written

private:
    // One "Style:" line, read with the section's Format: columns.
    void readStyle(const QStringList &format, const QString &data, SubStyleList &list);

    QStringList eventFormat_;           // the [Events] Format: columns, normalised
    bool hardSoftBreaks_ = false;       // WrapStyle 2: "\n" is a hard break too
    QMap<QString, QString> saveData_;   // the format data of the document being saved
};

// Advanced SubStation Alpha (.ass, v4.00+): 23-field style table, extended
// override dictionary, Layer events.
class AdvancedSubStation : public SubStationAlpha {
public:
    QString getExtension() const override { return QStringLiteral("ass"); }
    QString getName() const override { return QStringLiteral("AdvancedSubStation"); }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<AdvancedSubStation>(); }

    // The plugin API's text form: the entry's text with its inline styling
    // as {\…} override tags, "\n" line breaks and literal braces as "\{",
    // "\}"; and back (the entry's style must be set first: resets use it).
    static QString toTaggedText(const SubEntry &entry);
    static void setTaggedText(SubEntry &entry, const QString &text);

protected:
    const QRegularExpression &getTestPattern() override;
    const QList<StyledFormat> &getStylesDictionary() override;
    QString getExtraVersion() override { return QStringLiteral("+"); }
    QString angleTag(float angle) override { return QStringLiteral("{\\frz%1}").arg(QString::number(angle)); }
    QString getLayerTitle() override { return QStringLiteral("Layer"); }
    void appendStyles(const Subtitles &subs, QString &header) override;
    bool alphaInStyles() const override { return true; }
    QString styleFormat() const override;
    const StyledFormat::DirectionMap &styleDirections() const override { return assDirections(); }
    QString alignmentTag(Direction d) override;
};
