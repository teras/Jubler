/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QRegularExpression>

#include "core/media/MediaFile.h"
#include "core/tools/Tool.h"

// The built-in editing tools (Java `coretools`): algorithms and parameters.
// The dialogs that fill the parameters live in the app layer.

// One synchronisation point from the video "pipette": subtitle time and the
// difference video − subtitle (seconds). Port of `TimeSync`.
struct TimeSync {
    double timepos = 0, timediff = 0;
    bool smallerThan(const TimeSync &o) const { return timepos < o.timepos; }
    bool isEqualDiff(const TimeSync &o) const { return std::abs(timediff - o.timediff) < 0.001; }
};

// Shift every affected entry by a fixed offset.
class ShiftTime : public OneByOneTool {
public:
    ShiftTime();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("shift"); }
    QString getCommandLineHelp() const override;
    void setShift(double seconds) { shift_ = seconds; }
    double shift() const { return shift_; }
    // From two pipette points: false when the difference is below 1 ms.
    bool setValues(const TimeSync &first, const TimeSync &second);

protected:
    void affect(SubEntry &sub) override;
    QStringList gatherSelfTags() const override { return {QStringLiteral("delta")}; }
    QString applyToolSpecificArguments(const QMap<QString, QString> &args) override;

private:
    double shift_ = 0;
};

// Scale times around a centre: t = (t − c)·f + c.
class RecodeTime : public OneByOneTool {
public:
    RecodeTime();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("recode"); }
    QString getCommandLineHelp() const override;
    void setCustom(double center, double factor) { center_ = center; factor_ = factor; }
    void setFromFps(double from, double to) { center_ = 0; factor_ = from / to; }
    double center() const { return center_; }
    double factor() const { return factor_; }
    // From two pipette points; false when they do not define a valid line.
    bool setValues(const TimeSync &first, const TimeSync &second, double &centerOut, double &factorOut);

protected:
    void affect(SubEntry &sub) override;
    QStringList gatherSelfTags() const override { return {QStringLiteral("center"), QStringLiteral("factor"), QStringLiteral("fromfps"), QStringLiteral("tofps")}; }
    QString applyToolSpecificArguments(const QMap<QString, QString> &args) override;

private:
    double center_ = 0, factor_ = 1;
};

// Copy times and/or text from another document, entry by index (+offset).
class Synchronize : public OneByOneTool {
public:
    Synchronize();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("sync"); }
    QString getCommandLineHelp() const override;
    void setModel(const Subtitles *model) { model_ = model; }
    void setTarget(const Subtitles *target) { target_ = target; }
    void setCopy(bool time, bool text) { copytime_ = time; copytext_ = text; }
    void setOffset(int offset) { offset_ = offset; }
    bool affect(QList<SubEntryPtr> &list) override;

protected:
    void affect(SubEntry &sub) override;
    QStringList gatherSelfTags() const override { return {QStringLiteral("sourcesub"), QStringLiteral("offset"), QStringLiteral("timestamp"), QStringLiteral("text")}; }
    QString applyToolSpecificArguments(const QMap<QString, QString> &args) override;

private:
    const Subtitles *model_ = nullptr, *target_ = nullptr;
    bool copytime_ = true, copytext_ = false;
    int offset_ = 0;
};

// Fix durations, gaps and overlaps.
class Fixer : public OneByOneTool {
public:
    enum PushModel { DISTRIBUTE = 0, DIVIDE = 1, SHIFT = 2 };
    Fixer();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("fix"); }
    QString getCommandLineHelp() const override;
    // Durations: absolute seconds or per-character factors (−1 = unset).
    // Durations in seconds, reading rates in characters per second; -1 = unused.
    // The minimum duration comes from the fastest acceptable reading rate, the
    // maximum duration from the slowest one.
    void setDurations(double minAbs, double fastCps, double maxAbs, double slowCps) { minAbs_ = minAbs; fastCps_ = fastCps; maxAbs_ = maxAbs; slowCps_ = slowCps; }
    void setFix(bool fix, PushModel model, double gapSeconds) { fix_ = fix; pushmodel_ = model; gap_ = gapSeconds; }
    void setSortFirst(bool sort) { sortFirst_ = sort; }
    bool sortFirst() const { return sortFirst_; }
    bool affect(QList<SubEntryPtr> &list) override;

protected:
    void affect(SubEntry &sub) override;
    QStringList gatherSelfTags() const override { return {QStringLiteral("mintime"), QStringLiteral("maxtime"), QStringLiteral("mincps"), QStringLiteral("maxcps"), QStringLiteral("overlap"), QStringLiteral("gap")}; }
    QString applyToolSpecificArguments(const QMap<QString, QString> &args) override;

private:
    bool fix_ = false, sortFirst_ = false;
    int pushmodel_ = DISTRIBUTE;
    double minAbs_ = -1, fastCps_ = -1, maxAbs_ = -1, slowCps_ = -1, gap_ = 0;
    double pushTime_ = 0;
};

// Round times to a number of decimals.
class Rounder : public OneByOneTool {
public:
    Rounder();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("round"); }
    QString getCommandLineHelp() const override;
    void setDecimals(int decimals);   // 0..3
    static void roundTime(Time &t, int precise);

protected:
    void affect(SubEntry &sub) override;
    QStringList gatherSelfTags() const override { return {QStringLiteral("decimals")}; }
    QString applyToolSpecificArguments(const QMap<QString, QString> &args) override;

