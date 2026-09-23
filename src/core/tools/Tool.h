/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QMap>
#include <QSet>
#include <QString>
#include <QStringList>
#include <functional>
#include <memory>
#include <stdexcept>

#include "core/subs/Subtitles.h"

// Where a tool's menu item lives (Java `ToolMenu.Location`).
enum class ToolLocation { FILETOOL, CONTENTTOOL, TIMETOOL, MARK, DELETE, STYLE };

// Menu description of a tool: translated label, component name (e.g. "TSH"),
// location, and an optional accelerator (Qt key + modifiers, 0 = none).
struct ToolMenu {
    QString text;
    QString name;
    ToolLocation location;
    int key = 0;
    int modifiers = 0;
};

// A user-facing parameter/filter error of the command-line path.
class FilterException : public std::runtime_error {
public:
    explicit FilterException(const QString &msg) : std::runtime_error(msg.toStdString()), msg_(msg) {}
    const QString &message() const { return msg_; }

private:
    QString msg_;
};

// The document slots of the command-line mode ("tags"); the null/empty tag
// is the default document tools operate on. Port of the static part of
// `CommandLine`.
namespace CommandLineContext {
Subtitles *getSubtitles(const QString &tag);
void addSubtitles(const QString &tag, std::unique_ptr<Subtitles> subs);
void removeSubtitles(const QString &tag);
bool hasSubtitles(const QString &tag);
void clear();
// Swap the tagged document with the default one.
void swap(const QString &tag);
}  // namespace CommandLineContext

// Base of every editing tool, port of the Java `Tool` minus the Swing
// dialog: the algorithm, its parameters (set by the GUI or parsed from the
// command line) and the command-line contract.
class Tool {
public:
    explicit Tool(std::optional<ToolMenu> menu) : menu_(std::move(menu)) {}
    virtual ~Tool() = default;

    const std::optional<ToolMenu> &menu() const { return menu_; }
    // Translated dialog/undo title.
    virtual QString getToolTitle() const = 0;
    // Command-line name (null/empty = not available on the command line).
    virtual QString getCommandOptionName() const = 0;
    virtual QString getCommandLineHelp() const = 0;
    // Valid command-line parameter keys.
    virtual QStringList gatherToolTags() const = 0;
    const QStringList &getToolTags();

    // Command-line entry: "key=value:key=value"; null on success.
    QString executeParamsLine(const QString &argument, bool debug);
    virtual QString executeParams(const QMap<QString, QString> &params, bool debug) = 0;

    // Parameter helpers (Java semantics).
    static double parseDoubleParameter(const QMap<QString, QString> &m, const QString &key);  // NaN when missing
    static int parseIntParameter(const QMap<QString, QString> &m, const QString &key);        // throws when missing
    static std::optional<bool> parseBooleanParameter(const QMap<QString, QString> &m, const QString &key);
    // Colour key ("pink"…) or index (0–5) → mark; -1 otherwise.
    static int parseMarkParam(const QMap<QString, QString> &m, const QString &key);
    static SubStylePtr parseStyleParam(const QMap<QString, QString> &m, const Subtitles *subs, const QString &key);
    static QList<SubEntryPtr> filterByColor(const Subtitles &subs, int mark);
    static QList<SubEntryPtr> filterByStyle(const Subtitles &subs, const SubStylePtr &style);

private:
    std::optional<ToolMenu> menu_;
    QStringList tags_;
    bool tagsGathered_ = false;
};

// A tool that works on a list of entries chosen by time range / selection /
// mark / style. Port of `TimeBaseTool` (the algorithm and command-line
// half; the dialog half is in the app layer).
class TimeBaseTool : public Tool {
public:
    TimeBaseTool(bool freeform, std::optional<ToolMenu> menu) : Tool(std::move(menu)), freeform_(freeform) {}

    // freeform: the GUI offers by-selection / by-colour / by-style / by-time
    // range; otherwise only the time range.
    bool isFreeform() const { return freeform_; }

    // The document the tool works on (set by the GUI before affect(); the
    // command line sets it from the default slot).
    void setSubtitles(Subtitles *subs) { subtitles_ = subs; }
    Subtitles *subtitles() const { return subtitles_; }

    // Apply the tool to the affected entries. False = failure.
    virtual bool affect(QList<SubEntryPtr> &list) = 0;

    QStringList gatherToolTags() const override;
    QString executeParams(const QMap<QString, QString> &params, bool debug) override;

    // Command-line filtering: overlap with [start,end] → by mark → by style.
    QList<SubEntryPtr> filterSubtitles(const Subtitles &subs, const QMap<QString, QString> &params) const;

protected:
    virtual QStringList gatherExtendedTimedTags() const = 0;
    // Set the tool's own parameters from the command line; null on success.
    virtual QString applyToolSpecificArguments(const QMap<QString, QString> &args) = 0;

    Subtitles *subtitles_ = nullptr;

private:
    bool freeform_;
};

// A tool applied to every affected entry in turn. Port of `OneByOneTool`.
class OneByOneTool : public TimeBaseTool {
public:
    using TimeBaseTool::TimeBaseTool;
    bool affect(QList<SubEntryPtr> &list) override;

protected:
    virtual void affect(SubEntry &sub) = 0;
    // Neighbours within the affected list (null at the ends).
    SubEntryPtr getPreviousEntry() const { return index_ > 0 ? current_->at(index_ - 1) : nullptr; }
    SubEntryPtr getNextEntry() const { return index_ + 1 < current_->size() ? current_->at(index_ + 1) : nullptr; }
    QStringList gatherExtendedTimedTags() const override;
    virtual QStringList gatherSelfTags() const = 0;

private:
    const QList<SubEntryPtr> *current_ = nullptr;
    int index_ = 0;
};

// The registered tools in registration order, port of the core of
// `ToolsManager`. Tools with a menu appear in the GUI; every tool with a
// command name is available on the command line.
class ToolRegistry {
public:
    static ToolRegistry &instance();
    void add(std::shared_ptr<Tool> tool) { tools_.append(std::move(tool)); }
    const QList<std::shared_ptr<Tool>> &tools() const { return tools_; }
    QList<std::shared_ptr<Tool>> toolsAt(ToolLocation loc) const;
    std::shared_ptr<Tool> findByCommandName(const QString &name) const;
    // Command names of every command-line tool.
    QStringList commandNames() const;
    template <typename T>
    std::shared_ptr<T> find() const {
        for (const auto &t : tools_)
            if (auto p = std::dynamic_pointer_cast<T>(t)) return p;
        return nullptr;
    }

private:
    QList<std::shared_ptr<Tool>> tools_;
};

// Registers every built-in tool (coretools + command-line-only ones).
void registerBuiltinTools(ToolRegistry &reg);
