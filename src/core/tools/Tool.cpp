/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/tools/Tool.h"

#include <map>
#include <cmath>
#include <limits>

#include "core/os/Debug.h"
#include "core/os/Escapes.h"

// ---- CommandLineContext -------------------------------------------------------

namespace CommandLineContext {
namespace {
std::map<QString, std::unique_ptr<Subtitles>> &map() {
    static std::map<QString, std::unique_ptr<Subtitles>> m;
    return m;
}
QString norm(const QString &tag) { return tag.isNull() ? QString(QLatin1String("")) : tag; }
}  // namespace

Subtitles *getSubtitles(const QString &tag) {
    auto it = map().find(norm(tag));
    return it == map().end() ? nullptr : it->second.get();
}
void addSubtitles(const QString &tag, std::unique_ptr<Subtitles> subs) { map()[norm(tag)] = std::move(subs); }
void removeSubtitles(const QString &tag) { map().erase(norm(tag)); }
bool hasSubtitles(const QString &tag) { return map().count(norm(tag)) > 0; }
void clear() { map().clear(); }
void swap(const QString &tag) {
    std::unique_ptr<Subtitles> a = std::move(map()[norm(QString())]);
    std::unique_ptr<Subtitles> b = std::move(map()[norm(tag)]);
    map()[norm(QString())] = std::move(b);
    map()[norm(tag)] = std::move(a);
}
}  // namespace CommandLineContext

// ---- Tool ---------------------------------------------------------------------

const QStringList &Tool::getToolTags() {
    if (!tagsGathered_) {
        tagsGathered_ = true;
        tags_ = gatherToolTags();
    }
    return tags_;
}

QString Tool::executeParamsLine(const QString &argument, bool debug) {
    QMap<QString, QString> params;
    if (!argument.trimmed().isEmpty()) {
        for (const QString &piece : Escapes::parseParametersWithEscaping(argument)) {
            const int eq = piece.indexOf(QLatin1Char('='));
            if (eq >= 0) {
                const QString key = piece.left(eq);
                const QString value = Escapes::unescapeParameterValue(piece.mid(eq + 1));
                if (!getToolTags().contains(key))
                    return QStringLiteral("Invalid parameter: %1. Valid parameters are: %2").arg(key, getToolTags().join(QStringLiteral(", ")));
                params.insert(key, value);  // last one wins
            } else if (!piece.trimmed().isEmpty())
                return QStringLiteral("Invalid parameter format: %1. Expected format: key=value").arg(piece);
        }
    }
    return executeParams(params, debug);
}

double Tool::parseDoubleParameter(const QMap<QString, QString> &m, const QString &key) {
    if (!m.contains(key))
        return std::numeric_limits<double>::quiet_NaN();
    bool ok = false;
    const double v = m.value(key).trimmed().toDouble(&ok);
    if (!ok)
        throw FilterException(QStringLiteral("Invalid number format for %1=%2").arg(key, m.value(key)));
    return v;
}

int Tool::parseIntParameter(const QMap<QString, QString> &m, const QString &key) {
    if (!m.contains(key))
        throw FilterException(QStringLiteral("Missing parameter: %1").arg(key));
    bool ok = false;
    const int v = m.value(key).trimmed().toInt(&ok);
    if (!ok)
        throw FilterException(QStringLiteral("Invalid number format for %1=%2").arg(key, m.value(key)));
    return v;
}

std::optional<bool> Tool::parseBooleanParameter(const QMap<QString, QString> &m, const QString &key) {
    if (!m.contains(key))
        return std::nullopt;
    const QString v = m.value(key).trimmed().toLower();
    if (v == QLatin1String("true") || v == QLatin1String("yes") || v == QLatin1String("1")) return true;
    if (v == QLatin1String("false") || v == QLatin1String("no") || v == QLatin1String("0")) return false;
    return std::nullopt;
}

int Tool::parseMarkParam(const QMap<QString, QString> &m, const QString &key) {
    if (!m.contains(key))
        return -1;
    QString v = m.value(key).trimmed().toLower();
    v.remove(QLatin1Char(' '));
    for (int i = 0; i < SubEntry::MARK_COUNT; ++i)
        if (v == QLatin1String(SubEntry::MarkColorKeys[i]))
            return i;
    bool ok = false;
    const int idx = v.toInt(&ok);
    if (ok && idx >= 0 && idx < SubEntry::MARK_COUNT)
        return idx;
    return -1;
}

SubStylePtr Tool::parseStyleParam(const QMap<QString, QString> &m, const Subtitles *subs, const QString &key) {
    if (!subs || !m.contains(key) || m.value(key).trimmed().isEmpty())
        return nullptr;
    const QString name = m.value(key);
    for (const SubStylePtr &s : subs->getStyleList().all())
        if (s->getName() == name)
            return s;
    return nullptr;
}

