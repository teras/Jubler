/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <QStringList>

// String un-escaping helpers for command-line tool parameters, port of the
// Java `Escapes`.
namespace Escapes {
// \b \t \n \f \r \" \' \\, \uXXXX (4 hex digits), octal \0..\377; unknown
// escapes and a trailing backslash are kept literally.
QString unescapeJavaLenient(const QString &s);
// Split on unescaped ':' ("\:" is a literal colon); every other backslash
// sequence is passed through untouched. An empty final segment is dropped.
QStringList parseParametersWithEscaping(const QString &input);
// \: → :, \= → =, \\ → \; any other \x kept; trailing backslash kept.
QString unescapeParameterValue(const QString &value);
}  // namespace Escapes
