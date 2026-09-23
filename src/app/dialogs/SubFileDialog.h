/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <optional>

#include "core/subs/SubFile.h"

class QWidget;
class Subtitles;
class AppMediaFile;

// The open/save dialogs of subtitle documents; the last directory is
// remembered, the save dialog offers only the document's format and forces
// its extension. Port of `JSubFileDialog`.
namespace SubFileDialog {
std::optional<SubFile> getLoadFile(QWidget *parent, AppMediaFile *mfile);
std::optional<SubFile> getSaveFile(QWidget *parent, Subtitles *subs, AppMediaFile *mfile);
}  // namespace SubFileDialog