private:
    int precise_ = 1;
};

// Set the mark of the affected entries.
class Marker : public OneByOneTool {
public:
    Marker();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("mark"); }
    QString getCommandLineHelp() const override;
    void setMark(int mark) { mark_ = mark; }

protected:
    void affect(SubEntry &sub) override { sub.setMark(mark_); }
    QStringList gatherSelfTags() const override { return {QStringLiteral("mark")}; }
    QString applyToolSpecificArguments(const QMap<QString, QString> &args) override;

private:
    int mark_ = 0;
};

// Delete the affected entries.
class DelSelection : public OneByOneTool {
public:
    DelSelection();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("delete"); }
    QString getCommandLineHelp() const override;

protected:
    void affect(SubEntry &sub) override;
    QStringList gatherSelfTags() const override { return {}; }
    QString applyToolSpecificArguments(const QMap<QString, QString> &) override { return QString(); }
};

// Set the style of the affected entries.
class Styler : public OneByOneTool {
public:
    Styler();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("style"); }
    QString getCommandLineHelp() const override;
    void setStyle(const SubStylePtr &s) { style_ = s; }

protected:
    void affect(SubEntry &sub) override { sub.setStyle(style_); }
    QStringList gatherSelfTags() const override { return {QStringLiteral("style")}; }
    QString applyToolSpecificArguments(const QMap<QString, QString> &args) override;

private:
    SubStylePtr style_;
};

// Join the affected entries into the first one.
class JoinEntries : public TimeBaseTool {
public:
    JoinEntries();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("jointext"); }
    QString getCommandLineHelp() const override;
    bool affect(QList<SubEntryPtr> &list) override;

protected:
    QStringList gatherExtendedTimedTags() const override { return {}; }
    QString applyToolSpecificArguments(const QMap<QString, QString> &) override { return QString(); }
};

// Split every affected entry at its line breaks.
class SplitEntries : public OneByOneTool {
public:
    SplitEntries();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("splittext"); }
    QString getCommandLineHelp() const override;

protected:
    void affect(SubEntry &sub) override;
    QStringList gatherSelfTags() const override { return {}; }
    QString applyToolSpecificArguments(const QMap<QString, QString> &) override { return QString(); }
};

// Regular-expression replace (menu-less; used by the Edit menu and the
// command line).
class RegExpReplace : public OneByOneTool {
public:
    RegExpReplace();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("regex"); }
    QString getCommandLineHelp() const override;
    // Replace rules (pattern, replacement); invalid patterns → false.
    bool setRules(const QList<QPair<QString, QString>> &rules, QString *error = nullptr);
    // Null when every "$" / "\\" reference of `replacement` is valid for `re`.
    static QString checkReplacement(const QRegularExpression &re, const QString &replacement);
    // Java replaceAll semantics ($1, ${name}, backslash escapes).
    static QString replaceAll(const QString &text, const QRegularExpression &re, const QString &replacement);

protected:
    void affect(SubEntry &sub) override;
    QStringList gatherSelfTags() const override { return {QStringLiteral("pattern"), QStringLiteral("replace"), QStringLiteral("esc")}; }
    QString applyToolSpecificArguments(const QMap<QString, QString> &args) override;

private:
    QList<QRegularExpression> patterns_;
    QStringList replacements_;
};

// Split a document in two at a time (the second part re-based to zero).
class SubSplit : public Tool {
public:
    SubSplit();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("split"); }
    QString getCommandLineHelp() const override;
    QStringList gatherToolTags() const override { return {QStringLiteral("at"), QStringLiteral("target")}; }
    QString executeParams(const QMap<QString, QString> &params, bool debug) override;
    // The algorithm: entries starting before `at` stay (as a new document
    // "<name>_1"), the rest go to "<name>_2" shifted by −at.
    static void split(Subtitles &source, double atSeconds, Subtitles &first, Subtitles &second);
};

// Append another document after this one (with a gap) or prepend it.
class SubJoin : public Tool {
public:
    SubJoin();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("join"); }
    QString getCommandLineHelp() const override;
    QStringList gatherToolTags() const override { return {QStringLiteral("gap"), QStringLiteral("append")}; }
    QString executeParams(const QMap<QString, QString> &params, bool debug) override;
};

// Set the parent document (GUI only).
class Reparent : public Tool {
public:
    Reparent();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QString(); }
    QString getCommandLineHelp() const override { return QString(); }
    QStringList gatherToolTags() const override { return {}; }
    QString executeParams(const QMap<QString, QString> &, bool) override { return QString(); }
};

// Command-line only: insert one entry at the right chronological position.
class AddSubtitle : public Tool {
public:
    AddSubtitle() : Tool(std::nullopt) {}
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("add"); }
    QString getCommandLineHelp() const override;
    QStringList gatherToolTags() const override { return {QStringLiteral("start"), QStringLiteral("end"), QStringLiteral("text"), QStringLiteral("style"), QStringLiteral("mark")}; }
    QString executeParams(const QMap<QString, QString> &params, bool debug) override;
};

// Command-line only: sort the entries of a time range.
class SortSubtitles : public Tool {
public:
    SortSubtitles() : Tool(std::nullopt) {}
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QStringLiteral("sort"); }
    QString getCommandLineHelp() const override;
    QStringList gatherToolTags() const override { return {QStringLiteral("start"), QStringLiteral("end")}; }
    QString executeParams(const QMap<QString, QString> &params, bool debug) override;
};
