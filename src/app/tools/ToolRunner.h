/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>

#include "core/tools/CoreTools.h"

class MainWindow;
class QMenu;

// The GUI side of the tool registry: the Tools menu / Edit sub-menu items,
// their availability, and the entry points used outside the menus. Port of
// `ToolsManager` (+ the `execute` half of every tool).
namespace ToolRunner {
void registerMenus(MainWindow *window, QMenu *tools, QMenu *deleteMenu, QMenu *markMenu, QMenu *styleMenu);
void updateToolsAvailability(MainWindow *window, QMenu *tools);
// Edit ▸ Replace ▸ Regular Expression.
void runRegExpReplace(MainWindow *window);
// The preview pipette: shift when both differences agree, recode otherwise.
// False when the points do not define a transformation.
bool runSync(MainWindow *window, const TimeSync &first, const TimeSync &second);
// Run a tool by its menu name (e.g. "TSH"); false when unknown.
bool runTool(MainWindow *window, const QString &menuName);
}  // namespace ToolRunner
