/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/subdownload/SubDownloadTool.h"

#include "core/i18n/I18N.h"

SubDownloadTool::SubDownloadTool() : Tool(ToolMenu{__("Download subtitles…"), QStringLiteral("TDL"), ToolLocation::FILETOOL}) {}
QString SubDownloadTool::getToolTitle() const { return __("Download subtitles…"); }
