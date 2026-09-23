/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QHash>
#include <QPair>
#include <QString>
#include <QStringList>
#include <array>
#include <memory>

#include "core/style/Styleover.h"
#include "core/style/SubStyle.h"
#include "core/subs/SubMetrics.h"
#include "core/time/Time.h"

// One subtitle event, port of the Java `SubEntry`: a time span, the text
// (lines separated by '\n'), the referenced style, an optional per-attribute
// inline override table, the quality/user mark, and the ASS-only extras
// (layer, actor name, margins, effect) carried through for round-tripping.
class SubEntry {
public:
    static constexpr int SMALL_MILLIS = 50;
    // Mark 0 is "none"; 1..5 are the highlight colours (translated names via
    // markName()).
    static constexpr int MARK_COUNT = 6;
    static const char *const MarkColorKeys[MARK_COUNT];  // "none","pink","yellow","cyan","orange","lightgreen"
    static QString markName(int mark);                    // translated: None, Pink, Yellow, Cyan, Orange, Light Green

    SubEntry();
    SubEntry(double start, double finish, const QString &line);
    SubEntry(const Time &start, const Time &finish, const QString &line);
    SubEntry(const SubEntry &old);
    SubEntry &operator=(const SubEntry &old);

    int compareTo(const SubEntry &other) const { return start_.compareTo(other.start_); }
    bool operator<(const SubEntry &o) const { return start_ < o.start_; }

    // --- mark / style / ASS extras ---
    void setMark(int i) { mark_ = i; }
    int getMark() const { return mark_; }
    void setStyle(const SubStylePtr &s) { style_ = s; }
    const SubStylePtr &getStyle() const { return style_; }
    QString getLayer() const { return layer_; }
    void setLayer(const QString &l) { layer_ = l; }
    QString getName() const { return name_; }
    void setName(const QString &n) { name_ = n.isNull() ? QString(QLatin1String("")) : n; }
    QString getMarginL() const { return marginL_; }
    void setMarginL(const QString &m) { marginL_ = m; }
    QString getMarginR() const { return marginR_; }
    void setMarginR(const QString &m) { marginR_ = m; }
    QString getMarginV() const { return marginV_; }
    void setMarginV(const QString &m) { marginV_ = m; }
    QString getEffect() const { return effect_; }
    void setEffect(const QString &e) { effect_ = e; }
    // The written text of an inline font size as the file had it ("40.5"),
    // for the core size it was read as at the canvas `factor`: written back
    // while the size and the canvas are unchanged (core sizes are integers).
    void keepFontSizeText(int core, const QString &text, float factor) { fontSizeTexts_.insert(core, {text, factor}); }
    QString keptFontSizeText(int core, float factor) const {
        const auto it = fontSizeTexts_.constFind(core);
        return it != fontSizeTexts_.constEnd() && qFuzzyCompare(it->second, factor) ? it->first : QString();
    }
    QString getToolTipText() const { return toolTipText_; }
    void setToolTipText(const QString &t) { toolTipText_ = t; }

    // --- inline overrides ---
    bool hasStyleovers() const { return hasOverstyle_; }
    // Null when this attribute has no override.
    const Styleover *getStyleover(StyleType::Id type) const { return overstyle_[type].get(); }
    Styleover *getStyleoverMutable(StyleType::Id type) { return overstyle_[type].get(); }
    // Raw change point (parsers).
    void addOverStyle(StyleType::Id type, const StyleValue &value, int start);
    // Apply a value over [start, end] of the text.
    void setOverStyle(StyleType::Id type, const StyleValue &value, int start, int end);
    void resetOverStyle();
    void cleanupEvents();
    // The value of `type` in effect over [start, end]; nullopt when mixed.
    std::optional<StyleValue> overValue(StyleType::Id type, int start, int end) const;
    // Text-edit bookkeeping for the overrides.
    void insertText(int start, int length);
    void removeText(int start, int length);