QList<SubEntryPtr> Tool::filterByColor(const Subtitles &subs, int mark) {
    QList<SubEntryPtr> out;
    for (const SubEntryPtr &e : subs.entries())
        if (e->getMark() == mark)
            out.append(e);
    return out;
}

QList<SubEntryPtr> Tool::filterByStyle(const Subtitles &subs, const SubStylePtr &style) {
    QList<SubEntryPtr> out;
    for (const SubEntryPtr &e : subs.entries())
        if (e->getStyle() == style)
            out.append(e);
    return out;
}

// ---- TimeBaseTool -------------------------------------------------------------

QStringList TimeBaseTool::gatherToolTags() const {
    QStringList tags = {QStringLiteral("start"), QStringLiteral("end"), QStringLiteral("alsomark")};
    for (const QString &t : gatherExtendedTimedTags())
        if (!tags.contains(t))
            tags.append(t);
    return tags;
}

QList<SubEntryPtr> TimeBaseTool::filterSubtitles(const Subtitles &subs, const QMap<QString, QString> &params) const {
    const double start = parseDoubleParameter(params, QStringLiteral("start"));
    const double end = parseDoubleParameter(params, QStringLiteral("end"));
    if (!std::isnan(start) && !std::isnan(end)) {
        QList<SubEntryPtr> out;
        for (const SubEntryPtr &e : subs.entries())
            if (e->getFinishTime().toSeconds() >= start && e->getStartTime().toSeconds() <= end)
                out.append(e);
        return out;
    }
    const int mark = parseMarkParam(params, QStringLiteral("bymark"));
    if (mark >= 0)
        return filterByColor(subs, mark);
    if (const SubStylePtr style = parseStyleParam(params, &subs, QStringLiteral("bystyle")))
        return filterByStyle(subs, style);
    throw FilterException(QStringLiteral("No valid subtitle filtering criteria found."));
}

QString TimeBaseTool::executeParams(const QMap<QString, QString> &params, bool debug) {
    Subtitles *subs = CommandLineContext::getSubtitles(QString());
    if (!subs)
        return QStringLiteral("No default subtitle file loaded");
    subtitles_ = subs;
    if (debug)
        Debug::debug(QStringLiteral("Executing %1 tool with %2 arguments:").arg(getCommandOptionName()).arg(params.size()));
    QList<SubEntryPtr> list;
    try {
        list = filterSubtitles(*subs, params);
    } catch (const FilterException &e) {
        return e.message();
    }
    if (list.isEmpty()) {
        if (debug)
            Debug::debug(QStringLiteral("No subtitles match the filtering criteria"));
        return QString();
    }
    if (debug)
        Debug::debug(QStringLiteral("Processing %1 subtitle(s)").arg(list.size()));
    try {
        const QString err = applyToolSpecificArguments(params);
        if (!err.isNull())
            return err;
    } catch (const FilterException &e) {
        return e.message();
    }
    if (!affect(list))
        return QStringLiteral("Tool execution failed");
    const int alsomark = parseMarkParam(params, QStringLiteral("alsomark"));
    if (alsomark >= 0)
        for (const SubEntryPtr &e : list)
            e->setMark(alsomark);
    if (debug)
        Debug::debug(QStringLiteral("Tool execution completed successfully"));
    return QString();
}

// ---- OneByOneTool -------------------------------------------------------------

bool OneByOneTool::affect(QList<SubEntryPtr> &list) {
    current_ = &list;
    for (index_ = 0; index_ < list.size(); ++index_)
        affect(*list.at(index_));
    current_ = nullptr;
    return true;
}

QStringList OneByOneTool::gatherExtendedTimedTags() const {
    QStringList tags = {QStringLiteral("bymark"), QStringLiteral("bystyle")};
    for (const QString &t : gatherSelfTags())
        if (!tags.contains(t))
            tags.append(t);
    tags.sort();   // a TreeSet in the Java: alphabetical ("Valid parameters are: …")
    return tags;
}

// ---- ToolRegistry -------------------------------------------------------------

ToolRegistry &ToolRegistry::instance() {
    static ToolRegistry reg;
    static bool init = false;
    if (!init) {
        init = true;
        registerBuiltinTools(reg);
    }
    return reg;
}

QList<std::shared_ptr<Tool>> ToolRegistry::toolsAt(ToolLocation loc) const {
    QList<std::shared_ptr<Tool>> out;
    for (const auto &t : tools_)
        if (t->menu() && t->menu()->location == loc)
            out.append(t);
    return out;
}

std::shared_ptr<Tool> ToolRegistry::findByCommandName(const QString &name) const {
    for (const auto &t : tools_)
        if (!t->getCommandOptionName().isEmpty() && t->getCommandOptionName() == name)
            return t;
    return nullptr;
}

QStringList ToolRegistry::commandNames() const {
    QStringList out;
    for (const auto &t : tools_)
        if (!t->getCommandOptionName().isEmpty())
            out.append(t->getCommandOptionName());
    return out;
}
