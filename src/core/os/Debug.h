/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <exception>

// Diagnostic logging, port of the Java `DEBUG`: messages go to stderr and to
// the log file next to the per-user data (rotated at start: the previous log
// is kept with a ".1" suffix).
namespace Debug {
void debug(const QString &msg);
void debug(const std::exception &e);
void init();                 // open the log file
QString logFilePath();
}  // namespace Debug
