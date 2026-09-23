/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/cmdline/CommandLine.h"

#include <QFileInfo>
#include <QSet>

#include "core/formats/SubFormat.h"
#include "core/os/Debug.h"
#include "core/os/FileCommunicator.h"
#include "core/subs/Subtitles.h"
#include "core/tools/Tool.h"

namespace CommandLine {

namespace {
struct Ctx {
    QTextStream &out;
    QTextStream &err;
    bool debug = false;
};

// ":tag:path" → (tag, path); anything else → (null, argument).
void parseTagged(const QString &argument, QString &tag, QString &value) {
    if (argument.startsWith(QLatin1Char(':'))) {
        const int second = argument.indexOf(QLatin1Char(':'), 1);
        if (second > 1) {
            tag = argument.mid(1, second - 1);
            value = argument.mid(second + 1);
            return;
        }
    }
    tag = QString();
    value = argument;
}

QString tagLabel(const QString &tag) {
    return tag.isNull() || tag.isEmpty() ? QStringLiteral(" (default)") : QStringLiteral(" with tag '%1'").arg(tag);
}

QString longName(const QString &shortOpt) {
    if (shortOpt == QLatin1String("-l")) return QStringLiteral("--load");
    if (shortOpt == QLatin1String("-s")) return QStringLiteral("--save");
    if (shortOpt == QLatin1String("-x")) return QStringLiteral("--execute");
    if (shortOpt == QLatin1String("-r")) return QStringLiteral("--remove");
    return shortOpt;
}

// The command line's debug output: standard output, no timestamps (as the Java).
QTextStream *g_debugOut = nullptr;
void debugLine(const QString &line) {
    if (g_debugOut) *g_debugOut << line << "\n";
    else Debug::debug(line);
}

std::unique_ptr<Subtitles> importFile(const QString &path, bool debug, QString &error) {
    if (debug) debugLine(QStringLiteral("Loading subtitle file: ") + path);
    SubFile sfile(path, SubFile::EXTENSION_GIVEN);
    const QString data = FileCommunicator::load(sfile, debug);
    if (data.isNull()) {
        error = QStringLiteral("Could not load file. Possibly an encoding error.");
        return nullptr;
    }
    auto subs = std::make_unique<Subtitles>(sfile);
    subs->populate(sfile, data, debug);
    if (subs->isEmpty()) {
        error = QStringLiteral("File not recognized!");
        return nullptr;
    }
    if (debug)
        debugLine(QStringLiteral("Loaded %1 subtitles from %2").arg(subs->size()).arg(path));
    return subs;
}

bool exportFile(Subtitles &subs, const QString &path, bool debug, QString &error) {
    SubFile sfile(path, SubFile::EXTENSION_GIVEN);
    sfile.setEncoding(QStringLiteral("UTF-8"));   // the Java exporter always wrote UTF-8
    sfile.setFPS(subs.getSubFile().getFPS());
    const QString ext = QFileInfo(path).suffix();
    SubFormatPtr byExt = ext.isEmpty() ? nullptr : Availabilities::formats().findFromExtension(ext);
    sfile.setFormat(byExt ? byExt : subs.getSubFile().getFormat());
    if (debug)
        debugLine(QStringLiteral("Saving subtitle file: ") + path);
    const QString err = FileCommunicator::save(subs, sfile, nullptr);
    if (!err.isNull()) {
        error = QStringLiteral("Could not save file %1: %2").arg(path, err);
        return false;
    }
    if (debug)
        debugLine(QStringLiteral("Successfully saved %1 subtitles to %2").arg(subs.size()).arg(path));
    return true;
}

void printToolList(QTextStream &s, bool withHint) {
    const QStringList names = ToolRegistry::instance().commandNames();
    if (names.isEmpty()) {
        s << "No tools available\n";
        return;
    }
    s << "Available tools:\n";   // then all names on one line, as the Java printed them
    for (const QString &n : names)
        s << " " << n;
    s << "\n";
    if (withHint)
        s << "Use --help-tool <name> to get detailed help for a specific tool.\n";
}

int executeTool(Ctx &c, const QString &toolString) {
    if (!CommandLineContext::getSubtitles(QString())) { c.err << "ERROR: No default subtitle file loaded\n"; return 1; }
    if (toolString.isEmpty()) { c.err << "ERROR: Tool string cannot be empty\n"; return 1; }
    const int colon = toolString.indexOf(QLatin1Char(':'));
    if (colon < 0) { c.err << "ERROR: Tool string must have at least one parameter\n"; return 1; }
    const QString toolName = toolString.left(colon).trimmed();
    const QString parameters = toolString.mid(colon + 1).trimmed();
    if (toolName.isEmpty()) { c.err << "ERROR: Tool name cannot be empty\n"; return 1; }
    if (parameters.isEmpty()) { c.err << "ERROR: Tool parameters cannot be empty\n"; return 1; }
    std::shared_ptr<Tool> tool = ToolRegistry::instance().findByCommandName(toolName);
    if (!tool) {
        c.err << "ERROR: Tool '" << toolName << "' not found\n";
        printToolList(c.err, false);
        return 1;
    }
    QString result;
    try {
        result = tool->executeParamsLine(parameters, c.debug);
    } catch (const std::exception &e) {
        c.err << "ERROR: Failed to execute tool '" << toolName << "': " << e.what() << "\n";
        return 1;
    }
    if (!result.isNull()) {
        c.err << "ERROR: " << result << "\n";
        return 1;
    }
    return 0;
}
}  // namespace

// The Java (Args library) help layout; --new and --debug are flags.
QString usage() {
    return QStringLiteral(
        "jubler - Subtitle Editor\n"
        "\n"
        "Usage:\n"
        "  jubler [--load LOAD]... [--save SAVE]... [--execute EXECUTE]... [--remove\n"
        "REMOVE]... [--swap SWAP]... [--new] [--debug] [--list-tools]\n"
        "[--help-tool HELP-TOOL] ARGS...\n"
        "\n"
        "Arguments:\n"
        "  --load|-l LOAD        : Load subtitle file (format: file or :tag:file) - use\n"
        "                          tags to load multiple files or leave it empty for the\n"
        "                          base subtitle file where all actions are performed\n"
        "  --save|-s SAVE        : Save subtitle file (format: file or :tag:file) - use\n"
        "                          tags to save specific files\n"
        "  --execute|-x EXECUTE  : Execute tool with name and parameters (e.g.,\n"
        "                          name:param1:param2)\n"
        "  --remove|-r REMOVE    : Remove tagged subtitle from memory (format: tag) -\n"
        "                          null tag name is not allowed\n"
        "  --swap SWAP           : Swap tagged subtitle with default (format: tag)\n"
        "  --new                 : Create a new empty subtitle file in memory\n"
        "  --debug               : Enable debugging output\n"
        "  --list-tools          : List all available tools\n"
        "  --help-tool HELP-TOOL : Show detailed help for a specific tool\n"
        "\n"
        "Argument --load can be used more than once.\n"
        "Argument --save can be used more than once.\n"
        "Argument --swap can be used more than once.\n"
        "Argument --remove can be used more than once.\n"
        "Argument --execute can be used more than once.\n");
}

bool isCommandLineInvocation(const QStringList &args) {
    static const QStringList shorts = {QStringLiteral("-l"), QStringLiteral("-s"), QStringLiteral("-x"), QStringLiteral("-r"), QStringLiteral("-h")};
    for (const QString &a : args)
        if (a.startsWith(QLatin1String("--")) || shorts.contains(a))
            return true;
    return false;
}

QString loadSubtitles(const QString &path, bool debug, QString &error) {
    QString tag, file;
    parseTagged(path, tag, file);
    auto subs = importFile(file, debug, error);
    if (!subs)
        return QString();
    CommandLineContext::addSubtitles(tag, std::move(subs));
    return tag.isNull() ? QString(QLatin1String("")) : tag;
}

int run(const QStringList &args, QTextStream &out, QTextStream &err) {
    Ctx c{out, err, false};
    g_debugOut = &out;
    static const QStringList valued = {QStringLiteral("--load"), QStringLiteral("-l"), QStringLiteral("--save"), QStringLiteral("-s"),
                                       QStringLiteral("--execute"), QStringLiteral("-x"), QStringLiteral("--remove"), QStringLiteral("-r"),
                                       QStringLiteral("--swap"), QStringLiteral("--help-tool")};
    // --help anywhere wins: nothing else runs (Java). Values of options are skipped.
    for (int i = 0; i < args.size(); ++i) {
        if (args.at(i) == QLatin1String("--help") || args.at(i) == QLatin1String("-h")) {
            out << usage();
            return 0;
        }
        if (valued.contains(args.at(i))) ++i;
    }
    auto needValue = [&](int i, const QString &opt) -> bool {
        if (i + 1 >= args.size()) {
            err << "ERROR: Too few arguments: unable to find value of argument " << longName(opt) << "\n";
            out << usage();
            return false;
        }
        return true;
    };
    // Options that may appear only once.
    QSet<QString> seen;
    auto once = [&](const QString &opt) -> bool {
        if (!seen.contains(opt)) {
            seen.insert(opt);
            return true;
        }
        err << "ERROR: Argument " << opt << " should appear only once\n";
        out << usage();
        return false;
    };
    QStringList leftovers;
    for (int i = 0; i < args.size(); ++i) {
        const QString a = args.at(i);
        if ((a == QLatin1String("--debug") || a == QLatin1String("--new") || a == QLatin1String("--list-tools") || a == QLatin1String("--help-tool")) && !once(a))
            return 1;
        if (a == QLatin1String("--debug")) {
            // Optional value: true/1/yes (a following option word is not a value).
            if (i + 1 < args.size() && !args.at(i + 1).startsWith(QLatin1Char('-'))) {
                const QString v = args.at(i + 1).toLower();
                c.debug = v == QLatin1String("true") || v == QLatin1String("1") || v == QLatin1String("yes");
                ++i;
            } else
                c.debug = true;
        } else if (a == QLatin1String("--load") || a == QLatin1String("-l")) {
            if (!needValue(i, a)) return 1;
            QString tag, file;
            parseTagged(args.at(++i), tag, file);
            QString error;
            auto subs = importFile(file, c.debug, error);
            if (!subs) { err << "ERROR: " << error << "\n"; return 1; }
            CommandLineContext::addSubtitles(tag, std::move(subs));
            if (c.debug)
                out << "Loaded subtitle file" << (tag.isNull() ? QStringLiteral(" as default") : QStringLiteral(" with tag '%1'").arg(tag)) << ": " << file << "\n";
        } else if (a == QLatin1String("--save") || a == QLatin1String("-s")) {
            if (!needValue(i, a)) return 1;
            QString tag, file;
            parseTagged(args.at(++i), tag, file);
            Subtitles *subs = CommandLineContext::getSubtitles(tag);
            if (!subs) { err << "ERROR: No subtitles loaded" << tagLabel(tag) << ". Use --load first\n"; return 1; }
            QString error;
            if (!exportFile(*subs, file, c.debug, error)) { err << "ERROR: " << error << "\n"; return 1; }
            if (c.debug)
                out << "Saved subtitle file" << tagLabel(tag) << ": " << file << "\n";
        } else if (a == QLatin1String("--execute") || a == QLatin1String("-x")) {
            if (!needValue(i, a)) return 1;
            const int rc = executeTool(c, args.at(++i));
            if (rc) return rc;
        } else if (a == QLatin1String("--remove") || a == QLatin1String("-r")) {
            if (!needValue(i, a)) return 1;
            const QString tag = args.at(++i);
            if (tag.trimmed().isEmpty()) { err << "ERROR: Tag cannot be empty for --remove command.\n"; return 1; }
            if (!CommandLineContext::hasSubtitles(tag)) { err << "ERROR: No subtitles found with tag '" << tag << "' to remove\n"; return 1; }
            CommandLineContext::removeSubtitles(tag);
            if (c.debug) out << "Removed subtitles with tag '" << tag << "' from memory\n";
        } else if (a == QLatin1String("--swap")) {
            if (!needValue(i, a)) return 1;
            const QString tag = args.at(++i);
            if (tag.trimmed().isEmpty()) { err << "ERROR: Tag cannot be empty for --swap command\n"; return 1; }
            if (!CommandLineContext::hasSubtitles(tag)) { err << "ERROR: No subtitles found with tag '" << tag << "'\n"; return 1; }
            if (!CommandLineContext::hasSubtitles(QString())) { err << "ERROR: No default subtitles loaded to swap with\n"; return 1; }
            CommandLineContext::swap(tag);
            if (c.debug) out << "Swapped: tag '" << tag << "' is now default, previous default is now tagged '" << tag << "'\n";
        } else if (a == QLatin1String("--new")) {
            // The old value form "--new <anything>" is still accepted; the value is ignored.
            if (i + 1 < args.size() && !args.at(i + 1).startsWith(QLatin1Char('-'))) ++i;
            CommandLineContext::addSubtitles(QString(), std::make_unique<Subtitles>());
            if (c.debug) out << "Created new empty subtitle file in memory\n";
        } else if (a == QLatin1String("--list-tools")) {
            printToolList(out, true);
        } else if (a == QLatin1String("--help-tool")) {
            if (!needValue(i, a)) return 1;
            const QString name = args.at(++i).trimmed();
            if (name.isEmpty()) { err << "ERROR: Tool name cannot be empty\n"; return 1; }
            std::shared_ptr<Tool> tool = ToolRegistry::instance().findByCommandName(name);
            if (!tool) { err << "ERROR: Tool '" << name << "' not found\n"; printToolList(err, false); return 1; }
            out << name << "\n";
            const QString help = tool->getCommandLineHelp();
            out << "  " << (help.isEmpty() ? QStringLiteral("No help available for this tool") : help) << "\n";
        } else
            leftovers.append(a);
    }
    if (!leftovers.isEmpty()) {
        err << "ERROR: Unknown arguments: " << leftovers.join(QLatin1Char(' ')) << "\n";   // no help (Java)
        return 1;
    }
    return 0;
}

}  // namespace CommandLine