    // --- text & times ---
    void setText(const QString &text);
    const QString &getText() const { return subtext_; }
    void setStartTime(const Time &t) { start_.setTime(t); }
    void setFinishTime(const Time &t) { finish_.setTime(t); }
    Time &getStartTime() { return start_; }
    Time &getFinishTime() { return finish_; }
    const Time &getStartTime() const { return start_; }
    const Time &getFinishTime() const { return finish_; }
    Time getDurationTime() const { return finish_.difference(start_); }
    bool isInTime(double t) const { return t >= start_.toSeconds() && t <= finish_.toSeconds(); }
    bool isDurationSmall() const { return getDurationTime().getMillis() < SMALL_MILLIS; }

    // Table cell text for column `col` (0 #, 1 Start, 2 End, 3 Duration,
    // 4 Layer, 5 Style, 6 Cpm, 7 Cps, 8 Subtitle with '\n' shown as '|').
    QString getData(int row, int col) const;

    QString toString() const { return start_.toString() + QLatin1String("->") + finish_.toString() + QLatin1Char(' ') + subtext_; }

    // --- quality ---
    SubMetrics getMetrics() const;
    void updateQuality();
    void updateQuality(const SubMetrics &m);

    // --- text helpers (Java "record" helpers) ---
    void copyRecord(const SubEntry &o) { *this = o; }
    // Merge another entry into this one: text appended (see appendText), time
    // span = union.
    void mergeRecord(const SubEntry &o);
    // Split this entry in two halves by word count and by duration; returns
    // the second half.
    SubEntry splitRecord();
    int getTextLineCount() const;
    QStringList getTextList() const;
    bool setText(const QStringList &lines);
    QString getTextWithoutLineBreak() const;
    QString getTextLine(int line) const;        // null when out of range
    QString getFirstTextLine() const { return getTextLine(0); }
    QString getLastTextLine() const { return getTextLine(getTextLineCount() - 1); }
    bool isTextLineEqual(const SubEntry &other, int line) const;
    bool isFirstTextLineEqual(const SubEntry &other) const { return isTextLineEqual(other, 0); }
    bool isThisBottomTextLineDuplicatedToOtherTopLine(const SubEntry &other) const;
    QStringList getTextExcludingLine(int line) const;
    QStringList getTextExcludingTopLine() const { return getTextExcludingLine(0); }
    QStringList getTextExcludingBottomLine() const { return getTextExcludingLine(getTextLineCount() - 1); }
    void removeTextLine(int index) { setText(getTextExcludingLine(index)); }
    void addAllText(const QStringList &more);
    bool isSameStartTime(const SubEntry &o) const;
    bool isSameEndTime(const SubEntry &o) const;
    bool appendText(const QString &line, const QString &separator);
    bool isOneWord() const;
    bool appendText(const QStringList &lines);
    bool addTextLine(const QString &line) { return appendText(line, QStringLiteral("\n")); }
    bool addWord(const QString &word) { return appendText(word, QStringLiteral(" ")); }
    bool reSpacingText();
    bool removeLineBreak(int line);
    bool copyText(const SubEntry &source) { setText(source.getText()); return true; }
    bool copyTime(const SubEntry &source) { setStartTime(source.start_); setFinishTime(source.finish_); return true; }
    bool cutText() { setText(QString(QLatin1String(""))); return true; }
    bool cutTime() { setStartTime(Time(0.0)); setFinishTime(Time(0.0)); return true; }

    static int wordCount(const QString &text);

private:
    Styleover &styleover(StyleType::Id type);
    const StyleValue &basicValue(StyleType::Id type) const;
    static bool isHyphenatedWord(const QString &word);
    static QString collectWord(const QStringList &list, int from, int to);
    void copyOverstyle(const SubEntry &old);

    Time start_, finish_;
    QString subtext_;
    int mark_ = 0;
    SubStylePtr style_;
    QString layer_ = QStringLiteral("0");
    QString name_ = QLatin1String("");
    QString marginL_ = QStringLiteral("0000");
    QString marginR_ = QStringLiteral("0000");
    QString marginV_ = QStringLiteral("0000");
    QString effect_ = QLatin1String("");
    QString toolTipText_;
    QHash<int, QPair<QString, float>> fontSizeTexts_;
    std::array<StyleoverPtr, StyleType::COUNT> overstyle_;
    bool hasOverstyle_ = false;
};
