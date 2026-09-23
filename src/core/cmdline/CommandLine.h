/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <QStringList>
#include <QTextStream>

// Headless command-line mode, port of `CommandLine`/`CmdTools`/`Importer`/
// `Exporter`. Options are processed in the order given:
//   --load/-l <file | :tag:file>   --save/-s <file | :tag:file>
//   --execute/-x <tool:params>     --remove/-r <tag>   --swap <tag>
//   --new   --debug [true|1|yes]   --list-tools   --help-tool <name>
//   --help/-h
// Returns the process exit code. Errors go to `err` (stderr) and stop the
// batch at the first failure, as in the Java build.
namespace CommandLine {

// True when the arguments select command-line mode (any --option or the
// short aliases -l/-s/-x/-r/-h).
bool isCommandLineInvocation(const QStringList &args);
int run(const QStringList &args, QTextStream &out, QTextStream &err);

// Pieces, also used by tests.
QString loadSubtitles(const QString &path, bool debug, QString &error);   // into the tag slot: see run()
QString usage();

}  // namespace CommandLine
